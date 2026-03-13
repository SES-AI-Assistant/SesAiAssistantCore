package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SES_AI_T_WATCHLotTests {

  private Connection connection;

  @BeforeEach
  void setUp() throws Exception {
    // テスト実行ごとに新しいコネクションを取得
    connection = DBConnection.getConnection();
    connection.setAutoCommit(false); // トランザクション管理を有効化

    // テスト用のテーブルをクリーンアップ＆作成
    try (Statement stmt = connection.createStatement()) {
      // もしテーブルが存在すれば削除
      stmt.execute("DROP TABLE IF EXISTS SES_AI_T_WATCH");
      // テーブルを新規作成
      stmt.execute(
          "CREATE TABLE SES_AI_T_WATCH ("
              + "user_id VARCHAR(255) NOT NULL,"
              + "target_id VARCHAR(255) NOT NULL,"
              + "target_type VARCHAR(50),"
              + "memo VARCHAR(255),"
              + "register_date TIMESTAMP,"
              + "register_user VARCHAR(255),"
              + "ttl TIMESTAMP,"
              + "PRIMARY KEY (user_id, target_id))");
    } catch (SQLException e) {
      // H2 Databaseでの初回実行時など、IF EXISTSが使えない場合のエラーをハンドリング
      if (!e.getMessage().contains("Table \"SES_AI_T_WATCH\" not found")) {
        throw e;
      }
      // テーブルが存在しない場合は何もしない
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    if (connection != null && !connection.isClosed()) {
      try {
        // テストの変更をロールバックしてクリーンな状態に戻す
        connection.rollback();
      } catch (SQLException e) {
        // ロールバック失敗のログ
      } finally {
        try {
          connection.close();
        } catch (SQLException e) {
          // クローズ失敗のログ
        }
      }
    }
  }

  @Test
  void testDeleteExpired() throws Exception {
    // 1. テストデータの準備
    // 期限切れのデータ (TTLが1日前)
    SES_AI_T_WATCH expiredWatch = new SES_AI_T_WATCH();
    expiredWatch.setUserId("user01");
    expiredWatch.setTargetId("expired01");
    OriginalDateTime expiredTtl = new OriginalDateTime();
    expiredTtl.plusDays(-1); // 1日前の時刻
    expiredWatch.setTtl(expiredTtl);
    expiredWatch.setRegisterUser("test");
    expiredWatch.insert(connection);

    // 有効なデータ (TTLが1日後)
    SES_AI_T_WATCH validWatch = new SES_AI_T_WATCH();
    validWatch.setUserId("user02");
    validWatch.setTargetId("valid01");
    OriginalDateTime validTtl = new OriginalDateTime();
    validTtl.plusDays(1); // 1日後の時刻
    validWatch.setTtl(validTtl);
    validWatch.setRegisterUser("test");
    validWatch.insert(connection);

    connection.commit();

    // 2. 削除処理の実行
    SES_AI_T_WATCHLot lot = new SES_AI_T_WATCHLot();
    int deletedCount = lot.deleteExpired(connection);
    connection.commit();

    // 3. 検証
    assertEquals(1, deletedCount, "期限切れのレコードが1件削除されていること");

    // 期限切れデータが存在しないことを確認
    assertFalse(expiredWatch.isExist(connection), "期限切れのデータは削除されていること");

    // 有効なデータが存在することを確認
    assertTrue(validWatch.isExist(connection), "有効なデータは削除されていないこと");
  }
}
