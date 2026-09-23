package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import copel.sesproductpackage.core.api.gpt.entity.SkillsheetInfoSchema.Experience;
import copel.sesproductpackage.core.api.gpt.entity.SkillsheetInfoSchema.ProjectExperience;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkillsheetInfoSchemaTest {

  // ================================================
  // 正常系テスト（正規表現パターンに合致する有効な期間）
  // ================================================

  @Test
  @DisplayName("正常系: 年のみの期間（例: 「3年」→ そのまま「・Java: 3年」と出力されること）")
  void testToSummaryText_ValidDuration_Years() {
    // 「N年」形式の期間が指定された場合、そのまま出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "3年")));

    String expected = "■スキル・経験\n・Java: 3年";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正常系: 「ヶ月」表記の月数（例: 「6ヶ月」→ そのまま「・Java: 6ヶ月」と出力されること）")
  void testToSummaryText_ValidDuration_Months() {
    // 「Nヶ月」形式の期間が指定された場合、そのまま出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "6ヶ月")));

    String expected = "■スキル・経験\n・Java: 6ヶ月";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正常系: 年と月の組み合わせ（例: 「1年2ヶ月」→ そのまま「・Java: 1年2ヶ月」と出力されること）")
  void testToSummaryText_ValidDuration_YearsAndMonths() {
    // 「N年Nヶ月」形式の期間が指定された場合、そのまま出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "1年2ヶ月")));

    String expected = "■スキル・経験\n・Java: 1年2ヶ月";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正常系: カタカナ「カ月」表記（例: 「3カ月」→ そのまま「・Java: 3カ月」と出力されること）")
  void testToSummaryText_ValidDuration_KatakanaMonth() {
    // 「Nカ月」形式（表記揺らぎ）が指定された場合、そのまま出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "3カ月")));

    String expected = "■スキル・経験\n・Java: 3カ月";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正常系: ひらがな「か月」表記（例: 「3か月」→ そのまま「・Java: 3か月」と出力されること）")
  void testToSummaryText_ValidDuration_HiraganaMonth() {
    // 「Nか月」形式（表記揺らぎ）が指定された場合、そのまま出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "3か月")));

    String expected = "■スキル・経験\n・Java: 3か月";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正常系: 「月」のみ表記（例: 「3月」→ そのまま「・Java: 3月」と出力されること）")
  void testToSummaryText_ValidDuration_SimpleMonth() {
    // 「N月」形式（表記揺らぎ）が指定された場合、そのまま出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "3月")));

    String expected = "■スキル・経験\n・Java: 3月";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正常系: 2桁以上の年・月表記（例: 「10年11ヶ月」→ そのまま「・Java: 10年11ヶ月」と出力されること）")
  void testToSummaryText_ValidDuration_MultiDigits() {
    // 2桁以上の数値を含む期間が指定された場合、そのまま出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "10年11ヶ月")));

    String expected = "■スキル・経験\n・Java: 10年11ヶ月";
    assertEquals(expected, schema.toSummaryText());
  }

  // ================================================
  // 異常系・未設定系テスト（null、空文字、空白）
  // ================================================

  @Test
  @DisplayName("未設定系: durationがnullの場合に「経験あり」へ置換される")
  void testToSummaryText_NullDuration() {
    // 期間が未設定(null)の場合、一律「経験あり」として表示されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", null)));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("未設定系: durationが空文字の場合に「経験あり」へ置換される")
  void testToSummaryText_EmptyDuration() {
    // 期間が空文字("")の場合も未設定と判定され「経験あり」として扱われることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("未設定系: durationが空白文字列の場合に「経験あり」へ置換される")
  void testToSummaryText_BlankDuration() {
    // 期間が半角空白のみ("   ")の場合も未設定と判定され「経験あり」として扱われることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "   ")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  // ================================================
  // 正規表現不一致系テスト（不明、未記載、なし、約3年、abc等）
  // ================================================

  @Test
  @DisplayName("正規表現不一致系: 「不明」の場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_Unknown() {
    // 「不明」と記載された場合、パターンに合致しないため「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "不明")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正規表現不一致系: 前後に空白がある「  不明  」の場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_UnknownWithWhitespace() {
    // 前後に空白を含む「  不明  」でもトリムされて判定され「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "  不明  ")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正規表現不一致系: 「未記載」の場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_NotWritten() {
    // 「未記載」と記載された場合、パターンに合致しないため「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "未記載")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正規表現不一致系: 「なし」の場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_None() {
    // 「なし」と記載された場合、パターンに合致しないため「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "なし")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正規表現不一致系: 「約3年」など曖昧な接頭辞がある場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_PrefixApprox() {
    // 「約3年」のように接頭辞が付く場合、厳密パターンに合致しないため「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "約3年")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正規表現不一致系: 「abc」など英字無効値の場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_Alphabet() {
    // 「abc」のような無効文字列の場合、パターンに合致しないため「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "abc")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正規表現不一致系: 「0年」など0始まりの無効数値の場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_ZeroYears() {
    // 「0年」のように1以上の正の整数でない場合、「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "0年")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正規表現不一致系: 「0ヶ月」など0始まりの無効月数の場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_ZeroMonths() {
    // 「0ヶ月」のように1以上の正の整数でない場合、「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "0ヶ月")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("正規表現不一致系: 「3年半」など規定外表記の場合に「経験あり」へ置換される")
  void testToSummaryText_InvalidDuration_HalfYear() {
    // 「3年半」のように非標準表記の場合、「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "3年半")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  // ================================================
  // スキーマアノテーション定義テスト
  // ================================================

  @Test
  @DisplayName("スキーマ定義: Experience.durationの@Schemaに期待する正規表現patternが設定されていること")
  void testSchemaAnnotation_DurationPattern() throws NoSuchFieldException {
    // OpenAPI / JSON Schema生成用アノテーションに期待通りの正規表現が指定されていることを検証
    Field durationField = Experience.class.getDeclaredField("duration");
    Schema schemaAnnotation = durationField.getAnnotation(Schema.class);

    assertEquals(SkillsheetInfoSchema.DURATION_REGEX, schemaAnnotation.pattern());
    assertEquals(
        "^([1-9][0-9]*年([1-9][0-9]*[ヶケカヵか]?月)?|[1-9][0-9]*[ヶケカヵか]?月)$",
        SkillsheetInfoSchema.DURATION_REGEX);
  }

  @Test
  @DisplayName("スキーマ定義: Experienceのアノテーションtitleが「スキル」「経験年数」に設定されていること")
  void testSchemaAnnotation_ExperienceTitles() throws NoSuchFieldException {
    Field perspectiveField = Experience.class.getDeclaredField("perspective");
    Schema perspectiveSchema = perspectiveField.getAnnotation(Schema.class);
    assertEquals("スキル", perspectiveSchema.title());

    Field durationField = Experience.class.getDeclaredField("duration");
    Schema durationSchema = durationField.getAnnotation(Schema.class);
    assertEquals("経験年数", durationSchema.title());
  }

  @Test
  @DisplayName("スキーマ定義: ProjectExperienceの年月フィールドに期待する正規表現patternが設定されていること")
  void testSchemaAnnotation_ProjectExperiencePatterns() throws NoSuchFieldException {
    Field startMonthField = ProjectExperience.class.getDeclaredField("startMonth");
    Schema startMonthSchema = startMonthField.getAnnotation(Schema.class);
    assertEquals(SkillsheetInfoSchema.YEAR_MONTH_REGEX, startMonthSchema.pattern());

    Field endMonthField = ProjectExperience.class.getDeclaredField("endMonth");
    Schema endMonthSchema = endMonthField.getAnnotation(Schema.class);
    assertEquals(SkillsheetInfoSchema.YEAR_MONTH_REGEX, endMonthSchema.pattern());

    assertEquals("^[0-9]{4}年([1-9]|1[0-2])月$", SkillsheetInfoSchema.YEAR_MONTH_REGEX);
  }

  // ================================================
  // その他の基本動作テスト
  // ================================================

  @Test
  @DisplayName("experiencesがnullまたは空リストの場合")
  void testToSummaryText_NullOrEmptyExperiences() {
    SkillsheetInfoSchema schemaNull = new SkillsheetInfoSchema();
    schemaNull.setExperiences(null);
    assertEquals("", schemaNull.toSummaryText());

    SkillsheetInfoSchema schemaEmpty = new SkillsheetInfoSchema();
    schemaEmpty.setExperiences(Collections.emptyList());
    assertEquals("", schemaEmpty.toSummaryText());
  }

  @Test
  @DisplayName("projectExperiencesがある場合")
  void testToSummaryText_WithProjectExperiences() {
    SkillsheetInfoSchema schemaOnlyPj = new SkillsheetInfoSchema();
    schemaOnlyPj.setProjectExperiences(
        List.of(new ProjectExperience("2023年8月", "2023年11月", "Webアプリ開発")));

    String expectedOnlyPj = "■直近のPJ経験\n2023年8月-2023年11月: Webアプリ開発";
    assertEquals(expectedOnlyPj, schemaOnlyPj.toSummaryText());

    SkillsheetInfoSchema schemaBoth = new SkillsheetInfoSchema();
    schemaBoth.setExperiences(List.of(new Experience("Java", "3年")));
    schemaBoth.setProjectExperiences(
        List.of(new ProjectExperience("2023年8月", "2023年11月", "Webアプリ開発")));

    String expectedBoth = "■スキル・経験\n・Java: 3年\n■直近のPJ経験\n2023年8月-2023年11月: Webアプリ開発";
    assertEquals(expectedBoth, schemaBoth.toSummaryText());
  }

  @Test
  @DisplayName("1000文字を超える場合のカット処理")
  void testToSummaryText_Exceeds1000Characters() {
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    List<Experience> longExperiences = new ArrayList<>();
    // 1項目あたり約35文字 x 35個 = 約1225文字
    for (int i = 0; i < 35; i++) {
      longExperiences.add(new Experience("スキル項目名あいうえおかきくけこさしすせそたちつてとなにぬねの" + i, "10年"));
    }
    schema.setExperiences(longExperiences);

    String result = schema.toSummaryText();
    assertEquals(1000, result.length());
    assertTrue(result.startsWith("■スキル・経験\n・スキル項目名あいうえおかきくけこさしすせそたちつてとなにぬねの0"));
  }
}
