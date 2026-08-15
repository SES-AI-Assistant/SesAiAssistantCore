package copel.sesproductpackage.core.api.gpt.entity;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるスキルシート要約結果エンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillsheetInfoSchema {
  @Schema(
      title = "スキル一覧",
      description = "スキル、経験のリスト。各プロジェクトで使用されたスキルについて、各プロジェクトの期間（開始年月～終了年月）から月数を算出し、スキル毎に合算したもの。",
      itemType = Experience.class,
      minItems = 1,
      maxItems = 20)
  private List<Experience> experiences = null;

  @Schema(
      title = "PJ経験一覧",
      description = "直近のPJ経験",
      itemType = ProjectExperience.class,
      minItems = 1,
      maxItems = 10)
  private List<ProjectExperience> projectExperiences = null;

  // ================================================
  // メソッド
  // ================================================
  /**
   * 案件概要文に変換します.
   *
   * @return 案件概要文
   */
  public String toSummaryText() {
    StringBuilder sb = new StringBuilder();
    // 1. スキル
    if (this.experiences != null && !this.experiences.isEmpty()) {
      sb.append("■スキル・経験\n");
      for (Experience experience : this.experiences) {
        sb.append("・")
            .append(experience.getPerspective())
            .append(experience.getDuration() != null ? ": " + experience.getDuration() : "")
            .append("\n");
      }
    }
    // 1. 直近の経験
    if (this.projectExperiences != null && !this.projectExperiences.isEmpty()) {
      sb.append("■直近のPJ経験\n");
      for (ProjectExperience projectExperience : this.projectExperiences) {
        sb.append(projectExperience.getStartMonth())
            .append("-")
            .append(projectExperience.getEndMonth())
            .append(": ")
            .append(projectExperience.getPerspective())
            .append("\n");
      }
    }
    // 1000文字を超える場合は安全にカット（DB制約対策）
    String resultText = sb.toString().trim();
    if (resultText.length() > 1000) {
      return resultText.substring(0, 1000);
    }
    return resultText;
  }

  // ================================================
  // フィールド用のクラス
  // ================================================
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Experience {
    @Schema(
        title = "要求項目",
        description = "経験、スキル、観点など",
        maxLength = 30,
        required = true,
        example = "SpringBootによるWEBアプリ開発")
    private String perspective;

    @Schema(
        title = "要求期間",
        description =
            "各プロジェクトで使用されたスキルについて、各プロジェクトの期間（開始年月～終了年月）から月数を算出し、スキル毎に合算した経験年数や期間（例: 「3年」）。本文中に明確な年数や期間の記載がない場合は、推測せず必ず「null」にすること。",
        maxLength = 30,
        example = "3年")
    private String duration = null;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectExperience {
    @Schema(
        title = "開始年月",
        description = "プロジェクト開始年月（「YYYY年M月」形式）。「8月」のように月しか記載がない場合は適切な年を補完すること。",
        required = true,
        pattern = "^[0-9]{4}年([1-9]|1[0-2])月$",
        example = "2023年8月")
    private String startMonth;

    @Schema(
        title = "終了年月",
        description = "プロジェクト終了年月（「YYYY年M月」形式）。「8月」のように月しか記載がない場合は適切な年を補完すること。",
        required = true,
        pattern = "^[0-9]{4}年([1-9]|1[0-2])月$",
        example = "2023年11月")
    private String endMonth;

    @Schema(
        title = "内容",
        description = "プロジェクト内容",
        maxLength = 50,
        required = true,
        example = "大規模通信システムにおいてSEとして20人規模の基本設計工程のとりまとめ、設計レビューを実施")
    private String perspective;
  }
}
