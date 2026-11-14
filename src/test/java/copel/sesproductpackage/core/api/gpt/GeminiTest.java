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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.genai.Models;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentResponse;
import copel.sesproductpackage.core.api.gpt.decision.ChoiceQuestion;
import copel.sesproductpackage.core.api.gpt.decision.DecisionBlockedException;
import copel.sesproductpackage.core.api.gpt.decision.DecisionResult;
import copel.sesproductpackage.core.api.gpt.decision.NoulQuestion;
import copel.sesproductpackage.core.database.base.DynamoDbClientFactory;
import copel.sesproductpackage.core.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/** {@link Gemini} の単体テスト。 */
class GeminiTest {

  /** 判定 API のエンドポイント。テストでは設定プロパティに入れる。 */
  private static final String DECISION_URL =
      "https://generativelanguage.googleapis.com/v1beta/decisions";

  /** 判定 API のエンドポイントを保持する設定プロパティの名前。 */
  private static final String DECISION_URL_KEY = "GEMINI_DECISION_API_URL";

  /** 判定 API の正常な応答。Yes/No の回答 person_1 と択一の回答 link を含む。 */
  private static final String DECISION_OK =
      "{\"answers\":{\"person_1\":{\"type\":\"noul\",\"noul\":0.97,\"confidence\":0.97},"
          + "\"link\":{\"type\":\"choice\",\"choice\":\"billing\","
          + "\"probabilities\":{\"billing\":0.9,\"access\":0.1},\"confidence\":0.9}},"
          + "\"usage\":{\"total_input_tokens\":10,\"total_output_tokens\":0}}";

  private MockedStatic<DynamoDbClientFactory> dynamo;
  private DynamoDbClient dynamoClient;
  private Gemini.ModelsFactory originalModelsFactory;
  private Gemini.ConnectionOpener originalConnectionOpener;

  /** 使用履歴の DynamoDB を、AWS へ接続しないモックに差し替える。判定の設定プロパティも入れる. */
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
    originalConnectionOpener = Gemini.connectionOpener;
    Properties.putForTest(DECISION_URL_KEY, DECISION_URL);
  }

  /** 差し替えと設定を元に戻す. */
  @AfterEach
  void closeMocks() {
    Gemini.modelsFactory = originalModelsFactory;
    Gemini.connectionOpener = originalConnectionOpener;
    Properties.removeForTest(DECISION_URL_KEY);
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

  @Nested
  class openDefault {

    @Nested
    class 正常系 {

      /**
       * 既定の接続は指定の URL へ接続を開くだけで、通信を行わないことを確かめる。 テストで差し替えない本番の経路が、判定 API の URL を指定どおりに使う仕様を守る。
       *
       * @given 判定 API の URL が "https://example.test/decisions"
       * @when openDefault を URL で呼び出す
       * @then 同じ URL を持つ HTTP 接続を返す
       */
      @Test
      void 判定APIのURLの場合_同じURLを持つ接続を返す() throws Exception {
        HttpURLConnection conn = Gemini.openDefault("https://example.test/decisions");

        assertThat(conn.getURL().toString()).isEqualTo("https://example.test/decisions");
      }
    }
  }

  @Nested
  class decide {

    @Nested
    class 正常系 {

      /**
       * Yes/No の質問と択一の質問を1回の判定に混在させても、それぞれの型で回答を取り出せることを確かめる。 共有の入力を1回だけ送り、質問の種類ごとに回答の型を分ける仕様を守る。
       *
       * @given 判定 API が person_1 の Yes/No の回答と link の択一の回答を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 と択一の質問 link で実行する
       * @then person_1 は確率 0.97 の回答、link は選ばれた選択肢 "billing" の回答を返す
       */
      @Test
      void 質問の種類が混在する場合_それぞれの型の回答を返す() throws Exception {
        useOpener(connection(200, DECISION_OK, new ByteArrayOutputStream()));
        NoulQuestion person = NoulQuestion.of("person_1", "条件を満たすか");
        ChoiceQuestion link = ChoiceQuestion.of("link", "該当する要員は", billingAccessOptions());

        DecisionResult result = new Gemini("key").decide("共有の入力", List.of(person, link));

        assertThat(result.answerOf(person).getProbability()).isEqualTo(0.97);
        assertThat(result.answerOf(link).getChoice()).isEqualTo("billing");
      }

      /**
       * 判定を実行すると設定の URL へ送信し、認証ヘッダーを付けて回答を返すことを確かめる。 送信先を設定で切り替え、認証は API キーのヘッダーで行う仕様を守る。
       *
       * @given 設定の URL が DECISION_URL、判定 API が person_1 の回答を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then 設定の URL に接続を開き、x-goog-api-key に "key" を設定して、確率 0.97 の回答を返す
       */
      @Test
      void 判定を実行する場合_設定のURLへ送信して回答を返す() throws Exception {
        HttpURLConnection conn = connection(200, DECISION_OK, new ByteArrayOutputStream());
        Gemini.ConnectionOpener opener = useOpener(conn);
        NoulQuestion person = NoulQuestion.of("person_1", "条件を満たすか");

        DecisionResult result = new Gemini("key").decide("共有の入力", List.of(person));

        verify(opener).open(DECISION_URL);
        verify(conn).setRequestProperty("x-goog-api-key", "key");
        assertThat(result.answerOf(person).getProbability()).isEqualTo(0.97);
      }

      /**
       * 判定を実行すると接続と読み取りのタイムアウトを設定して送信することを確かめる。 応答が止まった判定の呼び出しを、無期限に待たずに打ち切る仕様である。
       *
       * @given 設定の URL が DECISION_URL、判定 API が person_1 の回答を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then 接続のタイムアウトに 5000、読み取りのタイムアウトに 60000 を設定し、確率 0.97 の回答を返す
       */
      @Test
      void 判定を実行する場合_接続と読み取りのタイムアウトを設定して回答を返す() throws Exception {
        HttpURLConnection conn = connection(200, DECISION_OK, new ByteArrayOutputStream());
        useOpener(conn);
        NoulQuestion person = NoulQuestion.of("person_1", "条件を満たすか");

        DecisionResult result = new Gemini("key").decide("共有の入力", List.of(person));

        verify(conn).setConnectTimeout(5000);
        verify(conn).setReadTimeout(60000);
        assertThat(result.answerOf(person).getProbability()).isEqualTo(0.97);
      }

      /**
       * 質問応答用のモデルが別の場合でも、判定の要求は判定用のモデルの名前で送ることを確かめる。 判定の課金と品質は判定用のモデルで決まるため、質問応答の設定に左右されない仕様を守る。
       *
       * @given 質問応答のモデルが gemini-1.5-pro の Gemini、判定 API が person_1 の回答を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then 要求のモデルは gemini-decision-1 で、gemini-1.5-pro を含まない要求を送信する
       */
      @Test
      void 質問応答のモデルが別の場合_判定のモデルを含む要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        useOpener(connection(200, DECISION_OK, sent));

        new Gemini("key", "gemini-1.5-pro")
            .decide("共有の入力", List.of(NoulQuestion.of("person_1", "条件を満たすか")));

        String body = sent.toString(StandardCharsets.UTF_8);
        assertThat(body)
            .contains("\"model\":\"gemini-decision-1\"")
            .doesNotContain("gemini-1.5-pro");
      }

      /**
       * 判断基準を指定した Yes/No の質問は、Yes と No の判断基準を含む要求で送ることを確かめる。 判断基準は判定の根拠として API に渡す仕様を守る。
       *
       * @given Yes の判断基準が "満たす"、No の判断基準が "満たさない" の Yes/No の質問、判定 API が person_1 の回答を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then 要求の質問に Yes と No の判断基準を含む判断基準を送信する
       */
      @Test
      void 判断基準を指定したYesNoの質問の場合_判断基準を含む要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        useOpener(connection(200, DECISION_OK, sent));

        new Gemini("key")
            .decide("共有の入力", List.of(NoulQuestion.of("person_1", "条件を満たすか", "満たす", "満たさない")));

        assertThat(sent.toString(StandardCharsets.UTF_8))
            .contains("\"type\":\"noul\"")
            .contains("\"true_description\":\"満たす\"")
            .contains("\"false_description\":\"満たさない\"");
      }

      /**
       * 判断基準を指定しない Yes/No の質問は、判断基準の項目を含めずに送ることを確かめる。 判断基準は任意の項目のため、指定が無い場合は要求に含めない仕様を守る。
       *
       * @given 判断基準を指定しない Yes/No の質問 person_1、判定 API が person_1 の回答を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then 要求の質問に criteria を含めずに送信する
       */
      @Test
      void 判断基準を指定しないYesNoの質問の場合_criteriaを含まない要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        useOpener(connection(200, DECISION_OK, sent));

        new Gemini("key").decide("共有の入力", List.of(NoulQuestion.of("person_1", "条件を満たすか")));

        assertThat(sent.toString(StandardCharsets.UTF_8)).doesNotContain("criteria");
      }

      /**
       * 択一の質問は、選択肢のキーと説明を criteria として送ることを確かめる。 選択肢の説明は判定の根拠として API に渡す仕様を守る。
       *
       * @given 選択肢 "billing" の説明が "請求"、"access" の説明が空文字の択一の質問 link、判定 API が link の回答を返す
       * @when decide を入力 "共有の入力" と択一の質問 link で実行する
       * @then 要求の質問に選択肢のキーと説明を並べた criteria を送信する
       */
      @Test
      void 選択肢の質問の場合_選択肢のキーと説明を含む要求を送信する() throws Exception {
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        useOpener(connection(200, DECISION_OK, sent));

        new Gemini("key")
            .decide("共有の入力", List.of(ChoiceQuestion.of("link", "該当する要員は", billingAccessOptions())));

        assertThat(sent.toString(StandardCharsets.UTF_8))
            .contains("\"type\":\"choice\"")
            .contains("\"criteria\":{\"billing\":\"請求\",\"access\":\"\"}");
      }

      /**
       * 判定を実行すると、種別 Decision と判定用のモデル名、入力文字数で使用履歴を登録することを確かめる。 判定の課金を判定用のモデルの行で集計できるようにする仕様を守る。
       *
       * @given 共有の入力 "共有の入力"、5文字、Yes/No の質問 person_1 の指示文、7文字、択一の質問 link の指示文と選択肢、22文字、判定 API が
       *     DECISION_OK を返す
       * @when decide を入力 "共有の入力" と2つの質問で実行する
       * @then 種別 "Decision"、モデル "gemini-decision-1"、入力文字数 "34"、出力文字数 "0" の使用履歴を登録する
       */
      @Test
      void 判定を実行する場合_種別がDecisionの使用履歴を登録する() throws Exception {
        useOpener(connection(200, DECISION_OK, new ByteArrayOutputStream()));

        new Gemini("key")
            .decide(
                "共有の入力",
                List.of(
                    NoulQuestion.of("person_1", "条件を満たすか"),
                    ChoiceQuestion.of("link", "該当する要員は", billingAccessOptions())));

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoClient).putItem(captor.capture());
        Map<String, AttributeValue> item = captor.getValue().item();
        assertThat(item.get("apiType").s()).isEqualTo("Decision");
        assertThat(item.get("model").s()).isEqualTo("gemini-decision-1");
        assertThat(item.get("inputCount").n()).isEqualTo("34");
        assertThat(item.get("outputCount").n()).isEqualTo("0");
      }

      /**
       * 判定を実行すると、呼び出し元のクラス名と回答全文をログに出すことを確かめる。 判定の結果を後から確かめられるように、質問応答と同じ粒度で残す仕様を守る。
       *
       * @given Gemini のログに追加先があり、判定 API が person_1 の回答を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then ログの本文に呼び出し元のクラス名 GeminiTest と、回答全文の "noul" の値 0.97 を含むログを登録する
       */
      @Test
      void 判定を実行する場合_呼び出し元と回答全文を含むログを登録する() throws Exception {
        useOpener(connection(200, DECISION_OK, new ByteArrayOutputStream()));
        Logger logger = (Logger) LoggerFactory.getLogger(Gemini.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
          new Gemini("key").decide("共有の入力", List.of(NoulQuestion.of("person_1", "条件を満たすか")));
        } finally {
          logger.detachAppender(appender);
        }

        assertThat(appender.list)
            .anyMatch(event -> event.getFormattedMessage().contains("GeminiTest"))
            .anyMatch(event -> event.getFormattedMessage().contains("\"noul\":0.97"));
      }

      /**
       * 1回目の判定が429で2回目が成功する場合に、2回目の回答を返すことを確かめる。 レート制限の一時的な失敗は再試行で回復させる仕様を守る。
       *
       * @given 1回目の応答が 429、2回目の応答が判定 API の正常な応答
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then 確率 0.97 の回答を返し、接続を2回開く
       */
      @Test
      void ステータス429の後に成功する場合_2回目の回答を返す() throws Exception {
        Gemini.ConnectionOpener opener =
            useOpener(
                connection(429, "{}", new ByteArrayOutputStream()),
                connection(200, DECISION_OK, new ByteArrayOutputStream()));
        NoulQuestion person = NoulQuestion.of("person_1", "条件を満たすか");

        DecisionResult result = new Gemini("key").decide("共有の入力", List.of(person));

        verify(opener, times(2)).open(DECISION_URL);
        assertThat(result.answerOf(person).getProbability()).isEqualTo(0.97);
      }

      /**
       * 設定の URL を変えた後の判定は、新しい URL へ接続することを確かめる。 設定の変更を再起動なしで次の呼び出しから反映する仕様を守る。
       *
       * @given 設定の URL を "https://example.test/first" にして1回判定し、その後 "https://example.test/second"
       *     に変更する
       * @when decide を2回実行する
       * @then 2回目は新しい URL へ接続を開き、確率 0.97 の回答を返す
       */
      @Test
      void プロパティの値を変えた場合_2回目は新しいURLで接続して回答を返す() throws Exception {
        Gemini.ConnectionOpener opener =
            useOpener(
                connection(200, DECISION_OK, new ByteArrayOutputStream()),
                connection(200, DECISION_OK, new ByteArrayOutputStream()));
        NoulQuestion person = NoulQuestion.of("person_1", "条件を満たすか");
        Gemini gemini = new Gemini("key");
        Properties.putForTest(DECISION_URL_KEY, "https://example.test/first");
        gemini.decide("共有の入力", List.of(person));
        Properties.putForTest(DECISION_URL_KEY, "https://example.test/second");

        DecisionResult result = gemini.decide("共有の入力", List.of(person));

        verify(opener).open("https://example.test/first");
        verify(opener).open("https://example.test/second");
        assertThat(result.answerOf(person).getProbability()).isEqualTo(0.97);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 質問キーが重複する場合に、判定を送る前に例外を投げることを確かめる。 同じキーの回答が2つあると取り違えるため、入口で弾く仕様を守る。
       *
       * @given 質問キーが "person_1" の Yes/No の質問が2つ
       * @when decide を入力 "共有の入力" と2つの質問で実行する
       * @then IllegalArgumentException を投げ、判定 API への接続を開かない
       */
      @Test
      void 質問キーが重複する場合_IllegalArgumentExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener = useOpener();
        List<NoulQuestion> questions =
            List.of(NoulQuestion.of("person_1", "条件を満たすか"), NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(opener);
      }

      /**
       * 質問が0件の場合に、判定を送る前に例外を投げることを確かめる。 質問の無い判定は回答を得られないため、入口で弾く仕様を守る。
       *
       * @given 質問の一覧が空
       * @when decide を入力 "共有の入力" と空の質問一覧で実行する
       * @then IllegalArgumentException を投げ、判定 API への接続を開かない
       */
      @Test
      void 質問が0件の場合_IllegalArgumentExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener = useOpener();

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(opener);
      }

      /**
       * 質問の一覧が null の場合に、判定を送る前に例外を投げることを確かめる。 一覧が無い判定は送れないため、入口で弾く仕様を守る。
       *
       * @given 質問の一覧が null
       * @when decide を入力 "共有の入力" と null の質問一覧で実行する
       * @then IllegalArgumentException を投げ、判定 API への接続を開かない
       */
      @Test
      void 質問の一覧がnullの場合_IllegalArgumentExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener = useOpener();

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", null))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(opener);
      }

      /**
       * 質問が1001件の場合に、判定を送る前に例外を投げることを確かめる。 API の質問数の上限を超えるリクエストを送らない仕様を守る。
       *
       * @given 質問キーが重複しない Yes/No の質問が1001件
       * @when decide を入力 "共有の入力" と1001件の質問で実行する
       * @then IllegalArgumentException を投げ、判定 API への接続を開かない
       */
      @Test
      void 質問が1001件の場合_IllegalArgumentExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener = useOpener();
        List<NoulQuestion> questions = manyNoulQuestions(1001);

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(opener);
      }

      /**
       * 429 が3回続く場合に、3回目の試行の後に状態コードを持つ例外を投げることを確かめる。 レート制限は総試行3回までとし、なお失敗なら呼び出し側に任せる仕様を守る。
       *
       * @given 判定 API が3回とも 429 を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then HttpStatusException を投げ、接続を3回開く
       */
      @Test
      void ステータス429が3回続く場合_HttpStatusExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener =
            useOpener(
                connection(429, "{}", new ByteArrayOutputStream()),
                connection(429, "{}", new ByteArrayOutputStream()),
                connection(429, "{}", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(ApiRetryHelper.HttpStatusException.class)
            .hasMessageContaining("429");
        verify(opener, times(3)).open(DECISION_URL);
      }

      /**
       * 503 が3回続く場合に、3回目の試行の後に状態コードを持つ例外を投げることを確かめる。 過負荷による失敗も総試行3回までとする仕様を守る。
       *
       * @given 判定 API が3回とも 503 を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then HttpStatusException を投げ、接続を3回開く
       */
      @Test
      void ステータス503が3回続く場合_HttpStatusExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener =
            useOpener(
                connection(503, "{}", new ByteArrayOutputStream()),
                connection(503, "{}", new ByteArrayOutputStream()),
                connection(503, "{}", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(ApiRetryHelper.HttpStatusException.class)
            .hasMessageContaining("503");
        verify(opener, times(3)).open(DECISION_URL);
      }

      /**
       * 500 が返る場合に、再試行せず1回で状態コードを持つ例外を投げることを確かめる。 再試行の対象は 429 と 503 だけとする仕様を守る。
       *
       * @given 判定 API が 500 を返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then HttpStatusException を投げ、接続を1回だけ開く
       */
      @Test
      void ステータス500が返る場合_HttpStatusExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener =
            useOpener(connection(500, "{}", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(ApiRetryHelper.HttpStatusException.class)
            .hasMessageContaining("500");
        verify(opener, times(1)).open(DECISION_URL);
      }

      /**
       * 400 で error.code が無い場合に、再試行せず通常の状態コードの例外を投げることを確かめる。 安全性以外の 400 は入力の不備として扱い、再試行しない仕様を守る。
       *
       * @given 判定 API が 400 を返し、本文が error.message だけを持つ
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then HttpStatusException を投げ、DecisionBlockedException は投げず、接続を1回だけ開く
       */
      @Test
      void ステータス400でerrorCodeが無い場合_HttpStatusExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener =
            useOpener(
                connection(
                    400, "{\"error\":{\"message\":\"不正な要求\"}}", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(ApiRetryHelper.HttpStatusException.class)
            .hasMessageContaining("400");
        verify(opener, times(1)).open(DECISION_URL);
      }

      /**
       * 400 で error.code が安全性以外の値の場合に、ブロックの例外ではない例外を投げることを確かめる。 安全性以外の入力の不備を、ブロックとして扱わない仕様を守る。
       *
       * @given 判定 API が 400 を返し、error.code が "invalid_argument"
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then HttpStatusException を投げ、DecisionBlockedException は投げない
       */
      @Test
      void ステータス400でerrorCodeが安全性以外の値の場合_HttpStatusExceptionを投げる() throws Exception {
        useOpener(
            connection(
                400, "{\"error\":{\"code\":\"invalid_argument\"}}", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(ApiRetryHelper.HttpStatusException.class)
            .isNotInstanceOf(DecisionBlockedException.class);
      }

      /**
       * 400 で error.code が safety の場合に、ブロックの例外を投げ再試行しないことを確かめる。
       * 安全性のフィルタによるブロックは同じ入力では成功しないため、再試行しない仕様を守る。
       *
       * @given 判定 API が 400 を返し、error.code が "safety"
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then DecisionBlockedException を投げ、接続を1回だけ開く
       */
      @Test
      void ステータス400でerrorCodeがsafetyの場合_DecisionBlockedExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener =
            useOpener(connection(400, blockedBody("safety"), new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(DecisionBlockedException.class);
        verify(opener, times(1)).open(DECISION_URL);
      }

      /**
       * 400 の本文が JSON でない場合に、ブロックではない状態コードの例外を投げることを確かめる。 本文が読めない応答でも、原因を状態コードで伝える仕様を守る。
       *
       * @given 判定 API が 400 を返し、本文が JSON でない文字列
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then HttpStatusException を投げ、DecisionBlockedException は投げない
       */
      @Test
      void ステータス400で本文がJSONでない場合_HttpStatusExceptionを投げる() throws Exception {
        useOpener(connection(400, "不正な本文", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(ApiRetryHelper.HttpStatusException.class)
            .isNotInstanceOf(DecisionBlockedException.class);
      }

      /**
       * 400 でエラー本文が無い場合に、状態コードの例外を投げることを確かめる。 本文が無くても原因を状態コードで伝える仕様を守る。
       *
       * @given 判定 API が 400 を返し、エラー本文が無い
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then HttpStatusException を投げる
       */
      @Test
      void ステータス400でエラー本文が無い場合_HttpStatusExceptionを投げる() throws Exception {
        useOpener(connection(400, null, new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(ApiRetryHelper.HttpStatusException.class)
            .hasMessageContaining("400");
      }

      /**
       * 接続を開けない場合に、入出力の例外をそのまま投げることを確かめる。 通信の失敗は呼び出し元が既存の入出力のエラーとして扱う仕様を守る。
       *
       * @given 接続の生成が IOException を投げる
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then IOException を投げる
       */
      @Test
      void 接続を開けない場合_IOExceptionを投げる() throws Exception {
        Gemini.connectionOpener =
            endpoint -> {
              throw new IOException("接続失敗");
            };
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IOException.class)
            .hasMessage("接続失敗");
      }

      /**
       * 読み取りがタイムアウトした場合に、SocketTimeoutException をそのまま投げ、再試行しないことを確かめる。 再試行の対象は状態コードが 429 と 503
       * の場合だけで、タイムアウトの再試行は Lambda の時間を使い切るため行わない仕様である。
       *
       * @given 応答コードの取得が SocketTimeoutException を投げる接続を、接続の生成が1回だけ返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then SocketTimeoutException を投げ、接続を1回だけ開く
       */
      @Test
      void 読み取りがタイムアウトする場合_SocketTimeoutExceptionを投げる() throws Exception {
        HttpURLConnection conn = connection(200, DECISION_OK, new ByteArrayOutputStream());
        when(conn.getResponseCode()).thenThrow(new SocketTimeoutException("Read timed out"));
        Gemini.ConnectionOpener opener = useOpener(conn);
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(SocketTimeoutException.class);

        verify(opener, times(1)).open(DECISION_URL);
      }

      /**
       * 応答の本文が JSON でない場合に、入出力の例外を投げることを確かめる。 応答を読めない判定は回答を組み立てられないため、失敗として伝える仕様を守る。
       *
       * @given 判定 API が 200 を返し、本文が JSON でない文字列
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then IOException を投げる
       */
      @Test
      void 応答の本文がJSONでない場合_IOExceptionを投げる() throws Exception {
        useOpener(connection(200, "不正な本文", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IOException.class);
      }

      /**
       * 応答に質問の回答が無い場合に、例外を投げることを確かめる。 質問と回答の対応が欠けた判定を黙って通さない仕様を守る。
       *
       * @given 判定 API が answers を空で返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 回答が無い質問の場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(connection(200, "{\"answers\":{}}", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("person_1");
      }

      /**
       * Yes/No の質問に択一の回答だけが返る場合に、例外を投げることを確かめる。 回答の種類が質問と違う応答を、別の種類の回答として扱わない仕様を守る。
       *
       * @given 判定 API が person_1 に択一の項目だけを返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void YesNoの質問に択一の回答だけが返る場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"person_1\":{\"type\":\"choice\",\"choice\":\"billing\",\"confidence\":0.9}}}",
                new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * Yes/No の回答に確信度が無い場合に、例外を投げることを確かめる。 確信度の無い回答を確信度 0 として扱わない仕様を守る。
       *
       * @given 判定 API が person_1 に確率だけを返し、確信度が無い
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void YesNoの確信度が無い場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"person_1\":{\"type\":\"noul\",\"noul\":0.97}}}",
                new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * 択一の質問に Yes/No の回答だけが返る場合に、例外を投げることを確かめる。 回答の種類が質問と違う応答を、別の種類の回答として扱わない仕様を守る。
       *
       * @given 判定 API が link に Yes/No の項目だけを返す
       * @when decide を入力 "共有の入力" と択一の質問 link で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 択一の質問にYesNoの回答だけが返る場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"link\":{\"type\":\"noul\",\"noul\":0.9,\"confidence\":0.9}}}",
                new ByteArrayOutputStream()));
        List<ChoiceQuestion> questions =
            List.of(ChoiceQuestion.of("link", "該当する要員は", billingAccessOptions()));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * 択一の回答に全選択肢の確率が無い場合に、例外を投げることを確かめる。 確率が無い択一は並び順の判断に使えないため、失敗として伝える仕様を守る。
       *
       * @given 判定 API が link に選択肢と確信度だけを返し、確率が無い
       * @when decide を入力 "共有の入力" と択一の質問 link で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 択一の確率が無い場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"link\":{\"type\":\"choice\",\"choice\":\"billing\",\"confidence\":0.9}}}",
                new ByteArrayOutputStream()));
        List<ChoiceQuestion> questions =
            List.of(ChoiceQuestion.of("link", "該当する要員は", billingAccessOptions()));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * 択一の回答に確信度が無い場合に、例外を投げることを確かめる。 確信度の無い択一を確信度 0 として扱わない仕様を守る。
       *
       * @given 判定 API が link に選択肢と確率を返し、確信度が無い
       * @when decide を入力 "共有の入力" と択一の質問 link で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 択一の確信度が無い場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"link\":{\"type\":\"choice\",\"choice\":\"billing\",\"probabilities\":{\"billing\":0.9}}}}",
                new ByteArrayOutputStream()));
        List<ChoiceQuestion> questions =
            List.of(ChoiceQuestion.of("link", "該当する要員は", billingAccessOptions()));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * Yes/No の回答の確率が無い場合に、例外を投げることを確かめる。 種類は合っていても確率が無い回答を確率 0 として扱わない仕様を守る。
       *
       * @given 判定 API が person_1 に種類 noul と確信度だけを返し、確率が無い
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void YesNoの確率が無い場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"person_1\":{\"type\":\"noul\",\"confidence\":0.9}}}",
                new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * 択一の回答の選択肢が無い場合に、例外を投げることを確かめる。 種類は合っていても選んだ選択肢が無い回答を扱わない仕様を守る。
       *
       * @given 判定 API が link に種類 choice、確率、確信度を返し、選んだ選択肢が無い
       * @when decide を入力 "共有の入力" と択一の質問 link で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 択一の選択肢が無い場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"link\":{\"type\":\"choice\",\"probabilities\":{\"billing\":0.9},\"confidence\":0.9}}}",
                new ByteArrayOutputStream()));
        List<ChoiceQuestion> questions =
            List.of(ChoiceQuestion.of("link", "該当する要員は", billingAccessOptions()));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * 選ばれた選択肢が質問の選択肢に無い場合に、例外を投げることを確かめる。 質問に無い選択肢を回答として扱うと、呼び出し側が存在しない候補を選ぶ仕様を守る。
       *
       * @given 判定 API が link に種類 choice で選択肢 "unknown" を返し、質問の選択肢は billing と access
       * @when decide を入力 "共有の入力" と択一の質問 link で実行する
       * @then 選択肢 "unknown" を含む IllegalStateException を投げる
       */
      @Test
      void 選ばれた選択肢が質問の選択肢に無い場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"link\":{\"type\":\"choice\",\"choice\":\"unknown\",\"probabilities\":{\"unknown\":0.9},\"confidence\":0.9}}}",
                new ByteArrayOutputStream()));
        List<ChoiceQuestion> questions =
            List.of(ChoiceQuestion.of("link", "該当する要員は", billingAccessOptions()));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unknown");
      }

      /**
       * 応答の answers が null の場合に、例外を投げることを確かめる。 回答の一覧が無い応答を NullPointerException
       * にせず、原因が分かる例外で伝える仕様を守る。
       *
       * @given 判定 API が answers を null で返す
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then IllegalStateException を投げ、メッセージに answers を含む
       */
      @Test
      void answersがnullの場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(connection(200, "{\"answers\":null}", new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("answers");
      }

      /**
       * 回答の種類が質問の種類と違う場合に、例外を投げることを確かめる。 Yes/No の質問に択一の種類の回答が返る応答を、別の種類として扱わない仕様を守る。
       *
       * @given 判定 API が Yes/No の質問 person_1 に種類 choice の回答を返し、確率と確信度の項目は揃っている
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 回答の種類が質問と違う場合_IllegalStateExceptionを投げる() throws Exception {
        useOpener(
            connection(
                200,
                "{\"answers\":{\"person_1\":{\"type\":\"choice\",\"noul\":0.97,\"confidence\":0.97}}}",
                new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * 設定の URL が未設定の場合に、設定名を含む例外を投げ判定 API を呼ばないことを確かめる。
       * 送信先が分からないまま通信すると、誤った先へ入力を送るため、呼び出し前に止める仕様を守る。
       *
       * @given 設定の URL が未設定
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then 設定名 GEMINI_DECISION_API_URL を含む IllegalStateException を投げ、接続を開かない
       */
      @Test
      void 設定のURLが未設定の場合_IllegalStateExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener = useOpener();
        Properties.removeForTest(DECISION_URL_KEY);
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GEMINI_DECISION_API_URL");
        verifyNoInteractions(opener);
      }

      /**
       * 設定の URL が空の場合に、設定名を含む例外を投げ判定 API を呼ばないことを確かめる。 空の送信先は未設定と同じく扱い、通信を始めない仕様を守る。
       *
       * @given 設定の URL が空文字
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then 設定名 GEMINI_DECISION_API_URL を含む IllegalStateException を投げ、接続を開かない
       */
      @Test
      void 設定のURLが空の場合_IllegalStateExceptionを投げる() throws Exception {
        Gemini.ConnectionOpener opener = useOpener();
        Properties.putForTest(DECISION_URL_KEY, "");
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GEMINI_DECISION_API_URL");
        verifyNoInteractions(opener);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 質問が1件の場合に、その1件の回答を返すことを確かめる。 質問数の下限ちょうどの判定を通す仕様を守る。
       *
       * @given 判定 API が person_1 の回答を返す、Yes/No の質問が1件
       * @when decide を入力 "共有の入力" と質問1件で実行する
       * @then person_1 の確率 0.97 の回答を返す
       */
      @Test
      void 質問が1件の場合_その回答を返す() throws Exception {
        useOpener(connection(200, DECISION_OK, new ByteArrayOutputStream()));
        NoulQuestion person = NoulQuestion.of("person_1", "条件を満たすか");

        DecisionResult result = new Gemini("key").decide("共有の入力", List.of(person));

        assertThat(result.answerOf(person).getProbability()).isEqualTo(0.97);
      }

      /**
       * 質問が1000件の場合に、判定を送り最後の質問の回答を返すことを確かめる。 質問数の上限ちょうどの判定を通す仕様を守る。
       *
       * @given 判定 API が q0 から q999 までの回答を返す、Yes/No の質問が1000件
       * @when decide を入力 "共有の入力" と1000件の質問で実行する
       * @then 最後の質問 q999 の確率 0.5 の回答を返す
       */
      @Test
      void 質問が1000件の場合_最後の質問の回答を返す() throws Exception {
        StringBuilder answers = new StringBuilder("{\"answers\":{");
        for (int i = 0; i < 1000; i++) {
          answers
              .append(i == 0 ? "" : ",")
              .append("\"q")
              .append(i)
              .append("\":{\"type\":\"noul\",\"noul\":0.5,\"confidence\":0.5}");
        }
        useOpener(connection(200, answers.append("}}").toString(), new ByteArrayOutputStream()));
        List<NoulQuestion> questions = manyNoulQuestions(1000);

        DecisionResult result = new Gemini("key").decide("共有の入力", questions);

        assertThat(result.answerOf(questions.get(999)).getProbability()).isEqualTo(0.5);
      }

      /**
       * 安全性のフィルタの error.code の値ごとに、ブロックの例外を投げることを確かめる。 許可された5つの値はいずれもブロックとして扱う仕様を守る。
       *
       * @given 判定 API が 400 を返し、error.code が CSV の各行の値
       * @when decide を入力 "共有の入力" と Yes/No の質問 person_1 で実行する
       * @then DecisionBlockedException を投げる
       */
      @ParameterizedTest
      @ValueSource(
          strings = {
            "safety",
            "prohibited_content",
            "blocklist",
            "image_safety",
            "content_blocked"
          })
      void errorCodeが安全性の値の場合_DecisionBlockedExceptionを投げる(final String code) throws Exception {
        useOpener(connection(400, blockedBody(code), new ByteArrayOutputStream()));
        List<NoulQuestion> questions = List.of(NoulQuestion.of("person_1", "条件を満たすか"));

        assertThatThrownBy(() -> new Gemini("key").decide("共有の入力", questions))
            .isInstanceOf(DecisionBlockedException.class);
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

  /**
   * 判定 API への接続を、与えた接続を呼び出し順に返す差し替えに設定する.
   *
   * @param connections 呼び出し順に返す接続。0件の場合は接続を返さない。呼ばれないことを確かめる場合に使う
   * @return 差し替えた接続の関数のモック
   * @throws IOException 設定中に例外が起きた場合
   */
  private static Gemini.ConnectionOpener useOpener(final HttpURLConnection... connections)
      throws IOException {
    Gemini.ConnectionOpener opener = mock(Gemini.ConnectionOpener.class);
    if (connections.length > 0) {
      when(opener.open(anyString()))
          .thenReturn(connections[0], Arrays.copyOfRange(connections, 1, connections.length));
    }
    Gemini.connectionOpener = opener;
    return opener;
  }

  /**
   * 応答コードと本文を持つ接続のモックを作る.
   *
   * <p>200 の応答は本文を通常の応答として読ませる。200 以外の応答は本文をエラー本文として読ませ、本文が null の場合はエラー本文を持たせない。
   *
   * @param code 応答コード
   * @param body 応答本文。null の場合はエラー本文を持たない
   * @param sent 送信した本文を受け取る出力先
   * @return 接続のモック
   * @throws IOException 設定中に例外が起きた場合
   */
  private static HttpURLConnection connection(
      final int code, final String body, final OutputStream sent) throws IOException {
    HttpURLConnection conn = mock(HttpURLConnection.class);
    when(conn.getResponseCode()).thenReturn(code);
    when(conn.getOutputStream()).thenReturn(sent);
    if (code == HttpURLConnection.HTTP_OK) {
      when(conn.getInputStream()).thenReturn(bytes(body));
    } else if (body != null) {
      when(conn.getErrorStream()).thenReturn(bytes(body));
    }
    return conn;
  }

  /**
   * 文字列を UTF-8 の入力ストリームにする.
   *
   * @param text 文字列
   * @return 入力ストリーム
   */
  private static InputStream bytes(final String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 安全性のフィルタによるブロックの応答本文を作る.
   *
   * @param code error.code の値
   * @return 応答本文
   */
  private static String blockedBody(final String code) {
    return "{\"error\":{\"code\":\"" + code + "\"}}";
  }

  /**
   * 択一の選択肢として billing と access を作る. billing の説明は "請求"、access の説明は空文字とする.
   *
   * @return 選択肢のマップ
   */
  private static Map<String, String> billingAccessOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    options.put("billing", "請求");
    options.put("access", "");
    return options;
  }

  /**
   * Yes/No の質問を指定の件数だけ作る。質問キーは q0 から順に付ける.
   *
   * @param count 質問の件数
   * @return 質問の一覧
   */
  private static List<NoulQuestion> manyNoulQuestions(final int count) {
    return IntStream.range(0, count).mapToObj(i -> NoulQuestion.of("q" + i, "条件を満たすか")).toList();
  }

  /** 構造化出力の読み込み先。 */
  public static class Person {
    /** 名前。 */
    public String name;

    /** 年齢。 */
    public int age;
  }
}
