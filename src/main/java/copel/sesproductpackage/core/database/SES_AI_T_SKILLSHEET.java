package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.SkillSheet;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 【Entityクラス】 スキルシート情報(SES_AI_T_SKILLSHEET)テーブル.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_SKILLSHEET extends SES_AI_T_EntityBase {

  public SES_AI_T_SKILLSHEET(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }

  // ================================
  // SQL
  // ================================
  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_SKILLSHEET (from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?)";

  /** SELECT文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String SELECT_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE file_id = ?";

  /** SELECT文(原文抜き、tenantId フィルタなし、テンプレートメソッドが自動追加する). */
  private static final String SELECT_WITHOUT_CONTENT_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content_summary, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE file_id = ?";

  /** 重複チェック用SQL. */
  private static final String CHECK_SQL =
      "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET WHERE tenant_id = ? AND file_content % ? AND similarity(file_content, ?) > ?";

  /** DELETE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_T_SKILLSHEET WHERE file_id = ?";

  // ================================
  // メンバ
  // ================================
  /** スキルシート. */
  private SkillSheet skillSheet = new SkillSheet();

  // ================================
  // メソッド
  // ================================
  /**
   * LLMに最もマッチするスキルシートのファイルIDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String toスキルシート選出用文章() {
    return "ファイルID：" + this.skillSheet.getFileId() + "内容：" + this.getFileContentSummary();
  }

  /**
   * このレコードがもつスキルシートのダウンロードURLを返却します.
   *
   * @return ダウンロードURL
   */
  public String getFileUrl() {
    return this.skillSheet != null ? this.skillSheet.getFileUrl() : null;
  }

  /**
   * 紐づくS3ファイルのオブジェクトキーを返却する.
   *
   * @return オブジェクトキー.
   */
  public String getObjectKey() {
    return this.skillSheet.getObjectKey();
  }

  /**
   * このオブジェクトに格納されているPKをキーにレコードを1件SELECTしこのオブジェクトに持ちます(原文抜きでSELECT).
   * 原文が1.5万文字程度あり、頻繁にSELECTすると負荷を書けるため、可能であれば除外して検索する.
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public void selectByPkWithoutRawContent(final Connection connection) throws SQLException {
    if (this.getFileId() == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_WITHOUT_CONTENT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.getFileId()),
        (rs) -> {
          this.fromGroup = rs.getString("from_group");
          this.fromId = rs.getString("from_id");
          this.fromName = rs.getString("from_name");
          this.skillSheet = new SkillSheet(rs.getString("file_id"), rs.getString("file_name"), "");
          this.skillSheet.setFileContentSummary(rs.getString("file_content_summary"));
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
          this.ttl = new OriginalDateTime(rs.getString("ttl"));
        },
        "SES_AI_T_SKILLSHEET.selectByPkWithoutRawContent");
  }

  // ================================
  // Overrideメソッド
  // ================================
  @Override
  protected String getRawContent() {
    return this.getFileContent();
  }

  @Override
  protected String getContentSummary() {
    return this.getFileContentSummary();
  }

  @Override
  protected String getCheckSql() {
    return CHECK_SQL;
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.fromGroup);
          stmt.setString(2, this.fromId);
          stmt.setString(3, this.fromName);
          stmt.setString(4, this.skillSheet == null ? null : this.skillSheet.getFileId());
          stmt.setString(5, this.skillSheet == null ? null : this.skillSheet.getFileName());
          stmt.setString(6, this.skillSheet == null ? null : this.skillSheet.getFileContent());
          stmt.setString(
              7, this.skillSheet == null ? null : this.skillSheet.getFileContentSummary());
          stmt.setString(8, this.vectorData == null ? null : this.vectorData.toString());
          stmt.setTimestamp(9, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(10, this.registerUser);
          stmt.setTimestamp(11, this.ttl == null ? null : this.ttl.toTimestamp());
        },
        "SES_AI_T_SKILLSHEET.insert");
  }

  @Override
  public void selectByPk(final Connection connection) throws SQLException {
    if (this.getFileId() == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.getFileId()),
        (rs) -> {
          this.fromGroup = rs.getString("from_group");
          this.fromId = rs.getString("from_id");
          this.fromName = rs.getString("from_name");
          this.skillSheet =
              new SkillSheet(
                  rs.getString("file_id"), rs.getString("file_name"), rs.getString("file_content"));
          this.skillSheet.setFileContentSummary(rs.getString("file_content_summary"));
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
          this.ttl = new OriginalDateTime(rs.getString("ttl"));
        },
        "SES_AI_T_SKILLSHEET.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    return false;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.getFileId() == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.getFileId()),
        "SES_AI_T_SKILLSHEET.deleteByPk");
  }

  // ================================
  // Getter / Setter
  // ================================
  public SkillSheet getSkillSheet() {
    return this.skillSheet;
  }

  public void setSkillSheet(SkillSheet skillSheet) {
    this.skillSheet = skillSheet;
  }

  public String getFileId() {
    return this.skillSheet == null ? null : this.skillSheet.getFileId();
  }

  public void setFileId(String fileId) {
    this.skillSheet.setFileId(fileId);
  }

  public String getFileName() {
    return this.skillSheet == null ? null : this.skillSheet.getFileName();
  }

  public void setFileName(String fileName) {
    this.skillSheet.setFileName(fileName);
  }

  public String getFileContent() {
    return this.skillSheet == null ? "" : this.skillSheet.getFileContent();
  }

  public void setFileContent(String fileContent) {
    this.skillSheet.setFileContent(fileContent);
  }

  public String getFileContentSummary() {
    return this.skillSheet == null ? "" : this.skillSheet.getFileContentSummary();
  }

  public void setFileContentSummary(String fileContentSummary) {
    this.skillSheet.setFileContentSummary(fileContentSummary);
  }
}
