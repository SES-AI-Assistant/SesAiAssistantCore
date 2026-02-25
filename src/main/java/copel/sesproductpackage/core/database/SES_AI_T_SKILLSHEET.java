package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.SkillSheet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 【Entityクラス】 スキルシート情報(SES_AI_T_SKILLSHEET)テーブル.
 *
 * @author 鈴木一矢
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_SKILLSHEET extends SES_AI_T_EntityBase {
  // ================================
  // SQL
  // ================================
  /** INSERTR文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_SKILLSHEET (from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE file_id = ?";

  /** SELECT文(原文抜き). */
  private static final String SELECT_WITHOUT_CONTENT_SQL =
      "SELECT from_group, from_id, from_name, file_id, file_name, file_content_summary, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE file_id = ?";

  /** 重複チェック用SQL. */
  private static final String CHECK_SQL =
      "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET WHERE file_content % ? AND similarity(file_content, ?) > ?";

  /** DELETE文. */
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
    if (connection == null || this.getFileId() == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_WITHOUT_CONTENT_SQL);
    preparedStatement.setString(1, this.getFileId());
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.fromGroup = resultSet.getString("from_group");
      this.fromId = resultSet.getString("from_id");
      this.fromName = resultSet.getString("from_name");
      this.skillSheet =
          new SkillSheet(resultSet.getString("file_id"), resultSet.getString("file_name"), "");
      this.skillSheet.setFileContentSummary(resultSet.getString("file_content_summary"));
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
      this.ttl = new OriginalDateTime(resultSet.getString("ttl"));
    }
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
    if (connection == null) {
      return 0;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.fromGroup);
    preparedStatement.setString(2, this.fromId);
    preparedStatement.setString(3, this.fromName);
    preparedStatement.setString(4, this.skillSheet == null ? null : this.skillSheet.getFileId());
    preparedStatement.setString(5, this.skillSheet == null ? null : this.skillSheet.getFileName());
    preparedStatement.setString(
        6, this.skillSheet == null ? null : this.skillSheet.getFileContent());
    preparedStatement.setString(
        7, this.skillSheet == null ? null : this.skillSheet.getFileContentSummary());
    preparedStatement.setString(8, this.vectorData == null ? null : this.vectorData.toString());
    preparedStatement.setTimestamp(
        9, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(10, this.registerUser);
    preparedStatement.setTimestamp(11, this.ttl == null ? null : this.ttl.toTimestamp());
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(final Connection connection) throws SQLException {
    if (connection == null || this.getFileId() == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.getFileId());
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.fromGroup = resultSet.getString("from_group");
      this.fromId = resultSet.getString("from_id");
      this.fromName = resultSet.getString("from_name");
      this.skillSheet =
          new SkillSheet(
              resultSet.getString("file_id"),
              resultSet.getString("file_name"),
              resultSet.getString("file_content"));
      this.skillSheet.setFileContentSummary(resultSet.getString("file_content_summary"));
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
      this.ttl = new OriginalDateTime(resultSet.getString("ttl"));
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    return false;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.getFileId());
    return preparedStatement.executeUpdate() > 0;
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
