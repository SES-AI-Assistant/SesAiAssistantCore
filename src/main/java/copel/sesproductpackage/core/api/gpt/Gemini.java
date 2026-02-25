package copel.sesproductpackage.core.api.gpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.ApiType;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.Properties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 【SES AIアシスタント】 Geminiクラス.
 *
 * @author 鈴木一矢.
 */
public class Gemini implements Transformer {
  /** 生成APIのエンドポイント. */
  private static final String GEMINI_COMPLETION_API_URL = Properties.get("GEMINI_COMPLETION_API_URL");

  /** 生成APIのデフォルトGPTモデル名. */
  private static final String COMPLETION_MODEL_DEFAULT = GeminiModel.GEMINI_1_5_FLASH_LITE.getModelName();

  /** エンベディングAPIのデフォルトモデル名. */
  private static final String EMBEDDING_MODEL_DEFAULT = GeminiModel.GEMINI_EMBEDDING_001.getModelName();

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
   * @param apiKey          APIキー
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
   * @param apiKey      APIキー
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
    ObjectMapper objectMapper = new ObjectMapper();

    // リクエストボディの作成
    // モデル名を含める必要があります
    String requestBody = String.format(
        "{\"model\":\"%s\",\"content\":{\"parts\":[{\"text\":\"%s\"}]}}",
        EMBEDDING_MODEL_DEFAULT, inputString);

    // HTTPリクエストの準備
    // エンベディングのエンドポイントは :embedContent
    URL url = new URL(
        "https://generativelanguage.googleapis.com/v1beta/"
            + EMBEDDING_MODEL_DEFAULT
            + ":embedContent?key="
            + this.apiKey);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    connection.setDoOutput(true);

    // リクエストボディを送信
    try (OutputStream os = connection.getOutputStream()) {
      byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    // レスポンスの取得（generateと同じエラーハンドリングを利用可能）
    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      // 既存のgenerateメソッドと同様のエラー処理ロジックをここに記述、または共通化
      throw new RuntimeException("Embedding API Error: " + responseCode);
    }

    // JSONレスポンスの解析
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
      StringBuilder response = new StringBuilder();
      String responseLine;
      while ((responseLine = br.readLine()) != null) {
        response.append(responseLine.trim());
      }

      JsonNode jsonResponse = objectMapper.readTree(response.toString());

      // "embedding" -> "values" から float配列を取得
      JsonNode valuesNode = jsonResponse.path("embedding").path("values");
      if (valuesNode.isArray()) {
        float[] embeddings = new float[valuesNode.size()];
        for (int i = 0; i < valuesNode.size(); i++) {
          embeddings[i] = (float) valuesNode.get(i).asDouble();
        }

        // API使用履歴の登録
        SES_AI_API_USAGE_HISTORY usageHistory = new SES_AI_API_USAGE_HISTORY();
        usageHistory.setProvider(Provider.Google);
        usageHistory.setModel(this.embeddingModel);
        usageHistory.setUsageMonth(new OriginalDateTime().getYYYYMM());
        usageHistory.setUserId("SesAiAssitantCore");
        usageHistory.setApiType(ApiType.Embedding);
        usageHistory.fetch();
        usageHistory.addInputCount(inputString != null ? inputString.length() : 0);
        usageHistory.save();

        return embeddings;
      }
    }

    return null;
  }

  @Override
  public GptAnswer generate(final String prompt) throws IOException, RuntimeException {
    if (prompt == null || prompt.isBlank()) {
      return null;
    }
    ObjectMapper objectMapper = new ObjectMapper();

    // リクエストボディの作成
    String requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}", prompt);

    // HTTPリクエストの準備
    URL url = new URL(
        GEMINI_COMPLETION_API_URL
            + this.completionModel
            + ":generateContent?key="
            + this.apiKey);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    connection.setDoOutput(true);

    // リクエストボディを送信
    try (OutputStream os = connection.getOutputStream()) {
      byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    // レスポンスの取得
    int responseCode = connection.getResponseCode();
    switch (responseCode) {
      case HttpURLConnection.HTTP_OK:
        break;
      case HttpURLConnection.HTTP_BAD_REQUEST:
        connection.disconnect();
        throw new RuntimeException("400 Bad Request: 無効なパラメータ、不適切なリクエストフォーマット、支払い上限超過エラー");
      case HttpURLConnection.HTTP_UNAUTHORIZED:
        connection.disconnect();
        throw new RuntimeException("401 Unauthorized: APIキーが無効、または提供されていないエラー");
      case HttpURLConnection.HTTP_FORBIDDEN:
        connection.disconnect();
        throw new RuntimeException("403 Forbidden: アカウントの制限、または対象モデルが利用不可のエラー");
      case HttpURLConnection.HTTP_NOT_FOUND:
        connection.disconnect();
        throw new RuntimeException("404 Not Found: APIのエンドポイントが間違っている、またはモデル名が無効のエラー");
      case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
        connection.disconnect();
        throw new RuntimeException("408 Request Timeout: リクエストが時間内に処理されなかったエラー");
      case 429:
        connection.disconnect();
        throw new RuntimeException("429 Too Many Requests: クレジット不足、短時間に過剰なリクエストを送信したためエラーが発生しました");
      case HttpURLConnection.HTTP_INTERNAL_ERROR:
        connection.disconnect();
        throw new RuntimeException("500 Internal Server Error: OpenAIのサーバーで問題が発生しました");
      case HttpURLConnection.HTTP_UNAVAILABLE:
        connection.disconnect();
        throw new RuntimeException("503 Service Unavailable: OpenAIのサーバーがメンテナンス中、または負荷が高い状態です");
      default:
        break;
    }

    // JSONレスポンスの解析
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
      StringBuilder response = new StringBuilder();
      String responseLine;
      while ((responseLine = br.readLine()) != null) {
        response.append(responseLine.trim());
      }

      // JSONをJacksonで解析
      JsonNode jsonResponse = objectMapper.readTree(response.toString());

      // "candidates" 配列から "content" -> "parts" -> "text" を取得
      JsonNode candidates = jsonResponse.path("candidates");
      if (candidates.isArray() && candidates.size() > 0) {
        JsonNode content = candidates.get(0).path("content");
        JsonNode parts = content.path("parts");
        if (parts.isArray() && parts.size() > 0) {
          String resultText = parts.get(0).path("text").asText();

          // API使用履歴テーブル（SES_AI_API_USAGE_HISTORY）に履歴を登録
          SES_AI_API_USAGE_HISTORY sesAiApiUsageHistory = new SES_AI_API_USAGE_HISTORY();
          sesAiApiUsageHistory.setProvider(Provider.Google);
          sesAiApiUsageHistory.setModel(this.completionModel);
          sesAiApiUsageHistory.setUsageMonth(new OriginalDateTime().getYYYYMM());
          sesAiApiUsageHistory.setUserId("SesAiAssitantCore");
          sesAiApiUsageHistory.setApiType(ApiType.Generate);
          sesAiApiUsageHistory.fetch();
          sesAiApiUsageHistory.addInputCount(prompt != null ? prompt.length() : 0);
          sesAiApiUsageHistory.addOutputCount(resultText != null ? resultText.length() : 0);
          sesAiApiUsageHistory.save();

          return new GptAnswer(resultText, Gemini.class);
        }
      }

      return new GptAnswer(null, Gemini.class);
    }
  }
}
