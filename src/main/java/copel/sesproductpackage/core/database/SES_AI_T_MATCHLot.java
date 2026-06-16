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
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_MATCHLot extends EntityLotBase<SES_AI_T_MATCH> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT matching_id, user_id, job_id, person_id, job_content, person_content, status_cd, evaluation_text, register_date, register_user, tenant_id FROM SES_AI_T_MATCH";


  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  protected String getSelectSql() {
    return "SELECT matching_id, user_id, job_id, person_id, job_content, person_content, status_cd, evaluation_text, register_date, register_user FROM SES_AI_T_MATCH WHERE ";
  }

  @Override
  public void selectAll(Connection connection, String tenantId) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  /**
   * tenantId指定なしで全レコードを取得する（Batch削除用）.
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public void selectAllWithoutTenantId(Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    if (connection == null) {
      return;
    }
    try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      while (resultSet.next()) {
        this.entityLot.add(mapResultSet(resultSet));
      }
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

  /**
   * 案件IDが一致するレコードを指定件数取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param jobId 案件ID
   * @param page ページ番号
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void selectByJobIdPaged(Connection connection, String tenantId, String jobId, int page, int size)
      throws SQLException {
    if (connection == null || jobId == null) {
      return;
    }
    java.util.Map<String, String> query = new java.util.HashMap<>();
    query.put("job_id", jobId);
    this.selectByQueryPaged(connection, tenantId, SELECT_ALL_SQL, query, true, page, size);
  }

  /**
   * 要員IDが一致するレコードを指定件数取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param personId 要員ID
   * @param page ページ番号
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void selectByPersonIdPaged(Connection connection, String tenantId, String personId, int page, int size)
      throws SQLException {
    if (connection == null || personId == null) {
      return;
    }
    java.util.Map<String, String> query = new java.util.HashMap<>();
    query.put("person_id", personId);
    this.selectByQueryPaged(connection, tenantId, SELECT_ALL_SQL, query, true, page, size);
  }

  @Override
  protected SES_AI_T_MATCH mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_T_MATCH sesAiTMatch = new SES_AI_T_MATCH(tenantId);
    sesAiTMatch.setMatchingId(resultSet.getString("matching_id"));
    sesAiTMatch.setUserId(resultSet.getString("user_id"));
    sesAiTMatch.setJobId(resultSet.getString("job_id"));
    sesAiTMatch.setPersonId(resultSet.getString("person_id"));
    sesAiTMatch.setJobContent(resultSet.getString("job_content"));
    sesAiTMatch.setPersonContent(resultSet.getString("person_content"));
    sesAiTMatch.setStatus(MatchingStatus.getEnum(resultSet.getString("status_cd")));
    sesAiTMatch.setEvaluationText(resultSet.getString("evaluation_text"));
    sesAiTMatch.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiTMatch.setRegisterUser(resultSet.getString("register_user"));
    return sesAiTMatch;
  }
}
