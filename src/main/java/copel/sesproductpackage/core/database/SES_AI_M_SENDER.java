package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.SQLException;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 送信者マスタテーブルのエンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_M_SENDER extends EntityBase {

  public SES_AI_M_SENDER(String tenantId) {
    super(tenantId);
  }

  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_M_SENDER (from_id, from_name, register_date, register_user) VALUES (?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT from_id, from_name, register_date, register_user FROM SES_AI_M_SENDER WHERE from_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_M_SENDER SET from_name = ?, register_date = ?, register_user = ? WHERE from_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_M_SENDER WHERE from_id = ?";

  /** EXISTS文. */
  private static final String EXISTS_SQL =
      "SELECT EXISTS (SELECT 1 FROM SES_AI_M_SENDER WHERE from_id = ?)";

  /** 【PK】 送信者ID* / from_id */
  @Column(required = true, primary = true, physicalName = "from_id", logicalName = "送信者ID")
  private String fromId;

  /** 送信者名 / from_name */
  @Column(physicalName = "from_name", logicalName = "送信者名")
  private String fromName;

  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.fromId);
          stmt.setString(2, this.fromName);
          stmt.setTimestamp(3, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(4, this.registerUser);
        },
        "SES_AI_M_SENDER.insert");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.fromId == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.fromId),
        (rs) -> {
          this.fromId = rs.getString("from_id");
          this.fromName = rs.getString("from_name");
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
        },
        "SES_AI_M_SENDER.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.fromId == null) {
      return false;
    }
    return executeUpdateByPk(
        connection,
        UPDATE_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.fromName);
          stmt.setTimestamp(2, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(3, this.registerUser);
          stmt.setString(4, this.fromId);
        },
        "SES_AI_M_SENDER.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.fromId == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.fromId),
        "SES_AI_M_SENDER.deleteByPk");
  }

  /**
   * 送信者マスタに該当IDの送信者が存在するかを判定する.
   *
   * @param connection DB接続
   * @return 存在する場合はtrue、存在しない場合またはconnectionがnullの場合はfalse
   * @throws SQLException SQL実行エラー時
   */
  public boolean isExist(Connection connection) throws SQLException {
    if (this.fromId == null) {
      return false;
    }
    return executeExists(
        connection,
        EXISTS_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.fromId),
        "SES_AI_M_SENDER.isExist");
  }
}
