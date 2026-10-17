package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.api.gpt.entity.ChooseBestInfoResponseSchema.CandidateEvaluationResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ChooseBestInfoResponseSchema の単体テスト.
 *
 * @author Copel Co., Ltd.
 */
class ChooseBestInfoResponseSchemaTest {

  /**
   * 候補の評価結果を生成する.
   *
   * @param id 識別ID
   * @param score マッチ度
   * @param must 必須スキル評価
   * @return 候補の評価結果
   */
  private CandidateEvaluationResult candidate(String id, int score, EvaluateType must) {
    return new CandidateEvaluationResult(id, score, must, EvaluateType.FullyMet, true, true);
  }

  @Test
  @DisplayName("getBestResult: candidateResultsがnullの場合はnull")
  void testGetBestResult_Null() {
    assertNull(new ChooseBestInfoResponseSchema(null).getBestResult());
  }

  @Test
  @DisplayName("getBestResult: 必須スキルがPartiallyMet（類似経験の明記あり）の候補も選出対象となること")
  void testGetBestResult_PartiallyMetIncluded() {
    ChooseBestInfoResponseSchema schema =
        new ChooseBestInfoResponseSchema(
            List.of(
                candidate("P000000001", 60, EvaluateType.FullyMet),
                candidate("P000000002", 80, EvaluateType.PartiallyMet)));
    assertEquals("P000000002", schema.getBestResult().getId());
  }

  @Test
  @DisplayName("getBestResult: 必須スキルがNotMet・Unknownの候補は選出されないこと")
  void testGetBestResult_NotMetExcluded() {
    ChooseBestInfoResponseSchema schema =
        new ChooseBestInfoResponseSchema(
            List.of(
                candidate("P000000001", 50, EvaluateType.FullyMet),
                candidate("P000000002", 90, EvaluateType.NotMet),
                candidate("P000000003", 95, EvaluateType.Unknown)));
    assertEquals("P000000001", schema.getBestResult().getId());
  }

  @Test
  @DisplayName("getBestResult: 人月工数・その他制約を満たさない候補は選出されないこと")
  void testGetBestResult_ConstraintsExcluded() {
    ChooseBestInfoResponseSchema schema =
        new ChooseBestInfoResponseSchema(
            List.of(
                new CandidateEvaluationResult(
                    "P000000001", 90, EvaluateType.FullyMet, EvaluateType.FullyMet, false, true),
                new CandidateEvaluationResult(
                    "P000000002", 90, EvaluateType.FullyMet, EvaluateType.FullyMet, true, false)));
    assertNull(schema.getBestResult());
  }

  @Test
  @DisplayName("Schema: 必須スキル評価に集約ルールと必須記載なし時の扱いが指示されていること")
  void testSchema_MustSkillDescription() throws NoSuchFieldException {
    copel.sesproductpackage.core.api.gpt.schema.Schema schema =
        CandidateEvaluationResult.class
            .getDeclaredField("mustSkillEvaluateResult")
            .getAnnotation(copel.sesproductpackage.core.api.gpt.schema.Schema.class);
    assertTrue(schema.description().contains("PartiallyMetより優先"));
    assertTrue(schema.description().contains("必須スキルの記載が無ければ一律FullyMet"));
  }

  @Test
  @DisplayName("toEvaluiationText: 必須スキルがPartiallyMetの場合は〇で表示されること")
  void testToEvaluiationText_PartiallyMet() {
    assertTrue(
        candidate("P000000001", 70, EvaluateType.PartiallyMet)
            .toEvaluiationText()
            .contains("■必須スキル: 〇"));
  }
}
