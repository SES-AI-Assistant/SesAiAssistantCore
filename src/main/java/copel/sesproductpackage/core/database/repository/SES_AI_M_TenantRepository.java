package copel.sesproductpackage.core.database.repository;

import copel.sesproductpackage.core.database.SES_AI_M_TENANT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * テナント情報マスタテーブルのリポジトリ.
 *
 * <p>Spring Data JPA により基本的な CRUD 操作（findById, findAll, save, delete など）が 自動生成されます。
 *
 * @author Copel Co., Ltd.
 */
@Repository
public interface SES_AI_M_TenantRepository extends JpaRepository<SES_AI_M_TENANT, String> {
  // 基本的な CRUD 操作は自動生成される
  // 追加の検索メソッドが必要な場合はここに記載する
}
