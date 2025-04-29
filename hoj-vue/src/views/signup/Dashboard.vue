<template>
  <div>
    <el-row :gutter="20">
      <el-col :xs="24" :md="10" :lg="12">
        <el-card class="admin-info">
          <div slot="header">
            <el-row :gutter="10" justify="space-between">
              <el-col :span="8" class="align-left">
                <avatar
                  :username="userInfo.username"
                  :inline="true"
                  :size="100"
                  color="#FFF"
                  :src="userInfo.avatar"
                ></avatar>
              </el-col>
              <el-col :span="16" class="align-right">
                <span class="panel-title admin-info-name">{{ userInfo.username }}</span>
                <p>
                  <el-tag effect="dark" size="small" type="warning">
                    {{
                    isMainAdminRole == true
                    ? $t('m.Super_Admin')
                    : isCoachAdmin == true
                    ? $t('m.Coach_Admin')
                    : $t('m.User')
                    }}
                  </el-tag>
                </p>
              </el-col>
              <p></p>
            </el-row>
            <p></p>
          </div>
          <p class="last-info-title home-title">{{ $t('m.Last_Login') }}</p>
          <div class="last-info">
            <el-form label-width="80px" class="last-info-body">
              <el-form-item label="Time:">
                <span>{{ session.gmtCreate | localtime }}</span>
              </el-form-item>
              <el-form-item label="IP:">
                <span>{{ session.ip }}</span>
              </el-form-item>
              <el-form-item label="OS:">
                <span>{{ os }}</span>
              </el-form-item>
              <el-form-item label="Browser:">
                <span>{{ browser }}</span>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import browserDetector from "browser-detect";
const InfoCard = () => import("@/components/admin/infoCard.vue");
import api from "@/common/api";
import Avatar from "vue-avatar";

export default {
  name: "signup-dashboard",
  components: {
    InfoCard,
    Avatar,
  },
  data() {
    return {
      session: {},
    };
  },
  mounted() {
    api.getSessions(this.userInfo.uid).then(
      (resp) => {
        this.session = resp.data.data;
      },
      () => {}
    );
  },
  computed: {
    ...mapGetters(["userInfo", "isCoachAdmin", "isMainAdminRole"]),
    browser() {
      let b = browserDetector(this.session.userAgent);
      if (b.name && b.version) {
        return b.name + " " + b.version;
      } else {
        return "Unknown";
      }
    },
    os() {
      let b = browserDetector(this.session.userAgent);
      return b.os ? b.os : "Unknown";
    },
  },
};
</script>

<style scoped>
.admin-info {
  margin-bottom: 20px;
  padding: 35px 35px 15px 35px;
  font-size: 26px;
}
.admin-info-name {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 10px;
  color: #409eff;
}
.admin-info .last-info-title {
  font-size: 16px;
}
.el-form-item {
  margin-bottom: 5px;
}
.align-left {
  text-align: left;
}
.align-right {
  text-align: right;
}
.info-container {
  display: flex;
  justify-content: flex-start;
  flex-wrap: wrap;
}
.info-container .info-item {
  flex: 1 0 auto;
  min-width: 200px;
  margin-bottom: 10px;
}
/deep/ .el-tag--dark {
  border-color: #fff;
}
/deep/.el-card__header {
  padding-bottom: 0;
}
@media screen and (min-width: 1150px) {
  /deep/ .vxe-table--body-wrapper {
    overflow-x: hidden !important;
  }
}
</style>