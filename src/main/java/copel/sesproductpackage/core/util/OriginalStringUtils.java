package copel.sesproductpackage.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * 文字列操作ユーティリティクラス.
 *
 * @author Copel Co., Ltd.
 */
public class OriginalStringUtils {
  /**
   * オブジェクトをJSON化します.
   *
   * @param obj オブジェクト
   * @return JSON文字列
   */
  public static String toJson(Object obj) {
    try {
      return ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * オブジェクトを成形済みJSON化します.
   *
   * @param obj オブジェクト
   * @return JSON文字列
   */
  public static String toFormatJson(Object obj) {
    try {
      return ObjectMapperFactory.OBJECT_MAPPER
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 引数の文字列が空文字またはNULLであるかどうかを判定する.
   *
   * @param str 文字列
   * @return 空であればtrue、それ以外はfalse
   */
  public static boolean isEmpty(final String str) {
    return str == null || str.isBlank() || str.isEmpty() || "null".equals(str);
  }

  /**
   * 引数の文字列が未設定（空・NULL）または「Unknown」であるかどうかを判定する.
   *
   * <p>LLMは項目の記載が無い場合に、指示どおり省略せず「Unknown」という文字列を返すことがある（場所の抽出で確認済み）。
   * そのまま保存すると画面にも「Unknown」と表示されるため、空と同じく未設定として扱えるようにする.
   *
   * @param str 文字列
   * @return 未設定または「Unknown」（大文字小文字・前後空白は問わない）であればtrue
   */
  public static boolean isUnspecified(final String str) {
    return isEmpty(str) || "unknown".equalsIgnoreCase(str.strip());
  }

  /**
   * 文字列を指定した文字数（コードポイント数）以内に切り詰める.
   *
   * <p>PostgreSQLのVARCHAR長は文字（コードポイント）単位で数えるため、{@link String#length()}（UTF-16単位）で切ると
   * サロゲートペア（例: 𠮷）を分断したり、上限より短く切りすぎたりする。DBカラム長に合わせる用途ではこのメソッドを使う.
   *
   * @param str 文字列
   * @param maxLength 最大文字数（コードポイント数）
   * @return maxLength以内ならそのまま、超える場合は先頭からmaxLength文字。nullの場合はnull
   */
  public static String truncateByCodePoints(final String str, final int maxLength) {
    if (str == null || str.codePointCount(0, str.length()) <= maxLength) {
      return str;
    }
    return str.substring(0, str.offsetByCodePoints(0, maxLength));
  }
}
