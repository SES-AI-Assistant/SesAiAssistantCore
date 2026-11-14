package copel.sesproductpackage.core.api.gpt.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link NoulQuestion} の単体テスト。 */
class NoulQuestionTest {

  @Nested
  class of_String_String {

    @Nested
    class 正常系 {

      /**
       * 判断基準を指定しない質問を作ると、判断基準が無い質問になることを確かめる。 判断基準は任意の項目のため、指定が無い場合は送信に含めない仕様を守る。
       *
       * @given 質問キーが "person_1"、指示文が "条件を満たすか"
       * @when NoulQuestion.of を質問キーと指示文で実行する
       * @then 判断基準の無い質問を返し、Yes の判断基準が null である
       */
      @Test
      void 判断基準を指定しない場合_判断基準がnullの質問を返す() {
        NoulQuestion question = NoulQuestion.of("person_1", "条件を満たすか");

        assertThat(question.getTrueDescription()).isNull();
      }
    }
  }

  @Nested
  class of_String_String_String_String {

    @Nested
    class 正常系 {

      /**
       * Yes と No の判断基準を指定した質問は、両方の判断基準を保持することを確かめる。 判断基準は両方そろえて API に送る仕様を守る。
       *
       * @given 質問キーが "person_1"、指示文が "条件を満たすか"、Yes の判断基準が "満たす"、No の判断基準が "満たさない"
       * @when NoulQuestion.of を質問キーと指示文と両方の判断基準で実行する
       * @then Yes の判断基準 "満たす" と No の判断基準 "満たさない" を持つ質問を返す
       */
      @Test
      void 判断基準を指定した場合_YesとNoの判断基準を持つ質問を返す() {
        NoulQuestion question = NoulQuestion.of("person_1", "条件を満たすか", "満たす", "満たさない");

        assertThat(question.getTrueDescription()).isEqualTo("満たす");
        assertThat(question.getFalseDescription()).isEqualTo("満たさない");
      }
    }

    @Nested
    class 異常系 {

      /**
       * Yes の判断基準に null を指定した場合に、例外を投げることを確かめる。 判断基準を一方だけ欠くと、API に不完全な判断基準を送るため弾く仕様を守る。
       *
       * @given 質問キーが "person_1"、指示文が "条件を満たすか"、Yes の判断基準が null
       * @when NoulQuestion.of を質問キーと指示文と判断基準で実行する
       * @then NullPointerException を投げる
       */
      @Test
      void Yesの判断基準がnullの場合_NullPointerExceptionを投げる() {
        assertThatThrownBy(() -> NoulQuestion.of("person_1", "条件を満たすか", null, "満たさない"))
            .isInstanceOf(NullPointerException.class);
      }
    }
  }

  @Nested
  class answerType {

    @Nested
    class 正常系 {

      /**
       * Yes/No の質問の回答の型が NoulAnswer であることを確かめる。 呼び出し側が回答を型付きで取り出すために、型の対応を守る。
       *
       * @given 判断基準を指定した Yes/No の質問
       * @when answerType を呼び出す
       * @then NoulAnswer の型を返す
       */
      @Test
      void 判断基準を指定した質問の場合_NoulAnswerの型を返す() {
        NoulQuestion question = NoulQuestion.of("person_1", "条件を満たすか", "満たす", "満たさない");

        assertThat(question.answerType()).isEqualTo(NoulAnswer.class);
      }
    }
  }

  @Nested
  class textLength {

    @Nested
    class 正常系 {

      /**
       * 判断基準を指定しない質問の文字数は指示文だけで数えることを確かめる。 入力文字数の見積もりに判断基準を含めない仕様を守る。
       *
       * @given 指示文が "条件を満たすか"、7文字、判断基準が無い
       * @when textLength を呼び出す
       * @then 7 を返す
       */
      @Test
      void 判断基準を指定しない場合_指示文の文字数を返す() {
        NoulQuestion question = NoulQuestion.of("person_1", "条件を満たすか");

        assertThat(question.textLength()).isEqualTo(7);
      }

      /**
       * 判断基準を指定した質問の文字数は指示文と両方の判断基準の合計で数えることを確かめる。 API の課金の見積もりと同じ数え方を守る。
       *
       * @given 指示文が "条件を満たすか"、7文字、Yes の判断基準が "満たす"、3文字、No の判断基準が "満たさない"、5文字
       * @when textLength を呼び出す
       * @then 15 を返す
       */
      @Test
      void 判断基準を指定した場合_指示文と判断基準の文字数の合計を返す() {
        NoulQuestion question = NoulQuestion.of("person_1", "条件を満たすか", "満たす", "満たさない");

        assertThat(question.textLength()).isEqualTo(15);
      }
    }
  }
}
