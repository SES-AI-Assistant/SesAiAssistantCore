package copel.sesproductpackage.core.internal;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.core.JsonProcessingException;

import copel.sesproductpackage.core.api.aws.SQSEntityBase;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;

/**
 * Web Push 通知 SQS 送信エンティティ. SesAiAssistantWebAppNotifier Lambda をトリガーする SQS へメッセージを送信する.
 *
 * @author Copel Co., Ltd.
 */
public class SesAiAssistantWebAppNotifierSqsEntity extends SQSEntityBase {
  private static final String SQS_QUEUE_URL_WEBAPP_NOTIFIER =
      Properties.get(SsmParameterKey.NOTIFIER_QUEUE_URL.getKey());

  private final SesAiAssistantWebAppNotifierRequestEntity request;

  /**
   * コンストラクタ.
   *
   * @param queueUrl SQS の URL
   * @param request プッシュ通知リクエスト
   */
  public SesAiAssistantWebAppNotifierSqsEntity(SesAiAssistantWebAppNotifierRequestEntity request) {
    super(Regions.AP_NORTHEAST_1, SQS_QUEUE_URL_WEBAPP_NOTIFIER);
    this.request = request;
  }

  @Override
  protected String getMessageBody() throws JsonProcessingException {
    return ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(this.request);
  }
}
