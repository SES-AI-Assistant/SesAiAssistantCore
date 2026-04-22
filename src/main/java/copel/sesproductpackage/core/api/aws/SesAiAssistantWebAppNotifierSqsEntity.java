package copel.sesproductpackage.core.api.aws;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.api.SesAiAssistantWebAppNotifierRequestEntity;

/**
 * Web Push 通知 SQS 送信エンティティ. SesAiAssistantWebAppNotifier Lambda をトリガーする SQS
 * へメッセージを送信する.
 *
 * @author Copel Co., Ltd.
 */
public class SesAiAssistantWebAppNotifierSqsEntity extends SQSEntityBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final SesAiAssistantWebAppNotifierRequestEntity request;

  /**
   * コンストラクタ.
   *
   * @param queueUrl SQS の URL
   * @param request プッシュ通知リクエスト
   */
  public SesAiAssistantWebAppNotifierSqsEntity(
      String queueUrl, SesAiAssistantWebAppNotifierRequestEntity request) {
    super(Regions.AP_NORTHEAST_1, queueUrl);
    this.request = request;
  }

  @Override
  protected String getMessageBody() throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(this.request);
  }
}
