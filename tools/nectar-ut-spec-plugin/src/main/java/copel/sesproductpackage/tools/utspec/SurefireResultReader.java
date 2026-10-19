package copel.sesproductpackage.tools.utspec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * surefire のレポート（TEST-*.xml）を読み込み、{@link SurefireResults} にする。
 *
 * <p>DOCTYPE を禁止して外部実体を読ませないため、XML の解析は安全な設定で行う。
 *
 * @author Copel Co., Ltd.
 */
public class SurefireResultReader {

  /** レポートのファイル名の接頭辞。 */
  private static final String REPORT_PREFIX = "TEST-";

  /** レポートのファイル名の接尾辞。 */
  private static final String REPORT_SUFFIX = ".xml";

  /** 外部の DTD を読み込まないための XML の機能。 */
  private static final String DISALLOW_DOCTYPE =
      "http://apache.org/xml/features/disallow-doctype-decl";

  /**
   * ディレクトリ内の TEST-*.xml をすべて読み込む。ディレクトリが無い場合は空の結果を返す。
   *
   * @param dir surefire のレポートのディレクトリ
   * @return 実行結果
   * @throws IOException ファイルを読み込めない場合
   */
  public SurefireResults read(Path dir) throws IOException {
    Map<String, List<SurefireResults.Outcome>> outcomes = new HashMap<>();
    if (!Files.isDirectory(dir)) {
      return new SurefireResults(outcomes);
    }
    List<Path> reports;
    try (Stream<Path> paths = Files.list(dir)) {
      reports = paths.filter(path -> isReport(path)).sorted().toList();
    }
    for (Path report : reports) {
      collect(report, outcomes);
    }
    return new SurefireResults(outcomes);
  }

  /**
   * ファイル名が surefire のレポートの形式かを判定する。
   *
   * @param path 対象のパス
   * @return TEST-*.xml なら true
   */
  public boolean isReport(Path path) {
    String fileName = Optional.ofNullable(path.getFileName()).map(Path::toString).orElse("");
    return fileName.startsWith(REPORT_PREFIX) && fileName.endsWith(REPORT_SUFFIX);
  }

  /**
   * 1つのレポートの testcase を読み、クラス名ごとの結果に追加する。
   *
   * @param report レポートのパス
   * @param outcomes 追加先（クラス名から結果の一覧への対応）
   * @throws IOException レポートを読めない場合
   */
  public void collect(Path report, Map<String, List<SurefireResults.Outcome>> outcomes)
      throws IOException {
    NodeList testcases = parse(report).getElementsByTagName("testcase");
    for (int index = 0; index < testcases.getLength(); index++) {
      Element testcase = (Element) testcases.item(index);
      String className = testcase.getAttribute("classname");
      SurefireResults.Outcome outcome =
          new SurefireResults.Outcome(testcase.getAttribute("name"), statusOf(testcase));
      outcomes.computeIfAbsent(className, key -> new ArrayList<>()).add(outcome);
    }
  }

  /**
   * テストケースの要素から実行結果を求める。failure と error は失敗、skipped はスキップ、それ以外は成功。
   *
   * @param testcase testcase 要素
   * @return 実行結果
   */
  public TestStatus statusOf(Element testcase) {
    if (hasChild(testcase, "failure") || hasChild(testcase, "error")) {
      return TestStatus.FAILED;
    }
    if (hasChild(testcase, "skipped")) {
      return TestStatus.SKIPPED;
    }
    return TestStatus.PASSED;
  }

  /**
   * 要素が指定した名前の直下の子を持つかを判定する。
   *
   * @param element 親の要素
   * @param tagName 子の要素名
   * @return 持つ場合は true
   */
  public boolean hasChild(Element element, String tagName) {
    NodeList children = element.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
      Node child = children.item(index);
      if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * レポートを安全な設定で XML として解析する。
   *
   * @param report レポートのパス
   * @return ドキュメント
   * @throws IOException 解析できない場合
   */
  public org.w3c.dom.Document parse(Path report) throws IOException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(DISALLOW_DOCTYPE, true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(report.toFile());
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException("surefire のレポートを解析できない: " + report, e);
    }
  }
}
