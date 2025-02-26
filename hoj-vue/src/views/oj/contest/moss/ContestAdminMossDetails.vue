<template>
  <el-row type="flex" justify="space-around">
    <el-col :span="24">
      <vxe-table border="inner" stripe auto-resize align="center" :data="mossIndexList">
        <vxe-table-column
          :title="mossResultInfo.username1 + ' (' + mossResultInfo.percent1 + '%)'"
          min-width="150"
        >
          <template v-slot="{ row, rowIndex }">
            <span>
              <a
                :href="'#' + rowIndex"
                :style="{ color: 'rgb(87, 163, 243)', fontSize: '18px', display: 'flex', flexDirection: 'column', alignItems: 'center' }"
              >
                {{ row.col1 }}
                <el-image :src="row.icon1" class="trophy"></el-image>
              </a>
            </span>
          </template>
        </vxe-table-column>
        <vxe-table-column
          :title="mossResultInfo.username2 + ' (' + mossResultInfo.percent2 + '%)'"
          min-width="150"
        >
          <template v-slot="{ row, rowIndex }">
            <span>
              <a
                :href="'#' + (rowIndex + mossIndexList.length) "
                :style="{ color: 'rgb(87, 163, 243)', fontSize: '18px', display: 'flex', flexDirection: 'column', alignItems: 'center' }"
              >
                {{ row.col2 }}
                <el-image :src="row.icon2"></el-image>
              </a>
            </span>
          </template>
        </vxe-table-column>
      </vxe-table>
    </el-col>
    <el-col :span="24">
      <el-col :span="12">
        <div class="frameset-col">
          <moss-code :code="mossResultInfo.code1"></moss-code>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="frameset-col">
          <moss-code :code="mossResultInfo.code2"></moss-code>
        </div>
      </el-col>
    </el-col>
  </el-row>
</template>

<script>
import api from "@/common/api";
import { addCodeBtn } from "@/common/codeblock";
import MossCode from "@/views/oj/contest/moss/MossCode.vue";

export default {
  name: "contestAdminMossDetails",
  components: {
    MossCode,
  },
  data() {
    return {
      isMobile: false,
      contestID: null,
      mossID: null,
      mossResultInfo: {},
      mossIndexList: [],
    };
  },
  mounted() {
    let screenWidth = window.screen.width;
    if (screenWidth < 768) {
      this.isMobile = true;
    }
    this.init();
    this.getContestMossResult();
  },
  methods: {
    init() {
      this.getContestMossResult();
      this.contestID = this.$route.params.contestID;
    },
    getContestMossResult() {
      api
        .getContestMossResult(
          this.$route.params.mossID,
          this.$route.params.contestID
        )
        .then((res) => {
          this.loadingTable = false;
          let data = res.data.data;

          const replace = (s) =>
            s?.replace(
              /http:\/\/moss\.stanford\.edu\/bitmaps\/(.+?)\.gif/g,
              "/api/public/img/$1.gif"
            );

          this.mossResultInfo = {
            ...data,
            code1: replace(data.code1),
            code2: replace(data.code2),
          };

          this.mossIndexList =
            data.indexList?.map((item) => ({
              ...item,
              icon1: replace(item.icon1),
              icon2: replace(item.icon2),
            })) || [];
        });
    },
    getUserACSubmit(username) {
      this.$router.push({
        path: "/contest/" + this.contestID + "/submissions",
        query: { username: username, status: 0 },
      });
    },
  },
  watch: {
    submission(newVal, oldVal) {
      if (newVal.code) {
        this.$nextTick((_) => {
          addCodeBtn();
        });
      }
    },
  },
};
</script>

<style scoped>
.el-row--flex {
  flex-wrap: wrap;
}
.el-col {
  padding-left: 5px !important;
  padding-right: 5px !important;
}

.trophy {
  margin-left: 10px;
  /* margin-right: -20px; */
}

.frameset-col {
  width: 100%;
  overflow-y: auto;
}
.hljs {
  padding: 0 !important;
}
.submission-detail pre {
  padding-left: 30px !important;
}
</style>
