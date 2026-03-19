package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
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
  void setUp() throws SQLException {
    mockConnection = mock(Connection.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);

    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
  }

  @Test
  void testSelectByPersonId() throws SQLException {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("file_id")).thenReturn("f1");
    when(mockResultSet.getString("file_name")).thenReturn("n1");
    when(mockResultSet.getString("file_content_summary")).thenReturn("fs1");
    when(mockResultSet.getString("person_id")).thenReturn("p1");
    when(mockResultSet.getString("raw_content")).thenReturn("r1");
    when(mockResultSet.getString("content_summary")).thenReturn("cs1");
    when(mockResultSet.getTimestamp("register_date")).thenReturn(new Timestamp(System.currentTimeMillis()));
    when(mockResultSet.getString("register_user")).thenReturn("u1");

    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectByPersonId(mockConnection, "p1");

    assertEquals("f1", entity.getFileId());
    assertEquals("p1", entity.getPersonId());
  }

  @Test
  void testSelectOuterJoinByPersonId() throws SQLException {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getTimestamp("register_date")).thenReturn(null);
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectOuterJoinByPersonId(mockConnection, "p1");
    assertNull(entity.getRegisterDate());
  }

  @Test
  void testSelectByFileId() throws SQLException {
    when(mockResultSet.next()).thenReturn(true);
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectByFileId(mockConnection, "f1");
    verify(mockPreparedStatement).setString(1, "f1");
  }

  @Test
  void testSelectOuterJoinByFileId() throws SQLException {
    when(mockResultSet.next()).thenReturn(true);
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectOuterJoinByFileId(mockConnection, "f1");
    verify(mockPreparedStatement).setString(1, "f1");
  }

  @Test
  void testTo要員選出用文章() {
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.setPersonId("p1");
    entity.setContentSummary("cs1");
    entity.setFileContentSummary("fs1");
    assertEquals("要員ID：p1 内容：cs1fs1", entity.to要員選出用文章());

    entity.setContentSummary(null);
    entity.setFileContentSummary(null);
    assertEquals("要員ID：p1 内容：", entity.to要員選出用文章());
  }

  @Test
  void testToスキルシート選出用文章() {
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.setFileId("f1");
    entity.setContentSummary("cs1");
    entity.setFileContentSummary("fs1");
    assertEquals("ファイルID：f1 内容：fs1cs1", entity.toスキルシート選出用文章());
  }

  @Test
  void testDummyImplementations() throws Exception {
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    assertDoesNotThrow(() -> entity.embedding(null));
    assertFalse(entity.uniqueCheck(mockConnection, 0.5));
    assertEquals(0, entity.insert(mockConnection));
    assertDoesNotThrow(() -> entity.selectByPk(mockConnection));
    assertFalse(entity.updateByPk(mockConnection));
    assertFalse(entity.deleteByPk(mockConnection));
    assertNull(entity.getCheckSql());
    assertNull(entity.getRawContent());
    assertNull(entity.getContentSummary());
  }

  @Test
  void testLombokMethods() {
    SES_AI_T_SKILLSHEET_PERSON entity1 = new SES_AI_T_SKILLSHEET_PERSON();
    entity1.setFileId("f1");
    entity1.setFileContentSummary("fs1");
    entity1.setPersonId("p1");
    entity1.setContentSummary("cs1");

    SES_AI_T_SKILLSHEET_PERSON entity2 = new SES_AI_T_SKILLSHEET_PERSON();
    entity2.setFileId("f1");
    entity2.setFileContentSummary("fs1");
    entity2.setPersonId("p1");
    entity2.setContentSummary("cs1");

    assertEquals(entity1, entity2);
    assertEquals(entity1.hashCode(), entity2.hashCode());
    assertNotNull(entity1.toString());
  }
}
