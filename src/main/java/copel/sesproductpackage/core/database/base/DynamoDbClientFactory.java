package copel.sesproductpackage.core.database.base;

import copel.sesproductpackage.core.util.AwsEndpointUtil;
import copel.sesproductpackage.core.util.EnvUtils;
import java.net.URI;
import java.util.function.Function;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** DynamoDbClientを生成するファクトリクラス. */
public final class DynamoDbClientFactory {
  private DynamoDbClientFactory() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * DynamoDbClientを生成します.
   *
   * @return DynamoDbClient
   */
  public static DynamoDbClient create() {
    return create(AwsEndpointUtil.resolveEndpointUrl(), EnvUtils::get);
  }

  /**
   * エンドポイントと環境変数の読み取りを指定して、DynamoDbClientを生成します.
   *
   * <p>実行環境（Lambda・GitHub Actions・ローカル）による分岐を、環境変数に依存せず確かめられるように、エンドポイントと環境変数の読み取りを引数で受け取ります。本番では
   * {@link #create()} から実行環境の値を渡します。
   *
   * @param endpointUrl エンドポイントのURL（null または空白の場合は未指定として扱う）
   * @param environment 環境変数を名前から読む関数
   * @return DynamoDbClient
   */
  static DynamoDbClient create(
      final String endpointUrl, final Function<String, String> environment) {
    if (endpointUrl != null && !endpointUrl.isBlank()) {
      return DynamoDbClient.builder()
          .region(Region.AP_NORTHEAST_1)
          .endpointOverride(URI.create(endpointUrl))
          .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
          .build();
    }
    // Lambda上で実行された場合、クレデンシャル指定をしない
    if (environment.apply("AWS_LAMBDA_FUNCTION_NAME") != null) {
      return DynamoDbClient.builder().region(Region.AP_NORTHEAST_1).build();
    }
    // GitHub Actions上で実行された場合、環境変数からCredentialを提供する
    if (environment.apply("CI") != null) {
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
