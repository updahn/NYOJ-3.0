<template>
  <div class="box" style="height: 100%;">
    <div>
      <el-button
        v-for="(item, index) in buttons"
        :key="index"
        size="small"
        @click.native="restartDocker(item.version)"
        type="primary"
        icon="el-icon-refresh"
      >{{ item.version === 1 ? $t('m.Restart_Fronted') : item.version === 2 ? $t('m.Restart_Judgeserver') : $t('m.Restart_Backend') }}</el-button>
      <el-button
        size="small"
        @click.native="restartDocker(4)"
        type="primary"
        icon="el-icon-view"
      >{{ $t('m.Seem_Docker')}}</el-button>
    </div>
    <div style="background: #002833;">
      <div id="terminal" ref="terminal"></div>
    </div>
  </div>
</template>

<script>
import { Terminal } from "xterm";
import "xterm/css/xterm.css";
import "xterm/lib/xterm.js";
import { FitAddon } from "xterm-addon-fit";
import api from "@/common/api";
import myMessage from "@/common/message";

export default {
  name: "Shell",
  data() {
    return {
      shellWs: "",
      term: "", // 保存terminal实例
      rows: 40,
      cols: 120,
      ssh: {
        sshHost: null,
        sshPassword: null,
        sshPort: null,
        sshUsername: null,
        sshPath: null,
        sshFronted: null,
        sshBackend: null,
        sshJudgeserver: null,
      },
      buttons: [
        { version: 1, name: "fronted" },
        { version: 2, name: "judgeserver" },
        { version: 3, name: "backend" },
      ], // 容器列表
    };
  },

  created() {
    api.admin_getSSHConfig().then((res) => {
      if (res.data.data) {
        this.ssh = res.data.data;
        this.wsShell();
      } else {
        myMessage.warning("No SSH Config");
      }
    });
  },

  mounted() {
    let route = this.$route.name;
    let _this = this;
    let term = new Terminal({
      rendererType: "canvas", //渲染类型
      rows: parseInt(_this.rows), //行数
      cols: parseInt(_this.cols), // 不指定行数，自动回车后光标从下一行开始
      convertEol: true, //启用时，光标将设置为下一行的开头
      disableStdin: false, //是否应禁用输入。
      cursorStyle: "underline", //光标样式
      cursorBlink: true, //光标闪烁
      scrollback: 50, //终端中的回滚量
      tabStopWidth: 4,
      theme: {
        foreground: "#7e9192", //字体
        background: "#002833", //背景色
        cursor: "help", //设置光标
        lineHeight: 16,
      },
    });
    // 创建terminal实例
    term.open(this.$refs["terminal"]);
    //限制和后端交互，只有输入回车键才显示结果
    term.prompt = () => {
      term.write("\r\n$ ");
    };
    term.prompt();
    // canvas背景全屏
    var fitAddon = new FitAddon();
    term.loadAddon(fitAddon);
    // 内容全屏显示
    fitAddon.fit();
    function runFakeTerminal(_this) {
      if (term._initialized) {
        return;
      }
      // 初始化
      term._initialized = true;
      term.prompt();
      /**
       * 添加事件监听器，用于按下键时的事件。事件值包含
       * 将在data事件以及DOM事件中发送的字符串
       * 触发了它。
       * @returns {IDisposable} 停止监听的对象。
       */
      /** 更新：xterm 4.x（新增）
       *为数据事件触发时添加事件侦听器。发生这种情况
       *用户输入或粘贴到终端时的示例。事件值
       *是`string`结果的结果，在典型的设置中，应该通过
       *到支持pty。
       * @返回一个IDisposable停止监听。
       */
      if (route === "admin-ssh") {
        // 通过route控制是否能输入选项，和显示按钮
        term.onData(function (key) {
          let order = {
            Data: key,
            Op: "stdin",
          };
          _this.onSend(order);
        });
      }
      _this.term = term;
    }
    runFakeTerminal(_this);
  },

  methods: {
    /**
     * **wsShell 创建页面级别的websocket,加载页面数据
     * ws 接口:/xxx/xxx/xxx
     * 参数:无
     * ws参数:
     * @deployId   任务id
     * @tagString  当前节点
     * 返回:无
     * **/
    wsShell() {
      const _this = this;
      let username = this.ssh.sshUsername;
      let password = this.ssh.sshPassword;
      let host = this.ssh.sshHost;
      let port = this.ssh.sshPort;
      let sshPath = this.ssh.sshPath;

      // websocket连接接口
      let query = `?username=${username}&password=${password}&host=${host}&port=${port}`;
      let url = `ws/ssh${query}`;

      this.shellWs = this.base.WS({
        url,
        isInit: true,
        openFn(e) {
          // 默认跳转到项目路径
          this.onSend("cd " + sshPath + "\n");
        },
        messageFn(e) {
          if (e) {
            let data = e.data;
            if (data.Data == "\n" || data.Data == "\r\nexit\r\n") {
              myMessage.error("连接已关闭");
            }
            // 打印后端返回数据
            _this.term.write(data);
          }
        },
        errorFn(e) {
          //出现错误关闭当前ws,并且提示
          myMessage.error("ws 请求失败,请刷新重试~: " + e);
        },
      });
    },

    onSend(data) {
      let Op = data.Op;
      if (Op === "stdin") {
        this.shellWs.onSend(data.Data);
      }
    },

    restartDocker(version) {
      const { sshPath, sshFrontedPath, sshBackendPath, sshJudgeserver } =
        this.ssh;

      let commond1 = `cd ${sshPath}`;
      let dockerName;
      let dockerPath;

      switch (version) {
        case 1:
          // 重启前端
          dockerName = "hoj-frontend";
          dockerPath = sshFrontedPath;
          break;
        case 2:
          // 重启判题机
          dockerName = "hoj-judgeserver";
          dockerPath = sshJudgeserver;
          break;
        case 3:
          // 重启后端
          dockerName = "hoj-backend";
          dockerPath = sshBackendPath;
          break;
        default:
          // 查看容器状态
          this.shellWs.onSend(commond1 + "\n");
          this.shellWs.onSend("docker ps -a\n");
          break;
      }

      if (version == 4) {
        return;
      }

      // 重新打包
      if (version !== 1) {
        commond1 += `${dockerPath} && docker build -t ${dockerName} . && cd ${sshPath}`;
      }

      // 重启容器
      const DCCommond = "docker-compose";
      // 停止
      let commond2 = `${DCCommond} stop ${dockerName}`;
      // 移除
      let commond3 = `${DCCommond} rm -f ${dockerName}`;
      // 启动
      let commond4 = `${DCCommond} up -d ${dockerName}`;

      // 发送指令
      const Tips =
        version === 1
          ? this.$t("m.Restart_Fronted_Tips")
          : version === 2
          ? this.$t("m.Restart_Judgeserver_Tips")
          : this.$t("m.Restart_Backend_Tips");

      // 重启后端为警告级别
      const type = version === 3 ? "error" : "warning";

      this.$confirm(Tips, "Tips", {
        confirmButtonText: this.$i18n.t("m.OK"),
        cancelButtonText: this.$i18n.t("m.Cancel"),
        type: type,
      }).then(() => {
        this.shellWs.onSend(commond1 + "\n");
        this.shellWs.onSend(commond2 + "\n");
        this.shellWs.onSend(commond3 + "\n");
        this.shellWs.onSend(commond4 + "\n");
      });
    },
  },
  beforeDestroy() {
    // 关闭网页前，断开socket连接
    this.shellWs.onClose();
  },
};
</script>

