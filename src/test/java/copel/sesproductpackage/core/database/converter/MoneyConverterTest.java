package copel.sesproductpackage.core.database.converter;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.unit.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * MoneyConverter のテストクラス.
 *
 * <p>Money と BigDecimal の相互変換を検証します。
 *
 * @author Copel Co., Ltd.
 */
class MoneyConverterTest {

  private MoneyConverter converter;

  @BeforeEach
  void setUp() {
    converter = new MoneyConverter();
  }

  /** テスト1: convertToDatabaseColumn で null を渡したとき → null を返す. */
  @Test
  void testConvertToDatabaseColumnWithNull() {
    assertNull(converter.convertToDatabaseColumn(null));
  }

  /** テスト2: convertToEntityAttribute で null を渡したとき → null を返す. */
  @Test
  void testConvertToEntityAttributeWithNull() {
    assertNull(converter.convertToEntityAttribute(null));
  }

  /**
   * テスト3: convertToDatabaseColumn で有効な Money を渡したとき.
   *
   * <p>Money を BigDecimal に正しく変換できることを確認します。
   */
  @Test
  void testConvertToDatabaseColumnSuccess() {
    Money money = new Money(new BigDecimal("1000.50"));
    BigDecimal result = converter.convertToDatabaseColumn(money);
    assertNotNull(result);
    assertEquals(new BigDecimal("1000.50"), result);
  }

  /**
   * テスト4: convertToEntityAttribute で有効な BigDecimal を渡したとき.
   *
   * <p>BigDecimal を Money に正しく変換できることを確認します。
   */
  @Test
  void testConvertToEntityAttributeSuccess() {
    BigDecimal dbData = new BigDecimal("2500.75");
    Money result = converter.convertToEntityAttribute(dbData);
    assertNotNull(result);
    assertEquals(new BigDecimal("2500.75"), result.getValue());
  }

  /**
   * テスト5: 0 円の Money を変換.
   *
   * <p>BigDecimal.ZERO で初期化された Money が正しく変換されることを確認します。
   */
  @Test
  void testConvertZeroAmount() {
    Money money = new Money(BigDecimal.ZERO);
    BigDecimal result = converter.convertToDatabaseColumn(money);
    // MoneyConverter は isEmpty() チェックを行うため、ZERO でも null を返す可能性がある
    // Money の isEmpty() 実装を確認して、適切なアサーションを追加
    // 実装では isEmpty() は valueInYen == null でチェックするため、ZERO は empty ではない
    assertEquals(BigDecimal.ZERO, result);
  }

  /**
   * テスト6: 往復変換の正確性.
   *
   * <p>Money → BigDecimal → Money のラウンドトリップ変換後、元の値と同じになることを確認します。
   */
  @Test
  void testRoundTripConversion() {
    Money original = new Money(new BigDecimal("5000.99"));
    BigDecimal intermediate = converter.convertToDatabaseColumn(original);
    Money restored = converter.convertToEntityAttribute(intermediate);
    assertEquals(original.getValue(), restored.getValue());
  }

  /**
   * テスト7: 大きい金額の変換.
   *
   * <p>数百万円単位の大きな金額が正しく変換されることを確認します。
   */
  @Test
  void testConvertLargeAmount() {
    Money money = new Money(new BigDecimal("10000000"));
    BigDecimal result = converter.convertToDatabaseColumn(money);
    assertNotNull(result);
    assertEquals(new BigDecimal("10000000"), result);
  }

  /**
   * テスト8: 小数第三位まで正確な金額の変換.
   *
   * <p>小数第二位までの金額（銭単位）が正しく変換されることを確認します。
   */
  @Test
  void testConvertPrecisionAmount() {
    Money money = new Money(new BigDecimal("123.45"));
    BigDecimal result = converter.convertToDatabaseColumn(money);
    assertNotNull(result);
    assertEquals(new BigDecimal("123.45"), result);
  }

  /**
   * テスト9: long型コンストラクタで生成された Money の変換.
   *
   * <p>long 値で初期化された Money も正しく変換されることを確認します。
   */
  @Test
  void testConvertLongConstructorMoney() {
    Money money = new Money(100000L);
    BigDecimal result = converter.convertToDatabaseColumn(money);
    assertNotNull(result);
    assertEquals(new BigDecimal("100000"), result);
  }

  /**
   * テスト10: empty な Money の変換.
   *
   * <p>Money.empty() で生成された空の Money は null を返すことを確認します。
   */
  @Test
  void testConvertEmptyMoney() {
    Money money = Money.empty();
    BigDecimal result = converter.convertToDatabaseColumn(money);
    assertNull(result);
  }

  /**
   * テスト11: empty な Money との相互変換.
   *
   * <p>null から変換された Money が empty 状態を保つことを確認します。
   */
  @Test
  void testConvertFromNullMoney() {
    Money money = converter.convertToEntityAttribute(null);
    assertNull(money);
  }

  /**
   * テスト12: 負の金額の変換.
   *
   * <p>負の金額が正しく変換されることを確認します。
   */
  @Test
  void testConvertNegativeAmount() {
    Money money = new Money(new BigDecimal("-1000"));
    BigDecimal result = converter.convertToDatabaseColumn(money);
    assertNotNull(result);
    assertEquals(new BigDecimal("-1000"), result);
  }
}
