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
 * @author 鈴木一矢
 */
public class SES_AI_T_JOBLot extends EntityLotBase<SES_AI_T_JOB> {
  /** ベクトル検索SQL. */
  private static final String RETRIEVE_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, register_date, register_user, ttl, vector_data <=> ?::vector AS distance FROM SES_AI_T_JOB ORDER BY distance LIMIT ?";

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
    if (connection == null) {
      return;
    }
    try (PreparedStatement preparedStatement = connection.prepareStatement(RETRIEVE_SQL)) {
      preparedStatement.setString(1, query == null ? null : query.toString());
      preparedStatement.setInt(2, limit);
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
