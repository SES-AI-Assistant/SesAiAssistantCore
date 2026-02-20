package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;

class SES_AI_T_SKILLSHEETTests {

    @Test
    void testSKILLSHEET() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1, 1, 1, 1, 1, 1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false, true, false, true, false, true, false);
        when(rs.getString(anyString())).thenReturn("F1");
        
        SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
        ss.setFileId("F1");
        ss.setRegisterDate(new OriginalDateTime());
        ss.setTtl(new OriginalDateTime());
        
        assertEquals(1, ss.insert(connection));
        // SES_AI_T_SKILLSHEET.updateByPk は常に false を返す実装になっている
        assertFalse(ss.updateByPk(connection));
        ss.selectByPk(connection);
        ss.selectByPkWithoutRawContent(connection);
        assertTrue(ss.deleteByPk(connection));
        assertNotNull(ss.toString());
    }

    @Test
    void testSKILLSHEETLot() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false, true, false, true, false);
        when(rs.getDouble("distance")).thenReturn(0.5);
        
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        lot.selectAll(connection);
        
        Vector vector = new Vector(null);
        java.lang.reflect.Field valueField = Vector.class.getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(vector, new float[]{0.1f});
        
        lot.retrieve(connection, vector, 10);
        lot.searchByFileContent(connection, "java");
        lot.searchByFileContent(connection, "java", List.of());
        lot.selectByAndQuery(connection, Map.of("c", "v"));
        lot.selectByOrQuery(connection, Map.of("c", "v"));
        
        SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
        ss.setFileId("F1");
        lot.add(ss);
        assertEquals(ss, lot.getEntityByPk("F1"));
        assertNotNull(lot.toString());
    }
}
