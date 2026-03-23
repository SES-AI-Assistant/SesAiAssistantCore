package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_T_MATCHLotTest {

  @Test
  void testExistMethods() {
    SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();
    SES_AI_T_MATCH match = new SES_AI_T_MATCH();
    match.setJobId("J123");
    match.setPersonId("P123");
    lot.add(match);

    assertTrue(lot.isExistByJobId("J123"));
    assertFalse(lot.isExistByJobId("J999"));
    assertTrue(lot.isExistByPersonId("P123"));
    assertFalse(lot.isExistByPersonId("P999"));

    assertFalse(lot.isExistByJobId(null));
    assertFalse(lot.isExistByPersonId(null));
  }

  @Test
  void testSelectAll() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    when(rs.getString("matching_id")).thenReturn("M1");
    when(rs.getString("status_cd")).thenReturn("00");
    when(rs.getString("register_date")).thenReturn("2026-01-01 10:00:00");

    SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();
    lot.selectAll(connection);
    assertEquals(1, lot.size());
    assertEquals("M1", lot.get(0).getMatchingId());
  }
}
