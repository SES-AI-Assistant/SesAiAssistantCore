package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import copel.sesproductpackage.core.unit.OriginalDateTime;

public class SES_AI_T_SKILLSHEETLotTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private SES_AI_T_SKILLSHEETLot skillsheetLot;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        skillsheetLot = new SES_AI_T_SKILLSHEETLot();
    }

    @Test
    public void testSelectAll() throws SQLException {
        // Mocking the database interaction
        when(mockConnection.prepareStatement("SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET")).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Mocking the result set
        when(mockResultSet.next()).thenReturn(true, true, false); // Two rows of data
        when(mockResultSet.getString("from_group")).thenReturn("group1", "group2");
        when(mockResultSet.getString("from_id")).thenReturn("from1", "from2");
        when(mockResultSet.getString("from_name")).thenReturn("name1", "name2");
        when(mockResultSet.getString("file_id")).thenReturn("file1", "file2");
        when(mockResultSet.getString("file_name")).thenReturn("fileName1", "fileName2");
        when(mockResultSet.getString("file_content")).thenReturn("content1", "content2");
        when(mockResultSet.getString("file_content_summary")).thenReturn("summary1", "summary2");
        when(mockResultSet.getString("register_date")).thenReturn("2023-01-01 00:00:00", "2023-01-02 00:00:00");
        when(mockResultSet.getString("register_user")).thenReturn("user1", "user2");
        when(mockResultSet.getString("ttl")).thenReturn("2024-01-01 00:00:00", "2024-01-02 00:00:00");

        // Call the method to be tested
        skillsheetLot.selectAll(mockConnection);

        // Assertions
        assertEquals(2, skillsheetLot.size());

        SES_AI_T_SKILLSHEET skillsheet1 = skillsheetLot.get(0);
        assertEquals("group1", skillsheet1.getFromGroup());
        assertEquals("from1", skillsheet1.getFromId());
        assertEquals("name1", skillsheet1.getFromName());
        assertEquals("file1", skillsheet1.getFileId());
        assertEquals("fileName1", skillsheet1.getFileName());
        assertEquals("content1", skillsheet1.getFileContent());
        assertEquals("summary1", skillsheet1.getFileContentSummary());
        assertEquals(new OriginalDateTime("2023-01-01 00:00:00"), skillsheet1.getRegisterDate());
        assertEquals("user1", skillsheet1.getRegisterUser());
        assertEquals(new OriginalDateTime("2024-01-01 00:00:00"), skillsheet1.getTtl());

        SES_AI_T_SKILLSHEET skillsheet2 = skillsheetLot.get(1);
        assertEquals("group2", skillsheet2.getFromGroup());
        assertEquals("from2", skillsheet2.getFromId());
        assertEquals("name2", skillsheet2.getFromName());
        assertEquals("file2", skillsheet2.getFileId());
        assertEquals("fileName2", skillsheet2.getFileName());
        assertEquals("content2", skillsheet2.getFileContent());
        assertEquals("summary2", skillsheet2.getFileContentSummary());
        assertEquals(new OriginalDateTime("2023-01-02 00:00:00"), skillsheet2.getRegisterDate());
        assertEquals("user2", skillsheet2.getRegisterUser());
        assertEquals(new OriginalDateTime("2024-01-02 00:00:00"), skillsheet2.getTtl());
    }
}
