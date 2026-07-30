package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * ApiRetryHelper のテストクラス.
 *
 * @author Copel Co., Ltd.
 */
class ApiRetryHelperTest {

  @Test
  void testExecuteWithRetry_Success() throws IOException {
    AtomicInteger callCount = new AtomicInteger(0);

    String result =
        ApiRetryHelper.executeWithRetry(
            () -> {
              callCount.incrementAndGet();
              return "Success";
            });

    assertEquals("Success", result);
    assertEquals(1, callCount.get());
  }

  @Test
  void testExecuteWithRetry_RetryOn503ThenSuccess() throws IOException {
    AtomicInteger callCount = new AtomicInteger(0);

    String result =
        ApiRetryHelper.executeWithRetry(
            () -> {
              int count = callCount.incrementAndGet();
              if (count == 1) {
                throw new RuntimeException("503 Service Unavailable: Server error");
              }
              return "Success after retry";
            });

    assertEquals("Success after retry", result);
    assertEquals(2, callCount.get());
  }

  @Test
  void testExecuteWithRetry_RetryOn429ThenSuccess() throws IOException {
    AtomicInteger callCount = new AtomicInteger(0);

    String result =
        ApiRetryHelper.executeWithRetry(
            () -> {
              int count = callCount.incrementAndGet();
              if (count <= 2) {
                throw new RuntimeException("429 Too Many Requests: Rate limit exceeded");
              }
              return "Success after retries";
            });

    assertEquals("Success after retries", result);
    assertEquals(3, callCount.get());
  }

  @Test
  void testExecuteWithRetry_FailAfterMaxRetries() throws IOException {
    AtomicInteger callCount = new AtomicInteger(0);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                ApiRetryHelper.executeWithRetry(
                    () -> {
                      callCount.incrementAndGet();
                      throw new RuntimeException("503 Service Unavailable: Persistent error");
                    }));

    assertTrue(exception.getMessage().contains("503"), "エラーメッセージに503を含む必要があります");
    assertEquals(3, callCount.get());
  }

  @Test
  void testExecuteWithRetry_NonRetryableError() throws IOException {
    AtomicInteger callCount = new AtomicInteger(0);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                ApiRetryHelper.executeWithRetry(
                    () -> {
                      callCount.incrementAndGet();
                      throw new RuntimeException("400 Bad Request: Invalid input");
                    }));

    assertTrue(exception.getMessage().contains("400"), "エラーメッセージに400を含む必要があります");
    assertEquals(1, callCount.get());
  }

  @Test
  void testExecuteWithRetry_NoRetryOn4xxErrors() throws IOException {
    AtomicInteger callCount = new AtomicInteger(0);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                ApiRetryHelper.executeWithRetry(
                    () -> {
                      callCount.incrementAndGet();
                      throw new RuntimeException("401 Unauthorized: Invalid API key");
                    }));

    assertEquals(1, callCount.get());
  }

  @Test
  void testExecuteWithRetry_RetryableErrorsOnly() throws IOException {
    AtomicInteger callCount = new AtomicInteger(0);

    String result =
        ApiRetryHelper.executeWithRetry(
            () -> {
              int count = callCount.incrementAndGet();
              if (count == 1) {
                throw new RuntimeException("503 Service Unavailable: Maintenance");
              }
              if (count == 2) {
                throw new RuntimeException("429 Too Many Requests: Rate limit");
              }
              return "OK";
            });

    assertEquals("OK", result);
    assertEquals(3, callCount.get());
  }
}
