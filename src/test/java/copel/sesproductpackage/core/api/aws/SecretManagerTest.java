package copel.sesproductpackage.core.api.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * {@link SecretManager} の単体テスト。
 *
 * <p>取得結果は ARN ごとに static キャッシュされるため、テストごとに一意な ARN を使う。
 */
class SecretManagerTest {

  private static final String JSON = "{\"host\":\"db.example\",\"port\":5432}";

  /**
   * テストごとに一意な ARN を作る.
   *
   * @return ARN
   */
  private static String newArn() {
    return "arn:aws:secretsmanager:test:" + UUID.randomUUID();
  }

  @Nested
  class コンストラクタ_String_Region {

    @Nested
    class 正常系 {

      /**
       * リージョンを指定した場合に、そのリージョンでクライアントを作ることを確かめる。 シークレットを置いたリージョンへ問い合わせるため、指定の値を使うことを守る。
       *
       * @given ARN とリージョン US_EAST_1 がある
       * @when SecretManager を生成する
       * @then US_EAST_1 のリージョンを指定してクライアントを作る
       */
      @Test
      void リージョンを指定した場合_指定のリージョンを使うクライアントを返す() {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClientBuilder builder = prepareBuilder(sm);

          new SecretManager(newArn(), Region.US_EAST_1);

          verify(builder).region(Region.US_EAST_1);
        }
      }
    }
  }

  @Nested
  class コンストラクタ_String {

    @Nested
    class 正常系 {

      /**
       * ARN だけを指定した場合に、既定のリージョンでクライアントを作ることを確かめる。 呼び出し側でリージョンを省けるようにするため、既定の値を使うことを守る。
       *
       * @given ARN だけがある
       * @when SecretManager を生成する
       * @then ap-northeast-1 のリージョンを指定してクライアントを作る
       */
      @Test
      void ARNのみの場合_既定のリージョンを使うクライアントを返す() {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClientBuilder builder = prepareBuilder(sm);

          new SecretManager(newArn());

          verify(builder).region(Region.AP_NORTHEAST_1);
        }
      }
    }
  }

  @Nested
  class load {

    @Nested
    class 正常系 {

      /**
       * JSON のシークレットを読み込んだ場合に、キーごとの値を取り出せることを確かめる。 接続情報を JSON から読み込んで使うため、値の型を文字列にそろえることを守る。
       *
       * @given シークレットの本文が host と port を持つ JSON である
       * @when load を呼び出す
       * @then host の値と port の値を文字列として取り出せる
       */
      @Test
      void JSONのシークレットを読み込んだ場合_読み込んだ値を返す() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClient client = prepareClient(sm, JSON);
          SecretManager manager = new SecretManager(newArn(), Region.AP_NORTHEAST_1);

          manager.load();

          assertThat(manager.get("host")).isEqualTo("db.example");
          assertThat(manager.get("port")).isEqualTo("5432");
          verify(client, times(1)).getSecretValue(any(java.util.function.Consumer.class));
        }
      }

      /**
       * 同じ ARN を取得時刻から TTL の内に再度読み込む場合に、取得を行わないことを確かめる。 取得の回数を抑えて、シークレットの問い合わせを減らすことを守る。
       *
       * @given 同じ ARN のシークレットを1回読み込み、現在時刻が変わらない
       * @when 別の SecretManager で同じ ARN を load する
       * @then 2回目は取得を呼ばず、キャッシュの値を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 同じARNを再度読み込む場合_取得を呼ばずキャッシュの値を返す() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClient client = prepareClient(sm, JSON);
          String arn = newArn();
          new SecretManager(arn, Region.AP_NORTHEAST_1).load();
          SecretManager second = new SecretManager(arn, Region.AP_NORTHEAST_1);

          second.load();

          verify(client, times(1)).getSecretValue(any(java.util.function.Consumer.class));
          assertThat(second.get("host")).isEqualTo("db.example");
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 取得が失敗した場合に、例外を呼び出し元へ投げることを確かめる。 接続情報が無いまま処理を続けると、誤った接続先へ進むことを守る。
       *
       * @given 取得の呼び出しが RuntimeException を投げる
       * @when load を呼び出す
       * @then RuntimeException を投げる
       */
      @Test
      void 取得が失敗する場合_RuntimeExceptionを投げる() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClient client = prepareClient(sm, JSON);
          doThrow(new IllegalStateException("接続失敗"))
              .when(client)
              .getSecretValue(any(java.util.function.Consumer.class));
          SecretManager manager = new SecretManager(newArn(), Region.AP_NORTHEAST_1);

          assertThatThrownBy(manager::load).isInstanceOf(IllegalStateException.class);
        }
      }

      /**
       * シークレットの本文が JSON として解析できない場合に、例外を投げることを確かめる。 解析できない値を部分的に読み込まないことを守る。
       *
       * @given シークレットの本文が JSON ではない文字列である
       * @when load を呼び出す
       * @then JsonProcessingException を投げる
       */
      @Test
      void JSONとして解析できない場合_JsonProcessingExceptionを投げる() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          prepareClient(sm, "not json");
          SecretManager manager = new SecretManager(newArn(), Region.AP_NORTHEAST_1);

          assertThatThrownBy(manager::load).isInstanceOf(JsonProcessingException.class);
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * TTL が 0 の場合に、キャッシュを使わず取得し直した値を返すことを確かめる。 TTL の設定で期限切れの扱いを変えられることを守る。
       *
       * @given 設定 CACHE_TTL_MS が 0 で、同じ ARN を1回読み込んでいる
       * @when 同じ ARN を再度 load する
       * @then 取得をもう一度行い、再取得した値を返す
       */
      @Test
      @TestProperty(key = "CACHE_TTL_MS", value = "0")
      @FixedClock("2026-10-20T10:00:00+09:00")
      void TTLが0の場合_再取得した値を返す() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClient client = prepareClient(sm, JSON);
          String arn = newArn();
          new SecretManager(arn, Region.AP_NORTHEAST_1).load();
          SecretManager second = new SecretManager(arn, Region.AP_NORTHEAST_1);

          second.load();

          verify(client, times(2)).getSecretValue(any(java.util.function.Consumer.class));
          assertThat(second.get("port")).isEqualTo("5432");
        }
      }

      /**
       * TTL の設定が数値でない場合に、既定の TTL でキャッシュを使うことを確かめる。 設定の誤りで取得が毎回走らないようにするため、既定の値に戻すことを守る。
       *
       * @given 設定 CACHE_TTL_MS が abc で、同じ ARN を1回読み込んでいる
       * @when 同じ ARN を再度 load する
       * @then 取得を呼ばず、キャッシュの値を返す
       */
      @Test
      @TestProperty(key = "CACHE_TTL_MS", value = "abc")
      @FixedClock("2026-10-20T10:00:00+09:00")
      void TTLが数値でない場合_既定のTTLでキャッシュを使い取得を呼ばない() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClient client = prepareClient(sm, JSON);
          String arn = newArn();
          new SecretManager(arn, Region.AP_NORTHEAST_1).load();

          new SecretManager(arn, Region.AP_NORTHEAST_1).load();

          verify(client, times(1)).getSecretValue(any(java.util.function.Consumer.class));
        }
      }

      /**
       * TTL の設定が空文字の場合に、既定の TTL でキャッシュを使うことを確かめる。 空の設定を 0 として扱い、キャッシュを無効にしないことを守る。
       *
       * @given 設定 CACHE_TTL_MS が空文字で、同じ ARN を1回読み込んでいる
       * @when 同じ ARN を再度 load する
       * @then 取得を呼ばず、キャッシュの値を返す
       */
      @Test
      @TestProperty(key = "CACHE_TTL_MS", value = "")
      @FixedClock("2026-10-20T10:00:00+09:00")
      void TTLが空文字の場合_既定のTTLでキャッシュを使い取得を呼ばない() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClient client = prepareClient(sm, JSON);
          String arn = newArn();
          new SecretManager(arn, Region.AP_NORTHEAST_1).load();

          new SecretManager(arn, Region.AP_NORTHEAST_1).load();

          verify(client, times(1)).getSecretValue(any(java.util.function.Consumer.class));
        }
      }

      /**
       * シークレットの値が空文字の場合に、値を取り出せないことを確かめる。 空の値を有効な設定として扱わないことを守る。
       *
       * @given シークレットの本文が空文字である
       * @when load を呼び出す
       * @then キーの値は空文字として取り出せる
       */
      @Test
      void シークレットの値が空文字の場合_空文字を返す() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          prepareClient(sm, "");
          SecretManager manager = new SecretManager(newArn(), Region.AP_NORTHEAST_1);

          manager.load();

          assertThat(manager.get("host")).isEmpty();
        }
      }

      /**
       * シークレットの値が未設定の場合に、値を取り出せないことを確かめる。 値が無い応答で例外を投げず、空として扱うことを守る。
       *
       * @given シークレットの本文が未設定である
       * @when load を呼び出す
       * @then キーの値は空文字として取り出せる
       */
      @Test
      void シークレットの値が未設定の場合_空文字を返す() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          prepareClient(sm, null);
          SecretManager manager = new SecretManager(newArn(), Region.AP_NORTHEAST_1);

          manager.load();

          assertThat(manager.get("host")).isEmpty();
        }
      }
    }
  }

  @Nested
  class get {

    @Nested
    class 正常系 {

      /**
       * キーが存在する場合に、その値を返すことを確かめる。 読み込んだ接続情報を呼び出し元で使えるようにすることを守る。
       *
       * @given シークレットに host が db.example として含まれ、load 済みである
       * @when get を host で呼び出す
       * @then db.example を返す
       */
      @Test
      void キーが存在する場合_値を返す() throws Exception {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          prepareClient(sm, JSON);
          SecretManager manager = new SecretManager(newArn(), Region.AP_NORTHEAST_1);
          manager.load();

          assertThat(manager.get("host")).isEqualTo("db.example");
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * キーが存在しない場合に、空文字を返すことを確かめる。 存在しない項目で例外にせず、空の値として扱うことを守る。
       *
       * @given シークレットに含まれないキー missing がある
       * @when get を missing で呼び出す
       * @then 空文字を返す
       */
      @Test
      void キーが存在しない場合_空文字を返す() {
        assertThat(new SecretManager(newArn(), Region.AP_NORTHEAST_1).get("missing")).isEmpty();
      }
    }
  }

  @Nested
  class close {

    @Nested
    class 正常系 {

      /**
       * クライアントが存在する場合に、クライアントを閉じることを確かめる。 接続の資源を解放して、使い終わったクライアントを残さないことを守る。
       *
       * @given クライアントを持つ SecretManager がある
       * @when close を呼び出す
       * @then クライアントの close を1回呼び出す
       */
      @Test
      void クライアントが存在する場合_クライアントの接続資源を削除する() {
        try (MockedStatic<SecretsManagerClient> sm = mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClient client = mock(SecretsManagerClient.class);
          SecretsManagerClientBuilder builder =
              mock(SecretsManagerClientBuilder.class, RETURNS_SELF);
          when(builder.build()).thenReturn(client);
          sm.when(SecretsManagerClient::builder).thenReturn(builder);

          new SecretManager(newArn(), Region.AP_NORTHEAST_1).close();

          verify(client).close();
        }
      }
    }
  }

  /**
   * SecretsManagerClient の生成を差し替え、ビルダーを返す.
   *
   * @param sm SecretsManagerClient の静的モック
   * @return ビルダーのモック
   */
  private static SecretsManagerClientBuilder prepareBuilder(
      final MockedStatic<SecretsManagerClient> sm) {
    SecretsManagerClientBuilder builder = mock(SecretsManagerClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(mock(SecretsManagerClient.class));
    sm.when(SecretsManagerClient::builder).thenReturn(builder);
    return builder;
  }

  /**
   * 指定の本文を返すクライアントを差し替えて作る.
   *
   * @param sm SecretsManagerClient の静的モック
   * @param secretString シークレットの本文（null の場合は未設定の応答）
   * @return クライアントのモック
   */
  private static SecretsManagerClient prepareClient(
      final MockedStatic<SecretsManagerClient> sm, final String secretString) {
    SecretsManagerClientBuilder builder = mock(SecretsManagerClientBuilder.class, RETURNS_SELF);
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    when(builder.build()).thenReturn(client);
    sm.when(SecretsManagerClient::builder).thenReturn(builder);
    GetSecretValueResponse.Builder response = GetSecretValueResponse.builder();
    if (secretString != null) {
      response.secretString(secretString);
    }
    // 取得の指定（ARN を設定するラムダ）も実行して、ARN の設定を含めて検証する
    doAnswer(
            invocation -> {
              java.util.function.Consumer<GetSecretValueRequest.Builder> request =
                  invocation.getArgument(0);
              request.accept(GetSecretValueRequest.builder());
              return response.build();
            })
        .when(client)
        .getSecretValue(any(java.util.function.Consumer.class));
    return client;
  }
}
