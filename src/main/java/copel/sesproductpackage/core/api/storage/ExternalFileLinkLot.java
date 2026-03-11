package copel.sesproductpackage.core.api.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 【SES AIアシスタント】 テキストから抽出した ExternalFileLink のリストを管理するLotクラス.
 *
 * <p>
 * テキスト（Geminiが生成した要約文など）から外部ストレージのURLを正規表現で抽出し、 一括ダウンロード処理を提供する。
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class ExternalFileLinkLot {

  /** URLを抽出する正規表現パターン. */
  static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w/:%#\\$&\\?\\(\\)~\\.=\\+\\-@!,;]+");

  /** 抽出されたURLのリスト. */
  private final List<ExternalFileLink> links;

  /**
   * テキストからURLを正規表現で抽出してリストを生成するコンストラクタ.
   *
   * @param text URLを含む可能性のあるテキスト（nullまたは空文字の場合は空リストを生成）
   */
  public ExternalFileLinkLot(final String text) {
    this.links = new ArrayList<>();
    if (text == null || text.isBlank()) {
      return;
    }
    Matcher matcher = URL_PATTERN.matcher(text);
    while (matcher.find()) {
      links.add(new ExternalFileLink(matcher.group()));
    }
  }

  /**
   * ダウンロード可能なリンクが1件以上あるかを返す.
   *
   * <p>
   * 内部でisDownloadable()を呼び出すため、HEADリクエストが発生する。
   *
   * @return ダウンロード可能なリンクが1件以上あればtrue
   */
  public boolean hasDownloadableLinks() {
    return links.stream().anyMatch(ExternalFileLink::isDownloadable);
  }

  /**
   * 全リンクのダウンロードを試みる.
   *
   * <p>
   * 失敗したリンクはwarningログのみ出力してスキップし、処理を継続する。
   */
  public void downloadAll() {
    for (ExternalFileLink link : links) {
      if (link.isDownloadable()) {
        try {
          link.download();
        } catch (Exception e) {
          log.warn("外部リンクからのファイルDLに失敗しました（スキップ）: {} / {}", link.getRawUrl(), e.getMessage());
        }
      }
    }
  }

  /**
   * ダウンロード成功（fileDataが非null）のリンクのみを返す.
   *
   * @return ダウンロード済みリンクのリスト
   */
  public List<ExternalFileLink> getDownloadedLinks() {
    return links.stream().filter(link -> link.getFileData() != null).collect(Collectors.toList());
  }

  /**
   * 全リンク数を返す（テスト・デバッグ用）.
   *
   * @return 抽出されたURL総数
   */
  public int size() {
    return links.size();
  }
}
