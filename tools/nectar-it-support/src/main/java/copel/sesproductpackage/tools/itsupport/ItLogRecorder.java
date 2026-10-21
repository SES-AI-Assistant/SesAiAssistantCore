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
import java.util.List;
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
 * <p>ケースの間に WARN 以上のログが出た場合は、{@link #allowWarnings()} を呼んでいない限りケースを失敗にします。ステータスが 200
 * でも、内部でエラーを握りつぶして続行した場合などを見逃さないためです.
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

  /** ケースで WARN 以上のログが出てもよいと宣言したかどうかです。ケースごとに戻します. */
  private boolean warningsAllowed;

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
   * <p>想定外の WARN 以上のログが出ていた場合は、ファイルの結果を失敗にしたうえでケースを失敗にします.
   *
   * @param context JUnit の実行コンテキスト
   * @throws IOException ファイルの名前を変えられない場合
   */
  @Override
  public void afterEach(final ExtensionContext context) throws IOException {
    rootLogger().detachAppender(this.appender);
    rootLogger().detachAppender(this.fileAppender);
    this.fileAppender.stop();
    String unexpectedWarnings = this.unexpectedWarnings();
    boolean failed = context.getExecutionException().isPresent() || !unexpectedWarnings.isEmpty();

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
    if (!unexpectedWarnings.isEmpty()) {
      throw new AssertionError(unexpectedWarnings);
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
   * 記録したログのうち、WARN 以上のレベルのメッセージを返します.
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
   * このケースでは WARN 以上のログが出ることを期待していると宣言します。エラーをログに出して 400 などを返す仕様のときに呼びます.
   *
   * <p>宣言したケースでも、出た WARN 以上のログの内容は {@link #warnings()} で確かめてください.
   */
  public void allowWarnings() {
    this.warningsAllowed = true;
  }

  /**
   * 想定外の WARN 以上のログがあれば、失敗の内容を返します.
   *
   * @return 失敗の内容。想定外のログが無い場合は空文字
   */
  private String unexpectedWarnings() {
    List<String> warnings = this.warnings();
    if (this.warningsAllowed || warnings.isEmpty()) {
      return "";
    }
    return "想定外の WARN 以上のログが出ました（意図したものなら allowWarnings() を呼ぶ）: " + warnings;
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
