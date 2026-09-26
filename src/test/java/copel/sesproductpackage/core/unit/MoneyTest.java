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

  // ================================================
  // 定数テスト
  // ================================================
  @Test
  void testNegotiablePrice() {
    assertNotNull(Money.NEGOTIABLE_PRICE);
    assertEquals(9_990_000L, Money.NEGOTIABLE_PRICE.toYenValue());
    assertEquals("999万円", Money.NEGOTIABLE_PRICE.toJapaneseFormat());
    assertEquals("999", Money.NEGOTIABLE_PRICE.toManFormat());
    assertEquals(new BigDecimal(9_990_000L), Money.NEGOTIABLE_PRICE.getValue());
    assertTrue(Money.NEGOTIABLE_PRICE.hasValue());
    assertFalse(Money.NEGOTIABLE_PRICE.isEmpty());
  }

  @Test
  void testIsNegotiable() {
    assertTrue(Money.NEGOTIABLE_PRICE.isNegotiable());
    assertTrue(new Money(9_990_000L).isNegotiable());
    assertFalse(new Money(1_000_000L).isNegotiable());
    assertFalse(Money.empty().isNegotiable());
  }

  // ================================================
  // 比較メソッド（isGreaterThan, isGreaterThanOrEqualTo, isLessThan, isLessThanOrEqualTo）テスト
  // ================================================
  @Test
  void testIsGreaterThan() {
    Money m100 = new Money(1000000);
    Money m80 = new Money(800000);
    Money mSame = new Money(1000000);
    Money mEmpty = Money.empty();

    // 大小関係
    assertTrue(m100.isGreaterThan(m80));
    assertFalse(m80.isGreaterThan(m100));
    assertFalse(m100.isGreaterThan(mSame));

    // null / empty 境界値
    assertFalse(m100.isGreaterThan(null));
    assertFalse(m100.isGreaterThan(mEmpty));
    assertFalse(mEmpty.isGreaterThan(m100));
    assertFalse(mEmpty.isGreaterThan(null));
    assertFalse(mEmpty.isGreaterThan(mEmpty));
  }

  @Test
  void testIsGreaterThanOrEqualTo() {
    Money m100 = new Money(1000000);
    Money m80 = new Money(800000);
    Money mSame = new Money(1000000);
    Money mEmpty = Money.empty();

    // 大小関係・同額
    assertTrue(m100.isGreaterThanOrEqualTo(m80));
    assertFalse(m80.isGreaterThanOrEqualTo(m100));
    assertTrue(m100.isGreaterThanOrEqualTo(mSame));

    // null / empty 境界値
    assertFalse(m100.isGreaterThanOrEqualTo(null));
    assertFalse(m100.isGreaterThanOrEqualTo(mEmpty));
    assertFalse(mEmpty.isGreaterThanOrEqualTo(m100));
    assertFalse(mEmpty.isGreaterThanOrEqualTo(null));
    assertFalse(mEmpty.isGreaterThanOrEqualTo(mEmpty));
  }

  @Test
  void testIsLessThan() {
    Money m100 = new Money(1000000);
    Money m80 = new Money(800000);
    Money mSame = new Money(1000000);
    Money mEmpty = Money.empty();

    // 大小関係
    assertFalse(m100.isLessThan(m80));
    assertTrue(m80.isLessThan(m100));
    assertFalse(m100.isLessThan(mSame));

    // null / empty 境界値
    assertFalse(m100.isLessThan(null));
    assertFalse(m100.isLessThan(mEmpty));
    assertFalse(mEmpty.isLessThan(m100));
    assertFalse(mEmpty.isLessThan(null));
    assertFalse(mEmpty.isLessThan(mEmpty));
  }

  @Test
  void testIsLessThanOrEqualTo() {
    Money m100 = new Money(1000000);
    Money m80 = new Money(800000);
    Money mSame = new Money(1000000);
    Money mEmpty = Money.empty();

    // 大小関係・同額
    assertFalse(m100.isLessThanOrEqualTo(m80));
    assertTrue(m80.isLessThanOrEqualTo(m100));
    assertTrue(m100.isLessThanOrEqualTo(mSame));

    // null / empty 境界値
    assertFalse(m100.isLessThanOrEqualTo(null));
    assertFalse(m100.isLessThanOrEqualTo(mEmpty));
    assertFalse(mEmpty.isLessThanOrEqualTo(m100));
    assertFalse(mEmpty.isLessThanOrEqualTo(null));
    assertFalse(mEmpty.isLessThanOrEqualTo(mEmpty));
  }
}
