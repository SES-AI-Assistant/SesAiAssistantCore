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
 * ウォッチ管理テーブルのエンティティ.
 *
 * @author 鈴木一矢
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class SES_AI_T_WATCH extends EntityBase {
  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_WATCH (user_id, target_id, target_type, memo, register_date, register_user, ttl) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user ttl"
          + "FROM SES_AI_T_WATCH WHERE user_id = ? AND target_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_T_WATCH SET target_type = ?, memo = ?, register_date = ?, register_user = ?, ttl = ? "
          + "WHERE user_id = ? AND target_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL =
      "DELETE FROM SES_AI_T_WATCH WHERE user_id = ? AND target_id = ?";

  /** EXISTS文. */
  private static final String EXISTS_SQL =
      "SELECT EXISTS (SELECT 1 FROM SES_AI_T_WATCH WHERE user_id = ? AND target_id = ?)";

  /** EXISTS文2. */
  private static final String EXISTS_BY_TAGET_ID_SQL =
      "SELECT EXISTS (SELECT 1 FROM SES_AI_T_WATCH WHERE target_id = ?)";

  /** ユーザーID / user_id */
  @Column(physicalName = "user_id", logicalName = "ユーザーID")
  private String userId;

  /** 対象ID / target_id */
  @Column(physicalName = "target_id", logicalName = "対象ID")
  private String targetId;

  /** 対象種別 / target_type */
  @Column(physicalName = "target_type", logicalName = "対象種別")
  private TargetType targetType;

  /** メモ / memo */
  @Column(physicalName = "memo", logicalName = "メモ")
  private String memo;

  /** 有効期限 / ttl */
  @Column(physicalName = "ttl", logicalName = "有効期限")
  protected OriginalDateTime ttl;

  /**
   * このEntityの持つユーザーIDと対象IDの組み合わせを持つレコードが存在するかどうかを判定します.
   *
   * @param connection DBコネクション
   * @return 存在すればtrue、存在しなければfalse
   * @throws SQLException
   */
  public boolean isExist(Connection connection) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(EXISTS_SQL);
    preparedStatement.setString(1, this.userId);
    preparedStatement.setString(2, this.targetId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      return resultSet.getBoolean(1);
    }
    return false;
  }

  /**
   * このEntityの持つ対象IDを持つレコードが存在するかどうかを判定します.
   *
   * @param connection DBコネクション
   * @return 存在すればtrue、存在しなければfalse
   * @throws SQLException
   */
  public boolean isExistByTargetId(Connection connection) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(EXISTS_BY_TAGET_ID_SQL);
    preparedStatement.setString(1, this.targetId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      return resultSet.getBoolean(1);
    }
    return false;
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    if (connection == null) {
      return 0;
    }

    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.userId);
    preparedStatement.setString(2, this.targetId);
    preparedStatement.setString(3, this.targetType == null ? null : this.targetType.name());
    preparedStatement.setString(4, this.memo);
    preparedStatement.setTimestamp(5, new OriginalDateTime().toTimestamp());
    preparedStatement.setString(6, this.registerUser);
    preparedStatement.setTimestamp(7, this.ttl != null ? this.ttl.toTimestamp() : null);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (connection == null || this.userId == null || this.targetId == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.userId);
    preparedStatement.setString(2, this.targetId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.userId = resultSet.getString("user_id");
      this.targetId = resultSet.getString("target_id");
      this.targetType = TargetType.getEnumByName(resultSet.getString("target_type"));
      this.memo = resultSet.getString("memo");
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
      this.ttl = new OriginalDateTime(resultSet.getString("ttl"));
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (connection == null || this.userId == null || this.targetId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
    preparedStatement.setString(1, this.targetType != null ? this.targetType.name() : null);
    preparedStatement.setString(2, this.memo);
    preparedStatement.setTimestamp(
        3, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(4, this.registerUser);
    preparedStatement.setTimestamp(5, this.ttl != null ? this.ttl.toTimestamp() : null);
    preparedStatement.setString(6, this.userId);
    preparedStatement.setString(7, this.targetId);
    return preparedStatement.executeUpdate() > 0;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null || this.userId == null || this.targetId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.userId);
    preparedStatement.setString(2, this.targetId);
    return preparedStatement.executeUpdate() > 0;
  }

  /** 対象種別Enum. */
  public enum TargetType {
    JOB,
    PERSON,
    SKILLSHEET;

    /**
     * Enum名からEnumインスタンスを取得する関数.
     *
     * @param name Enum名
     * @return TargetTypeインスタンス
     */
    public static TargetType getEnumByName(String name) {
      for (TargetType targetType : TargetType.values()) {
        if (targetType.name().equals(name)) {
          return targetType;
        }
      }
      return null;
    }
  }
}
