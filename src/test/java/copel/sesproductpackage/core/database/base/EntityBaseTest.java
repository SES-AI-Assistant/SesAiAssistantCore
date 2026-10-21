package copel.sesproductpackage.core.database.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** {@link EntityBase} の単体テスト。 */
class EntityBaseTest {

  /** 全テストで使うテナントID。 */
  private static final String TENANT_ID = "T001";

  /** ログを受け取るアペンダー。ログの出力を確かめるテストだけで付けて外す。 */
  private ListAppender<ILoggingEvent> logAppender;

  /** ログのレベルを変えたテストで、後始末のために元に戻すロガー。 */
  private Logger entityBaseLogger;

  /** ログを受け取るアペンダーを外し、ロガーのレベルを元に戻す。 他のテストへログとレベルの変更が残ると結果が変わるため、毎回戻す。 */
  @AfterEach
  void ログのアペンダーとレベルを戻す() {
    if (entityBaseLogger != null) {
      if (logAppender != null) {
        entityBaseLogger.detachAppender(logAppender);
      }
      entityBaseLogger.setLevel(null);
    }
    logAppender = null;
    entityBaseLogger = null;
  }

  /** テスト用の {@link EntityBase} の具象クラス。 登録日時とテナントIDを持ち、DB 操作の抽象メソッドは何もしない。 */
  private static class TestEntity extends EntityBase {

    /**
     * テナントIDだけを指定して生成する。
     *
     * @param tenantId テナントID
     */
    TestEntity(final String tenantId) {
      super(tenantId);
    }

    /**
     * テナントIDと登録日時を指定して生成する。
     *
     * @param tenantId テナントID
     * @param registerDate 登録日時
     */
    TestEntity(final String tenantId, final OriginalDateTime registerDate) {
      super(tenantId);
      this.registerDate = registerDate;
    }

    @Override
    public int insert(final Connection connection) throws SQLException {
      return 0;
    }

    @Override
    public void selectByPk(final Connection connection) throws SQLException {
      // テストでは使わない
    }

    @Override
    public boolean updateByPk(final Connection connection) throws SQLException {
      return false;
    }

    @Override
    public boolean deleteByPk(final Connection connection) throws SQLException {
      return false;
    }
  }

  /**
   * SQL を実行する接続のモックを作る。 準備文は任意の SQL に対して同じモックを返し、更新件数は 1 件を返す。
   *
   * @return 接続のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static Connection mockConnection() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenReturn(1);
    return connection;
  }

  /**
   * 準備文のモックを、接続のモックから取り出す。 接続のモックは任意の SQL に同じ準備文を返すため、空の SQL で取り出す。 テストの対象の呼び出しより前に取り出すので、検証の対象外の
   * SQL で呼び出し回数を増やさない。
   *
   * @param connection 接続のモック
   * @return 準備文のモック
   * @throws SQLException 取り出し中に例外が起きた場合
   */
  private static PreparedStatement statementOf(final Connection connection) throws SQLException {
    return connection.prepareStatement("");
  }

  /**
   * ログを受け取るアペンダーを付けて、ロガーのレベルを指定する。
   *
   * @param level 設定するレベル
   */
  private void attachLogAppender(final Level level) {
    entityBaseLogger = (Logger) LoggerFactory.getLogger(EntityBase.class);
    entityBaseLogger.setLevel(level);
    logAppender = new ListAppender<>();
    logAppender.start();
    entityBaseLogger.addAppender(logAppender);
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定したエンティティがテナントIDを保持することを確かめる。 全ての SQL のテナント隔離はこのテナントIDを基に行うためである。
       *
       * @given テナントIDが T001
       * @when TestEntity を T001 のテナントIDで生成する
       * @then T001 のテナントIDを返す
       */
      @Test
      void テナントIDがT001の場合_T001のテナントIDを返す() {
        assertThat(new TestEntity(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に例外を投げることを確かめる。 テナントIDの無いエンティティはテナント隔離の条件を付けられない。
       *
       * @given テナントIDが null
       * @when TestEntity を null のテナントIDで生成する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TestEntity(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("tenantId must not be null or empty");
      }

      /**
       * テナントIDが空文字の場合に例外を投げることを確かめる。 空のテナントIDは他テナントの行を区別できないため受け付けない。
       *
       * @given テナントIDが空文字
       * @when TestEntity を空文字のテナントIDで生成する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TestEntity("")).isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが空白だけの場合に例外を投げることを確かめる。 空白だけのテナントIDは実質的に無いため受け付けない。
       *
       * @given テナントIDが半角スペース1文字
       * @when TestEntity を空白のテナントIDで生成する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDが空白だけの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TestEntity(" ")).isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class compareTo {

    @Nested
    class 正常系 {

      /**
       * 両方の登録日時が古い順に並ぶことを確かめる。 並び替えの基準は登録日時であるためである。
       *
       * @given 自分の登録日時が 2026年1月15日、相手の登録日時が 2026年1月16日
       * @when compareTo を相手のエンティティで実行する
       * @then 負の値を返す
       */
      @Test
      void 自分の登録日時が相手より古い場合_負の値を返す() {
        TestEntity older = new TestEntity(TENANT_ID, new OriginalDateTime(2026, 1, 15, 0, 0, 0));
        TestEntity newer = new TestEntity(TENANT_ID, new OriginalDateTime(2026, 1, 16, 0, 0, 0));

        assertThat(older.compareTo(newer)).isNegative();
      }

      /**
       * 自分の登録日時が相手より新しい場合に正の値を返すことを確かめる。 並び替えで新しい登録日時を後ろに並べるためである。
       *
       * @given 自分の登録日時が 2026年1月16日、相手の登録日時が 2026年1月15日
       * @when compareTo を相手のエンティティで実行する
       * @then 正の値を返す
       */
      @Test
      void 自分の登録日時が相手より新しい場合_正の値を返す() {
        TestEntity newer = new TestEntity(TENANT_ID, new OriginalDateTime(2026, 1, 16, 0, 0, 0));
        TestEntity older = new TestEntity(TENANT_ID, new OriginalDateTime(2026, 1, 15, 0, 0, 0));

        assertThat(newer.compareTo(older)).isPositive();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 両方の登録日時が無い場合に 0 を返すことを確かめる。 登録日時が無い行同士は順序を決められないため同順位とする。
       *
       * @given 自分と相手の登録日時が null
       * @when compareTo を相手のエンティティで実行する
       * @then 0 を返す
       */
      @Test
      void 両方の登録日時が無い場合_0を返す() {
        TestEntity self = new TestEntity(TENANT_ID);
        TestEntity other = new TestEntity(TENANT_ID);

        assertThat(self.compareTo(other)).isZero();
      }

      /**
       * 相手が null で自分の登録日時も無い場合に 0 を返すことを確かめる。 null の相手と登録日時が無い自分は同順位として扱うためである。
       *
       * @given 自分の登録日時が null、相手が null
       * @when compareTo を null で実行する
       * @then 0 を返す
       */
      @Test
      void 相手がnullで自分の登録日時も無い場合_0を返す() {
        TestEntity self = new TestEntity(TENANT_ID);

        assertThat(self.compareTo(null)).isZero();
      }

      /**
       * 相手が null で自分の登録日時がある場合に正の値を返すことを確かめる。 登録日時が有る行を、無い相手より後ろに並べるためである。
       *
       * @given 自分の登録日時が 2026年1月15日、相手が null
       * @when compareTo を null で実行する
       * @then 正の値を返す
       */
      @Test
      void 相手がnullで自分の登録日時がある場合_正の値を返す() {
        TestEntity self = new TestEntity(TENANT_ID, new OriginalDateTime(2026, 1, 15, 0, 0, 0));

        assertThat(self.compareTo(null)).isPositive();
      }

      /**
       * 自分の登録日時が無く相手の登録日時が無い場合に 0 を返すことを確かめる。 相手の登録日時が無いときは、自分が無い場合と同じく同順位として扱うためである。
       *
       * @given 自分の登録日時が null、相手の登録日時が null
       * @when compareTo を登録日時の無い相手のエンティティで実行する
       * @then 0 を返す
       */
      @Test
      void 相手の登録日時が無い場合の自分の登録日時も無い場合_0を返す() {
        TestEntity self = new TestEntity(TENANT_ID);
        TestEntity other = new TestEntity(TENANT_ID);

        assertThat(self.compareTo(other)).isZero();
      }

      /**
       * 自分の登録日時が無く相手の登録日時がある場合に負の値を返すことを確かめる。 登録日時が無い行を、有る相手より前に並べるためである。
       *
       * @given 自分の登録日時が null、相手の登録日時が 2026年1月15日
       * @when compareTo を相手のエンティティで実行する
       * @then 負の値を返す
       */
      @Test
      void 自分の登録日時が無く相手の登録日時がある場合_負の値を返す() {
        TestEntity self = new TestEntity(TENANT_ID);
        TestEntity other = new TestEntity(TENANT_ID, new OriginalDateTime(2026, 1, 15, 0, 0, 0));

        assertThat(self.compareTo(other)).isNegative();
      }

      /**
       * 自分の登録日時がある場合に相手の登録日時が無いと正の値を返すことを確かめる。 登録日時が有る行を、無い相手より後ろに並べるためである。
       *
       * @given 自分の登録日時が 2026年1月15日、相手の登録日時が null
       * @when compareTo を登録日時の無い相手のエンティティで実行する
       * @then 正の値を返す
       */
      @Test
      void 相手の登録日時が無い場合の自分の登録日時がある場合_正の値を返す() {
        TestEntity self = new TestEntity(TENANT_ID, new OriginalDateTime(2026, 1, 15, 0, 0, 0));
        TestEntity other = new TestEntity(TENANT_ID);

        assertThat(self.compareTo(other)).isPositive();
      }
    }
  }

  @Nested
  class executeInsert {

    @Nested
    class 正常系 {

      /**
       * テナントIDの列と値を追加したINSERT文を実行することを確かめる。 登録する行は必ず自分のテナントに属させるためである。
       *
       * @given 接続のモックが更新件数 1 件を返し、INSERT 文は列が 2 つ、値が 2 つ
       * @when executeInsert をテナントIDが T001 で実行する
       * @then テナントIDの列を追加したINSERT文で準備し、テナントIDを3番目に設定して 1 を返す
       */
      @Test
      void テナントIDがT001の場合_テナントIDを追加したINSERT文を実行して1を返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        EntityBase entity = new TestEntity(TENANT_ID);

        int result =
            entity.executeInsert(
                connection,
                "INSERT INTO SES_AI_M_TENANT (a, b) VALUES (?, ?)",
                TENANT_ID,
                stmt -> {
                  stmt.setString(1, "A");
                  stmt.setString(2, "B");
                },
                "insert");

        verify(connection)
            .prepareStatement("INSERT INTO SES_AI_M_TENANT (a, b, tenant_id) VALUES (?, ?, ?)");
        verify(statement).setString(1, "A");
        verify(statement).setString(2, "B");
        verify(statement).setString(3, TENANT_ID);
        assertThat(result).isEqualTo(1);
      }
    }

    @Nested
    class 異常系 {

      /**
       * VALUES 句の無いSQLの場合に例外を投げることを確かめる。 テナントIDの列を追加する位置を決められないため、準備文を作らずに止める。
       *
       * @given INSERT 文に VALUES 句が無い
       * @when executeInsert を VALUES 句の無い SQL で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void VALUES句が無い場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () ->
                    new TestEntity(TENANT_ID)
                        .executeInsert(
                            connection, "INSERT INTO T (a)", TENANT_ID, stmt -> {}, "insert"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("INSERT SQL must contain VALUES clause");
        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 列の一覧が括弧で囲まれていない場合に例外を投げることを確かめる。 括弧の位置を決められないと tenant_id の列を追加できないためである。
       *
       * @given INSERT 文の列の一覧に括弧が無い
       * @when executeInsert を列の括弧が無い SQL で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void 列の一覧に括弧が無い場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () ->
                    new TestEntity(TENANT_ID)
                        .executeInsert(
                            connection,
                            "INSERT INTO T VALUES (?)",
                            TENANT_ID,
                            stmt -> {},
                            "insert"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Column list must be in parentheses");
        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * VALUES 句の値の一覧が括弧で囲まれていない場合に例外を投げることを確かめる。 値の末尾に ? を追加する位置を決められないため、準備文を作らずに止める。
       *
       * @given INSERT 文の VALUES 句に括弧が無い
       * @when executeInsert を VALUES 句の括弧が無い SQL で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void VALUES句の値の一覧に括弧が無い場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () ->
                    new TestEntity(TENANT_ID)
                        .executeInsert(
                            connection,
                            "INSERT INTO T (a) VALUES ?",
                            TENANT_ID,
                            stmt -> {},
                            "insert"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("VALUES clause must have parentheses");
        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 準備文の作成に失敗した場合に例外をそのまま投げることを確かめる。 失敗を握りつぶすと呼び出し元が登録失敗に気付けないためである。
       *
       * @given 接続が SQLException を投げる
       * @when executeInsert を実行する
       * @then SQLException を投げる
       */
      @Test
      void 準備文の作成に失敗した場合_SQLExceptionを投げる() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("接続失敗"));

        assertThatThrownBy(
                () ->
                    new TestEntity(TENANT_ID)
                        .executeInsert(
                            connection,
                            "INSERT INTO T (a) VALUES (?)",
                            TENANT_ID,
                            stmt -> stmt.setString(1, "A"),
                            "insert"))
            .isInstanceOf(SQLException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に 0 を返しSQLを実行しないことを確かめる。 接続が無い状態で準備文を作ると例外になるため、例外を投げて処理を止める。
       *
       * @given 接続が null
       * @when executeInsert を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeInsert(
                          null,
                          "INSERT INTO T (a) VALUES (?)",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "A"),
                          "insert");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが null の場合に 0 を返しSQLを実行しないことを確かめる。 テナントIDが無い挿入は行わないため、例外を投げて処理を止める。
       *
       * @given テナントIDが null
       * @when executeInsert をテナントID null で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeInsert(
                          null,
                          "INSERT INTO T (a) VALUES (?)",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "A"),
                          "insert");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが空文字の場合に 0 を返しSQLを実行しないことを確かめる。 空のテナントIDでは挿入しないため、例外を投げて処理を止める。
       *
       * @given テナントIDが空文字
       * @when executeInsert をテナントIDが空文字で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeInsert(
                          null,
                          "INSERT INTO T (a) VALUES (?)",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "A"),
                          "insert");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 挿入する行のテナントIDが指定したテナントIDになることを確かめる。 他テナントの行として登録されると、そのテナントのデータを汚すためである。
       *
       * @given テナントIDが T002、列が 1 つ、値が 1 つ
       * @when executeInsert をテナントIDが T002 で実行する
       * @then テナントIDの値に T002 を設定する
       */
      @Test
      void テナントIDがT002の場合_1を返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);

        new TestEntity(TENANT_ID)
            .executeInsert(
                connection,
                "INSERT INTO T (a) VALUES (?)",
                "T002",
                stmt -> stmt.setString(1, "A"),
                "insert");

        verify(statement).setString(2, "T002");
      }
    }
  }

  @Nested
  class executeInsertWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントIDを付けずにINSERT文を実行することを確かめる。 マスタテーブルはテナントに属さない行を登録するため、SQL を変えずに実行する。
       *
       * @given 接続のモックが更新件数 1 件を返し、INSERT 文は値が 1 つ
       * @when executeInsertWithoutTenantFilter を実行する
       * @then 元のSQLで準備し、値を1番目に設定して 1 を返す
       */
      @Test
      void マスタ用のINSERT文の場合_元のSQLで実行して1を返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);

        int result =
            new TestEntity(TENANT_ID)
                .executeInsertWithoutTenantFilter(
                    connection,
                    "INSERT INTO SES_AI_M_TENANT (tenant_name) VALUES (?)",
                    stmt -> stmt.setString(1, "Copel"),
                    "insert");

        verify(connection).prepareStatement("INSERT INTO SES_AI_M_TENANT (tenant_name) VALUES (?)");
        verify(statement).setString(1, "Copel");
        assertThat(result).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では準備文を作れないため、例外を投げて処理を止める。
       *
       * @given 接続が null
       * @when executeInsertWithoutTenantFilter を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeInsertWithoutTenantFilter(
                          null,
                          "INSERT INTO T (a) VALUES (?)",
                          stmt -> stmt.setString(1, "A"),
                          "insert");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class executeSelectByPk {

    @Nested
    class 正常系 {

      /**
       * 該当する行があれば結果をマッピングして true を返すことを確かめる。 主キーで取得した行の値を呼び出し元へ渡すためである。
       *
       * @given 接続のモックが1行を返し、SELECT 文の条件は主キーが 1 つ
       * @when executeSelectByPk を主キーとテナントID T001 で実行する
       * @then マッピング処理を1回実行し、true を返す
       */
      @Test
      void 行が1件ある場合_マッピングしてtrueを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        EntityBase.ResultSetMapper mapper = mock(EntityBase.ResultSetMapper.class);

        boolean found =
            new TestEntity(TENANT_ID)
                .executeSelectByPk(
                    connection,
                    "SELECT id FROM T WHERE id = ?",
                    TENANT_ID,
                    stmt -> stmt.setString(1, "K001"),
                    mapper,
                    "select");

        verify(mapper).map(resultSet);
        assertThat(found).isTrue();
      }

      /**
       * 複数の主キーの場合にテナントIDの条件を元のSQLの後ろに付けることを確かめる。 主キーの値の後ろにテナントIDを並べ、値の取り違えを防ぐためである。
       *
       * @given SELECT 文の条件は主キーが 2 つ
       * @when executeSelectByPk を主キーの値 2 つとテナントID T001 で実行する
       * @then テナントIDの条件を付けたSQLで準備し、テナントIDを3番目に設定する
       */
      @Test
      void 主キーが2つの場合_falseを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        new TestEntity(TENANT_ID)
            .executeSelectByPk(
                connection,
                "SELECT id FROM T WHERE a = ? AND b = ?",
                TENANT_ID,
                stmt -> {
                  stmt.setString(1, "A1");
                  stmt.setString(2, "B1");
                },
                mock(EntityBase.ResultSetMapper.class),
                "select");

        verify(connection)
            .prepareStatement("SELECT id FROM T WHERE a = ? AND b = ? AND tenant_id = ?");
        verify(statement).setString(3, TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 問い合わせに失敗した場合に SQLException を投げることを確かめる。 失敗を握りつぶすと、存在しない行として扱われてしまうためである。
       *
       * @given 準備文の実行が SQLException を投げる
       * @when executeSelectByPk を実行する
       * @then SQLException を投げる
       */
      @Test
      void 問い合わせに失敗した場合_SQLExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeQuery()).thenThrow(new SQLException("問い合わせ失敗"));

        assertThatThrownBy(
                () ->
                    new TestEntity(TENANT_ID)
                        .executeSelectByPk(
                            connection,
                            "SELECT id FROM T WHERE id = ?",
                            TENANT_ID,
                            stmt -> stmt.setString(1, "K001"),
                            mock(EntityBase.ResultSetMapper.class),
                            "select"))
            .isInstanceOf(SQLException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 該当する行が無い場合に false を返しマッピングを行わないことを確かめる。 行が無いときに空の結果を変換すると、存在しない行を扱ってしまうためである。
       *
       * @given 接続のモックが0行を返す
       * @when executeSelectByPk を主キーとテナントID T001 で実行する
       * @then false を返し、マッピング処理を呼ばない
       */
      @Test
      void 行が0件の場合_falseを返しマッピングを呼ばない() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        EntityBase.ResultSetMapper mapper = mock(EntityBase.ResultSetMapper.class);

        boolean found =
            new TestEntity(TENANT_ID)
                .executeSelectByPk(
                    connection,
                    "SELECT id FROM T WHERE id = ?",
                    TENANT_ID,
                    stmt -> stmt.setString(1, "K001"),
                    mapper,
                    "select");

        assertThat(found).isFalse();
        verify(mapper, never()).map(resultSet);
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では問い合わせできないため、例外を投げて処理を止める。
       *
       * @given 接続が null
       * @when executeSelectByPk を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeSelectByPk(
                          null,
                          "SELECT id FROM T WHERE id = ?",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "K001"),
                          mock(EntityBase.ResultSetMapper.class),
                          "select");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが空文字の場合に false を返し問い合わせを行わないことを確かめる。 テナントIDの無い問い合わせは全テナントの行を対象にしてしまうため、行わない。
       *
       * @given テナントIDが空文字
       * @when executeSelectByPk をテナントIDが空文字で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeSelectByPk(
                          null,
                          "SELECT id FROM T WHERE id = ?",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "K001"),
                          mock(EntityBase.ResultSetMapper.class),
                          "select");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが null の場合に false を返し問い合わせを行わないことを確かめる。 テナントIDの無い問い合わせは全テナントの行を対象にしてしまうため、行わない。
       *
       * @given テナントIDが null
       * @when executeSelectByPk をテナントIDが null で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeSelectByPk(
                          null,
                          "SELECT id FROM T WHERE id = ?",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "K001"),
                          mock(EntityBase.ResultSetMapper.class),
                          "select");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 結合を含む SELECT 文では結合先の ON 句と主テーブルの WHERE 句にテナントIDの条件を付けることを確かめる。
       * 結合先の行が他テナントの場合に、その行を結合させないためである。
       *
       * @given SELECT 文が先頭テーブル p と結合テーブル s を INNER JOIN で結合している
       * @when executeSelectByPk をテナントID T001 で実行する
       * @then 結合先の ON 句と主テーブルの WHERE 句にテナント条件を付けたSQLで準備する
       */
      @Test
      void 結合を含む場合_falseを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        boolean found =
            new TestEntity(TENANT_ID)
                .executeSelectByPk(
                    connection,
                    "SELECT p.id FROM T p INNER JOIN S s ON s.id = p.id WHERE p.id = ?",
                    TENANT_ID,
                    stmt -> stmt.setString(1, "K001"),
                    mock(EntityBase.ResultSetMapper.class),
                    "select");

        verify(connection)
            .prepareStatement(
                "SELECT p.id FROM T p INNER JOIN S s ON (s.id = p.id) AND s.tenant_id = p.tenant_id WHERE p.id = ? AND p.tenant_id = ?");
        assertThat(found).isFalse();
      }
    }
  }

  @Nested
  class executeSelectByPkWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントIDの条件を付けずに問い合わせて、行があれば true を返すことを確かめる。 マスタテーブルの取得は全テナント共通の行を対象にするため、SQL を変えない。
       *
       * @given 接続のモックが1行を返す
       * @when executeSelectByPkWithoutTenantFilter を実行する
       * @then 元のSQLで準備し、マッピング処理を1回実行して true を返す
       */
      @Test
      void マスタの取得の場合_元のSQLで問い合わせて行があればtrueを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        EntityBase.ResultSetMapper mapper = mock(EntityBase.ResultSetMapper.class);

        boolean found =
            new TestEntity(TENANT_ID)
                .executeSelectByPkWithoutTenantFilter(
                    connection,
                    "SELECT id FROM M WHERE id = ?",
                    stmt -> stmt.setString(1, "K001"),
                    mapper,
                    "select");

        verify(connection).prepareStatement("SELECT id FROM M WHERE id = ?");
        verify(mapper).map(resultSet);
        assertThat(found).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 行が無い場合に false を返しマッピングを呼ばないことを確かめる。 行が無いときは変換しないためである。
       *
       * @given 接続のモックが0行を返す
       * @when executeSelectByPkWithoutTenantFilter を実行する
       * @then false を返し、マッピング処理を呼ばない
       */
      @Test
      void 行が0件の場合_falseを返しマッピングを呼ばない() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        EntityBase.ResultSetMapper mapper = mock(EntityBase.ResultSetMapper.class);

        boolean found =
            new TestEntity(TENANT_ID)
                .executeSelectByPkWithoutTenantFilter(
                    connection, "SELECT id FROM M", stmt -> {}, mapper, "select");

        assertThat(found).isFalse();
        verify(mapper, never()).map(resultSet);
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では問い合わせできないためである。
       *
       * @given 接続が null
       * @when executeSelectByPkWithoutTenantFilter を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeSelectByPkWithoutTenantFilter(
                          null,
                          "SELECT id FROM M",
                          stmt -> {},
                          mock(EntityBase.ResultSetMapper.class),
                          "select");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class executeExists {

    @Nested
    class 正常系 {

      /**
       * EXISTS 文の結果が true なら true を返すことを確かめる。 存在判定の結果をそのまま呼び出し元へ返すためである。
       *
       * @given 接続のモックが1行を返し、その行の1列目が true
       * @when executeExists をテナントID T001 で実行する
       * @then true を返す
       */
      @Test
      void 存在する場合_trueを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(true);

        boolean exists =
            new TestEntity(TENANT_ID)
                .executeExists(
                    connection,
                    "SELECT EXISTS(SELECT 1 FROM T WHERE id = ?)",
                    TENANT_ID,
                    stmt -> stmt.setString(1, "K001"),
                    "exists");

        assertThat(exists).isTrue();
      }

      /**
       * 括弧内のWHERE句にテナントIDの条件を挿入することを確かめる。 存在確認も他テナントの行を見つけないように、括弧の中で絞り込むためである。
       *
       * @given SELECT EXISTS 文の括弧内に WHERE 句が1つある
       * @when executeExists をテナントID T001 で実行する
       * @then 括弧の中にテナントIDの条件を付けたSQLで準備し、テナントIDを2番目に設定する
       */
      @Test
      void 括弧内にWHERE句がある場合_falseを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        new TestEntity(TENANT_ID)
            .executeExists(
                connection,
                "SELECT EXISTS(SELECT 1 FROM T WHERE id = ?)",
                TENANT_ID,
                stmt -> stmt.setString(1, "K001"),
                "exists");

        verify(connection)
            .prepareStatement("SELECT EXISTS(SELECT 1 FROM T WHERE id = ? AND tenant_id = ?)");
        verify(statement).setString(2, TENANT_ID);
      }
    }

    @Nested
    class 境界値 {

      /**
       * テナントIDが空文字の場合に false を返し準備文を作らないことを確かめる。 空のテナントIDの存在確認は全テナントの行を対象にしてしまうため、行わない。
       *
       * @given テナントIDが空文字
       * @when executeExists をテナントIDが空文字で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeExists(
                          null,
                          "SELECT EXISTS(SELECT 1 FROM T WHERE id = ?)",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "K001"),
                          "exists");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 結果の行が無い場合に false を返すことを確かめる。 行が無いときは存在しないと判定するためである。
       *
       * @given 接続のモックが0行を返す
       * @when executeExists をテナントID T001 で実行する
       * @then false を返す
       */
      @Test
      void 行が0件の場合_falseを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        boolean exists =
            new TestEntity(TENANT_ID)
                .executeExists(
                    connection,
                    "SELECT EXISTS(SELECT 1 FROM T WHERE id = ?)",
                    TENANT_ID,
                    stmt -> stmt.setString(1, "K001"),
                    "exists");

        assertThat(exists).isFalse();
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では存在確認できないためである。
       *
       * @given 接続が null
       * @when executeExists を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeExists(
                          null,
                          "SELECT EXISTS(SELECT 1 FROM T WHERE id = ?)",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "K001"),
                          "exists");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが null の場合に例外を投げ準備文を作らないことを確かめる。 テナントIDの無い存在確認は全テナントを対象にしてしまうためである。
       *
       * @given テナントIDが null
       * @when executeExists をテナントID null で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeExists(
                          null,
                          "SELECT EXISTS(SELECT 1 FROM T WHERE id = ?)",
                          TENANT_ID,
                          stmt -> stmt.setString(1, "K001"),
                          "exists");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class executeUpdateByPk {

    @Nested
    class 正常系 {

      /**
       * 1件以上更新した場合に true を返し、テナントIDを条件の後ろに設定することを確かめる。 他テナントの行を更新しないように、テナントIDの条件を付けて更新するためである。
       *
       * @given SET 句が1つ、WHERE 句の主キーが1つ、更新件数が 1 件
       * @when executeUpdateByPk をテナントID T001 で実行する
       * @then テナントIDの条件を付けたSQLで準備し、テナントIDを3番目に設定して true を返す
       */
      @Test
      void 更新件数が1件の場合_テナントIDを3番目に設定してtrueを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeUpdate()).thenReturn(1);

        boolean updated =
            new TestEntity(TENANT_ID)
                .executeUpdateByPk(
                    connection,
                    "UPDATE T SET a = ? WHERE id = ?",
                    TENANT_ID,
                    stmt -> {
                      stmt.setString(1, "A1");
                      stmt.setString(2, "K001");
                    },
                    "update");

        verify(connection).prepareStatement("UPDATE T SET a = ? WHERE id = ? AND tenant_id = ?");
        verify(statement).setString(3, TENANT_ID);
        assertThat(updated).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * テナントIDが null の場合に例外を投げ準備文を作らないことを確かめる。 テナントIDの無い更新は全テナントの行を対象にしてしまうため、行わない。
       *
       * @given テナントIDが null
       * @when executeUpdateByPk をテナントID null で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeUpdateByPk(
                          null, "UPDATE T SET a = ? WHERE id = ?", null, stmt -> {}, "update");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象の行が無い、または他テナントの行のため、更新されなかったと判定する。
       *
       * @given 更新件数が 0 件
       * @when executeUpdateByPk をテナントID T001 で実行する
       * @then false を返す
       */
      @Test
      void 更新件数が0件の場合_falseを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeUpdate()).thenReturn(0);

        boolean updated =
            new TestEntity(TENANT_ID)
                .executeUpdateByPk(
                    connection,
                    "UPDATE T SET a = ? WHERE id = ?",
                    TENANT_ID,
                    stmt -> stmt.setString(1, "A1"),
                    "update");

        assertThat(updated).isFalse();
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では更新できないためである。
       *
       * @given 接続が null
       * @when executeUpdateByPk を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeUpdateByPk(
                          null, "UPDATE T SET a = ? WHERE id = ?", TENANT_ID, stmt -> {}, "update");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが空文字の場合に false を返し準備文を作らないことを確かめる。 テナントIDの無い更新は全テナントの行を対象にしてしまうため、行わない。
       *
       * @given テナントIDが空文字
       * @when executeUpdateByPk をテナントIDが空文字で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeUpdateByPk(
                          null, "UPDATE T SET a = ? WHERE id = ?", "", stmt -> {}, "update");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 更新する行のテナントIDの条件に指定したテナントIDを設定することを確かめる。 他テナントの行を更新すると、そのテナントのデータを壊すためである。
       *
       * @given テナントIDが T002、WHERE 句の主キーが1つ
       * @when executeUpdateByPk をテナントID T002 で実行する
       * @then テナントIDの値に T002 を設定する
       */
      @Test
      void テナントIDがT002の場合_trueを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);

        new TestEntity(TENANT_ID)
            .executeUpdateByPk(
                connection,
                "UPDATE T SET a = ? WHERE id = ?",
                "T002",
                stmt -> {
                  stmt.setString(1, "A1");
                  stmt.setString(2, "K001");
                },
                "update");

        verify(statement).setString(3, "T002");
      }
    }
  }

  @Nested
  class executeDeleteByPk {

    @Nested
    class 正常系 {

      /**
       * 1件以上削除した場合に true を返すことを確かめる。 削除できたかどうかを呼び出し元へ返すためである。
       *
       * @given WHERE 句の主キーが1つ、削除件数が 1 件
       * @when executeDeleteByPk をテナントID T001 で実行する
       * @then テナントIDの条件を付けたSQLで準備し、テナントIDを2番目に設定して true を返す
       */
      @Test
      void 削除件数が1件の場合_テナントIDを2番目に設定してtrueを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeUpdate()).thenReturn(1);

        boolean deleted =
            new TestEntity(TENANT_ID)
                .executeDeleteByPk(
                    connection,
                    "DELETE FROM T WHERE id = ?",
                    TENANT_ID,
                    stmt -> stmt.setString(1, "K001"),
                    "delete");

        verify(connection).prepareStatement("DELETE FROM T WHERE id = ? AND tenant_id = ?");
        verify(statement).setString(2, TENANT_ID);
        assertThat(deleted).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * テナントIDが空文字の場合に false を返し準備文を作らないことを確かめる。 空のテナントIDの削除は全テナントの行を対象にしてしまうため、行わない。
       *
       * @given テナントIDが空文字
       * @when executeDeleteByPk をテナントIDが空文字で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeDeleteByPk(
                          null, "DELETE FROM T WHERE id = ?", "", stmt -> {}, "delete");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象の行が無い、または他テナントの行のため、削除されなかったと判定する。
       *
       * @given 削除件数が 0 件
       * @when executeDeleteByPk をテナントID T001 で実行する
       * @then false を返す
       */
      @Test
      void 削除件数が0件の場合_falseを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeUpdate()).thenReturn(0);

        boolean deleted =
            new TestEntity(TENANT_ID)
                .executeDeleteByPk(
                    connection,
                    "DELETE FROM T WHERE id = ?",
                    TENANT_ID,
                    stmt -> stmt.setString(1, "K001"),
                    "delete");

        assertThat(deleted).isFalse();
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では削除できないためである。
       *
       * @given 接続が null
       * @when executeDeleteByPk を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeDeleteByPk(
                          null, "DELETE FROM T WHERE id = ?", TENANT_ID, stmt -> {}, "delete");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが null の場合に例外を投げ準備文を作らないことを確かめる。 テナントIDの無い削除は全テナントの行を対象にしてしまうため、行わない。
       *
       * @given テナントIDが null
       * @when executeDeleteByPk をテナントID null で実行する
       * @then IllegalArgumentException を投げ、準備文を作らない
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        Connection connection = mockConnection();

        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeDeleteByPk(
                          null, "DELETE FROM T WHERE id = ?", null, stmt -> {}, "delete");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class executeUpdateByPkWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントIDの条件を付けずに更新し、1件以上なら true を返すことを確かめる。 マスタテーブルの更新は全テナント共通の行を対象にするため、SQL を変えない。
       *
       * @given 更新件数が 1 件
       * @when executeUpdateByPkWithoutTenantFilter を実行する
       * @then 元のSQLで準備し、true を返す
       */
      @Test
      void マスタの更新の場合_元のSQLで更新してtrueを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeUpdate()).thenReturn(1);

        boolean updated =
            new TestEntity(TENANT_ID)
                .executeUpdateByPkWithoutTenantFilter(
                    connection,
                    "UPDATE M SET a = ? WHERE id = ?",
                    stmt -> stmt.setString(1, "A1"),
                    "update");

        verify(connection).prepareStatement("UPDATE M SET a = ? WHERE id = ?");
        assertThat(updated).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象の行が無いため、更新されなかったと判定する。
       *
       * @given 更新件数が 0 件
       * @when executeUpdateByPkWithoutTenantFilter を実行する
       * @then false を返す
       */
      @Test
      void 更新件数が0件の場合_falseを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeUpdate()).thenReturn(0);

        boolean updated =
            new TestEntity(TENANT_ID)
                .executeUpdateByPkWithoutTenantFilter(
                    connection,
                    "UPDATE M SET a = ? WHERE id = ?",
                    stmt -> stmt.setString(1, "A1"),
                    "update");

        assertThat(updated).isFalse();
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では更新できないためである。
       *
       * @given 接続が null
       * @when executeUpdateByPkWithoutTenantFilter を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeUpdateByPkWithoutTenantFilter(
                          null, "UPDATE M SET a = ?", stmt -> {}, "update");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class executeDeleteByPkWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントIDの条件を付けずに削除し、1件以上なら true を返すことを確かめる。 マスタテーブルの削除は全テナント共通の行を対象にするため、SQL を変えない。
       *
       * @given 削除件数が 1 件
       * @when executeDeleteByPkWithoutTenantFilter を実行する
       * @then 元のSQLで準備し、true を返す
       */
      @Test
      void マスタの削除の場合_元のSQLで削除してtrueを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeUpdate()).thenReturn(1);

        boolean deleted =
            new TestEntity(TENANT_ID)
                .executeDeleteByPkWithoutTenantFilter(
                    connection,
                    "DELETE FROM M WHERE id = ?",
                    stmt -> stmt.setString(1, "K001"),
                    "delete");

        verify(connection).prepareStatement("DELETE FROM M WHERE id = ?");
        assertThat(deleted).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象の行が無いため、削除されなかったと判定する。
       *
       * @given 削除件数が 0 件
       * @when executeDeleteByPkWithoutTenantFilter を実行する
       * @then false を返す
       */
      @Test
      void 削除件数が0件の場合_falseを返す() throws SQLException {
        Connection connection = mockConnection();
        PreparedStatement statement = statementOf(connection);
        when(statement.executeUpdate()).thenReturn(0);

        boolean deleted =
            new TestEntity(TENANT_ID)
                .executeDeleteByPkWithoutTenantFilter(
                    connection,
                    "DELETE FROM M WHERE id = ?",
                    stmt -> stmt.setString(1, "K001"),
                    "delete");

        assertThat(deleted).isFalse();
      }

      /**
       * 接続が null の場合に例外を投げることを確かめる。 接続が無い状態では削除できないためである。
       *
       * @given 接続が null
       * @when executeDeleteByPkWithoutTenantFilter を接続 null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> {
                  new TestEntity(TENANT_ID)
                      .executeDeleteByPkWithoutTenantFilter(
                          null, "DELETE FROM M WHERE id = ?", stmt -> {}, "delete");
                })
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class addTenantIdFilter {

    @Nested
    class 正常系 {

      /**
       * WHERE 句の無いSELECT文にテナントIDの WHERE 句を付けることを確かめる。 テナントIDの条件を必ず付けて、他テナントの行を返さないためである。
       *
       * @given 基本SQLが "SELECT id FROM T" の形で WHERE 句が無い
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 末尾に WHERE tenant_id の条件を付けたSQLを返す
       */
      @Test
      void WHERE句が無い場合_末尾にWHERE句でテナントIDの条件を付けたSQLを返す() {
        String sql = new TestEntity(TENANT_ID).addTenantIdFilter("SELECT id FROM T", TENANT_ID);

        assertThat(sql).isEqualTo("SELECT id FROM T WHERE tenant_id = ?");
      }

      /**
       * WHERE 句のあるSQLにテナントIDの AND 条件を付けることを確かめる。 既存の絞り込み条件を残したまま、テナントIDの条件を加えるためである。
       *
       * @given 基本SQLが WHERE 句を1つ持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 末尾に AND tenant_id の条件を付けたSQLを返す
       */
      @Test
      void WHERE句がある場合_末尾にAND条件でテナントIDの条件を付けたSQLを返す() {
        String sql =
            new TestEntity(TENANT_ID).addTenantIdFilter("SELECT id FROM T WHERE x = ?", TENANT_ID);

        assertThat(sql).isEqualTo("SELECT id FROM T WHERE x = ? AND tenant_id = ?");
      }

      /**
       * テーブルの別名を持つ FROM 句では、別名付きのテナントIDの条件を付けることを確かめる。 別名を付けた列を明示しないと、結合時に列名が曖昧になるためである。
       *
       * @given 基本SQLが FROM T t の形で別名 t を持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 別名 t のテナントIDの条件を付けたSQLを返す
       */
      @Test
      void テーブルの別名がある場合_別名付きのテナントIDの条件を付けたSQLを返す() {
        String sql =
            new TestEntity(TENANT_ID)
                .addTenantIdFilter("SELECT t.id FROM T t WHERE t.x = ?", TENANT_ID);

        assertThat(sql).isEqualTo("SELECT t.id FROM T t WHERE t.x = ? AND t.tenant_id = ?");
      }

      /**
       * UPDATE 文の WHERE 句にテナントIDの AND 条件を付けることを確かめる。 更新も他テナントの行を対象にしないように絞り込むためである。
       *
       * @given 基本SQLが UPDATE 文で WHERE 句を1つ持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 末尾に AND tenant_id の条件を付けたSQLを返す
       */
      @Test
      void UPDATE文の場合_末尾にAND条件でテナントIDの条件を付けたSQLを返す() {
        String sql =
            new TestEntity(TENANT_ID)
                .addTenantIdFilter("UPDATE T SET a = ? WHERE id = ?", TENANT_ID);

        assertThat(sql).isEqualTo("UPDATE T SET a = ? WHERE id = ? AND tenant_id = ?");
      }

      /**
       * ORDER BY を含むSQLでは、テナントIDの条件を ORDER BY 句の前に入れることを確かめる。 句の後ろに条件を置くと SQL の構文が壊れるためである。
       *
       * @given 基本SQLが ORDER BY を持つ FROM 句で WHERE 句が無い
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then WHERE 句を補い ORDER BY 句の前に置いたSQLを返す
       */
      @Test
      void ORDER句を含む場合_ORDER句の前にテナントIDの条件を付けたSQLを返す() {
        String sql =
            new TestEntity(TENANT_ID).addTenantIdFilter("SELECT id FROM T ORDER BY id", TENANT_ID);

        assertThat(sql).isEqualTo("SELECT id FROM T WHERE tenant_id = ? ORDER BY id");
      }
    }

    @Nested
    class 境界値 {

      /**
       * カンマ区切りの FROM 句では、2 番目以降の表の条件を WHERE 句の末尾に付けることを確かめる。 カンマ区切りの表には ON 句が無いため、WHERE
       * 句で結合先のテナントを揃えるためである。
       *
       * @given 基本SQLが FROM 句に T と U をカンマで並べ、WHERE 句を1つ持つ
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 2番目のテーブル名 U を別名とした条件を付けたSQLを返す
       */
      @Test
      void カンマ区切りのFROM句の場合_結合先の条件をWHERE句の末尾に付けたSQLを返す() {
        String sql =
            new TestEntity(TENANT_ID)
                .addTenantIdFilter("SELECT * FROM T, U WHERE x = ?", TENANT_ID);

        assertThat(sql)
            .isEqualTo(
                "SELECT * FROM T, U WHERE x = ? AND T.tenant_id = ? AND U.tenant_id = T.tenant_id");
      }

      /**
       * 予約語の ORDER が別名として扱われないことを確かめる。 予約語を別名と誤認すると、テナントIDの条件が存在しない別名に付くためである。
       *
       * @given 基本SQLが FROM T の直後に ORDER BY が続く
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 別名を付けずに tenant_id の条件を付けたSQLを返す
       */
      @Test
      void 予約語のORDERが続く場合_別名を付けないSQLを返す() {
        String sql =
            new TestEntity(TENANT_ID).addTenantIdFilter("SELECT id FROM T ORDER BY id", TENANT_ID);

        assertThat(sql).doesNotContain(".tenant_id");
      }

      /**
       * テナントIDが null の場合に例外を投げることを確かめる。 テナントIDの無い条件を付けると、全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが null
       * @when addTenantIdFilter をテナントID null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> new TestEntity(TENANT_ID).addTenantIdFilter("SELECT id FROM T", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TenantId must not be null or empty");
      }

      /**
       * テナントIDが空文字の場合に例外を投げることを確かめる。 空のテナントIDの条件は全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが空文字
       * @when addTenantIdFilter をテナントIDが空文字で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> new TestEntity(TENANT_ID).addTenantIdFilter("SELECT id FROM T", ""))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 基本SQLが null の場合に例外を投げることを確かめる。 条件を付ける対象が無いため、続けると NullPointerException になるところを先に止める。
       *
       * @given 基本SQLが null
       * @when addTenantIdFilter を基本SQL null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 基本SQLがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TestEntity(TENANT_ID).addTenantIdFilter(null, TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("baseSql must not be null");
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 結合を含む SQL では、結合先の表にも主テーブルと同じテナントの条件を付けることを確かめる。 結合先の行が他テナントの場合に、その行を結合させないためである。
       *
       * @given 基本SQLが先頭テーブル p と結合テーブル s を INNER JOIN で結合する
       * @when addTenantIdFilter をテナントID T001 で実行する
       * @then 先頭テーブル p の条件だけを付けたSQLを返す
       */
      @Test
      void 結合を含む場合_結合先と主テーブルの条件を付けたSQLを返す() {
        String sql =
            new TestEntity(TENANT_ID)
                .addTenantIdFilter(
                    "SELECT p.id FROM T p INNER JOIN S s ON s.id = p.id WHERE s.x = ?", TENANT_ID);

        assertThat(sql)
            .isEqualTo(
                "SELECT p.id FROM T p INNER JOIN S s ON (s.id = p.id) AND s.tenant_id = p.tenant_id WHERE s.x = ? AND p.tenant_id = ?");
      }
    }
  }

  @Nested
  class setTenantIdParameter {

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に例外を投げ、値を設定しないことを確かめる。 空のテナントIDを設定すると、全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが null
       * @when setTenantIdParameter をテナントID null で実行する
       * @then IllegalArgumentException を投げ、値を設定しない
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        assertThatThrownBy(() -> new TestEntity(TENANT_ID).setTenantIdParameter(statement, 1, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TenantId must not be null or empty");
        verify(statement, never()).setString(org.mockito.ArgumentMatchers.anyInt(), anyString());
      }

      /**
       * テナントIDが空文字の場合に例外を投げ、値を設定しないことを確かめる。 空のテナントIDは全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが空文字
       * @when setTenantIdParameter をテナントIDが空文字で実行する
       * @then IllegalArgumentException を投げ、値を設定しない
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);

        assertThatThrownBy(() -> new TestEntity(TENANT_ID).setTenantIdParameter(statement, 1, ""))
            .isInstanceOf(IllegalArgumentException.class);
        verify(statement, never()).setString(org.mockito.ArgumentMatchers.anyInt(), anyString());
      }
    }
  }

  @Nested
  class logSql {

    @Nested
    class 正常系 {

      /**
       * INFO ログが有効な場合に SQL とラベルをログへ出力することを確かめる。 実行される SQL を後から追えるようにするためである。
       *
       * @given INFO レベルのログが有効で、ログのアペンダーを付けている
       * @when logSql を SQL SELECT 1 とラベル select で実行する
       * @then ログに SQL とラベルを含む1件を出力する
       */
      @Test
      void INFOレベルが有効な場合_1件のログを送信する() {
        attachLogAppender(Level.INFO);
        List<ILoggingEvent> events = logAppender.list;

        new TestEntity(TENANT_ID).logSql("SELECT 1", "select");

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("[SQL] select - SELECT 1");
      }

      /**
       * INFO ログが無効な場合にログを出力しないことを確かめる。 ログの出力を止めた環境で、SQL の文字列を余分に扱わないためである。
       *
       * @given INFO より高いレベルのみ有効で、ログのアペンダーを付けている
       * @when logSql を SQL SELECT 1 で実行する
       * @then ログを出力しない
       */
      @Test
      void INFOレベルが無効な場合_何もしない() {
        attachLogAppender(Level.WARN);
        List<ILoggingEvent> events = logAppender.list;

        new TestEntity(TENANT_ID).logSql("SELECT 1", "select");

        assertThat(events).isEmpty();
      }
    }
  }

  @Nested
  class requireConnection {

    @Nested
    class 正常系 {

      /**
       * DBコネクションが有効な場合に例外を投げないことを確かめる。 有効な接続で問い合わせを続けるためである。
       *
       * @given DBコネクションが有効
       * @when requireConnection を有効な接続で実行する
       * @then 何もしない
       */
      @Test
      void 接続が有効な場合_何もしない() {
        Connection connection = mock(Connection.class);

        assertThatCode(() -> EntityBase.requireConnection(connection)).doesNotThrowAnyException();
      }
    }

    @Nested
    class 異常系 {

      /**
       * DBコネクションが null の場合に例外を投げることを確かめる。 接続が無いまま問い合わせると、件数0や未登録として誤認されるためである。
       *
       * @given DBコネクションが null
       * @when requireConnection を null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> EntityBase.requireConnection(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class requireTenantId {

    @Nested
    class 正常系 {

      /**
       * テナントIDが有効な場合に例外を投げないことを確かめる。 テナント隔離の条件を付けて問い合わせを続けるためである。
       *
       * @given テナントIDが T001
       * @when requireTenantId を T001 で実行する
       * @then 何もしない
       */
      @Test
      void テナントIDがT001の場合_何もしない() {
        assertThatCode(() -> EntityBase.requireTenantId("T001")).doesNotThrowAnyException();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に例外を投げることを確かめる。 テナントIDの無い問い合わせは全テナントの行を対象にしてしまうためである。
       *
       * @given テナントIDが null
       * @when requireTenantId を null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> EntityBase.requireTenantId(null))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが空文字の場合に例外を投げることを確かめる。 空のテナントIDは他テナントの行と区別できないためである。
       *
       * @given テナントIDが空文字
       * @when requireTenantId を空文字で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> EntityBase.requireTenantId(""))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }
}
