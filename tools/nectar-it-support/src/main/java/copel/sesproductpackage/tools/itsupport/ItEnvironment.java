package copel.sesproductpackage.tools.itsupport;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;

/**
 * IT1（結合テスト）の実行環境です。DB と AWS を提供する1つのコンテナと、そこへ初期データを入れる・結果を確かめる操作を持ちます.
 *
 * <p>コンテナは {@code nectar-it-env/Dockerfile}（このライブラリ同梱）と、利用側の DDL からビルドし、JVM 内で1回だけ起動します。 PostgreSQL
 * は Core が環境変数から読む {@code DB_PORT} の番号でホストに公開し、AWS（moto）は空いているポートで公開して {@code aws.endpointUrl}
 * で接続先を渡します。Core の {@code Properties} は最初の利用時に SSM と S3 を1回だけ読むため、ハンドラを読み込む前に {@link
 * #builder(String)} の {@link Builder#start()} を呼んで設定値を投入します.
 *
 * <p>利用側は、モジュール固有の値（DDL、キュー、SSM のパラメータ、DynamoDB のテーブル）を {@link Builder} で渡し、 業務に沿った操作（「経路を登録する」など）を
 * {@link #execute(String, Object...)} で包んだ薄いクラスを書きます.
 *
 * <p>アプリが実行した SQL は、PostgreSQL のログから {@link StatementLog} が読み取ります。テストの準備・確認に使う接続は application_name
 * を {@value StatementLog#SETUP_APPLICATION_NAME} にして区別するため、{@link #execute} や {@link #query} の SQL
 * はアプリの SQL として記録しません.
 *
 * @author Copel Co., Ltd.
 */
public final class ItEnvironment {

  /** 最後に起動した実行環境です。{@link ItLogRecorder} がケースごとの SQL を読むために参照します. */
  private static volatile ItEnvironment current;

  /** コンテナ内で moto server が待ち受けるポートです. */
  private static final int MOTO_PORT = 5000;

  /** コンテナ内で PostgreSQL が待ち受けるポートです. */
  private static final int POSTGRES_PORT = 5432;

  /** SQL のログがコンテナのログに現れるのを待つ最大の時間です. */
  private static final Duration LOG_FLUSH_TIMEOUT = Duration.ofSeconds(10);

  /** SQS の本文を読むための ObjectMapper です. */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** コンテナです. */
  private final GenericContainer<?> container;

  /** AWS（moto）の接続先です. */
  private final String endpoint;

  /** 初期データの投入と送信結果の確認に使う SQS クライアントです. */
  private final AmazonSQS sqs;

  /** 初期データの投入と確認に使う DynamoDB クライアントです. */
  private final DynamoDbClient dynamoDb;

  /** キュー名から URL への対応です. */
  private final Map<String, String> queueUrls;

  /** {@link #reset()} で作り直す DynamoDB のテーブルです. */
  private final List<DynamoTable> dynamoTables;

  /** {@link #reset()} で空にする DB のテーブルです. */
  private final List<String> truncateTables;

  /**
   * 最後に起動した実行環境を返します。ログの記録がケースごとの SQL を読むために使います.
   *
   * @return 実行環境。まだ起動していない場合は null
   */
  public static ItEnvironment current() {
    return current;
  }

  /**
   * 実行環境を組み立てる Builder を返します.
   *
   * @param name 実行環境の名前。コンテナのイメージ名に使う（例: {@code line}）
   * @return Builder
   */
  public static Builder builder(final String name) {
    return new Builder(name);
  }

  /**
   * コンテナを起動し、ハンドラが起動時に読む設定値を SSM と S3 に投入します.
   *
   * <p>コンテナは JVM の終了時に Testcontainers が削除するため、ここでは停止しません.
   *
   * @param builder 利用側が指定した内容
   */
  @SuppressWarnings("resource")
  private ItEnvironment(final Builder builder) {
    int dbPort = Integer.parseInt(requiredEnv("DB_PORT"));
    String environment = requiredEnv("ENVIRONMENT");
    this.container =
        new GenericContainer<>(
                new ImageFromDockerfile("nectar-it-env-" + builder.name, false)
                    .withFileFromClasspath("Dockerfile", "nectar-it-env/Dockerfile")
                    .withFileFromClasspath("entrypoint.sh", "nectar-it-env/entrypoint.sh")
                    .withFileFromClasspath("ddl.sql", builder.ddlResource))
            .withEnv("POSTGRES_DB", requiredEnv("DB_NAME"))
            .withEnv("POSTGRES_USER", requiredEnv("DB_USERNAME"))
            .withEnv("POSTGRES_PASSWORD", requiredEnv("DB_PASSWORD"))
            .withExposedPorts(MOTO_PORT)
            // Core は DB_PORT を起動時に1回だけ読むため、PostgreSQL だけは固定のポートで公開する
            .withCreateContainerCmdModifier(
                cmd -> {
                  List<ExposedPort> exposedPorts = new ArrayList<>(List.of(cmd.getExposedPorts()));
                  exposedPorts.add(ExposedPort.tcp(POSTGRES_PORT));
                  cmd.withExposedPorts(exposedPorts);
                  Ports bindings = cmd.getHostConfig().getPortBindings();
                  bindings.bind(ExposedPort.tcp(POSTGRES_PORT), Ports.Binding.bindPort(dbPort));
                  cmd.getHostConfig().withPortBindings(bindings);
                })
            // 初期化中の PostgreSQL は TCP で待ち受けないため、TCP での接続と moto の応答の両方を待つ。
            // CI のランナーは手元より遅いことがあるため、既定の60秒より長く待つ
            .waitingFor(
                Wait.forSuccessfulCommand(
                        "pg_isready -h localhost -U \"$POSTGRES_USER\" && python3 -c \"import"
                            + " urllib.request; urllib.request.urlopen('http://localhost:5000/moto-api/')\"")
                    .withStartupTimeout(Duration.ofMinutes(3)));
    this.container.start();

    this.endpoint =
        "http://" + this.container.getHost() + ":" + this.container.getMappedPort(MOTO_PORT);
    // Core の AWS クライアントは、システムプロパティ aws.endpointUrl があればその接続先を使う
    System.setProperty("aws.endpointUrl", this.endpoint);

    StaticCredentialsProvider credentials =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    this.sqs =
        AmazonSQSClientBuilder.standard()
            .withEndpointConfiguration(new EndpointConfiguration(this.endpoint, "ap-northeast-1"))
            .withCredentials(
                new AWSStaticCredentialsProvider(new BasicAWSCredentials("test", "test")))
            .build();
    this.dynamoDb =
        DynamoDbClient.builder()
            .endpointOverride(URI.create(this.endpoint))
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(credentials)
            .build();
    this.dynamoTables = List.copyOf(builder.dynamoTables);
    this.truncateTables = List.copyOf(builder.truncateTables);

    Map<String, String> ssmParameters = new LinkedHashMap<>(builder.ssmParameters);
    this.queueUrls = new LinkedHashMap<>();
    builder.queues.forEach(
        (queueName, ssmKey) -> {
          String url = this.sqs.createQueue(queueName).getQueueUrl();
          this.queueUrls.put(queueName, url);
          ssmParameters.put(ssmKey, url);
        });
    if (builder.configBucket != null) {
      ssmParameters.put(builder.configBucketSsmKey, builder.configBucket);
    }

    // 本番と同じく、キューの URL と設定ファイルのバケット名は SSM に、設定ファイルは S3 に置く
    try (SsmClient ssm =
            SsmClient.builder()
                .endpointOverride(URI.create(this.endpoint))
                .region(Region.AP_NORTHEAST_1)
                .credentialsProvider(credentials)
                .build();
        S3Client s3 =
            S3Client.builder()
                .endpointOverride(URI.create(this.endpoint))
                .region(Region.AP_NORTHEAST_1)
                .credentialsProvider(credentials)
                .forcePathStyle(true)
                .build()) {
      ssmParameters.forEach(
          (key, value) ->
              ssm.putParameter(
                  p ->
                      p.name("/nectar/" + environment + "/" + key)
                          .value(value)
                          .type(ParameterType.STRING)
                          .overwrite(true)));
      if (builder.configBucket != null) {
        s3.createBucket(b -> b.bucket(builder.configBucket));
        s3.putObject(
            b -> b.bucket(builder.configBucket).key("config.properties"),
            RequestBody.fromString(builder.configProperties));
      }
    }
    current = this;
  }

  /**
   * 前のテストのデータを消し、DB・SQS・DynamoDB を空の状態に戻します.
   *
   * <p>DynamoDB のテーブルは削除してすぐ作り直します。moto は削除をすぐに終えるためで、実際の AWS では削除の完了を待つ必要があります.
   *
   * @throws SQLException DB の操作に失敗した場合
   * @throws IOException キューに残ったメッセージを読めない場合
   */
  public void reset() throws SQLException, IOException {
    if (!this.truncateTables.isEmpty()) {
      try (Connection connection = this.openConnection();
          Statement statement = connection.createStatement()) {
        statement.executeUpdate("TRUNCATE " + String.join(", ", this.truncateTables));
      }
    }

    // PurgeQueue は60秒に1回しか実行できないため、残ったメッセージを受け取って消す
    for (String queueName : this.queueUrls.keySet()) {
      this.receiveMessages(queueName);
    }

    for (DynamoTable table : this.dynamoTables) {
      try {
        this.dynamoDb.deleteTable(b -> b.tableName(table.name()));
      } catch (ResourceNotFoundException e) {
        // 初回はテーブルが無い
      }
      this.dynamoDb.createTable(table.toCreateRequest());
    }
  }

  /**
   * テストの準備のための SQL（INSERT など）を実行します。Entity は使わず、テストが SQL で直接データを入れます.
   *
   * @param sql SQL。値は {@code ?} でバインドする
   * @param parameters バインドする値。順序は SQL の {@code ?} に対応する
   * @throws SQLException DB の操作に失敗した場合
   */
  public void execute(final String sql, final Object... parameters) throws SQLException {
    try (Connection connection = this.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);
      statement.execute();
    }
  }

  /**
   * テストの結果の確認のための SQL（SELECT）を実行し、行を返します.
   *
   * @param sql SQL。値は {@code ?} でバインドする
   * @param parameters バインドする値。順序は SQL の {@code ?} に対応する
   * @return 行のリスト。各行は列名（小文字）から値への対応
   * @throws SQLException DB の操作に失敗した場合
   */
  public List<Map<String, Object>> query(final String sql, final Object... parameters)
      throws SQLException {
    try (Connection connection = this.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);
      try (ResultSet resultSet = statement.executeQuery()) {
        ResultSetMetaData metaData = resultSet.getMetaData();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= metaData.getColumnCount(); i++) {
            row.put(metaData.getColumnLabel(i).toLowerCase(Locale.ROOT), resultSet.getObject(i));
          }
          rows.add(row);
        }
        return rows;
      }
    }
  }

  /**
   * キューに届いたメッセージの本文を、すべて受け取って返します。受け取ったメッセージはキューから消します.
   *
   * <p>moto は送信と同時に受信できる状態にするため、待たずに受け取ります。1回の受信は最大10件のため、キューが空になるまで受け取ります。 届いていない場合は空のリストを返します.
   *
   * @param queueName {@link Builder#queue(String, String)} で指定したキューの名前
   * @return メッセージの本文（JSON）のリスト
   * @throws IOException 本文を JSON として読めない場合
   */
  public List<JsonNode> receiveMessages(final String queueName) throws IOException {
    String queueUrl =
        Objects.requireNonNull(this.queueUrls.get(queueName), "未定義のキューです: " + queueName);
    List<JsonNode> bodies = new ArrayList<>();
    List<Message> messages;
    do {
      messages =
          this.sqs
              .receiveMessage(
                  new ReceiveMessageRequest(queueUrl)
                      .withMaxNumberOfMessages(10)
                      .withWaitTimeSeconds(0))
              .getMessages();
      for (Message message : messages) {
        bodies.add(OBJECT_MAPPER.readTree(message.getBody()));
        this.sqs.deleteMessage(queueUrl, message.getReceiptHandle());
      }
    } while (!messages.isEmpty());
    return bodies;
  }

  /**
   * DynamoDB のクライアントを返します。項目の確認に使います.
   *
   * @return コンテナの moto に接続した DynamoDB のクライアント
   */
  public DynamoDbClient dynamoDb() {
    return this.dynamoDb;
  }

  /**
   * これまでにアプリが実行した SQL の件数を返します。ケースの開始時に呼び、ケースの終了時の {@link #statementsSince(int)} の起点にします.
   *
   * @return アプリが実行した SQL の件数
   */
  public int statementCount() {
    return this.readStatements().size();
  }

  /**
   * 起点より後にアプリが実行した SQL を、実行順に返します.
   *
   * @param from {@link #statementCount()} で得た起点
   * @return アプリが実行した SQL のリスト
   */
  public List<StatementLog.Statement> statementsSince(final int from) {
    List<StatementLog.Statement> all = this.readStatements();
    return all.subList(Math.min(from, all.size()), all.size());
  }

  /**
   * PostgreSQL のログがコンテナのログに現れるのを待ってから、アプリが実行した SQL をすべて読み取ります.
   *
   * <p>印を付ける SELECT をテスト用の接続で実行し、その印がログに現れた時点で、それより前の SQL はすべて読み取れたとみなします.
   *
   * @return アプリが実行した SQL のリスト
   */
  private List<StatementLog.Statement> readStatements() {
    String marker = "it-flush-" + UUID.randomUUID();
    try {
      this.execute("SELECT '" + marker + "'");
      long deadline = System.nanoTime() + LOG_FLUSH_TIMEOUT.toNanos();
      String logs = this.container.getLogs();
      while (!logs.contains(marker) && System.nanoTime() < deadline) {
        Thread.sleep(50);
        logs = this.container.getLogs();
      }
      return StatementLog.parse(logs);
    } catch (SQLException e) {
      throw new IllegalStateException("SQL のログを読み取れません", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("SQL のログの読み取り中に中断されました", e);
    }
  }

  /**
   * PreparedStatement に値をバインドします.
   *
   * @param statement バインド先
   * @param parameters バインドする値
   * @throws SQLException バインドに失敗した場合
   */
  private static void bind(final PreparedStatement statement, final Object[] parameters)
      throws SQLException {
    for (int i = 0; i < parameters.length; i++) {
      statement.setObject(i + 1, parameters[i]);
    }
  }

  /**
   * Core と同じ接続情報（環境変数）でコンテナの PostgreSQL に接続します.
   *
   * <p>application_name を {@value StatementLog#SETUP_APPLICATION_NAME} にし、アプリの SQL と区別します.
   *
   * @return 自動コミットの DB 接続
   * @throws SQLException 接続に失敗した場合
   */
  private Connection openConnection() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", requiredEnv("DB_USERNAME"));
    properties.setProperty("password", requiredEnv("DB_PASSWORD"));
    properties.setProperty("ApplicationName", StatementLog.SETUP_APPLICATION_NAME);
    return DriverManager.getConnection(
        "jdbc:postgresql://localhost:" + requiredEnv("DB_PORT") + "/" + requiredEnv("DB_NAME"),
        properties);
  }

  /**
   * 環境変数を読みます。IT1 の環境変数は pom（nectar-parent の failsafe）が設定するため、無ければ mvn verify 以外での実行です.
   *
   * @param name 環境変数の名前
   * @return 環境変数の値
   */
  private static String requiredEnv(final String name) {
    return Objects.requireNonNull(
        System.getenv(name), "環境変数 " + name + " がありません。IT1 は mvn verify で実行してください");
  }

  /**
   * 文字列をキーに持つ DynamoDB のテーブルの定義です.
   *
   * @param name テーブル名
   * @param partitionKey パーティションキーの属性名
   * @param sortKey ソートキーの属性名。無い場合は null
   */
  private record DynamoTable(String name, String partitionKey, String sortKey) {

    /**
     * テーブルを作る要求を返します.
     *
     * @return オンデマンド課金のテーブルの作成要求
     */
    CreateTableRequest toCreateRequest() {
      List<AttributeDefinition> attributes = new ArrayList<>();
      List<KeySchemaElement> keys = new ArrayList<>();
      attributes.add(string(this.partitionKey));
      keys.add(key(this.partitionKey, KeyType.HASH));
      if (this.sortKey != null) {
        attributes.add(string(this.sortKey));
        keys.add(key(this.sortKey, KeyType.RANGE));
      }
      return CreateTableRequest.builder()
          .tableName(this.name)
          .billingMode(BillingMode.PAY_PER_REQUEST)
          .attributeDefinitions(attributes)
          .keySchema(keys)
          .build();
    }

    private static AttributeDefinition string(final String attributeName) {
      return AttributeDefinition.builder()
          .attributeName(attributeName)
          .attributeType(ScalarAttributeType.S)
          .build();
    }

    private static KeySchemaElement key(final String attributeName, final KeyType type) {
      return KeySchemaElement.builder().attributeName(attributeName).keyType(type).build();
    }
  }

  /**
   * 実行環境の内容を組み立てる Builder です。利用側のモジュール固有の値だけを指定します.
   *
   * @author Copel Co., Ltd.
   */
  public static final class Builder {

    /** 実行環境の名前です. */
    private final String name;

    /** DB を初期化する DDL のクラスパス上の位置です. */
    private String ddlResource = "it-env/ddl.sql";

    /** SSM に置くパラメータです（キーは {@code /nectar/環境名/} より後ろ）. */
    private final Map<String, String> ssmParameters = new LinkedHashMap<>();

    /** 作成するキューです。キーはキュー名、値は URL を置く SSM のキーです. */
    private final Map<String, String> queues = new LinkedHashMap<>();

    /** 設定ファイル（config.properties）を置く S3 バケットの名前です。置かない場合は null. */
    private String configBucket;

    /** 設定ファイルのバケット名を置く SSM のキーです. */
    private String configBucketSsmKey;

    /** 設定ファイル（config.properties）の内容です. */
    private String configProperties = "";

    /** 作り直す DynamoDB のテーブルです. */
    private final List<DynamoTable> dynamoTables = new ArrayList<>();

    /** 空にする DB のテーブルです. */
    private final List<String> truncateTables = new ArrayList<>();

    private Builder(final String name) {
      this.name = name;
    }

    /**
     * DB を初期化する DDL を指定します。指定しない場合は {@code it-env/ddl.sql} を使います.
     *
     * @param classpathResource クラスパス上の DDL の位置。本番の DDL から、そのモジュールが使うテーブルの CREATE TABLE だけを写す
     * @return この Builder
     */
    public Builder ddl(final String classpathResource) {
      this.ddlResource = classpathResource;
      return this;
    }

    /**
     * SSM のパラメータを置きます.
     *
     * @param key {@code /nectar/環境名/} より後ろのキー
     * @param value 値
     * @return この Builder
     */
    public Builder ssmParameter(final String key, final String value) {
      this.ssmParameters.put(key, value);
      return this;
    }

    /**
     * SQS のキューを作り、その URL を SSM に置きます.
     *
     * @param queueName キューの名前
     * @param ssmKey URL を置く SSM のキー（{@code /nectar/環境名/} より後ろ）
     * @return この Builder
     */
    public Builder queue(final String queueName, final String ssmKey) {
      this.queues.put(queueName, ssmKey);
      return this;
    }

    /**
     * 設定ファイル（config.properties）を置く S3 バケットを作り、そのバケット名を SSM に置きます.
     *
     * @param bucketName バケット名
     * @param ssmKey バケット名を置く SSM のキー（{@code /nectar/環境名/} より後ろ）
     * @param configProperties config.properties の内容。設定値がすべて SSM にある場合は空文字
     * @return この Builder
     */
    public Builder configBucket(
        final String bucketName, final String ssmKey, final String configProperties) {
      this.configBucket = bucketName;
      this.configBucketSsmKey = ssmKey;
      this.configProperties = configProperties;
      return this;
    }

    /**
     * 文字列のキーを持つ DynamoDB のテーブルを、{@link ItEnvironment#reset()} のたびに作り直すように指定します.
     *
     * @param tableName テーブル名
     * @param partitionKey パーティションキーの属性名
     * @param sortKey ソートキーの属性名。無い場合は null
     * @return この Builder
     */
    public Builder dynamoTable(
        final String tableName, final String partitionKey, final String sortKey) {
      this.dynamoTables.add(new DynamoTable(tableName, partitionKey, sortKey));
      return this;
    }

    /**
     * {@link ItEnvironment#reset()} のたびに空にする DB のテーブルを指定します.
     *
     * @param tables テーブル名
     * @return この Builder
     */
    public Builder truncate(final String... tables) {
      this.truncateTables.addAll(List.of(tables));
      return this;
    }

    /**
     * コンテナを起動して実行環境を返します.
     *
     * @return 起動した実行環境
     */
    public ItEnvironment start() {
      return new ItEnvironment(this);
    }
  }
}
