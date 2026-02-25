package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * 【Entityクラス】 マッチング(SES_AI_T_MATCH)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 */
public class SES_AI_T_MATCHLot extends EntityLotBase<SES_AI_T_MATCH> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT matching_id, user_id, job_id, person_id, job_content, person_content, status_cd, register_date, register_user FROM SES_AI_T_MATCH";

  public SES_AI_T_MATCHLot() {
    super();
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

  /**
   * 引数の案件IDを持つレコードが存在するかどうかを判定する.
   *
   * @param jobId 案件ID
   * @return 存在すればtrue、それ以外はfalse
   */
  public boolean isExistByJobId(final String jobId) {
    return jobId != null && this.entityLot.stream().anyMatch(e -> jobId.equals(e.getJobId()));
  }

  /**
   * 引数の要員IDを持つレコードが存在するかどうかを判定する.
   *
   * @param personId 要員ID
   * @return 存在すればtrue、それ以外はfalse
   */
  public boolean isExistByPersonId(final String personId) {
    return personId != null
        && this.entityLot.stream().anyMatch(e -> personId.equals(e.getPersonId()));
  }

  @Override
  protected SES_AI_T_MATCH mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_MATCH sesAiTMatch = new SES_AI_T_MATCH();
    sesAiTMatch.setMatchingId(resultSet.getString("matching_id"));
    sesAiTMatch.setUserId(resultSet.getString("user_id"));
    sesAiTMatch.setJobId(resultSet.getString("job_id"));
    sesAiTMatch.setPersonId(resultSet.getString("person_id"));
    sesAiTMatch.setJobContent(resultSet.getString("job_content"));
    sesAiTMatch.setPersonContent(resultSet.getString("person_content"));
    sesAiTMatch.setStatus(MatchingStatus.getEnum(resultSet.getString("status_cd")));
    sesAiTMatch.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiTMatch.setRegisterUser(resultSet.getString("register_user"));
    return sesAiTMatch;
  }
}
