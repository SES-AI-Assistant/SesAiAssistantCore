package copel.sesproductpackage.core.database.repository;

import copel.sesproductpackage.core.database.SES_AI_M_SENDER;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 送信者マスタテーブルのリポジトリ.
 *
 * <p>Spring Data JPA により基本的な CRUD 操作（findById, findAll, save, delete など）が 自動生成されます。
 *
 * @author Copel Co., Ltd.
 */
@Repository
public interface SES_AI_M_SenderRepository extends JpaRepository<SES_AI_M_SENDER, String> {

  /**
   * テナント指定での検索.
   *
   * @param fromId 送信者ID
   * @param tenantId テナントID
   * @return SES_AI_M_SENDER（存在しない場合は empty）
   */
  Optional<SES_AI_M_SENDER> findByFromIdAndTenantId(String fromId, String tenantId);

  /**
   * テナント内の全送信者取得.
   *
   * @param tenantId テナントID
   * @return 送信者リスト
   */
  List<SES_AI_M_SENDER> findAllByTenantId(String tenantId);
}
