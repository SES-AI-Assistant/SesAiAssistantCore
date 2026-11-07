package copel.sesproductpackage.core.util;

import copel.sesproductpackage.core.api.aws.SecretManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;

/**
 * 【フレームワーク部品】 DBコネクションを取得するためのクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class DBConnection {
  /** インスタンス化を禁止する。すべての操作は静的メンバーで提供する。 */
  private DBConnection() {}

  /** 接続先の URL。初期化の最後に設定するため、null でない場合は他の接続情報も設定済みである. */
  private static volatile String url;

  private static String userName;
  private static String password;

  /** 最後に接続情報の初期化を試みた時刻. 再試行の間隔を空けるために使う. */
  private static long lastInitializeAttemptTime = 0;

  /**
   * 接続情報が未設定のときに初期化を再試行する最短の間隔（ミリ秒）.
   *
   * <p>初期化に失敗し続けている間、リクエストのたびに Secrets Manager と Parameter Store を呼ぶと、失敗の原因になっている負荷をさらに増やす。1
   * 秒空けて、同時に来た要求が一度の再試行を共有できるようにする。
   */
  static final long RETRY_INTERVAL_MS = 1000;

  /**
   * DB への接続を待つ最長の秒数（JDBC の connectTimeout）.
   *
   * <p>DB が応答しないまま接続を待ち続けると、Lambda の実行時間を使い切り、同時に開いている接続も増え続ける。接続だけに期限を付け、重い検索を止める socketTimeout
   * は付けない。
   */
  static final int CONNECT_TIMEOUT_SECONDS = 5;

  /**
   * memory のときに使う接続生成関数。テストが {@link #setConnectionFactoryForTest(ConnectionFactory)} で設定し、未設定なら
   * null。
   */
  private static ConnectionFactory memoryConnectionFactory;

  static {
    initializeAtStartup(System::getenv);
  }

  /**
   * クラス初期化時の DB 接続情報の読み込みを行います。
   *
   * <p>本番では、環境変数または Secrets Manager から接続情報を読む。例外は握りつぶしてログに出す。失敗した場合は、{@link #getConnection()}
   * が次の呼び出しで再実行する。
   *
   * <p>読み込み元が memory のときは何もしない。Secrets Manager に触れないため、オフラインでも即座に完了する。
   *
   * @param environment 環境変数を読む関数。本番では {@code System::getenv} を渡す。テストでは任意の値を返す関数を渡す
   */
  static void initializeAtStartup(final UnaryOperator<String> environment) {
    if (ConfigSource.INSTANCE.isMemory()) {
      log.info("設定の読み込み元が memory のため、DB 接続情報の読み込みを行いません。");
      return;
    }
    lastInitializeAttemptTime = ApplicationClock.INSTANCE.clock().millis();
    try {
      initializeDbConnectionCredentials(environment);
    } catch (Throwable e) {
      log.error("DB接続情報の初期化に失敗しました", e);
    }
  }

  private static void initializeDbConnectionCredentials(final UnaryOperator<String> environment)
      throws Exception {
    // ① 環境変数をまず確認（Lambda環境変数から取得）
    String host = environment.apply("DB_HOST");
    String port = environment.apply("DB_PORT");
    String username = environment.apply("DB_USERNAME");
    String password = environment.apply("DB_PASSWORD");
    String dbName = environment.apply("DB_NAME");

    // ② 環境変数がなければ Secrets Manager から取得（従来の流れ）
    if (host == null || host.isEmpty()) {
      log.info("環境変数が見つかりません。Secrets Manager から取得します");

      String secretArn = Properties.get(SsmParameterKey.RDS_DATABASE_SECRET_ARN.getKey());
      SecretManager secretManager = new SecretManager(secretArn);
      try {
        secretManager.load();
        log.info("SecretManagerからDB接続情報を取得しました");

        host = secretManager.get("host");
        port = secretManager.get("port");
        dbName = secretManager.get("dbname");
        username = secretManager.get("username");
        password = secretManager.get("password");

      } finally {
        secretManager.close();
      }
    } else {
      log.info("環境変数からDB接続情報を取得しました");
    }

    userName = username;
    DBConnection.password = password;
    // 他の接続情報を設定してから URL を設定する。URL が null でないことで、初期化の完了を判定できるようにするため
    url =
        String.format(
            "jdbc:postgresql://%s:%s/%s?connectTimeout=%d",
            host, port, dbName, CONNECT_TIMEOUT_SECONDS);
  }

  /**
   * DBコネクションを生成し返却します.
   *
   * <p>本番では、毎回 {@link DriverManager} で新しい接続を生成する。プールは持たない。
   *
   * <p>読み込み元が memory のときは、{@link #setConnectionFactoryForTest(ConnectionFactory)}
   * で設定した接続生成関数で接続を生成する。 設定が無い場合は、ネットワークに接続せず {@link SQLException} を投げる。memory で DB
   * に接続することを防ぐためである。
   *
   * <p>本番で接続情報の初期化に失敗している場合は、{@link #RETRY_INTERVAL_MS} 以上空けて初期化をやり直す。コールドスタートで Parameter Store
   * が制限にかかって初期化に失敗しても、Lambda の実行環境が破棄されるまで DB に接続できない状態が続かないようにするためである（#392）。
   *
   * @return DBコネクション
   * @throws SQLException 接続の生成に失敗した場合、memory で接続生成関数が未設定か null を返した場合、または本番で接続情報を初期化できていない場合
   * @throws ClassNotFoundException ドライバが見つからない場合
   */
  public static Connection getConnection() throws SQLException, ClassNotFoundException {
    Connection connection;
    if (ConfigSource.INSTANCE.isMemory()) {
      if (memoryConnectionFactory == null) {
        throw new SQLException(
            "memory 設定では DB に接続しません。テストでは setConnectionFactoryForTest で接続生成関数を設定してください。");
      }
      connection = memoryConnectionFactory.create();
      if (connection == null) {
        throw new SQLException("接続生成関数が null を返しました。");
      }
    } else {
      // 同時に来た要求のうち、先頭の1件だけが初期化をやり直す。待っていた要求は、初期化済みの url を使う
      synchronized (DBConnection.class) {
        if (url == null
            && ApplicationClock.INSTANCE.clock().millis() - lastInitializeAttemptTime
                >= RETRY_INTERVAL_MS) {
          log.info("DB 接続情報が未設定のため、初期化をやり直します。");
          initializeAtStartup(System::getenv);
        }
      }
      String connectionUrl = url;
      if (connectionUrl == null) {
        throw new SQLException(
            "DB 接続情報を初期化できていません。Parameter Store と Secrets Manager の取得に失敗している可能性があります。");
      }
      connection = DriverManager.getConnection(connectionUrl, userName, password);
    }
    connection.setAutoCommit(false);
    return connection;
  }

  /**
   * テスト用に接続生成関数を設定します。読み込み元が memory の場合だけ許可されます。
   *
   * <p>{@link #getConnection()} が memory のときに使う接続の取得元を、テストから差し替えるための口である。null を渡すと設定を解除し、未設定の状態に戻る。
   *
   * @param factory 接続生成関数。null で解除する
   * @throws IllegalStateException 読み込み元が memory 以外の場合
   */
  public static void setConnectionFactoryForTest(final ConnectionFactory factory) {
    ConfigSource.INSTANCE.requireMemory("接続生成関数の設定");
    memoryConnectionFactory = factory;
  }

  /**
   * DBコネクションの生成処理を表す関数型インターフェースです。
   *
   * <p>読み込み元が memory のときに {@link #getConnection()} が使う接続の取得元の型である。テストでは、モックの接続を返す関数を設定する。
   *
   * @author Copel Co., Ltd.
   */
  @FunctionalInterface
  public interface ConnectionFactory {

    /**
     * DBコネクションを生成します。
     *
     * @return 生成した DB コネクション
     * @throws SQLException 接続の生成に失敗した場合
     * @throws ClassNotFoundException ドライバが見つからない場合
     */
    Connection create() throws SQLException, ClassNotFoundException;
  }
}
