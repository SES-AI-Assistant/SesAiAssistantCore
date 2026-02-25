package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.LogicalOperators.論理演算子;
import copel.sesproductpackage.core.unit.Vector;

class SES_AI_T_JOBLotTests {

  private Connection mockConn;
  private PreparedStatement mockStmt;
  private ResultSet mockRs;

  @BeforeEach
  void setUp() throws SQLException {
    mockConn = mock(Connection.class);
    mockStmt = mock(PreparedStatement.class);
    mockRs = mock(ResultSet.class);

    when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
    when(mockStmt.executeQuery()).thenReturn(mockRs);
  }

  private void setupDefaultResultSet() throws SQLException {
    when(mockRs.next()).thenReturn(true).thenReturn(false);
    when(mockRs.getString("job_id")).thenReturn("jid1");
    when(mockRs.getString("from_group")).thenReturn("fg1");
    when(mockRs.getString("from_id")).thenReturn("fid1");
    when(mockRs.getString("from_name")).thenReturn("fname1");
    when(mockRs.getString("raw_content")).thenReturn("raw1");
    when(mockRs.getString("content_summary")).thenReturn("summary1");
    when(mockRs.getString("register_date")).thenReturn("2023-01-01 12:00:00");
    when(mockRs.getString("register_user")).thenReturn("user1");
    when(mockRs.getString("ttl")).thenReturn("2024-01-01 12:00:00");
    when(mockRs.getDouble("distance")).thenReturn(0.5);
  }

  private Vector createTestVector() throws Exception {
    Vector vector = new Vector(null);
    Field valueField = Vector.class.getDeclaredField("value");
    valueField.setAccessible(true);
    valueField.set(vector, new float[] {1.0f, 2.0f});
    return vector;
  }

  @Test
  void testSelectAll() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.selectAll(mockConn);
    assertEquals(1, lot.size());
  }

  @Test
  void testRetrieve() throws Exception {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.retrieve(mockConn, createTestVector(), 10);
    assertEquals(1, lot.size());
    assertEquals(0.5, lot.get(0).getDistance());

    // Null connection
    SES_AI_T_JOBLot lotForNull = new SES_AI_T_JOBLot();
    lotForNull.retrieve(null, null, 0);
    assertTrue(lotForNull.isEmpty());

    // Null query
    lot.retrieve(mockConn, null, 1);
    verify(mockStmt).setString(1, null);
  }

  @Test
  void testSearchByRawContentSingle() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.searchByRawContent(mockConn, "query");
    assertEquals(1, lot.size());
  }

  @Test
  void testSearchByRawContentMultiple() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    List<LogicalOperators> queries = new ArrayList<>();
    queries.add(new LogicalOperators(論理演算子.AND, "val1"));
    queries.add(new LogicalOperators(論理演算子.OR, "val2"));
    queries.add(null);
    lot.searchByRawContent(mockConn, "first", queries);
    assertEquals(1, lot.size());

    // Test with null list
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot2 = new SES_AI_T_JOBLot();
    lot2.searchByRawContent(mockConn, "first", null);
    assertEquals(1, lot2.size());

    // Test selectAll null connection
    lot2.selectAll(null);
    assertEquals(0, lot2.size());
  }

  @Test
  void testSelectByAndQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Map<String, String> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByAndQuery(mockConn, query);
    assertEquals(1, lot.size());

    setupDefaultResultSet();
    SES_AI_T_JOBLot lot2 = new SES_AI_T_JOBLot();
    lot2.selectByAndQuery(mockConn, Collections.emptyMap());
    assertEquals(0, lot2.size());
  }

  @Test
  void testSelectByOrQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Map<String, String> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByOrQuery(mockConn, query);
    assertEquals(1, lot.size());

    setupDefaultResultSet();
    SES_AI_T_JOBLot lot2 = new SES_AI_T_JOBLot();
    lot2.selectByOrQuery(mockConn, Collections.emptyMap());
    assertEquals(0, lot2.size());
  }

  @Test
  void testGetEntityByPk() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.selectAll(mockConn);

    assertNotNull(lot.getEntityByPk("jid1"));
    assertNull(lot.getEntityByPk("nonexistent"));
    assertNull(lot.getEntityByPk(null));
  }

  @Test
  void testTo文章() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.selectAll(mockConn);

    assertFalse(lot.to案件選出用文章().isEmpty());

    // Empty lot
    SES_AI_T_JOBLot emptyLot = new SES_AI_T_JOBLot();
    assertTrue(emptyLot.to案件選出用文章().isEmpty());
  }
}
