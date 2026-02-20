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
    void testUSER() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1, 1, 1, 1, 1, 1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false, true, false);
        when(rs.getString(anyString())).thenReturn("U1");
        when(rs.getString("role_cd")).thenReturn("10");
        
        SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
        user.setUserId("U1");
        user.setRegisterDate(new OriginalDateTime());
        user.setRole(Role.システムユーザー);
        
        assertEquals(1, user.insert(connection));
        assertTrue(user.updateByPk(connection));
        user.selectByPk(connection);
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
