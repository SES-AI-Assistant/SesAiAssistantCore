package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_T_MATCHLotTests {

  @Test
  void testExistMethods() {
    SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();
    SES_AI_T_MATCH match = new SES_AI_T_MATCH();
    match.setJobId("J123");
    match.setPersonId("P123");
    lot.add(match);

    assertTrue(lot.isExistByJobId("J123"));
    assertFalse(lot.isExistByJobId("J999"));
    assertFalse(lot.isExistByJobId(null));

    assertTrue(lot.isExistByPersonId("P123"));
    assertFalse(lot.isExistByPersonId("P999"));
    assertFalse(lot.isExistByPersonId(null));
  }

  @Test
  void testLot() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("status_cd")).thenReturn("00");

    SES_AI_T_MATCHLot lot = new SES_AI_T_MATCHLot();
    lot.selectAll(connection);
    assertEquals(1, lot.size());

    SES_AI_T_MATCH match = new SES_AI_T_MATCH();
    match.setMatchingId("M1");
    lot.add(match);
    assertNotNull(lot.toString());
  }
}
