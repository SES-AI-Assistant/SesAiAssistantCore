package copel.sesproductpackage.core.api.google;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class GoogleDriveFetcherTest {

    @Test
    public void testFetchText_EmptyUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            GoogleDriveFetcher.fetchText("");
        });
    }

    @Test
    public void testFetchText_NullUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            GoogleDriveFetcher.fetchText(null);
        });
    }

    @Test
    public void testFetchText_InvalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            GoogleDriveFetcher.fetchText("https://example.com");
        });
    }

    @Test
    public void testFetchText_NonExistentSpreadsheet() {
        // 存在しないスプレッドシートのURLでIOExceptionが発生することを確認
        assertThrows(IOException.class, () -> {
            GoogleDriveFetcher.fetchText("https://docs.google.com/spreadsheets/d/non-existent-file-id/edit#gid=0");
        });
    }

    @Test
    public void testFetchText_NonExistentDocument() {
        // 存在しないドキュメントのURLでIOExceptionが発生することを確認
        assertThrows(IOException.class, () -> {
            GoogleDriveFetcher.fetchText("https://docs.google.com/document/d/non-existent-file-id/edit");
        });
    }
}
