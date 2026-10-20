package copel.sesproductpackage.core.api.gpt;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * API呼び出しのリトライ処理をサポートするヘルパークラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class ApiRetryHelper {
  /** 再試行の対象となる HTTP 状態コード（429: レート制限、503: 一時的な過負荷）. */
  private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 503);

  /** 最大リトライ回数. */
  private static final int MAX_RETRIES = 3;

  /** リトライ間隔（ミリ秒）: [500ms, 1000ms, 2000ms]. */
  private static final long[] RETRY_DELAYS = {500L, 1000L, 2000L};

  private ApiRetryHelper() {
    // ユーティリティクラスのため、インスタンス化を禁止
  }

  /**
   * Exponential backoff リトライ機構でAPI呼び出しを実行します.
   *
   * <p>503（Service Unavailable）と429（Rate Limit）のみリトライ対象とします。 その他の例外は即座に スロー、4xx系エラーはリトライなしでスロー。
   *
   * @param <T> 戻り値型
   * @param call リトライ対象の処理を実装した関数インターフェース
   * @return 呼び出し結果
   * @throws RuntimeException ランタイム例外（リトライ3回失敗後、または非リトライ対象）
   */
  public static <T> T executeWithRetry(final RetryableCall<T> call) throws RuntimeException {
    // 終了は最大回数に達したときの throw だけで行うため、条件を持たない for で書く
    for (int attempt = 1; ; attempt++) {
      try {
        return call.call();
      } catch (RuntimeException e) {
        // 再試行の対象は HTTP 状態コードで判定する（例外メッセージの文字列には依存しない）
        boolean retryable =
            e instanceof HttpStatusException status
                && RETRYABLE_STATUS_CODES.contains(status.getStatusCode());

        if (!retryable) {
          throw e;
        }

        // 最後のリトライであれば例外をスロー
        if (attempt == MAX_RETRIES) {
          log.warn("【API リトライ】最大リトライ回数（{}回）に達しました。処理を中止します", MAX_RETRIES);
          throw e;
        }

        // Exponential backoff で待機
        long delayMs = RETRY_DELAYS[attempt - 1];
        log.warn(
            "【API リトライ】試行 {}/{}: {}ms 後に再試行します。エラー内容: {}",
            attempt,
            MAX_RETRIES,
            delayMs,
            e.getMessage());

        try {
          Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.error("【API リトライ】待機中に割り込まれました", ie);
          throw new RuntimeException("API リトライ中に割り込まれました", ie);
        }
      }
    }
  }

  /** HTTP 応答がエラーだったことを、状態コード付きで表す例外. 再試行の判定に状態コードを使う。 */
  public static class HttpStatusException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** HTTP 状態コード. */
    private final int statusCode;

    /**
     * コンストラクタ.
     *
     * @param statusCode HTTP 状態コード
     * @param message 例外メッセージ
     */
    public HttpStatusException(final int statusCode, final String message) {
      super(message);
      this.statusCode = statusCode;
    }

    /**
     * HTTP 状態コードを返します.
     *
     * @return HTTP 状態コード
     */
    public int getStatusCode() {
      return statusCode;
    }
  }

  /**
   * リトライ対象の関数インターフェース.
   *
   * @param <T> 戻り値型
   */
  @FunctionalInterface
  public interface RetryableCall<T> {
    /**
     * API呼び出しを実行します.
     *
     * @return 呼び出し結果
     * @throws RuntimeException API例外またはIO例外をラップした例外
     */
    T call() throws RuntimeException;
  }
}
