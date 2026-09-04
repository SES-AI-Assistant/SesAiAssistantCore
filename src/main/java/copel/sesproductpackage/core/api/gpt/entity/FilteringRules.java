package copel.sesproductpackage.core.api.gpt.entity;

import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI検索結果をフィルタリングするためのルール設定.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilteringRules {
  /** 必須スキルの要求レベル。nullの場合は無視。 */
  private EvaluateType mustSkillLevel;

  /** 尚好スキルの要求レベル。nullの場合は無視。 */
  private EvaluateType wantSkillLevel;

  /** 出社要件の検証が必須かどうか。 */
  private boolean officeRequired;

  /** その他制約条件の検証が必須かどうか。 */
  private boolean otherConstraintsRequired;

  /**
   * デフォルトのフィルタリングルール（全て必須）を返却する.
   *
   * @return デフォルトのFilteringRules
   */
  public static FilteringRules createDefault() {
    FilteringRules rules = new FilteringRules();
    rules.setMustSkillLevel(EvaluateType.FullyMet);
    rules.setWantSkillLevel(EvaluateType.FullyMet);
    rules.setOfficeRequired(true);
    rules.setOtherConstraintsRequired(true);
    return rules;
  }
}
