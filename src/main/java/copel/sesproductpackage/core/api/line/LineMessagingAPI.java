package copel.sesproductpackage.core.api.line;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import copel.sesproductpackage.core.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 * 【フレームワーク部品】
 * LINE Messaging APIでLINE通知を送るクラス
 *
 * @author 鈴木一矢
 *
 */
@Slf4j
public class LineMessagingAPI {
    // ================================
    // 定数
    // ================================
    /**
     * メッセージ送信APIのエンドポイント
     */
    private static final String PUSH_MESSAGE_API_ENDPOINT = Properties.get("LINE_PUSH_MESSAGE_API_ENDPOINT");
    /**
     * ブロードキャストAPIのエンドポイント
     */
    private static final String BROADCAST_API_ENDPOINT = Properties.get("LINE_BROADCAST_API_ENDPOINT");
    /**
     * ファイルダウンロードAPIのエンドポイント
     */
    private static final String DONLOAD_FILE_API_ENDPOINT = Properties.get("LINE_DONLOAD_FILE_API_ENDPOINT");

    // ================================
    // フィールド定義
    // ================================
    /**
     * メッセージリスト.
     */
    private List<String> messageList;
    /**
     * LINEアクセストークン.
     */
    private final String channelAccessToken;

    // ================================
    // コンストラクタ定義
    // ================================
    public LineMessagingAPI(final String channelAccessToken) {
        this.messageList = new ArrayList<String>();
        this.channelAccessToken = channelAccessToken;
    }

    // ================================
    // メソッド定義
    // ================================
    /**
     * このオブジェクトのmessageListフィールドに引数のmessageを格納します
     *
     * @param message メッセージ
     */
    public void addMessage(final String message) {
        this.messageList.add(message);
    }

    /**
     * messageListの各要素ごとにこのオブジェクトにもつuserIdの個別LINEにメッセージ送信リクエストを送信するメソッド
     * @return ResultObject 送信結果
     */
    public void sendSeparate(final String toUserId) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            for (final String message : this.messageList) {
                // JSON形式のメッセージボディを作成
                String json = "{\"to\":\""
                    + toUserId
                    + "\",\"messages\":[{\"type\":\"text\",\"text\":\""
                    + message
                    + "\"}]}";

                // HTTPリクエストの作成
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(PUSH_MESSAGE_API_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + this.channelAccessToken)
                        .POST(BodyPublishers.ofString(json))
                        .build();
                
                // リクエスト送信
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // レスポンスステータスの確認
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    log.warn("【SesAiAssitantCore】LINE Messaging APIでのメッセージ送信中にエラーが発生しました：{}", response.statusCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * このオブジェクトに格納されているメッセージを全ユーザーにメッセージ配信する
     */
    public void broadCast() {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
 
            // メッセージボディを作成
            String jsonMessage = "{\"messages\":[{\"type\":\"text\",\"text\":\"";
            for (final String lineMessage : this.messageList) {
                jsonMessage += lineMessage;
            }
            jsonMessage += "\"}]}";

            // HTTPリクエストの作成
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BROADCAST_API_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + this.channelAccessToken)
                    .POST(BodyPublishers.ofString(jsonMessage))
                    .build();

            // リクエストを送信
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // レスポンスステータスの確認
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                log.warn("【SesAiAssitantCore】LINE Messaging APIでのメッセージ配信中にエラーが発生しました：{}", response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 引数で指定したメッセージIDで送信されたファイルをbyte形式で取得する.
     *
     * @param messageId メッセージID.
     * @return ファイルのバイナリ値
     * @throws IOException
     * @throws InterruptedException
     */
    public byte[] getFile(final String messageId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String url = String.format(DONLOAD_FILE_API_ENDPOINT, messageId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + this.channelAccessToken)
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            return response.body();
        } else {
            log.warn("【SesAiAssitantCore】LINE Messaging APIでのファイルダウンロード中にエラーが発生しました：{}", response.statusCode());
            return null;
        }
    }

    /**
     * このクラスのメッセージリストを返却します.
     *
     * @return メッセージリスト
     */
	public List<String> getMessageList() {
		return this.messageList;
	}
}
