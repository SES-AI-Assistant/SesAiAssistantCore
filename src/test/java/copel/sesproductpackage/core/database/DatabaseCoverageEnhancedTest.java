package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DatabaseCoverageEnhancedTest {

  @Test
  void testSES_AI_T_PERSONLot_EdgeCases() throws SQLException {
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

    // Null personId in getEntityByPk
    assertNull(lot.getEntityByPk(null));

    // Person not found in lot
    SES_AI_T_PERSON p = new SES_AI_T_PERSON();
    p.setPersonId("some-id");
    lot.add(p);
    assertNull(lot.getEntityByPk("non-existent"));

    // Null connection check in searchByRawContent
    assertDoesNotThrow(() -> lot.searchByRawContent(null, "query", Collections.emptyList()));
    assertDoesNotThrow(() -> lot.searchByRawContent(null, "query"));

    // ResultSet.next() is false
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    lot.searchByRawContent(conn, "query", Collections.emptyList());
    assertEquals(0, lot.size());
  }

  @Test
  void testSES_AI_T_PERSON_InsertNulls() throws SQLException {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);

    // Set fields to null to hit ternary branches in insert
    person.setVectorData(null);
    person.setRegisterDate(null);
    person.setTtl(null);

    person.insert(conn);
    // PERSON insert indices: vector_data(9), register_date(10), ttl(12)
    verify(ps).setString(eq(9), isNull());
    verify(ps).setTimestamp(eq(10), isNull());
    verify(ps).setTimestamp(eq(12), isNull());
  }

  @Test
  void testSES_AI_T_MATCH_EdgeCases() throws SQLException {
    SES_AI_T_MATCH match = new SES_AI_T_MATCH();
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);

    // Null checks
    assertFalse(match.deleteByPk(null));
    assertFalse(match.updateByPk(null));

    // MATCH insert index 8 is registerDate, but it uses new OriginalDateTime()
    match.insert(conn);
  }

  @Test
  void testSES_AI_T_SKILLSHEET_EdgeCases() throws SQLException {
    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);

    // Null checks
    assertFalse(ss.updateByPk(null));
    assertFalse(ss.deleteByPk(null));
    assertEquals(0, ss.insert(null));
    ss.selectByPk(null);
    ss.selectByPkWithoutRawContent(null);

    // getFileId/Name with null skillSheet
    ss.setSkillSheet(null);
    assertNull(ss.getFileId());
    assertNull(ss.getFileName());
    assertEquals("", ss.getFileContent());
    assertEquals("", ss.getFileContentSummary());

    // insert with null skillSheet (indices 4, 5, 6, 7 are file_*)
    ss.insert(conn);
    verify(ps).setString(eq(4), isNull());
    verify(ps).setString(eq(5), isNull());
  }
}
