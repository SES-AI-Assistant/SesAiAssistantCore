package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.SES_AI_M_INGEST_ROUTE.ChannelType;
import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * 【Lotクラス】 テナント取込ルーティングマスタ(SES_AI_M_INGEST_ROUTE)テーブル.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_M_INGEST_ROUTELot extends EntityLotBase<SES_AI_M_INGEST_ROUTE> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT channel_type, route_key, tenant_id, register_date, register_user FROM SES_AI_M_INGEST_ROUTE";

  /** チャネルタイプとルートキーで検索するSELECT文. */
  private static final String SELECT_BY_CHANNEL_AND_ROUTE_SQL =
      "SELECT channel_type, route_key, tenant_id, register_date, register_user FROM SES_AI_M_INGEST_ROUTE WHERE channel_type = ? AND route_key = ?";

  /** チャネルタイプとルートキーで検索するSELECT文（テナントID指定なし）. */
  private static final String SELECT_BY_CHANNEL_AND_ROUTE_WITHOUT_TENANT_SQL =
      "SELECT channel_type, route_key, tenant_id, register_date, register_user FROM SES_AI_M_INGEST_ROUTE WHERE channel_type = ? AND route_key = ?";

  public SES_AI_M_INGEST_ROUTELot() {
    super();
  }

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  protected String getSelectSql() {
    return "SELECT channel_type, route_key, tenant_id, register_date, register_user FROM SES_AI_M_INGEST_ROUTE WHERE ";
  }

  @Override
  public void selectAll(final Connection connection, final String tenantId) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL + " WHERE tenant_id = ?");
    preparedStatement.setString(1, tenantId);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  @Override
  protected SES_AI_M_INGEST_ROUTE mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_M_INGEST_ROUTE sesAiMIngestRoute = new SES_AI_M_INGEST_ROUTE(tenantId);
    sesAiMIngestRoute.setChannelType(ChannelType.fromValue(resultSet.getString("channel_type")));
    sesAiMIngestRoute.setRouteKey(resultSet.getString("route_key"));
    sesAiMIngestRoute.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiMIngestRoute.setRegisterUser(resultSet.getString("register_user"));
    return sesAiMIngestRoute;
  }

  /**
   * チャネルタイプとルートキーで検索し、該当する全テナントID を取得する（テナント対応）.
   *
   * @param connection DB接続
   * @param tenantId テナントID
   * @param channelType チャネルタイプ (LINE / EMAIL)
   * @param routeKey ルートキー (メールアドレス、LINE ID など)
   * @throws SQLException DB例外
   */
  public void selectByChannelTypeAndRouteKey(final Connection connection, final String tenantId, final ChannelType channelType, final String routeKey) throws SQLException {
    if (connection == null || channelType == null || routeKey == null) {
      this.entityLot = new ArrayList<>();
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_CHANNEL_AND_ROUTE_SQL);
    preparedStatement.setString(1, channelType.getValue());
    preparedStatement.setString(2, routeKey);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  /**
   * チャネルタイプとルートキーで検索し、該当する全テナントID を取得する（テナント指定なし）.
   *
   * @param connection DB接続
   * @param channelType チャネルタイプ (LINE / EMAIL)
   * @param routeKey ルートキー (メールアドレス、LINE ID など)
   * @throws SQLException DB例外
   */
  public void selectByChannelTypeAndRouteKeyWithoutTenantId(final Connection connection, final ChannelType channelType, final String routeKey) throws SQLException {
    if (connection == null || channelType == null || routeKey == null) {
      this.entityLot = new ArrayList<>();
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_CHANNEL_AND_ROUTE_WITHOUT_TENANT_SQL);
    preparedStatement.setString(1, channelType.getValue());
    preparedStatement.setString(2, routeKey);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }
}
