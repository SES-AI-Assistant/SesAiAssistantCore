package copel.sesproductpackage.core.database.base;

import copel.sesproductpackage.core.unit.LogicalOperators;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EntityLotの基底クラス.
 *
 * @author Copel Co., Ltd.
 */
public abstract class EntityLotBase<E extends EntityBase> implements Iterable<E> {

  /**
   * SELECT 列リストを COUNT(*) に置換する（ページング件数取得用）.
   *
   * <p>{@code (?i)SELECT.*?FROM} のような単純パターンは、列名 {@code from_group} / {@code from_id} などに含まれる {@code
   * from} を {@code FROM} キーワードと誤認するため使用しない。
   */
  static String toCountSql(final String baseSql) {
    if (baseSql == null) {
      return null;
    }
    String countSql = baseSql.replaceFirst("(?i)\\bSELECT\\s+.*?\\s+\\bFROM\\s+", "SELECT COUNT(*) FROM ");
    // ORDER BY / GROUP BY を削除（COUNT では不要で、PostgreSQL の GROUP BY 検証エラー回避）
    countSql = countSql.replaceAll("(?i)\\s+(ORDER\\s+BY|GROUP\\s+BY)\\s+.*$", "");
    return countSql;
  }

  // ================================
  // メンバ
  // ================================
  /** エンティティLot. */
  protected Collection<E> entityLot;

  /** 全レコード数. */
  protected long totalCount;

  /** 1ページあたりの表示件数. */
  protected int pageSize;

  /** 現在のページ番号. */
  protected int currentPageIndex;

  // ================================
  // コンストラクタ
  // ================================
  public EntityLotBase() {
    this.entityLot = new ArrayList<>();
  }

  // ================================
  // メソッド定義
  // ================================
  /**
   * テーブルからレコードを全件SELECTし、このLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @throws SQLException
   */
  public abstract void selectAll(final Connection connection, final String tenantId) throws SQLException;

  /**
   * テーブルからレコードを全件ページングSELECTし、このLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void selectAllPaged(final Connection connection, final String tenantId, final int page, final int size)
      throws SQLException {
    this.selectByQueryPaged(connection, tenantId, getSelectAllSql(), null, true, page, size);
  }

  /**
   * 全件検索用の基底SQLを取得します.
   *
   * @return SQL
   */
  protected abstract String getSelectAllSql();

  /**
   * このLotにEntityを追加します.
   *
   * @param entity エンティティ
   */
  public void add(E entity) {
    this.entityLot.add(entity);
  }

  /**
   * 引数のindex番目のエンティティを追加します.
   *
   * @param index インデックス
   * @return エンティティ
   */
  public E get(final int index) {
    if (index >= this.size()) {
      return null;
    }
    int currentIndex = 0;
    for (E entity : this.entityLot) {
      if (currentIndex == index) {
        return entity;
      }
      currentIndex++;
    }
    return null;
  }

  /**
   * このLotの要素数を返却します.
   *
   * @return 要素数
   */
  public int size() {
    return this.entityLot.size();
  }

  /**
   * このLotが空であるかどうを返却します.
   *
   * @return 空であればtrue、そうでなければfalse
   */
  public boolean isEmpty() {
    return this.entityLot.isEmpty();
  }

  /**
   * このLotをIteratorとして返却します.
   *
   * @return Iterator
   */
  public Iterator<E> iterator() {
    return this.entityLot.iterator();
  }

  /** このLotを昇順でソートします. */
  public void sort() {
    this.entityLot = this.entityLot.stream().sorted().collect(Collectors.toList());
  }

  /**
   * 全レコード数を取得します.
   *
   * @return 全レコード数
   */
  public long getTotalCount() {
    return totalCount;
  }

  /**
   * 全レコード数を設定します.
   *
   * @param totalCount 全レコード数
   */
  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  /**
   * 1ページあたりの表示件数を取得します.
   *
   * @return 1ページあたりの表示件数
   */
  public int getPageSize() {
    return pageSize;
  }

  /**
   * 1ページあたりの表示件数を設定します.
   *
   * @param pageSize 1ページあたりの表示件数
   */
  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * 現在のページ番号を取得します.
   *
   * @return 現在のページ番号
   */
  public int getCurrentPageIndex() {
    return currentPageIndex;
  }

  /**
   * 現在のページ番号を設定します.
   *
   * @param currentPageIndex 現在のページ番号
   */
  public void setCurrentPageIndex(int currentPageIndex) {
    this.currentPageIndex = currentPageIndex;
  }

  /**
   * 全ページ数を計算して返却します.
   *
   * @return 全ページ数
   */
  public int getTotalPages() {
    if (pageSize <= 0) {
      return 1;
    }
    return (int) Math.ceil((double) totalCount / pageSize);
  }

  /**
   * SELECTをAND条件で実行する.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param andQuery カラム名と検索値をkey-valueで持つMap
   * @throws SQLException
   */
  public void selectByAndQuery(final Connection connection, final String tenantId, final Map<String, Object> andQuery)
      throws SQLException {
    this.selectByQuery(connection, tenantId, getSelectSql(), andQuery, true);
  }

  /**
   * SELECTをOR条件で実行する.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param orQuery カラム名と検索値をkey-valueで持つMap
   * @throws SQLException
   */
  public void selectByOrQuery(final Connection connection, final String tenantId, final Map<String, Object> orQuery)
      throws SQLException {
    this.selectByQuery(connection, tenantId, getSelectSql(), orQuery, false);
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
  public void searchByField(final Connection connection, final String tenantId, final String column, final String query)
      throws SQLException {
    this.selectByLikeQuery(connection, tenantId, getSelectLikeSql(), column, query, null);
  }

  /**
   * 指定したカラムで全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param column 検索対象カラム
   * @param query 検索文字列
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFieldPaged(
      final Connection connection,
      final String tenantId,
      final String column,
      final String query,
      final int page,
      final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(connection, tenantId, getSelectLikeSql(), column, query, null, page, size);
  }

  /**
   * 指定したカラムに対して複数条件で全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param column 検索対象カラム
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @throws SQLException
   */
  public void searchByField(
      final Connection connection,
      final String tenantId,
      final String column,
      final String firstLikeQuery,
      final List<LogicalOperators> query)
      throws SQLException {
    this.selectByLikeQuery(connection, tenantId, getSelectLikeSql(), column, firstLikeQuery, query);
  }

  /**
   * 指定したカラムに対して複数条件で全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param column 検索対象カラム
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFieldPaged(
      final Connection connection,
      final String tenantId,
      final String column,
      final String firstLikeQuery,
      final List<LogicalOperators> query,
      final int page,
      final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(
        connection, tenantId, getSelectLikeSql(), column, firstLikeQuery, query, page, size);
  }

  /**
   * 検索用SQL(WHERE句の前まで)を取得します.
   *
   * @return SQL
   */
  protected String getSelectSql() {
    return null;
  }

  /**
   * 全文検索用SQL(WHERE句の前まで)を取得します.
   *
   * @return SQL
   */
  protected String getSelectLikeSql() {
    return null;
  }

  /**
   * 条件を指定して検索を実行します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param baseSql 基底SQL
   * @param query 検索条件Map
   * @param isAnd AND検索ならtrue, OR検索ならfalse
   * @throws SQLException
   */
  protected void selectByQuery(
      final Connection connection,
      final String tenantId,
      final String baseSql,
      final Map<String, Object> query,
      final boolean isAnd)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || query == null || query.isEmpty()) {
      return;
    }

    // SQL 構築（tenantId フィルターなし）
    StringBuilder sql = new StringBuilder(baseSql);
    boolean isFirst = true;
    for (final String columnName : query.keySet()) {
      if (isFirst) {
        sql.append(columnName).append(" = ?");
        isFirst = false;
      } else {
        sql.append(isAnd ? " AND " : " OR ").append(columnName).append(" = ?");
      }
    }

    // 新しいテンプレートメソッドで実行
    List<E> results = executeQuery(
        connection,
        sql.toString(),
        tenantId,
        this::mapResultSet,
        (stmt, paramIndex) -> {
          int idx = paramIndex;
          for (final String columnName : query.keySet()) {
            Object value = query.get(columnName);
            if (value instanceof Boolean) {
              stmt.setBoolean(idx, (Boolean) value);
            } else if (value instanceof Integer) {
              stmt.setInt(idx, (Integer) value);
            } else if (value instanceof Long) {
              stmt.setLong(idx, (Long) value);
            } else {
              stmt.setString(idx, value != null ? value.toString() : null);
            }
            idx++;
          }
          return idx;
        }
    );
    this.entityLot.addAll(results);
  }

  /**
   * あいまい検索を実行します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param baseSql 基底SQL(LIKE句の前まで)
   * @param columnName 検索対象カラム名
   * @param firstLikeQuery 最初の検索ワード
   * @param query 追加の検索条件リスト
   * @throws SQLException
   */
  protected void selectByLikeQuery(
      final Connection connection,
      final String tenantId,
      final String baseSql,
      final String columnName,
      final String firstLikeQuery,
      final List<LogicalOperators> query)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || firstLikeQuery == null) {
      return;
    }

    // SQL 構築（tenantId フィルターなし）
    StringBuilder sql = new StringBuilder(baseSql);
    if (query != null) {
      for (final LogicalOperators logicalOperator : query) {
        if (logicalOperator != null) {
          logicalOperator.setColumnName(columnName);
          sql.append(logicalOperator.getLikeQuery());
        }
      }
    }

    // 新しいテンプレートメソッドで実行
    List<E> results = executeQuery(
        connection,
        sql.toString(),
        tenantId,
        this::mapResultSet,
        (stmt, paramIndex) -> {
          int idx = paramIndex;
          stmt.setString(idx, "%" + firstLikeQuery + "%");
          idx++;
          if (query != null && !query.isEmpty()) {
            for (LogicalOperators operator : query) {
              if (operator != null) {
                stmt.setString(idx, "%" + operator.getValue() + "%");
                idx++;
              }
            }
          }
          return idx;
        }
    );
    this.entityLot.addAll(results);
  }

  /**
   * あいまい検索をページングで実行します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param baseSql 基底SQL(LIKE句の前まで)
   * @param columnName 検索対象カラム名
   * @param firstLikeQuery 最初の検索ワード
   * @param query 追加の検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  protected void selectByLikeQueryPaged(
      final Connection connection,
      final String tenantId,
      final String baseSql,
      final String columnName,
      final String firstLikeQuery,
      final List<LogicalOperators> query,
      final int page,
      final int size)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || firstLikeQuery == null || baseSql == null) {
      return;
    }

    // (1) 件数用のSQL構築（tenantId フィルターなし）
    StringBuilder countSql = new StringBuilder(toCountSql(baseSql));
    if (query != null) {
      for (final LogicalOperators logicalOperator : query) {
        if (logicalOperator != null) {
          logicalOperator.setColumnName(columnName);
          countSql.append(logicalOperator.getLikeQuery());
        }
      }
    }

    // tenant_id フィルターを追加して実行
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(addTenantIdFilter(countSql.toString(), tenantId))) {
      int paramIndex = 1;
      preparedStatement.setString(paramIndex++, "%" + firstLikeQuery + "%");
      if (query != null && !query.isEmpty()) {
        for (LogicalOperators operator : query) {
          if (operator != null) {
            preparedStatement.setString(paramIndex++, "%" + operator.getValue() + "%");
          }
        }
      }
      setTenantIdParameter(preparedStatement, paramIndex, tenantId);

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

    // (2) ページング用SQL構築（tenantId フィルターなし）
    StringBuilder sql = new StringBuilder(baseSql);
    if (query != null) {
      for (final LogicalOperators logicalOperator : query) {
        if (logicalOperator != null) {
          logicalOperator.setColumnName(columnName);
          sql.append(logicalOperator.getLikeQuery());
        }
      }
    }
    sql.append(" LIMIT ? OFFSET ?");

    // 新しいテンプレートメソッドで実行
    List<E> results = executeQuery(
        connection,
        sql.toString(),
        tenantId,
        this::mapResultSet,
        (stmt, paramIndex) -> {
          int idx = paramIndex;
          stmt.setString(idx++, "%" + firstLikeQuery + "%");
          if (query != null && !query.isEmpty()) {
            for (LogicalOperators operator : query) {
              if (operator != null) {
                stmt.setString(idx++, "%" + operator.getValue() + "%");
              }
            }
          }
          stmt.setInt(idx, size);
          stmt.setInt(idx + 1, (page - 1) * size);
          return idx + 2;
        }
    );
    this.entityLot.addAll(results);
  }

  /**
   * 動的 WHERE 句（LIKE / NOT LIKE 等）でページング全文検索を実行します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param selectSqlPrefix {@code SELECT ... FROM ... WHERE } まで（末尾に WHERE を含む）
   * @param whereClauseWithoutWhere WHERE に続く条件式のみ（例: {@code (a LIKE ?) OR (b LIKE ?)}）
   * @param likeParams プレースホルダに順にバインドする値
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  protected void selectByDynamicWherePaged(
      final Connection connection,
      final String tenantId,
      final String selectSqlPrefix,
      final String whereClauseWithoutWhere,
      final List<String> likeParams,
      final int page,
      final int size)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || selectSqlPrefix == null || whereClauseWithoutWhere == null) {
      return;
    }
    if (likeParams == null || likeParams.isEmpty()) {
      return;
    }

    // (1) 件数用 SQL 構築（tenantId フィルターなし）
    final String fullSelect = selectSqlPrefix + whereClauseWithoutWhere;
    final String countSql = toCountSql(fullSelect);

    // tenant_id フィルターを追加して実行
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(addTenantIdFilter(countSql, tenantId))) {
      int paramIndex = 1;
      for (String p : likeParams) {
        preparedStatement.setString(paramIndex++, p);
      }
      setTenantIdParameter(preparedStatement, paramIndex, tenantId);

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

    // (2) ページング用 SQL（tenantId フィルターなし）
    final String pagedSql = fullSelect + " LIMIT ? OFFSET ?";

    // 新しいテンプレートメソッドで実行
    List<E> results = executeQuery(
        connection,
        pagedSql,
        tenantId,
        this::mapResultSet,
        (stmt, paramIndex) -> {
          int idx = paramIndex;
          for (String p : likeParams) {
            stmt.setString(idx++, p);
          }
          stmt.setInt(idx, size);
          stmt.setInt(idx + 1, (page - 1) * size);
          return idx + 2;
        }
    );
    this.entityLot.addAll(results);
  }

  /**
   * 条件を指定して件数を取得します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param baseSql 基底SQL(SELECT * FROM ...)
   * @param query 検索条件Map
   * @param isAnd AND検索ならtrue, OR検索ならfalse
   * @return 件数
   * @throws SQLException
   */
  protected long countByQuery(
      final Connection connection,
      final String tenantId,
      final String baseSql,
      final Map<String, String> query,
      final boolean isAnd)
      throws SQLException {
    if (connection == null || baseSql == null) {
      return 0;
    }

    // COUNT SQL 構築（tenantId フィルターなし）
    String countSql = toCountSql(baseSql);
    StringBuilder sql = new StringBuilder(countSql);

    if (query != null && !query.isEmpty()) {
      boolean isFirst = true;
      if (!countSql.toUpperCase().contains("WHERE")) {
        sql.append(" WHERE ");
      } else {
        isFirst = false;
      }

      for (final String columnName : query.keySet()) {
        if (isFirst) {
          sql.append(columnName).append(" = ?");
          isFirst = false;
        } else {
          sql.append(isAnd ? " AND " : " OR ").append(columnName).append(" = ?");
        }
      }
    }

    // 新しいテンプレートメソッドで実行
    final int paramCount = query != null ? query.size() : 0;
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(addTenantIdFilter(sql.toString(), tenantId))) {
      int paramIndex = 1;
      if (query != null && !query.isEmpty()) {
        for (final String columnName : query.keySet()) {
          preparedStatement.setString(paramIndex, query.get(columnName));
          paramIndex++;
        }
      }
      setTenantIdParameter(preparedStatement, paramIndex, tenantId);

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getLong(1);
        }
      }
    }
    return 0;
  }

  /**
   * 条件を指定してページング検索を実行します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param baseSql 基底SQL
   * @param query 検索条件Map
   * @param isAnd AND検索ならtrue, OR検索ならfalse
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  protected void selectByQueryPaged(
      final Connection connection,
      final String tenantId,
      final String baseSql,
      final Map<String, String> query,
      final boolean isAnd,
      final int page,
      final int size)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || baseSql == null) {
      return;
    }

    // (1) 全件数を取得（countByQuery が tenant_id フィルターを適用済み）
    this.totalCount = countByQuery(connection, tenantId, baseSql, query, isAnd);
    this.pageSize = size;
    this.currentPageIndex = page;

    if (totalCount == 0) {
      return;
    }

    // (2) ページング SQL 構築（tenantId フィルターなし）
    StringBuilder sql = new StringBuilder(baseSql);
    boolean hasWhereClause = baseSql.toUpperCase().contains("WHERE");

    if (query != null && !query.isEmpty()) {
      boolean isFirst = true;
      if (!hasWhereClause) {
        sql.append(" WHERE ");
        hasWhereClause = true;
      } else {
        isFirst = false;
      }

      for (final String columnName : query.keySet()) {
        if (isFirst) {
          sql.append(columnName).append(" = ?");
          isFirst = false;
        } else {
          sql.append(isAnd ? " AND " : " OR ").append(columnName).append(" = ?");
        }
      }
    }

    sql.append(" LIMIT ? OFFSET ?");

    // 新しいテンプレートメソッドで実行
    // addTenantIdFilter() が WHERE 句を追加するため、パラメータ位置が変わる
    final boolean finalHasWhereClause = hasWhereClause;
    List<E> results = executeQuery(
        connection,
        sql.toString(),
        tenantId,
        this::mapResultSet,
        (stmt, paramIndex) -> {
          // executeQuery() が WHERE tenant_id = ? を最初に追加する場合、
          // paramIndex は 2 から開始する（1 は tenant_id）
          // hasWhereClause がない場合、tenant_id が最初のパラメータになる
          int idx = !finalHasWhereClause ? paramIndex + 1 : paramIndex;

          if (query != null && !query.isEmpty()) {
            for (final String columnName : query.keySet()) {
              stmt.setString(idx, query.get(columnName));
              idx++;
            }
          }
          stmt.setInt(idx, size);
          stmt.setInt(idx + 1, (page - 1) * size);
          return idx + 2;
        }
    );
    this.entityLot.addAll(results);
  }

  // ================================
  // TenantId フィルター（新規）
  // ================================

  /**
   * PreparedStatement へのパラメータバインド処理を定義するインターフェース.
   * <p>executeQuery() / executeQueryWithoutTenantFilter() で使用。
   */
  @FunctionalInterface
  protected interface PreparedStatementBinder {
    /**
     * @param stmt バインド対象の PreparedStatement
     * @param startIndex 開始パラメータインデックス（通常は 1）
     * @return 次のパラメータインデックス（tenantId バインド時に使用）
     * @throws SQLException
     */
    int bind(PreparedStatement stmt, int startIndex) throws SQLException;
  }

  /**
   * ResultSet → Entity マッピングを定義するインターフェース.
   * <p>executeQuery() / executeQueryWithoutTenantFilter() で使用。
   */
  @FunctionalInterface
  protected interface ResultSetMapper<T> {
    /**
     * @param rs マッピング対象の ResultSet
     * @return マッピング後のエンティティ
     * @throws SQLException
     */
    T map(ResultSet rs) throws SQLException;
  }

  /**
   * SQL末尾に tenant_id フィルター条件を自動付加します.
   *
   * <p>既に SQL に tenant_id 条件が含まれている場合はスキップします。
   * WHERE句がある場合は AND を使用し、ない場合は WHERE を使用します。
   * LIMIT/OFFSET がある場合は、それらの前に条件を挿入します。
   *
   * @param baseSql 基本SQL（WHERE句を含む場合も含まない場合も可、LIMIT/OFFSETを含む場合も可）
   * @param tenantId テナントID（null 不可）
   * @return tenant_id フィルターが付加された SQL
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected String addTenantIdFilter(final String baseSql, final String tenantId) {
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("TenantId must not be null or empty");
    }
    if (baseSql == null) {
      throw new IllegalArgumentException("baseSql must not be null");
    }
    // 既に tenant_id 条件がある場合はスキップ
    if (baseSql.contains("tenant_id")) {
      return baseSql;
    }
    String trimmedSql = baseSql.trim();
    String upperSql = trimmedSql.toUpperCase();

    // LIMIT/OFFSET の位置を検出
    int limitIndex = upperSql.indexOf(" LIMIT");
    int offsetIndex = upperSql.indexOf(" OFFSET");
    int insertPosition = trimmedSql.length();

    if (limitIndex >= 0) {
      insertPosition = limitIndex;
    } else if (offsetIndex >= 0) {
      insertPosition = offsetIndex;
    }

    String beforeLimitOffset = trimmedSql.substring(0, insertPosition).trim();
    String afterLimitOffset = insertPosition < trimmedSql.length()
        ? " " + trimmedSql.substring(insertPosition).trim()
        : "";

    // WHERE句が含まれているかチェック
    if (beforeLimitOffset.toUpperCase().contains(" WHERE ")) {
      return beforeLimitOffset + " AND tenant_id = ?" + afterLimitOffset;
    } else {
      return beforeLimitOffset + " WHERE tenant_id = ?" + afterLimitOffset;
    }
  }

  /**
   * PreparedStatement に tenant_id パラメータをバインドします.
   *
   * @param stmt バインド対象の PreparedStatement
   * @param paramIndex パラメータインデックス
   * @param tenantId テナントID（null 不可）
   * @throws SQLException
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected void setTenantIdParameter(
      final PreparedStatement stmt, final int paramIndex, final String tenantId)
      throws SQLException {
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("TenantId must not be null or empty");
    }
    stmt.setString(paramIndex, tenantId);
  }

  /**
   * TenantId 必須の統一クエリ実行テンプレート.
   *
   * <p>SQL末尾に自動的に {@code " AND tenant_id = ?"} を付加し、tenantId でフィルターします。
   * 各 Lot クラスの検索メソッドはこのテンプレートを使用してください。
   *
   * @param conn DBコネクション（null の場合は空リストを返す）
   * @param sql WHERE句を含まない基本SQL
   * @param tenantId テナントID（必須）
   * @param mapper ResultSet → Entity マッピング処理
   * @param binder カスタムパラメータバインド処理
   * @return クエリ実行結果のエンティティリスト
   * @throws SQLException
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected List<E> executeQuery(
      final Connection conn,
      final String sql,
      final String tenantId,
      final ResultSetMapper<E> mapper,
      final PreparedStatementBinder binder)
      throws SQLException {
    if (conn == null) {
      return new ArrayList<>();
    }

    // SQL末尾に tenant_id フィルター条件を自動付加
    final String filteredSql = addTenantIdFilter(sql, tenantId);

    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      // ユーザー定義のバインド処理を実行
      int nextParamIndex = binder.bind(stmt, 1);

      // 最後に tenant_id をバインド
      setTenantIdParameter(stmt, nextParamIndex, tenantId);

      List<E> results = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(mapper.map(rs));
        }
      }
      return results;
    }
  }

  /**
   * TenantId フィルター無しのクエリ実行テンプレート（WithoutTenantFilter専用）.
   *
   * <p>⚠️ このメソッドは tenant_id フィルターを適用しません。
   * バッチ処理など全テナント対象のクエリのみで使用してください。
   * メソッド名に {@code WithoutTenantFilter} を含め、コードレビュー対象と明示してください。
   *
   * @param conn DBコネクション（null の場合は空リストを返す）
   * @param sql SQL（WHERE句を含める場合も不含の場合も可）
   * @param mapper ResultSet → Entity マッピング処理
   * @param binder カスタムパラメータバインド処理
   * @return クエリ実行結果のエンティティリスト
   * @throws SQLException
   */
  protected List<E> executeQueryWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final ResultSetMapper<E> mapper,
      final PreparedStatementBinder binder)
      throws SQLException {
    if (conn == null) {
      return new ArrayList<>();
    }

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      binder.bind(stmt, 1);

      List<E> results = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(mapper.map(rs));
        }
      }
      return results;
    }
  }

  /**
   * ResultSetからEntityにマッピングします.
   *
   * @param resultSet レスポンス
   * @return Entity
   * @throws SQLException
   */
  protected abstract E mapResultSet(ResultSet resultSet) throws SQLException;

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    int i = 0;
    for (E entity : entityLot) {
      result.append("(").append(i).append(")").append(entity.toString());
      i++;
    }
    return result.toString();
  }
}
