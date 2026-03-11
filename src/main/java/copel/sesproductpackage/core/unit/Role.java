package copel.sesproductpackage.core.unit;

import java.util.EnumSet;
import java.util.Set;

/**
 * ロール.
 *
 * @author 鈴木一矢
 */
public enum Role {
  システム利用不可("00", EnumSet.noneOf(Permission.class)),
  システムユーザー("10", EnumSet.of(Permission.MANAGE_WATCH)),
  開発("20", EnumSet.of(Permission.MANAGE_WATCH)),
  運用("30", EnumSet.of(Permission.MANAGE_WATCH)),
  システム管理者("99", EnumSet.allOf(Permission.class));

  /** コード値. */
  private String code;
  /** 権限セット. */
  private final Set<Permission> permissions;

  /**
   * コンストラクタ.
   *
   * @param code        コード値.
   * @param permissions 権限セット.
   */
  Role(final String code, final Set<Permission> permissions) {
    this.code = code;
    this.permissions = permissions;
  }

  /**
   * コード値からEnumを取得します.
   *
   * @param code コード値
   * @return マッチング状態区分Enum
   */
  public static Role getEnum(final String code) {
    for (final Role status : values()) {
      if (status.code.equals(code)) {
        return status;
      }
    }
    return null;
  }

  /**
   * システム利用権限があるかどうかを判定します.
   *
   * @return 利用権限があればtrue、なければfalse
   */
  public boolean isSystemUseAuth() {
    return this.compareTo(システム利用不可) > 0;
  }

  /**
   * コード値を返却します.
   *
   * @return コード値
   */
  public String getCode() {
    return code;
  }

  /**
   * コード値をセットする.
   *
   * @param code コード値
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * 権限セットを返却します.
   *
   * @return 権限セット
   */
  public Set<Permission> getPermissions() {
    return permissions;
  }
}
