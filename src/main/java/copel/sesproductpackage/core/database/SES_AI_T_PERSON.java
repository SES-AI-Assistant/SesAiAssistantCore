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
import copel.sesproductpackage.core.util.OriginalStringUtils;

/**
 * 【Entityクラス】
 * 要員情報(SES_AI_T_PERSON)テーブル.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_T_PERSON implements Comparable<SES_AI_T_PERSON> {
    /**
     * INSERTR文.
     */
    private final static String INSERT_SQL = "INSERT INTO SES_AI_T_PERSON (person_id, from_group, from_id, from_name, raw_content, file_id, vector_data, register_date, register_user, ttl) VALUES (?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?)";
    /**
     * SELECT文.
     */
    private final static String SELECT_SQL = "SELECT person_id, from_group, from_id, from_name, raw_content, file_id, vector_data, register_date, register_user, ttl FROM SES_AI_T_PERSON WHERE person_id = ?";
    /**
     * UPDATE文.
     */
    private final static String UPDATE_SQL = "UPDATE SES_AI_T_PERSON SET from_group = ?, from_id = ?, from_name = ?, raw_content = ?, file_id = ?, vector_data = ?::vector, ttl = ? WHERE person_id = ?";
    /**
     * 重複チェック用SQL.
     */
    private final static String CHECK_SQL = "SELECT COUNT(*) FROM SES_AI_T_PERSON WHERE raw_content % ? AND similarity(raw_content, ?) > ?";

    /**
     * 要員ID(PK).
     */
    private String personId;
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
     * ファイルID.
     */
    private String fileId;
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
     * このレコードがfile_idを持つかどうかを返却します.
     *
     * @return file_idを持つならtrue、そうでないならfalse
     */
    public boolean isスキルシート登録済() {
        return OriginalStringUtils.isEmpty(this.fileId);
    }

    /**
     * INSERT処理を実行します.
     *
     * @param connection DBコネクション
     * @throws SQLException
     */
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
        preparedStatement.setString(6, this.fileId);
        preparedStatement.setString(7, this.vectorData == null ? null : this.vectorData.toString());
        preparedStatement.setTimestamp(8, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(9, this.registerUser);
        preparedStatement.setTimestamp(10, this.ttl == null ? null : this.ttl.toTimestamp());
        return preparedStatement.executeUpdate();
    }

    /**
     * UPDATE処理を実行します.
     *
     * @param connection DBコネクション
     * @return 成功 or 失敗
     * @throws SQLException
     */
    public boolean updateByPk(final Connection connection) throws SQLException {
        if (connection == null || this.personId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
        preparedStatement.setString(1, this.fromGroup);
        preparedStatement.setString(2, this.fromId);
        preparedStatement.setString(3, this.fromName);
        preparedStatement.setString(4, this.rawContent);
        preparedStatement.setString(5, this.fileId);
        preparedStatement.setString(6, this.vectorData == null ? null : this.vectorData.toString());
        preparedStatement.setTimestamp(7, this.ttl == null ? null : this.ttl.toTimestamp());
        preparedStatement.setString(8, this.personId);
        return preparedStatement.executeUpdate() > 0;
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
     * SES_AI_T_PERSONテーブル内にこのエンティティの持つrawContentと類似したrawContentがあるかどうを判定する.
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
        if (connection == null || this.personId == null) {
            return;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
        preparedStatement.setString(1, this.personId);
        ResultSet resultSet = preparedStatement.executeQuery();
        this.fromGroup = resultSet.getString("from_group");
        this.fromId = resultSet.getString("from_id");
        this.fromName = resultSet.getString("from_name");
        this.rawContent = resultSet.getString("raw_content");
        this.fileId = resultSet.getString("file_id");
        this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
        this.registerUser = resultSet.getString("register_user");
        this.ttl = new OriginalDateTime(resultSet.getString("ttl"));
    }

    /**
     * LLMに最もマッチする要員の要員IDを選出させるための文章に変換する.
     *
     * @return 変換後の文章
     */
    public String to要員選出用文章() {
    	return "要員ID：" + this.personId + "内容：" + this.rawContent;
    }

    @Override
    public String toString() {
        return "{"
                + "\n  personId: " + this.personId
                + "\n  fromGroup: " + this.fromGroup
                + "\n  fromId: " + this.fromId
                + "\n  fromName: " + this.fromName
                + "\n  skillsheetId: " + this.fileId
                + "\n  rawContent: " + this.rawContent
                + "\n  vectorData: " + this.vectorData
                + "\n  registerDate: " + this.registerDate
                + "\n  registerUser: " + this.registerUser
                + "\n  ttl: " + this.ttl
                + "\n  distance: " + this.distance
                + "\n}";
    }

    // ================================
    // Getter / Setter
    // ================================
    public String getPersonId() {
		return personId;
	}
	public void setPersonId(String personId) {
		this.personId = personId;
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
    public String getFileId() {
        return fileId;
    }
    public void setFileId(String fileId) {
        this.fileId = fileId;
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

    @Override
    public int compareTo(SES_AI_T_PERSON o) {
        return Double.compare(this.getDistance(), o.getDistance());
    }
}
