package copel.sesproductpackage.core.api.gpt.entity;

import java.util.List;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによる要員要約結果エンティティ.
 *
 * @author Copel Co., Ltd.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonInfoSummary {
  @Schema(
    description = "このメール・文章を送信・執筆した営業担当者や紹介元の名前。「紹介されている要員本人（name）」の名前やイニシャルを設定してはならない。文章内に差出人の名前・会社名が記載されていない場合は必ずnullにすること。",
    maxLength = 30,
    example = "株式会社ABC 田中"
  )
  private String senderName = null;
  @Schema(
    description = "紹介されている要員本人の名前またはイニシャル",
    maxLength = 10,
    required = true,
    example = "T.T"
  )
  private String name;
  @Schema(
    description = "年齢（不明な場合は-1）",
    gt=17,
    lt=100,
    example = "35"
  )
  private int age = -1;
  @Schema(
    description = "性別",
    itemType = Gender.class,
    required = true,
    example = "Man"
  )
  private Gender gender;
  @Schema(
    description = "国籍（未記載の場合は「日本」とする）",
    maxLength = 10,
    required = true,
    example = "日本"
  )
  private String nationality = "日本";
  @Schema(
    description = "稼働開始可能月",
    required = true,
    gt=0,
    lt=13,
    example = "6"
  )
  private int startMonth;
  @Schema(
    description = "単価（円）",
    required = true,
    itemType = Money.class
  )
  private Money price;
  @Schema(
    description = "要員の在住地域や最寄駅名など",
    example = "品川"
  )
  private String place = null;
  @Schema(
    description = "1週間あたり出社可能な頻度。フルリモート希望の場合は「0」、常駐可能や未記載の場合は「5」とする。週1～2など幅がある場合は最も多い数（この場合は「2」）を記入する。",
    required = true,
    gt=-1,
    lt=6,
    example = "1"
  )
  private int officeAvailability;
  @Schema(
    description = "商流や所属。不明な場合は「未記載」とする。",
    required = true,
    example = "1社先正社員"
  )
  private String organization = "未記載";
  @Schema(
    description = "経歴のリスト",
    itemType = Experience.class
  )
  private List<Experience> experiences = null;
  @Schema(
    description = "要員都合のNG条件のリスト",
    itemType = String.class
  )
  private List<String> ngRequirements = null;
  @Schema(
    description = "その他、備考などの事項",
    itemType = String.class
  )
  private List<String> otherRequirements = null;
  @Schema(
    description = "経歴書やスキルシートのURL。未記載の場合は設定しない。",
    format = "uri",
    pattern = "^https?://.+",
    example = "https://www.google.com"
  )
  private String url = null;

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
    // 1. 名前
    sb.append("■名前: ").append(this.name).append("\n");
    // 2. 年齢
    if (this.age > 0) {
      sb.append("■年齢: ").append(this.age).append("歳\n");
    }
    // 2. 性別
    sb.append("■性別: ").append(this.gender.toJapanese()).append("\n");
    // 3. 国籍
    sb.append("■国籍: ").append(this.nationality).append("\n");
    // 4. 稼働開始可能月
    sb.append("■開始: ").append(this.startMonth).append("月\n");
    // 5. 単価
    sb.append("■単価: ").append(this.price).append("\n");
    // 6. 所属形態
    sb.append("■所属形態: ").append(this.organization).append("\n");
    // 7. 場所
    if (this.place != null) {
      sb.append("■場所: ").append(this.place).append("\n");
    }
    // 8. 出社可否
    if (this.officeAvailability == 0) {
      sb.append("■出社: ").append("フルリモート希望\n");
    } else if (this.officeAvailability == 5) {
      sb.append("■出社: ").append("常駐可").append("\n");
    } else {
      sb.append("■出社: ").append("週").append(this.officeAvailability).append("まで可").append("\n");
    }
    // 9. 経歴
    if (this.experiences != null && !this.experiences.isEmpty()) {
      sb.append("■経歴\n");
      for (Experience experience : this.experiences) {
        sb.append("・").append(experience.getPerspective())
          .append(experience.getDuration() != null ? ": " + experience.getDuration() : "")
          .append("\n");
      }
    }
    // 10. NG条件
    if (this.ngRequirements != null && !this.ngRequirements.isEmpty()) {
      sb.append("■NG条件\n");
      for (String ng : this.ngRequirements) {
        sb.append("・").append(ng).append("\n");
      }
    }
    // 11. 備考
    if (this.otherRequirements != null && !this.otherRequirements.isEmpty()) {
      sb.append("■備考\n");
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

  @Override
  public String toString() {
      return OriginalStringUtils.toJson(this);
  }

  // ================================================
  // フィールド用のクラス
  // ================================================
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Experience {
    @Schema(
      description = "経験、スキル、観点など",
      maxLength = 30,
      required = true,
      example = "SpringBootによるWEBアプリ開発"
    )
    private String perspective;
    @Schema(
      description = "経験年数や期間（例: 「3年」）。本文中に明確な年数や期間の記載がない場合は、推測せず必ずnullにすること。（「不明」「未記載」「なし」などの文字列は絶対に設定しないこと）",
      maxLength = 30,
      example = "3年"
    )
    private String duration = null;
  }
}
