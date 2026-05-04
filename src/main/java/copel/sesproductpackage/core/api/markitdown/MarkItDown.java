package copel.sesproductpackage.core.api.markitdown;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import copel.sesproductpackage.core.util.Properties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

/**
 * markitdown-lambda を同期 invoke して Markdown 変換結果を取得するユーティリティ.
 *
 * <p>関数名は環境変数 {@value #ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME}、リージョンは {@value #ENV_AWS_REGION}
 * （未設定時は {@link Region#AP_NORTHEAST_1}）を使用する。
 */
@Slf4j
public final class MarkItDown {

  /** markitdown-lambda の関数名（ARN の最後のセグメントでも可）。 */
  public static final String ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME = "MARKITDOWN_LAMBDA_FUNCTION_NAME";

  private static final String ENV_AWS_REGION = "AWS_REGION";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * UT 専用: {@code true} のとき、リクエストの JSON 化で {@link JsonProcessingException} を投げる（本番では常に {@code false}）。
   */
  static volatile boolean injectJsonProcessingExceptionOnSerializeForTest;

  private MarkItDown() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** UT 専用: テストで設定したフラグをリセットする. */
  static void resetTestHooks() {
    injectJsonProcessingExceptionOnSerializeForTest = false;
  }

  /**
   * {@code MARKITDOWN_LAMBDA_FUNCTION_NAME} で指定した Lambda を同期 invoke し、応答を {@link MarkitdownLambdaResponseEntity} にマッピングする.
   *
   * @param request 内部ペイロード（SPEC 3.1）
   * @return Lambda が返す JSON（SPEC 3.2）
   * @throws IllegalStateException 環境変数未設定、JSON 化失敗、invoke 失敗、応答解析失敗、または Lambda の FunctionError 時
   */
  public static MarkitdownLambdaResponseEntity invoke(final MarkitdownLambdaRequestEntity request) {
    final String functionName = Properties.get(ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME);
    if (functionName == null || functionName.isBlank()) {
      throw new IllegalStateException("プロパティ " + ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME + " が未設定です。");
    }

    final String regionStr = System.getenv(ENV_AWS_REGION);
    final Region region =
        regionStr != null && !regionStr.isBlank() ? Region.of(regionStr) : Region.AP_NORTHEAST_1;

    final String payloadJson;
    try {
      if (injectJsonProcessingExceptionOnSerializeForTest) {
        throw new JsonParseException(null, "injected for test");
      }
      payloadJson = MAPPER.writeValueAsString(request);
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("markitdown-lambda 向けリクエストの JSON 化に失敗しました。", e);
    }

    try (LambdaClient client =
        LambdaClient.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()) {

      final InvokeResponse response =
          client.invoke(
              InvokeRequest.builder()
                  .functionName(functionName)
                  .payload(SdkBytes.fromUtf8String(payloadJson))
                  .build());

      if (response.functionError() != null) {
        final String errBody =
            response.payload() != null ? response.payload().asUtf8String() : "";
        log.error(
            "markitdown-lambda が FunctionError を返しました: type={} body={}",
            response.functionError(),
            errBody);
        throw new IllegalStateException(
            "Lambda 実行エラー (" + response.functionError() + "): " + errBody);
      }

      if (response.payload() == null) {
        throw new IllegalStateException("markitdown-lambda の応答ペイロードが空です。");
      }

      try {
        return MAPPER.readValue(
            response.payload().asUtf8String(), MarkitdownLambdaResponseEntity.class);
      } catch (final JsonProcessingException e) {
        throw new IllegalStateException("markitdown-lambda 応答の JSON 解析に失敗しました。", e);
      }
    } catch (final LambdaException e) {
      log.error("markitdown-lambda の invoke に失敗しました: {}", e.getMessage());
      throw new IllegalStateException("Lambda の呼び出しに失敗しました。", e);
    }
  }

  /**
   * markitdown-lambda を <strong>同期 invoke</strong> するときのイベント（内部ペイロード）に対応する Entity.
   *
   * <p>API Gateway / Function URL 経由の HTTP ラッパーではなく、Lambda に直接渡す JSON と同一形状を想定する。
   * 仕様: markitdown-lambda の SPECIFICATION.md「3.1 リクエスト」。
   *
   * <p>入力は {@code url} / {@code s3} / {@code file_base64} のいずれか（優先順は Lambda 実装に従う）。
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class MarkitdownLambdaRequestEntity {

    /** MarkItDown が HTTP GET する URL（非空なら最優先）。 */
    private String url;

    /** 非公開オブジェクト等をバケット・キーで指定。 */
    private S3ObjectRef s3;

    @JsonProperty("file_base64")
    private String fileBase64;

    /** 元ファイル名。省略可。拡張子があると形式判定に効く。 */
    private String filename;

    /** 内部ペイロードの {@code s3} オブジェクト。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class S3ObjectRef {
      private String bucket;
      private String key;
    }
  }

  /**
   * markitdown-lambda の戻り値（同期 invoke のペイロード）に対応する Entity.
   *
   * <p>業務上の成否は {@link #success} で判別する。失敗時は {@link #error} に詳細が入る。
   * 仕様: markitdown-lambda の SPECIFICATION.md「3.2 レスポンス」。
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class MarkitdownLambdaResponseEntity {

    private boolean success;

    private String markdown;

    private String title;

    private ErrorDetail error;

    /** {@code success == false} のときに設定されるエラー内容。 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
      /** 捕捉した例外の Python クラス名に相当する文字列。 */
      private String type;

      private String message;
    }
  }
}
