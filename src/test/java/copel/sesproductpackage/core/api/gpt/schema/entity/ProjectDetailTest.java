package copel.sesproductpackage.core.api.gpt.schema.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.unit.ProjectPeriod;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ProjectDetail} の単体テスト。 */
class ProjectDetailTest {

  @Nested
  class toSummaryLine {

    @Nested
    class 正常系 {

      /**
       * 案件の期間と内容から、要約文の1行を作ることを確かめる。 直近の案件の内訳を、期間と内容の形で利用者に見せるためである。
       *
       * @given 案件期間が 2024年4月から2024年9月、内容が "基幹システムの改修"
       * @when toSummaryLine を実行する
       * @then "2024年4月-2024年9月: 基幹システムの改修" を返す
       */
      @Test
      void 期間と内容がある場合_期間と内容の行を返す() {
        ProjectDetail detail = new ProjectDetail(period("2024年4月", "2024年9月"), "基幹システムの改修");

        assertThat(detail.toSummaryLine()).isEqualTo("2024年4月-2024年9月: 基幹システムの改修");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 内容が null の場合は、内容を空にした行を作ることを確かめる。 null の文字列を要約文に出すと、利用者に意味の無い語が見えるためである。
       *
       * @given 案件期間が 2024年4月から2024年9月、内容が null
       * @when toSummaryLine を実行する
       * @then "2024年4月-2024年9月: " を返す
       */
      @Test
      void 内容がnullの場合_内容を空にした行を返す() {
        ProjectDetail detail = new ProjectDetail(period("2024年4月", "2024年9月"), null);

        assertThat(detail.toSummaryLine()).isEqualTo("2024年4月-2024年9月: ");
      }
    }
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
