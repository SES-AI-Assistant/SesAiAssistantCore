package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.PersonDetailFilterCondition;
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

/** {@link SES_AI_T_SKILLSHEET_PERSONLot} の単体テスト。 */
class SES_AI_T_SKILLSHEET_PERSONLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 要員ID。 */
  private static final String PERSON_ID = "P001";

  /** 取得結果の列距離を含む、結合の結果の列の順。 */
  private static final String[] COLUMNS_WITH_DISTANCE = {
    "file_id",
    "file_name",
    "file_content_summary",
    "person_id",
    "mail_subject",
    "raw_content",
    "content_summary",
    "register_date",
    "register_user",
    "unit_price",
    "from_group",
    "from_id",
    "from_name",
    "name",
    "age",
    "gender",
    "nationality",
    "start_date",
    "place",
    "area",
    "office_availability",
    "organization",
    "experiences",
    "ng_requirements",
    "other_requirements",
    "url",
    "distance",
    "tenant_id"
  };

  /** 取得結果の列距離を含まない。 */
  private static final String[] COLUMNS = {
    "file_id",
    "file_name",
    "file_content_summary",
    "person_id",
    "mail_subject",
    "raw_content",
    "content_summary",
    "register_date",
    "register_user",
    "unit_price",
    "from_group",
    "from_id",
    "from_name",
    "name",
    "age",
    "gender",
    "nationality",
    "start_date",
    "place",
    "area",
    "office_availability",
    "organization",
    "experiences",
    "ng_requirements",
    "other_requirements",
    "url",
    "tenant_id"
  };

  /**
   * 結合の結果を1行分与える結果セットを作る。距離の列を含むかどうかを指定する。
   *
   * @param withDistance 距離の列を含める場合は true
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet row(final boolean withDistance) throws SQLException {
    MockResultSetBuilder builder =
        MockResultSetBuilder.builder().columns(withDistance ? COLUMNS_WITH_DISTANCE : COLUMNS);
    List<Object> values =
        new java.util.ArrayList<>(
            List.of(
                "F001",
                "skill.xlsx",
                "ファイル要約",
                PERSON_ID,
                "件名",
                "原文",
                "要約",
                java.sql.Timestamp.valueOf("2026-01-15 10:00:00"),
                "U001",
                new BigDecimal(600_000),
                "G001",
                "S001",
                "送信者",
                "山田太郎",
                35,
                "Man",
                "日本",
                java.sql.Timestamp.valueOf("2026-02-01 00:00:00"),
                "東京",
                "関東_首都圏",
                3,
                "組織",
                "経験1",
                "NG1",
                "その他1",
                "https://example.com"));
    if (withDistance) {
      values.add(0.1);
    }
    values.add(TENANT_ID);
    return builder.row(values.toArray()).build();
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
  private static Vector vector() {
    return new Vector(null);
  }

  @Nested
  class getSelectAllSql {

    @Nested
    class 正常系 {

      /**
       * 全件の SQL がテナントの条件を付けられる SELECT 文であることを確かめる。 全件の一覧を取得する基になる。
       *
       * @given 読み取り専用ではない結合Lot
       * @when getSelectAllSql を実行する
       * @then 要員とスキルシートの結合を含む SELECT 文を返す
       */
      @Test
      void 全件の取得の場合_結合を含むSELECT文を返す() {
        assertThat(new SES_AI_T_SKILLSHEET_PERSONLot().getSelectAllSql())
            .startsWith("SELECT")
            .contains("FROM SES_AI_T_SKILLSHEET s");
      }
    }
  }

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントの結合の結果を全件読み込み、項目を変換することを確かめる。 画面の一覧は自テナントの要員だけを表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、性別 "Man"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 要員IDが "P001"、性別が Man の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_変換した1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getPersonId()).isEqualTo(PERSON_ID);
        assertThat(lot.get(0).getGender().name()).isEqualTo("Man");
      }

      /**
       * 検索の条件にテナントIDを渡して、他テナントの要員を読まないことを確かめる。 他テナントの要員の情報が漏えいする。
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

        new SES_AI_T_SKILLSHEET_PERSONLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントの要員を返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_T_SKILLSHEET_PERSONLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントの要員を読み込むことを確かめる。 バッチ処理は全テナントの要員を対象にする。
       *
       * @given SELECT の結果が 1 行
       * @when selectAllWithoutTenantFilter を接続で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.selectAllWithoutTenantFilter(connection);

        assertThat(lot.size()).isEqualTo(1);
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
                () -> new SES_AI_T_SKILLSHEET_PERSONLot().selectAllWithoutTenantFilter(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class retrieveOuterJoinByPersonVector {

    @Nested
    class 正常系 {

      /**
       * ベクトルの類似検索で、距離を含む結合の結果を読み込むことを確かめる。 類似度の高い要員を候補として並べる。
       *
       * @given 検索の結果が 1 行距離 0.1、ベクトルが設定済み、件数の上限が 10
       * @when retrieveOuterJoinByPersonVector を接続、テナントID "T001"、類似度 0.8、件数 10 で実行する
       * @then 1件の一覧を返し、要員IDが "P001" になる
       */
      @MockDbConnection
      @Test
      void 検索の結果が1行の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(true));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveOuterJoinByPersonVector(connection, TENANT_ID, vector(), 0.8, 10);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getPersonId()).isEqualTo(PERSON_ID);
      }

      /**
       * 検索の条件にテナントIDを渡し、件数と開始位置を上限に合わせることを確かめる。 他テナントの要員を候補にしない。
       *
       * @given 検索の結果が 1 行、テナントID "T001"、件数の上限が 10
       * @when retrieveOuterJoinByPersonVector を接続、テナントID "T001"、類似度 0.8、件数 10 で実行する
       * @then テナントIDの位置に "T001"、件数の位置に 10、開始位置に 0 を設定し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_テナントと件数と開始位置を設定して1件の一覧を返す(final Connection connection)
          throws SQLException {
        PreparedStatement statement = JdbcMocks.stubQuery(connection, row(true));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveOuterJoinByPersonVector(connection, TENANT_ID, vector(), 0.8, 10);

        verify(statement).setString(4, TENANT_ID);
        verify(statement).setInt(5, 10);
        verify(statement).setInt(6, 0);
        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ベクトルが null の場合に検索を行わないことを確かめる。 検索の基準が無いと、全件を距離順に返してしまう。
       *
       * @given ベクトルが null
       * @when retrieveOuterJoinByPersonVector を接続、テナントID "T001" で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ベクトルがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_SKILLSHEET_PERSONLot()
            .retrieveOuterJoinByPersonVector(connection, TENANT_ID, null, 0.8, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class retrieveByPersonVectorWithFilter_Connection_String_Vector_Money_OriginalDateTime_double_int {

    @Nested
    class 正常系 {

      /**
       * 出社要件が1以上でエリアが無い案件は、エリアの条件に NULL を設定することを確かめる。 エリアの一致が必要な案件を、エリアの無い要員と照合しないため、一致しない NULL
       * を渡す。
       *
       * @given 件数の問い合わせが 0 行を返す、出社要件が 3、エリアが null
       * @when retrieveByPersonVectorWithFilter を価格、開始日、出社要件 3、エリア null で実行する
       * @then 件数の問い合わせの 3・4 番目に出社要件 3 を設定し、5 番目に null を設定して空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 出社要件が1以上でエリアがnullの場合_エリアの条件にnullを設定して空の一覧を返す(final Connection connection)
          throws SQLException {
        PreparedStatement countStatement =
            stubCountAndRows(
                connection, 0L, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithFilter(
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
        verify(countStatement).setInt(4, 3);
        verify(countStatement).setString(5, null);
      }

      /**
       * フルリモートの案件は、エリアの条件を要員のエリアの一致で絞らないことを確かめる。 出社要件が 0 の案件は場所を問わないため、要員のエリアで候補を落とさない。
       *
       * @given 案件の出社要件が 0、エリアが 関東_首都圏
       * @when retrieveByPersonVectorWithFilter を価格、開始日、出社要件 0、エリア 関東_首都圏 で実行する
       * @then 検索の SQL に出社要件 0 の判定を含め、件数の問い合わせの 3・4 番目に 0 を設定し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void フルリモートの案件でエリアがある場合_出社要件0の判定を含む1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = stubCountAndRows(connection, 1L, row(true));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            0,
            Area.関東_首都圏,
            0.8,
            10);

        verify(connection, atLeastOnce()).prepareStatement(contains("(? = 0 OR p.area = ?)"));
        verify(countStatement).setInt(3, 0);
        verify(countStatement).setInt(4, 0);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * フルリモートの案件でエリアが無い場合も、エリアの条件で候補を落とさないことを確かめる。 出社要件が 0 の案件は場所を問わないため、エリアが無くても候補にする。
       *
       * @given 案件の出社要件が 0、エリアが null
       * @when retrieveByPersonVectorWithFilter を価格、開始日、出社要件 0、エリア null で実行する
       * @then 件数の問い合わせの 3・4 番目に 0 を設定し、5 番目に null を設定して1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void フルリモートの案件でエリアがnullの場合_出社要件0の判定を含む1件の一覧を返す(final Connection connection)
          throws SQLException {
        PreparedStatement countStatement = stubCountAndRows(connection, 1L, row(true));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            0,
            null,
            0.8,
            10);

        verify(countStatement).setInt(3, 0);
        verify(countStatement).setInt(4, 0);
        verify(countStatement).setString(5, null);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 一覧の問い合わせで、SELECT 句の距離の計算と類似度の条件の2か所にベクトルを渡すことを確かめる。 先頭を飛ばすと以降の値が1つずつずれ、実際の DB で numeric から
       * vector にキャストできないエラーになる。
       *
       * @given 件数が 1 件、オフィス要件 3、エリアが関東_首都圏
       * @when retrieveByPersonVectorWithFilter を全ての条件で実行する
       * @then 一覧の問い合わせの 1・7 番目にベクトル、4・5 番目に出社要件 3、6 番目にエリア、8 番目に類似度を設定し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 出社要件とエリアがある場合_一覧の問い合わせの先頭と類似度の条件にベクトルを設定した一覧を返す(final Connection connection)
          throws SQLException {
        PreparedStatement countStatement = mock(PreparedStatement.class);
        when(countStatement.executeQuery())
            .thenReturn(MockResultSetBuilder.builder().columns("cnt").row(1L).build());
        PreparedStatement rowStatement = mock(PreparedStatement.class);
        when(rowStatement.executeQuery()).thenReturn(row(true));
        when(connection.prepareStatement(anyString())).thenReturn(rowStatement);
        when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
        String vectorStr = vector().toString();

        new SES_AI_T_SKILLSHEET_PERSONLot()
            .retrieveByPersonVectorWithFilter(
                connection,
                TENANT_ID,
                vector(),
                new Money(1_000_000L),
                new OriginalDateTime("2026-01-01 00:00:00"),
                3,
                Area.関東_首都圏,
                0.8,
                10);

        verify(rowStatement).setString(1, vectorStr);
        verify(rowStatement).setBigDecimal(eq(2), any(BigDecimal.class));
        verify(rowStatement).setInt(4, 3);
        verify(rowStatement).setInt(5, 3);
        verify(rowStatement).setString(6, Area.関東_首都圏.name());
        verify(rowStatement).setString(7, vectorStr);
        verify(rowStatement).setDouble(8, 0.8);
        verify(rowStatement).setString(9, TENANT_ID);
        verify(rowStatement).setInt(10, 10);
        verify(rowStatement).setInt(11, 0);
      }

      /**
       * 件数の問い合わせにテナントIDを条件として渡すことを確かめる。 他テナントの要員を件数に含めない。
       *
       * @given 件数が 0 件、テナントID "T001"
       * @when retrieveByPersonVectorWithFilter を価格と開始日のみで実行する
       * @then 件数の条件の5番目に "T001" を設定し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_件数の条件にT001を設定して空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement =
            stubCountAndRows(
                connection, 0L, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithFilter(
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
       * ベクトルが null の場合に検索を行わないことを確かめる。 検索の基準が無いと、全件を距離順に返してしまう。
       *
       * @given ベクトルが null
       * @when retrieveByPersonVectorWithFilter を価格と開始日のみで実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ベクトルがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_SKILLSHEET_PERSONLot()
            .retrieveByPersonVectorWithFilter(
                connection,
                TENANT_ID,
                null,
                new Money(1_000_000L),
                new OriginalDateTime("2026-01-01 00:00:00"),
                0.8,
                10);

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 件数の問い合わせが行を返さない場合に、件数を0のまま空の一覧を返すことを確かめる。 件数が取れないときは一覧の問い合わせを行わない。
       *
       * @given 件数の問い合わせの結果が 0 行
       * @when retrieveByPersonVectorWithFilter を接続、テナントID "T001"、価格と開始日の条件で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数の結果が0行の場合_件数0の空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = mock(PreparedStatement.class);
        when(countStatement.executeQuery())
            .thenReturn(MockResultSetBuilder.builder().columns("cnt").build());
        when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            0.8,
            10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class retrieveByPersonVectorWithFilter_Connection_String_Vector_Money_OriginalDateTime_int_Area_double_int {

    @Nested
    class 正常系 {

      /**
       * 価格と開始日で絞り込んだ類似検索の件数と行を読み込むことを確かめる。 条件に合う要員を候補として並べる。
       *
       * @given 件数が 1 件、検索の結果が 1 行、単価の上限が 1000000 円、開始日が 2026-01-01
       * @when retrieveByPersonVectorWithFilter を価格と開始日のみで実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 価格と開始日の条件の場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(true));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            0.8,
            10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 価格と開始日とオフィス要件とエリアで絞り込んだ類似検索の件数と行を読み込むことを確かめる。 出社要件とエリアが揃う要員を候補として並べる。
       *
       * @given 件数が 1 件、検索の結果が 1 行、オフィス要件が 3、エリアが 関東_首都圏
       * @when retrieveByPersonVectorWithFilter を価格、開始日、オフィス要件、エリアで実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 価格と開始日と出社要件とエリアの条件の場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = stubCountAndRows(connection, 1L, row(true));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            3,
            Area.関東_首都圏,
            0.8,
            10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
        verify(countStatement).setInt(3, 3);
        verify(countStatement).setInt(4, 3);
        verify(countStatement).setString(5, Area.関東_首都圏.name());
      }
    }

    @Nested
    class 境界値 {

      /**
       * 件数が0件の場合に、一覧の問い合わせを行わず空の一覧を返すことを確かめる。 対象が無い条件で不要な検索を行わない。
       *
       * @given 件数の問い合わせが 0 を返す
       * @when retrieveByPersonVectorWithFilter を接続、テナントID "T001"、4つの条件で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数が0件の場合_件数0の空の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 0L, row(false));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithFilter(
            connection,
            TENANT_ID,
            vector(),
            new Money(1_000_000L),
            new OriginalDateTime("2026-01-01 00:00:00"),
            3,
            Area.関東_首都圏,
            0.8,
            10);

        assertThat(lot.getTotalCount()).isEqualTo(0L);
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class searchByPersonOrSkillSheetSummaryPaged {

    @Nested
    class 正常系 {

      /**
       * 全文検索の条件で件数と行を読み込むことを確かめる。 キーワードに合う要員を一覧に表示する。
       *
       * @given 件数が 1 件、検索の結果が 1 行、キーワード "山田" の全文検索条件
       * @when searchByPersonOrSkillSheetSummaryPaged を接続、テナントID "T001"、条件、ページ 1、サイズ 10 で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 全文検索の条件がある場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.searchByPersonOrSkillSheetSummaryPaged(
            connection, TENANT_ID, List.of(new FulltextCondition("AND", "山田", false)), 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 全文検索の条件で、要員の原文に絞り込んだ結合の検索を読み込むことを確かめる。 原文の一致する要員を確実に候補にする。
       *
       * @given 件数が 1 件、検索の結果が 1 行、キーワード "山田" の全文検索条件
       * @when searchByPersonOrSkillSheetSummaryInnerJoinPaged を接続、テナントID "T001"、条件、ページ 1、サイズ 10
       *     で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 原文の全文検索の条件がある場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.searchByPersonOrSkillSheetSummaryInnerJoinPaged(
            connection, TENANT_ID, List.of(new FulltextCondition("AND", "山田", false)), 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }
    }
  }

  @Nested
  class searchByPersonOrSkillSheetSummaryWithDetailFilter {

    @Nested
    class 正常系 {

      /**
       * 全文検索の条件と詳細条件年齢の下限を組み合わせて件数と行を読み込むことを確かめる。 条件に合う要員だけを候補にする。
       *
       * @given 件数が 1 件、検索の結果が 1 行、キーワード "山田"、年齢の下限が 30
       * @when searchByPersonOrSkillSheetSummaryWithDetailFilter を接続、テナントID "T001"、条件、詳細条件で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 詳細条件がある場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false));
        PersonDetailFilterCondition detail = new PersonDetailFilterCondition();
        detail.setMinAge(30);
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.searchByPersonOrSkillSheetSummaryWithDetailFilter(
            connection,
            TENANT_ID,
            List.of(new FulltextCondition("AND", "山田", false)),
            detail,
            1,
            10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 詳細条件が無い場合に全文検索の条件だけで件数と行を読み込むことを確かめる。 詳細条件を省いた検索も行えるようにする。
       *
       * @given 件数が 1 件、検索の結果が 1 行、キーワード "山田"、詳細条件が null
       * @when searchByPersonOrSkillSheetSummaryWithDetailFilter を接続、テナントID "T001"、条件、詳細条件 null
       *     で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 詳細条件がnullの場合_全文検索の条件だけで1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(false));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.searchByPersonOrSkillSheetSummaryWithDetailFilter(
            connection, TENANT_ID, List.of(new FulltextCondition("AND", "山田", false)), null, 1, 10);

        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 全文検索の条件が無い場合に DB を呼ばないことを確かめる。 キーワードの無い検索は全要員を返す。
       *
       * @given 全文検索の条件が空
       * @when searchByPersonOrSkillSheetSummaryWithDetailFilter を接続、テナントID "T001" で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 全文検索の条件が空の場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_SKILLSHEET_PERSONLot()
            .searchByPersonOrSkillSheetSummaryWithDetailFilter(
                connection, TENANT_ID, List.of(), null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 全文検索の条件が null の場合に検索を行わないことを確かめる。 条件の無い検索は全件の一覧になるおそれがある。
       *
       * @given 全文検索の条件が null
       * @when searchByPersonOrSkillSheetSummaryWithDetailFilter を接続、テナントID "T001"、詳細条件 null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 全文検索の条件がnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_SKILLSHEET_PERSONLot()
            .searchByPersonOrSkillSheetSummaryWithDetailFilter(
                connection, TENANT_ID, null, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class retrieveByPersonVectorWithDetailFilter {

    @Nested
    class 正常系 {

      /**
       * ベクトルの類似検索と詳細条件性別を組み合わせて件数と行を読み込むことを確かめる。 条件に合う要員を類似度の高い順に並べる。
       *
       * @given 件数が 1 件、検索の結果が 1 行、性別の条件が Man
       * @when retrieveByPersonVectorWithDetailFilter を接続、テナントID "T001"、類似度 0.8、詳細条件で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 詳細条件がある場合_件数を保持して1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(true));
        PersonDetailFilterCondition detail = new PersonDetailFilterCondition();
        detail.setGender("Man");
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithDetailFilter(
            connection, TENANT_ID, vector(), 0.8, detail, 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(1L);
        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 詳細条件が null の場合にベクトルの検索だけで件数と行を読み込むことを確かめる。 詳細条件を省いた類似検索も行えるようにする。
       *
       * @given 件数が 1 件、検索の結果が 1 行、詳細条件が null
       * @when retrieveByPersonVectorWithDetailFilter を接続、テナントID "T001"、類似度 0.8、詳細条件 null で実行する
       * @then 件数に 1 を保持し、1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 詳細条件がnullの場合_ベクトルの検索だけで1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, row(true));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithDetailFilter(
            connection, TENANT_ID, vector(), 0.8, null, 1, 10);

        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 件数の問い合わせが0件の場合に、件数を0のまま空の一覧を返すことを確かめる。 件数が取れないときは一覧の問い合わせを行わない。
       *
       * @given 件数の問い合わせが 0 を返す、詳細条件 null
       * @when retrieveByPersonVectorWithDetailFilter を接続、テナントID "T001"、類似度 0.8 で実行する
       * @then 件数に 0 を保持し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数が0件の場合_件数0の空の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 0L, row(true));
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

        lot.retrieveByPersonVectorWithDetailFilter(
            connection, TENANT_ID, vector(), 0.8, null, 1, 10);

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
       * @when retrieveByPersonVectorWithDetailFilter を接続、テナントID "T001" で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ベクトルがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_SKILLSHEET_PERSONLot()
            .retrieveByPersonVectorWithDetailFilter(connection, TENANT_ID, null, 0.8, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class getEntityByPk {

    @Nested
    class 正常系 {

      /**
       * 要員IDが一致する要員を一覧から返すことを確かめる。 選出された候補の詳細を取り出す。
       *
       * @given 一覧に要員ID "P001" と "P002" の要員がある
       * @when getEntityByPk を "P002" で実行する
       * @then 要員ID "P002" の要員を返す
       */
      @Test
      void 要員IDが一致する要素がある場合_該当の要員を返す() {
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
        lot.add(personOf("P001"));
        lot.add(personOf("P002"));

        assertThat(lot.getEntityByPk("P002").getPersonId()).isEqualTo("P002");
      }

      /**
       * 要員IDが一致する要素が無い場合に null を返すことを確かめる。 存在しない候補を取り出すと誤った詳細を表示する。
       *
       * @given 一覧に要員ID "P001" の要員がある
       * @when getEntityByPk を "P999" で実行する
       * @then nullを返す
       */
      @Test
      void 要員IDが一致する要素が無い場合_nullを返す() {
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
        lot.add(personOf("P001"));

        assertThat(lot.getEntityByPk("P999")).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に null を返すことを確かめる。 キーの無い検索で先頭の要員を誤って返さない。
       *
       * @given 一覧に要員ID "P001" の要員がある
       * @when getEntityByPk を null で実行する
       * @then nullを返す
       */
      @Test
      void 要員IDがnullの場合_nullを返す() {
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
        lot.add(personOf(PERSON_ID));

        assertThat(lot.getEntityByPk(null)).isNull();
      }
    }
  }

  @Nested
  class to要員選出用文章 {

    @Nested
    class 正常系 {

      /**
       * 一覧の各要員を人目の番号を付けて連結した文章を返すことを確かめる。 LLM に候補を番号で示す。
       *
       * @given 一覧に要員ID "P001" と "P002" の要員が2件ある
       * @when to要員選出用文章 を実行する
       * @then 1人目と2人目の順に番号を付けた文章を返す
       */
      @Test
      void 一覧に2件ある場合_番号を付けて連結した文章を返す() {
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
        lot.add(personOf("P001"));
        lot.add(personOf("P002"));

        assertThat(lot.to要員選出用文章()).isEqualTo("1人目：要員ID：P001 内容：\n2人目：要員ID：P002 内容：\n");
      }
    }
  }

  @Nested
  class toSkillSheetSelectionText {

    @Nested
    class 正常系 {

      /**
       * 一覧の各要員のスキルシートを人目の番号を付けて連結した文章を返すことを確かめる。 LLM にスキルシートを番号で示す。
       *
       * @given 一覧に要員が1件ある
       * @when toSkillSheetSelectionText を実行する
       * @then 1件目の番号を付けた文章を返す
       */
      @Test
      void 一覧に1件ある場合_番号を付けた文章を返す() {
        SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
        lot.add(personOf("P001"));

        assertThat(lot.toSkillSheetSelectionText()).startsWith("1件目：");
      }
    }
  }

  /**
   * 要員IDを持つ結合の結果を生成する。
   *
   * @param personId 要員ID
   * @return 生成した結合の結果
   */
  private static SES_AI_T_SKILLSHEET_PERSON personOf(final String personId) {
    SES_AI_T_SKILLSHEET_PERSON person = new SES_AI_T_SKILLSHEET_PERSON(TENANT_ID);
    person.setPersonId(personId);
    return person;
  }
}
