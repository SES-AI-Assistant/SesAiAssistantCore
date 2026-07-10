package copel.sesproductpackage.core.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import copel.sesproductpackage.core.api.aws.SecretManager;
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
      log.error("【SesAiAssitantCore】DB接続情報の初期化に失敗しました", e);
    }
  }

  private static void initializeDbConnectionCredentials() throws Exception {
    // ParameterStoreからSecretManagerのARNを取得する
    String secretArn = Properties.get(SsmParameterKey.RDS_DATABASE_SECRET_ARN.getKey());

    // SecretManagerからDB接続情報を取得する
    SecretManager secretManager = new SecretManager(secretArn);
    try {
      secretManager.load();
      log.info("SecretManagerからDB接続情報を取得しました");

      String host = secretManager.get("host");
      String port = secretManager.get("port");
      String dbName = secretManager.get("dbname");
      String username = secretManager.get("username");
      String pwd = secretManager.get("password");

      url = String.format("jdbc:mysql://%s:%s/%s", host, port, dbName);
      userName = username;
      password = pwd;

    } finally {
      secretManager.close();
    }
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
