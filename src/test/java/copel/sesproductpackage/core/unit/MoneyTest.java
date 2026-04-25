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
  // 案件の単価抽出テスト（MAX値）
  // ================================================

  @Test
  void testExtractJobUnitPrice_SimpleRange() {
    String content = "■単価：100-130万円";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1300000"), money.getValue());
    assertEquals("130万円", money.toJapaneseFormat());
  }

  @Test
  void testExtractJobUnitPrice_WithTilde() {
    String content = "■単価：～130万円（スキル見合い）";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1300000"), money.getValue());
  }

  @Test
  void testExtractJobUnitPrice_SingleValue() {
    String content = "■単価：100万円";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
  }

  @Test
  void testExtractJobUnitPrice_MaxFormat() {
    String content = "■単価：MAX70万円";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("700000"), money.getValue());
  }

  @Test
  void testExtractJobUnitPrice_WithParen() {
    String content = "■単価：100-130万円（100%稼働時）";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1300000"), money.getValue());
  }

  @Test
  void testExtractJobUnitPrice_SkillBased() {
    String content = "■単価：スキル見合い";
    Money money = Money.extractJobUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractJobUnitPrice_NotYetFixed() {
    String content = "■単価：精算確認中（スキル見合い）";
    Money money = Money.extractJobUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractJobUnitPrice_Empty() {
    String content = "";
    Money money = Money.extractJobUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractJobUnitPrice_Null() {
    Money money = Money.extractJobUnitPrice(null);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractJobUnitPrice_YenUnit() {
    String content = "■単価：1,000,000円";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
  }

  // ================================================
  // 要員の単価抽出テスト（MIN値）
  // ================================================

  @Test
  void testExtractPersonUnitPrice_SimpleRange() {
    String content = "■単価：100-120万円";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
    assertEquals("100万円", money.toJapaneseFormat());
  }

  @Test
  void testExtractPersonUnitPrice_WithTilde() {
    String content = "■単価：70～75万円";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("700000"), money.getValue());
  }

  @Test
  void testExtractPersonUnitPrice_SingleValue() {
    String content = "■単価：70万円";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("700000"), money.getValue());
  }

  @Test
  void testExtractPersonUnitPrice_WithParen() {
    String content = "■単価：100-120万円（1人月）";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
  }

  @Test
  void testExtractPersonUnitPrice_SkillBased() {
    String content = "■単価：スキル見合い";
    Money money = Money.extractPersonUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractPersonUnitPrice_SkillWithNumber() {
    String content = "■単価：スキル見合い　70-75万";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("700000"), money.getValue());
  }

  @Test
  void testExtractPersonUnitPrice_MultipleConditions() {
    String content = "■単価：100-120万円（1人月）、50-60万円（0.5人月）";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
  }

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

  @Test
  void testToYenValue_Empty() {
    Money money = Money.empty();
    assertEquals(0L, money.toYenValue());
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

  @Test
  void testCompareTo_WithEmpty() {
    Money m1 = Money.empty();
    Money m2 = new Money(1000000);
    assertTrue(m1.compareTo(m2) < 0);
  }

  @Test
  void testCompareTo_BothEmpty() {
    Money m1 = Money.empty();
    Money m2 = Money.empty();
    assertEquals(0, m1.compareTo(m2));
  }

  // ================================================
  // 状態判定テスト
  // ================================================

  @Test
  void testIsEmpty_True() {
    Money money = Money.empty();
    assertTrue(money.isEmpty());
  }

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

  @Test
  void testHasValue_False() {
    Money money = Money.empty();
    assertFalse(money.hasValue());
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
  void testEquals_BothEmpty() {
    Money m1 = Money.empty();
    Money m2 = Money.empty();
    assertEquals(m1, m2);
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
  void testExtractJobUnitPrice_DecimalValue() {
    String content = "■単価：100.5-130.8万円";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1300000"), money.getValue());
  }

  @Test
  void testExtractPersonUnitPrice_DecimalValue() {
    String content = "■単価：100.5-120.8万円";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
  }

  @Test
  void testExtractJobUnitPrice_MultipleRanges() {
    String content = "■単価：50-60万、100-130万（100%稼働時）";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1300000"), money.getValue());
  }

  @Test
  void testToString() {
    Money money = new Money(1300000);
    assertEquals("130万円", money.toString());
  }

  @Test
  void testToString_Empty() {
    Money money = Money.empty();
    assertEquals("empty", money.toString());
  }

  // ================================================
  // 追加エッジケーステスト（100%カバレッジ）
  // ================================================

  @Test
  void testExtractJobUnitPrice_NoUnitPriceSection() {
    String content = "何か別の内容があります";
    Money money = Money.extractJobUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractPersonUnitPrice_EmptyString() {
    String content = "";
    Money money = Money.extractPersonUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractPersonUnitPrice_NoUnitPriceSection() {
    String content = "何か別の内容があります";
    Money money = Money.extractPersonUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testToJapaneseFormat_Empty() {
    Money money = Money.empty();
    assertNull(money.toJapaneseFormat());
  }

  @Test
  void testToManFormat_Empty() {
    Money money = Money.empty();
    assertNull(money.toManFormat());
  }

  @Test
  void testCompareTo_GreaterThanEmpty() {
    Money m1 = new Money(1000000);
    Money m2 = Money.empty();
    assertTrue(m1.compareTo(m2) > 0);
  }

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
  void testHashCode_Empty() {
    Money money = Money.empty();
    assertEquals(0, money.hashCode());
  }

  @Test
  void testExtractJobUnitPrice_NoNumbers() {
    String content = "■単価：スキル見合い";
    Money money = Money.extractJobUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractPersonUnitPrice_NoNumbers() {
    String content = "■単価：スキル見合い";
    Money money = Money.extractPersonUnitPrice(content);
    assertTrue(money.isEmpty());
  }

  @Test
  void testExtractJobUnitPrice_YenDirectFormat() {
    String content = "■単価：1000000";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
  }

  @Test
  void testExtractPersonUnitPrice_RangeWithWaveFormat() {
    String content = "■単価：60～90万";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("600000"), money.getValue());
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

  @Test
  void testExtractJobUnitPrice_LargeNumber() {
    String content = "■単価：5000000円";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("5000000"), money.getValue());
  }

  @Test
  void testExtractPersonUnitPrice_DecimalWithWave() {
    String content = "■単価：60.5～90.8万";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("600000"), money.getValue());
  }

  @Test
  void testCompareTo_AllEmpty() {
    Money m1 = Money.empty();
    Money m2 = Money.empty();
    assertEquals(0, m1.compareTo(m2));
  }

  @Test
  void testExtractJobUnitPrice_BareNumber() {
    String content = "■単価：100";
    Money money = Money.extractJobUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
  }

  @Test
  void testExtractPersonUnitPrice_BareNumber() {
    String content = "■単価：100-120";
    Money money = Money.extractPersonUnitPrice(content);
    assertFalse(money.isEmpty());
    assertEquals(new BigDecimal("1000000"), money.getValue());
  }
}
