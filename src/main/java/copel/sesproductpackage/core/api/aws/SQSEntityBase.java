package copel.sesproductpackage.core.api.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * SQSメッセージのリクエストエンティティの基底クラス.
 *
 * @author 鈴木一矢
 *
 */
public abstract class SQSEntityBase {
    /**
     * キューイング対象のSQSのURL.
     */
    private final String queueUrl;
    /**
     * SQSクライアント.
     */
    private final AmazonSQS sqsClient;

    /**
     * コンストラクタ.
     *
     * @param queueUrl SQSのURL
     */
    public SQSEntityBase(final Regions region, final String queueUrl) {
        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withRegion(region)
                .build();
        this.queueUrl = queueUrl;
    }

    /**
     * このEntityの内容でSQSへメッセージをキューイングします.
     *
     * @return 送信結果
     * @throws JsonProcessingException 
     */
    public SendMessageResult sendMessage() throws JsonProcessingException {
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(this.queueUrl)
                .withMessageBody(this.getMessageBody());
        return this.sqsClient.sendMessage(sendMessageRequest);
    }

    /**
     * このEntityの内容をJSON形式のメッセージBody文字列に変換し返却します.
     *
     * @return メッセージBody
     * @throws JsonProcessingException 
     */
    protected abstract String getMessageBody() throws JsonProcessingException;
}
