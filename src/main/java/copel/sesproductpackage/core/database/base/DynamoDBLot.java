package copel.sesproductpackage.core.database.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DynamoDBLot<E extends DynamoDB<E>> implements Iterable<E> {
    /**
     * JSON変換用 ObjectMapper.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Lotオブジェクト.
     */
    protected Collection<E> entityLot = new ArrayList<E>();

    /** 
     * テーブルマッピング
     */
    @JsonIgnore
    protected final DynamoDbTable<E> table;

    /**
     * コンストラクタ.
     *
     * @param tableName テーブル
     * @param clazz クラス
     */
    public DynamoDBLot(final String tableName, final Class<E> clazz) {
        DynamoDbClient client;

        // Lambda上で実行された場合、クレデンシャル指定をしない
        if (System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null) {
            client = DynamoDbClient.builder()
                    .region(Region.AP_NORTHEAST_1)
                    .build();
        // GitHub Actions上で実行された場合、環境変数からCredentialを提供する
        } else if (System.getenv("CI") != null) {
            client = DynamoDbClient.builder()
                    .region(Region.AP_NORTHEAST_1)
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build();
        // ローカルで実行された場合、ProfileCredentialsProviderを使用する
        } else {
            client = DynamoDbClient.builder()
                    .region(Region.AP_NORTHEAST_1)
                    .credentialsProvider(ProfileCredentialsProvider.create())
                    .build();
        }

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(clazz));
    }

    /**
     * PartitonKeyで絞り込み、検索結果をこのLotに持つ.
     */
    public void fetchByPk(final String partitionKey) {
        this.entityLot.clear();
        this.table
             .query(QueryConditional.keyEqualTo(Key.builder().partitionValue(partitionKey).build()))
             .items()
             .forEach(this.entityLot::add);
    }

    /**
     * 指定のカラムで絞り込み、検索結果をこのLotに持つ.
     */
    public void fetchByColumn(final String columnName, final String columnValue) {
        this.entityLot.clear();

        // フィルタ式
        Expression filterExpression = Expression.builder()
                .expression("#col = :val")
                .putExpressionName("#col", columnName)
                .putExpressionValue(":val", AttributeValue.builder().s(columnValue).build())
                .build();

        this.table.scan(ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build())
            .items()
            .forEach(this.entityLot::add);
    }

    @Override
    public Iterator<E> iterator() {
        return this.entityLot.iterator();
    }

    @Override
    public String toString() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.entityLot);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
