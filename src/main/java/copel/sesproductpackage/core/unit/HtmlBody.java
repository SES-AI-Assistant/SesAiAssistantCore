package copel.sesproductpackage.core.unit;

import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * HTML の本文（Content-Type が text/html のメール本文）を表す値オブジェクト.
 *
 * <p>HTML メールの原文には、style の中身、img の alt、a の href などタグ以外の文字列が多く含まれる。
 * そのまま保存すると原文が読めなくなるため、表示される文字だけを取り出して {@link #toPlainText()} で返す。 text/plain
 * の本文はタグを除くだけの処理で済むため、この値オブジェクトを通さない。
 *
 * <p>HTML の解析には Jsoup を使う。タグの閉じ忘れや実体参照の解釈を、正規表現で自前に書くと取りこぼすためである。
 *
 * @author Copel Co., Ltd.
 */
public final class HtmlBody {

  /**
   * 終わりで改行を入れる要素の名前。ブロック要素の区切りを文字の上で残すため、br も含める。
   *
   * <p>br は空要素のため、終わりの処理で1回だけ改行を入れる。
   */
  private static final Set<String> LINE_BREAK_TAGS =
      Set.of("br", "p", "div", "li", "tr", "h1", "h2", "h3", "h4", "h5", "h6");

  /** 本文として表示されない要素を指定する CSS セレクタ。head には title が含まれ、style と script は中身がタグの装飾や実行コードであるため除く。 */
  private static final String REMOVED_TAGS_SELECTOR = "head, title, style, script";

  /** HTML の本文。null は「本文なし」を表す. */
  private final String html;

  /**
   * コンストラクタ. 生成は {@link #of(String)} で行う.
   *
   * @param html HTML の本文（null 可）
   */
  private HtmlBody(final String html) {
    this.html = html;
  }

  /**
   * HTML の本文の値オブジェクトを作る.
   *
   * @param html HTML の本文（null 可）
   * @return HTML の本文の値オブジェクト
   */
  public static HtmlBody of(final String html) {
    return new HtmlBody(html);
  }

  /**
   * HTML から表示される文字だけを取り出して返す.
   *
   * <p>次の順で整形する。
   *
   * <ol>
   *   <li>head・title・style・script を取り除く。中身は表示されない文字である。コメントは文字として扱わないため、走査で読まない。
   *   <li>文字の断片の中の半角空白・タブ・改行を半角空白1つにまとめる。HTML のソースの字下げを改行として扱わないためである。
   *   <li>実体参照をデコードし、nbsp は半角空白にする。Jsoup が文字の断片を返す際に実体参照を解釈する。
   *   <li>br・p・div・li・tr・h1〜h6 の終わりで改行を入れる。ブロック要素の区切りを文字の上で残すため。
   *   <li>各行の行頭と行末の空白を除く。
   *   <li>3行以上続く改行を空行1つにまとめ、先頭と末尾の改行を除く。
   * </ol>
   *
   * <p>a の href と img の alt は属性のため表示されず、表示文字だけが残る。null の場合は空文字を返す。
   *
   * @return 表示される文字だけを取り出した本文。本文が null の場合は空文字
   */
  public String toPlainText() {
    if (this.html == null) {
      return "";
    }
    Document document = Jsoup.parse(this.html);
    document.select(REMOVED_TAGS_SELECTOR).remove();

    StringBuilder builder = new StringBuilder();
    NodeTraversor.traverse(
        new NodeVisitor() {
          @Override
          public void head(final Node node, final int depth) {
            if (node instanceof TextNode) {
              // HTML の空白は1つにまとめる。nbsp（U+00A0）は半角空白に変換する。
              // nbsp は通常の空白と見た目で区別できず、整形や編集で通常の空白に化けると変換が黙って壊れるため、ソースには直書きせず文字コード 0x00A0 で書く。
              String text = ((TextNode) node).getWholeText();
              builder.append(text.replaceAll("[ \\t\\r\\n\\f]+", " ").replace((char) 0x00A0, ' '));
            }
          }

          @Override
          public void tail(final Node node, final int depth) {
            if (node instanceof Element
                && LINE_BREAK_TAGS.contains(((Element) node).normalName())) {
              builder.append('\n');
            }
          }
        },
        document.body());

    // 行ごとに前後の空白を除いてから連続する改行をまとめる。空白だけの行が空行として残るようにするため、先に行を整える。
    String lines =
        new MultiLineText(builder.toString())
            .toLines().stream().map(String::strip).collect(Collectors.joining("\n"));
    return lines.replaceAll("\n{3,}", "\n\n").strip();
  }
}
