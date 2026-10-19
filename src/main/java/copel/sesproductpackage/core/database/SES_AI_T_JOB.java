package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 【Entityクラス】 案件情報(SES_AI_T_JOB)テーブル.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_JOB extends SES_AI_T_EntityBase {

  public SES_AI_T_JOB(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }

  // ================================
  // SQL
  // ================================
  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_T_JOB (job_id, from_group, from_id, from_name, mail_subject, raw_content, content_summary, unit_price, vector_data, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String SELECT_SQL =
      "SELECT job_id, from_group, from_id, from_name, mail_subject, raw_content, content_summary, unit_price, vector_data, title, overview, must_skills, want_skills, start_date, place, area, office_requirements, other_requirements, register_date, register_user, ttl FROM SES_AI_T_JOB WHERE job_id = ?";

  /** UPDATE文（tenantId フィルタなし、テンプレートメソッドが自動追加する）. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_T_JOB SET from_group = ?, from_id = ?, from_name = ?, mail_subject = ?, raw_content = ?, content_summary = ?, unit_price = ?, vector_data = ?::vector, title = ?, overview = ?, must_skills = ?, want_skills = ?, start_date = ?, place = ?, area = ?, office_requirements = ?, other_requirements = ?, ttl = ? WHERE job_id = ?";

  /** 重複チェック用SQL. */
  private static final String CHECK_SQL =
      "SELECT COUNT(*) FROM SES_AI_T_JOB WHERE tenant_id = ? AND raw_content % ? AND similarity(raw_content, ?) > ?";

  /** 単価等取得用・重複チェックSQL. */
  private static final String FIND_SIMILAR_SQL =
      "SELECT job_id, unit_price FROM SES_AI_T_JOB WHERE tenant_id = ? AND regexp_replace(raw_content, 'https?://[^\\s]+', '', 'g') % ? AND similarity(regexp_replace(raw_content, 'https?://[^\\s]+', '', 'g'), ?) > ? LIMIT 1";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_T_JOB WHERE job_id = ?";

  /** 場所 / place カラムの最大文字数（DDL の VARCHAR(30) に対応）. */
  public static final int PLACE_MAX_LENGTH = 30;

  // ================================
  // メンバ
  // ================================
  /** 【PK】 案件ID* / job_id */
  @Column(required = true, primary = true, physicalName = "job_id", logicalName = "案件ID")
  private String jobId;

  /** 原文 / raw_content */
  @Column(physicalName = "raw_content", logicalName = "原文")
  private String rawContent;

  /** 要約 / content_summary */
  @Column(physicalName = "content_summary", logicalName = "要約")
  private String contentSummary;

  /** 単価 / unit_price */
  @Column(physicalName = "unit_price", logicalName = "単価")
  private Money unitPrice;

  /** 案件名 / title */
  @Column(physicalName = "title", logicalName = "案件名")
  private String title;

  /** 案件概要 / overview */
  @Column(physicalName = "overview", logicalName = "案件概要")
  private String overview;

  /** 必須スキル / must_skills */
  @Column(physicalName = "must_skills", logicalName = "必須スキル")
  private String mustSkills;

  /** 尚可スキル / want_skills */
  @Column(physicalName = "want_skills", logicalName = "尚可スキル")
  private String wantSkills;

  /** 開始日 / start_date */
  @Column(physicalName = "start_date", logicalName = "開始日")
  private OriginalDateTime startDate;

  /** 場所 / place */
  @Column(physicalName = "place", logicalName = "場所")
  private String place;

  /** 地域 / area */
  @Column(physicalName = "area", logicalName = "地域")
  private Area area;

  /** 出社要求日数 / office_requirements */
  @Column(physicalName = "office_requirements", logicalName = "出社要求日数")
  private Integer officeRequirements;

  /** その他条件 / other_requirements */
  @Column(physicalName = "other_requirements", logicalName = "その他条件")
  private String otherRequirements;

  // ================================
  // メソッド
  // ================================
  /**
   * LLMに最もマッチする案件の案件IDを選出させるための文章に変換する.
   *
   * @return 変換後の文章
   */
  public String to案件選出用文章() {
    return "案件ID：" + this.jobId + "内容：" + this.rawContent;
  }

  /**
   * 場所を設定します.
   *
   * <p>場所はLLMの抽出結果をそのまま受け取ることが多く、スキーマの文字数指定（maxLength）が守られない場合がある
   * （例: 最寄駅の記載が無いと「フルリモート希望（…）」のような通勤条件の文章が入る）。カラム長を超えるとINSERT/UPDATEが
   * 失敗して案件情報の登録自体が失われるため、カラムの最大文字数で切り詰めて保持する.
   *
   * <p>また、LLMが「記載なし」を空文字や"null"・"Unknown"という文字列で返す場合があり、null判定のみの利用側（要約文や通知文）で
   * 「不明」扱いにならず空欄表示になるため、空とみなせる値はnullに正規化する.
   *
   * @param place 場所
   */
  public void setPlace(final String place) {
    this.place =
        OriginalStringUtils.isUnspecified(place)
            ? null
            : OriginalStringUtils.truncateByCodePoints(place, PLACE_MAX_LENGTH);
  }

  /**
   * 画面・通知に表示するための場所を取得します.
   *
   * <p>場所は記載が無ければnullになるため、文字列連結やString.formatにそのまま渡すと「null」と表示されてしまう。
   * 要約文やプッシュ通知で表記を揃えるため、未設定の場合は「不明」を返す.
   *
   * @return 場所。未設定の場合は「不明」
   */
  public String getPlaceForDisplay() {
    return OriginalStringUtils.isUnspecified(this.place) ? "不明" : this.place;
  }

  /**
   * 画面・通知に表示するための案件名を取得します.
   *
   * <p>案件名はDB上nullを許容しており、そのまま通知文に埋め込むと「【null】」と表示されてしまうため、
   * 未設定（LLMが返す「Unknown」を含む）の場合は「案件名不明」を返す.
   *
   * @return 案件名。未設定の場合は「案件名不明」
   */
  public String getTitleForDisplay() {
    return OriginalStringUtils.isUnspecified(this.title) ? "案件名不明" : this.title;
  }

  /**
   * 画面・通知に表示するための出社要件を取得します.
   *
   * <p>出社要求日数は0がフルリモート、5が常駐を意味するため（JobInfoSchemaの定義）、「週0日」のような
   * 不自然な表示にならないよう要約文（getSummaryWithTargetPrice）と同じ表記に揃える。未設定（null）の場合も
   * 「null」と表示されないよう「不明」を返す.
   *
   * @return 出社要件（例: 「フルリモート」「常駐（品川駅）」「週3日（品川駅）」「不明」）
   */
  public String getOfficeRequirementsForDisplay() {
    if (this.officeRequirements == null) {
      return "不明";
    }
    if (this.officeRequirements == 0) {
      return "フルリモート";
    }
    if (this.officeRequirements == 5) {
      return "常駐（" + this.getPlaceForDisplay() + "）";
    }
    return "週" + this.officeRequirements + "日（" + this.getPlaceForDisplay() + "）";
  }

  /**
   * プッシュ通知に表示するための出社要件を取得します.
   *
   * <p>通知は1行あたりの表示幅が狭いため、{@link #getOfficeRequirementsForDisplay()} と異なり場所を付けずに短く表記する。
   * また「不明」の表示は情報量が無く行を圧迫するだけなので、未設定（null）の場合はnullを返し、呼び出し側で項目ごと省略させる.
   *
   * @return 出社要件（例: 「フルリモート」「常駐」「週3出社」）。未設定の場合はnull
   * @author Copel Co., Ltd.
   */
  public String getOfficeRequirementsForNotification() {
    if (this.officeRequirements == null) {
      return null;
    }
    if (this.officeRequirements == 0) {
      return "フルリモート";
    }
    if (this.officeRequirements == 5) {
      return "常駐";
    }
    return "週" + this.officeRequirements + "出社";
  }

  /**
   * 案件情報を「案件名｜単価」形式で返す.
   *
   * <p>プッシュ通知の本文で案件を1行で表示するために使用する。単価が未設定の場合は「｜」以降を省略する.
   *
   * @return フォーマットされた案件情報（例：「Salesforce導入支援｜79万円」「案件名不明｜スキル見合い」）
   * @author Copel Co., Ltd.
   */
  public String toNotificationInfo() {
    String priceLabel = this.unitPrice == null ? null : this.unitPrice.toPriceLabel();
    return priceLabel == null
        ? this.getTitleForDisplay()
        : this.getTitleForDisplay() + "｜" + priceLabel;
  }

  /**
   * URLを除外したテキストで類似レコードを検索し、存在する場合はそのレコードのIDと単価を返す.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param textToCheck チェック対象のテキスト
   * @param similarityThreshold 類似度のしきい値
   * @return 類似レコードが見つかった場合はそのSES_AI_T_JOB、見つからなかった場合はnull
   * @throws SQLException
   */
  public static SES_AI_T_JOB findSimilarRecord(
      final Connection connection,
      final String tenantId,
      final String textToCheck,
      final double similarityThreshold)
      throws SQLException {
    if (connection == null || tenantId == null || textToCheck == null) {
      return null;
    }
    String maskedText = textToCheck.replaceAll("https?://[^\\s]+", "");

    try (PreparedStatement preparedStatement = connection.prepareStatement(FIND_SIMILAR_SQL)) {
      preparedStatement.setString(1, tenantId);
      preparedStatement.setString(2, maskedText);
      preparedStatement.setString(3, maskedText);
      preparedStatement.setDouble(4, similarityThreshold);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          SES_AI_T_JOB result = new SES_AI_T_JOB(tenantId);
          result.setJobId(resultSet.getString("job_id"));
          BigDecimal unitPriceValue = resultSet.getBigDecimal("unit_price");
          result.setUnitPrice(unitPriceValue == null ? Money.empty() : new Money(unitPriceValue));
          return result;
        }
      }
    }
    return null;
  }

  // ================================
  // Overrideメソッド
  // ================================
  @Override
  public String getRawContent() {
    return this.rawContent;
  }

  @Override
  public String getContentSummary() {
    return this.contentSummary;
  }

  @Override
  public String getCheckSql() {
    return CHECK_SQL;
  }

  /**
   * この案件情報をデータベースに挿入します.
   *
   * @param connection DBコネクション
   * @return 挿入に影響を受けた行数
   * @throws SQLException DB操作エラー
   */
  @Override
  public int insert(Connection connection) throws SQLException {
    // 案件IDを発行
    this.jobId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.jobId);
          stmt.setString(2, this.fromGroup);
          stmt.setString(3, this.fromId);
          stmt.setString(4, this.fromName);
          stmt.setString(5, this.mailSubject);
          stmt.setString(6, this.rawContent);
          stmt.setString(7, this.contentSummary);
          stmt.setObject(8, this.unitPrice == null ? null : this.unitPrice.getValue());
          stmt.setString(9, this.vectorData == null ? null : this.vectorData.toString());
          stmt.setString(10, this.title);
          stmt.setString(11, this.overview);
          stmt.setString(12, this.mustSkills);
          stmt.setString(13, this.wantSkills);
          stmt.setTimestamp(14, this.startDate == null ? null : this.startDate.toTimestamp());
          stmt.setString(15, this.place);
          stmt.setString(16, this.area == null ? null : this.area.toString());
          stmt.setObject(17, this.officeRequirements);
          stmt.setString(18, this.otherRequirements);
          stmt.setTimestamp(19, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(20, this.registerUser);
          stmt.setTimestamp(21, this.ttl == null ? null : this.ttl.toTimestamp());
        },
        "SES_AI_T_JOB.insert");
  }

  /**
   * 主キーに基づいて案件情報を検索します.
   *
   * @param connection DBコネクション
   * @throws SQLException DB操作エラー
   */
  @Override
  public void selectByPk(final Connection connection) throws SQLException {
    if (this.jobId == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.jobId),
        (rs) -> {
          this.fromGroup = rs.getString("from_group");
          this.fromId = rs.getString("from_id");
          this.fromName = rs.getString("from_name");
          this.mailSubject = rs.getString("mail_subject");
          this.rawContent = rs.getString("raw_content");
          this.contentSummary = rs.getString("content_summary");
          BigDecimal unitPriceValue = rs.getBigDecimal("unit_price");
          this.unitPrice = unitPriceValue == null ? Money.empty() : new Money(unitPriceValue);
          this.title = rs.getString("title");
          this.overview = rs.getString("overview");
          this.mustSkills = rs.getString("must_skills");
          this.wantSkills = rs.getString("want_skills");
          this.startDate = new OriginalDateTime(rs.getString("start_date"));
          // 既存行に残る空文字や"null"文字列も表示されないよう、setPlaceで正規化して保持する
          this.setPlace(rs.getString("place"));
          String areaStr = rs.getString("area");
          this.area = areaStr == null ? null : Area.valueOf(areaStr);
          this.officeRequirements =
              rs.getObject("office_requirements") == null ? null : rs.getInt("office_requirements");
          this.otherRequirements = rs.getString("other_requirements");
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
          this.ttl = new OriginalDateTime(rs.getString("ttl"));
        },
        "SES_AI_T_JOB.selectByPk");
  }

  /**
   * 主キーに基づいて案件情報を更新します.
   *
   * @param connection DBコネクション
   * @return 更新成功時はtrue、それ以外はfalse
   * @throws SQLException DB操作エラー
   */
  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.jobId == null) {
      return false;
    }
    return executeUpdateByPk(
        connection,
        UPDATE_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.fromGroup);
          stmt.setString(2, this.fromId);
          stmt.setString(3, this.fromName);
          stmt.setString(4, this.mailSubject);
          stmt.setString(5, this.rawContent);
          stmt.setString(6, this.contentSummary);
          stmt.setObject(7, this.unitPrice == null ? null : this.unitPrice.getValue());
          stmt.setString(8, this.vectorData == null ? null : this.vectorData.toString());
          stmt.setString(9, this.title);
          stmt.setString(10, this.overview);
          stmt.setString(11, this.mustSkills);
          stmt.setString(12, this.wantSkills);
          stmt.setTimestamp(13, this.startDate == null ? null : this.startDate.toTimestamp());
          stmt.setString(14, this.place);
          stmt.setString(15, this.area == null ? null : this.area.toString());
          stmt.setObject(16, this.officeRequirements);
          stmt.setString(17, this.otherRequirements);
          stmt.setTimestamp(18, this.ttl == null ? null : this.ttl.toTimestamp());
          stmt.setString(19, this.jobId);
        },
        "SES_AI_T_JOB.updateByPk");
  }

  /**
   * 主キーに基づいて案件情報を削除します.
   *
   * @param connection DBコネクション
   * @return 削除成功時はtrue、それ以外はfalse
   * @throws SQLException DB操作エラー
   */
  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.jobId == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.jobId),
        "SES_AI_T_JOB.deleteByPk");
  }

  /**
   * 案件概要を1行に整形し、指定文字数で短縮した形で返す.
   *
   * <p>プッシュ通知の本文で案件概要を表示する際に使用する。概要に含まれる改行をそのまま渡すと通知の行構成が崩れるため、
   * 連続する空白・改行は半角スペース1つにまとめる。また、OSに省略を任せると端末によって途中で切れたり省略記号が付かなかったりするため、
   * 指定文字数（コードポイント数）で切って末尾に「…」を付ける.
   *
   * @param maxLength 最大文字数（省略記号を除く）
   * @return 短縮された案件概要。maxLength以下の場合はそのまま返す。超過時は末尾に「…」を付ける。未設定の場合は空文字
   * @author Copel Co., Ltd.
   */
  public String getTruncatedOverview(int maxLength) {
    if (OriginalStringUtils.isUnspecified(this.overview)) {
      return "";
    }
    // Javaの\sは全角スペースを含まないため明示的に対象に加える
    String singleLine = this.overview.strip().replaceAll("[\\s\\u3000]+", " ");
    String truncated = OriginalStringUtils.truncateByCodePoints(singleLine, maxLength);
    return truncated.equals(singleLine) ? singleLine : truncated + "…";
  }

  /**
   * 必須スキルを改行区切りの文字列配列として取得します.
   *
   * @return 必須スキルリスト
   */
  public List<String> getMustSkillsAsList() {
    return this.mustSkills == null ? Arrays.asList() : Arrays.asList(this.mustSkills.split("\n"));
  }

  /**
   * 尚可スキルを改行区切りの文字列配列として取得します.
   *
   * @return 尚可スキルリスト
   */
  public List<String> getWantSkillsAsList() {
    return this.wantSkills == null ? Arrays.asList() : Arrays.asList(this.wantSkills.split("\n"));
  }

  /**
   * その他条件を改行区切りの文字列配列として取得します.
   *
   * @return その他条件リスト
   */
  public List<String> getOtherRequirementsAsList() {
    return this.otherRequirements == null
        ? Arrays.asList()
        : Arrays.asList(this.otherRequirements.split("\n"));
  }

  /**
   * 指定した単価に書き換えた案件サマリを作成する。 指定された単価がスキル見合い単価である場合、案件の単価に指定された乗数をかけた金額を記載する。（未指定の場合は0.9倍）
   * 単価が未指定の場合は単価未記載のサマリを作成する。
   *
   * @param targetPrice 指定単価
   * @param multiplier 単価倍率
   * @return 案件サマリ
   */
  public String getSummaryWithTargetPrice(Money targetPrice) {
    return getSummaryWithTargetPrice(targetPrice, 0.9);
  }

  public String getSummaryWithTargetPrice(Money targetPrice, double multiplier) {
    StringBuilder sb = new StringBuilder();
    // 1. 案件名
    sb.append(this.title).append("\n");
    // 2. 概要
    sb.append("■概要\n").append(this.overview).append("\n");
    // 3. 必須
    if (this.mustSkills != null && !this.mustSkills.isEmpty()) {
      sb.append("■必須\n").append(this.mustSkills).append("\n");
    }
    // 4. 尚可
    if (this.wantSkills != null && !this.wantSkills.isEmpty()) {
      sb.append("■尚可\n").append(this.wantSkills).append("\n");
    }
    // 5. 開始
    if (this.startDate != null) {
      sb.append("■開始: ").append(this.startDate.month()).append("\n");
    }
    // 6. 単価
    if (targetPrice != null && !targetPrice.isEmpty()) {
      if (targetPrice.isNegotiable()) {
        sb.append("■単価: ")
            .append(this.unitPrice.multiply(multiplier).toJapaneseFormat())
            .append("\n");
      } else {
        sb.append("■単価: ").append(targetPrice.toJapaneseFormat()).append("\n");
      }
    }
    // 7. 出社要件、場所
    if (this.officeRequirements == 0) {
      sb.append("■出社要件: ").append("フルリモート\n");
    } else if (this.officeRequirements == 5) {
      sb.append("■出社要件: ").append("常駐").append("\n");
      sb.append("■場所: ").append(this.getPlaceForDisplay()).append("\n");
    } else {
      sb.append("■出社要件: ").append("週").append(this.officeRequirements).append("\n");
      sb.append("■場所: ").append(this.getPlaceForDisplay()).append("\n");
    }
    // 8. その他
    if (this.otherRequirements != null && !this.otherRequirements.isEmpty()) {
      sb.append("■その他\n").append(otherRequirements).append("\n");
    }
    return sb.toString();
  }
}
