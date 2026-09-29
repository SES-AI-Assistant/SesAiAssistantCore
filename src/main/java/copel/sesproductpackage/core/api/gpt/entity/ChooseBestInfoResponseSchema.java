package copel.sesproductpackage.core.api.gpt.entity;

import java.util.List;

import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.api.gpt.schema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
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
        // 各評価フラグがすべて true である要素のみに絞り込み
        .filter(r -> r.isPersonMonthsEvaluateResult())
        .filter(r -> r.isOtherConstraintsResult())
        // 必須スキル評価が FullyMet の要素のみに絞り込み
        .filter(r -> EvaluateType.FullyMet.equals(r.getMustSkillEvaluateResult()))
        // マッチ度の最大値（同点の場合は任意で1つ）を取得
        .max((r1, r2) -> r1.compareTo(r2))
        .orElse(null);
  }

  /**
   * 評価結果を元に、フィルタリングルールに基づいて条件を満たす全候補をマッチ度の降順で返却する.
   *
   * @param rules フィルタリングルール。nullの場合はデフォルトのフィルタリングを実行
   * @return 条件を満たすCandidateEvaluationResultのリスト（該当なしの場合は空リスト）
   */
  @SchemaIgnore
  public List<CandidateEvaluationResult> getFilteredSortedResults(FilteringRules rules) {
    if (this.candidateResults == null) {
      return List.of();
    }

    if (rules == null) {
      return getFilteredSortedResults();
    }

    return this.candidateResults.stream()
        .filter(r -> r.isPersonMonthsEvaluateResult())
        // その他制約条件（required=true の場合のみチェック）
        .filter(r -> !rules.isOtherConstraintsRequired() || r.isOtherConstraintsResult())
        // 必須スキル（level=null の場合は無視）
        .filter(r -> {
          if (rules.getMustSkillLevel() == null) {
            return true;
          }
          EvaluateType result = r.getMustSkillEvaluateResult();
          EvaluateType level = rules.getMustSkillLevel();
          return result.ordinal() <= level.ordinal();
        })
        // 尚好スキル（level=null の場合は無視）
        .filter(r -> {
          if (rules.getWantSkillLevel() == null) {
            return true;
          }
          EvaluateType result = r.getWantSkillEvaluateResult();
          EvaluateType level = rules.getWantSkillLevel();
          return result.ordinal() <= level.ordinal();
        })
        .sorted(java.util.Comparator.reverseOrder())
        .toList();
  }

  /**
   * 評価結果を元に、条件を満たす全候補をマッチ度の降順（大きい順）で返却する.
   * デフォルトのフィルタリングルール（全て必須）を使用します.
   *
   * @return 条件を満たすCandidateEvaluationResultのリスト（該当なしの場合は空リスト）
   */
  @SchemaIgnore
  public List<CandidateEvaluationResult> getFilteredSortedResults() {
    return getFilteredSortedResults(FilteringRules.createDefault());
  }

  @Data
  @Getter
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
        title = "人月工数評価結果",
        description = "要員の稼働可能人月工数 >= 案件の求める人月工数であればtrue、それ以外はfalse。案件の求める人月工数が不明の場合は一律trueとする。",
        defaultValue = "true")
    private boolean personMonthsEvaluateResult = true;

    @Schema(
        title = "その他制約条件評価結果",
        description = "単価、出社頻度を除いたその他の制約条件の評価結果。要員が希望する制約条件を案件が1つ以上違反している場合はfalse。案件が希望する制約条件を要員が1つ以上違反している場合はfalse。それ以外はtrue。例えば年齢制限や商流制限などを評価する。",
        defaultValue = "true")
    private boolean otherConstraintsResult = true;

    @SchemaIgnore
    @Override
    public int compareTo(CandidateEvaluationResult o) {
      if (o == null) {
        return 1;
      }
      // matchScore の降順（大きい順）
      return Integer.compare(this.matchScore, o.matchScore);
    }

    @SchemaIgnore
    public boolean isPersonMonthsEvaluateResult() {
      return this.personMonthsEvaluateResult;
    }

    @SchemaIgnore
    public boolean isOtherConstraintsResult() {
      return this.otherConstraintsResult;
    }

    /**
     * 評価結果をまとめた文章を返却する.
     *
     * @return 評価文
     */
    @SchemaIgnore
    public String toEvaluiationText() {
      StringBuilder sb = new StringBuilder();
      // 必須スキル
      sb.append("■必須スキル: ")
        .append(this.mustSkillEvaluateResult != null ? this.mustSkillEvaluateResult.getIcon() : "-")
        .append("\n");
      // 尚可スキル
      sb.append("■尚可スキル: ")
        .append(this.wantSkillEvaluateResult != null ? this.wantSkillEvaluateResult.getIcon() : "-")
        .append("\n");
      // その他
      sb.append("■その他制約事項: ")
        .append(this.otherConstraintsResult ? "満たす" : "満たさない")
        .append("\n");
      return sb.toString();
    }
  }
}
