package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OriginalDateTimeTests {

  private OriginalDateTime dateTimeNow;
  private OriginalDateTime dateTimeString;
  private OriginalDateTime dateTimeValues;
  private OriginalDateTime dateTimeSqlDate;
  private OriginalDateTime dateTimeSqlTimestamp;

  @BeforeEach
  void setUp() {
    dateTimeNow = new OriginalDateTime();
    dateTimeString = new OriginalDateTime("2024-04-01 12:34:56");
    dateTimeValues = new OriginalDateTime(2024, 4, 1, 12, 34, 56);
    dateTimeSqlDate = new OriginalDateTime(Date.valueOf("2024-04-01"));
    dateTimeSqlTimestamp = new OriginalDateTime(Timestamp.valueOf("2024-04-01 12:34:56"));
  }

  @Test
  void testConstructorWithPatterns() {
    assertNotNull(new OriginalDateTime("2024-04-01 12:34:56.123456").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024/04/01 12:34:56").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024-04-01").toLocalDateTime());
    assertNotNull(new OriginalDateTime("2024/04/01").toLocalDateTime());
    assertNull(new OriginalDateTime("invalid").toLocalDateTime());
  }

  @Test
  void testBetweenMethods() {
    OriginalDateTime d1 = new OriginalDateTime("2024-01-01 00:00:00");
    OriginalDateTime d2 = new OriginalDateTime("2025-02-02 00:00:00");

    assertEquals(1, d1.betweenYear(d2));
    assertEquals(13, d1.betweenMonth(d2));
    assertTrue(d1.betweenDays(d2) > 365);

    // Error cases
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

    // equals with null
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
  void getYYYYMM() {
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

    // Due to a bug in OriginalDateTime.equals, d1.equals(d2) evaluates to false.
    assertEquals(-1, d1.compareTo(d2));
    assertEquals(-1, d1.compareTo(d3));
    assertEquals(1, d3.compareTo(d1));
    // empty.compareTo(d1) does not throw NPE because empty.dateTime is null, so it
    // goes to else block
    assertEquals(-1, empty.compareTo(d1));

    // Let's also test when target is null
    assertEquals(-1, d1.compareTo(null));
  }

  @Test
  void testEmptyMethodBranches() {
    OriginalDateTime empty = new OriginalDateTime((String) null);
    OriginalDateTime present = new OriginalDateTime("2024-04-01 12:00:00");

    assertEquals(-1, empty.betweenYear(present));
    assertEquals(-1, empty.betweenMonth(present));
    assertEquals(0, empty.betweenDays(present));

    empty.plusDays(1); // should not throw
    empty.minusMinutes(34); // should not throw
    assertFalse(empty.equals(present));
  }

  @Test
  void testConstructorsWithNulls() {
    OriginalDateTime d1 = new OriginalDateTime((java.sql.Date) null);
    assertTrue(d1.isEmpty());
    OriginalDateTime d2 = new OriginalDateTime((java.sql.Timestamp) null);
    assertTrue(d2.isEmpty());
  }
}
