package copel.sesproductpackage.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** {@link SesInfoMatcherRequestSqsEntity} の単体テスト。 */
@TestProperty(
    key = "infrastructure/messaging/matcher-queue/url",
    value = "https://sqs.ap-northeast-1.amazonaws.com/123/matcher-queue")
class SesInfoMatcherRequestSqsEntityTest {

  /** SQS クライアントのビルダーの静的メソッドのモック。テスト後に解除する。 */
  private MockedStatic<AmazonSQSClientBuilder> builderStatic;

  /** SQS クライアントの作成をモックに差し替える。 生成時に実 AWS へ接続しないようにするため。 */
  @BeforeEach
  void SQSクライアントの作成をモックに差し替える() {
    builderStatic = mockStatic(AmazonSQSClientBuilder.class);
    AmazonSQSClientBuilder builder = mock(AmazonSQSClientBuilder.class);
    lenient().when(builder.withRegion(any(Regions.class))).thenReturn(builder);
    lenient().when(builder.withEndpointConfiguration(any())).thenReturn(builder);
    lenient().when(builder.withCredentials(any())).thenReturn(builder);
    when(builder.build()).thenReturn(mock(AmazonSQS.class));
    builderStatic.when(AmazonSQSClientBuilder::standard).thenReturn(builder);
  }

  /** SQS クライアントの作成のモックを解除する。 */
  @AfterEach
  void SQSクライアントの作成のモックを解除する() {
    builderStatic.close();
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * リージョンを指定した場合に、Matcher のキューの設定値を使うことを確かめる。 Matcher 向けのメッセージを想定のキューへ向けるためである。
       *
       * @given 設定値 infrastructure/messaging/matcher-queue/url が matcher-queue の URL
       * @when リージョン AP_NORTHEAST_1 で生成する
       * @then キュー名として "matcher-queue" を返す
       */
      @Test
      void リージョンを指定する場合_Matcher用のキュー名を返す() {
        SesInfoMatcherRequestSqsEntity target =
            new SesInfoMatcherRequestSqsEntity(Regions.AP_NORTHEAST_1);

        assertThat(target.getQueueName()).isEqualTo("matcher-queue");
      }
    }
  }

  @Nested
  class fromJson {

    @Nested
    class 正常系 {

      /**
       * 5項目を指定した場合に、各項目を設定したエンティティを返すことを確かめる。 JSON から読み込んだ値を Matcher の処理で使えるようにするためである。
       *
       * @given テナントIDが "T001"、案件IDが "J001"、要員IDが "P001"、案件内容が "案件本文"、要員内容が "要員本文"
       * @when fromJson を5項目で呼び出す
       * @then 5項目をそれぞれの値として持つエンティティを返す
       */
      @Test
      void 五項目を指定する場合_各項目を設定したエンティティを返す() {
        SesInfoMatcherRequestSqsEntity target =
            SesInfoMatcherRequestSqsEntity.fromJson("T001", "J001", "P001", "案件本文", "要員本文");

        assertThat(target.getTenantId()).isEqualTo("T001");
        assertThat(target.getJobId()).isEqualTo("J001");
        assertThat(target.getPersonId()).isEqualTo("P001");
        assertThat(target.getJobContent()).isEqualTo("案件本文");
        assertThat(target.getPersonContent()).isEqualTo("要員本文");
      }

      /**
       * スネークケースの JSON を読み込んだ場合に、fromJson の値を設定したエンティティを返すことを確かめる。 Matcher Lambda が SQS
       * のメッセージ本文をこの形式で受け取るためである。
       *
       * @given JSON が tenant_id に "T001"、job_id に "J001"、person_id に "P001" を持つ
       * @when Jackson で SesInfoMatcherRequestSqsEntity に読み込む
       * @then テナントIDと案件IDと要員IDをそれぞれの値として持つエンティティを返す
       */
      @Test
      void JSONを読み込む場合_各項目を設定したエンティティを返す() throws Exception {
        String json = "{\"tenant_id\":\"T001\",\"job_id\":\"J001\",\"person_id\":\"P001\"}";

        SesInfoMatcherRequestSqsEntity target =
            ObjectMapperFactory.OBJECT_MAPPER.readValue(json, SesInfoMatcherRequestSqsEntity.class);

        assertThat(target.getTenantId()).isEqualTo("T001");
        assertThat(target.getJobId()).isEqualTo("J001");
        assertThat(target.getPersonId()).isEqualTo("P001");
      }
    }
  }

  @Nested
  class getMessageBody {

    @Nested
    class 異常系 {

      /**
       * 送信用の本文を作ろうとした場合に例外を投げることを確かめる。 本番では Matcher 向けの送信を Register Lambda で行い、このクラスからは送らないためである。
       *
       * @given 案件IDが "J001" のエンティティ
       * @when getMessageBody を実行する
       * @then UnsupportedOperationException を投げる
       */
      @Test
      void 送信用の本文を作る場合_UnsupportedOperationExceptionを投げる() {
        SesInfoMatcherRequestSqsEntity target =
            SesInfoMatcherRequestSqsEntity.fromJson("T001", "J001", null, null, null);

        assertThatThrownBy(target::getMessageBody)
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class isValid {

    @Nested
    class 正常系 {

      /**
       * 案件IDとテナントIDがある場合に有効と判定することを確かめる。 案件を指定したマッチング要求を処理対象にするためである。
       *
       * @given 案件IDが "J001"、テナントIDが "T001"、要員の指定が無い
       * @when isValid を実行する
       * @then true を返す
       */
      @Test
      void 案件IDとテナントIDがある場合_trueを返す() {
        SesInfoMatcherRequestSqsEntity target =
            SesInfoMatcherRequestSqsEntity.fromJson("T001", "J001", null, null, null);

        assertThat(target.isValid()).isTrue();
      }

      /**
       * 要員内容とテナントIDがある場合に有効と判定することを確かめる。 要員の本文だけを指定したマッチング要求も処理対象にするためである。
       *
       * @given 要員内容が "要員本文"、テナントIDが "T001"、案件の指定が無い
       * @when isValid を実行する
       * @then true を返す
       */
      @Test
      void 要員内容とテナントIDがある場合_trueを返す() {
        SesInfoMatcherRequestSqsEntity target =
            SesInfoMatcherRequestSqsEntity.fromJson("T001", null, null, null, "要員本文");

        assertThat(target.isValid()).isTrue();
      }

      /**
       * JSON に変換した場合に valid の項目を含めないことを確かめる。 判定結果は送信データの一部ではないため、JSON に出さない。
       *
       * @given 案件IDが "J001"、テナントIDが "T001" のエンティティ
       * @when JSON に変換する
       * @then valid の項目を含まない JSON を返す
       */
      @Test
      void JSONに変換する場合_validを含まないJSONを返す() throws Exception {
        SesInfoMatcherRequestSqsEntity target =
            SesInfoMatcherRequestSqsEntity.fromJson("T001", "J001", null, null, null);

        String json = ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(target);

        assertThat(json).doesNotContain("valid");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 案件も要員も指定が無い場合に無効と判定することを確かめる。 マッチングの対象が無いため、処理を進めてはいけないためである。
       *
       * @given 案件IDも案件内容も要員IDも要員内容も無く、テナントIDが "T001"
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void 案件も要員も指定が無い場合_falseを返す() {
        SesInfoMatcherRequestSqsEntity target =
            SesInfoMatcherRequestSqsEntity.fromJson("T001", null, null, null, null);

        assertThat(target.isValid()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * テナントIDが空文字の場合に無効と判定することを確かめる。 テナントIDが無いと他テナントのデータを扱うおそれがあるためである。
       *
       * @given 案件IDが "J001"、テナントIDが空文字
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void テナントIDが空文字の場合_falseを返す() {
        SesInfoMatcherRequestSqsEntity target =
            SesInfoMatcherRequestSqsEntity.fromJson("", "J001", null, null, null);

        assertThat(target.isValid()).isFalse();
      }

      /**
       * テナントIDが null の場合に無効と判定することを確かめる。 テナントIDが無いと他テナントのデータを扱うおそれがあるためである。
       *
       * @given 案件IDが "J001"、テナントIDが null
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void テナントIDがnullの場合_falseを返す() {
        SesInfoMatcherRequestSqsEntity target =
            SesInfoMatcherRequestSqsEntity.fromJson(null, "J001", null, null, null);

        assertThat(target.isValid()).isFalse();
      }
    }
  }

  @Nested
  class hasJob {

    @Nested
    class 正常系 {

      /**
       * 案件IDを持つ場合に案件情報ありと判定することを確かめる。 案件IDだけで案件を指定するケースを扱うためである。
       *
       * @given 案件IDが "J001"、案件内容が null
       * @when hasJob を実行する
       * @then true を返す
       */
      @Test
      void 案件IDがある場合_trueを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", "J001", null, null, null).hasJob())
            .isTrue();
      }

      /**
       * 案件IDが無く案件内容だけを持つ場合に案件情報ありと判定することを確かめる。 本文だけで案件を渡すケースを扱うためである。
       *
       * @given 案件IDが null、案件内容が "案件本文"
       * @when hasJob を実行する
       * @then true を返す
       */
      @Test
      void 案件内容だけがある場合_trueを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", null, null, "案件本文", null).hasJob())
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDと案件内容がどちらも空文字の場合に案件情報なしと判定することを確かめる。 空の文字列を案件として扱うと、空のマッチングを実行するためである。
       *
       * @given 案件IDが空文字、案件内容が空文字
       * @when hasJob を実行する
       * @then false を返す
       */
      @Test
      void 案件IDと案件内容が空文字の場合_falseを返す() {
        assertThat(SesInfoMatcherRequestSqsEntity.fromJson("T001", "", null, "", null).hasJob())
            .isFalse();
      }
    }
  }

  @Nested
  class hasPerson {

    @Nested
    class 正常系 {

      /**
       * 要員IDを持つ場合に要員情報ありと判定することを確かめる。 要員IDだけで要員を指定するケースを扱うためである。
       *
       * @given 要員IDが "P001"、要員内容が null
       * @when hasPerson を実行する
       * @then true を返す
       */
      @Test
      void 要員IDがある場合_trueを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", null, "P001", null, null)
                    .hasPerson())
            .isTrue();
      }

      /**
       * 要員IDが無く要員内容だけを持つ場合に要員情報ありと判定することを確かめる。 本文だけで要員を渡すケースを扱うためである。
       *
       * @given 要員IDが null、要員内容が "要員本文"
       * @when hasPerson を実行する
       * @then true を返す
       */
      @Test
      void 要員内容だけがある場合_trueを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", null, null, null, "要員本文")
                    .hasPerson())
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null で要員内容も空文字の場合に要員情報なしと判定することを確かめる。 空の要員を処理すると、マッチングの結果が意味を持たないためである。
       *
       * @given 要員IDが null、要員内容が空文字
       * @when hasPerson を実行する
       * @then false を返す
       */
      @Test
      void 要員IDがnullで要員内容が空文字の場合_falseを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", null, null, null, "").hasPerson())
            .isFalse();
      }
    }
  }

  @Nested
  class hasJobId {

    @Nested
    class 正常系 {

      /**
       * 案件IDを持つ場合に true を返すことを確かめる。 案件IDで案件を読み込めるかどうかの判定に使うためである。
       *
       * @given 案件IDが "J001"
       * @when hasJobId を実行する
       * @then true を返す
       */
      @Test
      void 案件IDがJ001の場合_trueを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", "J001", null, null, null)
                    .hasJobId())
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが空文字の場合に false を返すことを確かめる。 空の案件IDで検索すると、案件が1件も見つからないためである。
       *
       * @given 案件IDが空文字
       * @when hasJobId を実行する
       * @then false を返す
       */
      @Test
      void 案件IDが空文字の場合_falseを返す() {
        assertThat(SesInfoMatcherRequestSqsEntity.fromJson("T001", "", null, null, null).hasJobId())
            .isFalse();
      }

      /**
       * 案件IDが null の場合に false を返すことを確かめる。 案件IDが無い要求を案件の検索に使わないためである。
       *
       * @given 案件IDが null
       * @when hasJobId を実行する
       * @then false を返す
       */
      @Test
      void 案件IDがnullの場合_falseを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", null, null, null, null).hasJobId())
            .isFalse();
      }
    }
  }

  @Nested
  class hasPersonId {

    @Nested
    class 正常系 {

      /**
       * 要員IDを持つ場合に true を返すことを確かめる。 要員IDで要員を読み込めるかどうかの判定に使うためである。
       *
       * @given 要員IDが "P001"
       * @when hasPersonId を実行する
       * @then true を返す
       */
      @Test
      void 要員IDがP001の場合_trueを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", null, "P001", null, null)
                    .hasPersonId())
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが空文字の場合に false を返すことを確かめる。 空の要員IDで検索すると、要員が1件も見つからないためである。
       *
       * @given 要員IDが空文字
       * @when hasPersonId を実行する
       * @then false を返す
       */
      @Test
      void 要員IDが空文字の場合_falseを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", null, "", null, null).hasPersonId())
            .isFalse();
      }

      /**
       * 要員IDが null の場合に false を返すことを確かめる。 要員IDが無い要求を要員の検索に使わないためである。
       *
       * @given 要員IDが null
       * @when hasPersonId を実行する
       * @then false を返す
       */
      @Test
      void 要員IDがnullの場合_falseを返す() {
        assertThat(
                SesInfoMatcherRequestSqsEntity.fromJson("T001", null, null, null, null)
                    .hasPersonId())
            .isFalse();
      }
    }
  }
}
