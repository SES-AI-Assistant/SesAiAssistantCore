package copel.sesproductpackage.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * プロパティファイルを扱うクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class Properties {
  /** デフォルト: 本番で従来から使用しているバケット名. */
  private static final String DEFAULT_CONFIG_BUCKET = "environment-variables-configuration";

  /** デフォルト: 本番で従来から使用しているオブジェクトキー. */
  private static final String DEFAULT_CONFIG_OBJECT_KEY = "config.properties";

  /** 環境変数: config を格納する S3 バケット（IT1 / LocalStack 用に上書き可）. */
  private static final String ENV_CONFIG_BUCKET = "SES_AI_PROPERTIES_S3_BUCKET";

  /** 環境変数: config のオブジェクトキー. */
  private static final String ENV_CONFIG_OBJECT_KEY = "SES_AI_PROPERTIES_S3_KEY";

  /** 環境変数: S3 クライアントのリージョン（未設定時は ap-northeast-1）. */
  private static final String ENV_AWS_REGION = "AWS_REGION";

  private static final String CONFIG_BUCKET = resolveConfigBucketName();
  private static final String CONFIG_OBJECT_KEY = resolveConfigObjectKey();

  /** プロパティ. */
  private static final Map<String, String> properties = new HashMap<>();

  /* staticイニシャライザ. */
  static {
    try {
      try (S3Client s3Client = buildS3ClientForConfigLoad()) {
        load(s3Client);
      }
    } catch (Throwable e) {
      log.error("【SesAiAssitantCore】Properties static block failed", e);
      // We don't rethrow to avoid "Could not initialize class" errors in subsequent tests
    }
  }

  private static String trimOrNull(String value) {
    if (value == null) {
      return null;
    }
    String t = value.trim();
    return t.isEmpty() ? null : t;
  }

  private static String resolveConfigBucketName() {
    String v = trimOrNull(System.getenv(ENV_CONFIG_BUCKET));
    return v != null ? v : DEFAULT_CONFIG_BUCKET;
  }

  private static String resolveConfigObjectKey() {
    String v = trimOrNull(System.getenv(ENV_CONFIG_OBJECT_KEY));
    return v != null ? v : DEFAULT_CONFIG_OBJECT_KEY;
  }

  private static Region resolveS3Region() {
    String r = trimOrNull(System.getenv(ENV_AWS_REGION));
    if (r != null) {
      try {
        return Region.of(r);
      } catch (Exception e) {
        log.warn("【SesAiAssitantCore】無効なリージョン '{}' のため {} を使用します。", r, Region.AP_NORTHEAST_1);
      }
    }
    return Region.AP_NORTHEAST_1;
  }

  static S3Client buildS3ClientForConfigLoad() {
    Region region = resolveS3Region();
    String endpointUrl = AwsEndpointUtil.resolveEndpointUrl();
    S3ClientBuilder b = S3Client.builder().region(region);

    if (endpointUrl != null) {
      try {
        URI uri = URI.create(endpointUrl);
        b.endpointOverride(uri).forcePathStyle(true);
        log.info("【SesAiAssitantCore】Properties S3 はカスタムエンドポイントを使用します: {}", endpointUrl);
        String ak = trimOrNull(System.getenv("AWS_ACCESS_KEY_ID"));
        String sk = trimOrNull(System.getenv("AWS_SECRET_ACCESS_KEY"));
        if (ak != null && sk != null) {
          b.credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk)));
        } else {
          b.credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        }
      } catch (IllegalArgumentException e) {
        log.warn("【SesAiAssitantCore】無効なエンドポイント URL のためデフォルトの S3 エンドポイントを使用します: {}", endpointUrl);
        b.credentialsProvider(DefaultCredentialsProvider.create());
      }
    } else {
      b.credentialsProvider(DefaultCredentialsProvider.create());
    }
    return b.build();
  }

  /**
   * プロパティファイルをS3から読み込みます。
   *
   * @param s3Client S3クライアント
   */
  static void load(S3Client s3Client) {
    try (InputStream inputStream =
        s3Client.getObject(
            GetObjectRequest.builder().bucket(CONFIG_BUCKET).key(CONFIG_OBJECT_KEY).build())) {

      // プロパティファイルの内容を読み込む
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
          // 行が空でなく、'='で区切られていればプロパティとして登録
          if (!line.trim().isEmpty() && line.contains("=")) {
            String[] keyValue = line.split("=", 2);
            if (keyValue.length == 2) {
              properties.put(keyValue[0].trim(), keyValue[1].trim());
            }
          }
        }
      }
    } catch (IOException e) {
      log.info("【SesAiAssitantCore】バケット名：{} ファイル名：{}", CONFIG_BUCKET, CONFIG_OBJECT_KEY);
      log.error("【SesAiAssitantCore】環境変数の読み込みに失敗しました。{}", e.getMessage());
    }
  }

  /**
   * 引数をキーにプロパティの値を取得します.
   *
   * @param key キー
   * @return 値
   */
  public static String get(final String key) {
    return properties.getOrDefault(key, "");
  }

  /**
   * 引数をキーにプロパティの値を整数型で取得します.
   *
   * @param key キー
   * @return 整数型の値
   */
  public static Integer getInt(final String key) {
    String val = properties.get(key);
    return val != null ? Integer.parseInt(val) : 0;
  }

  /**
   * 引数をキーにプロパティの値を小数型で取得します.
   *
   * @param key キー
   * @return 小数型の値
   */
  public static Double getDouble(final String key) {
    String val = properties.get(key);
    return val != null ? Double.parseDouble(val) : 0.0;
  }

  /**
   * 引数をキーにプロパティの値を文字配列で取得します.
   *
   * @param key キー
   * @return 配列
   */
  public static String[] getAsArray(final String key) {
    String value = properties.get(key);
    return value != null ? value.split(",") : new String[0];
  }
}
