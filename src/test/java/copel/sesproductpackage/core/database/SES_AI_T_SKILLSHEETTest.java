package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_T_SKILLSHEET} の単体テスト。 */
@TestProperty(key = "S3_BUCKET_NAME", value = "nectar-bucket")
class SES_AI_T_SKILLSHEETTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** スキルシートのファイルID。 */
  private static final String FILE_ID = "20260115_skill.xlsx";

  /** 登録日時と有効期限の文字列。 */
  private static final String DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列原文を含む。 */
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
    "ttl"
  };

  /** 取得結果の列原文を含まない。 */
  private static final String[] COLUMNS_WITHOUT_CONTENT = {
    "from_group",
    "from_id",
    "from_name",
    "file_id",
    "file_name",
    "file_content_summary",
    "register_date",
    "register_user",
    "ttl"
  };

  /**
   * 準備文のモックを作り、接続の prepareStatement がその準備文を返すように設定する。
   *
   * @param connection 接続のモック
   * @param updated executeUpdate が返す更新件数
   * @return 準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static PreparedStatement stubUpdate(final Connection connection, final int updated)
      throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.executeUpdate()).thenReturn(updated);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    return statement;
  }

  /**
   * 項目が揃ったスキルシートを生成する。
   *
   * @return 生成したスキルシート
   */
  private static SES_AI_T_SKILLSHEET skillSheet() {
    SES_AI_T_SKILLSHEET skillSheet = new SES_AI_T_SKILLSHEET(TENANT_ID);
    skillSheet.setFromGroup("G001");
    skillSheet.setFromId("S001");
    skillSheet.setFromName("送信者");
    skillSheet.setFileId(FILE_ID);
    skillSheet.setFileName("skill.xlsx");
    skillSheet.setFileContent("原文");
    skillSheet.setFileContentSummary("要約");
    skillSheet.setRegisterDate(new OriginalDateTime(DATE_TEXT));
    skillSheet.setRegisterUser("U001");
    skillSheet.setTtl(new OriginalDateTime(DATE_TEXT));
    return skillSheet;
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 スキルシートをテナントの条件で管理する。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_T_SKILLSHEET を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_T_SKILLSHEET(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無いスキルシートは他テナントの要員と結び付く。
       *
       * @given テナントIDが null
       * @when SES_AI_T_SKILLSHEET を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_SKILLSHEET(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class toSkillSheetSelectionText {

    @Nested
    class 正常系 {

      /**
       * ファイルIDと要約を組み合わせた選出用の文章を返すことを確かめる。 LLM にスキルシートを選ばせる入力に使う。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、要約が "要約"
       * @when toSkillSheetSelectionText を実行する
       * @then ファイルIDと要約を並べた文章を返す
       */
      @Test
      void ファイルIDと要約がある場合_選出用の文章を返す() {
        assertThat(skillSheet().toSkillSheetSelectionText())
            .isEqualTo("ファイルID：20260115_skill.xlsx内容：要約");
      }
    }
  }

  @Nested
  class getFileUrl {

    @Nested
    class 正常系 {

      /**
       * ファイルIDとファイル名からダウンロード URL を組み立てることを確かめる。 画面からスキルシートを取得するためのリンクに使う。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、ファイル名が "skill.xlsx"、バケット名が "nectar-bucket"
       * @when getFileUrl を実行する
       * @then S3 の URL を返す
       */
      @Test
      void ファイルIDとファイル名が揃っている場合_S3のURLを返す() {
        assertThat(skillSheet().getFileUrl())
            .isEqualTo("https://nectar-bucket.s3.ap-northeast-1.amazonaws.com/20260115_skill.xlsx");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイルIDとファイル名が未設定の場合に URL を作らないことを確かめる。 保存先の場所が決まらない。
       *
       * @given ファイルIDが null、ファイル名が null
       * @when getFileUrl を実行する
       * @then nullを返す
       */
      @Test
      void ファイルIDとファイル名が未設定の場合_nullを返す() {
        SES_AI_T_SKILLSHEET skillSheet = new SES_AI_T_SKILLSHEET(TENANT_ID);

        assertThat(skillSheet.getFileUrl()).isNull();
      }
    }
  }

  @Nested
  class getObjectKey {

    @Nested
    class 正常系 {

      /**
       * ファイルIDが既にファイル名を含む場合に、ファイルIDをオブジェクトキーとして返すことを確かめる。 保存先のキーを二重に付けない。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、ファイル名が "skill.xlsx"
       * @when getObjectKey を実行する
       * @then ファイルIDの "20260115_skill.xlsx" を返す
       */
      @Test
      void ファイルIDがファイル名を含む場合_ファイルIDを返す() {
        assertThat(skillSheet().getObjectKey()).isEqualTo(FILE_ID);
      }
    }
  }

  @Nested
  class selectByPkWithoutRawContent {

    @Nested
    class 正常系 {

      /**
       * ファイルIDが一致する行の原文を除く項目を読み込むことを確かめる。 一覧で原文を読み込まないことで、通信量を抑える。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、SELECT の結果が 1 行ファイル名 "skill.xlsx"、要約 "要約"
       * @when selectByPkWithoutRawContent を接続で実行する
       * @then ファイル名を "skill.xlsx"、要約を "要約" に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_項目を更新する(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS_WITHOUT_CONTENT)
                .row(
                    "G001",
                    "S001",
                    "送信者",
                    FILE_ID,
                    "skill.xlsx",
                    "要約",
                    DATE_TEXT,
                    "U001",
                    DATE_TEXT)
                .build());
        SES_AI_T_SKILLSHEET skillSheet = new SES_AI_T_SKILLSHEET(TENANT_ID);
        skillSheet.setFileId(FILE_ID);

        skillSheet.selectByPkWithoutRawContent(connection);

        assertThat(skillSheet.getFileName()).isEqualTo("skill.xlsx");
        assertThat(skillSheet.getFileContentSummary()).isEqualTo("要約");
      }

      /**
       * 該当行が無い場合に項目を変えないことを確かめる。 存在しないスキルシートを読み込んでも、入力中の値を壊さない。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、SELECT の結果が 0 行
       * @when selectByPkWithoutRawContent を接続で実行する
       * @then ファイル名を変えずに何もしない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_何もしない(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection, MockResultSetBuilder.builder().columns(COLUMNS_WITHOUT_CONTENT).build());
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();

        skillSheet.selectByPkWithoutRawContent(connection);

        assertThat(skillSheet.getFileName()).isEqualTo("skill.xlsx");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイルIDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは別のスキルシートを返すおそれがある。
       *
       * @given ファイルIDが null
       * @when selectByPkWithoutRawContent を接続で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ファイルIDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_SKILLSHEET(TENANT_ID).selectByPkWithoutRawContent(connection);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class getRawContent {

    @Nested
    class 正常系 {

      /**
       * 重複チェックの判定に使う原文を返すことを確かめる。 原文で同じスキルシートがないかを比べる。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、原文が "原文"、重複チェックの SQL の結果が 0 件
       * @when uniqueCheck を接続と類似度 0.8 で実行する
       * @then 原文 "原文" を検索条件に設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 重複チェックから呼ばれた場合_原文を検索条件にしてtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        java.sql.ResultSet rs = MockResultSetBuilder.builder().columns("count").row(0).build();
        when(statement.executeQuery()).thenReturn(rs);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        assertThat(skillSheet().uniqueCheck(connection, 0.8)).isTrue();

        verify(statement).setString(2, "原文");
        verify(statement).setString(3, "原文");
      }
    }
  }

  @Nested
  class getContentSummary {

    @Nested
    class 異常系 {

      /**
       * 要約が空のスキルシートを埋め込むと例外を投げることを確かめる。 要約のない内容は埋め込みに使えない。
       *
       * @given 要約が空文字
       * @when embedding を変換器で実行する
       * @then RuntimeExceptionを投げる
       */
      @Test
      void embeddingから呼ばれた場合_RuntimeExceptionを投げる() {
        SES_AI_T_SKILLSHEET skillSheet = new SES_AI_T_SKILLSHEET(TENANT_ID);
        skillSheet.setFileContentSummary("");
        Transformer transformer = mock(Transformer.class);

        assertThatThrownBy(() -> skillSheet.embedding(transformer))
            .isInstanceOf(RuntimeException.class);
      }
    }
  }

  @Nested
  class getCheckSql {

    @Nested
    class 正常系 {

      /**
       * 重複チェックで同じ原文が見つからない場合に、一意と判定することを確かめる。 同じスキルシートを二重に登録しないための判定である。
       *
       * @given 重複チェックの SQL の結果が 0 件
       * @when uniqueCheck を接続と類似度 0.8 で実行する
       * @then trueを返す
       */
      @Test
      void 重複が無い場合_trueを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        java.sql.ResultSet rs = MockResultSetBuilder.builder().columns("count").row(0).build();
        when(statement.executeQuery()).thenReturn(rs);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        assertThat(skillSheet().uniqueCheck(connection, 0.8)).isTrue();
      }

      /**
       * 重複チェックで同じ原文が見つかった場合に、一意ではないと判定することを確かめる。 同じスキルシートの二重登録を防ぐ。
       *
       * @given 重複チェックの SQL の結果が 1 件
       * @when uniqueCheck を接続と類似度 0.8 で実行する
       * @then falseを返す
       */
      @Test
      void 重複がある場合_falseを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        java.sql.ResultSet rs = MockResultSetBuilder.builder().columns("count").row(1).build();
        when(statement.executeQuery()).thenReturn(rs);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        assertThat(skillSheet().uniqueCheck(connection, 0.8)).isFalse();
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * スキルシートの項目を列の順に設定し、テナントIDを加えて登録することを確かめる。 テナントの無いスキルシートは他テナントの要員と区別できない。
       *
       * @given 項目が揃ったスキルシート、ベクトルが未設定、更新件数が 1 件
       * @when insert を接続で実行する
       * @then ベクトルの位置に null を設定し、テナントIDを含めて1件を返す
       */
      @MockDbConnection
      @Test
      void ベクトルが未設定の場合_nullを設定してテナントIDを含め1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();

        assertThat(skillSheet.insert(connection)).isEqualTo(1);

        verify(connection)
            .prepareStatement(
                "INSERT INTO SES_AI_T_SKILLSHEET (from_group, from_id, from_name, file_id, file_name,"
                    + " file_content, file_content_summary, vector_data, register_date,"
                    + " register_user, ttl, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?,"
                    + " ?, ?)");
        verify(statement).setString(4, FILE_ID);
        verify(statement).setString(8, null);
        verify(statement).setTimestamp(9, Timestamp.valueOf(DATE_TEXT));
        verify(statement).setTimestamp(11, Timestamp.valueOf(DATE_TEXT));
        verify(statement).setString(12, TENANT_ID);
      }

      /**
       * ベクトルと登録日時と有効期限が揃っている場合に、ベクトルの文字列を設定することを確かめる。 埋め込み済みのスキルシートを検索に使えるように保存する。
       *
       * @given 要約を埋め込み済みのスキルシート、登録日時と有効期限が設定済み、更新件数が 1 件
       * @when insert を接続で実行する
       * @then ベクトルの位置にベクトルの文字列を設定して1件を返す
       */
      @MockDbConnection
      @Test
      void ベクトルが設定されている場合_ベクトルの文字列を設定して1件を返す(final Connection connection) throws Exception {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("要約")).thenReturn(new float[] {0.1f, 0.2f});
        skillSheet.embedding(transformer);

        assertThat(skillSheet.insert(connection)).isEqualTo(1);

        verify(statement).setString(8, skillSheet.getVectorData().toString());
      }
    }

    @Nested
    class 境界値 {

      /**
       * 登録日時と有効期限が未設定の場合に、各項目の位置に null を設定することを確かめる。 未設定の日時があっても登録を失敗させない。
       *
       * @given 登録日時が null、有効期限が null、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 登録日時と有効期限の位置に null を設定して1件を返す
       */
      @MockDbConnection
      @Test
      void 登録日時と有効期限が未設定の場合_nullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();
        skillSheet.setRegisterDate(null);
        skillSheet.setTtl(null);

        assertThat(skillSheet.insert(connection)).isEqualTo(1);

        verify(statement).setTimestamp(9, null);
        verify(statement).setTimestamp(11, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * ファイルIDとテナントIDが一致する行の項目を読み込むことを確かめる。 保存済みのスキルシートを画面に表示する。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、SELECT の結果が 1 行原文 "原文"
       * @when selectByPk を接続で実行する
       * @then trueを返し、原文を "原文" に更新し、ファイル名を "skill.xlsx" に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(
                    "G001",
                    "S001",
                    "送信者",
                    FILE_ID,
                    "skill.xlsx",
                    "原文",
                    "要約",
                    DATE_TEXT,
                    "U001",
                    DATE_TEXT)
                .build());
        SES_AI_T_SKILLSHEET skillSheet = new SES_AI_T_SKILLSHEET(TENANT_ID);
        skillSheet.setFileId(FILE_ID);

        assertThat(skillSheet.selectByPk(connection)).isTrue();

        assertThat(skillSheet.getFileContent()).isEqualTo("原文");
        assertThat(skillSheet.getFileName()).isEqualTo("skill.xlsx");
      }

      /**
       * 該当行が無い場合に false を返し、項目を変えないことを確かめる。 存在しないスキルシートを読み込んでも、入力中の値を壊さない。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then falseを返し、原文を変えない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();

        assertThat(skillSheet.selectByPk(connection)).isFalse();

        assertThat(skillSheet.getFileContent()).isEqualTo("原文");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイルIDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは別のスキルシートを返すおそれがある。
       *
       * @given ファイルIDが null
       * @when selectByPk を接続で実行する
       * @then falseを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ファイルIDがnullの場合_DBを呼ばずfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_SKILLSHEET(TENANT_ID).selectByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * 更新はサポートせず、DB を呼ばずに false を返すことを確かめる。 スキルシートは登録と削除だけで扱う。
       *
       * @given 項目が揃ったスキルシート
       * @when updateByPk を接続で実行する
       * @then DB を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 項目が揃っている場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(skillSheet().updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * ファイルIDとテナントIDが一致する行を削除することを確かめる。 不要になったスキルシートを残さない。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then tenant_id を条件に含めて削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(skillSheet().deleteByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "DELETE FROM SES_AI_T_SKILLSHEET WHERE file_id = ? AND tenant_id = ?");
        verify(statement).setString(1, FILE_ID);
        verify(statement).setString(2, TENANT_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象のスキルシートが無いときに削除成功と誤認しない。
       *
       * @given ファイルIDが "20260115_skill.xlsx"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(skillSheet().deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイルIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全件を消すおそれがある。
       *
       * @given ファイルIDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ファイルIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_SKILLSHEET(TENANT_ID).deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class getSkillSheet {

    @Nested
    class 正常系 {

      /**
       * 保持しているスキルシートの値を返すことを確かめる。 スキルシートの詳細を画面や連携に渡す。
       *
       * @given ファイルIDが "20260115_skill.xlsx" のスキルシート
       * @when getSkillSheet を実行する
       * @then ファイルIDが "20260115_skill.xlsx" のスキルシートを返す
       */
      @Test
      void スキルシートを保持している場合_保持しているスキルシートを返す() {
        assertThat(skillSheet().getSkillSheet().getFileId()).isEqualTo(FILE_ID);
      }
    }
  }

  @Nested
  class getFileId {

    @Nested
    class 正常系 {

      /**
       * 保持しているファイルIDを返すことを確かめる。 ファイルの一意な識別子として使う。
       *
       * @given ファイルIDが "20260115_skill.xlsx"
       * @when getFileId を実行する
       * @then "20260115_skill.xlsx" を返す
       */
      @Test
      void ファイルIDを保持している場合_ファイルIDを返す() {
        assertThat(skillSheet().getFileId()).isEqualTo(FILE_ID);
      }
    }
  }

  @Nested
  class setFileId {

    @Nested
    class 正常系 {

      /**
       * ファイルIDを設定すると、取得した値に反映されることを確かめる。 ファイルIDを画面の入力値から設定する。
       *
       * @given 項目が揃ったスキルシート
       * @when setFileId を "F002" で実行する
       * @then getFileId が "F002" を返す
       */
      @Test
      void ファイルIDを指定した場合_指定したファイルIDを返す() {
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();

        skillSheet.setFileId("F002");

        assertThat(skillSheet.getFileId()).isEqualTo("F002");
      }
    }
  }

  @Nested
  class getFileName {

    @Nested
    class 正常系 {

      /**
       * 保持しているファイル名を返すことを確かめる。 画面に元のファイル名を表示する。
       *
       * @given ファイル名が "skill.xlsx"
       * @when getFileName を実行する
       * @then "skill.xlsx" を返す
       */
      @Test
      void ファイル名を保持している場合_ファイル名を返す() {
        assertThat(skillSheet().getFileName()).isEqualTo("skill.xlsx");
      }
    }
  }

  @Nested
  class setFileName {

    @Nested
    class 正常系 {

      /**
       * ファイル名を設定すると、取得した値に反映されることを確かめる。 元のファイル名を画面の入力値から設定する。
       *
       * @given 項目が揃ったスキルシート
       * @when setFileName を "other.xlsx" で実行する
       * @then getFileName が "other.xlsx" を返す
       */
      @Test
      void ファイル名を指定した場合_指定したファイル名を返す() {
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();

        skillSheet.setFileName("other.xlsx");

        assertThat(skillSheet.getFileName()).isEqualTo("other.xlsx");
      }
    }
  }

  @Nested
  class getFileContent {

    @Nested
    class 正常系 {

      /**
       * 保持している原文を返すことを確かめる。 スキルシートの原文を解析や表示に使う。
       *
       * @given 原文が "原文"
       * @when getFileContent を実行する
       * @then "原文" を返す
       */
      @Test
      void 原文を保持している場合_原文を返す() {
        assertThat(skillSheet().getFileContent()).isEqualTo("原文");
      }
    }
  }

  @Nested
  class setFileContent {

    @Nested
    class 正常系 {

      /**
       * 原文を設定すると、取得した値に反映されることを確かめる。 解析後の原文を保持する。
       *
       * @given 項目が揃ったスキルシート
       * @when setFileContent を "新しい原文" で実行する
       * @then getFileContent が "新しい原文" を返す
       */
      @Test
      void 原文を指定した場合_指定した原文を返す() {
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();

        skillSheet.setFileContent("新しい原文");

        assertThat(skillSheet.getFileContent()).isEqualTo("新しい原文");
      }
    }
  }

  @Nested
  class getFileContentSummary {

    @Nested
    class 正常系 {

      /**
       * 保持している要約を返すことを確かめる。 一覧表示や選出用の文章に使う。
       *
       * @given 要約が "要約"
       * @when getFileContentSummary を実行する
       * @then "要約" を返す
       */
      @Test
      void 要約を保持している場合_要約を返す() {
        assertThat(skillSheet().getFileContentSummary()).isEqualTo("要約");
      }
    }
  }

  @Nested
  class setFileContentSummary {

    @Nested
    class 正常系 {

      /**
       * 要約を設定すると、取得した値に反映されることを確かめる。 解析で作った要約を保持する。
       *
       * @given 項目が揃ったスキルシート
       * @when setFileContentSummary を "新しい要約" で実行する
       * @then getFileContentSummary が "新しい要約" を返す
       */
      @Test
      void 要約を指定した場合_指定した要約を返す() {
        SES_AI_T_SKILLSHEET skillSheet = skillSheet();

        skillSheet.setFileContentSummary("新しい要約");

        assertThat(skillSheet.getFileContentSummary()).isEqualTo("新しい要約");
      }
    }
  }
}
