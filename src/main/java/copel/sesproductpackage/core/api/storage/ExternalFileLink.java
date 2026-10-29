package copel.sesproductpackage.core.api.storage;

import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.core.util.FileNameUtils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 外部ストレージ上の共有URLを1件分表すクラス.
 *
 * <p>Google Drive, Dropbox, OneDrive/SharePoint の共有リンクを検出し、 ダウンロード可能な直リンクへ変換してファイルを取得する。
 *
 * <p>通信は {@link #download()} で初めて行う。判定の {@link #isDownloadable()} は文字列の検査だけで済ませ、利用者が書いた URL へ接続しない。
 * 対応ドメインの一覧は利用者が書いた最初の URL にだけかけ、転送先は https と公開アドレスだけを確かめる。 転送は自動で追わず、転送先ごとに {@link PublicHttpsUri}
 * で取得してよい先かを確かめてから接続する（SSRF の防止）。
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class ExternalFileLink {

  /** ファイルサイズ上限（バイト）: 20MB. */
  static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;

  /** 対応するストレージサービスのドメイン（ホスト名の完全一致またはサブドメイン）. */
  static final List<String> SUPPORTED_DOMAINS =
      List.of("drive.google.com", "docs.google.com", "dropbox.com", "sharepoint.com", "1drv.ms");

  /** HTTP接続のタイムアウト（秒）. 1回の要求に使う上限であり、全体の上限は TOTAL_TIMEOUT_SECONDS で別に決める。 */
  static final int HTTP_TIMEOUT_SECONDS = 15;

  /** 転送を含めた全体の上限（秒）. Backend は API Gateway の 29 秒の枠内で動くため、それより手前の 25 秒で打ち切る。 */
  static final int TOTAL_TIMEOUT_SECONDS = 25;

  /** 転送を手動で追う上限の回数. 転送が繰り返されて止まらない応答から、接続を際限なく続けないために設ける。 */
  static final int MAX_REDIRECTS = 5;

  /** 転送として扱うステータスコード（301, 302, 303, 307, 308）。Location で次の URL を受け取る。 */
  static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 303, 307, 308);

  /** 正常に取得できたことを表すステータスコード. */
  private static final int HTTP_OK = 200;

  /** 1回の要求に使う上限のタイムアウト. HTTP_TIMEOUT_SECONDS と同じ値を Duration にしたもの。 */
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(HTTP_TIMEOUT_SECONDS);

  /** 元のURL（本文または要約文から抽出したまま）. */
  private final String rawUrl;

  /** 転送を解決した最終URL（転送の度に転送先の URL で更新する）. 転送前は rawUrl と同じ。 */
  private String resolvedUrl;

  /** 実際に GET する直リンクURL（サービス別変換後）. 転送の度に転送先を変換した URL で更新する。 */
  private String directUrl;

  /** ファイル名（Content-Dispositionヘッダ or URLから推定）. */
  private String fileName;

  /** ダウンロード済みのバイト配列. */
  private byte[] fileData;

  /**
   * HTTPクライアント. 転送を自動で追わない設定にし、転送の度に検査してから手動で追う。
   *
   * <p>転送を自動で追うと、検査していない先へ接続されるため、自動の転送は無効にする。
   */
  private HttpClient httpClient;

  /**
   * コンストラクタ.
   *
   * <p>転送を自動で追う設定にすると、対応ドメインのサーバーが内部アドレスへ転送しても検査されない（SSRF）。そのため転送は自動で追わない {@link
   * HttpClient.Redirect#NEVER} にする。
   *
   * @param rawUrl 本文または要約文から抽出した元のURL
   */
  public ExternalFileLink(final String rawUrl) {
    this.rawUrl = rawUrl;
    this.httpClient =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
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
   * ダウンロード対象のストレージサービスへのリンクか否かを返す. 通信は行わない.
   *
   * <p>接続の前に判定を終えるのは、利用者が書いた URL へ判定だけのために接続させないためである。 対象と判定した場合は、resolvedUrl に元の URL、directUrl
   * に直リンクを保持する。
   *
   * @return https で、対応ドメインのURLであれば true
   */
  public boolean isDownloadable() {
    if (rawUrl == null || rawUrl.isBlank()) {
      return false;
    }
    if (!new PublicHttpsUri(rawUrl).isHttpsWithHost() || !isSupportedDomain(rawUrl)) {
      return false;
    }
    this.resolvedUrl = rawUrl;
    this.directUrl = convertToDirectUrl(rawUrl);
    return true;
  }

  /**
   * 直リンクURLからファイルをダウンロードし fileData に格納する.
   *
   * <p>転送は最大 {@link #MAX_REDIRECTS} 回まで手動で追う。対応ドメインの一覧は利用者が書いた最初の URL だけに使い、転送先は https と公開アドレスだけを
   * {@link PublicHttpsUri} で確かめる。 転送先は Google Drive の drive.usercontent.google.com
   * など一覧に無い正しいホストへ向くため、一覧で拒否しない。 転送先の閲覧用URL（Google Drive など）は、次の要求の前に直リンクへ変換する。 全体の時間は {@link
   * #TOTAL_TIMEOUT_SECONDS} で打ち切り、Backend の API Gateway の枠を超えないようにする。
   *
   * @throws IOException ダウンロード失敗時、転送先が取得してよい先でない時、全体の時間を超えた時
   * @throws InterruptedException HTTPリクエスト中断時
   */
  public void download() throws IOException, InterruptedException {
    if (directUrl == null) {
      throw new IOException("directUrl が未設定です。isDownloadable() を先に呼び出してください。");
    }
    // 全体の残り時間は時計から測る。時計を経由すると単体テストで時間の経過を再現できる
    Instant start = ApplicationClock.INSTANCE.clock().instant();
    int redirectCount = 0;
    while (true) {
      // 転送先を含む全ての要求の前に検査する。検査の後で接続するため、内部アドレスへの接続を先に行わない
      // 名前解決（DNS）の時間も全体の上限に含めるため、残り時間は検査の後で測る
      checkPublicHttps(directUrl);
      Duration remaining =
          Duration.ofSeconds(TOTAL_TIMEOUT_SECONDS)
              .minus(Duration.between(start, ApplicationClock.INSTANCE.clock().instant()));
      if (remaining.compareTo(Duration.ZERO) <= 0) {
        throw new IOException(
            "全体のタイムアウト(" + TOTAL_TIMEOUT_SECONDS + "秒)を超えたためスキップします: " + directUrl);
      }
      // 1回の要求の時間は、15秒と全体の残り時間の短い方にする。残りが少ない時に 15 秒待たせないため
      Duration requestTimeout =
          remaining.compareTo(REQUEST_TIMEOUT) < 0 ? remaining : REQUEST_TIMEOUT;
      HttpRequest request =
          HttpRequest.newBuilder().uri(URI.create(directUrl)).GET().timeout(requestTimeout).build();
      // 200 以外の応答は本文を読み捨てる。転送や失敗の本文を受け取ると、上限の検査を通らずにメモリを使うため
      HttpResponse<byte[]> response =
          httpClient.send(
              request,
              info ->
                  info.statusCode() == HTTP_OK
                      ? HttpResponse.BodySubscribers.ofByteArray()
                      : HttpResponse.BodySubscribers.replacing(new byte[0]));
      int status = response.statusCode();

      if (REDIRECT_STATUSES.contains(status)) {
        if (redirectCount >= MAX_REDIRECTS) {
          throw new IOException("転送が上限(" + MAX_REDIRECTS + "回)を超えたためスキップします: " + directUrl);
        }
        String location =
            response
                .headers()
                .firstValue("Location")
                .orElseThrow(() -> new IOException("転送先(Location)が無い応答のためスキップします: " + directUrl));
        // 空の転送先は解決しても元の URL と同じになり、転送を追う意味がないため止める
        if (location.isBlank()) {
          throw new IOException("転送先(Location)が空のためスキップします: " + directUrl);
        }
        redirectCount++;
        try {
          // 転送先が相対参照の場合も考えて、今の直リンクを基準に解決する
          resolvedUrl = URI.create(directUrl).resolve(location).toString();
        } catch (IllegalArgumentException e) {
          throw new IOException("転送先のURLが不正なためスキップします: " + location, e);
        }
        // 変換の前の転送先も検査する。変換は http を https へ書き換えることがあり、変換後の URL だけでは http の転送先を見逃すため
        checkPublicHttps(resolvedUrl);
        // 転送先の閲覧用URLを直リンクへ変換する。変換は冪等なので、再度通る時にも結果は変わらない
        directUrl = convertToDirectUrl(resolvedUrl);
      } else {
        if (status != HTTP_OK) {
          throw new IOException("Download failed: HTTP " + status + " / " + directUrl);
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
              "ファイルサイズが上限("
                  + (MAX_FILE_SIZE_BYTES / 1024 / 1024)
                  + "MB)を超えているためスキップします: "
                  + directUrl);
        }

        this.fileData = data;
        this.fileName = extractFileName(response, directUrl);
        log.info("外部リンクからファイルをダウンロードしました: {} ({}bytes)", directUrl, data != null ? data.length : 0);
        return;
      }
    }
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
   * 転送を解決した最終URLを返す（テスト・デバッグ用）.
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
   * <p>転送のたびに転送先へ適用するため、同じ URL へ何度適用しても結果が変わらないこと（冪等）が必要である。SharePoint と OneDrive は download=1
   * を既に含む場合に追加しない。他の変換は、変換後の URL に再適用しても同じ URL になる。
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
    // OneDrive / SharePoint。既に download=1 があれば二重に付けない（転送のたびに変換されるため）
    if (url.contains("sharepoint.com") || url.contains("1drv.ms")) {
      if (url.contains("download=1")) {
        return url;
      }
      return url + (url.contains("?") ? "&" : "?") + "download=1";
    }
    return url;
  }

  /**
   * 要求する URL が https で、解決先が全て公開アドレスかを検査し、許可されない場合は IOException を投げる.
   *
   * <p>対応ドメインの一覧はここでは使わない。利用者が書いた最初の URL だけを一覧で確かめ、転送先は https と公開アドレスだけを確かめる。 Google Drive
   * は大きなファイルを drive.usercontent.google.com へ転送し、Dropbox は dl.dropboxusercontent.com
   * へ転送するため、転送先を一覧で確かめると正しい取得先を拒否してしまう。
   *
   * @param url 検査する URL
   * @throws IOException 取得先として許可されない場合
   */
  private void checkPublicHttps(final String url) throws IOException {
    if (!new PublicHttpsUri(url).isAllowed()) {
      throw new IOException("取得先として許可されていません（https と公開アドレスのみ許可）: " + url);
    }
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
    String host;
    try {
      host = URI.create(url).getHost();
    } catch (IllegalArgumentException e) {
      return false;
    }
    if (host == null) {
      return false;
    }
    // ホスト名が対応ドメインと一致するか、そのサブドメインである場合だけ対応とする
    String lowerHost = host.toLowerCase(Locale.ROOT);
    return SUPPORTED_DOMAINS.stream()
        .anyMatch(domain -> lowerHost.equals(domain) || lowerHost.endsWith("." + domain));
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
        // 値は空白と ; を除く1文字以上なので、空白だけになることはない
        return FileNameUtils.decode(m.group(1));
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
    return "skillsheet_" + ApplicationClock.INSTANCE.clock().millis();
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
