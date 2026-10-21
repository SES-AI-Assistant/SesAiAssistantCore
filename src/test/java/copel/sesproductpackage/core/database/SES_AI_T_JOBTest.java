package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
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
    assertThrows(IllegalArgumentException.class, () -> job.insert(null));
    assertThrows(IllegalArgumentException.class, () -> job.selectByPk(null));
    job.setJobId(null);
    job.selectByPk(connection);

    // tenantId null check - selectByPk throws IllegalArgumentException when tenantId is null
    job.setJobId("J1");
    job.setTenantId(null);
    assertThrows(IllegalArgumentException.class, () -> job.selectByPk(connection));

    // updateByPk return values
    job.setJobId("J1");
    job.setTenantId("default");
    assertTrue(job.updateByPk(connection));
    assertThrows(IllegalArgumentException.class, () -> job.updateByPk(null));
    job.setJobId(null);
    assertFalse(job.updateByPk(connection));

    // tenantId null check - updateByPk should return false when tenantId is null
    job.setJobId("J1");
    job.setTenantId(null);
    assertThrows(IllegalArgumentException.class, () -> job.updateByPk(connection));

    // deleteByPk return values
    job.setJobId("J1");
    job.setTenantId("default");
    assertTrue(job.deleteByPk(connection));
    assertThrows(IllegalArgumentException.class, () -> job.deleteByPk(null));

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

  @Test
  void testSetPlace_WithinMaxLength() {
    SES_AI_T_JOB entity = new SES_AI_T_JOB("test-tenant");

    entity.setPlace("広木駅");
    assertEquals("広木駅", entity.getPlace());

    String exact = "あ".repeat(SES_AI_T_JOB.PLACE_MAX_LENGTH);
    entity.setPlace(exact);
    assertEquals(exact, entity.getPlace());
  }

  @Test
  void testSetPlace_TruncatedWhenExceedsMaxLength() {
    SES_AI_T_JOB entity = new SES_AI_T_JOB("test-tenant");

    // 最寄駅の記載が無いメールでLLMが場所に設定してきた実例（32文字）
    entity.setPlace("フルリモート希望（初日出社・キャッチアップ・緊急時のみ出社OK）");

    assertEquals("フルリモート希望（初日出社・キャッチアップ・緊急時のみ出社O", entity.getPlace());
    assertEquals(SES_AI_T_JOB.PLACE_MAX_LENGTH, entity.getPlace().length());
  }

  @Test
  void testSetPlace_EmptyNormalizedToNull() {
    SES_AI_T_JOB entity = new SES_AI_T_JOB("test-tenant");

    entity.setPlace("");
    assertNull(entity.getPlace());

    entity.setPlace("   ");
    assertNull(entity.getPlace());

    entity.setPlace("null");
    assertNull(entity.getPlace());
  }

  @Test
  void testSetPlace_Null() {
    SES_AI_T_JOB entity = new SES_AI_T_JOB("test-tenant");

    entity.setPlace(null);

    assertNull(entity.getPlace());
  }

  @Test
  void testSelectByPk_PlaceNormalized() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    // 修正前に登録された既存行には "null" 文字列が残っている可能性がある
    when(rs.getString("place")).thenReturn("null");

    SES_AI_T_JOB entity = new SES_AI_T_JOB("test-tenant");
    entity.setJobId("ID1");
    entity.selectByPk(connection);

    assertNull(entity.getPlace());
  }

  @Test
  void testGetPlaceForDisplay() {
    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");

    job.setPlace(null);
    assertEquals("不明", job.getPlaceForDisplay());

    job.setPlace("宇宿駅");
    assertEquals("宇宿駅", job.getPlaceForDisplay());
  }

  @Test
  void testGetTitleForDisplay() {
    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");

    job.setTitle(null);
    assertEquals("案件名不明", job.getTitleForDisplay());

    job.setTitle("Unknown");
    assertEquals("案件名不明", job.getTitleForDisplay());

    job.setTitle("自治体向け業務システム改修");
    assertEquals("自治体向け業務システム改修", job.getTitleForDisplay());
  }

  @Test
  void testGetOfficeRequirementsForDisplay() {
    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");

    job.setOfficeRequirements(null);
    assertEquals("不明", job.getOfficeRequirementsForDisplay());

    // 0はフルリモートを意味するため「週0日」と表示しないこと
    job.setOfficeRequirements(0);
    assertEquals("フルリモート", job.getOfficeRequirementsForDisplay());

    job.setOfficeRequirements(5);
    job.setPlace("宇宿駅");
    assertEquals("常駐（宇宿駅）", job.getOfficeRequirementsForDisplay());

    job.setOfficeRequirements(3);
    assertEquals("週3日（宇宿駅）", job.getOfficeRequirementsForDisplay());

    // 場所が未設定でも「null」と表示しないこと
    job.setPlace(null);
    assertEquals("週3日（不明）", job.getOfficeRequirementsForDisplay());
  }

  @Test
  void testGetOfficeRequirementsForNotification() {
    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");
    job.setPlace("宇宿駅");

    // 未設定の場合は項目ごと省略させるためnullを返すこと
    assertNull(job.getOfficeRequirementsForNotification());

    job.setOfficeRequirements(0);
    assertEquals("フルリモート", job.getOfficeRequirementsForNotification());

    // 通知では場所を付けずに短く表記すること
    job.setOfficeRequirements(5);
    assertEquals("常駐", job.getOfficeRequirementsForNotification());

    job.setOfficeRequirements(1);
    assertEquals("週1出社", job.getOfficeRequirementsForNotification());
  }

  @Test
  void testToNotificationInfo() {
    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");
    job.setTitle("Salesforce導入支援");
    job.setUnitPrice(new Money(790000));
    assertEquals("Salesforce導入支援｜79万円", job.toNotificationInfo());

    // スキル見合いの仕様値を「999万円」と表示しないこと
    job.setUnitPrice(Money.NEGOTIABLE_PRICE);
    assertEquals("Salesforce導入支援｜スキル見合い", job.toNotificationInfo());

    // 単価が未設定の場合は「｜」以降を省略すること
    job.setTitle(null);
    job.setUnitPrice(Money.empty());
    assertEquals("案件名不明", job.toNotificationInfo());

    job.setUnitPrice(null);
    assertEquals("案件名不明", job.toNotificationInfo());
  }

  @Test
  void testGetTruncatedOverview() {
    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");
    assertEquals("", job.getTruncatedOverview(10));

    job.setOverview("Unknown");
    assertEquals("", job.getTruncatedOverview(10));

    // 改行や連続する空白は通知の行構成を崩さないよう半角スペース1つにまとめること
    job.setOverview(" 要件定義\n\n設計　担当 ");
    assertEquals("要件定義 設計 担当", job.getTruncatedOverview(10));

    // 指定文字数ちょうどの場合は省略記号を付けないこと
    job.setOverview("0123456789");
    assertEquals("0123456789", job.getTruncatedOverview(10));

    // 超過時は指定文字数で切って「…」を付けること
    job.setOverview("01234567890");
    assertEquals("0123456789…", job.getTruncatedOverview(10));

    // 切断位置が空白の場合は省略記号の直前に空白を残さないこと
    job.setOverview("012345678 9");
    assertEquals("012345678…", job.getTruncatedOverview(10));

    // サロゲートペアの文字を分断せず1文字として数えること
    job.setOverview("𠮷野家の店舗システム開発");
    assertEquals("𠮷野家…", job.getTruncatedOverview(3));
  }

  @Test
  void testSetPlace_UnknownNormalizedToNull() {
    SES_AI_T_JOB job = new SES_AI_T_JOB("test-tenant");

    job.setPlace("Unknown");

    assertNull(job.getPlace());
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * ベクトルデータがある場合に、ベクトルの文字列を8番目にバインドして行を更新することを確かめる。 登録時に埋め込んだベクトルを DB に保存するためである。
       *
       * @given 要約が "要約" で埋め込み結果が 0.5 と 1.0 のベクトル、テナントIDが "T001"、キーが "J001"
       * @when updateByPk を接続で実行する
       * @then vector_data に COALESCE の形の SQL を準備し、8番目に {@code "[0.5,1.0]"} を設定して1を返す
       */
      @Test
      void ベクトルデータがある場合_ベクトルを8番目に設定して行を更新する() throws Exception {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("要約")).thenReturn(new float[] {0.5f, 1.0f});
        SES_AI_T_JOB entity = new SES_AI_T_JOB("T001");
        entity.setJobId("J001");
        entity.setContentSummary("要約");
        entity.embedding(transformer);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        assertTrue(entity.updateByPk(connection));

        verify(connection).prepareStatement(contains("COALESCE(?::vector, vector_data)"));
        verify(statement).setString(8, "[0.5,1.0]");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ベクトルデータが無い場合に、8番目に null をバインドして既存のベクトルを残すことを確かめる。 埋め込みを作り直さない更新で、DB の vector_data を NULL
       * で上書きしないためである。
       *
       * @given ベクトルデータが無く、テナントIDが "T001"、キーが "J001"
       * @when updateByPk を接続で実行する
       * @then vector_data に COALESCE の形の SQL を準備し、8番目に null を設定して1を返す
       */
      @Test
      void ベクトルデータが無い場合_既存のベクトルを残して行を更新する() throws Exception {
        SES_AI_T_JOB entity = new SES_AI_T_JOB("T001");
        entity.setJobId("J001");
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        assertTrue(entity.updateByPk(connection));

        verify(connection).prepareStatement(contains("COALESCE(?::vector, vector_data)"));
        verify(statement).setString(8, null);
      }
    }
  }
}
