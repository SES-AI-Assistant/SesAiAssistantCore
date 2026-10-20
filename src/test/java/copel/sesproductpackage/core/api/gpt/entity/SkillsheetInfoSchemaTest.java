package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SkillsheetInfoSchema} の単体テスト。 */
class SkillsheetInfoSchemaTest {

  @Nested
  class toSummaryText {

    @Nested
    class 正常系 {

      /**
       * スキル経験と PJ 経験の両方がある場合に、見出しごとに並べた要約を返すことを確かめる。 要約は AI
       * の判定の入力になるため、項目の並びと書式を固定して崩れを防ぐので、その仕様を守る。
       *
       * @given 期間が "3年" のスキル経験と期間が未設定のスキル経験、PJ 経験が1件ある
       * @when toSummaryText を呼び出す
       * @then スキル経験の期間と PJ 経験の期間を含む要約を返す
       */
      @Test
      void スキル経験とPJ経験が両方ある場合_見出しごとの要約を返す() {
        SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
        schema.setExperiences(List.of(experience("SpringBoot", "3年"), experience("Oracle", null)));
        schema.setProjectExperiences(List.of(project("2023年8月", "2023年11月", "基本設計")));

        assertThat(schema.toSummaryText())
            .isEqualTo(
                "■スキル・経験\n"
                    + "・SpringBoot: 3年\n"
                    + "・Oracle: 経験あり\n"
                    + "■直近のPJ経験\n"
                    + "2023年8月-2023年11月: 基本設計");
      }

      /**
       * PJ 経験が無い場合に、スキル経験だけの要約を返すことを確かめる。 PJ 経験の見出しを空で出すと、AI に存在しない経験を示すおそれがあるので、その仕様を守る。
       *
       * @given スキル経験が1件あり、PJ 経験が未設定である
       * @when toSummaryText を呼び出す
       * @then スキル経験の見出しだけを含む要約を返す
       */
      @Test
      void PJ経験が無い場合_スキル経験だけを含む要約を返す() {
        SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
        schema.setExperiences(List.of(experience("Java", "1年")));

        assertThat(schema.toSummaryText()).isEqualTo("■スキル・経験\n・Java: 1年");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 期間が年の形式に一致しない場合に、期間を経験ありとして要約することを確かめる。 推測した期間を出さず、記載の無い期間は経験の有無だけを示すという仕様を守る。
       *
       * @given 期間が "約3年" のスキル経験が1件ある
       * @when toSummaryText を呼び出す
       * @then 期間の代わりに経験ありを示す要約を返す
       */
      @Test
      void 期間が形式に一致しない場合_期間に経験ありを表示した要約を返す() {
        SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
        schema.setExperiences(List.of(experience("Java", "約3年")));

        assertThat(schema.toSummaryText()).isEqualTo("■スキル・経験\n・Java: 経験あり");
      }

      /**
       * 期間が空白だけの場合に、期間を経験ありとして要約することを確かめる。 空白だけの期間を表示すると、要約の見た目が崩れるので、その仕様を守る。
       *
       * @given 期間が " " のスキル経験が1件ある
       * @when toSummaryText を呼び出す
       * @then 期間の代わりに経験ありを示す要約を返す
       */
      @Test
      void 期間が空白だけの場合_期間に経験ありを表示した要約を返す() {
        SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
        schema.setExperiences(List.of(experience("Java", "  ")));

        assertThat(schema.toSummaryText()).isEqualTo("■スキル・経験\n・Java: 経験あり");
      }

      /**
       * スキル経験と PJ 経験が両方とも無い場合に、空文字を返すことを確かめる。 見出しだけの要約を返すと、AI が経験の無い要員を経験ありと読むおそれがあるので、その仕様を守る。
       *
       * @given スキル経験と PJ 経験が未設定である
       * @when toSummaryText を呼び出す
       * @then 空文字を返す
       */
      @Test
      void スキル経験もPJ経験も無い場合_空文字を返す() {
        assertThat(new SkillsheetInfoSchema().toSummaryText()).isEmpty();
      }

      /**
       * 要約が1000文字を超える場合に、1000文字に切り詰めることを確かめる。 要約の長さは AI への入力の上限に合わせるため、超過分を切り捨てることを守る。
       *
       * @given PJ 経験が30件あり、それぞれの内容が50文字である
       * @when toSummaryText を呼び出す
       * @then 1000文字に切り詰めた要約を返す
       */
      @Test
      void 要約が1000文字を超える場合_1000文字に切り詰めた要約を返す() {
        String longContent = "あ".repeat(50);
        List<SkillsheetInfoSchema.ProjectExperience> projects = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
          projects.add(project("2023年8月", "2023年11月", longContent));
        }
        SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
        schema.setProjectExperiences(projects);

        assertThat(schema.toSummaryText()).hasSize(1000);
      }

      /**
       * スキル経験とPJ経験が空の一覧の場合に、空文字を返すことを確かめる。 空の一覧で見出しだけを出さないことを守る。
       *
       * @given スキル経験とPJ経験の一覧が空である
       * @when toSummaryText を呼び出す
       * @then 空文字を返す
       */
      @Test
      void スキル経験とPJ経験が空の一覧の場合_空文字を返す() throws Exception {
        SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
        schema.setExperiences(List.of());
        schema.setProjectExperiences(List.of());

        assertThat(schema.toSummaryText()).isEmpty();
      }
    }
  }

  /**
   * スキル経験を1件作る.
   *
   * @param perspective 要求項目
   * @param duration 期間
   * @return スキル経験
   */
  private static SkillsheetInfoSchema.Experience experience(
      final String perspective, final String duration) {
    SkillsheetInfoSchema.Experience experience = new SkillsheetInfoSchema.Experience();
    experience.setPerspective(perspective);
    experience.setDuration(duration);
    return experience;
  }

  /**
   * PJ 経験を1件作る.
   *
   * @param startMonth 開始年月
   * @param endMonth 終了年月
   * @param perspective 内容
   * @return PJ 経験
   */
  private static SkillsheetInfoSchema.ProjectExperience project(
      final String startMonth, final String endMonth, final String perspective) {
    SkillsheetInfoSchema.ProjectExperience project = new SkillsheetInfoSchema.ProjectExperience();
    project.setStartMonth(startMonth);
    project.setEndMonth(endMonth);
    project.setPerspective(perspective);
    return project;
  }
}
