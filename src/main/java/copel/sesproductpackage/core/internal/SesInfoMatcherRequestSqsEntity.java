package copel.sesproductpackage.core.internal;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import copel.sesproductpackage.core.api.aws.SQSEntityBase;
import copel.sesproductpackage.core.util.OriginalStringUtils;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AwsLambdaSesInfoMatcherに付帯するSQSへのリクエストEntityクラス.
 *
 * <p>Matcher Lambda は本クラスを JSON から読み込むだけで、本クラスから送信しない。Matcher 向けの送信は Register Lambda が独自に行う。
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = false)
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

  /**
   * 送信用のメッセージ本文を作ります。本番では送信しないため、常に例外を投げます.
   *
   * <p>基底クラスの抽象メソッドを実装するために残している。Matcher 向けの送信は Register Lambda の独自実装で行うため、本クラスからは送らない。
   *
   * @return なし。常に例外を投げる
   * @throws UnsupportedOperationException 常に投げる
   */
  @Override
  protected String getMessageBody() {
    throw new UnsupportedOperationException("本クラスからの SQS 送信は行わない");
  }

  /**
   * このリクエストのバリデーションチェックをする.
   * job_idまたはjob_contentのどちらかが存在する、またはperson_idまたはperson_contentのどちらかが存在する事を確認する
   * なおかつ、tenantIdが存在する事を確認する.
   *
   * @return 正常であればtrue、異常であればfalse
   */
  @JsonIgnore
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
}
