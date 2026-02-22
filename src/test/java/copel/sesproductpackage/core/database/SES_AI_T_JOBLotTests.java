package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SES_AI_T_JOBLotTests {

  @Test
  void testJOB() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_T_JOB job = new SES_AI_T_JOB();
    job.setRegisterDate(new OriginalDateTime());
    job.setTtl(new OriginalDateTime());

    assertEquals(1, job.insert(connection));
    job.setJobId("J1");
    assertTrue(job.updateByPk(connection));
    job.selectByPk(connection);
    assertTrue(job.deleteByPk(connection));

    assertEquals("案件ID：J1内容：null", job.to案件選出用文章());
    assertNotNull(job.toString());
  }

  @Test
  void testJOBLot() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    // Ensure rs.next() returns true for search calls
    when(rs.next())
        .thenReturn(
            true, false, // selectAll
            true, false, // retrieve(connection, null, 10)
            true, false, // retrieve(connection, vector, 10)
            true, false, // searchByRawContent
            true, false, // selectByAndQuery(multiple)
            true, false, // selectByAndQuery(empty)
            true, false, // selectByOrQuery(multiple)
            true, false, // selectByOrQuery(empty)
            true, false, // searchByRawContent(ops)
            true, false, // searchByRawContent(null list)
            true, false // searchByRawContent(empty list)
            );
    when(rs.getString(anyString())).thenReturn("J1");
    when(rs.getDouble("distance")).thenReturn(0.5);

    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.selectAll(connection);
    assertEquals(1, lot.size());

    // retrieve null connection
    lot.retrieve(null, null, 10);

    // retrieve null query vector (hits ternary)
    lot.retrieve(connection, null, 10);

    Vector vector = new Vector(null);
    java.lang.reflect.Field valueField = Vector.class.getDeclaredField("value");
    valueField.setAccessible(true);
    valueField.set(vector, new float[] {0.1f});

    lot.retrieve(connection, vector, 10);
    assertEquals(1, lot.size());

    // searchByRawContent
    lot.searchByRawContent(connection, "q");
    assertEquals(1, lot.size());

    // selectByAndQuery with multiple params + empty case
    lot.selectByAndQuery(connection, Map.of("c1", "v1", "c2", "v2"));
    lot.selectByAndQuery(connection, Map.of()); // tests isFirst
    assertEquals(1, lot.size());

    // selectByOrQuery with multiple params + empty case
    lot.selectByOrQuery(connection, Map.of("c1", "v1", "c2", "v2"));
    lot.selectByOrQuery(connection, Map.of()); // tests isFirst
    assertEquals(1, lot.size());

    // searchByRawContent with operators (include null operator)
    List<LogicalOperators> ops = new ArrayList<>();
    ops.add(new LogicalOperators(LogicalOperators.論理演算子.AND, "q2"));
    ops.add(null);
    lot.searchByRawContent(connection, "q1", ops);
    assertEquals(1, lot.size());

    // searchByRawContent with null operator list
    try {
      lot.searchByRawContent(connection, "q1", null);
    } catch (Exception e) {
    }

    // searchByRawContent with empty list
    lot.searchByRawContent(connection, "q1", new ArrayList<>());

    // getEntityByPk edge cases
    assertNull(lot.getEntityByPk(null));
    assertNull(lot.getEntityByPk("MISSING"));

    // Fresh lot for manual check to avoid collisions with mock search results
    lot = new SES_AI_T_JOBLot();
    SES_AI_T_JOB job = new SES_AI_T_JOB();
    job.setJobId("J1");
    job.setFromGroup("J1");
    job.setFromId("J1");
    job.setFromName("J1");
    job.setRawContent("J1");
    job.setContentSummary("J1");
    job.setRegisterUser("J1");
    lot.add(job);
    assertEquals(job, lot.getEntityByPk("J1"));
    assertNotNull(lot.to案件選出用文章());
  }
}
