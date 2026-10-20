package copel.sesproductpackage.core.api.gpt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ApiRetryHelper} の単体テスト。 */
class ApiRetryHelperTest {

  @Nested
  class executeWithRetry {

    @Nested
    class 正常系 {

      /**
       * 初回の呼び出しが成功した場合に結果を返すことを確かめる。 成功した呼び出しを繰り返すと外部 API の利用量が無駄に増えるため、初回で戻すことを守る。
       *
       * @given 呼び出しが "OK" を返す
       * @when executeWithRetry を呼び出し関数付きで実行する
       * @then "OK" を返し、呼び出し回数が1回である
       */
      @Test
      void 初回の呼び出しが成功した場合_結果を返す() {
        AtomicInteger calls = new AtomicInteger();

        String result =
            ApiRetryHelper.executeWithRetry(
                () -> {
                  calls.incrementAndGet();
                  return "OK";
                });

        assertThat(result).isEqualTo("OK");
        assertThat(calls).hasValue(1);
      }

      /**
       * 503 が1回発生した後の呼び出しが成功した場合に、2回目の結果を返すことを確かめる。 一時的な過負荷では再試行で成功させ、処理を止めないことを守る。
       *
       * @given 1回目の呼び出しが 503 を含む例外を投げ、2回目の呼び出しが "OK" を返す
       * @when executeWithRetry を呼び出し関数付きで実行する
       * @then "OK" を返し、呼び出し回数が2回である
       */
      @Test
      void ステータス503が1回発生した後に成功する場合_2回目の結果を返す() {
        AtomicInteger calls = new AtomicInteger();

        String result =
            ApiRetryHelper.executeWithRetry(
                () -> {
                  if (calls.incrementAndGet() == 1) {
                    throw new ApiRetryHelper.HttpStatusException(503, "503 Service Unavailable");
                  }
                  return "OK";
                });

        assertThat(result).isEqualTo("OK");
        assertThat(calls).hasValue(2);
      }

      /**
       * 429 が1回発生した後の呼び出しが成功した場合に、2回目の結果を返すことを確かめる。 レート制限も再試行の対象であり、503 と同じ扱いにすることを守る。
       *
       * @given 1回目の呼び出しが 429 を含む例外を投げ、2回目の呼び出しが "OK" を返す
       * @when executeWithRetry を呼び出し関数付きで実行する
       * @then "OK" を返し、呼び出し回数が2回である
       */
      @Test
      void ステータス429が1回発生した後に成功する場合_2回目の結果を返す() {
        AtomicInteger calls = new AtomicInteger();

        String result =
            ApiRetryHelper.executeWithRetry(
                () -> {
                  if (calls.incrementAndGet() == 1) {
                    throw new ApiRetryHelper.HttpStatusException(429, "429 Too Many Requests");
                  }
                  return "OK";
                });

        assertThat(result).isEqualTo("OK");
        assertThat(calls).hasValue(2);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 再試行の対象外の例外は再試行せずに、そのまま投げることを確かめる。 4xx 系の誤ったリクエストは同じ内容で再送しても成功しないため、即座に失敗させることを守る。
       *
       * @given 呼び出しが 400 を含む例外を投げる
       * @when executeWithRetry を呼び出し関数付きで実行する
       * @then 同じ例外を投げ、呼び出し回数が1回である
       */
      @Test
      void 再試行の対象外の例外が発生した場合_再試行せずに例外を投げる() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(
                () ->
                    ApiRetryHelper.executeWithRetry(
                        () -> {
                          calls.incrementAndGet();
                          throw new RuntimeException("400 Bad Request");
                        }))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("400 Bad Request");
        assertThat(calls).hasValue(1);
      }

      /**
       * メッセージを持たない例外は再試行の対象外として、そのまま投げることを確かめる。 メッセージの判定で NullPointerException
       * が起きると本来の例外が隠れるので、null を許容する仕様を守る。
       *
       * @given 呼び出しが メッセージを持たない RuntimeException を投げる
       * @when executeWithRetry を呼び出し関数付きで実行する
       * @then 同じ例外を投げ、呼び出し回数が1回である
       */
      @Test
      void メッセージがnullの例外が発生した場合_再試行せずに例外を投げる() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException cause = new RuntimeException((String) null);

        assertThatThrownBy(
                () ->
                    ApiRetryHelper.executeWithRetry(
                        () -> {
                          calls.incrementAndGet();
                          throw cause;
                        }))
            .isSameAs(cause);
        assertThat(calls).hasValue(1);
      }

      /**
       * 503 が最大回数まで続いた場合に、最後の例外を投げることを確かめる。 再試行を無限に続けると Lambda の実行時間を超えるため、上限で打ち切ることを守る。
       *
       * @given 呼び出しが毎回 503 を含む例外を投げる
       * @when executeWithRetry を呼び出し関数付きで実行する
       * @then 最後の例外を投げ、呼び出し回数が3回である
       */
      @Test
      void ステータス503が最大回数まで続いた場合_最後の例外を投げる() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(
                () ->
                    ApiRetryHelper.executeWithRetry(
                        () -> {
                          calls.incrementAndGet();
                          throw new ApiRetryHelper.HttpStatusException(
                              503, "503 Service Unavailable");
                        }))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("503 Service Unavailable");
        assertThat(calls).hasValue(3);
      }

      /**
       * 待機中にスレッドが中断された場合に、例外を投げて割り込み状態を保持することを確かめる。 中断を握りつぶすと停止要求が呼び出し元に伝わらないので、割り込み状態を保持する仕様を守る。
       *
       * @given 呼び出しが割り込み状態を設定したうえで 503 を含む例外を投げる
       * @when executeWithRetry を呼び出し関数付きで実行する
       * @then API リトライ中に割り込まれたことを示す例外を投げ、スレッドの割り込み状態が true である
       */
      @Test
      void 待機中にスレッドが割り込まれた場合_割り込み状態を保持して例外を投げる() {
        try {
          assertThatThrownBy(
                  () ->
                      ApiRetryHelper.executeWithRetry(
                          () -> {
                            Thread.currentThread().interrupt();
                            throw new ApiRetryHelper.HttpStatusException(
                                503, "503 Service Unavailable");
                          }))
              .isInstanceOf(RuntimeException.class)
              .hasMessage("API リトライ中に割り込まれました")
              .hasCauseInstanceOf(InterruptedException.class);
          assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
          // 後続のテストに割り込み状態を残さないように解除する
          Thread.interrupted();
        }
      }

      /**
       * 状態コードを持たない例外は、メッセージに 503 を含んでいても再試行せずに投げることを確かめる。 再試行の判定を状態コードだけで行い、文字列の偶然の一致で再送しないことを守る。
       *
       * @given 呼び出しがメッセージに 503 を含む RuntimeException を投げる
       * @when executeWithRetry を呼び出し関数付きで実行する
       * @then 同じ例外を投げ、呼び出し回数が1回である
       */
      @Test
      void 状態コードを持たない例外の場合_メッセージに503を含んでも再試行せずに例外を投げる() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException failure = new RuntimeException("503 Service Unavailable");

        assertThatThrownBy(
                () ->
                    ApiRetryHelper.executeWithRetry(
                        () -> {
                          calls.incrementAndGet();
                          throw failure;
                        }))
            .isSameAs(failure);
        assertThat(calls).hasValue(1);
      }
    }
  }
}
