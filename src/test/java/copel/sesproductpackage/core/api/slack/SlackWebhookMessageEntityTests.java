package copel.sesproductpackage.core.api.slack;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.AccessoryType;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.ImageAccessory;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.BlockType;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.SlackMessageBlock;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.TextObject;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.TextType;

class SlackWebhookMessageEntityTests {

    @Test
    void testImageAccessory() {
        ImageAccessory acc = ImageAccessory.builder()
                .type(AccessoryType.IMAGE)
                .imageUrl("http://img")
                .altText("alt")
                .build();
        assertEquals(AccessoryType.IMAGE, acc.getType());
        assertEquals("http://img", acc.getImageUrl());
        assertEquals("alt", acc.getAltText());
    }

    @Test
    void testApiResponse() {
        SlackWebhookMessageEntity.SlackApiResponse res = new SlackWebhookMessageEntity.SlackApiResponse();
        res.setOk(true);
        res.setTs("123");
        res.setError("err");
        assertTrue(res.isOk());
        assertEquals("123", res.getTs());
        assertEquals("err", res.getError());
        
        // Message クラスが非公開の場合はリフレクション経由でセットするか、
        // あるいは setter 自体が外部から使えないならテスト対象から外す
    }

    @Test
    void testEntity() throws Exception {
        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        entity.setChannel("C1");
        entity.setText("text");
        entity.setThreadTs("ts");
        
        assertEquals("C1", entity.getChannel());
        assertEquals("text", entity.getText());
        assertEquals("ts", entity.getThreadTs());
        
        assertNotNull(entity.toJson());
    }

    @Test
    void testBuilder() {
        SlackWebhookMessageEntity entity = SlackWebhookMessageEntity.builder()
                .channel("C1")
                .text("text")
                .build();
        assertEquals("C1", entity.getChannel());
    }

    @Test
    void testBlocks() throws Exception {
        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        SlackMessageBlock block = SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .text(TextObject.builder().type(TextType.MRKDWN).text("hi").build())
                .build();
        entity.addBlock(block);
        
        assertEquals(1, entity.getBlocks().size());
        assertTrue(entity.toJson().contains("hi"));
    }
}
