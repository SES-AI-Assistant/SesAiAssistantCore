# Phase 1: マスタテーブル JPA 実装計画

**バージョン**: 1.0  
**最終更新日**: 2026-08-05  
**ステータス**: 実装準備中

---

## 1. 概要

本ドキュメントは、SesAiAssistantCore の 5つのマスタテーブルを JPA/Hibernate に移行する具体的な実装計画である。

**対象Entity（5個）**:
1. `SES_AI_M_TENANT` - テナント情報マスタ
2. `SES_AI_M_GROUP` - 送信元グループマスタ
3. `SES_AI_M_SENDER` - 送信者マスタ
4. `SES_AI_WEBAPP_M_USER` - Webアプリユーザーマスタ
5. `SES_AI_WEBAPP_M_NOTIFICATION` - プッシュ通知デバイスマスタ

**成果物**:
- Entity クラス (JPA アノテーション追加)
- AttributeConverter (Unit値オブジェクト対応)
- pom.xml 更新（spring-boot-starter-data-jpa は削除）
- application.yml 設定追加
- Unit Test / IT1 Test

**アーキテクチャ**: EntityManager を Entity メソッド内で直接使用（Repository パターン不使用）

---

## 2. Entity 別実装設計

### 2.1. SES_AI_M_TENANT（テナント情報マスタ）

#### テーブルスキーマ
```sql
CREATE TABLE SES_AI_M_TENANT (
    tenant_id VARCHAR(36) PRIMARY KEY NOT NULL,
    tenant_name VARCHAR(100),
    tenant_status_cd VARCHAR(10),
    register_date TIMESTAMP NOT NULL,
    register_user VARCHAR(50) NOT NULL
);
```

#### Entity クラス（修正イメージ）

**修正前**:
```java
public class SES_AI_M_TENANT extends EntityBase {
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_M_TENANT (tenant_id, tenant_name, tenant_status_cd, register_date, register_user) VALUES (?, ?, ?, ?, ?)";
  
  @Column(physicalName = "tenant_name")
  private String tenantName;
  
  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsertWithoutTenantFilter(...);
  }
}
```

**修正後**:
```java
@Entity
@Table(name = "SES_AI_M_TENANT")
public class SES_AI_M_TENANT extends EntityBase {

  @Id
  private String tenantId;

  @Column(name = "tenant_name")
  private String tenantName;

  @Column(name = "tenant_status_cd")
  private String tenantStatusCd;

  @Column(name = "register_date")
  @Convert(converter = OriginalDateTimeConverter.class)
  private OriginalDateTime registerDate;

  @Column(name = "register_user")
  private String registerUser;

  // コンストラクタ（既存のまま）
  public SES_AI_M_TENANT(String tenantId) {
    super(tenantId);
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    try {
      EntityManager entityManager = getEntityManager();
      entityManager.persist(this);
      return 1;
    } catch (Exception e) {
      throw new SQLException("Insert failed", e);
    }
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return;
    }
    try {
      EntityManager entityManager = getEntityManager();
      SES_AI_M_TENANT result = entityManager.find(SES_AI_M_TENANT.class, this.tenantId);
      if (result != null) {
        this.tenantName = result.getTenantName();
        this.registerDate = result.getRegisterDate();
        this.registerUser = result.getRegisterUser();
      }
    } catch (Exception e) {
      throw new SQLException("Select failed", e);
    }
  }

  // 同様に updateByPk(), deleteByPk() を実装
}
```


#### マッピング確認表

| 物理名 | Java型 | JPA カラム定義 | 補足 |
|:---|:---|:---|:---|
| tenant_id | String | @Id (PK) | テナント識別子 |
| tenant_name | String | @Column(name = "tenant_name") | - |
| tenant_status_cd | String | @Column(name = "tenant_status_cd") | - |
| register_date | OriginalDateTime | @Column + @Convert(OriginalDateTimeConverter.class) | Unit値オブジェクト |
| register_user | String | @Column(name = "register_user") | - |

---

### 2.2. SES_AI_M_GROUP（送信元グループマスタ）

#### テーブルスキーマ
```sql
CREATE TABLE SES_AI_M_GROUP (
    from_group VARCHAR(100) PRIMARY KEY NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    group_name VARCHAR(100),
    register_date TIMESTAMP NOT NULL,
    register_user VARCHAR(50) NOT NULL,
    CONSTRAINT fk_group_tenant FOREIGN KEY (tenant_id) REFERENCES SES_AI_M_TENANT(tenant_id)
);
```

#### Entity クラス（修正イメージ）

```java
@Entity
@Table(name = "SES_AI_M_GROUP")
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = "string")
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SES_AI_M_GROUP extends EntityBase {

  @Id
  @Column(name = "from_group")
  private String fromGroup;

  @Column(name = "group_name")
  private String groupName;

  @Column(name = "register_date")
  @Convert(converter = OriginalDateTimeConverter.class)
  private OriginalDateTime registerDate;

  @Column(name = "register_user")
  private String registerUser;

  @Column(name = "tenant_id", insertable = false, updatable = false)
  private String tenantId; // EntityBase からも継承

  public SES_AI_M_GROUP(String tenantId) {
    super(tenantId);
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    try {
      EntityManager entityManager = getEntityManager();
      entityManager.persist(this);
      return 1;
    } catch (Exception e) {
      throw new SQLException("Insert failed", e);
    }
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.fromGroup == null) {
      return;
    }
    try {
      EntityManager entityManager = getEntityManager();
      Session session = entityManager.unwrap(Session.class);
      session.enableFilter("tenantFilter").setParameter("tenantId", this.tenantId);
      
      try {
        SES_AI_M_GROUP result = entityManager.find(SES_AI_M_GROUP.class, this.fromGroup);
        if (result != null) {
          this.groupName = result.getGroupName();
          this.registerDate = result.getRegisterDate();
          this.registerUser = result.getRegisterUser();
        }
      } finally {
        session.disableFilter("tenantFilter");
      }
    } catch (Exception e) {
      throw new SQLException("Select failed", e);
    }
  }

  // 同様に updateByPk(), deleteByPk() を実装
}
```

#### マッピング確認表

| 物理名 | Java型 | JPA カラム定義 | 補足 |
|:---|:---|:---|:---|
| from_group | String | @Id (PK) | 送信元グループID |
| tenant_id | String | @Column(name = "tenant_id", insertable=false, updatable=false) | テナント隔離対象カラム |
| group_name | String | @Column(name = "group_name") | - |
| register_date | OriginalDateTime | @Column + @Convert | Unit値オブジェクト |
| register_user | String | @Column(name = "register_user") | - |

---

### 2.3. SES_AI_M_SENDER（送信者マスタ）

#### テーブルスキーマ
```sql
CREATE TABLE SES_AI_M_SENDER (
    from_id VARCHAR(50) PRIMARY KEY NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    from_name VARCHAR(50),
    register_date TIMESTAMP NOT NULL,
    register_user VARCHAR(50) NOT NULL,
    CONSTRAINT fk_sender_tenant FOREIGN KEY (tenant_id) REFERENCES SES_AI_M_TENANT(tenant_id)
);
```

#### Entity クラス（修正イメージ）

```java
@Entity
@Table(name = "SES_AI_M_SENDER")
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = "string")
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SES_AI_M_SENDER extends EntityBase {

  @Id
  @Column(name = "from_id")
  private String fromId;

  @Column(name = "from_name")
  private String fromName;

  @Column(name = "register_date")
  @Convert(converter = OriginalDateTimeConverter.class)
  private OriginalDateTime registerDate;

  @Column(name = "register_user")
  private String registerUser;

  @Column(name = "tenant_id", insertable = false, updatable = false)
  private String tenantId;

  public SES_AI_M_SENDER(String tenantId) {
    super(tenantId);
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    try {
      EntityManager entityManager = getEntityManager();
      entityManager.persist(this);
      return 1;
    } catch (Exception e) {
      throw new SQLException("Insert failed", e);
    }
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    // 実装省略
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    // 実装省略
    return false;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    // 実装省略
    return false;
  }

  /**
   * 送信者マスタに該当IDの送信者が存在するかを判定します.
   *
   * @param connection DB接続
   * @return 存在する場合 true
   * @throws SQLException SQL実行エラー
   */
  public boolean isExist(Connection connection) throws SQLException {
    try {
      EntityManager entityManager = getEntityManager();
      SES_AI_M_SENDER result = entityManager.find(SES_AI_M_SENDER.class, this.fromId);
      return result != null;
    } catch (Exception e) {
      throw new SQLException("Exists check failed", e);
    }
  }
}
```

#### マッピング確認表

| 物理名 | Java型 | JPA カラム定義 | 補足 |
|:---|:---|:---|:---|
| from_id | String | @Id (PK) | 送信者ID |
| tenant_id | String | @Column(name = "tenant_id", insertable=false, updatable=false) | テナント隔離対象 |
| from_name | String | @Column(name = "from_name") | - |
| register_date | OriginalDateTime | @Column + @Convert | Unit値オブジェクト |
| register_user | String | @Column(name = "register_user") | - |

---

### 2.4. SES_AI_WEBAPP_M_USER（Webアプリユーザーマスタ）

#### テーブルスキーマ
```sql
CREATE TABLE SES_AI_WEBAPP_M_USER (
    user_id VARCHAR(50) PRIMARY KEY NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    user_name VARCHAR(100),
    role_cd VARCHAR(10),
    plan_cd VARCHAR(10),
    register_date TIMESTAMP NOT NULL,
    register_user VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES SES_AI_M_TENANT(tenant_id)
);
```

#### Entity クラス（修正イメージ）

```java
@Entity
@Table(name = "SES_AI_WEBAPP_M_USER")
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = "string")
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SES_AI_WEBAPP_M_USER extends EntityBase {

  @Id
  @Column(name = "user_id")
  private String userId;

  @Column(name = "user_name")
  private String userName;

  @Column(name = "role_cd")
  @Enumerated(EnumType.STRING)
  private Role role;

  @Column(name = "plan_cd")
  @Enumerated(EnumType.STRING)
  private Plan plan;

  @Column(name = "register_date")
  @Convert(converter = OriginalDateTimeConverter.class)
  private OriginalDateTime registerDate;

  @Column(name = "register_user")
  private String registerUser;

  @Column(name = "tenant_id", insertable = false, updatable = false)
  private String tenantId;

  // テナントIDを後から設定できるコンストラクタ
  public SES_AI_WEBAPP_M_USER() {
    super("_temp_"); // 仮のテナントID
  }

  public SES_AI_WEBAPP_M_USER(String tenantId) {
    super(tenantId);
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    try {
      EntityManager entityManager = getEntityManager();
      entityManager.persist(this);
      return 1;
    } catch (Exception e) {
      throw new SQLException("Insert failed", e);
    }
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.userId == null) {
      return;
    }
    try {
      EntityManager entityManager = getEntityManager();
      Session session = entityManager.unwrap(Session.class);
      session.enableFilter("tenantFilter").setParameter("tenantId", this.tenantId);
      
      try {
        SES_AI_WEBAPP_M_USER result = entityManager.find(SES_AI_WEBAPP_M_USER.class, this.userId);
        if (result != null) {
          this.userName = result.getUserName();
          this.role = result.getRole();
          this.plan = result.getPlan();
          this.registerDate = result.getRegisterDate();
          this.registerUser = result.getRegisterUser();
        }
      } finally {
        session.disableFilter("tenantFilter");
      }
    } catch (Exception e) {
      throw new SQLException("Select failed", e);
    }
  }

  /**
   * ユーザーをユーザーID で取得（テナントID条件なし、システム管理者用）.
   *
   * @param connection DBコネクション
   * @throws SQLException SQL実行エラー
   */
  public void selectByPkWithoutTenantId(Connection connection) throws SQLException {
    if (this.userId == null) {
      return;
    }
    try {
      EntityManager entityManager = getEntityManager();
      SES_AI_WEBAPP_M_USER result = entityManager.find(SES_AI_WEBAPP_M_USER.class, this.userId);
      if (result != null) {
        this.userName = result.getUserName();
        this.role = result.getRole();
        this.plan = result.getPlan();
        this.registerDate = result.getRegisterDate();
        this.registerUser = result.getRegisterUser();
      }
    } catch (Exception e) {
      throw new SQLException("Select failed", e);
    }
  }

  // 同様に updateByPk(), updateByPkWithoutTenantId(),
  // deleteByPk(), deleteByPkWithoutTenantIdFilter() を実装
  
  // ビジネスロジック（既存のまま）
  public boolean hasSystemUseAuth() {
    return this.role != null && this.role.isSystemUseAuth();
  }

  public Set<Permission> getPermissions() {
    Set<Permission> permissions = new HashSet<>();
    if (this.role != null) {
      permissions.addAll(this.role.getPermissions());
    }
    if (this.plan != null) {
      permissions.addAll(this.plan.getPermissions());
    }
    if (!isEligibleForRegisterInfoListImport()) {
      permissions.remove(Permission.REGISTER_INFO_LIST_IMPORT);
    }
    return permissions;
  }

  private boolean isEligibleForRegisterInfoListImport() {
    if (this.plan == null || this.plan == Plan.FREE) {
      return false;
    }
    if (this.role == null) {
      return false;
    }
    try {
      int roleNum = Integer.parseInt(this.role.getCode(), 10);
      int generalMin = Integer.parseInt(Role.システムユーザー.getCode(), 10);
      return roleNum >= generalMin;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
```

#### マッピング確認表

| 物理名 | Java型 | JPA カラム定義 | 補足 |
|:---|:---|:---|:---|
| user_id | String | @Id (PK) | ユーザーID |
| tenant_id | String | @Column(name = "tenant_id", insertable=false, updatable=false) | テナント隔離対象 |
| user_name | String | @Column(name = "user_name") | - |
| role_cd | Role (Enum) | @Column + @Enumerated(STRING) | Role Enum |
| plan_cd | Plan (Enum) | @Column + @Enumerated(STRING) | Plan Enum |
| register_date | OriginalDateTime | @Column + @Convert | Unit値オブジェクト |
| register_user | String | @Column(name = "register_user") | - |

---

### 2.5. SES_AI_WEBAPP_M_NOTIFICATION（プッシュ通知デバイスマスタ）

#### テーブルスキーマ
```sql
CREATE TABLE SES_AI_WEBAPP_M_NOTIFICATION (
    notification_id VARCHAR(50) PRIMARY KEY NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    device_type VARCHAR(20),
    device_name VARCHAR(100),
    push_notification_endpoint VARCHAR(500),
    p256dh VARCHAR(200),
    auth VARCHAR(200),
    enabled BOOLEAN DEFAULT FALSE,
    notify_all_match BOOLEAN DEFAULT FALSE,
    register_date TIMESTAMP NOT NULL,
    register_user VARCHAR(50) NOT NULL,
    CONSTRAINT fk_notification_tenant FOREIGN KEY (tenant_id) REFERENCES SES_AI_M_TENANT(tenant_id)
);
```

#### Entity クラス（修正イメージ）

```java
@Entity
@Table(name = "SES_AI_WEBAPP_M_NOTIFICATION")
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = "string")
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SES_AI_WEBAPP_M_NOTIFICATION extends EntityBase {

  @Id
  @Column(name = "notification_id")
  private String notificationId;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "device_type", nullable = false)
  private String deviceType;

  @Column(name = "device_name")
  private String deviceName;

  @Column(name = "push_notification_endpoint", nullable = false)
  private String pushNotificationEndpoint;

  @Column(name = "p256dh", nullable = false)
  private String p256dh;

  @Column(name = "auth", nullable = false)
  private String auth;

  @Column(name = "enabled")
  private Boolean enabled;

  @Column(name = "notify_all_match")
  private Boolean notifyAllMatch;

  @Column(name = "register_date")
  @Convert(converter = OriginalDateTimeConverter.class)
  private OriginalDateTime registerDate;

  @Column(name = "register_user")
  private String registerUser;

  @Column(name = "tenant_id", insertable = false, updatable = false)
  private String tenantId;

  public SES_AI_WEBAPP_M_NOTIFICATION(String tenantId) {
    super(tenantId);
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    try {
      EntityManager entityManager = getEntityManager();
      entityManager.persist(this);
      return 1;
    } catch (Exception e) {
      throw new SQLException("Insert failed", e);
    }
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.notificationId == null) {
      return;
    }
    try {
      EntityManager entityManager = getEntityManager();
      SES_AI_WEBAPP_M_NOTIFICATION result = entityManager.find(SES_AI_WEBAPP_M_NOTIFICATION.class, this.notificationId);
      if (result != null) {
        this.userId = result.getUserId();
        this.deviceType = result.getDeviceType();
        this.deviceName = result.getDeviceName();
        this.pushNotificationEndpoint = result.getPushNotificationEndpoint();
        this.enabled = result.getEnabled();
        this.notifyAllMatch = result.getNotifyAllMatch();
        this.registerDate = result.getRegisterDate();
        this.registerUser = result.getRegisterUser();
      }
    } catch (Exception e) {
      throw new SQLException("Select failed", e);
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.notificationId == null) {
      return false;
    }
    try {
      EntityManager entityManager = getEntityManager();
      entityManager.merge(this);
      return true;
    } catch (Exception e) {
      throw new SQLException("Update failed", e);
    }
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.notificationId == null) {
      return false;
    }
    try {
      EntityManager entityManager = getEntityManager();
      SES_AI_WEBAPP_M_NOTIFICATION result = entityManager.find(SES_AI_WEBAPP_M_NOTIFICATION.class, this.notificationId);
      if (result != null) {
        entityManager.remove(result);
        return true;
      }
      return false;
    } catch (Exception e) {
      throw new SQLException("Delete failed", e);
    }
  }
}
```

#### マッピング確認表

| 物理名 | Java型 | JPA カラム定義 | 補足 |
|:---|:---|:---|:---|
| notification_id | String | @Id (PK) | 通知デバイスID |
| tenant_id | String | @Column(insertable=false, updatable=false) | テナント隔離対象 |
| user_id | String | @Column(nullable=false) | ユーザーID |
| device_type | String | @Column(nullable=false) | デバイスタイプ |
| device_name | String | @Column | - |
| push_notification_endpoint | String | @Column(nullable=false) | Push APIエンドポイント |
| p256dh | String | @Column(nullable=false) | ECDH公開鍵 |
| auth | String | @Column(nullable=false) | HMACトークン |
| enabled | Boolean | @Column | 有効フラグ |
| notify_all_match | Boolean | @Column | 全件通知フラグ |
| register_date | OriginalDateTime | @Column + @Convert | Unit値オブジェクト |
| register_user | String | @Column | - |

---

## 3. AttributeConverter の実装

### 3.1. OriginalDateTimeConverter

```java
package copel.sesproductpackage.core.converter;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;

/**
 * OriginalDateTime 型を LocalDateTime にマッピングする AttributeConverter.
 *
 * @author Copel Co., Ltd.
 */
@Converter(autoApply = true)
public class OriginalDateTimeConverter implements AttributeConverter<OriginalDateTime, LocalDateTime> {

  @Override
  public LocalDateTime convertToDatabaseColumn(OriginalDateTime attribute) {
    if (attribute == null) {
      return null;
    }
    // OriginalDateTime から LocalDateTime を取得
    // (OriginalDateTime クラスに getValue() メソッドを追加する必要がある)
    return attribute.toLocalDateTime();
  }

  @Override
  public OriginalDateTime convertToEntityAttribute(LocalDateTime dbData) {
    if (dbData == null) {
      return null;
    }
    return new OriginalDateTime(dbData);
  }
}
```

**注**: OriginalDateTime クラスに以下のメソッドが必要
```java
public LocalDateTime toLocalDateTime() {
    return this.value; // 内部の LocalDateTime を返す
}
```

---

## 4. pom.xml 修正案

### 4.1. 削除する依存性

spring-boot-starter-data-jpa を削除：

```xml
<!-- 削除対象 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### 4.2. 保持する依存性

以下は既に pom.xml に含まれており、保持：

```xml
<!-- Hibernate ORM -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>6.4.4.Final</version>
</dependency>

<!-- PostgreSQL JDBC -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.5</version>
</dependency>
```

---

## 5. application.yml 設定案

### 5.1. 簡潔な設定

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL15Dialect
  datasource:
    url: jdbc:postgresql://localhost:5432/ses_ai_assistant
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
```

---

## 6. ファイル配置構成

Phase 1 実装完了時の ディレクトリ構成（修正ファイル）:

```
SesAiAssistantCore/
├── docs/
│   ├── JPA移行設計.md (修正)
│   └── Phase1_マスタテーブルJPA実装計画.md (本ファイル、修正)
│
├── src/main/java/copel/sesproductpackage/core/
│   ├── database/
│   │   ├── SES_AI_M_TENANT.java (修正)
│   │   ├── SES_AI_M_GROUP.java (修正)
│   │   ├── SES_AI_M_SENDER.java (修正)
│   │   ├── SES_AI_WEBAPP_M_USER.java (修正)
│   │   ├── SES_AI_WEBAPP_M_NOTIFICATION.java (修正)
│   │   ├── base/
│   │   │   └── EntityBase.java (修正なし)
│   │   └── repository/ (削除)
│   │
│   └── converter/ (新規ディレクトリ)
│       └── OriginalDateTimeConverter.java (新規)
│
├── src/main/resources/
│   └── application.yml (修正 JPA設定追加)
│
├── src/test/java/copel/sesproductpackage/core/
│   ├── database/
│   │   ├── SES_AI_M_TenantTest.java (新規/修正)
│   │   ├── SES_AI_M_GroupTest.java (新規/修正)
│   │   ├── SES_AI_M_SenderTest.java (新規/修正)
│   │   ├── SES_AI_WEBAPP_M_UserTest.java (新規/修正)
│   │   └── SES_AI_WEBAPP_M_NotificationTest.java (新規/修正)
│   │
│   └── converter/
│       └── OriginalDateTimeConverterTest.java (新規)
│
└── pom.xml (修正 spring-boot-starter-data-jpa削除)
```

**削除ファイル**:
- `src/main/java/copel/sesproductpackage/core/database/repository/` ディレクトリ全体

---

## 7. テスト設計

### 7.1. Unit Test 観点（各Entity ごと）

| シナリオ ID | シナリオ名 | テスト内容 | 確認項目 |
|:---|:---|:---|:---|
| UT-M-TENANT-001 | insert 成功 | Entity.insert() を呼び出し | Repository.save() が呼ばれ、return 1 |
| UT-M-TENANT-002 | selectByPk 成功 | Entity.selectByPk() を呼び出し | フィールドが Entity から設定される |
| UT-M-TENANT-003 | updateByPk 成功 | Entity.updateByPk() を呼び出し | return true |
| UT-M-TENANT-004 | deleteByPk 成功 | Entity.deleteByPk() を呼び出し | return true |
| UT-M-TENANT-005 | insert 例外ハンドリング | 重複キー時の例外 | SQLException がスロー される |

### 7.2. IT1 Test 観点（統合テスト）

| シナリオ ID | シナリオ名 | 前提条件 | 入力 | 期待値 | 補足 |
|:---|:---|:---|:---|:---|:---|
| IT1-M-001 | テナント新規登録 | DB初期化 | SES_AI_M_TENANT.insert() | DB に 1件追加 | commit 確認 |
| IT1-M-002 | テナント参照 | テナント存在 | SES_AI_M_TENANT.selectByPk() | フィールド値が正確に取得 | - |
| IT1-M-003 | テナントグループ隔離 | 複数テナント存在 | GROUP.selectByPk(tenantId=A) | tenantId=A のみ取得 | Filter 動作確認 |
| IT1-M-004 | ユーザー権限取得 | USER に Role/Plan あり | getPermissions() | 権限セット が正確 | ビジネスロジック確認 |

---

## 8. 実装チェックリスト

### Phase 1A: Entity アノテーション追加

- [ ] SES_AI_M_TENANT に @Entity, @Table, @Column, @Id, @Convert アノテーション追加
- [ ] SES_AI_M_GROUP に @Entity, @Table, @FilterDef, @Filter アノテーション追加
- [ ] SES_AI_M_SENDER に @Entity, @Table, @FilterDef, @Filter アノテーション追加
- [ ] SES_AI_WEBAPP_M_USER に @Entity, @Table, @Enumerated アノテーション追加
- [ ] SES_AI_WEBAPP_M_NOTIFICATION に @Entity, @Table アノテーション追加
- [ ] 既存の SQL文 (INSERT_SQL, SELECT_SQL 等) が削除されたことを確認

### Phase 1B: AttributeConverter 実装

- [ ] OriginalDateTimeConverter を実装
- [ ] OriginalDateTime クラスに toLocalDateTime() メソッドを追加

### Phase 1C: pom.xml / application.yml 更新

- [ ] pom.xml から spring-boot-starter-data-jpa を削除
- [ ] pom.xml に Hibernate ORM が含まれていることを確認
- [ ] application.yml に JPA/Hibernate 設定を追加
- [ ] PostgreSQL Dialect を確認

### Phase 1D: Entity メソッド実装

- [ ] insert() メソッドの内部実装を JPA に変更（EntityManager.persist() 呼び出し）
- [ ] selectByPk() メソッドの内部実装を JPA に変更（EntityManager.find() 呼び出し）
- [ ] updateByPk() メソッドの内部実装を JPA に変更（EntityManager.merge() 呼び出し）
- [ ] deleteByPk() メソッドの内部実装を JPA に変更（EntityManager.remove() 呼び出し）
- [ ] 各メソッドで Connection パラメータが使用されないことを確認
- [ ] 例外処理が SQLException に統一されていることを確認
- [ ] テナントフィルタが必要な Entity では Hibernate Filter を有効化

### Phase 1E: テスト実装

- [ ] Unit Test で EntityManager CRUD を確認
- [ ] IT1 Test で Entity メソッドのシグネチャ互換性を確認
- [ ] Hibernate Filter の テナント隔離 動作を確認
- [ ] mvn clean install で全テスト PASS を確認
- [ ] JaCoCo カバレッジ 100% を確認

### Phase 1F: Repository ディレクトリ削除

- [ ] src/main/java/copel/sesproductpackage/core/database/repository/ を削除

---

## 9. 注意事項

### 9.1. シグネチャ互換性

Entity メソッドのシグネチャ（引数・戻り値）は **絶対に変更しない**:

| メソッド | シグネチャ | 注記 |
|:---|:---|:---|
| insert | `int insert(Connection connection)` | 戻り値は 1（成功）か例外 |
| selectByPk | `void selectByPk(Connection connection)` | 戻り値なし、フィールド セット |
| updateByPk | `boolean updateByPk(Connection connection)` | 更新成功時 true |
| deleteByPk | `boolean deleteByPk(Connection connection)` | 削除成功時 true |

### 9.2. テナントフィルタ設定

テナント隔離が必要な Entity には必ず @FilterDef / @Filter を付加:

```java
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
```

### 9.3. Connection パラメータの処理

内部実装では Connection を使用しないが、**シグネチャのため形式的に受け取る**:

```java
@Override
public int insert(Connection connection) throws SQLException {
    // connection は使用しない（null を渡されることもある）
    try {
        EntityManager entityManager = getEntityManager();
        entityManager.persist(this);
        return 1;
    } catch (Exception e) {
        throw new SQLException("Insert failed", e);
    }
}
```

### 9.4. EntityManager の取得

Entity メソッド内で EntityManager を取得する方法：

```java
// DIコンテナから取得（推奨）
EntityManager entityManager = getEntityManager();

// または Spring のコンテキストから取得
ApplicationContext context = SpringContextHolder.getContext();
EntityManager entityManager = context.getBean(EntityManager.class);
```

### 9.5. トランザクション管理

Entity メソッドは @Transactional を持たない。呼び出し側で @Transactional を設定:

```java
@Service
@Transactional
public class TenantService {
    public void registerTenant(SES_AI_M_TENANT tenant) throws SQLException {
        tenant.insert(null); // トランザクション内で実行
    }
}
```

---

## 10. 成果物チェックリスト

実装完了時に以下を確認:

- [ ] 5つの Entity に @Entity アノテーション有
- [ ] AttributeConverter (OriginalDateTime) 実装済み
- [ ] pom.xml から spring-boot-starter-data-jpa が削除
- [ ] pom.xml に Hibernate 依存性確認
- [ ] application.yml に JPA/Hibernate 設定有
- [ ] Entity メソッド内部が EntityManager 直接呼び出しに変更済み
- [ ] repository/ ディレクトリが削除済み
- [ ] Unit Test で EntityManager CRUD PASS
- [ ] IT1 Test で Entity メソッド互換性 PASS
- [ ] mvn clean install で全ビルド PASS
- [ ] JaCoCo カバレッジ 100% 達成

---

**ドキュメント作成者**: Copel Co., Ltd.  
**最終確認**: 2026-08-05
