package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ウォッチ・案件・要員を結合したEntityのLotクラス.
 *
 * <p>SES_AI_T_WATCH、SES_AI_T_JOB、SES_AI_T_PERSONを LEFT JOIN で結合し、 ウォッチ情報と紐づく案件・要員の詳細情報を一度のクエリで取得する。
 * JOB/PERSONが削除済みの場合でもウォッチ行は取得される（LEFT JOIN により NULL 値となる）。 これにより、従来の N+1 クエリ問題を解決し、パフォーマンスを向上させる。
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_WATCH_JOB_PERSONLot extends EntityLotBase<SES_AI_T_WATCH_JOB_PERSON> {

  /**
   * ページング付き条件検索用のSELECT文（WHERE句を含まない）.
   *
   * <p>{@link copel.sesproductpackage.core.database.base.EntityLotBase#selectByQueryPaged}
   * はbaseSqlに「WHERE」が含まれるかどうかで条件追加の挙動を変えるため、 WHERE句を含まない版を用意する。 WATCH (w) を JOB (j) と PERSON (p) に
   * LEFT JOIN し、 target_type が 'JOB' の場合は j.job_id でマッチし、 target_type が 'PERSON' の場合は p.person_id
   * でマッチするようにしている。 register_date で降順ソート（最新のウォッチが最初に表示される）。
   */
  private static final String SELECT_BASE_SQL_FOR_PAGED_QUERY =
      "SELECT w.user_id, w.target_id, w.target_type, w.memo, w.register_date, w.register_user, w.ttl, w.tenant_id, "
          + "j.title AS job_title, j.from_name AS job_from_name, j.content_summary AS job_content_summary, "
          + "j.raw_content AS job_raw_content, "
          + "j.start_date AS job_start_date, j.office_requirements AS job_office_requirements, "
          + "j.unit_price AS job_unit_price, j.place AS job_place, j.area AS job_area, "
          + "p.name AS person_name, p.from_name AS person_from_name, p.content_summary AS person_content_summary, "
          + "p.raw_content AS person_raw_content, "
          + "p.age AS person_age, p.gender AS person_gender, p.start_date AS person_start_date, "
          + "p.office_availability AS person_office_availability, p.unit_price AS person_unit_price, "
          + "p.place AS person_place, p.area AS person_area "
          + "FROM SES_AI_T_WATCH w "
          + "LEFT JOIN SES_AI_T_JOB j ON w.target_id = j.job_id AND w.target_type = 'JOB' "
          + "LEFT JOIN SES_AI_T_PERSON p ON w.target_id = p.person_id AND w.target_type = 'PERSON' "
          + "ORDER BY w.register_date DESC ";

  @Override
  protected String getSelectAllSql() {
    throw new UnsupportedOperationException(
        "SES_AI_T_WATCH_JOB_PERSONLot は selectAll をサポートしていません。selectByUserIdPaged を使用してください。");
  }

  @Override
  protected String getSelectSql() {
    throw new UnsupportedOperationException(
        "SES_AI_T_WATCH_JOB_PERSONLot は selectSql をサポートしていません。selectByUserIdPaged を使用してください。");
  }

  @Override
  public void selectAll(Connection connection, String tenantId) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_WATCH_JOB_PERSONLot は selectAll をサポートしていません。selectByUserIdPaged を使用してください。");
  }

  /**
   * ユーザーIDが一致するウォッチレコードをページング付きで取得し、このLotに格納します.
   *
   * <p>SES_AI_T_WATCH、SES_AI_T_JOB、SES_AI_T_PERSONを LEFT JOIN で結合し、
   * ウォッチ情報と紐づく案件・要員の詳細情報を1クエリで取得します。 JOB/PERSONが削除済みの場合でもウォッチ行は取得され、対応フィールドは NULL となります。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID（テナント隔離を厳格に実施）
   * @param userId ユーザーID（フィルター条件）
   * @param page ページ番号（1から開始）
   * @param size 1ページあたりの件数
   * @throws SQLException SQL実行エラーの場合
   */
  public void selectByUserIdPaged(
      Connection connection, String tenantId, String userId, int page, int size)
      throws SQLException {
    if (connection == null || userId == null) {
      return;
    }
    java.util.Map<String, String> query = new java.util.HashMap<>();
    query.put("w.user_id", userId);
    this.selectByQueryPaged(
        connection, tenantId, SELECT_BASE_SQL_FOR_PAGED_QUERY, query, true, page, size);
  }

  @Override
  protected SES_AI_T_WATCH_JOB_PERSON mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_WATCH_JOB_PERSON entity =
        new SES_AI_T_WATCH_JOB_PERSON(resultSet.getString("tenant_id"));

    // SES_AI_T_WATCH のフィールド
    entity.setUserId(resultSet.getString("user_id"));
    entity.setTargetId(resultSet.getString("target_id"));
    String targetTypeStr = resultSet.getString("target_type");
    entity.setTargetType(
        targetTypeStr == null ? null : SES_AI_T_WATCH.TargetType.getEnumByName(targetTypeStr));
    entity.setMemo(resultSet.getString("memo"));
    String registerDateStr = resultSet.getString("register_date");
    entity.setRegisterDate(registerDateStr == null ? null : new OriginalDateTime(registerDateStr));
    entity.setRegisterUser(resultSet.getString("register_user"));
    String ttlStr = resultSet.getString("ttl");
    entity.setTtl(ttlStr == null ? null : new OriginalDateTime(ttlStr));

    // SES_AI_T_JOB のフィールド（LEFT JOIN による NULL 安全処理）
    entity.setJobTitle(resultSet.getString("job_title"));
    entity.setJobFromName(resultSet.getString("job_from_name"));
    entity.setJobContentSummary(resultSet.getString("job_content_summary"));
    entity.setJobRawContent(resultSet.getString("job_raw_content"));
    String jobStartDateStr = resultSet.getString("job_start_date");
    entity.setJobStartDate(jobStartDateStr == null ? null : new OriginalDateTime(jobStartDateStr));
    entity.setJobOfficeRequirements(
        resultSet.getObject("job_office_requirements") != null
            ? resultSet.getInt("job_office_requirements")
            : null);
    BigDecimal jobUnitPriceVal = resultSet.getBigDecimal("job_unit_price");
    entity.setJobUnitPrice(jobUnitPriceVal == null ? Money.empty() : new Money(jobUnitPriceVal));
    entity.setJobPlace(resultSet.getString("job_place"));
    entity.setJobArea(resultSet.getString("job_area"));

    // SES_AI_T_PERSON のフィールド（LEFT JOIN による NULL 安全処理）
    entity.setPersonName(resultSet.getString("person_name"));
    entity.setPersonFromName(resultSet.getString("person_from_name"));
    entity.setPersonContentSummary(resultSet.getString("person_content_summary"));
    entity.setPersonRawContent(resultSet.getString("person_raw_content"));
    entity.setPersonAge(
        resultSet.getObject("person_age") != null ? resultSet.getInt("person_age") : null);
    String personGenderStr = resultSet.getString("person_gender");
    entity.setPersonGender(personGenderStr == null ? null : Gender.valueOf(personGenderStr));
    String personStartDateStr = resultSet.getString("person_start_date");
    entity.setPersonStartDate(
        personStartDateStr == null ? null : new OriginalDateTime(personStartDateStr));
    entity.setPersonOfficeAvailability(
        resultSet.getObject("person_office_availability") != null
            ? resultSet.getInt("person_office_availability")
            : null);
    BigDecimal personUnitPriceVal = resultSet.getBigDecimal("person_unit_price");
    entity.setPersonUnitPrice(
        personUnitPriceVal == null ? Money.empty() : new Money(personUnitPriceVal));
    entity.setPersonPlace(resultSet.getString("person_place"));
    entity.setPersonArea(resultSet.getString("person_area"));

    return entity;
  }
}
