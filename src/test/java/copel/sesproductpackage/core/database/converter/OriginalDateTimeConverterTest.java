package copel.sesproductpackage.core.database.converter;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * OriginalDateTimeConverter のテストクラス.
 *
 * <p>OriginalDateTime と LocalDateTime の相互変換を検証します。
 *
 * @author Copel Co., Ltd.
 */
class OriginalDateTimeConverterTest {

  private OriginalDateTimeConverter converter;

  @BeforeEach
  void setUp() {
    converter = new OriginalDateTimeConverter();
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
   * テスト3: convertToDatabaseColumn で有効な OriginalDateTime を渡したとき.
   *
   * <p>OriginalDateTime を LocalDateTime に正しく変換できることを確認します。
   */
  @Test
  void testConvertToDatabaseColumnSuccess() {
    OriginalDateTime original = new OriginalDateTime("2026-08-05 14:30:45");
    LocalDateTime result = converter.convertToDatabaseColumn(original);
    assertNotNull(result);
    assertEquals(2026, result.getYear());
    assertEquals(8, result.getMonthValue());
    assertEquals(5, result.getDayOfMonth());
    assertEquals(14, result.getHour());
    assertEquals(30, result.getMinute());
    assertEquals(45, result.getSecond());
  }

  /**
   * テスト4: convertToEntityAttribute で有効な LocalDateTime を渡したとき.
   *
   * <p>LocalDateTime を OriginalDateTime に正しく変換できることを確認します。
   */
  @Test
  void testConvertToEntityAttributeSuccess() {
    LocalDateTime dbData = LocalDateTime.of(2026, 8, 5, 14, 30, 45);
    OriginalDateTime result = converter.convertToEntityAttribute(dbData);
    assertNotNull(result);
    assertEquals("2026-08-05 14:30:45", result.toString());
  }

  /**
   * テスト5: 往復変換（OriginalDateTime → LocalDateTime → OriginalDateTime）の正確性.
   *
   * <p>ラウンドトリップ変換後、元の値と同じになることを確認します。
   */
  @Test
  void testRoundTripConversion() {
    OriginalDateTime original = new OriginalDateTime("2026-08-05 14:30:45");
    LocalDateTime intermediate = converter.convertToDatabaseColumn(original);
    OriginalDateTime restored = converter.convertToEntityAttribute(intermediate);
    assertEquals(original.toString(), restored.toString());
  }

  /**
   * テスト6: 異なるフォーマットで入力された OriginalDateTime の変換.
   *
   * <p>OriginalDateTime はさまざまなフォーマットに対応するため、スラッシュ区切りの入力でも正しく変換されることを確認します。
   */
  @Test
  void testConvertToDatabaseColumnWithSlashFormat() {
    OriginalDateTime original = new OriginalDateTime("2026/08/05 14:30:45");
    LocalDateTime result = converter.convertToDatabaseColumn(original);
    assertNotNull(result);
    assertEquals(2026, result.getYear());
    assertEquals(8, result.getMonthValue());
    assertEquals(5, result.getDayOfMonth());
  }

  /**
   * テスト7: 日付のみの OriginalDateTime の変換.
   *
   * <p>時刻なしで日付のみが指定された場合、時刻は 00:00:00 で初期化されることを確認します。
   */
  @Test
  void testConvertToDatabaseColumnDateOnly() {
    OriginalDateTime original = new OriginalDateTime("2026-08-05");
    LocalDateTime result = converter.convertToDatabaseColumn(original);
    assertNotNull(result);
    assertEquals(0, result.getHour());
    assertEquals(0, result.getMinute());
    assertEquals(0, result.getSecond());
  }

  /**
   * テスト8: 現在日時で生成された OriginalDateTime の変換.
   *
   * <p>引数なしで生成された OriginalDateTime（現在日時）も正しく変換されることを確認します。
   */
  @Test
  void testConvertToDatabaseColumnCurrentDateTime() {
    OriginalDateTime original = new OriginalDateTime();
    LocalDateTime result = converter.convertToDatabaseColumn(original);
    assertNotNull(result);
    // 現在日時なので、年月日が現在と同じことを確認
    LocalDateTime now = LocalDateTime.now();
    assertEquals(now.getYear(), result.getYear());
    assertEquals(now.getMonthValue(), result.getMonthValue());
    assertEquals(now.getDayOfMonth(), result.getDayOfMonth());
  }
}
