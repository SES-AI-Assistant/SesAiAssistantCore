package copel.sesproductpackage.core.search;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FulltextConditionTest {

  @Test
  void testHasSearchableKeyword() {
    assertFalse(new FulltextCondition("AND", null, false).hasSearchableKeyword());
    assertFalse(new FulltextCondition("AND", "  \t", false).hasSearchableKeyword());
    assertTrue(new FulltextCondition("OR", "a", true).hasSearchableKeyword());
  }

  @Test
  void testEqualsHashCode() {
    FulltextCondition a = new FulltextCondition("AND", "k", true);
    FulltextCondition b = new FulltextCondition("AND", "k", true);
    FulltextCondition c = new FulltextCondition("OR", "k", true);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
    FulltextCondition d = new FulltextCondition("AND", "k", false);
    assertNotEquals(a, d);
    FulltextCondition e = new FulltextCondition("AND", "other", true);
    assertNotEquals(a, e);
    assertNotEquals(a, null);
    assertNotEquals(a, "x");
    assertEquals(a, a);
  }
}
