package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【Entityクラス】 マッチング(SES_AI_T_MATCH)テーブルのLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_MATCHLot extends EntityLotBase<SES_AI_T_MATCH> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT matching_id, user_id, job_id, person_id, job_content, person_content, status_cd, "
          + "evaluation_text, score, must_evaluation_text, want_evaluation_text, "
          + "other_evaluation_text, profit, register_date, register_user, tenant_id "
          + "FROM SES_AI_T_MATCH ORDER BY "
          + "register_date DESC";

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  public void selectAll(Connection connection, String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_MATCH> results =
        executeQuery(
            connection,
            SELECT_ALL_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * 全レコードを取得する（WithoutTenantFilter - バッチ処理専用）.
   *
   * <p>⚠️ このメソッドは全テナント対象です。 バッチ処理専用。コードレビュー必須。
   *
   * @param connection DBコネクション
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void selectAllWithoutTenantId(Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_MATCH> results =
        executeQueryWithoutTenantFilter(
            connection, SELECT_ALL_SQL, this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
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

  @Override
  protected SES_AI_T_MATCH mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_MATCH match = new SES_AI_T_MATCH(resultSet.getString("tenant_id"));
    match.setMatchingId(resultSet.getString("matching_id"));
    match.setUserId(resultSet.getString("user_id"));
    match.setJobId(resultSet.getString("job_id"));
    match.setPersonId(resultSet.getString("person_id"));
    match.setJobContent(resultSet.getString("job_content"));
    match.setPersonContent(resultSet.getString("person_content"));
    match.setStatus(MatchingStatus.getEnum(resultSet.getString("status_cd")));
    match.setEvaluationText(resultSet.getString("evaluation_text"));
    match.setScore(resultSet.getObject("score") != null ? resultSet.getInt("score") : null);
    match.setMustEvaluationText(resultSet.getString("must_evaluation_text"));
    match.setWantEvaluationText(resultSet.getString("want_evaluation_text"));
    match.setOtherEvaluationText(resultSet.getString("other_evaluation_text"));
    BigDecimal profitVal = resultSet.getBigDecimal("profit");
    match.setProfit(profitVal == null ? Money.empty() : new Money(profitVal));
    match.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    match.setRegisterUser(resultSet.getString("register_user"));
    return match;
  }
}
