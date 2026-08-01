package copel.sesproductpackage.core.api.gpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Geminiクラス.
 *
 * @author Copel Co., Ltd..
 */
@Data
@Slf4j
public class Gemini implements Transformer {
  /** 生成APIのエンドポイント. */
  private static final String GEMINI_COMPLETION_API_URL =
      Properties.get("GEMINI_COMPLETION_API_URL");

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
    ObjectMapper objectMapper = new ObjectMapper();

    // リクエストボディの作成（JSON連結ではなくObjectMapperで構築）
    ObjectNode rootNode = objectMapper.createObjectNode();
    rootNode.put("model", EMBEDDING_MODEL_DEFAULT);
    ObjectNode contentNode = rootNode.putObject("content");
    ArrayNode partsArray = contentNode.putArray("parts");
    ObjectNode partNode = partsArray.addObject();
    partNode.put("text", inputString);
    String requestBody = objectMapper.writeValueAsString(rootNode);

    // HTTPリクエストの準備
    // エンベディングのエンドポイントは :embedContent
    URL url =
        new URL(
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

    // レスポンスの取得
    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      connection.disconnect();
      switch (responseCode) {
        case HttpURLConnection.HTTP_BAD_REQUEST:
          throw new RuntimeException("400 Bad Request: 無効なパラメータ、または不適切なリクエストフォーマットです");
        case HttpURLConnection.HTTP_UNAUTHORIZED:
          throw new RuntimeException("401 Unauthorized: APIキーが無効、または提供されていないエラー");
        case HttpURLConnection.HTTP_FORBIDDEN:
          throw new RuntimeException("403 Forbidden: アカウントの制限、または対象モデルが利用不可のエラー");
        case HttpURLConnection.HTTP_NOT_FOUND:
          throw new RuntimeException("404 Not Found: APIのエンドポイントが間違っている、またはモデル名が無効のエラー");
        case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
          throw new RuntimeException("408 Request Timeout: リクエストが時間内に処理されなかったエラー");
        case 429:
          throw new RuntimeException(
              "429 Too Many Requests: レート制限に達しました。短時間に過剰なリクエストを送信したためエラーが発生しました");
        case HttpURLConnection.HTTP_INTERNAL_ERROR:
          throw new RuntimeException("500 Internal Server Error: Geminiサーバーで問題が発生しました");
        case HttpURLConnection.HTTP_UNAVAILABLE:
          throw new RuntimeException("503 Service Unavailable: Geminiサーバーがメンテナンス中、または負荷が高い状態です");
        default:
          throw new RuntimeException("Embedding API Error: " + responseCode);
      }
    }

    // JSONレスポンスの解析
    try (BufferedReader br =
        new BufferedReader(
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
        OriginalDateTime nowDt = new OriginalDateTime();
        usageHistory.setUsageMonth(nowDt.getYYYYMM());
        usageHistory.setUsageDate(nowDt.getYYYYMMDD());
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

    // リクエストボディの作成（JSON連結ではなくObjectMapperで構築）
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode contentsArray = rootNode.putArray("contents");
    ObjectNode contentNode = contentsArray.addObject();
    ArrayNode partsArray = contentNode.putArray("parts");
    ObjectNode partNode = partsArray.addObject();
    partNode.put("text", prompt);
    String requestBody = objectMapper.writeValueAsString(rootNode);

    // HTTPリクエストの準備
    URL url =
        new URL(
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
        throw new RuntimeException("400 Bad Request: 無効なパラメータ、または不適切なリクエストフォーマットです");
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
        throw new RuntimeException(
            "429 Too Many Requests: レート制限に達しました。短時間に過剰なリクエストを送信したためエラーが発生しました");
      case HttpURLConnection.HTTP_INTERNAL_ERROR:
        connection.disconnect();
        throw new RuntimeException("500 Internal Server Error: Geminiサーバーで問題が発生しました");
      case HttpURLConnection.HTTP_UNAVAILABLE:
        connection.disconnect();
        throw new RuntimeException("503 Service Unavailable: Geminiサーバーがメンテナンス中、または負荷が高い状態です");
      default:
        break;
    }

    // JSONレスポンスの解析
    try (BufferedReader br =
        new BufferedReader(
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
          OriginalDateTime nowDt = new OriginalDateTime();
          sesAiApiUsageHistory.setUsageMonth(nowDt.getYYYYMM());
          sesAiApiUsageHistory.setUsageDate(nowDt.getYYYYMMDD());
          sesAiApiUsageHistory.setUserId("SesAiAssitantCore");
          sesAiApiUsageHistory.setApiType(ApiType.Generate);
          sesAiApiUsageHistory.fetch();
          sesAiApiUsageHistory.addInputCount(prompt != null ? prompt.length() : 0);
          sesAiApiUsageHistory.addOutputCount(resultText != null ? resultText.length() : 0);
          sesAiApiUsageHistory.save();

          // 呼び出し元の特定
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
              (prompt != null ? prompt.length() : 0),
              (resultText != null ? resultText.length() : 0));

          return new GptAnswer(resultText, Gemini.class);
        }
      }
    }
    return new GptAnswer(null, Gemini.class);
  }

  /**
   * 引数に入力された文字列を質問として、LLMにJSON形式での回答の生成を実行させその回答を返却します.
   *
   * <p>503（Service Unavailable）と429（Rate Limit）エラーに対してはexponential backoff リトライを自動実行します。
   *
   * @param prompt プロンプト
   * @return 回答
   * @throws RuntimeException
   */
  public GptAnswer generateJson(final String prompt) throws RuntimeException {
    if (prompt == null || prompt.isBlank()) {
      return null;
    }
    return ApiRetryHelper.executeWithRetry(
        () -> {
          try {
            return generateJsonInternal(prompt);
          } catch (IOException e) {
            throw new RuntimeException("【Gemini API】JSON生成処理でIO例外が発生しました", e);
          }
        });
  }

  /**
   * generateJsonの内部実装（リトライロジックなし）.
   *
   * @param prompt プロンプト
   * @return 回答
   * @throws IOException
   * @throws RuntimeException
   */
  private GptAnswer generateJsonInternal(final String prompt) throws IOException, RuntimeException {
    ObjectMapper objectMapper = new ObjectMapper();

    // リクエストボディの作成
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode contentsArray = rootNode.putArray("contents");
    ObjectNode contentNode = contentsArray.addObject();
    ArrayNode partsArray = contentNode.putArray("parts");
    ObjectNode partNode = partsArray.addObject();
    partNode.put("text", prompt);

    // JSON指定のための generationConfig を追加
    ObjectNode configNode = rootNode.putObject("generationConfig");
    configNode.put("responseMimeType", "application/json");

    String requestBody = objectMapper.writeValueAsString(rootNode);

    // HTTPリクエストの準備
    URL url =
        new URL(
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
    if (responseCode != HttpURLConnection.HTTP_OK) {
      connection.disconnect();
      switch (responseCode) {
        case HttpURLConnection.HTTP_BAD_REQUEST:
          throw new RuntimeException("400 Bad Request: 無効なパラメータ、または不適切なリクエストフォーマットです");
        case HttpURLConnection.HTTP_UNAUTHORIZED:
          throw new RuntimeException("401 Unauthorized: APIキーが無効、または提供されていないエラー");
        case HttpURLConnection.HTTP_FORBIDDEN:
          throw new RuntimeException("403 Forbidden: アカウントの制限、または対象モデルが利用不可のエラー");
        case HttpURLConnection.HTTP_NOT_FOUND:
          throw new RuntimeException("404 Not Found: APIのエンドポイントが間違っている、またはモデル名が無効のエラー");
        case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
          throw new RuntimeException("408 Request Timeout: リクエストが時間内に処理されなかったエラー");
        case 429:
          throw new RuntimeException(
              "429 Too Many Requests: レート制限に達しました。短時間に過剰なリクエストを送信したためエラーが発生しました");
        case HttpURLConnection.HTTP_INTERNAL_ERROR:
          throw new RuntimeException("500 Internal Server Error: Geminiサーバーで問題が発生しました");
        case HttpURLConnection.HTTP_UNAVAILABLE:
          throw new RuntimeException("503 Service Unavailable: Geminiサーバーがメンテナンス中、または負荷が高い状態です");
        default:
          throw new RuntimeException("Gemini JSON API Error: " + responseCode);
      }
    }

    // JSONレスポンスの解析
    try (BufferedReader br =
        new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
      StringBuilder response = new StringBuilder();
      String responseLine;
      while ((responseLine = br.readLine()) != null) {
        response.append(responseLine.trim());
      }

      JsonNode jsonResponse = objectMapper.readTree(response.toString());
      JsonNode candidates = jsonResponse.path("candidates");
      if (candidates.isArray() && candidates.size() > 0) {
        JsonNode content = candidates.get(0).path("content");
        JsonNode parts = content.path("parts");
        if (parts.isArray() && parts.size() > 0) {
          String resultText = parts.get(0).path("text").asText();

          // API使用履歴の登録
          SES_AI_API_USAGE_HISTORY sesAiApiUsageHistory = new SES_AI_API_USAGE_HISTORY();
          sesAiApiUsageHistory.setProvider(Provider.Google);
          sesAiApiUsageHistory.setModel(this.completionModel);
          OriginalDateTime nowDtJson = new OriginalDateTime();
          sesAiApiUsageHistory.setUsageMonth(nowDtJson.getYYYYMM());
          sesAiApiUsageHistory.setUsageDate(nowDtJson.getYYYYMMDD());
          sesAiApiUsageHistory.setUserId("SesAiAssitantCore");
          sesAiApiUsageHistory.setApiType(ApiType.Generate);
          sesAiApiUsageHistory.fetch();
          sesAiApiUsageHistory.addInputCount(prompt != null ? prompt.length() : 0);
          sesAiApiUsageHistory.addOutputCount(resultText != null ? resultText.length() : 0);
          sesAiApiUsageHistory.save();

          // 呼び出し元の特定
          String callerInfo = "Unknown";
          StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
          for (int i = 2; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!className.equals(Gemini.class.getName())
                && !className.equals(ApiRetryHelper.class.getName())
                && !className.contains("Thread")) {
              String[] classNameParts = className.split("\\.");
              String simpleClassName = classNameParts[classNameParts.length - 1];
              callerInfo = simpleClassName + "." + stackTrace[i].getMethodName();
              break;
            }
          }
          log.info(
              "【Gemini API (JSON)】呼び出し元: {}, モデル: {}, 入力: {} 文字, 出力: {} 文字",
              callerInfo,
              this.completionModel,
              (prompt != null ? prompt.length() : 0),
              (resultText != null ? resultText.length() : 0));

          return new GptAnswer(resultText, Gemini.class);
        }
      }

      return new GptAnswer(null, Gemini.class);
    }
  }
}
