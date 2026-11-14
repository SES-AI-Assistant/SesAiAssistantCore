package copel.sesproductpackage.core.api.gpt.decision;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link NoulAnswer} の単体テスト。 */
class NoulAnswerTest {

  @Nested
  class isYes {

    @Nested
    class 正常系 {

      /**
       * 確率が閾値より高い場合に Yes と判定することを確かめる。 確率が高い回答を条件を満たすと扱う仕様を守る。
       *
       * @given Yes の確率が 0.9、確信度が 0.9
       * @when isYes を閾値 0.5 で呼び出す
       * @then true を返す
       */
      @Test
      void 確率が閾値より高い場合_trueを返す() {
        NoulAnswer answer = new NoulAnswer(0.9, 0.9);

        assertThat(answer.isYes(0.5)).isTrue();
      }

      /**
       * 確率が閾値より低い場合に Yes と判定しないことを確かめる。 確率が低い回答を条件を満たさないと扱う仕様を守る。
       *
       * @given Yes の確率が 0.1、確信度が 0.9
       * @when isYes を閾値 0.5 で呼び出す
       * @then false を返す
       */
      @Test
      void 確率が閾値より低い場合_falseを返す() {
        NoulAnswer answer = new NoulAnswer(0.1, 0.9);

        assertThat(answer.isYes(0.5)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 確率が閾値と等しい場合に Yes と判定しないことを確かめる。 閾値の値ちょうどは Yes に含めない仕様を守る。
       *
       * @given Yes の確率が 0.5、確信度が 0.5
       * @when isYes を閾値 0.5 で呼び出す
       * @then false を返す
       */
      @Test
      void 確率が閾値と等しい場合_falseを返す() {
        NoulAnswer answer = new NoulAnswer(0.5, 0.5);

        assertThat(answer.isYes(0.5)).isFalse();
      }

      /**
       * 確率が閾値をわずかに超える場合に Yes と判定することを確かめる。 閾値を超えた値だけを Yes に含める仕様を守る。
       *
       * @given Yes の確率が 0.51、確信度が 0.51
       * @when isYes を閾値 0.5 で呼び出す
       * @then true を返す
       */
      @Test
      void 確率が閾値を超える場合_trueを返す() {
        NoulAnswer answer = new NoulAnswer(0.51, 0.51);

        assertThat(answer.isYes(0.5)).isTrue();
      }
    }
  }
}
