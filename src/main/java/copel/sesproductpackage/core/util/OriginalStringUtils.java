package copel.sesproductpackage.core.util;

/**
 * 文字列操作ユーティリティクラス.
 *
 * @author 鈴木一矢
 *
 */
public class OriginalStringUtils {
    /**
     * 引数の文字列が空文字またはNULLであるかどうかを判定する.
     *
     * @param str 文字列
     * @return 空であればtrue、それ以外はfalse
     */
    public static boolean isEmpty(final String str) {
        return (str != null) ? (str.isBlank() || str.isEmpty() || str.length() == 0) : (str == null || "".equals(str));
    }
}
