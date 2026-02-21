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
        
        SES_AI_T_WATCH watch = new SES_AI_T_WATCH();
        
        // True branch
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(true);
        assertTrue(watch.isExist(connection));
        assertTrue(watch.isExistByTargetId(connection));
        
        // False branch
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(false);
        assertFalse(watch.isExist(connection));
        assertFalse(watch.isExistByTargetId(connection));
        
        // ResultSet empty branch
        reset(rs);
        when(rs.next()).thenReturn(false);
        assertFalse(watch.isExist(connection));
        assertFalse(watch.isExistByTargetId(connection));
    }

    @Test
    void testTargetTypeAll() {
        for (SES_AI_T_WATCH.TargetType type : SES_AI_T_WATCH.TargetType.values()) {
            assertEquals(type, SES_AI_T_WATCH.TargetType.getEnumByName(type.name()));
        }
        assertNull(SES_AI_T_WATCH.TargetType.getEnumByName("OTHER"));
        assertNull(SES_AI_T_WATCH.TargetType.getEnumByName(null));
    }

    @Test
    void testNullScenarios() throws SQLException {
        SES_AI_T_WATCH entity = new SES_AI_T_WATCH();
        Connection connection = mock(Connection.class);
        
        assertEquals(0, entity.insert(null));
        entity.selectByPk(null);
        entity.setUserId(null);
        entity.selectByPk(connection);
        assertFalse(entity.updateByPk(null));
        assertFalse(entity.updateByPk(connection));
        assertFalse(entity.deleteByPk(null));
        assertFalse(entity.deleteByPk(connection));
        
        entity.setRegisterDate(null);
        entity.setTtl(null);
        entity.setTargetType(null);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        entity.setUserId("U1");
        entity.setTargetId("T1");
        entity.insert(connection);
        entity.updateByPk(connection);
        
        // toString null branches
        assertNotNull(entity.toString());
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
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        
        when(rs.getString("user_id")).thenReturn("U1");
        when(rs.getString("target_id")).thenReturn("T1");
        when(rs.getString("target_type")).thenReturn("PERSON");
        when(rs.getString("memo")).thenReturn("memo1");
        when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00");
        when(rs.getString("register_user")).thenReturn("admin");
        when(rs.getString("ttl")).thenReturn("2026-12-31 23:59:59");
        
        SES_AI_T_WATCH watch = new SES_AI_T_WATCH();
        watch.setUserId("U1");
        watch.setTargetId("T1");
        watch.setTargetType(SES_AI_T_WATCH.TargetType.JOB);
        watch.setMemo("memo1");
        watch.setRegisterDate(new OriginalDateTime());
        watch.setTtl(new OriginalDateTime());
        
        assertEquals(1, watch.insert(connection));
        assertTrue(watch.updateByPk(connection));
        
        SES_AI_T_WATCH target = new SES_AI_T_WATCH();
        target.setUserId("U1");
        target.setTargetId("T1");
        target.selectByPk(connection);
        
        assertEquals("U1", target.getUserId());
        assertEquals("T1", target.getTargetId());
        assertEquals(SES_AI_T_WATCH.TargetType.PERSON, target.getTargetType());
        assertEquals("memo1", target.getMemo());
        assertNotNull(target.getTtl());
        
        assertTrue(watch.deleteByPk(connection));
        assertNotNull(watch.toString());
        
        // Covering Lombok and more branches
        assertNotNull(watch.hashCode());
        assertTrue(watch.equals(watch));
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
