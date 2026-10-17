package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.Money;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * マッチングテーブル、案件テーブル、要員テーブルを結合したエンティティ.
 *
 * <p>ポータル画面のマッチング一覧表示用に、マッチング情報に加えて案件名・単価、要員名・年齢・性別・単価を
 * 1クエリで取得するための結合Entity。JOB/PERSONが削除済みの場合、該当フィールドはNULLとなる。
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_T_MATCH_JOB_PERSON extends EntityBase {

  public SES_AI_T_MATCH_JOB_PERSON(String tenantId) {
    super(tenantId);
    this.tenantId = tenantId;
  }

  // ================================
  // SES_AI_T_MATCH のフィールド
  // ================================

  /** マッチングID / matching_id */
  @Column(required = true, primary = true, physicalName = "matching_id", logicalName = "マッチングID")
  private String matchingId;

  /** 担当者ユーザーID / user_id */
  @Column(physicalName = "user_id", logicalName = "担当者ユーザーID")
  private String userId;

  /** 案件ID / job_id */
  @Column(physicalName = "job_id", logicalName = "案件ID")
  private String jobId;

  /** 案件内容 / job_content */
  @Column(physicalName = "job_content", logicalName = "案件内容")
  private String jobContent;

  /** 要員ID / person_id */
  @Column(physicalName = "person_id", logicalName = "要員ID")
  private String personId;

  /** 要員内容 / person_content */
  @Column(physicalName = "person_content", logicalName = "要員内容")
  private String personContent;

  /** MatchingStatus / status_cd */
  @Column(physicalName = "status_cd", logicalName = "MatchingStatus")
  private MatchingStatus status;

  /** 評価文 / evaluation_text */
  @Column(physicalName = "evaluation_text", logicalName = "評価文")
  private String evaluationText;

  /** 点数 / score */
  @Column(physicalName = "score", logicalName = "点数")
  private Integer score;

  /** 必須スキル評価文 / must_evaluation_text */
  @Column(physicalName = "must_evaluation_text", logicalName = "必須スキル評価文")
  private String mustEvaluationText;

  /** 尚可スキル評価文 / want_evaluation_text */
  @Column(physicalName = "want_evaluation_text", logicalName = "尚可スキル評価文")
  private String wantEvaluationText;

  /** 場所評価文 / place_evaluation_text */
  @Column(physicalName = "place_evaluation_text", logicalName = "場所評価文")
  private String placeEvaluationText;

  /** 出社要件評価文 / office_evaluation_text */
  @Column(physicalName = "office_evaluation_text", logicalName = "出社要件評価文")
  private String officeEvaluationText;

  /** その他条件評価文 / other_evaluation_text */
  @Column(physicalName = "other_evaluation_text", logicalName = "その他条件評価文")
  private String otherEvaluationText;

  /** 利益 / profit */
  @Column(physicalName = "profit", logicalName = "利益")
  private Money profit;

  // ================================
  // SES_AI_T_JOB のフィールド（JOIN先）
  // ================================

  /** 案件名 / job_title */
  @Column(physicalName = "job_title", logicalName = "案件名")
  private String jobTitle;

  /** 案件単価 / job_unit_price */
  @Column(physicalName = "job_unit_price", logicalName = "案件単価")
  private Money jobUnitPrice;

  // ================================
  // SES_AI_T_PERSON のフィールド（JOIN先）
  // ================================

  /** 要員名 / person_name */
  @Column(physicalName = "person_name", logicalName = "要員名")
  private String personName;

  /** 要員年齢 / person_age */
  @Column(physicalName = "person_age", logicalName = "要員年齢")
  private Integer personAge;

  /** 要員性別 / person_gender */
  @Column(physicalName = "person_gender", logicalName = "要員性別")
  private Gender personGender;

  /** 要員単価 / person_unit_price */
  @Column(physicalName = "person_unit_price", logicalName = "要員単価")
  private Money personUnitPrice;

  /**
   * このレコードがjob_idを持つかどうかを判定します.
   *
   * @return 持っていればtrue、持たなければfalse
   */
  public boolean hasJobId() {
    return this.jobId != null && !this.jobId.isEmpty();
  }

  /**
   * このレコードがperson_idを持つかどうかを判定します.
   *
   * @return 持っていればtrue、持たなければfalse
   */
  public boolean hasPersonId() {
    return this.personId != null && !this.personId.isEmpty();
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_MATCH_JOB_PERSON は読み取り専用Entity。insert操作はサポートしていません。");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_MATCH_JOB_PERSON は読み取り専用Entity。selectByPk操作はサポートしていません。");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_MATCH_JOB_PERSON は読み取り専用Entity。updateByPk操作はサポートしていません。");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(
        "SES_AI_T_MATCH_JOB_PERSON は読み取り専用Entity。deleteByPk操作はサポートしていません。");
  }
}
