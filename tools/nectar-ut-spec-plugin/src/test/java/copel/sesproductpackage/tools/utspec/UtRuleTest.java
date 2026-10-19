package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link UtRule} の単体テスト。 */
class UtRuleTest {

  @Nested
  class code {

    @Nested
    class 正常系 {

      /**
       * ルール番号が仕様書の番号どおりに返ることを確かめる。 違反の報告とルールの対応が変わると、利用者が仕様書と照合できなくなる。
       *
       * @given ルール UT01 が定義されている
       * @when code を実行する
       * @then "UT-01" を返す
       */
      @Test
      void 規則がUT01の場合_UT01の番号を返す() {
        assertThat(UtRule.UT01.code()).isEqualTo("UT-01");
      }
    }
  }

  @Nested
  class description {

    @Nested
    class 正常系 {

      /**
       * ルールの内容が空でない説明文として返ることを確かめる。 違反の一覧に説明を付けて、利用者が直すべき点を分かるようにする。
       *
       * @given ルール UT14 が定義されている
       * @when description を実行する
       * @then 禁止語に関する説明文を返す
       */
      @Test
      void 規則がUT14の場合_禁止語に関する説明文を返す() {
        assertThat(UtRule.UT14.description()).contains("禁止語");
      }
    }
  }
}
