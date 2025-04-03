package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.マッチング状態区分;

/**
 * マッチングテーブルのエンティティ.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_T_MATCH extends EntityBase {
    /**
     * INSERTR文.
     */
    private final static String INSERT_SQL = "INSERT INTO SES_AI_T_MATCH (job_id, person_id, status_cd, register_date, register_user) VALUES (?, ?, ?, ?, ?)";
    /**
     * SELECT文.
     */
    private final static String SELECT_SQL = "SELECT job_id, person_id, status_cd, register_date, register_user FROM SES_AI_T_MATCH WHERE job_id = ? AND person_id = ?";
    /**
     * UPDATE文.
     */
    private final static String UPDATE_SQL = "UPDATE SES_AI_T_MATCH SET job_id = ?, person_id = ?, status_cd = ?, register_date = ?, register_user = ? WHERE job_id = ? AND person_id = ?";
    /**
     * DELETE文.
     */
    private final static String DELETE_SQL = "DELETE FROM SES_AI_T_MATCH WHERE job_id = ? AND person_id = ?";

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
     * 【PK】
     * 要員ID* / person_id
     */
    @Column(
        required = true,
        primary = true,
        physicalName = "person_id",
        logicalName = "要員ID")
    private String personId;
    /**
     * マッチング状態区分 / status_cd
     */
    @Column(
        physicalName = "status_cd",
        logicalName = "マッチング状態区分")
    private マッチング状態区分 status;

    /**
     * コンストラクタ.
     */
    public SES_AI_T_MATCH() {
        super();
    }

    @Override
    public int insert(Connection connection) throws SQLException {
        if (connection == null) {
            return 0;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
        preparedStatement.setString(1, this.jobId);
        preparedStatement.setString(2, this.personId);
        preparedStatement.setString(3, this.status == null ? null : this.status.getCode());
        preparedStatement.setTimestamp(4, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(5, this.registerUser);
        return preparedStatement.executeUpdate();
    }

    @Override
    public void selectByPk(Connection connection) throws SQLException {
        if (connection == null || this.personId == null || this.jobId == null) {
            return;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
        preparedStatement.setString(1, this.jobId);
        preparedStatement.setString(2, this.personId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            this.jobId = resultSet.getString("job_id");
            this.personId = resultSet.getString("person_id");
            this.status = マッチング状態区分.getEnum(resultSet.getString("status_cd"));
            this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
            this.registerUser = resultSet.getString("register_user");
        }
    }

    @Override
    public boolean updateByPk(Connection connection) throws SQLException {
        if (connection == null || this.personId == null || this.jobId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
        preparedStatement.setString(1, this.jobId);
        preparedStatement.setString(2, this.personId);
        preparedStatement.setString(3, this.status == null ? null : this.status.getCode());
        preparedStatement.setTimestamp(4, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(5, this.registerUser);
        preparedStatement.setString(6, this.jobId);
        preparedStatement.setString(7, this.personId);
        return preparedStatement.executeUpdate() > 0;
    }

    @Override
    public boolean deleteByPk(Connection connection) throws SQLException {
        if (connection == null || this.personId == null || this.jobId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
        preparedStatement.setString(1, this.jobId);
        preparedStatement.setString(2, this.personId);
        return preparedStatement.executeUpdate() > 0;
    }

    // ================================
    // GETTER / SETTER
    // ================================
    public String getJobId() {
        return jobId;
    }
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    public String getPersonId() {
        return personId;
    }
    public void setPersonId(String personId) {
        this.personId = personId;
    }
    public マッチング状態区分 getStatus() {
        return status;
    }
    public void setStatus(マッチング状態区分 status) {
        this.status = status;
    }
}
