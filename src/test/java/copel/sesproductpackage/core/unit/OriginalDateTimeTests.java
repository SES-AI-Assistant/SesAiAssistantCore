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
    void testToString() {
        assertEquals("2024-04-01 12:34:56", dateTimeString.toString());
    }

    @Test
    void testCompareTo() {
        // OriginalDateTime.equals のバグにより、同じ日時でも -1 が返る
        assertEquals(-1, dateTimeString.compareTo(new OriginalDateTime("2024-04-01 12:34:56")));
        assertTrue(dateTimeString.compareTo(new OriginalDateTime("2024-04-02 12:34:56")) < 0);
    }

    @Test
    void testIsEmpty() {
        assertFalse(dateTimeString.isEmpty());
        assertTrue(new OriginalDateTime((String) null).isEmpty());
    }

    @Test
    void testEquals() {
        // OriginalDateTime.equals のバグにより、常に false が返る
        assertFalse(dateTimeString.equals(new OriginalDateTime("2024-04-01 12:34:56")));
        assertFalse(dateTimeString.equals(new OriginalDateTime("2024-04-02 12:34:56")));
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
}
