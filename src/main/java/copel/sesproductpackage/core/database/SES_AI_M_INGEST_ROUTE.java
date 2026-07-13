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
 * 【Entityクラス】 テナント取込ルーティングマスタ(SES_AI_M_INGEST_ROUTE)テーブル.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_M_INGEST_ROUTE extends EntityBase {

  public SES_AI_M_INGEST_ROUTE(String tenantId) {
    super(tenantId);
  }

  /** チャネル種別の列挙型. */
  public enum ChannelType {
    LINE("LINE"),
    EMAIL("EMAIL");

    private final String value;

    ChannelType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static ChannelType fromValue(String value) {
      for (ChannelType ct : ChannelType.values()) {
        if (ct.value.equals(value)) {
          return ct;
        }
      }
      throw new IllegalArgumentException("Invalid channel type: " + value);
    }
  }

  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_M_INGEST_ROUTE (channel_type, route_key, register_date, register_user) VALUES (?, ?, ?, ?)";

  /** SELECT文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String SELECT_SQL =
      "SELECT channel_type, route_key, register_date, register_user FROM SES_AI_M_INGEST_ROUTE WHERE channel_type = ? AND route_key = ?";

  /** UPDATE文（tenantId 条件を含む）. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_M_INGEST_ROUTE SET register_user = ? WHERE channel_type = ? AND route_key = ? AND tenant_id = ?";

  /** DELETE文（tenantId 条件を含む）. */
  private static final String DELETE_SQL =
      "DELETE FROM SES_AI_M_INGEST_ROUTE WHERE channel_type = ? AND route_key = ? AND tenant_id = ?";

  /** 【PK】 チャネル種別 / channel_type */
  @Column(required = true, primary = true, physicalName = "channel_type", logicalName = "チャネル種別")
  private ChannelType channelType;

  /** 【PK】 ルートキー / route_key */
  @Column(required = true, primary = true, physicalName = "route_key", logicalName = "ルートキー")
  private String routeKey;

  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.channelType == null ? null : this.channelType.getValue());
          stmt.setString(2, this.routeKey);
          stmt.setTimestamp(3, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(4, this.registerUser);
        },
        "SES_AI_M_INGEST_ROUTE.insert");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.channelType == null || this.routeKey == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.channelType.getValue());
          stmt.setString(2, this.routeKey);
        },
        (rs) -> {
          this.channelType = ChannelType.fromValue(rs.getString("channel_type"));
          this.routeKey = rs.getString("route_key");
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
        },
        "SES_AI_M_INGEST_ROUTE.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.channelType == null || this.routeKey == null) {
      return false;
    }
    return executeUpdateByPkWithoutTenantFilter(
        connection,
        UPDATE_SQL,
        (stmt) -> {
          stmt.setString(1, this.registerUser);
          stmt.setString(2, this.channelType.getValue());
          stmt.setString(3, this.routeKey);
          stmt.setString(4, this.tenantId);
        },
        "SES_AI_M_INGEST_ROUTE.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.channelType == null || this.routeKey == null) {
      return false;
    }
    return executeDeleteByPkWithoutTenantFilter(
        connection,
        DELETE_SQL,
        (stmt) -> {
          stmt.setString(1, this.channelType.getValue());
          stmt.setString(2, this.routeKey);
          stmt.setString(3, this.tenantId);
        },
        "SES_AI_M_INGEST_ROUTE.deleteByPk");
  }
}
