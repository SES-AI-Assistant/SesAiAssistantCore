package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_T_WATCH.TargetType;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_T_WATCHLot} の単体テスト。 */
class SES_AI_T_WATCHLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "user_id",
    "target_id",
    "target_type",
    "memo",
    "register_date",
    "register_user",
    "ttl",
    "tenant_id"
  };

  /** 登録日時と有効期限の文字列。 */
  private static final String DATE_TEXT = "2026-01-15 10:00:00";

  /**
   * ウォッチを1行分与える結果セットを作る。
   *
   * @param userId ユーザーID
   * @param targetId 対象ID
   * @param tenantId テナントID
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet oneRow(final String userId, final String targetId, final String tenantId)
      throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(userId, targetId, "JOB", "メモ", DATE_TEXT, "U000", DATE_TEXT, tenantId)
        .build();
  }

  /**
   * ウォッチを生成する。テナントIDと対象IDとユーザーIDを指定する。
   *
   * @param tenantId テナントID
   * @param targetId 対象ID
   * @return 生成したウォッチ
   */
  private static SES_AI_T_WATCH watch(final String tenantId, final String targetId) {
    SES_AI_T_WATCH watch = new SES_AI_T_WATCH(tenantId);
    watch.setUserId("U001");
    watch.setTargetId(targetId);
    watch.setTargetType(TargetType.JOB);
    return watch;
  }

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントのウォッチを全件読み込み、対象種別を列挙値に変換することを確かめる。 画面の一覧は自テナントのウォッチだけを表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、対象種別 "JOB"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 対象種別が JOB の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_対象種別を変換した1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("U001", "J001", TENANT_ID));
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getTargetType()).isEqualTo(TargetType.JOB);
        assertThat(lot.get(0).getTtl().toString()).isEqualTo("2026-01-15 10:00:00");
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントのウォッチを返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_T_WATCHLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントのウォッチを読まないことを確かめる。 他テナントのウォッチが一覧に出ると、通知の対象が漏えいする。
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

        new SES_AI_T_WATCHLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントのウォッチを読み込むことを確かめる。 バッチ処理は全テナントのウォッチを対象にする。
       *
       * @given SELECT の結果が 1 行テナントID "T002"
       * @when selectAllWithoutTenantFilter を接続で実行する
       * @then テナントIDが "T002" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("U002", "J002", "T002"));
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();

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
        assertThatThrownBy(() -> new SES_AI_T_WATCHLot().selectAllWithoutTenantFilter(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectByUserId {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDとテナントIDで検索し、一致するウォッチを1件の一覧にすることを確かめる。 ユーザーごとの監視一覧を表示する。
       *
       * @given SELECT の結果が 1 行、ユーザーIDが "U001"、テナントIDが "T001"
       * @when selectByUserId を接続、テナントID "T001"、ユーザーID "U001" で実行する
       * @then 1件の一覧を返し、ユーザーIDとテナントIDを条件に設定する
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(connection, oneRow("U001", "J001", TENANT_ID));
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();

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
        assertThatThrownBy(() -> new SES_AI_T_WATCHLot().selectByUserId(null, TENANT_ID, "U001"))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectByUserIdPaged {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDで件数を数えてから、指定のページの行を読み込むことを確かめる。 ページ送りの件数と表示行を揃える。
       *
       * @given 件数が 1 件、SELECT の結果が 1 行、ユーザーIDが "U001"、ページ番号が 1、ページサイズが 10
       * @when selectByUserIdPaged を接続、テナントID "T001" で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数が1件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = mock(PreparedStatement.class);
        when(countStatement.executeQuery())
            .thenReturn(MockResultSetBuilder.builder().columns("cnt").row(1L).build());
        PreparedStatement rowStatement = mock(PreparedStatement.class);
        when(rowStatement.executeQuery()).thenReturn(oneRow("U001", "J001", TENANT_ID));
        when(connection.prepareStatement(anyString())).thenReturn(rowStatement);
        when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();

        lot.selectByUserIdPaged(connection, TENANT_ID, "U001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に検索を行わず、一覧を変えないことを確かめる。 キーの無い検索は他のユーザーのウォッチを返す。
       *
       * @given ユーザーIDが null、テナントIDが "T001"
       * @when selectByUserIdPaged を接続、テナントID "T001"、ユーザーID null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_WATCHLot().selectByUserIdPaged(connection, TENANT_ID, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class containsById {

    @Nested
    class 正常系 {

      /**
       * 一覧に対象IDが一致するウォッチがある場合に true を返すことを確かめる。 画面で既に監視中の対象を判定する。
       *
       * @given 一覧に対象ID "J001" のウォッチが1件ある
       * @when containsById を "J001" で実行する
       * @then trueを返す
       */
      @Test
      void 対象IDが一致する要素がある場合_trueを返す() {
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
        lot.add(watch(TENANT_ID, "J001"));

        assertThat(lot.containsById("J001")).isTrue();
      }

      /**
       * 一覧に対象IDが一致するウォッチが無い場合に false を返すことを確かめる。 未監視の対象を監視中と誤認しない。
       *
       * @given 一覧に対象ID "J001" のウォッチが1件ある
       * @when containsById を "J002" で実行する
       * @then falseを返す
       */
      @Test
      void 対象IDが一致する要素が無い場合_falseを返す() {
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
        lot.add(watch(TENANT_ID, "J001"));

        assertThat(lot.containsById("J002")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対象IDが null の場合に false を返すことを確かめる。 対象の無い判定は一覧の全件に一致させない。
       *
       * @given 一覧に対象ID "J001" のウォッチが1件ある
       * @when containsById を null で実行する
       * @then falseを返す
       */
      @Test
      void 対象IDがnullの場合_falseを返す() {
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
        lot.add(watch(TENANT_ID, "J001"));

        assertThat(lot.containsById(null)).isFalse();
      }

      /**
       * 対象IDが空文字の場合に false を返すことを確かめる。 空の対象IDは判定の対象にしない。
       *
       * @given 一覧に対象ID "J001" のウォッチが1件ある
       * @when containsById を空文字で実行する
       * @then falseを返す
       */
      @Test
      void 対象IDが空文字の場合_falseを返す() {
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
        lot.add(watch(TENANT_ID, "J001"));

        assertThat(lot.containsById("")).isFalse();
      }
    }
  }

  @Nested
  class selectByTargetId {

    @Nested
    class 正常系 {

      /**
       * 対象IDと対象種別とテナントIDで検索し、一致するウォッチを1件の一覧にすることを確かめる。 対象ごとの監視者を求める。
       *
       * @given SELECT の結果が 1 行、対象IDが "J001"、対象種別が JOB、テナントIDが "T001"
       * @when selectByTargetId を接続、テナントID "T001"、対象ID "J001"、対象種別 JOB で実行する
       * @then 1件の一覧を返し、対象IDと対象種別とテナントIDを条件に設定する
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(connection, oneRow("U001", "J001", TENANT_ID));
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();

        lot.selectByTargetId(connection, TENANT_ID, "J001", TargetType.JOB);

        assertThat(lot.size()).isEqualTo(1);
        verify(statement).setString(1, "J001");
        verify(statement).setString(2, "JOB");
        verify(statement).setString(3, TENANT_ID);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対象IDが null の場合に検索を行わず、空の一覧を返すことを確かめる。 キーの無い検索は全対象のウォッチを返す。
       *
       * @given 対象IDが null、対象種別が JOB
       * @when selectByTargetId を接続、テナントID "T001" で実行する
       * @then 空の一覧を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 対象IDがnullの場合_空の一覧を返す(final Connection connection) throws SQLException {
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();

        lot.selectByTargetId(connection, TENANT_ID, null, TargetType.JOB);

        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 対象種別が null の場合に検索を行わず、空の一覧を返すことを確かめる。 対象種別の無い検索は種別の違う行を返す。
       *
       * @given 対象IDが "J001"、対象種別が null
       * @when selectByTargetId を接続、テナントID "T001" で実行する
       * @then 空の一覧を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 対象種別がnullの場合_空の一覧を返す(final Connection connection) throws SQLException {
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();

        lot.selectByTargetId(connection, TENANT_ID, "J001", null);

        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class deleteExpiredWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * 期限切れのウォッチを全テナントから削除し、削除件数を返すことを確かめる。 期限を過ぎたウォッチを残すと、通知の対象が古いままになる。
       *
       * @given 期限切れのウォッチが 2 件ある削除件数 2
       * @when deleteExpiredWithoutTenantId を接続で実行する
       * @then 期限切れの条件だけで削除し、2件を返す
       */
      @MockDbConnection
      @Test
      void 期限切れが2件の場合_2件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        when(statement.executeUpdate()).thenReturn(2);
        when(connection.prepareStatement("DELETE FROM SES_AI_T_WATCH WHERE ttl < NOW()"))
            .thenReturn(statement);

        assertThat(new SES_AI_T_WATCHLot().deleteExpiredWithoutTenantId(connection)).isEqualTo(2);
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に削除を拒否することを確かめる。 接続が無いまま削除を実行できない。
       *
       * @given 接続が null
       * @when deleteExpiredWithoutTenantId を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_WATCHLot().deleteExpiredWithoutTenantId(null))
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
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();

        lot.selectAllPaged(connection, "T001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }
}
