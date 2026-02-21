package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class SES_AI_T_WATCHLotTests {

    @Test
    void testSelectByUserId() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(anyString())).thenReturn("test");
        
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
        lot.selectByUserId(connection, "user123");
        assertEquals(1, lot.size());
        
        // Null checks
        lot.selectByUserId(null, "user123");
        lot.selectByUserId(connection, null);
    }

    @Test
    void testContainsById() {
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
        SES_AI_T_WATCH entity = new SES_AI_T_WATCH();
        entity.setTargetId("target123");
        lot.add(entity);
        
        assertTrue(lot.containsById("target123"));
        assertFalse(lot.containsById("other"));
        assertFalse(lot.containsById(null));
        assertFalse(lot.containsById(""));
    }

    @Test
    void testSelectAll() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(anyString())).thenReturn("test");
        
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
        lot.selectAll(connection);
        assertEquals(1, lot.size());
    }
}
