<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">
          {{
          $t('m.Training_Problem_List')
          }}
        </span>
        <div class="filter-row">
          <span>
            <el-button
              type="primary"
              size="small"
              icon="el-icon-plus"
              @click="addProblemDialogVisible = true"
            >{{ $t('m.Add_From_Public_Problem') }}</el-button>
          </span>
          <span>
            <el-button
              type="success"
              size="small"
              @click="AddRemoteOJProblemDialogVisible = true"
              icon="el-icon-plus"
            >{{ $t('m.Add_Rmote_OJ_Problem') }}</el-button>
          </span>
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
        <vxe-table-column min-width="100" field="problemId" :title="$t('m.Display_ID')"></vxe-table-column>
        <vxe-table-column field="title" min-width="150" :title="$t('m.Title')" show-overflow></vxe-table-column>

        <vxe-table-column min-width="150" :title="$t('m.Selected_Description')">
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

        <vxe-table-column field="author" min-width="100" :title="$t('m.Author')" show-overflow></vxe-table-column>
        <vxe-table-column min-width="200" :title="$t('m.Training_Problem_Rank')">
          <template v-slot="{ row }">
            <el-input-number
              v-model="trainingProblemMap[row.id].rank"
              @change="handleChangeRank(trainingProblemMap[row.id])"
              :min="0"
              :max="2147483647"
            ></el-input-number>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="120" :title="$t('m.Auth')">
          <template v-slot="{ row }">
            <el-select
              v-model="row.auth"
              @change="changeProblemAuth(row)"
              :disabled="!isAdminRole"
              size="small"
            >
              <el-option :label="$t('m.Public_Problem')" :value="1" :disabled="!isAdminRole"></el-option>
              <el-option :label="$t('m.Private_Problem')" :value="2"></el-option>
              <el-option :label="$t('m.Contest_Problem')" :value="3" :disabled="true"></el-option>
            </el-select>
          </template>
        </vxe-table-column>
        <vxe-table-column :title="$t('m.Option')" min-width="300">
          <template v-slot="{ row }">
            <el-tooltip
              effect="dark"
              :content="$t('m.Edit')"
              placement="top"
              v-if="isMainAdminRole || row.author == userInfo.username"
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

            <el-tooltip effect="dark" :content="$t('m.Remove')" placement="top">
              <el-button
                icon="el-icon-close"
                size="mini"
                @click.native="removeProblem(row.id)"
                type="warning"
              ></el-button>
            </el-tooltip>
          </template>
        </vxe-table-column>
      </vxe-table>

      <div class="panel-options">
        <el-pagination
          class="page"
          layout="prev, pager, next, sizes"
          @current-change="currentChange"
          :page-size="pageSize"
          :total="total"
          @size-change="onPageSizeChange"
          :page-sizes="[10, 30, 50, 100]"
        ></el-pagination>
      </div>
    </el-card>

    <el-dialog
      :title="$t('m.Add_Training_Problem')"
      width="90%"
      :visible.sync="addProblemDialogVisible"
      :close-on-click-modal="false"
      z-index="3000"
    >
      <AddPublicProblem :trainingID="trainingId" @on-change="getProblemList"></AddPublicProblem>
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
        <p style="text-align: left; white-space: pre-wrap; color: #66b1ff;">
          <span style="font-size: 18px;">{{ $t('m.Remote_Tips_' + otherOJName) + ' '}}</span>
          <el-popover placement="right" trigger="hover">
            <p style="font-size: 14px;">{{ $t('m.Remote_Tips1') }}</p>
            <p style="font-size: 14px;">{{ $t('m.Remote_Tips2') }}</p>
            <p style="font-size: 14px;">{{ $t('m.Remote_Tips3') }}</p>
            <i slot="reference" class="el-icon-question"></i>
          </el-popover>
        </p>
        <el-form-item :label="$t('m.Problem_ID')" required>
          <el-input v-model="otherOJProblemId" size="small" @blur="updateProblemIdList"></el-input>
          <div class="userPreview">
            <span
              v-if="problemIdList.length"
            >{{ $t('m.The_actual_problemId_will_be') }} {{ problemIdList.join(',') }}</span>
            <span v-else>{{ errorMessage }}</span>
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
  </div>
</template>

<script>
import api from "@/common/api";
import AddPublicProblem from "@/components/admin/AddPublicProblem.vue";
import myMessage from "@/common/message";
import { REMOTE_OJ } from "@/common/constants";
import { mapGetters } from "vuex";
import utils from "@/common/utils";
export default {
  name: "ProblemList",
  components: {
    AddPublicProblem,
  },
  data() {
    return {
      problemListAuth: 0,
      oj: "All",
      pageSize: 10,
      total: 0,
      problemList: [],
      trainingProblemMap: {},
      keyword: "",
      loading: false,
      currentPage: 1,
      routeName: "",
      trainingId: "",
      // for make public use
      currentProblemID: "",
      currentRow: {},
      addProblemDialogVisible: false,
      AddRemoteOJProblemDialogVisible: false,
      addRemoteOJproblemLoading: false,
      otherOJName: "HDU",
      otherOJProblemId: "",
      REMOTE_OJ: {},
      problemIdList: [],
      errorMessage: "",
    };
  },
  mounted() {
    this.init();
  },
  computed: {
    ...mapGetters(["userInfo", "isAdminRole", "isMainAdminRole"]),
  },
  methods: {
    init() {
      this.routeName = this.$route.name;
      this.trainingId = this.$route.params.trainingId;
      this.getProblemList(this.currentPage);
      this.REMOTE_OJ = Object.assign({}, REMOTE_OJ);
    },

    goEdit(problemId) {
      this.$router.push({
        name: "admin-edit-problem",
        params: { problemId: problemId },
      });
    },
    // 切换页码回调
    currentChange(page) {
      this.currentPage = page;
      this.getProblemList(page);
    },
    onPageSizeChange(pageSize) {
      this.pageSize = pageSize;
      this.getProblemList(this.currentPage);
    },
    getProblemList(page = 1) {
      this.loading = true;
      let params = {
        limit: this.pageSize,
        currentPage: page,
        keyword: this.keyword,
        tid: this.trainingId,
        queryExisted: true,
      };
      if (this.problemListAuth != 0) {
        params["auth"] = this.problemListAuth;
      }
      api.admin_getTrainingProblemList(params).then(
        (res) => {
          this.loading = false;
          this.total = res.data.data.problemList.total;
          this.problemList = res.data.data.problemList.records;
          this.trainingProblemMap = res.data.data.trainingProblemMap;
        },
        (err) => {
          this.loading = false;
        }
      );
    },
    handleChangeRank(data) {
      api.admin_updateTrainingProblem(data).then((res) => {
        myMessage.success(this.$i18n.t("m.Update_Successfully"));
        this.getProblemList(1);
      });
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
          api
            .admin_deleteTrainingProblem(id, null)
            .then((res) => {
              myMessage.success(this.$i18n.t("m.Delete_successfully"));
              this.getProblemList(this.currentPage);
            })
            .catch(() => {});
        },
        () => {}
      );
    },
    removeProblem(pid) {
      this.$confirm(this.$i18n.t("m.Remove_Training_Problem_Tips"), "Tips", {
        type: "warning",
      }).then(
        () => {
          api
            .admin_deleteTrainingProblem(pid, this.trainingId)
            .then((res) => {
              myMessage.success("success");
              this.getProblemList(this.currentPage);
            })
            .catch(() => {});
        },
        () => {}
      );
    },
    downloadTestCase(problemID) {
      let url = "/api/file/download-testcase?pid=" + problemID;
      utils.downloadFile(url).then(() => {
        this.$alert(this.$i18n.t("m.Download_Testcase_Success"), "Tips");
      });
    },
    filterByKeyword() {
      this.currentChange(1);
    },
    addRemoteOJProblem() {
      if (utils.getValidateField(this.otherOJProblemId, "Problem_ID")) {
        return;
      }

      this.addRemoteOJproblemLoading = true;
      api
        .admin_addTrainingRemoteOJProblem(
          this.otherOJName,
          this.otherOJProblemId,
          this.trainingId,
          null
        )
        .then(
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
    changeProblemDescription(pid, peid) {
      let data = {
        pid: pid,
        peid: peid,
        tid: this.trainingId,
      };
      api.admin_changeTrainingProblemDescription(data).then((res) => {
        myMessage.success(this.$i18n.t("m.Update_Successfully"));
        this.currentChange(1);
      });
    },
    updateRemoteDescription(pid) {
      this.$confirm(this.$i18n.t("m.Update_RemoteDescription_Tips"), "Tips", {
        type: "warning",
      }).then(
        () => {
          api.admin_updateRemoteDescription(pid).then((res) => {
            myMessage.success(this.$i18n.t("m.Update_Successfully"));
            this.getProblemList(this.currentPage);
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
    isValidNumber(value) {
      return /^\d+$/.test(value);
    },
  },
  watch: {
    $route(newVal, oldVal) {
      if (
        newVal.params.trainingId != oldVal.params.trainingId ||
        newVal.name != oldVal.name
      ) {
        this.init();
      }
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
