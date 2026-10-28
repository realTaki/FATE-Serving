/* Copyright 2026 The FATE Authors. Licensed under the Apache License, Version 2.0. */
package com.webank.ai.fate.serving.bean;

import com.alibaba.fastjson.JSON;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises the actual inference DTO; does not start the service or touch stores/models. */
public class FastjsonInferenceConversionTest {
    @Test public void inferenceRequestRetainsItsNormalFeatureValues() {
        InferenceRequest request = new InferenceRequest(); request.setAppid("local-test");
        request.setModelId("model"); request.setModelVersion("v1");
        request.getFeatureData().put("unicode", "测试"); request.getFeatureData().put("count", 7);
        request.getFeatureData().put("items", Arrays.asList("a", "b"));
        InferenceRequest restored = JSON.parseObject(request.toString(), InferenceRequest.class);
        assertEquals("local-test", restored.getAppid()); assertEquals("local-test", restored.getPartyId());
        assertEquals("model", restored.getModelId()); assertEquals("v1", restored.getModelVersion());
        assertEquals(request.getFeatureData(), restored.getFeatureData());
    }
}
