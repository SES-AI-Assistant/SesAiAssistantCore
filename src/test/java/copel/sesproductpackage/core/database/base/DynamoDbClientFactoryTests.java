package copel.sesproductpackage.core.database.base;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class DynamoDbClientFactoryTests {

  @Test
  void testCreate_Lambda() throws Exception {
    withEnvironmentVariable("AWS_LAMBDA_FUNCTION_NAME", "test-lambda")
        .execute(
            () -> {
              assertNotNull(DynamoDbClientFactory.create());
            });
  }

  @Test
  void testCreate_CI() throws Exception {
    withEnvironmentVariable("CI", "true")
        .execute(
            () -> {
              assertNotNull(DynamoDbClientFactory.create());
            });
  }

  @Test
  void testCreate_Local() throws Exception {
    // No environment variables set, assuming no AWS credentials profile is configured for local
    // testing
    // to avoid exceptions from ProfileCredentialsProvider.
    // This test ensures the create method can be called without throwing an exception in a
    // local-like environment.
    withEnvironmentVariable("AWS_LAMBDA_FUNCTION_NAME", null)
        .and("CI", null)
        .execute(
            () -> {
              assertNotNull(DynamoDbClientFactory.create());
            });
  }

  @Test
  void testPrivateConstructor() {
    InvocationTargetException thrown =
        assertThrows(
            InvocationTargetException.class,
            () -> {
              java.lang.reflect.Constructor<DynamoDbClientFactory> constructor =
                  DynamoDbClientFactory.class.getDeclaredConstructor();
              constructor.setAccessible(true);
              constructor.newInstance();
            });
    assertEquals(UnsupportedOperationException.class, thrown.getTargetException().getClass());
  }
}
