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
   * 各項目の評価結果をもとに、決定論的なロジック演算によってマッチスコア（0〜100点）を算出する.
   *
   * <p><b>■ 配点構造（合計 100点満点）</b>
   * <ul>
   *   <li><b>必須スキル（mustList）：最大 40点</b>
   *       <br>各要素の達成率（FullyMet=1.0, PartiallyMet=0.4, NotMet/Unknown=0.0）の平均 × 40点。
   *       <br>※未設定（nullまたは空）の場合は満点（40点）扱い。</li>
   *   <li><b>条件面評価：最大 30点</b>
   *       <ul>
   *         <li>単価（priceResult）：適合で 10点 / 不可で 0点</li>
   *         <li>出社要件（officeResult）：FullyMet=10点 / PartiallyMet=4点 / NotMet=0点</li>
   *         <li>場所（placeResult）：FullyMet=5点 / PartiallyMet=2点 / NotMet=0点（※null時は5点）</li>
   *         <li>人月工数（personMonthsResult）：適合で 5点 / 不可で 0点（※null時は5点）</li>
   *       </ul>
   *   </li>
   *   <li><b>尚可スキル（wantList）：最大 20点</b>
   *       <br>各要素の達成率の平均 × 20点。
   *       <br>※未設定（nullまたは空）の場合は満点（20点）扱い。</li>
   *   <li><b>その他の評価（otherList）：最大 10点</b>
   *       <br>各要素の達成率の平均 × 10点。
   *       <br>※未設定（nullまたは空）の場合は満点（10点）扱い。</li>
   * </ul>
   *
   * <p><b>■ 計算例</b>
   * <pre>{@code
   * 【前提条件】
   * - isMatch() == true
   * - 必須スキル(40点満点): 2件中 1件FullyMet(1.0), 1件PartiallyMet(0.4)
   *     ⇒ 40 * ((1.0 + 0.4) / 2) = 28.0点
   * - 条件面(30点満点): 単価OK(10点), 出社FullyMet(10点), 場所FullyMet(5点), 工数OK(5点)
   *     ⇒ 10 + 10 + 5 + 5 = 30.0点
   * - 尚可スキル(20点満点): 未設定(null/空)
   *     ⇒ 20.0点（満点）
   * - その他(10点満点): 未設定(null/空)
   *     ⇒ 10.0点（満点）
   *
   * 【合計】 28.0 + 30.0 + 20.0 + 10.0 = 88点
   * }</pre>
   *
   * @return 算出されたマッチスコア（0〜100点）
   */
  public int getMatchScore() {
    // 1. 必須スキルスコア（最大 40点 / null・空は40点満点）
    double mustScore = 40.0;
    if (this.mustList != null && !this.mustList.isEmpty()) {
      double sum = this.mustList.stream()
          .mapToDouble(m -> m.getResult() != null ? m.getResult().getRate() : 0.0)
          .sum();
      mustScore = 40.0 * (sum / this.mustList.size());
    }
    // 2. 条件面スコア（最大 30点）
    double conditionScore = 0.0;
    // 単価 (10点)
    if (this.priceResult != null && this.priceResult.isResult()) {
      conditionScore += 10.0;
    }
    // 出社要件 (10点)
    if (this.officeResult != null && this.officeResult.getResult() != null) {
      conditionScore += 10.0 * this.officeResult.getResult().getRate();
    }
    // 場所 (5点)
    if (this.placeResult == null) {
      conditionScore += 5.0;
    } else if (this.placeResult.getResult() != null) {
      conditionScore += 5.0 * this.placeResult.getResult().getRate();
    }
    // 人月工数 (5点)
    if (this.personMonthsResult == null || this.personMonthsResult.isResult()) {
      conditionScore += 5.0;
    }
    // 3. 尚可スキルスコア（最大 20点 / null・空は20点満点）
    double wantScore = 20.0;
    if (this.wantList != null && !this.wantList.isEmpty()) {
      double sum = this.wantList.stream()
          .mapToDouble(w -> w.getResult() != null ? w.getResult().getRate() : 0.0)
          .sum();
      wantScore = 20.0 * (sum / this.wantList.size());
    }
    // 4. その他スコア（最大 10点 / null・空は10点満点）
    double otherScore = 10.0;
    if (this.otherList != null && !this.otherList.isEmpty()) {
      double sum = this.otherList.stream()
          .mapToDouble(o -> o.getResult() != null ? o.getResult().getRate() : 0.0)
          .sum();
      otherScore = 10.0 * (sum / this.otherList.size());
    }
    // 合計点の算出（四捨五入）
    int totalScore = (int) Math.round(mustScore + conditionScore + wantScore + otherScore);
    // 範囲の正規化 (0〜100)
    totalScore = Math.max(0, Math.min(100, totalScore));
    return totalScore;
  }

  /**
   * 必須スキルの評価結果をテキスト形式で取得する.
   *
   * @return 必須スキル評価結果
   */
  public String getMustEvaluationText() {
    StringBuilder sb = new StringBuilder();
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
    return sb.toString();
  }

  /**
   * 尚可スキルの評価結果をテキスト形式で取得する.
   *
   * @return 尚可スキル評価結果
   */
  public String getWantEvaluationText() {
    StringBuilder sb = new StringBuilder();
    if (this.wantList != null && !this.wantList.isEmpty()) {
      sb.append("■尚可\n");
      for (SkillEvaluateResult must : this.wantList) {
        sb.append("・")
            .append(must.getPerspective())
            .append("：")
            .append(must.getResult() != null ? (must.getResult().getIcon() + "(" + must.getComment() + ")") : "-")
            .append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * 場所の評価結果をテキスト形式で取得する.
   *
   * @return 場所の評価結果.
   */
  public String getPlaceEvaluationText() {
    StringBuilder sb = new StringBuilder();
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
    return sb.toString();
  }

  /**
   * 出社要件の評価結果をテキスト形式で取得する.
   *
   * @return 出社要件の評価結果.
   */
  public String getOfficeEvaluationText() {
    StringBuilder sb = new StringBuilder();
    if (this.officeResult != null) {
      String icon =
          this.officeResult.getResult() != null ? this.officeResult.getResult().getIcon() : "-";
      sb.append("■出社要件：").append(icon);
      if (!OriginalStringUtils.isEmpty(this.officeResult.getComment())) {
        sb.append("（").append(this.officeResult.getComment()).append("）");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * その他条件の評価結果をテキスト形式で取得する.
   *
   * @return その他条件の評価結果.
   */
  public String getOtherEvaluationText() {
    StringBuilder sb = new StringBuilder();
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
    return sb.toString();
  }

  /**
   * 評価結果を通知用のサマリーテキストに変換する.
   *
   * プッシュ通知のペイロード制限（iOS/Android 各 4KB）の中で、マッチング評価の主要な判定項目を
   * コンパクトに表示するために、必須スキル・条件面・尚可スキル・その他条件をアイコンと
   * 簡潔なテキストで表現する.
   *
   * @return 評価サマリー（複数行のテキスト、各行は「・項目名：判定アイコン」形式）
   */
  public String toNotificationSummary() {
    StringBuilder sb = new StringBuilder();

    // 必須スキル評価サマリー
    if (this.mustList != null && !this.mustList.isEmpty()) {
      boolean allMet =
          this.mustList.stream()
              .allMatch(m -> m.result == EvaluateType.FullyMet);
      sb.append("・必須スキル：").append(allMet ? "◎" : "〇").append("\n");
    }

    // 単価評価
    if (this.priceResult != null) {
      sb.append("・単価：").append(this.priceResult.result ? "◎" : "×").append("\n");
    }

    // 出社要件
    if (this.officeResult != null) {
      String icon =
          this.officeResult.result != null
              ? this.officeResult.result.getIcon()
              : "-";
      sb.append("・出社要件：").append(icon).append("\n");
    }

    // 場所
    if (this.placeResult != null) {
      String icon =
          this.placeResult.result != null
              ? this.placeResult.result.getIcon()
              : "◎";
      sb.append("・場所：").append(icon).append("\n");
    }

    // 尚可スキル（簡略版）
    if (this.wantList != null && !this.wantList.isEmpty()) {
      long metCount =
          this.wantList.stream()
              .filter(w -> w.result == EvaluateType.FullyMet)
              .count();
      sb.append("・尚可スキル：").append(metCount).append("/").append(this.wantList.size()).append("\n");
    }

    return sb.toString();
  }

  /**
   * マッチング詳細テーブルの評価文カラムにセットする文字列に変換する. 最大2000文字.
   *
   * @return 評価文
   */
  public String toEvaluiationText() {
    StringBuilder sb = new StringBuilder();
    // 1. マッチ度
    sb.append("マッチ度：").append(this.getMatchScore()).append("点\n");
    // 2. 必須項目
    sb.append(this.getMustEvaluationText());
    // 3. 尚可項目
    sb.append(this.getWantEvaluationText());
    // 4. 出社要件
    sb.append(this.getOfficeEvaluationText());
    // 5. 場所
    sb.append(this.getPlaceEvaluationText());
    // 6. 人月工数（必要に応じて出力）
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
    sb.append(this.getOtherEvaluationText());
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
        description = "案件が求める評価項目。スキル評価は『記載された実務経験』のみを対象。",
        required = true,
        maxLength = 50,
        example = "時系列データ分析")
    private String perspective;

    @Schema(
        title = "評価結果",
        description = "FullyMet=記載あり、PartiallyMet=関連スキルのみ、NotMet=記載なし",
        itemType = EvaluateType.class,
        required = true,
        example = "FullyMet")
    private EvaluateType result;

    @Schema(
        title = "評価コメント",
        description = "判定根拠",
        maxLength = 50,
        example = "スキルシートに『時系列データ分析』の記載がないためNotMet")
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
    FullyMet,      // 記載された実務経験あり
    PartiallyMet,  // 関連スキルのみ（記載スキルと異なる）
    NotMet,        // 記載なし
    Unknown;       // 不明

    public String getIcon() {
      return switch (this) {
        case FullyMet -> "◎";
        case PartiallyMet -> "〇";
        case NotMet -> "×";
        case Unknown -> "-";
      };
    }

    public double getRate() {
      return switch (this) {
        case FullyMet -> 1.0;
        case PartiallyMet -> 0.4;
        case NotMet -> 0.0;
        case Unknown -> 0.0;
      };
    }
  }
}
