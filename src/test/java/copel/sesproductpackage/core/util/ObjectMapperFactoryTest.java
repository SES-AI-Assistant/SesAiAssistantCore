package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ObjectMapperFactoryTest {

  /** 変換先のテスト用クラス. */
  static class Sample {
    public String name;
    public Integer page;
    public String other;
    public List<String> tags;
  }

  @Test
  void testDeserialize_SnakeCaseAndTrim() throws Exception {
    Sample sample =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(
            "{\"name\": \"  taro \", \"page\": 2, \"tags\": [\" a \", \"null\"]}", Sample.class);

    assertEquals("taro", sample.name);
    assertEquals(2, sample.page);
    assertEquals(java.util.Arrays.asList("a", null), sample.tags);
  }

  @Test
  void testDeserialize_NullString() throws Exception {
    Sample sample =
        ObjectMapperFactory.OBJECT_MAPPER.readValue("{\"name\": \" NULL \"}", Sample.class);

    assertNull(sample.name);
  }

  @Test
  void testDeserialize_JsonNull() throws Exception {
    Sample sample = ObjectMapperFactory.OBJECT_MAPPER.readValue("{\"name\": null}", Sample.class);

    assertNull(sample.name);
  }

  @Test
  void testDeserialize_ScalarNumber() throws Exception {
    Sample sample = ObjectMapperFactory.OBJECT_MAPPER.readValue("{\"name\": 123}", Sample.class);

    assertEquals("123", sample.name);
  }

  @Test
  void testDeserialize_ArrayForStringKeepsFollowingFields() throws Exception {
    Sample sample =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(
            "{\"name\": [\"a\", [\"b\"]], \"page\": 2, \"other\": \"z\"}", Sample.class);

    // 文字列項目の配列はnullとし、後続の項目は欠落させない
    assertNull(sample.name);
    assertEquals(2, sample.page);
    assertEquals("z", sample.other);
  }

  @Test
  void testDeserialize_ObjectForStringKeepsFollowingFields() throws Exception {
    Sample sample =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(
            "{\"name\": {\"x\": {\"y\": 1}}, \"page\": 3, \"other\": \"z\"}", Sample.class);

    assertNull(sample.name);
    assertEquals(3, sample.page);
    assertEquals("z", sample.other);
  }

  @Test
  void testConstructor() {
    assertNotNull(new ObjectMapperFactory());
  }
}
