package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PersonInfoSchema の単体テスト.
 *
 * @author Copel Co., Ltd.
 */
class PersonInfoSchemaTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // ================================================
  // priceInMan および getPrice() の動作検証
  // ================================================

  @Test
  @DisplayName("getPrice: priceInMan が通常万円数値（80）の場合、800,000円の Money が返ること")
  void testGetPrice_NormalMan() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setPriceInMan(new BigDecimal("80"));

    Money price = schema.getPrice();
    assertNotNull(price);
    assertFalse(price.isEmpty());
    assertEquals(800000L, price.toYenValue());
    assertEquals("80万円", price.toJapaneseFormat());
  }

  @Test
  @DisplayName("getPrice: priceInMan が小数万円数値（65.5）の場合、655,000円の Money が返ること")
  void testGetPrice_DecimalMan() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setPriceInMan(new BigDecimal("65.5"));

    Money price = schema.getPrice();
    assertNotNull(price);
    assertFalse(price.isEmpty());
    assertEquals(655000L, price.toYenValue());
  }

  @Test
  @DisplayName("getPrice: priceInMan がスキル見合い仕様値（999）の場合、isNegotiable が true となること")
  void testGetPrice_Negotiable() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setPriceInMan(new BigDecimal("999"));

    Money price = schema.getPrice();
    assertNotNull(price);
    assertFalse(price.isEmpty());
    assertEquals(9990000L, price.toYenValue());
    assertTrue(price.isNegotiable());
  }

  @Test
  @DisplayName("getPrice: priceInMan が null の場合、Money.empty が返ること")
  void testGetPrice_Null() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setPriceInMan(null);

    Money price = schema.getPrice();
    assertNotNull(price);
    assertTrue(price.isEmpty());
    assertNull(price.getValue());
  }

  // ================================================
  // Jackson デシリアライズ検証
  // ================================================

  @Test
  @DisplayName("Jackson: JSONから priceInMan が数値（80）としてデシリアライズされ、getPrice で円換算されること")
  void testJacksonDeserialize_Number() throws Exception {
    String json = "{\"name\":\"T.T\",\"priceInMan\":80}";
    PersonInfoSchema schema = objectMapper.readValue(json, PersonInfoSchema.class);

    assertNotNull(schema);
    assertEquals(new BigDecimal("80"), schema.getPriceInMan());
    assertEquals(800000L, schema.getPrice().toYenValue());
  }

  @Test
  @DisplayName("Jackson: JSONから priceInMan がスキル見合い（999）としてデシリアライズされること")
  void testJacksonDeserialize_Negotiable() throws Exception {
    String json = "{\"name\":\"T.T\",\"priceInMan\":999}";
    PersonInfoSchema schema = objectMapper.readValue(json, PersonInfoSchema.class);

    assertNotNull(schema);
    assertEquals(new BigDecimal("999"), schema.getPriceInMan());
    assertTrue(schema.getPrice().isNegotiable());
  }

  @Test
  @DisplayName("Jackson: JSONの priceInMan が null の場合、空の Money が返ること")
  void testJacksonDeserialize_NullPrice() throws Exception {
    String json = "{\"name\":\"T.T\",\"priceInMan\":null}";
    PersonInfoSchema schema = objectMapper.readValue(json, PersonInfoSchema.class);

    assertNotNull(schema);
    assertNull(schema.getPriceInMan());
    assertTrue(schema.getPrice().isEmpty());
  }

  @Test
  @DisplayName("Jackson: JSONに priceInMan キーが存在しない場合、空の Money が返ること")
  void testJacksonDeserialize_MissingKey() throws Exception {
    String json = "{\"name\":\"T.T\"}";
    PersonInfoSchema schema = objectMapper.readValue(json, PersonInfoSchema.class);

    assertNotNull(schema);
    assertNull(schema.getPriceInMan());
    assertTrue(schema.getPrice().isEmpty());
  }

  // ================================================
  // toSummaryText の検証
  // ================================================

  @Test
  @DisplayName("toSummaryText: 単価が概要文に正しくフォーマットされて含まれること")
  void testToSummaryText_ContainsPrice() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setName("T.T");
    schema.setGender(Gender.Man);
    schema.setPriceInMan(new BigDecimal("80"));

    String summaryText = schema.toSummaryText();
    assertTrue(summaryText.contains("■単価: 80万円"));
  }

  // ================================================
  // @Schema アノテーション定義の検証
  // ================================================

  @Test
  @DisplayName("@Schema: priceInMan に期待通りのアノテーションが設定されていること")
  void testSchemaAnnotation() throws Exception {
    Field field = PersonInfoSchema.class.getDeclaredField("priceInMan");
    Schema schemaAnnotation = field.getAnnotation(Schema.class);

    assertNotNull(schemaAnnotation);
    assertEquals("希望単価（万円）", schemaAnnotation.title());
    assertTrue(schemaAnnotation.required());
    assertEquals("number", schemaAnnotation.type());
    assertEquals("80", schemaAnnotation.example());
    assertTrue(schemaAnnotation.description().contains("月額希望単価を万円単位の数値で設定"));
  }
}
