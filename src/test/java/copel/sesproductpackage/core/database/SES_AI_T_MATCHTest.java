package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.MatchingStatus;

public class SES_AI_T_MATCHTest {

    private SES_AI_T_MATCH match;

    @BeforeEach
    public void setUp() {
        match = new SES_AI_T_MATCH();
        match.setUserId("user001");
        match.setJobId("job001");
        match.setPersonId("person001");
        match.setJobContent("Java Engineer");
        match.setPersonContent("Experienced Java Developer");
        match.setStatus(MatchingStatus.サジェスト中);
        match.setRegisterUser("test_admin");
    }

    @Test
    public void testHasJobId() {
        assertTrue(match.hasJobId());

        match.setJobId("");
        assertFalse(match.hasJobId());
    }

    @Test
    public void testHasPersonId() {
        assertTrue(match.hasPersonId());

        match.setPersonId(null);
        assertFalse(match.hasPersonId());
    }

    @Test
    public void testInsert() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeUpdate()).thenReturn(1);

        int result = match.insert(conn);
        assertEquals(1, result);
        assertNotNull(match.getMatchingId());
        assertEquals(10, match.getMatchingId().length());
    }

    @Test
    public void testSelectByPk() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        match.setMatchingId("match12345");
        match.setPersonId("person001");
        match.setJobId("job001");

        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("user_id")).thenReturn("user001");
        when(rs.getString("job_id")).thenReturn("job001");
        when(rs.getString("person_id")).thenReturn("person001");
        when(rs.getString("job_content")).thenReturn("Java Engineer");
        when(rs.getString("person_content")).thenReturn("Experienced Java Developer");
        when(rs.getString("status_cd")).thenReturn("10");
        when(rs.getString("register_date")).thenReturn("2024-04-01 12:00:00");
        when(rs.getString("register_user")).thenReturn("test_admin");

        match.selectByPk(conn);

        assertEquals("user001", match.getUserId());
        assertEquals("10", match.getStatus().getCode());
    }

    @Test
    public void testUpdateByPk() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeUpdate()).thenReturn(1);

        match.setMatchingId("match12345");
        match.setPersonId("person001");
        match.setJobId("job001");

        boolean result = match.updateByPk(conn);
        assertTrue(result);
    }

    @Test
    public void testDeleteByPk() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeUpdate()).thenReturn(1);

        match.setMatchingId("match12345");
        match.setPersonId("person001");
        match.setJobId("job001");

        boolean result = match.deleteByPk(conn);
        assertTrue(result);
    }

    @Test
    public void testGetterAndSetter() {
        String jobContent = "Backend Dev";
        match.setJobContent(jobContent);
        assertEquals(jobContent, match.getJobContent());

        String personContent = "SE with 10y exp";
        match.setPersonContent(personContent);
        assertEquals(personContent, match.getPersonContent());

        String userId = "user100";
        match.setUserId(userId);
        assertEquals(userId, match.getUserId());
    }
}
