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
import lombok.extern.slf4j.Slf4j;

/**
 * 【SES AIアシスタント】 OpenAIクラス.
 *
 * @author 鈴木一矢.
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
  private static final String COMPLETION_MODEL_DEFAULT = OpenAIModel.GPT_3_5_TURBO.getModelName();

  /** OpenAIの質問応答を処理する際のtemperatureパラメータのデフォルト値. */
  private static final Float COMPLETION_TEMPERATURE =
      Float.valueOf(Properties.get("OPEN_AI_COMPLETION_TEMPERATURE"));

  /** OpenAIのファイルアップロードAPIのエンドポイント. */
  private static final String FILE_UPLOAD_URL = Properties.get("OPEN_AI_FILE_UPLOAD_URL");

  /** OpenAIのファインチューニングAPIのエンドポイント. */
  private static final String FINE_TUNE_URL = Properties.get("OPEN_AI_FINE_TUNE_URL");

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

    URL url = new URL(EMBEDDING_API_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
    conn.setDoOutput(true);

    log.info("【OpenAI】{}文字のエンベディング処理を実行しました", inputString.length());
    String jsonBody =
        "{\"input\": \""
            + inputString.replaceAll("\\p{Cntrl}", "")
            + "\", \"model\": \""
            + EMBEDDING_MODEL
            + "\"}";
    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    checkResponseCode(conn, responseCode);

    String response = readResponse(conn);
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonResponse = objectMapper.readTree(response);
    JsonNode embeddingArray = jsonResponse.get("data").get(0).get("embedding");

    float[] vectorValue = new float[embeddingArray.size()];
    for (int i = 0; i < embeddingArray.size(); i++) {
      vectorValue[i] = embeddingArray.get(i).floatValue();
    }

    // API使用履歴テーブル（SES_AI_API_USAGE_HISTORY）に履歴を登録
    SES_AI_API_USAGE_HISTORY sesAiApiUsageHistory = new SES_AI_API_USAGE_HISTORY();
    sesAiApiUsageHistory.setProvider(Provider.OpenAI);
    sesAiApiUsageHistory.setModel(this.completionModel);
    sesAiApiUsageHistory.setUsageMonth(new OriginalDateTime().getYYYYMM());
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

    String content = prompt.replaceAll("[\\p{C}\\p{P}\"]", "");
    String jsonBody =
        "{\"model\": \""
            + this.completionModel
            + "\", \"messages\": [{\"role\": \"user\", \"content\": \""
            + content
            + "\"}], \"temperature\": "
            + temperature.toString()
            + "}";
    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    checkResponseCode(conn, responseCode);

    String response = readResponse(conn);
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonResponse = objectMapper.readTree(response);
    JsonNode contentNode = jsonResponse.get("choices").get(0).get("message").get("content");
    String resultText = contentNode == null || contentNode.isNull() ? null : contentNode.asText();

    // API使用履歴テーブル（SES_AI_API_USAGE_HISTORY）に履歴を登録
    SES_AI_API_USAGE_HISTORY sesAiApiUsageHistory = new SES_AI_API_USAGE_HISTORY();
    sesAiApiUsageHistory.setProvider(Provider.OpenAI);
    sesAiApiUsageHistory.setModel(this.completionModel);
    sesAiApiUsageHistory.setUsageMonth(new OriginalDateTime().getYYYYMM());
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
   */
  public void fineTuning(final String trainingData) throws IOException {
    // 1. JSONLフォーマットに変換
    String jsonlData =
        "{\"messages\": [{\"role\": \"system\", \"content\": \"ファインチューニングデータ\"}]}\n"
            + "{\"messages\": [{\"role\": \"user\", \"content\": \""
            + trainingData
            + "\"}, {\"role\": \"assistant\", \"content\": \"OK\"}]}";

    // 2. OpenAI にデータをアップロード
    URL fileUrl = new URL(FILE_UPLOAD_URL);
    HttpURLConnection fileConn = (HttpURLConnection) fileUrl.openConnection();
    fileConn.setRequestMethod("POST");
    fileConn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
    fileConn.setRequestProperty("Content-Type", "application/json");
    fileConn.setDoOutput(true);

    String jsonBody = "{\"purpose\": \"fine-tune\", \"file\": \"" + jsonlData + "\"}";
    try (OutputStream os = fileConn.getOutputStream()) {
      byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int fileResponseCode = fileConn.getResponseCode();
    checkResponseCode(fileConn, fileResponseCode);

    String fileResponse = readResponse(fileConn);
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode fileJson = objectMapper.readTree(fileResponse);
    String fileId = fileJson.get("id").asText();

    // 3. ファインチューニングジョブを開始
    URL fineTuneUrl = new URL(FINE_TUNE_URL);
    HttpURLConnection fineTuneConn = (HttpURLConnection) fineTuneUrl.openConnection();
    fineTuneConn.setRequestMethod("POST");
    fineTuneConn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
    fineTuneConn.setRequestProperty("Content-Type", "application/json");
    fineTuneConn.setDoOutput(true);

    String fineTuneBody =
        "{\"training_file\": \"" + fileId + "\", \"model\": \"" + this.completionModel + "\"}";
    try (OutputStream os = fineTuneConn.getOutputStream()) {
      os.write(fineTuneBody.getBytes(StandardCharsets.UTF_8));
    }

    int responseCode = fineTuneConn.getResponseCode();
    if (responseCode != 200) {
      throw new RuntimeException("Fine-tuning Error: " + responseCode);
    }
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
        conn.disconnect();
        throw new RuntimeException("400 Bad Request: 無効なパラメータ、不適切なリクエストフォーマット、支払い上限超過エラー");
      case HttpURLConnection.HTTP_UNAUTHORIZED:
        conn.disconnect();
        throw new RuntimeException("401 Unauthorized: APIキーが無効、または提供されていないエラー");
      case HttpURLConnection.HTTP_FORBIDDEN:
        conn.disconnect();
        throw new RuntimeException("403 Forbidden: アカウントの制限、または対象モデルが利用不可のエラー");
      case HttpURLConnection.HTTP_NOT_FOUND:
        conn.disconnect();
        throw new RuntimeException("404 Not Found: APIのエンドポイントが間違っている、またはモデル名が無効のエラー");
      case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
        conn.disconnect();
        throw new RuntimeException("408 Request Timeout: リクエストが時間内に処理されなかったエラー");
      case 429:
        conn.disconnect();
        throw new RuntimeException("429 Too Many Requests: クレジット不足、短時間に過剰なリクエストを送信したためエラーが発生しました");
      case HttpURLConnection.HTTP_INTERNAL_ERROR:
        conn.disconnect();
        throw new RuntimeException("500 Internal Server Error: OpenAIのサーバーで問題が発生しました");
      case HttpURLConnection.HTTP_UNAVAILABLE:
        conn.disconnect();
        throw new RuntimeException("503 Service Unavailable: OpenAIのサーバーがメンテナンス中、または負荷が高い状態です");
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
}
