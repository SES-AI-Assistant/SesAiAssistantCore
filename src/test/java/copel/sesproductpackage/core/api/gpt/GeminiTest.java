package copel.sesproductpackage.core.api.gpt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.genai.Models;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentResponse;
import copel.sesproductpackage.core.database.base.DynamoDbClientFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/** {@link Gemini} の単体テスト。 */
class GeminiTest {

  private MockedStatic<DynamoDbClientFactory> dynamo;
  private DynamoDbClient dynamoClient;
  private Gemini.ModelsFactory originalModelsFactory;

  /** 使用履歴の DynamoDB を、AWS へ接続しないモックに差し替える. */
  @BeforeEach
  void mockClients() {
    dynamoClient = mock(DynamoDbClient.class);
    when(dynamoClient.getItem(any(GetItemRequest.class)))
        .thenReturn(GetItemResponse.builder().build());
    when(dynamoClient.putItem(any(PutItemRequest.class)))
        .thenReturn(PutItemResponse.builder().build());
    when(dynamoClient.updateItem(any(UpdateItemRequest.class)))
        .thenReturn(UpdateItemResponse.builder().build());
    dynamo = mockStatic(DynamoDbClientFactory.class);
    dynamo.when(DynamoDbClientFactory::create).thenReturn(dynamoClient);
    originalModelsFactory = Gemini.modelsFactory;
  }

  /** 差し替えを元に戻す. */
  @AfterEach
  void closeMocks() {
    Gemini.modelsFactory = originalModelsFactory;
    dynamo.close();
  }

  @Nested
  class コンストラクタ_String {

    @Nested
    class 正常系 {

      /**
       * APIキーだけを指定した場合に、既定の質問応答モデルで要求することを確かめる。 呼び出し側でモデルを省けるように既定値を使うことを守る。
       *
       * @given APIキーだけがあり、Gemini の応答が "はい" を返す
       * @when generate を "質問" で呼び出す
       * @then 既定のモデル gemini-2.5-flash-lite で要求を送信する
       */
      @Test
      void APIキーのみの場合_既定のモデルで要求を送信する() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("はい")).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);

        new Gemini("key").generate("質問");

        verify(models).generateContent(eq("gemini-2.5-flash-lite"), eq("質問"), isNull());
      }

      /**
       * 差し替えを行わない既定の生成関数が、SDK の Models を返すことを確かめる。 本番では既定の生成関数が使われるため、既定の経路が Models を作ることを守る。
       *
       * @given 生成関数を既定の状態に戻し、通信を伴わない
       * @when APIキー "key" で既定の生成関数を呼び出す
       * @then 空でない Models を返す
       */
      @Test
      void 既定の生成関数を使う場合_SDKのModelsを返す() throws Exception {
        Gemini.modelsFactory = originalModelsFactory;

        assertThat(Gemini.modelsFactory.create("key")).isNotNull();
      }
    }
  }

  @Nested
  class コンストラクタ_String_String {

    @Nested
    class 正常系 {

      /**
       * モデル名を指定した場合に、そのモデルで要求を送信することを確かめる。 用途に応じたモデルを選べるようにするため、指定の値を使うことを守る。
       *
       * @given APIキーと モデル名 gemini-1.5-pro がある
       * @when generate を "質問" で呼び出す
       * @then モデル gemini-1.5-pro で要求を送信する
       */
      @Test
      void モデル名を指定した場合_そのモデルで要求を送信する() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("はい")).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);

        new Gemini("key", "gemini-1.5-pro").generate("質問");

        verify(models).generateContent(eq("gemini-1.5-pro"), eq("質問"), isNull());
      }
    }
  }

  @Nested
  class コンストラクタ_String_GeminiModel {

    @Nested
    class 正常系 {

      /**
       * モデルの列挙値を指定した場合に、その名前で要求を送信することを確かめる。 列挙値の名前と送信するモデル名を一致させることを守る。
       *
       * @given APIキーと モデルの列挙値 GEMINI_1_5_FLASH がある
       * @when generate を "質問" で呼び出す
       * @then モデル gemini-1.5-flash で要求を送信する
       */
      @Test
      void モデルの列挙値を指定した場合_その名前で要求を送信する() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("はい")).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);

        new Gemini("key", GeminiModel.GEMINI_1_5_FLASH).generate("質問");

        verify(models).generateContent(eq("gemini-1.5-flash"), eq("質問"), isNull());
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
       * @given 埋め込みの値が 0.5 と 0.25 である
       * @when embedding を "文字列" で呼び出す
       * @then 0.5 と 0.25 を要素に持つベクトルを返す
       */
      @Test
      void 文字列の場合_埋め込みベクトルを返す() throws Exception {
        Models models = mock(Models.class);
        doReturn(embedding(List.of(0.5f, 0.25f)))
            .when(models)
            .embedContent(anyString(), anyString(), isNull());
        useClient(models);

        assertThat(new Gemini("key").embedding("文字列")).containsExactly(0.5f, 0.25f);
      }

      /**
       * 埋め込みを行う場合に、使用履歴の種別を Embedding として記録することを確かめる。 埋め込みの利用量を質問応答と区別して集計できるようにすることを守る。
       *
       * @given 埋め込みの値が 0.5 と 0.25 である
       * @when embedding を "文字列" で呼び出す
       * @then 使用履歴の API 種別が Embedding である
       */
      @Test
      void 埋め込みを行う場合_種別がEmbeddingの使用履歴を登録する() throws Exception {
        Models models = mock(Models.class);
        doReturn(embedding(List.of(0.5f, 0.25f)))
            .when(models)
            .embedContent(anyString(), anyString(), isNull());
        useClient(models);

        new Gemini("key").embedding("文字列");

        assertThat(recordedApiType()).isEqualTo("Embedding");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 通信が失敗する場合に、RuntimeException を投げることを確かめる。 失敗を埋め込みなしの結果として扱わないことを守る。
       *
       * @given 埋め込みの呼び出しが例外を投げる
       * @when embedding を "文字列" で呼び出す
       * @then RuntimeException を投げる
       */
      @Test
      void 通信が失敗する場合_RuntimeExceptionを投げる() throws Exception {
        Models models = mock(Models.class);
        when(models.embedContent(anyString(), anyString(), isNull()))
            .thenThrow(new IllegalStateException("接続失敗"));
        useClient(models);

        assertThatThrownBy(() -> new Gemini("key").embedding("文字列"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Embedding API Error");
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
        assertThat(new Gemini("key").embedding(null)).isNull();
      }

      /**
       * 入力が空白だけの場合に、null を返すことを確かめる。 空白だけの文字列を埋め込まないことを守る。
       *
       * @given 入力が空白だけである
       * @when embedding を空白で呼び出す
       * @then null を返す
       */
      @Test
      void 入力が空白だけの場合_nullを返す() throws Exception {
        assertThat(new Gemini("key").embedding("  ")).isNull();
      }

      /**
       * 埋め込みの一覧が無い応答の場合に、null を返すことを確かめる。 埋め込みが無い応答を空のベクトルとして扱わないことを守る。
       *
       * @given 応答の埋め込みが空の Optional である
       * @when embedding を "文字列" で呼び出す
       * @then null を返す
       */
      @Test
      void 埋め込みの一覧が無い場合_nullを返す() throws Exception {
        EmbedContentResponse response = mock(EmbedContentResponse.class);
        when(response.embeddings()).thenReturn(Optional.empty());
        Models models = mock(Models.class);
        doReturn(response).when(models).embedContent(anyString(), anyString(), isNull());
        useClient(models);

        assertThat(new Gemini("key").embedding("文字列")).isNull();
      }

      /**
       * 埋め込みの一覧が空の応答の場合に、null を返すことを確かめる。 先頭の埋め込みが無い応答で例外を投げないことを守る。
       *
       * @given 応答の埋め込みが空の一覧である
       * @when embedding を "文字列" で呼び出す
       * @then null を返す
       */
      @Test
      void 埋め込みの一覧が空の場合_nullを返す() throws Exception {
        EmbedContentResponse response = mock(EmbedContentResponse.class);
        when(response.embeddings()).thenReturn(Optional.of(List.of()));
        Models models = mock(Models.class);
        doReturn(response).when(models).embedContent(anyString(), anyString(), isNull());
        useClient(models);

        assertThat(new Gemini("key").embedding("文字列")).isNull();
      }

      /**
       * 埋め込みの値が無い応答の場合に、null を返すことを確かめる。 値の無い埋め込みを空のベクトルとして扱わないことを守る。
       *
       * @given 先頭の埋め込みの値が空の Optional である
       * @when embedding を "文字列" で呼び出す
       * @then null を返す
       */
      @Test
      void 埋め込みの値が無い場合_nullを返す() throws Exception {
        ContentEmbedding embedding = mock(ContentEmbedding.class);
        when(embedding.values()).thenReturn(Optional.empty());
        Models models = mock(Models.class);
        doReturn(embeddingOf(embedding))
            .when(models)
            .embedContent(anyString(), anyString(), isNull());
        useClient(models);

        assertThat(new Gemini("key").embedding("文字列")).isNull();
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
       * @given 応答の本文が "はい" である
       * @when generate を "質問" で呼び出す
       * @then 回答 "はい" を含む GptAnswer を返す
       */
      @Test
      void 質問がある場合_回答を返す() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("はい")).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);

        assertThat(new Gemini("key").generate("質問").getAnswer()).isEqualTo("はい");
      }

      /**
       * 使用履歴の記録が失敗しても、回答を返すことを確かめる。 履歴の記録の失敗で業務の回答を失わないことを守る。
       *
       * @given 応答の本文が "はい" で、使用履歴の DynamoDB の生成が例外を投げる
       * @when generate を "質問" で呼び出す
       * @then 回答 "はい" を含む GptAnswer を返す
       */
      @Test
      void 使用履歴の記録が失敗する場合_回答を返す() throws Exception {
        dynamo.when(DynamoDbClientFactory::create).thenThrow(new IllegalStateException("接続失敗"));
        Models models = mock(Models.class);
        doReturn(text("はい")).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);

        assertThat(new Gemini("key").generate("質問").getAnswer()).isEqualTo("はい");
      }

      /**
       * 質問応答を行う場合に、呼び出し元のクラス名をログに出すことを確かめる。 障害の調査で呼び出し元を特定できるように、ログに呼び出し元を含めることを守る。
       *
       * @given Gemini のログにテスト用の追加先があり、応答は "はい" である
       * @when generate を "質問" で呼び出す
       * @then ログの本文に呼び出し元のクラス名 GeminiTest を含む
       */
      @Test
      void 質問応答を行う場合_呼び出し元のクラス名を含むログを登録する() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("はい")).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);
        Logger logger = (Logger) LoggerFactory.getLogger(Gemini.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
          new Gemini("key").generate("質問");
        } finally {
          logger.detachAppender(appender);
        }

        assertThat(appender.list)
            .anyMatch(event -> event.getFormattedMessage().contains("GeminiTest"));
      }
    }

    @Nested
    class 異常系 {

      /**
       * 通信が失敗する場合に、RuntimeException を投げることを確かめる。 失敗を空の回答として扱わないことを守る。
       *
       * @given 生成の呼び出しが例外を投げる
       * @when generate を "質問" で呼び出す
       * @then RuntimeException を投げる
       */
      @Test
      void 通信が失敗する場合_RuntimeExceptionを投げる() throws Exception {
        Models models = mock(Models.class);
        when(models.generateContent(anyString(), anyString(), isNull()))
            .thenThrow(new IllegalStateException("接続失敗"));
        useClient(models);

        assertThatThrownBy(() -> new Gemini("key").generate("質問"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Generate API Error");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 質問が null の場合に、null を返すことを確かめる。 質問が無い時に通信を行わないことを守る。
       *
       * @given 質問が null である
       * @when generate を null で呼び出す
       * @then null を返す
       */
      @Test
      void 質問がnullの場合_nullを返す() throws Exception {
        assertThat(new Gemini("key").generate(null)).isNull();
      }

      /**
       * 応答が無い場合に、回答が null の GptAnswer を返すことを確かめる。 応答が無いことを回答の無い結果として扱い、例外にしないことを守る。
       *
       * @given 生成の応答が null である
       * @when generate を "質問" で呼び出す
       * @then 回答が null の GptAnswer を返す
       */
      @Test
      void 応答が無い場合_回答がnullのGptAnswerを返す() throws Exception {
        Models models = mock(Models.class);
        doReturn(null).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);

        assertThat(new Gemini("key").generate("質問").getAnswer()).isNull();
      }

      /**
       * 応答の本文が無い場合に、回答が null の GptAnswer を返すことを確かめる。 本文の無い応答を空の文字列として扱わないことを守る。
       *
       * @given 生成の応答の本文が null である
       * @when generate を "質問" で呼び出す
       * @then 回答が null の GptAnswer を返す
       */
      @Test
      void 応答の本文が無い場合_回答がnullのGptAnswerを返す() throws Exception {
        GenerateContentResponse response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn(null);
        Models models = mock(Models.class);
        doReturn(response).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);

        assertThat(new Gemini("key").generate("質問").getAnswer()).isNull();
      }

      /**
       * 質問が空白だけの場合に、null を返すことを確かめる。 空白だけの質問で通信を行わないことを守る。
       *
       * @given 質問が空白だけである
       * @when generate を空白で呼び出す
       * @then null を返す
       */
      @Test
      void 質問が空白だけの場合_nullを返す() throws Exception {
        assertThat(new Gemini("key").generate("  ")).isNull();
      }

      /**
       * 出力が空の質問応答の場合に、使用履歴の種別を Generate として記録することを確かめる。 出力の文字数が 0 でも質問応答は Generate
       * として記録し、埋め込みと区別することを守る。
       *
       * @given 応答の本文が空文字である
       * @when generate を "質問" で呼び出す
       * @then 使用履歴の API 種別が Generate である
       */
      @Test
      void 出力が空の質問応答の場合_種別がGenerateの使用履歴を登録する() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("")).when(models).generateContent(anyString(), anyString(), isNull());
        useClient(models);

        new Gemini("key").generate("質問");

        assertThat(recordedApiType()).isEqualTo("Generate");
      }
    }
  }

  @Nested
  class generate_String_Class {

    @Nested
    class 正常系 {

      /**
       * 応答が型に合う JSON の場合に、その型の値を返すことを確かめる。 構造化された応答を型として扱えるようにするため、読み込みの結果を守る。
       *
       * @given 応答の本文が name と age を持つ JSON である
       * @when generate を "質問" と Person の型で呼び出す
       * @then name が "田中" で age が30の Person を返す
       */
      @Test
      void 応答が型に合うJSONの場合_その型の値を返す() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("{\"name\":\"田中\",\"age\":30}"))
            .when(models)
            .generateContent(anyString(), anyString(), any());
        useClient(models);

        Person person = new Gemini("key").generate("質問", Person.class);

        assertThat(person.name).isEqualTo("田中");
        assertThat(person.age).isEqualTo(30);
      }

      /**
       * 使用履歴の記録が失敗しても、型の値を返すことを確かめる。 履歴の記録の失敗で構造化された応答を失わないことを守る。
       *
       * @given 応答の本文が型に合う JSON で、使用履歴の DynamoDB の生成が例外を投げる
       * @when generate を "質問" と Person の型で呼び出す
       * @then name が "田中" の Person を返す
       */
      @Test
      void 使用履歴の記録が失敗する場合_型の値を返す() throws Exception {
        dynamo.when(DynamoDbClientFactory::create).thenThrow(new IllegalStateException("接続失敗"));
        Models models = mock(Models.class);
        doReturn(text("{\"name\":\"田中\",\"age\":30}"))
            .when(models)
            .generateContent(anyString(), anyString(), any());
        useClient(models);

        assertThat(new Gemini("key").generate("質問", Person.class).name).isEqualTo("田中");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 応答が型に合わない場合に、構造化の失敗として例外を投げることを確かめる。 解析できない応答を部分的に読み込まないことを守る。
       *
       * @given 応答の本文が JSON ではない文字列である
       * @when generate を "質問" と Person の型で呼び出す
       * @then RuntimeException を投げ、メッセージに Structured Generate を含む
       */
      @Test
      void 応答が型に合わない場合_RuntimeExceptionを投げる() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("not json")).when(models).generateContent(anyString(), anyString(), any());
        useClient(models);

        assertThatThrownBy(() -> new Gemini("key").generate("質問", Person.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Structured Generate API Error");
      }

      /**
       * 通信が失敗する場合に、元の RuntimeException をそのまま投げることを確かめる。 実行時の例外を包み直さず、原因を変えないことを守る。
       *
       * @given 生成の呼び出しが RuntimeException を投げる
       * @when generate を "質問" と Person の型で呼び出す
       * @then 同じ例外を投げる
       */
      @Test
      void 通信が失敗する場合_RuntimeExceptionを投げる() throws Exception {
        IllegalStateException failure = new IllegalStateException("接続失敗");
        Models models = mock(Models.class);
        when(models.generateContent(anyString(), anyString(), any())).thenThrow(failure);
        useClient(models);

        assertThatThrownBy(() -> new Gemini("key").generate("質問", Person.class)).isSameAs(failure);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 質問が空白だけの場合に、例外を投げることを確かめる。 空の質問で要求を送らないことを守る。
       *
       * @given 質問が空白だけである
       * @when generate を空白と Person の型で呼び出す
       * @then RuntimeException を投げ、メッセージに Prompt を含む
       */
      @Test
      void 質問が空白だけの場合_RuntimeExceptionを投げる() {
        assertThatThrownBy(() -> new Gemini("key").generate(" ", Person.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Prompt must not be blank");
      }

      /**
       * 応答が無い場合に、例外を投げることを確かめる。 応答が無いまま空の値を返すと、呼び出し元が誤った結果を使うことを守る。
       *
       * @given 生成の応答が null である
       * @when generate を "質問" と Person の型で呼び出す
       * @then RuntimeException を投げ、メッセージに null response を含む
       */
      @Test
      void 応答が無い場合_RuntimeExceptionを投げる() throws Exception {
        Models models = mock(Models.class);
        doReturn(null).when(models).generateContent(anyString(), anyString(), any());
        useClient(models);

        assertThatThrownBy(() -> new Gemini("key").generate("質問", Person.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("API returned null response");
      }

      /**
       * 応答の本文が空の場合に、例外を投げることを確かめる。 空の本文を JSON として読めないまま扱わないことを守る。
       *
       * @given 生成の応答の本文が空白だけである
       * @when generate を "質問" と Person の型で呼び出す
       * @then RuntimeException を投げ、メッセージに empty response を含む
       */
      @Test
      void 応答の本文が空の場合_RuntimeExceptionを投げる() throws Exception {
        Models models = mock(Models.class);
        doReturn(text("  ")).when(models).generateContent(anyString(), anyString(), any());
        useClient(models);

        assertThatThrownBy(() -> new Gemini("key").generate("質問", Person.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("API returned empty response");
      }

      /**
       * 型を指定しない場合に、例外を投げることを確かめる。 読み込み先の型が無いまま要求を送らないことを守る。
       *
       * @given 型が null である
       * @when generate を "質問" と null の型で呼び出す
       * @then RuntimeException を投げ、メッセージに Response type を含む
       */
      @Test
      void 型がnullの場合_RuntimeExceptionを投げる() {
        assertThatThrownBy(() -> new Gemini("key").generate("質問", null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Response type must not be null");
      }

      /**
       * 質問が null の場合に、例外を投げることを確かめる。 質問が無いまま要求を送らないことを守る。
       *
       * @given 質問が null である
       * @when generate を null と Person の型で呼び出す
       * @then RuntimeException を投げ、メッセージに Prompt を含む
       */
      @Test
      void 質問がnullの場合_RuntimeExceptionを投げる() throws Exception {
        assertThatThrownBy(() -> new Gemini("key").generate(null, Person.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Prompt must not be blank");
      }

      /**
       * 応答の本文が null の場合に、例外を投げることを確かめる。 本文の無い応答を空として扱わず、呼び出し元へ伝えることを守る。
       *
       * @given 生成の応答の本文が null である
       * @when generate を "質問" と Person の型で呼び出す
       * @then RuntimeException を投げ、メッセージに empty response を含む
       */
      @Test
      void 応答の本文がnullの場合_RuntimeExceptionを投げる() throws Exception {
        GenerateContentResponse response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn(null);
        Models models = mock(Models.class);
        doReturn(response).when(models).generateContent(anyString(), anyString(), any());
        useClient(models);

        assertThatThrownBy(() -> new Gemini("key").generate("質問", Person.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("API returned empty response");
      }
    }
  }

  /**
   * Models の生成を差し替え、指定の Models を使うようにする.
   *
   * @param models 使わせる Models のモック
   */
  private static void useClient(final Models models) {
    Gemini.modelsFactory = apiKey -> models;
  }

  /**
   * 使用履歴として記録された API 種別を取り出す.
   *
   * @return 記録された API 種別
   */
  private String recordedApiType() {
    ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
    verify(dynamoClient).putItem(captor.capture());
    return captor.getValue().item().get("apiType").s();
  }

  /**
   * 本文を持つ生成の応答を作る.
   *
   * @param body 本文
   * @return 生成の応答
   */
  private static GenerateContentResponse text(final String body) {
    GenerateContentResponse response = mock(GenerateContentResponse.class);
    when(response.text()).thenReturn(body);
    return response;
  }

  /**
   * 指定の値を持つ埋め込みの応答を作る.
   *
   * @param values 埋め込みの値
   * @return 埋め込みの応答
   */
  private static EmbedContentResponse embedding(final List<Float> values) {
    ContentEmbedding embedding = mock(ContentEmbedding.class);
    when(embedding.values()).thenReturn(Optional.of(values));
    return embeddingOf(embedding);
  }

  /**
   * 埋め込みを1件持つ応答を作る.
   *
   * @param embedding 埋め込み
   * @return 埋め込みの応答
   */
  private static EmbedContentResponse embeddingOf(final ContentEmbedding embedding) {
    EmbedContentResponse response = mock(EmbedContentResponse.class);
    when(response.embeddings()).thenReturn(Optional.of(List.of(embedding)));
    return response;
  }

  /** 構造化出力の読み込み先。 */
  public static class Person {
    /** 名前。 */
    public String name;

    /** 年齢。 */
    public int age;
  }
}
