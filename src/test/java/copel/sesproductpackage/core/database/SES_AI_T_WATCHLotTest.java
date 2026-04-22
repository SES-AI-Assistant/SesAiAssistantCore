package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SES_AI_T_WATCHLotTest {

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
    when(mockPreparedStatement.executeUpdate()).thenReturn(1);
  }

  @Test
  void testSelectByUserId() throws SQLException {
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("user_id")).thenReturn("u1");
    when(mockResultSet.getString("target_id")).thenReturn("t1");
    when(mockResultSet.getString("target_type")).thenReturn("JOB");

    SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
    lot.selectByUserId(mockConnection, "u1");

    assertEquals(1, lot.size());
    assertEquals("t1", lot.get(0).getTargetId());
    assertTrue(lot.containsById("t1"));
    assertFalse(lot.containsById("t2"));
    assertFalse(lot.containsById(null));
    assertFalse(lot.containsById(""));

    lot.selectByUserId(null, "u1");
    lot.selectByUserId(mockConnection, null);
  }

  @Test
  void testDeleteExpired() throws SQLException {
    SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
    int count = lot.deleteExpired(mockConnection);
    assertEquals(1, count);
    verify(mockPreparedStatement).executeUpdate();
  }

  @Test
  void testSelectAll() throws SQLException {
    when(mockResultSet.next()).thenReturn(true, false);
    SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
    lot.selectAll(mockConnection);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByTargetId() throws SQLException {
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("user_id")).thenReturn("u1");
    when(mockResultSet.getString("target_id")).thenReturn("job001");
    when(mockResultSet.getString("target_type")).thenReturn("JOB");

    SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
    lot.selectByTargetId(mockConnection, "job001", SES_AI_T_WATCH.TargetType.JOB);

    assertEquals(1, lot.size());
    assertEquals("u1", lot.get(0).getUserId());
    assertEquals("job001", lot.get(0).getTargetId());

    lot.selectByTargetId(null, "job001", SES_AI_T_WATCH.TargetType.JOB);
    lot.selectByTargetId(mockConnection, null, SES_AI_T_WATCH.TargetType.JOB);
    lot.selectByTargetId(mockConnection, "job001", null);
  }
}
