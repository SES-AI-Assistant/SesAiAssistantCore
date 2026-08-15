package copel.sesproductpackage.core.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

/** Core共通で使用するObjectMapper */
public class ObjectMapperFactory {
  /** ObjectMapperインスタンス. */
  public static final ObjectMapper OBJECT_MAPPER = createGptObjectMapper();

  /**
   * ObjectMapper生成クラス.
   *
   * @return ObjectMapper
   */
  private static ObjectMapper createGptObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    // 変数宣言で型を明示する
    JsonDeserializer<String> stringDeserializer =
        new JsonDeserializer<String>() {
          @Override
          public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null) return null;
            String trimmed = value.trim();
            if (trimmed.equalsIgnoreCase("null")) {
              return null;
            }
            return trimmed;
          }
        };
    module.addDeserializer(String.class, stringDeserializer);
    objectMapper.registerModule(module);
    return objectMapper;
  }
}
