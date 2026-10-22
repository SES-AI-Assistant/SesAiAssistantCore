package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ParsedMessage} の単体テスト。 */
class ParsedMessageTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 引数なしで生成した場合、各項目が未設定であることを確かめる。 解析前の状態を空として扱わないと解析結果と混同するので、未設定の状態にする。
       *
       * @given 引数を指定しない
       * @when 生成する
       * @then 種別、要約、単価が null のインスタンスを返す
       */
      @Test
      void 引数なしで生成した場合_各項目が未設定のインスタンスを返す() {
        ParsedMessage message = new ParsedMessage();

        assertThat(message.getType()).isNull();
        assertThat(message.getSummary()).isNull();
        assertThat(message.getUnitPrice()).isNull();
      }
    }
  }

  @Nested
  class コンストラクタ_String_String_Money {

    @Nested
    class 正常系 {

      /**
       * 種別、要約、単価を指定した場合、その値を保持することを確かめる。 解析結果を後続の処理へそのまま渡さないと単価や種別が失われるので、値を保持する。
       *
       * @given 種別が "JOB"、要約が "案件概要"、単価が 100万円
       * @when 生成する
       * @then 指定した種別、要約、単価を保持するインスタンスを返す
       */
      @Test
      void 種別と要約と単価を指定した場合_指定した値を保持するインスタンスを返す() {
        Money unitPrice = new Money(1_000_000L);

        ParsedMessage message = new ParsedMessage("JOB", "案件概要", unitPrice);

        assertThat(message.getType()).isEqualTo("JOB");
        assertThat(message.getSummary()).isEqualTo("案件概要");
        assertThat(message.getUnitPrice()).isEqualTo(unitPrice);
      }
    }
  }
}
