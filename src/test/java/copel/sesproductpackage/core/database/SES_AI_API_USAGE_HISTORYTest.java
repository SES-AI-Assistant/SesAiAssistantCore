package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.ApiType;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import java.math.BigDecimal;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** {@link SES_AI_API_USAGE_HISTORY} の単体テスト。 */
@TestProperty(
    key = "infrastructure/dynamodb/api-usage-history-table/name",
    value = "api-usage-history")
class SES_AI_API_USAGE_HISTORYTest {

  /** 使用月の値。 */
  private static final String USAGE_MONTH = "202501";

  /** 使用日の値。 */
  private static final String USAGE_DATE = "20250115";

  /** 拡張クライアントのモック。テーブルの作成を確かめるために使う。 */
  private DynamoDbEnhancedClient client;

  /** テーブルのモック。 */
  private DynamoDbTable<SES_AI_API_USAGE_HISTORY> table;

  /** 拡張クライアントの静的メソッドのモック。テスト後に解除する。 */
  private MockedStatic<DynamoDbEnhancedClient> enhanced;

  /** DynamoDB の拡張クライアントをモックに差し替え、テーブルのモックを返すように設定する。 */
  @BeforeEach
  void DynamoDBの拡張クライアントをモックに差し替える() {
    enhanced = mockStatic(DynamoDbEnhancedClient.class);
    DynamoDbEnhancedClient.Builder builder =
        mock(DynamoDbEnhancedClient.Builder.class, RETURNS_SELF);
    client = mock(DynamoDbEnhancedClient.class);
    enhanced.when(DynamoDbEnhancedClient::builder).thenReturn(builder);
    when(builder.build()).thenReturn(client);
    table = mock(DynamoDbTable.class);
    doReturn(table).when(client).table(anyString(), any(TableSchema.class));
  }

  /** 拡張クライアントの静的メソッドの差し替えを解除する。 */
  @AfterEach
  void 拡張クライアントの差し替えを解除する() {
    enhanced.close();
  }

  /**
   * 利用履歴を生成する。プロバイダ、モデル、使用月、ユーザーID、API種別を設定し、使用日を指定する。
   *
   * @param usageDate 使用日
   * @return 生成した利用履歴
   */
  private static SES_AI_API_USAGE_HISTORY history(final String usageDate) {
    SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
    history.setProvider(Provider.OpenAI);
    history.setModel("gpt-4o-mini");
    history.setUsageMonth(USAGE_MONTH);
    history.setUsageDate(usageDate);
    history.setUserId("U001");
    history.setApiType(ApiType.Generate);
    return history;
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 設定値のテーブル名で生成すると、そのテーブル名のテーブルを作ることを確かめる。 保存先のテーブルを設定値で切り替えるためである。
       *
       * @given 設定値のテーブル名が "api-usage-history"
       * @when SES_AI_API_USAGE_HISTORY を空のインスタンスとして生成する
       * @then テーブル名 "api-usage-history" で作成したテーブルを保持する
       */
      @Test
      void テーブル名が設定されている場合_設定値のテーブル名で作成したテーブルを返す() {
        SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();

        verify(client).table(eq("api-usage-history"), any(TableSchema.class));
        assertThat(history.getTable()).isSameAs(table);
      }
    }
  }

  @Nested
  class getPartitionKey {

    @Nested
    class 正常系 {

      /**
       * プロバイダとモデルと使用月からパーティションキーを組み立てることを確かめる。 同じ月の同じモデルの利用を1つの区画にまとめる。
       *
       * @given プロバイダが OpenAI、モデルが "gpt-4o-mini"、使用月が "202501"
       * @when getPartitionKey を実行する
       * @then "OpenAI#gpt-4o-mini#202501" を返す
       */
      @Test
      void プロバイダとモデルと使用月が揃っている場合_パーティションキーを返す() {
        assertThat(history(USAGE_DATE).getPartitionKey()).isEqualTo("OpenAI#gpt-4o-mini#202501");
      }
    }
  }

  @Nested
  class getSortKey {

    @Nested
    class 正常系 {

      /**
       * 使用日がある場合に使用日とユーザーIDとAPI種別からソートキーを組み立てることを確かめる。 同じ日の利用を並べて集計する。
       *
       * @given 使用日が "20250115"、ユーザーIDが "U001"、API種別が Generate
       * @when getSortKey を実行する
       * @then "20250115#U001#Generate" を返す
       */
      @Test
      void 使用日がある場合_ソートキーを返す() {
        assertThat(history(USAGE_DATE).getSortKey()).isEqualTo("20250115#U001#Generate");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 使用日が null の場合にユーザーIDとAPI種別からソートキーを組み立てることを確かめる。 使用日の無い利用も保存できる必要がある。
       *
       * @given 使用日が null、ユーザーIDが "U001"、API種別が Generate
       * @when getSortKey を実行する
       * @then "U001#Generate" を返す
       */
      @Test
      void 使用日がnullの場合_ユーザーIDとAPI種別のソートキーを返す() {
        assertThat(history(null).getSortKey()).isEqualTo("U001#Generate");
      }

      /**
       * 使用日が空文字の場合に使用日を含めないソートキーを組み立てることを確かめる。 空の使用日を連結すると区切り文字だけが残る。
       *
       * @given 使用日が空文字、ユーザーIDが "U001"、API種別が Generate
       * @when getSortKey を実行する
       * @then "U001#Generate" を返す
       */
      @Test
      void 使用日が空文字の場合_ユーザーIDとAPI種別のソートキーを返す() {
        assertThat(history("").getSortKey()).isEqualTo("U001#Generate");
      }
    }
  }

  @Nested
  class getUsageDate {

    @Nested
    class 正常系 {

      /**
       * 設定した使用日をそのまま返すことを確かめる。 使用日はソートキーと集計に使う。
       *
       * @given 使用日が "20250115"
       * @when getUsageDate を実行する
       * @then "20250115" を返す
       */
      @Test
      void 使用日が設定されている場合_設定した使用日を返す() {
        assertThat(history(USAGE_DATE).getUsageDate()).isEqualTo(USAGE_DATE);
      }
    }
  }

  @Nested
  class setUsageDate {

    @Nested
    class 正常系 {

      /**
       * 使用日を指定すると、後から取得した値に反映されることを確かめる。 使用日の設定が取得できないと、ソートキーが古い値のまま保存される。
       *
       * @given 使用日が "20250115" の利用履歴
       * @when setUsageDate を "20250116" で実行する
       * @then getUsageDate が "20250116" を返す
       */
      @Test
      void 使用日を指定した場合_指定した使用日を返す() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);

        history.setUsageDate("20250116");

        assertThat(history.getUsageDate()).isEqualTo("20250116");
      }
    }
  }

  @Nested
  class addInputCount_int {

    @Nested
    class 正常系 {

      /**
       * 入力文字数の初期値に整数を加えると、加えた値になることを確かめる。 利用ごとの入力文字数を集計する。
       *
       * @given 入力文字数が初期値の 0
       * @when addInputCount を 5 で実行する
       * @then 入力文字数に 5 を返す
       */
      @Test
      void 入力文字数が初期値の場合_加算した入力文字数に5を返す() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);

        history.addInputCount(5);

        assertThat(history.getInputCount()).isEqualByComparingTo(new BigDecimal(5));
      }
    }
  }

  @Nested
  class addInputCount_BigDecimal {

    @Nested
    class 正常系 {

      /**
       * 既存の入力文字数に値を加えると、合計になることを確かめる。 同じ利用の入力文字数を積み上げる。
       *
       * @given 入力文字数が 10
       * @when addInputCount を 5 で実行する
       * @then 入力文字数に 15 を返す
       */
      @Test
      void 入力文字数に既存の値がある場合_合計の入力文字数に15を返す() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setInputCount(BigDecimal.TEN);

        history.addInputCount(new BigDecimal(5));

        assertThat(history.getInputCount()).isEqualByComparingTo(new BigDecimal(15));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 入力文字数が null の場合に 0 を基準として加えることを確かめる。 null に加算すると例外になる。
       *
       * @given 入力文字数が null
       * @when addInputCount を 5 で実行する
       * @then 入力文字数に 5 を返す
       */
      @Test
      void 入力文字数がnullの場合_0に加算した入力文字数に5を返す() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setInputCount(null);

        history.addInputCount(new BigDecimal(5));

        assertThat(history.getInputCount()).isEqualByComparingTo(new BigDecimal(5));
      }
    }
  }

  @Nested
  class addOutputCount_int {

    @Nested
    class 正常系 {

      /**
       * 出力文字数の初期値に整数を加えると、加えた値になることを確かめる。 利用ごとの出力文字数を集計する。
       *
       * @given 出力文字数が初期値の 0
       * @when addOutputCount を 3 で実行する
       * @then 出力文字数に 3 を返す
       */
      @Test
      void 出力文字数が初期値の場合_加算した出力文字数に3を返す() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);

        history.addOutputCount(3);

        assertThat(history.getOutputCount()).isEqualByComparingTo(new BigDecimal(3));
      }
    }
  }

  @Nested
  class addOutputCount_BigDecimal {

    @Nested
    class 正常系 {

      /**
       * 既存の出力文字数に値を加えると、合計になることを確かめる。 同じ利用の出力文字数を積み上げる。
       *
       * @given 出力文字数が 4
       * @when addOutputCount を 2 で実行する
       * @then 出力文字数に 6 を返す
       */
      @Test
      void 出力文字数に既存の値がある場合_合計の出力文字数に6を返す() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setOutputCount(new BigDecimal(4));

        history.addOutputCount(new BigDecimal(2));

        assertThat(history.getOutputCount()).isEqualByComparingTo(new BigDecimal(6));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 出力文字数が null の場合に 0 を基準として加えることを確かめる。 null に加算すると例外になる。
       *
       * @given 出力文字数が null
       * @when addOutputCount を 2 で実行する
       * @then 出力文字数に 2 を返す
       */
      @Test
      void 出力文字数がnullの場合_0に加算した出力文字数に2を返す() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setOutputCount(null);

        history.addOutputCount(new BigDecimal(2));

        assertThat(history.getOutputCount()).isEqualByComparingTo(new BigDecimal(2));
      }
    }
  }

  @Nested
  class save {

    @Nested
    class 正常系 {

      /**
       * 必須項目が揃っている場合に現在時刻を設定して登録することを確かめる。 更新日時を残して、最後に使われた時刻を追えるようにする。
       *
       * @given プロバイダが OpenAI、モデルが "gpt-4o-mini"、使用月が "202501"、現在時刻が 2026-01-15 12:00 の日本時間
       * @when save を実行する
       * @then 更新日時に "2026-01-15T03:00:00Z" を設定し、1件を登録する
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @Test
      void 必須項目が揃っている場合_更新日時を現在時刻にして1件を登録する() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);

        history.save();

        assertThat(history.getTimestamp()).isEqualTo("2026-01-15T03:00:00Z");
        verify(table).putItem(history);
      }
    }

    @Nested
    class 境界値 {

      /**
       * プロバイダが null の場合に保存しないことを確かめる。 プロバイダはパーティションキーの一部のため、欠けると区画が決まらない。
       *
       * @given プロバイダが null、モデルが "gpt-4o-mini"、使用月が "202501"
       * @when save を実行する
       * @then DynamoDB への登録を呼ばない
       */
      @Test
      void プロバイダがnullの場合_DynamoDBへの登録を呼ばない() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setProvider(null);

        history.save();

        verify(table, never()).putItem(any(SES_AI_API_USAGE_HISTORY.class));
      }

      /**
       * モデルが null の場合に保存しないことを確かめる。 モデルはパーティションキーの一部のため、欠けると区画が決まらない。
       *
       * @given プロバイダが OpenAI、モデルが null、使用月が "202501"
       * @when save を実行する
       * @then DynamoDB への登録を呼ばない
       */
      @Test
      void モデルがnullの場合_DynamoDBへの登録を呼ばない() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setModel(null);

        history.save();

        verify(table, never()).putItem(any(SES_AI_API_USAGE_HISTORY.class));
      }

      /**
       * モデルが空文字の場合に保存しないことを確かめる。 空のモデルでは区画を分けられない。
       *
       * @given プロバイダが OpenAI、モデルが空文字、使用月が "202501"
       * @when save を実行する
       * @then DynamoDB への登録を呼ばない
       */
      @Test
      void モデルが空文字の場合_DynamoDBへの登録を呼ばない() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setModel("");

        history.save();

        verify(table, never()).putItem(any(SES_AI_API_USAGE_HISTORY.class));
      }

      /**
       * 使用月が null の場合に保存しないことを確かめる。 使用月はパーティションキーの一部のため、欠けると区画が決まらない。
       *
       * @given プロバイダが OpenAI、モデルが "gpt-4o-mini"、使用月が null
       * @when save を実行する
       * @then DynamoDB への登録を呼ばない
       */
      @Test
      void 使用月がnullの場合_DynamoDBへの登録を呼ばない() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setUsageMonth(null);

        history.save();

        verify(table, never()).putItem(any(SES_AI_API_USAGE_HISTORY.class));
      }

      /**
       * 使用月が空文字の場合に保存しないことを確かめる。 空の使用月では区画を分けられない。
       *
       * @given プロバイダが OpenAI、モデルが "gpt-4o-mini"、使用月が空文字
       * @when save を実行する
       * @then DynamoDB への登録を呼ばない
       */
      @Test
      void 使用月が空文字の場合_DynamoDBへの登録を呼ばない() {
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);
        history.setUsageMonth("");

        history.save();

        verify(table, never()).putItem(any(SES_AI_API_USAGE_HISTORY.class));
      }
    }
  }

  @Nested
  class delete {

    @Nested
    class 正常系 {

      /**
       * パーティションキーとソートキーを指定して、レコードを削除することを確かめる。 キーの組み立てが削除対象のレコードと一致する必要がある。
       *
       * @given プロバイダが OpenAI、モデルが "gpt-4o-mini"、使用月が "202501"、使用日が "20250115"
       * @when delete を実行する
       * @then パーティションキーに "OpenAI#gpt-4o-mini#202501" を指定してレコードを削除する
       */
      @Test
      void キーが揃っている場合_キーを指定してレコードを削除する() {
        ArgumentCaptor<Key> captor = ArgumentCaptor.forClass(Key.class);

        history(USAGE_DATE).delete();

        verify(table).deleteItem(captor.capture());
        assertThat(captor.getValue().partitionKeyValue().s())
            .isEqualTo("OpenAI#gpt-4o-mini#202501");
        assertThat(captor.getValue().sortKeyValue().orElseThrow().s())
            .isEqualTo("20250115#U001#Generate");
      }
    }
  }

  @Nested
  class fetch {

    @Nested
    class 正常系 {

      /**
       * 最新のレコードがある場合に、取得した項目で自身の項目を更新することを確かめる。 集計の前に、保存済みの最新の使用量を読み込む。
       *
       * @given 最新のレコードの使用日が "20250116"、入力文字数が 7、更新日時が "2026-01-16T00:00:00Z"
       * @when fetch を実行する
       * @then 使用日を "20250116" に更新し、入力文字数を 7 に更新する
       */
      @Test
      void 最新のレコードがある場合_最新の項目を更新する() {
        SES_AI_API_USAGE_HISTORY latest = history("20250116");
        latest.setInputCount(new BigDecimal(7));
        latest.setTimestamp("2026-01-16T00:00:00Z");
        doReturn(latest).when(table).getItem(any(Consumer.class));
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);

        history.fetch();

        assertThat(history.getUsageDate()).isEqualTo("20250116");
        assertThat(history.getInputCount()).isEqualByComparingTo(new BigDecimal(7));
        assertThat(history.getTimestamp()).isEqualTo("2026-01-16T00:00:00Z");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 最新のレコードが無い場合に項目を変えないことを確かめる。 対象の利用が無いときに、入力中の値を消さない。
       *
       * @given 最新のレコードが null
       * @when fetch を実行する
       * @then 使用日を変えずに何もしない
       */
      @Test
      void 最新のレコードが無い場合_何もしない() {
        doReturn(null).when(table).getItem(any(Consumer.class));
        SES_AI_API_USAGE_HISTORY history = history(USAGE_DATE);

        history.fetch();

        assertThat(history.getUsageDate()).isEqualTo(USAGE_DATE);
      }
    }
  }
}
