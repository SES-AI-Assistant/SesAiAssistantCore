package copel.sesproductpackage.core.api.gpt.entity;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.unit.Money;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによる案件要約結果エンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobInfoSchema {
  @Schema(
      title = "案件名",
      description = "案件名の記載がない場合は内容から簡潔な案件名を考えて記載してください",
      maxLength = 30,
      required = true,
      example = "大手製造業者向け調達管理システムの開発支援")
  private String title;

  @Schema(
      title = "案件概要",
      description = "案件の概要、背景、目的などの概要",
      maxLength = 100,
      example =
          "製造業者様で運用していた調達管理システムの刷新に伴い、周辺システムと連携しながら新規システムの開発～マイグレーションを実施中。基本設計からアプリの開発に加えデータクレンジングやDB設計も行う。")
  private String overview;

  @Schema(
      title = "必須スキル",
      description = "案件が要求する必須経験、スキル等のリスト",
      itemType = Requirements.class,
      required = true,
      minItems = 1,
      maxItems = 10)
  private List<Requirements> mustList;

  @Schema(
      title = "尚可スキル",
      description = "案件が要求する尚可経験、スキル等のリスト",
      itemType = Requirements.class,
      minItems = 1,
      maxItems = 10)
  private List<Requirements> wantList = null;

  @Schema(title = "開始月", description = "案件の開始月", required = true, gt = 0, lt = 13, example = "6")
  private int startMonth;

  @Schema(title = "場所", description = "案件の場所、オフィスの最寄り駅など", example = "品川")
  private String place;

  @Schema(
      title = "単価（円）",
      description = "案件の単価/月。幅がある場合は最大値を設定。時給や日給が設定されている場合は1ヵ月(160h)に換算する。スキル見合いの場合は999万円とする。",
      required = true,
      itemType = Money.class)
  private Money price;

  @Schema(
      title = "出社要件",
      description =
          "1週間あたり出社する必要のある頻度。フルリモートや未記載の場合は「0」、フル出社や常駐は「5」とする。週1～2など幅がある場合は最も多い数（この場合は「2」）を記入する。初日のみの出社や最初の数週間のみの出社は含まない。",
      required = true,
      gt = -1,
      lt = 6,
      example = "1")
  private int officeRequirements;

  @Schema(title = "清算幅", description = "未記載の場合は本項目は不要", example = "140～180h")
  private String baseHoursRange = null;

  @Schema(title = "その他条件", description = "その他の条件や制約事項などの一覧", itemType = String.class)
  private List<String> otherRequirements = null;

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
    // 1. 案件名
    sb.append(this.title).append("\n");
    // 2. 概要
    sb.append("■概要\n").append(this.overview).append("\n");
    // 3. 必須
    if (this.mustList != null && !this.mustList.isEmpty()) {
      sb.append("■必須\n");
      for (Requirements must : this.mustList) {
        sb.append("・")
            .append(must.getPerspective())
            .append(must.getDuration() != null ? ": " + must.getDuration() : "")
            .append("\n");
      }
    }
    // 4. 尚可
    if (this.wantList != null && !this.wantList.isEmpty()) {
      sb.append("■尚可\n");
      for (Requirements must : this.wantList) {
        sb.append("・")
            .append(must.getPerspective())
            .append(must.getDuration() != null ? "：" + must.getDuration() : "")
            .append("\n");
      }
    }
    // 5. 開始
    sb.append("■開始: ").append(this.startMonth).append("月").append("\n");
    // 6. 単価
    sb.append("■単価: ").append(this.price).append("\n");
    // 7. 出社要件、場所
    if (this.officeRequirements == 0) {
      sb.append("■出社要件: ").append("フルリモート\n");
    } else if (this.officeRequirements == 5) {
      sb.append("■出社要件: ").append("常駐").append("\n");
      sb.append("■場所: ").append(this.place != null ? this.place : "不明").append("\n");
    } else {
      sb.append("■出社要件: ").append("週").append(this.officeRequirements).append("\n");
      sb.append("■場所: ").append(this.place != null ? this.place : "不明").append("\n");
    }
    // 8. 清算幅
    if (this.baseHoursRange != null) {
      sb.append("■清算幅: ").append(this.baseHoursRange).append("\n");
    }
    // 9. その他
    if (this.otherRequirements != null && !this.otherRequirements.isEmpty()) {
      sb.append("■その他\n");
      for (String other : this.otherRequirements) {
        sb.append("・").append(other).append("\n");
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
  public static class Requirements {
    @Schema(
        title = "要求項目",
        description = "要求経験、スキル、観点など",
        maxLength = 30,
        required = true,
        example = "SpringBootを利用した開発経験")
    private String perspective;

    @Schema(
        title = "要求期間",
        description =
            "要求される経験年数や期間（例: 「3年」）。本文中に明確な年数や期間の記載がない場合は、推測せず必ずnullにすること。（「不明」「未記載」「なし」などの文字列は絶対に設定しないこと）",
        maxLength = 30,
        example = "3年")
    private String duration = null;
  }
}
