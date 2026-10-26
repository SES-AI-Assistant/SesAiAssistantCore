package copel.sesproductpackage.core.database.base;

import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.FulltextConditionsWhereClause;
import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 要員・案件の情報テーブルの Lot に共通する検索処理を持つ基底クラスです.
 *
 * <p>raw_content 列への全文検索（LIKE）と、類似度閾値によるベクトル検索（距離付き）を提供します。 派生クラスは、検索 SQL と距離付きの行変換を実装します。
 *
 * @param <E> Lot が保持するエンティティの型
 * @author Copel Co., Ltd.
 */
public abstract class SesAiTableLotBase<E extends SesAiTableEntityBase> extends EntityLotBase<E> {

  /**
   * ベクトル検索（類似度閾値）の基底SQLを返します（LIMIT と距離列を含まないもの）.
   *
   * @return 基底SQL
   */
  protected abstract String getThresholdSql();

  /**
   * 全文検索の複合条件で使う基底SQLを返します（WHERE 句の前まで）.
   *
   * @return 基底SQL
   */
  protected abstract String getRawContentFulltextSql();

  /**
   * 1行分の結果を、距離（distance 列）を設定したエンティティに変換します.
   *
   * @param resultSet 結果セット（distance 列を含む）
   * @return 距離を設定したエンティティ
   * @throws SQLException DB操作エラー
   */
  protected abstract E mapResultSetWithDistance(ResultSet resultSet) throws SQLException;

  /**
   * ベクトル検索の1行分の結果を、距離（distance 列）を設定したエンティティに変換します.
   *
   * @param resultSet 結果セット（distance 列を含む）
   * @return 距離を設定したエンティティ
   * @throws SQLException DB操作エラー
   */
  @Override
  protected E mapVectorResultSet(ResultSet resultSet) throws SQLException {
    return mapResultSetWithDistance(resultSet);
  }

  /**
   * 類似度閾値を用いてベクトル検索を実行し結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrieveWithThreshold(
      Connection connection, String tenantId, Vector query, double similarityThreshold, int limit)
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
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrievePagedWithThreshold(
      Connection connection,
      String tenantId,
      Vector query,
      double similarityThreshold,
      int page,
      int size)
      throws SQLException {
    requireConnection(connection);
    if (query == null) {
      return;
    }

    executeVectorPagedQuery(
        connection,
        getThresholdSql(),
        tenantId,
        query.toString(),
        similarityThreshold,
        (stmt, paramIndex, vectorValue, threshold) -> {
          stmt.setString(paramIndex, vectorValue);
          stmt.setDouble(paramIndex + 1, threshold);
          return paramIndex + 2;
        },
        page,
        size,
        this::mapResultSetWithDistance,
        (stmt, paramIndex, vectorValue, threshold) -> {
          stmt.setString(paramIndex, vectorValue);
          stmt.setString(paramIndex + 1, vectorValue);
          stmt.setDouble(paramIndex + 2, threshold);
          return paramIndex + 3;
        });
  }

  /**
   * raw_contentカラムで全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索条件Map
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByRawContent(
      final Connection connection, final String tenantId, final String query) throws SQLException {
    this.selectByLikeQuery(connection, tenantId, getSelectLikeSql(), "raw_content", query, null);
  }

  /**
   * raw_contentカラムに対して複数条件で全文検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByRawContent(
      final Connection connection,
      final String tenantId,
      final String firstLikeQuery,
      final List<LogicalOperators> query)
      throws SQLException {
    this.selectByLikeQuery(
        connection, tenantId, getSelectLikeSql(), "raw_content", firstLikeQuery, query);
  }

  /**
   * raw_contentカラムで全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索文字列
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByRawContentPaged(
      final Connection connection,
      final String tenantId,
      final String query,
      final int page,
      final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(
        connection, tenantId, getSelectLikeSql(), "raw_content", query, null, page, size);
  }

  /**
   * raw_contentカラムに対して複数条件で全文検索をページングで実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param firstLikeQuery 1つ目のLIKE句の検索条件
   * @param query 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByRawContentPaged(
      final Connection connection,
      final String tenantId,
      final String firstLikeQuery,
      final List<LogicalOperators> query,
      final int page,
      final int size)
      throws SQLException {
    this.selectByLikeQueryPaged(
        connection, tenantId, getSelectLikeSql(), "raw_content", firstLikeQuery, query, page, size);
  }

  /**
   * raw_content に対して複合条件（AND/OR/NOT）で全文検索をページング実行します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param conditions API の conditions 配列と同一の論理
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByRawContentPaged(
      final Connection connection,
      final String tenantId,
      final List<FulltextCondition> conditions,
      final int page,
      final int size)
      throws SQLException {
    FulltextConditionsWhereClause.Built built =
        FulltextConditionsWhereClause.build("raw_content", conditions);
    this.selectByDynamicWherePaged(
        connection,
        tenantId,
        getRawContentFulltextSql(),
        built.getWhereClauseWithoutWhereKeyword(),
        built.getLikeParams(),
        page,
        size);
  }
}
