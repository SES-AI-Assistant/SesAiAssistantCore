package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 送信元グループマスタテーブルのエンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_M_GROUP extends EntityBase {

  public SES_AI_M_GROUP(String tenantId) {
    super(tenantId);
  }

  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_M_GROUP (from_group, group_name, register_date, register_user) VALUES (?, ?, ?, ?)";

  /** SELECT文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String SELECT_SQL =
      "SELECT from_group, group_name, register_date, register_user FROM SES_AI_M_GROUP WHERE from_group = ?";

  /** UPDATE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_M_GROUP SET group_name = ?, register_date = ?, register_user = ? WHERE from_group = ?";

  /** DELETE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_M_GROUP WHERE from_group = ?";

  /** 【PK】 送信元グループ* / from_group */
  @Column(required = true, primary = true, physicalName = "from_group", logicalName = "送信元グループ")
  private String fromGroup;

  /** 送信元グループ名 / group_name */
  @Column(physicalName = "group_name", logicalName = "送信元グループ名")
  private String groupName;

  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.fromGroup);
          stmt.setString(2, this.groupName);
          stmt.setTimestamp(3, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(4, this.registerUser);
        },
        "SES_AI_M_GROUP.insert");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.fromGroup == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.fromGroup),
        (rs) -> {
          this.fromGroup = rs.getString("from_group");
          this.groupName = rs.getString("group_name");
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
        },
        "SES_AI_M_GROUP.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.fromGroup == null) {
      return false;
    }
    return executeUpdateByPk(
        connection,
        UPDATE_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.groupName);
          stmt.setTimestamp(2, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(3, this.registerUser);
          stmt.setString(4, this.fromGroup);
        },
        "SES_AI_M_GROUP.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.fromGroup == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.fromGroup),
        "SES_AI_M_GROUP.deleteByPk");
  }
}
