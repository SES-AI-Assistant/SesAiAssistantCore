package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.SesAiTableLotBase;
import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.FulltextConditionsWhereClause;
import copel.sesproductpackage.core.search.JobDetailFilterCondition;
import copel.sesproductpackage.core.search.WhereClauseAndParams;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【Entityクラス】 案件情報(SES_AI_T_JOB)テーブルのLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_JOBLot extends SesAiTableLotBase<SES_AI_T_JOB> {

  /** 複合条件全文検索用 SELECT 接頭辞（末尾に WHERE を含む）. */
  private static final String SELECT_RAW_CONTENT_FOR_FULLTEXT =
      "SELECT job_id, from_group, from_id, from_name, mail_subject, raw_content, content_summary, "
          + "unit_price, title, overview, must_skills, want_skills, start_date, "
          + "place, area, office_requirements, other_requirements, register_date, "
          + "register_user, ttl, tenant_id FROM SES_AI_T_JOB WHERE ";

  /** 全件検索SQL. */
  private static final String SELECT_ALL_SQL =
      "SELECT job_id, from_group, from_id, from_name, mail_subject, raw_content, content_summary, "
          + "unit_price, title, overview, must_skills, want_skills, start_date, place, area, "
          + "office_requirements, other_requirements, register_date, register_user, ttl, "
          + "tenant_id FROM SES_AI_T_JOB ORDER BY register_date DESC";

  /** 類似度閾値ベクトル検索用SQL（ページング用・LIMIT/OFFSET除外）. */
  private static final String RETRIEVE_WITH_THRESHOLD_SQL_WITHOUT_LIMIT =
      "SELECT job_id, from_group, from_id, from_name, mail_subject, raw_content, content_summary, "
          + "unit_price, title, overview, must_skills, want_skills, start_date, place, area, "
          + "office_requirements, other_requirements, register_date, register_user, ttl, "
          + "vector_data <=> ?::vector AS distance, tenant_id FROM SES_AI_T_JOB "
          + "WHERE 1 - "
          + "(vector_data <=> ?::vector) >= ? ORDER BY distance ASC";

  /** ベクトル検索＋価格・開始日フィルタ用SQL（ページング用・LIMIT/OFFSET除外）. */
  private static final String RETRIEVE_WITH_FILTER_2_VALUES_SQL_WITHOUT_LIMIT =
      "SELECT job_id, from_group, from_id, from_name, mail_subject, raw_content, content_summary, "
          + "unit_price, title, overview, must_skills, want_skills, start_date, place, area, "
          + "office_requirements, other_requirements, register_date, register_user, ttl, "
          + "vector_data <=> ?::vector AS distance, tenant_id FROM SES_AI_T_JOB WHERE "
          + "unit_price >= ? AND start_date <= ? AND 1 - (vector_data <=> ?::vector) "
          + ">= ? ORDER BY distance ASC";

  /**
   * ベクトル検索＋価格・開始日・オフィス要件・エリアフィルタ用SQL（ページング用・LIMIT/OFFSET除外）.
   *
   * <p>エリアの条件は {@code (office_requirements = 0 OR area = ?)} とし、フルリモートの案件（出社要件が 0）はエリアを問わない。
   */
  private static final String RETRIEVE_WITH_FILTER_4_VALUES_SQL_WITHOUT_LIMIT =
      "SELECT job_id, from_group, from_id, from_name, mail_subject, raw_content, content_summary, "
          + "unit_price, title, overview, must_skills, want_skills, start_date, place, area, "
          + "office_requirements, other_requirements, register_date, register_user, ttl, "
          + "vector_data <=> ?::vector AS distance, tenant_id FROM SES_AI_T_JOB WHERE "
          + "unit_price >= ? AND start_date <= ? AND office_requirements <= ? AND "
          + "(office_requirements = 0 OR area = ?) AND 1 - (vector_data <=> ?::vector) >= ? "
          + "ORDER BY distance ASC";

  /** 全文検索（raw_content の LIKE）用 SELECT 文（WHERE句を含む）. */
  private static final String SELECT_LIKE_SQL =
      "SELECT job_id, from_group, from_id, from_name, mail_subject, raw_content, content_summary, "
          + "unit_price, title, overview, must_skills, want_skills, start_date, "
          + "place, area, office_requirements, other_requirements, register_date, "
          + "register_user, ttl, tenant_id FROM SES_AI_T_JOB WHERE raw_content LIKE ?";

  /** コンストラクタ. */
  public SES_AI_T_JOBLot() {
    super();
  }

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  public void selectAll(Connection connection, String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_JOB> results =
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
  public void selectAllWithoutTenantFilter(Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_JOB> results =
        executeQueryWithoutTenantFilter(
            connection, SELECT_ALL_SQL, this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * 引数に指定した案件IDを持つEntityを返却する.
   *
   * @param jobId 案件ID
   * @return SES_AI_T_JOB
   */
  public SES_AI_T_JOB getEntityByPk(final String jobId) {
    if (jobId == null) {
      return null;
    }

    for (SES_AI_T_JOB entity : this.entityLot) {
      if (jobId.trim().equals(entity.getJobId().trim())) {
        return entity;
      }
    }
    return null;
  }

  /**
   * LLMに最もマッチする案件の案件IDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String to案件選出用文章() {
    StringBuilder result = new StringBuilder();
    int i = 1;
    for (SES_AI_T_JOB entity : this.entityLot) {
      result.append(i).append("人目：").append(entity.to案件選出用文章());
      i++;
    }
    return result.toString();
  }

  /**
   * 価格と開始日でフィルタされたベクトル検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param price 最低価格（単価がこの値以上のもの）
   * @param startDate 最遅開始日（開始日がこの日付以前のもの）
   * @param similarityThreshold 類似度閾値(0.0～1.0)
   * @param limit 取得上限件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrieveWithFilter(
      Connection connection,
      String tenantId,
      Vector query,
      Money price,
      OriginalDateTime startDate,
      double similarityThreshold,
      int limit)
      throws SQLException {
    requireConnection(connection);
    if (query == null) {
      return;
    }

    executeVectorPagedQuery(
        connection,
        RETRIEVE_WITH_FILTER_2_VALUES_SQL_WITHOUT_LIMIT,
        tenantId,
        query.toString(),
        similarityThreshold,
        (stmt, paramIndex, vectorValue, threshold) -> {
          stmt.setBigDecimal(paramIndex, price.getValue());
          stmt.setTimestamp(paramIndex + 1, startDate.toTimestamp());
          stmt.setString(paramIndex + 2, vectorValue);
          stmt.setDouble(paramIndex + 3, threshold);
          return paramIndex + 4;
        },
        1,
        limit,
        rs -> {
          SES_AI_T_JOB entity = mapResultSet(rs);
          entity.setDistance(rs.getDouble("distance"));
          return entity;
        },
        (stmt, paramIndex, vectorValue, threshold) -> {
          stmt.setString(paramIndex, vectorValue);
          stmt.setBigDecimal(paramIndex + 1, price.getValue());
          stmt.setTimestamp(paramIndex + 2, startDate.toTimestamp());
          stmt.setString(paramIndex + 3, vectorValue);
          stmt.setDouble(paramIndex + 4, threshold);
          return paramIndex + 5;
        });
  }

  /**
   * 価格・開始日・オフィス要件・エリアでフィルタされたベクトル検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param price 最低価格（単価がこの値以上のもの）
   * @param startDate 最遅開始日（開始日がこの日付以前のもの）
   * @param officeAvailability 最大オフィス要件（オフィス要件がこの値以下のもの）
   * @param area 要員のエリア。案件の出社要件が 0（フルリモート）の案件はエリアを問わない。それ以外は案件のエリアがこの値と一致するもの。null
   *     の場合は、フルリモートの案件だけが対象になる
   * @param similarityThreshold 類似度閾値(0.0～1.0)
   * @param limit 取得上限件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrieveWithFilter(
      Connection connection,
      String tenantId,
      Vector query,
      Money price,
      OriginalDateTime startDate,
      int officeAvailability,
      Area area,
      double similarityThreshold,
      int limit)
      throws SQLException {
    requireConnection(connection);
    if (query == null) {
      return;
    }
    // エリアが null の場合は SQL の NULL を渡す。案件のエリアと一致しないため、フルリモートの案件だけが候補になる
    final String areaName = area == null ? null : area.name();

    executeVectorPagedQuery(
        connection,
        RETRIEVE_WITH_FILTER_4_VALUES_SQL_WITHOUT_LIMIT,
        tenantId,
        query.toString(),
        similarityThreshold,
        (stmt, paramIndex, vectorValue, threshold) -> {
          stmt.setBigDecimal(paramIndex, price.getValue());
          stmt.setTimestamp(paramIndex + 1, startDate.toTimestamp());
          stmt.setInt(paramIndex + 2, officeAvailability);
          stmt.setString(paramIndex + 3, areaName);
          stmt.setString(paramIndex + 4, vectorValue);
          stmt.setDouble(paramIndex + 5, threshold);
          return paramIndex + 6;
        },
        1,
        limit,
        rs -> {
          SES_AI_T_JOB entity = mapResultSet(rs);
          entity.setDistance(rs.getDouble("distance"));
          return entity;
        },
        (stmt, paramIndex, vectorValue, threshold) -> {
          stmt.setString(paramIndex, vectorValue);
          stmt.setBigDecimal(paramIndex + 1, price.getValue());
          stmt.setTimestamp(paramIndex + 2, startDate.toTimestamp());
          stmt.setInt(paramIndex + 3, officeAvailability);
          stmt.setString(paramIndex + 4, areaName);
          stmt.setString(paramIndex + 5, vectorValue);
          stmt.setDouble(paramIndex + 6, threshold);
          return paramIndex + 7;
        });
  }

  /**
   * raw_contentカラムで複合条件全文検索を詳細フィルター条件付きでページング実行し、結果をこのLotに保持します.
   *
   * <p>FulltextConditionsWhereClauseで全文検索条件を構築し、JobDetailFilterConditionで詳細フィルター条件を構築します。
   * 両方のWHERE句とパラメータをマージして実行します。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param conditions 検索条件リスト
   * @param detailFilter 詳細フィルター条件（null可）
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByJobWithDetailFilter(
      final Connection connection,
      final String tenantId,
      final List<FulltextCondition> conditions,
      final JobDetailFilterCondition detailFilter,
      final int page,
      final int size)
      throws SQLException {
    requireConnection(connection);
    if (conditions == null || conditions.isEmpty()) {
      return;
    }

    // (1) 全文検索のWHERE句とパラメータを構築
    FulltextConditionsWhereClause.Built ftBuilt =
        FulltextConditionsWhereClause.build("raw_content", conditions);

    // (2) 詳細フィルターのWHERE句とパラメータを構築
    // SELECT_RAW_CONTENT_FOR_FULLTEXT のFROM句に "j" エイリアスが存在しないため、
    // エイリアス無しでWHERE句を構築する（付与すると「missing FROM-clause entry」エラーとなる）
    WhereClauseAndParams detailFilterWhere =
        (detailFilter != null)
            ? detailFilter.buildWhereClause()
            : new JobDetailFilterCondition().buildWhereClause();

    // (3) WHERE句をマージ
    StringBuilder mergedWhere = new StringBuilder();
    mergedWhere.append(ftBuilt.getWhereClauseWithoutWhereKeyword());
    if (!detailFilterWhere.isEmpty()) {
      // 全文検索の条件は必ず括弧付きの空でない句になるため、区切りの AND を常に付ける
      mergedWhere.append(" AND ");
      mergedWhere
          .append('(')
          .append(detailFilterWhere.getWhereClauseWithoutWhereKeyword())
          .append(')');
    }

    // (4) selectByDynamicWhereWithFilterPagedを呼び出してページング検索を実行
    // 全文検索パラメータと詳細フィルターパラメータをマージしてLotに保持
    this.selectByDynamicWhereWithFilterPaged(
        connection,
        tenantId,
        SELECT_RAW_CONTENT_FOR_FULLTEXT,
        mergedWhere.toString(),
        ftBuilt.getLikeParams(),
        detailFilterWhere.getParams(),
        page,
        size);
  }

  /**
   * ベクトル検索を詳細フィルター条件付きでページング実行し、結果をこのLotに保持します.
   *
   * <p>ベクトル検索にJobDetailFilterConditionで構築された詳細フィルター条件を追加して実行します。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param detailFilter 詳細フィルター条件（null可）
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrieveByJobVectorWithDetailFilter(
      final Connection connection,
      final String tenantId,
      final Vector query,
      final double similarityThreshold,
      final JobDetailFilterCondition detailFilter,
      final int page,
      final int size)
      throws SQLException {
    requireConnection(connection);
    if (query == null) {
      return;
    }

    // (1) 詳細フィルターのWHERE句とパラメータを構築
    // ベクトル検索SQLのFROM句に "j" エイリアスが存在しないため、
    // エイリアス無しでWHERE句を構築する（付与すると「missing FROM-clause entry」エラーとなる）
    WhereClauseAndParams detailFilterWhere =
        (detailFilter != null)
            ? detailFilter.buildWhereClause()
            : new JobDetailFilterCondition().buildWhereClause();

    // (2) ベクトル検索のWHERE句を構築（詳細フィルターを追加）
    StringBuilder whereClause = new StringBuilder();
    whereClause.append("1 - (vector_data <=> ?::vector) >= ?");
    if (!detailFilterWhere.isEmpty()) {
      whereClause
          .append(" AND (")
          .append(detailFilterWhere.getWhereClauseWithoutWhereKeyword())
          .append(')');
    }

    // (3) ベクトル検索SQLを構築
    String sql =
        "SELECT job_id, from_group, from_id, from_name, mail_subject, raw_content, "
            + "content_summary, unit_price, title, overview, must_skills, want_skills, start_date, "
            + "place, area, office_requirements, other_requirements, register_date, "
            + "register_user, ttl, vector_data <=> ?::vector AS distance, tenant_id "
            + "FROM SES_AI_T_JOB "
            + "WHERE "
            + whereClause.toString()
            + " ORDER BY distance ASC";

    // (4) COUNT SQL用の部分SQL（ベクトル距離計算とフィルター条件を含む）
    String countQueryPart = "SES_AI_T_JOB WHERE " + whereClause.toString();

    // (5) executeRetrieveWithFilterPagedを呼び出してページング検索を実行
    // 詳細フィルターパラメータをメソッドに渡す
    this.executeRetrieveWithFilterPaged(
        connection,
        tenantId,
        sql,
        query,
        detailFilterWhere.getParams(),
        similarityThreshold,
        page,
        size,
        countQueryPart);
  }

  /**
   * ベクトル検索を詳細フィルター条件付きでページング実行します（動的パラメータ対応版）.
   *
   * <p>ベクトル検索に動的な詳細フィルター条件を追加して実行します。 tenant_id フィルター、PreparedStatement 実行をこのメソッドが一元的に処理します。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param sql ベクトル検索SQL
   * @param query 検索ベクトル
   * @param filterParams 詳細フィルターのパラメータリスト
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @param countQueryPart COUNT SQL用の部分SQL
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  private void executeRetrieveWithFilterPaged(
      final Connection connection,
      final String tenantId,
      final String sql,
      final Vector query,
      final List<Object> filterParams,
      final double similarityThreshold,
      final int page,
      final int size,
      final String countQueryPart)
      throws SQLException {
    this.executeVectorWithFilterPaged(
        connection,
        tenantId,
        sql + " LIMIT ? OFFSET ?",
        "SELECT COUNT(*) FROM " + countQueryPart,
        query,
        filterParams,
        similarityThreshold,
        page,
        size);
  }

  /**
   * ResultSetを案件情報エンティティにマッピングします.
   *
   * @param resultSet 結果セット
   * @return マッピング済みの案件情報エンティティ
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
  protected SES_AI_T_JOB mapResultSetWithDistance(ResultSet resultSet) throws SQLException {
    SES_AI_T_JOB entity = mapResultSet(resultSet);
    entity.setDistance(resultSet.getDouble("distance"));
    return entity;
  }

  @Override
  protected SES_AI_T_JOB mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_T_JOB job = new SES_AI_T_JOB(tenantId);
    job.setJobId(resultSet.getString("job_id"));
    job.setFromGroup(resultSet.getString("from_group"));
    job.setFromId(resultSet.getString("from_id"));
    job.setFromName(resultSet.getString("from_name"));
    job.setMailSubject(resultSet.getString("mail_subject"));
    job.setRawContent(resultSet.getString("raw_content"));
    job.setContentSummary(resultSet.getString("content_summary"));
    java.math.BigDecimal unitPriceValue = resultSet.getBigDecimal("unit_price");
    job.setUnitPrice(unitPriceValue == null ? Money.empty() : new Money(unitPriceValue));
    job.setTitle(resultSet.getString("title"));
    job.setOverview(resultSet.getString("overview"));
    job.setMustSkills(resultSet.getString("must_skills"));
    job.setWantSkills(resultSet.getString("want_skills"));
    job.setStartDate(new OriginalDateTime(resultSet.getString("start_date")));
    job.setPlace(resultSet.getString("place"));
    String areaStr = resultSet.getString("area");
    job.setArea(areaStr == null ? null : Area.valueOf(areaStr));
    job.setOfficeRequirements(
        resultSet.getObject("office_requirements") == null
            ? null
            : resultSet.getInt("office_requirements"));
    job.setOtherRequirements(resultSet.getString("other_requirements"));
    job.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    job.setRegisterUser(resultSet.getString("register_user"));
    job.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return job;
  }
}
