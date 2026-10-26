package copel.sesproductpackage.core.api.gpt;

import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import copel.sesproductpackage.core.api.gpt.schema.SchemaGenerator;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
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

            recordApiUsage(inputString.length(), 0, SES_AI_API_USAGE_HISTORY.ApiType.Embedding);
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
    if (prompt == null || prompt.isBlank()) {
      throw new RuntimeException("Prompt must not be blank");
    }
    if (responseType == null) {
      throw new RuntimeException("Response type must not be null");
    }

    try {
      // SchemaGenerator は型ごとに必ず非空のスキーマを返すため、空の確認は行わない
      Map<String, Object> jsonSchema = SchemaGenerator.generate(responseType);

      GenerateContentConfig config =
          GenerateContentConfig.builder()
              .responseMimeType("application/json")
              .responseJsonSchema(jsonSchema)
              .build();

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
            prompt.length(), resultJson.length(), SES_AI_API_USAGE_HISTORY.ApiType.Generate);

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
   * @param inputCount 入力文字数
   * @param outputCount 出力文字数
   */
  private void recordApiUsage(
      int inputCount, int outputCount, SES_AI_API_USAGE_HISTORY.ApiType apiType) {
    try {
      new SES_AI_API_USAGE_HISTORY()
          .recordUsage(
              SES_AI_API_USAGE_HISTORY.Provider.Google,
              this.completionModel,
              apiType,
              inputCount,
              outputCount);
    } catch (Exception e) {
      log.warn("Failed to record API usage history", e);
    }
  }
}
