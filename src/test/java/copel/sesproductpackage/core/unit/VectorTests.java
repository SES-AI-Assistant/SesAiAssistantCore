package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.Transformer;
import org.junit.jupiter.api.Test;

class VectorTests {

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

    // rawString is null
    vector.embedding();
    assertNull(vector.getValue());

    // rawString is empty
    vector.setRawString("");
    vector.embedding();
    assertNull(vector.getValue());

    // transformer is null
    Vector vectorNoTransformer = new Vector(null);
    vectorNoTransformer.setRawString("test");
    vectorNoTransformer.embedding();
    assertNull(vectorNoTransformer.getValue());
  }

  @Test
  void testToString() {
    Vector vector = new Vector(null);
    // value が null の場合 NullPointerException が発生する実装になっているので、あえてセットする
    // 実際の実装: for (float value : this.value) { ... }

    // リフレクションで value をセットするか、embedding を通す
    // ここでは直接 float[] をセットするメソッドがないので、モックを使って embedding させるか
    // または、実装の不備（value が null の時の toString）を露呈させるテストを書く

    // 正常系
    float[] vals = {1.0f, 2.0f};
    // 内部フィールドにアクセスできないので、モック Transformer でセット
    Transformer transformer = mock(Transformer.class);
    try {
      when(transformer.embedding(anyString())).thenReturn(vals);
      vector = new Vector(transformer);
      vector.setRawString("test");
      vector.embedding();
      assertEquals("[1.0,2.0]", vector.toString());
    } catch (Exception e) {
      fail(e);
    }
  }
}
