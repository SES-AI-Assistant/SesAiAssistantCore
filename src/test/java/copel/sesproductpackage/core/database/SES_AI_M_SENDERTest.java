package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_M_SENDER} の単体テスト。 */
class SES_AI_M_SENDERTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 送信者の値。 */
  private static final String FROM_ID = "S001";

  /** 登録日時の文字列。 */
  private static final String REGISTER_DATE_TEXT = "2026-01-15 10:00:00";

  /** テスト用の登録日時。 */
  private static final OriginalDateTime REGISTER_DATE = new OriginalDateTime(REGISTER_DATE_TEXT);

  /** 送信者の取得 SQL の結果の列。 */
  private static final String[] COLUMNS = {
    "from_id", "from_name", "register_date", "register_user"
  };

  /**
   * 準備文のモックを作り、接続の prepareStatement がその準備文を返すように設定する。
   *
   * @param connection 接続のモック
   * @param updated executeUpdate が返す更新件数
   * @return 準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static PreparedStatement stubUpdate(final Connection connection, final int updated)
      throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.executeUpdate()).thenReturn(updated);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    return statement;
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成するとテナントIDを保持することを確かめる。 テナントIDの無い項目は他テナントのデータと混ざる。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_M_SENDER を "T001" で生成する
       * @then テナントIDに "T001" を保持する
       */
      @Test
      void テナントIDが指定された場合_保持するテナントIDを返す() {
        assertThat(new SES_AI_M_SENDER(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントIDの無い項目を作ると後の検索で他テナントを対象にしてしまう。
       *
       * @given テナントIDが null
       * @when SES_AI_M_SENDER を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_M_SENDER(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * 送信者を登録するSQLにテナントIDの列と値を加えて実行することを確かめる。 テナントIDの無い登録は他テナントの行と区別できない。
       *
       * @given 送信者が "S001"、送信者名が "送信者A"、登録日時が "2026-01-15 10:00:00"、登録ユーザーが "U001"、更新件数が 1 件
       * @when insert を接続に対して実行する
       * @then tenant_id を含む INSERT 文を準備し、1 件を返す
       */
      @MockDbConnection
      @Test
      void 登録日時が設定されている場合_tenant_idを含めて1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);
        sender.setFromName("送信者A");
        sender.setRegisterDate(REGISTER_DATE);
        sender.setRegisterUser("U001");

        assertThat(sender.insert(connection)).isEqualTo(1);

        verify(connection)
            .prepareStatement(
                "INSERT INTO SES_AI_M_SENDER (from_id, from_name, register_date, register_user, tenant_id)"
                    + " VALUES (?, ?, ?, ?, ?)");
        verify(statement).setString(1, FROM_ID);
        verify(statement).setString(2, "送信者A");
        verify(statement).setTimestamp(3, REGISTER_DATE.toTimestamp());
        verify(statement).setString(4, "U001");
        verify(statement).setString(5, TENANT_ID);
      }

      /**
       * 登録日時が未設定でも日時にnullを設定して登録することを確かめる。 登録日時は必須の列ではないため、未設定の項目も登録できる必要がある。
       *
       * @given 送信者が "S001"、登録日時が未設定、更新件数が 1 件
       * @when insert を接続に対して実行する
       * @then 登録日時の位置に null を設定し、1 件を返す
       */
      @MockDbConnection
      @Test
      void 登録日時が未設定の場合_登録日時にnullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.insert(connection)).isEqualTo(1);

        verify(statement).setTimestamp(3, null);
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に登録を拒否することを確かめる。 接続が無いまま SQL を準備すると、未接続の状態で例外が起きる。
       *
       * @given 接続が null、送信者が "S001"
       * @when insert を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThatThrownBy(() -> sender.insert(null)).isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * 送信者が一致する行の項目を読み込むことを確かめる。 画面で編集する前に、保存済みの値を読み込む必要がある。
       *
       * @given 送信者が "S001"、SELECT の結果が 1 行、送信者名 "送信者A"、登録ユーザー "U001"
       * @when selectByPk を接続に対して実行する
       * @then trueを返し、送信者名に "送信者A" を読み込み、登録ユーザーに "U001" を読み込む
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(FROM_ID, "送信者A", REGISTER_DATE_TEXT, "U001")
                .build());
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.selectByPk(connection)).isTrue();

        assertThat(sender.getFromName()).isEqualTo("送信者A");
        assertThat(sender.getRegisterUser()).isEqualTo("U001");
      }

      /**
       * 該当行が無い場合に false を返し、項目を変えないことを確かめる。 存在しない送信者を読み込んでも、入力中の値を壊さない。
       *
       * @given 送信者が "S001"、SELECT の結果が 0 行
       * @when selectByPk を接続に対して実行する
       * @then falseを返し、送信者の値を変えない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.selectByPk(connection)).isFalse();

        assertThat(sender.getFromId()).isEqualTo(FROM_ID);
        assertThat(sender.getFromName()).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 送信者が null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返す恐れがある。
       *
       * @given 送信者が null
       * @when selectByPk を接続に対して実行する
       * @then falseを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 送信者がnullの場合_DBを呼ばずfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_M_SENDER(TENANT_ID).selectByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * 送信者を主キーに一致する行に更新することを確かめる。 送信者名と登録情報を保存する。
       *
       * @given 送信者が "S001"、送信者名が "送信者B"、更新件数が 1 件
       * @when updateByPk を接続に対して実行する
       * @then tenant_id を条件に含めて更新し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);
        sender.setFromName("送信者B");

        assertThat(sender.updateByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_M_SENDER SET from_name = ?, register_date = ?, register_user = ?"
                    + " WHERE from_id = ? AND tenant_id = ?");
        verify(statement).setString(1, "送信者B");
        verify(statement).setString(4, FROM_ID);
        verify(statement).setString(5, TENANT_ID);
      }

      /**
       * 登録日時を設定して更新すると、日時をタイムスタンプとして渡すことを確かめる。 登録日時の列は DB の日時型に合わせる必要がある。
       *
       * @given 送信者が "S001"、登録日時が "2026-01-15 10:00:00"、更新件数が 1 件
       * @when updateByPk を接続に対して実行する
       * @then 登録日時の位置にタイムスタンプを設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 登録日時が設定されている場合_登録日時をタイムスタンプで設定してtrueを返す(final Connection connection)
          throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);
        sender.setRegisterDate(REGISTER_DATE);

        assertThat(sender.updateByPk(connection)).isTrue();

        verify(statement).setTimestamp(2, Timestamp.valueOf("2026-01-15 10:00:00"));
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象の行が無いときに成功と誤認しない。
       *
       * @given 送信者が "S001"、更新件数が 0 件
       * @when updateByPk を接続に対して実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 送信者が null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全行を書き換える恐れがある。
       *
       * @given 送信者が null
       * @when updateByPk を接続に対して実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 送信者がnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_M_SENDER(TENANT_ID).updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * 送信者が一致する行を削除することを確かめる。 不要になったグループを残さない。
       *
       * @given 送信者が "S001"、削除件数が 1 件
       * @when deleteByPk を接続に対して実行する
       * @then tenant_id を条件に含めて削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.deleteByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement("DELETE FROM SES_AI_M_SENDER WHERE from_id = ? AND tenant_id = ?");
        verify(statement).setString(1, FROM_ID);
        verify(statement).setString(2, TENANT_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象の行が無いときに削除成功と誤認しない。
       *
       * @given 送信者が "S001"、削除件数が 0 件
       * @when deleteByPk を接続に対して実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 送信者が null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全行を消す恐れがある。
       *
       * @given 送信者が null
       * @when deleteByPk を接続に対して実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 送信者がnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_M_SENDER(TENANT_ID).deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class isExist {

    @Nested
    class 正常系 {

      /**
       * 送信者IDに一致する行がある場合に true を返すことを確かめる。 未登録の送信者だけを登録するための判定である。
       *
       * @given 送信者IDが "S001"、EXISTS の結果が true
       * @when isExist を接続で実行する
       * @then trueを返す
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection, MockResultSetBuilder.builder().columns("exists").row(true).build());
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.isExist(connection)).isTrue();
      }

      /**
       * 送信者IDに一致する行が無い場合に false を返すことを確かめる。 未登録の送信者は新しく登録する必要がある。
       *
       * @given 送信者IDが "S001"、SELECT の結果が 0 行
       * @when isExist を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns("exists").build());
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.isExist(connection)).isFalse();
      }

      /**
       * 該当行の値が偽の場合に false を返すことを確かめる。 存在の判定は値が真の行だけを該当とする。
       *
       * @given SELECT の結果が 1 行、値が false
       * @when isExist を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 該当行の値が偽の場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection, MockResultSetBuilder.builder().columns("exists").row(false).build());
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);
        assertThat(sender.isExist(connection)).isFalse();
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB接続が null の場合に例外を投げ、問い合わせを行わないことを確かめる。 接続の無い状態では存在を判定できないためである。
       *
       * @given 送信者ID "S001"、接続が null
       * @when isExist を接続 null で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);
        assertThatThrownBy(() -> sender.isExist(null)).isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 送信者IDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い判定は全行を対象にしてしまう。
       *
       * @given 送信者IDが null
       * @when isExist を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 送信者IDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_M_SENDER(TENANT_ID).isExist(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 判定の条件にテナントIDを渡して、他テナントの送信者を存在とみなさないことを確かめる。 他テナントの送信者で登録を省くと、自テナントの登録漏れになる。
       *
       * @given 送信者IDが "S001"、テナントIDが "T001"、EXISTS の結果が true
       * @when isExist を接続で実行する
       * @then テナントIDの位置に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を付けてtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection, MockResultSetBuilder.builder().columns("exists").row(true).build());
        SES_AI_M_SENDER sender = new SES_AI_M_SENDER(TENANT_ID);
        sender.setFromId(FROM_ID);

        assertThat(sender.isExist(connection)).isTrue();

        verify(statement).setString(2, TENANT_ID);
      }
    }
  }
}
