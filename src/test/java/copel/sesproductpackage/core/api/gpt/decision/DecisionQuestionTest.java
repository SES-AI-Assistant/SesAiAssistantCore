package copel.sesproductpackage.core.api.gpt.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link DecisionQuestion} の単体テスト。
 *
 * <p>抽象クラスのため、コンストラクタの検証は具象クラスの {@link NoulQuestion#of(String, String)} を通して行う。
 */
class DecisionQuestionTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 英数字とアンダースコアとハイフンだけの質問キーを受け付けることを確かめる。 API が受け付ける質問キーの文字種と同じ範囲を通す仕様を守る。
       *
       * @given 質問キーが "person_1-A"、指示文が "条件を満たすか"
       * @when NoulQuestion.of を質問キーと指示文で実行する
       * @then 質問キーの "person_1-A" を返す
       */
      @Test
      void 英数字とアンダースコアとハイフンのキーの場合_そのキーを返す() {
        assertThat(NoulQuestion.of("person_1-A", "条件を満たすか").getKey()).isEqualTo("person_1-A");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 質問キーに空白を含む場合に、例外を投げることを確かめる。 空白を含むキーは API の文字種の制約に反するため弾く仕様を守る。
       *
       * @given 質問キーが "person 1"、指示文が "条件を満たすか"
       * @when NoulQuestion.of を質問キーと指示文で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void キーに空白を含む場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> NoulQuestion.of("person 1", "条件を満たすか"))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 質問キーが空文字の場合に、例外を投げることを確かめる。 キーは回答を取り出す識別子のため、空では回答を特定できない仕様を守る。
       *
       * @given 質問キーが空文字、指示文が "条件を満たすか"
       * @when NoulQuestion.of を質問キーと指示文で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void キーが空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> NoulQuestion.of("", "条件を満たすか"))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 質問キーに全角文字を含む場合に、例外を投げることを確かめる。 API が受け付けない文字種を送信前に弾く仕様を守る。
       *
       * @given 質問キーが "人物1"、指示文が "条件を満たすか"
       * @when NoulQuestion.of を質問キーと指示文で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void キーに全角文字を含む場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> NoulQuestion.of("人物1", "条件を満たすか"))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 質問キーが64文字の場合に、キーとして受け付けることを確かめる。 API の質問キーの上限ちょうどは使えることを守る。
       *
       * @given 質問キーが英字の64文字、指示文が "条件を満たすか"
       * @when NoulQuestion.of を質問キーと指示文で実行する
       * @then 64文字の質問キーを返す
       */
      @Test
      void キーが64文字の場合_そのキーを返す() {
        String key = "a".repeat(64);

        assertThat(NoulQuestion.of(key, "条件を満たすか").getKey()).isEqualTo(key);
      }

      /**
       * 質問キーが65文字の場合に、例外を投げることを確かめる。 API の上限を超えるキーを送信前に弾く仕様を守る。
       *
       * @given 質問キーが英字の65文字、指示文が "条件を満たすか"
       * @when NoulQuestion.of を質問キーと指示文で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void キーが65文字の場合_IllegalArgumentExceptionを投げる() {
        String key = "a".repeat(65);

        assertThatThrownBy(() -> NoulQuestion.of(key, "条件を満たすか"))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }
}
