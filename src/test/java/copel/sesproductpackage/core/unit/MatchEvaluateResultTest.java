package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link MatchEvaluateResult} の単体テスト。 */
class MatchEvaluateResultTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 引数なしで生成した場合、判定と評価テキストが未設定であることを確かめる。 初期状態を空にしておかないと、後から設定する使い方で値の抜けを見逃すので、初期値を固定する。
       *
       * @given 引数を指定しない
       * @when 生成する
       * @then 判定と評価テキストが null のインスタンスを返す
       */
      @Test
      void 引数なしで生成した場合_判定と評価テキストが未設定のインスタンスを返す() {
        MatchEvaluateResult result = new MatchEvaluateResult();

        assertThat(result.getMatch()).isNull();
        assertThat(result.getEvaluationText()).isNull();
      }
    }
  }

  @Nested
  class コンストラクタ_String_String {

    @Nested
    class 正常系 {

      /**
       * 判定と評価テキストを指定した場合、その値を保持することを確かめる。 AI の評価結果を加工せずに保持しないと判定の根拠が失われるので、値をそのまま保持する。
       *
       * @given 判定が "YES"、評価テキストが "評価結果"
       * @when 生成する
       * @then 指定した判定と評価テキストを保持するインスタンスを返す
       */
      @Test
      void 判定と評価テキストを指定した場合_指定した値を保持するインスタンスを返す() {
        MatchEvaluateResult result = new MatchEvaluateResult("YES", "評価結果");

        assertThat(result.getMatch()).isEqualTo("YES");
        assertThat(result.getEvaluationText()).isEqualTo("評価結果");
      }
    }
  }
}
