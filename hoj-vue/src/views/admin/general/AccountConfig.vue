<template>
  <div>
    <el-card class="card-top">
      <div slot="header">
        <span class="panel-title home-title">{{ $t('m.Account_Config') }}</span>
      </div>
      <el-row :gutter="20">
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.hduUsernameList"
            :passwordList.sync="switchConfig.hduPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="HDU"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.cfUsernameList"
            :passwordList.sync="switchConfig.cfPasswordList"
            :aliveList.sync="switchConfig.cfAliveList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="Codeforces"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.pojUsernameList"
            :passwordList.sync="switchConfig.pojPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="POJ"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.atcoderUsernameList"
            :passwordList.sync="switchConfig.atcoderPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="Atcoder"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.spojUsernameList"
            :passwordList.sync="switchConfig.spojPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="SPOJ"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.libreojUsernameList"
            :passwordList.sync="switchConfig.libreojPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="LibreOJ"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.scpcUsernameList"
            :passwordList.sync="switchConfig.scpcPasswordList"
            :superAccount.sync="switchConfig.scpcSuperAdminAccount"
            :superPassword.sync="switchConfig.scpcSuperAdminPassword"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="SCPC"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.qojUsernameList"
            :passwordList.sync="switchConfig.qojPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="QOJ"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.nswojUsernameList"
            :passwordList.sync="switchConfig.nswojPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="NSWOJ"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.newojUsernameList"
            :passwordList.sync="switchConfig.newojPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="NEWOJ"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.vjUsernameList"
            :passwordList.sync="switchConfig.vjPasswordList"
            :aliveList.sync="switchConfig.vjAliveList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="VJ"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.dotcppUsernameList"
            :passwordList.sync="switchConfig.dotcppPasswordList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="DOTCPP"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.nowcoderUsernameList"
            :passwordList.sync="switchConfig.nowcoderPasswordList"
            :aliveList.sync="switchConfig.nowcoderAliveList"
            :titleList.sync="switchConfig.nowcoderTitleList"
            :linkList.sync="switchConfig.nowcoderLinkList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="Nowcoder"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.acwingUsernameList"
            :passwordList.sync="switchConfig.acwingPasswordList"
            :aliveList.sync="switchConfig.acwingAliveList"
            :titleList.sync="switchConfig.acwingTitleList"
            :linkList.sync="switchConfig.acwingLinkList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="Acwing"
          ></RemoteJudgeAccount>
        </el-col>
        <el-col :xs="24" :md="12" style="margin-top: 15px;">
          <RemoteJudgeAccount
            :usernameList.sync="switchConfig.mossUsernameList"
            :loading.sync="loading"
            @saveSwitchConfig="saveSwitchConfig"
            OJ="MOSS"
          ></RemoteJudgeAccount>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>
<script>
import api from "@/common/api";
import myMessage from "@/common/message";
const RemoteJudgeAccount = () =>
  import("@/components/admin/RemoteJudgeAccount.vue");
export default {
  name: "SystemConfig",
  components: {
    RemoteJudgeAccount,
  },
  data() {
    return {
      loading: false,
      switchConfig: {},
      labelPosition: "left",
    };
  },
  created() {
    let screenWidth = window.screen.width;
    if (screenWidth < 500) {
      this.labelPosition = "top";
    }
  },
  mounted() {
    api.admin_getSwitchConfig().then((res) => {
      this.switchConfig = res.data.data;
    });
  },
  methods: {
    saveSwitchConfig() {
      this.loading = true;
      api.admin_saveSwitchConfig(this.switchConfig).then(
        (res) => {
          myMessage.success(this.$i18n.t("m.Update_Successfully"));
          this.loading = false;
        },
        () => {
          this.loading = false;
        }
      );
    },
  },
};
</script>
<style scoped>
.switch-item-title {
  font-size: 18px;
  font-weight: bolder;
}
@media screen and (max-width: 992px) {
  .card-top {
    margin-top: 15px;
  }
}
</style>
