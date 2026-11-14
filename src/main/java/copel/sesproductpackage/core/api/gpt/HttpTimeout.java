package copel.sesproductpackage.core.api.gpt;

import java.net.HttpURLConnection;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 外部 API への HTTP 接続に設定するタイムアウト.
 *
 * <p>応答が止まった呼び出しが Lambda のタイムアウトまで待ち続けないように、接続を開いた直後に設定する。 OpenAI の呼び出しと Gemini Decision
 * の呼び出しで、同じ値を使う。
 *
 * <p>注意: 読み取りのタイムアウトは「データが届かない時間」の上限であり、応答全体の上限ではない。 応答が少しずつ届き続ける場合は、この時間を超えても打ち切られない。
 * 応答全体の上限は、この値では保証しない。
 *
 * @author Copel Co., Ltd.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpTimeout {

  /**
   * 既定のタイムアウト（接続 5 秒、読み取り 60 秒）.
   *
   * <p>応答時間の実測がないため、暫定の値である。接続は TCP と TLS の確立だけなので、短くても足りる。 読み取りは、Gemini Decision が最大で約 26
   * 万トークンの入力を受け取って判定するため、応答が遅い場合を考えて長めにした。
   *
   * <p>呼び出し元の Lambda のタイムアウトがこの値より短い場合は、この値より先に Lambda が打ち切られるため、読み取りのタイムアウトは効かない。 Matcher の Lambda
   * は 15 秒で該当する。Register の Lambda は 90 秒のため、この値が効く。
   */
  public static final HttpTimeout DEFAULT = new HttpTimeout(5_000, 60_000);

  /** 接続の確立を待つ時間（ミリ秒）. */
  private final int connectMillis;

  /** 応答のデータが届くのを待つ時間（ミリ秒）。応答全体の上限ではない。 */
  private final int readMillis;

  /**
   * 接続に、接続のタイムアウトと読み取りのタイムアウトを設定します.
   *
   * @param conn タイムアウトを設定する HTTP 接続
   */
  public void applyTo(final HttpURLConnection conn) {
    conn.setConnectTimeout(connectMillis);
    conn.setReadTimeout(readMillis);
  }
}
