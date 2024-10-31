package top.hcode.hoj.utils;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;

import java.nio.FloatBuffer;

@Slf4j(topic = "hoj")
public class ONNXRuntimeUtils implements AutoCloseable {
    /** ONNX服务的环境 */
    private OrtEnvironment env = OrtEnvironment.getEnvironment();

    /**
     * 创建ONNX模型会话
     *
     * @param modelPath 模型文件路径
     * @return 创建的ONNX会话
     */
    public OrtSession createSession(String modelPath) {
        try {
            return env.createSession(modelPath); // 创建会话
        } catch (Exception e) {
            log.error("Failed to create ONNX model session", e); // 创建模型失败的错误处理
            return null; // 返回空会话
        }
    }

    /**
     * 创建单精度浮点数张量
     *
     * @param data  浮点缓存数据
     * @param shape 张量形状
     * @return 创建的ONNX张量
     */
    public OnnxTensor createTensor(FloatBuffer data, long[] shape) {
        try {
            return OnnxTensor.createTensor(env, data, shape); // 创建张量
        } catch (Exception e) {
            log.error("Failed to create tensor", e); // 创建张量失败的错误处理
            return null; // 返回空张量
        }
    }

    /**
     * 创建单精度浮点数张量
     *
     * @param data  浮点数组
     * @param shape 张量形状
     * @return 创建的ONNX张量
     */
    public OnnxTensor createTensor(float[] data, long[] shape) {
        return createTensor(FloatBuffer.wrap(data), shape); // 使用FloatBuffer包装数据创建张量
    }

    /**
     * 关闭ONNX服务
     */
    @Override
    public void close() throws Exception {
        env.close(); // 关闭环境
    }
}
