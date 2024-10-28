<template>
  <div :key="modeKey">
    <template v-if="!mobileNar">
      <div id="header" :class="{ 'fixed-header': mode === 'training' || mode === 'contest' }">
        <div class="nyoj_logo">
          <router-link to="/" v-if="mode == 'defalut' || mode == 'group'">
            <el-image :src="big_imgUrl" style="width:100%;"></el-image>
          </router-link>
        </div>

        <el-menu
          :default-active="activeMenuName"
          mode="horizontal"
          router
          active-text-color="#2196f3"
          class="custom-menu"
        >
          <div class="logo">
            <el-tooltip :content="$t('m.Click_To_Home')" placement="bottom">
              <router-link to="/">
                <div class="group-title" v-if="mode == 'group'">{{ groupTitle }}</div>
                <div v-else>
                  <el-image style="width: 139px; height: 50px" :src="imgUrl" fit="scale-down"></el-image>
                </div>
              </router-link>
            </el-tooltip>
          </div>

          <template v-if="mode == 'defalut'">
            <el-menu-item index="/">
              <i class="el-icon-s-home"></i>
              {{ $t("m.NavBar_Home") }}
            </el-menu-item>
            <el-menu-item index="/announcement">
              <i class="el-icon-s-flag"></i>
              {{ $t("m.Announcement") }}
            </el-menu-item>
            <el-menu-item index="/problem">
              <i class="el-icon-s-grid"></i>
              {{ $t("m.NavBar_Problem") }}
            </el-menu-item>
            <el-menu-item index="/training">
              <i class="el-icon-s-claim"></i>
              {{ $t("m.NavBar_Training") }}
            </el-menu-item>
            <el-menu-item index="/contest">
              <i class="el-icon-trophy"></i>
              {{ $t("m.NavBar_Contest") }}
            </el-menu-item>
            <el-menu-item index="/submissions">
              <i class="el-icon-s-marketing"></i>
              {{ $t("m.NavBar_Status") }}
            </el-menu-item>
            <el-menu-item index="/group">
              <i class="fa fa-users navbar-icon"></i>
              {{ $t("m.NavBar_Group") }}
            </el-menu-item>
            <el-menu-item index="/discussion" v-if="websiteConfig.openPublicDiscussion">
              <i class="el-icon-s-comment"></i>
              {{ $t("m.NavBar_Discussion") }}
            </el-menu-item>
            <el-menu-item index="/honor">
              <i class="el-icon-medal"></i>
              {{ $t("m.NavBar_Honor") }}
            </el-menu-item>
            <el-submenu index="rank">
              <template slot="title">
                <i class="el-icon-s-data"></i>
                <span @click="goRank()">{{ $t("m.NavBar_Rank") }}</span>
              </template>
              <el-menu-item index="/acm-rank">
                {{
                $t("m.NavBar_ACM_Rank")
                }}
              </el-menu-item>
              <el-menu-item index="/oi-rank">
                {{
                $t("m.NavBar_OI_Rank")
                }}
              </el-menu-item>
              <el-menu-item index="/oj-rank">
                {{
                $t("m.NavBar_MultiOj_Rank")
                }}
              </el-menu-item>
              <el-menu-item index="/code-rank">
                {{
                $t("m.NavBar_Code_Rank")
                }}
              </el-menu-item>
            </el-submenu>
          </template>
          <template v-else-if="mode == 'group'">
            <el-menu-item :index="'/group/' + $route.params.groupID">
              <i class="fa fa-users navbar-icon"></i>
              {{ $t("m.NavBar_Group_Home") }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/announcement'"
              :disabled="groupMenuDisabled"
              v-if="isGroupAdmin"
            >
              <i class="el-icon-s-flag"></i>
              {{ $t("m.Announcement") }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/problem'"
              :disabled="groupMenuDisabled"
            >
              <i class="el-icon-s-grid"></i>
              {{ $t("m.NavBar_Problem") }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/training'"
              :disabled="groupMenuDisabled"
            >
              <i class="el-icon-s-claim"></i>
              {{ $t("m.NavBar_Training") }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/contest'"
              :disabled="groupMenuDisabled"
            >
              <i class="el-icon-trophy"></i>
              {{ $t("m.NavBar_Contest") }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/submissions'"
              :disabled="groupMenuDisabled"
            >
              <i class="el-icon-s-marketing"></i>
              {{ $t("m.NavBar_Status") }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/discussion'"
              v-if="websiteConfig.openPublicDiscussion"
              :disabled="groupMenuDisabled"
            >
              <i class="el-icon-s-comment"></i>
              {{ $t("m.NavBar_Discussion") }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/member'"
              :disabled="groupMenuDisabled"
            >
              <i class="el-icon-user-solid"></i>
              {{ $t('m.Group_Member') }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/setting'"
              :disabled="groupMenuDisabled"
              v-if="isGroupAdmin"
            >
              <i class="el-icon-s-tools"></i>
              {{ $t("m.Group_Setting") }}
            </el-menu-item>
            <el-menu-item
              :index="'/group/' + $route.params.groupID + '/rank'"
              :disabled="groupMenuDisabled"
            >
              <i class="el-icon-medal-1"></i>
              {{ $t('m.Group_Rank') }}
            </el-menu-item>
          </template>

          <template v-else-if="mode == 'training'">
            <el-menu-item :index="getTrainingHomePath">
              <i class="el-icon-s-claim"></i>
              {{ $t("m.NavBar_Training_Home") }}
            </el-menu-item>
            <el-menu-item :index="getTrainingProblemListPath">
              <i class="fa fa-list navbar-icon"></i>
              {{ $t("m.NavBar_Training_Problem") }}
            </el-menu-item>
            <el-menu-item :index="getTrainingSubmissionsPath">
              <i class="el-icon-menu"></i>
              {{ $t("m.NavBar_Training_Submission") }}
            </el-menu-item>
            <el-menu-item :index="getTrainingRankPath">
              <i class="fa fa-bar-chart navbar-icon"></i>
              {{ $t("m.NavBar_Training_Rank") }}
            </el-menu-item>
          </template>
          <template v-else-if="mode == 'contest'">
            <el-menu-item :index="getContestHomePath">
              <i class="el-icon-trophy"></i>
              {{ $t("m.NavBar_Contest_Home") }}
            </el-menu-item>
            <el-menu-item :index="getContestProblemPath">
              <i class="fa fa-list navbar-icon"></i>
              {{ $t("m.NavBar_Contest_Problem") }}
            </el-menu-item>
            <el-menu-item :index="getContestSubmissionsPath">
              <i class="el-icon-menu"></i>
              {{ $t("m.NavBar_Contest_Submission") }}
            </el-menu-item>
            <el-menu-item :index="getContestRankPath">
              <i class="fa fa-bar-chart navbar-icon"></i>
              {{ $t("m.NavBar_Contest_Rank") }}
            </el-menu-item>
            <el-menu-item :index="getContestAnnouncementPath">
              <i class="el-icon-s-flag navbar-icon"></i>
              {{ $t("m.NavBar_Contest_Announcement") }}
            </el-menu-item>
            <el-menu-item :index="getContestCommentPath">
              <i class="fa fa-commenting navbar-icon"></i>
              {{ $t("m.NavBar_Contest_Comment") }}
            </el-menu-item>
          </template>

          <div style="margin-left: auto;">
            <template v-if="!isAuthenticated">
              <div class="btn-menu">
                <el-button
                  type="primary"
                  size="medium"
                  round
                  @click="handleBtnClick('Login')"
                >{{ $t("m.NavBar_Login") }}</el-button>
                <el-button
                  v-if="websiteConfig.register"
                  size="medium"
                  round
                  @click="handleBtnClick('Register')"
                  style="margin-left: 5px"
                >{{ $t("m.NavBar_Register") }}</el-button>
              </div>
            </template>
            <template v-else-if="mode == 'defalut'">
              <el-dropdown
                class="drop-menu"
                @command="handleRoute"
                placement="bottom"
                trigger="hover"
              >
                <span class="el-dropdown-link" @click="goUserHome()">
                  {{ userInfo.username }}
                  <i class="el-icon-caret-bottom"></i>
                </span>

                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="/user-home">
                    {{
                    $t("m.NavBar_UserHome")
                    }}
                  </el-dropdown-item>
                  <el-dropdown-item command="/setting">
                    {{
                    $t("m.NavBar_Setting")
                    }}
                  </el-dropdown-item>
                  <el-dropdown-item v-if="isAdminRole" command="/admin">
                    {{
                    $t("m.NavBar_Management")
                    }}
                  </el-dropdown-item>
                  <div style="display: flex; justify-content: center; align-items: center;">
                    <el-dropdown-item divided command="/logout">
                      {{
                      $t("m.NavBar_Logout")
                      }}
                    </el-dropdown-item>
                  </div>
                </el-dropdown-menu>
              </el-dropdown>
              <avatar
                :username="userInfo.username"
                :inline="true"
                :size="30"
                color="#FFF"
                :src="avatar"
                class="drop-avatar"
              ></avatar>
              <el-dropdown class="drop-msg" @command="handleRoute" placement="bottom">
                <span class="el-dropdown-link">
                  <i class="el-icon-message-solid"></i>
                  <svg
                    v-if="
                    unreadMessage.comment > 0 ||
                    unreadMessage.reply > 0 ||
                    unreadMessage.like > 0 ||
                    unreadMessage.sys > 0 ||
                    unreadMessage.mine > 0 ||
                    unreadMessage.invent > 0
                  "
                    width="10"
                    height="10"
                    style="
                    vertical-align: top;
                    margin-left: -11px;
                    margin-top: 3px;
                  "
                  >
                    <circle cx="5" cy="5" r="5" style="fill: red" />
                  </svg>
                </span>

                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="/message/discuss">
                    <span>{{ $t("m.DiscussMsg") }}</span>
                    <span class="drop-msg-count" v-if="unreadMessage.comment > 0">
                      <MsgSvg :total="unreadMessage.comment"></MsgSvg>
                    </span>
                  </el-dropdown-item>
                  <el-dropdown-item command="/message/reply">
                    <span>{{ $t("m.ReplyMsg") }}</span>
                    <span class="drop-msg-count" v-if="unreadMessage.reply > 0">
                      <MsgSvg :total="unreadMessage.reply"></MsgSvg>
                    </span>
                  </el-dropdown-item>
                  <el-dropdown-item command="/message/like">
                    <span>{{ $t("m.LikeMsg") }}</span>
                    <span class="drop-msg-count" v-if="unreadMessage.like > 0">
                      <MsgSvg :total="unreadMessage.like"></MsgSvg>
                    </span>
                  </el-dropdown-item>
                  <el-dropdown-item command="/message/invent">
                    <span>{{ $t("m.InventMsg") }}</span>
                    <span class="drop-msg-count" v-if="unreadMessage.invent > 0">
                      <MsgSvg :total="unreadMessage.invent"></MsgSvg>
                    </span>
                  </el-dropdown-item>
                  <el-dropdown-item command="/message/sys">
                    <span>{{ $t("m.SysMsg") }}</span>
                    <span class="drop-msg-count" v-if="unreadMessage.sys > 0">
                      <MsgSvg :total="unreadMessage.sys"></MsgSvg>
                    </span>
                  </el-dropdown-item>
                  <el-dropdown-item command="/message/mine">
                    <span>{{ $t("m.MineMsg") }}</span>
                    <span class="drop-msg-count" v-if="unreadMessage.mine > 0">
                      <MsgSvg :total="unreadMessage.mine"></MsgSvg>
                    </span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </template>
            <template v-else-if="mode == 'contest'">
              <el-tag class="drop-avatar" size="large" :style="countdownColor()">
                <i class="fa fa-hourglass-end" aria-hidden="true"></i>
                <span
                  style="font-weight: bold; font-size: 15px;"
                >{{ " " + $t('m.EndAt') }}：{{ contest.endTime | localtime }}</span>
              </el-tag>
            </template>
          </div>
        </el-menu>
      </div>
      <!-- <div id="header-hidden" v-show="isScrolled"></div> -->
    </template>
    <template v-else>
      <div style="top: 0px; left: 0px">
        <mu-appbar class="mobile-nav" color="primary">
          <mu-button icon slot="left" @click="opendrawer = !opendrawer">
            <i class="el-icon-s-unfold"></i>
          </mu-button>
          <el-tooltip :content="$t('m.Click_To_Home')" placement="bottom" effect="dark">
            <router-link to="/">{{ websiteConfig.shortName ? websiteConfig.shortName : "OJ" }}</router-link>
          </el-tooltip>
          <mu-button
            flat
            slot="right"
            @click="handleBtnClick('Login')"
            v-show="!isAuthenticated"
          >{{ $t("m.NavBar_Login") }}</mu-button>
          <mu-button
            flat
            slot="right"
            @click="handleBtnClick('Register')"
            v-show="!isAuthenticated && websiteConfig.register"
          >{{ $t("m.NavBar_Register") }}</mu-button>

          <mu-menu slot="right" v-show="isAuthenticated" :open.sync="openmsgmenu">
            <mu-button flat>
              <mu-icon value=":el-icon-message-solid" size="24"></mu-icon>
              <svg
                v-if="
                  unreadMessage.comment > 0 ||
                  unreadMessage.reply > 0 ||
                  unreadMessage.like > 0 ||
                  unreadMessage.sys > 0 ||
                  unreadMessage.mine > 0 ||
                  unreadMessage.invent > 0
                "
                width="10"
                height="10"
                style="margin-left: -11px; margin-top: -13px"
              >
                <circle cx="5" cy="5" r="5" style="fill: red" />
              </svg>
            </mu-button>
            <mu-list slot="content" @change="handleCommand">
              <mu-list-item button value="/message/discuss">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{ $t("m.DiscussMsg") }}
                    <span
                      class="drop-msg-count"
                      v-if="unreadMessage.comment > 0"
                    >
                      <MsgSvg :total="unreadMessage.comment"></MsgSvg>
                    </span>
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
              <mu-divider></mu-divider>
              <mu-list-item button value="/message/reply">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{ $t("m.ReplyMsg") }}
                    <span
                      class="drop-msg-count"
                      v-if="unreadMessage.reply > 0"
                    >
                      <MsgSvg :total="unreadMessage.reply"></MsgSvg>
                    </span>
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
              <mu-divider></mu-divider>
              <mu-list-item button value="/message/like">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{ $t("m.LikeMsg") }}
                    <span class="drop-msg-count" v-if="unreadMessage.like > 0">
                      <MsgSvg :total="unreadMessage.like"></MsgSvg>
                    </span>
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
              <mu-divider></mu-divider>
              <mu-list-item button value="/message/invent">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{ $t("m.InventMsg") }}
                    <span
                      class="drop-msg-count"
                      v-if="unreadMessage.invent > 0"
                    >
                      <MsgSvg :total="unreadMessage.invent"></MsgSvg>
                    </span>
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
              <mu-list-item button value="/message/sys">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{ $t("m.SysMsg") }}
                    <span class="drop-msg-count" v-if="unreadMessage.sys > 0">
                      <MsgSvg :total="unreadMessage.sys"></MsgSvg>
                    </span>
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
              <mu-divider></mu-divider>

              <mu-list-item button value="/message/mine">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{ $t("m.MineMsg") }}
                    <span class="drop-msg-count" v-if="unreadMessage.mine > 0">
                      <MsgSvg :total="unreadMessage.mine"></MsgSvg>
                    </span>
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
            </mu-list>
          </mu-menu>

          <mu-menu slot="right" v-if="isAuthenticated" :open.sync="openusermenu">
            <mu-button flat>
              <avatar
                :username="userInfo.username"
                :inline="true"
                :size="30"
                color="#FFF"
                :src="avatar"
                :title="userInfo.username"
              ></avatar>
              <i class="el-icon-caret-bottom"></i>
            </mu-button>
            <mu-list slot="content" @change="handleCommand">
              <mu-list-item button value="/user-home">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{
                    $t("m.NavBar_UserHome")
                    }}
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
              <mu-divider></mu-divider>
              <mu-list-item button value="/setting">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{
                    $t("m.NavBar_Setting")
                    }}
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
              <mu-divider></mu-divider>
              <mu-list-item button value="/admin" v-show="isAdminRole">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{
                    $t("m.NavBar_Management")
                    }}
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
              <mu-divider></mu-divider>

              <mu-list-item button value="/logout">
                <mu-list-item-content>
                  <mu-list-item-title>
                    {{
                    $t("m.NavBar_Logout")
                    }}
                  </mu-list-item-title>
                </mu-list-item-content>
              </mu-list-item>
            </mu-list>
          </mu-menu>
        </mu-appbar>

        <mu-appbar style="width: 100%">
          <!--占位，刚好占领导航栏的高度-->
        </mu-appbar>

        <mu-drawer :open.sync="opendrawer" :docked="false" :right="false">
          <mu-list toggle-nested v-if="mode == 'defalut'">
            <mu-list-item
              button
              to="/"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-home" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>{{ $t("m.NavBar_Home") }}</mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              to="/announcement"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-flag" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.Announcement")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              to="/problem"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-grid" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Problem")
                }}
              </mu-list-item-title>
            </mu-list-item>
            <mu-list-item
              button
              to="/training"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-claim" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Training")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              to="/contest"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-trophy" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Contest")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              to="/submissions"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-marketing" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Status")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              to="/group"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":fa fa-users" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Group")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              v-if="websiteConfig.openPublicDiscussion"
              button
              to="/discussion"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":fa fa-comments" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Discussion")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              to="/honor"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-medal" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Honor")
                }}
              </mu-list-item-title>
            </mu-list-item>
            <mu-list-item
              button
              :ripple="false"
              nested
              :open="openSideMenu === 'rank'"
              @toggle-nested="openSideMenu = arguments[0] ? 'rank' : ''"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-data" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>{{ $t("m.NavBar_Rank") }}</mu-list-item-title>
              <mu-list-item-action>
                <mu-icon class="toggle-icon" size="24" value=":el-icon-arrow-down"></mu-icon>
              </mu-list-item-action>
              <mu-list-item
                button
                :ripple="false"
                slot="nested"
                to="/acm-rank"
                @click="opendrawer = !opendrawer"
                active-class="mobile-menu-active"
              >
                <mu-list-item-title>
                  {{
                  $t("m.NavBar_ACM_Rank")
                  }}
                </mu-list-item-title>
              </mu-list-item>
              <mu-list-item
                button
                :ripple="false"
                slot="nested"
                to="/oi-rank"
                @click="opendrawer = !opendrawer"
                active-class="mobile-menu-active"
              >
                <mu-list-item-title>
                  {{
                  $t("m.NavBar_OI_Rank")
                  }}
                </mu-list-item-title>
              </mu-list-item>
              <mu-list-item
                button
                :ripple="false"
                slot="nested"
                to="/oj-rank"
                @click="opendrawer = !opendrawer"
                active-class="mobile-menu-active"
              >
                <mu-list-item-title>
                  {{
                  $t("m.NavBar_MultiOj_Rank")
                  }}
                </mu-list-item-title>
              </mu-list-item>
              <mu-list-item
                button
                :ripple="false"
                slot="nested"
                to="/code-rank"
                @click="opendrawer = !opendrawer"
                active-class="mobile-menu-active"
              >
                <mu-list-item-title>
                  {{
                  $t("m.NavBar_Code_Rank")
                  }}
                </mu-list-item-title>
              </mu-list-item>
            </mu-list-item>
          </mu-list>

          <mu-list toggle-nested v-if="mode == 'group'">
            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
            >
              <mu-list-item-action>
                <mu-icon value=":fa fa-users navbar-icon" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>{{ $t("m.NavBar_Group_Home") }}</mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID + '/announcement'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
              v-if="isGroupAdmin"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-flag" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.Announcement")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID + '/problem'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-grid" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Problem")
                }}
              </mu-list-item-title>
            </mu-list-item>
            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID + '/training'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-claim" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Training")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID + '/contest'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-trophy" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Contest")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID + '/submissions'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-marketing" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Status")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              v-if="websiteConfig.openPublicDiscussion"
              button
              :to="'/group/' + $route.params.groupID + '/discussion'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
            >
              <mu-list-item-action>
                <mu-icon value=":fa fa-comments" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.NavBar_Discussion")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID + '/member'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-user-solid" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.Group_Member")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID + '/setting'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-s-tools" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.Group_Setting")
                }}
              </mu-list-item-title>
            </mu-list-item>

            <mu-list-item
              button
              :to="'/group/' + $route.params.groupID + '/rank'"
              @click="opendrawer = !opendrawer"
              active-class="mobile-menu-active"
              :disabled="groupMenuDisabled"
            >
              <mu-list-item-action>
                <mu-icon value=":el-icon-medal-1" size="24"></mu-icon>
              </mu-list-item-action>
              <mu-list-item-title>
                {{
                $t("m.Group_Rank")
                }}
              </mu-list-item-title>
            </mu-list-item>
          </mu-list>
        </mu-drawer>
      </div>
    </template>

    <el-dialog
      :visible.sync="modalVisible"
      width="370px"
      class="dialog"
      :title="title"
      :close-on-click-modal="false"
    >
      <component :is="modalStatus.mode" v-if="modalVisible"></component>
      <div slot="footer" style="display: none"></div>
    </el-dialog>
  </div>
</template>
<script>
import Login from "@/components/oj/common/Login";
import ContestAccountLogin from "@/components/oj/common/ContestAccountLogin";
import Register from "@/components/oj/common/Register";
import ResetPwd from "@/components/oj/common/ResetPassword";
import MsgSvg from "@/components/oj/msg/msgSvg";
import { mapGetters, mapActions } from "vuex";
import Avatar from "vue-avatar";
import api from "@/common/api";
import { CONTEST_STATUS_REVERSE } from "@/common/constants";

export default {
  components: {
    Login,
    ContestAccountLogin,
    Register,
    ResetPwd,
    Avatar,
    MsgSvg,
  },
  created() {
    this.page_width();
    window.onresize = () => {
      this.page_width();
      this.setHiddenHeaderHeight();
    };
    this.CONTEST_STATUS_REVERSE = Object.assign({}, CONTEST_STATUS_REVERSE);
  },
  mounted() {
    this.switchMode();
    this.setHiddenHeaderHeight();
    if (this.isAuthenticated) {
      this.getUnreadMsgCount();
      this.addSession();
      this.msgTimer = setInterval(() => {
        this.getUnreadMsgCount();
      }, 120 * 1000);
      this.msgTimer2 = setInterval(() => {
        this.addSession();
      }, 20 * 1000);
    }
  },
  beforeDestroy() {
    clearInterval(this.msgTimer);
    clearInterval(this.msgTimer2);
  },
  data() {
    return {
      mode: "defalut",
      centerDialogVisible: false,
      mobileNar: false,
      opendrawer: false,
      openusermenu: false,
      openmsgmenu: false,
      openSideMenu: "",
      big_imgUrl: require("@/assets/nyoj-logo.png"),
      imgUrl: require("@/assets/logo.png"),
      avatarStyle:
        "display: inline-flex;width: 30px;height: 30px;border-radius: 50%;align-items: center;justify-content: center;text-align: center;user-select: none;",
      CONTEST_STATUS_REVERSE: {},
      contest: {},
      groupTitle: null,
    };
  },
  methods: {
    ...mapActions(["changeModalStatus"]),
    page_width() {
      let screenWidth = window.screen.width;
      if (screenWidth < 992) {
        this.mobileNar = true;
      } else {
        this.mobileNar = false;
      }
    },
    handleBtnClick(mode) {
      this.changeModalStatus({
        mode,
        visible: true,
      });
    },
    handleRoute(route) {
      //电脑端导航栏路由跳转事件
      if (route && route.split("/")[1] != "admin") {
        this.$router.push(route);
      } else {
        window.open("/admin/");
      }
    },
    handleCommand(route) {
      // 移动端导航栏路由跳转事件
      this.openusermenu = false;
      this.openmsgmenu = false;
      if (route && route.split("/")[1] != "admin") {
        this.$router.push(route);
      } else {
        window.open("/admin/");
      }
    },
    addSession() {
      this.routeName = this.$route.name;
      let data = {
        routeName: this.routeName,
      };
      if (
        this.userInfo.roleList.includes("contest_account") ||
        this.userInfo.roleList.includes("team_contest_account")
      ) {
        api.addSession(data).then(
          (res) => {},
          (_) => {}
        );
      }
    },
    getUnreadMsgCount() {
      api.getUnreadMsgCount().then((res) => {
        let data = res.data.data;
        this.$store.dispatch("updateUnreadMessageCount", data);
        let sumMsg =
          data.comment + data.reply + data.like + data.mine + data.sys;
        if (sumMsg > 0) {
          if (this.webLanguage == "zh-CN") {
            this.$notify.info({
              title: "未读消息",
              message:
                "亲爱的【" +
                this.userInfo.username +
                "】，您有最新的" +
                sumMsg +
                "条未读消息，请注意查看！",
              position: "bottom-right",
              duration: 5000,
            });
          } else {
            this.$notify.info({
              title: "Unread Message",
              message:
                "Dear【" +
                this.userInfo.username +
                "】, you have the latest " +
                sumMsg +
                " unread messages. Please check them!",
              position: "bottom-right",
              duration: 5000,
            });
          }
        }
      });
    },
    changeWebLanguage() {
      this.$store.commit("changeWebLanguage", {
        language: this.webLanguage == "zh-CN" ? "en-US" : "zh-CN",
      });
    },
    setHiddenHeaderHeight() {
      if (!this.mobileNar) {
        try {
          let headerHeight = document.getElementById("header").offsetHeight;
          document
            .getElementById("header-hidden")
            .setAttribute("style", "height:" + headerHeight + "px");
        } catch (e) {}
      }
    },
    switchMode() {
      if (this.$route.meta.fullScreenSource) {
        this.mode = this.$route.meta.fullScreenSource;
      } else {
        this.mode = "defalut";
      }
      this.contestID = this.$route.params.contestID;

      if (this.contestID) {
        this.$store.dispatch("getContest").then((res) => {
          this.contest = res.data.data;
        });
      }
      this.groupID = this.$route.params.groupID;
      if (this.groupID) {
        this.$store.dispatch("getGroup").then((res) => {
          this.groupTitle = res.data.data.name;
        });
      }
    },
    goUserHome() {
      const routeName = this.$route.params.groupID
        ? "GroupUserHome"
        : "UserHome";
      this.$router.push({
        name: routeName,
      });
    },
    goRank() {
      this.$router.push({
        name: "ACM Rank",
      });
    },
    countdownColor() {
      return "color:" + this.CONTEST_STATUS_REVERSE[this.contest.status].color;
    },
  },
  computed: {
    ...mapGetters([
      "modalStatus",
      "userInfo",
      "isAuthenticated",
      "isAdminRole",
      "token",
      "websiteConfig",
      "unreadMessage",
      "webLanguage",
      "groupMenuDisabled",
      "isGroupAdmin",
    ]),
    avatar() {
      return this.$store.getters.userInfo.avatar;
    },
    activeMenuName() {
      let path = this.$route.path;
      const { contestID, trainingID, groupID } = this.$route.params;

      const isContest = path.includes("/contest");
      const isTraining = path.includes("/training");
      const isGroup = path.startsWith("/group");

      const groupPrefix = isGroup ? `/group/${groupID}` : "";

      path = path
        .replace(/\/submission-detail.*$/, "/submissions")
        .replace(/\/discussion-detail.*$/, "/discussion");

      if (path.includes("/full-screen/problem")) {
        if (isContest)
          return `${groupPrefix}/contest/${contestID}/full-screen/problem`;
        if (isTraining)
          return `${groupPrefix}/training/${trainingID}/full-screen/problem`;
      }

      if (path.includes("/full-screen")) {
        return path;
      }

      return isGroup
        ? path.split("/").slice(0, 4).join("/")
        : "/" + path.split("/")[1];
    },
    modeKey() {
      return `mode-${this.mode}`; // 用mode的值作为key
    },
    modalVisible: {
      get() {
        return this.modalStatus.visible;
      },
      set(value) {
        this.changeModalStatus({ visible: value });
      },
    },
    title: {
      get() {
        let ojName = this.websiteConfig.shortName
          ? this.websiteConfig.shortName
          : "OJ";
        if (this.modalStatus.mode == "ResetPwd") {
          return this.$i18n.t("m.Dialog_Reset_Password") + " - " + ojName;
        } else {
          return (
            this.$i18n.t("m.Dialog_" + this.modalStatus.mode) + " - " + ojName
          );
        }
      },
    },
    getTrainingHomePath() {
      let tid = this.$route.params.trainingID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/training/${tid}`;
      } else {
        return `/training/${tid}`;
      }
    },
    getTrainingProblemListPath() {
      let tid = this.$route.params.trainingID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/training/${tid}/full-screen/problem`;
      } else {
        return `/training/${tid}/full-screen/problem`;
      }
    },
    getTrainingSubmissionDetailPath() {
      let tid = this.$route.params.trainingID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/training/${tid}/full-screen/submission-detail`;
      } else {
        return `/training/${tid}/full-screen/submission-detail`;
      }
    },
    getTrainingSubmissionsPath() {
      let tid = this.$route.params.trainingID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/training/${tid}/full-screen/submissions`;
      } else {
        return `/training/${tid}/full-screen/submissions`;
      }
    },
    getTrainingRankPath() {
      let tid = this.$route.params.trainingID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/training/${tid}/full-screen/rank`;
      } else {
        return `/training/${tid}/full-screen/rank`;
      }
    },

    getContestHomePath() {
      let cid = this.$route.params.contestID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/contest/${cid}`;
      } else {
        return `/contest/${cid}`;
      }
    },
    getContestProblemPath() {
      let cid = this.$route.params.contestID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/contest/${cid}/full-screen/problem`;
      } else {
        return `/contest/${cid}/full-screen/problem`;
      }
    },
    getContestSubmissionsPath() {
      let cid = this.$route.params.contestID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/contest/${cid}/full-screen/submissions`;
      } else {
        return `/contest/${cid}/full-screen/submissions`;
      }
    },
    getContestRankPath() {
      let cid = this.$route.params.contestID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/contest/${cid}/full-screen/rank`;
      } else {
        return `/contest/${cid}/full-screen/rank`;
      }
    },
    getContestAnnouncementPath() {
      let cid = this.$route.params.contestID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/contest/${cid}/full-screen/announcement`;
      } else {
        return `/contest/${cid}/full-screen/announcement`;
      }
    },
    getContestCommentPath() {
      let cid = this.$route.params.contestID;
      let gid = this.$route.params.groupID;
      if (gid) {
        return `/group/${gid}/contest/${cid}/full-screen/comment`;
      } else {
        return `/contest/${cid}/full-screen/comment`;
      }
    },
  },
  watch: {
    isAuthenticated() {
      if (this.isAuthenticated) {
        if (this.msgTimer) {
          clearInterval(this.msgTimer);
        }
        if (this.msgTimer2) {
          clearInterval(this.msgTimer2);
        }
        this.getUnreadMsgCount();
        this.addSession();
        this.msgTimer = setInterval(() => {
          this.getUnreadMsgCount();
        }, 120 * 1000);
        // 每20秒检测session
        this.msgTimer2 = setInterval(() => {
          this.addSession();
        }, 20 * 1000);
      } else {
        clearInterval(this.msgTimer);
        clearInterval(this.msgTimer2);
      }
    },
    $route: {
      immediate: true,
      handler() {
        this.switchMode();
      },
    },
  },
};
</script>
<style scoped>
.fixed-header {
  min-width: 300px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  margin-left: auto;
  margin-right: auto;
  height: auto;
  z-index: 2000;
  background-color: #fff;
  box-shadow: 0 1px 5px 0 rgba(0, 0, 0, 0.1);
}

@media screen and (min-width: 1050px) {
  #header {
    min-width: 300px;
    /* position: fixed; */
    top: 0;
    left: 0;
    height: auto;
    width: 80%;
    margin: 0 auto;
    z-index: 2000;
    /* background-color: #fff; */
    box-shadow: 0 1px 5px 0 rgba(0, 0, 0, 0.1);
  }
}

@media screen and (max-width: 1050px) {
  #header {
    min-width: 300px;
    top: 0;
    left: 0;
    height: auto;
    width: 100%;
    margin: 0 auto;
    z-index: 2000;
    box-shadow: 0 1px 5px 0 rgba(0, 0, 0, 0.1);
  }
}
.custom-menu .el-menu-item {
  width: auto;
  text-align: center;
}

.custom-menu .spacer {
  visibility: hidden;
}
.mobile-nav {
  position: fixed;
  left: 0px;
  top: 0px;
  z-index: 2500;
  height: auto;
  width: 100%;
}

#drawer {
  position: fixed;
  left: 0px;
  bottom: 0px;
  z-index: 1000;
  width: 100%;
  /* box-shadow: 00px 0px 00px rgb(255, 255, 255), 0px 0px 10px rgb(255, 255, 255),
    0px 0px 0px rgb(255, 255, 255), 1px 1px 0px rgb(218, 218, 218); */
}

.nyoj_logo {
  width: 100%;
}
.logo {
  cursor: pointer;
  margin-left: 1%;
  margin-right: 2%;
  float: left;
  width: 110px;
  height: 42px;
  margin-top: 5px;
}
.el-dropdown-link {
  cursor: pointer;
  color: #409eff !important;
}
.el-icon-arrow-down {
  font-size: 18px;
}
.drop-menu {
  float: right;
  margin-right: 30px;
  position: relative;
  font-weight: 500;
  right: 10px;
  margin-top: 18px;
  font-size: 18px;
}
.drop-avatar {
  float: right;
  margin-right: 15px;
  position: relative;
  margin-top: 16px;
}
.drop-msg {
  float: right;
  font-size: 25px;
  margin-right: 10px;
  position: relative;
  margin-top: 13px;
}
.drop-msg-count {
  margin-left: 2px;
}
.btn-menu {
  font-size: 16px;
  float: right;
  margin-right: 10px;
  margin-top: 12px;
}
/deep/ .el-dialog {
  border-radius: 10px !important;
  text-align: center;
}
/deep/ .el-dialog__header .el-dialog__title {
  font-size: 22px;
  font-weight: 600;
  font-family: Arial, Helvetica, sans-serif;
  line-height: 1em;
  color: #4e4e4e;
}
.el-menu-item {
  padding: 0 13px;
}
.el-menu-item:hover,
.el-menu .el-menu-item:hover {
  border-bottom: 2px solid #2474b5 !important;
}
.el-menu .el-menu-item:hover,
.el-menu .el-menu-item:hover i,
.el-submenu .el-submenu__title:hover,
.el-submenu .el-submenu__title:hover i {
  outline: 0 !important;
  color: #2e95fb !important;
  background: linear-gradient(270deg, #f2f7fc 0%, #fefefe 100%) !important;
  transition: all 0.2s ease;
}
.el-menu .el-menu-item.is-active,
.el-menu .el-menu-item.is-active i,
.el-submenu.is-active,
.el-submenu.is-active i {
  color: #2e95fb !important;
  background: linear-gradient(270deg, #f2f7fc 0%, #fefefe 100%) !important;
  transition: all 0.2s ease;
}
.el-menu--horizontal .el-menu .el-menu-item:hover,
.el-submenu /deep/.el-submenu__title:hover {
  color: #2e95fb !important;
  background: linear-gradient(270deg, #f2f7fc 0%, #fefefe 100%) !important;
}
.el-menu-item i {
  color: #495060;
}
.is-active .el-submenu__title i,
.is-active {
  color: #2196f3 !important;
}
.el-menu-item.is-active i {
  color: #2196f3 !important;
}
.navbar-icon {
  margin-right: 5px !important;
  width: 24px !important;
  text-align: center !important;
}
.group-title {
  display: flex;
  justify-content: center;
  align-items: center;
  color: #555555;
  font-weight: bold;
  font-style: italic;
  letter-spacing: 0.1em;
  text-shadow: -1px -1px 1px #111111, 2px 2px 1px #363636, -2px -2px 1px #ffffff,
    2px 2px 1px #000000, 3px 3px 2px rgba(0, 0, 0, 0.6),
    5px 5px 4px rgba(0, 0, 0, 0.3);
  width: 100%;
  height: 100%;
  text-align: center;
  font-size: 1.2vw;
  line-height: 1;
}
</style>
