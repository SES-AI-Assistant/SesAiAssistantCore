package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GptAnswerTests {

  private final Class<?> fakeTransformer = GptAnswerTests.class;

  @Test
  void testIsEmpty() {
    assertTrue(new GptAnswer(null, fakeTransformer).isEmpty());
    assertTrue(new GptAnswer("", fakeTransformer).isEmpty());
    assertTrue(new GptAnswer(" ", fakeTransformer).isEmpty());
    assertFalse(new GptAnswer("a", fakeTransformer).isEmpty());
  }

  @Test
  void testLength() {
    assertEquals(0, new GptAnswer(null, fakeTransformer).length());
    assertEquals(4, new GptAnswer("test", fakeTransformer).length());
  }

  @Test
  void testIsYES() {
    assertTrue(new GptAnswer("YES", fakeTransformer).isYES());
    assertTrue(new GptAnswer("はい", fakeTransformer).isYES());
    assertFalse(new GptAnswer("NO", fakeTransformer).isYES());
  }

  @Test
  void testIsNO() {
    assertTrue(new GptAnswer("NO", fakeTransformer).isNO());
    assertTrue(new GptAnswer("いいえ", fakeTransformer).isNO());
    assertFalse(new GptAnswer("YES", fakeTransformer).isNO());
  }

  @Test
  void testIsAlphanumeric() {
    assertTrue(new GptAnswer("abc123", fakeTransformer).isAlphanumeric());
    assertFalse(new GptAnswer("abc 123", fakeTransformer).isAlphanumeric());
    assertFalse(new GptAnswer(null, fakeTransformer).isAlphanumeric());
  }

  @Test
  void testIsAlphanumericWithSymbols() {
    assertTrue(new GptAnswer("a-b_c@123.!&", fakeTransformer).isAlphanumericWithSymbols());
    assertFalse(new GptAnswer("日本語", fakeTransformer).isAlphanumericWithSymbols());
    assertFalse(new GptAnswer(null, fakeTransformer).isAlphanumericWithSymbols());
  }

  @Test
  void testIsJapaneseOnly() {
    assertTrue(new GptAnswer("あいうえお漢字", fakeTransformer).isJapaneseOnly());
    assertFalse(new GptAnswer("abc", fakeTransformer).isJapaneseOnly());
    assertFalse(new GptAnswer(null, fakeTransformer).isJapaneseOnly());
  }

  @Test
  void testEqualsString() {
    assertTrue(new GptAnswer(null, fakeTransformer).equals(null));
    assertFalse(new GptAnswer(null, fakeTransformer).equals("word"));
    assertFalse(new GptAnswer("test", fakeTransformer).equals(null));
    assertFalse(new GptAnswer("test", fakeTransformer).equals("testing"));
    assertTrue(new GptAnswer("test", fakeTransformer).equals("test"));
  }

  @Test
  void testAsInt() {
    assertEquals(123, new GptAnswer("123", fakeTransformer).asInt());
    assertThrows(NumberFormatException.class, () -> new GptAnswer("abc", fakeTransformer).asInt());
  }

  @Test
  void testIsJsonArrayFormat() {
    assertTrue(new GptAnswer("[\"a\", \"b\"]", fakeTransformer).isJsonArrayFormat());
    assertFalse(new GptAnswer("{\"a\": \"b\"}", fakeTransformer).isJsonArrayFormat());
  }

  @Test
  void testGetAsList() throws JsonProcessingException {
    List<String> expected = List.of("a", "b");
    assertEquals(expected, new GptAnswer("[\"a\", \"b\"]", fakeTransformer).getAsList());

    // This should trigger the catch block by having unescaped control characters
    // like \n in the string literal for JSON
    List<String> expectedWithLn = List.of("a\nb");
    assertEquals(expectedWithLn, new GptAnswer("[\"a\nb\"]", fakeTransformer).getAsList());

    assertNull(new GptAnswer("not a list", fakeTransformer).getAsList());

    assertThrows(
        JsonProcessingException.class,
        () -> new GptAnswer("[\"a, \"b\"]", fakeTransformer).getAsList());
  }

  @Test
  void testToStringAndGetAnswer() {
    GptAnswer answer = new GptAnswer("test", fakeTransformer);
    assertEquals("test", answer.getAnswer());
    assertEquals("test", answer.toString());
  }
}
