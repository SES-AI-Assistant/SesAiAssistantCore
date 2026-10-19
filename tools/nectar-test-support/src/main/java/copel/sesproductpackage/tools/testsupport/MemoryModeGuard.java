package copel.sesproductpackage.tools.testsupport;

import copel.sesproductpackage.core.util.ConfigSource;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * テスト用の差し替えの前提、つまり読み込み元が memory であることと直列実行であることを確かめる、単一インスタンスの列挙型。
 *
 * <p>Core の時計、設定値、DB 接続の差し替えは、読み込み元が memory のときだけ動く。本クラスは、その条件を満たさない場合に 「利用側の surefire 設定で memory
 * を指定する」という対処方法を含めた例外を投げる。Core 側の例外メッセージだけでは、利用側が原因にたどり着きにくいためである。
 *
 * <p>差し替えは静的な状態を書き換えるため、JUnit の並列実行では他のテストと状態を共有して壊れる。そのため並列実行が有効な場合も例外を投げる。
 *
 * <p>システムプロパティは拡張側で自動設定しない。memory への切り替えは、利用側の pom の surefire 設定で明示する。
 *
 * @author Copel Co., Ltd.
 */
public enum MemoryModeGuard {
  /** アプリケーション全体で共有する唯一のインスタンスです。利用は {@code MemoryModeGuard.INSTANCE} で行います。 */
  INSTANCE;

  /** JUnit の並列実行の有効化を表す設定のキーです。 */
  private static final String PARALLEL_PROPERTY = "junit.jupiter.execution.parallel.enabled";

  /**
   * 指定した機能が memory のときだけ使えることを確かめます。
   *
   * <p>読み込み元が memory でない場合は、利用側の surefire 設定を直す必要があることを例外メッセージに含める。
   *
   * @param feature 拒否時のメッセージに載せる機能名。例: "@FixedClock"
   * @throws IllegalStateException 読み込み元が memory でない場合
   */
  public void require(final String feature) {
    if (!ConfigSource.INSTANCE.isMemory()) {
      throw new IllegalStateException(
          feature
              + " は読み込み元が memory のときだけ使えます。"
              + "surefire の systemPropertyVariables に "
              + ConfigSource.CONFIG_SOURCE_PROPERTY
              + "="
              + ConfigSource.CONFIG_SOURCE_MEMORY
              + " を設定してください。");
    }
  }

  /**
   * 指定した機能が直列実行のときだけ使えることを確かめます。
   *
   * <p>JUnit の設定 junit.jupiter.execution.parallel.enabled が true の場合は、差し替えが他のテストと競合するため拒否する。
   * 設定が無い、または false の場合は許可する。
   *
   * @param context テストの実行コンテキスト。設定値の読み取りに使う
   * @param feature 拒否時のメッセージに載せる機能名。例: "@TestProperty"
   * @throws IllegalStateException 並列実行が有効な場合
   */
  public void requireNotParallel(final ExtensionContext context, final String feature) {
    if (context
        .getConfigurationParameter(PARALLEL_PROPERTY)
        .filter(Boolean::parseBoolean)
        .isPresent()) {
      throw new IllegalStateException(
          feature
              + " は JUnit の並列実行（"
              + PARALLEL_PROPERTY
              + "=true）では使えません。"
              + "差し替えは静的な状態を書き換えるため、並列実行を無効にしてください。");
    }
  }
}
