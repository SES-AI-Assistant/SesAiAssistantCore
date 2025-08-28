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

public class SES_AI_T_PERSONLotTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private SES_AI_T_PERSONLot personLot;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        personLot = new SES_AI_T_PERSONLot();
    }

    @Test
    public void testSelectAll() throws SQLException {
        // Mocking the database interaction
        when(mockConnection.prepareStatement("SELECT person_id, from_group, from_id, from_name, raw_content, file_id, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON")).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Mocking the result set
        when(mockResultSet.next()).thenReturn(true, true, false); // Two rows of data
        when(mockResultSet.getString("person_id")).thenReturn("person1", "person2");
        when(mockResultSet.getString("from_group")).thenReturn("group1", "group2");
        when(mockResultSet.getString("from_id")).thenReturn("from1", "from2");
        when(mockResultSet.getString("from_name")).thenReturn("name1", "name2");
        when(mockResultSet.getString("raw_content")).thenReturn("content1", "content2");
        when(mockResultSet.getString("file_id")).thenReturn("file1", "file2");
        when(mockResultSet.getString("register_date")).thenReturn("2023-01-01 00:00:00", "2023-01-02 00:00:00");
        when(mockResultSet.getString("register_user")).thenReturn("user1", "user2");
        when(mockResultSet.getString("ttl")).thenReturn("2024-01-01 00:00:00", "2024-01-02 00:00:00");

        // Call the method to be tested
        personLot.selectAll(mockConnection);

        // Assertions
        assertEquals(2, personLot.size());

        SES_AI_T_PERSON person1 = personLot.get(0);
        assertEquals("person1", person1.getPersonId());
        assertEquals("group1", person1.getFromGroup());
        assertEquals("from1", person1.getFromId());
        assertEquals("name1", person1.getFromName());
        assertEquals("content1", person1.getRawContent());
        assertEquals("file1", person1.getFileId());
        assertEquals(new OriginalDateTime("2023-01-01 00:00:00"), person1.getRegisterDate());
        assertEquals("user1", person1.getRegisterUser());
        assertEquals(new OriginalDateTime("2024-01-01 00:00:00"), person1.getTtl());

        SES_AI_T_PERSON person2 = personLot.get(1);
        assertEquals("person2", person2.getPersonId());
        assertEquals("group2", person2.getFromGroup());
        assertEquals("from2", person2.getFromId());
        assertEquals("name2", person2.getFromName());
        assertEquals("content2", person2.getRawContent());
        assertEquals("file2", person2.getFileId());
        assertEquals(new OriginalDateTime("2023-01-02 00:00:00"), person2.getRegisterDate());
        assertEquals("user2", person2.getRegisterUser());
        assertEquals(new OriginalDateTime("2024-01-02 00:00:00"), person2.getTtl());
    }
}
