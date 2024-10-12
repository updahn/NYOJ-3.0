<template>
  <div id="app">
    <h1>Random Screen Capture</h1>
    <button @click="startCapture">Start Monitoring</button>
    <div v-if="screenType">
      <p>Captured screen type: {{ screenType }}</p>
    </div>
    <div>
      <img
        v-for="(image, index) in capturedImages"
        :key="index"
        :src="image"
        alt="Captured Screen"
        class="captured-image"
      />
    </div>
    <div v-if="stopInfo">
      <p>Stop Info: {{ stopInfo }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: "App",
  data() {
    return {
      capturedImages: [], // 存储多次捕获到的屏幕截图
      screenType: "", // 存储捕获到的屏幕类型
      stopInfo: "",
      captureStream: null, // 屏幕流
      captureInterval: null, // 定时器
      monitorDuration: 60000, // 监控总时长60秒
      captureCount: 20, // 随机截取20次
      capturesRemaining: 20, // 剩余截取次数
    };
  },
  methods: {
    async startCapture() {
      try {
        // 请求用户授权并获取显示媒体
        this.captureStream = await navigator.mediaDevices.getDisplayMedia({
          video: {
            cursor: "always", // 显示鼠标指针
            displaySurface: "monitor", // 捕获整个显示器
          },
        });

        // 获取流中的视频轨道
        const track = this.captureStream.getVideoTracks()[0];
        const settings = track.getSettings();

        // 获取屏幕类型，可能值为：monitor, window, application
        this.screenType = settings.displaySurface;

        // 监听监控结束的事件（手动停止共享时触发）
        track.onended = () => {
          console.log("Screen monitoring stopped.");
          this.stopInfo = "Screen monitoring stopped.";
          this.stopCapture(); // 当手动停止屏幕共享时，停止截屏操作
        };

        // 开始随机截取20次屏幕图像
        this.startRandomCapture();
      } catch (err) {
        console.error("Error: " + err);
      }
    },

    // 随机截取20次屏幕，持续60秒
    startRandomCapture() {
      // 确保每次调用重置计数
      this.capturesRemaining = this.captureCount;

      this.captureInterval = setInterval(() => {
        if (this.capturesRemaining > 0) {
          this.captureScreen();
          this.capturesRemaining--;
        } else {
          // 当达到20次后，停止继续截取
          clearInterval(this.captureInterval);
        }
      }, Math.random() * 3000); // 每次随机间隔 0 到 3 秒之间，分布在60秒内截取20次
    },

    // 截取当前屏幕并显示图片
    captureScreen() {
      const video = document.createElement("video");
      video.srcObject = this.captureStream;

      // 等待视频加载
      video.onloadedmetadata = async () => {
        await video.play();

        // 创建 canvas 并绘制视频帧
        const canvas = document.createElement("canvas");
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const context = canvas.getContext("2d");

        // 在 canvas 上绘制当前视频帧
        context.drawImage(video, 0, 0, canvas.width, canvas.height);

        // 将 canvas 转换为图片 URL（data URL）并存储在数组中
        const capturedImage = canvas.toDataURL("image/png");
        this.capturedImages.push(capturedImage); // 存储图片

        // 停止视频播放
        video.pause();
      };
    },

    // 停止捕获屏幕
    stopCapture() {
      // 停止流和定时器
      if (this.captureStream) {
        this.captureStream.getTracks().forEach((track) => track.stop()); // 停止所有流
        this.captureStream = null;
      }

      if (this.captureInterval) {
        clearInterval(this.captureInterval); // 清理定时器
      }

      console.log("Screen monitoring has been stopped manually.");
    },
  },
};
</script>

<style scoped>
img.captured-image {
  width: 150px;
  height: 100px;
  object-fit: cover; /* 保持图片缩放不失真 */
  margin-top: 10px;
  border: 1px solid black;
}
</style>
