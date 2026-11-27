package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SkillName} の単体テスト。 */
class SkillNameTest {

  @Nested
  class of {

    @Nested
    class 異常系 {

      /**
       * スキル名が null の場合は、スキルとして扱わないことを確かめる。 スキル名が無い行を集計に入れると、空の表示名のスキルが要約に出るためである。
       *
       * @given スキル名が null
       * @when of を実行する
       * @then 空を返す
       */
      @Test
      void nullの場合_空を返す() {
        assertThat(SkillName.of(null)).isEmpty();
      }

      /**
       * 空文字のスキル名は、スキルとして扱わないことを確かめる。 AIが空のスキルを返した場合に、要約へ空行を出さないためである。
       *
       * @given スキル名が空文字
       * @when of を実行する
       * @then 空を返す
       */
      @Test
      void 空文字の場合_空を返す() {
        assertThat(SkillName.of("")).isEmpty();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 全角の空白だけのスキル名は、スキルとして扱わないことを確かめる。 全角の空白は半角の空白と同じく、前後の空白として除く必要があるためである。
       *
       * @given スキル名が全角の空白だけの文字列
       * @when of を実行する
       * @then 空を返す
       */
      @Test
      void 全角の空白だけの場合_空を返す() {
        assertThat(SkillName.of("　　")).isEmpty();
      }
    }
  }

  @Nested
  class displayName {

    @Nested
    class 正常系 {

      /**
       * 前後に空白があるスキル名の表示名は、前後の空白を除いた名前になることを確かめる。 要約の行に余分な空白を出さないためである。
       *
       * @given スキル名が前後に半角の空白を持つ " Java "
       * @when displayName を実行する
       * @then "Java" を返す
       */
      @Test
      void 前後に空白がある場合_前後の空白を除いた表示名を返す() {
        assertThat(SkillName.of(" Java ").orElseThrow().displayName()).isEqualTo("Java");
      }

      /**
       * 全角の英字で書かれたスキル名の表示名は、全角のまま残すことを確かめる。 表示名は最初に出てきた表記を使う仕様で、表記を変えないためである。
       *
       * @given スキル名が全角の英字 "ＪＡＶＡ"
       * @when displayName を実行する
       * @then "ＪＡＶＡ" を返す
       */
      @Test
      void 全角の英字の場合_全角のままの表示名を返す() {
        assertThat(SkillName.of("ＪＡＶＡ").orElseThrow().displayName()).isEqualTo("ＪＡＶＡ");
      }

      /**
       * 途中に空白やドットを含むスキル名の表示名は、元の表記を残すことを確かめる。 表示名は前後の空白だけを除き、途中の空白やドットを削らないためである。
       *
       * @given スキル名が " My SQL " と " Node.js "
       * @when displayName を実行する
       * @then "My SQL" と "Node.js" を返す
       */
      @Test
      void 途中に空白やドットがある場合_元の表記のままの表示名を返す() {
        assertThat(SkillName.of(" My SQL ").orElseThrow().displayName()).isEqualTo("My SQL");
        assertThat(SkillName.of(" Node.js ").orElseThrow().displayName()).isEqualTo("Node.js");
      }
    }
  }

  @Nested
  class key {

    @Nested
    class 正常系 {

      /**
       * 全角の英字のスキル名のキーは、半角の小文字になることを確かめる。 全角と半角の表記ゆれを同じスキルとして集計するためである。
       *
       * @given スキル名が全角の英字 "ＪＡＶＡ"
       * @when key を実行する
       * @then "java" を返す
       */
      @Test
      void 全角の英字の場合_半角の小文字のキーを返す() {
        assertThat(SkillName.of("ＪＡＶＡ").orElseThrow().key()).isEqualTo("java");
      }

      /**
       * 大文字で書かれたスキル名のキーは、小文字になることを確かめる。 大文字と小文字の違いだけで、同じスキルを別々に数えないためである。
       *
       * @given スキル名が "JAVA"
       * @when key を実行する
       * @then "java" を返す
       */
      @Test
      void 大文字の場合_小文字のキーを返す() {
        assertThat(SkillName.of("JAVA").orElseThrow().key()).isEqualTo("java");
      }

      /**
       * 途中に半角や全角の空白を含むスキル名のキーは、空白を除いた小文字になることを確かめる。 MySQL と My SQL のような空白のゆれを同じスキルとして集計するためである。
       *
       * @given スキル名が "My SQL"、"my sql"、"My　SQL"、"Spring Boot"
       * @when key を実行する
       * @then 空白を除いた "mysql" と "springboot" を返す
       */
      @Test
      void 途中に空白がある場合_空白を取り除いたキーを返す() {
        assertThat(SkillName.of("My SQL").orElseThrow().key()).isEqualTo("mysql");
        assertThat(SkillName.of("my sql").orElseThrow().key()).isEqualTo("mysql");
        assertThat(SkillName.of("My　SQL").orElseThrow().key()).isEqualTo("mysql");
        assertThat(SkillName.of("Spring Boot").orElseThrow().key()).isEqualTo("springboot");
      }

      /**
       * 英字と英字の間にドットを含むスキル名のキーは、ドットを除いた小文字になることを確かめる。 NodeJS と Node.js のようなドットの有無を同じスキルとして集計するためである。
       *
       * @given スキル名が "Node.JS"、"Node.js"、"Node . js"、"VB.NET"
       * @when key を実行する
       * @then 英字の間のドットと空白を除いた "nodejs" と "vbnet" を返す
       */
      @Test
      void 英字の間のドットがある場合_ドットを取り除いたキーを返す() {
        assertThat(SkillName.of("Node.JS").orElseThrow().key()).isEqualTo("nodejs");
        assertThat(SkillName.of("Node.js").orElseThrow().key()).isEqualTo("nodejs");
        assertThat(SkillName.of("Node . js").orElseThrow().key()).isEqualTo("nodejs");
        assertThat(SkillName.of("VB.NET").orElseThrow().key()).isEqualTo("vbnet");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 数字を含むスキル名のキーは、数字をそのまま残すことを確かめる。 Java8 と Java11 を別のスキルとして区別するためである。
       *
       * @given スキル名が "Java8"
       * @when key を実行する
       * @then "java8" を返す
       */
      @Test
      void 数字を含む場合_数字を残したキーを返す() {
        assertThat(SkillName.of("Java8").orElseThrow().key()).isEqualTo("java8");
      }

      /**
       * 数字と数字の間にドットがあるスキル名のキーは、ドットを残すことを確かめる。 Java 1.8 と Java 18 が同じキーになり混ざることを防ぐためである。
       *
       * @given スキル名が "Java 1.8" と "Java 18"
       * @when key を実行する
       * @then "java1.8" と "java18" を返し、異なるキーになる
       */
      @Test
      void 数字の間のドットがある場合_ドットを残したキーを返す() {
        String java18WithDot = SkillName.of("Java 1.8").orElseThrow().key();
        String java18 = SkillName.of("Java 18").orElseThrow().key();

        assertThat(java18WithDot).isEqualTo("java1.8");
        assertThat(java18).isEqualTo("java18");
        assertThat(java18WithDot).isNotEqualTo(java18);
      }

      /**
       * 先頭にドットがあるスキル名のキーは、先頭のドットを残すことを確かめる。 .NET のように先頭のドットが名称の一部であるスキルを別のスキルと区別するためである。
       *
       * @given スキル名が ".NET" と "VB.NET"
       * @when key を実行する
       * @then ".NET" は ".net" になり、"VB.NET" の "vbnet" と異なるキーになる
       */
      @Test
      void 先頭にドットがある場合_先頭のドットを残したキーを返す() {
        assertThat(SkillName.of(".NET").orElseThrow().key()).isEqualTo(".net");
        assertThat(SkillName.of(".NET").orElseThrow().key())
            .isNotEqualTo(SkillName.of("VB.NET").orElseThrow().key());
      }

      /**
       * ドット以外の記号を含むスキル名のキーは、記号をそのまま残すことを確かめる。 C と C# と C++ や PL/SQL を別のスキルとして区別するためである。
       *
       * @given スキル名が "C"、"C#"、"C++"、"PL/SQL"
       * @when key を実行する
       * @then "c"、"c#"、"c++"、"pl/sql" をそれぞれ返す
       */
      @Test
      void ドット以外の記号を含む場合_記号を残したキーを返す() {
        assertThat(SkillName.of("C").orElseThrow().key()).isEqualTo("c");
        assertThat(SkillName.of("C#").orElseThrow().key()).isEqualTo("c#");
        assertThat(SkillName.of("C++").orElseThrow().key()).isEqualTo("c++");
        assertThat(SkillName.of("PL/SQL").orElseThrow().key()).isEqualTo("pl/sql");
      }
    }
  }
}
