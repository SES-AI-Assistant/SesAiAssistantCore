package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;

/** {@link AwsClientRetry} の単体テスト。 */
class AwsClientRetryTest {

  @Nested
  class overrideConfiguration {

    @Nested
    class 正常系 {

      /**
       * 再試行を 4 回にした標準の方式と、タイムアウトを付けた設定を返すことを確かめる。 既定の 3 回では、同時のコールドスタートによる Parameter Store
       * の制限を越えられない。
       *
       * @given 再試行の設定を作る前の状態
       * @when overrideConfiguration を実行する
       * @then 方式が STANDARD、再試行が 4 回、1 回の呼び出しが 5 秒、全体が 8 秒の設定を返す
       */
      @Test
      void 設定を作る場合_再試行を4回にした標準の方式を返す() {
        ClientOverrideConfiguration configuration = AwsClientRetry.overrideConfiguration();

        RetryPolicy policy = configuration.retryPolicy().orElseThrow();
        assertThat(policy.retryMode()).isEqualTo(RetryMode.STANDARD);
        assertThat(policy.numRetries()).isEqualTo(4);
        assertThat(configuration.apiCallAttemptTimeout()).contains(Duration.ofSeconds(5));
        assertThat(configuration.apiCallTimeout()).contains(Duration.ofSeconds(8));
      }
    }
  }
}
