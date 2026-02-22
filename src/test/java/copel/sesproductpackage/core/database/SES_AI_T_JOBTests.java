package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_T_JOBTests {

  @Test
  void testJOBMethods() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate())
        .thenReturn(1, 0, 1, 0, 1); // For update/delete success/fail cases and final insert
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getInt(1)).thenReturn(0);

    SES_AI_T_JOB job = new SES_AI_T_JOB();
    job.setJobId("J1");

    // uniqueCheck
    when(rs.getInt(1)).thenReturn(0);
    assertTrue(job.uniqueCheck(connection, 0.8));
    when(rs.getInt(1)).thenReturn(1);
    assertFalse(job.uniqueCheck(connection, 0.8));

    // selectByPk hits
    when(rs.next()).thenReturn(true);
    job.selectByPk(connection);
    when(rs.next()).thenReturn(false);
    job.selectByPk(connection);

    // updateByPk
    assertTrue(job.updateByPk(connection));
    assertFalse(job.updateByPk(connection)); // returns false because executeUpdate() == 0

    // deleteByPk
    assertTrue(job.deleteByPk(connection));
    assertFalse(job.deleteByPk(connection)); // returns false because executeUpdate() == 0

    // embedding
    Transformer mockTrans = mock(Transformer.class);
    when(mockTrans.embedding(anyString())).thenReturn(new float[] {0.1f});
    job.setContentSummary("summary");
    job.embedding(mockTrans);
    assertNotNull(job.getVectorData());

    // registerDate & ttl == null branches
    job.setRegisterDate(null);
    job.setTtl(null);
    job.insert(connection);
  }

  @Test
  void testJOB() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate())
        .thenReturn(1, 1, 1, 1, 1); // For all successful DB operations in this test
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString(anyString())).thenReturn("test");

    SES_AI_T_JOB job = new SES_AI_T_JOB();
    job.setJobId("J1");
    job.setRegisterDate(new OriginalDateTime());

    assertEquals(1, job.insert(connection));
    assertNotNull(job.to案件選出用文章());
    assertNotNull(job.toString());

    // Null checks
    assertEquals(0, job.insert(null));
    job.selectByPk(null);
    job.setJobId(null);
    job.selectByPk(connection); // jobId is null

    // updateByPk return values
    job.setJobId("J1");
    assertTrue(job.updateByPk(connection));
    assertFalse(job.updateByPk(null));
    job.setJobId(null);
    assertFalse(job.updateByPk(connection));

    // deleteByPk return values
    job.setJobId("J1");
    assertTrue(job.deleteByPk(connection));
    assertFalse(job.deleteByPk(null));

    // vectorData populated paths
    copel.sesproductpackage.core.unit.Vector vec =
        new copel.sesproductpackage.core.unit.Vector(mock(Transformer.class));
    try {
      java.lang.reflect.Field embField =
          copel.sesproductpackage.core.unit.Vector.class.getDeclaredField("value");
      embField.setAccessible(true);
      embField.set(vec, new float[] {0.1f});
    } catch (Exception e) {
    }
    job.setVectorData(vec);
    job.setTtl(new OriginalDateTime());
    assertEquals(1, job.insert(connection));
    assertTrue(job.updateByPk(connection));
  }

  @Test
  void testLombokCoverage() throws Exception {
    SES_AI_T_JOB obj1 = new SES_AI_T_JOB();
    SES_AI_T_JOB obj2 = new SES_AI_T_JOB();
    SES_AI_T_JOB diff = new SES_AI_T_JOB();
    diff.setJobId("test_id");
    diff.setRawContent("test");
    copel.sesproductpackage.core.util.EntityCoverageHelper.checkLombokCoverage(
        SES_AI_T_JOB.class, obj1, obj2, diff);
  }
}
