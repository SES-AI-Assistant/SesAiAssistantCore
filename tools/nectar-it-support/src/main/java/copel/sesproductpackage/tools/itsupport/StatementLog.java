package copel.sesproductpackage.tools.itsupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IT 用コンテナの PostgreSQL が出力した SQL のログを読み取り、アプリが実行した SQL を取り出します.
 *
 * <p>コンテナの PostgreSQL は {@code log_statement=all}、{@code log_line_prefix='PGSQL|%a| '}
 * で起動します。拡張クエリ（PreparedStatement）の SQL は {@code $1} のプレースホルダーのまま {@code LOG: execute ...}
 * に出て、バインド値は直後の {@code DETAIL: parameters:} に出ます。この2行を1つにまとめ、プレースホルダーをバインド値で置き換えた「実際に DB が受け取った
 * SQL」を作ります。Core の実装がログに出す SQL と違い、実装の組み立てとログのずれがありません.
 *
 * @author Copel Co., Ltd.
 */
public final class StatementLog {

  /** テストの準備・確認用の接続の application_name です。アプリの SQL と区別するために除外します. */
  public static final String SETUP_APPLICATION_NAME = "it-setup";

  /** アプリの SQL ではない接続の application_name です（初期化スクリプトの psql など）. */
  private static final Set<String> IGNORED_APPLICATIONS =
      Set.of(SETUP_APPLICATION_NAME, "psql", "[unknown]", "");

  /** PostgreSQL のログの1行です。接頭辞の {@code PGSQL|アプリ名| } に続けて、レベルと本文が並びます. */
  private static final Pattern LOG_LINE = Pattern.compile("^PGSQL\\|(.*?)\\| ([A-Z]+):  (.*)$");

  /** 実行した SQL を示す本文です。名前つきの準備済み文（{@code S_1}）と名前なし（{@code <unnamed>}）の両方に合います. */
  private static final Pattern EXECUTE = Pattern.compile("^(?:execute [^:]*|statement): (.*)$");

  /**
   * DETAIL のバインド値の1つ分です。文字列は引用符で囲まれ、引用符は2つ重ねて表されます.
   *
   * <p>引用符以外の連続を {@code [^']++} で1回に読むのは、1文字ずつ繰り返すと、ベクトル（数千文字）のような長い値で正規表現の再帰が深くなり {@link
   * StackOverflowError} になるため。
   */
  private static final Pattern PARAMETER = Pattern.compile("\\$(\\d+) = ('(?:[^']++|'')*+'|NULL)");

  /** SQL の中のプレースホルダーです. */
  private static final Pattern PLACEHOLDER = Pattern.compile("\\$(\\d+)");

  /** 接続のたびに JDBC ドライバーが送る設定の文で、アプリの処理ではないため除外します. */
  private static final Pattern DRIVER_NOISE =
      Pattern.compile("^SET (extra_float_digits|application_name)\\b.*", Pattern.CASE_INSENSITIVE);

  private StatementLog() {}

  /**
   * アプリが実行した SQL の1件です.
   *
   * @param applicationName 接続の application_name
   * @param sql 実行した SQL。プレースホルダー（{@code $1}）のまま
   * @param boundSql プレースホルダーをバインド値で置き換えた SQL。バインド値が無い場合は {@code sql} と同じ
   */
  public record Statement(String applicationName, String sql, String boundSql) {}

  /**
   * コンテナのログ全体から、アプリが実行した SQL を実行順に取り出します.
   *
   * @param containerLog コンテナの標準出力と標準エラー出力。moto など PostgreSQL 以外の行が混ざっていてもよい
   * @return アプリが実行した SQL のリスト。テストの準備用の接続と JDBC ドライバーの設定の文は含まない
   */
  public static List<Statement> parse(final String containerLog) {
    List<Statement> statements = new ArrayList<>();
    String application = null;
    StringBuilder sql = null;
    StringBuilder parameters = null;
    // 直前の行が SQL と DETAIL のどちらの続きかを表す。PostgreSQL は複数行のメッセージの2行目以降を、行頭のタブで区切る
    StringBuilder last = null;
    for (String line : containerLog.split("\\R")) {
      Matcher log = LOG_LINE.matcher(line);
      if (log.matches()) {
        String level = log.group(2);
        String body = log.group(3);
        Matcher execute = EXECUTE.matcher(body);
        if ("LOG".equals(level) && execute.matches()) {
          add(statements, application, sql, parameters);
          application = log.group(1);
          sql = new StringBuilder(execute.group(1));
          parameters = new StringBuilder();
          last = sql;
        } else if ("DETAIL".equals(level) && sql != null && body.startsWith("parameters: ")) {
          parameters.append(body.substring("parameters: ".length()));
          last = parameters;
        } else {
          add(statements, application, sql, parameters);
          application = null;
          sql = null;
          parameters = null;
          last = null;
        }
      } else if (line.startsWith("\t") && last != null) {
        last.append('\n').append(line.substring(1));
      } else {
        add(statements, application, sql, parameters);
        application = null;
        sql = null;
        parameters = null;
        last = null;
      }
    }
    add(statements, application, sql, parameters);
    return statements;
  }

  /**
   * 組み立て中の SQL を、除外の対象でなければ結果に加えます.
   *
   * @param statements 結果
   * @param application 接続の application_name。SQL を組み立てていない場合は null
   * @param sql 組み立て中の SQL。無い場合は null
   * @param parameters 組み立て中のバインド値の文字列
   */
  private static void add(
      final List<Statement> statements,
      final String application,
      final StringBuilder sql,
      final StringBuilder parameters) {
    if (sql == null
        || IGNORED_APPLICATIONS.contains(application)
        || DRIVER_NOISE.matcher(sql.toString().strip()).matches()) {
      return;
    }
    statements.add(new Statement(application, sql.toString(), bind(sql.toString(), parameters)));
  }

  /**
   * SQL のプレースホルダーを、ログに出たバインド値で置き換えます.
   *
   * <p>置き換えは1回の走査で行うため、{@code $1} と {@code $10} を取り違えません。バインド値はログの表記のまま（文字列は引用符つき、NULL は {@code
   * NULL}）入れます.
   *
   * @param sql プレースホルダーを含む SQL
   * @param parameters ログの {@code DETAIL: parameters:} の本文。バインド値が無い場合は空
   * @return バインド値を入れた SQL
   */
  private static String bind(final String sql, final CharSequence parameters) {
    Map<String, String> values = new HashMap<>();
    Matcher parameter = PARAMETER.matcher(parameters);
    while (parameter.find()) {
      values.put(parameter.group(1), parameter.group(2));
    }
    if (values.isEmpty()) {
      return sql;
    }
    Matcher placeholder = PLACEHOLDER.matcher(sql);
    StringBuilder bound = new StringBuilder();
    while (placeholder.find()) {
      placeholder.appendReplacement(
          bound,
          Matcher.quoteReplacement(values.getOrDefault(placeholder.group(1), placeholder.group())));
    }
    placeholder.appendTail(bound);
    return bound.toString();
  }
}
