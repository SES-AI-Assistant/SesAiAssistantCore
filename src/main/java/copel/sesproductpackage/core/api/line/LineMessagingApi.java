package copel.sesproductpackage.core.api.line;

import copel.sesproductpackage.core.util.Properties;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 【フレームワーク部品】 LINE Messaging API で送信されたファイルを取得するクラス
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class LineMessagingApi {
  // ================================
  // 定数
  // ================================
  // ================================
  // フィールド定義
  // ================================
  /** LINEアクセストークン. */
  private final String channelAccessToken;

  // ================================
  // コンストラクタ定義
  // ================================
  /**
   * コンストラクタ.
   *
   * @param channelAccessToken LINE のチャネルアクセストークン
   */
  public LineMessagingApi(final String channelAccessToken) {
    this.channelAccessToken = channelAccessToken;
  }

  // ================================
  // メソッド定義
  // ================================
  /**
   * 引数で指定したメッセージIDで送信されたファイルをbyte形式で取得する.
   *
   * @param messageId メッセージID.
   * @return ファイルのバイナリ値
   * @throws IOException 入出力（ファイル・ストリーム）の操作に失敗した場合
   * @throws InterruptedException 待機中にスレッドが中断された場合
   */
  public byte[] getFile(final String messageId) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    // ファイルダウンロード API のエンドポイントは、使う時点で Properties から読む
    String url = String.format(Properties.get("LINE_DONLOAD_FILE_API_ENDPOINT"), messageId);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + this.channelAccessToken)
            .GET()
            .build();

    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

    if (response.statusCode() == HttpURLConnection.HTTP_OK) {
      return response.body();
    } else {
      log.warn(
          "【SesAiAssitantCore】LINE Messaging APIでのファイルダウンロード中にエラーが発生しました：{}",
          response.statusCode());
      return null;
    }
  }
}
