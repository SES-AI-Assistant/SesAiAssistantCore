package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.OriginalDateTime;

class SES_AI_M_SENDERTests {

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
    }
}
