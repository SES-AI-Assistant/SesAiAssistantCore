package copel.sesproductpackage.core.unit;

/**
 * ロール.
 *
 * @author 鈴木一矢
 *
 */
public enum Role {
    システム利用不可("00"),
    システムユーザー("10"),
    開発("20"),
    運用("30"),
    システム管理者("99");

    /**
     * コード値.
     */
    private String code;

    /**
     * コンストラクタ.
     *
     * @param code コード値.
     */
    Role(final String code) {
        this.code = code;
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
}
