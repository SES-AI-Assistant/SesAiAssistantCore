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

class SES_AI_T_SKILLSHEETLotTest {

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
    when(mockRs.getString("tenant_id")).thenReturn("test-tenant");
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
    lot.selectAll(mockConn, "test-tenant");
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectAllWithNullConnection() throws SQLException {
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectAll(null, "test-tenant");
    assertTrue(lot.isEmpty());
  }

  @Test
  void testSelectAllWithoutTenantFilter() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectAllWithoutTenantFilter(mockConn);
    assertEquals(1, lot.size());
  }

  @Test
  void testGetEntityByPk() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectAll(mockConn, "test-tenant");

    assertNotNull(lot.getEntityByPk("id1"));
    assertNull(lot.getEntityByPk("nonexistent"));
    assertNull(lot.getEntityByPk(null));
  }

  @Test
  void testRetrieve() throws Exception {
    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.retrieve(mockConn, "test-tenant", createTestVector(), 10);
    assertEquals(1, lot.size());
    assertEquals(0.5, lot.get(0).getDistance());

    SES_AI_T_SKILLSHEETLot lotForNull = new SES_AI_T_SKILLSHEETLot();
    lotForNull.retrieve(null, "test-tenant", null, 0);
    assertTrue(lotForNull.isEmpty());

    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    SES_AI_T_SKILLSHEETLot lot2 = new SES_AI_T_SKILLSHEETLot();
    lot2.retrieve(mockConn, "test-tenant", createTestVector(), 1);
    assertEquals(1, lot2.size());
  }

  @Test
  void testSelectLike() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectLike(mockConn, "test-tenant", "file_name", "query");
    assertEquals(1, lot.size());
  }

  @Test
  void testSearchByFileContent() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.searchByFileContent(mockConn, "test-tenant", "query");
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByFileName() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectByFileName(mockConn, "test-tenant", "file1.pdf");
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
    lot.searchByFileContent(mockConn, "test-tenant", "first", queries);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByAndQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    Map<String, Object> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByAndQuery(mockConn, "test-tenant", query);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByOrQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    Map<String, Object> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByOrQuery(mockConn, "test-tenant", query);
    assertEquals(1, lot.size());
  }

  @Test
  void testToSkillSheetSelectionText() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.selectAll(mockConn, "test-tenant");

    assertFalse(lot.toスキルシート選出用文章().isEmpty());

    SES_AI_T_SKILLSHEETLot emptyLot = new SES_AI_T_SKILLSHEETLot();
    assertTrue(emptyLot.toスキルシート選出用文章().isEmpty());
  }

  @Test
  void testRetrieveWithThresholdOverloads() throws Exception {
    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    SES_AI_T_SKILLSHEETLot lot = new SES_AI_T_SKILLSHEETLot();
    lot.retrieve(mockConn, "test-tenant", createTestVector());
    assertEquals(1, lot.size());
    assertEquals(0.5, lot.get(0).getDistance());

    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    lot.retrieveWithThreshold(mockConn, "test-tenant", createTestVector(), 0.8, 10);
    assertEquals(1, lot.size());

    SES_AI_T_SKILLSHEETLot emptyLot2 = new SES_AI_T_SKILLSHEETLot();
    emptyLot2.retrievePagedWithThreshold(null, "test-tenant", createTestVector(), 0.8, 1, 10);
    assertTrue(emptyLot2.isEmpty());
    emptyLot2.retrievePagedWithThreshold(mockConn, "test-tenant", null, 0.8, 1, 10);
    assertTrue(emptyLot2.isEmpty());

    when(mockRs.next()).thenReturn(false);
    when(mockRs.getLong(1)).thenReturn(0L);
    emptyLot2.retrievePagedWithThreshold(mockConn, "test-tenant", createTestVector(), 0.8, 1, 10);
    assertTrue(emptyLot2.isEmpty());
  }
}

