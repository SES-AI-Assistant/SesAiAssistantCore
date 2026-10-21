package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.ApiType;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

/** {@link SES_AI_API_USAGE_HISTORYLot} の単体テスト。 */
@TestProperty(
    key = "infrastructure/dynamodb/api-usage-history-table/name",
    value = "api-usage-history")
class SES_AI_API_USAGE_HISTORYLotTest {

  /** テーブルのモック。 */
  private DynamoDbTable<SES_AI_API_USAGE_HISTORY> table;

  /** 拡張クライアントの静的メソッドのモック。テスト後に解除する。 */
  private MockedStatic<DynamoDbEnhancedClient> enhanced;

  /** DynamoDB の拡張クライアントをモックに差し替え、テーブルのモックを返すように設定する。 */
  @BeforeEach
  void DynamoDBの拡張クライアントをモックに差し替える() {
    enhanced = mockStatic(DynamoDbEnhancedClient.class);
    DynamoDbEnhancedClient.Builder builder =
        mock(DynamoDbEnhancedClient.Builder.class, RETURNS_SELF);
    DynamoDbEnhancedClient client = mock(DynamoDbEnhancedClient.class);
    enhanced.when(DynamoDbEnhancedClient::builder).thenReturn(builder);
    when(builder.build()).thenReturn(client);
    table = mock(DynamoDbTable.class);
    doReturn(table).when(client).table(anyString(), any(TableSchema.class));
  }

  /** 拡張クライアントの静的メソッドの差し替えを解除する。 */
  @AfterEach
  void 拡張クライアントの差し替えを解除する() {
    enhanced.close();
  }

  /**
   * 検索結果として返すページのモックを作り、テーブルの検索に設定する。
   *
   * @param records 検索結果のレコード
   */
  @SuppressWarnings("unchecked")
  private void stubScanResult(final List<SES_AI_API_USAGE_HISTORY> records) {
    PageIterable<SES_AI_API_USAGE_HISTORY> pages = mock(PageIterable.class);
    SdkIterable<SES_AI_API_USAGE_HISTORY> items = records::iterator;
    when(pages.items()).thenReturn(items);
    doReturn(pages).when(table).scan(any(ScanEnhancedRequest.class));
  }

  /**
   * 利用履歴を1件生成する。使用月だけを指定する。
   *
   * @param usageMonth 使用月
   * @return 生成した利用履歴
   */
  private static SES_AI_API_USAGE_HISTORY history(final String usageMonth) {
    SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
    history.setProvider(Provider.Google);
    history.setModel("gemini-1.5-flash");
    history.setUsageMonth(usageMonth);
    history.setUsageDate("20250115");
    history.setUserId("U001");
    history.setApiType(ApiType.Embedding);
    return history;
  }

  @Nested
  class fetchByUsageMonth_String_String {

    @Nested
    class 正常系 {

      /**
       * 年と月を指定すると、年と月を連結した使用月で検索することを確かめる。 画面の月指定は年と月を別々に受け取る。
       *
       * @given 年が "2025"、月が "01"、使用月 "202501" の利用履歴が1件ある
       * @when fetchByUsageMonth を年と月で実行する
       * @then 使用月の条件に "202501" を指定し、1件の一覧を保持する
       */
      @Test
      void 年が2025で月が01の場合_202501の1件を返す() {
        stubScanResult(List.of(history("202501")));
        SES_AI_API_USAGE_HISTORYLot lot = new SES_AI_API_USAGE_HISTORYLot();

        lot.fetchByUsageMonth("2025", "01");

        ArgumentCaptor<ScanEnhancedRequest> captor =
            ArgumentCaptor.forClass(ScanEnhancedRequest.class);
        verify(table).scan(captor.capture());
        Expression filter = captor.getValue().filterExpression();
        assertThat(filter.expressionValues().get(":val").s()).isEqualTo("202501");
        assertThat(filter.expressionNames().get("#col")).isEqualTo("usageMonth");
        assertThat(lot.entityLot).hasSize(1);
        assertThat(lot.entityLot.get(0).getUsageMonth()).isEqualTo("202501");
      }
    }
  }

  @Nested
  class fetchByUsageMonth_String {

    @Nested
    class 正常系 {

      /**
       * 年月を指定すると、その使用月で検索することを確かめる。 月の集計は年月単位で行う。
       *
       * @given 年月が "202501"、使用月 "202501" の利用履歴が1件ある
       * @when fetchByUsageMonth を年月で実行する
       * @then 使用月の条件に "202501" を指定し、1件の一覧を保持する
       */
      @Test
      void 年月が202501の場合_202501の1件を返す() {
        stubScanResult(List.of(history("202501")));
        SES_AI_API_USAGE_HISTORYLot lot = new SES_AI_API_USAGE_HISTORYLot();

        lot.fetchByUsageMonth("202501");

        ArgumentCaptor<ScanEnhancedRequest> captor =
            ArgumentCaptor.forClass(ScanEnhancedRequest.class);
        verify(table).scan(captor.capture());
        assertThat(captor.getValue().filterExpression().expressionValues().get(":val").s())
            .isEqualTo("202501");
        assertThat(lot.entityLot).hasSize(1);
      }
    }
  }
}
