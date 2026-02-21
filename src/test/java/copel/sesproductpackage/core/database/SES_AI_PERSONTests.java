package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.OriginalDateTime;

class SES_AI_PERSONTests {

    @Test
    void testSelectByPk() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        
        when(rs.getString("from_group")).thenReturn("group1");
        when(rs.getString("from_id")).thenReturn("id1");
        when(rs.getString("from_name")).thenReturn("name1");
        when(rs.getString("raw_content")).thenReturn("raw1");
        when(rs.getString("content_summary")).thenReturn("sum1");
        when(rs.getString("file_id")).thenReturn("fid1");
        when(rs.getString("file_summary")).thenReturn("fsum1");
        when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00");
        when(rs.getString("register_user")).thenReturn("admin");
        when(rs.getString("ttl")).thenReturn("2026-12-31 23:59:59");
        
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        person.setPersonId("P1");
        person.selectByPk(connection);
        
        assertEquals("group1", person.getFromGroup());
        assertEquals("id1", person.getFromId());
        assertEquals("name1", person.getFromName());
        assertEquals("raw1", person.getRawContent());
        assertEquals("sum1", person.getContentSummary());
        assertEquals("fid1", person.getFileId());
        assertEquals("fsum1", person.getFileSummary());
        assertNotNull(person.toString());
    }

    @Test
    void testInsert() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        person.setRegisterDate(new OriginalDateTime());
        person.setTtl(new OriginalDateTime());
        int result = person.insert(connection);
        
        assertEquals(1, result);
        assertNotNull(person.getPersonId());
    }

    @Test
    void testUpdateByPk() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        person.setPersonId("id1");
        person.setRegisterDate(new OriginalDateTime());
        person.setTtl(new OriginalDateTime());
        boolean result = person.updateByPk(connection);
        
        assertTrue(result);
    }

    @Test
    void testDeleteByPk() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        person.setPersonId("id1");
        boolean result = person.deleteByPk(connection);
        
        assertTrue(result);
    }

    @Test
    void testEmbedding() throws Exception {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding(anyString())).thenReturn(new float[]{0.1f});
        
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        person.setContentSummary("summary");
        person.embedding(transformer);
        
        assertNotNull(person.getVectorData());
    }

    @Test
    void testUniqueCheck() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        
        // Unique case (count < 1)
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(0);
        assertTrue(person.uniqueCheck(connection, 0.8));
        
        // Not unique case (count >= 1)
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(1);
        assertFalse(person.uniqueCheck(connection, 0.8));
    }

    @Test
    void testUpdateFileIdByPk() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        person.setPersonId("id1");
        person.setFileId("fid1");
        assertTrue(person.updateFileIdByPk(connection));
        
        // Null case
        person.setPersonId(null);
        assertFalse(person.updateFileIdByPk(connection));
    }

    @Test
    void testIsスキルシート登録済() {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        assertFalse(person.isスキルシート登録済());
        person.setFileId("");
        assertFalse(person.isスキルシート登録済());
        person.setFileId("F1");
        assertTrue(person.isスキルシート登録済());
    }

    @Test
    void testNullCases() throws SQLException {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        assertEquals(0, person.insert(null));
        assertFalse(person.updateByPk(null));
        assertFalse(person.deleteByPk(null));
        person.selectByPk(null);
        
        Connection connection = mock(Connection.class);
        person.setPersonId(null);
        person.selectByPk(connection);
        assertFalse(person.updateByPk(connection));
        
        person.setPersonId("P1");
        person.setVectorData(null);
        person.setRegisterDate(null);
        person.setTtl(null);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        person.insert(connection);
        person.updateByPk(connection);
        
        // ResultSet.next() false
        ResultSet rs = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        person.selectByPk(connection);
        
        // Covering Lombok
        assertNotNull(person.toString());
        assertNotNull(person.hashCode());
        assertTrue(person.equals(person));
    }

    @Test
    void testTo要員選出用文章() {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        person.setPersonId("P1");
        person.setRawContent("Content");
        person.setFileSummary("FileSummary");
        assertEquals("要員ID：P1内容：ContentFileSummary", person.to要員選出用文章());
    }

    @Test
    void testUniqueCheckBranch() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(1); // Not unique

        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        assertFalse(person.uniqueCheck(connection, 0.8));
    }

    @Test
    void testUpdateFileIdByPkBranch() throws SQLException {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        assertFalse(person.updateFileIdByPk(null)); // connection null
    }
}
