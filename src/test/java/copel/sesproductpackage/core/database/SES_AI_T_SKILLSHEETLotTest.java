package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** {@link SES_AI_T_SKILLSHEETLot} の単体テスト。 */
class SES_AI_T_SKILLSHEETLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 取得結果の列テナントIDを含む。 */
  private static final String[] COLUMNS = {
    "from_group",
    "from_id",
    "from_name",
    "file_id",
    "file_name",
    "file_content",
    "file_content_summary",
    "register_date",
    "register_user",
    "ttl",
    "tenant_id"
  };

  /** 類似検索の結果の列距離を含む。 */
  private static final String[] COLUMNS_WITH_DISTANCE = {
    "from_group",
    "from_id",
    "from_name",
    "file_id",
    "file_name",
    "file_content",
    "file_content_summary",
    "register_date",
    "register_user",
    "ttl",
    "tenant_id",
    "distance"
  };

  /**
   * スキルシートを1行分与える結果セットを作る。
   *
   * @param fileId ファイルID
   * @param tenantId テナントID
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet oneRow(final String fileId, final String tenantId) throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            "G001",
            "S001",
            "送信者",
            fileId,
            "skill.xlsx",
            "原文",
            "要約",
            "2026-01-15 10:00:00",
            "U001",
            "2026-02-15 10:00:00",
            tenantId)
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
   * ベクトルの値を持つ検索結果を作る。
   *
   * @return 検索結果の Vector
   */
  private static Vector vector() {
    return new Vector(null);
  }

  /**
   * 類似検索の結果として、距離を含むスキルシートを1行分与える結果セットを作る。
   *
   * @param fileId ファイルID
   * @param tenantId テナントID
   * @return 距離を含む1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet vectorRow(final String fileId, final String tenantId)
      throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS_WITH_DISTANCE)
        .row(
            "G001",
            "S001",
            "送信者",
            fileId,
            "skill.xlsx",
            "原文",
            "要約",
            "2026-01-15 10:00:00",
            "U001",
            "2026-02-15 10:00:00",
            tenantId,
            0.1)
        .build();
  }

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントのスキルシートを全件読み込み、項目を変換することを確かめる。 画面の一覧は自テナントのスキルシートだけを表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、ファイルID "F001"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then ファイルID "F001" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("F001", TENANT_ID));
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getFileId()).isEqualTo("F001");
        assertThat(lot.get(0).getTtl().toString()).isEqualTo("2026-02-15 10:00:00");
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントのスキルシートを返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_T_SKILLSHEETLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントのスキルシートを読まないことを確かめる。 他テナントの要員情報が漏えいする。
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

        new SES_AI_T_SKILLSHEETLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class getSelectAllSql {

    @Nested
    class 正常系 {

      /**
       * selectAll から全件の SQL を使うと、テナントの条件を付けて1件の一覧を返すことを確かめる。 全件の取得に同じ SQL を使う。
       *
       * @given SELECT の結果が 1 行テナントID "T001"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then テナントの条件を付けた全件の SQL で1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void selectAllから呼ばれた場合_テナントの条件を付けて1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("F001", TENANT_ID));
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.selectAll(connection, TENANT_ID);

        verify(connection)
            .prepareStatement(
                "SELECT from_group, from_id, from_name, file_id, file_name, file_content,"
                    + " file_content_summary, register_date, register_user, ttl, tenant_id FROM"
                    + " SES_AI_T_SKILLSHEET WHERE tenant_id = ? ORDER BY register_date DESC");
        assertThat(lot.size()).isEqualTo(1);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントのスキルシートを読み込むことを確かめる。 バッチ処理は全テナントのスキルシートを対象にする。
       *
       * @given SELECT の結果が 1 行テナントID "T002"
       * @when selectAllWithoutTenantFilter を接続で実行する
       * @then テナントIDが "T002" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("F002", "T002"));
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

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
        assertThatThrownBy(() -> new SES_AI_T_SKILLSHEETLot().selectAllWithoutTenantFilter(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class toSkillSheetSelectionText {

    @Nested
    class 正常系 {

      /**
       * 一覧の各スキルシートを人目の番号を付けて連結した文章を返すことを確かめる。 LLM に候補を番号で示す。
       *
       * @given 一覧にファイルID "F001" と "F002" のスキルシートが2件ある
       * @when toSkillSheetSelectionText を実行する
       * @then 1人目と2人目の順に番号を付けた文章を返す
       */
      @Test
      void 一覧に2件ある場合_番号を付けて連結した文章を返す() {
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        lot.add(skillSheetOf("F001"));
        lot.add(skillSheetOf("F002"));

        assertThat(lot.toSkillSheetSelectionText())
            .isEqualTo("1人目：ファイルID：F001内容：要約2人目：ファイルID：F002内容：要約");
      }

      /**
       * 一覧が空の場合に空の文章を返すことを確かめる。 候補が無いときに番号だけの文章を作らない。
       *
       * @given 一覧が空
       * @when toSkillSheetSelectionText を実行する
       * @then 空の文字列を返す
       */
      @Test
      void 一覧が空の場合_空の文字列を返す() {
        assertThat(new SES_AI_T_SKILLSHEETLot().toSkillSheetSelectionText()).isEmpty();
      }
    }
  }

  @Nested
  class getEntityByPk {

    @Nested
    class 正常系 {

      /**
       * ファイルIDが一致するスキルシートを一覧から返すことを確かめる。 選出された候補の詳細を取り出す。
       *
       * @given 一覧にファイルID "F001" と "F002" のスキルシートがある
       * @when getEntityByPk を "F002" で実行する
       * @then ファイルID "F002" のスキルシートを返す
       */
      @Test
      void ファイルIDが一致する要素がある場合_該当のスキルシートを返す() {
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        lot.add(skillSheetOf("F001"));
        lot.add(skillSheetOf("F002"));

        assertThat(lot.getEntityByPk("F002").getFileId()).isEqualTo("F002");
      }

      /**
       * ファイルIDが一致する要素が無い場合に null を返すことを確かめる。 存在しない候補を取り出すと誤った詳細を表示する。
       *
       * @given 一覧にファイルID "F001" のスキルシートがある
       * @when getEntityByPk を "F999" で実行する
       * @then nullを返す
       */
      @Test
      void ファイルIDが一致する要素が無い場合_nullを返す() {
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        lot.add(skillSheetOf("F001"));

        assertThat(lot.getEntityByPk("F999")).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイルIDが null の場合に null を返すことを確かめる。 キーの無い検索は先頭の要素を誤って返す。
       *
       * @given 一覧にファイルID "F001" のスキルシートがある
       * @when getEntityByPk を null で実行する
       * @then nullを返す
       */
      @Test
      void ファイルIDがnullの場合_nullを返す() {
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        lot.add(skillSheetOf("F001"));

        assertThat(lot.getEntityByPk(null)).isNull();
      }
    }
  }

  @Nested
  class retrieveWithThreshold {

    @Nested
    class 正常系 {

      /**
       * ベクトルの類似検索で件数と行を読み込み、距離を設定することを確かめる。 類似度の高い候補を一覧に並べる。
       *
       * @given 件数が 1 件、検索の結果が 1 行ファイルID "F001"、ベクトルが設定済み
       * @when retrieveWithThreshold を接続、テナントID "T001"、類似度 0.8、件数 10 で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 件数が1件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 1L, vectorRow("F001", TENANT_ID));
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.retrieveWithThreshold(connection, TENANT_ID, vector(), 0.8, 10);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.getTotalCount()).isEqualTo(1L);
      }

      /**
       * 件数の問い合わせにテナントIDを条件として渡すことを確かめる。 他テナントのスキルシートを類似検索の候補にしない。
       *
       * @given 件数が 0 件、ベクトルが設定済み
       * @when retrieveWithThreshold を接続、テナントID "T001" で実行する
       * @then 件数の条件の3番目に "T001" を設定し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_件数の条件にT001を設定して空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement =
            stubCountAndRows(
                connection, 0L, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.retrieveWithThreshold(connection, TENANT_ID, vector(), 0.8, 10);

        assertThat(lot.isEmpty()).isTrue();
        verify(countStatement).setString(3, TENANT_ID);
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
        new SES_AI_T_SKILLSHEETLot().retrieveWithThreshold(connection, TENANT_ID, null, 0.8, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class selectByFileName {

    @Nested
    class 正常系 {

      /**
       * ファイル名とテナントIDが一致する行を1件の一覧にすることを確かめる。 同じ名前のファイルを探して詳細を表示する。
       *
       * @given SELECT の結果が 1 行、ファイル名が "skill.xlsx"、テナントIDが "T001"
       * @when selectByFileName を接続、テナントID "T001"、ファイル名 "skill.xlsx" で実行する
       * @then 1件の一覧を返し、ファイル名を条件に設定する
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = JdbcMocks.stubQuery(connection, oneRow("F001", TENANT_ID));
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.selectByFileName(connection, TENANT_ID, "skill.xlsx");

        assertThat(lot.size()).isEqualTo(1);
        verify(statement).setString(1, "skill.xlsx");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイル名が null の場合に DB を呼ばず、空の一覧を返すことを確かめる。 キーの無い検索は全件を返す。
       *
       * @given ファイル名が null
       * @when selectByFileName を接続、テナントID "T001" で実行する
       * @then 空の一覧を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ファイル名がnullの場合_空の一覧を返す(final Connection connection) throws SQLException {
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.selectByFileName(connection, TENANT_ID, null);

        assertThat(lot.isEmpty()).isTrue();
        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class selectExpiredSkillsheetsWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * 期限切れのスキルシートを全テナントから読み込むことを確かめる。 期限切れの原文を削除対象として拾う。
       *
       * @given SELECT の結果が 1 行テナントID "T002"、経過日数 30、オフセット 0、件数 100
       * @when selectExpiredSkillsheetsWithoutTenantId を接続で実行する
       * @then テナントIDが "T002" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 期限切れの行が1件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("F001", "T002"));
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.selectExpiredSkillsheetsWithoutTenantId(connection, 30, 0, 100);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getTenantId()).isEqualTo("T002");
      }

      /**
       * 期限切れの判定が ttl を優先し、ttl も登録日も無い行を対象にしないことを確かめる。 ttl が未来の行を登録日の欠落だけで削除すると有効なデータを失う。
       *
       * @given SELECT の結果が 0 行、経過日数 30、オフセット 0、件数 100
       * @when selectExpiredSkillsheetsWithoutTenantId を接続で実行する
       * @then register_date IS NULL を条件にせず、登録日とファイルIDの順に並べる SQL を準備する
       */
      @MockDbConnection
      @Test
      void 期限切れの検索の場合_登録日が無い行を条件にしないSQLで空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns("file_id").build());
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.selectExpiredSkillsheetsWithoutTenantId(connection, 30, 0, 100);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue())
            .doesNotContain("OR (register_date IS NULL)")
            .contains("ORDER BY register_date ASC, file_id ASC");
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class selectBundledWithPersonWithoutTenantFilter {

    @Nested
    class 正常系 {

      /**
       * 要員の登録日時を使って、同梱されたスキルシートを読み込むことを確かめる。 要員と同時に登録されたスキルシートを探す。
       *
       * @given 要員の送信者グループが "G001"、送信者IDが "S001"、登録日時が "2026-01-15 10:00:00"、SELECT の結果が 1 行
       * @when selectBundledWithPersonWithoutTenantFilter を接続と要員で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 登録日時がある要員の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("F001", "T002"));
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.selectBundledWithPersonWithoutTenantFilter(connection, personOf(true));

        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 要員のテナントIDで同梱のスキルシートを絞り込むことを確かめる。 別テナントのスキルシートを巻き込んで削除するとデータ漏洩になる。
       *
       * @given 要員のテナントIDが "T001"、送信者グループが "G001"、送信者IDが "S001"、SELECT の結果が 1 行
       * @when selectBundledWithPersonWithoutTenantFilter を接続と要員で実行する
       * @then tenant_id の条件を含む SQL を準備し、4番目のバインド値に "T001" を渡す
       */
      @MockDbConnection
      @Test
      void 要員のテナントIDがT001の場合_T001で絞り込んだスキルシートを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = JdbcMocks.stubQuery(connection, oneRow("F001", TENANT_ID));

        new SES_AI_T_SKILLSHEETLot()
            .selectBundledWithPersonWithoutTenantFilter(connection, personOf(true));

        verify(connection).prepareStatement(contains("AND tenant_id = ?"));
        verify(statement).setString(4, TENANT_ID);
      }

      /**
       * 要員の登録日時が未設定の場合に、現在時刻を使って検索することを確かめる。 登録日時の無い要員でも同梱の検索を行えるようにする。
       *
       * @given 要員の登録日時が未設定、SELECT の結果が 1 行
       * @when selectBundledWithPersonWithoutTenantFilter を接続と要員で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 登録日時が未設定の要員の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, oneRow("F001", "T002"));
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();

        lot.selectBundledWithPersonWithoutTenantFilter(connection, personOf(false));

        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員が null の場合に DB を呼ばないことを確かめる。 要員の無い検索は同梱の対象を決められない。
       *
       * @given 要員が null
       * @when selectBundledWithPersonWithoutTenantFilter を接続と null の要員で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 要員がnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_SKILLSHEETLot().selectBundledWithPersonWithoutTenantFilter(connection, null);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  /**
   * ファイルIDを持つスキルシートを生成する。
   *
   * @param fileId ファイルID
   * @return 生成したスキルシート
   */
  private static SES_AI_T_SKILLSHEET skillSheetOf(final String fileId) {
    SES_AI_T_SKILLSHEET skillSheet = new SES_AI_T_SKILLSHEET(TENANT_ID);
    skillSheet.setFileId(fileId);
    skillSheet.setFileContentSummary("要約");
    return skillSheet;
  }

  /**
   * 送信者と登録日時を持つ要員を生成する。
   *
   * @param withRegisterDate 登録日時を設定する場合は true
   * @return 生成した要員
   */
  private static SES_AI_T_PERSON personOf(final boolean withRegisterDate) {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);
    person.setFromGroup("G001");
    person.setFromId("S001");
    if (withRegisterDate) {
      person.setRegisterDate(new OriginalDateTime("2026-01-15 10:00:00"));
    }
    return person;
  }
}
