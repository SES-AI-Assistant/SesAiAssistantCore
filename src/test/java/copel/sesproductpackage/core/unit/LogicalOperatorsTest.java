package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.unit.LogicalOperators.LogicalOperatorType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link LogicalOperators} の単体テスト。 */
class LogicalOperatorsTest {

  @Nested
  class getLikeQuery {

    @Nested
    class 正常系 {

      /**
       * 論理演算子とカラム名がある場合に LIKE 句を返すことを確かめる。 条件の演算子が無いと SQL の結合が崩れるので、条件が作れない場合は null にする。
       *
       * @given 論理演算子が AND、カラム名が "name"、値が "x"
       * @when getLikeQuery を実行する
       * @then " AND name LIKE ?" を返す
       */
      @Test
      void 論理演算子とカラム名を指定した場合_LIKE句を返す() {
        LogicalOperators operators = new LogicalOperators(LogicalOperatorType.AND, "name", "x");

        assertThat(operators.getLikeQuery()).isEqualTo(" AND name LIKE ?");
      }

      /**
       * カラム名を後から設定すると LIKE 句に反映されることを確かめる。 SQL の断片の形式が変わると検索条件が壊れるので、形式を固定する。
       *
       * @given 論理演算子が OR、カラム名が未設定、値が "x"
       * @when setColumnName を "col" で実行してから getLikeQuery を実行する
       * @then " OR col LIKE ?" を返す
       */
      @Test
      void カラム名を設定した場合_LIKE句を返す() {
        LogicalOperators operators = new LogicalOperators(LogicalOperatorType.OR, "x");
        operators.setColumnName("col");

        assertThat(operators.getLikeQuery()).isEqualTo(" OR col LIKE ?");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 論理演算子が未設定の場合に null を返すことを確かめる。 後から設定したカラム名が反映されないと検索条件の組み立てが不完全になるので、設定の反映を固定する。
       *
       * @given 論理演算子が null、カラム名が "name"
       * @when getLikeQuery を実行する
       * @then null を返す
       */
      @Test
      void 論理演算子が未設定の場合_nullを返す() {
        LogicalOperators operators = new LogicalOperators(null, "name", "x");

        assertThat(operators.getLikeQuery()).isNull();
      }

      /**
       * カラム名が未設定の場合に null を返すことを確かめる。 カラム名が無い条件を SQL に入れると構文が壊れるので、null を返す。
       *
       * @given 論理演算子が AND、カラム名が未設定
       * @when getLikeQuery を実行する
       * @then null を返す
       */
      @Test
      void カラム名が未設定の場合_nullを返す() {
        LogicalOperators operators = new LogicalOperators(LogicalOperatorType.AND, "x");

        assertThat(operators.getLikeQuery()).isNull();
      }
    }
  }

  @Nested
  class getValue {

    @Nested
    class 正常系 {

      /**
       * 指定した検索条件値を取得することを確かめる。 検索に使う値が入れ替わると別の結果を検索してしまうので、値の保持を固定する。
       *
       * @given 値が "x" で生成された
       * @when getValue を実行する
       * @then "x" を返す
       */
      @Test
      void 値を指定した場合_指定した値を返す() {
        LogicalOperators operators = new LogicalOperators(LogicalOperatorType.AND, "x");

        assertThat(operators.getValue()).isEqualTo("x");
      }
    }
  }
}
