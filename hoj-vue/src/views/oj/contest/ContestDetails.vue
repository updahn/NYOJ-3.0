<template>
  <div class="contest-body">
    <el-row>
      <el-col :xs="24" :md="24" :lg="24">
        <el-card shadow>
          <div class="contest-title">
            <div slot="header">
              <span class="panel-title">{{ contest.title }}</span>
            </div>
          </div>
          <el-row style="margin-top: 10px;">
            <el-col :span="14" class="text-align:left">
              <el-tooltip
                v-if="contest.auth != null && contest.auth != undefined"
                :content="$t('m.' + CONTEST_TYPE_REVERSE[contest.auth]['tips'])"
                placement="top"
              >
                <el-tag
                  :type.sync="CONTEST_TYPE_REVERSE[contest.auth]['color']"
                  effect="plain"
                  style="font-size:13px"
                >
                  <i class="el-icon-collection-tag"></i>
                  {{ $t('m.' + CONTEST_TYPE_REVERSE[contest.auth]['name']) }}
                </el-tag>
              </el-tooltip>
              <el-tooltip
                v-if="contest.gid != null"
                :content="$t('m.Go_To_Group_Contest_List')"
                style="margin-left:10px;"
                placement="top"
              >
                <el-button size="small" type="primary" @click="toGroupContestList(contest.gid)">
                  <i class="fa fa-users"></i>
                  {{ $t('m.Group_Contest_Tag')}}
                </el-button>
              </el-tooltip>
            </el-col>
            <el-col :span="10" style="text-align:right">
              <el-button size="small" plain v-if="contest.count != null">
                <i class="el-icon-user-solid" style="color:rgb(48, 145, 242);"></i>
                x{{ contest.count }}
              </el-button>
              <template v-if="contest.type == 0">
                <el-button size="small" :type="'primary'">
                  <i class="fa fa-trophy"></i>
                  {{ contest.type | parseContestType }}
                </el-button>
              </template>
              <template v-else-if="contest.type == 1">
                <el-tooltip
                  :content="
                    $t('m.Contest_Rank') +
                      '：' +
                      (contest.oiRankScoreType == 'Recent'
                        ? $t(
                            'm.Based_on_The_Recent_Score_Submitted_Of_Each_Problem'
                          )
                        : $t(
                            'm.Based_on_The_Highest_Score_Submitted_For_Each_Problem'
                          ))
                  "
                  placement="top"
                >
                  <el-button size="small" :type="'warning'">
                    <i class="fa fa-trophy"></i>
                    {{ contest.type | parseContestType }}
                  </el-button>
                </el-tooltip>
              </template>
              <template v-else>
                <el-tooltip :content="$t('m.Examination_Tips')" placement="top" effect="light">
                  <el-button size="small" :type="'danger'">
                    <i class="fa fa-trophy"></i>
                    {{ contest.type | parseContestType }}
                  </el-button>
                </el-tooltip>
              </template>
            </el-col>
          </el-row>
          <div class="contest-time">
            <el-row>
              <el-col :xs="24" :md="12" class="left">
                <p>
                  <i class="fa fa-hourglass-start" aria-hidden="true"></i>
                  {{ $t('m.StartAt') }}：{{ contest.startTime | localtime }}
                </p>
              </el-col>
              <el-col :xs="24" :md="12" class="right">
                <p>
                  <i class="fa fa-hourglass-end" aria-hidden="true"></i>
                  {{ $t('m.EndAt') }}：{{ contest.endTime | localtime }}
                </p>
              </el-col>
            </el-row>
          </div>
          <div style="width: 100%">
            <Timebar
              :progressValue="progressValue"
              :Tooltip="formatTooltip()"
              @transfer="getStopFlag"
            />
            <p></p>
          </div>
          <el-row>
            <el-col :span="24" style="text-align:center">
              <el-tag effect="dark" size="medium" :style="countdownColor">
                <i class="fa fa-circle" aria-hidden="true"></i>
                {{ countdown }}
              </el-tag>
            </el-col>
          </el-row>
          <div class="contest-config" style="display: flex;">
            <div
              v-if="contest.auth == CONTEST_TYPE.OFFICIAL"
              class="config"
              style="margin-right: 10px;"
            >
              <el-button
                round
                size="small"
                slot="reference"
                @click="toSignupList(contest.title)"
              >{{$t('m.Signup_List')}}</el-button>
            </div>
            <div v-if="isShowContestSetting" class="config" style="margin-right: 10px;">
              <el-popover trigger="hover" placement="left-start">
                <el-button round size="small" slot="reference">{{$t('m.Contest_Setting')}}</el-button>
                <div class="contest-config-switches">
                  <p>
                    <span>{{ $t('m.Contains_Submission_After_Contest') }}</span>
                    <el-switch v-model="isContainsAfterContestJudge"></el-switch>
                  </p>
                </div>
              </el-popover>
            </div>
            <div
              class="config"
              v-if="contest.pdfDescription && isShowContestPdf"
              style="margin-right: 10px;"
            >
              <el-button
                round
                size="small"
                slot="reference"
                @click="openPdf(contest.pdfDescription)"
              >{{$t('m.Contest_PDF')}}</el-button>
            </div>
            <div class="admin">
              <el-button
                v-if="isContestAdmin"
                round
                size="small"
                slot="reference"
                @click="toAdminContest(contest.gid, contest.title)"
              >{{$t('m.To_Admin_Background')}}</el-button>
            </div>

            <span class="hidden-sm-and-down" style="margin-left: 10px;">
              <el-tooltip :content="$t('m.Enter_Focus_Mode')" placement="bottom">
                <el-button icon="el-icon-full-screen" @click="switchFocusMode" size="small"></el-button>
              </el-tooltip>
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <Announcement></Announcement>
    <div class="sub-menu">
      <el-tabs @tab-click="tabClick" v-model="route_name">
        <el-tab-pane name="Details" lazy>
          <span slot="label">
            <i class="el-icon-s-home"></i>
            &nbsp;{{ $t('m.Overview') }}
          </span>
          <!-- 判断是否需要密码验证 -->
          <el-card
            v-if="passwordFormVisible"
            class="password-form-card"
            style="text-align:center;margin-bottom:15px"
          >
            <div slot="header">
              <span class="panel-title" style="color: #e6a23c;">
                <i class="el-icon-warning">{{ $t('m.Password_Required') }}</i>
              </span>
            </div>
            <p class="password-form-tips">{{ $t('m.To_Enter_Need_Password') }}</p>
            <el-form>
              <el-input
                v-model="contestPassword"
                type="password"
                show-password
                :placeholder="$t('m.Enter_the_contest_password')"
                @keydown.enter.native="checkPassword"
                style="width:70%"
              />
              <el-button
                type="primary"
                @click="checkPassword"
                style="float:right;"
              >{{ $t('m.Enter') }}</el-button>
            </el-form>
          </el-card>

          <el-card class="box-card">
            <Markdown :isAvoidXss="contest.gid != null" :content="contest.description"></Markdown>
          </el-card>
          <div v-if="contest.openFile">
            <box-file :isAdmin="false" :cid="contest.id"></box-file>
          </div>
        </el-tab-pane>

        <el-tab-pane name="ProblemList" lazy :disabled="contestMenuDisabled">
          <span slot="label">
            <i class="fa fa-list" aria-hidden="true"></i>
            &nbsp;{{
            $t('m.Problem')
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'ProblemList'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane name="SubmissionList" lazy :disabled="contestMenuDisabled">
          <span slot="label">
            <i class="el-icon-menu"></i>
            &nbsp;{{ $t('m.Status') }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'SubmissionList'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane
          v-if="(contest.auth === CONTEST_TYPE.EXAMINATION && isContestAdmin) || (contest.auth !== CONTEST_TYPE.EXAMINATION)"
          name="Rank"
          lazy
          :disabled="contestMenuDisabled"
        >
          <span slot="label">
            <i class="fa fa-bar-chart" aria-hidden="true"></i>
            &nbsp;{{
            $t('m.NavBar_Rank')
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'Rank'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane name="AnnouncementList" lazy :disabled="contestMenuDisabled">
          <span slot="label">
            <i class="fa fa-bullhorn" aria-hidden="true"></i>
            &nbsp;{{
            $t('m.Announcement')
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'AnnouncementList'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane
          name="Comment"
          lazy
          :disabled="contestMenuDisabled"
          v-if="websiteConfig.openContestComment"
        >
          <span slot="label">
            <i class="fa fa-commenting" aria-hidden="true"></i>
            &nbsp;{{
            $t('m.Comment')
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'Comment'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane name="Print" lazy :disabled="contestMenuDisabled" v-if="contest.openPrint">
          <span slot="label">
            <i class="el-icon-printer"></i>
            &nbsp;{{ $t('m.Print') }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'Print'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane
          name="AdminPrint"
          lazy
          :disabled="contestMenuDisabled"
          v-if="isContestAdmin && contest.openPrint"
        >
          <span slot="label">
            <i class="el-icon-printer"></i>
            &nbsp;{{
            $t('m.Admin_Print')
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'AdminPrint'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane name="ACInfo" lazy :disabled="contestMenuDisabled" v-if="showAdminHelper">
          <span slot="label">
            <i class="el-icon-s-help" aria-hidden="true"></i>
            &nbsp;{{
            $t('m.Admin_Helper')
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'ACInfo'"></router-view>
          </transition>
        </el-tab-pane>
        <el-tab-pane name="RejudgeAdmin" lazy :disabled="contestMenuDisabled" v-if="isContestAdmin">
          <span slot="label">
            <i class="el-icon-refresh" aria-hidden="true"></i>
            &nbsp;{{
            $t('m.Rejudge')
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'RejudgeAdmin'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane v-if="isContestAdmin" name="AdminMoss" lazy :disabled="contestMenuDisabled">
          <span slot="label">
            <i class="el-icon-connection"></i>
            &nbsp;{{
            $t("m.Admin_Moss")
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'AdminMoss'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane
          v-if="contest.auth != CONTEST_TYPE.EXAMINATION && isContestAdmin"
          name="Session"
          lazy
          :disabled="contestMenuDisabled"
        >
          <span slot="label">
            <i class="el-icon-aim"></i>
            &nbsp;{{
            $t("m.Admin_Session")
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'Session'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane
          name="ScrollBoard"
          lazy
          :disabled="contestMenuDisabled"
          v-if=" contest.auth != CONTEST_TYPE.EXAMINATION && showScrollBoard"
        >
          <span slot="label">
            <i class="el-icon-video-camera-solid" aria-hidden="true"></i>
            &nbsp;{{
            $t('m.ScrollBoard')
            }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'ScrollBoard'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane
          name="AcCsv"
          lazy
          :disabled="contestMenuDisabled"
          v-if="contest.auth != CONTEST_TYPE.EXAMINATION && contestEnded"
        >
          <span slot="label">
            <i class="el-icon-s-grid" aria-hidden="true"></i>
            &nbsp;{{ $t('m.ContestAcCsv') }}
          </span>
          <transition name="el-zoom-in-bottom">
            <router-view v-if="route_name === 'AcCsv'"></router-view>
          </transition>
        </el-tab-pane>

        <el-tab-pane
          name="Discussion"
          lazy
          :disabled="contestMenuDisabled"
          v-if="contest.gid == null && contestEnded"
        >
          <span slot="label">
            <i class="fa fa-comments" aria-hidden="true"></i>
            &nbsp;{{ $t('m.Problem_Discussion') }}
          </span>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>
<script>
import time from "@/common/time";
import moment from "moment";
import api from "@/common/api";
import { mapState, mapGetters, mapActions } from "vuex";
import { addCodeBtn } from "@/common/codeblock";
import {
  CONTEST_STATUS_REVERSE,
  CONTEST_STATUS,
  CONTEST_TYPE_REVERSE,
  CONTEST_TYPE,
  RULE_TYPE,
  buildContestAnnounceKey,
  SIGN_TYPE_REVERSE,
} from "@/common/constants";
import myMessage from "@/common/message";
import storage from "@/common/storage";
import Markdown from "@/components/oj/common/Markdown";
import Timebar from "@/components/oj/common/Timebar.vue";
const Announcement = () => import("@/views/oj/announcement/Announcement.vue");
import BoxFile from "@/components/oj/common/BoxFile";
import utils from "@/common/utils";

export default {
  name: "ContestDetails",
  components: {
    Markdown,
    Timebar,
    BoxFile,
    Announcement,
  },
  data() {
    return {
      percentage: -1, // 点击点占总长度的距离
      sliderValue: 0,
      route_name: "contestDetails",
      timer: null,
      CONTEST_STATUS: {},
      CONTEST_STATUS_REVERSE: {},
      CONTEST_TYPE_REVERSE: {},
      CONTEST_TYPE: {},
      RULE_TYPE: {},
      btnLoading: false,
      contestPassword: "",
      defaultAvatar: require("@/assets/default.jpg"),
      isContest: true,
    };
  },
  created() {
    this.contestID = this.$route.params.contestID;
    this.route_name = this.$route.name.replace("Group", "");

    if (this.route_name.toLowerCase().includes("contest")) {
      this.isContest = true;
    } else {
      this.isContest = false;
    }

    if (this.route_name == "ContestProblemDetails") {
      this.route_name = "ContestProblemList";
    }
    if (this.route_name == "ContestSubmissionDetails") {
      this.route_name = "ContestSubmissionList";
    }
    if (this.route_name == "ContestAdminMossDetails") {
      this.route_name = "ContestAdminMoss";
    }
    if (this.route_name == "ExamProblemDetails") {
      this.route_name = "ExamProblemList";
    }

    this.route_name = this.route_name
      .replace("Contest", "")
      .replace("Exam", "");

    this.CONTEST_TYPE_REVERSE = Object.assign({}, CONTEST_TYPE_REVERSE);
    this.CONTEST_TYPE = Object.assign({}, CONTEST_TYPE);
    this.CONTEST_STATUS = Object.assign({}, CONTEST_STATUS);
    this.CONTEST_STATUS_REVERSE = Object.assign({}, CONTEST_STATUS_REVERSE);
    this.RULE_TYPE = Object.assign({}, RULE_TYPE);
    this.SIGN_TYPE_REVERSE = Object.assign({}, SIGN_TYPE_REVERSE);

    this.$store.dispatch("getContest").then((res) => {
      this.changeDomTitle({ title: res.data.data.title });
      let data = res.data.data;
      let endTime = moment(data.endTime);
      // 如果当前时间还是在比赛结束前的时间，需要计算倒计时，同时开启获取比赛公告的定时器
      if (endTime.isAfter(moment(data.now))) {
        // 实时更新时间
        this.timer = setInterval(() => {
          this.$store.commit("nowAdd1s");
        }, 1000);

        // 每分钟获取一次是否存在未阅读的公告
        this.announceTimer = setInterval(() => {
          let key = buildContestAnnounceKey(this.userInfo.uid, this.contestID);
          let readAnnouncementList = storage.get(key) || [];
          let data = {
            cid: this.contestID,
            readAnnouncementList: readAnnouncementList,
          };

          api.getContestUserNotReadAnnouncement(data).then((res) => {
            let newAnnounceList = res.data.data;
            for (let i = 0; i < newAnnounceList.length; i++) {
              readAnnouncementList.push(newAnnounceList[i].id);
              this.$notify({
                title: newAnnounceList[i].title,
                message:
                  '<p style="text-align:center;"><i class="el-icon-time"> ' +
                  time.utcToLocal(newAnnounceList[i].gmtCreate) +
                  "</i></p>" +
                  '<p style="text-align:center;color:#409eff">' +
                  this.$i18n.t(
                    "m.Please_check_the_contest_announcement_for_details"
                  ) +
                  "</p>",
                type: "warning",
                dangerouslyUseHTMLString: true,
                duration: 0,
              });
            }
            storage.set(key, readAnnouncementList);
          });
        }, 60 * 1000);
      }
      this.$nextTick((_) => {
        addCodeBtn();
      });
    });
  },
  methods: {
    ...mapActions(["changeDomTitle"]),
    getStopFlag(percentage) {
      this.percentage = percentage;
    },
    formatTooltip(val) {
      if (this.percentage !== -1) {
        const selectedTime = this.contest.duration * this.percentage;
        this.selectedTime = parseInt(selectedTime);
        return time.secondFormat(selectedTime); // 格式化时间
      } else {
        if (this.contest.status == -1) {
          // 还未开始
          return "00:00:00";
        } else if (this.contest.status == 0) {
          return time.secondFormat(this.BeginToNowDuration);
        } else {
          return time.secondFormat(this.contest.duration);
        }
      }
    },
    checkPassword() {
      if (this.contestPassword === "") {
        myMessage.warning(this.$i18n.t("m.Enter_the_contest_password"));
        return;
      }
      this.btnLoading = true;
      api.registerContest(this.contestID + "", this.contestPassword).then(
        (res) => {
          myMessage.success(this.$i18n.t("m.Register_contest_successfully"));
          this.$store.commit("contestIntoAccess", { intoAccess: true });
          this.btnLoading = false;
        },
        (res) => {
          this.btnLoading = false;
        }
      );
    },
    tabClick(tab) {
      let name = tab.name;
      if (name === "Discussion") {
        this.goToContestDiscussion();
        return;
      }
      if (!this.isContest) {
        name = "Exam" + name;
      } else {
        name = "Contest" + name;
      }
      if (this.contest.gid) {
        name = "Group" + name;
      }
      if (name !== this.$route.name) {
        this.$router.push({ name: name });
      }
    },
    toGroupContestList(gid) {
      this.$router.push({
        name: "GroupContestList",
        params: {
          groupID: gid,
        },
      });
    },
    toAdminContest(gid, title) {
      if (gid != null) {
        this.$router.push({
          name: "GroupContestList",
          params: {
            groupID: gid,
          },
          query: { adminPage: true, keyword: title },
        });
      } else {
        this.$router.push({
          name: "admin-contest-list",
          query: { keyword: this.contest.title, auth: this.contest.auth },
        });
      }
    },
    openPdf(pdfDescription) {
      window.open(pdfDescription, "_blank");
    },
    goToContestDiscussion() {
      this.contestID = this.$route.params.contestID;
      this.trainingID = this.$route.params.trainingID;
      this.groupID = this.$route.params.groupID;

      const routeName = utils.getRouteRealName(
        this.$route.path,
        this.contestID,
        this.trainingID,
        this.groupID,
        "Discussion"
      );
      this.$router.push({
        name: routeName,
        params: { contestID: this.contestID },
      });
    },
    toSignupList(title) {
      this.$router.push({
        name: "signup-contest-list",
        query: { keyword: title },
      });
    },
    switchFocusMode() {
      this.contestID = this.$route.params.contestID;
      this.trainingID = this.$route.params.trainingID;
      this.groupID = this.$route.params.groupID;

      let isExam = this.$route.name.toLowerCase().includes("exam");

      let routeName = isExam
        ? "ExamFullProblemDetails"
        : this.groupID
        ? "GroupContestFullProblemDetails"
        : "ContestFullProblemDetails";

      this.$router.push({
        name: routeName,
        params: {
          trainingID: this.trainingID,
          contestID: this.contestID,
          problemID: this.problemID,
          groupID: this.groupID,
        },
      });
    },
  },
  computed: {
    ...mapState({
      contest: (state) => state.contest.contest,
      now: (state) => state.contest.now,
    }),
    ...mapGetters([
      "contestMenuDisabled",
      "contestRuleType",
      "contestStatus",
      "countdown",
      "isShowContestSetting",
      "BeginToNowDuration",
      "isContestAdmin",
      "ContestRealTimePermission",
      "passwordFormVisible",
      "userInfo",
      "websiteConfig",
    ]),
    progressValue: {
      get: function () {
        return this.$store.getters.progressValue;
      },
      set: function () {},
    },
    timeStep() {
      // 时间段平分滑条长度
      return 100 / this.contest.duration;
    },
    countdownColor() {
      if (this.contestStatus) {
        return "color:" + CONTEST_STATUS_REVERSE[this.contestStatus].color;
      }
    },
    showAdminHelper() {
      return this.isContestAdmin && this.contestRuleType === RULE_TYPE.ACM;
    },
    showScrollBoard() {
      return this.isContestAdmin && this.contestRuleType === RULE_TYPE.ACM;
    },
    isShowContestPdf() {
      return (
        this.isContestAdmin || this.contestStatus !== CONTEST_STATUS.SCHEDULED
      );
    },
    contestEnded() {
      return this.contestStatus === CONTEST_STATUS.ENDED;
    },
    isContainsAfterContestJudge: {
      get() {
        return this.$store.state.contest.isContainsAfterContestJudge;
      },
      set(value) {
        this.$store.commit("changeContainsAfterContestJudge", { value: value });
      },
    },
    selectedTime: {
      get() {
        return this.$store.state.contest.selectedTime;
      },
      set(value) {
        this.$store.commit("changeSelectedTime", { value: value });
      },
    },
  },
  watch: {
    $route(newVal) {
      this.route_name = newVal.name.replace("Group", "");

      if (this.route_name.toLowerCase().includes("contest")) {
        if (this.route_name == "ContestProblemDetails") {
          this.route_name = "ContestProblemList";
        } else if (this.route_name == "ContestSubmissionDetails") {
          this.route_name = "ContestSubmissionList";
        } else if (this.route_name == "ContestAdminMossDetails") {
          this.route_name = "ContestAdminMoss";
        } else if (this.route_name == "ExamProblemDetails") {
          this.route_name = "ExamProblemList";
        }
        this.isContest = true;
      } else {
        if (this.route_name == "ExamProblemDetails") {
          this.route_name = "ExamProblemList";
        } else if (this.route_name == "ExamSubmissionDetails") {
          this.route_name = "ExamSubmissionList";
        } else if (this.route_name == "ExamAdminMossDetails") {
          this.route_name = "ExamAdminMoss";
        }
        this.isContest = false;
      }

      this.route_name = this.route_name
        .replace("Contest", "")
        .replace("Exam", "");

      this.contestID = newVal.params.contestID;
      this.changeDomTitle({ title: this.contest.title });
    },
  },
  beforeDestroy() {
    clearInterval(this.timer);
    clearInterval(this.announceTimer);
    this.$store.commit("clearContest");
  },
};
</script>
<style scoped>
.panel-title {
  font-size: 1.5rem !important;
  font-weight: 500;
}
@media screen and (min-width: 768px) {
  .contest-time .left {
    text-align: left;
  }
  .contest-time .right {
    text-align: right;
  }
  .password-form-card {
    width: 400px;
    margin: 0 auto;
  }
}
@media screen and (max-width: 768px) {
  .contest-time .left,
  .contest-time .right {
    text-align: center;
  }
}
/* /deep/.el-slider__button {
  width: 20px !important;
  height: 20px !important;
  background-color: #409eff !important;
}
/deep/.el-slider__button-wrapper {
  z-index: 500;
}
/deep/.el-slider__bar {
  height: 10px !important;
  background-color: #09be24 !important;
} */
/deep/ .el-card__header {
  border-bottom: 0px;
  padding-bottom: 0px;
}
/deep/.el-tabs__nav-wrap {
  background: #fff;
  border-radius: 3px;
}
/deep/.el-tabs--top .el-tabs__item.is-top:nth-child(2) {
  padding-left: 20px;
}
.contest-title {
  text-align: center;
}
.contest-time {
  width: 100%;
  font-size: 16px;
}
.el-tag--dark {
  border-color: #fff;
}
.el-tag {
  color: rgb(25, 190, 107);
  background: #fff;
  border: 1px solid #e9eaec;
  font-size: 18px;
}
.sub-menu {
  margin-top: 15px;
}
.password-form-tips {
  text-align: center;
  font-size: 14px;
}
.sign-name {
  font-size: 0.95rem;
  font-weight: 600;
}
.Sign_Required .sign-name {
  color: rgb(230, 162, 60);
}
.Sign_Waiting .sign-name {
  color: rgb(230, 162, 60);
}
.Sign_Refused .sign-name {
  color: rgb(245, 108, 108);
}
.Sign_Successfully .sign-name {
  color: rgb(103, 194, 58);
}
.reply-opt {
  align-items: center;
  cursor: pointer;
}
.reply-text:hover {
  color: #66b1ff;
}
.reply-delete:hover {
  color: #ff503f;
}
</style>
