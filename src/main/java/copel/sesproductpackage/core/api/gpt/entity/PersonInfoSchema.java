package copel.sesproductpackage.core.api.gpt.entity;

import java.math.BigDecimal;
import java.util.List;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによる要員要約結果エンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonInfoSchema {
  @Schema(
      title = "氏名",
      description = "紹介されている要員本人の名前またはイニシャル",
      maxLength = 10,
      required = true,
      example = "T.T")
  private String name;

  @Schema(title = "年齢", description = "紹介されている要員本人の年齢。不明な場合は-1", gt = 17, lt = 100, example = "35")
  private int age = -1;

  @Schema(
      title = "性別",
      description = "紹介されている要員本人の性別",
      itemType = Gender.class,
      required = true,
      example = "Man")
  private Gender gender;

  @Schema(
      title = "国籍",
      description = "紹介されている要員本人の国籍。未記載の場合は「日本」とする）",
      maxLength = 10,
      required = true,
      example = "日本")
  private String nationality = "日本";

  @Schema(title = "開始年月", description = "稼働開始が可能な年月。必ずyyyy/MM形式で設定してください。未記載の場合や即日、などの表現がされている場合は本日日付にして。", pattern = "^\\d{4}/(0?[1-9]|1[0-2])$", required = true, example = "2026/6", currentAndNextYearOnly = true)
  private String startYearMonth;

  @Schema(
      title = "希望単価（万円）",
      description =
          "月額希望単価を万円単位の数値で設定。例: 80万/800,000円/800K→80、65.5万→65.5、時給2500円(160h)→40。幅がある場合は希望下限。スキル見合い・未記載は999。",
      required = true,
      type = "number",
      example = "80")
  private BigDecimal priceInMan;

  /**
   * 希望単価を Money 値オブジェクト（円単位）で取得します.
   *
   * @return 円単位の Money インスタンス
   */
  public Money getPrice() {
    return Money.toSesUnitFromMan(this.priceInMan);
  }

  @Schema(title = "場所", description = "要員の在住地域や最寄駅名など", maxLength = 20, example = "品川")
  private String place = null;

  @Schema(title = "地域", description = "要員の在住地域や最寄駅が属する地域。判別が難しい場合や未記載の場合は関東_首都圏とする", example = "関東_首都圏")
  private Area area = Area.関東_首都圏;

  @Schema(
      title = "出社可能頻度",
      description =
          "1週間あたり出社可能な頻度。フルリモート希望の場合は「0」、常駐可能や未記載の場合は「5」とする。週1～2など幅がある場合は最も多い数（この場合は「2」）を記入する。",
      required = true,
      gt = -1,
      lt = 6,
      example = "1")
  private int officeAvailability;

  @Schema(title = "所属", description = "商流や所属。不明な場合は「未記載」とする。", required = true, maxLength = 20, example = "1社先正社員")
  private String organization = "未記載";

  @Schema(
	title = "経歴", 
	description = "箇条書き形式の経歴のリスト", 
	itemType = Experience.class,
    required = true,
    minItems = 1,
    maxItems = 10)
  private List<Experience> experiences = null;

  @Schema(
	title = "NG条件", 
	description = "要員都合のNG条件のリスト", 
	itemType = String.class,
    required = true,
    minItems = 0,
    maxItems = 10)
  private List<String> ngRequirements = null;

  @Schema(title = "その他", description = "その他、備考などの事項", itemType = String.class, minItems = 0, maxItems = 10)
  private List<String> otherRequirements = null;

  @Schema(
      title = "URL",
      description = "経歴書やスキルシートのURL。未記載の場合は設定しない。",
      maxLength = 300,
      format = "uri",
      pattern = "^https?://.+",
      example = "https://www.google.com")
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
    if (this.startYearMonth != null) {
      sb.append("■開始: ").append(this.startYearMonth.replace("/", "年")).append("月\n");
    }
    // 5. 単価
    sb.append("■単価: ").append(this.getPrice()).append("\n");
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
        sb.append("・")
            .append(experience.getPerspective())
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

  /**
   * 開始年月から OriginalDateTime を生成します.
   *
   * @return OriginalDateTime
   */
  public OriginalDateTime getStartDateAsOriginalDateTime() {
    if (this.startYearMonth == null) {
      OriginalDateTime now = new OriginalDateTime();
      return OriginalDateTime.fromMonth(now.toLocalDate().getYear(), now.toLocalDate().getMonthValue());
    }
    String[] parts = this.startYearMonth.split("/");
    if (parts.length == 2) {
      try {
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        return OriginalDateTime.fromMonth(year, month);
      } catch (NumberFormatException e) {
        OriginalDateTime now = new OriginalDateTime();
        return OriginalDateTime.fromMonth(now.toLocalDate().getYear(), now.toLocalDate().getMonthValue());
      }
    }
    OriginalDateTime now = new OriginalDateTime();
    return OriginalDateTime.fromMonth(now.toLocalDate().getYear(), now.toLocalDate().getMonthValue());
  }

  /**
   * 経歴リストを改行区切りの文字列に変換します.
   *
   * @return 改行区切りの文字列、またはnull
   */
  public String getExperiencesAsString() {
    if (this.experiences == null || this.experiences.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (Experience exp : this.experiences) {
      if (sb.length() > 0) {
        sb.append("\n");
      }
      sb.append(exp.getPerspective());
      if (exp.getDuration() != null) {
        sb.append(": ").append(exp.getDuration());
      }
    }
    return sb.toString();
  }

  // ================================================
  // フィールド用のクラス
  // ================================================
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Experience {
    @Schema(
        title = "スキル",
        description = "経験、スキル、観点など",
        maxLength = 30,
        required = true,
        example = "SpringBootによるWEBアプリ開発")
    private String perspective;

    @Schema(
        title = "経験年数",
        description =
            "経験年数や期間（例: 「3年」）。本文中に明確な年数や期間の記載がない場合は、推測せず必ずnullにすること。（「不明」「未記載」「なし」などの文字列は絶対に設定しないこと）",
        maxLength = 30,
        example = "3年")
    private String duration = null;
  }
}
