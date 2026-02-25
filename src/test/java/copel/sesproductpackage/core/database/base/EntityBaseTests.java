package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

class EntityBaseTests {

  // テスト用の具象クラス
  static class TestEntity extends EntityBase {
    @Override
    public int insert(Connection connection) throws SQLException {
      return 0;
    }

    @Override
    public void selectByPk(Connection connection) throws SQLException {}

    @Override
    public boolean updateByPk(Connection connection) throws SQLException {
      return true;
    }

    @Override
    public boolean deleteByPk(Connection connection) throws SQLException {
      return true;
    }

    @Override
    public String toString() {
      return "TestEntity";
    }
  }

  static class TestEntityLot extends EntityLotBase<TestEntity> {
    @Override
    public void selectAll(Connection connection) throws SQLException {}

    @Override
    protected TestEntity mapResultSet(ResultSet resultSet) throws SQLException {
      return new TestEntity();
    }
  }

  @Test
  void testEntityBaseBasic() {
    TestEntity entity = new TestEntity();
    OriginalDateTime now = new OriginalDateTime();
    entity.setRegisterDate(now);
    entity.setRegisterUser("user1");

    assertEquals(now, entity.getRegisterDate());
    assertEquals("user1", entity.getRegisterUser());
  }

  @Test
  void testEntityBaseCompareTo() {
    TestEntity e1 = new TestEntity();
    TestEntity e2 = new TestEntity();

    // 両方 null
    assertEquals(0, e1.compareTo(null));

    OriginalDateTime d1 = new OriginalDateTime("2026-01-01 00:00:00");
    OriginalDateTime d2 = new OriginalDateTime("2026-01-02 00:00:00");

    e1.setRegisterDate(d1);
    e2.setRegisterDate(d2);

    assertTrue(e1.compareTo(e2) < 0);
    assertTrue(e2.compareTo(e1) > 0);
    // 同一オブジェクトの比較は 0 が返る
    assertEquals(0, e1.compareTo(e1));

    // this.registerDate == null のケース
    TestEntity e3 = new TestEntity();
    TestEntity e4 = new TestEntity();
    e4.setRegisterDate(d1);
    assertEquals(-1, e3.compareTo(e4));
    assertEquals(0, e3.compareTo(new TestEntity()));
    assertEquals(1, e4.compareTo(e3));
    assertEquals(1, e4.compareTo(null));
  }

  @Test
  void testEntityLotBase() {
    TestEntityLot lot = new TestEntityLot();
    assertTrue(lot.isEmpty());
    assertEquals(0, lot.size());

    TestEntity e1 = new TestEntity();
    e1.setRegisterDate(new OriginalDateTime("2026-01-02 00:00:00"));
    TestEntity e2 = new TestEntity();
    e2.setRegisterDate(new OriginalDateTime("2026-01-01 00:00:00"));

    lot.add(e1);
    lot.add(e2);

    assertFalse(lot.isEmpty());
    assertEquals(2, lot.size());
    assertEquals(e1, lot.get(0));
    assertEquals(e2, lot.get(1));
    assertNull(lot.get(2));

    // Sort
    lot.sort();
    assertEquals(e2, lot.get(0)); // 2026-01-01 が先に来る

    // Iterator
    Iterator<TestEntity> it = lot.iterator();
    assertTrue(it.hasNext());
    assertEquals(e2, it.next());

    // toString
    String str = lot.toString();
    assertTrue(str.contains("(0)TestEntity"));
    assertTrue(str.contains("(1)TestEntity"));
  }
}
