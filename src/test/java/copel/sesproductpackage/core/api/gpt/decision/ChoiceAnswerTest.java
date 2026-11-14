package copel.sesproductpackage.core.api.gpt.decision;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ChoiceAnswer} の単体テスト。 */
class ChoiceAnswerTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 境界値 {

      /**
       * 確率が同率の場合に、判定 API が返した選択肢をそのまま保持することを確かめる。 同率の選択は API の決定を正とし、呼び出し側が再判定しない仕様を守る。
       *
       * @given 選択肢 "billing" と "access" の確率がともに 0.5、API が返した選択肢が "access"、確信度が 0.9
       * @when ChoiceAnswer を選択肢と確率と確信度で生成する
       * @then API が返した選択肢 "access" を返す
       */
      @Test
      void 確率が同率の場合_APIが返した選択肢のキーを返す() {
        // 先に並ぶのは "billing" のため、再判定すると "billing" になる
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("billing", 0.5);
        probabilities.put("access", 0.5);

        ChoiceAnswer answer = new ChoiceAnswer("access", probabilities, 0.9);

        assertThat(answer.getChoice()).isEqualTo("access");
      }

      /**
       * 確率が一方に偏る場合に、選んだ選択肢と全選択肢の確率を保持することを確かめる。 呼び出し側が選択肢ごとの確率を並び順の判断に使えるようにする仕様を守る。
       *
       * @given 選択肢 "billing" の確率が 0.95、"access" の確率が 0.05、API が返した選択肢が "billing"、確信度が 0.99
       * @when ChoiceAnswer を選択肢と確率と確信度で生成する
       * @then 選択肢 "billing" と、全選択肢の確率 0.95 と 0.05 を返す
       */
      @Test
      void 選択肢の確率が一方に偏る場合_選んだ選択肢と全選択肢の確率を返す() {
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("billing", 0.95);
        probabilities.put("access", 0.05);

        ChoiceAnswer answer = new ChoiceAnswer("billing", probabilities, 0.99);

        assertThat(answer.getChoice()).isEqualTo("billing");
        assertThat(answer.getProbabilities())
            .containsEntry("billing", 0.95)
            .containsEntry("access", 0.05);
      }
    }
  }
}
