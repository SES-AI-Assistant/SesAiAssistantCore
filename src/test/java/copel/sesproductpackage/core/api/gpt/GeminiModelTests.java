package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GeminiModelTests {

  @Test
  void testGetModelName() {
    assertEquals("gemini-1.5-pro", GeminiModel.GEMINI_1_5_PRO.getModelName());
  }

  @Test
  void testFromModelName() {
    assertEquals(GeminiModel.GEMINI_1_5_FLASH, GeminiModel.fromModelName("gemini-1.5-flash"));
    assertNull(GeminiModel.fromModelName("invalid"));
  }

  @Test
  void testEnumMethods() {
    assertNotNull(GeminiModel.valueOf("GEMINI_1_5_PRO"));
    assertTrue(GeminiModel.values().length > 0);
  }
}
