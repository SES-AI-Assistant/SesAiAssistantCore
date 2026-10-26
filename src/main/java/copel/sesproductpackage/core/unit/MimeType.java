package copel.sesproductpackage.core.unit;

/**
 * MimeTypeの列挙型.
 *
 * <p>メールの Content-Type を、本文・添付ファイルの種類に対応づける。メール受信 Lambda の本文と添付の振り分けで使う。
 *
 * @author Copel Co., Ltd.
 */
public enum MimeType {
  // テキスト
  TEXT_PLAIN("text/plain"),
  TEXT_HTML("text/html"),

  // 本文コンテナ
  MULTIPART_ALTERNATIVE("multipart/alternative"),
  MULTIPART_MIXED("multipart/mixed"),
  MULTIPART_RELATED("multipart/related"),

  // 添付ファイル
  EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
  WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
  PDF("application/pdf"),
  POWERPOINT("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
  IMAGE_JPEG("image/jpeg"),
  IMAGE_PNG("image/png"),
  IMAGE_GIF("image/gif"),
  ZIP("application/zip"),
  RAR("application/vnd.rar"),
  CSV("text/csv"),

  // その他
  JSON("application/json"),
  XML("application/xml"),
  INVALID("");

  /** Content-Type の文字列. */
  private final String mimeType;

  /**
   * 種類に対応する Content-Type の文字列を持たせる.
   *
   * @param mimeType Content-Type の文字列
   */
  MimeType(final String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * Content-Type の文字列に対応するMimeTypeを取得する.
   *
   * <p>完全一致（大文字小文字を除く）を先に探し、無ければ部分一致で探す。 部分一致は、区切りの前までしか分からない Content-Type や、charset などの付加情報を持つ
   * Content-Type を判定するためである。INVALID は部分一致の対象から外す。
   *
   * @param mimeType Content-Type の文字列
   * @return 対応するMimeType。null、空文字、対応が無い場合は INVALID
   */
  public static MimeType getEnum(final String mimeType) {
    if (mimeType == null || mimeType.isEmpty()) {
      return INVALID;
    }
    // 完全一致を優先
    for (MimeType obj : MimeType.values()) {
      if (obj.mimeType.equalsIgnoreCase(mimeType)) {
        return obj;
      }
    }
    // 部分一致チェック（INVALIDは除外）
    for (MimeType obj : MimeType.values()) {
      if (obj == INVALID) {
        continue;
      }
      if (obj.mimeType.contains(mimeType) || mimeType.contains(obj.mimeType)) {
        return obj;
      }
    }
    return INVALID;
  }

  /**
   * 添付ファイルとして処理する種類か判定する.
   *
   * @return EXCEL、WORD、PDF のいずれかであればtrue
   */
  public boolean isFile() {
    return this == EXCEL || this == WORD || this == PDF;
  }

  /**
   * 本文のテキストとして処理する種類か判定する.
   *
   * @return TEXT_PLAIN、MULTIPART_ALTERNATIVE のいずれかであればtrue
   */
  public boolean isText() {
    return this == TEXT_PLAIN || this == MULTIPART_ALTERNATIVE;
  }
}
