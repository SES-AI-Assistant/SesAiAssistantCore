package copel.sesproductpackage.core.database.repository;

import copel.sesproductpackage.core.database.SES_AI_WEBAPP_M_USER;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * システムユーザーマスタテーブルのリポジトリ.
 *
 * <p>Spring Data JPA により基本的な CRUD 操作（findById, findAll, save, delete など）が 自動生成されます。
 *
 * @author Copel Co., Ltd.
 */
@Repository
public interface SES_AI_WEBAPP_M_UserRepository
    extends JpaRepository<SES_AI_WEBAPP_M_USER, String> {

  /**
   * テナント指定でのユーザー検索.
   *
   * @param userId ユーザーID
   * @param tenantId テナントID
   * @return SES_AI_WEBAPP_M_USER（存在しない場合は empty）
   */
  Optional<SES_AI_WEBAPP_M_USER> findByUserIdAndTenantId(String userId, String tenantId);

  /**
   * テナント内の全ユーザー取得.
   *
   * @param tenantId テナントID
   * @return ユーザーリスト
   */
  List<SES_AI_WEBAPP_M_USER> findAllByTenantId(String tenantId);
}
