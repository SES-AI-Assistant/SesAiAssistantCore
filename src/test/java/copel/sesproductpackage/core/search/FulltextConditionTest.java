package copel.sesproductpackage.core.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link FulltextCondition} の単体テスト。 */
class FulltextConditionTest {

  @Nested
  class getOperator {

    @Nested
    class 正常系 {

      /**
       * 指定した演算子をそのまま返すことを確かめる。 区間の分け方は演算子で決まるため、保持した値を取り違えないようにする。
       *
       * @given 演算子が "OR"、キーワードが "Python"、否定でない
       * @when getOperator を実行する
       * @then "OR" を返す
       */
      @Test
      void 演算子がORの場合_ORを返す() {
        assertThat(new FulltextCondition("OR", "Python", false).getOperator()).isEqualTo("OR");
      }
    }
  }

  @Nested
  class getKeyword {

    @Nested
    class 正常系 {

      /**
       * 指定したキーワードをそのまま返すことを確かめる。 検索語を別の値に変えると、画面で入力した語と異なる検索になるためである。
       *
       * @given 演算子が "AND"、キーワードが "Python"、否定でない
       * @when getKeyword を実行する
       * @then "Python" を返す
       */
      @Test
      void キーワードがPythonの場合_Pythonを返す() {
        assertThat(new FulltextCondition("AND", "Python", false).getKeyword()).isEqualTo("Python");
      }
    }
  }

  @Nested
  class isNegated {

    @Nested
    class 正常系 {

      /**
       * 否定を指定した場合に true を返すことを確かめる。 含まない条件は NOT LIKE の組み立てに使うため、否定の判定が必要である。
       *
       * @given 演算子が "AND"、キーワードが "Python"、否定
       * @when isNegated を実行する
       * @then true を返す
       */
      @Test
      void 否定を指定した場合_trueを返す() {
        assertThat(new FulltextCondition("AND", "Python", true).isNegated()).isTrue();
      }

      /**
       * 否定を指定しない場合に false を返すことを確かめる。 含む条件は LIKE の組み立てに使うため、否定でないことを判定する必要がある。
       *
       * @given 演算子が "AND"、キーワードが "Python"、否定でない
       * @when isNegated を実行する
       * @then false を返す
       */
      @Test
      void 否定を指定しない場合_falseを返す() {
        assertThat(new FulltextCondition("AND", "Python", false).isNegated()).isFalse();
      }
    }
  }

  @Nested
  class hasSearchableKeyword {

    @Nested
    class 正常系 {

      /**
       * 前後に空白を含むキーワードを検索可能と判定することを確かめる。 トリムすると検索語が残るため、入力の前後の空白で検索対象外にしないようにする。
       *
       * @given キーワードが " Python "
       * @when hasSearchableKeyword を実行する
       * @then true を返す
       */
      @Test
      void キーワードが前後に空白を含む場合_trueを返す() {
        assertThat(new FulltextCondition("AND", "  Python  ", false).hasSearchableKeyword())
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * キーワードが空白だけの場合に検索可能でないと判定することを確かめる。 空の検索語で LIKE の条件を作ると、すべての行に一致してしまうためである。
       *
       * @given キーワードが " "
       * @when hasSearchableKeyword を実行する
       * @then false を返す
       */
      @Test
      void キーワードが空白だけの場合_falseを返す() {
        assertThat(new FulltextCondition("AND", "   ", false).hasSearchableKeyword()).isFalse();
      }

      /**
       * キーワードが空文字の場合に検索可能でないと判定することを確かめる。 空の検索語で条件を作ると、すべての行に一致してしまうためである。
       *
       * @given キーワードが空文字
       * @when hasSearchableKeyword を実行する
       * @then false を返す
       */
      @Test
      void キーワードが空文字の場合_falseを返す() {
        assertThat(new FulltextCondition("AND", "", false).hasSearchableKeyword()).isFalse();
      }

      /**
       * キーワードが null の場合に検索可能でないと判定することを確かめる。 キーワードの無い条件を検索に使わないようにするためである。
       *
       * @given キーワードが null
       * @when hasSearchableKeyword を実行する
       * @then false を返す
       */
      @Test
      void キーワードがnullの場合_falseを返す() {
        assertThat(new FulltextCondition("AND", null, false).hasSearchableKeyword()).isFalse();
      }
    }
  }

  @Nested
  class equals {

    @Nested
    class 正常系 {

      /**
       * 同じインスタンスと比べた場合に等しいと判定することを確かめる。 自分自身との比較は常に等しい必要があるためである。
       *
       * @given 演算子が "AND"、キーワードが "Python"、否定でない条件
       * @when 同じインスタンスと equals で比べる
       * @then true を返す
       */
      @Test
      void 自分自身の場合_trueを返す() {
        FulltextCondition condition = new FulltextCondition("AND", "Python", false);

        assertThat(condition.equals(condition)).isTrue();
      }

      /**
       * 値が同じ別のインスタンスは等しいと判定することを確かめる。 画面から送られた同じ条件を、別々の処理で比べられるようにするためである。
       *
       * @given 演算子が "AND"、キーワードが "Python"、否定でない条件を2つ作る
       * @when 一方を他方と equals で比べる
       * @then true を返す
       */
      @Test
      void 同じ値の別インスタンスの場合_trueを返す() {
        FulltextCondition left = new FulltextCondition("AND", "Python", false);
        FulltextCondition right = new FulltextCondition("AND", "Python", false);

        assertThat(left.equals(right)).isTrue();
      }

      /**
       * 否定フラグだけが違う場合に等しくないと判定することを確かめる。 含むと含まないは検索結果が反対になるため、別の条件として扱う。
       *
       * @given 演算子が "AND"、キーワードが "Python" で、一方は否定、他方は否定でない
       * @when 両者を equals で比べる
       * @then false を返す
       */
      @Test
      void 否定フラグだけが違う場合_falseを返す() {
        FulltextCondition positive = new FulltextCondition("AND", "Python", false);
        FulltextCondition negative = new FulltextCondition("AND", "Python", true);

        assertThat(positive.equals(negative)).isFalse();
      }

      /**
       * 演算子だけが違う場合に等しくないと判定することを確かめる。 OR と AND では区間の分け方が変わるため、別の条件として扱う。
       *
       * @given キーワードが "Python" で否定でない条件を、演算子 "AND" と "OR" で作る
       * @when 両者を equals で比べる
       * @then false を返す
       */
      @Test
      void 演算子だけが違う場合_falseを返す() {
        FulltextCondition and = new FulltextCondition("AND", "Python", false);
        FulltextCondition or = new FulltextCondition("OR", "Python", false);

        assertThat(and.equals(or)).isFalse();
      }

      /**
       * キーワードだけが違う場合に等しくないと判定することを確かめる。 検索語が違えば検索結果も違うためである。
       *
       * @given 演算子が "AND" で否定でない条件を、キーワード "Python" と "Java" で作る
       * @when 両者を equals で比べる
       * @then false を返す
       */
      @Test
      void キーワードだけが違う場合_falseを返す() {
        FulltextCondition python = new FulltextCondition("AND", "Python", false);
        FulltextCondition java = new FulltextCondition("AND", "Java", false);

        assertThat(python.equals(java)).isFalse();
      }
    }

    @Nested
    class 異常系 {

      /**
       * null と比べた場合に等しくないと判定することを確かめる。 null との比較で例外を投げないようにするためである。
       *
       * @given 演算子が "AND"、キーワードが "Python"、否定でない条件
       * @when null と equals で比べる
       * @then false を返す
       */
      @Test
      void nullの場合_falseを返す() {
        assertThat(new FulltextCondition("AND", "Python", false).equals(null)).isFalse();
      }

      /**
       * 別のクラスのインスタンスと比べた場合に等しくないと判定することを確かめる。 文字列のような別の型と誤って同じ値とみなさないようにするためである。
       *
       * @given 演算子が "AND"、キーワードが "Python"、否定でない条件
       * @when 文字列 "Python" と equals で比べる
       * @then false を返す
       */
      @Test
      void 別のクラスの場合_falseを返す() {
        assertThat(new FulltextCondition("AND", "Python", false).equals("Python")).isFalse();
      }
    }
  }

  @Nested
  class hashCode {

    @Nested
    class 正常系 {

      /**
       * 等しい条件同士が同じハッシュ値を返すことを確かめる。 ハッシュを使う集合で同じ条件を重複させないため、等しい値は同じハッシュ値である必要がある。
       *
       * @given 演算子が "AND"、キーワードが "Python"、否定でない条件を2つ作る
       * @when 両者の hashCode を取得する
       * @then 同じハッシュ値を返す
       */
      @Test
      void 同じ値の別インスタンスの場合_同じハッシュ値を返す() {
        FulltextCondition left = new FulltextCondition("AND", "Python", false);
        FulltextCondition right = new FulltextCondition("AND", "Python", false);

        assertThat(left.hashCode()).isEqualTo(right.hashCode());
      }
    }
  }
}
