package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_M_GROUPLot} の単体テスト。 */
class SES_AI_M_GROUPLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 送信元グループの取得結果の列。 */
  private static final String[] COLUMNS = {
    "from_group", "group_name", "register_date", "register_user", "tenant_id"
  };

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントの送信元グループを全件読み込むことを確かめる。 画面の一覧は自テナントのグループだけを表示する。
       *
       * @given SELECT の結果が "G001" と "G002" の 2 行テナントID "T001"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 送信元グループが "G001"、"G002" の順に 2 件になる
       */
      @MockDbConnection
      @Test
      void 結果が2行の場合_2件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("G001", "グループA", "2026-01-15 10:00:00", "U001", TENANT_ID)
                .row("G002", "グループB", "2026-01-16 10:00:00", "U002", TENANT_ID)
                .build());
        SES_AI_M_GROUPLot lot = new SES_AI_M_GROUPLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(2);
        assertThat(lot.get(0).getFromGroup()).isEqualTo("G001");
        assertThat(lot.get(1).getGroupName()).isEqualTo("グループB");
      }

      /**
       * 該当行が無い場合に空の一覧になることを確かめる。 対象のグループが無いテナントで画面を空表示にする。
       *
       * @given SELECT の結果が 0 行
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が0行の場合_空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_M_GROUPLot lot = new SES_AI_M_GROUPLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントのグループを返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_M_GROUPLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま SQL を実行できない。
       *
       * @given 接続が null
       * @when selectAll を null の接続とテナントID "T001" で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_M_GROUPLot().selectAll(null, TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントのグループを読まないことを確かめる。 テナントIDを条件にしないと他テナントのデータが見える。
       *
       * @given SELECT の結果が 0 行
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then tenant_id の位置に "T001" を設定し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_T001で絞り込んだ空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection, MockResultSetBuilder.builder().columns(COLUMNS).build());

        new SES_AI_M_GROUPLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントの送信元グループを読み込むことを確かめる。 バッチ処理は全テナントを対象にする。
       *
       * @given SELECT の結果が "G001" の 1 行テナントID "T002"
       * @when selectAllWithoutTenantFilter を接続で実行する
       * @then 送信元グループが "G001" の 1 件になり、テナントIDが "T002" になる
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("G001", "グループA", "2026-01-15 10:00:00", "U001", "T002")
                .build());
        SES_AI_M_GROUPLot lot = new SES_AI_M_GROUPLot();

        lot.selectAllWithoutTenantFilter(connection);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getFromGroup()).isEqualTo("G001");
        assertThat(lot.get(0).getTenantId()).isEqualTo("T002");
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま SQL を実行できない。
       *
       * @given 接続が null
       * @when selectAllWithoutTenantFilter を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_M_GROUPLot().selectAllWithoutTenantFilter(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class getSelectAllSql {

    @Nested
    class 正常系 {

      /**
       * 件数が0件の場合に空の一覧を返すことを確かめる。 対象が無いテナントでページ送りの件数を0にそろえる。
       *
       * @given 件数の問い合わせが 0 を返し、一覧の問い合わせが 0 行を返す
       * @when selectAllPaged をテナントID "T001" で、1ページ目、1ページ10件で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 全件のページ取得で全件のページ取得で件数が0件の場合_件数0の空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubStatement(
            connection, MockResultSetBuilder.builder().columns("cnt").row(0).build());
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns("cnt").build());
        SES_AI_M_GROUPLot lot = new SES_AI_M_GROUPLot();

        lot.selectAllPaged(connection, "T001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }
}
