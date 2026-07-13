package copel.sesproductpackage.core.database.base;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
   * @throws SQLException
   */
  public abstract int insert(Connection connection) throws SQLException;

  /**
   * PKをキーにSELECT処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public abstract void selectByPk(final Connection connection) throws SQLException;

  /**
   * PKをキーにUPDATE処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public abstract boolean updateByPk(final Connection connection) throws SQLException;

  /**
   * PKをキーにDELETE処理を実行します.
   *
   * @param connection DBコネクション
   * @throws SQLException
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
   * @throws SQLException
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected int executeInsert(
      final Connection conn,
      final String sql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    if (conn == null || tenantId == null || tenantId.isEmpty()) {
      return 0;
    }

    logSql(sql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      paramBinder.bind(stmt);
      // tenantId は呼び出し元が INSERT SQL に含めてバインドすること
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
   * @throws SQLException
   */
  protected int executeInsertWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    if (conn == null) {
      return 0;
    }

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
   * @throws SQLException
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected boolean executeSelectByPk(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final ResultSetMapper resultMapper,
      final String logLabel)
      throws SQLException {
    if (conn == null || tenantId == null || tenantId.isEmpty()) {
      return false;
    }

    String filteredSql = addTenantIdFilter(baseSql, tenantId);
    logSql(filteredSql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      paramBinder.bind(stmt);
      setTenantIdParameter(stmt, 2, tenantId); // 通常は PK が第1パラメータなので、tenantId は第2

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          resultMapper.map(rs);
          return true;
        }
      }
    }
    return false;
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
   * @throws SQLException
   */
  protected boolean executeSelectByPkWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final ResultSetMapper resultMapper,
      final String logLabel)
      throws SQLException {
    if (conn == null) {
      return false;
    }

    logSql(sql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      paramBinder.bind(stmt);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          resultMapper.map(rs);
          return true;
        }
      }
    }
    return false;
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
   * @throws SQLException
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected boolean executeExists(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    if (conn == null || tenantId == null || tenantId.isEmpty()) {
      return false;
    }

    String filteredSql = addTenantIdFilter(baseSql, tenantId);
    logSql(filteredSql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      paramBinder.bind(stmt);
      setTenantIdParameter(stmt, 2, tenantId); // 通常は PK が第1パラメータなので、tenantId は第2
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean(1);
        }
      }
    }
    return false;
  }

  /**
   * EXISTS クエリ実行テンプレートメソッド（tenantId フィルタなし）.
   *
   * <p>マスタテーブルの確認など、tenantId フィルタリングが不要な場合に使用します。
   *
   * @param conn DBコネクション（null の場合は false を返す）
   * @param sql EXISTS SQL文
   * @param paramBinder パラメータバインディング処理
   * @param logLabel ログ出力用ラベル
   * @return レコードが存在する場合は true、存在しない場合は false
   * @throws SQLException
   */
  protected boolean executeExistsWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    if (conn == null) {
      return false;
    }

    logSql(sql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      paramBinder.bind(stmt);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean(1);
        }
      }
    }
    return false;
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
   * @throws SQLException
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected boolean executeUpdateByPk(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    if (conn == null || tenantId == null || tenantId.isEmpty()) {
      return false;
    }

    String filteredSql = addTenantIdFilter(baseSql, tenantId);
    logSql(filteredSql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      paramBinder.bind(stmt);
      setTenantIdParameter(stmt, 5, tenantId); // UPDATE の場合、通常は SET値が第1-4、PK が第5

      return stmt.executeUpdate() > 0;
    }
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
   * @throws SQLException
   */
  protected boolean executeUpdateByPkWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    if (conn == null) {
      return false;
    }

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
   * @throws SQLException
   * @throws IllegalArgumentException tenantId が null または空文字列の場合
   */
  protected boolean executeDeleteByPk(
      final Connection conn,
      final String baseSql,
      final String tenantId,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    if (conn == null || tenantId == null || tenantId.isEmpty()) {
      return false;
    }

    String filteredSql = addTenantIdFilter(baseSql, tenantId);
    logSql(filteredSql, logLabel);

    try (PreparedStatement stmt = conn.prepareStatement(filteredSql)) {
      paramBinder.bind(stmt);
      setTenantIdParameter(stmt, 2, tenantId); // DELETE の場合、通常は PK が第1なので tenantId は第2

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
   * @throws SQLException
   */
  protected boolean executeDeleteByPkWithoutTenantFilter(
      final Connection conn,
      final String sql,
      final PreparedStatementBinder paramBinder,
      final String logLabel)
      throws SQLException {
    if (conn == null) {
      return false;
    }

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
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("TenantId must not be null or empty");
    }
    if (baseSql == null) {
      throw new IllegalArgumentException("baseSql must not be null");
    }

    String trimmedSql = baseSql.trim();
    String upperSql = trimmedSql.toUpperCase();

    // SELECT EXISTS の場合は特別処理（括弧内のWHERE句に tenant_id を挿入）
    if (upperSql.startsWith("SELECT EXISTS")) {
      if (upperSql.contains(" WHERE ")) {
        int lastParenIndex = trimmedSql.lastIndexOf(')');
        if (lastParenIndex > 0) {
          return trimmedSql.substring(0, lastParenIndex) + " AND tenant_id = ?"
              + trimmedSql.substring(lastParenIndex);
        }
      }
    }

    // WHERE句が含まれているかチェック
    boolean hasWhereClause = upperSql.contains(" WHERE ");

    // FROM句からテーブルエイリアスを自動抽出
    Pattern pattern =
        Pattern.compile(
            "FROM\\s+\\S+\\s+([a-zA-Z_]\\w*)(?:\\s|,|JOIN|WHERE|$)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(trimmedSql);
    String tableAlias = null;
    if (matcher.find()) {
      String alias = matcher.group(1);
      // WHERE, JOIN, ON などの予約語がテーブルエイリアスと誤検出されないようフィルタ
      if (!alias.matches("(?i)WHERE|JOIN|ON|AND|OR|LIMIT|ORDER|GROUP|OFFSET|UNION")) {
        tableAlias = alias;
      }
    }

    String tenantIdCondition =
        tableAlias != null && !tableAlias.isEmpty()
            ? tableAlias + ".tenant_id = ?"
            : "tenant_id = ?";

    if (hasWhereClause) {
      return trimmedSql + " AND " + tenantIdCondition;
    } else {
      return trimmedSql + " WHERE " + tenantIdCondition;
    }
  }

  /**
   * PreparedStatement に tenant_id パラメータをバインドします.
   *
   * @param stmt バインド対象の PreparedStatement
   * @param paramIndex パラメータインデックス
   * @param tenantId テナントID（必須）
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
}
