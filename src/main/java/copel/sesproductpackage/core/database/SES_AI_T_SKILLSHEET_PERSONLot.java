package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【Entityクラス】 スキルシート情報と要員情報を結合したEntity(SES_AI_T_SKILLSHEET_PERSON)のLotクラス.
 *
 * @author
 */
public class SES_AI_T_SKILLSHEET_PERSONLot extends EntityLotBase<SES_AI_T_SKILLSHEET_PERSON> {

  private static final String RETRIEVE_BY_PERSON_VECTOR_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, p.vector_data <=> ?::vector AS distance "
          + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE 1 - (p.vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  private static final String RETRIEVE_BY_SKILLSHEET_VECTOR_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, s.vector_data <=> ?::vector AS distance "
          + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE 1 - (s.vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  private static final String RETRIEVE_OUTER_JOIN_BY_PERSON_VECTOR_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, p.vector_data <=> ?::vector AS distance "
          + "FROM SES_AI_T_PERSON p LEFT JOIN SES_AI_T_SKILLSHEET s ON p.file_id = s.file_id "
          + "WHERE 1 - (p.vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  private static final String RETRIEVE_OUTER_JOIN_BY_SKILLSHEET_VECTOR_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, s.register_date, s.register_user, COALESCE(s.from_group, p.from_group) AS from_group, COALESCE(s.from_id, p.from_id) AS from_id, COALESCE(s.from_name, p.from_name) AS from_name, s.vector_data <=> ?::vector AS distance "
          + "FROM SES_AI_T_SKILLSHEET s LEFT JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE 1 - (s.vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  private static final String SELECT_BY_PERSON_RAW_CONTENT_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name "
          + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE p.raw_content LIKE ?";

  private static final String SELECT_BY_SKILLSHEET_RAW_CONTENT_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name "
          + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE s.file_content LIKE ?";

  /** コンストラクタ. */
  public SES_AI_T_SKILLSHEET_PERSONLot() {
    super();
  }

  @Override
  protected String getSelectAllSql() {
    return "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id";
  }

  @Override
  protected String getSelectSql() {
    return "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id WHERE ";
  }

  @Override
  protected String getSelectLikeSql() {
    return null;
  }

  /**
   * 要員のベクトルデータに対してセマンティック検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieveByPersonVector(
      final Connection connection,
      final Vector query,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    executeRetrieve(connection, RETRIEVE_BY_PERSON_VECTOR_SQL, query, similarityThreshold, limit);
  }

  /**
   * スキルシートのベクトルデータに対してセマンティック検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieveBySkillSheetVector(
      final Connection connection,
      final Vector query,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    executeRetrieve(
        connection, RETRIEVE_BY_SKILLSHEET_VECTOR_SQL, query, similarityThreshold, limit);
  }

  /**
   * 要員のベクトルデータに対してセマンティック検索を実行し、結果をこのLotに保持します(OUTER JOIN).
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieveOuterJoinByPersonVector(
      final Connection connection,
      final Vector query,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    executeRetrieve(
        connection, RETRIEVE_OUTER_JOIN_BY_PERSON_VECTOR_SQL, query, similarityThreshold, limit);
  }

  /**
   * スキルシートのベクトルデータに対してセマンティック検索を実行し、結果をこのLotに保持します(OUTER JOIN).
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieveOuterJoinBySkillSheetVector(
      final Connection connection,
      final Vector query,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    executeRetrieve(
        connection,
        RETRIEVE_OUTER_JOIN_BY_SKILLSHEET_VECTOR_SQL,
        query,
        similarityThreshold,
        limit);
  }

  /**
   * 要員のベクトルデータに対してセマンティック検索を実行し、結果をこのLotに保持します(ページング対応).
   *
   * @param connection DBコネクション
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrieveByPersonVectorPaged(
      final Connection connection,
      final Vector query,
      final double similarityThreshold,
      final int page,
      final int size)
      throws SQLException {
    executeRetrievePaged(
        connection,
        RETRIEVE_BY_PERSON_VECTOR_SQL,
        query,
        similarityThreshold,
        page,
        size,
        "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id WHERE 1 - (p.vector_data <=> ?::vector) >= ?");
  }

  private void executeRetrieve(
      final Connection connection,
      final String sql,
      final Vector query,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    executeRetrievePaged(connection, sql, query, similarityThreshold, 1, limit, null);
  }

  private void executeRetrievePaged(
      final Connection connection,
      final String sql,
      final Vector query,
      final double similarityThreshold,
      final int page,
      final int size,
      final String countQueryPart)
      throws SQLException {
    if (connection == null || query == null) {
      return;
    }

    // (1) 全件数を取得 (countQueryPart があれば)
    if (countQueryPart != null) {
      String countSql = "SELECT COUNT(*) " + countQueryPart;
      try (PreparedStatement preparedStatement = connection.prepareStatement(countSql)) {
        String vectorStr = query.toString();
        preparedStatement.setString(1, vectorStr);
        preparedStatement.setDouble(2, similarityThreshold);
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
          if (resultSet.next()) {
            this.totalCount = resultSet.getLong(1);
          }
        }
      }
    } else {
      // 簡易的に limit を totalCount とする (countQueryPartがない場合)
      this.totalCount = size;
    }

    this.pageSize = size;
    this.currentPageIndex = page;

    if (this.totalCount == 0 && countQueryPart != null) {
      return;
    }

    // (2) ページング実行
    String pagedSql = sql + " OFFSET ?";
    try (PreparedStatement preparedStatement = connection.prepareStatement(pagedSql)) {
      String vectorStr = query.toString();
      preparedStatement.setString(1, vectorStr);
      preparedStatement.setString(2, vectorStr);
      preparedStatement.setDouble(3, similarityThreshold);
      preparedStatement.setInt(4, size);
      preparedStatement.setInt(5, (page - 1) * size);

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        this.entityLot = new ArrayList<>();
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
    }
  }

  /**
   * 要員の全文情報(raw_content)に対して検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索キーワード
   * @throws SQLException
   */
  public void retrieveByPersonRawContent(final Connection connection, final String query)
      throws SQLException {
    this.selectByLikeQuery(
        connection, SELECT_BY_PERSON_RAW_CONTENT_SQL, "p.raw_content", query, null);
  }

  /**
   * 要員の全文情報(raw_content)に対してページング検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索キーワード
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrieveByPersonRawContentPaged(
      final Connection connection, final String query, final int page, final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(
        connection, SELECT_BY_PERSON_RAW_CONTENT_SQL, "p.raw_content", query, null, page, size);
  }

  /**
   * 要員の全文情報(raw_content)に対して複数条件で検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param firstLikeQuery 1つ目の検索キーワード
   * @param query 追加の検索条件
   * @throws SQLException
   */
  public void retrieveByPersonRawContent(
      final Connection connection, final String firstLikeQuery, final List<LogicalOperators> query)
      throws SQLException {
    this.selectByLikeQuery(
        connection, SELECT_BY_PERSON_RAW_CONTENT_SQL, "p.raw_content", firstLikeQuery, query);
  }

  /**
   * スキルシートの全文情報(file_content)に対して検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索キーワード
   * @throws SQLException
   */
  public void retrieveBySkillSheetRawContent(final Connection connection, final String query)
      throws SQLException {
    this.selectByLikeQuery(
        connection, SELECT_BY_SKILLSHEET_RAW_CONTENT_SQL, "s.file_content", query, null);
  }

  /**
   * スキルシートの全文情報(file_content)に対してページング検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param query 検索キーワード
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void retrieveBySkillSheetRawContentPaged(
      final Connection connection, final String query, final int page, final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(
        connection, SELECT_BY_SKILLSHEET_RAW_CONTENT_SQL, "s.file_content", query, null, page, size);
  }

  /**
   * スキルシートの全文情報(file_content)に対して複数条件で検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param firstLikeQuery 1つ目の検索キーワード
   * @param query 追加の検索条件
   * @throws SQLException
   */
  public void retrieveBySkillSheetRawContent(
      final Connection connection, final String firstLikeQuery, final List<LogicalOperators> query)
      throws SQLException {
    this.selectByLikeQuery(
        connection, SELECT_BY_SKILLSHEET_RAW_CONTENT_SQL, "s.file_content", firstLikeQuery, query);
  }

  /**
   * 指定した要員IDのEntityを取得する.
   *
   * @param personId 要員ID
   * @return 指定した要員IDのEntity
   */
  public SES_AI_T_SKILLSHEET_PERSON getEntityByPk(final String personId) {
    if (this.entityLot == null || personId == null) {
      return null;
    }
    for (SES_AI_T_SKILLSHEET_PERSON entity : this.entityLot) {
      if (personId.trim().equals(entity.getPersonId().trim())) {
        return entity;
      }
    }
    return null;
  }

  /**
   * 指定したファイルIDのEntityを取得する.
   *
   * @param fileId ファイルID
   * @return 指定したファイルIDのEntity
   */
  public SES_AI_T_SKILLSHEET_PERSON getEntityByFileId(final String fileId) {
    if (this.entityLot == null || fileId == null) {
      return null;
    }
    for (SES_AI_T_SKILLSHEET_PERSON entity : this.entityLot) {
      if (entity.getFileId() != null && fileId.trim().equals(entity.getFileId().trim())) {
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
    if (this.entityLot != null) {
      for (SES_AI_T_SKILLSHEET_PERSON entity : this.entityLot) {
        result.append(i).append("人目：").append(entity.to要員選出用文章()).append("\n");
        i++;
      }
    }
    return result.toString();
  }

  /**
   * LLMに最もマッチするスキルシートのファイルIDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String toスキルシート選出用文章() {
    StringBuilder result = new StringBuilder();
    int i = 1;
    if (this.entityLot != null) {
      for (SES_AI_T_SKILLSHEET_PERSON entity : this.entityLot) {
        result.append(i).append("件目：").append(entity.toスキルシート選出用文章()).append("\n");
        i++;
      }
    }
    return result.toString();
  }

  @Override
  public void selectAll(Connection connection) throws SQLException {
    // 全件検索は不要なため未実装とするが、Abstractメソッドのため空実装にしておく
  }

  @Override
  protected SES_AI_T_SKILLSHEET_PERSON mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_SKILLSHEET_PERSON entity = new SES_AI_T_SKILLSHEET_PERSON();
    entity.setEntityDataFromResultSet(resultSet);
    if (hasColumn(resultSet, "distance")) {
      entity.setDistance(resultSet.getDouble("distance"));
    }
    return entity;
  }

  private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
    java.sql.ResultSetMetaData rsmd = rs.getMetaData();
    int columns = rsmd.getColumnCount();
    for (int x = 1; x <= columns; x++) {
      if (columnName.equalsIgnoreCase(rsmd.getColumnLabel(x))) {
        return true;
      }
    }
    return false;
  }
}
