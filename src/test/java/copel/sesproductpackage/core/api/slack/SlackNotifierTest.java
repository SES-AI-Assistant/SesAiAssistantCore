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
     * UT用のSlackチャンネルIDでテストを実施する.
     */
    private static final String SLACK_JUNIT_TEST_CHANNEL_ID = Properties.get("SLACK_JUNIT_TEST_CHANNEL_ID");

    @Test
    void sendByWebhookTest() {
        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        entity.setText("Junitテスト(sendByWebhookTest)実行");

        try {
            SlackNotifier.sendByWebhook(entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void sendMessageTest() {
        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        entity.setChannel(SLACK_JUNIT_TEST_CHANNEL_ID);
        entity.setText("Junitテスト(sendMessageTest)実行");

        try {
            String ts = SlackNotifier.send(entity);
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
            parentEntity.setChannel(SLACK_JUNIT_TEST_CHANNEL_ID);
            parentEntity.setText("Junitテスト(sendReplyTest)実行 - 親メッセージ");
            String parentTs = SlackNotifier.send(parentEntity);
            assertNotNull(parentTs);

            // 返信を送信
            SlackWebhookMessageEntity replyEntity = new SlackWebhookMessageEntity();
            replyEntity.setChannel(SLACK_JUNIT_TEST_CHANNEL_ID);
            replyEntity.setText("Junitテスト(sendReplyTest)実行 - 返信メッセージ");
            replyEntity.setThreadTs(parentTs);
            String replyTs = SlackNotifier.send(replyEntity);
            assertNotNull(replyTs);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void sendBlockMessageTest() {
        SlackWebhookMessageEntity entity = SlackWebhookMessageEntity.builder()
                .channel(SLACK_JUNIT_TEST_CHANNEL_ID)
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
            String ts = SlackNotifier.send(entity);
            assertNotNull(ts);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
