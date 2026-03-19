package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OriginalDateTimeTest {

  private OriginalDateTime dateTimeString;

  @BeforeEach
  void setUp() {
    dateTimeString = new OriginalDateTime("2024-04-01 12:34:56");
  }

  @Test
  void testConstructorWithPatterns() {
    assertNotNull(new OriginalDateTime("2024-04-01 12:34:56.123456").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024-04-01 12:34:56.12345").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024-04-01 12:34:56.1234").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024-04-01 12:34:56.123").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024-04-01 12:34:56.12").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024-04-01 12:34:56.1").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024-04-01 12:34").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024/04/01 12:34:56.123456").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024/04/01 12:34:56").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024/04/01 12:34:56.1").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024/04/01 12:34").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024-04-01").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024/04/01").toLocalDateTime());
    assertNull(new OriginalDateTime("invalid").toLocalDateTime());
  }

  @Test
  void testConstructorWithSqlDate() {
    OriginalDateTime fromNull = new OriginalDateTime((Date) null);
    assertTrue(fromNull.isEmpty());
    assertNull(fromNull.toLocalDateTime());

    Date sqlDate = Date.valueOf("2024-06-15");
    OriginalDateTime odt = new OriginalDateTime(sqlDate);
    assertEquals(LocalDate.of(2024, 6, 15), odt.toLocalDate());
  }

  @Test
  void testConstructorWithSqlTimestamp() {
    OriginalDateTime fromNull = new OriginalDateTime((Timestamp) null);
    assertTrue(fromNull.isEmpty());
    assertNull(fromNull.toLocalDateTime());

    Timestamp ts = Timestamp.valueOf("2024-06-15 14:30:00");
    OriginalDateTime odt = new OriginalDateTime(ts);
    assertEquals(LocalDateTime.of(2024, 6, 15, 14, 30, 0), odt.toLocalDateTime());
  }

  @Test
  void testConstructorWithIntParams() {
    OriginalDateTime odt = new OriginalDateTime(2024, 3, 19, 10, 30, 45);
    assertEquals(2024, odt.toLocalDateTime().getYear());
    assertEquals(3, odt.toLocalDateTime().getMonthValue());
    assertEquals(19, odt.toLocalDateTime().getDayOfMonth());
    assertEquals(10, odt.toLocalDateTime().getHour());
    assertEquals(30, odt.toLocalDateTime().getMinute());
    assertEquals(45, odt.toLocalDateTime().getSecond());
  }

  @Test
  void testBetweenMethods() {
    OriginalDateTime d1 = new OriginalDateTime("2024-01-01 00:00:00");
    OriginalDateTime d2 = new OriginalDateTime("2025-02-02 00:00:00");

    assertEquals(1, d1.betweenYear(d2));
    assertEquals(13, d1.betweenMonth(d2));
    assertTrue(d1.betweenDays(d2) > 365);

    assertEquals(-1, d1.betweenYear(null));
    assertEquals(-1, d1.betweenMonth(null));
    assertEquals(0, d1.betweenDays(null));
  }

  @Test
  void testEmptyAndNull() {
    OriginalDateTime empty = new OriginalDateTime((String) null);
    assertTrue(empty.isEmpty());
    assertNull(empty.toString());
    assertNull(empty.get曜日());
    assertNull(empty.getMMdd());
    assertNull(empty.getYYYYMM());
    assertNull(empty.getHHmm());
    assertNull(empty.getHHmmss());
    assertNull(empty.getYyyyMMdd());
    assertNull(empty.getYyyy_MM_dd());
    assertNull(empty.toLocalDate());
    assertNull(empty.toTimestamp());

    assertFalse(dateTimeString.equals(null));
  }

  @Test
  void testGet曜日() {
    assertEquals("(月)", dateTimeString.get曜日());
  }

  @Test
  void testGetMMdd() {
    assertEquals("04/01", dateTimeString.getMMdd());
  }

  @Test
  void testGetYYYYMM() {
    assertEquals("202404", dateTimeString.getYYYYMM());
  }

  @Test
  void testGetHHmm() {
    assertEquals("12:34", dateTimeString.getHHmm());
  }

  @Test
  void testGetHHmmss() {
    assertEquals("12:34:56", dateTimeString.getHHmmss());
  }

  @Test
  void testGetYyyyMMdd() {
    assertEquals("2024/04/01", dateTimeString.getYyyyMMdd());
  }

  @Test
  void testGetYyyy_MM_dd() {
    assertEquals("2024-04-01", dateTimeString.getYyyy_MM_dd());
  }

  @Test
  void testToLocalDateTime() {
    assertEquals(LocalDateTime.parse("2024-04-01T12:34:56"), dateTimeString.toLocalDateTime());
  }

  @Test
  void testToLocalDate() {
    assertEquals(LocalDate.parse("2024-04-01"), dateTimeString.toLocalDate());
  }

  @Test
  void testToTimestamp() {
    assertEquals(Timestamp.valueOf("2024-04-01 12:34:56"), dateTimeString.toTimestamp());
  }

  @Test
  void testBetweenDays() {
    assertEquals(-7, dateTimeString.betweenDays(new OriginalDateTime("2024-03-25 12:34:56")));
  }

  @Test
  void testBetweenMonth() {
    assertEquals(-1, dateTimeString.betweenMonth(new OriginalDateTime("2024-03-01 12:34:56")));
  }

  @Test
  void testBetweenYear() {
    assertEquals(-1, dateTimeString.betweenYear(new OriginalDateTime("2023-04-01 12:34:56")));
  }

  @Test
  void testPlusDays() {
    dateTimeString.plusDays(1);
    assertEquals("2024-04-02", dateTimeString.getYyyy_MM_dd());
  }

  @Test
  void testMinusMinutes() {
    dateTimeString.minusMinutes(34);
    assertEquals("12:00", dateTimeString.getHHmm());
  }

  @Test
  void testCompareTo() {
    OriginalDateTime d1 = new OriginalDateTime("2024-04-01 12:00:00");
    OriginalDateTime d2 = new OriginalDateTime("2024-04-01 12:00:00");
    OriginalDateTime d3 = new OriginalDateTime("2024-04-02 12:00:00");
    OriginalDateTime empty = new OriginalDateTime((String) null);

    assertEquals(0, d1.compareTo(d1));
    assertTrue(d1.equals(d1));

    assertEquals(0, d1.compareTo(d2));
    assertEquals(-1, d1.compareTo(d3));
    assertEquals(1, d3.compareTo(d1));
    assertEquals(-1, empty.compareTo(d1));
    assertEquals(-1, d1.compareTo(null));

    OriginalDateTime dLower = new OriginalDateTime("2024-04-01 11:59:59");
    assertEquals(1, d1.compareTo(dLower));
  }

  @Test
  void testEqualsBrackets() {
    OriginalDateTime d1 = new OriginalDateTime("2024-01-01 10:00:00");
    OriginalDateTime d2 = new OriginalDateTime("2024-01-01 10:00:00");

    assertTrue(d1.equals(d1));
    assertFalse(d1.equals(null));
    assertFalse(d1.equals("str"));
    assertTrue(d1.equals(d2));

    OriginalDateTime dNull = new OriginalDateTime((String) null);
    assertFalse(dNull.equals(d1));

    OriginalDateTime dNull2 = new OriginalDateTime((String) null);
    assertFalse(dNull.equals(dNull2));
  }
}
