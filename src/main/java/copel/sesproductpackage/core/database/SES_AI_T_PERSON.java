package copel.sesproductpackage.core.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import lombok.Data;

/**
 * 【Entityクラス】
 * 要員情報(SES_AI_T_PERSON)テーブル.
 *
 * @author 鈴木一矢
 *
 */
@Data
public class SES_AI_T_PERSON extends SES_AI_T_EntityBase {
    // ================================
    // SQL
    // ================================
    /**
     * INSERTR文.
     */
    private final static String INSERT_SQL = "INSERT INTO SES_AI_T_PERSON (person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, file_summary, vector_data, register_date, register_user, ttl) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?)";
    /**
     * SELECT文.
     */
    private final static String SELECT_SQL = "SELECT person_id, from_group, from_id, from_name, raw_content, content_summary, file_id, file_summary, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON WHERE person_id = ?";
    /**
     * UPDATE文.
     */
    private final static String UPDATE_SQL = "UPDATE SES_AI_T_PERSON SET from_group = ?, from_id = ?, from_name = ?, raw_content = ?, content_summary = ?, file_id = ?, file_summary = ?, vector_data = ?::vector, ttl = ? WHERE person_id = ?";
    /**
     * UPDATE文(file_idのみ).
     */
    private final static String UPDATE_FILE_ID_SQL = "UPDATE SES_AI_T_PERSON SET file_id = ? WHERE person_id = ?";
    /**
     * 重複チェック用SQL.
     */
    private final static String CHECK_SQL = "SELECT COUNT(*) FROM SES_AI_T_PERSON WHERE raw_content % ? AND similarity(raw_content, ?) > ?";
    /**
     * DELETE文.
     */
    private final static String DELETE_SQL = "DELETE FROM SES_AI_T_PERSON WHERE person_id = ?";

    // ================================
    // メンバ
    // ================================
    /**
     * 要員ID(PK).
     */
    @Column(
        required = true,
        primary = true,
        physicalName = "person_id",
        logicalName = "要員ID")
    private String personId;
    /**
     * 原文 / raw_content
     */
    @Column(
        physicalName = "raw_content",
        logicalName = "原文")
    private String rawContent;
    /**
     * ファイルID / file_id.
     */
    @Column(
        physicalName = "file_id",
        logicalName = "ファイルID")
    private String fileId;
    /**
     * スキルシートの要約 / file_summary.
     */
    @Column(
        physicalName = "file_summary",
        logicalName = "スキルシートの要約")
    private String fileSummary;
    /**
     * 要約 / content_summary
     */
    @Column(
        physicalName = "content_summary",
        logicalName = "要約")
    private String contentSummary;

    // ================================
    // メソッド
    // ================================
    /**
     * LLMに最もマッチする要員の要員IDを選出させるための文章に変換する.
     *
     * @return 変換後の文章
     */
    public String to要員選出用文章() {
        return "要員ID：" + this.personId + "内容：" + this.rawContent + this.fileSummary;
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
        if (connection == null || this.personId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_FILE_ID_SQL);
        preparedStatement.setString(1, this.fileId);
        preparedStatement.setString(2, this.personId);
        return preparedStatement.executeUpdate() > 0;
    }

    // ================================
    // Overrideメソッド
    // ================================
    @Override
    public void embedding(final Transformer embeddingProcessListener) throws IOException, RuntimeException {
        this.vectorData = new Vector(embeddingProcessListener);
        this.vectorData.setRawString(this.rawContent);
        this.vectorData.embedding();
    }

    @Override
    public boolean uniqueCheck(final Connection connection, final double similarityThreshold) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(CHECK_SQL);
        preparedStatement.setString(1, this.rawContent);
        preparedStatement.setString(2, this.rawContent);
        preparedStatement.setDouble(3, similarityThreshold);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getInt(1) < 1;
    }

    @Override
    public int insert(final Connection connection) throws SQLException {
        if (connection == null) {
            return 0;
        }

        // 要員IDを発行
        this.personId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
        preparedStatement.setString(1, this.personId);
        preparedStatement.setString(2, this.fromGroup);
        preparedStatement.setString(3, this.fromId);
        preparedStatement.setString(4, this.fromName);
        preparedStatement.setString(5, this.rawContent);
        preparedStatement.setString(6, this.contentSummary);
        preparedStatement.setString(7, this.fileId);
        preparedStatement.setString(8, this.fileSummary);
        preparedStatement.setString(9, this.vectorData == null ? null : this.vectorData.toString());
        preparedStatement.setTimestamp(10, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(11, this.registerUser);
        preparedStatement.setTimestamp(12, this.ttl == null ? null : this.ttl.toTimestamp());
        return preparedStatement.executeUpdate();
    }

    @Override
    public boolean updateByPk(final Connection connection) throws SQLException {
        if (connection == null || this.personId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
        preparedStatement.setString(1, this.fromGroup);
        preparedStatement.setString(2, this.fromId);
        preparedStatement.setString(3, this.fromName);
        preparedStatement.setString(4, this.rawContent);
        preparedStatement.setString(5, this.contentSummary);
        preparedStatement.setString(6, this.fileId);
        preparedStatement.setString(7, this.fileSummary);
        preparedStatement.setString(8, this.vectorData == null ? null : this.vectorData.toString());
        preparedStatement.setTimestamp(9, this.ttl == null ? null : this.ttl.toTimestamp());
        preparedStatement.setString(10, this.personId);
        return preparedStatement.executeUpdate() > 0;
    }

    @Override
    public void selectByPk(final Connection connection) throws SQLException {
        if (connection == null || this.personId == null) {
            return;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
        preparedStatement.setString(1, this.personId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            this.fromGroup = resultSet.getString("from_group");
            this.fromId = resultSet.getString("from_id");
            this.fromName = resultSet.getString("from_name");
            this.rawContent = resultSet.getString("raw_content");
            this.contentSummary = resultSet.getString("content_summary");
            this.fileId = resultSet.getString("file_id");
            this.fileSummary = resultSet.getString("file_summary");
            this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
            this.registerUser = resultSet.getString("register_user");
            this.ttl = new OriginalDateTime(resultSet.getString("ttl"));
        }
    }

    @Override
    public boolean deleteByPk(Connection connection) throws SQLException {
        if (connection == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
        preparedStatement.setString(1, this.personId);
        return preparedStatement.executeUpdate() > 0;
    }

    @Override
    public String toString() {
        return "{"
                + "\n  person_id: " + this.personId
                + "\n  from_group: " + this.fromGroup
                + "\n  from_id: " + this.fromId
                + "\n  from_name: " + this.fromName
                + "\n  raw_content: " + this.rawContent
                + "\n  content_summary: " + this.contentSummary
                + "\n  file_id: " + this.fileId
                + "\n  file_summary: " + this.fileSummary
                + "\n  vector_data: " + this.vectorData
                + "\n  register_date: " + this.registerDate
                + "\n  register_user: " + this.registerUser
                + "\n  ttl: " + this.ttl
                + "\n  distance: " + this.distance
                + "\n}";
    }
}
