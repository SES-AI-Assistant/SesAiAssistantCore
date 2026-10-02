package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.api.gpt.entity.PersonInfoSchema.Experience;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  // ================================================
  // isValid の動作検証
  // ================================================

  @Test
  @DisplayName("isValid: 正常系（氏名・スキル・年齢完備の要員データ）")
  void isValid_normal_withNameAndSkillsAndAge() {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName("山田太郎");
    person.setAge(30);
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertTrue(person.isValid());
  }

  @Test
  @DisplayName("isValid: 正常系（スキルなし・年齢のみ判明している要員データ）")
  void isValid_normal_withNameAndAgeOnly() {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName("佐藤一郎");
    person.setAge(28);
    person.setExperiences(null);

    assertTrue(person.isValid());
  }

  @Test
  @DisplayName("isValid: 正常系（年齢不明・スキルありの要員データ）")
  void isValid_normal_withNameAndSkillsOnly() {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName("鈴木二郎");
    person.setAge(-1);
    person.setExperiences(List.of(new Experience("React開発", "2年")));

    assertTrue(person.isValid());
  }

  @Test
  @DisplayName("isValid: 正常系（イニシャル表記の氏名）")
  void isValid_normal_initialName() {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName("T.T");
    person.setAge(35);
    person.setExperiences(List.of(new Experience("AWS設計", "5年")));

    assertTrue(person.isValid());
  }

  @Test
  @DisplayName("isValid: 異常系（氏名がnull）")
  void isValid_abnormal_nullName() {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName(null);
    person.setAge(30);
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertFalse(person.isValid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   ", "　", "\t\n"})
  @DisplayName("isValid: 異常系（氏名が空文字・空白文字）")
  void isValid_abnormal_blankName(String invalidName) {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName(invalidName);
    person.setAge(30);
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertFalse(person.isValid());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "n/a",
        "na",
        "none",
        "N/A",
        "None",
        "不明",
        "なし",
        "未定",
        "要員様",
        "要員",
        "エンジニア",
        "技術者",
        "プロパー",
        "メンバー",
        "様",
        "さん",
        "担当",
        "営業"
      })
  @DisplayName("isValid: 異常系（架空要員名・プレースホルダー）")
  void isValid_abnormal_placeholderNames(String placeholderName) {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName(placeholderName);
    person.setAge(30);
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertFalse(person.isValid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"---", "...", "★", "【】", "!?#$"})
  @DisplayName("isValid: 異常系（記号のみの氏名）")
  void isValid_abnormal_symbolsOnly(String symbol) {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName(symbol);
    person.setAge(30);
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertFalse(person.isValid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"12345", "090-1234-5678", "001", "#123"})
  @DisplayName("isValid: 異常系（数字のみ・数字と記号のみの氏名）")
  void isValid_abnormal_digitsOnly(String digits) {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName(digits);
    person.setAge(30);
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertFalse(person.isValid());
  }

  @Test
  @DisplayName("isValid: 異常系（実体プロファイルなし：経験なし かつ 年齢不明）")
  void isValid_abnormal_noSubstance() {
    PersonInfoSchema person1 = new PersonInfoSchema();
    person1.setName("山田太郎");
    person1.setAge(-1);
    person1.setExperiences(null);

    PersonInfoSchema person2 = new PersonInfoSchema();
    person2.setName("山田太郎");
    person2.setAge(0);
    person2.setExperiences(List.of());

    assertAll(
        () -> assertFalse(person1.isValid()),
        () -> assertFalse(person2.isValid()));
  }

  // ================================================
  // normalizePrice の動作検証
  // ================================================

  @Test
  @DisplayName("normalizePrice: 単位誤認値（45円、55円など）の自己修復正規化")
  void normalizePrice_repairsLowPrices() {
    // 45円 (priceInMan = 0.0045) -> 450,000円
    PersonInfoSchema person45 = new PersonInfoSchema();
    person45.setPriceInMan(new BigDecimal("0.0045"));
    Money repaired45 = person45.normalizePrice();
    assertEquals(450000L, repaired45.toYenValue());
    assertEquals(new BigDecimal("45"), person45.getPriceInMan());

    // 55円 (priceInMan = 0.0055) -> 550,000円
    PersonInfoSchema person55 = new PersonInfoSchema();
    person55.setPriceInMan(new BigDecimal("0.0055"));
    Money repaired55 = person55.normalizePrice();
    assertEquals(550000L, repaired55.toYenValue());
    assertEquals(new BigDecimal("55"), person55.getPriceInMan());

    // 通常の単価（80万円 = 800,000円）はそのまま
    PersonInfoSchema person80 = new PersonInfoSchema();
    person80.setPriceInMan(new BigDecimal("80"));
    Money normal80 = person80.normalizePrice();
    assertEquals(800000L, normal80.toYenValue());
    assertEquals(new BigDecimal("80"), person80.getPriceInMan());

    // null または空の場合は安全に空Moneyを返却
    PersonInfoSchema personNull = new PersonInfoSchema();
    personNull.setPriceInMan(null);
    Money emptyPrice = personNull.normalizePrice();
    assertNotNull(emptyPrice);
    assertTrue(emptyPrice.isEmpty());
  }
}
