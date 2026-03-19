package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class OpenAITest extends HttpTestBase {

  private MockedStatic<DynamoDbClient> mockedClient;
  private MockedStatic<DynamoDbEnhancedClient> mockedEnhancedClient;
  private DynamoDbTable<Object> mockTable;

  @BeforeAll
  @SuppressWarnings("unchecked")
  static void setupProperties() throws Exception {
    Field propertiesField = Properties.class.getDeclaredField("properties");
    propertiesField.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
    propertiesMap.put("OPEN_AI_EMBEDDING_API_URL", "http://localhost/embedding");
    propertiesMap.put("OPEN_AI_EMBEDDING_MODEL", "text-embedding-3-small");
    propertiesMap.put("OPEN_AI_COMPLETION_API_URL", "http://localhost/completion");
    propertiesMap.put("OPEN_AI_COMPLETION_TEMPERATURE", "0.7");
    propertiesMap.put("OPEN_AI_FILE_UPLOAD_URL", "http://localhost/upload");
    propertiesMap.put("OPEN_AI_FINE_TUNE_URL", "http://localhost/finetune");
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setupMocks() {
    mockedClient = mockStatic(DynamoDbClient.class);
    mockedEnhancedClient = mockStatic(DynamoDbEnhancedClient.class);

    DynamoDbClient mockDbClient = mock(DynamoDbClient.class);
    DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
    when(mockBuilder.region(any())).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockDbClient);
    mockedClient.when(DynamoDbClient::builder).thenReturn(mockBuilder);

    DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
    DynamoDbEnhancedClient.Builder mockEnhancedBuilder = mock(DynamoDbEnhancedClient.Builder.class);
    when(mockEnhancedBuilder.dynamoDbClient(any())).thenReturn(mockEnhancedBuilder);
    when(mockEnhancedBuilder.build()).thenReturn(mockEnhancedClient);
    mockedEnhancedClient.when(DynamoDbEnhancedClient::builder).thenReturn(mockEnhancedBuilder);

    mockTable = mock(DynamoDbTable.class);
    when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);

    PageIterable<Object> mockPageIterable = mock(PageIterable.class);
    SdkIterable<Object> mockSdkIterable = mock(SdkIterable.class);
    when(mockTable.query(
            any(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.class)))
        .thenReturn(mockPageIterable);
    when(mockPageIterable.items()).thenReturn(mockSdkIterable);
    when(mockSdkIterable.iterator()).thenReturn(java.util.Collections.emptyIterator());

    sharedMockConn = mock(HttpURLConnection.class);
  }

  @AfterEach
  void tearDown() {
    mockedClient.close();
    mockedEnhancedClient.close();
  }

  @Test
  void testEmbeddingSuccess() throws Exception {
    String jsonResponse = "{\"data\":[{\"embedding\":[0.1, 0.2]}]}";
    when(sharedMockConn.getResponseCode()).thenReturn(200);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    OpenAI api = new OpenAI("key");
    float[] result = api.embedding("test");
    assertArrayEquals(new float[] {0.1f, 0.2f}, result);
  }

  @Test
  void testGenerateSuccess() throws Exception {
    String jsonResponse = "{\"choices\":[{\"message\":{\"content\":\"Hello\"}}]}";
    when(sharedMockConn.getResponseCode()).thenReturn(200);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    OpenAI api = new OpenAI("key");
    GptAnswer answer = api.generate("hi");
    assertEquals("Hello", answer.getAnswer());

    assertNull(api.generate("hi", null));
    assertNull(api.generate(null, 0.5f));
  }

  @Test
  void testFineTuningSuccess() throws Exception {
    String uploadResponse = "{\"id\":\"file-123\"}";
    when(sharedMockConn.getResponseCode()).thenReturn(200, 200);
    when(sharedMockConn.getInputStream())
        .thenReturn(
            new ByteArrayInputStream(uploadResponse.getBytes()),
            new ByteArrayInputStream("{}".getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    OpenAI api = new OpenAI("key");
    api.fineTuning("data");
    verify(sharedMockConn, atLeastOnce()).getOutputStream();
  }
}
