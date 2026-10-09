package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JobInfoSchema の単体テスト.
 *
 * @author Copel Co., Ltd.
 */
class JobInfoSchemaTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // ================================================
  // priceInMan および getPrice() の動作検証
  // ================================================

  @Test
  @DisplayName("getPrice: priceInMan が通常万円数値（185）の場合、1,850,000円の Money が返ること")
  void testGetPrice_NormalMan() {
    JobInfoSchema schema = new JobInfoSchema();
    schema.setPriceInMan(new BigDecimal("185"));

    Money price = schema.getPrice();
    assertNotNull(price);
    assertFalse(price.isEmpty());
    assertEquals(1850000L, price.toYenValue());
    assertEquals("185万円", price.toJapaneseFormat());
  }

  @Test
  @DisplayName("getPrice: priceInMan が小数万円数値（65.5）の場合、655,000円の Money が返ること")
  void testGetPrice_DecimalMan() {
    JobInfoSchema schema = new JobInfoSchema();
    schema.setPriceInMan(new BigDecimal("65.5"));

    Money price = schema.getPrice();
    assertNotNull(price);
    assertFalse(price.isEmpty());
    assertEquals(655000L, price.toYenValue());
  }

  @Test
  @DisplayName("getPrice: priceInMan がスキル見合い仕様値（999）の場合、isNegotiable が true となること")
  void testGetPrice_Negotiable() {
    JobInfoSchema schema = new JobInfoSchema();
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
    JobInfoSchema schema = new JobInfoSchema();
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
  @DisplayName("Jackson: JSONから priceInMan が数値（185）としてデシリアライズされ、getPrice で円換算されること")
  void testJacksonDeserialize_Number() throws Exception {
    String json = "{\"title\":\"テスト案件\",\"priceInMan\":185}";
    JobInfoSchema schema = objectMapper.readValue(json, JobInfoSchema.class);

    assertNotNull(schema);
    assertEquals(new BigDecimal("185"), schema.getPriceInMan());
    assertEquals(1850000L, schema.getPrice().toYenValue());
  }

  @Test
  @DisplayName("Jackson: JSONから priceInMan がスキル見合い（999）としてデシリアライズされること")
  void testJacksonDeserialize_Negotiable() throws Exception {
    String json = "{\"title\":\"テスト案件\",\"priceInMan\":999}";
    JobInfoSchema schema = objectMapper.readValue(json, JobInfoSchema.class);

    assertNotNull(schema);
    assertEquals(new BigDecimal("999"), schema.getPriceInMan());
    assertTrue(schema.getPrice().isNegotiable());
  }

  @Test
  @DisplayName("Jackson: JSONの priceInMan が null の場合、空の Money が返ること")
  void testJacksonDeserialize_NullPrice() throws Exception {
    String json = "{\"title\":\"テスト案件\",\"priceInMan\":null}";
    JobInfoSchema schema = objectMapper.readValue(json, JobInfoSchema.class);

    assertNotNull(schema);
    assertNull(schema.getPriceInMan());
    assertTrue(schema.getPrice().isEmpty());
  }

  @Test
  @DisplayName("Jackson: JSONに priceInMan キーが存在しない場合、空の Money が返ること")
  void testJacksonDeserialize_MissingKey() throws Exception {
    String json = "{\"title\":\"テスト案件\"}";
    JobInfoSchema schema = objectMapper.readValue(json, JobInfoSchema.class);

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
    JobInfoSchema schema = new JobInfoSchema();
    schema.setTitle("テスト案件");
    schema.setOverview("テスト概要");
    schema.setPriceInMan(new BigDecimal("185"));

    String summaryText = schema.toSummaryText();
    assertTrue(summaryText.contains("■単価: 185万円"));
  }

  // ================================================
  // @Schema アノテーション定義の検証
  // ================================================

  @Test
  @DisplayName("@Schema: priceInMan に期待通りのアノテーションが設定されていること")
  void testSchemaAnnotation() throws Exception {
    Field field = JobInfoSchema.class.getDeclaredField("priceInMan");
    Schema schemaAnnotation = field.getAnnotation(Schema.class);

    assertNotNull(schemaAnnotation);
    assertEquals("単価（万円）", schemaAnnotation.title());
    assertTrue(schemaAnnotation.required());
    assertEquals("number", schemaAnnotation.type());
    assertEquals("185", schemaAnnotation.example());
    assertTrue(schemaAnnotation.description().contains("月額単価を万円単位の数値で設定"));
  }

  // ================================================
  // 必須・尚可（要求月数）の出力検証
  // ================================================

  @Test
  @DisplayName(
      "toSummaryText/getMustSkillsAsString/getWantSkillsAsString: 要求月数は年月表記で出力され、未設定の場合は項目名のみ出力されること")
  void testRequirementsWithDurationMonths() {
    JobInfoSchema schema = new JobInfoSchema();
    schema.setTitle("テスト案件");
    schema.setOverview("テスト概要");
    schema.setMustList(
        List.of(
            new JobInfoSchema.Requirements("PMO経験", null),
            new JobInfoSchema.Requirements("Java", 36)));
    schema.setWantList(
        List.of(
            new JobInfoSchema.Requirements("Big4出身", null),
            new JobInfoSchema.Requirements("Python", 6)));

    assertEquals("PMO経験\nJava: 3年", schema.getMustSkillsAsString());
    assertEquals("Big4出身\nPython: 6ヶ月", schema.getWantSkillsAsString());

    String summaryText = schema.toSummaryText();
    assertTrue(summaryText.contains("・PMO経験\n"));
    assertTrue(summaryText.contains("・Java: 3年\n"));
    assertTrue(summaryText.contains("・Big4出身\n"));
    assertTrue(summaryText.contains("・Python：6ヶ月\n"));
  }

  // ================================================
  // 推測抑止: 記載の無い項目の捏造を誘発する最低件数指定が無いこと
  // ================================================

  @Test
  @DisplayName("Schema: リスト項目に最低件数（minItems）が設定されていないこと")
  void testSchema_NoMinItems() throws NoSuchFieldException {
    for (String fieldName : List.of("mustList", "wantList")) {
      Schema schema = JobInfoSchema.class.getDeclaredField(fieldName).getAnnotation(Schema.class);
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

    JobInfoSchema schema = new JobInfoSchema();
    schema.setStartYearMonth((currentYear + 1) + "/10");

    OriginalDateTime result = schema.getStartDateAsOriginalDateTime("案件情報です。【即日】スタート希望");
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

    JobInfoSchema schema1 = new JobInfoSchema();
    schema1.setStartYearMonth("2024/06");

    OriginalDateTime result1 = schema1.getStartDateAsOriginalDateTime("通常案件本文");
    assertNotNull(result1);
    assertEquals(currentYear, result1.toLocalDate().getYear());
    assertEquals(currentMonth, result1.toLocalDate().getMonthValue());

    JobInfoSchema schema2 = new JobInfoSchema();
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

    JobInfoSchema schema = new JobInfoSchema();
    schema.setStartYearMonth(futureYear + "/05");

    OriginalDateTime result = schema.getStartDateAsOriginalDateTime("通常案件本文");
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

    JobInfoSchema schemaNull = new JobInfoSchema();
    schemaNull.setStartYearMonth(null);
    OriginalDateTime resultNull = schemaNull.getStartDateAsOriginalDateTime();
    assertNotNull(resultNull);
    assertEquals(currentYear, resultNull.toLocalDate().getYear());
    assertEquals(currentMonth, resultNull.toLocalDate().getMonthValue());

    JobInfoSchema schemaEmpty = new JobInfoSchema();
    schemaEmpty.setStartYearMonth("");
    OriginalDateTime resultEmpty = schemaEmpty.getStartDateAsOriginalDateTime();
    assertNotNull(resultEmpty);
    assertEquals(currentYear, resultEmpty.toLocalDate().getYear());
    assertEquals(currentMonth, resultEmpty.toLocalDate().getMonthValue());

    JobInfoSchema schemaBlank = new JobInfoSchema();
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

    JobInfoSchema schema = new JobInfoSchema();
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
