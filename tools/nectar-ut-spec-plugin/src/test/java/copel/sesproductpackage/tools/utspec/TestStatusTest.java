package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TestStatus} の単体テスト。 */
class TestStatusTest {

  @Nested
  class label {

    @Nested
    class 正常系 {

      /**
       * 失敗の実行結果が仕様書の表示名を返すことを確かめる。 仕様書の結果の列に日本語の表示名を出す。
       *
       * @given 実行結果が FAILED
       * @when label を実行する
       * @then "失敗" を返す
       */
      @Test
      void 結果が失敗の場合_失敗の表示名を返す() {
        assertThat(TestStatus.FAILED.label()).isEqualTo("失敗");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未実行の表示名を返すことを確かめる。 surefire に無いテストを成功と誤って表示しないようにする。
       *
       * @given 実行結果が UNKNOWN
       * @when label を実行する
       * @then "未実行" を返す
       */
      @Test
      void 結果が未実行の場合_未実行の表示名を返す() {
        assertThat(TestStatus.UNKNOWN.label()).isEqualTo("未実行");
      }
    }
  }
}
