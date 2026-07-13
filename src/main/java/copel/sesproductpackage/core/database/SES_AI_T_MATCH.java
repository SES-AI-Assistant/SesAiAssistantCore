package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * マッチングテーブルのエンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_MATCH extends EntityBase {

  public SES_AI_T_MATCH(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }

  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_MATCH (matching_id, user_id, job_id, person_id, job_content, person_content, status_cd, evaluation_text, register_date, register_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String SELECT_SQL =
      "SELECT matching_id, user_id, job_id, person_id, job_content, person_content, status_cd, evaluation_text, register_date, register_user FROM SES_AI_T_MATCH WHERE matching_id = ?";

  /** UPDATE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_T_MATCH SET user_id = ?, job_id = ?, person_id = ?, job_content = ?, person_content = ?, status_cd = ?, evaluation_text = ?, register_date = ?, register_user = ? WHERE matching_id = ?";

  /** DELETE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_T_MATCH WHERE matching_id = ?";

  /** マッチングID / matching_id */
  @Column(required = true, primary = true, physicalName = "matching_id", logicalName = "マッチングID")
  private String matchingId;

  /** 担当者ユーザーID / user_id */
  @Column(physicalName = "user_id", logicalName = "担当者ユーザーID")
  private String userId;

  /** 案件ID / job_id */
  @Column(physicalName = "job_id", logicalName = "案件ID")
  private String jobId;

  /** 案件内容 / job_content */
  @Column(physicalName = "job_content", logicalName = "案件内容")
  private String jobContent;

  /** 要員ID / person_id */
  @Column(physicalName = "person_id", logicalName = "要員ID")
  private String personId;

  /** 要員内容 / person_content */
  @Column(physicalName = "person_content", logicalName = "要員内容")
  private String personContent;

  /** MatchingStatus / status_cd */
  @Column(physicalName = "status_cd", logicalName = "MatchingStatus")
  private MatchingStatus status;

  /** 評価文 / evaluation_text */
  @Column(physicalName = "evaluation_text", logicalName = "評価文")
  private String evaluationText;

  /**
   * このレコードがjob_idを持つかどうかを判定します.
   *
   * @return 持っていればtrue、持たなければfalse
   */
  public boolean hasJobId() {
    return !OriginalStringUtils.isEmpty(this.jobId);
  }

  /**
   * このレコードがperson_idを持つかどうかを判定します.
   *
   * @return 持っていればtrue、持たなければfalse
   */
  public boolean hasPersonId() {
    return !OriginalStringUtils.isEmpty(this.personId);
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    // マッチングIDを発行
    this.matchingId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.matchingId);
          stmt.setString(2, this.userId);
          stmt.setString(3, this.jobId);
          stmt.setString(4, this.personId);
          stmt.setString(5, this.jobContent);
          stmt.setString(6, this.personContent);
          stmt.setString(7, this.status == null ? null : this.status.getCode());
          stmt.setString(8, this.evaluationText);
          stmt.setTimestamp(9, new OriginalDateTime().toTimestamp());
          stmt.setString(10, this.registerUser);
        },
        "SES_AI_T_MATCH.insert");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.matchingId == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.matchingId),
        (rs) -> {
          this.userId = rs.getString("user_id");
          this.jobId = rs.getString("job_id");
          this.personId = rs.getString("person_id");
          this.jobContent = rs.getString("job_content");
          this.personContent = rs.getString("person_content");
          this.status = MatchingStatus.getEnum(rs.getString("status_cd"));
          this.evaluationText = rs.getString("evaluation_text");
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
        },
        "SES_AI_T_MATCH.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.matchingId == null) {
      return false;
    }
    return executeUpdateByPk(
        connection,
        UPDATE_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.jobId);
          stmt.setString(3, this.personId);
          stmt.setString(4, this.jobContent);
          stmt.setString(5, this.personContent);
          stmt.setString(6, this.status == null ? null : this.status.getCode());
          stmt.setString(7, this.evaluationText);
          stmt.setTimestamp(8, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(9, this.registerUser);
          stmt.setString(10, this.matchingId);
        },
        "SES_AI_T_MATCH.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.matchingId == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.matchingId),
        "SES_AI_T_MATCH.deleteByPk");
  }
}
