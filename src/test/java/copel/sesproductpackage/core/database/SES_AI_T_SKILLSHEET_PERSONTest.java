package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SES_AI_T_SKILLSHEET_PERSONTest {

  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;

  @BeforeEach
  void setUp() {
    mockConnection = mock(Connection.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);
  }

  @Test
  void testSelectByPersonId_Found() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("file_id")).thenReturn("f1");
    when(mockResultSet.getString("file_content_summary")).thenReturn("fs");
    when(mockResultSet.getString("person_id")).thenReturn("p1");
    when(mockResultSet.getString("content_summary")).thenReturn("ps");
    when(mockResultSet.getTimestamp("register_date"))
        .thenReturn(new Timestamp(System.currentTimeMillis()));
    when(mockResultSet.getString("register_user")).thenReturn("user");

    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectByPersonId(mockConnection, "p1");

    assertEquals("p1", entity.getPersonId());
    assertEquals("f1", entity.getFileId());
    assertEquals("fs", entity.getFileContentSummary());
    assertEquals("ps", entity.getContentSummary());
    assertNotNull(entity.getRegisterDate());
    assertEquals("user", entity.getRegisterUser());

    verify(mockPreparedStatement).setString(1, "p1");
  }

  @Test
  void testSelectByPersonId_NotFound() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(false);

    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectByPersonId(mockConnection, "p99");

    assertNull(entity.getPersonId());
  }

  @Test
  void testSelectByFileId_Found() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("file_id")).thenReturn("f2");
    when(mockResultSet.getString("person_id")).thenReturn("p2");
    when(mockResultSet.getTimestamp("register_date")).thenReturn(null);

    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectByFileId(mockConnection, "f2");

    assertEquals("p2", entity.getPersonId());
    assertEquals("f2", entity.getFileId());
    assertNull(entity.getRegisterDate());

    verify(mockPreparedStatement).setString(1, "f2");
  }

  @Test
  void testSelectByFileId_NotFound() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(false);

    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectByFileId(mockConnection, "f99");

    assertNull(entity.getPersonId());
  }

  @Test
  void testNotImplementedMethods() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    assertDoesNotThrow(() -> entity.embedding(null));
    assertFalse(entity.uniqueCheck(mockConnection, 0.5));
    assertEquals(0, entity.insert(mockConnection));
    assertDoesNotThrow(() -> entity.selectByPk(mockConnection));
    assertFalse(entity.updateByPk(mockConnection));
    assertFalse(entity.deleteByPk(mockConnection));
    assertNull(entity.getRawContent());
    assertNull(entity.getCheckSql());

    entity.setContentSummary("hoge");
    assertEquals("hoge", entity.getContentSummary());
  }

  @Test
  void testTo要員選出用文章() {
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.setPersonId("p3");
    entity.setContentSummary("cSummary");
    entity.setFileContentSummary("fSummary");
    assertEquals("要員ID：p3 内容：cSummaryfSummary", entity.to要員選出用文章());

    entity.setContentSummary(null);
    entity.setFileContentSummary(null);
    assertEquals("要員ID：p3 内容：", entity.to要員選出用文章());
  }
}
