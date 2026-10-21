package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_M_INGEST_ROUTE.ChannelType;
import copel.sesproductpackage.core.database.base.DynamoDbClientFactory;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/** {@link SES_AI_TENANT_MONTHLY_INGEST_COUNT} の単体テスト。 */
@FixedClock("2026-01-15T12:00:00+09:00")
@TestProperty(
    key = "infrastructure/dynamodb/tenant-monthly-ingest-count-table/name",
    value = "ingest-count-table")
class SES_AI_TENANT_MONTHLY_INGEST_COUNTTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 年月。 */
  private static final String YEAR_MONTH = "202601";

  /** 拡張クライアントのモック。テーブルの作成名を確かめるために使う。 */
  private DynamoDbEnhancedClient enhancedClient;

  /** テーブルのモック。 */
  private DynamoDbTable<SES_AI_TENANT_MONTHLY_INGEST_COUNT> table;

  /** 拡張クライアントの静的メソッドのモック。テスト後に解除する。 */
  private MockedStatic<DynamoDbEnhancedClient> enhanced;

  /** 低レベルの DynamoDbClient のモック。 */
  private DynamoDbClient dynamoDbClient;

  /** DynamoDB の拡張クライアントと低レベルクライアントをモックに差し替える。 */
  @BeforeEach
  void DynamoDBのクライアントをモックに差し替える() {
    enhanced = mockStatic(DynamoDbEnhancedClient.class);
    DynamoDbEnhancedClient.Builder builder =
        mock(DynamoDbEnhancedClient.Builder.class, RETURNS_SELF);
    enhancedClient = mock(DynamoDbEnhancedClient.class);
    enhanced.when(DynamoDbEnhancedClient::builder).thenReturn(builder);
    when(builder.build()).thenReturn(enhancedClient);
    table = mock(DynamoDbTable.class);
    doReturn(table).when(enhancedClient).table(anyString(), any(TableSchema.class));
    dynamoDbClient = mock(DynamoDbClient.class);
    SES_AI_TENANT_MONTHLY_INGEST_COUNT.setDynamoDbClient(dynamoDbClient);
  }

  /** 拡張クライアントの静的メソッドと低レベルクライアントの差し替えを解除する。 */
  @AfterEach
  void 差し替えを解除する() {
    enhanced.close();
    SES_AI_TENANT_MONTHLY_INGEST_COUNT.setDynamoDbClient(null);
  }

  /**
   * 項目が揃った集計情報を生成する。テナントID、年月、チャネル種別は EMAIL とする。
   *
   * @return 生成した集計情報
   */
  private static SES_AI_TENANT_MONTHLY_INGEST_COUNT count() {
    SES_AI_TENANT_MONTHLY_INGEST_COUNT count = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
    count.setTenantId(TENANT_ID);
    count.setYearMonth(YEAR_MONTH);
    count.setChannelType(ChannelType.EMAIL);
    return count;
  }

  /**
   * 加算の更新要求を捕まえ、その要求を返す。
   *
   * @return 送信された更新要求
   * @throws Exception 呼び出しの確認中に例外が起きた場合
   */
  private UpdateItemRequest capturedUpdate() throws Exception {
    ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
    verify(dynamoDbClient).updateItem(captor.capture());
    return captor.getValue();
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テーブル名の設定がある場合に、その名前でテーブルを作成することを確かめる。 集計の保存先を設定値で切り替える。
       *
       * @given 設定値のテーブル名が "ingest-count-table"
       * @when SES_AI_TENANT_MONTHLY_INGEST_COUNT を生成する
       * @then テーブル名 "ingest-count-table" で作成したテーブルを返す
       */
      @TestProperty(
          key = "infrastructure/dynamodb/tenant-monthly-ingest-count-table/name",
          value = "ingest-count-table")
      @Test
      void テーブル名の設定がある場合_設定値のテーブルを返す() {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();

        verify(enhancedClient).table(eq("ingest-count-table"), any(TableSchema.class));
        assertThat(count.getTable()).isSameAs(table);
      }
    }

    @Nested
    class 境界値 {

      /**
       * テーブル名の設定が空白の場合に、既定のテーブル名でテーブルを作成することを確かめる。 空白の名前では DynamoDB に接続できない。
       *
       * @given 設定値のテーブル名が空白
       * @when SES_AI_TENANT_MONTHLY_INGEST_COUNT を生成する
       * @then 既定のテーブル名で作成したテーブルを返す
       */
      @TestProperty(
          key = "infrastructure/dynamodb/tenant-monthly-ingest-count-table/name",
          value = " ")
      @Test
      void テーブル名の設定が空白の場合_既定のテーブルを返す() {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();

        verify(enhancedClient)
            .table(eq("SES_AI_TENANT_MONTHLY_INGEST_COUNT"), any(TableSchema.class));
        assertThat(count.getTable()).isSameAs(table);
      }

      /**
       * テーブル名の設定が無い場合に、既定のテーブル名でテーブルを作成することを確かめる。 設定の無い環境でも集計を保存できるようにする。
       *
       * @given 設定値のテーブル名が無い
       * @when SES_AI_TENANT_MONTHLY_INGEST_COUNT を生成する
       * @then 既定のテーブル名で作成したテーブルを返す
       */
      @Test
      void テーブル名の設定が無い場合_既定のテーブルを返す() {
        Properties.removeForTest(SsmParameterKey.TENANT_MONTHLY_INGEST_COUNT_TABLE_NAME.getKey());
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();

        verify(enhancedClient)
            .table(eq("SES_AI_TENANT_MONTHLY_INGEST_COUNT"), any(TableSchema.class));
        assertThat(count.getTable()).isSameAs(table);
      }
    }
  }

  @Nested
  class setDynamoDbClient {

    @Nested
    class 正常系 {

      /**
       * 低レベルクライアントを設定すると、加算の更新要求をそのクライアントへ送信することを確かめる。 テストと差し替えのために、送信先を外から指定できるようにする。
       *
       * @given 低レベルクライアントのモックを設定済み、加算量が 1
       * @when increment を テナントID "T001"、チャネル EMAIL で実行する
       * @then そのクライアントへ更新要求を送信する
       */
      @Test
      void クライアントを設定した場合_そのクライアントへ更新要求を送信する() throws Exception {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(TENANT_ID, ChannelType.EMAIL);

        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
      }
    }
  }

  @Nested
  class getPartitionKey {

    @Nested
    class 正常系 {

      /**
       * テナントIDと年月を # で連結したパーティションキーを返すことを確かめる。 テナントごと・月ごとに集計を分ける。
       *
       * @given テナントIDが "T001"、年月が "202601"
       * @when getPartitionKey を実行する
       * @then "T001#202601" を返す
       */
      @Test
      void テナントIDと年月が揃っている場合_パーティションキーを返す() {
        assertThat(count().getPartitionKey()).isEqualTo("T001#202601");
      }
    }
  }

  @Nested
  class getSortKey {

    @Nested
    class 正常系 {

      /**
       * チャネル種別がある場合に、チャネル種別の値をソートキーとして返すことを確かめる。 チャネルごとに集計を分ける。
       *
       * @given チャネル種別が EMAIL
       * @when getSortKey を実行する
       * @then "EMAIL" を返す
       */
      @Test
      void チャネル種別がある場合_チャネル種別の値を返す() {
        assertThat(count().getSortKey()).isEqualTo("EMAIL");
      }
    }

    @Nested
    class 境界値 {

      /**
       * チャネル種別が null の場合に、ソートキーを null とすることを確かめる。 チャネルの無い集計は保存先を決められない。
       *
       * @given チャネル種別が null
       * @when getSortKey を実行する
       * @then nullを返す
       */
      @Test
      void チャネル種別がnullの場合_nullを返す() {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = count();
        count.setChannelType(null);

        assertThat(count.getSortKey()).isNull();
      }
    }
  }

  @Nested
  class save {

    @Nested
    class 正常系 {

      /**
       * 必須の項目が揃っている場合に、現在時刻を更新日時として登録することを確かめる。 集計の最終更新を追えるようにする。
       *
       * @given テナントIDが "T001"、年月が "202601"、チャネル種別が EMAIL、現在時刻が 2026-01-15 12:00 の日本時間
       * @when save を実行する
       * @then 更新日時に "2026-01-15T03:00:00Z" を設定し、1件を登録する
       */
      @Test
      void 必須の項目が揃っている場合_更新日時を現在時刻にして1件を登録する() {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = count();

        count.save();

        assertThat(count.getTimestamp()).isEqualTo("2026-01-15T03:00:00Z");
        verify(table).putItem(count);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須の項目のいずれかが未設定や空の場合に、登録しないことを確かめる。 キーの欠けた集計を保存すると、別の集計と区別できない。
       *
       * @given テナントID、年月、チャネル種別のいずれかが未設定または空
       * @when save を実行する
       * @then DynamoDB への登録を呼ばない
       */
      @ParameterizedTest
      @CsvSource({
        ", 202601, EMAIL",
        "'', 202601, EMAIL",
        "T001, , EMAIL",
        "T001, '', EMAIL",
        "T001, 202601, "
      })
      void 必須の項目のいずれかが未設定の場合_DynamoDBへの登録を呼ばない(
          final String tenantId, final String yearMonth, final ChannelType channelType) {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = count();
        count.setTenantId(tenantId);
        count.setYearMonth(yearMonth);
        count.setChannelType(channelType);

        count.save();

        verify(table, never()).putItem(any(SES_AI_TENANT_MONTHLY_INGEST_COUNT.class));
      }
    }
  }

  @Nested
  class delete {

    @Nested
    class 正常系 {

      /**
       * パーティションキーとソートキーを指定して、集計のレコードを削除することを確かめる。 キーが一致しないと、別の集計を消してしまう。
       *
       * @given テナントIDが "T001"、年月が "202601"、チャネル種別が EMAIL
       * @when delete を実行する
       * @then パーティションキーに "T001#202601"、ソートキーに "EMAIL" を指定してレコードを削除する
       */
      @Test
      void キーが揃っている場合_キーを指定してレコードを削除する() {
        ArgumentCaptor<Key> captor = ArgumentCaptor.forClass(Key.class);

        count().delete();

        verify(table).deleteItem(captor.capture());
        assertThat(captor.getValue().partitionKeyValue().s()).isEqualTo("T001#202601");
        assertThat(captor.getValue().sortKeyValue().orElseThrow().s()).isEqualTo("EMAIL");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須の項目のいずれかが未設定や空の場合に、削除しないことを確かめる。 キーの欠けた削除は別の集計を消すおそれがある。
       *
       * @given テナントID、年月、チャネル種別のいずれかが未設定または空
       * @when delete を実行する
       * @then DynamoDB の削除を呼ばない
       */
      @ParameterizedTest
      @CsvSource({
        ", 202601, EMAIL",
        "'', 202601, EMAIL",
        "T001, , EMAIL",
        "T001, '', EMAIL",
        "T001, 202601, "
      })
      void 必須の項目のいずれかが未設定の場合_DynamoDBの削除を呼ばない(
          final String tenantId, final String yearMonth, final ChannelType channelType) {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = count();
        count.setTenantId(tenantId);
        count.setYearMonth(yearMonth);
        count.setChannelType(channelType);

        count.delete();

        verify(table, never()).deleteItem(any(Key.class));
      }
    }
  }

  @Nested
  class fetch {

    @Nested
    class 正常系 {

      /**
       * 最新のレコードがある場合に、取得した件数と更新日時で自身を更新することを確かめる。 画面に表示する件数を保存済みの値に合わせる。
       *
       * @given 最新のレコードの件数が 7、更新日時が "2026-01-16T00:00:00Z"
       * @when fetch を実行する
       * @then 件数を 7 に、更新日時を "2026-01-16T00:00:00Z" に更新する
       */
      @Test
      void 最新のレコードがある場合_件数と更新日時を更新する() {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT latest = count();
        latest.setCount(7L);
        latest.setTimestamp("2026-01-16T00:00:00Z");
        doReturn(latest).when(table).getItem(any(Consumer.class));
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = count();
        count.setCount(0L);

        count.fetch();

        assertThat(count.getCount()).isEqualTo(7L);
        assertThat(count.getTimestamp()).isEqualTo("2026-01-16T00:00:00Z");
      }

      /**
       * 取得要求のキーをテナントと年月とチャネルから組み立て、整合性のある読み込みを指定することを確かめる。 最新の値を取り違えないためである。
       *
       * @given テナントID "T001"、年月 "202601"、チャネル EMAIL のレコード
       * @when fetch を実行する
       * @then 取得要求のキーに "T001#202601" と "EMAIL" を設定し、整合性のある読み込みにする
       */
      @Test
      void 取得要求の場合_テナントと年月とチャネルのキーで取得要求を送信する() {
        GetItemEnhancedRequest[] requested = new GetItemEnhancedRequest[1];
        doAnswer(
                invocation -> {
                  Consumer<GetItemEnhancedRequest.Builder> consumer = invocation.getArgument(0);
                  GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
                  consumer.accept(builder);
                  requested[0] = builder.build();
                  return null;
                })
            .when(table)
            .getItem(any(Consumer.class));

        count().fetch();

        assertThat(requested[0].key().partitionKeyValue().s()).isEqualTo("T001#202601");
        assertThat(requested[0].key().sortKeyValue().orElseThrow().s()).isEqualTo("EMAIL");
        assertThat(requested[0].consistentRead()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 最新のレコードが無い場合に項目を変えないことを確かめる。 対象の集計が無いときに、入力中の件数を消さない。
       *
       * @given 最新のレコードが null、件数が 3
       * @when fetch を実行する
       * @then 件数を 3 のまま何もしない
       */
      @Test
      void 最新のレコードが無い場合_何もしない() {
        doReturn(null).when(table).getItem(any(Consumer.class));
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = count();
        count.setCount(3L);

        count.fetch();

        assertThat(count.getCount()).isEqualTo(3L);
      }

      /**
       * 必須の項目のいずれかが未設定や空の場合に、取得しないことを確かめる。 キーの欠けた取得は別の集計を返すおそれがある。
       *
       * @given テナントID、年月、チャネル種別のいずれかが未設定または空
       * @when fetch を実行する
       * @then DynamoDB からの取得を呼ばない
       */
      @ParameterizedTest
      @CsvSource({
        ", 202601, EMAIL",
        "'', 202601, EMAIL",
        "T001, , EMAIL",
        "T001, '', EMAIL",
        "T001, 202601, "
      })
      void 必須の項目のいずれかが未設定の場合_DynamoDBからの取得を呼ばない(
          final String tenantId, final String yearMonth, final ChannelType channelType) {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT count = count();
        count.setTenantId(tenantId);
        count.setYearMonth(yearMonth);
        count.setChannelType(channelType);

        count.fetch();

        verify(table, never()).getItem(any(Consumer.class));
      }
    }
  }

  @Nested
  class increment_String_ChannelType {

    @Nested
    class 正常系 {

      /**
       * 現在年月のカウントを1件分加算する更新要求を送信することを確かめる。 受信1件ごとに件数を増やす。
       *
       * @given テナントIDが "T001"、チャネル種別が EMAIL、現在時刻が 2026-01-15 12:00 の日本時間
       * @when increment を テナントID と チャネル種別で実行する
       * @then 年月 "202601" の EMAIL のカウントに 1 を加算する更新要求を送信する
       */
      @Test
      void テナントIDとチャネルを指定した場合_1件分の加算要求を送信する() throws Exception {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(TENANT_ID, ChannelType.EMAIL);

        UpdateItemRequest request = capturedUpdate();
        assertThat(request.key().get("partitionKey").s()).isEqualTo("T001#202601");
        assertThat(request.key().get("sortKey").s()).isEqualTo("EMAIL");
        assertThat(request.expressionAttributeValues().get(":inc").n()).isEqualTo("1");
      }

      /**
       * クライアントが未設定の場合に、生成したクライアントへ加算要求を送信することを確かめる。 設定の無い環境でも加算を行えるようにする。
       *
       * @given クライアントが未設定、生成されるクライアントがモック
       * @when increment をテナントID "T001"、チャネル EMAIL で実行する
       * @then クライアントを生成し、更新要求を1件送信する
       */
      @Test
      void クライアントが未設定の場合_生成したクライアントへ加算要求を送信する() throws Exception {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.setDynamoDbClient(null);
        try (MockedStatic<DynamoDbClientFactory> factory =
            mockStatic(DynamoDbClientFactory.class)) {
          factory.when(DynamoDbClientFactory::create).thenReturn(dynamoDbClient);

          SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(TENANT_ID, ChannelType.EMAIL);

          factory.verify(DynamoDbClientFactory::create);
        }
        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
      }
    }
  }

  @Nested
  class increment_String_ChannelType_long {

    @Nested
    class 正常系 {

      /**
       * 現在年月のカウントを指定量だけ加算する更新要求を送信することを確かめる。 一括で受信した件数を一度に加算する。
       *
       * @given テナントIDが "T001"、チャネル種別が EMAIL、加算量が 5
       * @when increment を テナントID、チャネル種別、加算量で実行する
       * @then 加算量 5 を指定した更新要求を送信する
       */
      @Test
      void テナントIDとチャネルと加算量を指定した場合_加算量を指定した要求を送信する() throws Exception {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(TENANT_ID, ChannelType.EMAIL, 5L);

        assertThat(capturedUpdate().expressionAttributeValues().get(":inc").n()).isEqualTo("5");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 低レベルクライアントの更新が失敗した場合に、その例外を呼び出し元へ伝えることを確かめる。 受信件数の加算に失敗したことを見逃さない。
       *
       * @given 更新要求の送信が IllegalStateException を投げる
       * @when increment を テナントID "T001"、チャネル EMAIL、加算量 1 で実行する
       * @then IllegalStateExceptionを投げる
       */
      @Test
      void 更新が失敗した場合_IllegalStateExceptionを投げる() {
        doThrow(new IllegalStateException("接続失敗"))
            .when(dynamoDbClient)
            .updateItem(any(UpdateItemRequest.class));

        assertThatThrownBy(
                () ->
                    SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(TENANT_ID, ChannelType.EMAIL, 1L))
            .isInstanceOf(IllegalStateException.class);
      }
    }
  }

  @Nested
  class increment_String_String_ChannelType_long {

    @Nested
    class 正常系 {

      /**
       * 指定の年月と加算量で、カウントの更新要求を送信することを確かめる。 月をまたぐ集計を指定の年月へ入れる。
       *
       * @given テナントIDが "T001"、年月が "202512"、チャネル種別が LINE、加算量が 3
       * @when increment を年月、チャネル種別、加算量を指定して実行する
       * @then 年月 "202512" の LINE のカウントに 3 を加算する更新要求を送信する
       */
      @Test
      void 年月とチャネルと加算量を指定した場合_指定した年月の加算要求を送信する() throws Exception {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(TENANT_ID, "202512", ChannelType.LINE, 3L);

        UpdateItemRequest request = capturedUpdate();
        assertThat(request.key().get("partitionKey").s()).isEqualTo("T001#202512");
        assertThat(request.key().get("sortKey").s()).isEqualTo("LINE");
        assertThat(request.expressionAttributeValues().get(":inc").n()).isEqualTo("3");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 引数のいずれかが無効な場合に、更新要求を送信しないことを確かめる。 無効な引数で更新すると、別の集計の件数を書き換えるおそれがある。
       *
       * @given テナントID、年月、チャネル種別、加算量のいずれかが無効未設定、空白、加算量が 0 以下
       * @when increment を無効な引数で実行する
       * @then 更新要求を送信しない
       */
      @ParameterizedTest
      @CsvSource({
        ", 202601, EMAIL, 1",
        "'  ', 202601, EMAIL, 1",
        "T001, , EMAIL, 1",
        "T001, '  ', EMAIL, 1",
        "T001, 202601, , 1",
        "T001, 202601, EMAIL, 0",
        "T001, 202601, EMAIL, -1"
      })
      void 引数のいずれかが無効な場合_更新を呼ばない(
          final String tenantId,
          final String yearMonth,
          final ChannelType channelType,
          final long amount) {
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(tenantId, yearMonth, channelType, amount);

        verify(dynamoDbClient, never()).updateItem(any(UpdateItemRequest.class));
      }
    }
  }
}
