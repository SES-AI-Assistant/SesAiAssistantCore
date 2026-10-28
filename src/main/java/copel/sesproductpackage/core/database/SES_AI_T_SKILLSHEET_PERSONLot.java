package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.search.FulltextCondition;
import copel.sesproductpackage.core.search.FulltextConditionsWhereClause;
import copel.sesproductpackage.core.search.PersonDetailFilterCondition;
import copel.sesproductpackage.core.search.WhereClauseAndParams;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【Entityクラス】 スキルシート情報と要員情報を結合したEntity(SES_AI_T_SKILLSHEET_PERSON)のLotクラス.
 *
 * @author
 */
public class SES_AI_T_SKILLSHEET_PERSONLot extends EntityLotBase<SES_AI_T_SKILLSHEET_PERSON> {

  /** SELECT_ALL_SQL - 全件取得. */
  private static final String SELECT_ALL_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.mail_subject, "
          + "p.raw_content, p.content_summary, p.register_date, p.register_user, p.unit_price, "
          + "COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, "
          + "s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, "
          + "p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, "
          + "p.office_availability, p.organization, p.experiences, "
          + "p.ng_requirements, p.other_requirements, p.url, p.tenant_id FROM "
          + "SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON "
          + "s.file_id = p.file_id";

  /** COUNT SQL用プレフィックス. */
  private static final String COUNT_SQL_PREFIX = "SELECT COUNT(*) ";

  /** ページング用 OFFSET サフィックス. */
  private static final String PAGED_SQL_OFFSET_SUFFIX = " OFFSET ?";

  private static final String RETRIEVE_OUTER_JOIN_BY_PERSON_VECTOR_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.mail_subject, "
          + "p.raw_content, p.content_summary, p.register_date, p.register_user, p.unit_price, "
          + "COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, "
          + "s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, "
          + "p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, "
          + "p.office_availability, p.organization, p.experiences, "
          + "p.ng_requirements, p.other_requirements, p.url, p.vector_data <=> "
          + "?::vector AS distance, p.tenant_id "
          + "FROM SES_AI_T_PERSON p LEFT JOIN SES_AI_T_SKILLSHEET s ON p.file_id = s.file_id "
          + "WHERE 1 - (p.vector_data <=> ?::vector) >= ? ORDER BY distance ASC LIMIT ?";

  /**
   * 要員 raw_content と スキルシート file_content_summary の両方を OR 照合する LEFT JOIN 用 SELECT 接頭辞.
   * スキルシート未紐づきの要員も除外しない。
   */
  private static final String SELECT_BY_PERSON_OR_SKILLSHEET_SUMMARY_PREFIX =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.mail_subject, "
          + "p.raw_content, p.content_summary, p.register_date, p.register_user, p.unit_price, "
          + "COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, "
          + "s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, "
          + "p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, "
          + "p.office_availability, p.organization, p.experiences, "
          + "p.ng_requirements, p.other_requirements, p.url, p.tenant_id "
          + "FROM SES_AI_T_PERSON p LEFT JOIN SES_AI_T_SKILLSHEET s "
          + "ON p.file_id = s.file_id WHERE ";

  /** 要員 raw_content 複合条件全文検索用 SELECT 接頭辞（末尾に WHERE を含む）. */
  private static final String SELECT_BY_PERSON_RAW_CONTENT_PREFIX =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.mail_subject, "
          + "p.raw_content, p.content_summary, p.register_date, p.register_user, p.unit_price, "
          + "COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, "
          + "s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, "
          + "p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, "
          + "p.office_availability, p.organization, p.experiences, "
          + "p.ng_requirements, p.other_requirements, p.url, p.tenant_id "
          + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE ";

  /** 要員ベクトル検索用COUNT_QUERY_PART（価格・開始日フィルタ付き）. */
  private static final String COUNT_QUERY_PART_FOR_PERSON_VECTOR_WITH_FILTER_2_VALUES =
      "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id WHERE "
          + "p.unit_price <= ? AND p.start_date >= ? AND 1 - (p.vector_data <=> ?::vector) >= ?";

  /** 要員ベクトル検索SQL（価格・開始日フィルタ付き）. */
  private static final String RETRIEVE_BY_PERSON_VECTOR_WITH_FILTER_2_VALUES_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.mail_subject, "
          + "p.raw_content, p.content_summary, p.register_date, p.register_user, p.unit_price, "
          + "COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, "
          + "s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, "
          + "p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, "
          + "p.office_availability, p.organization, p.experiences, "
          + "p.ng_requirements, p.other_requirements, p.url, p.vector_data <=> "
          + "?::vector AS distance, p.tenant_id "
          + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE p.unit_price <= ? AND p.start_date >= ? AND 1 - (p.vector_data <=> ?::vector) "
          + ">= ? ORDER BY distance ASC LIMIT ?";

  /**
   * 要員ベクトル検索用COUNT_QUERY_PART（価格・開始日・オフィス可用性・エリアフィルタ付き）.
   *
   * <p>エリアの条件は {@code (? = 0 OR p.area = ?)} とし、案件の出社要件が 0（フルリモート）の場合はエリアを問わない。
   * フルリモート判定と可用性の比較の両方に出社要件を使うため、同じ値を続けて渡す。
   */
  private static final String COUNT_QUERY_PART_FOR_PERSON_VECTOR_WITH_FILTER_4_VALUES =
      "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id WHERE "
          + "p.unit_price <= ? AND p.start_date >= ? AND p.office_availability >= ? AND "
          + "(? = 0 OR p.area = ?) AND 1 - (p.vector_data <=> ?::vector) >= ?";

  /**
   * 要員ベクトル検索SQL（価格・開始日・オフィス可用性・エリアフィルタ付き）.
   *
   * <p>エリアの条件は {@code (? = 0 OR p.area = ?)} とし、案件の出社要件が 0（フルリモート）の場合はエリアを問わない。
   * フルリモート判定と可用性の比較の両方に出社要件を使うため、同じ値を続けて渡す。
   */
  private static final String RETRIEVE_BY_PERSON_VECTOR_WITH_FILTER_4_VALUES_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.mail_subject, "
          + "p.raw_content, p.content_summary, p.register_date, p.register_user, p.unit_price, "
          + "COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, "
          + "s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, "
          + "p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, "
          + "p.office_availability, p.organization, p.experiences, "
          + "p.ng_requirements, p.other_requirements, p.url, p.vector_data <=> "
          + "?::vector AS distance, p.tenant_id "
          + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE p.unit_price <= ? AND p.start_date >= ? AND p.office_availability >= ? AND "
          + "(? = 0 OR p.area = ?) AND 1 - (p.vector_data <=> ?::vector) >= ? ORDER BY distance "
          + "ASC LIMIT ?";

  /** コンストラクタ. */
  public SES_AI_T_SKILLSHEET_PERSONLot() {
    super();
  }

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  /**
   * 要員のベクトルデータに対してセマンティック検索を実行し、結果をこのLotに保持します(OUTER JOIN).
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrieveOuterJoinByPersonVector(
      final Connection connection,
      final String tenantId,
      final Vector query,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    executeRetrieve(
        connection,
        tenantId,
        RETRIEVE_OUTER_JOIN_BY_PERSON_VECTOR_SQL,
        query,
        similarityThreshold,
        limit);
  }

  /**
   * 要員のベクトルデータに対して価格と開始日でフィルタされたセマンティック検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param price 最大価格（単価がこの値以下のもの）
   * @param startDate 最早開始日（開始日がこの日付以降のもの）
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrieveByPersonVectorWithFilter(
      final Connection connection,
      final String tenantId,
      final Vector query,
      final Money price,
      final OriginalDateTime startDate,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    this.executeRetrieveWithFilterPaged(
        connection,
        tenantId,
        RETRIEVE_BY_PERSON_VECTOR_WITH_FILTER_2_VALUES_SQL,
        query,
        price,
        startDate,
        null,
        null,
        similarityThreshold,
        1,
        limit,
        COUNT_QUERY_PART_FOR_PERSON_VECTOR_WITH_FILTER_2_VALUES);
  }

  /**
   * 要員のベクトルデータに対して価格・開始日・オフィス可用性・エリアでフィルタされたセマンティック検索を実行し、結果をこのLotに保持します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param price 最大価格（単価がこの値以下のもの）
   * @param startDate 最早開始日（開始日がこの日付以降のもの）
   * @param officeRequirements 案件の出社要件。最小オフィス可用性（オフィス可用性がこの値以上のもの）としても使う
   * @param area 案件のエリア。出社要件が 0（フルリモート）の場合はエリアを問わない。1以上の場合はエリアがこの値と一致するもの。null
   *     の場合は、1以上の出社要件では候補が0件になる
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param limit 取得上限件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrieveByPersonVectorWithFilter(
      final Connection connection,
      final String tenantId,
      final Vector query,
      final Money price,
      final OriginalDateTime startDate,
      final int officeRequirements,
      final Area area,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    this.executeRetrieveWithFilterPaged(
        connection,
        tenantId,
        RETRIEVE_BY_PERSON_VECTOR_WITH_FILTER_4_VALUES_SQL,
        query,
        price,
        startDate,
        officeRequirements,
        area,
        similarityThreshold,
        1,
        limit,
        COUNT_QUERY_PART_FOR_PERSON_VECTOR_WITH_FILTER_4_VALUES);
  }

  /**
   * 出社要件とエリアの条件のパラメータを、指定の位置から3つバインドする.
   *
   * <p>SQL の順は {@code p.office_availability >= ?}、{@code (? = 0 OR p.area = ?)}
   * のため、出社要件、出社要件、エリアの順にバインドする。 エリアが null の場合は SQL の NULL をバインドする。そのため出社要件が 1 以上の検索では一致する要員が無い。
   *
   * @param stmt 準備文
   * @param idx 出社要件をバインドする最初の位置
   * @param officeRequirements 案件の出社要件
   * @param area 案件のエリア（null 可）
   * @return 次にバインドする位置
   * @throws SQLException バインドに失敗した場合
   */
  private static int bindOfficeRequirementsAndArea(
      final PreparedStatement stmt, final int idx, final int officeRequirements, final Area area)
      throws SQLException {
    stmt.setInt(idx, officeRequirements);
    stmt.setInt(idx + 1, officeRequirements);
    stmt.setString(idx + 2, area == null ? null : area.name());
    return idx + 3;
  }

  private void executeRetrieve(
      final Connection connection,
      final String tenantId,
      final String sql,
      final Vector query,
      final double similarityThreshold,
      final int limit)
      throws SQLException {
    executeRetrievePaged(connection, tenantId, sql, query, similarityThreshold, 1, limit);
  }

  /**
   * ベクトル検索を指定のページで実行し、このLotに保持します.
   *
   * <p>件数は取得せず、ページサイズを件数として保持します。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param sql ベクトル検索SQL
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  private void executeRetrievePaged(
      final Connection connection,
      final String tenantId,
      final String sql,
      final Vector query,
      final double similarityThreshold,
      final int page,
      final int size)
      throws SQLException {
    requireConnection(connection);
    if (query == null) {
      return;
    }
    this.totalCount = size;
    this.pageSize = size;
    this.currentPageIndex = page;

    String pagedSql = sql + PAGED_SQL_OFFSET_SUFFIX;
    List<SES_AI_T_SKILLSHEET_PERSON> results =
        executeQuery(
            connection,
            pagedSql,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              int idx = paramIndex;
              final String vectorStr = query.toString();
              stmt.setString(idx, vectorStr);
              stmt.setString(idx + 1, vectorStr);
              stmt.setDouble(idx + 2, similarityThreshold);
              stmt.setInt(idx + 4, size);
              stmt.setInt(idx + 5, (page - 1) * size);
              return idx + 3;
            });
    this.entityLot = results;
  }

  private void executeRetrieveWithFilterPaged(
      final Connection connection,
      final String tenantId,
      final String sql,
      final Vector query,
      final Money price,
      final OriginalDateTime startDate,
      final Integer officeRequirements,
      final Area area,
      final double similarityThreshold,
      final int page,
      final int size,
      final String countQueryPart)
      throws SQLException {
    requireConnection(connection);
    if (query == null) {
      return;
    }

    // (1) 全件数を取得（tenant_id フィルターを適用）
    executeCountQuery(
        connection,
        COUNT_SQL_PREFIX + countQueryPart,
        tenantId,
        (stmt, paramIndex) -> {
          int idx = paramIndex;
          stmt.setBigDecimal(idx++, price.getValue());
          stmt.setTimestamp(idx++, startDate.toTimestamp());
          if (officeRequirements != null) {
            idx = bindOfficeRequirementsAndArea(stmt, idx, officeRequirements, area);
          }
          stmt.setString(idx++, query.toString());
          stmt.setDouble(idx++, similarityThreshold);
          return idx;
        });

    this.pageSize = size;
    this.currentPageIndex = page;

    if (this.totalCount == 0) {
      return;
    }

    // (2) ページング実行（tenant_id フィルターを適用）
    String pagedSql = sql + PAGED_SQL_OFFSET_SUFFIX;
    List<SES_AI_T_SKILLSHEET_PERSON> results =
        executeQuery(
            connection,
            pagedSql,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              int idx = paramIndex;
              final String vectorStr = query.toString();
              // SQL の SELECT 句の先頭に距離の計算（?::vector AS distance）があるため、ベクトルが先頭と、類似度の条件の2か所に要る。
              // 先頭を飛ばすと、以降の値が1つずつずれて「numeric から vector にキャストできない」エラーになる
              stmt.setString(idx, vectorStr);
              stmt.setBigDecimal(idx + 1, price.getValue());
              stmt.setTimestamp(idx + 2, startDate.toTimestamp());
              if (officeRequirements != null) {
                // 出社要件とエリアは検索条件の途中にあり、その後ろに類似度の条件が続く。
                // tenant_id は SQL への挿入位置に合わせて戻り値で受け、LIMIT と OFFSET はその後ろに置く
                int next = bindOfficeRequirementsAndArea(stmt, idx + 3, officeRequirements, area);
                stmt.setString(next, vectorStr);
                stmt.setDouble(next + 1, similarityThreshold);
                stmt.setInt(next + 3, size);
                stmt.setInt(next + 4, (page - 1) * size);
                return next + 2;
              } else {
                stmt.setString(idx + 3, vectorStr);
                stmt.setDouble(idx + 4, similarityThreshold);
                stmt.setInt(idx + 6, size);
                stmt.setInt(idx + 7, (page - 1) * size);
                return idx + 5;
              }
            });
    this.entityLot = results;
  }

  private void executeRetrieveWithFilterPaged(
      final Connection connection,
      final String tenantId,
      final String sql,
      final Vector query,
      final List<Object> filterParams,
      final double similarityThreshold,
      final int page,
      final int size,
      final String countQueryPart)
      throws SQLException {
    this.executeVectorWithFilterPaged(
        connection,
        tenantId,
        sql + PAGED_SQL_OFFSET_SUFFIX,
        COUNT_SQL_PREFIX + countQueryPart,
        query,
        filterParams,
        similarityThreshold,
        page,
        size);
  }

  /**
   * 要員の raw_content とスキルシートの file_content_summary の両方に対して複合条件でページング検索します（LEFT JOIN）.
   *
   * <p>スキルシート未紐づきの要員は raw_content のみが照合対象となり、結果から除外されない。 スキルシート原文（file_content）は検索対象外。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param conditions 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByPersonOrSkillSheetSummaryPaged(
      final Connection connection,
      final String tenantId,
      final List<FulltextCondition> conditions,
      final int page,
      final int size)
      throws SQLException {
    FulltextConditionsWhereClause.Built built =
        FulltextConditionsWhereClause.buildForMultipleColumns(
            "p.raw_content", List.of("s.file_content_summary"), conditions);
    this.selectByDynamicWherePaged(
        connection,
        tenantId,
        SELECT_BY_PERSON_OR_SKILLSHEET_SUMMARY_PREFIX,
        built.getWhereClauseWithoutWhereKeyword(),
        built.getLikeParams(),
        page,
        size);
  }

  /**
   * 要員の raw_content とスキルシートの file_content_summary の両方に対して複合条件でページング検索します（INNER JOIN）.
   *
   * <p>スキルシートが紐づいている要員のみが対象（has_skillsheet_only: true）。 file_content_summary が NULL の行も NOT
   * 条件で誤除外されないよう IS NULL チェックを行う。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param conditions 検索条件リスト
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByPersonOrSkillSheetSummaryInnerJoinPaged(
      final Connection connection,
      final String tenantId,
      final List<FulltextCondition> conditions,
      final int page,
      final int size)
      throws SQLException {
    FulltextConditionsWhereClause.Built built =
        FulltextConditionsWhereClause.buildForMultipleColumns(
            "p.raw_content", List.of("s.file_content_summary"), conditions);
    this.selectByDynamicWherePaged(
        connection,
        tenantId,
        SELECT_BY_PERSON_RAW_CONTENT_PREFIX,
        built.getWhereClauseWithoutWhereKeyword(),
        built.getLikeParams(),
        page,
        size);
  }

  /**
   * 指定した要員IDのEntityを取得する.
   *
   * @param personId 要員ID
   * @return 指定した要員IDのEntity
   */
  public SES_AI_T_SKILLSHEET_PERSON getEntityByPk(final String personId) {
    if (personId == null) {
      return null;
    }
    for (SES_AI_T_SKILLSHEET_PERSON entity : this.entityLot) {
      if (personId.trim().equals(entity.getPersonId().trim())) {
        return entity;
      }
    }
    return null;
  }

  /**
   * LLMに最もマッチする要員の要員IDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String to要員選出用文章() {
    StringBuilder result = new StringBuilder();
    int i = 1;
    for (SES_AI_T_SKILLSHEET_PERSON entity : this.entityLot) {
      result.append(i).append("人目：").append(entity.to要員選出用文章()).append("\n");
      i++;
    }
    return result.toString();
  }

  /**
   * LLMに最もマッチするスキルシートのファイルIDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String toSkillSheetSelectionText() {
    StringBuilder result = new StringBuilder();
    int i = 1;
    for (SES_AI_T_SKILLSHEET_PERSON entity : this.entityLot) {
      result.append(i).append("件目：").append(entity.toSkillSheetSelectionText()).append("\n");
      i++;
    }
    return result.toString();
  }

  @Override
  public void selectAll(final Connection connection, final String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_SKILLSHEET_PERSON> results =
        executeQuery(
            connection,
            getSelectAllSql(),
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * 要員・スキルシート複合全文検索を詳細フィルター条件付きでページング実行し、結果をこのLotに保持します（LEFT JOIN）.
   *
   * <p>FulltextConditionsWhereClauseで全文検索条件を構築し、PersonDetailFilterConditionで詳細フィルター条件を構築します。
   * 両方のWHERE句とパラメータをマージして実行します。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param conditions 検索条件リスト
   * @param detailFilter 詳細フィルター条件（null可）
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void searchByPersonOrSkillSheetSummaryWithDetailFilter(
      final Connection connection,
      final String tenantId,
      final List<FulltextCondition> conditions,
      final PersonDetailFilterCondition detailFilter,
      final int page,
      final int size)
      throws SQLException {
    requireConnection(connection);
    if (conditions == null || conditions.isEmpty()) {
      return;
    }

    // (1) 全文検索のWHERE句とパラメータを構築
    FulltextConditionsWhereClause.Built ftBuilt =
        FulltextConditionsWhereClause.buildForMultipleColumns(
            "p.raw_content", List.of("s.file_content_summary"), conditions);

    // (2) 詳細フィルターのWHERE句とパラメータを構築
    WhereClauseAndParams detailFilterWhere =
        (detailFilter != null)
            ? detailFilter.buildWhereClause("p")
            : new PersonDetailFilterCondition().buildWhereClause("p");

    // (3) WHERE句をマージ
    StringBuilder mergedWhere = new StringBuilder();
    mergedWhere.append(ftBuilt.getWhereClauseWithoutWhereKeyword());
    if (!detailFilterWhere.isEmpty()) {
      mergedWhere.append(" AND ");
      mergedWhere
          .append('(')
          .append(detailFilterWhere.getWhereClauseWithoutWhereKeyword())
          .append(')');
    }

    // (4) selectByDynamicWhereWithFilterPagedを呼び出してページング検索を実行
    // 全文検索パラメータと詳細フィルターパラメータをマージしてLotに保持
    this.selectByDynamicWhereWithFilterPaged(
        connection,
        tenantId,
        SELECT_BY_PERSON_OR_SKILLSHEET_SUMMARY_PREFIX,
        mergedWhere.toString(),
        ftBuilt.getLikeParams(),
        detailFilterWhere.getParams(),
        page,
        size);
  }

  /**
   * 要員のベクトルデータに対してセマンティック検索を詳細フィルター条件付きでページング実行し、結果をこのLotに保持します.
   *
   * <p>ベクトル検索にPersonDetailFilterConditionで構築された詳細フィルター条件を追加して実行します。
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param query 検索ベクトル
   * @param similarityThreshold 類似度閾値 (0.0 ~ 1.0)
   * @param detailFilter 詳細フィルター条件（null可）
   * @param page ページ番号(1-based)
   * @param size 1ページあたりの件数
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void retrieveByPersonVectorWithDetailFilter(
      final Connection connection,
      final String tenantId,
      final Vector query,
      final double similarityThreshold,
      final PersonDetailFilterCondition detailFilter,
      final int page,
      final int size)
      throws SQLException {
    requireConnection(connection);
    if (query == null) {
      return;
    }

    // (1) 詳細フィルターのWHERE句とパラメータを構築
    WhereClauseAndParams detailFilterWhere =
        (detailFilter != null)
            ? detailFilter.buildWhereClause("p")
            : new PersonDetailFilterCondition().buildWhereClause("p");

    // (2) ベクトル検索のWHERE句を構築（詳細フィルターを追加）
    StringBuilder whereClause = new StringBuilder();
    whereClause.append("1 - (p.vector_data <=> ?::vector) >= ?");
    if (!detailFilterWhere.isEmpty()) {
      whereClause
          .append(" AND (")
          .append(detailFilterWhere.getWhereClauseWithoutWhereKeyword())
          .append(')');
    }

    // (3) ベクトル検索SQLを構築
    String sql =
        "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.mail_subject, "
            + "p.raw_content, p.content_summary, p.register_date, p.register_user, p.unit_price, "
            + "COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, "
            + "s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, "
            + "p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, "
            + "p.office_availability, p.organization, p.experiences, "
            + "p.ng_requirements, p.other_requirements, p.url, p.vector_data <=> "
            + "?::vector AS distance, p.tenant_id "
            + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
            + "WHERE "
            + whereClause.toString()
            + " ORDER BY distance ASC LIMIT ?";

    // (4) COUNT SQL用の部分SQL（ベクトル距離計算とフィルター条件を含む）
    String countQueryPart =
        "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
            + "WHERE "
            + whereClause.toString();

    // (5) executeRetrieveWithFilterPagedを呼び出してページング検索を実行
    // 詳細フィルターパラメータをメソッドに渡す
    this.executeRetrieveWithFilterPaged(
        connection,
        tenantId,
        sql,
        query,
        detailFilterWhere.getParams(),
        similarityThreshold,
        page,
        size,
        countQueryPart);
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
    List<SES_AI_T_SKILLSHEET_PERSON> results =
        executeQueryWithoutTenantFilter(
            connection, getSelectAllSql(), this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  @Override
  protected SES_AI_T_SKILLSHEET_PERSON mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_T_SKILLSHEET_PERSON entity =
        new SES_AI_T_SKILLSHEET_PERSON(resultSet.getString("tenant_id"));
    entity.setEntityDataFromResultSet(resultSet);
    if (hasColumn(resultSet, "distance")) {
      entity.setDistance(resultSet.getDouble("distance"));
    }
    return entity;
  }

  /**
   * 件数の問い合わせを実行し、総件数を保持します.
   *
   * <p>件数の SQL には tenant_id の条件を付けます。binder が WHERE 句のパラメータを順にバインドし、続けて tenant_id をバインドします。
   *
   * @param connection DBコネクション
   * @param countSql 件数の SQL（tenant_id の条件は付けない）
   * @param tenantId テナントID
   * @param binder WHERE 句のパラメータをバインドする処理（次のパラメータ位置を返す）
   * @throws SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  private void executeCountQuery(
      final Connection connection,
      final String countSql,
      final String tenantId,
      final PreparedStatementBinder binder)
      throws SQLException {
    try (PreparedStatement preparedStatement =
        connection.prepareStatement(addTenantIdFilter(countSql, tenantId))) {
      int paramIndex = binder.bind(preparedStatement, 1);
      setTenantIdParameter(preparedStatement, paramIndex, tenantId);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          this.totalCount = resultSet.getLong(1);
        }
      }
    }
  }

  private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
    java.sql.ResultSetMetaData rsmd = rs.getMetaData();
    int columns = rsmd.getColumnCount();
    for (int x = 1; x <= columns; x++) {
      if (columnName.equalsIgnoreCase(rsmd.getColumnLabel(x))) {
        return true;
      }
    }
    return false;
  }
}
