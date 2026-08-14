package copel.sesproductpackage.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 文字列操作ユーティリティクラス.
 *
 * @author Copel Co., Ltd.
 */
public class OriginalStringUtils {
  /**
   * JacksonのObjectMapperインスタンス.
   */
  public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * オブジェクトをJSON化します.
   *
   * @param obj オブジェクト
   * @return JSON文字列
   */
  public static String toJson(Object obj) {
      try {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
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
