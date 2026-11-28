package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.SES_AI_T_WATCH.TargetType;
import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【Entityクラス】 ウォッチ管理テーブル(SES_AI_T_WATCH)のLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_WATCHLot extends EntityLotBase<SES_AI_T_WATCH> {

  /**
   * ウォッチ中の対象IDを求めるSQL前半（テナント条件は末尾に付く。対象IDのプレースホルダの後ろに閉じ括弧を付けて使う）.
   *
   * <p>ウォッチ中は、ttl が現在時刻より後であること。ttl が NULL のウォッチと、ttl が現在時刻ちょうどのウォッチは含めない。 TTL 削除の {@code ttl <
   * NOW()} と同じ DB の時刻で判定し、削除と判定が食い違わないようにする。
   */
  private static final String SELECT_WATCHED_TARGET_IDS_SQL_PREFIX =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id "
          + "FROM SES_AI_T_WATCH WHERE target_type = ? AND ttl IS NOT NULL AND ttl > NOW() "
          + "AND target_id IN (";

  /**
   * 1回の問い合わせで IN 句に渡す対象IDの件数の上限.
   *
   * <p>IN 句は対象IDの件数だけプレースホルダを使う。件数が多いと JDBC のバインド数の上限を超えるため、この件数ごとに問い合わせを分ける。
   */
  private static final int WATCHED_CHECK_CHUNK_SIZE = 500;

  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id "
          + "FROM SES_AI_T_WATCH";

  /** SELECT文(ユーザーIDキー). */
  private static final String SELECT_BY_USER_ID_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id "
          + "FROM SES_AI_T_WATCH WHERE user_id = ?";

  /** SELECT文(対象IDキー). */
  private static final String SELECT_BY_TARGET_ID_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id "
          + "FROM SES_AI_T_WATCH WHERE target_id = ? AND target_type = ?";

  /** DELETE期限切れSQL（テナントIDなし、バッチ用）. */
  private static final String DELETE_EXPIRED_WITHOUT_TENANT_SQL =
      "DELETE FROM SES_AI_T_WATCH WHERE ttl < NOW()";

  public SES_AI_T_WATCHLot() {
    super();
  }

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  /**
   * ユーザーIDが一致するレコードを全て取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param userId ユーザーID
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void selectByUserId(
      final Connection connection, final String tenantId, final String userId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_WATCH> results =
        executeQuery(
            connection,
            SELECT_BY_USER_ID_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              stmt.setString(paramIndex, userId);
              return paramIndex + 1;
            });
    this.entityLot.addAll(results);
  }

  /**
   * ユーザーIDが一致するレコードを指定件数取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param userId ユーザーID
   * @param page ページ番号
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void selectByUserIdPaged(
      final Connection connection,
      final String tenantId,
      final String userId,
      final int page,
      final int size)
      throws SQLException {
    requireConnection(connection);
    if (userId == null) {
      return;
    }
    Map<String, String> query = new HashMap<>();
    query.put("user_id", userId);
    this.selectByQueryPaged(connection, tenantId, SELECT_ALL_SQL, query, true, page, size);
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

  /**
   * 対象IDと対象種別が一致するレコードを全て取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param targetId 対象ID
   * @param targetType 対象種別
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void selectByTargetId(
      final Connection connection,
      final String tenantId,
      final String targetId,
      final TargetType targetType)
      throws SQLException {
    requireConnection(connection);
    if (targetId == null || targetType == null) {
      this.entityLot = new ArrayList<>();
      return;
    }
    List<SES_AI_T_WATCH> results =
        executeQuery(
            connection,
            SELECT_BY_TARGET_ID_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              stmt.setString(paramIndex, targetId);
              stmt.setString(paramIndex + 1, targetType.name());
              return paramIndex + 2;
            });
    this.entityLot = results;
  }

  /**
   * 指定テナントで、対象種別と一致し、ウォッチ中の対象IDの集合を返します.
   *
   * <p>ウォッチ中は、同じテナントと対象IDのウォッチのうち ttl が現在時刻より後のものが1件でもあること。複数ユーザーのうち1人でも該当すればウォッチ中とする。 他テナントのウォッチは
   * tenant_id の条件で読まない。対象IDの件数が多い場合は {@code WATCHED_CHECK_CHUNK_SIZE} 件ごとに問い合わせを分ける。
   *
   * <p>このメソッドは Lot の保持するレコードを変えない。TTL 削除の対象から外す判定に使う。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param targetType 対象種別（JOB・PERSON・SKILLSHEET）。null の場合は空の集合を返す
   * @param targetIds 判定する対象IDの一覧。null または空の場合は空の集合を返す
   * @return ウォッチ中の対象IDの集合。ウォッチ中の対象が無い場合は空の集合
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   * @throws IllegalArgumentException DBコネクションが null の場合、またはテナントIDが null か空文字の場合（対象IDが1件以上ある場合）
   */
  public Set<String> selectWatchedTargetIds(
      final Connection connection,
      final String tenantId,
      final TargetType targetType,
      final Collection<String> targetIds)
      throws SQLException {
    requireConnection(connection);
    Set<String> watchedTargetIds = new HashSet<>();
    if (targetType == null || targetIds == null || targetIds.isEmpty()) {
      return watchedTargetIds;
    }
    List<String> ids = new ArrayList<>(targetIds);
    for (int from = 0; from < ids.size(); from += WATCHED_CHECK_CHUNK_SIZE) {
      List<String> chunk = ids.subList(from, Math.min(from + WATCHED_CHECK_CHUNK_SIZE, ids.size()));
      String sql =
          SELECT_WATCHED_TARGET_IDS_SQL_PREFIX
              + String.join(", ", Collections.nCopies(chunk.size(), "?"))
              + ")";
      List<SES_AI_T_WATCH> results =
          executeQuery(
              connection,
              sql,
              tenantId,
              this::mapResultSet,
              (stmt, paramIndex) -> {
                stmt.setString(paramIndex, targetType.name());
                int next = paramIndex + 1;
                for (String targetId : chunk) {
                  stmt.setString(next, targetId);
                  next++;
                }
                return next;
              });
      for (SES_AI_T_WATCH watch : results) {
        watchedTargetIds.add(watch.getTargetId());
      }
    }
    return watchedTargetIds;
  }

  /**
   * 期限切れウォッチデータを削除します（テナントID条件なし、バッチ用）.
   *
   * <p>⚠️ このメソッドは全テナント対象です。 バッチ処理専用。コードレビュー必須。
   *
   * @param connection DBコネクション
   * @return 削除件数
   * @throws SQLException SQL実行エラー
   */
  public int deleteExpiredWithoutTenantId(final Connection connection) throws SQLException {
    return executeDeleteWithoutTenantFilter(
        connection, DELETE_EXPIRED_WITHOUT_TENANT_SQL, (stmt, paramIndex) -> paramIndex);
  }

  @Override
  public void selectAll(final Connection connection, final String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_WATCH> results =
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
  public void selectAllWithoutTenantFilter(final Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_WATCH> results =
        executeQueryWithoutTenantFilter(
            connection, SELECT_ALL_SQL, this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  @Override
  protected SES_AI_T_WATCH mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_WATCH watch = new SES_AI_T_WATCH(resultSet.getString("tenant_id"));
    watch.setUserId(resultSet.getString("user_id"));
    watch.setTargetId(resultSet.getString("target_id"));
    watch.setTargetType(TargetType.getEnumByName(resultSet.getString("target_type")));
    watch.setMemo(resultSet.getString("memo"));
    watch.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    watch.setRegisterUser(resultSet.getString("register_user"));
    watch.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return watch;
  }
}
