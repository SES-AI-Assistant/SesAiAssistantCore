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
 * 送信元グループマスタテーブルのエンティティ.
 *
 * @author 鈴木一矢
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class SES_AI_M_GROUP extends EntityBase {
  /** INSERTR文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_M_GROUP (from_group, group_name, register_date, register_user) VALUES (?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT from_group, group_name, register_date, register_user FROM SES_AI_M_GROUP WHERE from_group = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_M_GROUP SET from_group = ?, group_name = ?, register_date = ?, register_user = ? WHERE from_group = ?";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_M_GROUP WHERE from_group = ?";

  /** 【PK】 送信元グループ* / from_group */
  @Column(required = true, primary = true, physicalName = "from_group", logicalName = "送信元グループ")
  private String fromGroup;

  /** 送信元グループ名 / group_name */
  @Column(physicalName = "group_name", logicalName = "送信元グループ名")
  private String groupName;

  @Override
  public int insert(Connection connection) throws SQLException {
    if (connection == null) {
      return 0;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.fromGroup);
    preparedStatement.setString(2, this.groupName);
    preparedStatement.setTimestamp(
        3, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(4, this.registerUser);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (connection == null || this.fromGroup == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.fromGroup);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.fromGroup = resultSet.getString("from_group");
      this.groupName = resultSet.getString("group_name");
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (connection == null || this.fromGroup == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
    preparedStatement.setString(1, this.fromGroup);
    preparedStatement.setString(2, this.groupName);
    preparedStatement.setTimestamp(
        3, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(4, this.registerUser);
    preparedStatement.setString(5, this.fromGroup);
    return preparedStatement.executeUpdate() > 0;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null || this.fromGroup == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.fromGroup);
    return preparedStatement.executeUpdate() > 0;
  }
}
