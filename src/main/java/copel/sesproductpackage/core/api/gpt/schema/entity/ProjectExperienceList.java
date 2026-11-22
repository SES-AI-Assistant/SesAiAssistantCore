package copel.sesproductpackage.core.api.gpt.schema.entity;

import copel.sesproductpackage.core.unit.ProjectPeriod;
import copel.sesproductpackage.core.unit.SkillName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * AIが案件ごとに抽出した経験の一覧を、集計して表すクラス.
 *
 * <p>案件は、開始年月と終了年月が「YYYY年M月」として解釈でき、終了が開始より前でない場合だけ集計する。 集計できない案件は WARN
 * ログを出して除外し、内訳にも含めない。例外で登録全体を失敗させないためである。
 *
 * <p>スキルは、案件の期間の和集合の月数で集計する。同じ案件の期間が重なる月は1回だけ数える。
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
final class ProjectExperienceList {

  /** 直近の案件の順。終了年月の新しい順、同じなら開始年月の新しい順。 */
  private static final Comparator<ProjectDetail> RECENT_FIRST =
      Comparator.comparing((ProjectDetail detail) -> detail.period().end())
          .thenComparing(detail -> detail.period().start())
          .reversed();

  /** 集計に使った案件の内訳。入力の案件の並びどおりに並ぶ。 */
  private final List<ProjectDetail> details = new ArrayList<>();

  /** スキルごとの経験。キーは同一判定のキーで、最初に出てきた順に並ぶ。 */
  private final Map<String, SkillExperience> skills = new LinkedHashMap<>();

  /**
   * コンストラクタ. 案件の一覧を集計します.
   *
   * <p>案件の一覧が null の場合は、案件が無いものとして扱う。
   *
   * @param projects AIが抽出した案件の一覧
   */
  ProjectExperienceList(final List<SkillsheetInfoSchema.ProjectExperience> projects) {
    if (projects == null) {
      return;
    }
    for (SkillsheetInfoSchema.ProjectExperience project : projects) {
      Optional<ProjectPeriod> period =
          ProjectPeriod.of(project.getStartMonth(), project.getEndMonth());
      if (period.isEmpty()) {
        log.warn(
            "スキルシートの案件を集計から除外しました。開始年月: {}, 終了年月: {}",
            project.getStartMonth(),
            project.getEndMonth());
        continue;
      }
      this.details.add(new ProjectDetail(period.get(), project.getPerspective()));
      List<String> names = project.getSkills() == null ? List.of() : project.getSkills();
      for (String rawName : names) {
        SkillName.of(rawName)
            .ifPresent(
                name ->
                    this.skills
                        .computeIfAbsent(name.key(), key -> new SkillExperience(name))
                        .addPeriod(period.get()));
      }
    }
  }

  /**
   * スキルの経験を、経験月数の多い順に返します.
   *
   * <p>同じ月数のスキルは、案件に最初に出てきた順に並べる。
   *
   * @return スキルの経験の一覧
   */
  List<SkillExperience> skillExperiencesByMonths() {
    List<SkillExperience> sorted = new ArrayList<>(this.skills.values());
    sorted.sort(Comparator.comparingInt((SkillExperience skill) -> skill.months()).reversed());
    return sorted;
  }

  /**
   * 直近の案件の内訳を、新しい順に先頭から指定の件数だけ返します.
   *
   * <p>終了年月の新しい順に並べ、同じなら開始年月の新しい順に並べる。
   *
   * @param limit 返す件数の上限
   * @return 直近の案件の内訳
   */
  List<ProjectDetail> recentDetails(final int limit) {
    List<ProjectDetail> sorted = new ArrayList<>(this.details);
    sorted.sort(RECENT_FIRST);
    return sorted.stream().limit(limit).toList();
  }
}
