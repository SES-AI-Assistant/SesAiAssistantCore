package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_T_WATCH.TargetType;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_T_WATCH} の単体テスト。 */
class SES_AI_T_WATCHTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** ユーザーID。 */
  private static final String USER_ID = "U001";

  /** 対象ID。 */
  private static final String TARGET_ID = "J001";

  /** 登録日時と有効期限の文字列。 */
  private static final String DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "user_id", "target_id", "target_type", "memo", "register_date", "register_user", "ttl"
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
   * 項目が揃ったウォッチを生成する。対象種別は JOB、有効期限は指定の値とする。
   *
   * @return 生成したウォッチ
   */
  private static SES_AI_T_WATCH watch() {
    SES_AI_T_WATCH watch = new SES_AI_T_WATCH(TENANT_ID);
    watch.setUserId(USER_ID);
    watch.setTargetId(TARGET_ID);
    watch.setTargetType(TargetType.JOB);
    watch.setMemo("メモ");
    watch.setRegisterUser("U000");
    watch.setTtl(new OriginalDateTime(DATE_TEXT));
    return watch;
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 ウォッチをテナントの条件で管理する。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_T_WATCH を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_T_WATCH(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無いウォッチは他テナントの通知対象になる。
       *
       * @given テナントIDが null
       * @when SES_AI_T_WATCH を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_WATCH(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class isExist {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDと対象IDの組が一致する行がある場合に true を返すことを確かめる。 同じ対象を二重にウォッチしないための判定である。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、EXISTS の結果が true
       * @when isExist を接続で実行する
       * @then trueを返す
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection, MockResultSetBuilder.builder().columns("exists").row(true).build());

        assertThat(watch().isExist(connection)).isTrue();
      }

      /**
       * ユーザーIDと対象IDの組が一致する行が無い場合に false を返すことを確かめる。 未登録の対象は新しくウォッチに登録する。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、EXISTS の結果が 0 行
       * @when isExist を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns("exists").build());

        assertThat(watch().isExist(connection)).isFalse();
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 判定の条件にテナントIDを含め、他テナントのウォッチを存在とみなさないことを確かめる。 他テナントの同じ対象を見つけると、登録を省いてしまう。
       *
       * @given テナントIDが "T001"、EXISTS の結果が true
       * @when isExist を接続で実行する
       * @then SQL の tenant_id の条件に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection, MockResultSetBuilder.builder().columns("exists").row(true).build());

        assertThat(watch().isExist(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "SELECT EXISTS (SELECT 1 FROM SES_AI_T_WATCH WHERE user_id = ? AND target_id = ?"
                    + " AND tenant_id = ?)");
        verify(statement).setString(3, TENANT_ID);
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * ウォッチの項目を列の順に設定し、現在時刻を登録日時にしてテナントIDを加えて登録することを確かめる。 登録日時の無いウォッチは一覧の並びに使えない。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、対象種別が JOB、有効期限が "2026-01-15 10:00:00"、現在時刻が 2026-01-15
       *     12:00、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 登録日時に現在時刻を設定し、テナントIDを含めて1件を返す
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @MockDbConnection
      @Test
      void 項目が揃っている場合_テナントIDを含めて1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(watch().insert(connection)).isEqualTo(1);

        verify(connection)
            .prepareStatement(
                "INSERT INTO SES_AI_T_WATCH (user_id, target_id, target_type, memo, register_date,"
                    + " register_user, ttl, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        verify(statement).setString(1, USER_ID);
        verify(statement).setString(3, "JOB");
        verify(statement).setTimestamp(5, Timestamp.valueOf("2026-01-15 12:00:00"));
        verify(statement).setTimestamp(7, Timestamp.valueOf(DATE_TEXT));
        verify(statement).setString(8, TENANT_ID);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対象種別と有効期限が未設定の場合に、各項目の位置に null を設定して登録することを確かめる。 未設定の項目があっても登録を失敗させない。
       *
       * @given 対象種別が null、有効期限が null、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 対象種別と有効期限の位置に null を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void 対象種別と有効期限が未設定の場合_nullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_WATCH watch = watch();
        watch.setTargetType(null);
        watch.setTtl(null);

        assertThat(watch.insert(connection)).isEqualTo(1);

        verify(statement).setString(3, null);
        verify(statement).setTimestamp(7, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDと対象IDとテナントIDが一致する行の項目を読み込むことを確かめる。 保存済みのウォッチを編集画面に表示する。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、SELECT の結果が 1 行対象種別 "JOB"
       * @when selectByPk を接続で実行する
       * @then 対象種別を JOB に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_項目を更新する(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(USER_ID, TARGET_ID, "JOB", "メモ", DATE_TEXT, "U000", DATE_TEXT)
                .build());
        SES_AI_T_WATCH watch = watch();
        watch.setTargetType(null);

        watch.selectByPk(connection);

        assertThat(watch.getTargetType()).isEqualTo(TargetType.JOB);
        assertThat(watch.getMemo()).isEqualTo("メモ");
      }

      /**
       * 該当行が無い場合に項目を変えないことを確かめる。 存在しないウォッチを読み込んでも、入力中の値を壊さない。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then 対象種別を変えずに何もしない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_何もしない(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_WATCH watch = watch();

        watch.selectByPk(connection);

        assertThat(watch.getTargetType()).isEqualTo(TargetType.JOB);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込んだ対象種別が列挙値に無い場合に null を返すことを確かめる。 現状の対象種別の扱いを固定し、例外で止めない。
       *
       * @given SELECT の結果の対象種別が "UNKNOWN" の 1 行
       * @when selectByPk を接続で実行する
       * @then 対象種別としてnullを返す
       */
      @MockDbConnection
      @Test
      void 不正な対象種別の場合_対象種別としてnullを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(USER_ID, TARGET_ID, "UNKNOWN", "メモ", DATE_TEXT, "U000", DATE_TEXT)
                .build());
        SES_AI_T_WATCH watch = watch();

        watch.selectByPk(connection);

        assertThat(watch.getTargetType()).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは他のユーザーのウォッチを返すおそれがある。
       *
       * @given ユーザーIDが null、対象IDが "J001"
       * @when selectByPk を接続で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        SES_AI_T_WATCH watch = watch();
        watch.setUserId(null);

        watch.selectByPk(connection);

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 対象IDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは他の対象のウォッチを返すおそれがある。
       *
       * @given ユーザーIDが "U001"、対象IDが null
       * @when selectByPk を接続で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 対象IDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        SES_AI_T_WATCH watch = watch();
        watch.setTargetId(null);

        watch.selectByPk(connection);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDと対象IDが一致する行の項目を更新することを確かめる。 メモや有効期限の変更を保存する。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then tenant_id を条件に含めて更新し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(watch().updateByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_T_WATCH SET target_type = ?, memo = ?, register_date = ?,"
                    + " register_user = ?, ttl = ? WHERE user_id = ? AND target_id = ? AND"
                    + " tenant_id = ?");
        verify(statement).setString(1, "JOB");
        verify(statement).setString(2, "メモ");
        verify(statement).setString(6, USER_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象のウォッチが無いときに更新成功と誤認しない。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(watch().updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対象種別と有効期限が未設定の場合に、各項目の位置に null を設定して更新することを確かめる。 未設定の項目があっても更新を失敗させない。
       *
       * @given 対象種別が null、有効期限が null、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 対象種別と有効期限の位置に null を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 対象種別と有効期限が未設定の場合_nullを設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_WATCH watch = watch();
        watch.setTargetType(null);
        watch.setTtl(null);

        assertThat(watch.updateByPk(connection)).isTrue();

        verify(statement).setString(1, null);
        verify(statement).setTimestamp(5, null);
      }

      /**
       * ユーザーIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は別のウォッチを書き換えるおそれがある。
       *
       * @given ユーザーIDが null、対象IDが "J001"
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_T_WATCH watch = watch();
        watch.setUserId(null);

        assertThat(watch.updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 対象IDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は別のウォッチを書き換えるおそれがある。
       *
       * @given ユーザーIDが "U001"、対象IDが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 対象IDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_T_WATCH watch = watch();
        watch.setTargetId(null);

        assertThat(watch.updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 登録日時が未設定の場合に、登録日時の位置に null を設定して更新することを確かめる。 未設定の日時があっても更新を失敗させない。
       *
       * @given 登録日時が未設定、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 登録日時の位置に null を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 登録日時が未設定の場合_登録日時にnullを設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_WATCH watch = watch();
        watch.setRegisterDate(null);

        assertThat(watch.updateByPk(connection)).isTrue();

        verify(statement).setTimestamp(3, null);
      }

      /**
       * 登録日時がある場合に、登録日時の位置に時刻を設定して更新することを確かめる。 登録日時を変えずに保存できるようにする。
       *
       * @given 登録日時が "2026-01-15 10:00:00"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 登録日時の位置に時刻を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 登録日時がある場合_登録日時を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_WATCH watch = watch();
        watch.setRegisterDate(new OriginalDateTime("2026-01-15 10:00:00"));

        assertThat(watch.updateByPk(connection)).isTrue();

        verify(statement).setTimestamp(3, Timestamp.valueOf("2026-01-15 10:00:00"));
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 更新の条件にテナントIDを含め、他テナントのウォッチを更新しないことを確かめる。 他テナントのウォッチを書き換えると、通知の対象が変わる。
       *
       * @given テナントIDが "T001"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 条件の8番目に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(watch().updateByPk(connection)).isTrue();

        verify(statement).setString(8, TENANT_ID);
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDと対象IDとテナントIDが一致する行を削除することを確かめる。 監視を止めた対象へ通知を送らない。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then tenant_id を条件に含めて削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(watch().deleteByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "DELETE FROM SES_AI_T_WATCH WHERE user_id = ? AND target_id = ? AND tenant_id = ?");
        verify(statement).setString(3, TENANT_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象のウォッチが無いときに削除成功と誤認しない。
       *
       * @given ユーザーIDが "U001"、対象IDが "J001"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(watch().deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全件を消すおそれがある。
       *
       * @given ユーザーIDが null、対象IDが "J001"
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_T_WATCH watch = watch();
        watch.setUserId(null);

        assertThat(watch.deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 対象IDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全件を消すおそれがある。
       *
       * @given ユーザーIDが "U001"、対象IDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 対象IDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_T_WATCH watch = watch();
        watch.setTargetId(null);

        assertThat(watch.deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }
}
