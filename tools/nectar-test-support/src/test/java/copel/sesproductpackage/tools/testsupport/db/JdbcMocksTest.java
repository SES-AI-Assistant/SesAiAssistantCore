package copel.sesproductpackage.tools.testsupport.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link JdbcMocks} の単体テスト。 */
class JdbcMocksTest {

  @Nested
  class stubQuery {

    @Nested
    class 正常系 {

      /**
       * 接続の準備文が結果セットを返すように設定されることを確かめる。 問い合わせの結果がテストの中で読めず、結果の受け渡しが途切れる不具合を防ぐ。
       *
       * @given 接続のモック、結果セットのモック
       * @when stubQuery で設定し、接続の prepareStatement を "SELECT 1" で呼び出して executeQuery を呼び出す
       * @then 結果セットのモックを返す
       */
      @Test
      void 接続と結果セットを指定した場合_問い合わせが結果セットを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        ResultSet resultSet = mock(ResultSet.class);

        JdbcMocks.stubQuery(connection, resultSet);

        assertThat(connection.prepareStatement("SELECT 1").executeQuery()).isSameAs(resultSet);
      }

      /**
       * 設定した準備文のモックを返すことを確かめる。 利用側が準備文の呼び出しを検証できるようにし、設定した準備文と別の準備文が使われる不具合を防ぐ。
       *
       * @given 接続のモック、結果セットのモック
       * @when stubQuery の戻り値と、接続の prepareStatement を "SELECT 1" で呼び出した結果を比べる
       * @then 同じ準備文のモックを返す
       */
      @Test
      void 接続と結果セットを指定した場合_準備文のモックを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        ResultSet resultSet = mock(ResultSet.class);

        PreparedStatement statement = JdbcMocks.stubQuery(connection, resultSet);

        assertThat(connection.prepareStatement("SELECT 1")).isSameAs(statement);
      }
    }
  }

  @Nested
  class stubStatement {

    @Nested
    class 正常系 {

      /**
       * 接続の文が結果セットを返すように設定されることを確かめる。 createStatement を使う処理の問い合わせ結果を、テストの中で読めるようにする。
       *
       * @given 接続のモック、結果セットのモック
       * @when stubStatement で設定し、接続の createStatement を呼び出して executeQuery を "SELECT 1" で呼び出す
       * @then 結果セットのモックを返す
       */
      @Test
      void 接続と結果セットを指定した場合_問い合わせが結果セットを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        ResultSet resultSet = mock(ResultSet.class);

        JdbcMocks.stubStatement(connection, resultSet);

        assertThat(connection.createStatement().executeQuery("SELECT 1")).isSameAs(resultSet);
      }

      /**
       * 設定した文のモックを返すことを確かめる。 利用側が文の呼び出しを検証できるようにし、設定した文と別の文が使われる不具合を防ぐ。
       *
       * @given 接続のモック、結果セットのモック
       * @when stubStatement の戻り値と、接続の createStatement の結果を比べる
       * @then 同じ文のモックを返す
       */
      @Test
      void 接続と結果セットを指定した場合_文のモックを返す() throws SQLException {
        Connection connection = mock(Connection.class);
        ResultSet resultSet = mock(ResultSet.class);

        Statement statement = JdbcMocks.stubStatement(connection, resultSet);

        assertThat(connection.createStatement()).isSameAs(statement);
      }
    }
  }
}
