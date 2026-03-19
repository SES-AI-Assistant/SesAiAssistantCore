package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.Transformer;
import org.junit.jupiter.api.Test;

class VectorTest {

  @Test
  void testEmbedding() throws Exception {
    Transformer transformer = mock(Transformer.class);
    float[] mockVector = {0.1f, 0.2f, 0.3f};
    when(transformer.embedding("test")).thenReturn(mockVector);

    Vector vector = new Vector(transformer);
    vector.setRawString("test");
    assertEquals("test", vector.getRawString());

    vector.embedding();
    assertArrayEquals(mockVector, vector.getValue());
  }

  @Test
  void testEmbeddingNullEmpty() throws Exception {
    Transformer transformer = mock(Transformer.class);
    Vector vector = new Vector(transformer);

    vector.embedding();
    assertNull(vector.getValue());

    vector.setRawString("");
    vector.embedding();
    assertNull(vector.getValue());

    Vector vectorNoTransformer = new Vector(null);
    vectorNoTransformer.setRawString("test");
    vectorNoTransformer.embedding();
    assertNull(vectorNoTransformer.getValue());
  }

  @Test
  void testToString() {
    float[] vals = {1.0f, 2.0f};
    Transformer transformer = mock(Transformer.class);
    try {
      when(transformer.embedding(anyString())).thenReturn(vals);
      Vector vector = new Vector(transformer);
      vector.setRawString("test");
      vector.embedding();
      assertEquals("[1.0,2.0]", vector.toString());
    } catch (Exception e) {
      fail(e);
    }

    Vector nullVector = new Vector(null);
    assertEquals("[]", nullVector.toString());
  }
}
