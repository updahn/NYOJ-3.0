<template>
  <el-card>
    <div slot="header">
      <span class="title">{{OJ}} {{$t('m.Account_Config')}}</span>
    </div>
    <el-row v-if="OJ == 'SCPC'" :gutter="15">
      <el-col :xs="24" :md="10">
        <el-input v-model="superAccount" size="small" @blur="superAccountChange()" clearable>
          <template slot="prepend">{{$t('m.Super_Account')}}</template>
        </el-input>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-input v-model="superPassword" size="small" @blur="superPasswordChange()" show-password>
          <template slot="prepend">{{$t('m.Password')}}</template>
        </el-input>
      </el-col>
    </el-row>
    <el-row v-for="(value,index) in usernameListTmp" :key="index" :gutter="15" class="mg-top">
      <el-col :xs="24" :md="hasAlive ? 8: 10">
        <el-input v-model="usernameListTmp[index]" size="small" clearable>
          <template slot="prepend">{{$t('m.Account')}}{{index+1}}</template>
        </el-input>
      </el-col>
      <el-col v-if="OJ !== 'MOSS'" :xs="24" :md="hasAlive ? 8: 10">
        <el-input v-model="passwordListTmp[index]" size="small" :show-password="true">
          <template slot="prepend">{{ $t('m.Password') }} {{ index + 1 }}</template>
        </el-input>
      </el-col>
      <el-col v-if="hasAlive" :xs="24" :md="4" style="margin-top:5px">
        <div class="switch-container">
          <el-switch v-model="aliveListTmp[index]" :active-text="$t('m.Alive')"></el-switch>
          <el-popover placement="right" trigger="hover">
            <template #reference>
              <i class="el-icon-question" style="margin-left: 2px;"></i>
            </template>
            <p>{{ $t('m.Alive_Tips') }}</p>
          </el-popover>
        </div>
      </el-col>
      <el-col v-if="isCourse && aliveListTmp[index]" :xs="24" :md="20" style="margin-top:5px">
        <el-input v-model="titleListTmp[index]" size="small">
          <template slot="prepend">{{ $t('m.Course_Title') }} {{ index + 1 }}</template>
        </el-input>
      </el-col>
      <el-col v-if="isCourse && aliveListTmp[index]" :xs="24" :md="20" style="margin-top:5px">
        <el-input v-model="linkListTmp[index]" size="small">
          <template slot="prepend">{{ $t('m.Course_Link') }} {{ index + 1 }}</template>
        </el-input>
      </el-col>
      <el-col :xs="24" :md="4" class="t-center">
        <el-button
          type="danger"
          icon="el-icon-delete"
          circle
          size="small"
          @click="deleteAccount(index)"
        ></el-button>
      </el-col>
    </el-row>
    <el-button
      type="warning"
      round
      size="small"
      class="mg-top"
      @click="addAccount"
      icon="el-icon-plus"
    >{{ $t('m.Add_Account') }}</el-button>
    <el-button
      type="primary"
      :loading="loading"
      style="margin-top:15px"
      @click.native="saveSwitchConfig"
      size="small"
    >
      <i class="fa fa-save">{{ $t('m.Save') }}</i>
    </el-button>
  </el-card>
</template>

<script>
export default {
  props: {
    usernameList: {
      default: [],
      type: Array,
    },
    passwordList: {
      default: [],
      type: Array,
    },
    aliveList: {
      default: [],
      type: Array,
    },
    titleList: {
      default: [],
      type: Array,
    },
    linkList: {
      default: [],
      type: Array,
    },
    OJ: {
      type: String,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    superAccount: {
      type: String,
    },
    superPassword: {
      type: String,
    },
  },
  data() {
    return {
      usernameListTmp: [],
      passwordListTmp: [],
      aliveListTmp: [],
      titleListTmp: [],
      linkListTmp: [],
    };
  },
  mounted() {
    this.usernameListTmp = this.usernameList;
    this.passwordListTmp = this.passwordList;
    this.aliveListTmp = this.aliveList;
    this.titleListTmp = this.titleList;
    this.linkListTmp = this.linkList;
  },
  methods: {
    deleteAccount(index) {
      this.usernameListTmp.splice(index, 1);
      this.$emit("update:usernameList", this.usernameListTmp);
      this.passwordListTmp.splice(index, 1);
      this.$emit("update:passwordList", this.passwordListTmp);
      if (this.hasAlive) {
        this.aliveListTmp.splice(index, 1);
        this.$emit("update:aliveList", this.aliveListTmp);
        this.titleListTmp.splice(index, 1);
        this.$emit("update:titleList", this.titleListTmp);
        this.linkListTmp.splice(index, 1);
        this.$emit("update:linkList", this.linkListTmp);
      }
    },
    addAccount() {
      this.usernameListTmp.push("");
      this.$emit("update:usernameList", this.usernameListTmp);
      this.passwordListTmp.push("");
      this.$emit("update:passwordList", this.passwordListTmp);

      if (this.hasAlive) {
        this.aliveListTmp.push(false);
        this.$emit("update:aliveList", this.aliveListTmp);
        this.titleListTmp.push("");
        this.$emit("update:titleList", this.titleListTmp);
        this.linkListTmp.push("");
        this.$emit("update:linkList", this.linkListTmp);
      }
    },
    saveSwitchConfig() {
      this.$emit("saveSwitchConfig");
    },
    superAccountChange() {
      this.$emit("update:superAccount", this.superAccount);
    },
    superPasswordChange() {
      this.$emit("update:superPassword", this.superPassword);
    },
  },
  watch: {
    usernameList(val) {
      if (this.usernameListTmp !== val) {
        this.usernameListTmp = val;
      }
    },
    passwordList(val) {
      if (this.passwordListTmp !== val) {
        this.passwordListTmp = val;
      }
    },
    aliveList(val) {
      if (this.aliveListTmp !== val) {
        this.aliveListTmp = val;
      }
    },
    titleList(val) {
      if (this.titleListTmp !== val) {
        this.titleListTmp = val;
      }
    },
    linkList(val) {
      if (this.linkListTmp !== val) {
        this.linkListTmp = val;
      }
    },
    superAccount(val) {
      if (this.superAccount !== val) {
        this.superAccount = val;
      }
    },
    superPassword(val) {
      if (this.superPassword !== val) {
        this.superPassword = val;
      }
    },
  },
  computed: {
    hasAlive() {
      return ["VJ", "Nowcoder", "Acwing", "Codeforces"].includes(this.OJ);
    },
    isCourse() {
      return ["Nowcoder", "Acwing"].includes(this.OJ);
    },
  },
};
</script>

<style scoped>
.title {
  font-size: 18px;
  font-weight: bolder;
}
.mg-top {
  margin-top: 15px;
}
@media screen and (max-width: 992px) {
  .t-center {
    text-align: center;
    margin-top: 10px;
  }
}
.switch-container {
  display: flex;
  align-items: center;
}
</style>