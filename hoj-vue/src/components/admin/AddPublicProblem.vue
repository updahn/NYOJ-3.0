<template>
  <div style="text-align:center">
    <div style="margin-bottom:10px" v-if="contest.type != undefined">
      <span class="tips">
        {{
        contest.type == CONTEST_TYPE.ACM
        ? $t('m.ACM_Contest_Add_From_Public_Problem_Tips')
        : contest.type == CONTEST_TYPE.OI
        ? $t('m.OI_Contest_Add_From_Public_Problem_Tips')
        : $t('m.EXAM_Contest_Add_From_Public_Problem_Tips')
        }}
      </span>
    </div>
    <vxe-input
      v-model="keyword"
      :placeholder="$t('m.Enter_keyword')"
      type="search"
      size="medium"
      @search-click="filterByKeyword"
      @keyup.enter.native="filterByKeyword"
      style="margin-bottom:10px"
    ></vxe-input>
    <vxe-table :data="problems" :loading="loading" auto-resize stripe align="center">
      <vxe-table-column title="ID" min-width="100" field="problemId"></vxe-table-column>
      <vxe-table-column min-width="150" :title="$t('m.Title')" field="title"></vxe-table-column>
      <vxe-table-column min-width="150" :title="$t('m.Type')" field="type">
        <template v-slot="{ row }">
          <el-tag effect="dark" color="#1559A1" v-if="row.type == 0">{{ 'ACM' }}</el-tag>
          <el-tag effect="dark" color="#19be6b" v-if="row.type == 1">{{ 'OI' }}</el-tag>
          <el-tag effect="dark" color="#66B1FF" v-if="row.type == 2">{{ $t('m.Selection') }}</el-tag>
          <el-tag effect="dark" color="#EEAC3C" v-if="row.type == 3">{{ $t('m.Filling') }}</el-tag>
          <el-tag effect="dark" color="#881E1F" v-if="row.type == 4">{{ $t('m.Decide') }}</el-tag>
        </template>
      </vxe-table-column>
      <vxe-table-column :title="$t('m.Option')" align="center" min-width="100">
        <template v-slot="{ row }">
          <el-tooltip effect="dark" :content="$t('m.Add')" placement="top">
            <el-button
              icon="el-icon-plus"
              size="mini"
              @click.native="handleAddProblem(row.id, row.problemId, row.problemDescriptionList)"
              type="primary"
            ></el-button>
          </el-tooltip>
        </template>
      </vxe-table-column>
    </vxe-table>

    <el-pagination
      class="page"
      layout="prev, pager, next"
      @current-change="getPublicProblem"
      :page-size="limit"
      :current-page.sync="page"
      :total="total"
    ></el-pagination>

    <el-dialog append-to-body :visible.sync="handleVisible" z-index="9000">
      <el-form>
        <el-form-item
          v-if="contestID"
          :label="$t('m.Enter_The_Problem_Display_ID_in_the_Contest')"
          required
        >
          <el-input v-model="displayId" size="small"></el-input>
        </el-form-item>

        <el-form-item :label="$t('m.Enter_The_Problem_Description_ID')" required>
          <el-tag
            v-for="problemDescription in problemDescriptionList"
            size="medium"
            class="filter-item"
            :effect="peid == problemDescription.id ? 'dark' : 'plain'"
            :key="problemDescription.id"
            @click="filterByPeid(problemDescription.id)"
          >{{ problemDescription.id + ": " + problemDescription.title }}</el-tag>
        </el-form-item>

        <el-form-item style="text-align: center">
          <el-button type="primary" @click="addProblem" :loading="handleLoading">{{ $t("m.OK") }}</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>
<script>
import api from "@/common/api";
import myMessage from "@/common/message";
import { CONTEST_TYPE } from "@/common/constants";

export default {
  name: "add-problem-from-public",
  props: ["contestID", "trainingID"],
  data() {
    return {
      page: 1,
      limit: 10,
      total: 0,
      loading: false,
      problems: [],
      contest: {},
      keyword: "",
      handleVisible: false,
      handleLoading: false,
      problemDescriptionList: [],
      pid: null,
      peid: null,
      displayId: null,
      problemId: null,
      CONTEST_TYPE: {},
    };
  },
  mounted() {
    this.CONTEST_TYPE = Object.assign({}, CONTEST_TYPE);
    if (this.contestID) {
      api
        .admin_getContest(this.contestID)
        .then((res) => {
          this.contest = res.data.data;
          this.getPublicProblem(1);
        })
        .catch(() => {});
    } else if (this.trainingID) {
      this.getPublicProblem(1);
    }
  },
  methods: {
    getPublicProblem(page) {
      this.loading = true;
      let params = {
        keyword: this.keyword,
        currentPage: page,
        limit: this.limit,
        problemType: this.contest.type,
        cid: this.contest.id,
        tid: this.trainingID,
      };

      let func = null;
      if (this.contestID) {
        func = "admin_getContestProblemList";
      } else if (this.trainingID) {
        func = "admin_getTrainingProblemList";
      }

      api[func](params)
        .then((res) => {
          this.loading = false;
          this.total = res.data.data.problemList.total;
          this.problems = res.data.data.problemList.records;
        })
        .catch(() => {
          this.loading = false;
        });
    },

    handleAddProblem(id, problemId, problemDescriptionList) {
      this.pid = id;
      this.problemId = problemId;
      this.problemDescriptionList = problemDescriptionList;
      this.handleVisible = true;
      this.peid = null;
      this.displayId = null;
    },

    addProblem() {
      let data = {
        pid: this.pid,
        peid: this.peid,
        displayId: this.displayId || this.problemId,
        cid: this.contestID,
        tid: this.trainingID,
      };

      const func = this.contestID
        ? "admin_addContestProblemFromPublic"
        : "admin_addTrainingProblemFromPublic";

      api[func](data).then(
        (res) => {
          myMessage.success(this.$i18n.t("m.Add_Successfully"));
          this.getPublicProblem(this.page);
          this.handleVisible = false;
          this.handleLoading = false;
          this.$emit("on-change");
        },
        () => {
          this.handleVisible = false;
          this.handleLoading = false;
        }
      );
    },
    filterByPeid(peid) {
      this.peid = peid;
    },
    filterByKeyword() {
      this.page = 1;
      this.getPublicProblem(this.page);
    },
  },
};
</script>
<style scoped>
.page {
  margin-top: 20px;
  text-align: right;
}
.tips {
  color: red;
  font-weight: bolder;
  font-size: 1rem;
}
.filter-item {
  margin-right: 1em;
  margin-top: 0.5em;
  font-size: 13px;
}
.filter-item:hover {
  cursor: pointer;
}
</style>
