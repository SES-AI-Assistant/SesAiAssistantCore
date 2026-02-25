package copel.sesproductpackage.core.database.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Data
public abstract class DynamoDB<E> {
  /** JSON変換用ObjectMapper（pretty print対応） */
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /** テーブルマッピング */
  @JsonIgnore protected final DynamoDbTable<E> table;

  /** パーティションキー. */
  private String partitionKey;

  /** ソートキー. */
  private String sortKey;

  /** 最終更新日時（ISO-8601形式） */
  protected String timestamp;

  /**
   * コンストラクタ.
   *
   * @param tableName テーブル
   * @param clazz クラス
   */
  public DynamoDB(final String tableName, final Class<E> clazz) {
    DynamoDbClient client = DynamoDbClientFactory.create();
    DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(clazz));
  }

  /**
   * タイムスタンプを取得します（ISO-8601形式）。
   *
   * @return タイムスタンプ
   */
  @DynamoDbAttribute("timestamp")
  public String getTimestamp() {
    return timestamp;
  }

  /**
   * このインスタンスの内容をDynamoDBに保存します。<br>
   * タイムスタンプは現在時刻に自動更新されます。
   */
  public abstract void save();

  /** このインスタンスのinstagramIdに対応するレコードをDynamoDBから削除します。 */
  public abstract void delete();

  /**
   * このインスタンスのinstagramIdに基づき、DynamoDBから最新の情報を取得し、<br>
   * インスタンスのフィールドを上書きします。<br>
   * レコードが見つからない場合は何もしません。
   */
  public abstract void fetch();

  /**
   * オブジェクトの内容をJSON形式で文字列として返します。
   *
   * @return JSON形式の文字列表現
   */
  @Override
  public String toString() {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
