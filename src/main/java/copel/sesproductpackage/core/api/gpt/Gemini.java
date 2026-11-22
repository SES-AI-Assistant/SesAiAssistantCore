package copel.sesproductpackage.core.api.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import copel.sesproductpackage.core.api.gpt.decision.ChoiceAnswer;
import copel.sesproductpackage.core.api.gpt.decision.ChoiceQuestion;
import copel.sesproductpackage.core.api.gpt.decision.DecisionAnswer;
import copel.sesproductpackage.core.api.gpt.decision.DecisionBlockedException;
import copel.sesproductpackage.core.api.gpt.decision.DecisionQuestion;
import copel.sesproductpackage.core.api.gpt.decision.DecisionResult;
import copel.sesproductpackage.core.api.gpt.decision.NoulAnswer;
import copel.sesproductpackage.core.api.gpt.decision.NoulQuestion;
import copel.sesproductpackage.core.api.gpt.schema.SchemaGenerator;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.core.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Geminiクラス. Google Genai SDKを使用してGemini APIと通信する実装.
 *
 * @author Copel Co., Ltd.
 */
@Data
@Slf4j
public class Gemini implements Transformer {

  /**
   * SDK の Models を作る関数（既定は本来の SDK の Client から取り出す）。
   *
   * <p>テストで必要な理由: 実際の API を呼ばずに応答を再現するため。SDK の Client は内部のフィールドを
   * 反射で差し替えるしかなかったため、この差し替え口を設けた。本番のコードは既定の関数を使う。
   */
  @FunctionalInterface
  interface ModelsFactory {
    /**
     * APIキーから Models を作る.
     *
     * @param apiKey APIキー
     * @return Models
     */
    Models create(String apiKey);
  }

  /** Models を作る関数（既定は本来の SDK）。テストでのみ差し替える。 */
  static ModelsFactory modelsFactory = apiKey -> Client.builder().apiKey(apiKey).build().models;

  /**
   * 判定 API への HTTP 接続を開く関数（既定は URL から開く）.
   *
   * <p>テストで必要な理由: 実際の HTTP 通信を行わずに、成功・失敗の応答を再現するため。OpenAI の接続の差し替え口と同じ流儀にそろえる。
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

  /** HTTP 接続を開く関数（既定は URL から開く）。テストでのみ差し替える。 */
  static ConnectionOpener connectionOpener = Gemini::openDefault;

  /**
   * 既定の接続として、URL から HTTP 接続を開きます.
   *
   * <p>接続を開くだけで、この時点では通信を行わない。通信は呼び出し側が接続を使ったときに行う。
   *
   * @param endpoint 送信先の URL
   * @return HTTP 接続
   * @throws IOException 接続を開けなかった場合（URL の形式が不正な場合を含む）
   */
  static HttpURLConnection openDefault(final String endpoint) throws IOException {
    return (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
  }

  /** 判定 API のエンドポイントを保持する設定プロパティの名前。値は呼び出しのたびに読む。 */
  private static final String DECISION_API_URL_PROPERTY = "GEMINI_DECISION_API_URL";

  /** 1回の判定で扱える質問数の上限。API の制約（1,000問以内）に合わせる。 */
  private static final int MAX_DECISION_QUESTIONS = 1000;

  /**
   * 安全性のフィルタによるブロックを表す error.code の値。
   *
   * <p>この値の 400 応答だけを {@link DecisionBlockedException} にする。他の 400 は通常の例外とし、再試行もしない。
   */
  private static final Set<String> BLOCKED_ERROR_CODES =
      Set.of("safety", "prohibited_content", "blocklist", "image_safety", "content_blocked");

  /** 生成APIのデフォルトGPTモデル名. */
  private static final String COMPLETION_MODEL_DEFAULT =
      GeminiModel.GEMINI_2_5_FLASH_LITE.getModelName();

  /** エンベディングAPIのデフォルトモデル名. */
  private static final String EMBEDDING_MODEL_DEFAULT =
      GeminiModel.GEMINI_EMBEDDING_001.getModelName();

  /** APIキー. */
  private final String apiKey;

  /** GPTモデル. */
  private final String completionModel;

  /** 埋め込みモデル. */
  private final String embeddingModel;

  /**
   * コンストラクタ.
   *
   * @param apiKey APIキー
   */
  public Gemini(final String apiKey) {
    this.apiKey = apiKey;
    this.completionModel = COMPLETION_MODEL_DEFAULT;
    this.embeddingModel = EMBEDDING_MODEL_DEFAULT;
  }

  /**
   * コンストラクタ.
   *
   * @param apiKey APIキー
   * @param completionModel GPTモデル
   */
  public Gemini(final String apiKey, final String completionModel) {
    this.apiKey = apiKey;
    this.completionModel = completionModel;
    this.embeddingModel = EMBEDDING_MODEL_DEFAULT;
  }

  /**
   * コンストラクタ.
   *
   * @param apiKey APIキー
   * @param geminiModel Geminiモデル
   */
  public Gemini(final String apiKey, final GeminiModel geminiModel) {
    this.apiKey = apiKey;
    this.completionModel = geminiModel.getModelName();
    this.embeddingModel = EMBEDDING_MODEL_DEFAULT;
  }

  @Override
  public float[] embedding(final String inputString) throws IOException, RuntimeException {
    if (inputString == null || inputString.isBlank()) {
      return null;
    }

    try {
      Models models = modelsFactory.create(this.apiKey);
      EmbedContentResponse response = models.embedContent(this.embeddingModel, inputString, null);

      Optional<List<ContentEmbedding>> embeddingsOpt = response.embeddings();
      if (embeddingsOpt.isPresent()) {
        List<ContentEmbedding> embeddingsList = embeddingsOpt.get();
        if (!embeddingsList.isEmpty()) {
          ContentEmbedding firstEmbedding = embeddingsList.get(0);
          Optional<List<Float>> valuesOpt = firstEmbedding.values();
          if (valuesOpt.isPresent()) {
            List<Float> values = valuesOpt.get();
            float[] embeddings = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
              embeddings[i] = values.get(i);
            }

            recordApiUsage(
                this.completionModel,
                inputString.length(),
                0,
                SES_AI_API_USAGE_HISTORY.ApiType.Embedding);
            return embeddings;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Embedding API Error: " + e.getMessage(), e);
    }

    return null;
  }

  @Override
  public GptAnswer generate(final String prompt) throws IOException, RuntimeException {
    if (prompt == null || prompt.isBlank()) {
      return null;
    }

    try {
      Models models = modelsFactory.create(this.apiKey);
      GenerateContentResponse response = models.generateContent(this.completionModel, prompt, null);

      if (response != null) {
        String resultText = response.text();
        recordApiUsage(
            this.completionModel,
            prompt.length(),
            resultText != null ? resultText.length() : 0,
            SES_AI_API_USAGE_HISTORY.ApiType.Generate);

        String callerInfo = callerInfo();
        log.info(
            "【Gemini API】呼び出し元: {}, モデル: {}, 入力: {} 文字, 出力: {} 文字",
            callerInfo,
            this.completionModel,
            prompt.length(),
            resultText != null ? resultText.length() : 0);
        log.info("【Gemini API】出力結果: {}", resultText);
        return new GptAnswer(resultText, Gemini.class);
      }
      return new GptAnswer(null, Gemini.class);
    } catch (Exception e) {
      throw new RuntimeException("Generate API Error: " + e.getMessage(), e);
    }
  }

  @Override
  public <T> T generate(final String prompt, final Class<T> responseType)
      throws IOException, RuntimeException {
    return generateStructured(prompt, responseType, null);
  }

  /**
   * LLM APIに対して、温度を指定して構造化出力を生成させます.
   *
   * <p>温度を下げると、同じ入力に対する出力のばらつきが小さくなる。スキルシートの要約のように、同じ本文から同じ結果を得たい場合に使う。 温度を指定しない {@link
   * #generate(String, Class)} の応答には影響しない。
   *
   * @param <T> 応答型
   * @param prompt LLM APIに送信するプロンプト
   * @param responseType 応答型を指定する {@code Class<T>}
   * @param temperature 温度（0で出力のばらつきを最小にする）
   * @return プロンプトに対する応答をresponseType型のインスタンスとして返します
   * @throws IOException ファイルI/Oエラーが発生した場合
   * @throws RuntimeException APIエラー、スキーマ不正、マッピングエラーが発生した場合
   */
  public <T> T generate(final String prompt, final Class<T> responseType, final float temperature)
      throws IOException, RuntimeException {
    return generateStructured(prompt, responseType, temperature);
  }

  /**
   * LLM APIに対して構造化出力を生成させ、応答を指定の型に読み込みます.
   *
   * <p>温度は指定された場合だけ要求に含める。指定しない場合は要求から温度を外し、SDK の既定値をそのまま使う。
   *
   * @param <T> 応答型
   * @param prompt LLM APIに送信するプロンプト
   * @param responseType 応答型を指定する {@code Class<T>}
   * @param temperature 温度。null の場合は要求に含めない
   * @return プロンプトに対する応答をresponseType型のインスタンスとして返します
   * @throws IOException ファイルI/Oエラーが発生した場合
   * @throws RuntimeException APIエラー、スキーマ不正、マッピングエラーが発生した場合
   */
  private <T> T generateStructured(
      final String prompt, final Class<T> responseType, final Float temperature)
      throws IOException, RuntimeException {
    if (prompt == null || prompt.isBlank()) {
      throw new RuntimeException("Prompt must not be blank");
    }
    if (responseType == null) {
      throw new RuntimeException("Response type must not be null");
    }

    try {
      // SchemaGenerator は型ごとに必ず非空のスキーマを返すため、空の確認は行わない
      Map<String, Object> jsonSchema = SchemaGenerator.generate(responseType);

      GenerateContentConfig.Builder configBuilder =
          GenerateContentConfig.builder()
              .responseMimeType("application/json")
              .responseJsonSchema(jsonSchema);
      if (temperature != null) {
        configBuilder = configBuilder.temperature(temperature);
      }
      GenerateContentConfig config = configBuilder.build();

      Models models = modelsFactory.create(this.apiKey);
      GenerateContentResponse response =
          models.generateContent(this.completionModel, prompt, config);

      if (response != null) {
        String resultJson = response.text();
        if (resultJson == null || resultJson.isBlank()) {
          throw new RuntimeException("API returned empty response");
        }

        final T result = ObjectMapperFactory.OBJECT_MAPPER.readValue(resultJson, responseType);

        recordApiUsage(
            this.completionModel,
            prompt.length(),
            resultJson.length(),
            SES_AI_API_USAGE_HISTORY.ApiType.Generate);

        String callerInfo = callerInfo();
        log.info(
            "【Gemini API (Structured)】呼び出し元: {}, モデル: {}, 型: {}, 入力: {} 文字, 出力: {} 文字",
            callerInfo,
            this.completionModel,
            responseType.getSimpleName(),
            prompt.length(),
            resultJson.length());
        log.info("【Gemini API (Structured)】出力結果: {}", resultJson);

        return result;
      }

      throw new RuntimeException("API returned null response");
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Structured Generate API Error: " + e.getMessage(), e);
    }
  }

  /**
   * 共有の入力と質問を判定 API に送り、質問ごとの回答を型付きで返します.
   *
   * <p>モデルは {@code completionModel} に関係なく、判定用の {@link GeminiModel#GEMINI_DECISION_1} を使う。 送信は REST
   * で直接行い、429 と 503 だけを {@link ApiRetryHelper#executeWithRetry} で再試行する（総試行3回）。 安全性のフィルタによるブロックは
   * {@link DecisionBlockedException} にし、再試行しない。 送信先は設定プロパティ GEMINI_DECISION_API_URL を、呼び出しのたびに読む。
   *
   * @param input 全質問で共有する入力
   * @param questions 質問の一覧。1問以上1,000問以下で、質問キーは重複させない
   * @return 質問キーごとの回答を持つ判定結果
   * @throws IOException 入出力の操作に失敗した場合
   * @throws RuntimeException 判定 API のエラー、安全性のフィルタによるブロック、応答の形式が想定外の場合
   */
  @Override
  public DecisionResult decide(
      final String input, final List<? extends DecisionQuestion<?>> questions)
      throws IOException, RuntimeException {
    if (questions == null || questions.isEmpty() || questions.size() > MAX_DECISION_QUESTIONS) {
      throw new IllegalArgumentException("質問は1問以上1,000問以下で指定してください");
    }
    long distinctKeyCount = questions.stream().map(DecisionQuestion::getKey).distinct().count();
    if (distinctKeyCount != questions.size()) {
      throw new IllegalArgumentException("質問キーが重複しています");
    }
    // 設定は使う時点で読む（テストや設定の変更を次の呼び出しから反映するため）
    String endpoint = Properties.get(DECISION_API_URL_PROPERTY);
    if (endpoint.isBlank()) {
      throw new IllegalStateException(DECISION_API_URL_PROPERTY + " が未設定のため、判定を実行できません");
    }

    String model = GeminiModel.GEMINI_DECISION_1.getModelName();
    // 入力文字数は共有の入力と、各質問の指示文・判断基準の合計（API の課金の見積もりと同じ数え方）
    Map<String, DecisionQuestionBody> questionBodies = new LinkedHashMap<>();
    int inputCount = input.length();
    for (DecisionQuestion<?> question : questions) {
      questionBodies.put(question.getKey(), DecisionQuestionBody.of(question));
      inputCount += question.textLength();
    }
    DecisionCall call =
        new DecisionCall(endpoint, this.apiKey, new DecisionRequest(model, input, questionBodies));

    // 再試行は ApiRetryHelper に任せる。IO例外は再試行の対象外のため、一度 unchecked にして戻す
    String responseBody;
    try {
      responseBody =
          ApiRetryHelper.executeWithRetry(
              () -> {
                try {
                  return call.send();
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }

    DecisionResponse response =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(responseBody, DecisionResponse.class);
    Map<String, DecisionAnswerBody> answerBodies = response.getAnswers();
    if (answerBodies == null) {
      throw new IllegalStateException("判定結果に answers がありません");
    }
    Map<String, DecisionAnswer> answers = new LinkedHashMap<>();
    for (DecisionQuestion<?> question : questions) {
      DecisionAnswerBody body = answerBodies.get(question.getKey());
      if (body == null) {
        throw new IllegalStateException("判定結果に質問キーの回答がありません: " + question.getKey());
      }
      answers.put(question.getKey(), body.toAnswer(question));
    }

    recordApiUsage(model, inputCount, 0, SES_AI_API_USAGE_HISTORY.ApiType.Decision);
    // 呼び出し元の特定は callerInfo のスタック位置に依存するため、この処理の中で直接呼ぶ
    String callerInfo = callerInfo();
    log.info("【Gemini API (Decision)】呼び出し元: {}, モデル: {}, 入力: {} 文字", callerInfo, model, inputCount);
    log.info("【Gemini API (Decision)】回答全文: {}", responseBody);
    return new DecisionResult(answers);
  }

  /**
   * 呼び出し元のクラス名とメソッド名を、ログ用の文字列にして返します.
   *
   * <p>スタックの先頭から、この処理・生成処理・呼び出し元の順に並ぶため、呼び出し元は index 3 になる。
   *
   * @return 「クラス名.メソッド名」の形式の文字列
   */
  private static String callerInfo() {
    StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
    String className = caller.getClassName();
    return className.substring(className.lastIndexOf('.') + 1) + "." + caller.getMethodName();
  }

  /**
   * API使用履歴をDBに記録します.
   *
   * <p>モデル名は呼び出しごとに渡す。質問応答の既定のモデルと、判定用のモデルは別の名前で記録するため。
   *
   * @param model 使用したモデル名
   * @param inputCount 入力文字数
   * @param outputCount 出力文字数
   * @param apiType API種別
   */
  private void recordApiUsage(
      final String model,
      final int inputCount,
      final int outputCount,
      final SES_AI_API_USAGE_HISTORY.ApiType apiType) {
    try {
      new SES_AI_API_USAGE_HISTORY()
          .recordUsage(
              SES_AI_API_USAGE_HISTORY.Provider.Google, model, apiType, inputCount, outputCount);
    } catch (Exception e) {
      log.warn("Failed to record API usage history", e);
    }
  }

  /**
   * 判定 API への1回分の送信（送信と応答の確認）を表します.
   *
   * <p>再試行は呼び出し側の {@link ApiRetryHelper#executeWithRetry} が行うため、この処理は1回の送信だけを行う。 応答が 200
   * 以外の場合は状態コードを持つ例外にし、再試行の判定に使う。 安全性のフィルタによるブロックだけは、再試行しない {@link DecisionBlockedException} にする。
   */
  private static final class DecisionCall {

    /** 判定 API のエンドポイント. */
    private final String endpoint;

    /** API キー。ログには出さない。 */
    private final String apiKey;

    /** 送信するリクエスト本体. */
    private final DecisionRequest request;

    /**
     * コンストラクタ.
     *
     * @param endpoint 判定 API のエンドポイント
     * @param apiKey API キー
     * @param request 送信するリクエスト本体
     */
    DecisionCall(final String endpoint, final String apiKey, final DecisionRequest request) {
      this.endpoint = endpoint;
      this.apiKey = apiKey;
      this.request = request;
    }

    /**
     * 判定 API へ1回送信し、応答本文を返します.
     *
     * <p>エラー本文が読めない場合や JSON でない場合も、状態コードを含む例外にする。 400 のうち error.code が安全性のフィルタの値の場合だけ {@link
     * DecisionBlockedException} にする。
     *
     * <p>接続と読み取りには {@link HttpTimeout#DEFAULT} を設定する。タイムアウトした場合は {@link
     * java.net.SocketTimeoutException} をそのまま投げ、再試行はしない（再試行の対象は状態コードが 429 と 503 の場合だけ）。
     *
     * @return 応答本文
     * @throws IOException 入出力の操作に失敗した場合
     * @throws RuntimeException 応答が 200 以外の場合（状態コードを持つ例外、またはブロックの例外）
     */
    String send() throws IOException {
      HttpURLConnection conn = connectionOpener.open(endpoint);
      try {
        HttpTimeout.DEFAULT.applyTo(conn);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-goog-api-key", apiKey);
        conn.setDoOutput(true);
        byte[] payload = ObjectMapperFactory.OBJECT_MAPPER.writeValueAsBytes(request);
        try (OutputStream os = conn.getOutputStream()) {
          os.write(payload);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
          try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
          }
        }

        String errorBody;
        try (InputStream es = conn.getErrorStream()) {
          errorBody = es == null ? "" : new String(es.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
          String errorCode;
          try {
            errorCode =
                ObjectMapperFactory.OBJECT_MAPPER
                    .readTree(errorBody)
                    .path("error")
                    .path("code")
                    .asText("");
          } catch (JsonProcessingException e) {
            // 本文が JSON でない場合は error.code が無いものとして、通常の例外にする
            errorCode = "";
          }
          if (BLOCKED_ERROR_CODES.contains(errorCode)) {
            throw new DecisionBlockedException(
                "【Gemini Decision API】安全性のフィルタで入力がブロックされました。error.code: " + errorCode);
          }
        }
        throw new ApiRetryHelper.HttpStatusException(
            responseCode, "【Gemini Decision API】HTTP " + responseCode + " エラー: " + errorBody);
      } finally {
        conn.disconnect();
      }
    }
  }

  /** 判定 API へのリクエスト本体を表します. */
  @Data
  @AllArgsConstructor
  static class DecisionRequest {

    /** モデル名。 */
    private String model;

    /** 全質問で共有する入力。 */
    private String input;

    /** 質問キーから質問本体へのマップ。入力の順序を保つ。 */
    private Map<String, DecisionQuestionBody> questions;
  }

  /** 判定 API の1問分の質問本体を表します. */
  @Data
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class DecisionQuestionBody {

    /** 質問の種類（noul または choice）。 */
    private String type;

    /** 指示文。 */
    private String instructions;

    /** 判断基準。noul は任意のオブジェクト、choice は選択肢のキーから説明へのオブジェクト。指定が無い場合は null。 */
    private Object criteria;

    /**
     * 質問から、判定 API へ送る質問本体を作ります.
     *
     * <p>noul は判断基準を指定した場合だけ criteria を付け、choice は選択肢を必ず criteria として付ける。
     *
     * @param question 質問
     * @return 判定 API へ送る質問本体
     */
    static DecisionQuestionBody of(final DecisionQuestion<?> question) {
      if (question instanceof NoulQuestion noul) {
        Map<String, String> criteria =
            noul.getTrueDescription() == null
                ? null
                : Map.of(
                    "true_description",
                    noul.getTrueDescription(),
                    "false_description",
                    noul.getFalseDescription());
        return new DecisionQuestionBody("noul", noul.getInstructions(), criteria);
      }
      // 質問の型は sealed で noul と choice の2つだけのため、残りは choice になる
      ChoiceQuestion choice = (ChoiceQuestion) question;
      return new DecisionQuestionBody("choice", choice.getInstructions(), choice.getOptions());
    }
  }

  /** 判定 API からの応答を表します. */
  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class DecisionResponse {

    /** 質問キーから回答本体へのマップ。応答に無い場合や null の場合は null のまま。 */
    private Map<String, DecisionAnswerBody> answers;
  }

  /** 判定 API の1問分の回答本体を表します. */
  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class DecisionAnswerBody {

    /** 回答の種類（noul または choice）。質問の種類と一致することを確かめる。 */
    private String type;

    /** Yes の確率（noul の回答の場合）。 */
    private Double noul;

    /** 選ばれた選択肢のキー（choice の回答の場合）。 */
    private String choice;

    /** 全選択肢の確率（choice の回答の場合）。 */
    private Map<String, Double> probabilities;

    /** 確信度。 */
    private Double confidence;

    /**
     * 質問の種類に合う回答を作ります.
     *
     * <p>回答の種類が質問の種類と違う場合、必要な項目が無い場合、選ばれた選択肢が質問の選択肢に無い場合は、回答の形式が想定と異なるとして例外にする。
     *
     * @param question 回答の対象の質問
     * @return 質問の種類に合う回答
     * @throws IllegalStateException 応答が質問の種類や項目や選択肢に合わない場合
     */
    DecisionAnswer toAnswer(final DecisionQuestion<?> question) {
      if (question instanceof NoulQuestion) {
        if (!"noul".equals(type) || noul == null || confidence == null) {
          throw new IllegalStateException("Yes/No の回答の形式が想定と異なります。質問キー: " + question.getKey());
        }
        return new NoulAnswer(noul, confidence);
      }
      ChoiceQuestion choiceQuestion = (ChoiceQuestion) question;
      if (!"choice".equals(type) || choice == null || probabilities == null || confidence == null) {
        throw new IllegalStateException("択一の回答の形式が想定と異なります。質問キー: " + question.getKey());
      }
      if (!choiceQuestion.getOptions().containsKey(choice)) {
        throw new IllegalStateException(
            "選ばれた選択肢が質問の選択肢にありません。質問キー: " + question.getKey() + ", 選択肢: " + choice);
      }
      return new ChoiceAnswer(choice, probabilities, confidence);
    }
  }
}
