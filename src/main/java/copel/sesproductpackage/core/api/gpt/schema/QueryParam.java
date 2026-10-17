package copel.sesproductpackage.core.api.gpt.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * このフィールドがrequestBodyではなくクエリパラメータであることを示すマーカーアノテーション.
 *
 * <p>OpenAPI生成時、本アノテーションが付与されたフィールドは requestBody のプロパティとしてではなく、 GET/DELETE等のクエリパラメータ（{@code in:
 * query}）として出力される。フィールド自体の型・説明・例は 通常どおり {@link Schema} アノテーションで指定する（{@code @SchemaIgnore}
 * と併用してもよい）。
 *
 * <p>使用例：
 *
 * <pre>{@code
 * class SomeRequestEntity {
 *   @QueryParam
 *   @Schema(description = "取得対象年月（省略時は当月）", example = "202602")
 *   private String targetUsageMonth;
 * }
 * }</pre>
 *
 * @author Copel Co., Ltd.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParam {}
