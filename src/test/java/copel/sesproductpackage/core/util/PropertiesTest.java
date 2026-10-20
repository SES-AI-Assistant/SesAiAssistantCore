package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/** {@link Properties} の単体テスト。 */
@FixedClock("2026-01-15T12:00:00+09:00")
class PropertiesTest {

  /** 環境設定バケット名のパラメータのキー。接頭辞を除いた値。 */
  private static final String BUCKET_KEY = "infrastructure/s3/environment-config-bucket/name";

  /** テスト前の読み込み元の設定値。テスト後に元へ戻すために保存する。 */
  private String 読み込み元の設定値;

  @BeforeEach
  void 読み込み元と設定値を準備する() {
    読み込み元の設定値 = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    Properties.resetForTest();
  }

  @AfterEach
  void 設定値と読み込み元を元に戻す() {
    if (読み込み元の設定値 == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, 読み込み元の設定値);
    }
    Properties.resetForTest();
    System.clearProperty("aws.endpointUrl");
  }

  /**
   * 読み込み元の設定値を指定の値にする。
   *
   * @param value 設定する読み込み元の値
   */
  private static void 読み込み元を設定する(final String value) {
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, value);
  }

  /** 時計を 2 日後にする。既定の TTL の 1 日を過ぎて、キャッシュの期限が切れるようにするため。 */
  private static void 時計を2日後にする() {
    ApplicationClock.INSTANCE.setClock(
        Clock.fixed(Instant.parse("2026-01-17T03:00:00Z"), ZoneId.of("Asia/Tokyo")));
  }

  /**
   * 時計を指定の時刻にする。
   *
   * @param instant 設定する時刻
   */
  private static void 時計を設定する(final Instant instant) {
    ApplicationClock.INSTANCE.setClock(Clock.fixed(instant, ZoneId.of("Asia/Tokyo")));
  }

  /**
   * 環境変数の関数を、指定のマップから値を返すものにする。
   *
   * @param values 環境変数の値
   * @return 環境変数の関数
   */
  private static UnaryOperator<String> 環境変数(final Map<String, String> values) {
    return values::get;
  }

  /**
   * SSM のクライアントの構築を、指定のクライアントを返すように差し替える。
   *
   * @param client 差し替えるクライアントのモック
   * @return 差し替えを解除するためのハンドル
   */
  private static MockedStatic<SsmClient> SSMを差し替える(final SsmClient client) {
    SsmClientBuilder builder = mock(SsmClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(client);
    MockedStatic<SsmClient> statics = mockStatic(SsmClient.class);
    statics.when(SsmClient::builder).thenReturn(builder);
    return statics;
  }

  /**
   * S3 のクライアントの構築を、指定のクライアントを返すように差し替える。
   *
   * @param client 差し替えるクライアントのモック
   * @return 差し替えを解除するためのハンドル
   */
  private static MockedStatic<S3Client> S3を差し替える(final S3Client client) {
    S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(client);
    MockedStatic<S3Client> statics = mockStatic(S3Client.class);
    statics.when(S3Client::builder).thenReturn(builder);
    return statics;
  }

  /**
   * パラメータの応答を作る。パラメータ名は要求されたパスに値のキーを連結したものにする。
   *
   * @param path 要求されたパス
   * @param values キーと値の組
   * @param nextToken 次のページのトークン。最後のページは null
   * @return パラメータの応答
   */
  private static GetParametersByPathResponse パラメータの応答(
      final String path, final Map<String, String> values, final String nextToken) {
    return GetParametersByPathResponse.builder()
        .parameters(
            values.entrySet().stream()
                .map(
                    entry ->
                        Parameter.builder()
                            .name(path + entry.getKey())
                            .value(entry.getValue())
                            .build())
                .toList())
        .nextToken(nextToken)
        .build();
  }

  /**
   * 設定ファイルの内容を読み込むための応答を作る。
   *
   * @param content 設定ファイルの内容
   * @return S3 の応答の入力ストリーム
   */
  private static ResponseInputStream<GetObjectResponse> 設定ファイル(final String content) {
    return new ResponseInputStream<>(
        GetObjectResponse.builder().build(),
        AbortableInputStream.create(
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
  }

  /** 本番の読み込み元として、静的初期化の読み込み処理を実行する。 */
  private static void 本番の読み込みを実行する() {
    読み込み元を設定する("production");
    try {
      Properties.initializeAtStartup();
    } finally {
      読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);
    }
  }

  /**
   * SSM が環境設定バケット名を返し、S3 が設定ファイルを返すモックを作る。
   *
   * @param ssm SSM のモック
   * @param s3 S3 のモック
   * @param content 設定ファイルの内容
   */
  private static void 本番の読み込みのモックを設定する(
      final SsmClient ssm, final S3Client s3, final String content) {
    when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
        .thenAnswer(
            invocation -> {
              GetParametersByPathRequest request = invocation.getArgument(0);
              return パラメータの応答(request.path(), Map.of(BUCKET_KEY, "bucket-x"), null);
            });
    when(s3.getObject(any(GetObjectRequest.class))).thenReturn(設定ファイル(content));
  }

  @Nested
  class initializeAtStartup {

    @Nested
    class 正常系 {

      /**
       * 読み込み元が本番の場合は、環境設定バケットの設定ファイルから設定値を登録することを確かめる。 本番の起動時の読み込み手順が変わると設定値の取得結果が変わるので、手順を固定する。
       *
       * @given 読み込み元が本番、SSM が環境設定バケット名 "bucket-x" を返し、S3 が "A=1" の設定ファイルを返す
       * @when initializeAtStartup を実行する
       * @then 設定ファイルのキーと値を登録し、バケット名 "bucket-x" から取得する
       */
      @Test
      void 読み込み元が本番の場合_設定値を更新する() {
        SsmClient ssm = mock(SsmClient.class);
        S3Client s3 = mock(S3Client.class);
        本番の読み込みのモックを設定する(ssm, s3, "A=1\n");
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);

        try (MockedStatic<SsmClient> ssmStatic = SSMを差し替える(ssm);
            MockedStatic<S3Client> s3Static = S3を差し替える(s3)) {
          本番の読み込みを実行する();

          verify(s3).getObject(captor.capture());
          assertThat(captor.getValue().bucket()).isEqualTo("bucket-x");
          assertThat(Properties.get("A")).isEqualTo("1");
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * SSM のクライアントの生成に失敗した場合は、例外を外に出さずに終わることを確かめる。 設定の読み込みの失敗で起動全体を止めるとサービスが使えなくなるので、例外を外に出さない。
       *
       * @given 読み込み元が本番、SSM のクライアントの構築が例外を投げる
       * @when initializeAtStartup を実行する
       * @then 例外を投げずに終わる
       */
      @Test
      void SSMクライアントの生成に失敗した場合_何もしない() {
        読み込み元を設定する("production");
        try (MockedStatic<SsmClient> ssmStatic = mockStatic(SsmClient.class)) {
          ssmStatic.when(SsmClient::builder).thenThrow(new IllegalStateException("生成失敗"));

          assertThatCode(Properties::initializeAtStartup).doesNotThrowAnyException();
        } finally {
          読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 読み込み元が memory の場合は AWS のクライアントを作らないことを確かめる。 テストでオフラインのまま設定値を扱えないとテストが外部に依存するので、AWS を呼ばない。
       *
       * @given 読み込み元が memory、SSM と S3 のクライアントの構築を差し替える
       * @when initializeAtStartup を実行する
       * @then AWS のクライアントの生成を呼ばない
       */
      @Test
      void 読み込み元がmemoryの場合_AWSのクライアントの生成を呼ばない() {
        try (MockedStatic<SsmClient> ssmStatic = mockStatic(SsmClient.class);
            MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
          Properties.initializeAtStartup();

          ssmStatic.verify(SsmClient::builder, never());
          s3Static.verify(S3Client::builder, never());
        }
      }

      /**
       * 環境設定バケット名が Parameter Store に無い場合は、S3 から設定ファイルを取得しないことを確かめる。 バケット名が無い状態で S3
       * に問い合わせると無効な要求になるので、取得を行わない。
       *
       * @given 読み込み元が本番、Parameter Store が環境設定バケット名を返さない
       * @when initializeAtStartup を実行する
       * @then S3 からの取得を呼ばない
       */
      @Test
      void 環境設定バケット名が未解決の場合_S3からの取得を呼ばない() {
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenReturn(GetParametersByPathResponse.builder().build());
        S3Client s3 = mock(S3Client.class);

        try (MockedStatic<SsmClient> ssmStatic = SSMを差し替える(ssm);
            MockedStatic<S3Client> s3Static = S3を差し替える(s3)) {
          本番の読み込みを実行する();

          verify(s3, never()).getObject(any(GetObjectRequest.class));
        }
      }
    }
  }

  @Nested
  class resolveEnvironment {

    @Nested
    class 正常系 {

      /**
       * ENVIRONMENT の設定がある場合は、その値を返すことを確かめる。 明示した環境名を無視すると誤った環境の設定が読まれるので、その値を優先する。
       *
       * @given 環境変数 ENVIRONMENT が " prod "
       * @when resolveEnvironment を環境変数の関数で実行する
       * @then 前後の空白を除いた "prod" を返す
       */
      @Test
      void ENVIRONMENTが設定されている場合_その値を返す() {
        assertThat(Properties.resolveEnvironment(環境変数(Map.of("ENVIRONMENT", " prod "))))
            .isEqualTo("prod");
      }

      /**
       * ENVIRONMENT が空白で AWS_REGION に prod を含む場合は、prod を返すことを確かめる。
       * 空の指定を有効な値とすると環境名が空になるので、リージョンで判定する。
       *
       * @given 環境変数 ENVIRONMENT が " "、AWS_REGION が "ap-prod-1"
       * @when resolveEnvironment を環境変数の関数で実行する
       * @then "prod" を返す
       */
      @Test
      void ENVIRONMENTが空白でAWS_REGIONにprodを含む場合_prodを返す() {
        assertThat(
                Properties.resolveEnvironment(
                    環境変数(Map.of("ENVIRONMENT", "   ", "AWS_REGION", "ap-prod-1"))))
            .isEqualTo("prod");
      }

      /**
       * AWS_REGION に prod を含まない場合は dev を返すことを確かめる。 本番以外のリージョンを本番扱いすると開発の設定が本番になるので、dev を返す。
       *
       * @given 環境変数 AWS_REGION が "ap-northeast-1"
       * @when resolveEnvironment を環境変数の関数で実行する
       * @then "dev" を返す
       */
      @Test
      void AWS_REGIONにprodを含まない場合_devを返す() {
        assertThat(Properties.resolveEnvironment(環境変数(Map.of("AWS_REGION", "ap-northeast-1"))))
            .isEqualTo("dev");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 環境変数が何も無い場合は dev を返すことを確かめる。 未設定の既定の環境名を変えると読み込み先が変わるので、dev を固定する。
       *
       * @given 環境変数が未設定
       * @when resolveEnvironment を環境変数の関数で実行する
       * @then "dev" を返す
       */
      @Test
      void 環境変数が未設定の場合_devを返す() {
        assertThat(Properties.resolveEnvironment(環境変数(Map.of()))).isEqualTo("dev");
      }
    }
  }

  @Nested
  class resolveS3Region {

    @Nested
    class 正常系 {

      /**
       * AWS_REGION が有効な値の場合は、そのリージョンを返すことを確かめる。 設定されたリージョンを使わないと別のリージョンに接続するので、その値を使う。
       *
       * @given 環境変数 AWS_REGION が "us-east-1"
       * @when resolveS3Region を環境変数の関数で実行する
       * @then US_EAST_1 のリージョンを返す
       */
      @Test
      void AWS_REGIONが有効な場合_そのリージョンを返す() {
        assertThat(Properties.resolveS3Region(環境変数(Map.of("AWS_REGION", "us-east-1"))))
            .isEqualTo(Region.US_EAST_1);
      }
    }
  }

  @Nested
  class buildS3ClientForConfigLoad {

    @Nested
    class 正常系 {

      /**
       * 引数なしで呼んだ場合も、構築したクライアントを返すことを確かめる。 本番の環境変数を読む入口で生成処理が分かれると設定の差が出るので、同じ生成処理を通す。
       *
       * @given S3 のクライアントの構築を差し替え、構築結果がモックのクライアント
       * @when 引数なしの buildS3ClientForConfigLoad を実行する
       * @then 構築したクライアントを返す
       */
      @Test
      void 環境変数を読んで生成した場合_S3クライアントを返す() {
        S3Client s3 = mock(S3Client.class);

        try (MockedStatic<S3Client> s3Static = S3を差し替える(s3)) {
          assertThat(Properties.buildS3ClientForConfigLoad()).isSameAs(s3);
        }
      }
    }
  }

  @Nested
  class buildS3ClientForConfigLoad_UnaryOperator {

    @Nested
    class 正常系 {

      /**
       * エンドポイントが未設定の場合は既定の資格情報を使い、リージョンを設定することを確かめる。 既定の接続先が変わると本番の接続が変わるので、既定の資格情報を固定する。
       *
       * @given 環境変数に AWS_ENDPOINT_URL が無く、AWS_REGION が "us-east-1"
       * @when buildS3ClientForConfigLoad を環境変数の関数で実行する
       * @then 既定の資格情報を使うクライアントを返す
       */
      @Test
      void エンドポイントが未設定の場合_既定の資格情報を使うクライアントを返す() {
        S3Client s3 = mock(S3Client.class);
        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(s3);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
          s3Static.when(S3Client::builder).thenReturn(builder);

          assertThat(Properties.buildS3ClientForConfigLoad(環境変数(Map.of("AWS_REGION", "us-east-1"))))
              .isSameAs(s3);
          verify(builder).region(Region.US_EAST_1);
          verify(builder, never()).endpointOverride(any(URI.class));
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue()).isInstanceOf(DefaultCredentialsProvider.class);
        }
      }

      /**
       * エンドポイントと資格情報が環境変数にある場合は、そのURLをパススタイルで指定することを確かめる。
       * ローカル検証の接続先と資格情報を使えないと検証環境に接続できないので、指定の値を使う。
       *
       * @given 環境変数の AWS_ENDPOINT_URL が "http://localhost:4566"、資格情報が "ak" と "sk"
       * @when buildS3ClientForConfigLoad を環境変数の関数で実行する
       * @then 指定の URL をパススタイルで使い、資格情報 "ak" を使うクライアントを返す
       */
      @Test
      void エンドポイントと資格情報が設定されている場合_指定のURLを使うクライアントを返す() {
        S3Client s3 = mock(S3Client.class);
        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(s3);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
          s3Static.when(S3Client::builder).thenReturn(builder);

          assertThat(
                  Properties.buildS3ClientForConfigLoad(
                      環境変数(
                          Map.of(
                              "AWS_REGION", "us-east-1",
                              "AWS_ENDPOINT_URL", "http://localhost:4566",
                              "AWS_ACCESS_KEY_ID", "ak",
                              "AWS_SECRET_ACCESS_KEY", "sk"))))
              .isSameAs(s3);
          verify(builder).endpointOverride(URI.create("http://localhost:4566"));
          verify(builder).forcePathStyle(true);
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue().resolveCredentials().accessKeyId()).isEqualTo("ak");
        }
      }

      /**
       * エンドポイントだけが設定されている場合は、固定の資格情報を使うことを確かめる。 資格情報が無いと生成できない設計だとローカル検証が始められないので、固定の資格情報を使う。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、資格情報が未設定
       * @when buildS3ClientForConfigLoad を環境変数の関数で実行する
       * @then 固定の資格情報 "test" を使うクライアントを返す
       */
      @Test
      void エンドポイントのみ設定されている場合_固定の資格情報を使うクライアントを返す() {
        S3Client s3 = mock(S3Client.class);
        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(s3);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
          s3Static.when(S3Client::builder).thenReturn(builder);

          assertThat(
                  Properties.buildS3ClientForConfigLoad(
                      環境変数(Map.of("AWS_ENDPOINT_URL", "http://localhost:4566"))))
              .isSameAs(s3);
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue().resolveCredentials().accessKeyId()).isEqualTo("test");
        }
      }

      /**
       * エンドポイントと資格情報の片方だけが設定されている場合は、固定の資格情報を使うことを確かめる。
       * 資格情報が片方だけの場合に既定へ戻すと意図しない資格情報が使われるので、固定の資格情報にする。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、AWS_ACCESS_KEY_ID が "ak" だけ
       * @when buildS3ClientForConfigLoad を環境変数の関数で実行する
       * @then 固定の資格情報 "test" を使うクライアントを返す
       */
      @Test
      void エンドポイントと資格情報の片方だけが設定されている場合_固定の資格情報を使うクライアントを返す() {
        S3Client s3 = mock(S3Client.class);
        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(s3);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
          s3Static.when(S3Client::builder).thenReturn(builder);

          assertThat(
                  Properties.buildS3ClientForConfigLoad(
                      環境変数(
                          Map.of(
                              "AWS_ENDPOINT_URL", "http://localhost:4566",
                              "AWS_ACCESS_KEY_ID", "ak"))))
              .isSameAs(s3);
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue().resolveCredentials().accessKeyId()).isEqualTo("test");
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * エンドポイントの URL が無効な場合は、既定の資格情報を使うことを確かめる。 誤った接続先で処理全体を止めると起動できなくなるので、既定の資格情報で作る。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://bad host"
       * @when buildS3ClientForConfigLoad を環境変数の関数で実行する
       * @then 既定の資格情報を使い、エンドポイントを指定しないクライアントを返す
       */
      @Test
      void エンドポイントが無効な場合_既定の資格情報を使うクライアントを返す() {
        S3Client s3 = mock(S3Client.class);
        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(s3);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<S3Client> s3Static = mockStatic(S3Client.class)) {
          s3Static.when(S3Client::builder).thenReturn(builder);

          assertThat(
                  Properties.buildS3ClientForConfigLoad(
                      環境変数(Map.of("AWS_ENDPOINT_URL", "http://bad host"))))
              .isSameAs(s3);
          verify(builder, never()).endpointOverride(any(URI.class));
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue()).isInstanceOf(DefaultCredentialsProvider.class);
        }
      }
    }
  }

  @Nested
  class buildSsmClientForConfigLoad {

    @Nested
    class 正常系 {

      /**
       * 引数なしで呼んだ場合も、構築したクライアントを返すことを確かめる。 本番の環境変数を読む入口で生成処理が分かれると設定の差が出るので、同じ生成処理を通す。
       *
       * @given SSM のクライアントの構築を差し替え、構築結果がモックのクライアント
       * @when 引数なしの buildSsmClientForConfigLoad を実行する
       * @then 構築したクライアントを返す
       */
      @Test
      void 環境変数を読んで生成した場合_SSMクライアントを返す() {
        SsmClient ssm = mock(SsmClient.class);

        try (MockedStatic<SsmClient> ssmStatic = SSMを差し替える(ssm)) {
          assertThat(Properties.buildSsmClientForConfigLoad()).isSameAs(ssm);
        }
      }

      /**
       * エンドポイントと資格情報が環境変数にある場合は、そのURLを指定することを確かめる。 ローカル検証用の SSM の接続先を使えないと検証環境で動かせないので、指定の値を使う。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、資格情報の "ak" と "sk" がある
       * @when buildSsmClientForConfigLoad を環境変数の関数で実行する
       * @then 指定の URL と資格情報 "ak" を使うクライアントを返す
       */
      @Test
      void エンドポイントと資格情報が設定されている場合_指定のURLを使うクライアントを返す() {
        SsmClient ssm = mock(SsmClient.class);
        SsmClientBuilder builder = mock(SsmClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(ssm);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<SsmClient> ssmStatic = mockStatic(SsmClient.class)) {
          ssmStatic.when(SsmClient::builder).thenReturn(builder);

          assertThat(
                  Properties.buildSsmClientForConfigLoad(
                      環境変数(
                          Map.of(
                              "AWS_ENDPOINT_URL", "http://localhost:4566",
                              "AWS_ACCESS_KEY_ID", "ak",
                              "AWS_SECRET_ACCESS_KEY", "sk"))))
              .isSameAs(ssm);
          verify(builder).endpointOverride(URI.create("http://localhost:4566"));
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue().resolveCredentials().accessKeyId()).isEqualTo("ak");
        }
      }

      /**
       * エンドポイントだけが設定されている場合は、固定の資格情報を使うことを確かめる。 資格情報が片方だけの場合に既定へ戻すと意図しない資格情報が使われるので、固定の資格情報にする。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、資格情報の片方だけ "ak"
       * @when buildSsmClientForConfigLoad を環境変数の関数で実行する
       * @then 固定の資格情報 "test" を使うクライアントを返す
       */
      @Test
      void エンドポイントと資格情報の片方だけが設定されている場合_固定の資格情報を使うクライアントを返す() {
        SsmClient ssm = mock(SsmClient.class);
        SsmClientBuilder builder = mock(SsmClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(ssm);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<SsmClient> ssmStatic = mockStatic(SsmClient.class)) {
          ssmStatic.when(SsmClient::builder).thenReturn(builder);

          assertThat(
                  Properties.buildSsmClientForConfigLoad(
                      環境変数(
                          Map.of(
                              "AWS_ENDPOINT_URL", "http://localhost:4566",
                              "AWS_ACCESS_KEY_ID", "ak"))))
              .isSameAs(ssm);
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue().resolveCredentials().accessKeyId()).isEqualTo("test");
        }
      }

      /**
       * エンドポイントが未設定の場合は既定の資格情報を使うことを確かめる。 本番の既定の接続先が変わると本番の接続が変わるので、既定を固定する。
       *
       * @given 環境変数に AWS_ENDPOINT_URL がない
       * @when buildSsmClientForConfigLoad を環境変数の関数で実行する
       * @then 既定の資格情報を使うクライアントを返す
       */
      @Test
      void エンドポイントが未設定の場合_既定の資格情報を使うクライアントを返す() {
        SsmClient ssm = mock(SsmClient.class);
        SsmClientBuilder builder = mock(SsmClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(ssm);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<SsmClient> ssmStatic = mockStatic(SsmClient.class)) {
          ssmStatic.when(SsmClient::builder).thenReturn(builder);

          assertThat(Properties.buildSsmClientForConfigLoad(環境変数(Map.of()))).isSameAs(ssm);
          verify(builder, never()).endpointOverride(any(URI.class));
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue()).isInstanceOf(DefaultCredentialsProvider.class);
        }
      }

      /**
       * エンドポイントだけが設定されている場合は、固定の資格情報を使うことを確かめる。 資格情報が無いローカル検証でもクライアントを作れないと検証が始められないので、固定の資格情報を使う。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、資格情報が未設定
       * @when buildSsmClientForConfigLoad を環境変数の関数で実行する
       * @then 固定の資格情報 "test" を使うクライアントを返す
       */
      @Test
      void エンドポイントのみ設定されている場合_固定の資格情報を使うクライアントを返す() {
        SsmClient ssm = mock(SsmClient.class);
        SsmClientBuilder builder = mock(SsmClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(ssm);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<SsmClient> ssmStatic = mockStatic(SsmClient.class)) {
          ssmStatic.when(SsmClient::builder).thenReturn(builder);

          assertThat(
                  Properties.buildSsmClientForConfigLoad(
                      環境変数(Map.of("AWS_ENDPOINT_URL", "http://localhost:4566"))))
              .isSameAs(ssm);
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue().resolveCredentials().accessKeyId()).isEqualTo("test");
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * エンドポイントの URL が無効な場合は、既定の資格情報を使うことを確かめる。 誤った接続先で処理全体を止めると起動できなくなるので、既定の資格情報で作る。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://bad host"
       * @when buildSsmClientForConfigLoad を環境変数の関数で実行する
       * @then 既定の資格情報を使い、エンドポイントを指定しないクライアントを返す
       */
      @Test
      void エンドポイントが無効な場合_既定の資格情報を使うクライアントを返す() {
        SsmClient ssm = mock(SsmClient.class);
        SsmClientBuilder builder = mock(SsmClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(ssm);
        ArgumentCaptor<AwsCredentialsProvider> captor =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);

        try (MockedStatic<SsmClient> ssmStatic = mockStatic(SsmClient.class)) {
          ssmStatic.when(SsmClient::builder).thenReturn(builder);

          assertThat(
                  Properties.buildSsmClientForConfigLoad(
                      環境変数(Map.of("AWS_ENDPOINT_URL", "http://bad host"))))
              .isSameAs(ssm);
          verify(builder, never()).endpointOverride(any(URI.class));
          verify(builder).credentialsProvider(captor.capture());
          assertThat(captor.getValue()).isInstanceOf(DefaultCredentialsProvider.class);
        }
      }
    }
  }

  @Nested
  class loadFromParameterStore {

    @Nested
    class 正常系 {

      /**
       * パラメータが 1 ページに収まる場合は、キーの接頭辞を除いて設定値を登録することを確かめる。 接頭辞を除かないとアプリケーションのキーで参照できないので、接頭辞を除いて登録する。
       *
       * @given SSM がパス配下の "a/b" に値 "v1" を1ページで返す
       * @when loadFromParameterStore を SSM のモックで実行する
       * @then 接頭辞を除いたキー "a/b" に "v1" を登録する
       */
      @Test
      void パラメータが1ページに収まる場合_接頭辞を除いたキーを登録する() {
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenAnswer(
                invocation -> {
                  GetParametersByPathRequest request = invocation.getArgument(0);
                  return パラメータの応答(request.path(), Map.of("a/b", "v1"), null);
                });

        Properties.loadFromParameterStore(ssm);

        assertThat(Properties.get("a/b")).isEqualTo("v1");
      }

      /**
       * 次のページがある場合は、全ページのパラメータを登録することを確かめる。 1 ページ目だけ読むと残りの設定値が欠けるので、次のページも読む。
       *
       * @given SSM が1ページ目に "x/1" と次のトークン、2ページ目に "x/2" を返す
       * @when loadFromParameterStore を SSM のモックで実行する
       * @then 両方のページのパラメータを登録し、SSM を 2 回呼ぶ
       */
      @Test
      void 次のページがある場合_全ページのパラメータを登録する() {
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenAnswer(
                invocation -> {
                  GetParametersByPathRequest request = invocation.getArgument(0);
                  if (request.nextToken() == null) {
                    return パラメータの応答(request.path(), Map.of("x/1", "v1"), "T1");
                  }
                  return パラメータの応答(request.path(), Map.of("x/2", "v2"), null);
                });

        Properties.loadFromParameterStore(ssm);

        assertThat(Properties.get("x/1")).isEqualTo("v1");
        assertThat(Properties.get("x/2")).isEqualTo("v2");
        verify(ssm, times(2)).getParametersByPath(any(GetParametersByPathRequest.class));
      }

      /**
       * CACHE_TTL_MS が 1000 の場合、期限が切れた後は設定値を更新することを確かめる。 TTL の設定値で再読み込みの間隔が変わると更新の遅れが変わるので、TTL
       * を反映する。
       *
       * @given CACHE_TTL_MS が "1000"、1回目の読み込み後に 2 秒経過し、2回目の値が "v2"
       * @when loadFromParameterStore を 2 回続けて実行する
       * @then 2 回目の読み込みで "v2" を登録し、SSM を 2 回呼ぶ
       */
      @Test
      void CACHE_TTL_MSが1000の場合_期限切れ後に設定値を更新する() {
        Properties.putForTest("CACHE_TTL_MS", "1000");
        String[] value = {"v1"};
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenAnswer(
                invocation -> {
                  GetParametersByPathRequest request = invocation.getArgument(0);
                  return パラメータの応答(request.path(), Map.of("a/b", value[0]), null);
                });
        Properties.loadFromParameterStore(ssm);
        value[0] = "v2";
        時計を設定する(Instant.parse("2026-01-15T03:00:02Z"));

        Properties.loadFromParameterStore(ssm);

        assertThat(Properties.get("a/b")).isEqualTo("v2");
        verify(ssm, times(2)).getParametersByPath(any(GetParametersByPathRequest.class));
      }
    }

    @Nested
    class 異常系 {

      /**
       * Parameter Store の呼び出しに失敗した場合は、例外を外に出さずに終わることを確かめる。 取得の失敗で起動全体を止めるとサービスが使えなくなるので、例外を外に出さない。
       *
       * @given SSM の呼び出しが例外を投げる
       * @when loadFromParameterStore を SSM のモックで実行する
       * @then 例外を投げずに終わり、設定値を登録しない
       */
      @Test
      void Parameter_Storeの呼び出しに失敗した場合_何もしない() {
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenThrow(new RuntimeException("呼び出し失敗"));

        assertThatCode(() -> Properties.loadFromParameterStore(ssm)).doesNotThrowAnyException();
        assertThat(Properties.get("a/b")).isEmpty();
      }
    }

    @Nested
    class 境界値 {

      /**
       * キャッシュが有効な間は、2 回目の読み込みで Parameter Store を呼ばないことを確かめる。 キャッシュが効かないと Parameter Store
       * への問い合わせが増えるので、期間内は呼ばない。
       *
       * @given SSM が1ページのパラメータを返す、時計が変わらない
       * @when loadFromParameterStore を 2 回続けて実行する
       * @then SSM を 1 回だけ呼ぶ
       */
      @Test
      void キャッシュが有効な場合_Parameter_Storeを呼ばない() {
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenAnswer(
                invocation -> {
                  GetParametersByPathRequest request = invocation.getArgument(0);
                  return パラメータの応答(request.path(), Map.of("a/b", "v1"), null);
                });
        Properties.loadFromParameterStore(ssm);

        Properties.loadFromParameterStore(ssm);

        verify(ssm, times(1)).getParametersByPath(any(GetParametersByPathRequest.class));
      }

      /**
       * CACHE_TTL_MS が数値でない場合は、既定の 1 日の期間内はキャッシュを使うことを確かめる。 不正な TTL
       * の値で再読み込みが頻発すると問い合わせが増えるので、既定の期間を使う。
       *
       * @given CACHE_TTL_MS が "abc"、時計が変わらない
       * @when loadFromParameterStore を 2 回続けて実行する
       * @then SSM を 1 回だけ呼ぶ
       */
      @Test
      void CACHE_TTL_MSが数値でない場合_既定の期間内はParameter_Storeを呼ばない() {
        Properties.putForTest("CACHE_TTL_MS", "abc");
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenReturn(GetParametersByPathResponse.builder().build());
        Properties.loadFromParameterStore(ssm);

        Properties.loadFromParameterStore(ssm);

        verify(ssm, times(1)).getParametersByPath(any(GetParametersByPathRequest.class));
      }

      /**
       * CACHE_TTL_MS が空の場合は、既定の 1 日の期間内はキャッシュを使うことを確かめる。 空の TTL を不正な値として扱うと既定の期間にならないので、未設定と同じに扱う。
       *
       * @given CACHE_TTL_MS が空文字、時計が変わらない
       * @when loadFromParameterStore を 2 回続けて実行する
       * @then SSM を 1 回だけ呼ぶ
       */
      @Test
      void CACHE_TTL_MSが空の場合_既定の期間内はParameter_Storeを呼ばない() {
        Properties.putForTest("CACHE_TTL_MS", "");
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenReturn(GetParametersByPathResponse.builder().build());
        Properties.loadFromParameterStore(ssm);

        Properties.loadFromParameterStore(ssm);

        verify(ssm, times(1)).getParametersByPath(any(GetParametersByPathRequest.class));
      }

      /**
       * AWS_REGION が未設定の場合は東京リージョンを返すことを確かめる。 設定がない環境でも S3 の接続先が決まるようにするため、既定のリージョンを固定する。
       *
       * @given 環境変数 AWS_REGION が未設定
       * @when resolveS3Region を環境変数の関数で実行する
       * @then AP_NORTHEAST_1 のリージョンを返す
       */
      @Test
      void AWS_REGIONが未設定の場合_東京リージョンを返す() {
        assertThat(Properties.resolveS3Region(環境変数(Map.of()))).isEqualTo(Region.AP_NORTHEAST_1);
      }

      /**
       * AWS_REGION が空白だけの場合は東京リージョンを返すことを確かめる。 空白の設定を無効な値として扱い、既定のリージョンへ戻すことを守る。
       *
       * @given 環境変数 AWS_REGION が空白だけの文字列
       * @when resolveS3Region を環境変数の関数で実行する
       * @then AP_NORTHEAST_1 のリージョンを返す
       */
      @Test
      void AWS_REGIONが空白だけの場合_東京リージョンを返す() {
        assertThat(Properties.resolveS3Region(環境変数(Map.of("AWS_REGION", "   "))))
            .isEqualTo(Region.AP_NORTHEAST_1);
      }
    }
  }

  @Nested
  class load {

    @Nested
    class 正常系 {

      /**
       * 設定ファイルを読み込んだ場合は、キーと値を登録し、区切りのない行は無視することを確かめる。 設定ファイルの書式の解釈が変わると設定値の意味が変わるので、解釈を固定する。
       *
       * @given 読み込み済みの状態、キャッシュの期限が切れている、設定ファイルに "B = 2 = x" の行を含む
       * @when load を S3 のモックで実行する
       * @then "B" に "2 = x"、"C" に "3" を登録し、区切りのない行は登録しない
       */
      @Test
      void 設定ファイルを読み込んだ場合_キーと値を登録する() {
        S3Client s3 = mock(S3Client.class);
        読み込み済みにする(s3);
        時計を2日後にする();
        when(s3.getObject(any(GetObjectRequest.class)))
            .thenReturn(設定ファイル("B = 2 = x\n\nnoequal\nC=3\n"));

        Properties.load(s3);

        assertThat(Properties.get("B")).isEqualTo("2 = x");
        assertThat(Properties.get("C")).isEqualTo("3");
        assertThat(Properties.get("noequal")).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 設定ファイルの読み込みで入出力が失敗した場合は、例外を外に出さずに終わることを確かめる。
       * 設定ファイルの一時的な失敗で起動全体を止めるとサービスが使えなくなるので、例外を外に出さない。
       *
       * @given 読み込み済みの状態、キャッシュの期限が切れている、読み込み中に入出力例外が起きる
       * @when load を S3 のモックで実行する
       * @then 例外を投げずに終わる
       */
      @Test
      void 設定ファイルの読み込みに失敗した場合_何もしない() {
        S3Client s3 = mock(S3Client.class);
        読み込み済みにする(s3);
        時計を2日後にする();
        InputStream failing =
            new InputStream() {
              @Override
              public int read() throws IOException {
                throw new IOException("読み込み失敗");
              }
            };
        when(s3.getObject(any(GetObjectRequest.class)))
            .thenReturn(
                new ResponseInputStream<>(
                    GetObjectResponse.builder().build(), AbortableInputStream.create(failing)));

        assertThatCode(() -> Properties.load(s3)).doesNotThrowAnyException();
      }
    }

    @Nested
    class 境界値 {

      /**
       * キャッシュが有効な間は、S3 から設定ファイルを取得しないことを確かめる。 キャッシュが効かないと S3 への問い合わせが増えるので、期間内は取得しない。
       *
       * @given 読み込み済みの状態、時計が変わらない
       * @when load を S3 のモックで実行する
       * @then S3 からの取得は初回の 1 回だけになる
       */
      @Test
      void キャッシュが有効な場合_S3からの取得を呼ばない() {
        S3Client s3 = mock(S3Client.class);
        読み込み済みにする(s3);

        Properties.load(s3);

        verify(s3, times(1)).getObject(any(GetObjectRequest.class));
      }

      /**
       * 環境設定バケット名が未解決の場合は、S3 から設定ファイルを取得しないことを確かめる。 バケット名が無い状態で S3 に問い合わせると無効な要求になるので、取得を行わない。
       *
       * @given 読み込み済みの状態から設定値を初期化し、バケット名が未解決
       * @when load を S3 のモックで実行する
       * @then S3 からの取得を呼ばない
       */
      @Test
      void 環境設定バケット名が未解決の場合_S3からの取得を呼ばない() {
        S3Client s3 = mock(S3Client.class);
        読み込み済みにする(s3);
        Properties.resetForTest();
        時計を2日後にする();

        Properties.load(s3);

        verify(s3, times(1)).getObject(any(GetObjectRequest.class));
      }
    }
  }

  /**
   * 本番の読み込みを 1 回実行し、設定値と S3 の最終読み込み時刻を読み込み済みの状態にする。
   *
   * @param s3 設定ファイルを返す S3 のモック
   */
  private static void 読み込み済みにする(final S3Client s3) {
    SsmClient ssm = mock(SsmClient.class);
    本番の読み込みのモックを設定する(ssm, s3, "A=1\n");
    try (MockedStatic<SsmClient> ssmStatic = SSMを差し替える(ssm);
        MockedStatic<S3Client> s3Static = S3を差し替える(s3)) {
      本番の読み込みを実行する();
    }
  }

  @Nested
  class get {

    @Nested
    class 正常系 {

      /**
       * 登録されているキーの値を返すことを確かめる。 登録した設定値を取り出せないと設定が使えないので、値を返す。
       *
       * @given キー "k" に値 "v" を登録している
       * @when get を "k" で実行する
       * @then "v" を返す
       */
      @Test
      void 登録されているキーの場合_値を返す() {
        Properties.putForTest("k", "v");

        assertThat(Properties.get("k")).isEqualTo("v");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未登録のキーは空文字を返すことを確かめる。 未登録の値を null にすると呼び出し側で null 判定が必要になるので、空文字を返す。
       *
       * @given キー "missing" が未登録
       * @when get を "missing" で実行する
       * @then 空文字を返す
       */
      @Test
      void 未登録のキーの場合_空文字を返す() {
        assertThat(Properties.get("missing")).isEmpty();
      }
    }
  }

  @Nested
  class getInt {

    @Nested
    class 正常系 {

      /**
       * 数値の文字列を整数として返すことを確かめる。 数値の設定を整数として使えないと設定値の利用に支障が出るので、整数に変換する。
       *
       * @given キー "n" に値 "12" を登録している
       * @when getInt を "n" で実行する
       * @then 12 を返す
       */
      @Test
      void 数値の文字列の場合_整数を返す() {
        Properties.putForTest("n", "12");

        assertThat(Properties.getInt("n")).isEqualTo(12);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 数値でない文字列は NumberFormatException を投げることを確かめる。 誤った数値の設定を 0 にすると設定の誤りが見えなくなるので、例外を投げる。
       *
       * @given キー "n" に値 "abc" を登録している
       * @when getInt を "n" で実行する
       * @then NumberFormatException を投げる
       */
      @Test
      void 数値でない文字列の場合_NumberFormatExceptionを投げる() {
        Properties.putForTest("n", "abc");

        assertThatThrownBy(() -> Properties.getInt("n")).isInstanceOf(NumberFormatException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未登録のキーは 0 を返すことを確かめる。 未登録の数値を 0 として扱うと既定の動作が決まるので、0 を返す。
       *
       * @given キー "missing" が未登録
       * @when getInt を "missing" で実行する
       * @then 0 を返す
       */
      @Test
      void 未登録のキーの場合_0を返す() {
        assertThat(Properties.getInt("missing")).isZero();
      }
    }
  }

  @Nested
  class getDouble {

    @Nested
    class 正常系 {

      /**
       * 小数の文字列を小数として返すことを確かめる。 小数の設定を小数として使えないと設定値の利用に支障が出るので、小数に変換する。
       *
       * @given キー "d" に値 "1.5" を登録している
       * @when getDouble を "d" で実行する
       * @then 1.5 を返す
       */
      @Test
      void 小数の文字列の場合_小数を返す() {
        Properties.putForTest("d", "1.5");

        assertThat(Properties.getDouble("d")).isEqualTo(1.5);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未登録のキーは 0.0 を返すことを確かめる。 未登録の小数を 0.0 として扱うと既定の動作が決まるので、0.0 を返す。
       *
       * @given キー "missing" が未登録
       * @when getDouble を "missing" で実行する
       * @then 0.0 を返す
       */
      @Test
      void 未登録のキーの場合_0_0を返す() {
        assertThat(Properties.getDouble("missing")).isEqualTo(0.0);
      }
    }
  }

  @Nested
  class putForTest {

    @Nested
    class 正常系 {

      /**
       * キーと値を指定すると設定値を登録することを確かめる。 テストで設定値を入れる口が動かないとテストの前提が作れないので、値を登録する。
       *
       * @given 読み込み元が memory、キー "k" と値 "v"
       * @when putForTest を実行する
       * @then 設定値 "k" に "v" を登録する
       */
      @Test
      void キーと値を指定した場合_設定値を登録する() {
        Properties.putForTest("k", "v");

        assertThat(Properties.get("k")).isEqualTo("v");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が本番の場合は設定値の登録を拒否することを確かめる。 本番で設定値が書き換わると読み込み結果と食い違うので、拒否する。
       *
       * @given 読み込み元が本番で memory 以外
       * @when putForTest を実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 読み込み元が本番の場合_IllegalStateExceptionを投げる() {
        読み込み元を設定する("production");

        assertThatThrownBy(() -> Properties.putForTest("k", "v"))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * キーが null の場合は NullPointerException を投げることを確かめる。 無効なキーを登録すると参照できない値が残るので、null を拒否する。
       *
       * @given キーが null、値が "v"
       * @when putForTest を実行する
       * @then NullPointerException を投げる
       */
      @Test
      void キーがnullの場合_NullPointerExceptionを投げる() {
        assertThatThrownBy(() -> Properties.putForTest(null, "v"))
            .isInstanceOf(NullPointerException.class);
      }

      /**
       * 値が null の場合は NullPointerException を投げることを確かめる。 無効な値を登録すると参照時に例外が起きるので、null を拒否する。
       *
       * @given キーが "k"、値が null
       * @when putForTest を実行する
       * @then NullPointerException を投げる
       */
      @Test
      void 値がnullの場合_NullPointerExceptionを投げる() {
        assertThatThrownBy(() -> Properties.putForTest("k", null))
            .isInstanceOf(NullPointerException.class);
      }
    }
  }

  @Nested
  class removeForTest {

    @Nested
    class 正常系 {

      /**
       * 登録済みのキーを指定すると設定値を削除することを確かめる。 テストで設定値を未設定に戻せないとテスト同士が影響し合うので、削除できるようにする。
       *
       * @given キー "k" に値 "v" を登録している
       * @when removeForTest を "k" で実行する
       * @then 設定値 "k" を削除し、空文字を返す
       */
      @Test
      void 登録済みのキーの場合_設定値を削除する() {
        Properties.putForTest("k", "v");

        Properties.removeForTest("k");

        assertThat(Properties.get("k")).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が本番の場合は設定値の削除を拒否することを確かめる。 本番で設定値が消えると起動時の設定が失われるので、拒否する。
       *
       * @given 読み込み元が本番で memory 以外
       * @when removeForTest を "k" で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 読み込み元が本番の場合_IllegalStateExceptionを投げる() {
        読み込み元を設定する("production");

        assertThatThrownBy(() -> Properties.removeForTest("k"))
            .isInstanceOf(IllegalStateException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未登録のキーを指定した場合は何もしないことを確かめる。 未登録の削除で例外が起きると後始末が途中で止まるので、何もしない。
       *
       * @given キー "missing" が未登録
       * @when removeForTest を "missing" で実行する
       * @then 何もしない
       */
      @Test
      void 未登録のキーの場合_何もしない() {
        assertThatCode(() -> Properties.removeForTest("missing")).doesNotThrowAnyException();
      }
    }
  }

  @Nested
  class resetForTest {

    @Nested
    class 正常系 {

      /**
       * 設定値が登録されている場合は、すべての設定値を削除することを確かめる。 テストごとに設定値を初期化できないと前のテストの値が残るので、全件削除する。
       *
       * @given キー "k" に値 "v" を登録している
       * @when resetForTest を実行する
       * @then すべての設定値を削除し、"k" は空文字を返す
       */
      @Test
      void 設定値が登録されている場合_すべての設定値を削除する() {
        Properties.putForTest("k", "v");

        Properties.resetForTest();

        assertThat(Properties.get("k")).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が本番の場合は初期化を拒否することを確かめる。 本番で設定値が消えると起動時の設定が失われるので、拒否する。
       *
       * @given 読み込み元が本番で memory 以外
       * @when resetForTest を実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 読み込み元が本番の場合_IllegalStateExceptionを投げる() {
        読み込み元を設定する("production");

        assertThatThrownBy(Properties::resetForTest).isInstanceOf(IllegalStateException.class);
      }
    }
  }
}
