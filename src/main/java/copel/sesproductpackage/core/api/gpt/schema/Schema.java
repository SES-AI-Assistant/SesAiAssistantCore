package copel.sesproductpackage.core.api.gpt.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON Schemaのメタデータを定義するアノテーション.
 *
 * <p>SchemaGeneratorでフィールドのJSON Schemaを生成する際に使用します。 @JsonPropertyと併用する場合、本アノテーションの設定が優先されます。
 *
 * <p>使用例：
 *
 * <pre>{@code
 * class PersonResponse {
 *   @Schema(description = "人物の名前", required = true)
 *   private String name;
 *
 *   @Schema(description = "人物の年齢", required = true)
 *   private int age;
 * }
 * }</pre>
 *
 * @author Copel Co., Ltd.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Schema {

  /**
   * フィールドの説明文.
   *
   * <p>JSON Schemaの"description"プロパティにマッピングされます。
   *
   * @return フィールドの説明文。デフォルトは空文字列
   */
  String description() default "";

  /**
   * フィールドが必須かどうか.
   *
   * <p>trueの場合、スキーマの"required"配列に含まれます。
   *
   * @return 必須の場合true。デフォルトはfalse
   */
  boolean required() default false;

  /**
   * JSON Schemaにおけるフィールドの型.
   *
   * <p>指定した場合、SchemaGeneratorの型推測を上書きします。 通常は指定不要です。複雑な型を使用する際に指定してください。
   *
   * <p>有効な値：
   *
   * <ul>
   *   <li>"string" - 文字列型
   *   <li>"integer" - 整数型
   *   <li>"number" - 数値型
   *   <li>"boolean" - 真偽値型
   *   <li>"array" - 配列型
   *   <li>"object" - オブジェクト型
   * </ul>
   *
   * @return JSON Schemaの型。デフォルトは空文字列（自動推測）
   */
  String type() default "";

  /**
   * 配列要素の型.
   *
   * <p>List等の配列型フィールドに対して、要素型を指定します。 指定された場合、JSON Schemaのitemsプロパティにこの型の情報が含まれます。
   *
   * <p>使用例：
   *
   * <pre>{@code
   * @Schema(description = "スキル一覧", itemType = String.class)
   * private List<String> skills;
   *
   * @Schema(description = "ユーザー一覧", itemType = User.class)
   * private List<User> users;
   * }</pre>
   *
   * @return 配列要素の型。デフォルトはObject.class（type指定なし）
   */
  Class<?> itemType() default Object.class;
}
