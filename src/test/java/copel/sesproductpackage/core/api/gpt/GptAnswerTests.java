package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GptAnswerTests {

  @Test
  public void testConstructor() {
    GptAnswer answer = new GptAnswer("YES", GptAnswer.class);
    assertNotNull(answer);
    assertEquals("YES", answer.getAnswer());
  }

  @Test
  public void testIsEmpty() {
    GptAnswer answer = new GptAnswer("", GptAnswer.class);
    assertTrue(answer.isEmpty());

    answer = new GptAnswer(" ", GptAnswer.class);
    assertTrue(answer.isEmpty());

    answer = new GptAnswer("Valid Answer", GptAnswer.class);
    assertFalse(answer.isEmpty());
  }

  @Test
  public void testLength() {
    GptAnswer answer = new GptAnswer("Test", GptAnswer.class);
    assertEquals(4, answer.length());

    answer = new GptAnswer("", GptAnswer.class);
    assertEquals(0, answer.length());

    answer = new GptAnswer(null, GptAnswer.class);
    assertEquals(0, answer.length());
  }

  @Test
  public void testIsYES() {
    GptAnswer answer = new GptAnswer("YES", GptAnswer.class);
    assertTrue(answer.isYES());

    answer = new GptAnswer("yes.", GptAnswer.class);
    assertTrue(answer.isYES());

    answer = new GptAnswer("no", GptAnswer.class);
    assertFalse(answer.isYES());

    answer = new GptAnswer(null, GptAnswer.class);
    assertFalse(answer.isYES());
  }

  @Test
  public void testIsNO() {
    GptAnswer answer = new GptAnswer("NO", GptAnswer.class);
    assertTrue(answer.isNO());

    answer = new GptAnswer("no.", GptAnswer.class);
    assertTrue(answer.isNO());

    answer = new GptAnswer("yes", GptAnswer.class);
    assertFalse(answer.isNO());

    answer = new GptAnswer(null, GptAnswer.class);
    assertFalse(answer.isNO());
  }

  @Test
  public void testIsAlphanumeric() {
    GptAnswer answer = new GptAnswer("123ABC", GptAnswer.class);
    assertTrue(answer.isAlphanumeric());

    answer = new GptAnswer("123 ABC", GptAnswer.class);
    assertFalse(answer.isAlphanumeric());

    answer = new GptAnswer("!@#123", GptAnswer.class);
    assertFalse(answer.isAlphanumeric());

    answer = new GptAnswer(null, GptAnswer.class);
    assertFalse(answer.isAlphanumeric());
  }

  @Test
  public void testIsAlphanumericWithSymbols() {
    GptAnswer answer = new GptAnswer("123ABC!@#", GptAnswer.class);
    assertTrue(answer.isAlphanumericWithSymbols());

    answer = new GptAnswer("123 ABC", GptAnswer.class);
    assertFalse(answer.isAlphanumericWithSymbols());

    answer = new GptAnswer("123ABC", GptAnswer.class);
    assertTrue(answer.isAlphanumericWithSymbols());

    answer = new GptAnswer(null, GptAnswer.class);
    assertFalse(answer.isAlphanumericWithSymbols());
  }

  @Test
  public void testIsJapaneseOnly() {
    GptAnswer answer = new GptAnswer("こんにちは", GptAnswer.class);
    assertTrue(answer.isJapaneseOnly());

    answer = new GptAnswer("hello", GptAnswer.class);
    assertFalse(answer.isJapaneseOnly());

    answer = new GptAnswer(null, GptAnswer.class);
    assertFalse(answer.isJapaneseOnly());
  }

  @Test
  public void testEquals() {
    GptAnswer answer = new GptAnswer("Test", GptAnswer.class);
    assertTrue(answer.equals("Test"));
    assertFalse(answer.equals("NotTest"));

    answer = new GptAnswer(null, GptAnswer.class);
    assertTrue(answer.equals(null));
    assertFalse(answer.equals("Not null"));
  }

  @Test
  public void testToString() {
    GptAnswer answer = new GptAnswer("Test", GptAnswer.class);
    assertEquals("Test", answer.toString());
  }

  @Test
  public void testAsInt() {
    GptAnswer answer = new GptAnswer("123", GptAnswer.class);
    assertEquals(123, answer.asInt());
  }

  @Test
  public void testIsJsonArrayFormat() {
    GptAnswer answer = new GptAnswer("[\"item1\", \"item2\"]", GptAnswer.class);
    assertTrue(answer.isJsonArrayFormat());

    answer = new GptAnswer("[\n\"item1\", \"item2\"]", GptAnswer.class);
    assertTrue(answer.isJsonArrayFormat());

    answer = new GptAnswer("not a json array", GptAnswer.class);
    assertFalse(answer.isJsonArrayFormat());
  }

  @Test
  public void testGetAsList() throws Exception {
    GptAnswer answer = new GptAnswer("[\"\nitem1\", \"item2\"]", GptAnswer.class);
    List<String> result = answer.getAsList();
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(Arrays.asList("\nitem1", "item2"), result);

    answer = new GptAnswer("not a json array", GptAnswer.class);
    assertNull(answer.getAsList());
  }
}
