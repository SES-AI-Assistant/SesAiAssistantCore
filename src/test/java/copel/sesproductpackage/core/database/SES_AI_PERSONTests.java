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
        
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        person.setPersonId("0062ad24f3");
        person.selectByPk(connection);
        
        assertEquals("group1", person.getFromGroup());
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
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(0);

        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        boolean isUnique = person.uniqueCheck(connection, 0.8);
        assertTrue(isUnique);
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
    void testNullCases() throws SQLException {
        SES_AI_T_PERSON person = new SES_AI_T_PERSON();
        assertEquals(0, person.insert(null));
        assertFalse(person.updateByPk(null));
        assertFalse(person.deleteByPk(null));
        person.selectByPk(null);
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
