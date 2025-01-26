<template>
  <div>
    <el-card>
      <div slot="header">
        <template v-if="isAdmin">
          <span class="panel-title home-title">{{ $t("m.Ranks_Admin") }}</span>
        </template>
        <template v-else>
          <ul class="nav-list">
            <span class="panel-title-acm">{{ $t("m.Ranks_Admin") }}</span>
          </ul>
        </template>
        <div class="filter-row" v-if="isAdmin">
          <span>
            <el-button
              type="primary"
              size="small"
              @click="goCreateStatistic"
              icon="el-icon-plus"
            >{{ $t("m.Create") }}</el-button>
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
        :loading="loading"
        ref="xTable"
        :data="statisticList"
        auto-resize
        stripe
        align="center"
      >
        <vxe-table-column field="scid" width="250" title="ID"></vxe-table-column>
        <vxe-table-column field="title" min-width="150" :title="$t('m.Title')" show-overflow></vxe-table-column>
        <vxe-table-column field="cid_percent" min-width="150" height="100" :show-overflow="false"></vxe-table-column>

        <vxe-table-column :title="$t('m.Visible')" min-width="80" v-if="isAdmin">
          <template v-slot="{ row }">
            <el-switch
              v-model="row.visible"
              :disabled="!isMainAdminRole && userInfo.username != row.author"
              @change="changeStatisticVisible(row.scid, row.visible, row.author)"
            ></el-switch>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="150" :title="$t('m.Option')">
          <template v-slot="{ row }">
            <div v-if="isAdmin">
              <template v-if="isMainAdminRole || userInfo.username == row.author">
                <el-tooltip
                  effect="dark"
                  :content="$t('m.Edit')"
                  placement="top"
                  v-if="isSuperAdmin || userInfo.username == row.author"
                >
                  <el-button
                    icon="el-icon-edit"
                    size="mini"
                    @click.native="goEditStatistic(row.scid)"
                    type="primary"
                  ></el-button>
                </el-tooltip>
                <el-tooltip
                  effect="dark"
                  :content="$t('m.Delete')"
                  placement="top"
                  v-if="isSuperAdmin || userInfo.username == row.author"
                >
                  <el-button
                    icon="el-icon-delete"
                    size="mini"
                    @click.native="deleteStatistic(row.scid)"
                    type="danger"
                  ></el-button>
                </el-tooltip>
              </template>
            </div>
            <div v-else>
              <el-tooltip effect="dark" :content="$t('m.Moss_View')" placement="top">
                <el-button
                  icon="el-icon-edit"
                  size="mini"
                  @click.native="goView(row.scid)"
                  type="primary"
                ></el-button>
              </el-tooltip>
            </div>
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
import { mapGetters } from "vuex";
import myMessage from "@/common/message";
export default {
  name: "HonorList",
  data() {
    return {
      pageSize: 10,
      total: 0,
      statisticList: [],
      keyword: "",
      loading: false,
      currentPage: 1,
      isAdmin: true,
    };
  },
  mounted() {
    this.route_name = this.$route.name;
    if (this.route_name == "admin-static-ranks-list") {
      this.isAdmin = true;
    } else {
      this.isAdmin = false;
    }
    this.getStatisticList(this.currentPage);
  },
  watch: {
    $route() {
      let refresh = this.$route.query.refresh == "true" ? true : false;
      if (refresh) {
        this.getStatisticList(1);
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
      this.getStatisticList(page);
    },
    getStatisticList(page) {
      this.loading = true;

      let funcName = this.isAdmin
        ? "admin_getStatisticList"
        : "getStatisticList";

      api[funcName](page, this.pageSize, this.keyword).then(
        (res) => {
          this.loading = false;
          this.total = res.data.data.total;
          this.statisticList = res.data.data.records;
          this.statisticList.forEach((statistic) => {
            const cidsParts = statistic.cids.split("+");
            const percentsParts = statistic.percents
              ? statistic.percents.split("-")
              : [];

            // 直接使用 map 并 join 合并，处理 percents 为空的情况
            statistic.cid_percent = cidsParts
              .map((cid, index) => {
                const percent = percentsParts[index];
                // 当 percent 存在时，显示 "cid-percent"，否则只显示 "cid"
                return percent !== undefined
                  ? `${cid} - ( ${percent}% )`
                  : `${cid}`;
              })
              .join("\n");
          });
        },
        () => {
          this.loading = false;
        }
      );
    },

    deleteStatistic(scid) {
      this.$confirm(this.$i18n.t("m.Delete_Statistic_Tips"), "Tips", {
        confirmButtonText: this.$i18n.t("m.OK"),
        cancelButtonText: this.$i18n.t("m.Cancel"),
        type: "warning",
      }).then(() => {
        api.admin_deleteStatistic(scid).then((res) => {
          myMessage.success(this.$i18n.t("m.Delete_successfully"));
          this.currentChange(1);
        });
      });
    },
    changeStatisticVisible(scid, visible, author) {
      api.admin_changeStatisticVisible(scid, visible, author).then((res) => {
        myMessage.success(this.$i18n.t("m.Update_Successfully"));
      });
    },
    filterByKeyword() {
      this.currentChange(1);
    },
    goCreateStatistic() {
      this.$router.push({ name: "admin-create-static-rank" });
    },
    goEditStatistic(scid) {
      this.$router.push({
        name: "admin-edit-static-rank",
        params: { scid: scid },
      });
    },
    statisticListChangeFilter() {
      this.currentPage = 1;
      this.getStatisticList();
    },
    goView(scid) {
      this.$router.push({
        name: "Static Rank",
        params: { cids: scid },
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
.panel-title-acm {
  font-size: 2em;
  font-weight: 500;
  line-height: 30px;
}
</style>
