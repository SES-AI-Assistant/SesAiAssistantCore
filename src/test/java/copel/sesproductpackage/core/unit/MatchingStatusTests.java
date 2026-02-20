package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MatchingStatusTests {

    @Test
    void testGetEnum() {
        assertEquals(MatchingStatus.アンマッチ, MatchingStatus.getEnum("00"));
        assertEquals(MatchingStatus.サジェスト中, MatchingStatus.getEnum("01"));
        assertEquals(MatchingStatus.提案中, MatchingStatus.getEnum("10"));
        assertEquals(MatchingStatus.無効, MatchingStatus.getEnum("99"));
        assertNull(MatchingStatus.getEnum("ZZ"));
    }

    @Test
    void testGetSetCode() {
        MatchingStatus status = MatchingStatus.アンマッチ;
        assertEquals("00", status.getCode());
        status.setCode("XX");
        assertEquals("XX", status.getCode());
        // 戻しておく
        status.setCode("00");
    }
}
