package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import copel.sesproductpackage.core.util.EnvUtils;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DynamoDbClientFactoryTest {

  @Test
  void testCreate_Lambda() throws Exception {
    try (MockedStatic<EnvUtils> mockedEnv = mockStatic(EnvUtils.class)) {
      mockedEnv.when(() -> EnvUtils.get("AWS_LAMBDA_FUNCTION_NAME")).thenReturn("test-lambda");
      assertNotNull(DynamoDbClientFactory.create());
    }
  }

  @Test
  void testCreate_CI() throws Exception {
    try (MockedStatic<EnvUtils> mockedEnv = mockStatic(EnvUtils.class)) {
      mockedEnv.when(() -> EnvUtils.get("CI")).thenReturn("true");
      assertNotNull(DynamoDbClientFactory.create());
    }
  }

  @Test
  void testCreate_Local() throws Exception {
    try (MockedStatic<EnvUtils> mockedEnv = mockStatic(EnvUtils.class)) {
      mockedEnv.when(() -> EnvUtils.get("AWS_LAMBDA_FUNCTION_NAME")).thenReturn(null);
      mockedEnv.when(() -> EnvUtils.get("CI")).thenReturn(null);
      assertNotNull(DynamoDbClientFactory.create());
    }
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
