package copel.sesproductpackage.core.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import copel.sesproductpackage.core.database.base.DynamoDB;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@DynamoDbBean
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SES_AI_API_USAGE_HISTORY extends DynamoDB<SES_AI_API_USAGE_HISTORY> {
  /** 【PK】 プロバイダ名. 例：OpenAI、Googleなど */
  private Provider provider;

  /** 【PK】 モデル名. 例：GPT4o-mini、Gemini-1.5-flashなど */
  private String model;

  /** 【PK】 使用月（YYYYMM形式）. 例：202501、など */
  private String usageMonth;

  /** 【SK】 APIを使用したユーザーのID. システムがAPIを利用した場合はモジュール名が入力されます 例：ユーザーID、lambda関数名など */
  private String userId;

  /** 【SK】 API種別. 例：Generate、Embedding、FineTuningなど */
  private ApiType apiType;

  /** 入力文字数. */
  private BigDecimal inputCount = new BigDecimal(0);

  /** 出力文字数. */
  private BigDecimal outputCount = new BigDecimal(0);

  @DynamoDbPartitionKey
  @DynamoDbAttribute("partitionKey")
  public String getPartitionKey() {
    return this.provider + "#" + this.model + "#" + this.usageMonth;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute("sortKey")
  public String getSortKey() {
    return this.userId + "#" + this.apiType;
  }

  public SES_AI_API_USAGE_HISTORY() {
    super("SES_AI_API_USAGE_HISTORY", SES_AI_API_USAGE_HISTORY.class);
  }

  @Override
  public void save() {
    if (this.provider == null
        || this.model == null
        || this.model.isEmpty()
        || this.usageMonth == null
        || this.usageMonth.isEmpty()) {
      return;
    } else {
      this.timestamp = Instant.now().toString();
      this.table.putItem(this);
    }
  }

  @Override
  public void delete() {
    this.table.deleteItem(
        Key.builder().partitionValue(this.getPartitionKey()).sortValue(this.getSortKey()).build());
  }

  @Override
  public void fetch() {
    SES_AI_API_USAGE_HISTORY latest =
        this.table.getItem(
            r ->
                r.key(
                        Key.builder()
                            .partitionValue(this.getPartitionKey())
                            .sortValue(this.getSortKey())
                            .build())
                    .consistentRead(true));
    if (latest != null) {
      this.provider = latest.getProvider();
      this.model = latest.getModel();
      this.usageMonth = latest.getUsageMonth();
      this.userId = latest.getUserId();
      this.apiType = latest.getApiType();
      this.inputCount = latest.getInputCount();
      this.outputCount = latest.getOutputCount();
      this.timestamp = latest.getTimestamp();
    }
  }

  /**
   * このレコードのInputCountに値を加える.
   *
   * @param count 加えたい数
   */
  public void addInputCount(final int count) {
    this.addInputCount(new BigDecimal(count));
  }

  public void addInputCount(final BigDecimal count) {
    this.inputCount = this.inputCount.add(count);
  }

  /**
   * このレコードのOutputCountに値を加える.
   *
   * @param count 加えたい数
   */
  public void addOutputCount(final int count) {
    this.addOutputCount(new BigDecimal(count));
  }

  public void addOutputCount(final BigDecimal count) {
    this.outputCount = this.outputCount.add(count);
  }

  @Override
  public String toString() {
    return super.toString();
  }

  /**
   * プロバイダ名列挙型.
   *
   * @author 鈴木一矢
   */
  public enum Provider {
    OpenAI,
    Google
  }

  /**
   * API種別列挙型.
   *
   * @author 鈴木一矢
   */
  public enum ApiType {
    Generate,
    Embedding,
    FineTuning
  }
}
