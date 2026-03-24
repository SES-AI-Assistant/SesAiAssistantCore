package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
  /** ベクトル検索SQL. */
  private static final String RETRIEVE_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, register_date, register_user, ttl, vector_data <=> ?::vector AS distance FROM SES_AI_T_JOB ORDER BY distance LIMIT ?";

  /** 類似度閾値を用いたベクトル検索SQL. */
  private static final String RETRIEVE_WITH_THRESHOLD_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, register_date, register_user, ttl, vector_data <=> ?::vector AS distance FROM SES_AI_T_JOB WHERE 1 - (vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  /** 全文検索SQL. */
  private static final String SELECT_LIKE_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_JOB WHERE raw_content LIKE ?";

  /** 検索SQL. */
  private static final String SELECT_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_JOB WHERE ";

  /** 全件検索SQL. */
  private static final String SELECT_ALL_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, register_date, register_user, ttl FROM SES_AI_T_JOB";

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
  public void selectAll(Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null) {
      return;
    }
    try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      while (resultSet.next()) {
        this.add(mapResultSet(resultSet));
      }
    }
  }

  /**
   * ベクトル検索を実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieve(Connection connection, Vector query, int limit) throws SQLException {
    this.retrievePaged(connection, query, 1, limit);
  }

  /**
   * ベクトル検索をページングで実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrievePaged(Connection connection, Vector query, int page, int size)
      throws SQLException {
    if (connection == null) {
      return;
    }

    // (1) 全件数を取得
    String countSql = "SELECT COUNT(*) FROM SES_AI_T_JOB";
    try (PreparedStatement preparedStatement = connection.prepareStatement(countSql);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      if (resultSet.next()) {
        this.totalCount = resultSet.getLong(1);
      }
    }
    this.pageSize = size;
    this.currentPageIndex = page;

    if (this.totalCount == 0) {
      return;
    }

    // (2) ページング用ベクトル検索
    String sql = RETRIEVE_SQL + " OFFSET ?";
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, query == null ? null : query.toString());
      preparedStatement.setInt(2, size);
      preparedStatement.setInt(3, (page - 1) * size);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        this.entityLot = new ArrayList<>();
        while (resultSet.next()) {
          SES_AI_T_JOB sesAiTJob = mapResultSet(resultSet);
          sesAiTJob.setDistance(resultSet.getDouble("distance"));
          this.entityLot.add(sesAiTJob);
        }
      }
    }
  }

  /**
   * 類似度閾値を用いてベクトル検索を実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @throws SQLException
   */
  public void retrieve(Connection connection, Vector query) throws SQLException {
    this.retrieveWithThreshold(connection, query, 0.0, 5);
  }

  /**
   * 類似度閾値を用いてベクトル検索を実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieveWithThreshold(Connection connection, Vector query, double similarityThreshold, int limit) throws SQLException {
    this.retrievePagedWithThreshold(connection, query, similarityThreshold, 1, limit);
  }

  /**
   * 類似度閾値を用いてベクトル検索をページングで実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrievePagedWithThreshold(Connection connection, Vector query, double similarityThreshold, int page, int size)
      throws SQLException {
    if (connection == null || query == null) {
      return;
    }

    // (1) 全件数を取得
    String countSql = "SELECT COUNT(*) FROM SES_AI_T_JOB WHERE 1 - (vector_data <=> ?::vector) >= ?";
    try (PreparedStatement preparedStatement = connection.prepareStatement(countSql)) {
      preparedStatement.setString(1, query.toString());
      preparedStatement.setDouble(2, similarityThreshold);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          this.totalCount = resultSet.getLong(1);
        }
      }
    }
    this.pageSize = size;
    this.currentPageIndex = page;

    if (this.totalCount == 0) {
      return;
    }

    // (2) ページング用ベクトル検索
    String sql = RETRIEVE_WITH_THRESHOLD_SQL + " OFFSET ?";
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      String vectorStr = query.toString();
      preparedStatement.setString(1, vectorStr);
      preparedStatement.setString(2, vectorStr);
      preparedStatement.setDouble(3, similarityThreshold);
      preparedStatement.setInt(4, size);
      preparedStatement.setInt(5, (page - 1) * size);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        this.entityLot = new ArrayList<>();
        while (resultSet.next()) {
          SES_AI_T_JOB entity = mapResultSet(resultSet);
          entity.setDistance(resultSet.getDouble("distance"));
          this.entityLot.add(entity);
        }
      }
    }
  }

  /**
   * raw_contentカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索条件Map
   * @throws SQLException
   */
  public void searchByRawContent(final Connection connection, final String query)
      throws SQLException {
    this.searchByField(connection, "raw_content", query);
  }

  /**
   * raw_contentカラムで全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索文字列
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByRawContentPaged(
      final Connection connection, final String query, final int page, final int size)
      throws SQLException {
    this.searchByFieldPaged(connection, "raw_content", query, page, size);
  }

  /**
   * raw_contentカラムに対して複数条件で全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @throws SQLException
   */
  public void searchByRawContent(
      final Connection connection, final String firstLikeQuery, final List<LogicalOperators> query)
      throws SQLException {
    this.searchByField(connection, "raw_content", firstLikeQuery, query);
  }

  /**
   * raw_contentカラムに対して複数条件で全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByRawContentPaged(
      final Connection connection,
      final String firstLikeQuery,
      final List<LogicalOperators> query,
      final int page,
      final int size)
      throws SQLException {
    this.searchByFieldPaged(connection, "raw_content", firstLikeQuery, query, page, size);
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

  @Override
  protected SES_AI_T_JOB mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_JOB sesAiTJob = new SES_AI_T_JOB();
    sesAiTJob.setJobId(resultSet.getString("job_id"));
    sesAiTJob.setFromGroup(resultSet.getString("from_group"));
    sesAiTJob.setFromId(resultSet.getString("from_id"));
    sesAiTJob.setFromName(resultSet.getString("from_name"));
    sesAiTJob.setRawContent(resultSet.getString("raw_content"));
    sesAiTJob.setContentSummary(resultSet.getString("content_summary"));
    sesAiTJob.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiTJob.setRegisterUser(resultSet.getString("register_user"));
    sesAiTJob.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return sesAiTJob;
  }
}
