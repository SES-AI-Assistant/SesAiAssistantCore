package copel.sesproductpackage.core.unit;

/**
 * システム内の各機能の利用権限を定義するEnum.
 *
 * @author Copel Co., Ltd.
 */
public enum Permission {
  // 画面閲覧系
  VIEW_MATCHING_LIST("マッチング一覧閲覧"),
  VIEW_JOB_LIST("案件一覧閲覧"),
  // アクション系
  DOWNLOAD_SKILLSHEET("スキルシートダウンロード"),
  EXEC_AI_MATCHING("AIマッチング実行"),
  /** 案件・要員登録画面の「一覧からインポート」結果をシステムに登録する（プレミアム以上かつ一般ロール以上で付与）. */
  REGISTER_INFO_LIST_IMPORT("一覧から案件・要員登録"),
  MANAGE_WATCH("ウォッチ管理"),
  /** 送信者マスタの管理（送信者マスタの参照・登録・更新・削除）. */
  MANAGE_SENDER("送信者マスタ管理"),
  /** 送信元グループマスタの管理（参照・登録・更新・削除）. */
  MANAGE_FROM_GROUP("送信元グループマスタ管理"),
  // 実績・ログ系
  VIEW_USAGE_HISTORY("利用履歴閲覧"),
  // 管理系
  MANAGE_USERS("ユーザー管理"),
  MANAGE_MASTER_DATA("マスターデータ管理"),
  /** テナント設定の管理（自テナントのマッチング強度の参照・変更）. */
  MANAGE_TENANT_SETTINGS("テナント設定管理");

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
