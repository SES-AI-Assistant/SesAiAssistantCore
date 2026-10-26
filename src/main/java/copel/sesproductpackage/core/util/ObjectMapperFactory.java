package copel.sesproductpackage.core.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Optional;

/** Core共通で使用するObjectMapper */
public class ObjectMapperFactory {
  /** インスタンス化を禁止する。すべての操作は静的メンバーで提供する。 */
  private ObjectMapperFactory() {}

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
    // 種別が Unknown の場合、生成AIは種別に紐づく詳細項目（summary）を出力しない。
    // 外部の型ID項目が無いだけで解析全体を失敗させず、当該項目をnullとして読み込む。
    objectMapper.disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
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
            // getValueAsString が null を返す場合（JSON の null トークン）と、前後の空白を除いた値が "null"（大文字小文字を問わない）の場合は
            // null とする
            return Optional.ofNullable(p.getValueAsString())
                .map(String::trim)
                .filter(value -> !"null".equalsIgnoreCase(value))
                .orElse(null);
          }
        };
    module.addDeserializer(String.class, stringDeserializer);
    objectMapper.registerModule(module);
    return objectMapper;
  }
}
