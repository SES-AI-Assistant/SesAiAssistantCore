package copel.sesproductpackage.tools.itsupport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

/**
 * IT1 の1ケースの間に出たアプリのログと、アプリが DB に実行した SQL を記録する JUnit の拡張です.
 *
 * <p>記録したログは、テストからアサーションに使えます。同時に、ケースごとのファイル（{@code logs/テストクラス/結果_ケース.log}）にも書き出します。ファイルの形式は本番の
 * {@code logback.xml} と同じ LogstashEncoder の JSON で、Lambda が CloudWatch Logs
 * に出すログと同じものを読めます。最初のケースより前に出たログ（Lambda の INIT にあたる、ハンドラの読み込み時の設定の読み込みなど）は {@code logs/00_初期化.log}
 * に書き出します.
 *
 * <p>{@link ItEnvironment} を起動している場合は、アプリが DB に実行した SQL も、バインド値を埋めた形でケースごとのファイル（{@code
 * 結果_ケース.sql.txt}）に書き出し、{@link #sqls()} からアサーションに使えます。SQL は PostgreSQL が受け取ったものをログから読むため、 実装がログに出す
 * SQL とのずれがありません.
 *
 * <p>ケースの間に WARN 以上のログが出た場合は、ケースを失敗にします。ただし、{@link #allowWarning(String)} で許可した文言を含むログは除きます。ステータスが
 * 200 でも、内部でエラーを握りつぶして続行した場合などを見逃さないためです。許可した文言が1件も出なかった場合も、ケースを失敗にします.
 *
 * @author Copel Co., Ltd.
 */
public final class ItLogRecorder
    implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

  /** ログの出力先のフォルダです。failsafe が設定します. */
  private static final Path LOG_DIR =
      Path.of(System.getProperty("nectar.it.log.dir", "target/it-results/logs"));

  /** 初期化時のログを書き出すアペンダーです。最初のケースが始まったら外し、以降は null にします. */
  private static FileAppender<ILoggingEvent> initAppender;

  /** 初期化時のログの記録を始めたかどうかです。@Nested のクラスごとに beforeAll が呼ばれるため、1回だけ始めます. */
  private static boolean initStarted;

  /** アサーションに使うため、ケースのログを受け取るアペンダーです。ケースごとに作り直します. */
  private ListAppender<ILoggingEvent> appender;

  /** ケースのログをファイルに書き出すアペンダーです。ケースごとに作り直します. */
  private FileAppender<ILoggingEvent> fileAppender;

  /** ケースの開始時点でアプリが実行済みだった SQL の件数です。ケースの SQL を切り出す起点に使います. */
  private int sqlMark;

  /** ケースで WARN 以上のログが出てもよいと全許可したかどうかです。ケースごとに戻します. */
  private boolean warningsAllowed;

  /** ケースで出てもよい WARN 以上のログの文言です。{@link #allowWarning(String)} で追加し、ケースごとに空に戻します. */
  private final Set<String> allowedWarningTexts = new LinkedHashSet<>();

  /**
   * テストケースの結果と名前から、そのケースのログファイルの場所を返します。実行結果のレポートにログを載せるときにも使います.
   *
   * @param className テストクラスのバイナリ名。@Nested のクラスは「パッケージ.外側$内側」の形
   * @param methodName テストメソッドの名前
   * @param result 結果。成功、失敗、スキップのいずれか
   * @return ログの出力先のフォルダからの相対パス
   */
  public static Path logFile(final String className, final String methodName, final String result) {
    return Path.of(
        className.substring(className.lastIndexOf('.') + 1).replace('$', '.'),
        result + "_" + methodName + ".log");
  }

  /**
   * ケースのログファイルに対応する、SQL のファイルの場所を返します.
   *
   * @param logFile {@link #logFile(String, String, String)} が返したログファイル
   * @return 同じ場所の SQL のファイル
   */
  public static Path sqlFile(final Path logFile) {
    String name = logFile.getFileName().toString();
    return logFile.resolveSibling(name.substring(0, name.length() - ".log".length()) + ".sql.txt");
  }

  /**
   * 最初のテストクラスの開始時に、初期化時のログの記録を始めます.
   *
   * <p>ハンドラの static 初期化は最初のテストのインスタンスを作るときに走るため、beforeEach より前のこの時点から記録します.
   *
   * @param context JUnit の実行コンテキスト
   * @throws IOException フォルダを作れない場合
   */
  @Override
  public void beforeAll(final ExtensionContext context) throws IOException {
    synchronized (ItLogRecorder.class) {
      if (!initStarted) {
        initStarted = true;
        initAppender = startFileAppender(LOG_DIR.resolve("00_初期化.log"));
      }
    }
  }

  /**
   * 初期化時のログの記録を終え、ケースのログの記録を始めます.
   *
   * <p>許可した文言は、ケースごとに空に戻します。前のケースの許可が次のケースに残らないようにするためです.
   *
   * @param context JUnit の実行コンテキスト
   * @throws IOException フォルダを作れない場合
   */
  @Override
  public void beforeEach(final ExtensionContext context) throws IOException {
    synchronized (ItLogRecorder.class) {
      if (initAppender != null) {
        rootLogger().detachAppender(initAppender);
        initAppender.stop();
        initAppender = null;
      }
    }
    ItEnvironment environment = ItEnvironment.current();
    this.sqlMark = environment == null ? 0 : environment.statementCount();
    this.warningsAllowed = false;
    this.allowedWarningTexts.clear();
    this.appender = new ListAppender<>();
    this.appender.start();
    rootLogger().addAppender(this.appender);
    // 結果はケースの終了まで分からないため、結果を付けずに書き出し、終了後に結果を付けた名前に変える
    this.fileAppender =
        startFileAppender(
            LOG_DIR.resolve(
                logFile(
                    context.getRequiredTestClass().getName(),
                    context.getRequiredTestMethod().getName(),
                    "実行中")));
  }

  /**
   * ケースのログと SQL の記録を終え、ファイルの名前に結果を付けます.
   *
   * <p>想定外の WARN 以上のログ、または許可した文言が1件も出なかった場合は、ファイルの結果を失敗にしたうえでケースを失敗にします。
   * ただし、許可した文言の取りこぼしは、テスト本体が既に失敗している場合は検査しません。本体の失敗の原因を隠さないためです.
   *
   * @param context JUnit の実行コンテキスト
   * @throws IOException ファイルの名前を変えられない場合
   */
  @Override
  public void afterEach(final ExtensionContext context) throws IOException {
    rootLogger().detachAppender(this.appender);
    rootLogger().detachAppender(this.fileAppender);
    this.fileAppender.stop();

    boolean testFailed = context.getExecutionException().isPresent();
    List<String> warnings = this.warnings();
    List<String> problems = new ArrayList<>();
    // 全許可のケースでは、想定外の WARN 以上のログを検査しない
    List<String> unexpected =
        this.warningsAllowed ? List.of() : unexpectedWarnings(warnings, this.allowedWarningTexts);
    if (!unexpected.isEmpty()) {
      problems.add(
          "想定外の WARN 以上のログが出ました（意図したものなら allowWarning(String) で許可する文言を指定する）: " + unexpected);
    }
    // 許可した文言の取りこぼしは、テスト本体が失敗している場合は検査しない
    List<String> missing =
        testFailed ? List.of() : missingAllowedTexts(warnings, this.allowedWarningTexts);
    if (!missing.isEmpty()) {
      problems.add("allowWarning で許可した文言が1件も出ませんでした: " + missing);
    }
    String failure = String.join("\n", problems);
    boolean failed = testFailed || !failure.isEmpty();

    String className = context.getRequiredTestClass().getName();
    String methodName = context.getRequiredTestMethod().getName();
    Path finished = LOG_DIR.resolve(logFile(className, methodName, failed ? "失敗" : "成功"));
    Files.move(
        LOG_DIR.resolve(logFile(className, methodName, "実行中")),
        finished,
        StandardCopyOption.REPLACE_EXISTING);
    Files.writeString(
        sqlFile(finished),
        this.sqls().stream().collect(Collectors.joining("\n\n")),
        StandardCharsets.UTF_8);
    if (!failure.isEmpty()) {
      throw new AssertionError(failure);
    }
  }

  /**
   * 記録したログのメッセージを、出力された順に返します.
   *
   * @return ログのメッセージのリスト
   */
  public List<String> messages() {
    return this.appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
  }

  /**
   * 記録したログのうち、WARN 以上（ERROR を含む）のレベルのメッセージを、出力された順に返します.
   *
   * @return WARN 以上のログのメッセージのリスト
   */
  public List<String> warnings() {
    return this.appender.list.stream()
        .filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
        .map(ILoggingEvent::getFormattedMessage)
        .toList();
  }

  /**
   * アプリが DB に実行した SQL を、バインド値を埋めた形で、実行された順に返します.
   *
   * <p>テストの準備・確認に使う {@link ItEnvironment#execute} と {@link ItEnvironment#query} の SQL は含みません。{@link
   * ItEnvironment} を起動していない場合は空です.
   *
   * @return バインド値を埋めた SQL のリスト
   */
  public List<String> sqls() {
    ItEnvironment environment = ItEnvironment.current();
    if (environment == null) {
      return List.of();
    }
    return environment.statementsSince(this.sqlMark).stream()
        .map(StatementLog.Statement::boundSql)
        .toList();
  }

  /**
   * このケースで出てもよい WARN 以上のログの文言を1件追加します。ケースの終了時に、ログのメッセージがこの文言を含むかで照合します.
   *
   * <p>照合は部分一致で、どれか1つの文言を含むログを許可します。許可した文言が1件も出なかった場合は、ケースを失敗にします。 同じ文言が複数回出ても失敗にしません。エラーをログに出して
   * 400 などを返す仕様のときに、意図したログだけを許可するために使います.
   *
   * @param text 許可する文言。null と空白だけの文字列は指定できません
   * @throws IllegalArgumentException text が null または空白だけの場合
   */
  public void allowWarning(final String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("許可する文言は null と空白だけの文字列にできません");
    }
    this.allowedWarningTexts.add(text);
  }

  /**
   * このケースでは WARN 以上のログが出ることを期待していると宣言し、想定外のログを許可します.
   *
   * <p>許可する文言を {@link #allowWarning(String)} で指定すること。他リポジトリの移行後に削除予定です。
   *
   * <p>全許可と {@link #allowWarning(String)} を同じケースで使った場合は、全許可が優先されます。想定外のログは失敗にしません。 ただし、{@link
   * #allowWarning(String)} で指定した文言が1件も出なかった場合は、全許可でも失敗にします。
   *
   * @deprecated 許可する文言を {@link #allowWarning(String)} で指定すること。他リポジトリの移行後に削除予定
   */
  @Deprecated
  public void allowWarnings() {
    this.warningsAllowed = true;
  }

  /**
   * 許可した文言のどれも含まない WARN 以上のメッセージを、出力された順に返します.
   *
   * <p>照合は部分一致です。メッセージが許可した文言を1つでも含めば、そのメッセージは想定内とみなします.
   *
   * @param warnings WARN 以上のログのメッセージのリスト
   * @param allowedTexts 許可した文言の集まり
   * @return 想定外のメッセージのリスト。想定外のものが無い場合は空
   */
  static List<String> unexpectedWarnings(
      final List<String> warnings, final Collection<String> allowedTexts) {
    return warnings.stream()
        .filter(message -> allowedTexts.stream().noneMatch(message::contains))
        .toList();
  }

  /**
   * 1件も該当するログが出なかった、許可した文言を許可した順に返します.
   *
   * <p>同じ文言が複数回出ていれば、取りこぼしではないとみなします。1件以上出ていれば十分です.
   *
   * @param warnings WARN 以上のログのメッセージのリスト
   * @param allowedTexts 許可した文言の集まり
   * @return 出なかった文言のリスト。すべて出た場合は空
   */
  static List<String> missingAllowedTexts(
      final List<String> warnings, final Collection<String> allowedTexts) {
    return allowedTexts.stream()
        .filter(text -> warnings.stream().noneMatch(message -> message.contains(text)))
        .toList();
  }

  /**
   * 本番と同じ JSON の形式でファイルに書き出すアペンダーを作り、ルートロガーに付けます.
   *
   * <p>イベントを受けた時点で書き出すため、ハンドラが処理の最後に MDC を消しても requestId がログに残ります.
   *
   * @param file 書き出すファイル
   * @return 開始したアペンダー
   * @throws IOException フォルダを作れない場合
   */
  private static FileAppender<ILoggingEvent> startFileAppender(final Path file) throws IOException {
    Files.createDirectories(file.getParent());
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    LogstashEncoder encoder = new LogstashEncoder();
    encoder.setContext(loggerContext);
    encoder.start();
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setContext(loggerContext);
    fileAppender.setFile(file.toString());
    fileAppender.setAppend(false);
    fileAppender.setEncoder(encoder);
    fileAppender.start();
    rootLogger().addAppender(fileAppender);
    return fileAppender;
  }

  /**
   * logback のルートロガーを返します.
   *
   * @return ルートロガー
   */
  private static Logger rootLogger() {
    return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
  }
}
