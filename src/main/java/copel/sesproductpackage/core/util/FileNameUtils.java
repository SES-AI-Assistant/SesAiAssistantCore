package copel.sesproductpackage.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import jakarta.mail.internet.MimeUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * ファイル名に関するユーティリティクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class FileNameUtils {

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
    try {
      // MimeUtility.decodeTextは内部でMIME形式か判定してくれる
      decoded = MimeUtility.decodeText(decoded);
    } catch (UnsupportedEncodingException e) {
      log.debug("MIMEデコードに失敗しました（スキップ）: {}", rawFileName);
    }

    // 2. RFC 5987形式のプレフィックス除去 (UTF-8'' 等)
    // filename*=UTF-8''%e3... の %e3 以降が渡されるケースと UTF-8'' 自体が含まれるケースに対応
    if (decoded.toUpperCase().startsWith("UTF-8''")) {
      decoded = decoded.substring(7);
    } else if (decoded.toUpperCase().startsWith("SHIFT_JIS''")) {
      decoded = decoded.substring(11);
    } else if (decoded.toUpperCase().startsWith("ISO-8859-1''")) {
      decoded = decoded.substring(12);
    }

    // 3. URLデコード (%E6%88%B8... 形式)
    // URLデコードは複数回かかっている可能性があるため、変化がなくなるまで繰り返す（最大3回まで）
    try {
      for (int i = 0; i < 3; i++) {
        if (!decoded.contains("%")) {
          break;
        }
        String previous = decoded;
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
}
