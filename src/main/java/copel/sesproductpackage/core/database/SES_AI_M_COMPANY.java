package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;

/**
 * 会社マスタテーブルのエンティティ.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_M_COMPANY extends EntityBase {
    /**
     * INSERTR文.
     */
    private final static String INSERT_SQL = "INSERT INTO SES_AI_M_COMPANY (company_id, company_name, memo, register_date, register_user) VALUES (?, ?, ?, ?, ?)";
    /**
     * SELECT文.
     */
    private final static String SELECT_SQL = "SELECT company_id, company_name, memo, register_date, register_user FROM SES_AI_M_COMPANY WHERE company_id = ?";
    /**
     * UPDATE文.
     */
    private final static String UPDATE_SQL = "UPDATE SES_AI_M_COMPANY SET company_id = ?, company_name = ?, memo = ?, register_date = ?, register_user = ? WHERE company_id = ?";
    /**
     * DELETE文.
     */
    private final static String DELETE_SQL = "DELETE FROM SES_AI_M_COMPANY WHERE company_id = ?";

    /**
     * 【PK】
     * 会社ID* / company_id
     */
    @Column(
        required = true,
        primary = true,
        physicalName = "company_id",
        logicalName = "会社ID")
    private String companyId;
    /**
     * 会社名 / company_name
     */
    @Column(
        physicalName = "company_name",
        logicalName = "会社名")
    private String companyName;
    /**
     * メモ / memo
     */
    @Column(
        physicalName = "memo",
        logicalName = "メモ")
    private String memo;

    /**
     * コンストラクタ.
     */
    public SES_AI_M_COMPANY() {
        super();
    }

    @Override
    public int insert(Connection connection) throws SQLException {
        if (connection == null) {
            return 0;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
        preparedStatement.setString(1, this.companyId);
        preparedStatement.setString(2, this.companyName);
        preparedStatement.setString(3, this.memo);
        preparedStatement.setTimestamp(4, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(5, this.registerUser);
        return preparedStatement.executeUpdate();
    }

    @Override
    public void selectByPk(Connection connection) throws SQLException {
        if (connection == null || this.companyId == null) {
            return;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
        preparedStatement.setString(1, this.companyId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            this.companyId = resultSet.getString("company_id");
            this.companyName = resultSet.getString("company_name");
            this.memo = resultSet.getString("memo");
            this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
            this.registerUser = resultSet.getString("register_user");
        }
    }

    @Override
    public boolean updateByPk(Connection connection) throws SQLException {
        if (connection == null || this.companyId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
        preparedStatement.setString(1, this.companyId);
        preparedStatement.setString(2, this.companyName);
        preparedStatement.setString(3, this.memo);
        preparedStatement.setTimestamp(4, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(5, this.registerUser);
        preparedStatement.setString(6, this.companyId);
        return preparedStatement.executeUpdate() > 0;
    }

    @Override
    public boolean deleteByPk(Connection connection) throws SQLException {
        if (connection == null || this.companyId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
        preparedStatement.setString(1, this.companyId);
        return preparedStatement.executeUpdate() > 0;
    }

    // ================================
    // GETTER / SETTER
    // ================================
    public String getCompanyId() {
        return companyId;
    }
    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }
    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    public String getMemo() {
        return memo;
    }
    public void setMemo(String memo) {
        this.memo = memo;
    }
}
