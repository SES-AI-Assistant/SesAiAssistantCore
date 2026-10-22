package copel.sesproductpackage.core.database.base;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * テナント条件を付けるSQLを表す値クラス.
 *
 * <p>入力のSQLに、主テーブルの {@code tenant_id} 条件を必ず付けます。結合（JOIN）を含む場合は、結合先のテーブルにも主テーブルと列どうしで比べる条件を付けます。
 * 句の切り出しは括弧の深さを数え、括弧の外にある（最上位の）キーワードだけを見ます。副問い合わせや関数の中の FROM・WHERE・カンマを誤認しないためです。
 *
 * <p>安全に書き換えられない形は、テナント条件の付け損ないを避けるため書き換えず、{@link IllegalArgumentException} で拒否します。
 * 拒否する形は、USING・NATURAL・集合演算、FROM 句の副問い合わせ、ON 句の無い LEFT/RIGHT/FULL 結合、INSERT 文、外側に句が続く EXISTS 文、
 * UPDATE の FROM 句、括弧の中の副問い合わせ（SELECT を含む括弧）、主テーブルのテナント条件を手書きした SQL などです。
 * 括弧の中の副問い合わせは、そこに書いた表へテナント条件が付かないため拒否します（先頭の SELECT EXISTS の括弧は中身を書き換えます）。
 *
 * @author Copel Co., Ltd.
 */
final class TenantFilterSql {

  /** 切り出しの対象となるキーワード。 */
  private static final Set<String> KEYWORDS =
      Set.of(
          "FROM",
          "WHERE",
          "SET",
          "GROUP BY",
          "ORDER BY",
          "HAVING",
          "LIMIT",
          "OFFSET",
          "FOR",
          "RETURNING",
          "JOIN",
          "INNER",
          "LEFT",
          "RIGHT",
          "FULL",
          "CROSS",
          "OUTER",
          "ON",
          "OR");

  /** 句の終わりを示すキーワード（WHERE 句の後ろに続く句）。 */
  private static final Set<String> TAIL_KEYWORDS =
      Set.of("GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET", "FOR", "RETURNING");

  /** ORDER と GROUP の後ろに続く BY 句の形。 */
  private static final Pattern BY_CLAUSE = Pattern.compile("\\s+BY\\b", Pattern.CASE_INSENSITIVE);

  /** 結合の前置語（JOIN の前に付くもの）。 */
  private static final Set<String> JOIN_MODIFIERS =
      Set.of("INNER", "LEFT", "RIGHT", "FULL", "CROSS");

  /** 括弧の直後（空白を読み飛ばした先）に現れる SELECT という単語。 SELECT の後ろに文字が続く識別子は含めない。 */
  private static final Pattern SELECT_AT =
      Pattern.compile("\\s*SELECT(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);

  /** 安全に書き換えられないため拒否するキーワード。 */
  private static final Set<String> REJECTED =
      Set.of("USING", "NATURAL", "UNION", "INTERSECT", "EXCEPT");

  /** 表名と任意の別名（AS 付きも可）。 */
  private static final Pattern TABLE_REF =
      Pattern.compile("(\\w+)(?:\\s+(?:AS\\s+)?([A-Za-z_]\\w*))?", Pattern.CASE_INSENSITIVE);

  /** 括弧を使った SELECT EXISTS 文の先頭（末尾の開き括弧まで）。 */
  private static final Pattern EXISTS_PREFIX =
      Pattern.compile("^SELECT\\s+EXISTS\\s*\\(", Pattern.CASE_INSENSITIVE);

  /** INSERT 文の先頭。 */
  private static final Pattern INSERT_PREFIX =
      Pattern.compile("^INSERT\\b", Pattern.CASE_INSENSITIVE);

  /** UPDATE 文の先頭。 */
  private static final Pattern UPDATE_PREFIX =
      Pattern.compile("^UPDATE\\b", Pattern.CASE_INSENSITIVE);

  /** 修飾の有無を問わない tenant_id の列参照。 */
  private static final Pattern TENANT_ID_COLUMN =
      Pattern.compile(
          "(?<![A-Za-z0-9_])(?:[A-Za-z_]\\w*\\s*\\.\\s*)?tenant_id(?![A-Za-z0-9_])",
          Pattern.CASE_INSENSITIVE);

  /** 書き換え対象の SQL。 */
  private final String sql;

  /**
   * 書き換え対象の SQL を指定して生成します.
   *
   * @param sql 書き換え対象の SQL（WHERE 句・結合・句の終わりを含んでよい）
   */
  TenantFilterSql(final String sql) {
    this.sql = sql;
  }

  /**
   * テナント条件を付けたSQLを返します.
   *
   * <p>SELECT EXISTS 文は括弧の中身を通常の SELECT として再帰的に書き換えます。括弧の外に句が続く形は、条件の位置がずれるため拒否します。
   *
   * @param tenantId テナントID（必須）
   * @return テナント条件を付けたSQL
   * @throws IllegalArgumentException テナントIDまたはSQLが無い場合、安全に書き換えられない形の場合
   */
  String withTenantFilter(final String tenantId) {
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("TenantId must not be null or empty");
    }
    if (sql == null) {
      throw new IllegalArgumentException("baseSql must not be null");
    }
    String trimmed = sql.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("SQL が空です");
    }
    if (INSERT_PREFIX.matcher(trimmed).find()) {
      throw new IllegalArgumentException("INSERT 文にはテナント条件を付けられません");
    }
    Matcher exists = EXISTS_PREFIX.matcher(trimmed);
    if (exists.find()) {
      int open = exists.end() - 1;
      int close = new SqlScan(trimmed).closeOf[open];
      if (close != trimmed.length() - 1) {
        throw new IllegalArgumentException("EXISTS 文の外側に句が続く形は書き換えできません");
      }
      String inner = trimmed.substring(open + 1, close);
      return trimmed.substring(0, open + 1)
          + new TenantFilterSql(inner).withTenantFilter(tenantId)
          + ")";
    }

    SqlScan scan = new SqlScan(trimmed);
    if (scan.hasSubquery) {
      throw new IllegalArgumentException("括弧の中の副問い合わせにはテナント条件を付けられません");
    }
    for (Keyword keyword : scan.keywords) {
      if (REJECTED.contains(keyword.word)) {
        throw new IllegalArgumentException("テナント条件を安全に付けられない句です: " + keyword.word);
      }
    }
    Keyword from = scan.first("FROM", null);
    if (from != null && UPDATE_PREFIX.matcher(trimmed).find()) {
      throw new IllegalArgumentException("UPDATE の FROM 句は書き換えできません");
    }
    int fromEnd = from == null ? 0 : from.end;
    Keyword where = scan.first("WHERE", from);
    int clauseEnd = scan.firstTail(fromEnd, trimmed.length());
    if (where != null) {
      clauseEnd = Math.min(clauseEnd, where.start);
    }
    int tailStart = where == null ? clauseEnd : scan.firstTail(where.end, trimmed.length());
    // tenant_id の手書きは、最上位の FROM・ON 句と WHERE 句だけを対象にする（GROUP BY や ORDER BY は対象外）
    if (from != null && scan.hasTopLevelTenantId(fromEnd, clauseEnd)
        || where != null && scan.hasTopLevelTenantId(where.end, tailStart)) {
      throw new IllegalArgumentException("主テーブルのテナント条件は付与されるため手書きできません");
    }
    if (UPDATE_PREFIX.matcher(trimmed).find()) {
      Keyword set = scan.first("SET", null);
      if (set == null) {
        throw new IllegalArgumentException("UPDATE 文に SET 句がありません");
      }
      if (scan.hasTopLevelTenantId(set.end, where == null ? tailStart : where.start)) {
        throw new IllegalArgumentException("UPDATE の SET 句では tenant_id を書き換えできません");
      }
    }

    Edits edits = new Edits();
    List<String> conditions = new ArrayList<>();
    String qualifier = null;
    if (from != null) {
      qualifier = new FromClause(scan, fromEnd, clauseEnd).rewrite(edits, conditions);
    }
    List<String> all = new ArrayList<>();
    all.add((qualifier == null ? "tenant_id" : qualifier + ".tenant_id") + " = ?");
    all.addAll(conditions);
    String joinedConditions = String.join(" AND ", all);

    if (where == null) {
      String insert =
          tailStart == trimmed.length()
              ? " WHERE " + joinedConditions
              : "WHERE " + joinedConditions + " ";
      edits.add(tailStart, tailStart, insert);
    } else {
      String body = trimmed.substring(where.end, tailStart).trim();
      boolean hasOr = false;
      for (Keyword keyword : scan.keywords) {
        if (keyword.word.equals("OR") && keyword.start >= where.end && keyword.start < tailStart) {
          hasOr = true;
        }
      }
      String wrapped = hasOr ? "(" + body + ")" : body;
      String trailingSpace = tailStart < trimmed.length() ? " " : "";
      edits.add(where.end, tailStart, " " + wrapped + " AND " + joinedConditions + trailingSpace);
    }
    return edits.applyTo(trimmed);
  }

  /**
   * 表名と別名の形を確かめて一致結果を返します.
   *
   * @param text 表の句の文字列
   * @return 一致結果（グループ 1 が表名、2 が別名）
   * @throws IllegalArgumentException 表名の形でない場合（副問い合わせなど）
   */
  private static Matcher matchTableRef(final String text) {
    Matcher matcher = TABLE_REF.matcher(text.trim());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("表の指定を安全に解析できません（副問い合わせなど）: " + text.trim());
    }
    return matcher;
  }

  /** SQL を走査した結果（最上位のキーワード、括弧の対応、最上位だけの見た目）を持つ値クラス. */
  static final class SqlScan {

    /** 走査した SQL。 */
    private final String text;

    /** 括弧の中と引用符の中を空白に置き換えた SQL（最上位の文字だけが残る）。 */
    private final char[] topLevel;

    /** 開き括弧の位置ごとの対応する閉じ括弧の位置（開き括弧以外は -1）。 */
    private final int[] closeOf;

    /** 最上位のキーワードを出現順に並べた一覧。 */
    private final List<Keyword> keywords = new ArrayList<>();

    /** 括弧の直後に SELECT が現れた（括弧の中に副問い合わせがある）場合は true。 */
    private boolean hasSubquery;

    /**
     * SQL を先頭から走査して、キーワードと括弧の対応を求めます.
     *
     * @param text 走査する SQL
     * @throws IllegalArgumentException 括弧や引用符が閉じていない場合
     */
    SqlScan(final String text) {
      this.text = text;
      this.topLevel = text.toCharArray();
      this.closeOf = new int[text.length()];
      Arrays.fill(closeOf, -1);
      int length = text.length();
      Deque<Integer> opens = new ArrayDeque<>();
      int depth = 0;
      int i = 0;
      while (i < length) {
        char c = text.charAt(i);
        if (c == '\'' || c == '"') {
          // 引用符の終わりを探し、中身を最上位の見た目から外す（'' はエスケープとして読み飛ばす）
          int end = i + 1;
          boolean closed = false;
          while (!closed) {
            if (end >= length) {
              throw new IllegalArgumentException("文字列リテラルまたは引用符が閉じていません");
            }
            if (text.charAt(end) != c) {
              end++;
            } else if (c == '\'' && end + 1 < length && text.charAt(end + 1) == '\'') {
              end += 2;
            } else {
              end++;
              closed = true;
            }
          }
          mask(i, end);
          i = end;
        } else if (c == '(') {
          if (isSelectAt(text, i + 1)) {
            hasSubquery = true;
          }
          mask(i, i + 1, depth);
          opens.push(i);
          depth++;
          i++;
        } else if (c == ')') {
          if (opens.isEmpty()) {
            throw new IllegalArgumentException("括弧の対応が取れていません");
          }
          closeOf[opens.pop()] = i;
          depth--;
          mask(i, i + 1, depth + 1);
          i++;
        } else if (text.startsWith("--", i)) {
          // 行コメントの後ろに付けた条件はコメントとして無視されるため、拒否する（リテラル内の -- は上の分岐で読み飛ばし済み）
          throw new IllegalArgumentException("SQL コメントを含むSQLはテナント条件を安全に付けられません");
        } else if (text.startsWith("/*", i)) {
          // ブロックコメントで副問い合わせの括弧と SELECT の間を隠せるため、拒否する
          throw new IllegalArgumentException("SQL コメントを含むSQLはテナント条件を安全に付けられません");
        } else if (c == ',' && depth == 0) {
          keywords.add(new Keyword(",", i, i + 1));
          i++;
        } else if (Character.isLetter(c) || c == '_') {
          int start = i;
          while (i < length
              && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_')) {
            i++;
          }
          mask(start, i, depth);
          if (depth == 0) {
            String word = text.substring(start, i).toUpperCase(Locale.ROOT);
            Matcher by = BY_CLAUSE.matcher(text);
            by.region(i, length);
            if ((word.equals("ORDER") || word.equals("GROUP")) && by.lookingAt()) {
              keywords.add(new Keyword(word + " BY", start, by.end()));
              i = by.end();
            } else if (KEYWORDS.contains(word) || REJECTED.contains(word)) {
              keywords.add(new Keyword(word, start, i));
            }
          }
        } else {
          mask(i, i + 1, depth);
          i++;
        }
      }
      if (!opens.isEmpty()) {
        throw new IllegalArgumentException("括弧の対応が取れていません");
      }
    }

    /**
     * 範囲を最上位の見た目から外します（括弧の中や引用符の中の文字を空白にする）.
     *
     * @param from 開始位置
     * @param to 終了位置（含まない）
     */
    void mask(final int from, final int to) {
      Arrays.fill(topLevel, from, to, ' ');
    }

    /**
     * 深さが 0 より大きい場合だけ範囲を最上位の見た目から外します.
     *
     * @param from 開始位置
     * @param to 終了位置（含まない）
     * @param depth 範囲の深さ
     */
    void mask(final int from, final int to, final int depth) {
      if (depth > 0) {
        mask(from, to);
      }
    }

    /**
     * 指定の位置から空白を読み飛ばした先が SELECT という単語かを返します.
     *
     * <p>括弧の直後に SELECT があると、その括弧は副問い合わせになる。副問い合わせの中の表には、主テーブルのテナント条件を付けられないため拒否する。
     *
     * @param text 走査する SQL
     * @param from 検査を始める位置（括弧の次の位置）
     * @return SELECT という単語であれば true
     */
    static boolean isSelectAt(final String text, final int from) {
      Matcher matcher = SELECT_AT.matcher(text);
      matcher.region(from, text.length());
      return matcher.lookingAt();
    }

    /**
     * 最上位のキーワードを、指定のキーワードの後ろから順に探します.
     *
     * @param word 探す単語（大文字）
     * @param anchor この後ろから探すキーワード（null の場合は先頭から）
     * @return 見つかったキーワード（無ければ null）
     */
    Keyword first(final String word, final Keyword anchor) {
      int from = anchor == null ? 0 : keywords.indexOf(anchor) + 1;
      for (int i = from; i < keywords.size(); i++) {
        if (keywords.get(i).word.equals(word)) {
          return keywords.get(i);
        }
      }
      return null;
    }

    /**
     * 指定の位置以降で最初に現れる句の終わりの位置を返します.
     *
     * @param from この位置以降を探す
     * @param defaultEnd 見つからない場合の位置
     * @return 句の終わりの位置、無ければ defaultEnd
     */
    int firstTail(final int from, final int defaultEnd) {
      for (Keyword keyword : keywords) {
        if (keyword.start >= from && TAIL_KEYWORDS.contains(keyword.word)) {
          return keyword.start;
        }
      }
      return defaultEnd;
    }

    /**
     * 指定の範囲に、最上位の tenant_id の列参照があるかを返します.
     *
     * @param from 検査を始める位置
     * @param to 検査を終える位置（含まない）
     * @return 最上位に tenant_id の列参照があれば true
     */
    boolean hasTopLevelTenantId(final int from, final int to) {
      return TENANT_ID_COLUMN.matcher(new String(topLevel, from, to - from)).find();
    }
  }

  /** FROM 句の結合を読み取り、結合先のテナント条件を ON 句または WHERE 句へ書き換える値クラス. */
  static final class FromClause {

    /** 走査した SQL。 */
    private final SqlScan scan;

    /** FROM キーワードの終わり。 */
    private final int fromEnd;

    /** FROM 句の終わり。 */
    private final int clauseEnd;

    /** 結合の区切りの一覧。 */
    private final List<Cut> cuts = new ArrayList<>();

    /**
     * FROM 句の範囲から結合の区切りを読み取ります.
     *
     * @param scan 走査した SQL
     * @param fromEnd FROM キーワードの終わり
     * @param clauseEnd FROM 句の終わり
     */
    FromClause(final SqlScan scan, final int fromEnd, final int clauseEnd) {
      this.scan = scan;
      this.fromEnd = fromEnd;
      this.clauseEnd = clauseEnd;
      boolean inHeader = false;
      for (Keyword keyword : scan.keywords) {
        if (keyword.start < fromEnd || keyword.start >= clauseEnd) {
          continue;
        }
        if (keyword.word.equals(",")) {
          cuts.add(new Cut(keyword.start, ",", keyword.end));
        } else if (JOIN_MODIFIERS.contains(keyword.word)) {
          cuts.add(new Cut(keyword.start, keyword.word, -1));
          inHeader = true;
        } else if (keyword.word.equals("JOIN")) {
          if (!inHeader) {
            cuts.add(new Cut(keyword.start, "INNER", keyword.end));
          } else {
            cuts.get(cuts.size() - 1).tableStart = keyword.end;
            inHeader = false;
          }
        } else if (keyword.word.equals("ON")) {
          if (cuts.isEmpty()) {
            throw new IllegalArgumentException("結合の対象が無い ON 句です");
          }
          Cut last = cuts.get(cuts.size() - 1);
          last.onKeywordStart = keyword.start;
          last.onStart = keyword.end;
        }
      }
    }

    /**
     * 結合先のテナント条件を ON 句と WHERE 句の追加条件へ書き換えます.
     *
     * @param edits 書き換えの一覧（追加する）
     * @param conditions WHERE 句に追加する条件（追加する）
     * @return 主テーブルの修飾子（修飾しない場合は null）
     * @throws IllegalArgumentException 安全に書き換えられない結合の場合
     */
    String rewrite(final Edits edits, final List<String> conditions) {
      String text = scan.text;
      Matcher main =
          matchTableRef(text.substring(fromEnd, cuts.isEmpty() ? clauseEnd : cuts.get(0).start));
      String mainAlias = main.group(2);
      String mainRef = mainAlias != null ? mainAlias : main.group(1);
      for (int i = 0; i < cuts.size(); i++) {
        Cut cut = cuts.get(i);
        int segmentEnd = i + 1 < cuts.size() ? cuts.get(i + 1).start : clauseEnd;
        int tableEnd = cut.onKeywordStart >= 0 ? cut.onKeywordStart : segmentEnd;
        if (cut.tableStart < 0) {
          throw new IllegalArgumentException("結合の句が不完全です");
        }
        Matcher table = matchTableRef(text.substring(cut.tableStart, tableEnd));
        String ref = table.group(2) != null ? table.group(2) : table.group(1);
        String tenantEquality = ref + ".tenant_id = " + mainRef + ".tenant_id";
        if (cut.onStart < 0) {
          if (!cut.kind.equals("CROSS") && !cut.kind.equals("INNER") && !cut.kind.equals(",")) {
            throw new IllegalArgumentException("ON 句の無い " + cut.kind + " 結合は書き換えできません");
          }
          conditions.add(tenantEquality);
        } else {
          if (cut.kind.equals(",")) {
            throw new IllegalArgumentException("カンマ区切りの表に ON 句があります");
          }
          String raw = text.substring(cut.onStart, segmentEnd);
          String leading = raw.substring(0, raw.length() - raw.stripLeading().length());
          String trailing = raw.substring(raw.stripTrailing().length());
          edits.add(
              cut.onStart,
              segmentEnd,
              leading + "(" + raw.strip() + ") AND " + tenantEquality + trailing);
        }
      }
      return cuts.isEmpty() ? mainAlias : mainRef;
    }
  }

  /** 結合の区切り（新しい表の始まり）。 */
  static final class Cut {

    /** 区切りの位置。 */
    private final int start;

    /** 結合の種類（"," はカンマ区切り）。 */
    private final String kind;

    /** 表名の開始位置（結合の句の JOIN の後ろ）。 */
    private int tableStart;

    /** ON キーワードの位置（無ければ -1）。 */
    private int onKeywordStart = -1;

    /** ON 条件の開始位置（無ければ -1）。 */
    private int onStart = -1;

    /**
     * 区切りを生成します.
     *
     * @param start 区切りの位置
     * @param kind 結合の種類
     * @param tableStart 表名の開始位置
     */
    Cut(final int start, final String kind, final int tableStart) {
      this.start = start;
      this.kind = kind;
      this.tableStart = tableStart;
    }
  }

  /** 最上位のキーワード（位置と語）。 */
  static final class Keyword {

    /** キーワード（大文字）。 */
    private final String word;

    /** 開始位置。 */
    private final int start;

    /** 終了位置。 */
    private final int end;

    /**
     * キーワードを生成します.
     *
     * @param word キーワード
     * @param start 開始位置
     * @param end 終了位置
     */
    Keyword(final String word, final int start, final int end) {
      this.word = word;
      this.start = start;
      this.end = end;
    }
  }

  /** 書き換えの一覧（位置の範囲と置き換える文字列）を持つ値クラス. */
  static final class Edits {

    /** 書き換えの一覧。 */
    private final List<Edit> edits = new ArrayList<>();

    /**
     * 書き換えを追加します.
     *
     * @param start 開始位置
     * @param end 終了位置
     * @param text 置き換える文字列
     */
    void add(final int start, final int end, final String text) {
      edits.add(new Edit(start, end, text));
    }

    /**
     * 書き換えを位置の後ろから適用して文字列を作ります.
     *
     * @param text 元の SQL
     * @return 書き換え後の SQL
     */
    String applyTo(final String text) {
      List<Edit> sorted = new ArrayList<>(edits);
      sorted.sort((a, b) -> Integer.compare(b.start, a.start));
      StringBuilder result = new StringBuilder(text);
      for (Edit edit : sorted) {
        result.replace(edit.start, edit.end, edit.text);
      }
      return result.toString();
    }
  }

  /** 1 件の書き換え（位置の範囲を置き換える）。 */
  static final class Edit {

    /** 開始位置。 */
    private final int start;

    /** 終了位置。 */
    private final int end;

    /** 置き換える文字列。 */
    private final String text;

    /**
     * 書き換えを生成します.
     *
     * @param start 開始位置
     * @param end 終了位置
     * @param text 置き換える文字列
     */
    Edit(final int start, final int end, final String text) {
      this.start = start;
      this.end = end;
      this.text = text;
    }
  }
}
