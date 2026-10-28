package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.api.gpt.entity.FilteringRules;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 【Entityクラス】 テナント情報マスタ(SES_AI_M_TENANT)テーブル.
 *
 * <p>マッチング強度（必須・尚可）は {@link EvaluateType} の列挙名の文字列で保存する。NotMet と NULL は制限なしとして扱う（{@link
 * #toFilteringRules()}）。
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_M_TENANT extends EntityBase {

  public SES_AI_M_TENANT(String tenantId) {
    super(tenantId);
  }

  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_M_TENANT (tenant_id, tenant_name, tenant_status_cd, match_must_level, "
          + "match_want_level, register_date, register_user) VALUES (?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT tenant_id, tenant_name, tenant_status_cd, match_must_level, match_want_level, "
          + "register_date, register_user FROM SES_AI_M_TENANT WHERE tenant_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_M_TENANT SET tenant_name = ?, tenant_status_cd = ?, match_must_level = ?, "
          + "match_want_level = ? WHERE tenant_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_M_TENANT WHERE tenant_id = ?";

  /** テナント名 / tenant_name */
  @Column(physicalName = "tenant_name", logicalName = "テナント名")
  private String tenantName;

  /** ステータス区分 / tenant_status_cd */
  @Column(physicalName = "tenant_status_cd", logicalName = "ステータス区分")
  private String tenantStatusCd;

  /** マッチング強度（必須）/ match_must_level. 新規の既定値は FullyMet（DDL の DEFAULT と同じ）。null と NotMet は制限なし。 */
  @Column(physicalName = "match_must_level", logicalName = "マッチング強度（必須）")
  private EvaluateType matchMustLevel = EvaluateType.FullyMet;

  /** マッチング強度（尚可）/ match_want_level. 新規の既定値は NotMet（DDL の DEFAULT と同じで制限なし）。null と NotMet は制限なし。 */
  @Column(physicalName = "match_want_level", logicalName = "マッチング強度（尚可）")
  private EvaluateType matchWantLevel = EvaluateType.NotMet;

  /**
   * テナント情報を1件登録する。テナントIDが主キーのため、テナント条件は付けない。
   *
   * @param connection DBコネクション
   * @return 登録件数
   * @throws SQLException DB 操作に失敗した場合
   */
  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsertWithoutTenantFilter(
        connection,
        INSERT_SQL,
        (stmt) -> {
          stmt.setString(1, this.tenantId);
          stmt.setString(2, this.tenantName);
          stmt.setString(3, this.tenantStatusCd);
          stmt.setString(4, this.matchMustLevel == null ? null : this.matchMustLevel.name());
          stmt.setString(5, this.matchWantLevel == null ? null : this.matchWantLevel.name());
          stmt.setTimestamp(6, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(7, this.registerUser);
        },
        "SES_AI_M_TENANT.insert");
  }

  /**
   * テナントIDが一致するテナント情報を読み込む。
   *
   * @param connection DBコネクション
   * @return 該当行があれば true
   * @throws SQLException DB 操作に失敗した場合
   */
  @Override
  public boolean selectByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return false;
    }
    return executeSelectByPkWithoutTenantFilter(
        connection,
        SELECT_SQL,
        (stmt) -> stmt.setString(1, this.tenantId),
        (rs) -> {
          this.tenantId = rs.getString("tenant_id");
          this.tenantName = rs.getString("tenant_name");
          this.tenantStatusCd = rs.getString("tenant_status_cd");
          String mustLevel = rs.getString("match_must_level");
          this.matchMustLevel = mustLevel == null ? null : EvaluateType.valueOf(mustLevel);
          String wantLevel = rs.getString("match_want_level");
          this.matchWantLevel = wantLevel == null ? null : EvaluateType.valueOf(wantLevel);
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
        },
        "SES_AI_M_TENANT.selectByPk");
  }

  /**
   * テナントIDが一致するテナント情報のテナント名、ステータス区分、マッチング強度を更新する。
   *
   * <p>マッチング強度も更新するため、呼び出し側は selectByPk で読み込んだ後に変更すること。未設定のまま呼ぶと初期値で上書きされる。
   *
   * @param connection DBコネクション
   * @return 更新件数が1件以上の場合 true
   * @throws SQLException DB 操作に失敗した場合
   */
  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return false;
    }
    return executeUpdateByPkWithoutTenantFilter(
        connection,
        UPDATE_SQL,
        (stmt) -> {
          stmt.setString(1, this.tenantName);
          stmt.setString(2, this.tenantStatusCd);
          stmt.setString(3, this.matchMustLevel == null ? null : this.matchMustLevel.name());
          stmt.setString(4, this.matchWantLevel == null ? null : this.matchWantLevel.name());
          stmt.setString(5, this.tenantId);
        },
        "SES_AI_M_TENANT.updateByPk");
  }

  /**
   * テナントIDが一致するテナント情報を削除する。
   *
   * @param connection DBコネクション
   * @return 削除件数が1件以上の場合 true
   * @throws SQLException DB 操作に失敗した場合
   */
  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return false;
    }
    return executeDeleteByPkWithoutTenantFilter(
        connection,
        DELETE_SQL,
        (stmt) -> stmt.setString(1, this.tenantId),
        "SES_AI_M_TENANT.deleteByPk");
  }

  /**
   * マッチング強度の設定から、AIのマッチ判定に渡すフィルタリングルールを作成する.
   *
   * <p>FullyMet と PartiallyMet はそのまましきい値にする。NotMet、Unknown、NULL は制限なしとして、しきい値を null にする。
   * その他制約の必須フラグは true を設定する。ただし MatchEvaluateResponseSchema の isMatch(FilteringRules) では参照されず、
   * ChooseBestInfoResponseSchema の getFilteredSortedResults と getBestResult でのみ参照される。
   *
   * @return フィルタリングルール
   */
  public FilteringRules toFilteringRules() {
    EvaluateType must = this.matchMustLevel;
    EvaluateType mustThreshold =
        must == EvaluateType.FullyMet || must == EvaluateType.PartiallyMet ? must : null;
    EvaluateType want = this.matchWantLevel;
    EvaluateType wantThreshold =
        want == EvaluateType.FullyMet || want == EvaluateType.PartiallyMet ? want : null;
    return new FilteringRules(mustThreshold, wantThreshold, true);
  }
}
