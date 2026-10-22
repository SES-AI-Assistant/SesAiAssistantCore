package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.unit.Gender;
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
public class SES_AI_T_MATCH_JOB_PERSON extends SES_AI_T_MATCH {

  /**
   * テナントIDを指定して生成します.
   *
   * @param tenantId テナントID
   */
  public SES_AI_T_MATCH_JOB_PERSON(String tenantId) {
    super(tenantId);
  }

  /** 読み取り専用のため、書き込みと削除の操作を受け付けない理由を示す。 */
  private static final String READ_ONLY_MESSAGE =
      "SES_AI_T_MATCH_JOB_PERSON は読み取り専用Entity。%s操作はサポートしていません。";

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
   * <p>結合Entityは従来どおり null と空文字だけを未設定とみなす。親の判定（空白や文字列 "null" も未設定）とは扱いが異なるため上書きする。
   *
   * @return 持っていればtrue、持たなければfalse
   */
  @Override
  public boolean hasJobId() {
    return this.getJobId() != null && !this.getJobId().isEmpty();
  }

  /**
   * このレコードがperson_idを持つかどうかを判定します.
   *
   * <p>結合Entityは従来どおり null と空文字だけを未設定とみなす。親の判定と扱いが異なるため上書きする。
   *
   * @return 持っていればtrue、持たなければfalse
   */
  @Override
  public boolean hasPersonId() {
    return this.getPersonId() != null && !this.getPersonId().isEmpty();
  }

  /**
   * 読み取り専用のため、登録操作は受け付けずに例外を投げます.
   *
   * @param connection DBコネクション
   * @return なし（常に例外を投げる）
   */
  @Override
  public int insert(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(String.format(READ_ONLY_MESSAGE, "insert"));
  }

  /**
   * 読み取り専用のため、主キーによる取得操作は受け付けずに例外を投げます.
   *
   * @param connection DBコネクション
   */
  @Override
  public void selectByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(String.format(READ_ONLY_MESSAGE, "selectByPk"));
  }

  /**
   * 読み取り専用のため、更新操作は受け付けずに例外を投げます.
   *
   * @param connection DBコネクション
   * @return なし（常に例外を投げる）
   */
  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(String.format(READ_ONLY_MESSAGE, "updateByPk"));
  }

  /**
   * 読み取り専用のため、削除操作は受け付けずに例外を投げます.
   *
   * @param connection DBコネクション
   * @return なし（常に例外を投げる）
   */
  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    throw new UnsupportedOperationException(String.format(READ_ONLY_MESSAGE, "deleteByPk"));
  }
}
