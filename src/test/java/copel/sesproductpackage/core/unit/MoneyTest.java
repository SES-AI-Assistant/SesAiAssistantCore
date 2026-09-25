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
    assertTrue(Money.toSesUnitFromMan(new BigDecimal("999")).isNegotiable());
    assertTrue(Money.toSesUnitFromMan(new BigDecimal("9990000")).isNegotiable());
    assertFalse(Money.toSesUnitFromMan(new BigDecimal("80")).isNegotiable());
    assertFalse(new Money(1_000_000L).isNegotiable());
    assertFalse(Money.empty().isNegotiable());
    assertFalse(new Money(100).isNegotiable());
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

  // ================================================
  // 純粋な円受取（コンストラクタ）の検証（サニタイズ副作用なし）
  // ================================================

  @Test
  void testConstructor_PureYenWithoutSanitizeSideEffects() {
    // new Money(100) は 100円のままであり、サニタイズ（100万円への変換等）の副作用を受けないこと
    Money m100 = new Money(100);
    assertEquals(100L, m100.toYenValue());
    assertEquals(new BigDecimal("100"), m100.getValue());

    Money m100Bd = new Money(new BigDecimal("100"));
    assertEquals(100L, m100Bd.toYenValue());
    assertEquals(new BigDecimal("100"), m100Bd.getValue());

    // 80 も 80万円ではなく 80円のまま
    Money m80 = new Money(80);
    assertEquals(80L, m80.toYenValue());
    assertEquals(new BigDecimal("80"), m80.getValue());

    Money m80Bd = new Money(new BigDecimal("80"));
    assertEquals(80L, m80Bd.toYenValue());
    assertEquals(new BigDecimal("80"), m80Bd.getValue());
  }

  // ================================================
  // 万円単位からSES単位への変換ファクトリ（toSesUnitFromMan）自己修復テスト
  // ================================================

  @Test
  void testToSesUnitFromMan_Null() {
    Money moneyNull = Money.toSesUnitFromMan(null);
    assertTrue(moneyNull.isEmpty());
    assertNull(moneyNull.getValue());
  }

  @Test
  void testToSesUnitFromMan_ZeroAndNegative() {
    Money mZero = Money.toSesUnitFromMan(BigDecimal.ZERO);
    assertEquals(0L, mZero.toYenValue());

    Money mNegative = Money.toSesUnitFromMan(new BigDecimal("-100"));
    assertEquals(-100L, mNegative.toYenValue());
  }

  @Test
  void testToSesUnitFromMan_NegotiablePrice() {
    // 999（万円） -> 9,990,000円（NEGOTIABLE_PRICE定数オブジェクトと一致）
    Money m999 = Money.toSesUnitFromMan(new BigDecimal("999"));
    assertEquals(9990000L, m999.toYenValue());
    assertTrue(m999.isNegotiable());
    assertSame(Money.NEGOTIABLE_PRICE, m999);

    // 9,990,000（円） -> 9,990,000円
    Money mYen = Money.toSesUnitFromMan(new BigDecimal("9990000"));
    assertEquals(9990000L, mYen.toYenValue());
    assertTrue(mYen.isNegotiable());
    assertSame(Money.NEGOTIABLE_PRICE, mYen);
  }

  @Test
  void testToSesUnitFromMan_NormalManRange() {
    // 185 -> 1,850,000円
    Money m185 = Money.toSesUnitFromMan(new BigDecimal("185"));
    assertEquals(1850000L, m185.toYenValue());

    // 80 -> 800,000円
    Money m80 = Money.toSesUnitFromMan(new BigDecimal("80"));
    assertEquals(800000L, m80.toYenValue());

    // 10（境界値下限） -> 100,000円
    Money m10 = Money.toSesUnitFromMan(new BigDecimal("10"));
    assertEquals(100000L, m10.toYenValue());

    // 500（境界値上限） -> 5,000,000円
    Money m500 = Money.toSesUnitFromMan(new BigDecimal("500"));
    assertEquals(5000000L, m500.toYenValue());
  }

  @Test
  void testToSesUnitFromMan_DecimalManRange() {
    // 65.5 -> 655,000円
    Money m65_5 = Money.toSesUnitFromMan(new BigDecimal("65.5"));
    assertEquals(655000L, m65_5.toYenValue());
  }

  @Test
  void testToSesUnitFromMan_HourlyWageLeakage() {
    // 3,000 (160h換算: 480,000円)
    Money m3000 = Money.toSesUnitFromMan(new BigDecimal("3000"));
    assertEquals(480000L, m3000.toYenValue());

    // 1,500 (160h換算: 240,000円)
    Money m1500 = Money.toSesUnitFromMan(new BigDecimal("1500"));
    assertEquals(240000L, m1500.toYenValue());

    // 1,000（境界値下限: 160,000円）
    Money m1000 = Money.toSesUnitFromMan(new BigDecimal("1000"));
    assertEquals(160000L, m1000.toYenValue());

    // 8,000（境界値上限: 1,280,000円）
    Money m8000 = Money.toSesUnitFromMan(new BigDecimal("8000"));
    assertEquals(1280000L, m8000.toYenValue());
  }

  @Test
  void testToSesUnitFromMan_DailyWageLeakage() {
    // 25,000 (20日換算: 500,000円)
    Money m25000 = Money.toSesUnitFromMan(new BigDecimal("25000"));
    assertEquals(500000L, m25000.toYenValue());

    // 10,000（境界値下限: 200,000円）
    Money m10000 = Money.toSesUnitFromMan(new BigDecimal("10000"));
    assertEquals(200000L, m10000.toYenValue());

    // 50,000（境界値上限: 1,000,000円）
    Money m50000 = Money.toSesUnitFromMan(new BigDecimal("50000"));
    assertEquals(1000000L, m50000.toYenValue());
  }

  @Test
  void testToSesUnitFromMan_ExtraZeroRelief() {
    // 18,500,000 -> 1,850,000円 (ゼロ1個過剰)
    Money m18500000 = Money.toSesUnitFromMan(new BigDecimal("18500000"));
    assertEquals(1850000L, m18500000.toYenValue());

    // 10,000,000（境界値下限: 1,000,000円）
    Money m10000000 = Money.toSesUnitFromMan(new BigDecimal("10000000"));
    assertEquals(1000000L, m10000000.toYenValue());
  }

  @Test
  void testToSesUnitFromMan_AlreadyInYen() {
    // 800,000 -> 800,000円
    Money m800000 = Money.toSesUnitFromMan(new BigDecimal("800000"));
    assertEquals(800000L, m800000.toYenValue());

    // 100,000（境界値下限） -> 100,000円
    Money m100000 = Money.toSesUnitFromMan(new BigDecimal("100000"));
    assertEquals(100000L, m100000.toYenValue());

    // 5,000,000（境界値上限） -> 5,000,000円
    Money m5000000 = Money.toSesUnitFromMan(new BigDecimal("5000000"));
    assertEquals(5000000L, m5000000.toYenValue());
  }

  @Test
  void testToSesUnitFromMan_OtherRangeUnder1000() {
    // 600（500超〜1000未満: 万円換算 6,000,000円）
    Money m600 = Money.toSesUnitFromMan(new BigDecimal("600"));
    assertEquals(6000000L, m600.toYenValue());

    // 750 -> 7,500,000円
    Money m750 = Money.toSesUnitFromMan(new BigDecimal("750"));
    assertEquals(7500000L, m750.toYenValue());
  }

  @Test
  void testToSesUnitFromMan_OtherFallback() {
    // 8500（8000超〜10000未満: そのまま 8500）
    Money m8500 = Money.toSesUnitFromMan(new BigDecimal("8500"));
    assertEquals(8500L, m8500.toYenValue());
  }
}
