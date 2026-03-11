package copel.sesproductpackage.core.unit;

/**
 * システム内の各機能の利用権限を定義するEnum.
 *
 * @author antigravity
 */
public enum Permission {
    // 画面閲覧系
    VIEW_MATCHING_LIST,
    VIEW_SKILLS_SHEET_LIST,
    VIEW_JOB_LIST,
    // アクション系
    DOWNLOAD_SKILLSHEET,
    EXEC_AI_MATCHING,
    MANAGE_WATCH,
    // 実績・ログ系
    VIEW_USAGE_HISTORY,
    // 管理系
    MANAGE_USERS,
    MANAGE_MASTER_DATA;
}
