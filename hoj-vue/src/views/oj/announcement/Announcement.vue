<template>
  <div v-if="announcements.length > 0">
    <el-row :gutter="20">
      <div class="textBox">
        <router-link
          class="announcement-item"
          :to="{ name: route_name, params: { announcementID: text.val.id } }"
        >
          <transition name="slide">
            <p class="text" :key="text.id">
              <el-tag :type="isContest ? 'danger' : 'warning'" class="tag">{{ text.val.title }}</el-tag>
            </p>
          </transition>
        </router-link>
      </div>
    </el-row>
  </div>
</template>

<script>
import api from "@/common/api";

export default {
  name: "scroll",
  data() {
    return {
      announcements: [],
      number: 0,
      timer: null,
      length: 5,
    };
  },
  computed: {
    text() {
      const defaultText = { id: 0, title: "", content: "" };
      return {
        id: this.number,
        val: this.announcements[this.number] || defaultText,
      };
    },
    isContest() {
      return !!this.$route.params.contestID;
    },
    route_name() {
      let name = this.$route.name;
      if (name === "ContestFullProblemDetails") {
        return "ContestFullAnnouncement";
      }
      return this.isContest ? "ContestAnnouncementList" : "Announcements";
    },
  },
  watch: {
    // 监听announcements数组长度变化
    "announcements.length": function (newLength, oldLength) {
      if (newLength !== oldLength) {
        this.number = 0; // 重置number为0
      }
    },
  },
  mounted() {
    this.init();
    this.startRefreshInterval(); // 启动定时器
  },
  methods: {
    init() {
      if (this.isContest) {
        this.getContestAnnouncementList();
      } else {
        this.getAnnouncementList();
      }
      this.startMove();
    },
    getAnnouncementList(page = 1) {
      api.getAnnouncementList(page, this.limit, null).then(
        (res) => {
          this.announcements = res.data.data.records.slice(0, this.length);
        },
        () => {
          this.announcements = [];
        }
      );
    },
    getContestAnnouncementList(page = 1) {
      api
        .getContestAnnouncementList(
          page,
          this.limit,
          this.$route.params.contestID,
          null
        )
        .then(
          (res) => {
            this.announcements = res.data.data.records;
          },
          () => {
            this.announcements = [];
          }
        );
    },
    startMove() {
      // 清除之前的定时器，避免重复
      if (this.timer) {
        clearInterval(this.timer);
      }

      // 使用setInterval代替递归的setTimeout
      this.timer = setInterval(() => {
        if (this.number === this.announcements.length - 1) {
          this.number = 0;
        } else {
          this.number += 1;
        }
      }, 3000); // 滚动时间为3秒
    },
    startRefreshInterval() {
      // 根据isContest设置不同的刷新间隔
      const refreshTime = this.isContest ? 30000 : 60000; // 比赛模式30秒，其他情况60秒

      this.refreshInterval = setInterval(() => {
        // 清除当前的移动定时器
        if (this.timer) {
          clearInterval(this.timer);
        }

        // 重新获取公告数据
        if (this.isContest) {
          this.getContestAnnouncementList();
        } else {
          this.getAnnouncementList();
        }

        // 重新开始移动
        this.startMove();
      }, refreshTime);
    },
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
    }
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  },
};
</script>

<style scoped>
.textBox {
  width: 100%;
  height: 35px;
  margin: 0 auto;
  overflow: hidden;
  text-align: center;
}

.text {
  width: 100%;
  position: absolute;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tag {
  height: 20px;
  line-height: 20px;
  margin-right: 5px;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.5s linear;
}

.slide-enter {
  transform: translateY(20px) scale(1);
  opacity: 1;
}

.slide-leave-to {
  transform: translateY(-20px) scale(0.8);
  opacity: 0;
}

.textBox .announcement-item {
  cursor: pointer;
}
.announcement-item:hover {
  background-color: #f0f0f0;
}
</style>
