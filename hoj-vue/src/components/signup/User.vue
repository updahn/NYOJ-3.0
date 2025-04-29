<template>
  <div>
    <el-form
      label-position="left"
      ref="formProfile"
      :model="formProfile"
      :rules="isAdmin ? rules : rules2"
    >
      <el-row :gutter="20" v-if="isAdmin">
        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.Email')" prop="email">
            <el-input v-model="formProfile.email" :placeholder="$t('m.Email')"></el-input>
          </el-form-item>
        </el-col>
        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.Student_Number')" prop="number">
            <el-input v-model="formProfile.number" :placeholder="$t('m.Student_Number')"></el-input>
          </el-form-item>
        </el-col>
        <el-col :offset="8" :xs="24"></el-col>

        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.RealName')" prop="realname">
            <el-input v-model="formProfile.realname" :placeholder="$t('m.RealName')"></el-input>
          </el-form-item>
        </el-col>
        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.Englishname')" prop="englishname">
            <el-input v-model="formProfile.englishname" :placeholder="$t('m.Englishname')"></el-input>
          </el-form-item>
        </el-col>
        <el-col :offset="8" :xs="24"></el-col>

        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.Gender')" prop="gender">
            <el-radio-group v-model="formProfile.gender">
              <el-radio label="female">{{$t('m.Female')}}</el-radio>
              <el-radio label="male">{{$t('m.Male')}}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :offset="8" :xs="24"></el-col>
        <el-col :offset="8" :xs="24"></el-col>

        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.Phone_Number')" prop="phoneNumber">
            <el-input v-model="formProfile.phoneNumber" :placeholder="$t('m.Phone_Number')"></el-input>
          </el-form-item>
        </el-col>
        <el-col :offset="8" :xs="24"></el-col>
        <el-col :offset="8" :xs="24"></el-col>

        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.School')" prop="school">
            <el-select
              v-model="formProfile.school"
              filterable
              remote
              reserve-keyword
              :placeholder="$t('m.Enter_Your_School')"
              :remote-method="fetchStates"
              :loading="loading"
              style="width: 100%;"
              :disabled="!isRoom"
            >
              <el-option
                v-for="state in filteredStates"
                :key="state.value"
                :label="state.label"
                :value="state.value"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :offset="8" :xs="24"></el-col>
        <el-col :offset="8" :xs="24"></el-col>

        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.Faculty')" prop="faculty">
            <el-input v-model="formProfile.faculty" :placeholder="$t('m.Faculty')"></el-input>
          </el-form-item>
        </el-col>
        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.Course')" prop="course">
            <el-input v-model="formProfile.course" :placeholder="$t('m.Course')"></el-input>
          </el-form-item>
        </el-col>
        <el-col :offset="8" :xs="24"></el-col>

        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.Start_School')" prop="stSchool">
            <el-date-picker
              v-model="formProfile.stSchool"
              type="year"
              :placeholder="$t('m.Start_School')"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :md="8" :xs="24">
          <el-form-item :label="$t('m.End_School')" prop="edSchool">
            <el-date-picker
              v-model="formProfile.edSchool"
              type="year"
              :placeholder="$t('m.End_School')"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :offset="8" :xs="24"></el-col>

        <el-col :md="24" :xs="24">
          <el-form-item :label="$t('m.Clothes_Size')" prop="clothesSize">
            <el-radio-group v-model="formProfile.clothesSize">
              <el-radio v-for="size in sizes" :key="size" :label="size">{{ size }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20" v-else>
        <el-col :md="22" :xs="24">
          <el-form-item :label="$t('m.Username')" prop="username">
            <el-input v-model="formProfile.username"></el-input>
          </el-form-item>
        </el-col>
        <el-col :offset="2" :xs="24"></el-col>

        <el-col :md="22" :xs="24">
          <el-form-item :label="$t('m.Invent_msg')" prop="content">
            <el-input v-model="formProfile.content"></el-input>
          </el-form-item>
        </el-col>
        <el-col :offset="2" :xs="24"></el-col>
      </el-row>

      <el-row>
        <el-form-item>
          <el-button
            v-if="!isTeam"
            type="primary"
            :loading="loadingSaveBtn"
            @click.native="saveUserSign"
            :style="isRoom === false ? 'display: block; margin: 0 auto;' : ''"
          >{{ $t("m.Save") }}</el-button>
        </el-form-item>
      </el-row>
    </el-form>
  </div>
</template>

<script>
import api from "@/common/api";
import myMessage from "@/common/message";
import utils from "@/common/utils";
import time from "@/common/time";
import { mapGetters } from "vuex";

export default {
  props: {
    profile: {
      type: Object,
    },
    visible: {
      type: Boolean,
      default: false,
    },
    isAdmin: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    const CheckUsernameNotExist = (rule, value, callback) => {
      api.checkUsernameOrEmail(value, undefined).then(
        (res) => {
          if (res.data.data.username === false) {
            callback(new Error(this.$i18n.t("m.The_username_not_exists")));
          } else {
            callback();
          }
        },
        (_) => callback()
      );
    };
    const CheckUsernameAdmin = (rule, value, callback) => {
      api.checkUsernameOrEmail(value, undefined, true).then(
        (res) => {
          if (res.data.data.root === true) {
            callback(new Error(this.$i18n.t("m.The_username_check_root")));
          } else {
            callback();
          }
        },
        (_) => callback()
      );
    };
    return {
      loading: false,
      filteredStates: [],
      sizes: ["S", "M", "L", "XL", "XXL", "XXXL", "XXXXL", "XXXXXL"],
      formProfile: {
        uid: null,
        username: null,
        realname: null,
        englishname: null,
        gender: null,
        school: null,
        faculty: null,
        course: null,
        number: null,
        clothesSize: null,
        phoneNumber: null,
        stSchool: null,
        edSchool: null,
        email: null,
        username: null,
        content: null,
      },
      // 当前用户model
      loadingSaveBtn: false,
      rules: {
        realname: [
          {
            required: true,
            message: this.$i18n.t("m.RealName_Check_Required"),
            trigger: "blur",
          },
          {
            min: 2,
            max: 6,
            message: this.$i18n.t("m.RealName_Check_length"),
            trigger: "blur",
          },
        ],
        englishname: [
          {
            required: true,
            message: this.$i18n.t("m.Englishname_Check_Required"),
            trigger: "blur",
          },
        ],
        gender: [
          {
            required: true,
          },
        ],
        school: [
          {
            required: true,
            message: this.$i18n.t("m.School_Check_Required"),
            trigger: "blur",
          },
          {
            pattern: /^[\u4e00-\u9fa5\d]*[\u4e00-\u9fa5]+[\u4e00-\u9fa5\d]*$/,
            min: 2,
            max: 15,
            message: this.$i18n.t("m.School_Check_length"),
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              if (value === null || value === "") {
                callback();
              } else if (!this.states.find((item) => item.name === value)) {
                callback(new Error(this.$i18n.t("m.Not_Find_School")));
              } else {
                callback();
              }
            },
            trigger: "blur",
          },
        ],
        faculty: [
          {
            required: true,
            message: this.$i18n.t("m.Faculty_Check_Required"),
            trigger: "blur",
          },
        ],
        course: [
          {
            required: true,
            message: this.$i18n.t("m.Course_Check_Required"),
            trigger: "blur",
          },
        ],
        email: [
          {
            required: true,
            message: this.$i18n.t("m.Email_Check_Required"),
            trigger: "blur",
          },
        ],
        number: [
          {
            required: true,
            message: this.$i18n.t("m.Number_Check_Required"),
            trigger: "blur",
          },
          {
            pattern: /^\d+$/,
            message: this.$i18n.t("m.StudentNumber_Check_OnlyDigits"),
            trigger: "blur",
          },
          {
            min: 6,
            max: 15,
            message: this.$i18n.t("m.StudentNumber_Check_length"),
            trigger: "blur",
          },
        ],
        phoneNumber: [
          {
            required: true,
            message: this.$i18n.t("m.PhoneNumber_Check_Required"),
            trigger: "blur",
          },
          {
            min: 11,
            pattern:
              /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/,
            message: this.$i18n.t("m.PhoneNumber_Check_length"),
            trigger: "blur",
          },
        ],
        clothesSize: [
          {
            required: true,
            message: this.$i18n.t("m.ClothesSize_Check_Required"),
            trigger: "blur",
          },
        ],
        stSchool: [
          {
            required: true,
            message: this.$i18n.t("m.StartSchool_Check_Required"),
            trigger: "blur",
          },
        ],
        edSchool: [
          {
            required: true,
            message: this.$i18n.t("m.EndSchool_Check_Required"),
            trigger: "blur",
          },
        ],
      },
      rules2: {
        username: [
          {
            required: true,
            message: this.$i18n.t("m.Username_Check_Required"),
            trigger: "blur",
          },
          {
            validator: CheckUsernameNotExist,
            trigger: "blur",
            message: this.$i18n.t("m.The_username_not_exists"),
          },
          {
            validator: CheckUsernameAdmin,
            trigger: "blur",
            message: this.$i18n.t("m.The_username_check_root"),
          },
          {
            max: 255,
            message: this.$i18n.t("m.Username_Check_Max"),
            trigger: "blur",
          },
        ],
      },
      states: [],
    };
  },
  created() {
    this.getSchoolList();
  },
  mounted() {
    this.syncProfileToForm();
  },
  methods: {
    syncProfileToForm() {
      Object.keys(this.formProfile).forEach((element) => {
        if (this.profile[element] !== undefined) {
          this.formProfile[element] = this.profile[element];
        }
      });
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
    saveUserSign() {
      this.$refs["formProfile"].validate((valid) => {
        if (valid) {
          if (this.isAdmin) {
            let start = this.formProfile.stSchool;
            let end = this.formProfile.edSchool;
            let durationMS = time.durationMs(start, end);
            if (durationMS < 0) {
              myMessage.warning(this.$i18n.t("m.Contets_Time_Check"));
              return;
            }
          }

          this.loadingSaveBtn = true;

          let data = utils.filterEmptyValue(
            Object.assign({}, this.formProfile)
          );

          const func = this.isCreate ? "addUserSign" : "updateUserSign";

          api[func](data).then(
            (res) => {
              const successMessage = this.isCreate
                ? "m.Add_Successfully"
                : "m.Update_Successfully";

              myMessage.success(this.$i18n.t(successMessage));

              // 如果是本人信息修改
              // 更新登录用户的信息
              if (this.isRoom) {
                this.$store.dispatch("setUserInfo", res.data.data);
              }

              this.loadingSaveBtn = false;
              this.visible = false;
            },
            (_) => {
              this.loadingSaveBtn = false;
              this.visible = false;
            }
          );
        }
      });
    },
  },
  watch: {
    profile(newVal, oldVal) {
      this.syncProfileToForm();
    },
    formProfile: {
      handler(newValue) {
        this.$emit("update:profile", newValue);
      },
      deep: true,
    },
    visible(newVal) {
      this.$emit("update:visible", newVal);
    },
  },
  computed: {
    isCreate() {
      if (this.profile) {
        this.syncProfileToForm(); // 主页传入用户信息，编辑用户
      }
      return !this.profile; // 如果没有传入 profile，则表示创建用户
    },
    ...mapGetters(["userInfo"]),
    isRoom() {
      return this.userInfo.username === this.formProfile.username;
    },
    isTeam() {
      return this.$route.name === "signup-contest-sign-list";
    },
  },
};
</script>

<style scoped>
</style>
