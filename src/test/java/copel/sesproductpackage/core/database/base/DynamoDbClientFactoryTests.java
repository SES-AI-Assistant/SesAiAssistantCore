package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class DynamoDbClientFactoryTests {

  @Test
  void testCreate_Lambda() throws Exception {
    DynamoDbClient mockClient = mock(DynamoDbClient.class);
    DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
    when(mockBuilder.region(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.regions.Region.class))).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.auth.credentials.AwsCredentialsProvider.class))).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockClient);

    try (MockedStatic<DynamoDbClient> mockedDynamoDbClient = mockStatic(DynamoDbClient.class)) {
      mockedDynamoDbClient.when(DynamoDbClient::builder).thenReturn(mockBuilder);

      assertNotNull(DynamoDbClientFactory.create());
    }
  }

  @Test
  void testCreate_CI() throws Exception {
    DynamoDbClient mockClient = mock(DynamoDbClient.class);
    DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
    when(mockBuilder.region(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.regions.Region.class))).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.auth.credentials.AwsCredentialsProvider.class))).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockClient);

    try (MockedStatic<DynamoDbClient> mockedDynamoDbClient = mockStatic(DynamoDbClient.class)) {
      mockedDynamoDbClient.when(DynamoDbClient::builder).thenReturn(mockBuilder);

      assertNotNull(DynamoDbClientFactory.create());
    }
  }

  @Test
  void testCreate_Local() throws Exception {
    DynamoDbClient mockClient = mock(DynamoDbClient.class);
    DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
    when(mockBuilder.region(any(software.amazon.awssdk.regions.Region.class))).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(any(software.amazon.awssdk.auth.credentials.AwsCredentialsProvider.class))).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockClient);

    try (MockedStatic<DynamoDbClient> mockedDynamoDbClient = mockStatic(DynamoDbClient.class)) {
      mockedDynamoDbClient.when(DynamoDbClient::builder).thenReturn(mockBuilder);

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
