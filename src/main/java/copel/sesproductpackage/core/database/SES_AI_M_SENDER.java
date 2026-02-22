package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 送信者マスタテーブルのエンティティ.
 *
 * @author 鈴木一矢
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class SES_AI_M_SENDER extends EntityBase {
  /** INSERTR文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_M_SENDER (from_id, from_name, company_id, register_date, register_user) VALUES (?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT from_id, from_name, company_id, register_date, register_user FROM SES_AI_M_SENDER WHERE from_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_M_SENDER SET from_id = ?, from_name = ?, company_id = ?, register_date = ?, register_user = ? WHERE from_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_M_SENDER WHERE from_id = ?";

  /** 【PK】 送信者ID* / from_id */
  @Column(required = true, primary = true, physicalName = "from_id", logicalName = "送信者ID")
  private String fromId;

  /** 送信者名 / from_name */
  @Column(physicalName = "from_name", logicalName = "送信者名")
  private String fromName;

  /** 会社ID / company_id */
  @Column(physicalName = "company_id", logicalName = "会社ID")
  private String companyId;

  @Override
  public int insert(Connection connection) throws SQLException {
    if (connection == null) {
      return 0;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.fromId);
    preparedStatement.setString(2, this.fromName);
    preparedStatement.setString(3, this.companyId);
    preparedStatement.setTimestamp(
        4, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(5, this.registerUser);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (connection == null || this.fromId == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.fromId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.fromId = resultSet.getString("from_id");
      this.fromName = resultSet.getString("from_name");
      this.companyId = resultSet.getString("company_id");
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (connection == null || this.fromId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
    preparedStatement.setString(1, this.fromId);
    preparedStatement.setString(2, this.fromName);
    preparedStatement.setString(3, this.companyId);
    preparedStatement.setTimestamp(
        4, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(5, this.registerUser);
    preparedStatement.setString(6, this.fromId);
    return preparedStatement.executeUpdate() > 0;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null || this.fromId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.fromId);
    return preparedStatement.executeUpdate() > 0;
  }
}
