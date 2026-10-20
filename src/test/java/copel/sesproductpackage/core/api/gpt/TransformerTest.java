package copel.sesproductpackage.core.api.gpt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link Transformer} の単体テスト。 */
class TransformerTest {

  @Nested
  class generate_String_Class {

    @Nested
    class 異常系 {

      /**
       * 構造化出力を実装していない変換器が、既定の実装で例外を投げることを確かめる。
       * 実装していない機能を黙って空の結果で返すと、呼び出し元が誤った結果を使ってしまうので、例外で知らせる仕様を守る。
       *
       * @given 構造化出力を上書きしていない変換器 StubTransformer が存在する
       * @when generate を prompt と String.class で呼び出す
       * @then UnsupportedOperationException を投げ、例外のメッセージに StubTransformer を含む
       */
      @Test
      void 構造化出力を実装していない場合_UnsupportedOperationExceptionを投げる() {
        Transformer transformer = new StubTransformer();

        assertThatThrownBy(() -> transformer.generate("質問", String.class))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("StubTransformer");
      }
    }
  }

  /** 構造化出力を上書きしない最小の変換器。 */
  static class StubTransformer implements Transformer {

    @Override
    public float[] embedding(final String inputString) throws IOException {
      return new float[0];
    }

    @Override
    public GptAnswer generate(final String prompt) throws IOException {
      return new GptAnswer(prompt, StubTransformer.class);
    }
  }
}
