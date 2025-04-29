<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ $t('m.Coach_UserPool') }}</span>
        <div class="filter-row">
          <span>
            <el-button
              type="primary"
              size="small"
              @click="addUserSign"
              icon="el-icon-plus"
            >{{ $t('m.Add') }}</el-button>
          </span>
          <span v-if="isCoachAdmin">
            <el-button
              type="primary"
              size="small"
              @click="addUserFromSchool"
              icon="el-icon-plus"
            >{{ $t('m.Import_User_From_School') }}</el-button>
          </span>
          <span>
            <el-button
              type="primary"
              size="small"
              @click="addUserFromExcel"
              icon="el-icon-plus"
            >{{ $t('m.Import_User_From_Excel') }}</el-button>
          </span>
          <span>
            <vxe-input
              v-model="keyword"
              :placeholder="$t('m.Enter_keyword')"
              type="search"
              size="small"
              @search-click="filterByKeyword"
              @keyup.enter.native="filterByKeyword"
            ></vxe-input>
          </span>
          <span>
            <el-date-picker
              v-model="startYear"
              type="year"
              size="small"
              :placeholder="$t('m.Start_School')"
              @change="filterByStatus"
            ></el-date-picker>
          </span>
        </div>
      </div>

      <vxe-table :loading="loading" ref="xTable" :data="userList" auto-resize stripe align="center">
        <vxe-table-column field="username" min-width="200" :title="$t('m.Username')"></vxe-table-column>
        <vxe-table-column field="realname" min-width="150" :title="$t('m.RealName')"></vxe-table-column>
        <vxe-table-column field="email" min-width="100" :title="$t('m.Email')"></vxe-table-column>
        <vxe-table-column field="number" min-width="100" :title="$t('m.Student_Number')"></vxe-table-column>
        <vxe-table-column field="school" min-width="150" :title="$t('m.School')"></vxe-table-column>
        <vxe-table-column field="faculty" min-width="150" :title="$t('m.Faculty')"></vxe-table-column>
        <vxe-table-column field="stSchool" min-width="150" :title="$t('m.Start_School')">
          <template v-slot="{ row }">
            <p v-if="row.stSchool">{{ new Date(row.stSchool).getFullYear() }}</p>
          </template>
        </vxe-table-column>
        <vxe-table-column field="gmtCreate" min-width="150" :title="$t('m.Register_Time')">
          <template v-slot="{ row }">
            <p v-if="row.gmtCreate">{{ row.gmtCreate | localtime }}</p>
          </template>
        </vxe-table-column>

        <vxe-table-column min-width="200" :title="$t('m.Option')">
          <template v-slot="{ row }">
            <div style="margin-bottom:10px">
              <el-tooltip effect="dark" :content="$t('m.Edit')" placement="top">
                <el-button
                  icon="el-icon-edit"
                  size="mini"
                  @click.native="getUserSign(row.username)"
                  type="primary"
                ></el-button>
              </el-tooltip>
              <el-tooltip
                v-if="row.username !== userInfo.username || isCoachAdmin"
                effect="dark"
                :content="$t('m.Remove_UserSign')"
                placement="top"
              >
                <el-button
                  icon="el-icon-close"
                  size="mini"
                  @click.native="removeUserSign(row.username, row.id)"
                  type="warning"
                ></el-button>
              </el-tooltip>
            </div>
          </template>
        </vxe-table-column>
      </vxe-table>
      <div class="panel-options">
        <el-pagination
          class="page"
          layout="prev, pager, next, sizes"
          @current-change="currentChange"
          :page-size="pageSize"
          :total="total"
          @size-change="onPageSizeChange"
          :page-sizes="[10, 30, 50, 100]"
        ></el-pagination>
      </div>

      <el-dialog
        :title="$t('m.Edit_User')"
        width="1000px"
        :visible.sync="handleEditVisible"
        :close-on-click-modal="false"
      >
        <User :profile.sync="profile" :visible.sync="handleEditVisible"></User>
      </el-dialog>

      <el-dialog
        :title="$t('m.Add_User')"
        :width="isCoachAdmin ? '1000px' : '500px'"
        :visible.sync="handleAddVisible"
        :close-on-click-modal="false"
      >
        <User :visible.sync="handleAddVisible" :isAdmin="isCoachAdmin"></User>
      </el-dialog>

      <el-dialog
        :title="$t('m.Import_User_From_School')"
        width="800px"
        :visible.sync="handleAddUserFromSchoolVisible"
        :close-on-click-modal="false"
      >
        <el-form>
          <el-row :gutter="20">
            <el-col :md="20" :xs="24">
              <el-form-item :label="$t('m.Selected_Users')">
                <el-select
                  v-model="excludedUsers"
                  multiple
                  filterable
                  :placeholder="$t('m.Please_SelectUser_In_UserPool')"
                  style="width: 100%;"
                >
                  <el-option
                    v-for="stu in filteredUsers"
                    :key="stu.value"
                    :label="stu.label "
                    :value="stu.value"
                  >
                    <div class="option-content">
                      <span>{{ stu.label }}</span>

                      <el-tag
                        v-for="coachInfo in stu.coachInfoVoList.filter(item => item.root)"
                        :key="coachInfo.coach"
                        type="warning"
                        size="mini"
                        style="margin-left: 8px;"
                      >{{$t('m.Added')}} {{ coachInfo.coach }} ({{ coachInfo.coachUsername }}) {{$t('m.Coach_UserPool')}}</el-tag>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :offset="4" :xs="24"></el-col>

            <el-col>
              <el-form-item style="text-align:center">
                <el-button
                  type="primary"
                  @click="addUserSignBatch()"
                  :loading="loadingSaveBtn"
                >{{ $t('m.Save') }}</el-button>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-dialog>

      <el-dialog
        :title="$t('m.Import_User_From_Excel')"
        width="800px"
        :visible.sync="handleAddUserFromExcelVisible"
        :close-on-click-modal="false"
      >
        <p>1. {{ $t('m.Import_User_Tips15') }}</p>
        <p>2. {{ $t('m.Import_User_Tips16') }}</p>
        <p>3. {{ $t('m.Import_User_Tips4') }}</p>
        <p>4. {{ $t('m.Import_User_Tips17') }}</p>
        <p>5. {{ $t('m.Import_User_Tips18') }}</p>
        <el-upload
          v-if="!uploadUsers.length"
          action
          :show-file-list="false"
          accept=".csv, .xlsx, .xls"
          :before-upload="handleUsersCSV"
        >
          <el-button size="small" icon="el-icon-folder-opened" type="primary">
            {{
            $t('m.Choose_File')
            }}
          </el-button>
        </el-upload>
        <template v-else>
          <vxe-table :data="uploadUsersPage" stripe auto-resize>
            <vxe-table-column
              :title="$t('m.RealName')"
              field="realname"
              min-width="150"
              show-overflow
            >
              <template v-slot="{ row }">{{ row[0] }}</template>
            </vxe-table-column>
            <vxe-table-column :title="$t('m.Email')" field="email" min-width="120" show-overflow>
              <template v-slot="{ row }">{{ row[1] }}</template>
            </vxe-table-column>
            <vxe-table-column :title="$t('m.School')" field="school" min-width="100" show-overflow>
              <template v-slot="{ row }">{{ row[2] }}</template>
            </vxe-table-column>
            <vxe-table-column
              :title="$t('m.Student_Number')"
              field="number"
              min-width="130"
              show-overflow
            >
              <template v-slot="{ row }">{{ row[3] }}</template>
            </vxe-table-column>

            <vxe-table-column :title="$t('m.Gender')" field="gender" min-width="60" show-overflow>
              <template v-slot="{ row }">{{ row[4] }}</template>
            </vxe-table-column>
            <vxe-table-column
              :title="$t('m.Englishname')"
              field="englishname"
              min-width="100"
              show-overflow
            >
              <template v-slot="{ row }">{{ row[5] }}</template>
            </vxe-table-column>
            <vxe-table-column
              :title="$t('m.Phone_Number')"
              field="phoneNumber"
              min-width="96"
              show-overflow
            >
              <template v-slot="{ row }">{{ row[6] }}</template>
            </vxe-table-column>
            <vxe-table-column :title="$t('m.Faculty')" field="faculty" min-width="96" show-overflow>
              <template v-slot="{ row }">{{ row[7] }}</template>
            </vxe-table-column>
            <vxe-table-column :title="$t('m.Course')" field="course" min-width="96" show-overflow>
              <template v-slot="{ row }">{{ row[8] }}</template>
            </vxe-table-column>
            <vxe-table-column
              :title="$t('m.Start_School')"
              field="stSchool"
              min-width="96"
              show-overflow
            >
              <template v-slot="{ row }">{{ row[9] }}</template>
            </vxe-table-column>
            <vxe-table-column
              :title="$t('m.End_School')"
              field="edSchool"
              min-width="96"
              show-overflow
            >
              <template v-slot="{ row }">{{ row[10] }}</template>
            </vxe-table-column>
            <vxe-table-column
              :title="$t('m.Clothes_Size')"
              field="clothesSize"
              min-width="96"
              show-overflow
            >
              <template v-slot="{ row }">{{ row[11] }}</template>
            </vxe-table-column>
          </vxe-table>

          <div class="panel-options">
            <el-button
              type="danger"
              size="small"
              icon="el-icon-delete"
              @click="handleResetData"
            >{{ $t('m.Clear_All') }}</el-button>
            <el-button
              type="primary"
              size="small"
              icon="el-icon-upload"
              :loading="loadingUserUpload"
              @click="handleUsersUpload"
            >{{ $t('m.Upload_All') }}</el-button>
            <el-pagination
              class="page"
              layout="prev, pager, next"
              :page-size="uploadUsersPageSize"
              :current-page.sync="uploadUsersCurrentPage"
              :total="uploadUsers.length"
            ></el-pagination>
          </div>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import api from "@/common/api";
const User = () => import("@/components/signup/User");
import { mapGetters } from "vuex";
import myMessage from "@/common/message";
import { exel } from "@/common/exel";

export default {
  name: "SignupUserPool",
  components: {
    User,
  },
  data() {
    return {
      pageSize: 10,
      currentPage: 1,
      total: 0,
      userList: [],
      userLists: [],
      keyword: "",
      startYear: null, // 入学年份
      loading: false,
      handleEditVisible: false,
      handleAddVisible: false,
      handleAddUserFromSchoolVisible: false,
      handleAddUserFromExcelVisible: false,
      loadingSaveBtn: false,
      loadingSelect: false,
      profile: {},
      excludedUsers: [],
      filteredUsers: [],
      formProfile: {
        uid: null,
        username: null,
        realname: null,
        englishname: null,
        school: null,
        faculty: null,
        course: null,
        number: null,
        clothesSize: null,
        phoneNumber: null,
        stSchool: null,
        edSchool: null,
        email: null,
        gender: null,
        coachInfoVoList: [],
      },
      uploadUsers: [],
      uploadUsersPage: [],
      uploadUsersCurrentPage: 1,
      uploadUsersPageSize: 15,

      loadingUserUpload: false,
    };
  },
  mounted() {
    this.keyword = this.$route.query.keyword || "";
    this.getUserList(this.currentPage);
  },
  methods: {
    // 切换页码回调
    currentChange(page) {
      this.currentPage = page;
      this.getUserList(page);
    },
    onPageSizeChange(pageSize) {
      this.pageSize = pageSize;
      this.getUserList(this.currentPage);
    },
    filterByKeyword() {
      this.currentChange(1);
    },
    filterByStatus() {
      if (this.startYear) {
        this.startYear = this.startYear.getFullYear().toString();
      }
      this.getUserList(1);
    },

    getUserList(page) {
      this.loading = true;
      api
        .getUserSignList(page, this.pageSize, this.startYear, this.keyword)
        .then(
          (res) => {
            this.loading = false;
            this.total = res.data.data.total;
            this.userList = res.data.data.records;
          },
          (res) => {
            this.loading = false;
          }
        );
    },

    getUserSign(username) {
      this.handleEditVisible = true;

      api.getUserSign(username).then(
        (res) => {
          this.profile = res.data.data;
        },
        (res) => {}
      );
    },
    removeUserSign(username, id) {
      this.$confirm(this.$i18n.t("m.Remove_User_Sign_Tips"), "Tips", {
        type: "warning",
      }).then(
        () => {
          api
            .removeUserSign(username, id)
            .then((res) => {
              myMessage.success("success");
              this.getSchoolUserList();
              this.getUserList(this.currentPage);
            })
            .catch(() => {
              this.getSchoolUserList();
              this.getUserList(this.currentPage);
            });
        },
        () => {}
      );
    },

    formatContestTimes(startTime, endTime) {
      const localtime = (time) => {
        return new Date(time).toLocaleString();
      };
      return `${localtime(startTime)} - ${localtime(endTime)}`;
    },

    addUserSign() {
      this.handleAddVisible = true;
    },
    addUserFromSchool() {
      this.handleAddUserFromSchoolVisible = true;
      this.loadingSelect = true;
      this.getSchoolUserList();
    },
    addUserFromExcel() {
      this.handleAddUserFromExcelVisible = true;
    },
    getSchoolUserList() {
      api.getUserSignList(1, null, null, null, this.userInfo.school).then(
        (res) => {
          let userList = res.data.data.records;

          userList = userList.map((profile) => {
            // 只保留 formProfile 中定义的键，并从 profile 获取值
            return Object.keys(this.formProfile).reduce((result, key) => {
              result[key] = profile[key] !== undefined ? profile[key] : null;
              return result;
            }, {});
          });

          this.filteredUsers = userList.map((state) => ({
            label: `${
              state.realname
                ? state.realname
                : this.$i18n.t("m.User") + "( " + state.username + " )"
            } /
                ${state.email ? state.email : this.$i18n.t("m.Email")} /
                ${
                  state.number ? state.number : this.$i18n.t("m.Student_Number")
                }`,
            value: state.uid,
            coachInfoVoList: state.coachInfoVoList,
          }));

          this.userLists = userList;
          this.loadingSelect = false;
        },
        (res) => {
          this.filteredUsers = [];
          this.loadingSelect = false;
        }
      );
    },
    addUserSignBatch() {
      this.loadingSaveBtn = true;

      let excludedUserSet = new Set(this.excludedUsers);
      const excludedUsers = this.userLists.filter((stu) =>
        excludedUserSet.has(stu.uid)
      );

      api.addUserSignBatch(excludedUsers).then(
        (res) => {
          myMessage.success(this.$i18n.t("m.Add_Successfully"));

          this.loadingSaveBtn = false;
          this.handleAddUserFromSchoolVisible = false;
          this.excludedUsers = [];
          this.getSchoolUserList();
          this.getUserList();
        },
        (_) => {
          this.loadingSaveBtn = false;
          this.handleAddUserFromSchoolVisible = false;
          this.excludedUsers = [];
          this.getSchoolUserList();
          this.getUserList();
        }
      );
    },
    removeTag(item) {
      const index = this.excludedUsers.findIndex((st) => st.id === item.id);
      if (index !== -1) {
        this.excludedUsers.splice(index, 1);
      }
    },
    async handleUsersCSV(file) {
      try {
        // 调用 exel 解析方法
        const results = await exel.parse(file);

        let data = results.filter((user) => {
          return user[0] && user[1] && user[2] && user[3];
        });

        let delta = results.length - data.length;
        if (delta > 0) {
          myMessage.warning(delta + this.$i18n.t("m.Generate_Skipped_Reason5"));
        }

        this.uploadUsersCurrentPage = 1;
        this.uploadUsers = data;
        this.uploadUsersPage = data.slice(0, this.uploadUsersPageSize);
        return false; // 阻止默认上传行为
      } catch (error) {
        myMessage.error(`文件解析失败: ${error.message}`);
        return false;
      }
    },
    handleUsersUpload() {
      let data = [];

      for (let i = 0; i < this.uploadUsers.length; i++) {
        // 将年份字符串转换为日期格式
        const stSchoolYear = this.uploadUsers[i][9];
        const edSchoolYear = this.uploadUsers[i][10];

        const stSchoolDate = stSchoolYear ? new Date(stSchoolYear, 0, 1) : null;
        const edSchoolDate = edSchoolYear ? new Date(edSchoolYear, 0, 1) : null;

        data.push({
          realname: this.uploadUsers[i][0],
          email: this.uploadUsers[i][1],
          school: this.uploadUsers[i][2],
          number: this.uploadUsers[i][3],
          gender: this.uploadUsers[i][4],
          englishname: this.uploadUsers[i][5],
          phoneNumber: this.uploadUsers[i][6],
          faculty: this.uploadUsers[i][7],
          course: this.uploadUsers[i][8],
          stSchool: stSchoolDate,
          edSchool: edSchoolDate,
          clothesSize: this.uploadUsers[i][11],
        });
      }

      this.loadingUserUpload = true;
      api.addUserSignBatch(data).then(
        (res) => {
          this.loadingUserUpload = false;
          this.handleAddUserFromExcelVisible = false;
          this.getUserList(this.currentPage);
        },
        (_) => {
          this.loadingUserUpload = false;
          this.handleAddUserFromExcelVisible = false;
          this.getUserList(this.currentPage);
        }
      );
    },
    handleResetData() {
      this.uploadUsers = [];
    },
  },
  computed: {
    ...mapGetters(["userInfo", "isCoachAdmin"]),
  },
  watch: {
    handleEditVisible(newVal) {
      this.getUserList(this.currentPage);
    },
    handleAddVisible(newVal) {
      this.getUserList(this.currentPage);
    },
    uploadUsersCurrentPage(page) {
      this.uploadUsersPage = this.uploadUsers.slice(
        (page - 1) * this.uploadUsersPageSize,
        page * this.uploadUsersPageSize
      );
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

.select-popover-class .el-scrollbar__wrap {
  max-height: 300px;
}
.select-students-demo {
  background-color: #2f2f2f; /* 如果想模拟你截图中较暗的背景，可加深颜色 */
  padding: 20px;
  color: #fff; /* 字体白色以便在深色背景下可见 */
}
.option-content {
  display: flex;
  align-items: center;
}
</style>
