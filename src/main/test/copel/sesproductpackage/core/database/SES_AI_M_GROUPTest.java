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

public class SES_AI_M_GROUPTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private SES_AI_M_GROUP group;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        group = new SES_AI_M_GROUP();
        group.setFromGroup("group1");
        group.setGroupName("Test Group");
        group.setRegisterDate(new OriginalDateTime("2023-01-01 00:00:00"));
        group.setRegisterUser("testuser");
    }

    @Test
    public void testInsert() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        int result = group.insert(mockConnection);

        assertEquals(1, result);
        verify(mockPreparedStatement).setString(1, "group1");
        verify(mockPreparedStatement).setString(2, "Test Group");
        verify(mockPreparedStatement).setTimestamp(3, new Timestamp(group.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(4, "testuser");
    }

    @Test
    public void testSelectByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("from_group")).thenReturn("group1");
        when(mockResultSet.getString("group_name")).thenReturn("Test Group");
        when(mockResultSet.getString("register_date")).thenReturn("2023-01-01 00:00:00");
        when(mockResultSet.getString("register_user")).thenReturn("testuser");

        group.selectByPk(mockConnection);

        assertEquals("group1", group.getFromGroup());
        assertEquals("Test Group", group.getGroupName());
        assertEquals(new OriginalDateTime("2023-01-01 00:00:00"), group.getRegisterDate());
        assertEquals("testuser", group.getRegisterUser());
    }

    @Test
    public void testUpdateByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = group.updateByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "group1");
        verify(mockPreparedStatement).setString(2, "Test Group");
        verify(mockPreparedStatement).setTimestamp(3, new Timestamp(group.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(4, "testuser");
        verify(mockPreparedStatement).setString(5, "group1");
    }

    @Test
    public void testDeleteByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = group.deleteByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "group1");
    }

    @Test
    public void testDeleteByPk_NoConnection_ReturnsFalse() throws SQLException {
        assertFalse(group.deleteByPk(null));
    }
}
