package copel.sesproductpackage.core.database.base;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** {@link DynamoDb} の単体テスト。 */
public class DynamoDbTest {

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

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テーブル名を指定すると、そのテーブルの参照を持つことを確かめる。 保存と取得は、このテーブルの参照を通して行うためである。
       *
       * @given テーブル名が TEST_TABLE
       * @when TestRecord を TEST_TABLE のテーブル名で生成する
       * @then TEST_TABLE のテーブルを返す
       */
      @Test
      void テーブル名がTEST_TABLEの場合_TEST_TABLEのテーブルを返す() {
        TestRecord record = new TestRecord("TEST_TABLE");

        assertThat(record.table.tableName()).isEqualTo("TEST_TABLE");
      }
    }
  }

  @Nested
  class getTimestamp {

    @Nested
    class 正常系 {

      /**
       * タイムスタンプを設定していれば、その値を返すことを確かめる。 最終更新日時を呼び出し元へ返すためである。
       *
       * @given タイムスタンプが 2026-01-15T10:00:00+09:00
       * @when getTimestamp を実行する
       * @then 2026-01-15T10:00:00+09:00 を返す
       */
      @Test
      void タイムスタンプが設定済みの場合_設定したタイムスタンプを返す() {
        TestRecord record = new TestRecord("TEST_TABLE");
        record.setTimestamp("2026-01-15T10:00:00+09:00");

        assertThat(record.getTimestamp()).isEqualTo("2026-01-15T10:00:00+09:00");
      }
    }

    @Nested
    class 境界値 {

      /**
       * タイムスタンプが未設定の場合に null を返すことを確かめる。 未設定の行を保存していない状態と区別できるように、そのまま null を返す。
       *
       * @given タイムスタンプが未設定
       * @when getTimestamp を実行する
       * @then null を返す
       */
      @Test
      void タイムスタンプが未設定の場合_nullを返す() {
        assertThat(new TestRecord("TEST_TABLE").getTimestamp()).isNull();
      }
    }
  }

  @Nested
  class toString {

    @Nested
    class 正常系 {

      /**
       * 項目を設定した状態で、項目を含むJSON形式の文字列を返すことを確かめる。 ログや画面表示で中身を確かめられるよう、テーブルの参照は含めない。
       *
       * @given パーティションキーが P001、項目の名前が山田
       * @when toString を実行する
       * @then パーティションキーと名前を含むJSON形式の文字列を返し、テーブルの参照を含まない
       */
      @Test
      void 項目が設定済みの場合_項目を含むJSON形式の文字列を返す() {
        TestRecord record = new TestRecord("TEST_TABLE");
        record.setPartitionKey("P001");
        record.setName("山田");

        String json = record.toString();

        assertThat(json)
            .contains("\"partitionKey\" : \"P001\"")
            .contains("\"name\" : \"山田\"")
            .doesNotContain("\"table\"");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 項目を取得できない場合に空のJSONを返すことを確かめる。 表示のための文字列化で例外を外へ出すと、画面やログの出力が止まるためである。
       *
       * @given 取得に失敗する項目を持つ
       * @when toString を実行する
       * @then 空のJSONオブジェクトを返す
       */
      @Test
      void 取得に失敗する項目がある場合_空のJSONを返す() {
        TestRecord record = new TestRecord("TEST_TABLE");
        record.setBroken(true);

        assertThat(record.toString()).isEqualTo("{}");
      }
    }
  }
}
