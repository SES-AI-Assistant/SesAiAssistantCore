package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.SkillSheet;
import copel.sesproductpackage.core.unit.Vector;
import copel.sesproductpackage.core.api.gpt.Transformer;

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
        // Valid date strings for selectByPk
        when(rs.getString("register_date")).thenReturn("2023-01-01 10:00:00");
        when(rs.getString("ttl")).thenReturn("2023-01-02 10:00:00");
        when(rs.getString("file_id")).thenReturn("F1");
        when(rs.getString("file_name")).thenReturn("F1.txt");
        when(rs.getString("file_content")).thenReturn("content");
        when(rs.getString("file_content_summary")).thenReturn("summary");
        when(rs.getString("from_group")).thenReturn("G1");
        
        SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
        ss.setFileId("F1");
        ss.setRegisterDate(new OriginalDateTime());
        ss.setTtl(new OriginalDateTime());
        
        assertEquals(1, ss.insert(connection));
        assertFalse(ss.updateByPk(connection));
        
        ss.selectByPk(connection);
        assertNotNull(ss.getRegisterDate());
        
        ss.selectByPkWithoutRawContent(connection);
        assertNotNull(ss.getRegisterDate());
        
        assertTrue(ss.deleteByPk(connection));
        assertNotNull(ss.toString());
    }

    @Test
    void testSKILLSHEETMethods() throws Exception {
        SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
        SkillSheet inner = new SkillSheet("f1", "n1", "c1");
        inner.setFileContentSummary("sum1");
        ss.setSkillSheet(inner);
        
        assertEquals("ファイルID：f1内容：sum1", ss.toスキルシート選出用文章());
        assertNotNull(ss.getFileUrl());
        assertEquals("f1_n1", ss.getObjectKey());
        assertEquals("f1", ss.getFileId());
        assertEquals("n1", ss.getFileName());
        assertEquals("c1", ss.getFileContent());
        assertEquals("sum1", ss.getFileContentSummary());
        
        // embedding
        Transformer mockTrans = mock(Transformer.class);
        when(mockTrans.embedding(anyString())).thenReturn(new float[]{0.1f});
        ss.embedding(mockTrans);
        assertNotNull(ss.getVectorData());
        
        // uniqueCheck
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(0);
        assertTrue(ss.uniqueCheck(conn, 0.8));
        
        // Setters
        ss.setFileId("f2");
        ss.setFileName("n2");
        ss.setFileContent("c2");
        ss.setFileContentSummary("sum2");
        assertEquals("f2", ss.getFileId());
        
        // Ensure toString works with fully populated fields
        ss.setFromGroup("G1");
        ss.setFromId("ID1");
        ss.setFromName("Name1");
        ss.setRegisterDate(new OriginalDateTime());
        ss.setRegisterUser("User1");
        ss.setTtl(new OriginalDateTime());
        ss.setDistance(0.5);
        assertNotNull(ss.toString());
    }

    @Test
    void testSKILLSHEETNulls() throws SQLException {
        SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
        ss.setSkillSheet(null);
        assertNull(ss.getFileId());
        assertNull(ss.getFileName());
        assertEquals("", ss.getFileContent());
        assertEquals("", ss.getFileContentSummary());
        assertNull(ss.getFileUrl());
        
        assertFalse(ss.deleteByPk(null));
        assertEquals(0, ss.insert(null));
        
        ss.selectByPk(null);
        ss.selectByPkWithoutRawContent(null);
        
        // Test toString with null skillSheet
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
        when(rs.getString("file_id")).thenReturn("F1");
        when(rs.getString("register_date")).thenReturn("2023-01-01 10:00:00");
        when(rs.getString("ttl")).thenReturn("2023-01-02 10:00:00");
        
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
        assertNotNull(lot.toスキルシート選出用文章());
    }
}
