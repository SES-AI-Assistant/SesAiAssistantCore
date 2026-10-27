package copel.sesproductpackage.core.api.gpt.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import copel.sesproductpackage.core.api.gpt.entity.JobInfoSchema;
import copel.sesproductpackage.core.api.gpt.entity.PersonInfoSchema;
import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SchemaGenerator} の単体テスト。 */
class SchemaGeneratorTest {

  @Nested
  class generate {

    @Nested
    class 正常系 {

      /**
       * 単純な型のフィールドを持つ場合に、型と必須項目を含むスキーマを返すことを確かめる。 入力の型定義からスキーマを作る基本の動作を守ることで、AI への指示が型と一致することを守る。
       *
       * @given 必須の文字列フィールドと、整数と真偽値のフィールドを持つクラスがある
       * @when generate を対象クラスで呼び出す
       * @then type が object で、必須に text を含むスキーマを返す
       */
      @Test
      void 単純な型のフィールドを持つ場合_型と必須項目を含むスキーマを返す() {
        Map<String, Object> schema = SchemaGenerator.generate(Basic.class);

        assertThat(schema).containsEntry("type", "object");
        assertThat(schema.get("required")).isEqualTo(List.of("text"));
        assertThat(properties(schema))
            .containsEntry("text", Map.of("type", "string", "description", "本文"))
            .containsEntry("count", Map.of("type", "integer"))
            .containsEntry("flag", Map.of("type", "boolean"));
      }

      /**
       * 数値の型を全て持つ場合に、整数は integer、小数は number として表すことを確かめる。 型の違いを AI に伝えないと、小数の値を整数で返させてしまうことを守る。
       *
       * @given 整数、長整数、小数、単精度の小数のフィールドを、プリミティブとラッパーの両方で持つ
       * @when generate を対象クラスで呼び出す
       * @then 整数系は integer、小数系は number で表す
       */
      @Test
      void 数値の型を全て持つ場合_整数をinteger小数をnumberとするスキーマを返す() {
        Map<String, Object> props = properties(SchemaGenerator.generate(NumericTypes.class));

        assertThat(props)
            .containsEntry("big", Map.of("type", "integer"))
            .containsEntry("boxed_big", Map.of("type", "integer"))
            .containsEntry("ratio", Map.of("type", "number"))
            .containsEntry("boxed_ratio", Map.of("type", "number"))
            .containsEntry("score", Map.of("type", "number"))
            .containsEntry("boxed_score", Map.of("type", "number"))
            .containsEntry("boxed_flag", Map.of("type", "boolean"))
            .containsEntry("boxed_count", Map.of("type", "integer"));
      }

      /**
       * 親クラスのフィールドを持つ場合に、親のフィールドを先に並べて含めることを確かめる。 継承したフィールドを落とすと、AI の応答から一部の項目が欠けることを守る。
       *
       * @given 親クラスに parentField があり、子クラスに childField がある
       * @when generate を子クラスで呼び出す
       * @then parentField と childField の両方を含み、parentField を先に並べる
       */
      @Test
      void 親クラスのフィールドを持つ場合_親のフィールドを含むスキーマを返す() {
        Map<String, Object> props = properties(SchemaGenerator.generate(Child.class));

        assertThat(props).containsOnlyKeys("parent_field", "child_field");
        assertThat(props.keySet()).containsExactly("parent_field", "child_field");
      }

      /**
       * 列挙型のフィールドを持つ場合に、列挙値の一覧を含めることを確かめる。 列挙値以外の値を AI に返させないための制約になることを守る。
       *
       * @given 列挙型 Color のフィールドがある
       * @when generate を対象クラスで呼び出す
       * @then type が string で、RED と BLUE の列挙値を含む
       */
      @Test
      void 列挙型のフィールドを持つ場合_列挙値の一覧を含むスキーマを返す() {
        Map<String, Object> props = properties(SchemaGenerator.generate(Holder.class));

        assertThat(props.get("color"))
            .isEqualTo(Map.of("type", "string", "enum", List.of("RED", "BLUE")));
      }

      /**
       * 入れ子のオブジェクトのフィールドを持つ場合に、入れ子のプロパティを含めることを確かめる。 入れ子の項目も AI の応答に含める必要があることを守る。
       *
       * @given 入れ子のクラス Children のフィールドがある
       * @when generate を対象クラスで呼び出す
       * @then child の type が object で、子の childText を必須として含む
       */
      @Test
      void 入れ子のオブジェクトのフィールドを持つ場合_入れ子のプロパティを含むスキーマを返す() {
        Map<String, Object> child = property(SchemaGenerator.generate(Holder.class), "child");

        assertThat(child).containsEntry("type", "object");
        assertThat(castMap(child.get("properties"))).containsKey("child_text");
      }

      /**
       * 要素が未指定の配列を持つ場合に、要素の型を string として表すことを確かめる。 要素の型が無いまま出力すると、AI の応答の形式が定まらないことを守る。
       *
       * @given 要素型を指定しない文字列の一覧 tags がある
       * @when generate を対象クラスで呼び出す
       * @then tags の type が array で、items の type が string である
       */
      @Test
      void 要素型が未指定の配列の場合_要素の型がstringのスキーマを返す() {
        Map<String, Object> tags = property(SchemaGenerator.generate(Holder.class), "tags");

        assertThat(tags).containsEntry("type", "array");
        assertThat(tags.get("items")).isEqualTo(Map.of("type", "string"));
      }

      /**
       * 要素型が列挙型の配列を持つ場合に、要素を列挙値の一覧として表すことを確かめる。 配列の要素にも列挙値の制約を付けることを守る。
       *
       * @given 要素型 Color の一覧 colors がある
       * @when generate を対象クラスで呼び出す
       * @then items が列挙値 RED と BLUE の string である
       */
      @Test
      void 要素型が列挙型の配列の場合_要素が列挙値の一覧のスキーマを返す() {
        Map<String, Object> colors = property(SchemaGenerator.generate(Holder.class), "colors");

        assertThat(colors.get("items"))
            .isEqualTo(Map.of("type", "string", "enum", List.of("RED", "BLUE")));
      }

      /**
       * 要素型が入れ子のオブジェクトの配列を持つ場合に、要素を object として表すことを確かめる。 配列の要素の中身も AI の応答に含めることを守る。
       *
       * @given 要素型 Children の一覧 items があり、件数の制約が1と5である
       * @when generate を対象クラスで呼び出す
       * @then items の type が object で、minItems と maxItems を含む
       */
      @Test
      void 要素型が入れ子のオブジェクトの配列の場合_要素がobjectで件数の制約を含むスキーマを返す() {
        Map<String, Object> items = property(SchemaGenerator.generate(Holder.class), "items");

        assertThat(items).containsEntry("minItems", 1).containsEntry("maxItems", 5);
        assertThat(castMap(items.get("items"))).containsEntry("type", "object");
      }

      /**
       * 要素型が配列の場合に、要素を array として表すことを確かめる。 二次元の配列を扱う項目で、要素の型を伝えることを守る。
       *
       * @given 要素型が String の配列である arrays がある
       * @when generate を対象クラスで呼び出す
       * @then items の type が array である
       */
      @Test
      void 要素型が配列の場合_要素がarrayのスキーマを返す() {
        Map<String, Object> arrays = property(SchemaGenerator.generate(Holder.class), "arrays");

        assertThat(arrays.get("items")).isEqualTo(Map.of("type", "array"));
      }

      /**
       * 要素型が List の配列を持つ場合に、要素を array として表すことを確かめる。 入れ子の一覧も配列として表示することを守る。
       *
       * @given 要素型が List の一覧 lists がある
       * @when generate を対象クラスで呼び出す
       * @then items の type が array である
       */
      @Test
      void 要素型がListの配列の場合_要素がarrayのスキーマを返す() {
        Map<String, Object> lists = property(SchemaGenerator.generate(Holder.class), "lists");

        assertThat(lists.get("items")).isEqualTo(Map.of("type", "array"));
      }

      /**
       * 要素型が単純型の配列を持つ場合に、要素の型を単純型として表すことを確かめる。 数値の一覧を文字列として扱わないようにすることを守る。
       *
       * @given 要素型が Integer の一覧 numbers がある
       * @when generate を対象クラスで呼び出す
       * @then items の type が integer である
       */
      @Test
      void 要素型が単純型の配列の場合_要素が単純型のスキーマを返す() {
        Map<String, Object> numbers = property(SchemaGenerator.generate(Holder.class), "numbers");

        assertThat(numbers.get("items")).isEqualTo(Map.of("type", "integer"));
      }

      /**
       * 除外指定のフィールドを持つ場合に、そのフィールドをスキーマに含めないことを確かめる。 内部用の項目を AI に見せないようにすることを守る。
       *
       * @given SchemaIgnore を付けた ignored フィールドがある
       * @when generate を対象クラスで呼び出す
       * @then properties に ignored を含まない
       */
      @Test
      void 除外指定のフィールドがある場合_そのフィールドを含まないスキーマを返す() {
        assertThat(properties(SchemaGenerator.generate(Tagged.class))).doesNotContainKey("ignored");
      }

      /**
       * 静的、一時、$ で始まるフィールドがある場合に、それらを含めないことを確かめる。 実行に使う内部の値を AI の応答の項目として扱わないようにすることを守る。
       *
       * @given 静的フィールド、transient フィールド、$ で始まるフィールドがある
       * @when generate を対象クラスで呼び出す
       * @then properties にそれらのフィールドを含まない
       */
      @Test
      void 静的と一時と$で始まるフィールドがある場合_それらを含まないスキーマを返す() {
        Map<String, Object> props = properties(SchemaGenerator.generate(Tagged.class));

        assertThat(props).doesNotContainKeys("static_field", "transient_field", "$dollar");
      }

      /**
       * JsonProperty の指定がある場合に、指定した名前をプロパティ名に使うことを確かめる。 応答の項目名は JsonProperty と一致させる必要があることを守る。
       *
       * @given JsonProperty で custom_name と指定した named フィールドがある
       * @when generate を対象クラスで呼び出す
       * @then プロパティ名に custom_name を含む
       */
      @Test
      void JsonPropertyの指定がある場合_指定した名前をプロパティ名に含むスキーマを返す() {
        assertThat(properties(SchemaGenerator.generate(Tagged.class))).containsKey("custom_name");
      }

      /**
       * 文字数と書式の制約を持つ場合に、それらを含むスキーマを返すことを確かめる。 制約を AI に示すことで、範囲外の値を返させないことを守る。
       *
       * @given 文字数の上限と下限、書式、タイトル、既定値、例、データ元を持つフィールドがある
       * @when generate を対象クラスで呼び出す
       * @then minLength、maxLength、pattern、format、title、default、example、x-data-source を含む
       */
      @Test
      void 文字数と書式の制約がある場合_制約を含むスキーマを返す() {
        Map<String, Object> text = property(SchemaGenerator.generate(Constraints.class), "text");

        assertThat(text)
            .containsEntry("type", "string")
            .containsEntry("minLength", 1)
            .containsEntry("maxLength", 10)
            .containsEntry("pattern", "^a")
            .containsEntry("format", "date")
            .containsEntry("title", "題")
            .containsEntry("default", "x")
            .containsEntry("example", "ex")
            .containsEntry("x-data-source", "ds")
            .containsEntry("description", "名前");
      }

      /**
       * 範囲と桁数の制約を持つ場合に、それらを含むスキーマを返すことを確かめる。 数値の範囲を AI に示し、範囲外の点数を返させないことを守る。
       *
       * @given 下限0、上限100、桁数の下限1と上限3を持つフィールドがある
       * @when generate を対象クラスで呼び出す
       * @then exclusiveMinimum、exclusiveMaximum、minDigits、maxDigits を含む
       */
      @Test
      void 範囲と桁数の制約がある場合_範囲と桁数を含むスキーマを返す() {
        Map<String, Object> number =
            property(SchemaGenerator.generate(Constraints.class), "number");

        assertThat(number)
            .containsEntry("exclusiveMinimum", 0L)
            .containsEntry("exclusiveMaximum", 100L)
            .containsEntry("minDigits", 1)
            .containsEntry("maxDigits", 3);
      }

      /**
       * 型の指定がある場合に、指定した型でスキーマを作ることを確かめる。 Java の型と AI に渡す型が違う項目を指定どおりに表すことを守る。
       *
       * @given 整数のフィールドに type として number を指定している
       * @when generate を対象クラスで呼び出す
       * @then そのフィールドの type が number である
       */
      @Test
      void 型の指定がある場合_指定した型のスキーマを返す() {
        assertThat(property(SchemaGenerator.generate(Constraints.class), "typed"))
            .containsEntry("type", "number");
      }

      /**
       * BigDecimal のフィールドを持つ場合に、型を string として表すことを確かめる。 金額の型をどう表すかは未決定なので、現在の挙動を固定する。
       *
       * @given BigDecimal 型の amount フィールドがある
       * @when generate を対象クラスで呼び出す
       * @then amount の type が string である
       */
      @Test
      void BigDecimalのフィールドがある場合_型がstringのスキーマを返す() {
        assertThat(property(SchemaGenerator.generate(Holder.class), "amount"))
            .containsEntry("type", "string");
      }

      /**
       * 必須の指定が無い場合に、必須の項目を含めないことを確かめる。 任意の項目を必須として AI に要求しないようにすることを守る。
       *
       * @given 必須の指定が無い name フィールドだけを持つ
       * @when generate を対象クラスで呼び出す
       * @then required を含まない
       */
      @Test
      void 必須の指定が無い場合_requiredを含まないスキーマを返す() {
        assertThat(SchemaGenerator.generate(OptionalOnly.class)).doesNotContainKey("required");
      }

      /**
       * フィールドが無い場合に、空のプロパティを持つスキーマを返すことを確かめる。 空のクラスでも例外にせず、空の定義として扱うことを守る。
       *
       * @given フィールドを持たない Empty クラスがある
       * @when generate を対象クラスで呼び出す
       * @then properties が空で、required を含まない
       */
      @Test
      void フィールドが無い場合_空のプロパティを持つスキーマを返す() {
        Map<String, Object> schema = SchemaGenerator.generate(Empty.class);

        assertThat(properties(schema)).isEmpty();
        assertThat(schema).doesNotContainKey("required");
      }

      /**
       * 配列、列挙、入れ子、必須の注釈を持つ場合に、それぞれの定義を含むスキーマを返すことを確かめる。 注釈ごとの制約が出力に反映されることを守る。
       *
       * @given 配列、列挙、入れ子の型に注釈があるクラスがある
       * @when generate を対象クラスで呼び出す
       * @then 配列の制約、列挙の説明、入れ子の説明、必須の項目を含むスキーマを返す
       */
      @Test
      void 配列とenumと入れ子の注釈がある場合_それぞれの定義を含むスキーマを返す() {
        Map<String, Object> schema = SchemaGenerator.generate(Annotated.class);
        Map<String, Object> props = properties(schema);

        assertThat(property(schema, "codes")).containsEntry("type", "array");
        assertThat(property(schema, "ids")).containsEntry("minItems", 1);
        assertThat(property(schema, "favorite")).containsEntry("description", "色");
        assertThat(property(schema, "with_schema")).containsEntry("description", "子");
        assertThat(property(schema, "plain")).doesNotContainKey("description");
        assertThat(castMap(property(schema, "empties").get("items")))
            .doesNotContainKey("properties");
        assertThat(schema.get("required")).isEqualTo(List.of("plain"));
        assertThat(props)
            .containsKeys("codes", "ids", "empties", "favorite", "with_schema", "plain");
      }

      /**
       * 入れ子の型が静的フィールドを持つ場合に、静的フィールドを除いたプロパティを返すことを確かめる。 内部の静的な値を AI への項目として出さないことを守る。
       *
       * @given 入れ子の型 WithStatic が静的フィールド shared と値フィールド value を持つ
       * @when generate を NestedStatic で呼び出す
       * @then 入れ子のプロパティは value だけを持つ
       */
      @Test
      void 入れ子の型が静的フィールドを持つ場合_静的フィールドを除いたプロパティを返す() {
        Map<String, Object> inner = property(SchemaGenerator.generate(NestedStatic.class), "inner");

        assertThat(castMap(inner.get("properties"))).containsOnlyKeys("value");
      }
    }

    @Nested
    class 異常系 {

      /**
       * クラスが null の場合に、例外を投げることを確かめる。 対象が無いまま空のスキーマを返すと、誤ったスキーマが送信されることを守る。
       *
       * @given 対象クラスが null である
       * @when generate を呼び出す
       * @then RuntimeException を投げる
       */
      @Test
      void クラスがnullの場合_RuntimeExceptionを投げる() {
        assertThatThrownBy(() -> SchemaGenerator.generate(null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Class must not be null");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 条件付きスキーマのケースを持つ場合に、ケースごとの anyOf を含むことを確かめる。 結果の種類ごとにペイロードの形を分けて AI に示すことを守る。
       *
       * @given 成功と失敗の2つのケースがあり、成功のケースには題と説明がある
       * @when generate を対象クラスで呼び出す
       * @then result の anyOf に成功の題と説明、失敗の既定の題を含む
       */
      @Test
      void 条件付きスキーマのケースがある場合_ケースごとのanyOfを含むスキーマを返す() {
        Map<String, Object> result =
            property(SchemaGenerator.generate(Conditional.class), "result");

        assertThat(result).containsEntry("description", "結果");
        assertThat(result.get("anyOf"))
            .isEqualTo(
                List.of(
                    Map.of(
                        "type",
                        "object",
                        "title",
                        "成功",
                        "description",
                        "成功時",
                        "properties",
                        Map.of("data", Map.of("type", "string")),
                        "required",
                        List.of("data")),
                    Map.of(
                        "type",
                        "object",
                        "title",
                        "Ng",
                        "properties",
                        Map.of("message", Map.of("type", "string")))));
      }

      /**
       * 全てのケースが除外指定の場合に、そのフィールドをスキーマに含めないことを確かめる。 出力しない結果の項目を AI に指示しないようにすることを守る。
       *
       * @given 全てのケースに除外の値が指定されている
       * @when generate を対象クラスで呼び出す
       * @then properties に result を含まない
       */
      @Test
      void 全てのケースが除外指定の場合_プロパティに含まないスキーマを返す() {
        assertThat(properties(SchemaGenerator.generate(AllExcluded.class)))
            .doesNotContainKey("result");
      }

      /**
       * 除外の値が一部のケースだけにある場合に、全てのケースを anyOf に含めることを確かめる。 除外されないケースを落とさないことを守る。
       *
       * @given 除外の値があるケースと無いケースがある
       * @when generate を対象クラスで呼び出す
       * @then result を含み、anyOf に2件を持つ
       */
      @Test
      void 除外の値が一部のケースだけにある場合_全てのケースを含むanyOfを返す() {
        Map<String, Object> result =
            property(SchemaGenerator.generate(ExcludeSome.class), "result");

        assertThat((List<?>) result.get("anyOf")).hasSize(2);
      }

      /**
       * 条件付きスキーマのケースが無い場合に、空の anyOf を返すことを確かめる。 ケースが無い定義でも例外にしないことを守る。
       *
       * @given 条件付きスキーマのケースが空である
       * @when generate を対象クラスで呼び出す
       * @then result の anyOf が空である
       */
      @Test
      void 条件付きスキーマのケースが無い場合_空のanyOfを返す() {
        Map<String, Object> result = property(SchemaGenerator.generate(NoCases.class), "result");

        assertThat(result.get("anyOf")).isEqualTo(List.of());
      }

      /**
       * 現在の年と翌年だけの制約を持つ場合に、その年を含む書式で表すことを確かめる。 年の選択肢を現在の年に合わせ、古い年の値を AI に返させないことを守る。
       *
       * @given 現在時刻が2026年10月20日で、年の制約がある year フィールドがある
       * @when generate を対象クラスで呼び出す
       * @then year の pattern が 2026 と 2027 を含む既定の書式である
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 現在年と翌年だけの制約で書式がない場合_既定の年の書式を返す() {
        assertThat(property(SchemaGenerator.generate(YearConstraints.class), "year"))
            .containsEntry("pattern", "^(2026|2027)/(0?[1-9]|1[0-2])$");
      }

      /**
       * 年の制約と書式が両方ある場合に、書式の年の部分を置き換えることを確かめる。 指定した書式の形を残しつつ、年の選択肢だけを現在の年に合わせることを守る。
       *
       * @given 現在時刻が2026年10月20日で、書式の年部分が {@code \d{4}} と {@code [0-9]{4}} である
       * @when generate を対象クラスで呼び出す
       * @then 両方の年の部分を {@code (2026|2027)} に置き換えた書式を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 年の制約と書式がある場合_年の部分を置き換えた書式を返す() {
        Map<String, Object> props = properties(SchemaGenerator.generate(YearConstraints.class));

        assertThat(castMap(props.get("year_pattern")))
            .containsEntry("pattern", "^(2026|2027)/\\d{1,2}$");
        assertThat(castMap(props.get("bracket_pattern")))
            .containsEntry("pattern", "^(2026|2027)/x$");
      }

      /**
       * 現在の年と翌年だけの制約があり説明文が無い場合に、本日の日付だけを説明文にすることを確かめる。 AI は今日の日付を知らないため、日付を渡さないと即日を本日の年月にできない。
       *
       * @given 現在時刻が2026年10月20日で、説明文が無い year フィールドがある
       * @when generate を対象クラスで呼び出す
       * @then year の description が本日は2026年10月20日である
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 年の制約があり説明文がない場合_本日の日付だけの説明文を返す() {
        assertThat(property(SchemaGenerator.generate(YearConstraints.class), "year"))
            .containsEntry("description", "本日は2026年10月20日である。");
      }

      /**
       * 現在の年と翌年だけの制約があり説明文がある場合に、説明文の末尾へ本日の日付を足すことを確かめる。 指定した説明文を残したまま、AI に今日の日付を伝える必要がある。
       *
       * @given 現在時刻が2026年10月5日で、説明文が開始年月の説明である described フィールドがある
       * @when generate を対象クラスで呼び出す
       * @then described の description が説明文の後に本日は2026年10月5日であることを続ける
       */
      @Test
      @FixedClock("2026-10-05T10:00:00+09:00")
      void 年の制約があり説明文がある場合_説明文の末尾に本日の日付を足した説明文を返す() {
        assertThat(property(SchemaGenerator.generate(YearConstraints.class), "described"))
            .containsEntry("description", "開始年月。本日は2026年10月5日である。");
      }

      /**
       * 案件情報の開始年月が、本日の日付を含む説明文と現在年の書式で生成されることを確かめる。 AI は今日の日付を知らないため、即日を本日の年月にするには説明文で日付を渡す必要がある。
       *
       * @given 現在時刻が2026年10月27日で、入力が JobInfoSchema である
       * @when generate を JobInfoSchema で呼び出す
       * @then start_year_month は本日の日付を続けた説明文と現在年と翌年の書式を持ち、example を持たず、必須に含まれる
       */
      @Test
      @FixedClock("2026-10-27T10:00:00+09:00")
      void 入力が案件情報の場合_開始年月に本日の日付を含むスキーマを返す() {
        Map<String, Object> schema = SchemaGenerator.generate(JobInfoSchema.class);
        Map<String, Object> startYearMonth = property(schema, "start_year_month");

        assertThat(startYearMonth)
            .containsEntry(
                "description",
                "案件の開始年月。必ずyyyy/MM形式で設定してください。未記載の場合や即日、などの表現がされている場合は本日日付にして。本日は2026年10月27日である。")
            .containsEntry("pattern", "^(2026|2027)/(0?[1-9]|1[0-2])$")
            .doesNotContainKey("example");
        assertThat(((List<?>) schema.get("required")).contains("start_year_month")).isTrue();
      }

      /**
       * 要員情報の開始年月が、本日の日付を含む説明文と現在年の書式で生成されることを確かめる。 AI は今日の日付を知らないため、即日を本日の年月にするには説明文で日付を渡す必要がある。
       *
       * @given 現在時刻が2026年10月27日で、入力が PersonInfoSchema である
       * @when generate を PersonInfoSchema で呼び出す
       * @then start_year_month は本日の日付を続けた説明文と現在年と翌年の書式を持ち、example を持たず、必須に含まれる
       */
      @Test
      @FixedClock("2026-10-27T10:00:00+09:00")
      void 入力が要員情報の場合_開始年月に本日の日付を含むスキーマを返す() {
        Map<String, Object> schema = SchemaGenerator.generate(PersonInfoSchema.class);
        Map<String, Object> startYearMonth = property(schema, "start_year_month");

        assertThat(startYearMonth)
            .containsEntry(
                "description",
                "稼働開始が可能な年月。必ずyyyy/MM形式で設定してください。未記載の場合や即日、などの表現がされている場合は本日日付にして。本日は2026年10月27日である。")
            .containsEntry("pattern", "^(2026|2027)/(0?[1-9]|1[0-2])$")
            .doesNotContainKey("example");
        assertThat(((List<?>) schema.get("required")).contains("start_year_month")).isTrue();
      }

      /**
       * 現在時刻が取得できない場合に、スキーマ生成の例外として投げることを確かめる。 時刻の失敗を隠さず、呼び出し元に例外として伝えることを守る。
       *
       * @given 現在時刻の取得が例外を投げる時計を設定している
       * @when generate を年の制約があるクラスで呼び出す
       * @then RuntimeException を投げる
       */
      @Test
      void 時刻の取得が失敗する場合_RuntimeExceptionを投げる() {
        ApplicationClock.INSTANCE.setClock(failingClock());
        try {
          assertThatThrownBy(() -> SchemaGenerator.generate(YearConstraints.class))
              .isInstanceOf(RuntimeException.class);
        } finally {
          ApplicationClock.INSTANCE.resetToDefault();
        }
      }

      /**
       * 入れ子の型の全てのケースが除外指定の場合に、そのフィールドを含まないことを確かめる。 入れ子の内部でも除外指定の項目を AI に見せないことを守る。
       *
       * @given 入れ子の型 ExcludedInner が除外指定の条件付きスキーマを持つ
       * @when generate を NestedExcluded で呼び出す
       * @then 入れ子のスキーマは type だけを持ち、result を含まない
       */
      @Test
      void 入れ子の型の全てのケースが除外指定の場合_そのフィールドを含まないプロパティを返す() {
        Map<String, Object> inner =
            property(SchemaGenerator.generate(NestedExcluded.class), "inner");

        assertThat(inner).containsEntry("type", "object").doesNotContainKey("properties");
      }

      /**
       * ケースの型がプロパティを持たない場合に、そのケースに properties を含めないことを確かめる。 空の型をケースとして出しても例外にしないことを守る。
       *
       * @given ケースの型 Empty がフィールドを持たない
       * @when generate を EmptyCase で呼び出す
       * @then 最初のケースに properties を含まない
       */
      @Test
      void ケースの型がプロパティを持たない場合_propertiesを含まないケースを返す() {
        Map<String, Object> result = property(SchemaGenerator.generate(EmptyCase.class), "result");

        assertThat(((List<?>) result.get("anyOf")).get(0)).isInstanceOf(Map.class);
        assertThat(castMap(((List<?>) result.get("anyOf")).get(0))).doesNotContainKey("properties");
      }

      /**
       * ケースの型が静的フィールドを持つ場合に、静的フィールドを除いたプロパティを返すことを確かめる。 条件付きスキーマのケースでも内部の静的な値を出さないことを守る。
       *
       * @given ケースの型 WithStatic が静的フィールド shared と値フィールド value を持つ
       * @when generate を StaticCase で呼び出す
       * @then ケースのプロパティは value だけを持つ
       */
      @Test
      void ケースの型が静的フィールドを持つ場合_静的フィールドを除いたプロパティを返す() {
        Map<String, Object> result = property(SchemaGenerator.generate(StaticCase.class), "result");
        Map<String, Object> caseSchema = castMap(((List<?>) result.get("anyOf")).get(0));

        assertThat(castMap(caseSchema.get("properties"))).containsOnlyKeys("value");
      }

      /**
       * ケースの型の注釈が必須でない場合に、必須の項目を含めないことを確かめる。 必須ではない項目を必須として AI に要求しないことを守る。
       *
       * @given ケースの型 Note が必須ではない注釈の memo を持つ
       * @when generate を NoteCase で呼び出す
       * @then ケースに required を含まない
       */
      @Test
      void ケースの型の注釈が必須でない場合_requiredを含まないケースを返す() {
        Map<String, Object> result = property(SchemaGenerator.generate(NoteCase.class), "result");
        Map<String, Object> caseSchema = castMap(((List<?>) result.get("anyOf")).get(0));

        assertThat(caseSchema).doesNotContainKey("required");
      }

      /**
       * インターフェースの場合に、フィールドを持たないスキーマを返すことを確かめる。 親を持たない型でも例外にせず空の定義を返すことを守る。
       *
       * @given 対象が Runnable のインターフェースである
       * @when generate を対象クラスで呼び出す
       * @then 空のプロパティを持つスキーマを返す
       */
      @Test
      void インターフェースの場合_フィールドを持たないスキーマを返す() {
        assertThat(properties(SchemaGenerator.generate(Runnable.class))).isEmpty();
      }

      /**
       * 説明の無い注釈を持つ条件付きスキーマの場合に、説明を含めないことを確かめる。 注釈に説明が無い条件付きスキーマで説明の項目を空にしないことを守る。
       *
       * @given 条件付きスキーマの注釈が題だけを持つ
       * @when generate を対象クラスで呼び出す
       * @then result に description を含まず、anyOf を持つ
       */
      @Test
      void 説明の無い注釈を持つ条件付きスキーマの場合_説明を含まないスキーマを返す() {
        Map<String, Object> result =
            property(SchemaGenerator.generate(CondWithoutDescription.class), "result");

        assertThat(result).doesNotContainKey("description").containsKey("anyOf");
      }
    }
  }

  @Nested
  class generateFieldSchema_Field {

    @Nested
    class 正常系 {

      /**
       * フィールドを指定した場合に、そのフィールドのスキーマを返すことを確かめる。 クラス全体を作らずに項目単体の定義を取り出せるようにすることを守る。
       *
       * @given 説明付きの文字列フィールド text がある
       * @when generateFieldSchema をそのフィールドで呼び出す
       * @then type が string で、説明を含むスキーマを返す
       */
      @Test
      void フィールドを指定した場合_そのフィールドのスキーマを返す() throws NoSuchFieldException {
        Field field = Basic.class.getDeclaredField("text");

        assertThat(SchemaGenerator.generateFieldSchema(field))
            .containsEntry("type", "string")
            .containsEntry("description", "本文");
      }

      /**
       * 除外指定のフィールドを指定した場合に、除外せずにスキーマを返すことを確かめる。 除外はクラス全体のスキーマからだけ行い、項目単体の定義は残すことを守る。
       *
       * @given SchemaIgnore を付けた ignored フィールドがある
       * @when generateFieldSchema をそのフィールドで呼び出す
       * @then type が string のスキーマを返す
       */
      @Test
      void 除外指定のフィールドを指定した場合_除外せずにスキーマを返す() throws NoSuchFieldException {
        Field field = Tagged.class.getDeclaredField("ignored");

        assertThat(SchemaGenerator.generateFieldSchema(field)).containsEntry("type", "string");
      }
    }
  }

  @Nested
  class resolveFieldName {

    @Nested
    class 正常系 {

      /**
       * JsonProperty の指定がある場合に、指定した名前を返すことを確かめる。 応答の項目名は指定した名前と一致させる必要があることを守る。
       *
       * @given JsonProperty で custom_name と指定したフィールド named がある
       * @when resolveFieldName をそのフィールドで呼び出す
       * @then custom_name を返す
       */
      @Test
      void JsonPropertyの指定がある場合_指定した名前を返す() throws NoSuchFieldException {
        assertThat(SchemaGenerator.resolveFieldName(Tagged.class.getDeclaredField("named")))
            .isEqualTo("custom_name");
      }

      /**
       * キャメルケースのフィールドを指定した場合に、スネークケースの名前を返すことを確かめる。 Gemini の応答のプロパティ名は、スネークケースで読み込むことを守る。
       *
       * @given キャメルケースの camelCase フィールドがある
       * @when resolveFieldName をそのフィールドで呼び出す
       * @then camel_case を返す
       */
      @Test
      void キャメルケースの場合_スネークケースの名前を返す() throws NoSuchFieldException {
        assertThat(SchemaGenerator.resolveFieldName(Tagged.class.getDeclaredField("camelCase")))
            .isEqualTo("camel_case");
      }

      /**
       * 連続する大文字を含む場合に、大文字の塊の前だけに区切りを入れることを確かめる。 ID のような略語を1文字ずつ区切ると、Jackson の命名と食い違うことを守る。
       *
       * @given userID という名前のフィールドがある
       * @when resolveFieldName をそのフィールドで呼び出す
       * @then user_id を返す
       */
      @Test
      void 連続する大文字を含む場合_大文字の塊の前だけに区切りを入れた名前を返す() throws NoSuchFieldException {
        assertThat(SchemaGenerator.resolveFieldName(Tagged.class.getDeclaredField("userID")))
            .isEqualTo("user_id");
      }

      /**
       * 先頭が大文字の場合に、先頭に区切りを入れないことを確かめる。 先頭の区切りは余分な _ となり、プロパティ名が一致しなくなることを守る。
       *
       * @given 先頭が大文字の Name フィールドがある
       * @when resolveFieldName をそのフィールドで呼び出す
       * @then name を返す
       */
      @Test
      void 先頭が大文字の場合_先頭に区切りを入れない名前を返す() throws NoSuchFieldException {
        assertThat(SchemaGenerator.resolveFieldName(Tagged.class.getDeclaredField("Name")))
            .isEqualTo("name");
      }

      /**
       * アンダースコアの直後に大文字がある場合に、区切りを重ねないことを確かめる。 既に区切りがあるところに二重の区切りを入れると、名前が一致しなくなることを守る。
       *
       * @given a_B という名前のフィールドがある
       * @when resolveFieldName をそのフィールドで呼び出す
       * @then a_b を返す
       */
      @Test
      void アンダースコアの直後に大文字がある場合_区切りを重ねない名前を返す() throws NoSuchFieldException {
        assertThat(SchemaGenerator.resolveFieldName(Tagged.class.getDeclaredField("a_B")))
            .isEqualTo("a_b");
      }
    }

    @Nested
    class 境界値 {

      /**
       * JsonProperty の値が空の場合に、フィールド名から変換した名前を返すことを確かめる。 値が空の指定は無いものとして扱うことを守る。
       *
       * @given JsonProperty の値が空の empty フィールドがある
       * @when resolveFieldName をそのフィールドで呼び出す
       * @then フィールド名をスネークケースにした名前を返す
       */
      @Test
      void JsonPropertyの値が空の場合_フィールド名から変換した名前を返す() throws NoSuchFieldException {
        assertThat(SchemaGenerator.resolveFieldName(Tagged.class.getDeclaredField("emptyName")))
            .isEqualTo("empty_name");
      }
    }
  }

  @Nested
  class resolveAllFields {

    @Nested
    class 正常系 {

      /**
       * 親クラスがある場合に、親のフィールドを先に並べて返すことを確かめる。 スキーマのプロパティの並びを親から子の順に揃えることを守る。
       *
       * @given 親クラス Parent の parentField と、子クラス Child の childField がある
       * @when resolveAllFields を子クラスで呼び出す
       * @then parentField、childField の順のフィールドを返す
       */
      @Test
      void 親クラスがある場合_親のフィールドを先に並べたリストを返す() {
        List<String> names =
            SchemaGenerator.resolveAllFields(Child.class).stream().map(Field::getName).toList();

        assertThat(names).containsExactly("parentField", "childField");
      }
    }
  }

  @Nested
  class toYaml {

    @Nested
    class 正常系 {

      /**
       * マップを指定した場合に、YAML の文字列を返すことを確かめる。 OpenAPI の文書を YAML に変換して出力するため、その形式を守る。
       *
       * @given キー type と値 object を持つマップがある
       * @when toYaml を呼び出す
       * @then type: object の行を含む YAML 文字列を返す
       */
      @Test
      void マップを指定した場合_YAML文字列を返す() {
        assertThat(SchemaGenerator.toYaml(Map.of("type", "object")))
            .contains("type:")
            .contains("object");
      }
    }

    @Nested
    class 異常系 {

      /**
       * YAML に変換できない値を含む場合に、例外を投げることを確かめる。 変換できない値を黙って落とすと、出力された文書が欠けることを守る。
       *
       * @given 値に変換できないオブジェクトを持つマップがある
       * @when toYaml を呼び出す
       * @then RuntimeException を投げる
       */
      @Test
      void 変換できない値を含む場合_RuntimeExceptionを投げる() {
        assertThatThrownBy(() -> SchemaGenerator.toYaml(Map.of("x", new Object())))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to format schema to YAML");
      }
    }
  }

  /**
   * プロパティ一覧を取り出す.
   *
   * @param schema スキーマ
   * @return プロパティ名と定義の対応
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> properties(final Map<String, Object> schema) {
    return (Map<String, Object>) schema.get("properties");
  }

  /**
   * 指定した項目の定義を取り出す.
   *
   * @param schema スキーマ
   * @param name 項目名
   * @return 項目の定義
   */
  private static Map<String, Object> property(final Map<String, Object> schema, final String name) {
    return castMap(properties(schema).get(name));
  }

  /**
   * 値をマップとして扱う.
   *
   * @param value 値
   * @return マップ
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(final Object value) {
    return (Map<String, Object>) value;
  }

  /**
   * 現在時刻の取得で必ず例外を投げる時計を作る.
   *
   * @return 例外を投げる時計
   */
  private static Clock failingClock() {
    return new Clock() {
      @Override
      public ZoneId getZone() {
        return ZoneId.of("Asia/Tokyo");
      }

      @Override
      public Clock withZone(final ZoneId zone) {
        return this;
      }

      @Override
      public Instant instant() {
        throw new IllegalStateException("時計の障害");
      }
    };
  }

  /** 配列、列挙、入れ子、必須の注釈を持つ入力。 */
  static class Annotated {
    private String[] codes;

    @Schema(minItems = 1)
    private List<String> ids;

    @Schema(itemType = Empty.class)
    private List<Empty> empties;

    @Schema(description = "色")
    private Color favorite;

    @Schema(description = "子")
    private Children withSchema;

    @Schema(required = true)
    private String plain;
  }

  /** 静的フィールドを持つ入れ子の入力。 */
  static class WithStatic {
    private static String shared;
    private String value;
  }

  /** 静的フィールドを持つ入力を入れ子に持つ入力。 */
  static class NestedStatic {
    private WithStatic inner;
  }

  /** 全てのケースが除外指定の入力。 */
  static class ExcludedInner {
    @ConditionalSchema({@ConditionalCase(enumValue = "A", schema = Ok.class, excludeOn = "A")})
    private Object result;
  }

  /** 除外指定の条件付きスキーマを入れ子に持つ入力。 */
  static class NestedExcluded {
    private ExcludedInner inner;
  }

  /** プロパティを持たないケースの入力。 */
  static class EmptyCase {
    @ConditionalSchema({@ConditionalCase(enumValue = "A", schema = Empty.class)})
    private Object result;
  }

  /** 静的フィールドを持つケースの入力。 */
  static class StaticCase {
    @ConditionalSchema({@ConditionalCase(enumValue = "A", schema = WithStatic.class)})
    private Object result;
  }

  /** 必須ではない注釈を持つフィールドの入力。 */
  static class Note {
    @Schema(description = "メモ")
    private String memo;
  }

  /** 必須ではない注釈を持つケースの入力。 */
  static class NoteCase {
    @ConditionalSchema({@ConditionalCase(enumValue = "A", schema = Note.class)})
    private Object result;
  }

  /** 説明の無い注釈を持つ条件付きスキーマの入力。 */
  static class CondWithoutDescription {
    @Schema(title = "題")
    @ConditionalSchema({@ConditionalCase(enumValue = "A", schema = Ok.class)})
    private Object result;
  }

  /** 単純な型の入力。 */
  static class Basic {
    @Schema(description = "本文", required = true)
    private String text;

    private int count;

    private boolean flag;
  }

  /** 数値の型を全て持つ入力。 */
  static class NumericTypes {
    private int count;
    private Integer boxedCount;
    private long big;
    private Long boxedBig;
    private double ratio;
    private Double boxedRatio;
    private float score;
    private Float boxedScore;
    private boolean flag;
    private Boolean boxedFlag;
  }

  /** 親クラス。 */
  static class Parent {
    private String parentField;
  }

  /** 親を継承する子クラス。 */
  static class Child extends Parent {
    private String childField;
  }

  /** 列挙型。 */
  enum Color {
    RED,
    BLUE
  }

  /** 入れ子の入力。 */
  static class Children {
    @Schema(required = true)
    private String childText;
  }

  /** フィールドを持たない入力。 */
  static class Empty {}

  /** 配列と入れ子と列挙型を持つ入力。 */
  static class Holder {
    private Color color;

    @Schema(itemType = Color.class)
    private List<Color> colors;

    private List<String> tags;

    @Schema(itemType = Children.class, minItems = 1, maxItems = 5)
    private List<Children> items;

    @Schema(itemType = Integer.class)
    private List<Integer> numbers;

    @Schema(itemType = String[].class)
    private List<String[]> arrays;

    @Schema(itemType = List.class)
    private List<List<String>> lists;

    private Children child;

    private Empty empty;

    private BigDecimal amount;
  }

  /** 名前の変換と除外と静的の扱いを確かめる入力。 */
  static class Tagged {
    @JsonProperty("custom_name")
    private String named;

    private String camelCase;

    private String userID;

    private String Name;

    private String a_B;

    @JsonProperty("")
    private String emptyName;

    @SchemaIgnore private String ignored;

    private static String staticField;

    private transient String transientField;

    private String $dollar;
  }

  /** 文字数、範囲、型の制約を持つ入力。 */
  static class Constraints {
    @Schema(
        description = "名前",
        minLength = 1,
        maxLength = 10,
        pattern = "^a",
        format = "date",
        title = "題",
        defaultValue = "x",
        example = "ex",
        xDataSource = "ds")
    private String text;

    @Schema(gt = 0, lt = 100, minDigits = 1, maxDigits = 3)
    private int number;

    @Schema(type = "number")
    private int typed;
  }

  /** 必須の指定が無い入力。 */
  static class OptionalOnly {
    private String name;
  }

  /** 年の制約を持つ入力。 */
  static class YearConstraints {
    @Schema(currentAndNextYearOnly = true)
    private String year;

    @Schema(currentAndNextYearOnly = true, pattern = "^\\d{4}/\\d{1,2}$")
    private String yearPattern;

    @Schema(currentAndNextYearOnly = true, pattern = "^[0-9]{4}/x$")
    private String bracketPattern;

    @Schema(currentAndNextYearOnly = true, description = "開始年月。")
    private String described;
  }

  /** 条件付きスキーマの成功のケースで使う入力。 */
  static class Ok {
    @Schema(required = true)
    private String data;
  }

  /** 条件付きスキーマの失敗のケースで使う入力。 */
  static class Ng {
    private String message;
  }

  /** 条件付きスキーマを持つ入力。 */
  static class Conditional {
    @ConditionalSchema({
      @ConditionalCase(enumValue = "OK", schema = Ok.class, title = "成功", description = "成功時"),
      @ConditionalCase(enumValue = "NG", schema = Ng.class)
    })
    @Schema(description = "結果")
    private Object result;
  }

  /** 全てのケースが除外指定の入力。 */
  static class AllExcluded {
    @ConditionalSchema({@ConditionalCase(enumValue = "A", schema = Ok.class, excludeOn = "A")})
    private Object result;
  }

  /** 除外の値が一部のケースだけにある入力。 */
  static class ExcludeSome {
    @ConditionalSchema({
      @ConditionalCase(enumValue = "A", schema = Ok.class, excludeOn = "A"),
      @ConditionalCase(enumValue = "B", schema = Ng.class)
    })
    private Object result;
  }

  /** 条件付きスキーマのケースが無い入力。 */
  static class NoCases {
    @ConditionalSchema({})
    private Object result;
  }
}
