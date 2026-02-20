package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.util.Properties;

class OpenAITests {

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupProperties() throws Exception {
        Field propertiesField = Properties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
        propertiesMap.put("OPEN_AI_EMBEDDING_API_URL", "http://localhost/embedding");
        propertiesMap.put("OPEN_AI_EMBEDDING_MODEL", "text-embedding-3-small");
        propertiesMap.put("OPEN_AI_COMPLETION_API_URL", "http://localhost/completion");
        propertiesMap.put("OPEN_AI_COMPLETION_TEMPERATURE", "0.7");
        propertiesMap.put("OPEN_AI_FILE_UPLOAD_URL", "http://localhost/upload");
        propertiesMap.put("OPEN_AI_FINE_TUNE_URL", "http://localhost/finetune");
    }

    @Test
    void testConstructor() {
        OpenAI api = new OpenAI("key");
        assertNotNull(api);
        
        OpenAI api2 = new OpenAI("key", "model");
        assertNotNull(api2);
    }

    @Test
    void testEmbeddingNull() throws Exception {
        OpenAI api = new OpenAI("key");
        assertNull(api.embedding(null));
    }

    @Test
    void testGenerateNull() throws Exception {
        OpenAI api = new OpenAI("key");
        assertNull(api.generate(null, null));
        assertNull(api.generate("prompt", null));
        assertNull(api.generate(null, 0.7f));
    }
    
    // HttpURLConnection のモックが困難なため、実接続を伴うテストはスキップするか
    // あるいは例外が発生することを期待するテストにする（URLが不正など）
}
