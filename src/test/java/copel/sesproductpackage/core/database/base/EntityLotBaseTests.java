package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class EntityLotBaseTests {

  static class TestEntity extends EntityBase {
    public TestEntity(OriginalDateTime date) {
      this.registerDate = date;
    }

    @Override
    public int insert(Connection connection) throws SQLException {
      return 0;
    }

    @Override
    public void selectByPk(Connection connection) throws SQLException {}

    @Override
    public boolean updateByPk(Connection connection) throws SQLException {
      return false;
    }

    @Override
    public boolean deleteByPk(Connection connection) throws SQLException {
      return false;
    }
  }

  static class TestEntityLot extends EntityLotBase<TestEntity> {
    @Override
    public void selectAll(Connection connection) throws SQLException {}

    @Override
    protected TestEntity mapResultSet(ResultSet resultSet) throws SQLException {
      return new TestEntity(new OriginalDateTime());
    }
  }

  @Test
  void testLotOperations() {
    TestEntityLot lot = new TestEntityLot();
    assertTrue(lot.isEmpty());
    assertEquals(0, lot.size());

    TestEntity e1 = new TestEntity(new OriginalDateTime("2023-01-01 10:00:00"));
    TestEntity e2 = new TestEntity(new OriginalDateTime("2023-01-02 10:00:00"));
    TestEntity e3 = new TestEntity(new OriginalDateTime("2023-01-01 09:00:00"));

    lot.add(e1);
    lot.add(e2);
    lot.add(e3);

    assertFalse(lot.isEmpty());
    assertEquals(3, lot.size());
    assertEquals(e1, lot.get(0));
    assertEquals(e2, lot.get(1));
    assertEquals(e3, lot.get(2));
    assertNull(lot.get(99));

    // Test iterator
    int count = 0;
    for (TestEntity e : lot) {
      assertNotNull(e);
      count++;
    }
    assertEquals(3, count);

    // Test sort
    lot.sort();
    assertEquals(e3, lot.get(0)); // 09:00
    assertEquals(e1, lot.get(1)); // 10:00 (Jan 1)
    assertEquals(e2, lot.get(2)); // Jan 2

    // Test toString
    assertNotNull(lot.toString());
  }
}
