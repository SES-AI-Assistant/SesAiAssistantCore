package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link EnvUtils} の単体テスト。 */
class EnvUtilsTest {

  @Nested
  class get {

    @Nested
    class 境界値 {

      /**
       * 未設定の環境変数は null を返すことを確かめる。 未設定の環境変数を空文字にすると未設定と空の区別が消えるので、null を返す。
       *
       * @given 環境変数 "NECTAR_UT_UNDEFINED_ENV" が未設定
       * @when get を未設定の環境変数名で実行する
       * @then null を返す
       */
      @Test
      void 環境変数が未設定の場合_nullを返す() {
        assertThat(EnvUtils.get("NECTAR_UT_UNDEFINED_ENV")).isNull();
      }
    }
  }
}
