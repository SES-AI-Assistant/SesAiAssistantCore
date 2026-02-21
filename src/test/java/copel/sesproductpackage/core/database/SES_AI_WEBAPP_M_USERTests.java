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
import copel.sesproductpackage.core.unit.Role;

class SES_AI_WEBAPP_M_USERTests {

    @Test
    void testAuth() {
        SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
        user.setRole(Role.システム管理者);
        assertTrue(user.hasSystemUseAuth());
        user.setRole(Role.システムユーザー);
        assertTrue(user.hasSystemUseAuth());
    }

    @Test
    void testNullScenarios() throws SQLException {
        SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
        Connection connection = mock(Connection.class);
        
        // insert null connection
        assertEquals(0, user.insert(null));
        
        // selectByPk null connection or userId
        user.selectByPk(null);
        user.setUserId(null);
        user.selectByPk(connection);
        
        // updateByPk null connection or userId
        assertFalse(user.updateByPk(null));
        user.setUserId(null);
        assertFalse(user.updateByPk(connection));
        
        // deleteByPk null connection or userId
        assertFalse(user.deleteByPk(null));
        user.setUserId(null);
        assertFalse(user.deleteByPk(connection));
        
        user.setRole(null);
        user.setRegisterDate(null);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        user.setUserId("U1");
        user.insert(connection);
        user.updateByPk(connection);
        
        // Covering Lombok equals/hashCode/toString
        assertNotNull(user.toString());
        assertNotNull(user.hashCode());
        assertTrue(user.equals(user));
        assertFalse(user.equals(null));
        assertFalse(user.equals(new Object()));
    }

    @Test
    void testResultSetBranches() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        
        SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
        user.setUserId("U1");
        
        // resultSet.next() is false
        when(rs.next()).thenReturn(false);
        user.selectByPk(connection);
        
        // role is null in DB
        reset(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("role_cd")).thenReturn(null);
        user.selectByPk(connection);
        assertNull(user.getRole());
    }

    @Test
    void testUSER() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        
        when(rs.getString("user_id")).thenReturn("U1");
        when(rs.getString("user_name")).thenReturn("Name1");
        when(rs.getString("company_id")).thenReturn("C1");
        when(rs.getString("role_cd")).thenReturn("10");
        when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00");
        when(rs.getString("register_user")).thenReturn("admin");
        
        SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
        user.setUserId("U1");
        user.setUserName("Name1");
        user.setCompanyId("C1");
        user.setRole(Role.システムユーザー);
        user.setRegisterDate(new OriginalDateTime());
        
        assertEquals(1, user.insert(connection));
        assertTrue(user.updateByPk(connection));
        
        SES_AI_WEBAPP_M_USER target = new SES_AI_WEBAPP_M_USER();
        target.setUserId("U1");
        target.selectByPk(connection);
        
        assertEquals("U1", target.getUserId());
        assertEquals("Name1", target.getUserName());
        assertEquals("C1", target.getCompanyId());
        assertEquals(Role.システムユーザー, target.getRole());
        
        assertTrue(user.deleteByPk(connection));
        assertNotNull(user.toString());
    }

    @Test
    void testUSERLot() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("role_cd")).thenReturn("99");
        
        SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();
        lot.selectAll(connection);
        
        SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
        user.setUserId("U1");
        lot.add(user);
        assertNotNull(lot.toString());
    }
}
