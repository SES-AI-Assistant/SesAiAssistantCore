package copel.sesproductpackage.core.api.gpt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link OpenAiModel} の単体テスト。 */
class OpenAiModelTest {

  @Nested
  class getModelName {

    @Nested
    class 正常系 {

      /**
       * 質問応答用のモデルがモデル名を返すことを確かめる。 呼び出し先の API に渡すモデル名が列挙値の定義どおりであることを守る。
       *
       * @given OpenAiModel の GPT_4_TURBO が存在する
       * @when getModelName を呼び出す
       * @then モデル名 gpt-4-turbo を返す
       */
      @Test
      void GPT_4_TURBOの場合_モデル名を返す() {
        assertThat(OpenAiModel.GPT_4_TURBO.getModelName()).isEqualTo("gpt-4-turbo");
      }

      /**
       * エンベディング用のモデルがモデル名を返すことを確かめる。 エンベディングの呼び出しは、この列挙値のモデル名をそのまま使うことを守る。
       *
       * @given OpenAiModel の TEXT_EMBEDDING_ADA_002 が存在する
       * @when getModelName を呼び出す
       * @then モデル名 text-embedding-ada-002 を返す
       */
      @Test
      void TEXT_EMBEDDING_ADA_002の場合_モデル名を返す() {
        assertThat(OpenAiModel.TEXT_EMBEDDING_ADA_002.getModelName())
            .isEqualTo("text-embedding-ada-002");
      }
    }
  }
}
