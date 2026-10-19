package copel.sesproductpackage.tools.utspec;

/**
 * UT 仕様の違反を1件表す値オブジェクト。
 *
 * <p>違反はファイルと行番号を持つため、ログを見た開発者がそのまま修正箇所を開けるようにする。
 *
 * @param file 違反があるファイルのリポジトリ直下からの相対パス
 * @param line 違反がある行番号。ファイル単位の違反は 0
 * @param rule 違反したルール
 * @param message 違反の内容（何が問題か）
 * @author Copel Co., Ltd.
 */
public record Violation(String file, int line, UtRule rule, String message) {

  /**
   * ログに出す形式（ファイル:行 [ルール番号] 内容）の文字列を返す。
   *
   * <p>行番号が無い違反はファイル名だけを出す。行番号の形式を1か所にまとめ、ログの見た目を揃える。
   *
   * @return ログに出す1行の文字列
   */
  public String format() {
    String location = line > 0 ? file + ":" + line : file;
    return location + " [" + rule.code() + "] " + message;
  }
}
