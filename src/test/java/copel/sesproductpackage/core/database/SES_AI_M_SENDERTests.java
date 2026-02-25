package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_M_SENDERTests {

  @Test
  void testNullScenarios() throws SQLException {
    SES_AI_M_SENDER entity = new SES_AI_M_SENDER();
    Connection connection = mock(Connection.class);

    assertEquals(0, entity.insert(null));
    entity.selectByPk(null);
    entity.setFromId(null);
    entity.selectByPk(connection);
    assertFalse(entity.updateByPk(null));
    assertFalse(entity.updateByPk(connection));
    assertFalse(entity.deleteByPk(null));
    assertFalse(entity.deleteByPk(connection));

    entity.setRegisterDate(null);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    entity.setFromId("S1");
    entity.insert(connection);
    entity.updateByPk(connection);
  }

  @Test
  void testSENDER() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1, 1, 1, 1, 1, 1);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false, true, false, true, false);
    when(rs.getString(anyString())).thenReturn("S1");

    SES_AI_M_SENDER sender = new SES_AI_M_SENDER();
    sender.setFromId("S1");
    sender.setRegisterDate(new OriginalDateTime());

    assertEquals(1, sender.insert(connection));
    assertTrue(sender.updateByPk(connection));
    sender.selectByPk(connection);
    assertTrue(sender.deleteByPk(connection));
    assertNotNull(sender.toString());
  }

  @Test
  void testSENDERLot() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_M_SENDERLot lot = new SES_AI_M_SENDERLot();
    lot.selectAll(connection);

    SES_AI_M_SENDER sender = new SES_AI_M_SENDER();
    sender.setFromId("S1");
    lot.add(sender);
    assertNotNull(lot.toString());

    // failure branches
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(sender.updateByPk(connection));
    assertFalse(sender.deleteByPk(connection));
    when(rs.next()).thenReturn(false);
    sender.selectByPk(connection);
  }
}
