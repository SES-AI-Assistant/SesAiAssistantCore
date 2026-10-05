package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * ウォッチテーブル、案件テーブル、要員テーブルを結合したエンティティ.
 *
 * <p>ポータル画面のウォッチ一覧表示用に、ウォッチ情報に加えて案件の詳細（タイトル、開始月、出社要件、単価等）、
 * 要員の詳細（名前、年齢、性別、開始月、出社許容、単価等）を1クエリで取得するための結合Entity。 JOB/PERSONが削除済みの場合、該当フィールドはNULLとなる（LEFT
 * JOINを使用）。
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_WATCH_JOB_PERSON extends EntityBase {

  public SES_AI_T_WATCH_JOB_PERSON(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }

  // ================================
  // SES_AI_T_WATCH のフィールド
  // ================================

  /** ユーザーID / user_id */
  @Column(physicalName = "user_id", logicalName = "ユーザーID")
  private String userId;

  /** 対象ID / target_id */
  @Column(physicalName = "target_id", logicalName = "対象ID")
  private String targetId;

  /** 対象種別 / target_type */
  @Column(physicalName = "target_type", logicalName = "対象種別")
  private SES_AI_T_WATCH.TargetType targetType;

  /** メモ / memo */
  @Column(physicalName = "memo", logicalName = "メモ")
  private String memo;

  /** 有効期限 / ttl */
  @Column(physicalName = "ttl", logicalName = "有効期限")
  private OriginalDateTime ttl;

  // ================================
  // SES_AI_T_JOB のフィールド（LEFT JOIN先）
  // ================================

  /** 案件名 / job_title */
  @Column(physicalName = "job_title", logicalName = "案件名")
  private String jobTitle;

  /** 案件送信者名 / job_from_name */
  @Column(physicalName = "job_from_name", logicalName = "案件送信者名")
  private String jobFromName;

  /** 案件要約 / job_content_summary */
  @Column(physicalName = "job_content_summary", logicalName = "案件要約")
  private String jobContentSummary;

  /** 案件原文 / job_raw_content */
  @Column(physicalName = "job_raw_content", logicalName = "案件原文")
  private String jobRawContent;

  /** 案件開始月 / job_start_date */
  @Column(physicalName = "job_start_date", logicalName = "案件開始月")
  private OriginalDateTime jobStartDate;

  /** 案件出社要件 / job_office_requirements */
  @Column(physicalName = "job_office_requirements", logicalName = "案件出社要件")
  private Integer jobOfficeRequirements;

  /** 案件単価 / job_unit_price */
  @Column(physicalName = "job_unit_price", logicalName = "案件単価")
  private Money jobUnitPrice;

  /** 案件勤務地 / job_place */
  @Column(physicalName = "job_place", logicalName = "案件勤務地")
  private String jobPlace;

  /** 案件エリア区分 / job_area */
  @Column(physicalName = "job_area", logicalName = "案件エリア区分")
  private String jobArea;

  // ================================
  // SES_AI_T_PERSON のフィールド（LEFT JOIN先）
  // ================================

  /** 要員名 / person_name */
  @Column(physicalName = "person_name", logicalName = "要員名")
  private String personName;

  /** 要員送信者名 / person_from_name */
  @Column(physicalName = "person_from_name", logicalName = "要員送信者名")
  private String personFromName;

  /** 要員要約 / person_content_summary */
  @Column(physicalName = "person_content_summary", logicalName = "要員要約")
  private String personContentSummary;

  /** 要員原文 / person_raw_content */
  @Column(physicalName = "person_raw_content", logicalName = "要員原文")
  private String personRawContent;

  /** 要員年齢 / person_age */
  @Column(physicalName = "person_age", logicalName = "要員年齢")
  private Integer personAge;

  /** 要員性別 / person_gender */
  @Column(physicalName = "person_gender", logicalName = "要員性別")
  private Gender personGender;

  /** 要員開始月 / person_start_date */
  @Column(physicalName = "person_start_date", logicalName = "要員開始月")
  private OriginalDateTime personStartDate;

  /** 要員出社許容 / person_office_availability */
  @Column(physicalName = "person_office_availability", logicalName = "要員出社許容")
  private Integer personOfficeAvailability;

  /** 要員単価 / person_unit_price */
  @Column(physicalName = "person_unit_price", logicalName = "要員単価")
  private Money personUnitPrice;

  /** 要員勤務地 / person_place */
  @Column(physicalName = "person_place", logicalName = "要員勤務地")
  private String personPlace;

  /** 要員エリア区分 / person_area */
  @Column(physicalName = "person_area", logicalName = "要員エリア区分")
  private String personArea;

  /**
   * このレコードが案件情報を持つかどうかを判定します. target_type='JOB' で、LEFT JOINの結果 jobTitle が存在する場合に true を返します。
   *
   * @return 案件情報を持っていればtrue、持たなければfalse
   */
  public boolean hasJobInfo() {
    return this.jobTitle != null && SES_AI_T_WATCH.TargetType.JOB == this.targetType;
  }

  /**
   * このレコードが要員情報を持つかどうかを判定します. target_type='PERSON' で、LEFT JOINの結果 personName が存在する場合に true を返します。
   *
   * @return 要員情報を持っていればtrue、持たなければfalse
   */
  public boolean hasPersonInfo() {
    return this.personName != null && SES_AI_T_WATCH.TargetType.PERSON == this.targetType;
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_WATCH_JOB_PERSON は読み取り専用Entity。insert操作はサポートしていません。");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_WATCH_JOB_PERSON は読み取り専用Entity。selectByPk操作はサポートしていません。");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_WATCH_JOB_PERSON は読み取り専用Entity。updateByPk操作はサポートしていません。");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_WATCH_JOB_PERSON は読み取り専用Entity。deleteByPk操作はサポートしていません。");
  }
}
