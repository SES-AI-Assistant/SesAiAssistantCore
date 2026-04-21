package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_WEBAPP_M_NOTIFICATIONLotTest {

  @Test
  void testLot() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    when(rs.getString("notification_id")).thenReturn("N1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("device_type")).thenReturn("WEB_PUSH");

    SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();
    lot.selectAll(connection);

    assertEquals(1, lot.size());
    SES_AI_WEBAPP_M_NOTIFICATION first = lot.get(0);
    assertEquals("N1", first.getNotificationId());
    assertEquals("U1", first.getUserId());

    assertNotNull(lot.toString());
  }
}
