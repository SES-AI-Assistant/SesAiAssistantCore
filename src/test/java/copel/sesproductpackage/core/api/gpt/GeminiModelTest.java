package copel.sesproductpackage.core.api.gpt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link GeminiModel} の単体テスト。 */
class GeminiModelTest {

  @Nested
  class getModelName {

    @Nested
    class 正常系 {

      /**
       * 質問応答用のモデルがモデル名を返すことを確かめる。 Gemini の既定の質問応答モデルは、この列挙値のモデル名で指定するので、名前の対応を守る。
       *
       * @given GeminiModel の GEMINI_2_5_FLASH_LITE が存在する
       * @when getModelName を呼び出す
       * @then モデル名 gemini-2.5-flash-lite を返す
       */
      @Test
      void GEMINI_2_5_FLASH_LITEの場合_モデル名を返す() {
        assertThat(GeminiModel.GEMINI_2_5_FLASH_LITE.getModelName())
            .isEqualTo("gemini-2.5-flash-lite");
      }

      /**
       * エンベディング用のモデルがモデル名の接頭辞を含めて返すことを確かめる。 SDK は埋め込みモデルの名前に models/ の接頭辞を要求するので、接頭辞を含む名前を守る。
       *
       * @given GeminiModel の GEMINI_EMBEDDING_001 が存在する
       * @when getModelName を呼び出す
       * @then モデル名 models/gemini-embedding-001 を返す
       */
      @Test
      void GEMINI_EMBEDDING_001の場合_接頭辞付きのモデル名を返す() {
        assertThat(GeminiModel.GEMINI_EMBEDDING_001.getModelName())
            .isEqualTo("models/gemini-embedding-001");
      }
    }
  }
}
