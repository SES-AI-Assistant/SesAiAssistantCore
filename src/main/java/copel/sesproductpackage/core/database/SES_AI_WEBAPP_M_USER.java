package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Role;

/**
 * システムユーザーマスタテーブルのエンティティ.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_WEBAPP_M_USER extends EntityBase {
    /**
     * INSERTR文.
     */
    private final static String INSERT_SQL = "INSERT INTO SES_AI_WEBAPP_M_USER (user_id, user_name, user_password, company_id, role_cd, register_date, register_user) VALUES (?, ?, ?, ?, ?, ?, ?)";
    /**
     * SELECT文.
     */
    private final static String SELECT_SQL = "SELECT user_id, user_name, user_password, company_id, role_cd, register_date, register_user FROM SES_AI_WEBAPP_M_USER WHERE user_id = ?";
    /**
     * UPDATE文.
     */
    private final static String UPDATE_SQL = "UPDATE SES_AI_WEBAPP_M_USER SET user_id = ?, user_name = ?, user_password = ?, company_id = ?, role_cd = ?, register_date = ?, register_user = ? WHERE user_id = ?";
    /**
     * DELETE文.
     */
    private final static String DELETE_SQL = "DELETE FROM SES_AI_WEBAPP_M_USER WHERE user_id = ?";

    /**
     * 【PK】
     * ユーザーID* / user_id
     */
    @Column(
        required = true,
        primary = true,
        physicalName = "user_id",
        logicalName = "ユーザーID")
    private String userId;
    /**
     * ユーザー名 / user_name
     */
    @Column(
        physicalName = "user_name",
        logicalName = "ユーザー名")
    private String userName;
    /**
     * パスワード / user_password
     */
    @Column(
        physicalName = "user_password",
        logicalName = "パスワード")
    private String userPassword;
    /**
     * 会社ID / company_id
     */
    @Column(
        physicalName = "company_id",
        logicalName = "会社ID")
    private String companyId;
    /**
     * ロール / role_cd
     */
    @Column(
        physicalName = "role_cd",
        logicalName = "ロール")
    private Role role;

    /**
     * コンストラクタ.
     */
    public SES_AI_WEBAPP_M_USER() {
        super();
    }

    /**
     * このユーザーがシステム利用可能であるかどうかを判定します.
     *
     * @return 利用可能であればtrue、不可であればfalse
     */
    public boolean hasSystemUseAuth() {
        return this.role.isSystemUseAuth();
    }

    @Override
    public int insert(Connection connection) throws SQLException {
        if (connection == null) {
            return 0;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
        preparedStatement.setString(1, this.userId);
        preparedStatement.setString(2, this.userName);
        preparedStatement.setString(3, this.userPassword);
        preparedStatement.setString(4, this.companyId);
        preparedStatement.setString(5, this.role == null ? null : this.role.getCode());
        preparedStatement.setTimestamp(6, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(7, this.registerUser);
        return preparedStatement.executeUpdate();
    }

    @Override
    public void selectByPk(Connection connection) throws SQLException {
        if (connection == null || this.userId == null) {
            return;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
        preparedStatement.setString(1, this.userId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            this.userId = resultSet.getString("user_id");
            this.userName = resultSet.getString("user_name");
            this.userPassword = resultSet.getString("user_password");
            this.companyId = resultSet.getString("company_id");
            this.role = Role.getEnum(resultSet.getString("role_cd"));
            this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
            this.registerUser = resultSet.getString("register_user");
        }
    }

    @Override
    public boolean updateByPk(Connection connection) throws SQLException {
        if (connection == null || this.userId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
        preparedStatement.setString(1, this.userId);
        preparedStatement.setString(2, this.userName);
        preparedStatement.setString(3, this.userPassword);
        preparedStatement.setString(4, this.companyId);
        preparedStatement.setString(5, this.role == null ? null : this.role.getCode());
        preparedStatement.setTimestamp(6, this.registerDate == null ? null : this.registerDate.toTimestamp());
        preparedStatement.setString(7, this.registerUser);
        preparedStatement.setString(8, this.userId);
        return preparedStatement.executeUpdate() > 0;
    }

    @Override
    public boolean deleteByPk(Connection connection) throws SQLException {
        if (connection == null || this.userId == null) {
            return false;
        }
        PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
        preparedStatement.setString(1, this.userId);
        return preparedStatement.executeUpdate() > 0;
    }

    // ================================
    // GETTER / SETTER
    // ================================
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getUserPassword() {
        return userPassword;
    }
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }
    public String getCompanyId() {
        return companyId;
    }
    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }
    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }
}
