package copel.sesproductpackage.core.api.gpt.schema.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * スキルシートの要約文を組み立てるクラス. スキルの経験と直近の案件の内訳を、1000文字以内の要約文にする。
 *
 * <p>1000文字を超える場合は、行の途中では切らず、次の順に行を末尾から1行ずつ落とす。 (1) 直近の案件の内訳（全行が落ちたら見出しも出さない） (2)
 * スキルの経験（月数の少ない行から）。
 *
 * @author Copel Co., Ltd.
 */
final class SkillsheetSummary {

  /** 要約文の上限の文字数。AI の入力と DB の列の上限に合わせる。 */
  private static final int MAX_LENGTH = 1000;

  /** 要約文に載せる直近の案件の件数の上限。 */
  private static final int RECENT_DETAIL_LIMIT = 5;

  /** スキルの見出し。 */
  private static final String SKILL_HEADING = "■スキル・経験";

  /** 直近の案件の見出し。 */
  private static final String DETAIL_HEADING = "■直近のPJ経験";

  /** スキルの経験の行。月数の多い順に並ぶ。 */
  private final List<SkillExperience> skills;

  /** 直近の案件の内訳の行。新しい順に並ぶ。 */
  private final List<ProjectDetail> details;

  /**
   * コンストラクタ.
   *
   * @param skills スキルの経験の行
   * @param details 直近の案件の内訳の行
   */
  SkillsheetSummary(final List<SkillExperience> skills, final List<ProjectDetail> details) {
    this.skills = List.copyOf(skills);
    this.details = List.copyOf(details);
  }

  /**
   * 案件の一覧から、要約文の元になる行を作ります.
   *
   * @param projects 集計済みの案件の一覧
   * @return 要約文の元になる行
   */
  static SkillsheetSummary of(final ProjectExperienceList projects) {
    return new SkillsheetSummary(
        projects.skillExperiencesByMonths(), projects.recentDetails(RECENT_DETAIL_LIMIT));
  }

  /**
   * 1000文字以内の要約文を返します.
   *
   * <p>行を組み立てて長さを確かめ、超えていれば末尾の行を1つ落として組み立て直す。 行が無くなれば空文字になり、必ず上限以内で止まる。
   *
   * @return 要約文。行が無い場合は空文字
   */
  String toText() {
    List<String> lines = new ArrayList<>();
    if (!this.skills.isEmpty()) {
      lines.add(SKILL_HEADING);
      for (SkillExperience skill : this.skills) {
        lines.add(skill.toSummaryLine());
      }
    }
    if (!this.details.isEmpty()) {
      lines.add(DETAIL_HEADING);
      for (ProjectDetail detail : this.details) {
        lines.add(detail.toSummaryLine());
      }
    }
    String text = String.join("\n", lines);
    return text.length() > MAX_LENGTH ? withoutLastLine().toText() : text;
  }

  /**
   * 末尾の行を1つ落とした要約の元を返します.
   *
   * <p>直近の案件の内訳を先に落とし、内訳が無くなってからスキルの経験を落とす。 呼び出し側は、要約文が上限を超えて行が1つ以上ある場合だけ呼ぶ。
   *
   * @return 末尾の行を1つ落とした要約の元
   */
  SkillsheetSummary withoutLastLine() {
    if (!this.details.isEmpty()) {
      return new SkillsheetSummary(this.skills, this.details.subList(0, this.details.size() - 1));
    }
    return new SkillsheetSummary(this.skills.subList(0, this.skills.size() - 1), this.details);
  }
}
