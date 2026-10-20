package copel.sesproductpackage.core.api.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** {@link ExternalFileLinkLot} の単体テスト。 */
class ExternalFileLinkLotTest {

  private static final String DRIVE_VIEW = "https://drive.google.com/file/d/ABC/view";
  private static final String DRIVE_DIRECT = "https://drive.google.com/uc?export=download&id=ABC";

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テキストに URL が複数ある場合に、URL ごとにリンクを保持することを確かめる。 文中の複数の共有リンクを取りこぼさないようにするため、URL ごとに判定することを守る。
       *
       * @given テキストに Google ドライブの URL が2件含まれ、通信の応答はドライブのファイルを返す
       * @when ExternalFileLinkLot を生成して downloadAll を呼び出す
       * @then 2件ともダウンロードして保持する
       */
      @Test
      void テキストに複数のURLがある場合_URLごとにダウンロードしたリストを返す() throws Exception {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          clientFor(hc, okHead(), okGet(), okHead(), okGet());
          ExternalFileLinkLot lot = new ExternalFileLinkLot(DRIVE_VIEW + " と " + DRIVE_VIEW);

          lot.downloadAll();

          assertThat(lot.getDownloadedLinks()).hasSize(2);
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * テキストが null の場合に、リンクを持たず通信もしないことを確かめる。 本文が無い場合に通信を始めないことを守る。
       *
       * @given テキストが null である
       * @when ExternalFileLinkLot を生成して downloadAll を呼び出す
       * @then ダウンロード済みのリンクが空で、HttpClient を生成しない
       */
      @Test
      void テキストがnullの場合_空のリストを返す() {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          ExternalFileLinkLot lot = new ExternalFileLinkLot(null);

          lot.downloadAll();

          assertThat(lot.getDownloadedLinks()).isEmpty();
          hc.verify(HttpClient::newBuilder, never());
        }
      }

      /**
       * テキストが空白だけの場合に、リンクを持たないことを確かめる。 空白だけの本文から URL を探さないことを守る。
       *
       * @given テキストが空白だけである
       * @when ExternalFileLinkLot を生成して downloadAll を呼び出す
       * @then ダウンロード済みのリンクが空である
       */
      @Test
      void テキストが空白だけの場合_空のリストを返す() {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          ExternalFileLinkLot lot = new ExternalFileLinkLot("   ");

          lot.downloadAll();

          assertThat(lot.getDownloadedLinks()).isEmpty();
          hc.verify(HttpClient::newBuilder, never());
        }
      }

      /**
       * URL を含まないテキストの場合に、リンクを持たないことを確かめる。 URL の無い本文で通信を行わないことを守る。
       *
       * @given テキストに URL が含まれない
       * @when ExternalFileLinkLot を生成して downloadAll を呼び出す
       * @then ダウンロード済みのリンクが空である
       */
      @Test
      void URLを含まないテキストの場合_空のリストを返す() {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          ExternalFileLinkLot lot = new ExternalFileLinkLot("本文だけの文章");

          lot.downloadAll();

          assertThat(lot.getDownloadedLinks()).isEmpty();
          hc.verify(HttpClient::newBuilder, never());
        }
      }
    }
  }

  @Nested
  class downloadAll {

    @Nested
    class 正常系 {

      /**
       * 対応するストレージの URL がある場合に、ダウンロードして保持することを確かめる。 取り込むファイルを後の処理で使えるようにすることを守る。
       *
       * @given テキストに Google ドライブの URL が1件あり、応答はファイルの本文を返す
       * @when downloadAll を呼び出す
       * @then ダウンロード済みのリンクが1件になる
       */
      @Test
      void 対応するURLがある場合_ダウンロードしたリストを返す() throws Exception {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          clientFor(hc, okHead(), okGet());
          ExternalFileLinkLot lot = new ExternalFileLinkLot(DRIVE_VIEW);

          lot.downloadAll();

          assertThat(lot.getDownloadedLinks()).hasSize(1);
          assertThat(lot.getDownloadedLinks().get(0).getRawUrl()).isEqualTo(DRIVE_VIEW);
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 1件のダウンロードが失敗しても、後続のリンクを処理し続けることを確かめる。 1件の失敗で残りのファイルを取りこぼさないことを守る。
       *
       * @given 1件目の本文の取得が 500 で失敗し、2件目は成功する
       * @when downloadAll を呼び出す
       * @then 2件目だけをダウンロード済みとして保持する
       */
      @Test
      void ダウンロードが1件失敗する場合_成功した1件だけのリストを返す() throws Exception {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          clientFor(hc, okHead(), response(500, DRIVE_DIRECT, new byte[0]), okHead(), okGet());
          ExternalFileLinkLot lot = new ExternalFileLinkLot(DRIVE_VIEW + " " + DRIVE_VIEW);

          lot.downloadAll();

          assertThat(lot.getDownloadedLinks()).hasSize(1);
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対応外のドメインの URL の場合に、ダウンロードせずに保持しないことを確かめる。 対応外の共有リンクを取り込まないことを守る。
       *
       * @given テキストの URL の応答の最終 URL が example.com である
       * @when downloadAll を呼び出す
       * @then ダウンロード済みのリンクが空である
       */
      @Test
      void 対応外のドメインの場合_空のリストを返す() throws Exception {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          clientFor(hc, response(200, "https://example.com/a", new byte[0]));
          ExternalFileLinkLot lot = new ExternalFileLinkLot("https://example.com/a");

          lot.downloadAll();

          assertThat(lot.getDownloadedLinks()).isEmpty();
        }
      }
    }
  }

  @Nested
  class getDownloadedLinks {

    @Nested
    class 正常系 {

      /**
       * ダウンロードしていない場合に、空の一覧を返すことを確かめる。 取得していないファイルを含めないことを守る。
       *
       * @given downloadAll を呼び出していない
       * @when getDownloadedLinks を呼び出す
       * @then 空の一覧を返す
       */
      @Test
      void ダウンロードしていない場合_空の一覧を返す() {
        assertThat(new ExternalFileLinkLot(DRIVE_VIEW).getDownloadedLinks()).isEmpty();
      }
    }
  }

  /**
   * HttpClient の生成を差し替え、送信の応答を順に返すモックを作る.
   *
   * @param hc HttpClient の静的モック
   * @param responses 送信のたびに返す応答
   * @return HttpClient のモック
   * @throws Exception 送信のスタブに失敗した場合
   */
  private static HttpClient clientFor(
      final MockedStatic<HttpClient> hc, final HttpResponse<?>... responses) throws Exception {
    HttpClient client = mock(HttpClient.class);
    HttpClient.Builder builder = mock(HttpClient.Builder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(client);
    hc.when(HttpClient::newBuilder).thenReturn(builder);
    HttpResponse<?> first = responses[0];
    HttpResponse<?>[] rest = java.util.Arrays.copyOfRange(responses, 1, responses.length);
    doReturn(first, (Object[]) rest).when(client).send(any(HttpRequest.class), any());
    return client;
  }

  /**
   * 成功した HEAD の応答を作る（最終 URL は Google ドライブのファイル）.
   *
   * @return 応答
   */
  private static HttpResponse<Void> okHead() {
    return response(200, DRIVE_VIEW, null);
  }

  /**
   * 成功した GET の応答を作る（本文を持つ）.
   *
   * @return 応答
   */
  private static HttpResponse<byte[]> okGet() {
    return response(200, DRIVE_DIRECT, new byte[] {1});
  }

  /**
   * 応答を作る.
   *
   * @param status ステータス
   * @param uri 最終 URL
   * @param body 本文（HEAD の場合は null）
   * @param <T> 本文の型
   * @return 応答
   */
  @SuppressWarnings("unchecked")
  private static <T> HttpResponse<T> response(
      final int status, final String uri, final byte[] body) {
    HttpResponse<T> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.uri()).thenReturn(URI.create(uri));
    when(response.body()).thenReturn((T) body);
    when(response.headers())
        .thenReturn(
            HttpHeaders.of(Map.of("Content-Type", List.of("application/pdf")), (a, b) -> true));
    return response;
  }
}
