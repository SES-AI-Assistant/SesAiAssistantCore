package copel.sesproductpackage.tools.testsupport.db;

import copel.sesproductpackage.core.database.SES_AI_WEBAPP_M_USER;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.SQLException;
import org.mockito.Mockito;

/**
 * 登録済みのシステムユーザーを、DB の行を返すモック接続から読み込んだ状態で作るクラスです。
 *
 * <p>Core の {@link SES_AI_WEBAPP_M_USER#selectByPk(Connection)} を本物のまま通すため、登録済みの状態を テストの中で {@code
 * isRegistered} を差し替えて作らない。ユーザーIDとテナントIDは、SELECT の行の値として戻り、エンティティの項目に読み込まれる。
 *
 * <p>ロールは行の値を null にするため未設定になる。プランは行の値を null にするため、Core の変換で LIGHT になる。
 *
 * <p>インスタンスと状態を持たず、ユーザーを作る手順だけを担う。
 *
 * @author Copel Co., Ltd.
 */
public final class RegisteredUsers {

  /** ユーザーIDを省略して作る場合の既定値。テストごとに同じ値を書かなくて済むようにする。 */
  private static final String DEFAULT_USER_ID = "U001";

  /** インスタンス化を禁止する。すべての操作は静的メソッドで提供する。 */
  private RegisteredUsers() {}

  /**
   * 指定したユーザーIDとテナントIDで、登録済みのシステムユーザーを作ります。
   *
   * <p>接続のモックに、ユーザーIDとテナントIDを持つ1行を返す準備文を設定し、その接続で本物の selectByPk を実行する。 行が見つかるため、戻り値は {@code
   * isRegistered()} が true のユーザーになる。
   *
   * <p>ユーザーIDが null または空文字の場合は、selectByPk が検索せず未登録のユーザーを返すため、入口で例外を投げる。 テナントIDが null
   * または空文字の場合は、Core のエンティティの検査で例外を投げる。
   *
   * @param userId ユーザーID。行の user_id と検索条件に使う。null と空文字は不可
   * @param tenantId テナントID。行の tenant_id に使う。null と空文字は不可
   * @return 行を読み込んだ登録済みのシステムユーザー
   * @throws IllegalArgumentException ユーザーIDまたはテナントIDが null または空文字の場合
   * @throws IllegalStateException 読み込みの準備または実行で SQLException が起きた場合
   */
  public static SES_AI_WEBAPP_M_USER create(final String userId, final String tenantId) {
    if (userId == null || userId.trim().isEmpty()) {
      throw new IllegalArgumentException("userId must not be null or empty");
    }
    return createOn(Mockito.mock(Connection.class), userId, tenantId);
  }

  /**
   * テナントIDだけを指定し、既定のユーザーIDで登録済みのシステムユーザーを作ります。
   *
   * <p>戻り値と例外の仕様は {@link #create(String, String)} と同じである。
   *
   * @param tenantId テナントID。行の tenant_id に使う。null と空文字は不可
   * @return 行を読み込んだ登録済みのシステムユーザー
   * @throws IllegalArgumentException テナントIDが null または空文字の場合
   * @throws IllegalStateException 読み込みの準備または実行で SQLException が起きた場合
   */
  public static SES_AI_WEBAPP_M_USER create(final String tenantId) {
    return create(DEFAULT_USER_ID, tenantId);
  }

  /**
   * 指定した接続に1行を返す準備文を設定し、その接続で登録済みのシステムユーザーを読み込みます。
   *
   * <p>この口は、接続を差し替えるためのテスト用の口である。接続の準備文の作成や実行で SQLException が起きる経路を、テストから確かめられるように分けている。 呼び出し側は
   * SQLException を IllegalStateException として扱えばよく、ラップの仕様を1か所に集める。
   *
   * @param connection 行を返すように設定する接続のモック
   * @param userId ユーザーID。行の user_id と検索条件に使う
   * @param tenantId テナントID。行の tenant_id に使う
   * @return 行を読み込んだ登録済みのシステムユーザー
   * @throws IllegalStateException 読み込みの準備または実行で SQLException が起きた場合
   */
  static SES_AI_WEBAPP_M_USER createOn(
      final Connection connection, final String userId, final String tenantId) {
    try {
      JdbcMocks.stubQuery(
          connection,
          MockResultSetBuilder.builder()
              .columns(
                  "user_id",
                  "user_name",
                  "role_cd",
                  "plan_cd",
                  "register_date",
                  "register_user",
                  "tenant_id")
              .row(userId, null, null, null, null, null, tenantId)
              .build());
      SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER(tenantId);
      user.setUserId(userId);
      user.selectByPk(connection);
      return user;
    } catch (SQLException e) {
      throw new IllegalStateException("登録済みのシステムユーザーを読み込めなかった。", e);
    }
  }
}
