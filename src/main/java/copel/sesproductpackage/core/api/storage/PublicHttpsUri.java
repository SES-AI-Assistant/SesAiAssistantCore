package copel.sesproductpackage.core.api.storage;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;

/**
 * 外部ストレージから取得してよい URL かを表す値オブジェクト.
 *
 * <p>外部ファイルの取得は利用者が書いた URL を起点に行うため、そのまま通信すると内部ネットワークやメタデータサービス（169.254.169.254 など）へ接続させられる。
 * これを防ぐため、要求を送る直前に「スキームが https」「ホストを解決した全てのアドレスが公開アドレス」を確かめる。
 * 接続の後で検査すると、内部アドレスへ接続した後になってしまうため、接続の前に検査する。
 *
 * <p>DNS リバインディング（検査の時点では公開アドレスを返し、接続の時点では内部アドレスへ差し替える攻撃）は、この検査だけでは完全には防げない。Java の HttpClient
 * は検査済みのアドレスへ接続を固定できないためである。
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
final class PublicHttpsUri {

  /** https の既定のポート番号。ポートを明示する場合もこの番号だけを許可する。 */
  private static final int HTTPS_PORT = 443;

  /** 解析済みの URI。解析できない場合や null の場合は null で、許可しない扱いになる。 */
  private final URI uri;

  /**
   * URI から生成する.
   *
   * @param uri 検査対象の URI
   */
  PublicHttpsUri(final URI uri) {
    this.uri = uri;
  }

  /**
   * 文字列の URL から生成する. 解析できない文字列は許可しない扱いにする.
   *
   * <p>利用者が書いた文字列は不正な形式を含み得る。例外を外へ投げると処理全体が止まるため、解析できない場合は null の URI として扱い、後の判定で許可しないことにする。
   *
   * @param url 検査対象の URL 文字列（null も可）
   */
  PublicHttpsUri(final String url) {
    URI parsed = null;
    if (url != null) {
      try {
        parsed = URI.create(url);
      } catch (IllegalArgumentException e) {
        log.debug("URL を解析できないため、取得先として扱わないことにします: {}", e.getMessage());
      }
    }
    this.uri = parsed;
  }

  /**
   * スキームが https で、ホストを持つかを返す. 名前解決は行わない.
   *
   * <p>通信の前に名前解決を行わないことで、判定だけでは外部へ問い合わせを送らないようにする。ホストの無い URL は接続先が無いため許可しない。
   *
   * @return スキームが https で、ホストがあれば true
   */
  boolean isHttpsWithHost() {
    return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
  }

  /**
   * 取得先として許可するかを返す. ホストを名前解決して、全てのアドレスを確かめる.
   *
   * <p>解決したアドレスの一部だけが内部のものでも、接続先はどのアドレスにもなり得るため、全てが公開アドレスの場合だけ許可する。解決できないホストは接続先を確かめられないため許可しない。
   *
   * @return https で、解決した全てのアドレスが公開アドレスであれば true
   */
  boolean isAllowed() {
    if (!isHttpsWithHost()) {
      return false;
    }
    // 省略時は -1 になる。443 以外のポートは内部の別サービスへ接続させ得るため許可しない
    int port = uri.getPort();
    if (port != -1 && port != HTTPS_PORT) {
      return false;
    }
    InetAddress[] addresses;
    try {
      addresses = InetAddress.getAllByName(uri.getHost());
    } catch (UnknownHostException e) {
      log.debug("ホストを解決できないため、取得先として扱わないことにします: {} / {}", uri.getHost(), e.getMessage());
      return false;
    }
    for (InetAddress address : addresses) {
      byte[] bytes = address.getAddress();
      boolean reserved =
          // ループバック、リンクローカル、サイトローカル、マルチキャスト、ワイルドカードは Java の判定を使う
          address.isAnyLocalAddress()
              || address.isLoopbackAddress()
              || address.isLinkLocalAddress()
              || address.isSiteLocalAddress()
              || address.isMulticastAddress()
              // 0.0.0.0/8 は「この」ネットワークを表す予約アドレスで、接続先として使えない
              || bytes.length == 4 && (bytes[0] & 0xFF) == 0
              // 100.64.0.0/10 は事業者内の共有アドレス（CGNAT）で、インターネットの公開アドレスではない
              || bytes.length == 4 && (bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 64
              // 240.0.0.0/4 は将来用の予約で、255.255.255.255 はその範囲に含まれる限定ブロードキャスト
              || bytes.length == 4 && (bytes[0] & 0xFF) >= 240
              // 198.18.0.0/15 は回線の性能測定用に予約された範囲で、公開の接続先ではない
              || bytes.length == 4 && (bytes[0] & 0xFF) == 198 && (bytes[1] & 0xFE) == 18
              // IPv6 は 2000::/3 のグローバルユニキャストだけを公開とする。
              // 先頭が 2000::/3 以外（::/96 の IPv4 互換や 64:ff9b::/96 の NAT64 を含む）は内部の IPv4 へ変換され得るため除外する
              || bytes.length == 16
                  && ((bytes[0] & 0xFF) < 0x20
                      || (bytes[0] & 0xFF) > 0x3F
                      // 2002::/16 は 6to4 で、埋め込んだ IPv4 の内部アドレスへ向き得るため除外する
                      || (bytes[0] & 0xFF) == 0x20 && (bytes[1] & 0xFF) == 0x02
                      // 2001:db8::/32 はドキュメント用の範囲で、公開の接続先ではない
                      || (bytes[0] & 0xFF) == 0x20
                          && (bytes[1] & 0xFF) == 0x01
                          && (bytes[2] & 0xFF) == 0x0D
                          && (bytes[3] & 0xFF) == 0xB8);
      if (reserved) {
        return false;
      }
    }
    return true;
  }
}
