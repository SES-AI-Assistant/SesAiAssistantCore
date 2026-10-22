package copel.sesproductpackage.core.api.line;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** {@link LineMessagingApi} の単体テスト。 */
@TestProperty(
    key = "LINE_DONLOAD_FILE_API_ENDPOINT",
    value = "https://api-data.line.example/v2/bot/message/%s/content")
class LineMessagingApiTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * チャネルアクセストークンを指定した場合に、ファイルの取得に使うインスタンスを作ることを確かめる。 取得の認証にトークンを使うため、生成時点で例外にならないことを守る。
       *
       * @given チャネルアクセストークン "token" がある
       * @when LineMessagingApi を生成する
       * @then LineMessagingApi のインスタンスを返す
       */
      @Test
      void トークンを指定した場合_インスタンスを返す() {
        assertThat(new LineMessagingApi("token")).isNotNull();
      }
    }
  }

  @Nested
  class getFile {

    @Nested
    class 正常系 {

      /**
       * 応答が200の場合に、ファイルの本文を返すことを確かめる。 受信したファイルを後続の登録処理で使うため、本文を返すとともに、送信先 URI と認証で取得していることを守る。
       *
       * @given 送信の応答が200で、本文が3バイトで、トークンが token である
       * @when getFile をメッセージIDで呼び出す
       * @then 本文の3バイトを返し、送信先 URI と Bearer の認証ヘッダーを使う
       */
      @Test
      void 応答が200の場合_本文を返す() throws Exception {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          HttpClient client = clientReturning(response(200, new byte[] {1, 2, 3}));
          hc.when(HttpClient::newHttpClient).thenReturn(client);

          assertThat(new LineMessagingApi("token").getFile("m1")).containsExactly(1, 2, 3);

          ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
          verify(client).send(captor.capture(), any());
          assertThat(captor.getValue().uri().toString())
              .isEqualTo("https://api-data.line.example/v2/bot/message/m1/content");
          assertThat(captor.getValue().headers().firstValue("Authorization"))
              .contains("Bearer token");
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 通信が失敗した場合に、入出力の例外を投げることを確かめる。 通信の失敗を黙って握りつぶさず、呼び出し元に伝えることを守る。
       *
       * @given 送信が IOException を投げる
       * @when getFile をメッセージIDで呼び出す
       * @then IOException を投げる
       */
      @Test
      void 通信が失敗する場合_IOExceptionを投げる() throws Exception {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          HttpClient client = mock(HttpClient.class);
          doThrow(new IOException("接続失敗")).when(client).send(any(HttpRequest.class), any());
          hc.when(HttpClient::newHttpClient).thenReturn(client);

          assertThatThrownBy(() -> new LineMessagingApi("token").getFile("m1"))
              .isInstanceOf(IOException.class);
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 応答が200以外の場合に、null を返すことを確かめる。 取得できないファイルを空の本文として扱うと、誤った登録になることを守る。
       *
       * @given 送信の応答が404である
       * @when getFile をメッセージIDで呼び出す
       * @then null を返す
       */
      @Test
      void 応答が200以外の場合_nullを返す() throws Exception {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          HttpClient client = clientReturning(response(404, null));
          hc.when(HttpClient::newHttpClient).thenReturn(client);

          assertThat(new LineMessagingApi("token").getFile("m1")).isNull();
        }
      }
    }
  }

  /**
   * 指定の応答を返す HttpClient のモックを作る.
   *
   * @param response 応答
   * @return HttpClient のモック
   * @throws Exception 送信のスタブに失敗した場合
   */
  private static HttpClient clientReturning(final HttpResponse<byte[]> response) throws Exception {
    HttpClient client = mock(HttpClient.class);
    doReturn(response).when(client).send(any(HttpRequest.class), any());
    return client;
  }

  /**
   * 本文付きの応答を作る.
   *
   * @param status ステータス
   * @param body 本文
   * @return 応答
   */
  @SuppressWarnings("unchecked")
  private static HttpResponse<byte[]> response(final int status, final byte[] body) {
    HttpResponse<byte[]> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn(body);
    when(response.uri()).thenReturn(URI.create("https://api-data.line.example/"));
    return response;
  }
}
