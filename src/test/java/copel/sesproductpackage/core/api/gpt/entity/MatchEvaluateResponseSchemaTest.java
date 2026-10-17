package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.OfficeEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.PersonMonthsEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.PlaceEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.PriceEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.SkillEvaluateResult;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * MatchEvaluateResponseSchema の単体テスト.
 *
 * @author Copel Co., Ltd.
 */
class MatchEvaluateResponseSchemaTest {

  /**
   * 必須スキル以外の条件をすべて満たす評価結果を生成する.
   *
   * @param mustResults 必須スキルの評価結果
   * @return 評価結果
   */
  private MatchEvaluateResponseSchema createSchema(EvaluateType... mustResults) {
    List<SkillEvaluateResult> mustList = new ArrayList<>();
    for (EvaluateType result : mustResults) {
      mustList.add(new SkillEvaluateResult("Java", result, "根拠"));
    }
    MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
    schema.setMustList(mustList);
    schema.setPriceResult(new PriceEvaluateResult(true, null));
    schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.FullyMet, null));
    schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.FullyMet, null));
    schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(true, null));
    return schema;
  }

  // ================================================
  // isMatch() の必須スキル判定
  // ================================================

  @Test
  @DisplayName("isMatch: 必須スキルがすべてFullyMetの場合はtrue")
  void testIsMatch_AllFullyMet() {
    assertTrue(createSchema(EvaluateType.FullyMet, EvaluateType.FullyMet).isMatch());
  }

  @Test
  @DisplayName("isMatch: 必須スキルにPartiallyMet（類似経験のみ明記）が含まれる場合はfalse")
  void testIsMatch_PartiallyMet() {
    assertFalse(createSchema(EvaluateType.FullyMet, EvaluateType.PartiallyMet).isMatch());
  }

  @Test
  @DisplayName("isMatch: 必須スキルにNotMetが含まれる場合はfalse")
  void testIsMatch_NotMet() {
    assertFalse(createSchema(EvaluateType.PartiallyMet, EvaluateType.NotMet).isMatch());
  }

  @Test
  @DisplayName("isMatch: 必須スキルにUnknownが含まれる場合はfalse")
  void testIsMatch_Unknown() {
    assertFalse(createSchema(EvaluateType.FullyMet, EvaluateType.Unknown).isMatch());
  }

  @Test
  @DisplayName("isMatch: 必須スキルの評価結果がnullの場合はfalse")
  void testIsMatch_NullResult() {
    assertFalse(createSchema(EvaluateType.FullyMet, null).isMatch());
  }

  @Test
  @DisplayName("isMatch: 案件に必須スキルの記載が無く空リストの場合はtrue")
  void testIsMatch_EmptyMustList() {
    assertTrue(createSchema().isMatch());
  }

  // ================================================
  // isMatch() の条件面判定
  // ================================================

  @Test
  @DisplayName("isMatch: mustListがnullの場合はfalse")
  void testIsMatch_NullMustList() {
    MatchEvaluateResponseSchema schema = createSchema(EvaluateType.FullyMet);
    schema.setMustList(null);
    assertFalse(schema.isMatch());
  }

  @Test
  @DisplayName("isMatch: 単価がfalseの場合はfalse")
  void testIsMatch_PriceNg() {
    MatchEvaluateResponseSchema schema = createSchema(EvaluateType.FullyMet);
    schema.setPriceResult(new PriceEvaluateResult(false, null));
    assertFalse(schema.isMatch());
  }

  @Test
  @DisplayName("isMatch: 出社要件がFullyMet以外の場合はfalse")
  void testIsMatch_OfficeNg() {
    MatchEvaluateResponseSchema schema = createSchema(EvaluateType.FullyMet);
    schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.PartiallyMet, null));
    assertFalse(schema.isMatch());
  }

  @Test
  @DisplayName("isMatch: 場所がNotMetの場合はfalse")
  void testIsMatch_PlaceNg() {
    MatchEvaluateResponseSchema schema = createSchema(EvaluateType.FullyMet);
    schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.NotMet, null));
    assertFalse(schema.isMatch());
  }

  @Test
  @DisplayName("isMatch: 人月工数がfalseの場合はfalse")
  void testIsMatch_PersonMonthsNg() {
    MatchEvaluateResponseSchema schema = createSchema(EvaluateType.FullyMet);
    schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(false, null));
    assertFalse(schema.isMatch());
  }

  // ================================================
  // getMatchScore() の類似経験の減点
  // ================================================

  @Test
  @DisplayName("getMatchScore: 必須スキルのPartiallyMetはFullyMetより減点されること")
  void testGetMatchScore_PartiallyMetDeducted() {
    // FullyMet(1.0) + PartiallyMet(0.4) の平均 0.7 × 40 = 28点 + 条件面30点 + 尚可20点 + その他10点
    assertEquals(
        88, createSchema(EvaluateType.FullyMet, EvaluateType.PartiallyMet).getMatchScore());
    assertEquals(100, createSchema(EvaluateType.FullyMet, EvaluateType.FullyMet).getMatchScore());
  }

  // ================================================
  // LLMへ送信されるプロンプト（@Schema）の検証
  // ================================================

  @Test
  @DisplayName("Schema: 必須スキル評価の判定基準に、明記された内容のみで判定し推測しない旨が含まれること")
  void testSchema_SkillResultDescription() throws NoSuchFieldException {
    Schema schema =
        SkillEvaluateResult.class.getDeclaredField("result").getAnnotation(Schema.class);
    assertTrue(schema.description().contains("要員情報に明記された実務経験のみで判定する"));
    assertTrue(schema.description().contains("代替可能な類似スキル・経験"));
    assertTrue(schema.description().contains("推測"));
  }

  @Test
  @DisplayName("Schema: 必須・尚可・その他リストに最低件数が設定されていないこと（記載が無い項目の捏造防止）")
  void testSchema_NoMinItems() throws NoSuchFieldException {
    for (String fieldName : List.of("mustList", "wantList", "otherList")) {
      Schema schema =
          MatchEvaluateResponseSchema.class.getDeclaredField(fieldName).getAnnotation(Schema.class);
      assertEquals(-1, schema.minItems(), fieldName);
    }
  }

  @Test
  @DisplayName("toNotificationSummary/toEvaluiationText: 必須スキルのPartiallyMetが〇で表示されること")
  void testText_PartiallyMet() {
    MatchEvaluateResponseSchema schema =
        createSchema(EvaluateType.FullyMet, EvaluateType.PartiallyMet);
    assertTrue(schema.toNotificationSummary().contains("・必須スキル：〇"));
    assertTrue(schema.toEvaluiationText().contains("・Java：〇(根拠)"));
  }
}
