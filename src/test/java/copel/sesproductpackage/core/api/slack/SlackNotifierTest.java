package copel.sesproductpackage.core.api.slack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.AccessoryType;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.BlockType;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.ImageAccessory;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.SlackMessageBlock;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.TextObject;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.TextType;
import copel.sesproductpackage.core.util.Properties;

class SlackNotifierTest {
    /**
     * ウォッチ用SlackチャンネルのID.
     */
    private static final String SLACK_WATCH_NOTIFY_CHANNEL_ID = Properties.get("SLACK_WATCH_NOTIFY_CHANNEL_ID");
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

    @Test
    void sendByWebhookTest() {
        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        entity.setText("Junitテスト(sendByWebhookTest)実行");

        try {
            SlackNotifier.sendByWebhook(SLACK_INCOMING_WEBHOOK_URL, entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void sendMessageTest() {
        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        entity.setChannel(SLACK_WATCH_NOTIFY_CHANNEL_ID);
        entity.setText("Junitテスト(sendMessageTest)実行");

        try {
            String ts = SlackNotifier.send(SLACK_BOT_TOKEN, entity);
            assertNotNull(ts);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void sendCodeSnipetMessageTest() {
        SlackWebhookMessageEntity entity = SlackWebhookMessageEntity.builder()
                .channel(SLACK_WATCH_NOTIFY_CHANNEL_ID)
                .build();

        // ヘッダー
        entity.addBlock(SlackMessageBlock.builder()
                .type(BlockType.HEADER)
                .text(TextObject.builder()
                        .type(TextType.PLAIN_TEXT)
                        .text("コードスニペットのテスト")
                        .build())
                .build());

        // コードスニペットセクション
        entity.addBlock(SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .text(TextObject.builder()
                        .type(TextType.MRKDWN)
                        .text("```こんにちは```")
                        .build())
                .build());
        try {
            String ts = SlackNotifier.send(SLACK_BOT_TOKEN, entity);
            assertNotNull(ts);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void sendReplyTest() {
        try {
            // 親メッセージを送信
            SlackWebhookMessageEntity parentEntity = new SlackWebhookMessageEntity();
            parentEntity.setChannel(SLACK_WATCH_NOTIFY_CHANNEL_ID);
            parentEntity.setText("Junitテスト(sendReplyTest)実行 - 親メッセージ");
            String parentTs = SlackNotifier.send(SLACK_BOT_TOKEN, parentEntity);
            assertNotNull(parentTs);

            // 返信を送信
            SlackWebhookMessageEntity replyEntity = new SlackWebhookMessageEntity();
            replyEntity.setChannel(SLACK_WATCH_NOTIFY_CHANNEL_ID);
            replyEntity.setText("Junitテスト(sendReplyTest)実行 - 返信メッセージ");
            replyEntity.setThreadTs(parentTs);
            String replyTs = SlackNotifier.send(SLACK_BOT_TOKEN, replyEntity);
            assertNotNull(replyTs);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void sendBlockMessageTest() {
        SlackWebhookMessageEntity entity = SlackWebhookMessageEntity.builder()
                .channel(SLACK_WATCH_NOTIFY_CHANNEL_ID)
                .text("フォールバックテキスト")
                .build();

        // ヘッダー
        entity.addBlock(SlackMessageBlock.builder()
                .type(BlockType.HEADER)
                .text(TextObject.builder()
                        .type(TextType.PLAIN_TEXT)
                        .text("ブロックメッセージのテスト")
                        .build())
                .build());

        // 区切り線
        entity.addBlock(SlackMessageBlock.区切り線);

        // 本文セクション
        entity.addBlock(SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .text(TextObject.builder()
                        .type(TextType.MRKDWN)
                        .text("これはマークダウンで書かれた本文です。\n*太字*や`コード`も使えます。")
                        .build())
                .build());

        // 画像付きセクション
        entity.addBlock(SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .text(TextObject.builder()
                        .type(TextType.MRKDWN)
                        .text("画像を表示するテストです。")
                        .build())
                .accessory(ImageAccessory.builder()
                        .type(AccessoryType.IMAGE)
                        .imageUrl("https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png")
                        .altText("Google Logo")
                        .build())
                .build());

        try {
            String ts = SlackNotifier.send(SLACK_BOT_TOKEN, entity);
            assertNotNull(ts);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void sendRealityTest() throws Exception {
        String jobId = "JOBXXX";
        String jobContent = "ここに案件内容案件内容案件内容案件内容案件内容案件内容案件内容案件内容案件内容";
        String personId = "PERXXX";
        String personContent = "ここに要員内容要員内容要員内容要員内容要員内容要員内容要員内容要員内容要員内容要員内容要員内容";

        SlackWebhookMessageEntity slackMessage = SlackWebhookMessageEntity.builder()
                .channel(SLACK_WATCH_NOTIFY_CHANNEL_ID)
                .text("ウォッチ中案件・マッチング通知")
                .build();
        slackMessage.addBlock(SlackWebhookMessageEntity.SlackMessageBlock.builder()
                .type(BlockType.HEADER)
                .text(TextObject.builder().type(TextType.PLAIN_TEXT).text("ウォッチ中案件・マッチング通知").build())
                .build());
        slackMessage.addBlock(SlackWebhookMessageEntity.SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .text(TextObject.builder().type(TextType.MRKDWN).text("マッチする要員が見つかりました。\n" + TextObject.リンクテキスト("詳細はこちら", "https://google.com")).build())
                .build());
        slackMessage.addBlock(SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .text(TextObject.builder().type(TextType.MRKDWN).text("案件ID：" + jobId).build())
                .build());
        slackMessage.addBlock(SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .text(TextObject.builder().type(TextType.MRKDWN).text("要員ID：" + personId).build())
                .build());
        String ts = SlackNotifier.send(SLACK_BOT_TOKEN, slackMessage);

        SlackWebhookMessageEntity reply = SlackWebhookMessageEntity.builder()
                .channel(SLACK_WATCH_NOTIFY_CHANNEL_ID)
                .threadTs(ts)
                .text("マッチング詳細")
                .build();
        reply.addBlock(SlackWebhookMessageEntity.SlackMessageBlock.builder()
            .type(BlockType.HEADER)
            .text(TextObject.builder().type(TextType.PLAIN_TEXT).text("案件情報").build())
            .build());
        reply.addBlock(SlackWebhookMessageEntity.SlackMessageBlock.builder()
            .type(BlockType.SECTION)
            .text(TextObject.コードスニペット(jobContent))
            .build());
        ts = SlackNotifier.send(SLACK_BOT_TOKEN, reply);
        reply = SlackWebhookMessageEntity.builder()
                .channel(SLACK_WATCH_NOTIFY_CHANNEL_ID)
                .threadTs(ts)
                .build();
        reply.addBlock(SlackWebhookMessageEntity.SlackMessageBlock.builder()
            .type(BlockType.HEADER)
            .text(TextObject.builder().type(TextType.PLAIN_TEXT).text("要員情報").build())
            .build());
        reply.addBlock(SlackWebhookMessageEntity.SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .text(TextObject.コードスニペット(personContent))
                .build());
        reply.addBlock(SlackWebhookMessageEntity.SlackMessageBlock.builder()
            .type(BlockType.SECTION)
            .text(TextObject.builder().type(TextType.MRKDWN).text("スキルシート: " + TextObject.リンクテキスト("ZZ_経歴書.xlsx", "https://google.com")).build())
            .build());
        SlackNotifier.send(SLACK_BOT_TOKEN, reply);
    }
}
