package copel.sesproductpackage.core.internal;

import java.sql.Connection;
import java.sql.SQLException;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import copel.sesproductpackage.core.api.aws.SQSEntityBase;
import copel.sesproductpackage.core.database.SES_AI_T_JOB;
import copel.sesproductpackage.core.database.SES_AI_T_PERSON;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import lombok.Data;

/**
 * AwsLambdaSesInfoMatcherに付帯するSQSへのリクエストEntityクラス.
 *
 * @author Copel Co., Ltd.
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SesInfoMatcherRequestSqsEntity extends SQSEntityBase {
  /** 案件ID. */
  private String jobId;

  /** 要員ID. */
  private String personId;

  /** 案件内容. */
  private String jobContent;

  /** 要員内容. */
  private String personContent;

  /** テナントID. */
  private String tenantId;

  /**
   * コンストラクタ.
   *
   * @param region リージョン
   * @param queueUrl SQSのURL
   */
  public SesInfoMatcherRequestSqsEntity(Regions region, String queueUrl) {
    super(region, queueUrl);
  }

  @Override
  protected String getMessageBody() {
    try {
      return ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * このリクエストのバリデーションチェックをする.
   * job_idまたはjob_contentのどちらかが存在する、またはperson_idまたはperson_contentのどちらかが存在する事を確認する
   * なおかつ、tenantIdが存在する事を確認する.
   *
   * @return 正常であればtrue、異常であればfalse
   */
  public boolean isValid() {
    return (this.hasJob() || this.hasPerson()) && !OriginalStringUtils.isEmpty(this.tenantId);
  }

  /**
   * 案件情報を持つかどうを判定します.
   *
   * @return 案件情報を持てばtrue、持たなければfalse.
   */
  public boolean hasJob() {
    return this.hasJobId() || !OriginalStringUtils.isEmpty(this.jobContent);
  }

  /**
   * 要員情報を持つかどうを判定します.
   *
   * @return 要員情報を持てばtrue、持たなければfalse.
   */
  public boolean hasPerson() {
    return this.hasPersonId() || !OriginalStringUtils.isEmpty(this.personContent);
  }

  /**
   * job_idを持つかどうかを判定します.
   *
   * @return job_idを持つ場合はtrue、持っていない場合はfalse
   */
  public boolean hasJobId() {
    return !OriginalStringUtils.isEmpty(this.jobId);
  }

  /**
   * person_idを持つかどうかを判定します.
   *
   * @return person_idを持つ場合はtrue、持っていない場合はfalse
   */
  public boolean hasPersonId() {
    return !OriginalStringUtils.isEmpty(this.personId);
  }

  /**
   * 案件情報を取得します.
   *
   * @param connection DBコネクション
   * @return リクエストのjob_content、またはDBから取得した案件情報
   * @throws SQLException DBアクセスエラーが発生した場合
   */
  public String getJobInfo(final Connection connection) throws SQLException {
    if (this.hasJobId()) {
      SES_AI_T_JOB job = new SES_AI_T_JOB(this.tenantId);
      job.setJobId(this.jobId);
      job.selectByPk(connection);
      return job.getRawContent();
    } else {
      return this.jobContent;
    }
  }

  /**
   * 案件情報を取得します.
   *
   * @param connection DBコネクション
   * @return リクエストのperson_content、またはDBから取得した案件情報
   * @throws SQLException DBアクセスエラーが発生した場合
   */
  public String getPersonInfo(final Connection connection) throws SQLException {
    if (this.hasPersonId()) {
      SES_AI_T_PERSON person = new SES_AI_T_PERSON(this.tenantId);
      person.setPersonId(this.personId);
      person.selectByPk(connection);
      return person.getRawContent();
    } else {
      return this.personContent;
    }
  }
}
