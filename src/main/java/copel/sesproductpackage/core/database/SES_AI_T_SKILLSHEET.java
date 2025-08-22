        package copel.sesproductpackage.core.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.SkillSheet;
import copel.sesproductpackage.core.unit.Vector;

/**
 * 【Entityクラス】
 * スキルシート情報(SES_AI_T_SKILLSHEET)テーブル.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_T_SKILLSHEET extends SES_AI_T_EntityBase {
    // ================================
    // SQL
    // ================================
    /**
     * INSERTR文.
     */
    private final static String INSERT_SQL = "INSERT INTO SES_AI_T_SKILLSHEET (from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, vector_data, register_date, register_user, ttl) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?)";
    /**
     * SELECT文.
     */
    private final static String SELECT_SQL = "SELECT from_group, from_id, from_name, file_id, file_name, file_content, file_content_summary, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE file_id = ?";
    /**
     * SELECT文(原文抜き).
     */
    private final static String SELECT_WITHOUT_CONTENT_SQL = "SELECT from_group, from_id, from_name, file_id, file_name, file_content_summary, register_date, register_user, ttl FROM SES_AI_T_SKILLSHEET WHERE file_id = ?";
    /**
     * 重複チェック用SQL.
     */
    private final static String CHECK_SQL = "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET WHERE file_content % ? AND similarity(file_content, ?) > ?";

    // ================================
    // メンバ
    // ================================
    /**
     * スキルシート.
     */
    private SkillSheet skillSheet;

    /**
     * コンストラクタ.
     */
    public SES_AI_T_SKILLSHEET() {
        this.skillSheet = new SkillSheet();
    }

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
            this.skillSheet = new SkillSheet(resultSet.getString("file_id"), resultSet.getString("file_name"), "");
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
    public void embedding(final Transformer embeddingProcessListener) throws IOException, RuntimeException {
        this.vectorData = new Vector(embeddingProcessListener);
        String content = this.skillSheet.getFileContent();
        if (content != null) {
            // 特殊文字や記号を省いた文字列をカウントし6000文字より少なければファイル内容をそのままエンベディングする
            // 6000文字以上であればエンベディングできない可能性が高いため、要約をエンベディングする
            content = content.replaceAll("[\\p{C}\\p{P}\"]", "");
            this.vectorData.setRawString(content.length() < 7000 ? content : this.skillSheet.getFileContentSummary());
            this.vectorData.embedding();
        }
    }

    @Override
    public boolean uniqueCheck(final Connection connection, final double similarityThreshold) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(CHECK_SQL);
        preparedStatement.setString(1, this.skillSheet == null ? null : this.skillSheet.getFileContent());
        preparedStatement.setString(2, this.skillSheet == null ? null : this.skillSheet.getFileContent());
        preparedStatement.setDouble(3, similarityThreshold);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getInt(1) < 1;
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
        preparedStatement.setString(6, this.skillSheet == null ? null : this.skillSheet.getFileContent());
        preparedStatement.setString(7, this.skillSheet == null ? null : this.skillSheet.getFileContentSummary());
        preparedStatement.setString(8, this.vectorData == null ? null : this.vectorData.toString());
        preparedStatement.setTimestamp(9, this.registerDate == null ? null : this.registerDate.toTimestamp());
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
            this.skillSheet = new SkillSheet(resultSet.getString("file_id"), resultSet.getString("file_name"), resultSet.getString("file_content"));
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
        // レコード削除とS3削除を行う
        return false;
    }

    @Override
    public String toString() {
        return "{\n from_group: " + this.fromGroup
                + "\n from_id: " + this.fromId
                + "\n from_name: " + this.fromName
                + "\n file_id: " + this.skillSheet == null ? null : this.skillSheet.getFileId()
                + "\n file_name: " + this.skillSheet == null ? null : this.skillSheet.getFileName()
                + "\n file_content: " + this.skillSheet == null ? null : this.skillSheet.getFileContent()
                + "\n file_content_summary: " + this.skillSheet == null ? null : this.skillSheet.getFileContentSummary()
                + "\n vector_data: " + this.vectorData
                + "\n register_date: " + this.registerDate
                + "\n register_user: " + this.registerUser
                + "\n ttl: " + this.ttl
                + "\n distance: " + this.distance
                + "\n}";
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
