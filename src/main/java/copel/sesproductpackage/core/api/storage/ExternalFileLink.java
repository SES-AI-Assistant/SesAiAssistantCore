package copel.sesproductpackage.core.api.storage;

import copel.sesproductpackage.core.util.FileNameUtils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 【SES AIアシスタント】 外部ストレージ上の共有URLを1件分表すクラス.
 *
 * <p>Google Drive, Dropbox, OneDrive/SharePoint の共有リンクを検出し、 ダウンロード可能な直リンクへ変換してファイルを取得する。
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class ExternalFileLink {

  /** ファイルサイズ上限（バイト）: 20MB. */
  static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;

  /** HTTPタイムアウト（秒）. */
  static final int HTTP_TIMEOUT_SECONDS = 15;

  /** 元のURL（本文または要約文から抽出したまま）. */
  private final String rawUrl;

  /** リダイレクト解決後の最終URL. */
  private String resolvedUrl;

  /** ダウンロード可能な直リンクURL（サービス別変換後）. */
  private String directUrl;

  /** ファイル名（Content-Dispositionヘッダ or URLから推定）. */
  private String fileName;

  /** ダウンロード済みのバイト配列. */
  private byte[] fileData;

  /** HTTPクライアント（リダイレクト自動追跡）. */
  private HttpClient httpClient;

  /**
   * コンストラクタ.
   *
   * @param rawUrl 本文または要約文から抽出した元のURL
   */
  public ExternalFileLink(final String rawUrl) {
    this.rawUrl = rawUrl;
    this.httpClient =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .build();
  }

  /**
   * テスト用コンストラクタ（HttpClientを注入可能）.
   *
   * @param rawUrl 元URL
   * @param httpClient テスト用HttpClient
   */
  ExternalFileLink(final String rawUrl, final HttpClient httpClient) {
    this.rawUrl = rawUrl;
    this.httpClient = httpClient;
  }

  /**
   * サポート対象のストレージサービスへのリンクか否かを返す.
   *
   * <p>HEADリクエストでリダイレクトを解決し、最終URLのドメインで判定する。 判定に成功した場合は内部に resolvedUrl と directUrl をセットする。
   *
   * @return サポート対象であれば true
   */
  public boolean isDownloadable() {
    if (rawUrl == null || rawUrl.isBlank()) {
      return false;
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(rawUrl))
              .method("HEAD", HttpRequest.BodyPublishers.noBody())
              .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
              .build();
      HttpResponse<Void> response =
          httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      this.resolvedUrl = response.uri().toString();
      this.directUrl = convertToDirectUrl(this.resolvedUrl);
      return isSupportedDomain(this.resolvedUrl);
    } catch (Exception e) {
      log.debug("isDownloadable チェック失敗 (スキップ): {} / {}", rawUrl, e.getMessage());
      return false;
    }
  }

  /**
   * 直リンクURLからファイルをダウンロードし fileData に格納する.
   *
   * @throws IOException ダウンロード失敗時
   * @throws InterruptedException HTTPリクエスト中断時
   */
  public void download() throws IOException, InterruptedException {
    if (directUrl == null || directUrl.isBlank()) {
      throw new IOException("directUrl が未設定です。isDownloadable() を先に呼び出してください。");
    }
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(directUrl))
            .GET()
            .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .build();
    HttpResponse<byte[]> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

    if (response.statusCode() != 200) {
      throw new IOException("Download failed: HTTP " + response.statusCode() + " / " + directUrl);
    }

    // Google Drive のウイルス確認ページ対策
    String contentType = response.headers().firstValue("Content-Type").orElse("");
    if (contentType.startsWith("text/html")) {
      throw new IOException("HTMLが返されました（ウイルス確認ページまたは認証要求）。ダウンロードをスキップします: " + directUrl);
    }

    // ファイルサイズ上限チェック
    byte[] data = response.body();
    if (data != null && data.length > MAX_FILE_SIZE_BYTES) {
      throw new IOException(
          "ファイルサイズが上限(" + (MAX_FILE_SIZE_BYTES / 1024 / 1024) + "MB)を超えているためスキップします: " + directUrl);
    }

    this.fileData = data;
    this.fileName = extractFileName(response, directUrl);
    log.info("外部リンクからファイルをダウンロードしました: {} ({}bytes)", directUrl, data != null ? data.length : 0);
  }

  /**
   * 元のURLを返す.
   *
   * @return rawUrl
   */
  public String getRawUrl() {
    return rawUrl;
  }

  /**
   * ファイル名を返す.
   *
   * @return fileName
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * ダウンロード済みバイト配列を返す.
   *
   * @return fileData
   */
  public byte[] getFileData() {
    return fileData != null ? fileData.clone() : null;
  }

  /**
   * リダイレクト解決後の最終URLを返す（テスト・デバッグ用）.
   *
   * @return resolvedUrl
   */
  String getResolvedUrl() {
    return resolvedUrl;
  }

  /**
   * 直リンクURLを返す（テスト・デバッグ用）.
   *
   * @return directUrl
   */
  String getDirectUrl() {
    return directUrl;
  }

  /**
   * 閲覧用URLをダウンロード可能な直リンクURLへ変換する.
   *
   * @param url 変換対象URL
   * @return 直リンクURL（サポート対象でない場合は元URLをそのまま返す）
   */
  String convertToDirectUrl(final String url) {
    if (url == null) {
      return null;
    }
    // Google Drive 通常ファイル
    if (url.contains("drive.google.com/file/d/")) {
      String id = extractId(url, "/file/d/([^/?]+)");
      return "https://drive.google.com/uc?export=download&id=" + id;
    }
    // Google スプレッドシート
    if (url.contains("docs.google.com/spreadsheets/d/")) {
      String id = extractId(url, "/spreadsheets/d/([^/?]+)");
      return "https://docs.google.com/spreadsheets/d/" + id + "/export?format=xlsx";
    }
    // Google ドキュメント
    if (url.contains("docs.google.com/document/d/")) {
      String id = extractId(url, "/document/d/([^/?]+)");
      return "https://docs.google.com/document/d/" + id + "/export?format=docx";
    }
    // Google スライド
    if (url.contains("docs.google.com/presentation/d/")) {
      String id = extractId(url, "/presentation/d/([^/?]+)");
      return "https://docs.google.com/presentation/d/" + id + "/export?format=pdf";
    }
    // Dropbox
    if (url.contains("dropbox.com/s/") || url.contains("dropbox.com/scl/")) {
      String base = url.split("\\?")[0];
      return base + "?dl=1";
    }
    // OneDrive / SharePoint
    if (url.contains("sharepoint.com") || url.contains("1drv.ms")) {
      return url + (url.contains("?") ? "&" : "?") + "download=1";
    }
    return url;
  }

  /**
   * URLがサポート対象のストレージサービスのドメインか判定する.
   *
   * @param url 判定対象URL
   * @return サポート対象であれば true
   */
  boolean isSupportedDomain(final String url) {
    if (url == null) {
      return false;
    }
    return url.contains("drive.google.com")
        || url.contains("docs.google.com")
        || url.contains("dropbox.com")
        || url.contains("sharepoint.com")
        || url.contains("1drv.ms");
  }

  /**
   * HTTPレスポンスまたはURLからファイル名を決定する.
   *
   * <p>優先順位: 1. Content-Disposition ヘッダ / 2. URL末尾のパス部分 / 3. タイムスタンプをつけたデフォルト名
   *
   * @param response HTTPレスポンス
   * @param url ダウンロードURL
   * @return ファイル名
   */
  String extractFileName(final HttpResponse<byte[]> response, final String url) {
    // 1st: Content-Disposition ヘッダから取得
    String disposition = response.headers().firstValue("Content-Disposition").orElse("");
    if (!disposition.isBlank()) {
      // filename*= (RFC 5987) を優先
      Pattern rfc5987 =
          Pattern.compile("filename\\*=(?:UTF-8'')?([^;\\s]+)", Pattern.CASE_INSENSITIVE);
      Matcher m = rfc5987.matcher(disposition);
      if (m.find()) {
        String name = m.group(1).trim();
        if (!name.isBlank()) {
          return FileNameUtils.decode(name);
        }
      }
      // filename= (通常)
      Pattern plain = Pattern.compile("filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);
      Matcher m2 = plain.matcher(disposition);
      if (m2.find()) {
        String name = m2.group(1).trim();
        if (!name.isBlank()) {
          return FileNameUtils.decode(name);
        }
      }
    }
    // 2nd: URL末尾のパス部分
    try {
      String path = URI.create(url).getPath();
      String name = path.substring(path.lastIndexOf('/') + 1);
      if (!name.isBlank()) {
        return FileNameUtils.decode(name);
      }
    } catch (Exception e) {
      log.debug("URLからのファイル名抽出に失敗しました: {}", e.getMessage());
    }
    // 3rd: タイムスタンプをつけたデフォルト名
    return "skillsheet_" + System.currentTimeMillis();
  }

  /**
   * URLから正規表現でIDを抽出する.
   *
   * @param url 対象URL
   * @param regex 抽出正規表現（グループ1がID部分）
   * @return 抽出したID（見つからない場合は空文字）
   */
  String extractId(final String url, final String regex) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(url);
    return matcher.find() ? matcher.group(1) : "";
  }
}
