package copel.sesproductpackage.core.unit;

/**
 * 性別.
 *
 * @author Copel Co., Ltd.
 */
public enum Gender {
  Man,
  Woman,
  Unknown;

  public String toJapanese() {
    return switch (this) {
      case Man -> "男性";
      case Woman -> "女性";
      case Unknown -> "不明";
    };
  }
}
