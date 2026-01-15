package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class GeminiTests {
    private static String GEMINI_TEST_API_KEY = null;

    @Test
    void generateTest() throws IOException, RuntimeException {
        Gemini gemini = new Gemini(GEMINI_TEST_API_KEY, GeminiModel.GEMINI_2_5_FLASH_LITE);
        try {
            GptAnswer answer = gemini.generate("こんにちは");
            System.out.println(answer);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void embeddingTest() throws IOException, RuntimeException {
        Gemini gemini = new Gemini(GEMINI_TEST_API_KEY);
        try {
            float[] result = gemini.embedding("こんにちは");
            System.out.println(Arrays.toString(result));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
