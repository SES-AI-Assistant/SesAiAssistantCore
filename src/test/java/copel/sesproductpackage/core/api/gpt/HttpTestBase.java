package copel.sesproductpackage.core.api.gpt;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import static org.mockito.Mockito.*;

public class HttpTestBase {
    public static HttpURLConnection sharedMockConn;
    
    static {
        try {
            URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
                @Override
                public URLStreamHandler createURLStreamHandler(String protocol) {
                    if ("http".equals(protocol) || "https".equals(protocol)) {
                        return new URLStreamHandler() {
                            @Override
                            protected HttpURLConnection openConnection(URL u) {
                                return sharedMockConn;
                            }
                        };
                    }
                    return null;
                }
            });
        } catch (Error e) {
            // Already set
        }
    }
}
