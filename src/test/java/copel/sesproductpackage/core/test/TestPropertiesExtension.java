package copel.sesproductpackage.core.test;

import copel.sesproductpackage.core.util.Properties;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Global UT bootstrap for {@link Properties}.
 *
 * <p>The production {@link Properties} loads config from AWS S3 in a static initializer.
 * In unit tests we allow that attempt to fail, then we inject required keys into the internal map
 * so that {@code Properties.get*()} calls behave deterministically offline.
 */
public class TestPropertiesExtension implements BeforeAllCallback {

  @Override
  @SuppressWarnings("unchecked")
  public void beforeAll(ExtensionContext context) throws Exception {
    Field field = Properties.class.getDeclaredField("properties");
    field.setAccessible(true);
    Map<String, String> map = (Map<String, String>) field.get(null);

    // Provide sane defaults used by API/UT without relying on external config.
    map.putIfAbsent("GEMINI_COMPLETION_API_URL", "http://localhost/");
    map.putIfAbsent("OPEN_AI_EMBEDDING_API_URL", "http://localhost/embedding");
    map.putIfAbsent("OPEN_AI_EMBEDDING_MODEL", "text-embedding-3-small");
    map.putIfAbsent("OPEN_AI_COMPLETION_API_URL", "http://localhost/completion");
    map.putIfAbsent("OPEN_AI_FILE_UPLOAD_URL", "http://localhost/upload");
    map.putIfAbsent("OPEN_AI_FINE_TUNE_URL", "http://localhost/finetune");
    map.putIfAbsent("OPEN_AI_COMPLETION_TEMPERATURE", "0.7");
  }
}

