package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GenderTest {

  @Test
  void testToJapanese() {
    assertEquals("男性", Gender.Man.toJapanese());
    assertEquals("女性", Gender.Woman.toJapanese());
    assertEquals("不明", Gender.Unknown.toJapanese());
  }

  @Test
  void testFromJapanese() {
    assertEquals(Gender.Man, Gender.fromJapanese("男性"));
    assertEquals(Gender.Woman, Gender.fromJapanese("女性"));
    assertEquals(Gender.Unknown, Gender.fromJapanese("不明"));
  }

  @Test
  void testRoundTrip() {
    for (Gender gender : Gender.values()) {
      assertEquals(gender, Gender.fromJapanese(gender.toJapanese()));
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "Man", "Woman", "Unknown", "M", "F", "男", "男性 "})
  void testFromJapaneseThrowsForInvalidValue(String value) {
    assertThrows(IllegalArgumentException.class, () -> Gender.fromJapanese(value));
  }
}
