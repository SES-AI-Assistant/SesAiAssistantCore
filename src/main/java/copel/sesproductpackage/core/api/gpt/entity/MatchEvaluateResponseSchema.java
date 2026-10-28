package copel.sesproductpackage.core.api.gpt.entity;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるマッチ評価結果エンティティ（段階B）.
 *
 * <p>段階Bの評価は判定には使わない。マッチ判定は段階A（{@link
 * ChooseBestInfoResponseSchema}）で終えている。段階Bは、必須・尚可・その他の詳細評価の文章と、マッチ登録用の評価文・スコアを作るために使う。
 * 単価・出社・場所・人月工数はベクトル検索のSQLで絞り込み済みのため、本クラスでは評価しない。
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchEvaluateResponseSchema {
  @Schema(
      title = "必須スキル評価",
      description =
          "案件情報に必須要件として明記されている項目の評価結果リスト。案件情報に記載のない項目を推測で追加しないこと。必須要件の記載が無い場合は空リストとする。評価は要員情報に明記された"
              + "内容のみを根拠とし、記載から推測・補完して判断しないこと。",
      itemType = SkillEvaluateResult.class,
      required = true,
      maxItems = 10)
  private List<SkillEvaluateResult> mustList;

  @Schema(
      title = "尚可スキル評価",
      description = "案件情報に尚可要件として明記されている項目の評価結果リスト。案件情報に記載のない項目を推測で追加しないこと。尚可要件の記載が無い場合は空リストとする。",
      itemType = SkillEvaluateResult.class,
      maxItems = 10)
  private List<SkillEvaluateResult> wantList;

  @Schema(
      title = "その他の評価",
      description =
          "単価・場所・出社・人月工数以外で、案件または要員の情報に明記されている条件（年齢、商流、国籍など）に対するマッチ評価結果。明記された条件が無い場合は空リストとする。",
      itemType = OtherEvaluateResult.class,
      maxItems = 10)
  private List<OtherEvaluateResult> otherList;

  /** 必須スキルの配点（満点）。 合計100点のうち、最も重く見る項目に割り当てる。 */
  private static final double MUST_POINTS = 57.0;

  /** 尚可スキルの配点（満点）。 必須の次に重く見る項目に割り当てる。 */
  private static final double WANT_POINTS = 29.0;

  /** その他の評価の配点（満点）。 残りの点数を割り当てる。 */
  private static final double OTHER_POINTS = 14.0;

  // ================================================
  // メソッド
  // ================================================

  /**
   * 各項目の評価結果をもとに、決定論的なロジック演算によってマッチスコア（0〜100点）を算出する.
   *
   * <p><b>■ 配点構造（合計 100点満点）</b>
   *
   * <ul>
   *   <li><b>必須スキル（mustList）：最大 57点</b> <br>
   *       各要素の達成率（FullyMet=1.0, PartiallyMet=0.4, NotMet/Unknown=0.0）の平均 × 57点。 <br>
   *       ※未設定（nullまたは空）の場合は満点（57点）扱い。
   *   <li><b>尚可スキル（wantList）：最大 29点</b> <br>
   *       各要素の達成率の平均 × 29点。 <br>
   *       ※未設定（nullまたは空）の場合は満点（29点）扱い。
   *   <li><b>その他の評価（otherList）：最大 14点</b> <br>
   *       各要素の達成率の平均 × 14点。 <br>
   *       ※未設定（nullまたは空）の場合は満点（14点）扱い。
   * </ul>
   *
   * <p><b>■ 計算例</b>
   *
   * <pre>{@code
   * 【前提条件】
   * - 必須スキル(57点満点): 2件中 1件FullyMet(1.0), 1件PartiallyMet(0.4)
   *     ⇒ 57 * ((1.0 + 0.4) / 2) = 39.9点
   * - 尚可スキル(29点満点): 未設定(null/空)
   *     ⇒ 29.0点（満点）
   * - その他(14点満点): 未設定(null/空)
   *     ⇒ 14.0点（満点）
   *
   * 【合計】 39.9 + 29.0 + 14.0 = 82.9 → 四捨五入で 83点
   * }</pre>
   *
   * @return 算出されたマッチスコア（0〜100点）
   */
  public int getMatchScore() {
    double mustScore = scoreOf(this.mustList, SkillEvaluateResult::getResult, MUST_POINTS);
    double wantScore = scoreOf(this.wantList, SkillEvaluateResult::getResult, WANT_POINTS);
    double otherScore = scoreOf(this.otherList, OtherEvaluateResult::getResult, OTHER_POINTS);
    // 合計点の算出（四捨五入）
    int totalScore = (int) Math.round(mustScore + wantScore + otherScore);
    // 範囲の正規化 (0〜100)
    return Math.max(0, Math.min(100, totalScore));
  }

  /**
   * 評価の一覧を、配点に対する達成率の平均で点数に換算する.
   *
   * <p>一覧が null または空の場合は記載が無いため、満点を返す。各要素の評価が null の場合は達成率を 0 として平均する。
   *
   * @param <T> 評価の要素の型
   * @param items 評価の一覧。null も可
   * @param resultOf 要素から評価区分を取り出す関数
   * @param points 配点（満点）
   * @return 換算した点数
   */
  private static <T> double scoreOf(
      final List<T> items, final Function<T, EvaluateType> resultOf, final double points) {
    if (items == null || items.isEmpty()) {
      return points;
    }
    double sum =
        items.stream()
            .mapToDouble(
                item -> {
                  EvaluateType result = resultOf.apply(item);
                  return result != null ? result.getRate() : 0.0;
                })
            .sum();
    return points * (sum / items.size());
  }

  /**
   * 既定のルールで、段階Bの詳細評価がマッチ条件を満たすかを返却する.
   *
   * <p>既定のルールは必須を FullyMet、尚可を判定しない（null）とする。 {@link #isMatch(FilteringRules)} に null
   * を渡した場合と同じ結果になる。
   *
   * @return マッチしていればtrue、そうでなければfalse.
   */
  public boolean isMatch() {
    return isMatch(null);
  }

  /**
   * フィルタリングルールの必須・尚可のしきい値で、段階Bの詳細評価がマッチ条件を満たすかを返却する.
   *
   * <p>判定は必須と尚可の2つだけで行う。必須は {@link #summarizeSkills(List)} で1つの評価区分にまとめた結果が、しきい値以上かで判定する。尚可は
   * しきい値が設定されている場合だけ同じ方法で判定し、しきい値が null の場合は判定しない。 ルールが null の場合は既定のルール（必須 FullyMet、尚可は判定しない）で判定する。
   *
   * <p>単価・出社・場所・人月工数は段階Bでは評価しないため判定に使わない。その他の評価（otherList）も判定に使わず、スコアと評価文にのみ反映する。 {@link
   * FilteringRules#isOtherConstraintsRequired()} は参照しない（その他制約は段階Aだけで判定する）。
   *
   * @param rules フィルタリングルール。nullの場合は既定のルール
   * @return マッチしていればtrue、そうでなければfalse.
   */
  public boolean isMatch(final FilteringRules rules) {
    FilteringRules applied =
        rules == null ? new FilteringRules(EvaluateType.FullyMet, null, false) : rules;
    // 必須：まとめた評価がしきい値を満たさなければNG（しきい値が null なら判定しない）
    if (applied.getMustSkillLevel() != null
        && !summarizeSkills(this.mustList).satisfies(applied.getMustSkillLevel())) {
      return false;
    }
    // 尚可：まとめた評価がしきい値を満たさなければNG（しきい値が null なら判定しない）
    return applied.getWantSkillLevel() == null
        || summarizeSkills(this.wantList).satisfies(applied.getWantSkillLevel());
  }

  /**
   * スキル評価の一覧を、1つの評価区分にまとめる.
   *
   * <p>一覧が null または空の場合は記載が無いため FullyMet とする。 1件でも FullyMet と PartiallyMet
   * 以外（NotMet、Unknown、評価の値が未設定）があれば NotMet とし、PartiallyMet より優先する。 全件が FullyMet なら
   * FullyMet、それ以外（FullyMet と PartiallyMet の混在）は PartiallyMet とする。
   *
   * @param skills スキル評価の一覧。null も可
   * @return まとめた評価区分
   */
  private static EvaluateType summarizeSkills(final List<SkillEvaluateResult> skills) {
    if (skills == null || skills.isEmpty()) {
      return EvaluateType.FullyMet;
    }
    boolean hasUnmet =
        skills.stream()
            .anyMatch(
                skill ->
                    skill.getResult() != EvaluateType.FullyMet
                        && skill.getResult() != EvaluateType.PartiallyMet);
    if (hasUnmet) {
      return EvaluateType.NotMet;
    }
    boolean allFullyMet =
        skills.stream().allMatch(skill -> skill.getResult() == EvaluateType.FullyMet);
    return allFullyMet ? EvaluateType.FullyMet : EvaluateType.PartiallyMet;
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
            .append(
                must.getResult() != null
                    ? must.getResult().getIcon() + "(" + must.getComment() + ")"
                    : "-")
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
            .append(
                must.getResult() != null
                    ? must.getResult().getIcon() + "(" + must.getComment() + ")"
                    : "-")
            .append("\n");
      }
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
   * 評価結果を通知用の1行サマリーテキストに変換する.
   *
   * <p>プッシュ通知の本文はスマホで表示できる行数が限られるため、マッチング評価の主要な判定項目を1行に並べる。段階Bでは必須スキルだけを評価するため、表示するのも必須スキルだけとする。
   * 必須スキルが評価されていない場合は空文字を返す。
   *
   * @return 評価サマリー（例：「必須◎」）。必須スキルの評価が1つも無い場合は空文字
   */
  public String toNotificationSummary() {
    if (this.mustList == null || this.mustList.isEmpty()) {
      return "";
    }
    // 必須スキルは1件でも完全一致でなければ〇とし、個別の判定は詳細画面で確認させる
    boolean allMet = this.mustList.stream().allMatch(m -> m.result == EvaluateType.FullyMet);
    return "必須" + (allMet ? "◎" : "〇");
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
    // 4. その他
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
  /** スキル評価の結果を表すスキーマです。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SkillEvaluateResult {
    @Schema(
        title = "評価項目",
        description = "案件情報に記載された要求項目。案件情報の表現をそのまま用いること。",
        required = true,
        maxLength = 50,
        example = "時系列データ分析")
    private String perspective;

    @Schema(
        title = "評価結果",
        description =
            "要員情報に明記された実務経験のみで判定する。FullyMet=評価項目と同一のスキル・経験が明記されている。PartiallyMet=同一ではないが、代替可能な類似スキル・"
                + "経験（同一の技術基盤上にあり、記載された経験を要求にそのまま転用できるもの。例: 要求がSpring Bootで記載がSpring "
                + "Framework）が具体的に明記されている。技術基盤が異なる製品・サービス（例: "
                + "要求がAWSで記載がGCP）は類似とみなさない。NotMet=同一・類似いずれの記載も無い。職種や他スキルからの推測（例: "
                + "Javaエンジニアだから SQL も経験があるはず）でFullyMet/PartiallyMetにしてはならない。",
        itemType = EvaluateType.class,
        required = true,
        example = "FullyMet")
    private EvaluateType result;

    @Schema(
        title = "評価コメント",
        description = "判定根拠。FullyMet/PartiallyMetの場合は根拠とした要員情報の記載を30字以内で抜粋し、NotMetの場合は記載が無い旨を書くこと。",
        maxLength = 50,
        example = "スキルシートに『時系列データ分析』の記載がないためNotMet")
    private String comment;
  }

  /** その他条件の評価結果を表すスキーマです。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OtherEvaluateResult {
    @Schema(
        title = "評価項目",
        description = "案件または要員の情報に明記されている条件",
        required = true,
        maxLength = 50,
        example = "成長できる環境であること")
    private String perspective;

    @Schema(
        title = "評価結果",
        description =
            "明記された条件を相手側が満たすかどうか。相手側の情報に明記された内容のみで判定し、推測しないこと。FullyMet=満たすことが明記されている、または相手側に該当する記載が"
                + "無く違反が確認できない。PartiallyMet=一部のみ満たす記載がある。NotMet=条件に反する記載がある。",
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

  /** 評価区分（充足・一部充足・未充足・不明）を表す列挙型です。 */
  public enum EvaluateType {
    FullyMet, // 同一の実務経験が明記されている
    PartiallyMet, // 代替可能な類似の実務経験が明記されている
    NotMet, // 記載なし
    Unknown; // 不明

    /**
     * 評価区分に対応する記号を返します。
     *
     * @return 記号
     */
    public String getIcon() {
      return switch (this) {
        case FullyMet -> "◎";
        case PartiallyMet -> "〇";
        case NotMet -> "×";
        case Unknown -> "-";
      };
    }

    /**
     * 評価区分に対応する充足率を返します。
     *
     * @return 充足率（0.0 から 1.0）
     */
    public double getRate() {
      return switch (this) {
        case FullyMet -> 1.0;
        case PartiallyMet -> 0.4;
        case NotMet -> 0.0;
        case Unknown -> 0.0;
      };
    }

    /**
     * 評価区分がしきい値の評価区分を満たすかを返します。
     *
     * <p>列挙値は満たす度合いの高い順に並んでいる（FullyMet、PartiallyMet、NotMet、Unknown）。 しきい値が FullyMet なら FullyMet
     * だけを満たし、PartiallyMet なら FullyMet と PartiallyMet を満たす。 Unknown は FullyMet と PartiallyMet
     * のどちらのしきい値でも満たさない。
     *
     * @param threshold しきい値の評価区分（null は呼び出し側で判定しない項目として扱う）
     * @return しきい値を満たす場合は true
     */
    public boolean satisfies(final EvaluateType threshold) {
      return this.ordinal() <= threshold.ordinal();
    }
  }
}
