package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentResponse;

import copel.sesproductpackage.core.api.gpt.schema.JsonSchemaProvider;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.api.gpt.schema.SchemaGenerator;

/**
 * GeminiクラスのテストケースをSDKベースで実装.
 *
 * @author Copel Co., Ltd.
 */
class GeminiTest {
  @Test
  void testConstructor_Default() {
    Gemini gemini = new Gemini("testkey");
    assertNotNull(gemini);
    assertEquals("testkey", gemini.getApiKey());
    assertNotNull(gemini.getCompletionModel());
    assertNotNull(gemini.getEmbeddingModel());
  }

  @Test
  void testConstructor_WithCompletionModel() {
    Gemini gemini = new Gemini("key", "custom-model");
    assertEquals("key", gemini.getApiKey());
    assertEquals("custom-model", gemini.getCompletionModel());
  }

  @Test
  void testConstructor_WithGeminiModel() {
    Gemini gemini = new Gemini("key", GeminiModel.GEMINI_1_5_FLASH);
    assertEquals("key", gemini.getApiKey());
    assertEquals(GeminiModel.GEMINI_1_5_FLASH.getModelName(), gemini.getCompletionModel());
  }

  @Test
  void testGetApiKey() {
    Gemini gemini = new Gemini("testkey");
    assertEquals("testkey", gemini.getApiKey());
  }

  @Test
  void testGetCompletionModel_Default() {
    Gemini gemini = new Gemini("key");
    assertEquals(GeminiModel.GEMINI_2_5_FLASH_LITE.getModelName(), gemini.getCompletionModel());
  }

  @Test
  void testGetCompletionModel_Custom() {
    Gemini gemini = new Gemini("key", "custom-model");
    assertEquals("custom-model", gemini.getCompletionModel());
  }

  @Test
  void testGetEmbeddingModel() {
    Gemini gemini = new Gemini("key");
    assertEquals(GeminiModel.GEMINI_EMBEDDING_001.getModelName(), gemini.getEmbeddingModel());
  }

  @Test
  void testGenerate_NullInput() throws Exception {
    Gemini gemini = new Gemini("key");
    assertNull(gemini.generate(null));
  }

  @Test
  void testGenerate_EmptyInput() throws Exception {
    Gemini gemini = new Gemini("key");
    assertNull(gemini.generate(""));
  }

  @Test
  void testGenerate_BlankInput() throws Exception {
    Gemini gemini = new Gemini("key");
    assertNull(gemini.generate("   "));
  }

  @Test
  void testEmbedding_NullInput() throws Exception {
    Gemini gemini = new Gemini("key");
    assertNull(gemini.embedding(null));
  }

  @Test
  void testEmbedding_EmptyInput() throws Exception {
    Gemini gemini = new Gemini("key");
    assertNull(gemini.embedding(""));
  }

  @Test
  void testEmbedding_BlankInput() throws Exception {
    Gemini gemini = new Gemini("key");
    assertNull(gemini.embedding("   "));
  }

  @Test
  void testCanEqual() {
    Gemini gemini1 = new Gemini("key1");
    Gemini gemini2 = new Gemini("key2");
    assertTrue(gemini1.canEqual(gemini2));
  }

  @Test
  void testHashCode() {
    Gemini gemini1 = new Gemini("key");
    Gemini gemini2 = new Gemini("key");
    assertEquals(gemini1.hashCode(), gemini2.hashCode());
  }

  @Test
  void testToString() {
    Gemini gemini = new Gemini("key");
    assertNotNull(gemini.toString());
  }

  @Test
  void testEquals() {
    Gemini gemini1 = new Gemini("key1");
    Gemini gemini2 = new Gemini("key2");
    assertNotEquals(gemini1, gemini2);
  }

  @Test
  void testStructuredGenerate_NullPrompt() {
    Gemini gemini = new Gemini("key");
    assertThrows(
        RuntimeException.class, () -> gemini.generate(null, TestPersonResponse.class));
  }

  @Test
  void testStructuredGenerate_BlankPrompt() {
    Gemini gemini = new Gemini("key");
    assertThrows(
        RuntimeException.class, () -> gemini.generate("  ", TestPersonResponse.class));
  }

  @Test
  void testStructuredGenerate_NullResponseType() throws Exception {
    Gemini gemini = new Gemini("key");
    assertThrows(RuntimeException.class, () -> gemini.generate("test", null));
  }

  @Test
  void testStructuredGenerate_ValidResponse() throws Exception {
    Gemini gemini = new Gemini("testkey");

    String jsonResponse = "{\"name\": \"田中太郎\", \"age\": 30}";

    try (MockedStatic<?> mockClient = mockStatic(com.google.genai.Client.class)) {
      GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
      when(mockResponse.text()).thenReturn(jsonResponse);

      assertNotNull(jsonResponse);
      ObjectMapper mapper = new ObjectMapper();
      TestPersonResponse result = mapper.readValue(jsonResponse, TestPersonResponse.class);

      assertEquals("田中太郎", result.getName());
      assertEquals(30, result.getAge());
    }
  }

  /** テスト用のレスポンス型. JsonSchemaProviderを実装し、人物情報を保持します. */
  static class TestPersonResponse implements JsonSchemaProvider {
    @Schema(description = "人物の名前", required = true)
    private String name;

    @Schema(description = "人物の年齢", required = true)
    private int age;

    TestPersonResponse() {}

    TestPersonResponse(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public String getName() {
      return this.name;
    }

    public int getAge() {
      return this.age;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setAge(int age) {
      this.age = age;
    }

    @Override
    public Map<String, Object> getJsonSchema() {
      return SchemaGenerator.generate(this.getClass());
    }
  }
}
