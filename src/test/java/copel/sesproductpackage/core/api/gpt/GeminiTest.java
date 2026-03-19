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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class GeminiTest extends HttpTestBase {

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
    Gemini gemini = new Gemini("key");
    assertNotNull(gemini);
    assertNotNull(new Gemini("key", "model"));
    assertNotNull(new Gemini("key", GeminiModel.GEMINI_1_5_FLASH));
    assertTrue(gemini.canEqual(new Gemini("key2")));
  }

  @Test
  void testGenerate_NullOrBlank() throws Exception {
    Gemini gemini = new Gemini("key");
    assertNull(gemini.generate(null));
    assertNull(gemini.generate(""));
    assertNull(gemini.generate("   "));
  }

  @Test
  void testGenerateSuccess() throws Exception {
    String jsonResponse =
        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello from Gemini\"}]}}]}";
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");
    GptAnswer answer = gemini.generate("hello");
    assertEquals("Hello from Gemini", answer.getAnswer());

    String nullTextResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":null}]}}]}";
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream(nullTextResponse.getBytes()));
    gemini.generate("hello");
  }

  @Test
  void testGenerate_NoCandidatesOrParts() throws Exception {
    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    // candidates が空
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream("{\"candidates\":[]}".getBytes()));
    Gemini gemini = new Gemini("key");
    GptAnswer answer = gemini.generate("hello");
    assertNull(answer.getAnswer());

    // parts が空
    when(sharedMockConn.getInputStream())
        .thenReturn(
            new ByteArrayInputStream(
                "{\"candidates\":[{\"content\":{\"parts\":[]}}]}".getBytes()));
    answer = gemini.generate("hello");
    assertNull(answer.getAnswer());
  }

  @Test
  void testGenerate_ErrorCodes() throws Exception {
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> gemini.generate("hello"));
    assertTrue(e.getMessage().contains("400"));
    verify(sharedMockConn, atLeastOnce()).disconnect();

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
    e = assertThrows(RuntimeException.class, () -> gemini.generate("hello"));
    assertTrue(e.getMessage().contains("401"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    e = assertThrows(RuntimeException.class, () -> gemini.generate("hello"));
    assertTrue(e.getMessage().contains("403"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
    e = assertThrows(RuntimeException.class, () -> gemini.generate("hello"));
    assertTrue(e.getMessage().contains("404"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
    e = assertThrows(RuntimeException.class, () -> gemini.generate("hello"));
    assertTrue(e.getMessage().contains("408"));

    when(sharedMockConn.getResponseCode()).thenReturn(429);
    e = assertThrows(RuntimeException.class, () -> gemini.generate("hello"));
    assertTrue(e.getMessage().contains("429"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
    e = assertThrows(RuntimeException.class, () -> gemini.generate("hello"));
    assertTrue(e.getMessage().contains("500"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);
    e = assertThrows(RuntimeException.class, () -> gemini.generate("hello"));
    assertTrue(e.getMessage().contains("503"));

    when(sharedMockConn.getResponseCode()).thenReturn(418);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream("{\"candidates\":[]}".getBytes()));
    GptAnswer defaultAnswer = gemini.generate("hello");
    assertNull(defaultAnswer.getAnswer());
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
    assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f}, result);
  }

  @Test
  void testEmbedding_NullOrBlank() throws Exception {
    Gemini gemini = new Gemini("key");
    assertNull(gemini.embedding(null));
    assertNull(gemini.embedding(""));
    assertNull(gemini.embedding("   "));
  }

  @Test
  void testEmbedding_ErrorOrUnexpectedResponse() throws Exception {
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

    Gemini gemini = new Gemini("key");

    when(sharedMockConn.getResponseCode()).thenReturn(500);
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> gemini.embedding("hello"));
    assertTrue(e.getMessage().contains("Embedding API Error"));

    when(sharedMockConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(sharedMockConn.getInputStream())
        .thenReturn(new ByteArrayInputStream("{\"embedding\":{}}".getBytes()));
    assertNull(gemini.embedding("hello"));
  }
}
