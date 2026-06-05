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
 * 【Entityクラス】 テナント取込ルーティングマスタ(SES_AI_M_INGEST_ROUTE)テーブル.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class SES_AI_M_INGEST_ROUTE extends EntityBase {
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
      "INSERT INTO SES_AI_M_INGEST_ROUTE (channel_type, route_key, tenant_id, register_date, register_user) VALUES (?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT channel_type, route_key, tenant_id, register_date, register_user FROM SES_AI_M_INGEST_ROUTE WHERE channel_type = ? AND route_key = ? AND tenant_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_M_INGEST_ROUTE SET register_user = ? WHERE channel_type = ? AND route_key = ? AND tenant_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_M_INGEST_ROUTE WHERE channel_type = ? AND route_key = ? AND tenant_id = ?";

  /** 【PK】 チャネル種別 / channel_type */
  @Column(required = true, primary = true, physicalName = "channel_type", logicalName = "チャネル種別")
  private ChannelType channelType;

  /** 【PK】 ルートキー / route_key */
  @Column(required = true, primary = true, physicalName = "route_key", logicalName = "ルートキー")
  private String routeKey;

  /** 【PK】 テナントID / tenant_id */
  @Column(required = true, primary = true, physicalName = "tenant_id", logicalName = "テナントID")
  private String tenantId;

  @Override
  public int insert(Connection connection) throws SQLException {
    if (connection == null) {
      return 0;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.channelType == null ? null : this.channelType.getValue());
    preparedStatement.setString(2, this.routeKey);
    preparedStatement.setString(3, this.tenantId);
    preparedStatement.setTimestamp(
        4, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(5, this.registerUser);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (connection == null || this.channelType == null || this.routeKey == null || this.tenantId == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.channelType.getValue());
    preparedStatement.setString(2, this.routeKey);
    preparedStatement.setString(3, this.tenantId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.channelType = ChannelType.fromValue(resultSet.getString("channel_type"));
      this.routeKey = resultSet.getString("route_key");
      this.tenantId = resultSet.getString("tenant_id");
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (connection == null || this.channelType == null || this.routeKey == null || this.tenantId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
    preparedStatement.setString(1, this.registerUser);
    preparedStatement.setString(2, this.channelType.getValue());
    preparedStatement.setString(3, this.routeKey);
    preparedStatement.setString(4, this.tenantId);
    return preparedStatement.executeUpdate() > 0;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null || this.channelType == null || this.routeKey == null || this.tenantId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.channelType.getValue());
    preparedStatement.setString(2, this.routeKey);
    preparedStatement.setString(3, this.tenantId);
    return preparedStatement.executeUpdate() > 0;
  }
}
