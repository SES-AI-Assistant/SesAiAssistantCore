package copel.sesproductpackage.core.api.gpt.schema.entity;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるスキルシート要約結果エンティティ.
 *
 * <p>AIは案件ごとに開始年月、終了年月、使用スキルを抽出するだけで、月数は算出しない。 月数の集計と要約文の組み立ては {@link #toSummaryText()} で Java が行う。
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillsheetInfoSchema {

  @Schema(
      title = "PJ経験一覧",
      description = "スキルシートに明記された案件（プロジェクト）を1件ずつ抽出する。案件ごとに開始、終了、スキルを抽出し、月数は算出しない。記載が無い場合は空リストとする。",
      itemType = ProjectExperience.class,
      maxItems = 50)
  private List<ProjectExperience> projectExperiences = null;

  // ================================================
  // メソッド
  // ================================================
  /**
   * 案件概要文に変換します.
   *
   * <p>案件ごとの期間から、スキルごとに重なりを除いた月数を集計し、月数の多い順に並べる。 直近の案件の内訳は、終了年月の新しい順に先頭5件を載せる。
   * 集計できない案件（開始または終了が解釈できない、終了が開始より前など）は除外する。 要約文は1000文字以内に収め、超える場合は内訳の行、スキルの行の順に末尾から落とす。
   *
   * @return 案件概要文。案件が無い場合は空文字
   */
  public String toSummaryText() {
    return SkillsheetSummary.of(new ProjectExperienceList(this.projectExperiences)).toText();
  }

  // ================================================
  // フィールド用のクラス
  // ================================================
  /** スキルシートに記載された案件（プロジェクト）の経験を表すスキーマです。 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectExperience {
    @Schema(
        title = "開始年月",
        description =
            "案件の開始年月（「YYYY年M月」形式）。「8月」のように月しか記載がない場合は適切な年を補完すること。"
                + "「現在」などの記載は、説明文の末尾の本日の日付を基に、その年月を「YYYY年M月」形式で設定すること。",
        required = true,
        pattern = "^[0-9]{4}年([1-9]|1[0-2])月$",
        todayInDescription = true,
        example = "2023年8月")
    private String startMonth;

    @Schema(
        title = "終了年月",
        description =
            "案件の終了年月（「YYYY年M月」形式）。「8月」のように月しか記載がない場合は適切な年を補完すること。"
                + "「現在」などの記載は、説明文の末尾の本日の日付を基に、その年月を「YYYY年M月」形式で設定すること。",
        required = true,
        pattern = "^[0-9]{4}年([1-9]|1[0-2])月$",
        todayInDescription = true,
        example = "2023年11月")
    private String endMonth;

    @Schema(
        title = "使用スキル",
        description = "この案件で使用されたスキルの名前のリスト。スキル名は記載どおりに抽出し、言い換えたり一般化したりしないこと。",
        itemType = String.class,
        maxItems = 15,
        itemMaxLength = 30)
    private List<String> skills;

    @Schema(
        title = "内容",
        description = "案件内容",
        maxLength = 50,
        required = true,
        example = "大規模通信システムにおいてSEとして20人規模の基本設計工程のとりまとめ、設計レビューを実施")
    private String perspective;
  }
}
