package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_T_SKILLSHEET_PERSON} の単体テスト。 */
class SES_AI_T_SKILLSHEET_PERSONTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 要員ID。 */
  private static final String PERSON_ID = "P001";

  /** 結合の結果の列SELECT 文の列の順。 */
  private static final String[] COLUMNS = {
    "file_id",
    "file_name",
    "file_content_summary",
    "person_id",
    "mail_subject",
    "raw_content",
    "content_summary",
    "unit_price",
    "register_date",
    "register_user",
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
    "other_requirements",
    "ng_requirements",
    "url"
  };

  /**
   * 項目が全て揃った結合の結果を1行分与える結果セットを作る。
   *
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet fullRow() throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            "F001",
            "skill.xlsx",
            "ファイル要約",
            PERSON_ID,
            "件名",
            "原文",
            "要約",
            new BigDecimal(600_000),
            Timestamp.valueOf("2026-01-15 10:00:00"),
            "U001",
            "G001",
            "S001",
            "送信者",
            "山田太郎",
            35,
            "Man",
            "日本",
            "2026-02-01 00:00:00",
            "東京",
            "関東_首都圏",
            3,
            "組織",
            "経験1\n経験2",
            "その他1\nその他2",
            "NG1\nNG2",
            "https://example.com")
        .build();
  }

  /**
   * 任意項目が未設定の結合の結果を1行分与える結果セットを作る。
   *
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet nullableRow() throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            "F001",
            "skill.xlsx",
            "ファイル要約",
            PERSON_ID,
            "件名",
            "原文",
            "要約",
            null,
            null,
            "U001",
            "G001",
            "S001",
            "送信者",
            "山田太郎",
            null,
            null,
            "日本",
            null,
            "東京",
            null,
            null,
            "組織",
            null,
            null,
            null,
            "https://example.com")
        .build();
  }

  /**
   * 要員の詳細を持つスキルシートを生成する。
   *
   * @return 生成したスキルシート
   */
  private static SES_AI_T_SKILLSHEET_PERSON entity() {
    return new SES_AI_T_SKILLSHEET_PERSON(TENANT_ID);
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 要員とスキルシートの結合結果をテナントの条件で扱う。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_T_SKILLSHEET_PERSON を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(entity().getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無い結合結果は他テナントの要員を含む。
       *
       * @given テナントIDが null
       * @when SES_AI_T_SKILLSHEET_PERSON を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_SKILLSHEET_PERSON(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class to要員選出用文章 {

    @Nested
    class 正常系 {

      /**
       * 要員の要約とスキルシートの要約を連結した選出用の文章を返すことを確かめる。 LLM に要員を選ばせる入力に使う。
       *
       * @given 要員ID "P001"、要約 "要約"、スキルシートの要約 "ファイル要約"
       * @when to要員選出用文章 を実行する
       * @then 要員IDと要約を並べた文章を返す
       */
      @Test
      void 両方の要約がある場合_連結した文章を返す() {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        entity.setPersonId(PERSON_ID);
        entity.setContentSummary("要約");
        entity.setFileContentSummary("ファイル要約");

        assertThat(entity.to要員選出用文章()).isEqualTo("要員ID：P001 内容：要約ファイル要約");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要約が未設定の場合に空として連結することを確かめる。 要約が無くても選出用の文章を作れるようにする。
       *
       * @given 要員ID "P001"、要約が null、スキルシートの要約 "ファイル要約"
       * @when to要員選出用文章 を実行する
       * @then 要約を空にして連結した文章を返す
       */
      @Test
      void 要約が未設定の場合_要約を空にして連結した文章を返す() {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        entity.setPersonId(PERSON_ID);
        entity.setFileContentSummary("ファイル要約");

        assertThat(entity.to要員選出用文章()).isEqualTo("要員ID：P001 内容：ファイル要約");
      }

      /**
       * スキルシートの要約が未設定の場合に空として連結することを確かめる。 スキルシートが無い要員でも選出用の文章を作れるようにする。
       *
       * @given 要員ID "P001"、要約 "要約"、スキルシートの要約が null
       * @when to要員選出用文章 を実行する
       * @then スキルシートの要約を空にして連結した文章を返す
       */
      @Test
      void スキルシートの要約が未設定の場合_要約だけの文章を返す() {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        entity.setPersonId(PERSON_ID);
        entity.setContentSummary("要約");

        assertThat(entity.to要員選出用文章()).isEqualTo("要員ID：P001 内容：要約");
      }
    }
  }

  @Nested
  class toSkillSheetSelectionText {

    @Nested
    class 正常系 {

      /**
       * スキルシートのファイルIDと要約を連結した選出用の文章を返すことを確かめる。 LLM にスキルシートを選ばせる入力に使う。
       *
       * @given ファイルID "F001"、要約 "要約"、スキルシートの要約 "ファイル要約"
       * @when toSkillSheetSelectionText を実行する
       * @then ファイルIDとスキルシートの要約と要約を並べた文章を返す
       */
      @Test
      void 両方の要約がある場合_連結した文章を返す() {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        entity.setFileId("F001");
        entity.setContentSummary("要約");
        entity.setFileContentSummary("ファイル要約");

        assertThat(entity.toSkillSheetSelectionText()).isEqualTo("ファイルID：F001 内容：ファイル要約要約");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要約が未設定の場合に空として連結することを確かめる。 要約が無くても選出用の文章を作れるようにする。
       *
       * @given ファイルID "F001"、要約が null、スキルシートの要約 "ファイル要約"
       * @when toSkillSheetSelectionText を実行する
       * @then 要約を空にして連結した文章を返す
       */
      @Test
      void 要約が未設定の場合_要約を空にして連結した文章を返す() {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        entity.setFileId("F001");
        entity.setFileContentSummary("ファイル要約");

        assertThat(entity.toSkillSheetSelectionText()).isEqualTo("ファイルID：F001 内容：ファイル要約");
      }

      /**
       * スキルシートの要約が未設定の場合に空として連結することを確かめる。 スキルシートが無い要員でも選出用の文章を作れるようにする。
       *
       * @given ファイルID "F001"、要約 "要約"、スキルシートの要約が null
       * @when toSkillSheetSelectionText を実行する
       * @then スキルシートの要約を空にして連結した文章を返す
       */
      @Test
      void スキルシートの要約が未設定の場合_要約だけの文章を返す() {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        entity.setFileId("F001");
        entity.setContentSummary("要約");

        assertThat(entity.toSkillSheetSelectionText()).isEqualTo("ファイルID：F001 内容：要約");
      }
    }
  }

  @Nested
  class selectOuterJoinByPersonId {

    @Nested
    class 正常系 {

      /**
       * 要員IDが一致する結合の結果を読み込み、項目を設定することを確かめる。 要員と紐づくスキルシートを一覧に表示する。
       *
       * @given 要員ID "P001"、SELECT の結果が 1 行全項目が揃っている
       * @when selectOuterJoinByPersonId を接続と要員ID "P001" で実行する
       * @then 要員の年齢を 35、性別を Man、エリアを 関東_首都圏 に設定する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_年齢と性別とエリアを更新する(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, fullRow());
        SES_AI_T_SKILLSHEET_PERSON entity = entity();

        entity.selectOuterJoinByPersonId(connection, PERSON_ID);

        assertThat(entity.getAge()).isEqualTo(35);
        assertThat(entity.getGender()).isEqualTo(Gender.Man);
        assertThat(entity.getArea()).isEqualTo(Area.関東_首都圏);
      }

      /**
       * 該当行が無い場合に項目を変えないことを確かめる。 存在しない要員の結合結果で入力中の値を壊さない。
       *
       * @given 要員ID "P001"、SELECT の結果が 0 行
       * @when selectOuterJoinByPersonId を接続と要員ID "P001" で実行する
       * @then 年齢を変えずに何もしない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_何もしない(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_SKILLSHEET_PERSON entity = entity();

        entity.selectOuterJoinByPersonId(connection, PERSON_ID);

        assertThat(entity.getAge()).isNull();
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントの要員を読まないことを確かめる。 他テナントの要員の情報が漏えいする。
       *
       * @given 要員ID "P001"、テナントID "T001"、SELECT の結果が 0 行
       * @when selectOuterJoinByPersonId を接続と要員ID "P001" で実行する
       * @then 条件の2番目に "T001" を設定し、何もしない
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_何もしない(final Connection connection) throws SQLException {
        java.sql.PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection, MockResultSetBuilder.builder().columns(COLUMNS).build());

        entity().selectOuterJoinByPersonId(connection, PERSON_ID);

        verify(statement).setString(2, TENANT_ID);
      }
    }
  }

  @Nested
  class setEntityDataFromResultSet {

    @Nested
    class 正常系 {

      /**
       * 結果セットの全項目を設定し、値を変換することを確かめる。 画面に要員とスキルシートの詳細を表示する。
       *
       * @given 全項目が揃った1行の結果セット
       * @when setEntityDataFromResultSet を結果セットで実行する
       * @then 単価を 600000、性別を Man、開始日を 2026年02月、出社許容を 3 に設定する
       */
      @Test
      void 全項目が揃っている場合_変換した値を更新する() throws Exception {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        ResultSet rs = fullRow();
        rs.next();

        entity.setEntityDataFromResultSet(rs);

        assertThat(entity.getUnitPrice().getValue()).isEqualByComparingTo(new BigDecimal(600_000));
        assertThat(entity.getGender()).isEqualTo(Gender.Man);
        assertThat(entity.getStartDate().toString()).isEqualTo("2026-02-01 00:00:00");
        assertThat(entity.getOfficeAvailability()).isEqualTo(3);
        assertThat(entity.getRegisterDate().toString()).isEqualTo("2026-01-15 10:00:00");
      }

      /**
       * 任意の項目が null の結果セットを読み込むと、未設定の項目を null や空として設定することを確かめる。 結合先が無い行でも詳細を読み込めるようにする。
       *
       * @given 日付、年齢、性別、エリア、出社許容、単価が null の1行の結果セット
       * @when setEntityDataFromResultSet を結果セットで実行する
       * @then 単価を空、年齢と性別と開始日とエリアと出社許容を null に設定する
       */
      @Test
      void 任意の項目が未設定の場合_未設定の値を更新する() throws Exception {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        ResultSet rs = nullableRow();
        rs.next();

        entity.setEntityDataFromResultSet(rs);

        assertThat(entity.getUnitPrice().isEmpty()).isTrue();
        assertThat(entity.getAge()).isNull();
        assertThat(entity.getGender()).isNull();
        assertThat(entity.getStartDate()).isNull();
        assertThat(entity.getArea()).isNull();
        assertThat(entity.getOfficeAvailability()).isNull();
        assertThat(entity.getRegisterDate()).isNull();
      }
    }
  }

  @Nested
  class getExperiencesAsList {

    @Nested
    class 正常系 {

      /**
       * 経験を改行で分けた一覧を返すことを確かめる。 経験を1件ずつ表示する。
       *
       * @given 経験が "経験1" と "経験2" を改行で連結した文字列
       * @when getExperiencesAsList を実行する
       * @then "経験1" と "経験2" の2件の一覧を返す
       */
      @Test
      void 経験がある場合_改行で分けた2件の一覧を返す() throws Exception {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        ResultSet rs = fullRow();
        rs.next();
        entity.setEntityDataFromResultSet(rs);

        assertThat(entity.getExperiencesAsList()).containsExactly("経験1", "経験2");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 経験が未設定の場合に空の一覧を返すことを確かめる。 経験が無い要員を表示で落とさない。
       *
       * @given 経験が null
       * @when getExperiencesAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void 経験が未設定の場合_空の一覧を返す() {
        assertThat(entity().getExperiencesAsList()).isEmpty();
      }
    }
  }

  @Nested
  class getOtherRequirementsAsList {

    @Nested
    class 正常系 {

      /**
       * その他条件を改行で分けた一覧を返すことを確かめる。 条件を1件ずつ表示する。
       *
       * @given その他条件が "その他1" と "その他2" を改行で連結した文字列
       * @when getOtherRequirementsAsList を実行する
       * @then "その他1" と "その他2" の2件の一覧を返す
       */
      @Test
      void その他条件がある場合_改行で分けた2件の一覧を返す() throws Exception {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        ResultSet rs = fullRow();
        rs.next();
        entity.setEntityDataFromResultSet(rs);

        assertThat(entity.getOtherRequirementsAsList()).containsExactly("その他1", "その他2");
      }
    }

    @Nested
    class 境界値 {

      /**
       * その他条件が未設定の場合に空の一覧を返すことを確かめる。 条件が無い要員を表示で落とさない。
       *
       * @given その他条件が null
       * @when getOtherRequirementsAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void その他条件が未設定の場合_空の一覧を返す() {
        assertThat(entity().getOtherRequirementsAsList()).isEmpty();
      }
    }
  }

  @Nested
  class getNgRequirementsAsList {

    @Nested
    class 正常系 {

      /**
       * NG条件を改行で分けた一覧を返すことを確かめる。 NG条件を1件ずつ確認する。
       *
       * @given NG条件が "NG1" と "NG2" を改行で連結した文字列
       * @when getNgRequirementsAsList を実行する
       * @then "NG1" と "NG2" の2件の一覧を返す
       */
      @Test
      void NG条件がある場合_改行で分けた2件の一覧を返す() throws Exception {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        ResultSet rs = fullRow();
        rs.next();
        entity.setEntityDataFromResultSet(rs);

        assertThat(entity.getNgRequirementsAsList()).containsExactly("NG1", "NG2");
      }
    }

    @Nested
    class 境界値 {

      /**
       * NG条件が未設定の場合に空の一覧を返すことを確かめる。 NG条件が無い要員を表示で落とさない。
       *
       * @given NG条件が null
       * @when getNgRequirementsAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void NG条件が未設定の場合_空の一覧を返す() {
        assertThat(entity().getNgRequirementsAsList()).isEmpty();
      }
    }
  }

  @Nested
  class getRawContent {

    @Nested
    class 正常系 {

      /**
       * 保持している原文を返すことを確かめる。 重複チェックや埋め込みの入力に原文を使う。
       *
       * @given 原文が "原文"
       * @when getRawContent を実行する
       * @then "原文" を返す
       */
      @Test
      void 原文を保持している場合_原文を返す() {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        entity.setRawContent("原文");

        assertThat(entity.getRawContent()).isEqualTo("原文");
      }
    }
  }

  @Nested
  class getContentSummary {

    @Nested
    class 正常系 {

      /**
       * 保持している要約を返すことを確かめる。 埋め込みの入力に要約を使う。
       *
       * @given 要約が "要約"
       * @when getContentSummary を実行する
       * @then "要約" を返す
       */
      @Test
      void 要約を保持している場合_要約を返す() {
        SES_AI_T_SKILLSHEET_PERSON entity = entity();
        entity.setContentSummary("要約");

        assertThat(entity.getContentSummary()).isEqualTo("要約");
      }
    }
  }

  @Nested
  class getCheckSql {

    @Nested
    class 異常系 {

      /**
       * 重複チェック用の SQL を求めると、未対応として例外を投げることを確かめる。 同じ要員の二重登録を判定できない状態で登録させない。
       *
       * @given 結合Entity
       * @when getCheckSql を実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 呼び出された場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> entity().getCheckSql())
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 異常系 {

      /**
       * 登録をサポートせず、例外を投げることを確かめる。 結合の結果を単体で登録すると、スキルシートと要員の対応が壊れる。
       *
       * @given 結合Entity
       * @when insert を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 登録を呼び出した場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> entity().insert((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 異常系 {

      /**
       * 主キーによる取得をサポートせず、例外を投げることを確かめる。 結合の結果を単体の取得として扱うと、結合先の項目が欠ける。
       *
       * @given 結合Entity
       * @when selectByPk を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 主キーによる取得を呼び出した場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> entity().selectByPk((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 異常系 {

      /**
       * 更新をサポートせず、例外を投げることを確かめる。 結合の結果を更新すると、結合先の要員の値を誤って書き換える。
       *
       * @given 結合Entity
       * @when updateByPk を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 更新を呼び出した場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> entity().updateByPk((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 異常系 {

      /**
       * 削除をサポートせず、例外を投げることを確かめる。 結合の結果から削除すると、要員の行が消えるおそれがある。
       *
       * @given 結合Entity
       * @when deleteByPk を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 削除を呼び出した場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> entity().deleteByPk((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class embedding {

    @Nested
    class 異常系 {

      /**
       * エンベディングを求めると、読み取り専用のため未対応として例外を投げることを確かめる。 結合の結果から外部のAPIを呼び出さないようにする。
       *
       * @given 結合Entity、変換器
       * @when embedding を変換器で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 呼び出された場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> entity().embedding(mock(Transformer.class)))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class uniqueCheck {

    @Nested
    class 異常系 {

      /**
       * 重複チェックを求めると、読み取り専用のため未対応として例外を投げることを確かめる。 結合の結果で重複判定の問い合わせを行わないようにする。
       *
       * @given 結合Entity、接続、類似度閾値 0.9
       * @when uniqueCheck を接続と類似度閾値で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 呼び出された場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> entity().uniqueCheck(mock(Connection.class), 0.9))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }
}
