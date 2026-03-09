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
  void testSelectOuterJoinByPersonId_Found() throws SQLException {
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
    entity.selectOuterJoinByPersonId(mockConnection, "p1");

    assertEquals("p1", entity.getPersonId());
    assertEquals("f1", entity.getFileId());
    assertEquals("fs", entity.getFileContentSummary());
    assertEquals("ps", entity.getContentSummary());
    assertNotNull(entity.getRegisterDate());
    assertEquals("user", entity.getRegisterUser());

    verify(mockPreparedStatement).setString(1, "p1");
  }

  @Test
  void testSelectOuterJoinByPersonId_NotFound() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(false);

    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectOuterJoinByPersonId(mockConnection, "p99");

    assertNull(entity.getPersonId());
  }

  @Test
  void testSelectOuterJoinByFileId_Found() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("file_id")).thenReturn("f2");
    when(mockResultSet.getString("person_id")).thenReturn("p2");
    when(mockResultSet.getTimestamp("register_date")).thenReturn(null);

    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectOuterJoinByFileId(mockConnection, "f2");

    assertEquals("p2", entity.getPersonId());
    assertEquals("f2", entity.getFileId());
    assertNull(entity.getRegisterDate());

    verify(mockPreparedStatement).setString(1, "f2");
  }

  @Test
  void testSelectOuterJoinByFileId_NotFound() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(false);

    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.selectOuterJoinByFileId(mockConnection, "f99");

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

  @Test
  void testLombokMethods() {
    SES_AI_T_SKILLSHEET_PERSON entity1 = new SES_AI_T_SKILLSHEET_PERSON();
    entity1.setFileId("f1");
    entity1.setFileContentSummary("fs1");
    entity1.setPersonId("p1");
    entity1.setContentSummary("cs1");
    entity1.setRegisterUser("user1");

    SES_AI_T_SKILLSHEET_PERSON entity2 = new SES_AI_T_SKILLSHEET_PERSON();
    entity2.setFileId("f1");
    entity2.setFileContentSummary("fs1");
    entity2.setPersonId("p1");
    entity2.setContentSummary("cs1");
    entity2.setRegisterUser("user1");

    // equals() and hashCode()
    assertTrue(entity1.equals(entity2));
    assertEquals(entity1.hashCode(), entity2.hashCode());

    // canEqual()
    assertTrue(entity1.canEqual(entity2));

    // setters and getters
    assertEquals("f1", entity1.getFileId());
    assertEquals("fs1", entity1.getFileContentSummary());
    assertEquals("p1", entity1.getPersonId());
    assertEquals("cs1", entity1.getContentSummary());

    // different entity
    entity2.setFileId("f2");
    assertFalse(entity1.equals(entity2));
    entity2.setFileId("f1");
    entity2.setPersonId("p2");
    assertFalse(entity1.equals(entity2));
    entity2.setPersonId("p1");
    entity2.setContentSummary("cs2");
    assertFalse(entity1.equals(entity2));
    entity2.setContentSummary("cs1");
    entity2.setFileContentSummary("fs2");
    assertFalse(entity1.equals(entity2));

    // toString()
    String toString = entity1.toString();
    assertTrue(toString.contains("fileId=f1"));
    assertTrue(toString.contains("personId=p1"));

    // edge cases
    assertFalse(entity1.equals(null));
    assertFalse(entity1.equals(new Object()));
    assertTrue(entity1.equals(entity1));

    SES_AI_T_SKILLSHEET_PERSON entityNull1 = new SES_AI_T_SKILLSHEET_PERSON();
    SES_AI_T_SKILLSHEET_PERSON entityNull2 = new SES_AI_T_SKILLSHEET_PERSON();
    assertTrue(entityNull1.equals(entityNull2));
    assertEquals(entityNull1.hashCode(), entityNull2.hashCode());

    // Cover branch: this field is null, other is not
    entityNull2.setFileId("f1");
    assertFalse(entityNull1.equals(entityNull2));
    assertFalse(entityNull2.equals(entityNull1));

    entityNull1.setFileId("f1");
    entityNull2.setFileContentSummary("fs1");
    assertFalse(entityNull1.equals(entityNull2));
    assertFalse(entityNull2.equals(entityNull1));

    entityNull1.setFileContentSummary("fs1");
    entityNull2.setPersonId("p1");
    assertFalse(entityNull1.equals(entityNull2));
    assertFalse(entityNull2.equals(entityNull1));

    entityNull1.setPersonId("p1");
    entityNull2.setContentSummary("cs1");
    assertFalse(entityNull1.equals(entityNull2));
    assertFalse(entityNull2.equals(entityNull1));

    // Additional checks for canEqual and type checks
    assertFalse(entity1.canEqual(new Object()));
    assertFalse(entity1.canEqual(null));

    // Cover branch: !other.canEqual((Object) this)
    SES_AI_T_SKILLSHEET_PERSON entitySub = new SES_AI_T_SKILLSHEET_PERSON() {
      @Override
      protected boolean canEqual(Object other) {
        return false;
      }
    };
    assertFalse(entity1.equals(entitySub));
  }
}
