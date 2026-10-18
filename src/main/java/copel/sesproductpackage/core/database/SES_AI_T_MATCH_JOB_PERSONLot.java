package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * マッチング・案件・要員を結合したEntityのLotクラス.
 *
 * <p>SES_AI_T_MATCH、SES_AI_T_JOB、SES_AI_T_PERSONを LEFT JOIN で結合し、
 * マッチング情報と紐づく案件・要員の詳細情報を一度のクエリで取得する。 JOB/PERSONが削除済みの場合でもマッチング行は取得される（LEFT JOIN により NULL 値となる）。
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_MATCH_JOB_PERSONLot extends EntityLotBase<SES_AI_T_MATCH_JOB_PERSON> {

  /** 全件SELECT文（ページング用のORDER BY含む）. */
  private static final String SELECT_ALL_SQL =
      "SELECT m.matching_id, m.user_id, m.job_id, m.person_id, m.job_content, m.person_content, m.status_cd, m.evaluation_text, m.score, m.must_evaluation_text, m.want_evaluation_text, m.place_evaluation_text, m.office_evaluation_text, m.other_evaluation_text, m.profit, m.register_date, m.register_user, m.tenant_id, j.title AS job_title, j.unit_price AS job_unit_price, p.name AS person_name, p.age AS person_age, p.gender AS person_gender, p.unit_price AS person_unit_price FROM SES_AI_T_MATCH m LEFT JOIN SES_AI_T_JOB j ON m.job_id = j.job_id LEFT JOIN SES_AI_T_PERSON p ON m.person_id = p.person_id ORDER BY m.register_date DESC";

  /** SELECT文（WHERE句あり）. */
  private static final String SELECT_SQL =
      "SELECT m.matching_id, m.user_id, m.job_id, m.person_id, m.job_content, m.person_content, m.status_cd, m.evaluation_text, m.score, m.must_evaluation_text, m.want_evaluation_text, m.place_evaluation_text, m.office_evaluation_text, m.other_evaluation_text, m.profit, m.register_date, m.register_user, m.tenant_id, j.title AS job_title, j.unit_price AS job_unit_price, p.name AS person_name, p.age AS person_age, p.gender AS person_gender, p.unit_price AS person_unit_price FROM SES_AI_T_MATCH m LEFT JOIN SES_AI_T_JOB j ON m.job_id = j.job_id LEFT JOIN SES_AI_T_PERSON p ON m.person_id = p.person_id WHERE ";

  /**
   * ページング付き条件検索用のSELECT文（WHERE句を含まない）.
   *
   * <p>{@link copel.sesproductpackage.core.database.base.EntityLotBase#selectByQueryPaged}
   * はbaseSqlに「WHERE」が含まれるかどうかで条件追加の挙動を変えるため、 末尾に「WHERE 」を含む{@link #SELECT_SQL}をそのまま渡すと 「WHERE AND
   * ...」という不正なSQLが生成されてしまう。 そのためページング条件検索専用に、WHERE句を含まない版を別途用意する。
   */
  private static final String SELECT_BASE_SQL_FOR_PAGED_QUERY =
      "SELECT m.matching_id, m.user_id, m.job_id, m.person_id, m.job_content, m.person_content, m.status_cd, m.evaluation_text, m.score, m.must_evaluation_text, m.want_evaluation_text, m.place_evaluation_text, m.office_evaluation_text, m.other_evaluation_text, m.profit, m.register_date, m.register_user, m.tenant_id, j.title AS job_title, j.unit_price AS job_unit_price, p.name AS person_name, p.age AS person_age, p.gender AS person_gender, p.unit_price AS person_unit_price FROM SES_AI_T_MATCH m LEFT JOIN SES_AI_T_JOB j ON m.job_id = j.job_id LEFT JOIN SES_AI_T_PERSON p ON m.person_id = p.person_id ";

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  protected String getSelectSql() {
    return SELECT_SQL;
  }

  @Override
  public void selectAll(Connection connection, String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_MATCH_JOB_PERSON> results =
        executeQuery(
            connection,
            SELECT_ALL_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * 案件IDが一致するレコードを指定件数取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param jobId 案件ID
   * @param page ページ番号
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void selectByJobIdPaged(
      Connection connection, String tenantId, String jobId, int page, int size)
      throws SQLException {
    if (connection == null || jobId == null) {
      return;
    }
    java.util.Map<String, String> query = new java.util.HashMap<>();
    query.put("m.job_id", jobId);
    this.selectByQueryPaged(
        connection, tenantId, SELECT_BASE_SQL_FOR_PAGED_QUERY, query, true, page, size);
  }

  /**
   * 要員IDが一致するレコードを指定件数取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param personId 要員ID
   * @param page ページ番号
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void selectByPersonIdPaged(
      Connection connection, String tenantId, String personId, int page, int size)
      throws SQLException {
    if (connection == null || personId == null) {
      return;
    }
    java.util.Map<String, String> query = new java.util.HashMap<>();
    query.put("m.person_id", personId);
    this.selectByQueryPaged(
        connection, tenantId, SELECT_BASE_SQL_FOR_PAGED_QUERY, query, true, page, size);
  }

  @Override
  protected SES_AI_T_MATCH_JOB_PERSON mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_MATCH_JOB_PERSON entity =
        new SES_AI_T_MATCH_JOB_PERSON(resultSet.getString("tenant_id"));

    // SES_AI_T_MATCH のフィールド
    entity.setMatchingId(resultSet.getString("matching_id"));
    entity.setUserId(resultSet.getString("user_id"));
    entity.setJobId(resultSet.getString("job_id"));
    entity.setPersonId(resultSet.getString("person_id"));
    entity.setJobContent(resultSet.getString("job_content"));
    entity.setPersonContent(resultSet.getString("person_content"));
    entity.setStatus(MatchingStatus.getEnum(resultSet.getString("status_cd")));
    entity.setEvaluationText(resultSet.getString("evaluation_text"));
    entity.setScore(resultSet.getObject("score") != null ? resultSet.getInt("score") : null);
    entity.setMustEvaluationText(resultSet.getString("must_evaluation_text"));
    entity.setWantEvaluationText(resultSet.getString("want_evaluation_text"));
    entity.setPlaceEvaluationText(resultSet.getString("place_evaluation_text"));
    entity.setOfficeEvaluationText(resultSet.getString("office_evaluation_text"));
    entity.setOtherEvaluationText(resultSet.getString("other_evaluation_text"));
    BigDecimal profitVal = resultSet.getBigDecimal("profit");
    entity.setProfit(profitVal == null ? Money.empty() : new Money(profitVal));
    entity.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    entity.setRegisterUser(resultSet.getString("register_user"));

    // SES_AI_T_JOB のフィールド（LEFT JOIN による NULL 安全処理）
    entity.setJobTitle(resultSet.getString("job_title"));
    BigDecimal jobUnitPriceVal = resultSet.getBigDecimal("job_unit_price");
    entity.setJobUnitPrice(jobUnitPriceVal == null ? Money.empty() : new Money(jobUnitPriceVal));

    // SES_AI_T_PERSON のフィールド（LEFT JOIN による NULL 安全処理）
    entity.setPersonName(resultSet.getString("person_name"));
    entity.setPersonAge(
        resultSet.getObject("person_age") != null ? resultSet.getInt("person_age") : null);
    String personGenderStr = resultSet.getString("person_gender");
    entity.setPersonGender(personGenderStr == null ? null : Gender.valueOf(personGenderStr));
    BigDecimal personUnitPriceVal = resultSet.getBigDecimal("person_unit_price");
    entity.setPersonUnitPrice(
        personUnitPriceVal == null ? Money.empty() : new Money(personUnitPriceVal));

    return entity;
  }
}
