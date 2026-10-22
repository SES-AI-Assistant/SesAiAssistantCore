package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import copel.sesproductpackage.core.database.SES_AI_M_INGEST_ROUTE.ChannelType;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_M_INGEST_ROUTELot} の単体テスト。 */
class SES_AI_M_INGEST_ROUTELotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "channel_type", "route_key", "register_date", "register_user", "tenant_id"
  };

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントのルーティングを全件読み込むことを確かめる。 画面の一覧は自テナントの取込先だけを表示する。
       *
       * @given SELECT の結果が LINE と EMAIL の 2 行テナントID "T001"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then チャネル種別が LINE、EMAIL の順に 2 件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が2行の場合_2件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("LINE", "U-LINE-001", "2026-01-15 10:00:00", "U001", TENANT_ID)
                .row("EMAIL", "a@example.com", "2026-01-16 10:00:00", "U002", TENANT_ID)
                .build());
        SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(2);
        assertThat(lot.get(0).getChannelType()).isEqualTo(ChannelType.LINE);
        assertThat(lot.get(1).getRouteKey()).isEqualTo("a@example.com");
      }

      /**
       * 該当行が無い場合に空の一覧を返すことを確かめる。 取込先が未設定のテナントで画面を空表示にする。
       *
       * @given SELECT の結果が 0 行
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が0行の場合_空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込んだチャネル種別が対応しない値の場合に例外を投げることを確かめる。 対応しない取込先を黙って一覧に含めない。
       *
       * @given SELECT の結果のチャネル種別が "SMS" の 1 行
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void 不正なチャネル種別の場合_IllegalArgumentExceptionを投げる(final Connection connection)
          throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("SMS", "U-SMS-001", "2026-01-15 10:00:00", "U001", TENANT_ID)
                .build());

        assertThatThrownBy(() -> new SES_AI_M_INGEST_ROUTELot().selectAll(connection, TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントの取込先を返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_M_INGEST_ROUTELot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントの取込先を読まないことを確かめる。 他テナントの取込先が見えると誤った取込に繋がる。
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

        new SES_AI_M_INGEST_ROUTELot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントの取込先を読み込むことを確かめる。 バッチ処理は全テナントを対象にする。
       *
       * @given SELECT の結果が LINE の 1 行テナントID "T002"
       * @when selectAllWithoutTenantFilter を接続で実行する
       * @then 1件の一覧を返し、テナントIDが "T002" になる
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("LINE", "U-LINE-001", "2026-01-15 10:00:00", "U001", "T002")
                .build());
        SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();

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
        assertThatThrownBy(() -> new SES_AI_M_INGEST_ROUTELot().selectAllWithoutTenantFilter(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectByChannelTypeAndRouteKeyWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * チャネル種別とルートキーで全テナントの取込先を検索し、テナントの条件を付けないことを確かめる。 取込先の所属テナントをバッチ処理で求める。
       *
       * @given SELECT の結果が LINE の 1 行、チャネル種別が LINE、ルートキーが "U-LINE-001"
       * @when selectByChannelTypeAndRouteKeyWithoutTenantId を接続で実行する
       * @then 1件の一覧を返し、tenant_id の条件を付けずに "LINE" と "U-LINE-001" を設定する
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder()
                    .columns(COLUMNS)
                    .row("LINE", "U-LINE-001", "2026-01-15 10:00:00", "U001", TENANT_ID)
                    .build());
        SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();

        lot.selectByChannelTypeAndRouteKeyWithoutTenantId(
            connection, ChannelType.LINE, "U-LINE-001");

        assertThat(lot.size()).isEqualTo(1);
        verify(statement).setString(1, "LINE");
        verify(statement).setString(2, "U-LINE-001");
        verify(statement, never()).setString(eq(3), anyString());
      }

      /**
       * チャネル種別が null の場合に DB を呼ばず、空の一覧を返すことを確かめる。 キーの無い検索は全件を返す恐れがある。
       *
       * @given チャネル種別が null、ルートキーが "U-LINE-001"
       * @when selectByChannelTypeAndRouteKeyWithoutTenantId を接続で実行する
       * @then 空の一覧を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void チャネル種別がnullの場合_空の一覧を返す(final Connection connection) throws SQLException {
        SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();

        lot.selectByChannelTypeAndRouteKeyWithoutTenantId(connection, null, "U-LINE-001");

        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * ルートキーが null の場合に DB を呼ばず、空の一覧を返すことを確かめる。 キーの無い検索は全件を返す恐れがある。
       *
       * @given チャネル種別が LINE、ルートキーが null
       * @when selectByChannelTypeAndRouteKeyWithoutTenantId を接続で実行する
       * @then 空の一覧を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ルートキーがnullの場合_空の一覧を返す(final Connection connection) throws SQLException {
        SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();

        lot.selectByChannelTypeAndRouteKeyWithoutTenantId(connection, ChannelType.LINE, null);

        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま SQL を実行できない。
       *
       * @given 接続が null、チャネル種別が LINE、ルートキーが "U-LINE-001"
       * @when selectByChannelTypeAndRouteKeyWithoutTenantId を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new SES_AI_M_INGEST_ROUTELot()
                        .selectByChannelTypeAndRouteKeyWithoutTenantId(
                            null, ChannelType.LINE, "U-LINE-001"))
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
        SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();

        lot.selectAllPaged(connection, "T001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }
}
