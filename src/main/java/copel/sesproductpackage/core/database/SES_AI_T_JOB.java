package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 【Entityクラス】 案件情報(SES_AI_T_JOB)テーブル.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_JOB extends SES_AI_T_EntityBase {

  public SES_AI_T_JOB(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }
  // ================================
  // SQL
  // ================================
  /** INSERT文（tenantId を含む）. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_JOB (job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, vector_data, register_date, register_user, ttl, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?)";

  /** SELECT文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String SELECT_SQL =
      "SELECT job_id, from_group, from_id, from_name, raw_content, content_summary, unit_price, vector_data, register_date, register_user, ttl FROM SES_AI_T_JOB WHERE job_id = ?";

  /** UPDATE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_T_JOB SET from_group = ?, from_id = ?, from_name = ?, raw_content = ?, content_summary = ?, unit_price = ?, vector_data = ?::vector, ttl = ? WHERE job_id = ?";

  /** 重複チェック用SQL. */
  private static final String CHECK_SQL =
      "SELECT COUNT(*) FROM SES_AI_T_JOB WHERE tenant_id = ? AND raw_content % ? AND similarity(raw_content, ?) > ?";

  /** 単価等取得用・重複チェックSQL. */
  private static final String FIND_SIMILAR_SQL =
      "SELECT job_id, unit_price FROM SES_AI_T_JOB WHERE tenant_id = ? AND regexp_replace(raw_content, 'https?://[^\\s]+', '', 'g') % ? AND similarity(regexp_replace(raw_content, 'https?://[^\\s]+', '', 'g'), ?) > ? LIMIT 1";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_T_JOB WHERE job_id = ? AND tenant_id = ?";

  // ================================
  // メンバ
  // ================================
  /** 【PK】 案件ID* / job_id */
  @Column(required = true, primary = true, physicalName = "job_id", logicalName = "案件ID")
  private String jobId;

  /** 原文 / raw_content */
  @Column(physicalName = "raw_content", logicalName = "原文")
  private String rawContent;

  /** 要約 / content_summary */
  @Column(physicalName = "content_summary", logicalName = "要約")
  private String contentSummary;

  /** 単価 / unit_price */
  @Column(physicalName = "unit_price", logicalName = "単価")
  private Money unitPrice;

  // ================================
  // メソッド
  // ================================
  /**
   * LLMに最もマッチする案件の案件IDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String to案件選出用文章() {
    return "案件ID：" + this.jobId + "内容：" + this.rawContent;
  }

  /**
   * URLを除外したテキストで類似レコードを検索し、存在する場合はそのレコードのIDと単価を返す.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param textToCheck チェック対象のテキスト
   * @param similarityThreshold 類似度のしきい値
   * @return 類似レコードが見つかった場合はそのSES_AI_T_JOB、見つからなかった場合はnull
   * @throws SQLException
   */
  public static SES_AI_T_JOB findSimilarRecord(
      final Connection connection, final String tenantId, final String textToCheck, final double similarityThreshold)
      throws SQLException {
    if (connection == null || tenantId == null || textToCheck == null) {
      return null;
    }
    String maskedText = textToCheck.replaceAll("https?://[^\\s]+", "");

    try (PreparedStatement preparedStatement = connection.prepareStatement(FIND_SIMILAR_SQL)) {
      preparedStatement.setString(1, tenantId);
      preparedStatement.setString(2, maskedText);
      preparedStatement.setString(3, maskedText);
      preparedStatement.setDouble(4, similarityThreshold);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          SES_AI_T_JOB result = new SES_AI_T_JOB(tenantId);
          result.setJobId(resultSet.getString("job_id"));
          BigDecimal unitPriceValue = resultSet.getBigDecimal("unit_price");
          result.setUnitPrice(unitPriceValue == null ? Money.empty() : new Money(unitPriceValue));
          return result;
        }
      }
    }
    return null;
  }

  // ================================
  // Overrideメソッド
  // ================================
  @Override
  public String getRawContent() {
    return this.rawContent;
  }

  @Override
  public String getContentSummary() {
    return this.contentSummary;
  }

  @Override
  public String getCheckSql() {
    return CHECK_SQL;
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    // 案件IDを発行
    this.jobId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.jobId);
          stmt.setString(2, this.fromGroup);
          stmt.setString(3, this.fromId);
          stmt.setString(4, this.fromName);
          stmt.setString(5, this.rawContent);
          stmt.setString(6, this.contentSummary);
          stmt.setObject(7, this.unitPrice == null ? null : this.unitPrice.getValue());
          stmt.setString(8, this.vectorData == null ? null : this.vectorData.toString());
          stmt.setTimestamp(9, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(10, this.registerUser);
          stmt.setTimestamp(11, this.ttl == null ? null : this.ttl.toTimestamp());
          stmt.setString(12, this.tenantId);
        },
        "SES_AI_T_JOB.insert");
  }

  @Override
  public void selectByPk(final Connection connection) throws SQLException {
    if (this.jobId == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.jobId),
        (rs) -> {
          this.fromGroup = rs.getString("from_group");
          this.fromId = rs.getString("from_id");
          this.fromName = rs.getString("from_name");
          this.rawContent = rs.getString("raw_content");
          this.contentSummary = rs.getString("content_summary");
          BigDecimal unitPriceValue = rs.getBigDecimal("unit_price");
          this.unitPrice = unitPriceValue == null ? Money.empty() : new Money(unitPriceValue);
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
          this.ttl = new OriginalDateTime(rs.getString("ttl"));
        },
        "SES_AI_T_JOB.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.jobId == null) {
      return false;
    }
    return executeUpdateByPk(
        connection,
        UPDATE_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.fromGroup);
          stmt.setString(2, this.fromId);
          stmt.setString(3, this.fromName);
          stmt.setString(4, this.rawContent);
          stmt.setString(5, this.contentSummary);
          stmt.setObject(6, this.unitPrice == null ? null : this.unitPrice.getValue());
          stmt.setString(7, this.vectorData == null ? null : this.vectorData.toString());
          stmt.setTimestamp(8, this.ttl == null ? null : this.ttl.toTimestamp());
          stmt.setString(9, this.jobId);
        },
        "SES_AI_T_JOB.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.jobId == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.jobId),
        "SES_AI_T_JOB.deleteByPk");
  }
}
