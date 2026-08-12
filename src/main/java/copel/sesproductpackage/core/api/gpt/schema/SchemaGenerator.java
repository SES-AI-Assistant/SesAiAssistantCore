package copel.sesproductpackage.core.api.gpt.schema;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Jacksonアノテーションとリフレクションを使用してJSON Schemaを自動生成するユーティリティクラス.
 *
 * <p>クラスのフィールドから@JsonPropertyアノテーションを読み取り、JSON Schema形式のMapを生成します。
 * 複雑なスキーマ定義の手書きを避け、Javaクラスの定義を単一の情報源として活用します。
 *
 * <p>サポートする型：
 *
 * <ul>
 *   <li>String - type: "string"
 *   <li>int, Integer - type: "integer"
 *   <li>long, Long - type: "integer"
 *   <li>double, Double - type: "number"
 *   <li>boolean, Boolean - type: "boolean"
 *   <li>List - type: "array"
 * </ul>
 *
 * <p>使用例：
 *
 * <pre>{@code
 * class PersonResponse implements JsonSchemaProvider {
 *   @JsonProperty(description = "人物の名前", required = true)
 *   private String name;
 *
 *   @JsonProperty(description = "人物の年齢", required = true)
 *   private int age;
 *
 *   @Override
 *   public Map<String, Object> getJsonSchema() {
 *     return SchemaGenerator.generate(this.getClass());
 *   }
 * }
 * }</pre>
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public final class SchemaGenerator {

  private SchemaGenerator() {}

  /**
   * 指定されたクラスからJSON Schemaを自動生成します.
   *
   * @param clazz JSON Schema生成対象のクラス
   * @return JSON SchemaをMap形式で返す。type: "object"のスキーマオブジェクト
   * @throws RuntimeException スキーマ生成に失敗した場合
   */
  public static Map<String, Object> generate(final Class<?> clazz) {
    if (clazz == null) {
      throw new RuntimeException("Class must not be null");
    }

    try {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", "object");

      Map<String, Object> properties = new LinkedHashMap<>();
      List<String> required = new ArrayList<>();

      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
        if (isStaticOrSpecial(field)) {
          continue;
        }

        Schema schemaAnnotation = field.getAnnotation(Schema.class);
        String fieldName = getFieldName(field);
        Map<String, Object> fieldSchema = generateFieldSchema(field, schemaAnnotation);

        properties.put(fieldName, fieldSchema);

        if (schemaAnnotation != null && schemaAnnotation.required()) {
          required.add(fieldName);
        }
      }

      schema.put("properties", properties);
      if (!required.isEmpty()) {
        schema.put("required", required);
      }

      log.debug("Generated JSON Schema for {}: {}", clazz.getSimpleName(), schema);
      return schema;
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to generate schema for " + clazz.getName() + ": " + e.getMessage(), e);
    }
  }

  /**
   * フィールド名を取得します.
   *
   * @param field リフレクションフィールド
   * @return フィールド名
   */
  private static String getFieldName(final Field field) {
    return field.getName();
  }

  /**
   * フィールド用のJSON Schemaを生成します.
   *
   * @param field リフレクションフィールド
   * @param schema @Schemaアノテーション（nullableあり）
   * @return フィールドのスキーマオブジェクト
   */
  private static Map<String, Object> generateFieldSchema(final Field field, final Schema schema) {
    Map<String, Object> fieldSchema = new LinkedHashMap<>();

    Class<?> fieldType = field.getType();

    if (fieldType == List.class || fieldType.isArray()) {
      handleArrayType(fieldSchema, field, schema);
    } else if (fieldType.isEnum()) {
      handleEnumType(fieldSchema, fieldType);
    } else if (isComplexType(fieldType)) {
      handleNestedObjectType(fieldSchema, fieldType);
    } else {
      handleSimpleType(fieldSchema, fieldType, schema);
    }

    if (schema != null && !schema.description().isEmpty()) {
      fieldSchema.put("description", schema.description());
    }

    return fieldSchema;
  }

  /**
   * 配列型フィールドを処理します.
   *
   * @param fieldSchema 生成対象のスキーマMap
   * @param field リフレクションフィールド
   * @param schema @Schemaアノテーション（nullableあり）
   */
  private static void handleArrayType(
      final Map<String, Object> fieldSchema, final Field field, final Schema schema) {
    fieldSchema.put("type", "array");

    Class<?> itemType = Object.class;
    if (schema != null && schema.itemType() != Object.class) {
      itemType = schema.itemType();
    }

    Map<String, Object> itemsSchema = new LinkedHashMap<>();
    if (itemType == List.class || itemType.isArray()) {
      itemsSchema.put("type", "array");
    } else if (itemType.isEnum()) {
      populateEnumSchema(itemsSchema, itemType);
    } else if (isComplexType(itemType)) {
      itemsSchema.put("type", "object");
      Map<String, Object> nestedProps = generateObjectProperties(itemType);
      if (!nestedProps.isEmpty()) {
        itemsSchema.put("properties", nestedProps);
      }
    } else {
      itemsSchema.put("type", getJsonType(itemType));
    }

    fieldSchema.put("items", itemsSchema);
  }

  /**
   * Enum型フィールドを処理します.
   *
   * @param fieldSchema 生成対象のスキーマMap
   * @param enumType Enum型クラス
   */
  private static void handleEnumType(
      final Map<String, Object> fieldSchema, final Class<?> enumType) {
    populateEnumSchema(fieldSchema, enumType);
  }

  /**
   * Enum型スキーマを入力Mapに追加します.
   *
   * @param schemaMap 生成対象のスキーマMap
   * @param enumType Enum型クラス
   */
  private static void populateEnumSchema(
      final Map<String, Object> schemaMap, final Class<?> enumType) {
    schemaMap.put("type", "string");
    Object[] enumConstants = enumType.getEnumConstants();
    List<String> enumValues = new ArrayList<>();
    for (Object constant : enumConstants) {
      enumValues.add(constant.toString());
    }
    schemaMap.put("enum", enumValues);
  }

  /**
   * ネストされたオブジェクト型フィールドを処理します.
   *
   * @param fieldSchema 生成対象のスキーマMap
   * @param objectType オブジェクト型クラス
   */
  private static void handleNestedObjectType(
      final Map<String, Object> fieldSchema, final Class<?> objectType) {
    fieldSchema.put("type", "object");
    Map<String, Object> nestedProps = generateObjectProperties(objectType);
    if (!nestedProps.isEmpty()) {
      fieldSchema.put("properties", nestedProps);
    }
  }

  /**
   * シンプル型フィールドを処理します.
   *
   * @param fieldSchema 生成対象のスキーマMap
   * @param fieldType フィールド型
   * @param schema @Schemaアノテーション（nullableあり）
   */
  private static void handleSimpleType(
      final Map<String, Object> fieldSchema, final Class<?> fieldType, final Schema schema) {
    String jsonType = getJsonType(fieldType);

    if (schema != null && !schema.type().isEmpty()) {
      jsonType = schema.type();
    }

    fieldSchema.put("type", jsonType);
  }

  /**
   * クラスのプロパティ定義を生成します.
   *
   * @param clazz 対象クラス
   * @return propertiesのMap
   */
  private static Map<String, Object> generateObjectProperties(final Class<?> clazz) {
    Map<String, Object> properties = new LinkedHashMap<>();
    Field[] fields = clazz.getDeclaredFields();

    for (Field field : fields) {
      if (isStaticOrSpecial(field)) {
        continue;
      }

      Schema schemaAnnotation = field.getAnnotation(Schema.class);
      String fieldName = getFieldName(field);
      Map<String, Object> fieldSchema = generateFieldSchema(field, schemaAnnotation);
      properties.put(fieldName, fieldSchema);
    }

    return properties;
  }

  /**
   * 複雑な型（カスタムオブジェクト）かどうかを判定します.
   *
   * @param clazz 対象クラス
   * @return 複雑な型の場合true
   */
  private static boolean isComplexType(final Class<?> clazz) {
    return clazz != null
        && !clazz.isPrimitive()
        && !clazz.equals(String.class)
        && !clazz.equals(Integer.class)
        && !clazz.equals(Long.class)
        && !clazz.equals(Double.class)
        && !clazz.equals(Float.class)
        && !clazz.equals(Boolean.class)
        && !clazz.equals(List.class)
        && !clazz.isArray()
        && !clazz.isEnum()
        && !clazz.getPackage().getName().startsWith("java.");
  }

  /**
   * Javaの型をJSON Schema の型文字列にマッピングします.
   *
   * @param fieldType Javaのフィールド型
   * @return JSON Schemaの型文字列（"string", "integer"など）
   */
  private static String getJsonType(final Class<?> fieldType) {
    if (fieldType == String.class) {
      return "string";
    }
    if (fieldType == int.class || fieldType == Integer.class) {
      return "integer";
    }
    if (fieldType == long.class || fieldType == Long.class) {
      return "integer";
    }
    if (fieldType == double.class || fieldType == Double.class) {
      return "number";
    }
    if (fieldType == float.class || fieldType == Float.class) {
      return "number";
    }
    if (fieldType == boolean.class || fieldType == Boolean.class) {
      return "boolean";
    }
    if (fieldType == java.util.List.class || fieldType.isArray()) {
      return "array";
    }
    return "string";
  }

  /**
   * staticフィールドまたは特殊フィールドかどうかを判定します.
   *
   * @param field リフレクションフィールド
   * @return staticまたは特殊フィールドの場合true
   */
  private static boolean isStaticOrSpecial(final Field field) {
    int modifiers = field.getModifiers();
    return java.lang.reflect.Modifier.isStatic(modifiers)
        || java.lang.reflect.Modifier.isTransient(modifiers)
        || field.getName().startsWith("$");
  }
}
