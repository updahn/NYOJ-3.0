<template>
  <el-card shadow>
    <div slot="header">
      <span class="panel-title">{{ $t('m.Contest_Rank') }}</span>
    </div>
    <div v-show="showChart" class="echarts">
      <ECharts :options="options" ref="chart" :autoresize="true"></ECharts>
    </div>
    <el-row>
      <el-col :xs="24" :md="8">
        <div class="contest-rank-search contest-rank-filter">
          <el-input
            :placeholder="$t('m.Contest_Rank_Search_Placeholder')"
            v-model="keyword"
            @keyup.enter.native="getContestRankData(page)"
          >
            <el-button
              slot="append"
              icon="el-icon-search"
              class="search-btn"
              @click="getContestRankData(page)"
            ></el-button>
          </el-input>
        </div>
      </el-col>
      <el-col :xs="24" :md="16">
        <div class="contest-rank-config">
          <el-popover trigger="hover" placement="left-start">
            <el-button round size="small" slot="reference">{{$t('m.Contest_Rank_Setting')}}</el-button>
            <div id="switches">
              <p>
                <span>{{ $t('m.Chart') }}</span>
                <el-switch v-model="showChart"></el-switch>
              </p>
              <p>
                <span>{{ $t('m.Table') }}</span>
                <el-switch v-model="showTable"></el-switch>
              </p>
              <p>
                <span>{{ $t('m.Star_User') }}</span>
                <el-switch v-model="showStarUser" @change="getContestRankData(page)"></el-switch>
              </p>
              <p>
                <span>{{ $t('m.Auto_Refresh') }}(10s)</span>
                <el-switch
                  :disabled="refreshDisabled"
                  v-model="autoRefresh"
                  @change="handleAutoRefresh"
                ></el-switch>
              </p>
              <template v-if="isContestAdmin && selectedTime === null">
                <p>
                  <span>{{ $t('m.Force_Update') }}</span>
                  <el-switch v-model="forceUpdate" @change="getContestRankData(page)"></el-switch>
                </p>
              </template>
              <template v-if="isContestAdmin">
                <el-button type="primary" size="small" @click="downloadRankCSV">
                  {{
                  $t('m.Download_as_CSV')
                  }}
                </el-button>
              </template>
            </div>
          </el-popover>
        </div>
      </el-col>
    </el-row>

    <div v-show="showTable">
      <vxe-grid
        round
        border
        auto-resize
        size="medium"
        align="center"
        ref="ACMContestRank"
        :data="dataRank"
        :key="contestProblems"
        :cell-class-name="cellClassName"
        @cell-click="getUserProblemSubmission"
      >
        <vxe-table-column field="rank" width="50" fixed="left" :title="$t('m.Contest_Rank_Seq')">
          <template v-slot="{ row }">
            <template v-if="row.rank == -1">
              <span>*</span>
            </template>
            <template v-else>
              <template v-if="row.isWinAward">
                <RankBox
                  :num="row.rank"
                  :background="row.awardBackground"
                  :color="row.awardColor"
                  :name="row.awardName"
                ></RankBox>
              </template>
              <template v-else>
                <RankBox :num="row.rank"></RankBox>
              </template>
            </template>
          </template>
        </vxe-table-column>
        <vxe-table-column field="school" min-width="120" fixed="left" :title="$t('m.School')">
          <template v-slot="{ row }">
            <span
              class="contest-school"
              v-if="row.school"
              :title="row.school"
              style="margin-left: 5px;"
            >
              <div class="school-info" style="display: flex; align-items: center;">
                <div v-if="row.schoolRank" class="num-box" style="margin-right: 5px;">
                  <span>{{ row.schoolRank }}</span>
                </div>
                <span>{{ row.school }}</span>
              </div>
            </span>
          </template>
        </vxe-table-column>
        <vxe-table-column
          field="username"
          fixed="left"
          v-if="!isMobileView"
          min-width="300"
          :title="$t('m.User')"
          header-align="center"
          align="left"
        >
          <template v-slot="{ row }">
            <div class="contest-rank-user-box">
              <span>
                <avatar
                  :username="row.rankShowName"
                  :inline="true"
                  :size="37"
                  color="#FFF"
                  :src="row.avatar"
                  :title="row.rankShowName"
                ></avatar>
              </span>
              <el-tooltip placement="top">
                <div slot="content">
                  {{
                  row.isConcerned ? $t('m.Unfollow') : $t('m.Top_And_Follow')
                  }}
                </div>
                <span
                  class="contest-rank-concerned"
                  @click="updateConcernedList(row.uid, !row.isConcerned)"
                >
                  <i class="fa fa-star" v-if="row.isConcerned" style="color: red;"></i>
                  <i class="el-icon-star-off" v-else></i>
                </span>
              </el-tooltip>
              <span class="contest-rank-user-info">
                <a @click="getUserHomeByUsername(row.uid, row.username, row.synchronous)">
                  <span class="contest-username" :title="row.rankShowName">
                    <span class="contest-rank-flag" v-if="row.uid == userInfo.uid">Own</span>
                    <span class="contest-rank-flag" v-if="row.rank == -1">Star</span>
                    <span class="contest-rank-flag" v-if="row.gender == 'female'">Girl</span>
                    {{ row.rankShowName }}
                  </span>
                </a>
              </span>
            </div>
          </template>
        </vxe-table-column>
        <vxe-table-column
          field="username"
          v-else
          min-width="300"
          :title="$t('m.User')"
          header-align="center"
          align="left"
        >
          <template v-slot="{ row }">
            <div class="contest-rank-user-box">
              <span>
                <avatar
                  :username="row.rankShowName"
                  :inline="true"
                  :size="37"
                  color="#FFF"
                  :src="row.avatar"
                  :title="row.rankShowName"
                ></avatar>
              </span>
              <el-tooltip placement="top">
                <div slot="content">
                  {{
                  row.isConcerned ? $t('m.Unfollow') : $t('m.Top_And_Follow')
                  }}
                </div>
                <span
                  class="contest-rank-concerned"
                  @click="updateConcernedList(row.uid, !row.isConcerned)"
                >
                  <i class="fa fa-star" v-if="row.isConcerned" style="color: red;"></i>
                  <i class="el-icon-star-off" v-else></i>
                </span>
              </el-tooltip>
              <span class="contest-rank-user-info">
                <a @click="getUserHomeByUsername(row.uid, row.username, row.synchronous)">
                  <span class="contest-username" :title="row.rankShowName">
                    <span class="contest-rank-flag" v-if="row.uid == userInfo.uid">Own</span>
                    <span class="contest-rank-flag" v-if="row.rank == -1">Star</span>
                    <span class="contest-rank-flag" v-if="row.gender == 'female'">Girl</span>
                    {{ row.rankShowName }}
                  </span>
                </a>
              </span>
            </div>
          </template>
        </vxe-table-column>
        <vxe-table-column
          field="realname"
          min-width="96"
          :title="$t('m.RealName')"
          show-overflow
          v-if="isContestAdmin"
        ></vxe-table-column>
        <vxe-table-column field="rating" :title="$t('m.AC')" min-width="60">
          <template v-slot="{ row }">
            <span>
              <a
                @click="getUserACSubmit(row.username)"
                style="color:rgb(87, 163, 243);font-weight: 600;font-size: 14px;"
              >{{ row.ac }}</a>
            </span>
          </template>
        </vxe-table-column>
        <vxe-table-column field="totalTime" :title="$t('m.TotalTime')" min-width="60">
          <template v-slot="{ row }">
            <el-tooltip effect="dark" placement="top">
              <div slot="content">{{ parseTimeToSpecific(row.totalTime) }}</div>
              <span>{{ parseInt(row.totalTime / 60) }}</span>
            </el-tooltip>
          </template>
        </vxe-table-column>
        <vxe-table-column
          min-width="74"
          v-for="problem in contestProblems"
          :key="problem.displayId"
          :field="problem.displayId"
        >
          <template v-slot:header>
            <span class="contest-rank-balloon" v-if="problem.color">
              <svg
                t="1633685184463"
                class="icon"
                viewBox="0 0 1088 1024"
                version="1.1"
                xmlns="http://www.w3.org/2000/svg"
                p-id="5840"
                width="25"
                height="25"
              >
                <path
                  d="M575.872 849.408c-104.576 0-117.632-26.56-119.232-31.808-6.528-22.528 32.896-70.592 63.744-96.768l-1.728-2.624c137.6-42.688 243.648-290.112 243.648-433.472A284.544 284.544 0 0 0 478.016 0a284.544 284.544 0 0 0-284.288 284.736c0 150.4 116.352 415.104 263.744 438.336-25.152 29.568-50.368 70.784-39.104 108.928 12.608 43.136 62.72 63.232 157.632 63.232 7.872 0 11.52 9.408 4.352 19.52-21.248 29.248-77.888 63.424-167.68 63.424V1024c138.944 0 215.936-74.816 215.936-126.528a46.72 46.72 0 0 0-16.32-36.608 56.32 56.32 0 0 0-36.416-11.456zM297.152 297.472c0 44.032-38.144 25.344-38.144-38.656 0-108.032 85.248-195.712 190.592-195.712 62.592 0 81.216 39.232 38.08 39.232-105.152 0.064-190.528 87.04-190.528 195.136z"
                  :fill="problem.color"
                  p-id="5841"
                />
              </svg>
            </span>
            <span>
              <a
                @click="getContestProblemById(problem.displayId)"
                class="emphasis"
                style="color:#495060;"
              >{{ problem.displayId }}</a>
            </span>
            <br />
            <span>
              <el-tooltip effect="dark" placement="top">
                <div slot="content">
                  {{ problem.displayId + '. ' + problem.displayTitle }}
                  <br />
                  {{ 'Accepted: ' + problem.ac }}
                  <br />
                  {{ 'Rejected: ' + (problem.total - problem.ac) }}
                </div>
                <span>({{ problem.ac }}/{{ problem.total }})</span>
              </el-tooltip>
            </span>
          </template>
          <template v-slot="{ row }">
            <span v-if="row.submissionInfo[problem.displayId]" class="submission-hover">
              <el-tooltip effect="dark" placement="top">
                <div slot="content">{{ row.submissionInfo[problem.displayId].specificTime }}</div>
                <span v-if="row.submissionInfo[problem.displayId].isAC" class="submission-time">
                  {{ row.submissionInfo[problem.displayId].isAfterContest?'*': row.submissionInfo[problem.displayId].ACTime}}
                  <br />
                </span>
              </el-tooltip>

              <span
                class="submission-error"
                v-if="
                  row.submissionInfo[problem.displayId].tryNum == null &&
                    row.submissionInfo[problem.displayId].errorNum != 0
                "
              >
                {{
                row.submissionInfo[problem.displayId].errorNum > 1
                ? row.submissionInfo[problem.displayId].errorNum + ' tries'
                : row.submissionInfo[problem.displayId].errorNum + ' try'
                }}
              </span>
              <span v-if="row.submissionInfo[problem.displayId].tryNum != null">
                <template v-if="row.submissionInfo[problem.displayId].errorNum > 0">
                  {{
                  row.submissionInfo[problem.displayId].errorNum
                  }}+
                </template>
                {{ row.submissionInfo[problem.displayId].tryNum
                }}{{
                row.submissionInfo[problem.displayId].errorNum +
                row.submissionInfo[problem.displayId].tryNum >
                1
                ? ' tries'
                : ' try'
                }}
              </span>
            </span>
          </template>
        </vxe-table-column>
      </vxe-grid>
    </div>
    <Pagination
      :total="total"
      :page-size.sync="limit"
      :page-sizes="[10, 30, 50, 100, 300, 500]"
      :current.sync="page"
      @on-change="getContestRankData"
      @on-page-size-change="getContestRankData(1)"
      :layout="'prev, pager, next, sizes'"
    ></Pagination>
  </el-card>
</template>
<script>
import Avatar from "vue-avatar";
import moment from "moment";
import { mapState, mapGetters } from "vuex";
const Pagination = () => import("@/components/oj/common/Pagination");
const RankBox = () => import("@/components/oj/common/RankBox");
import time from "@/common/time";
import utils from "@/common/utils";
import ContestRankMixin from "./contestRankMixin";

export default {
  name: "ACMContestRank",
  mixins: [ContestRankMixin],
  components: {
    Pagination,
    RankBox,
    Avatar,
  },
  data() {
    return {
      total: 0,
      page: 1,
      limit: 50,
      autoRefresh: false,
      showChart: false,
      showStarUser: false,
      contestID: "",
      dataRank: [],
      keyword: null,
      options: {
        title: {
          text: this.$i18n.t("m.Top_10_Teams"),
          left: "center",
          top: 0,
        },
        dataZoom: [
          {
            type: "inside",
            filterMode: "none",
            xAxisIndex: [0],
            start: 0,
            end: 100,
          },
        ],
        toolbox: {
          show: true,
          feature: {
            saveAsImage: { show: true, title: this.$i18n.t("m.save_as_image") },
          },
          right: "0",
        },
        tooltip: {
          trigger: "axis",
          axisPointer: {
            type: "cross",
            axis: "x",
          },
        },
        legend: {
          orient: "horizontal",
          x: "center",
          top: "8%",
          right: 0,
          data: [],
          formatter: (value) => {
            return utils.breakLongWords(value, 16);
          },
          textStyle: {
            fontSize: 12,
          },
        },
        grid: {
          x: 80,
          x2: 100,
          left: "5%", //设置canvas图距左的距离
          top: "25%",
          right: "5%",
          bottom: "10%",
        },
        xAxis: [
          {
            type: "time",
            splitLine: false,
            axisPointer: {
              show: true,
              snap: true,
            },
            axisLabel: {
              textStyle: {
                color: this.getAxisLabelColor(),
              },
            },
          },
        ],
        yAxis: [
          {
            type: "category",
            boundaryGap: false,
            data: [0],
            axisLabel: {
              textStyle: {
                color: this.getAxisLabelColor(),
              },
            },
          },
        ],
        series: [],
      },
    };
  },
  created() {
    this.initConcernedList();
  },
  mounted() {
    this.contestID = this.$route.params.contestID;
    this.getContestRankData(1);
    this.addChartCategory(this.contestProblems);
    if (!this.refreshDisabled) {
      this.autoRefresh = true;
      this.handleAutoRefresh(true);
    }
  },
  methods: {
    getUserACSubmit(username) {
      this.contestID = this.$route.params.contestID;
      this.trainingID = this.$route.params.trainingID;
      this.groupID = this.$route.params.groupID;

      const routeName = utils.getRouteRealName(
        this.$route.path,
        this.contestID,
        this.trainingID,
        this.groupID,
        "SubmissionList"
      );

      this.$router.push({
        name: routeName,
        query: { username: username, status: 0 },
      });
    },
    getUserHomeByUsername(uid, username, synchronous) {
      if (!synchronous) {
        const routeName = this.$route.params.groupID
          ? "GroupUserHome"
          : "UserHome";
        this.$router.push({
          name: routeName,
          query: { username: username, uid: uid },
        });
      }
    },
    getContestProblemById(pid) {
      this.contestID = this.$route.params.contestID;
      this.trainingID = this.$route.params.trainingID;
      this.groupID = this.$route.params.groupID;

      const routeName = utils.getRouteRealName(
        this.$route.path,
        this.contestID,
        this.trainingID,
        this.groupID,
        "ProblemDetails"
      );

      this.$router.push({
        name: routeName,
        params: {
          contestID: this.contestID,
          problemID: pid,
        },
      });
    },
    getUserProblemSubmission({ row, column }) {
      if (
        column.property != "rank" &&
        column.property != "rating" &&
        column.property != "totalTime" &&
        column.property != "username" &&
        column.property != "realname" &&
        column.property != "school"
      ) {
        this.contestID = this.$route.params.contestID;
        this.trainingID = this.$route.params.trainingID;
        this.groupID = this.$route.params.groupID;

        const routeName = utils.getRouteRealName(
          this.$route.path,
          this.contestID,
          this.trainingID,
          this.groupID,
          "SubmissionList"
        );

        this.$router.push({
          name: routeName,
          query: {
            username: row.username,
            problemID: column.property,
            completeProblemID: true,
          },
        });
      }
    },
    cellClassName({ row, rowIndex, column, columnIndex }) {
      if (row.username == this.userInfo.username) {
        if (
          column.property == "rank" ||
          column.property == "rating" ||
          column.property == "totalTime" ||
          column.property == "username" ||
          column.property == "realname" ||
          column.property == "school"
        ) {
          return "own-submit-row";
        }
      }

      if (column.property == "username" && row.userCellClassName) {
        return row.userCellClassName;
      }

      if (
        column.property != "rank" &&
        column.property != "rating" &&
        column.property != "totalTime" &&
        column.property != "username" &&
        column.property != "realname" &&
        column.property != "school"
      ) {
        if (this.isContestAdmin) {
          return row.cellClassName[
            [this.contestProblems[columnIndex - 6].displayId]
          ];
        } else {
          return row.cellClassName[
            [this.contestProblems[columnIndex - 5].displayId]
          ];
        }
      } else {
        if (row.isConcerned && column.property !== "username") {
          return "bg-concerned";
        }
      }
    },
    applyToTable(dataRank) {
      dataRank.forEach((rank, i) => {
        let info = rank.submissionInfo;
        let cellClass = {};
        if (this.concernedList.indexOf(rank.uid) != -1) {
          rank.isConcerned = true;
        }
        Object.keys(info).forEach((problemID) => {
          rank[problemID] = info[problemID];
          if (rank[problemID].ACTime != null) {
            rank[problemID].errorNum += 1;
            rank[problemID].specificTime = this.parseTimeToSpecific(
              rank[problemID].ACTime
            );
            rank[problemID].ACTime = parseInt(rank[problemID].ACTime / 60);
          }
          let status = info[problemID];
          if (status.isAfterContest && status.isAC) {
            cellClass[problemID] = "after-ac";
          } else if (status.isFirstAC) {
            cellClass[problemID] = "first-ac";
          } else if (status.isAC) {
            cellClass[problemID] = "ac";
          } else if (status.tryNum != null && status.tryNum > 0) {
            cellClass[problemID] = "try";
          } else if (status.errorNum != 0) {
            cellClass[problemID] = "wa";
          }
        });
        rank.cellClassName = cellClass;
        if (rank.rank == -1) {
          rank.userCellClassName = "bg-star";
        }
        if (rank.gender == "female") {
          rank.userCellClassName = "bg-female";
        }
        rank.rankShowName = this.getRankShowName(
          rank[this.contest.rankShowName],
          rank.username
        );
      });
      this.dataRank = dataRank;
    },
    addChartCategory(contestProblems) {
      let category = [];
      for (let i = 0; i <= contestProblems.length; ++i) {
        category.push(i);
      }
      this.options.yAxis[0].data = category;
    },
    applyToChart(rankData) {
      let [users, seriesData] = [[], []];
      let len = rankData.length;
      let topIndex = this.concernedList.length || 0;
      if (rankData.length > 0) {
        if (rankData[0].uid == this.userInfo.uid) {
          topIndex++;
        }
      }
      for (let i = topIndex; i < len && i < topIndex + 10; i++) {
        let rank = rankData[i];
        let rankShowName = this.getRankShowName(
          rank[this.contest.rankShowName],
          rank.username
        );
        users.push(rankShowName);
        let info = rank.submissionInfo;
        // 提取出已AC题目的时间
        let timeData = [];
        Object.keys(info).forEach((problemID) => {
          if (info[problemID].isAC) {
            timeData.push(info[problemID].ACTime);
          }
        });
        timeData.sort((a, b) => {
          return a - b;
        });

        let data = [];
        data.push([this.contest.startTime, 0]);

        for (let [index, value] of timeData.entries()) {
          let realTime = moment(this.contest.startTime)
            .add(value, "seconds")
            .format();
          data.push([realTime, index + 1]);
        }
        seriesData.push({
          name: rankShowName,
          type: "line",
          data,
        });
      }
      this.options.legend.data = users;
      this.options.series = seriesData;
    },
    parseTimeToSpecific(totalTime) {
      return time.secondFormat(totalTime);
    },
    downloadRankCSV() {
      utils.downloadFile(
        `/api/file/download-contest-rank?cid=${
          this.$route.params.contestID
        }&forceRefresh=${this.forceUpdate ? true : false}&containEnd=${
          this.isContainsAfterContestJudge
        }`
      );
    },
    getAxisLabelColor() {
      return this.webTheme === "Dark" ? "white" : "black";
    },
  },
  watch: {
    contestProblems(newVal, OldVal) {
      if (newVal.length != 0) {
        this.addChartCategory(this.contestProblems);
      }
    },
    isContainsAfterContestJudge(newVal, OldVal) {
      this.$store.dispatch("getContestProblems");
      this.getContestRankData(this.page);
    },
    selectedTime(newVal, OldVal) {
      this.$store.dispatch("getContestProblems");
      this.getContestRankData(this.page);
      if (this.selectedTime !== null) {
        // 禁止自动更新
        this.autoRefresh = false;
        this.handleAutoRefresh(false);
      }
    },
    webTheme(newVal, OldVal) {
      if (this.options.xAxis && this.options.yAxis) {
        this.options.xAxis[0].axisLabel.textStyle.color =
          this.getAxisLabelColor();
        this.options.yAxis[0].axisLabel.textStyle.color =
          this.getAxisLabelColor();
      }
    },
  },
  computed: {
    ...mapState({ contestProblems: (state) => state.contest.contestProblems }),
    ...mapGetters(["webTheme", "isContainsAfterContestJudge", "selectedTime"]),
    contest() {
      return this.$store.state.contest.contest;
    },
    isMobileView() {
      return window.screen.width < 768;
    },
  },
};
</script>

<style scoped>
.echarts {
  margin: 20px auto;
  height: 400px;
  width: 100%;
}
/deep/.el-card__body {
  padding: 20px !important;
  padding-top: 0px !important;
}

.screen-full {
  margin-right: 8px;
}

#switches p {
  margin-top: 5px;
}
#switches p:first-child {
  margin-top: 0;
}
#switches p span {
  margin-left: 8px;
  margin-right: 4px;
}
.vxe-cell p,
.vxe-cell span {
  margin: 0;
  padding: 0;
}

/deep/.vxe-table .vxe-header--column:not(.col--ellipsis) {
  padding: 4px 0 !important;
}

/deep/.vxe-table .vxe-body--column {
  line-height: 20px !important;
  padding: 0 !important;
}
@media screen and (max-width: 768px) {
  /deep/.el-card__body {
    padding: 0 !important;
  }
}
a.emphasis {
  color: #495060 !important;
}
a.emphasis:hover {
  color: #2d8cf0 !important;
}
/deep/.vxe-body--column {
  min-width: 0;
  height: 48px;
  box-sizing: border-box;
  text-align: left;
  text-overflow: ellipsis;
  vertical-align: middle;
}
/deep/.vxe-table .vxe-cell {
  padding-left: 5px !important;
  padding-right: 5px !important;
}
.submission-time {
  font-size: 15.6px;
  font-family: Roboto, sans-serif;
}
.submission-error {
  font-weight: 400;
}
.num-box span {
  background: #ededed;
  color: #666;
  display: block;
  font: 700 15px/15px Arial;
  height: 15px;
  width: 15px;
  margin-left: auto;
  overflow: hidden;
  text-align: center;
}
</style>
