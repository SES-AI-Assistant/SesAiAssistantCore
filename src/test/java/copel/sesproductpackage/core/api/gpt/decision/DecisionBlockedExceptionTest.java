package copel.sesproductpackage.core.api.gpt.decision;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link DecisionBlockedException} の単体テスト。 */
class DecisionBlockedExceptionTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 例外に指定したメッセージを保持することを確かめる。 呼び出し側がブロックの理由をログに残せるようにする仕様を守る。
       *
       * @given メッセージが "安全性のフィルタでブロックされた" の例外
       * @when getMessage を呼び出す
       * @then メッセージ "安全性のフィルタでブロックされた" を返す
       */
      @Test
      void メッセージを指定した場合_そのメッセージを返す() {
        DecisionBlockedException exception = new DecisionBlockedException("安全性のフィルタでブロックされた");

        assertThat(exception.getMessage()).isEqualTo("安全性のフィルタでブロックされた");
      }
    }
  }
}
