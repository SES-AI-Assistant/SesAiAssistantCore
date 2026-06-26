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
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, register_date, register_user, ttl, vector_data <=> ?::vector AS distance, tenant_id FROM SES_AI_T_SKILLSHEET ORDER BY distance LIMIT ?";

  /** 類似度閾値を用いたベクトル検索SQL. */
  private static final String RETRIEVE_WITH_THRESHOLD_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, register_date, register_user, ttl, vector_data <=> ?::vector AS distance, tenant_id FROM SES_AI_T_SKILLSHEET WHERE 1 - (vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  /** 全文検索SQL(file_contentカラム). */
  private static final String SELECT_FILE_CONTENT_LIKE_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl, tenant_id FROM SES_AI_T_SKILLSHEET WHERE file_content LIKE ?";

  /** 複合条件全文検索用 SELECT 接頭辞（末尾に WHERE を含む）. */
  private static final String SELECT_FILE_CONTENT_FOR_FULLTEXT =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl, tenant_id FROM SES_AI_T_SKILLSHEET WHERE ";

  /** 全文検索SQL. */
  private static final String SELECT_LIKE_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl, tenant_id FROM SES_AI_T_SKILLSHEET WHERE ";

  /** 検索SQL. */
  private static final String SELECT_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl, tenant_id FROM SES_AI_T_SKILLSHEET WHERE ";

  /** SELECT文(file_name検索). */
  private static final String SELECT_BY_FILE_NAME_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl, tenant_id FROM SES_AI_T_SKILLSHEET WHERE file_name = ?";

  /** ベクトル検索のカウント用SQL. */
  private static final String COUNT_SQL_FOR_RETRIEVE = "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET";

  /** ベクトル検索のページング用SQL（OFFSET付き）. */
  private static final String RETRIEVE_SQL_WITH_OFFSET = RETRIEVE_SQL + " OFFSET ?";

  /** 類似度閾値ベクトル検索のカウント用SQL. */
  private static final String COUNT_SQL_FOR_RETRIEVE_WITH_THRESHOLD =
      "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET WHERE 1 - (vector_data <=> ?::vector) >= ?";

  /** 類似度閾値ベクトル検索のページング用SQL（OFFSET付き）. */
  private static final String RETRIEVE_WITH_THRESHOLD_SQL_WITH_OFFSET =
      RETRIEVE_WITH_THRESHOLD_SQL + " OFFSET ?";

  /** 期限切れスキルシート取得SQL前半（テナントIDあり）. */
  private static final String SELECT_EXPIRED_SKILLSHEETS_PREFIX =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl "
          + "FROM SES_AI_T_SKILLSHEET "
          + "WHERE ((ttl IS NOT NULL AND ttl < NOW()) "
          + "   OR (ttl IS NULL AND register_date IS NOT NULL AND (register_date + INTERVAL '";

  /** 期限切れスキルシート取得SQL後半（テナントIDあり）. */
  private static final String SELECT_EXPIRED_SKILLSHEETS_SUFFIX =
      " days') < NOW())) " + "ORDER BY register_date ASC " + "LIMIT ? OFFSET ?";

  /** 期限切れスキルシート取得SQL前半（テナントIDなし、バッチ用）. */
  private static final String SELECT_EXPIRED_SKILLSHEETS_WITHOUT_TENANT_PREFIX =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl, tenant_id "
          + "FROM SES_AI_T_SKILLSHEET "
          + "WHERE ((ttl IS NOT NULL AND ttl < NOW()) "
          + "   OR (ttl IS NULL AND register_date IS NOT NULL AND (register_date + INTERVAL '";

  /** 期限切れスキルシート取得SQL後半（テナントIDなし、バッチ用）. */
  private static final String SELECT_EXPIRED_SKILLSHEETS_WITHOUT_TENANT_SUFFIX =
      " days') < NOW())) " + "ORDER BY register_date ASC " + "LIMIT ? OFFSET ?";

  /** 要員に紐づくスキルシート取得SQL（テナントIDあり）. */
  private static final String SELECT_BUNDLED_WITH_PERSON_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl "
          + "FROM SES_AI_T_SKILLSHEET "
          + "WHERE from_group = ? AND from_id = ? AND DATE(register_date) = DATE(?)";

  /** 要員に紐づくスキルシート取得SQL（テナントIDなし、バッチ用）. */
  private static final String SELECT_BUNDLED_WITH_PERSON_WITHOUT_TENANT_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl, tenant_id "
          + "FROM SES_AI_T_SKILLSHEET "
          + "WHERE from_group = ? AND from_id = ? AND DATE(register_date) = DATE(?)";

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

  /** SELECT_ALL_SQL - 全件取得（登録日降順）. */
  private static final String SELECT_ALL_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl, tenant_id FROM SES_AI_T_SKILLSHEET ORDER BY register_date DESC";

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
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
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void retrieve(
      final Connection connection, final String tenantId, final Vector query, final int limit)
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
      final Connection connection,
      final String tenantId,
      final Vector query,
      final int page,
      final int size)
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
          SES_AI_T_SKILLSHEET entity = mapResultSet(rs);
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
  public void retrieve(final Connection connection, final String tenantId, final Vector query)
      throws SQLException {
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
      final Connection connection,
      final String tenantId,
      final Vector query,
      final double similarityThreshold,
      final int limit)
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
      final Connection connection,
      final String tenantId,
      final Vector query,
      final double similarityThreshold,
      final int page,
      final int size)
      throws SQLException {
    if (connection == null || query == null) {
      return;
    }

    executeVectorPagedQuery(
        connection,
        COUNT_SQL_FOR_RETRIEVE_WITH_THRESHOLD,
        tenantId,
        query.toString(),
        similarityThreshold,
        page,
        size,
        rs -> {
          SES_AI_T_SKILLSHEET entity = mapResultSet(rs);
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
   * 指定したカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param column 検索対象カラム
   * @param query 検索文字列
   * @throws SQLException
   */
  public void selectLike(
      final Connection connection, final String tenantId, final String column, final String query)
      throws SQLException {
    this.searchByField(connection, tenantId, column, query);
  }

  /**
   * file_contentカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索条件Map
   * @throws SQLException
   */
  public void searchByFileContent(
      final Connection connection, final String tenantId, final String query) throws SQLException {
    this.selectByLikeQuery(
        connection, tenantId, SELECT_FILE_CONTENT_LIKE_SQL, "file_content", query, null);
  }

  /**
   * file_contentカラムで全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索文字列
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFileContentPaged(
      final Connection connection,
      final String tenantId,
      final String query,
      final int page,
      final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(
        connection,
        tenantId,
        SELECT_FILE_CONTENT_LIKE_SQL,
        "file_content",
        query,
        null,
        page,
        size);
  }

  /**
   * file_nameカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param fileName ファイル名
   * @throws SQLException
   */
  public void selectByFileName(
      final Connection connection, final String tenantId, final String fileName)
      throws SQLException {
    if (connection == null || fileName == null) {
      this.entityLot = new ArrayList<>();
      return;
    }
    List<SES_AI_T_SKILLSHEET> results =
        executeQuery(
            connection,
            SELECT_BY_FILE_NAME_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              stmt.setString(paramIndex, fileName);
              return paramIndex + 1;
            });
    this.entityLot = results;
  }

  /**
   * file_contentカラムに対して複数条件で全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @throws SQLException
   */
  public void searchByFileContent(
      final Connection connection,
      final String tenantId,
      final String firstLikeQuery,
      final List<LogicalOperators> query)
      throws SQLException {
    this.searchByField(connection, tenantId, "file_content", firstLikeQuery, query);
  }

  /**
   * file_contentカラムに対して複数条件で全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFileContentPaged(
      final Connection connection,
      final String tenantId,
      final String firstLikeQuery,
      final List<LogicalOperators> query,
      final int page,
      final int size)
      throws SQLException {
    this.searchByFieldPaged(
        connection, tenantId, "file_content", firstLikeQuery, query, page, size);
  }

  /**
   * file_content に対して複合条件（AND/OR/NOT）で全文検索をページング実行します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param conditions API の conditions 配列と同一の論理
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFileContentPaged(
      final Connection connection,
      final String tenantId,
      final List<FulltextCondition> conditions,
      final int page,
      final int size)
      throws SQLException {
    FulltextConditionsWhereClause.Built built =
        FulltextConditionsWhereClause.build("file_content", conditions);
    this.selectByDynamicWherePaged(
        connection,
        tenantId,
        SELECT_FILE_CONTENT_FOR_FULLTEXT,
        built.getWhereClauseWithoutWhereKeyword(),
        built.getLikeParams(),
        page,
        size);
  }

  /**
   * TTL 期限切れのスキルシートを段階的に取得（チャンク処理用）.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param ttlDays TTL 日数
   * @param offset オフセット
   * @param limit 取得上限件数
   * @throws SQLException
   */
  public void selectExpiredSkillsheets(
      final Connection connection,
      final String tenantId,
      final int ttlDays,
      final int offset,
      final int limit)
      throws SQLException {
    List<SES_AI_T_SKILLSHEET> results =
        executeTTLExpiredQuery(
            connection,
            SELECT_EXPIRED_SKILLSHEETS_PREFIX,
            SELECT_EXPIRED_SKILLSHEETS_SUFFIX,
            tenantId,
            ttlDays,
            offset,
            limit,
            this::mapResultSet);
    this.entityLot = results;
  }

  /**
   * 期限切れスキルシートを取得します（テナントID条件なし、バッチ用）.
   *
   * <p>⚠️ このメソッドは全テナント対象です。 バッチ処理専用。コードレビュー必須。
   *
   * @param connection DBコネクション
   * @param ttlDays TTL日数
   * @param offset オフセット
   * @param limit リミット
   * @throws SQLException SQL実行エラー
   */
  public void selectExpiredSkillsheetsWithoutTenantId(
      final Connection connection, final int ttlDays, final int offset, final int limit)
      throws SQLException {
    List<SES_AI_T_SKILLSHEET> results =
        executeTTLExpiredQueryWithoutTenantFilter(
            connection,
            SELECT_EXPIRED_SKILLSHEETS_WITHOUT_TENANT_PREFIX,
            SELECT_EXPIRED_SKILLSHEETS_WITHOUT_TENANT_SUFFIX,
            ttlDays,
            offset,
            limit,
            this::mapResultSet);
    this.entityLot = results;
  }

  /**
   * 指定された要員に紐づくスキルシート（同一送信元・同一登録日）を取得.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param person 要員
   * @throws SQLException
   */
  public void selectBundledWithPerson(
      final Connection connection, final String tenantId, final SES_AI_T_PERSON person)
      throws SQLException {
    if (connection == null || person == null) {
      return;
    }
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(SELECT_BUNDLED_WITH_PERSON_SQL)) {
      preparedStatement.setString(1, person.getFromGroup());
      preparedStatement.setString(2, person.getFromId());
      preparedStatement.setTimestamp(
          3,
          person.getRegisterDate() != null
              ? person.getRegisterDate().toTimestamp()
              : new OriginalDateTime().toTimestamp());
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        this.entityLot = new ArrayList<>();
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
    }
  }

  /**
   * 指定された要員に紐づくスキルシート（同一送信元・同一登録日）を取得（WithoutTenantFilter - バッチ用）.
   *
   * <p>⚠️ このメソッドは全テナント対象です。 バッチ処理専用。コードレビュー必須。
   *
   * @param connection DBコネクション
   * @param person 要員
   * @throws SQLException SQL実行エラー
   */
  public void selectBundledWithPersonWithoutTenantFilter(
      final Connection connection, final SES_AI_T_PERSON person) throws SQLException {
    if (connection == null || person == null) {
      return;
    }
    List<SES_AI_T_SKILLSHEET> results =
        executeQueryWithoutTenantFilter(
            connection,
            SELECT_BUNDLED_WITH_PERSON_WITHOUT_TENANT_SQL,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              int idx = paramIndex;
              stmt.setString(idx, person.getFromGroup());
              stmt.setString(idx + 1, person.getFromId());
              stmt.setTimestamp(
                  idx + 2,
                  person.getRegisterDate() != null
                      ? person.getRegisterDate().toTimestamp()
                      : new OriginalDateTime().toTimestamp());
              return idx + 3;
            });
    this.entityLot = results;
  }

  @Override
  public void selectAll(final Connection connection, final String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_SKILLSHEET> results =
        executeQuery(
            connection,
            getSelectAllSql(),
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
  public void selectAllWithoutTenantFilter(final Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_SKILLSHEET> results =
        executeQueryWithoutTenantFilter(
            connection, getSelectAllSql(), this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  @Override
  protected SES_AI_T_SKILLSHEET mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_T_SKILLSHEET sesAiTSkillsheet = new SES_AI_T_SKILLSHEET(tenantId);
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
