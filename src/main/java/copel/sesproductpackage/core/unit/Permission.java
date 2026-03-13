package copel.sesproductpackage.core.unit;

/**
 * システム内の各機能の利用権限を定義するEnum.
 *
 * @author Copel Co., Ltd.
 */
public enum Permission {
  // 画面閲覧系
  VIEW_MATCHING_LIST("マッチング一覧閲覧"),
  VIEW_SKILLS_SHEET_LIST("スキルシート一覧閲覧"),
  VIEW_JOB_LIST("案件一覧閲覧"),
  // アクション系
  DOWNLOAD_SKILLSHEET("スキルシートダウンロード"),
  EXEC_AI_MATCHING("AIマッチング実行"),
  MANAGE_WATCH("ウォッチ管理"),
  // 実績・ログ系
  VIEW_USAGE_HISTORY("利用履歴閲覧"),
  // 管理系
  MANAGE_USERS("ユーザー管理"),
  MANAGE_MASTER_DATA("マスターデータ管理");

  /** 名称. */
  private final String name;

  /**
   * コンストラクタ.
   *
   * @param name 名称
   */
  Permission(final String name) {
    this.name = name;
  }

  /**
   * 名称を返却します.
   *
   * @return 名称
   */
  public String getName() {
    return name;
  }
}
