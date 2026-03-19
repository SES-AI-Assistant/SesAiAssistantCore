package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_WEBAPP_M_USERLotTest {

  @Test
  void testSelectAll() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("role_cd")).thenReturn("10");
    when(rs.getString("plan_cd")).thenReturn("P1");
    when(rs.getString("user_id")).thenReturn("U1");

    SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();
    lot.selectAll(connection);
    assertEquals(1, lot.size());
  }
}
