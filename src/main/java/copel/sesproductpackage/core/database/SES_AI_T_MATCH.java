package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * マッチングテーブルのエンティティ.
 *
 * @author 鈴木一矢
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class SES_AI_T_MATCH extends EntityBase {
  /** INSERTR文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_MATCH (matching_id, user_id, job_id, person_id, job_content, person_content, status_cd, register_date, register_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT matching_id, user_id, job_id, person_id, job_content, person_content, status_cd, register_date, register_user FROM SES_AI_T_MATCH WHERE matching_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_T_MATCH SET user_id = ?, job_id = ?, person_id = ?, job_content = ?, person_content = ?, status_cd = ?, register_date = ?, register_user = ? WHERE matching_id = ?";

  /** DELETE文. */
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
    if (connection == null) {
      return 0;
    }

    // マッチングIDを発行
    this.matchingId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.matchingId);
    preparedStatement.setString(2, this.userId);
    preparedStatement.setString(3, this.jobId);
    preparedStatement.setString(4, this.personId);
    preparedStatement.setString(5, this.jobContent);
    preparedStatement.setString(6, this.personContent);
    preparedStatement.setString(7, this.status == null ? null : this.status.getCode());
    preparedStatement.setTimestamp(8, new OriginalDateTime().toTimestamp());
    preparedStatement.setString(9, this.registerUser);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (connection == null || this.matchingId == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.matchingId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.userId = resultSet.getString("user_id");
      this.jobId = resultSet.getString("job_id");
      this.personId = resultSet.getString("person_id");
      this.jobContent = resultSet.getString("job_content");
      this.personContent = resultSet.getString("person_content");
      this.status = MatchingStatus.getEnum(resultSet.getString("status_cd"));
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (connection == null || this.matchingId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
    preparedStatement.setString(1, this.userId);
    preparedStatement.setString(2, this.jobId);
    preparedStatement.setString(3, this.personId);
    preparedStatement.setString(4, this.jobContent);
    preparedStatement.setString(5, this.personContent);
    preparedStatement.setString(6, this.status == null ? null : this.status.getCode());
    preparedStatement.setTimestamp(
        7, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(8, this.registerUser);
    preparedStatement.setString(9, this.matchingId);
    return preparedStatement.executeUpdate() > 0;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null || this.matchingId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.matchingId);
    return preparedStatement.executeUpdate() > 0;
  }
}
