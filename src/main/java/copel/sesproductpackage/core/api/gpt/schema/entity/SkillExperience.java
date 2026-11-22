package copel.sesproductpackage.core.api.gpt.schema.entity;

import copel.sesproductpackage.core.unit.ExperienceDuration;
import copel.sesproductpackage.core.unit.ProjectPeriod;
import copel.sesproductpackage.core.unit.ProjectPeriodList;
import copel.sesproductpackage.core.unit.SkillName;

/**
 * スキル1つ分の経験を表すクラス. そのスキルを使った案件の期間から、重なりを除いた経験月数を求める。
 *
 * @author Copel Co., Ltd.
 */
final class SkillExperience {

  /** スキル名。表示名は最初に出てきた表記。 */
  private final SkillName name;

  /** このスキルを使った案件の期間の一覧。 */
  private final ProjectPeriodList periods = new ProjectPeriodList();

  /**
   * コンストラクタ.
   *
   * @param name スキル名
   */
  SkillExperience(final SkillName name) {
    this.name = name;
  }

  /**
   * このスキルを使った案件の期間を追加します.
   *
   * @param period 案件期間
   */
  void addPeriod(final ProjectPeriod period) {
    this.periods.add(period);
  }

  /**
   * 経験月数を返します.
   *
   * <p>案件の期間が重なる月は1回だけ数える。同じ期間を2回使っても、経験は1か月分である。
   *
   * @return 経験月数
   */
  int months() {
    return this.periods.unionMonths();
  }

  /**
   * 要約文の1行を返します.
   *
   * <p>「・Java: 1年6ヶ月」の形式にする。月数の表記は {@link ExperienceDuration} の整形に従う。
   *
   * @return 要約文の1行
   */
  String toSummaryLine() {
    return "・" + this.name.displayName() + new ExperienceDuration(months()).toSuffix(": ");
  }
}
