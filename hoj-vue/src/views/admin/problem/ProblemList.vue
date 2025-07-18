<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">
          {{
          query.contestId ? $t('m.Contest_Problem_List') : $t('m.Problem_List')
          }}
        </span>
        <div class="filter-row">
          <span>
            <el-button
              type="primary"
              size="small"
              @click="goCreateProblem"
              icon="el-icon-plus"
            >{{ $t('m.Create') }}</el-button>
          </span>
          <span v-if="query.contestId">
            <el-button
              type="primary"
              size="small"
              icon="el-icon-plus"
              @click="addProblemDialogVisible = true"
            >{{ $t('m.Add_From_Public_Problem') }}</el-button>
          </span>
          <span v-if="!(query.contestId && (contestAuth == CONTEST_TYPE.EXAMINATION))">
            <el-button
              type="success"
              size="small"
              @click="AddRemoteOJProblemDialogVisible = true"
              icon="el-icon-plus"
            >{{ $t('m.Add_Rmote_OJ_Problem') }}</el-button>
          </span>
          <span>
            <vxe-input
              v-model="query.keyword"
              :placeholder="$t('m.Enter_keyword')"
              type="search"
              size="medium"
              @search-click="filterByKeyword"
              @keyup.enter.native="filterByKeyword"
            ></vxe-input>
          </span>

          <span
            v-if="!(query.contestId && (contestAuth == CONTEST_TYPE.EXAMINATION || contestAuth == CONTEST_TYPE.PRIVATE))"
          >
            <el-select
              v-model="query.oj"
              @change="ProblemListChangeFilter"
              size="small"
              style="width: 180px;"
            >
              <el-option :label="$t('m.All_Problem')" :value="'All'"></el-option>
              <el-option :label="$t('m.My_OJ')" :value="'Mine'"></el-option>
              <el-option
                :label="remoteOj.name"
                :key="index"
                :value="remoteOj.key"
                v-for="(remoteOj, index) in REMOTE_OJ"
              ></el-option>
            </el-select>
          </span>

          <span v-if="!query.contestId">
            <el-select
              v-model="query.problemListAuth"
              @change="ProblemListChangeFilter"
              size="small"
              style="width: 180px;"
            >
              <el-option :label="$t('m.All_Problem')" :value="0"></el-option>
              <el-option :label="$t('m.Public_Problem')" :value="1"></el-option>
              <el-option :label="$t('m.Private_Problem')" :value="2"></el-option>
              <el-option :label="$t('m.Contest_Problem')" :value="3"></el-option>
            </el-select>
          </span>
          <span>
            <el-select
              v-model="query.type"
              @change="ProblemListChangeFilter"
              size="small"
              style="width: 180px"
            >
              <el-option :label="$t('m.All_Problem')" :value="'All'"></el-option>
              <el-option :label="'ACM'" :value="'0'"></el-option>
              <el-option :label="'OI'" :value="'1'"></el-option>
              <el-option :label="$t('m.Selection')" :value="'2'"></el-option>
              <el-option :label="$t('m.Filling')" :value="'3'"></el-option>
              <el-option :label="$t('m.Decide')" :value="'4'"></el-option>
            </el-select>
          </span>
          <span>
            <el-select
              v-model="query.difficulty"
              @change="ProblemListChangeFilter"
              size="small"
              style="width: 180px"
            >
              <el-option :label="$t('m.All_Problem')" :value="'All'"></el-option>
              <el-option
                :label="getLevelName(key)"
                :value="key"
                v-for="(value, key, index) in PROBLEM_LEVEL"
                :key="index"
              ></el-option>
            </el-select>
          </span>
        </div>
      </div>
      <vxe-table
        stripe
        auto-resize
        :data="problemList"
        ref="adminProblemList"
        :loading="loading"
        align="center"
      >
        <vxe-table-column min-width="64" field="id" title="ID"></vxe-table-column>
        <vxe-table-column
          min-width="100"
          field="problemId"
          :title="$t('m.Display_ID')"
          v-if="!isContest"
        ></vxe-table-column>

        <vxe-table-column min-width="150" :title="$t('m.Original_Display')" v-else align="left">
          <template v-slot="{ row }">
            <p v-if="query.contestId">{{ $t('m.Display_ID') }}：{{ row.problemId }}</p>
            <p v-if="query.contestId">{{ $t('m.Title') }}：{{ row.title }}</p>
            <span v-else>{{ row.problemId }}</span>
          </template>
        </vxe-table-column>

        <vxe-table-column
          field="title"
          min-width="150"
          :title="$t('m.Title')"
          show-overflow
          v-if="!isContest"
        ></vxe-table-column>

        <vxe-table-column min-width="150" :title="$t('m.Contest_Display')" v-else align="left">
          <template v-slot="{ row }">
            <p v-if="contestProblemMap[row.id]">
              {{ $t('m.Display_ID') }}：{{
              contestProblemMap[row.id]['displayId']
              }}
            </p>
            <p v-if="contestProblemMap[row.id]">
              {{ $t('m.Title') }}：{{
              contestProblemMap[row.id]['displayTitle']
              }}
            </p>
            <span v-if="contestProblemMap[row.id]">
              {{ $t('m.Balloon_Color') }}：
              <el-color-picker
                v-model="contestProblemMap[row.id].color"
                show-alpha
                :predefine="predefineColors"
                size="small"
                style="vertical-align: middle;"
                @change="
                  changeContestProblemColor(
                    contestProblemMap[row.id].id,
                    contestProblemMap[row.id].color
                  )
                "
              ></el-color-picker>
            </span>
            <span v-else>{{ row.title }}</span>
          </template>
        </vxe-table-column>

        <vxe-table-column
          min-width="150"
          :title="$t('m.Selected_Description')"
          v-if="query.contestId"
        >
          <template v-slot="{ row }">
            <el-select
              v-model="row.peid"
              @change="changeProblemDescription(row.id,row.peid)"
              size="small"
            >
              <el-option
                v-for="item in row.problemDescriptionList"
                :key="item.id"
                :label="item.title"
                :value="item.id"
              >{{ item.title }}</el-option>
            </el-select>
          </template>
        </vxe-table-column>

        <vxe-table-column
          v-if="query.contestId && (contestAuth == CONTEST_TYPE.EXAMINATION)"
          field="score"
          min-width="50"
          :title="$t('m.Score')"
          show-overflow
        >
          <template v-slot="{ row }">
            <el-button
              size="mini"
              type="primary"
              @click="handleUpdateProblemScore(row.title, row.id, row.peid, row.score)"
            >{{ row.score ? row.score : '--' }}</el-button>
          </template>
        </vxe-table-column>

        <vxe-table-column field="author" min-width="130" :title="$t('m.Author')" show-overflow></vxe-table-column>
        <vxe-table-column min-width="120" :title="$t('m.Created_Time')">
          <template v-slot="{ row }">{{ row.gmtCreate | localtime }}</template>
        </vxe-table-column>
        <vxe-table-column
          min-width="96"
          field="modifiedUser"
          :title="$t('m.Modified_User')"
          show-overflow
        ></vxe-table-column>
        <vxe-table-column min-width="120" :title="$t('m.Auth')">
          <template v-slot="{ row }">
            <el-select
              v-model="row.auth"
              @change="changeProblemAuth(row)"
              size="small"
              :disabled="!isAdminRole && !query.contestId"
            >
              <el-option :label="$t('m.Public_Problem')" :value="1" :disabled="!isAdminRole"></el-option>
              <el-option :label="$t('m.Private_Problem')" :value="2"></el-option>
              <el-option :label="$t('m.Contest_Problem')" :value="3"></el-option>
            </el-select>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="60" :title="$t('m.Type')">
          <template v-slot="{ row }">
            <el-tag effect="dark" color="#1559A1" v-if="row.type == 0">{{ 'ACM' }}</el-tag>
            <el-tag effect="dark" color="#19be6b" v-if="row.type == 1">{{ 'OI' }}</el-tag>
            <el-tag effect="dark" color="#66B1FF" v-if="row.type == 2">{{ $t('m.Selection') }}</el-tag>
            <el-tag effect="dark" color="#EEAC3C" v-if="row.type == 3">{{ $t('m.Filling') }}</el-tag>
            <el-tag effect="dark" color="#881E1F" v-if="row.type == 4">{{ $t('m.Decide') }}</el-tag>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="50" :title="$t('m.Level')">
          <template v-slot="{ row }">
            <span>{{getLevelName(row.difficulty)}}</span>
          </template>
        </vxe-table-column>
        <vxe-table-column :title="$t('m.Option')" min-width="300">
          <template v-slot="{ row }">
            <el-tooltip
              effect="dark"
              :content="$t('m.Edit')"
              placement="top"
              v-if=" isMainAdminRole || row.author == userInfo.username"
            >
              <el-button
                icon="el-icon-edit-outline"
                size="mini"
                @click.native="goEdit(row.id)"
                type="primary"
              ></el-button>
            </el-tooltip>

            <el-tooltip
              effect="dark"
              :content="$t('m.Update_RemoteDescription')"
              placement="top"
              v-show="row.isRemote === true"
              v-if="isMainAdminRole || row.author == userInfo.username"
            >
              <el-button
                icon="el-icon-refresh"
                size="mini"
                @click.native="updateRemoteDescription(row.id)"
                type="info"
              ></el-button>
            </el-tooltip>

            <el-tooltip
              effect="dark"
              :content="$t('m.Download_Testcase')"
              placement="top"
              v-if="isMainAdminRole || row.author == userInfo.username"
            >
              <el-button
                icon="el-icon-download"
                size="mini"
                @click.native="downloadTestCase(row.id)"
                type="success"
              ></el-button>
            </el-tooltip>

            <el-tooltip
              effect="dark"
              :content="$t('m.Remove')"
              placement="top"
              v-if="query.contestId"
            >
              <el-button
                icon="el-icon-close"
                size="mini"
                @click.native="removeProblem(row.id)"
                type="warning"
              ></el-button>
            </el-tooltip>

            <el-tooltip
              effect="dark"
              :content="$t('m.Delete')"
              placement="top"
              v-if="!query.contestId && (isSuperAdmin || row.author == userInfo.username)"
            >
              <el-button
                icon="el-icon-delete-solid"
                size="mini"
                @click.native="deleteProblem(row.id)"
                type="danger"
              ></el-button>
            </el-tooltip>
          </template>
        </vxe-table-column>
      </vxe-table>

      <div class="panel-options" v-if="showPagination">
        <el-pagination
          class="page"
          layout="prev, pager, next, sizes"
          @current-change="currentChange"
          :page-size.sync="query.pageSize"
          :total.sync="total"
          :current-page.sync="query.currentPage"
          @size-change="onPageSizeChange"
          :page-sizes="[10, 30, 50, 100]"
        ></el-pagination>
      </div>
    </el-card>

    <el-dialog
      :title="$t('m.Add_Contest_Problem')"
      v-if="query.contestId"
      width="90%"
      :visible.sync="addProblemDialogVisible"
      :close-on-click-modal="false"
      z-index="3000"
    >
      <AddPublicProblem :contestID="query.contestId" @on-change="getProblemList"></AddPublicProblem>
    </el-dialog>

    <el-dialog
      :title="$t('m.Add_Rmote_OJ_Problem')"
      width="950px"
      :visible.sync="AddRemoteOJProblemDialogVisible"
      :close-on-click-modal="false"
      z-index="3000"
    >
      <el-form>
        <el-form-item :label="$t('m.Remote_OJ')">
          <el-select v-model="otherOJName" style="width:100%;">
            <el-option
              :label="remoteOj.name"
              :value="remoteOj.key"
              v-for="(remoteOj, index) in REMOTE_OJ"
              :key="index"
            ></el-option>
          </el-select>
        </el-form-item>
        <span style="text-align: left; white-space: pre-wrap; color: #66b1ff;">
          <p style="font-size: 18px;">{{ $t('m.Remote_Tips_' + otherOJName) + ' '}}</p>
          <p style="font-size: 14px;">{{ $t('m.Remote_Tips1') }}</p>
        </span>
        <el-form-item :label="$t('m.Problem_ID')" required>
          <el-popover placement="right" trigger="hover">
            <p style="font-size: 14px;">{{ $t('m.Remote_Tips2') }}</p>
            <p style="font-size: 14px;">{{ $t('m.Remote_Tips3') }}</p>
            <i slot="reference" class="el-icon-question"></i>
          </el-popover>
          <el-input v-model="otherOJProblemId" size="small" @blur="updateProblemIdList"></el-input>
          <div class="userPreview">
            <span
              v-if="problemIdList.length"
            >{{ $t('m.The_actual_problemId_will_be') }} {{ problemIdList.join(',') }}</span>
            <span v-else>{{ errorMessage }}</span>
          </div>
        </el-form-item>

        <el-form-item
          v-if="query.contestId"
          :label="$t('m.Enter_The_Problem_Display_ID_in_the_Contest')"
          required
        >
          <el-popover placement="right" trigger="hover">
            <p style="font-size: 14px;">{{ $t('m.Remote_Tips2') }}</p>
            <p style="font-size: 14px;">{{ $t('m.Remote_Tips3') }}</p>
            <i slot="reference" class="el-icon-question"></i>
          </el-popover>
          <el-input v-model="displayId" size="small" @blur="updateDisplayIdList"></el-input>
          <div class="userPreview">
            <span
              v-if="displayIdList.length"
            >{{ $t('m.The_actual_displayId_will_be') }} {{ displayIdList.join(',') }}</span>
            <span v-else>{{ errorMessage2 }}</span>
          </div>
        </el-form-item>

        <el-form-item style="text-align:center">
          <el-button
            type="primary"
            icon="el-icon-plus"
            @click="addRemoteOJProblem"
            :loading="addRemoteOJproblemLoading"
          >{{ $t('m.Add') }}</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
    <el-dialog
      width="350px"
      :visible.sync="updateProblemScoreDialogVisible"
      :close-on-click-modal="false"
      center
    >
      <template #title>
        <span style="font-size: 18px;">{{ $t('m.Update_Problem_Score') }}</span>
        <br />
        <span style="font-size: 14px;">(Title: {{ contestProblemDto.title }})</span>
      </template>

      <el-form>
        <el-form-item :label="$t('m.Score')" required>
          <el-input-number size="small" v-model="contestProblemDto.score"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button
          type="primary"
          @click="changeContestProblemScore(contestProblemDto.pid, contestProblemDto.peid, contestProblemDto.score)"
          :loading="changeProblemScoreLoading"
        >{{ $t('m.To_Update') }}</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import api from "@/common/api";
import utils from "@/common/utils";
import AddPublicProblem from "@/components/admin/AddPublicProblem.vue";
import myMessage from "@/common/message";
import {
  REMOTE_OJ,
  PROBLEM_LEVEL,
  CONTEST_TYPE,
  JUDGE_STATUS_RESERVE,
} from "@/common/constants";
import { mapGetters } from "vuex";
export default {
  name: "ProblemList",
  components: {
    AddPublicProblem,
  },
  data() {
    return {
      total: 0,
      query: {
        problemListAuth: 0,
        oj: "All",
        pageSize: 10,
        keyword: "",
        currentPage: 1,
        contestId: null,
        difficulty: "All",
        type: "All",
      },
      problemList: [],
      contestProblemMap: {},
      loading: false,
      routeName: "",
      // for make public use
      currentProblemID: "",
      currentRow: {},
      addProblemDialogVisible: false,
      AddRemoteOJProblemDialogVisible: false,
      addRemoteOJproblemLoading: false,
      otherOJName: "HDU",
      otherOJProblemId: "",
      REMOTE_OJ: {},
      PROBLEM_LEVEL: {},
      displayId: "",

      showPagination: false,

      predefineColors: [
        "#ff4500",
        "#ff8c00",
        "#ffd700",
        "#90ee90",
        "#00ced1",
        "#1e90ff",
        "#c71585",
      ],
      problemIdList: [],
      displayIdList: [],
      errorMessage: "",
      errorMessage2: "",
      updateProblemScoreDialogVisible: false,
      changeProblemScoreLoading: false,
      CONTEST_TYPE: {},
      JUDGE_STATUS_RESERVE: {},
      contestProblemDto: {
        title: "",
        pid: null,
        peid: null,
        score: null,
      },
    };
  },
  mounted() {
    this.init();
  },
  created() {
    if (this.$route.params.contestID) {
      this.$store.dispatch("getContest");
    }
  },
  computed: {
    ...mapGetters([
      "userInfo",
      "isSuperAdmin",
      "isAdminRole",
      "isMainAdminRole",
      "contestAuth",
    ]),
    isContest() {
      return !(this.routeName == "admin-problem-list" && !this.query.contestId);
    },
  },
  methods: {
    init() {
      this.routeName = this.$route.name;
      let query = this.$route.query;
      this.query.currentPage = query.currentPage || 1;
      this.query.pageSize = parseInt(query.pageSize) || 10;
      this.query.keyword = query.keyword;
      this.query.problemListAuth = query.problemListAuth
        ? parseInt(query.problemListAuth)
        : 0;
      this.query.oj = query.oj || "All";
      this.query.difficulty = query.difficulty || "All";
      this.query.type = query.type || "All";
      this.query.contestId = this.$route.params.contestID;
      this.contestProblemMap = {};
      this.getProblemList();
      this.REMOTE_OJ = Object.assign({}, REMOTE_OJ);
      this.PROBLEM_LEVEL = Object.assign({}, PROBLEM_LEVEL);
      this.CONTEST_TYPE = Object.assign({}, CONTEST_TYPE);
      this.JUDGE_STATUS_RESERVE = Object.assign({}, JUDGE_STATUS_RESERVE);
    },

    goEdit(problemId) {
      if (this.routeName === "admin-problem-list") {
        this.$router.push({
          name: "admin-edit-problem",
          params: { problemId },
          query: {
            back: this.$route.fullPath,
          },
        });
      } else if (this.routeName === "admin-contest-problem-list") {
        this.$router.push({
          name: "admin-edit-contest-problem",
          params: { problemId: problemId, contestID: this.query.contestId },
        });
      }
    },
    goCreateProblem() {
      if (this.routeName === "admin-problem-list") {
        this.$router.push({
          name: "admin-create-problem",
          query: {
            back: this.$route.fullPath,
          },
        });
      } else if (this.routeName === "admin-contest-problem-list") {
        this.$router.push({
          name: "admin-create-contest-problem",
          params: { contestID: this.query.contestId },
        });
      }
    },

    pushRouter() {
      if (this.query.contestId) {
        this.$router.push({
          name: "admin-contest-problem-list",
          query: this.query,
          params: {
            contestID: this.query.contestId,
          },
        });
      } else {
        this.$router.push({
          name: "admin-problem-list",
          query: this.query,
        });
      }
    },

    // 切换页码回调
    currentChange(page) {
      this.query.currentPage = page;
      this.pushRouter();
    },
    onPageSizeChange(pageSize) {
      this.query.pageSize = pageSize;
      this.pushRouter();
    },
    getProblemList() {
      let params = {
        limit: this.query.pageSize,
        currentPage: this.query.currentPage,
        keyword: this.query.keyword,
        cid: this.query.contestId,
        oj: this.query.oj,
      };
      if (this.problemListAuth != 0) {
        params["auth"] = this.query.problemListAuth;
      }
      if (this.query.type !== "All") {
        params["type"] = this.query.type;
      }
      if (this.query.difficulty !== "All") {
        params["difficulty"] = this.query.difficulty;
      }
      this.loading = true;
      if (this.routeName === "admin-problem-list") {
        this.showPagination = false;
        api.admin_getProblemList(params).then(
          (res) => {
            this.loading = false;
            this.total = res.data.data.total;
            this.problemList = res.data.data.records;
            this.showPagination = true;
          },
          (err) => {
            this.loading = false;
            this.showPagination = true;
          }
        );
      } else {
        this.showPagination = false;
        api.admin_getContestProblemList(params).then(
          (res) => {
            this.loading = false;
            this.total = res.data.data.problemList.total;
            this.problemList = res.data.data.problemList.records;
            this.contestProblemMap = res.data.data.contestProblemMap;
            this.showPagination = true;
          },
          (err) => {
            this.loading = false;
            this.showPagination = true;
          }
        );
      }
    },

    changeProblemAuth(row) {
      api.admin_changeProblemAuth(row).then((res) => {
        myMessage.success(this.$i18n.t("m.Update_Successfully"));
      });
    },

    deleteProblem(id) {
      this.$confirm(this.$i18n.t("m.Delete_Problem_Tips"), "Tips", {
        type: "warning",
      }).then(
        () => {
          let funcName =
            this.routeName === "admin-problem-list"
              ? "admin_deleteProblem"
              : "admin_deleteContestProblem";
          api[funcName](id, null)
            .then((res) => {
              myMessage.success(this.$i18n.t("m.Delete_successfully"));
              this.getProblemList();
            })
            .catch(() => {});
        },
        () => {}
      );
    },
    removeProblem(pid) {
      this.$confirm(this.$i18n.t("m.Remove_Contest_Problem_Tips"), "Tips", {
        type: "warning",
      }).then(
        () => {
          api
            .admin_deleteContestProblem(pid, this.query.contestId)
            .then((res) => {
              myMessage.success("success");
              this.getProblemList();
            })
            .catch(() => {});
        },
        () => {}
      );
    },
    updateProblem(row) {
      let data = Object.assign({}, row);
      let funcName = "";
      if (this.query.contestId) {
        data.contest_id = this.query.contestId;
        funcName = "admin_editContestProblem";
      } else {
        funcName = "admin_editProblem";
      }
      api[funcName](data)
        .then((res) => {
          myMessage.success(this.$i18n.t("m.Update_Successfully"));
          this.getProblemList();
        })
        .catch(() => {});
    },
    downloadTestCase(problemID) {
      let url = "/api/file/download-testcase?pid=" + problemID;
      utils.downloadFile(url).then(() => {
        this.$alert(this.$i18n.t("m.Download_Testcase_Success"), "Tips");
      });
    },
    ProblemListChangeFilter() {
      this.query.currentPage = 1;
      this.pushRouter();
    },
    filterByKeyword() {
      this.query.currentPage = 1;
      this.pushRouter();
    },
    addRemoteOJProblem() {
      if (!this.otherOJProblemId) {
        myMessage.error(this.$i18n.t("m.Problem_ID_is_required"));
        return;
      }
      if (this.query.contestId) {
        if (utils.getValidateField(this.displayId, "Problem_ID")) {
          return;
        }
      }

      if (
        this.problemIdList.length > 0 &&
        this.displayIdList.length != this.problemIdList.length
      ) {
        myMessage.error(
          this.$i18n.t("m.Problem_ID_and_Display_ID_length_not_match")
        );
        return;
      }

      this.addRemoteOJproblemLoading = true;
      let funcName = "";
      if (this.query.contestId) {
        funcName = "admin_addContestRemoteOJProblem";
      } else {
        funcName = "admin_addRemoteOJProblem";
      }
      api[funcName](
        this.otherOJName,
        this.otherOJProblemId,
        null,
        this.query.contestId,
        this.displayId
      ).then(
        (res) => {
          this.addRemoteOJproblemLoading = false;
          this.AddRemoteOJProblemDialogVisible = false;
          myMessage.success(this.$i18n.t("m.Add_Successfully"));
          this.currentChange(1);
        },
        (err) => {
          this.addRemoteOJproblemLoading = false;
        }
      );
    },
    changeContestProblemColor(id, color) {
      let data = {
        id: id,
        color: color,
      };
      api.admin_setContestProblemInfo(data).then((res) => {
        myMessage.success(this.$i18n.t("m.Update_Balloon_Color_Successfully"));
      });
    },
    getLevelName(difficulty) {
      return utils.getLevelName(difficulty);
    },
    getLevelName(difficulty) {
      return utils.getLevelName(difficulty);
    },
    changeProblemDescription(pid, peid) {
      let data = {
        pid: pid,
        peid: peid,
        cid: this.query.contestId,
      };
      api.admin_changeContestProblemDescription(data).then((res) => {
        myMessage.success(this.$i18n.t("m.Update_Successfully"));
        this.pushRouter();
      });
    },
    changeContestProblemScore(pid, peid, score) {
      this.changeProblemScoreLoading = true;
      let data = {
        pid: pid,
        peid: peid,
        score: score,
      };
      api.admin_changeContestProblemScore(data).then((res) => {
        myMessage.success(this.$i18n.t("m.Update_Successfully"));
        this.getProblemList();
        this.changeProblemScoreLoading = false;
        this.updateProblemScoreDialogVisible = false;
      });
    },
    updateRemoteDescription(pid) {
      this.$confirm(this.$i18n.t("m.Update_RemoteDescription_Tips"), "Tips", {
        type: "warning",
      }).then(
        () => {
          api.admin_updateRemoteDescription(pid).then((res) => {
            myMessage.success(this.$i18n.t("m.Update_Successfully"));
            this.getProblemList();
          });
        },
        () => {}
      );
    },
    updateProblemIdList() {
      const inputValue = this.otherOJProblemId.trim();

      // 检查是否是范围格式
      if (inputValue.includes("-")) {
        const [start, end] = inputValue.split("-").map((num) => num.trim());

        if (!this.isValidNumber(start) || !this.isValidNumber(end)) {
          this.errorMessage = this.$i18n.t("m.Range_ID_must_be_valid_number");
          this.problemIdList = [];
          return;
        }

        const startNum = parseInt(start);
        const endNum = parseInt(end);

        if (startNum > endNum) {
          this.errorMessage = this.$i18n.t("m.Range_ID_error");
          this.problemIdList = [];
          return;
        } else {
          this.problemIdList = Array.from(
            { length: endNum - startNum + 1 },
            (_, i) => (startNum + i).toString()
          );
        }
      } else if (inputValue.includes(",")) {
        this.problemIdList = inputValue.split(",").map((num) => num.trim());
      } else {
        this.problemIdList = [inputValue];
      }
    },
    updateDisplayIdList() {
      const inputValue = this.displayId.trim();

      // 检查是否是范围格式
      if (inputValue.includes("-")) {
        const [start, end] = inputValue.split("-").map((num) => num.trim());

        if (!this.isValidNumber(start) || !this.isValidNumber(end)) {
          this.errorMessage2 = this.$i18n.t("m.Range_ID_must_be_valid_number");
          this.displayIdList = [];
          return;
        }

        const startNum = parseInt(start);
        const endNum = parseInt(end);

        if (startNum > endNum) {
          this.errorMessage2 = this.$i18n.t("m.Range_ID_error");
          this.displayIdList = [];
          return;
        } else {
          this.displayIdList = Array.from(
            { length: endNum - startNum + 1 },
            (_, i) => (startNum + i).toString()
          );
        }
      } else if (inputValue.includes(",")) {
        this.displayIdList = inputValue.split(",").map((num) => num.trim());
      } else {
        this.displayIdList = [inputValue];
      }
    },
    isValidNumber(value) {
      return /^\d+$/.test(value);
    },
    handleUpdateProblemScore(title, pid, peid, score) {
      this.updateProblemScoreDialogVisible = true;
      this.contestProblemDto = {
        title: title,
        pid: pid,
        peid: peid,
        score: score,
      };
    },
  },
  watch: {
    $route(newVal, oldVal) {
      this.init();
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
.userPreview {
  padding-left: 10px;
  padding-top: 5px;
  color: red;
  font-size: 16px;
}
</style>
