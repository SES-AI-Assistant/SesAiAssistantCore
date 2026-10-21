package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import copel.sesproductpackage.core.unit.Plan;
import copel.sesproductpackage.core.unit.Role;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_WEBAPP_M_USERLot} の単体テスト。 */
class SES_AI_WEBAPP_M_USERLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 全件取得の SQLWHERE 句を含まない。 */
  private static final String SELECT_ALL_SQL =
      "SELECT user_id, user_name, role_cd, plan_cd, register_date, register_user, tenant_id FROM "
          + "SES_AI_WEBAPP_M_USER";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "user_id", "user_name", "role_cd", "plan_cd", "register_date", "register_user", "tenant_id"
  };

  /**
   * 件数の問い合わせに、指定の件数を返す結果セットを作る。
   *
   * @param count 件数
   * @return 件数の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet countResult(final int count) throws SQLException {
    return MockResultSetBuilder.builder().columns("cnt").row(count).build();
  }

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントのユーザーを全件読み込み、ロールとプランをコードから変換することを確かめる。 画面の一覧に役割の名称を表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、ロール "30"、プラン "10"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then ロールが 運用、プランが PREMIUM の 1 件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_ロールとプランを変換した1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("U001", "山田太郎", "30", "10", "2026-01-15 10:00:00", "U000", TENANT_ID)
                .build());
        SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getRole()).isEqualTo(Role.運用);
        assertThat(lot.get(0).getTenantId()).isEqualTo(TENANT_ID);
      }

      /**
       * 該当行が無い場合に空の一覧を返すことを確かめる。 ユーザーが未登録のテナントで画面を空表示にする。
       *
       * @given SELECT の結果が 0 行
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が0行の場合_空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントのユーザーを返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_WEBAPP_M_USERLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントのユーザーを読まないことを確かめる。 他テナントのユーザーが一覧に出ると、権限の漏えいになる。
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

        new SES_AI_WEBAPP_M_USERLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントのユーザーを読み込むことを確かめる。 システム管理者の画面とバッチ処理は全テナントを対象にする。
       *
       * @given SELECT の結果が 2 行テナントID "T001" と "T002"
       * @when selectAllWithoutTenantId を接続で実行する
       * @then 2件の一覧を返し、2件目のテナントIDが "T002" になる
       */
      @MockDbConnection
      @Test
      void 結果が2行の場合_テナントを問わず2件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("U001", "山田太郎", "30", "10", "2026-01-15 10:00:00", "U000", TENANT_ID)
                .row("U002", "鈴木一郎", "10", "00", "2026-01-16 10:00:00", "U000", "T002")
                .build());
        SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();

        lot.selectAllWithoutTenantId(connection);

        assertThat(lot.size()).isEqualTo(2);
        assertThat(lot.get(1).getTenantId()).isEqualTo("T002");
        assertThat(lot.get(1).getPlan()).isEqualTo(Plan.FREE);
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま SQL を実行できない。
       *
       * @given 接続が null
       * @when selectAllWithoutTenantId を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_WEBAPP_M_USERLot().selectAllWithoutTenantId(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectAllPagedWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * 全件数を数えてから、指定のページの行を読み込むことを確かめる。 画面のページ送りに必要な件数と、表示する行を揃える。
       *
       * @given 件数が 3 件、SELECT の結果が 2 行、ページ番号が 1、ページサイズが 2
       * @when selectAllPagedWithoutTenantId を接続で実行する
       * @then 件数に 3 を保持し、LIMIT 2 OFFSET 0 の SQL で 2件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数が3件の場合_先頭の2件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubStatement(connection, countResult(3));
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("U001", "山田太郎", "30", "10", "2026-01-15 10:00:00", "U000", TENANT_ID)
                .row("U002", "鈴木一郎", "10", "00", "2026-01-16 10:00:00", "U000", TENANT_ID)
                .build());
        SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();

        lot.selectAllPagedWithoutTenantId(connection, 1, 2);

        assertThat(lot.getTotalCount()).isEqualTo(3L);
        assertThat(lot.size()).isEqualTo(2);
        verify(connection).prepareStatement(SELECT_ALL_SQL + " LIMIT 2 OFFSET 0");
      }

      /**
       * 件数の問い合わせが行を返さない場合に、件数を0のまま空の一覧を返すことを確かめる。 件数が取れないときに一覧の問い合わせを行わないためである。
       *
       * @given 件数の問い合わせの結果が 0 行
       * @when selectAllPagedWithoutTenantId を接続でページ 1、サイズ 2 で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数の結果が0行の場合_件数0の空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubStatement(connection, MockResultSetBuilder.builder().columns("cnt").build());
        SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();

        lot.selectAllPagedWithoutTenantId(connection, 1, 2);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま件数を数えられない。
       *
       * @given 接続が null
       * @when selectAllPagedWithoutTenantId を null の接続でページ 1、サイズ 2 で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> new SES_AI_WEBAPP_M_USERLot().selectAllPagedWithoutTenantId(null, 1, 2))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 件数が 0 件の場合に行の検索を行わず、空の一覧を返すことを確かめる。 0 件のときに行の問い合わせを省く。
       *
       * @given 件数が 0 件
       * @when selectAllPagedWithoutTenantId を接続でページ 1、サイズ 2 で実行する
       * @then 空の一覧を返し、行の SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 件数が0件の場合_空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubStatement(connection, countResult(0));
        SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();

        lot.selectAllPagedWithoutTenantId(connection, 1, 2);

        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(anyString());
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
        SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();

        lot.selectAllPaged(connection, "T001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }
}
