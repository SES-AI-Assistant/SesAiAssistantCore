package copel.sesproductpackage.core.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import copel.sesproductpackage.core.database.SES_AI_M_INGEST_ROUTE.ChannelType;
import copel.sesproductpackage.core.database.base.DynamoDB;
import copel.sesproductpackage.core.database.base.DynamoDbClientFactory;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * テナント別月次受信集計情報テーブル（SES_AI_TENANT_MONTHLY_INGEST_COUNT）のエンティティクラス.
 * <p>
 * テナントごと・年月別・チャネル別のメッセージ受信件数を集計・管理します。
 * 各テナントの月間受信上限チェックや利用状況モニタリングなどの用途で利用されます。
 * </p>
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
@Data
@DynamoDbBean
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SES_AI_TENANT_MONTHLY_INGEST_COUNT
    extends DynamoDB<SES_AI_TENANT_MONTHLY_INGEST_COUNT> {

  /** デフォルトテーブル名. */
  public static final String DEFAULT_TABLE_NAME = "SES_AI_TENANT_MONTHLY_INGEST_COUNT";

  /** 【PK】 テナントID. */
  private String tenantId;

  /** 【PK】 年月（YYYYMM形式）. */
  private String yearMonth;

  /**
   * 【SK】 チャネル種別（EMAIL / LINE）.
   * 文字列ではなくEnumを使用することで型安全性を確保し、不正なチャネル名の混入を防止します。
   */
  private ChannelType channelType;

  /** 受信カウント. */
  private Long count = 0L;

  // ※ 最終更新日時（timestamp）は親クラス DynamoDB の仕様（ISO-8601形式文字列）に準拠して継承・管理

  /** 低レベル DynamoDbClient の静的キャッシュ. */
  private static DynamoDbClient dynamoDbClient;

  /** コンストラクタ. */
  public SES_AI_TENANT_MONTHLY_INGEST_COUNT() {
    super(resolveTableName(), SES_AI_TENANT_MONTHLY_INGEST_COUNT.class);
  }

  /**
   * SSM パラメータまたはフォールバックからテーブル名を解決します.
   *
   * @return テーブル名
   */
  public static String resolveTableName() {
    String tableName =
        Properties.get(SsmParameterKey.TENANT_MONTHLY_INGEST_COUNT_TABLE_NAME.getKey());
    if (tableName == null || tableName.isBlank()) {
      return DEFAULT_TABLE_NAME;
    }
    return tableName;
  }

  /**
   * DynamoDbClient を取得します（遅延初期化・静的キャッシュ）.
   *
   * @return DynamoDbClient
   */
  public static synchronized DynamoDbClient getDynamoDbClient() {
    if (dynamoDbClient == null) {
      dynamoDbClient = DynamoDbClientFactory.create();
    }
    return dynamoDbClient;
  }

  /**
   * DynamoDbClient を設定します（テスト・DI用）.
   *
   * @param client DynamoDbClient
   */
  public static synchronized void setDynamoDbClient(DynamoDbClient client) {
    dynamoDbClient = client;
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("partitionKey")
  public String getPartitionKey() {
    return this.tenantId + "#" + this.yearMonth;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute("sortKey")
  public String getSortKey() {
    return this.channelType != null ? this.channelType.getValue() : null;
  }

  @Override
  public void save() {
    if (this.tenantId == null
        || this.tenantId.isEmpty()
        || this.yearMonth == null
        || this.yearMonth.isEmpty()
        || this.channelType == null) {
      return;
    }
    // 親クラス DynamoDB の仕様に準拠し、最終更新日時（ISO-8601形式）を記録
    this.timestamp = Instant.now().toString();
    this.table.putItem(this);
  }

  @Override
  public void delete() {
    if (this.tenantId == null
        || this.tenantId.isEmpty()
        || this.yearMonth == null
        || this.yearMonth.isEmpty()
        || this.channelType == null) {
      return;
    }
    this.table.deleteItem(
        Key.builder().partitionValue(this.getPartitionKey()).sortValue(this.getSortKey()).build());
  }

  @Override
  public void fetch() {
    if (this.tenantId == null
        || this.tenantId.isEmpty()
        || this.yearMonth == null
        || this.yearMonth.isEmpty()
        || this.channelType == null) {
      return;
    }
    SES_AI_TENANT_MONTHLY_INGEST_COUNT latest =
        this.table.getItem(
            r ->
                r.key(
                        Key.builder()
                            .partitionValue(this.getPartitionKey())
                            .sortValue(this.getSortKey())
                            .build())
                    .consistentRead(true));
    if (latest != null) {
      this.tenantId = latest.getTenantId();
      this.yearMonth = latest.getYearMonth();
      this.channelType = latest.getChannelType();
      this.count = latest.getCount();
      // 親クラス DynamoDB の仕様に準拠した最終更新日時を取得
      this.timestamp = latest.getTimestamp();
    }
  }

  /**
   * 現在年月（JST基準）のメッセージ受信カウントをアトミックに 1 加算します.
   * チャネル種別にChannelType Enumを使用することで型安全性を確保し、不正なチャネル名の混入を防止します.
   *
   * @param tenantId テナントID
   * @param channelType チャネル種別（ChannelType）
   */
  public static void increment(String tenantId, ChannelType channelType) {
    if (channelType == null) {
      log.warn("increment skipped due to null channelType: tenantId={}", tenantId);
      return;
    }
    increment(tenantId, channelType.getValue());
  }

  /**
   * 現在年月（JST基準）のメッセージ受信カウントを指定量アトミックに加算します.
   *
   * @param tenantId テナントID
   * @param channelType チャネル種別（ChannelType）
   * @param amount 加算量
   */
  public static void increment(String tenantId, ChannelType channelType, long amount) {
    OriginalDateTime nowDt = new OriginalDateTime();
    String yearMonth = nowDt.getYYYYMM();
    increment(tenantId, yearMonth, channelType, amount);
  }

  /**
   * 指定年月のメッセージ受信カウントをアトミックに加算します.
   * チャネル種別にChannelType Enumを使用することで型安全性を確保し、不正なチャネル名の混入を防止します.
   *
   * @param tenantId テナントID
   * @param yearMonth 年月（YYYYMM）
   * @param channelType チャネル種別（ChannelType）
   * @param amount 加算量
   */
  public static void increment(
      String tenantId, String yearMonth, ChannelType channelType, long amount) {
    if (channelType == null) {
      log.warn(
          "increment skipped due to null channelType: tenantId={}, yearMonth={}",
          tenantId,
          yearMonth);
      return;
    }
    increment(tenantId, yearMonth, channelType.getValue(), amount);
  }

  /**
   * 現在年月（JST基準）のメッセージ受信カウントをアトミックに 1 加算します.
   *
   * @param tenantId テナントID
   * @param channelType チャネル種別文字列（EMAIL / LINE）
   */
  public static void increment(String tenantId, String channelType) {
    OriginalDateTime nowDt = new OriginalDateTime();
    String yearMonth = nowDt.getYYYYMM();
    increment(tenantId, yearMonth, channelType, 1L);
  }

  /**
   * 指定年月のメッセージ受信カウントをアトミックに加算します.
   *
   * @param tenantId テナントID
   * @param yearMonth 年月（YYYYMM）
   * @param channelType チャネル種別（EMAIL / LINE）
   * @param amount 加算量
   */
  public static void increment(
      String tenantId, String yearMonth, String channelType, long amount) {
    // 引数バリデーション
    // 0以下の不正な加算値をガードし、意図しない減算や無駄な更新をスキップ
    if (tenantId == null
        || tenantId.isBlank()
        || yearMonth == null
        || yearMonth.isBlank()
        || channelType == null
        || channelType.isBlank()
        || amount <= 0) {
      log.warn(
          "increment skipped due to invalid arguments: tenantId={}, yearMonth={}, channelType={}, amount={}",
          tenantId,
          yearMonth,
          channelType,
          amount);
      return;
    }

    Map<String, AttributeValue> key = new HashMap<>();
    key.put("partitionKey", AttributeValue.builder().s(tenantId + "#" + yearMonth).build());
    key.put("sortKey", AttributeValue.builder().s(channelType).build());

    Map<String, String> expressionAttributeNames = new HashMap<>();
    // 親クラス仕様に準拠した最終更新日時属性
    expressionAttributeNames.put("#upd", "timestamp");
    expressionAttributeNames.put("#tid", "tenantId");
    expressionAttributeNames.put("#ym", "yearMonth");
    expressionAttributeNames.put("#ct", "channelType");
    expressionAttributeNames.put("#cnt", "count");

    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(
        ":now", AttributeValue.builder().s(Instant.now().toString()).build());
    expressionAttributeValues.put(":tid", AttributeValue.builder().s(tenantId).build());
    expressionAttributeValues.put(":ym", AttributeValue.builder().s(yearMonth).build());
    expressionAttributeValues.put(":ct", AttributeValue.builder().s(channelType).build());
    expressionAttributeValues.put(
        ":inc", AttributeValue.builder().n(String.valueOf(amount)).build());

    // 受信件数を高速かつアトミックに加算するため、DynamoDbClientのUpdateItemを使用
    UpdateItemRequest updateItemRequest =
        UpdateItemRequest.builder()
            .tableName(resolveTableName())
            .key(key)
            .updateExpression(
                "SET #upd = :now, #tid = if_not_exists(#tid, :tid), #ym = if_not_exists(#ym, :ym), #ct = if_not_exists(#ct, :ct) ADD #cnt :inc")
            .expressionAttributeNames(expressionAttributeNames)
            .expressionAttributeValues(expressionAttributeValues)
            .build();

    try {
      getDynamoDbClient().updateItem(updateItemRequest);
    } catch (Exception e) {
      log.error(
          "Failed to increment monthly ingest count: tenantId={}, yearMonth={}, channelType={}, amount={}",
          tenantId,
          yearMonth,
          channelType,
          amount,
          e);
      throw e;
    }
  }
}
