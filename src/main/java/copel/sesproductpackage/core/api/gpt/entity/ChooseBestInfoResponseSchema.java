package copel.sesproductpackage.core.api.gpt.entity;

import java.util.List;

import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateSchema.EvaluateType;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.api.gpt.schema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるベストマッチ選出結果エンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChooseBestInfoResponseSchema {
  @Schema(
      title = "選出対象の評価結果リスト",
      description = "マッチングの基準となる案件または要員に対して、マッチ度合いを評価した結果を持つリスト",
      itemType = CandidateEvaluationResult.class,
      required = true)
  private List<CandidateEvaluationResult> candidateResults;

  /**
   * 評価結果を元に、ベストマッチな候補を返却する.
   *
   * @return 最適なCandidateEvaluationResult（該当なしの場合はnull）
   */
  @SchemaIgnore
  public CandidateEvaluationResult getBestResult() {
    // 必須オブジェクトの存在チェック
    if (this.candidateResults == null) {
      return null;
    }
    return this.candidateResults.stream()
        // 1. 各評価フラグがすべて true である要素のみに絞り込み
        .filter(CandidateEvaluationResult::isPriceEvaluateResult)
        .filter(CandidateEvaluationResult::isPlaceEvaluateResult)
        .filter(CandidateEvaluationResult::isOfficeEvaluateResult)
        .filter(CandidateEvaluationResult::isPersonMonthsEvaluateResult)
        // 2. 必須スキル評価が FullyMet の要素のみに絞り込み
        .filter(r -> EvaluateType.FullyMet.equals(r.getMustSkillEvaluateResult()))
        // 3. マッチ度（matchScore）が90点以上である要素のみに絞り込み
        .filter(r -> r.getMatchScore() >= 90)
        // 4. マッチ度の最大値（同点の場合は任意で1つ）を取得
        .max(CandidateEvaluationResult::compareTo)
        .orElse(null);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CandidateEvaluationResult implements Comparable<CandidateEvaluationResult> {
    @Schema(title = "識別ID", description = "当該情報の案件IDまたは要員ID", maxLength = 10, minLength = 10)
    private String id = null;

    @Schema(
        title = "マッチ度（点）",
        description = "要員と案件のマッチ度合いを示した数値",
        required = true,
        gt = -1,
        lt = 100)
    private int matchScore;

    @Schema(
        title = "必須スキル評価",
        description = "案件の求める必須スキル項目を要員が満たしているかどうかを3段階で表現した結果",
        itemType = EvaluateType.class,
        required = true,
        example = "FullyMet")
    private EvaluateType mustSkillEvaluateResult;

    @Schema(
        title = "尚可スキル評価",
        description = "案件の求める尚可スキル項目を要員が満たしているかどうかを3段階で表現した結果。案件に尚可スキルの要求記載が無ければ一律FullyMet",
        itemType = EvaluateType.class,
        required = true,
        example = "FullyMet")
    private EvaluateType wantSkillEvaluateResult;

    @Schema(
        title = "単価評価結果",
        description = "案件単価 >= 要員単価であればtrue、それ以外はfalse。案件側がスキル見合いである場合は一律true",
        defaultValue = "false",
        required = true)
    private boolean priceEvaluateResult;

    @Schema(
        title = "場所評価結果",
        description =
            "案件場所から要員の場所まで在来線1時間以内で通勤できるかどうか。案件側がフルリモートである場合、または案件や要員どちらかの場所が不明な場合は一律trueとする。",
        defaultValue = "false")
    private boolean placeEvaluateResult;

    @Schema(
        title = "出社要件評価結果",
        description = "要員がフルリモート希望かつ、案件がフルリモートである場合はtrue。要員が常駐可能かつ、案件が常駐である場合はtrue。要員の出社許容日数/週 >= 案件の求める出社日数/週である場合はtrue。案件または要員側に出社・フルリモート希望や要件が未記載の場合はtrueとする。それ以外は全てfalse。",
        defaultValue = "false",
        required = true)
    private boolean officeEvaluateResult;

    @Schema(
        title = "人月工数評価結果",
        description = "要員の稼働可能人月工数 >= 案件の求める人月工数であればtrue、それ以外はfalse。案件の求める人月工数が不明の場合は一律trueとする。",
        defaultValue = "true")
    private boolean personMonthsEvaluateResult = true;

    @SchemaIgnore
    @Override
    public int compareTo(CandidateEvaluationResult o) {
      if (o == null) {
        return 1;
      }
      // matchScore の降順（大きい順）
      return Integer.compare(this.matchScore, o.matchScore);
    }
  }
}
