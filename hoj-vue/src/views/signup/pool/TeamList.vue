<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ $t('m.Coach_TeamPool') }}</span>
        <div class="filter-row">
          <span>
            <el-button
              type="primary"
              size="small"
              @click="addUserSign"
              icon="el-icon-plus"
            >{{ $t('m.Add') }}</el-button>
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

      <vxe-table :loading="loading" ref="xTable" :data="teamList" auto-resize stripe align="center">
        <vxe-table-column min-width="150" :title="$t('m.Team_Name')">
          <template v-slot="{ row }">{{ [row.cname, row.ename].filter(Boolean).join(' | ') }}</template>
        </vxe-table-column>

        <vxe-table-column :title="$t('m.Team_Names')" min-width="220">
          <template slot-scope="{ row }">
            <span>{{ [1, 2, 3].map(num => formatRealname(row[`realname${num}`], row[`englishname${num}`])).filter(Boolean) .join(' | ') }}</span>
          </template>
        </vxe-table-column>

        <vxe-table-column :title="$t('m.Team_Numbers')" min-width="50">
          <template slot-scope="{ row }">
            <span>{{ [row.username1, row.username2, row.username3].filter(Boolean).length }}</span>
          </template>
        </vxe-table-column>

        <vxe-table-column :title="$t('m.Instructor')" min-width="150">
          <template slot-scope="{ row }">
            <span>{{ row.instructor }}</span>
          </template>
        </vxe-table-column>

        <vxe-table-column :title="$t('m.IsGirl')" min-width="150">
          <template slot-scope="{ row }">
            <span>{{ row.type == 1 ? "Y" : "N" }}</span>
          </template>
        </vxe-table-column>

        <vxe-table-column field="option" :title="$t('m.Option')" min-width="150">
          <template v-slot="{ row }">
            <template v-if="row.username1 === userInfo.username || isCoachAdmin">
              <el-tooltip effect="dark" :content="$t('m.Edit')" placement="top">
                <el-button
                  icon="el-icon-edit"
                  size="mini"
                  @click.native="getTeamSign(row.id)"
                  type="primary"
                ></el-button>
              </el-tooltip>
              <el-tooltip effect="dark" :content="$t('m.Remove_TeamSign')" placement="top">
                <el-button
                  icon="el-icon-close"
                  size="mini"
                  @click.native="removeTeamSign(row.id)"
                  type="warning"
                ></el-button>
              </el-tooltip>
            </template>
          </template>
        </vxe-table-column>
      </vxe-table>

      <div class="panel-options">
        <el-pagination
          class="page"
          layout="prev, pager, next, sizes"
          @current-change="currentChange"
          :page-size.sync="limit"
          :current.sync="page"
          :total="total"
          @size-change="onPageSizeChange"
          :page-sizes="[10, 30, 50, 100]"
        ></el-pagination>
      </div>

      <el-dialog
        :title="$t('m.Edit_Team')"
        width="800px"
        :visible.sync="handleEditVisible"
        :close-on-click-modal="false"
      >
        <AddTeam :teamSign.sync="teamSign" :visible.sync="handleEditVisible"></AddTeam>
      </el-dialog>

      <el-dialog
        :title="$t('m.Add_Team')"
        width="800px"
        :visible.sync="handleAddVisible"
        :close-on-click-modal="false"
      >
        <AddTeam :visible.sync="handleAddVisible"></AddTeam>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import api from "@/common/api";
const AddTeam = () => import("@/components/signup/AddTeam");
import myMessage from "@/common/message";
import { mapGetters } from "vuex";

export default {
  name: "SignupTeamPool",
  components: {
    AddTeam,
  },
  data() {
    return {
      page: 1,
      limit: 15,
      total: 0,
      teamList: [],
      keyword: "",
      startYear: null, // 入学年份
      loading: false,
      handleEditVisible: false,
      handleAddVisible: false,
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
        participants: 1,
        maxParticipants: 3,
        instructor: null,
      },
    };
  },
  mounted() {
    this.keyword = this.$route.query.keyword || "";
    this.getTeamList(this.page);
  },
  methods: {
    // 切换页码回调
    currentChange(page) {
      this.page = page;
      this.getTeamList(page);
    },
    onPageSizeChange(limit) {
      this.limit = limit;
      this.getTeamList(this.page);
    },
    filterByKeyword() {
      this.currentChange(1);
    },
    filterByStatus() {
      if (this.startYear) {
        this.startYear = this.startYear.getFullYear().toString();
      }
      this.getTeamList(1);
    },

    getTeamList(page) {
      let params = {
        currentPage: page,
        limit: this.limit,
      };

      if (this.keyword != null && this.keyword != "") {
        params.keyword = this.keyword;
      }

      this.loading = true;
      api.getTeamSignList(params).then(
        (res) => {
          this.loading = false;
          this.total = res.data.data.total;
          this.teamList = res.data.data.records;

          this.teamList.forEach((user) => {
            user.teamConfig.forEach(({ username, realname, englishname }) => {
              ["1", "2", "3"].forEach((num) => {
                if (user[`username${num}`] === username) {
                  user[`realname${num}`] = realname;
                  user[`englishname${num}`] = englishname;
                }
              });
            });
          });
        },
        (res) => {
          this.loading = false;
        }
      );
    },

    getTeamSign(id) {
      this.handleEditVisible = true;
      api.getTeamSign(id).then(
        (res) => {
          this.teamSign = res.data.data;
        },
        (res) => {}
      );
    },
    removeTeamSign(id) {
      this.$confirm(this.$i18n.t("m.Remove_Team_Sign_Tips1"), "Tips", {
        type: "warning",
      }).then(
        () => {
          api
            .removeTeamSign(id)
            .then((res) => {
              myMessage.success("success");
              this.getTeamList(this.currentPage);
            })
            .catch(() => {});
        },
        () => {}
      );
    },
    addUserSign() {
      this.handleAddVisible = true;
    },
    formatRealname(realname, englishname) {
      if (realname && englishname) return `${realname} / ${englishname}`;
      return realname || "";
    },
  },
  watch: {
    handleEditVisible(newVal) {
      this.getTeamList(this.currentPage);
    },
    handleAddVisible(newVal) {
      this.getTeamList(this.currentPage);
    },
  },
  computed: {
    isContest() {
      return this.$route.params.contestID ?? false;
    },
    ...mapGetters(["userInfo", "isCoachAdmin"]),
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
