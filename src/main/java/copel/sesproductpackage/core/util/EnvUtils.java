package copel.sesproductpackage.core.util;

/**
 * 環境変数を取得するためのユーティリティクラス. テスト時に環境変数をモック化しやすくするために作成されました。
 *
 * @author Copel Co., Ltd.
 */
public class EnvUtils {
  /** インスタンス化を禁止する。すべての操作は静的メンバーで提供する。 */
  private EnvUtils() {}

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
