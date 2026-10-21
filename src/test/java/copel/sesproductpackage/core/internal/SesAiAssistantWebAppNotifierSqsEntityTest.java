package copel.sesproductpackage.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** {@link SesAiAssistantWebAppNotifierSqsEntity} の単体テスト。 */
@TestProperty(
    key = "infrastructure/messaging/notifier-queue/url",
    value = "https://sqs.ap-northeast-1.amazonaws.com/123/notifier-queue")
class SesAiAssistantWebAppNotifierSqsEntityTest {

  /** SQS クライアントのビルダーの静的メソッドのモック。テスト後に解除する。 */
  private MockedStatic<AmazonSQSClientBuilder> builderStatic;

  /** SQS クライアントの作成をモックに差し替える。 生成時に実 AWS へ接続しないようにするため。 */
  @BeforeEach
  void SQSクライアントの作成をモックに差し替える() {
    builderStatic = mockStatic(AmazonSQSClientBuilder.class);
    AmazonSQSClientBuilder builder = mock(AmazonSQSClientBuilder.class);
    lenient().when(builder.withRegion(any(Regions.class))).thenReturn(builder);
    lenient().when(builder.withEndpointConfiguration(any())).thenReturn(builder);
    lenient().when(builder.withCredentials(any())).thenReturn(builder);
    when(builder.build()).thenReturn(mock(AmazonSQS.class));
    builderStatic.when(AmazonSQSClientBuilder::standard).thenReturn(builder);
  }

  /** SQS クライアントの作成のモックを解除する。 */
  @AfterEach
  void SQSクライアントの作成のモックを解除する() {
    builderStatic.close();
  }

  /**
   * テストで使うプッシュ通知の内容を作る。
   *
   * @return 全項目を持つプッシュ通知リクエスト
   */
  private static SesAiAssistantWebAppNotifierRequestEntity 全項目のリクエストを作る() {
    return new SesAiAssistantWebAppNotifierRequestEntity(
        "U001", "新着", "案件が届きました", "/icon.png", "/badge.png", "job", "/jobs");
  }

  @Nested
  class getMessageBody {

    @Nested
    class 正常系 {

      /**
       * 通知の全項目を、スネークケースのキーを持つ JSON で返すことを確かめる。 Web Push の受信側はこのキー名で内容を読むためである。
       *
       * @given ユーザーIDが "U001"、タイトルが "新着"、本文が "案件が届きました"、残りの項目が全て値を持つ "job"、遷移先が "/jobs"
       * @when getMessageBody を実行する
       * @then 全項目をスネークケースのキーに入れた JSON 本文を返す
       */
      @Test
      void 全項目を持つ場合_スネークケースのキーでJSON本文を返す() throws Exception {
        SesAiAssistantWebAppNotifierSqsEntity target =
            new SesAiAssistantWebAppNotifierSqsEntity(全項目のリクエストを作る());

        String json = target.getMessageBody();

        assertThat(json)
            .contains("\"user_id\":\"U001\"")
            .contains("\"title\":\"新着\"")
            .contains("\"body\":\"案件が届きました\"")
            .contains("\"icon\":\"/icon.png\"")
            .contains("\"badge\":\"/badge.png\"")
            .contains("\"tag\":\"job\"")
            .contains("\"url\":\"/jobs\"");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 任意の項目が null の場合に、そのキーの値を null として出力することを確かめる。 タグや遷移先を省略した通知でも、受信側が項目の欠落に気付けるようにするためである。
       *
       * @given ユーザーIDが "U001"、タイトルが "新着"、それ以外の項目が null
       * @when getMessageBody を実行する
       * @then 省略した項目を null として含む JSON 本文を返す
       */
      @Test
      void 任意の項目がnullの場合_そのキーをnullとしたJSON本文を返す() throws Exception {
        SesAiAssistantWebAppNotifierSqsEntity target =
            new SesAiAssistantWebAppNotifierSqsEntity(
                new SesAiAssistantWebAppNotifierRequestEntity(
                    "U001", "新着", null, null, null, null, null));

        String json = target.getMessageBody();

        assertThat(json).contains("\"body\":null").contains("\"url\":null");
      }
    }
  }
}
