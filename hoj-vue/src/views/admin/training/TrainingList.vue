<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ $t('m.Training_List') }}</span>
        <div class="filter-row">
          <span>
            <el-button
              type="primary"
              size="small"
              @click="goCreateTraining"
              icon="el-icon-plus"
            >{{ $t("m.Create") }}</el-button>
          </span>
          <span>
            <vxe-input
              v-model="keyword"
              :placeholder="$t('m.Enter_keyword')"
              type="search"
              size="medium"
              @search-click="onKeywordChange"
              @keyup.enter.native="onKeywordChange"
            ></vxe-input>
          </span>
          <span>
            <el-select
              v-model="categoryId"
              @change="onCategoryChange"
              size="small"
              style="width: 180px"
            >
              <el-option :label="$t('m.All_Traning')" :value="0"></el-option>
              <el-option
                :label="category.name"
                :key="index"
                :value="category.id"
                v-for="(category, index) in categoryList"
              ></el-option>
            </el-select>
          </span>
          <span>
            <el-select
              v-model="traingAuth"
              @change="onAuthChange"
              size="small"
              style="width: 180px"
            >
              <el-option :label="$t('m.All_Traning')" :value="'All'"></el-option>
              <el-option :label="$t('m.Training_Public')" :value="'Public'"></el-option>
              <el-option :label="$t('m.Training_Private')" :value="'Private'"></el-option>
            </el-select>
          </span>
        </div>
      </div>
      <vxe-table
        :loading="loading"
        ref="xTable"
        :data="trainingList"
        auto-resize
        stripe
        align="center"
      >
        <vxe-table-column field="id" width="80" title="ID"></vxe-table-column>
        <vxe-table-column field="rank" width="80" :title="$t('m.Order_Number')"></vxe-table-column>
        <vxe-table-column field="title" min-width="150" :title="$t('m.Title')" show-overflow></vxe-table-column>
        <vxe-table-column :title="$t('m.Auth')" width="100">
          <template v-slot="{ row }">
            <el-tag :type="TRAINING_TYPE[row.auth]['color']" effect="dark">{{ row.auth }}</el-tag>
          </template>
        </vxe-table-column>
        <vxe-table-column :title="$t('m.Visible')" min-width="80">
          <template v-slot="{ row }">
            <el-switch
              v-model="row.status"
              :disabled="!isMainAdminRole && userInfo.username != row.author"
              @change="changeTrainingStatus(row.id, row.status, row.author)"
            ></el-switch>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="210" :title="$t('m.Info')">
          <template v-slot="{ row }">
            <p>Created Time: {{ row.gmtCreate | localtime }}</p>
            <p>Update Time: {{ row.gmtModified | localtime }}</p>
            <p>Creator: {{ row.author }}</p>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="150" :title="$t('m.Option')">
          <template v-slot="{ row }">
            <template v-if="isMainAdminRole || userInfo.username == row.author">
              <div style="margin-bottom:10px">
                <el-tooltip effect="dark" :content="$t('m.Edit')" placement="top">
                  <el-button
                    icon="el-icon-edit"
                    size="mini"
                    @click.native="goEdit(row.id)"
                    type="primary"
                  ></el-button>
                </el-tooltip>
                <el-tooltip
                  effect="dark"
                  :content="$t('m.View_Training_Problem_List')"
                  placement="top"
                >
                  <el-button
                    icon="el-icon-tickets"
                    size="mini"
                    @click.native="goTrainingProblemList(row.id)"
                    type="success"
                  ></el-button>
                </el-tooltip>
              </div>
            </template>
            <el-tooltip
              effect="dark"
              :content="$t('m.Delete')"
              placement="top"
              v-if="isSuperAdmin || userInfo.username == row.author"
            >
              <el-button
                icon="el-icon-delete"
                size="mini"
                @click.native="deleteTraining(row.id)"
                type="danger"
              ></el-button>
            </el-tooltip>
          </template>
        </vxe-table-column>
      </vxe-table>
      <div class="panel-options">
        <el-pagination
          class="page"
          layout="prev, pager, next"
          @current-change="currentChange"
          :page-size="pageSize"
          :total="total"
        ></el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script>
import api from "@/common/api";
import { TRAINING_TYPE } from "@/common/constants";
import { mapGetters } from "vuex";
import myMessage from "@/common/message";
import utils from "@/common/utils";

export default {
  name: "TrainingList",
  data() {
    return {
      pageSize: 10,
      total: 0,
      trainingList: [],
      categoryList: [],
      categoryId: 0,
      traingAuth: "All",
      keyword: "",
      loading: false,
      excludeAdmin: true,
      currentPage: 1,
      currentId: 1,
      downloadDialogVisible: false,
      TRAINING_TYPE: {},
      query: {
        cid: 0,
        keyword: "",
        auth: "All",
      },
    };
  },
  mounted() {
    let route = this.$route.query;
    if (route) {
      this.categoryId = parseInt(route.cid) || 0;
      this.traingAuth = route.auth || "All";
      this.keyword = route.keyword || "";
    }

    this.getTrainingList(this.currentPage);
    this.TRAINING_TYPE = Object.assign({}, TRAINING_TYPE);
    this.getTrainingCategoryList();
  },
  watch: {
    $route() {
      let refresh = this.$route.query.refresh == "true" ? true : false;
      if (refresh) {
        this.getTrainingList(1);
      }
    },
  },
  computed: {
    ...mapGetters(["isSuperAdmin", "isMainAdminRole", "userInfo"]),
  },
  methods: {
    // 切换页码回调
    currentChange(page) {
      this.currentPage = page;
      this.getTrainingList(page);
    },
    getTrainingList(page) {
      this.loading = true;
      api
        .admin_getTrainingList(
          page,
          this.pageSize,
          this.keyword,
          this.categoryId,
          this.traingAuth
        )
        .then(
          (res) => {
            this.loading = false;
            this.total = res.data.data.total;
            this.trainingList = res.data.data.records;
          },
          (res) => {
            this.loading = false;
          }
        );
    },
    goEdit(trainingId) {
      this.$router.push({
        name: "admin-edit-training",
        params: { trainingId },
      });
    },
    goTrainingProblemList(trainingId) {
      this.$router.push({
        name: "admin-training-problem-list",
        params: { trainingId },
      });
    },
    deleteTraining(trainingId) {
      this.$confirm(this.$i18n.t("m.Delete_Training_Tips"), "Tips", {
        confirmButtonText: this.$i18n.t("m.OK"),
        cancelButtonText: this.$i18n.t("m.Cancel"),
        type: "warning",
      }).then(() => {
        api.admin_deleteTraining(trainingId).then((res) => {
          myMessage.success(this.$i18n.t("m.Delete_successfully"));
          this.currentChange(1);
        });
      });
    },
    changeTrainingStatus(trainingId, status, author) {
      api.admin_changeTrainingStatus(trainingId, status, author).then((res) => {
        myMessage.success(this.$i18n.t("m.Update_Successfully"));
      });
    },
    goCreateTraining() {
      this.$router.push({ name: "admin-create-training" });
    },
    getTrainingCategoryList() {
      this.getCategoryListLoading = true;
      api.getTrainingCategoryList().then(
        (res) => {
          this.categoryList = res.data.data;
          this.getCategoryListLoading = false;
        },
        (err) => {
          this.getCategoryListLoading = false;
        }
      );
    },
    onKeywordChange() {
      this.query.keyword = this.keyword;
      this.currentPage = 1;
      this.filterByChange();
    },
    onCategoryChange(category) {
      this.query.cid = category;
      this.currentPage = 1;
      this.filterByChange();
    },
    onAuthChange(auth) {
      this.query.auth = auth;
      this.currentPage = 1;
      this.filterByChange();
    },
    filterByChange() {
      this.getTrainingList(this.currentPage);
      let query = Object.assign({}, this.query);
      this.$router.push({
        name: "admin-training-list",
        query: utils.filterEmptyValue(query),
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
