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

/**
 * 【Entityクラス】
 * 案件情報(SES_AI_T_JOB)テーブル.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_T_JOB extends SES_AI_T_EntityBase {
    // ================================
    // SQL
    // ================================
    /**
     * INSERTR文.
     */
    private final static String INSERT_SQL = "INSERT INTO SES_AI_T_JOB (job_id, from_group, from_id, from_name, raw_content, vector_data, register_date, register_user, ttl) VALUES (?, ?, ?, ?, ?, ?::vector, ?, ?, ?)";
    /**
     * SELECT文.
     */
    private final static String SELECT_SQL = "SELECT job_id, from_group, from_id, from_name, raw_content, vector_data, register_date, register_user, ttl FROM SES_AI_T_JOB WHERE job_id = ?";
    /**
     * 重複チェック用SQL.
     */
    private final static String CHECK_SQL = "SELECT COUNT(*) FROM SES_AI_T_JOB WHERE raw_content % ? AND similarity(raw_content, ?) > ?";
    /**
     * DELETE文.
     */
    private final static String DELETE_SQL = "DELETE FROM SES_AI_T_JOB WHERE job_id = ?";

    // ================================
    // メンバ
    // ================================
    /**
     * 【PK】
     * 案件ID* / job_id
     */
    @Column(
        required = true,
        primary = true,
        physicalName = "job_id",
        logicalName = "案件ID")
    private String jobId;
    /**
     * 原文 / raw_content
     */
    @Column(
        physicalName = "raw_content",
        logicalName = "原文")
    private String rawContent;

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
    public int insert(Connection connection) throws SQLException {
        if (connection == null) {
            return 0;
        }

        // 案件IDを発行
        this.jobId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
        preparedStatement.setString(1, this.jobId);
        preparedStatement.setString(2, this.fromGroup);
        preparedStatement.setString(3, this.fromId);
        preparedStatement.setString(4, this.fromName);
        preparedStatement.setString(5, this.rawContent);
        preparedStatement.setString(6, this.vectorData == null ? null : this.vectorData.toString());
        preparedStatement.setTimestamp(7, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(8, this.registerUser);
        preparedStatement.setTimestamp(9, this.ttl == null ? null : this.ttl.toTimestamp());
        return preparedStatement.executeUpdate();
    }

    @Override
    public void selectByPk(final Connection connection) throws SQLException {
        if (connection == null || this.jobId == null) {
            return;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
        preparedStatement.setString(1, this.jobId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            this.fromGroup = resultSet.getString("from_group");
            this.fromId = resultSet.getString("from_id");
            this.fromName = resultSet.getString("from_name");
            this.rawContent = resultSet.getString("raw_content");
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
        preparedStatement.setString(1, this.jobId);
        return preparedStatement.executeUpdate() > 0;
    }

    @Override
    public String toString() {
        return "{\n fromGroup: " + this.fromGroup
                + "\n job_id: " + this.jobId
                + "\n from_id: " + this.fromId
                + "\n from_name: " + this.fromName
                + "\n raw_content: " + this.rawContent
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
    public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
    public String getRawContent() {
        return rawContent;
    }
    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }
}
