package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** {@link SES_AI_T_MATCH} の単体テスト。 */
class SES_AI_T_MATCHTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** マッチングIDの値。 */
  private static final String MATCHING_ID = "0123456789";

  /** 登録日時の文字列。 */
  private static final String REGISTER_DATE_TEXT = "2026-01-15 10:00:00";

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
    "place_evaluation_text",
    "office_evaluation_text",
    "other_evaluation_text",
    "profit",
    "register_date",
    "register_user"
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
   * 項目が揃ったマッチングを生成する。ステータスは進行中、点数は 80、利益は 1000000 円とする。
   *
   * @return 生成したマッチング
   */
  private static SES_AI_T_MATCH match() {
    SES_AI_T_MATCH match = new SES_AI_T_MATCH(TENANT_ID);
    match.setMatchingId(MATCHING_ID);
    match.setUserId("U001");
    match.setJobId("J001");
    match.setPersonId("P001");
    match.setJobContent("案件内容");
    match.setPersonContent("要員内容");
    match.setStatus(MatchingStatus.進行中);
    match.setEvaluationText("評価");
    match.setScore(80);
    match.setMustEvaluationText("必須");
    match.setWantEvaluationText("尚可");
    match.setPlaceEvaluationText("場所");
    match.setOfficeEvaluationText("出社");
    match.setOtherEvaluationText("その他");
    match.setProfit(new Money(1_000_000L));
    match.setRegisterDate(new OriginalDateTime(REGISTER_DATE_TEXT));
    match.setRegisterUser("U000");
    return match;
  }

  /**
   * 項目の一部が未設定のマッチングを生成する。ステータス、点数、利益、登録日時を未設定とする。
   *
   * @return 生成したマッチング
   */
  private static SES_AI_T_MATCH matchWithoutOptionalFields() {
    SES_AI_T_MATCH match = match();
    match.setStatus(null);
    match.setScore(null);
    match.setProfit(null);
    match.setRegisterDate(null);
    return match;
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 マッチングをテナントの条件で管理する。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_T_MATCH を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_T_MATCH(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無いマッチングは他テナントの案件と結び付く。
       *
       * @given テナントIDが null
       * @when SES_AI_T_MATCH を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_MATCH(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class hasJobId {

    @Nested
    class 正常系 {

      /**
       * 案件IDがある場合に true を返すことを確かめる。 案件と結び付くマッチングを判定する。
       *
       * @given 案件IDが "J001"
       * @when hasJobId を実行する
       * @then trueを返す
       */
      @Test
      void 案件IDがある場合_trueを返す() {
        assertThat(match().hasJobId()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが null の場合に false を返すことを確かめる。 案件の無いマッチングを案件と結び付けない。
       *
       * @given 案件IDが null
       * @when hasJobId を実行する
       * @then falseを返す
       */
      @Test
      void 案件IDがnullの場合_falseを返す() {
        SES_AI_T_MATCH match = match();
        match.setJobId(null);

        assertThat(match.hasJobId()).isFalse();
      }

      /**
       * 案件IDが空白の場合に false を返すことを確かめる。 空白の案件IDは有効な案件を指さない。
       *
       * @given 案件IDが空白
       * @when hasJobId を実行する
       * @then falseを返す
       */
      @Test
      void 案件IDが空白の場合_falseを返す() {
        SES_AI_T_MATCH match = match();
        match.setJobId("  ");

        assertThat(match.hasJobId()).isFalse();
      }
    }
  }

  @Nested
  class hasPersonId {

    @Nested
    class 正常系 {

      /**
       * 要員IDがある場合に true を返すことを確かめる。 要員と結び付くマッチングを判定する。
       *
       * @given 要員IDが "P001"
       * @when hasPersonId を実行する
       * @then trueを返す
       */
      @Test
      void 要員IDがある場合_trueを返す() {
        assertThat(match().hasPersonId()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に false を返すことを確かめる。 要員の無いマッチングを要員と結び付けない。
       *
       * @given 要員IDが null
       * @when hasPersonId を実行する
       * @then falseを返す
       */
      @Test
      void 要員IDがnullの場合_falseを返す() {
        SES_AI_T_MATCH match = match();
        match.setPersonId(null);

        assertThat(match.hasPersonId()).isFalse();
      }

      /**
       * 要員IDが文字列の "null" の場合に false を返すことを確かめる。 画面から未選択の値が文字列として届くことがある。
       *
       * @given 要員IDが "null" の文字列
       * @when hasPersonId を実行する
       * @then falseを返す
       */
      @Test
      void 要員IDが文字列nullの場合_falseを返す() {
        SES_AI_T_MATCH match = match();
        match.setPersonId("null");

        assertThat(match.hasPersonId()).isFalse();
      }
    }
  }

  @Nested
  class setProfit {

    @Nested
    class 正常系 {

      /**
       * 案件単価から要員単価を引いた粗利益を設定することを確かめる。 マッチングの利益を単価の差で算出する。
       *
       * @given 案件単価が 1500000 円、要員単価が 500000 円
       * @when setProfit を両方の単価で実行する
       * @then 利益を 1000000 円に更新する
       */
      @Test
      void 両方の単価が有効な場合_差額の利益を更新する() {
        SES_AI_T_MATCH match = match();

        match.setProfit(new Money(1_500_000L), new Money(500_000L));

        assertThat(match.getProfit().getValue()).isEqualByComparingTo(new BigDecimal(1_000_000));
      }
    }

    @Nested
    class 異常系 {

      /**
       * 案件単価が null の場合に利益を null にすることを確かめる。 単価が無い状態で利益を計算すると誤った値になる。
       *
       * @given 案件単価が null、要員単価が 500000 円
       * @when setProfit を実行する
       * @then 利益をnullに更新する
       */
      @Test
      void 案件単価がnullの場合_利益としてnullを更新する() {
        SES_AI_T_MATCH match = match();

        match.setProfit(null, new Money(500_000L));

        assertThat(match.getProfit()).isNull();
      }

      /**
       * 案件単価が空の場合に利益を null にすることを確かめる。 単価の未設定を0円として扱うと、利益が過大になる。
       *
       * @given 案件単価が空の単価、要員単価が 500000 円
       * @when setProfit を実行する
       * @then 利益をnullに更新する
       */
      @Test
      void 案件単価が空の場合_利益としてnullを更新する() {
        SES_AI_T_MATCH match = match();

        match.setProfit(Money.empty(), new Money(500_000L));

        assertThat(match.getProfit()).isNull();
      }

      /**
       * 案件単価が単価不問の場合に利益を null にすることを確かめる。 応相談の単価では利益を算出できない。
       *
       * @given 案件単価が単価不問、要員単価が 500000 円
       * @when setProfit を実行する
       * @then 利益をnullに更新する
       */
      @Test
      void 案件単価が単価不問の場合_利益としてnullを更新する() {
        SES_AI_T_MATCH match = match();

        match.setProfit(new Money(9_990_000L), new Money(500_000L));

        assertThat(match.getProfit()).isNull();
      }

      /**
       * 要員単価が null の場合に利益を null にすることを確かめる。 単価が無い状態で利益を計算すると誤った値になる。
       *
       * @given 案件単価が 1500000 円、要員単価が null
       * @when setProfit を実行する
       * @then 利益をnullに更新する
       */
      @Test
      void 要員単価がnullの場合_利益としてnullを更新する() {
        SES_AI_T_MATCH match = match();

        match.setProfit(new Money(1_500_000L), null);

        assertThat(match.getProfit()).isNull();
      }

      /**
       * 要員単価が空の場合に利益を null にすることを確かめる。 単価の未設定を0円として扱うと、利益が過大になる。
       *
       * @given 案件単価が 1500000 円、要員単価が空の単価
       * @when setProfit を実行する
       * @then 利益をnullに更新する
       */
      @Test
      void 要員単価が空の場合_利益としてnullを更新する() {
        SES_AI_T_MATCH match = match();

        match.setProfit(new Money(1_500_000L), Money.empty());

        assertThat(match.getProfit()).isNull();
      }

      /**
       * 要員単価が単価不問の場合に利益を null にすることを確かめる。 応相談の単価では利益を算出できない。
       *
       * @given 案件単価が 1500000 円、要員単価が単価不問
       * @when setProfit を実行する
       * @then 利益をnullに更新する
       */
      @Test
      void 要員単価が単価不問の場合_利益としてnullを更新する() {
        SES_AI_T_MATCH match = match();

        match.setProfit(new Money(1_500_000L), new Money(9_990_000L));

        assertThat(match.getProfit()).isNull();
      }
    }
  }

  @Nested
  class getSkillEvaluateText {

    @Nested
    class 正常系 {

      /**
       * 必須と尚可の評価を改行で連結した文章を返すことを確かめる。 画面でスキル要件の評価を一続きに表示する。
       *
       * @given 必須スキル評価文が "必須"、尚可スキル評価文が "尚可"
       * @when getSkillEvaluateText を実行する
       * @then "必須" と "尚可" を改行で連結した文章を返す
       */
      @Test
      void 尚可の評価がある場合_改行で連結した文章を返す() {
        assertThat(match().getSkillEvaluateText()).isEqualTo("必須\n尚可");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 尚可の評価が null の場合に必須の評価だけを返すことを確かめる。 尚可が無いと改行だけが末尾に残る。
       *
       * @given 必須スキル評価文が "必須"、尚可スキル評価文が null
       * @when getSkillEvaluateText を実行する
       * @then "必須" だけの文章を返す
       */
      @Test
      void 尚可の評価がnullの場合_必須の評価だけの文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setWantEvaluationText(null);

        assertThat(match.getSkillEvaluateText()).isEqualTo("必須");
      }

      /**
       * 尚可の評価が空文字の場合に必須の評価だけを返すことを確かめる。 空の尚可を改行で連結すると、末尾に空行が残る。
       *
       * @given 必須スキル評価文が "必須"、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then "必須" だけの文章を返す
       */
      @Test
      void 尚可の評価が空文字の場合_必須の評価だけの文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("必須");
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * マッチングIDを発行し、項目を列の順に設定してテナントIDを加えて登録することを確かめる。 マッチングIDはテナントの中で一意にする。
       *
       * @given 項目が揃ったマッチング、発行されるUUIDが固定値、現在時刻が 2026-01-15 12:00 の日本時間、更新件数が 1 件
       * @when insert を接続で実行する
       * @then マッチングIDに "0123456789" を発行し、tenant_id を含めて1件を返す
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @MockDbConnection
      @Test
      void 項目が揃っている場合_発行したマッチングIDで1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_MATCH match = match();
        UUID fixed = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");

        try (MockedStatic<UUID> uuid = mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(fixed);
          assertThat(match.insert(connection)).isEqualTo(1);
        }

        assertThat(match.getMatchingId()).isEqualTo(MATCHING_ID);
        verify(statement).setString(1, MATCHING_ID);
        verify(statement).setString(7, "20");
        verify(statement).setObject(9, 80);
        verify(statement).setObject(15, new BigDecimal(1_000_000));
        verify(statement).setTimestamp(16, Timestamp.valueOf("2026-01-15 12:00:00"));
        verify(statement).setString(18, TENANT_ID);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ステータス、点数、利益が未設定の場合に、各項目の位置に null を設定して登録することを確かめる。 未設定の項目があっても登録を失敗させない。
       *
       * @given ステータスが null、点数が null、利益が null、更新件数が 1 件
       * @when insert を接続で実行する
       * @then ステータス、点数、利益の位置に null を設定し、1件を返す
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @MockDbConnection
      @Test
      void ステータスと点数と利益が未設定の場合_nullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_MATCH match = matchWithoutOptionalFields();

        assertThat(match.insert(connection)).isEqualTo(1);

        verify(statement).setString(7, null);
        verify(statement).setObject(9, null);
        verify(statement).setObject(15, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * マッチングIDとテナントIDが一致する行の項目を読み込むことを確かめる。 保存済みのマッチングを画面に表示する。
       *
       * @given マッチングIDが "0123456789"、SELECT の結果が 1 行ステータス "20"、点数 80、利益 1000000
       * @when selectByPk を接続で実行する
       * @then ステータスを進行中、点数を 80、利益を 1000000 円に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_項目を更新する(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(
                    MATCHING_ID,
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
                    "場所",
                    "出社",
                    "その他",
                    new BigDecimal(1_000_000),
                    REGISTER_DATE_TEXT,
                    "U000")
                .build());
        SES_AI_T_MATCH match = new SES_AI_T_MATCH(TENANT_ID);
        match.setMatchingId(MATCHING_ID);

        match.selectByPk(connection);

        assertThat(match.getStatus()).isEqualTo(MatchingStatus.進行中);
        assertThat(match.getScore()).isEqualTo(80);
        assertThat(match.getProfit().getValue()).isEqualByComparingTo(new BigDecimal(1_000_000));
      }

      /**
       * 該当行が無い場合に項目を変えないことを確かめる。 存在しないマッチングを読み込んでも、入力中の値を壊さない。
       *
       * @given マッチングIDが "0123456789"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then 利益を変えずに何もしない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_何もしない(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_MATCH match = match();

        match.selectByPk(connection);

        assertThat(match.getProfit().getValue()).isEqualByComparingTo(new BigDecimal(1_000_000));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 点数と利益が未設定の行を読み込んだ場合に、点数を null、利益を空の値に更新することを確かめる。 未設定の行を読み込んでも例外にしない。
       *
       * @given マッチングIDが "0123456789"、SELECT の結果が 1 行点数が null、利益が null
       * @when selectByPk を接続で実行する
       * @then 点数をnullに、利益を空の値に更新する
       */
      @MockDbConnection
      @Test
      void 点数と利益が未設定の行の場合_点数としてnullと利益として空の値を更新する(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(
                    MATCHING_ID,
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
                    "場所",
                    "出社",
                    "その他",
                    null,
                    REGISTER_DATE_TEXT,
                    "U000")
                .build());
        SES_AI_T_MATCH match = match();

        match.selectByPk(connection);

        assertThat(match.getScore()).isNull();
        assertThat(match.getProfit().isEmpty()).isTrue();
      }

      /**
       * マッチングIDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返すおそれがある。
       *
       * @given マッチングIDが null
       * @when selectByPk を接続で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void マッチングIDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_MATCH(TENANT_ID).selectByPk(connection);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * マッチングIDとテナントIDが一致する行を、項目の値で更新することを確かめる。 マッチングの進捗や評価を保存する。
       *
       * @given マッチングIDが "0123456789"、ステータスが 進行中、点数が 80、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then ステータスのコードと点数を設定し、tenant_id を条件に含めて更新し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(match().updateByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_T_MATCH SET user_id = ?, job_id = ?, person_id = ?, job_content = ?,"
                    + " person_content = ?, status_cd = ?, evaluation_text = ?, score = ?,"
                    + " must_evaluation_text = ?, want_evaluation_text = ?, place_evaluation_text"
                    + " = ?, office_evaluation_text = ?, other_evaluation_text = ?, profit = ?,"
                    + " register_date = ?, register_user = ? WHERE matching_id = ? AND tenant_id"
                    + " = ?");
        verify(statement).setString(6, "20");
        verify(statement).setObject(8, 80);
        verify(statement).setString(17, MATCHING_ID);
      }

      /**
       * ステータス、点数、利益、登録日時が未設定の場合に、各項目の位置に null を設定して更新することを確かめる。 未設定の項目があっても更新を失敗させない。
       *
       * @given ステータスが null、点数が null、利益が null、登録日時が未設定、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then ステータス、点数、利益、登録日時の位置に null を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 未設定の項目がある場合_nullを設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(matchWithoutOptionalFields().updateByPk(connection)).isTrue();

        verify(statement).setString(6, null);
        verify(statement).setObject(8, null);
        verify(statement).setObject(14, null);
        verify(statement).setTimestamp(15, null);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象のマッチングが無いときに更新成功と誤認しない。
       *
       * @given マッチングIDが "0123456789"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(match().updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * マッチングIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全件を書き換えるおそれがある。
       *
       * @given マッチングIDが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void マッチングIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_MATCH(TENANT_ID).updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 更新の条件にテナントIDを含め、他テナントのマッチングを更新しないことを確かめる。 他テナントの案件との結び付きを書き換えると、業務の情報が混ざる。
       *
       * @given テナントIDが "T001"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 条件の18番目に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(match().updateByPk(connection)).isTrue();

        verify(statement).setString(18, TENANT_ID);
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * マッチングIDとテナントIDが一致する行を削除することを確かめる。 不要になったマッチングを残さない。
       *
       * @given マッチングIDが "0123456789"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then tenant_id を条件に含めて削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(match().deleteByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement("DELETE FROM SES_AI_T_MATCH WHERE matching_id = ? AND tenant_id = ?");
        verify(statement).setString(1, MATCHING_ID);
        verify(statement).setString(2, TENANT_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象のマッチングが無いときに削除成功と誤認しない。
       *
       * @given マッチングIDが "0123456789"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(match().deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * マッチングIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全件を消すおそれがある。
       *
       * @given マッチングIDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void マッチングIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_MATCH(TENANT_ID).deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }
}
