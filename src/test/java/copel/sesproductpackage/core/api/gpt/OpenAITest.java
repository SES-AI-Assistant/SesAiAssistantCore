package copel.sesproductpackage.core.api.gpt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.base.DynamoDbClientFactory;
import copel.sesproductpackage.core.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/** {@link OpenAI} の単体テスト。 */
class OpenAITest {

  private static final String EMBEDDING_OK = "{\"data\":[{\"embedding\":[0.5,0.25],\"index\":0}]}";
  private static final String EMBEDDING_EMPTY = "{\"data\":[]}";
  private static final String CHAT_OK =
      "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"はい\"}}]}";
  private static final String CHAT_NO_CHOICE = "{\"choices\":[]}";

  private MockedStatic<DynamoDbClientFactory> dynamo;

  /** 使用履歴の DynamoDB 呼び出しを、AWS へ接続しないモックに差し替える. */
  @BeforeEach
  void mockDynamoDb() {
    DynamoDbClient client = mock(DynamoDbClient.class);
    when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
    when(client.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
    when(client.updateItem(any(UpdateItemRequest.class)))
        .thenReturn(UpdateItemResponse.builder().build());
    dynamo = mockStatic(DynamoDbClientFactory.class);
    dynamo.when(DynamoDbClientFactory::create).thenReturn(client);
  }

  /** DynamoDB のモックを閉じる. */
  @AfterEach
  void closeDynamoDb() {
    dynamo.close();
  }

  @Nested
  class コンストラクタ_String {

    @Nested
    class 正常系 {

      /**
       * APIキーだけを指定した場合に、既定のモデルで質問応答を要求することを確かめる。 呼び出し側でモデルを省けるように既定値を使うことを守る。
       *
       * @given APIキーだけがあり、応答は回答 "はい" を返す
       * @when generate を prompt で呼び出す
       * @then 既定のモデル gpt-4o-mini を含む要求を送る
       */
      @Test
      void APIキーのみの場合_既定のモデルを含む要求を送信する() throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try (AutoCloseable opened = open(okChat(body))) {
          new OpenAI("key").generate("質問");

          assertThat(body.toString(StandardCharsets.UTF_8)).contains("\"model\":\"gpt-4o-mini\"");
        }
      }
    }
  }

  @Nested
  class コンストラクタ_String_String {

    @Nested
    class 正常系 {

      /**
       * モデルを指定した場合に、そのモデルで質問応答を要求することを確かめる。 用途に応じたモデルを選べるようにするため、指定の値を使うことを守る。
       *
       * @given APIキーと モデル名 gpt-4 がある
       * @when generate を prompt で呼び出す
       * @then モデル gpt-4 を含む要求を送る
       */
      @Test
      void モデルを指定した場合_そのモデルを含む要求を送信する() throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try (AutoCloseable opened = open(okChat(body))) {
          new OpenAI("key", "gpt-4").generate("質問");

          assertThat(body.toString(StandardCharsets.UTF_8)).contains("\"model\":\"gpt-4\"");
        }
      }
    }
  }

  @Nested
  class embedding {

    @Nested
    class 正常系 {

      /**
       * 文字列を指定した場合に、埋め込みベクトルを返すことを確かめる。 類似検索に使うベクトルを応答から取り出すことを守る。
       *
       * @given 応答の data の埋め込みが 0.5 と 0.25 である
       * @when embedding を "文字列" で呼び出す
       * @then 0.5 と 0.25 を要素に持つベクトルを返す
       */
      @Test
      void 文字列の場合_埋め込みベクトルを返す() throws Exception {
        try (AutoCloseable opened = open(okEmbedding())) {
          assertThat(new OpenAI("key").embedding("文字列")).containsExactly(0.5f, 0.25f);
        }
      }

      /**
       * 応答が503の後に成功する場合に、2回目の結果を返すことを確かめる。 一時的な過負荷は状態コードで判定して再試行し、処理を続けることを守る。
       *
       * @given 1回目の応答が503で、2回目の応答が埋め込み 0.5 と 0.25 である
       * @when embedding を "文字列" で呼び出す
       * @then 0.5 と 0.25 を要素に持つベクトルを返す
       */
      @Test
      void ステータス503が1回発生した後に成功する場合_2回目の結果を返す() throws Exception {
        HttpURLConnection failing = status(503, null, null);
        HttpURLConnection ok = status(200, null, EMBEDDING_OK);
        try (AutoCloseable opened = openSequence(failing, ok)) {
          assertThat(new OpenAI("key").embedding("文字列")).containsExactly(0.5f, 0.25f);
        }
      }

      /**
       * 埋め込みを要求する場合に、入力文字列と float 形式を要求の本文に含めることを確かめる。 埋め込みの要求が指定どおりに組み立てられることを守る。
       *
       * @given 入力が "文字列" である
       * @when embedding を "文字列" で呼び出す
       * @then 要求の本文に入力文字列と float の形式を含む
       */
      @Test
      void 埋め込みを要求する場合_入力文字列とfloat形式を含む要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        try (AutoCloseable opened = open(status(200, null, EMBEDDING_OK, sent))) {
          new OpenAI("key").embedding("文字列");
        }
        assertThat(sent.toString(StandardCharsets.UTF_8))
            .contains("\"input\":\"文字列\"", "\"encoding_format\":\"float\"");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 応答の data が空の場合に、IOException を投げることを確かめる。 埋め込みの無い応答を分かりやすい例外として伝え、呼び出し元の既存の例外処理に合わせることを守る。
       *
       * @given 応答の data が空の配列である
       * @when embedding を "文字列" で呼び出す
       * @then メッセージに data を含む IOException を投げる
       */
      @Test
      void 応答のdataが空の場合_IOExceptionを投げる() throws Exception {
        try (AutoCloseable opened = open(okEmbeddingBody("{\"data\":[]}"))) {
          assertThatThrownBy(() -> new OpenAI("key").embedding("文字列"))
              .isInstanceOf(IOException.class)
              .hasMessageContaining("data");
        }
      }

      /**
       * 応答が400の場合に、ステータスの説明を含む RuntimeException を投げることを確かめる。 誤ったリクエストを再試行せず、原因を呼び出し元へ伝えることを守る。
       *
       * @given 応答のステータスが400で、エラー本文が無い
       * @when embedding を "文字列" で呼び出す
       * @then 400 の説明を含む RuntimeException を投げる
       */
      @Test
      void 応答が400の場合_RuntimeExceptionを投げる() throws Exception {
        try (AutoCloseable opened = open(status(400, null, null))) {
          assertThatThrownBy(() -> new OpenAI("key").embedding("文字列"))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("400 Bad Request");
        }
      }

      /**
       * 送信の入出力が失敗する場合に、IOException を投げることを確かめる。 通信の失敗を実行時の例外に変えずに、入出力の例外として伝えることを守る。
       *
       * @given 接続の出力ストリームの取得が IOException を投げる
       * @when embedding を "文字列" で呼び出す
       * @then IOException を投げる
       */
      @Test
      void 送信の入出力が失敗する場合_IOExceptionを投げる() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getOutputStream()).thenThrow(new IOException("送信失敗"));
        try (AutoCloseable opened = open(conn)) {
          assertThatThrownBy(() -> new OpenAI("key").embedding("文字列"))
              .isInstanceOf(IOException.class);
        }
      }

      /**
       * 応答に data の項目が無い場合に、IOException を投げることを確かめる。 項目の無い応答で例外にせず、呼び出し元が扱う IOException として伝えることを守る。
       *
       * @given 応答に data の項目が無い
       * @when embedding を "文字列" で呼び出す
       * @then IOException を投げる
       */
      @Test
      void 応答のdataが無い場合_IOExceptionを投げる() throws Exception {
        try (AutoCloseable opened = open(okEmbeddingBody("{}"))) {
          assertThatThrownBy(() -> new OpenAI("key").embedding("文字列"))
              .isInstanceOf(IOException.class);
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 入力が null の場合に、null を返すことを確かめる。 入力が無い時に通信を行わないことを守る。
       *
       * @given 入力が null である
       * @when embedding を null で呼び出す
       * @then null を返す
       */
      @Test
      void 入力がnullの場合_nullを返す() throws Exception {
        assertThat(new OpenAI("key").embedding(null)).isNull();
      }
    }
  }

  @Nested
  class generate_String {

    @Nested
    class 正常系 {

      /**
       * 質問を送った場合に、応答の回答を返すことを確かめる。 判定に使う回答を応答の本文から取り出すことを守る。
       *
       * @given 応答の回答が "はい" である
       * @when generate を "質問" で呼び出す
       * @then 回答 "はい" を含む GptAnswer を返す
       */
      @Test
      void 質問がある場合_回答を返す() throws Exception {
        try (AutoCloseable opened = open(okChat(new ByteArrayOutputStream()))) {
          assertThat(new OpenAI("key").generate("質問").getAnswer()).isEqualTo("はい");
        }
      }

      /**
       * ステータスが200以外でも例外にならない場合に、応答の回答を返すことを確かめる。 想定外のステータスは例外にせず、本文の解析に任せる仕様を守る。
       *
       * @given 応答のステータスが302で、回答 "はい" の本文を返す
       * @when generate を "質問" で呼び出す
       * @then 例外を投げずに回答 "はい" を返す
       */
      @Test
      void ステータスが302の場合_例外を投げずに回答を返す() throws Exception {
        try (AutoCloseable opened = open(status(302, null, CHAT_OK))) {
          assertThat(new OpenAI("key").generate("質問").getAnswer()).isEqualTo("はい");
        }
      }

      /**
       * 温度の設定がある場合に、設定値を質問の要求に含めることを確かめる。 温度は設定値で変えられるようにし、要求に確実に反映することを守る。
       *
       * @given 設定 OPEN_AI_COMPLETION_TEMPERATURE が 0.2 である
       * @when generate を "質問" で呼び出す
       * @then 要求の本文に temperature の 0.2 を含む
       */
      @Test
      void 温度の設定がある場合_設定値を含む要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        Properties.putForTest("OPEN_AI_COMPLETION_TEMPERATURE", "0.2");
        try (AutoCloseable opened = open(status(200, null, CHAT_OK, sent))) {
          new OpenAI("key").generate("質問");
        } finally {
          Properties.removeForTest("OPEN_AI_COMPLETION_TEMPERATURE");
        }
        assertThat(sent.toString(StandardCharsets.UTF_8)).contains("\"temperature\":0.2");
      }

      /**
       * 温度の設定が無い場合に、既定の 0.7 を質問の要求に含めることを確かめる。 設定が無くても既定の温度で要求を送ることを守る。
       *
       * @given 設定 OPEN_AI_COMPLETION_TEMPERATURE が無い
       * @when generate を "質問" で呼び出す
       * @then 要求の本文に temperature の 0.7 を含む
       */
      @Test
      void 温度の設定が無い場合_既定の温度を含む要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        Properties.removeForTest("OPEN_AI_COMPLETION_TEMPERATURE");
        try (AutoCloseable opened = open(status(200, null, CHAT_OK, sent))) {
          new OpenAI("key").generate("質問");
        }
        assertThat(sent.toString(StandardCharsets.UTF_8)).contains("\"temperature\":0.7");
      }

      /**
       * APIキーを指定した場合に、Bearer 形式の認証ヘッダーを付けることを確かめる。 認証に使う APIキーが要求に載ることを守る。
       *
       * @given APIキーが "key" である
       * @when generate を "質問" で呼び出す
       * @then 認証ヘッダーに Bearer key を設定する
       */
      @Test
      void APIキーを指定した場合_Bearer形式の認証ヘッダーを送信する() throws Exception {
        HttpURLConnection conn = status(200, null, CHAT_OK);
        try (AutoCloseable opened = open(conn)) {
          new OpenAI("key").generate("質問");
        }
        verify(conn).setRequestProperty("Authorization", "Bearer key");
      }

      /**
       * 質問を送る場合に、要求の本文に質問文とモデル名を含めることを確かめる。 要求の内容が指定どおりに組み立てられることを守る。
       *
       * @given モデル名が gpt-4 で、質問が "質問" である
       * @when generate を "質問" で呼び出す
       * @then 要求の本文に user の役割、質問文、モデル名を含む
       */
      @Test
      void 質問を送る場合_質問文とモデル名を含む要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        try (AutoCloseable opened = open(status(200, null, CHAT_OK, sent))) {
          new OpenAI("key", "gpt-4").generate("質問");
        }
        assertThat(sent.toString(StandardCharsets.UTF_8))
            .contains("\"role\":\"user\"", "\"content\":\"質問\"", "\"model\":\"gpt-4\"");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 応答が401の場合に、APIキーの説明を含む RuntimeException を投げることを確かめる。 認証の失敗を原因付きで伝え、再試行しないことを守る。
       *
       * @given 応答のステータスが401で、エラー本文が JSON の文字列である
       * @when generate を "質問" で呼び出す
       * @then 401 の説明と本文を含む RuntimeException を投げる
       */
      @Test
      void 応答が401の場合_RuntimeExceptionを投げる() throws Exception {
        try (AutoCloseable opened = open(status(401, "{\"error\":\"x\"}", null))) {
          assertThatThrownBy(() -> new OpenAI("key").generate("質問"))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("401 Unauthorized")
              .hasMessageContaining("| OpenAI: {\"error\":\"x\"}");
        }
      }

      /**
       * 応答が各エラーステータスの場合に、そのステータスの説明を含む例外を投げることを確かめる。 ステータスごとの原因を取り違えずに伝えることを守る。
       *
       * @given 応答のステータスが403、404、408、429、500、503 のいずれかで、エラー本文が無い
       * @when generate を "質問" で呼び出す
       * @then 各ステータスの説明を含む RuntimeException を投げる
       */
      @Test
      void 応答が他のエラーステータスの場合_RuntimeExceptionを投げる() throws Exception {
        int[] codes = {400, 403, 404, 408, 429, 500, 503};
        for (int code : codes) {
          try (AutoCloseable opened = open(status(code, null, null))) {
            assertThatThrownBy(() -> new OpenAI("key").generate("質問"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(String.valueOf(code));
          }
        }
      }

      /**
       * エラー本文の読み取りが失敗する場合に、要約だけの RuntimeException を投げることを確かめる。 本文が読めなくても、エラーの原因を伝えることを守る。
       *
       * @given 応答のステータスが500で、エラー本文の取得が IOException を投げる
       * @when generate を "質問" で呼び出す
       * @then 500 の説明だけを含む RuntimeException を投げる
       */
      @Test
      void エラー本文の読み取りが失敗する場合_RuntimeExceptionを投げる() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getResponseCode()).thenReturn(500);
        when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(conn.getErrorStream())
            .thenReturn(
                new InputStream() {
                  @Override
                  public int read() throws IOException {
                    throw new IOException("読み取り失敗");
                  }
                });
        try (AutoCloseable opened = open(conn)) {
          assertThatThrownBy(() -> new OpenAI("key").generate("質問"))
              .isInstanceOf(RuntimeException.class)
              .hasMessage("500 Internal Server Error: OpenAIのサーバーで問題が発生しました");
        }
      }

      /**
       * エラー本文が空白だけの場合に、要約だけの RuntimeException を投げることを確かめる。 空の本文を区切り文字と一緒に出さないことを守る。
       *
       * @given 応答のステータスが500で、エラー本文が空白だけである
       * @when generate を "質問" で呼び出す
       * @then 500 の説明だけを含む RuntimeException を投げる
       */
      @Test
      void エラー本文が空白だけの場合_RuntimeExceptionを投げる() throws Exception {
        try (AutoCloseable opened = open(status(500, "  ", null))) {
          assertThatThrownBy(() -> new OpenAI("key").generate("質問"))
              .isInstanceOf(RuntimeException.class)
              .hasMessage("500 Internal Server Error: OpenAIのサーバーで問題が発生しました");
        }
      }

      /**
       * 応答の本文の読み取りが失敗する場合に、IOException を投げることを確かめる。 読めなかった応答を空の回答として扱わないことを守る。
       *
       * @given 応答のステータスが200で、本文の取得が IOException を投げる
       * @when generate を "質問" で呼び出す
       * @then IOException を投げる
       */
      @Test
      void 本文の読み取りが失敗する場合_IOExceptionを投げる() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getResponseCode()).thenReturn(200);
        when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(conn.getInputStream()).thenThrow(new IOException("読み取り失敗"));
        try (AutoCloseable opened = open(conn)) {
          assertThatThrownBy(() -> new OpenAI("key").generate("質問"))
              .isInstanceOf(IOException.class);
        }
      }

      /**
       * 既定の接続を使う場合に、エンドポイントが無いことで IOException を投げることを確かめる。 エンドポイントが未設定の状態で外部へ接続しないことを守る。
       *
       * @given エンドポイントの設定が無く、接続の差し替えを行っていない
       * @when generate を "質問" で呼び出す
       * @then IOException を投げる
       */
      @Test
      void 既定の接続を使いエンドポイントが無い場合_IOExceptionを投げる() throws Exception {
        assertThatThrownBy(() -> new OpenAI("key").generate("質問")).isInstanceOf(IOException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 応答の選択肢が無い場合に、回答が null の GptAnswer を返すことを確かめる。 選択肢が無い応答で例外を投げず、回答の無い結果として扱うことを守る。
       *
       * @given 応答の choices が空の配列である
       * @when generate を "質問" で呼び出す
       * @then 回答が null の GptAnswer を返す
       */
      @Test
      void 応答の選択肢が無い場合_回答がnullのGptAnswerを返す() throws Exception {
        try (AutoCloseable opened = open(status(200, null, CHAT_NO_CHOICE))) {
          assertThat(new OpenAI("key").generate("質問").getAnswer()).isNull();
        }
      }

      /**
       * 質問が null の場合に、null を返すことを確かめる。 質問が無い時に通信を行わないことを守る。
       *
       * @given 質問が null である
       * @when generate を null で呼び出す
       * @then null を返す
       */
      @Test
      void 質問がnullの場合_nullを返す() throws Exception {
        assertThat(new OpenAI("key").generate(null)).isNull();
      }

      /**
       * 応答に選択肢の項目が無い場合に、回答が null の GptAnswer を返すことを確かめる。 選択肢の無い応答を空の回答として扱い、例外にしないことを守る。
       *
       * @given 応答の本文が choices を持たない JSON である
       * @when generate を "質問" で呼び出す
       * @then 回答が null の GptAnswer を返す
       */
      @Test
      void 応答の選択肢の項目が無い場合_回答がnullのGptAnswerを返す() throws Exception {
        try (AutoCloseable opened = open(status(200, null, "{}"))) {
          assertThat(new OpenAI("key").generate("質問").getAnswer()).isNull();
        }
      }

      /**
       * 応答の選択肢に本文が無い場合に、回答が null の GptAnswer を返すことを確かめる。 本文の無い選択肢を空の回答として扱い、例外にしないことを守る。
       *
       * @given 応答の choices の先頭が message を持たない
       * @when generate を "質問" で呼び出す
       * @then 回答が null の GptAnswer を返す
       */
      @Test
      void 選択肢に本文が無い場合_回答がnullのGptAnswerを返す() throws Exception {
        try (AutoCloseable opened = open(status(200, null, "{\"choices\":[{}]}"))) {
          assertThat(new OpenAI("key").generate("質問").getAnswer()).isNull();
        }
      }

      /**
       * 温度の設定が空文字の場合に、既定の 0.7 を質問の要求に含めることを確かめる。 空の設定を 0 として扱わず、既定の温度で要求を送ることを守る。
       *
       * @given 設定 OPEN_AI_COMPLETION_TEMPERATURE が空文字である
       * @when generate を "質問" で呼び出す
       * @then 要求の本文に temperature の 0.7 を含む
       */
      @Test
      void 温度の設定が空文字の場合_既定の温度を含む要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        Properties.putForTest("OPEN_AI_COMPLETION_TEMPERATURE", "");
        try (AutoCloseable opened = open(status(200, null, CHAT_OK, sent))) {
          new OpenAI("key").generate("質問");
        } finally {
          Properties.removeForTest("OPEN_AI_COMPLETION_TEMPERATURE");
        }
        assertThat(sent.toString(StandardCharsets.UTF_8)).contains("\"temperature\":0.7");
      }
    }
  }

  @Nested
  class generate_String_Float {

    @Nested
    class 境界値 {

      /**
       * 温度が null の場合に、null を返すことを確かめる。 温度が無い要求を送らないことを守る。
       *
       * @given 温度が null である
       * @when generate を prompt と null の温度で呼び出す
       * @then null を返す
       */
      @Test
      void 温度がnullの場合_nullを返す() throws Exception {
        assertThat(new OpenAI("key").generate("質問", (Float) null)).isNull();
      }
    }
  }

  /**
   * HTTP 接続の差し替えを設定し、テストの後に元へ戻す.
   *
   * @param conn HTTP 接続のモック
   * @return 元に戻す処理
   */
  private static AutoCloseable openSequence(final HttpURLConnection... conns) {
    OpenAI.ConnectionOpener original = OpenAI.connectionOpener;
    java.util.Iterator<HttpURLConnection> iterator = List.of(conns).iterator();
    OpenAI.connectionOpener = endpoint -> iterator.next();
    return () -> OpenAI.connectionOpener = original;
  }

  /**
   * HTTP 接続の差し替えを設定し、テストの後に元へ戻す.
   *
   * @param conn HTTP 接続のモック
   * @return 元に戻す処理
   */
  private static AutoCloseable open(final HttpURLConnection conn) {
    OpenAI.ConnectionOpener original = OpenAI.connectionOpener;
    OpenAI.connectionOpener = endpoint -> conn;
    return () -> OpenAI.connectionOpener = original;
  }

  /**
   * 質問の応答を返すための HTTP 接続を作る.
   *
   * @param body 送信内容を受け取るストリーム
   * @return HTTP 接続のモック
   */
  private static HttpURLConnection okChat(final ByteArrayOutputStream body) {
    return status(200, null, CHAT_OK, body);
  }

  /**
   * embedding の応答を返すための HTTP 接続を作る.
   *
   * @return HTTP 接続のモック
   */
  private static HttpURLConnection okEmbedding() {
    return okEmbeddingBody(EMBEDDING_OK);
  }

  /**
   * 指定の本文の embedding 応答を返す HTTP 接続を作る.
   *
   * @param body 応答本文
   * @return HTTP 接続のモック
   */
  private static HttpURLConnection okEmbeddingBody(final String body) {
    return status(200, null, body);
  }

  /**
   * ステータスと本文を指定した HTTP 接続を作る.
   *
   * @param code ステータス
   * @param errorBody エラー本文（null の場合はエラー本文を持たない）
   * @param okBody 成功時の本文（null の場合は空）
   * @return HTTP 接続のモック
   */
  private static HttpURLConnection status(
      final int code, final String errorBody, final String okBody) {
    return status(code, errorBody, okBody, new ByteArrayOutputStream());
  }

  /**
   * ステータスと本文と送信内容の受け口を指定した HTTP 接続を作る.
   *
   * @param code ステータス
   * @param errorBody エラー本文
   * @param okBody 成功時の本文
   * @param sent 送信内容を受け取るストリーム
   * @return HTTP 接続のモック
   */
  private static HttpURLConnection status(
      final int code,
      final String errorBody,
      final String okBody,
      final ByteArrayOutputStream sent) {
    HttpURLConnection conn = mock(HttpURLConnection.class);
    try {
      when(conn.getOutputStream()).thenReturn(sent);
      when(conn.getResponseCode()).thenReturn(code);
      when(conn.getInputStream()).thenReturn(stream(okBody != null ? okBody : ""));
      when(conn.getErrorStream()).thenReturn(errorBody == null ? null : stream(errorBody));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return conn;
  }

  /**
   * 文字列を入力ストリームにする.
   *
   * @param text 文字列
   * @return 入力ストリーム
   */
  private static InputStream stream(final String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  @Nested
  class openDefault {

    @Nested
    class 正常系 {

      /**
       * URL を指定した場合に、通信を行わずに HTTP 接続を開くことを確かめる。 既定の接続は接続を開くだけで、通信は後から行うためである。
       *
       * @given 送信先が "http://127.0.0.1/embedding" の URL
       * @when openDefault を実行する
       * @then HttpURLConnection を返す
       */
      @Test
      void URLを指定する場合_HttpURLConnectionを返す() throws IOException {
        assertThat(OpenAI.openDefault("http://127.0.0.1/embedding"))
            .isInstanceOf(HttpURLConnection.class);
      }
    }

    @Nested
    class 異常系 {

      /**
       * URL の形式が不正な場合に例外を投げることを確かめる。 形式の誤った送信先へ接続を試みないためである。
       *
       * @given 送信先が "不正なURL" の文字列
       * @when openDefault を実行する
       * @then MalformedURLException を投げる
       */
      @Test
      void URLの形式が不正な場合_MalformedURLExceptionを投げる() {
        assertThatThrownBy(() -> OpenAI.openDefault("不正なURL"))
            .isInstanceOf(MalformedURLException.class);
      }
    }
  }
}
