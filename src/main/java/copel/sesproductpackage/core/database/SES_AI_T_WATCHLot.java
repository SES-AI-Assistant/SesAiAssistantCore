package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.SES_AI_T_WATCH.TargetType;
import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * 【Entityクラス】 ウォッチ管理テーブル(SES_AI_T_WATCH)のLotクラス.
 *
 * @author 鈴木一矢
 */
public class SES_AI_T_WATCHLot extends EntityLotBase<SES_AI_T_WATCH> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl FROM SES_AI_T_WATCH";

  /** SELECT文(ユーザーIDキー). */
  private static final String SELECT_BY_USER_ID_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl FROM SES_AI_T_WATCH WHERE user_id = ?";

  public SES_AI_T_WATCHLot() {
    super();
  }

  /**
   * ユーザーIDが一致するレコードを全て取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param userId ユーザーID
   * @throws SQLException
   */
  public void selectByUserId(Connection connection, String userId) throws SQLException {
    if (connection == null || userId == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_USER_ID_SQL);
    preparedStatement.setString(1, userId);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  /**
   * このLotに引数のIDと一致するIDを持つレコードが存在するかどうかを判定します.
   *
   * @param targetId 対象ID
   * @return 存在すればtrue、しなければfalse
   */
  public boolean containsById(String targetId) {
    if (targetId != null && !targetId.isEmpty()) {
      for (SES_AI_T_WATCH entity : this.entityLot) {
        if (targetId.equals(entity.getTargetId())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void selectAll(Connection connection) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  @Override
  protected SES_AI_T_WATCH mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_WATCH sesAiTWatch = new SES_AI_T_WATCH();
    sesAiTWatch.setUserId(resultSet.getString("user_id"));
    sesAiTWatch.setTargetId(resultSet.getString("target_id"));
    sesAiTWatch.setTargetType(TargetType.getEnumByName(resultSet.getString("target_type")));
    sesAiTWatch.setMemo(resultSet.getString("memo"));
    sesAiTWatch.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiTWatch.setRegisterUser(resultSet.getString("register_user"));
    sesAiTWatch.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return sesAiTWatch;
  }
}
