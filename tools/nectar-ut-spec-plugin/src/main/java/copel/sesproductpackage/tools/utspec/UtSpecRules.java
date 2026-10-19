package copel.sesproductpackage.tools.utspec;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * UT 仕様の検査で使う語彙・文字数・判定の定義を1か所にまとめたクラス。
 *
 * <p>禁止語、括弧、装飾記号、期待結果の語尾、第2階層の種類、枕詞、文字数上限をここでだけ定義する。 後から仕様を調整するときは、このクラスの定数を変更すれば検査全体に反映される。
 *
 * @author Copel Co., Ltd.
 */
public final class UtSpecRules {

  /** 1文の文字数の上限（文末の「。」を含む）。運用しながら調整する目安の値。 */
  public static final int MAX_SENTENCE_LENGTH = 100;

  /** JavaDoc の本文1文目の文末。何を確かめるテストかを「ことを確かめる。」で言い切らせるため。 */
  public static final String REQUIRED_SENTENCE_END = "ことを確かめる。";

  /** 第2階層に置いてよい種類の名前。この順番で書く。 */
  public static final List<String> KIND_NAMES = List.of("正常系", "異常系", "境界値", "認可", "テナント隔離");

  /** テスト名と JavaDoc で使ってはいけない禁止語。 */
  public static final List<String> FORBIDDEN_WORDS =
      List.of("正しく", "正しい", "正常に", "適切に", "など", "うまく");

  /** 使ってはいけない括弧の文字。リンクのタグとコードのタグの中は検査対象から外す。 */
  public static final String BRACKET_CHARS = "（）()【】［］[]「」『』";

  /** 使ってはいけない装飾記号。絵文字の範囲は {@link #isEmojiOrSymbol(int)} で判定する。 */
  public static final String DECORATION_CHARS = "※→←↑↓⇒★☆●○◎◆◇■□▲△▼▽✓✔✕✖♪♥";

  /**
   * 期待結果の許可された語尾。テスト名の期待結果部分がいずれかに完全一致する場合だけ許可する。
   *
   * <p>「〜例外を投げる」は「XxxExceptionを投げる」の表記も許可する。
   */
  public static final List<String> EXPECTED_RESULT_PATTERNS =
      List.of(
          ".+を返す",
          ".+例外を投げる",
          "[A-Za-z][A-Za-z0-9]*Exceptionを投げる",
          ".+を登録する",
          ".+を更新する",
          ".+を削除する",
          ".+を送信する",
          ".+を呼ばない",
          "何もしない");

  /** JavaDoc の本文2文目の先頭に置いてはいけない枕詞。理由を言い切りで書かせるため。 */
  public static final List<String> PROLOGUE_WORDS = List.of("なぜ", "理由", "目的", "背景", "意図");

  /** 本文2文目の文末に置いてはいけない理由の言い方。理由は言い切りで書かせるため。 */
  public static final String PROHIBITED_REASON_ENDING = "ため。";

  /** 検査対象のテストアノテーションのうち、テストとして扱うもの。 */
  public static final List<String> SUPPORTED_TEST_ANNOTATIONS =
      List.of("Test", "ParameterizedTest");

  /** 仕様で使わないテストアノテーション。第2階層に置くと違反になる。 */
  public static final List<String> UNSUPPORTED_TEST_ANNOTATIONS =
      List.of("RepeatedTest", "TestFactory", "TestTemplate");

  /** 文の区切りとして使う文末の記号。 */
  private static final String SENTENCE_END = "。";

  /** 括弧を検出するときに除外する inline タグ（リンクのタグとコードのタグ）。 */
  private static final Pattern INLINE_TAG = Pattern.compile("\\{@(link|code)\\s[^}]*\\}");

  /** 期待結果の語尾の判定に使う正規表現。 */
  private final List<Pattern> expectedResultPatterns;

  /** 既定のルールで生成する。 */
  public UtSpecRules() {
    this.expectedResultPatterns = new ArrayList<>();
    for (String pattern : EXPECTED_RESULT_PATTERNS) {
      expectedResultPatterns.add(Pattern.compile(pattern));
    }
  }

  /**
   * テキストに含まれる禁止語を、定義の順に返す。
   *
   * @param text 検査するテキスト
   * @return 含まれていた禁止語の一覧。無ければ空のリスト
   */
  public List<String> findForbiddenWords(String text) {
    List<String> found = new ArrayList<>();
    for (String word : FORBIDDEN_WORDS) {
      if (text.contains(word)) {
        found.add(word);
      }
    }
    return found;
  }

  /**
   * テキストに含まれる括弧の文字を、重複を除いて返す。
   *
   * <p>{@code {@link}} と {@code {@code}} の中は検査対象から外す。ここでは渡された文字列をそのまま調べる。
   *
   * @param text 検査するテキスト（inline タグは呼び出し側で除いておく）
   * @return 含まれていた括弧の一覧。無ければ空のリスト
   */
  public List<String> findBrackets(String text) {
    return findCharsIn(text, BRACKET_CHARS, false);
  }

  /**
   * テキストに含まれる装飾記号と絵文字を、重複を除いて返す。
   *
   * @param text 検査するテキスト
   * @return 含まれていた装飾記号と絵文字の一覧。無ければ空のリスト
   */
  public List<String> findDecorations(String text) {
    return findCharsIn(text, DECORATION_CHARS, true);
  }

  /**
   * コードポイントが絵文字や記号の範囲に含まれるかを判定する。
   *
   * <p>矢印、幾何学図形、その他の記号、絵文字の範囲と、Unicode 上の「その他の記号（So）」を対象にする。 日本語の句読点（、。・など）は対象に含めない。
   *
   * @param codePoint 判定するコードポイント
   * @return 絵文字または記号なら true
   */
  public boolean isEmojiOrSymbol(int codePoint) {
    // 範囲の判定は「かつ」を「または」より先に評価する（括弧なしで範囲ごとに並べる）
    boolean inRange =
        codePoint >= 0x2190 && codePoint <= 0x21FF
            || codePoint >= 0x2600 && codePoint <= 0x27BF
            || codePoint >= 0x2B00 && codePoint <= 0x2BFF
            || codePoint >= 0x1F000 && codePoint <= 0x1FAFF
            || codePoint == 0xFE0F
            || codePoint == 0x200D;
    return inRange || Character.getType(codePoint) == Character.OTHER_SYMBOL;
  }

  /**
   * 期待結果の部分が許可された語尾の一覧のどれかに一致するかを判定する。
   *
   * @param expected テスト名の「_」より後ろの期待結果の部分
   * @return 許可された語尾に一致すれば true
   */
  public boolean isAllowedExpectedResult(String expected) {
    for (Pattern pattern : expectedResultPatterns) {
      if (pattern.matcher(expected).matches()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 第2階層の種類の名前が何番目かを返す。規定の順番の判定に使う。
   *
   * @param kind 第2階層のクラス名
   * @return 0 から始まる順番。規定外の名前なら -1
   */
  public int kindIndex(String kind) {
    return KIND_NAMES.indexOf(kind);
  }

  /**
   * 文の末尾が理由の禁止された言い方（「ため。」）で終わるかを判定する。理由は言い切りで書かせるため。
   *
   * @param sentence 判定する文
   * @return 禁止された言い方で終われば true
   */
  public boolean endsWithProhibitedReason(String sentence) {
    return sentence.endsWith(PROHIBITED_REASON_ENDING);
  }

  /**
   * 文の先頭が枕詞（なぜ・理由・目的など）で始まるかを判定する。
   *
   * @param sentence 判定する文
   * @return 枕詞で始まれば true
   */
  public boolean startsWithProhibitedPrefix(String sentence) {
    for (String word : PROLOGUE_WORDS) {
      if (sentence.startsWith(word)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 文の文字数が上限を超えているかを判定する。文字数はコードポイント数で数える。
   *
   * @param sentence 判定する文（文末の「。」を含む）
   * @return 上限を超えていれば true
   */
  public boolean isOverSentenceLimit(String sentence) {
    return sentence.codePointCount(0, sentence.length()) > MAX_SENTENCE_LENGTH;
  }

  /**
   * アノテーション名がテストとして扱うもの（対応済みまたは未対応）かを判定する。
   *
   * @param annotationName アノテーションの単純名
   * @return テストのアノテーションなら true
   */
  public boolean isTestAnnotation(String annotationName) {
    return SUPPORTED_TEST_ANNOTATIONS.contains(annotationName)
        || UNSUPPORTED_TEST_ANNOTATIONS.contains(annotationName);
  }

  /**
   * 文の末尾が文末の記号で終わっているかを判定する。
   *
   * @param sentence 判定する文
   * @return 「。」で終わっていれば true
   */
  public boolean endsWithSentenceEnd(String sentence) {
    return sentence.endsWith(SENTENCE_END);
  }

  /**
   * inline タグ（{@code {@link}} と {@code {@code}}）を取り除いたテキストを返す。
   *
   * @param text 元のテキスト
   * @return inline タグを除いたテキスト
   */
  public String removeInlineTags(String text) {
    return INLINE_TAG.matcher(text).replaceAll("");
  }

  /**
   * 指定した文字の集合に含まれる文字を、出現順・重複なしで返す。絵文字の判定を含める場合はそれも対象にする。
   *
   * @param text 検査するテキスト
   * @param chars 対象の文字の集合
   * @param includeEmoji 絵文字と記号の範囲も対象にするなら true
   * @return 見つかった文字の一覧
   */
  public List<String> findCharsIn(String text, String chars, boolean includeEmoji) {
    List<String> found = new ArrayList<>();
    text.codePoints()
        .forEach(
            codePoint -> {
              boolean matched =
                  chars.indexOf(codePoint) >= 0 || includeEmoji && isEmojiOrSymbol(codePoint);
              String symbol = new String(Character.toChars(codePoint));
              if (matched && !found.contains(symbol)) {
                found.add(symbol);
              }
            });
    return found;
  }
}
