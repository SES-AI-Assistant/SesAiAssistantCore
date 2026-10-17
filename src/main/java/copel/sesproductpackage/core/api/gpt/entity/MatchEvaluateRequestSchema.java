package copel.sesproductpackage.core.api.gpt.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import copel.sesproductpackage.core.api.gpt.entity.ChooseBestInfoRequestSchema.ResourceInformation;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるマッチ評価要求エンティティ. 【重要】スキル評価は『記載された実務経験』のみを対象とし、推測・拡大解釈は不可。
 * 代替可能な類似経験が明記されている場合のみPartiallyMetとする。 なお、LLMへの評価指示は Javadoc ではなく {@link
 * MatchEvaluateResponseSchema} の各フィールドの @Schema(description) として送信される点に注意。
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MatchEvaluateRequestSchema {
  @Schema(title = "案件情報", description = "マッチング対象となる案件の情報")
  private ResourceInformation jobInformation;

  @Schema(title = "要員情報", description = "マッチング対象となる要員のスキルシート情報")
  private ResourceInformation personInformation;
}
