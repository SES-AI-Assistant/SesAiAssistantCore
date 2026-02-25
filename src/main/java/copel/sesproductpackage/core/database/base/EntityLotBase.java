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
 * @author 鈴木一矢
 */
public abstract class EntityLotBase<E extends EntityBase> implements Iterable<E> {
  // ================================
  // メンバ
  // ================================
  /** エンティティLot. */
  protected Collection<E> entityLot;

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
    if (connection == null || query == null) {
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
