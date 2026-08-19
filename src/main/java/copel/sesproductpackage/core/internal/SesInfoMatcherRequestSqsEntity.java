package copel.sesproductpackage.core.internal;

import java.sql.Connection;
import java.sql.SQLException;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import copel.sesproductpackage.core.api.aws.SQSEntityBase;
import copel.sesproductpackage.core.database.SES_AI_T_JOB;
import copel.sesproductpackage.core.database.SES_AI_T_PERSON;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AwsLambdaSesInfoMatcherに付帯するSQSへのリクエストEntityクラス.
 *
 * @author Copel Co., Ltd.
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SesInfoMatcherRequestSqsEntity extends SQSEntityBase {
  /** 案件ID. */
  @JsonProperty("job_id")
  private String jobId;

  /** 要員ID. */
  @JsonProperty("person_id")
  private String personId;

  /** 案件内容. */
  @JsonProperty("job_content")
  private String jobContent;

  /** 要員内容. */
  @JsonProperty("person_content")
  private String personContent;

  /** テナントID. */
  @JsonProperty("tenant_id")
  private String tenantId;

  /**
   * コンストラクタ.
   *
   * @param region リージョン
   */
  public SesInfoMatcherRequestSqsEntity(Regions region) {
    super(region, Properties.get(SsmParameterKey.MATCHER_QUEUE_URL.getKey()));
  }

  /**
   * JSONからのデシリアライゼーション用ファクトリメソッド. Jacksonがこのメソッドを使用してSQSメッセージボディをデシリアライズします.
   *
   * @param tenantId テナントID
   * @param jobId 案件ID
   * @param personId 要員ID
   * @param jobContent 案件内容
   * @param personContent 要員内容
   * @return SesInfoMatcherRequestSqsEntityインスタンス
   */
  @JsonCreator
  public static SesInfoMatcherRequestSqsEntity fromJson(
      @JsonProperty("tenant_id") final String tenantId,
      @JsonProperty("job_id") final String jobId,
      @JsonProperty("person_id") final String personId,
      @JsonProperty("job_content") final String jobContent,
      @JsonProperty("person_content") final String personContent) {
    SesInfoMatcherRequestSqsEntity entity =
        new SesInfoMatcherRequestSqsEntity(Regions.AP_NORTHEAST_1);
    entity.setTenantId(tenantId);
    entity.setJobId(jobId);
    entity.setPersonId(personId);
    entity.setJobContent(jobContent);
    entity.setPersonContent(personContent);
    return entity;
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
