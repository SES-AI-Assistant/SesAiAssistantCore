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
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_WATCH extends EntityBase {

  public SES_AI_T_WATCH(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }
  /** INSERT文（tenantId を含む）. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_WATCH (user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String SELECT_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl "
          + "FROM SES_AI_T_WATCH WHERE user_id = ? AND target_id = ?";

  /** UPDATE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_T_WATCH SET target_type = ?, memo = ?, register_date = ?, register_user = ?, ttl = ? "
          + "WHERE user_id = ? AND target_id = ?";

  /** DELETE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
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

  /** テナントID / tenant_id（Phase 1 テナント対応） */
  @Column(physicalName = "tenant_id", logicalName = "テナントID")
  private String tenantId;

  /**
   * このEntityの持つユーザーIDと対象IDの組み合わせを持つレコードが存在するかどうかを判定します.
   *
   * @param connection DBコネクション
   * @return 存在すればtrue、存在しなければfalse
   * @throws SQLException
   */
  public boolean isExist(Connection connection) throws SQLException {
    return executeExists(
        connection,
        EXISTS_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.targetId);
        },
        "SES_AI_T_WATCH.isExist");
  }

  /**
   * このEntityの持つ対象IDを持つレコードが存在するかどうかを判定します.
   *
   * @param connection DBコネクション
   * @return 存在すればtrue、存在しなければfalse
   * @throws SQLException
   */
  public boolean isExistByTargetId(Connection connection) throws SQLException {
    return executeExists(
        connection,
        EXISTS_BY_TAGET_ID_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.targetId),
        "SES_AI_T_WATCH.isExistByTargetId");
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.targetId);
          stmt.setString(3, this.targetType == null ? null : this.targetType.name());
          stmt.setString(4, this.memo);
          stmt.setTimestamp(5, new OriginalDateTime().toTimestamp());
          stmt.setString(6, this.registerUser);
          stmt.setTimestamp(7, this.ttl != null ? this.ttl.toTimestamp() : null);
          stmt.setString(8, this.tenantId);
        },
        "SES_AI_T_WATCH.insert");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.userId == null || this.targetId == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.targetId);
        },
        (rs) -> {
          this.userId = rs.getString("user_id");
          this.targetId = rs.getString("target_id");
          this.targetType = TargetType.getEnumByName(rs.getString("target_type"));
          this.memo = rs.getString("memo");
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
          this.ttl = new OriginalDateTime(rs.getString("ttl"));
        },
        "SES_AI_T_WATCH.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.userId == null || this.targetId == null) {
      return false;
    }
    return executeUpdateByPk(
        connection,
        UPDATE_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.targetType != null ? this.targetType.name() : null);
          stmt.setString(2, this.memo);
          stmt.setTimestamp(3, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(4, this.registerUser);
          stmt.setTimestamp(5, this.ttl != null ? this.ttl.toTimestamp() : null);
          stmt.setString(6, this.userId);
          stmt.setString(7, this.targetId);
        },
        "SES_AI_T_WATCH.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.userId == null || this.targetId == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.targetId);
        },
        "SES_AI_T_WATCH.deleteByPk");
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
