package copel.sesproductpackage.core.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import copel.sesproductpackage.core.database.base.DynamoDb;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/** SES_AI_API_USAGE_HISTORY テーブルの1レコードを表す Entity です。DynamoDB に保存します。 */
@Data
@DynamoDbBean
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SES_AI_API_USAGE_HISTORY extends DynamoDb<SES_AI_API_USAGE_HISTORY> {
  /** Core が API を呼び出したときに使用履歴へ記録する利用者ID. モジュール名を入れる運用に合わせた固定値。 */
  public static final String CORE_USER_ID = "SesAiAssitantCore";

  /** 【PK】 プロバイダ名. 例：OpenAI、Googleなど */
  private Provider provider;

  /** 【PK】 モデル名. 例：GPT4o-mini、Gemini-1.5-flashなど */
  private String model;

  /** 【PK】 使用月（YYYYMM形式）. 例：202501、など */
  private String usageMonth;

  /** 【SK】 使用日（YYYYMMDD形式）. 例：20250115、など */
  private String usageDate;

  /** 【SK】 APIを使用したユーザーのID. システムがAPIを利用した場合はモジュール名が入力されます 例：ユーザーID、lambda関数名など */
  private String userId;

  /** 【SK】 API種別. 例：Generate、Embedding、FineTuningなど */
  private ApiType apiType;

  /** 入力文字数. */
  private BigDecimal inputCount = BigDecimal.ZERO;

  /** 出力文字数. */
  private BigDecimal outputCount = BigDecimal.ZERO;

  /**
   * 使用日を取得します.
   *
   * @return 使用日（YYYYMMDD形式）
   */
  public String getUsageDate() {
    return this.usageDate;
  }

  /**
   * 使用日を設定します.
   *
   * @param usageDate 使用日（YYYYMMDD形式）
   */
  public void setUsageDate(String usageDate) {
    this.usageDate = usageDate;
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("partitionKey")
  public String getPartitionKey() {
    return this.provider + "#" + this.model + "#" + this.usageMonth;
  }

  /**
   * DynamoDB のソートキーを返します。利用日がある場合は利用日・ユーザーID・API種別、ない場合はユーザーID・API種別を連結した文字列です。
   *
   * @return ソートキー
   */
  @DynamoDbSortKey
  @DynamoDbAttribute("sortKey")
  public String getSortKey() {
    if (this.usageDate != null && !this.usageDate.isEmpty()) {
      return this.usageDate + "#" + this.userId + "#" + this.apiType;
    }
    return this.userId + "#" + this.apiType;
  }

  /** 空のインスタンスを生成します。 */
  public SES_AI_API_USAGE_HISTORY() {
    super(
        Properties.get(SsmParameterKey.API_USAGE_HISTORY_TABLE_NAME.getKey()),
        SES_AI_API_USAGE_HISTORY.class);
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
      this.timestamp = Instant.now(ApplicationClock.INSTANCE.clock()).toString();
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
      this.usageDate = latest.getUsageDate();
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

  /**
   * このレコードの入力件数に値を加えます。
   *
   * @param count 加えたい数
   */
  public void addInputCount(final BigDecimal count) {
    if (this.inputCount == null) {
      this.inputCount = BigDecimal.ZERO;
    }
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

  /**
   * このレコードの出力件数に値を加えます。
   *
   * @param count 加えたい数
   */
  public void addOutputCount(final BigDecimal count) {
    if (this.outputCount == null) {
      this.outputCount = BigDecimal.ZERO;
    }
    this.outputCount = this.outputCount.add(count);
  }

  /**
   * API の利用を1件記録します。同じキーのレコードが既にあれば、入力と出力の件数を加算します。
   *
   * <p>利用月と利用日は現在時刻から決める。利用者は常に {@link #CORE_USER_ID} とする。 各 API
   * クライアントは、この処理を呼ぶ前後で例外の扱いを変えるため、記録の成否は呼び出し側で決める。
   *
   * @param provider プロバイダ
   * @param model モデル名
   * @param apiType API 種別
   * @param inputCount 入力文字数
   * @param outputCount 出力文字数
   */
  public void recordUsage(
      final Provider provider,
      final String model,
      final ApiType apiType,
      final int inputCount,
      final int outputCount) {
    OriginalDateTime now = new OriginalDateTime();
    this.setProvider(provider);
    this.setModel(model);
    this.setUsageMonth(now.getYYYYMM());
    this.setUsageDate(now.getYyyyMmDd());
    this.setUserId(CORE_USER_ID);
    this.setApiType(apiType);
    this.fetch();
    this.addInputCount(inputCount);
    this.addOutputCount(outputCount);
    this.save();
  }

  /**
   * プロバイダ名列挙型.
   *
   * @author Copel Co., Ltd.
   */
  public enum Provider {
    OpenAI,
    Google
  }

  /**
   * API種別列挙型.
   *
   * @author Copel Co., Ltd.
   */
  public enum ApiType {
    Generate,
    Embedding,
    FineTuning
  }
}
