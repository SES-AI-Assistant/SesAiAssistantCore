package copel.sesproductpackage.core.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link WhereClauseAndParams} の単体テスト。 */
class WhereClauseAndParamsTest {

  @Nested
  class getWhereClauseWithoutWhereKeyword {

    @Nested
    class 正常系 {

      /**
       * 条件式をWHERE キーワードなしで返すことを確かめる。 Lot は返された条件式を SQL の末尾へそのまま連結する仕様である。
       *
       * @given 条件式 "name = ?" と値 "A" で生成している
       * @when getWhereClauseWithoutWhereKeyword を実行する
       * @then "name = ?" を返す
       */
      @Test
      void 条件式を持つ場合_条件式を返す() {
        WhereClauseAndParams target = new WhereClauseAndParams("name = ?", List.of("A"));

        assertThat(target.getWhereClauseWithoutWhereKeyword()).isEqualTo("name = ?");
      }
    }
  }

  @Nested
  class getParams {

    @Nested
    class 正常系 {

      /**
       * 値を渡した順番のまま返すことを確かめる。 プレースホルダの順番と値の順番が一致しないと、検索結果が別の条件で絞られるためである。
       *
       * @given 値 "A" と 1 を渡して生成している
       * @when getParams を実行する
       * @then "A" と 1 を順番どおりに返す
       */
      @Test
      void 値を2件持つ場合_値の並びを返す() {
        WhereClauseAndParams target = new WhereClauseAndParams("a = ? AND b = ?", List.of("A", 1));

        assertThat(target.getParams()).containsExactly("A", 1);
      }

      /**
       * 生成後に元のリストを変えても、保持する値が変わらないことを確かめる。 検索条件を組み立てた後に値が書き換わると、別の条件で検索されるためである。
       *
       * @given 値 "A" を持つ可変リストで生成し、その後に "B" を追加している
       * @when getParams を実行する
       * @then "A" だけを返す
       */
      @Test
      void 元のリストを後から追加した場合_生成時の値だけを返す() {
        List<Object> source = new ArrayList<>(List.of("A"));
        WhereClauseAndParams target = new WhereClauseAndParams("a = ?", source);
        source.add("B");

        assertThat(target.getParams()).containsExactly("A");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 値が無い場合に空のリストを返すことを確かめる。 プレースホルダの無い条件式でも呼び出し側が null を扱わずに済むようにするためである。
       *
       * @given 値が0件の空のリストで生成している
       * @when getParams を実行する
       * @then 空のリストを返す
       */
      @Test
      void 値が0件の場合_空のリストを返す() {
        WhereClauseAndParams target = new WhereClauseAndParams("", List.of());

        assertThat(target.getParams()).isEmpty();
      }
    }
  }

  @Nested
  class isEmpty {

    @Nested
    class 正常系 {

      /**
       * 条件式がある場合に空でないと判定することを確かめる。 条件がある検索では WHERE 句を付けて絞り込むためである。
       *
       * @given 条件式 "a = ?" で生成している
       * @when isEmpty を実行する
       * @then false を返す
       */
      @Test
      void 条件式がある場合_falseを返す() {
        assertThat(new WhereClauseAndParams("a = ?", List.of("A")).isEmpty()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 条件式が空文字の場合に空だと判定することを確かめる。 条件が無い検索条件では WHERE 句を付けないため、空の判定が必要である。
       *
       * @given 条件式が空文字で生成している
       * @when isEmpty を実行する
       * @then true を返す
       */
      @Test
      void 条件式が空文字の場合_trueを返す() {
        assertThat(new WhereClauseAndParams("", List.of()).isEmpty()).isTrue();
      }
    }
  }
}
