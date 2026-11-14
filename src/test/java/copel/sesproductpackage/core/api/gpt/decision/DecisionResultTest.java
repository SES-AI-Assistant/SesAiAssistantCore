package copel.sesproductpackage.core.api.gpt.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link DecisionResult} の単体テスト。 */
class DecisionResultTest {

  @Nested
  class answerOf {

    @Nested
    class 正常系 {

      /**
       * Yes/No の質問の回答を、質問の型の回答として取り出せることを確かめる。 呼び出し側が回答の型を変換せずに使えるようにする仕様を守る。
       *
       * @given 質問キー "person_1" の Yes の確率が 0.97 の回答を持つ判定結果
       * @when answerOf を Yes/No の質問 "person_1" で呼び出す
       * @then Yes の確率が 0.97 の NoulAnswer を返す
       */
      @Test
      void Yesと_Noの質問の場合_NoulAnswerを返す() {
        NoulQuestion question = NoulQuestion.of("person_1", "条件を満たすか");
        DecisionResult result = resultOf("person_1", new NoulAnswer(0.97, 0.97));

        NoulAnswer answer = result.answerOf(question);

        assertThat(answer.getProbability()).isEqualTo(0.97);
      }

      /**
       * 択一の質問の回答を、質問の型の回答として取り出せることを確かめる。 択一の回答を選ばれた選択肢付きで取り出せる仕様を守る。
       *
       * @given 質問キー "link" の選ばれた選択肢が "billing" の回答を持つ判定結果
       * @when answerOf を択一の質問 "link" で呼び出す
       * @then 選ばれた選択肢 "billing" の ChoiceAnswer を返す
       */
      @Test
      void 択一の質問の場合_ChoiceAnswerを返す() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("billing", "請求");
        options.put("access", "");
        ChoiceQuestion question = ChoiceQuestion.of("link", "該当する要員は", options);
        Map<String, Double> probabilities = Map.of("billing", 0.9, "access", 0.1);
        DecisionResult result = resultOf("link", new ChoiceAnswer("billing", probabilities, 0.9));

        ChoiceAnswer answer = result.answerOf(question);

        assertThat(answer.getChoice()).isEqualTo("billing");
      }

      /**
       * Yes/No の質問と択一の質問が混在する判定結果でも、それぞれの型の回答を取り出せることを確かめる。 1回の判定の中で型の異なる質問を混ぜて使う仕様を守る。
       *
       * @given 質問キー "person_1" の Yes/No の回答と、質問キー "link" の択一の回答を持つ判定結果
       * @when answerOf を Yes/No の質問と択一の質問でそれぞれ呼び出す
       * @then Yes の確率 0.97 の NoulAnswer と、選ばれた選択肢 "billing" の ChoiceAnswer を返す
       */
      @Test
      void Yesと_Noの質問と択一の質問が混在する場合_それぞれの型の回答を返す() {
        NoulQuestion noul = NoulQuestion.of("person_1", "条件を満たすか");
        ChoiceQuestion choice =
            ChoiceQuestion.of("link", "該当する要員は", Map.of("billing", "", "access", ""));
        Map<String, DecisionAnswer> answers = new LinkedHashMap<>();
        answers.put("person_1", new NoulAnswer(0.97, 0.97));
        answers.put(
            "link", new ChoiceAnswer("billing", Map.of("billing", 0.9, "access", 0.1), 0.9));
        DecisionResult result = new DecisionResult(answers);

        assertThat(result.answerOf(noul).getProbability()).isEqualTo(0.97);
        assertThat(result.answerOf(choice).getChoice()).isEqualTo("billing");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 判定結果に無い質問キーで回答を取り出そうとした場合に、例外を投げることを確かめる。 質問と回答の対応が無い状態を黙って通さない仕様を守る。
       *
       * @given 質問キー "person_1" の回答だけを持つ判定結果
       * @when answerOf を質問キー "person_2" の Yes/No の質問で呼び出す
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 回答が無い質問キーの場合_IllegalArgumentExceptionを投げる() {
        NoulQuestion question = NoulQuestion.of("person_2", "条件を満たすか");
        DecisionResult result = resultOf("person_1", new NoulAnswer(0.97, 0.97));

        assertThatThrownBy(() -> result.answerOf(question))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  /**
   * 1件の回答だけを持つ判定結果を生成する。
   *
   * @param key 質問キー
   * @param answer 回答
   * @return 判定結果
   */
  private static DecisionResult resultOf(final String key, final DecisionAnswer answer) {
    Map<String, DecisionAnswer> answers = new LinkedHashMap<>();
    answers.put(key, answer);
    return new DecisionResult(answers);
  }
}
