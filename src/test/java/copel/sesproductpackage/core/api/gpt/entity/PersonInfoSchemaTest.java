package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
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

  // ================================================
  // 経歴（経験月数）の出力検証
  // ================================================

  @Test
  @DisplayName("toSummaryText/getExperiencesAsString: 経験月数は年月表記で出力され、未設定の場合はスキル名のみ出力されること")
  void testExperiencesWithDurationMonths() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setName("H.M");
    schema.setGender(Gender.Woman);
    schema.setExperiences(
        List.of(
            new PersonInfoSchema.Experience("PMO経験", null),
            new PersonInfoSchema.Experience("Java", 36)));

    assertEquals("PMO経験\nJava: 3年", schema.getExperiencesAsString());

    String summaryText = schema.toSummaryText();
    assertTrue(summaryText.contains("・PMO経験\n"));
    assertTrue(summaryText.contains("・Java: 3年"));
  }

  @Test
  @DisplayName("JSON: 本番のObjectMapper（SNAKE_CASE）で duration_months が数値としてデシリアライズされること")
  void testDeserializeDurationMonths() throws Exception {
    PersonInfoSchema schema =
        ObjectMapperFactory.OBJECT_MAPPER.readValue(
            "{\"experiences\":[{\"perspective\":\"Java\",\"duration_months\":18},{\"perspective\":\"PMO\"}]}",
            PersonInfoSchema.class);

    assertEquals(18, schema.getExperiences().get(0).getDurationMonths());
    assertNull(schema.getExperiences().get(1).getDurationMonths());
    assertEquals("Java: 1年6ヶ月\nPMO", schema.getExperiencesAsString());
  }

  // ================================================
  // 推測抑止: 記載の無い項目の捏造を誘発する最低件数指定が無いこと
  // ================================================

  @Test
  @DisplayName("Schema: リスト項目に最低件数（minItems）が設定されていないこと")
  void testSchema_NoMinItems() throws NoSuchFieldException {
    for (String fieldName : List.of("experiences")) {
      Schema schema =
          PersonInfoSchema.class.getDeclaredField(fieldName).getAnnotation(Schema.class);
      assertEquals(-1, schema.minItems(), fieldName);
    }
  }

  // ================================================
  // getStartDateAsOriginalDateTime の検証
  // ================================================

  @Test
  @DisplayName("getStartDateAsOriginalDateTime: rawContent に「即日」を含む場合、当月が返ること")
  void testGetStartDateAsOriginalDateTime_ImmediateInRawContent() {
    OriginalDateTime now = new OriginalDateTime();
    int currentYear = now.toLocalDate().getYear();
    int currentMonth = now.toLocalDate().getMonthValue();

    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setStartYearMonth((currentYear + 1) + "/10");

    OriginalDateTime result = schema.getStartDateAsOriginalDateTime("要員情報です。【即日】稼働可能");
    assertNotNull(result);
    assertEquals(currentYear, result.toLocalDate().getYear());
    assertEquals(currentMonth, result.toLocalDate().getMonthValue());
  }

  @Test
  @DisplayName(
      "getStartDateAsOriginalDateTime: startYearMonth が過去年月（2024/06, 2023/11）の場合、当月へフォールバックすること")
  void testGetStartDateAsOriginalDateTime_PastYearMonth_FallbackToCurrent() {
    OriginalDateTime now = new OriginalDateTime();
    int currentYear = now.toLocalDate().getYear();
    int currentMonth = now.toLocalDate().getMonthValue();

    PersonInfoSchema schema1 = new PersonInfoSchema();
    schema1.setStartYearMonth("2024/06");

    OriginalDateTime result1 = schema1.getStartDateAsOriginalDateTime("通常要員本文");
    assertNotNull(result1);
    assertEquals(currentYear, result1.toLocalDate().getYear());
    assertEquals(currentMonth, result1.toLocalDate().getMonthValue());

    PersonInfoSchema schema2 = new PersonInfoSchema();
    schema2.setStartYearMonth("2023/11");

    OriginalDateTime result2 = schema2.getStartDateAsOriginalDateTime();
    assertNotNull(result2);
    assertEquals(currentYear, result2.toLocalDate().getYear());
    assertEquals(currentMonth, result2.toLocalDate().getMonthValue());
  }

  @Test
  @DisplayName("getStartDateAsOriginalDateTime: startYearMonth が未来年月の場合、指定年月がそのまま返ること")
  void testGetStartDateAsOriginalDateTime_FutureYearMonth() {
    OriginalDateTime now = new OriginalDateTime();
    int currentYear = now.toLocalDate().getYear();
    int futureYear = currentYear + 1;

    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setStartYearMonth(futureYear + "/05");

    OriginalDateTime result = schema.getStartDateAsOriginalDateTime("通常要員本文");
    assertNotNull(result);
    assertEquals(futureYear, result.toLocalDate().getYear());
    assertEquals(5, result.toLocalDate().getMonthValue());
  }

  @Test
  @DisplayName("getStartDateAsOriginalDateTime: startYearMonth が null または空文字の場合、当月が返ること")
  void testGetStartDateAsOriginalDateTime_NullOrBlank() {
    OriginalDateTime now = new OriginalDateTime();
    int currentYear = now.toLocalDate().getYear();
    int currentMonth = now.toLocalDate().getMonthValue();

    PersonInfoSchema schemaNull = new PersonInfoSchema();
    schemaNull.setStartYearMonth(null);
    OriginalDateTime resultNull = schemaNull.getStartDateAsOriginalDateTime();
    assertNotNull(resultNull);
    assertEquals(currentYear, resultNull.toLocalDate().getYear());
    assertEquals(currentMonth, resultNull.toLocalDate().getMonthValue());

    PersonInfoSchema schemaEmpty = new PersonInfoSchema();
    schemaEmpty.setStartYearMonth("");
    OriginalDateTime resultEmpty = schemaEmpty.getStartDateAsOriginalDateTime();
    assertNotNull(resultEmpty);
    assertEquals(currentYear, resultEmpty.toLocalDate().getYear());
    assertEquals(currentMonth, resultEmpty.toLocalDate().getMonthValue());

    PersonInfoSchema schemaBlank = new PersonInfoSchema();
    schemaBlank.setStartYearMonth("   ");
    OriginalDateTime resultBlank = schemaBlank.getStartDateAsOriginalDateTime();
    assertNotNull(resultBlank);
    assertEquals(currentYear, resultBlank.toLocalDate().getYear());
    assertEquals(currentMonth, resultBlank.toLocalDate().getMonthValue());
  }

  @Test
  @DisplayName("getStartDateAsOriginalDateTime: startYearMonth が不正形式の場合、当月が返ること")
  void testGetStartDateAsOriginalDateTime_InvalidFormat() {
    OriginalDateTime now = new OriginalDateTime();
    int currentYear = now.toLocalDate().getYear();
    int currentMonth = now.toLocalDate().getMonthValue();

    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setStartYearMonth("invalid_date");
    OriginalDateTime result = schema.getStartDateAsOriginalDateTime();
    assertNotNull(result);
    assertEquals(currentYear, result.toLocalDate().getYear());
    assertEquals(currentMonth, result.toLocalDate().getMonthValue());

    schema.setStartYearMonth("2026/abc");
    OriginalDateTime resultNumberFormat = schema.getStartDateAsOriginalDateTime();
    assertNotNull(resultNumberFormat);
    assertEquals(currentYear, resultNumberFormat.toLocalDate().getYear());
    assertEquals(currentMonth, resultNumberFormat.toLocalDate().getMonthValue());

    schema.setStartYearMonth((currentYear + 1) + "/13");
    OriginalDateTime resultMonth13 = schema.getStartDateAsOriginalDateTime();
    assertNotNull(resultMonth13);
    assertEquals(currentYear, resultMonth13.toLocalDate().getYear());
    assertEquals(currentMonth, resultMonth13.toLocalDate().getMonthValue());

    schema.setStartYearMonth((currentYear + 1) + "/00");
    OriginalDateTime resultMonth00 = schema.getStartDateAsOriginalDateTime();
    assertNotNull(resultMonth00);
    assertEquals(currentYear, resultMonth00.toLocalDate().getYear());
    assertEquals(currentMonth, resultMonth00.toLocalDate().getMonthValue());
  }
}
