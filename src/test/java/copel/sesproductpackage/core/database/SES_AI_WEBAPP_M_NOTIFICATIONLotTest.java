package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_WEBAPP_M_NOTIFICATIONLot} の単体テスト。 */
class SES_AI_WEBAPP_M_NOTIFICATIONLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "notification_id",
    "user_id",
    "device_type",
    "device_name",
    "push_notification_endpoint",
    "p256dh",
    "auth",
    "enabled",
    "notify_all_match",
    "register_date",
    "register_user",
    "tenant_id"
  };

  /**
   * 通知デバイスを1行分与える結果セットを作り、テナントIDを指定して返す。
   *
   * @param notificationId 通知デバイスID
   * @param userId ユーザーID
   * @param tenantId テナントID
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet oneRow(
      final String notificationId, final String userId, final String tenantId) throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            notificationId,
            userId,
            "web",
            "Chrome",
            "https://push.example.com/abc",
            "p256dh-key",
            "auth-token",
            true,
            false,
            "2026-01-15 10:00:00",
            "U000",
            tenantId)
        .build();
  }

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントの通知デバイスを全件読み込み、テナントIDと有効フラグを反映することを確かめる。 画面の一覧は自テナントの通知先だけを表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、有効フラグ true
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then テナントIDが "T001" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントIDが一致する1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("N001", "U001", TENANT_ID));
        SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getTenantId()).isEqualTo(TENANT_ID);
        assertThat(lot.get(0).getEnabled()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントの通知デバイスを返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_WEBAPP_M_NOTIFICATIONLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントの通知デバイスを読まないことを確かめる。 他テナントの通知先が一覧に出ると、通知の内容が漏えいする。
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

        new SES_AI_WEBAPP_M_NOTIFICATIONLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントの通知デバイスを読み込むことを確かめる。 バッチ処理は全テナントの通知先を対象にする。
       *
       * @given SELECT の結果が 1 行テナントID "T002"
       * @when selectAllWithoutTenantFilter を接続で実行する
       * @then テナントIDが "T002" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("N002", "U002", "T002"));
        SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();

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
        assertThatThrownBy(
                () -> new SES_AI_WEBAPP_M_NOTIFICATIONLot().selectAllWithoutTenantFilter(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectByUserId {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDとテナントIDで通知デバイスを検索し、1件の一覧を返すことを確かめる。 ユーザーごとに複数の端末へ通知する。
       *
       * @given SELECT の結果が 1 行、ユーザーIDが "U001"、テナントIDが "T001"
       * @when selectByUserId を接続、テナントID "T001"、ユーザーID "U001" で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(connection, oneRow("N001", "U001", TENANT_ID));
        SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();

        lot.selectByUserId(connection, TENANT_ID, "U001");

        assertThat(lot.size()).isEqualTo(1);
        verify(statement).setString(1, "U001");
        verify(statement).setString(2, TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま SQL を実行できない。
       *
       * @given 接続が null、ユーザーIDが "U001"
       * @when selectByUserId を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> new SES_AI_WEBAPP_M_NOTIFICATIONLot().selectByUserId(null, TENANT_ID, "U001"))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばず、空の一覧を返すことを確かめる。 キーの無い検索は全ユーザーの端末を返す。
       *
       * @given ユーザーIDが null、テナントIDが "T001"
       * @when selectByUserId を接続、テナントID "T001"、ユーザーID null で実行する
       * @then 空の一覧を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_空の一覧を返す(final Connection connection) throws SQLException {
        SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();

        lot.selectByUserId(connection, TENANT_ID, null);

        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class selectByNotifyAllMatch {

    @Nested
    class 正常系 {

      /**
       * 全件通知の通知デバイスを、テナントの条件付きで検索することを確かめる。 全件通知の対象を自テナントの端末に限る。
       *
       * @given SELECT の結果が 1 行、テナントIDが "T001"
       * @when selectByNotifyAllMatch を接続とテナントID "T001" で実行する
       * @then 全件通知の条件とテナントの条件を含む SQL で 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_全件通知とテナントの条件で1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(connection, oneRow("N001", "U001", TENANT_ID));
        SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();

        lot.selectByNotifyAllMatch(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        verify(connection)
            .prepareStatement(contains("WHERE notify_all_match = true AND tenant_id = ?"));
        verify(statement).setString(1, TENANT_ID);
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
        SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();

        lot.selectAllPaged(connection, "T001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }
}
