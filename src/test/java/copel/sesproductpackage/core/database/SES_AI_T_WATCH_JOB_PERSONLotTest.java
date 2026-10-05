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
 * SES_AI_T_WATCH_JOB_PERSONLot のテストクラス.
 *
 * @author Copel Co., Ltd.
 */
class SES_AI_T_WATCH_JOB_PERSONLotTest {

  private SES_AI_T_WATCH_JOB_PERSONLot lot;
  private Connection connection;
  private PreparedStatement ps;
  private ResultSet rs;

  @BeforeEach
  void setUp() throws SQLException {
    lot = new SES_AI_T_WATCH_JOB_PERSONLot();
    connection = mock(Connection.class);
    ps = mock(PreparedStatement.class);
    rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
  }

  @Test
  void testMapResultSetWithJobFields() throws SQLException {
    setupResultSetMocks(rs, true, false);

    SES_AI_T_WATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNotNull(entity);
    assertEquals("user-123", entity.getUserId());
    assertEquals("target-456", entity.getTargetId());
    assertEquals(SES_AI_T_WATCH.TargetType.JOB, entity.getTargetType());
    assertEquals("Test memo", entity.getMemo());
    assertEquals("test-tenant", entity.getTenantId());

    // JOB フィールドが有効
    assertEquals("Software Engineer", entity.getJobTitle());
    assertEquals("田中商事", entity.getJobFromName());
    assertEquals("Java案件の要約", entity.getJobContentSummary());
    assertEquals("Java案件の原文", entity.getJobRawContent());
    assertEquals("2026-04-01 00:00:00", entity.getJobStartDate().toString());
    assertEquals(3, entity.getJobOfficeRequirements());
    assertEquals(800000L, entity.getJobUnitPrice().toYenValue());
    assertEquals("Tokyo", entity.getJobPlace());
    assertEquals("Kanto", entity.getJobArea());

    // PERSON フィールドが NULL
    assertNull(entity.getPersonName());
    assertNull(entity.getPersonFromName());
    assertNull(entity.getPersonContentSummary());
    assertNull(entity.getPersonRawContent());
    assertNull(entity.getPersonAge());
    assertNull(entity.getPersonGender());
    assertNull(entity.getPersonStartDate());
    assertNull(entity.getPersonOfficeAvailability());
    assertTrue(entity.getPersonUnitPrice().isEmpty());
    assertNull(entity.getPersonPlace());
    assertNull(entity.getPersonArea());
  }

  @Test
  void testMapResultSetWithPersonFields() throws SQLException {
    setupResultSetMocks(rs, false, true);

    SES_AI_T_WATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNotNull(entity);
    assertEquals("user-123", entity.getUserId());
    assertEquals("target-789", entity.getTargetId());
    assertEquals(SES_AI_T_WATCH.TargetType.PERSON, entity.getTargetType());
    assertEquals("Person watch", entity.getMemo());
    assertEquals("test-tenant", entity.getTenantId());

    // JOB フィールドが NULL
    assertNull(entity.getJobTitle());
    assertNull(entity.getJobFromName());
    assertNull(entity.getJobContentSummary());
    assertNull(entity.getJobRawContent());
    assertNull(entity.getJobStartDate());
    assertNull(entity.getJobOfficeRequirements());
    assertTrue(entity.getJobUnitPrice().isEmpty());
    assertNull(entity.getJobPlace());
    assertNull(entity.getJobArea());

    // PERSON フィールドが有効
    assertEquals("John Doe", entity.getPersonName());
    assertEquals("鈴木エージェント", entity.getPersonFromName());
    assertEquals("要員の要約", entity.getPersonContentSummary());
    assertEquals("要員の原文", entity.getPersonRawContent());
    assertEquals(35, entity.getPersonAge());
    assertEquals(Gender.Man, entity.getPersonGender());
    assertEquals("2026-05-01 00:00:00", entity.getPersonStartDate().toString());
    assertEquals(2, entity.getPersonOfficeAvailability());
    assertEquals(600000L, entity.getPersonUnitPrice().toYenValue());
    assertEquals("Osaka", entity.getPersonPlace());
    assertEquals("Kansai", entity.getPersonArea());
  }

  @Test
  void testMapResultSetWithNullJobAndPersonFields() throws SQLException {
    setupResultSetMocks(rs, false, false);

    SES_AI_T_WATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNotNull(entity);
    assertEquals("user-123", entity.getUserId());
    assertEquals("test-tenant", entity.getTenantId());

    // 全てのJOB/PERSONフィールドが NULL
    assertNull(entity.getJobTitle());
    assertNull(entity.getJobStartDate());
    assertNull(entity.getJobOfficeRequirements());
    assertTrue(entity.getJobUnitPrice().isEmpty());
    assertNull(entity.getJobPlace());
    assertNull(entity.getJobArea());

    assertNull(entity.getPersonName());
    assertNull(entity.getPersonAge());
    assertNull(entity.getPersonGender());
    assertNull(entity.getPersonStartDate());
    assertNull(entity.getPersonOfficeAvailability());
    assertTrue(entity.getPersonUnitPrice().isEmpty());
    assertNull(entity.getPersonPlace());
    assertNull(entity.getPersonArea());
  }

  @Test
  void testMapResultSetWithNullOptionalFields() throws SQLException {
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("user_id")).thenReturn("user-123");
    when(rs.getString("target_id")).thenReturn("target-456");
    when(rs.getString("target_type")).thenReturn("JOB");
    when(rs.getString("memo")).thenReturn(null);
    when(rs.getString("register_date")).thenReturn(null);
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("ttl")).thenReturn(null);

    when(rs.getString("job_title")).thenReturn("Job Title");
    when(rs.getString("job_start_date")).thenReturn(null);
    when(rs.getObject("job_office_requirements")).thenReturn(null);
    when(rs.getBigDecimal("job_unit_price")).thenReturn(null);
    when(rs.getString("job_place")).thenReturn("Tokyo");
    when(rs.getString("job_area")).thenReturn("Kanto");

    when(rs.getString("person_name")).thenReturn(null);
    when(rs.getObject("person_age")).thenReturn(null);
    when(rs.getString("person_gender")).thenReturn(null);
    when(rs.getString("person_start_date")).thenReturn(null);
    when(rs.getObject("person_office_availability")).thenReturn(null);
    when(rs.getBigDecimal("person_unit_price")).thenReturn(null);
    when(rs.getString("person_place")).thenReturn(null);
    when(rs.getString("person_area")).thenReturn(null);

    SES_AI_T_WATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNotNull(entity);
    assertNull(entity.getMemo());
    assertNull(entity.getRegisterDate());
    assertNull(entity.getTtl());
    assertNull(entity.getJobStartDate());
    assertNull(entity.getJobOfficeRequirements());
    assertEquals("Tokyo", entity.getJobPlace());
  }

  @Test
  void testMapResultSetWithNullTargetType() throws SQLException {
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("user_id")).thenReturn("user-123");
    when(rs.getString("target_id")).thenReturn("target-456");
    when(rs.getString("target_type")).thenReturn(null);
    when(rs.getString("memo")).thenReturn(null);
    when(rs.getString("register_date")).thenReturn(null);
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("ttl")).thenReturn(null);

    when(rs.getString("job_title")).thenReturn(null);
    when(rs.getString("job_start_date")).thenReturn(null);
    when(rs.getObject("job_office_requirements")).thenReturn(null);
    when(rs.getBigDecimal("job_unit_price")).thenReturn(null);
    when(rs.getString("job_place")).thenReturn(null);
    when(rs.getString("job_area")).thenReturn(null);

    when(rs.getString("person_name")).thenReturn(null);
    when(rs.getObject("person_age")).thenReturn(null);
    when(rs.getString("person_gender")).thenReturn(null);
    when(rs.getString("person_start_date")).thenReturn(null);
    when(rs.getObject("person_office_availability")).thenReturn(null);
    when(rs.getBigDecimal("person_unit_price")).thenReturn(null);
    when(rs.getString("person_place")).thenReturn(null);
    when(rs.getString("person_area")).thenReturn(null);

    SES_AI_T_WATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertNull(entity.getTargetType());
  }

  @Test
  void testMapResultSetWithZeroOfficeRequirements() throws SQLException {
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("user_id")).thenReturn("user-123");
    when(rs.getString("target_id")).thenReturn("target-456");
    when(rs.getString("target_type")).thenReturn("JOB");
    when(rs.getString("memo")).thenReturn("Test");
    when(rs.getString("register_date")).thenReturn("2026-01-01 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("ttl")).thenReturn("2026-12-31 23:59:59");

    when(rs.getString("job_title")).thenReturn("Remote Job");
    when(rs.getString("job_start_date")).thenReturn("2026-04");
    when(rs.getObject("job_office_requirements")).thenReturn(0);
    when(rs.getInt("job_office_requirements")).thenReturn(0);
    when(rs.getBigDecimal("job_unit_price")).thenReturn(new BigDecimal("1000000"));
    when(rs.getString("job_place")).thenReturn("Remote");
    when(rs.getString("job_area")).thenReturn("N/A");

    when(rs.getString("person_name")).thenReturn(null);
    when(rs.getObject("person_age")).thenReturn(null);
    when(rs.getString("person_gender")).thenReturn(null);
    when(rs.getString("person_start_date")).thenReturn(null);
    when(rs.getObject("person_office_availability")).thenReturn(null);
    when(rs.getBigDecimal("person_unit_price")).thenReturn(null);
    when(rs.getString("person_place")).thenReturn(null);
    when(rs.getString("person_area")).thenReturn(null);

    SES_AI_T_WATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertEquals(0, entity.getJobOfficeRequirements());
  }

  @Test
  void testMapResultSetWithZeroOfficeAvailability() throws SQLException {
    when(rs.getString("tenant_id")).thenReturn("test-tenant");
    when(rs.getString("user_id")).thenReturn("user-123");
    when(rs.getString("target_id")).thenReturn("target-789");
    when(rs.getString("target_type")).thenReturn("PERSON");
    when(rs.getString("memo")).thenReturn("Watch person");
    when(rs.getString("register_date")).thenReturn("2026-01-01 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("ttl")).thenReturn("2026-12-31 23:59:59");

    when(rs.getString("job_title")).thenReturn(null);
    when(rs.getString("job_start_date")).thenReturn(null);
    when(rs.getObject("job_office_requirements")).thenReturn(null);
    when(rs.getBigDecimal("job_unit_price")).thenReturn(null);
    when(rs.getString("job_place")).thenReturn(null);
    when(rs.getString("job_area")).thenReturn(null);

    when(rs.getString("person_name")).thenReturn("Remote Worker");
    when(rs.getObject("person_age")).thenReturn(28);
    when(rs.getInt("person_age")).thenReturn(28);
    when(rs.getString("person_gender")).thenReturn("Woman");
    when(rs.getString("person_start_date")).thenReturn("2026-05");
    when(rs.getObject("person_office_availability")).thenReturn(0);
    when(rs.getInt("person_office_availability")).thenReturn(0);
    when(rs.getBigDecimal("person_unit_price")).thenReturn(new BigDecimal("750000"));
    when(rs.getString("person_place")).thenReturn("Remote");
    when(rs.getString("person_area")).thenReturn("N/A");

    SES_AI_T_WATCH_JOB_PERSON entity = lot.mapResultSet(rs);

    assertEquals(0, entity.getPersonOfficeAvailability());
  }

  @Test
  void testGetSelectAllSqlThrowsUnsupportedOperationException() {
    assertThrows(UnsupportedOperationException.class, () -> lot.getSelectAllSql());
  }

  @Test
  void testGetSelectSqlThrowsUnsupportedOperationException() {
    assertThrows(UnsupportedOperationException.class, () -> lot.getSelectSql());
  }

  @Test
  void testSelectAllThrowsUnsupportedOperationException() {
    assertThrows(
        UnsupportedOperationException.class, () -> lot.selectAll(connection, "test-tenant"));
  }

  @Test
  void testSelectByUserIdPaged() throws SQLException {
    // 1回目のnext()はCOUNT文用、2回目はデータ取得1件目、3回目でループ終了
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getLong(1)).thenReturn(1L);
    setupResultSetMocks(rs, true, true);

    lot.selectByUserIdPaged(connection, "test-tenant", "user-123", 1, 10);

    assertEquals(1, lot.size());
    verify(ps, times(2)).executeQuery();
  }

  @Test
  void testSelectByUserIdPagedWithNullConnection() throws SQLException {
    lot.selectByUserIdPaged(null, "test-tenant", "user-123", 1, 10);
    assertEquals(0, lot.size());
  }

  @Test
  void testSelectByUserIdPagedWithNullUserId() throws SQLException {
    lot.selectByUserIdPaged(connection, "test-tenant", null, 1, 10);
    assertEquals(0, lot.size());
  }

  /**
   * ResultSetのモック設定補助メソッド.
   *
   * @param resultSet モック化されたResultSet
   * @param includeJobFields JOB関連フィールドを含めるかどうか
   * @param includePersonFields PERSON関連フィールドを含めるかどうか
   * @throws SQLException
   */
  private void setupResultSetMocks(
      ResultSet resultSet, boolean includeJobFields, boolean includePersonFields)
      throws SQLException {
    when(resultSet.getString("tenant_id")).thenReturn("test-tenant");
    when(resultSet.getString("user_id")).thenReturn("user-123");

    if (includeJobFields) {
      when(resultSet.getString("target_id")).thenReturn("target-456");
      when(resultSet.getString("target_type")).thenReturn("JOB");
      when(resultSet.getString("memo")).thenReturn("Test memo");
      when(resultSet.getString("register_date")).thenReturn("2026-01-01 10:00:00");
      when(resultSet.getString("register_user")).thenReturn("admin");
      when(resultSet.getString("ttl")).thenReturn("2026-12-31 23:59:59");

      when(resultSet.getString("job_title")).thenReturn("Software Engineer");
      when(resultSet.getString("job_from_name")).thenReturn("田中商事");
      when(resultSet.getString("job_content_summary")).thenReturn("Java案件の要約");
      when(resultSet.getString("job_raw_content")).thenReturn("Java案件の原文");
      when(resultSet.getString("job_start_date")).thenReturn("2026-04");
      when(resultSet.getObject("job_office_requirements")).thenReturn(3);
      when(resultSet.getInt("job_office_requirements")).thenReturn(3);
      when(resultSet.getBigDecimal("job_unit_price")).thenReturn(new BigDecimal("800000"));
      when(resultSet.getString("job_place")).thenReturn("Tokyo");
      when(resultSet.getString("job_area")).thenReturn("Kanto");
    } else {
      when(resultSet.getString("target_id")).thenReturn("target-789");
      when(resultSet.getString("target_type")).thenReturn("PERSON");
      when(resultSet.getString("memo")).thenReturn("Person watch");
      when(resultSet.getString("register_date")).thenReturn("2026-01-02 11:00:00");
      when(resultSet.getString("register_user")).thenReturn("user");
      when(resultSet.getString("ttl")).thenReturn("2026-12-31 23:59:59");

      when(resultSet.getString("job_title")).thenReturn(null);
      when(resultSet.getString("job_start_date")).thenReturn(null);
      when(resultSet.getObject("job_office_requirements")).thenReturn(null);
      when(resultSet.getBigDecimal("job_unit_price")).thenReturn(null);
      when(resultSet.getString("job_place")).thenReturn(null);
      when(resultSet.getString("job_area")).thenReturn(null);
    }

    if (includePersonFields) {
      when(resultSet.getString("person_name")).thenReturn("John Doe");
      when(resultSet.getString("person_from_name")).thenReturn("鈴木エージェント");
      when(resultSet.getString("person_content_summary")).thenReturn("要員の要約");
      when(resultSet.getString("person_raw_content")).thenReturn("要員の原文");
      when(resultSet.getObject("person_age")).thenReturn(35);
      when(resultSet.getInt("person_age")).thenReturn(35);
      when(resultSet.getString("person_gender")).thenReturn("Man");
      when(resultSet.getString("person_start_date")).thenReturn("2026-05");
      when(resultSet.getObject("person_office_availability")).thenReturn(2);
      when(resultSet.getInt("person_office_availability")).thenReturn(2);
      when(resultSet.getBigDecimal("person_unit_price")).thenReturn(new BigDecimal("600000"));
      when(resultSet.getString("person_place")).thenReturn("Osaka");
      when(resultSet.getString("person_area")).thenReturn("Kansai");
    } else {
      when(resultSet.getString("person_name")).thenReturn(null);
      when(resultSet.getObject("person_age")).thenReturn(null);
      when(resultSet.getString("person_gender")).thenReturn(null);
      when(resultSet.getString("person_start_date")).thenReturn(null);
      when(resultSet.getObject("person_office_availability")).thenReturn(null);
      when(resultSet.getBigDecimal("person_unit_price")).thenReturn(null);
      when(resultSet.getString("person_place")).thenReturn(null);
      when(resultSet.getString("person_area")).thenReturn(null);
    }
  }
}
