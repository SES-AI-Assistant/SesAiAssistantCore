package copel.sesproductpackage.core.database.base;

import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * EntityLotの基底クラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public abstract class EntityLotBase<E extends EntityBase> implements Iterable<E> {
  /**
   * DBコネクションが null でないことを確かめます（Lot の各問い合わせで使う）.
   *
   * @param conn DBコネクション
   * @throws IllegalArgumentException DBコネクションが null の場合
   */
  protected static void requireConnection(final Connection conn) {
    EntityBase.requireConnection(conn);
  }

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
    String countSql =
        baseSql.replaceFirst("(?i)\\bSELECT\\s+.*?\\s+\\bFROM\\s+", "SELECT COUNT(*) FROM ");
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
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public abstract void selectAll(final Connection connection, final String tenantId)
      throws SQLException;

  /**
   * テーブルからレコードを全件ページングSELECTし、このLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void selectAllPaged(
      final Connection connection, final String tenantId, final int page, final int size)
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
   * 指定された述語にマッチするエンティティをこのLotから削除します.
   *
   * @param filter 削除対象を決定する述語
   * @return 要素が削除された場合は true、そうでない場合は false
   * @author Copel Co., Ltd.
   */
  public boolean removeIf(java.util.function.Predicate<? super E> filter) {
    return this.entityLot.removeIf(filter);
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
   * 1ページあたりの表示件数を取得します.
   *
   * @return 1ページあたりの表示件数
   */
  public int getPageSize() {
    return pageSize;
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
   * 全文検索用SQL(WHERE句の前まで)を取得します.
   *
   * @return SQL
   */
  protected String getSelectLikeSql() {
    return null;
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
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
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
    EntityBase.requireConnection(connection);
    EntityBase.requireTenantId(tenantId);
    if (firstLikeQuery == null) {
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
    List<E> results =
        executeQuery(
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
            });
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
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
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
    EntityBase.requireConnection(connection);
    EntityBase.requireTenantId(tenantId);
    if (firstLikeQuery == null || baseSql == null) {
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

    // tenant_id は LIMIT/OFFSET より前に挿入されるため、その順でバインドする
    final String filteredPagedSql = addTenantIdFilter(sql.toString(), tenantId);
    List<E> results =
        executeQueryWithoutTenantFilter(
            connection,
            filteredPagedSql,
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
              setTenantIdParameter(stmt, idx++, tenantId);
              stmt.setInt(idx++, size);
              stmt.setInt(idx, (page - 1) * size);
              return idx + 1;
            });
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
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
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
    EntityBase.requireConnection(connection);
    EntityBase.requireTenantId(tenantId);
    if (selectSqlPrefix == null || whereClauseWithoutWhere == null) {
      return;
    }
    if (likeParams == null || likeParams.isEmpty()) {
      return;
    }

    // (1) 件数用 SQL 構築（tenant_id フィルターを追加）
    final String fullSelect = selectSqlPrefix + whereClauseWithoutWhere;
    final String filteredCountSql = addTenantIdFilter(toCountSql(fullSelect), tenantId);
    List<Object> countBindValues = new ArrayList<>(likeParams);
    countBindValues.add(tenantId);
    logExecutedSql(filteredCountSql, countBindValues);

    countWithTenantFilter(
        connection,
        filteredCountSql,
        tenantId,
        (stmt, startIndex) -> {
          int idx = startIndex;
          for (String p : likeParams) {
            stmt.setString(idx++, p);
          }
          return idx;
        });

    // (2) ページング用 SQL。tenant_id は LIMIT/OFFSET より前に挿入されるため、その順でバインドする
    selectPagedWithTenantFilter(
        connection,
        fullSelect,
        tenantId,
        page,
        size,
        (stmt, paramIndex) -> {
          int idx = paramIndex;
          for (String p : likeParams) {
            stmt.setString(idx++, p);
          }
          stmt.setString(idx++, tenantId);
          stmt.setInt(idx++, size);
          stmt.setInt(idx, (page - 1) * size);
          return idx + 1;
        });
  }

  /**
   * 動的WHERE句と詳細フィルターでページング検索を実行し、結果をこのLotに保持します.
   *
   * <p>全文検索条件と詳細フィルター条件をマージしたWHERE句でページング検索を実行します。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param selectSqlPrefix SELECT句から WHERE句の直前までのSQL（例：SELECT ... FROM ... INNER JOIN ... WHERE）
   * @param whereClauseWithoutWhere マージされたWHERE句（WHEREキーワード含まない）
   * @param likeParams 全文検索のLIKEパラメータリスト
   * @param filterParams 詳細フィルターのパラメータリスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  protected void selectByDynamicWhereWithFilterPaged(
      final Connection connection,
      final String tenantId,
      final String selectSqlPrefix,
      final String whereClauseWithoutWhere,
      final List<String> likeParams,
      final List<Object> filterParams,
      final int page,
      final int size)
      throws SQLException {
    this.entityLot = new ArrayList<>();
    EntityBase.requireConnection(connection);
    EntityBase.requireTenantId(tenantId);
    if (selectSqlPrefix == null || whereClauseWithoutWhere == null) {
      return;
    }

    // (1) 件数用 SQL 構築（tenant_id フィルターを追加）
    final String fullSelect = selectSqlPrefix + whereClauseWithoutWhere;
    final String filteredCountSql = addTenantIdFilter(toCountSql(fullSelect), tenantId);
    logSql(filteredCountSql);
    countWithTenantFilter(
        connection,
        filteredCountSql,
        tenantId,
        (stmt, startIndex) -> {
          int idx = bindLikeAndFilterParams(stmt, startIndex, likeParams, filterParams);
          return idx;
        });

    // (2) ページング用 SQL。tenant_id は LIMIT/OFFSET より前に挿入されるため、その順でバインドする
    selectPagedWithTenantFilter(
        connection,
        fullSelect,
        tenantId,
        page,
        size,
        (stmt, paramIndex) -> {
          int idx = bindLikeAndFilterParams(stmt, paramIndex, likeParams, filterParams);
          setTenantIdParameter(stmt, idx++, tenantId);
          stmt.setInt(idx++, size);
          stmt.setInt(idx++, (page - 1) * size);
          return idx;
        });
  }

  /**
   * 件数用の SQL を実行し、総件数を保持します（tenant_id 条件付き）.
   *
   * <p>結果の行が無い場合は総件数を変更しません。
   *
   * @param connection DBコネクション
   * @param filteredCountSql tenant_id 条件を付加済みの件数 SQL
   * @param tenantId テナントID
   * @param binder 検索条件のパラメータをバインドする処理。次の位置を返すこと
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  private void countWithTenantFilter(
      final Connection connection,
      final String filteredCountSql,
      final String tenantId,
      final PreparedStatementBinder binder)
      throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement(filteredCountSql)) {
      int paramIndex = binder.bind(preparedStatement, 1);
      setTenantIdParameter(preparedStatement, paramIndex, tenantId);

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          this.totalCount = resultSet.getLong(1);
        }
      }
    }
  }

  /**
   * ページ情報を保持し、ページ分のデータを取得してこのLotに追加します（tenant_id 条件付き）.
   *
   * <p>総件数が 0 件の場合はデータ取得を行いません。
   *
   * @param connection DBコネクション
   * @param fullSelect WHERE句までを含む SELECT SQL（LIMIT/OFFSET と tenant_id 条件は含まない）
   * @param tenantId テナントID
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @param binder パラメータをバインドする処理。tenant_id の後に LIMIT と OFFSET をバインドすること
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  private void selectPagedWithTenantFilter(
      final Connection connection,
      final String fullSelect,
      final String tenantId,
      final int page,
      final int size,
      final PreparedStatementBinder binder)
      throws SQLException {
    this.pageSize = size;
    this.currentPageIndex = page;

    if (this.totalCount == 0) {
      return;
    }

    final String filteredPagedSql = addTenantIdFilter(fullSelect + " LIMIT ? OFFSET ?", tenantId);
    List<E> results =
        executeQueryWithoutTenantFilter(connection, filteredPagedSql, this::mapResultSet, binder);
    this.entityLot.addAll(results);
  }

  /**
   * 全文検索のパラメータと詳細フィルターのパラメータを順にバインドします.
   *
   * <p>どちらの一覧も null の場合は、その分のバインドを行いません。
   *
   * @param stmt バインド対象の PreparedStatement
   * @param startIndex 開始パラメータインデックス
   * @param likeParams 全文検索のLIKEパラメータリスト（null可）
   * @param filterParams 詳細フィルターのパラメータリスト（null可）
   * @return 次のパラメータインデックス
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  private int bindLikeAndFilterParams(
      final PreparedStatement stmt,
      final int startIndex,
      final List<String> likeParams,
      final List<Object> filterParams)
      throws SQLException {
    int idx = startIndex;
    if (likeParams != null) {
      for (String p : likeParams) {
        stmt.setString(idx++, p);
      }
    }
    if (filterParams != null) {
      for (Object param : filterParams) {
        bindParameter(stmt, idx++, param);
      }
    }
    return idx;
  }

  /**
   * PreparedStatement に tenant_id パラメータをバインドします.
   *
   * @param stmt バインド対象の PreparedStatement
   * @param paramIndex パラメータインデックス
   * @param tenantId テナントID（null 不可）
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
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
   * PreparedStatementにパラメータを型に応じてバインドするヘルパーメソッド.
   *
   * @param stmt PreparedStatement
   * @param paramIndex パラメータインデックス（1-based）
   * @param param バインドするパラメータ値
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  protected void bindParameter(
      final PreparedStatement stmt, final int paramIndex, final Object param) throws SQLException {
    if (param instanceof java.math.BigDecimal) {
      stmt.setBigDecimal(paramIndex, (java.math.BigDecimal) param);
    } else if (param instanceof java.sql.Timestamp) {
      stmt.setTimestamp(paramIndex, (java.sql.Timestamp) param);
    } else if (param instanceof OriginalDateTime) {
      // OriginalDateTime は独自の日時ラッパー型のため、JDBCドライバが型推論できる
      // java.sql.Timestamp に変換してからバインドする必要がある（さもないと
      // 「Can't infer the SQL type」エラーになる）。
      stmt.setTimestamp(paramIndex, ((OriginalDateTime) param).toTimestamp());
    } else if (param instanceof Enum<?>) {
      stmt.setString(paramIndex, ((Enum<?>) param).name());
    } else if (param instanceof Integer) {
      stmt.setInt(paramIndex, (Integer) param);
    } else if (param instanceof String) {
      stmt.setString(paramIndex, (String) param);
    } else if (param instanceof Double) {
      stmt.setDouble(paramIndex, (Double) param);
    } else if (param instanceof Long) {
      stmt.setLong(paramIndex, (Long) param);
    } else {
      stmt.setObject(paramIndex, param);
    }
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
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合
   */
  protected long countByQuery(
      final Connection connection,
      final String tenantId,
      final String baseSql,
      final Map<String, String> query,
      final boolean isAnd)
      throws SQLException {
    EntityBase.requireConnection(connection);
    if (baseSql == null) {
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

    long count = 0;
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
          count = resultSet.getLong(1);
        }
      }
    }
    return count;
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
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
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
    EntityBase.requireConnection(connection);
    EntityBase.requireTenantId(tenantId);
    if (baseSql == null) {
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
    // baseSql に ORDER BY / GROUP BY / HAVING が含まれる場合、
    // WHERE/AND条件はそれらの句より前に挿入しないと構文エラーになるため、挿入位置を検出する。
    final String upperBaseSql = baseSql.toUpperCase();
    int clauseInsertPosition = baseSql.length();
    for (final String keyword : new String[] {" ORDER BY", " GROUP BY", " HAVING"}) {
      final int keywordIndex = upperBaseSql.indexOf(keyword);
      if (keywordIndex >= 0 && keywordIndex < clauseInsertPosition) {
        clauseInsertPosition = keywordIndex;
      }
    }
    final String beforeClause = baseSql.substring(0, clauseInsertPosition);
    final String afterClause = baseSql.substring(clauseInsertPosition);
    boolean hasWhereClause = beforeClause.toUpperCase().contains("WHERE");

    StringBuilder conditionBuilder = new StringBuilder();
    if (query != null && !query.isEmpty()) {
      boolean isFirst = true;
      if (!hasWhereClause) {
        conditionBuilder.append(" WHERE ");
        hasWhereClause = true;
      } else {
        isFirst = false;
      }

      for (final String columnName : query.keySet()) {
        if (isFirst) {
          conditionBuilder.append(columnName).append(" = ?");
          isFirst = false;
        } else {
          conditionBuilder.append(isAnd ? " AND " : " OR ").append(columnName).append(" = ?");
        }
      }
    }

    StringBuilder sql =
        new StringBuilder(beforeClause).append(conditionBuilder).append(afterClause);
    sql.append(" LIMIT ? OFFSET ?");

    // tenant_id フィルターを SQL に追加
    final String filteredSql = addTenantIdFilter(sql.toString(), tenantId);

    try (PreparedStatement stmt = connection.prepareStatement(filteredSql)) {
      List<Object> bindValues = new ArrayList<>();

      // パラメータ設定順序:
      // 1. query 値（WHERE 句内）
      // 2. tenant_id（addTenantIdFilter() で追加された AND tenant_id = ?）
      // 3. LIMIT
      // 4. OFFSET
      int paramIndex = 1;

      // (1) query 値をセット
      if (query != null && !query.isEmpty()) {
        for (final String columnName : query.keySet()) {
          String value = query.get(columnName);
          stmt.setString(paramIndex, value);
          bindValues.add(value);
          paramIndex++;
        }
      }

      // (2) tenant_id をセット
      setTenantIdParameter(stmt, paramIndex, tenantId);
      bindValues.add(tenantId);
      paramIndex++;

      // (3) LIMIT, OFFSET をセット
      stmt.setInt(paramIndex, size);
      bindValues.add(size);
      stmt.setInt(paramIndex + 1, (page - 1) * size);
      bindValues.add((page - 1) * size);

      // SQL と バインド値をログ出力
      logExecutedSql(filteredSql, bindValues);

      List<E> results = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(mapResultSet(rs));
        }
      }
      this.entityLot.addAll(results);
    }
  }

  // ================================
  // SQL ログ出力
  // ================================

  /**
   * SQL をログ出力します（シンプル版）.
   *
   * @param sql SQL文
   */
  protected void logSql(final String sql) {
    if (log.isInfoEnabled()) {
      log.info("[SQL] {}", sql);
    }
  }

  /**
   * 実行される SQL を バインド値を含めてログ出力します.
   *
   * <p>パラメータプレースホルダー(?) をバインド値で置換して、実際の SQL を標準出力（ログ）に出力します。
   *
   * @param sql パラメータプレースホルダーを含む SQL
   * @param bindValues バインド値のリスト（順序は SQL 内のプレースホルダーの順序と対応）
   */
  protected void logExecutedSql(final String sql, final List<Object> bindValues) {
    if (!log.isInfoEnabled()) {
      return;
    }

    String executedSql = sql;
    if (bindValues != null && !bindValues.isEmpty()) {
      // バインド値を SQL に置換
      for (final Object value : bindValues) {
        String replacement;
        if (value == null) {
          replacement = "NULL";
        } else if (value instanceof String) {
          replacement = "'" + ((String) value).replace("'", "''") + "'";
        } else {
          replacement = String.valueOf(value);
        }
        executedSql = executedSql.replaceFirst("\\?", Matcher.quoteReplacement(replacement));
      }
    }

    log.info("[SQL] {}", executedSql);
  }

  // ================================
  // TenantId フィルター（新規）
  // ================================

  /**
   * PreparedStatement へのパラメータバインド処理を定義するインターフェース.
   *
   * <p>executeQuery() / executeQueryWithoutTenantFilter() で使用。
   */
  @FunctionalInterface
  protected interface PreparedStatementBinder {
    /**
     * PreparedStatement に値をバインドし、次のパラメータインデックスを返します。
     *
     * @param stmt バインド対象の PreparedStatement
     * @param startIndex 開始パラメータインデックス（通常は 1）
     * @return 次のパラメータインデックス（tenantId バインド時に使用）
     * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
     */
    int bind(PreparedStatement stmt, int startIndex) throws SQLException;
  }

  /**
   * ResultSet → Entity マッピングを定義するインターフェース.
   *
   * <p>executeQuery() / executeQueryWithoutTenantFilter() で使用。
   */
  @FunctionalInterface
  protected interface ResultSetMapper<T> {
    /**
     * 1行分の ResultSet を、エンティティへ変換します。
     *
     * @param rs マッピング対象の ResultSet
     * @return マッピング後のエンティティ
     * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
     */
    T map(ResultSet rs) throws SQLException;
  }

  /**
   * SQL末尾に tenant_id フィルター条件を自動付加します.
   *
   * <p>既に WHERE 句に tenant_id 条件が含まれている場合はスキップします。 WHERE句がある場合は AND を使用し、ない場合は WHERE を使用します。
   * LIMIT/OFFSET がある場合は、それらの前に条件を挿入します。
   *
   * @param baseSql 基本SQL（WHERE句を含む場合も含まない場合も可、LIMIT/OFFSETを含む場合も可）
   * @param tenantId テナントID（null 不可）
   * @return tenant_id フィルターが付加された SQL
   * @throws IllegalArgumentException tenantId が null または空文字列の場合、または TenantFilterSql
   *     が拒否する形（括弧内の副問い合わせ、SQL コメント、解析できない表の指定など）の場合
   */
  protected String addTenantIdFilter(final String baseSql, final String tenantId) {
    return new TenantFilterSql(baseSql).withTenantFilter(tenantId);
  }

  /**
   * TenantId 必須の統一クエリ実行テンプレート.
   *
   * <p>SQL末尾に自動的に {@code " AND tenant_id = ?"} を付加し、tenantId でフィルターします。 各 Lot
   * クラスの検索メソッドはこのテンプレートを使用してください。
   *
   * @param conn DBコネクション（null の場合は空リストを返す）
   * @param sql WHERE句を含まない基本SQL
   * @param tenantId テナントID（必須）
   * @param mapper ResultSet → Entity マッピング処理
   * @param binder カスタムパラメータバインド処理
   * @return クエリ実行結果のエンティティリスト
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected List<E> executeQuery(
      final Connection conn,
      final String sql,
      final String tenantId,
      final ResultSetMapper<E> mapper,
      final PreparedStatementBinder binder)
      throws SQLException {
    EntityBase.requireConnection(conn);
    EntityBase.requireTenantId(tenantId);

    // SQL末尾に tenant_id フィルター条件を自動付加
    final String filteredSql = addTenantIdFilter(sql, tenantId);

    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      // ユーザー定義のバインド処理を実行
      int nextParamIndex = binder.bind(stmt, 1);

      // 最後に tenant_id をバインド
      setTenantIdParameter(stmt, nextParamIndex, tenantId);

      // SQL を実行前にログ出力
      logSql(filteredSql);

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
   * <p>⚠️ このメソッドは tenant_id フィルターを適用しません。 バッチ処理など全テナント対象のクエリのみで使用してください。 メソッド名に {@code
   * WithoutTenantFilter} を含め、コードレビュー対象と明示してください。
   *
   * @param conn DBコネクション（null の場合は空リストを返す）
   * @param sql SQL（WHERE句を含める場合も不含の場合も可）
   * @param mapper ResultSet → Entity マッピング処理
   * @param binder カスタムパラメータバインド処理
   * @return クエリ実行結果のエンティティリスト
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  protected List<E> executeQueryWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final ResultSetMapper<E> mapper,
      final PreparedStatementBinder binder)
      throws SQLException {
    EntityBase.requireConnection(conn);

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      binder.bind(stmt, 1);

      logSql(sql);

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
   * TenantId フィルター無しのDELETE実行テンプレート（WithoutTenantFilter専用）.
   *
   * <p>⚠️ このメソッドは tenant_id フィルターを適用しません。 バッチ処理など全テナント対象のDELETEのみで使用してください。 メソッド名に {@code
   * WithoutTenantFilter} を含め、コードレビュー対象と明示してください。
   *
   * @param conn DBコネクション（null の場合は 0 を返す）
   * @param sql DELETE SQL（WHERE句を含める）
   * @param binder カスタムパラメータバインド処理
   * @return 削除したレコード数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合
   */
  protected int executeDeleteWithoutTenantFilter(
      final Connection conn, final String sql, final PreparedStatementBinder binder)
      throws SQLException {
    EntityBase.requireConnection(conn);

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      binder.bind(stmt, 1);

      logSql(sql);

      return stmt.executeUpdate();
    }
  }

  /**
   * ベクトル検索パラメータをバインドするインターフェース（VectorPagedQuery用）.
   *
   * <p>SQL のプレースホルダーに対応するパラメータ（ベクトル値・スレッショルド値を含む）をバインドします。
   */
  @FunctionalInterface
  protected interface VectorParameterBinder {
    /**
     * SQL のパラメータを PreparedStatement にバインドします。
     *
     * @param stmt バインド対象の PreparedStatement
     * @param startIndex 開始パラメータインデックス
     * @param vectorValue ベクトル値（文字列形式）
     * @param similarityThreshold 類似度スレッショルド（0.0で閾値なし）
     * @return 次のパラメータインデックス
     * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
     */
    int bind(PreparedStatement stmt, int startIndex, String vectorValue, double similarityThreshold)
        throws SQLException;
  }

  /**
   * 類似度と詳細条件を付けたベクトル検索を、件数の問い合わせと指定ページの問い合わせで実行し、結果をこのLotに保持します.
   *
   * <p>件数の問い合わせは tenant_id フィルターを付けて実行し、件数が0件の場合は指定ページの問い合わせを行いません。
   * ベクトルは検索と並べ替えの両方で使うため、指定ページの問い合わせでは2回バインドします。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param pagedSql 指定ページの検索SQL（末尾に LIMIT と OFFSET のプレースホルダを含む）
   * @param countSql 件数の検索SQL（tenant_id フィルターは付けていない）
   * @param query 検索ベクトル
   * @param filterParams 詳細条件のパラメータリスト（空の場合も空のリストを渡す）
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  protected void executeVectorWithFilterPaged(
      final Connection connection,
      final String tenantId,
      final String pagedSql,
      final String countSql,
      final Vector query,
      final List<Object> filterParams,
      final double similarityThreshold,
      final int page,
      final int size)
      throws SQLException {
    EntityBase.requireConnection(connection);
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(addTenantIdFilter(countSql, tenantId))) {
      int paramIndex = 1;
      String vectorStr = query.toString();
      preparedStatement.setString(paramIndex++, vectorStr);
      preparedStatement.setDouble(paramIndex++, similarityThreshold);
      for (Object param : filterParams) {
        bindParameter(preparedStatement, paramIndex++, param);
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

    List<E> results =
        executeQuery(
            connection,
            pagedSql,
            tenantId,
            this::mapVectorResultSet,
            (stmt, paramIndex) -> {
              int idx = paramIndex;
              String vectorStr = query.toString();
              stmt.setString(idx, vectorStr);
              stmt.setString(idx + 1, vectorStr);
              stmt.setDouble(idx + 2, similarityThreshold);
              // 詳細条件のパラメータを、ベクトルの後ろからバインドする
              int filterParamStartIdx = idx + 3;
              for (int i = 0; i < filterParams.size(); i++) {
                bindParameter(stmt, filterParamStartIdx + i, filterParams.get(i));
              }
              // nextIdx は addTenantIdFilter が挿入する tenant_id 用プレースホルダーの位置のため空けておく
              int nextIdx = filterParamStartIdx + filterParams.size();
              stmt.setInt(nextIdx + 1, size);
              stmt.setInt(nextIdx + 2, (page - 1) * size);
              return nextIdx;
            });
    this.entityLot = results;
  }

  /**
   * ベクトル検索 + ページング実行テンプレート（TenantId フィルター必須）.
   *
   * <p>件数の取得と一覧の取得で、バインドする処理を分けて受け取ります。件数の SQL は SELECT 句が無いため、件数用のバインダーで WHERE
   * 句のパラメータだけを順にバインドします。 SQL 末尾には {@code " AND tenant_id = ?"} を付加します。ページング情報（totalCount, pageSize,
   * currentPageIndex）を自動的に this に設定します。
   *
   * @param conn DBコネクション（null の場合は IllegalArgumentException を投げる）
   * @param baseQuerySql SELECT ... FROM ... WHERE ... のベースSQL（LIMIT/OFFSET なし）
   * @param tenantId テナントID（必須）
   * @param vectorValue ベクトル値（文字列形式、例：「[1.0, 2.0, 3.0]」）
   * @param similarityThreshold 類似度スレッショルド（0.0 で閾値なし、つまり距離計算のみ）
   * @param countBinder 件数の SQL（SELECT 句なし）のパラメータをバインドする処理
   * @param page ページ番号（1-based）
   * @param size 1ページあたりの件数
   * @param mapper ResultSet → Entity マッピング処理
   * @param binder 一覧の SQL（SELECT 句あり）のパラメータをバインドする処理
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合、またはテナントIDが null または空文字列の場合
   */
  protected void executeVectorPagedQuery(
      final Connection conn,
      final String baseQuerySql,
      final String tenantId,
      final String vectorValue,
      final double similarityThreshold,
      final VectorParameterBinder countBinder,
      final int page,
      final int size,
      final ResultSetMapper<E> mapper,
      final VectorParameterBinder binder)
      throws SQLException {
    if (conn == null) {
      throw new IllegalArgumentException("DBコネクションは null にできません");
    }

    // 件数: ORDER BY を除いた COUNT SQL。件数用のバインダーで WHERE 句のパラメータをバインドする
    final String countSql = addTenantIdFilter(toCountSql(baseQuerySql), tenantId);
    long count = 0;
    try (PreparedStatement stmt = conn.prepareStatement(countSql)) {
      int nextParamIndex = countBinder.bind(stmt, 1, vectorValue, similarityThreshold);
      setTenantIdParameter(stmt, nextParamIndex, tenantId);
      logSql(countSql);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          count = rs.getLong(1);
        }
      }
    }
    this.totalCount = count;

    // 一覧: tenant_id の後ろに LIMIT と OFFSET をバインドする
    final String pagedSql = addTenantIdFilter(baseQuerySql, tenantId) + " LIMIT ? OFFSET ?";
    try (PreparedStatement stmt = conn.prepareStatement(pagedSql)) {
      int nextParamIndex = binder.bind(stmt, 1, vectorValue, similarityThreshold);
      setTenantIdParameter(stmt, nextParamIndex, tenantId);
      nextParamIndex++;
      stmt.setInt(nextParamIndex, size);
      nextParamIndex++;
      stmt.setInt(nextParamIndex, (page - 1) * size);
      logSql(pagedSql);
      List<E> results = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(mapper.map(rs));
        }
      }
      this.entityLot = results;
    }

    this.pageSize = size;
    this.currentPageIndex = page;
  }

  /**
   * TTL 期限切れレコード取得テンプレート（TenantId フィルター無し、バッチ処理専用）.
   *
   * <p>⚠️ このメソッドは tenant_id フィルターを適用しません。 バッチ処理など全テナント対象のクエリのみで使用してください。 メソッド名に {@code
   * WithoutTenantFilter} を含め、コードレビュー対象と明示してください。
   *
   * @param conn DBコネクション
   * @param selectPrefix SELECT ... FROM ... WHERE ((ttl IS NOT NULL AND ttl < NOW()) OR ...
   *     INTERVAL ' のプレフィックス
   * @param selectSuffix days') < NOW())) ... のサフィックス
   * @param ttlDays TTL日数
   * @param offset オフセット
   * @param limit リミット
   * @param mapper ResultSet → Entity マッピング処理
   * @return クエリ実行結果のエンティティリスト
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  protected List<E> executeTtlExpiredQueryWithoutTenantFilter(
      final Connection conn,
      final String selectPrefix,
      final String selectSuffix,
      final int ttlDays,
      final int offset,
      final int limit,
      final ResultSetMapper<E> mapper)
      throws SQLException {
    EntityBase.requireConnection(conn);

    final String sql = selectPrefix + ttlDays + selectSuffix;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      int paramIndex = 1;

      // LIMIT をバインド
      stmt.setInt(paramIndex, limit);
      paramIndex++;

      // OFFSET をバインド
      stmt.setInt(paramIndex, offset);

      logSql(sql);

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
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  protected abstract E mapResultSet(ResultSet resultSet) throws SQLException;

  /**
   * ベクトル検索の1行分の結果をエンティティに変換します.
   *
   * <p>ベクトル検索のSQLは distance 列を返すため、距離を保持するエンティティはこのメソッドを上書きして距離を設定する。 既定では距離を扱わず {@link
   * #mapResultSet(ResultSet)} と同じ。
   *
   * @param resultSet 結果セット
   * @return 変換したエンティティ
   * @throws SQLException DB操作エラー
   */
  protected E mapVectorResultSet(ResultSet resultSet) throws SQLException {
    return mapResultSet(resultSet);
  }

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
