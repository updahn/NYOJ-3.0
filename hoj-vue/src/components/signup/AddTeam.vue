<template>
  <div>
    <el-form :model="teamSign" ref="teamSign" :rules="contestRules">
      <template v-if="isContest">
        <span class="info-title" style="font-weight: bold;">{{ $t("m.Contest_Description") }}</span>
        <div class="info">
          <el-row :gutter="20">
            <el-col :md="20" :xs="24">
              <el-form-item :label="$t('m.Contest_Title')">
                <el-input v-model="teamSign.title" size="small" :maxlength="20" disabled></el-input>
              </el-form-item>
            </el-col>
            <el-col :offset="4" :xs="24"></el-col>

            <el-col :md="20" :xs="24">
              <el-form-item :label="$t('m.Contest_Times')">
                <el-input
                  :value="formatContestTimes(teamSign.startTime, teamSign.endTime)"
                  size="small"
                  :maxlength="20"
                  disabled
                ></el-input>
              </el-form-item>
            </el-col>
            <el-col :offset="4" :xs="24"></el-col>
          </el-row>
        </div>

        <span class="info-title" style="font-weight: bold;">{{ $t("m.Select_Teams") }}</span>
        <div class="info">
          <el-row :gutter="20">
            <el-col :md="20" :xs="24">
              <el-form-item :label="$t('m.Selected_Teams')">
                <el-select
                  multiple
                  v-model="excludedTeams"
                  :placeholder="$t('m.Please_SelectTeam_In_TeamPool')"
                  style="width: 100%;"
                  popper-class="select-popover-class"
                >
                  <el-checkbox
                    :value="selectAll"
                    :indeterminate="indeterminate"
                    @change="selectAllHandle"
                  >{{ $t("m.Selected_All") }}</el-checkbox>

                  <el-option
                    v-for="state in filteredTeams"
                    :key="state.value"
                    :label="state.label"
                    :value="state.value"
                  >
                    <el-checkbox :value="excludedTeams.includes(state.value)" :label="state.label"></el-checkbox>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :offset="4" :xs="24"></el-col>
          </el-row>
        </div>
      </template>

      <template v-else>
        <template v-if="teamSign.participants > 1">
          <span class="info-title" style="font-weight: bold;">{{ $t("m.Team_Name") }}</span>
          <div class="info">
            <el-row :gutter="20">
              <el-col :md="11" :xs="24">
                <el-form-item prop="cname" :label="$t('m.Cname')">
                  <el-input v-model="teamSign.cname" size="small" :maxlength="20"></el-input>
                </el-form-item>
              </el-col>
              <el-col :md="11" :xs="24">
                <el-form-item prop="ename" :label="$t('m.Ename')">
                  <el-input v-model="teamSign.ename" size="small" :maxlength="20"></el-input>
                </el-form-item>
              </el-col>
              <el-col :offset="2" :xs="24"></el-col>
            </el-row>
          </div>
        </template>

        <span class="info-title" style="font-weight: bold;">{{ $t("m.Team_Names") }}</span>
        <div class="info">
          <el-row :gutter="20" align="middle">
            <el-col :md="11" :xs="24" style="display: flex; align-items: center;">
              <el-form-item prop="username1" :label="$t('m.TeamMember1')" style="flex: 1;">
                <div style="width: 100%;">
                  <el-select
                    v-model="teamSign.username1"
                    filterable
                    remote
                    reserve-keyword
                    clearable
                    :placeholder="$t('m.Please_SelectUser_In_UserPool')"
                    :remote-method="fetchUsers"
                    :loading="loading"
                    style="width: 100%;"
                    :disabled="!isCoachAdmin"
                  >
                    <el-option
                      v-for="state in filteredUsers"
                      :key="state.value"
                      :label="state.label"
                      :value="state.value"
                    ></el-option>
                  </el-select>
                  <span
                    style="display: block; margin-top: 5px; font-size: 12px;"
                  >{{ $t("m.SelectUser_Tips") }}</span>
                </div>
              </el-form-item>
            </el-col>

            <el-col :md="11" :xs="24" style="display: flex; align-items: center; height: 120px;">
              <el-form-item style="margin-bottom: 0;">
                <el-button
                  icon="el-icon-plus"
                  type="primary"
                  size="small"
                  @click="addUser"
                  :disabled="!maxParticipants"
                  style="margin-left: 10px;"
                ></el-button>
                <el-button
                  icon="el-icon-minus"
                  type="primary"
                  size="small"
                  @click="deleteUser"
                  :disabled="!minParticipants"
                  style="margin-left: 10px;"
                ></el-button>
              </el-form-item>
            </el-col>
            <el-col :offset="2" :xs="24"></el-col>

            <el-col :md="11" :xs="24" v-if="teamSign.participants > 1">
              <el-form-item prop="username2" :label="$t('m.TeamMember2')" style="flex: 1;">
                <div style="width: 100%;">
                  <el-select
                    v-model="teamSign.username2"
                    filterable
                    remote
                    reserve-keyword
                    clearable
                    :placeholder="$t('m.Please_SelectUser_In_UserPool')"
                    :remote-method="fetchUsers"
                    :loading="loading"
                    style="width: 100%;"
                  >
                    <el-option
                      v-for="state in filteredUsers"
                      :key="state.value"
                      :label="state.label"
                      :value="state.value"
                    ></el-option>
                  </el-select>
                  <span
                    style="display: block; margin-top: 5px; font-size: 12px;"
                  >{{ $t("m.SelectUser_Tips") }}</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :offset="13" :xs="24"></el-col>

            <el-col :md="11" :xs="24" v-if="teamSign.participants > 2">
              <el-form-item prop="username3" :label="$t('m.TeamMember3')" style="flex: 1;">
                <div style="width: 100%;">
                  <el-select
                    v-model="teamSign.username3"
                    filterable
                    remote
                    reserve-keyword
                    clearable
                    :placeholder="$t('m.Please_SelectUser_In_UserPool')"
                    :remote-method="fetchUsers"
                    :loading="loading"
                    style="width: 100%;"
                  >
                    <el-option
                      v-for="state in filteredUsers"
                      :key="state.value"
                      :label="state.label"
                      :value="state.value"
                    ></el-option>
                  </el-select>
                  <span
                    style="display: block; margin-top: 5px; font-size: 12px;"
                  >{{ $t("m.SelectUser_Tips") }}</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :offset="13" :xs="24"></el-col>
          </el-row>
        </div>

        <span class="info-title" style="font-weight: bold;">{{ $t("m.Coach_Info") }}</span>
        <div class="info">
          <el-row :gutter="20">
            <el-col :md="11" :xs="24">
              <el-form-item prop="instructor" :label="$t('m.Instructor')">
                <el-input v-model="teamSign.instructor" size="small" :maxlength="20"></el-input>
              </el-form-item>
            </el-col>
            <el-col :offset="13" :xs="24"></el-col>
          </el-row>
        </div>
      </template>

      <span class="info-title" style="font-weight: bold;">
        {{ isContest ? $t("m.Signup_Type") : $t("m.Team_Type") }}
        <p></p>
      </span>
      <div class="info">
        <el-row :gutter="20">
          <el-col :md="isContest ? 20 : 11" :xs="24">
            <el-form-item prop="type">
              <template v-if="isContest">
                <el-select
                  v-model="teamSign.type"
                  :placeholder="$t('m.Please_SelectQ  uota_In_QuotaType')"
                  style="width: 100%;"
                >
                  <el-option
                    v-for="(item, i) in QUOTA_TYPE_REVERSE"
                    :key="i"
                    :value="parseInt(i)"
                    :label="$t('m.' + item.name)"
                  />
                </el-select>
              </template>
              <template v-else>
                <el-switch
                  v-model="teamSign.type"
                  :active-value="1"
                  :inactive-value="0"
                  :active-text="$t('m.Girls')"
                  :inactive-text="$t('m.Formal')"
                />
              </template>
            </el-form-item>
          </el-col>
          <el-col :offset="isContest ? 4 : 13" :xs="24"></el-col>
        </el-row>
      </div>

      <el-form-item style="text-align:center">
        <el-button
          type="primary"
          @click="isContest ? addTeamSignBatch(): saveTeamSign()"
          :loading="loadingSaveBtn"
        >{{ $t('m.Save') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
const User = () => import("@/components/signup/User");
const Accordion = () => import("@/components/admin/Accordion.vue");
import { QUOTA_TYPE_REVERSE } from "@/common/constants";
import api from "@/common/api";
import utils from "@/common/utils";
import myMessage from "@/common/message";
import { mapGetters } from "vuex";

export default {
  components: {
    User,
    Accordion,
  },
  props: {
    teamSign: {
      type: Object,
      required: true,
      default: {
        id: null,
        cid: null,
        cname: null,
        ename: null,
        username1: null,
        username2: null,
        username3: null,
        school: null,
        type: 0,
        instructor: null,
        title: null,
        startTime: null,
        endTime: null,
        participants: 1,
        maxParticipants: 3,
        visible: false, // 队伍
        school: null,
      },
    },
    visible: {
      type: Boolean,
      default: false,
    },
    filteredTeams: {
      type: Array,
      default: [],
    },
  },
  data() {
    return {
      contestRules: {
        cname: [
          {
            required: true,
            message: this.$i18n.t("m.Cname_Check_Required"),
            trigger: "blur",
          },
          {
            min: 1,
            max: 50,
            message: this.$i18n.t("m.TeamName_Check_Length"),
            trigger: "blur",
          },
        ],
        ename: [
          {
            required: true,
            message: this.$i18n.t("m.Ename_Check_Required"),
            trigger: "blur",
          },
          {
            min: 1,
            max: 50,
            message: this.$i18n.t("m.TeamName_Check_Length"),
            trigger: "blur",
          },
        ],
      },
      userList: [],
      filteredUsers: [],
      loading: false,
      excludedTeams: [],
      loadingSaveBtn: false,
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
      },
    };
  },
  created() {
    this.QUOTA_TYPE_REVERSE = Object.assign({}, QUOTA_TYPE_REVERSE);
  },
  mounted() {
    this.getUserList();
    if (!this.isCoachAdmin) {
      this.teamSign.username1 = this.userInfo.username;
    }
    this.refreshFilteredUsers();
  },
  methods: {
    getUserList() {
      api.getUserSignList(1, null, null, null).then(
        (res) => {
          let userList = res.data.data.records;

          this.userList = userList.map((profile) => {
            // 只保留 formProfile 中定义的键，并从 profile 获取值
            return Object.keys(this.formProfile).reduce((result, key) => {
              result[key] = profile[key] !== undefined ? profile[key] : null;
              return result;
            }, {});
          });

          this.filteredUsers = this.userList.map((state) => ({
            label: state.email
              ? `${state.realname} (${state.email})`
              : state.realname,
            value: state.username,
          }));
        },
        (res) => {
          this.loading = false;
        }
      );
    },
    refreshFilteredUsers() {
      this.fetchUsers("");
    },
    fetchUsers(query) {
      this.loading = true;

      setTimeout(() => {
        const filterQuery = query.trim().toLowerCase();

        // 确保 userList 存在且为数组
        const userList = Array.isArray(this.userList) ? this.userList : [];

        // 保证三个队员不重复
        const excludedUsernames = [
          this.teamSign.username1,
          this.teamSign.username2,
          this.teamSign.username3,
        ].filter(Boolean);

        this.filteredUsers = userList
          .filter((state) =>
            filterQuery
              ? state.realname.toLowerCase().includes(filterQuery)
              : true
          )
          .filter((state) => !excludedUsernames.includes(state.username))
          .map((state) => ({
            label: state.email
              ? `${state.realname} (${state.email})`
              : state.realname,
            value: state.username,
          }));

        const excludedNotInList = excludedUsernames.filter(
          (username) => !userList.some((user) => user.username === username)
        );

        excludedNotInList.forEach((username) => {
          if (userList.some((user) => user.includes(username))) {
            this.filteredUsers.push({
              label: `Not Found (${username})`,
              value: String(username),
            });
          }
        });

        this.loading = false;
      }, 200);
    },

    saveTeamSign() {
      const userList = Array.isArray(this.userList) ? this.userList : [];

      this.teamSign.teamConfig = Array.isArray(this.teamSign.teamConfig)
        ? this.teamSign.teamConfig
        : [];

      const result = userList.filter(
        (user) =>
          [
            this.teamSign.username1,
            this.teamSign.username2,
            this.teamSign.username3,
          ]
            .filter(Boolean)
            .includes(user.username) &&
          !this.teamSign.teamConfig.some(
            (config) => config.username === user.username
          )
      );

      this.teamSign.teamConfig.push(...result);

      // 如果 this.teamSign.school 为空，从 result 的第一位取 school 值
      if (!this.teamSign.school && result.length > 0 && result[0].school) {
        this.teamSign.school = result[0].school;
      }

      this.$refs["teamSign"].validate((valid) => {
        if (valid) {
          this.loadingSaveBtn = true;

          const func = this.isCreate ? "addTeamSign" : "updateTeamSign";

          let data = utils.filterEmptyValue(Object.assign({}, this.teamSign));

          api[func](data).then(
            (res) => {
              const successMessage = this.isCreate
                ? "m.Add_Successfully"
                : "m.Update_Successfully";

              myMessage.success(this.$i18n.t(successMessage));

              this.loadingSaveBtn = false;
              this.visible = false;
            },
            (_) => {
              this.loadingSaveBtn = false;
              this.visible = false;
            }
          );
        } else {
          myMessage.error(this.$i18n.t("m.Please_check_your_Cname_or_Ename"));
        }
      });
    },

    addTeamSignBatch() {
      this.loadingSaveBtn = true;

      api
        .addTeamSignBatch(
          this.excludedTeams,
          this.teamSign.cid,
          this.teamSign.type
        )
        .then(
          (res) => {
            myMessage.success(this.$i18n.t("m.Add_Successfully"));

            this.loadingSaveBtn = false;
            this.visible = false;
          },
          (_) => {
            this.loadingSaveBtn = false;
            this.visible = false;
          }
        );
    },

    addUser() {
      if (this.maxParticipants) {
        this.teamSign.participants += 1;
      }
    },
    deleteUser() {
      if (this.minParticipants) {
        this.teamSign.participants -= 1;
      }
    },

    formatContestTimes(startTime, endTime) {
      const localtime = (time) => {
        return new Date(time).toLocaleString();
      };
      return `${localtime(startTime)} - ${localtime(endTime)}`;
    },

    selectAllHandle(bool) {
      this.excludedTeams = bool ? this.filteredTeams.map((v) => v.value) : [];
    },
  },
  watch: {
    teamSign(newVal) {
      this.$emit("update:teamSign", newVal);
    },
    "teamSign.username1": "refreshFilteredUsers",
    "teamSign.username2": "refreshFilteredUsers",
    "teamSign.username3": "refreshFilteredUsers",
    visible(newVal) {
      this.$emit("update:visible", newVal);
    },
    $route() {
      this.getUserList();
    },
  },
  computed: {
    ...mapGetters(["userInfo", "isCoachAdmin"]),
    isContest() {
      let routeName = this.$route.name;

      return (
        routeName === "signup-contest-list" ||
        routeName === "signup-contest-sign-list"
      );
    },
    isCreate() {
      return this.teamSign.id === null;
    },
    maxParticipants() {
      return this.teamSign.participants < this.teamSign.maxParticipants;
    },
    minParticipants() {
      return this.teamSign.participants > 1;
    },
    selectAll() {
      if (this.excludedTeams.length) {
        return this.excludedTeams.length === this.filteredTeams.length;
      }
      return false;
    },
    indeterminate() {
      if (this.excludedTeams.length) {
        return this.excludedTeams.length !== this.filteredTeams.length;
      }
      return false;
    },
  },
};
</script>

<style>
.info {
  margin-left: 10px;
  margin-right: 10px;
}
el-col {
  height: 150px;
}

/* 控制全选按钮样式 */
.select-popover-class .el-scrollbar__view > .el-checkbox {
  padding: 5px 20px;
}
/* 取消多选框触发事件 */
.select-popover-class .el-scrollbar__view > li .el-checkbox {
  pointer-events: none;
}
/* 隐藏多选框选中勾选样式 √ */
.select-popover-class .el-scrollbar__view > li::after {
  display: none;
}
</style>