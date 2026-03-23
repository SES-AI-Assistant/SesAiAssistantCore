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
    verify(sharedMockConn, atLeastOnce()).disconnect();
  }

  @Test
  void testEmbedding_Null() throws Exception {
    OpenAI api = new OpenAI("key");
    assertNull(api.embedding(null));
  }

  @Test
  void testEmbedding_ErrorCodes() throws Exception {
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    OpenAI api = new OpenAI("key");

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
    RuntimeException e = assertThrows(RuntimeException.class, () -> api.embedding("test"));
    assertTrue(e.getMessage().contains("400"));
    verify(sharedMockConn, atLeastOnce()).disconnect();

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
    e = assertThrows(RuntimeException.class, () -> api.embedding("test"));
    assertTrue(e.getMessage().contains("401"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    e = assertThrows(RuntimeException.class, () -> api.embedding("test"));
    assertTrue(e.getMessage().contains("403"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
    e = assertThrows(RuntimeException.class, () -> api.embedding("test"));
    assertTrue(e.getMessage().contains("404"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
    e = assertThrows(RuntimeException.class, () -> api.embedding("test"));
    assertTrue(e.getMessage().contains("408"));

    when(sharedMockConn.getResponseCode()).thenReturn(429);
    e = assertThrows(RuntimeException.class, () -> api.embedding("test"));
    assertTrue(e.getMessage().contains("429"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
    e = assertThrows(RuntimeException.class, () -> api.embedding("test"));
    assertTrue(e.getMessage().contains("500"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);
    e = assertThrows(RuntimeException.class, () -> api.embedding("test"));
    assertTrue(e.getMessage().contains("503"));
  }

  @Test
  void testConstructorWithModel() {
    OpenAI api = new OpenAI("key", "gpt-4");
    assertNotNull(api);
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
  void testGenerate_ContentNullAndDefaultResponseCode() throws Exception {
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    // checkResponseCode の default 分岐（例: 418）でも readResponse が走り、content が null の場合は null を返す
    when(sharedMockConn.getResponseCode()).thenReturn(418);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream("{\"choices\":[{\"message\":{}}]}".getBytes()));

    OpenAI api = new OpenAI("key");
    GptAnswer answer = api.generate("hi");
    assertNull(answer.getAnswer());
    verify(sharedMockConn, atLeastOnce()).disconnect();
  }

  @Test
  void testGenerate_ErrorCodes() throws Exception {
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    OpenAI api = new OpenAI("key");

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
    RuntimeException e = assertThrows(RuntimeException.class, () -> api.generate("hi"));
    assertTrue(e.getMessage().contains("401"));
    verify(sharedMockConn, atLeastOnce()).disconnect();

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
    e = assertThrows(RuntimeException.class, () -> api.generate("hi"));
    assertTrue(e.getMessage().contains("500"));
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

  @Test
  void testFineTuning_ErrorOnJobStart() throws Exception {
    String uploadResponse = "{\"id\":\"file-123\"}";
    // 1回目: upload OK, 2回目: fine-tune job start NG
    when(sharedMockConn.getResponseCode()).thenReturn(200, 500);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(uploadResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    OpenAI api = new OpenAI("key");
    RuntimeException e = assertThrows(RuntimeException.class, () -> api.fineTuning("data"));
    assertTrue(e.getMessage().contains("Fine-tuning Error"));
  }
}
