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
  @DisplayName("getPrice: priceInMan が応相談（0）の場合、0円の Money が返り isConsultation が true となること")
  void testGetPrice_Zero() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setPriceInMan(BigDecimal.ZERO);

    Money price = schema.getPrice();
    assertNotNull(price);
    assertFalse(price.isEmpty());
    assertEquals(0L, price.toYenValue());
    assertTrue(price.isConsultation());
    assertTrue(price.isNegotiable());
    assertEquals("0万円", price.toJapaneseFormat());
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
  @DisplayName("Jackson: JSONから priceInMan が応相談（0）としてデシリアライズされること")
  void testJacksonDeserialize_ZeroPrice_Consultation() throws Exception {
    String json = "{\"name\":\"T.T\",\"priceInMan\":0}";
    PersonInfoSchema schema = objectMapper.readValue(json, PersonInfoSchema.class);

    assertNotNull(schema);
    assertEquals(new BigDecimal("0"), schema.getPriceInMan());
    assertEquals(0L, schema.getPrice().toYenValue());
    assertTrue(schema.getPrice().isConsultation());
    assertTrue(schema.getPrice().isNegotiable());
    assertEquals("0万円", schema.getPrice().toJapaneseFormat());
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
    assertTrue(schemaAnnotation.description().contains("スキル見合い・応相談・未記載は0"));
  }

  @Test
  @DisplayName("@Schema: name フィールドに架空要員・一般名詞設定禁止およびUnknown判定制約が設定されていること")
  void testSchemaAnnotation_NameFieldProhibition() throws Exception {
    Field field = PersonInfoSchema.class.getDeclaredField("name");
    Schema schemaAnnotation = field.getAnnotation(Schema.class);

    assertNotNull(schemaAnnotation);
    assertEquals("氏名", schemaAnnotation.title());
    assertTrue(schemaAnnotation.required());
    assertEquals(10, schemaAnnotation.maxLength());
    assertEquals("T.T", schemaAnnotation.example());

    String desc = schemaAnnotation.description();
    assertNotNull(desc);
    assertAll(
        "nameフィールドのdescription制約アサーション",
        () -> assertTrue(desc.contains("紹介されている要員本人の氏名またはイニシャル")),
        () -> assertTrue(desc.contains("職種名（エンジニア、プログラマー、PM等）")),
        () -> assertTrue(desc.contains("所属名（〇〇所属、弊社プロパー等）")),
        () -> assertTrue(desc.contains("一般名詞・プレースホルダー（要員様、N/A、担当者等）を設定することは厳禁")),
        () -> assertTrue(desc.contains("特定個人の名前・イニシャルが記載されていない場合は架空の要員として出力せず、必ず Unknown と判定すること")));
  }

  // ================================================
  // isValid の動作検証
  // ================================================

  @Test
  @DisplayName("isValid: 正常系（氏名・スキル・年齢・単価完備の通常要員データ）")
  void isValid_normal_withNameAndSkillsAndAge() {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName("山田太郎");
    person.setAge(30);
    person.setPriceInMan(new BigDecimal("80"));
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertTrue(person.isValid());
  }

  @Test
  @DisplayName("isValid: 正常系（年齢不明-1、単価未記載999/0であっても、氏名とスキル経歴があれば妥当と判定されること）")
  void isValid_normal_unknownAgeAndUnsetPrice() {
    // スキル見合い・単価未記載（999）、年齢不明（-1）
    PersonInfoSchema person1 = new PersonInfoSchema();
    person1.setName("鈴木二郎");
    person1.setAge(-1);
    person1.setPriceInMan(new BigDecimal("999"));
    person1.setExperiences(List.of(new Experience("React開発", "2年")));

    // 単価未記載（0）、年齢不明（-1）
    PersonInfoSchema person2 = new PersonInfoSchema();
    person2.setName("佐藤一郎");
    person2.setAge(-1);
    person2.setPriceInMan(BigDecimal.ZERO);
    person2.setExperiences(List.of(new Experience("Python開発", "1年")));

    assertAll(
        "年齢不明または単価未記載でも氏名とスキルがあれば正当な要員",
        () -> assertTrue(person1.isValid()),
        () -> assertTrue(person2.isValid()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"T.T", "Y.M", "S.K.", "A.B"})
  @DisplayName("isValid: 正常系（イニシャル表記の氏名＋スキル完備）")
  void isValid_normal_initialName(String initialName) {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName(initialName);
    person.setAge(35);
    person.setPriceInMan(new BigDecimal("70"));
    person.setExperiences(List.of(new Experience("AWS設計", "5年")));

    assertTrue(person.isValid());
  }

  @Test
  @DisplayName("isValid: 異常系（氏名がnull）")
  void isValid_abnormal_nullName() {
    PersonInfoSchema person = new PersonInfoSchema();
    person.setName(null);
    person.setAge(30);
    person.setPriceInMan(new BigDecimal("80"));
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
    person.setPriceInMan(new BigDecimal("80"));
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertFalse(person.isValid());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "unknown",
        "Unknown",
        "UNKNOWN",
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
    person.setPriceInMan(new BigDecimal("80"));
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
    person.setPriceInMan(new BigDecimal("80"));
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
    person.setPriceInMan(new BigDecimal("80"));
    person.setExperiences(List.of(new Experience("Java開発", "3年")));

    assertFalse(person.isValid());
  }

  @Test
  @DisplayName("isValid: 異常系（実体プロファイルなし：スキル経歴がnullまたは空リスト）")
  void isValid_abnormal_noExperiences() {
    // 氏名・年齢・単価があっても、スキルがnullの場合は実体なしとして除外
    PersonInfoSchema person1 = new PersonInfoSchema();
    person1.setName("山田太郎");
    person1.setAge(30);
    person1.setPriceInMan(new BigDecimal("80"));
    person1.setExperiences(null);

    // 氏名・年齢・単価があっても、スキルが空リストの場合は実体なしとして除外
    PersonInfoSchema person2 = new PersonInfoSchema();
    person2.setName("佐藤一郎");
    person2.setAge(28);
    person2.setPriceInMan(new BigDecimal("60"));
    person2.setExperiences(List.of());

    // 年齢不明・単価未記載かつスキルがnullの場合も除外
    PersonInfoSchema person3 = new PersonInfoSchema();
    person3.setName("鈴木二郎");
    person3.setAge(-1);
    person3.setPriceInMan(new BigDecimal("999"));
    person3.setExperiences(null);

    assertAll(
        "スキル経歴が存在しない場合は実体なしとして不正と判定されること",
        () -> assertFalse(person1.isValid()),
        () -> assertFalse(person2.isValid()),
        () -> assertFalse(person3.isValid()));
  }
}
