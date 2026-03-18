package copel.sesproductpackage.core.database.base;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/**
 * UT用のDynamoDbClientFactoryクラス.
 */
public final class DynamoDbClientFactory {
  private DynamoDbClientFactory() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * DynamoDbClientを生成します.
   * 既存のテストで DynamoDbClient が mockStatic されている場合はそれを利用し、
   * そうでない場合は安全なモック（RETURNS_DEEP_STUBS）を返却します.
   *
   * @return DynamoDbClient
   */
  public static DynamoDbClient create() {
    try {
      DynamoDbClientBuilder builder = DynamoDbClient.builder();
      // DynamoDbClient.builder() がモックされている（テストで設定されている）場合
      if (mockingDetails(builder).isMock()) {
        return builder.region(Region.AP_NORTHEAST_1).build();
      }
    } catch (Throwable t) {
      // ignore
    }
    // それ以外は安全なデフォルトモックを返す
    return mock(DynamoDbClient.class, RETURNS_DEEP_STUBS);
  }
}
