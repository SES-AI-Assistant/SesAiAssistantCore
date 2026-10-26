package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import copel.sesproductpackage.core.unit.InformationType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** {@link InfoSchemaValidator} の単体テスト。 */
class InfoSchemaValidatorTest {

  @Nested
  class convert {

    @Nested
    class 正常系 {

      /**
       * 種別が PERSON / JOB で summary がある情報をそのまま返すことを確かめる。 登録に必要な詳細データが揃った情報は検査を通す。
       *
       * @given 種別が PERSON または JOB で summary が設定された情報
       * @when convert を実行する
       * @then 渡した情報と同じものを返す
       */
      @ParameterizedTest
      @EnumSource(
          value = InformationType.class,
          names = {"PERSON", "JOB"})
      void summaryがある場合_同じ情報を返す(final InformationType type) {
        ContentParseSchema.InfoSchema info = new ContentParseSchema.InfoSchema(type, "詳細");

        assertThat(new InfoSchemaValidator().convert(info)).isSameAs(info);
      }

      /**
       * 種別が Unknown で summary が無い情報をそのまま返すことを確かめる。 案件でも要員でもない本文では summary が出力されない。
       *
       * @given 種別が Unknown で summary が null の情報
       * @when convert を実行する
       * @then 渡した情報と同じものを返す
       */
      @Test
      void 種別がUnknownでsummaryが無い場合_同じ情報を返す() {
        ContentParseSchema.InfoSchema info =
            new ContentParseSchema.InfoSchema(InformationType.Unknown, null);

        assertThat(new InfoSchemaValidator().convert(info)).isSameAs(info);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 種別が PERSON / JOB で summary が無い情報を例外にすることを確かめる。 登録されないまま成功扱いになるのを防ぐ。
       *
       * @given 種別が PERSON または JOB で summary が null の情報
       * @when convert を実行する
       * @then summary が無い旨のメッセージを持つ IllegalArgumentException を投げる
       */
      @ParameterizedTest
      @EnumSource(
          value = InformationType.class,
          names = {"PERSON", "JOB"})
      void summaryが無い場合_summary欠落の例外を投げる(final InformationType type) {
        ContentParseSchema.InfoSchema info = new ContentParseSchema.InfoSchema(type, null);

        assertThatThrownBy(() -> new InfoSchemaValidator().convert(info))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("summary がありません");
      }
    }
  }
}
