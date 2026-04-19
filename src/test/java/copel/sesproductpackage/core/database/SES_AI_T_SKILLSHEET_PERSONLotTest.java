package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.LogicalOperators.論理演算子;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    lot.retrieveByPersonVector(mockConnection, mockVector, 0.5, 10);
    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());
    assertEquals(0.85, lot.get(0).getDistance());
  }

  @Test
  void testRetrieveBySkillSheetVector() throws SQLException {
    prepareMockResultSet(true);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveBySkillSheetVector(mockConnection, mockVector, 0.6, 5);
    assertEquals(1, lot.size());
  }

  @Test
  void testRetrieveOuterJoinVectors() throws SQLException {
    prepareMockResultSet(true);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveOuterJoinByPersonVector(mockConnection, mockVector, 0.5, 10);
    assertEquals(1, lot.size());

    when(mockResultSet.next()).thenReturn(true, false);
    lot.retrieveOuterJoinBySkillSheetVector(mockConnection, mockVector, 0.5, 10);
    assertEquals(1, lot.size());
  }

  @Test
  void testRetrieveByPersonRawContent() throws SQLException {
    prepareMockResultSet(false);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonRawContent(mockConnection, "keyword");
    assertEquals(1, lot.size());

    when(mockResultSet.next()).thenReturn(true, false);
    lot.retrieveByPersonRawContent(
        mockConnection, "k1", List.of(new LogicalOperators(論理演算子.AND, "k2")));
    assertEquals(1, lot.size());
  }

  @Test
  void testRetrieveBySkillSheetRawContent() throws SQLException {
    prepareMockResultSet(false);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveBySkillSheetRawContent(mockConnection, "skill");
    assertEquals(1, lot.size());

    when(mockResultSet.next()).thenReturn(true, false);
    lot.retrieveBySkillSheetRawContent(
        mockConnection, "s1", List.of(new LogicalOperators(論理演算子.OR, "s2")));
    assertEquals(1, lot.size());
  }

  @Test
  void testGetEntityMethods() throws SQLException {
    prepareMockResultSet(false);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonRawContent(mockConnection, "keyword");

    assertNotNull(lot.getEntityByPk("p1"));
    assertNotNull(lot.getEntityByFileId("f1"));
    assertNull(lot.getEntityByPk("p2"));
    assertNull(lot.getEntityByFileId("f2"));
  }

  @Test
  void testToSelectionTexts() throws SQLException {
    prepareMockResultSet(false);
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonRawContent(mockConnection, "keyword");
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
    when(mockResultSetMetaData.getColumnCount()).thenReturn(1);
    when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("file_id");

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    List<FulltextCondition> conditions =
        List.of(new FulltextCondition("AND", "Java", false));
    lot.retrieveByPersonOrSkillSheetSummaryPaged(mockConnection, conditions, 1, 20);
    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());
  }

  @Test
  void testRetrieveByPersonOrSkillSheetSummaryPaged_nullConnection() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    List<FulltextCondition> conditions =
        List.of(new FulltextCondition("AND", "Java", false));
    assertDoesNotThrow(() -> lot.retrieveByPersonOrSkillSheetSummaryPaged(null, conditions, 1, 20));
    assertEquals(0, lot.size());
  }

  @Test
  void testSelectAll() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    assertDoesNotThrow(() -> lot.selectAll(mockConnection));
  }
}
