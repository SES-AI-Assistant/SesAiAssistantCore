package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.SesAiTableEntityBase;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.MultiLineText;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import lombok.Data;

/**
 * 【Entityクラス】 スキルシート情報と要員情報を結合したEntityクラス. スキルシートの要約と要員の要約を保持する.
 *
 * @author Copel Co., Ltd.
 */
@Data
@lombok.EqualsAndHashCode(callSuper = false)
public class SES_AI_T_SKILLSHEET_PERSON extends SesAiTableEntityBase {

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
  public int insert(Connection connection) throws java.sql.SQLException {
    throw new UnsupportedOperationException("SES_AI_T_SKILLSHEET_PERSON は登録をサポートしていません。");
  }

  @Override
  public boolean selectByPk(Connection connection) throws java.sql.SQLException {
    throw new UnsupportedOperationException("SES_AI_T_SKILLSHEET_PERSON は主キーによる取得をサポートしていません。");
  }

  @Override
  public boolean updateByPk(Connection connection) throws java.sql.SQLException {
    throw new UnsupportedOperationException("SES_AI_T_SKILLSHEET_PERSON は更新をサポートしていません。");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws java.sql.SQLException {
    throw new UnsupportedOperationException("SES_AI_T_SKILLSHEET_PERSON は削除をサポートしていません。");
  }

  private static final String SELECT_OUTER_JOIN_BY_PERSON_ID_SQL =
      "SELECT s.file_id, s.file_name, s.file_content_summary, p.person_id, p.mail_subject, "
          + "p.raw_content, p.content_summary, p.unit_price, p.register_date, p.register_user, "
          + "COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, "
          + "s.from_id) AS from_id, COALESCE(p.from_name, s.from_name) AS from_name, "
          + "p.name, p.age, p.gender, p.nationality, p.start_date, p.place, p.area, "
          + "p.office_availability, p.organization, p.experiences, "
          + "p.ng_requirements, p.other_requirements, p.url "
          + "FROM SES_AI_T_PERSON p LEFT JOIN SES_AI_T_SKILLSHEET s ON p.file_id = s.file_id "
          + "WHERE p.person_id = ?";

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
  public String toSkillSheetSelectionText() {
    String summary = this.contentSummary != null ? this.contentSummary : "";
    String fileSummary = this.fileContentSummary != null ? this.fileContentSummary : "";
    return "ファイルID：" + this.fileId + " 内容：" + fileSummary + summary;
  }

  /**
   * person_idをキーにしてOUTER JOIN検索し、取得結果をEntityにセットする. 要員とスキルシートが両方揃っていれば両方の情報を、片方なら片方の情報を返却する.
   *
   * @param connection DBコネクション
   * @param personId 要員ID
   * @throws java.sql.SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void selectOuterJoinByPersonId(Connection connection, String personId)
      throws java.sql.SQLException {
    executeSelectByPk(
        connection,
        SELECT_OUTER_JOIN_BY_PERSON_ID_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, personId),
        (rs) -> setEntityDataFromResultSet(rs),
        "SES_AI_T_SKILLSHEET_PERSON.selectOuterJoinByPersonId");
  }

  @Override
  public String getRawContent() {
    return this.rawContent;
  }

  @Override
  public String getContentSummary() {
    return this.contentSummary;
  }

  /**
   * 要約をエンベディングしてベクトルを設定します（本Entityは読み取り専用のため未対応）.
   *
   * <p>本Entityは結合結果の読み取り専用であり、ベクトルの生成と保存は元のスキルシート側で行う。基底の GPT
   * 呼び出しを継承すると、読み取り用の要員に意図しない外部呼び出しが発生するため、明示的に例外を投げる。
   *
   * @param transformer エンベディング用の変換器（使用しない）
   * @throws UnsupportedOperationException 常に投げる
   */
  @Override
  public void embedding(final Transformer transformer) {
    throw new UnsupportedOperationException(
        "SES_AI_T_SKILLSHEET_PERSON は読み取り専用のためエンベディングをサポートしていません。");
  }

  /**
   * 類似する要員が既に登録されているかを判定します（本Entityは読み取り専用のため未対応）.
   *
   * <p>本Entityは結合結果の読み取り専用であり、重複登録の判定は元のスキルシート側で行う。
   * 基底の判定を継承すると、読み取り用の要員に対して意図しない問い合わせが発生するため、明示的に例外を投げる。
   *
   * @param connection DB接続（使用しない）
   * @param similarityThreshold 類似度閾値（使用しない）
   * @return なし（常に例外を投げる）
   * @throws UnsupportedOperationException 常に投げる
   */
  @Override
  public boolean uniqueCheck(final Connection connection, final double similarityThreshold) {
    throw new UnsupportedOperationException(
        "SES_AI_T_SKILLSHEET_PERSON は読み取り専用のため重複チェックをサポートしていません。");
  }

  /**
   * 重複チェック用の SQL を返します（本Entityは読み取り専用のため未対応）.
   *
   * @return なし（常に例外を投げる）
   * @throws UnsupportedOperationException 常に投げる
   */
  @Override
  protected String getCheckSql() {
    throw new UnsupportedOperationException("SES_AI_T_SKILLSHEET_PERSON は重複チェックをサポートしていません。");
  }

  /**
   * ResultSetからEntityにデータをセットする共通メソッド.
   *
   * @param resultSet ResultSet
   * @throws java.sql.SQLException DB 操作（SQL の実行・結果の読み取り）に失敗した場合
   */
  public void setEntityDataFromResultSet(java.sql.ResultSet resultSet)
      throws java.sql.SQLException {
    this.fileId = resultSet.getString("file_id");
    this.fileName = resultSet.getString("file_name");
    this.fileContentSummary = resultSet.getString("file_content_summary");
    this.personId = resultSet.getString("person_id");
    this.mailSubject = resultSet.getString("mail_subject");
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
    this.gender =
        genderStr == null ? null : copel.sesproductpackage.core.unit.Gender.valueOf(genderStr);
    this.nationality = resultSet.getString("nationality");
    String startDateStr = resultSet.getString("start_date");
    if (startDateStr != null) {
      this.startDate = new copel.sesproductpackage.core.unit.OriginalDateTime(startDateStr);
    }
    this.place = resultSet.getString("place");
    String areaStr = resultSet.getString("area");
    this.area = areaStr == null ? null : copel.sesproductpackage.core.unit.Area.valueOf(areaStr);
    this.officeAvailability =
        resultSet.getObject("office_availability") != null
            ? resultSet.getInt("office_availability")
            : null;
    this.organization = resultSet.getString("organization");
    this.experiences = resultSet.getString("experiences");
    this.otherRequirements = resultSet.getString("other_requirements");
    this.ngRequirements = resultSet.getString("ng_requirements");
    this.url = resultSet.getString("url");
  }

  /**
   * 経験を改行区切りの文字列配列として取得します.
   *
   * @return 経験リスト
   */
  public List<String> getExperiencesAsList() {
    return new MultiLineText(this.experiences).toLines();
  }

  /**
   * その他条件を改行区切りの文字列配列として取得します.
   *
   * @return その他条件リスト
   */
  public List<String> getOtherRequirementsAsList() {
    return new MultiLineText(this.otherRequirements).toLines();
  }

  /**
   * NG条件を改行区切りの文字列配列として取得します.
   *
   * @return NG条件リスト
   */
  public List<String> getNgRequirementsAsList() {
    return new MultiLineText(this.ngRequirements).toLines();
  }
}
