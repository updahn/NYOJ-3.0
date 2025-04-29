<template>
  <el-card shadow="always">
    <div slot="header">
      <span class="panel-title">{{ $t('m.Signup_TeamSignList') }}</span>
      <div class="filter-row">
        <span v-if="isContestAdmin">
          <el-button
            type="success"
            icon="el-icon-s-check"
            @click="updateStatus(null, 1, null)"
            size="small"
          >{{ $t("m.Sign_Accept_More") }}</el-button>
        </span>
        <span>
          <vxe-input
            v-model="keyword"
            :placeholder="$t('m.Enter_Signkeyword')"
            type="search"
            size="large"
            @search-click="filterByKeyword"
            @keyup.enter.native="filterByKeyword"
            style="width: 400px;"
          ></vxe-input>
        </span>
        <span>
          <el-dropdown @command="onTypeChange" placement="bottom" trigger="hover" class="drop-menu">
            <span class="el-dropdown-link">
              {{ $t("m.Quota") }}
              <i class="el-icon-caret-bottom"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command>{{ $t("m.All") }}</el-dropdown-item>
              <el-dropdown-item
                v-for="(item, index) in QUOTA_TYPE_REVERSE"
                :key="index"
                :command="parseInt(index)"
              >{{ $t('m.' + item.name) }}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </span>
        <span>
          <el-dropdown
            @command="onStatusChange"
            placement="bottom"
            trigger="hover"
            class="drop-menu"
          >
            <span class="el-dropdown-link">
              {{ $t("m.Status") }}
              <i class="el-icon-caret-bottom"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command>{{ $t("m.All") }}</el-dropdown-item>
              <el-dropdown-item command="0">{{ $t("m.Not_Signed") }}</el-dropdown-item>
              <el-dropdown-item command="1">{{ $t("m.Sign_Accept") }}</el-dropdown-item>
              <el-dropdown-item command="2">{{ $t("m.Sign_Refuse") }}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </span>
        <div class="filter-right">
          <span v-if="isContestAdmin">
            {{ $t('m.Auto_Refresh') }}(10s)
            <el-switch @change="handleAutoRefresh" v-model="autoRefresh"></el-switch>
          </span>
          <span>
            <el-button
              type="primary"
              @click="getSignupList(1)"
              size="small"
              icon="el-icon-refresh"
              :loading="btnLoading"
            >{{ $t('m.Refresh') }}</el-button>
          </span>
        </div>
      </div>

      <vxe-table
        border="inner"
        stripe
        auto-resize
        align="center"
        :data="signList"
        ref="xTable"
        :loading="loadingTable"
        @checkbox-change="handleSelectionChange"
        @checkbox-all="handlechangeAll"
      >
        <vxe-table-column v-if="isContestAdmin" type="checkbox" width="60"></vxe-table-column>
        <vxe-table-column field="title" min-width="150" :title="$t('m.Contest_Title')"></vxe-table-column>

        <vxe-table-column
          v-if="signList[0].maxParticipants > 1"
          min-width="150"
          :title="$t('m.Team_Name')"
        >
          <template v-slot="{ row }">{{ [row.cname, row.ename].filter(Boolean).join(' | ') }}</template>
        </vxe-table-column>

        <vxe-table-column :title="$t('m.Team_Names')" min-width="220">
          <template slot-scope="{ row }">
            <span>{{ [1, 2, 3].map(num => formatRealname(row[`realname${num}`], row[`englishname${num}`])).filter(Boolean) .join(' | ') }}</span>
          </template>
        </vxe-table-column>
        <vxe-table-column field="instructor" min-width="150" :title="$t('m.Instructor')"></vxe-table-column>

        <vxe-table-column field="type" :title="$t('m.Team_Type')" min-width="150">
          <template v-slot="{ row }">
            <el-tag
              effect="dark"
              :value="parseInt(index)"
              :color="QUOTA_TYPE_REVERSE[row.type].color"
            >{{ $t('m.' + QUOTA_TYPE_REVERSE[row.type].name) }}</el-tag>
          </template>
        </vxe-table-column>
        <vxe-table-column field="status" :title="$t('m.Status')" min-width="150">
          <template v-slot="{ row }">
            <el-tag effect="dark" color="#f90" v-if="row.status == 0">{{ $t('m.Not_Signed') }}</el-tag>
            <el-tag effect="dark" color="#19be6b" v-if="row.status == 1">{{ $t('m.Sign_Accept') }}</el-tag>
            <el-tag effect="dark" color="#f56c6c" v-if="row.status == 2">{{ $t('m.Sign_Refuse') }}</el-tag>
          </template>
        </vxe-table-column>

        <vxe-table-column field="gmtCreate" min-width="150" :title="$t('m.Submit_Time')">
          <template v-slot="{ row }">
            <span>{{ row.gmtCreate | localtime }}</span>
          </template>
        </vxe-table-column>

        <vxe-table-column field="option" :title="$t('m.Option')" min-width="150">
          <template v-slot="{ row }">
            <div style="margin-bottom:10px">
              <el-tooltip effect="dark" :content="$t('m.Edit')" placement="top">
                <el-button
                  icon="el-icon-edit"
                  size="mini"
                  @click.native="getSignInfo(row.id)"
                  type="primary"
                ></el-button>
              </el-tooltip>
              <el-tooltip effect="dark" :content="$t('m.Remove_Signup')" placement="top">
                <el-button
                  icon="el-icon-close"
                  size="mini"
                  @click.native="removeTeamSign(row.id)"
                  type="warning"
                ></el-button>
              </el-tooltip>
            </div>

            <template v-if="isContestAdmin">
              <div style="margin-bottom:10px">
                <el-tooltip effect="dark" :content="$t('m.Sign_Accept')" placement="top">
                  <el-button
                    type="success"
                    size="small"
                    icon="el-icon-circle-check"
                    @click="updateStatus([row.id], 1, null)"
                  ></el-button>
                </el-tooltip>
                <el-tooltip effect="dark" :content="$t('m.Sign_Refuse')" placement="top">
                  <el-button
                    type="danger"
                    size="small"
                    icon="el-icon-circle-close"
                    @click="refuseSignDialog(row.id)"
                  ></el-button>
                </el-tooltip>
              </div>
            </template>
          </template>
        </vxe-table-column>
      </vxe-table>

      <el-dialog
        v-if="isContestAdmin"
        :title="$t('m.Refuse_Sign')"
        width="500px"
        :visible.sync="refuseDialogVisible"
        :close-on-click-modal="false"
      >
        <el-form :model="refuseSign" @submit.native.prevent>
          <el-form-item :label="$t('m.Refuse_msg')">
            <el-input v-model="refuseSign.msg" size="small" :maxlength="300"></el-input>
          </el-form-item>
          <el-form-item style="text-align:center">
            <el-button
              type="primary"
              @click="refuseSignBtn(refuseSign.signId, refuseSign.msg)"
              :loading="refuseLoading"
            >{{ $t('m.Send') }}</el-button>
          </el-form-item>
        </el-form>
      </el-dialog>

      <el-dialog :title="$t('m.Sign_View')" :visible.sync="viewDialogVisible" width="85%">
        <Team :teamSign.sync="teamSign"></Team>
        <div style="text-align:center">
          <el-button type="primary" @click="updateTeamSignup()">{{ $t('m.Edit') }}</el-button>
        </div>
      </el-dialog>
    </div>
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
  </el-card>
</template>

<script>
import api from "@/common/api";
import myMessage from "@/common/message";
import { mapGetters } from "vuex";
const Pagination = () => import("@/components/oj/common/Pagination");
const Team = () => import("@/components/signup/Team");
import { QUOTA_TYPE_REVERSE } from "@/common/constants";

export default {
  name: "SignupTeamSignList",
  components: {
    Pagination,
    Team,
  },
  data() {
    return {
      currentPage: 1,
      limit: 15,
      total: 0,
      btnLoading: false,
      autoRefresh: false,
      contestID: null,
      signList: [],
      teamSign: [],
      loadingTable: false,
      selectedSigns: [],
      refuseDialogVisible: false,
      viewDialogVisible: false,
      refuseLoading: false,
      refuseSign: {
        msg: "信息未填写完整",
        signId: null,
      },
      keyword: null,
      searchStatus: null,
      searchType: null,
      sizes: ["S", "M", "L", "XL", "XXL", "XXXL", "XXXXL", "XXXXXL"],
    };
  },
  mounted() {
    this.cid = this.$route.params.contestID;
    this.getSignupList(1);
  },
  created() {
    this.$store.dispatch("getContest");
    this.QUOTA_TYPE_REVERSE = Object.assign({}, QUOTA_TYPE_REVERSE);
  },
  methods: {
    // 切换页码回调
    currentChange(page) {
      this.currentPage = page;
      this.getSignupList(page);
    },
    onPageSizeChange(limit) {
      this.limit = limit;
      this.getSignupList(this.currentPage);
    },
    filterByKeyword() {
      this.currentChange(1);
    },
    filterByStar() {
      this.currentChange(1);
    },
    filterByGirls() {
      this.currentChange(1);
    },
    onStatusChange(status) {
      this.searchStatus = status;
      this.currentPage = 1;
      this.currentChange(1);
    },
    onTypeChange(type) {
      this.searchType = type;
      this.currentPage = 1;
      this.currentChange(1);
    },
    refuseSignBtn(id, msg) {
      this.refuseLoading = false;
      this.refuseDialogVisible = false;
      this.updateStatus([id], 2, msg);
    },
    refuseSignDialog(id) {
      this.refuseDialogVisible = true;
      this.refuseSign.signId = id;
    },
    getSignInfo(id) {
      api
        .getTeamSign(id)
        .then((res) => {
          this.teamSign = res.data.data;
          this.viewDialogVisible = true;
        })
        .catch(() => {
          this.teamSign = [];
          this.viewDialogVisible = true;
        });
    },
    updateStatus(ids, status, msg) {
      if (!ids) {
        ids = this.selectedSigns;
      }
      if (ids.length > 0) {
        api.updateTeamSignStatus(ids, this.cid, status, msg).then((res) => {
          myMessage.success(this.$i18n.t("m.Update_Successfully"));
          this.selectedSigns = [];
          this.getSignupList(this.page);
        });
      } else {
        myMessage.warning(
          this.$i18n.t("m.The_number_of_signs_selected_cannot_be_empty")
        );
      }
    },
    updateTeamSignup() {
      api
        .updateTeamSign(this.teamSign)
        .then((res) => {
          myMessage.success(this.$i18n.t("m.Update_Successfully"));
          this.getSignupList(this.page);
          this.viewDialogVisible = false;
        })
        .catch(() => {
          this.getSignupList(this.page);
          this.viewDialogVisible = false;
        });
    },

    getSignupList(page = 1) {
      this.loadingTable = true;
      this.btnLoading = true;
      let params = {
        currentPage: page,
        limit: this.limit,
        cid: this.cid,
        signCid: this.cid,
      };

      if (this.keyword != null && this.keyword != "") {
        params.keyword = this.keyword;
      }
      if (this.searchStatus != null && this.searchStatus != "All") {
        params.status = this.searchStatus;
      }
      if (this.searchType != null && this.searchType != "All") {
        params.type = this.searchType;
      }

      api
        .getTeamSignList(params)
        .then((res) => {
          this.loadingTable = false;
          this.btnLoading = false;
          this.signList = res.data.data.records;
          this.signList.forEach((user) => {
            user.teamConfig.forEach(({ username, realname, englishname }) => {
              ["1", "2", "3"].forEach((num) => {
                if (user[`username${num}`] === username) {
                  user[`realname${num}`] = realname;
                  user[`englishname${num}`] = englishname;
                }
              });
            });
          });

          this.total = res.data.data.total;
        })
        .catch(() => {
          this.btnLoading = false;
          this.loadingTable = false;
          this.signList = [];
          this.total = 0;
        });
    },
    // 改变选中的内容
    handleSelectionChange({ records }) {
      this.selectedSigns = [];
      for (let num = 0; num < records.length; num++) {
        this.selectedSigns.push(records[num].id);
      }
    },
    // 一键全部选中，改变选中的内容列表
    handlechangeAll() {
      let userList = this.$refs.xTable.getCheckboxRecords();
      this.selectedSigns = [];
      for (let num = 0; num < userList.length; num++) {
        this.selectedSigns.push(userList[num].id);
      }
    },
    handleAutoRefresh() {
      if (this.autoRefresh) {
        this.refreshFunc = setInterval(() => {
          this.page = 1;
          this.getSignupList(1);
        }, 10000);
      } else {
        clearInterval(this.refreshFunc);
      }
    },
    formatRealname(realname, englishname) {
      if (realname && englishname) return `${realname} / ${englishname}`;
      return realname || "";
    },
    removeTeamSign(id) {
      this.$confirm(this.$i18n.t("m.Remove_Team_Sign_Tips2"), "Tips", {
        type: "warning",
      }).then(
        () => {
          api
            .removeTeamSign(id)
            .then((res) => {
              myMessage.success("success");
              this.getSignupList(this.page);
            })
            .catch(() => {});
        },
        () => {}
      );
    },
  },
  beforeDestroy() {
    clearInterval(this.refreshFunc);
  },
  computed: {
    ...mapGetters(["isContestAdmin"]),
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
  margin-left: auto;
}

/deep/ .el-tag--dark {
  border-color: #fff;
}
.info-title {
  font-size: 18px;
  font-weight: 500;
  padding-top: 10px;
  padding-bottom: 20px;
  line-height: 30px;
  color: #409eff;
  font-family: "Raleway";
}
</style>
