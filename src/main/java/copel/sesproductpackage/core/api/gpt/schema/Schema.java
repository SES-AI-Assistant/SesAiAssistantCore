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
 * <pre>{@code
 * public class Sample {
 *     @Schema(
 *         description = "全属性を設定したサンプルフィールド",
 *         required = true,
 *         type = "integer",
 *         itemType = String.class,
 *         minLength = 1,
 *         maxLength = 10,
 *         pattern = "^[0-9]+$",
 *         format = "int32",
 *         gt = 0,
 *         lt = 100,
 *         minDigits = 1,
 *         maxDigits = 3,
 *         minItems = 1,
 *         maxItems = 5,
 *         defaultValue = "50",
 *         example = "10"
 *     )
 *     private int sampleField;
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

  /**
   * 最小文字数（文字列型用）.
   *
   * <p>JSON Schemaの"minLength"プロパティにマッピングされます。
   *
   * @return 最小文字数。デフォルトは-1（未指定）
   */
  int minLength() default -1;

  /**
   * 最大文字数（文字列型用）.
   *
   * <p>JSON Schemaの"maxLength"プロパティにマッピングされます。
   *
   * @return 最大文字数。デフォルトは-1（未指定）
   */
  int maxLength() default -1;

  /**
   * 正規表現パターン（文字列型用）.
   *
   * <p>JSON Schemaの"pattern"プロパティにマッピングされます。
   *
   * @return 正規表現パターン。デフォルトは空文字列（未指定）
   */
  String pattern() default "";

  /**
   * フォーマット（文字列型用）.
   *
   * <p>JSON Schemaの"format"プロパティにマッピングされます。 例：date、time、email、uri等。
   *
   * @return フォーマット。デフォルトは空文字列（未指定）
   */
  String format() default "";

  /**
   * 最小値より大きい（数値型用、排他的最小値）.
   *
   * <p>JSON Schemaの"exclusiveMinimum"プロパティにマッピングされます。
   *
   * @return 最小値。デフォルトはLong.MIN_VALUEで未指定を表す
   */
  long gt() default Long.MIN_VALUE;

  /**
   * 最大値より小さい（数値型用、排他的最大値）.
   *
   * <p>JSON Schemaの"exclusiveMaximum"プロパティにマッピングされます。
   *
   * @return 最大値。デフォルトはLong.MAX_VALUEで未指定を表す
   */
  long lt() default Long.MAX_VALUE;

  /**
   * 最小桁数（数値型用）.
   *
   * <p>JSON Schemaの拡張プロパティとして"minDigits"にマッピングされます。
   *
   * @return 最小桁数。デフォルトは-1（未指定）
   */
  int minDigits() default -1;

  /**
   * 最大桁数（数値型用）.
   *
   * <p>JSON Schemaの拡張プロパティとして"maxDigits"にマッピングされます。
   *
   * @return 最大桁数。デフォルトは-1（未指定）
   */
  int maxDigits() default -1;

  /**
   * 配列の最小要素数.
   *
   * <p>JSON Schemaの"minItems"プロパティにマッピングされます。
   *
   * @return 最小要素数。デフォルトは-1（未指定）
   */
  int minItems() default -1;

  /**
   * 配列の最大要素数.
   *
   * <p>JSON Schemaの"maxItems"プロパティにマッピングされます。
   *
   * @return 最大要素数。デフォルトは-1（未指定）
   */
  int maxItems() default -1;

  /**
   * デフォルト値.
   *
   * <p>JSON Schemaの"default"プロパティにマッピングされます。 文字列表現で指定します。
   *
   * @return デフォルト値。デフォルトは空文字列（未指定）
   */
  String defaultValue() default "";

  /**
   * データ例.
   *
   * <p>JSON Schemaの"example"プロパティにマッピングされます。 文字列表現で指定します。
   *
   * @return データ例。デフォルトは空文字列（未指定）
   */
  String example() default "";
}
