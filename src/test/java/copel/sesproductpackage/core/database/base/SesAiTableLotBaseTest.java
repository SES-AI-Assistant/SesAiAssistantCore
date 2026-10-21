package copel.sesproductpackage.core.database.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_T_JOB;
import copel.sesproductpackage.core.database.SES_AI_T_JOBLot;
import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.unit.Vector;
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

/** {@link SesAiTableLotBase} の単体テスト（具象クラスとして SES_AI_T_JOBLot を用いる）。 */
class SesAiTableLotBaseTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 取得結果の列距離を含まない。 */
  private static final String[] COLUMNS = {
    "job_id",
    "from_group",
    "from_id",
    "from_name",
    "mail_subject",
    "raw_content",
    "content_summary",
    "unit_price",
    "title",
    "overview",
    "must_skills",
    "want_skills",
    "start_date",
    "place",
    "area",
    "office_requirements",
    "other_requirements",
    "register_date",
    "register_user",
    "ttl",
    "tenant_id"
  };

  /** 取得結果の列距離を含む。 */
  private static final String[] COLUMNS_WITH_DISTANCE = {
    "job_id",
    "from_group",
    "from_id",
    "from_name",
    "mail_subject",
    "raw_content",
    "content_summary",
    "unit_price",
    "title",
    "overview",
    "must_skills",
    "want_skills",
    "start_date",
    "place",
    "area",
    "office_requirements",
    "other_requirements",
    "register_date",
    "register_user",
    "ttl",
    "tenant_id",
    "distance"
  };

  /**
   * 案件を1行分与える結果セットを作る。距離を含むかどうかを指定する。
   *
   * @param withDistance 距離の列を含める場合は true
   * @param tenantId テナントID
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet row(final boolean withDistance, final String tenantId)
      throws SQLException {
    List<Object> values =
        new java.util.ArrayList<>(
            List.of(
                "0123456789",
                "G001",
                "S001",
                "送信者",
                "件名",
                "原文",
                "要約",
                new BigDecimal(1_500_000),
                "Javaエンジニア",
                "概要",
                "Java",
                "AWS",
                "2026-02-01 00:00:00",
                "東京",
                "関東_首都圏",
                3,
                "その他",
                "2026-01-15 10:00:00",
                "U001",
                "2026-02-15 10:00:00",
                tenantId));
    if (withDistance) {
      values.add(0.1);
    }
    return MockResultSetBuilder.builder()
        .columns(withDistance ? COLUMNS_WITH_DISTANCE : COLUMNS)
        .row(values.toArray())
        .build();
  }

  /**
   * 件数の問い合わせと結果の問い合わせに、それぞれ別の準備文を返すように設定する。
   *
   * @param connection 接続のモック
   * @param count 件数
   * @param rows 行の結果セット
   * @return 件数の準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static PreparedStatement stubCountAndRows(
      final Connection connection, final long count, final ResultSet rows) throws SQLException {
    PreparedStatement countStatement = mock(PreparedStatement.class);
    when(countStatement.executeQuery())
        .thenReturn(MockResultSetBuilder.builder().columns("cnt").row(count).build());
    PreparedStatement rowStatement = mock(PreparedStatement.class);
    when(rowStatement.executeQuery()).thenReturn(rows);
    when(connection.prepareStatement(anyString())).thenReturn(rowStatement);
    when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
    return countStatement;
  }

  /**
   * 検索用のベクトルを生成する。
   *
   * @return 検索用のベクトル
   */
  /**
   * 単価・エリア・出社要件が NULL の案件の1行分の結果セットを作る。
   *
   * @param tenantId テナントID
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet rowWithNullOptionals(final String tenantId) throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            "0123456789",
            "G001",
            "S001",
            "送信者",
            "件名",
            "原文",
            "要約",
            null,
            "Javaエンジニア",
            "概要",
            "Java",
            "AWS",
            "2026-02-01 00:00:00",
            "東京",
            null,
            null,
            "その他",
            "2026-01-15 10:00:00",
            "U001",
            "2026-02-15 10:00:00",
            tenantId)
        .build();
  }

  private static Vector vector() {
    return new Vector(null);
  }

  /**
   * 案件IDを持つ案件を生成する。
   *
   * @param jobId 案件ID
   * @return 生成した案件
   */
  private static SES_AI_T_JOB jobOf(final String jobId) {
    SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
    job.setJobId(jobId);
    job.setRawContent("原文");
    return job;
  }

  @Nested
  class retrieveWithThreshold {

    @Nested
    class 正常系 {

      /**
       * ベクトルの類似検索で件数と行を読み込み、距離を設定することを確かめる。 類似度の高い案件を候補として並べる。
       *
       * @given 件数が 1 件、検索の結果が 1 行距離 0.1、ベクトルが設定済み
       * @when retrieveWithThreshold を接続、テナントID "T001"、類似度 0.8、件数 10 で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数が1件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(true, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveWithThreshold(connection, TENANT_ID, vector(), 0.8, 10);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getDistance()).isEqualTo(0.1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ベクトルが null の場合に検索を行わないことを確かめる。 検索の基準が無いと、全件を距離順に返してしまう。
       *
       * @given ベクトルが null
       * @when retrieveWithThreshold を接続、テナントID "T001" で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ベクトルがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_JOBLot().retrieveWithThreshold(connection, TENANT_ID, null, 0.8, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class retrievePagedWithThreshold {

    @Nested
    class 正常系 {

      /**
       * 件数の問い合わせが行を返さない場合に、件数を0のまま空の一覧を返すことを確かめる。 件数が取れないときは一覧の問い合わせを行わない。
       *
       * @given 件数の問い合わせの結果が 0 行
       * @when retrievePagedWithThreshold を接続、テナントID "T001"、ベクトル、類似度 0.8、1ページ目、1ページ10件で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数の結果が0行の場合_件数0の空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement rowStatement = mock(PreparedStatement.class);
        when(rowStatement.executeQuery())
            .thenReturn(MockResultSetBuilder.builder().columns("cnt").build());
        when(connection.prepareStatement(anyString())).thenReturn(rowStatement);
        PreparedStatement countStatement = mock(PreparedStatement.class);
        when(countStatement.executeQuery())
            .thenReturn(MockResultSetBuilder.builder().columns("cnt").build());
        when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrievePagedWithThreshold(connection, TENANT_ID, vector(), 0.8, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class searchByRawContent_Connection_String_String {

    @Nested
    class 正常系 {

      /**
       * 原文の全文検索で、検索語に一致する案件を1件の一覧にすることを確かめる。 キーワードで案件を探す。
       *
       * @given SELECT の結果が 1 行、検索語が "Java"、テナントID "T001"
       * @when searchByRawContent を接続、テナントID "T001"、検索語 "Java" で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 検索語がある場合_1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.searchByRawContent(connection, TENANT_ID, "Java");

        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 検索語が null の場合に DB を呼ばず、空の一覧を返すことを確かめる。 キーワードの無い検索は全件を返す。
       *
       * @given 検索語が null
       * @when searchByRawContent を接続、テナントID "T001"、検索語 null で実行する
       * @then 空の一覧を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 検索語がnullの場合_空の一覧を返す(final Connection connection) throws SQLException {
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.searchByRawContent(connection, TENANT_ID, null);

        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class searchByRawContent_Connection_String_String_List {

    @Nested
    class 正常系 {

      /**
       * 検索語と追加条件のない全文検索で、検索語に一致する案件を1件の一覧にすることを確かめる。 追加条件を省いた原文の検索も行えるようにする。
       *
       * @given SELECT の結果が 1 行、検索語が "Java"、追加条件が空、テナントID "T001"
       * @when searchByRawContent を接続、テナントID "T001"、検索語 "Java"、追加条件の空の一覧で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 検索語と空の追加条件がある場合_1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.searchByRawContent(connection, TENANT_ID, "Java", List.of());

        assertThat(lot.size()).isEqualTo(1);
      }
    }
  }

  @Nested
  class searchByRawContentPaged_Connection_String_String_int_int {

    @Nested
    class 正常系 {

      /**
       * 原文の全文検索を件数と行で読み込み、件数を保持することを確かめる。 ページ送りの件数を表示する。
       *
       * @given 件数が 1 件、検索の結果が 1 行、検索語が "Java"
       * @when searchByRawContentPaged を接続、テナントID "T001"、検索語 "Java"、ページ 1、サイズ 10 で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 検索語がある場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.searchByRawContentPaged(connection, TENANT_ID, "Java", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }
    }
  }

  @Nested
  class searchByRawContentPaged_Connection_String_String_List_int_int {

    @Nested
    class 正常系 {

      /**
       * 検索語と追加条件のない全文検索を件数と行で読み込むことを確かめる。 追加条件を省いたページ送りの検索も行えるようにする。
       *
       * @given 件数が 1 件、検索の結果が 1 行、検索語が "Java"、追加条件が空
       * @when searchByRawContentPaged を接続、テナントID "T001"、検索語 "Java"、追加条件の空の一覧、ページ 1、サイズ 10 で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 検索語と空の追加条件がある場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.searchByRawContentPaged(connection, TENANT_ID, "Java", List.of(), 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }
    }
  }

  @Nested
  class searchByRawContentPaged_Connection_String_List_int_int {

    @Nested
    class 正常系 {

      /**
       * 複数の全文検索条件で、件数と行を読み込むことを確かめる。 キーワードの組み合わせで案件を探す。
       *
       * @given 件数が 1 件、検索の結果が 1 行、キーワード "Java" の全文検索条件
       * @when searchByRawContentPaged を接続、テナントID "T001"、条件、ページ 1、サイズ 10 で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 全文検索の条件がある場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.searchByRawContentPaged(
            connection, TENANT_ID, List.of(new FulltextCondition("AND", "Java", false)), 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }
    }
  }
}
