package copel.sesproductpackage.tools.testsupport.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_WEBAPP_M_USER;
import copel.sesproductpackage.core.unit.Plan;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** {@link RegisteredUsers} の単体テスト。 */
class RegisteredUsersTest {

  @Nested
  class create_String_String {

    @Nested
    class 正常系 {

      /**
       * 指定したユーザーIDをユーザーの項目に読み込むことを確かめる。 ユーザーIDを取り違えると、別の利用者の権限で処理してしまうので、取り違えを防ぐ。
       *
       * @given 引数のユーザーIDが "U001"、テナントIDが "T001"
       * @when create を "U001" と "T001" で実行する
       * @then ユーザーIDが "U001" のユーザーを返す
       */
      @Test
      void ユーザーIDがU001の場合_ユーザーIDがU001のユーザーを返す() {
        SES_AI_WEBAPP_M_USER user = RegisteredUsers.create("U001", "T001");

        assertThat(user.getUserId()).isEqualTo("U001");
      }

      /**
       * 指定したテナントIDをユーザーの項目に読み込むことを確かめる。 テナントIDを取り違えると、他顧客のデータを参照する恐れがあるので、取り違えを防ぐ。
       *
       * @given 引数のユーザーIDが "U001"、テナントIDが "T001"
       * @when create を "U001" と "T001" で実行する
       * @then テナントIDが "T001" のユーザーを返す
       */
      @Test
      void テナントIDがT001の場合_テナントIDがT001のユーザーを返す() {
        SES_AI_WEBAPP_M_USER user = RegisteredUsers.create("U001", "T001");

        assertThat(user.getTenantId()).isEqualTo("T001");
      }

      /**
       * 行が見つかったユーザーを登録済みとして返すことを確かめる。 登録済みの利用者を未登録と判定すると、認可の分岐を通せないテストになるので、判定が外れないようにする。
       *
       * @given 引数のユーザーIDが "U001"、テナントIDが "T001" で、接続が1行を返す
       * @when create を "U001" と "T001" で実行する
       * @then isRegistered が true を返す
       */
      @Test
      void 行が見つかる場合_isRegisteredがtrueを返す() {
        SES_AI_WEBAPP_M_USER user = RegisteredUsers.create("U001", "T001");

        assertThat(user.isRegistered()).isTrue();
      }

      /**
       * ヘルパーがロールを設定せず、ロールが null のユーザーを返すことを確かめる。 ロールを既定値で埋めると、権限の有無をテストごとに決める前提が崩れるので、未設定で返す。
       *
       * @given 引数のユーザーIDが "U001"、テナントIDが "T001" で、接続がロール列 null の1行を返す
       * @when create を "U001" と "T001" で実行する
       * @then ロールが null のユーザーを返す
       */
      @Test
      void 行のロールが未設定の場合_ロールがnullのユーザーを返す() {
        SES_AI_WEBAPP_M_USER user = RegisteredUsers.create("U001", "T001");

        assertThat(user.getRole()).isNull();
      }

      /**
       * プランが未設定の行を読み込むと、プランが LIGHT のユーザーを作ることを確かめる。 不明なプランを有料の権限で扱うと権限が過剰になるので、LIGHT にそろえる。
       *
       * @given 引数のユーザーIDが "U001"、テナントIDが "T001" で、接続がプラン列 null の1行を返す
       * @when create を "U001" と "T001" で実行する
       * @then プランが LIGHT のユーザーを返す
       */
      @Test
      void 行のプランが未設定の場合_プランがLIGHTのユーザーを返す() {
        SES_AI_WEBAPP_M_USER user = RegisteredUsers.create("U001", "T001");

        assertThat(user.getPlan()).isEqualTo(Plan.LIGHT);
      }
    }

    @Nested
    class 異常系 {

      /**
       * ユーザーIDが null の場合に IllegalArgumentException を投げることを確かめる。 ユーザーIDが null
       * のまま作ると、検索されず未登録のユーザーが返り、登録済みの前提のテストが静かに失敗するので、入口で止める。
       *
       * @given 引数のユーザーIDが null、テナントIDが "T001"
       * @when create を null と "T001" で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void ユーザーIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> RegisteredUsers.create(null, "T001"))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * ユーザーIDが空文字の場合に IllegalArgumentException を投げることを確かめる。
       * 空文字の検索条件でも未登録のユーザーが返り、原因が見えにくくなるので、入口で止める。
       *
       * @given 引数のユーザーIDが空文字、テナントIDが "T001"
       * @when create を "" と "T001" で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void ユーザーIDが空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> RegisteredUsers.create("", "T001"))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが null の場合に IllegalArgumentException を投げることを確かめる。 テナントIDが null
       * のユーザーを作ると、テナント隔離の前提が崩れるので、Core のエンティティの検査で止める。
       *
       * @given 引数のユーザーIDが "U001"、テナントIDが null
       * @when create を "U001" と null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> RegisteredUsers.create("U001", null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class create_String {

    @Nested
    class 正常系 {

      /**
       * テナントIDだけを指定しても、既定のユーザーIDで登録済みのユーザーを作ることを確かめる。 テストごとにユーザーIDを書き分けると書き間違いが起きやすいので、既定値を使う。
       *
       * @given 引数のテナントIDが "T001"
       * @when create を "T001" で実行する
       * @then ユーザーIDが "U001"、テナントIDが "T001"、isRegistered が true のユーザーを返す
       */
      @Test
      void テナントIDがT001の場合_ユーザーIDがU001で登録済みのユーザーを返す() {
        SES_AI_WEBAPP_M_USER user = RegisteredUsers.create("T001");

        assertThat(user.getUserId()).isEqualTo("U001");
        assertThat(user.getTenantId()).isEqualTo("T001");
        assertThat(user.isRegistered()).isTrue();
      }
    }
  }

  @Nested
  class createOn {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDで検索し、そのユーザーを読み込むことを確かめる。 ユーザーIDの条件が欠けると、別の利用者の行を読み込んでしまうので、条件の欠落を防ぐ。
       *
       * @given 準備文を作る接続のモック
       * @when createOn を接続、"U001"、"T001" で実行する
       * @then 検索の SQL に user_id の条件を含み、1番目のバインド値に "U001" を渡し、ユーザーIDが "U001" のユーザーを返す
       */
      @Test
      void ユーザーIDがU001の場合_U001で検索したユーザーを返す() throws SQLException {
        Connection connection = mock(Connection.class);

        SES_AI_WEBAPP_M_USER user = RegisteredUsers.createOn(connection, "U001", "T001");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("user_id = ?");
        PreparedStatement statement = connection.prepareStatement(sql.getValue());
        verify(statement).setString(1, "U001");
        assertThat(user.getUserId()).isEqualTo("U001");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 準備文の作成が失敗した場合に、IllegalStateException で呼び出し元へ伝えることを確かめる。 検査例外のままだと、テストの各メソッドに throws
       * の宣言が広がるので、非検査例外に変換する。
       *
       * @given 接続の prepareStatement が SQLException "接続失敗" を投げる
       * @when createOn を接続、"U001"、"T001" で実行する
       * @then IllegalStateException を投げ、原因が SQLException である
       */
      @Test
      void 準備文の作成が失敗する場合_IllegalStateExceptionを投げる() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("接続失敗"));

        assertThatThrownBy(() -> RegisteredUsers.createOn(connection, "U001", "T001"))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(SQLException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * テナントIDで絞り込んで検索し、そのテナントのユーザーを読み込むことを確かめる。 テナントIDの条件が欠けると、他テナントの利用者を読み込んでしまうので、条件の欠落を防ぐ。
       *
       * @given 準備文を作る接続のモック
       * @when createOn を接続、"U001"、"T001" で実行する
       * @then 検索の SQL に tenant_id の条件を含み、2番目のバインド値に "T001" を渡し、テナントIDが "T001" のユーザーを返す
       */
      @Test
      void テナントIDがT001の場合_T001で絞り込んだユーザーを返す() throws SQLException {
        Connection connection = mock(Connection.class);

        SES_AI_WEBAPP_M_USER user = RegisteredUsers.createOn(connection, "U001", "T001");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("tenant_id = ?");
        PreparedStatement statement = connection.prepareStatement(sql.getValue());
        verify(statement).setString(2, "T001");
        assertThat(user.getTenantId()).isEqualTo("T001");
      }
    }
  }
}
