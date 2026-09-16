package copel.sesproductpackage.core.util;

import copel.sesproductpackage.core.api.aws.SecretManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;

/**
 * 【フレームワーク部品】 DBコネクションを取得するためのクラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class DBConnection {
  private static String url;
  private static String userName;
  private static String password;

  static {
    try {
      initializeDbConnectionCredentials();
    } catch (Throwable e) {
      log.error("DB接続情報の初期化に失敗しました", e);
    }
  }

  private static void initializeDbConnectionCredentials() throws Exception {
    // ① 環境変数をまず確認（Lambda環境変数から取得）
    String host = System.getenv("DB_HOST");
    String port = System.getenv("DB_PORT");
    String username = System.getenv("DB_USERNAME");
    String password = System.getenv("DB_PASSWORD");
    String dbName = System.getenv("DB_NAME");

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

    url = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
    userName = username;
    DBConnection.password = password;
  }

  /**
   * DBコネクションを生成し返却します.
   *
   * @return DBコネクション
   * @throws SQLException
   * @throws ClassNotFoundException
   */
  public static Connection getConnection() throws SQLException, ClassNotFoundException {
    Connection connection = DriverManager.getConnection(url, userName, password);
    connection.setAutoCommit(false);
    return connection;
  }
}
