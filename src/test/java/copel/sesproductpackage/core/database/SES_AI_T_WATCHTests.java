package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

class SES_AI_T_WATCHTests {

  @Test
  void testAllBranches() throws Exception {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(ps.executeUpdate()).thenReturn(1);
    when(rs.next()).thenReturn(true, false);
    when(rs.getBoolean(1)).thenReturn(true);
    when(rs.getString(anyString())).thenReturn("JOB", "2024-01-01 00:00:00");

    SES_AI_T_WATCH watch = new SES_AI_T_WATCH();

    // insert branches
    watch.insert(null);
    watch.setUserId("u");
    watch.setTargetId("t");
    watch.setTargetType(SES_AI_T_WATCH.TargetType.JOB);
    when(ps.executeUpdate()).thenReturn(1);
    assertEquals(1, watch.insert(conn));
    when(ps.executeUpdate()).thenReturn(0);
    assertEquals(0, watch.insert(conn));
    watch.setTtl(new OriginalDateTime());
    watch.insert(conn);

    // isExist branches
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getBoolean(1)).thenReturn(true);
    assertTrue(watch.isExist(conn));
    when(rs.next()).thenReturn(true);
    when(rs.getBoolean(1)).thenReturn(false);
    assertFalse(watch.isExist(conn));
    when(rs.next()).thenReturn(false);
    assertFalse(watch.isExist(conn));

    // isExistByTargetId branches
    when(rs.next()).thenReturn(true);
    when(rs.getBoolean(1)).thenReturn(true);
    assertTrue(watch.isExistByTargetId(conn));
    when(rs.next()).thenReturn(false);
    assertFalse(watch.isExistByTargetId(conn));

    // selectByPk branches
    watch.selectByPk(null);
    watch.setUserId(null);
    watch.selectByPk(conn);
    watch.setUserId("u");
    watch.setTargetId(null);
    watch.selectByPk(conn);
    watch.setTargetId("t");
    when(rs.next()).thenReturn(false);
    watch.selectByPk(conn);
    when(rs.next()).thenReturn(true);
    watch.selectByPk(conn);

    // updateByPk branches
    watch.updateByPk(null);
    watch.setUserId(null);
    watch.updateByPk(conn);
    watch.setUserId("u");
    watch.setTargetId(null);
    watch.updateByPk(conn);
    watch.setTargetId("t");
    watch.setTargetType(null);
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(watch.updateByPk(conn));
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(watch.updateByPk(conn));
    watch.setTargetType(SES_AI_T_WATCH.TargetType.PERSON);
    watch.setRegisterDate(new OriginalDateTime());
    watch.updateByPk(conn);

    // deleteByPk branches
    watch.deleteByPk(null);
    watch.setUserId(null);
    watch.deleteByPk(conn);
    watch.setUserId("u");
    watch.setTargetId(null);
    watch.deleteByPk(conn);
    watch.setTargetId("t");
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(watch.deleteByPk(conn));
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(watch.deleteByPk(conn));

    // Enum branches
    assertEquals(SES_AI_T_WATCH.TargetType.JOB, SES_AI_T_WATCH.TargetType.getEnumByName("JOB"));
    assertEquals(
        SES_AI_T_WATCH.TargetType.PERSON, SES_AI_T_WATCH.TargetType.getEnumByName("PERSON"));
    assertEquals(
        SES_AI_T_WATCH.TargetType.SKILLSHEET,
        SES_AI_T_WATCH.TargetType.getEnumByName("SKILLSHEET"));
    assertNull(SES_AI_T_WATCH.TargetType.getEnumByName("INVALID"));

    // Lombok coverage
    SES_AI_T_WATCH w1 = new SES_AI_T_WATCH();
    SES_AI_T_WATCH w2 = new SES_AI_T_WATCH();
    w1.setUserId("u");
    w1.setTargetId("t");
    w2.setUserId("u");
    w2.setTargetId("t");
    assertEquals(w1, w1);
    assertEquals(w1, w2);
    assertEquals(w1.hashCode(), w2.hashCode());
    assertNotNull(w1.toString());
    assertNotEquals(w1, null);
    assertNotEquals(w1, new Object());
  }
}
