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
}
