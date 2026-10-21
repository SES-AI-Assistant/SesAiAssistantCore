package copel.sesproductpackage.core.database.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

/** {@link DynamoDbLot} の単体テスト。 */
public class DynamoDbLotTest {

  /** テスト用の {@link DynamoDb} の具象クラス。 項目の値を持ち、取得時に例外を投げる項目を切り替えられる。 */
  @DynamoDbBean
  public static class TestRecord extends DynamoDb<TestRecord> {

    /** 項目の名前。 */
    private String name;

    /** 真の場合に {@link #getBrokenValue()} が例外を投げる。 */
    private boolean broken;

    /**
     * テーブル名を指定して生成する。
     *
     * @param tableName テーブル名
     */
    TestRecord(final String tableName) {
      super(tableName, TestRecord.class);
    }

    /** 既定のテーブル名で生成する。テーブルのスキーマを作るために、引数なしの生成子が必要なためである。 */
    public TestRecord() {
      this("TEST_TABLE");
    }

    /**
     * パーティションキーを返す。テーブルのスキーマに使うため注釈を付ける。
     *
     * @return パーティションキー
     */
    @Override
    @DynamoDbPartitionKey
    public String getPartitionKey() {
      return super.getPartitionKey();
    }

    /**
     * 項目の名前を返す。
     *
     * @return 項目の名前
     */
    public String getName() {
      return name;
    }

    /**
     * 項目の名前を設定する。
     *
     * @param name 項目の名前
     */
    public void setName(final String name) {
      this.name = name;
    }

    /**
     * 取得時に例外を投げるかを返す。
     *
     * @return 真の場合は例外を投げる
     */
    public boolean isBroken() {
      return broken;
    }

    /**
     * 取得時に例外を投げるかを設定する。
     *
     * @param broken 真の場合は例外を投げる
     */
    public void setBroken(final boolean broken) {
      this.broken = broken;
    }

    /**
     * 例外を投げる項目の値を返す。
     *
     * @return 項目の値
     * @throws IllegalStateException 項目を取得できない状態の場合
     */
    public String getBrokenValue() {
      if (broken) {
        throw new IllegalStateException("項目を取得できない");
      }
      return "値";
    }

    @Override
    public void save() {
      // テストでは使わない
    }

    @Override
    public void delete() {
      // テストでは使わない
    }

    @Override
    public void fetch() {
      // テストでは使わない
    }
  }

  /**
   * DynamoDB の拡張クライアントのビルダーを差し替え、テーブルのモックを返すように設定する。 AWS へ接続せずに、テーブルへの問い合わせと結果を確かめるためである。
   *
   * @param enhanced 差し替え先の静的メソッドのモック
   * @return 拡張クライアントのモック
   */
  @SuppressWarnings("unchecked")
  private static DynamoDbEnhancedClient stubEnhancedClient(
      final MockedStatic<DynamoDbEnhancedClient> enhanced) {
    DynamoDbEnhancedClient.Builder builder =
        mock(DynamoDbEnhancedClient.Builder.class, RETURNS_SELF);
    DynamoDbEnhancedClient client = mock(DynamoDbEnhancedClient.class);
    enhanced.when(DynamoDbEnhancedClient::builder).thenReturn(builder);
    when(builder.build()).thenReturn(client);
    return client;
  }

  /**
   * 指定したテーブル名のテーブルのモックを、拡張クライアントが返すように設定する。
   *
   * @param client 拡張クライアントのモック
   * @return テーブルのモック
   */
  @SuppressWarnings("unchecked")
  private static DynamoDbTable<TestRecord> stubTable(final DynamoDbEnhancedClient client) {
    DynamoDbTable<TestRecord> table = mock(DynamoDbTable.class);
    doReturn(table).when(client).table(anyString(), any(TableSchema.class));
    return table;
  }

  /**
   * 1件のレコードを返すページのモックを作り、テーブルの問い合わせに設定する。
   *
   * @param table テーブルのモック
   * @param records 問い合わせの結果
   */
  @SuppressWarnings("unchecked")
  private static void stubPage(
      final DynamoDbTable<TestRecord> table, final List<TestRecord> records) {
    PageIterable<TestRecord> pages = mock(PageIterable.class);
    SdkIterable<TestRecord> items = records::iterator;
    when(pages.items()).thenReturn(items);
    doReturn(pages).when(table).query(any(QueryConditional.class));
    doReturn(pages).when(table).scan(any(ScanEnhancedRequest.class));
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テーブル名を指定すると、拡張クライアントからそのテーブルを作ることを確かめる。 問い合わせは、このテーブルの参照を通して行うためである。
       *
       * @given 拡張クライアントがテーブルのモックを返す
       * @when DynamoDbLot を TEST_TABLE のテーブル名で生成する
       * @then TEST_TABLE のテーブルのモックを返す
       */
      @Test
      void テーブル名がTEST_TABLEの場合_作成したテーブルを返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          DynamoDbEnhancedClient client = stubEnhancedClient(enhanced);
          DynamoDbTable<TestRecord> table = stubTable(client);

          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);

          verify(client).table(eq("TEST_TABLE"), any(TableSchema.class));
          assertThat(lot.table).isSameAs(table);
        }
      }
    }
  }

  @Nested
  class toString {

    @Nested
    class 正常系 {

      /**
       * レコードが1件ある場合に、そのレコードを含むJSON配列を返すことを確かめる。 検索結果の中身を確かめられるようにするためである。
       *
       * @given 検索結果に項目の名前が山田、パーティションキーが P001 のレコードが1件ある
       * @when toString を実行する
       * @then パーティションキーと名前を含むJSON配列を返す
       */
      @Test
      void レコードが1件の場合_レコードを含むJSON配列を返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          stubEnhancedClient(enhanced);
          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);
          TestRecord record = new TestRecord("TEST_TABLE");
          record.setPartitionKey("P001");
          record.setName("山田");
          lot.entityLot.add(record);

          String json = lot.toString();

          assertThat(json).contains("\"partitionKey\" : \"P001\"").contains("\"name\" : \"山田\"");
        }
      }

      /**
       * レコードが0件の場合に空の配列を返すことを確かめる。 0件の結果を表示しても例外にならないようにするためである。
       *
       * @given 検索結果が空
       * @when toString を実行する
       * @then 空の配列の表記を返す
       */
      @Test
      void レコードが0件の場合_空の配列を返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          stubEnhancedClient(enhanced);
          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);

          assertThat(lot.toString()).isEqualTo("[ ]");
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * シリアライズできないレコードがある場合に、空の配列を返すことを確かめる。 表示のための文字列化で例外を外へ出すと、画面やログの出力が止まるためである。
       *
       * @given 検索結果に取得に失敗する項目を持つレコードが1件ある
       * @when toString を実行する
       * @then 空の配列を返す
       */
      @Test
      void シリアライズできないレコードがある場合_空の配列を返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          stubEnhancedClient(enhanced);
          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);
          TestRecord record = new TestRecord("TEST_TABLE");
          record.setBroken(true);
          lot.entityLot.add(record);

          assertThat(lot.toString()).isEqualTo("[]");
        }
      }
    }
  }

  @Nested
  class fetchByPk {

    @Nested
    class 正常系 {

      /**
       * パーティションキーで問い合わせた結果を1件保持することを確かめる。 同じキーのレコードを画面やバッチで扱えるようにするためである。
       *
       * @given テーブルの問い合わせが項目の名前 山田 のレコードを1件返す
       * @when fetchByPk をパーティションキー P001 で実行する
       * @then 問い合わせを1回行い、1件のレコードを返す
       */
      @Test
      void パーティションキーがP001の場合_1件のレコードを返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          DynamoDbTable<TestRecord> table = stubTable(stubEnhancedClient(enhanced));
          TestRecord record = new TestRecord("TEST_TABLE");
          record.setName("山田");
          stubPage(table, List.of(record));
          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);

          lot.fetchByPk("P001");

          verify(table).query(any(QueryConditional.class));
          assertThat(lot.entityLot).extracting(TestRecord::getName).containsExactly("山田");
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 以前の結果がある場合に、以前の結果を消してから新しい結果を保持することを確かめる。 前の検索結果が混ざると、別のキーのレコードを扱ってしまうためである。
       *
       * @given 以前の結果に項目の名前 古い のレコードが1件あり、問い合わせが 新しい のレコードを1件返す
       * @when fetchByPk をパーティションキー P001 で実行する
       * @then 以前の結果を消して、新しい 1件のレコードを返す
       */
      @Test
      void 以前の結果がある場合_以前の結果を消して1件のレコードを返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          DynamoDbTable<TestRecord> table = stubTable(stubEnhancedClient(enhanced));
          TestRecord fresh = new TestRecord("TEST_TABLE");
          fresh.setName("新しい");
          stubPage(table, List.of(fresh));
          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);
          TestRecord old = new TestRecord("TEST_TABLE");
          old.setName("古い");
          lot.entityLot.add(old);

          lot.fetchByPk("P001");

          assertThat(lot.entityLot).extracting(TestRecord::getName).containsExactly("新しい");
        }
      }
    }
  }

  @Nested
  class fetchByColumn {

    @Nested
    class 正常系 {

      /**
       * 列名と値で絞り込んだ結果を保持することを確かめる。 年月等の条件に一致するレコードだけを扱うためである。
       *
       * @given 列名が usageMonth、値が 2026-01 で、問い合わせが項目の名前 山田 のレコードを1件返す
       * @when fetchByColumn を列名 usageMonth と値 2026-01 で実行する
       * @then 列名と値を条件にしたスキャンを行い、1件のレコードを返す
       */
      @Test
      void 列名がusageMonthの場合_一致する1件のレコードを返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          DynamoDbTable<TestRecord> table = stubTable(stubEnhancedClient(enhanced));
          TestRecord record = new TestRecord("TEST_TABLE");
          record.setName("山田");
          stubPage(table, List.of(record));
          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);

          lot.fetchByColumn("usageMonth", "2026-01");

          ArgumentCaptor<ScanEnhancedRequest> request =
              ArgumentCaptor.forClass(ScanEnhancedRequest.class);
          verify(table).scan(request.capture());
          Expression filter = request.getValue().filterExpression();
          assertThat(filter.expression()).isEqualTo("#col = :val");
          assertThat(filter.expressionNames()).containsEntry("#col", "usageMonth");
          assertThat(filter.expressionValues().get(":val").s()).isEqualTo("2026-01");
          assertThat(lot.entityLot).extracting(TestRecord::getName).containsExactly("山田");
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 一致するレコードが無い場合に空のリストを保持することを確かめる。 0件の検索結果を、そのまま空の結果として扱うためである。
       *
       * @given 問い合わせが0件を返す
       * @when fetchByColumn を列名 usageMonth と値 2026-02 で実行する
       * @then 空のリストを返す
       */
      @Test
      void 一致するレコードが無い場合_空のリストを返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          DynamoDbTable<TestRecord> table = stubTable(stubEnhancedClient(enhanced));
          stubPage(table, new ArrayList<>());
          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);

          lot.fetchByColumn("usageMonth", "2026-02");

          assertThat(lot.entityLot).isEmpty();
        }
      }
    }
  }

  @Nested
  class iterator {

    @Nested
    class 正常系 {

      /**
       * 保持しているレコードを追加した順に取り出せることを確かめる。 呼び出し元の for 文で、検索結果を順に処理できるようにするためである。
       *
       * @given 検索結果にレコードが2件、項目の名前が 山田 と 佐藤 の順である
       * @when iterator で全件を取り出す
       * @then 山田 と 佐藤 の順に2件を返す
       */
      @Test
      void レコードが2件の場合_追加した順に2件を返す() {
        try (MockedStatic<DynamoDbEnhancedClient> enhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {
          stubEnhancedClient(enhanced);
          DynamoDbLot<TestRecord> lot = new DynamoDbLot<>("TEST_TABLE", TestRecord.class);
          TestRecord first = new TestRecord("TEST_TABLE");
          first.setName("山田");
          TestRecord second = new TestRecord("TEST_TABLE");
          second.setName("佐藤");
          lot.entityLot.add(first);
          lot.entityLot.add(second);

          List<String> names = new ArrayList<>();
          lot.iterator().forEachRemaining(record -> names.add(record.getName()));

          assertThat(names).containsExactly("山田", "佐藤");
        }
      }
    }
  }
}
