package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.core.util.ConfigSource;
import copel.sesproductpackage.core.util.Properties;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * {@link SecretManager} の単体テスト。
 *
 * <p>グローバルキャッシュは ARN ごとに static で保持するため、テストごとに ARN を変えて他のテストの影響を受けないようにする。Secrets Manager
 * への接続はモックで置き換える。
 *
 * @author Copel Co., Ltd.
 */
class SecretManagerTest {

  /** テストで使う固定時刻の基準点。UTC の "2026-01-15T03:04:05Z" を表す。 */
  private static final Instant BASE_INSTANT = Instant.parse("2026-01-15T03:04:05Z");

  /** テストで使う時計のゾーン。本番の時計と同じ Asia/Tokyo を使う。 */
  private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

  /** 既定の TTL のミリ秒。1 日分の値で、SecretManager の既定値と同じ。 */
  private static final long DEFAULT_TTL_MS = 86400000L;

  /** テストの前に設定読み込み元のプロパティの値を退避する。 */
  private String originalConfigSource;

  /** テストの前に設定読み込み元を memory にし、時計と設定値を初期化する。 */
  @BeforeEach
  void setUp() {
    originalConfigSource = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    ApplicationClock.INSTANCE.resetToDefault();
    Properties.resetForTest();
  }

  /** テストの後に設定値と時計を初期化し、設定読み込み元のプロパティを復元する。 */
  @AfterEach
  void tearDown() {
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    Properties.resetForTest();
    ApplicationClock.INSTANCE.resetToDefault();
    if (originalConfigSource == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, originalConfigSource);
    }
  }

  /** {@link SecretManager#load()} のうち、TTL によるキャッシュの分岐のテスト。 */
  @Nested
  class load {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {
      /**
       * SecretManager#load が JSON の各項目を取得可能な値として登録することを確かめる。 各項目を読めないと接続情報を使えない。
       *
       * @given Secrets Manager が host localhost、port 3306、username testuser、password
       *     testpass、dbname testdb の JSON を返す
       * @when load を呼び出す
       * @then isLoaded が true を返し、各項目の get が localhost、3306、testuser、testpass、testdb を返す
       */
      @Test
      void シークレットに項目がある場合_各項目の値を登録する() throws Exception {
        String secretJson =
            "{\"host\":\"localhost\",\"port\":\"3306\",\"username\":\"testuser\",\"password\":\"testpass\",\"dbname\":\"testdb\"}";
        GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
        when(mockResponse.secretString()).thenReturn(secretJson);

        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

        SecretManager secretManager = new SecretManager(newSecretArn());
        setClientField(secretManager, mockClient);
        secretManager.load();

        assertTrue(secretManager.isLoaded());
        assertEquals("localhost", secretManager.get("host"));
        assertEquals("3306", secretManager.get("port"));
        assertEquals("testuser", secretManager.get("username"));
        assertEquals("testpass", secretManager.get("password"));
        assertEquals("testdb", secretManager.get("dbname"));
      }

      /**
       * SecretManager#load が数値の項目を文字列として登録することを確かめる。 接続 URL は文字列の値で組み立てるため、数値を文字列に変換する必要がある。
       *
       * @given Secrets Manager が port に数値 3306 を含む JSON を返す
       * @when load を呼び出す
       * @then get で port を取得すると "3306" を返す
       */
      @Test
      void portが数値の場合_文字列の値を登録する() throws Exception {
        String secretJson = "{\"host\":\"localhost\",\"port\":3306,\"username\":\"user\"}";
        GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
        when(mockResponse.secretString()).thenReturn(secretJson);

        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

        SecretManager secretManager = new SecretManager(newSecretArn());
        setClientField(secretManager, mockClient);
        secretManager.load();

        assertEquals("3306", secretManager.get("port"));
      }

      /**
       * SecretManager#load が取得リクエストの secretId に指定の ARN を設定することを確かめる。 別のシークレットを読むと、接続情報を取り違えるため。
       *
       * @given 前回の読み込みがない状態で、シークレット ARN が一意な値、Secrets Manager が host "host1" の JSON を返す
       * @when load を呼び出す
       * @then Secrets Manager の取得に渡されたリクエストを組み立てると、secretId が指定の ARN になり、リクエストを 1 回送信する
       */
      @Test
      @SuppressWarnings("unchecked")
      void 前回の読み込みがない場合_指定のARNをsecretIdとするリクエストを送信する() throws Exception {
        String arn = newSecretArn();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(Consumer.class)))
            .thenReturn(secretResponse("{\"host\":\"host1\"}"));

        loadWithClient(arn, client);

        ArgumentCaptor<Consumer<GetSecretValueRequest.Builder>> captor =
            (ArgumentCaptor) ArgumentCaptor.forClass(Consumer.class);
        verify(client, times(1)).getSecretValue(captor.capture());
        GetSecretValueRequest.Builder requestBuilder = GetSecretValueRequest.builder();
        captor.getValue().accept(requestBuilder);
        assertEquals(arn, requestBuilder.build().secretId());
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * SecretManager#load が Secrets Manager の例外をそのまま投げることを確かめる。 例外を握りつぶすと、接続情報の取得失敗が見えなくなる。
       *
       * @given Secrets Manager の getSecretValue が RuntimeException "Test error" を投げる
       * @when load を呼び出す
       * @then RuntimeException を投げる
       */
      @Test
      void SecretsManagerの取得が例外を投げた場合_RuntimeExceptionを投げる() throws Exception {
        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(any(Consumer.class)))
            .thenThrow(new RuntimeException("Test error"));

        SecretManager secretManager = new SecretManager(newSecretArn());
        setClientField(secretManager, mockClient);

        assertThrows(RuntimeException.class, secretManager::load);
      }
    }

    /** 境界値のテスト。 */
    @Nested
    class 境界値 {

      /**
       * SecretManager#load が空のシークレットを読み込み済みにしないことを確かめる。 空の値を読み込み済みにすると、未設定の項目の扱いが崩れる。
       *
       * @given Secrets Manager が空文字の secretString を返す
       * @when load を呼び出す
       * @then isLoaded が false を返し、get で nonexistent を取得すると空文字を返す
       */
      @Test
      void シークレットの値が空文字の場合_何もしない() throws Exception {
        GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
        when(mockResponse.secretString()).thenReturn("");

        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

        SecretManager secretManager = new SecretManager(newSecretArn());
        setClientField(secretManager, mockClient);
        secretManager.load();

        assertFalse(secretManager.isLoaded());
        assertEquals("", secretManager.get("nonexistent"));
      }

      /**
       * SecretManager#load が null のシークレットを読み込み済みにしないことを確かめる。 null の値を読み込むと、後続の取得で例外が出る。
       *
       * @given Secrets Manager が secretString に null を返す
       * @when load を呼び出す
       * @then isLoaded が false を返す
       */
      @Test
      void シークレットの値がnullの場合_何もしない() throws Exception {
        GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
        when(mockResponse.secretString()).thenReturn(null);

        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

        SecretManager secretManager = new SecretManager(newSecretArn());
        setClientField(secretManager, mockClient);
        secretManager.load();

        assertFalse(secretManager.isLoaded());
      }

      /**
       * SecretManager#load が前回の読み込みからTTL未満のとき Secrets Manager を呼ばないことを確かめる。 1日以内の再取得を防ぐことで、本番の
       * Secrets Manager の呼び出し回数を変えないための仕様である。
       *
       * @given 前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 86399999 ミリ秒後、Secrets Manager が host
       *     "host1" の JSON を返す
       * @when 同じ ARN の SecretManager で load を 2 回呼び出す
       * @then 2 回目の load で Secrets Manager を呼ばず、get で "host" を取得すると "host1" を返す
       */
      @Test
      void 前回読み込みからの経過が86399999ミリ秒の場合_SecretsManagerを呼ばない() throws Exception {
        String arn = newSecretArn();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(Consumer.class)))
            .thenReturn(secretResponse("{\"host\":\"host1\"}"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        loadWithClient(arn, client);
        ApplicationClock.INSTANCE.setClock(
            fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS - 1)));
        SecretManager second = loadWithClient(arn, client);

        verify(client, times(1)).getSecretValue(any(Consumer.class));
        assertEquals("host1", second.get("host"));
      }

      /**
       * SecretManager#load が前回の読み込みからTTLちょうどのとき Secrets Manager から再読み込みすることを確かめる。 TTL
       * の境界で再読み込みが起きないと、シークレットの更新が1日遅れる。
       *
       * @given 前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 86400000 ミリ秒後、1 回目が host "host1"、2 回目が
       *     host "host2" の JSON を返す
       * @when 同じ ARN の SecretManager で load を 2 回呼び出す
       * @then 2 回目の load で Secrets Manager から読み込み、get で "host" を取得すると "host2" を返す
       */
      @Test
      void 前回読み込みからの経過がTTLちょうどの場合_値を更新する() throws Exception {
        String arn = newSecretArn();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(Consumer.class)))
            .thenReturn(secretResponse("{\"host\":\"host1\"}"))
            .thenReturn(secretResponse("{\"host\":\"host2\"}"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        loadWithClient(arn, client);
        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS)));
        SecretManager second = loadWithClient(arn, client);

        verify(client, times(2)).getSecretValue(any(Consumer.class));
        assertEquals("host2", second.get("host"));
      }

      /**
       * SecretManager#load が CACHE_TTL_MS で指定した値をTTLとして使うことを確かめる。 TTL
       * を設定値で変えられることで、シークレットの再取得の間隔を運用に合わせられる。
       *
       * @given CACHE_TTL_MS が "1000"、前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 1000 ミリ秒後、1 回目が
       *     host "host1"、2 回目が host "host2" の JSON を返す
       * @when 同じ ARN の SecretManager で load を 2 回呼び出す
       * @then 2 回目の load で Secrets Manager から読み込み、get で "host" を取得すると "host2" を返す
       */
      /**
       * SecretManager#load が CACHE_TTL_MS に数値でない値があるとき既定の1日をTTLとして使うことを確かめる。
       * 不正な設定値で再取得が毎回起きると、Secrets Manager の呼び出し回数が増える。
       *
       * @given CACHE_TTL_MS が "abc"、前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 86399999
       *     ミリ秒後、Secrets Manager が host "host1" の JSON を返す
       * @when 同じ ARN の SecretManager で load を 2 回呼び出す
       * @then 2 回目の load で Secrets Manager を呼ばない
       */
      @Test
      void CACHE_TTL_MSが数値でない場合_1日未満の経過ではSecretsManagerを呼ばない() throws Exception {
        Properties.putForTest("CACHE_TTL_MS", "abc");
        String arn = newSecretArn();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(Consumer.class)))
            .thenReturn(secretResponse("{\"host\":\"host1\"}"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        loadWithClient(arn, client);
        ApplicationClock.INSTANCE.setClock(
            fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS - 1)));
        loadWithClient(arn, client);

        verify(client, times(1)).getSecretValue(any(Consumer.class));
      }

      /**
       * SecretManager#load が CACHE_TTL_MS の値が null のとき既定の1日をTTLとして使うことを確かめる。 Parameter Store
       * の値が空の場合も、既定値で動くようにするため。
       *
       * @given Properties に CACHE_TTL_MS の値が null で登録されており、前回の読み込み時刻が
       *     "2026-01-15T03:04:05Z"、現在時刻が前回から 86399999 ミリ秒後、Secrets Manager が host "host1" の JSON
       *     を返す
       * @when 同じ ARN の SecretManager で load を 2 回呼び出す
       * @then 2 回目の load で Secrets Manager を呼ばない
       */
      @Test
      void CACHE_TTL_MSの値がnullの場合_1日未満の経過ではSecretsManagerを呼ばない() throws Exception {
        putNullPropertyByReflection("CACHE_TTL_MS");
        String arn = newSecretArn();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(Consumer.class)))
            .thenReturn(secretResponse("{\"host\":\"host1\"}"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        loadWithClient(arn, client);
        ApplicationClock.INSTANCE.setClock(
            fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS - 1)));
        loadWithClient(arn, client);

        verify(client, times(1)).getSecretValue(any(Consumer.class));
      }

      /**
       * SecretManager#load が CACHE_TTL_MS が空の場合に既定の1日をTTLとして使うことを確かめる。
       * 空の設定値を数値として解析すると失敗するため、既定値へ戻す分岐が必要である。
       *
       * @given CACHE_TTL_MS が空文字、前回の読み込み時刻が "2026-01-15T03:04:05Z"、現在時刻が前回から 86399999 ミリ秒後、Secrets
       *     Manager が host "host1" の JSON を返す
       * @when 同じ ARN の SecretManager で load を 2 回呼び出す
       * @then 2 回目の load で Secrets Manager を呼ばない
       */
      @Test
      void CACHE_TTL_MSが空の場合_1日未満の経過ではSecretsManagerを呼ばない() throws Exception {
        Properties.putForTest("CACHE_TTL_MS", "");
        String arn = newSecretArn();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(Consumer.class)))
            .thenReturn(secretResponse("{\"host\":\"host1\"}"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        loadWithClient(arn, client);
        ApplicationClock.INSTANCE.setClock(
            fixedClockAt(BASE_INSTANT.plusMillis(DEFAULT_TTL_MS - 1)));
        loadWithClient(arn, client);

        verify(client, times(1)).getSecretValue(any(Consumer.class));
      }

      @Test
      void CACHE_TTL_MSが1000の場合_1000ミリ秒経過で値を更新する() throws Exception {
        Properties.putForTest("CACHE_TTL_MS", "1000");
        String arn = newSecretArn();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(Consumer.class)))
            .thenReturn(secretResponse("{\"host\":\"host1\"}"))
            .thenReturn(secretResponse("{\"host\":\"host2\"}"));

        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT));
        loadWithClient(arn, client);
        ApplicationClock.INSTANCE.setClock(fixedClockAt(BASE_INSTANT.plusMillis(1000)));
        SecretManager second = loadWithClient(arn, client);

        assertEquals("host2", second.get("host"));
      }
    }
  }

  /** SecretManager の取得メソッドのテスト。 */
  @Nested
  class get {

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * SecretManager#get が未登録のキーに空文字を返すことを確かめる。 get は空文字、getOrNull は null を返す仕様である。
       *
       * @given 読み込み済みのシークレットが host と port だけを持つ
       * @when get と getOrNull を nonexistent で呼び出す
       * @then get が空文字を返し、getOrNull が null を返す
       */
      @Test
      void 未登録のキーの場合_空文字を返す() throws Exception {
        String secretJson = "{\"host\":\"localhost\",\"port\":\"3306\"}";
        GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
        when(mockResponse.secretString()).thenReturn(secretJson);

        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

        SecretManager secretManager = new SecretManager(newSecretArn());
        setClientField(secretManager, mockClient);
        secretManager.load();

        assertEquals("", secretManager.get("nonexistent"));
        assertNull(secretManager.getOrNull("nonexistent"));
      }
    }
  }

  /** SecretManager の getAll のテスト。 */
  @Nested
  class getAll {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * SecretManager#getAll が全項目を複製したマップを返すことを確かめる。 全項目を一度に取得できないと、接続情報をまとめて扱えない。
       *
       * @given 読み込み済みのシークレットが host、port、username の 3 項目を持つ
       * @when getAll を呼び出す
       * @then サイズが 3 のマップを返し、host が localhost、port が 3306、username が user を返す
       */
      @Test
      void 項目が3件の場合_全項目のマップを返す() throws Exception {
        String secretJson = "{\"host\":\"localhost\",\"port\":\"3306\",\"username\":\"user\"}";
        GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
        when(mockResponse.secretString()).thenReturn(secretJson);

        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

        SecretManager secretManager = new SecretManager(newSecretArn());
        setClientField(secretManager, mockClient);
        secretManager.load();

        Map<String, String> allValues = secretManager.getAll();
        assertEquals(3, allValues.size());
        assertEquals("localhost", allValues.get("host"));
        assertEquals("3306", allValues.get("port"));
        assertEquals("user", allValues.get("username"));
      }
    }
  }

  /** {@link SecretManager#close()} のテスト。 */
  @Nested
  class close {

    /** 境界値のテスト。 */
    @Nested
    class 境界値 {

      /**
       * SecretManager#close がクライアントを持たない場合に例外を投げないことを確かめる。 後始末の経路で例外が出ると、接続情報の取得後の処理が中断されるため。
       *
       * @given SecretManager のクライアントを null に設定している
       * @when close を引数なしで呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void クライアントがnullの場合_何もしない() throws Exception {
        SecretManager secretManager = new SecretManager(newSecretArn());
        setClientField(secretManager, null);

        assertDoesNotThrow(secretManager::close);
      }
    }
  }

  /**
   * Properties の内部マップに値が null のキーを直接登録します。
   *
   * <p>Properties は null の値を登録させない。Parameter Store
   * が値のないパラメータを返した場合の状態を再現するため、既存のテストと同じくリフレクションで登録する。
   *
   * @param key 登録するキー
   * @throws IllegalStateException リフレクションに失敗した場合
   */
  @SuppressWarnings("unchecked")
  private static void putNullPropertyByReflection(final String key) {
    try {
      Field field = Properties.class.getDeclaredField("properties");
      field.setAccessible(true);
      ((Map<String, String>) field.get(null)).put(key, null);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * テスト用の一意な ARN を作ります。
   *
   * <p>グローバルキャッシュは ARN をキーにするため、テストごとに別の ARN を使って他のテストのキャッシュを読まないようにする。
   *
   * @return テストごとに一意なシークレット ARN
   */
  private static String newSecretArn() {
    return "arn:aws:secretsmanager:region:account:secret:test-" + UUID.randomUUID();
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
   * シークレットの JSON を持つ応答を作ります。
   *
   * <p>応答は SDK のビルダーで実体を作る。モックにすると、スタブの戻り値を作る処理が別のスタブと干渉する。
   *
   * @param secretJson シークレットの JSON 文字列
   * @return secretString が指定の JSON を返す応答
   */
  private static GetSecretValueResponse secretResponse(final String secretJson) {
    return GetSecretValueResponse.builder().secretString(secretJson).build();
  }

  /**
   * 指定の ARN と Secrets Manager クライアントで SecretManager を読み込みます。
   *
   * @param arn シークレット ARN
   * @param client 差し替える Secrets Manager クライアント
   * @return 読み込み済みの SecretManager
   * @throws Exception 読み込みに失敗した場合、またはリフレクションに失敗した場合
   */
  private SecretManager loadWithClient(final String arn, final SecretsManagerClient client)
      throws Exception {
    SecretManager secretManager = new SecretManager(arn);
    setClientField(secretManager, client);
    secretManager.load();
    return secretManager;
  }

  private void setClientField(SecretManager secretManager, SecretsManagerClient mockClient)
      throws NoSuchFieldException, IllegalAccessException {
    Field clientField = SecretManager.class.getDeclaredField("client");
    clientField.setAccessible(true);
    clientField.set(secretManager, mockClient);
  }
}
