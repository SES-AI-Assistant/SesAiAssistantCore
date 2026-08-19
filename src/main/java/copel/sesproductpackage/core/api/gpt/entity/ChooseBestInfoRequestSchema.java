package copel.sesproductpackage.core.api.gpt.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import copel.sesproductpackage.core.api.gpt.entity.ContentParseSchema.InformationType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるベストマッチ選出要求エンティティ.
 * 案件(Job)または要員(Person)のいずれかを軸(target)とし、候補(candidates)群から最適なマッチングを選出するためのリクエストデータ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChooseBestInfoRequestSchema {
  /** マッチングの基準となる1つの情報（案件または要員） */
  private ResourceInformation target;

  /** targetに対する選出対象となる候補リスト（targetが案件なら要員リスト、要員なら案件リスト） */
  private List<ResourceInformation> candidates;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class ResourceInformation {
    /** 案件か要員か種別を示す識別子 */
    private InformationType type;

    /** 識別ID（案件ID または 要員ID） */
    private String id;

    /** 案件または要員の要約文 */
    private String summary;
  }
}
