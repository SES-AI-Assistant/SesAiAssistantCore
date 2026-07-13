package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 【Entityクラス】 要員情報(SES_AI_T_PERSON)テーブル.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_PERSON extends SES_AI_T_EntityBase {

  public SES_AI_T_PERSON(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }

  // ================================
  // SQL
  // ================================
  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_PERSON (person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, vector_data, register_date, register_user, ttl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?)";

  /** SELECT文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String SELECT_SQL =
      "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, unit_price, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON WHERE person_id = ?";

  /** UPDATE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_T_PERSON SET from_group = ?, from_id = ?, from_name = ?, raw_content = ?, content_summary = ?, file_id = ?, unit_price = ?, vector_data = ?::vector, ttl = ? WHERE person_id = ?";

  /** UPDATE文(file_idのみ). */
  private static final String UPDATE_FILE_ID_SQL =
      "UPDATE SES_AI_T_PERSON SET file_id = ? WHERE person_id = ? AND tenant_id = ?";

  /** 重複チェック用SQL. */
  private static final String CHECK_SQL =
      "SELECT COUNT(*) FROM SES_AI_T_PERSON WHERE tenant_id = ? AND raw_content % ? AND similarity(raw_content, ?) > ?";

  /** 単価等取得用・重複チェックSQL. */
  private static final String FIND_SIMILAR_SQL =
      "SELECT person_id, unit_price FROM SES_AI_T_PERSON WHERE tenant_id = ? AND regexp_replace(raw_content, 'https?://[^\\s]+', '', 'g') % ? AND similarity(regexp_replace(raw_content, 'https?://[^\\s]+', '', 'g'), ?) > ? LIMIT 1";

  /** DELETE文. */
  private static final String DELETE_SQL =
      "DELETE FROM SES_AI_T_PERSON WHERE person_id = ? AND tenant_id = ?";

  /** file_id 一致で file_id のみ NULL に戻す（スキルシート削除前の参照解除用）. */
  private static final String CLEAR_FILE_ID_BY_FILE_ID_SQL =
      "UPDATE SES_AI_T_PERSON SET file_id = NULL WHERE file_id = ?";

  // ================================
  // メンバ
  // ================================
  /** 要員ID(PK). */
  @Column(required = true, primary = true, physicalName = "person_id", logicalName = "要員ID")
  private String personId;

  /** 原文 / raw_content */
  @Column(physicalName = "raw_content", logicalName = "原文")
  private String rawContent;

  /** ファイルID / file_id. */
  @Column(physicalName = "file_id", logicalName = "ファイルID")
  private String fileId;

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
   * LLMに最もマッチする要員の要員IDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String to要員選出用文章() {
    return "要員ID：" + this.personId + "内容：" + this.rawContent;
  }

  /**
   * このレコードがfile_idを持つかどうかを返却します.
   *
   * @return file_idを持つならtrue、そうでないならfalse
   */
  public boolean isスキルシート登録済() {
    return !OriginalStringUtils.isEmpty(this.fileId);
  }

  /**
   * このエンティティが持つファイルIDでレコードを更新します.
   *
   * @param connection DBコネクション
   * @return 更新成功すればtrue、それ以外はfalse
   * @throws SQLException
   */
  public boolean updateFileIdByPk(final Connection connection) throws SQLException {
    if (connection == null || this.personId == null || this.tenantId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_FILE_ID_SQL);
    preparedStatement.setString(1, this.fileId);
    preparedStatement.setString(2, this.personId);
    preparedStatement.setString(3, this.tenantId);
    return preparedStatement.executeUpdate() > 0;
  }

  /**
   * 指定の file_id を参照している要員行の file_id を NULL に戻す.
   *
   * <p>スキルシート行を削除する前に呼び、参照整合性と TTL バッチのブロック条件を外す。
   *
   * @param connection DBコネクション
   * @param fileId ファイルID
   * @return 更新件数
   * @throws SQLException SQL例外
   */
  public static int clearFileIdWhereFileId(final Connection connection, final String fileId)
      throws SQLException {
    if (connection == null || fileId == null) {
      return 0;
    }
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(CLEAR_FILE_ID_BY_FILE_ID_SQL)) {
      preparedStatement.setString(1, fileId);
      return preparedStatement.executeUpdate();
    }
  }

  /**
   * URLを除外したテキストで類似レコードを検索し、存在する場合はそのレコードのIDと単価を返す.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param textToCheck チェック対象のテキスト
   * @param similarityThreshold 類似度のしきい値
   * @return 類似レコードが見つかった場合はそのSES_AI_T_PERSON、見つからなかった場合はnull
   * @throws SQLException
   */
  public static SES_AI_T_PERSON findSimilarRecord(
      final Connection connection,
      final String tenantId,
      final String textToCheck,
      final double similarityThreshold)
      throws SQLException {
    if (connection == null || tenantId == null || textToCheck == null) {
      return null;
    }
    // 比較用にURLをマスキング
    String maskedText = textToCheck.replaceAll("https?://[^\\s]+", "");

    try (PreparedStatement preparedStatement = connection.prepareStatement(FIND_SIMILAR_SQL)) {
      preparedStatement.setString(1, tenantId);
      preparedStatement.setString(2, maskedText);
      preparedStatement.setString(3, maskedText);
      preparedStatement.setDouble(4, similarityThreshold);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          SES_AI_T_PERSON result = new SES_AI_T_PERSON(tenantId);
          result.setPersonId(resultSet.getString("person_id"));
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
  public int insert(final Connection connection) throws SQLException {
    // 要員IDを発行
    this.personId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.personId);
          stmt.setString(2, this.fromGroup);
          stmt.setString(3, this.fromId);
          stmt.setString(4, this.fromName);
          stmt.setString(5, this.rawContent);
          stmt.setString(6, this.contentSummary);
          stmt.setString(7, this.fileId);
          stmt.setObject(8, this.unitPrice == null ? null : this.unitPrice.getValue());
          stmt.setString(9, this.vectorData == null ? null : this.vectorData.toString());
          stmt.setTimestamp(10, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(11, this.registerUser);
          stmt.setTimestamp(12, this.ttl == null ? null : this.ttl.toTimestamp());
        },
        "SES_AI_T_PERSON.insert");
  }

  @Override
  public boolean updateByPk(final Connection connection) throws SQLException {
    if (this.personId == null) {
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
          stmt.setString(6, this.fileId);
          stmt.setObject(7, this.unitPrice == null ? null : this.unitPrice.getValue());
          stmt.setString(8, this.vectorData == null ? null : this.vectorData.toString());
          stmt.setTimestamp(9, this.ttl == null ? null : this.ttl.toTimestamp());
          stmt.setString(10, this.personId);
        },
        "SES_AI_T_PERSON.updateByPk");
  }

  @Override
  public void selectByPk(final Connection connection) throws SQLException {
    if (this.personId == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.personId),
        (rs) -> {
          this.fromGroup = rs.getString("from_group");
          this.fromId = rs.getString("from_id");
          this.fromName = rs.getString("from_name");
          this.rawContent = rs.getString("raw_content");
          this.contentSummary = rs.getString("content_summary");
          this.fileId = rs.getString("file_id");
          BigDecimal unitPriceValue = rs.getBigDecimal("unit_price");
          this.unitPrice = unitPriceValue == null ? Money.empty() : new Money(unitPriceValue);
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
          this.ttl = new OriginalDateTime(rs.getString("ttl"));
        },
        "SES_AI_T_PERSON.selectByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.personId == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.personId),
        "SES_AI_T_PERSON.deleteByPk");
  }
}
