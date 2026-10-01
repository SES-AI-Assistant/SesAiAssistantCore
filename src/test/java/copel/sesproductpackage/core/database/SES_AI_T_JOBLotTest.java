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
import org.mockito.ArgumentCaptor;

import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.JobDetailFilterCondition;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.LogicalOperators.論理演算子;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;

class SES_AI_T_JOBLotTest {

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
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.selectAll(mockConn, "test-tenant");
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectAllWithoutTenantFilter() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.selectAllWithoutTenantFilter(mockConn);
    assertEquals(1, lot.size());
  }

  @Test
  void testRetrieve() throws Exception {
    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.retrieve(mockConn, "test-tenant", createTestVector(), 10);
    assertEquals(1, lot.size());
    assertEquals(0.5, lot.get(0).getDistance());

    SES_AI_T_JOBLot lotForNull = new SES_AI_T_JOBLot();
    lotForNull.retrieve(null, "test-tenant", null, 0);
    assertTrue(lotForNull.isEmpty());
  }

  @Test
  void testSearchByRawContentSingle() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.searchByRawContent(mockConn, "test-tenant", "query");
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
    lot.searchByRawContent(mockConn, "test-tenant", "first", queries);
    assertEquals(1, lot.size());

    setupDefaultResultSet();
    SES_AI_T_JOBLot lot2 = new SES_AI_T_JOBLot();
    lot2.searchByRawContent(mockConn, "test-tenant", "first", null);
    assertEquals(1, lot2.size());

    lot2.selectAll(null, "test-tenant");
    assertEquals(0, lot2.size());
  }

  @Test
  void testSelectByAndQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Map<String, Object> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByAndQuery(mockConn, "test-tenant", query);
    assertEquals(1, lot.size());

    setupDefaultResultSet();
    SES_AI_T_JOBLot lot2 = new SES_AI_T_JOBLot();
    lot2.selectByAndQuery(mockConn, "test-tenant", Collections.emptyMap());
    assertEquals(0, lot2.size());
  }

  @Test
  void testSelectByOrQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Map<String, Object> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByOrQuery(mockConn, "test-tenant", query);
    assertEquals(1, lot.size());

    setupDefaultResultSet();
    SES_AI_T_JOBLot lot2 = new SES_AI_T_JOBLot();
    lot2.selectByOrQuery(mockConn, "test-tenant", Collections.emptyMap());
    assertEquals(0, lot2.size());
  }

  @Test
  void testGetEntityByPk() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.selectAll(mockConn, "test-tenant");

    assertNotNull(lot.getEntityByPk("jid1"));
    assertNull(lot.getEntityByPk("nonexistent"));
    assertNull(lot.getEntityByPk(null));
  }

  @Test
  void testToJobSelectionText() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.selectAll(mockConn, "test-tenant");

    assertFalse(lot.to案件選出用文章().isEmpty());

    SES_AI_T_JOBLot emptyLot = new SES_AI_T_JOBLot();
    assertTrue(emptyLot.to案件選出用文章().isEmpty());
  }

  @Test
  void testRetrieveWithThresholdOverloads() throws Exception {
    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    lot.retrieve(mockConn, "test-tenant", createTestVector());
    assertEquals(1, lot.size());
    assertEquals(0.5, lot.get(0).getDistance());

    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    lot.retrieveWithThreshold(mockConn, "test-tenant", createTestVector(), 0.8, 10);
    assertEquals(1, lot.size());

    SES_AI_T_JOBLot emptyLot2 = new SES_AI_T_JOBLot();
    emptyLot2.retrievePagedWithThreshold(null, "test-tenant", createTestVector(), 0.8, 1, 10);
    assertTrue(emptyLot2.isEmpty());
    emptyLot2.retrievePagedWithThreshold(mockConn, "test-tenant", null, 0.8, 1, 10);
    assertTrue(emptyLot2.isEmpty());

    when(mockRs.next()).thenReturn(false);
    when(mockRs.getLong(1)).thenReturn(0L);
    emptyLot2.retrievePagedWithThreshold(mockConn, "test-tenant", createTestVector(), 0.8, 1, 10);
    assertTrue(emptyLot2.isEmpty());
  }

  @Test
  void testSearchByRawContentPagedFulltextConditions() throws SQLException {
    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    List<FulltextCondition> conds = new ArrayList<>();
    conds.add(new FulltextCondition("AND", "java", false));
    conds.add(new FulltextCondition("OR", "aws", false));
    lot.searchByRawContentPaged(mockConn, "test-tenant", conds, 1, 5);
    assertEquals(1, lot.size());

    SES_AI_T_JOBLot empty = new SES_AI_T_JOBLot();
    empty.searchByRawContentPaged(null, "test-tenant", conds, 1, 5);
    assertTrue(empty.isEmpty());
  }

  @Test
  void testRetrieveWithFilter2Values() throws Exception {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Vector testVector = createTestVector();
    Money price = new Money(new java.math.BigDecimal("100.00"));
    OriginalDateTime startDate = new OriginalDateTime("2023-12-31 23:59:59");

    lot.retrieveWithFilter(mockConn, "test-tenant", testVector, price, startDate, 0.5, 5);
    assertEquals(1, lot.size());

    SES_AI_T_JOBLot empty = new SES_AI_T_JOBLot();
    empty.retrieveWithFilter(null, "test-tenant", testVector, price, startDate, 0.5, 5);
    assertTrue(empty.isEmpty());
  }

  @Test
  void testRetrieveWithFilter4Values() throws Exception {
    setupDefaultResultSet();
    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Vector testVector = createTestVector();
    Money price = new Money(new java.math.BigDecimal("100.00"));
    OriginalDateTime startDate = new OriginalDateTime("2023-12-31 23:59:59");

    lot.retrieveWithFilter(
        mockConn, "test-tenant", testVector, price, startDate, 3, Area.関東_首都圏, 0.5, 5);
    assertEquals(1, lot.size());

    SES_AI_T_JOBLot empty = new SES_AI_T_JOBLot();
    empty.retrieveWithFilter(null, "test-tenant", testVector, price, startDate, 3, Area.関東_首都圏, 0.5, 5);
    assertTrue(empty.isEmpty());
  }

  @Test
  void testSearchByJobWithDetailFilter() throws SQLException {
    // 全文検索条件
    FulltextCondition condition1 = new FulltextCondition("AND", "Java", false);
    FulltextCondition condition2 = new FulltextCondition("AND", "Spring Boot", false);

    // 詳細フィルター条件を設定
    JobDetailFilterCondition detailFilter = new JobDetailFilterCondition();
    detailFilter.setMinPrice(new Money(new java.math.BigDecimal("50.00")));
    detailFilter.setMaxPrice(new Money(new java.math.BigDecimal("150.00")));
    detailFilter.setStartDate(new OriginalDateTime("2023-01-01 00:00:00"));
    detailFilter.setArea(Area.関東_首都圏);
    detailFilter.setOfficeRequirements(3);

    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

    // 詳細フィルター付き全文検索
    assertDoesNotThrow(
        () ->
            lot.searchByJobWithDetailFilter(
                mockConn,
                "test-tenant",
                List.of(condition1, condition2),
                detailFilter,
                1,
                10));
  }

  @Test
  void testSearchByJobWithDetailFilterNullFilter() throws SQLException {
    // 全文検索条件
    FulltextCondition condition = new FulltextCondition("OR", "Java", false);

    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

    // nullフィルターでも例外が発生しないことを確認
    assertDoesNotThrow(
        () ->
            lot.searchByJobWithDetailFilter(
                mockConn,
                "test-tenant",
                List.of(condition),
                null,
                1,
                10));
  }

  @Test
  void testRetrieveByJobVectorWithDetailFilter() throws Exception {
    setupDefaultResultSet();

    // 詳細フィルター条件を設定
    JobDetailFilterCondition detailFilter = new JobDetailFilterCondition();
    detailFilter.setMinPrice(new Money(new java.math.BigDecimal("50.00")));
    detailFilter.setMaxPrice(new Money(new java.math.BigDecimal("150.00")));
    detailFilter.setStartDate(new OriginalDateTime("2023-01-01 00:00:00"));

    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Vector testVector = createTestVector();

    // 詳細フィルター付きベクトル検索
    assertDoesNotThrow(
        () ->
            lot.retrieveByJobVectorWithDetailFilter(
                mockConn,
                "test-tenant",
                testVector,
                0.5,
                detailFilter,
                1,
                10));
  }

  @Test
  void testRetrieveByJobVectorWithDetailFilterNullFilter() throws Exception {
    setupDefaultResultSet();

    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Vector testVector = createTestVector();

    // nullフィルターでも例外が発生しないことを確認
    assertDoesNotThrow(
        () ->
            lot.retrieveByJobVectorWithDetailFilter(
                mockConn,
                "test-tenant",
                testVector,
                0.5,
                null,
                1,
                10));
  }

  @Test
  void testRetrieveByJobVectorWithDetailFilterNullFilterBindsTenantIdBeforeLimitOffset()
      throws Exception {
    // tenant_id は addTenantIdFilter により LIMIT/OFFSET より前（4番目）に挿入されるため、
    // 同じ位置でバインドされている必要がある（過去にLIMIT/OFFSETがtenant_idの位置に
    // バインドされ、character varying = integer の型不一致エラーが発生した不具合の再発防止）
    PreparedStatement mockCountStmt = mock(PreparedStatement.class);
    PreparedStatement mockDataStmt = mock(PreparedStatement.class);
    ResultSet mockCountRs = mock(ResultSet.class);
    ResultSet mockDataRs = mock(ResultSet.class);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    when(mockConn.prepareStatement(sqlCaptor.capture()))
        .thenReturn(mockCountStmt)
        .thenReturn(mockDataStmt);
    when(mockCountStmt.executeQuery()).thenReturn(mockCountRs);
    when(mockCountRs.next()).thenReturn(true);
    when(mockCountRs.getLong(1)).thenReturn(1L);
    when(mockDataStmt.executeQuery()).thenReturn(mockDataRs);
    when(mockDataRs.next()).thenReturn(true, false);
    when(mockDataRs.getString("job_id")).thenReturn("jid1");
    when(mockDataRs.getString("tenant_id")).thenReturn("test-tenant");
    when(mockDataRs.getDouble("distance")).thenReturn(0.5);

    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Vector testVector = createTestVector();

    lot.retrieveByJobVectorWithDetailFilter(mockConn, "test-tenant", testVector, 0.5, null, 1, 10);

    assertEquals(1, lot.size());

    List<String> capturedSqls = sqlCaptor.getAllValues();
    String pagedSql = capturedSqls.get(1);
    assertTrue(pagedSql.contains("ORDER BY distance ASC LIMIT ?"));
    assertTrue(pagedSql.contains("OFFSET ?"));

    verify(mockDataStmt).setString(4, "test-tenant");
    verify(mockDataStmt).setInt(5, 10);
    verify(mockDataStmt).setInt(6, 0);
  }

  @Test
  void testSearchByJobWithDetailFilterWhereClauseHasNoTableAlias() throws SQLException {
    // 全文検索条件
    FulltextCondition condition = new FulltextCondition("AND", "Java", false);

    // 詳細フィルター条件を設定（SELECT_RAW_CONTENT_FOR_FULLTEXTのFROM句にはエイリアスが無いため、
    // WHERE句に "j." エイリアスが付与されると「missing FROM-clause entry」エラーとなる回帰を検出する）
    JobDetailFilterCondition detailFilter = new JobDetailFilterCondition();
    detailFilter.setMinPrice(new Money(new java.math.BigDecimal("50.00")));
    detailFilter.setMaxPrice(new Money(new java.math.BigDecimal("150.00")));

    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();

    lot.searchByJobWithDetailFilter(mockConn, "test-tenant", List.of(condition), detailFilter, 1, 10);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockConn, atLeastOnce()).prepareStatement(sqlCaptor.capture());
    for (String executedSql : sqlCaptor.getAllValues()) {
      assertFalse(executedSql.contains("j.unit_price"), "WHERE句にテーブルエイリアス j が付与されていないこと: " + executedSql);
    }
  }

  @Test
  void testRetrieveByJobVectorWithDetailFilterWhereClauseHasNoTableAlias() throws Exception {
    setupDefaultResultSet();

    // ベクトル検索SQLのFROM句にはエイリアスが無いため、WHERE句に "j." エイリアスが付与されると
    // 「missing FROM-clause entry」エラーとなる回帰を検出する
    JobDetailFilterCondition detailFilter = new JobDetailFilterCondition();
    detailFilter.setMinPrice(new Money(new java.math.BigDecimal("50.00")));
    detailFilter.setStartDate(new OriginalDateTime("2023-01-01 00:00:00"));

    SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
    Vector testVector = createTestVector();

    lot.retrieveByJobVectorWithDetailFilter(mockConn, "test-tenant", testVector, 0.5, detailFilter, 1, 10);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockConn, atLeastOnce()).prepareStatement(sqlCaptor.capture());
    for (String executedSql : sqlCaptor.getAllValues()) {
      assertFalse(executedSql.contains("j.unit_price"), "WHERE句にテーブルエイリアス j が付与されていないこと: " + executedSql);
      assertFalse(executedSql.contains("j.start_date"), "WHERE句にテーブルエイリアス j が付与されていないこと: " + executedSql);
    }
  }
}
