/* Copyright 2026 The FATE Authors. Licensed under the Apache License, Version 2.0. */
package com.webank.ai.fate.serving.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import com.webank.ai.fate.serving.core.bean.HostFederatedParams;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Bounded, local parser checks: no service, network, gadget execution or external data. */
public class FastjsonSecurityTest {
    public static class LocalValue { public int value; public String text; }
    public static class AcceptedValue { public int value; }

    @Test public void patchedVersionIsActuallyLoaded() throws Exception {
        Properties artifact = new Properties();
        try (java.io.InputStream in = JSON.class.getResourceAsStream(
                "/META-INF/maven/com.alibaba/fastjson/pom.properties")) {
            assertNotNull(in); artifact.load(in);
        }
        // The official 1.2.84 JAR still reports JSON.VERSION="1.2.83"; use runtime artifact metadata.
        assertEquals("1.2.84", artifact.getProperty("version"));
    }

    @Test public void typedCacheValueRoundTripsAsBefore() {
        LocalValue value = new LocalValue(); value.value = 7; value.text = "本地测试";
        String cached = JSON.toJSONString(value);
        LocalValue restored = JSON.parseObject(cached, LocalValue.class);
        assertEquals(value.value, restored.value); assertEquals(value.text, restored.text);
    }

    @Test public void byteSerializationRetainsNormalNestedValues() {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("unicode", "测试"); data.put("count", 7);
        data.put("items", Arrays.asList("a", "b"));
        Map<?,?> restored = JSON.parseObject(new String(JSON.toJSONBytes(data), StandardCharsets.UTF_8), Map.class);
        assertEquals(data, restored);
    }

    @Test public void actualHostFederatedParamsStillRoundTrips() {
        HostFederatedParams params = JSON.parseObject(
                "{\"caseId\":\"local-case\",\"seqNo\":\"local-seq\",\"methodName\":\"predict\",\"data\":{\"x\":7}}",
                HostFederatedParams.class);
        HostFederatedParams restored = JSON.parseObject(JSON.toJSONBytes(params), HostFederatedParams.class);
        assertEquals("local-case", restored.getCaseId()); assertEquals("local-seq", restored.getSeqNo());
        assertEquals("predict", restored.getMethodName()); assertEquals(7, restored.getData().get("x"));
    }

    @Test public void illegalTypeNamesNeverReachAClassLoader() {
        final List<String> requests = new ArrayList<>();
        ClassLoader recording = new ClassLoader(null) {
            @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
                requests.add(name); throw new ClassNotFoundException(name);
            }
        };
        // Deliberately non-URL strings. The loader records and rejects everything; it cannot fetch anything.
        assertNull(TypeUtils.loadClass("local:test.Invalid", recording, false));
        assertNull(TypeUtils.loadClass("local!test.Invalid", recording, false));
        assertEquals(Collections.emptyList(), requests);
    }

    @Test public void illegalAutoTypeNamesAreRejectedEvenWhenEnabledLocally() {
        ParserConfig config = new ParserConfig(); config.setAutoTypeSupport(true);
        for (String name : Arrays.asList("local:test.Invalid", "local!test.Invalid")) {
            try { config.checkAutoType(name, null, 0); fail("illegal type accepted: " + name); }
            catch (JSONException expected) { /* expected before class resolution */ }
        }
    }

    @Test public void explicitBenignNestedClassAcceptStillWorks() {
        ParserConfig config = new ParserConfig();
        config.addAccept(AcceptedValue.class.getName().replace('$', '.'));
        assertSame(AcceptedValue.class, config.checkAutoType(AcceptedValue.class.getName(), null, 0));
    }
}
