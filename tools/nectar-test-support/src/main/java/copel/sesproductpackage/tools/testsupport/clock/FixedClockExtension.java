package copel.sesproductpackage.tools.testsupport.clock;

import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.tools.testsupport.ExtensionScope;
import copel.sesproductpackage.tools.testsupport.MemoryModeGuard;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * {@link FixedClock} の注釈に従い、テストの前に {@link ApplicationClock} の時計を固定し、テストの後に既定へ戻す JUnit 拡張です。
 *
 * <p>時計の差し替えは Core の {@link ApplicationClock#setClock(Clock)} で行う。この操作は読み込み元が memory のときだけ許可されるため、
 * 先に {@link MemoryModeGuard} で判定し、設定漏れを分かる形で失敗させる。
 *
 * <p>時刻の既定のタイムゾーンは Core と同じ Asia/Tokyo にそろえる。固定した時刻を本番と同じタイムゾーンで読めるようにするためである。
 *
 * @author Copel Co., Ltd.
 */
public class FixedClockExtension implements BeforeEachCallback, AfterEachCallback {

  /** 固定した時計のタイムゾーン。Core の {@link ApplicationClock} の既定値と同じ Asia/Tokyo にする。 */
  private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

  /**
   * テストの前に、注釈が指定する時刻へ現在時刻の取得元を固定します。
   *
   * <p>注釈が無い場合は何もしない。注釈がある場合は、読み込み元が memory であることを確かめてから固定する。
   *
   * @param context テストの実行コンテキスト
   * @throws IllegalStateException 読み込み元が memory でない場合
   * @throws java.time.format.DateTimeParseException 注釈の時刻が ISO-8601 の形式でない場合
   */
  @Override
  public void beforeEach(final ExtensionContext context) {
    Optional<FixedClock> annotation = findAnnotation(context);
    if (annotation.isEmpty()) {
      return;
    }
    MemoryModeGuard.INSTANCE.require("@FixedClock");
    MemoryModeGuard.INSTANCE.requireNotParallel(context, "@FixedClock");
    ApplicationClock.INSTANCE.setClock(
        Clock.fixed(OffsetDateTime.parse(annotation.get().value()).toInstant(), ZONE));
  }

  /**
   * テストの後に、現在時刻の取得元を既定のシステム時計へ戻します。
   *
   * <p>注釈が無い場合は時計に触れない。注釈が付いたテストは、読み込み元の判定に失敗していても既定へ戻す。
   *
   * @param context テストの実行コンテキスト
   */
  @Override
  public void afterEach(final ExtensionContext context) {
    if (findAnnotation(context).isPresent()) {
      ApplicationClock.INSTANCE.resetToDefault();
    }
  }

  /**
   * テストの実行階層のうち、最も内側で {@link FixedClock} が付いた注釈を返します。
   *
   * <p>テストメソッド、{@code @Nested} クラス、外側のテストクラスの順に探し、最初に見つかったものを使う。メソッドの指定がクラスより優先されるのはこの順序による。
   *
   * @param context テストの実行コンテキスト
   * @return 見つかった注釈。付いていない場合は空
   */
  private static Optional<FixedClock> findAnnotation(final ExtensionContext context) {
    return ExtensionScope.INSTANCE.innermostFirst(context).stream()
        .map(element -> AnnotationSupport.findAnnotation(element, FixedClock.class))
        .flatMap(Optional::stream)
        .findFirst();
  }
}
