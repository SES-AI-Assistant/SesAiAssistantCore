package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Money クラスの単体テスト.
 *
 * @author Copel Co., Ltd.
 */
class MoneyTest {
  // ================================================
  // 出力フォーマットテスト
  // ================================================

  @Test
  void testToJapaneseFormat() {
    Money money = new Money(1300000);
    assertEquals("130万円", money.toJapaneseFormat());
  }

  @Test
  void testToJapaneseFormat_WithFraction() {
    Money money = new Money(new BigDecimal("1350000"));
    assertEquals("135万円", money.toJapaneseFormat());
  }

  @Test
  void testToManFormat() {
    Money money = new Money(1300000);
    assertEquals("130", money.toManFormat());
  }

  @Test
  void testToYenValue() {
    Money money = new Money(1300000);
    assertEquals(1300000L, money.toYenValue());
  }

  // ================================================
  // 比較テスト
  // ================================================

  @Test
  void testCompareTo_SameValue() {
    Money m1 = new Money(1000000);
    Money m2 = new Money(1000000);
    assertEquals(0, m1.compareTo(m2));
  }

  @Test
  void testCompareTo_LessThan() {
    Money m1 = new Money(1000000);
    Money m2 = new Money(1300000);
    assertTrue(m1.compareTo(m2) < 0);
  }

  @Test
  void testCompareTo_GreaterThan() {
    Money m1 = new Money(1300000);
    Money m2 = new Money(1000000);
    assertTrue(m1.compareTo(m2) > 0);
  }

  // ================================================
  // 状態判定テスト
  // ================================================
  @Test
  void testIsEmpty_False() {
    Money money = new Money(1000000);
    assertFalse(money.isEmpty());
  }

  @Test
  void testHasValue_True() {
    Money money = new Money(1000000);
    assertTrue(money.hasValue());
  }

  // ================================================
  // equals/hashCode テスト
  // ================================================

  @Test
  void testEquals_SameValue() {
    Money m1 = new Money(1000000);
    Money m2 = new Money(1000000);
    assertEquals(m1, m2);
  }

  @Test
  void testEquals_DifferentValue() {
    Money m1 = new Money(1000000);
    Money m2 = new Money(1300000);
    assertNotEquals(m1, m2);
  }

  @Test
  void testHashCode_Consistent() {
    Money m1 = new Money(1000000);
    Money m2 = new Money(1000000);
    assertEquals(m1.hashCode(), m2.hashCode());
  }

  // ================================================
  // エッジケーステスト
  // ================================================
  @Test
  void testToString() {
    Money money = new Money(1300000);
    assertEquals("130万円", money.toString());
  }

  // ================================================
  // 追加エッジケーステスト（100%カバレッジ）
  // ================================================
  @Test
  void testEquals_WithNonMoneyObject() {
    Money money = new Money(1000000);
    assertNotEquals(money, "not a money object");
  }

  @Test
  void testEquals_WithNull() {
    Money money = new Money(1000000);
    assertNotEquals(money, null);
  }

  @Test
  void testEquals_DifferentType() {
    Money money = new Money(1000000);
    assertNotEquals(money, 1000000);
  }

  @Test
  void testHashCode_WithValue() {
    Money m1 = new Money(1000000);
    Money m2 = new Money(1000000);
    assertEquals(m1.hashCode(), m2.hashCode());
  }
}
