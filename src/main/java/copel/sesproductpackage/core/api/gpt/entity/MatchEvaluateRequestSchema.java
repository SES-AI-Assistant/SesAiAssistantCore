package copel.sesproductpackage.core.api.gpt.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import copel.sesproductpackage.core.api.gpt.entity.ChooseBestInfoRequestSchema.ResourceInformation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるマッチ評価要求エンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MatchEvaluateRequestSchema {
  /** 案件情報 */
  private ResourceInformation jobInformation;
  /** 要員情報 */
  private ResourceInformation personInformation;
}
