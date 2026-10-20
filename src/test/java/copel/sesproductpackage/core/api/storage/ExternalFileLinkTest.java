package copel.sesproductpackage.core.api.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ExternalFileLink} の単体テスト。 */
class ExternalFileLinkTest {

  private static final String DRIVE_VIEW = "https://drive.google.com/file/d/ABC/view";
  private static final String DRIVE_DIRECT = "https://drive.google.com/uc?export=download&id=ABC";

  @Nested
  class コンストラクタ_String {

    @Nested
    class 正常系 {

      /**
       * 元の URL だけを指定した場合に、元の URL を保持することを確かめる。 抽出した URL を後の判定とログの出力に使うため、保持する値を揃えることを守る。
       *
       * @given 元の URL が DRIVE_VIEW である
       * @when ExternalFileLink を生成する
       * @then getRawUrl が元の URL を返す
       */
      @Test
      void 元のURLを指定した場合_元のURLを返す() {
        assertThat(new ExternalFileLink(DRIVE_VIEW).getRawUrl()).isEqualTo(DRIVE_VIEW);
      }
    }
  }

  @Nested
  class コンストラクタ_String_HttpClient {

    @Nested
    class 正常系 {

      /**
       * HttpClient を注入した場合に、その元の URL を保持することを確かめる。 通信を差し替えて判定できるようにするため、注入の経路を守る。
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
       * 対応するストレージの URL の場合に、ダウンロード可能と判定することを確かめる。 対応する共有リンクだけを取り込むため、ドメインの判定結果を返すことを守る。
       *
       * @given HEAD の応答の最終 URL が Google ドライブのファイル URL である
       * @when isDownloadable を呼び出す
       * @then true を返し、直リンクを解決して保持する
       */
      @Test
      void 対応ストレージのURLの場合_trueを返す() throws Exception {
        ExternalFileLink link =
            new ExternalFileLink(DRIVE_VIEW, clientReturning(voidResponse(200, DRIVE_VIEW)));

        assertThat(link.isDownloadable()).isTrue();
        assertThat(link.getDirectUrl()).isEqualTo(DRIVE_DIRECT);
        assertThat(link.getResolvedUrl()).isEqualTo(DRIVE_VIEW);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 通信が失敗した場合に、ダウンロード不可と判定することを確かめる。 通信の失敗で処理を止めず、そのリンクだけを対象外にすることを守る。
       *
       * @given HEAD の送信が IOException を投げる
       * @when isDownloadable を呼び出す
       * @then false を返す
       */
      @Test
      void 通信が失敗した場合_falseを返す() throws Exception {
        HttpClient client = mock(HttpClient.class);
        doThrow(new IOException("接続失敗")).when(client).send(any(HttpRequest.class), any());

        assertThat(new ExternalFileLink(DRIVE_VIEW, client).isDownloadable()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * URL が null の場合に、ダウンロード不可と判定することを確かめる。 通信をせずに判定を終えることを守る。
       *
       * @given 元の URL が null である
       * @when isDownloadable を呼び出す
       * @then false を返す
       */
      @Test
      void URLがnullの場合_falseを返す() throws Exception {
        assertThat(new ExternalFileLink(null, mock(HttpClient.class)).isDownloadable()).isFalse();
      }

      /**
       * URL が空白だけの場合に、ダウンロード不可と判定することを確かめる。 空白だけの URL で通信を行わないことを守る。
       *
       * @given 元の URL が空白だけである
       * @when isDownloadable を呼び出す
       * @then false を返す
       */
      @Test
      void URLが空白だけの場合_falseを返す() throws Exception {
        assertThat(new ExternalFileLink("  ", mock(HttpClient.class)).isDownloadable()).isFalse();
      }

      /**
       * 対応外のドメインの場合に、ダウンロード不可と判定することを確かめる。 対応外の URL を取り込まないことを守る。
       *
       * @given HEAD の応答の最終 URL が example.com である
       * @when isDownloadable を呼び出す
       * @then false を返す
       */
      @Test
      void 対応外のドメインの場合_falseを返す() throws Exception {
        ExternalFileLink link =
            new ExternalFileLink(
                "https://example.com/x",
                clientReturning(voidResponse(200, "https://example.com/x")));

        assertThat(link.isDownloadable()).isFalse();
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
       * @then 本文と report.pdf を保持する
       */
      @Test
      void 応答が成功した場合_本文と名前を保持したファイルを返す() throws Exception {
        ExternalFileLink link =
            linkWithDirectUrl(
                bytesResponse(
                    200,
                    DRIVE_DIRECT,
                    new byte[] {1, 2, 3},
                    Map.of("Content-Disposition", List.of("attachment; filename=report.pdf"))));

        link.download();

        assertThat(link.getFileData()).containsExactly(1, 2, 3);
        assertThat(link.getFileName()).isEqualTo("report.pdf");
      }

      /**
       * 応答の本文が null の場合に、ファイルの本文を null として保持することを確かめる。 本文が無い応答を例外にせず、空として扱うことを守る。
       *
       * @given 直リンクの応答が200で、本文が null である
       * @when download を呼び出す
       * @then getFileData が null を返す
       */
      @Test
      void 本文がnullの場合_本文がnullのファイルを返す() throws Exception {
        ExternalFileLink link = linkWithDirectUrl(bytesResponse(200, DRIVE_DIRECT, null, Map.of()));

        link.download();

        assertThat(link.getFileData()).isNull();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 直リンクが未設定の場合に、例外を投げることを確かめる。 判定を先に行わずに取得を始めると、誤った URL へ通信することを守る。
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
       * 応答が200以外の場合に、例外を投げることを確かめる。 エラーの応答を本文として保存しないことを守る。
       *
       * @given 直リンクの応答が500である
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 応答が200以外の場合_IOExceptionを投げる() throws Exception {
        ExternalFileLink link =
            linkWithDirectUrl(bytesResponse(500, DRIVE_DIRECT, new byte[0], Map.of()));

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
            linkWithDirectUrl(
                bytesResponse(
                    200,
                    DRIVE_DIRECT,
                    new byte[0],
                    Map.of("Content-Type", List.of("text/html; charset=utf-8"))));

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
        ExternalFileLink link =
            linkWithDirectUrl(bytesResponse(200, DRIVE_DIRECT, large, Map.of()));

        assertThatThrownBy(link::download)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("上限");
      }

      /**
       * 直リンクが空の場合に、例外を投げることを確かめる。 空の直リンクへ通信を行わないことを守る。
       *
       * @given HEAD の応答の最終 URL が空で、直リンクが空文字になる
       * @when download を呼び出す
       * @then IOException を投げる
       */
      @Test
      void 直リンクが空の場合_IOExceptionを投げる() throws Exception {
        ExternalFileLink link =
            new ExternalFileLink(DRIVE_VIEW, clientReturning(voidResponse(200, "")));
        link.isDownloadable();

        assertThatThrownBy(link::download).isInstanceOf(IOException.class);
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
    }

    @Nested
    class 境界値 {

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
   * 指定の応答を返す HttpClient のモックを作る.
   *
   * @param response 応答
   * @return HttpClient のモック
   * @throws Exception 送信のスタブに失敗した場合
   */
  private static HttpClient clientReturning(final HttpResponse<?> response) throws Exception {
    HttpClient client = mock(HttpClient.class);
    doReturn(response).when(client).send(any(HttpRequest.class), any());
    return client;
  }

  /**
   * 直リンクを解決済みの ExternalFileLink を作る.
   *
   * @param response ダウンロード時の応答
   * @return ExternalFileLink
   * @throws Exception 送信のスタブに失敗した場合
   */
  private static ExternalFileLink linkWithDirectUrl(final HttpResponse<byte[]> response)
      throws Exception {
    HttpClient client = mock(HttpClient.class);
    // 1回目は HEAD（直リンクの解決）、2回目は GET（本文の取得）の応答を返す
    doReturn(voidResponse(200, DRIVE_VIEW), response)
        .when(client)
        .send(any(HttpRequest.class), any());
    ExternalFileLink link = new ExternalFileLink(DRIVE_VIEW, client);
    link.isDownloadable();
    return link;
  }

  /**
   * HEAD の応答を作る（本文を持たない）.
   *
   * @param status ステータス
   * @param uri 最終 URL
   * @return 応答
   */
  @SuppressWarnings("unchecked")
  private static HttpResponse<Void> voidResponse(final int status, final String uri) {
    HttpResponse<Void> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.uri()).thenReturn(URI.create(uri));
    when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));
    return response;
  }

  /**
   * GET の応答を作る.
   *
   * @param status ステータス
   * @param uri 最終 URL
   * @param body 本文
   * @param headers ヘッダー
   * @return 応答
   */
  @SuppressWarnings("unchecked")
  private static HttpResponse<byte[]> bytesResponse(
      final int status,
      final String uri,
      final byte[] body,
      final Map<String, List<String>> headers) {
    HttpResponse<byte[]> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.uri()).thenReturn(URI.create(uri));
    when(response.body()).thenReturn(body);
    when(response.headers()).thenReturn(HttpHeaders.of(headers, (a, b) -> true));
    return response;
  }
}
