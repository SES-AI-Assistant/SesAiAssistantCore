package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GptCoverageEnhancedTest extends HttpTestBase {

  @BeforeEach
  void setup() {
    sharedMockConn = mock(HttpURLConnection.class);
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

    setupMock(201, "{\"data\":[{\"embedding\":[0.1, 0.2]}]}");
    assertNotNull(api.embedding("test"));

    setupMock(201, "{\"choices\":[{\"message\":{\"content\":\"ans\"}}]}");
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

    // Default code 999
    setupMock(999, "{\"data\":[{\"embedding\":[0.1, 0.2]}]}");
    api.embedding("test");
    setupMock(999, "{\"choices\":[{\"message\":{\"content\":\"ans\"}}]}");
    api.generate("test");
  }

  @Test
  void testGeminiEdgeCases() throws Exception {
    Gemini api = new Gemini("key");
    assertNull(api.embedding(null));
    assertNull(api.generate(null));

    // Error with body
    setupMock(400, "{\"error\":\"bad\"}");
    assertThrows(RuntimeException.class, () -> api.generate("test"));

    setupMock(500, "{\"error\":\"fail\"}");
    assertThrows(RuntimeException.class, () -> api.embedding("test"));

    // JSON paths
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
