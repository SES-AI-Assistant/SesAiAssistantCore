package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_M_TENANTLot} の単体テスト。 */
class SES_AI_M_TENANTLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** テナントの取得結果の列。 マッチング強度の列を含む。 */
  private static final String[] COLUMNS = {
    "tenant_id",
    "tenant_name",
    "tenant_status_cd",
    "match_must_level",
    "match_want_level",
    "register_date",
    "register_user"
  };

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントのテナントを全件読み込み、マッチング強度を列挙値に変換することを確かめる。 画面の一覧は自テナントの設定を表示するため、強度を読み違えないことを守る。
       *
       * @given SELECT の結果が 2 行で、1行目は必須 PartiallyMet と尚可 NotMet、2行目は必須と尚可が NULL
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 2件の一覧を返し、1件目の必須を PartiallyMet、2件目の必須を null で読み込む
       */
      @MockDbConnection
      @Test
      void 結果が2行の場合_2件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(
                    TENANT_ID,
                    "テナントA",
                    "1",
                    "PartiallyMet",
                    "NotMet",
                    "2026-01-15 10:00:00",
                    "U001")
                .row("T002", "テナントB", "2", null, null, "2026-01-16 10:00:00", "U002")
                .build());
        SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(2);
        assertThat(lot.get(0).getTenantId()).isEqualTo(TENANT_ID);
        assertThat(lot.get(0).getMatchMustLevel()).isEqualTo(EvaluateType.PartiallyMet);
        assertThat(lot.get(1).getTenantName()).isEqualTo("テナントB");
        assertThat(lot.get(1).getMatchMustLevel()).isNull();
      }

      /**
       * 該当行が無い場合に空の一覧になることを確かめる。 対象のテナントが無いテナントで画面を空表示にする。
       *
       * @given SELECT の結果が 0 行
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が0行の場合_空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントのテナントを返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_M_TENANTLot().selectAll(connection, null))
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
        assertThatThrownBy(() -> new SES_AI_M_TENANTLot().selectAll(null, TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントのテナントを読まないことを確かめる。 テナントIDを条件にしないと他テナントのデータが見える。
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

        new SES_AI_M_TENANTLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントのテナントを読み込むことを確かめる。 バッチ処理は全テナントを対象にする。
       *
       * @given SELECT の結果が TENANT_ID の 1 行テナントID "T002"
       * @when selectAllWithoutTenantFilter を接続で実行する
       * @then テナントが TENANT_ID の 1 件になり、テナントIDが "T002" になる
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("T002", "テナントA", "1", "FullyMet", "NotMet", "2026-01-15 10:00:00", "U001")
                .build());
        SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();

        lot.selectAllWithoutTenantFilter(connection);

        assertThat(lot.size()).isEqualTo(1);
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
        assertThatThrownBy(() -> new SES_AI_M_TENANTLot().selectAllWithoutTenantFilter(null))
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
        SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();

        lot.selectAllPaged(connection, "T001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }
}
