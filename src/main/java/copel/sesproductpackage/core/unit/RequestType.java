package copel.sesproductpackage.core.unit;

/**
 * リクエスト種別
 *
 * @author 鈴木一矢
 *
 */
public enum RequestType {
    LineMessage("11"),
    LineFile("12"),
    EmailMessage("21"),
    EmailFile("22"),
    OtherMessage("01"),
    OtherFile("02");

    /**
     * 種別コード値.
     */
    private String code;

    RequestType(final String code) {
        this.code = code;   
    }

    /**
     * 引数のcodeに対応するEnumを返却します.
     *
     * @param code コード値
     * @return RequestType
     */
    public static RequestType getEnum(final String code) {
        if (code == null) {
            return null;
        } else {
            switch (code) {
            case "11": 
                return LineMessage;
            case "12":
                return LineFile;
            case "21": 
                return EmailMessage;
            case "22":
                return EmailFile;
            case "01": 
                return OtherMessage;
            case "02":
                return OtherFile;
            default:
                return OtherMessage;
            }
        }
    }

    /**
     * このオブジェクトがもつcodeを返却します.
     *
     * @return コード値
     */
    public String getCode() {
        return this.code;
    }
}
