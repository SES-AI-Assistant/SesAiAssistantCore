package copel.sesproductpackage.core.unit;

/**
 * FromId（送信者ID）ユーティリティ.
 *
 * FromIdはメールアドレスまたはLINE ID形式の送信者識別子。
 * 本クラスはFromId関連の操作（形式判定、ドメイン抽出、重複チェック）を提供する。
 *
 * @author Copel Co., Ltd.
 */
public class FromId {

  /**
   * FromIdがメールアドレス形式かを判定する.
   *
   * @param fromId FromId (メールアドレスまたはLINE ID)
   * @return メールアドレス形式ならtrue、そうでないならfalse
   */
  public static boolean isEmailFormat(String fromId) {
    return fromId != null && fromId.contains("@");
  }

  /**
   * メールアドレスからドメイン部分を抽出する.
   *
   * @param email メールアドレス
   * @return ドメイン部分（小文字）、メールアドレス形式でない場合は空文字
   */
  public static String extractDomain(String email) {
    if (email == null || !email.contains("@")) {
      return "";
    }
    return email.substring(email.indexOf("@") + 1).toLowerCase();
  }

  /**
   * 2つのFromIdが同じ送信者ID またはメールドメインが同じか判定する.
   *
   * 以下の場合に true を返す:
   * - FromId が完全一致する場合
   * - 両方がメールアドレス形式で、ドメイン部分が同じ場合
   *
   * @param fromId1 FromId1
   * @param fromId2 FromId2
   * @return 同じ送信者またはドメインが同じならtrue
   */
  public static boolean isSameSenderOrDomain(String fromId1, String fromId2) {
    if (fromId1 == null || fromId2 == null) {
      return false;
    }

    // 完全一致チェック
    if (fromId1.equals(fromId2)) {
      return true;
    }

    // メールアドレス形式のドメイン比較
    if (isEmailFormat(fromId1) && isEmailFormat(fromId2)) {
      String domain1 = extractDomain(fromId1);
      String domain2 = extractDomain(fromId2);
      return domain1.equals(domain2) && !domain1.isEmpty();
    }

    return false;
  }

  private FromId() {
    // ユーティリティクラスのため、インスタンス化を禁止
  }
}
