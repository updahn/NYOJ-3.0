<template>
  <div class="admin-container">
    <div v-if="!mobileNar" class="menu-container">
      <el-menu
        class="vertical_menu"
        :router="true"
        :default-active="currentPath"
        :collapse="isCollapse"
        :collapse-transition="false"
        unique-opened
        :style="{ width: isCollapse ? '64px' : '210px' }"
      >
        <div class="logo">
          <el-tooltip :content="$t('m.Click_To_Home')" placement="bottom" effect="dark">
            <router-link to="/">
              <img
                :style="{ width: isCollapse ? '50px' : '110px', height: isCollapse ? '50px' : '110px' }"
                :src="imgUrl"
                alt="Online Judge Signup"
              />
            </router-link>
          </el-tooltip>
        </div>

        <el-menu-item index="/signup/">
          <i class="fa fa-tachometer fa-size" aria-hidden="true"></i>
          {{ $t("m.Dashboard") }}
        </el-menu-item>
        <el-submenu index="user">
          <template slot="title">
            <i class="fa el-icon-user-solid fa-size"></i>
            {{ $t("m.Account_Info") }}
          </template>
          <el-menu-item index="/signup/user/account">
            {{
            $t("m.User_Info")
            }}
          </el-menu-item>
          <el-menu-item index="/signup/user/setting">
            {{
            $t("m.Account_Config")
            }}
          </el-menu-item>
        </el-submenu>
        <el-submenu index="pool">
          <template slot="title">
            <i class="fa fa-users fa-size"></i>
            {{ $t("m.Coach_Pool") }}
          </template>
          <el-menu-item index="/signup/pool/user">
            {{
            $t("m.Coach_UserPool")
            }}
          </el-menu-item>
          <el-menu-item index="/signup/pool/team">
            {{
            $t("m.Coach_TeamPool")
            }}
          </el-menu-item>
        </el-submenu>
        <el-menu-item index="/signup/contest">
          <i class="fa fa-trophy fa-size" aria-hidden="true"></i>
          {{ $t("m.Signup_ContestList") }}
        </el-menu-item>

        <div
          style="position: fixed; bottom: 0; z-index: 9999; background: white;"
          :style="{ width: isCollapse ? '63px' : '209px' }"
        >
          <div style="height: 2px;">
            <el-divider></el-divider>
          </div>
          <el-menu-item @click="toggleCollapse">
            <i
              :class="isCollapse
            ? 'fa fa-caret-square-o-right fa-size'
            : 'fa fa-caret-square-o-left fa-size'"
            ></i>
          </el-menu-item>
        </div>
      </el-menu>
      <div id="header" :style="{ marginLeft: isCollapse ? '64px' : '210px' }">
        <el-row type="flex" justify="space-between" align="middle">
          <div style="text-align: left;">
            <div class="breadcrumb-container">
              <el-breadcrumb separator-class="el-icon-arrow-right">
                <el-breadcrumb-item :to="{ path: '/signup/' }">{{ $t("m.Home_Page") }}</el-breadcrumb-item>
                <el-breadcrumb-item
                  v-for="item in routeList"
                  :key="item.path"
                >{{ $t("m." + item.meta.title.replaceAll(" ", "_")) }}</el-breadcrumb-item>
              </el-breadcrumb>
            </div>
          </div>
          <div v-show="isAuthenticated" style="text-align: right;">
            <avatar
              :username="userInfo.username"
              :inline="true"
              :size="30"
              color="#FFF"
              :src="userInfo.avatar"
              class="drop-avatar"
            ></avatar>
            <el-dropdown @command="handleCommand" style="vertical-align: middle">
              <span class="dropdown-trigger">
                {{ userInfo.username }}
                <i class="el-icon-caret-bottom el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="logout">{{ $t("m.Logout") }}</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </el-row>
      </div>
    </div>

    <div v-else>
      <mu-appbar class="mobile-nav" color="primary">
        <mu-button icon slot="left" @click="opendrawer = !opendrawer">
          <i class="el-icon-s-unfold"></i>
        </mu-button>
        {{
        websiteConfig.shortName ? websiteConfig.shortName + " SIGNUP" : "SIGNUP"
        }}
        <mu-menu slot="right" v-show="isAuthenticated" :open.sync="openusermenu">
          <mu-button flat>
            {{ userInfo.username }}
            <i class="el-icon-caret-bottom"></i>
          </mu-button>
          <mu-list slot="content" @change="handleCommand">
            <mu-list-item button value="logout">
              <mu-list-item-content>
                <mu-list-item-title>{{ $t("m.Logout") }}</mu-list-item-title>
              </mu-list-item-content>
            </mu-list-item>
          </mu-list>
        </mu-menu>
      </mu-appbar>

      <mu-drawer :open.sync="opendrawer" :docked="false" :right="false">
        <mu-list toggle-nested>
          <mu-list-item
            button
            :ripple="true"
            nested
            to="/signup/dashboard"
            @click="opendrawer = !opendrawer"
            active-class="mobile-menu-active"
          >
            <mu-list-item-action>
              <mu-icon value=":fa fa-tachometer" size="24"></mu-icon>
            </mu-list-item-action>
            <mu-list-item-title>{{ $t("m.Dashboard") }}</mu-list-item-title>
          </mu-list-item>

          <mu-list-item
            button
            :ripple="false"
            nested
            :open="openSideMenu === 'user'"
            @toggle-nested="openSideMenu = arguments[0] ? 'user' : ''"
          >
            <mu-list-item-action>
              <mu-icon value=":fa el-icon-user-solid fa-size" size="24"></mu-icon>
            </mu-list-item-action>
            <mu-list-item-title>{{ $t("m.Account_Info") }}</mu-list-item-title>
            <mu-list-item-action>
              <mu-icon class="toggle-icon" size="24" value=":el-icon-arrow-down"></mu-icon>
            </mu-list-item-action>

            <mu-list-item
              button
              :ripple="false"
              slot="nested"
              to="/signup/user/account"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-title>
                {{
                $t("m.User_Info")
                }}
              </mu-list-item-title>
            </mu-list-item>
            <mu-list-item
              button
              :ripple="false"
              slot="nested"
              to="/signup/user/setting"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-title>
                {{
                $t("m.Account_Config")
                }}
              </mu-list-item-title>
            </mu-list-item>
          </mu-list-item>

          <mu-list-item
            button
            :ripple="false"
            nested
            :open="openSideMenu === 'pool'"
            @toggle-nested="openSideMenu = arguments[0] ? 'pool' : ''"
          >
            <mu-list-item-action>
              <mu-icon value=":fa fa-users fa-size" size="24"></mu-icon>
            </mu-list-item-action>
            <mu-list-item-title>{{ $t("m.Coach_Pool") }}</mu-list-item-title>
            <mu-list-item-action>
              <mu-icon class="toggle-icon" size="24" value=":el-icon-arrow-down"></mu-icon>
            </mu-list-item-action>

            <mu-list-item
              button
              :ripple="false"
              slot="nested"
              to="/signup/pool/user"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-title>
                {{
                $t("m.Coach_UserPool")
                }}
              </mu-list-item-title>
            </mu-list-item>
            <mu-list-item
              button
              :ripple="false"
              slot="nested"
              to="/signup/pool/team"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-title>
                {{
                $t("m.Coach_TeamPool")
                }}
              </mu-list-item-title>
            </mu-list-item>
          </mu-list-item>

          <mu-list-item
            button
            :ripple="true"
            nested
            to="/signup/contest"
            @click="opendrawer = !opendrawer"
            active-class="mobile-menu-active"
          >
            <mu-list-item-action>
              <mu-icon value=":fa fa-trophy fa-size" size="24"></mu-icon>
            </mu-list-item-action>
            <mu-list-item-title>{{ $t("m.Signup_ContestList") }}</mu-list-item-title>
          </mu-list-item>
        </mu-list>
      </mu-drawer>
    </div>

    <div
      class="content-app"
      :style="{ marginLeft: (!mobileNar ? (isCollapse ? '74px' : '220px') : '10px') }"
    >
      <transition name="fadeInUp" mode="out-in">
        <router-view v-if="!$route.meta.keepAlive"></router-view>
      </transition>
      <transition name="fadeInUp" mode="out-in">
        <keep-alive>
          <router-view v-if="$route.meta.keepAlive"></router-view>
        </keep-alive>
      </transition>
      <div class="footer">
        Powered by
        <a
          :href="websiteConfig.projectUrl"
          style="color: #1e9fff"
          target="_blank"
        >{{ websiteConfig.projectName }}</a>
        <span style="margin-left: 10px">
          <el-dropdown @command="changeWebLanguage" placement="top">
            <span class="el-dropdown-link" style="font-size: 14px">
              <i
                class="fa fa-globe"
                aria-hidden="true"
              >{{ this.webLanguage == "zh-CN" ? "简体中文" : "English" }}</i>
              <i class="el-icon-arrow-up el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="zh-CN">简体中文</el-dropdown-item>
              <el-dropdown-item command="en-US">English</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </span>
        <span style="margin-left: 10px">
          <el-dropdown @command="changeWebTheme" placement="top">
            <span class="el-dropdown-link">
              <i
                class="fa fa-globe"
                aria-hidden="true"
              >{{ this.webTheme == "Light" ? $t("m.Light") : $t("m.Dark") }}</i>
              <i class="el-icon-arrow-up el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="Light">{{ $t("m.Light") }}</el-dropdown-item>
              <el-dropdown-item command="Dark">{{ $t("m.Dark") }}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import api from "@/common/api";
import mMessage from "@/common/message";
import Avatar from "vue-avatar";
export default {
  name: "app",
  mounted() {
    this.currentPath = this.$route.path;
    this.getBreadcrumb();
    window.onresize = () => {
      this.page_width();
    };
    this.page_width();
  },
  data() {
    return {
      openusermenu: false,
      openSideMenu: "",
      katexVisible: false,
      opendrawer: false,
      mobileNar: false,
      currentPath: "",
      routeList: [],
      imgUrl: require("@/assets/backstage.png"),
      isCollapse: false,
    };
  },
  components: {
    Avatar,
  },
  methods: {
    handleCommand(command) {
      if (command === "logout") {
        api.signup_logout().then((res) => {
          this.$router.push({ path: "/signup/login" });
          mMessage.success(this.$i18n.t("m.Log_Out_Successfully"));
          this.$store.commit("clearUserInfoAndToken");
        });
      }
    },
    page_width() {
      let screenWidth = window.screen.width;
      if (screenWidth < 992) {
        this.mobileNar = true;
      } else {
        this.mobileNar = false;
      }
    },
    getBreadcrumb() {
      let matched = this.$route.matched.filter((item) => item.meta.title); //获取路由信息，并过滤保留路由标题信息存入数组
      this.routeList = matched;
    },
    changeWebLanguage(language) {
      this.$store.commit("changeWebLanguage", { language: language });
    },
    changeWebTheme(theme) {
      this.$store.commit("changeWebTheme", { theme: theme });
    },
    toggleCollapse() {
      this.isCollapse = !this.isCollapse;
    },
  },
  computed: {
    ...mapGetters([
      "userInfo",
      "isSuperAdmin",
      "isMainAdminRole",
      "isAdminRole",
      "isAuthenticated",
      "websiteConfig",
      "webLanguage",
      "webTheme",
      "isCoachAdmin",
    ]),
    "window.screen.width"(newVal, oldVal) {
      if (newVal < 992) {
        this.mobileNar = true;
      } else {
        this.mobileNar = false;
      }
    },
  },
  watch: {
    $route() {
      this.getBreadcrumb(); //监听路由变化
    },
  },
};
</script>

<style scoped>
.vertical_menu {
  overflow: auto;
  height: calc(100% - 80px);
  position: fixed !important;
  z-index: 100;
  top: 0;
  bottom: 0;
  left: 0;

  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}
.vertical_menu::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera */
}
.vertical_menu .logo {
  margin: 20px 0;
  text-align: center;
  cursor: pointer;
  height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.vertical_menu .logo img {
  background-color: #fff;
  border: 3px solid #fff;
}
.vertical_menu ::v-deep .el-submenu__title {
  display: flex;
  align-items: center;
}

.fa-size {
  text-align: center;
  font-size: 18px;
  vertical-align: middle;
  margin-right: 28px;
}
a {
  background-color: transparent;
}
a:active,
a:hover {
  outline-width: 0;
}

img {
  border-style: none;
}

.admin-container {
  overflow: auto;
  font-weight: 400;
  height: 100%;
  -webkit-font-smoothing: antialiased;
  background-color: #eff3f5;
  overflow-y: auto;
}
.breadcrumb-container {
  padding: 17px;
  background-color: #fff;
}
* {
  box-sizing: border-box;
}

#header {
  text-align: right;
  padding-right: 30px;
  line-height: 50px;
  height: 50px;
  background: #f9fafc;
}
.footer {
  margin: 15px;
  text-align: center;
  font-size: small;
}
@media screen and (max-width: 992px) {
  .content-app {
    padding: 0 5px;
    margin-top: 20px;
    margin-right: 15px;
  }
}
@media screen and (min-width: 992px) {
  .content-app {
    margin-top: 10px;
    margin-right: 10px;
    margin-left: calc(20% + 10px);
  }
}
@media screen and (min-width: 1150px) {
  .content-app {
    margin-top: 10px;
    margin-right: 10px;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translate(0, 30px);
  }

  to {
    opacity: 1;
    transform: none;
  }
}

.fadeInUp-enter-active {
  animation: fadeInUp 0.8s;
}

.katex-editor {
  margin-right: 5px;
  cursor: pointer;
  vertical-align: middle;
  margin-right: 10px;
}
.drop-avatar {
  vertical-align: middle;
  margin-right: 10px;
}
.dropdown-trigger {
  cursor: pointer;
}

.menu-header {
  display: flex;
  align-items: center;
  padding: 10px;
}

.toggle-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.menu-container {
  overflow: hidden;
}

/*
 * 过渡动画
 * - .menu-collapse-enter-active, .menu-collapse-leave-active：过渡过程
 * - .menu-collapse-enter, .menu-collapse-leave-to：进入/离开时的初始或结束状态
 */
.menu-collapse-enter-active,
.menu-collapse-leave-active {
  transition: width 0.2s ease;
}
.menu-collapse-enter,
.menu-collapse-leave-to {
  width: 64px;
}

.el-submenu__title {
  padding-left: 10px;
}
</style>
