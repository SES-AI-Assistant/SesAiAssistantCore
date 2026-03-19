package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class GptCoverageEnhancedTest extends HttpTestBase {

  private MockedStatic<DynamoDbClient> mockedClient;
  private MockedStatic<DynamoDbEnhancedClient> mockedEnhancedClient;

  @BeforeAll
  static void setupOnce() throws Exception {
    // URLs are provided by the test-only Properties replacement.
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setup() {
    mockedClient = mockStatic(DynamoDbClient.class);
    mockedEnhancedClient = mockStatic(DynamoDbEnhancedClient.class);

    DynamoDbClient mockDbClient = mock(DynamoDbClient.class);
    DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
    when(mockBuilder.region(any())).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockDbClient);
    mockedClient.when(DynamoDbClient::builder).thenReturn(mockBuilder);

    DynamoDbEnhancedClient mockEnhanced = mock(DynamoDbEnhancedClient.class);
    DynamoDbEnhancedClient.Builder mockEnhancedBuilder = mock(DynamoDbEnhancedClient.Builder.class);
    when(mockEnhancedBuilder.dynamoDbClient(any())).thenReturn(mockEnhancedBuilder);
    when(mockEnhancedBuilder.build()).thenReturn(mockEnhanced);
    mockedEnhancedClient.when(DynamoDbEnhancedClient::builder).thenReturn(mockEnhancedBuilder);

    DynamoDbTable<Object> mockTable = mock(DynamoDbTable.class, RETURNS_DEEP_STUBS);
    when(mockEnhanced.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);

    sharedMockConn = mock(HttpURLConnection.class);
  }

  @AfterEach
  void tearDown() {
    mockedClient.close();
    mockedEnhancedClient.close();
  }

  private void setupMock(int code, String response) throws Exception {
    reset(sharedMockConn);
    when(sharedMockConn.getResponseCode()).thenReturn(code);
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    if (code == 200 || code == 201 || code == 999) {
      when(sharedMockConn.getInputStream())
          .thenReturn(new ByteArrayInputStream(response.getBytes()));
    } else {
      when(sharedMockConn.getErrorStream())
          .thenReturn(new ByteArrayInputStream(response.getBytes()));
    }
  }

  @Test
  void testOpenAINullAndDefaults() throws Exception {
    OpenAI api = new OpenAI("key");
    assertNull(api.embedding(null));
    assertNull(api.generate(null));

    setupMock(200, "{\"data\":[{\"embedding\":[0.1, 0.2]}]}");
    assertNotNull(api.embedding("test"));

    setupMock(200, "{\"choices\":[{\"message\":{\"content\":\"ans\"}}]}");
    assertNotNull(api.generate("test").getAnswer());
  }

  @Test
  void testOpenAIErrorHandling() throws Exception {
    OpenAI api = new OpenAI("key");
    int[] errorCodes = {400, 401, 403, 404, 408, 429, 500, 503};
    for (int code : errorCodes) {
      setupMock(code, "error");
      assertThrows(RuntimeException.class, () -> api.embedding("test"));
      setupMock(code, "error");
      assertThrows(RuntimeException.class, () -> api.generate("test"));
    }
  }

  @Test
  void testGeminiEdgeCases() throws Exception {
    Gemini api = new Gemini("key");
    assertNull(api.embedding(null));
    assertNull(api.generate(null));

    setupMock(400, "{\"error\":\"bad\"}");
    assertThrows(RuntimeException.class, () -> api.generate("test"));

    setupMock(500, "{\"error\":\"fail\"}");
    assertThrows(RuntimeException.class, () -> api.embedding("test"));

    setupMock(200, "{\"embedding\":{\"values\":[0.1, 0.2]}}");
    assertNotNull(api.embedding("test"));

    setupMock(200, "{\"embedding\":{\"values\":\"not-array\"}}");
    assertNull(api.embedding("test"));

    setupMock(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ans\"}]}}]}");
    assertNotNull(api.generate("test").getAnswer());

    setupMock(200, "{\"candidates\":\"not-array\"}");
    assertNull(api.generate("test").getAnswer());
  }
}
