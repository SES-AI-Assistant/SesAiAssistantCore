package copel.sesproductpackage.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * UT用のプロパティクラス. S3への初期アクセスを回避しつつ、必要に応じてS3Client(Mock)から読み込めるようにします.
 */
@Slf4j
public class Properties {
  private static final Map<String, String> properties = new HashMap<>();

  static {
    // 最小限のデフォルト値を設定
    properties.put("GEMINI_COMPLETION_API_URL", "http://localhost/gemini/");
    properties.put("OPEN_AI_EMBEDDING_API_URL", "http://localhost/openai/embedding");
    properties.put("OPEN_AI_EMBEDDING_MODEL", "dummy-embedding-model");
    properties.put("OPEN_AI_COMPLETION_API_URL", "http://localhost/openai/completion");
    properties.put("OPEN_AI_COMPLETION_TEMPERATURE", "0.7");
    properties.put("OPEN_AI_FILE_UPLOAD_URL", "http://localhost/openai/upload");
    properties.put("OPEN_AI_FINE_TUNE_URL", "http://localhost/openai/finetune");
    properties.put("LINE_PUSH_MESSAGE_API_ENDPOINT", "http://localhost/line/push");
    properties.put("LINE_BROADCAST_API_ENDPOINT", "http://localhost/line/broadcast");
    properties.put("LINE_DONLOAD_FILE_API_ENDPOINT", "http://localhost/line/download");
    properties.put("SKILLSHEET_SUMMARIZE_PROMPT", "Summarize this: ");
    properties.put("SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH", "1000");
    properties.put("S3_BUCKET_NAME", "dummy-bucket");
    properties.put("SES_DB_ENDPOINT_URL", "jdbc:postgresql://localhost:5432/dummy");
    properties.put("SES_DB_USER_NAME", "user");
    properties.put("SES_DB_USER_PASSWORD", "password");
    
    properties.put("JOB_FEATURES_ARRAY_HIGH", "案件,募集,要件");
    properties.put("JOB_FEATURES_ARRAY_LOW", "場所,単価");
    properties.put("PERSONEL_FEATURES_ARRAY_HIGH", "氏名,スキル,経験");
    properties.put("PERSONEL_FEATURES_ARRAY_LOW", "可能,希望");
    properties.put("JOB_FEATURES_ARRAY", "案件,募集");
    properties.put("PERSONEL_FEATURES_ARRAY", "氏名,スキル");
    properties.put("TARGET_NUMBER_OF_CRITERIA", "10");
    properties.put("MULTIPLE_PERSONNEL_JUDGMENT_PROMPT", "Multiple Persons?");
    properties.put("MULTIPLE_JOB_JUDGMENT_PROMPT", "Multiple Jobs?");
  }

  static void load(S3Client s3Client) {
     try (InputStream inputStream =
        s3Client.getObject(GetObjectRequest.builder().build())) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.trim().isEmpty() && line.contains("=")) {
            String[] keyValue = line.split("=", 2);
            if (keyValue.length == 2) {
              properties.put(keyValue[0].trim(), keyValue[1].trim());
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("【Mock】環境変数の読み込みに失敗しました。{}", e.getMessage());
    }
  }

  public static String get(final String key) {
    return properties.get(key);
  }

  public static Integer getInt(final String key) {
    String val = get(key);
    return val != null ? Integer.parseInt(val) : 0;
  }

  public static Double getDouble(final String key) {
    String val = get(key);
    return val != null ? Double.parseDouble(val) : 0.0;
  }

  public static String[] getAsArray(final String key) {
    String value = get(key);
    return value != null ? value.split(",") : new String[0];
  }
}
