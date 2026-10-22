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
import java.util.Optional;
import java.util.function.Predicate;
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
  /**
   * エンドポイントから HTTP 接続を開く処理.
   *
   * <p>本番では URL から接続を開く。テストでは HTTP の境界をこの関数で差し替え、外部へ接続せずに応答を再現する。 差し替えは UT からのみ行い、本番のコードは既定の関数を使う。
   */
  @FunctionalInterface
  interface ConnectionOpener {
    /**
     * エンドポイントへの接続を開く.
     *
     * @param endpoint 送信先の URL
     * @return HTTP 接続
     * @throws IOException 接続の生成に失敗した場合
     */
    HttpURLConnection open(String endpoint) throws IOException;
  }

  /**
   * HTTP 接続を開く関数（既定は URL から開く）。
   *
   * <p>テストで必要な理由: 実際の HTTP 通信を行わずに、成功・失敗の応答を再現するため。 URL
   * の生成そのものを差し替える方法は、クラスの読み込みまで巻き込んで失敗したため採らなかった。
   */
  static ConnectionOpener connectionOpener = OpenAI::openDefault;

  /**
   * 既定の接続として、URL から HTTP 接続を開きます。
   *
   * <p>接続を開くだけで、この時点では通信を行わない。通信は呼び出し側が接続を使ったときに行う。
   *
   * @param endpoint 送信先の URL
   * @return HTTP 接続
   * @throws IOException 接続を開けなかった場合（URL の形式が不正な場合を含む）
   */
  static HttpURLConnection openDefault(final String endpoint) throws IOException {
    return (HttpURLConnection) new URL(endpoint).openConnection();
  }

  /** OpenAI APIのエンベディング処理のエンドポイント. */
  private static final String EMBEDDING_API_URL = Properties.get("OPEN_AI_EMBEDDING_API_URL");

  /** OpenAIのエンベディング処理を実施するモデル名. */
  private static final String EMBEDDING_MODEL = Properties.get("OPEN_AI_EMBEDDING_MODEL");

  /** OpenAIの質問応答APIのエンドポイント. */
  private static final String COMPLETION_API_URL = Properties.get("OPEN_AI_COMPLETION_API_URL");

  /** OpenAIの質問応答を処理するモデル名のデフォルト値. */
  private static final String COMPLETION_MODEL_DEFAULT = "gpt-4o-mini";

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
    log.info("【OpenAI】{}文字のエンベディング処理を実行しました", inputString.length());
    OpenAiEmbeddingRequest embeddingRequest =
        new OpenAiEmbeddingRequest(inputString, EMBEDDING_MODEL, "float", "SesAiAssitantCore");
    String response = postJson(EMBEDDING_API_URL, embeddingRequest);
    OpenAiEmbeddingResponse embeddingResponse =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(response, OpenAiEmbeddingResponse.class);

    List<EmbeddingData> data = embeddingResponse.getData();
    if (data == null || data.isEmpty()) {
      // 埋め込みの無い応答は、呼び出し元が既に扱う IOException として伝える
      throw new IOException("【OpenAI】エンベディングの応答に data がありません");
    }
    List<Double> values = data.get(0).getEmbedding();
    float[] vectorValue = new float[values.size()];
    for (int i = 0; i < values.size(); i++) {
      vectorValue[i] = values.get(i).floatValue();
    }

    // API使用履歴テーブル（SES_AI_API_USAGE_HISTORY）に履歴を登録
    SES_AI_API_USAGE_HISTORY sesAiApiUsageHistory = new SES_AI_API_USAGE_HISTORY();
    sesAiApiUsageHistory.setProvider(Provider.OpenAI);
    sesAiApiUsageHistory.setModel(EMBEDDING_MODEL);
    OriginalDateTime nowDt = new OriginalDateTime();
    sesAiApiUsageHistory.setUsageMonth(nowDt.getYYYYMM());
    sesAiApiUsageHistory.setUsageDate(nowDt.getYyyyMmDd());
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
    // 温度は使う時点で読む。Properties は未設定のキーに空文字を返すため、空文字は既定値 0.7 として扱う
    String temperatureSetting = Properties.get("OPEN_AI_COMPLETION_TEMPERATURE");
    Float temperature =
        Optional.of(temperatureSetting)
            .filter(Predicate.not(String::isEmpty))
            .map(Float::parseFloat)
            .orElse(0.7f);
    return this.generate(prompt, temperature);
  }

  /**
   * OpenAIのLLMに回答の生成を実行させその回答を返却します.
   *
   * @param prompt プロンプト
   * @param temperature 温度（回答のばらつき度を示す）
   * @return 回答
   * @throws IOException 入出力（ファイル・ストリーム）の操作に失敗した場合
   */
  public GptAnswer generate(final String prompt, final Float temperature) throws IOException {
    if (temperature == null || prompt == null) {
      return null;
    }
    Message userMessage = new Message("user", prompt);
    OpenAiChatCompletionRequest chatRequest =
        new OpenAiChatCompletionRequest(
            this.completionModel, List.of(userMessage), temperature, null);
    String response = postJson(COMPLETION_API_URL, chatRequest);
    OpenAiChatCompletionResponse chatResponse =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(response, OpenAiChatCompletionResponse.class);
    String resultText =
        chatResponse.getChoices() != null
                && !chatResponse.getChoices().isEmpty()
                && chatResponse.getChoices().get(0).getMessage() != null
            ? chatResponse.getChoices().get(0).getMessage().getContent()
            : null;

    // API使用履歴テーブル（SES_AI_API_USAGE_HISTORY）に履歴を登録
    SES_AI_API_USAGE_HISTORY sesAiApiUsageHistory = new SES_AI_API_USAGE_HISTORY();
    sesAiApiUsageHistory.setProvider(Provider.OpenAI);
    sesAiApiUsageHistory.setModel(this.completionModel);
    OriginalDateTime nowDtGen = new OriginalDateTime();
    sesAiApiUsageHistory.setUsageMonth(nowDtGen.getYYYYMM());
    sesAiApiUsageHistory.setUsageDate(nowDtGen.getYyyyMmDd());
    sesAiApiUsageHistory.setUserId("SesAiAssitantCore");
    sesAiApiUsageHistory.setApiType(ApiType.Generate);
    sesAiApiUsageHistory.fetch();
    sesAiApiUsageHistory.addInputCount(prompt.length());
    sesAiApiUsageHistory.addOutputCount(resultText != null ? resultText.length() : 0);
    sesAiApiUsageHistory.save();

    return new GptAnswer(resultText, OpenAI.class);
  }

  /**
   * OpenAI のエンドポイントへ JSON を POST し、応答本文を返します.
   *
   * <p>接続の設定、送信、応答コードの確認、本文の読み取りを行う。embedding と質問応答の両方で同じ手順のため、ここにまとめる。
   * 正常応答（200）と想定外のコードは何もしない。上記以外のエラー応答は、要約と OpenAI の本文を含む {@link
   * ApiRetryHelper.HttpStatusException} を投げる（再試行の判定は状態コードで行う）。
   *
   * @param endpoint 送信先の URL
   * @param requestBody JSON に変換して送信する値
   * @return 応答本文
   * @throws IOException 入出力（ファイル・ストリーム）の操作に失敗した場合
   */
  private String postJson(final String endpoint, final Object requestBody) throws IOException {
    HttpURLConnection conn = connectionOpener.open(endpoint);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
    conn.setDoOutput(true);

    String jsonBody = ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(requestBody);
    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    String summary;
    switch (responseCode) {
      case HttpURLConnection.HTTP_BAD_REQUEST -> summary =
          "400 Bad Request: 無効なパラメータ、または不適切なリクエストフォーマットです";
      case HttpURLConnection.HTTP_UNAUTHORIZED -> summary =
          "401 Unauthorized: APIキーが無効、または提供されていないエラー";
      case HttpURLConnection.HTTP_FORBIDDEN -> summary =
          "403 Forbidden: アカウントの制限、または対象モデルが利用不可のエラー";
      case HttpURLConnection.HTTP_NOT_FOUND -> summary =
          "404 Not Found: APIのエンドポイントが間違っている、またはモデル名が無効のエラー";
      case HttpURLConnection.HTTP_CLIENT_TIMEOUT -> summary =
          "408 Request Timeout: リクエストが時間内に処理されなかったエラー";
      case 429 -> summary = "429 Too Many Requests（レート制限・利用枠・課金など。詳細は OpenAI レスポンス参照）";
      case HttpURLConnection.HTTP_INTERNAL_ERROR -> summary =
          "500 Internal Server Error: OpenAIのサーバーで問題が発生しました";
      case HttpURLConnection.HTTP_UNAVAILABLE -> summary =
          "503 Service Unavailable: OpenAIのサーバーがメンテナンス中、または負荷が高い状態です";
      default -> summary = null;
    }
    if (summary != null) {
      // エラー本文は読めなくても例外は投げる。本文が空の場合は要約だけを残す
      String body = null;
      try (InputStream es = conn.getErrorStream()) {
        if (es != null) {
          body = new String(es.readAllBytes(), StandardCharsets.UTF_8);
        }
      } catch (IOException e) {
        log.warn("【OpenAI】エラーレスポンス本文の読み取りに失敗しました: {}", e.getMessage());
      }
      conn.disconnect();
      throw new ApiRetryHelper.HttpStatusException(
          responseCode, body == null || body.isBlank() ? summary : summary + " | OpenAI: " + body);
    }

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

  /** 埋め込み（Embedding）APIへのリクエストを表します。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OpenAiEmbeddingRequest {
    private String input;

    private String model;

    @JsonProperty("encoding_format")
    private String encodingFormat;

    private String user;
  }

  /** 埋め込みAPIの応答に含まれる1件分のデータを表します。 */
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

  /** API の利用量（トークン数）を表します。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OpenAiUsageMetadata {
    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    @JsonProperty("total_tokens")
    private Integer totalTokens;
  }

  /** 埋め込み（Embedding）APIからの応答を表します。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpenAiEmbeddingResponse {
    private List<EmbeddingData> data;

    private String model;

    private String object;

    private OpenAiUsageMetadata usage;
  }

  /** チャット補完APIのメッセージ（役割と内容）を表します。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Message {
    private String role;

    private String content;
  }

  /** チャット補完APIへのリクエストを表します。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OpenAiChatCompletionRequest {
    private String model;

    private List<Message> messages;

    private Float temperature;

    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;
  }

  /** チャット補完APIの応答に含まれる選択肢1件を表します。 */
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

  /** チャット補完APIからの応答を表します。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OpenAiChatCompletionResponse {
    private String id;

    private String object;

    private Long created;

    private String model;

    private List<Choice> choices;

    private OpenAiUsageMetadata usage;
  }
}
