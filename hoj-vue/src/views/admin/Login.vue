<template>
  <div>
    <vue-particles
      color="#dedede"
      :particleOpacity="0.7"
      :particlesNumber="80"
      shapeType="circle"
      :particleSize="4"
      linesColor="#dedede"
      :linesWidth="1"
      :lineLinked="true"
      :lineOpacity="0.4"
      :linesDistance="150"
      :moveSpeed="0.5"
      :hoverEffect="true"
      hoverMode="grab"
      :clickEffect="true"
      clickMode="push"
      style="position: absolute; top: 0; left: 0; z-index: 1;"
    ></vue-particles>
    <div class="form" style="position: relative; z-index: 10;">
      <el-form
        :model="ruleForm2"
        :rules="isComplete ? rules2 : rules"
        ref="ruleForm2"
        label-position="left"
        label-width="0px"
        class="demo-ruleForm login-container"
      >
        <h1 class="title">{{ isAdmin ? $t('m.Welcome_to_Login_Admin') : $t('m.Welcome_to_Signup')}}</h1>
        <el-form-item prop="username">
          <el-input
            type="text"
            v-model="ruleForm2.username"
            prefix-icon="el-icon-user-solid"
            auto-complete="off"
            :placeholder="$t('m.Please_enter_username')"
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            type="password"
            prefix-icon="el-icon-lock"
            show-password
            v-model="ruleForm2.password"
            auto-complete="off"
            :placeholder="$t('m.Please_enter_password')"
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>

        <el-form-item v-if="isComplete" prop="email">
          <el-input
            v-model="ruleForm2.email"
            prefix-icon="el-icon-message"
            :placeholder="$t('m.Register_Email')"
            @keyup.enter.native="handleLogin"
          >
            <el-button
              slot="append"
              icon="el-icon-message"
              type="primary"
              @click.native="sendRegisterEmail"
              :loading="btnEmailLoading"
            >
              <span v-show="btnEmailLoading">{{ countdownNum }}</span>
            </el-button>
          </el-input>
        </el-form-item>
        <el-form-item v-if="isComplete" prop="code">
          <el-input
            v-model="ruleForm2.code"
            prefix-icon="el-icon-s-check"
            :placeholder="$t('m.Register_Email_Captcha')"
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item v-if="isComplete" prop="realname">
          <el-input
            v-model="ruleForm2.realname"
            prefix-icon="fa fa-drivers-license"
            :placeholder="$t('m.Register_Realname')"
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item v-if="isComplete" prop="school">
          <el-select
            v-model="ruleForm2.school"
            filterable
            remote
            reserve-keyword
            :placeholder="$t('m.Enter_Your_School')"
            :remote-method="fetchStates"
            :loading="loading"
            style="width: 100%;"
            :dropdown-style="{ zIndex: 9999 }"
            prefix-icon="el-icon-school"
            @keyup.enter.native="handleLogin"
          >
            <template #prefix>
              <span style="padding-left: 5px;">
                <i class="el-icon-school"></i>
              </span>
            </template>
            <el-option
              v-for="state in filteredStates"
              :key="state.value"
              :label="state.label"
              :value="state.value"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item style="width: 100%">
          <el-button
            type="primary"
            style="width: 100%"
            @click.native.prevent="handleLogin"
            :loading="logining"
          >{{ $t('m.Login') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import api from "@/common/api";
import mMessage from "@/common/message";
import { mapGetters, mapActions } from "vuex";

export default {
  data() {
    const CheckEmailNotExist = (rule, value, callback) => {
      api.checkUsernameOrEmail(undefined, value).then(
        (res) => {
          if (res.data.data.email === true) {
            callback(new Error(this.$i18n.t("m.The_email_already_exists")));
          } else {
            callback();
          }
        },
        (_) => callback()
      );
    };

    return {
      logining: false,
      ruleForm2: {
        username: null,
        password: null,
        email: null,
        code: null,
        realname: null,
        school: null,
      },
      rules: {
        username: [
          {
            required: true,
            trigger: "blur",
            message: this.$i18n.t("m.Username_Check_Required"),
          },
        ],
        password: [
          {
            required: true,
            trigger: "blur",
            message: this.$i18n.t("m.Password_Check_Required"),
          },
        ],
      },
      rules2: {
        username: [
          {
            required: true,
            trigger: "blur",
            message: this.$i18n.t("m.Username_Check_Required"),
          },
        ],
        password: [
          {
            required: true,
            trigger: "blur",
            message: this.$i18n.t("m.Password_Check_Required"),
          },
        ],
        email: [
          {
            required: true,
            message: this.$i18n.t("m.Email_Check_Required"),
            trigger: "blur",
          },
          {
            type: "email",
            message: this.$i18n.t("m.Email_Check_Format"),
            trigger: "blur",
          },
          {
            validator: CheckEmailNotExist,
            message: this.$i18n.t("m.The_email_already_exists"),
            trigger: "blur",
          },
        ],
        code: [
          {
            required: true,
            message: this.$i18n.t("m.Code_Check_Required"),
            trigger: "blur",
          },
          {
            min: 6,
            max: 6,
            message: this.$i18n.t("m.Code_Check_Length"),
            trigger: "blur",
          },
        ],
        realname: [
          {
            required: true,
            message: this.$i18n.t("m.Realname_Check_Required"),
            trigger: "blur",
          },
        ],
        school: [
          {
            required: true,
            message: this.$i18n.t("m.School_Check_Required"),
            trigger: "blur",
          },
        ],
      },
      checked: true,
      isAdmin: false,
      btnEmailLoading: false,
      countdownNum: null,
      filteredStates: [],
      states: [],
      loading: false,
    };
  },
  created() {
    this.getSchoolList();
    if (this.time != 60 && this.time != 0) {
      this.btnEmailLoading = true;
      this.countDown();
    }
  },
  mounted() {
    const routePath = this.$route.path;
    if (routePath.startsWith("/admin")) {
      this.isAdmin = true;
    } else {
      this.isAdmin = false;
    }
  },
  methods: {
    ...mapActions(["startTimeOut", "changeRegisterTimeOut"]),
    handleLogin(ev) {
      const loginApi = this.isAdmin ? "admin_login" : "signup_login";
      const successMessage = this.$i18n.t(
        this.isAdmin ? "m.Admin_Login_Success" : "m.Signup_Login_Success"
      );
      const routeName = this.isAdmin ? "admin-dashboard" : "signup-dashboard";

      this.$refs.ruleForm2.validate((valid) => {
        if (valid) {
          this.logining = true;

          let ruleForm2 = Object.assign({}, this.ruleForm2);
          Object.keys(ruleForm2).forEach((key) => {
            if (ruleForm2[key] === null) {
              delete ruleForm2[key];
            }
          });

          api[loginApi](ruleForm2).then(
            (res) => {
              this.logining = false;
              const jwt = res.headers["authorization"];
              this.$store.commit("changeUserToken", jwt);
              this.$store.dispatch("setUserInfo", res.data.data);
              mMessage.success(successMessage);
              this.$router.push({ name: routeName });
            },
            (_) => {
              this.logining = false;

              // 禁止登录
              if (!this.isAdmin && _.data.status === 403) {
                // 切换到添加学校和邮箱登录界面
                this.$router.push({
                  path: "/signup/login",
                  query: { complete: true },
                });
              }
            }
          );
        } else {
          const errorMessage = this.$i18n.t(
            this.isComplete
              ? "m.Please_check_your_info"
              : "m.Please_check_your_username_or_password"
          );
          mMessage.error(errorMessage);
        }
      });
    },
    countDown() {
      let i = this.time;
      if (i == 0) {
        this.btnEmailLoading = false;
        return;
      }
      this.countdownNum = i;
      setTimeout(() => {
        this.countDown();
      }, 1000);
    },
    sendRegisterEmail() {
      var emailReg =
        /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
      if (!emailReg.test(this.ruleForm2.email)) {
        mMessage.error(this.$i18n.t("m.Email_Check_Format"));
        return;
      }
      this.btnEmailLoading = true;
      this.countdownNum = "Waiting...";
      if (this.ruleForm2.email) {
        mMessage.info(this.$i18n.t("m.The_system_is_processing"));
        api.getRegisterEmail(this.ruleForm2.email).then(
          (res) => {
            if (res.data.msg != null) {
              mMessage.message(
                "success",
                this.$i18n.t("m.Register_Send_Email_Msg"),
                5000
              );
              this.$notify.success({
                title: this.$i18n.t("m.Success"),
                message: this.$i18n.t("m.Register_Send_Email_Msg"),
                duration: 5000,
                offset: 50,
              });
              this.countDown();
              this.startTimeOut({ name: "registerTimeOut" });
            }
          },
          (res) => {
            this.btnEmailLoading = false;
            this.countdownNum = null;
          }
        );
      }
    },
    getSchoolList() {
      api.getSchoolList().then(
        (res) => {
          this.states = res.data.data;
          this.filteredStates = this.states.map((state) => ({
            label: state.name,
            value: state.name,
          }));
        },
        (_) => {
          this.states = [];
          this.filteredStates = [];
        }
      );
    },
    fetchStates(query) {
      this.loading = true;
      setTimeout(() => {
        const filterQuery = query.trim().toLowerCase();
        this.filteredStates = this.states
          .filter((state) =>
            filterQuery ? state.name.toLowerCase().includes(filterQuery) : true
          )
          .map((state) => ({ label: state.name, value: state.name }));
        this.loading = false;
      }, 200);
    },
  },
  computed: {
    ...mapGetters(["userInfo", "registerTimeOut"]),
    time: {
      get() {
        return this.registerTimeOut;
      },
      set(value) {
        this.changeRegisterTimeOut({ time: value });
      },
    },
    isComplete() {
      // 是否需要补全信息
      return this.$route.query.complete === "true";
    },
  },
};
</script>

<style scoped>
.login-container {
  -webkit-border-radius: 5px;
  border-radius: 5px;
  -moz-border-radius: 5px;
  background-clip: padding-box;
  margin: 180px auto;
  width: 350px;
  padding: 35px 35px 15px 35px;
  background: #fff;
  border: 1px solid #eaeaea;
  box-shadow: 0 0 25px #cac6c6;
}
.login-container .title {
  margin: 0px auto 40px auto;
  text-align: center;
  color: #1e9fff;
  font-size: 25px;
  font-weight: bold;
}
.login-container .remember {
  margin: 0px 0px 35px 0px;
}
.form {
  position: relative;
  z-index: 9999;
}
</style>
