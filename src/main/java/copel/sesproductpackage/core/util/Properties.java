package copel.sesproductpackage.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/**
 * プロパティファイルを扱うクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class Properties {
  /** S3 上の config ファイルのオブジェクトキー（固定）. */
  private static final String CONFIG_OBJECT_KEY = "config.properties";

  /** 環境変数: S3 クライアントのリージョン（未設定時は ap-northeast-1）. */
  private static final String ENV_AWS_REGION = "AWS_REGION";

  /** 環境変数: Parameter Store の環境（dev または prod）. */
  private static final String ENV_ENVIRONMENT = "ENVIRONMENT";

  private static final String ENVIRONMENT = resolveEnvironment();

  /** プロパティ. */
  private static final Map<String, String> properties = new HashMap<>();

  /** S3 バケット名（Parameter Store から動的に解決）. */
  private static String CONFIG_BUCKET;

  /** Parameter Store キャッシュ最終更新時刻. */
  private static long lastParameterStoreLoadTime = 0;

  /** S3 キャッシュ最終更新時刻. */
  private static long lastS3LoadTime = 0;

  /** キャッシュ有効期限のデフォルト値（1日）. */
  private static final long DEFAULT_CACHE_TTL_MS = 86400000;

  /* staticイニシャライザ. 読み込み元が memory のときは AWS に触れない。 */
  static {
    initializeAtStartup();
  }

  /**
   * クラス初期化時の設定読み込みを行います。
   *
   * <p>本番では、Parameter Store から読み込んでバケット名を解決し、そのバケットから S3 の設定ファイルを読み込む。この順番は、バケット名が Parameter Store
   * の値に依存するため変えない。例外は握りつぶしてログに出す。Lambda のコールドスタートで一度だけ実行され、結果は static のキャッシュに残る。
   *
   * <p>読み込み元が memory のときは何もしない。AWS のクライアントを作らないため、オフラインでも即座に完了し、設定値は空から始まる。設定値はテストから {@link
   * #putForTest(String, String)} で入れる。
   */
  static void initializeAtStartup() {
    if (ConfigSource.INSTANCE.isMemory()) {
      log.info("設定の読み込み元が memory のため、AWS からの設定読み込みを行いません。");
      return;
    }
    try {
      try (SsmClient ssmClient = buildSsmClientForConfigLoad()) {
        loadFromParameterStore(ssmClient);
      }
      CONFIG_BUCKET = resolveConfigBucketName();
      try (S3Client s3Client = buildS3ClientForConfigLoad()) {
        load(s3Client);
      }
    } catch (Throwable e) {
      log.error("Properties static block failed", e);
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

  private static long getCacheTTL() {
    String ttlStr = properties.get("CACHE_TTL_MS");
    if (ttlStr != null && !ttlStr.isEmpty()) {
      try {
        return Long.parseLong(ttlStr.trim());
      } catch (NumberFormatException e) {
        log.warn("キャッシュTTLの値が不正です: {}", ttlStr);
      }
    }
    return DEFAULT_CACHE_TTL_MS;
  }

  private static String resolveConfigBucketName() {
    String bucketName = properties.get(SsmParameterKey.ENVIRONMENT_CONFIG_BUCKET_NAME.getKey());
    if (trimOrNull(bucketName) != null) {
      log.info("Parameter Store から環境設定バケット名を取得しました: {}", bucketName);
      return bucketName;
    }
    log.warn("Parameter Store から環境設定バケット名を取得できませんでした");
    return null;
  }

  private static String resolveEnvironment() {
    String v = trimOrNull(System.getenv(ENV_ENVIRONMENT));
    if (v != null) {
      return v;
    }
    String region = trimOrNull(System.getenv(ENV_AWS_REGION));
    if (region != null && region.contains("prod")) {
      return "prod";
    }
    return "dev";
  }

  /**
   * 環境変数 AWS_REGION からリージョンを解決します。
   *
   * <p>未設定、空、または解決できない値は既定の ap-northeast-1 にする。環境変数の読み取りを引数で受け取り、テストで値を差し替えられるようにする。
   *
   * @param environment 環境変数を読む関数。本番では {@code System::getenv} を渡す
   * @return 解決したリージョン
   */
  static Region resolveS3Region(final UnaryOperator<String> environment) {
    String r = trimOrNull(environment.apply(ENV_AWS_REGION));
    if (r != null) {
      try {
        return Region.of(r);
      } catch (Exception e) {
        log.warn("無効なリージョン '{}' のため {} を使用します。", r, Region.AP_NORTHEAST_1);
      }
    }
    return Region.AP_NORTHEAST_1;
  }

  /**
   * 設定ファイル読み込み用の S3 クライアントを、本番の環境変数で生成します。
   *
   * @return S3 クライアント
   */
  static S3Client buildS3ClientForConfigLoad() {
    return buildS3ClientForConfigLoad(System::getenv);
  }

  /**
   * 設定ファイル読み込み用の S3 クライアントを生成します。
   *
   * <p>エンドポイント URL が有効なら、そのエンドポイントに向けて path-style で接続する。資格情報は環境変数の組、なければ固定値 test
   * を使う。エンドポイントが無効な場合は既定の資格情報を使う。
   *
   * @param environment 環境変数を読む関数。本番では {@code System::getenv} を渡す
   * @return S3 クライアント
   */
  static S3Client buildS3ClientForConfigLoad(final UnaryOperator<String> environment) {
    Region region = resolveS3Region(environment);
    String endpointUrl = AwsEndpointUtil.resolveEndpointUrl(environment);
    S3ClientBuilder b = S3Client.builder().region(region);

    if (endpointUrl != null) {
      try {
        URI uri = URI.create(endpointUrl);
        b.endpointOverride(uri).forcePathStyle(true);
        log.info("Properties S3 はカスタムエンドポイントを使用します: {}", endpointUrl);
        String ak = trimOrNull(environment.apply("AWS_ACCESS_KEY_ID"));
        String sk = trimOrNull(environment.apply("AWS_SECRET_ACCESS_KEY"));
        if (ak != null && sk != null) {
          b.credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk)));
        } else {
          b.credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        }
      } catch (IllegalArgumentException e) {
        log.warn("無効なエンドポイント URL のためデフォルトの S3 エンドポイントを使用します: {}", endpointUrl);
        b.credentialsProvider(DefaultCredentialsProvider.create());
      }
    } else {
      b.credentialsProvider(DefaultCredentialsProvider.create());
    }
    return b.build();
  }

  /**
   * Parameter Store 読み込み用の SSM クライアントを、本番の環境変数で生成します。
   *
   * @return SSM クライアント
   */
  static SsmClient buildSsmClientForConfigLoad() {
    return buildSsmClientForConfigLoad(System::getenv);
  }

  /**
   * Parameter Store 読み込み用の SSM クライアントを生成します。
   *
   * <p>S3 の場合と同じく、エンドポイント URL と資格情報の解決に環境変数の読み取りを使う。エンドポイントが無効な場合は既定の資格情報を使う。
   *
   * @param environment 環境変数を読む関数。本番では {@code System::getenv} を渡す
   * @return SSM クライアント
   */
  static SsmClient buildSsmClientForConfigLoad(final UnaryOperator<String> environment) {
    Region region = resolveS3Region(environment);
    String endpointUrl = AwsEndpointUtil.resolveEndpointUrl(environment);
    SsmClientBuilder b = SsmClient.builder().region(region);

    if (endpointUrl != null) {
      try {
        URI uri = URI.create(endpointUrl);
        b.endpointOverride(uri);
        log.info("Properties SSM はカスタムエンドポイントを使用します: {}", endpointUrl);
        String ak = trimOrNull(environment.apply("AWS_ACCESS_KEY_ID"));
        String sk = trimOrNull(environment.apply("AWS_SECRET_ACCESS_KEY"));
        if (ak != null && sk != null) {
          b.credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk)));
        } else {
          b.credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        }
      } catch (IllegalArgumentException e) {
        log.warn("無効なエンドポイント URL のためデフォルトの SSM エンドポイントを使用します: {}", endpointUrl);
        b.credentialsProvider(DefaultCredentialsProvider.create());
      }
    } else {
      b.credentialsProvider(DefaultCredentialsProvider.create());
    }
    return b.build();
  }

  /**
   * プロパティファイルをS3から読み込みます。 キャッシュが有効な場合はスキップします（TTL: CACHE_TTL_MS プロパティで設定、デフォルト1日）。
   *
   * @param s3Client S3クライアント
   */
  static void load(S3Client s3Client) {
    long now = ApplicationClock.INSTANCE.clock().millis();
    long cacheTTL = getCacheTTL();
    if (lastS3LoadTime > 0 && (now - lastS3LoadTime) < cacheTTL) {
      log.debug("S3 プロパティファイル キャッシュが有効なため読み込みをスキップします。");
      return;
    }

    if (CONFIG_BUCKET == null) {
      log.warn("S3 バケット名が解決できなかったため、S3 からのプロパティファイル読み込みをスキップします");
      return;
    }

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
      lastS3LoadTime = now;
    } catch (IOException e) {
      log.info("バケット名：{} ファイル名：{}", CONFIG_BUCKET, CONFIG_OBJECT_KEY);
      log.error("環境変数の読み込みに失敗しました。{}", e.getMessage());
    }
  }

  /**
   * Parameter Store からパラメータを読み込みます。/nectar/{env}/ 以下のパラメータを全て読み込みます。 キー名が S3 のプロパティと被った場合は
   * Parameter Store の値を優先します。 キャッシュが有効な場合はスキップします（TTL: CACHE_TTL_MS プロパティで設定、デフォルト1日）。
   *
   * @param ssmClient SSM クライアント
   */
  static void loadFromParameterStore(SsmClient ssmClient) {
    long now = ApplicationClock.INSTANCE.clock().millis();
    long cacheTTL = getCacheTTL();
    if (lastParameterStoreLoadTime > 0 && (now - lastParameterStoreLoadTime) < cacheTTL) {
      log.debug("Parameter Store キャッシュが有効なため読み込みをスキップします。");
      return;
    }

    try {
      String parameterPath = String.format("/nectar/%s/", ENVIRONMENT);
      GetParametersByPathRequest request =
          GetParametersByPathRequest.builder()
              .path(parameterPath)
              .recursive(true)
              .maxResults(10)
              .build();

      GetParametersByPathResponse response = ssmClient.getParametersByPath(request);
      List<Parameter> params = response.parameters();

      while (params != null) {
        for (Parameter param : params) {
          String fullPath = param.name();
          String normalizedKey = fullPath.substring(parameterPath.length());
          properties.put(normalizedKey, param.value());
        }

        if (response.nextToken() != null) {
          GetParametersByPathRequest nextRequest =
              GetParametersByPathRequest.builder()
                  .path(parameterPath)
                  .recursive(true)
                  .maxResults(10)
                  .nextToken(response.nextToken())
                  .build();
          response = ssmClient.getParametersByPath(nextRequest);
          params = response.parameters();
        } else {
          params = null;
        }
      }

      lastParameterStoreLoadTime = now;
      log.info("Parameter Store から {} 件のパラメータを読み込みました。", properties.size());
    } catch (Exception e) {
      log.warn("Parameter Store からのパラメータ読み込みに失敗しました。{}", e.getMessage());
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

  /**
   * テスト用に設定値を1件登録します。読み込み元が memory の場合だけ許可されます。
   *
   * <p>テストで設定値を入れるための口である。同じキーが既にあれば値を置き換える。本番で設定値を書き換えると読み込み結果と食い違うため、memory 以外では拒否する。
   *
   * @param key キー。null は指定できない
   * @param value 値。null は指定できない
   * @throws IllegalStateException 読み込み元が memory 以外の場合
   * @throws NullPointerException key または value が null の場合
   */
  public static void putForTest(final String key, final String value) {
    ConfigSource.INSTANCE.requireMemory("設定値の登録");
    properties.put(
        Objects.requireNonNull(key, "キーにはnullを指定できません。"),
        Objects.requireNonNull(value, "値にはnullを指定できません。"));
  }

  /**
   * テスト用に設定値を1件削除します。読み込み元が memory の場合だけ許可されます。
   *
   * <p>テストで特定のキーを未設定の状態に戻すための口である。登録されていないキーを指定した場合は何もしない。
   *
   * @param key 削除するキー
   * @throws IllegalStateException 読み込み元が memory 以外の場合
   */
  public static void removeForTest(final String key) {
    ConfigSource.INSTANCE.requireMemory("設定値の削除");
    properties.remove(key);
  }

  /**
   * テスト用に設定値と読み込みの状態を初期化します。読み込み元が memory の場合だけ許可されます。
   *
   * <p>設定値をすべて消し、S3 のバケット名と Parameter Store と S3 の最終読み込み時刻を初期値に戻す。これにより、テストごとに TTL
   * キャッシュの影響を受けずに読み込みを検証できる。
   *
   * @throws IllegalStateException 読み込み元が memory 以外の場合
   */
  public static void resetForTest() {
    ConfigSource.INSTANCE.requireMemory("設定値の初期化");
    properties.clear();
    CONFIG_BUCKET = null;
    lastParameterStoreLoadTime = 0;
    lastS3LoadTime = 0;
  }
}
