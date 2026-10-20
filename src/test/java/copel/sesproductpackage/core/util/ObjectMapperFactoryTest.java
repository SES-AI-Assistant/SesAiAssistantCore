package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link ObjectMapperFactory} の単体テスト。 */
class ObjectMapperFactoryTest {

  /** 項目名の変換を確かめるための検証用の値。 */
  public static class 検証用の値 {

    /** ユーザー名。 */
    private final String userName;

    /**
     * 検証用の値を生成する。
     *
     * @param userName ユーザー名
     */
    public 検証用の値(final String userName) {
      this.userName = userName;
    }

    /**
     * ユーザー名を返す。
     *
     * @return ユーザー名
     */
    public String getUserName() {
      return userName;
    }
  }

  @Nested
  class createGptObjectMapper {

    @Nested
    class 正常系 {

      /**
       * キャメルケースの項目名をスネークケースで出力することを確かめる。 項目名の形式が変わると外部 API が読めなくなるので、スネークケースを固定する。
       *
       * @given 項目 userName が "山田" の検証用の値
       * @when OBJECT_MAPPER で JSON に変換する
       * @then {"user_name":"山田"} の JSON 文字列を返す
       */
      @Test
      void 項目名がキャメルケースの場合_スネークケースのJSON文字列を返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(new 検証用の値("山田")))
            .isEqualTo("{\"user_name\":\"山田\"}");
      }

      /**
       * 文字列の前後の空白を取り除いて読み込むことを確かめる。 生成 AI の出力の余分な空白が残ると項目の値が一致しなくなるので、前後の空白を取り除く。
       *
       * @given JSON の文字列が " 山田 "
       * @when OBJECT_MAPPER で String 型に読み込む
       * @then 前後の空白を除いた "山田" を返す
       */
      @Test
      void 文字列の前後に空白がある場合_空白を除いた文字列を返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue("\"  山田  \"", String.class))
            .isEqualTo("山田");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 文字列の項目にオブジェクトが来た場合は中身を読み捨てて null を返すことを確かめる。 1 項目の不正で解析全体が失敗すると他の項目も失われるので、その項目だけ null にする。
       *
       * @given JSON の値が {"a":1} のオブジェクト
       * @when OBJECT_MAPPER で String 型に読み込む
       * @then null を返す
       */
      @Test
      void オブジェクトの場合_nullを返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue("{\"a\":1}", String.class)).isNull();
      }

      /**
       * 文字列の項目に配列が来た場合は中身を読み捨てて null を返すことを確かめる。 配列を文字列の項目に入れても解析全体を止めないために、その項目だけ null にする。
       *
       * @given JSON の値が a を要素に持つ配列
       * @when OBJECT_MAPPER で String 型に読み込む
       * @then null を返す
       */
      @Test
      void 配列の場合_nullを返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue("[\"a\"]", String.class)).isNull();
      }

      /**
       * 数値や真偽値の値は文字列として読み込むことを確かめる。 数値を文字列の項目で受け取れないと解析に失敗するので、文字列として保持する。
       *
       * @given JSON の値が 12 の数値
       * @when OBJECT_MAPPER で String 型に読み込む
       * @then "12" を返す
       */
      @ValueSource(strings = {"12"})
      @ParameterizedTest
      void 数値の場合_文字列を返す(final String json) throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue(json, String.class)).isEqualTo("12");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 文字列の値が大文字小文字を問わず null の場合は null を返すことを確かめる。 生成 AI が文字列で "null" を返すので、未設定として扱う。
       *
       * @given JSON の文字列が "NULL"
       * @when OBJECT_MAPPER で String 型に読み込む
       * @then null を返す
       */
      @Test
      void 文字列の値がnullの場合_nullを返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue("\"NULL\"", String.class)).isNull();
      }

      /**
       * 空白だけの文字列は空文字として返すことを確かめる。 空白だけの値を null にすると空の値と区別できなくなるので、空文字を返す。
       *
       * @given JSON の文字列が " "
       * @when OBJECT_MAPPER で String 型に読み込む
       * @then 空文字を返す
       */
      @Test
      void 空白だけの文字列の場合_空文字を返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue("\"   \"", String.class)).isEmpty();
      }

      /**
       * JSON の null を読み込むと null を返すことを確かめる。 JSON の null が文字列にならないと項目が欠けて見えるので、null を返す。
       *
       * @given JSON の値が null
       * @when OBJECT_MAPPER で String 型に読み込む
       * @then null を返す
       */
      @Test
      void JSONのnullの場合_nullを返す() throws Exception {
        assertThat(ObjectMapperFactory.OBJECT_MAPPER.readValue("null", String.class)).isNull();
      }
    }
  }
}
