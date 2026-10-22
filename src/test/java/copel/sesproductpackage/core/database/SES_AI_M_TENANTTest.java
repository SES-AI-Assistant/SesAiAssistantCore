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

/** {@link SES_AI_M_TENANT} の単体テスト。 */
class SES_AI_M_TENANTTest {

  /** テナントID。 テナント情報マスタの主キーであり、テナント条件を付けない。 */
  private static final String TENANT_ID = "T001";

  /** 登録日時の文字列。 */
  private static final String REGISTER_DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "tenant_id", "tenant_name", "tenant_status_cd", "register_date", "register_user"
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
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナント情報は主キーのテナントIDで識別する。
       *
       * @given テナントIDが null
       * @when SES_AI_M_TENANT を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_M_TENANT(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * テナント情報を登録するSQLにテナントIDを含めずに実行することを確かめる。 テナント情報はテナントIDそのものが主キーの。
       *
       * @given テナントIDが "T001"、テナント名が "テナントA"、ステータス区分が "1"、登録日時が "2026-01-15 10:00:00"、登録ユーザーが
       *     "U001"、更新件数が 1 件
       * @when insert を接続で実行する
       * @then テナントIDを列に含むINSERT文を準備し、1件を返す
       */
      @MockDbConnection
      @Test
      void テナント情報が揃っている場合_1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantName("テナントA");
        tenant.setTenantStatusCd("1");
        tenant.setRegisterDate(new OriginalDateTime(REGISTER_DATE_TEXT));
        tenant.setRegisterUser("U001");

        assertThat(tenant.insert(connection)).isEqualTo(1);

        verify(connection)
            .prepareStatement(
                "INSERT INTO SES_AI_M_TENANT (tenant_id, tenant_name, tenant_status_cd,"
                    + " register_date, register_user) VALUES (?, ?, ?, ?, ?)");
        verify(statement).setString(1, TENANT_ID);
        verify(statement).setString(2, "テナントA");
        verify(statement).setString(3, "1");
        verify(statement).setTimestamp(4, Timestamp.valueOf(REGISTER_DATE_TEXT));
        verify(statement).setString(5, "U001");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 登録日時が未設定の場合に日時にnullを設定することを確かめる。 登録日時は必須の列ではないため、未設定でも登録できる必要がある。
       *
       * @given テナントIDが "T001"、登録日時が未設定、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 登録日時の位置に null を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void 登録日時が未設定の場合_登録日時にnullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).insert(connection)).isEqualTo(1);

        verify(statement).setTimestamp(4, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * テナントIDが一致する行の項目を読み込むことを確かめる。 画面で編集する前に、保存済みのテナント情報を読み込む必要がある。
       *
       * @given テナントIDが "T001"、SELECT の結果が 1 行テナント名 "テナントA"、ステータス区分 "1"
       * @when selectByPk を接続で実行する
       * @then テナント名とステータス区分を更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_項目を更新する(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(TENANT_ID, "テナントA", "1", REGISTER_DATE_TEXT, "U001")
                .build());
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);

        tenant.selectByPk(connection);

        assertThat(tenant.getTenantName()).isEqualTo("テナントA");
        assertThat(tenant.getTenantStatusCd()).isEqualTo("1");
      }

      /**
       * 該当行が無い場合に項目を変えないことを確かめる。 存在しないテナントを読み込んでも、入力中の値を壊さない。
       *
       * @given テナントIDが "T001"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then テナント名を変えずに何もしない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_何もしない(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);

        tenant.selectByPk(connection);

        assertThat(tenant.getTenantName()).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * テナントIDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返す恐れがある。
       *
       * @given テナントIDが null
       * @when selectByPk を接続で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantId(null);

        tenant.selectByPk(connection);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * テナントIDが一致する行のテナント名とステータス区分を更新することを確かめる。 テナント情報の変更を保存する。
       *
       * @given テナントIDが "T001"、テナント名が "テナントB"、ステータス区分が "2"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then テナント名、ステータス区分、テナントIDの順に設定して更新し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantName("テナントB");
        tenant.setTenantStatusCd("2");

        assertThat(tenant.updateByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_M_TENANT SET tenant_name = ?, tenant_status_cd = ?"
                    + " WHERE tenant_id = ?");
        verify(statement).setString(1, "テナントB");
        verify(statement).setString(2, "2");
        verify(statement).setString(3, TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象のテナントが無いときに更新成功と誤認しない。
       *
       * @given テナントIDが "T001"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * テナントIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全テナントを書き換える恐れがある。
       *
       * @given テナントIDが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantId(null);

        assertThat(tenant.updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * テナントIDが一致する行を削除することを確かめる。 不要になったテナント情報を残さない。
       *
       * @given テナントIDが "T001"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then テナントIDを条件にして削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).deleteByPk(connection)).isTrue();

        verify(connection).prepareStatement("DELETE FROM SES_AI_M_TENANT WHERE tenant_id = ?");
        verify(statement).setString(1, TENANT_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象のテナントが無いときに削除成功と誤認しない。
       *
       * @given テナントIDが "T001"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * テナントIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全テナントを消す恐れがある。
       *
       * @given テナントIDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantId(null);

        assertThat(tenant.deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }
}
