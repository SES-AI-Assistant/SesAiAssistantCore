package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_M_INGEST_ROUTE.ChannelType;
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

/** {@link SES_AI_M_INGEST_ROUTE} の単体テスト。 */
class SES_AI_M_INGEST_ROUTETest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** ルートキーの値。 */
  private static final String ROUTE_KEY = "U-LINE-001";

  /** 登録日時の文字列。 */
  private static final String REGISTER_DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "channel_type", "route_key", "register_date", "register_user"
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
   * 項目が揃ったテナント取込ルーティングを生成する。チャネル種別は LINE とする。
   *
   * @return 生成したルーティング
   */
  private static SES_AI_M_INGEST_ROUTE route() {
    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE(TENANT_ID);
    route.setChannelType(ChannelType.LINE);
    route.setRouteKey(ROUTE_KEY);
    return route;
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無いルーティングは他テナントの取込先を指す。
       *
       * @given テナントIDが null
       * @when SES_AI_M_INGEST_ROUTE を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_M_INGEST_ROUTE(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * チャネル種別とルートキーを登録するSQLに、テナントIDを加えて実行することを確かめる。 テナントの取込先を他テナントと区別する。
       *
       * @given チャネル種別が LINE、ルートキーが "U-LINE-001"、登録日時が "2026-01-15 10:00:00"、登録ユーザーが "U001"、更新件数が 1
       *     件
       * @when insert を接続で実行する
       * @then tenant_id を含むINSERT文を準備し、1件を返す
       */
      @MockDbConnection
      @Test
      void 項目が揃っている場合_1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_INGEST_ROUTE route = route();
        route.setRegisterDate(new OriginalDateTime(REGISTER_DATE_TEXT));
        route.setRegisterUser("U001");

        assertThat(route.insert(connection)).isEqualTo(1);

        verify(connection)
            .prepareStatement(
                "INSERT INTO SES_AI_M_INGEST_ROUTE (channel_type, route_key, register_date,"
                    + " register_user, tenant_id) VALUES (?, ?, ?, ?, ?)");
        verify(statement).setString(1, "LINE");
        verify(statement).setString(2, ROUTE_KEY);
        verify(statement).setTimestamp(3, Timestamp.valueOf(REGISTER_DATE_TEXT));
        verify(statement).setString(4, "U001");
        verify(statement).setString(5, TENANT_ID);
      }
    }

    @Nested
    class 境界値 {

      /**
       * チャネル種別が null の場合に、チャネル種別の位置に null を設定することを確かめる。 チャネル種別が未設定の登録も失敗させずに処理する。
       *
       * @given チャネル種別が null、ルートキーが "U-LINE-001"、更新件数が 1 件
       * @when insert を接続で実行する
       * @then チャネル種別の位置に null を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void チャネル種別がnullの場合_チャネル種別にnullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE(TENANT_ID);
        route.setRouteKey(ROUTE_KEY);

        assertThat(route.insert(connection)).isEqualTo(1);

        verify(statement).setString(1, null);
      }

      /**
       * 登録日時が未設定の場合に、日時の位置に null を設定することを確かめる。 登録日時は必須の列ではない。
       *
       * @given 登録日時が未設定、チャネル種別が LINE、ルートキーが "U-LINE-001"、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 登録日時の位置に null を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void 登録日時が未設定の場合_登録日時にnullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(route().insert(connection)).isEqualTo(1);

        verify(statement).setTimestamp(3, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * チャネル種別とルートキーが一致する行の項目を読み込むことを確かめる。 保存済みのルーティングを編集画面に表示する。
       *
       * @given チャネル種別が LINE、ルートキーが "U-LINE-001"、SELECT の結果が 1 行チャネル種別 "LINE"、登録ユーザー "U001"
       * @when selectByPk を接続で実行する
       * @then チャネル種別 LINE と登録ユーザー "U001" を読み込んだ項目に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_項目を更新する(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder()
                    .columns(COLUMNS)
                    .row("LINE", ROUTE_KEY, REGISTER_DATE_TEXT, "U001")
                    .build());
        SES_AI_M_INGEST_ROUTE route = route();

        route.selectByPk(connection);

        assertThat(route.getChannelType()).isEqualTo(ChannelType.LINE);
        assertThat(route.getRegisterUser()).isEqualTo("U001");
        verify(statement).setString(3, TENANT_ID);
      }

      /**
       * 該当行が無い場合に項目を変えないことを確かめる。 存在しないルーティングを読み込んでも、入力中の値を壊さない。
       *
       * @given チャネル種別が LINE、ルートキーが "U-LINE-001"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then ルートキーを変えずに何もしない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_何もしない(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_M_INGEST_ROUTE route = route();

        route.selectByPk(connection);

        assertThat(route.getRouteKey()).isEqualTo(ROUTE_KEY);
        assertThat(route.getRegisterUser()).isNull();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込んだチャネル種別が対応しない値の場合に例外を投げることを確かめる。 対応しない取込先を黙って扱うと、誤ったチャネルに取り込む。
       *
       * @given SELECT の結果の チャネル種別が "SMS" の 1 行
       * @when selectByPk を接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void 不正なチャネル種別の場合_IllegalArgumentExceptionを投げる(final Connection connection)
          throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row("SMS", ROUTE_KEY, REGISTER_DATE_TEXT, "U001")
                .build());

        assertThatThrownBy(() -> route().selectByPk(connection))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * チャネル種別が null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返す恐れがある。
       *
       * @given チャネル種別が null、ルートキーが "U-LINE-001"
       * @when selectByPk を接続で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void チャネル種別がnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        SES_AI_M_INGEST_ROUTE route = route();
        route.setChannelType(null);

        route.selectByPk(connection);

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * ルートキーが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返す恐れがある。
       *
       * @given チャネル種別が LINE、ルートキーが null
       * @when selectByPk を接続で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ルートキーがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        SES_AI_M_INGEST_ROUTE route = route();
        route.setRouteKey(null);

        route.selectByPk(connection);

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * チャネル種別とルートキーとテナントIDが一致する行の登録ユーザーを更新することを確かめる。 ルーティングの担当者を変える。
       *
       * @given チャネル種別が LINE、ルートキーが "U-LINE-001"、登録ユーザーが "U002"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then tenant_id を条件に含めて更新し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_M_INGEST_ROUTE route = route();
        route.setRegisterUser("U002");

        assertThat(route.updateByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_M_INGEST_ROUTE SET register_user = ? WHERE channel_type = ?"
                    + " AND route_key = ? AND tenant_id = ?");
        verify(statement).setString(1, "U002");
        verify(statement).setString(2, "LINE");
        verify(statement).setString(3, ROUTE_KEY);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象の行が無いときに更新成功と誤認しない。
       *
       * @given チャネル種別が LINE、ルートキーが "U-LINE-001"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(route().updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * チャネル種別が null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全行を書き換える恐れがある。
       *
       * @given チャネル種別が null、ルートキーが "U-LINE-001"
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void チャネル種別がnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_INGEST_ROUTE route = route();
        route.setChannelType(null);

        assertThat(route.updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * ルートキーが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全行を書き換える恐れがある。
       *
       * @given チャネル種別が LINE、ルートキーが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ルートキーがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_INGEST_ROUTE route = route();
        route.setRouteKey(null);

        assertThat(route.updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 更新の条件にテナントIDを含め、他テナントの行を更新しないことを確かめる。 他テナントのルーティングを書き換えると取込先が入れ替わる。
       *
       * @given テナントIDが "T001"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 条件の4番目に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(route().updateByPk(connection)).isTrue();

        verify(statement).setString(4, TENANT_ID);
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * チャネル種別とルートキーとテナントIDが一致する行を削除することを確かめる。 不要になった取込先を残さない。
       *
       * @given チャネル種別が LINE、ルートキーが "U-LINE-001"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then tenant_id を条件に含めて削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(route().deleteByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "DELETE FROM SES_AI_M_INGEST_ROUTE WHERE channel_type = ? AND route_key = ?"
                    + " AND tenant_id = ?");
        verify(statement).setString(1, "LINE");
        verify(statement).setString(2, ROUTE_KEY);
        verify(statement).setString(3, TENANT_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象の行が無いときに削除成功と誤認しない。
       *
       * @given チャネル種別が LINE、ルートキーが "U-LINE-001"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(route().deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * チャネル種別が null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全行を消す恐れがある。
       *
       * @given チャネル種別が null、ルートキーが "U-LINE-001"
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void チャネル種別がnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_INGEST_ROUTE route = route();
        route.setChannelType(null);

        assertThat(route.deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * ルートキーが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全行を消す恐れがある。
       *
       * @given チャネル種別が LINE、ルートキーが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ルートキーがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        SES_AI_M_INGEST_ROUTE route = route();
        route.setRouteKey(null);

        assertThat(route.deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }
}
