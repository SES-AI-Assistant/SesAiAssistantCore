package copel.sesproductpackage.core.api.gpt.schema;

import java.util.Map;

/**
 * JSONスキーマを提供するインターフェース.
 *
 * <p>Gemini APIの構造化出力機能を利用する際、応答型がこのインターフェースを実装することで、 型に対応するJSONスキーマを提供します。
 *
 * <p>スキーマ定義方法：
 *
 * <ul>
 *   <li><strong>自動生成（推奨）</strong>: SchemaGeneratorを使用してリフレクションから自動生成
 *   <li><strong>手動定義</strong>: Map形式で直接定義
 * </ul>
 *
 * @author Copel Co., Ltd.
 */
public interface JsonSchemaProvider {

  /**
   * このクラスの応答型に対応するJSONスキーマをMap形式で返します.
   *
   * <p>スキーマはGemini APIの構造化出力に直接渡されるため、 <a href="https://json-schema.org/">JSON
   * Schema</a>仕様に準拠している必要があります。
   *
   * <p>例：
   *
   * <pre>{@code
   * class PersonResponse implements JsonSchemaProvider {
   *   @Schema(description = "人物の名前", required = true)
   *   private String name;
   *
   *   @Schema(description = "人物の年齢", required = true)
   *   private int age;
   *
   *   @Override
   *   public Map<String, Object> getJsonSchema() {
   *     return SchemaGenerator.generate(this.getClass());
   *   }
   * }
   * }</pre>
   *
   * @return JSON SchemaをMap形式で返すMap。nullを返してはいけません。
   */
  Map<String, Object> getJsonSchema();
}
