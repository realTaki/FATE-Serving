package com.webank.ai.fate.serving.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class JacksonInferenceConversionTest {
    @Test public void realInferenceRequestConversionRetainsItsValues() throws Exception {
        InferenceRequest request=new InferenceRequest(); request.setAppid("local-test");
        request.setModelId("model"); request.setModelVersion("v1"); request.setCaseId("case");
        Map<String,Object> features=new LinkedHashMap<>(); features.put("unicode","测试");
        features.put("count",7);features.put("fraction",0.25);features.put("missing",null);
        features.put("list",Arrays.asList("a","b"));request.getFeatureData().putAll(features);
        ObjectMapper mapper=new ObjectMapper();
        Map<?,?> converted=mapper.convertValue(request,HashMap.class);
        assertEquals("local-test",converted.get("appid"));assertEquals("local-test",converted.get("partyId"));
        assertEquals("model",converted.get("modelId"));assertEquals("v1",converted.get("modelVersion"));
        assertEquals(features,converted.get("featureData"));
        Map<?,?> roundTrip=mapper.readValue(mapper.writeValueAsString(converted),Map.class);
        assertEquals(converted,roundTrip);assertEquals("2.18.11",mapper.version().toString());
        assertEquals("2.18.11",new com.fasterxml.jackson.core.JsonFactory().version().toString());
    }
    @Test public void existingJavaTimeRuntimeModuleStillWorks() throws Exception {
        ObjectMapper mapper=new ObjectMapper();
        // The existing Boot closure keeps jsr310 2.9.6; exercise that actual module.
        mapper.registerModule((com.fasterxml.jackson.databind.Module)
            Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule").newInstance());
        java.time.LocalDate date=java.time.LocalDate.of(2026,10,27);
        assertEquals(date,mapper.readValue(mapper.writeValueAsString(date),java.time.LocalDate.class));
        java.time.Instant instant=java.time.Instant.parse("2026-10-27T00:00:00Z");
        assertEquals(instant,mapper.readValue(mapper.writeValueAsString(instant),java.time.Instant.class));
    }
}
