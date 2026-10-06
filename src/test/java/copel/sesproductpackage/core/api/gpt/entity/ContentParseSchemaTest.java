package copel.sesproductpackage.core.api.gpt.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.api.gpt.entity.ContentParseSchema.InfoSchema;
import copel.sesproductpackage.core.api.gpt.entity.ContentParseSchema.InformationType;
import copel.sesproductpackage.core.api.gpt.schema.ConditionalCase;
import copel.sesproductpackage.core.api.gpt.schema.ConditionalSchema;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import copel.sesproductpackage.core.api.gpt.schema.SchemaGenerator;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ContentParseSchema の単体テスト.
 *
 * <p>AIによるメール本文・文章のパース結果エンティティに対する
 * Jacksonデシリアライズ、@Schemaメタデータ定義、およびSchemaGeneratorによる
 * JSON Schema生成の動作を網羅的に検証します。
 *
 * @author Copel Co., Ltd.
 */
class ContentParseSchemaTest {

  private final ObjectMapper objectMapper = ObjectMapperFactory.OBJECT_MAPPER;

  // ==========================================================================
  // 1. Jackson デシリアライズ検証（PERSON、JOB、Unknown、混在メール）
  // ==========================================================================

  @Test
  @DisplayName("Jackson: PERSON 情報が正しく PersonInfoSchema としてデシリアライズされること")
  void testDeserialize_PersonInfo() throws Exception {
    String json =
        "{"
            + "\"sender_name\":\"株式会社ABC 田中\","
            + "\"informations\":[{"
            + "  \"type\":\"PERSON\","
            + "  \"summary\":{"
            + "    \"name\":\"T.T\","
            + "    \"age\":35,"
            + "    \"gender\":\"Man\","
            + "    \"nationality\":\"日本\","
            + "    \"price_in_man\":80,"
            + "    \"start_year_month\":\"2026/06\""
            + "  }"
            + "}]"
            + "}";

    ContentParseSchema result = objectMapper.readValue(json, ContentParseSchema.class);

    assertNotNull(result);
    assertEquals("株式会社ABC 田中", result.getSenderName());
    assertNotNull(result.getInformations());
    assertEquals(1, result.getInformations().size());

    InfoSchema info = result.getInformations().get(0);
    assertEquals(InformationType.PERSON, info.getType());
    assertNotNull(info.getSummary());
    assertTrue(info.getSummary() instanceof PersonInfoSchema);

    PersonInfoSchema person = (PersonInfoSchema) info.getSummary();
    assertEquals("T.T", person.getName());
    assertEquals(35, person.getAge());
    assertEquals(new BigDecimal("80"), person.getPriceInMan());
    assertEquals("2026/06", person.getStartYearMonth());
  }

  @Test
  @DisplayName("Jackson: JOB 情報が正しく JobInfoSchema としてデシリアライズされること")
  void testDeserialize_JobInfo() throws Exception {
    String json =
        "{"
            + "\"sender_name\":\"株式会社XYZ 佐藤\","
            + "\"informations\":[{"
            + "  \"type\":\"JOB\","
            + "  \"summary\":{"
            + "    \"title\":\"Javaエンジニア募集\","
            + "    \"overview\":\"基幹システムのマイクロサービス移行開発\","
            + "    \"price_in_man\":85,"
            + "    \"start_year_month\":\"2026/07\","
            + "    \"office_requirements\":2"
            + "  }"
            + "}]"
            + "}";

    ContentParseSchema result = objectMapper.readValue(json, ContentParseSchema.class);

    assertNotNull(result);
    assertEquals("株式会社XYZ 佐藤", result.getSenderName());
    assertNotNull(result.getInformations());
    assertEquals(1, result.getInformations().size());

    InfoSchema info = result.getInformations().get(0);
    assertEquals(InformationType.JOB, info.getType());
    assertNotNull(info.getSummary());
    assertTrue(info.getSummary() instanceof JobInfoSchema);

    JobInfoSchema job = (JobInfoSchema) info.getSummary();
    assertEquals("Javaエンジニア募集", job.getTitle());
    assertEquals("基幹システムのマイクロサービス移行開発", job.getOverview());
    assertEquals(new BigDecimal("85"), job.getPriceInMan());
    assertEquals("2026/07", job.getStartYearMonth());
    assertEquals(2, job.getOfficeRequirements());
  }

  @Test
  @DisplayName("Jackson: Unknown 情報の場合、summary が設定されないこと")
  void testDeserialize_Unknown() throws Exception {
    String json =
        "{"
            + "\"sender_name\":null,"
            + "\"informations\":[{"
            + "  \"type\":\"Unknown\","
            + "  \"summary\":null"
            + "}]"
            + "}";

    ContentParseSchema result = objectMapper.readValue(json, ContentParseSchema.class);

    assertNotNull(result);
    assertNull(result.getSenderName());
    assertNotNull(result.getInformations());
    assertEquals(1, result.getInformations().size());

    InfoSchema info = result.getInformations().get(0);
    assertEquals(InformationType.Unknown, info.getType());
    assertNull(info.getSummary());
  }

  @Test
  @DisplayName("Jackson: 混在メール（JOB と PERSON が併記）で複数要素が正しくデシリアライズされること")
  void testDeserialize_MixedCoexistence() throws Exception {
    String json =
        "{"
            + "\"sender_name\":\"アライアンス事務局\","
            + "\"informations\":["
            + "  {"
            + "    \"type\":\"JOB\","
            + "    \"summary\":{"
            + "      \"title\":\"クラウド案件\","
            + "      \"price_in_man\":90"
            + "    }"
            + "  },"
            + "  {"
            + "    \"type\":\"PERSON\","
            + "    \"summary\":{"
            + "      \"name\":\"山田\","
            + "      \"price_in_man\":75"
            + "    }"
            + "  }"
            + "]"
            + "}";

    ContentParseSchema result = objectMapper.readValue(json, ContentParseSchema.class);

    assertNotNull(result);
    assertEquals("アライアンス事務局", result.getSenderName());
    assertNotNull(result.getInformations());
    assertEquals(2, result.getInformations().size());

    InfoSchema info0 = result.getInformations().get(0);
    assertEquals(InformationType.JOB, info0.getType());
    assertTrue(info0.getSummary() instanceof JobInfoSchema);
    JobInfoSchema job = (JobInfoSchema) info0.getSummary();
    assertEquals("クラウド案件", job.getTitle());
    assertEquals(new BigDecimal("90"), job.getPriceInMan());

    InfoSchema info1 = result.getInformations().get(1);
    assertEquals(InformationType.PERSON, info1.getType());
    assertTrue(info1.getSummary() instanceof PersonInfoSchema);
    PersonInfoSchema person = (PersonInfoSchema) info1.getSummary();
    assertEquals("山田", person.getName());
    assertEquals(new BigDecimal("75"), person.getPriceInMan());
  }

  // ==========================================================================
  // 2. @Schema アノテーション定義の検証（5大判定ルールの網羅性チェック）
  // ==========================================================================

  @Test
  @DisplayName("@Schema: senderName フィールドのアノテーション定義が正しいこと")
  void testSchemaAnnotation_SenderName() throws Exception {
    Field field = ContentParseSchema.class.getDeclaredField("senderName");
    Schema schema = field.getAnnotation(Schema.class);

    assertNotNull(schema);
    assertEquals("送信者会社名+送信者名", schema.title());
    assertEquals(30, schema.maxLength());
    assertEquals("株式会社ABC 田中", schema.example());
    assertTrue(schema.description().contains("営業担当者"));
    assertTrue(schema.description().contains("要員本人（name）"));
  }

  @Test
  @DisplayName("@Schema: informations フィールドのアノテーション定義が正しいこと")
  void testSchemaAnnotation_Informations() throws Exception {
    Field field = ContentParseSchema.class.getDeclaredField("informations");
    Schema schema = field.getAnnotation(Schema.class);

    assertNotNull(schema);
    assertEquals("情報リスト", schema.title());
    assertTrue(schema.required());
    assertEquals(InfoSchema.class, schema.itemType());
    assertTrue(schema.description().contains("本文から案件情報、要員情報、その他の情報を抽出しリスト形式で取得する"));
  }

  @Test
  @DisplayName("@Schema: InfoSchema.type に 5大判定ルールが漏れなく網羅されていること")
  void testSchemaAnnotation_InfoSchemaType_FiveClassificationRules() throws Exception {
    Field field = InfoSchema.class.getDeclaredField("type");
    Schema schema = field.getAnnotation(Schema.class);

    assertNotNull(schema);
    assertEquals("情報種別", schema.title());
    assertTrue(schema.required());
    assertEquals(InformationType.class, schema.itemType());

    String desc = schema.description();
    assertNotNull(desc);

    // ① 主述の向き（最優先基準）
    assertTrue(desc.contains("【最重要判定原則: 主述の向き】"), "主述の向きが明記されていること");
    assertTrue(desc.contains("メールの主旨が人材の提案・売り込み（PERSON）なのか、案件の参画者募集（JOB）なのかを最優先で判定する"));
    assertTrue(desc.contains("返信メール（Re:）でのエントリーの場合も本文中の案件引用に惑わされず「PERSON」として抽出すること"));

    // ② 架空要員の捏造絶対禁止
    assertTrue(desc.contains("【架空要員の捏造絶対禁止】"), "架空要員の捏造絶対禁止が明記されていること");
    assertTrue(desc.contains("特定個人を識別できる具体的な属性（氏名・イニシャル、実年齢、単価等）が本文中に明記されていない場合"));
    assertTrue(desc.contains("職種名（「エンジニア」「プログラマー」「PM」「テックリード」等）や会社名・所属名（「〇〇所属」等）を勝手に氏名（name）として捏造して PERSON を出力することは厳禁とする"));
    assertTrue(desc.contains("具体的な個人プロファイルのない紹介導入文、打診文、一般的な連絡文は、PERSON ではなく必ず「Unknown」と判定すること"));

    // ③ 特定個人属性の存在
    assertTrue(desc.contains("氏名/イニシャル"));
    assertTrue(desc.contains("実年齢"));
    assertTrue(desc.contains("単価"));
    assertTrue(desc.contains("所属形態"));
    assertTrue(desc.contains("稼働開始日"));

    // ④ 案件募集（JOB）の条件と架空要員捏造禁止
    assertTrue(desc.contains("【JOB】: プロジェクト参画者の募集"));
    assertTrue(desc.contains("営業担当者の名前"));
    assertTrue(desc.contains("募集条件"));
    assertTrue(desc.contains("惑わされてPERSONを出力してはならない"));

    // ⑤ 混在メールの切り分け
    assertTrue(desc.contains("【混在メールの切り分け】"));
    assertTrue(desc.contains("案件ブロックからJOBを、要員ブロックからPERSONをそれぞれ別個に抽出すること"));

    // ⑥ Unknown（対象外）の判定条件
    assertTrue(desc.contains("【Unknown】"));
    assertTrue(desc.contains("不在通知"));
    assertTrue(desc.contains("自動返信"));
    assertTrue(desc.contains("退職挨拶"));
    assertTrue(desc.contains("配信停止連絡"));
  }

  @Test
  @DisplayName("@Schema: InfoSchema.type に「架空要員の捏造絶対禁止」および「Unknown判定」の制約が全件アサートされること")
  void testSchemaAnnotation_InfoSchemaType_ProhibitionOfFictitiousPersonConstraint() throws Exception {
    Field field = InfoSchema.class.getDeclaredField("type");
    Schema schema = field.getAnnotation(Schema.class);

    assertNotNull(schema);
    String desc = schema.description();
    assertNotNull(desc);

    assertAll(
        "架空要員の捏造絶対禁止およびUnknown判定の必須文言アサーション",
        () -> assertTrue(desc.contains("【架空要員の捏造絶対禁止】")),
        () -> assertTrue(desc.contains("特定個人を識別できる具体的な属性（氏名・イニシャル、実年齢、単価等）が本文中に明記されていない場合")),
        () -> assertTrue(desc.contains("職種名（「エンジニア」「プログラマー」「PM」「テックリード」等）")),
        () -> assertTrue(desc.contains("会社名・所属名（「〇〇所属」等）")),
        () -> assertTrue(desc.contains("勝手に氏名（name）として捏造して PERSON を出力することは厳禁とする")),
        () -> assertTrue(desc.contains("具体的な個人プロファイルのない紹介導入文、打診文、一般的な連絡文は、PERSON ではなく必ず「Unknown」と判定すること")));
  }

  @Test
  @DisplayName("@ConditionalSchema: summary フィールドの条件付きスキーマ設定が正しいこと")
  void testConditionalSchema_Summary() throws Exception {
    Field field = InfoSchema.class.getDeclaredField("summary");
    ConditionalSchema conditionalSchema = field.getAnnotation(ConditionalSchema.class);
    assertNotNull(conditionalSchema);

    ConditionalCase[] cases = conditionalSchema.value();
    assertEquals(2, cases.length);

    // PERSON ケース
    assertEquals("PERSON", cases[0].enumValue());
    assertEquals("要員情報", cases[0].title());
    assertEquals("", cases[0].description());
    assertEquals(PersonInfoSchema.class, cases[0].schema());

    // JOB ケース
    assertEquals("JOB", cases[1].enumValue());
    assertEquals("案件情報", cases[1].title());
    assertEquals("", cases[1].description());
    assertEquals(JobInfoSchema.class, cases[1].schema());

    // summary の @Schema
    Schema schema = field.getAnnotation(Schema.class);
    assertNotNull(schema);
    assertTrue(schema.description().contains("Unknown の場合は出力しないこと"));
  }

  // ==========================================================================
  // 3. SchemaGenerator による JSON Schema 自動生成検証
  // ==========================================================================

  @Test
  @DisplayName("SchemaGenerator: ContentParseSchema から正しく JSON Schema が生成されること")
  void testGenerateSchema_ContentParseSchema() {
    Map<String, Object> schema = SchemaGenerator.generate(ContentParseSchema.class);

    assertNotNull(schema);
    assertEquals("object", schema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    assertNotNull(properties);
    assertTrue(properties.containsKey("sender_name"));
    assertTrue(properties.containsKey("informations"));

    // informations 配列スキーマの検証
    @SuppressWarnings("unchecked")
    Map<String, Object> informationsSchema = (Map<String, Object>) properties.get("informations");
    assertEquals("array", informationsSchema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> itemsSchema = (Map<String, Object>) informationsSchema.get("items");
    assertEquals("object", itemsSchema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> itemProperties = (Map<String, Object>) itemsSchema.get("properties");
    assertNotNull(itemProperties);
    assertTrue(itemProperties.containsKey("type"));
    assertTrue(itemProperties.containsKey("summary"));

    // type スキーマの検証
    @SuppressWarnings("unchecked")
    Map<String, Object> typeSchema = (Map<String, Object>) itemProperties.get("type");
    assertEquals("string", typeSchema.get("type"));

    @SuppressWarnings("unchecked")
    List<String> enums = (List<String>) typeSchema.get("enum");
    assertEquals(3, enums.size());
    assertTrue(enums.contains("JOB"));
    assertTrue(enums.contains("PERSON"));
    assertTrue(enums.contains("Unknown"));

    String typeDesc = (String) typeSchema.get("description");
    assertNotNull(typeDesc);
    assertTrue(typeDesc.contains("【最重要判定原則: 主述の向き】"));

    // summary スキーマの anyOf 検証
    @SuppressWarnings("unchecked")
    Map<String, Object> summarySchema = (Map<String, Object>) itemProperties.get("summary");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> anyOf = (List<Map<String, Object>>) summarySchema.get("anyOf");
    assertNotNull(anyOf);
    assertEquals(2, anyOf.size());
  }

  // ==========================================================================
  // 4. Getter / Setter / Enum の動作検証
  // ==========================================================================

  @Test
  @DisplayName("Lombok: ContentParseSchema および InfoSchema の Getter / Setter が正常動作すること")
  void testGetterSetterAndConstructors() {
    ContentParseSchema schema = new ContentParseSchema();
    schema.setSenderName("株式会社テスト");

    List<InfoSchema> list = new ArrayList<>();
    InfoSchema info = new InfoSchema();
    info.setType(InformationType.PERSON);
    info.setSummary(new PersonInfoSchema());
    list.add(info);
    schema.setInformations(list);

    assertEquals("株式会社テスト", schema.getSenderName());
    assertEquals(1, schema.getInformations().size());
    assertEquals(InformationType.PERSON, schema.getInformations().get(0).getType());
    assertNotNull(schema.getInformations().get(0).getSummary());

    // AllArgsConstructor の動作
    ContentParseSchema allArgsSchema = new ContentParseSchema("株式会社XYZ", list);
    assertEquals("株式会社XYZ", allArgsSchema.getSenderName());
    assertEquals(list, allArgsSchema.getInformations());

    InfoSchema allArgsInfo = new InfoSchema(InformationType.JOB, new JobInfoSchema());
    assertEquals(InformationType.JOB, allArgsInfo.getType());
    assertTrue(allArgsInfo.getSummary() instanceof JobInfoSchema);

    // equals / hashCode / toString
    assertNotNull(schema.toString());
    assertNotNull(info.toString());
    assertEquals(schema, new ContentParseSchema("株式会社テスト", list));
  }

  @Test
  @DisplayName("InformationType: Enum 値の定義と valueOf が正常動作すること")
  void testInformationTypeEnum() {
    assertEquals(InformationType.JOB, InformationType.valueOf("JOB"));
    assertEquals(InformationType.PERSON, InformationType.valueOf("PERSON"));
    assertEquals(InformationType.Unknown, InformationType.valueOf("Unknown"));

    InformationType[] values = InformationType.values();
    assertEquals(3, values.length);
  }
}
