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
   * @throws SQLException
   */
  public abstract void selectAll(final Connection connection) throws SQLException;

  /**
   * テーブルからレコードを全件ページングSELECTし、このLotに保持します.
   *
   * @param connection DBコネクション
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void selectAllPaged(final Connection connection, final int page, final int size)
      throws SQLException {
    this.selectByQueryPaged(connection, getSelectAllSql(), null, true, page, size);
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
   * @param andQuery カラム名と検索値をkey-valueで持つMap
   * @throws SQLException
   */
  public void selectByAndQuery(final Connection connection, final Map<String, String> andQuery)
      throws SQLException {
    this.selectByQuery(connection, getSelectSql(), andQuery, true);
  }

  /**
   * SELECTをOR条件で実行する.
   *
   * @param connection DBコネクション
   * @param orQuery カラム名と検索値をkey-valueで持つMap
   * @throws SQLException
   */
  public void selectByOrQuery(final Connection connection, final Map<String, String> orQuery)
      throws SQLException {
    this.selectByQuery(connection, getSelectSql(), orQuery, false);
  }

  /**
   * 指定したカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param column 検索対象カラム
   * @param query 検索文字列
   * @throws SQLException
   */
  public void searchByField(final Connection connection, final String column, final String query)
      throws SQLException {
    this.selectByLikeQuery(connection, getSelectLikeSql(), column, query, null);
  }

  /**
   * 指定したカラムで全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param column 検索対象カラム
   * @param query 検索文字列
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFieldPaged(
      final Connection connection,
      final String column,
      final String query,
      final int page,
      final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(connection, getSelectLikeSql(), column, query, null, page, size);
  }

  /**
   * 指定したカラムに対して複数条件で全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param column 検索対象カラム
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @throws SQLException
   */
  public void searchByField(
      final Connection connection,
      final String column,
      final String firstLikeQuery,
      final List<LogicalOperators> query)
      throws SQLException {
    this.selectByLikeQuery(connection, getSelectLikeSql(), column, firstLikeQuery, query);
  }

  /**
   * 指定したカラムに対して複数条件で全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param column 検索対象カラム
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void searchByFieldPaged(
      final Connection connection,
      final String column,
      final String firstLikeQuery,
      final List<LogicalOperators> query,
      final int page,
      final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(
        connection, getSelectLikeSql(), column, firstLikeQuery, query, page, size);
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
   * @param baseSql 基底SQL
   * @param query 検索条件Map
   * @param isAnd AND検索ならtrue, OR検索ならfalse
   * @throws SQLException
   */
  protected void selectByQuery(
      final Connection connection,
      final String baseSql,
      final Map<String, String> query,
      final boolean isAnd)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || query == null || query.isEmpty()) {
      return;
    }

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

    try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
      int i = 1;
      for (final String columnName : query.keySet()) {
        preparedStatement.setString(i, query.get(columnName));
        i++;
      }

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
    }
  }

  /**
   * あいまい検索を実行します.
   *
   * @param connection DBコネクション
   * @param baseSql 基底SQL(LIKE句の前まで)
   * @param columnName 検索対象カラム名
   * @param firstLikeQuery 最初の検索ワード
   * @param query 追加の検索条件リスト
   * @throws SQLException
   */
  protected void selectByLikeQuery(
      final Connection connection,
      final String baseSql,
      final String columnName,
      final String firstLikeQuery,
      final List<LogicalOperators> query)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null || firstLikeQuery == null) {
      return;
    }

    StringBuilder sql = new StringBuilder(baseSql);
    if (query != null) {
      for (final LogicalOperators logicalOperator : query) {
        if (logicalOperator != null) {
          logicalOperator.setColumnName(columnName);
          sql.append(logicalOperator.getLikeQuery());
        }
      }
    }

    try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
      preparedStatement.setString(1, "%" + firstLikeQuery + "%");
      if (query != null && !query.isEmpty()) {
        for (int i = 0; i < query.size(); i++) {
          LogicalOperators operator = query.get(i);
          if (operator != null) {
            preparedStatement.setString(i + 2, "%" + operator.getValue() + "%");
          }
        }
      }

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
    }
  }

  /**
   * あいまい検索をページングで実行します.
   *
   * @param connection DBコネクション
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

    // (1) 件数用のSQLを構築して実行
    StringBuilder countSql = new StringBuilder(toCountSql(baseSql));
    if (query != null) {
      for (final LogicalOperators logicalOperator : query) {
        if (logicalOperator != null) {
          logicalOperator.setColumnName(columnName);
          countSql.append(logicalOperator.getLikeQuery());
        }
      }
    }

    try (PreparedStatement preparedStatement = connection.prepareStatement(countSql.toString())) {
      preparedStatement.setString(1, "%" + firstLikeQuery + "%");
      if (query != null && !query.isEmpty()) {
        for (int i = 0; i < query.size(); i++) {
          LogicalOperators operator = query.get(i);
          if (operator != null) {
            preparedStatement.setString(i + 2, "%" + operator.getValue() + "%");
          }
        }
      }
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

    // (2) ページング用SQLを構築して実行
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

    try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
      preparedStatement.setString(1, "%" + firstLikeQuery + "%");
      int paramIndex = 2;
      if (query != null && !query.isEmpty()) {
        for (LogicalOperators operator : query) {
          if (operator != null) {
            preparedStatement.setString(paramIndex++, "%" + operator.getValue() + "%");
          }
        }
      }
      preparedStatement.setInt(paramIndex++, size);
      preparedStatement.setInt(paramIndex, (page - 1) * size);

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
    }
  }

  /**
   * 動的 WHERE 句（LIKE / NOT LIKE 等）でページング全文検索を実行します.
   *
   * @param connection DBコネクション
   * @param selectSqlPrefix {@code SELECT ... FROM ... WHERE } まで（末尾に WHERE を含む）
   * @param whereClauseWithoutWhere WHERE に続く条件式のみ（例: {@code (a LIKE ?) OR (b LIKE ?)}）
   * @param likeParams プレースホルダに順にバインドする値
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  protected void selectByDynamicWherePaged(
      final Connection connection,
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

    final String fullSelect = selectSqlPrefix + whereClauseWithoutWhere;
    final String countSql = toCountSql(fullSelect);

    try (PreparedStatement preparedStatement = connection.prepareStatement(countSql)) {
      int i = 1;
      for (String p : likeParams) {
        preparedStatement.setString(i++, p);
      }
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

    final String pagedSql = fullSelect + " LIMIT ? OFFSET ?";
    try (PreparedStatement preparedStatement = connection.prepareStatement(pagedSql)) {
      int pi = 1;
      for (String p : likeParams) {
        preparedStatement.setString(pi++, p);
      }
      preparedStatement.setInt(pi++, size);
      preparedStatement.setInt(pi, (page - 1) * size);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
    }
  }

  /**
   * 条件を指定して件数を取得します.
   *
   * @param connection DBコネクション
   * @param baseSql 基底SQL(SELECT * FROM ...)
   * @param query 検索条件Map
   * @param isAnd AND検索ならtrue, OR検索ならfalse
   * @return 件数
   * @throws SQLException
   */
  protected long countByQuery(
      final Connection connection,
      final String baseSql,
      final Map<String, String> query,
      final boolean isAnd)
      throws SQLException {
    if (connection == null || baseSql == null) {
      return 0;
    }

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

    try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
      if (query != null && !query.isEmpty()) {
        int i = 1;
        for (final String columnName : query.keySet()) {
          preparedStatement.setString(i, query.get(columnName));
          i++;
        }
      }

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
   * @param baseSql 基底SQL
   * @param query 検索条件Map
   * @param isAnd AND検索ならtrue, OR検索ならfalse
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  protected void selectByQueryPaged(
      final Connection connection,
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

    // (1) 全件数を取得
    this.totalCount = countByQuery(connection, baseSql, query, isAnd);
    this.pageSize = size;
    this.currentPageIndex = page;

    if (totalCount == 0) {
      return;
    }

    // (2) ページングSQLを構築
    StringBuilder sql = new StringBuilder(baseSql);
    if (query != null && !query.isEmpty()) {
      boolean isFirst = true;
      if (!baseSql.toUpperCase().contains("WHERE")) {
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

    sql.append(" LIMIT ? OFFSET ?");

    try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
      int i = 1;
      if (query != null && !query.isEmpty()) {
        for (final String columnName : query.keySet()) {
          preparedStatement.setString(i, query.get(columnName));
          i++;
        }
      }

      preparedStatement.setInt(i++, size);
      preparedStatement.setInt(i, (page - 1) * size);

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          this.entityLot.add(mapResultSet(resultSet));
        }
      }
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
