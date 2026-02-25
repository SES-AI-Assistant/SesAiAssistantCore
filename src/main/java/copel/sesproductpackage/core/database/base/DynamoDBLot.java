package copel.sesproductpackage.core.database.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DynamoDBLot<E extends DynamoDB<E>> implements Iterable<E> {
  /** JSON変換用 ObjectMapper. */
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /** ターゲットテーブル. */
  protected final DynamoDbTable<E> table;

  /** 取得結果. */
  public List<E> entityLot = new ArrayList<>();

  /**
   * コンストラクタ.
   *
   * @param tableName テーブル名
   * @param clazz エンティティクラス
   */
  public DynamoDBLot(final String tableName, final Class<E> clazz) {
    DynamoDbClient client = DynamoDbClientFactory.create();
    DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(clazz));
  }

  /**
   * 検索結果（entityLot）をJSON形式の文字列として返します。<br>
   *
   * @return JSON形式の文字列表現
   */
  @Override
  public String toString() {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.entityLot);
    } catch (JsonProcessingException e) {
      return "[]";
    }
  }

  /**
   * 指定したパーティションキーに基づきレコードを取得し、entityLotに格納します。<br>
   * 以前のentityLotはクリアされます。
   *
   * @param partitionKey パーティションキーの値
   */
  public void fetchByPk(final String partitionKey) {
    this.entityLot.clear();
    this.table
        .query(QueryConditional.keyEqualTo(Key.builder().partitionValue(partitionKey).build()))
        .items()
        .forEach(this.entityLot::add);
  }

  /**
   * 指定した列名と値に対してScanを行い、一致するレコードを取得してentityLotに格納します。<br>
   * 以前のentityLotはクリアされます。
   *
   * @param columnName 列名
   * @param columnValue 代表値
   */
  public void fetchByColumn(final String columnName, final String columnValue) {
    this.entityLot.clear();
    // フィルタ式
    Expression filterExpression =
        Expression.builder()
            .expression("#col = :val")
            .putExpressionName("#col", columnName)
            .putExpressionValue(":val", AttributeValue.builder().s(columnValue).build())
            .build();

    this.table
        .scan(ScanEnhancedRequest.builder().filterExpression(filterExpression).build())
        .items()
        .forEach(this.entityLot::add);
  }

  /**
   * entityLotのイテレーターを返します。
   *
   * @return Iterator
   */
  @Override
  public Iterator<E> iterator() {
    return this.entityLot.iterator();
  }
}
