package copel.sesproductpackage.core.api.slack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
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
    void testFullBuilders() throws Exception {
        // ImageAccessory
        ImageAccessory acc = ImageAccessory.builder()
                .type(AccessoryType.IMAGE)
                .imageUrl("url")
                .altText("alt")
                .build();
        assertNotNull(acc.toString());
        
        // SlackMessageBlock
        SlackMessageBlock block = SlackMessageBlock.builder()
                .type(BlockType.SECTION)
                .blockId("B1")
                .text(TextObject.builder().type(TextType.PLAIN_TEXT).text("hi").build())
                .fields(new ArrayList<>())
                .accessory(acc)
                .build();
        block.addTextObject(new TextObject(TextType.MRKDWN, "field"));
        assertNotNull(block.toString());
        
        // SlackWebhookMessageEntity
        SlackWebhookMessageEntity entity = SlackWebhookMessageEntity.builder()
                .channel("C1")
                .text("T1")
                .threadTs("TS1")
                .blocks(new ArrayList<>())
                .build();
        entity.addBlock(block);
        assertNotNull(entity.toString());
        assertNotNull(entity.toJson());
        
        // TextObject
        TextObject text = TextObject.builder().type(TextType.MRKDWN).text("txt").build();
        assertNotNull(text.toString());
    }

    @Test
    void testApiResponse() throws Exception {
        SlackWebhookMessageEntity.SlackApiResponse res = new SlackWebhookMessageEntity.SlackApiResponse();
        res.setOk(true);
        res.setTs("123");
        res.setError("err");
        assertTrue(res.isOk());
        assertEquals("123", res.getTs());
        assertEquals("err", res.getError());
        
        // Message class is private but has public Lombok setters/getters
        // We can access it via reflection if we want to cover its methods
        Class<?> messageClass = Class.forName("copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity$SlackApiResponse$Message");
        java.lang.reflect.Constructor<?> constructor = messageClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object message = constructor.newInstance();
        
        java.lang.reflect.Method setText = messageClass.getMethod("setText", String.class);
        setText.invoke(message, "hello");
        java.lang.reflect.Method getText = messageClass.getMethod("getText");
        assertEquals("hello", getText.invoke(message));
        
        java.lang.reflect.Method setTs = messageClass.getMethod("setTs", String.class);
        setTs.invoke(message, "456");
        
        java.lang.reflect.Method setUser = messageClass.getMethod("setUser", String.class);
        setUser.invoke(message, "U1");

        java.lang.reflect.Method setBotId = messageClass.getMethod("setBotId", String.class);
        setBotId.invoke(message, "B1");

        java.lang.reflect.Method setType = messageClass.getMethod("setType", String.class);
        setType.invoke(message, "T1");

        java.lang.reflect.Method setAppId = messageClass.getMethod("setAppId", String.class);
        setAppId.invoke(message, "A1");

        java.lang.reflect.Field messageField = SlackWebhookMessageEntity.SlackApiResponse.class.getDeclaredField("message");
        messageField.setAccessible(true);
        messageField.set(res, message);
        
        assertNotNull(res.getMessage());
    }

    @Test
    void testMessageBlock() {
        SlackMessageBlock block = new SlackMessageBlock();
        block.setType(BlockType.SECTION);
        block.setBlockId("B1");
        
        TextObject text = new TextObject(TextType.PLAIN_TEXT, "hello");
        block.setText(text);
        
        block.addTextObject(text);
        assertEquals(1, block.getFields().size());
        
        assertNotNull(SlackMessageBlock.区切り線);
        assertEquals(BlockType.DIVIDER, SlackMessageBlock.区切り線.getType());
    }

    @Test
    void testTextObjectUtils() {
        TextObject snippet = TextObject.コードスニペット("code");
        assertEquals("```code```", snippet.getText());
        assertEquals(TextType.MRKDWN, snippet.getType());
        
        String link = TextObject.リンクテキスト("label", "url");
        assertEquals("<url | label>", link);
    }

    @Test
    void testEnums() {
        for (BlockType t : BlockType.values()) assertNotNull(BlockType.valueOf(t.name()));
        for (TextType t : TextType.values()) assertNotNull(TextType.valueOf(t.name()));
        for (AccessoryType t : AccessoryType.values()) assertNotNull(AccessoryType.valueOf(t.name()));
    }

    @Test
    void testLombokMethods() {
        SlackWebhookMessageEntity e1 = new SlackWebhookMessageEntity();
        SlackWebhookMessageEntity e2 = new SlackWebhookMessageEntity();
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotNull(e1.toString());
        
        SlackMessageBlock b1 = new SlackMessageBlock();
        SlackMessageBlock b2 = new SlackMessageBlock();
        assertEquals(b1, b2);
        assertNotNull(b1.toString());
        
        TextObject t1 = new TextObject();
        TextObject t2 = new TextObject();
        assertEquals(t1, t2);
        assertNotNull(t1.toString());
        
        ImageAccessory a1 = new ImageAccessory();
        ImageAccessory a2 = new ImageAccessory();
        assertEquals(a1, a2);
        assertNotNull(a1.toString());
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
