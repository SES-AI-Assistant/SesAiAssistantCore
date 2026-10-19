package copel.sesproductpackage.core.util;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * 設定の読み込み元が memory、つまりテスト実行時かどうかを判定する、単一インスタンスの列挙型。
 *
 * <p>判定に使うのはシステムプロパティ {@link #CONFIG_SOURCE_PROPERTY} である。本番ではこのプロパティを設定しないため、{@link #isMemory()} は
 * false を返し、Properties と DBConnection は AWS から設定を読む従来の経路を通る。
 *
 * <p>Lambda 上、つまり環境変数 {@link #LAMBDA_FUNCTION_NAME_ENV} が設定されている場合は、プロパティの値に関係なく memory
 * を無効にする。Lambda で誤って memory を設定しても、設定値が空のまま起動することを防ぐためである。
 *
 * <p>memory 判定を 1 か所にまとめるのは、Properties、DBConnection、ApplicationClock の判定を同じ基準にそろえるためである。
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public enum ConfigSource {
  /** アプリケーション全体で共有する唯一のインスタンスです。利用は {@code ConfigSource.INSTANCE} で行います。 */
  INSTANCE;

  // ================================
  // 定数定義
  // ================================
  /**
   * 設定読み込み元を表すシステムプロパティのキーです。
   *
   * <p>テスト実行時の値を {@link #CONFIG_SOURCE_MEMORY} に固定する。時計と接続の差し替えは、この値のときだけ許可される。
   */
  public static final String CONFIG_SOURCE_PROPERTY = "nectar.config.source";

  /** 設定読み込み元がメモリであることを表す値です。テスト実行時だけこの値を設定する。 */
  public static final String CONFIG_SOURCE_MEMORY = "memory";

  /** Lambda 上で設定される環境変数の名前です。この環境変数が空でなければ Lambda 上とみなす。 */
  public static final String LAMBDA_FUNCTION_NAME_ENV = "AWS_LAMBDA_FUNCTION_NAME";

  // ================================
  // フィールド定義
  // ================================
  /**
   * memory が有効になったことを警告ログに出したかどうかを保持します。
   *
   * <p>判定は呼び出しごとに行われるため、警告は JVM 内で 1 回だけ出す。
   */
  private final AtomicBoolean memoryWarned = new AtomicBoolean(false);

  // ================================
  // 公開メソッド
  // ================================
  /**
   * 設定の読み込み元が memory かどうかを返します。
   *
   * <p>環境変数 {@link #LAMBDA_FUNCTION_NAME_ENV} の値を読み、{@link #isMemory(String)} に渡す。
   *
   * @return memory の場合 true、それ以外は false
   */
  public boolean isMemory() {
    return isMemory(System.getenv(LAMBDA_FUNCTION_NAME_ENV));
  }

  /**
   * Lambda 上の関数名を受け取り、設定の読み込み元が memory かどうかを返します。
   *
   * <p>システムプロパティ {@link #CONFIG_SOURCE_PROPERTY} の値が {@link #CONFIG_SOURCE_MEMORY}
   * と完全一致し、かつ関数名が空でない場合、つまり Lambda 上でない場合だけ true を返す。memory が有効になった場合は、初回に WARN
   * ログを出す。関数名は環境変数の値をそのまま受け取るため、テストでは任意の値を渡して両方の経路を確かめられる。
   *
   * @param lambdaFunctionName 環境変数 {@link #LAMBDA_FUNCTION_NAME_ENV} の値。未設定の場合は null
   * @return memory として扱う場合 true、それ以外は false
   */
  boolean isMemory(final String lambdaFunctionName) {
    if (!CONFIG_SOURCE_MEMORY.equals(System.getProperty(CONFIG_SOURCE_PROPERTY))) {
      return false;
    }
    if (lambdaFunctionName != null && !lambdaFunctionName.isEmpty()) {
      return false;
    }
    if (!memoryWarned.getAndSet(true)) {
      log.warn("設定の読み込み元が memory に設定されています。AWS から設定を読み込みません。テスト以外で設定されていないか確認してください。");
    }
    return true;
  }

  /**
   * 指定した操作が memory のときだけ許可されていることを確かめます。
   *
   * <p>テスト専用の操作を本番で実行すると、設定値や接続先が差し替わり本番の挙動が変わる。そのため memory 以外では実行を拒否する。
   *
   * @param operation 拒否時のメッセージに載せる操作名。例: "設定値の登録"
   * @throws IllegalStateException 読み込み元が memory 以外の場合
   */
  public void requireMemory(final String operation) {
    if (!isMemory()) {
      throw new IllegalStateException(
          operation
              + "はシステムプロパティ "
              + CONFIG_SOURCE_PROPERTY
              + " が "
              + CONFIG_SOURCE_MEMORY
              + " のときだけ許可されています。");
    }
  }
}
