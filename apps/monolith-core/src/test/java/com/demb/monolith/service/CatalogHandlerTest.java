package com.demb.monolith.service;

import com.demb.monolith.db.SqlStore;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CatalogHandlerTest {
    @Test
    public void searchRejectsCommandInjectionPayloads() {
        CatalogHandler handler = new CatalogHandler(new SqlStore("test"), new ProductService(new SqlStore("test")));

        try {
            handler.search("SKU-100; raw_exec('whoami')", "us-east");
        } catch (IllegalArgumentException expected) {
            assertEquals("catalog search contains unsupported characters", expected.getMessage());
            return;
        }

        throw new AssertionError("Expected unsafe catalog search payload to be rejected");
    }

    @Test
    public void searchRejectsUnsafeRegionPayloads() {
        CatalogHandler handler = new CatalogHandler(new SqlStore("test"), new ProductService(new SqlStore("test")));

        try {
            handler.search("SKU-100", "us-east && whoami");
        } catch (IllegalArgumentException expected) {
            assertEquals("catalog region contains unsupported characters", expected.getMessage());
            return;
        }

        throw new AssertionError("Expected unsafe catalog region payload to be rejected");
    }

    @Test
    public void searchUsesProductServiceWithoutRawExec() {
        TrackingSqlStore sql = new TrackingSqlStore();
        CatalogHandler handler = new CatalogHandler(sql, new ProductService(sql));

        Map<String, Object> result = handler.search("widget", "us-east");

        assertEquals(1, result.get("count"));
        assertEquals("widget", result.get("q"));
        assertFalse(sql.rawExecCalled);
    }

    private static class TrackingSqlStore extends SqlStore {
        private boolean rawExecCalled;

        private TrackingSqlStore() {
            super("test");
        }

        @Override
        public Map<String, Object> rawExec(String sql, Object params) {
            rawExecCalled = true;
            return super.rawExec(sql, params);
        }
    }
}
