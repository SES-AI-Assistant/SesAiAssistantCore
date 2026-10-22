package copel.sesproductpackage.core.api.gpt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link GptAnswer} の単体テスト。 */
class GptAnswerTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 回答の前後に空白がある場合に、前後を除いた回答を保持することを確かめる。 回答の判定は前後の空白を除いた文字列で行うため、保持する値を揃えることを守る。
       *
       * @given 回答が前後に空白を持つ " はい " である
       * @when GptAnswer を生成する
       * @then 前後を除いた "はい" を回答として保持する
       */
      @Test
      void 回答の前後に空白がある場合_前後を除いた回答を返す() {
        assertThat(new GptAnswer("  はい  ", GptAnswerTest.class).getAnswer()).isEqualTo("はい");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 回答が null の場合に、null を保持することを確かめる。 応答が無い場合を空の回答として扱い、例外で止めないことを守る。
       *
       * @given 回答が null である
       * @when GptAnswer を生成する
       * @then null を回答として保持する
       */
      @Test
      void 回答がnullの場合_nullを返す() {
        assertThat(new GptAnswer(null, GptAnswerTest.class).getAnswer()).isNull();
      }
    }
  }

  @Nested
  class isEmpty {

    @Nested
    class 正常系 {

      /**
       * 回答が文字列の場合に、空ではないと判定することを確かめる。 文字列のある回答を空として扱わないことを守る。
       *
       * @given 回答が "はい" である
       * @when isEmpty を呼び出す
       * @then false を返す
       */
      @Test
      void 回答が文字列の場合_falseを返す() {
        assertThat(answer("はい").isEmpty()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 回答が空白だけの場合に、空と判定することを確かめる。 空白だけの回答を内容のある回答として扱わないことを守る。
       *
       * @given 回答が " " である
       * @when isEmpty を呼び出す
       * @then true を返す
       */
      @Test
      void 回答が空白だけの場合_trueを返す() {
        assertThat(answer("   ").isEmpty()).isTrue();
      }

      /**
       * 回答が null の場合に、空と判定することを確かめる。 応答が無い場合を空として扱うことを守る。
       *
       * @given 回答が null である
       * @when isEmpty を呼び出す
       * @then true を返す
       */
      @Test
      void 回答がnullの場合_trueを返す() {
        assertThat(answer(null).isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class length {

    @Nested
    class 正常系 {

      /**
       * 回答が文字列の場合に、文字数を返すことを確かめる。 上限の判定に使うため、文字数を数えることを守る。
       *
       * @given 回答が "日本語" である
       * @when length を呼び出す
       * @then 3を返す
       */
      @Test
      void 回答が文字列の場合_文字数を返す() {
        assertThat(answer("日本語").length()).isEqualTo(3);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 回答が null の場合に、0を返すことを確かめる。 応答が無い場合に例外を投げず、空の長さとして扱うことを守る。
       *
       * @given 回答が null である
       * @when length を呼び出す
       * @then 0を返す
       */
      @Test
      void 回答がnullの場合_0を返す() {
        assertThat(answer(null).length()).isZero();
      }
    }
  }

  @Nested
  class isAlphanumeric {

    @Nested
    class 正常系 {

      /**
       * 英数字だけの回答の場合に、英数字と判定することを確かめる。 コードのような識別子の回答を受け付けることを守る。
       *
       * @given 回答が "abc123" である
       * @when isAlphanumeric を呼び出す
       * @then true を返す
       */
      @Test
      void 英数字だけの場合_trueを返す() {
        assertThat(answer("abc123").isAlphanumeric()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 記号を含む回答の場合に、英数字ではないと判定することを確かめる。 記号の混じった回答を識別子として受け付けないことを守る。
       *
       * @given 回答が "abc-1" である
       * @when isAlphanumeric を呼び出す
       * @then false を返す
       */
      @Test
      void 記号を含む場合_falseを返す() {
        assertThat(answer("abc-1").isAlphanumeric()).isFalse();
      }

      /**
       * 回答が null の場合に、英数字ではないと判定することを確かめる。 応答が無い場合を英数字として受け付けないことを守る。
       *
       * @given 回答が null である
       * @when isAlphanumeric を呼び出す
       * @then false を返す
       */
      @Test
      void 回答がnullの場合_falseを返す() {
        assertThat(answer(null).isAlphanumeric()).isFalse();
      }
    }
  }

  @Nested
  class equals {

    @Nested
    class 正常系 {

      /**
       * 同じ文字列と比べた場合に、一致と判定することを確かめる。 回答が指定の語と同じかどうかを判定することを守る。
       *
       * @given 回答が "はい" である
       * @when 比較対象の "はい" で equals を呼び出す
       * @then true を返す
       */
      @Test
      void 同じ文字列の場合_trueを返す() {
        assertThat(answer("はい").equals("はい")).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 異なる文字列と比べた場合に、不一致と判定することを確かめる。 別の回答を同じものとして扱わないことを守る。
       *
       * @given 回答が "はい" である
       * @when 比較対象の "いいえ" で equals を呼び出す
       * @then false を返す
       */
      @Test
      void 異なる文字列の場合_falseを返す() {
        assertThat(answer("はい").equals("いいえ")).isFalse();
      }

      /**
       * 回答と比較対象が両方 null の場合に、一致と判定することを確かめる。 両方が無い場合を同じ状態として扱うことを守る。
       *
       * @given 回答が null である
       * @when 比較対象の null で equals を呼び出す
       * @then true を返す
       */
      @Test
      void 両方がnullの場合_trueを返す() {
        assertThat(answer(null).equals((String) null)).isTrue();
      }

      /**
       * 回答が null で比較対象が文字列の場合に、不一致と判定することを確かめる。 回答が無いときに、文字列との一致を誤って判定しないことを守る。
       *
       * @given 回答が null である
       * @when 比較対象の "はい" で equals を呼び出す
       * @then false を返す
       */
      @Test
      void 回答がnullで比較対象が文字列の場合_falseを返す() {
        assertThat(answer(null).equals("はい")).isFalse();
      }
    }
  }

  @Nested
  class contains {

    @Nested
    class 正常系 {

      /**
       * 回答が指定の文字列を含む場合に、真を返すことを確かめる。 回答の中に特定の語があるかを判定することを守る。
       *
       * @given 回答が "はい、承知しました" である
       * @when 比較対象の "承知" で contains を呼び出す
       * @then true を返す
       */
      @Test
      void 指定の文字列を含む場合_trueを返す() {
        assertThat(answer("はい、承知しました").contains("承知")).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 回答が指定の文字列を含まない場合に、偽を返すことを確かめる。 含まない語を含むと誤って判定しないことを守る。
       *
       * @given 回答が "はい" である
       * @when 比較対象の "いいえ" で contains を呼び出す
       * @then false を返す
       */
      @Test
      void 指定の文字列を含まない場合_falseを返す() {
        assertThat(answer("はい").contains("いいえ")).isFalse();
      }

      /**
       * 比較対象が null の場合に、偽を返すことを確かめる。 比較対象が無い判定で例外を投げないことを守る。
       *
       * @given 回答が "はい" である
       * @when 比較対象の null で contains を呼び出す
       * @then false を返す
       */
      @Test
      void 比較対象がnullの場合_falseを返す() {
        assertThat(answer("はい").contains(null)).isFalse();
      }

      /**
       * 回答が null の場合に、偽を返すことを確かめる。 応答が無いときに含む判定を行わないことを守る。
       *
       * @given 回答が null である
       * @when 比較対象の "はい" で contains を呼び出す
       * @then false を返す
       */
      @Test
      void 回答がnullの場合_falseを返す() {
        assertThat(answer(null).contains("はい")).isFalse();
      }
    }
  }

  @Nested
  class asInt {

    @Nested
    class 正常系 {

      /**
       * 回答が数字の文字列の場合に、整数を返すことを確かめる。 件数の数値を応答から取り出すことを守る。
       *
       * @given 回答が "42" である
       * @when asInt を呼び出す
       * @then 42を返す
       */
      @Test
      void 数字の文字列の場合_整数を返す() {
        assertThat(answer("42").asInt()).isEqualTo(42);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 数字ではない文字列の場合に、例外を投げることを確かめる。 数字として解釈できない応答を黙って 0 にしないことを守る。
       *
       * @given 回答が "abc" である
       * @when asInt を呼び出す
       * @then NumberFormatException を投げる
       */
      @Test
      void 数字ではない文字列の場合_NumberFormatExceptionを投げる() {
        assertThatThrownBy(() -> answer("abc").asInt()).isInstanceOf(NumberFormatException.class);
      }

      /**
       * 回答が null の場合に、例外を投げることを確かめる。 応答が無い場合を数値として扱わないことを守る。
       *
       * @given 回答が null である
       * @when asInt を呼び出す
       * @then NumberFormatException を投げる
       */
      @Test
      void 回答がnullの場合_NumberFormatExceptionを投げる() {
        assertThatThrownBy(() -> answer(null).asInt()).isInstanceOf(NumberFormatException.class);
      }
    }
  }

  @Nested
  class getAsList {

    @Nested
    class 正常系 {

      /**
       * 前後に文章がある配列の場合に、配列の要素を文字列の一覧で返すことを確かめる。 AI の応答は前後に説明が付くことがあるため、配列の部分だけを取り出すことを守る。
       *
       * @given 回答が前置きと {@code ["a","b"]} と後置きを含む
       * @when getAsList を呼び出す
       * @then a と b の一覧を返す
       */
      @Test
      void 前後に文章がある配列の場合_要素の文字列一覧を返す() throws Exception {
        assertThat(answer("前置き [\"a\",\"b\"] 後置き").getAsList()).isEqualTo(List.of("a", "b"));
      }

      /**
       * 配列の要素がオブジェクトの場合に、要素を JSON の文字列にして返すことを確かめる。 要素の型が文字列でも一覧として扱えるようにすることを守る。
       *
       * @given 回答が JSON のオブジェクトを1件含む配列である
       * @when getAsList を呼び出す
       * @then 要素を JSON 文字列の一覧として返す
       */
      @Test
      void 要素がオブジェクトの場合_要素をJSON文字列にした一覧を返す() throws Exception {
        assertThat(answer("[{\"k\":1}]").getAsList()).isEqualTo(List.of("{\"k\":1}"));
      }
    }

    @Nested
    class 異常系 {

      /**
       * 配列の形式が解析できない場合に、JSON の例外を投げることを確かめる。 解析できない応答を空として扱うと、誤った結果を使うことを守る。
       *
       * @given 回答が {@code [abc]} である
       * @when getAsList を呼び出す
       * @then JsonProcessingException を投げる
       */
      @Test
      void 配列の形式が解析できない場合_JsonProcessingExceptionを投げる() {
        assertThatThrownBy(() -> answer("[abc]").getAsList())
            .isInstanceOf(JsonProcessingException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 配列の括弧が無い場合に、null を返すことを確かめる。 配列が無い応答を空の一覧と区別して、呼び出し元が判断できるようにすることを守る。
       *
       * @given 回答が "括弧の無い文章" である
       * @when getAsList を呼び出す
       * @then null を返す
       */
      @Test
      void 配列の括弧が無い場合_nullを返す() throws Exception {
        assertThat(answer("括弧の無い文章").getAsList()).isNull();
      }

      /**
       * 文字列の中に改行がある配列の場合に、制御文字をエスケープして解析することを確かめる。 改行を含む応答でも要素を失わないようにすることを守る。
       *
       * @given 回答が改行を含む文字列の要素を持つ配列である
       * @when getAsList を呼び出す
       * @then 改行を含む1件の要素を返す
       */
      @Test
      void 文字列の中に改行がある場合_制御文字をエスケープして一覧を返す() throws Exception {
        assertThat(answer("[\"a\nb\"]").getAsList()).isEqualTo(List.of("a\nb"));
      }

      /**
       * 回答が null の場合に、空のリストを返すことを確かめる。 回答が無い場合に例外にせず、配列が無い場合と同じ扱いにすることを守る。
       *
       * @given 回答が null である
       * @when getAsList を呼び出す
       * @then 空のリストを返す
       */
      @Test
      void 回答がnullの場合_空のリストを返す() throws Exception {
        assertThat(answer(null).getAsList()).isEmpty();
      }
    }
  }

  @Nested
  class getAnswer {

    @Nested
    class 正常系 {

      /**
       * 保持している回答を返すことを確かめる。 呼び出し元が応答の本文をそのまま使えるようにすることを守る。
       *
       * @given 回答が "はい" である
       * @when getAnswer を呼び出す
       * @then "はい" を返す
       */
      @Test
      void 回答を保持する場合_回答を返す() {
        assertThat(answer("はい").getAnswer()).isEqualTo("はい");
      }
    }
  }

  @Nested
  class toString {

    @Nested
    class 正常系 {

      /**
       * 保持している回答を文字列として返すことを確かめる。 ログに回答の内容を残すため、回答の文字列をそのまま出すことを守る。
       *
       * @given 回答が "はい" である
       * @when toString を呼び出す
       * @then "はい" を返す
       */
      @Test
      void 回答を保持する場合_回答の文字列を返す() {
        assertThat(answer("はい").toString()).isEqualTo("はい");
      }
    }
  }

  /**
   * 回答を持つ GptAnswer を作る.
   *
   * @param answer 回答
   * @return GptAnswer
   */
  private static GptAnswer answer(final String answer) {
    return new GptAnswer(answer, GptAnswerTest.class);
  }
}
