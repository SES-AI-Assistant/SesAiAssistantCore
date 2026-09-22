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
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "3年")));

    String expected = "■スキル・経験\n・Java: 3年";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("durationが「不明」の場合に「経験あり」へ置換される")
  void testToSummaryText_UnknownDuration() {
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "不明")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("durationの前後に空白がある「  不明  」の場合に「経験あり」へ置換される")
  void testToSummaryText_UnknownDurationWithWhitespace() {
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", "  不明  ")));

    String expected = "■スキル・経験\n・Java: 経験あり";
    assertEquals(expected, schema.toSummaryText());
  }

  @Test
  @DisplayName("durationがnullの場合（「・Java」となること）")
  void testToSummaryText_NullDuration() {
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setExperiences(List.of(new Experience("Java", null)));

    String expected = "■スキル・経験\n・Java";
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
