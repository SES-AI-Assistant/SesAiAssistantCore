package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import copel.sesproductpackage.core.api.gpt.entity.SkillsheetInfoSchema.Experience;
import copel.sesproductpackage.core.api.gpt.entity.SkillsheetInfoSchema.ProjectExperience;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkillsheetInfoSchemaTest {

  @Test
  @DisplayName("通常の期間あり（例: 3年 → ・Java: 3年）")
  void testToSummaryText_NormalDuration() {
    // 期間が明示されている場合はそのまま「・スキル名: 期間」の形式で出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "3年")));

    String expected = "■スキル・経験\n・Java: 3年";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("durationが「不明」の場合に「経験あり」へ置換される")
  void testToSummaryText_UnknownDuration() {
    // 期間が「不明」と推測された場合、「経験あり」に置換されて出力されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "不明")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("durationの前後に空白がある「  不明  」の場合に「経験あり」へ置換される")
  void testToSummaryText_UnknownDurationWithWhitespace() {
    // 前後に空白を含む「  不明  」でもトリムされて「経験あり」に置換されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "  不明  ")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("durationがnullの場合に「経験あり」へ置換される")
  void testToSummaryText_NullDuration() {
    // 期間が未設定(null)の場合、期間なしではなく「経験あり」として表示されることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", null)));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("durationが空文字の場合に「経験あり」へ置換される")
  void testToSummaryText_EmptyDuration() {
    // 期間が空文字("")の場合も「経験あり」として扱われることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("durationが空白文字列の場合に「経験あり」へ置換される")
  void testToSummaryText_BlankDuration() {
    // 期間が空白のみ("   ")の場合もトリムされて「経験あり」として扱われることを検証
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "   ")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

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
    // 1項目あたり約40文字 x 30個 = 約1200文字
    for (int i = 0; i < 30; i++) {
      longExperiences.add(new Experience("スキル項目名あいうえおかきくけこ" + i, "10年間の実務開発経験があります"));
    }
    schema.setExperiences(longExperiences);

    String result = schema.toSummaryText();
    assertEquals(1000, result.length());
    assertTrue(result.startsWith("■スキル・経験\n・スキル項目名あいうえおかきくけこ0"));
  }
}
