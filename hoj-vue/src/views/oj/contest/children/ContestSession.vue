<template>
  <el-card shadow="always">
    <div slot="header">
      <span class="panel-title">
        <el-switch
          v-model="isIp"
          :active-text="$t('m.Reset_Ip')"
          :inactive-text="$t('m.Admin_Session')"
        ></el-switch>
      </span>
      <div class="filter-row" v-if="!isIp">
        <span>
          <vxe-input
            v-model="keyword"
            :placeholder="$t('m.Enter_Session_Keyword')"
            type="search"
            size="small"
            @search-click="filterByKeyword"
            @keyup.enter.native="filterByKeyword"
            style="width: 250px;"
          ></vxe-input>
        </span>
        <span>
          <vxe-input
            v-model="unkeyword"
            :placeholder="$t('m.Enter_Session_Unkeyword')"
            type="search"
            size="small"
            @search-click="filterByUnkeyword"
            @keyup.enter.native="filterByUnkeyword"
            style="width: 250px;"
          ></vxe-input>
        </span>
        <span>
          {{ $t('m.Auto_Refresh') }}(10s)
          <el-switch @change="handleAutoRefresh" v-model="autoRefresh"></el-switch>
        </span>
        <span>
          <el-button
            type="primary"
            @click="getSessionList(1)"
            size="small"
            icon="el-icon-refresh"
            :loading="btnLoading"
          >{{ $t('m.Refresh') }}</el-button>
        </span>
      </div>
      <div>
        <p></p>
      </div>
    </div>
    <vxe-table :key="tableKey" border="inner" stripe auto-resize align="center" :data="sessionList">
      <vxe-table-column field="username" :title="$t('m.Username')" min-width="150">
        <template v-slot="{ row }">
          <span>
            <a
              @click="getUserTotalSubmit(row.username)"
              style="color:rgb(87, 163, 243);"
            >{{ row.username }}</a>
          </span>
        </template>
      </vxe-table-column>
      <vxe-table-column field="realname" :title="$t('m.RealName')" min-width="150"></vxe-table-column>

      <vxe-table-column field="ip" title="IP" min-width="150" :visible="!isIp"></vxe-table-column>
      <vxe-table-column
        field="routeName"
        :title="$t('m.routeName')"
        min-width="150"
        :visible="!isIp"
      ></vxe-table-column>
      <vxe-table-column
        field="gmtCreate"
        min-width="150"
        :title="$t('m.Submit_Time')"
        :visible="!isIp"
      >
        <template v-slot="{ row }">
          <span>{{ row.gmtCreate | localtime }}</span>
        </template>
      </vxe-table-column>

      <vxe-table-column
        field="ipList"
        :title="$t('m.SubmitIp_List')"
        min-width="150"
        :formatter="formatIpList"
        :visible="isIp"
      ></vxe-table-column>
      <vxe-table-column field="option" :title="$t('m.Option')" min-width="150" :visible="isIp">
        <template v-slot="{ row }">
          <el-button
            type="primary"
            size="small"
            :loading="btnLoading2"
            icon="el-icon-refresh-right"
            @click="rejudgeProblem(row)"
            round
          >{{ $t('m.Reset') }}</el-button>
        </template>
      </vxe-table-column>
    </vxe-table>
    <Pagination
      :total="total"
      :page-size.sync="limit"
      :page-sizes="[10, 30, 50, 100, 300, 500]"
      :current.sync="page"
      @on-change="getSessionList"
      @on-page-size-change="getSessionList(1)"
      :layout="'prev, pager, next, sizes'"
    ></Pagination>
  </el-card>
</template>
<script>
import api from "@/common/api";
const Pagination = () => import("@/components/oj/common/Pagination");

export default {
  name: "ACM-Info-Admin",
  components: {
    Pagination,
  },
  data() {
    return {
      page: 1,
      limit: 10,
      total: 0,
      btnLoading: false,
      autoRefresh: false,
      sessionList: [],
      keyword: null,
      unkeyword: null,
      isIp: false,
      btnLoading2: false,
      tableKey: 0,
    };
  },
  mounted() {
    this.contestID = this.$route.params.contestID;
    this.getSessionList();
  },
  methods: {
    getUserTotalSubmit(username) {
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
        query: { username: username },
      });
    },
    getSessionList(page = 1) {
      this.btnLoading = true;

      let params = {
        cid: this.$route.params.contestID,
        limit: this.limit,
        currentPage: page,
      };

      if (!this.isIp) {
        if (this.keyword != null && this.keyword != "") {
          params.keyword = this.keyword;
        }

        if (this.unkeyword != null && this.unkeyword != "") {
          params.unkeyword = this.unkeyword;
        }
      }

      const func = this.isIp ? "getContestIp" : "getContestSession";
      api[func](params)
        .then((res) => {
          this.btnLoading = false;
          this.sessionList = res.data.data.records;
          this.total = res.data.data.total;
        })
        .catch(() => {
          this.btnLoading = false;
        });
    },
    handleAutoRefresh() {
      if (this.autoRefresh) {
        this.refreshFunc = setInterval(() => {
          this.page = 1;
          this.getSessionList();
        }, 10000);
      } else {
        clearInterval(this.refreshFunc);
      }
    },
    filterByKeyword() {
      this.getSessionList(1);
    },
    filterByUnkeyword() {
      this.getSessionList(1);
    },

    formatIpList({ cellValue }) {
      if (cellValue) {
        return cellValue.split(",").join("\n");
      }
      return "";
    },
    rejudgeProblem(row) {
      this.$confirm(this.$i18n.t("m.Contest_ResetIp_Tips"), "Tips", {
        confirmButtonText: this.$i18n.t("m.OK"),
        cancelButtonText: this.$i18n.t("m.Cancel"),
        type: "warning",
      }).then(
        () => {
          let params = {
            cid: this.contestID,
            uid: row.uid,
          };
          this.btnLoading2 = true;
          api
            .ContestResetIp(params)
            .then((res) => {
              myMessage.success(this.$i18n.t("m.Reset_successfully"));
              this.btnLoading2 = false;
            })
            .catch(() => {
              this.btnLoading2 = false;
            });
        },
        () => {}
      );
    },
  },
  beforeDestroy() {
    clearInterval(this.refreshFunc);
  },
  watch: {
    isIp(newVal, oldVal) {
      this.getSessionList();
      this.tableKey += 1;
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

.filter-right {
  float: right;
  margin-right: 20px;
  margin-top: 10px;
  margin-left: auto;
}

/deep/ .el-tag--dark {
  border-color: #fff;
}

/deep/ .el-switch__label span {
  font-size: 21px !important;
}
</style>
