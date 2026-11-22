package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    "other_evaluation_text",
    "profit",
    "register_date",
    "register_user"
  };

  /** テナントIDを含む取得結果の列。 */
  private static final String[] COLUMNS_WITH_TENANT_ID = {
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
    "tenant_id"
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

      /**
       * コメントが {@code (null)} の評価から括弧を取り除いて連結することを確かめる。 旧実装が保存した {@code (null)} を画面に出さない仕様である。
       *
       * @given 必須スキル評価文が {@code ◎(null)}、尚可スキル評価文が {@code 〇(良い)}
       * @when getSkillEvaluateText を実行する
       * @then 必須は記号だけ、尚可はそのままの文章を改行で連結して返す
       */
      @Test
      void 必須のコメントがnullの括弧の場合_括弧を取り除いて連結した文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("◎(null)");
        match.setWantEvaluationText("〇(良い)");

        assertThat(match.getSkillEvaluateText()).isEqualTo("◎\n〇(良い)");
      }

      /**
       * 尚可のコメントが {@code (null)} の評価から括弧を取り除くことを確かめる。 必須と尚可のどちらにも同じ除去を適用する仕様である。
       *
       * @given 必須スキル評価文が {@code ◎(良い)}、尚可スキル評価文が {@code -(null)}
       * @when getSkillEvaluateText を実行する
       * @then 必須はそのまま、尚可は記号だけの文章を改行で連結して返す
       */
      @Test
      void 尚可のコメントがnullの括弧の場合_括弧を取り除いて連結した文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("◎(良い)");
        match.setWantEvaluationText("-(null)");

        assertThat(match.getSkillEvaluateText()).isEqualTo("◎(良い)\n-");
      }

      /**
       * コメントが空の括弧の評価から括弧を取り除いて連結することを確かめる。 旧実装が保存した空の括弧を画面に出さない仕様である。
       *
       * @given 必須スキル評価文が {@code -()}、尚可スキル評価文が {@code ◎()}
       * @when getSkillEvaluateText を実行する
       * @then 記号だけを改行で連結した文章を返す
       */
      @Test
      void コメントが空の括弧の場合_括弧を取り除いて連結した文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("-()");
        match.setWantEvaluationText("◎()");

        assertThat(match.getSkillEvaluateText()).isEqualTo("-\n◎");
      }

      /**
       * コメントの本文に {@code (null)} を含む場合に、その部分だけを取り除くことを確かめる。 コメントのほかの文字を変えない仕様である。
       *
       * @given 必須スキル評価文が {@code ◎(abc(null))}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then {@code (null)} の部分だけを取り除いた必須の文章を返す
       */
      @Test
      void コメントの本文にnullの括弧を含む場合_その部分だけを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("◎(abc(null))");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("◎(abc)");
      }

      /**
       * 評価文を読んでも、保存する値のゲッターは変わらないことを確かめる。 DB に保存した値を書き換えない仕様である。
       *
       * @given 必須スキル評価文が {@code ◎(null)}、尚可スキル評価文が {@code 〇()}
       * @when getSkillEvaluateText を実行する
       * @then 必須と尚可の評価文のゲッターは元の文字列を返す
       */
      @Test
      void 評価文を読んだ場合_保存した評価文のゲッターは元の文字列を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("◎(null)");
        match.setWantEvaluationText("〇()");

        match.getSkillEvaluateText();

        assertThat(match.getMustEvaluationText()).isEqualTo("◎(null)");
        assertThat(match.getWantEvaluationText()).isEqualTo("〇()");
      }

      /**
       * 未充足の項目のコメントを取り除くことを確かめる。 提案ダイアログの評価文は、未充足の項目にコメントを付けない。
       *
       * @given 必須スキル評価文が {@code ■必須\n・時系列データ分析：×(根拠B)\n}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then コメントの括弧を取り除いた {@code ■必須\n・時系列データ分析：×} を返す
       */
      @Test
      void 必須の項目が未充足でコメントがある場合_コメントを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("■必須\n・時系列データ分析：×(根拠B)\n");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("■必須\n・時系列データ分析：×");
      }

      /**
       * 評価値が不明の項目のコメントを取り除くことを確かめる。 保存した文字列では評価値が不明の項目と評価値が無い項目を区別できないので、同じ扱いにする。
       *
       * @given 必須スキル評価文が空文字、尚可スキル評価文が {@code ■尚可\n・Java：-(理由)\n}
       * @when getSkillEvaluateText を実行する
       * @then コメントの括弧を取り除いた {@code ■尚可\n・Java：-} を返す
       */
      @Test
      void 尚可の項目が不明でコメントがある場合_コメントを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("");
        match.setWantEvaluationText("■尚可\n・Java：-(理由)\n");

        assertThat(match.getSkillEvaluateText()).isEqualTo("■尚可\n・Java：-");
      }

      /**
       * 充足と一部充足の項目のコメントを残すことを確かめる。 提案ダイアログの評価文は、未充足以外の項目のコメントを表示する。
       *
       * @given 必須スキル評価文が {@code ■必須\n・Java：◎(根拠A)\n・Spring：〇(類似)\n}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 末尾の改行だけを除いた {@code ■必須\n・Java：◎(根拠A)\n・Spring：〇(類似)} を返す
       */
      @Test
      void 必須の項目が充足と一部充足の場合_コメントを残した文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("■必須\n・Java：◎(根拠A)\n・Spring：〇(類似)\n");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("■必須\n・Java：◎(根拠A)\n・Spring：〇(類似)");
      }

      /**
       * 必須と尚可の見出しと並び順を変えずに、未充足と不明の項目のコメントだけを取り除くことを確かめる。 提案文は必須の次に尚可を並べる仕様である。
       *
       * @given 必須スキル評価文が {@code ■必須\n・A：×(a)\n・B：◎(b)\n}、尚可スキル評価文が {@code ■尚可\n・C：-(c)\n}
       * @when getSkillEvaluateText を実行する
       * @then 必須と尚可の見出しを残し、未充足と不明のコメントだけを取り除いた文章を返す
       */
      @Test
      void 必須と尚可に未充足と不明の項目がある場合_見出しを残してコメントを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("■必須\n・A：×(a)\n・B：◎(b)\n");
        match.setWantEvaluationText("■尚可\n・C：-(c)\n");

        assertThat(match.getSkillEvaluateText()).isEqualTo("■必須\n・A：×\n・B：◎(b)\n\n■尚可\n・C：-");
      }
    }

    @Nested
    class 境界値 {

      /**
       * コメントが空白だけの括弧と大文字の括弧は、取り除かないことを確かめる。 旧実装が出した形だけを対象にする仕様である。
       *
       * @given 必須スキル評価文が空白だけの括弧を持つ {@code ◎( )}、尚可スキル評価文が大文字の {@code 〇(NULL)}
       * @when getSkillEvaluateText を実行する
       * @then どちらの括弧も残した文章を改行で連結して返す
       */
      @Test
      void コメントが空白だけの括弧と大文字の括弧の場合_括弧を残した文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("◎(  )");
        match.setWantEvaluationText("〇(NULL)");

        assertThat(match.getSkillEvaluateText()).isEqualTo("◎(  )\n〇(NULL)");
      }

      /**
       * 必須が括弧だけで取り除くと空になる場合に、尚可だけを返すことを確かめる。 先頭に改行だけの空行を画面に出さない仕様である。
       *
       * @given 必須スキル評価文が {@code (null)}、尚可スキル評価文が "尚可"
       * @when getSkillEvaluateText を実行する
       * @then 改行を含まない "尚可" だけの文章を返す
       */
      @Test
      void 必須が括弧だけで取り除くと空になる場合_尚可の評価だけの文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("(null)");
        match.setWantEvaluationText("尚可");

        assertThat(match.getSkillEvaluateText()).isEqualTo("尚可");
      }

      /**
       * 必須と尚可が括弧だけで取り除くとどちらも空になる場合に、空文字を返すことを確かめる。 改行だけの文章を画面に出さない仕様である。
       *
       * @given 必須スキル評価文が {@code ()}、尚可スキル評価文が {@code (null)}
       * @when getSkillEvaluateText を実行する
       * @then 空文字を返す
       */
      @Test
      void 必須と尚可が括弧だけで取り除くとどちらも空になる場合_空文字を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("()");
        match.setWantEvaluationText("(null)");

        assertThat(match.getSkillEvaluateText()).isEmpty();
      }

      /**
       * 必須の評価が null の場合に尚可の評価だけを返すことを確かめる。 文字列 "null" と先頭の改行を画面に出さない仕様である。
       *
       * @given 必須スキル評価文が null、尚可スキル評価文が "尚可"
       * @when getSkillEvaluateText を実行する
       * @then "尚可" だけの文章を返す
       */
      @Test
      void 必須の評価がnullの場合_尚可の評価だけの文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText(null);

        assertThat(match.getSkillEvaluateText()).isEqualTo("尚可");
      }

      /**
       * 必須の評価が空文字の場合に尚可の評価だけを返すことを確かめる。 空の必須を改行で連結すると、先頭に空行が残る。
       *
       * @given 必須スキル評価文が空文字、尚可スキル評価文が "尚可"
       * @when getSkillEvaluateText を実行する
       * @then "尚可" だけの文章を返す
       */
      @Test
      void 必須の評価が空文字の場合_尚可の評価だけの文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("尚可");
      }

      /**
       * 必須と尚可の評価がどちらも null の場合に空文字を返すことを確かめる。 評価文が無い行で "null" を画面に出さない仕様である。
       *
       * @given 必須スキル評価文と尚可スキル評価文がどちらも null
       * @when getSkillEvaluateText を実行する
       * @then 空文字を返す
       */
      @Test
      void 必須と尚可の評価がどちらもnullの場合_空文字を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText(null);
        match.setWantEvaluationText(null);

        assertThat(match.getSkillEvaluateText()).isEmpty();
      }

      /**
       * 必須と尚可の評価がどちらも空文字の場合に空文字を返すことを確かめる。 改行だけの文章を画面に出さない仕様である。
       *
       * @given 必須スキル評価文と尚可スキル評価文がどちらも空文字
       * @when getSkillEvaluateText を実行する
       * @then 空文字を返す
       */
      @Test
      void 必須と尚可の評価がどちらも空文字の場合_空文字を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEmpty();
      }

      /**
       * 必須の評価が null で尚可の評価が空文字の場合に空文字を返すことを確かめる。 どちらかが null でも空文字でも同じ扱いにする仕様である。
       *
       * @given 必須スキル評価文が null、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 空文字を返す
       */
      @Test
      void 必須の評価がnullで尚可の評価が空文字の場合_空文字を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText(null);
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEmpty();
      }

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

      /**
       * 項目名に括弧を含む未充足の項目で、項目名の括弧を残してコメントだけを取り除くことを確かめる。 項目名は AI の自由記述で、括弧を含む場合がある。
       *
       * @given 必須スキル評価文が {@code ■必須\n・Java(Spring Boot)：×(記載なし)\n}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 項目名の括弧を残した {@code ■必須\n・Java(Spring Boot)：×} を返す
       */
      @Test
      void 項目名に括弧を含む未充足の場合_項目名の括弧を残した文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("■必須\n・Java(Spring Boot)：×(記載なし)\n");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("■必須\n・Java(Spring Boot)：×");
      }

      /**
       * コメントの中に括弧を含む未充足の項目で、コメント全体を取り除くことを確かめる。 コメントの途中の閉じ括弧で止めると、残りの文字が画面に出る。
       *
       * @given 必須スキル評価文が {@code ・A：×(根拠(補足))}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then コメント全体を取り除いた {@code ・A：×} を返す
       */
      @Test
      void 未充足のコメントに括弧を含む場合_コメント全体を取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(根拠(補足))");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×");
      }

      /**
       * コメントの中に括弧を含む充足の項目で、コメントを変えないことを確かめる。 充足の項目のコメントは提案文の根拠として表示する。
       *
       * @given 必須スキル評価文が {@code ・A：◎(根拠(補足))}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 元の {@code ・A：◎(根拠(補足))} を返す
       */
      @Test
      void 充足のコメントに括弧を含む場合_元の文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：◎(根拠(補足))");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：◎(根拠(補足))");
      }

      /**
       * コメントに改行を含む未充足の項目で、次の項目を残してコメント全体を取り除くことを確かめる。 コメントの改行が残ると、次の項目の行が壊れる。
       *
       * @given 必須スキル評価文が {@code ・A：×(根拠1\n根拠2)\n・B：◎(良い)\n}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 次の項目を残した {@code ・A：×\n・B：◎(良い)} を返す
       */
      @Test
      void 未充足のコメントに改行を含む場合_次の項目を残してコメントを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(根拠1\n根拠2)\n・B：◎(良い)\n");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×\n・B：◎(良い)");
      }

      /**
       * 括弧が閉じていない未充足の項目で、次の項目を取り除かないことを確かめる。 閉じ括弧を探して次の項目まで消すと、充足の項目の評価が画面から無くなる。
       *
       * @given 必須スキル評価文が {@code ・A：×(根拠\n・B：◎(良い)\n}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 末尾の改行だけを除いた {@code ・A：×(根拠\n・B：◎(良い)} を返す
       */
      @Test
      void 未充足の括弧が閉じていない場合_次の項目を残した文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(根拠\n・B：◎(良い)\n");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×(根拠\n・B：◎(良い)");
      }

      /**
       * 充足の項目のコメントの中に未充足の記号と括弧が続く場合に、コメントを変えないことを確かめる。 項目名と記号は行頭からの形で判定し、コメントの本文は対象にしない。
       *
       * @given 必須スキル評価文が {@code ・A：◎(理由：×(補足))}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 元の {@code ・A：◎(理由：×(補足))} を返す
       */
      @Test
      void 充足のコメントに未充足の記号と括弧を含む場合_元の文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：◎(理由：×(補足))");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：◎(理由：×(補足))");
      }

      /**
       * コメントが無い未充足の項目を変えないことを確かめる。 括弧が無い行から括弧を探して、次の項目を取り除いてはならない。
       *
       * @given 必須スキル評価文が {@code ・A：×\n・B：◎(良い)}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 元の {@code ・A：×\n・B：◎(良い)} を返す
       */
      @Test
      void 未充足の項目にコメントが無い場合_元の文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×\n・B：◎(良い)");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×\n・B：◎(良い)");
      }

      /**
       * 評価文の末尾に空白と改行が続く場合に、取り除いて返すことを確かめる。 提案文のテンプレートが区切りの改行を足すので、評価文の末尾に空行を残さない。
       *
       * @given 必須スキル評価文が {@code ■必須\n・A：◎\n}、尚可スキル評価文が {@code ■尚可\n・B：〇\n\n} と半角スペース2つ
       * @when getSkillEvaluateText を実行する
       * @then 末尾の空白と改行だけを除いた {@code ■必須\n・A：◎\n\n■尚可\n・B：〇} を返す
       */
      @Test
      void 末尾に空白と改行が続く場合_末尾を取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("■必須\n・A：◎\n");
        match.setWantEvaluationText("■尚可\n・B：〇\n\n  ");

        assertThat(match.getSkillEvaluateText()).isEqualTo("■必須\n・A：◎\n\n■尚可\n・B：〇");
      }

      /**
       * 閉じ括弧の後ろに空白とタブが続く未充足の項目で、空白も含めてコメントを取り除くことを確かめる。 AI の出力は行末に空白が付く場合がある。
       *
       * @given 必須スキル評価文が {@code ・A：×(a) \n・B：×(b)　\n・C：×(c)\t\n・D：◎(y)}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 空白もコメントも取り除いた三つの項目と、そのままの充足の項目を返す
       */
      @Test
      void 未充足の閉じ括弧の後ろに空白が続く場合_空白とコメントを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(a) \n・B：×(b)　\n・C：×(c)\t\n・D：◎(y)");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×\n・B：×\n・C：×\n・D：◎(y)");
      }

      /**
       * 未充足の項目と次の項目の間に空行がある場合に、空行を変えずにコメントを取り除くことを確かめる。 空行は評価文の区切りで、コメントの一部ではない。
       *
       * @given 必須スキル評価文が {@code ・A：×(a)\n\n・B：◎(y)}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 空行を残した {@code ・A：×\n\n・B：◎(y)} を返す
       */
      @Test
      void 未充足の項目と次の項目の間に空行がある場合_空行を残してコメントを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(a)\n\n・B：◎(y)");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×\n\n・B：◎(y)");
      }

      /**
       * 閉じ括弧の後ろに改行が5000個続いて文字で終わる評価文で、例外を投げずに文章を変えないことを確かめる。 長い空行の判定でスタックを使い切ると、詳細画面が表示できない。
       *
       * @given 必須スキル評価文が {@code ・A：×(a)} の後ろに改行を5000個と "x" を続けた文字列、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 元の文字列を返す
       */
      @Test
      void 閉じ括弧の後ろに改行が5000個続く場合_元の文章を返す() {
        SES_AI_T_MATCH match = match();
        String must = "・A：×(a)" + "\n".repeat(5000) + "x";
        match.setMustEvaluationText(must);
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo(must);
      }

      /**
       * 閉じ括弧の後ろに文字が続く未充足の項目で、コメントも後ろの文字も変えないことを確かめる。 形式が保証されない入力から本文の情報を消さない。
       *
       * @given 必須スキル評価文が {@code ・A：×(a) 補足\n・B：◎(y)}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 元の {@code ・A：×(a) 補足\n・B：◎(y)} を返す
       */
      @Test
      void 未充足の閉じ括弧の後ろに文字が続く場合_元の文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(a) 補足\n・B：◎(y)");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×(a) 補足\n・B：◎(y)");
      }

      /**
       * コメントが閉じたあとに項目でも見出しでもない行が続く場合に、文章を変えないことを確かめる。 その行が項目の続きかを判断できないので、本文の情報を消さない。
       *
       * @given 必須スキル評価文が {@code ・A：×(a\nb)\nc\n・B：◎(y)}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 元の {@code ・A：×(a\nb)\nc\n・B：◎(y)} を返す
       */
      @Test
      void コメントのあとに項目でも見出しでもない行が続く場合_元の文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(a\nb)\nc\n・B：◎(y)");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×(a\nb)\nc\n・B：◎(y)");
      }

      /**
       * 改行が復帰文字だけの評価文で、次の項目を巻き込まずにコメントを取り除くことを確かめる。 復帰文字を境界にしないと、次の項目の評価が画面から消える。
       *
       * @given 必須スキル評価文が {@code ・A：×(a)\r・B：◎(y)}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 次の項目を残した {@code ・A：×\r・B：◎(y)} を返す
       */
      @Test
      void 改行が復帰文字だけの場合_次の項目を残してコメントを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(a)\r・B：◎(y)");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×\r・B：◎(y)");
      }

      /**
       * 改行が復帰文字と改行文字の組の評価文で、次の項目を残してコメントを取り除くことを確かめる。 Windows 形式の改行でも同じ表示にそろえる。
       *
       * @given 必須スキル評価文が {@code ・A：×(a)\r\n・B：◎(y)}、尚可スキル評価文が空文字
       * @when getSkillEvaluateText を実行する
       * @then 次の項目を残した {@code ・A：×\r\n・B：◎(y)} を返す
       */
      @Test
      void 改行が復帰文字と改行文字の組の場合_次の項目を残してコメントを取り除いた文章を返す() {
        SES_AI_T_MATCH match = match();
        match.setMustEvaluationText("・A：×(a)\r\n・B：◎(y)");
        match.setWantEvaluationText("");

        assertThat(match.getSkillEvaluateText()).isEqualTo("・A：×\r\n・B：◎(y)");
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
        verify(statement).setObject(13, new BigDecimal(1_000_000));
        verify(statement).setTimestamp(14, Timestamp.valueOf("2026-01-15 12:00:00"));
        verify(statement).setString(15, "U000");
        verify(statement).setString(16, TENANT_ID);
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
        verify(statement).setObject(13, null);
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
       * @then trueを返し、ステータスを進行中、点数を 80、利益を 1000000 円に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
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
                    "その他",
                    new BigDecimal(1_000_000),
                    REGISTER_DATE_TEXT,
                    "U000")
                .build());
        SES_AI_T_MATCH match = new SES_AI_T_MATCH(TENANT_ID);
        match.setMatchingId(MATCHING_ID);

        assertThat(match.selectByPk(connection)).isTrue();

        assertThat(match.getStatus()).isEqualTo(MatchingStatus.進行中);
        assertThat(match.getScore()).isEqualTo(80);
        assertThat(match.getProfit().getValue()).isEqualByComparingTo(new BigDecimal(1_000_000));
      }

      /**
       * 該当行が無い場合に false を返し、項目を変えないことを確かめる。 存在しないマッチングを読み込んでも、入力中の値を壊さない。
       *
       * @given マッチングIDが "0123456789"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then falseを返し、利益を変えない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_MATCH match = match();

        assertThat(match.selectByPk(connection)).isFalse();

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
       * @then trueを返し、点数をnullに、利益を空の値に更新する
       */
      @MockDbConnection
      @Test
      void 点数と利益が未設定の行の場合_trueを返す(final Connection connection) throws SQLException {
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
                    "その他",
                    null,
                    REGISTER_DATE_TEXT,
                    "U000")
                .build());
        SES_AI_T_MATCH match = match();

        assertThat(match.selectByPk(connection)).isTrue();

        assertThat(match.getScore()).isNull();
        assertThat(match.getProfit().isEmpty()).isTrue();
      }

      /**
       * マッチングIDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返すおそれがある。
       *
       * @given マッチングIDが null
       * @when selectByPk を接続で実行する
       * @then falseを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void マッチングIDがnullの場合_DBを呼ばずfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_MATCH(TENANT_ID).selectByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class selectByPkWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * マッチングIDが一致する行を、テナントの条件を付けずに読み込むことを確かめる。 共有ビューでは利用者のテナントが分からず、マッチングIDからテナントを特定する。
       *
       * @given テナントID "_temp_"、マッチングIDが "0123456789"、SELECT の結果が 1 行テナントID "T002"
       * @when selectByPkWithoutTenantId を接続で実行する
       * @then trueを返し、テナントIDを "T002" に更新し、テナントIDを条件に設定しない
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_テナントIDを更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder()
                    .columns(COLUMNS_WITH_TENANT_ID)
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
                        "その他",
                        new BigDecimal(1_000_000),
                        REGISTER_DATE_TEXT,
                        "U000",
                        "T002")
                    .build());
        SES_AI_T_MATCH match = new SES_AI_T_MATCH("_temp_");
        match.setMatchingId(MATCHING_ID);

        assertThat(match.selectByPkWithoutTenantId(connection)).isTrue();

        assertThat(match.getTenantId()).isEqualTo("T002");
        assertThat(match.getJobId()).isEqualTo("J001");
        assertThat(match.getStatus()).isEqualTo(MatchingStatus.進行中);
        verify(statement, never()).setString(eq(2), anyString());
      }

      /**
       * 該当行が無い場合に false を返し、テナントIDを変えないことを確かめる。 存在しないマッチングIDで、別のテナントとして扱わない。
       *
       * @given テナントID "_temp_"、マッチングIDが "0123456789"、SELECT の結果が 0 行
       * @when selectByPkWithoutTenantId を接続で実行する
       * @then falseを返し、テナントIDを "_temp_" のままにする
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection, MockResultSetBuilder.builder().columns(COLUMNS_WITH_TENANT_ID).build());
        SES_AI_T_MATCH match = new SES_AI_T_MATCH("_temp_");
        match.setMatchingId(MATCHING_ID);

        assertThat(match.selectByPkWithoutTenantId(connection)).isFalse();

        assertThat(match.getTenantId()).isEqualTo("_temp_");
      }
    }

    @Nested
    class 境界値 {

      /**
       * マッチングIDが null の場合に DB を呼ばず false を返すことを確かめる。 キーの無い検索は全マッチングを対象にしてしまう。
       *
       * @given マッチングIDが null
       * @when selectByPkWithoutTenantId を接続で実行する
       * @then falseを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void マッチングIDがnullの場合_DBを呼ばずfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_MATCH(TENANT_ID).selectByPkWithoutTenantId(connection)).isFalse();

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
                    + " must_evaluation_text = ?, want_evaluation_text = ?, other_evaluation_text"
                    + " = ?, profit = ?, register_date = ?, register_user = ? WHERE matching_id = ?"
                    + " AND tenant_id = ?");
        verify(statement).setString(6, "20");
        verify(statement).setObject(8, 80);
        verify(statement).setString(15, MATCHING_ID);
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
        verify(statement).setObject(12, null);
        verify(statement).setTimestamp(13, null);
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
       * @then 条件の16番目に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(match().updateByPk(connection)).isTrue();

        verify(statement).setString(16, TENANT_ID);
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
