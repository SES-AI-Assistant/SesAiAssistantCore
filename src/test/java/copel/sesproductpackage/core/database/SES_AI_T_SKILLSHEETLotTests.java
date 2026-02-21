package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import copel.sesproductpackage.core.unit.Vector;
import copel.sesproductpackage.core.unit.LogicalOperators;

class SES_AI_T_SKILLSHEETLotTests {

    @Test
    void testSelectAll() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(anyString())).thenReturn("test");
        
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        lot.selectAll(connection);
        assertEquals(1, lot.size());
    }

    @Test
    void testGetEntityByPk() {
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        SES_AI_T_SKILLSHEET entity = new SES_AI_T_SKILLSHEET();
        entity.setFileId("F123");
        lot.add(entity);
        
        assertEquals(entity, lot.getEntityByPk("F123"));
        assertNull(lot.getEntityByPk("OTHER"));
        assertNull(lot.getEntityByPk(null));
    }

    @Test
    void testToスキルシート選出用文章() {
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        SES_AI_T_SKILLSHEET entity = new SES_AI_T_SKILLSHEET();
        entity.setFileId("F1");
        entity.setFileContentSummary("Summary");
        lot.add(entity);
        
        String result = lot.toスキルシート選出用文章();
        assertTrue(result.contains("F1"));
        assertTrue(result.contains("Summary"));
    }

    @Test
    void testRetrieve() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getDouble("distance")).thenReturn(0.5);
        
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        
        Vector vector = new Vector(null);
        java.lang.reflect.Field valueField = Vector.class.getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(vector, new float[]{0.1f});
        
        lot.retrieve(connection, vector, 10);
        assertEquals(1, lot.size());
        lot.retrieve(null, null, 0); // null path
    }

    @Test
    void testSearchMethods() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false, true, false, true, false, true, false, true, false);
        
        SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
        lot.selectLike(connection, "col", "query");
        assertEquals(1, lot.size());

        lot.searchByFileContent(connection, "query");
        assertEquals(1, lot.size());

        lot.selectByFileName(connection, "file.pdf");
        assertEquals(1, lot.size());
        
        // Covering logical operators branch
        LogicalOperators op = new LogicalOperators(LogicalOperators.論理演算子.AND, "val");
        lot.searchByFileContent(connection, "q1", List.of(op));
        assertEquals(1, lot.size());

        lot.selectByAndQuery(connection, Map.of("k", "v"));
        assertEquals(1, lot.size());
        
        lot.selectByOrQuery(connection, Map.of("k", "v"));
        assertEquals(1, lot.size());
    }
}
