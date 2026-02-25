package copel.sesproductpackage.core.database.base;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** DynamoDbClientを生成するファクトリクラス. */
public class DynamoDbClientFactory {
  private DynamoDbClientFactory() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * DynamoDbClientを生成します.
   *
   * @return DynamoDbClient
   */
  public static DynamoDbClient create() {
    // Lambda上で実行された場合、クレデンシャル指定をしない
    if (copel.sesproductpackage.core.util.EnvUtils.get("AWS_LAMBDA_FUNCTION_NAME") != null) {
      return DynamoDbClient.builder().region(Region.AP_NORTHEAST_1).build();
    }
    // GitHub Actions上で実行された場合、環境変数からCredentialを提供する
    if (copel.sesproductpackage.core.util.EnvUtils.get("CI") != null) {
      return DynamoDbClient.builder()
          .region(Region.AP_NORTHEAST_1)
          .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
          .build();
    }
    // ローカルで実行された場合、ProfileCredentialsProviderを使用する
    return DynamoDbClient.builder()
        .region(Region.AP_NORTHEAST_1)
        .credentialsProvider(ProfileCredentialsProvider.create())
        .build();
  }
}
