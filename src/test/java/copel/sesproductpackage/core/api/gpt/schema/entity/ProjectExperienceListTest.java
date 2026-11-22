package copel.sesproductpackage.core.api.gpt.schema.entity;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** {@link ProjectExperienceList} の単体テスト。 */
class ProjectExperienceListTest {

  /** 集計の警告ログを集めるアペンダー。除外した案件の警告を確かめるテストだけが使う。 */
  private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

  @BeforeEach
  void 警告ログを集める() {
    logAppender.start();
    ((Logger) LoggerFactory.getLogger(ProjectExperienceList.class)).addAppender(logAppender);
  }

  @AfterEach
  void 警告ログの収集を解除する() {
    ((Logger) LoggerFactory.getLogger(ProjectExperienceList.class)).detachAppender(logAppender);
    logAppender.stop();
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * スキルが未設定の案件は、スキルを集計せず内訳だけを残すことを確かめる。 スキルの無い案件も期間の内訳として要約に残す仕様を守るためである。
       *
       * @given 案件が開始 2024年4月、終了 2024年9月、スキルが null で内容が "基幹システムの改修"
       * @when skillExperiencesByMonths と recentDetails を実行する
       * @then スキルの経験は空で、内訳は1件を返す
       */
      @Test
      void スキルが未設定の案件の場合_スキルの経験を空にして内訳の1件を返す() {
        SkillsheetInfoSchema.ProjectExperience project =
            project("2024年4月", "2024年9月", "基幹システムの改修", null);
        ProjectExperienceList projects = new ProjectExperienceList(List.of(project));

        assertThat(projects.skillExperiencesByMonths()).isEmpty();
        assertThat(projects.recentDetails(5)).hasSize(1);
      }

      /**
       * 案件の一覧が null の場合は、案件が無いものとして空の結果を返すことを確かめる。 案件が未設定の要約でも例外にしないためである。
       *
       * @given 案件の一覧が null
       * @when skillExperiencesByMonths と recentDetails を実行する
       * @then スキルの経験と内訳がどちらも空を返す
       */
      @Test
      void 案件の一覧がnullの場合_空の結果を返す() {
        ProjectExperienceList projects = new ProjectExperienceList(null);

        assertThat(projects.skillExperiencesByMonths()).isEmpty();
        assertThat(projects.recentDetails(5)).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 開始年月が解釈できない案件は集計から除外し、WARN ログを出すことを確かめる。 例外で登録全体を失敗させないためであり、除外した事実を運用で追えるようにするためである。
       *
       * @given 案件の開始年月が "2023年13月"、終了年月が "2023年6月"、スキルが "Java" の案件が1件
       * @when skillExperiencesByMonths と recentDetails を実行する
       * @then スキルの経験と内訳がどちらも空を返し、WARN のログを1件出す
       */
      @Test
      void 開始年月が解釈できない場合_除外して空の結果を返す() {
        SkillsheetInfoSchema.ProjectExperience project =
            project("2023年13月", "2023年6月", "除外される案件", List.of("Java"));
        ProjectExperienceList projects = new ProjectExperienceList(List.of(project));

        assertThat(projects.skillExperiencesByMonths()).isEmpty();
        assertThat(projects.recentDetails(5)).isEmpty();
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getLevel)
            .containsExactly(Level.WARN);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 空白だけのスキル名は、スキルとして集計しないことを確かめる。 空のスキル名が要約に出ると、利用者に意味の無い行が見えるためである。
       *
       * @given 案件のスキルが空白だけの文字列 " " の1件
       * @when skillExperiencesByMonths を実行する
       * @then スキルの経験は空を返す
       */
      @Test
      void スキル名が空白だけの場合_スキルの経験が空のリストを返す() {
        SkillsheetInfoSchema.ProjectExperience project =
            project("2024年4月", "2024年9月", "内容", List.of("  "));
        ProjectExperienceList projects = new ProjectExperienceList(List.of(project));

        assertThat(projects.skillExperiencesByMonths()).isEmpty();
      }
    }
  }

  @Nested
  class skillExperiencesByMonths {

    @Nested
    class 正常系 {

      /**
       * スキルの経験を、経験月数の多い順に並べることを確かめる。 利用経験の長いスキルを要約の先頭に出すためである。
       *
       * @given スキル "Java" の案件が3か月、スキル "AWS" の案件が6か月
       * @when skillExperiencesByMonths を実行する
       * @then "AWS" の6か月、"Java" の3か月の順に返す
       */
      @Test
      void 月数が異なるスキルの場合_月数の多い順に並べたリストを返す() {
        ProjectExperienceList projects =
            new ProjectExperienceList(
                List.of(
                    project("2024年1月", "2024年3月", "内容1", List.of("Java")),
                    project("2024年1月", "2024年6月", "内容2", List.of("AWS"))));

        assertThat(projects.skillExperiencesByMonths())
            .extracting(SkillExperience::toSummaryLine)
            .containsExactly("・AWS: 6ヶ月", "・Java: 3ヶ月");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 同じ月数のスキルは、案件に最初に出てきた順に並べることを確かめる。 同じ条件のスキルの並びが毎回変わらないようにするためである。
       *
       * @given スキル "Java" と "AWS" がどちらも3か月で、"Java" の案件が先
       * @when skillExperiencesByMonths を実行する
       * @then "Java" を先に、"AWS" を後に返す
       */
      @Test
      void 同じ月数のスキルの場合_最初に出てきた順のリストを返す() {
        ProjectExperienceList projects =
            new ProjectExperienceList(
                List.of(
                    project("2024年1月", "2024年3月", "内容1", List.of("Java")),
                    project("2024年1月", "2024年3月", "内容2", List.of("AWS"))));

        assertThat(projects.skillExperiencesByMonths())
            .extracting(SkillExperience::toSummaryLine)
            .containsExactly("・Java: 3ヶ月", "・AWS: 3ヶ月");
      }
    }
  }

  @Nested
  class recentDetails {

    @Nested
    class 正常系 {

      /**
       * 直近の案件を、終了年月の新しい順に並べることを確かめる。 直近に携わった案件を要約の先頭に出すためである。
       *
       * @given 終了年月が 2023年6月 の案件と 2024年9月 の案件
       * @when recentDetails を上限5件で実行する
       * @then 2024年9月に終わった案件、2023年6月に終わった案件の順に返す
       */
      @Test
      void 終了年月が異なる案件の場合_終了年月の新しい順のリストを返す() {
        ProjectExperienceList projects =
            new ProjectExperienceList(
                List.of(
                    project("2023年4月", "2023年6月", "古い案件", null),
                    project("2024年4月", "2024年9月", "新しい案件", null)));

        assertThat(projects.recentDetails(5))
            .extracting(ProjectDetail::toSummaryLine)
            .containsExactly("2024年4月-2024年9月: 新しい案件", "2023年4月-2023年6月: 古い案件");
      }

      /**
       * 件数の上限より多い案件がある場合は、先頭の件数だけを返すことを確かめる。 要約に載せる直近の案件を上限の件数に絞るためである。
       *
       * @given 案件が3件あり、それぞれ終了年月が異なる
       * @when recentDetails を上限2件で実行する
       * @then 終了年月が新しい順に2件を返す
       */
      @Test
      void 件数の上限より多い場合_先頭の件数だけを返す() {
        ProjectExperienceList projects =
            new ProjectExperienceList(
                List.of(
                    project("2023年1月", "2023年3月", "案件A", null),
                    project("2023年4月", "2023年6月", "案件B", null),
                    project("2023年7月", "2023年9月", "案件C", null)));

        assertThat(projects.recentDetails(2))
            .extracting(ProjectDetail::toSummaryLine)
            .containsExactly("2023年7月-2023年9月: 案件C", "2023年4月-2023年6月: 案件B");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 終了年月が同じ案件は、開始年月の新しい順に並べることを確かめる。 同じ時期に終わった案件の並びを、期間の長さに関係なく決めるためである。
       *
       * @given 終了年月が 2024年12月 で開始年月が 2023年10月 の案件と 2024年1月 の案件
       * @when recentDetails を上限5件で実行する
       * @then 開始年月が 2024年1月 の案件を先に返す
       */
      @Test
      void 終了年月が同じ案件の場合_開始年月の新しい順のリストを返す() {
        ProjectExperienceList projects =
            new ProjectExperienceList(
                List.of(
                    project("2023年10月", "2024年12月", "長い案件", null),
                    project("2024年1月", "2024年12月", "短い案件", null)));

        assertThat(projects.recentDetails(5))
            .extracting(ProjectDetail::toSummaryLine)
            .containsExactly("2024年1月-2024年12月: 短い案件", "2023年10月-2024年12月: 長い案件");
      }
    }
  }

  /**
   * AIが抽出した案件を1件作る.
   *
   * @param startMonth 開始年月
   * @param endMonth 終了年月
   * @param perspective 内容
   * @param skills 使用スキル。null の場合は未設定
   * @return 案件
   */
  private static SkillsheetInfoSchema.ProjectExperience project(
      final String startMonth,
      final String endMonth,
      final String perspective,
      final List<String> skills) {
    SkillsheetInfoSchema.ProjectExperience project = new SkillsheetInfoSchema.ProjectExperience();
    project.setStartMonth(startMonth);
    project.setEndMonth(endMonth);
    project.setPerspective(perspective);
    project.setSkills(skills);
    return project;
  }
}
