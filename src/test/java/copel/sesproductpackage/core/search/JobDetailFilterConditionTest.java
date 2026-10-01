package copel.sesproductpackage.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class JobDetailFilterConditionTest {

  @Test
  void testBuildWhereClause_allFieldsNull_returnsEmpty() {
    JobDetailFilterCondition condition = new JobDetailFilterCondition();
    JobDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("j");
    assertTrue(result.isEmpty());
    assertEquals(0, result.getParams().size());
  }

  @Test
  void testBuildWhereClause_noAlias() {
    JobDetailFilterCondition condition = new JobDetailFilterCondition();
    condition.setArea(Area.関東_首都圏);
    JobDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause();
    assertEquals("area = ?", result.getWhereClauseWithoutWhereKeyword());
  }

  @Test
  void testBuildWhereClause_startDate() {
    JobDetailFilterCondition condition = new JobDetailFilterCondition();
    OriginalDateTime startDate = new OriginalDateTime("2026/11");
    condition.setStartDate(startDate);
    JobDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("j");
    assertEquals("j.start_date >= ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(1, result.getParams().size());
    assertEquals(startDate, result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_area() {
    JobDetailFilterCondition condition = new JobDetailFilterCondition();
    condition.setArea(Area.関東_首都圏);
    JobDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("j");
    assertEquals("j.area = ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(Area.関東_首都圏, result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_officeRequirements_fullRemote() {
    // フルリモート(0)を指定した場合、出社を伴う案件(office_requirements >= 1)を除外するため
    // "<=" で絞り込む必要がある（">=" だと全件ヒットしてしまう回帰の防止）
    JobDetailFilterCondition condition = new JobDetailFilterCondition();
    condition.setOfficeRequirements(0);
    JobDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("j");
    assertEquals("j.office_requirements <= ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(1, result.getParams().size());
    assertEquals(0, result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_officeRequirements_weekly2Days() {
    JobDetailFilterCondition condition = new JobDetailFilterCondition();
    condition.setOfficeRequirements(2);
    JobDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("j");
    assertEquals("j.office_requirements <= ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(2, result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_priceRange() {
    JobDetailFilterCondition condition = new JobDetailFilterCondition();
    condition.setMinPrice(new Money(new BigDecimal("500000")));
    condition.setMaxPrice(new Money(new BigDecimal("800000")));
    JobDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("j");
    assertEquals(
        "j.unit_price >= ? AND j.unit_price <= ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(new BigDecimal("500000"), result.getParams().get(0));
    assertEquals(new BigDecimal("800000"), result.getParams().get(1));
  }

  @Test
  void testBuildWhereClause_allFieldsCombined() {
    JobDetailFilterCondition condition = new JobDetailFilterCondition();
    condition.setStartDate(new OriginalDateTime("2026/11"));
    condition.setArea(Area.関東_首都圏);
    condition.setOfficeRequirements(0);
    condition.setMinPrice(new Money(new BigDecimal("500000")));
    condition.setMaxPrice(new Money(new BigDecimal("800000")));

    JobDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("j");

    assertEquals(
        "j.start_date >= ? AND j.area = ? AND j.office_requirements <= ?"
            + " AND j.unit_price >= ? AND j.unit_price <= ?",
        result.getWhereClauseWithoutWhereKeyword());
    assertEquals(5, result.getParams().size());
  }
}
