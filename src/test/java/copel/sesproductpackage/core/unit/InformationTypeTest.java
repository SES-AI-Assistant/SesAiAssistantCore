package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.util.ObjectMapperFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/** {@link InformationType} の単体テスト。 */
class InformationTypeTest {

  @Nested
  class fromJson {

    @Nested
    class 正常系 {

      /**
       * 大文字の種別名を対応する情報種別として解決することを確かめる。 AI の応答は大文字の種別名で返すため、この解決が分類の入口になるためである。
       *
       * @given 種別名が "PERSON"
       * @when fromJson を実行する
       * @then PERSON を返す
       */
      @Test
      void 種別名が大文字の場合_PERSONを返す() {
        assertThat(InformationType.fromJson("PERSON")).isEqualTo(InformationType.PERSON);
      }

      /**
       * 小文字や前後に空白がある種別名を、空白と大小文字を除いて解決することを確かめる。 応答の表記ゆれで種別が Unknown に落ちると、案件や要員を取りこぼすためである。
       *
       * @given 種別名が " job " と "Person"
       * @when fromJson を実行する
       * @then 前者は JOB、後者は PERSON を返す
       */
      @ParameterizedTest
      @CsvSource({" job , JOB", "Person, PERSON"})
      void 表記が揃わない種別名の場合_対応する情報種別を返す(final String value, final InformationType expected) {
        assertThat(InformationType.fromJson(value)).isEqualTo(expected);
      }

      /**
       * JSON の種別名を読み込むと、対応する情報種別になることを確かめる。 AI の応答を要約の型へ読み込む入口で、この変換が分類の結果を決めるためである。
       *
       * @given JSON の文字列 "\"PERSON\""
       * @when ObjectMapper で InformationType に読み込む
       * @then PERSON を返す
       */
      @Test
      void JSONの種別名を読み込む場合_対応する情報種別を返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue("\"PERSON\"", InformationType.class))
            .isEqualTo(InformationType.PERSON);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対応しない種別名を Unknown として解決することを確かめる。 解決できない値で例外にすると、リクエストの読み込み全体が失敗するためである。
       *
       * @given 種別名が "OTHER"
       * @when fromJson を実行する
       * @then Unknown を返す
       */
      @Test
      void 対応しない種別名の場合_Unknownを返す() {
        assertThat(InformationType.fromJson("OTHER")).isEqualTo(InformationType.Unknown);
      }

      /**
       * JSON の種別が null の場合に、情報種別を null のまま読み込むことを確かめる。 未設定の種別を既定の値で埋めると、分類の結果を誤って確定させるためである。
       *
       * @given JSON の値が null
       * @when ObjectMapper で InformationType に読み込む
       * @then null を返す
       */
      @Test
      void JSONの種別がnullの場合_nullを返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue("null", InformationType.class))
            .isNull();
      }

      /**
       * 種別名が null または空文字の場合に Unknown として解決することを確かめる。 未指定の種別を案件や要員として扱わないためである。
       *
       * @given 種別名が null と空文字
       * @when fromJson を実行する
       * @then Unknown を返す
       */
      @ParameterizedTest
      @NullAndEmptySource
      void 種別名がnullまたは空文字の場合_Unknownを返す(final String value) {
        assertThat(InformationType.fromJson(value)).isEqualTo(InformationType.Unknown);
      }
    }
  }

  @Nested
  class requiredPermission {

    @Nested
    class 正常系 {

      /**
       * 案件の登録とインポートには案件一覧の権限が必要なことを確かめる。 案件の登録は案件一覧の画面から行うため、権限の対応を変えると利用者が操作できなくなるためである。
       *
       * @given 情報種別が JOB
       * @when requiredPermission を実行する
       * @then VIEW_JOB_LIST を返す
       */
      @Test
      void 情報種別が案件の場合_案件一覧の権限を返す() {
        assertThat(InformationType.JOB.requiredPermission()).isEqualTo(Permission.VIEW_JOB_LIST);
      }

      /**
       * 要員の登録とインポートにはマッチング一覧の権限が必要なことを確かめる。 要員の登録は要員の画面から行うため、案件とは別の権限で守るためである。
       *
       * @given 情報種別が PERSON
       * @when requiredPermission を実行する
       * @then VIEW_MATCHING_LIST を返す
       */
      @Test
      void 情報種別が要員の場合_マッチング一覧の権限を返す() {
        assertThat(InformationType.PERSON.requiredPermission())
            .isEqualTo(Permission.VIEW_MATCHING_LIST);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対象外の情報種別は要員と同じマッチング一覧の権限にすることを確かめる。 従来の Backend の判定では案件以外をマッチング一覧としていたため、結果を変えないためである。
       *
       * @given 情報種別が Unknown
       * @when requiredPermission を実行する
       * @then VIEW_MATCHING_LIST を返す
       */
      @Test
      void 情報種別が対象外の場合_マッチング一覧の権限を返す() {
        assertThat(InformationType.Unknown.requiredPermission())
            .isEqualTo(Permission.VIEW_MATCHING_LIST);
      }
    }
  }
}
