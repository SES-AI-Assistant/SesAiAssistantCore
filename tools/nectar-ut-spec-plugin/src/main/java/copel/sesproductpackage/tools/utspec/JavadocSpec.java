package copel.sesproductpackage.tools.utspec;

import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * テストメソッドの JavaDoc を、検査と仕様書のために分解した値オブジェクト。
 *
 * <p>本文は「。」で区切って文の一覧にする。タグは名前ごとに内容の一覧にする。 空白は1つの空白に揃えるため、JavaDoc の改行の位置に影響されない。
 *
 * @param sentences 本文の文の一覧（各文は文末の「。」を含む）
 * @param tags タグ名から内容の一覧への対応（定義の順番を保つ）
 * @author Copel Co., Ltd.
 */
public record JavadocSpec(List<String> sentences, Map<String, List<String>> tags) {

  /** google-java-format が空行の後の段落に挿入する段落タグ。文分割の前に取り除く。 */
  private static final Pattern PARAGRAPH_OR_LINE_BREAK = Pattern.compile("(?i)<p>|<br\\s*/?>");

  /** 文の一覧とタグの対応を防御的にコピーして保持する。解析結果を後から変えないようにするため。 */
  public JavadocSpec {
    sentences = List.copyOf(sentences);
    tags = Map.copyOf(tags);
  }

  /**
   * JavaParser の JavaDoc を分解して生成する。
   *
   * @param javadoc 解析済みの JavaDoc
   * @return 分解した JavaDoc
   */
  public static JavadocSpec from(Javadoc javadoc) {
    String description = normalize(javadoc.getDescription().toText());
    List<String> sentences = new ArrayList<>();
    for (String piece : description.split("(?<=。)")) {
      String sentence = piece.trim();
      if (!sentence.isEmpty()) {
        sentences.add(sentence);
      }
    }
    Map<String, List<String>> tags = new LinkedHashMap<>();
    for (JavadocBlockTag tag : javadoc.getBlockTags()) {
      tags.computeIfAbsent(tag.getTagName(), key -> new ArrayList<>())
          .add(normalize(tag.getContent().toText()));
    }
    return new JavadocSpec(sentences, tags);
  }

  /**
   * JavaDoc が無い場合の空の値を返す。検査の結果は違反として別に報告する。
   *
   * @return 本文もタグも無い JavaDoc
   */
  public static JavadocSpec empty() {
    return new JavadocSpec(List.of(), Map.of());
  }

  /**
   * 文字列の改行と連続する空白を1つの空白にまとめ、前後の空白を除く。
   *
   * @param text 元の文字列
   * @return 正規化した文字列
   */
  public static String normalize(String text) {
    return PARAGRAPH_OR_LINE_BREAK
        .matcher(text)
        .replaceAll(" ")
        .replaceAll("[\\s\u3000]+", " ")
        .trim();
  }

  /**
   * 本文1文目（何を確かめるか）を返す。本文が空なら空文字。
   *
   * @return 本文1文目
   */
  public String what() {
    return sentences.isEmpty() ? "" : sentences.get(0);
  }

  /**
   * 本文2文目（理由）を返す。本文が2文に満たない場合は空文字。
   *
   * @return 本文2文目
   */
  public String why() {
    return sentences.size() < 2 ? "" : sentences.get(1);
  }

  /**
   * 指定したタグの最初の内容を返す。
   *
   * @param name タグ名（given / when / then など）
   * @return タグの内容。無ければ空文字
   */
  public String tag(String name) {
    List<String> values = tags.get(name);
    return values == null || values.isEmpty() ? "" : values.get(0);
  }

  /**
   * 指定したタグの個数を返す。
   *
   * @param name タグ名
   * @return タグの個数
   */
  public int tagCount(String name) {
    List<String> values = tags.get(name);
    return values == null ? 0 : values.size();
  }

  /**
   * 本文とすべてのタグの内容を1つの文字列にまとめる。禁止語や括弧の検査に使う。
   *
   * @return 本文とタグの内容を空白で連結した文字列
   */
  public String allText() {
    StringBuilder builder = new StringBuilder(String.join(" ", sentences));
    for (List<String> values : tags.values()) {
      for (String value : values) {
        builder.append(' ').append(value);
      }
    }
    return builder.toString();
  }
}
