package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.FulltextConditionsWhereClause;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.LogicalOperators;
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
public class SES_AI_T_JOBLot extends EntityLotBase<SES_AI_T_JOB> {
  /** 全文検索SQL. */
  private static final String SELECT_LIKE_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, vector_data, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, tenant_id FROM SES_AI_T_JOB WHERE raw_content LIKE ?";

  /** 複合条件全文検索用 SELECT 接頭辞（末尾に WHERE を含む）. */
  private static final String SELECT_RAW_CONTENT_FOR_FULLTEXT =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, vector_data, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, tenant_id FROM SES_AI_T_JOB WHERE ";

  /** 検索SQL. */
  private static final String SELECT_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, vector_data, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, tenant_id FROM SES_AI_T_JOB WHERE ";

  /** 全件検索SQL. */
  private static final String SELECT_ALL_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, tenant_id FROM SES_AI_T_JOB ORDER BY register_date DESC";

  /** ベクトル検索のカウント用SQL. */
  private static final String COUNT_SQL_FOR_RETRIEVE = "SELECT COUNT(*) FROM SES_AI_T_JOB";

  /** 類似度閾値ベクトル検索用SQL（ページング用・LIMIT/OFFSET除外）. */
  private static final String RETRIEVE_WITH_THRESHOLD_SQL_WITHOUT_LIMIT =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, vector_data <=> ?::vector AS distance, tenant_id FROM SES_AI_T_JOB WHERE 1 - (vector_data <=> ?::vector) >= ? ORDER BY distance ASC";

  /** ベクトル検索＋価格・開始日フィルタ用SQL（ページング用・LIMIT/OFFSET除外）. */
  private static final String RETRIEVE_WITH_FILTER_2_VALUES_SQL_WITHOUT_LIMIT =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, vector_data <=> ?::vector AS distance, tenant_id FROM SES_AI_T_JOB WHERE unit_price >= ? AND start_date <= ? AND 1 - (vector_data <=> ?::vector) >= ? ORDER BY distance ASC";

  /** ベクトル検索＋価格・開始日・オフィス要件・エリアフィルタ用SQL（ページング用・LIMIT/OFFSET除外）. */
  private static final String RETRIEVE_WITH_FILTER_4_VALUES_SQL_WITHOUT_LIMIT =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, vector_data <=> ?::vector AS distance, tenant_id FROM SES_AI_T_JOB WHERE unit_price >= ? AND start_date <= ? AND office_requirements <= ? AND area = ? AND 1 - (vector_data <=> ?::vector) >= ? ORDER BY distance ASC";

  /** 全文検索＋価格・開始日フィルタ用SQL. */
  private static final String SELECT_RAW_CONTENT_WITH_FILTER_2_VALUES_FOR_FULLTEXT =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, tenant_id FROM SES_AI_T_JOB WHERE unit_price >= ? AND start_date <= ? AND raw_content LIKE ?";

  /** 全文検索＋価格・開始日・オフィス要件・エリアフィルタ用SQL. */
  private static final String SELECT_RAW_CONTENT_WITH_FILTER_4_VALUES_FOR_FULLTEXT =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl, tenant_id FROM SES_AI_T_JOB WHERE unit_price >= ? AND start_date <= ? AND office_requirements <= ? AND area = ? AND raw_content LIKE ?";

  /** コンストラクタ. */
  public SES_AI_T_JOBLot() {
    super();
  }

  @Override
  protected String getSelectSql() {
    return SELECT_SQL;
  }

  @Override
  protected String getSelectLikeSql() {
    return SELECT_LIKE_SQL;
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
   * @throws SQLException
   */
  public void selectAllWithoutTenantFilter(Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_JOB> results =
        executeQueryWithoutTenantFilter(
            connection, SELECT_ALL_SQL, this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * ベクトル検索を実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieve(Connection connection, String tenantId, Vector query, int limit)
      throws SQLException {
    this.retrievePaged(connection, tenantId, query, 1, limit);
  }

  /**
   * ベクトル検索をページングで実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrievePaged(
      Connection connection, String tenantId, Vector query, int page, int size)
      throws SQLException {
    if (connection == null || query == null) {
      return;
    }

    executeVectorPagedQuery(
        connection,
        COUNT_SQL_FOR_RETRIEVE,
        tenantId,
        query.toString(),
        0.0,
        page,
        size,
        rs -> {
          SES_AI_T_JOB entity = mapResultSet(rs);
          try {
            entity.setDistance(rs.getDouble("distance"));
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
          return entity;
        },
        (stmt, paramIndex, vectorValue, similarityThreshold) -> {
          stmt.setString(paramIndex, vectorValue);
          return paramIndex + 1;
        });
  }

  /**
   * 類似度閾値を用いてベクトル検索を実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @throws SQLException
   */
  public void retrieve(Connection connection, String tenantId, Vector query) throws SQLException {
    this.retrieveWithThreshold(connection, tenantId, query, 0.0, 5);
  }

  /**
   * 類似度閾値を用いてベクトル検索を実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieveWithThreshold(
      Connection connection, String tenantId, Vector query, double similarityThreshold, int limit)
      throws SQLException {
    this.retrievePagedWithThreshold(connection, tenantId, query, similarityThreshold, 1, limit);
  }

  /**
   * 類似度閾値を用いてベクトル検索をページングで実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrievePagedWithThreshold(
      Connection connection,
      String tenantId,
      Vector query,
      double similarityThreshold,
      int page,
      int size)
      throws SQLException {
    if (connection == null || query == null) {
      return;
    }

    executeVectorPagedQuery(
        connection,
        RETRIEVE_WITH_THRESHOLD_SQL_WITHOUT_LIMIT,
        tenantId,
        query.toString(),
        similarityThreshold,
        page,
        size,
        rs -> {
          SES_AI_T_JOB entity = mapResultSet(rs);
          try {
            entity.setDistance(rs.getDouble("distance"));
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
          return entity;
        },
        (stmt, paramIndex, vectorValue, threshold) -> {
          stmt.setString(paramIndex, vectorValue);
          stmt.setString(paramIndex + 1, vectorValue);
          stmt.setDouble(paramIndex + 2, threshold);
          return paramIndex + 3;
        });
  }

  /**
   * raw_contentカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索条件Map
   * @throws SQLException
   */
  public void searchByRawContent(
      final Connection connection, final String tenantId, final String query) throws SQLException {
    this.searchByField(connection, tenantId, "raw_content", query);
  }

  /**
   * raw_contentカラムで全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索文字列
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByRawContentPaged(
      final Connection connection,
      final String tenantId,
      final String query,
      final int page,
      final int size)
      throws SQLException {
    this.searchByFieldPaged(connection, tenantId, "raw_content", query, page, size);
  }

  /**
   * raw_contentカラムに対して複数条件で全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @throws SQLException
   */
  public void searchByRawContent(
      final Connection connection,
      final String tenantId,
      final String firstLikeQuery,
      final List<LogicalOperators> query)
      throws SQLException {
    this.searchByField(connection, tenantId, "raw_content", firstLikeQuery, query);
  }

  /**
   * raw_contentカラムに対して複数条件で全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByRawContentPaged(
      final Connection connection,
      final String tenantId,
      final String firstLikeQuery,
      final List<LogicalOperators> query,
      final int page,
      final int size)
      throws SQLException {
    this.searchByFieldPaged(connection, tenantId, "raw_content", firstLikeQuery, query, page, size);
  }

  /**
   * raw_contentカラムで価格と開始日の条件を付けて全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索文字列
   * @param price 最低価格（単価がこの値以上のもの）
   * @param startDate 最遅開始日（開始日がこの日付以前のもの）
   * @throws SQLException
   */
  public void searchByRawContentWithFilter(
      final Connection connection,
      final String tenantId,
      final String query,
      final Money price,
      final OriginalDateTime startDate)
      throws SQLException {
    this.searchByRawContentWithFilterPaged(connection, tenantId, query, price, startDate, 1, Integer.MAX_VALUE);
  }

  /**
   * raw_contentカラムで価格と開始日の条件を付けてページング全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索文字列
   * @param price 最低価格（単価がこの値以上のもの）
   * @param startDate 最遅開始日（開始日がこの日付以前のもの）
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByRawContentWithFilterPaged(
      final Connection connection,
      final String tenantId,
      final String query,
      final Money price,
      final OriginalDateTime startDate,
      final int page,
      final int size)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || query == null) {
      return;
    }

    List<SES_AI_T_JOB> results =
        executeQuery(
            connection,
            SELECT_RAW_CONTENT_WITH_FILTER_2_VALUES_FOR_FULLTEXT,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              stmt.setBigDecimal(paramIndex, price.getValue());
              stmt.setTimestamp(paramIndex + 1, startDate.toTimestamp());
              stmt.setString(paramIndex + 2, "%" + query + "%");
              return paramIndex + 3;
            });
    this.entityLot.addAll(results);
  }

  /**
   * raw_contentカラムで価格・開始日・オフィス要件・エリアの条件を付けて全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索文字列
   * @param price 最低価格（単価がこの値以上のもの）
   * @param startDate 最遅開始日（開始日がこの日付以前のもの）
   * @param officeRequirements 最大オフィス要件（オフィス要件がこの値以下のもの）
   * @param area 対象エリア（エリアがこの値と一致するもの）
   * @throws SQLException
   */
  public void searchByRawContentWithFilter(
      final Connection connection,
      final String tenantId,
      final String query,
      final Money price,
      final OriginalDateTime startDate,
      final int officeRequirements,
      final Area area)
      throws SQLException {
    this.searchByRawContentWithFilterPaged(
        connection, tenantId, query, price, startDate, officeRequirements, area, 1, Integer.MAX_VALUE);
  }

  /**
   * raw_contentカラムで価格・開始日・オフィス要件・エリアの条件を付けてページング全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索文字列
   * @param price 最低価格（単価がこの値以上のもの）
   * @param startDate 最遅開始日（開始日がこの日付以前のもの）
   * @param officeRequirements 最大オフィス要件（オフィス要件がこの値以下のもの）
   * @param area 対象エリア（エリアがこの値と一致するもの）
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByRawContentWithFilterPaged(
      final Connection connection,
      final String tenantId,
      final String query,
      final Money price,
      final OriginalDateTime startDate,
      final int officeRequirements,
      final Area area,
      final int page,
      final int size)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || query == null || area == null) {
      return;
    }

    List<SES_AI_T_JOB> results =
        executeQuery(
            connection,
            SELECT_RAW_CONTENT_WITH_FILTER_4_VALUES_FOR_FULLTEXT,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              stmt.setBigDecimal(paramIndex, price.getValue());
              stmt.setTimestamp(paramIndex + 1, startDate.toTimestamp());
              stmt.setInt(paramIndex + 2, officeRequirements);
              stmt.setString(paramIndex + 3, area.name());
              stmt.setString(paramIndex + 4, "%" + query + "%");
              return paramIndex + 5;
            });
    this.entityLot.addAll(results);
  }

  /**
   * raw_content に対して複合条件（AND/OR/NOT）で全文検索をページング実行します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param conditions API の conditions 配列と同一の論理
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByRawContentPaged(
      final Connection connection,
      final String tenantId,
      final List<FulltextCondition> conditions,
      final int page,
      final int size)
      throws SQLException {
    FulltextConditionsWhereClause.Built built =
        FulltextConditionsWhereClause.build("raw_content", conditions);
    this.selectByDynamicWherePaged(
        connection,
        tenantId,
        SELECT_RAW_CONTENT_FOR_FULLTEXT,
        built.getWhereClauseWithoutWhereKeyword(),
        built.getLikeParams(),
        page,
        size);
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
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieveWithFilter(
      Connection connection,
      String tenantId,
      Vector query,
      Money price,
      OriginalDateTime startDate,
      int limit)
      throws SQLException {
    this.retrieveWithFilterPaged(connection, tenantId, query, price, startDate, 1, limit);
  }

  /**
   * 価格と開始日でフィルタされたベクトル検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param price 最低価格（単価がこの値以上のもの）
   * @param startDate 最遅開始日（開始日がこの日付以前のもの）
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrieveWithFilterPaged(
      Connection connection,
      String tenantId,
      Vector query,
      Money price,
      OriginalDateTime startDate,
      int page,
      int size)
      throws SQLException {
    if (connection == null || query == null) {
      return;
    }

    executeVectorPagedQuery(
        connection,
        RETRIEVE_WITH_FILTER_2_VALUES_SQL_WITHOUT_LIMIT,
        tenantId,
        query.toString(),
        0.0,
        page,
        size,
        rs -> {
          SES_AI_T_JOB entity = mapResultSet(rs);
          try {
            entity.setDistance(rs.getDouble("distance"));
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
          return entity;
        },
        (stmt, paramIndex, vectorValue, similarityThreshold) -> {
          stmt.setString(paramIndex, vectorValue);
          stmt.setBigDecimal(paramIndex + 1, price.getValue());
          stmt.setTimestamp(paramIndex + 2, startDate.toTimestamp());
          stmt.setString(paramIndex + 3, vectorValue);
          return paramIndex + 4;
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
   * @param area 対象エリア（エリアがこの値と一致するもの）
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieveWithFilter(
      Connection connection,
      String tenantId,
      Vector query,
      Money price,
      OriginalDateTime startDate,
      int officeAvailability,
      Area area,
      int limit)
      throws SQLException {
    this.retrieveWithFilterPaged(
        connection, tenantId, query, price, startDate, officeAvailability, area, 1, limit);
  }

  /**
   * 価格・開始日・オフィス要件・エリアでフィルタされたベクトル検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param price 最低価格（単価がこの値以上のもの）
   * @param startDate 最遅開始日（開始日がこの日付以前のもの）
   * @param officeAvailability 最大オフィス要件（オフィス要件がこの値以下のもの）
   * @param area 対象エリア（エリアがこの値と一致するもの）
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrieveWithFilterPaged(
      Connection connection,
      String tenantId,
      Vector query,
      Money price,
      OriginalDateTime startDate,
      int officeAvailability,
      Area area,
      int page,
      int size)
      throws SQLException {
    if (connection == null || query == null || area == null) {
      return;
    }

    executeVectorPagedQuery(
        connection,
        RETRIEVE_WITH_FILTER_4_VALUES_SQL_WITHOUT_LIMIT,
        tenantId,
        query.toString(),
        0.0,
        page,
        size,
        rs -> {
          SES_AI_T_JOB entity = mapResultSet(rs);
          try {
            entity.setDistance(rs.getDouble("distance"));
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
          return entity;
        },
        (stmt, paramIndex, vectorValue, similarityThreshold) -> {
          stmt.setString(paramIndex, vectorValue);
          stmt.setBigDecimal(paramIndex + 1, price.getValue());
          stmt.setTimestamp(paramIndex + 2, startDate.toTimestamp());
          stmt.setInt(paramIndex + 3, officeAvailability);
          stmt.setString(paramIndex + 4, area.name());
          stmt.setString(paramIndex + 5, vectorValue);
          return paramIndex + 6;
        });
  }

  /**
   * ResultSetを案件情報エンティティにマッピングします.
   *
   * @param resultSet 結果セット
   * @return マッピング済みの案件情報エンティティ
   * @throws SQLException DB操作エラー
   */
  @Override
  protected SES_AI_T_JOB mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_T_JOB sesAiTJob = new SES_AI_T_JOB(tenantId);
    sesAiTJob.setJobId(resultSet.getString("job_id"));
    sesAiTJob.setFromGroup(resultSet.getString("from_group"));
    sesAiTJob.setFromId(resultSet.getString("from_id"));
    sesAiTJob.setFromName(resultSet.getString("from_name"));
    sesAiTJob.setRawContent(resultSet.getString("raw_content"));
    sesAiTJob.setContentSummary(resultSet.getString("content_summary"));
    java.math.BigDecimal unitPriceValue = resultSet.getBigDecimal("unit_price");
    sesAiTJob.setUnitPrice(unitPriceValue == null ? Money.empty() : new Money(unitPriceValue));
    sesAiTJob.setTitle(resultSet.getString("title"));
    sesAiTJob.setOverview(resultSet.getString("overview"));
    sesAiTJob.setMustSkills(resultSet.getString("must_skills"));
    sesAiTJob.setWantSkills(resultSet.getString("want_skills"));
    sesAiTJob.setStartDate(new OriginalDateTime(resultSet.getString("start_date")));
    sesAiTJob.setPlace(resultSet.getString("place"));
    String areaStr = resultSet.getString("area");
    sesAiTJob.setArea(areaStr == null ? null : Area.valueOf(areaStr));
    sesAiTJob.setOfficeRequirements(resultSet.getObject("office_requirements") == null ? null : resultSet.getInt("office_requirements"));
    sesAiTJob.setOtherRequirements(resultSet.getString("other_requirements"));
    sesAiTJob.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiTJob.setRegisterUser(resultSet.getString("register_user"));
    sesAiTJob.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return sesAiTJob;
  }
}
