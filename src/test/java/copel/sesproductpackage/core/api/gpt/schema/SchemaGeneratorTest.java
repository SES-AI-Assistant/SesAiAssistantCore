package copel.sesproductpackage.core.api.gpt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * SchemaGeneratorのテストケース.
 *
 * @author Copel Co., Ltd.
 */
class SchemaGeneratorTest {

  @Test
  void testGenerate_NullClass() {
    assertThrows(RuntimeException.class, () -> SchemaGenerator.generate(null));
  }

  @Test
  void testGenerate_SimpleClass() {
    Map<String, Object> schema = SchemaGenerator.generate(SimpleResponse.class);

    assertNotNull(schema);
    assertEquals("object", schema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    assertNotNull(properties);
    assertTrue(properties.containsKey("name"));
    assertTrue(properties.containsKey("age"));

    @SuppressWarnings("unchecked")
    List<String> required = (List<String>) schema.get("required");
    assertNotNull(required);
    assertTrue(required.contains("name"));
    assertTrue(required.contains("age"));
  }

  @Test
  void testGenerate_WithDescriptions() {
    Map<String, Object> schema = SchemaGenerator.generate(SimpleResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> nameSchema = (Map<String, Object>) properties.get("name");

    assertEquals("string", nameSchema.get("type"));
    assertEquals("人物の名前", nameSchema.get("description"));
  }

  @Test
  void testGenerate_StringType() {
    Map<String, Object> schema = SchemaGenerator.generate(SimpleResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> nameSchema = (Map<String, Object>) properties.get("name");

    assertEquals("string", nameSchema.get("type"));
  }

  @Test
  void testGenerate_IntegerType() {
    Map<String, Object> schema = SchemaGenerator.generate(SimpleResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> ageSchema = (Map<String, Object>) properties.get("age");

    assertEquals("integer", ageSchema.get("type"));
  }

  @Test
  void testGenerate_OptionalField() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithOptional.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    assertTrue(properties.containsKey("name"));
    assertTrue(properties.containsKey("email"));

    @SuppressWarnings("unchecked")
    List<String> required = (List<String>) schema.get("required");
    assertTrue(required.contains("name"));
    assertFalse(required.contains("email"));
  }

  @Test
  void testGenerate_EmptyClass() {
    Map<String, Object> schema = SchemaGenerator.generate(EmptyResponse.class);

    assertNotNull(schema);
    assertEquals("object", schema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    assertTrue(properties.isEmpty());
  }

  /** テスト用の単純なレスポンス型. */
  static class SimpleResponse {
    @Schema(description = "人物の名前", required = true)
    public String name;

    @Schema(description = "人物の年齢", required = true)
    public int age;
  }

  /** テスト用のオプショナルフィールド付きレスポンス型. */
  static class ResponseWithOptional {
    @Schema(description = "人物の名前", required = true)
    public String name;

    @Schema(description = "メールアドレス", required = false)
    public String email;
  }

  /** テスト用の空のレスポンス型. */
  static class EmptyResponse {}

  @Test
  void testGenerate_ListOfStrings() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithList.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> skillsSchema = (Map<String, Object>) properties.get("skills");

    assertEquals("array", skillsSchema.get("type"));
    assertNotNull(skillsSchema.get("items"));

    @SuppressWarnings("unchecked")
    Map<String, Object> itemsSchema = (Map<String, Object>) skillsSchema.get("items");
    assertEquals("string", itemsSchema.get("type"));
  }

  @Test
  void testGenerate_Enum() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithEnum.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> statusSchema = (Map<String, Object>) properties.get("status");

    assertEquals("string", statusSchema.get("type"));
    assertNotNull(statusSchema.get("enum"));

    @SuppressWarnings("unchecked")
    List<String> enumValues = (List<String>) statusSchema.get("enum");
    assertTrue(enumValues.contains("ACTIVE"));
    assertTrue(enumValues.contains("INACTIVE"));
  }

  @Test
  void testGenerate_NestedObject() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithNested.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> userSchema = (Map<String, Object>) properties.get("user");

    assertEquals("object", userSchema.get("type"));
    assertNotNull(userSchema.get("properties"));

    @SuppressWarnings("unchecked")
    Map<String, Object> nestedProps = (Map<String, Object>) userSchema.get("properties");
    assertTrue(nestedProps.containsKey("name"));
    assertTrue(nestedProps.containsKey("email"));
  }

  @Test
  void testGenerate_ListOfObjects() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithListOfObjects.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> usersSchema = (Map<String, Object>) properties.get("users");

    assertEquals("array", usersSchema.get("type"));

    @SuppressWarnings("unchecked")
    Map<String, Object> itemsSchema = (Map<String, Object>) usersSchema.get("items");
    assertEquals("object", itemsSchema.get("type"));
  }

  /** テスト用の配列型を含むレスポンス. */
  static class ResponseWithList {
    @Schema(description = "スキル一覧", itemType = String.class)
    public List<String> skills;
  }

  /** テスト用のEnum型を含むレスポンス. */
  static class ResponseWithEnum {
    @Schema(description = "ステータス", required = true)
    public Status status;
  }

  /** テスト用のステータスEnum. */
  enum Status {
    ACTIVE,
    INACTIVE,
    PENDING
  }

  /** テスト用のネストされたオブジェクトを含むレスポンス. */
  static class ResponseWithNested {
    @Schema(description = "ユーザー情報")
    public UserInfo user;
  }

  /** テスト用のユーザー情報クラス. */
  static class UserInfo {
    @Schema(description = "ユーザー名")
    public String name;

    @Schema(description = "メールアドレス")
    public String email;
  }

  /** テスト用のオブジェクト配列を含むレスポンス. */
  static class ResponseWithListOfObjects {
    @Schema(description = "ユーザー一覧", itemType = UserInfo.class)
    public List<UserInfo> users;
  }

  @Test
  void testGenerate_StringConstraints() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithStringConstraints.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> usernameSchema = (Map<String, Object>) properties.get("username");

    assertEquals("string", usernameSchema.get("type"));
    assertEquals(3, usernameSchema.get("minLength"));
    assertEquals(20, usernameSchema.get("maxLength"));
  }

  @Test
  void testGenerate_PatternConstraint() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithStringConstraints.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> emailSchema = (Map<String, Object>) properties.get("email");

    assertEquals("string", emailSchema.get("type"));
    assertEquals("email", emailSchema.get("format"));
  }

  @Test
  void testGenerate_NumericConstraints() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithNumericConstraints.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> ageSchema = (Map<String, Object>) properties.get("age");

    assertEquals("integer", ageSchema.get("type"));
    assertEquals(0L, ageSchema.get("exclusiveMinimum"));
    assertEquals(150L, ageSchema.get("exclusiveMaximum"));
  }

  @Test
  void testGenerate_DigitConstraints() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithNumericConstraints.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> priceSchema = (Map<String, Object>) properties.get("price");

    assertEquals("number", priceSchema.get("type"));
    assertEquals(5, priceSchema.get("maxDigits"));
    assertEquals(2, priceSchema.get("minDigits"));
  }

  @Test
  void testGenerate_ArrayConstraints() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithArrayConstraints.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> tagsSchema = (Map<String, Object>) properties.get("tags");

    assertEquals("array", tagsSchema.get("type"));
    assertEquals(1, tagsSchema.get("minItems"));
    assertEquals(10, tagsSchema.get("maxItems"));
  }

  @Test
  void testGenerate_DefaultValue() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithDefaultValue.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> statusSchema = (Map<String, Object>) properties.get("status");

    assertEquals("string", statusSchema.get("type"));
    assertEquals("active", statusSchema.get("default"));
  }

  @Test
  void testGenerate_Example() {
    Map<String, Object> schema = SchemaGenerator.generate(ResponseWithExample.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> phoneSchema = (Map<String, Object>) properties.get("phone");

    assertEquals("string", phoneSchema.get("type"));
    assertEquals("090-1234-5678", phoneSchema.get("example"));
  }

  /** テスト用の文字列制約を含むレスポンス. */
  static class ResponseWithStringConstraints {
    @Schema(description = "ユーザー名", required = true, minLength = 3, maxLength = 20)
    public String username;

    @Schema(description = "メールアドレス", format = "email")
    public String email;
  }

  /** テスト用の数値制約を含むレスポンス. */
  static class ResponseWithNumericConstraints {
    @Schema(description = "年齢", gt = 0, lt = 150)
    public int age;

    @Schema(description = "価格", minDigits = 2, maxDigits = 5)
    public double price;
  }

  /** テスト用の配列制約を含むレスポンス. */
  static class ResponseWithArrayConstraints {
    @Schema(description = "タグ一覧", itemType = String.class, minItems = 1, maxItems = 10)
    public List<String> tags;
  }

  /** テスト用のデフォルト値を含むレスポンス. */
  static class ResponseWithDefaultValue {
    @Schema(description = "ステータス", defaultValue = "active")
    public String status;
  }

  /** テスト用の例を含むレスポンス. */
  static class ResponseWithExample {
    @Schema(description = "電話番号", example = "090-1234-5678")
    public String phone;
  }
}
