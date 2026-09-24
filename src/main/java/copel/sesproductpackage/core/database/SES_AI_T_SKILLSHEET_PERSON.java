package copel.sesproductpackage.core.database;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import copel.sesproductpackage.core.unit.Money;
import lombok.Data;

/**
 * 【Entityクラス】 スキルシート情報と要員情報を結合したEntityクラス. スキルシートの要約と要員の要約を保持する.
 *
 * @author Copel Co., Ltd.
 */
@Data
@lombok.EqualsAndHashCode(callSuper = false)
public class SES_AI_T_SKILLSHEET_PERSON extends SES_AI_T_EntityBase {

  public SES_AI_T_SKILLSHEET_PERSON(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }

  /** ファイルID / file_id */
  @Column(physicalName = "file_id", logicalName = "ファイルID")
  private String fileId;

  /** スキルシートの要約 / file_content_summary */
  @Column(physicalName = "file_summary", logicalName = "スキルシートの要約")
  private String fileContentSummary;

  /** ファイル名 / file_name */
  @Column(physicalName = "file_name", logicalName = "ファイル名")
  private String fileName;

  /** 要員ID / person_id */
  @Column(physicalName = "person_id", logicalName = "要員ID")
  private String personId;

  /** 原文 / raw_content */
  @Column(physicalName = "raw_content", logicalName = "原文")
  private String rawContent;

  /** 要約 / content_summary */
  @Column(physicalName = "content_summary", logicalName = "要約")
  private String contentSummary;

  /** 単価 / unit_price */
  @Column(physicalName = "unit_price", logicalName = "単価")
  private Money unitPrice;

  /** 氏名 / name */
  @Column(physicalName = "name", logicalName = "氏名")
  private String name;

  /** 年齢 / age */
  @Column(physicalName = "age", logicalName = "年齢")
  private Integer age;

  /** 性別 / gender */
  @Column(physicalName = "gender", logicalName = "性別")
  private copel.sesproductpackage.core.unit.Gender gender;

  /** 国籍 / nationality */
  @Column(physicalName = "nationality", logicalName = "国籍")
  private String nationality;

  /** 開始日 / start_date */
  @Column(physicalName = "start_date", logicalName = "開始日")
  private copel.sesproductpackage.core.unit.OriginalDateTime startDate;

  /** 場所 / place */
  @Column(physicalName = "place", logicalName = "場所")
  private String place;

  /** 地域 / area */
  @Column(physicalName = "area", logicalName = "地域")
  private copel.sesproductpackage.core.unit.Area area;

  /** 出社可能日数 / office_availability */
  @Column(physicalName = "office_availability", logicalName = "出社可能日数")
  private Integer officeAvailability;

  /** 所属 / organization */
  @Column(physicalName = "organization", logicalName = "所属")
  private String organization;

  /** 経歴 / experiences */
  @Column(physicalName = "experiences", logicalName = "経歴")
  private String experiences;

  /** NG条件 / ng_requirements */
  @Column(physicalName = "ng_requirements", logicalName = "NG条件")
  private String ngRequirements;

  /** その他条件 / other_requirements */
  @Column(physicalName = "other_requirements", logicalName = "その他条件")
  private String otherRequirements;

  /** URL / url */
  @Column(physicalName = "url", logicalName = "URL")
  private String url;

  // ================================
  // Overrideメソッド (EntityBaseの抽象メソッド実装)
  // ================================

  // このEntityはJOIN用のため、単体でのINSERT/UPDATE/DELETE/SELECT等はサポートしない想定だが、
  // EntityBaseを継承しているためダミー実装または例外スローが必要であれば実装する。
  // 今回は使用しないため空実装またはfalseを返す。

  @Override
  public void embedding(copel.sesproductpackage.core.api.gpt.Transformer embeddingProcessListener)
      throws java.io.IOException, RuntimeException {
    // 何もしない
  }

  @Override
  public boolean uniqueCheck(java.sql.Connection connection, double similarityThreshold)
      throws java.sql.SQLException {
    return false;
  }

  @Override
  public int insert(java.sql.Connection connection) throws java.sql.SQLException {
    return 0;
  }

  @Override
  public void selectByPk(java.sql.Connection connection) throws java.sql.SQLException {
    // 何もしない
  }

  @Override
  public boolean updateByPk(java.sql.Connection connection) throws java.sql.SQLException {
    return false;
  }

  @Override
  public boolean deleteByPk(java.sql.Connection connection) throws java.sql.SQLException {
    return false;
  }

  private static final String SELECT_BY_PERSON_ID_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.unit_price, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, p.office_availability, p.organization, p.experiences, p.ng_requirements, p.other_requirements, p.url "
          + "FROM SES_AI_T_PERSON p INNER JOIN SES_AI_T_SKILLSHEET s ON p.file_id = s.file_id "
          + "WHERE p.person_id = ?";

  private static final String SELECT_BY_FILE_ID_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.unit_price, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, p.office_availability, p.organization, p.experiences, p.ng_requirements, p.other_requirements, p.url "
          + "FROM SES_AI_T_PERSON p INNER JOIN SES_AI_T_SKILLSHEET s ON p.file_id = s.file_id "
          + "WHERE s.file_id = ?";

  private static final String SELECT_OUTER_JOIN_BY_PERSON_ID_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.unit_price, p.register_date, p.register_user, COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, p.office_availability, p.organization, p.experiences, p.ng_requirements, p.other_requirements, p.url "
          + "FROM SES_AI_T_PERSON p LEFT JOIN SES_AI_T_SKILLSHEET s ON p.file_id = s.file_id "
          + "WHERE p.person_id = ?";

  private static final String SELECT_OUTER_JOIN_BY_FILE_ID_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.raw_content, p.content_summary, p.unit_price, s.register_date, s.register_user, COALESCE(s.from_group, p.from_group) AS from_group, COALESCE(s.from_id, p.from_id) AS from_id, COALESCE(s.from_name, p.from_name) AS from_name, p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, p.office_availability, p.organization, p.experiences, p.ng_requirements, p.other_requirements, p.url "
          + "FROM SES_AI_T_SKILLSHEET s LEFT JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id "
          + "WHERE s.file_id = ?";

  // ================================
  // メソッド
  // ================================

  /**
   * LLMに最もマッチする要員の要員IDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String to要員選出用文章() {
    String summary = this.contentSummary != null ? this.contentSummary : "";
    String fileSummary = this.fileContentSummary != null ? this.fileContentSummary : "";
    return "要員ID：" + this.personId + " 内容：" + summary + fileSummary;
  }

  /**
   * LLMに最もマッチするスキルシートのファイルIDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String toスキルシート選出用文章() {
    String summary = this.contentSummary != null ? this.contentSummary : "";
    String fileSummary = this.fileContentSummary != null ? this.fileContentSummary : "";
    return "ファイルID：" + this.fileId + " 内容：" + fileSummary + summary;
  }

  /**
   * person_idをキーにしてJOIN検索し、取得結果をEntityにセットする.
   *
   * @param connection DBコネクション
   * @param personId 要員ID
   * @throws java.sql.SQLException
   */
  public void selectByPersonId(java.sql.Connection connection, String personId)
      throws java.sql.SQLException {
    executeSelectByPk(
        connection,
        SELECT_BY_PERSON_ID_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, personId),
        (rs) -> setEntityDataFromResultSet(rs),
        "SES_AI_T_SKILLSHEET_PERSON.selectByPersonId");
  }

  /**
   * person_idをキーにしてOUTER JOIN検索し、取得結果をEntityにセットする. 要員とスキルシートが両方揃っていれば両方の情報を、片方なら片方の情報を返却する.
   *
   * @param connection DBコネクション
   * @param personId 要員ID
   * @throws java.sql.SQLException
   */
  public void selectOuterJoinByPersonId(java.sql.Connection connection, String personId)
      throws java.sql.SQLException {
    executeSelectByPk(
        connection,
        SELECT_OUTER_JOIN_BY_PERSON_ID_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, personId),
        (rs) -> setEntityDataFromResultSet(rs),
        "SES_AI_T_SKILLSHEET_PERSON.selectOuterJoinByPersonId");
  }

  /**
   * file_idをキーにしてJOIN検索し、取得結果をEntityにセットする.
   *
   * @param connection DBコネクション
   * @param fileId ファイルID
   * @throws java.sql.SQLException
   */
  public void selectByFileId(java.sql.Connection connection, String fileId)
      throws java.sql.SQLException {
    executeSelectByPk(
        connection,
        SELECT_BY_FILE_ID_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, fileId),
        (rs) -> setEntityDataFromResultSet(rs),
        "SES_AI_T_SKILLSHEET_PERSON.selectByFileId");
  }

  /**
   * file_idをキーにしてOUTER JOIN検索し、取得結果をEntityにセットする. 要員とスキルシートが両方揃っていれば両方の情報を、片方なら片方の情報を返却する.
   *
   * @param connection DBコネクション
   * @param fileId ファイルID
   * @throws java.sql.SQLException
   */
  public void selectOuterJoinByFileId(java.sql.Connection connection, String fileId)
      throws java.sql.SQLException {
    executeSelectByPk(
        connection,
        SELECT_OUTER_JOIN_BY_FILE_ID_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, fileId),
        (rs) -> setEntityDataFromResultSet(rs),
        "SES_AI_T_SKILLSHEET_PERSON.selectOuterJoinByFileId");
  }

  /**
   * ResultSetからEntityにデータをセットする共通メソッド.
   *
   * @param resultSet ResultSet
   * @throws java.sql.SQLException
   */
  public void setEntityDataFromResultSet(java.sql.ResultSet resultSet)
      throws java.sql.SQLException {
    this.fileId = resultSet.getString("file_id");
    this.fileName = resultSet.getString("file_name");
    this.fileContentSummary = resultSet.getString("file_content_summary");
    this.personId = resultSet.getString("person_id");
    this.rawContent = resultSet.getString("raw_content");
    this.contentSummary = resultSet.getString("content_summary");
    BigDecimal unitPriceValue = resultSet.getBigDecimal("unit_price");
    this.unitPrice = unitPriceValue == null ? Money.empty() : new Money(unitPriceValue);
    java.sql.Timestamp ts = resultSet.getTimestamp("register_date");
    if (ts != null) {
      this.registerDate = new copel.sesproductpackage.core.unit.OriginalDateTime(ts);
    }
    this.registerUser = resultSet.getString("register_user");
    this.setFromGroup(resultSet.getString("from_group"));
    this.setFromId(resultSet.getString("from_id"));
    this.setFromName(resultSet.getString("from_name"));
    this.name = resultSet.getString("name");
    this.age = resultSet.getObject("age") != null ? resultSet.getInt("age") : null;
    String genderStr = resultSet.getString("gender");
    this.gender = genderStr == null ? null : copel.sesproductpackage.core.unit.Gender.valueOf(genderStr);
    this.nationality = resultSet.getString("nationality");
    String startDateStr = resultSet.getString("start_date");
    if (startDateStr != null) {
      this.startDate = new copel.sesproductpackage.core.unit.OriginalDateTime(startDateStr);
    }
    this.place = resultSet.getString("place");
    String areaStr = resultSet.getString("area");
    this.area = areaStr == null ? null : copel.sesproductpackage.core.unit.Area.valueOf(areaStr);
    this.officeAvailability = resultSet.getObject("office_availability") != null ? resultSet.getInt("office_availability") : null;
    this.organization = resultSet.getString("organization");
    this.experiences = resultSet.getString("experiences");
    this.otherRequirements = resultSet.getString("other_requirements");
    this.ngRequirements = resultSet.getString("ng_requirements");
    this.url = resultSet.getString("url");
  }

  @Override
  public String getRawContent() {
    return this.rawContent;
  }

  @Override
  public String getContentSummary() {
    return this.contentSummary;
  }

  @Override
  protected String getCheckSql() {
    return null; // 今回は使用しない
  }

  /**
   * 経験を改行区切りの文字列配列として取得します.
   *
   * @return 経験リスト
   */
  public List<String> getExperiencesAsList() {
    return this.experiences == null ? Arrays.asList() : Arrays.asList(this.experiences.split("\n"));
  }

  /**
   * その他条件を改行区切りの文字列配列として取得します.
   *
   * @return その他条件リスト
   */
  public List<String> getOtherRequirementsAsList() {
    return this.otherRequirements == null ? Arrays.asList() : Arrays.asList(this.otherRequirements.split("\n"));
  }

  /**
   * NG条件を改行区切りの文字列配列として取得します.
   *
   * @return NG条件リスト
   */
  public List<String> getNgRequirementsAsList() {
    return this.ngRequirements == null ? Arrays.asList() : Arrays.asList(this.ngRequirements.split("\n"));
  }
}
