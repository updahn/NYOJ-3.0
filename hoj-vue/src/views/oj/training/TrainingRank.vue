<template>
  <div style="margin-top:5px">
    <el-card shadow>
      <div slot="header" class="rank-title">
        <span class="panel-title">{{ $t('m.Record_List') }}</span>
      </div>
      <div class="training-rank-search">
        <el-input
          :placeholder="$t('m.Training_Rank_Search_Placeholder')"
          v-model="keyword"
          @keyup.enter.native="getTrainingRankData"
        >
          <el-button
            slot="append"
            icon="el-icon-search"
            class="search-btn"
            @click="getTrainingRankData"
          ></el-button>
        </el-input>
      </div>
      <vxe-table
        round
        border
        auto-resize
        size="medium"
        align="center"
        :data="dataRank"
        :cell-class-name="cellClassName"
        ref="TraningtRank"
        :seq-config="{ startIndex: (this.page - 1) * this.limit }"
        @cell-click="getUserProblemSubmission"
        :loading="loading"
      >
        <vxe-table-column field="rank" type="seq" width="50" fixed="left"></vxe-table-column>
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
              <span style="margin-right: 0.5rem">
                <avatar
                  :username="row.username"
                  :inline="true"
                  :size="37"
                  color="#FFF"
                  :src="row.avatar"
                  :title="row.username"
                ></avatar>
              </span>
              <span class="contest-rank-user-info">
                <a @click="getUserHomeByUsername(row.uid, row.username)">
                  <span class="contest-username" :title="row.username">
                    <span class="contest-rank-flag" v-if="row.uid == userInfo.uid">Own</span>
                    <span class="contest-rank-flag" v-if="row.gender == 'female'">Girl</span>
                    {{ row.username }}
                  </span>
                  <span class="contest-school" v-if="row.school" :title="row.school">
                    {{
                    row.school
                    }}
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
              <span style="margin-right: 0.5rem">
                <avatar
                  :username="row.username"
                  :inline="true"
                  :size="37"
                  color="#FFF"
                  :src="row.avatar"
                  :title="row.username"
                ></avatar>
              </span>
              <span class="contest-rank-user-info">
                <a @click="getUserHomeByUsername(row.uid, row.username)">
                  <span class="contest-username" :title="row.username">
                    <span class="contest-rank-flag" v-if="row.uid == userInfo.uid">Own</span>
                    <span class="contest-rank-flag" v-if="row.gender == 'female'">Girl</span>
                    {{ row.username }}
                  </span>
                  <span class="contest-school" v-if="row.school" :title="row.school">
                    {{
                    row.school
                    }}
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
          v-if="isTrainingAdmin"
        ></vxe-table-column>
        <vxe-table-column field="rating" :title="$t('m.Total_AC')" min-width="90">
          <template v-slot="{ row }">
            <span>
              <a
                @click="getUserACSubmit(row.username)"
                style="color:rgb(87, 163, 243);font-size:16px"
              >{{ row.ac }}</a>
              <br />
              <span class="judge-time">({{ row.totalRunTime }}ms)</span>
            </span>
          </template>
        </vxe-table-column>
        <vxe-table-column field="totalScore" :title="$t('m.Total_Score')" min-width="90">
          <template v-slot="{ row }">
            <span>{{ row.totalScore }}</span>
          </template>
        </vxe-table-column>
        <vxe-table-column
          min-width="70"
          v-for="(problem, index) in trainingProblemList"
          :key="index"
          :field="problem.problemId"
        >
          <template v-slot:header>
            <span>
              <a
                @click="getTrainingProblemById(problem.problemId)"
                class="emphasis"
                style="color:#495060;"
              >{{ problem.problemId }}</a>
            </span>
          </template>
          <template v-slot="{ row }">
            <template v-if="row.submissionInfo[problem.problemId]">
              <el-tooltip effect="dark" placement="top">
                <div slot="content">
                  {{
                  JUDGE_STATUS[row.submissionInfo[problem.problemId].status]
                  .name
                  }}
                </div>
                <span
                  class="judge-status submission-hover"
                  :style="
                    'color:' +
                      JUDGE_STATUS[row.submissionInfo[problem.problemId].status]
                        .rgb
                  "
                >
                  {{
                  JUDGE_STATUS[row.submissionInfo[problem.problemId].status]
                  .short
                  }}
                </span>
              </el-tooltip>
              <br />
              <span class="judge-time">
                ({{
                row.submissionInfo[problem.problemId].runTime
                ? row.submissionInfo[problem.problemId].runTime
                : 0
                }}ms)
              </span>
            </template>
          </template>
        </vxe-table-column>
      </vxe-table>
      <Pagination
        :total="total"
        :page-size.sync="limit"
        :current.sync="page"
        @on-change="getTrainingRankData"
        @on-page-size-change="getTrainingRankData(1)"
        :layout="'prev, pager, next, sizes'"
      ></Pagination>
    </el-card>
  </div>
</template>
<script>
import Avatar from "vue-avatar";
import { mapActions, mapGetters } from "vuex";
import { JUDGE_STATUS } from "@/common/constants";
const Pagination = () => import("@/components/oj/common/Pagination");
import api from "@/common/api";
import { mapState } from "vuex";
import time from "@/common/time";
import utils from "@/common/utils";

export default {
  name: "TrainingRank",
  components: {
    Pagination,
    Avatar,
  },
  data() {
    return {
      total: 0,
      page: 1,
      limit: 30,
      keyword: "",
      trainingID: "",
      dataRank: [],
      JUDGE_STATUS: {},
      groupID: null,
      loading: false,
    };
  },
  mounted() {
    this.JUDGE_STATUS = Object.assign({}, JUDGE_STATUS);
    if (this.$route.params.groupID) {
      this.groupID = this.$route.params.groupID;
    }
    if (!this.trainingProblemList.length) {
      this.getTrainingProblemList();
    }
    this.trainingID = this.$route.params.trainingID;
    this.getTrainingRankData();
  },
  methods: {
    ...mapActions(["getTrainingProblemList"]),

    getTrainingRankData() {
      let data = {
        tid: this.trainingID,
        limit: this.limit,
        currentPage: this.page,
        keyword: this.keyword,
      };
      this.loading = true;
      api.getTrainingRank(data).then(
        (res) => {
          this.total = res.data.data.total;
          this.applyToTable(res.data.data.records);
          this.loading = false;
        },
        (err) => {
          this.loading = false;
        }
      );
    },

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

      let params = {};

      if (this.groupID) {
        params.groupID = this.groupID;
      }

      this.$router.push({
        name: routeName,
        params,
        query: { username: username, status: 0 },
      });
    },
    getUserHomeByUsername(uid, username) {
      const routeName = this.$route.params.groupID
        ? "GroupUserHome"
        : "UserHome";
      this.$router.push({
        name: routeName,
        query: { username: username, uid: uid },
      });
    },
    getTrainingProblemById(pid) {
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

      let params = { problemID: pid, trainingID: this.trainingID };

      if (this.groupID) {
        params.groupID = this.groupID;
      }

      this.$router.push({
        name: routeName,
        params,
      });
    },
    getUserProblemSubmission({ row, column }) {
      if (
        column.property !== "rank" &&
        column.property !== "username" &&
        column.property !== "realname" &&
        column.property !== "rating"
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

        let params = {};

        if (this.groupID) {
          params.groupID = this.groupID;
        }

        this.$router.push({
          name: routeName,
          params,
          query: { username: row.username, problemID: column.property },
        });
      }
    },
    cellClassName({ row, rowIndex, column, columnIndex }) {
      if (row.username == this.userInfo.username) {
        if (
          column.property == "rank" ||
          column.property == "username" ||
          column.property == "realname"
        ) {
          return "own-submit-row";
        }
      }
      if (column.property === "username" && row.userCellClassName) {
        return row.userCellClassName;
      }
    },
    applyToTable(dataRank) {
      dataRank.forEach((rank, i) => {
        if (dataRank[i].gender == "female") {
          dataRank[i].userCellClassName = "bg-female";
        }
      });
      this.dataRank = dataRank;
    },
    parseTotalTime(totalTime) {
      return time.secondFormat(totalTime);
    },
  },
  computed: {
    ...mapState({
      trainingProblemList: (state) => state.training.trainingProblemList,
    }),
    ...mapGetters(["isTrainingAdmin", "userInfo"]),
    training() {
      return this.$store.state.training.training;
    },
    isMobileView() {
      return window.screen.width < 768;
    },
  },
};
</script>
<style scoped>
.rank-title {
  text-align: center;
}
/deep/.el-card__body {
  padding: 20px !important;
}
.training-rank-search {
  text-align: center;
  margin: 10px auto;
  width: 90%;
}
@media screen and (min-width: 768px) {
  .training-rank-search {
    width: 50%;
  }
}
@media screen and (min-width: 1050px) {
  .training-rank-search {
    width: 30%;
  }
}

.vxe-cell p,
.vxe-cell span {
  margin: 0;
  padding: 0;
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

/deep/.vxe-table .vxe-header--column:not(.col--ellipsis) {
  padding: 4px 0 !important;
}
/deep/.vxe-table .vxe-body--column {
  padding: 4px 0 !important;
  line-height: 20px !important;
}
/deep/.vxe-table .vxe-body--column:not(.col--ellipsis) {
  line-height: 20px !important;
  padding: 0 !important;
}
/deep/.vxe-body--column {
  min-width: 0;
  height: 51px !important;
  box-sizing: border-box;
  text-align: left;
  text-overflow: ellipsis;
  vertical-align: middle;
}
/deep/.vxe-table .vxe-cell {
  padding-left: 5px !important;
  padding-right: 5px !important;
}
.judge-status {
  font-size: 16px;
  font-weight: bold;
}
.judge-time {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
