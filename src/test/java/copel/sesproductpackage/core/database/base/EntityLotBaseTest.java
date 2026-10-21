package copel.sesproductpackage.core.database.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.LogicalOperators.LogicalOperatorType;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** {@link EntityLotBase} の単体テスト。 */
class EntityLotBaseTest {

  /** テスト用のテーブルから全件を取得するSQL。WHERE 句を含まない。 */
  private static final String SELECT_ALL_SQL = "SELECT id, name, tenant_id FROM TEST_TABLE";

  /** テスト用の全文検索のSQL。 */
  private static final String SELECT_LIKE_SQL =
      "SELECT id, name, tenant_id FROM TEST_TABLE WHERE name LIKE ?";

  /** テナントIDを使う全テスト共通の値。 */
  private static final String TENANT_ID = "T001";

  /** ログを受け取るアペンダー。ログを確かめるテストだけで付けて外す。 */
  private ListAppender<ILoggingEvent> logAppender;

  /** ログのレベルを変えたテストで、後始末のために元に戻すロガー。 */
  private Logger lotLogger;

  /** ログのアペンダーを外し、ロガーのレベルを元に戻す。他のテストへの影響を残さないため毎回戻す。 */
  @AfterEach
  void ログのアペンダーとレベルを戻す() {
    if (lotLogger != null) {
      if (logAppender != null) {
        lotLogger.detachAppender(logAppender);
      }
      lotLogger.setLevel(null);
    }
    logAppender = null;
    lotLogger = null;
  }

  /** テスト用の {@link EntityBase} の具象クラス。 テナントID と ID と名前を持ち、DB 操作の抽象メソッドは何もしない。 */
  private static class TestEntity extends EntityBase {

    /** 項目の ID。 */
    private final String id;

    /** 項目の名前。 */
    private final String name;

    /**
     * テナントIDと ID と名前を指定して生成する。
     *
     * @param tenantId テナントID
     * @param id 項目の ID
     * @param name 項目の名前
     */
    TestEntity(final String tenantId, final String id, final String name) {
      super(tenantId);
      this.id = id;
      this.name = name;
    }

    /**
     * 項目の ID を返す。
     *
     * @return 項目の ID
     */
    String getId() {
      return id;
    }

    /**
     * 項目の名前を返す。
     *
     * @return 項目の名前
     */
    String getName() {
      return name;
    }

    @Override
    public int insert(final Connection connection) {
      return 0;
    }

    @Override
    public void selectByPk(final Connection connection) {
      // テストでは使わない
    }

    @Override
    public boolean updateByPk(final Connection connection) {
      return false;
    }

    @Override
    public boolean deleteByPk(final Connection connection) {
      return false;
    }
  }

  /** テスト用の {@link EntityLotBase} の具象クラス。 検索用のSQLを定義し、ResultSet を {@link TestEntity} に変換する。 */
  private static class TestLot extends EntityLotBase<TestEntity> {

    @Override
    public void selectAll(final Connection connection, final String tenantId) throws SQLException {
      this.entityLot =
          new ArrayList<>(
              executeQuery(
                  connection,
                  SELECT_ALL_SQL,
                  tenantId,
                  this::mapResultSet,
                  (stmt, index) -> index));
    }

    @Override
    protected String getSelectAllSql() {
      return SELECT_ALL_SQL;
    }

    @Override
    protected String getSelectLikeSql() {
      return SELECT_LIKE_SQL;
    }

    @Override
    protected TestEntity mapResultSet(final ResultSet resultSet) throws SQLException {
      return new TestEntity(
          resultSet.getString("tenant_id"), resultSet.getString("id"), resultSet.getString("name"));
    }
  }

  /** 検索用のSQLを定義しないテスト用の {@link EntityLotBase} の具象クラス。 既定の検索用SQLを確かめるためだけに使う。 */
  private static class PlainLot extends EntityLotBase<TestEntity> {

    @Override
    public void selectAll(final Connection connection, final String tenantId) {
      // テストでは使わない
    }

    @Override
    protected String getSelectAllSql() {
      return SELECT_ALL_SQL;
    }

    @Override
    protected TestEntity mapResultSet(final ResultSet resultSet) {
      return null;
    }
  }

  /**
   * 指定した SQL の一部を含む準備文が、指定の結果を返す接続のモックを用意する。 件数の取得と一覧の取得のように、1つのテストで複数の SQL を流すときに使う。
   *
   * @param connection 接続のモック
   * @param sqlPart SQL に含まれる文字列
   * @param resultSet 問い合わせの結果
   * @return 準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static PreparedStatement stubStatement(
      final Connection connection, final String sqlPart, final ResultSet resultSet)
      throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(connection.prepareStatement(contains(sqlPart))).thenReturn(statement);
    return statement;
  }

  /**
   * 任意の SQL に対して同じ結果を返す準備文のモックを用意する。
   *
   * @param connection 接続のモック
   * @param resultSet 問い合わせの結果
   * @return 準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static PreparedStatement stubAnyStatement(
      final Connection connection, final ResultSet resultSet) throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    return statement;
  }

  /**
   * 件数を1列目に持つ1行の結果を作る。
   *
   * @param count 件数
   * @return 件数の結果
   * @throws SQLException 作成中に例外が起きた場合
   */
  private static ResultSet countResult(final long count) throws SQLException {
    return MockResultSetBuilder.builder().columns("count").row(count).build();
  }

  /**
   * 項目の結果を作る。2件を返す。
   *
   * @return 項目の結果
   * @throws SQLException 作成中に例外が起きた場合
   */
  private static ResultSet twoRowsResult() throws SQLException {
    return MockResultSetBuilder.builder()
        .columns("id", "name", "tenant_id")
        .row("U001", "山田", TENANT_ID)
        .row("U002", "佐藤", TENANT_ID)
        .build();
  }

  /**
   * 0件の項目の結果を作る。
   *
   * @return 0件の結果
   * @throws SQLException 作成中に例外が起きた場合
   */
  private static ResultSet emptyResult() throws SQLException {
    return MockResultSetBuilder.builder().columns("id", "name", "tenant_id").build();
  }

  /**
   * ログを受け取るアペンダーを付けて、ロガーのレベルを指定する。
   *
   * @param level 設定するレベル
   */
  private void attachLogAppender(final Level level) {
    lotLogger = (Logger) LoggerFactory.getLogger(EntityLotBase.class);
    lotLogger.setLevel(level);
    logAppender = new ListAppender<>();
    logAppender.start();
    lotLogger.addAppender(logAppender);
  }

  @Nested
  class toCountSql {

    @Nested
    class 正常系 {

      /**
       * SELECT 句を {@code COUNT(*)} に置き換えたSQLを返すことを確かめる。 件数の取得は一覧と同じ条件で数えるため、条件の部分は残す。
       *
       * @given 基本SQLが SELECT 句と WHERE 句を持つ
       * @when toCountSql を基本SQLで実行する
       * @then {@code COUNT(*)} と WHERE 句を持つSQLを返す
       */
      @Test
      void WHERE句のあるSQLの場合_COUNTのSQLを返す() {
        assertThat(EntityLotBase.toCountSql("SELECT id, name FROM TEST_TABLE WHERE id = ?"))
            .isEqualTo("SELECT COUNT(*) FROM TEST_TABLE WHERE id = ?");
      }

      /**
       * ORDER BY 句を件数の取得から取り除くことを確かめる。 件数の取得では並び順が不要で、PostgreSQL の GROUP BY の検証に影響するためである。
       *
       * @given 基本SQLが ORDER BY 句を持つ
       * @when toCountSql を基本SQLで実行する
       * @then ORDER BY 句を除いたSQLを返す
       */
      @Test
      void ORDER句を含む場合_ORDER句を除いたSQLを返す() {
        assertThat(EntityLotBase.toCountSql("SELECT id FROM TEST_TABLE WHERE id = ? ORDER BY id"))
            .isEqualTo("SELECT COUNT(*) FROM TEST_TABLE WHERE id = ?");
      }

      /**
       * 列名に from を含んでも FROM 句の直前までを置き換えることを確かめる。 列名の from_group 等を FROM 句と誤認すると、件数のSQLが壊れるためである。
       *
       * @given 基本SQLの列名が from_group と from_id
       * @when toCountSql を基本SQLで実行する
       * @then FROM 句の前の列だけを {@code COUNT(*)} に置き換えたSQLを返す
       */
      @Test
      void 列名にfromを含む場合_FROM句の前だけをCOUNTに置き換えたSQLを返す() {
        assertThat(EntityLotBase.toCountSql("SELECT from_group, from_id FROM TEST_TABLE"))
            .isEqualTo("SELECT COUNT(*) FROM TEST_TABLE");
      }

      /**
       * GROUP BY 句を件数の取得から取り除くことを確かめる。 件数の取得では集約の単位が不要なためである。
       *
       * @given 基本SQLが GROUP BY 句を持つ
       * @when toCountSql を基本SQLで実行する
       * @then GROUP BY 句を除いたSQLを返す
       */
      @Test
      void GROUP句を含む場合_GROUP句を除いたSQLを返す() {
        assertThat(EntityLotBase.toCountSql("SELECT id FROM TEST_TABLE GROUP BY id"))
            .isEqualTo("SELECT COUNT(*) FROM TEST_TABLE");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 基本SQLが null の場合に null を返すことを確かめる。 件数のSQLが作れないことを呼び出し元へそのまま返すためである。
       *
       * @given 基本SQLが null
       * @when toCountSql を基本SQL null で実行する
       * @then null を返す
       */
      @Test
      void 基本SQLがnullの場合_nullを返す() {
        assertThat(EntityLotBase.toCountSql(null)).isNull();
      }
    }
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 生成直後のLotが要素を持たないことを確かめる。 検索の結果を入れる前の状態で、件数が0であることを保証するためである。
       *
       * @given 引数なし
       * @when TestLot を生成する
       * @then 件数が0のLotを返す
       */
      @Test
      void 引数なしの場合_件数が0のLotを返す() {
        assertThat(new TestLot().size()).isZero();
      }
    }
  }

  @Nested
  class add {

    @Nested
    class 正常系 {

      /**
       * 追加したエンティティが先頭として取り出せることを確かめる。 画面に返す順番は追加した順番になるためである。
       *
       * @given 空のLot
       * @when エンティティ U001 を add で追加する
       * @then 追加したエンティティを先頭として返す
       */
      @Test
      void エンティティを1件追加した場合_追加したエンティティを返す() {
        TestLot lot = new TestLot();
        TestEntity entity = new TestEntity(TENANT_ID, "U001", "山田");

        lot.add(entity);

        assertThat(lot.get(0)).isSameAs(entity);
      }
    }
  }

  @Nested
  class get {

    @Nested
    class 正常系 {

      /**
       * 範囲内の位置を指定すると、その位置のエンティティを返すことを確かめる。 一覧の任意の行を取り出すためである。
       *
       * @given 2件のLotで、位置1にエンティティ U002 がある
       * @when get を位置 1 で実行する
       * @then U002 のエンティティを返す
       */
      @Test
      void 位置が1の場合_2件目のエンティティを返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));
        TestEntity second = new TestEntity(TENANT_ID, "U002", "佐藤");
        lot.add(second);

        assertThat(lot.get(1)).isSameAs(second);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 件数と同じ位置を指定すると null を返すことを確かめる。 範囲外の位置を例外にせず null で示すためである。
       *
       * @given 1件のLot
       * @when get を位置 1 で実行する
       * @then null を返す
       */
      @Test
      void 位置が件数と同じ場合_nullを返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));

        assertThat(lot.get(1)).isNull();
      }

      /**
       * 負の位置を指定すると null を返すことを確かめる。 負の位置は要素が無いものとして扱うためである。
       *
       * @given 1件のLot
       * @when get を位置 -1 で実行する
       * @then null を返す
       */
      @Test
      void 位置が負の場合_nullを返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));

        assertThat(lot.get(-1)).isNull();
      }
    }
  }

  @Nested
  class size {

    @Nested
    class 正常系 {

      /**
       * 追加した件数を返すことを確かめる。 0件の判定や件数の表示に使うためである。
       *
       * @given 2件のLot
       * @when size を実行する
       * @then 2を返す
       */
      @Test
      void 件数が2件の場合_2を返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));
        lot.add(new TestEntity(TENANT_ID, "U002", "佐藤"));

        assertThat(lot.size()).isEqualTo(2);
      }
    }
  }

  @Nested
  class isEmpty {

    @Nested
    class 境界値 {

      /**
       * 要素が無い場合に true を返すことを確かめる。 0件の結果を空として扱うためである。
       *
       * @given 空のLot
       * @when isEmpty を実行する
       * @then trueを返す
       */
      @Test
      void 要素が無い場合_trueを返す() {
        assertThat(new TestLot().isEmpty()).isTrue();
      }

      /**
       * 要素がある場合に false を返すことを確かめる。 1件でもあれば空ではないと判定するためである。
       *
       * @given 1件のLot
       * @when isEmpty を実行する
       * @then falseを返す
       */
      @Test
      void 要素が1件ある場合_falseを返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));

        assertThat(lot.isEmpty()).isFalse();
      }
    }
  }

  @Nested
  class iterator {

    @Nested
    class 正常系 {

      /**
       * 追加した順にエンティティを取り出せることを確かめる。 呼び出し元の for 文で、一覧を順に処理できるようにするためである。
       *
       * @given 2件のLot で追加した順が U001 と U002
       * @when iterator で全件を取り出す
       * @then U001 と U002 の順に2件を返す
       */
      @Test
      void 要素が2件の場合_追加した順に2件を返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));
        lot.add(new TestEntity(TENANT_ID, "U002", "佐藤"));

        Iterator<TestEntity> iterator = lot.iterator();
        List<String> ids = new ArrayList<>();
        iterator.forEachRemaining(entity -> ids.add(entity.getId()));

        assertThat(ids).containsExactly("U001", "U002");
      }
    }
  }

  @Nested
  class sort {

    @Nested
    class 正常系 {

      /**
       * 登録日時の逆順で追加したエンティティが、登録日時の昇順に並ぶことを確かめる。 一覧の既定の並び順を登録日時の古い順にするためである。
       *
       * @given 登録日時 2026年1月16日の U002 を先に、登録日時 2026年1月15日の U001 を後に追加する
       * @when sort を実行する
       * @then U001 と U002 の順に2件を返す
       */
      @Test
      void 登録日時が逆順の場合_昇順の2件を返す() {
        TestLot lot = new TestLot();
        lot.add(withRegisterDate(new TestEntity(TENANT_ID, "U002", "佐藤"), 16));
        lot.add(withRegisterDate(new TestEntity(TENANT_ID, "U001", "山田"), 15));

        lot.sort();

        List<String> ids = new ArrayList<>();
        lot.iterator().forEachRemaining(entity -> ids.add(entity.getId()));
        assertThat(ids).containsExactly("U001", "U002");
      }

      /**
       * 登録日時を設定したエンティティを返すことを確かめる。 並び替えのテストで、登録日時の差だけを用意するためである。
       *
       * @param entity 対象のエンティティ
       * @param day 2026年1月の日
       * @return 登録日時を設定したエンティティ
       */
      private static TestEntity withRegisterDate(final TestEntity entity, final int day) {
        entity.registerDate = new OriginalDateTime(2026, 1, day, 0, 0, 0);
        return entity;
      }
    }
  }

  @Nested
  class removeIf {

    @Nested
    class 正常系 {

      /**
       * 述語に一致する要素を削除し、削除したことを true で返すことを確かめる。 不要な行を一覧から外すためである。
       *
       * @given U001 と U002 の2件のLot、述語は ID が U001 の要素を選ぶ
       * @when removeIf を述語で実行する
       * @then 一致した要素を削除し、残りの1件を返す
       */
      @Test
      void 述語に一致する要素がある場合_一致した要素を削除しtrueを返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));
        lot.add(new TestEntity(TENANT_ID, "U002", "佐藤"));

        boolean removed = lot.removeIf(entity -> "U001".equals(entity.getId()));

        assertThat(removed).isTrue();
        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 述語に一致する要素が無い場合に false を返すことを確かめる。 何も削除されなかったことを呼び出し元が判定できるようにするためである。
       *
       * @given U001 の1件のLot、述語は ID が U009 の要素を選ぶ
       * @when removeIf を述語で実行する
       * @then falseを返す
       */
      @Test
      void 述語に一致する要素が無い場合_falseを返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));

        assertThat(lot.removeIf(entity -> "U009".equals(entity.getId()))).isFalse();
      }
    }
  }

  @Nested
  class getTotalCount {

    @Nested
    class 境界値 {

      /**
       * 件数を取得していない状態では0を返すことを確かめる。 検索を実行する前の一覧が、件数0として扱われるためである。
       *
       * @given 検索を実行していない空のLot
       * @when getTotalCount を実行する
       * @then 0を返す
       */
      @Test
      void 検索を実行していない場合_0を返す() {
        assertThat(new TestLot().getTotalCount()).isZero();
      }
    }
  }

  @Nested
  class getPageSize {

    @Nested
    class 境界値 {

      /**
       * 検索を実行していない状態では0を返すことを確かめる。 ページの件数が未設定であることを示すためである。
       *
       * @given 検索を実行していない空のLot
       * @when getPageSize を実行する
       * @then 0を返す
       */
      @Test
      void 検索を実行していない場合_0を返す() {
        assertThat(new TestLot().getPageSize()).isZero();
      }
    }
  }

  @Nested
  class getCurrentPageIndex {

    @Nested
    class 境界値 {

      /**
       * 検索を実行していない状態では0を返すことを確かめる。 現在のページが未設定であることを示すためである。
       *
       * @given 検索を実行していない空のLot
       * @when getCurrentPageIndex を実行する
       * @then 0を返す
       */
      @Test
      void 検索を実行していない場合_0を返す() {
        assertThat(new TestLot().getCurrentPageIndex()).isZero();
      }
    }
  }

  @Nested
  class getTotalPages {

    @Nested
    class 正常系 {

      /**
       * 件数を1ページあたりの件数で割り上げた数を返すことを確かめる。 端数のページも1ページとして数えるためである。
       *
       * @given 全件数が 25、1ページあたりの件数が 10
       * @when getTotalPages を実行する
       * @then 3を返す
       */
      @Test
      void 件数が25で1ページ10件の場合_3を返す() {
        TestLot lot = new TestLot();
        lot.totalCount = 25;
        lot.pageSize = 10;

        assertThat(lot.getTotalPages()).isEqualTo(3);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 1ページあたりの件数が0の場合に1を返すことを確かめる。 割り算の0除算を避け、少なくとも1ページとして扱うためである。
       *
       * @given 全件数が 25、1ページあたりの件数が 0
       * @when getTotalPages を実行する
       * @then 1を返す
       */
      @Test
      void ページ件数が0の場合_1を返す() {
        TestLot lot = new TestLot();
        lot.totalCount = 25;
        lot.pageSize = 0;

        assertThat(lot.getTotalPages()).isEqualTo(1);
      }

      /**
       * 全件数が0の場合に0を返すことを確かめる。 0件の検索結果ではページを表示しないためである。
       *
       * @given 全件数が 0、1ページあたりの件数が 10
       * @when getTotalPages を実行する
       * @then 0を返す
       */
      @Test
      void 件数が0の場合_0を返す() {
        TestLot lot = new TestLot();
        lot.totalCount = 0;
        lot.pageSize = 10;

        assertThat(lot.getTotalPages()).isZero();
      }
    }
  }

  @Nested
  class selectAllPaged {

    @Nested
    class 正常系 {

      /**
       * 全件を1ページ目として取得し、件数と一覧を保持することを確かめる。 一覧画面の初期表示で、件数とページ情報をそろえるためである。
       *
       * @given 件数の問い合わせが 2 を返し、一覧の問い合わせが U001 と U002 を返す
       * @when selectAllPaged をテナントID T001 で、1ページ目、1ページ10件で実行する
       * @then 件数に 2、ページ情報に 10 と 1 を保持し、2件を返す
       */
      @Test
      void 件数が2件の場合_1ページ目の2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(connection, "COUNT(*)", countResult(2));
        stubStatement(connection, "LIMIT", twoRowsResult());
        TestLot lot = new TestLot();

        lot.selectAllPaged(connection, TENANT_ID, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(2);
        assertThat(lot.getPageSize()).isEqualTo(10);
        assertThat(lot.getCurrentPageIndex()).isEqualTo(1);
        assertThat(lot.size()).isEqualTo(2);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 件数が0件の場合に一覧の問い合わせを行わず、空のLotを返すことを確かめる。 0件の一覧で余分な問い合わせを行わないためである。
       *
       * @given 件数の問い合わせが 0 を返す
       * @when selectAllPaged をテナントID T001 で実行する
       * @then 問い合わせを1回だけ行い、空のLotを返す
       */
      @Test
      void 件数が0件の場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = stubStatement(connection, "COUNT(*)", countResult(0));
        TestLot lot = new TestLot();

        lot.selectAllPaged(connection, TENANT_ID, 1, 10);

        verify(connection, times(1)).prepareStatement(anyString());
        verify(statement).setString(1, TENANT_ID);
        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 件数と一覧の両方に指定したテナントIDを設定することを確かめる。 他テナントの件数や行を混ぜないためである。
       *
       * @given 件数の問い合わせが 2 を返し、一覧の問い合わせが 2件を返す
       * @when selectAllPaged をテナントID T001 で実行する
       * @then 件数と一覧の両方の1番目の値に T001 を設定し、2件を返す
       */
      @Test
      void テナントIDがT001の場合_T001の2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubStatement(connection, "COUNT(*)", countResult(2));
        PreparedStatement pageStatement = stubStatement(connection, "LIMIT", twoRowsResult());
        TestLot lot = new TestLot();

        lot.selectAllPaged(connection, TENANT_ID, 1, 10);

        verify(countStatement).setString(1, TENANT_ID);
        verify(pageStatement).setString(1, TENANT_ID);
        assertThat(lot.size()).isEqualTo(2);
      }
    }
  }

  @Nested
  class selectByLikeQuery {

    @Nested
    class 正常系 {

      /**
       * 条件を連ねたSQLで検索し、null の条件を飛ばして検索語を順に設定することを確かめる。 条件の一部が未設定でも検索を続けられるようにする。
       *
       * @given 検索語が 山、条件が AND の x、null、OR の y
       * @when selectByLikeQuery を接続、テナントID "T001"、列名 name で実行する
       * @then 条件を連ねたSQLを準備し、検索語を 1 から 3 番目に設定する
       */
      @Test
      void 条件が2件でnullを含む場合_条件を連ねたSQLを送信する() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement =
            stubStatement(
                connection,
                " OR name LIKE ?",
                MockResultSetBuilder.builder().columns("id", "name", "tenant_id").build());
        List<LogicalOperators> query =
            Arrays.asList(
                new LogicalOperators(LogicalOperatorType.AND, "x"),
                null,
                new LogicalOperators(LogicalOperatorType.OR, "y"));

        new TestLot().selectByLikeQuery(connection, TENANT_ID, SELECT_LIKE_SQL, "name", "山", query);

        verify(statement).setString(1, "%山%");
        verify(statement).setString(2, "%x%");
        verify(statement).setString(3, "%y%");
      }

      /**
       * 条件が null の場合に、検索語だけで検索することを確かめる。 追加の条件が無い検索も行えるようにする。
       *
       * @given 検索語が 山、条件が null
       * @when selectByLikeQuery を接続、テナントID "T001"、列名 name、条件 null で実行する
       * @then 検索語を 1 番目だけに設定する
       */
      @Test
      void 条件がnullの場合_検索語だけのSQLを送信する() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement =
            stubStatement(
                connection,
                "WHERE name LIKE ?",
                MockResultSetBuilder.builder().columns("id", "name", "tenant_id").build());

        new TestLot().selectByLikeQuery(connection, TENANT_ID, SELECT_LIKE_SQL, "name", "山", null);

        verify(statement).setString(1, "%山%");
        verify(statement).setString(2, TENANT_ID);
      }

      /**
       * 条件が空の場合に、検索語だけで検索することを確かめる。 条件の並びが無い検索で余分な値を設定しない。
       *
       * @given 検索語が 山、条件が空のリスト
       * @when selectByLikeQuery を接続、テナントID "T001"、列名 name、空の条件で実行する
       * @then 検索語を 1 番目だけに設定する
       */
      @Test
      void 条件が空の場合_検索語だけのSQLを送信する() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement =
            stubStatement(
                connection,
                "WHERE name LIKE ?",
                MockResultSetBuilder.builder().columns("id", "name", "tenant_id").build());

        new TestLot()
            .selectByLikeQuery(connection, TENANT_ID, SELECT_LIKE_SQL, "name", "山", List.of());

        verify(statement).setString(1, "%山%");
        verify(statement).setString(2, TENANT_ID);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 接続が無い状態では検索できないためである。
       *
       * @given 接続が null、検索語が 山
       * @when selectByLikeQuery を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        TestLot lot = new TestLot();
        assertThatThrownBy(
                () -> {
                  lot.selectByLikeQuery(null, TENANT_ID, SELECT_LIKE_SQL, "name", "山", null);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectByQueryPaged {

    @Nested
    class 正常系 {

      /**
       * 条件が無い場合に、テナントIDの条件だけのSQLで件数と1ページ目を取得することを確かめる。 条件を指定しない一覧でも、テナントIDで絞り込むためである。
       *
       * @given 件数の問い合わせが 2 を返し、一覧の問い合わせが 2件を返す
       * @when selectByQueryPaged を条件 null、1ページ目、1ページ10件で実行する
       * @then 件数と一覧のSQLにテナントIDの条件だけを付け、2件を返す
       */
      @Test
      void 条件がnullの場合_テナントIDの条件だけのSQLで2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(connection, "COUNT(*)", countResult(2));
        PreparedStatement pageStatement = stubStatement(connection, "LIMIT", twoRowsResult());
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection, TENANT_ID, "SELECT id, name, tenant_id FROM TEST_TABLE", null, true, 1, 10);

        verify(connection)
            .prepareStatement(
                "SELECT id, name, tenant_id FROM TEST_TABLE WHERE tenant_id = ? LIMIT ? OFFSET ?");
        verify(pageStatement).setString(1, TENANT_ID);
        verify(pageStatement).setInt(2, 10);
        verify(pageStatement).setInt(3, 0);
        assertThat(lot.size()).isEqualTo(2);
      }

      /**
       * 複数の条件を AND で連ね、条件の値とテナントIDを順にバインドすることを確かめる。 条件の値の順番が SQL の位置と一致しないと、別の条件で検索されるためである。
       *
       * @given 条件が name と id の2件、2ページ目、1ページ10件
       * @when selectByQueryPaged を AND 条件で実行する
       * @then name と id を AND で連ね、テナントIDの後に LIMIT と OFFSET の20を設定して2件を返す
       */
      @Test
      void AND条件の場合_条件を連ねたSQLで2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(2));
        Map<String, String> query = new LinkedHashMap<>();
        query.put("name", "山田");
        query.put("id", "U001");
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE",
            query,
            true,
            2,
            10);

        verify(connection)
            .prepareStatement(
                "SELECT id, name, tenant_id FROM TEST_TABLE WHERE name = ? AND id = ? AND tenant_id = ? LIMIT ? OFFSET ?");
        assertThat(lot.getCurrentPageIndex()).isEqualTo(2);
      }

      /**
       * 基本SQLが ORDER BY を持つ場合に、条件を ORDER BY の前に挿入したSQLを作ることを確かめる。 ORDER BY の後ろに条件を置くと、SQL
       * の構文が壊れるためである。
       *
       * @given 基本SQLが WHERE 句と ORDER BY 句を持ち、条件が id の1件
       * @when selectByQueryPaged を条件つきで実行する
       * @then 条件を ORDER BY の前に挿入したSQLを準備する
       */
      @Test
      void ORDER句を含む場合_条件をORDER句の前に挿入したSQLを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(1));
        Map<String, String> query = new LinkedHashMap<>();
        query.put("id", "U001");
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE x = ? ORDER BY id",
            query,
            true,
            1,
            10);

        verify(connection)
            .prepareStatement(
                "SELECT id FROM TEST_TABLE WHERE x = ? AND id = ? AND tenant_id = ? ORDER BY id LIMIT ? OFFSET ?");
      }

      /**
       * 基本SQLが GROUP BY と HAVING を持つ場合に、条件を GROUP BY の前に挿入することを確かめる。 集約の後ろに条件を置くと、集約の対象が変わるためである。
       *
       * @given 基本SQLが GROUP BY と HAVING を持ち、条件が id の1件
       * @when selectByQueryPaged を条件つきで実行する
       * @then 条件を GROUP BY の前に挿入したSQLを準備する
       */
      @Test
      void GROUP句とHAVING句を含む場合_条件をGROUP句の前に挿入したSQLを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(1));
        Map<String, String> query = new LinkedHashMap<>();
        query.put("id", "U001");
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE x = ? GROUP BY id HAVING count(*) > 0",
            query,
            true,
            1,
            10);

        verify(connection)
            .prepareStatement(
                "SELECT id FROM TEST_TABLE WHERE x = ? AND id = ? AND tenant_id = ? GROUP BY id HAVING count(*) > 0 LIMIT ? OFFSET ?");
      }

      /**
       * 2ページ目を指定すると、OFFSET に件数分の値を設定することを確かめる。 次のページの行を飛ばすためである。
       *
       * @given 件数の問い合わせが 25 を返し、基本SQLが WHERE 句を持つ
       * @when selectByQueryPaged をページ2、1ページ10件で実行する
       * @then LIMIT に 10、OFFSET に 10 を設定して一覧の問い合わせを行う
       */
      @Test
      void ページ2の場合_空の結果を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(connection, "COUNT(*)", countResult(25));
        PreparedStatement pageStatement = stubStatement(connection, "LIMIT", emptyResult());
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection, TENANT_ID, "SELECT id FROM TEST_TABLE", null, true, 2, 10);

        verify(pageStatement).setInt(2, 10);
        verify(pageStatement).setInt(3, 10);
      }

      /**
       * 条件が空の Map の場合に、テナントIDの条件だけで件数と一覧を取得することを確かめる。 空の条件でも一覧はテナントIDで絞り込むためである。
       *
       * @given 条件が空の Map、件数の問い合わせが 2 を返し、一覧の問い合わせが 2件を返す
       * @when selectByQueryPaged を1ページ目、1ページ10件で実行する
       * @then テナントIDの条件だけのSQLで件数と一覧を取得し、2件を返す
       */
      @Test
      void 条件が空の場合_テナントIDの条件だけのSQLで2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(connection, "COUNT(*)", countResult(2));
        stubStatement(connection, "LIMIT", twoRowsResult());
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE",
            new LinkedHashMap<>(),
            true,
            1,
            10);

        assertThat(lot.size()).isEqualTo(2);
      }

      /**
       * OR 条件では、条件を OR で連ねたSQLで件数と一覧を取得することを確かめる。 候補のどれかに当たる行を取得するためである。
       *
       * @given 条件が name と id の OR、件数の問い合わせが 2 を返し、一覧の問い合わせが 2件を返す
       * @when selectByQueryPaged を OR 条件で実行する
       * @then OR で連ねた条件とテナントIDの条件のSQLで準備し、2件を返す
       */
      @Test
      void OR条件の場合_OR全体を括弧で包んだSQLで2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(connection, "COUNT(*)", countResult(2));
        stubStatement(connection, "LIMIT", twoRowsResult());
        Map<String, String> query = new LinkedHashMap<>();
        query.put("name", "山田");
        query.put("id", "U001");
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE",
            query,
            false,
            1,
            10);

        verify(connection)
            .prepareStatement(
                "SELECT id, name, tenant_id FROM TEST_TABLE WHERE (name = ? OR id = ?) AND tenant_id = ? LIMIT ? OFFSET ?");
        assertThat(lot.size()).isEqualTo(2);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 件数が 0 件の場合に一覧の問い合わせを行わず、ページ情報だけを保持することを確かめる。 0件の一覧で余分な問い合わせを行わないためである。
       *
       * @given 件数の問い合わせが 0 を返す
       * @when selectByQueryPaged を1ページ目、1ページ10件で実行する
       * @then 一覧の問い合わせを行わず、ページ情報だけを保持して空のLotを返す
       */
      @Test
      void 件数が0件の場合_ページ情報だけを保持して空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection, TENANT_ID, "SELECT id FROM TEST_TABLE", null, true, 1, 10);

        verify(connection, times(1)).prepareStatement(anyString());
        assertThat(lot.getPageSize()).isEqualTo(10);
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 接続が null の場合に何もせず空のLotを返すことを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null
       * @when selectByQueryPaged を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        TestLot lot = new TestLot();
        assertThatThrownBy(
                () -> {
                  lot.selectByQueryPaged(
                      null, TENANT_ID, "SELECT id FROM TEST_TABLE", null, true, 1, 10);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 基本SQLが null の場合に何もせず空のLotを返すことを確かめる。 SQL が無いと件数も一覧も作れないためである。
       *
       * @given 基本SQLが null
       * @when selectByQueryPaged を基本SQL null で実行する
       * @then 空のLotを返す
       */
      @Test
      void 基本SQLがnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(connection, TENANT_ID, null, null, true, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 件数の問い合わせが行を返さない場合に、件数0のまま空の一覧を返すことを確かめる。 件数が取れないときは一覧を読み込まない。
       *
       * @given 件数の問い合わせの結果が 0 行
       * @when selectByQueryPaged を接続、テナントID "T001"、基本SQL、条件 null、1ページ目、1ページ10件で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @Test
      void 件数の結果が0行の場合_件数0の空の一覧を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(
            connection, "COUNT(*)", MockResultSetBuilder.builder().columns("count").build());
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(connection, TENANT_ID, SELECT_ALL_SQL, null, true, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0);
        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 件数と一覧のテナントIDの条件に、指定したテナントIDを設定することを確かめる。 他テナントの件数や行を混ぜないためである。
       *
       * @given 件数の問い合わせが 1 を返し、条件が id の1件
       * @when selectByQueryPaged をテナントID T001 で実行する
       * @then 件数の問い合わせの2番目の値に T001 を設定し、1件を返す
       */
      @Test
      void テナントIDがT001の場合_T001の条件で1件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubAnyStatement(connection, countResult(1));
        Map<String, String> query = new LinkedHashMap<>();
        query.put("id", "U001");
        TestLot lot = new TestLot();

        lot.selectByQueryPaged(
            connection, TENANT_ID, "SELECT id FROM TEST_TABLE", query, true, 1, 10);

        verify(countStatement, times(2)).setString(2, TENANT_ID);
        assertThat(lot.getTotalCount()).isEqualTo(1);
      }
    }
  }

  @Nested
  class countByQuery {

    @Nested
    class 正常系 {

      /**
       * 条件が無い場合に、テナントIDの条件だけで件数を返すことを確かめる。 件数は必ず自分のテナントの行だけを数えるためである。
       *
       * @given 件数の問い合わせが 3 を返す
       * @when countByQuery を条件 null で実行する
       * @then テナントIDの条件だけのSQLで準備し、3を返す
       */
      @Test
      void 条件がnullの場合_テナントIDの条件だけで3を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = stubAnyStatement(connection, countResult(3));
        TestLot lot = new TestLot();

        long count =
            lot.countByQuery(connection, TENANT_ID, "SELECT id FROM TEST_TABLE", null, true);

        verify(connection).prepareStatement("SELECT COUNT(*) FROM TEST_TABLE WHERE tenant_id = ?");
        verify(statement).setString(1, TENANT_ID);
        assertThat(count).isEqualTo(3);
      }

      /**
       * 基本SQLが WHERE 句を持つ場合に、条件を AND で連ねて件数を返すことを確かめる。 既存の絞り込みを残したまま、条件を加えるためである。
       *
       * @given 基本SQLが WHERE 句を持ち、条件が name の1件、件数の問い合わせが 2 を返す
       * @when countByQuery を AND 条件で実行する
       * @then name の条件とテナントIDの条件を AND で連ねて準備し、2を返す
       */
      @Test
      void WHERE句がある場合_AND条件で連ねて2を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(2));
        Map<String, String> query = new LinkedHashMap<>();
        query.put("name", "山田");

        long count =
            new TestLot()
                .countByQuery(
                    connection, TENANT_ID, "SELECT id FROM TEST_TABLE WHERE x = ?", query, true);

        verify(connection)
            .prepareStatement(
                "SELECT COUNT(*) FROM TEST_TABLE WHERE x = ? AND name = ? AND tenant_id = ?");
        assertThat(count).isEqualTo(2);
      }

      /**
       * OR 条件では、条件を OR で連ねて件数を返すことを確かめる。 候補のどれかに当たる行を数えるためである。
       *
       * @given 条件が name と id の2件、件数の問い合わせが 1 を返す
       * @when countByQuery を OR 条件で実行する
       * @then name と id を OR で連ねて準備し、1を返す
       */
      @Test
      void OR条件の場合_OR全体を括弧で包んで1を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(1));
        Map<String, String> query = new LinkedHashMap<>();
        query.put("name", "山田");
        query.put("id", "U001");

        long count =
            new TestLot()
                .countByQuery(connection, TENANT_ID, "SELECT id FROM TEST_TABLE", query, false);

        verify(connection)
            .prepareStatement(
                "SELECT COUNT(*) FROM TEST_TABLE WHERE (name = ? OR id = ?) AND tenant_id = ?");
        assertThat(count).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 結果の行が無い場合に 0 を返すことを確かめる。 件数の行が無い状態を0件として扱うためである。
       *
       * @given 件数の問い合わせが行を返さない
       * @when countByQuery を条件 null で実行する
       * @then 0を返す
       */
      @Test
      void 結果の行が無い場合_0を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, emptyResult());

        assertThat(
                new TestLot()
                    .countByQuery(connection, TENANT_ID, "SELECT id FROM TEST_TABLE", null, true))
            .isZero();
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では件数を数えられないためである。
       *
       * @given 接続が null
       * @when countByQuery を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestLot()
                      .countByQuery(null, TENANT_ID, "SELECT id FROM TEST_TABLE", null, true);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 基本SQLが null の場合に 0 を返すことを確かめる。 SQL が無いと件数を数えられないためである。
       *
       * @given 基本SQLが null
       * @when countByQuery を基本SQL null で実行する
       * @then 0を返す
       */
      @Test
      void 基本SQLがnullの場合_0を返す() throws SQLException {
        assertThat(new TestLot().countByQuery(mock(Connection.class), TENANT_ID, null, null, true))
            .isZero();
      }

      /**
       * 条件が空の Map の場合に、テナントIDの条件だけで件数を返すことを確かめる。 空の条件でも件数はテナントIDで絞り込むためである。
       *
       * @given 条件が空の Map と件数の問い合わせが 3 を返す
       * @when countByQuery を条件が空で実行する
       * @then テナントIDの条件だけのSQLで準備し、3を返す
       */
      @Test
      void 条件が空の場合_3を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(3));

        long count =
            new TestLot()
                .countByQuery(
                    connection,
                    TENANT_ID,
                    "SELECT id FROM TEST_TABLE",
                    new LinkedHashMap<>(),
                    true);

        verify(connection).prepareStatement("SELECT COUNT(*) FROM TEST_TABLE WHERE tenant_id = ?");
        assertThat(count).isEqualTo(3);
      }
    }
  }

  @Nested
  class bindParameter {

    @Nested
    class 正常系 {

      /**
       * BigDecimal の値を数値としてSQLに送信することを確かめる。 価格の比較を数値で行うためである。
       *
       * @given 値が BigDecimal の 1.5
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に数値 1.5 を送信する
       */
      @Test
      void BigDecimalの場合_数値を送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        new TestLot().bindParameter(statement, 1, new BigDecimal("1.5"));

        verify(statement).setBigDecimal(1, new BigDecimal("1.5"));
      }

      /**
       * Timestamp の値を日時としてSQLに送信することを確かめる。 開始日の比較を日時で行うためである。
       *
       * @given 値が Timestamp の 2026-01-15 00:00:00
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に同じ Timestamp を送信する
       */
      @Test
      void Timestampの場合_日時を送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        Timestamp timestamp = Timestamp.valueOf("2026-01-15 00:00:00");

        new TestLot().bindParameter(statement, 1, timestamp);

        verify(statement).setTimestamp(1, timestamp);
      }

      /**
       * OriginalDateTime の値を Timestamp に変換してSQLに送信することを確かめる。 独自の日時の型は JDBC で直接扱えないためである。
       *
       * @given 値が OriginalDateTime の 2026年1月15日
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に変換した Timestamp を送信する
       */
      @Test
      void OriginalDateTimeの場合_変換したTimestampを送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        OriginalDateTime dateTime = new OriginalDateTime(2026, 1, 15, 0, 0, 0);

        new TestLot().bindParameter(statement, 1, dateTime);

        verify(statement).setTimestamp(1, dateTime.toTimestamp());
      }

      /**
       * Enum の値を名前の文字列としてSQLに送信することを確かめる。 列挙値はDBに名前で保存されているためである。
       *
       * @given 値が Enum の TestArea.東京
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に 東京 を文字列として送信する
       */
      @Test
      void Enumの場合_名前の文字列を送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        new TestLot().bindParameter(statement, 1, TestArea.東京);

        verify(statement).setString(1, "東京");
      }

      /**
       * Integer の値を整数としてSQLに送信することを確かめる。 件数の比較を整数で行うためである。
       *
       * @given 値が Integer の 7
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に整数 7 を送信する
       */
      @Test
      void Integerの場合_整数を送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        new TestLot().bindParameter(statement, 1, 7);

        verify(statement).setInt(1, 7);
      }

      /**
       * String の値を文字列としてSQLに送信することを確かめる。 検索語を文字列の条件として比較するためである。
       *
       * @given 値が String の 山田
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に 山田 を文字列として送信する
       */
      @Test
      void Stringの場合_文字列を送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        new TestLot().bindParameter(statement, 1, "山田");

        verify(statement).setString(1, "山田");
      }

      /**
       * Double の値を浮動小数点数としてSQLに送信することを確かめる。 類似度の比較を数値で行うためである。
       *
       * @given 値が Double の 0.8
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に 0.8 を送信する
       */
      @Test
      void Doubleの場合_浮動小数点数を送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        new TestLot().bindParameter(statement, 1, 0.8d);

        verify(statement).setDouble(1, 0.8d);
      }

      /**
       * Long の値を長整数としてSQLに送信することを確かめる。 件数や ID を長整数の条件として比較するためである。
       *
       * @given 値が Long の 9000000000
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に長整数 9000000000 を送信する
       */
      @Test
      void Longの場合_長整数を送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        new TestLot().bindParameter(statement, 1, 9_000_000_000L);

        verify(statement).setLong(1, 9_000_000_000L);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対応しない型の値をオブジェクトのままSQLに送信することを確かめる。 真偽値等はドライバに型の判断を任せるためである。
       *
       * @given 値が Boolean の true
       * @when bindParameter を位置 1 で実行する
       * @then 位置 1 に true をオブジェクトとして送信する
       */
      @Test
      void その他の型の場合_オブジェクトを送信する() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        new TestLot().bindParameter(statement, 1, Boolean.TRUE);

        verify(statement).setObject(1, Boolean.TRUE);
      }
    }
  }

  /** テスト用の列挙値。 */
  private enum TestArea {
    /** 東京。 */
    東京,
    /** 大阪。 */
    大阪
  }

  @Nested
  class executeQuery {

    @Nested
    class 正常系 {

      /**
       * 結果の各行をエンティティに変換して返すことを確かめる。 検索の行を一覧として呼び出し元へ渡すためである。
       *
       * @given SQL が 2 つの値を持ち、結果が 2件
       * @when executeQuery をテナントID T001 で実行し、値を1番目と2番目に設定する
       * @then テナントIDを3番目に設定し、2件のエンティティを返す
       */
      @Test
      void 値が2つの場合_テナントIDを3番目に設定して2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = stubAnyStatement(connection, twoRowsResult());

        List<TestEntity> result =
            new TestLot()
                .executeQuery(
                    connection,
                    "SELECT id, name, tenant_id FROM TEST_TABLE WHERE id = ? AND name = ?",
                    TENANT_ID,
                    new TestLot()::mapResultSet,
                    (stmt, index) -> {
                      stmt.setString(index, "U001");
                      stmt.setString(index + 1, "山田");
                      return index + 2;
                    });

        verify(statement).setString(3, TENANT_ID);
        assertThat(result).extracting(TestEntity::getName).containsExactly("山田", "佐藤");
      }

      /**
       * ORDER BY を含むSQLでは、テナントIDの条件を ORDER BY の前に挿入することを確かめる。 並び替えの句の後ろに条件を置くと、SQL の構文が壊れるためである。
       *
       * @given 基本SQLが ORDER BY 句を持ち、結果が 2件
       * @when executeQuery をテナントID T001 で実行する
       * @then テナントIDの条件を ORDER BY の前に挿入したSQLを準備し、2件を返す
       */
      @Test
      void ORDER句を含む場合_ORDER句の前にテナントIDの条件を挿入したSQLを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, twoRowsResult());

        new TestLot()
            .executeQuery(
                connection,
                "SELECT id, name, tenant_id FROM TEST_TABLE ORDER BY id",
                TENANT_ID,
                new TestLot()::mapResultSet,
                (stmt, index) -> index);

        verify(connection)
            .prepareStatement(
                "SELECT id, name, tenant_id FROM TEST_TABLE WHERE tenant_id = ? ORDER BY id");
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に例外を投げることを確かめる。 テナントIDの無い問い合わせは全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが null
       * @when executeQuery をテナントID null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        Connection connection = mock(Connection.class);

        assertThatThrownBy(
                () ->
                    new TestLot()
                        .executeQuery(
                            connection,
                            SELECT_ALL_SQL,
                            null,
                            new TestLot()::mapResultSet,
                            (stmt, index) -> index))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null
       * @when executeQuery を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        assertThatThrownBy(
                () -> {
                  new TestLot()
                      .executeQuery(
                          null,
                          SELECT_ALL_SQL,
                          TENANT_ID,
                          new TestLot()::mapResultSet,
                          (stmt, index) -> index);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class executeQueryWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントIDの条件を付けずに問い合わせて、結果を返すことを確かめる。 バッチ等全テナントを対象にする処理で、SQL を変えずに実行するためである。
       *
       * @given 結果が 2件
       * @when executeQueryWithoutTenantFilter を値を設定せずに実行する
       * @then 元のSQLで準備し、2件を返す
       */
      @Test
      void 全テナント対象の場合_元のSQLで2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, twoRowsResult());

        List<TestEntity> result =
            new TestLot()
                .executeQueryWithoutTenantFilter(
                    connection,
                    SELECT_ALL_SQL,
                    new TestLot()::mapResultSet,
                    (stmt, index) -> index);

        verify(connection).prepareStatement(SELECT_ALL_SQL);
        assertThat(result).hasSize(2);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null
       * @when executeQueryWithoutTenantFilter を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        assertThatThrownBy(
                () -> {
                  new TestLot()
                      .executeQueryWithoutTenantFilter(
                          null,
                          SELECT_ALL_SQL,
                          new TestLot()::mapResultSet,
                          (stmt, index) -> index);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class executeDeleteWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントIDの条件を付けずに削除し、削除件数を返すことを確かめる。 マスタの削除は全テナント共通の行を対象にするため、SQL を変えない。
       *
       * @given 削除の件数が 2 件
       * @when executeDeleteWithoutTenantFilter を実行する
       * @then 元のSQLで準備し、2を返す
       */
      @Test
      void 全テナント対象の場合_元のSQLで削除して2を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(statement.executeUpdate()).thenReturn(2);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        int deleted =
            new TestLot()
                .executeDeleteWithoutTenantFilter(
                    connection, "DELETE FROM TEST_TABLE WHERE x = ?", (stmt, index) -> index);

        verify(connection).prepareStatement("DELETE FROM TEST_TABLE WHERE x = ?");
        assertThat(deleted).isEqualTo(2);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では削除できないためである。
       *
       * @given 接続が null
       * @when executeDeleteWithoutTenantFilter を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestLot()
                      .executeDeleteWithoutTenantFilter(
                          null, "DELETE FROM TEST_TABLE", (stmt, index) -> index);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  /**
   * ベクトルの値と類似度を、開始位置から順にバインドする。
   *
   * @param stmt バインド対象の準備文
   * @param start 開始位置
   * @param vector ベクトルの値
   * @param threshold 類似度
   * @return 次の位置
   * @throws SQLException バインド中に例外が起きた場合
   */
  private static int bindVector(
      final PreparedStatement stmt, final int start, final String vector, final double threshold)
      throws SQLException {
    stmt.setString(start, vector);
    stmt.setDouble(start + 1, threshold);
    return start + 2;
  }

  @Nested
  class executeVectorPagedQuery {

    @Nested
    class 正常系 {

      /**
       * 件数を件数用の値で、一覧を一覧用の値で取得し、ページ情報を保持することを確かめる。 画面のページ送りに件数と現在のページを使うためである。
       *
       * @given 件数の問い合わせが 2 を返し、一覧の問い合わせが 2件を返す
       * @when executeVectorPagedQuery を類似度 0.8 、1ページ目、1ページ10件で実行する
       * @then 件数に 2 を保持し、ページサイズに 10 を保持し、2件を返す
       */
      @Test
      void 件数が2件の場合_2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubStatement(connection, "COUNT(*)", countResult(2));
        PreparedStatement pageStatement = stubStatement(connection, "LIMIT", twoRowsResult());
        TestLot lot = new TestLot();

        lot.executeVectorPagedQuery(
            connection,
            "SELECT id, name, tenant_id FROM TEST_TABLE WHERE vector_data <=> ?::vector < ? ORDER BY id",
            TENANT_ID,
            "[0.1]",
            0.8,
            (stmt, start, vector, threshold) -> bindVector(stmt, start, vector, threshold),
            1,
            10,
            lot::mapResultSet,
            (stmt, start, vector, threshold) -> bindVector(stmt, start, vector, threshold));

        verify(countStatement).setString(1, "[0.1]");
        verify(countStatement).setDouble(2, 0.8);
        verify(countStatement).setString(3, TENANT_ID);
        verify(pageStatement).setString(1, "[0.1]");
        verify(pageStatement).setDouble(2, 0.8);
        verify(pageStatement).setString(3, TENANT_ID);
        verify(pageStatement).setInt(4, 10);
        verify(pageStatement).setInt(5, 0);
        assertThat(lot.getTotalCount()).isEqualTo(2);
        assertThat(lot.getPageSize()).isEqualTo(10);
        assertThat(lot.getCurrentPageIndex()).isEqualTo(1);
        assertThat(lot.size()).isEqualTo(2);
      }

      /**
       * 2ページ目の一覧は、1ページあたりの件数分をずらした OFFSET で取得することを確かめる。 ページ送りの位置が1ページ目とずれるとデータが重複または欠落するためである。
       *
       * @given 件数の問い合わせが 25 を返し、一覧の問い合わせが 2件を返す
       * @when executeVectorPagedQuery を類似度 0.8 、2ページ目、1ページ10件で実行する
       * @then OFFSET に 10 を設定し、2件を返す
       */
      @Test
      void 二ページ目の場合_OFFSETに10を設定して2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(connection, "COUNT(*)", countResult(25));
        PreparedStatement pageStatement = stubStatement(connection, "LIMIT", twoRowsResult());
        TestLot lot = new TestLot();

        lot.executeVectorPagedQuery(
            connection,
            "SELECT id, name, tenant_id FROM TEST_TABLE WHERE vector_data <=> ?::vector < ? ORDER BY id",
            TENANT_ID,
            "[0.1]",
            0.8,
            (stmt, start, vector, threshold) -> bindVector(stmt, start, vector, threshold),
            2,
            10,
            lot::mapResultSet,
            (stmt, start, vector, threshold) -> bindVector(stmt, start, vector, threshold));

        verify(pageStatement).setInt(5, 10);
        assertThat(lot.getCurrentPageIndex()).isEqualTo(2);
        assertThat(lot.size()).isEqualTo(2);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無いまま件数0と空の結果を返すと、検索結果が無いと誤認されるためである。
       *
       * @given 接続が null
       * @when executeVectorPagedQuery を接続 null で1ページ10件として実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        TestLot lot = new TestLot();

        assertThatThrownBy(
                () ->
                    lot.executeVectorPagedQuery(
                        null,
                        "SELECT id, name, tenant_id FROM TEST_TABLE WHERE vector_data <=> ?::vector < ? ORDER BY id",
                        TENANT_ID,
                        "[0.1]",
                        0.8,
                        (stmt, start, vector, threshold) ->
                            bindVector(stmt, start, vector, threshold),
                        1,
                        10,
                        lot::mapResultSet,
                        (stmt, start, vector, threshold) ->
                            bindVector(stmt, start, vector, threshold)))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが null の場合に例外を投げることを確かめる。 テナントIDの無い検索は全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが null で、接続が有効
       * @when executeVectorPagedQuery をテナントID null で1ページ10件として実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        assertThatThrownBy(
                () ->
                    lot.executeVectorPagedQuery(
                        connection,
                        "SELECT id, name, tenant_id FROM TEST_TABLE WHERE vector_data <=> ?::vector < ? ORDER BY id",
                        null,
                        "[0.1]",
                        0.8,
                        (stmt, start, vector, threshold) ->
                            bindVector(stmt, start, vector, threshold),
                        1,
                        10,
                        lot::mapResultSet,
                        (stmt, start, vector, threshold) ->
                            bindVector(stmt, start, vector, threshold)))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class executeTtlExpiredQueryWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントIDの条件を付けない期限切れ取得で、LIMIT と OFFSET を順にバインドすることを確かめる。 バッチ用の全テナント対象の取得で、件数とオフセットを渡すためである。
       *
       * @given 期限切れの判定の SQL の日数が 30 、結果が 2件
       * @when executeTtlExpiredQueryWithoutTenantFilter をオフセット 0 、件数上限 10 で実行する
       * @then 1番目に件数上限の10、2番目に OFFSET の0を設定し、2件を返す
       */
      @Test
      void 全テナント対象の場合_LIMITとOFFSETを順に設定して2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = stubAnyStatement(connection, twoRowsResult());

        List<TestEntity> result =
            new TestLot()
                .executeTtlExpiredQueryWithoutTenantFilter(
                    connection,
                    "SELECT id, name, tenant_id FROM TEST_TABLE WHERE (ttl < NOW() - INTERVAL '",
                    " days') < NOW() ORDER BY id LIMIT ? OFFSET ?",
                    30,
                    0,
                    10,
                    new TestLot()::mapResultSet);

        verify(statement).setInt(1, 10);
        verify(statement).setInt(2, 0);
        assertThat(result).hasSize(2);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null
       * @when executeTtlExpiredQueryWithoutTenantFilter を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        assertThatThrownBy(
                () -> {
                  new TestLot()
                      .executeTtlExpiredQueryWithoutTenantFilter(
                          null,
                          "SELECT id FROM TEST_TABLE",
                          "",
                          30,
                          0,
                          10,
                          new TestLot()::mapResultSet);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class toString {

    @Nested
    class 正常系 {

      /**
       * 要素を番号付きの文字列として連結して返すことを確かめる。 一覧の内容をログで追えるようにするためである。
       *
       * @given 2件のLot
       * @when toString を実行する
       * @then 番号 0 と 1 を付けた要素を含む文字列を返す
       */
      @Test
      void 要素が2件の場合_番号付きの文字列を返す() {
        TestLot lot = new TestLot();
        lot.add(new TestEntity(TENANT_ID, "U001", "山田"));
        lot.add(new TestEntity(TENANT_ID, "U002", "佐藤"));

        assertThat(lot.toString()).startsWith("(0)").contains("(1)");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要素が無い場合に空の文字列を返すことを確かめる。 0件の一覧をログに出しても例外にならないためである。
       *
       * @given 空のLot
       * @when toString を実行する
       * @then 空の文字列を返す
       */
      @Test
      void 要素が無い場合_空の文字列を返す() {
        assertThat(new TestLot().toString()).isEmpty();
      }
    }
  }

  @Nested
  class getSelectLikeSql {

    @Nested
    class 境界値 {

      /**
       * 全文検索用のSQLを定義していない場合に null を返すことを確かめる。 定義が無い Lot は全文検索を実行しないことを示すためである。
       *
       * @given 全文検索用のSQLを定義しない Lot
       * @when getSelectLikeSql を実行する
       * @then null を返す
       */
      @Test
      void 全文検索用SQLを定義しない場合_nullを返す() {
        assertThat(new PlainLot().getSelectLikeSql()).isNull();
      }
    }
  }

  @Nested
  class selectByDynamicWherePaged {

    @Nested
    class 正常系 {

      /**
       * 検索語で絞り込んだ件数と1ページ目を取得し、テナントIDを LIMIT より前にバインドすることを確かめる。 動的な検索条件でも、テナントIDの条件が LIMIT
       * より前に入るためである。
       *
       * @given 件数の問い合わせが 2 を返し、一覧の問い合わせが 2件を返す、検索語が 山 の1件
       * @when selectByDynamicWherePaged を1ページ目、1ページ10件で実行する
       * @then 検索語とテナントIDを順に設定し、2件を返す
       */
      @Test
      void 検索語がある場合_2件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(connection, "COUNT(*)", countResult(2));
        PreparedStatement pageStatement = stubStatement(connection, "LIMIT", twoRowsResult());
        TestLot lot = new TestLot();

        lot.selectByDynamicWherePaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of("%山%"),
            1,
            10);

        verify(pageStatement).setString(1, "%山%");
        verify(pageStatement).setString(2, TENANT_ID);
        verify(pageStatement).setInt(3, 10);
        verify(pageStatement).setInt(4, 0);
        assertThat(lot.size()).isEqualTo(2);
      }

      /**
       * 件数が 0 件の場合に一覧の問い合わせを行わないことを確かめる。 0件の検索結果で余分な問い合わせを行わないためである。
       *
       * @given 件数の問い合わせが 0 を返す、検索語が 山
       * @when selectByDynamicWherePaged を実行する
       * @then 件数の問い合わせだけを行い、空のLotを返す
       */
      @Test
      void 件数が0件の場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        lot.selectByDynamicWherePaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of("%山%"),
            1,
            10);

        verify(connection, times(1)).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 件数の問い合わせが行を返さない場合に、件数0のまま空の一覧を返すことを確かめる。 件数が取れないときは一覧を読み込まない。
       *
       * @given 件数の問い合わせの結果が 0 行
       * @when selectByDynamicWherePaged を接続、テナントID "T001"、条件 "name LIKE ?"、1ページ目、1ページ10件で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @Test
      void 件数の結果が0行の場合_件数0の空の一覧を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(
            connection, "COUNT(*)", MockResultSetBuilder.builder().columns("count").build());
        TestLot lot = new TestLot();

        lot.selectByDynamicWherePaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of("%山%"),
            1,
            10);

        assertThat(lot.getTotalCount()).isEqualTo(0);
        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null、検索語が 山
       * @when selectByDynamicWherePaged を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        TestLot lot = new TestLot();
        assertThatThrownBy(
                () -> {
                  lot.selectByDynamicWherePaged(
                      null,
                      TENANT_ID,
                      "SELECT id FROM TEST_TABLE WHERE ",
                      "name LIKE ?",
                      List.of("%山%"),
                      1,
                      10);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 検索語が空の場合に問い合わせを行わないことを確かめる。 検索語の無い全文検索で全件を返さないためである。
       *
       * @given 検索語が空のリスト
       * @when selectByDynamicWherePaged を検索語が空で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 検索語が空の場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWherePaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of(),
            1,
            10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 検索語が null の場合に問い合わせを行わないことを確かめる。 検索語の無い全文検索で全件を返さないためである。
       *
       * @given 検索語が null
       * @when selectByDynamicWherePaged を検索語 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 検索語がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWherePaged(
            connection, TENANT_ID, "SELECT id FROM TEST_TABLE WHERE ", "name LIKE ?", null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 条件式が null の場合に問い合わせを行わないことを確かめる。 条件式の無い SQL は作れないためである。
       *
       * @given 条件式が null
       * @when selectByDynamicWherePaged を条件式 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 条件式がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWherePaged(
            connection, TENANT_ID, "SELECT id FROM TEST_TABLE WHERE ", null, List.of("%山%"), 1, 10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * SQL の先頭部分が null の場合に問い合わせを行わないことを確かめる。 先頭部分の無い SQL は作れないためである。
       *
       * @given SQL の先頭部分が null
       * @when selectByDynamicWherePaged を先頭部分 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 先頭部分がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWherePaged(
            connection, TENANT_ID, null, "name LIKE ?", List.of("%山%"), 1, 10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 件数の問い合わせのテナントIDの条件に、指定したテナントIDを設定することを確かめる。 他テナントの件数を混ぜないためである。
       *
       * @given 件数の問い合わせが 0 を返す、検索語が 山
       * @when selectByDynamicWherePaged をテナントID T001 で実行する
       * @then 件数の問い合わせの2番目の値に T001 を設定する
       */
      @Test
      void テナントIDがT001の場合_件数の問い合わせに設定して空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        lot.selectByDynamicWherePaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of("%山%"),
            1,
            10);

        verify(countStatement).setString(2, TENANT_ID);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class selectByDynamicWhereWithFilterPaged_Connection_String_String_String_List_Object_Object_Integer_Enum_int_int {

    @Nested
    class 正常系 {

      /**
       * 価格・開始日・整数・エリアのフィルタと検索語を、順にバインドしてページング検索することを確かめる。 詳細な条件を全て指定した検索でも、各値の位置を揃えるためである。
       *
       * @given 件数の問い合わせが 1 を返し、一覧が 1件 を返す、全てのフィルタに値がある
       * @when selectByDynamicWhereWithFilterPaged を全フィルタ付きで1ページ目、1ページ10件で実行する
       * @then 価格、開始日、整数、エリア、検索語、テナントIDの順に値を設定し、1件を返す
       */
      @Test
      void 全フィルタに値がある場合_1件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubStatement(connection, "COUNT(*)", countResult(1));
        PreparedStatement pageStatement =
            stubStatement(
                connection,
                "LIMIT",
                MockResultSetBuilder.builder()
                    .columns("id", "name", "tenant_id")
                    .row("U001", "山田", TENANT_ID)
                    .build());
        TestLot lot = new TestLot();
        Timestamp startDate = Timestamp.valueOf("2026-01-15 00:00:00");

        lot.<BigDecimal, java.time.LocalDateTime, Enum<?>>selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE WHERE ",
            "price <= ? AND start_date <= ? AND n = ? AND area = ? AND name LIKE ?",
            List.of("%山%"),
            new BigDecimal("1000"),
            startDate,
            3,
            TestArea.東京,
            1,
            10);

        verify(countStatement).setBigDecimal(1, new BigDecimal("1000"));
        verify(countStatement).setTimestamp(2, startDate);
        verify(countStatement).setInt(3, 3);
        verify(countStatement).setString(4, "東京");
        verify(countStatement).setString(5, "%山%");
        verify(countStatement).setString(6, TENANT_ID);
        verify(pageStatement).setString(6, TENANT_ID);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * フィルタに値が無い場合に、検索語とテナントIDだけをバインドすることを確かめる。 指定の無いフィルタは条件に含めないためである。
       *
       * @given 価格・開始日・整数・エリアが null、件数の問い合わせが 1 を返す
       * @when selectByDynamicWhereWithFilterPaged を検索語 山 で実行する
       * @then 検索語とテナントIDだけを順に設定し、1件を返す
       */
      @Test
      void フィルタが無い場合_検索語とテナントIDだけをバインドして1件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubStatement(connection, "COUNT(*)", countResult(1));
        stubStatement(
            connection,
            "LIMIT",
            MockResultSetBuilder.builder()
                .columns("id", "name", "tenant_id")
                .row("U001", "山田", TENANT_ID)
                .build());
        TestLot lot = new TestLot();

        lot.<BigDecimal, java.time.LocalDateTime, Enum<?>>selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of("%山%"),
            null,
            null,
            null,
            null,
            1,
            10);

        verify(countStatement).setString(1, "%山%");
        verify(countStatement).setString(2, TENANT_ID);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 件数が 0 件の場合に一覧の問い合わせを行わないことを確かめる。 0件の検索結果で余分な問い合わせを行わないためである。
       *
       * @given 件数の問い合わせが 0 を返す
       * @when selectByDynamicWhereWithFilterPaged を検索語 山 で実行する
       * @then 件数の問い合わせだけを行い、空のLotを返す
       */
      @Test
      void 件数が0件の場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        lot.<BigDecimal, java.time.LocalDateTime, Enum<?>>selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of("%山%"),
            null,
            null,
            null,
            null,
            1,
            10);

        verify(connection, times(1)).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 整数フィルタだけを渡した場合に例外を投げることを確かめる。 整数とエリアは対で SQL の条件を作るため、片方だけでは条件とプレースホルダがずれるためである。
       *
       * @given 整数フィルタが 3、エリアフィルタが null、件数の問い合わせが 0 を返す
       * @when selectByDynamicWhereWithFilterPaged を整数フィルタだけで実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 整数フィルタだけの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        assertThatThrownBy(
                () ->
                    lot
                        .<BigDecimal, java.time.LocalDateTime, Enum<?>>
                            selectByDynamicWhereWithFilterPaged(
                                connection,
                                TENANT_ID,
                                "SELECT id FROM TEST_TABLE WHERE ",
                                "name LIKE ?",
                                List.of("%山%"),
                                null,
                                null,
                                3,
                                null,
                                1,
                                10))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * エリアフィルタだけを渡した場合に例外を投げることを確かめる。 整数とエリアは対で SQL の条件を作るため、片方だけでは条件とプレースホルダがずれるためである。
       *
       * @given 整数フィルタが null、エリアフィルタが 東京、件数の問い合わせが 0 を返す
       * @when selectByDynamicWhereWithFilterPaged をエリアフィルタだけで実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void エリアフィルタだけの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        assertThatThrownBy(
                () ->
                    lot
                        .<BigDecimal, java.time.LocalDateTime, Enum<?>>
                            selectByDynamicWhereWithFilterPaged(
                                connection,
                                TENANT_ID,
                                "SELECT id FROM TEST_TABLE WHERE ",
                                "name LIKE ?",
                                List.of("%山%"),
                                null,
                                null,
                                null,
                                TestArea.東京,
                                1,
                                10))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 価格に BigDecimal 以外の値を渡した場合に例外を投げることを確かめる。 型の合わない値をそのままバインドすると、SQL の条件が誤った値で実行されるためである。
       *
       * @given 価格が文字列 "1000"、件数の問い合わせが 0 を返す
       * @when selectByDynamicWhereWithFilterPaged を文字列の価格で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 価格が文字列の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        assertThatThrownBy(
                () ->
                    lot
                        .<BigDecimal, java.time.LocalDateTime, Enum<?>>
                            selectByDynamicWhereWithFilterPaged(
                                connection,
                                TENANT_ID,
                                "SELECT id FROM TEST_TABLE WHERE ",
                                "name LIKE ?",
                                List.of("%山%"),
                                "1000",
                                null,
                                null,
                                null,
                                1,
                                10))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 開始日に Timestamp 以外の値を渡した場合に例外を投げることを確かめる。 型の合わない値をそのままバインドすると、開始日の条件が誤った値で実行されるためである。
       *
       * @given 開始日が文字列 "2026-01-15"、件数の問い合わせが 0 を返す
       * @when selectByDynamicWhereWithFilterPaged を文字列の開始日で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 開始日が文字列の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        assertThatThrownBy(
                () ->
                    lot
                        .<BigDecimal, java.time.LocalDateTime, Enum<?>>
                            selectByDynamicWhereWithFilterPaged(
                                connection,
                                TENANT_ID,
                                "SELECT id FROM TEST_TABLE WHERE ",
                                "name LIKE ?",
                                List.of("%山%"),
                                null,
                                "2026-01-15",
                                null,
                                null,
                                1,
                                10))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null
       * @when selectByDynamicWhereWithFilterPaged を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        TestLot lot = new TestLot();
        assertThatThrownBy(
                () -> {
                  lot
                      .<BigDecimal, java.time.LocalDateTime, Enum<?>>
                          selectByDynamicWhereWithFilterPaged(
                              null,
                              TENANT_ID,
                              "SELECT id FROM TEST_TABLE WHERE ",
                              "name LIKE ?",
                              List.of("%山%"),
                              null,
                              null,
                              null,
                              null,
                              1,
                              10);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 検索語が空の場合に問い合わせを行わないことを確かめる。 検索語の無い全文検索で全件を返さないためである。
       *
       * @given 検索語が空のリスト
       * @when selectByDynamicWhereWithFilterPaged を検索語が空で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 検索語が空の場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.<BigDecimal, java.time.LocalDateTime, Enum<?>>selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of(),
            null,
            null,
            null,
            null,
            1,
            10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 先頭部分が null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 先頭部分の無い SQL は作れないためである。
       *
       * @given 先頭部分が null
       * @when selectByDynamicWhereWithFilterPaged を先頭部分 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 先頭部分がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            null,
            "name LIKE ?",
            List.of("%山%"),
            null,
            null,
            null,
            null,
            1,
            10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 条件式が null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 条件式の無い SQL は作れないためである。
       *
       * @given 条件式が null
       * @when selectByDynamicWhereWithFilterPaged を条件式 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 条件式がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            null,
            List.of("%山%"),
            null,
            null,
            null,
            null,
            1,
            10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 検索語が null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 検索語の無い全文検索で全件を返さないためである。
       *
       * @given 検索語が null
       * @when selectByDynamicWhereWithFilterPaged を検索語 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 検索語がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            null,
            null,
            null,
            null,
            null,
            1,
            10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class selectByDynamicWhereWithFilterPaged_Connection_String_String_String_List_List_int_int {

    @Nested
    class 正常系 {

      /**
       * 検索語と詳細フィルタを順にバインドして、件数と1ページ目を取得することを確かめる。 文字列と整数の詳細条件を、検索語と合わせて絞り込むためである。
       *
       * @given 件数の問い合わせが 1 を返し、一覧が 1件 を返す、詳細フィルタが 東京 と 100
       * @when selectByDynamicWhereWithFilterPaged を検索語 山 、詳細フィルタつきで1ページ目、1ページ10件で実行する
       * @then 検索語、詳細フィルタ、テナントIDの順に設定し、1件を返す
       */
      @Test
      void 検索語と詳細フィルタがある場合_1件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubStatement(connection, "COUNT(*)", countResult(1));
        stubStatement(
            connection,
            "LIMIT",
            MockResultSetBuilder.builder()
                .columns("id", "name", "tenant_id")
                .row("U001", "山田", TENANT_ID)
                .build());
        TestLot lot = new TestLot();

        lot.selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE WHERE ",
            "name LIKE ? AND area = ? AND n = ?",
            List.of("%山%"),
            List.of("東京", 100),
            1,
            10);

        verify(countStatement).setString(1, "%山%");
        verify(countStatement).setString(2, "東京");
        verify(countStatement).setInt(3, 100);
        verify(countStatement).setString(4, TENANT_ID);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 検索語と詳細フィルタが無い場合に、テナントIDだけをバインドして検索することを確かめる。 条件の無い一覧でも、テナントIDで絞り込むためである。
       *
       * @given 検索語と詳細フィルタが null、件数の問い合わせが 1 を返す
       * @when selectByDynamicWhereWithFilterPaged を検索語と詳細フィルタ null で実行する
       * @then テナントIDだけを1番目に設定し、1件を返す
       */
      @Test
      void 検索語と詳細フィルタが無い場合_テナントIDだけをバインドして1件を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubStatement(connection, "COUNT(*)", countResult(1));
        stubStatement(
            connection,
            "LIMIT",
            MockResultSetBuilder.builder()
                .columns("id", "name", "tenant_id")
                .row("U001", "山田", TENANT_ID)
                .build());
        TestLot lot = new TestLot();

        lot.selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id, name, tenant_id FROM TEST_TABLE WHERE ",
            "status = 'A'",
            null,
            null,
            1,
            10);

        verify(countStatement).setString(1, TENANT_ID);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 件数が 0 件の場合に一覧の問い合わせを行わないことを確かめる。 0件の検索結果で余分な問い合わせを行わないためである。
       *
       * @given 件数の問い合わせが 0 を返す
       * @when selectByDynamicWhereWithFilterPaged を検索語 山 で実行する
       * @then 件数の問い合わせだけを行い、空のLotを返す
       */
      @Test
      void 件数が0件の場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubAnyStatement(connection, countResult(0));
        TestLot lot = new TestLot();

        lot.selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            "name LIKE ?",
            List.of("%山%"),
            List.of(),
            1,
            10);

        verify(connection, times(1)).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null
       * @when selectByDynamicWhereWithFilterPaged を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        TestLot lot = new TestLot();
        assertThatThrownBy(
                () -> {
                  lot.selectByDynamicWhereWithFilterPaged(
                      null,
                      TENANT_ID,
                      "SELECT id FROM TEST_TABLE WHERE ",
                      "name LIKE ?",
                      List.of("%山%"),
                      List.of(),
                      1,
                      10);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 先頭部分が null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 先頭部分の無い SQL は作れないためである。
       *
       * @given 先頭部分が null
       * @when selectByDynamicWhereWithFilterPaged を先頭部分 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 先頭部分がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWhereWithFilterPaged(
            connection, TENANT_ID, null, "name LIKE ?", List.of("%山%"), List.of(), 1, 10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 条件式が null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 条件式の無い SQL は作れないためである。
       *
       * @given 条件式が null
       * @when selectByDynamicWhereWithFilterPaged を条件式 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 条件式がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByDynamicWhereWithFilterPaged(
            connection,
            TENANT_ID,
            "SELECT id FROM TEST_TABLE WHERE ",
            null,
            List.of("%山%"),
            List.of(),
            1,
            10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class logExecutedSql {

    @Nested
    class 正常系 {

      /**
       * バインド値を埋め込んだSQLをログに送信することを確かめる。 実行されたSQLを値付きで後から追えるようにするためである。
       *
       * @given INFO レベルのログが有効で、ログのアペンダーを付けている、バインド値が 山田 と 3
       * @when logExecutedSql を SQL と バインド値で実行する
       * @then 値を埋め込んだSQLを1件ログに送信する
       */
      @Test
      void 文字列と数値のバインド値がある場合_値を埋め込んだSQLのログを送信する() {
        attachLogAppender(Level.INFO);
        List<ILoggingEvent> events = logAppender.list;

        new TestLot().logExecutedSql("SELECT ? , ?", List.of("山田", 3));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("[SQL] SELECT '山田' , 3");
      }

      /**
       * 値に記号の $ と \ を含んでも、値をそのまま埋め込んだSQLをログに送信することを確かめる。 置換の記号として解釈すると値が壊れ、ログを見ても実行された値を追えないためである。
       *
       * @given INFO レベルのログが有効で、ログのアペンダーを付けている、バインド値が 価格$1 と C:\dir
       * @when logExecutedSql を SQL と記号を含むバインド値で実行する
       * @then 記号を含む値をそのまま埋め込んだSQLを1件ログに送信する
       */
      @Test
      void 記号を含むバインド値がある場合_値をそのまま埋め込んだSQLのログを送信する() {
        attachLogAppender(Level.INFO);
        List<ILoggingEvent> events = logAppender.list;

        new TestLot().logExecutedSql("SELECT ?, ?", List.of("価格$1", "C:\\dir"));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("[SQL] SELECT '価格$1', 'C:\\dir'");
      }

      /**
       * バインド値が null の場合に NULL と埋め込んでログに送信することを確かめる。 値が無い条件も SQL として読めるようにするためである。
       *
       * @given INFO レベルのログが有効で、バインド値が null
       * @when logExecutedSql を SQL と null のバインド値で実行する
       * @then NULL を埋め込んだSQLを1件ログに送信する
       */
      @Test
      void バインド値がnullの場合_NULLを埋め込んだSQLのログを送信する() {
        attachLogAppender(Level.INFO);
        List<ILoggingEvent> events = logAppender.list;
        List<Object> values = new ArrayList<>();
        values.add(null);

        new TestLot().logExecutedSql("SELECT ?", values);

        assertThat(events.get(0).getFormattedMessage()).isEqualTo("[SQL] SELECT NULL");
      }

      /**
       * バインド値が無い場合にSQLをそのままログに送信することを確かめる。 値を持たないSQL でも、ログに残せるようにするためである。
       *
       * @given INFO レベルのログが有効で、バインド値が空
       * @when logExecutedSql を SQL と空のバインド値で実行する
       * @then SQL をそのまま1件ログに送信する
       */
      @Test
      void バインド値が空の場合_SQLのログを送信する() {
        attachLogAppender(Level.INFO);
        List<ILoggingEvent> events = logAppender.list;

        new TestLot().logExecutedSql("SELECT 1", List.of());

        assertThat(events.get(0).getFormattedMessage()).isEqualTo("[SQL] SELECT 1");
      }

      /**
       * INFO ログが無効な場合にログを出力しないことを確かめる。 ログを止めた環境では、値の埋め込みを行わないためである。
       *
       * @given INFO より高いレベルのみ有効で、ログのアペンダーを付けている
       * @when logExecutedSql を SQL とバインド値で実行する
       * @then 何もしない
       */
      @Test
      void INFOレベルが無効な場合_何もしない() {
        attachLogAppender(Level.WARN);
        List<ILoggingEvent> events = logAppender.list;

        new TestLot().logExecutedSql("SELECT ?", List.of("山田"));

        assertThat(events).isEmpty();
      }

      /**
       * バインド値の一覧が null の場合に、SQL をそのままログに送信することを確かめる。 一覧が無い呼び出しでも、ログの出力で例外を出さないためである。
       *
       * @given INFO レベルのログが有効で、バインド値の一覧が null
       * @when logExecutedSql を SQL SELECT 1 と null で実行する
       * @then SQL をそのまま1件ログに送信する
       */
      @Test
      void バインド値の一覧がnullの場合_SQLのログを送信する() {
        attachLogAppender(Level.INFO);
        List<ILoggingEvent> events = logAppender.list;

        new TestLot().logExecutedSql("SELECT 1", null);

        assertThat(events.get(0).getFormattedMessage()).isEqualTo("[SQL] SELECT 1");
      }
    }
  }

  @Nested
  class logSql {

    @Nested
    class 正常系 {

      /**
       * INFO ログが有効な場合に SQL をログに送信することを確かめる。 実行前の SQL を後から追えるようにするためである。
       *
       * @given INFO レベルのログが有効で、ログのアペンダーを付けている
       * @when logSql を SQL SELECT 1 で実行する
       * @then SQL を含む1件のログを送信する
       */
      @Test
      void INFOレベルが有効な場合_SQLを含むログを送信する() {
        attachLogAppender(Level.INFO);
        List<ILoggingEvent> events = logAppender.list;

        new TestLot().logSql("SELECT 1");

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("[SQL] SELECT 1");
      }
    }

    @Nested
    class 境界値 {
      /**
       * INFO ログが無効な場合にログを出力しないことを確かめる。 ログを止めた環境では SQL の文字列を余分に扱わないためである。
       *
       * @given INFO より高いレベルのみ有効で、ログのアペンダーを付けている
       * @when logSql を SQL SELECT 1 で実行する
       * @then 何もしない
       */
      @Test
      void INFOレベルが無効な場合_何もしない() {
        attachLogAppender(Level.WARN);
        List<ILoggingEvent> events = logAppender.list;

        new TestLot().logSql("SELECT 1");

        assertThat(events).isEmpty();
      }
    }
  }

  @Nested
  class addTenantIdFilter {

    @Nested
    class 正常系 {

      /**
       * カンマ区切りの FROM 句で別名を持つ結合先の条件を、結合先の別名で付けることを確かめる。 別名を無視して表名で付けると、別名付きの列を参照するSQLと食い違うためである。
       *
       * @given 基本SQLが FROM T1 a, T2 b の形で WHERE 句を1つ持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 結合先の別名 b の条件を WHERE 句の末尾に付けたSQLを返す
       */
      @Test
      void カンマ区切りの別名付きの場合_結合先の別名で条件を付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter("SELECT * FROM T1 a, T2 b WHERE a.x = ?", TENANT_ID))
            .isEqualTo(
                "SELECT * FROM T1 a, T2 b WHERE a.x = ? AND a.tenant_id = ? AND b.tenant_id = a.tenant_id");
      }

      /**
       * WHERE 句の無い SQL にテナントIDの WHERE 句を付けることを確かめる。 テナントIDの条件を必ず付けて、他テナントの行を返さないためである。
       *
       * @given 基本SQLが WHERE 句を持たない SELECT 文
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 末尾に WHERE tenant_id の条件を付けたSQLを返す
       */
      @Test
      void WHERE句が無い場合_WHERE句でテナントIDの条件を付けたSQLを返す() {
        assertThat(new TestLot().addTenantIdFilter("SELECT id FROM TEST_TABLE", TENANT_ID))
            .isEqualTo("SELECT id FROM TEST_TABLE WHERE tenant_id = ?");
      }

      /**
       * LIMIT 句の前にテナントIDの条件を挿入することを確かめる。 LIMIT の後ろに条件を置くと SQL の構文が壊れるためである。
       *
       * @given 基本SQLが WHERE 句と LIMIT 句を持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then LIMIT の前に AND の条件を挿入したSQLを返す
       */
      @Test
      void LIMIT句を含む場合_LIMIT句の前にテナントIDの条件を挿入したSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter("SELECT id FROM TEST_TABLE WHERE x = ? LIMIT ?", TENANT_ID))
            .isEqualTo("SELECT id FROM TEST_TABLE WHERE x = ? AND tenant_id = ? LIMIT ?");
      }

      /**
       * 別名を持つ SELECT 句のテナントIDの列を基に、別名付きの条件を付けることを確かめる。 結合のある SQL で、どのテーブルの条件かを明示するためである。
       *
       * @given 基本SQLが SELECT 句に t.tenant_id を持ち、FROM 句で別名 t を付けている
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 別名 t のテナントIDの条件を付けたSQLを返す
       */
      @Test
      void テーブル別名がある場合_別名付きのテナントIDの条件を付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter(
                        "SELECT t.id, t.tenant_id FROM TEST_TABLE t WHERE t.x = ?", TENANT_ID))
            .isEqualTo(
                "SELECT t.id, t.tenant_id FROM TEST_TABLE t WHERE t.x = ? AND t.tenant_id = ?");
      }

      /**
       * INNER JOIN の場合に右側のテーブルの別名を基に条件を付けることを確かめる。 結合先のテーブルも自分のテナントに絞るためである。
       *
       * @given 基本SQLが FROM T1 a と INNER JOIN T2 b を持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 右側の別名 b のテナントIDの条件を付けたSQLを返す
       */
      @Test
      void 内部結合の場合_結合先と主テーブルの条件を付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter(
                        "SELECT a.id FROM T1 a INNER JOIN T2 b ON b.x = a.id", TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a INNER JOIN T2 b ON (b.x = a.id) AND b.tenant_id = a.tenant_id WHERE a.tenant_id = ?");
      }

      /**
       * LEFT JOIN の場合に左側のテーブルの別名を基に条件を付けることを確かめる。 左側の一覧の行を自分のテナントに絞るためである。
       *
       * @given 基本SQLが FROM T1 a と LEFT JOIN T2 b を持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 左側の別名 a のテナントIDの条件を付けたSQLを返す
       */
      @Test
      void 左外部結合の場合_右側の条件をON句に付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter(
                        "SELECT a.id FROM T1 a LEFT JOIN T2 b ON b.x = a.id", TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a LEFT JOIN T2 b ON (b.x = a.id) AND b.tenant_id = a.tenant_id WHERE a.tenant_id = ?");
      }

      /**
       * EXISTS 文では括弧の中の WHERE 句にテナントIDの条件を挿入することを確かめる。 存在確認も他テナントの行を見つけないように括弧の中で絞り込むためである。
       *
       * @given 基本SQLが SELECT EXISTS の括弧内に WHERE 句を1つ持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 括弧の中にテナントIDの条件を挿入したSQLを返す
       */
      @Test
      void EXISTS文の場合_括弧内にテナントIDの条件を挿入したSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter("SELECT EXISTS(SELECT 1 FROM T WHERE id = ?)", TENANT_ID))
            .isEqualTo("SELECT EXISTS(SELECT 1 FROM T WHERE id = ? AND tenant_id = ?)");
      }

      /**
       * CROSS JOIN は ON 句が無いため、結合先のテナント条件を WHERE 句の末尾に付けることを確かめる。 結合先の列どうしを比べる条件はプレースホルダを使わないためである。
       *
       * @given 基本SQLが FROM T1 a と CROSS JOIN T2 b を持ち WHERE 句が1つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 主テーブルと結合先の条件を WHERE 句の末尾に付けたSQLを返す
       */
      @Test
      void CROSS_JOINの場合_結合先の条件をWHERE句の末尾に付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter(
                        "SELECT * FROM T1 a CROSS JOIN T2 b WHERE a.x = ?", TENANT_ID))
            .isEqualTo(
                "SELECT * FROM T1 a CROSS JOIN T2 b WHERE a.x = ? AND a.tenant_id = ? AND b.tenant_id = a.tenant_id");
      }

      /**
       * 別名が無い結合では、主テーブル名で修飾したテナント条件を付けることを確かめる。 別名が無いと列の修飾先が無くなるためである。
       *
       * @given 基本SQLが FROM T1 と INNER JOIN T2 を持ち、ON 句は T2.x = T1.id
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then ON 句を括弧で包み、主テーブル名で修飾した条件を WHERE 句に付けたSQLを返す
       */
      @Test
      void 別名の無いJOINの場合_主テーブル名で修飾した条件を付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter("SELECT * FROM T1 INNER JOIN T2 ON T2.x = T1.id", TENANT_ID))
            .isEqualTo(
                "SELECT * FROM T1 INNER JOIN T2 ON (T2.x = T1.id) AND T2.tenant_id = T1.tenant_id WHERE T1.tenant_id = ?");
      }

      /**
       * WHERE 句が無い結合でも、ON 句の終わりを見つけて条件を付けることを確かめる。 WHERE 句の無い検索の SQL でも結合先の条件が漏れないようにするためである。
       *
       * @given 基本SQLが FROM T1 a と LEFT JOIN T2 b ON 句を持ち WHERE 句が無い
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then ON 句の結合先の条件と主テーブルの条件を WHERE 句に付けたSQLを返す
       */
      @Test
      void ON句の結合でWHERE句が無い場合_ON句の結合先と主テーブルの条件を付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter(
                        "SELECT a.id FROM T1 a LEFT JOIN T2 b ON b.x = a.id", TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a LEFT JOIN T2 b ON (b.x = a.id) AND b.tenant_id = a.tenant_id WHERE a.tenant_id = ?");
      }

      /**
       * ON 句の後ろに別の結合が続く場合、ON 句の終わりで区切って条件を挿入することを確かめる。 次の結合の句を ON 句に含めると、条件の位置が変わるためである。
       *
       * @given 基本SQLが LEFT JOIN T2 b ON と CROSS JOIN T3 c を持ち WHERE 句が無い
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then ON 句は括弧で包み、CROSS JOIN の結合先は WHERE 句に付けたSQLを返す
       */
      @Test
      void ON句の後にCROSS_JOINが続く場合_ON句の終わりで区切った条件を付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter(
                        "SELECT a.id FROM T1 a LEFT JOIN T2 b ON b.x = a.id CROSS JOIN T3 c",
                        TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a LEFT JOIN T2 b ON (b.x = a.id) AND b.tenant_id = a.tenant_id CROSS JOIN T3 c WHERE a.tenant_id = ? AND c.tenant_id = a.tenant_id");
      }
    }

    @Nested
    class 境界値 {

      /**
       * FROM 句の無いSQLの場合に、テナントIDの条件を WHERE 句として付けることを確かめる。 主テーブルの別名は FROM 句から取るため、FROM
       * 句が無いSQLでも条件の付加を止めない。
       *
       * @given 基本SQLが SELECT 1 の1行で FROM 句も WHERE 句も無い
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then tenant_id の WHERE 句を末尾に付けたSQLを返す
       */
      @Test
      void FROM句が無い場合_tenant_idの条件を付けたSQLを返す() {
        assertThat(new TestLot().addTenantIdFilter("SELECT 1", TENANT_ID))
            .isEqualTo("SELECT 1 WHERE tenant_id = ?");
      }

      /**
       * WHERE 句に主テーブルの tenant_id を手書きした SQL を拒否することを確かめる。 テナント条件は必ず付与されるため、手書きすると二重に付くためである。
       *
       * @given 基本SQLの WHERE 句に tenant_id = ? と x = ? がある
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void WHERE句に主テーブルのtenant_idがある場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TestLot()
                        .addTenantIdFilter(
                            "SELECT id FROM TEST_TABLE WHERE tenant_id = ? AND x = ?", TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 2文字の別名を持つ表でも、別名付きの条件を付けることを確かめる。 別名を読めないと列名だけの条件になり、結合時に列名が曖昧になるためである。
       *
       * @given 基本SQLの SELECT 句に tt.tenant_id を持ち、FROM 句で別名 tt を付けている
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 2文字の別名付きの条件を付けたSQLを返す
       */
      @Test
      void 別名が2文字の場合_2文字の別名付きの条件を付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter(
                        "SELECT tt.id, tt.tenant_id FROM TEST_TABLE tt WHERE tt.x = ?", TENANT_ID))
            .isEqualTo(
                "SELECT tt.id, tt.tenant_id FROM TEST_TABLE tt WHERE tt.x = ? AND tt.tenant_id = ?");
      }

      /**
       * テナントIDが null の場合に例外を投げることを確かめる。 テナントIDの無い条件は全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが null
       * @when addTenantIdFilter をテナントID null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TestLot().addTenantIdFilter("SELECT id FROM TEST_TABLE", null))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 基本SQLが null の場合に例外を投げることを確かめる。 条件を付ける対象が無いため、先に止める。
       *
       * @given 基本SQLが null
       * @when addTenantIdFilter を基本SQL null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 基本SQLがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TestLot().addTenantIdFilter(null, TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 内部結合の右側に ON 句が無い場合に、別名を検出せず tenant_id の条件を付けることを確かめる。 右側の別名が無いと、別名付きの条件を組み立てられないためである。
       *
       * @given 基本SQLが FROM T1 a と INNER JOIN T2 b を持ち ON 句が無い
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 別名を付けない tenant_id の条件を付けたSQLを返す
       */
      @Test
      void 内部結合でONが無い場合_結合先の条件をWHERE句の末尾に付けたSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter(
                        "SELECT a.id FROM T1 a INNER JOIN T2 b WHERE a.x = ?", TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a INNER JOIN T2 b WHERE a.x = ? AND a.tenant_id = ? AND b.tenant_id = a.tenant_id");
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * WHERE 句に OR がある場合、OR 全体を括弧で包んでからテナントIDの条件を AND で付けることを確かめる。 括弧で包まないと、テナントIDの条件が最後の OR
       * の項だけに掛かるためである。
       *
       * @given 基本SQLの WHERE 句が a = ? OR b = ?
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then テナントIDの条件を b の条件の後ろに付けたSQLを返す
       */
      @Test
      void WHERE句にORがある場合_OR全体を括弧で包んだSQLを返す() {
        assertThat(
                new TestLot()
                    .addTenantIdFilter("SELECT id FROM TEST_TABLE WHERE a = ? OR b = ?", TENANT_ID))
            .isEqualTo("SELECT id FROM TEST_TABLE WHERE (a = ? OR b = ?) AND tenant_id = ?");
      }
    }
  }

  @Nested
  class setTenantIdParameter {

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に例外を投げ、値を設定しないことを確かめる。 テナントIDの無い条件のバインドは全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが null
       * @when setTenantIdParameter をテナントID null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        assertThatThrownBy(() -> new TestLot().setTenantIdParameter(statement, 1, null))
            .isInstanceOf(IllegalArgumentException.class);
        verify(statement, never()).setString(org.mockito.ArgumentMatchers.anyInt(), anyString());
      }

      /**
       * テナントIDが空文字の場合に例外を投げることを確かめる。 空のテナントIDの条件は全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが空文字
       * @when setTenantIdParameter をテナントIDが空文字で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        assertThatThrownBy(() -> new TestLot().setTenantIdParameter(statement, 1, ""))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * WHERE 句の括弧の中に結合を含む副問い合わせがあると拒否することを確かめる。 副問い合わせの表にはテナント条件が付かないため、結合先の行が他テナントを含むおそれがある。
       *
       * @given 基本SQLの WHERE 句の副問い合わせが INNER JOIN を含む
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void WHERE句の副問い合わせに結合がある場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TestLot()
                        .addTenantIdFilter(
                            "SELECT * FROM T1 a WHERE a.id IN (SELECT x FROM T2 b INNER JOIN T3 c ON c.id = b.id)",
                            TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectByLikeQueryPaged {

    @Nested
    class 正常系 {

      /**
       * 条件を連ねた件数の問い合わせを行い、1ページ目の一覧を読み込むことを確かめる。 画面の検索結果に件数とページ情報をそろえる。
       *
       * @given 件数の問い合わせが 2 を返し、一覧の問い合わせが 2 行を返す、条件が AND の x
       * @when selectByLikeQueryPaged を接続、テナントID "T001"、列名 name、1ページ目、1ページ10件で実行する
       * @then 件数に 2、ページ情報に 10 と 1 を保持する
       */
      @Test
      void 件数が2件の場合_件数を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = stubStatement(connection, "COUNT(*)", countResult(2));
        stubStatement(connection, "LIMIT", twoRowsResult());
        List<LogicalOperators> query =
            Arrays.asList(new LogicalOperators(LogicalOperatorType.AND, "x"), null);
        TestLot lot = new TestLot();

        lot.selectByLikeQueryPaged(
            connection, TENANT_ID, SELECT_LIKE_SQL, "name", "山", query, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(2);
        assertThat(lot.getPageSize()).isEqualTo(10);
        assertThat(lot.getCurrentPageIndex()).isEqualTo(1);
        verify(countStatement).setString(1, "%山%");
        verify(countStatement).setString(2, "%x%");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null
       * @when selectByLikeQueryPaged を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        TestLot lot = new TestLot();
        assertThatThrownBy(
                () -> {
                  lot.selectByLikeQueryPaged(
                      null, TENANT_ID, SELECT_LIKE_SQL, "name", "山", null, 1, 10);
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 検索語が null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 検索語の無い全文検索で全件を返さないためである。
       *
       * @given 検索語が null
       * @when selectByLikeQueryPaged を検索語 null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 検索語がnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByLikeQueryPaged(
            connection, TENANT_ID, SELECT_LIKE_SQL, "name", null, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 基本SQLが null の場合に問い合わせを行わず、空のLotを返すことを確かめる。 基本SQLが無いと件数も一覧も作れないためである。
       *
       * @given 基本SQLが null
       * @when selectByLikeQueryPaged を基本SQL null で実行する
       * @then 問い合わせを行わず、空のLotを返す
       */
      @Test
      void 基本SQLがnullの場合_空のLotを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        TestLot lot = new TestLot();

        lot.selectByLikeQueryPaged(connection, TENANT_ID, null, "name", "山", null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 件数が0件の場合に一覧を読まず、件数0を保持することを確かめる。 対象が無い検索で一覧の問い合わせを行わない。
       *
       * @given 件数の問い合わせが 0 を返す
       * @when selectByLikeQueryPaged を接続、テナントID "T001"、列名 name、1ページ目、1ページ10件で実行する
       * @then 件数に 0 を保持し、一覧の問い合わせを呼ばない
       */
      @Test
      void 件数が0件の場合_一覧を読まず件数0を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(connection, "COUNT(*)", countResult(0));
        TestLot lot = new TestLot();

        lot.selectByLikeQueryPaged(
            connection, TENANT_ID, SELECT_LIKE_SQL, "name", "山", null, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0);
        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(contains("LIMIT"));
      }

      /**
       * 件数の問い合わせが行を返さない場合に、件数0のまま一覧を読まないことを確かめる。 件数が取れないときは一覧を読まない。
       *
       * @given 件数の問い合わせの結果が 0 行
       * @when selectByLikeQueryPaged を接続、テナントID "T001"、列名 name、1ページ目、1ページ10件で実行する
       * @then 件数に 0 を保持し、一覧の問い合わせを呼ばない
       */
      @Test
      void 件数の結果が0行の場合_一覧を読まず件数0を返す() throws SQLException {
        Connection connection = mock(Connection.class);
        stubStatement(
            connection, "COUNT(*)", MockResultSetBuilder.builder().columns("count").build());
        TestLot lot = new TestLot();

        lot.selectByLikeQueryPaged(
            connection, TENANT_ID, SELECT_LIKE_SQL, "name", "山", null, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0);
        verify(connection, never()).prepareStatement(contains("LIMIT"));
      }
    }
  }
}
