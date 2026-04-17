package copel.sesproductpackage.core.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FulltextConditionsWhereClauseTest {

  @Test
  void testEscapeLikeKeyword() {
    assertEquals("", FulltextConditionsWhereClause.escapeLikeKeyword(null));
    assertEquals("a", FulltextConditionsWhereClause.escapeLikeKeyword("a"));
    assertEquals("\\%a\\_", FulltextConditionsWhereClause.escapeLikeKeyword("%a_"));
    assertEquals("\\\\", FulltextConditionsWhereClause.escapeLikeKeyword("\\"));
  }

  @Test
  void testHasSearchableKeyword() {
    assertFalse(FulltextConditionsWhereClause.hasSearchableKeyword(null));
    assertFalse(FulltextConditionsWhereClause.hasSearchableKeyword(Collections.emptyList()));
    assertFalse(
        FulltextConditionsWhereClause.hasSearchableKeyword(
            Collections.singletonList(new FulltextCondition("AND", "  ", false))));
    assertTrue(
        FulltextConditionsWhereClause.hasSearchableKeyword(
            Collections.singletonList(new FulltextCondition("AND", "a", false))));
    List<FulltextCondition> withNull = new ArrayList<>();
    withNull.add(null);
    withNull.add(new FulltextCondition("AND", "a", false));
    assertTrue(FulltextConditionsWhereClause.hasSearchableKeyword(withNull));
  }

  @Test
  void testBuildSimpleAnd() {
    FulltextConditionsWhereClause.Built b =
        FulltextConditionsWhereClause.build(
            "col",
            Arrays.asList(
                new FulltextCondition("AND", "x", false),
                new FulltextCondition("AND", "y", false)));
    assertFalse(b.getWhereClauseWithoutWhereKeyword().contains(" OR "));
    assertEquals(2, b.getLikeParams().size());
    assertTrue(b.getLikeParams().get(0).contains("x"));
    assertTrue(b.getLikeParams().get(1).contains("y"));
  }

  @Test
  void testBuildSkipsBlankOnlySegmentAfterOr() {
    FulltextConditionsWhereClause.Built b =
        FulltextConditionsWhereClause.build(
            "col",
            Arrays.asList(
                new FulltextCondition("AND", "a", false),
                new FulltextCondition("OR", "  ", false),
                new FulltextCondition("AND", "  ", false)));
    assertEquals(1, b.getLikeParams().size());
    assertFalse(b.getWhereClauseWithoutWhereKeyword().contains(" OR "));
  }

  @Test
  void testBuildOrSegments() {
    FulltextConditionsWhereClause.Built b =
        FulltextConditionsWhereClause.build(
            "c",
            Arrays.asList(
                new FulltextCondition("AND", "A", false),
                new FulltextCondition("AND", "B", false),
                new FulltextCondition("OR", "D", true),
                new FulltextCondition("AND", "E", false)));
    assertEquals(4, b.getLikeParams().size());
    assertTrue(b.getWhereClauseWithoutWhereKeyword().contains(" OR "));
    assertTrue(b.getWhereClauseWithoutWhereKeyword().contains("NOT LIKE"));
  }

  @Test
  void testBuildInvalid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FulltextConditionsWhereClause.build("c", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> FulltextConditionsWhereClause.build(null, Collections.emptyList()));
    assertThrows(
        IllegalArgumentException.class,
        () -> FulltextConditionsWhereClause.build("   ", Collections.emptyList()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            FulltextConditionsWhereClause.build(
                null,
                Collections.singletonList(new FulltextCondition("AND", "ok", false))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            FulltextConditionsWhereClause.build(
                "c",
                Collections.singletonList(new FulltextCondition("AND", "", false))));
  }

  @Test
  void testSplitSegmentsOrOnlyFirst() {
    List<List<FulltextCondition>> segs =
        FulltextConditionsWhereClause.splitSegments(
            Collections.singletonList(new FulltextCondition("OR", "only", false)));
    assertEquals(1, segs.size());
    assertEquals(1, segs.get(0).size());
  }

  @Test
  void testSplitSegmentsLeadingNullThenOr() {
    List<List<FulltextCondition>> segs =
        FulltextConditionsWhereClause.splitSegments(
            Arrays.asList(null, new FulltextCondition("OR", "x", false)));
    assertEquals(1, segs.size());
    assertEquals(1, segs.get(0).size());
  }

  @Test
  void testSplitSegmentsAllNulls() {
    assertTrue(FulltextConditionsWhereClause.splitSegments(Arrays.asList(null, null)).isEmpty());
  }

  @Test
  void testSplitSegmentsSkipsNullAndNonOrContinues() {
    List<List<FulltextCondition>> segs =
        FulltextConditionsWhereClause.splitSegments(
            Arrays.asList(
                new FulltextCondition("AND", "a", false),
                null,
                new FulltextCondition("AND", "b", false)));
    assertEquals(1, segs.size());
    assertEquals(2, segs.get(0).size());

    segs =
        FulltextConditionsWhereClause.splitSegments(
            Arrays.asList(
                new FulltextCondition("AND", "a", false),
                new FulltextCondition("NAND", "b", false)));
    assertEquals(1, segs.size());
    assertEquals(2, segs.get(0).size());

    segs =
        FulltextConditionsWhereClause.splitSegments(
            Arrays.asList(
                new FulltextCondition("AND", "a", false),
                new FulltextCondition(null, "b", false)));
    assertEquals(1, segs.size());
    assertEquals(2, segs.get(0).size());
  }

  @Test
  void testBuildSinglePositiveLikeBranch() {
    FulltextConditionsWhereClause.Built b =
        FulltextConditionsWhereClause.build(
            "col", Collections.singletonList(new FulltextCondition("AND", "one", false)));
    assertTrue(b.getWhereClauseWithoutWhereKeyword().contains("LIKE ?"));
    assertEquals(1, b.getLikeParams().size());
  }
}
