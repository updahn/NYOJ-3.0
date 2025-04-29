<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ $t('m.Signup_ContestList') }}</span>
        <div class="filter-row">
          <span>
            <vxe-input
              v-model="keyword"
              :placeholder="$t('m.Enter_keyword')"
              type="search"
              size="medium"
              @search-click="filterByKeyword"
              @keyup.enter.native="filterByKeyword"
            ></vxe-input>
          </span>
          <span>
            <el-select
              v-model="contestStatus"
              @change="filterByStatus"
              size="small"
              style="width: 180px"
            >
              <el-option :label="$t('m.All_Contest')" :value="'All'"></el-option>
              <el-option :label="$t('m.Scheduled')" :value="-1"></el-option>
              <el-option :label="$t('m.Running')" :value="0"></el-option>
              <el-option :label="$t('m.Ended')" :value="1"></el-option>
            </el-select>
          </span>
        </div>
      </div>
      <vxe-table
        :loading="loading"
        ref="xTable"
        :data="contestList"
        auto-resize
        stripe
        align="center"
      >
        <vxe-table-column field="id" width="80" title="ID"></vxe-table-column>
        <vxe-table-column field="title" min-width="150" :title="$t('m.Title')" show-overflow></vxe-table-column>
        <vxe-table-column min-width="210" :title="$t('m.Contest_Times')">
          <template v-slot="{ row }">
            <p>Start Time: {{ row.startTime | localtime }}</p>
            <p>End Time: {{ row.endTime | localtime }}</p>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="210" :title="$t('m.Signup_Times')">
          <template v-slot="{ row }">
            <p>Start Time: {{ row.signStartTime | localtime }}</p>
            <p>End Time: {{ row.signEndTime | localtime }}</p>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="210" :title="$t('m.Modify_endTime')">
          <template v-slot="{ row }">
            <p>End Time: {{ row.modifyEndTime | localtime }}</p>
          </template>
        </vxe-table-column>
        <vxe-table-column :title="$t('m.Status')" width="100">
          <template v-slot="{ row }">
            <el-tag
              effect="dark"
              :color="CONTEST_STATUS_REVERSE[row.status].color"
              size="medium"
            >{{ CONTEST_STATUS_REVERSE[row.status].name }}</el-tag>
          </template>
        </vxe-table-column>

        <vxe-table-column min-width="200" :title="$t('m.Option')">
          <template v-slot="{ row }">
            <div style="margin-bottom:10px">
              <el-tooltip effect="dark" :content="$t('m.Send_Sign')" placement="top">
                <el-button
                  icon="el-icon-s-promotion"
                  size="mini"
                  @click.native="handleEditInfo(row)"
                  type="primary"
                ></el-button>
              </el-tooltip>
              <el-tooltip effect="dark" :content="$t('m.View_Signup_List')" placement="top">
                <el-button
                  icon="el-icon-tickets"
                  size="mini"
                  @click.native="goTeamSign(row.id)"
                  type="success"
                ></el-button>
              </el-tooltip>
            </div>
          </template>
        </vxe-table-column>
      </vxe-table>
      <div class="panel-options">
        <el-pagination
          class="page"
          layout="prev, pager, next, sizes"
          @current-change="currentChange"
          :page-size.sync="limit"
          :current.sync="currentPage"
          :total="total"
          @size-change="onPageSizeChange"
          :page-sizes="[10, 30, 50, 100]"
        ></el-pagination>
      </div>

      <el-dialog
        :title="$t('m.Send_Sign')"
        width="500px"
        :visible.sync="handleSignupVisible"
        :close-on-click-modal="false"
        :loading="loading"
      >
        <AddTeam
          :filteredTeams.sync="filteredTeams"
          :teamSign.sync="teamSign"
          :visible.sync="handleSignupVisible"
        ></AddTeam>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import api from "@/common/api";
import { CONTEST_STATUS_REVERSE, QUOTA_TYPE_REVERSE } from "@/common/constants";
const AddTeam = () => import("@/components/signup/AddTeam");

export default {
  name: "SignupContestList",
  components: {
    AddTeam,
  },
  data() {
    return {
      limit: 10,
      currentPage: 1,
      total: 0,
      contestList: [],
      filteredTeams: [],
      keyword: "",
      contestStatus: "All", // 比赛状态
      loading: false,
      handleSendSignupLoading: false,
      handleSignupVisible: false,
      teamSign: {
        cid: null,
        cname: null,
        ename: null,
        username1: null,
        username2: null,
        username3: null,
        school: null,
        type: 0,
        title: null,
        startTime: null,
        endTime: null,
        maxParticipants: 1,
      },
      loading: false,
    };
  },
  created() {
    this.CONTEST_STATUS_REVERSE = Object.assign({}, CONTEST_STATUS_REVERSE);
    this.QUOTA_TYPE_REVERSE = Object.assign({}, QUOTA_TYPE_REVERSE);
  },
  mounted() {
    this.keyword = this.$route.query.keyword || "";
    this.getContestList(this.currentPage);
  },
  methods: {
    // 切换页码回调
    currentChange(page) {
      this.currentPage = page;
      this.getContestList(page);
    },
    onPageSizeChange(limit) {
      this.limit = limit;
      this.getContestList(this.currentPage);
    },
    filterByKeyword() {
      this.currentPage = 1;
      this.currentChange(1);
    },
    filterByStatus() {
      this.currentPage = 1;
      this.getContestList(1);
    },
    getContestList(page) {
      this.loading = true;
      api
        .signup_getContestList(
          page,
          this.limit,
          this.contestStatus,
          this.keyword
        )
        .then(
          (res) => {
            this.loading = false;
            this.total = res.data.data.total;
            this.contestList = res.data.data.records;
          },
          (res) => {
            this.loading = false;
          }
        );
    },

    handleEditInfo(row) {
      this.handleSignupVisible = true;

      Object.keys(this.teamSign).forEach((element) => {
        if (row[element] !== undefined) {
          this.teamSign[element] = row[element];
        }
      });
      this.teamSign.cid = row.id;
      this.getTeamList(row.id);
    },
    getTeamList(signCid) {
      let params = {
        currentPage: 1,
        signCid: signCid,
      };
      api.getTeamSignList(params).then(
        (res) => {
          this.total = res.data.data.total;
          let teamList = res.data.data.records;
          teamList.forEach((user) => {
            user.teamConfig.forEach(({ username, realname, englishname }) => {
              ["1", "2", "3"].forEach((num) => {
                if (user[`username${num}`] === username) {
                  user[`realname${num}`] = realname;
                  user[`englishname${num}`] = englishname;
                }
              });
            });
          });

          this.filteredTeams = teamList.map((state) => ({
            label:
              state.cname && state.ename
                ? state.cname + " / " + state.ename
                : state.realname1 && state.englishname1
                ? state.realname1 + " / " + state.englishname1
                : state.cname ||
                  state.ename ||
                  state.realname1 ||
                  state.englishname1 ||
                  "",
            value: state.id,
          }));
        },
        (res) => {}
      );
    },

    goTeamSign(contestID) {
      this.$router.push({
        name: "signup-contest-sign-list",
        params: { contestID },
      });
    },
  },
};
</script>
<style scoped>
.filter-row {
  margin-top: 10px;
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
}
.filter-row span {
  margin-right: 15px;
  margin-top: 10px;
}

.el-tag--dark {
  border-color: #fff;
}
</style>
