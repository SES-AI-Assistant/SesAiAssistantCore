package copel.sesproductpackage.core.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link JobDetailFilterCondition} の単体テスト。 */
class JobDetailFilterConditionTest {

  /** 開始月の検索に使う日付。 */
  private static final OriginalDateTime START_DATE = new OriginalDateTime(2026, 1, 1, 0, 0, 0);

  @Nested
  class buildWhereClause_String {

    @Nested
    class 正常系 {

      /**
       * 開始月とエリアを指定した場合に、両方の条件を AND で連結することを確かめる。 案件一覧の絞り込みは複数の条件を同時に満たす案件だけを返すためである。
       *
       * @given 開始月が 2026年1月1日、エリアが 関東_首都圏
       * @when buildWhereClause を "j" で実行する
       * @then "j.start_date >= ? AND j.area = ?" を返し、開始月とエリアを値として持つ
       */
      @Test
      void 開始月とエリアを指定する場合_両方の条件を連結した条件式を返す() {
        JobDetailFilterCondition condition = new JobDetailFilterCondition();
        condition.setStartDate(START_DATE);
        condition.setArea(Area.関東_首都圏);

        WhereClauseAndParams result = condition.buildWhereClause("j");

        assertThat(result.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("j.start_date >= ? AND j.area = ?");
        assertThat(result.getParams()).containsExactly(START_DATE, Area.関東_首都圏);
      }

      /**
       * 全ての条件を指定した場合に、すべての条件を定義の順に連結することを確かめる。 どの条件も欠けると、画面で選んだ絞り込みと異なる案件が返るためである。
       *
       * @given 開始月、エリア、出社要件が 3、最小単価が 800000円、最大単価が 1200000円
       * @when buildWhereClause を "j" で実行する
       * @then 5件の条件を AND で連結した条件式を返し、値を条件の順に5件返す
       */
      @Test
      void 全ての条件を指定する場合_5件の条件を連結した条件式を返す() {
        JobDetailFilterCondition condition = new JobDetailFilterCondition();
        condition.setStartDate(START_DATE);
        condition.setArea(Area.関東_首都圏);
        condition.setOfficeRequirements(3);
        condition.setMinPrice(new Money(800_000L));
        condition.setMaxPrice(new Money(1_200_000L));

        WhereClauseAndParams result = condition.buildWhereClause("j");

        assertThat(result.getWhereClauseWithoutWhereKeyword())
            .isEqualTo(
                "j.start_date >= ? AND j.area = ? AND j.office_requirements <= ?"
                    + " AND j.unit_price >= ? AND j.unit_price <= ?");
        assertThat(result.getParams())
            .containsExactly(
                START_DATE, Area.関東_首都圏, 3, new BigDecimal(800_000L), new BigDecimal(1_200_000L));
      }

      /**
       * 単価の上限と下限だけを指定した場合に、unit_price の範囲条件を返すことを確かめる。 単価は範囲で絞り込むため、下限と上限の両方を条件に含める必要がある。
       *
       * @given 最小単価が 800000円、最大単価が 1200000円
       * @when buildWhereClause を null で実行する
       * @then "unit_price >= ? AND unit_price <= ?" を返し、単価の値を2件返す
       */
      @Test
      void 単価の上限と下限を指定する場合_unit_priceの範囲条件を返す() {
        JobDetailFilterCondition condition = new JobDetailFilterCondition();
        condition.setMinPrice(new Money(800_000L));
        condition.setMaxPrice(new Money(1_200_000L));

        WhereClauseAndParams result = condition.buildWhereClause(null);

        assertThat(result.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("unit_price >= ? AND unit_price <= ?");
        assertThat(result.getParams())
            .containsExactly(new BigDecimal(800_000L), new BigDecimal(1_200_000L));
      }
    }

    @Nested
    class 異常系 {

      /**
       * テーブルエイリアスに空白を含む場合に例外を投げることを確かめる。 エイリアスは SQL に直接連結されるため、識別子でない値を拒否する。
       *
       * @given 開始月だけを指定し、テーブルエイリアスが "j x"
       * @when buildWhereClause を "j x" で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void エイリアスに空白を含む場合_IllegalArgumentExceptionを投げる() {
        JobDetailFilterCondition condition = new JobDetailFilterCondition();
        condition.setStartDate(START_DATE);

        assertThatThrownBy(() -> condition.buildWhereClause("j x"))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 出社要件が 0 の場合に、0 以下の案件を対象にする条件を返すことを確かめる。 フルリモートを選んだユーザーに出社を伴う案件を出さないため、0 を含めて判定する。
       *
       * @given 出社要件が 0
       * @when buildWhereClause を "j" で実行する
       * @then "j.office_requirements <= ?" を返し、値として 0 を持つ
       */
      @Test
      void 出社要件が0の場合_出社要件以下の条件を返す() {
        JobDetailFilterCondition condition = new JobDetailFilterCondition();
        condition.setOfficeRequirements(0);

        WhereClauseAndParams result = condition.buildWhereClause("j");

        assertThat(result.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("j.office_requirements <= ?");
        assertThat(result.getParams()).containsExactly(0);
      }

      /**
       * 単価が値を持たない Money の場合に、単価の条件を含めないことを確かめる。 値が null のまま SQL のパラメータに渡すと組み立てに失敗するためである。
       *
       * @given 最小単価と最大単価がどちらも値を持たない Money
       * @when buildWhereClause を null で実行する
       * @then 空の条件式を返す
       */
      @Test
      void 単価が値を持たない場合_空の条件式を返す() {
        JobDetailFilterCondition condition = new JobDetailFilterCondition();
        condition.setMinPrice(Money.empty());
        condition.setMaxPrice(Money.empty());

        WhereClauseAndParams result = condition.buildWhereClause(null);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getParams()).isEmpty();
      }

      /**
       * 条件を1つも指定しない場合に、空の条件式を返すことを確かめる。 絞り込みなしの検索では WHERE 句を付けないためである。
       *
       * @given すべての条件が null
       * @when buildWhereClause を null で実行する
       * @then 空の条件式を返す
       */
      @Test
      void 条件を1つも指定しない場合_空の条件式を返す() {
        WhereClauseAndParams result = new JobDetailFilterCondition().buildWhereClause(null);

        assertThat(result.isEmpty()).isTrue();
      }

      /**
       * テーブルエイリアスが空文字の場合に、列名に前置をしないことを確かめる。 エイリアスの無い SQL にドットだけが付くと構文が壊れるためである。
       *
       * @given 開始月だけを指定し、テーブルエイリアスが空文字
       * @when buildWhereClause を空文字で実行する
       * @then "start_date >= ?" を返す
       */
      @Test
      void エイリアスが空文字の場合_前置のない条件式を返す() {
        JobDetailFilterCondition condition = new JobDetailFilterCondition();
        condition.setStartDate(START_DATE);

        assertThat(condition.buildWhereClause("").getWhereClauseWithoutWhereKeyword())
            .isEqualTo("start_date >= ?");
      }
    }
  }

  @Nested
  class buildWhereClause {

    @Nested
    class 正常系 {

      /**
       * 引数を指定しない場合に、列名に前置をしない条件式を返すことを確かめる。 テーブルを1つだけ使う検索で、列名をそのまま使うためである。
       *
       * @given 開始月が 2026年1月1日
       * @when 引数なしの buildWhereClause を実行する
       * @then "start_date >= ?" を返し、開始月を値として持つ
       */
      @Test
      void 開始月を指定する場合_列名に前置をしない条件式を返す() {
        JobDetailFilterCondition condition = new JobDetailFilterCondition();
        condition.setStartDate(START_DATE);

        WhereClauseAndParams result = condition.buildWhereClause();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("start_date >= ?");
        assertThat(result.getParams()).containsExactly(START_DATE);
      }
    }
  }
}
