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

import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.JobDetailFilterCondition;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
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

/** {@link SES_AI_T_JOBLot} の単体テスト。 */
class SES_AI_T_JOBLotTest {

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
  class getSelectAllSql {

    @Nested
    class 正常系 {

      /**
       * 全件の SQL が案件の表から全件を読む SELECT 文であることを確かめる。 全件の一覧を取得する基になる。
       *
       * @given 案件Lot
       * @when getSelectAllSql を実行する
       * @then SES_AI_T_JOB から全件を読む SELECT 文を返す
       */
      @Test
      void 全件の取得の場合_案件の表からのSELECT文を返す() {
        assertThat(new SES_AI_T_JOBLot().getSelectAllSql())
            .startsWith("SELECT")
            .contains("FROM SES_AI_T_JOB");
      }
    }
  }

  @Nested
  class getSelectLikeSql {

    @Nested
    class 正常系 {

      /**
       * 全文検索の SQL が原文の LIKE 条件を持つことを確かめる。 原文の全文検索の基になる。
       *
       * @given 案件Lot
       * @when getSelectLikeSql を実行する
       * @then 原文の LIKE 条件を持つ SELECT 文を返す
       */
      @Test
      void 全文検索の場合_原文のLIKE条件を含むSELECT文を返す() {
        assertThat(new SES_AI_T_JOBLot().getSelectLikeSql()).contains("raw_content LIKE ?");
      }
    }
  }

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントの案件を全件読み込み、項目を変換することを確かめる。 画面の一覧は自テナントの案件だけを表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、単価 1500000
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 案件IDが "0123456789" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_変換した1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getJobId()).isEqualTo("0123456789");
        assertThat(lot.get(0).getUnitPrice().getValue())
            .isEqualByComparingTo(new BigDecimal(1_500_000));
      }

      /**
       * 単価・エリア・出社要件が NULL の行を読み込み、対応する項目を未設定にすることを確かめる。 未入力の案件を一覧で壊さない。
       *
       * @given SELECT の結果が 1 行、単価・エリア・出社要件が NULL
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 単価・エリア・出社要件を未設定にした1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 任意の項目がNULLの行の場合_未設定の項目を持つ1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, rowWithNullOptionals(TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getUnitPrice().getValue()).isNull();
        assertThat(lot.get(0).getArea()).isNull();
        assertThat(lot.get(0).getOfficeRequirements()).isNull();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントの案件を返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_T_JOBLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントの案件を読まないことを確かめる。 他テナントの案件が一覧に出ると、契約社の情報が漏えいする。
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

        new SES_AI_T_JOBLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントの案件を読み込むことを確かめる。 バッチ処理は全テナントの案件を対象にする。
       *
       * @given SELECT の結果が 1 行テナントID "T002"
       * @when selectAllWithoutTenantFilter を接続で実行する
       * @then テナントIDが "T002" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false, "T002"));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

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
        assertThatThrownBy(() -> new SES_AI_T_JOBLot().selectAllWithoutTenantFilter(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class retrieveWithFilter_Connection_String_Vector_Money_OriginalDateTime_double_int {

    @Nested
    class 正常系 {

      /**
       * 価格、開始日、出社要件、エリアで絞り込んだ類似検索で、件数と行を読み込むことを確かめる。 出社要件とエリアが揃う案件を候補として並べる。
       *
       * @given 件数が 1 件、検索の結果が 1 行、出社要件が 3、エリアが 関東_首都圏
       * @when retrieveWithFilter を価格、開始日、出社要件、エリアで実行する
       * @then 検索の SQL に出社要件 0 の判定を含め、件数の問い合わせの 3 番目に 3、4 番目に 関東_首都圏 を設定し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 出社要件とエリアの条件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = stubCountAndRows(connection, 1L, row(true, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            3,
            Area.関東_首都圏,
            0.8,
            10);

        verify(connection, atLeastOnce())
            .prepareStatement(contains("(office_requirements = 0 OR area = ?)"));
        verify(countStatement).setInt(3, 3);
        verify(countStatement).setString(4, Area.関東_首都圏.name());
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 件数の問い合わせにテナントIDを条件として渡すことを確かめる。 他テナントの案件を件数に含めない。
       *
       * @given 件数が 0 件、テナントID "T001"
       * @when retrieveWithFilter を価格と開始日のみで実行する
       * @then 件数の条件の5番目に "T001" を設定し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_件数の条件にT001を設定して空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement =
            stubCountAndRows(
                connection, 0L, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            0.8,
            10);

        assertThat(lot.isEmpty()).isTrue();
        verify(countStatement).setString(5, TENANT_ID);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ベクトルが null の場合に検索を行わないことを確かめる。 検索の基準が無いと全件を返してしまう。
       *
       * @given ベクトルが null
       * @when retrieveWithFilter を接続、テナントID "T001"、ベクトル null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ベクトルがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_JOBLot()
            .retrieveWithFilter(
                connection,
                TENANT_ID,
                null,
                new Money(1_000_000L),
                new OriginalDateTime("2026-01-01 00:00:00"),
                0.8,
                10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class retrieveWithFilter_Connection_String_Vector_Money_OriginalDateTime_int_Area_double_int {

    @Nested
    class 正常系 {

      /**
       * 価格と開始日で絞り込んだ類似検索で、件数と行を読み込むことを確かめる。 条件に合う案件を候補として並べる。
       *
       * @given 件数が 1 件、検索の結果が 1 行、単価の上限が 1000000 円、開始日が 2026-01-01
       * @when retrieveWithFilter を価格と開始日のみで実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 価格と開始日の条件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(true, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            0.8,
            10);

        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 要員の単価が応相談の場合に、単価で案件を絞り込まないことを確かめる。 単価を提案時に決める要員は、案件の単価が低いことを理由に候補から外さない。
       *
       * @given 件数が 1 件、検索の結果が 1 行、要員の単価が 0 円、開始日が 2026-01-01
       * @when retrieveWithFilter を接続、テナントID "T001"、単価 0 円、出社要件 3、エリア 関東_首都圏 で実行する
       * @then SQL に単価が未設定か 0 以下の案件を通す条件と COALESCE を含め、件数の問い合わせの 1 番目に null を設定し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 要員の単価が0円の場合_単価を絞り込まず1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = stubCountAndRows(connection, 1L, row(true, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(0L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            3,
            Area.関東_首都圏,
            0.8,
            10);

        verify(connection, atLeastOnce())
            .prepareStatement(
                contains("(unit_price IS NULL OR unit_price <= 0 OR unit_price = 9990000 OR"));
        verify(countStatement).setBigDecimal(1, null);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 要員の開始日が未設定の場合に、開始日で案件を絞り込まないことを確かめる。 開始日が分からない要員を、開始日の条件で一律に候補から外さない。
       *
       * @given 件数が 1 件、検索の結果が 1 行、要員の開始日が未設定、単価が 1000000 円
       * @when retrieveWithFilter を接続、テナントID "T001"、開始日 未設定、出社要件 3、エリア 関東_首都圏 で実行する
       * @then SQL に開始日の COALESCE の条件を含め、件数の問い合わせの 2 番目に null を設定し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 要員の開始日が未設定の場合_開始日を絞り込まず1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = stubCountAndRows(connection, 1L, row(true, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime((java.sql.Timestamp) null),
            3,
            Area.関東_首都圏,
            0.8,
            10);

        verify(connection, atLeastOnce())
            .prepareStatement(
                contains("COALESCE(start_date, 'infinity'::timestamp) <= COALESCE(?::timestamp,"));
        verify(countStatement).setTimestamp(2, null);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 要員のエリアが無くても、フルリモートの案件を検索の候補に含めることを確かめる。 出社要件が 0 の案件は場所を問わないため、エリアが無いことで候補を落とさない。
       *
       * @given 件数が 1 件、検索の結果が 1 行、要員のオフィス可用性が 3、エリアが null
       * @when retrieveWithFilter を接続、テナントID "T001"、オフィス可用性 3、エリア null で実行する
       * @then 検索の SQL に出社要件 0 の判定を含め、件数の問い合わせの 3 番目に 3、4 番目に null を設定し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void エリアがnullで出社要件0の判定を含む場合_1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = stubCountAndRows(connection, 1L, row(true, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            3,
            null,
            0.8,
            10);

        verify(connection, atLeastOnce())
            .prepareStatement(contains("(office_requirements = 0 OR area = ?)"));
        verify(countStatement).setInt(3, 3);
        verify(countStatement).setString(4, null);
        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ベクトルが null の場合に検索を行わないことを確かめる。 検索の基準が無いと全件を返してしまう。
       *
       * @given ベクトルが null、エリアが関東_首都圏
       * @when retrieveWithFilter を接続、テナントID "T001"、ベクトル null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ベクトルがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_JOBLot()
            .retrieveWithFilter(
                connection,
                TENANT_ID,
                null,
                new Money(1_000_000L),
                new OriginalDateTime("2026-01-01 00:00:00"),
                3,
                Area.関東_首都圏,
                0.8,
                10);

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 要員のエリアが無い場合に、エリアの条件に NULL を設定することを確かめる。 エリアの一致が必要な案件を、エリアの無い要員と照合しないため NULL を渡して一致させない。
       *
       * @given 件数の問い合わせが 0 行を返す、出社許容が 3、エリアが null
       * @when retrieveWithFilter を接続、テナントID "T001"、出社許容 3、エリア null で実行する
       * @then 件数の問い合わせの 3 番目に 3、4 番目に null を設定し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void エリアがnullの場合_エリアの条件にnullを設定して空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement =
            stubCountAndRows(
                connection, 0L, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            3,
            null,
            0.8,
            10);

        assertThat(lot.isEmpty()).isTrue();
        verify(countStatement).setInt(3, 3);
        verify(countStatement).setString(4, null);
      }
    }
  }

  @Nested
  class getEntityByPk {

    @Nested
    class 正常系 {

      /**
       * 案件IDが一致する案件を一覧から返すことを確かめる。 選出された案件の詳細を取り出す。
       *
       * @given 一覧に案件ID "J001" と "J002" の案件がある
       * @when getEntityByPk を "J002" で実行する
       * @then 案件ID "J002" の案件を返す
       */
      @Test
      void 案件IDが一致する要素がある場合_該当の案件を返す() {
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
        lot.add(jobOf("J001"));
        lot.add(jobOf("J002"));

        assertThat(lot.getEntityByPk("J002").getJobId()).isEqualTo("J002");
      }

      /**
       * 案件IDが一致する要素が無い場合に null を返すことを確かめる。 存在しない案件を取り出すと誤った詳細を表示する。
       *
       * @given 一覧に案件ID "J001" の案件がある
       * @when getEntityByPk を "J999" で実行する
       * @then nullを返す
       */
      @Test
      void 案件IDが一致する要素が無い場合_nullを返す() {
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
        lot.add(jobOf("J001"));

        assertThat(lot.getEntityByPk("J999")).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが null の場合に null を返すことを確かめる。 キーの無い検索は先頭の案件を誤って返す。
       *
       * @given 一覧に案件ID "J001" の案件がある
       * @when getEntityByPk を null で実行する
       * @then nullを返す
       */
      @Test
      void 案件IDがnullの場合_nullを返す() {
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
        lot.add(jobOf("J001"));

        assertThat(lot.getEntityByPk(null)).isNull();
      }
    }
  }

  @Nested
  class to案件選出用文章 {

    @Nested
    class 正常系 {

      /**
       * 一覧の各案件を人目の番号を付けて連結した文章を返すことを確かめる。 LLM に候補を番号で示す。
       *
       * @given 一覧に案件ID "J001" と "J002" の案件が2件ある
       * @when to案件選出用文章 を実行する
       * @then 1人目と2人目の番号を付けた文章を返す
       */
      @Test
      void 一覧に2件ある場合_番号を付けて連結した文章を返す() {
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
        lot.add(jobOf("J001"));
        lot.add(jobOf("J002"));

        assertThat(lot.to案件選出用文章()).startsWith("1人目：").contains("2人目：");
      }
    }
  }

  @Nested
  class searchByJobWithDetailFilter {

    @Nested
    class 正常系 {

      /**
       * 全文検索の条件と詳細条件出社要件を組み合わせて件数と行を読み込むことを確かめる。 条件に合う案件だけを候補にする。
       *
       * @given 件数が 1 件、検索の結果が 1 行、キーワード "Java"、出社要件が 3
       * @when searchByJobWithDetailFilter を接続、テナントID "T001"、条件、詳細条件で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 詳細条件がある場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false, TENANT_ID));
        JobDetailFilterCondition detail = new JobDetailFilterCondition();
        detail.setOfficeRequirements(3);
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.searchByJobWithDetailFilter(
            connection,
            TENANT_ID,
            List.of(new FulltextCondition("AND", "Java", false)),
            detail,
            1,
            10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 詳細条件が null の場合に全文検索の条件だけで件数と行を読み込むことを確かめる。 詳細条件を省いた検索も行えるようにする。
       *
       * @given 件数が 1 件、検索の結果が 1 行、詳細条件が null
       * @when searchByJobWithDetailFilter を接続、テナントID "T001"、条件、詳細条件 null で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 詳細条件がnullの場合_全文検索の条件だけで1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.searchByJobWithDetailFilter(
            connection,
            TENANT_ID,
            List.of(new FulltextCondition("AND", "Java", false)),
            null,
            1,
            10);

        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 全文検索の条件が null の場合に検索を行わないことを確かめる。 条件の無い検索は全件の一覧になるおそれがある。
       *
       * @given 全文検索の条件が null
       * @when searchByJobWithDetailFilter を接続、テナントID "T001"、詳細条件 null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 全文検索の条件がnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_JOBLot().searchByJobWithDetailFilter(connection, TENANT_ID, null, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 全文検索の条件が空の場合に検索を行わないことを確かめる。 キーワードの無い検索は条件を組み立てられない。
       *
       * @given 全文検索の条件が空のリスト
       * @when searchByJobWithDetailFilter を接続、テナントID "T001"、詳細条件 null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 全文検索の条件が空の場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_JOBLot()
            .searchByJobWithDetailFilter(connection, TENANT_ID, List.of(), null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class retrieveByJobVectorWithDetailFilter {

    @Nested
    class 正常系 {

      /**
       * ベクトルの類似検索と詳細条件最低単価を組み合わせて件数と行を読み込むことを確かめる。 条件に合う案件を類似度の高い順に並べる。
       *
       * @given 件数が 1 件、検索の結果が 1 行距離 0.1、最低単価が 1000000 円
       * @when retrieveByJobVectorWithDetailFilter を接続、テナントID "T001"、類似度 0.8、詳細条件で実行する
       * @then 件数に 1 を保持し、距離 0.1 を持つ1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 詳細条件がある場合_件数を保持して距離を持つ1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(true, TENANT_ID));
        JobDetailFilterCondition detail = new JobDetailFilterCondition();
        detail.setMinPrice(new Money(1_000_000L));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveByJobVectorWithDetailFilter(
            connection, TENANT_ID, vector(), 0.8, detail, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getDistance()).isEqualTo(0.1);
      }

      /**
       * 詳細条件が null の場合にベクトルの検索だけで1件の一覧を返すことを確かめる。 詳細条件を省いた類似検索も行えるようにする。
       *
       * @given 件数が 1 件、検索の結果が 1 行、詳細条件が null
       * @when retrieveByJobVectorWithDetailFilter を接続、テナントID "T001"、類似度 0.8、詳細条件 null で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 詳細条件がnullの場合_ベクトルの検索だけで1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(true, TENANT_ID));
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveByJobVectorWithDetailFilter(connection, TENANT_ID, vector(), 0.8, null, 1, 10);

        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 件数の問い合わせが行を返さない場合に、件数を0のまま空の一覧を返すことを確かめる。 件数が取れないときは一覧の問い合わせを行わない。
       *
       * @given 件数の問い合わせの結果が 0 行
       * @when retrieveByJobVectorWithDetailFilter を接続、テナントID "T001"、類似度 0.8、詳細条件 null で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数の結果が0行の場合_件数0の空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = mock(PreparedStatement.class);
        when(countStatement.executeQuery())
            .thenReturn(MockResultSetBuilder.builder().columns("cnt").build());
        when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

        lot.retrieveByJobVectorWithDetailFilter(connection, TENANT_ID, vector(), 0.8, null, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ベクトルが null の場合に検索を行わないことを確かめる。 検索の基準が無いと、全件を距離順に返してしまう。
       *
       * @given ベクトルが null
       * @when retrieveByJobVectorWithDetailFilter を接続、テナントID "T001" で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ベクトルがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_JOBLot()
            .retrieveByJobVectorWithDetailFilter(connection, TENANT_ID, null, 0.8, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }
}
