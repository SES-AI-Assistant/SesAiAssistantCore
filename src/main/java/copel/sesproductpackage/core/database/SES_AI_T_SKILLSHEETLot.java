package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.FulltextConditionsWhereClause;
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
 * 【Entityクラス】 スキルシート情報(SES_AI_T_SKILLSHEET)テーブルのLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_SKILLSHEETLot extends EntityLotBase<SES_AI_T_SKILLSHEET> {
  /** ベクトル検索SQL. */
  private static final String RETRIEVE_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, register_date, register_user, ttl, vector_data <=> ?::vector AS distance FROM SES_AI_T_SKILLSHEET ORDER BY distance LIMIT ?";

  /** 類似度閾値を用いたベクトル検索SQL. */
  private static final String RETRIEVE_WITH_THRESHOLD_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, register_date, register_user, ttl, vector_data <=> ?::vector AS distance FROM SES_AI_T_SKILLSHEET WHERE 1 - (vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  /** 全文検索SQL(file_contentカラム). */
  private static final String SELECT_FILE_CONTENT_LIKE_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE file_content LIKE ?";

  /** 複合条件全文検索用 SELECT 接頭辞（末尾に WHERE を含む）. */
  private static final String SELECT_FILE_CONTENT_FOR_FULLTEXT =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE ";

  /** 全文検索SQL. */
  private static final String SELECT_LIKE_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE ";

  /** 検索SQL. */
  private static final String SELECT_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE ";

  /** SELECT文(file_name検索). */
  private static final String SELECT_BY_FILE_NAME_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE file_name = ?";

  /** コンストラクタ. */
  public SES_AI_T_SKILLSHEETLot() {
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
    return "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET";
  }

  /**
   * LLMに最もマッチするスキルシートのファイルIDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String toスキルシート選出用文章() {
    StringBuilder result = new StringBuilder();
    int i = 1;
    for (SES_AI_T_SKILLSHEET entity : this.entityLot) {
      result.append(i).append("人目：").append(entity.toスキルシート選出用文章());
      i++;
    }
    return result.toString();
  }

  /**
   * 引数に指定したファイルIDを持つEntityを返却する.
   *
   * @param fileId ファイルID
   * @return SES_AI_T_SKILLSHEET
   */
  public SES_AI_T_SKILLSHEET getEntityByPk(final String fileId) {
    if (fileId == null) {
      return null;
    }
    for (SES_AI_T_SKILLSHEET entity : this.entityLot) {
      if (fileId.trim().equals(entity.getFileId().trim())) {
        return entity;
      }
    }
    return null;
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
    String countSql = "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET";
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
          SES_AI_T_SKILLSHEET sesAiTSkillsheet = mapResultSet(resultSet);
          sesAiTSkillsheet.setDistance(resultSet.getDouble("distance"));
          this.entityLot.add(sesAiTSkillsheet);
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
    String countSql = "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET WHERE 1 - (vector_data <=> ?::vector) >= ?";
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
          SES_AI_T_SKILLSHEET entity = mapResultSet(resultSet);
          entity.setDistance(resultSet.getDouble("distance"));
          this.entityLot.add(entity);
        }
      }
    }
  }

  /**
   * 指定したカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param column 検索対象カラム
   * @param query 検索文字列
   * @throws SQLException
   */
  public void selectLike(final Connection connection, final String column, final String query)
      throws SQLException {
    this.searchByField(connection, column, query);
  }

  /**
   * file_contentカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索条件Map
   * @throws SQLException
   */
  public void searchByFileContent(final Connection connection, final String query)
      throws SQLException {
    this.selectByLikeQuery(connection, SELECT_FILE_CONTENT_LIKE_SQL, "file_content", query, null);
  }

  /**
   * file_contentカラムで全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索文字列
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFileContentPaged(
      final Connection connection, final String query, final int page, final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(
        connection, SELECT_FILE_CONTENT_LIKE_SQL, "file_content", query, null, page, size);
  }

  /**
   * file_nameカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param fileName ファイル名
   * @throws SQLException
   */
  public void selectByFileName(final Connection connection, final String fileName)
      throws SQLException {
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(SELECT_BY_FILE_NAME_SQL)) {
      preparedStatement.setString(1, fileName);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        this.entityLot = new ArrayList<>();
        while (resultSet.next()) {
          SES_AI_T_SKILLSHEET sesAiTSkillsheet = new SES_AI_T_SKILLSHEET();
          sesAiTSkillsheet.setFromGroup(resultSet.getString("from_group"));
          sesAiTSkillsheet.setFromId(resultSet.getString("from_id"));
          sesAiTSkillsheet.setFromName(resultSet.getString("from_name"));
          sesAiTSkillsheet.setFileId(resultSet.getString("file_id"));
          sesAiTSkillsheet.setFileName(resultSet.getString("file_name"));
          sesAiTSkillsheet.setRegisterDate(
              new OriginalDateTime(resultSet.getString("register_date")));
          sesAiTSkillsheet.setRegisterUser(resultSet.getString("register_user"));
          sesAiTSkillsheet.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
          this.entityLot.add(sesAiTSkillsheet);
        }
      }
    }
  }

  /**
   * file_contentカラムに対して複数条件で全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @throws SQLException
   */
  public void searchByFileContent(
      final Connection connection, final String firstLikeQuery, final List<LogicalOperators> query)
      throws SQLException {
    this.searchByField(connection, "file_content", firstLikeQuery, query);
  }

  /**
   * file_contentカラムに対して複数条件で全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFileContentPaged(
      final Connection connection,
      final String firstLikeQuery,
      final List<LogicalOperators> query,
      final int page,
      final int size)
      throws SQLException {
    this.searchByFieldPaged(connection, "file_content", firstLikeQuery, query, page, size);
  }

  /**
   * file_content に対して複合条件（AND/OR/NOT）で全文検索をページング実行します.
   *
   * @param connection DBコネクション
   * @param conditions API の conditions 配列と同一の論理
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFileContentPaged(
      final Connection connection,
      final List<FulltextCondition> conditions,
      final int page,
      final int size)
      throws SQLException {
    FulltextConditionsWhereClause.Built built =
        FulltextConditionsWhereClause.build("file_content", conditions);
    this.selectByDynamicWherePaged(
        connection,
        SELECT_FILE_CONTENT_FOR_FULLTEXT,
        built.getWhereClauseWithoutWhereKeyword(),
        built.getLikeParams(),
        page,
        size);
  }

  @Override
  public void selectAll(Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null) {
      return;
    }
    try (PreparedStatement preparedStatement =
            connection.prepareStatement(
                "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET");
        ResultSet resultSet = preparedStatement.executeQuery()) {
      while (resultSet.next()) {
        this.entityLot.add(mapResultSet(resultSet));
      }
    }
  }

  @Override
  protected SES_AI_T_SKILLSHEET mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_SKILLSHEET sesAiTSkillsheet = new SES_AI_T_SKILLSHEET();
    sesAiTSkillsheet.setFromGroup(resultSet.getString("from_group"));
    sesAiTSkillsheet.setFromId(resultSet.getString("from_id"));
    sesAiTSkillsheet.setFromName(resultSet.getString("from_name"));
    sesAiTSkillsheet.setFileId(resultSet.getString("file_id"));
    sesAiTSkillsheet.setFileName(resultSet.getString("file_name"));
    sesAiTSkillsheet.setFileContent(resultSet.getString("file_content"));
    sesAiTSkillsheet.setFileContentSummary(resultSet.getString("file_content_summary"));
    sesAiTSkillsheet.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiTSkillsheet.setRegisterUser(resultSet.getString("register_user"));
    sesAiTSkillsheet.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return sesAiTSkillsheet;
  }
}
