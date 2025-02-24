<template>
  <el-form ref="formProfile" :model="formProfile" :rules="rules">
    <el-row :gutter="30" justify="space-around">
      <el-col :md="10" :xs="24">
        <div class="left">
          <p class="section-title">{{ $t("m.Real_Info") }}</p>
        </div>
      </el-col>
      <el-col :md="4" :lg="4">
        <p></p>
      </el-col>
      <el-col :md="10" :xs="24">
        <div class="right">
          <p class="section-title">{{ $t("m.Race_Info") }}</p>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="30" justify="space-around">
      <el-col :md="10" :xs="24">
        <div class="left">
          <el-form-item prop="realname" :label="$t('m.RealName')">
            <el-input v-model="formProfile.realname" />
          </el-form-item>
          <el-form-item prop="school" :label="$t('m.School')" label-width="auto">
            <el-select
              v-model="formProfile.school"
              filterable
              remote
              reserve-keyword
              :placeholder="$t('m.Enter_Your_School')"
              :remote-method="fetchStates"
              :loading="loading"
              style="width: 100%;"
            >
              <el-option
                v-for="state in filteredStates"
                :key="state.value"
                :label="state.label"
                :value="state.value"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item prop="faculty" :label="$t('m.Faculty')">
            <el-input v-model="formProfile.faculty" :maxlength="20" />
          </el-form-item>
          <el-form-item prop="course" :label="$t('m.Course')">
            <el-input v-model="formProfile.course" :maxlength="20" />
          </el-form-item>
          <el-form-item prop="number" :label="$t('m.Student_Number')">
            <el-input v-model="formProfile.number" :maxlength="20" />
          </el-form-item>
        </div>
      </el-col>
      <el-col :md="4" :lg="4">
        <div class="separator hidden-md-and-down"></div>
        <p></p>
      </el-col>
      <el-col :md="10" :xs="24">
        <div class="right">
          <el-form-item :label="$t('m.Clothes_Size')">
            <el-radio-group v-model="formProfile.clothesSize">
              <el-radio v-for="size in sizes" :key="size" :label="size">{{ size }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item prop="phoneNumber" :label="$t('m.Phone_Number')">
            <el-input v-model="formProfile.phoneNumber" :maxlength="20" />
          </el-form-item>
        </div>
      </el-col>
    </el-row>
    <div style="text-align: center; margin-top: 10px">
      <el-button type="primary" @click="updateUserSign" :loading="loadingSaveBtn">{{ $t("m.Save") }}</el-button>
    </div>
  </el-form>
</template>

<script>
import api from "@/common/api";
import myMessage from "@/common/message";
import "element-ui/lib/theme-chalk/display.css";
import { mapGetters } from "vuex";
import utils from "@/common/utils";

export default {
  data() {
    return {
      loadingSaveBtn: false,
      formProfile: {
        username: "",
        realname: "",
        school: null,
        faculty: "",
        course: "",
        number: "",
        clothesSize: "",
        phoneNumber: "",
      },
      sizes: ["S", "M", "L", "XL", "XXL", "XXXL", "XXXXL", "XXXXXL"],
      rules: {
        realname: [
          {
            required: true,
            message: this.$i18n.t("m.RealName_Check_Required"),
            trigger: "blur",
          },
          {
            pattern: /^[\u4e00-\u9fa5]+$/,
            min: 2,
            max: 6,
            message: this.$i18n.t("m.RealName_Check_length"),
            trigger: "blur",
          },
        ],
        school: [
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
        number: [
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
            min: 11,
            pattern:
              /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/,
            message: this.$i18n.t("m.PhoneNumber_Check_length"),
            trigger: "blur",
          },
        ],
      },
      filteredStates: [],
      loading: false,
      states: [],
    };
  },
  mounted() {
    this.getSchoolList();
    let profile = this.$store.getters.userInfo;
    Object.keys(this.formProfile).forEach((element) => {
      if (profile[element] !== undefined) {
        this.formProfile[element] = profile[element];
      }
    });
  },
  methods: {
    updateUserSign() {
      this.$refs["formProfile"].validate((valid) => {
        if (valid) {
          this.loadingSaveBtn = true;
          let updateData = utils.filterEmptyValue(
            Object.assign({}, this.formProfile)
          );
          api.changeUserRace(updateData).then(
            (res) => {
              myMessage.success(this.$i18n.t("m.Update_Successfully"));
              this.$store.dispatch("setUserInfo", res.data.data);
              this.loadingSaveBtn = false;
            },
            (_) => {
              this.loadingSaveBtn = false;
            }
          );
        }
      });
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
    ...mapGetters(["webLanguage", "token", "isAuthenticated"]),
  },
};
</script>

<style scoped>
.form-item-wrapper {
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 21px;
  font-weight: 500;
  padding-top: 10px;
  padding-bottom: 20px;
  line-height: 30px;
}
.left {
  text-align: center;
}
.right {
  text-align: center;
}
/deep/ .el-input__inner {
  height: 32px;
}
/deep/ .el-form-item__label {
  font-size: 12px;
  line-height: 20px;
}
.separator {
  display: block;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  border: 1px dashed #eee;
}
.template_code {
  text-align: left;
  margin-left: 10px;
}
</style>
