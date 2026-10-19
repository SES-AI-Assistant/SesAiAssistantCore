package copel.sesproductpackage.tools.testsupport.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mockito.Mockito;

/**
 * モックの接続、準備文、結果セットを組み合わせるための補助を行うクラスです。
 *
 * <p>Core の Entity や EntityLot は「接続、準備文の作成、問い合わせの実行、結果の読み取り」の順で JDBC を使う。その流れを、テストで1行の設定にまとめる。 接続は
 * {@link Connection} のモックを受け取り、結果は {@link ResultSet} のモックを受け取る。
 *
 * <p>インスタンスと状態を持たず、JDBC のモックを組み立てる手順だけを担う。
 *
 * @author Copel Co., Ltd.
 */
public final class JdbcMocks {

  /** インスタンス化を禁止する。すべての操作は静的メソッドで提供する。 */
  private JdbcMocks() {}

  /**
   * 接続のモックに、問い合わせの結果セットを返す準備文を設定します。
   *
   * <p>接続の {@code prepareStatement(String)} が準備文のモックを返し、その準備文の {@code executeQuery()}
   * が指定した結果セットを返すように設定する。 準備文のモックを返すため、利用側は {@code verify} で呼び出しを確かめられる。
   *
   * @param connection 接続のモック
   * @param resultSet 問い合わせの結果として返す結果セット
   * @return 設定した準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  public static PreparedStatement stubQuery(final Connection connection, final ResultSet resultSet)
      throws SQLException {
    PreparedStatement statement = Mockito.mock(PreparedStatement.class);
    Mockito.when(statement.executeQuery()).thenReturn(resultSet);
    Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);
    return statement;
  }

  /**
   * 接続のモックに、問い合わせの結果セットを返す文を設定します。
   *
   * <p>接続の {@code createStatement()} が文のモックを返し、その文の {@code executeQuery(String)}
   * が指定した結果セットを返すように設定する。 準備文を使わない実装（{@code createStatement} を使う処理）を、テストで読めるようにするための補助である。
   *
   * @param connection 接続のモック
   * @param resultSet 問い合わせの結果として返す結果セット
   * @return 設定した文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  public static Statement stubStatement(final Connection connection, final ResultSet resultSet)
      throws SQLException {
    Statement statement = Mockito.mock(Statement.class);
    Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
    Mockito.when(connection.createStatement()).thenReturn(statement);
    return statement;
  }
}
