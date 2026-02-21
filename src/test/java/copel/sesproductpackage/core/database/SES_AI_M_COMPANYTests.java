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

class SES_AI_M_COMPANYTests {

    @Test
    void testNullScenarios() throws SQLException {
        SES_AI_M_COMPANY entity = new SES_AI_M_COMPANY();
        Connection connection = mock(Connection.class);
        
        assertEquals(0, entity.insert(null));
        entity.selectByPk(null);
        entity.setCompanyId(null);
        entity.selectByPk(connection);
        assertFalse(entity.updateByPk(null));
        assertFalse(entity.updateByPk(connection));
        assertFalse(entity.deleteByPk(null));
        assertFalse(entity.deleteByPk(connection));
        
        entity.setRegisterDate(null);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        entity.setCompanyId("C1");
        entity.insert(connection);
        entity.updateByPk(connection);
    }

    @Test
    void testCOMPANY() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1, 1, 1, 1, 1, 1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false, true, false);
        when(rs.getString(anyString())).thenReturn("C1");
        
        SES_AI_M_COMPANY company = new SES_AI_M_COMPANY();
        company.setCompanyId("C1");
        company.setRegisterDate(new OriginalDateTime());
        
        assertEquals(1, company.insert(connection));
        assertTrue(company.updateByPk(connection));
        company.selectByPk(connection);
        assertTrue(company.deleteByPk(connection));
        assertNotNull(company.toString());
    }

    @Test
    void testCOMPANYLot() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        
        SES_AI_M_COMPANYLot lot = new SES_AI_M_COMPANYLot();
        lot.selectAll(connection);
        
        SES_AI_M_COMPANY company = new SES_AI_M_COMPANY();
        company.setCompanyId("C1");
        lot.add(company);
        assertNotNull(lot.toString());
    }
}
