package copel.sesproductpackage.core.util;

import java.time.Duration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;

/**
 * 起動時に設定を読む AWS クライアント（Parameter Store・Secrets Manager・S3）の再試行の設定.
 *
 * <p>Lambda のコールドスタートが同時に多数起きると、Parameter Store が「Rate exceeded」を返して初期化に失敗する（#392）。 既定の再試行は 3
 * 回までで足りないため、再試行を増やして一時的な制限を越えられるようにする。待ち時間は、Lambda の初期化（INIT）の上限（10 秒）と API Gateway の上限（29
 * 秒）に収まりやすいよう、4 回の再試行（最大 5 回の呼び出し）にとどめ、1 回の呼び出しと全体にタイムアウトを付ける。
 *
 * @author Copel Co., Ltd.
 */
public final class AwsClientRetry {

  /** インスタンス化を禁止する。すべての操作は静的メンバーで提供する。 */
  private AwsClientRetry() {}

  /** 再試行の最大回数。最初の呼び出しと合わせて最大 5 回呼び出す。 */
  static final int MAX_RETRIES = 4;

  /** 1 回の呼び出しを待つ最長の時間。応答が返らないまま待ち続けて、初期化の上限を超えないようにする。 */
  static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(5);

  /** 再試行を含む呼び出し全体を待つ最長の時間。ページごとの呼び出しが、初期化の上限に収まりやすいようにする。 */
  static final Duration CALL_TIMEOUT = Duration.ofSeconds(8);

  /**
   * AWS クライアントのビルダーに渡す、再試行の設定を作ります.
   *
   * <p>標準の再試行方式（指数バックオフとジッター）のまま、再試行の回数を {@link #MAX_RETRIES} にし、タイムアウトを {@link #ATTEMPT_TIMEOUT} と
   * {@link #CALL_TIMEOUT} にする。
   *
   * @return クライアントのビルダーの {@code overrideConfiguration} に渡す設定
   */
  public static ClientOverrideConfiguration overrideConfiguration() {
    return ClientOverrideConfiguration.builder()
        .retryPolicy(RetryPolicy.builder(RetryMode.STANDARD).numRetries(MAX_RETRIES).build())
        .apiCallAttemptTimeout(ATTEMPT_TIMEOUT)
        .apiCallTimeout(CALL_TIMEOUT)
        .build();
  }
}
