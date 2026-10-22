package copel.sesproductpackage.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.api.line.LineMessagingApi;
import copel.sesproductpackage.core.unit.RequestType;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** {@link SesInfoRegisterRequestSqsEntity} の単体テスト。 */
@TestProperty(
    key = "infrastructure/messaging/register-queue/url",
    value = "https://sqs.ap-northeast-1.amazonaws.com/123/register-queue")
class SesInfoRegisterRequestSqsEntityTest {

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

  /**
   * 種別に応じた、必須項目をすべて持つエンティティを作る。
   *
   * @param requestType リクエスト種別
   * @return 全項目に値を持つエンティティ
   */
  private static SesInfoRegisterRequestSqsEntity 全項目を持つエンティティを作る(final RequestType requestType) {
    SesInfoRegisterRequestSqsEntity entity = new SesInfoRegisterRequestSqsEntity();
    entity.setRequestType(requestType);
    entity.setFromGroup("WEBAPP");
    entity.setFromId("U001");
    entity.setFromName("山田");
    entity.setRawContent("本文");
    entity.setFileName("skillsheet.xlsx");
    return entity;
  }

  /**
   * 指定した項目の値を null にする。
   *
   * @param entity 対象のエンティティ
   * @param field 項目名（fromGroup、fromId、fromName、rawContent、fileName のいずれか）
   */
  private static void 項目をnullにする(final SesInfoRegisterRequestSqsEntity entity, final String field) {
    switch (field) {
      case "fromGroup" -> entity.setFromGroup(null);
      case "fromId" -> entity.setFromId(null);
      case "fromName" -> entity.setFromName(null);
      case "rawContent" -> entity.setRawContent(null);
      case "fileName" -> entity.setFileName(null);
      default -> throw new IllegalArgumentException(field);
    }
  }

  @Nested
  class コンストラクタ_Regions {

    @Nested
    class 正常系 {

      /**
       * リージョンだけを指定した場合に、登録用の設定値からキュー名を決めることを確かめる。 登録 Lambda の送信先を設定値で切り替えられるようにするためである。
       *
       * @given 設定値 infrastructure/messaging/register-queue/url が register-queue の URL
       * @when リージョン AP_NORTHEAST_1 で生成する
       * @then キュー名として "register-queue" を返す
       */
      @Test
      void リージョンを指定する場合_登録用のキュー名を返す() {
        SesInfoRegisterRequestSqsEntity target =
            new SesInfoRegisterRequestSqsEntity(Regions.AP_NORTHEAST_1);

        assertThat(target.getQueueName()).isEqualTo("register-queue");
      }
    }
  }

  @Nested
  class コンストラクタ_Regions_String {

    @Nested
    class 正常系 {

      /**
       * キューの URL を指定した場合に、その URL のキュー名を使うことを確かめる。 テスト用や別キューへ送るときに、設定値を読まずに送信先を決められるようにするためである。
       *
       * @given キューの URL が custom-queue の URL
       * @when リージョン AP_NORTHEAST_1 とキューの URL を指定して生成する
       * @then キュー名として "custom-queue" を返す
       */
      @Test
      void キューURLを指定する場合_指定したURLのキュー名を返す() {
        SesInfoRegisterRequestSqsEntity target =
            new SesInfoRegisterRequestSqsEntity(
                Regions.AP_NORTHEAST_1,
                "https://sqs.ap-northeast-1.amazonaws.com/123/custom-queue");

        assertThat(target.getQueueName()).isEqualTo("custom-queue");
      }
    }
  }

  @Nested
  class addRawContent {

    @Nested
    class 正常系 {

      /**
       * 本文が null の場合に、追加する内容をそのまま本文に設定することを確かめる。 初回の追加で本文を欠けさせないためである。
       *
       * @given rawContent が null
       * @when addRawContent を "追加本文" で実行する
       * @then rawContent として "追加本文" を返す
       */
      @Test
      void rawContentがnullの場合_追加した内容を返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();

        target.addRawContent("追加本文");

        assertThat(target.getRawContent()).isEqualTo("追加本文");
      }

      /**
       * 既存の本文がある場合に、追加する内容を末尾に連結することを確かめる。 分割されて届いた本文を順番どおりに組み立てるためである。
       *
       * @given rawContent が "前半"
       * @when addRawContent を "後半" で実行する
       * @then rawContent として "前半後半" を返す
       */
      @Test
      void 既存の本文がある場合_末尾に連結した本文を返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRawContent("前半");

        target.addRawContent("後半");

        assertThat(target.getRawContent()).isEqualTo("前半後半");
      }
    }
  }

  @Nested
  class getMessageBody {

    @Nested
    class 正常系 {

      /**
       * リクエスト種別と送信元を持つ場合に、スネークケースのキーで JSON に含めることを確かめる。 登録 Lambda がこのキー名で内容を読むためである。
       *
       * @given 種別が ScreenMessage、送信元グループが "WEBAPP"、送信者IDが "U001"
       * @when getMessageBody を実行する
       * @then request_type と from_group と from_id のキーを持つ JSON 本文を返す
       */
      @Test
      void 種別と送信元を持つ場合_スネークケースのキーでJSON本文を返す() throws Exception {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.ScreenMessage);
        target.setFromGroup("WEBAPP");
        target.setFromId("U001");

        String json = target.getMessageBody();

        assertThat(json)
            .contains("\"request_type\":\"ScreenMessage\"")
            .contains("\"from_group\":\"WEBAPP\"")
            .contains("\"from_id\":\"U001\"");
      }

      /**
       * メール件名がある場合に、mail_subject のキーで JSON に含めることを確かめる。 登録先のテーブルへ件名を反映させるためである。
       *
       * @given メール件名が "案件のご紹介"
       * @when getMessageBody を実行する
       * @then mail_subject に "案件のご紹介" を持つ JSON 本文を返す
       */
      @Test
      void メール件名がある場合_mail_subjectで件名を含むJSON本文を返す() throws Exception {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.EmailMessage);
        target.setMailSubject("案件のご紹介");

        String json = target.getMessageBody();

        assertThat(json).contains("\"mail_subject\":\"案件のご紹介\"").doesNotContain("mailSubject");
      }

      /**
       * ウォッチ期間が 14 の場合に、watch_period のキーで JSON に含めることを確かめる。 登録 Lambda が指定された期間をそのまま使うためである。
       *
       * @given ウォッチ期間が 14
       * @when getMessageBody を実行する
       * @then watch_period に 14 を持つ JSON 本文を返す
       */
      @Test
      void ウォッチ期間が14の場合_watch_periodで14を含むJSON本文を返す() throws Exception {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.ScreenMessage);
        target.setWatchPeriod(14);

        String json = target.getMessageBody();

        assertThat(json).contains("\"watch_period\":14").doesNotContain("watchPeriod");
      }

      /**
       * JSON に変換してから読み込むと、元の値に戻ることを確かめる。 Register Lambda が Web から送られた内容を失わずに読み込むためである。
       *
       * @given 種別が ScreenMessage、送信者IDが "U001"、本文が "本文"、メール件名が "件名"、ウォッチ期間が 3
       * @when getMessageBody の結果を SesInfoRegisterRequestSqsEntity に読み込む
       * @then 種別と送信者IDと本文と件名とウォッチ期間が元の値を返す
       */
      @Test
      void JSONに変換して読み込む場合_元の値を持つエンティティを返す() throws Exception {
        SesInfoRegisterRequestSqsEntity source = new SesInfoRegisterRequestSqsEntity();
        source.setRequestType(RequestType.ScreenMessage);
        source.setFromId("U001");
        source.setRawContent("本文");
        source.setMailSubject("件名");
        source.setWatchPeriod(3);

        SesInfoRegisterRequestSqsEntity parsed =
            ObjectMapperFactory.OBJECT_MAPPER.readValue(
                source.getMessageBody(), SesInfoRegisterRequestSqsEntity.class);

        assertThat(parsed.getRequestType()).isEqualTo(RequestType.ScreenMessage);
        assertThat(parsed.getFromId()).isEqualTo("U001");
        assertThat(parsed.getRawContent()).isEqualTo("本文");
        assertThat(parsed.getMailSubject()).isEqualTo("件名");
        assertThat(parsed.getWatchPeriod()).isEqualTo(3);
      }
    }

    @Nested
    class 異常系 {

      /**
       * JSON への変換が失敗した場合に、例外を握りつぶさず呼び出し元へ伝えることを確かめる。 変換失敗を黙って null の本文で送ると、空のメッセージが登録されてしまうためである。
       *
       * @given JSON への変換が JsonProcessingException を投げる ObjectMapper に差し替えている
       * @when getMessageBody を実行する
       * @then JsonProcessingException を投げる
       */
      @Test
      void JSON変換に失敗する場合_JsonProcessingExceptionを投げる() throws Exception {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(any())).thenThrow(new JsonProcessingException("変換失敗") {});
        ObjectMapper original = SesInfoRegisterRequestSqsEntity.objectMapper;
        SesInfoRegisterRequestSqsEntity.objectMapper = failing;
        try {
          SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
          assertThatThrownBy(target::getMessageBody).isInstanceOf(JsonProcessingException.class);
        } finally {
          SesInfoRegisterRequestSqsEntity.objectMapper = original;
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 件名が null の場合に、mail_subject を null として含めることを確かめる。 件名ヘッダが無いメールでも、登録 Lambda
       * が項目の欠落を判断できるようにするためである。
       *
       * @given メール件名が null
       * @when getMessageBody を実行する
       * @then mail_subject に null を持つ JSON 本文を返す
       */
      @Test
      void メール件名がnullの場合_mail_subjectをnullとしたJSON本文を返す() throws Exception {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.EmailMessage);
        target.setMailSubject(null);

        String json = target.getMessageBody();

        assertThat(json).contains("\"mail_subject\":null");
      }

      /**
       * ウォッチ期間が null の場合に、watch_period を null として含めることを確かめる。 未指定時は登録 Lambda が既定値を適用するため、null のまま送る。
       *
       * @given ウォッチ期間が null
       * @when getMessageBody を実行する
       * @then watch_period に null を持つ JSON 本文を返す
       */
      @Test
      void ウォッチ期間がnullの場合_watch_periodをnullとしたJSON本文を返す() throws Exception {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.ScreenMessage);
        target.setWatchPeriod(null);

        String json = target.getMessageBody();

        assertThat(json).contains("\"watch_period\":null");
      }
    }
  }

  @Nested
  class isValid {

    @Nested
    class 正常系 {

      /**
       * 種別ごとの必須項目をすべて持つ場合に有効と判定することを確かめる。 必須項目が揃ったリクエストだけを登録処理へ進めるためである。
       *
       * @given requestType が CsvSource の各行の種別、必須項目をすべて持つ
       * @when isValid を実行する
       * @then true を返す
       */
      @ParameterizedTest
      @CsvSource({
        "LineMessage",
        "LineFile",
        "EmailMessage",
        "EmailFile",
        "OtherMessage",
        "OtherFile",
        "ScreenMessage",
        "ScreenFile"
      })
      void 種別が必須項目を持つ場合_trueを返す(final String requestType) {
        SesInfoRegisterRequestSqsEntity target = 全項目を持つエンティティを作る(RequestType.valueOf(requestType));

        assertThat(target.isValid()).isTrue();
      }

      /**
       * 種別ごとの必須項目が欠けている場合に無効と判定することを確かめる。 欠けた項目で登録処理を進めると、登録できない値が DB へ入るためである。
       *
       * @given requestType が CsvSource の1列目の種別、必須項目が CsvSource の2列目の項目だけ null
       * @when isValid を実行する
       * @then false を返す
       */
      @ParameterizedTest
      @CsvSource({
        "LineMessage, fromGroup",
        "LineMessage, fromId",
        "LineMessage, fromName",
        "LineMessage, rawContent",
        "LineFile, fromGroup",
        "LineFile, fromId",
        "LineFile, fromName",
        "LineFile, fileName",
        "EmailMessage, fromGroup",
        "EmailMessage, fromId",
        "EmailMessage, rawContent",
        "EmailFile, fromGroup",
        "EmailFile, fromId",
        "EmailFile, fileName",
        "OtherMessage, fromGroup",
        "OtherMessage, fromId",
        "OtherMessage, fromName",
        "OtherMessage, rawContent",
        "OtherFile, fromGroup",
        "OtherFile, fromId",
        "OtherFile, fromName",
        "OtherFile, fileName",
        "ScreenMessage, fromGroup",
        "ScreenMessage, fromId",
        "ScreenMessage, rawContent",
        "ScreenFile, fromGroup",
        "ScreenFile, fromId",
        "ScreenFile, fileName"
      })
      void 必須項目が欠けている場合_falseを返す(final String requestType, final String field) {
        SesInfoRegisterRequestSqsEntity target = 全項目を持つエンティティを作る(RequestType.valueOf(requestType));
        項目をnullにする(target, field);

        assertThat(target.isValid()).isFalse();
      }
    }

    @Nested
    class 異常系 {

      /**
       * info_type が JOB と PERSON 以外の場合に無効と判定することを確かめる。 画面登録の種別を誤って解釈すると、案件と要員を取り違えて登録するためである。
       *
       * @given 必須項目をすべて持つ ScreenMessage、infoTypeInvalid が true
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void infoTypeが不正な場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = 全項目を持つエンティティを作る(RequestType.ScreenMessage);
        target.setInfoTypeInvalid(true);

        assertThat(target.isValid()).isFalse();
      }

      /**
       * リクエスト種別が null の場合に無効と判定することを確かめる。 種別が無いと必須項目を判定できず、どの経路の登録か決められないためである。
       *
       * @given requestType が null で、他の項目は全て値を持つ
       * @when isValid を実行する
       * @then false を返す
       */
      @Test
      void requestTypeがnullの場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = 全項目を持つエンティティを作る(null);

        assertThat(target.isValid()).isFalse();
      }
    }
  }

  @Nested
  class isProcessingTargetBody {

    @Nested
    class 境界値 {

      /**
       * 本文が null の場合に処理対象外と判定することを確かめる。 本文が無いメールを Gemini の分類に送らないためである。
       *
       * @given 本文が null
       * @when isProcessingTargetBody を実行する
       * @then false を返す
       */
      @Test
      void 本文がnullの場合_falseを返す() {
        assertThat(SesInfoRegisterRequestSqsEntity.isProcessingTargetBody(null)).isFalse();
      }

      /**
       * 本文が 200 文字の場合に処理対象外と判定することを確かめる。 200 文字以下は登録対象の下限を超えないため、分類に送らない。
       *
       * @given 本文が 200 文字
       * @when isProcessingTargetBody を実行する
       * @then false を返す
       */
      @Test
      void 本文が200文字の場合_falseを返す() {
        assertThat(SesInfoRegisterRequestSqsEntity.isProcessingTargetBody("a".repeat(200)))
            .isFalse();
      }

      /**
       * 本文が 201 文字の場合に処理対象と判定することを確かめる。 200 文字を超えた本文だけを分類に送るためである。
       *
       * @given 本文が 201 文字
       * @when isProcessingTargetBody を実行する
       * @then true を返す
       */
      @Test
      void 本文が201文字の場合_trueを返す() {
        assertThat(SesInfoRegisterRequestSqsEntity.isProcessingTargetBody("a".repeat(201)))
            .isTrue();
      }
    }
  }

  @Nested
  class isProcessingTarget {

    @Nested
    class 正常系 {

      /**
       * 本文の長さによらず処理対象とする種別の場合に、true を返すことを確かめる。 ファイルと画面の本文は短くても登録の対象であるためである。
       *
       * @given 種別が CsvSource の各行の種別、本文が 1 文字
       * @when isProcessingTarget を実行する
       * @then true を返す
       */
      @ParameterizedTest
      @CsvSource({"LineFile", "EmailFile", "OtherFile", "ScreenFile", "ScreenMessage"})
      void 種別がファイルまたは画面入力の場合_trueを返す(final String requestType) {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.valueOf(requestType));
        target.setRawContent("a");

        assertThat(target.isProcessingTarget()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * リクエスト種別が null の場合に処理対象外と判定することを確かめる。 種別が無いリクエストを登録処理へ進めないためである。
       *
       * @given requestType が null で、本文が 201 文字
       * @when isProcessingTarget を実行する
       * @then false を返す
       */
      @Test
      void 種別がnullの場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRawContent("a".repeat(201));

        assertThat(target.isProcessingTarget()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 本文を持つ種別の本文の文字数が境界の場合に、処理対象かどうかを返すことを確かめる。 200 文字以下は Gemini を呼ばず、201 文字以上だけを処理するためである。
       *
       * @given 種別が CsvSource の1列目、本文が CsvSource の2列目の文字数
       * @when isProcessingTarget を実行する
       * @then CsvSource の3列目の処理対象かどうかを返す
       */
      @ParameterizedTest
      @CsvSource({
        "LineMessage, 200, false",
        "LineMessage, 201, true",
        "EmailMessage, 200, false",
        "EmailMessage, 201, true",
        "OtherMessage, 200, false",
        "OtherMessage, 201, true"
      })
      void 本文の文字数が境界の場合_処理対象かどうかを返す(
          final String requestType, final int length, final boolean expected) {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.valueOf(requestType));
        target.setRawContent("a".repeat(length));

        assertThat(target.isProcessingTarget()).isEqualTo(expected);
      }
    }
  }

  @Nested
  class isDirectedJobRegistration {

    @Nested
    class 正常系 {

      /**
       * 画面で JOB と指定した本文の登録の場合に、true を返すことを確かめる。 画面で案件と指定された入力を案件として登録するためである。
       *
       * @given infoType が "JOB"、種別が ScreenMessage、本文が "案件本文"
       * @when isDirectedJobRegistration を実行する
       * @then true を返す
       */
      @Test
      void 画面で案件と指定した本文の場合_trueを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setInfoType("JOB");
        target.setRequestType(RequestType.ScreenMessage);
        target.setRawContent("案件本文");

        assertThat(target.isDirectedJobRegistration()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 種別が画面メッセージ以外の場合に false を返すことを確かめる。 LINE の経路は画面指定の登録として扱わないためである。
       *
       * @given infoType が "JOB"、種別が LineMessage、本文が "案件本文"
       * @when isDirectedJobRegistration を実行する
       * @then false を返す
       */
      @Test
      void 種別が画面メッセージ以外の場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setInfoType("JOB");
        target.setRequestType(RequestType.LineMessage);
        target.setRawContent("案件本文");

        assertThat(target.isDirectedJobRegistration()).isFalse();
      }

      /**
       * infoType が null の場合に false を返すことを確かめる。 画面で種別を指定していない登録を案件として扱わないためである。
       *
       * @given infoType が null、種別が ScreenMessage、本文が "案件本文"
       * @when isDirectedJobRegistration を実行する
       * @then false を返す
       */
      @Test
      void infoTypeがnullの場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.ScreenMessage);
        target.setRawContent("案件本文");

        assertThat(target.isDirectedJobRegistration()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 本文が空文字の場合に false を返すことを確かめる。 本文の無い案件登録は処理できないためである。
       *
       * @given infoType が "JOB"、種別が ScreenMessage、本文が空文字
       * @when isDirectedJobRegistration を実行する
       * @then false を返す
       */
      @Test
      void 本文が空文字の場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setInfoType("JOB");
        target.setRequestType(RequestType.ScreenMessage);
        target.setRawContent("");

        assertThat(target.isDirectedJobRegistration()).isFalse();
      }
    }
  }

  @Nested
  class isDirectedCandidateRegistration {

    @Nested
    class 正常系 {

      /**
       * 画面で PERSON と指定した本文の登録の場合に、true を返すことを確かめる。 画面で要員と指定された入力を要員として登録するためである。
       *
       * @given infoType が "PERSON"、種別が ScreenMessage、本文が "要員本文"
       * @when isDirectedCandidateRegistration を実行する
       * @then true を返す
       */
      @Test
      void 画面で要員と指定した本文の場合_trueを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setInfoType("PERSON");
        target.setRequestType(RequestType.ScreenMessage);
        target.setRawContent("要員本文");

        assertThat(target.isDirectedCandidateRegistration()).isTrue();
      }

      /**
       * 画面で PERSON と指定したスキルシートの登録の場合に、true を返すことを確かめる。 本文が無くてもスキルシートがあれば要員として登録するためである。
       *
       * @given infoType が "PERSON"、種別が ScreenFile
       * @when isDirectedCandidateRegistration を実行する
       * @then true を返す
       */
      @Test
      void 画面で要員と指定したファイルの場合_trueを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setInfoType("PERSON");
        target.setRequestType(RequestType.ScreenFile);

        assertThat(target.isDirectedCandidateRegistration()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * infoType が JOB の場合に false を返すことを確かめる。 案件と指定された登録を要員として扱わないためである。
       *
       * @given infoType が "JOB"、種別が ScreenFile
       * @when isDirectedCandidateRegistration を実行する
       * @then false を返す
       */
      @Test
      void infoTypeがJOBの場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setInfoType("JOB");
        target.setRequestType(RequestType.ScreenFile);

        assertThat(target.isDirectedCandidateRegistration()).isFalse();
      }

      /**
       * 画面以外の種別で PERSON と指定された場合に false を返すことを確かめる。 LINE の経路は画面指定の登録として扱わないためである。
       *
       * @given infoType が "PERSON"、種別が LineMessage
       * @when isDirectedCandidateRegistration を実行する
       * @then false を返す
       */
      @Test
      void 種別が画面以外の場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setInfoType("PERSON");
        target.setRequestType(RequestType.LineMessage);

        assertThat(target.isDirectedCandidateRegistration()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 画面の本文が空文字の場合に false を返すことを確かめる。 本文の無い要員登録は処理できないためである。
       *
       * @given infoType が "PERSON"、種別が ScreenMessage、本文が空文字
       * @when isDirectedCandidateRegistration を実行する
       * @then false を返す
       */
      @Test
      void 画面の本文が空文字の場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setInfoType("PERSON");
        target.setRequestType(RequestType.ScreenMessage);
        target.setRawContent("");

        assertThat(target.isDirectedCandidateRegistration()).isFalse();
      }
    }
  }

  @Nested
  class isスキルシート {

    @Nested
    class 正常系 {

      /**
       * ファイル種別でファイル名がある場合に、スキルシートと判定することを確かめる。 ファイルの取り込みはスキルシートとして処理するためである。
       *
       * @given 種別が CsvSource の各行の種別、ファイル名が "skillsheet.xlsx"
       * @when isスキルシート を実行する
       * @then true を返す
       */
      @ParameterizedTest
      @CsvSource({"LineFile", "EmailFile", "ScreenFile", "OtherFile"})
      void 種別がファイルの場合_ファイル名があればtrueを返す(final String requestType) {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.valueOf(requestType));
        target.setFileName("skillsheet.xlsx");

        assertThat(target.isスキルシート()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * メッセージ種別の場合に、ファイル名があってもスキルシートでないと判定することを確かめる。 本文だけの投稿をファイルとして扱わないためである。
       *
       * @given 種別が LineMessage、ファイル名が "skillsheet.xlsx"
       * @when isスキルシート を実行する
       * @then false を返す
       */
      @Test
      void 種別がメッセージの場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.LineMessage);
        target.setFileName("skillsheet.xlsx");

        assertThat(target.isスキルシート()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイル名が null の場合に、スキルシートでないと判定することを確かめる。 ファイル名の無いファイルは取り込めないためである。
       *
       * @given 種別が LineFile、ファイル名が null
       * @when isスキルシート を実行する
       * @then false を返す
       */
      @Test
      void ファイル名がnullの場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.LineFile);

        assertThat(target.isスキルシート()).isFalse();
      }

      /**
       * ファイル名が空文字の場合に、スキルシートでないと判定することを確かめる。 名前の無いファイルは取り込めないためである。
       *
       * @given 種別が LineFile、ファイル名が空文字
       * @when isスキルシート を実行する
       * @then false を返す
       */
      @Test
      void ファイル名が空文字の場合_falseを返す() {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.LineFile);
        target.setFileName("");

        assertThat(target.isスキルシート()).isFalse();
      }
    }
  }

  @Nested
  class downloadFileData {

    @Nested
    class 正常系 {

      /**
       * LINE のファイルの場合に、LINE から取得したファイルのデータを持つことを確かめる。 LINE の添付ファイルは取り込みに本体が必要なためである。
       *
       * @given 種別が LineFile、ファイルIDが "F001"、LINE の取得結果が "file" のバイト列
       * @when downloadFileData をトークン "token" で実行する
       * @then 取得したバイト列を fileData として持つ
       */
      @Test
      void LINEのファイルの場合_取得したバイト列を返す() throws Exception {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.LineFile);
        target.setFileId("F001");
        byte[] content = "file".getBytes(StandardCharsets.UTF_8);

        try (MockedConstruction<LineMessagingApi> constructed =
            mockConstruction(
                LineMessagingApi.class,
                (mock, context) -> when(mock.getFile("F001")).thenReturn(content))) {
          target.downloadFileData("token");

          assertThat(constructed.constructed()).hasSize(1);
        }
        assertThat(target.getFileData()).isEqualTo(content);
      }
    }

    @Nested
    class 境界値 {

      /**
       * LINE のファイル以外の種別の場合に、ファイルを取得しないことを確かめる。 メッセージや他の経路のファイルは LINE から取得する対象ではないためである。
       *
       * @given 種別が CsvSource の各行の種別、LINE から取得する処理を差し替える
       * @when downloadFileData をトークン "token" で実行する
       * @then LINE のクライアントを作らず、fileData を null のまま保つ
       */
      @ParameterizedTest
      @CsvSource({
        "LineMessage",
        "EmailMessage",
        "OtherMessage",
        "EmailFile",
        "ScreenFile",
        "OtherFile",
        "ScreenMessage"
      })
      void LINEのファイル以外の種別の場合_LINEのクライアントを呼ばない(final String requestType) throws Exception {
        SesInfoRegisterRequestSqsEntity target = new SesInfoRegisterRequestSqsEntity();
        target.setRequestType(RequestType.valueOf(requestType));

        try (MockedConstruction<LineMessagingApi> constructed =
            mockConstruction(LineMessagingApi.class)) {
          target.downloadFileData("token");

          assertThat(constructed.constructed()).isEmpty();
        }
        assertThat(target.getFileData()).isNull();
      }
    }
  }
}
