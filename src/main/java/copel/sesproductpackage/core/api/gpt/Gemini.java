package copel.sesproductpackage.core.api.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import copel.sesproductpackage.core.api.gpt.schema.JsonSchemaProvider;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY;
import copel.sesproductpackage.core.unit.OriginalDateTime;
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
      Client client = Client.builder().apiKey(this.apiKey).build();
      EmbedContentResponse response =
          client.models.embedContent(this.embeddingModel, inputString, null);

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

            recordApiUsage(inputString.length(), 0);
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
      Client client = Client.builder().apiKey(this.apiKey).build();
      GenerateContentResponse response =
          client.models.generateContent(this.completionModel, prompt, null);

      if (response != null) {
        String resultText = response.text();
        recordApiUsage(prompt.length(), resultText != null ? resultText.length() : 0);

        String callerInfo = "Unknown";
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTrace.length; i++) {
          String className = stackTrace[i].getClassName();
          if (!className.equals(Gemini.class.getName()) && !className.contains("Thread")) {
            String[] classNameParts = className.split("\\.");
            String simpleClassName = classNameParts[classNameParts.length - 1];
            callerInfo = simpleClassName + "." + stackTrace[i].getMethodName();
            break;
          }
        }
        log.info(
            "【Gemini API】呼び出し元: {}, モデル: {}, 入力: {} 文字, 出力: {} 文字",
            callerInfo,
            this.completionModel,
            prompt.length(),
            (resultText != null ? resultText.length() : 0));

        return new GptAnswer(resultText, Gemini.class);
      }
      return new GptAnswer(null, Gemini.class);
    } catch (Exception e) {
      throw new RuntimeException("Generate API Error: " + e.getMessage(), e);
    }
  }

  @Override
  public <T extends JsonSchemaProvider> T generate(final String prompt, final Class<T> responseType)
      throws IOException, RuntimeException {
    if (prompt == null || prompt.isBlank()) {
      throw new RuntimeException("Prompt must not be blank");
    }
    if (responseType == null) {
      throw new RuntimeException("Response type must not be null");
    }

    try {
      T schemaInstance = responseType.getDeclaredConstructor().newInstance();
      Map<String, Object> jsonSchema = schemaInstance.getJsonSchema();

      if (jsonSchema == null || jsonSchema.isEmpty()) {
        throw new RuntimeException("JSON schema must not be null or empty");
      }

      GenerateContentConfig config =
          GenerateContentConfig.builder()
              .responseMimeType("application/json")
              .responseJsonSchema(jsonSchema)
              .build();

      Client client = Client.builder().apiKey(this.apiKey).build();
      GenerateContentResponse response =
          client.models.generateContent(this.completionModel, prompt, config);

      if (response != null) {
        String resultJson = response.text();
        if (resultJson == null || resultJson.isBlank()) {
          throw new RuntimeException("API returned empty response");
        }

        ObjectMapper mapper = new ObjectMapper();
        T result = mapper.readValue(resultJson, responseType);

        recordApiUsage(prompt.length(), resultJson.length());

        String callerInfo = "Unknown";
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTrace.length; i++) {
          String className = stackTrace[i].getClassName();
          if (!className.equals(Gemini.class.getName()) && !className.contains("Thread")) {
            String[] classNameParts = className.split("\\.");
            String simpleClassName = classNameParts[classNameParts.length - 1];
            callerInfo = simpleClassName + "." + stackTrace[i].getMethodName();
            break;
          }
        }
        log.info(
            "【Gemini API (Structured)】呼び出し元: {}, モデル: {}, 型: {}, 入力: {} 文字, 出力: {} 文字",
            callerInfo,
            this.completionModel,
            responseType.getSimpleName(),
            prompt.length(),
            resultJson.length());

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
   * API使用履歴をDBに記録します.
   *
   * @param inputCount 入力文字数
   * @param outputCount 出力文字数
   */
  private void recordApiUsage(int inputCount, int outputCount) {
    try {
      SES_AI_API_USAGE_HISTORY usageHistory = new SES_AI_API_USAGE_HISTORY();
      usageHistory.setProvider(SES_AI_API_USAGE_HISTORY.Provider.Google);
      usageHistory.setModel(this.completionModel);
      OriginalDateTime nowDt = new OriginalDateTime();
      usageHistory.setUsageMonth(nowDt.getYYYYMM());
      usageHistory.setUsageDate(nowDt.getYYYYMMDD());
      usageHistory.setUserId("SesAiAssitantCore");
      usageHistory.setApiType(
          inputCount > 0 && outputCount == 0
              ? SES_AI_API_USAGE_HISTORY.ApiType.Embedding
              : SES_AI_API_USAGE_HISTORY.ApiType.Generate);
      usageHistory.fetch();
      usageHistory.addInputCount(inputCount);
      usageHistory.addOutputCount(outputCount);
      usageHistory.save();
    } catch (Exception e) {
      log.warn("Failed to record API usage history", e);
    }
  }
}
