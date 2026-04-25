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
import lombok.extern.slf4j.Slf4j;

/**
 * 【Entityクラス】 要員情報(SES_AI_T_PERSON)テーブル의 Lotクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class SES_AI_T_PERSONLot extends EntityLotBase<SES_AI_T_PERSON> {
  /** ベクトル検索SQL. */
  private static final String RETRIEVE_SQL =
      "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, register_date, register_user, ttl, vector_data <=> ?::vector AS distance FROM SES_AI_T_PERSON ORDER BY distance LIMIT ?";

  /** 類似度閾値を用いたベクトル検索SQL. */
  private static final String RETRIEVE_WITH_THRESHOLD_SQL =
      "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, register_date, register_user, ttl, vector_data <=> ?::vector AS distance FROM SES_AI_T_PERSON WHERE 1 - (vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  /** 全文検索SQL. */
  private static final String SELECT_LIKE_SQL =
      "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON WHERE raw_content LIKE ?";

  /** 複合条件全文検索用 SELECT 接頭辞（末尾に WHERE を含む）. */
  private static final String SELECT_RAW_CONTENT_FOR_FULLTEXT =
      "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON WHERE ";

  /** 検索SQL. */
  private static final String SELECT_SQL =
      "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON WHERE ";

  /** 検索SQL(指定時間以降検索). */
  private static final String SELECT_SQL_BY_REGISTER_DATE =
      "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON WHERE register_date >= ?";

  /** 全件検索SQL. */
  private static final String SELECT_ALL_SQL =
      "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON";

  /** コンストラクタ. */
  public SES_AI_T_PERSONLot() {
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

  /**
   * 引数に指定した要員IDを持つEntityを返却する.
   *
   * @param personId 要員ID
   * @return SES_AI_T_PERSON
   */
  public SES_AI_T_PERSON getEntityByPk(final String personId) {
    log.debug("【getEntityByPk】personId:" + personId);
    if (personId == null) {
      return null;
    }

    for (SES_AI_T_PERSON entity : this.entityLot) {
      log.debug("【getEntityByPk】entity.getPersonId():" + entity.getPersonId());
      if (personId.trim().equals(entity.getPersonId().trim())) {
        log.debug("【getEntityByPk】return:" + entity.getPersonId());
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
   * 引数のファイルIDを持つレコードが存在するかどうかを判定する.
   *
   * @param fileId ファイルID
   * @return 存在すればtrue、それ以外はfalse
   */
  public boolean isExistByFileId(final String fileId) {
    return fileId != null && this.entityLot.stream().anyMatch(e -> fileId.equals(e.getFileId()));
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
    String countSql = "SELECT COUNT(*) FROM SES_AI_T_PERSON";
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
          SES_AI_T_PERSON sesAiTPerson = mapResultSet(resultSet);
          sesAiTPerson.setDistance(resultSet.getDouble("distance"));
          this.entityLot.add(sesAiTPerson);
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
    String countSql = "SELECT COUNT(*) FROM SES_AI_T_PERSON WHERE 1 - (vector_data <=> ?::vector) >= ?";
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
          SES_AI_T_PERSON entity = mapResultSet(resultSet);
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
   * raw_content に対して複合条件（AND/OR/NOT）で全文検索をページング実行します.
   *
   * @param connection DBコネクション
   * @param conditions API の conditions 配列と同一の論理
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByRawContentPaged(
      final Connection connection,
      final List<FulltextCondition> conditions,
      final int page,
      final int size)
      throws SQLException {
    FulltextConditionsWhereClause.Built built =
        FulltextConditionsWhereClause.build("raw_content", conditions);
    this.selectByDynamicWherePaged(
        connection,
        SELECT_RAW_CONTENT_FOR_FULLTEXT,
        built.getWhereClauseWithoutWhereKeyword(),
        built.getLikeParams(),
        page,
        size);
  }

  /**
   * 指定した時刻以降に登録されたレコードを全て取得する.
   *
   * @param connection DBコネクション
   * @param fromDate 時刻
   * @throws SQLException
   */
  public void selectByRegisterDateAfter(
      final Connection connection, final OriginalDateTime fromDate) throws SQLException {
    if (connection == null) {
      return;
    }
    // 検索条件を追加する
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(SELECT_SQL_BY_REGISTER_DATE)) {
      preparedStatement.setTimestamp(
          1, fromDate != null ? fromDate.toTimestamp() : new OriginalDateTime().toTimestamp());

      // 検索を実行する
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        this.entityLot = new ArrayList<>();
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
    }
  }

  /**
   * TTL 期限切れで MATCH に関連していない要員を段階的に取得（チャンク処理用）.
   *
   * @param connection DBコネクション
   * @param ttlDays TTL 日数
   * @param offset オフセット
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void selectExpiredPersonsNotInMatch(
      final Connection connection,
      final int ttlDays,
      final int offset,
      final int limit)
      throws SQLException {
    if (connection == null) {
      return;
    }
    String sql =
        "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, vector_data, register_date, register_user, ttl "
            + "FROM SES_AI_T_PERSON "
            + "WHERE ((ttl IS NOT NULL AND ttl < NOW()) "
            + "   OR (ttl IS NULL AND register_date IS NOT NULL AND (register_date + INTERVAL '"
            + ttlDays
            + " days') < NOW())) "
            + "  AND person_id NOT IN (SELECT DISTINCT person_id FROM SES_AI_T_MATCH) "
            + "ORDER BY register_date ASC "
            + "LIMIT ? OFFSET ?";
    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setInt(1, limit);
      preparedStatement.setInt(2, offset);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        this.entityLot = new ArrayList<>();
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
    }
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

  @Override
  public void selectAll(Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null) {
      return;
    }
    try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      while (resultSet.next()) {
        this.entityLot.add(mapResultSet(resultSet));
      }
    }
  }

  @Override
  protected SES_AI_T_PERSON mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_PERSON sesAiTPerson = new SES_AI_T_PERSON();
    sesAiTPerson.setPersonId(resultSet.getString("person_id"));
    sesAiTPerson.setFromGroup(resultSet.getString("from_group"));
    sesAiTPerson.setFromId(resultSet.getString("from_id"));
    sesAiTPerson.setFromName(resultSet.getString("from_name"));
    sesAiTPerson.setFileId(resultSet.getString("file_id"));
    sesAiTPerson.setRawContent(resultSet.getString("raw_content"));
    sesAiTPerson.setContentSummary(resultSet.getString("content_summary"));
    sesAiTPerson.setUnitPrice(resultSet.getBigDecimal("unit_price"));
    sesAiTPerson.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiTPerson.setRegisterUser(resultSet.getString("register_user"));
    sesAiTPerson.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return sesAiTPerson;
  }
}
