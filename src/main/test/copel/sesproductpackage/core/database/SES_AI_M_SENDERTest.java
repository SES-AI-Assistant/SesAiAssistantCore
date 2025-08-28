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

public class SES_AI_M_SENDERTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private SES_AI_M_SENDER sender;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        sender = new SES_AI_M_SENDER();
        sender.setFromId("sender1");
        sender.setFromName("Test Sender");
        sender.setCompanyId("comp1");
        sender.setRegisterDate(new OriginalDateTime("2023-01-01 00:00:00"));
        sender.setRegisterUser("testuser");
    }

    @Test
    public void testInsert() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        int result = sender.insert(mockConnection);

        assertEquals(1, result);
        verify(mockPreparedStatement).setString(1, "sender1");
        verify(mockPreparedStatement).setString(2, "Test Sender");
        verify(mockPreparedStatement).setString(3, "comp1");
        verify(mockPreparedStatement).setTimestamp(4, new Timestamp(sender.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(5, "testuser");
    }

    @Test
    public void testSelectByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("from_id")).thenReturn("sender1");
        when(mockResultSet.getString("from_name")).thenReturn("Test Sender");
        when(mockResultSet.getString("company_id")).thenReturn("comp1");
        when(mockResultSet.getString("register_date")).thenReturn("2023-01-01 00:00:00");
        when(mockResultSet.getString("register_user")).thenReturn("testuser");

        sender.selectByPk(mockConnection);

        assertEquals("sender1", sender.getFromId());
        assertEquals("Test Sender", sender.getFromName());
        assertEquals("comp1", sender.getCompanyId());
        assertEquals(new OriginalDateTime("2023-01-01 00:00:00"), sender.getRegisterDate());
        assertEquals("testuser", sender.getRegisterUser());
    }

    @Test
    public void testUpdateByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = sender.updateByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "sender1");
        verify(mockPreparedStatement).setString(2, "Test Sender");
        verify(mockPreparedStatement).setString(3, "comp1");
        verify(mockPreparedStatement).setTimestamp(4, new Timestamp(sender.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(5, "testuser");
        verify(mockPreparedStatement).setString(6, "sender1");
    }

    @Test
    public void testDeleteByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = sender.deleteByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "sender1");
    }

    @Test
    public void testDeleteByPk_NoConnection_ReturnsFalse() throws SQLException {
        assertFalse(sender.deleteByPk(null));
    }
}
