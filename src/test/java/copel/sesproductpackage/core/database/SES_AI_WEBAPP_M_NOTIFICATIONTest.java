package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_WEBAPP_M_NOTIFICATION} の単体テスト。 */
class SES_AI_WEBAPP_M_NOTIFICATIONTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 通知デバイスIDの値。 */
  private static final String NOTIFICATION_ID = "N001";

  /** 登録日時の文字列。 */
  private static final String REGISTER_DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "notification_id",
    "user_id",
    "device_type",
    "device_name",
    "push_notification_endpoint",
    "p256dh",
    "auth",
    "enabled",
    "notify_all_match",
    "register_date",
    "register_user"
  };

  /**
   * 準備文のモックを作り、接続の prepareStatement がその準備文を返すように設定する。
   *
   * @param connection 接続のモック
   * @param updated executeUpdate が返す更新件数
   * @return 準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static PreparedStatement stubUpdate(final Connection connection, final int updated)
      throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.executeUpdate()).thenReturn(updated);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    return statement;
  }

  /**
   * 項目が揃った通知デバイスを生成する。有効フラグは true、全件通知フラグは false とする。
   *
   * @return 生成した通知デバイス
   */
  private static SES_AI_WEBAPP_M_NOTIFICATION notification() {
    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION(TENANT_ID);
    notification.setNotificationId(NOTIFICATION_ID);
    notification.setUserId("U001");
    notification.setDeviceType("web");
    notification.setDeviceName("Chrome");
    notification.setPushNotificationEndpoint("https://push.example.com/abc");
    notification.setP256dh("p256dh-key");
    notification.setAuth("auth-token");
    notification.setEnabled(true);
    notification.setNotifyAllMatch(false);
    notification.setRegisterDate(new OriginalDateTime(REGISTER_DATE_TEXT));
    notification.setRegisterUser("U000");
    return notification;
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 通知デバイスをテナントの条件で検索する。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_WEBAPP_M_NOTIFICATION を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_WEBAPP_M_NOTIFICATION(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無い通知デバイスは他テナントの通知先になる。
       *
       * @given テナントIDが null
       * @when SES_AI_WEBAPP_M_NOTIFICATION を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_WEBAPP_M_NOTIFICATION(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * 通知デバイスの項目を列の順に設定し、テナントIDを加えて登録することを確かめる。 テナントIDの無い登録は他テナントの通知先と区別できない。
       *
       * @given 通知デバイスIDが "N001"、有効フラグが true、全件通知フラグが false、登録日時が "2026-01-15 10:00:00"、テナントIDが
       *     "T001"、更新件数が 1 件
       * @when insert を接続で実行する
       * @then tenant_id を含む INSERT 文を準備し、有効フラグを true、全件通知フラグを false に設定して1件を返す
       */
      @MockDbConnection
      @Test
      void 項目が揃っている場合_テナントIDを含めて1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(notification().insert(connection)).isEqualTo(1);

        verify(connection)
            .prepareStatement(
                "INSERT INTO SES_AI_WEBAPP_M_NOTIFICATION (notification_id, user_id, device_type,"
                    + " device_name, push_notification_endpoint, p256dh, auth, enabled,"
                    + " notify_all_match, register_date, register_user, tenant_id) VALUES (?, ?,"
                    + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        verify(statement).setString(1, NOTIFICATION_ID);
        verify(statement).setString(5, "https://push.example.com/abc");
        verify(statement).setBoolean(8, true);
        verify(statement).setBoolean(9, false);
        verify(statement).setTimestamp(10, Timestamp.valueOf(REGISTER_DATE_TEXT));
        verify(statement).setString(12, TENANT_ID);
      }

      /**
       * 有効フラグが false で全件通知フラグが true の場合に、それぞれの値を設定することを確かめる。 通知の有効と全件通知を別々に保存する。
       *
       * @given 有効フラグが false、全件通知フラグが true、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 有効フラグに false、全件通知フラグに true を設定して1件を返す
       */
      @MockDbConnection
      @Test
      void 有効フラグがfalseで全件通知がtrueの場合_各フラグを設定して1件を返す(final Connection connection)
          throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_WEBAPP_M_NOTIFICATION notification = notification();
        notification.setEnabled(false);
        notification.setNotifyAllMatch(true);

        assertThat(notification.insert(connection)).isEqualTo(1);

        verify(statement).setBoolean(8, false);
        verify(statement).setBoolean(9, true);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 有効フラグと全件通知フラグと登録日時が未設定の場合に、フラグを false、日時を null として登録することを確かめる。 未設定の項目があっても登録を失敗させない。
       *
       * @given 有効フラグが null、全件通知フラグが null、登録日時が未設定、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 有効フラグと全件通知フラグを false、登録日時を null に設定して1件を返す
       */
      @MockDbConnection
      @Test
      void フラグと登録日時が未設定の場合_フラグをfalseにして1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_WEBAPP_M_NOTIFICATION notification = notification();
        notification.setEnabled(null);
        notification.setNotifyAllMatch(null);
        notification.setRegisterDate(null);

        assertThat(notification.insert(connection)).isEqualTo(1);

        verify(statement).setBoolean(8, false);
        verify(statement).setBoolean(9, false);
        verify(statement).setTimestamp(10, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * 通知デバイスIDとテナントIDが一致する行の項目を読み込むことを確かめる。 保存済みの通知先を編集画面に表示する。
       *
       * @given 通知デバイスIDが "N001"、SELECT の結果が 1 行有効フラグ true、全件通知フラグ false
       * @when selectByPk を接続で実行する
       * @then 読み込んだ項目で、有効フラグを true、全件通知フラグを false に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_項目を更新する(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(
                    NOTIFICATION_ID,
                    "U001",
                    "web",
                    "Chrome",
                    "https://push.example.com/abc",
                    "p256dh-key",
                    "auth-token",
                    true,
                    false,
                    REGISTER_DATE_TEXT,
                    "U000")
                .build());
        SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION(TENANT_ID);
        notification.setNotificationId(NOTIFICATION_ID);

        notification.selectByPk(connection);

        assertThat(notification.getUserId()).isEqualTo("U001");
        assertThat(notification.getEnabled()).isTrue();
        assertThat(notification.getNotifyAllMatch()).isFalse();
      }

      /**
       * 該当行が無い場合に項目を変えないことを確かめる。 存在しない通知先を読み込んでも、入力中の値を壊さない。
       *
       * @given 通知デバイスIDが "N001"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then ユーザーIDを変えずに何もしない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_何もしない(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION(TENANT_ID);
        notification.setNotificationId(NOTIFICATION_ID);

        notification.selectByPk(connection);

        assertThat(notification.getUserId()).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 通知デバイスIDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返す恐れがある。
       *
       * @given 通知デバイスIDが null
       * @when selectByPk を接続で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 通知デバイスIDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_WEBAPP_M_NOTIFICATION(TENANT_ID).selectByPk(connection);

        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントの通知デバイスを読まないことを確かめる。 他テナントの通知先を読むと、通知の内容が漏えいする。
       *
       * @given 通知デバイスIDが "N001"、テナントIDが "T001"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then 条件の2番目に "T001" を設定し、何もしない
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_何もしない(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_WEBAPP_M_NOTIFICATION notification = notification();

        notification.selectByPk(connection);

        verify(statement).setString(2, TENANT_ID);
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * 通知デバイスIDとテナントIDが一致する行を、項目の値で更新することを確かめる。 通知先の設定変更を保存する。
       *
       * @given 通知デバイスIDが "N001"、有効フラグが true、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then tenant_id を条件に含めた UPDATE 文を準備し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(notification().updateByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_WEBAPP_M_NOTIFICATION SET notification_id = ?, user_id = ?,"
                    + " device_type = ?, device_name = ?, push_notification_endpoint = ?,"
                    + " p256dh = ?, auth = ?, enabled = ?, notify_all_match = ?, register_date = ?,"
                    + " register_user = ? WHERE notification_id = ? AND tenant_id = ?");
        verify(statement).setString(12, NOTIFICATION_ID);
      }

      /**
       * 有効フラグが false で全件通知フラグが true の場合に、それぞれの値を設定して更新することを確かめる。 通知の設定変更を確実に保存するためである。
       *
       * @given 有効フラグが false、全件通知フラグが true、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 有効フラグに false、全件通知フラグに true を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 有効フラグがfalseで全件通知がtrueの場合_各フラグを設定してtrueを返す(final Connection connection)
          throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_WEBAPP_M_NOTIFICATION notification = notification();
        notification.setEnabled(false);
        notification.setNotifyAllMatch(true);

        assertThat(notification.updateByPk(connection)).isTrue();

        verify(statement).setBoolean(8, false);
        verify(statement).setBoolean(9, true);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象の通知デバイスが無いときに更新成功と誤認しない。
       *
       * @given 通知デバイスIDが "N001"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(notification().updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 通知デバイスIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全件を書き換えるおそれがある。
       *
       * @given 通知デバイスIDが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 通知デバイスIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_WEBAPP_M_NOTIFICATION(TENANT_ID).updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 有効フラグと全件通知フラグと登録日時が未設定の場合に、フラグを false、日時を null として更新することを確かめる。 未設定の項目があっても更新を失敗させない。
       *
       * @given 有効フラグが null、全件通知フラグが null、登録日時が未設定、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 有効フラグと全件通知フラグを false、登録日時を null に設定してtrueを返す
       */
      @MockDbConnection
      @Test
      void フラグと登録日時が未設定の場合_フラグをfalseにしてtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_WEBAPP_M_NOTIFICATION notification = notification();
        notification.setEnabled(null);
        notification.setNotifyAllMatch(null);
        notification.setRegisterDate(null);

        assertThat(notification.updateByPk(connection)).isTrue();

        verify(statement).setBoolean(8, false);
        verify(statement).setBoolean(9, false);
        verify(statement).setTimestamp(10, null);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 更新の条件にテナントIDを含め、他テナントの通知デバイスを更新しないことを確かめる。 他テナントの通知先を書き換えると、通知が別の利用者に届く。
       *
       * @given 通知デバイスIDが "N001"、テナントIDが "T001"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 条件の2番目に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(notification().updateByPk(connection)).isTrue();

        verify(statement).setString(13, TENANT_ID);
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * 通知デバイスIDとテナントIDが一致する行を削除することを確かめる。 解除したデバイスへ通知を送らない。
       *
       * @given 通知デバイスIDが "N001"、テナントIDが "T001"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then tenant_id を条件に含めて削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(notification().deleteByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "DELETE FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE notification_id = ? AND tenant_id = ?");
        verify(statement).setString(1, NOTIFICATION_ID);
        verify(statement).setString(2, TENANT_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象の通知デバイスが無いときに削除成功と誤認しない。
       *
       * @given 通知デバイスIDが "N001"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(notification().deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 通知デバイスIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全件を消すおそれがある。
       *
       * @given 通知デバイスIDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 通知デバイスIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_WEBAPP_M_NOTIFICATION(TENANT_ID).deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }
}
