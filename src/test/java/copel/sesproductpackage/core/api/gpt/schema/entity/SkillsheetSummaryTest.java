package copel.sesproductpackage.core.api.gpt.schema.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.unit.ProjectPeriod;
import copel.sesproductpackage.core.unit.SkillName;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SkillsheetSummary} の単体テスト。 */
class SkillsheetSummaryTest {

  @Nested
  class toText {

    @Nested
    class 正常系 {

      /**
       * スキルと内訳がある場合に、見出しと行を改行で連結した要約を返すことを確かめる。 要約の書式を AI の入力として固定するためである。
       *
       * @given スキル "Java" の経験が3か月、内訳が 2024年4月から2024年6月 の案件1件
       * @when toText を実行する
       * @then スキルの見出しと行、内訳の見出しと行を改行で連結した文字列を返す
       */
      @Test
      void スキルと内訳がある場合_見出しと行を改行で連結した要約を返す() {
        SkillsheetSummary summary =
            new SkillsheetSummary(
                List.of(skill("Java", "2024年4月", "2024年6月")),
                List.of(detail("2024年4月", "2024年6月", "案件A")));

        assertThat(summary.toText())
            .isEqualTo("■スキル・経験\n・Java: 3ヶ月\n■直近のPJ経験\n2024年4月-2024年6月: 案件A");
      }
    }

    @Nested
    class 境界値 {

      /**
       * スキルの経験が無い場合に、内訳の見出しと行だけを返すことを確かめる。 スキルが無いときに空の見出しを出さないためである。
       *
       * @given スキルが空の一覧、内訳が 2024年4月から2024年6月 の案件1件
       * @when toText を実行する
       * @then 内訳の見出しと行だけを返す
       */
      @Test
      void スキルが無い場合_内訳の見出しと行だけを返す() {
        SkillsheetSummary summary =
            new SkillsheetSummary(List.of(), List.of(detail("2024年4月", "2024年6月", "案件A")));

        assertThat(summary.toText()).isEqualTo("■直近のPJ経験\n2024年4月-2024年6月: 案件A");
      }

      /**
       * 内訳が無い場合に、スキルの見出しと行だけを返すことを確かめる。 内訳が無いときに空の見出しを出さないためである。
       *
       * @given スキル "Java" の経験が3か月、内訳が空の一覧
       * @when toText を実行する
       * @then スキルの見出しと行だけを返す
       */
      @Test
      void 内訳が無い場合_スキルの見出しと行だけを返す() {
        SkillsheetSummary summary =
            new SkillsheetSummary(List.of(skill("Java", "2024年4月", "2024年6月")), List.of());

        assertThat(summary.toText()).isEqualTo("■スキル・経験\n・Java: 3ヶ月");
      }

      /**
       * スキルも内訳も無い場合に、空文字を返すことを確かめる。 見出しだけの要約を返すと、AI が経験の無い要員を経験ありと読むおそれがあるためである。
       *
       * @given スキルと内訳がどちらも空の一覧
       * @when toText を実行する
       * @then 空文字を返す
       */
      @Test
      void スキルも内訳も無い場合_空文字を返す() {
        assertThat(new SkillsheetSummary(List.of(), List.of()).toText()).isEmpty();
      }

      /**
       * 要約が1000文字を超える場合に、末尾の内訳から落として上限以内に収めることを確かめる。 行の途中で切ると文が壊れるため、行単位で落とすという仕様を守るためである。
       *
       * @given 内訳を末尾まで含めると1000文字を超え、内訳を1件落とすと収まる2件の内訳と、スキル "Java" の1件
       * @when toText を実行する
       * @then 末尾の内訳を落とした1000文字以内の要約を返す
       */
      @Test
      void 要約が1000文字を超える場合_末尾の内訳を落とした要約を返す() {
        String perspective = "あ".repeat(600);
        SkillsheetSummary summary =
            new SkillsheetSummary(
                List.of(skill("Java", "2024年4月", "2024年6月")),
                List.of(
                    detail("2024年4月", "2024年6月", perspective),
                    detail("2023年4月", "2023年6月", perspective)));

        assertThat(summary.toText())
            .isEqualTo("■スキル・経験\n・Java: 3ヶ月\n■直近のPJ経験\n2024年4月-2024年6月: " + perspective);
      }
    }
  }

  @Nested
  class withoutLastLine {

    @Nested
    class 正常系 {

      /**
       * 内訳がある場合に、末尾の内訳を1件だけ落とすことを確かめる。 内訳は新しい順に並ぶため、末尾が最も古い案件になるためである。
       *
       * @given 内訳が2件、スキル "Java" の1件
       * @when withoutLastLine を実行して toText を実行する
       * @then 先頭の内訳だけを残した要約を返す
       */
      @Test
      void 内訳がある場合_末尾の内訳を1件落とした要約を返す() {
        SkillsheetSummary summary =
            new SkillsheetSummary(
                List.of(skill("Java", "2024年4月", "2024年6月")),
                List.of(
                    detail("2024年4月", "2024年6月", "新しい案件"), detail("2023年4月", "2023年6月", "古い案件")));

        assertThat(summary.withoutLastLine().toText())
            .isEqualTo("■スキル・経験\n・Java: 3ヶ月\n■直近のPJ経験\n2024年4月-2024年6月: 新しい案件");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 内訳が無い場合に、スキルの末尾の行を1件だけ落とすことを確かめる。 内訳を落とし終えた後は、月数の少ないスキルから落とすためである。
       *
       * @given 内訳が空で、スキル "Java" が6か月、"AWS" が3か月の2件
       * @when withoutLastLine を実行して toText を実行する
       * @then 月数の多い "Java" の行だけを残した要約を返す
       */
      @Test
      void 内訳が無い場合_末尾のスキルを1件落とした要約を返す() {
        SkillsheetSummary summary =
            new SkillsheetSummary(
                List.of(skill("Java", "2024年1月", "2024年6月"), skill("AWS", "2024年1月", "2024年3月")),
                List.of());

        assertThat(summary.withoutLastLine().toText()).isEqualTo("■スキル・経験\n・Java: 6ヶ月");
      }
    }
  }

  /**
   * 1つのスキルの経験を、期間の文字列から作る.
   *
   * @param name スキル名
   * @param start 開始年月
   * @param end 終了年月
   * @return スキルの経験
   */
  private static SkillExperience skill(final String name, final String start, final String end) {
    SkillExperience experience = new SkillExperience(SkillName.of(name).orElseThrow());
    experience.addPeriod(period(start, end));
    return experience;
  }

  /**
   * 内訳の1件を、期間の文字列から作る.
   *
   * @param start 開始年月
   * @param end 終了年月
   * @param perspective 内容
   * @return 内訳の1件
   */
  private static ProjectDetail detail(
      final String start, final String end, final String perspective) {
    return new ProjectDetail(period(start, end), perspective);
  }

  /**
   * 案件期間を文字列から作る.
   *
   * @param start 開始年月の文字列
   * @param end 終了年月の文字列
   * @return 案件期間
   */
  private static ProjectPeriod period(final String start, final String end) {
    return ProjectPeriod.of(start, end).orElseThrow();
  }
}
