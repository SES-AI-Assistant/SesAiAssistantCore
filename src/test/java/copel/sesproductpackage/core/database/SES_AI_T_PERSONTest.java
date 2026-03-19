package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_T_PERSONTest {

  @Test
  void testSelectByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);

    when(rs.getString("from_group")).thenReturn("group1");
    when(rs.getString("from_id")).thenReturn("id1");
    when(rs.getString("from_name")).thenReturn("name1");
    when(rs.getString("raw_content")).thenReturn("raw1");
    when(rs.getString("content_summary")).thenReturn("sum1");
    when(rs.getString("file_id")).thenReturn("fid1");
    when(rs.getString("file_summary")).thenReturn("fsum1");
    when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("ttl")).thenReturn("2026-12-31 23:59:59");

    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId("P1");
    person.selectByPk(connection);

    assertEquals("group1", person.getFromGroup());
    assertNotNull(person.toString());

    when(rs.next()).thenReturn(true, true);
    when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00", null);
    when(rs.getString("ttl")).thenReturn("2026-12-31 23:59:59", null);
    SES_AI_T_PERSON person2 = new SES_AI_T_PERSON();
    person2.setPersonId("P2");
    person2.selectByPk(connection);
    assertEquals("group1", person2.getFromGroup());
  }

  @Test
  void testSelectByPkWithNullDates() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getString("from_group")).thenReturn("g1");
    when(rs.getString("from_id")).thenReturn("i1");
    when(rs.getString("from_name")).thenReturn("n1");
    when(rs.getString("raw_content")).thenReturn("r1");
    when(rs.getString("content_summary")).thenReturn("s1");
    when(rs.getString("file_id")).thenReturn("f1");
    when(rs.getString("file_summary")).thenReturn("fs1");
    when(rs.getString("register_date")).thenReturn(null);
    when(rs.getString("register_user")).thenReturn("u1");
    when(rs.getString("ttl")).thenReturn(null);
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId("P1");
    person.selectByPk(connection);
    assertEquals("g1", person.getFromGroup());
  }

  @Test
  void testInsert() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setRegisterDate(new OriginalDateTime());
    person.setTtl(new OriginalDateTime());
    int result = person.insert(connection);

    assertEquals(1, result);
    assertNotNull(person.getPersonId());

    SES_AI_T_PERSON personWithVector = new SES_AI_T_PERSON();
    personWithVector.setVectorData(new Vector(mock(Transformer.class)));
    when(ps.executeUpdate()).thenReturn(1);
    assertEquals(1, personWithVector.insert(connection));
  }

  @Test
  void testUpdateByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId("id1");
    person.setRegisterDate(new OriginalDateTime());
    person.setTtl(new OriginalDateTime());
    boolean result = person.updateByPk(connection);

    assertTrue(result);

    SES_AI_T_PERSON personWithVector = new SES_AI_T_PERSON();
    personWithVector.setPersonId("id2");
    personWithVector.setVectorData(new Vector(mock(Transformer.class)));
    personWithVector.setTtl(new OriginalDateTime());
    assertTrue(personWithVector.updateByPk(connection));
  }

  @Test
  void testDeleteByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId("id1");
    boolean result = person.deleteByPk(connection);

    assertTrue(result);
  }

  @Test
  void testEmbedding() throws Exception {
    Transformer transformer = mock(Transformer.class);
    when(transformer.embedding(anyString())).thenReturn(new float[] {0.1f});

    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setContentSummary("summary");
    person.embedding(transformer);

    assertNotNull(person.getVectorData());
  }

  @Test
  void testUniqueCheck() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);

    SES_AI_T_PERSON person = new SES_AI_T_PERSON();

    when(rs.next()).thenReturn(true);
    when(rs.getInt(1)).thenReturn(0);
    assertTrue(person.uniqueCheck(connection, 0.8));

    when(rs.next()).thenReturn(true);
    when(rs.getInt(1)).thenReturn(1);
    assertFalse(person.uniqueCheck(connection, 0.8));
  }

  @Test
  void testGetCheckSql() throws Exception {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    java.lang.reflect.Method m = SES_AI_T_PERSON.class.getDeclaredMethod("getCheckSql");
    m.setAccessible(true);
    assertNotNull(m.invoke(person));
    assertTrue(m.invoke(person).toString().contains("raw_content"));
  }

  @Test
  void testDeleteByPkWithNullPersonId() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(0);
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId(null);
    assertFalse(person.deleteByPk(connection));
  }

  @Test
  void testUpdateFileIdByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId("id1");
    person.setFileId("fid1");
    assertTrue(person.updateFileIdByPk(connection));

    assertFalse(person.updateFileIdByPk(null));
    person.setPersonId(null);
    assertFalse(person.updateFileIdByPk(connection));

    person.setPersonId("id1");
    person.setFileId(null);
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(person.updateFileIdByPk(connection));
  }

  @Test
  void testIsSkillSheetRegistered() {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    assertFalse(person.isスキルシート登録済());
    person.setFileId("");
    assertFalse(person.isスキルシート登録済());
    person.setFileId("F1");
    assertTrue(person.isスキルシート登録済());
  }

  @Test
  void testToPersonSelectionText() {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId("P1");
    person.setRawContent("Content");
    person.setFileSummary("FileSummary");
    assertEquals("要員ID：P1内容：ContentFileSummary", person.to要員選出用文章());

    person.setFileSummary(null);
    assertTrue(person.to要員選出用文章().contains("要員ID：P1"));
    assertTrue(person.to要員選出用文章().contains("内容：Content"));
  }

  @Test
  void testGetRawContentAndGetContentSummary() {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setRawContent("raw");
    person.setContentSummary("summary");
    assertEquals("raw", person.getRawContent());
    assertEquals("summary", person.getContentSummary());
  }

  @Test
  void testNullCases() throws SQLException {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    assertEquals(0, person.insert(null));
    assertFalse(person.updateByPk(null));
    assertFalse(person.deleteByPk(null));
    person.selectByPk(null);

    Connection connection = mock(Connection.class);
    person.setPersonId(null);
    person.selectByPk(connection);
    assertFalse(person.updateByPk(connection));

    person.setPersonId("P1");
    person.setVectorData(null);
    person.setRegisterDate(null);
    person.setTtl(null);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    person.insert(connection);
    person.updateByPk(connection);

    person.setPersonId("P1");
    ResultSet rs = mock(ResultSet.class);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);
    person.selectByPk(connection);

    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(person.deleteByPk(connection));
  }

  @Test
  void testSES_AI_T_PERSONLombok() {
    SES_AI_T_PERSON p1 = new SES_AI_T_PERSON();
    SES_AI_T_PERSON p2 = new SES_AI_T_PERSON();
    p1.setPersonId("id1");
    p2.setPersonId("id1");
    assertEquals(p1, p1);
    assertEquals(p1, p2);
    assertEquals(p1.hashCode(), p2.hashCode());
    assertNotNull(p1.toString());
    assertTrue(p1.canEqual(p2));
    assertNotEquals(p1, null);
    assertNotEquals(p1, "str");

    p2.setPersonId("id2");
    assertNotEquals(p1, p2);
    p1.setPersonId("id2");
    assertEquals(p1, p2);
  }
}
