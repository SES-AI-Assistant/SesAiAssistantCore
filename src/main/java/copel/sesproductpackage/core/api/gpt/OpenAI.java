package copel.sesproductpackage.core.api.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.ApiType;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.core.util.Properties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAIクラス.
 *
 * @author Copel Co., Ltd..
 */
@Slf4j
public class OpenAI implements Transformer {
  /** OpenAI APIのエンベディング処理のエンドポイント. */
  private static final String EMBEDDING_API_URL = Properties.get("OPEN_AI_EMBEDDING_API_URL");

  /** OpenAIのエンベディング処理を実施するモデル名. */
  private static final String EMBEDDING_MODEL = Properties.get("OPEN_AI_EMBEDDING_MODEL");

  /** OpenAIの質問応答APIのエンドポイント. */
  private static final String COMPLETION_API_URL = Properties.get("OPEN_AI_COMPLETION_API_URL");

  /** OpenAIの質問応答を処理するモデル名のデフォルト値. */
  private static final String COMPLETION_MODEL_DEFAULT = "gpt-4o-mini";

  /** OpenAIの質問応答を処理する際のtemperatureパラメータのデフォルト値. */
  private static final Float COMPLETION_TEMPERATURE;

  static {
    String temp = Properties.get("OPEN_AI_COMPLETION_TEMPERATURE");
    COMPLETION_TEMPERATURE = (temp != null && !temp.isEmpty()) ? Float.valueOf(temp) : 0.7f;
  }

  /** OpenAIのAPIキー. */
  private final String apiKey;

  /** OpenAIのモデル. */
  private final String completionModel;

  /**
   * コンストラクタ.
   *
   * @param apiKey APIキー
   */
  public OpenAI(final String apiKey) {
    this.apiKey = apiKey;
    this.completionModel = COMPLETION_MODEL_DEFAULT;
  }

  /**
   * コンストラクタ.
   *
   * @param apiKey APIキー
   * @param completionModel GPTモデル
   */
  public OpenAI(final String apiKey, final String completionModel) {
    this.apiKey = apiKey;
    this.completionModel = completionModel;
  }

  @Override
  public float[] embedding(final String inputString) throws IOException {
    if (inputString == null) {
      return null;
    }

    try {
      return ApiRetryHelper.executeWithRetry(
          () -> {
            try {
              return embeddingInternal(inputString);
            } catch (IOException e) {
              throw new RuntimeException("【OpenAI】エンベディング処理でIO例外が発生しました", e);
            }
          });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw e;
    }
  }

  /**
   * embeddingの内部実装（リトライロジックなし）.
   *
   * <p>503（Service Unavailable）と429（Rate Limit）エラーに対してはexponential backoff リトライが自動実行されます。
   *
   * @param inputString 入力文字列
   * @return 埋め込みベクトル
   * @throws IOException IO例外
   * @throws RuntimeException API例外
   */
  private float[] embeddingInternal(final String inputString) throws IOException, RuntimeException {
    URL url = new URL(EMBEDDING_API_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
    conn.setDoOutput(true);

    log.info("【OpenAI】{}文字のエンベディング処理を実行しました", inputString.length());
    OpenAIEmbeddingRequest embeddingRequest =
        new OpenAIEmbeddingRequest(inputString, EMBEDDING_MODEL, "float", "SesAiAssitantCore");
    String jsonBody = ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(embeddingRequest);
    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    checkResponseCode(conn, responseCode);

    String response = readResponse(conn);
    OpenAIEmbeddingResponse embeddingResponse =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(response, OpenAIEmbeddingResponse.class);

    float[] vectorValue = new float[embeddingResponse.getData().get(0).getEmbedding().size()];
    for (int i = 0; i < embeddingResponse.getData().get(0).getEmbedding().size(); i++) {
      vectorValue[i] = embeddingResponse.getData().get(0).getEmbedding().get(i).floatValue();
    }

    // API使用履歴テーブル（SES_AI_API_USAGE_HISTORY）に履歴を登録
    SES_AI_API_USAGE_HISTORY sesAiApiUsageHistory = new SES_AI_API_USAGE_HISTORY();
    sesAiApiUsageHistory.setProvider(Provider.OpenAI);
    sesAiApiUsageHistory.setModel(EMBEDDING_MODEL);
    OriginalDateTime nowDt = new OriginalDateTime();
    sesAiApiUsageHistory.setUsageMonth(nowDt.getYYYYMM());
    sesAiApiUsageHistory.setUsageDate(nowDt.getYYYYMMDD());
    sesAiApiUsageHistory.setUserId("SesAiAssitantCore");
    sesAiApiUsageHistory.setApiType(ApiType.Embedding);
    sesAiApiUsageHistory.fetch();
    sesAiApiUsageHistory.addInputCount(inputString.length());
    sesAiApiUsageHistory.addOutputCount(0);
    sesAiApiUsageHistory.save();

    return vectorValue;
  }

  @Override
  public GptAnswer generate(final String prompt) throws IOException {
    return this.generate(prompt, COMPLETION_TEMPERATURE);
  }

  /**
   * OpenAIのLLMに回答の生成を実行させその回答を返却します.
   *
   * @param prompt プロンプト
   * @param temperature 温度（回答のばらつき度を示す）
   * @return 回答
   * @throws IOException
   */
  public GptAnswer generate(final String prompt, final Float temperature) throws IOException {
    if (temperature == null || prompt == null) {
      return null;
    }
    URL url = new URL(COMPLETION_API_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
    conn.setDoOutput(true);

    Message userMessage = new Message("user", prompt);
    OpenAIChatCompletionRequest chatRequest =
        new OpenAIChatCompletionRequest(
            this.completionModel, List.of(userMessage), temperature, null);
    String jsonBody = ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(chatRequest);
    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    checkResponseCode(conn, responseCode);

    String response = readResponse(conn);
    OpenAIChatCompletionResponse chatResponse =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(response, OpenAIChatCompletionResponse.class);
    String resultText =
        (chatResponse.getChoices() != null
                && !chatResponse.getChoices().isEmpty()
                && chatResponse.getChoices().get(0).getMessage() != null)
            ? chatResponse.getChoices().get(0).getMessage().getContent()
            : null;

    // API使用履歴テーブル（SES_AI_API_USAGE_HISTORY）に履歴を登録
    SES_AI_API_USAGE_HISTORY sesAiApiUsageHistory = new SES_AI_API_USAGE_HISTORY();
    sesAiApiUsageHistory.setProvider(Provider.OpenAI);
    sesAiApiUsageHistory.setModel(this.completionModel);
    OriginalDateTime nowDtGen = new OriginalDateTime();
    sesAiApiUsageHistory.setUsageMonth(nowDtGen.getYYYYMM());
    sesAiApiUsageHistory.setUsageDate(nowDtGen.getYYYYMMDD());
    sesAiApiUsageHistory.setUserId("SesAiAssitantCore");
    sesAiApiUsageHistory.setApiType(ApiType.Generate);
    sesAiApiUsageHistory.fetch();
    sesAiApiUsageHistory.addInputCount(prompt.length());
    sesAiApiUsageHistory.addOutputCount(resultText != null ? resultText.length() : 0);
    sesAiApiUsageHistory.save();

    return new GptAnswer(resultText, OpenAI.class);
  }

  /**
   * OpenAIにこのオブジェクトがもつcompletionModelに対するファインチューニングをリクエストする.
   *
   * @param trainingData ファインチューニング用データ（文字列形式）
   * @throws IOException
   * @deprecated 現在のシステムでは使用されていません。将来的な実装に備えて保持されています。
   */
  @Deprecated
  public void fineTuning(final String trainingData) throws IOException {
    throw new UnsupportedOperationException("fineTuning は現在のシステムではサポートされていません");
  }

  /** OpenAI API エラー時のレスポンス本文（JSON）を読み取ります。成功時は {@code null} です。 */
  private static String readErrorResponseBody(HttpURLConnection conn) {
    try (InputStream es = conn.getErrorStream()) {
      if (es == null) {
        return null;
      }
      return new String(es.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.warn("【OpenAI】エラーレスポンス本文の読み取りに失敗しました: {}", e.getMessage());
      return null;
    }
  }

  /** ログ・例外用に、要約と OpenAI からの本文を結合します。 */
  private static String formatOpenAiHttpError(String summary, String responseBody) {
    if (responseBody == null || responseBody.isBlank()) {
      return summary;
    }
    return summary + " | OpenAI: " + responseBody;
  }

  /**
   * レスポンスコードをチェックします.
   *
   * @param conn コネクション
   * @param responseCode レスポンスコード
   */
  private void checkResponseCode(HttpURLConnection conn, int responseCode) {
    switch (responseCode) {
      case HttpURLConnection.HTTP_OK:
        break;
      case HttpURLConnection.HTTP_BAD_REQUEST:
        {
          String body = readErrorResponseBody(conn);
          conn.disconnect();
          throw new RuntimeException(
              formatOpenAiHttpError("400 Bad Request: 無効なパラメータ、または不適切なリクエストフォーマットです", body));
        }
      case HttpURLConnection.HTTP_UNAUTHORIZED:
        {
          String body = readErrorResponseBody(conn);
          conn.disconnect();
          throw new RuntimeException(
              formatOpenAiHttpError("401 Unauthorized: APIキーが無効、または提供されていないエラー", body));
        }
      case HttpURLConnection.HTTP_FORBIDDEN:
        {
          String body = readErrorResponseBody(conn);
          conn.disconnect();
          throw new RuntimeException(
              formatOpenAiHttpError("403 Forbidden: アカウントの制限、または対象モデルが利用不可のエラー", body));
        }
      case HttpURLConnection.HTTP_NOT_FOUND:
        {
          String body = readErrorResponseBody(conn);
          conn.disconnect();
          throw new RuntimeException(
              formatOpenAiHttpError("404 Not Found: APIのエンドポイントが間違っている、またはモデル名が無効のエラー", body));
        }
      case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
        {
          String body = readErrorResponseBody(conn);
          conn.disconnect();
          throw new RuntimeException(
              formatOpenAiHttpError("408 Request Timeout: リクエストが時間内に処理されなかったエラー", body));
        }
      case 429:
        {
          String body = readErrorResponseBody(conn);
          conn.disconnect();
          throw new RuntimeException(
              formatOpenAiHttpError(
                  "429 Too Many Requests（レート制限・利用枠・課金など。詳細は OpenAI レスポンス参照）", body));
        }
      case HttpURLConnection.HTTP_INTERNAL_ERROR:
        {
          String body = readErrorResponseBody(conn);
          conn.disconnect();
          throw new RuntimeException(
              formatOpenAiHttpError("500 Internal Server Error: OpenAIのサーバーで問題が発生しました", body));
        }
      case HttpURLConnection.HTTP_UNAVAILABLE:
        {
          String body = readErrorResponseBody(conn);
          conn.disconnect();
          throw new RuntimeException(
              formatOpenAiHttpError(
                  "503 Service Unavailable: OpenAIのサーバーがメンテナンス中、または負荷が高い状態です", body));
        }
      default:
        break;
    }
  }

  /**
   * レスポンスを読み込みます.
   *
   * @param conn コネクション
   * @return レスポンス文字列
   * @throws IOException
   */
  private String readResponse(HttpURLConnection conn) throws IOException {
    StringBuilder response = new StringBuilder();
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        response.append(line);
      }
    } finally {
      conn.disconnect();
    }
    return response.toString();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OpenAIEmbeddingRequest {
    private String input;

    private String model;

    @JsonProperty("encoding_format")
    private String encodingFormat;

    private String user;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class EmbeddingData {
    private List<Double> embedding;

    private Integer index;

    private String object;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OpenAIUsageMetadata {
    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    @JsonProperty("total_tokens")
    private Integer totalTokens;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpenAIEmbeddingResponse {
    private List<EmbeddingData> data;

    private String model;

    private String object;

    private OpenAIUsageMetadata usage;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Message {
    private String role;

    private String content;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OpenAIChatCompletionRequest {
    private String model;

    private List<Message> messages;

    private Float temperature;

    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Choice {
    private Integer index;

    private Message message;

    @JsonProperty("finish_reason")
    private String finishReason;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpenAIChatCompletionResponse {
    private String id;

    private String object;

    private Long created;

    private String model;

    private List<Choice> choices;

    private OpenAIUsageMetadata usage;
  }
}
