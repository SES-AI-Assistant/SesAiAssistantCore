package copel.sesproductpackage.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PersonDetailFilterConditionTest {

  @Test
  void testBuildWhereClause_allFieldsNull_returnsEmpty() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertTrue(result.isEmpty());
    assertEquals(0, result.getParams().size());
  }

  @Test
  void testBuildWhereClause_noAlias() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setMinAge(20);
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause();
    assertEquals("age >= ?", result.getWhereClauseWithoutWhereKeyword());
  }

  @Test
  void testBuildWhereClause_startDate() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    OriginalDateTime startDate = new OriginalDateTime("2026/11");
    condition.setStartDate(startDate);
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals("p.start_date >= ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(1, result.getParams().size());
    assertEquals(startDate, result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_priceRange() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setMinPrice(new Money(new BigDecimal("500000")));
    condition.setMaxPrice(new Money(new BigDecimal("800000")));
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals(
        "p.unit_price >= ? AND p.unit_price <= ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(new BigDecimal("500000"), result.getParams().get(0));
    assertEquals(new BigDecimal("800000"), result.getParams().get(1));
  }

  @Test
  void testBuildWhereClause_ageRange() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setMinAge(20);
    condition.setMaxAge(40);
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals("p.age >= ? AND p.age <= ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(20, result.getParams().get(0));
    assertEquals(40, result.getParams().get(1));
  }

  @Test
  void testBuildWhereClause_gender() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setGender("Man");
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals("p.gender = ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals("Man", result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_nationalityJapan_exactMatch() {
    // フロントエンドが nationality カラムの実際の格納値「日本」をそのまま送信してくる想定
    // （本番でnationality="JP"指定時に検索結果が0件になっていた不具合の回帰テスト。
    // "JP"のようなコード変換は行わず、送られてきた値でそのまま完全一致検索する）
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setNationality("日本");
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals("p.nationality = ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(1, result.getParams().size());
    assertEquals("日本", result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_nationalityOther_mapsToNotEqualsJapan() {
    // "OTHER" は「日本」以外の全ての自由入力値（例：ベトナム等）を対象にする特別な値
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setNationality("OTHER");
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals(
        "p.nationality IS NOT NULL AND p.nationality <> ?",
        result.getWhereClauseWithoutWhereKeyword());
    assertEquals(1, result.getParams().size());
    assertEquals("日本", result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_nationalityArbitraryValue_exactMatch() {
    // "OTHER"以外の値は常に完全一致（例えば特定の国名を直接指定するケースにも対応）
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setNationality("ベトナム");
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals("p.nationality = ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals("ベトナム", result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_nationalityEmpty_isIgnored() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setNationality("");
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertTrue(result.isEmpty());
  }

  @Test
  void testBuildWhereClause_area() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setArea(Area.関東_首都圏);
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals("p.area = ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(Area.関東_首都圏, result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_officeAvailability() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setOfficeAvailability(3);
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals("p.office_availability >= ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals(3, result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_organization() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setCompaniesTierNumber(1);
    condition.setEmploymentType("正社員");
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertEquals("p.organization = ?", result.getWhereClauseWithoutWhereKeyword());
    assertEquals("1社先正社員", result.getParams().get(0));
  }

  @Test
  void testBuildWhereClause_organization_employmentTypeEmpty_isIgnored() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setCompaniesTierNumber(1);
    condition.setEmploymentType("");
    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");
    assertTrue(result.isEmpty());
  }

  @Test
  void testBuildWhereClause_allFieldsCombined() {
    PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
    condition.setStartDate(new OriginalDateTime("2026/11"));
    condition.setMinPrice(new Money(new BigDecimal("500000")));
    condition.setMaxPrice(new Money(new BigDecimal("800000")));
    condition.setMinAge(20);
    condition.setMaxAge(40);
    condition.setGender("Woman");
    condition.setNationality("日本");
    condition.setArea(Area.関東_首都圏);
    condition.setOfficeAvailability(2);
    condition.setCompaniesTierNumber(0);
    condition.setEmploymentType("個人事業主");

    PersonDetailFilterCondition.WhereClauseAndParams result = condition.buildWhereClause("p");

    assertEquals(
        "p.start_date >= ? AND p.unit_price >= ? AND p.unit_price <= ? AND p.age >= ? AND p.age <= ?"
            + " AND p.gender = ? AND p.nationality = ? AND p.area = ? AND p.office_availability >= ?"
            + " AND p.organization = ?",
        result.getWhereClauseWithoutWhereKeyword());
    assertEquals(10, result.getParams().size());
  }
}
