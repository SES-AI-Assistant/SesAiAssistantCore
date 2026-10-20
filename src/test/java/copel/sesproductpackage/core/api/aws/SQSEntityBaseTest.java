package copel.sesproductpackage.core.api.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** {@link SQSEntityBase} の単体テスト。 */
class SQSEntityBaseTest {

  private static final String QUEUE_URL = "https://sqs.ap-northeast-1.amazonaws.com/123/my-queue";

  @Nested
  class コンストラクタ_Regions_String {

    @Nested
    class 正常系 {

      /**
       * エンドポイントの指定が無い場合に、リージョンを指定して SQS のクライアントを作ることを確かめる。 本番の既定の接続先で送信できるようにするため、リージョンを使うことを守る。
       *
       * @given システムプロパティ aws.endpointUrl と環境変数 AWS_ENDPOINT_URL が未設定である
       * @when 設定を外した状態で、リージョン AP_NORTHEAST_1 とキューの URL を指定して生成する
       * @then リージョンを指定してクライアントを作る
       */
      @Test
      void エンドポイントが未設定の場合_リージョンを指定したクライアントを返す() throws Exception {
        SystemLambda.withEnvironmentVariable("AWS_ENDPOINT_URL", null)
            .execute(
                () -> {
                  System.clearProperty("aws.endpointUrl");
                  try (MockedStatic<AmazonSQSClientBuilder> sqs =
                      mockStatic(AmazonSQSClientBuilder.class)) {
                    AmazonSQSClientBuilder builder = prepareBuilder(sqs);

                    new TestEntity(Regions.AP_NORTHEAST_1, QUEUE_URL);

                    verify(builder).withRegion(Regions.AP_NORTHEAST_1);
                  }
                });
      }

      /**
       * エンドポイントの指定がある場合に、そのエンドポイントでクライアントを作ることを確かめる。 ローカルの検証環境へ送信できるようにするため、指定の接続先を使うことを守る。
       *
       * @given システムプロパティ aws.endpointUrl が http://localhost:4566 である
       * @when リージョン AP_NORTHEAST_1 とキューの URL を指定して生成する
       * @then エンドポイントの設定を付けてクライアントを作る
       */
      @Test
      void エンドポイントが指定されている場合_そのエンドポイントを指定したクライアントを返す() {
        System.setProperty("aws.endpointUrl", "http://localhost:4566");
        try (MockedStatic<AmazonSQSClientBuilder> sqs = mockStatic(AmazonSQSClientBuilder.class)) {
          AmazonSQSClientBuilder builder = prepareBuilder(sqs);

          new TestEntity(Regions.AP_NORTHEAST_1, QUEUE_URL);

          verify(builder).withEndpointConfiguration(any());
        } finally {
          System.clearProperty("aws.endpointUrl");
        }
      }
    }
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 引数なしで生成した場合に、キューの URL とクライアントを持たないことを確かめる。 JSON から読み込む時に使う生成で、送信先を勝手に設定しないことを守る。
       *
       * @given 引数なしの生成がある
       * @when TestEntity を引数なしで生成する
       * @then キュー名は null を返す
       */
      @Test
      void 引数なしで生成する場合_キュー名はnullを返す() {
        assertThat(new TestEntity().getQueueName()).isNull();
      }
    }
  }

  @Nested
  class sendMessage {

    @Nested
    class 正常系 {

      /**
       * メッセージを送信する場合に、キューの URL と本文を付けて送信することを確かめる。 登録のイベントが送信先と内容で届くことを守る。
       *
       * @given 本文が {"a":1} を返す TestEntity が、キューの URL を持つ
       * @when sendMessage を呼び出す
       * @then キューの URL と本文を付けて送信し、送信結果のメッセージIDを返す
       */
      @Test
      void 本文を作れる場合_送信結果を返す() throws Exception {
        try (MockedStatic<AmazonSQSClientBuilder> sqs = mockStatic(AmazonSQSClientBuilder.class)) {
          AmazonSQS client = mock(AmazonSQS.class);
          prepareBuilder(sqs, client);
          when(client.sendMessage(any(SendMessageRequest.class)))
              .thenReturn(new SendMessageResult().withMessageId("m1"));
          TestEntity entity = new TestEntity(Regions.AP_NORTHEAST_1, QUEUE_URL);

          SendMessageResult result = entity.sendMessage();

          ArgumentCaptor<SendMessageRequest> captor =
              ArgumentCaptor.forClass(SendMessageRequest.class);
          verify(client).sendMessage(captor.capture());
          assertThat(captor.getValue().getQueueUrl()).isEqualTo(QUEUE_URL);
          assertThat(captor.getValue().getMessageBody()).isEqualTo("{\"a\":1}");
          assertThat(result.getMessageId()).isEqualTo("m1");
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 本文を JSON にできない場合に、例外を投げて送信しないことを確かめる。 壊れた本文を送信しないことを守る。
       *
       * @given 本文の作成で JsonProcessingException を投げる TestEntity がある
       * @when sendMessage を呼び出す
       * @then JsonProcessingException を投げる
       */
      @Test
      void 本文をJSONにできない場合_JsonProcessingExceptionを投げる() {
        TestEntity entity =
            new TestEntity(Regions.AP_NORTHEAST_1, QUEUE_URL) {
              @Override
              protected String getMessageBody() throws JsonProcessingException {
                throw new JsonProcessingException("変換失敗") {};
              }
            };

        assertThatThrownBy(entity::sendMessage).isInstanceOf(JsonProcessingException.class);
      }
    }
  }

  @Nested
  class getQueueName {

    @Nested
    class 正常系 {

      /**
       * キューの URL を持つ場合に、最後の階層のキュー名を返すことを確かめる。 ログに送信先のキュー名を残すため、URL の末尾を取り出すことを守る。
       *
       * @given キューの URL が https://.../my-queue である
       * @when getQueueName を呼び出す
       * @then my-queue を返す
       */
      @Test
      void キューのURLを持つ場合_最後の階層のキュー名を返す() {
        assertThat(new TestEntity(Regions.AP_NORTHEAST_1, QUEUE_URL).getQueueName())
            .isEqualTo("my-queue");
      }
    }
  }

  @Nested
  class toQueueName {

    @Nested
    class 正常系 {

      /**
       * キューの URL から最後の階層を取り出すことを確かめる。 キュー名は IaC で採番されるため、URL から取り出すことを守る。
       *
       * @given キューの URL が https://.../my-queue である
       * @when toQueueName を呼び出す
       * @then my-queue を返す
       */
      @Test
      void キューのURLを指定した場合_最後の階層を返す() {
        assertThat(SQSEntityBase.toQueueName(QUEUE_URL)).isEqualTo("my-queue");
      }
    }

    @Nested
    class 境界値 {

      /**
       * キューの URL が null の場合に、null を返すことを確かめる。 URL が無いまま解析して例外を投げないことを守る。
       *
       * @given キューの URL が null である
       * @when toQueueName を呼び出す
       * @then null を返す
       */
      @Test
      void キューのURLがnullの場合_nullを返す() {
        assertThat(SQSEntityBase.toQueueName(null)).isNull();
      }
    }
  }

  /**
   * AmazonSQSClientBuilder の生成を差し替え、ビルダーを返す.
   *
   * @param sqs AmazonSQSClientBuilder の静的モック
   * @return ビルダーのモック
   */
  private static AmazonSQSClientBuilder prepareBuilder(
      final MockedStatic<AmazonSQSClientBuilder> sqs) {
    return prepareBuilder(sqs, mock(AmazonSQS.class));
  }

  /**
   * AmazonSQSClientBuilder の生成を差し替え、指定のクライアントを返す.
   *
   * @param sqs AmazonSQSClientBuilder の静的モック
   * @param client 返すクライアント
   * @return ビルダーのモック
   */
  private static AmazonSQSClientBuilder prepareBuilder(
      final MockedStatic<AmazonSQSClientBuilder> sqs, final AmazonSQS client) {
    AmazonSQSClientBuilder builder = mock(AmazonSQSClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(client);
    sqs.when(AmazonSQSClientBuilder::standard).thenReturn(builder);
    return builder;
  }

  /** テスト用のメッセージ。本文は {"a":1} を返す。 */
  static class TestEntity extends SQSEntityBase {

    /** 引数なしの生成。 */
    TestEntity() {
      super();
    }

    /**
     * リージョンとキューの URL を指定して生成する.
     *
     * @param region リージョン
     * @param queueUrl キューの URL
     */
    TestEntity(final Regions region, final String queueUrl) {
      super(region, queueUrl);
    }

    @Override
    protected String getMessageBody() throws JsonProcessingException {
      return "{\"a\":1}";
    }
  }
}
