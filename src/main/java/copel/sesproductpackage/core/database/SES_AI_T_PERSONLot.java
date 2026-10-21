package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.SesAiTableLotBase;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【Entityクラス】 要員情報(SES_AI_T_PERSON)テーブルのLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_PERSONLot extends SesAiTableLotBase<SES_AI_T_PERSON> {

  /** 複合条件全文検索用 SELECT 接頭辞（末尾に WHERE を含む）. */
  private static final String SELECT_RAW_CONTENT_FOR_FULLTEXT =
      "SELECT person_id, from_group, from_id, from_name, mail_subject, raw_content, "
          + "content_summary, file_id, unit_price, name, age, gender, nationality, "
          + "start_date, place, area, office_availability, organization, experiences, "
          + "ng_requirements, other_requirements, url, register_date, register_user, ttl, "
          + "tenant_id FROM SES_AI_T_PERSON WHERE ";

  /** 検索SQL(指定時間以降検索). */
  private static final String SELECT_SQL_BY_REGISTER_DATE =
      "SELECT person_id, from_group, from_id, from_name, mail_subject, raw_content, "
          + "content_summary, file_id, unit_price, name, age, gender, nationality, "
          + "start_date, place, area, office_availability, organization, experiences, "
          + "ng_requirements, other_requirements, url, register_date, register_user, ttl, "
          + "tenant_id FROM SES_AI_T_PERSON WHERE register_date >= ?";

  /** 全件検索SQL. */
  private static final String SELECT_ALL_SQL =
      "SELECT person_id, from_group, from_id, from_name, mail_subject, raw_content, "
          + "content_summary, file_id, unit_price, name, age, gender, nationality, "
          + "start_date, place, area, office_availability, organization, experiences, "
          + "ng_requirements, other_requirements, url, register_date, register_user, ttl, "
          + "tenant_id FROM SES_AI_T_PERSON ORDER BY register_date DESC";

  /** 類似度閾値ベクトル検索用SQL（ページング用・LIMIT/OFFSET除外）. */
  private static final String RETRIEVE_WITH_THRESHOLD_SQL_WITHOUT_LIMIT =
      "SELECT person_id, from_group, from_id, from_name, mail_subject, raw_content, "
          + "content_summary, file_id, unit_price, name, age, gender, nationality, start_date, "
          + "place, area, office_availability, organization, experiences, ng_requirements, "
          + "other_requirements, url, register_date, register_user, ttl, "
          + "vector_data <=> "
          + "?::vector AS distance, tenant_id FROM SES_AI_T_PERSON WHERE 1 - "
          + "(vector_data <=> ?::vector) >= ? ORDER BY distance ASC";

  /** 期限切れ要員取得SQL前半（テナントIDなし、バッチ用）. */
  private static final String SELECT_EXPIRED_PERSONS_NOT_IN_MATCH_WITHOUT_TENANT_PREFIX =
      "SELECT person_id, from_group, from_id, from_name, mail_subject, raw_content, "
          + "content_summary, file_id, unit_price, name, age, gender, nationality, "
          + "start_date, place, area, office_availability, organization, experiences, "
          + "ng_requirements, other_requirements, url, register_date, register_user, ttl, "
          + "tenant_id "
          + "FROM SES_AI_T_PERSON "
          + "WHERE ((ttl IS NOT NULL AND ttl < NOW()) "
          + "   OR (ttl IS NULL AND register_date IS NOT NULL AND (register_date + INTERVAL '";

  /**
   * 期限切れ要員取得SQL後半（テナントIDなし、バッチ用）.
   *
   * <p>マッチングの存在確認は {@code NOT IN} ではなく相関の {@code NOT EXISTS} にする。{@code NOT IN} は副問い合わせに NULL
   * が1件でもあると 全件が不一致になり、期限切れの要員を1件も取得できなくなるため。同じテナントのマッチングだけを除外する。
   */
  private static final String SELECT_EXPIRED_PERSONS_NOT_IN_MATCH_WITHOUT_TENANT_SUFFIX =
      " days') < NOW()) OR (register_date IS NULL)) "
          + "  AND NOT EXISTS (SELECT 1 FROM SES_AI_T_MATCH m "
          + "WHERE m.person_id = SES_AI_T_PERSON.person_id "
          + "AND m.tenant_id = SES_AI_T_PERSON.tenant_id) "
          + "ORDER BY register_date ASC "
          + "LIMIT ? OFFSET ?";

  /** 全文検索（raw_content の LIKE）用 SELECT 文（WHERE句を含む）. */
  private static final String SELECT_LIKE_SQL =
      "SELECT person_id, from_group, from_id, from_name, mail_subject, raw_content, "
          + "content_summary, file_id, unit_price, name, age, gender, nationality, "
          + "start_date, place, area, office_availability, organization, experiences, "
          + "ng_requirements, other_requirements, url, register_date, register_user, ttl, "
          + "tenant_id FROM SES_AI_T_PERSON WHERE raw_content LIKE ?";

  /** コンストラクタ. */
  public SES_AI_T_PERSONLot() {
    super();
  }

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  /**
   * 引数に指定した要員IDを持つEntityを返却する.
   *
   * @param personId 要員ID
   * @return SES_AI_T_PERSON
   */
  public SES_AI_T_PERSON getEntityByPk(final String personId) {
    if (personId == null) {
      return null;
    }

    for (SES_AI_T_PERSON entity : this.entityLot) {
      if (personId.trim().equals(entity.getPersonId().trim())) {
        return entity;
      }
    }
    return null;
  }

  /**
   * LLMに最もマッチする要員の要員IDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String to要員選出用文章() {
    StringBuilder result = new StringBuilder();
    int i = 1;
    for (SES_AI_T_PERSON entity : this.entityLot) {
      result.append(i).append("人目：").append(entity.to要員選出用文章());
      i++;
    }
    return result.toString();
  }

  /**
   * 指定した時刻以降に登録されたレコードを全て取得する.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param fromDate 時刻
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void selectByRegisterDateAfter(
      final Connection connection, final String tenantId, final OriginalDateTime fromDate)
      throws SQLException {
    requireConnection(connection);
    List<SES_AI_T_PERSON> results =
        executeQuery(
            connection,
            SELECT_SQL_BY_REGISTER_DATE,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              stmt.setTimestamp(
                  paramIndex,
                  fromDate != null ? fromDate.toTimestamp() : new OriginalDateTime().toTimestamp());
              return paramIndex + 1;
            });
    this.entityLot = results;
  }

  /**
   * マッチング対象外の期限切れ要員を取得します（テナントID条件なし、バッチ用）.
   *
   * <p>⚠️ このメソッドは全テナント対象です。 バッチ処理専用。コードレビュー必須。
   *
   * @param connection DBコネクション
   * @param ttlDays TTL日数
   * @param offset オフセット
   * @param limit リミット
   * @throws SQLException SQL実行エラー
   */
  public void selectExpiredPersonsNotInMatchWithoutTenantId(
      final Connection connection, final int ttlDays, final int offset, final int limit)
      throws SQLException {
    List<SES_AI_T_PERSON> results =
        executeTtlExpiredQueryWithoutTenantFilter(
            connection,
            SELECT_EXPIRED_PERSONS_NOT_IN_MATCH_WITHOUT_TENANT_PREFIX,
            SELECT_EXPIRED_PERSONS_NOT_IN_MATCH_WITHOUT_TENANT_SUFFIX,
            ttlDays,
            offset,
            limit,
            this::mapResultSet);
    this.entityLot = results;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (SES_AI_T_PERSON entity : this.entityLot) {
      stringBuilder.append(entity.toString());
      stringBuilder.append("\n");
    }
    return stringBuilder.toString();
  }

  /**
   * 指定テナントの全要員情報を取得します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @throws SQLException DB操作エラー
   */
  @Override
  public void selectAll(Connection connection, String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_PERSON> results =
        executeQuery(
            connection,
            SELECT_ALL_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * 全レコードを取得する（WithoutTenantFilter - バッチ処理専用）.
   *
   * <p>⚠️ このメソッドは全テナント対象です。 バッチ処理専用。コードレビュー必須。
   *
   * @param connection DBコネクション
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void selectAllWithoutTenantId(Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_PERSON> results =
        executeQueryWithoutTenantFilter(
            connection, SELECT_ALL_SQL, this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * ResultSetを要員情報エンティティにマッピングします.
   *
   * @param resultSet 結果セット
   * @return マッピング済みの要員情報エンティティ
   * @throws SQLException DB操作エラー
   */
  @Override
  protected String getSelectLikeSql() {
    return SELECT_LIKE_SQL;
  }

  @Override
  protected String getThresholdSql() {
    return RETRIEVE_WITH_THRESHOLD_SQL_WITHOUT_LIMIT;
  }

  @Override
  protected String getRawContentFulltextSql() {
    return SELECT_RAW_CONTENT_FOR_FULLTEXT;
  }

  @Override
  protected SES_AI_T_PERSON mapResultSetWithDistance(ResultSet resultSet) throws SQLException {
    SES_AI_T_PERSON entity = mapResultSet(resultSet);
    entity.setDistance(resultSet.getDouble("distance"));
    return entity;
  }

  @Override
  protected SES_AI_T_PERSON mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON(resultSet.getString("tenant_id"));
    person.setPersonId(resultSet.getString("person_id"));
    person.setFromGroup(resultSet.getString("from_group"));
    person.setFromId(resultSet.getString("from_id"));
    person.setFromName(resultSet.getString("from_name"));
    person.setMailSubject(resultSet.getString("mail_subject"));
    person.setFileId(resultSet.getString("file_id"));
    person.setRawContent(resultSet.getString("raw_content"));
    person.setContentSummary(resultSet.getString("content_summary"));
    java.math.BigDecimal unitPriceValue = resultSet.getBigDecimal("unit_price");
    person.setUnitPrice(unitPriceValue == null ? Money.empty() : new Money(unitPriceValue));
    person.setName(resultSet.getString("name"));
    person.setAge(resultSet.getObject("age") == null ? null : resultSet.getInt("age"));
    String genderStr = resultSet.getString("gender");
    person.setGender(genderStr == null ? null : Gender.valueOf(genderStr));
    person.setNationality(resultSet.getString("nationality"));
    person.setStartDate(new OriginalDateTime(resultSet.getString("start_date")));
    person.setPlace(resultSet.getString("place"));
    String areaStr = resultSet.getString("area");
    person.setArea(areaStr == null ? null : Area.valueOf(areaStr));
    person.setOfficeAvailability(
        resultSet.getObject("office_availability") == null
            ? null
            : resultSet.getInt("office_availability"));
    person.setOrganization(resultSet.getString("organization"));
    person.setExperiences(resultSet.getString("experiences"));
    person.setUrl(resultSet.getString("url"));
    person.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    person.setRegisterUser(resultSet.getString("register_user"));
    person.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return person;
  }
}
