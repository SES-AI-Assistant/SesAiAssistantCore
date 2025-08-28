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
import copel.sesproductpackage.core.unit.Role;

public class SES_AI_WEBAPP_M_USERTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private SES_AI_WEBAPP_M_USER user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new SES_AI_WEBAPP_M_USER();
        user.setUserId("user1");
        user.setUserName("Test User");
        user.setUserPassword("password");
        user.setCompanyId("comp1");
        user.setRole(Role.ADMIN);
        user.setRegisterDate(new OriginalDateTime("2023-01-01 00:00:00"));
        user.setRegisterUser("testuser");
    }

    @Test
    public void testInsert() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        int result = user.insert(mockConnection);

        assertEquals(1, result);
        verify(mockPreparedStatement).setString(1, "user1");
        verify(mockPreparedStatement).setString(2, "Test User");
        verify(mockPreparedStatement).setString(3, "password");
        verify(mockPreparedStatement).setString(4, "comp1");
        verify(mockPreparedStatement).setString(5, Role.ADMIN.getCode());
        verify(mockPreparedStatement).setTimestamp(6, new Timestamp(user.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(7, "testuser");
    }

    @Test
    public void testSelectByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("user_id")).thenReturn("user1");
        when(mockResultSet.getString("user_name")).thenReturn("Test User");
        when(mockResultSet.getString("user_password")).thenReturn("password");
        when(mockResultSet.getString("company_id")).thenReturn("comp1");
        when(mockResultSet.getString("role_cd")).thenReturn(Role.ADMIN.getCode());
        when(mockResultSet.getString("register_date")).thenReturn("2023-01-01 00:00:00");
        when(mockResultSet.getString("register_user")).thenReturn("testuser");

        user.selectByPk(mockConnection);

        assertEquals("user1", user.getUserId());
        assertEquals("Test User", user.getUserName());
        assertEquals("password", user.getUserPassword());
        assertEquals("comp1", user.getCompanyId());
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(new OriginalDateTime("2023-01-01 00:00:00"), user.getRegisterDate());
        assertEquals("testuser", user.getRegisterUser());
    }

    @Test
    public void testUpdateByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = user.updateByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "user1");
        verify(mockPreparedStatement).setString(2, "Test User");
        verify(mockPreparedStatement).setString(3, "password");
        verify(mockPreparedStatement).setString(4, "comp1");
        verify(mockPreparedStatement).setString(5, Role.ADMIN.getCode());
        verify(mockPreparedStatement).setTimestamp(6, new Timestamp(user.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(7, "testuser");
        verify(mockPreparedStatement).setString(8, "user1");
    }

    @Test
    public void testDeleteByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = user.deleteByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "user1");
    }

    @Test
    public void testDeleteByPk_NoConnection_ReturnsFalse() throws SQLException {
        assertFalse(user.deleteByPk(null));
    }
}
