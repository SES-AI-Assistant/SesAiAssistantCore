package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_T_MATCHLot} の単体テスト。 */
class SES_AI_T_MATCHLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "matching_id",
    "user_id",
    "job_id",
    "person_id",
    "job_content",
    "person_content",
    "status_cd",
    "evaluation_text",
    "score",
    "must_evaluation_text",
    "want_evaluation_text",
    "other_evaluation_text",
    "profit",
    "register_date",
    "register_user",
    "tenant_id"
  };

  /**
   * マッチングを1行分与える結果セットを作る。
   *
   * @param matchingId マッチングID
   * @param tenantId テナントID
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet oneRow(final String matchingId, final String tenantId)
      throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            matchingId,
            "U001",
            "J001",
            "P001",
            "案件内容",
            "要員内容",
            "20",
            "評価",
            80,
            "必須",
            "尚可",
            "その他",
            new BigDecimal(1_000_000),
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
       * テナントのマッチングを全件読み込み、ステータスと利益を変換することを確かめる。 画面の一覧は自テナントのマッチングだけを表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、ステータス "20"、利益 1000000
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then ステータスが進行中の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_変換した1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("0123456789", TENANT_ID));
        SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getStatus().getCode()).isEqualTo("20");
        assertThat(lot.get(0).getProfit().getValue())
            .isEqualByComparingTo(new BigDecimal(1_000_000));
      }

      /**
       * スコアと利益が未設定の行を読み込み、未設定の値のまま保持することを確かめる。 未評価のマッチングを一覧で壊さないためである。
       *
       * @given SELECT の結果が 1 行、スコアと利益が null
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then スコアが null、利益が空の値の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void スコアと利益が未設定の場合_未設定の値を持つ1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(
                    "0123456789",
                    "U001",
                    "J001",
                    "P001",
                    "案件内容",
                    "要員内容",
                    "20",
                    "評価",
                    null,
                    "必須",
                    "尚可",
                    "その他",
                    null,
                    "2026-01-15 10:00:00",
                    "U000",
                    TENANT_ID)
                .build());
        SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getScore()).isNull();
        assertThat(lot.get(0).getProfit().getValue()).isNull();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントのマッチングを返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_T_MATCHLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントのマッチングを読まないことを確かめる。 他テナントのマッチングが一覧に出ると、案件と要員の情報が漏えいする。
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

        new SES_AI_T_MATCHLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントのマッチングを読み込むことを確かめる。 バッチ処理は全テナントのマッチングを対象にする。
       *
       * @given SELECT の結果が 1 行テナントID "T002"
       * @when selectAllWithoutTenantId を接続で実行する
       * @then テナントIDが "T002" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("0123456789", "T002"));
        SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();

        lot.selectAllWithoutTenantId(connection);

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
       * @when selectAllWithoutTenantId を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_MATCHLot().selectAllWithoutTenantId(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class isExistByJobId {

    @Nested
    class 正常系 {

      /**
       * 一覧に案件IDが一致するマッチングがある場合に true を返すことを確かめる。 案件に紐づくマッチングが既にあるかを判定する。
       *
       * @given 一覧に案件ID "J001" のマッチングが1件ある
       * @when isExistByJobId を "J001" で実行する
       * @then trueを返す
       */
      @Test
      void 案件IDが一致する要素がある場合_trueを返す() {
        SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();
        lot.add(matchOf("J001"));

        assertThat(lot.isExistByJobId("J001")).isTrue();
      }

      /**
       * 一覧に案件IDが一致するマッチングが無い場合に false を返すことを確かめる。 紐づくマッチングが無い案件を既存と誤認しない。
       *
       * @given 一覧に案件ID "J001" のマッチングが1件ある
       * @when isExistByJobId を "J002" で実行する
       * @then falseを返す
       */
      @Test
      void 案件IDが一致する要素が無い場合_falseを返す() {
        SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();
        lot.add(matchOf("J001"));

        assertThat(lot.isExistByJobId("J002")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが null の場合に false を返すことを確かめる。 案件の無い判定を、全件に一致させない。
       *
       * @given 一覧に案件ID "J001" のマッチングが1件ある
       * @when isExistByJobId を null で実行する
       * @then falseを返す
       */
      @Test
      void 案件IDがnullの場合_falseを返す() {
        SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();
        lot.add(matchOf("J001"));

        assertThat(lot.isExistByJobId(null)).isFalse();
      }
    }
  }

  /**
   * 案件IDを持つマッチングを生成する。
   *
   * @param jobId 案件ID
   * @return 生成したマッチング
   */
  private static SES_AI_T_MATCH matchOf(final String jobId) {
    SES_AI_T_MATCH match = new SES_AI_T_MATCH(TENANT_ID);
    match.setJobId(jobId);
    return match;
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
        SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();

        lot.selectAllPaged(connection, "T001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }
}
