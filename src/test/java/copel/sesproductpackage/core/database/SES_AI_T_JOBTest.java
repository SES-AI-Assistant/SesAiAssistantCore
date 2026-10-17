package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_T_JOBTest {

  @Test
  void testJOBMethods() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getInt(1)).thenReturn(0);

    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");
    job.setJobId("J1");
    job.setTenantId("default");

    // uniqueCheck
    when(rs.getInt(1)).thenReturn(0);
    assertTrue(job.uniqueCheck(connection, 0.8));
    when(rs.getInt(1)).thenReturn(1);
    assertFalse(job.uniqueCheck(connection, 0.8));

    // selectByPk hits
    when(rs.next()).thenReturn(true);
    job.selectByPk(connection);
    when(rs.next()).thenReturn(false);
    job.selectByPk(connection);

    // updateByPk - expect 1 then 0
    // Reset jobId/tenantId before updateByPk since selectByPk(connection) didn't set them
    job.setJobId("J1");
    job.setTenantId("default");
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(job.updateByPk(connection));
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(job.updateByPk(connection));

    // deleteByPk - expect 1 then 0
    // Reset jobId/tenantId before deleteByPk
    job.setJobId("J1");
    job.setTenantId("default");
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(job.deleteByPk(connection));
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(job.deleteByPk(connection));

    // embedding
    Transformer mockTrans = mock(Transformer.class);
    when(mockTrans.embedding(anyString())).thenReturn(new float[] {0.1f});
    job.setContentSummary("summary");
    job.embedding(mockTrans);
    assertNotNull(job.getVectorData());

    // registerDate & ttl == null branches
    job.setRegisterDate(null);
    job.setTtl(null);
    when(ps.executeUpdate()).thenReturn(1);
    job.insert(connection);
  }

  @Test
  void testJOB() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString(anyString())).thenReturn("test");

    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");
    job.setJobId("J1");
    job.setTenantId("default");
    job.setRegisterDate(new OriginalDateTime());

    assertEquals(1, job.insert(connection));
    assertNotNull(job.to案件選出用文章());
    assertNotNull(job.toString());

    // Null checks
    assertEquals(0, job.insert(null));
    job.selectByPk(null);
    job.setJobId(null);
    job.selectByPk(connection);

    // tenantId null check - selectByPk should return null when tenantId is null
    job.setJobId("J1");
    job.setTenantId(null);
    job.selectByPk(connection);

    // updateByPk return values
    job.setJobId("J1");
    job.setTenantId("default");
    assertTrue(job.updateByPk(connection));
    assertFalse(job.updateByPk(null));
    job.setJobId(null);
    assertFalse(job.updateByPk(connection));

    // tenantId null check - updateByPk should return false when tenantId is null
    job.setJobId("J1");
    job.setTenantId(null);
    assertFalse(job.updateByPk(connection));

    // deleteByPk return values
    job.setJobId("J1");
    job.setTenantId("default");
    assertTrue(job.deleteByPk(connection));
    assertFalse(job.deleteByPk(null));

    // vectorData populated paths
    copel.sesproductpackage.core.unit.Vector vec =
        new copel.sesproductpackage.core.unit.Vector(mock(Transformer.class));
    try {
      java.lang.reflect.Field embField =
          copel.sesproductpackage.core.unit.Vector.class.getDeclaredField("value");
      embField.setAccessible(true);
      embField.set(vec, new float[] {0.1f});
    } catch (Exception e) {
      // ignore
    }
    job.setVectorData(vec);
    job.setTtl(new OriginalDateTime());
    assertEquals(1, job.insert(connection));
    assertTrue(job.updateByPk(connection));
  }

  @Test
  void testGetSummaryWithTargetPrice() {
    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");
    job.setTitle("案件タイトル");
    job.setOverview("案件概要");
    job.setMustSkills("必須スキル");
    job.setWantSkills("尚可スキル");
    job.setStartDate(new OriginalDateTime(2026, 11, 1, 0, 0, 0));
    job.setUnitPrice(new Money(600_000L));
    job.setOfficeRequirements(0);
    job.setOtherRequirements("その他要件");

    // 単価が確定額（スキル見合いでない）、出社要件がフルリモートの場合
    String summary = job.getSummaryWithTargetPrice(new Money(600_000L));
    assertTrue(summary.contains("■概要\n案件概要\n"));
    assertTrue(summary.contains("■必須\n必須スキル\n"));
    assertTrue(summary.contains("■尚可\n尚可スキル\n"));
    assertTrue(summary.contains("■開始: 11月\n"));
    assertFalse(summary.contains("11月月"));
    assertTrue(summary.contains("■単価: "));
    assertTrue(summary.contains("■出社要件: フルリモート\n"));
    assertTrue(summary.contains("■その他\nその他要件\n"));

    // 単価がスキル見合い（応相談）の場合、要員単価に乗数をかけた金額を表示する
    String negotiableSummary = job.getSummaryWithTargetPrice(Money.NEGOTIABLE_PRICE, 0.9);
    assertTrue(negotiableSummary.contains("■単価: "));

    // 常駐案件（場所未設定のため「不明」表示）
    job.setOfficeRequirements(5);
    job.setPlace(null);
    String onsiteSummary = job.getSummaryWithTargetPrice(new Money(600_000L));
    assertTrue(onsiteSummary.contains("■出社要件: 常駐\n"));
    assertTrue(onsiteSummary.contains("■場所: 不明\n"));

    // 週N出社案件（場所設定あり）
    job.setOfficeRequirements(3);
    job.setPlace("東京都");
    String weeklySummary = job.getSummaryWithTargetPrice(new Money(600_000L));
    assertTrue(weeklySummary.contains("■出社要件: 週3\n"));
    assertTrue(weeklySummary.contains("■場所: 東京都\n"));

    // 常駐案件（場所設定あり）
    job.setOfficeRequirements(5);
    job.setPlace("大阪府");
    String onsiteWithPlaceSummary = job.getSummaryWithTargetPrice(new Money(600_000L));
    assertTrue(onsiteWithPlaceSummary.contains("■場所: 大阪府\n"));

    // 週N出社案件（場所未設定のため「不明」表示）
    job.setOfficeRequirements(3);
    job.setPlace(null);
    String weeklyWithoutPlaceSummary = job.getSummaryWithTargetPrice(new Money(600_000L));
    assertTrue(weeklyWithoutPlaceSummary.contains("■場所: 不明\n"));

    // 単価未指定（null）の場合は■単価セクションが出力されない
    job.setOfficeRequirements(0);
    String noPriceSummary = job.getSummaryWithTargetPrice(null);
    assertFalse(noPriceSummary.contains("■単価"));

    // 必須/尚可/その他が空文字列の場合は各セクションが出力されない
    job.setMustSkills("");
    job.setWantSkills("");
    job.setOtherRequirements("");
    String blankFieldsSummary = job.getSummaryWithTargetPrice(new Money(600_000L));
    assertFalse(blankFieldsSummary.contains("■必須"));
    assertFalse(blankFieldsSummary.contains("■尚可"));
    assertFalse(blankFieldsSummary.contains("■その他"));

    // 必須/尚可/開始/単価/その他が未設定の場合は各セクションが出力されない
    SES_AI_T_JOB emptyJob = new SES_AI_T_JOB("test-tenant");
    emptyJob.setTitle("案件タイトル");
    emptyJob.setOverview("案件概要");
    emptyJob.setOfficeRequirements(0);
    String minimalSummary = emptyJob.getSummaryWithTargetPrice(Money.empty());
    assertFalse(minimalSummary.contains("■必須"));
    assertFalse(minimalSummary.contains("■尚可"));
    assertFalse(minimalSummary.contains("■開始"));
    assertFalse(minimalSummary.contains("■単価"));
    assertFalse(minimalSummary.contains("■その他"));
  }

  @Test
  void testLombokCoverage() throws Exception {
    SES_AI_T_JOB obj1 = new SES_AI_T_JOB("test-tenant");
    SES_AI_T_JOB obj2 = new SES_AI_T_JOB("test-tenant");
    SES_AI_T_JOB diff = new SES_AI_T_JOB("test-tenant");
    diff.setJobId("test_id");
    diff.setRawContent("test");

    assertTrue(obj1.equals(obj1));
    assertFalse(obj1.equals(null));
    assertFalse(obj1.equals(new Object()));
    assertTrue(obj1.equals(obj2));
    assertFalse(obj1.equals(diff));
    assertTrue(obj1.canEqual(obj2));
    assertFalse(obj1.canEqual(new Object()));
    assertEquals(obj1.hashCode(), obj2.hashCode());
    assertNotNull(obj1.toString());
  }
}
