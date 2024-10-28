package top.hcode.hoj.util;

import ai.onnxruntime.OnnxTensor;

import java.util.HashMap;
import java.util.Map;

public class MapOf {
    public static Map<String, OnnxTensor> of(String k, OnnxTensor v) {
        Map<String, OnnxTensor> map = new HashMap<>();
        map.put(k, v);
        return map;
    }

    public static Map<String, OnnxTensor> of(String k, OnnxTensor v, String k1, OnnxTensor v1) {
        Map<String, OnnxTensor> map = new HashMap<>();
        map.put(k, v);
        map.put(k1, v1);
        return map;
    }
}
