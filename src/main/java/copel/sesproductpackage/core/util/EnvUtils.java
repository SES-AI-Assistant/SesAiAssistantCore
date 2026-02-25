package copel.sesproductpackage.core.util;

/**
 * 環境変数を取得するためのユーティリティクラス. テスト時に環境変数をモック化しやすくするために作成されました。
 *
 * @author 鈴木一矢
 */
public class EnvUtils {
  /**
   * 指定された環境変数の値を取得します.
   *
   * @param name 環境変数名
   * @return 環境変数の値
   */
  public static String get(final String name) {
    return System.getenv(name);
  }
}
