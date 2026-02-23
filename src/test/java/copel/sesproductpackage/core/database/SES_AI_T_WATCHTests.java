package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    watch.setUserId("u"); watch.setTargetId("t");
    watch.setTargetType(SES_AI_T_WATCH.TargetType.JOB);
    watch.insert(conn);
    watch.setTtl(new OriginalDateTime());
    watch.insert(conn);

    // isExist branches
    assertTrue(watch.isExist(conn));
    when(rs.next()).thenReturn(false);
    assertFalse(watch.isExist(conn));

    // isExistByTargetId branches
    when(rs.next()).thenReturn(true);
    assertTrue(watch.isExistByTargetId(conn));
    when(rs.next()).thenReturn(false);
    assertFalse(watch.isExistByTargetId(conn));

    // selectByPk branches
    watch.selectByPk(null);
    watch.setUserId(null); watch.selectByPk(conn);
    watch.setUserId("u"); watch.setTargetId(null); watch.selectByPk(conn);
    watch.setTargetId("t");
    when(rs.next()).thenReturn(true);
    watch.selectByPk(conn);

    // updateByPk branches
    watch.updateByPk(null);
    watch.setUserId(null); watch.updateByPk(conn);
    watch.setUserId("u"); watch.setTargetId(null); watch.updateByPk(conn);
    watch.setTargetId("t");
    watch.updateByPk(conn);
    watch.setRegisterDate(new OriginalDateTime());
    watch.updateByPk(conn);

    // deleteByPk branches
    watch.deleteByPk(null);
    watch.setUserId(null); watch.deleteByPk(conn);
    watch.setUserId("u"); watch.setTargetId(null); watch.deleteByPk(conn);
    watch.setTargetId("t");
    watch.deleteByPk(conn);

    // Enum branches
    assertEquals(SES_AI_T_WATCH.TargetType.JOB, SES_AI_T_WATCH.TargetType.getEnumByName("JOB"));
    assertNull(SES_AI_T_WATCH.TargetType.getEnumByName("INVALID"));
  }
}
