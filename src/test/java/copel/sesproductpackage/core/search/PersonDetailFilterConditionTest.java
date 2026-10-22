package copel.sesproductpackage.core.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link PersonDetailFilterCondition} の単体テスト。 */
class PersonDetailFilterConditionTest {

  /** 開始月の検索に使う日付。 */
  private static final OriginalDateTime START_DATE = new OriginalDateTime(2026, 1, 1, 0, 0, 0);

  @Nested
  class buildWhereClause {

    @Nested
    class 正常系 {

      /**
       * 年齢、性別、エリアの条件を指定した場合に、定義の順に AND で連結することを確かめる。 要員検索は指定された条件をすべて満たす要員だけを返すためである。
       *
       * @given 最小年齢が 30、最大年齢が 50、性別が "Man"、エリアが 関東_首都圏
       * @when buildWhereClause を "p" で実行する
       * @then "p.age >= ? AND p.age <= ? AND p.gender = ? AND p.area = ?" を返す
       */
      @Test
      void 年齢と性別とエリアを指定する場合_条件を定義の順に連結した条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setMinAge(30);
        condition.setMaxAge(50);
        condition.setGender("Man");
        condition.setArea(Area.関東_首都圏);

        WhereClauseAndParams result = condition.buildWhereClause("p");

        assertThat(result.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("p.age >= ? AND p.age <= ? AND p.gender = ? AND p.area = ?");
        assertThat(result.getParams()).containsExactly(30, 50, "Man", Area.関東_首都圏);
      }

      /**
       * 開始月、単価、出社可能日数を指定した場合に、各条件を連結することを確かめる。 要員の稼働開始と単価と出社の条件を同時に満たす要員を返すためである。
       *
       * @given 開始月が 2026年1月1日、最小単価が 600000円、最大単価が 900000円、出社可能日数が 2
       * @when buildWhereClause を "p" で実行する
       * @then 4件の条件を AND で連結した条件式を返し、値を条件の順に4件返す
       */
      @Test
      void 開始月と単価と出社可能日数を指定する場合_4件の条件を連結した条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setStartDate(START_DATE);
        condition.setMinPrice(new Money(600_000L));
        condition.setMaxPrice(new Money(900_000L));
        condition.setOfficeAvailability(2);

        WhereClauseAndParams result = condition.buildWhereClause("p");

        assertThat(result.getWhereClauseWithoutWhereKeyword())
            .isEqualTo(
                "p.start_date >= ? AND p.unit_price >= ? AND p.unit_price <= ?"
                    + " AND p.office_availability >= ?");
        assertThat(result.getParams())
            .containsExactly(START_DATE, new BigDecimal(600_000L), new BigDecimal(900_000L), 2);
      }

      /**
       * 国籍に "日本以外" を指定した場合に、国籍が日本でないことを条件にすることを確かめる。 日本国籍以外の要員を探す画面の検索結果を絞り込むためである。
       *
       * @given 国籍が "日本以外"
       * @when buildWhereClause を null で実行する
       * @then "nationality IS NOT NULL AND nationality <> ?" を返し、"日本" を値として持つ
       */
      @Test
      void 国籍が日本以外の場合_国籍が日本でない条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setNationality("日本以外");

        WhereClauseAndParams result = condition.buildWhereClause(null);

        assertThat(result.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("nationality IS NOT NULL AND nationality <> ?");
        assertThat(result.getParams()).containsExactly("日本");
      }

      /**
       * 国籍に日本以外の値を指定した場合に、完全一致の条件にすることを確かめる。 国籍の格納値と一致する要員だけを返すためである。
       *
       * @given 国籍が "中国"
       * @when buildWhereClause を null で実行する
       * @then "nationality = ?" を返し、"中国" を値として持つ
       */
      @Test
      void 国籍が日本以外の特別な値ではない場合_国籍の完全一致条件を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setNationality("中国");

        WhereClauseAndParams result = condition.buildWhereClause(null);

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("nationality = ?");
        assertThat(result.getParams()).containsExactly("中国");
      }

      /**
       * N社先と所属形態の両方を指定した場合に、N社先形態の文字列全体で一致させることを確かめる。 所属は N社先と形態の組で表すため、片方だけでは一致させない。
       *
       * @given N社先が 1、所属形態が "正社員"
       * @when buildWhereClause を "p" で実行する
       * @then "p.organization = ?" を返し、"1社先正社員" を値として持つ
       */
      @Test
      void N社先と所属形態の両方を指定する場合_N社先形態の文字列で一致する条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setCompaniesTierNumber(1);
        condition.setEmploymentType("正社員");

        WhereClauseAndParams result = condition.buildWhereClause("p");

        assertThat(result.getWhereClauseWithoutWhereKeyword()).isEqualTo("p.organization = ?");
        assertThat(result.getParams()).containsExactly("1社先正社員");
      }
    }

    @Nested
    class 異常系 {

      /**
       * テーブルエイリアスに記号を含む場合に例外を投げることを確かめる。 エイリアスは SQL に直接連結されるため、識別子でない値を拒否する。
       *
       * @given 性別が "Man"、テーブルエイリアスが "p;x"
       * @when buildWhereClause を "p;x" で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void エイリアスに記号を含む場合_IllegalArgumentExceptionを投げる() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setGender("Man");

        assertThatThrownBy(() -> condition.buildWhereClause("p;x"))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 性別が空文字の場合に、性別の条件を含めないことを確かめる。 性別が未選択の検索で、空の値で絞り込んで要員が消えないようにするためである。
       *
       * @given 性別が空文字
       * @when buildWhereClause を null で実行する
       * @then 空の条件式を返す
       */
      @Test
      void 性別が空文字の場合_空の条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setGender("");

        assertThat(condition.buildWhereClause(null).isEmpty()).isTrue();
      }

      /**
       * 性別が null の場合に、性別の条件を含めないことを確かめる。 性別が未指定の検索で、条件を付けないようにするためである。
       *
       * @given 性別が null
       * @when buildWhereClause を null で実行する
       * @then 空の条件式を返す
       */
      @Test
      void 性別がnullの場合_空の条件式を返す() {
        assertThat(new PersonDetailFilterCondition().buildWhereClause(null).isEmpty()).isTrue();
      }

      /**
       * 国籍が空文字の場合に、国籍の条件を含めないことを確かめる。 国籍が未選択の検索で、空の値で絞り込んで要員が消えないようにするためである。
       *
       * @given 国籍が空文字
       * @when buildWhereClause を null で実行する
       * @then 空の条件式を返す
       */
      @Test
      void 国籍が空文字の場合_空の条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setNationality("");

        assertThat(condition.buildWhereClause(null).isEmpty()).isTrue();
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
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setMinPrice(Money.empty());
        condition.setMaxPrice(Money.empty());

        assertThat(condition.buildWhereClause(null).isEmpty()).isTrue();
      }

      /**
       * N社先だけを指定した場合に、所属の条件を含めないことを確かめる。 所属は N社先と形態の両方が揃ったときだけ絞り込む仕様であるためである。
       *
       * @given N社先が 1、所属形態が指定されていない
       * @when buildWhereClause を null で実行する
       * @then 空の条件式を返す
       */
      @Test
      void N社先だけを指定する場合_空の条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setCompaniesTierNumber(1);

        assertThat(condition.buildWhereClause(null).isEmpty()).isTrue();
      }

      /**
       * 所属形態だけを指定した場合に、所属の条件を含めないことを確かめる。 所属は N社先と形態の両方が揃ったときだけ絞り込む仕様であるためである。
       *
       * @given N社先が指定されておらず、所属形態が "正社員"
       * @when buildWhereClause を null で実行する
       * @then 空の条件式を返す
       */
      @Test
      void 所属形態だけを指定する場合_空の条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setEmploymentType("正社員");

        assertThat(condition.buildWhereClause(null).isEmpty()).isTrue();
      }

      /**
       * 所属形態が空文字の場合に、所属の条件を含めないことを確かめる。 形態が空のN社先の文字列で一致させると、どの要員にも一致しないためである。
       *
       * @given N社先が 1、所属形態が空文字
       * @when buildWhereClause を null で実行する
       * @then 空の条件式を返す
       */
      @Test
      void 所属形態が空文字の場合_空の条件式を返す() {
        PersonDetailFilterCondition condition = new PersonDetailFilterCondition();
        condition.setCompaniesTierNumber(1);
        condition.setEmploymentType("");

        assertThat(condition.buildWhereClause(null).isEmpty()).isTrue();
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
        assertThat(new PersonDetailFilterCondition().buildWhereClause(null).isEmpty()).isTrue();
      }
    }
  }
}
