package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.PersonDetailFilterCondition;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.LogicalOperators.論理演算子;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SES_AI_T_SKILLSHEET_PERSONLotTest {

  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;
  private ResultSetMetaData mockResultSetMetaData;
  private Vector mockVector;

  @BeforeEach
  void setUp() throws SQLException {
    mockConnection = mock(Connection.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);
    mockResultSetMetaData = mock(ResultSetMetaData.class);
    mockVector = mock(Vector.class);

    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenReturn(mockResultSetMetaData);

    when(mockVector.toString()).thenReturn("[0.1, 0.2]");
  }

  private void prepareMockResultSet(boolean hasDistance) throws SQLException {
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("file_id")).thenReturn("f1");
    when(mockResultSet.getString("person_id")).thenReturn("p1");
    when(mockResultSet.getString("tenant_id")).thenReturn("test-tenant");

    if (hasDistance) {
      when(mockResultSetMetaData.getColumnCount()).thenReturn(2);
      when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("file_id");
      when(mockResultSetMetaData.getColumnLabel(2)).thenReturn("distance");
      when(mockResultSet.getDouble("distance")).thenReturn(0.85);
    } else {
      when(mockResultSetMetaData.getColumnCount()).thenReturn(1);
      when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("file_id");
    }
  }

  @Test
  void testRetrieveByPersonVector() throws SQLException {
    prepareMockResultSet(true);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonVector(mockConnection, "test-tenant", mockVector, 0.5, 10);
    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());
    assertEquals(0.85, lot.get(0).getDistance());
  }

  @Test
  void testRetrieveBySkillSheetVector() throws SQLException {
    prepareMockResultSet(true);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveBySkillSheetVector(mockConnection, "test-tenant", mockVector, 0.6, 5);
    assertEquals(1, lot.size());
  }

  @Test
  void testRetrieveOuterJoinVectors() throws SQLException {
    prepareMockResultSet(true);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveOuterJoinByPersonVector(mockConnection, "test-tenant", mockVector, 0.5, 10);
    assertEquals(1, lot.size());

    when(mockResultSet.next()).thenReturn(true, false);
    lot.retrieveOuterJoinBySkillSheetVector(mockConnection, "test-tenant", mockVector, 0.5, 10);
    assertEquals(1, lot.size());
  }

  @Test
  void testRetrieveByPersonRawContent() throws SQLException {
    prepareMockResultSet(false);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.searchByPersonRawContent(mockConnection, "test-tenant", "keyword");
    assertEquals(1, lot.size());

    when(mockResultSet.next()).thenReturn(true, false);
    lot.searchByPersonRawContent(
        mockConnection, "test-tenant", "k1", List.of(new LogicalOperators(論理演算子.AND, "k2")));
    assertEquals(1, lot.size());
  }

  @Test
  void testRetrieveBySkillSheetRawContent() throws SQLException {
    prepareMockResultSet(false);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.searchBySkillSheetRawContent(mockConnection, "test-tenant", "skill");
    assertEquals(1, lot.size());

    when(mockResultSet.next()).thenReturn(true, false);
    lot.searchBySkillSheetRawContent(
        mockConnection, "test-tenant", "s1", List.of(new LogicalOperators(論理演算子.OR, "s2")));
    assertEquals(1, lot.size());
  }

  @Test
  void testGetEntityMethods() throws SQLException {
    prepareMockResultSet(false);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.searchByPersonRawContent(mockConnection, "test-tenant", "keyword");

    assertNotNull(lot.getEntityByPk("p1"));
    assertNotNull(lot.getEntityByFileId("f1"));
    assertNull(lot.getEntityByPk("p2"));
    assertNull(lot.getEntityByFileId("f2"));
  }

  @Test
  void testToSelectionTexts() throws SQLException {
    prepareMockResultSet(false);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.searchByPersonRawContent(mockConnection, "test-tenant", "keyword");
    lot.get(0).setContentSummary("cs");
    lot.get(0).setFileContentSummary("fs");

    assertEquals("1人目：要員ID：p1 内容：csfs\n", lot.to要員選出用文章());
    assertEquals("1件目：ファイルID：f1 内容：fscs\n", lot.toスキルシート選出用文章());
  }

  @Test
  void testRetrieveByPersonOrSkillSheetSummaryPaged() throws SQLException {
    // COUNT クエリと取得クエリの両方を処理するため prepareStatement を2回呼ぶ
    PreparedStatement mockCountStmt = mock(PreparedStatement.class);
    PreparedStatement mockDataStmt = mock(PreparedStatement.class);
    ResultSet mockCountRs = mock(ResultSet.class);

    when(mockConnection.prepareStatement(anyString()))
        .thenReturn(mockCountStmt)
        .thenReturn(mockDataStmt);
    when(mockCountStmt.executeQuery()).thenReturn(mockCountRs);
    when(mockCountRs.next()).thenReturn(true);
    when(mockCountRs.getLong(1)).thenReturn(1L);
    when(mockDataStmt.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("file_id")).thenReturn("f1");
    when(mockResultSet.getString("person_id")).thenReturn("p1");
    when(mockResultSet.getString("tenant_id")).thenReturn("test-tenant");
    when(mockResultSetMetaData.getColumnCount()).thenReturn(1);
    when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("file_id");

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    List<FulltextCondition> conditions = List.of(new FulltextCondition("AND", "Java", false));
    lot.searchByPersonOrSkillSheetSummaryPaged(mockConnection, "test-tenant", conditions, 1, 20);
    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());
  }

  @Test
  void testRetrieveByPersonOrSkillSheetSummaryPaged_nullConnection() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    List<FulltextCondition> conditions = List.of(new FulltextCondition("AND", "Java", false));
    assertDoesNotThrow(
        () -> lot.searchByPersonOrSkillSheetSummaryPaged(null, "test-tenant", conditions, 1, 20));
    assertEquals(0, lot.size());
  }

  @Test
  void testRetrieveByPersonOrSkillSheetSummaryInnerJoinPaged() throws SQLException {
    PreparedStatement mockCountStmt = mock(PreparedStatement.class);
    PreparedStatement mockDataStmt = mock(PreparedStatement.class);
    ResultSet mockCountRs = mock(ResultSet.class);

    when(mockConnection.prepareStatement(anyString()))
        .thenReturn(mockCountStmt)
        .thenReturn(mockDataStmt);
    when(mockCountStmt.executeQuery()).thenReturn(mockCountRs);
    when(mockCountRs.next()).thenReturn(true);
    when(mockCountRs.getLong(1)).thenReturn(1L);
    when(mockDataStmt.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("file_id")).thenReturn("f1");
    when(mockResultSet.getString("person_id")).thenReturn("p1");
    when(mockResultSet.getString("tenant_id")).thenReturn("test-tenant");
    when(mockResultSetMetaData.getColumnCount()).thenReturn(1);
    when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("file_id");

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    List<FulltextCondition> conditions = List.of(new FulltextCondition("AND", "TWINPOS", false));
    lot.searchByPersonOrSkillSheetSummaryInnerJoinPaged(
        mockConnection, "test-tenant", conditions, 1, 20);
    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());
  }

  @Test
  void testRetrieveByPersonOrSkillSheetSummaryInnerJoinPaged_nullConnection() {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    List<FulltextCondition> conditions = List.of(new FulltextCondition("AND", "TWINPOS", false));
    assertDoesNotThrow(
        () ->
            lot.searchByPersonOrSkillSheetSummaryInnerJoinPaged(
                null, "test-tenant", conditions, 1, 20));
    assertEquals(0, lot.size());
  }

  @Test
  void testSelectAll() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    assertDoesNotThrow(() -> lot.selectAll(mockConnection, "test-tenant"));
  }

  @Test
  void testRetrieveByPersonVectorWithFilter2Values() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    Money price = new Money(new java.math.BigDecimal("100.00"));
    OriginalDateTime startDate = new OriginalDateTime("2023-01-01 00:00:00");

    assertDoesNotThrow(
        () ->
            lot.retrieveByPersonVectorWithFilter(
                mockConnection, "test-tenant", mockVector, price, startDate, 0.5, 5));
  }

  @Test
  void testRetrieveByPersonVectorWithFilter4Values() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    Money price = new Money(new java.math.BigDecimal("100.00"));
    OriginalDateTime startDate = new OriginalDateTime("2023-01-01 00:00:00");

    assertDoesNotThrow(
        () ->
            lot.retrieveByPersonVectorWithFilter(
                mockConnection,
                "test-tenant",
                mockVector,
                price,
                startDate,
                3,
                Area.関東_首都圏,
                0.5,
                5));
  }

  @Test
  void testSearchByPersonOrSkillSheetSummaryWithDetailFilter() throws SQLException {
    // 全文検索条件
    FulltextCondition condition1 = new FulltextCondition("AND", "Java", false);
    FulltextCondition condition2 = new FulltextCondition("AND", "Spring", false);

    // 詳細フィルター条件を設定
    PersonDetailFilterCondition detailFilter = new PersonDetailFilterCondition();
    detailFilter.setMinPrice(new Money(new java.math.BigDecimal("50.00")));
    detailFilter.setMaxPrice(new Money(new java.math.BigDecimal("150.00")));
    detailFilter.setMinAge(25);
    detailFilter.setMaxAge(60);
    detailFilter.setArea(Area.関東_首都圏);
    detailFilter.setGender("M");

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

    // null connectionでも例外が発生しないことを確認
    assertDoesNotThrow(
        () ->
            lot.searchByPersonOrSkillSheetSummaryWithDetailFilter(
                mockConnection,
                "test-tenant",
                List.of(condition1, condition2),
                detailFilter,
                1,
                10));
  }

  @Test
  void testSearchByPersonOrSkillSheetSummaryWithDetailFilterNullFilter() throws SQLException {
    // 全文検索条件
    FulltextCondition condition = new FulltextCondition("OR", "Java", false);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

    // nullフィルターでも例外が発生しないことを確認
    assertDoesNotThrow(
        () ->
            lot.searchByPersonOrSkillSheetSummaryWithDetailFilter(
                mockConnection, "test-tenant", List.of(condition), null, 1, 10));
  }

  @Test
  void testRetrieveByPersonVectorWithDetailFilter() throws SQLException {
    prepareMockResultSet(true);

    // 詳細フィルター条件を設定
    PersonDetailFilterCondition detailFilter = new PersonDetailFilterCondition();
    detailFilter.setStartDate(new OriginalDateTime("2023-01-01 00:00:00"));
    detailFilter.setMinPrice(new Money(new java.math.BigDecimal("50.00")));
    detailFilter.setMaxAge(65);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

    // 詳細フィルター付きベクトル検索
    assertDoesNotThrow(
        () ->
            lot.retrieveByPersonVectorWithDetailFilter(
                mockConnection, "test-tenant", mockVector, 0.5, detailFilter, 1, 10));
  }

  @Test
  void testRetrieveByPersonVectorWithDetailFilterNullFilter() throws SQLException {
    prepareMockResultSet(true);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

    // nullフィルターでも例外が発生しないことを確認
    assertDoesNotThrow(
        () ->
            lot.retrieveByPersonVectorWithDetailFilter(
                mockConnection, "test-tenant", mockVector, 0.5, null, 1, 10));
  }

  @Test
  void testRetrieveByPersonVectorWithDetailFilterNullFilterGeneratesValidSql() throws SQLException {
    // 詳細フィルター条件が全て未指定（null）の場合でも、COUNT SQL・ページングSQLが
    // 正しい構文（FROM句・LIMIT句を含む）で生成されることを確認する。
    // 過去にCOUNT SQLのFROM句欠落、ページングSQLのLIMIT句欠落によりPostgreSQL構文エラーが
    // 発生した不具合の再発防止テスト。
    PreparedStatement mockCountStmt = mock(PreparedStatement.class);
    PreparedStatement mockDataStmt = mock(PreparedStatement.class);
    ResultSet mockCountRs = mock(ResultSet.class);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    when(mockConnection.prepareStatement(sqlCaptor.capture()))
        .thenReturn(mockCountStmt)
        .thenReturn(mockDataStmt);
    when(mockCountStmt.executeQuery()).thenReturn(mockCountRs);
    when(mockCountRs.next()).thenReturn(true);
    when(mockCountRs.getLong(1)).thenReturn(1L);
    when(mockDataStmt.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("file_id")).thenReturn("f1");
    when(mockResultSet.getString("person_id")).thenReturn("p1");
    when(mockResultSet.getString("tenant_id")).thenReturn("test-tenant");
    when(mockResultSetMetaData.getColumnCount()).thenReturn(2);
    when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("file_id");
    when(mockResultSetMetaData.getColumnLabel(2)).thenReturn("distance");
    when(mockResultSet.getDouble("distance")).thenReturn(0.85);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();

    lot.retrieveByPersonVectorWithDetailFilter(
        mockConnection, "test-tenant", mockVector, 0.5, null, 1, 10);

    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());

    List<String> capturedSqls = sqlCaptor.getAllValues();
    String countSql = capturedSqls.get(0);
    String pagedSql = capturedSqls.get(1);

    assertTrue(countSql.contains("FROM SES_AI_T_SKILLSHEET"));
    assertTrue(pagedSql.contains("ORDER BY distance ASC LIMIT ?"));
    assertTrue(pagedSql.contains("OFFSET ?"));

    // tenant_id は addTenantIdFilter により LIMIT/OFFSET より前（4番目）に挿入されるため、
    // 同じ位置でバインドされている必要がある（過去にLIMIT/OFFSETがtenant_idの位置に
    // バインドされ、character varying = integer の型不一致エラーが発生した不具合の再発防止）
    verify(mockDataStmt).setString(4, "test-tenant");
    verify(mockDataStmt).setInt(5, 10);
    verify(mockDataStmt).setInt(6, 0);
  }
}
