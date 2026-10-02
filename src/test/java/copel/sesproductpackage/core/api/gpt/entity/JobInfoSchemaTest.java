package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.api.gpt.entity.JobInfoSchema.Requirements;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.unit.Money;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
  // isValid の動作検証
  // ================================================

  @Test
  @DisplayName("isValid: 正常系（タイトル・概要・必須スキル完備の案件データ）")
  void isValid_normal_withTitleAndOverviewAndMustSkills() {
    JobInfoSchema job = new JobInfoSchema();
    job.setTitle("大手ECサイト刷新に伴うバックエンド開発");
    job.setOverview("Java/SpringBootを用いたマイクロサービスの設計・開発");
    job.setMustList(List.of(new Requirements("Java開発経験3年以上", "3年")));

    assertTrue(job.isValid());
  }

  @Test
  @DisplayName("isValid: 正常系（必須スキルなし・概要のみ記載されている案件データ）")
  void isValid_normal_withTitleAndOverviewOnly() {
    JobInfoSchema job = new JobInfoSchema();
    job.setTitle("クラウド移行支援PMO");
    job.setOverview("AWS環境への移行プロジェクトにおける進捗管理およびベンダーコントロール");
    job.setMustList(null);

    assertTrue(job.isValid());
  }

  @Test
  @DisplayName("isValid: 正常系（概要なし・必須スキルのみ記載されている案件データ）")
  void isValid_normal_withTitleAndMustSkillsOnly() {
    JobInfoSchema job = new JobInfoSchema();
    job.setTitle("金融機関向けインフラ構築保守");
    job.setOverview(null);
    job.setMustList(List.of(new Requirements("Linux/RHEL環境の運用構築経験", "2年")));

    assertTrue(job.isValid());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Re: 【案件】Javaエンジニア募集",
        "re: 案件状況のご確認",
        "Fwd: 【急募】インフラ構築案件",
        "スキルシート管理システム開発",
        "職務経歴書自動生成AIサービスの開発支援",
        "人事システム刷新PJにおけるスキルシート連携機能の構築"
      })
  @DisplayName("isValid: 正常系（返信スレッド・スキルシート関連システム開発案件の救済通過）")
  void isValid_normal_replyThreadAndSkillSheetSystemDevelopment(String validTitle) {
    JobInfoSchema job = new JobInfoSchema();
    job.setTitle(validTitle);
    job.setOverview("案件概要です。");
    job.setMustList(List.of(new Requirements("Java経験", "2年")));

    assertTrue(job.isValid());
  }

  @Test
  @DisplayName("isValid: 異常系（タイトルがnull）")
  void isValid_abnormal_nullTitle() {
    JobInfoSchema job = new JobInfoSchema();
    job.setTitle(null);
    job.setOverview("概要文");
    job.setMustList(List.of(new Requirements("スキル", "1年")));

    assertFalse(job.isValid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "　", "\t\n"})
  @DisplayName("isValid: 異常系（案件名が空文字・空白文字）")
  void isValid_abnormal_blankTitle(String invalidTitle) {
    JobInfoSchema job = new JobInfoSchema();
    job.setTitle(invalidTitle);
    job.setOverview("概要文");
    job.setMustList(List.of(new Requirements("スキル", "1年")));

    assertFalse(job.isValid());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "【自動応答】お問い合わせを受信いたしました",
        "GW休暇のお知らせ（4/29〜5/6）",
        "不在通知：本日は外出しております",
        "退職のご挨拶（長年お世話になりました）",
        "メールマガジン配信停止のお知らせ"
      })
  @DisplayName("isValid: 異常系（不在通知・自動応答・挨拶等の誤混入排除）")
  void isValid_abnormal_absentNotificationKeywords(String invalidTitle) {
    JobInfoSchema job = new JobInfoSchema();
    job.setTitle(invalidTitle);
    job.setOverview("自動応答や不在通知などの本文です。");
    job.setMustList(List.of(new Requirements("スキル", "1年")));

    assertFalse(job.isValid());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "【要員紹介】Java上級エンジニアのご提案",
        "要員提案：即日稼働可能なPM候補",
        "【要員】フルスタックエンジニア（Vue/Node.js）",
        "最新スキルシート送付の件",
        "スキルシート添付いたします",
        "職務経歴書送付のご案内",
        "経歴書添付の件"
      })
  @DisplayName("isValid: 異常系（要員提案・スキルシート送付等の誤混入排除）")
  void isValid_abnormal_personProposalKeywords(String invalidTitle) {
    JobInfoSchema job = new JobInfoSchema();
    job.setTitle(invalidTitle);
    job.setOverview("要員プロファイル情報です。");
    job.setMustList(List.of(new Requirements("スキル", "1年")));

    assertFalse(job.isValid());
  }

  @Test
  @DisplayName("isValid: 異常系（募集実体なし：概要なし かつ 必須スキルなし）")
  void isValid_abnormal_noSubstance() {
    JobInfoSchema job1 = new JobInfoSchema();
    job1.setTitle("システム開発案件");
    job1.setOverview(null);
    job1.setMustList(null);

    JobInfoSchema job2 = new JobInfoSchema();
    job2.setTitle("システム開発案件");
    job2.setOverview("   ");
    job2.setMustList(List.of());

    assertAll(
        () -> assertFalse(job1.isValid()),
        () -> assertFalse(job2.isValid()));
  }
}
