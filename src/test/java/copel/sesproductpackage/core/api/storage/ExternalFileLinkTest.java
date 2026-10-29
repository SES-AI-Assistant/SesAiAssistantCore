package copel.sesproductpackage.core.api.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** {@link ExternalFileLink} の単体テスト。 */
@FixedClock("2026-01-15T12:00:00+09:00")
class ExternalFileLinkTest {

  private static final String DRIVE_VIEW = "https://drive.google.com/file/d/ABC/view";
  private static final String DRIVE_DIRECT = "https://drive.google.com/uc?export=download&id=ABC";
  private static final String DRIVE_VIEW_XYZ = "https://drive.google.com/file/d/XYZ/view";
  private static final String DRIVE_DIRECT_XYZ =
      "https://drive.google.com/uc?export=download&id=XYZ";

  /** 公開アドレスとして扱う 8.8.8.8 の4バイト表現。 */
  private static final byte[] PUBLIC_ADDRESS = {8, 8, 8, 8};

  /** 時間の経過を再現するテストの起点時刻。 */
  private static final Instant START = Instant.parse("2026-01-15T03:00:00Z");

  /** 名前解決の静的モック。外部の DNS へ問い合わせないように、テストごとに開いて閉じる。 */
  private MockedStatic<InetAddress> dns;

  /**
   * 名前解決をモックに差し替え、ドライブのホストを公開アドレスへ解決させる。
   *
   * @throws UnknownHostException 名前解決のスタブに失敗した場合
   */
  @BeforeEach
  void 名前解決をモックに差し替える() throws UnknownHostException {
    dns = mockStatic(InetAddress.class, CALLS_REAL_METHODS);
    resolveTo(dns, "drive.google.com", PUBLIC_ADDRESS);
  }

  /** 名前解決のモックと時計を既定に戻す。 */
  @AfterEach
  void モックを解除する() {
    dns.close();
    ApplicationClock.INSTANCE.resetToDefault();
  }

  @Nested
  class コンストラクタ_String {

    @Nested
    class 正常系 {

      /**
       * 元の URL を指定した場合に、転送を自動で追わない設定と接続のタイムアウトを HttpClient の生成に登録することを確かめる。
       * 自動の転送は検査していない先へ接続するため、無効にすることを守る。
       *
       * @given 元の URL が DRIVE_VIEW である
       * @when ExternalFileLink を生成する
       * @then Builder に Redirect.NEVER と接続のタイムアウト 15 秒を登録し、getRawUrl が元の URL を返す
       */
      @Test
      void 元のURLを指定した場合_転送を追わない設定を登録する() {
        try (MockedStatic<HttpClient> hc = mockStatic(HttpClient.class)) {
          HttpClient.Builder builder = mock(HttpClient.Builder.class, RETURNS_SELF);
          hc.when(HttpClient::newBuilder).thenReturn(builder);

          ExternalFileLink link = new ExternalFileLink(DRIVE_VIEW);

          verify(builder).followRedirects(HttpClient.Redirect.NEVER);
          verify(builder).connectTimeout(Duration.ofSeconds(15));
          assertThat(link.getRawUrl()).isEqualTo(DRIVE_VIEW);
        }
      }
    }
  }

  @Nested
  class コンストラクタ_String_HttpClient {

    @Nested
    class 正常系 {

      /**
       * HttpClient を注入した場合に、その元の URL を保持することを確かめる。 通信を差し替えて判定と取得を検査できるようにするため、注入の経路を守る。
       *
       * @given 元の URL と HttpClient のモックがある
       * @when ExternalFileLink を注入付きで生成する
       * @then getRawUrl が元の URL を返す
       */
      @Test
      void HttpClientを注入した場合_元のURLを返す() {
        assertThat(new ExternalFileLink(DRIVE_VIEW, mock(HttpClient.class)).getRawUrl())
            .isEqualTo(DRIVE_VIEW);
      }
    }
  }

  @Nested
  class isDownloadable {

    @Nested
    class 正常系 {

      /**
       * 対応するストレージの https の URL の場合に、ダウンロード可能と判定することを確かめる。 判定の時点では接続せず、取得に使う直リンクだけを保持することを守る。
       *
       * @given URL が DRIVE_VIEW である
       * @when isDownloadable を呼び出す
       * @then true を返し、直リンクを保持し、HttpClient の送信を呼ばない
       */
      @Test
      void 対応ストレージのURLの場合_trueを返す() throws Exception {
        HttpClient client = mock(HttpClient.class);
        ExternalFileLink link = new ExternalFileLink(DRIVE_VIEW, client);

        assertThat(link.isDownloadable()).isTrue();

        assertThat(link.getResolvedUrl()).isEqualTo(DRIVE_VIEW);
        assertThat(link.getDirectUrl()).isEqualTo(DRIVE_DIRECT);
        verify(client, never()).send(any(HttpRequest.class), any());
      }
    }

    @Nested
    class 境界値 {

      /**
       * URL が null の場合に、ダウンロード不可と判定することを確かめる。 値が無い URL で接続しないことを守る。
       *
       * @given 元の URL が null である
       * @when isDownloadable を呼び出す
       * @then false を返し、HttpClient の送信を呼ばない
       */
      @Test
      void URLがnullの場合_falseを返す() throws Exception {
        HttpClient client = mock(HttpClient.class);

        assertThat(new ExternalFileLink(null, client).isDownloadable()).isFalse();

        verify(client, never()).send(any(HttpRequest.class), any());
      }

      /**
       * URL が空白だけの場合に、ダウンロード不可と判定することを確かめる。 空白だけの URL で接続しないことを守る。
       *
       * @given 元の URL が空白だけである
       * @when isDownloadable を呼び出す
       * @then false を返し、HttpClient の送信を呼ばない
       */
      @Test
      void URLが空白だけの場合_falseを返す() throws Exception {
        HttpClient client = mock(HttpClient.class);

        assertThat(new ExternalFileLink("  ", client).isDownloadable()).isFalse();

        verify(client, never()).send(any(HttpRequest.class), any());
      }

      /**
       * スキームが http の場合に、ダウンロード不可と判定することを確かめる。 平文の通信で取得すると内容を途中で読み替えられるため、https の URL だけを対象にすることを守る。
       *
       * @given 元の URL が http の対応ドメインの URL である
       * @when isDownloadable を呼び出す
       * @then false を返し、HttpClient の送信を呼ばない
       */
      @Test
      void スキームがhttpの場合_falseを返す() throws Exception {
        HttpClient client = mock(HttpClient.class);

        assertThat(
                new ExternalFileLink("http://drive.google.com/file/d/ABC/view", client)
                    .isDownloadable())
            .isFalse();

        verify(client, never()).send(any(HttpRequest.class), any());
      }

      /**
       * URL が解析できない場合に、例外を投げずにダウンロード不可と判定することを確かめる。 本文の中の不正な URL で処理全体を止めないことを守る。
       *
       * @given 元の URL が空白を含む "https://drive.google.com/file/d/A BC/view" である
       * @when isDownloadable を呼び出す
       * @then false を返し、HttpClient の送信を呼ばない
       */
      @Test
      void URLが解析できない場合_falseを返す() throws Exception {
        HttpClient client = mock(HttpClient.class);

        assertThat(
                new ExternalFileLink("https://drive.google.com/file/d/A BC/view", client)
                    .isDownloadable())
            .isFalse();

        verify(client, never()).send(any(HttpRequest.class), any());
      }

      /**
       * 対応外のドメインの場合に、ダウンロード不可と判定することを確かめる。 対応外の共有リンクを取り込まないことを守る。
       *
       * @given 元の URL が https://example.com/x である
       * @when isDownloadable を呼び出す
       * @then false を返し、HttpClient の送信を呼ばない
       */
      @Test
      void 対応外のドメインの場合_falseを返す() throws Exception {
        HttpClient client = mock(HttpClient.class);

        assertThat(new ExternalFileLink("https://example.com/x", client).isDownloadable())
            .isFalse();

        verify(client, never()).send(any(HttpRequest.class), any());
      }
    }
  }

  @Nested
  class download {

    @Nested
    class 正常系 {

      /**
       * 直リンクの応答が成功した場合に、ファイルの本文と名前を保持することを確かめる。 取り込んだファイルを後の処理で使うため、本文と名前の両方を保持することを守る。
       *
       * @given 直リンクの応答が200で、本文が3バイト、Content-Disposition の filename が report.pdf である
       * @when download を呼び出す
       * @then 本文と report.pdf を保持したファイルを返す
       */
      @Test
      void 応答が成功した場合_本文と名前を保持したファイルを返す() throws Exception {
        ExternalFileLink link =
            downloadableLink(
                clientReturning(
                    response(
                        200,
                        new byte[] {1, 2, 3},
                        Map.of(
                            "Content-Disposition", List.of("attachment; filename=report.pdf")))));

        link.download();

        assertThat(link.getFileData()).containsExactly(1, 2, 3);
        assertThat(link.getFileName()).isEqualTo("report.pdf");
      }

      /**
       * 応答の本文が null の場合に、ファイルの本文を null として保持することを確かめる。 本文が無い応答を例外にせず、空として扱うことを守る。
       *
       * @given 直リンクの応答が200で、本文が null である
       * @when download を呼び出す
       * @then 本文が null のファイルを返す
       */
      @Test
      void 本文がnullの場合_本文がnullのファイルを返す() throws Exception {
        ExternalFileLink link = downloadableLink(clientReturning(response(200, null, Map.of())));

        link.download();

        assertThat(link.getFileData()).isNull();
      }

      /**
       * 転送先が閲覧用 URL の場合に、直リンクへ変換して取得することを確かめる。 Google ドライブの閲覧用 URL へ転送されても取得できることを守る。
       *
       * @given 直リンクの応答が302で転送先が Google ドライブの閲覧用 URL、転送先の応答が200で本文が1バイトである
       * @when download を呼び出す
       * @then 2回の要求はどちらも GET で、2回目は直リンクへ変換した URL を要求し、本文を返す
       */
      @Test
      void 閲覧用URLへ転送された場合_直リンクに変換した本文を返す() throws Exception {
        HttpClient client =
            clientReturning(
                response(302, null, Map.of("Location", List.of(DRIVE_VIEW_XYZ))),
                response(200, new byte[] {1}, Map.of()));
        ExternalFileLink link = downloadableLink(client);

        link.download();

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(2)).send(captor.capture(), any());
        assertThat(captor.getAllValues()).extracting(HttpRequest::method).containsOnly("GET");
        assertThat(captor.getAllValues())
            .extracting(request -> request.uri().toString())
            .containsExactly(DRIVE_DIRECT, DRIVE_DIRECT_XYZ);
        assertThat(link.getResolvedUrl()).isEqualTo(DRIVE_VIEW_XYZ);
        assertThat(link.getDirectUrl()).isEqualTo(DRIVE_DIRECT_XYZ);
        assertThat(link.getFileData()).containsExactly(1);
      }

      /**
       * 転送先が download 指定を含む SharePoint の URL の場合に、指定を重ねずに取得することを確かめる。 転送のたびに変換するため、download=1
       * が二重に付くと取得できないことを守る。
       *
       * @given 元の URL が SharePoint の URL で、転送先の URL が download=1 を含み、転送先の応答が200である
       * @when download を呼び出す
       * @then 2回目の要求は download=1 を1つだけ含む URL を要求し、本文を返す
       */
      @Test
      void 転送先にdownload指定がある場合_指定を重ねずに取得した本文を返す() throws Exception {
        resolveTo(dns, "contoso.sharepoint.com", PUBLIC_ADDRESS);
        HttpClient client =
            clientReturning(
                response(
                    302,
                    null,
                    Map.of(
                        "Location",
                        List.of("https://contoso.sharepoint.com/sites/doc.docx?download=1"))),
                response(200, new byte[] {2}, Map.of()));
        ExternalFileLink link =
            new ExternalFileLink("https://contoso.sharepoint.com/doc.docx", client);
        link.isDownloadable();

        link.download();

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(2)).send(captor.capture(), any());
        assertThat(captor.getAllValues().get(1).uri().toString())
            .isEqualTo("https://contoso.sharepoint.com/sites/doc.docx?download=1");
        assertThat(link.getFileData()).containsExactly(2);
      }

      /**
       * 一覧に無いホストでも公開アドレスに解決される転送先は取得できることを確かめる。 Google ドライブや Dropbox の転送先を対応ドメインの一覧で拒否しないことを守る。
       *
       * @given 直リンクの応答が302で、転送先が ValueSource の各行の URL で、その名前が公開アドレスに解決される
       * @when download を呼び出す
       * @then 転送先を2回目の要求として取得し、本文を返す
       */
      @ParameterizedTest
      @ValueSource(
          strings = {
            "https://drive.usercontent.google.com/download?id=XYZ&export=download",
            "https://dl.dropboxusercontent.com/s/abc/file.pdf",
            "https://public-host.example/file.pdf"
          })
      void 公開アドレスに解決される対応外ドメインへの転送の場合_本文を返す(final String location) throws Exception {
        resolveTo(dns, "drive.usercontent.google.com", PUBLIC_ADDRESS);
        resolveTo(dns, "dl.dropboxusercontent.com", PUBLIC_ADDRESS);
        resolveTo(dns, "public-host.example", PUBLIC_ADDRESS);
        HttpClient client =
            clientReturning(
                response(302, null, Map.of("Location", List.of(location))),
                response(200, new byte[] {3}, Map.of()));
        ExternalFileLink link = downloadableLink(client);

        link.download();

        assertThat(link.getFileData()).containsExactly(3);
        verify(client, times(2)).send(any(), any());
      }

      /**
       * 応答が200の場合に、本文を読み込んで保持することを確かめる。 取得したファイルの中身を失わないことを守る。
       *
       * @given 直リンクの応答が200で、送信に渡された本文の読み込み方を取り出す
       * @when 3バイトの本文を読み込み方に渡す
       * @then 本文の3バイトをそのまま返す
       */
      @Test
      @SuppressWarnings("unchecked")
      void 応答が200の場合_受信した本文を返す() throws Exception {
        HttpClient client = clientReturning(response(200, new byte[] {1}, Map.of()));
        ExternalFileLink link = downloadableLink(client);
        link.download();
        ArgumentCaptor<HttpResponse.BodyHandler> captor =
            ArgumentCaptor.forClass(HttpResponse.BodyHandler.class);
        verify(client).send(any(HttpRequest.class), captor.capture());

        assertThat(bodyOf(captor.getValue(), 200, new byte[] {1, 2, 3})).containsExactly(1, 2, 3);
      }

      /**
       * 転送先が同じホストの相対パスの場合に、解決した閲覧用 URL を保持することを確かめる。 相対参照の転送を取りこぼさず、同じホストで取得を続けることを守る。
       *
       * @given 直リンクの応答が302で、転送先が /file/d/XYZ/view の相対パスである
       * @when download を呼び出す
       * @then 解決した閲覧用 URL を resolvedUrl に、直リンクを directUrl に保持する
       */
      @Test
      void 相対パスの転送先の場合_解決した閲覧用URLを返す() throws Exception {
        HttpClient client =
            clientReturning(
                response(302, null, Map.of("Location", List.of("/file/d/XYZ/view"))),
                response(200, new byte[] {1}, Map.of()));
        ExternalFileLink link = downloadableLink(client);

        link.download();

        assertThat(link.getResolvedUrl()).isEqualTo(DRIVE_VIEW_XYZ);
        assertThat(link.getDirectUrl()).isEqualTo(DRIVE_DIRECT_XYZ);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 直リンクが未設定の場合に、例外を投げることを確かめる。 判定を先に行わずに取得を始めると、誤った URL へ接続することを守る。
       *
       * @given isDownloadable を呼び出していない
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 直リンクが未設定の場合_IOExceptionを投げる() {
        ExternalFileLink link = new ExternalFileLink(DRIVE_VIEW, mock(HttpClient.class));

        assertThatThrownBy(link::download).isInstanceOf(IOException.class);
      }

      /**
       * 直リンクの名前が内部アドレスに解決される場合に、接続せず例外を投げることを確かめる。 対応ドメインでも内部アドレスへ向く場合を取得させないことを守る。
       *
       * @given ホスト drive.google.com が 10.0.0.5 に解決される
       * @when download を呼び出す
       * @then IOException を投げ、HttpClient の送信を呼ばない
       */
      @Test
      void 直リンクが内部アドレスに解決される場合_IOExceptionを投げる() throws Exception {
        resolveTo(dns, "drive.google.com", new byte[] {10, 0, 0, 5});
        HttpClient client = mock(HttpClient.class);
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("取得先として許可されていません");
        verify(client, never()).send(any(HttpRequest.class), any());
      }

      /**
       * 応答が200以外の場合に、例外を投げることを確かめる。 エラーの応答を本文として保存しないことを守る。
       *
       * @given 直リンクの応答が500である
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 応答が200以外の場合_IOExceptionを投げる() throws Exception {
        ExternalFileLink link =
            downloadableLink(clientReturning(response(500, new byte[0], Map.of())));

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("HTTP 500");
      }

      /**
       * 応答が HTML の場合に、例外を投げることを確かめる。 ウイルス確認ページを取り込まないことを守る。
       *
       * @given 直リンクの応答の Content-Type が text/html である
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 応答がHTMLの場合_IOExceptionを投げる() throws Exception {
        ExternalFileLink link =
            downloadableLink(
                clientReturning(
                    response(
                        200,
                        new byte[0],
                        Map.of("Content-Type", List.of("text/html; charset=utf-8")))));

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("HTML");
      }

      /**
       * 本文が上限を超える場合に、例外を投げることを確かめる。 上限を超えるファイルをメモリに保持しないことを守る。
       *
       * @given 直リンクの応答の本文が上限より1バイト大きい
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 本文が上限を超える場合_IOExceptionを投げる() throws Exception {
        byte[] large = new byte[(int) ExternalFileLink.MAX_FILE_SIZE_BYTES + 1];
        ExternalFileLink link = downloadableLink(clientReturning(response(200, large, Map.of())));

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("上限");
      }

      /**
       * 送信が失敗した場合に、同じ例外をそのまま投げることを確かめる。 通信の失敗を握りつぶして空のファイルを返さないことを守る。
       *
       * @given 直リンクへの送信が IOException を投げる
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 送信が失敗した場合_IOExceptionを投げる() throws Exception {
        HttpClient client = mock(HttpClient.class);
        doThrow(new IOException("接続失敗")).when(client).send(any(HttpRequest.class), any());
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download).isInstanceOf(IOException.class).hasMessage("接続失敗");
      }

      /**
       * 転送の応答に Location が無い場合に、例外を投げることを確かめる。 転送先の無い応答を取得済みとして扱わないことを守る。
       *
       * @given 直リンクの応答が302で、Location ヘッダーが無い
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 転送先のLocationが無い場合_IOExceptionを投げる() throws Exception {
        ExternalFileLink link = downloadableLink(clientReturning(response(302, null, Map.of())));

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Location");
      }

      /**
       * 転送先の URL が不正な場合に、例外を投げることを確かめる。 解析できない転送先へ接続しないことを守る。
       *
       * @given 直リンクの応答が302で、Location が空白を含む "https://drive.google.com/a b" である
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 不正な転送先のURLの場合_IOExceptionを投げる() throws Exception {
        ExternalFileLink link =
            downloadableLink(
                clientReturning(
                    response(
                        302, null, Map.of("Location", List.of("https://drive.google.com/a b")))));

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("不正");
      }

      /**
       * 転送先が内部アドレスの場合に、接続せず例外を投げることを確かめる。 対応ドメインのサーバーが内部アドレスへ転送しても取得させないことを守る。
       *
       * @given 直リンクの応答が302で転送先が https://169.254.169.254/latest/meta-data/ である
       * @when download を呼び出す
       * @then IOException を投げ、転送先へは接続しない
       */
      @Test
      void 転送先が内部アドレスの場合_IOExceptionを投げる() throws Exception {
        HttpClient client =
            clientReturning(
                response(
                    302,
                    null,
                    Map.of("Location", List.of("https://169.254.169.254/latest/meta-data/"))));
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("取得先として許可されていません");
        verify(client, times(1)).send(any(), any());
      }

      /**
       * 転送先が対応ドメインでも内部アドレスに解決される場合に、接続せず例外を投げることを確かめる。 ドメインが対応していても、解決先が内部なら取得させないことを守る。
       *
       * @given 直リンクの応答が302で転送先が docs.google.com の URL、その名前が 10.0.0.5 に解決される
       * @when download を呼び出す
       * @then IOException を投げ、転送先へは接続しない
       */
      @Test
      void 転送先が内部アドレスに解決される場合_IOExceptionを投げる() throws Exception {
        resolveTo(dns, "docs.google.com", new byte[] {10, 0, 0, 5});
        HttpClient client =
            clientReturning(
                response(
                    302,
                    null,
                    Map.of("Location", List.of("https://docs.google.com/document/d/XYZ/edit"))));
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download).isInstanceOf(IOException.class);
        verify(client, times(1)).send(any(), any());
      }

      /**
       * 転送先が http の場合に、接続せず例外を投げることを確かめる。 変換で https に書き換えても、平文の転送先を見逃さないことを守る。
       *
       * @given 直リンクの応答が302で転送先が http://drive.google.com/file/d/XYZ/view である
       * @when download を呼び出す
       * @then IOException を投げ、転送先へは接続しない
       */
      @Test
      void 転送先がhttpの場合_IOExceptionを投げる() throws Exception {
        HttpClient client =
            clientReturning(
                response(
                    302,
                    null,
                    Map.of("Location", List.of("http://drive.google.com/file/d/XYZ/view"))));
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download).isInstanceOf(IOException.class);
        verify(client, times(1)).send(any(), any());
      }

      /**
       * 全体の時間を超える場合に、例外を投げることを確かめる。 転送を含めた全体を Backend の API Gateway の枠内に収めることを守る。
       *
       * @given 1回目の要求で26秒が経過し、直リンクの応答が302で転送される
       * @when download を呼び出す
       * @then IOException を投げ、2回目の要求は送らない
       */
      @Test
      void 全体の時間を超える場合_IOExceptionを投げる() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(START);
        ApplicationClock.INSTANCE.setClock(movableClock(now));
        HttpClient client =
            clientAdvancing(
                now,
                Duration.ofSeconds(26),
                redirect(DRIVE_VIEW_XYZ),
                response(200, new byte[] {1}, Map.of()));
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("タイムアウト");
        verify(client, times(1)).send(any(), any());
      }

      /**
       * 名前解決に25秒かかった場合に、全体の上限を超えたと判定して接続しないことを確かめる。 名前解決の時間も全体の上限に含め、Backend の API Gateway
       * の枠を超えないことを守る。
       *
       * @given 名前解決が25秒を経過させ、直リンクの応答は200である
       * @when download を呼び出す
       * @then タイムアウトの IOException を投げ、HttpClient の送信を呼ばない
       */
      @Test
      void DNS解決で25秒経過する場合_IOExceptionを投げる() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(START);
        ApplicationClock.INSTANCE.setClock(movableClock(now));
        resolveAfter(dns, "drive.google.com", now, Duration.ofSeconds(25));
        HttpClient client = mock(HttpClient.class);
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("タイムアウト");
        verify(client, never()).send(any(HttpRequest.class), any());
      }

      /**
       * 転送先の Location が空白だけの場合に、解決せずに例外を投げることを確かめる。 空の転送先で同じ URL へ戻る無駄な要求を送らないことを守る。
       *
       * @given 直リンクの応答が302で、Location が空白だけである
       * @when download を呼び出す
       * @then 空のLocationを理由にIOExceptionを投げ、2回目の要求を送らない
       */
      @Test
      void 転送先のLocationが空の場合_IOExceptionを投げる() throws Exception {
        HttpClient client =
            clientReturning(response(302, null, Map.of("Location", List.of("   "))));
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("空");
        verify(client, times(1)).send(any(), any());
      }

      /**
       * スキームを省いた転送先が内部アドレスを指す場合に、接続せず例外を投げることを確かめる。 スキームは直リンクの https を引き継ぐため、内部への転送を検査で止めることを守る。
       *
       * @given 直リンクの応答が302で、転送先が //169.254.169.254/x である
       * @when download を呼び出す
       * @then 取得先として許可されない IOException を投げ、転送先へは接続しない
       */
      @Test
      void スキーム相対の転送先が内部アドレスの場合_IOExceptionを投げる() throws Exception {
        HttpClient client =
            clientReturning(
                response(302, null, Map.of("Location", List.of("//169.254.169.254/x"))));
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("取得先として許可されていません");
        verify(client, times(1)).send(any(), any());
      }

      /**
       * 応答が304の場合に、転送ではなく例外を投げることを確かめる。 本文の無い応答を取得済みとして扱わないことを守る。
       *
       * @given 直リンクの応答が304で、Location が無い
       * @when download を呼び出す
       * @then ステータス304を含む IOException を投げる
       */
      @Test
      void 応答が304の場合_IOExceptionを投げる() throws Exception {
        ExternalFileLink link =
            downloadableLink(clientReturning(response(304, new byte[0], Map.of())));

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("HTTP 304");
      }

      /**
       * 200 以外の応答は本文を読み捨てることを確かめる。 失敗の応答の本文をメモリに読み込まないことを守る。
       *
       * @given 直リンクの応答が500である
       * @when download を呼び出し、送信に渡された本文の読み込み方に3バイトを渡す
       * @then IOException を投げ、読み込んだ本文として空の配列を返す
       */
      @Test
      @SuppressWarnings("unchecked")
      void 応答が200以外の場合_読み捨てた空の配列を返す() throws Exception {
        HttpClient client = clientReturning(response(500, new byte[0], Map.of()));
        ExternalFileLink link = downloadableLink(client);
        assertThatThrownBy(link::download).isInstanceOf(IOException.class);
        ArgumentCaptor<HttpResponse.BodyHandler> captor =
            ArgumentCaptor.forClass(HttpResponse.BodyHandler.class);
        verify(client).send(any(HttpRequest.class), captor.capture());

        assertThat(bodyOf(captor.getValue(), 500, new byte[] {1, 2, 3})).isEmpty();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 名前解決に24秒かかった場合に、残りの1秒だけを要求の上限にすることを確かめる。 名前解決の時間を全体の上限から引き、要求の時間に加えないことを守る。
       *
       * @given 名前解決が24秒を経過させ、直リンクの応答は200である
       * @when download を呼び出す
       * @then 残りの1秒を上限とした要求を1回送信する
       */
      @Test
      void DNS解決で24秒経過した場合_残りの1秒を上限とした要求を送信する() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(START);
        ApplicationClock.INSTANCE.setClock(movableClock(now));
        resolveAfter(dns, "drive.google.com", now, Duration.ofSeconds(24));
        HttpClient client = clientReturning(response(200, new byte[] {1}, Map.of()));
        ExternalFileLink link = downloadableLink(client);

        link.download();

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(1)).send(captor.capture(), any());
        assertThat(captor.getValue().timeout()).contains(Duration.ofSeconds(1));
      }

      /**
       * 転送がちょうど5回の場合に、最後の応答の本文を保持することを確かめる。 上限の5回までは転送を追うことを守る。
       *
       * @given 直リンクの応答が5回続けて302で転送され、6回目の応答が200で本文が1バイトである
       * @when download を呼び出す
       * @then 本文を保持したファイルを返す
       */
      @Test
      void 転送がちょうど5回の場合_最後の応答の本文を保持したファイルを返す() throws Exception {
        HttpClient client =
            clientReturning(
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ),
                response(200, new byte[] {1}, Map.of()));
        ExternalFileLink link = downloadableLink(client);

        link.download();

        assertThat(link.getFileData()).containsExactly(1);
      }

      /**
       * 転送が6回目も転送の場合に、例外を投げることを確かめる。 転送が止まらない応答で接続を際限なく続けないことを守る。
       *
       * @given 直リンクの応答が6回続けて302で転送される
       * @when download を呼び出す
       * @then IOException を投げ、6回目の要求までを送信する
       */
      @Test
      void 転送が6回を超える場合_IOExceptionを投げる() throws Exception {
        HttpClient client =
            clientReturning(
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ),
                redirect(DRIVE_VIEW_XYZ));
        ExternalFileLink link = downloadableLink(client);

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("上限");
        verify(client, times(6)).send(any(), any());
      }

      /**
       * 残り時間が15秒より短い場合に、残りの時間を要求の上限にすることを確かめる。 全体の上限を超えないように、1回目の転送で20秒経過した後の要求の上限を5秒にする。
       *
       * @given 1回目の要求で20秒が経過し、直リンクの応答が302で転送され、転送先の応答が200である
       * @when download を呼び出す
       * @then 1回目の要求の上限は15秒、2回目の要求の上限は残りの5秒である
       */
      @Test
      void 残り時間が15秒より短い場合_残りの時間を上限とした要求を送信する() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(START);
        ApplicationClock.INSTANCE.setClock(movableClock(now));
        HttpClient client =
            clientAdvancing(
                now,
                Duration.ofSeconds(20),
                redirect(DRIVE_VIEW_XYZ),
                response(200, new byte[] {1}, Map.of()));
        ExternalFileLink link = downloadableLink(client);

        link.download();

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(2)).send(captor.capture(), any());
        assertThat(captor.getAllValues())
            .extracting(request -> request.timeout().orElseThrow())
            .containsExactly(Duration.ofSeconds(15), Duration.ofSeconds(5));
      }
    }
  }

  @Nested
  class getRawUrl {

    @Nested
    class 正常系 {

      /**
       * 元の URL を返すことを確かめる。 抽出元の URL をログに残すことを守る。
       *
       * @given 元の URL が DRIVE_VIEW である
       * @when getRawUrl を呼び出す
       * @then DRIVE_VIEW を返す
       */
      @Test
      void 元のURLを保持する場合_元のURLを返す() {
        assertThat(new ExternalFileLink(DRIVE_VIEW).getRawUrl()).isEqualTo(DRIVE_VIEW);
      }
    }
  }

  @Nested
  class getFileName {

    @Nested
    class 正常系 {

      /**
       * 判定前は名前が未設定のため null を返すことを確かめる。 取得していないファイルの名前を返さないことを守る。
       *
       * @given 取得もしていない
       * @when getFileName を呼び出す
       * @then null を返す
       */
      @Test
      void 取得していない場合_nullを返す() {
        assertThat(new ExternalFileLink(DRIVE_VIEW).getFileName()).isNull();
      }
    }
  }

  @Nested
  class getFileData {

    @Nested
    class 正常系 {

      /**
       * 取得前は本文が未設定のため null を返すことを確かめる。 取得していないファイルの本文を返さないことを守る。
       *
       * @given 取得もしていない
       * @when getFileData を呼び出す
       * @then null を返す
       */
      @Test
      void 取得していない場合_nullを返す() {
        assertThat(new ExternalFileLink(DRIVE_VIEW).getFileData()).isNull();
      }
    }
  }

  @Nested
  class getResolvedUrl {

    @Nested
    class 正常系 {

      /**
       * 判定前は解決後の URL が未設定のため null を返すことを確かめる。 解決していない URL を返さないことを守る。
       *
       * @given 判定もしていない
       * @when getResolvedUrl を呼び出す
       * @then null を返す
       */
      @Test
      void 判定していない場合_nullを返す() {
        assertThat(new ExternalFileLink(DRIVE_VIEW).getResolvedUrl()).isNull();
      }
    }
  }

  @Nested
  class getDirectUrl {

    @Nested
    class 正常系 {

      /**
       * 判定前は直リンクが未設定のため null を返すことを確かめる。 解決していない直リンクを返さないことを守る。
       *
       * @given 判定もしていない
       * @when getDirectUrl を呼び出す
       * @then null を返す
       */
      @Test
      void 判定していない場合_nullを返す() {
        assertThat(new ExternalFileLink(DRIVE_VIEW).getDirectUrl()).isNull();
      }
    }
  }

  @Nested
  class convertToDirectUrl {

    @Nested
    class 正常系 {

      /**
       * Google ドライブのファイルの URL を直リンクに変換することを確かめる。 閲覧用の URL のままでは取得できないため、ダウンロード用に変換することを守る。
       *
       * @given URL が Google ドライブのファイルの閲覧 URL である
       * @when convertToDirectUrl を呼び出す
       * @then ダウンロード用の直リンクを返す
       */
      @Test
      void Googleドライブのファイルの場合_ダウンロード用の直リンクを返す() {
        assertThat(new ExternalFileLink(DRIVE_VIEW).convertToDirectUrl(DRIVE_VIEW))
            .isEqualTo(DRIVE_DIRECT);
      }

      /**
       * Google スプレッドシートの URL を xlsx の書き出し URL に変換することを確かめる。 表計算の内容を Excel 形式で取得することを守る。
       *
       * @given URL が Google スプレッドシートの URL である
       * @when convertToDirectUrl を呼び出す
       * @then xlsx の書き出し URL を返す
       */
      @Test
      void Googleスプレッドシートの場合_xlsxの書き出しURLを返す() {
        assertThat(
                new ExternalFileLink("x")
                    .convertToDirectUrl("https://docs.google.com/spreadsheets/d/ID/edit"))
            .isEqualTo("https://docs.google.com/spreadsheets/d/ID/export?format=xlsx");
      }

      /**
       * Google ドキュメントの URL を docx の書き出し URL に変換することを確かめる。 文書の内容を Word 形式で取得することを守る。
       *
       * @given URL が Google ドキュメントの URL である
       * @when convertToDirectUrl を呼び出す
       * @then docx の書き出し URL を返す
       */
      @Test
      void Googleドキュメントの場合_docxの書き出しURLを返す() {
        assertThat(
                new ExternalFileLink("x")
                    .convertToDirectUrl("https://docs.google.com/document/d/ID/edit"))
            .isEqualTo("https://docs.google.com/document/d/ID/export?format=docx");
      }

      /**
       * Google スライドの URL を pdf の書き出し URL に変換することを確かめる。 スライドの内容を PDF 形式で取得することを守る。
       *
       * @given URL が Google スライドの URL である
       * @when convertToDirectUrl を呼び出す
       * @then pdf の書き出し URL を返す
       */
      @Test
      void Googleスライドの場合_pdfの書き出しURLを返す() {
        assertThat(
                new ExternalFileLink("x")
                    .convertToDirectUrl("https://docs.google.com/presentation/d/ID/edit"))
            .isEqualTo("https://docs.google.com/presentation/d/ID/export?format=pdf");
      }

      /**
       * Dropbox の URL に dl=1 を付けることを確かめる。 Dropbox の共有 URL をダウンロード用に変えることを守る。
       *
       * @given URL が Dropbox の共有 URL で、クエリを持つ
       * @when convertToDirectUrl を呼び出す
       * @then クエリを除いて dl=1 を付けた URL を返す
       */
      @Test
      void Dropboxの場合_dl1を付けた直リンクを返す() {
        assertThat(
                new ExternalFileLink("x")
                    .convertToDirectUrl("https://www.dropbox.com/s/abc/file.pdf?dl=0"))
            .isEqualTo("https://www.dropbox.com/s/abc/file.pdf?dl=1");
      }

      /**
       * SharePoint の URL にダウンロード指定を付けることを確かめる。 既にクエリがある URL では & で連結することを確かめることを守る。
       *
       * @given URL が SharePoint の URL で、クエリを持つ
       * @when convertToDirectUrl を呼び出す
       * @then download=1 を & で連結した URL を返す
       */
      @Test
      void SharePointでクエリがある場合_downloadを連結した直リンクを返す() {
        assertThat(new ExternalFileLink("x").convertToDirectUrl("https://a.sharepoint.com/f?x=1"))
            .isEqualTo("https://a.sharepoint.com/f?x=1&download=1");
      }

      /**
       * 対応外の URL は元の URL を返すことを確かめる。 対応外の URL を勝手に変換しないことを守る。
       *
       * @given URL が対応外の example.com の URL である
       * @when convertToDirectUrl を呼び出す
       * @then 元の URL をそのまま返す
       */
      @Test
      void 対応外の場合_元のURLを返す() {
        assertThat(new ExternalFileLink("x").convertToDirectUrl("https://example.com/a"))
            .isEqualTo("https://example.com/a");
      }

      /**
       * Dropbox の scl 形式の URL にダウンロード指定を付けることを確かめる。 新しい形式の Dropbox の共有 URL もダウンロード用に変換することを守る。
       *
       * @given URL が Dropbox の scl 形式の URL である
       * @when convertToDirectUrl を呼び出す
       * @then クエリを除いて dl=1 を付けた URL を返す
       */
      @Test
      void Dropboxのscl形式の場合_dl1を付けた直リンクを返す() throws Exception {
        assertThat(new ExternalFileLink("x").convertToDirectUrl("https://www.dropbox.com/scl/fi/x"))
            .isEqualTo("https://www.dropbox.com/scl/fi/x?dl=1");
      }

      /**
       * 1drv.ms の URL にダウンロード指定を付けることを確かめる。 短縮形式の OneDrive の URL もダウンロード用に変換することを守る。
       *
       * @given URL が 1drv.ms の URL である
       * @when convertToDirectUrl を呼び出す
       * @then download=1 を付けた URL を返す
       */
      @Test
      void OneDrive短縮URLの場合_downloadを付けた直リンクを返す() throws Exception {
        assertThat(new ExternalFileLink("x").convertToDirectUrl("https://1drv.ms/x"))
            .isEqualTo("https://1drv.ms/x?download=1");
      }

      /**
       * 変換済みの URL をもう一度変換しても同じ直リンクになることを確かめる。 転送のたびに転送先へ変換を適用するため、結果が変わらないことを守る。
       *
       * @given URL が ValueSource の各行の値である
       * @when convertToDirectUrl の結果をもう一度 convertToDirectUrl に渡す
       * @then 1回目の結果と同じ直リンクを返す
       */
      @ParameterizedTest
      @ValueSource(
          strings = {
            "https://drive.google.com/file/d/ABC/view",
            "https://docs.google.com/spreadsheets/d/ID/edit",
            "https://docs.google.com/document/d/ID/edit",
            "https://docs.google.com/presentation/d/ID/edit",
            "https://www.dropbox.com/s/abc/file.pdf?dl=0",
            "https://a.sharepoint.com/f?x=1",
            "https://1drv.ms/x"
          })
      void 変換済みのURLを再度変換した場合_同じ直リンクを返す(final String url) {
        ExternalFileLink link = new ExternalFileLink("x");
        String once = link.convertToDirectUrl(url);

        assertThat(link.convertToDirectUrl(once)).isEqualTo(once);
      }
    }

    @Nested
    class 境界値 {

      /**
       * download 指定が既に付いた SharePoint の URL の場合に、同じ URL を返すことを確かめる。 転送先の URL が download=1
       * を含むと二重に付いて取得できないため、追加しないことを守る。
       *
       * @given URL が download=1 を含む SharePoint の URL である
       * @when convertToDirectUrl を呼び出す
       * @then 元の URL をそのまま返す
       */
      @Test
      void download指定が既にある場合_追加せず元のURLを返す() {
        assertThat(
                new ExternalFileLink("x")
                    .convertToDirectUrl("https://a.sharepoint.com/f?download=1"))
            .isEqualTo("https://a.sharepoint.com/f?download=1");
      }

      /**
       * URL が null の場合に、null を返すことを確かめる。 変換の対象が無いときに例外を投げないことを守る。
       *
       * @given URL が null である
       * @when convertToDirectUrl を呼び出す
       * @then null を返す
       */
      @Test
      void URLがnullの場合_nullを返す() {
        assertThat(new ExternalFileLink("x").convertToDirectUrl(null)).isNull();
      }

      /**
       * ID を含まない Google ドライブの URL の場合に、ID を空にした直リンクを返すことを確かめる。 ID が抽出できないときも例外にせず、そのまま変換を終えることを守る。
       *
       * @given URL が ID を含まない drive.google.com/file/d/ の URL である
       * @when convertToDirectUrl を呼び出す
       * @then id が空の直リンクを返す
       */
      @Test
      void IDを含まないGoogleドライブの場合_IDを空にした直リンクを返す() {
        assertThat(
                new ExternalFileLink("x")
                    .convertToDirectUrl("https://drive.google.com/file/d/?a=1"))
            .isEqualTo("https://drive.google.com/uc?export=download&id=");
      }

      /**
       * SharePoint の URL にクエリが無い場合に、? で download=1 を付けることを確かめる。 クエリの区切りを付けることを守る。
       *
       * @given URL が SharePoint の URL で、クエリを持たない
       * @when convertToDirectUrl を呼び出す
       * @then ? で download=1 を付けた URL を返す
       */
      @Test
      void SharePointでクエリが無い場合_区切りの問号を付けたdownload指定の直リンクを返す() {
        assertThat(new ExternalFileLink("x").convertToDirectUrl("https://a.sharepoint.com/f"))
            .isEqualTo("https://a.sharepoint.com/f?download=1");
      }
    }
  }

  @Nested
  class isSupportedDomain {

    @Nested
    class 正常系 {

      /**
       * 対応するストレージのドメインの場合に、対応と判定することを確かめる。 対応する共有リンクだけを取り込むことを守る。
       *
       * @given URL が Google ドライブの URL である
       * @when isSupportedDomain を呼び出す
       * @then true を返す
       */
      @Test
      void 対応するドメインの場合_trueを返す() {
        assertThat(new ExternalFileLink("x").isSupportedDomain(DRIVE_VIEW)).isTrue();
      }

      /**
       * 対応するドメインのサブドメインの場合に、対応と判定することを確かめる。 サブドメインの共有リンクも取り込めるようにし、従来の対象ドメインの意図に合わせることを守る。
       *
       * @given URL のホストが x.drive.google.com である
       * @when isSupportedDomain を呼び出す
       * @then true を返す
       */
      @Test
      void サブドメインの場合_trueを返す() {
        assertThat(new ExternalFileLink("x").isSupportedDomain("https://x.drive.google.com/a"))
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対応外のドメインの場合に、対応外と判定することを確かめる。 対応外の URL を取り込まないことを守る。
       *
       * @given URL が example.com の URL である
       * @when isSupportedDomain を呼び出す
       * @then false を返す
       */
      @Test
      void 対応外のドメインの場合_falseを返す() {
        assertThat(new ExternalFileLink("x").isSupportedDomain("https://example.com/")).isFalse();
      }

      /**
       * URL が null の場合に、対応外と判定することを確かめる。 判定の対象が無いときに例外を投げないことを守る。
       *
       * @given URL が null である
       * @when isSupportedDomain を呼び出す
       * @then false を返す
       */
      @Test
      void URLがnullの場合_falseを返す() {
        assertThat(new ExternalFileLink("x").isSupportedDomain(null)).isFalse();
      }

      /**
       * 各ストレージのドメインを順に判定することを確かめる。 対応するドメインを取りこぼさないことを守る。
       *
       * @given URL が docs.google.com、dropbox.com、1drv.ms、sharepoint.com のいずれかである
       * @when isSupportedDomain を呼び出す
       * @then いずれも true を返す
       */
      @Test
      void 他の対応ドメインの場合_trueを返す() {
        ExternalFileLink link = new ExternalFileLink("x");

        assertThat(link.isSupportedDomain("https://docs.google.com/a")).isTrue();
        assertThat(link.isSupportedDomain("https://www.dropbox.com/a")).isTrue();
        assertThat(link.isSupportedDomain("https://1drv.ms/a")).isTrue();
        assertThat(link.isSupportedDomain("https://a.sharepoint.com/a")).isTrue();
      }

      /**
       * ドメイン名の一部だけが一致する場合に、対応外と判定することを確かめる。 別のホストの名前に対応ドメインが含まれるだけでは取り込まないことを守る。
       *
       * @given URL のホストが evilsharepoint.com で、対応ドメインの接尾辞の前に区切りが無い
       * @when isSupportedDomain を呼び出す
       * @then false を返す
       */
      @Test
      void ドメインの一部だけが一致する場合_falseを返す() {
        assertThat(new ExternalFileLink("x").isSupportedDomain("https://evilsharepoint.com/x"))
            .isFalse();
        assertThat(
                new ExternalFileLink("x").isSupportedDomain("https://example.com/drive.google.com"))
            .isFalse();
      }

      /**
       * ホストを持たない URL の場合に、対応外と判定することを確かめる。 ホストを判定できない URL で例外を投げず、対応外として扱うことを守る。
       *
       * @given URL が mailto の形式でホストを持たない
       * @when isSupportedDomain を呼び出す
       * @then false を返す
       */
      @Test
      void ホストを持たないURLの場合_falseを返す() {
        assertThat(new ExternalFileLink("x").isSupportedDomain("mailto:someone@example.com"))
            .isFalse();
      }

      /**
       * URL が解析できない場合に、対応外と判定することを確かめる。 解析できない URL で例外を投げず、対応外として扱うことを守る。
       *
       * @given URL に空白を含み、解析できない
       * @when isSupportedDomain を呼び出す
       * @then false を返す
       */
      @Test
      void URLが解析できない場合_falseを返す() {
        assertThat(new ExternalFileLink("x").isSupportedDomain("https://a b/c")).isFalse();
      }

      /**
       * ユーザー情報を含む URL の場合に、実際のホストで判定して対応外にすることを確かめる。 ドメインらしい文字列を前に付けた偽のサイトへ接続させないことを守る。
       *
       * @given URL が https://drive.google.com@evil.example/ である
       * @when isSupportedDomain を呼び出す
       * @then false を返す
       */
      @Test
      void ユーザー情報を含むURLの場合_falseを返す() {
        assertThat(
                new ExternalFileLink("x")
                    .isSupportedDomain("https://drive.google.com@evil.example/"))
            .isFalse();
      }

      /**
       * ホストが大文字の URL の場合に、対応ドメインとして判定することを確かめる。 ホスト名は大文字と小文字を区別しないため、共有リンクを取りこぼさないことを守る。
       *
       * @given URL が HTTPS://DRIVE.GOOGLE.COM/file/d/ABC/view である
       * @when isSupportedDomain を呼び出す
       * @then true を返す
       */
      @Test
      void ホストが大文字の場合_trueを返す() {
        assertThat(
                new ExternalFileLink("x")
                    .isSupportedDomain("HTTPS://DRIVE.GOOGLE.COM/file/d/ABC/view"))
            .isTrue();
      }

      /**
       * ホストの末尾にドットがある URL の場合に、対応外として判定することを確かめる。 一覧と完全一致しない表記は安全側に倒して対応外として扱うことを守る。
       *
       * @given URL が https://drive.google.com./x である
       * @when isSupportedDomain を呼び出す
       * @then false を返す
       */
      @Test
      void ホストの末尾にドットがある場合_falseを返す() {
        assertThat(new ExternalFileLink("x").isSupportedDomain("https://drive.google.com./x"))
            .isFalse();
      }
    }
  }

  @Nested
  class extractFileName {

    @Nested
    class 正常系 {

      /**
       * RFC 5987 の filename* がある場合に、その名前を復号して返すことを確かめる。 日本語のファイル名を取り出すことを守る。
       *
       * @given Content-Disposition に filename*=UTF-8''%E8%A3%9C.pdf がある
       * @when extractFileName を呼び出す
       * @then 復号した補.pdf を返す
       */
      @Test
      void filenameの拡張指定がある場合_復号した名前を返す() {
        HttpResponse<byte[]> response =
            bytesResponse(
                200,
                DRIVE_DIRECT,
                new byte[0],
                Map.of(
                    "Content-Disposition", List.of("attachment; filename*=UTF-8''%E8%A3%9C.pdf")));

        assertThat(new ExternalFileLink("x").extractFileName(response, DRIVE_DIRECT))
            .isEqualTo("補.pdf");
      }

      /**
       * 通常の filename がある場合に、その名前を返すことを確かめる。 拡張指定が無い応答でもファイル名を取り出すことを守る。
       *
       * @given Content-Disposition に filename="a b.xlsx" がある
       * @when extractFileName を呼び出す
       * @then a b.xlsx を返す
       */
      @Test
      void filenameの指定がある場合_その名前を返す() {
        HttpResponse<byte[]> response =
            bytesResponse(
                200,
                DRIVE_DIRECT,
                new byte[0],
                Map.of("Content-Disposition", List.of("attachment; filename=\"a b.xlsx\"")));

        assertThat(new ExternalFileLink("x").extractFileName(response, DRIVE_DIRECT))
            .isEqualTo("a b.xlsx");
      }

      /**
       * Content-Disposition が無い場合に、URL の末尾の名前を返すことを確かめる。 ヘッダーが無くても URL からファイル名を得られるようにすることを守る。
       *
       * @given Content-Disposition が無く、URL の末尾が report.docx である
       * @when extractFileName を呼び出す
       * @then report.docx を返す
       */
      @Test
      void Content_Dispositionが無い場合_URLの末尾の名前を返す() {
        HttpResponse<byte[]> response =
            bytesResponse(200, "https://a.sharepoint.com/x/report.docx", new byte[0], Map.of());

        assertThat(
                new ExternalFileLink("x")
                    .extractFileName(response, "https://a.sharepoint.com/x/report.docx"))
            .isEqualTo("report.docx");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 名前が空白だけの filename の場合に、URL の末尾の名前を返すことを確かめる。 空白だけの名前を保存しないことを守る。
       *
       * @given Content-Disposition に filename=" " があり、URL の末尾が file.txt である
       * @when extractFileName を呼び出す
       * @then file.txt を返す
       */
      @Test
      void filenameが空白だけの場合_URLの末尾の名前を返す() {
        HttpResponse<byte[]> response =
            bytesResponse(
                200,
                "https://a.sharepoint.com/file.txt",
                new byte[0],
                Map.of("Content-Disposition", List.of("attachment; filename=\" \"")));

        assertThat(
                new ExternalFileLink("x")
                    .extractFileName(response, "https://a.sharepoint.com/file.txt"))
            .isEqualTo("file.txt");
      }

      /**
       * URL の末尾が空の場合に、タイムスタンプ付きの既定の名前を返すことを確かめる。 名前が決められないときも、保存先を一意にできる名前を返すことを守る。
       *
       * @given Content-Disposition が無く、URL の末尾が / で終わる
       * @when extractFileName を呼び出す
       * @then skillsheet_ で始まる名前を返す
       */
      @Test
      void URLの末尾が空の場合_タイムスタンプ付きの既定の名前を返す() {
        HttpResponse<byte[]> response =
            bytesResponse(200, "https://a.sharepoint.com/dir/", new byte[0], Map.of());

        assertThat(
                new ExternalFileLink("x")
                    .extractFileName(response, "https://a.sharepoint.com/dir/"))
            .startsWith("skillsheet_");
      }

      /**
       * URL が解析できない場合に、タイムスタンプ付きの既定の名前を返すことを確かめる。 解析できない URL で例外を投げないことを守る。
       *
       * @given Content-Disposition が無く、URL に空白が含まれて解析できない
       * @when extractFileName を呼び出す
       * @then skillsheet_ で始まる名前を返す
       */
      @Test
      void URLが解析できない場合_タイムスタンプ付きの既定の名前を返す() {
        HttpResponse<byte[]> response = bytesResponse(200, "x", new byte[0], Map.of());

        assertThat(new ExternalFileLink("x").extractFileName(response, "https://a b/c"))
            .startsWith("skillsheet_");
      }

      /**
       * Content-Disposition に filename が無い場合に、URL の末尾の名前を返すことを確かめる。 拡張の名前が無い応答でも URL
       * からファイル名を得られることを守る。
       *
       * @given Content-Disposition が inline だけで、URL の末尾が doc.pdf である
       * @when extractFileName を呼び出す
       * @then doc.pdf を返す
       */
      @Test
      void filenameの指定が無い場合_URLの末尾の名前を返す() throws Exception {
        HttpResponse<byte[]> response =
            bytesResponse(
                200,
                "https://a.sharepoint.com/y/doc.pdf",
                new byte[0],
                Map.of("Content-Disposition", List.of("inline")));

        assertThat(
                new ExternalFileLink("x")
                    .extractFileName(response, "https://a.sharepoint.com/y/doc.pdf"))
            .isEqualTo("doc.pdf");
      }
    }
  }

  @Nested
  class extractId {

    @Nested
    class 境界値 {

      /**
       * 正規表現に一致しない場合に、空文字を返すことを確かめる。 ID が無いときに例外にせず、空の ID として扱うことを守る。
       *
       * @given URL が ID を含まず、正規表現が ID の部分を要求する
       * @when extractId を呼び出す
       * @then 空文字を返す
       */
      @Test
      void 正規表現に一致しない場合_空文字を返す() {
        assertThat(
                new ExternalFileLink("x").extractId("https://a.example/none", "/file/d/([^/?]+)"))
            .isEmpty();
      }
    }
  }

  /**
   * 指定の応答を送信のたびに順に返す HttpClient のモックを作る.
   *
   * @param responses 送信のたびに順に返す応答
   * @return HttpClient のモック
   * @throws Exception 送信のスタブに失敗した場合
   */
  private static HttpClient clientReturning(final HttpResponse<?>... responses) throws Exception {
    HttpClient client = mock(HttpClient.class);
    HttpResponse<?> first = responses[0];
    HttpResponse<?>[] rest = Arrays.copyOfRange(responses, 1, responses.length);
    doReturn(first, (Object[]) rest).when(client).send(any(HttpRequest.class), any());
    return client;
  }

  /**
   * 送信のたびに時計を指定の時間だけ進め、順に応答を返す HttpClient のモックを作る.
   *
   * @param now 時計の現在時刻を保持する参照
   * @param step 送信のたびに進める時間
   * @param responses 送信のたびに順に返す応答
   * @return HttpClient のモック
   * @throws Exception 送信のスタブに失敗した場合
   */
  private static HttpClient clientAdvancing(
      final AtomicReference<Instant> now, final Duration step, final HttpResponse<?>... responses)
      throws Exception {
    HttpClient client = mock(HttpClient.class);
    AtomicInteger index = new AtomicInteger();
    when(client.send(any(HttpRequest.class), any()))
        .thenAnswer(
            invocation -> {
              now.updateAndGet(current -> current.plus(step));
              return responses[index.getAndIncrement()];
            });
    return client;
  }

  /**
   * 現在時刻を参照の値で返す時計のモックを作る.
   *
   * @param now 現在時刻を保持する参照
   * @return 時計のモック
   */
  private static Clock movableClock(final AtomicReference<Instant> now) {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenAnswer(invocation -> now.get());
    return clock;
  }

  /**
   * 直リンクを解決済みの ExternalFileLink を作る.
   *
   * @param client 送信の応答を返す HttpClient
   * @return ExternalFileLink
   */
  private static ExternalFileLink downloadableLink(final HttpClient client) {
    ExternalFileLink link = new ExternalFileLink(DRIVE_VIEW, client);
    link.isDownloadable();
    return link;
  }

  /**
   * ホストを指定のアドレスへ解決させる.
   *
   * @param dns 名前解決の静的モック
   * @param host 解決させるホスト
   * @param address 解決先のアドレスの各バイト
   * @throws UnknownHostException 名前解決のスタブに失敗した場合
   */
  private static void resolveTo(
      final MockedStatic<InetAddress> dns, final String host, final byte[] address)
      throws UnknownHostException {
    // 解決結果は静的モックのスタブの外で作る。スタブの途中で別の静的呼び出しをすると Mockito の記録が壊れるため
    InetAddress[] resolved = {InetAddress.getByAddress(address)};
    dns.when(() -> InetAddress.getAllByName(host)).thenReturn(resolved);
  }

  /**
   * 転送を表す応答を作る.
   *
   * @param location 転送先の URL
   * @return ステータス302の応答
   */
  private static HttpResponse<byte[]> redirect(final String location) {
    return response(302, null, Map.of("Location", List.of(location)));
  }

  /**
   * 最終 URL を持つ GET の応答を作る.
   *
   * @param status ステータス
   * @param uri 最終 URL
   * @param body 本文
   * @param headers ヘッダー
   * @return 応答
   */
  private static HttpResponse<byte[]> bytesResponse(
      final int status,
      final String uri,
      final byte[] body,
      final Map<String, List<String>> headers) {
    HttpResponse<byte[]> response = response(status, body, headers);
    when(response.uri()).thenReturn(URI.create(uri));
    return response;
  }

  /**
   * GET の応答を作る.
   *
   * @param status ステータス
   * @param body 本文
   * @param headers ヘッダー
   * @return 応答
   */
  @SuppressWarnings("unchecked")
  private static HttpResponse<byte[]> response(
      final int status, final byte[] body, final Map<String, List<String>> headers) {
    HttpResponse<byte[]> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn(body);
    when(response.headers()).thenReturn(HttpHeaders.of(headers, (a, b) -> true));
    return response;
  }

  /**
   * 名前解決に指定の時間を要するホストのスタブを作る。解決のたびに時計を指定の時間だけ進める。
   *
   * @param dns 名前解決の静的モック
   * @param host 解決させるホスト
   * @param now 時計の現在時刻を保持する参照
   * @param delay 解決のたびに進める時間
   * @throws UnknownHostException 名前解決のスタブに失敗した場合
   */
  private static void resolveAfter(
      final MockedStatic<InetAddress> dns,
      final String host,
      final AtomicReference<Instant> now,
      final Duration delay)
      throws UnknownHostException {
    // 解決結果は静的モックのスタブの外で作る。スタブの途中で別の静的呼び出しをすると Mockito の記録が壊れるため
    InetAddress[] resolved = {InetAddress.getByAddress(PUBLIC_ADDRESS)};
    dns.when(() -> InetAddress.getAllByName(host))
        .thenAnswer(
            invocation -> {
              now.updateAndGet(current -> current.plus(delay));
              return resolved;
            });
  }

  /**
   * ステータスだけを持つ応答の情報を作る.
   *
   * @param status ステータスコード
   * @return 応答の情報のモック
   */
  private static HttpResponse.ResponseInfo responseInfo(final int status) {
    HttpResponse.ResponseInfo info = mock(HttpResponse.ResponseInfo.class);
    when(info.statusCode()).thenReturn(status);
    return info;
  }

  /**
   * 送信に渡された本文の読み込み方で、指定のステータスの応答の本文を読み込む.
   *
   * @param handler 送信に渡された本文の読み込み方
   * @param status 応答のステータスコード
   * @param data 受信した本文
   * @return 読み込んだ本文
   * @throws Exception 読み込みに失敗した場合
   */
  private static byte[] bodyOf(
      final HttpResponse.BodyHandler<?> handler, final int status, final byte[] data)
      throws Exception {
    HttpResponse.BodySubscriber<?> subscriber = handler.apply(responseInfo(status));
    subscriber.onSubscribe(mock(Flow.Subscription.class));
    subscriber.onNext(List.of(ByteBuffer.wrap(data)));
    subscriber.onComplete();
    return (byte[]) subscriber.getBody().toCompletableFuture().get();
  }
}
