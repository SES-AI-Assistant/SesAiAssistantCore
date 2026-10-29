package copel.sesproductpackage.core.api.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/** {@link PublicHttpsUri} の単体テスト。 */
class PublicHttpsUriTest {

  @Nested
  class isHttpsWithHost {

    @Nested
    class 正常系 {

      /**
       * https でホストを持つ URL の場合に、判定の対象になることを確かめる。 接続の前に形式を満たす URL だけを先へ進めることを守る。
       *
       * @given URL が "https://8.8.8.8/" である
       * @when isHttpsWithHost を呼び出す
       * @then true を返す
       */
      @Test
      void httpsでホストを持つ場合_trueを返す() {
        assertThat(new PublicHttpsUri("https://8.8.8.8/").isHttpsWithHost()).isTrue();
      }

      /**
       * URI から生成した場合に、文字列から生成した場合と同じ判定になることを確かめる。 両方の生成経路で接続先の判定を揃えることを守る。
       *
       * @given URI が https://8.8.8.8/ である
       * @when isHttpsWithHost を呼び出す
       * @then true を返す
       */
      @Test
      void URIを指定した場合_trueを返す() {
        assertThat(new PublicHttpsUri(URI.create("https://8.8.8.8/")).isHttpsWithHost()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * スキームが http の場合に、判定の対象にならないことを確かめる。 平文の通信で取得すると内容を途中で読み替えられるため、https だけを許可する。
       *
       * @given URL が "http://8.8.8.8/" である
       * @when isHttpsWithHost を呼び出す
       * @then false を返す
       */
      @Test
      void スキームがhttpの場合_falseを返す() {
        assertThat(new PublicHttpsUri("http://8.8.8.8/").isHttpsWithHost()).isFalse();
      }

      /**
       * ホストが無い場合に、判定の対象にならないことを確かめる。 接続先が決まらない URL を接続の前に外すことを守る。
       *
       * @given URL が "https:///path" である
       * @when isHttpsWithHost を呼び出す
       * @then false を返す
       */
      @Test
      void ホストが無い場合_falseを返す() {
        assertThat(new PublicHttpsUri("https:///path").isHttpsWithHost()).isFalse();
      }

      /**
       * URL が null の場合に、判定の対象にならないことを確かめる。 値が無い状態で接続先を決めないことを守る。
       *
       * @given URL が null である
       * @when isHttpsWithHost を呼び出す
       * @then false を返す
       */
      @Test
      void URLがnullの場合_falseを返す() {
        assertThat(new PublicHttpsUri((String) null).isHttpsWithHost()).isFalse();
      }
    }
  }

  @Nested
  class isAllowed {

    @Nested
    class 正常系 {

      /**
       * 公開 IPv4 アドレスのリテラルの場合に、取得先として許可することを確かめる。 IP を直接指定した URL でも公開アドレスなら取得できることを守る。
       *
       * @given URL が "https://8.8.8.8/" である
       * @when isAllowed を呼び出す
       * @then true を返す
       */
      @Test
      void 公開IPv4アドレスの場合_trueを返す() {
        assertThat(new PublicHttpsUri("https://8.8.8.8/").isAllowed()).isTrue();
      }

      /**
       * 公開 IPv6 アドレスのリテラルの場合に、取得先として許可することを確かめる。 IPv6 の公開アドレスも IPv4 と同じく取得できることを守る。
       *
       * @given URL が {@code "https://[2001:4860:4860::8888]/"} である
       * @when isAllowed を呼び出す
       * @then true を返す
       */
      @Test
      void 公開IPv6アドレスの場合_trueを返す() {
        assertThat(new PublicHttpsUri("https://[2001:4860:4860::8888]/").isAllowed()).isTrue();
      }

      /**
       * ホストを解決した全てが公開アドレスの場合に、取得先として許可することを確かめる。 複数のアドレスを返すサービスを取りこぼさないことを守る。
       *
       * @given ホスト "public.example" が 8.8.8.8 と 1.1.1.1 に解決される
       * @when isAllowed を呼び出す
       * @then true を返す
       */
      @Test
      void 解決先の全てが公開アドレスの場合_trueを返す() throws Exception {
        InetAddress[] resolved = {
          InetAddress.getByAddress(new byte[] {8, 8, 8, 8}),
          InetAddress.getByAddress(new byte[] {1, 1, 1, 1})
        };
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class, CALLS_REAL_METHODS)) {
          dns.when(() -> InetAddress.getAllByName("public.example")).thenReturn(resolved);

          assertThat(new PublicHttpsUri("https://public.example/").isAllowed()).isTrue();
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * ホストを解決できない場合に、取得先として許可しないことを確かめる。 接続先を確かめられない URL へ接続しないことを守る。
       *
       * @given ホスト "unknown.example" の名前解決が UnknownHostException を投げる
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @Test
      void ホストを解決できない場合_falseを返す() throws Exception {
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class, CALLS_REAL_METHODS)) {
          dns.when(() -> InetAddress.getAllByName("unknown.example"))
              .thenThrow(new UnknownHostException("解決失敗"));

          assertThat(new PublicHttpsUri("https://unknown.example/").isAllowed()).isFalse();
        }
      }

      /**
       * 解決先の一部でも内部アドレスの場合に、取得先として許可しないことを確かめる。 公開と内部の両方に解決されると、内部へ接続され得るため全てを検査することを守る。
       *
       * @given ホスト "mixed.example" が 8.8.8.8 と 10.0.0.1 に解決される
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @Test
      void 解決先に内部アドレスが1つでもある場合_falseを返す() throws Exception {
        InetAddress[] resolved = {
          InetAddress.getByAddress(new byte[] {8, 8, 8, 8}),
          InetAddress.getByAddress(new byte[] {10, 0, 0, 1})
        };
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class, CALLS_REAL_METHODS)) {
          dns.when(() -> InetAddress.getAllByName("mixed.example")).thenReturn(resolved);

          assertThat(new PublicHttpsUri("https://mixed.example/").isAllowed()).isFalse();
        }
      }

      /**
       * URL が解析できない場合に、取得先として許可しないことを確かめる。 利用者が書いた不正な URL で例外を投げて処理を止めないことを守る。
       *
       * @given URL が空白を含む "https://exa mple.com/" である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @Test
      void URLが解析できない場合_falseを返す() {
        assertThat(new PublicHttpsUri("https://exa mple.com/").isAllowed()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ループバックアドレスの場合に、取得先として許可しないことを確かめる。 自分自身の内部サービスへ接続させないことを守る。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"https://127.0.0.1/", "https://[::1]/"})
      void ループバックの場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * リンクローカルアドレスの場合に、取得先として許可しないことを確かめる。 169.254.169.254 のメタデータサービスへ接続させないことを守る。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"https://169.254.169.254/", "https://[fe80::1]/"})
      void リンクローカルの場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * サイトローカルアドレスの範囲の境界値で、取得先として許可しないことを確かめる。 10/8、172.16/12、192.168/16 の内部網を接続させないことを守る。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(
          strings = {
            "https://10.0.0.1/",
            "https://172.16.0.1/",
            "https://172.31.255.255/",
            "https://192.168.1.1/"
          })
      void サイトローカルの場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * IPv6 のユニークローカルアドレスの場合に、取得先として許可しないことを確かめる。 fc00::/7 は内部網用で、2000::/3 の外として除外することを守る。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"https://[fc00::1]/", "https://[fd00::1]/"})
      void IPv6のユニークローカルの場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * ワイルドカードアドレスの場合に、取得先として許可しないことを確かめる。 0.0.0.0 と :: は接続先として意味を持たないため除外する。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"https://0.0.0.0/", "https://[::]/"})
      void ワイルドカードの場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * マルチキャストアドレスの場合に、取得先として許可しないことを確かめる。 複数の受信者へ送る宛先は単一のファイルの取得先にならないため除外する。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"https://224.0.0.1/", "https://[ff02::1]/"})
      void マルチキャストの場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * 0.0.0.0/8 の予約アドレスの場合に、取得先として許可しないことを確かめる。 この範囲は接続先として使えない予約であるため除外する。
       *
       * @given URL が "https://0.1.2.3/" である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @Test
      void 予約アドレスの場合_falseを返す() {
        assertThat(new PublicHttpsUri("https://0.1.2.3/").isAllowed()).isFalse();
      }

      /**
       * 100.64.0.0/10 の共有アドレスの範囲の境界値で、取得先として許可しないことを確かめる。 事業者内の共有アドレスはインターネットの公開先ではないため除外する。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"https://100.64.0.1/", "https://100.127.255.255/"})
      void CGNATの範囲の場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * 240.0.0.0/4 と限定ブロードキャストの場合に、取得先として許可しないことを確かめる。 将来用の予約アドレスは接続先として使えないため除外する。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"https://240.0.0.1/", "https://255.255.255.255/"})
      void 予約済みの240番台の場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * 198.18.0.0/15 のベンチマーク用アドレスの範囲の境界値で、取得先として許可しないことを確かめる。 性能測定用の予約範囲は公開の接続先ではないため除外する。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"https://198.18.0.1/", "https://198.19.255.255/"})
      void ベンチマーク用の範囲の場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * IPv6 の内部向けアドレスの場合に、取得先として許可しないことを確かめる。 IPv4 へ変換されて内部へ向く形や、6to4 とドキュメント用の範囲を接続させないことを守る。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @ParameterizedTest
      @ValueSource(
          strings = {
            "https://[64:ff9b::a00:1]/",
            "https://[2002:a00:1::1]/",
            "https://[::10.0.0.1]/",
            "https://[::127.0.0.1]/",
            "https://[::ffff:0:10.0.0.1]/",
            "https://[::ffff:10.0.0.1]/",
            "https://[4000::1]/",
            "https://[2001:db8::1]/"
          })
      void IPv6の内部向けの範囲の場合_falseを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isFalse();
      }

      /**
       * 公開アドレスの範囲の境界値で、取得先として許可することを確かめる。 内部の範囲の直前と直後の公開アドレスを取得できることを守り、範囲を広げすぎないことを確かめる。
       *
       * @given URL が ValueSource の各行の値である
       * @when isAllowed を呼び出す
       * @then true を返す
       */
      @ParameterizedTest
      @ValueSource(
          strings = {
            "https://172.32.0.1/",
            "https://100.63.255.255/",
            "https://100.128.0.1/",
            "https://198.20.0.1/",
            "https://11.0.0.1/",
            "https://[2606:4700::1111]/",
            "https://[2001:d00::1]/",
            "https://[2003::1]/"
          })
      void 内部の範囲の直前と直後の公開アドレスの場合_trueを返す(final String url) {
        assertThat(new PublicHttpsUri(url).isAllowed()).isTrue();
      }

      /**
       * 既定以外のポートを指定した場合に、取得先として許可しないことを確かめる。 22 番の別サービスへの接続を許可しないことを守る。
       *
       * @given URL が "https://203.0.113.5:22/" である
       * @when isAllowed を呼び出す
       * @then false を返す
       */
      @Test
      void ポートが443以外の場合_falseを返す() {
        assertThat(new PublicHttpsUri("https://203.0.113.5:22/").isAllowed()).isFalse();
      }

      /**
       * ポートに 443 を明示した場合に、取得先として許可することを確かめる。 https の既定のポートと同じ指定は省略時と同じ扱いにすることを守る。
       *
       * @given URL が "https://203.0.113.5:443/" である
       * @when isAllowed を呼び出す
       * @then true を返す
       */
      @Test
      void ポートが443の場合_trueを返す() {
        assertThat(new PublicHttpsUri("https://203.0.113.5:443/").isAllowed()).isTrue();
      }
    }
  }
}
