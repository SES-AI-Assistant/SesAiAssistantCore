package copel.sesproductpackage.core.api.gpt.schema.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.unit.ProjectPeriod;
import copel.sesproductpackage.core.unit.SkillName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SkillExperience} の単体テスト。 */
class SkillExperienceTest {

  @Nested
  class addPeriod {

    @Nested
    class 正常系 {

      /**
       * 追加した案件期間が、スキルの経験月数に反映されることを確かめる。 追加しても月数に入らなければ、スキルの経験が要約に出ないためである。
       *
       * @given スキル "Java" に 2023年4月から2023年6月の期間を追加する
       * @when months を実行する
       * @then 3を返す
       */
      @Test
      void 期間を1件追加した場合_その期間の月数を返す() {
        SkillExperience experience = experience("Java");
        experience.addPeriod(period("2023年4月", "2023年6月"));

        assertThat(experience.months()).isEqualTo(3);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 同じ期間を2回追加した場合に、重なる月を1回だけ数えることを確かめる。 同じ案件の期間が二重に届いても、経験を増やさないためである。
       *
       * @given スキル "Java" に 2023年4月から2023年9月の期間を2回追加する
       * @when months を実行する
       * @then 6を返す
       */
      @Test
      void 同じ期間を2回追加した場合_重なる月を1回だけ数えた月数を返す() {
        SkillExperience experience = experience("Java");
        experience.addPeriod(period("2023年4月", "2023年9月"));
        experience.addPeriod(period("2023年4月", "2023年9月"));

        assertThat(experience.months()).isEqualTo(6);
      }
    }
  }

  @Nested
  class months {

    @Nested
    class 境界値 {

      /**
       * 期間が1件も追加されていない場合に、経験月数が0になることを確かめる。 集計の途中で経験が無いスキルを扱っても、例外にしないためである。
       *
       * @given スキル "Java" に期間を追加しない
       * @when months を実行する
       * @then 0を返す
       */
      @Test
      void 期間が無い場合_0を返す() {
        assertThat(experience("Java").months()).isZero();
      }
    }
  }

  @Nested
  class toSummaryLine {

    @Nested
    class 正常系 {

      /**
       * 経験月数が18のスキルを、1年6ヶ月の表記で要約の行にすることを確かめる。 月数の表記を要約の仕様どおりにそろえるためである。
       *
       * @given スキル "Java" に 2023年1月から2024年6月の期間を追加する
       * @when toSummaryLine を実行する
       * @then "・Java: 1年6ヶ月" を返す
       */
      @Test
      void 経験月数が18の場合_表示名と1年6ヶ月の行を返す() {
        SkillExperience experience = experience("Java");
        experience.addPeriod(period("2023年1月", "2024年6月"));

        assertThat(experience.toSummaryLine()).isEqualTo("・Java: 1年6ヶ月");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 経験月数が1のスキルを、1ヶ月の表記で要約の行にすることを確かめる。 1か月の経験も、0ではなく1ヶ月として見せるためである。
       *
       * @given スキル "Xxx" に 2023年4月から2023年4月の期間を追加する
       * @when toSummaryLine を実行する
       * @then "・Xxx: 1ヶ月" を返す
       */
      @Test
      void 経験月数が1の場合_1ヶ月の行を返す() {
        SkillExperience experience = experience("Xxx");
        experience.addPeriod(period("2023年4月", "2023年4月"));

        assertThat(experience.toSummaryLine()).isEqualTo("・Xxx: 1ヶ月");
      }
    }
  }

  /**
   * 表示名だけを持つ、期間の無いスキルの経験を作る.
   *
   * @param name スキル名
   * @return スキルの経験
   */
  private static SkillExperience experience(final String name) {
    return new SkillExperience(SkillName.of(name).orElseThrow());
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
