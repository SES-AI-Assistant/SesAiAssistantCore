package copel.sesproductpackage.core.api.gpt.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件付きスキーマの1つのケースを定義するアノテーション.
 *
 * <p>@ConditionalSchemaと組み合わせて使用し、特定のEnum値に対応するスキーマを指定します。
 *
 * <p>使用例：
 *
 * <pre>{@code
 * @ConditionalSchema({
 *   @ConditionalCase(enumValue = "SUCCESS", schema = SuccessPayload.class),
 *   @ConditionalCase(enumValue = "ERROR", schema = ErrorPayload.class)
 * })
 * private Object payload;
 * }</pre>
 *
 * @author Copel Co., Ltd.
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalCase {

  /**
   * マッチ対象のEnum値.
   *
   * <p>このEnum値に合致する場合、指定されたスキーマクラスが使用されます。
   *
   * @return enum値の文字列表現
   */
  String enumValue();

  /**
   * このEnum値に対応するスキーマクラス.
   *
   * <p>条件マッチ時に使用されるクラスです。SchemaGeneratorによって JSON Schemaに変換されます。
   *
   * @return スキーマとして機能するクラス
   */
  Class<?> schema();

  /**
   * スキーマのタイトル（オプション）.
   *
   * <p>生成されるJSON Schemaの"title"プロパティに使用されます。 空の場合はクラス名が使用されます。
   *
   * @return スキーマのタイトル。デフォルトは空文字列
   */
  String title() default "";

  /**
   * スキーマの説明（オプション）.
   *
   * <p>生成されるJSON Schemaの"description"プロパティに使用されます。
   *
   * @return スキーマの説明。デフォルトは空文字列
   */
  String description() default "";

  /**
   * このEnum値の場合、当該フィールドを出力スキーマから除外するか（オプション）.
   *
   * <p>この属性で指定したEnum値が実際に出現した場合、 @ConditionalSchemaで修飾されたフィールド全体をスキーマから除外します。
   *
   * @return 除外対象のEnum値。デフォルトは空文字列（除外なし）
   */
  String excludeOn() default "";
}
