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

import copel.sesproductpackage.core.unit.OriginalDateTime;

public class SES_AI_T_JOBTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private SES_AI_T_JOB job;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        job = new SES_AI_T_JOB();
        job.setJobId("job1");
        job.setFromGroup("group1");
        job.setFromId("from1");
        job.setFromName("name1");
        job.setRawContent("Raw content");
        job.setRegisterDate(new OriginalDateTime("2023-01-01 00:00:00"));
        job.setRegisterUser("testuser");
        job.setTtl(new OriginalDateTime("2024-01-01 00:00:00"));
    }

    @Test
    public void testInsert() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        int result = job.insert(mockConnection);

        assertEquals(1, result);
        verify(mockPreparedStatement).setString(2, "group1");
        verify(mockPreparedStatement).setString(3, "from1");
        verify(mockPreparedStatement).setString(4, "name1");
        verify(mockPreparedStatement).setString(5, "Raw content");
        verify(mockPreparedStatement).setString(6, null);
        verify(mockPreparedStatement).setTimestamp(7, new Timestamp(job.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(8, "testuser");
        verify(mockPreparedStatement).setTimestamp(9, new Timestamp(job.getTtl().toTime()));
    }

    @Test
    public void testSelectByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("from_group")).thenReturn("group1");
        when(mockResultSet.getString("from_id")).thenReturn("from1");
        when(mockResultSet.getString("from_name")).thenReturn("name1");
        when(mockResultSet.getString("raw_content")).thenReturn("Raw content");
        when(mockResultSet.getString("register_date")).thenReturn("2023-01-01 00:00:00");
        when(mockResultSet.getString("register_user")).thenReturn("testuser");
        when(mockResultSet.getString("ttl")).thenReturn("2024-01-01 00:00:00");

        job.selectByPk(mockConnection);

        assertEquals("group1", job.getFromGroup());
        assertEquals("from1", job.getFromId());
        assertEquals("name1", job.getFromName());
        assertEquals("Raw content", job.getRawContent());
        assertEquals(new OriginalDateTime("2023-01-01 00:00:00"), job.getRegisterDate());
        assertEquals("testuser", job.getRegisterUser());
        assertEquals(new OriginalDateTime("2024-01-01 00:00:00"), job.getTtl());
    }

    @Test
    public void testDeleteByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = job.deleteByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "job1");
    }

    @Test
    public void testDeleteByPk_NoConnection_ReturnsFalse() throws SQLException {
        assertFalse(job.deleteByPk(null));
    }
    
    @Test
    public void testUpdateByPk() throws SQLException {
        assertFalse(job.updateByPk(mockConnection));
    }
}
