package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** {@link SES_AI_T_PERSON} の単体テスト。 */
class SES_AI_T_PERSONTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 要員IDの値。 */
  private static final String PERSON_ID = "0123456789";

  /** 登録日時と有効期限の文字列。 */
  private static final String DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "person_id",
    "from_group",
    "from_id",
    "from_name",
    "mail_subject",
    "raw_content",
    "content_summary",
    "file_id",
    "unit_price",
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
   * 項目が揃った要員を生成する。
   *
   * @return 生成した要員
   */
  /**
   * 要約をエンベディングしたベクトルを持つ要員を生成する。
   *
   * @return ベクトルを持つ要員
   * @throws Exception エンベディングに失敗した場合
   */
  private static SES_AI_T_PERSON personWithVector() throws Exception {
    SES_AI_T_PERSON person = person();
    Transformer transformer = mock(Transformer.class);
    when(transformer.embedding("要約")).thenReturn(new float[] {0.1f, 0.2f});
    person.embedding(transformer);
    return person;
  }

  private static SES_AI_T_PERSON person() {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);
    person.setPersonId(PERSON_ID);
    person.setFromGroup("G001");
    person.setFromId("S001");
    person.setFromName("送信者");
    person.setMailSubject("件名");
    person.setRawContent("原文");
    person.setContentSummary("要約");
    person.setFileId("F001");
    person.setUnitPrice(new Money(600_000L));
    person.setName("山田太郎");
    person.setAge(35);
    person.setGender(Gender.Man);
    person.setNationality("日本");
    person.setStartDate(new OriginalDateTime("2026-02-01 00:00:00"));
    person.setPlace("東京");
    person.setArea(Area.関東_首都圏);
    person.setOfficeAvailability(3);
    person.setOrganization("組織");
    person.setExperiences("経験1\n経験2");
    person.setNgRequirements("NG1\nNG2");
    person.setOtherRequirements("その他1\nその他2");
    person.setUrl("https://example.com");
    person.setRegisterDate(new OriginalDateTime(DATE_TEXT));
    person.setRegisterUser("U001");
    person.setTtl(new OriginalDateTime(DATE_TEXT));
    return person;
  }

  /**
   * 1行分の要員の結果セットを作る。
   *
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet row() throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            PERSON_ID,
            "G001",
            "S001",
            "送信者",
            "件名",
            "原文",
            "要約",
            "F001",
            new BigDecimal(600_000),
            "山田太郎",
            35,
            "Man",
            "日本",
            "2026-02-01 00:00:00",
            "東京",
            "関東_首都圏",
            3,
            "組織",
            "経験",
            "NG",
            "その他",
            "https://example.com",
            DATE_TEXT,
            "U001",
            DATE_TEXT)
        .build();
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 要員をテナントの条件で管理する。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_T_PERSON を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_T_PERSON(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無い要員は他テナントの案件と結び付く。
       *
       * @given テナントIDが null
       * @when SES_AI_T_PERSON を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_PERSON(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class to要員選出用文章 {

    @Nested
    class 正常系 {

      /**
       * 要員IDと原文を連結した選出用の文章を返すことを確かめる。 LLM に要員を選ばせる入力に使う。
       *
       * @given 要員ID "0123456789"、原文 "原文"
       * @when to要員選出用文章 を実行する
       * @then 要員IDと原文を並べた文章を返す
       */
      @Test
      void 要員IDと原文がある場合_連結した文章を返す() {
        assertThat(person().to要員選出用文章()).isEqualTo("要員ID：0123456789内容：原文");
      }
    }
  }

  @Nested
  class setPlace {

    @Nested
    class 正常系 {

      /**
       * 勤務地を設定すると、そのまま保持することを確かめる。 画面の入力した勤務地を保存する。
       *
       * @given 勤務地が "東京"
       * @when setPlace を "東京" で実行する
       * @then 勤務地に "東京" を保持する
       */
      @Test
      void 勤務地を指定した場合_指定した勤務地を返す() {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);

        person.setPlace("東京");

        assertThat(person.getPlace()).isEqualTo("東京");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 勤務地が未指定の場合に null を保持することを確かめる。 未指定の文字列を勤務地として表示させない。
       *
       * @given 勤務地が "unknown"
       * @when setPlace を "unknown" で実行する
       * @then 勤務地をnullにする
       */
      @Test
      void 勤務地が未指定の場合_nullを返す() {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);

        person.setPlace("unknown");

        assertThat(person.getPlace()).isNull();
      }

      /**
       * 勤務地が30文字を超える場合に、30文字で切り詰めることを確かめる。 保存先の列の長さを超えないようにする。
       *
       * @given 勤務地が 31 文字の文字列
       * @when setPlace を 31 文字の文字列で実行する
       * @then 勤務地を先頭の 30 文字に切り詰めて保持する
       */
      @Test
      void 勤務地が31文字の場合_先頭の30文字を返す() {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);

        person.setPlace("あ".repeat(31));

        assertThat(person.getPlace()).isEqualTo("あ".repeat(30));
      }
    }
  }

  @Nested
  class isスキルシート登録済 {

    @Nested
    class 正常系 {

      /**
       * スキルシートのファイルIDがある場合に登録済みと判定することを確かめる。 スキルシートを紐づけた要員を一覧で区別する。
       *
       * @given ファイルIDが "F001"
       * @when isスキルシート登録済 を実行する
       * @then trueを返す
       */
      @Test
      void ファイルIDがある場合_trueを返す() {
        assertThat(person().isスキルシート登録済()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイルIDが null の場合に未登録と判定することを確かめる。 スキルシートの無い要員を登録済みと誤認しない。
       *
       * @given ファイルIDが null
       * @when isスキルシート登録済 を実行する
       * @then falseを返す
       */
      @Test
      void ファイルIDがnullの場合_falseを返す() {
        SES_AI_T_PERSON person = person();
        person.setFileId(null);

        assertThat(person.isスキルシート登録済()).isFalse();
      }

      /**
       * ファイルIDが空文字の場合に未登録と判定することを確かめる。 空のファイルIDはスキルシートを指さない。
       *
       * @given ファイルIDが空文字
       * @when isスキルシート登録済 を実行する
       * @then falseを返す
       */
      @Test
      void ファイルIDが空文字の場合_falseを返す() {
        SES_AI_T_PERSON person = person();
        person.setFileId("");

        assertThat(person.isスキルシート登録済()).isFalse();
      }
    }
  }

  @Nested
  class updateFileIdByPk {

    @Nested
    class 正常系 {

      /**
       * 要員IDとテナントIDが一致する行のファイルIDを更新することを確かめる。 要員にスキルシートを紐づける。
       *
       * @given 要員ID "0123456789"、ファイルID "F001"、更新件数が 1 件
       * @when updateFileIdByPk を接続で実行する
       * @then ファイルID、要員ID、テナントIDの順に設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(person().updateFileIdByPk(connection)).isTrue();

        verify(statement).setString(1, "F001");
        verify(statement).setString(2, PERSON_ID);
        verify(statement).setString(3, TENANT_ID);
      }

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象の要員が無いときに紐づけ成功と誤認しない。
       *
       * @given 更新件数が 0 件
       * @when updateFileIdByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(person().updateFileIdByPk(connection)).isFalse();
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に更新を拒否することを確かめる。 接続が無いまま更新を実行できない。
       *
       * @given 接続が null
       * @when updateFileIdByPk を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> person().updateFileIdByPk(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全要員のファイルIDを書き換えるおそれがある。
       *
       * @given 要員IDが null
       * @when updateFileIdByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 要員IDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_T_PERSON person = person();
        person.setPersonId(null);

        assertThat(person.updateFileIdByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class clearFileIdWhereFileId {

    @Nested
    class 正常系 {

      /**
       * ファイルIDが一致する要員の紐づけを外し、更新件数を返すことを確かめる。 スキルシートを削除した後に要員の紐づけを残さない。
       *
       * @given ファイルID "F001"、更新件数が 2 件
       * @when clearFileIdWhereFileId を接続とファイルID "F001" で実行する
       * @then 2件を返す
       */
      @MockDbConnection
      @Test
      void 更新件数が2件の場合_2件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 2);

        assertThat(SES_AI_T_PERSON.clearFileIdWhereFileId(connection, "F001")).isEqualTo(2);

        verify(statement).setString(1, "F001");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイルIDが null の場合に DB を呼ばずに 0 件を返すことを確かめる。 キーの無い更新は全要員の紐づけを外すおそれがある。
       *
       * @given ファイルIDが null
       * @when clearFileIdWhereFileId を接続と null のファイルIDで実行する
       * @then 0件を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ファイルIDがnullの場合_0件を返しDBを呼ばない(final Connection connection) throws SQLException {
        assertThat(SES_AI_T_PERSON.clearFileIdWhereFileId(connection, null)).isZero();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class findSamePerson {

    @Nested
    class 正常系 {

      /**
       * 同一要員が見つかった場合に、要員IDと単価を持つ要員を返すことを確かめる。 同じ要員を重複して登録しないために使う。
       *
       * @given 検索の結果が 1 行要員ID "0123456789"、単価 600000
       * @when findSamePerson を氏名 "山田太郎"、年齢 35、性別 Man、場所 "渋谷" で実行する
       * @then 要員ID "0123456789" と単価 600000 の要員を返す
       */
      @MockDbConnection
      @Test
      void 同一要員がある場合_要員IDと単価を持つ要員を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns("person_id", "unit_price")
                .row(PERSON_ID, new BigDecimal(600_000))
                .build());

        SES_AI_T_PERSON result =
            SES_AI_T_PERSON.findSamePerson(
                connection, TENANT_ID, "山田太郎", 35, Gender.Man, "渋谷", List.of());

        assertThat(result.getPersonId()).isEqualTo(PERSON_ID);
        assertThat(result.getUnitPrice().getValue()).isEqualByComparingTo(new BigDecimal(600_000));
      }

      /**
       * 年齢・性別・場所が記載ありの場合に、その値を検索条件に設定することを確かめる。 記載のある項目は一致を求める。
       *
       * @given 年齢 35、性別 Man、前後に空白のある場所 " 渋谷 "
       * @when findSamePerson を実行する
       * @then 年齢 35、性別 "Man"、場所 "渋谷" を条件に設定する
       */
      @MockDbConnection
      @Test
      void 年齢と性別と場所がある場合_同一要員が無ければnullを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder().columns("person_id", "unit_price").build());

        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, TENANT_ID, "山田太郎", 35, Gender.Man, " 渋谷 ", List.of()))
            .isNull();

        verify(statement).setString(2, "山田太郎");
        verify(statement).setObject(3, 35);
        verify(statement).setString(4, "Man");
        verify(statement).setString(5, "渋谷");
      }

      /**
       * 年齢・性別・場所が記載なしの場合に、条件を null にして比較を除外することを確かめる。 記載の無い項目で別人と判定しない。
       *
       * @given 年齢 -1、性別 Unknown、場所が空文字
       * @when findSamePerson を実行する
       * @then 年齢・性別・場所の条件を null に設定する
       */
      @MockDbConnection
      @Test
      void 年齢と性別と場所が不明の場合_同一要員が無ければnullを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder().columns("person_id", "unit_price").build());

        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, TENANT_ID, "山田太郎", -1, Gender.Unknown, "", List.of()))
            .isNull();

        verify(statement).setObject(3, null);
        verify(statement).setString(4, null);
        verify(statement).setString(5, null);
      }

      /**
       * 性別と場所が null の場合に、条件を null にして比較を除外することを確かめる。 AI が項目を出力しない場合に備える。
       *
       * @given 性別が null、場所が null
       * @when findSamePerson を実行する
       * @then 性別・場所の条件を null に設定する
       */
      @MockDbConnection
      @Test
      void 性別と場所がnullの場合_同一要員が無ければnullを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder().columns("person_id", "unit_price").build());

        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, TENANT_ID, "山田太郎", 35, null, null, List.of()))
            .isNull();

        verify(statement).setString(4, null);
        verify(statement).setString(5, null);
      }

      /**
       * 単価が未設定の同一要員を、単価を空の値として返すことを確かめる。 単価の無い要員でも候補として扱えるようにする。
       *
       * @given 検索の結果が 1 行要員ID "0123456789"、単価が null
       * @when findSamePerson を実行する
       * @then 単価が空の要員を返す
       */
      @MockDbConnection
      @Test
      void 単価が未設定の同一要員の場合_単価が空の要員を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns("person_id", "unit_price")
                .row(PERSON_ID, null)
                .build());

        SES_AI_T_PERSON result =
            SES_AI_T_PERSON.findSamePerson(
                connection, TENANT_ID, "山田太郎", 35, Gender.Man, "渋谷", List.of());

        assertThat(result.getUnitPrice().isEmpty()).isTrue();
      }
    }

    @Nested
    class 境界値 {
      /**
       * 除外する要員IDを渡した場合に、その要員IDを検索の条件に渡すことを確かめる。 同じメッセージで登録した要員を、同一要員として扱わないためである。
       *
       * @given 除外する要員IDが "P1" と "P2"
       * @when findSamePerson を実行する
       * @then 要員ID "P1" と "P2" の配列を作って6番目の条件に渡し、nullを返す
       */
      @MockDbConnection
      @Test
      void 除外する要員IDがある場合_除外した検索でnullを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder().columns("person_id", "unit_price").build());

        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, TENANT_ID, "山田太郎", 35, Gender.Man, "渋谷", List.of("P1", "P2")))
            .isNull();

        verify(connection).createArrayOf("varchar", new String[] {"P1", "P2"});
        verify(statement).setArray(eq(6), any());
      }

      /**
       * 同一要員が無い場合に null を返すことを確かめる。 無いのに候補を返すと、別人の要員を更新する。
       *
       * @given 検索の結果が 0 行
       * @when findSamePerson を実行する
       * @then nullを返す
       */
      @MockDbConnection
      @Test
      void 同一要員が無い場合_nullを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection, MockResultSetBuilder.builder().columns("person_id", "unit_price").build());

        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, TENANT_ID, "山田太郎", 35, Gender.Man, "渋谷", List.of()))
            .isNull();
      }

      /**
       * 氏名が null または空白の場合に DB を呼ばずに null を返すことを確かめる。 氏名の無い検索は他の要員と一致してしまう。
       *
       * @given 氏名が null と空白
       * @when findSamePerson を実行する
       * @then どちらも null を返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 氏名がnullまたは空白の場合_nullを返しDBを呼ばない(final Connection connection) throws SQLException {
        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, TENANT_ID, null, 35, Gender.Man, "渋谷", List.of()))
            .isNull();
        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, TENANT_ID, " ", 35, Gender.Man, "渋谷", List.of()))
            .isNull();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * テナントIDが null の場合に DB を呼ばずに null を返すことを確かめる。 テナントの無い検索は他テナントの要員を返す。
       *
       * @given テナントIDが null
       * @when findSamePerson を実行する
       * @then nullを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_nullを返しDBを呼ばない(final Connection connection) throws SQLException {
        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, null, "山田太郎", 35, Gender.Man, "渋谷", List.of()))
            .isNull();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * DB 接続が null の場合に null を返すことを確かめる。 接続が無い状態で検索を行わない。
       *
       * @given 接続が null
       * @when findSamePerson を接続 null で実行する
       * @then nullを返す
       */
      @Test
      void 接続がnullの場合_nullを返す() throws SQLException {
        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    null, TENANT_ID, "山田太郎", 35, Gender.Man, "渋谷", List.of()))
            .isNull();
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * テナントIDがT001の場合に、T001のテナントの同一要員だけを探すことを確かめる。 他テナントの要員を同一とみなして上書きしないためである。
       *
       * @given テナントIDが "T001"
       * @when findSamePerson を実行する
       * @then テナントID "T001" を検索の1番目の条件に渡して、nullを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_T001の同一要員が無ければnullを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder().columns("person_id", "unit_price").build());

        assertThat(
                SES_AI_T_PERSON.findSamePerson(
                    connection, TENANT_ID, "山田太郎", 35, Gender.Man, "渋谷", List.of()))
            .isNull();

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class findSimilarRecord {

    @Nested
    class 正常系 {

      /**
       * 類似する要員が見つかった場合に、要員IDと単価を持つ要員を返すことを確かめる。 重複の候補を画面に示す。
       *
       * @given 検索の結果が 1 行要員ID "0123456789"、単価 600000
       * @when findSimilarRecord を接続、テナントID "T001"、本文 "原文"、類似度 0.8 で実行する
       * @then 要員ID "0123456789" の要員を返す
       */
      @MockDbConnection
      @Test
      void 類似する要員がある場合_要員IDを持つ要員を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns("person_id", "unit_price")
                .row(PERSON_ID, new BigDecimal(600_000))
                .build());

        SES_AI_T_PERSON result =
            SES_AI_T_PERSON.findSimilarRecord(connection, TENANT_ID, "原文", 0.8);

        assertThat(result.getPersonId()).isEqualTo(PERSON_ID);
        assertThat(result.getUnitPrice().getValue()).isEqualByComparingTo(new BigDecimal(600_000));
      }

      /**
       * 本文のURLを除いて類似検索の条件に使うことを確かめる。 URLの違いで同じ要員を別物と判定しない。
       *
       * @given 本文が "原文 https://example.com/a"
       * @when findSimilarRecord を接続、テナントID "T001"、その本文、類似度 0.8 で実行する
       * @then URLを除いた "原文 " を検索条件に2回設定する
       */
      @MockDbConnection
      @Test
      void 本文にURLがある場合_URLを除いた本文を条件に設定してnullを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder().columns("person_id", "unit_price").build());

        SES_AI_T_PERSON.findSimilarRecord(connection, TENANT_ID, "原文 https://example.com/a", 0.8);

        verify(statement).setString(2, "原文 ");
        verify(statement).setString(3, "原文 ");
      }

      /**
       * 単価が未設定の類似要員を、単価を空の値として返すことを確かめる。 単価の無い要員でも候補として扱えるようにする。
       *
       * @given 検索の結果が 1 行要員ID "0123456789"、単価が null
       * @when findSimilarRecord を接続、テナントID "T001"、本文 "原文"、類似度 0.8 で実行する
       * @then 単価が空の要員を返す
       */
      @MockDbConnection
      @Test
      void 単価が未設定の類似要員の場合_単価が空の要員を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns("person_id", "unit_price")
                .row(PERSON_ID, null)
                .build());

        SES_AI_T_PERSON result =
            SES_AI_T_PERSON.findSimilarRecord(connection, TENANT_ID, "原文", 0.8);

        assertThat(result.getUnitPrice().isEmpty()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 類似する要員が無い場合に null を返すことを確かめる。 類似が無いのに候補を返すと、重複でない要員を登録から除外する。
       *
       * @given 検索の結果が 0 行
       * @when findSimilarRecord を接続、テナントID "T001"、本文 "原文"、類似度 0.8 で実行する
       * @then nullを返す
       */
      @MockDbConnection
      @Test
      void 類似する要員が無い場合_nullを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection, MockResultSetBuilder.builder().columns("person_id", "unit_price").build());

        assertThat(SES_AI_T_PERSON.findSimilarRecord(connection, TENANT_ID, "原文", 0.8)).isNull();
      }

      /**
       * 本文が null の場合に DB を呼ばずに null を返すことを確かめる。 条件が欠けた検索で全件を比べない。
       *
       * @given 本文が null
       * @when findSimilarRecord を接続、テナントID "T001"、本文 null で実行する
       * @then nullを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 本文がnullの場合_nullを返しDBを呼ばない(final Connection connection) throws SQLException {
        assertThat(SES_AI_T_PERSON.findSimilarRecord(connection, TENANT_ID, null, 0.8)).isNull();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * テナントIDが null の場合に DB を呼ばずに null を返すことを確かめる。 テナントの無い検索は他テナントの要員を返す。
       *
       * @given テナントIDが null
       * @when findSimilarRecord を接続、テナントID null、本文 "原文" で実行する
       * @then nullを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_nullを返しDBを呼ばない(final Connection connection) throws SQLException {
        assertThat(SES_AI_T_PERSON.findSimilarRecord(connection, null, "原文", 0.8)).isNull();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * DB 接続が null の場合に null を返すことを確かめる。 接続が無い状態で検索を行わない。
       *
       * @given 接続が null
       * @when findSimilarRecord を接続 null、テナントID "T001"、本文 "原文" で実行する
       * @then nullを返す
       */
      @Test
      void 接続がnullの場合_nullを返す() throws SQLException {
        assertThat(SES_AI_T_PERSON.findSimilarRecord(null, TENANT_ID, "原文", 0.8)).isNull();
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
        assertThat(person().getRawContent()).isEqualTo("原文");
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
        assertThat(person().getContentSummary()).isEqualTo("要約");
      }
    }
  }

  @Nested
  class getCheckSql {

    @Nested
    class 正常系 {

      /**
       * 重複チェックの SQL を返すことを確かめる。 同じ原文の要員を二重に登録しないための判定に使う。
       *
       * @given 要員の原文が "原文"
       * @when getCheckSql を実行する
       * @then 原文の類似度で件数を数える SQL を返す
       */
      @Test
      void 重複チェックの場合_類似度で件数を数える文を返す() {
        assertThat(person().getCheckSql()).startsWith("SELECT COUNT(*) FROM SES_AI_T_PERSON");
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * 要員の項目を列の順に設定し、発行した要員IDで登録することを確かめる。 要員IDはテナントの中で一意にする。
       *
       * @given 項目が揃った要員、発行されるUUIDが固定値、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 要員IDに "0123456789" を発行し、テナントIDを含めて1件を返す
       */
      @MockDbConnection
      @Test
      void 項目が揃っている場合_発行した要員IDで1件を返す(final Connection connection) throws Exception {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_PERSON person = personWithVector();
        UUID fixed = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");

        try (MockedStatic<UUID> uuid = mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(fixed);
          assertThat(person.insert(connection)).isEqualTo(1);
        }

        assertThat(person.getPersonId()).isEqualTo(PERSON_ID);
        verify(statement).setString(1, PERSON_ID);
        verify(statement).setObject(9, new BigDecimal(600_000));
        verify(statement).setString(eq(10), anyString());
        verify(statement).setString(13, "Man");
        verify(statement).setString(17, "関東_首都圏");
        verify(statement).setObject(18, 3);
        verify(statement).setTimestamp(26, Timestamp.valueOf(DATE_TEXT));
        verify(statement).setString(27, TENANT_ID);
      }

      /**
       * 単価、ベクトル、性別、開始日、エリア、出社許容、登録日時、有効期限が未設定の場合に、各項目の位置に null を設定することを確かめる。 未設定の項目があっても登録を失敗させない。
       *
       * @given 任意の項目が未設定、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 未設定の位置に null を設定して1件を返す
       */
      @MockDbConnection
      @Test
      void 任意の項目が未設定の場合_nullを設定して1件を返す(final Connection connection) throws Exception {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);

        assertThat(person.insert(connection)).isEqualTo(1);

        verify(statement).setObject(9, null);
        verify(statement).setString(10, null);
        verify(statement).setString(13, null);
        verify(statement).setTimestamp(15, null);
        verify(statement).setString(17, null);
        verify(statement).setTimestamp(24, null);
        verify(statement).setTimestamp(26, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * 要員IDとテナントIDが一致する行の項目を読み込むことを確かめる。 保存済みの要員を編集画面に表示する。
       *
       * @given 要員ID "0123456789"、SELECT の結果が 1 行性別 "Man"、エリア "関東_首都圏"
       * @when selectByPk を接続で実行する
       * @then trueを返し、性別を Man、エリアを 関東_首都圏、年齢を 35 に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row());
        SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);
        person.setPersonId(PERSON_ID);

        assertThat(person.selectByPk(connection)).isTrue();

        assertThat(person.getGender()).isEqualTo(Gender.Man);
        assertThat(person.getArea()).isEqualTo(Area.関東_首都圏);
        assertThat(person.getAge()).isEqualTo(35);
      }

      /**
       * 任意の項目が NULL の行を読み込み、対応する項目を未設定にすることを確かめる。 未入力の要員を編集画面で壊さずに表示する。
       *
       * @given 要員ID "0123456789"、SELECT の結果が 1 行、単価・年齢・性別・エリア・出社許容が NULL
       * @when selectByPk を接続で実行する
       * @then trueを返し、単価・年齢・性別・エリア・出社許容を未設定にする
       */
      @MockDbConnection
      @Test
      void 任意の項目がNULLの行の場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(
                    PERSON_ID,
                    "G001",
                    "S001",
                    "送信者",
                    "件名",
                    "原文",
                    "要約",
                    "F001",
                    null,
                    "山田太郎",
                    null,
                    null,
                    "日本",
                    "2026-02-01 00:00:00",
                    "東京",
                    null,
                    null,
                    "組織",
                    "経験",
                    "NG",
                    "その他",
                    "https://example.com",
                    DATE_TEXT,
                    "U001",
                    DATE_TEXT)
                .build());
        SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);
        person.setPersonId(PERSON_ID);

        assertThat(person.selectByPk(connection)).isTrue();

        assertThat(person.getUnitPrice().getValue()).isNull();
        assertThat(person.getAge()).isNull();
        assertThat(person.getGender()).isNull();
        assertThat(person.getArea()).isNull();
        assertThat(person.getOfficeAvailability()).isNull();
      }

      /**
       * 該当行が無い場合に false を返し、項目を変えないことを確かめる。 存在しない要員を読み込んでも、入力中の値を壊さない。
       *
       * @given 要員ID "0123456789"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then falseを返し、氏名を変えない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_PERSON person = person();

        assertThat(person.selectByPk(connection)).isFalse();

        assertThat(person.getName()).isEqualTo("山田太郎");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返すおそれがある。
       *
       * @given 要員IDが null
       * @when selectByPk を接続で実行する
       * @then falseを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 要員IDがnullの場合_DBを呼ばずfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_PERSON(TENANT_ID).selectByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * 要員IDとテナントIDが一致する行を、項目の値で更新することを確かめる。 要員情報の変更を保存する。
       *
       * @given 要員ID "0123456789"、項目が揃った要員、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 要員IDを条件にして更新し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(person().updateByPk(connection)).isTrue();

        verify(statement).setString(24, PERSON_ID);
      }

      /**
       * ベクトルを持つ要員の場合に、ベクトルの文字列を更新の値として設定することを確かめる。 検索に使うベクトルを更新後も保つ。
       *
       * @given 要員ID "0123456789"、要約をエンベディングしたベクトルを持つ要員、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then ベクトルの位置に文字列を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void ベクトルがある場合_ベクトルの文字列を設定してtrueを返す(final Connection connection) throws Exception {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(personWithVector().updateByPk(connection)).isTrue();

        verify(statement).setString(eq(9), anyString());
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象の要員が無いときに更新成功と誤認しない。
       *
       * @given 要員ID "0123456789"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(person().updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全件を書き換えるおそれがある。
       *
       * @given 要員IDが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 要員IDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_PERSON(TENANT_ID).updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 任意の項目が未設定の場合に、各項目の位置に null を設定して更新することを確かめる。 未設定の項目があっても更新を失敗させない。
       *
       * @given 単価、ベクトル、性別、開始日、エリア、出社許容、有効期限が未設定、要員ID "0123456789"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 未設定の位置に null を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 任意の項目が未設定の場合_nullを設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);
        person.setPersonId(PERSON_ID);

        assertThat(person.updateByPk(connection)).isTrue();

        verify(statement).setObject(8, null);
        verify(statement).setString(9, null);
        verify(statement).setObject(11, null);
        verify(statement).setString(12, null);
        verify(statement).setTimestamp(14, null);
        verify(statement).setString(16, null);
        verify(statement).setObject(17, null);
        verify(statement).setTimestamp(23, null);
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * 要員IDとテナントIDが一致する行を削除することを確かめる。 不要になった要員を残さない。
       *
       * @given 要員ID "0123456789"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then 要員IDを条件にして削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(person().deleteByPk(connection)).isTrue();

        verify(statement).setString(1, PERSON_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象の要員が無いときに削除成功と誤認しない。
       *
       * @given 要員ID "0123456789"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(person().deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全件を消すおそれがある。
       *
       * @given 要員IDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 要員IDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_PERSON(TENANT_ID).deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class toNotificationInfo {

    @Nested
    class 正常系 {

      /**
       * 氏名と年齢と性別と単価を組み合わせた通知文を返すことを確かめる。 通知の1行に要員の概要を並べる。
       *
       * @given 氏名 "山田太郎"、年齢 35、性別 Man、単価 600000 円
       * @when toNotificationInfo を実行する
       * @then 氏名、年齢、性別、単価の表示を並べた文字列を返す
       */
      @Test
      void 氏名と年齢と性別と単価がある場合_並べた通知文を返す() {
        assertThat(person().toNotificationInfo())
            .startsWith("山田太郎 35歳 " + Gender.Man.toJapanese() + "｜")
            .endsWith(new Money(600_000L).toPriceLabel());
      }
    }

    @Nested
    class 境界値 {

      /**
       * 氏名が未指定の場合に既定の氏名で通知文を作ることを確かめる。 氏名の無い要員を空欄で通知しない。
       *
       * @given 氏名が null、年齢が 0、性別が Unknown、単価が null
       * @when toNotificationInfo を実行する
       * @then 既定の氏名 "氏名不明" だけを返す
       */
      @Test
      void 氏名が未指定の場合_既定の氏名を返す() {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);
        person.setAge(0);
        person.setGender(Gender.Unknown);

        assertThat(person.toNotificationInfo()).isEqualTo("氏名不明");
      }

      /**
       * 年齢と性別が未設定の場合に、氏名と単価だけの通知文を返すことを確かめる。 未設定の項目を空の表示として出さない。
       *
       * @given 氏名 "山田太郎"、年齢が null、性別が null、単価 600000 円
       * @when toNotificationInfo を実行する
       * @then 氏名と単価の表示だけを並べた文字列を返す
       */
      @Test
      void 年齢と性別が未設定の場合_氏名と単価だけの通知文を返す() {
        SES_AI_T_PERSON person = person();
        person.setAge(null);
        person.setGender(null);

        assertThat(person.toNotificationInfo())
            .isEqualTo("山田太郎｜" + new Money(600_000L).toPriceLabel());
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
      void 経験がある場合_改行で分けた2件の一覧を返す() {
        assertThat(person().getExperiencesAsList()).containsExactly("経験1", "経験2");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 経験が未設定の場合に空の一覧を返すことを確かめる。 経験の無い要員を表示で落とさない。
       *
       * @given 経験が null
       * @when getExperiencesAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void 経験が未設定の場合_空の一覧を返す() {
        assertThat(new SES_AI_T_PERSON(TENANT_ID).getExperiencesAsList()).isEmpty();
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
      void その他条件がある場合_改行で分けた2件の一覧を返す() {
        assertThat(person().getOtherRequirementsAsList()).containsExactly("その他1", "その他2");
      }
    }

    @Nested
    class 境界値 {

      /**
       * その他条件が未設定の場合に空の一覧を返すことを確かめる。 条件の無い要員を表示で落とさない。
       *
       * @given その他条件が null
       * @when getOtherRequirementsAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void その他条件が未設定の場合_空の一覧を返す() {
        assertThat(new SES_AI_T_PERSON(TENANT_ID).getOtherRequirementsAsList()).isEmpty();
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
      void NG条件がある場合_改行で分けた2件の一覧を返す() {
        assertThat(person().getNgRequirementsAsList()).containsExactly("NG1", "NG2");
      }
    }

    @Nested
    class 境界値 {

      /**
       * NG条件が未設定の場合に空の一覧を返すことを確かめる。 NG条件の無い要員を表示で落とさない。
       *
       * @given NG条件が null
       * @when getNgRequirementsAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void NG条件が未設定の場合_空の一覧を返す() {
        assertThat(new SES_AI_T_PERSON(TENANT_ID).getNgRequirementsAsList()).isEmpty();
      }
    }
  }

  @Nested
  class getSummaryWithTargetPrice_Money {

    @Nested
    class 正常系 {

      /**
       * 対象単価を指定すると、既定の係数で単価を含む要約を返すことを確かめる。 要員の提案文の要約に使う。
       *
       * @given 項目が揃った要員、対象単価が 1000000 円単価不問ではない
       * @when getSummaryWithTargetPrice を対象単価で実行する
       * @then 氏名、年齢、性別、国籍、開始、単価、場所、出社、経歴、URLを含む要約を返す
       */
      @Test
      void 対象単価がある場合_各項目を含む要約を返す() {
        assertThat(person().getSummaryWithTargetPrice(new Money(1_000_000L)))
            .startsWith("■名前: 山田太郎\n■年齢: 35歳\n")
            .contains("■国籍: 日本\n")
            .contains("■開始: ")
            .contains("■単価: " + new Money(1_000_000L).toJapaneseFormat() + "\n")
            .contains("■場所: 東京\n")
            .contains("■出社: 週3まで可\n")
            .contains("■経歴\n経験1\n経験2\n")
            .contains("■URL: https://example.com\n");
      }

      /**
       * 対象単価が単価不問の場合に、要員の単価に係数を掛けた単価を要約に含めることを確かめる。 応相談の要員でも単価の目安を示す。
       *
       * @given 要員単価が 600000 円、対象単価が単価不問
       * @when getSummaryWithTargetPrice を単価不問の対象単価で実行する
       * @then 要員単価に既定の係数 1.1 を掛けた単価を要約に含める
       */
      @Test
      void 対象単価が単価不問の場合_係数を掛けた単価を含む要約を返す() {
        assertThat(person().getSummaryWithTargetPrice(new Money(9_990_000L)))
            .contains("■単価: " + new Money(600_000L).multiply(1.1).toJapaneseFormat() + "\n");
      }

      /**
       * 出社許容が0の場合にフルリモート希望と返すことを確かめる。 リモートを希望する要員の条件を示す。
       *
       * @given 出社許容が 0、対象単価が null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then フルリモート希望を含む要約を返す
       */
      @Test
      void 出社許容がフルリモート希望の場合_フルリモート希望を含む要約を返す() {
        SES_AI_T_PERSON person = person();
        person.setOfficeAvailability(0);

        assertThat(person.getSummaryWithTargetPrice(null)).contains("■出社: フルリモート希望\n");
      }

      /**
       * 出社許容が5の場合に常駐可と返すことを確かめる。 常駐できる要員の条件を示す。
       *
       * @given 出社許容が 5、対象単価が null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 常駐可を含む要約を返す
       */
      @Test
      void 出社許容が常駐可の場合_常駐可を含む要約を返す() {
        SES_AI_T_PERSON person = person();
        person.setOfficeAvailability(5);

        assertThat(person.getSummaryWithTargetPrice(null)).contains("■出社: 常駐可\n");
      }

      /**
       * 対象単価が空の場合に単価の行を含めないことを確かめる。 単価の無い比較を要約に出さない。
       *
       * @given 対象単価が空の単価
       * @when getSummaryWithTargetPrice を空の対象単価で実行する
       * @then 単価の行を含まない要約を返す
       */
      @Test
      void 対象単価が空の場合_単価の行を含めない要約を返す() {
        assertThat(person().getSummaryWithTargetPrice(Money.empty())).doesNotContain("■単価: ");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 年齢が0以下、開始日が未設定、場所と経歴とURLが空の場合に、その行を含めないことを確かめる。 未設定の項目を空の見出しとして出さない。
       *
       * @given 年齢が 0、開始日が null、場所が空文字、経歴が空文字、URLが空文字
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 年齢、開始、場所、経歴、URLの行を含まない要約を返す
       */
      @Test
      void 任意の項目が未設定の場合_その行を含めない要約を返す() {
        SES_AI_T_PERSON person = person();
        person.setAge(0);
        person.setStartDate(null);
        person.setPlace("");
        person.setExperiences("");
        person.setUrl("");

        assertThat(person.getSummaryWithTargetPrice(null))
            .doesNotContain("■年齢")
            .doesNotContain("■開始")
            .doesNotContain("■場所")
            .doesNotContain("■経歴")
            .doesNotContain("■URL");
      }

      /**
       * 開始日が空で、場所と経歴とURLが null の場合に、その行を含めないことを確かめる。 未入力の項目を要約に出さない。
       *
       * @given 開始日が日付の無いオブジェクト、場所と経歴とURLが null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 開始、場所、経歴、URLの行を含まない要約を返す
       */
      @Test
      void 開始日が空で場所と経歴とURLが未設定の場合_その行を含めない要約を返す() {
        SES_AI_T_PERSON person = person();
        person.setStartDate(new OriginalDateTime((String) null));
        person.setPlace(null);
        person.setExperiences(null);
        person.setUrl(null);

        assertThat(person.getSummaryWithTargetPrice(null))
            .doesNotContain("■開始")
            .doesNotContain("■場所")
            .doesNotContain("■経歴")
            .doesNotContain("■URL");
      }

      /**
       * 性別が未設定の場合に、性別の欄にハイフンを表示することを確かめる。 性別の無い要員を空欄で出さない。
       *
       * @given 性別が null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 性別にハイフンを含む要約を返す
       */
      @Test
      void 性別が未設定の場合_性別にハイフンを含む要約を返す() {
        SES_AI_T_PERSON p = person();
        p.setGender(null);
        assertThat(p.getSummaryWithTargetPrice(null)).contains("■性別: -\n");
      }

      /**
       * 年齢が未設定の場合に、年齢の行を含めないことを確かめる。 未設定の年齢を空欄の年齢として出さない。
       *
       * @given 年齢が null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 年齢の行を含まない要約を返す
       */
      @Test
      void 年齢が未設定の場合_年齢の行を含まない要約を返す() {
        SES_AI_T_PERSON p = person();
        p.setAge(null);
        assertThat(p.getSummaryWithTargetPrice(null)).doesNotContain("■年齢");
      }

      /**
       * 出社許容が未設定の場合に、出社の行を含めないことを確かめる。 未設定の出社許容で誤った表示をしない。
       *
       * @given 出社許容が null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 出社の行を含まない要約を返す
       */
      @Test
      void 出社許容が未設定の場合_出社の行を含まない要約を返す() {
        SES_AI_T_PERSON p = person();
        p.setOfficeAvailability(null);
        assertThat(p.getSummaryWithTargetPrice(null)).doesNotContain("■出社");
      }

      /**
       * 単価が未設定で単価不問の対象単価の場合に、単価の行を含めないことを確かめる。 掛ける単価が無いため誤った金額を出さない。
       *
       * @given 要員の単価が null、対象単価が単価不問
       * @when getSummaryWithTargetPrice を単価不問の対象単価で実行する
       * @then 単価の行を含まない要約を返す
       */
      @Test
      void 単価が未設定で単価不問の場合_単価の行を含まない要約を返す() {
        SES_AI_T_PERSON p = person();
        p.setUnitPrice(null);
        assertThat(p.getSummaryWithTargetPrice(new Money(9_990_000L))).doesNotContain("■単価");
      }
    }
  }

  @Nested
  class getSummaryWithTargetPrice_Money_double {

    @Nested
    class 正常系 {

      /**
       * 係数を指定すると、単価不問の要員の単価にその係数を掛けることを確かめる。 係数を画面から変えて単価の目安を調整する。
       *
       * @given 要員単価が 1000000 円、対象単価が単価不問、係数が 0.5
       * @when getSummaryWithTargetPrice を対象単価と係数 0.5 で実行する
       * @then 要員単価に 0.5 を掛けた単価を要約に含める
       */
      @Test
      void 係数を指定した場合_係数を掛けた単価を含む要約を返す() {
        SES_AI_T_PERSON person = person();
        person.setUnitPrice(new Money(1_000_000L));

        assertThat(person.getSummaryWithTargetPrice(new Money(9_990_000L), 0.5))
            .contains("■単価: " + new Money(1_000_000L).multiply(0.5).toJapaneseFormat() + "\n");
      }
    }
  }
}
