package copel.sesproductpackage.core.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * ファイル名に関するユーティリティクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class FileNameUtils {
  /** インスタンス化を禁止する。すべての操作は静的メンバーで提供する。 */
  private FileNameUtils() {}

  /**
   * 日本語またはマルチバイト文字とみなす文字の範囲。各要素は {開始の文字, 終了の文字} の組で、両端を含む。
   *
   * <p>ひらがな、カタカナ、漢字、CJK の部首補助・拡張 A・互換漢字の範囲を並べる。
   */
  private static final char[][] MULTIBYTE_RANGES = {
    {'぀', 'ゟ'}, // ひらがな
    {'゠', 'ヿ'}, // カタカナ
    {'一', '鿿'}, // 漢字
    {'⺀', '⻿'}, // CJK Radicals Supplement
    {'㐀', '䶿'}, // CJK Unified Ideographs Extension A
    {'豈', '﫿'}, // CJK Compatibility Ideographs
  };

  // MIMEエンコードの正規表現 (例: =?UTF-8?B?...?= または =?ISO-8859-1?Q?...?=)
  private static final Pattern MIME_PATTERN =
      Pattern.compile("=\\?([^?]+)\\?([BQbq])\\?([^?]+)\\?=");

  /**
   * エンコードされたファイル名をデコードし、サニタイズした文字列を返す.
   *
   * <p>MIMEエンコード、URLエンコード、RFC 5987形式を自動判別して処理する。
   *
   * @param rawFileName デコード前のファイル名
   * @return デコード・サニタイズ後のファイル名
   */
  public static String decode(final String rawFileName) {
    if (rawFileName == null || rawFileName.isEmpty()) {
      return rawFileName;
    }

    String decoded = rawFileName.trim();

    // 1. MIMEデコード (=?UTF-8?B?...?= 形式など)
    decoded = decodeMimeText(decoded);

    // 2. RFC 5987形式のデコード (charset'lang'encoded-text)
    // 例: UTF-8''%e3%81%82.txt or UTF-8'ja'%e3%81%82.txt
    Pattern rfc5987Pattern = Pattern.compile("^([^']*)'[^']*'(.*)$");
    Matcher matcher = rfc5987Pattern.matcher(decoded);
    if (matcher.find()) {
      String charset = matcher.group(1);
      String value = matcher.group(2);
      if (charset.isEmpty()) {
        charset = StandardCharsets.UTF_8.name();
      }
      try {
        decoded = URLDecoder.decode(value, charset);
      } catch (Exception e) {
        log.debug("RFC 5987パース後のデコードに失敗しました: {} (charset: {})", value, charset);
        decoded = value; // 失敗時は値部分をそのまま採用
      }
    }

    // 3. URLデコード (%E6%88%B8... 形式)
    // RFC 5987を通らなかった場合や、二重エンコードされているケースに対応
    try {
      for (int i = 0; i < 3; i++) {
        if (!decoded.contains("%")) {
          break;
        }
        // 基本はUTF-8で試行し、失敗したら終了する。%XX を含む文字列は必ず変化するため、変化の有無では判定しない
        decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
      }
    } catch (Exception e) {
      log.debug("URLデコードに失敗しました（スキップ）: {}", decoded);
    }

    // 4. UTF-8がISO-8859-1として誤解釈されたものを修正
    decoded = recoverUtf8MisinterpretedAsLatin1(decoded);

    // 5. サニタイズ (OSで禁止されている文字を除去/置換)
    // \ / : * ? " < > | および制御文字をアンダースコアに置換
    decoded = decoded.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F\\x7F]", "_");

    // 末尾のドットやスペースもトリミング（Windows対策）
    decoded = decoded.trim();
    while (decoded.endsWith(".") && decoded.length() > 1) {
      decoded = decoded.substring(0, decoded.length() - 1).trim();
    }

    return decoded;
  }

  /**
   * MIMEエンコードされた文字列をデコードします.
   *
   * @param text 対象の文字列
   * @return デコード後の文字列
   */
  private static String decodeMimeText(String text) {
    if (!text.contains("=?")) {
      return text;
    }

    Matcher matcher = MIME_PATTERN.matcher(text);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String charset = matcher.group(1);
      String encoding = matcher.group(2).toUpperCase();
      String encodedText = matcher.group(3);

      try {
        if ("B".equals(encoding)) {
          byte[] bytes = Base64.getDecoder().decode(encodedText);
          matcher.appendReplacement(sb, Matcher.quoteReplacement(new String(bytes, charset)));
        } else {
          // Quoted-Printableデコードの簡易実装
          // 空白の代わりに _ を使う Q エンコードを、空白に戻してから 16 進数のデコードへ渡す
          // 正規表現の [BQbq] により、ここに来るのは Q だけである
          String quotedPrintableText = encodedText.replace("_", " ");
          byte[] bytes = decodeQuotedPrintable(quotedPrintableText);
          matcher.appendReplacement(sb, Matcher.quoteReplacement(new String(bytes, charset)));
        }
      } catch (Exception e) {
        log.debug("MIMEデコードに失敗しました（スキップ）: {}", matcher.group(0));
        matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
      }
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /** Quoted-Printableエンコードされた文字列をバイト配列にデコードします. */
  private static byte[] decodeQuotedPrintable(String s) {
    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '=' && i + 2 < s.length()) {
        // Character.digit は例外を投げず、16進数でない文字には -1 を返す
        int u = Character.digit(s.charAt(i + 1), 16);
        int l = Character.digit(s.charAt(i + 2), 16);
        if (u != -1 && l != -1) {
          buffer.write((char) ((u << 4) + l));
          i += 2;
          continue;
        }
      }
      buffer.write(c);
    }
    return buffer.toByteArray();
  }

  /**
   * Content-Dispositionヘッダなどで、UTF-8バイト列がISO-8859-1として誤解釈された文字列を修正する.
   *
   * <p>例: "スキルシート"のUTF-8バイト列(E3 82 B9...)がISO-8859-1文字として解釈されると "ã¹ã­ã«ã·ã¼ã"
   * となる。このような場合、正しいUTF-8文字列に復元する。
   *
   * @param text 対象の文字列
   * @return 修正後の文字列（修正不要な場合は元の文字列）
   */
  private static String recoverUtf8MisinterpretedAsLatin1(String text) {
    if (text.isEmpty()) {
      return text;
    }
    // ISO-8859-1 と UTF-8 の変換は例外を投げないため、try で囲まない
    byte[] latin1Bytes = text.getBytes(StandardCharsets.ISO_8859_1);
    String recovered = new String(latin1Bytes, StandardCharsets.UTF_8);
    // 復帰後の文字列に日本語（またはその他のマルチバイト文字）が含まれていたら採用
    // ひらがな、カタカナ、漢字、その他のCJK文字、絵文字などをチェック
    if (containsJapaneseOrMultibyte(recovered)) {
      return recovered;
    }
    return text;
  }

  /**
   * 文字列が日本語またはマルチバイト文字を含むか判定する.
   *
   * @param text 判定対象の文字列
   * @return 日本語またはマルチバイト文字を含めば true
   */
  private static boolean containsJapaneseOrMultibyte(String text) {
    for (char c : text.toCharArray()) {
      for (char[] range : MULTIBYTE_RANGES) {
        if (c >= range[0] && c <= range[1]) {
          return true;
        }
      }
    }
    return false;
  }
}
