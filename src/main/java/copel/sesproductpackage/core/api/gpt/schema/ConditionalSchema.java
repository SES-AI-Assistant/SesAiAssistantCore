package copel.sesproductpackage.core.api.gpt.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enum値に応じてスキーマを分岐させるアノテーション.
 *
 * <p>このアノテーションが付与されたフィールドのスキーマは、複数の条件分岐オプションから なるanyOf構造として生成されます。Geminiの構造化出力の"Content
 * Moderation"パターンに対応しています。
 *
 * <p>使用例：
 *
 * <pre>{@code
 * class ApiResponse {
 *   @Schema(description = "処理結果タイプ")
 *   private ResultType resultType;
 *
 *   @ConditionalSchema({
 *     @ConditionalCase(enumValue = "SUCCESS", schema = SuccessPayload.class),
 *     @ConditionalCase(enumValue = "ERROR", schema = ErrorPayload.class)
 *   })
 *   @Schema(description = "結果ペイロード")
 *   private Object payload;
 * }
 *
 * // 特定のEnum値でフィールドを除外する例
 * class SummaryInfo {
 *   @Schema(description = "情報種別")
 *   private InfoType type;
 *
 *   @ConditionalSchema({
 *     @ConditionalCase(enumValue = "PERSON", schema = PersonInfo.class),
 *     @ConditionalCase(enumValue = "JOB", schema = JobInfo.class),
 *     @ConditionalCase(enumValue = "Unknown", excludeOn = "Unknown")
 *   })
 *   @Schema(description = "詳細情報")
 *   private Object details;
 * }
 *
 * class SuccessPayload {
 *   @Schema(description = "処理結果データ")
 *   private String data;
 * }
 *
 * class ErrorPayload {
 *   @Schema(description = "エラーメッセージ")
 *   private String errorMessage;
 *
 *   @Schema(description = "エラーコード")
 *   private int errorCode;
 * }
 * }</pre>
 *
 * <p>生成されるJSON Schema（概要）:
 *
 * <pre>{@code
 * {
 *   "payload": {
 *     "anyOf": [
 *       {
 *         "title": "SuccessPayload",
 *         "type": "object",
 *         "properties": { ... }
 *       },
 *       {
 *         "title": "ErrorPayload",
 *         "type": "object",
 *         "properties": { ... }
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 *
 * @author Copel Co., Ltd.
 * @see ConditionalCase
 * @see SchemaGenerator
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalSchema {

  /**
   * 条件付きスキーマのケース群.
   *
   * <p>複数の@ConditionalCaseを指定し、各Enum値に対応するスキーマを定義します。 SchemaGeneratorが自動的にanyOf構造のJSON
   * Schemaに変換します。
   *
   * @return 条件付きケースの配列
   */
  ConditionalCase[] value();
}
