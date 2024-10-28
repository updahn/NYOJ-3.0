<template>
  <div v-if="announcements.length > 0">
    <el-row :gutter="20">
      <div class="textBox">
        <!-- 使用 <router-link> 包裹 <transition> -->
        <router-link
          :to="{ name: route_name, params: { announcementID: text.val.id } }"
          class="announcement-item"
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
      refreshInterval: null, // 添加刷新间隔的定时器
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
  mounted() {
    this.init();
    this.startRefreshInterval(); // 启动定时器
  },
  methods: {
    init() {
      this.startMove();
      if (this.isContest) {
        this.getContestAnnouncementList();
      } else {
        this.getAnnouncementList();
      }
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
      this.timer = setTimeout(() => {
        if (this.number === this.announcements.length - 1) {
          this.number = 0;
        } else {
          this.number += 1;
        }
        this.startMove();
      }, 2500);
    },
    startRefreshInterval() {
      // 每1分钟调用init
      this.refreshInterval = setInterval(() => {
        this.init(); // 重新初始化
        clearTimeout(this.timer); // 清除当前的移动定时器
        this.startMove(); // 重新开始移动
      }, 60000);
    },
  },
  beforeDestroy() {
    clearTimeout(this.timer);
    clearInterval(this.refreshInterval); // 清除刷新定时器
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
