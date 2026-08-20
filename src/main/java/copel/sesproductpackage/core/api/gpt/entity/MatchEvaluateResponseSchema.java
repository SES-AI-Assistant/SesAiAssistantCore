package copel.sesproductpackage.core.api.gpt.entity;

import java.util.List;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるマッチ評価結果エンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchEvaluateResponseSchema {
  @Schema(
      title = "マッチ度（点）",
      description = "要員と案件のマッチ度合いを示した数値",
      required = true,
      gt = -1,
      lt = 100,
      example = "85")
  private int matchScore;

  @Schema(
      title = "必須スキル評価",
      description = "案件の必須要求項目の評価結果リスト",
      itemType = SkillEvaluateResult.class,
      required = true,
      minItems = 1,
      maxItems = 10)
  private List<SkillEvaluateResult> mustList;

  @Schema(
      title = "尚可スキル評価",
      description = "案件の尚可要求項目の評価結果リスト",
      itemType = SkillEvaluateResult.class,
      minItems = 1,
      maxItems = 10)
  private List<SkillEvaluateResult> wantList;

  @Schema(
      title = "単価評価",
      description = "案件の単価が要員の希望単価を超えているかどうかの評価結果",
      required = true,
      itemType = PriceEvaluateResult.class)
  private PriceEvaluateResult priceResult;

  @Schema(
      title = "場所評価",
      description = "案件の場所と要員の場所がマッチしているかどうかの評価結果",
      required = true,
      itemType = PlaceEvaluateResult.class)
  private PlaceEvaluateResult placeResult;

  @Schema(
      title = "出社要件評価",
      description = "案件の求める出社要件と要員出社許容条件がマッチしているかどうかの評価結果",
      required = true,
      itemType = OfficeEvaluateResult.class)
  private OfficeEvaluateResult officeResult;

  @Schema(
      title = "人月工数評価",
      description = "案件の求める人月工数と要員の稼働可能人月工数がマッチしているかどうかの評価結果。案件または要員に人月工数に関する記載がある場合のみ設定する項目。",
      itemType = PersonMonthsEvaluateResult.class)
  private PersonMonthsEvaluateResult personMonthsResult;

  @Schema(
      title = "その他の評価",
      description = "その他の案件、要員が求める条件に対するマッチ評価結果",
      itemType = OtherEvaluateResult.class,
      minItems = 1,
      maxItems = 10)
  private List<OtherEvaluateResult> otherList;

  // ================================================
  // メソッド
  // ================================================
  /**
   * 評価結果を元に、マッチするかどうかを返却する.
   *
   * @return マッチしていればtrue、そうでなければfalse.
   */
  public boolean isMatch() {
    // 1. 必須オブジェクトの存在チェック（事前条件）
    if (this.mustList == null || this.priceResult == null || this.officeResult == null) {
      return false;
    }
    // 2. 必須条件：1つでも FullyMet 以外があればNG
    boolean isAllMustMet =
        this.mustList.stream().allMatch(must -> must.getResult() == EvaluateType.FullyMet);
    if (!isAllMustMet) {
      return false;
    }
    // 3. 案件単価要件
    if (!this.priceResult.isResult()) {
      return false;
    }
    // 4. 出社要件
    if (this.officeResult.getResult() != EvaluateType.FullyMet) {
      return false;
    }
    // 5. 場所要件（存在し、かつ NotMet の場合はNG）
    if (this.placeResult != null && this.placeResult.getResult() == EvaluateType.NotMet) {
      return false;
    }
    // 6. 人月工数要件（存在し、かつ満たさない場合はNG）
    if (this.personMonthsResult != null && !this.personMonthsResult.isResult()) {
      return false;
    }
    return true;
  }

  /**
   * マッチング詳細テーブルの評価文カラムにセットする文字列に変換する. 最大2000文字.
   *
   * @return 評価文
   */
  public String toEvaluiationText() {
    StringBuilder sb = new StringBuilder();
    // 1. マッチ度
    sb.append("マッチ度：").append(this.matchScore).append("点\n");
    // 2. 必須項目
    if (this.mustList != null && !this.mustList.isEmpty()) {
      sb.append("■必須\n");
      for (SkillEvaluateResult must : this.mustList) {
        sb.append("・")
            .append(must.getPerspective())
            .append("：")
            .append(must.getResult() != null ? (must.getResult().getIcon() + "(" + must.getComment() + ")") : "-")
            .append("\n");
      }
    }
    // 3. 尚可項目
    if (this.wantList != null && !this.wantList.isEmpty()) {
      sb.append("■尚可\n");
      for (SkillEvaluateResult want : this.wantList) {
        sb.append("・")
            .append(want.getPerspective())
            .append("：")
            .append(want.getResult() != null ? (want.getResult().getIcon() + "(" + want.getComment() + ")") : "-")
            .append("\n");
      }
    }
    // 4. 出社要件
    if (this.officeResult != null) {
      String icon =
          this.officeResult.getResult() != null ? this.officeResult.getResult().getIcon() : "-";
      sb.append("■出社要件：").append(icon);
      if (!OriginalStringUtils.isEmpty(this.officeResult.getComment())) {
        sb.append("（").append(this.officeResult.getComment()).append("）");
      }
      sb.append("\n");
    }
    // 5. 場所
    if (this.placeResult != null) {
      String icon =
          this.placeResult.getResult() != null ? this.placeResult.getResult().getIcon() : "-";
      sb.append("■場所：");
      if (!OriginalStringUtils.isEmpty(this.placeResult.getComment())) {
        sb.append(this.placeResult.getComment());
      } else {
        sb.append(icon);
      }
      sb.append("\n");
    }
    // 6. 単価（必要に応じて出力）
    if (this.priceResult != null && !OriginalStringUtils.isEmpty(this.priceResult.getComment())) {
      String icon = this.priceResult.isResult() ? "○" : "×";
      sb.append("■単価：")
          .append(icon)
          .append("（")
          .append(this.priceResult.getComment())
          .append("）\n");
    }
    // 7. 人月工数（必要に応じて出力）
    if (this.personMonthsResult != null
        && !OriginalStringUtils.isEmpty(this.personMonthsResult.getComment())) {
      String icon = this.personMonthsResult.isResult() ? "○" : "×";
      sb.append("■人月工数：")
          .append(icon)
          .append("（")
          .append(this.personMonthsResult.getComment())
          .append("）\n");
    }
    // 8. その他
    if (this.otherList != null && !this.otherList.isEmpty()) {
      sb.append("■その他：\n");
      for (OtherEvaluateResult other : this.otherList) {
        String icon = other.getResult() != null ? other.getResult().getIcon() : "-";
        sb.append("・").append(other.getPerspective()).append("：").append(icon);
        if (!OriginalStringUtils.isEmpty(other.getComment())) {
          sb.append("（").append(other.getComment()).append("）");
        }
        sb.append("\n");
      }
    }
    // 2000文字を超える場合は安全にカット（DB制約対策）
    String resultText = sb.toString().trim();
    if (resultText.length() > 2000) {
      return resultText.substring(0, 2000);
    }
    return resultText;
  }

  // ================================================
  // 各フィールド用のクラス
  // ================================================
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SkillEvaluateResult {
    @Schema(
        title = "評価項目",
        description = "案件が求める評価項目",
        required = true,
        maxLength = 50,
        example = "Javaの経験が5年以上であること")
    private String perspective;

    @Schema(
        title = "評価結果",
        description = "案件が求める項目を要員が満たすかどうかを3段階で表現した評価結果",
        itemType = EvaluateType.class,
        required = true,
        example = "FullyMet")
    private EvaluateType result;

    @Schema(
        title = "評価コメント",
        description = "この評価項目結果に対するコメント",
        maxLength = 50,
        example = "案件が求めるJava5年に対し要員はJava10年経験あり")
    private String comment;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PriceEvaluateResult {
    @Schema(
        title = "評価結果",
        description = "案件単価 >= 要員単価であればtrue、それ以外はfalse。案件側がスキル見合いである場合は一律true",
        required = true,
        example = "true")
    private boolean result;

    @Schema(
        title = "評価コメント",
        description = "この評価項目結果に対するコメント",
        maxLength = 30,
        example = "案件単価60万に対し要員単価は55万")
    private String comment;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlaceEvaluateResult {
    @Schema(
        title = "評価結果",
        description =
            "案件場所から要員の場所まで在来線1時間以内で通勤できるかどうか。案件側がフルリモートである場合、または案件や要員どちらかの場所が不明な場合は一律FullyMetとする。",
        itemType = EvaluateType.class,
        required = true,
        example = "FullyMet")
    private EvaluateType result;

    @Schema(
        title = "評価コメント",
        description = "評価項目結果に対するコメント",
        maxLength = 30,
        example = "案件場所は品川、要員場所は蒲田駅で電車30分程度で可能範囲内")
    private String comment;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OfficeEvaluateResult {
    @Schema(
        title = "評価結果",
        description = "案件の求める出社要件を要員の出社許容条件が満たしているかどうか。案件または要員側に希望や要件が未記載の場合は一律FullyMetとする。",
        itemType = EvaluateType.class,
        required = true,
        example = "FullyMet")
    private EvaluateType result;

    @Schema(
        title = "評価コメント",
        description = "この評価項目結果に対するコメント",
        maxLength = 30,
        example = "案件は週1出社必須で、要員は週2まで出社可能")
    private String comment;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PersonMonthsEvaluateResult {
    @Schema(
        title = "評価結果",
        description = "要員の稼働可能人月工数 >= 案件の求める人月工数であればtrue、それ以外はfalse。案件の求める人月工数が不明の場合は一律trueとする。",
        required = true,
        example = "true")
    private boolean result;

    @Schema(
        title = "評価コメント",
        description = "この評価項目結果に対するコメント",
        maxLength = 50,
        example = "案件は稼働率0.5人月であり、要員は0.3～1.0人月の稼働が可能である")
    private String comment;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OtherEvaluateResult {
    @Schema(
        title = "評価項目",
        description = "案件または要員が求める評価項目",
        required = true,
        maxLength = 50,
        example = "成長できる環境であること")
    private String perspective;

    @Schema(
        title = "評価結果",
        description = "案件または要員が求める項目を案件または要員が満たすかどうかを3段階で表現した評価結果",
        itemType = EvaluateType.class,
        required = true,
        example = "FullyMet")
    private EvaluateType result;

    @Schema(
        title = "評価コメント",
        description = "この評価項目結果に対するコメント",
        maxLength = 50,
        example = "非常にハイスキルかつ裁量が広く成長を期待できる案件である")
    private String comment;
  }

  public static enum EvaluateType {
    FullyMet, // 完全に満たす
    PartiallyMet, // 一部満たす
    NotMet, // 満たさない
    Unknown; // 不明

    public String getIcon() {
      return switch (this) {
        case FullyMet -> "◎";
        case PartiallyMet -> "〇";
        case NotMet -> "×";
        case Unknown -> "-";
      };
    }
  }
}
