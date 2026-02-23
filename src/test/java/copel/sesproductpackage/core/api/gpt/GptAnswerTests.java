package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GptAnswerTests {

    private final Class<?> FAKE_TRANSFORMER = GptAnswerTests.class;

    @Test
    void testIsEmpty() {
        assertTrue(new GptAnswer(null, FAKE_TRANSFORMER).isEmpty());
        assertTrue(new GptAnswer("", FAKE_TRANSFORMER).isEmpty());
        assertTrue(new GptAnswer(" ", FAKE_TRANSFORMER).isEmpty());
        assertFalse(new GptAnswer("a", FAKE_TRANSFORMER).isEmpty());
    }

    @Test
    void testLength() {
        assertEquals(0, new GptAnswer(null, FAKE_TRANSFORMER).length());
        assertEquals(4, new GptAnswer("test", FAKE_TRANSFORMER).length());
    }

    @Test
    void testIsYES() {
        assertTrue(new GptAnswer("YES", FAKE_TRANSFORMER).isYES());
        assertTrue(new GptAnswer("はい", FAKE_TRANSFORMER).isYES());
        assertFalse(new GptAnswer("NO", FAKE_TRANSFORMER).isYES());
    }

    @Test
    void testIsNO() {
        assertTrue(new GptAnswer("NO", FAKE_TRANSFORMER).isNO());
        assertTrue(new GptAnswer("いいえ", FAKE_TRANSFORMER).isNO());
        assertFalse(new GptAnswer("YES", FAKE_TRANSFORMER).isNO());
    }

    @Test
    void testIsAlphanumeric() {
        assertTrue(new GptAnswer("abc123", FAKE_TRANSFORMER).isAlphanumeric());
        assertFalse(new GptAnswer("abc 123", FAKE_TRANSFORMER).isAlphanumeric());
        assertFalse(new GptAnswer(null, FAKE_TRANSFORMER).isAlphanumeric());
    }

    @Test
    void testIsAlphanumericWithSymbols() {
        assertTrue(new GptAnswer("a-b_c@123.!&", FAKE_TRANSFORMER).isAlphanumericWithSymbols());
        assertFalse(new GptAnswer("日本語", FAKE_TRANSFORMER).isAlphanumericWithSymbols());
        assertFalse(new GptAnswer(null, FAKE_TRANSFORMER).isAlphanumericWithSymbols());
    }
    
    @Test
    void testIsJapaneseOnly() {
        assertTrue(new GptAnswer("あいうえお漢字", FAKE_TRANSFORMER).isJapaneseOnly());
        assertFalse(new GptAnswer("abc", FAKE_TRANSFORMER).isJapaneseOnly());
        assertFalse(new GptAnswer(null, FAKE_TRANSFORMER).isJapaneseOnly());
    }

    @Test
    void testEqualsString() {
        assertTrue(new GptAnswer("test", FAKE_TRANSFORMER).equals("test"));
        assertFalse(new GptAnswer("test", FAKE_TRANSFORMER).equals("testing"));
        assertTrue(new GptAnswer(null, FAKE_TRANSFORMER).equals(null));
        assertFalse(new GptAnswer(null, FAKE_TRANSFORMER).equals("word"));
    }

    @Test
    void testAsInt() {
        assertEquals(123, new GptAnswer("123", FAKE_TRANSFORMER).asInt());
        assertThrows(NumberFormatException.class, () -> new GptAnswer("abc", FAKE_TRANSFORMER).asInt());
    }

    @Test
    void testIsJsonArrayFormat() {
        assertTrue(new GptAnswer("[\"a\", \"b\"]", FAKE_TRANSFORMER).isJsonArrayFormat());
        assertFalse(new GptAnswer("{\"a\": \"b\"}", FAKE_TRANSFORMER).isJsonArrayFormat());
    }

    @Test
    void testGetAsList() throws JsonProcessingException {
        List<String> expected = List.of("a", "b");
        assertEquals(expected, new GptAnswer("[\"a\", \"b\"]", FAKE_TRANSFORMER).getAsList());
        
        assertEquals(expected, new GptAnswer("[\"a\", \n\"b\"]", FAKE_TRANSFORMER).getAsList());
        
        assertNull(new GptAnswer("not a list", FAKE_TRANSFORMER).getAsList());
        
        assertThrows(JsonProcessingException.class, () -> new GptAnswer("[\"a, \"b\"]", FAKE_TRANSFORMER).getAsList());
    }
}
