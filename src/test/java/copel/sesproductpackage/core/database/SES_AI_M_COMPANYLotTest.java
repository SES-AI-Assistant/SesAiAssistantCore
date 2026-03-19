package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_M_COMPANYLotTest {

  @Test
  void testSelectAll() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("company_id")).thenReturn("C1");
    when(rs.getString("company_name")).thenReturn("Name1");

    SES_AI_M_COMPANYLot lot = new SES_AI_M_COMPANYLot();
    lot.selectAll(connection);
    assertEquals(1, lot.size());
  }
}
