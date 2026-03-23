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
        String previous = decoded;
        // 基本はUTF-8で試行し、失敗や変化なしなら終了
        decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
        if (decoded.equals(previous)) {
          break;
        }
      }
    } catch (Exception e) {
      log.debug("URLデコードに失敗しました（スキップ）: {}", decoded);
    }

    // 4. サニタイズ (OSで禁止されている文字を除去/置換)
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
    if (text == null || !text.contains("=?")) {
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
        } else if ("Q".equals(encoding)) {
          // Quoted-Printableデコードの簡易実装
          String qDecoded = encodedText.replace("_", " ");
          byte[] bytes = decodeQuotedPrintable(qDecoded);
          matcher.appendReplacement(sb, Matcher.quoteReplacement(new String(bytes, charset)));
        } else {
          matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
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
        try {
          int u = Character.digit(s.charAt(i + 1), 16);
          int l = Character.digit(s.charAt(i + 2), 16);
          if (u != -1 && l != -1) {
            buffer.write((char) ((u << 4) + l));
            i += 2;
            continue;
          }
        } catch (Exception ignored) {
        }
      }
      buffer.write(c);
    }
    return buffer.toByteArray();
  }
}
