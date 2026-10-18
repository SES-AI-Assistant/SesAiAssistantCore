package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ExperienceDuration の単体テスト.
 *
 * @author Copel Co., Ltd.
 */
class ExperienceDurationTest {

  @Test
  @DisplayName("isEmpty: null・0以下は未設定、正の月数は設定済みと判定されること")
  void testIsEmpty() {
    assertTrue(new ExperienceDuration(null).isEmpty());
    assertTrue(new ExperienceDuration(0).isEmpty());
    assertTrue(new ExperienceDuration(-1).isEmpty());
    assertFalse(new ExperienceDuration(1).isEmpty());
  }

  @Test
  @DisplayName("toJapaneseFormat: 月数が「N年」「Nヶ月」「N年Nヶ月」形式に変換されること")
  void testToJapaneseFormat() {
    assertEquals("3年", new ExperienceDuration(36).toJapaneseFormat());
    assertEquals("6ヶ月", new ExperienceDuration(6).toJapaneseFormat());
    assertEquals("1年6ヶ月", new ExperienceDuration(18).toJapaneseFormat());
    assertEquals("", new ExperienceDuration(null).toJapaneseFormat());
    assertEquals("", new ExperienceDuration(0).toJapaneseFormat());
  }

  @Test
  @DisplayName("toSuffix: 設定済みの場合は区切り文字付き、未設定の場合は空文字が返ること")
  void testToSuffix() {
    assertEquals(": 3年", new ExperienceDuration(36).toSuffix(": "));
    assertEquals("：6ヶ月", new ExperienceDuration(6).toSuffix("："));
    assertEquals("", new ExperienceDuration(null).toSuffix(": "));
  }
}
