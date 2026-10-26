package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link PersonInfoSchema} の単体テスト。 */
class PersonInfoSchemaTest {

  @Nested
  class getPrice {

    @Nested
    class 正常系 {

      /**
       * 単価が万円で設定されている場合に、円に換算した金額を返すことを確かめる。 要員の単価も案件と同じ円の単位で比較するため、換算した値を使うことを守る。
       *
       * @given 単価が 65 万円である
       * @when getPrice を呼び出す
       * @then 65 万円を円に換算した 650000 円の金額を返す
       */
      @Test
      void 単価が65万円の場合_円に換算した金額を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setPriceInMan(new BigDecimal("65"));

        assertThat(schema.getPrice()).isEqualTo(new Money(650000L));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 単価が未設定の場合に、空の金額を返すことを確かめる。 未設定の単価を 0 円として扱うと、単価の無い要員が無料で提供されるように見えることを守る。
       *
       * @given 単価が未設定である
       * @when getPrice を呼び出す
       * @then 空の金額を返す
       */
      @Test
      void 単価が未設定の場合_空の金額を返す() {
        assertThat(new PersonInfoSchema().getPrice().isEmpty()).isTrue();
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
       * @given 名前、年齢、性別、所属、経歴、NG 条件、備考の全ての項目が設定されている
       * @when toSummaryText を呼び出す
       * @then 全ての見出しと項目を並べた要約を返す
       */
      @Test
      void 全ての項目が設定されている場合_見出しごとの要約を返す() {
        PersonInfoSchema schema = fullSchema();

        assertThat(schema.toSummaryText())
            .isEqualTo(
                "■名前: 山田太郎\n"
                    + "■年齢: 35歳\n"
                    + "■性別: 男性\n"
                    + "■国籍: 日本\n"
                    + "■開始: 2026年4月\n"
                    + "■単価: "
                    + new Money(650000L)
                    + "\n■所属形態: 弊社所属正社員\n"
                    + "■場所: 東京都\n"
                    + "■出社: 常駐可\n"
                    + "■経歴\n・Java: 5年\n"
                    + "■NG条件\n・夜間\n"
                    + "■備考\n・英語可");
      }

      /**
       * 所属が2社先の個人事業主の場合に、所属形態を社数と雇用形態で表示することを確かめる。 所属の段数が要約に出ないと、要員の所属先を誤って判定することを守る。
       *
       * @given 所属の段数が2、雇用形態が個人事業主である
       * @when toSummaryText を呼び出す
       * @then 所属形態を "2社先個人事業主" として含む要約を返す
       */
      @Test
      void 所属が2社先の個人事業主の場合_所属形態を社数と雇用形態で含む要約を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setName("山田太郎");
        schema.setGender(Gender.Man);
        schema.setOrganization(organization(2, PersonInfoSchema.EmploymentType.個人事業主));

        assertThat(schema.toSummaryText()).contains("■所属形態: 2社先個人事業主");
      }

      /**
       * 所属の段数が0の場合に、弊社所属の所属形態を表示することを確かめる。 自社の要員は弊社所属として区別して表示することを守る。
       *
       * @given 所属の段数が0、雇用形態が正社員である
       * @when toSummaryText を呼び出す
       * @then 所属形態を "弊社所属正社員" として含む要約を返す
       */
      @Test
      void 所属の段数が0の場合_弊社所属の所属形態を含む要約を返す() {
        PersonInfoSchema schema = minimalSchema();
        schema.setOrganization(organization(0, PersonInfoSchema.EmploymentType.正社員));

        assertThat(schema.toSummaryText()).contains("■所属形態: 弊社所属正社員");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 年齢が0以下の場合に、年齢の見出しを含めないことを確かめる。 年齢が未設定の既定値のまま出力すると、存在しない年齢として読まれることを守る。
       *
       * @given 年齢が未設定の既定値である
       * @when toSummaryText を呼び出す
       * @then 年齢の見出しを含まない要約を返す
       */
      @Test
      void 年齢が未設定の場合_年齢の見出しを含まない要約を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setName("山田太郎");
        schema.setGender(Gender.Woman);

        assertThat(schema.toSummaryText()).doesNotContain("■年齢").contains("■性別: 女性");
      }

      /**
       * 出社希望がフルリモートの場合に、フルリモート希望と表示することを確かめる。 出社の数値 0 はフルリモートを表すので、他の週数と区別して表示することを守る。
       *
       * @given 出社の希望が0である
       * @when toSummaryText を呼び出す
       * @then 出社の見出しをフルリモート希望として含む要約を返す
       */
      @Test
      void 出社の希望が0の場合_フルリモート希望を含む要約を返す() {
        PersonInfoSchema schema = minimalSchema();
        schema.setOfficeAvailability(0);

        assertThat(schema.toSummaryText()).contains("■出社: フルリモート希望");
      }

      /**
       * 出社希望が週単位の場合に、週数の上限として表示することを確かめる。 週数の上限を示すことで、案件の出社要件との照合を可能にすることを守る。
       *
       * @given 出社の希望が3である
       * @when toSummaryText を呼び出す
       * @then 出社の見出しを週3まで可として含む要約を返す
       */
      @Test
      void 出社の希望が週単位の場合_週数の上限を含む要約を返す() {
        PersonInfoSchema schema = minimalSchema();
        schema.setOfficeAvailability(3);

        assertThat(schema.toSummaryText()).contains("■出社: 週3まで可");
      }

      /**
       * 場所と所属と任意の一覧が未設定の場合に、それらの見出しを含めないことを確かめる。 未設定の項目を空の見出しで出すと、要約の読みやすさが損なわれることを守る。
       *
       * @given 場所、所属、経歴、NG 条件、備考が未設定である
       * @when toSummaryText を呼び出す
       * @then それぞれの見出しを含まない要約を返す
       */
      @Test
      void 場所と所属と任意の一覧が未設定の場合_それぞれの見出しを含まない要約を返す() {
        PersonInfoSchema schema = minimalSchema();

        assertThat(schema.toSummaryText()).doesNotContain("■所属形態", "■場所", "■経歴", "■NG条件", "■備考");
      }

      /**
       * 場所が空文字の場合に、場所の見出しを含めないことを確かめる。 空の場所を表示すると、場所の記載が無い要員に場所があるように見えることを守る。
       *
       * @given 場所が空文字である
       * @when toSummaryText を呼び出す
       * @then 場所の見出しを含まない要約を返す
       */
      @Test
      void 場所が空文字の場合_場所の見出しを含まない要約を返す() {
        PersonInfoSchema schema = minimalSchema();
        schema.setPlace("");

        assertThat(schema.toSummaryText()).doesNotContain("■場所");
      }

      /**
       * 開始年月が未設定の場合に、開始の見出しを含めないことを確かめる。 開始の見出しを空で出すと、開始時期が決まっているように読まれることを守る。
       *
       * @given 開始年月が未設定である
       * @when toSummaryText を呼び出す
       * @then 開始の見出しを含まない要約を返す
       */
      @Test
      void 開始年月が未設定の場合_開始の見出しを含まない要約を返す() {
        assertThat(minimalSchema().toSummaryText()).doesNotContain("■開始");
      }

      /**
       * 要約が1000文字を超える場合に、1000文字に切り詰めることを確かめる。 要約の長さは AI への入力の上限に合わせるため、超過分を切り捨てることを守る。
       *
       * @given 備考が200件あり、それぞれが10文字である
       * @when toSummaryText を呼び出す
       * @then 1000文字に切り詰めた要約を返す
       */
      @Test
      void 要約が1000文字を超える場合_1000文字に切り詰めた要約を返す() {
        List<String> remarks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
          remarks.add("あ".repeat(10));
        }
        PersonInfoSchema schema = minimalSchema();
        schema.setOtherRequirements(remarks);

        assertThat(schema.toSummaryText()).hasSize(1000);
      }

      /**
       * 経歴とNG条件と備考が空の一覧の場合に、それぞれの見出しを含めないことを確かめる。 空の一覧を見出しとして出さないことを守る。
       *
       * @given 経歴、NG 条件、備考の一覧が空である
       * @when toSummaryText を呼び出す
       * @then 経歴、NG 条件、備考の見出しを含まない要約を返す
       */
      @Test
      void 経歴とNG条件と備考が空の一覧の場合_見出しを含まない要約を返す() throws Exception {
        PersonInfoSchema schema = minimalSchema();
        schema.setExperiences(List.of());
        schema.setNgRequirements(List.of());
        schema.setOtherRequirements(List.of());

        assertThat(schema.toSummaryText()).doesNotContain("■経歴", "■NG条件", "■備考");
      }

      /**
       * 性別が未設定の場合に、性別をハイフンで表示した要約を返すことを確かめる。 性別の無い要員で例外にせず、表示できる要約を返すことを守る。
       *
       * @given 名前だけを設定し、性別が未設定である
       * @when toSummaryText を呼び出す
       * @then 性別の見出しにハイフンを含む要約を返す
       */
      @Test
      void 性別が未設定の場合_性別をハイフンとした要約を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setName("山田太郎");

        assertThat(schema.toSummaryText()).contains("■性別: -\n");
      }
    }
  }

  @Nested
  class getStartDateAsOriginalDateTime {

    @Nested
    class 正常系 {

      /**
       * 開始年月が年と月の区切りで書かれている場合に、その年月の日時を返すことを確かめる。 開始年月の判定は要員の稼働時期の比較に使うため、書かれた年月をそのまま使うことを守る。
       *
       * @given 開始年月が "2026/4" である
       * @when getStartDateAsOriginalDateTime を呼び出す
       * @then 2026年4月の日時を返す
       */
      @Test
      void 開始年月が年と月で書かれている場合_その年月の日時を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setStartYearMonth("2026/4");

        assertThat(schema.getStartDateAsOriginalDateTime().getYYYYMM()).isEqualTo("202604");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 開始年月の数字が解釈できない場合に、現在の年月を返すことを確かめる。 数字に変換できない値で例外を投げると、要員の登録全体が止まることを守る。
       *
       * @given 開始年月が "abc/xx" で、現在時刻が2026年10月20日である
       * @when getStartDateAsOriginalDateTime を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 数字に変換できない開始年月の場合_現在の年月を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setStartYearMonth("abc/xx");

        assertThat(schema.getStartDateAsOriginalDateTime().getYYYYMM()).isEqualTo("202610");
      }

      /**
       * 開始年月が区切りの無い形式の場合に、現在の年月を返すことを確かめる。 解釈できない形式で例外を投げると、要員の登録全体が止まることを守る。
       *
       * @given 開始年月が "2026年4月" で、現在時刻が2026年10月20日である
       * @when getStartDateAsOriginalDateTime を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 区切りが無い開始年月の場合_現在の年月を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setStartYearMonth("2026年4月");

        assertThat(schema.getStartDateAsOriginalDateTime().getYYYYMM()).isEqualTo("202610");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 開始年月が未設定の場合に、現在の年月を返すことを確かめる。 開始年月が無い要員でも稼働時期の比較ができるよう、今月を既定値とすることを守る。
       *
       * @given 開始年月が未設定で、現在時刻が2026年10月20日である
       * @when getStartDateAsOriginalDateTime を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 開始年月が未設定の場合_現在の年月を返す() {
        assertThat(new PersonInfoSchema().getStartDateAsOriginalDateTime().getYYYYMM())
            .isEqualTo("202610");
      }
    }
  }

  @Nested
  class getExperiencesAsString {

    @Nested
    class 正常系 {

      /**
       * 経歴が設定されている場合に、改行で区切った文字列を返すことを確かめる。 経歴は1行ずつ並べて AI に渡すため、区切りの形式を固定することを守る。
       *
       * @given 経歴が Java の60か月と SQL の期間未設定である
       * @when getExperiencesAsString を呼び出す
       * @then 改行で区切った "Java: 5年" と "SQL" の文字列を返す
       */
      @Test
      void 経歴が設定されている場合_改行で区切った文字列を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setExperiences(List.of(experience("Java", 60), experience("SQL", null)));

        assertThat(schema.getExperiencesAsString()).isEqualTo("Java: 5年\nSQL");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 経歴が空の場合に、null を返すことを確かめる。 空の経歴を文字列にすると、経歴があるかのように見えてしまうことを守る。
       *
       * @given 経歴の一覧が空である
       * @when getExperiencesAsString を呼び出す
       * @then null を返す
       */
      @Test
      void 経歴の一覧が空の場合_nullを返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setExperiences(List.of());

        assertThat(schema.getExperiencesAsString()).isNull();
      }

      /**
       * 経歴が未設定の場合に、null を返すことを確かめる。 未設定の経歴を空の文字列にしないことを守る。
       *
       * @given 経歴の一覧が未設定である
       * @when getExperiencesAsString を呼び出す
       * @then null を返す
       */
      @Test
      void 経歴の一覧が未設定の場合_nullを返す() {
        assertThat(new PersonInfoSchema().getExperiencesAsString()).isNull();
      }
    }
  }

  @Nested
  class toOrganizationDisplayText {

    @Nested
    class 正常系 {

      /**
       * 所属形態がある場合に、表示用の文字列を返すことを確かめる。 登録時の所属形態は画面と同じ表記で保存するためである。
       *
       * @given 所属形態の社先が 1、所属の種類が正社員
       * @when toOrganizationDisplayText を実行する
       * @then "1社先正社員" を返す
       */
      @Test
      void 所属形態がある場合_表示用の文字列を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setOrganization(organization(1, PersonInfoSchema.EmploymentType.正社員));

        assertThat(schema.toOrganizationDisplayText()).isEqualTo("1社先正社員");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 所属形態が無い場合に null を返すことを確かめる。 所属形態を抽出できなかった要員に、誤った値を保存しないためである。
       *
       * @given 所属形態が null
       * @when toOrganizationDisplayText を実行する
       * @then null を返す
       */
      @Test
      void 所属形態がnullの場合_nullを返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setOrganization(null);

        assertThat(schema.toOrganizationDisplayText()).isNull();
      }

      /**
       * 社先が0の所属形態を弊社所属として表示することを確かめる。 社先の0は自社の所属を表し、1社先とは表記を分けるためである。
       *
       * @given 所属形態の社先が 0、所属の種類が個人事業主
       * @when toOrganizationDisplayText を実行する
       * @then "弊社所属個人事業主" を返す
       */
      @Test
      void 社先が0の場合_弊社所属の文字列を返す() {
        PersonInfoSchema schema = new PersonInfoSchema();
        schema.setOrganization(organization(0, PersonInfoSchema.EmploymentType.個人事業主));

        assertThat(schema.toOrganizationDisplayText()).isEqualTo("弊社所属個人事業主");
      }
    }
  }

  @Nested
  class isValid {

    @Nested
    class 正常系 {

      /**
       * 名前と経歴が揃っている場合に、有効と判定することを確かめる。 名前と経歴が揃った要員だけを登録し、マッチングの対象にすることを守る。
       *
       * @given 名前 "山田太郎" と経歴が1件設定されている
       * @when isValid を呼び出す
       * @then true を返す
       */
      @Test
      void 名前と経歴が揃っている場合_trueを返す() {
        assertThat(validSchema("山田太郎").isValid()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 名前が未設定の場合に、無効と判定することを確かめる。 名前が null のまま判定を続けると例外になるため、先に無効として返すことを守る。
       *
       * @given 名前が未設定で、経歴が1件設定されている
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 名前が未設定の場合_falseを返す() {
        assertThat(validSchema(null).isValid()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 名前が空白だけの場合に、無効と判定することを確かめる。 名前の無い要員は特定できないため、登録の対象から外すことを守る。
       *
       * @given 名前が " " で、経歴が1件設定されている
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 名前が空白だけの場合_falseを返す() {
        assertThat(validSchema("  ").isValid()).isFalse();
      }

      /**
       * 名前が不明を表す語の場合に、無効と判定することを確かめる。 AI が名前を推測で埋めた値を、要員の名前として登録しないことを守る。
       *
       * @given 名前が "Unknown" で、経歴が1件設定されている
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 名前が不明を表す語の場合_falseを返す() {
        assertThat(validSchema("Unknown").isValid()).isFalse();
      }

      /**
       * 名前に文字が含まれない場合に、無効と判定することを確かめる。 数字だけの名前は人物を特定できないため、登録しないことを守る。
       *
       * @given 名前が "123" で、経歴が1件設定されている
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 名前に文字が含まれない場合_falseを返す() {
        assertThat(validSchema("123").isValid()).isFalse();
      }

      /**
       * 経歴が空の場合に、無効と判定することを確かめる。 経歴の無い要員はスキルの判定ができないため、登録の対象から外すことを守る。
       *
       * @given 名前 "山田太郎" があり、経歴の一覧が空である
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 経歴の一覧が空の場合_falseを返す() {
        PersonInfoSchema schema = validSchema("山田太郎");
        schema.setExperiences(List.of());

        assertThat(schema.isValid()).isFalse();
      }

      /**
       * 経歴が未設定の場合に、無効と判定することを確かめる。 未設定の経歴を空と同じく扱わないと、判定が例外で止まることを守る。
       *
       * @given 名前 "山田太郎" があり、経歴が未設定である
       * @when isValid を呼び出す
       * @then false を返す
       */
      @Test
      void 経歴が未設定の場合_falseを返す() {
        PersonInfoSchema schema = validSchema("山田太郎");
        schema.setExperiences(null);

        assertThat(schema.isValid()).isFalse();
      }
    }
  }

  /**
   * 全ての項目を設定した要員を作る.
   *
   * @return 要員情報
   */
  private static PersonInfoSchema fullSchema() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setName("山田太郎");
    schema.setAge(35);
    schema.setGender(Gender.Man);
    schema.setNationality("日本");
    schema.setStartYearMonth("2026/4");
    schema.setPriceInMan(new BigDecimal("65"));
    schema.setPlace("東京都");
    schema.setOfficeAvailability(5);
    schema.setOrganization(organization(0, PersonInfoSchema.EmploymentType.正社員));
    schema.setExperiences(List.of(experience("Java", 60)));
    schema.setNgRequirements(List.of("夜間"));
    schema.setOtherRequirements(List.of("英語可"));
    return schema;
  }

  /**
   * 名前と性別だけを設定した要員を作る.
   *
   * @return 要員情報
   */
  private static PersonInfoSchema minimalSchema() {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setName("山田太郎");
    schema.setGender(Gender.Man);
    return schema;
  }

  /**
   * 名前と1件の経歴を設定した要員を作る.
   *
   * @param name 名前
   * @return 要員情報
   */
  private static PersonInfoSchema validSchema(final String name) {
    PersonInfoSchema schema = new PersonInfoSchema();
    schema.setName(name);
    schema.setExperiences(List.of(experience("Java", 12)));
    return schema;
  }

  /**
   * 経歴を1件作る.
   *
   * @param perspective 要求項目
   * @param durationMonths 経験の月数
   * @return 経歴
   */
  private static PersonInfoSchema.Experience experience(
      final String perspective, final Integer durationMonths) {
    PersonInfoSchema.Experience experience = new PersonInfoSchema.Experience();
    experience.setPerspective(perspective);
    experience.setDurationMonths(durationMonths);
    return experience;
  }

  /**
   * 所属を作る.
   *
   * @param tier 所属の段数
   * @param employmentType 雇用形態
   * @return 所属
   */
  private static PersonInfoSchema.Organization organization(
      final int tier, final PersonInfoSchema.EmploymentType employmentType) {
    PersonInfoSchema.Organization organization = new PersonInfoSchema.Organization();
    organization.setCompaniesTierNumber(tier);
    organization.setEmploymentType(employmentType);
    return organization;
  }
}
