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

public class SES_AI_M_COMPANYTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private SES_AI_M_COMPANY company;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        company = new SES_AI_M_COMPANY();
        company.setCompanyId("comp1");
        company.setCompanyName("Test Company");
        company.setMemo("This is a test company.");
        company.setRegisterDate(new OriginalDateTime("2023-01-01 00:00:00"));
        company.setRegisterUser("testuser");
    }

    @Test
    public void testInsert() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        int result = company.insert(mockConnection);

        assertEquals(1, result);
        verify(mockPreparedStatement).setString(1, "comp1");
        verify(mockPreparedStatement).setString(2, "Test Company");
        verify(mockPreparedStatement).setString(3, "This is a test company.");
        verify(mockPreparedStatement).setTimestamp(4, new Timestamp(company.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(5, "testuser");
    }

    @Test
    public void testSelectByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("company_id")).thenReturn("comp1");
        when(mockResultSet.getString("company_name")).thenReturn("Test Company");
        when(mockResultSet.getString("memo")).thenReturn("This is a test company.");
        when(mockResultSet.getString("register_date")).thenReturn("2023-01-01 00:00:00");
        when(mockResultSet.getString("register_user")).thenReturn("testuser");

        company.selectByPk(mockConnection);

        assertEquals("comp1", company.getCompanyId());
        assertEquals("Test Company", company.getCompanyName());
        assertEquals("This is a test company.", company.getMemo());
        assertEquals(new OriginalDateTime("2023-01-01 00:00:00"), company.getRegisterDate());
        assertEquals("testuser", company.getRegisterUser());
    }

    @Test
    public void testUpdateByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = company.updateByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "comp1");
        verify(mockPreparedStatement).setString(2, "Test Company");
        verify(mockPreparedStatement).setString(3, "This is a test company.");
        verify(mockPreparedStatement).setTimestamp(4, new Timestamp(company.getRegisterDate().toTime()));
        verify(mockPreparedStatement).setString(5, "testuser");
        verify(mockPreparedStatement).setString(6, "comp1");
    }

    @Test
    public void testDeleteByPk() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = company.deleteByPk(mockConnection);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "comp1");
    }
    
    @Test
    public void testDeleteByPk_NoConnection_ReturnsFalse() throws SQLException {
        assertFalse(company.deleteByPk(null));
    }
}
