package copel.sesproductpackage.core.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;

/**
 * 【Entityクラス】
 * 案件情報(SES_AI_T_JOB)テーブル.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_T_JOB {
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
     * 案件ID(PK).
     */
    private String jobId;
    /**
     * 送信元グループ.
     */
    private String fromGroup;
    /**
     * 送信者ID.
     */
    private String fromId;
    /**
     * 送信者名.
     */
    private String fromName;
    /**
     * 原文.
     */
    private String rawContent;
    /**
     * OpenAIベクトルデータ.
     */
    private Vector vectorData;
    /**
     * 登録日時.
     */
    private OriginalDateTime registerDate;
    /**
     * 登録ユーザー.
     */
    private String registerUser;
    /**
     * 有効期限.
     */
    private OriginalDateTime ttl;
    /**
     * ユークリッド距離.
     */
    private double distance;

    /**
     * INSERT処理を実行します.
     *
     * @param connection DBコネクション
     * @throws SQLException
     */
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

    /**
     * このエンティティが持つrawContentをエンベディングする.
     *
     * @param embeddingProcessListener エンベディング処理リスナー
     * @throws IOException
     * @throws RuntimeException
     */
    public void embedding(final Transformer embeddingProcessListener) throws IOException, RuntimeException {
        this.vectorData = new Vector(embeddingProcessListener);
        this.vectorData.setRawString(this.rawContent);
        this.vectorData.embedding();
    }

    /**
     * SES_AI_T_JOBテーブル内にこのエンティティの持つrawContentと類似したrawContentがあるかどうを判定する.
     *
     * @param connection DBコネクション
     * @param similarityThreshold 類似度基準値(0.0～1.0で指定する。文章の一致率を示す。例えば0.8であれば、80%以上一致する文章が存在しなければユニークであると判定)
     * @return 類似するレコードがなければtrue、あればfalse
     * @throws SQLException
     */
    public boolean uniqueCheck(final Connection connection, final double similarityThreshold) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(CHECK_SQL);
        preparedStatement.setString(1, this.rawContent);
        preparedStatement.setString(2, this.rawContent);
        preparedStatement.setDouble(3, similarityThreshold);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getInt(1) < 1;
    }

    /**
     * このオブジェクトに格納されているPKをキーにレコードを1件SELECTしこのオブジェクトに持ちます.
     *
     * @param connection DBコネクション
     * @throws SQLException
     */
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
    public String getFromGroup() {
        return fromGroup;
    }
	public void setFromGroup(String fromGroup) {
        this.fromGroup = fromGroup;
    }
    public String getFromId() {
        return fromId;
    }
    public void setFromId(String fromId) {
        this.fromId = fromId;
    }
    public String getFromName() {
        return fromName;
    }
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
    public String getRawContent() {
        return rawContent;
    }
    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }
    public Vector getVectorData() {
        return vectorData;
    }
    public void setVectorData(Vector vectorData) {
        this.vectorData = vectorData;
    }
    public OriginalDateTime getRegisterDate() {
        return registerDate;
    }
    public void setRegisterDate(OriginalDateTime registerDate) {
        this.registerDate = registerDate;
    }
    public String getRegisterUser() {
        return registerUser;
    }
    public void setRegisterUser(String registerUser) {
        this.registerUser = registerUser;
    }
    public OriginalDateTime getTtl() {
        return ttl;
    }
    public void setTtl(OriginalDateTime ttl) {
        this.ttl = ttl;
    }
    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
}
