package copel.sesproductpackage.core.search;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link DetailFilterConditionBase} の単体テスト。 */
class DetailFilterConditionBaseTest {

  /** 開始月の検索に使う日付。 */
  private static final OriginalDateTime START_DATE = new OriginalDateTime(2026, 1, 1, 0, 0, 0);

  /** 検証用の具象サブクラス。抽象クラスの条件式の組み立てを直接確かめるために使う。 */
  private static class ConcreteCondition extends DetailFilterConditionBase {}

  @Nested
  class appendStartDateCondition {

    @Nested
    class 正常系 {

      /**
       * 開始月がある場合に、テーブルエイリアス付きの開始月以降の条件式を返すことを確かめる。 案件と要員は開始月で絞り込むため、開始月の条件を必ず含める必要がある。
       *
       * @given 開始月が 2026年1月1日
       * @when appendStartDateCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then "p.start_date >= ?" の条件式を返し、開始月を値として持つ
       */
      @Test
      void 開始月がある場合_開始月以降の条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        condition.setStartDate(START_DATE);
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendStartDateCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("p.start_date >= ?");
        assertThat(result.getParams()).containsExactly(START_DATE);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 開始月が null の場合は条件式を作らないことを確かめる。 開始月を指定しない検索で、開始月の条件を付けて案件を絞り込まないためである。
       *
       * @given 開始月が null
       * @when appendStartDateCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then 空の条件式を返す
       */
      @Test
      void 開始月がnullの場合_空の条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendStartDateCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("");
        assertThat(result.getParams()).isEmpty();
      }
    }
  }

  @Nested
  class appendAreaCondition {

    @Nested
    class 正常系 {

      /**
       * エリアがある場合に、テーブルエイリアス付きのエリアの条件式を返すことを確かめる。 エリアで一致する案件と要員だけを対象にするためである。
       *
       * @given エリアが 関東_首都圏
       * @when appendAreaCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then "p.area = ?" の条件式を返し、エリアを値として持つ
       */
      @Test
      void エリアがある場合_エリアの条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        condition.setArea(Area.関東_首都圏);
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendAreaCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("p.area = ?");
        assertThat(result.getParams()).containsExactly(Area.関東_首都圏);
      }
    }

    @Nested
    class 境界値 {

      /**
       * エリアが null の場合は条件式を作らないことを確かめる。 エリアを指定しない検索で、エリアの条件を付けて絞り込まないためである。
       *
       * @given エリアが null
       * @when appendAreaCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then 空の条件式を返す
       */
      @Test
      void エリアがnullの場合_空の条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendAreaCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("");
        assertThat(result.getParams()).isEmpty();
      }
    }
  }

  @Nested
  class appendPriceRangeCondition {

    @Nested
    class 正常系 {

      /**
       * 最小単価と最大単価の両方がある場合に、下限と上限の2件の条件式を返すことを確かめる。 単価は範囲で絞り込むため、下限と上限の両方を含める必要がある。
       *
       * @given 最小単価が 800000 円、最大単価が 1200000 円
       * @when appendPriceRangeCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then "p.unit_price >= ? AND p.unit_price <= ?" の条件式を返し、単価の値を2件返す
       */
      @Test
      void 最小単価と最大単価がある場合_単価の範囲の条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        condition.setMinPrice(new Money(800_000L));
        condition.setMaxPrice(new Money(1_200_000L));
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendPriceRangeCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("p.unit_price >= ? AND p.unit_price <= ?");
        assertThat(result.getParams())
            .containsExactly(new BigDecimal(800_000L), new BigDecimal(1_200_000L));
      }

      /**
       * 最小単価だけがある場合に、下限の条件式だけを返すことを確かめる。 上限を指定しない検索で、上限の条件を付けて案件を除外しないためである。
       *
       * @given 最小単価が 800000 円、最大単価が null
       * @when appendPriceRangeCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then "p.unit_price >= ?" の条件式を返し、下限の単価を値として持つ
       */
      @Test
      void 最小単価だけがある場合_下限の条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        condition.setMinPrice(new Money(800_000L));
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendPriceRangeCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("p.unit_price >= ?");
        assertThat(result.getParams()).containsExactly(new BigDecimal(800_000L));
      }

      /**
       * 最大単価だけがある場合に、上限の条件式だけを返すことを確かめる。 下限を指定しない検索で、下限の条件を付けて案件を除外しないためである。
       *
       * @given 最小単価が null、最大単価が 1200000 円
       * @when appendPriceRangeCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then "p.unit_price <= ?" の条件式を返し、上限の単価を値として持つ
       */
      @Test
      void 最大単価だけがある場合_上限の条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        condition.setMaxPrice(new Money(1_200_000L));
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendPriceRangeCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("p.unit_price <= ?");
        assertThat(result.getParams()).containsExactly(new BigDecimal(1_200_000L));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 単価が null の場合は条件式を作らないことを確かめる。 単価を指定しない検索で、単価の条件を付けて絞り込まないためである。
       *
       * @given 最小単価が null、最大単価が null
       * @when appendPriceRangeCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then 空の条件式を返す
       */
      @Test
      void 単価がnullの場合_空の条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendPriceRangeCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("");
        assertThat(result.getParams()).isEmpty();
      }

      /**
       * 値を持たない単価の場合は条件式を作らないことを確かめる。 値が null のまま SQL のパラメータに渡すと組み立てに失敗するためである。
       *
       * @given 最小単価が値を持たない Money、最大単価が値を持たない Money
       * @when appendPriceRangeCondition を テーブルエイリアス "p" のビルダーで実行し、ビルダーの結果を取得する
       * @then 空の条件式を返す
       */
      @Test
      void 値を持たない単価の場合_空の条件式を返す() {
        ConcreteCondition condition = new ConcreteCondition();
        condition.setMinPrice(Money.empty());
        condition.setMaxPrice(Money.empty());
        WhereClauseBuilder builder = new WhereClauseBuilder("p");

        condition.appendPriceRangeCondition(builder);
        WhereClauseAndParams result = builder.build();

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("");
        assertThat(result.getParams()).isEmpty();
      }
    }
  }
}
