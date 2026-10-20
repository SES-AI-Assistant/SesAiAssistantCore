package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link JobInfoSchema} の単体テスト。 */
class JobInfoSchemaTest {

  @Nested
  class getPrice {

    @Nested
    class 正常系 {

      /**
       * 単価が万円で設定されている場合に、円に換算した金額を返すことを確かめる。 案件の単価は円で比較するため、万円の入力を換算してから扱う必要があるので、その仕様を守る。
       *
       * @given 単価が 60 万円である
       * @when getPrice を呼び出す
       * @then 60 万円を円に換算した 600000 円の金額を返す
       */
      @Test
      void 単価が60万円の場合_円に換算した金額を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setPriceInMan(new BigDecimal("60"));

        assertThat(schema.getPrice()).isEqualTo(new Money(600000L));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 単価が未設定の場合に、空の金額を返すことを確かめる。 未設定の単価を 0 円として扱うと、金額の無い案件が無料扱いに見えるので、その仕様を守る。
       *
       * @given 単価が未設定である
       * @when getPrice を呼び出す
       * @then 空の金額を返す
       */
      @Test
      void 単価が未設定の場合_空の金額を返す() {
        assertThat(new JobInfoSchema().getPrice().isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class toSummaryText {

    @Nested
    class 正常系 {

      /**
       * 全ての項目が設定されている場合に、見出しごとの要約を返すことを確かめる。 要約は AI の判定の入力になるため、項目の並びと書式を固定して崩れを防ぐことを守る。
       *
       * @given 必須、尚可のスキル、開始年月、単価、フルリモート、清算幅、その他の条件が設定されている
       * @when toSummaryText を呼び出す
       * @then 全ての見出しと項目を並べた要約を返し、尚可の行も必須と同じ書式にする
       */
      @Test
      void 全ての項目が設定されている場合_見出しごとの要約を返す() {
        JobInfoSchema schema = fullSchema();

        assertThat(schema.toSummaryText())
            .isEqualTo(
                "案件A\n"
                    + "■概要\n概要B\n"
                    + "■必須\n・Java: 1年\n"
                    + "■尚可\n・Spring: 6ヶ月\n"
                    + "■開始: 2026年4月\n"
                    + "■単価: "
                    + new Money(600000L)
                    + "\n■出社要件: フルリモート\n"
                    + "■清算幅: 140-180\n"
                    + "■その他\n・英語可");
      }

      /**
       * 出社要件が常駐の場合に、常駐と案件の場所を要約に含めることを確かめる。 常駐の案件は場所が判断材料になるため、場所を省かないことを守る。
       *
       * @given 出社要件が5、場所が "東京都" である
       * @when toSummaryText を呼び出す
       * @then 出社要件を常駐とし、場所 "東京都" を含む要約を返す
       */
      @Test
      void 出社要件が常駐の場合_常駐と場所を含む要約を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setOfficeRequirements(5);
        schema.setPlace("東京都");

        assertThat(schema.toSummaryText()).contains("■出社要件: 常駐\n■場所: 東京都");
      }

      /**
       * 出社要件が週単位で場所がある場合に、場所を含む要約を返すことを確かめる。 週単位の出社の案件でも場所を要約に残すことを守る。
       *
       * @given 出社要件が3で、場所が "大阪" である
       * @when toSummaryText を呼び出す
       * @then 場所 "大阪" を含む要約を返す
       */
      @Test
      void 出社要件が週単位で場所がある場合_場所を含む要約を返す() throws Exception {
        JobInfoSchema schema = fullSchema();
        schema.setOfficeRequirements(3);
        schema.setPlace("大阪");

        assertThat(schema.toSummaryText()).contains("■場所: 大阪");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 出社要件が週単位で、場所が未設定の場合に、場所を不明として要約することを確かめる。 場所の空欄を出すと AI が場所の無い案件として判定するおそれがあるので、その仕様を守る。
       *
       * @given 出社要件が週3、場所が未設定である
       * @when toSummaryText を呼び出す
       * @then 出社要件を週3とし、場所を不明とした要約を返す
       */
      @Test
      void 出社要件が週単位で場所が未設定の場合_場所を不明とした要約を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setOfficeRequirements(3);

        assertThat(schema.toSummaryText()).contains("■出社要件: 週3\n■場所: 不明");
      }

      /**
       * 任意の項目が未設定の場合に、その見出しを含めないことを確かめる。 未設定の項目を空の見出しで出すと、要約の読みやすさが損なわれるので、その仕様を守る。
       *
       * @given 必須と尚可のスキル、開始年月、清算幅、その他の条件が未設定である
       * @when toSummaryText を呼び出す
       * @then 未設定の見出しを含まない要約を返す
       */
      @Test
      void 任意の項目が未設定の場合_未設定の見出しを含まない要約を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setTitle("案件B");
        schema.setOverview("概要C");
        schema.setOfficeRequirements(0);

        assertThat(schema.toSummaryText())
            .isEqualTo("案件B\n■概要\n概要C\n■単価: " + Money.empty() + "\n■出社要件: フルリモート");
      }

      /**
       * 要約が1000文字を超える場合に、1000文字に切り詰めることを確かめる。 要約の長さは AI への入力の上限に合わせるため、超過分を切り捨てることを守る。
       *
       * @given その他の条件が200件あり、それぞれが10文字である
       * @when toSummaryText を呼び出す
       * @then 1000文字に切り詰めた要約を返す
       */
      @Test
      void 要約が1000文字を超える場合_1000文字に切り詰めた要約を返す() {
        List<String> others = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
          others.add("あ".repeat(10));
        }
        JobInfoSchema schema = new JobInfoSchema();
        schema.setOtherRequirements(others);

        assertThat(schema.toSummaryText()).hasSize(1000);
      }

      /**
       * 必須、尚可、その他の一覧が空の場合に、それぞれの見出しを含めないことを確かめる。 空の一覧を見出しとして出さないことを守る。
       *
       * @given 必須、尚可、その他の一覧が空である
       * @when toSummaryText を呼び出す
       * @then 必須、尚可、その他の見出しを含まない要約を返す
       */
      @Test
      void 必須と尚可とその他の一覧が空の場合_見出しを含まない要約を返す() throws Exception {
        JobInfoSchema schema = fullSchema();
        schema.setMustList(List.of());
        schema.setWantList(List.of());
        schema.setOtherRequirements(List.of());

        assertThat(schema.toSummaryText()).doesNotContain("■必須", "■尚可", "■その他");
      }

      /**
       * 出社要件が常駐で場所が未設定の場合に、場所を不明として要約することを確かめる。 常駐の案件で場所が空欄にならないことを守る。
       *
       * @given 出社要件が5で、場所が未設定である
       * @when toSummaryText を呼び出す
       * @then 場所を不明とした要約を返す
       */
      @Test
      void 出社要件が常駐で場所が未設定の場合_場所を不明とした要約を返す() throws Exception {
        JobInfoSchema schema = fullSchema();
        schema.setOfficeRequirements(5);
        schema.setPlace(null);

        assertThat(schema.toSummaryText()).contains("■場所: 不明");
      }
    }
  }

  @Nested
  class getStartDateAsOriginalDateTime {

    @Nested
    class 正常系 {

      /**
       * 開始年月が年と月の区切りで書かれている場合に、その年月の日時を返すことを確かめる。 開始年月の判定は案件の期間の比較に使うため、書かれた年月をそのまま使うことを守る。
       *
       * @given 開始年月が "2026/4" である
       * @when getStartDateAsOriginalDateTime を呼び出す
       * @then 2026年4月の日時を返す
       */
      @Test
      void 開始年月が年と月で書かれている場合_その年月の日時を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setStartYearMonth("2026/4");

        assertThat(schema.getStartDateAsOriginalDateTime().getYYYYMM()).isEqualTo("202604");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 開始年月が年と月の区切りで書かれていない場合に、現在の年月を返すことを確かめる。 解釈できない文字列で例外を投げると、案件の登録全体が止まるので、その仕様を守る。
       *
       * @given 開始年月が "2026年4月" で、現在時刻が2026年10月20日である
       * @when getStartDateAsOriginalDateTime を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 区切りが無い開始年月の場合_現在の年月を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setStartYearMonth("2026年4月");

        assertThat(schema.getStartDateAsOriginalDateTime().getYYYYMM()).isEqualTo("202610");
      }

      /**
       * 開始年月の数字が解釈できない場合に、現在の年月を返すことを確かめる。 数字に変換できない値で例外を投げると、案件の登録全体が止まるので、その仕様を守る。
       *
       * @given 開始年月が "abc/xx" で、現在時刻が2026年10月20日である
       * @when getStartDateAsOriginalDateTime を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 数字に変換できない開始年月の場合_現在の年月を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setStartYearMonth("abc/xx");

        assertThat(schema.getStartDateAsOriginalDateTime().getYYYYMM()).isEqualTo("202610");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 開始年月が未設定の場合に、現在の年月を返すことを確かめる。 開始年月が無い案件でも期間の比較ができるよう、今月を既定値とするので、その仕様を守る。
       *
       * @given 開始年月が未設定で、現在時刻が2026年10月20日である
       * @when getStartDateAsOriginalDateTime を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 開始年月が未設定の場合_現在の年月を返す() {
        assertThat(new JobInfoSchema().getStartDateAsOriginalDateTime().getYYYYMM())
            .isEqualTo("202610");
      }
    }
  }

  @Nested
  class getMustSkillsAsString {

    @Nested
    class 正常系 {

      /**
       * 必須スキルが設定されている場合に、改行で区切った文字列を返すことを確かめる。 必須スキルは1行ずつ並べて AI に渡すため、区切りの形式を固定するので、その仕様を守る。
       *
       * @given 必須スキルが Java の12か月と SQL の期間未設定である
       * @when getMustSkillsAsString を呼び出す
       * @then 改行で区切った "Java: 1年" と "SQL" の文字列を返す
       */
      @Test
      void 必須スキルが設定されている場合_改行で区切った文字列を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setMustList(List.of(requirement("Java", 12), requirement("SQL", null)));

        assertThat(schema.getMustSkillsAsString()).isEqualTo("Java: 1年\nSQL");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須スキルが空の場合に、null を返すことを確かめる。 空の一覧を文字列にすると、要件の欄が空でなく見えてしまうので、その仕様を守る。
       *
       * @given 必須スキルの一覧が空である
       * @when getMustSkillsAsString を呼び出す
       * @then null を返す
       */
      @Test
      void 必須スキルの一覧が空の場合_nullを返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setMustList(List.of());

        assertThat(schema.getMustSkillsAsString()).isNull();
      }

      /**
       * 必須スキルの一覧が未設定の場合に、null を返すことを確かめる。 未設定の一覧を空の文字列にしないことを守る。
       *
       * @given 必須スキルの一覧が未設定である
       * @when getMustSkillsAsString を呼び出す
       * @then null を返す
       */
      @Test
      void 必須スキルの一覧が未設定の場合_nullを返す() throws Exception {
        assertThat(new JobInfoSchema().getMustSkillsAsString()).isNull();
      }
    }
  }

  @Nested
  class getWantSkillsAsString {

    @Nested
    class 正常系 {

      /**
       * 尚可スキルが設定されている場合に、改行で区切った文字列を返すことを確かめる。 尚可スキルは必須と同じ形式で並べるため、区切りの形式を固定するので、その仕様を守る。
       *
       * @given 尚可スキルが Spring の6か月である
       * @when getWantSkillsAsString を呼び出す
       * @then "Spring: 6ヶ月" を返す
       */
      @Test
      void 尚可スキルが設定されている場合_改行で区切った文字列を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setWantList(List.of(requirement("Spring", 6)));

        assertThat(schema.getWantSkillsAsString()).isEqualTo("Spring: 6ヶ月");
      }

      /**
       * 尚可スキルが2件ある場合に、2件を改行で区切った文字列を返すことを確かめる。 2件目以降も区切りを入れて並べることを守る。
       *
       * @given 尚可スキルが Spring の6か月と Docker の期間未設定である
       * @when getWantSkillsAsString を呼び出す
       * @then 改行で区切った "Spring: 6ヶ月" と "Docker" の文字列を返す
       */
      @Test
      void 尚可スキルが2件ある場合_改行で区切った文字列を返す() throws Exception {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setWantList(List.of(requirement("Spring", 6), requirement("Docker", null)));

        assertThat(schema.getWantSkillsAsString()).isEqualTo("Spring: 6ヶ月\nDocker");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 尚可スキルが未設定の場合に、null を返すことを確かめる。 尚可スキルが無い案件を空文字で返すと、欄の有無を区別できなくなるので、その仕様を守る。
       *
       * @given 尚可スキルの一覧が未設定である
       * @when getWantSkillsAsString を呼び出す
       * @then null を返す
       */
      @Test
      void 尚可スキルの一覧が未設定の場合_nullを返す() {
        assertThat(new JobInfoSchema().getWantSkillsAsString()).isNull();
      }

      /**
       * 尚可スキルの一覧が空の場合に、null を返すことを確かめる。 空の一覧を空の文字列にしないことを守る。
       *
       * @given 尚可スキルの一覧が空である
       * @when getWantSkillsAsString を呼び出す
       * @then null を返す
       */
      @Test
      void 尚可スキルの一覧が空の場合_nullを返す() throws Exception {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setWantList(List.of());

        assertThat(schema.getWantSkillsAsString()).isNull();
      }
    }
  }

  @Nested
  class getOtherRequirementsAsString {

    @Nested
    class 正常系 {

      /**
       * その他の条件が設定されている場合に、改行で連結した文字列を返すことを確かめる。 その他の条件は1件ずつ行に分けて扱うため、連結の形式を固定するので、その仕様を守る。
       *
       * @given その他の条件が "英語可" と "リモート可" である
       * @when getOtherRequirementsAsString を呼び出す
       * @then 改行で連結した "英語可\nリモート可" を返す
       */
      @Test
      void その他の条件が設定されている場合_改行で連結した文字列を返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setOtherRequirements(List.of("英語可", "リモート可"));

        assertThat(schema.getOtherRequirementsAsString()).isEqualTo("英語可\nリモート可");
      }
    }

    @Nested
    class 境界値 {

      /**
       * その他の条件が空の場合に、null を返すことを確かめる。 空の条件を文字列にすると、条件が存在するかのように見えてしまうので、その仕様を守る。
       *
       * @given その他の条件の一覧が空である
       * @when getOtherRequirementsAsString を呼び出す
       * @then null を返す
       */
      @Test
      void その他の条件の一覧が空の場合_nullを返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setOtherRequirements(List.of());

        assertThat(schema.getOtherRequirementsAsString()).isNull();
      }

      /**
       * その他の条件の一覧が未設定の場合に、null を返すことを確かめる。 未設定の条件を空の文字列にしないことを守る。
       *
       * @given その他の条件の一覧が未設定である
       * @when getOtherRequirementsAsString を呼び出す
       * @then null を返す
       */
      @Test
      void その他の条件の一覧が未設定の場合_nullを返す() throws Exception {
        assertThat(new JobInfoSchema().getOtherRequirementsAsString()).isNull();
      }
    }
  }

  @Nested
  class isValid {

    @Nested
    class 正常系 {

      /**
       * 案件名と概要と必須スキルが揃っている場合に、有効と判定することを確かめる。 登録に必要な最小限の情報が揃っている案件だけを登録するので、その仕様を守る。
       *
       * @given 案件名 "案件A" と概要 "概要B" と必須スキルが設定されている
       * @when isValid を呼び出す
       * @then true を返す
       */
      @Test
      void 案件名と概要と必須スキルが揃っている場合_trueを返す() {
        JobInfoSchema schema = fullSchema();

        assertThat(schema.isValid()).isTrue();
      }

      /**
       * 概要が無くても必須スキルがあれば有効と判定することを確かめる。 概要が空でも必須スキルで案件の内容を判断できるため、登録の対象から外さないことを守る。
       *
       * @given 案件名 "案件A" があり、概要が空白だけで、必須スキルが1件ある
       * @when isValid を呼び出す
       * @then true を返す
       */
      @Test
      void 概要が空白だけでも必須スキルがある場合_trueを返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setTitle("案件A");
        schema.setOverview("  ");
        schema.setMustList(List.of(requirement("Java", 12)));

        assertThat(schema.isValid()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 案件名が未設定の場合に、無効と判定することを確かめる。 案件名が null のまま判定を続けると例外になるため、先に無効として返すことを守る。
       *
       * @given 案件名が未設定で、概要と必須スキルが設定されている
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 案件名が未設定の場合_falseを返す() {
        JobInfoSchema schema = fullSchema();
        schema.setTitle(null);

        assertThat(schema.isValid()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件名が空白だけの場合に、無効と判定することを確かめる。 案件名の無い案件は一覧に表示できず、重複の判定もできないので、その仕様を守る。
       *
       * @given 案件名が " " で、概要と必須スキルが設定されている
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 案件名が空白だけの場合_falseを返す() {
        JobInfoSchema schema = fullSchema();
        schema.setTitle("  ");

        assertThat(schema.isValid()).isFalse();
      }

      /**
       * 概要も必須スキルも無い場合に、無効と判定することを確かめる。 内容の無い案件を登録すると、マッチングの対象にならないので、その仕様を守る。
       *
       * @given 案件名 "案件A" があり、概要が未設定で、必須スキルの一覧が空である
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 概要も必須スキルも無い場合_falseを返す() {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setTitle("案件A");
        schema.setMustList(List.of());

        assertThat(schema.isValid()).isFalse();
      }

      /**
       * 必須スキルの一覧が未設定で概要も無い場合に、無効と判定することを確かめる。 必須スキルの一覧が null のまま判定しても例外にならないことを守る。
       *
       * @given 案件名が "案件A" で、概要が未設定、必須スキルの一覧が未設定である
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 必須スキルの一覧が未設定で概要も無い場合_falseを返す() throws Exception {
        JobInfoSchema schema = new JobInfoSchema();
        schema.setTitle("案件A");
        schema.setMustList(null);

        assertThat(schema.isValid()).isFalse();
      }
    }
  }

  /**
   * 全ての項目を設定した案件を作る.
   *
   * @return 案件情報
   */
  private static JobInfoSchema fullSchema() {
    JobInfoSchema schema = new JobInfoSchema();
    schema.setTitle("案件A");
    schema.setOverview("概要B");
    schema.setMustList(List.of(requirement("Java", 12)));
    schema.setWantList(List.of(requirement("Spring", 6)));
    schema.setStartYearMonth("2026/4");
    schema.setPriceInMan(new BigDecimal("60"));
    schema.setOfficeRequirements(0);
    schema.setBaseHoursRange("140-180");
    schema.setOtherRequirements(List.of("英語可"));
    return schema;
  }

  /**
   * 要件を1件作る.
   *
   * @param perspective 要求項目
   * @param durationMonths 必要な月数
   * @return 要件
   */
  private static JobInfoSchema.Requirements requirement(
      final String perspective, final Integer durationMonths) {
    JobInfoSchema.Requirements requirement = new JobInfoSchema.Requirements();
    requirement.setPerspective(perspective);
    requirement.setDurationMonths(durationMonths);
    return requirement;
  }
}
