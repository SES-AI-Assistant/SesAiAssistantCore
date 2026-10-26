package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** {@link WatchPeriod} の単体テスト。 */
class WatchPeriodTest {

  /** 既定のウォッチ期間の日数。案件・要員の TTL の既定値を想定した値である。 */
  private static final int DEFAULT_DAYS = 30;

  @Nested
  class isValid {

    @Nested
    class 正常系 {

      /**
       * 1以上14以下のウォッチ期間を有効と判定することを確かめる。 画面で指定できる範囲の値を受け入れるからである。
       *
       * @given ウォッチ期間が7日
       * @when isValid を実行する
       * @then true を返す
       */
      @Test
      void ウォッチ期間が7日の場合_trueを返す() {
        assertThat(WatchPeriod.of(7).isValid()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 下限の1日を有効と判定することを確かめる。 画面で指定できる最短の期間を受け入れるからである。
       *
       * @given ウォッチ期間が1日
       * @when isValid を実行する
       * @then true を返す
       */
      @Test
      void ウォッチ期間が1日の場合_trueを返す() {
        assertThat(WatchPeriod.of(1).isValid()).isTrue();
      }

      /**
       * 上限の14日を有効と判定することを確かめる。 画面で指定できる最長の期間を受け入れるからである。
       *
       * @given ウォッチ期間が14日
       * @when isValid を実行する
       * @then true を返す
       */
      @Test
      void ウォッチ期間が14日の場合_trueを返す() {
        assertThat(WatchPeriod.of(14).isValid()).isTrue();
      }

      /**
       * 下限を下回る0日を無効と判定することを確かめる。 0日のウォッチは期限切れの行を作るだけで意味が無いからである。
       *
       * @given ウォッチ期間が0日
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void ウォッチ期間が0日の場合_falseを返す() {
        assertThat(WatchPeriod.of(0).isValid()).isFalse();
      }

      /**
       * 上限を超える15日を無効と判定することを確かめる。 画面の検証を通らない値は既定値に置き換えるからである。
       *
       * @given ウォッチ期間が15日
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void ウォッチ期間が15日の場合_falseを返す() {
        assertThat(WatchPeriod.of(15).isValid()).isFalse();
      }

      /**
       * 負の値を無効と判定することを確かめる。 負の日数ではウォッチの期限が過去になるからである。
       *
       * @given ウォッチ期間が負の1日
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void ウォッチ期間が負の値の場合_falseを返す() {
        assertThat(WatchPeriod.of(-1).isValid()).isFalse();
      }

      /**
       * 未指定のウォッチ期間を無効と判定することを確かめる。 メールや LINE 由来のメッセージは期間を持たず、既定値を使うからである。
       *
       * @given ウォッチ期間が null
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void ウォッチ期間がnullの場合_falseを返す() {
        assertThat(WatchPeriod.of(null).isValid()).isFalse();
      }
    }
  }

  @Nested
  class resolveDays {

    @Nested
    class 正常系 {

      /**
       * 有効なウォッチ期間は指定された日数をそのまま返すことを確かめる。 ユーザーが指定した期間でウォッチを登録するからである。
       *
       * @given ウォッチ期間が7日、既定値が30日
       * @when resolveDays を30で実行する
       * @then 7を返す
       */
      @Test
      void ウォッチ期間が有効な場合_指定された日数を返す() {
        assertThat(WatchPeriod.of(7).resolveDays(DEFAULT_DAYS)).isEqualTo(7);
      }

      /**
       * 未指定のウォッチ期間は既定値を返すことを確かめる。 メールや LINE 由来のメッセージは案件・要員の TTL と同じ期間でウォッチするからである。
       *
       * @given ウォッチ期間が null、既定値が30日
       * @when resolveDays を30で実行する
       * @then 30を返す
       */
      @Test
      void ウォッチ期間がnullの場合_既定値を返す() {
        assertThat(WatchPeriod.of(null).resolveDays(DEFAULT_DAYS)).isEqualTo(DEFAULT_DAYS);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 下限の1日は指定された日数をそのまま返すことを確かめる。 範囲内の最小値を既定値に置き換えないからである。
       *
       * @given ウォッチ期間が1日、既定値が30日
       * @when resolveDays を30で実行する
       * @then 1を返す
       */
      @Test
      void ウォッチ期間が1日の場合_1日を返す() {
        assertThat(WatchPeriod.of(1).resolveDays(DEFAULT_DAYS)).isEqualTo(1);
      }

      /**
       * 上限の14日は指定された日数をそのまま返すことを確かめる。 範囲内の最大値を既定値に置き換えないからである。
       *
       * @given ウォッチ期間が14日、既定値が30日
       * @when resolveDays を30で実行する
       * @then 14を返す
       */
      @Test
      void ウォッチ期間が14日の場合_14日を返す() {
        assertThat(WatchPeriod.of(14).resolveDays(DEFAULT_DAYS)).isEqualTo(14);
      }

      /**
       * 範囲外の0日は既定値にすることを確かめる。 例外にすると SQS の受信処理が失敗して再試行を繰り返すため、登録を続けるからである。
       *
       * @given ウォッチ期間が0日、既定値が30日
       * @when resolveDays を30で実行する
       * @then 30を返す
       */
      @Test
      void ウォッチ期間が0日の場合_既定値を返す() {
        assertThat(WatchPeriod.of(0).resolveDays(DEFAULT_DAYS)).isEqualTo(DEFAULT_DAYS);
      }

      /**
       * 範囲外の15日は既定値にすることを確かめる。 例外にすると SQS の受信処理が失敗して再試行を繰り返すため、登録を続けるからである。
       *
       * @given ウォッチ期間が15日、既定値が30日
       * @when resolveDays を30で実行する
       * @then 30を返す
       */
      @Test
      void ウォッチ期間が15日の場合_既定値を返す() {
        assertThat(WatchPeriod.of(15).resolveDays(DEFAULT_DAYS)).isEqualTo(DEFAULT_DAYS);
      }

      /**
       * 負の値は既定値にすることを確かめる。 負の日数でウォッチの期限を過去にしないからである。
       *
       * @given ウォッチ期間が負の3日、既定値が30日
       * @when resolveDays を30で実行する
       * @then 30を返す
       */
      @Test
      void ウォッチ期間が負の値の場合_既定値を返す() {
        assertThat(WatchPeriod.of(-3).resolveDays(DEFAULT_DAYS)).isEqualTo(DEFAULT_DAYS);
      }

      /**
       * 範囲外の値で既定値を返すときに、WARN のログを文言と引数どおりに出すことを確かめる。 範囲外の入力を黙って既定値にすると、入力の誤りに運用で気付けないからである。
       *
       * @given ウォッチ期間が15日、既定値が30日、WatchPeriod のロガーに ListAppender を付けている
       * @when resolveDays を30で実行する
       * @then 30を返し、WARN のログを1件、引数 15、1、14、30 で出す
       */
      @Test
      void ウォッチ期間が範囲外の場合_WARNのログを出して既定値を返す() {
        Logger logger = (Logger) LoggerFactory.getLogger(WatchPeriod.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        int result;
        try {
          result = WatchPeriod.of(15).resolveDays(DEFAULT_DAYS);
        } finally {
          logger.detachAppender(appender);
        }

        assertThat(result).isEqualTo(DEFAULT_DAYS);
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getMessage())
            .isEqualTo("watch_periodが範囲外のため既定値を適用します。watch_period={}, 許容範囲={}〜{}, 既定値={}");
        assertThat(event.getArgumentArray()).containsExactly(15, 1, 14, 30);
      }
    }
  }
}
