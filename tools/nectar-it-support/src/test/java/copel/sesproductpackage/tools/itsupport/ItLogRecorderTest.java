package copel.sesproductpackage.tools.itsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ItLogRecorder} の単体テスト.
 *
 * @author Copel Co., Ltd.
 */
class ItLogRecorderTest {

  /** ケースの中でアプリのログに見立てて出すロガーです。WARN 以上の判定を実際のログ経路で確かめるために使います. */
  private static final Logger LOG = LoggerFactory.getLogger(ItLogRecorderTest.class);

  @Nested
  class unexpectedWarnings {

    @Nested
    class 正常系 {

      /**
       * 許可した文言が無い場合に、すべての WARN 以上のログを返すことを確かめる。 許可していないログを黙って通すと、握りつぶされたエラーを見逃すので防ぐ必要がある。
       *
       * @given WARN のメッセージが "DB接続に失敗" と "要約が空" の2件、許可した文言が無い
       * @when unexpectedWarnings を実行する
       * @then 2件とも返す
       */
      @Test
      void 許可した文言が無い場合_すべてのログを返す() {
        assertThat(ItLogRecorder.unexpectedWarnings(List.of("DB接続に失敗", "要約が空"), List.of()))
            .containsExactly("DB接続に失敗", "要約が空");
      }

      /**
       * 許可した文言を含むログを除き、残りのログを返すことを確かめる。 意図したログだけを許可し、同じケースで別の WARN が出ても見逃さないための仕様である。
       *
       * @given WARN のメッセージが "要約が空だったため既定値にした" と "DB接続に失敗"、許可した文言が "要約が空"
       * @when unexpectedWarnings を実行する
       * @then "DB接続に失敗" だけを返す
       */
      @Test
      void 文言を含むログがある場合_含まないログだけを返す() {
        assertThat(
                ItLogRecorder.unexpectedWarnings(
                    List.of("要約が空だったため既定値にした", "DB接続に失敗"), List.of("要約が空")))
            .containsExactly("DB接続に失敗");
      }

      /**
       * 許可の照合が部分一致であり、前後に文字があるログも許可することを確かめる。 ログの前後に件数が付くため、文言を含んでいれば許可する仕様である。
       *
       * @given WARN のメッセージが "件数 3 の両方存在を検出"、許可した文言が "両方存在"
       * @when unexpectedWarnings を実行する
       * @then 空のリストを返す
       */
      @Test
      void 文言が前後に文字のあるログに含まれる場合_想定内として空のリストを返す() {
        assertThat(ItLogRecorder.unexpectedWarnings(List.of("件数 3 の両方存在を検出"), List.of("両方存在")))
            .isEmpty();
      }

      /**
       * 複数の許可した文言のどれも含まないログだけを返すことを確かめる。 文言が増えたときに、片方だけが効いて残りが想定外になるのを防ぐ仕様である。
       *
       * @given WARN のメッセージが "A を処理"、"B を処理"、"C を処理" の3件、許可した文言が "A"、"B"
       * @when unexpectedWarnings を実行する
       * @then "C を処理" だけを返す
       */
      @Test
      void 複数の文言の場合_どの文言も含まないログだけを返す() {
        assertThat(
                ItLogRecorder.unexpectedWarnings(
                    List.of("A を処理", "B を処理", "C を処理"), List.of("A", "B")))
            .containsExactly("C を処理");
      }

      /**
       * ERROR のメッセージが許可していなければ想定外として返すことを確かめる。 ERROR も WARN 以上の対象であり、ERROR
       * だけを黙って通すと失敗が検知できないので防ぐ仕様である。
       *
       * @given WARN 以上のメッセージが "ERROR 要約の生成に失敗" の1件、許可した文言が "別の文言"
       * @when unexpectedWarnings を実行する
       * @then "ERROR 要約の生成に失敗" を返す
       */
      @Test
      void ERRORのメッセージが含まれる場合_想定外としてそのメッセージを返す() {
        assertThat(ItLogRecorder.unexpectedWarnings(List.of("ERROR 要約の生成に失敗"), List.of("別の文言")))
            .containsExactly("ERROR 要約の生成に失敗");
      }
    }

    @Nested
    class 境界値 {

      /**
       * WARN 以上のログが1件も無い場合に、想定外のログも無いことを確かめる。 ログが出ていないケースは、想定外のログが無い正常なケースとして扱う仕様である。
       *
       * @given WARN のメッセージが無く、許可した文言が "A"
       * @when unexpectedWarnings を実行する
       * @then 空のリストを返す
       */
      @Test
      void ログが無い場合_空のリストを返す() {
        assertThat(ItLogRecorder.unexpectedWarnings(List.of(), List.of("A"))).isEmpty();
      }

      /**
       * 同じ文言を含むログが複数回出た場合に、すべて想定内として扱うことを確かめる。 同じ警告を繰り返す仕様で、回数の違いによって失敗が変わらないようにする必要がある。
       *
       * @given WARN のメッセージが "A を処理" と "A を再試行" の2件、許可した文言が "A"
       * @when unexpectedWarnings を実行する
       * @then 空のリストを返す
       */
      @Test
      void 同じ文言を含むログが2件ある場合_どちらも想定内として空のリストを返す() {
        assertThat(ItLogRecorder.unexpectedWarnings(List.of("A を処理", "A を再試行"), List.of("A")))
            .isEmpty();
      }
    }
  }

  @Nested
  class missingAllowedTexts {

    @Nested
    class 正常系 {

      /**
       * 許可した文言がすべてログに出た場合に、取りこぼしが無いことを確かめる。 許可した警告が実際に起きたことを確かめる検査であり、出ていれば失敗にしない仕様である。
       *
       * @given WARN のメッセージが "A を処理" と "B を処理"、許可した文言が "A"、"B"
       * @when missingAllowedTexts を実行する
       * @then 空のリストを返す
       */
      @Test
      void 許可した文言がすべて出た場合_空のリストを返す() {
        assertThat(ItLogRecorder.missingAllowedTexts(List.of("A を処理", "B を処理"), List.of("A", "B")))
            .isEmpty();
      }

      /**
       * 許可した文言のうち出なかったものだけを返すことを確かめる。 期待した警告が出なかった文言を示して知らせる仕様であり、どの文言かを特定できるようにする。
       *
       * @given WARN のメッセージが "A を処理" の1件、許可した文言が "A"、"B"
       * @when missingAllowedTexts を実行する
       * @then "B" だけを返す
       */
      @Test
      void 一部の文言が出なかった場合_出なかった文言だけを返す() {
        assertThat(ItLogRecorder.missingAllowedTexts(List.of("A を処理"), List.of("A", "B")))
            .containsExactly("B");
      }

      /**
       * 許可した文言が1件も出なかった場合に、すべての文言を許可した順に返すことを確かめる。 許可した警告がまったく出ていない状態を取りこぼしとして検知する仕様である。
       *
       * @given WARN のメッセージが無く、許可した文言が "A"、"B"
       * @when missingAllowedTexts を実行する
       * @then "A"、"B" の順に返す
       */
      @Test
      void 文言が1件も出ない場合_すべての文言を許可した順のリストを返す() {
        assertThat(ItLogRecorder.missingAllowedTexts(List.of(), List.of("A", "B")))
            .containsExactly("A", "B");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 許可した文言が無い場合に、取りこぼしも無いことを確かめる。 文言を指定していないケースは、期待した警告が無い検査の対象外とする仕様である。
       *
       * @given WARN のメッセージが "A"、許可した文言が無い
       * @when missingAllowedTexts を実行する
       * @then 空のリストを返す
       */
      @Test
      void 許可した文言が無い場合_空のリストを返す() {
        assertThat(ItLogRecorder.missingAllowedTexts(List.of("A"), List.of())).isEmpty();
      }

      /**
       * 同じ文言が2件出た場合に、取りこぼしとして扱わないことを確かめる。 1件以上出ていれば期待した警告は起きているので、回数では失敗にしない仕様である。
       *
       * @given WARN のメッセージが "A を1回目" と "A を2回目" の2件、許可した文言が "A"
       * @when missingAllowedTexts を実行する
       * @then 空のリストを返す
       */
      @Test
      void 同じ文言が2件出た場合_空のリストを返す() {
        assertThat(ItLogRecorder.missingAllowedTexts(List.of("A を1回目", "A を2回目"), List.of("A")))
            .isEmpty();
      }
    }
  }

  @Nested
  class allowWarning {

    @Nested
    class 異常系 {

      /**
       * 許可する文言が null の場合に例外を投げることを確かめる。 null の文言は全ログに一致する扱いになり、想定外の検知を無効にするので防ぐ必要がある。
       *
       * @given 許可する文言が null
       * @when allowWarning を null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 文言がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new ItLogRecorder().allowWarning(null))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 許可する文言が空文字の場合に例外を投げることを確かめる。 空文字は全ログに含まれるため、想定外の検知を無効にするので防ぐ必要がある。
       *
       * @given 許可する文言が空文字
       * @when allowWarning を空文字で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 文言が空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new ItLogRecorder().allowWarning(""))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 許可する文言が空白だけの場合に例外を投げることを確かめる。 空白だけの文言は意図しないログにも含まれ、想定外の検知を弱めるので防ぐ必要がある。
       *
       * @given 許可する文言が半角スペース1つ
       * @when allowWarning を半角スペースで実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 文言が空白だけの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new ItLogRecorder().allowWarning(" "))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class beforeEach {

    @Nested
    class 境界値 {

      /**
       * 次のケースの開始時に、前のケースで許可した文言が失効することを確かめる。 前のケースの許可が残ると、無関係なケースの想定外の WARN を見逃すので防ぐ必要がある。
       *
       * @given 1件目のケースで文言 "前のケースの文言" を許可し、それを含むログを出して終了する
       * @when 2件目のケースで同じ文言を含むログを出して終了する
       * @then 想定外のログとしてAssertionErrorを投げる
       */
      @Test
      void 前のケースで許可した文言を含むWARNが次のケースで出た場合_AssertionErrorを投げる() throws Exception {
        ItLogRecorder recorder = new ItLogRecorder();
        runCase(
            recorder,
            Optional.empty(),
            () -> {
              recorder.allowWarning("前のケースの文言");
              LOG.warn("前のケースの文言を含むログ");
            });

        assertThatThrownBy(
                () -> runCase(recorder, Optional.empty(), () -> LOG.warn("前のケースの文言を含むログ")))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("前のケースの文言を含むログ");
      }
    }
  }

  @Nested
  class afterEach {

    @Nested
    class 正常系 {

      /**
       * 許可した文言を含む WARN が出た場合に、ケースを失敗にしないことを確かめる。 意図して出した警告を許可し、同じ仕様の既存のテストを通すための仕様である。
       *
       * @given 許可した文言が "両方存在"、WARN のメッセージが "両方存在のため重複扱い"
       * @when ケースを開始し、そのログを出して終了する
       * @then 何もしない
       */
      @Test
      void 許可した文言を含むWARNが出た場合_何もしない() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatCode(
                () ->
                    runCase(
                        recorder,
                        Optional.empty(),
                        () -> {
                          recorder.allowWarning("両方存在");
                          LOG.warn("両方存在のため重複扱い");
                        }))
            .doesNotThrowAnyException();
      }

      /**
       * 許可した文言を含む ERROR が出た場合に、ケースを失敗にしないことを確かめる。 ERROR も WARN 以上の対象であり、許可の照合も ERROR に対して行う仕様である。
       *
       * @given 許可した文言が "要約が空"、ERROR のメッセージが "要約が空のため既定値にした"
       * @when ケースを開始し、そのログを出して終了する
       * @then 何もしない
       */
      @Test
      void 許可した文言を含むERRORが出た場合_何もしない() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatCode(
                () ->
                    runCase(
                        recorder,
                        Optional.empty(),
                        () -> {
                          recorder.allowWarning("要約が空");
                          LOG.error("要約が空のため既定値にした");
                        }))
            .doesNotThrowAnyException();
      }

      /**
       * WARN 以上のログが出なかった場合に、許可していなくても失敗にしないことを確かめる。 警告の無い通常のケースを失敗にしないことが、このレコーダーの前提である。
       *
       * @given 許可した文言が無く、ログを出さない
       * @when ケースを開始して終了する
       * @then 何もしない
       */
      @Test
      void WARN以上のログが無い場合_何もしない() {
        assertThatCode(() -> runCase(new ItLogRecorder(), Optional.empty(), () -> {}))
            .doesNotThrowAnyException();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 許可していない WARN が出た場合に、想定外のログを示して失敗にすることを確かめる。 握りつぶされたエラーを見逃さないことが、このレコーダーの目的である。
       *
       * @given 許可した文言が無く、WARN のメッセージが "DB接続に失敗"
       * @when ケースを開始し、そのログを出して終了する
       * @then 想定外のログを示すAssertionErrorを投げる
       */
      @Test
      void 許可していないWARNが出た場合_想定外のログを示すAssertionErrorを投げる() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatThrownBy(() -> runCase(recorder, Optional.empty(), () -> LOG.warn("DB接続に失敗")))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("想定外の WARN 以上のログ")
            .hasMessageContaining("DB接続に失敗");
      }

      /**
       * 許可していない ERROR が出た場合に、想定外のログを示して失敗にすることを確かめる。 ERROR も WARN 以上の対象であり、エラーの握りつぶしを見逃さない仕様である。
       *
       * @given 許可した文言が無く、ERROR のメッセージが "DB接続に失敗"
       * @when ケースを開始し、そのログを出して終了する
       * @then 想定外のログを示すAssertionErrorを投げる
       */
      @Test
      void 許可していないERRORが出た場合_想定外のログを示すAssertionErrorを投げる() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatThrownBy(() -> runCase(recorder, Optional.empty(), () -> LOG.error("DB接続に失敗")))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("想定外の WARN 以上のログ")
            .hasMessageContaining("DB接続に失敗");
      }

      /**
       * 許可した文言が1件も出なかった場合に、その文言を示して失敗にすることを確かめる。 期待した警告が起きなかったときに、テストが誤って通ることを防ぐ仕様である。
       *
       * @given 許可した文言が "両方存在"、WARN のメッセージが無い
       * @when ケースを開始し、ログを出さずに終了する
       * @then 取りこぼしを示すAssertionErrorを投げる
       */
      @Test
      void 許可した文言が1件も出なかった場合_取りこぼしを示すAssertionErrorを投げる() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatThrownBy(
                () -> runCase(recorder, Optional.empty(), () -> recorder.allowWarning("両方存在")))
            .isInstanceOf(AssertionError.class)
            .hasMessage("allowWarning で許可した文言が1件も出ませんでした: [両方存在]");
      }

      /**
       * 許可した文言が INFO でしか出なかった場合に、取りこぼしとして失敗にすることを確かめる。 検査の対象は WARN 以上であり、INFO
       * の一致で期待を満たしたことにしない仕様である。
       *
       * @given 許可した文言が "件数"、INFO のメッセージが "件数 3"
       * @when ケースを開始し、そのログを出して終了する
       * @then 取りこぼしを示すAssertionErrorを投げ、メッセージが許可した文言の取りこぼしだけを示す
       */
      @Test
      void 許可した文言がINFOでしか出ない場合_取りこぼしを示すAssertionErrorを投げる() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatThrownBy(
                () ->
                    runCase(
                        recorder,
                        Optional.empty(),
                        () -> {
                          recorder.allowWarning("件数");
                          LOG.info("件数 3");
                        }))
            .isInstanceOf(AssertionError.class)
            .hasMessage("allowWarning で許可した文言が1件も出ませんでした: [件数]");
      }

      /**
       * 想定外の WARN と許可した文言の取りこぼしが同時にある場合に、両方を示すことを確かめる。 一方だけを示すと、もう一方の問題を見落とすので防ぐ仕様である。
       *
       * @given 許可した文言が "両方存在"、WARN のメッセージが "DB接続に失敗" の1件
       * @when ケースを開始し、そのログを出して終了する
       * @then 想定外のログと取りこぼしの両方を示すAssertionErrorを投げる
       */
      @Test
      void 想定外のWARNと取りこぼしが両方ある場合_両方を示すAssertionErrorを投げる() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatThrownBy(
                () ->
                    runCase(
                        recorder,
                        Optional.empty(),
                        () -> {
                          recorder.allowWarning("両方存在");
                          LOG.warn("DB接続に失敗");
                        }))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("DB接続に失敗")
            .hasMessageContaining("allowWarning で許可した文言が1件も出ませんでした: [両方存在]");
      }

      /**
       * テスト本体が失敗した場合に、許可した文言の取りこぼしを報告せず何もしないことを確かめる。 本体の失敗の原因を、取りこぼしの報告で隠さない仕様である。
       *
       * @given テスト本体の例外があり、許可した文言が "両方存在"、ログを出さない
       * @when ケースを開始し、ログを出さずに終了する
       * @then 何もしない
       */
      @Test
      void テスト本体が失敗した場合_取りこぼしを報告せず何もしない() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatCode(
                () ->
                    runCase(
                        recorder,
                        Optional.of(new IllegalStateException("本体の失敗")),
                        () -> recorder.allowWarning("両方存在")))
            .doesNotThrowAnyException();
      }

      /**
       * テスト本体が失敗した場合も、想定外の WARN を示すことを確かめる。 想定外の WARN の検査は、本体の成否によらず行う仕様である。
       *
       * @given テスト本体の例外があり、許可した文言が無く、WARN のメッセージが "DB接続に失敗"
       * @when ケースを開始し、そのログを出して終了する
       * @then 想定外のログを示すAssertionErrorを投げる
       */
      @Test
      void テスト本体が失敗した場合でも想定外のWARNはAssertionErrorを投げる() {
        ItLogRecorder recorder = new ItLogRecorder();

        assertThatThrownBy(
                () ->
                    runCase(
                        recorder,
                        Optional.of(new IllegalStateException("本体の失敗")),
                        () -> LOG.warn("DB接続に失敗")))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("DB接続に失敗");
      }
    }
  }

  /**
   * ケースの開始と終了を、JUnit の拡張と同じ順に行う.
   *
   * @param recorder 記録する ItLogRecorder
   * @param executionException テスト本体の例外。本体が成功した場合は空
   * @param body ケースの本体。ケースの開始後、終了前に実行する
   * @throws Exception 開始または終了が例外を投げた場合
   */
  private static void runCase(
      final ItLogRecorder recorder,
      final Optional<Throwable> executionException,
      final Runnable body)
      throws Exception {
    ExtensionContext context = context(executionException);
    recorder.beforeEach(context);
    body.run();
    recorder.afterEach(context);
  }

  /**
   * ケースの名前と結果を返す JUnit の拡張のモックを作る.
   *
   * <p>ログのファイル名にだけ使うため、実在するメソッドの toString を使う。
   *
   * @param executionException テスト本体の例外。本体が成功した場合は空
   * @return 実行コンテキストのモック
   * @throws NoSuchMethodException 実在しないメソッドを指定した場合
   */
  private static ExtensionContext context(final Optional<Throwable> executionException)
      throws NoSuchMethodException {
    ExtensionContext context = mock(ExtensionContext.class);
    doReturn(ItLogRecorderTest.class).when(context).getRequiredTestClass();
    doReturn(Object.class.getMethod("toString")).when(context).getRequiredTestMethod();
    doReturn(executionException).when(context).getExecutionException();
    return context;
  }
}
