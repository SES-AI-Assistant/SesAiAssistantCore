package copel.sesproductpackage.core.api.slack;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.http.HttpException;

import com.fasterxml.jackson.databind.ObjectMapper;

import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.SlackApiResponse;
import copel.sesproductpackage.core.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 * 【フレームワーク部品】
 * Slack通知するためのクラス.
 *
 * @author 鈴木一矢
 *
 */
@Slf4j
public class SlackNotifier {
    /**
     * Slack Chat Post Message URL.
     *
     * https://docs.slack.dev/reference/methods/chat.postMessage/
     */
    private static final String CHAT_POST_MESSAGE_URL = "https://slack.com/api/chat.postMessage";
    /**
     * Slack Incoming Webhook URL.
     *
     * https://docs.slack.dev/messaging/sending-messages-using-incoming-webhooks/?utm_source=chatgpt.com#advanced_message_formatting
     */
    private static final String SLACK_INCOMING_WEBHOOK_URL = Properties.get("SLACK_INCOMING_WEBHOOK_URL");
    /**
     * Slack Bot Token.
     */
    private static final String SLACK_BOT_TOKEN = Properties.get("SLACK_BOT_TOKEN");

    /**
     * メッセージを送信する.
     *
     * @param message 送信メッセージ.
     * @return 送信したメッセージのタイムスタンプ
     * @throws Exception
     */
    public static String send(final SlackWebhookMessageEntity messageEntity) throws Exception {
        log.debug("Slack Request Payload: {}", messageEntity.toJson());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_POST_MESSAGE_URL))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + SLACK_BOT_TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(messageEntity.toJson()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            log.error("Slackへの通知リクエストを行いましたが、レスポンスが200以外です。 StatusCode: {}", response.statusCode());
            throw new HttpException("Slackへの通知リクエストを行いましたが、レスポンスが200以外です");
        }

        ObjectMapper mapper = new ObjectMapper();
        SlackApiResponse responseBody = mapper.readValue(response.body(), SlackApiResponse.class);

        if (!responseBody.isOk()) {
            log.error("Slackへの通知リクエストは成功しましたが、エラーが返却されました。 error: {}", responseBody.getError());
            throw new HttpException("Slackへの通知リクエストは成功しましたが、エラーが返却されました。");
        }
        return responseBody.getTs();
    }

    /**
     * Webhookを利用してメッセージを送信する.
     *
     * @param message 送信メッセージ.
     * @throws Exception
     */
    public static void sendByWebhook(final SlackWebhookMessageEntity messageEntity) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SLACK_INCOMING_WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(messageEntity.toJson()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() / 100 != 2) {
            log.error("Slackへの通知リクエストを行いましたが、レスポンスが200以外です。 StatusCode: {}", response.statusCode());
            throw new HttpException("Slackへの通知リクエストを行いましたが、レスポンスが200以外です");
        }
    }
}
