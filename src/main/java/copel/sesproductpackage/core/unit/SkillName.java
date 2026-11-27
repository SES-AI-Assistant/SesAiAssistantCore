package copel.sesproductpackage.core.unit;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * スキル名を表す値オブジェクト. 表示名と、同一のスキルであるかの判定に使うキーを持つ。
 *
 * <p>同じスキルの表記のゆれ（全角と半角、大文字と小文字、前後の空白、途中の空白、英字と英字の間のドット）を同じものとして扱うため、判定用のキーは Unicode
 * の正規化（NFKC）、前後の空白の除去、小文字への変換を順に行ったうえで、途中の空白と英字の間のドットを取り除いたものにする。数字の間のドット（Java 1.8 と Java 18
 * など）や、先頭のドット（.NET など）、他の記号（C# や C++ など）は残して別のスキルとして扱う。
 *
 * @author Copel Co., Ltd.
 */
public final class SkillName {

  /** 文字列に含まれる空白文字に一致する正規表現。 */
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s\\p{Zs}]+");

  /** 英字（a-z）と英字（a-z）の間にあるドットに一致する正規表現。 */
  private static final Pattern ALPHA_DOT_PATTERN = Pattern.compile("(?<=[a-z])\\.(?=[a-z])");

  /** 表示名。前後の空白を除いた、最初に出てきた表記。 */
  private final String displayName;

  /** 同一のスキルであるかの判定に使うキー。正規化と小文字への変換を行い、空白と英字の間のドットを取り除いたもの。 */
  private final String key;

  /**
   * コンストラクタ.
   *
   * @param displayName 表示名
   * @param key 判定に使うキー
   */
  private SkillName(final String displayName, final String key) {
    this.displayName = displayName;
    this.key = key;
  }

  /**
   * スキル名の文字列からスキル名を作る.
   *
   * <p>null や空白だけの文字列は、スキルとして扱えないため空を返す。判定用のキーには、NFKC
   * による正規化、前後の空白の除去、小文字への変換の後に、文字列の途中にある空白と、英字（a-z）と英字（a-z）の間にあるドットを取り除いた値を使う。
   *
   * @param raw スキル名の文字列（AIが抽出したままの表記）
   * @return スキル名。スキルとして扱えない場合は空
   */
  public static Optional<SkillName> of(final String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String normalized =
        Normalizer.normalize(raw, Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT);
    String withoutSpaces = WHITESPACE_PATTERN.matcher(normalized).replaceAll("");
    String key = ALPHA_DOT_PATTERN.matcher(withoutSpaces).replaceAll("");
    if (key.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new SkillName(raw.strip(), key));
  }

  /**
   * 表示名を返す.
   *
   * <p>表示名は前後の空白を除いた最初の表記のままにし、全角と半角の統一はしない。
   *
   * @return 表示名
   */
  public String displayName() {
    return this.displayName;
  }

  /**
   * 同一のスキルであるかの判定に使うキーを返す.
   *
   * @return 判定に使うキー
   */
  public String key() {
    return this.key;
  }
}
