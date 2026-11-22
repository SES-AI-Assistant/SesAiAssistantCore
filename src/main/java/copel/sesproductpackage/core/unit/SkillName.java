package copel.sesproductpackage.core.unit;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * スキル名を表す値オブジェクト. 表示名と、同一判定に使うキーを持つ。
 *
 * <p>同じスキルの表記ゆれ（全角と半角、大文字と小文字、前後の空白）を同一とみなすため、 同一判定のキーは Unicode 正規化（NFKC）の後で小文字にしたものにする。Java8 と
 * Java11 は別のスキルとして扱う。
 *
 * @author Copel Co., Ltd.
 */
public final class SkillName {

  /** 表示名。前後の空白を除いた、最初に出てきた表記。 */
  private final String displayName;

  /** 同一判定のキー。全角英数を半角にし、小文字にしたもの。 */
  private final String key;

  /**
   * コンストラクタ.
   *
   * @param displayName 表示名
   * @param key 同一判定のキー
   */
  private SkillName(final String displayName, final String key) {
    this.displayName = displayName;
    this.key = key;
  }

  /**
   * スキル名の文字列からスキル名を作ります.
   *
   * <p>null や空白だけの文字列は、スキルとして扱えないため空を返す。
   *
   * @param raw スキル名の文字列（AIが抽出したままの表記）
   * @return スキル名。スキルとして扱えない場合は空
   */
  public static Optional<SkillName> of(final String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String key = Normalizer.normalize(raw, Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT);
    if (key.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new SkillName(raw.strip(), key));
  }

  /**
   * 表示名を返します.
   *
   * <p>表示名は前後の空白を除いた最初の表記のままにし、全角と半角の統一はしない。
   *
   * @return 表示名
   */
  public String displayName() {
    return this.displayName;
  }

  /**
   * 同一判定のキーを返します.
   *
   * @return 同一判定のキー
   */
  public String key() {
    return this.key;
  }
}
