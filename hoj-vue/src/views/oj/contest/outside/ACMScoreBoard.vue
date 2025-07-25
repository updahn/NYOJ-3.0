<template>
  <div class="scoreboard-body">
    <el-card shadow v-loading="loading.info">
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
          <el-button
            size="small"
            v-if="contest != null"
            :type="contest.type == 0 ? 'primary' : 'warning'"
          >
            <i class="fa fa-trophy"></i>
            {{ contest.type | parseContestType }}
          </el-button>
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
      <div class="slider">
        <el-slider v-model="progressValue" :format-tooltip="formatTooltip" :step="timeStep"></el-slider>
      </div>
      <el-row>
        <el-col :span="24" style="text-align:center">
          <el-tag effect="dark" size="medium" :style="countdownColor">
            <i class="fa fa-circle" aria-hidden="true"></i>
            {{ countdown }}
          </el-tag>
        </el-col>
      </el-row>
    </el-card>
    <el-card shadow style="margin-top:15px;" v-loading="loading.rank">
      <el-row>
        <el-col :xs="24" :md="8">
          <div class="contest-rank-search contest-rank-filter">
            <el-input
              :placeholder="$t('m.Contest_Rank_Search_Placeholder')"
              v-model="keyword"
              @keyup.enter.native="getContestOutsideScoreboard"
            >
              <el-button
                slot="append"
                icon="el-icon-search"
                class="search-btn"
                @click="getContestOutsideScoreboard"
              ></el-button>
            </el-input>
          </div>
        </el-col>
        <el-col :xs="24" :md="16">
          <div class="contest-rank-config contest-scoreBoard-config">
            <span>
              <span>{{ $t('m.Star_User') }}</span>
              <el-switch v-model="showStarUser" @change="getContestOutsideScoreboard"></el-switch>
            </span>
            <span v-if="isShowContestSetting">
              <span>{{ $t('m.Contains_After_Contest') }}</span>
              <el-switch
                v-model="isContainsAfterContestJudge"
                @change="getContestOutsideScoreboard"
              ></el-switch>
            </span>
            <span v-if="isContestAdmin">
              <span>{{ $t('m.Force_Update') }}</span>
              <el-switch v-model="forceUpdate" @change="getContestOutsideScoreboard"></el-switch>
            </span>
            <span>
              <span>{{ $t('m.Auto_Refresh') }}(30s)</span>
              <el-switch :disabled="contestEnded" @change="handleAutoRefresh" v-model="autoRefresh"></el-switch>
            </span>
          </div>
        </el-col>
      </el-row>
      <vxe-table
        round
        border
        auto-resize
        size="medium"
        align="center"
        :data="dataRank"
        :cell-class-name="cellClassName"
        max-height="2500000px"
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
                <a @click="getUserHomeByUsername(row.uid, row.username)">
                  <span class="contest-username" :title="row.rankShowName">
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
                <a @click="getUserHomeByUsername(row.uid, row.username)">
                  <span class="contest-username" :title="row.rankShowName">
                    <span class="contest-rank-flag" v-if="row.rank == -1">Star</span>
                    <span class="contest-rank-flag" v-if="row.gender == 'female'">Girl</span>
                    {{ row.rankShowName }}
                  </span>
                </a>
              </span>
            </div>
          </template>
        </vxe-table-column>
        <vxe-table-column field="rating" :title="$t('m.AC')" min-width="60">
          <template v-slot="{ row }">
            <span style="color:rgb(87, 163, 243);font-weight: 600;font-size: 14px;">{{ row.ac }}</span>
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
              <a class="emphasis" style="color:#495060;">{{ problem.displayId }}</a>
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
            <span v-if="row.submissionInfo[problem.displayId]">
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
      </vxe-table>
      <Pagination
        :total="total"
        :page-size.sync="limit"
        :page-sizes="[10, 30, 50, 100, 300]"
        :current.sync="page"
        @on-change="getContestOutsideScoreboard"
        @on-page-size-change="getContestOutsideScoreboard"
        :layout="'prev, pager, next, sizes'"
      ></Pagination>
    </el-card>
  </div>
</template>
<script>
import Avatar from "vue-avatar";
import time from "@/common/time";
import ScoreBoardMixin from "./scoreBoardMixin";
const RankBox = () => import("@/components/oj/common/RankBox");
const Pagination = () => import("@/components/oj/common/Pagination");
export default {
  name: "ACMScoreBoard",
  mixins: [ScoreBoardMixin],
  components: {
    Avatar,
    RankBox,
    Pagination,
  },
  data() {
    return {
      autoRefresh: false,
      removeStar: false,
      loading: {
        info: false,
        rank: false,
      },
      contestID: "",
      dataRank: [],
      keyword: null,
      page: 1,
      limit: 50,
      total: 0,
      timer: null,
      CONTEST_STATUS: {},
      CONTEST_STATUS_REVERSE: {},
      CONTEST_TYPE_REVERSE: {},
      RULE_TYPE: {},
    };
  },
  created() {
    this.init();
  },
  methods: {
    getUserHomeByUsername(uid, username) {
      const routeName = this.$route.params.groupID
        ? "GroupUserHome"
        : "UserHome";
      this.$router.push({
        name: routeName,
        query: { username: username, uid: uid },
      });
    },
    cellClassName({ row, rowIndex, column, columnIndex }) {
      if (column.property === "username" && row.userCellClassName) {
        return row.userCellClassName;
      }
      if (
        column.property !== "rank" &&
        column.property !== "rating" &&
        column.property !== "totalTime" &&
        column.property !== "username" &&
        column.property != "school"
      ) {
        return row.cellClassName[
          [this.contestProblems[columnIndex - 5].displayId]
        ];
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
    parseTimeToSpecific(totalTime) {
      return time.secondFormat(totalTime);
    },
  },
  computed: {
    isMobileView() {
      return window.screen.width < 768;
    },
  },
};
</script>
<style scoped>
.contest-title {
  text-align: center;
}
.panel-title {
  font-size: 1.5rem !important;
  font-weight: 500;
}
.contest-time {
  width: 100%;
  font-size: 16px;
}
@media screen and (min-width: 768px) {
  .contest-time .left {
    text-align: left;
  }
  .contest-time .right {
    text-align: right;
  }
}
@media screen and (max-width: 768px) {
  .contest-time .left,
  .contest-time .right {
    text-align: center;
  }
}

/deep/.el-slider__button {
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
/deep/.el-card__body {
  padding: 15px !important;
  padding-top: 20px !important;
}

.vxe-cell p,
.vxe-cell span {
  margin: 0;
  padding: 0;
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

