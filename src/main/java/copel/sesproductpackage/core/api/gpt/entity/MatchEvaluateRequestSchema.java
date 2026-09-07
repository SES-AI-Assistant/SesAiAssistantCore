package copel.sesproductpackage.core.api.gpt.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import copel.sesproductpackage.core.api.gpt.entity.ChooseBestInfoRequestSchema.ResourceInformation;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるマッチ評価要求エンティティ.
 * 【重要】スキル評価は『記載された実務経験』のみを対象。類似スキルへの推測・拡大解釈は不可。
 * 例：『データ分析』と『時系列データ分析』は別スキル
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MatchEvaluateRequestSchema {
  @Schema(
      title = "案件情報",
      description = "マッチング対象となる案件の情報")
  private ResourceInformation jobInformation;

  @Schema(
      title = "要員情報",
      description = "マッチング対象となる要員のスキルシート情報")
  private ResourceInformation personInformation;
}
