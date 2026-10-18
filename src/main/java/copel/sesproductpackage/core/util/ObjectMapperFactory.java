package copel.sesproductpackage.core.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
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
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    SimpleModule module = new SimpleModule();
    // 変数宣言で型を明示する
    JsonDeserializer<String> stringDeserializer =
        new JsonDeserializer<String>() {
          @Override
          public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // 配列・オブジェクトはgetValueAsStringがトークンを消費せずにnullを返し、後続の項目まで
            // 読み飛ばされて欠落するため、中身を読み捨てて当該項目のみnullとする。
            // 生成AIの出力が文字列項目に配列を返しても、解析全体を失敗させないよう例外にはしない。
            if (!p.currentToken().isScalarValue()) {
              p.skipChildren();
              return null;
            }
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
