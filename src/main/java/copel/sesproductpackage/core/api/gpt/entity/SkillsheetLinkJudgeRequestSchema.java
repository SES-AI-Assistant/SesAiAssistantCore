package copel.sesproductpackage.core.api.gpt.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるスキルシートマッチ判定要求エンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SkillsheetLinkJudgeRequestSchema {
  /** 判定対象のファイル名. */
  private String fileName = null;

  /** 要員情報. */
  private List<PersonInfo> persons = null;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class PersonInfo {
    /** 要員ID. */
    private String personId;

    /** 要員情報要約. */
    private String summary = null;
  }

  @Override
  public String toString() {
    return OriginalStringUtils.toJson(this);
  }
}
