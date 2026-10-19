package copel.sesproductpackage.core.util;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * アプリケーション全体で使う現在時刻の取得元を保持する、単一インスタンスの列挙型。
 *
 * <p>既定は Asia/Tokyo のシステム時計であり、本番では現在時刻の取得結果を変えない。現在時刻は {@code Instant.now()} や {@link
 * System#currentTimeMillis()} を直接呼ばず、必ず {@link #clock()} が返す時計を経由して取得する。これにより単体テストで時刻を固定できる。
 *
 * <p>{@link #setClock(Clock)} はテスト実行時（システムプロパティ {@link #CONFIG_SOURCE_PROPERTY} が {@link
 * #CONFIG_SOURCE_MEMORY} のとき）だけ許可し、それ以外では {@link IllegalStateException}
 * を投げる。本番コードから時計を差し替えて現在時刻の挙動を変えることを防ぐ仕様である。 {@link #resetToDefault()}
 * は後始末に使うだけで現在時刻を固定しないので、制限を設けない。
 *
 * <p>時計は {@link AtomicReference} で保持するため、複数スレッドから同時に読み書きしても値が壊れない。
 *
 * <p>Properties と SecretManager の TTL 判定の {@code System.currentTimeMillis()} は、この時計を経由していない。
 * これらは段階0-2 の別 PR で対応する。
 *
 * <p>単一インスタンスは列挙型で表現する。列挙型はシリアライズや反射に対しても単一インスタンスを保証する。
 *
 * @author Copel Co., Ltd.
 */
public enum ApplicationClock {
  /** アプリケーション全体で共有する唯一のインスタンスです。利用は {@code ApplicationClock.INSTANCE} で行います。 */
  INSTANCE;

  // ================================
  // 定数定義
  // ================================
  /**
   * 設定読み込み元を表すシステムプロパティのキーです。
   *
   * <p>テスト実行時の値を {@link #CONFIG_SOURCE_MEMORY} に固定し、時計の差し替えを許可する判定に使う。 段階0-2
   * で設定の読み込み元の切り替えにも同じキーを使うため、定数にして一箇所で管理する。
   */
  public static final String CONFIG_SOURCE_PROPERTY = "nectar.config.source";

  /** 設定読み込み元がメモリであることを表す値です。テスト実行時だけこの値を設定する。 */
  public static final String CONFIG_SOURCE_MEMORY = "memory";

  /** 現在時刻の既定のタイムゾーン ID です。本番の挙動を変えないため Asia/Tokyo に固定する。 */
  private static final String DEFAULT_ZONE_ID = "Asia/Tokyo";

  // ================================
  // フィールド定義
  // ================================
  /**
   * 現在の時計を保持します。
   *
   * <p>読み書きをスレッド安全にするため {@link AtomicReference} で持つ。既定値は Asia/Tokyo のシステム時計である。
   */
  private final AtomicReference<Clock> clock = new AtomicReference<>(systemClock());

  // ================================
  // 公開メソッド
  // ================================
  /**
   * 現在時刻の取得に使う時計を返します。
   *
   * <p>本番では Asia/Tokyo のシステム時計を返す。テストでは {@link #setClock(Clock)} で設定した時計を返す。戻り値の {@link Clock}
   * は不変であるため、呼び出し側で値を変更されることはない。
   *
   * @return 現在時刻の取得に使う時計
   */
  public Clock clock() {
    return clock.get();
  }

  /**
   * 現在時刻の取得に使う時計を差し替えます。テスト専用です。
   *
   * <p>テストで時刻を固定するために使う。システムプロパティ {@link #CONFIG_SOURCE_PROPERTY} が {@link #CONFIG_SOURCE_MEMORY}
   * でないときは差し替えを拒否する。本番で時計が差し替わると現在時刻の挙動が変わるためである。テスト終了時は {@link #resetToDefault()} で既定に戻すこと。
   *
   * @param fixedClock 差し替える時計。null は指定できない
   * @throws IllegalStateException システムプロパティが {@link #CONFIG_SOURCE_MEMORY} 以外の場合
   * @throws NullPointerException fixedClock が null の場合
   */
  public void setClock(final Clock fixedClock) {
    if (!CONFIG_SOURCE_MEMORY.equals(System.getProperty(CONFIG_SOURCE_PROPERTY))) {
      throw new IllegalStateException(
          "時計の差し替えはシステムプロパティ "
              + CONFIG_SOURCE_PROPERTY
              + " が "
              + CONFIG_SOURCE_MEMORY
              + " のときだけ許可されています。");
    }
    clock.set(Objects.requireNonNull(fixedClock, "時計にはnullを指定できません。"));
  }

  /**
   * 現在時刻の取得に使う時計を既定のシステム時計に戻します。テスト専用です。
   *
   * <p>テストごとに時計が残らないようにするため、テストの後始末で呼び出す。既定は Asia/Tokyo のシステム時計である。
   * 現在時刻を固定しない方向の操作であるため、システムプロパティによる制限は設けない。
   */
  public void resetToDefault() {
    clock.set(systemClock());
  }

  // ================================
  // 内部処理
  // ================================
  /**
   * Asia/Tokyo のシステム時計を生成します。
   *
   * <p>初期化時と {@link #resetToDefault()} の両方で同じ既定値を使うため、生成処理を一箇所にまとめる。
   *
   * @return Asia/Tokyo のシステム時計
   */
  private static Clock systemClock() {
    return Clock.system(ZoneId.of(DEFAULT_ZONE_ID));
  }
}
