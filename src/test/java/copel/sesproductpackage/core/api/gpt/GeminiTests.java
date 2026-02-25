package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

import copel.sesproductpackage.core.util.Properties;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class GeminiTests extends HttpTestBase {

  private MockedStatic<DynamoDbClient> mockedClient;
  private MockedStatic<DynamoDbEnhancedClient> mockedEnhancedClient;

  @BeforeAll
  @SuppressWarnings("unchecked")
  static void setupProperties() throws Exception {
    Field propertiesField = Properties.class.getDeclaredField("properties");
    propertiesField.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
    propertiesMap.put("GEMINI_COMPLETION_API_URL", "http://localhost/");
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

    DynamoDbTable mockTable = mock(DynamoDbTable.class);
    when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);

    sharedMockConn = mock(HttpURLConnection.class);
  }

  @AfterEach
  void tearDown() {
    mockedClient.close();
    mockedEnhancedClient.close();
  }

  @Test
  void testConstructors() {
    assertNotNull(new Gemini("key"));
    assertNotNull(new Gemini("key", "model"));
    assertNotNull(new Gemini("key", GeminiModel.GEMINI_1_5_FLASH));
  }

  @Test
  void testGenerateSuccess() throws Exception {
    String jsonResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello from Gemini\"}]}}]}";
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    GptAnswer answer = gemini.generate("hello");
    assertEquals("Hello from Gemini", answer.getAnswer());
  }

  @Test
  void testEmbeddingSuccess() throws Exception {
    String jsonResponse = "{\"embedding\":{\"values\":[0.1, 0.2, 0.3]}}";
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    float[] result = gemini.embedding("hello");
    assertArrayEquals(new float[] { 0.1f, 0.2f, 0.3f }, result);
  }

  @Test
  void testGenerateDefaultCode() throws Exception {
    String jsonResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}";
    when(sharedMockConn.getResponseCode()).thenReturn(201); // Default case
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    GptAnswer answer = gemini.generate("hello");
    assertEquals("Hello", answer.getAnswer());
  }

  @Test
  void testGenerateErrorCodes() throws Exception {
    int[] codes = { 400, 401, 403, 404, 408, 429, 500, 503 };
    for (int code : codes) {
      reset(sharedMockConn);
      when(sharedMockConn.getResponseCode()).thenReturn(code);
      when(sharedMockConn.getErrorStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
      when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

      Gemini gemini = new Gemini("key");
      assertThrows(
          RuntimeException.class, () -> gemini.generate("hello"), "Should throw for " + code);
    }
  }

  @Test
  void testEmbeddingErrorCodes() throws Exception {
    when(sharedMockConn.getResponseCode()).thenReturn(400);
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    assertThrows(RuntimeException.class, () -> gemini.embedding("hello"));
  }

  @Test
  void testGenerateEmptyCandidates() throws Exception {
    String jsonResponse = "{\"candidates\":[]}";
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    GptAnswer answer = gemini.generate("hello");
    assertNull(answer.getAnswer());
  }

  @Test
  void testGenerateEmptyParts() throws Exception {
    String jsonResponse = "{\"candidates\":[{\"content\":{\"parts\":[]}}]}";
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    GptAnswer answer = gemini.generate("hello");
    assertNull(answer.getAnswer());
  }

  @Test
  void testGenerateNullPrompt() throws Exception {
    String jsonResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}";
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    assertNull(gemini.generate(null));
    assertNull(gemini.generate(""));
    assertNull(gemini.generate("   "));
  }

  @Test
  void testEmbeddingNotArray() throws Exception {
    String jsonResponse = "{\"embedding\":{\"values\":{}}}";
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    assertNull(gemini.embedding("hello"));
  }

  @Test
  void testEmbeddingNullInputString() throws Exception {
    String jsonResponse = "{\"embedding\":{\"values\":[0.1]}}";
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    assertNull(gemini.embedding(null));
    assertNull(gemini.embedding(""));
    assertNull(gemini.embedding("   "));
  }
}
