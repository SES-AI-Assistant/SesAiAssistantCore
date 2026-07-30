package copel.sesproductpackage.core.api.gpt;

import lombok.extern.slf4j.Slf4j;

/**
 * API呼び出しのリトライ処理をサポートするヘルパークラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class ApiRetryHelper {
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
    RuntimeException lastRuntimeException = null;

    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        return call.call();
      } catch (RuntimeException e) {
        lastRuntimeException = e;
        // リトライ対象のHTTPエラーコードかチェック
        if (!isRetryableError(e)) {
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

    // このコードに到達することはありませんが、コンパイラのためにここに記載
    if (lastRuntimeException != null) {
      throw lastRuntimeException;
    }
    throw new RuntimeException("API呼び出し失敗");
  }

  /**
   * エラーが再試行可能な種別かを判定します.
   *
   * <p>503（Service Unavailable）と429（Rate Limit）をリトライ対象と判定。
   *
   * @param e 判定対象の例外
   * @return リトライ対象の場合は true
   */
  private static boolean isRetryableError(final RuntimeException e) {
    String message = e.getMessage();
    if (message == null) {
      return false;
    }

    // 503 Service Unavailable（API側のメンテナンスや負荷過高）
    if (message.contains("503")) {
      return true;
    }

    // 429 Too Many Requests（レート制限）
    if (message.contains("429")) {
      return true;
    }

    return false;
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
