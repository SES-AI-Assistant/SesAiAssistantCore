package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** {@link SES_AI_T_MATCH_JOB_PERSONLot} の単体テスト。 */
class SES_AI_T_MATCH_JOB_PERSONLotTest {

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
    "tenant_id",
    "job_title",
    "job_unit_price",
    "person_name",
    "person_age",
    "person_gender",
    "person_unit_price"
  };

  /**
   * 結合結果を1行分与える結果セットを作る。
   *
   * @param tenantId テナントID
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet oneRow(final String tenantId) throws SQLException {
    return MockResultSetBuilder.builder()
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
            80,
            "必須",
            "尚可",
            "その他",
            new BigDecimal(1_000_000),
            "2026-01-15 10:00:00",
            "U000",
            tenantId,
            "案件名",
            new BigDecimal(1_500_000),
            "山田太郎",
            35,
            "Man",
            new BigDecimal(500_000))
        .build();
  }

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントの結合結果を読み込み、案件名と要員の性別を変換することを確かめる。 一覧に案件名と要員の情報を表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、性別 "Man"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 案件名が "案件名"、性別が Man の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_案件名と性別を変換した1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow(TENANT_ID));
        SES_AI_T_MATCH_JOB_PERSONLot lot = new SES_AI_T_MATCH_JOB_PERSONLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getJobTitle()).isEqualTo("案件名");
        assertThat(lot.get(0).getPersonGender()).isEqualTo(Gender.Man);
      }

      /**
       * 評価の値やスコア、単価、年齢、性別が NULL の行を読み込み、対応する項目を未設定にすることを確かめる。 未入力の値があっても一覧を壊さない。
       *
       * @given SELECT の結果が 1 行、スコア・利益・案件単価・要員年齢・性別・要員単価が NULL
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 該当の項目を未設定にした1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 任意の項目がNULLの行の場合_未設定の項目を持つ1件の一覧を返す(final Connection connection) throws SQLException {
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
                    TENANT_ID,
                    "案件名",
                    null,
                    "山田太郎",
                    null,
                    null,
                    null)
                .build());
        SES_AI_T_MATCH_JOB_PERSONLot lot = new SES_AI_T_MATCH_JOB_PERSONLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getScore()).isNull();
        assertThat(lot.get(0).getProfit().getValue()).isNull();
        assertThat(lot.get(0).getJobUnitPrice().getValue()).isNull();
        assertThat(lot.get(0).getPersonAge()).isNull();
        assertThat(lot.get(0).getPersonGender()).isNull();
        assertThat(lot.get(0).getPersonUnitPrice().getValue()).isNull();
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントの結合結果を読まないことを確かめる。 他テナントの案件と要員の情報が漏えいする。
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

        new SES_AI_T_MATCH_JOB_PERSONLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectByJobIdPaged {

    @Nested
    class 正常系 {

      /**
       * 案件IDで件数を数えてから、指定のページの結合結果を読み込むことを確かめる。 案件ごとのマッチング一覧をページ送りで表示する。
       *
       * @given 件数が 1 件、SELECT の結果が 1 行、案件IDが "J001"、ページ番号が 1、ページサイズが 10
       * @when selectByJobIdPaged を接続、テナントID "T001" で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数が1件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, oneRow(TENANT_ID));
        SES_AI_T_MATCH_JOB_PERSONLot lot = new SES_AI_T_MATCH_JOB_PERSONLot();

        lot.selectByJobIdPaged(connection, TENANT_ID, "J001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 案件IDで絞り込んだページ検索のSQLで、ORDER句をWHERE句の後・LIMIT句の前に置き、件数の問い合わせには付けないことを確かめる。
       * ページ送りで並びが変わらないように順序を固定するためである。
       *
       * @given 件数が 1 件、SELECT の結果が 1 行、案件IDが "J001"、ページ番号が 1、ページサイズが 10
       * @when selectByJobIdPaged を接続、テナントID "T001" で実行する
       * @then 件数の問い合わせに ORDER 句を含めず、一覧の問い合わせは WHERE 句の後に登録日の降順と主キーのORDER句を置いたSQLを送信する
       */
      @MockDbConnection
      @Test
      void 案件IDの場合_WHERE句の後に順序を固定したORDER句を付けた一覧の問い合わせを送信する(final Connection connection)
          throws SQLException {
        stubCountAndRows(connection, 1L, oneRow(TENANT_ID));
        new SES_AI_T_MATCH_JOB_PERSONLot().selectByJobIdPaged(connection, TENANT_ID, "J001", 1, 10);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection, atLeastOnce()).prepareStatement(sql.capture());
        List<String> statements = sql.getAllValues();
        assertThat(statements.get(0)).doesNotContain("ORDER");
        assertThat(statements.get(0)).endsWith("WHERE m.job_id = ? AND m.tenant_id = ?");
        assertThat(statements.get(1))
            .endsWith(
                "WHERE m.job_id = ? AND m.tenant_id = ? ORDER BY m.register_date DESC, m.matching_id LIMIT ? OFFSET ?");
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま件数を数えられない。
       *
       * @given 接続が null、案件IDが "J001"
       * @when selectByJobIdPaged を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new SES_AI_T_MATCH_JOB_PERSONLot()
                        .selectByJobIdPaged(null, TENANT_ID, "J001", 1, 10))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが null の場合に検索を行わないことを確かめる。 キーの無い検索は全案件のマッチングを返す。
       *
       * @given 案件IDが null、テナントIDが "T001"
       * @when selectByJobIdPaged を接続、テナントID "T001"、案件ID null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 案件IDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_MATCH_JOB_PERSONLot().selectByJobIdPaged(connection, TENANT_ID, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class selectByPersonIdPaged {

    @Nested
    class 正常系 {

      /**
       * 要員IDで件数を数えてから、指定のページの結合結果を読み込むことを確かめる。 要員ごとのマッチング一覧をページ送りで表示する。
       *
       * @given 件数が 1 件、SELECT の結果が 1 行、要員IDが "P001"、ページ番号が 1、ページサイズが 10
       * @when selectByPersonIdPaged を接続、テナントID "T001" で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数が1件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, oneRow(TENANT_ID));
        SES_AI_T_MATCH_JOB_PERSONLot lot = new SES_AI_T_MATCH_JOB_PERSONLot();

        lot.selectByPersonIdPaged(connection, TENANT_ID, "P001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に検索を行わないことを確かめる。 キーの無い検索は全要員のマッチングを返す。
       *
       * @given 要員IDが null、テナントIDが "T001"
       * @when selectByPersonIdPaged を接続、テナントID "T001"、要員ID null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 要員IDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_MATCH_JOB_PERSONLot()
            .selectByPersonIdPaged(connection, TENANT_ID, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  /**
   * 件数の問い合わせと結果の問い合わせに、それぞれ別の準備文を返すように設定する。
   *
   * @param connection 接続のモック
   * @param count 件数
   * @param rows 行の結果セット
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static void stubCountAndRows(
      final Connection connection, final long count, final ResultSet rows) throws SQLException {
    PreparedStatement countStatement = mock(PreparedStatement.class);
    when(countStatement.executeQuery())
        .thenReturn(MockResultSetBuilder.builder().columns("cnt").row(count).build());
    PreparedStatement rowStatement = mock(PreparedStatement.class);
    when(rowStatement.executeQuery()).thenReturn(rows);
    when(connection.prepareStatement(anyString())).thenReturn(rowStatement);
    when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
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
        SES_AI_T_MATCH_JOB_PERSONLot lot = new SES_AI_T_MATCH_JOB_PERSONLot();

        lot.selectAllPaged(connection, "T001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }
}
