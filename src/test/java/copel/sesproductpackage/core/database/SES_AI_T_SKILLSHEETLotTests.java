package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.LogicalOperators.論理演算子;
import copel.sesproductpackage.core.unit.Vector;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SES_AI_T_SKILLSHEETLotTests {

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
    when(mockRs.getString("file_id")).thenReturn("id1");
    when(mockRs.getString("file_name")).thenReturn("file1.pdf");
    when(mockRs.getString("file_content")).thenReturn("content1");
    when(mockRs.getString("file_content_summary")).thenReturn("summary1");
    when(mockRs.getString("from_group")).thenReturn("fg");
    when(mockRs.getString("from_id")).thenReturn("fi");
    when(mockRs.getString("from_name")).thenReturn("fn");
    when(mockRs.getString("register_date")).thenReturn("2023-01-01 12:00:00");
    when(mockRs.getString("register_user")).thenReturn("u");
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
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectAll(mockConn);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectAllWithNullConnection() throws SQLException {
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectAll(null);
    assertTrue(lot.isEmpty());
  }

  @Test
  void testGetEntityByPk() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectAll(mockConn);

    assertNotNull(lot.getEntityByPk("id1"));
    assertNull(lot.getEntityByPk("nonexistent"));
    assertNull(lot.getEntityByPk(null));
  }

  @Test
  void testRetrieve() throws Exception {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.retrieve(mockConn, createTestVector(), 10);
    assertEquals(1, lot.size());
    assertEquals(0.5, lot.get(0).getDistance());

    SES_AI_T_SKILLSHEETLot lotForNull = new SES_AI_T_SKILLSHEETLot();
    lotForNull.retrieve(null, null, 0);
    assertTrue(lotForNull.isEmpty());

    lot.retrieve(mockConn, null, 1);
    verify(mockStmt).setString(1, null);
  }

  @Test
  void testSelectLike() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectLike(mockConn, "file_name", "query");
    assertEquals(1, lot.size());
  }

  @Test
  void testSearchByFileContent() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.searchByFileContent(mockConn, "query");
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByFileName() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectByFileName(mockConn, "file1.pdf");
    assertEquals(1, lot.size());
  }

  @Test
  void testSearchByFileContentMultiple() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    List<LogicalOperators> queries = new ArrayList<>();
    queries.add(new LogicalOperators(論理演算子.AND, "val1"));
    queries.add(null);
    queries.add(new LogicalOperators(論理演算子.OR, "val2"));
    lot.searchByFileContent(mockConn, "first", queries);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByAndQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    Map<String, String> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByAndQuery(mockConn, query);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByOrQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    Map<String, String> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByOrQuery(mockConn, query);
    assertEquals(1, lot.size());
  }

  @Test
  void testTo文章() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectAll(mockConn);

    assertFalse(lot.toスキルシート選出用文章().isEmpty());

    SES_AI_T_SKILLSHEETLot emptyLot = new SES_AI_T_SKILLSHEETLot();
    assertTrue(emptyLot.toスキルシート選出用文章().isEmpty());
  }

  @Test
  void testBoundaryCases() throws SQLException {
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    // EntityLotBase test (get index < 0)
    assertNull(lot.get(-1));
    assertNull(lot.get(100));
    assertTrue(lot.isEmpty());
    assertEquals(0, lot.size());

    // SES_AI_T_SKILLSHEETLot method coverage (empty maps)
    Map<String, String> emptyMap = new java.util.HashMap<>();
    lot.selectByAndQuery(mockConn, emptyMap);
    lot.selectByOrQuery(mockConn, emptyMap);

    lot.add(new SES_AI_T_SKILLSHEET());
    assertNotNull(lot.get(0));
    assertFalse(lot.isEmpty());
    assertEquals(1, lot.size());

    lot.sort();
    assertNotNull(lot.toString());
  }
}
