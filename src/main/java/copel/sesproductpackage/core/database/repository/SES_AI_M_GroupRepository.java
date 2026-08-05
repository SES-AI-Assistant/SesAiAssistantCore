package copel.sesproductpackage.core.database.repository;

import copel.sesproductpackage.core.database.SES_AI_M_GROUP;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 送信元グループマスタテーブルのリポジトリ.
 *
 * <p>Spring Data JPA により基本的な CRUD 操作（findById, findAll, save, delete など）が 自動生成されます。
 *
 * @author Copel Co., Ltd.
 */
@Repository
public interface SES_AI_M_GroupRepository extends JpaRepository<SES_AI_M_GROUP, String> {

  /**
   * テナント指定での検索.
   *
   * @param fromGroup 送信元グループID
   * @param tenantId テナントID
   * @return SES_AI_M_GROUP（存在しない場合は empty）
   */
  Optional<SES_AI_M_GROUP> findByFromGroupAndTenantId(String fromGroup, String tenantId);

  /**
   * テナント内の全グループ取得.
   *
   * @param tenantId テナントID
   * @return グループリスト
   */
  List<SES_AI_M_GROUP> findAllByTenantId(String tenantId);
}
