package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.SkillSheet;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_T_SKILLSHEETTests {

  @Test
  void testSKILLSHEETBasics() throws SQLException {
    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
    assertNotNull(ss.getSkillSheet());

    ss.setFromGroup("G1");
    ss.setFromId("ID1");
    ss.setFromName("N1");
    ss.setRegisterUser("U1");
    ss.setDistance(1.0);

    assertEquals("G1", ss.getFromGroup());
    assertEquals("ID1", ss.getFromId());
    assertEquals("N1", ss.getFromName());
    assertEquals("U1", ss.getRegisterUser());
    assertEquals(1.0, ss.getDistance());

    assertNotNull(ss.toString());
  }

  @Test
  void testGetFileUrlAndKey() {
    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
    ss.setSkillSheet(new SkillSheet("F1", "N1", "C1"));
    assertNotNull(ss.getFileUrl());
    assertEquals("F1_N1", ss.getObjectKey());

    ss.setSkillSheet(null);
    assertNull(ss.getFileUrl());
    try {
      ss.getObjectKey();
    } catch (Exception e) {
    }
  }

  @Test
  void testSelectByPkBranches() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);

    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();

    ss.selectByPk(null);
    ss.selectByPkWithoutRawContent(null);

    ss.setFileId(null);
    ss.selectByPk(connection);
    ss.selectByPkWithoutRawContent(connection);

    ss.setFileId("F1");
    when(rs.next()).thenReturn(false);
    ss.selectByPk(connection);
    ss.selectByPkWithoutRawContent(connection);

    when(rs.next()).thenReturn(true);
    when(rs.getString(anyString())).thenReturn("val");
    ss.selectByPk(connection);
    ss.selectByPkWithoutRawContent(connection);
  }

  @Test
  void testInsertBranches() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);

    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();

    assertEquals(0, ss.insert(null));

    ss.setSkillSheet(null);
    ss.setVectorData(null);
    ss.setRegisterDate(null);
    ss.setTtl(null);
    ss.insert(connection);

    ss.setSkillSheet(new SkillSheet("F1", "N1", "C1"));

    Vector vector = new Vector(null);
    java.lang.reflect.Field valueField = Vector.class.getDeclaredField("value");
    valueField.setAccessible(true);
    valueField.set(vector, new float[] {0.1f});
    ss.setVectorData(vector);

    ss.setRegisterDate(new OriginalDateTime());
    ss.setTtl(new OriginalDateTime());
    ss.insert(connection);
  }

  @Test
  void testEmbeddingAndCheck() throws Exception {
    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
    Transformer trans = mock(Transformer.class);
    when(trans.embedding(anyString())).thenReturn(new float[] {0.1f});

    ss.embedding(trans);

    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getInt(1)).thenReturn(1);

    assertFalse(ss.uniqueCheck(conn, 0.5));

    ss.setSkillSheet(null);
    ss.uniqueCheck(conn, 0.5);
  }

  @Test
  void testDeleteByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
    assertFalse(ss.deleteByPk(null));

    ss.setFileId("F1");
    assertTrue(ss.deleteByPk(connection));
  }

  @Test
  void testGetterSetterCoverage() {
    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
    ss.setFileContent("content");
    assertEquals("content", ss.getFileContent());
    ss.setFileContentSummary("summary");
    assertEquals("summary", ss.getFileContentSummary());
    ss.setFileName("name");
    assertEquals("name", ss.getFileName());

    ss.setSkillSheet(null);
    assertEquals("", ss.getFileContent());
    assertEquals("", ss.getFileContentSummary());
    assertNull(ss.getFileName());
  }

  @Test
  void testUpdateByPk() throws SQLException {
    assertFalse(new SES_AI_T_SKILLSHEET().updateByPk(null));
  }
}
