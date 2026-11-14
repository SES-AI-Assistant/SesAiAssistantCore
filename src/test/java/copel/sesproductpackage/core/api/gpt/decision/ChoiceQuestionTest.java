package copel.sesproductpackage.core.api.gpt.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ChoiceQuestion} の単体テスト。 */
class ChoiceQuestionTest {

  /**
   * 指定した件数の選択肢を、キーを o0 から順に付けて生成する。 選択肢の件数だけを変えたい場合に使う。
   *
   * @param count 選択肢の件数
   * @return 選択肢のマップ。値は空文字
   */
  private static Map<String, String> options(final int count) {
    Map<String, String> options = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) {
      options.put("o" + i, "");
    }
    return options;
  }

  /**
   * 確かめる選択肢キーと、もう1件の選択肢 access を持つマップを生成する。 選択肢キーの形式だけを変えたい場合に、件数の制約を満たす残りの選択肢と組み合わせる。
   *
   * @param optionKey 確かめる選択肢キー
   * @return 選択肢のマップ。残りの選択肢は access
   */
  private static Map<String, String> optionsWithKey(final String optionKey) {
    Map<String, String> options = new LinkedHashMap<>();
    options.put(optionKey, "説明");
    options.put("access", "");
    return options;
  }

  @Nested
  class of {

    @Nested
    class 正常系 {

      /**
       * 2件の選択肢で質問を作ると、指定した順序の選択肢を持つことを確かめる。 選択肢の順序は送信内容とログの読みやすさに関わるため、入力の順序を保つ仕様を守る。
       *
       * @given 質問キーが "link"、指示文が "該当する要員は"、選択肢が "billing" と "access" の順
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then "billing" と "access" の順の選択肢を持つ質問を返す
       */
      @Test
      void 選択肢が2件の場合_指定した順序の選択肢を持つ質問を返す() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("billing", "請求");
        options.put("access", "");

        ChoiceQuestion question = ChoiceQuestion.of("link", "該当する要員は", options);

        assertThat(question.getOptions())
            .containsExactly(Map.entry("billing", "請求"), Map.entry("access", ""));
      }

      /**
       * 選択肢キーの中央に空白がある場合に、質問を作れることを確かめる。 空白は先頭と末尾だけを禁止し、文字の間の空白は許す仕様を守る。
       *
       * @given 選択肢キーが "bill ing"、もう1件の選択肢キーが "access"
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then "bill ing" を含む選択肢を持つ質問を返す
       */
      @Test
      void 選択肢キーの中央に空白を含む場合_質問を返す() {
        ChoiceQuestion question = ChoiceQuestion.of("link", "該当する要員は", optionsWithKey("bill ing"));

        assertThat(question.getOptions()).containsKey("bill ing");
      }

      /**
       * 選択肢の説明が空文字の場合に、質問を作れることを確かめる。 説明は不要な選択肢でも指定できるように、空文字を許す仕様を守る。
       *
       * @given 件数が2件の選択肢で、説明がすべて空文字
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then 説明が空文字の選択肢 o0 を持つ質問を返す
       */
      @Test
      void 選択肢の説明が空文字の場合_説明が空文字の選択肢を持つ質問を返す() {
        ChoiceQuestion question = ChoiceQuestion.of("link", "該当する要員は", options(2));

        assertThat(question.getOptions()).containsEntry("o0", "");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 選択肢が1件の場合に、例外を投げることを確かめる。 1件では択一の判定にならないため、2件以上を求める仕様を守る。
       *
       * @given 選択肢が1件
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 選択肢が1件の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> ChoiceQuestion.of("link", "該当する要員は", options(1)))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 選択肢キーが空文字の場合に、例外を投げることを確かめる。 空の選択肢キーは回答を特定できないため弾く仕様を守る。
       *
       * @given 選択肢キーが空文字、もう1件の選択肢キーが "access"
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 選択肢キーが空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> ChoiceQuestion.of("link", "該当する要員は", optionsWithKey("")))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 選択肢キーの先頭が空白の場合に、例外を投げることを確かめる。 先頭の空白は API に送る前に取り除けないため、弾く仕様を守る。
       *
       * @given 選択肢キーが " billing"、もう1件の選択肢キーが "access"
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 選択肢キーの先頭が空白の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> ChoiceQuestion.of("link", "該当する要員は", optionsWithKey(" billing")))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 選択肢キーの末尾が全角空白の場合に、例外を投げることを確かめる。 非 ASCII の空白も末尾に置けない仕様を守る。
       *
       * @given 選択肢キーが "billing" の後ろに全角空白、もう1件の選択肢キーが "access"
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 選択肢キーの末尾が全角空白の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> ChoiceQuestion.of("link", "該当する要員は", optionsWithKey("billing　")))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 選択肢キーに制御文字を含む場合に、例外を投げることを確かめる。 制御文字は位置を問わず API に送れないため弾く仕様を守る。
       *
       * @given 選択肢キーが "bill" と制御文字 U+0001 と "ing"、もう1件の選択肢キーが "access"
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 選択肢キーに制御文字を含む場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> ChoiceQuestion.of("link", "該当する要員は", optionsWithKey("bill\u0001ing")))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 選択肢の説明に null を指定した場合に、例外を投げることを確かめる。 説明は必須の項目として API に送るため、null を許さない仕様を守る。
       *
       * @given 選択肢 "billing" の説明が null、もう1件の選択肢の説明が空文字
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then NullPointerException を投げる
       */
      @Test
      void 選択肢の説明がnullの場合_NullPointerExceptionを投げる() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("billing", null);
        options.put("access", "");

        assertThatThrownBy(() -> ChoiceQuestion.of("link", "該当する要員は", options))
            .isInstanceOf(NullPointerException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 選択肢が255件の場合に、質問を作れることを確かめる。 API が受け付ける選択肢の件数の上限ちょうどを通す仕様を守る。
       *
       * @given 選択肢が255件
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then 255件の選択肢を持つ質問を返す
       */
      @Test
      void 選択肢が255件の場合_255件の選択肢を持つ質問を返す() {
        ChoiceQuestion question = ChoiceQuestion.of("link", "該当する要員は", options(255));

        assertThat(question.getOptions()).hasSize(255);
      }

      /**
       * 選択肢が256件の場合に、例外を投げることを確かめる。 API の上限を超える件数を送信前に弾く仕様を守る。
       *
       * @given 選択肢が256件
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 選択肢が256件の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> ChoiceQuestion.of("link", "該当する要員は", options(256)))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 選択肢キーが128文字の場合に、質問を作れることを確かめる。 選択肢キーの長さの上限ちょうどを通す仕様を守る。
       *
       * @given 選択肢キーが英字の128文字、もう1件の選択肢キーが "access"
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then 128文字の選択肢キーを持つ質問を返す
       */
      @Test
      void 選択肢キーが128文字の場合_質問を返す() {
        String key = "a".repeat(128);

        ChoiceQuestion question = ChoiceQuestion.of("link", "該当する要員は", optionsWithKey(key));

        assertThat(question.getOptions()).containsKey(key);
      }

      /**
       * 選択肢キーが129文字の場合に、例外を投げることを確かめる。 API の選択肢キーの上限を超えるキーを送信前に弾く仕様を守る。
       *
       * @given 選択肢キーが英字の129文字、もう1件の選択肢キーが "access"
       * @when ChoiceQuestion.of を質問キーと指示文と選択肢で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 選択肢キーが129文字の場合_IllegalArgumentExceptionを投げる() {
        String key = "a".repeat(129);

        assertThatThrownBy(() -> ChoiceQuestion.of("link", "該当する要員は", optionsWithKey(key)))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class answerType {

    @Nested
    class 正常系 {

      /**
       * 択一の質問の回答の型が ChoiceAnswer であることを確かめる。 呼び出し側が回答を型付きで取り出すために、型の対応を守る。
       *
       * @given 選択肢が2件の択一の質問
       * @when answerType を呼び出す
       * @then ChoiceAnswer の型を返す
       */
      @Test
      void 選択肢の質問の場合_ChoiceAnswerの型を返す() {
        ChoiceQuestion question = ChoiceQuestion.of("link", "該当する要員は", options(2));

        assertThat(question.answerType()).isEqualTo(ChoiceAnswer.class);
      }
    }
  }

  @Nested
  class textLength {

    @Nested
    class 正常系 {

      /**
       * 択一の質問の文字数は指示文と選択肢のキーと説明の合計で数えることを確かめる。 入力文字数の見積もりに選択肢も含める仕様を守る。
       *
       * @given 指示文が "該当する要員は" の7文字、選択肢 "billing" の説明が "請求" の2文字、選択肢 "access" の説明が空文字
       * @when textLength を呼び出す
       * @then 22 を返す
       */
      @Test
      void 選択肢の質問の場合_指示文と選択肢のキーと説明の文字数の合計を返す() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("billing", "請求");
        options.put("access", "");
        ChoiceQuestion question = ChoiceQuestion.of("link", "該当する要員は", options);

        assertThat(question.textLength()).isEqualTo(22);
      }
    }
  }
}
