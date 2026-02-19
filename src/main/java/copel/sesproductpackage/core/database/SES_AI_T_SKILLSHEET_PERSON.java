package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.SES_AI_T_EntityBase;
import lombok.Data;

/**
 * 【Entityクラス】
 * スキルシート情報と要員情報を結合したEntityクラス.
 * スキルシートの要約と要員の要約を保持する.
 *
 * @author
 *
 */
@Data
@lombok.EqualsAndHashCode(callSuper = false)
public class SES_AI_T_SKILLSHEET_PERSON extends SES_AI_T_EntityBase {

    /**
     * ファイルID / file_id
     */
	@Column(
        physicalName = "file_id",
        logicalName = "ファイルID")
    private String fileId;

    /**
     * スキルシートの要約 / file_content_summary
     */
	@Column(
        physicalName = "file_summary",
        logicalName = "スキルシートの要約")
    private String fileContentSummary;

    /**
     * 要員ID / person_id
     */
	@Column(
        physicalName = "person_id",
        logicalName = "要員ID")
    private String personId;

    /**
     * 要約 / content_summary
     */
	@Column(
        physicalName = "content_summary",
        logicalName = "要約")
    private String contentSummary;

    /**
     * コンストラクタ.
     */
    public SES_AI_T_SKILLSHEET_PERSON() {
        super();
    }

    // ================================
    // Overrideメソッド (EntityBaseの抽象メソッド実装)
    // ================================

    // このEntityはJOIN用のため、単体でのINSERT/UPDATE/DELETE/SELECT等はサポートしない想定だが、
    // EntityBaseを継承しているためダミー実装または例外スローが必要であれば実装する。
    // 今回は使用しないため空実装またはfalseを返す。

    @Override
    public void embedding(copel.sesproductpackage.core.api.gpt.Transformer embeddingProcessListener)
            throws java.io.IOException, RuntimeException {
        // 何もしない
    }

    @Override
    public boolean uniqueCheck(java.sql.Connection connection, double similarityThreshold)
            throws java.sql.SQLException {
        return false;
    }

    @Override
    public int insert(java.sql.Connection connection) throws java.sql.SQLException {
        return 0;
    }

    @Override
    public void selectByPk(java.sql.Connection connection) throws java.sql.SQLException {
        // 何もしない
    }

    @Override
    public boolean updateByPk(java.sql.Connection connection) throws java.sql.SQLException {
        return false;
    }

    @Override
    public boolean deleteByPk(java.sql.Connection connection) throws java.sql.SQLException {
        return false;
    }
}
