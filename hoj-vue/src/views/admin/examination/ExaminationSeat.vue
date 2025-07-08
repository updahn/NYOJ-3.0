<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ $t('m.Assign_ExaminationSeat') }}</span>
      </div>
      <span>
        <vxe-input
          v-model="keyword"
          :placeholder="$t('m.Enter_keyword')"
          type="search"
          size="medium"
          @search-click="filterByKeyword"
          @keyup.enter.native="filterByKeyword"
        ></vxe-input>
      </span>
      <p></p>
      <vxe-table
        :loading="loading"
        ref="xTable"
        :data="examinationRoomList"
        auto-resize
        stripe
        align="center"
      >
        <vxe-table-column field="eid" width="80" title="ID"></vxe-table-column>
        <vxe-table-column field="school" width="150" :title="$t('m.School')"></vxe-table-column>
        <vxe-table-column field="building" width="80" :title="$t('m.Place')">
          <template v-slot="{ row }">{{ row.building }} # {{ row.room }}</template>
        </vxe-table-column>
        <vxe-table-column field="count" width="80" :title="$t('m.Count')"></vxe-table-column>
        <vxe-table-column field="used" width="80" :title="$t('m.Used')"></vxe-table-column>
        <vxe-table-column :title="$t('m.Room_Size')" width="160" align="center">
          <template v-slot="{ row }">{{ row.maxRow }} / {{ row.maxCol }}</template>
        </vxe-table-column>
        <vxe-table-column min-width="210" :title="$t('m.Info')">
          <template v-slot="{ row }">
            <p>Created Time: {{ row.gmtCreate | localtime }}</p>
            <p>Update Time: {{ row.gmtModified | localtime }}</p>
            <p>Creator: {{ row.author }}</p>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="150" :title="$t('m.Option')">
          <template v-slot="{ row }">
            <template v-if="isMainAdminRole || userInfo.username == row.author">
              <div style="margin-bottom:10px">
                <el-tooltip effect="dark" :content="$t('m.Add_Place')" placement="top">
                  <el-button
                    icon="el-icon-location"
                    size="mini"
                    @click.native="addEid(row.eid)"
                    type="primary"
                  ></el-button>
                </el-tooltip>
              </div>
            </template>
          </template>
        </vxe-table-column>
      </vxe-table>
      <div class="panel-options">
        <el-pagination
          class="page"
          layout="prev, pager, next"
          @current-change="currentChange"
          :page-size="pageSize"
          :total="total"
        ></el-pagination>
      </div>
      <el-form>
        <el-row :gutter="20">
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Eid_List')" required>
              <el-tag
                v-for="username in examinationSeatData.eidList"
                closable
                :close-transition="false"
                :key="username"
                type="warning"
                size="medium"
                @close="removeEid(username)"
                style="margin-right: 7px;margin-top:4px"
              >{{ username }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Retroflex')" required>
              <el-switch
                v-model="examinationSeatData.retroflex"
                :active-text="$t('m.True')"
                :inactive-text="$t('m.False')"
                style="margin: 10px 0"
              ></el-switch>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Random')" required>
              <el-switch
                v-model="examinationSeatData.random"
                :active-text="$t('m.True')"
                :inactive-text="$t('m.False')"
                style="margin: 10px 0"
              ></el-switch>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Sorted')" required>
              <el-switch
                v-model="examinationSeatData.sorted"
                :active-text="$t('m.True')"
                :inactive-text="$t('m.False')"
                style="margin: 10px 0"
              ></el-switch>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Spaced')" required>
              <el-switch
                v-model="examinationSeatData.spaced"
                :active-text="$t('m.True')"
                :inactive-text="$t('m.False')"
                style="margin: 10px 0"
              ></el-switch>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('m.StudentInfo')" required>
          <el-col :span="24">
            <el-card>
              <p>1. {{ $t('m.Import_User_Tips6') }}</p>
              <p>2. {{ $t('m.Import_User_Tips7') }}</p>
              <p>3. {{ $t('m.Import_User_Tips8') }}</p>
              <el-upload
                v-if="!examinationSeatData.studentInfo.length"
                action
                :show-file-list="false"
                accept=".csv, .xlsx, .xls"
                :before-upload="handleUsersCSV"
              >
                <el-button
                  size="small"
                  icon="el-icon-folder-opened"
                  type="primary"
                >{{ $t('m.Choose_File') }}</el-button>
              </el-upload>
              <template v-else>
                <div style="margin-bottom: 10px">
                  <el-button
                    type="primary"
                    icon="el-icon-plus"
                    circle
                    @click="insertEvent(-1)"
                    size="small"
                  ></el-button>
                  <el-button
                    type="danger"
                    icon="el-icon-delete"
                    circle
                    @click="removeEvent()"
                    size="small"
                  ></el-button>
                </div>
                <vxe-table
                  border
                  ref="xAwardTable"
                  :data="uploadUsersPage"
                  :edit-config="{ trigger: 'click', mode: 'cell' }"
                  align="center"
                  @edit-closed="editClosedEvent"
                  style="margin-bottom: 15px"
                >
                  <vxe-table-column type="checkbox" width="60"></vxe-table-column>
                  <vxe-table-column
                    field="realname"
                    min-width="150"
                    :title="$t('m.RealName')"
                    :edit-render="{ name: 'input', attrs: { type: 'text' } }"
                  ></vxe-table-column>
                  <vxe-table-column
                    field="number"
                    min-width="150"
                    :title="$t('m.Student_Number')"
                    :edit-render="{ name: 'input', attrs: { type: 'text' } }"
                  ></vxe-table-column>
                  <vxe-table-column
                    field="course"
                    min-width="150"
                    :title="$t('m.Course')"
                    :edit-render="{ name: 'input', attrs: { type: 'text' } }"
                  ></vxe-table-column>
                  <vxe-table-column
                    field="subject"
                    min-width="150"
                    :title="$t('m.Subject')"
                    :edit-render="{ name: 'input', attrs: { type: 'text' } }"
                  ></vxe-table-column>
                  <vxe-table-column
                    field="username"
                    min-width="150"
                    :title="$t('m.Account')"
                    :edit-render="{ name: 'input', attrs: { type: 'text' } }"
                  ></vxe-table-column>
                </vxe-table>
                <div class="panel-options">
                  <el-pagination
                    class="page"
                    layout="prev, pager, next"
                    :page-size="uploadUsersPageSize"
                    :current-page.sync="uploadUsersCurrentPage"
                    :total="examinationSeatData.studentInfo.length"
                  ></el-pagination>
                </div>
              </template>
            </el-card>
          </el-col>
        </el-form-item>
      </el-form>
      <div>
        <el-button type="primary" @click.native="goAddExaminationSeat">{{ $t('m.Save') }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import api from "@/common/api";
import { mapGetters } from "vuex";
import myMessage from "@/common/message";
import { exel } from "@/common/exel";

export default {
  name: "ExaminationSeat",
  data() {
    return {
      pageSize: 5,
      total: 0,
      examinationRoomList: [],
      keyword: "",
      loading: false,
      excludeAdmin: true,
      currentPage: 1,
      currentId: 1,
      downloadDialogVisible: false,
      examinationSeatData: {
        eidList: [],
        cid: null,
        retroflex: false,
        spaced: true,
        sorted: false,
        random: true,
        studentInfo: [],
      },
      uploadUsersPage: [],
      uploadUsersCurrentPage: 1,
      uploadUsersPageSize: 15,
    };
  },
  mounted() {
    this.getExaminationRoomList(this.currentPage);
    this.getExaminationSeat();
  },
  watch: {
    $route() {
      let refresh = this.$route.query.refresh == "true" ? true : false;
      if (refresh) {
        this.getExaminationRoomList(1);
      }
    },
    uploadUsersCurrentPage(page) {
      this.uploadUsersPage = this.examinationSeatData.studentInfo.slice(
        (page - 1) * this.uploadUsersPageSize,
        page * this.uploadUsersPageSize
      );
    },
  },
  computed: {
    ...mapGetters(["isSuperAdmin", "isMainAdminRole", "userInfo"]),
  },
  methods: {
    // 切换页码回调
    currentChange(page) {
      this.currentPage = page;
      this.getExaminationRoomList(page);
    },
    filterByKeyword() {
      this.currentChange(1);
    },
    getExaminationRoomList(page) {
      let cid = parseInt(this.$route.params.contestID);
      this.loading = true;
      api.getExaminationRoomList(page, this.pageSize, this.keyword, cid).then(
        (res) => {
          this.loading = false;
          this.total = res.data.data.total;
          this.examinationRoomList = res.data.data.records;
          this.examinationSeatData.eidList = this.getUsedRooms();
        },
        (res) => {
          this.loading = false;
        }
      );
    },
    addEid(examinationRoomId) {
      if (!this.examinationSeatData.eidList.includes(examinationRoomId)) {
        this.examinationSeatData.eidList.push(examinationRoomId);
      }
    },
    removeEid(examinationRoomId) {
      this.examinationSeatData.eidList.splice(
        this.examinationSeatData.eidList
          .map((item) => item)
          .indexOf(examinationRoomId),
        1
      );
    },

    async insertEvent(row) {
      let record = {
        realname: "",
        number: "",
        course: "",
        subject: "",
        username: "",
      };
      let { row: newRow } = await this.$refs.xAwardTable.insertAt(record, row);
      const { insertRecords } = this.$refs.xAwardTable.getRecordset();
      this.examinationSeatData.studentInfo =
        this.examinationSeatData.studentInfo.concat(insertRecords);
      await this.$refs.xAwardTable.setActiveCell(newRow, "realname");
    },
    async removeEvent() {
      // 获取当前选中的行
      const selectedRows = this.$refs.xAwardTable.getCheckboxRecords();
      if (selectedRows.length === 0) {
        return;
      }

      // 获取当前页的数据
      const currentPageData = this.uploadUsersPage;

      // 过滤掉选中的行
      const remainingRows = currentPageData.filter(
        (row) =>
          !selectedRows.some((selectedRow) => selectedRow._XID === row._XID)
      );

      // 更新当前页的数据
      this.uploadUsersPage = remainingRows;

      // 更新完整的数据集
      const startIndex =
        (this.uploadUsersCurrentPage - 1) * this.uploadUsersPageSize;
      const endIndex = startIndex + this.uploadUsersPageSize;
      this.examinationSeatData.studentInfo = [
        ...this.examinationSeatData.studentInfo.slice(0, startIndex),
        ...remainingRows,
        ...this.examinationSeatData.studentInfo.slice(endIndex),
      ];

      // 重新加载表格数据
      this.$refs.xAwardTable.reloadData(this.uploadUsersPage);
    },
    editClosedEvent({ row, column }) {
      let xTable = this.$refs.xAwardTable;
      let field = column.property;
      // 判断单元格值是否被修改
      if (xTable.isUpdateByRow(row, field)) {
        setTimeout(() => {
          // 局部更新单元格为已保存状态
          this.$refs.xAwardTable.reloadRow(row, null, field);
        }, 300);
      }
    },
    goAddExaminationSeat() {
      let cid = parseInt(this.$route.params.contestID);

      this.examinationSeatData.cid = cid;

      api.admin_examinationSeat(this.examinationSeatData).then(
        (res) => {
          myMessage.success(this.$i18n.t("m.Assign_Success"));
          this.getExaminationSeat();
        },
        (res) => {
          myMessage.error(res.data.data.msg);
        }
      );
    },
    async handleUsersCSV(file) {
      try {
        // 调用 exel 解析方法
        const results = await exel.parse(file);

        let data = results.filter((user) => {
          return user[0] && user[1];
        });
        let delta = results.length - data.length;
        if (delta > 0) {
          myMessage.warning(delta + this.$i18n.t("m.Generate_Skipped_Reason2"));
        }
        let transformedData = data.map((item) => {
          return {
            realname: item[0],
            number: item[1],
            course: item[2] || null,
            subject: item[3] || null,
            username: item[4] || null,
          };
        });
        // 将转换后的数据添加到 Vue 列表中
        this.uploadUsersCurrentPage = 1;
        this.examinationSeatData.studentInfo = transformedData;
        this.uploadUsersPage = transformedData.slice(
          0,
          this.uploadUsersPageSize
        );
        return false; // 阻止默认上传行为
      } catch (error) {
        myMessage.error(`文件解析失败: ${error.message}`);
        return false;
      }
    },
    getExaminationSeat() {
      let cid = parseInt(this.$route.params.contestID);
      api.getExaminationSeat(null, cid).then(
        (res) => {
          let data = res.data.data;
          this.examinationSeatData.studentInfo = data.seatList;
        },
        (_) => {
          this.examinationSeatData.studentInfo = [];
        }
      );
    },
    getUsedRooms() {
      return this.examinationRoomList
        .sort((a, b) => a.eid - b.eid) // 按 eid 从小到大排序
        .filter((room) => room.used !== 0)
        .map((room) => room.eid);
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
</style>
