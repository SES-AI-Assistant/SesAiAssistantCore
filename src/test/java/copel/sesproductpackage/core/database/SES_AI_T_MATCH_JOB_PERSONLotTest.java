package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.Gender;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SES_AI_T_MATCH_JOB_PERSONLot のテストクラス.
 *
 * @author Copel Co., Ltd.
 */
class SES_AI_T_MATCH_JOB_PERSONLotTest {

  private SES_AI_T_MATCH_JOB_PERSONLot lot;
  private Connection connection;
  private PreparedStatement ps;
  private ResultSet rs;

  @BeforeEach
  void setUp() throws SQLException {
    lot = new SES_AI_T_MATCH_JOB_PERSONLot();
    connection = mock(Connection.class);
    ps = mock(PreparedStatement.class);
    rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
  }

  @Test
  void testSelectAll() throws SQLException {
    when(rs.next()).thenReturn(true, false);
    setupResultSetMocks(rs, true, true);

    lot.selectAll(connection, "test-tenant");
    assertEquals(1, lot.size());
    assertNotNull(lot.get(0));
    assertEquals("M1", lot.get(0).getMatchingId());
  }

  @Test
  void testSelectAllWithMultipleRows() throws SQLException {
    when(rs.next()).thenReturn(true, true, false);
    setupResultSetMocks(rs, true, true);

    lot.selectAll(connection, "test-tenant");
    assertEquals(2, lot.size());
  }

  @Test
  void testMapResultSetWithAllFields() throws SQLException {
    setupResultSetMocks(rs, true, true);
    SES_AI_T_MATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNotNull(entity);
    assertEquals("M1", entity.getMatchingId());
    assertEquals("U1", entity.getUserId());
    assertEquals("J1", entity.getJobId());
    assertEquals("P1", entity.getPersonId());
    assertEquals("JOB_CONTENT", entity.getJobContent());
    assertEquals("PERSON_CONTENT", entity.getPersonContent());
    assertEquals(85, entity.getScore());
    assertEquals(100000L, entity.getProfit().toYenValue());
    assertEquals("test-tenant", entity.getTenantId());
    assertEquals("Job Title", entity.getJobTitle());
    assertEquals(800000L, entity.getJobUnitPrice().toYenValue());
    assertEquals("Person Name", entity.getPersonName());
    assertEquals(35, entity.getPersonAge());
    assertEquals(Gender.Man, entity.getPersonGender());
    assertEquals(600000L, entity.getPersonUnitPrice().toYenValue());
  }

  @Test
  void testMapResultSetWithNullJobFields() throws SQLException {
    setupResultSetMocks(rs, false, true);

    SES_AI_T_MATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNotNull(entity);
    assertEquals("M1", entity.getMatchingId());
    // JOB フィールドが NULL
    assertNull(entity.getJobTitle());
    assertTrue(entity.getJobUnitPrice().isEmpty());
    // PERSON フィールドは有効
    assertEquals("Person Name", entity.getPersonName());
  }

  @Test
  void testMapResultSetWithNullPersonFields() throws SQLException {
    setupResultSetMocks(rs, true, false);

    SES_AI_T_MATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNotNull(entity);
    assertEquals("M1", entity.getMatchingId());
    // JOB フィールドは有効
    assertEquals("Job Title", entity.getJobTitle());
    // PERSON フィールドが NULL
    assertNull(entity.getPersonName());
    assertNull(entity.getPersonAge());
    assertNull(entity.getPersonGender());
    assertTrue(entity.getPersonUnitPrice().isEmpty());
  }

  @Test
  void testMapResultSetWithNullJobAndPersonFields() throws SQLException {
    setupResultSetMocks(rs, false, false);

    SES_AI_T_MATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNotNull(entity);
    assertEquals("M1", entity.getMatchingId());
    assertNull(entity.getJobTitle());
    assertTrue(entity.getJobUnitPrice().isEmpty());
    assertNull(entity.getPersonName());
    assertNull(entity.getPersonAge());
    assertNull(entity.getPersonGender());
    assertTrue(entity.getPersonUnitPrice().isEmpty());
  }

  @Test
  void testMapResultSetWithNullScore() throws SQLException {
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("matching_id")).thenReturn("M1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("job_id")).thenReturn("J1");
    when(rs.getString("person_id")).thenReturn("P1");
    when(rs.getString("job_content")).thenReturn("JOB_CONTENT");
    when(rs.getString("person_content")).thenReturn("PERSON_CONTENT");
    when(rs.getString("status_cd")).thenReturn("10");
    when(rs.getString("evaluation_text")).thenReturn("EVAL");
    when(rs.getObject("score")).thenReturn(null);
    when(rs.getString("must_evaluation_text")).thenReturn("MUST");
    when(rs.getString("want_evaluation_text")).thenReturn("WANT");
    when(rs.getString("place_evaluation_text")).thenReturn("PLACE");
    when(rs.getString("office_evaluation_text")).thenReturn("OFFICE");
    when(rs.getString("other_evaluation_text")).thenReturn("OTHER");
    when(rs.getBigDecimal("profit")).thenReturn(new BigDecimal("100000"));
    when(rs.getString("register_date")).thenReturn("2026-01-01 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("job_title")).thenReturn("Job Title");
    when(rs.getBigDecimal("job_unit_price")).thenReturn(new BigDecimal("800000"));
    when(rs.getString("person_name")).thenReturn("Person Name");
    when(rs.getObject("person_age")).thenReturn(null);
    when(rs.getString("person_gender")).thenReturn("Man");
    when(rs.getBigDecimal("person_unit_price")).thenReturn(new BigDecimal("600000"));

    SES_AI_T_MATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNull(entity.getScore());
  }

  @Test
  void testMapResultSetWithNullProfit() throws SQLException {
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("matching_id")).thenReturn("M1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("job_id")).thenReturn("J1");
    when(rs.getString("person_id")).thenReturn("P1");
    when(rs.getString("job_content")).thenReturn("JOB_CONTENT");
    when(rs.getString("person_content")).thenReturn("PERSON_CONTENT");
    when(rs.getString("status_cd")).thenReturn("10");
    when(rs.getString("evaluation_text")).thenReturn("EVAL");
    when(rs.getObject("score")).thenReturn(null);
    when(rs.getString("must_evaluation_text")).thenReturn("MUST");
    when(rs.getString("want_evaluation_text")).thenReturn("WANT");
    when(rs.getString("place_evaluation_text")).thenReturn("PLACE");
    when(rs.getString("office_evaluation_text")).thenReturn("OFFICE");
    when(rs.getString("other_evaluation_text")).thenReturn("OTHER");
    when(rs.getBigDecimal("profit")).thenReturn(null);
    when(rs.getString("register_date")).thenReturn("2026-01-01 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("job_title")).thenReturn("Job Title");
    when(rs.getBigDecimal("job_unit_price")).thenReturn(new BigDecimal("800000"));
    when(rs.getString("person_name")).thenReturn("Person Name");
    when(rs.getObject("person_age")).thenReturn(35);
    when(rs.getInt("person_age")).thenReturn(35);
    when(rs.getString("person_gender")).thenReturn("Man");
    when(rs.getBigDecimal("person_unit_price")).thenReturn(new BigDecimal("600000"));

    SES_AI_T_MATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertTrue(entity.getProfit().isEmpty());
  }

  @Test
  void testSelectByJobIdPaged() throws SQLException {
    // 1回目のnext()はCOUNT文用、2回目はデータ取得1件目、3回目でループ終了
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getLong(1)).thenReturn(1L);
    setupResultSetMocks(rs, true, true);

    lot.selectByJobIdPaged(connection, "test-tenant", "J1", 1, 10);

    assertEquals(1, lot.size());
    verify(ps, times(2)).executeQuery();
  }

  @Test
  void testSelectByJobIdPagedWithNullConnection() throws SQLException {
    lot.selectByJobIdPaged(null, "test-tenant", "J1", 1, 10);
    assertEquals(0, lot.size());
  }

  @Test
  void testSelectByJobIdPagedWithNullJobId() throws SQLException {
    lot.selectByJobIdPaged(connection, "test-tenant", null, 1, 10);
    assertEquals(0, lot.size());
  }

  @Test
  void testSelectByPersonIdPaged() throws SQLException {
    // 1回目のnext()はCOUNT文用、2回目はデータ取得1件目、3回目でループ終了
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getLong(1)).thenReturn(1L);
    setupResultSetMocks(rs, true, true);

    lot.selectByPersonIdPaged(connection, "test-tenant", "P1", 1, 10);

    assertEquals(1, lot.size());
    verify(ps, times(2)).executeQuery();
  }

  @Test
  void testSelectByPersonIdPagedWithNullConnection() throws SQLException {
    lot.selectByPersonIdPaged(null, "test-tenant", "P1", 1, 10);
    assertEquals(0, lot.size());
  }

  @Test
  void testSelectByPersonIdPagedWithNullPersonId() throws SQLException {
    lot.selectByPersonIdPaged(connection, "test-tenant", null, 1, 10);
    assertEquals(0, lot.size());
  }

  @Test
  void testLotSize() {
    assertEquals(0, lot.size());
    SES_AI_T_MATCH_JOB_PERSON entity = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");
    lot.add(entity);
    assertEquals(1, lot.size());
  }

  @Test
  void testLotIsEmpty() {
    assertTrue(lot.isEmpty());
    SES_AI_T_MATCH_JOB_PERSON entity = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");
    lot.add(entity);
    assertFalse(lot.isEmpty());
  }

  @Test
  void testLotGet() {
    SES_AI_T_MATCH_JOB_PERSON entity = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");
    entity.setMatchingId("M001");
    lot.add(entity);

    assertEquals(entity, lot.get(0));
    assertNull(lot.get(1));
  }

  @Test
  void testMapResultSetWithFemaleGender() throws SQLException {
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("matching_id")).thenReturn("M1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("job_id")).thenReturn("J1");
    when(rs.getString("person_id")).thenReturn("P1");
    when(rs.getString("job_content")).thenReturn("JOB_CONTENT");
    when(rs.getString("person_content")).thenReturn("PERSON_CONTENT");
    when(rs.getString("status_cd")).thenReturn("10");
    when(rs.getString("evaluation_text")).thenReturn("EVAL");
    when(rs.getObject("score")).thenReturn(85);
    when(rs.getInt("score")).thenReturn(85);
    when(rs.getString("must_evaluation_text")).thenReturn("MUST");
    when(rs.getString("want_evaluation_text")).thenReturn("WANT");
    when(rs.getString("place_evaluation_text")).thenReturn("PLACE");
    when(rs.getString("office_evaluation_text")).thenReturn("OFFICE");
    when(rs.getString("other_evaluation_text")).thenReturn("OTHER");
    when(rs.getBigDecimal("profit")).thenReturn(new BigDecimal("100000"));
    when(rs.getString("register_date")).thenReturn("2026-01-01 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("job_title")).thenReturn("Job Title");
    when(rs.getBigDecimal("job_unit_price")).thenReturn(new BigDecimal("800000"));
    when(rs.getString("person_name")).thenReturn("Person Name");
    when(rs.getObject("person_age")).thenReturn(30);
    when(rs.getInt("person_age")).thenReturn(30);
    when(rs.getString("person_gender")).thenReturn("Woman");
    when(rs.getBigDecimal("person_unit_price")).thenReturn(new BigDecimal("500000"));

    SES_AI_T_MATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertEquals(Gender.Woman, entity.getPersonGender());
  }

  @Test
  void testMapResultSetWithNullPersonGender() throws SQLException {
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("matching_id")).thenReturn("M1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("job_id")).thenReturn("J1");
    when(rs.getString("person_id")).thenReturn("P1");
    when(rs.getString("job_content")).thenReturn("JOB_CONTENT");
    when(rs.getString("person_content")).thenReturn("PERSON_CONTENT");
    when(rs.getString("status_cd")).thenReturn("10");
    when(rs.getString("evaluation_text")).thenReturn("EVAL");
    when(rs.getObject("score")).thenReturn(85);
    when(rs.getInt("score")).thenReturn(85);
    when(rs.getString("must_evaluation_text")).thenReturn("MUST");
    when(rs.getString("want_evaluation_text")).thenReturn("WANT");
    when(rs.getString("place_evaluation_text")).thenReturn("PLACE");
    when(rs.getString("office_evaluation_text")).thenReturn("OFFICE");
    when(rs.getString("other_evaluation_text")).thenReturn("OTHER");
    when(rs.getBigDecimal("profit")).thenReturn(new BigDecimal("100000"));
    when(rs.getString("register_date")).thenReturn("2026-01-01 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("job_title")).thenReturn("Job Title");
    when(rs.getBigDecimal("job_unit_price")).thenReturn(new BigDecimal("800000"));
    when(rs.getString("person_name")).thenReturn("Person Name");
    when(rs.getObject("person_age")).thenReturn(30);
    when(rs.getInt("person_age")).thenReturn(30);
    when(rs.getString("person_gender")).thenReturn(null);
    when(rs.getBigDecimal("person_unit_price")).thenReturn(new BigDecimal("600000"));

    SES_AI_T_MATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNull(entity.getPersonGender());
  }

  /**
   * ResultSet モック共通設定（JOB/PERSON フィールドのキャリア選択可能）.
   *
   * @param rs モック ResultSet
   * @param hasJobFields JOB フィールドを設定するか
   * @param hasPersonFields PERSON フィールドを設定するか
   * @throws SQLException
   */
  private void setupResultSetMocks(ResultSet rs, boolean hasJobFields, boolean hasPersonFields)
      throws SQLException {
    // 基本フィールド（常に設定）
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("matching_id")).thenReturn("M1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("job_id")).thenReturn("J1");
    when(rs.getString("person_id")).thenReturn("P1");
    when(rs.getString("job_content")).thenReturn("JOB_CONTENT");
    when(rs.getString("person_content")).thenReturn("PERSON_CONTENT");
    when(rs.getString("status_cd")).thenReturn("10");
    when(rs.getString("evaluation_text")).thenReturn("EVAL");
    when(rs.getObject("score")).thenReturn(85);
    when(rs.getInt("score")).thenReturn(85);
    when(rs.getString("must_evaluation_text")).thenReturn("MUST");
    when(rs.getString("want_evaluation_text")).thenReturn("WANT");
    when(rs.getString("place_evaluation_text")).thenReturn("PLACE");
    when(rs.getString("office_evaluation_text")).thenReturn("OFFICE");
    when(rs.getString("other_evaluation_text")).thenReturn("OTHER");
    when(rs.getBigDecimal("profit")).thenReturn(new BigDecimal("100000"));
    when(rs.getString("register_date")).thenReturn("2026-01-01 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");

    // JOB フィールド
    if (hasJobFields) {
      when(rs.getString("job_title")).thenReturn("Job Title");
      when(rs.getBigDecimal("job_unit_price")).thenReturn(new BigDecimal("800000"));
    } else {
      when(rs.getString("job_title")).thenReturn(null);
      when(rs.getBigDecimal("job_unit_price")).thenReturn(null);
    }

    // PERSON フィールド
    if (hasPersonFields) {
      when(rs.getString("person_name")).thenReturn("Person Name");
      when(rs.getObject("person_age")).thenReturn(35);
      when(rs.getInt("person_age")).thenReturn(35);
      when(rs.getString("person_gender")).thenReturn("Man");
      when(rs.getBigDecimal("person_unit_price")).thenReturn(new BigDecimal("600000"));
    } else {
      when(rs.getString("person_name")).thenReturn(null);
      when(rs.getObject("person_age")).thenReturn(null);
      when(rs.getString("person_gender")).thenReturn(null);
      when(rs.getBigDecimal("person_unit_price")).thenReturn(null);
    }
  }
}
