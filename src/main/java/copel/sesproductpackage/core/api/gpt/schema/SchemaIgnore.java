package copel.sesproductpackage.core.api.gpt.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON Schemaの自動生成からフィールドを除外するアノテーション.
 *
 * <p>このアノテーションが付与されたフィールドは、SchemaGeneratorによるJSON Schemaの生成時に
 * スキーマから除外されます。内部的なフィールドや外部APIに不要なフィールドを隠したい場合に使用します。
 *
 * <p>使用例：
 *
 * <pre>{@code
 * class PersonResponse {
 *   @Schema(description = "人物の名前", required = true)
 *   private String name;
 *
 *   @SchemaIgnore
 *   private String internalId;
 * }
 * }</pre>
 *
 * @author Copel Co., Ltd.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaIgnore {}
