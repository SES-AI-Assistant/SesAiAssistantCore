package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.api.gpt.entity.FilteringRules;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** {@link SES_AI_M_TENANT} の単体テスト。 */
class SES_AI_M_TENANTTest {

  /** テナントID。 テナント情報マスタの主キーであり、テナント条件を付けない。 */
  private static final String TENANT_ID = "T001";

  /** 登録日時の文字列。 */
  private static final String REGISTER_DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列。 マッチング強度の列を含む。 */
  private static final String[] COLUMNS = {
    "tenant_id",
    "tenant_name",
    "tenant_status_cd",
    "match_must_level",
    "match_want_level",
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

  @Nested
  class コンストラクタ {

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナント情報は主キーのテナントIDで識別する。
       *
       * @given テナントIDが null
       * @when SES_AI_M_TENANT を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_M_TENANT(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 新規のテナントのマッチング強度が DDL の既定値と同じになることを確かめる。 新規登録で既存テナントと同じ既定の絞り込みを保つため、既定値を固定する。
       *
       * @given テナントIDが "T001" だけを指定して生成する
       * @when マッチング強度の必須と尚可を取得する
       * @then 必須に FullyMet、尚可に NotMet を返す
       */
      @Test
      void テナントIDだけで生成した場合_必須にFullyMetと尚可にNotMetを返す() {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);

        assertThat(tenant.getMatchMustLevel()).isEqualTo(EvaluateType.FullyMet);
        assertThat(tenant.getMatchWantLevel()).isEqualTo(EvaluateType.NotMet);
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * テナント情報を登録するSQLにマッチング強度の列を含めて実行することを確かめる。 テナント情報の列が欠けると登録できないため、列の並びを固定する。
       *
       * @given テナントIDが "T001"、テナント名が "テナントA"、ステータス区分が "1"、登録日時が "2026-01-15 10:00:00"、登録ユーザーが
       *     "U001"
       * @when insert を接続で実行する
       * @then 7個の列を含むINSERT文を準備し、テナント情報を設定して1件を返す
       */
      @MockDbConnection
      @Test
      void テナント情報が揃っている場合_1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantName("テナントA");
        tenant.setTenantStatusCd("1");
        tenant.setRegisterDate(new OriginalDateTime(REGISTER_DATE_TEXT));
        tenant.setRegisterUser("U001");

        assertThat(tenant.insert(connection)).isEqualTo(1);

        verify(connection)
            .prepareStatement(
                "INSERT INTO SES_AI_M_TENANT (tenant_id, tenant_name, tenant_status_cd,"
                    + " match_must_level, match_want_level, register_date, register_user)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)");
        verify(statement).setString(1, TENANT_ID);
        verify(statement).setString(2, "テナントA");
        verify(statement).setString(3, "1");
        verify(statement).setTimestamp(6, Timestamp.valueOf(REGISTER_DATE_TEXT));
        verify(statement).setString(7, "U001");
      }

      /**
       * マッチング強度を指定して登録すると、指定した列挙名の文字列で保存することを確かめる。 テナントごとの絞り込みの強さを登録時に保存する。
       *
       * @given テナントIDが "T001"、必須が PartiallyMet、尚可が FullyMet、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 必須に "PartiallyMet"、尚可に "FullyMet" を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void マッチング強度が指定されている場合_必須PartiallyMetと尚可FullyMetを設定して1件を返す(final Connection connection)
          throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(EvaluateType.PartiallyMet);
        tenant.setMatchWantLevel(EvaluateType.FullyMet);

        assertThat(tenant.insert(connection)).isEqualTo(1);

        verify(statement).setString(4, "PartiallyMet");
        verify(statement).setString(5, "FullyMet");
      }

      /**
       * マッチング強度を指定せず生成したテナントを、既定値で登録することを確かめる。 新規テナントの絞り込みを既存テナントと同じ既定値にそろえる。
       *
       * @given テナントIDが "T001" だけを指定して生成され、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 必須に FullyMet、尚可に NotMet を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void マッチング強度が既定値の場合_必須FullyMetと尚可NotMetを設定して1件を返す(final Connection connection)
          throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).insert(connection)).isEqualTo(1);

        verify(statement).setString(4, "FullyMet");
        verify(statement).setString(5, "NotMet");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 登録日時が未設定の場合に日時にnullを設定することを確かめる。 登録日時は必須の列ではないため、未設定でも登録できる必要がある。
       *
       * @given テナントIDが "T001"、登録日時が未設定、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 登録日時の位置に null を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void 登録日時が未設定の場合_登録日時にnullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).insert(connection)).isEqualTo(1);

        verify(statement).setTimestamp(6, null);
      }

      /**
       * マッチング強度が未設定の場合に、両方の列にnullを設定することを確かめる。 null は制限なしとして保存するため、値を書き込まないことを守る。
       *
       * @given テナントIDが "T001"、必須と尚可が null、更新件数が 1 件
       * @when insert を接続で実行する
       * @then マッチング強度の位置に null を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void マッチング強度が未設定の場合_NULLを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(null);
        tenant.setMatchWantLevel(null);

        assertThat(tenant.insert(connection)).isEqualTo(1);

        verify(statement).setString(4, null);
        verify(statement).setString(5, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * テナントIDが一致する行の項目とマッチング強度を読み込むことを確かめる。 画面で編集する前に、保存済みのテナント情報とマッチング強度を読み込む必要がある。
       *
       * @given テナントIDが "T001"、SELECT の結果が 1 行、テナント名 "テナントA"、ステータス区分 "1"、必須 PartiallyMet、尚可 NotMet
       * @when selectByPk を接続で実行する
       * @then trueを返し、テナント名、ステータス区分、必須と尚可のマッチング強度を読み込む
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(TENANT_ID, "テナントA", "1", "PartiallyMet", "NotMet", REGISTER_DATE_TEXT, "U001")
                .build());
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);

        assertThat(tenant.selectByPk(connection)).isTrue();

        assertThat(tenant.getTenantName()).isEqualTo("テナントA");
        assertThat(tenant.getTenantStatusCd()).isEqualTo("1");
        assertThat(tenant.getMatchMustLevel()).isEqualTo(EvaluateType.PartiallyMet);
        assertThat(tenant.getMatchWantLevel()).isEqualTo(EvaluateType.NotMet);
      }

      /**
       * 該当行が無い場合に false を返し、項目を変えないことを確かめる。 存在しないテナントを読み込んでも、入力中の値を壊さない。
       *
       * @given テナントIDが "T001"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then falseを返し、テナント名を変えない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);

        assertThat(tenant.selectByPk(connection)).isFalse();

        assertThat(tenant.getTenantName()).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * マッチング強度の列が NULL の行を読み込む場合に、強度を null として読み込むことを確かめる。 NULL は制限なしとして扱うため、列挙値に変換せずに読み込む。
       *
       * @given テナントIDが "T001"、SELECT の結果が 1 行で必須と尚可の列が NULL
       * @when selectByPk を接続で実行する
       * @then trueを返し、必須と尚可のマッチング強度を null のまま読み込む
       */
      @MockDbConnection
      @Test
      void マッチング強度が未設定の行の場合_強度をnullで読み込んでtrueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(TENANT_ID, "テナントA", "1", null, null, REGISTER_DATE_TEXT, "U001")
                .build());
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);

        assertThat(tenant.selectByPk(connection)).isTrue();

        assertThat(tenant.getMatchMustLevel()).isNull();
        assertThat(tenant.getMatchWantLevel()).isNull();
      }

      /**
       * テナントIDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返す恐れがある。
       *
       * @given テナントIDが null
       * @when selectByPk を接続で実行する
       * @then falseを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_DBを呼ばずfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantId(null);

        assertThat(tenant.selectByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * テナントIDが一致する行のテナント名、ステータス区分、マッチング強度を更新することを確かめる。 テナント情報とテナントごとの絞り込みの強さの変更を保存する。
       *
       * @given テナントIDが "T001"、テナント名が "テナントB"、ステータス区分が "2"、必須が PartiallyMet、尚可が FullyMet、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then テナント名、ステータス区分、必須、尚可、テナントIDの順に設定して更新し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantName("テナントB");
        tenant.setTenantStatusCd("2");
        tenant.setMatchMustLevel(EvaluateType.PartiallyMet);
        tenant.setMatchWantLevel(EvaluateType.FullyMet);

        assertThat(tenant.updateByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_M_TENANT SET tenant_name = ?, tenant_status_cd = ?,"
                    + " match_must_level = ?, match_want_level = ? WHERE tenant_id = ?");
        verify(statement).setString(1, "テナントB");
        verify(statement).setString(2, "2");
        verify(statement).setString(3, "PartiallyMet");
        verify(statement).setString(4, "FullyMet");
        verify(statement).setString(5, TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象のテナントが無いときに更新成功と誤認しない。
       *
       * @given テナントIDが "T001"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * マッチング強度が未設定の場合に、両方の列へnullを設定して更新することを確かめる。 null は制限なしとして保存するため、既存の強度を残さず書き換えることを守る。
       *
       * @given テナントIDが "T001"、必須と尚可が null、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then マッチング強度の位置に null を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void マッチング強度が未設定の場合_NULLを設定して更新しtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(null);
        tenant.setMatchWantLevel(null);

        assertThat(tenant.updateByPk(connection)).isTrue();

        verify(statement).setString(3, null);
        verify(statement).setString(4, null);
      }

      /**
       * テナントIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全テナントを書き換える恐れがある。
       *
       * @given テナントIDが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantId(null);

        assertThat(tenant.updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * テナントIDが一致する行を削除することを確かめる。 不要になったテナント情報を残さない。
       *
       * @given テナントIDが "T001"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then テナントIDを条件にして削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).deleteByPk(connection)).isTrue();

        verify(connection).prepareStatement("DELETE FROM SES_AI_M_TENANT WHERE tenant_id = ?");
        verify(statement).setString(1, TENANT_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象のテナントが無いときに削除成功と誤認しない。
       *
       * @given テナントIDが "T001"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(new SES_AI_M_TENANT(TENANT_ID).deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * テナントIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全テナントを消す恐れがある。
       *
       * @given テナントIDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setTenantId(null);

        assertThat(tenant.deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class toFilteringRules {

    @Nested
    class 正常系 {

      /**
       * 必須と尚可が FullyMet か PartiallyMet の場合に、その強度をしきい値にすることを確かめる。 画面で選べる強度を、そのまま AI の絞り込みに渡す。
       *
       * @given 必須が CsvSource の1列目、尚可が2列目の強度で、3列目と4列目が期待するしきい値
       * @when toFilteringRules を呼び出す
       * @then 必須と尚可のしきい値に指定した強度を設定し、その他制約を必須にしたルールを返す
       */
      @ParameterizedTest
      @CsvSource({
        "FullyMet, FullyMet, FullyMet, FullyMet",
        "FullyMet, PartiallyMet, FullyMet, PartiallyMet",
        "PartiallyMet, FullyMet, PartiallyMet, FullyMet",
        "PartiallyMet, PartiallyMet, PartiallyMet, PartiallyMet"
      })
      void 必須と尚可がFullyMetかPartiallyMetの場合_指定した強度をしきい値にしたルールを返す(
          final EvaluateType must,
          final EvaluateType want,
          final EvaluateType expectedMust,
          final EvaluateType expectedWant) {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(must);
        tenant.setMatchWantLevel(want);

        FilteringRules rules = tenant.toFilteringRules();

        assertThat(rules.getMustSkillLevel()).isEqualTo(expectedMust);
        assertThat(rules.getWantSkillLevel()).isEqualTo(expectedWant);
        assertThat(rules.isOtherConstraintsRequired()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 尚可が NotMet の場合に、尚可のしきい値を null にすることを確かめる。 NotMet は制限なしを表すため、尚可の判定を外すことを守る。
       *
       * @given 必須が CsvSource の1列目、尚可が NotMet で、2列目が期待する必須のしきい値
       * @when toFilteringRules を呼び出す
       * @then 必須のしきい値に指定した強度を設定し、尚可のしきい値を null にしたルールを返す
       */
      @ParameterizedTest
      @CsvSource({"FullyMet, FullyMet", "PartiallyMet, PartiallyMet"})
      void 尚可がNotMetの場合_尚可のしきい値をnullにしたルールを返す(
          final EvaluateType must, final EvaluateType expectedMust) {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(must);
        tenant.setMatchWantLevel(EvaluateType.NotMet);

        FilteringRules rules = tenant.toFilteringRules();

        assertThat(rules.getMustSkillLevel()).isEqualTo(expectedMust);
        assertThat(rules.getWantSkillLevel()).isNull();
      }

      /**
       * 必須が NotMet の場合に、必須のしきい値を null にすることを確かめる。 NotMet は制限なしを表すため、必須の判定を外すことを守る。
       *
       * @given 必須が NotMet、尚可が CsvSource の2列目の強度で、2列目が期待する尚可のしきい値
       * @when toFilteringRules を呼び出す
       * @then 必須のしきい値を null にし、尚可のしきい値に指定した強度を設定したルールを返す
       */
      @ParameterizedTest
      @CsvSource({"FullyMet, FullyMet", "PartiallyMet, PartiallyMet"})
      void 必須がNotMetの場合_必須のしきい値をnullにしたルールを返す(
          final EvaluateType want, final EvaluateType expectedWant) {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(EvaluateType.NotMet);
        tenant.setMatchWantLevel(want);

        FilteringRules rules = tenant.toFilteringRules();

        assertThat(rules.getMustSkillLevel()).isNull();
        assertThat(rules.getWantSkillLevel()).isEqualTo(expectedWant);
      }

      /**
       * 必須と尚可が NotMet の場合に、両方のしきい値を null にすることを確かめる。 両方が制限なしの設定で、どちらの判定も外すことを守る。
       *
       * @given 必須と尚可が NotMet
       * @when toFilteringRules を呼び出す
       * @then 必須と尚可のしきい値を null にしたルールを返す
       */
      @Test
      void 必須と尚可がNotMetの場合_両方のしきい値をnullにしたルールを返す() {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(EvaluateType.NotMet);
        tenant.setMatchWantLevel(EvaluateType.NotMet);

        FilteringRules rules = tenant.toFilteringRules();

        assertThat(rules.getMustSkillLevel()).isNull();
        assertThat(rules.getWantSkillLevel()).isNull();
      }

      /**
       * 必須と尚可が未設定の場合に、両方のしきい値を null にすることを確かめる。 未設定は制限なしとして扱うため、判定を外すことを守る。
       *
       * @given 必須と尚可が null
       * @when toFilteringRules を呼び出す
       * @then 必須と尚可のしきい値を null にしたルールを返す
       */
      @Test
      void 必須と尚可が未設定の場合_両方のしきい値をnullにしたルールを返す() {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(null);
        tenant.setMatchWantLevel(null);

        FilteringRules rules = tenant.toFilteringRules();

        assertThat(rules.getMustSkillLevel()).isNull();
        assertThat(rules.getWantSkillLevel()).isNull();
      }

      /**
       * 必須と尚可が Unknown の場合に、両方のしきい値を null にすることを確かめる。 Unknown
       * をしきい値にすると判定の全件が除外されるため、制限なしとして扱うことを守る。
       *
       * @given 必須と尚可が Unknown
       * @when toFilteringRules を呼び出す
       * @then 必須と尚可のしきい値を null にしたルールを返す
       */
      @Test
      void 必須と尚可がUnknownの場合_両方のしきい値をnullにしたルールを返す() {
        SES_AI_M_TENANT tenant = new SES_AI_M_TENANT(TENANT_ID);
        tenant.setMatchMustLevel(EvaluateType.Unknown);
        tenant.setMatchWantLevel(EvaluateType.Unknown);

        FilteringRules rules = tenant.toFilteringRules();

        assertThat(rules.getMustSkillLevel()).isNull();
        assertThat(rules.getWantSkillLevel()).isNull();
      }
    }
  }
}
