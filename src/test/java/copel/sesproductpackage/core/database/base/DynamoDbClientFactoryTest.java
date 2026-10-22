package copel.sesproductpackage.core.database.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/** {@link DynamoDbClientFactory} の単体テスト。 */
class DynamoDbClientFactoryTest {

  /** 実行環境の変数を持たない読み取り関数。どの環境変数も未設定として扱う。 */
  private static final Function<String, String> NO_ENVIRONMENT = name -> null;

  /**
   * ビルダーのモックを作り、組み立てた結果の DynamoDbClient のモックを返すように設定する。
   *
   * @return ビルダーのモック
   */
  private static DynamoDbClientBuilder mockBuilder() {
    DynamoDbClientBuilder builder = mock(DynamoDbClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(mock(DynamoDbClient.class));
    return builder;
  }

  /**
   * ビルダーのモックを DynamoDbClient の静的メソッドに差し替えたまま、処理を実行する。 差し替えは処理の後に必ず戻す。
   *
   * @param builder 差し替えるビルダーのモック
   * @param action 差し替え中に実行する処理
   * @param <T> 処理の戻り値の型
   * @return 処理の戻り値
   */
  private static <T> T withBuilder(final DynamoDbClientBuilder builder, final Supplier<T> action) {
    try (MockedStatic<DynamoDbClient> dynamo = mockStatic(DynamoDbClient.class)) {
      dynamo.when(DynamoDbClient::builder).thenReturn(builder);
      return action.get();
    }
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 異常系 {

      /**
       * 非公開のコンストラクタを呼ぶと UnsupportedOperationException を投げることを確かめる。
       * 唯一のファクトリは静的メソッドで使うため、インスタンスを作らせない。
       *
       * @given 非公開のコンストラクタを取得する
       * @when コンストラクタを実行する
       * @then UnsupportedOperationException を原因とする例外を投げる
       */
      @Test
      void 非公開のコンストラクタの場合_UnsupportedOperationExceptionを投げる() throws Exception {
        Constructor<DynamoDbClientFactory> constructor =
            DynamoDbClientFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
            .isInstanceOf(InvocationTargetException.class)
            .hasCauseInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class create {

    @Nested
    class 正常系 {

      /**
       * エンドポイントが指定されている場合に、そのURLと環境変数の認証情報を使うことを確かめる。 ローカル検証の環境では指定されたエンドポイントへ接続するためである。
       *
       * @given エンドポイントが http://localhost:4566、環境変数は未設定
       * @when create をエンドポイントと環境変数の読み取りを指定して実行する
       * @then エンドポイントを指定し、環境変数の認証情報を設定したクライアントを返す
       */
      @Test
      void エンドポイントがある場合_指定のエンドポイントと環境変数の認証情報を設定したクライアントを返す() {
        DynamoDbClientBuilder builder = mockBuilder();

        withBuilder(
            builder, () -> DynamoDbClientFactory.create("http://localhost:4566", NO_ENVIRONMENT));

        verify(builder).region(Region.AP_NORTHEAST_1);
        verify(builder).endpointOverride(URI.create("http://localhost:4566"));
        ArgumentCaptor<AwsCredentialsProvider> credentials =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);
        verify(builder).credentialsProvider(credentials.capture());
        assertThat(credentials.getValue())
            .isInstanceOf(EnvironmentVariableCredentialsProvider.class);
      }

      /**
       * Lambda 上で実行された場合に認証情報を指定せずにクライアントを作ることを確かめる。 Lambda の実行ロールの認証情報を使わせるため、明示の認証情報を付けない。
       *
       * @given エンドポイントが未指定、環境変数 AWS_LAMBDA_FUNCTION_NAME が設定されている
       * @when create をエンドポイント null と Lambda の環境変数で実行する
       * @then リージョンだけを指定したクライアントを返し、認証情報を指定しない
       */
      @Test
      void Lambda上の場合_認証情報を指定せずにリージョンだけのクライアントを返す() {
        DynamoDbClientBuilder builder = mockBuilder();
        Function<String, String> lambda =
            name -> "AWS_LAMBDA_FUNCTION_NAME".equals(name) ? "fn" : null;

        withBuilder(builder, () -> DynamoDbClientFactory.create(null, lambda));

        verify(builder).region(Region.AP_NORTHEAST_1);
        verify(builder, never())
            .credentialsProvider(ArgumentMatchers.<AwsCredentialsProvider>any());
        verify(builder, never()).endpointOverride(any(URI.class));
      }

      /**
       * GitHub Actions 上で実行された場合に環境変数の認証情報を使うことを確かめる。 CI では認証情報を環境変数から与えるためである。
       *
       * @given エンドポイントが未指定、環境変数 CI が設定されている
       * @when create をエンドポイント null と CI の環境変数で実行する
       * @then 環境変数の認証情報を設定したクライアントを返す
       */
      @Test
      void CI上の場合_環境変数の認証情報を設定したクライアントを返す() {
        DynamoDbClientBuilder builder = mockBuilder();
        Function<String, String> ci = name -> "CI".equals(name) ? "true" : null;

        withBuilder(builder, () -> DynamoDbClientFactory.create(null, ci));

        ArgumentCaptor<AwsCredentialsProvider> credentials =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);
        verify(builder).credentialsProvider(credentials.capture());
        assertThat(credentials.getValue())
            .isInstanceOf(EnvironmentVariableCredentialsProvider.class);
      }

      /**
       * ローカルで実行された場合にプロファイルの認証情報を使うことを確かめる。 開発者の PC では AWS の設定ファイルのプロファイルを使うためである。
       *
       * @given エンドポイントが未指定、Lambda と CI の環境変数は未設定
       * @when create をエンドポイント null と未設定の環境変数で実行する
       * @then プロファイルの認証情報を設定したクライアントを返す
       */
      @Test
      void ローカルの場合_プロファイルの認証情報を設定したクライアントを返す() {
        DynamoDbClientBuilder builder = mockBuilder();

        withBuilder(builder, () -> DynamoDbClientFactory.create(null, NO_ENVIRONMENT));

        ArgumentCaptor<AwsCredentialsProvider> credentials =
            ArgumentCaptor.forClass(AwsCredentialsProvider.class);
        verify(builder).credentialsProvider(credentials.capture());
        assertThat(credentials.getValue()).isInstanceOf(ProfileCredentialsProvider.class);
      }

      /**
       * 引数なしの生成がエンドポイントとリージョンの設定を2引数版へ委譲することを確かめる。
       * システムプロパティのエンドポイントは環境変数より優先されるため、実行環境に依存せず接続先を固定できる。
       *
       * @given システムプロパティ aws.endpointUrl が http://localhost:4566
       * @when create を引数なしで実行する
       * @then エンドポイント http://localhost:4566 とリージョン AP_NORTHEAST_1 を指定したクライアントを返す
       */
      @Test
      void 引数なしの場合_システムプロパティのエンドポイントを指定したクライアントを返す() {
        String previous = System.getProperty("aws.endpointUrl");
        System.setProperty("aws.endpointUrl", "http://localhost:4566");
        try {
          DynamoDbClientBuilder builder = mockBuilder();

          withBuilder(builder, DynamoDbClientFactory::create);

          verify(builder).region(Region.AP_NORTHEAST_1);
          verify(builder).endpointOverride(URI.create("http://localhost:4566"));
        } finally {
          if (previous == null) {
            System.clearProperty("aws.endpointUrl");
          } else {
            System.setProperty("aws.endpointUrl", previous);
          }
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * エンドポイントが空白だけの場合に未指定として扱うことを確かめる。 空白のURLで接続先を上書きすると、接続できない状態になるためである。
       *
       * @given エンドポイントが空白 1 文字、環境変数は未設定
       * @when create をエンドポイント空白と未設定の環境変数で実行する
       * @then エンドポイントを指定せずにクライアントを返す
       */
      @Test
      void エンドポイントが空白の場合_エンドポイントを指定しないクライアントを返す() {
        DynamoDbClientBuilder builder = mockBuilder();

        withBuilder(builder, () -> DynamoDbClientFactory.create(" ", NO_ENVIRONMENT));

        verify(builder, never()).endpointOverride(any(URI.class));
      }

      /**
       * エンドポイントが null の場合に未指定として扱うことを確かめる。 環境変数もエンドポイントも無い本番に近い状態で、接続先を上書きしないためである。
       *
       * @given エンドポイントが null、環境変数は未設定
       * @when create をエンドポイント null と未設定の環境変数で実行する
       * @then エンドポイントを指定せずにクライアントを返す
       */
      @Test
      void エンドポイントがnullの場合_エンドポイントを指定しないクライアントを返す() {
        DynamoDbClientBuilder builder = mockBuilder();

        withBuilder(builder, () -> DynamoDbClientFactory.create(null, NO_ENVIRONMENT));

        verify(builder, never()).endpointOverride(any(URI.class));
      }
    }
  }
}
