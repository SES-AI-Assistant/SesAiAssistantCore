package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class OpenAiModelTest {

  @Test
  void testGetModelName() {
    assertEquals("gpt-4", OpenAiModel.GPT_4.getModelName());
  }

  @Test
  void testFromModelName() {
    assertEquals(OpenAiModel.GPT_3_5_TURBO, OpenAiModel.fromModelName("gpt-3.5-turbo"));
    assertNull(OpenAiModel.fromModelName("invalid"));
  }
}
