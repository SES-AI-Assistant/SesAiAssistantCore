package copel.sesproductpackage.core.api.gpt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.HttpURLConnection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link HttpTimeout} の単体テスト。 */
class HttpTimeoutTest {

  @Nested
  class applyTo {

    @Nested
    class 正常系 {

      /**
       * 既定のタイムアウトを接続に設定すると、接続は5秒、読み取りは60秒になることを確かめる。 応答が止まった外部 API の呼び出しを、無期限に待たずに打ち切る仕様である。
       *
       * @given HTTP 接続のモックがある
       * @when DEFAULT の applyTo を接続で実行する
       * @then 接続のタイムアウトに 5000 を設定し、読み取りのタイムアウトに 60000 を設定する
       */
      @Test
      void 既定のタイムアウトを接続に設定する場合_接続と読み取りのタイムアウトの設定を更新する() {
        HttpURLConnection conn = mock(HttpURLConnection.class);

        HttpTimeout.DEFAULT.applyTo(conn);

        verify(conn).setConnectTimeout(5000);
        verify(conn).setReadTimeout(60000);
      }
    }
  }
}
