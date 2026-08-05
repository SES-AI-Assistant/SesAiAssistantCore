package copel.sesproductpackage.core.database.repository;

import copel.sesproductpackage.core.database.SES_AI_WEBAPP_M_NOTIFICATION;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * プッシュ通知デバイス登録マスタのリポジトリ.
 *
 * <p>Spring Data JPA により基本的な CRUD 操作（findById, findAll, save, delete など）が 自動生成されます。
 *
 * @author Copel Co., Ltd.
 */
@Repository
public interface SES_AI_WEBAPP_M_NotificationRepository
    extends JpaRepository<SES_AI_WEBAPP_M_NOTIFICATION, String> {

  /**
   * テナント指定での通知取得.
   *
   * @param notificationId 通知ID
   * @param tenantId テナントID
   * @return SES_AI_WEBAPP_M_NOTIFICATION（存在しない場合は empty）
   */
  Optional<SES_AI_WEBAPP_M_NOTIFICATION> findByNotificationIdAndTenantId(
      String notificationId, String tenantId);

  /**
   * テナント内の全通知取得.
   *
   * @param tenantId テナントID
   * @return 通知リスト
   */
  List<SES_AI_WEBAPP_M_NOTIFICATION> findAllByTenantId(String tenantId);

  /**
   * ユーザーIDで通知を検索.
   *
   * @param userId ユーザーID
   * @param tenantId テナントID
   * @return 通知リスト
   */
  List<SES_AI_WEBAPP_M_NOTIFICATION> findByUserIdAndTenantId(String userId, String tenantId);
}
