package copel.sesproductpackage.core.api.markitdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.stefanbirkner.systemlambda.SystemLambda;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

/** {@link MarkItDown} の単体テスト。 */
class MarkItDownTest {

  /** invoke が出力するログを集めるアペンダー。ログの重大度を確かめるテストだけが使う。 */
  private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

  @BeforeEach
  void ログを集める() {
    logAppender.start();
    ((Logger) LoggerFactory.getLogger(MarkItDown.class)).addAppender(logAppender);
  }

  @AfterEach
  void ログの収集を止める() {
    ((Logger) LoggerFactory.getLogger(MarkItDown.class)).detachAppender(logAppender);
    logAppender.stop();
  }

  @BeforeEach
  void setFunctionName() {
    Properties.putForTest(SsmParameterKey.MARKITDOWN_NAME.getKey(), "markitdown-fn");
  }

  @AfterEach
  void removeFunctionName() {
    Properties.removeForTest(SsmParameterKey.MARKITDOWN_NAME.getKey());
  }

  private static final String SUCCESS_JSON =
      "{\"success\":true,\"markdown\":\"# T\",\"title\":\"T\"}";

  @Nested
  class コンストラクタ {

    @Nested
    class 異常系 {

      /**
       * 直接インスタンス化した場合に、UnsupportedOperationException を投げることを確かめる。
       * 静的メソッドだけの部品として使わせ、インスタンスを作らせないことを守る。
       *
       * @given 非公開のコンストラクタがある
       * @when リフレクションでコンストラクタを呼び出す
       * @then UnsupportedOperationException を原因とする例外を投げる
       */
      @Test
      void インスタンス化する場合_UnsupportedOperationExceptionを投げる() throws Exception {
        Constructor<MarkItDown> constructor = MarkItDown.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
            .isInstanceOf(InvocationTargetException.class)
            .hasCauseInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class invoke {

    @Nested
    class 正常系 {

      /**
       * 応答が成功の場合に、変換結果を返すことを確かめる。 変換した Markdown を後続の処理で使うため、応答の内容を読み取り、設定された関数名で呼び出すことを守る。
       *
       * @given 関数の応答が成功の JSON で、エラーは無く、関数名の設定が markitdown-fn である
       * @when invoke をリクエストで呼び出す
       * @then 成功の結果と Markdown の "# T" を返し、関数名 markitdown-fn で呼び出す
       */
      @Test
      void 応答が成功の場合_変換結果を返す() throws Exception {
        try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
          LambdaClient client = prepareClient(lc);
          when(client.invoke(any(InvokeRequest.class))).thenReturn(payload(SUCCESS_JSON, null));

          MarkItDown.MarkitdownLambdaResponseEntity result = MarkItDown.invoke(request());

          assertThat(result.isSuccess()).isTrue();
          assertThat(result.getMarkdown()).isEqualTo("# T");
          ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
          verify(client).invoke(captor.capture());
          assertThat(captor.getValue().functionName()).isEqualTo("markitdown-fn");
        }
      }

      /**
       * AWS_REGION が指定されている場合に、そのリージョンでクライアントを作ることを確かめる。 関数を置いたリージョンへ呼び出すため、環境変数の値を使うことを守る。
       *
       * @given 環境変数 AWS_REGION が us-east-1 である
       * @when invoke をリクエストで呼び出す
       * @then us-east-1 のリージョンでクライアントを作る
       */
      @Test
      void AWS_REGIONが指定されている場合_そのリージョンを使うクライアントを返す() throws Exception {
        SystemLambda.withEnvironmentVariable("AWS_REGION", "us-east-1")
            .execute(
                () -> {
                  try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
                    LambdaClientBuilder builder = mock(LambdaClientBuilder.class, RETURNS_SELF);
                    LambdaClient client = mock(LambdaClient.class);
                    when(builder.build()).thenReturn(client);
                    lc.when(LambdaClient::builder).thenReturn(builder);
                    when(client.invoke(any(InvokeRequest.class)))
                        .thenReturn(payload(SUCCESS_JSON, null));

                    MarkItDown.invoke(request());

                    verify(builder).region(Region.US_EAST_1);
                  }
                });
      }
    }

    @Nested
    class 異常系 {

      /**
       * リクエストを JSON にできない場合に、IllegalStateException を投げることを確かめる。 変換に失敗したまま呼び出しを続けないことを守る。
       *
       * @given url の取得で例外を投げるリクエストがある
       * @when invoke をそのリクエストで呼び出す
       * @then IllegalStateException を投げる
       */
      @Test
      void リクエストをJSONにできない場合_IllegalStateExceptionを投げる() {
        MarkItDown.MarkitdownLambdaRequestEntity broken =
            new MarkItDown.MarkitdownLambdaRequestEntity() {
              @Override
              public String getUrl() {
                throw new IllegalStateException("取得失敗");
              }
            };

        assertThatThrownBy(() -> MarkItDown.invoke(broken))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JSON 化");
      }

      /**
       * 関数がエラーを返す場合に、IllegalStateException を投げることを確かめる。 関数のエラーを成功として扱わないことを守る。
       *
       * @given 関数の応答に functionError と本文がある
       * @when invoke をリクエストで呼び出す
       * @then 実行エラーを示す IllegalStateException を投げ、WARN を出力する
       */
      @Test
      void 関数がエラーを返す場合_IllegalStateExceptionを投げる() throws Exception {
        try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
          LambdaClient client = prepareClient(lc);
          when(client.invoke(any(InvokeRequest.class)))
              .thenReturn(payload("{\"err\":1}", "Unhandled"));

          assertThatThrownBy(() -> MarkItDown.invoke(request()))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Lambda 実行エラー");
          assertThat(logAppender.list).extracting(ILoggingEvent::getLevel).containsOnly(Level.WARN);
        }
      }

      /**
       * 関数がエラーを返し本文が無い場合に、IllegalStateException を投げることを確かめる。 本文が無くても、エラーを成功として扱わないことを守る。
       *
       * @given 関数の応答に functionError があり、本文が無い
       * @when invoke をリクエストで呼び出す
       * @then 実行エラーを示す IllegalStateException を投げ、WARN を出力する
       */
      @Test
      void 関数がエラーで本文が無い場合_IllegalStateExceptionを投げる() throws Exception {
        try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
          LambdaClient client = prepareClient(lc);
          when(client.invoke(any(InvokeRequest.class)))
              .thenReturn(InvokeResponse.builder().functionError("Unhandled").build());

          assertThatThrownBy(() -> MarkItDown.invoke(request()))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Lambda 実行エラー");
          assertThat(logAppender.list).extracting(ILoggingEvent::getLevel).containsOnly(Level.WARN);
        }
      }

      /**
       * 応答の本文が無い場合に、IllegalStateException を投げることを確かめる。 本文の無い応答を変換結果として扱わないことを守る。
       *
       * @given 関数の応答に本文が無く、functionError も無い
       * @when invoke をリクエストで呼び出す
       * @then 応答が空であることを示す IllegalStateException を投げる
       */
      @Test
      void 応答の本文が無い場合_IllegalStateExceptionを投げる() throws Exception {
        try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
          LambdaClient client = prepareClient(lc);
          when(client.invoke(any(InvokeRequest.class)))
              .thenReturn(InvokeResponse.builder().build());

          assertThatThrownBy(() -> MarkItDown.invoke(request()))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("空です");
        }
      }

      /**
       * 応答の本文が JSON として解析できない場合に、IllegalStateException を投げることを確かめる。 解析できない応答で誤った結果を返さないことを守る。
       *
       * @given 関数の応答の本文が JSON ではない文字列である
       * @when invoke をリクエストで呼び出す
       * @then 解析に失敗したことを示す IllegalStateException を投げる
       */
      @Test
      void 応答の本文がJSONとして解析できない場合_IllegalStateExceptionを投げる() throws Exception {
        try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
          LambdaClient client = prepareClient(lc);
          when(client.invoke(any(InvokeRequest.class))).thenReturn(payload("not json", null));

          assertThatThrownBy(() -> MarkItDown.invoke(request()))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("解析に失敗");
        }
      }

      /**
       * 関数の呼び出し自体が失敗する場合に、IllegalStateException を投げることを確かめる。 呼び出しの失敗を原因付きで呼び出し元に伝えることを守る。
       *
       * @given invoke が LambdaException を投げる
       * @when invoke をリクエストで呼び出す
       * @then 呼び出しに失敗したことを示す IllegalStateException を投げ、原因を LambdaException とし、WARN を出力する
       */
      @Test
      void 呼び出しが失敗する場合_IllegalStateExceptionを投げる() throws Exception {
        try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
          LambdaClient client = prepareClient(lc);
          when(client.invoke(any(InvokeRequest.class)))
              .thenThrow(LambdaException.builder().message("boom").build());

          assertThatThrownBy(() -> MarkItDown.invoke(request()))
              .isInstanceOf(IllegalStateException.class)
              .hasCauseInstanceOf(LambdaException.class);
          assertThat(logAppender.list).extracting(ILoggingEvent::getLevel).containsOnly(Level.WARN);
        }
      }

      /**
       * 関数名が未設定の場合に、IllegalStateException を投げることを確かめる。 関数名が無いまま呼び出して AWS
       * 側の不明な例外にしないよう、呼び出し前に失敗させることを守る。
       *
       * @given 設定 MARKITDOWN_NAME を削除している
       * @when invoke をリクエストで呼び出す
       * @then 関数名が未設定であることを示す IllegalStateException を投げる
       */
      @Test
      void 関数名が未設定の場合_IllegalStateExceptionを投げる() {
        Properties.removeForTest(SsmParameterKey.MARKITDOWN_NAME.getKey());

        assertThatThrownBy(() -> MarkItDown.invoke(request()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("関数名が未設定");
      }
    }

    @Nested
    class 境界値 {

      /**
       * AWS_REGION が空白だけの場合に、既定のリージョンでクライアントを作ることを確かめる。 空白の設定でリージョンを解決できないことを避けることを守る。
       *
       * @given 環境変数 AWS_REGION が空白だけである
       * @when invoke をリクエストで呼び出す
       * @then 既定の ap-northeast-1 のリージョンでクライアントを作る
       */
      @Test
      void AWS_REGIONが空白だけの場合_既定のリージョンを使うクライアントを返す() throws Exception {
        SystemLambda.withEnvironmentVariable("AWS_REGION", "  ")
            .execute(
                () -> {
                  try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
                    LambdaClientBuilder builder = mock(LambdaClientBuilder.class, RETURNS_SELF);
                    LambdaClient client = mock(LambdaClient.class);
                    when(builder.build()).thenReturn(client);
                    lc.when(LambdaClient::builder).thenReturn(builder);
                    when(client.invoke(any(InvokeRequest.class)))
                        .thenReturn(payload(SUCCESS_JSON, null));

                    MarkItDown.invoke(request());

                    verify(builder).region(Region.AP_NORTHEAST_1);
                  }
                });
      }

      /**
       * AWS_REGION が未設定の場合に、既定のリージョンでクライアントを作ることを確かめる。 未設定でも呼び出しを続けられるようにすることを守る。
       *
       * @given 環境変数 AWS_REGION が未設定である
       * @when invoke をリクエストで呼び出す
       * @then 既定の ap-northeast-1 のリージョンでクライアントを作る
       */
      @Test
      void AWS_REGIONが未設定の場合_既定のリージョンを使うクライアントを返す() throws Exception {
        SystemLambda.withEnvironmentVariable("AWS_REGION", null)
            .execute(
                () -> {
                  try (MockedStatic<LambdaClient> lc = mockStatic(LambdaClient.class)) {
                    LambdaClientBuilder builder = mock(LambdaClientBuilder.class, RETURNS_SELF);
                    LambdaClient client = mock(LambdaClient.class);
                    when(builder.build()).thenReturn(client);
                    lc.when(LambdaClient::builder).thenReturn(builder);
                    when(client.invoke(any(InvokeRequest.class)))
                        .thenReturn(payload(SUCCESS_JSON, null));

                    MarkItDown.invoke(request());

                    verify(builder).region(Region.AP_NORTHEAST_1);
                  }
                });
      }
    }
  }

  /**
   * LambdaClient の生成を差し替え、テスト用のクライアントを返す.
   *
   * @param lc LambdaClient の静的モック
   * @return LambdaClient のモック
   */
  private static LambdaClient prepareClient(final MockedStatic<LambdaClient> lc) {
    LambdaClientBuilder builder = mock(LambdaClientBuilder.class, RETURNS_SELF);
    LambdaClient client = mock(LambdaClient.class);
    when(builder.build()).thenReturn(client);
    lc.when(LambdaClient::builder).thenReturn(builder);
    return client;
  }

  /**
   * 本文とエラー種別を持つ関数の応答を作る.
   *
   * @param body 本文
   * @param functionError 関数のエラー種別（無い場合は null）
   * @return 関数の応答
   */
  private static InvokeResponse payload(final String body, final String functionError) {
    InvokeResponse.Builder builder =
        InvokeResponse.builder().payload(SdkBytes.fromUtf8String(body));
    if (functionError != null) {
      builder.functionError(functionError);
    }
    return builder.build();
  }

  /**
   * テスト用のリクエストを作る.
   *
   * @return リクエスト
   */
  private static MarkItDown.MarkitdownLambdaRequestEntity request() {
    return MarkItDown.MarkitdownLambdaRequestEntity.builder()
        .url("https://a.example/b.pdf")
        .build();
  }
}
