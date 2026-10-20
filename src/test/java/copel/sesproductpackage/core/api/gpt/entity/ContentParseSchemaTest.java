package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.api.gpt.entity.ContentParseSchema.InformationType;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ContentParseSchema} の単体テスト。 */
class ContentParseSchemaTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * JSON の情報種別が大文字で書かれている場合に、対応する列挙値として読み込むことを確かめる。 AI の応答は大文字の種別名で返すため、この読み込みが分類の入口になるから守る。
       *
       * @given JSON に type が "PERSON" の情報が1件含まれる
       * @when ContentParseSchema に読み込む
       * @then 情報の種別が PERSON である
       */
      @Test
      void 種別が大文字で書かれている場合_情報種別PERSONを返す() throws IOException {
        ContentParseSchema schema =
            read("{\"informations\":[{\"type\":\"PERSON\",\"summary\":{}}]}");

        assertThat(schema.getInformations().get(0).getType()).isEqualTo(InformationType.PERSON);
      }

      /**
       * JSON の情報種別が小文字で書かれている場合に、大文字小文字を区別せず読み込むことを確かめる。 応答の表記ゆれで種別が Unknown
       * に落ちると、案件や要員の情報を取りこぼすので、その仕様を守る。
       *
       * @given JSON に type が "job" の情報が1件含まれる
       * @when ContentParseSchema に読み込む
       * @then 情報の種別が JOB である
       */
      @Test
      void 種別が小文字で書かれている場合_情報種別JOBを返す() throws IOException {
        ContentParseSchema schema = read("{\"informations\":[{\"type\":\"job\",\"summary\":{}}]}");

        assertThat(schema.getInformations().get(0).getType()).isEqualTo(InformationType.JOB);
      }
    }

    @Nested
    class 境界値 {

      /**
       * JSON の情報種別の前後に空白がある場合に、空白を除いて読み込むことを確かめる。 応答の文字列に余分な空白が入っても、同じ種別として扱うので、その仕様を守る。
       *
       * @given JSON に type が " JOB " の情報が1件含まれる
       * @when ContentParseSchema に読み込む
       * @then 情報の種別が JOB である
       */
      @Test
      void 種別の前後に空白がある場合_前後の空白を除いた情報種別JOBを返す() throws IOException {
        ContentParseSchema schema =
            read("{\"informations\":[{\"type\":\" JOB \",\"summary\":{}}]}");

        assertThat(schema.getInformations().get(0).getType()).isEqualTo(InformationType.JOB);
      }

      /**
       * JSON の情報種別がどの列挙値とも一致しない場合に、Unknown として読み込むことを確かめる。 想定外の種別で処理を止めず、未判定の情報として扱うことを守る。
       *
       * @given JSON に type が "XYZ" の情報が1件含まれる
       * @when ContentParseSchema に読み込む
       * @then 情報の種別が Unknown である
       */
      @Test
      void 種別が一致しない文字列の場合_情報種別Unknownを返す() throws IOException {
        ContentParseSchema schema = read("{\"informations\":[{\"type\":\"XYZ\",\"summary\":{}}]}");

        assertThat(schema.getInformations().get(0).getType()).isEqualTo(InformationType.Unknown);
      }

      /**
       * 種別の文字列が空の場合に、Unknown として読み込むことを確かめる。 空の種別は判定の材料が無いため、未判定として扱うことを守る。
       *
       * @given JSON に type が "" の情報が1件含まれる
       * @when ContentParseSchema に読み込む
       * @then 情報の種別が Unknown である
       */
      @Test
      void 種別が空文字の場合_情報種別Unknownを返す() throws IOException {
        ContentParseSchema schema = read("{\"informations\":[{\"type\":\"\",\"summary\":{}}]}");

        assertThat(schema.getInformations().get(0).getType()).isEqualTo(InformationType.Unknown);
      }

      /**
       * JSON の情報種別が null の場合に、種別を null として読み込むことを確かめる。 null の種別を既定値で埋めず、そのまま読み込む現在の挙動を守る。
       *
       * @given JSON に type が null の情報が1件含まれる
       * @when ContentParseSchema に読み込む
       * @then 情報の種別が null である
       */
      @Test
      void 種別がnullの場合_nullの種別を返す() throws Exception {
        ContentParseSchema schema = read("{\"informations\":[{\"type\":null,\"summary\":{}}]}");

        assertThat(schema.getInformations().get(0).getType()).isNull();
      }
    }
  }

  /**
   * JSON 文字列を {@link ContentParseSchema} に読み込む.
   *
   * @param json 読み込む JSON
   * @return 読み込んだ結果
   * @throws IOException 読み込みに失敗した場合
   */
  private static ContentParseSchema read(final String json) throws IOException {
    return ObjectMapperFactory.OBJECT_MAPPER.readValue(json, ContentParseSchema.class);
  }
}
