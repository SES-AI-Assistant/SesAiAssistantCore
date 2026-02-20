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

class SES_AI_M_GROUPTests {

    @Test
    void testGROUP() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1, 1, 1, 1, 1, 1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false, true, false);
        when(rs.getString(anyString())).thenReturn("G1");
        
        SES_AI_M_GROUP group = new SES_AI_M_GROUP();
        group.setFromGroup("G1");
        group.setRegisterDate(new OriginalDateTime());
        
        assertEquals(1, group.insert(connection));
        assertTrue(group.updateByPk(connection));
        group.selectByPk(connection);
        assertTrue(group.deleteByPk(connection));
        assertNotNull(group.toString());
    }

    @Test
    void testGROUPLot() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        
        SES_AI_M_GROUPLot lot = new SES_AI_M_GROUPLot();
        lot.selectAll(connection);
        
        SES_AI_M_GROUP group = new SES_AI_M_GROUP();
        group.setFromGroup("G1");
        lot.add(group);
        assertNotNull(lot.toString());
    }
}
