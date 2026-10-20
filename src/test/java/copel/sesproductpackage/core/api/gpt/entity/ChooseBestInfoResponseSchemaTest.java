package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import copel.sesproductpackage.core.api.gpt.entity.ChooseBestInfoResponseSchema.CandidateEvaluationResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** {@link ChooseBestInfoResponseSchema} の単体テスト。 */
class ChooseBestInfoResponseSchemaTest {

  @Nested
  class getBestResult {

    @Nested
    class 正常系 {

      /**
       * 全ての評価を満たす候補が複数ある場合に、マッチ度が最大の候補を返すことを確かめる。 選出の結果を通知に使うため、最も高いマッチ度の候補を選ぶことを守る。
       *
       * @given 候補 A のマッチ度が80、候補 E のマッチ度が85で、どちらも全ての評価を満たす
       * @when getBestResult を呼び出す
       * @then マッチ度が85の候補 E を返す
       */
      @Test
      void 全ての評価を満たす候補が複数ある場合_マッチ度が最大の候補を返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("A", 80, FULLY, FULLY, true, true),
                candidate("E", 85, FULLY, FULLY, true, true));

        assertThat(schema.getBestResult().getId()).isEqualTo("E");
      }

      /**
       * 全ての評価を満たす候補の場合に、評価文に各評価のアイコンを含む文字列を返すことを確かめる。 通知と画面に表示する評価の内容を、返却した候補から確認できるようにすることを守る。
       *
       * @given 候補 E は必須と尚可の評価が FullyMet で、その他の制約を満たす
       * @when getBestResult を呼び出し、返却した候補の toEvaluiationText を呼び出す
       * @then 必須、尚可のアイコンと、その他制約の満たすを含む文字列を返す
       */
      @Test
      void 全ての評価を満たす候補の場合_評価文に各評価のアイコンを含む文字列を返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(candidate("E", 85, FULLY, FULLY, true, true));

        assertThat(schema.getBestResult().toEvaluiationText())
            .isEqualTo("■必須スキル: ◎\n■尚可スキル: ◎\n■その他制約事項: 満たす\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須スキルが全て満たされる候補が無い場合に、null を返すことを確かめる。 必須を満たさない候補を選ぶと、要件に合わない人材を提案してしまうので、その仕様を守る。
       *
       * @given 候補 B は必須スキルが PartiallyMet で、候補 C はその他制約を満たさない
       * @when getBestResult を呼び出す
       * @then null を返す
       */
      @Test
      void 必須スキルが全て満たされる候補が無い場合_nullを返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("B", 90, PARTIAL, FULLY, true, true),
                candidate("C", 70, FULLY, PARTIAL, true, false));

        assertThat(schema.getBestResult()).isNull();
      }

      /**
       * 候補の一覧が未設定の場合に、null を返すことを確かめる。 一覧が無いときに例外を投げると、選出処理全体が止まるので、その仕様を守る。
       *
       * @given 候補の一覧が未設定である
       * @when getBestResult を呼び出す
       * @then null を返す
       */
      @Test
      void 候補の一覧が未設定の場合_nullを返す() {
        assertThat(new ChooseBestInfoResponseSchema().getBestResult()).isNull();
      }

      /**
       * 尚可スキルの評価が未設定の最良候補の場合に、評価テキストの尚可をハイフンで表示することを確かめる。 評価の無い尚可の項目を満たしたものとして表示しないことを守る。
       *
       * @given 候補 G は必須 FullyMet、尚可スキルの評価が null、その他制約を満たす
       * @when getBestResult を呼び出す
       * @then G を返し、評価テキストに尚可をハイフンで含む
       */
      @Test
      void 尚可スキルの評価が未設定の最良候補の場合_尚可をハイフンで表示したテキストを返す() {
        ChooseBestInfoResponseSchema schema = schemaOf(candidate("G", 90, FULLY, null, true, true));

        CandidateEvaluationResult best = schema.getBestResult();

        assertThat(best.getId()).isEqualTo("G");
        assertThat(best.toEvaluiationText()).contains("■尚可スキル: -\n");
      }
    }
  }

  @Nested
  class getFilteredSortedResults_FilteringRules {

    @Nested
    class 正常系 {

      /**
       * 絞り込みのルールを指定した場合に、条件を満たす候補をマッチ度の降順で返すことを確かめる。 画面に表示する順番は、マッチ度の高い候補から並ぶことを前提にしているので、その仕様を守る。
       *
       * @given 候補 A、B、C があり、B と A は条件を満たし、C は尚可スキルが PartiallyMet である
       * @when 必須と尚可の両方のレベルを PartiallyMet にして getFilteredSortedResults を呼び出す
       * @then 条件を満たす B、A、C を、マッチ度の高い順に返す
       */
      @Test
      void ルールを指定した場合_条件を満たす候補をマッチ度の降順に並べたリストを返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("A", 80, FULLY, FULLY, true, true),
                candidate("B", 90, PARTIAL, FULLY, true, true),
                candidate("C", 70, FULLY, PARTIAL, true, true));

        assertThat(schema.getFilteredSortedResults(rules(PARTIAL, PARTIAL, false)))
            .extracting(CandidateEvaluationResult::getId)
            .containsExactly("B", "A", "C");
      }
    }

    @Nested
    class 境界値 {

      /**
       * その他制約の検証を必須にしない場合に、その他制約を満たさない候補も含めて返し、その評価テキストに満たさないと表示することを確かめる。
       * その他制約の判定を外した条件で、対象外の候補を落とさないことを守る。
       *
       * @given 候補 A と C があり、C はその他制約を満たさない
       * @when その他制約を必須にしないルールで getFilteredSortedResults を呼び出す
       * @then C を含むリストを返し、C の評価テキストにその他制約の満たさないを含む
       */
      @Test
      void その他制約を必須にしない場合_その他制約を満たさない候補も含めたリストを返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("A", 80, FULLY, FULLY, true, true),
                candidate("C", 70, FULLY, FULLY, true, false));

        List<CandidateEvaluationResult> results =
            schema.getFilteredSortedResults(rules(FULLY, FULLY, false));

        assertThat(results).extracting(CandidateEvaluationResult::getId).containsExactly("A", "C");
        assertThat(results.get(1).toEvaluiationText()).contains("■その他制約事項: 満たさない\n");
      }

      /**
       * 必須スキルのレベルが未指定の場合に、必須スキルの評価を無視して、評価が無い候補も含めることを確かめる。 必須の評価が無い候補を、レベルを外した条件では除外しないことを守る。
       *
       * @given 候補 A と B と N があり、B は必須スキルが PartiallyMet で、N は必須スキルの評価が null である
       * @when 必須のレベルを null にして getFilteredSortedResults を呼び出す
       * @then B、A、N の順のリストを返し、N の評価テキストに必須をハイフンで含む
       */
      @Test
      void 必須スキルのレベルがnullの場合_必須スキルの評価を無視したリストを返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("A", 80, FULLY, FULLY, true, true),
                candidate("B", 90, PARTIAL, FULLY, true, true),
                candidate("N", 50, null, FULLY, true, true));

        List<CandidateEvaluationResult> results =
            schema.getFilteredSortedResults(rules(null, FULLY, true));

        assertThat(results)
            .extracting(CandidateEvaluationResult::getId)
            .containsExactly("B", "A", "N");
        assertThat(results.get(2).toEvaluiationText()).startsWith("■必須スキル: -\n");
      }

      /**
       * 尚可スキルのレベルが未指定の場合に、尚可スキルの評価を無視することを確かめる。 レベルを外したルールでは、尚可スキルの評価に関わらず候補を残すことを守る。
       *
       * @given 候補 A と B があり、B は尚可スキルが NotMet である
       * @when 尚可のレベルを null にして getFilteredSortedResults を呼び出す
       * @then B と A を、マッチ度の降順で返す
       */
      @Test
      void 尚可スキルのレベルがnullの場合_尚可スキルの評価を無視したリストを返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("A", 80, FULLY, FULLY, true, true),
                candidate("B", 90, FULLY, NOT_MET, true, true));

        assertThat(schema.getFilteredSortedResults(rules(FULLY, null, true)))
            .extracting(CandidateEvaluationResult::getId)
            .containsExactly("B", "A");
      }

      /**
       * 人月工数を満たさない候補がある場合に、その候補を除外することを確かめる。 稼働可能な工数に届かない人材を提案しないことを守る。
       *
       * @given 候補 A と D があり、D は人月工数の評価が false である
       * @when 既定のルールで getFilteredSortedResults を呼び出す
       * @then D を含まず A だけを返す
       */
      @Test
      void 人月工数を満たさない候補がある場合_その候補を除いたリストを返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("A", 80, FULLY, FULLY, true, true),
                candidate("D", 60, FULLY, FULLY, false, true));

        assertThat(schema.getFilteredSortedResults())
            .extracting(CandidateEvaluationResult::getId)
            .containsExactly("A");
      }

      /**
       * 絞り込みのルールが未指定の場合に、既定のルールで絞り込んだ結果を返すことを確かめる。 ルールの省略時も既定の絞り込みを使い、候補の基準を変えないことを守る。
       *
       * @given 候補 A と C があり、C はその他制約を満たさない
       * @when ルールを null にして getFilteredSortedResults を呼び出す
       * @then 既定のルールで絞り込んだ A だけを返す
       */
      @Test
      void 絞り込みのルールがnullの場合_既定のルールで絞り込んだ結果を返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("A", 80, FULLY, FULLY, true, true),
                candidate("C", 70, FULLY, FULLY, true, false));

        assertThat(schema.getFilteredSortedResults(null))
            .extracting(CandidateEvaluationResult::getId)
            .containsExactly("A");
      }

      /**
       * 候補の一覧が未設定の場合に、空のリストを返すことを確かめる。 一覧が無いときに例外を投げると、後続の処理が止まるので、その仕様を守る。
       *
       * @given 候補の一覧が未設定である
       * @when 既定のルールで getFilteredSortedResults を呼び出す
       * @then 空のリストを返す
       */
      @Test
      void 候補の一覧が未設定の場合_空のリストを返す() {
        assertThat(new ChooseBestInfoResponseSchema().getFilteredSortedResults()).isEmpty();
      }

      /**
       * 必須スキルがレベルより低い評価の候補がある場合に、その候補を除外することを確かめる。 必須スキルの評価が要求レベルに届かない候補を結果に含めないことを守る。
       *
       * @given 候補 X は必須スキルが NotMet で、候補 Y は FullyMet である
       * @when 必須のレベルを PartiallyMet にして getFilteredSortedResults を呼び出す
       * @then 候補 Y だけを含むリストを返す
       */
      @Test
      void 必須スキルがレベルより低い評価の候補がある場合_その候補を除いたリストを返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("X", 90, NOT_MET, FULLY, true, true),
                candidate("Y", 80, FULLY, FULLY, true, true));

        assertThat(schema.getFilteredSortedResults(rules(PARTIAL, FULLY, false)))
            .extracting(CandidateEvaluationResult::getId)
            .containsExactly("Y");
      }

      /**
       * 尚可スキルがレベルより低い評価の候補がある場合に、その候補を除外することを確かめる。 尚可スキルの評価が要求レベルに届かない候補を結果に含めないことを守る。
       *
       * @given 候補 X は尚可スキルが NotMet で、候補 Y は FullyMet である
       * @when 尚可のレベルを PartiallyMet にして getFilteredSortedResults を呼び出す
       * @then 候補 Y だけを含むリストを返す
       */
      @Test
      void 尚可スキルがレベルより低い評価の候補がある場合_その候補を除いたリストを返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("X", 90, FULLY, NOT_MET, true, true),
                candidate("Y", 80, FULLY, FULLY, true, true));

        assertThat(schema.getFilteredSortedResults(rules(FULLY, PARTIAL, false)))
            .extracting(CandidateEvaluationResult::getId)
            .containsExactly("Y");
      }

      /**
       * 必須スキルの評価が null の候補は、必須のレベル指定に対して満たさない扱いで除くことを確かめる。 評価の無い候補を満たしたものとして結果に含めず、除外した候補の ID
       * を警告ログに残すことを守る。
       *
       * @given 候補 E は必須スキルの評価が null で、候補 Y は FullyMet である
       * @when 必須のレベルを FullyMet にして getFilteredSortedResults を呼び出す
       * @then 候補 Y だけを含むリストを返し、警告ログに候補 E の ID を含む
       */
      @Test
      void 必須スキル評価がnullの候補があり必須レベルを指定した場合_その候補を除いたリストを返す() throws Exception {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("E", 80, null, FULLY, true, true),
                candidate("Y", 70, FULLY, FULLY, true, true));
        Logger logger = (Logger) LoggerFactory.getLogger(ChooseBestInfoResponseSchema.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        List<CandidateEvaluationResult> results;
        try {
          results = schema.getFilteredSortedResults(rules(FULLY, FULLY, true));
        } finally {
          logger.detachAppender(appender);
        }

        assertThat(results).extracting(CandidateEvaluationResult::getId).containsExactly("Y");
        assertThat(appender.list)
            .anyMatch(
                event ->
                    event.getLevel() == Level.WARN && event.getFormattedMessage().contains("ID=E"));
      }

      /**
       * 尚可スキルの評価が null の候補は、尚可のレベル指定に対して満たさない扱いで除くことを確かめる。 評価の無い尚可の候補を満たしたものとして結果に含めず、除外した候補の ID
       * を警告ログに残すことを守る。
       *
       * @given 候補 E は尚可スキルの評価が null で、候補 Y は FullyMet である
       * @when 尚可のレベルを FullyMet にして getFilteredSortedResults を呼び出す
       * @then 候補 Y だけを含むリストを返し、警告ログに候補 E の ID を含む
       */
      @Test
      void 尚可スキル評価がnullの候補があり尚可レベルを指定した場合_その候補を除いたリストを返す() throws Exception {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("E", 80, FULLY, null, true, true),
                candidate("Y", 70, FULLY, FULLY, true, true));
        Logger logger = (Logger) LoggerFactory.getLogger(ChooseBestInfoResponseSchema.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        List<CandidateEvaluationResult> results;
        try {
          results = schema.getFilteredSortedResults(rules(FULLY, FULLY, true));
        } finally {
          logger.detachAppender(appender);
        }

        assertThat(results).extracting(CandidateEvaluationResult::getId).containsExactly("Y");
        assertThat(appender.list)
            .anyMatch(
                event ->
                    event.getLevel() == Level.WARN && event.getFormattedMessage().contains("ID=E"));
      }
    }
  }

  @Nested
  class getFilteredSortedResults {

    @Nested
    class 正常系 {

      /**
       * 絞り込みのルールを省略した場合に、既定のルールで絞り込んだ結果を返すことを確かめる。 省略時の既定値を既定のルールと同じにし、呼び出し元ごとの差を出さないことを守る。
       *
       * @given 候補 A と C があり、C はその他制約を満たさない
       * @when 引数なしの getFilteredSortedResults を呼び出す
       * @then 既定のルールで絞り込んだ A だけを返す
       */
      @Test
      void 候補が混在する場合_既定のルールで絞り込んだ結果を返す() {
        ChooseBestInfoResponseSchema schema =
            schemaOf(
                candidate("A", 80, FULLY, FULLY, true, true),
                candidate("C", 70, FULLY, FULLY, true, false));

        assertThat(schema.getFilteredSortedResults())
            .extracting(CandidateEvaluationResult::getId)
            .containsExactly("A");
      }
    }
  }

  private static final EvaluateType FULLY = EvaluateType.FullyMet;
  private static final EvaluateType PARTIAL = EvaluateType.PartiallyMet;
  private static final EvaluateType NOT_MET = EvaluateType.NotMet;

  /**
   * 候補評価結果を1件作る.
   *
   * @param id 識別ID
   * @param score マッチ度
   * @param must 必須スキル評価
   * @param want 尚可スキル評価
   * @param personMonths 人月工数の評価
   * @param other その他制約の評価
   * @return 候補評価結果
   */
  private static CandidateEvaluationResult candidate(
      final String id,
      final int score,
      final EvaluateType must,
      final EvaluateType want,
      final boolean personMonths,
      final boolean other) {
    CandidateEvaluationResult result = new CandidateEvaluationResult();
    result.setId(id);
    result.setMatchScore(score);
    result.setMustSkillEvaluateResult(must);
    result.setWantSkillEvaluateResult(want);
    result.setPersonMonthsEvaluateResult(personMonths);
    result.setOtherConstraintsResult(other);
    return result;
  }

  /**
   * 候補評価結果を並べた応答を作る.
   *
   * @param candidates 候補評価結果
   * @return 応答
   */
  private static ChooseBestInfoResponseSchema schemaOf(
      final CandidateEvaluationResult... candidates) {
    ChooseBestInfoResponseSchema schema = new ChooseBestInfoResponseSchema();
    schema.setCandidateResults(List.of(candidates));
    return schema;
  }

  /**
   * 絞り込みのルールを作る.
   *
   * @param must 必須スキルのレベル
   * @param want 尚可スキルのレベル
   * @param otherRequired その他制約の検証が必須かどうか
   * @return 絞り込みのルール
   */
  private static FilteringRules rules(
      final EvaluateType must, final EvaluateType want, final boolean otherRequired) {
    return new FilteringRules(must, want, otherRequired);
  }
}
