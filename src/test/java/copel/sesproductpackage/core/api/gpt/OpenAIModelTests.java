package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OpenAIModelTests {

    @Test
    void testGetModelName() {
        assertEquals("gpt-4", OpenAIModel.GPT_4.getModelName());
    }

    @Test
    void testFromModelName() {
        assertEquals(OpenAIModel.GPT_3_5_TURBO, OpenAIModel.fromModelName("gpt-3.5-turbo"));
        assertNull(OpenAIModel.fromModelName("invalid"));
    }
}
