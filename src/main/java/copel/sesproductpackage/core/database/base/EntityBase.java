package copel.sesproductpackage.core.database.base;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * エンティティの基底クラス.
 *
 * @author Copel Co., Ltd.
 */
@Data
public abstract class EntityBase implements Comparable<EntityBase> {
  private static final Logger log = LoggerFactory.getLogger(EntityBase.class);

  /** 登録日時 / register_date */
  @Column(required = true, physicalName = "register_date", logicalName = "登録日時")
  protected OriginalDateTime registerDate;

  /** 登録ユーザー / register_user */
  @Column(required = true, physicalName = "register_user", logicalName = "登録ユーザー")
  protected String registerUser;

  /**
   * DBコネクションが null でないことを確かめます.
   *
   * @param conn DBコネクション
   * @throws IllegalArgumentException DBコネクションが null の場合
   */
  protected static void requireConnection(final Connection conn) {
    if (conn == null) {
      throw new IllegalArgumentException("DBコネクションは null にできません");
    }
  }

  /**
   * テナントIDが null または空文字列でないことを確かめます.
   *
   * @param tenantId テナントID
   * @throws IllegalArgumentException テナントIDが null または空文字列の場合
   */
  protected static void requireTenantId(final String tenantId) {
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("TenantId must not be null or empty");
    }
  }

  /** テナントID / tenant_id */
  @Column(physicalName = "tenant_id", logicalName = "テナントID")
  protected String tenantId;

  protected EntityBase(String tenantId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
      throw new IllegalArgumentException("tenantId must not be null or empty");
    }
    this.tenantId = tenantId;
  }

  /**
   * INSERT処理を実行します.
   *
   * @param connection DBコネクション
   * @return 登録成功可否
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public abstract int insert(Connection connection) throws SQLException;

  /**
   * PKをキーにSELECT処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public abstract void selectByPk(final Connection connection) throws SQLException;

  /**
   * PKをキーにUPDATE処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public abstract boolean updateByPk(final Connection connection) throws SQLException;

  /**
   * PKをキーにDELETE処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public abstract boolean deleteByPk(final Connection connection) throws SQLException;

  @Override
  public int compareTo(EntityBase o) {
    if (this.registerDate == null) {
      return o == null || o.getRegisterDate() == null ? 0 : -1;
    } else if (o == null || o.getRegisterDate() == null) {
      return 1;
    } else {
      return this.registerDate.compareTo(o.getRegisterDate());
    }
  }

  // ========================
  // 関数型インターフェース
  // ========================

  /**
   * PreparedStatement パラメータバインディング処理.
   *
   * <p>SQLException をスローできる Consumer です。
   */
  @FunctionalInterface
  protected interface PreparedStatementBinder {
    void bind(PreparedStatement stmt) throws SQLException;
  }

  /**
   * ResultSet マッピング処理.
   *
   * <p>SQLException をスローできる Consumer です。
   */
  @FunctionalInterface
  protected interface ResultSetMapper {
    void map(ResultSet rs) throws SQLException;
  }

  // ========================
  // CRUD テンプレートメソッド（tenantId 込み）
  // ========================

  /**
   * INSERT実行テンプレートメソッド（tenantId 自動追加）.
   *
   * <p>SQL に自動的に tenant_id カラムをバインドします。
   *
   * @param conn DBコネクション（null の場合は 0 を返す）
   * @param sql INSERT SQL文（tenant_id パラメータを含まない）
   * @param tenantId テナントID（必須）
   * @param paramBinder パラメータバインディング処理（インデックス 1 から開始）
   * @param logLabel ログ出力用ラベル
   * @return 挿入行数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合、またはテナントIDが null または空文字列の場合
   */
  protected int executeInsert(
      final Connection conn,
      final String sql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    requireConnection(conn);
    requireTenantId(tenantId);

    // VALUES 句の直前の列リストに tenant_id を、VALUES 句の値リストに ? を追加する
    String upperSql = sql.toUpperCase();
    int valuesIndex = upperSql.indexOf("VALUES");
    if (valuesIndex < 0) {
      throw new IllegalArgumentException("INSERT SQL must contain VALUES clause");
    }
    String beforeValues = sql.substring(0, valuesIndex);
    String afterValues = sql.substring(valuesIndex);
    int lastParenBeforeValues = beforeValues.lastIndexOf(')');
    if (lastParenBeforeValues < 0) {
      throw new IllegalArgumentException("Column list must be in parentheses");
    }
    int lastParenInValues = afterValues.lastIndexOf(')');
    if (lastParenInValues < 0) {
      throw new IllegalArgumentException("VALUES clause must have parentheses");
    }
    String columnList = beforeValues.substring(0, lastParenBeforeValues) + ", tenant_id)";
    String valuesPart = afterValues.substring(0, lastParenInValues) + ", ?)";
    String modifiedSql = columnList + " " + valuesPart;
    logSql(modifiedSql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(modifiedSql)) {
      paramBinder.bind(stmt);
      setTenantIdParameter(stmt, countParameterPlaceholders(sql) + 1, tenantId);
      return stmt.executeUpdate();
    }
  }

  /**
   * INSERT実行テンプレートメソッド（tenantId フィルタなし）.
   *
   * <p>マスタテーブル（SES_AI_M_TENANT など）のように tenantId フィルタリングが不要な場合に使用します。
   *
   * @param conn DBコネクション（null の場合は 0 を返す）
   * @param sql INSERT SQL文
   * @param paramBinder パラメータバインディング処理
   * @param logLabel ログ出力用ラベル
   * @return 挿入行数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合
   */
  protected int executeInsertWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    requireConnection(conn);

    logSql(sql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      paramBinder.bind(stmt);
      return stmt.executeUpdate();
    }
  }

  /**
   * SELECT-by-PK実行テンプレートメソッド（WHERE に tenantId フィルタを自動追加）.
   *
   * <p>SQL に自動的に "AND tenant_id = ?" を付加して実行します。 これにより、他のテナント行への誤りアクセスを防止します。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param baseSql SELECT SQL文（WHERE句の有無は自動判定）
   * @param tenantId テナントID（必須）
   * @param paramBinder パラメータバインディング処理
   * @param resultMapper ResultSet マッピング処理
   * @param logLabel ログ出力用ラベル
   * @return データが存在する場合は true、存在しない場合は false
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合、またはテナントIDが null または空文字列の場合
   */
  protected boolean executeSelectByPk(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final ResultSetMapper resultMapper,
      final String logLabel)
      throws SQLException {
    requireConnection(conn);
    requireTenantId(tenantId);

    String filteredSql = addTenantIdFilter(baseSql, tenantId);
    logSql(filteredSql, logLabel);

    boolean found = false;
    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      paramBinder.bind(stmt);
      // tenant_id 条件の位置は元SQLのプレースホルダ数 + 1（PK が複数列でも第2列を上書きしないため）
      setTenantIdParameter(stmt, countParameterPlaceholders(baseSql) + 1, tenantId);
      try (ResultSet rs = stmt.executeQuery()) {
        found = rs.next();
        if (found) {
          resultMapper.map(rs);
        }
      }
    }
    return found;
  }

  /**
   * SELECT-by-PK実行テンプレートメソッド（tenantId フィルタなし）.
   *
   * <p>マスタテーブルの取得など、tenantId フィルタリングが不要な場合に使用します。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param sql SELECT SQL文
   * @param paramBinder パラメータバインディング処理
   * @param resultMapper ResultSet マッピング処理
   * @param logLabel ログ出力用ラベル
   * @return データが存在する場合は true、存在しない場合は false
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合
   */
  protected boolean executeSelectByPkWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final ResultSetMapper resultMapper,
      final String logLabel)
      throws SQLException {
    requireConnection(conn);

    logSql(sql, logLabel);

    boolean found = false;
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      paramBinder.bind(stmt);
      try (ResultSet rs = stmt.executeQuery()) {
        found = rs.next();
        if (found) {
          resultMapper.map(rs);
        }
      }
    }
    return found;
  }

  /**
   * EXISTS クエリ実行テンプレートメソッド（WHERE に tenantId フィルタを自動追加）.
   *
   * <p>SQL に自動的に "AND tenant_id = ?" を付加して実行します。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param baseSql EXISTS SQL文（WHERE句の有無は自動判定）
   * @param tenantId テナントID（必須）
   * @param paramBinder パラメータバインディング処理
   * @param logLabel ログ出力用ラベル
   * @return レコードが存在する場合は true、存在しない場合は false
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合、またはテナントIDが null または空文字列の場合
   */
  protected boolean executeExists(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    requireConnection(conn);
    requireTenantId(tenantId);

    String filteredSql = addTenantIdFilter(baseSql, tenantId);
    logSql(filteredSql, logLabel);

    boolean exists = false;
    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      paramBinder.bind(stmt);
      // tenant_id 条件の位置は元SQLのプレースホルダ数 + 1（PK が複数列でも第2列を上書きしないため）
      setTenantIdParameter(stmt, countParameterPlaceholders(baseSql) + 1, tenantId);
      try (ResultSet rs = stmt.executeQuery()) {
        exists = rs.next() && rs.getBoolean(1);
      }
    }
    return exists;
  }

  /**
   * UPDATE-by-PK実行テンプレートメソッド（WHERE に tenantId フィルタを自動追加）.
   *
   * <p>SQL に自動的に "AND tenant_id = ?" を付加して実行します。 これにより、他のテナント行への誤り更新を防止します。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param baseSql UPDATE SQL文（WHERE句の有無は自動判定）
   * @param tenantId テナントID（必須）
   * @param paramBinder パラメータバインディング処理
   * @param logLabel ログ出力用ラベル
   * @return 更新が成功した場合は true、失敗した場合は false
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  protected boolean executeUpdateByPk(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    return executeTenantFilteredUpdate(conn, baseSql, tenantId, paramBinder, logLabel);
  }

  /**
   * UPDATE-by-PK実行テンプレートメソッド（tenantId フィルタなし）.
   *
   * <p>マスタテーブルの更新など、tenantId フィルタリングが不要な場合に使用します。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param sql UPDATE SQL文
   * @param paramBinder パラメータバインディング処理
   * @param logLabel ログ出力用ラベル
   * @return 更新が成功した場合は true、失敗した場合は false
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合
   */
  protected boolean executeUpdateByPkWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    requireConnection(conn);

    logSql(sql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      paramBinder.bind(stmt);
      return stmt.executeUpdate() > 0;
    }
  }

  /**
   * DELETE-by-PK実行テンプレートメソッド（WHERE に tenantId フィルタを自動追加）.
   *
   * <p>SQL に自動的に "AND tenant_id = ?" を付加して実行します。 これにより、他のテナント行への誤り削除を防止します。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param baseSql DELETE SQL文（WHERE句の有無は自動判定）
   * @param tenantId テナントID（必須）
   * @param paramBinder パラメータバインディング処理
   * @param logLabel ログ出力用ラベル
   * @return 削除が成功した場合は true、失敗した場合は false
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  protected boolean executeDeleteByPk(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    // tenant_id 条件の位置は元SQLのプレースホルダ数 + 1（PK が複数列でも第2列を上書きしないため）
    return executeTenantFilteredUpdate(conn, baseSql, tenantId, paramBinder, logLabel);
  }

  /**
   * tenant_id 条件付きの UPDATE または DELETE を実行します.
   *
   * <p>UPDATE と DELETE は SQL の組み立てと実行が同じため、共通の処理にまとめています。 tenant_id 条件は元SQLのプレースホルダ数 + 1
   * の位置にバインドします。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param baseSql UPDATE または DELETE の SQL文
   * @param tenantId テナントID（必須。null または空文字列の場合は false を返す）
   * @param paramBinder パラメータバインディング処理
   * @param logLabel ログ出力用ラベル
   * @return 1件以上更新または削除した場合は true、そうでない場合は false
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合、またはテナントIDが null または空文字列の場合
   */
  private boolean executeTenantFilteredUpdate(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    requireConnection(conn);
    requireTenantId(tenantId);

    String filteredSql = addTenantIdFilter(baseSql, tenantId);
    logSql(filteredSql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      paramBinder.bind(stmt);
      setTenantIdParameter(stmt, countParameterPlaceholders(baseSql) + 1, tenantId);

      return stmt.executeUpdate() > 0;
    }
  }

  /**
   * DELETE-by-PK実行テンプレートメソッド（tenantId フィルタなし）.
   *
   * <p>マスタテーブルの削除など、tenantId フィルタリングが不要な場合に使用します。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param sql DELETE SQL文
   * @param paramBinder パラメータバインディング処理
   * @param logLabel ログ出力用ラベル
   * @return 削除が成功した場合は true、失敗した場合は false
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合
   */
  protected boolean executeDeleteByPkWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    requireConnection(conn);

    logSql(sql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      paramBinder.bind(stmt);
      return stmt.executeUpdate() > 0;
    }
  }

  // ========================
  // ヘルパーメソッド
  // ========================

  /**
   * SQL に tenantId フィルタを自動追加します.
   *
   * <p>FROM 句からテーブルエイリアスを自動検出し、そのテーブルの tenant_id 条件を追加します。 JOIN を含む複雑な SQL にも対応します。
   *
   * @param baseSql 基本SQL
   * @param tenantId テナントID（必須）
   * @return tenant_id フィルターが付加された SQL
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected String addTenantIdFilter(final String baseSql, final String tenantId) {
    return new TenantFilterSql(baseSql).withTenantFilter(tenantId);
  }

  /**
   * PreparedStatement に tenant_id パラメータをバインドします.
   *
   * @param stmt バインド対象の PreparedStatement
   * @param paramIndex パラメータインデックス
   * @param tenantId テナントID（必須）
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
   * SQL ログを出力します.
   *
   * @param sql SQL文
   * @param label ログ出力用ラベル
   */
  protected void logSql(final String sql, final String label) {
    if (log.isInfoEnabled()) {
      log.info("[SQL] {} - {}", label, sql);
    }
  }

  /**
   * SQL 内のパラメータプレースホルダー（?）の数を数えます.
   *
   * @param sql SQL文
   * @return プレースホルダーの数
   */
  static int countParameterPlaceholders(final String sql) {
    int count = 0;
    for (char c : sql.toCharArray()) {
      if (c == '?') {
        count++;
      }
    }
    return count;
  }
}
