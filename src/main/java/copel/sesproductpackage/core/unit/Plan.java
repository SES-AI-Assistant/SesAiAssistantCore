package copel.sesproductpackage.core.unit;

import java.util.EnumSet;
import java.util.Set;

/**
 * プラン.
 *
 * @author Copel Co., Ltd.
 */
public enum Plan {
  FREE(
      "00",
      "フリープラン",
      EnumSet.of(
          Permission.VIEW_MATCHING_LIST,
          Permission.VIEW_JOB_LIST,
          Permission.VIEW_SKILLS_SHEET_LIST,
          Permission.MANAGE_WATCH)),
  PREMIUM("10", "プレミアムプラン", EnumSet.allOf(Permission.class));

  /** コード値. */
  private final String code;

  /** 名称. */
  private final String name;

  /** 権限セット. */
  private final Set<Permission> permissions;

  /**
   * コンストラクタ.
   *
   * @param code コード値
   * @param name 名称
   * @param permissions 権限セット
   */
  Plan(final String code, final String name, final Set<Permission> permissions) {
    this.code = code;
    this.name = name;
    this.permissions = permissions;
  }

  /**
   * コード値からEnumを取得します.
   *
   * @param code コード値
   * @return プランEnum
   */
  public static Plan getEnum(final String code) {
    for (final Plan plan : values()) {
      if (plan.code.equals(code)) {
        return plan;
      }
    }
    return FREE; // デフォルトはFREE
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
   * 名称を返却します.
   *
   * @return 名称
   */
  public String getName() {
    return name;
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
