package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.ResponseInputStream;
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

/**
 * {@link Properties} の単体テスト。
 *
 * <p>既存のテストは、設定値とキャッシュの状態をテストごとに初期化して実行する。読み込み元は memory に固定し、AWS に接続しない。
 *
 * @author Copel Co., Ltd.
 */
class PropertiesTest {

  /** テストで使う固定時刻の基準点。UTC の "2026-01-15T03:04:05Z" を表す。 */
  private static final Instant BASE_INSTANT = Instant.parse("2026-01-15T03:04:05Z");

  /** テストで使う時計のゾーン。本番の時計と同じ Asia/Tokyo を使う。 */
  private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

  /** システムプロパティのキー。カスタムエンドポイントの指定に使う。AwsEndpointUtil と同じキー。 */
  private static final String AWS_ENDPOINT_PROPERTY = "aws.endpointUrl";

  /** 既定の TTL のミリ秒。1 日分の値で、Properties の既定値と同じ。 */
  private static final long DEFAULT_TTL_MS = 86400000L;

  /** テストの前に設定読み込み元のプロパティの値を退避する。 */
  private String originalConfigSource;

  /** テストの前に退避する、システムプロパティ aws.endpointUrl の値。エンドポイント分岐のテストを環境に依存させないため。 */
  private String originalEndpointUrl;

  /** テストの前に設定読み込み元を memory にし、時計と設定値と読み込みの状態を初期化する。 */
  @BeforeEach
  void setUp() {
    originalConfigSource = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    originalEndpointUrl = System.getProperty(AWS_ENDPOINT_PROPERTY);
    System.clearProperty(AWS_ENDPOINT_PROPERTY);
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    ApplicationClock.INSTANCE.resetToDefault();
    Properties.resetForTest();
  }

  /** テストの後に設定値と読み込みの状態を初期化し、設定読み込み元のプロパティを復元する。 */
  @AfterEach
  void tearDown() {
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    Properties.resetForTest();
    ApplicationClock.INSTANCE.resetToDefault();
    if (originalEndpointUrl == null) {
      System.clearProperty(AWS_ENDPOINT_PROPERTY);
    } else {
      System.setProperty(AWS_ENDPOINT_PROPERTY, originalEndpointUrl);
    }
    if (originalConfigSource == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, originalConfigSource);
    }
  }

  /** Properties のコンストラクタのテスト。 */
  @Nested
  class コンストラクタ {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties のコンストラクタでインスタンスを生成できることを確かめる。 インスタンスを作れないと、クラスを利用する箇所が生成に失敗する。
       *
       * @given クラスの初期化が完了している
       * @when Properties を引数なしで生成する
       * @then インスタンスを返す
       */
      @Test
      void 引数がない場合_インスタンスを返す() {
        assertNotNull(new Properties());
      }
    }
  }

  /** Properties の取得メソッドのテスト。 */
  @Nested
  class get {

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * Properties の取得メソッドが未登録のキーに既定値を返すことを確かめる。 未登録のキーで例外を投げない仕様を守る。
       *
       * @given バケット名が test-bucket、S3 が "A=1" を返し、設定値に MISSING_KEY がない
       * @when load を S3Client で読み込み、get、getInt、getDouble、getAsArray を MISSING_KEY で呼び出す
       * @then get が空文字、getInt が 0、getDouble が 0.0、getAsArray が空の配列を返す
       */
      @Test
      @SuppressWarnings("unchecked")
      void キーが未登録の場合_既定値を返す() throws Exception {
        Field configBucketField = Properties.class.getDeclaredField("CONFIG_BUCKET");
        configBucketField.setAccessible(true);
        configBucketField.set(null, "test-bucket");

        Field field = Properties.class.getDeclaredField("properties");
        field.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) field.get(null);
        propertiesMap.clear();
        S3Client mockS3 = mock(S3Client.class);
        InputStream is = new ByteArrayInputStream("A=1".getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> s3Stream =
            new ResponseInputStream<>(GetObjectResponse.builder().build(), is);
        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);
        Properties.load(mockS3);

        assertEquals("", Properties.get("MISSING_KEY"));
        assertEquals(0, Properties.getInt("MISSING_KEY"));
        assertEquals(0.0, Properties.getDouble("MISSING_KEY"));
        assertArrayEquals(new String[0], Properties.getAsArray("MISSING_KEY"));
      }
    }
  }

  /** {@link Properties#initializeAtStartup()} のテスト。 */
  @Nested
  class initializeAtStartup {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#initializeAtStartup が memory のときに SSM と S3 のクライアントを作らないことを確かめる。 オフラインのテストで AWS
       * に接続を試みないための仕様である。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、SsmClient と S3Client の builder を
       *     mockStatic で差し替えている
       * @when initializeAtStartup を引数なしで呼び出す
       * @then SsmClient と S3Client の builder を呼ばず、設定値が空のままである
       */
      @Test
      void システムプロパティがmemoryの場合_SSMとS3を呼ばない() {
        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class);
            MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          Properties.initializeAtStartup();

          ssm.verifyNoInteractions();
          s3.verifyNoInteractions();
          assertEquals("", Properties.get(SsmParameterKey.ENVIRONMENT_CONFIG_BUCKET_NAME.getKey()));
        }
      }

      /**
       * Properties#initializeAtStartup が本番設定で SSM からバケット名を解決してから S3 を読むことを確かめる。 バケット名が Parameter
       * Store の値に依存するため、この順番を変えると本番で設定を読めなくなる。
       *
       * @given システムプロパティが未設定で、SSM がバケット名 "test-bucket" を返し、S3 の config.properties が "KEY2=VAL2" を返す
       * @when initializeAtStartup を引数なしで呼び出す
       * @then SSM の取得、S3 の取得の順に呼び出し、S3 の取得はバケット "test-bucket" を指定し、設定値 KEY2 を "VAL2" として登録する
       */
      @Test
      void システムプロパティが未設定の場合_SSMからバケット名を解決してからS3の設定値を登録する() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
        SsmClient ssmClient = mock(SsmClient.class);
        when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenAnswer(
                ssmParameterAnswer(
                    SsmParameterKey.ENVIRONMENT_CONFIG_BUCKET_NAME.getKey(), "test-bucket"));
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(configStream("KEY2=VAL2"));
        SsmClientBuilder ssmBuilder = mock(SsmClientBuilder.class, RETURNS_SELF);
        when(ssmBuilder.build()).thenReturn(ssmClient);
        S3ClientBuilder s3Builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        when(s3Builder.build()).thenReturn(s3Client);

        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class);
            MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          ssm.when(SsmClient::builder).thenReturn(ssmBuilder);
          s3.when(S3Client::builder).thenReturn(s3Builder);

          Properties.initializeAtStartup();

          InOrder order = inOrder(ssmClient, s3Client);
          order.verify(ssmClient).getParametersByPath(any(GetParametersByPathRequest.class));
          order
              .verify(s3Client)
              .getObject(
                  argThat((GetObjectRequest request) -> "test-bucket".equals(request.bucket())));
        }
        assertEquals("VAL2", Properties.get("KEY2"));
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * Properties#initializeAtStartup が SSM クライアントの生成に失敗しても例外を投げず、S3 を読まないことを確かめる。 起動時の失敗で Lambda
       * の初期化を止めないための仕様である。
       *
       * @given システムプロパティが未設定で、SsmClient の builder が IllegalStateException を投げる
       * @when initializeAtStartup を引数なしで呼び出す
       * @then 例外を投げず、S3Client の builder を呼ばない
       */
      @Test
      void SSMクライアントの生成が失敗した場合_S3の取得を呼ばない() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class);
            MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          ssm.when(SsmClient::builder)
              .thenThrow(new IllegalStateException("SSM client creation failed"));

          assertDoesNotThrow(Properties::initializeAtStartup);
          s3.verifyNoInteractions();
        }
      }

      /**
       * Properties#initializeAtStartup が Parameter Store にバケット名がない場合に S3 の取得を呼ばないことを確かめる。
       * バケット名が解決できないまま S3 を読むと、失敗の原因がわかりにくくなる。
       *
       * @given システムプロパティが未設定で、SSM の応答にパラメータがなく、SsmClient と S3Client の builder がそれぞれのモックを返す
       * @when initializeAtStartup を引数なしで呼び出す
       * @then S3 の取得を呼ばず、設定値 KEY2 は空文字を返す
       */
      @Test
      void SSMにバケット名がない場合_S3の取得を呼ばない() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
        SsmClient ssmClient = mock(SsmClient.class);
        when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenReturn(GetParametersByPathResponse.builder().build());
        S3Client s3Client = mock(S3Client.class);
        SsmClientBuilder ssmBuilder = mock(SsmClientBuilder.class, RETURNS_SELF);
        when(ssmBuilder.build()).thenReturn(ssmClient);
        S3ClientBuilder s3Builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        when(s3Builder.build()).thenReturn(s3Client);

        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class);
            MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          ssm.when(SsmClient::builder).thenReturn(ssmBuilder);
          s3.when(S3Client::builder).thenReturn(s3Builder);

          Properties.initializeAtStartup();

          verify(s3Client, never()).getObject(any(GetObjectRequest.class));
        }
        assertEquals("", Properties.get("KEY2"));
      }
    }
  }

  /** {@link Properties#load(S3Client)} のうち、TTL とバケット名の分岐のテスト。 */
  @Nested
  class load {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#load が S3 の設定ファイルを読み、各型の取得メソッドで値を取り出せることを確かめる。 設定ファイルの形式を守る仕様である。
       *
       * @given バケット名が test-bucket、S3 が KEY1=VAL1、INT=10、DBL=10.5、ARRAY=A,B,C の 4 行を返す
       * @when load を S3Client で呼び出す
       * @then KEY1 の値として "VAL1"、INT の値として 10、DBL の値として 10.5、ARRAY の値として "A"、"B"、"C" を返す
       */
      @Test
      @SuppressWarnings("unchecked")
      void S3に設定ファイルがある場合_各キーの値を返す() throws Exception {
        Field configBucketField = Properties.class.getDeclaredField("CONFIG_BUCKET");
        configBucketField.setAccessible(true);
        configBucketField.set(null, "test-bucket");

        S3Client mockS3 = mock(S3Client.class);
        String content = "KEY1=VAL1\nINT=10\nDBL=10.5\nARRAY=A,B,C\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> s3Stream =
            new ResponseInputStream<>(GetObjectResponse.builder().build(), is);
        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

        Properties.load(mockS3);

        assertEquals("VAL1", Properties.get("KEY1"));
        assertEquals(10, Properties.getInt("INT"));
        assertEquals(10.5, Properties.getDouble("DBL"));
        assertArrayEquals(new String[] {"A", "B", "C"}, Properties.getAsArray("ARRAY"));
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * Properties#load がバケット名を解決できていない場合に S3 を呼ばないことを確かめる。 バケット名が null のまま S3
       * を呼ぶと、読み込みに失敗するだけで原因がわかりにくい。
       *
       * @given バケット名が未解決の null で、S3 が "KEY1=VAL1" を返す
       * @when load を S3Client で呼び出す
       * @then S3 の取得を呼ばない
       */
      @Test
      void バケット名が未解決の場合_S3を呼ばない() {
        S3Client s3 = mock(S3Client.class);
        when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(configAnswer("KEY1=VAL1"));

        Properties.load(s3);

        verify(s3, never()).getObject(any(GetObjectRequest.class));
      }

      /**
       * Properties#load が S3 の読み込みで IOException が発生しても例外を投げないことを確かめる。 読み込みの失敗で Lambda
       * の初期化を止めない仕様である。
       *
       * @given バケット名が test-bucket、入力ストリームの read が IOException を投げる
       * @when load を S3Client で呼び出す
       * @then 例外を投げない
       */
      @Test
      void S3の読み込みでIOExceptionが発生した場合_何もしない() throws Exception {
        // 読み込み元の例外経路に入るため、バケット名を解決済みの状態にする
        setConfigBucket("test-bucket");
        S3Client mockS3 = mock(S3Client.class);
        InputStream mockIs = mock(InputStream.class);
        when(mockIs.read()).thenThrow(new IOException("Forced IO exception"));
        when(mockIs.read(any(byte[].class), anyInt(), anyInt()))
            .thenThrow(new IOException("Forced IO exception"));

        ResponseInputStream<GetObjectResponse> s3Stream =
            new ResponseInputStream<>(GetObjectResponse.builder().build(), mockIs);

        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

        assertDoesNotThrow(() -> Properties.load(mockS3));
      }
    }

    /** 境界値のテスト。 */
    @Nested
    class 境界値 {

      /**
       * Properties#load が前回の読み込みからTTL未満のとき S3 を呼ばないことを確かめる。
       * 1日以内の再読み込みを防ぐことで、本番の読み込み回数を変えないための仕様である。
       *
       * @given バケット名が "test-bucket"、前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 86399999 ミリ秒後、S3 が
       *     "KEY1=VAL1" を返す
       * @when load を S3Client で 2 回呼び出す
       * @then 2 回目の load で S3 の取得を呼ばず、設定値 KEY1 が "VAL1" のままである
       */
      @Test
      void 前回読み込みからTTL未満の場合_S3を呼ばない() {
        setConfigBucket("test-bucket");
        S3Client s3 = mock(S3Client.class);
        when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(configAnswer("KEY1=VAL1"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        Properties.load(s3);
        ApplicationClock.INSTANCE.setClock(
            fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS - 1)));
        Properties.load(s3);

        verify(s3, times(1)).getObject(any(GetObjectRequest.class));
        assertEquals("VAL1", Properties.get("KEY1"));
      }

      /**
       * Properties#load が前回の読み込みからTTLちょうどのとき S3 から再読み込みすることを確かめる。 TTL の境界で再読み込みが起きないと、設定値の更新が1日遅れる。
       *
       * @given バケット名が "test-bucket"、前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 86400000 ミリ秒後、1 回目の
       *     S3 が "KEY1=VAL1"、2 回目が "KEY1=VAL2" を返す
       * @when load を S3Client で 2 回呼び出す
       * @then 2 回目の load で S3 から読み込み、設定値 KEY1 を "VAL2" に更新する
       */
      @Test
      void 前回読み込みからTTLちょうどの場合_設定値を更新する() {
        setConfigBucket("test-bucket");
        S3Client s3 = mock(S3Client.class);
        when(s3.getObject(any(GetObjectRequest.class)))
            .thenReturn(configStream("KEY1=VAL1"), configStream("KEY1=VAL2"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        Properties.load(s3);
        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS)));
        Properties.load(s3);

        verify(s3, times(2)).getObject(any(GetObjectRequest.class));
        assertEquals("VAL2", Properties.get("KEY1"));
      }

      /**
       * Properties#load が CACHE_TTL_MS に数値でない値があるとき既定の1日をTTLとして使うことを確かめる。 不正な設定値で再読み込みが毎回起きると、S3
       * の呼び出しが増える。
       *
       * @given バケット名が "test-bucket"、CACHE_TTL_MS が "abc"、前回の読み込み時刻が
       *     "2026-01-15T03:04:05Z"、現在時刻が前回から 86399999 ミリ秒後
       * @when load を S3Client で 2 回呼び出す
       * @then 2 回目の load で S3 の取得を呼ばない
       */
      @Test
      void CACHE_TTL_MSが数値でない場合_1日未満の経過ではS3を呼ばない() {
        setConfigBucket("test-bucket");
        Properties.putForTest("CACHE_TTL_MS", "abc");
        S3Client s3 = mock(S3Client.class);
        when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(configAnswer("KEY1=VAL1"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        Properties.load(s3);
        ApplicationClock.INSTANCE.setClock(
            fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS - 1)));
        Properties.load(s3);

        verify(s3, times(1)).getObject(any(GetObjectRequest.class));
      }

      /**
       * Properties#load が CACHE_TTL_MS が空の場合に既定の1日をTTLとして使うことを確かめる。
       * 空の設定値を数値として解析すると失敗するため、既定値へ戻す分岐が必要である。
       *
       * @given バケット名が "test-bucket"、CACHE_TTL_MS が空文字、前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から
       *     86399999 ミリ秒後
       * @when load を S3Client で 2 回呼び出す
       * @then 2 回目の load で S3 の取得を呼ばない
       */
      @Test
      void CACHE_TTL_MSが空の場合_1日未満の経過ではS3を呼ばない() {
        setConfigBucket("test-bucket");
        Properties.putForTest("CACHE_TTL_MS", "");
        S3Client s3 = mock(S3Client.class);
        when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(configAnswer("KEY1=VAL1"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        Properties.load(s3);
        ApplicationClock.INSTANCE.setClock(
            fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS - 1)));
        Properties.load(s3);

        verify(s3, times(1)).getObject(any(GetObjectRequest.class));
      }

      /**
       * Properties#load が CACHE_TTL_MS で指定した値をTTLとして使うことを確かめる。 TTL
       * を設定値で変えられることで、再読み込みの間隔を運用に合わせられる。
       *
       * @given バケット名が "test-bucket"、CACHE_TTL_MS が "1000"、前回の読み込み時刻が
       *     "2026-01-15T03:04:05Z"、現在時刻が前回から 1000 ミリ秒後、1 回目の S3 が "KEY1=VAL1"、2 回目が "KEY1=VAL2" を返す
       * @when load を S3Client で 2 回呼び出す
       * @then 2 回目の load で S3 から読み込み、設定値 KEY1 を "VAL2" に更新する
       */
      @Test
      void CACHE_TTL_MSが1000の場合_1000ミリ秒経過で設定値を更新する() {
        setConfigBucket("test-bucket");
        Properties.putForTest("CACHE_TTL_MS", "1000");
        S3Client s3 = mock(S3Client.class);
        when(s3.getObject(any(GetObjectRequest.class)))
            .thenReturn(configStream("KEY1=VAL1"), configStream("KEY1=VAL2"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        Properties.load(s3);
        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT.plusMillis(1000)));
        Properties.load(s3);

        assertEquals("VAL2", Properties.get("KEY1"));
      }

      /**
       * Properties#load が空行と区切りのない行を無視し、有効な行の値だけを登録することを確かめる。 不正な行で設定値が壊れると、後続の取得値が食い違う。
       *
       * @given バケット名が test-bucket、設定値が空、S3 が空行と区切りのない行とキーのない行と値が空の行を含む内容を返す
       * @when load を S3Client で呼び出す
       * @then valid の値として "key" を登録し、invalid-line の値として空文字を返す
       */
      @Test
      @SuppressWarnings("unchecked")
      void 空行と区切りのない行を含む場合_有効な行の値を登録する() throws Exception {
        Field configBucketField = Properties.class.getDeclaredField("CONFIG_BUCKET");
        configBucketField.setAccessible(true);
        configBucketField.set(null, "test-bucket");

        Field field = Properties.class.getDeclaredField("properties");
        field.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) field.get(null);
        propertiesMap.clear();

        S3Client mockS3 = mock(S3Client.class);
        String content = "valid=key\n \ninvalid-line\n=no-key\nkey=\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        ResponseInputStream<GetObjectResponse> s3Stream =
            new ResponseInputStream<>(GetObjectResponse.builder().build(), is);
        when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

        Properties.load(mockS3);
        assertEquals("key", Properties.get("valid"));
        // Properties.get returns "" for missing keys
        assertEquals("", Properties.get("invalid-line"));
        assertEquals("key", Properties.get("valid"));
      }
    }
  }

  /** {@link Properties#loadFromParameterStore(SsmClient)} のうち、TTL の分岐のテスト。 */
  @Nested
  class loadFromParameterStore {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#loadFromParameterStore が SSM のパラメータを読み、パス接頭辞を除いたキーで登録することを確かめる。 キーの形式を守る仕様である。
       *
       * @given 前回の読み込み時刻がなく、設定値に infrastructure/s3/bucket/name が S3_OLD_VALUE で登録されており、SSM が
       *     /nectar/dev/ 以下のパラメータ 2 件を返す
       * @when loadFromParameterStore を SsmClient で呼び出す
       * @then infrastructure/s3/bucket/name の値として "test-bucket" を、infrastructure/rds/endpoint の値として
       *     "test-endpoint" を登録する
       */
      @Test
      @SuppressWarnings("unchecked")
      void SSMのパラメータがある場合_パラメータの値を登録する() throws Exception {
        Field field = Properties.class.getDeclaredField("properties");
        field.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) field.get(null);
        propertiesMap.clear();
        propertiesMap.put("infrastructure/s3/bucket/name", "S3_OLD_VALUE");

        SsmClient mockSsm = mock(SsmClient.class);
        List<Parameter> params = new ArrayList<>();
        params.add(
            Parameter.builder()
                .name("/nectar/dev/infrastructure/s3/bucket/name")
                .value("test-bucket")
                .build());
        params.add(
            Parameter.builder()
                .name("/nectar/dev/infrastructure/rds/endpoint")
                .value("test-endpoint")
                .build());

        GetParametersByPathResponse response =
            GetParametersByPathResponse.builder().parameters(params).nextToken(null).build();
        when(mockSsm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenReturn(response);

        Properties.loadFromParameterStore(mockSsm);

        assertEquals("test-bucket", Properties.get("infrastructure/s3/bucket/name"));
        assertEquals("test-endpoint", Properties.get("infrastructure/rds/endpoint"));
      }

      /**
       * Properties#loadFromParameterStore が nextToken をたどり、全ページのパラメータを登録することを確かめる。 2
       * ページ目以降を読まないと設定値が欠ける。
       *
       * @given 1 ページ目が param1 の値 value1 と nextToken を持ち、2 ページ目が param2 の値 value2 を持つ
       * @when loadFromParameterStore を SsmClient で呼び出す
       * @then param1 の値として "value1"、param2 の値として "value2" を登録する
       */
      @Test
      @SuppressWarnings("unchecked")
      void SSMの応答が次ページを持つ場合_全ページのパラメータを登録する() throws Exception {
        Field field = Properties.class.getDeclaredField("properties");
        field.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) field.get(null);
        propertiesMap.clear();

        SsmClient mockSsm = mock(SsmClient.class);

        List<Parameter> paramsPage1 = new ArrayList<>();
        paramsPage1.add(Parameter.builder().name("/nectar/dev/param1").value("value1").build());

        List<Parameter> paramsPage2 = new ArrayList<>();
        paramsPage2.add(Parameter.builder().name("/nectar/dev/param2").value("value2").build());

        GetParametersByPathResponse responsePage1 =
            GetParametersByPathResponse.builder().parameters(paramsPage1).nextToken("next").build();
        GetParametersByPathResponse responsePage2 =
            GetParametersByPathResponse.builder().parameters(paramsPage2).nextToken(null).build();

        when(mockSsm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenReturn(responsePage1)
            .thenReturn(responsePage2);

        Properties.loadFromParameterStore(mockSsm);

        assertEquals("value1", Properties.get("param1"));
        assertEquals("value2", Properties.get("param2"));
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * Properties#loadFromParameterStore が SSM の例外を投げずに終えることを確かめる。 SSM の一時的な失敗で Lambda
       * の初期化を止めない仕様である。
       *
       * @given SSM の getParametersByPath が RuntimeException を投げる
       * @when loadFromParameterStore を SsmClient で呼び出す
       * @then 例外を投げない
       */
      @Test
      void SSMの取得が例外を投げた場合_何もしない() throws Exception {
        SsmClient mockSsm = mock(SsmClient.class);
        when(mockSsm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenThrow(new RuntimeException("SSM access failed"));

        assertDoesNotThrow(() -> Properties.loadFromParameterStore(mockSsm));
      }
    }

    /** 境界値のテスト。 */
    @Nested
    class 境界値 {

      /**
       * Properties#loadFromParameterStore が前回の読み込みからTTL未満のとき SSM を呼ばないことを確かめる。
       * 1日以内の再読み込みを防ぐことで、本番の読み込み回数を変えないための仕様である。
       *
       * @given 前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 86399999 ミリ秒後、SSM が KEY1 "VAL1" を返す
       * @when loadFromParameterStore を SsmClient で 2 回呼び出す
       * @then 2 回目の呼び出しで SSM の取得を呼ばず、設定値 KEY1 が "VAL1" のままである
       */
      @Test
      void 前回読み込みからTTL未満の場合_SSMを呼ばない() {
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenAnswer(ssmParameterAnswer("KEY1", "VAL1"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        Properties.loadFromParameterStore(ssm);
        ApplicationClock.INSTANCE.setClock(
            fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS - 1)));
        Properties.loadFromParameterStore(ssm);

        verify(ssm, times(1)).getParametersByPath(any(GetParametersByPathRequest.class));
        assertEquals("VAL1", Properties.get("KEY1"));
      }

      /**
       * Properties#loadFromParameterStore が前回の読み込みからTTLちょうどのとき SSM から再読み込みすることを確かめる。 TTL
       * の境界で再読み込みが起きないと、設定値の更新が1日遅れる。
       *
       * @given 前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 86400000 ミリ秒後、1 回目の SSM が KEY1 "VAL1"、2
       *     回目が KEY1 "VAL2" を返す
       * @when loadFromParameterStore を SsmClient で 2 回呼び出す
       * @then 2 回目の呼び出しで SSM から読み込み、設定値 KEY1 を "VAL2" に更新する
       */
      @Test
      void 前回読み込みからTTLちょうどの場合_設定値を更新する() {
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.getParametersByPath(any(GetParametersByPathRequest.class)))
            .thenAnswer(ssmParameterAnswer("KEY1", "VAL1"))
            .thenAnswer(ssmParameterAnswer("KEY1", "VAL2"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        Properties.loadFromParameterStore(ssm);
        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS)));
        Properties.loadFromParameterStore(ssm);

        verify(ssm, times(2)).getParametersByPath(any(GetParametersByPathRequest.class));
        assertEquals("VAL2", Properties.get("KEY1"));
      }
    }
  }

  /** {@link Properties#putForTest(String, String)} のテスト。 */
  @Nested
  class putForTest {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#putForTest が登録した設定値を get で取得できることを確かめる。 テストが設定値を入れる口として使えないと、設定値に依存する処理を検証できない。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、設定値が空である
       * @when putForTest を キー "KEY1" と値 "VAL1" で呼び出す
       * @then get で "KEY1" を取得すると "VAL1" を返す
       */
      @Test
      void キーと値を登録した場合_設定値を返す() {
        Properties.putForTest("KEY1", "VAL1");

        assertEquals("VAL1", Properties.get("KEY1"));
      }

      /**
       * Properties#putForTest が同じキーの値を置き換えることを確かめる。 同じキーで二重に登録すると古い値が残り、テストの期待値とずれる。
       *
       * @given キー "KEY1" に値 "VAL1" が登録されている
       * @when putForTest を キー "KEY1" と値 "VAL2" で呼び出す
       * @then get で "KEY1" を取得すると "VAL2" を返す
       */
      @Test
      void 同じキーに登録した場合_値を更新する() {
        Properties.putForTest("KEY1", "VAL1");

        Properties.putForTest("KEY1", "VAL2");

        assertEquals("VAL2", Properties.get("KEY1"));
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * Properties#putForTest がキーに null を受け付けず例外を投げることを確かめる。 null のキーを登録すると、設定値の参照結果が不定になる。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" である
       * @when putForTest を キーに null と値 "VAL1" で呼び出す
       * @then NullPointerException を投げる
       */
      @Test
      void キーがnullの場合_NullPointerExceptionを投げる() {
        assertThrows(NullPointerException.class, () -> Properties.putForTest(null, "VAL1"));
      }

      /**
       * Properties#putForTest が値に null を受け付けず例外を投げることを確かめる。 null の値を登録すると、get の既定値の仕様と食い違う。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" である
       * @when putForTest を キー "KEY1" と値に null で呼び出す
       * @then NullPointerException を投げる
       */
      @Test
      void 値がnullの場合_NullPointerExceptionを投げる() {
        assertThrows(NullPointerException.class, () -> Properties.putForTest("KEY1", null));
      }
    }

    /** 認可のテスト。 */
    @Nested
    class 認可 {

      /**
       * Properties#putForTest が読み込み元が未設定のとき設定値を登録させないことを確かめる。 本番で設定値を書き換えると、AWS から読んだ値と食い違う。
       *
       * @given システムプロパティ "nectar.config.source" が未設定である
       * @when putForTest を キー "KEY1" と値 "VAL1" で呼び出す
       * @then IllegalStateException を投げ、get で "KEY1" を取得すると空文字を返す
       */
      @Test
      void システムプロパティが未設定の場合_IllegalStateExceptionを投げる() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        assertThrows(IllegalStateException.class, () -> Properties.putForTest("KEY1", "VAL1"));
        assertEquals("", Properties.get("KEY1"));
      }

      /**
       * Properties#putForTest が読み込み元が file のとき設定値を登録させないことを確かめる。 memory 以外の値では、テスト用の口を使わない仕様である。
       *
       * @given システムプロパティ "nectar.config.source" が "file" である
       * @when putForTest を キー "KEY1" と値 "VAL1" で呼び出す
       * @then IllegalStateException を投げ、get で "KEY1" を取得すると空文字を返す
       */
      @Test
      void システムプロパティがfileの場合_IllegalStateExceptionを投げる() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, "file");

        assertThrows(IllegalStateException.class, () -> Properties.putForTest("KEY1", "VAL1"));
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
        assertEquals("", Properties.get("KEY1"));
      }
    }
  }

  /** {@link Properties#removeForTest(String)} のテスト。 */
  @Nested
  class removeForTest {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#removeForTest が登録済みのキーを削除することを確かめる。 削除できないと、キーが未設定の状態をテストで再現できない。
       *
       * @given キー "KEY1" に値 "VAL1" が登録されている
       * @when removeForTest を キー "KEY1" で呼び出す
       * @then get で "KEY1" を取得すると空文字を返す
       */
      @Test
      void 登録済みのキーの場合_設定値を削除する() {
        Properties.putForTest("KEY1", "VAL1");

        Properties.removeForTest("KEY1");

        assertEquals("", Properties.get("KEY1"));
      }

      /**
       * Properties#removeForTest が未登録のキーを指定しても例外を投げないことを確かめる。 テストの後始末で、登録の有無を確認せずに削除できるようにする。
       *
       * @given キー "KEY1" が未登録である
       * @when removeForTest を キー "KEY1" で呼び出す
       * @then 例外を投げず、get で "KEY1" を取得すると空文字を返す
       */
      @Test
      void 未登録のキーの場合_何もしない() {
        Properties.removeForTest("KEY1");

        assertEquals("", Properties.get("KEY1"));
      }
    }

    /** 認可のテスト。 */
    @Nested
    class 認可 {

      /**
       * Properties#removeForTest が読み込み元が未設定のとき設定値を削除させないことを確かめる。 本番で設定値を消すと、業務処理が設定値を読めなくなる。
       *
       * @given キー "KEY1" に値 "VAL1" が登録されており、システムプロパティ "nectar.config.source" が未設定である
       * @when removeForTest を キー "KEY1" で呼び出す
       * @then IllegalStateException を投げ、get で "KEY1" を取得すると "VAL1" を返す
       */
      @Test
      void システムプロパティが未設定の場合_IllegalStateExceptionを投げる() {
        Properties.putForTest("KEY1", "VAL1");
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        assertThrows(IllegalStateException.class, () -> Properties.removeForTest("KEY1"));
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
        assertEquals("VAL1", Properties.get("KEY1"));
      }
    }
  }

  /** {@link Properties#resetForTest()} のテスト。 */
  @Nested
  class resetForTest {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#resetForTest が登録済みの設定値をすべて消すことを確かめる。 テストごとに設定値が残ると、テストの実行順で結果が変わる。
       *
       * @given キー "KEY1" に値 "VAL1" が登録されている
       * @when resetForTest を引数なしで呼び出す
       * @then get で "KEY1" を取得すると空文字を返す
       */
      @Test
      void 設定値が登録されている場合_設定値を削除する() {
        Properties.putForTest("KEY1", "VAL1");

        Properties.resetForTest();

        assertEquals("", Properties.get("KEY1"));
      }

      /**
       * Properties#resetForTest が最終読み込み時刻も初期化することを確かめる。 時刻が残ると、テストの中で TTL 内の読み込みが S3 を呼ばずに終わる。
       *
       * @given バケット名が "test-bucket"、前回の読み込み時刻が "2026-01-15T03:04:05Z" で、resetForTest
       *     の後にバケット名を再設定している
       * @when load を S3Client で呼び出す
       * @then S3 の取得を呼び、設定値 KEY1 を "VAL1" として登録する
       */
      @Test
      void 読み込み後にresetForTestを呼んだ場合_TTL内でも設定値を登録する() {
        setConfigBucket("test-bucket");
        S3Client s3 = mock(S3Client.class);
        when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(configAnswer("KEY1=VAL1"));
        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        Properties.load(s3);

        Properties.resetForTest();
        setConfigBucket("test-bucket");
        Properties.load(s3);

        verify(s3, times(2)).getObject(any(GetObjectRequest.class));
        assertEquals("VAL1", Properties.get("KEY1"));
      }
    }

    /** 認可のテスト。 */
    @Nested
    class 認可 {

      /**
       * Properties#resetForTest が読み込み元が未設定のとき設定値を初期化させないことを確かめる。 本番で設定値を初期化すると、起動時に読み込んだ設定値が消える。
       *
       * @given システムプロパティ "nectar.config.source" が未設定である
       * @when resetForTest を引数なしで呼び出す
       * @then IllegalStateException を投げる
       */
      @Test
      void システムプロパティが未設定の場合_IllegalStateExceptionを投げる() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        assertThrows(IllegalStateException.class, Properties::resetForTest);
      }
    }
  }

  /** {@link Properties#resolveS3Region(UnaryOperator)} のテスト。 */
  @Nested
  class resolveS3Region {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#resolveS3Region が AWS_REGION の値をリージョンとして返すことを確かめる。 本番の環境変数どおりのリージョンで接続するため。
       *
       * @given 環境変数 AWS_REGION が "us-east-1" である
       * @when resolveS3Region を環境変数の関数で呼び出す
       * @then Region.US_EAST_1 を返す
       */
      @Test
      void AWS_REGIONがus_east_1の場合_us_east_1を返す() {
        Region region = Properties.resolveS3Region(envOf(Map.of("AWS_REGION", "us-east-1")));

        assertEquals(Region.US_EAST_1, region);
      }
    }

    /** 境界値のテスト。 */
    @Nested
    class 境界値 {

      /**
       * Properties#resolveS3Region が AWS_REGION が空白のとき既定のリージョンを返すことを確かめる。
       * 空白の値で接続先を決めると、リージョンが解決できないため。
       *
       * @given 環境変数 AWS_REGION が空白文字 " " である
       * @when resolveS3Region を環境変数の関数で呼び出す
       * @then Region.AP_NORTHEAST_1 を返す
       */
      @Test
      void AWS_REGIONが空白の場合_既定のリージョンを返す() {
        Region region = Properties.resolveS3Region(envOf(Map.of("AWS_REGION", "   ")));

        assertEquals(Region.AP_NORTHEAST_1, region);
      }

      /**
       * Properties#resolveS3Region が AWS_REGION が未設定のとき既定のリージョンを返すことを確かめる。
       * 環境変数がない実行環境でも、既定のリージョンで接続するため。
       *
       * @given 環境変数 AWS_REGION が未設定である
       * @when resolveS3Region を環境変数の関数で呼び出す
       * @then Region.AP_NORTHEAST_1 を返す
       */
      @Test
      void AWS_REGIONが未設定の場合_既定のリージョンを返す() {
        Region region = Properties.resolveS3Region(envOf(Map.of()));

        assertEquals(Region.AP_NORTHEAST_1, region);
      }
    }
  }

  /** {@link Properties#buildS3ClientForConfigLoad(UnaryOperator)} のテスト。 */
  @Nested
  class buildS3ClientForConfigLoad {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#buildS3ClientForConfigLoad がエンドポイントもリージョンもないとき既定の設定で生成することを確かめる。 本番では AWS
       * の既定のエンドポイントと資格情報を使うため。
       *
       * @given 環境変数が空で、システムプロパティ aws.endpointUrl が未設定、S3Client の builder が client を返す
       * @when buildS3ClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、リージョンに Region.AP_NORTHEAST_1 を指定し、endpointOverride を呼ばず、既定の資格情報を設定する
       */
      @Test
      void エンドポイントもリージョンも未設定の場合_既定のリージョンのS3クライアントを返す() {
        S3Client client = mock(S3Client.class);
        S3ClientBuilder builder = s3BuilderReturning(client);

        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          s3.when(S3Client::builder).thenReturn(builder);

          S3Client result = Properties.buildS3ClientForConfigLoad(envOf(Map.of()));

          assertSame(client, result);
          verify(builder).region(Region.AP_NORTHEAST_1);
          verify(builder, never()).endpointOverride(any(URI.class));
          assertInstanceOf(DefaultCredentialsProvider.class, capturedCredentialsOf(builder));
        }
      }

      /**
       * Properties#buildS3ClientForConfigLoad がエンドポイントと資格情報の環境変数があるとき、それを使うことを確かめる。 ローカル検証の
       * LocalStack に向けて、指定の資格情報で接続するため。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、AWS_ACCESS_KEY_ID が
       *     "AK"、AWS_SECRET_ACCESS_KEY が "SK"、S3Client の builder が client を返す
       * @when buildS3ClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、endpointOverride に "http://localhost:4566" を指定し、forcePathStyle に true
       *     を指定し、資格情報の access key が "AK" になる
       */
      @Test
      void エンドポイントと資格情報の環境変数がある場合_カスタムエンドポイントのS3クライアントを返す() {
        S3Client client = mock(S3Client.class);
        S3ClientBuilder builder = s3BuilderReturning(client);
        UnaryOperator<String> environment =
            envOf(
                Map.of(
                    "AWS_ENDPOINT_URL", "http://localhost:4566",
                    "AWS_ACCESS_KEY_ID", "AK",
                    "AWS_SECRET_ACCESS_KEY", "SK"));

        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          s3.when(S3Client::builder).thenReturn(builder);

          S3Client result = Properties.buildS3ClientForConfigLoad(environment);

          assertSame(client, result);
          verify(builder).endpointOverride(URI.create("http://localhost:4566"));
          verify(builder).forcePathStyle(true);
          assertEquals("AK", accessKeyIdOf(capturedCredentialsOf(builder)));
        }
      }

      /**
       * Properties#buildS3ClientForConfigLoad がエンドポイントだけあり資格情報がないとき固定の資格情報を使うことを確かめる。 ローカル検証の
       * LocalStack は資格情報を検証しないため、固定値で接続できるようにするため。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566" で、資格情報の環境変数が未設定、S3Client の builder が
       *     client を返す
       * @when buildS3ClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、資格情報の access key が "test" になる
       */
      @Test
      void エンドポイントだけがあり資格情報がない場合_固定の資格情報のS3クライアントを返す() {
        S3Client client = mock(S3Client.class);
        S3ClientBuilder builder = s3BuilderReturning(client);

        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          s3.when(S3Client::builder).thenReturn(builder);

          S3Client result =
              Properties.buildS3ClientForConfigLoad(
                  envOf(Map.of("AWS_ENDPOINT_URL", "http://localhost:4566")));

          assertSame(client, result);
          assertEquals("test", accessKeyIdOf(capturedCredentialsOf(builder)));
        }
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * Properties#buildS3ClientForConfigLoad がエンドポイントと片方の資格情報だけあるとき固定の資格情報を使うことを確かめる。
       * 片方だけの資格情報では署名できないため、固定値で接続する仕様である。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、AWS_ACCESS_KEY_ID が "AK" で
       *     AWS_SECRET_ACCESS_KEY が未設定、S3Client の builder が client を返す
       * @when buildS3ClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、資格情報の access key が "test" になる
       */
      @Test
      void エンドポイントと片方の資格情報だけがある場合_固定の資格情報のS3クライアントを返す() {
        S3Client client = mock(S3Client.class);
        S3ClientBuilder builder = s3BuilderReturning(client);

        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          s3.when(S3Client::builder).thenReturn(builder);

          S3Client result =
              Properties.buildS3ClientForConfigLoad(
                  envOf(
                      Map.of(
                          "AWS_ENDPOINT_URL", "http://localhost:4566",
                          "AWS_ACCESS_KEY_ID", "AK")));

          assertSame(client, result);
          assertEquals("test", accessKeyIdOf(capturedCredentialsOf(builder)));
        }
      }

      /**
       * Properties#buildS3ClientForConfigLoad がエンドポイント URL を解釈できないとき endpointOverride を呼ばないことを確かめる。
       * 不正な URL で接続先を決めると、既定のエンドポイントを使う意図と食い違うため。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が空白を含む "http://bad uri" で、S3Client の builder が client を返す
       * @when buildS3ClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、endpointOverride を呼ばず、既定の資格情報を設定する
       */
      @Test
      void エンドポイントURLが不正な場合_endpointOverrideを呼ばない() {
        S3Client client = mock(S3Client.class);
        S3ClientBuilder builder = s3BuilderReturning(client);

        try (MockedStatic<S3Client> s3 = mockStatic(S3Client.class)) {
          s3.when(S3Client::builder).thenReturn(builder);

          S3Client result =
              Properties.buildS3ClientForConfigLoad(
                  envOf(Map.of("AWS_ENDPOINT_URL", "http://bad uri")));

          assertSame(client, result);
          verify(builder, never()).endpointOverride(any(URI.class));
          assertInstanceOf(DefaultCredentialsProvider.class, capturedCredentialsOf(builder));
        }
      }
    }
  }

  /** {@link Properties#buildSsmClientForConfigLoad(UnaryOperator)} のテスト。 */
  @Nested
  class buildSsmClientForConfigLoad {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * Properties#buildSsmClientForConfigLoad がエンドポイントもリージョンもないとき既定の設定で生成することを確かめる。 本番では AWS
       * の既定のエンドポイントと資格情報を使うため。
       *
       * @given 環境変数が空で、システムプロパティ aws.endpointUrl が未設定、SsmClient の builder が client を返す
       * @when buildSsmClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、リージョンに Region.AP_NORTHEAST_1 を指定し、endpointOverride を呼ばず、既定の資格情報を設定する
       */
      @Test
      void エンドポイントもリージョンも未設定の場合_既定のリージョンのSSMクライアントを返す() {
        SsmClient client = mock(SsmClient.class);
        SsmClientBuilder builder = ssmBuilderReturning(client);

        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class)) {
          ssm.when(SsmClient::builder).thenReturn(builder);

          SsmClient result = Properties.buildSsmClientForConfigLoad(envOf(Map.of()));

          assertSame(client, result);
          verify(builder).region(Region.AP_NORTHEAST_1);
          verify(builder, never()).endpointOverride(any(URI.class));
          assertInstanceOf(DefaultCredentialsProvider.class, capturedCredentialsOf(builder));
        }
      }

      /**
       * Properties#buildSsmClientForConfigLoad がエンドポイントと資格情報の環境変数があるとき、それを使うことを確かめる。 ローカル検証の
       * LocalStack に向けて、指定の資格情報で接続するため。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、AWS_ACCESS_KEY_ID が
       *     "AK"、AWS_SECRET_ACCESS_KEY が "SK"、SsmClient の builder が client を返す
       * @when buildSsmClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、endpointOverride に "http://localhost:4566" を指定し、資格情報の access key が "AK"
       *     になる
       */
      @Test
      void エンドポイントと資格情報の環境変数がある場合_カスタムエンドポイントのSSMクライアントを返す() {
        SsmClient client = mock(SsmClient.class);
        SsmClientBuilder builder = ssmBuilderReturning(client);
        UnaryOperator<String> environment =
            envOf(
                Map.of(
                    "AWS_ENDPOINT_URL", "http://localhost:4566",
                    "AWS_ACCESS_KEY_ID", "AK",
                    "AWS_SECRET_ACCESS_KEY", "SK"));

        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class)) {
          ssm.when(SsmClient::builder).thenReturn(builder);

          SsmClient result = Properties.buildSsmClientForConfigLoad(environment);

          assertSame(client, result);
          verify(builder).endpointOverride(URI.create("http://localhost:4566"));
          assertEquals("AK", accessKeyIdOf(capturedCredentialsOf(builder)));
        }
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * Properties#buildSsmClientForConfigLoad がエンドポイントだけあり資格情報がないとき固定の資格情報を使うことを確かめる。 ローカル検証の
       * LocalStack は資格情報を検証しないため、固定値で接続できるようにするため。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566" で、資格情報の環境変数が未設定、SsmClient の builder
       *     が client を返す
       * @when buildSsmClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、資格情報の access key が "test" になる
       */
      @Test
      void エンドポイントだけがあり資格情報がない場合_固定の資格情報のSSMクライアントを返す() {
        SsmClient client = mock(SsmClient.class);
        SsmClientBuilder builder = ssmBuilderReturning(client);

        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class)) {
          ssm.when(SsmClient::builder).thenReturn(builder);

          SsmClient result =
              Properties.buildSsmClientForConfigLoad(
                  envOf(Map.of("AWS_ENDPOINT_URL", "http://localhost:4566")));

          assertSame(client, result);
          assertEquals("test", accessKeyIdOf(capturedCredentialsOf(builder)));
        }
      }

      /**
       * Properties#buildSsmClientForConfigLoad がエンドポイントと片方の資格情報だけあるとき固定の資格情報を使うことを確かめる。
       * 片方だけの資格情報では署名できないため、固定値で接続する仕様である。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が "http://localhost:4566"、AWS_ACCESS_KEY_ID が "AK" で
       *     AWS_SECRET_ACCESS_KEY が未設定、SsmClient の builder が client を返す
       * @when buildSsmClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、資格情報の access key が "test" になる
       */
      @Test
      void エンドポイントと片方の資格情報だけがある場合_固定の資格情報のSSMクライアントを返す() {
        SsmClient client = mock(SsmClient.class);
        SsmClientBuilder builder = ssmBuilderReturning(client);

        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class)) {
          ssm.when(SsmClient::builder).thenReturn(builder);

          SsmClient result =
              Properties.buildSsmClientForConfigLoad(
                  envOf(
                      Map.of(
                          "AWS_ENDPOINT_URL", "http://localhost:4566",
                          "AWS_ACCESS_KEY_ID", "AK")));

          assertSame(client, result);
          assertEquals("test", accessKeyIdOf(capturedCredentialsOf(builder)));
        }
      }

      /**
       * Properties#buildSsmClientForConfigLoad がエンドポイント URL を解釈できないとき endpointOverride
       * を呼ばないことを確かめる。 不正な URL で接続先を決めると、既定のエンドポイントを使う意図と食い違うため。
       *
       * @given 環境変数 AWS_ENDPOINT_URL が空白を含む "http://bad uri" で、SsmClient の builder が client を返す
       * @when buildSsmClientForConfigLoad を環境変数の関数で呼び出す
       * @then client を返し、endpointOverride を呼ばず、既定の資格情報を設定する
       */
      @Test
      void エンドポイントURLが不正な場合_endpointOverrideを呼ばない() {
        SsmClient client = mock(SsmClient.class);
        SsmClientBuilder builder = ssmBuilderReturning(client);

        try (MockedStatic<SsmClient> ssm = mockStatic(SsmClient.class)) {
          ssm.when(SsmClient::builder).thenReturn(builder);

          SsmClient result =
              Properties.buildSsmClientForConfigLoad(
                  envOf(Map.of("AWS_ENDPOINT_URL", "http://bad uri")));

          assertSame(client, result);
          verify(builder, never()).endpointOverride(any(URI.class));
          assertInstanceOf(DefaultCredentialsProvider.class, capturedCredentialsOf(builder));
        }
      }
    }
  }

  /**
   * テスト用に環境変数の読み取りを表す関数を作ります。
   *
   * @param values キーと値の組。含まれないキーは null を返す
   * @return 環境変数を読む関数
   */
  private static UnaryOperator<String> envOf(final Map<String, String> values) {
    return key -> values.get(key);
  }

  /**
   * S3 の builder のモックを作ります。
   *
   * <p>builder の各メソッドは自身を返し、build で指定のクライアントを返す。SDK の fluent な呼び出しを 1 つのモックで表すため。
   *
   * @param client build が返すクライアント
   * @return build が指定のクライアントを返す builder のモック
   */
  private static S3ClientBuilder s3BuilderReturning(final S3Client client) {
    S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(client);
    return builder;
  }

  /**
   * SSM の builder のモックを作ります。
   *
   * <p>builder の各メソッドは自身を返し、build で指定のクライアントを返す。SDK の fluent な呼び出しを 1 つのモックで表すため。
   *
   * @param client build が返すクライアント
   * @return build が指定のクライアントを返す builder のモック
   */
  private static SsmClientBuilder ssmBuilderReturning(final SsmClient client) {
    SsmClientBuilder builder = mock(SsmClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(client);
    return builder;
  }

  /**
   * builder に設定された資格情報のプロバイダーを取り出します。
   *
   * <p>credentialsProvider の呼び出し引数を捕捉して返す。builder の種類によらず使えるように型引数で受ける。
   *
   * @param builder 資格情報を設定された builder のモック
   * @param <B> builder の型
   * @return 設定された資格情報のプロバイダー
   */
  private static <B extends AwsClientBuilder<B, ?>> AwsCredentialsProvider capturedCredentialsOf(
      final B builder) {
    ArgumentCaptor<AwsCredentialsProvider> captor =
        ArgumentCaptor.forClass(AwsCredentialsProvider.class);
    verify(builder).credentialsProvider(captor.capture());
    return captor.getValue();
  }

  /**
   * 固定の資格情報プロバイダーから access key を取り出します。
   *
   * @param provider 固定の資格情報プロバイダー
   * @return access key
   */
  private static String accessKeyIdOf(final AwsCredentialsProvider provider) {
    return assertInstanceOf(StaticCredentialsProvider.class, provider)
        .resolveCredentials()
        .accessKeyId();
  }

  /**
   * テスト用に CONFIG_BUCKET を設定します。
   *
   * <p>CONFIG_BUCKET は本番では SSM から解決する値であり、テストから設定する口がない。既存のテストと同じくリフレクションで設定する。
   *
   * @param bucket 設定するバケット名
   * @throws IllegalStateException リフレクションに失敗した場合
   */
  private static void setConfigBucket(final String bucket) {
    try {
      Field field = Properties.class.getDeclaredField("CONFIG_BUCKET");
      field.setAccessible(true);
      field.set(null, bucket);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * 指定した時刻で止まる時計を作ります。
   *
   * @param instant 固定する時刻
   * @return Asia/Tokyo のゾーンを持つ固定時計
   */
  private static Clock fixedClockAt(final Instant instant) {
    return Clock.fixed(instant, ZONE);
  }

  /**
   * 呼び出すたびに新しい設定ファイルの内容を返す S3 の応答を作ります。
   *
   * @param content 設定ファイルの内容
   * @return 読み込みごとに新しい入力ストリームを返す応答
   */
  private static Answer<ResponseInputStream<GetObjectResponse>> configAnswer(final String content) {
    return invocation -> configStream(content);
  }

  /**
   * 設定ファイルの内容を入力ストリームとして作ります。
   *
   * @param content 設定ファイルの内容
   * @return 設定ファイルの入力ストリーム
   */
  private static ResponseInputStream<GetObjectResponse> configStream(final String content) {
    return new ResponseInputStream<>(
        GetObjectResponse.builder().build(),
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * 指定したキーと値を1件だけ返す SSM の応答を作ります。
   *
   * <p>パラメータ名はリクエストのパスを前置して作る。環境変数 ENVIRONMENT の値によってパスが変わっても、テストが通るようにする。
   *
   * @param key 設定のキー
   * @param value 設定の値
   * @return リクエストのパスに合わせたパラメータ名を持つ応答
   */
  private static Answer<GetParametersByPathResponse> ssmParameterAnswer(
      final String key, final String value) {
    return invocation -> {
      GetParametersByPathRequest request = invocation.getArgument(0);
      return GetParametersByPathResponse.builder()
          .parameters(List.of(Parameter.builder().name(request.path() + key).value(value).build()))
          .build();
    };
  }
}
