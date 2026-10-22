package copel.sesproductpackage.core.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link WhereClauseBuilder} の単体テスト。 */
class WhereClauseBuilderTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 異常系 {

      /**
       * テーブルエイリアスが数字で始まる場合に例外を投げることを確かめる。 エイリアスは SQL に直接連結されるため、識別子の規則を外れる値を拒否する。
       *
       * @given テーブルエイリアスが "1a"
       * @when WhereClauseBuilder をテーブルエイリアス "1a" で生成する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void エイリアスが数字で始まる場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new WhereClauseBuilder("1a"))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テーブルエイリアスに記号を含む場合に例外を投げることを確かめる。 記号を含む値は SQL の組み立てを壊すため、連結させない。
       *
       * @given テーブルエイリアスが "p;DROP"
       * @when WhereClauseBuilder をテーブルエイリアス "p;DROP" で生成する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void エイリアスに記号を含む場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new WhereClauseBuilder("p;DROP"))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class column {

    @Nested
    class 正常系 {

      /**
       * エイリアスを指定した場合に列名の前へエイリアスとドットを付けることを確かめる。 結合したテーブルの列を、別のテーブルの列と取り違えないようにするためである。
       *
       * @given テーブルエイリアスが "p"
       * @when 列名 "start_date" を column で取得する
       * @then "p.start_date" を返す
       */
      @Test
      void エイリアスがpの場合_p_start_dateを返す() {
        assertThat(new WhereClauseBuilder("p").column("start_date")).isEqualTo("p.start_date");
      }
    }

    @Nested
    class 境界値 {

      /**
       * エイリアスが null の場合に列名をそのまま返すことを確かめる。 単一テーブルの検索では列名だけで条件を書くためである。
       *
       * @given テーブルエイリアスが null
       * @when 列名 "start_date" を column で取得する
       * @then "start_date" を返す
       */
      @Test
      void エイリアスがnullの場合_列名を返す() {
        assertThat(new WhereClauseBuilder(null).column("start_date")).isEqualTo("start_date");
      }

      /**
       * エイリアスが空文字の場合に列名をそのまま返すことを確かめる。 空のエイリアスでドットだけを前置すると SQL が壊れるためである。
       *
       * @given テーブルエイリアスが空文字
       * @when 列名 "start_date" を column で取得する
       * @then "start_date" を返す
       */
      @Test
      void エイリアスが空文字の場合_列名を返す() {
        assertThat(new WhereClauseBuilder("").column("start_date")).isEqualTo("start_date");
      }
    }
  }

  @Nested
  class and {

    @Nested
    class 正常系 {

      /**
       * 条件を追加するたびに同じビルダーを返すことを確かめる。 呼び出し側が条件を続けて連結できるようにするためである。
       *
       * @given テーブルエイリアスが null のビルダー
       * @when 条件 "a = ?" を値 "A" で追加する
       * @then 追加に使ったビルダー自身を返す
       */
      @Test
      void 条件を追加する場合_同じビルダーを返す() {
        WhereClauseBuilder builder = new WhereClauseBuilder(null);

        assertThat(builder.and("a = ?", "A")).isSameAs(builder);
      }

      /**
       * 2件の条件を AND で連結することを確かめる。 複数の検索条件はすべて満たす行だけを対象にするためである。
       *
       * @given テーブルエイリアスが "j" のビルダーに条件 "j.a = ?" と "j.b = ?" を追加している
       * @when build を実行する
       * @then "j.a = ? AND j.b = ?" を返す
       */
      @Test
      void 条件を2件追加する場合_ANDで連結した条件式を返す() {
        WhereClauseBuilder builder = new WhereClauseBuilder("j");
        builder.and(builder.column("a") + " = ?", "A").and(builder.column("b") + " = ?", "B");

        assertThat(builder.build().getWhereClauseWithoutWhereKeyword())
            .isEqualTo("j.a = ? AND j.b = ?");
      }
    }
  }

  @Nested
  class build {

    @Nested
    class 正常系 {

      /**
       * 追加した条件の値を、追加した順番で返すことを確かめる。 プレースホルダと値の順番が一致しないと、条件が取り違えられるためである。
       *
       * @given テーブルエイリアスが null のビルダーに値 "A" と 2 を持つ条件を順に追加している
       * @when build を実行する
       * @then "A" と 2 を順番どおりに返す
       */
      @Test
      void 値を持つ条件を2件追加する場合_値の並びを返す() {
        WhereClauseBuilder builder = new WhereClauseBuilder(null);
        builder.and("a = ?", "A").and("b >= ?", 2);

        assertThat(builder.build().getParams()).containsExactly("A", 2);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 条件を1件も追加しない場合に空の条件式を返すことを確かめる。 条件が無い検索では WHERE 句を付けないため、空だと判定できる必要がある。
       *
       * @given テーブルエイリアスが null で条件を追加していないビルダー
       * @when build を実行する
       * @then 空の条件式と空の値を返す
       */
      @Test
      void 条件を追加しない場合_空の条件式を返す() {
        WhereClauseAndParams result = new WhereClauseBuilder(null).build();

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getParams()).isEmpty();
      }
    }
  }
}
