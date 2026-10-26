package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

/** {@link RequestType} の単体テスト。 */
class RequestTypeTest {

  @Nested
  class getEnum {

    @Nested
    class 正常系 {

      /**
       * コード値から対応するリクエスト種別を取得することを確かめる。 受信元が送るコード値を、種別の列挙値へ対応づける仕様を守る。
       *
       * @given コード値が指定されている
       * @when getEnum をコード値で実行する
       * @then 対応するリクエスト種別を返す
       */
      @ParameterizedTest
      @CsvSource({
        "11, LineMessage",
        "12, LineFile",
        "21, EmailMessage",
        "22, EmailFile",
        "31, ScreenMessage",
        "32, ScreenFile",
        "01, OtherMessage",
        "02, OtherFile"
      })
      void コード値を指定した場合_対応するリクエスト種別を返す(final String code, final RequestType expected) {
        assertThat(RequestType.getEnum(code)).isEqualTo(expected);
      }

      /**
       * 列挙名を指定すると対応するリクエスト種別を取得することを確かめる。 JSON では列挙名で届く場合があるため、列挙名でも取得できることを守る。
       *
       * @given 列挙名が指定されている
       * @when getEnum を列挙名で実行する
       * @then 対応するリクエスト種別を返す
       */
      @ParameterizedTest
      @CsvSource({
        "LineMessage, LineMessage",
        "LineFile, LineFile",
        "EmailMessage, EmailMessage",
        "EmailFile, EmailFile",
        "ScreenMessage, ScreenMessage",
        "ScreenFile, ScreenFile",
        "OtherMessage, OtherMessage",
        "OtherFile, OtherFile"
      })
      void 列挙名を指定した場合_対応するリクエスト種別を返す(final String name, final RequestType expected) {
        assertThat(RequestType.getEnum(name)).isEqualTo(expected);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 未定義のコード値を指定すると OtherMessage を返すことを確かめる。 未知の受信種別を例外にすると受信処理全体が止まるので、その他のメッセージとして扱う。
       *
       * @given コード値が "99"
       * @when getEnum を "99" で実行する
       * @then OtherMessage を返す
       */
      @Test
      void 未定義のコード値の場合_OtherMessageを返す() {
        assertThat(RequestType.getEnum("99")).isEqualTo(RequestType.OtherMessage);
      }
    }

    @Nested
    class 境界値 {

      /**
       * null を指定すると null を返すことを確かめる。 種別が無い場合に既定の種別を入れると誤った分類になるので、null を返す。
       *
       * @given コード値が null
       * @when getEnum を null で実行する
       * @then null を返す
       */
      @NullSource
      @ParameterizedTest
      void コード値がnullの場合_nullを返す(final String code) {
        assertThat(RequestType.getEnum(code)).isNull();
      }
    }
  }

  @Nested
  class isFile {

    @Nested
    class 正常系 {

      /**
       * ファイルの種別の場合にtrueを返すことを確かめる。 ファイルの種別だけ、スキルシートの判定と本文の文字数の判定から外すためである。
       *
       * @given リクエスト種別が LineFile、EmailFile、ScreenFile、OtherFile のいずれか
       * @when isFile を実行する
       * @then trueを返す
       */
      @ParameterizedTest
      @CsvSource({"LineFile", "EmailFile", "ScreenFile", "OtherFile"})
      void ファイルの種別の場合_trueを返す(final String name) {
        assertThat(RequestType.valueOf(name).isFile()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * メッセージの種別の場合にfalseを返すことを確かめる。 メッセージの種別はファイルとして扱わないためである。
       *
       * @given リクエスト種別が LineMessage、EmailMessage、ScreenMessage、OtherMessage のいずれか
       * @when isFile を実行する
       * @then falseを返す
       */
      @ParameterizedTest
      @CsvSource({"LineMessage", "EmailMessage", "ScreenMessage", "OtherMessage"})
      void メッセージの種別の場合_falseを返す(final String name) {
        assertThat(RequestType.valueOf(name).isFile()).isFalse();
      }
    }
  }

  @Nested
  class hasBodyText {

    @Nested
    class 正常系 {

      /**
       * 本文を持つ種別の場合にtrueを返すことを確かめる。 本文を持つ種別だけ、本文の文字数で処理対象かを判定するためである。
       *
       * @given リクエスト種別が LineMessage、EmailMessage、OtherMessage のいずれか
       * @when hasBodyText を実行する
       * @then trueを返す
       */
      @ParameterizedTest
      @CsvSource({"LineMessage", "EmailMessage", "OtherMessage"})
      void 本文を持つ種別の場合_trueを返す(final String name) {
        assertThat(RequestType.valueOf(name).hasBodyText()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 本文を持たない種別の場合にfalseを返すことを確かめる。 ファイルと画面のメッセージは本文の文字数で処理対象を外さないためである。
       *
       * @given リクエスト種別が LineFile、EmailFile、ScreenMessage、ScreenFile、OtherFile のいずれか
       * @when hasBodyText を実行する
       * @then falseを返す
       */
      @ParameterizedTest
      @CsvSource({"LineFile", "EmailFile", "ScreenMessage", "ScreenFile", "OtherFile"})
      void 本文を持たない種別の場合_falseを返す(final String name) {
        assertThat(RequestType.valueOf(name).hasBodyText()).isFalse();
      }
    }
  }

  @Nested
  class isLineFile {

    @Nested
    class 正常系 {

      /**
       * LINEのファイルの場合にtrueを返すことを確かめる。 LINEのファイルだけ LINE Messaging API から本体を取得するためである。
       *
       * @given リクエスト種別が LineFile
       * @when isLineFile を実行する
       * @then trueを返す
       */
      @Test
      void LineFileの場合_trueを返す() {
        assertThat(RequestType.LineFile.isLineFile()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * LINEのファイル以外の種別の場合にfalseを返すことを確かめる。 他の種別は LINE Messaging API からファイルを取得しないためである。
       *
       * @given リクエスト種別が LineFile 以外の種別
       * @when isLineFile を実行する
       * @then falseを返す
       */
      @ParameterizedTest
      @CsvSource({"LineMessage", "EmailMessage", "EmailFile", "ScreenMessage", "ScreenFile"})
      void LineFile以外の種別の場合_falseを返す(final String name) {
        assertThat(RequestType.valueOf(name).isLineFile()).isFalse();
      }
    }
  }

  @Nested
  class requiredFieldNames {

    @Nested
    class 正常系 {

      /**
       * LINEとその他のメッセージの場合に、送信者名と本文を含む項目名を判定の順に返すことを確かめる。 どの項目が欠けたかをログで追えるよう、判定の順を固定するためである。
       *
       * @given リクエスト種別が LineMessage または OtherMessage
       * @when requiredFieldNames を実行する
       * @then "fromGroup"、"fromId"、"fromName"、"rawContent" の順に項目名の一覧を返す
       */
      @ParameterizedTest
      @CsvSource({"LineMessage", "OtherMessage"})
      void LINEとその他のメッセージの場合_送信者名と本文を含む項目名の一覧を返す(final String name) {
        assertThat(RequestType.valueOf(name).requiredFieldNames())
            .containsExactly("fromGroup", "fromId", "fromName", "rawContent");
      }

      /**
       * LINEとその他のファイルの場合に、送信者名とファイル名を含む項目名を判定の順に返すことを確かめる。 ファイルの取り込みはファイル名が無いと処理できないためである。
       *
       * @given リクエスト種別が LineFile または OtherFile
       * @when requiredFieldNames を実行する
       * @then "fromGroup"、"fromId"、"fromName"、"fileName" の順に項目名の一覧を返す
       */
      @ParameterizedTest
      @CsvSource({"LineFile", "OtherFile"})
      void LINEとその他のファイルの場合_送信者名とファイル名を含む項目名の一覧を返す(final String name) {
        assertThat(RequestType.valueOf(name).requiredFieldNames())
            .containsExactly("fromGroup", "fromId", "fromName", "fileName");
      }

      /**
       * メールと画面のメッセージの場合に、送信者名を必須とせず本文を含む項目名を返すことを確かめる。 メールと画面の送信者名は登録時に必要としない仕様のためである。
       *
       * @given リクエスト種別が EmailMessage または ScreenMessage
       * @when requiredFieldNames を実行する
       * @then "fromGroup"、"fromId"、"rawContent" の順に項目名の一覧を返す
       */
      @ParameterizedTest
      @CsvSource({"EmailMessage", "ScreenMessage"})
      void メールと画面のメッセージの場合_送信者名を除く本文を含む項目名の一覧を返す(final String name) {
        assertThat(RequestType.valueOf(name).requiredFieldNames())
            .containsExactly("fromGroup", "fromId", "rawContent");
      }

      /**
       * メールと画面のファイルの場合に、ファイル名を含む項目名を返すことを確かめる。 これらの種別は送信者名を必須とせず、ファイル名で取り込むためである。
       *
       * @given リクエスト種別が EmailFile または ScreenFile
       * @when requiredFieldNames を実行する
       * @then "fromGroup"、"fromId"、"fileName" の順に項目名の一覧を返す
       */
      @ParameterizedTest
      @CsvSource({"EmailFile", "ScreenFile"})
      void メールと画面のファイルの場合_ファイル名を含む項目名の一覧を返す(final String name) {
        assertThat(RequestType.valueOf(name).requiredFieldNames())
            .containsExactly("fromGroup", "fromId", "fileName");
      }
    }
  }

  @Nested
  class acceptsDirectedJob {

    @Nested
    class 正常系 {

      /**
       * 画面のメッセージで本文がある場合にtrueを返すことを確かめる。 画面から JOB と指定された本文は案件として登録するためである。
       *
       * @given リクエスト種別が ScreenMessage、原文が "本文"
       * @when acceptsDirectedJob を原文 "本文" で実行する
       * @then trueを返す
       */
      @Test
      void 画面のメッセージで本文がある場合_trueを返す() {
        assertThat(RequestType.ScreenMessage.acceptsDirectedJob("本文")).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 画面のメッセージ以外の種別の場合にfalseを返すことを確かめる。 画面以外から届いた本文を、画面指定の案件として扱わないためである。
       *
       * @given リクエスト種別が LineMessage、EmailMessage、ScreenFile のいずれか、原文が "本文"
       * @when acceptsDirectedJob を原文 "本文" で実行する
       * @then falseを返す
       */
      @ParameterizedTest
      @CsvSource({"LineMessage", "EmailMessage", "ScreenFile"})
      void 画面のメッセージ以外の種別の場合_falseを返す(final String name) {
        assertThat(RequestType.valueOf(name).acceptsDirectedJob("本文")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 画面のメッセージで本文が空の場合にfalseを返すことを確かめる。 本文の無い案件は登録しない仕様のためである。
       *
       * @given リクエスト種別が ScreenMessage、原文が空文字
       * @when acceptsDirectedJob を原文 "" で実行する
       * @then falseを返す
       */
      @Test
      void 画面のメッセージで本文が空の場合_falseを返す() {
        assertThat(RequestType.ScreenMessage.acceptsDirectedJob("")).isFalse();
      }
    }
  }

  @Nested
  class acceptsDirectedCandidate {

    @Nested
    class 正常系 {

      /**
       * 画面のメッセージで本文がある場合にtrueを返すことを確かめる。 画面から PERSON と指定された本文は要員として登録するためである。
       *
       * @given リクエスト種別が ScreenMessage、原文が "本文"
       * @when acceptsDirectedCandidate を原文 "本文" で実行する
       * @then trueを返す
       */
      @Test
      void 画面のメッセージで本文がある場合_trueを返す() {
        assertThat(RequestType.ScreenMessage.acceptsDirectedCandidate("本文")).isTrue();
      }

      /**
       * 画面のファイルの場合は原文に関わらずtrueを返すことを確かめる。 スキルシートは本文を持たず、ファイルで要員として登録するためである。
       *
       * @given リクエスト種別が ScreenFile、原文が null
       * @when acceptsDirectedCandidate を原文 null で実行する
       * @then trueを返す
       */
      @Test
      void 画面のファイルの場合_trueを返す() {
        assertThat(RequestType.ScreenFile.acceptsDirectedCandidate(null)).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 画面以外の種別の場合にfalseを返すことを確かめる。 画面以外から届いた本文を、画面指定の要員として扱わないためである。
       *
       * @given リクエスト種別が LineFile、EmailMessage、OtherMessage のいずれか、原文が "本文"
       * @when acceptsDirectedCandidate を原文 "本文" で実行する
       * @then falseを返す
       */
      @ParameterizedTest
      @CsvSource({"LineFile", "EmailMessage", "OtherMessage"})
      void 画面以外の種別の場合_falseを返す(final String name) {
        assertThat(RequestType.valueOf(name).acceptsDirectedCandidate("本文")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 画面のメッセージで本文が空の場合にfalseを返すことを確かめる。 本文の無い要員の登録は行わない仕様のためである。
       *
       * @given リクエスト種別が ScreenMessage、原文が空文字
       * @when acceptsDirectedCandidate を原文 "" で実行する
       * @then falseを返す
       */
      @Test
      void 画面のメッセージで本文が空の場合_falseを返す() {
        assertThat(RequestType.ScreenMessage.acceptsDirectedCandidate("")).isFalse();
      }
    }
  }
}
