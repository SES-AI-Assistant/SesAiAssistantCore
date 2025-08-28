package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.OriginalDateTime;

public class SES_AI_T_MATCHTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private SES_AI_T_MATCH match;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        match = new SES_AI_T_MATCH();
        match.setMatchingId("match1");
        match.setUserId("user1");
        match.setJobId("job1");
        match.setPersonId("person1");
        match.setJobContent("Job Content");
        match.setPersonContent("Person Content");
        match.setStatus(MatchingStatus.MATCHING);
        match.setRegisterDate(new OriginalDateTime("2023-01-01 00:00:00"));
        match.setRegisterUser("testuser");
    }

    @Test
    public void testInsert() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        int result = match.insert(mockConnection);

        assertEquals(1, result);
        verify(mockPreparedStatement).setString(2, "user1");
        verify(mockPreparedStatement).setString(3, "job1");
        verify(mockPreparedStatement).setString(4, "person1");
        verify(mockPreparedStatement).setString(5, "Job Content");
        verify(mockPreparedStatement).setString(6, "Person Content");
        verify(mockPreparedStatement).setString(7, MatchingStatus.MATCHING.getCode());
        verify(mockPreparedStatement).setString(9, "testuser");
    }

    @Test
    public void testSelectByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("matching_id")).thenReturn("match1");
        when(mockResultSet.getString("user_id")).thenReturn("user1");
        when(mockResultSet.getString("job_id")).thenReturn("job1");
        when(mockResultSet.getString("person_id")).thenReturn("person1");
        when(mockResultSet.getString("job_content")).thenReturn("Job Content");
        when(mockResultSet.getString("person_content")).thenReturn("Person Content");
        when(mockResultSet.getString("status_cd")).thenReturn(MatchingStatus.MATCHING.getCode());
        when(mockResultSet.getString("register_date")).thenReturn("2023-01-01 00:00:00");
        when(mockResultSet.getString("register_user")).thenReturn("testuser");

        match.selectByPk(mockConnection);

        assertEquals("match1", match.getMatchingId());
        assertEquals("user1", match.getUserId());
        assertEquals("job1", match.getJobId());
        assertEquals("person1", match.getPersonId());
        assertEquals("Job Content", match.getJobContent());
        assertEquals("Person Content", match.getPersonContent());
        assertEquals(MatchingStatus.MATCHING, match.getStatus());
        assertEquals(new OriginalDateTime("2023-01-01 00:00:00"), match.getRegisterDate());
        assertEquals("testuser", match.getRegisterUser());
    }

    @Test
    public void testUpdateByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = match.updateByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "user1");
        verify(mockPreparedStatement).setString(2, "job1");
        verify(mockPreparedStatement).setString(3, "person1");
        verify(mockPreparedStatement).setString(4, "Job Content");
        verify(mockPreparedStatement).setString(5, "Person Content");
        verify(mockPreparedStatement).setString(6, MatchingStatus.MATCHING.getCode());
        verify(mockPreparedStatement).setTimestamp(7, new Timestamp(match.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(8, "testuser");
        verify(mockPreparedStatement).setString(9, "match1");
    }

    @Test
    public void testDeleteByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = match.deleteByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "match1");
    }

    @Test
    public void testDeleteByPk_NoConnection_ReturnsFalse() throws SQLException {
        assertFalse(match.deleteByPk(null));
    }
}
