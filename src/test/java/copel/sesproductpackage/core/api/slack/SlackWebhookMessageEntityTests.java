package copel.sesproductpackage.core.api.slack;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.AccessoryType;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.BlockType;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.ImageAccessory;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.SlackMessageBlock;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.TextObject;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.TextType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackWebhookMessageEntityTests {

  @Test
  void testImageAccessory() {
    ImageAccessory acc =
        ImageAccessory.builder()
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
    ImageAccessory acc =
        ImageAccessory.builder().type(AccessoryType.IMAGE).imageUrl("url").altText("alt").build();
    assertNotNull(acc.toString());

    // SlackMessageBlock
    SlackMessageBlock block =
        SlackMessageBlock.builder()
            .type(BlockType.SECTION)
            .blockId("B1")
            .text(TextObject.builder().type(TextType.PLAIN_TEXT).text("hi").build())
            .fields(new ArrayList<>())
            .accessory(acc)
            .build();
    block.addTextObject(new TextObject(TextType.MRKDWN, "field"));
    assertNotNull(block.toString());

    // SlackWebhookMessageEntity
    SlackWebhookMessageEntity entity =
        SlackWebhookMessageEntity.builder()
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
  void testApiResponseEqualsThoroughly() throws Exception {
    SlackWebhookMessageEntity.SlackApiResponse res1 =
        new SlackWebhookMessageEntity.SlackApiResponse();
    SlackWebhookMessageEntity.SlackApiResponse res2 =
        new SlackWebhookMessageEntity.SlackApiResponse();
    assertEquals(res1, res2);

    res1.setOk(true);
    assertNotEquals(res1, res2);
    res2.setOk(true);
    assertEquals(res1, res2);

    res1.setTs("t");
    assertNotEquals(res1, res2);
    res2.setTs("t");
    assertEquals(res1, res2);

    res1.setError("e");
    assertNotEquals(res1, res2);
    res2.setError("e");
    assertEquals(res1, res2);

    Class<?> messageClass =
        Class.forName(
            "copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity$SlackApiResponse$Message");
    java.lang.reflect.Constructor<?> noArgMsg = messageClass.getDeclaredConstructor();
    noArgMsg.setAccessible(true);
    Object m1 = noArgMsg.newInstance();
    Object m2 = noArgMsg.newInstance();

    java.lang.reflect.Field messageField =
        SlackWebhookMessageEntity.SlackApiResponse.class.getDeclaredField("message");
    messageField.setAccessible(true);

    messageField.set(res1, m1);
    assertNotEquals(res1, res2);
    messageField.set(res2, m2);
    assertEquals(res1, res2);

    // Test Message equals field by field
    String[] fields = {"text", "user", "botId", "type", "ts", "appId"};
    for (String field : fields) {
      String setterName = "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
      java.lang.reflect.Method setter = messageClass.getMethod(setterName, String.class);

      setter.invoke(m1, "val");
      assertNotEquals(m1, m2);
      setter.invoke(m2, "val");
      assertEquals(m1, m2);

      setter.invoke(m1, (String) null);
      assertNotEquals(m1, m2);
      setter.invoke(m2, (String) null);
      assertEquals(m1, m2);
    }
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
  void testEqualsThoroughly() {
    // SlackWebhookMessageEntity
    SlackWebhookMessageEntity e1 = new SlackWebhookMessageEntity("c", "t", "ts", new ArrayList<>());
    SlackWebhookMessageEntity e2 = new SlackWebhookMessageEntity("c", "t", "ts", new ArrayList<>());
    assertEquals(e1, e2);

    e1.setChannel(null);
    assertNotEquals(e1, e2);
    e2.setChannel(null);
    assertEquals(e1, e2);

    e1.setText(null);
    assertNotEquals(e1, e2);
    e2.setText(null);
    assertEquals(e1, e2);

    e1.setThreadTs(null);
    assertNotEquals(e1, e2);
    e2.setThreadTs(null);
    assertEquals(e1, e2);

    e1.setBlocks(null);
    assertNotEquals(e1, e2);
    e2.setBlocks(null);
    assertEquals(e1, e2);

    // SlackMessageBlock
    SlackMessageBlock b1 = new SlackMessageBlock(BlockType.SECTION, "b", null, null, null);
    SlackMessageBlock b2 = new SlackMessageBlock(BlockType.SECTION, "b", null, null, null);
    assertEquals(b1, b2);

    b1.setType(null);
    assertNotEquals(b1, b2);
    b2.setType(null);
    assertEquals(b1, b2);

    b1.setBlockId(null);
    assertNotEquals(b1, b2);
    b2.setBlockId(null);
    assertEquals(b1, b2);

    b1.setText(new TextObject());
    assertNotEquals(b1, b2);
    b2.setText(new TextObject());
    assertEquals(b1, b2);

    b1.setFields(new ArrayList<>());
    assertNotEquals(b1, b2);
    b2.setFields(new ArrayList<>());
    assertEquals(b1, b2);

    b1.setAccessory(new ImageAccessory());
    assertNotEquals(b1, b2);
    b2.setAccessory(new ImageAccessory());
    assertEquals(b1, b2);

    // TextObject
    TextObject t1 = new TextObject(TextType.MRKDWN, "t");
    TextObject t2 = new TextObject(TextType.MRKDWN, "t");
    assertEquals(t1, t2);

    t1.setType(null);
    assertNotEquals(t1, t2);
    t2.setType(null);
    assertEquals(t1, t2);

    t1.setText(null);
    assertNotEquals(t1, t2);
    t2.setText(null);
    assertEquals(t1, t2);

    // ImageAccessory
    ImageAccessory i1 = new ImageAccessory(AccessoryType.IMAGE, "u", "a");
    ImageAccessory i2 = new ImageAccessory(AccessoryType.IMAGE, "u", "a");
    assertEquals(i1, i2);

    i1.setType(null);
    assertNotEquals(i1, i2);
    i2.setType(null);
    assertEquals(i1, i2);

    i1.setImageUrl(null);
    assertNotEquals(i1, i2);
    i2.setImageUrl(null);
    assertEquals(i1, i2);

    i1.setAltText(null);
    assertNotEquals(i1, i2);
    i2.setAltText(null);
    assertEquals(i1, i2);
  }

  @Test
  void testAddTextObjectEdgeCases() {
    SlackMessageBlock block = new SlackMessageBlock();
    assertNull(block.getFields());

    TextObject t1 = new TextObject(TextType.PLAIN_TEXT, "H1");
    block.addTextObject(t1);
    assertEquals(1, block.getFields().size());

    TextObject t2 = new TextObject(TextType.MRKDWN, "H2");
    block.addTextObject(t2); // Branch where fields is not null
    assertEquals(2, block.getFields().size());
  }

  @Test
  void testEnumsExtra() {
    // Ensure all enum constants are touched
    for (BlockType b : BlockType.values()) {
      assertNotNull(b.name());
    }
    for (TextType t : TextType.values()) {
      assertNotNull(t.name());
    }
    for (AccessoryType a : AccessoryType.values()) {
      assertNotNull(a.name());
    }
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
  void testConstructorsAndLombokExhaustive() throws Exception {
    // SlackWebhookMessageEntity
    SlackWebhookMessageEntity eEmpty = new SlackWebhookMessageEntity();
    assertNotNull(eEmpty);

    List<SlackMessageBlock> blocks = new ArrayList<>();
    SlackWebhookMessageEntity eFull =
        new SlackWebhookMessageEntity("channel", "text", "ts", blocks);
    assertEquals("channel", eFull.getChannel());

    // SlackMessageBlock
    SlackMessageBlock bEmpty = new SlackMessageBlock();
    assertNotNull(bEmpty);
    SlackMessageBlock bFull = new SlackMessageBlock(BlockType.SECTION, "bid", null, null, null);
    assertEquals(BlockType.SECTION, bFull.getType());

    // TextObject
    TextObject tEmpty = new TextObject();
    assertNotNull(tEmpty);
    TextObject tFull = new TextObject(TextType.PLAIN_TEXT, "txt");
    assertEquals(TextType.PLAIN_TEXT, tFull.getType());

    // ImageAccessory
    ImageAccessory aEmpty = new ImageAccessory();
    assertNotNull(aEmpty);
    ImageAccessory aFull = new ImageAccessory(AccessoryType.IMAGE, "url", "alt");
    assertEquals(AccessoryType.IMAGE, aFull.getType());

    // Message (private)
    Class<?> messageClass =
        Class.forName(
            "copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity$SlackApiResponse$Message");
    java.lang.reflect.Constructor<?> noArgMsg = messageClass.getDeclaredConstructor();
    noArgMsg.setAccessible(true);
    Object m1 = noArgMsg.newInstance();

    assertTrue(m1.toString().contains("Message"));
    assertEquals(m1, m1);
  }

  @Test
  void testBuildersExhaustive() {
    assertNotNull(SlackWebhookMessageEntity.builder().toString());
    assertNotNull(SlackMessageBlock.builder().toString());
    assertNotNull(TextObject.builder().toString());
    assertNotNull(ImageAccessory.builder().toString());

    SlackWebhookMessageEntity e =
        SlackWebhookMessageEntity.builder()
            .channel("c")
            .text("t")
            .threadTs("ts")
            .blocks(new ArrayList<>())
            .build();
    assertNotNull(e);

    SlackMessageBlock b =
        SlackMessageBlock.builder()
            .type(BlockType.SECTION)
            .blockId("b")
            .text(null)
            .fields(null)
            .accessory(null)
            .build();
    assertNotNull(b);

    TextObject t = TextObject.builder().type(TextType.PLAIN_TEXT).text("t").build();
    assertNotNull(t);

    ImageAccessory a =
        ImageAccessory.builder().type(AccessoryType.IMAGE).imageUrl("u").altText("a").build();
    assertNotNull(a);
  }

  @Test
  void testCanEqual() {
    SlackWebhookMessageEntity e = new SlackWebhookMessageEntity();
    assertTrue(e.canEqual(new SlackWebhookMessageEntity()));
    assertFalse(e.canEqual(new Object()));

    SlackMessageBlock b = new SlackMessageBlock();
    assertTrue(b.canEqual(new SlackMessageBlock()));

    TextObject t = new TextObject();
    assertTrue(t.canEqual(new TextObject()));

    ImageAccessory a = new ImageAccessory();
    assertTrue(a.canEqual(new ImageAccessory()));
  }

  @Test
  void testEqualsDifferentTypes() throws Exception {
    Object other = new Object();
    SlackWebhookMessageEntity e = new SlackWebhookMessageEntity();
    assertNotEquals(e, other);
    assertNotEquals(e, null);
    assertTrue(e.canEqual(e));
    assertFalse(e.canEqual(other));

    SlackMessageBlock b = new SlackMessageBlock();
    assertNotEquals(b, other);
    assertNotEquals(b, null);
    assertTrue(b.canEqual(b));
    assertFalse(b.canEqual(other));

    TextObject t = new TextObject();
    assertNotEquals(t, other);
    assertNotEquals(t, null);
    assertTrue(t.canEqual(t));
    assertFalse(t.canEqual(other));

    ImageAccessory a = new ImageAccessory();
    assertNotEquals(a, other);
    assertNotEquals(a, null);
    assertTrue(a.canEqual(a));
    assertFalse(a.canEqual(other));

    SlackWebhookMessageEntity.SlackApiResponse res =
        new SlackWebhookMessageEntity.SlackApiResponse();
    assertNotEquals(res, other);
    assertNotEquals(res, null);
    assertTrue(res.canEqual(res));
    assertFalse(res.canEqual(other));

    Class<?> messageClass =
        Class.forName(
            "copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity$SlackApiResponse$Message");
    java.lang.reflect.Constructor<?> constructor = messageClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object message = constructor.newInstance();
    assertNotEquals(message, other);
    assertNotEquals(message, null);
  }

  @Test
  void testHashCodeExhaustive() throws Exception {
    assertNotEquals(0, new SlackWebhookMessageEntity().hashCode());
    assertNotEquals(0, new SlackMessageBlock().hashCode());
    assertNotEquals(0, new TextObject().hashCode());
    assertNotEquals(0, new ImageAccessory().hashCode());
    assertNotEquals(0, new SlackWebhookMessageEntity.SlackApiResponse().hashCode());

    Class<?> messageClass =
        Class.forName(
            "copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity$SlackApiResponse$Message");
    java.lang.reflect.Constructor<?> constructor = messageClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object message = constructor.newInstance();
    assertNotEquals(0, message.hashCode());
  }

  @Test
  void testBuilderWithDefaults() {
    SlackWebhookMessageEntity e1 = SlackWebhookMessageEntity.builder().build();
    assertNotNull(e1.getBlocks());

    SlackWebhookMessageEntity e2 = SlackWebhookMessageEntity.builder().blocks(null).build();
    assertNull(e2.getBlocks());
  }

  @Test
  void testEqualsReflexiveAndSymmetric() {
    SlackWebhookMessageEntity e1 = new SlackWebhookMessageEntity();
    assertEquals(e1, e1);
    assertNotEquals(e1, null);
    assertNotEquals(e1, "not an entity");
  }
}
