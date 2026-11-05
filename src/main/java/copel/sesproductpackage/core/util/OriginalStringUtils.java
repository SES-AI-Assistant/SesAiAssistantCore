package copel.sesproductpackage.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.text.Normalizer;

/**
 * 文字列操作ユーティリティクラス.
 *
 * @author Copel Co., Ltd.
 */
public class OriginalStringUtils {
  /** インスタンス化を禁止する。すべての操作は静的メンバーで提供する。 */
  private OriginalStringUtils() {}

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
   * 引数の文字列が空文字またはNULLであるかどうかを判定する.
   *
   * @param str 文字列
   * @return 空であればtrue、それ以外はfalse
   */
  public static boolean isEmpty(final String str) {
    // isBlank は空文字も true を返すため、isEmpty での判定は不要
    return str == null || str.isBlank() || "null".equals(str);
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
   * 同じ内容かを比べるために、文字列の全角・半角を統一し、前後の空白を除く.
   *
   * <p>全角英数・半角カナなどを Unicode 正規化（NFKC）で統一し、全角空白も通常の空白にしてから前後を除く。
   * 正規化の後で未設定（空・NULL・「Unknown」）かを判定するため、 全角の「Ｕｎｋｎｏｗｎ」も未設定として扱う。 DB 側で同じ比較をするときは、列にも同じ正規化（{@code
   * normalize(列, NFKC)}）をかける。
   *
   * @param str 文字列
   * @return 全角・半角を統一して前後の空白を除いた文字列。未設定の場合は null
   */
  public static String toComparable(final String str) {
    if (str == null) {
      return null;
    }
    String normalized = Normalizer.normalize(str, Normalizer.Form.NFKC).strip();
    return isUnspecified(normalized) ? null : normalized;
  }

  /**
   * 文字列を指定した文字数（コードポイント数）以内に切り詰める.
   *
   * <p>PostgreSQLのVARCHAR長は文字（コードポイント）単位で数えるため、{@link String#length()}（UTF-16単位）で切ると サロゲートペア（例:
   * 𠮷）を分断したり、上限より短く切りすぎたりする。DBカラム長に合わせる用途ではこのメソッドを使う.
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

  /**
   * 文字列を省略記号「…」を含めて指定した文字数（コードポイント数）以内に切り詰める.
   *
   * <p>LLMの出力はスキーマの文字数指定（maxLength）が守られないことがあり、そのままDBへ保存するとカラム長を超えて INSERT
   * が失敗する。切り詰めた事実が読み手に伝わるよう末尾に「…」を付け、省略記号を含めてカラム長に収める.
   *
   * @param str 文字列
   * @param maxLength 省略記号を含めた最大文字数（コードポイント数）。1以上を指定する
   * @return maxLength以内ならそのまま、超える場合は先頭から(maxLength - 1)文字と「…」。nullの場合はnull
   */
  public static String truncateWithEllipsis(final String str, final int maxLength) {
    if (str == null || str.codePointCount(0, str.length()) <= maxLength) {
      return str;
    }
    return truncateByCodePoints(str, maxLength - 1) + "…";
  }
}
