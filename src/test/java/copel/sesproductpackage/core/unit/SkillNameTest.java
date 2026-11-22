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
    }
  }
}
