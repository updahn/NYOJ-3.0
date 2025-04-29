<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ $t("m.Change_Password") }}</span>
      </div>
      <el-form
        class="setting-content"
        ref="formPassword"
        :model="formPassword"
        :rules="rulePassword"
      >
        <el-row :gutter="20">
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Old_Password')" prop="oldPassword">
              <el-input v-model="formPassword.oldPassword" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :offset="8" :xs="24"></el-col>
          <el-col :offset="8" :xs="24"></el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.New_Password')" prop="newPassword">
              <el-input v-model="formPassword.newPassword" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :offset="8" :xs="24"></el-col>
          <el-col :offset="8" :xs="24"></el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Confirm_New_Password')" prop="againPassword">
              <el-input v-model="formPassword.againPassword" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :offset="8" :xs="24"></el-col>
          <el-col :offset="8" :xs="24"></el-col>
        </el-row>
      </el-form>
      <el-button
        type="primary"
        size="small"
        :loading="loading.btnPassword"
        :disabled="disabled.btnPassword"
        @click="verify.passwordSuccess = false"
      >{{ $t('m.Update_Password') }}</el-button>
      <Vcode :show="verify.passwordSuccess === false" @success="changePassword" />
      <el-alert
        :title="$t('m.Slide_Verify_Success')"
        type="success"
        v-show="verify.passwordSuccess"
        :center="true"
        :closable="false"
        show-icon
      ></el-alert>
    </el-card>

    <el-card style="margin-top: 15px">
      <div slot="header">
        <span class="panel-title home-title">{{ $t("m.Change_Email") }}</span>
      </div>
      <el-form class="setting-content" ref="formEmail" :model="formEmail" :rules="ruleEmail">
        <el-row :gutter="20">
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Current_Password')" prop="password">
              <el-input v-model="formEmail.password" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :offset="8" :xs="24"></el-col>
          <el-col :offset="8" :xs="24"></el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Old_Email')">
              <el-input v-model="formEmail.oldEmail" disabled />
            </el-form-item>
          </el-col>
          <el-col :offset="8" :xs="24"></el-col>
          <el-col :offset="8" :xs="24"></el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.New_Email')" prop="newEmail">
              <el-input v-model="formEmail.newEmail">
                <el-button
                  slot="append"
                  @click="getChangeEmailCode"
                  :loading="loading.btnSendEmail"
                  icon="el-icon-message"
                >{{$t('m.Get_Captcha')}}</el-button>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :offset="8" :xs="24"></el-col>
          <el-col :offset="8" :xs="24"></el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Captcha')" prop="code">
              <el-input v-model="formEmail.code" />
            </el-form-item>
          </el-col>
          <el-col :offset="8" :xs="24"></el-col>
          <el-col :offset="8" :xs="24"></el-col>
        </el-row>
      </el-form>
      <el-button
        type="primary"
        size="small"
        :disabled="disabled.btnEmail"
        @click="verify.emailSuccess = false"
      >{{ $t('m.Update_Email') }}</el-button>
      <Vcode :show="verify.emailSuccess === false" @success="changeEmail" />
      <el-alert
        :title="$t('m.Slide_Verify_Success')"
        type="success"
        :description="verify.emailMsg"
        v-show="verify.emailSuccess"
        :center="true"
        :closable="false"
        show-icon
      ></el-alert>
    </el-card>
  </div>
</template>

<script>
import api from "@/common/api";
import myMessage from "@/common/message";
import Vcode from "vue-puzzle-vcode";

export default {
  name: "UserSetting",
  components: {
    Vcode,
  },
  data() {
    const oldPasswordCheck = [
      {
        required: true,
        trigger: "blur",
        message: this.$i18n.t("m.The_current_password_cannot_be_empty"),
      },
      {
        trigger: "blur",
        min: 6,
        max: 20,
        message: this.$i18n.t("m.Password_Check_Between"),
      },
    ];
    const CheckAgainPassword = (rule, value, callback) => {
      if (value !== this.formPassword.newPassword) {
        callback(new Error(this.$i18n.t("m.Password_does_not_match")));
      }
      callback();
    };
    const CheckNewPassword = (rule, value, callback) => {
      if (this.formPassword.oldPassword !== "") {
        if (this.formPassword.oldPassword === this.formPassword.newPassword) {
          callback(
            new Error(this.$i18n.t("m.The_new_password_does_not_change"))
          );
        } else {
          // 对第二个密码框再次验证
          this.$refs.formPassword.validateField("again_password");
        }
      }
      callback();
    };
    const CheckEmail = (rule, value, callback) => {
      if (this.formEmail.oldEmail !== "") {
        if (this.formEmail.oldEmail === this.formEmail.newEmail) {
          callback(new Error(this.$i18n.t("m.The_new_email_does_not_change")));
        }
      }
      callback();
    };
    return {
      loading: {
        btnPassword: false,
        btnEmail: false,
        btnSendEmail: false,
      },
      disabled: {
        btnPassword: false,
        btnEmail: false,
      },
      verify: {
        passwordSuccess: null,
        emailSuccess: null,
        emailMsg: "",
      },
      visible: {
        passwordAlert: {
          type: "success",
          show: false,
          title: "",
          description: "",
        },
        emailAlert: {
          type: "success",
          show: false,
          title: "",
          description: "",
        },
      },
      formPassword: {
        oldPassword: "",
        newPassword: "",
        againPassword: "",
      },
      formEmail: {
        password: "",
        oldEmail: "",
        newEmail: "",
      },
      rulePassword: {
        oldPassword: oldPasswordCheck,
        newPassword: [
          {
            required: true,
            trigger: "blur",
            message: this.$i18n.t("m.The_new_password_cannot_be_empty"),
          },
          {
            trigger: "blur",
            min: 6,
            max: 20,
            message: this.$i18n.t("m.Password_Check_Between"),
          },
          {
            trigger: "blur",
            validator: (rule, value, callback) => {
              const passwordReg =
                /(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9])/;
              if (!passwordReg.test(value)) {
                callback(
                  new Error(this.$i18n.t("m.Password_Validation_Message"))
                );
              } else {
                callback();
              }
            },
          },
          { validator: CheckNewPassword, trigger: "blur" },
        ],
        againPassword: [
          {
            required: true,
            trigger: "blur",
            message: this.$i18n.t("m.Password_Again_Check_Required"),
          },
          { validator: CheckAgainPassword, trigger: "blur" },
        ],
      },
      ruleEmail: {
        password: oldPasswordCheck,
        newEmail: [
          {
            required: true,
            message: this.$i18n.t("m.Email_Check_Required"),
            trigger: "blur",
          },
          {
            type: "email",
            trigger: "change",
            message: this.$i18n.t("m.Email_Check_Format"),
          },
          { validator: CheckEmail, trigger: "blur" },
        ],
        code: [
          {
            required: true,
            message: this.$i18n.t("m.Code_Check_Required"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  mounted() {
    this.formEmail.oldEmail = this.$store.getters.userInfo.email || "";
  },
  methods: {
    changePassword() {
      this.verify.passwordSuccess = true;

      this.$refs["formPassword"].validate((valid) => {
        if (valid) {
          this.loading.btnPassword = true;
          let data = Object.assign({}, this.formPassword);
          delete data.againPassword;
          api.changePassword(data).then(
            (res) => {
              this.loading.btnPassword = false;
              setTimeout(() => {
                this.verify.passwordSuccess = null;
              }, 1000);

              if (res.data.data.code == 200) {
                myMessage.success(this.$i18n.t("m.Update_Successfully"));
                this.visible.passwordAlert = {
                  show: true,
                  title: this.$i18n.t("m.Update_Successfully"),
                  type: "success",
                  description: res.data.data.msg,
                };
                setTimeout(() => {
                  this.visible.passwordAlert = false;
                  this.$router.push({ name: "Logout" });
                }, 1000);
              } else {
                myMessage.error(res.data.data.msg);
                this.visible.passwordAlert = {
                  show: true,
                  title: this.$i18n.t("m.Update_Failed"),
                  type: "warning",
                  description: res.data.data.msg,
                };
                if (res.data.data.code == 403) {
                  this.visible.passwordAlert.type = "error";
                  this.disabled.btnPassword = true;
                }
              }
            },
            (err) => {
              this.loading.btnPassword = false;
              setTimeout(() => {
                this.verify.passwordSuccess = null;
              }, 1000);
            }
          );
        }
      });
    },
    getChangeEmailCode() {
      if (!this.formEmail.newEmail) {
        myMessage.error(this.$i18n.t("m.The_new_email_cannot_be_empty"));
      }
      var emailReg =
        /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
      if (!emailReg.test(this.formEmail.newEmail)) {
        mMessage.error(this.$i18n.t("m.Email_Check_Format"));
        return;
      }
      if (this.formEmail.oldEmail === this.formEmail.newEmail) {
        myMessage.error(this.$i18n.t("m.The_new_email_does_not_change"));
      }
      this.loading.btnSendEmail = true;
      api.getChangeEmailCode(this.formEmail.newEmail).then(
        (res) => {
          myMessage.success(this.$i18n.t("m.Change_Send_Email_Msg"));
          this.$notify.success({
            title: this.$i18n.t("m.Success"),
            message: this.$i18n.t("m.Change_Send_Email_Msg"),
            duration: 5000,
            offset: 50,
          });
          this.loading.btnSendEmail = false;
        },
        (_) => {
          this.loading.btnSendEmail = false;
        }
      );
    },
    changeEmail() {
      this.verify.emailSuccess = true;

      this.$refs["formEmail"].validate((valid) => {
        if (valid) {
          this.loading.btnEmail = true;
          let data = Object.assign({}, this.formEmail);
          api.changeEmail(data).then(
            (res) => {
              this.loading.btnEmail = false;
              setTimeout(() => {
                this.verify.emailSuccess = null;
              }, 1000);
              if (res.data.data.code == 200) {
                myMessage.success(this.$i18n.t("m.Update_Successfully"));
                this.visible.emailAlert = {
                  show: true,
                  title: this.$i18n.t("m.Update_Successfully"),
                  type: "success",
                  description: res.data.data.msg,
                };
                // 更新本地缓存
                this.$store.dispatch("setUserInfo", res.data.data.userInfo);
                this.$refs["formEmail"].resetFields();
                this.formEmail.oldEmail = res.data.data.userInfo.email;
              } else {
                myMessage.error(res.data.data.msg);
                this.visible.emailAlert = {
                  show: true,
                  title: this.$i18n.t("m.Update_Failed"),
                  type: "warning",
                  description: res.data.data.msg,
                };
                if (res.data.data.code == 403) {
                  this.visible.emailAlert.type = "error";
                  this.disabled.btnEmail = true;
                }
              }
            },
            (err) => {
              this.loading.btnEmail = false;
              setTimeout(() => {
                this.verify.emailSuccess = null;
              }, 1000);
            }
          );
        }
      });
    },
  },
};
</script>
