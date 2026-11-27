package copel.sesproductpackage.core.api.gpt.schema.entity;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** {@link SkillsheetInfoSchema} の単体テスト。 */
class SkillsheetInfoSchemaTest {

  /** 1案件の内容として使う、1000文字の上限を超えない短い内容。 */
  private static final String SHORT_PERSPECTIVE = "内容";

  /** 集計の警告ログを集めるアペンダー。除外した案件の警告を確かめるテストだけが使う。 */
  private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

  @BeforeEach
  void 警告ログを集める() {
    logAppender.start();
    ((Logger) LoggerFactory.getLogger(ProjectExperienceList.class)).addAppender(logAppender);
  }

  @AfterEach
  void 警告ログの収集を解除する() {
    ((Logger) LoggerFactory.getLogger(ProjectExperienceList.class)).detachAppender(logAppender);
    logAppender.stop();
  }

  @Nested
  class toSummaryText {

    @Nested
    class 正常系 {

      /**
       * 1件の案件のスキルの月数と内訳を、見出しごとの要約として返すことを確かめる。 要約は AI の判定の入力になるため、書式と項目の並びを固定して崩れを防ぐためである。
       *
       * @given スキル "Java" の案件が 2023年4月から2023年6月 で、内容が "基幹システムの改修"
       * @when toSummaryText を実行する
       * @then スキルの月数と内訳を見出しごとに並べた要約を返す
       */
      @Test
      void 案件が1件の場合_スキルの月数と内訳を含む要約を返す() {
        SkillsheetInfoSchema schema = schemaOf(project("2023年4月", "2023年6月", "基幹システムの改修", "Java"));

        assertThat(schema.toSummaryText())
            .isEqualTo("■スキル・経験\n・Java: 3ヶ月\n■直近のPJ経験\n2023年4月-2023年6月: 基幹システムの改修");
      }

      /**
       * スキル経験と内訳の両方がある場合の要約を、見出しごとに並べて返すことを確かめる。 既存の要約の見出しと書式を変えないためである。
       *
       * @given スキル "Java" の案件が 2023年8月から2023年11月 で、内容が "基本設計"
       * @when toSummaryText を実行する
       * @then スキルの月数と内訳の2つの見出しを含む要約を返す
       */
      @Test
      void スキルと案件の両方がある場合_見出しごとの要約を返す() {
        SkillsheetInfoSchema schema = schemaOf(project("2023年8月", "2023年11月", "基本設計", "Java"));

        assertThat(schema.toSummaryText())
            .isEqualTo("■スキル・経験\n・Java: 4ヶ月\n■直近のPJ経験\n2023年8月-2023年11月: 基本設計");
      }

      /**
       * 重ならない15か月と3か月の案件を合わせると、1年6ヶ月として要約することを確かめる。 経験年数を3年に誤って合算しないことを守るためである。
       *
       * @given スキル "Java" の案件が 2023年1月から2024年3月 と 2024年5月から2024年7月
       * @when toSummaryText を実行する
       * @then "・Java: 1年6ヶ月" を含み "・Java: 3年" を含まない要約を返す
       */
      @Test
      void 重ならない15か月と3か月の案件の場合_1年6ヶ月の合計を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2023年1月", "2024年3月", SHORT_PERSPECTIVE, "Java"),
                project("2024年5月", "2024年7月", SHORT_PERSPECTIVE, "Java"));

        assertThat(schema.toSummaryText()).contains("・Java: 1年6ヶ月").doesNotContain("・Java: 3年");
      }

      /**
       * 表記の違うだけの同じスキルを1行にまとめ、最初の表記を使うことを確かめる。 大文字小文字や全角半角の違いで、同じ経験を分けて数えないためである。
       *
       * @given スキル "java"、"Java"、"JAVA"、"ＪＡＶＡ"、" Java " の各案件が、それぞれ3か月で重ならない
       * @when toSummaryText を実行する
       * @then スキルの見出しの次に "・java: 1年3ヶ月" の1行だけを返す
       */
      @Test
      void 表記が異なる同じスキルの場合_1行にまとめた要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2020年1月", "2020年3月", SHORT_PERSPECTIVE, "java"),
                project("2020年4月", "2020年6月", SHORT_PERSPECTIVE, "Java"),
                project("2020年7月", "2020年9月", SHORT_PERSPECTIVE, "JAVA"),
                project("2020年10月", "2020年12月", SHORT_PERSPECTIVE, "ＪＡＶＡ"),
                project("2021年1月", "2021年3月", SHORT_PERSPECTIVE, " Java "));

        assertThat(schema.toSummaryText())
            .startsWith("■スキル・経験\n・java: 1年3ヶ月\n■直近のPJ経験")
            .doesNotContain("・Java")
            .doesNotContain("・JAVA");
      }

      /**
       * 途中の空白や英字の間のドットの有無だけが異なるスキルを1行にまとめ、最初の表記を使うことを確かめる。 MySQL と my sql や NodeJS と Node.js
       * が別々に集計されることを防ぐためである。
       *
       * @given スキル "MySQL"、"my sql"、"My SQL" の各案件と "NodeJS"、"Node.JS"、"Node.js" の各案件がそれぞれ3か月
       * @when toSummaryText を実行する
       * @then "・MySQL: 9ヶ月" と "・NodeJS: 9ヶ月" にまとめた要約を返す
       */
      @Test
      void 途中の空白や英字の間のドットだけが異なるスキルの場合_1行にまとめた要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2020年1月", "2020年3月", SHORT_PERSPECTIVE, "MySQL", "NodeJS"),
                project("2020年4月", "2020年6月", SHORT_PERSPECTIVE, "my sql", "Node.JS"),
                project("2020年7月", "2020年9月", SHORT_PERSPECTIVE, "My SQL", "Node.js"));

        assertThat(schema.toSummaryText())
            .startsWith("■スキル・経験\n・MySQL: 9ヶ月\n・NodeJS: 9ヶ月\n■直近のPJ経験")
            .doesNotContain("my sql")
            .doesNotContain("My SQL")
            .doesNotContain("Node.JS")
            .doesNotContain("Node.js");
      }

      /**
       * Java8 と Java11 を別のスキルとして、それぞれ行に分けることを確かめる。 バージョンの違う技術の経験を混ぜないためである。
       *
       * @given スキル "Java8" の案件と "Java11" の案件が、それぞれ3か月で重ならない
       * @when toSummaryText を実行する
       * @then "Java8" と "Java11" の2行を、登場順に並べて返す
       */
      @Test
      void Java8とJava11の場合_別々の2行を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2023年1月", "2023年3月", SHORT_PERSPECTIVE, "Java8"),
                project("2023年4月", "2023年6月", SHORT_PERSPECTIVE, "Java11"));

        assertThat(schema.toSummaryText())
            .isEqualTo(
                "■スキル・経験\n・Java8: 3ヶ月\n・Java11: 3ヶ月\n■直近のPJ経験\n"
                    + "2023年4月-2023年6月: 内容\n2023年1月-2023年3月: 内容");
      }

      /**
       * 月数の異なる4つのスキルを、月数の多い順に年月の表記で並べることを確かめる。 経験の長いスキルを要約の先頭に出すためである。
       *
       * @given スキル "S18" が18か月、"S12" が12か月、"S5" が5か月、"S13" が13か月の案件
       * @when toSummaryText を実行する
       * @then "1年6ヶ月"、"1年1ヶ月"、"1年"、"5ヶ月" の順にスキルを並べて返す
       */
      @Test
      void 月数が18と12と5と13の場合_月数の多い順に並べた要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2020年1月", "2021年6月", SHORT_PERSPECTIVE, "S18"),
                project("2020年1月", "2020年12月", SHORT_PERSPECTIVE, "S12"),
                project("2020年1月", "2020年5月", SHORT_PERSPECTIVE, "S5"),
                project("2020年1月", "2021年1月", SHORT_PERSPECTIVE, "S13"));

        assertThat(schema.toSummaryText())
            .startsWith("■スキル・経験\n・S18: 1年6ヶ月\n・S13: 1年1ヶ月\n・S12: 1年\n・S5: 5ヶ月\n■直近のPJ経験");
      }

      /**
       * 月数が異なるスキルを降順に並べ、同じ月数のスキルは登場順に並べることを確かめる。 同じ条件の行の並びが毎回変わらないようにするためである。
       *
       * @given スキル "Beta" が6か月、"Alpha" が12か月、"Gamma" が6か月、"Delta" が3か月の案件で、この順に登場する
       * @when toSummaryText を実行する
       * @then "Alpha"、"Beta"、"Gamma"、"Delta" の順にスキルを並べて返す
       */
      @Test
      void 月数が異なるスキルと同じ月数のスキルの場合_降順で同数は登場順に並べた要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2020年1月", "2020年6月", SHORT_PERSPECTIVE, "Beta"),
                project("2020年1月", "2020年12月", SHORT_PERSPECTIVE, "Alpha"),
                project("2021年1月", "2021年6月", SHORT_PERSPECTIVE, "Gamma"),
                project("2022年1月", "2022年3月", SHORT_PERSPECTIVE, "Delta"));

        assertThat(schema.toSummaryText())
            .startsWith("■スキル・経験\n・Alpha: 1年\n・Beta: 6ヶ月\n・Gamma: 6ヶ月\n・Delta: 3ヶ月\n■直近のPJ経験");
      }

      /**
       * 直近の案件の内訳を、終了年月と開始年月の新しい順に先頭5件だけ載せることを確かめる。 要約を短く保ち、最近の経験を優先して見せるためである。
       *
       * @given 案件が6件あり、終了年月が異なる4件と、終了年月が同じで開始年月が異なる2件、集計から除外される1件
       * @when toSummaryText を実行する
       * @then 終了年月の新しい順に先頭5件を返し、除外された案件と6件目を含まない
       */
      @Test
      void 案件が6件ある場合_終了年月の新しい順に先頭5件を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2023年1月", "2023年3月", "案件a", "Java"),
                project("2024年1月", "2024年6月", "案件b", "Java"),
                project("2024年4月", "2024年9月", "案件c", "Java"),
                project("2022年1月", "2022年2月", "案件d", "Java"),
                project("2023年10月", "2024年12月", "案件e", "Java"),
                project("2024年1月", "2024年12月", "案件f", "Java"),
                project("2025年1月", "2024年1月", "除外される案件", "Java"));

        assertThat(schema.toSummaryText())
            .endsWith(
                "■直近のPJ経験\n"
                    + "2024年1月-2024年12月: 案件f\n"
                    + "2023年10月-2024年12月: 案件e\n"
                    + "2024年4月-2024年9月: 案件c\n"
                    + "2024年1月-2024年6月: 案件b\n"
                    + "2023年1月-2023年3月: 案件a")
            .doesNotContain("案件d")
            .doesNotContain("除外される案件");
      }

      /**
       * 案件が50件ある場合に、50件すべてをスキルの月数に集計することを確かめる。 件数の上限ちょうどの案件を取りこぼさないためである。
       *
       * @given スキル "Java" の案件が、1か月ずつずれて重ならない50件
       * @when toSummaryText を実行する
       * @then 50か月分の "・Java: 4年2ヶ月" を含む要約を返す
       */
      @Test
      void 案件が50件ちょうどの場合_50件を集計した要約を返す() {
        SkillsheetInfoSchema.ProjectExperience[] projects =
            new SkillsheetInfoSchema.ProjectExperience[50];
        for (int i = 0; i < projects.length; i++) {
          String month = monthText(i);
          projects[i] = project(month, month, SHORT_PERSPECTIVE, "Java");
        }

        assertThat(schemaOf(projects).toSummaryText()).contains("・Java: 4年2ヶ月");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 案件の一覧が null の場合に、空文字を返すことを確かめる。 案件が未設定の応答でも例外にせず、要約を作らないためである。
       *
       * @given 案件の一覧が null
       * @when toSummaryText を実行する
       * @then 空文字を返す
       */
      @Test
      void 案件の一覧がnullの場合_空文字を返す() {
        assertThat(new SkillsheetInfoSchema().toSummaryText()).isEmpty();
      }

      /**
       * 案件の一覧が空の場合に、空文字を返すことを確かめる。 見出しだけの要約を返すと、AI が経験の無い要員を経験ありと読むおそれがあるためである。
       *
       * @given 案件の一覧が空のリスト
       * @when toSummaryText を実行する
       * @then 空文字を返す
       */
      @Test
      void 案件の一覧が空の場合_空文字を返す() {
        SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
        schema.setProjectExperiences(List.of());

        assertThat(schema.toSummaryText()).isEmpty();
      }

      /**
       * 開始年月が null の案件は集計から除外し、他の案件だけを要約することを確かめる。 1件の欠けで登録全体を失敗させないためである。
       *
       * @given 開始年月が null で内容が "除外される案件" の案件と、2023年4月から2023年6月の "Java" の案件
       * @when toSummaryText を実行する
       * @then 正常な案件のスキルと内訳だけを含む要約を返し、警告のログを1件出す
       */
      @Test
      void 開始年月がnullの案件の場合_その案件を除いた要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project(null, "2023年6月", "除外される案件", "Oracle"),
                project("2023年4月", "2023年6月", "基幹", "Java"));

        assertThat(schema.toSummaryText())
            .isEqualTo("■スキル・経験\n・Java: 3ヶ月\n■直近のPJ経験\n2023年4月-2023年6月: 基幹");
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getLevel)
            .containsExactly(Level.WARN);
      }

      /**
       * 終了年月が開始年月より前の案件は除外し、負の月数を出さないことを確かめる。 負の月数が合計に混ざると、経験が実際より少なく見えるためである。
       *
       * @given 2023年9月から2023年4月の "Java" の案件と、2023年4月から2023年6月の "Oracle" の案件
       * @when toSummaryText を実行する
       * @then "Oracle" の正常な案件だけを含む要約を返し、警告のログを1件出す
       */
      @Test
      void 終了年月が開始年月より前の案件の場合_除外した要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2023年9月", "2023年4月", "逆順の案件", "Java"),
                project("2023年4月", "2023年6月", "正常な案件", "Oracle"));

        assertThat(schema.toSummaryText())
            .isEqualTo("■スキル・経験\n・Oracle: 3ヶ月\n■直近のPJ経験\n2023年4月-2023年6月: 正常な案件")
            .doesNotContain("Java");
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getLevel)
            .containsExactly(Level.WARN);
      }

      /**
       * 解釈できない開始年月の案件は除外し、例外を投げないことを確かめる。 AI の出力の誤りで登録全体が失敗しないようにするためである。
       *
       * @given 開始年月が "2023年13月" と "令和5年4月" の2件の案件
       * @when toSummaryText を実行する
       * @then 空文字を返し、警告のログを2件出す
       */
      @Test
      void 開始年月が解釈できない案件の場合_除外して空文字と警告を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2023年13月", "2023年6月", SHORT_PERSPECTIVE, "Java"),
                project("令和5年4月", "2023年6月", SHORT_PERSPECTIVE, "Java"));

        assertThat(schema.toSummaryText()).isEmpty();
        assertThat(logAppender.list).extracting(ILoggingEvent::getLevel).containsOnly(Level.WARN);
        assertThat(logAppender.list).hasSize(2);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 開始と終了が同じ年月の案件を、1か月のスキルとして要約することを確かめる。 1か月の経験も0ではなく1ヶ月として見せるためである。
       *
       * @given スキル "Java" の案件が 2023年4月から2023年4月
       * @when toSummaryText を実行する
       * @then "・Java: 1ヶ月" を含む要約を返す
       */
      @Test
      void 開始と終了が同じ年月の場合_1ヶ月を含む要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(project("2023年4月", "2023年4月", SHORT_PERSPECTIVE, "Java"));

        assertThat(schema.toSummaryText()).contains("・Java: 1ヶ月");
      }

      /**
       * 1か月だけの案件のスキルを、1ヶ月の行として要約することを確かめる。 1か月の経験を省略すると、短い経験が要約から消えるためである。
       *
       * @given スキル "Xxx" の案件が 2024年1月から2024年1月
       * @when toSummaryText を実行する
       * @then "・Xxx: 1ヶ月" の行を含む要約を返す
       */
      @Test
      void 一か月だけの案件のスキルの場合_1ヶ月の行を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(project("2024年1月", "2024年1月", SHORT_PERSPECTIVE, "Xxx"));

        assertThat(schema.toSummaryText()).contains("・Xxx: 1ヶ月");
      }

      /**
       * 年をまたぐ案件を、開始と終了の両方を含めて4か月として要約することを確かめる。 年の境目で月数が1つずれないようにするためである。
       *
       * @given スキル "Java" の案件が 2023年11月から2024年2月
       * @when toSummaryText を実行する
       * @then "・Java: 4ヶ月" を含む要約を返す
       */
      @Test
      void 年をまたぐ案件の場合_4か月を含む要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(project("2023年11月", "2024年2月", SHORT_PERSPECTIVE, "Java"));

        assertThat(schema.toSummaryText()).contains("・Java: 4ヶ月");
      }

      /**
       * 重なる2件の案件では、重なる月を1回だけ数えて要約することを確かめる。 同じ時期の経験を二重に数えないためである。
       *
       * @given スキル "Java" の案件が 2023年4月から2023年9月 と 2023年7月から2023年12月
       * @when toSummaryText を実行する
       * @then "・Java: 9ヶ月" を含み "・Java: 12ヶ月" を含まない要約を返す
       */
      @Test
      void 重なる2件の案件の場合_重なる月を1回だけ数えた9ヶ月を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2023年4月", "2023年9月", SHORT_PERSPECTIVE, "Java"),
                project("2023年7月", "2023年12月", SHORT_PERSPECTIVE, "Java"));

        assertThat(schema.toSummaryText()).contains("・Java: 9ヶ月").doesNotContain("・Java: 12ヶ月");
      }

      /**
       * 終了年月が未来の案件を、記載どおりの3か月として要約することを確かめる。 未来の年月を補正や除外せず、記載どおりに集計する仕様を守るためである。
       *
       * @given スキル "Java" の案件が 2099年10月から2099年12月
       * @when toSummaryText を実行する
       * @then "・Java: 3ヶ月" を含む要約を返す
       */
      @Test
      void 終了年月が未来の案件の場合_3ヶ月を含む要約を返す() {
        SkillsheetInfoSchema schema =
            schemaOf(project("2099年10月", "2099年12月", SHORT_PERSPECTIVE, "Java"));

        assertThat(schema.toSummaryText()).contains("・Java: 3ヶ月");
      }

      /**
       * 要約の内容が1000文字ちょうどの場合は、切り詰めずにそのまま返すことを確かめる。 上限と同じ長さの要約を1文字でも削らないためである。
       *
       * @given スキル名が900文字の "Java" 相当の名前と、内容が59文字の案件が1件で要約が1000文字になる
       * @when toSummaryText を実行する
       * @then 1000文字の要約を全文のまま返す
       */
      @Test
      void 要約がちょうど1000文字の場合_切り詰めずに全文を返す() {
        String skill = "あ".repeat(900);
        String perspective = "い".repeat(59);
        SkillsheetInfoSchema schema = schemaOf(project("2023年4月", "2023年6月", perspective, skill));

        assertThat(schema.toSummaryText())
            .hasSize(1000)
            .isEqualTo("■スキル・経験\n・" + skill + ": 3ヶ月\n■直近のPJ経験\n2023年4月-2023年6月: " + perspective);
      }

      /**
       * 内訳を落とせば1000文字以内に収まる場合に、内訳の行を末尾から1行ずつ落とすことを確かめる。 スキルの経験は残し、古い案件の内訳から落とすという仕様を守るためである。
       *
       * @given スキル "Java" の案件が5件あり、それぞれ同じ期間と200文字の内容
       * @when toSummaryText を実行する
       * @then 末尾の1件を落とした4件の内訳と、スキルの行を残した要約を返す
       */
      @Test
      void 内訳を落とせば収まる場合_末尾の内訳を落とした要約を返す() {
        String perspective = "あ".repeat(200);
        SkillsheetInfoSchema schema =
            schemaOf(
                project("2024年4月", "2024年6月", perspective, "Java"),
                project("2024年4月", "2024年6月", perspective, "Java"),
                project("2024年4月", "2024年6月", perspective, "Java"),
                project("2024年4月", "2024年6月", perspective, "Java"),
                project("2024年4月", "2024年6月", perspective, "Java"));
        String detailLine = "2024年4月-2024年6月: " + perspective;

        assertThat(schema.toSummaryText())
            .isEqualTo(
                "■スキル・経験\n・Java: 3ヶ月\n■直近のPJ経験\n"
                    + String.join("\n", detailLine, detailLine, detailLine, detailLine));
      }

      /**
       * 内訳の1行でも収まらない場合に、内訳の全行と見出しを落とし、スキルの行だけを残すことを確かめる。 見出しだけが残ると、内容の無い見出しが要約に出るためである。
       *
       * @given スキル "Java" の案件が1件で、内容が1000文字
       * @when toSummaryText を実行する
       * @then 内訳の見出しを含まず、スキルの見出しと行だけの要約を返す
       */
      @Test
      void 内訳の1行でも収まらない場合_内訳と見出しを落としスキルだけを返す() {
        SkillsheetInfoSchema schema =
            schemaOf(project("2024年4月", "2024年6月", "あ".repeat(1000), "Java"));

        assertThat(schema.toSummaryText()).isEqualTo("■スキル・経験\n・Java: 3ヶ月");
      }

      /**
       * スキルの行だけで1000文字を超える場合に、内訳を全て落とし、スキルを末尾から1行ずつ落とすことを確かめる。
       * 行の途中で切ると文が壊れるため、行単位で収めるという仕様を守るためである。
       *
       * @given 40件の案件があり、それぞれ異なる30文字のスキル名を1つずつ持つ
       * @when toSummaryText を実行する
       * @then 内訳を含まず、先頭から26行のスキルだけを改行で連結した要約を返す
       */
      @Test
      void スキルの行だけで超える場合_内訳を落としたスキルだけの要約を返す() {
        SkillsheetInfoSchema.ProjectExperience[] projects =
            new SkillsheetInfoSchema.ProjectExperience[40];
        for (int i = 0; i < projects.length; i++) {
          projects[i] = project("2024年4月", "2024年6月", SHORT_PERSPECTIVE, skillName(i));
        }
        List<String> expectedLines = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
          expectedLines.add("・" + skillName(i) + ": 3ヶ月");
        }

        assertThat(schemaOf(projects).toSummaryText())
            .isEqualTo("■スキル・経験\n" + String.join("\n", expectedLines));
      }
    }
  }

  /**
   * 0年1月からの通し番号の月を、2000年の1月から数えた年月の文字列にする.
   *
   * @param monthOffset 2000年1月からの月数
   * @return YYYY年M月 形式の文字列
   */
  private static String monthText(final int monthOffset) {
    return (2000 + monthOffset / 12) + "年" + (monthOffset % 12 + 1) + "月";
  }

  /**
   * 30文字のスキル名を作る.
   *
   * @param index 案件の番号。スキル名の区別に使う
   * @return 30文字のスキル名
   */
  private static String skillName(final int index) {
    return String.format("S%02d", index + 1) + "あ".repeat(27);
  }

  /**
   * 複数の案件を持つスキルシートを作る.
   *
   * @param projects 案件の並び
   * @return スキルシート
   */
  private static SkillsheetInfoSchema schemaOf(
      final SkillsheetInfoSchema.ProjectExperience... projects) {
    SkillsheetInfoSchema schema = new SkillsheetInfoSchema();
    schema.setProjectExperiences(List.of(projects));
    return schema;
  }

  /**
   * AI が抽出した案件を1件作る.
   *
   * @param startMonth 開始年月
   * @param endMonth 終了年月
   * @param perspective 内容
   * @param skills 使用スキル
   * @return 案件
   */
  private static SkillsheetInfoSchema.ProjectExperience project(
      final String startMonth,
      final String endMonth,
      final String perspective,
      final String... skills) {
    SkillsheetInfoSchema.ProjectExperience project = new SkillsheetInfoSchema.ProjectExperience();
    project.setStartMonth(startMonth);
    project.setEndMonth(endMonth);
    project.setPerspective(perspective);
    project.setSkills(List.of(skills));
    return project;
  }
}
