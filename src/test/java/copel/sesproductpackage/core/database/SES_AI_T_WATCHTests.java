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

class SES_AI_T_WATCHTests {

    @Test
    void testIsExist() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(true);
        
        SES_AI_T_WATCH watch = new SES_AI_T_WATCH();
        assertTrue(watch.isExist(connection));
        assertTrue(watch.isExistByTargetId(connection));
    }

    @Test
    void testTargetTypeAll() {
        for (SES_AI_T_WATCH.TargetType type : SES_AI_T_WATCH.TargetType.values()) {
            assertEquals(type, SES_AI_T_WATCH.TargetType.getEnumByName(type.name()));
        }
    }

    @Test
    void testIsExistPaths() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        
        when(rs.next()).thenReturn(false); // Branch for next() false
        SES_AI_T_WATCH watch = new SES_AI_T_WATCH();
        assertFalse(watch.isExist(connection));
        assertFalse(watch.isExistByTargetId(connection));
    }

    @Test
    void testWATCH() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1, 1, 1, 1, 1, 1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false, true, false);
        when(rs.getString(anyString())).thenReturn("W1");
        when(rs.getString("target_type")).thenReturn("PERSON");
        
        SES_AI_T_WATCH watch = new SES_AI_T_WATCH();
        watch.setUserId("U1");
        watch.setTargetId("T1");
        watch.setRegisterDate(new OriginalDateTime());
        
        assertEquals(1, watch.insert(connection));
        assertTrue(watch.updateByPk(connection));
        watch.selectByPk(connection);
        assertTrue(watch.deleteByPk(connection));
        assertNotNull(watch.toString());
    }

    @Test
    void testWATCHLot() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("target_type")).thenReturn("JOB");
        
        SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
        lot.selectAll(connection);
        
        SES_AI_T_WATCH watch = new SES_AI_T_WATCH();
        watch.setUserId("U1");
        watch.setTargetId("T1");
        lot.add(watch);
        assertNotNull(lot.toString());
    }
}
