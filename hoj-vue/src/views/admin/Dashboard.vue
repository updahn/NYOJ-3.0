<template>
  <div>
    <el-row :gutter="20">
      <el-col :xs="24" :md="10" :lg="8">
        <el-card class="admin-info">
          <div slot="header">
            <el-row :gutter="10">
              <el-col :span="8">
                <avatar
                  :username="userInfo.username"
                  :inline="true"
                  :size="100"
                  color="#FFF"
                  :src="userInfo.avatar"
                ></avatar>
              </el-col>
              <el-col :span="16">
                <span class="panel-title admin-info-name">
                  {{
                  userInfo.username
                  }}
                </span>
                <p>
                  <el-tag effect="dark" size="small" type="warning">
                    {{
                    isSuperAdmin == true
                    ? $t('m.Super_Admin')
                    : isProblemAdmin == true
                    ? $t('m.All_Problem_Admin')
                    : $t('m.Admin')
                    }}
                  </el-tag>
                </p>
              </el-col>
            </el-row>
          </div>
          <div class="last-info">
            <p class="last-info-title home-title">{{ $t('m.Last_Login') }}</p>
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

      <!-- <el-col :md="14" :lg="16" v-if="isSuperAdmin"> -->
      <el-col :xs="24" :md="14" :lg="16">
        <div class="info-container">
          <info-card
            color="#909399"
            icon="fa fa-users"
            :message="$t('m.Total_Users')"
            iconSize="30px"
            class="info-item"
            :value="infoData.userNum"
          ></info-card>
          <info-card
            color="#67C23A"
            icon="fa fa-list"
            :message="$t('m.Today_Submissions')"
            class="info-item"
            :value="infoData.todayJudgeNum"
          ></info-card>
          <info-card
            color="#409EFF"
            icon="fa fa-trophy"
            :message="$t('m.Recent_14_Days_Contests')"
            class="info-item"
            :value="infoData.recentContestNum"
          ></info-card>
        </div>
        <!-- <el-card title="System_Overview" v-if="isSuperAdmin"> -->
        <el-card>
          <div slot="header">
            <span class="panel-title home-title">
              {{
              $t('m.Backend_System')
              }}
            </span>
          </div>
          <el-row>
            <el-col :xs="24" :md="8">
              <span>
                {{ $t('m.Server_Number') }}：
                <el-tag effect="dark" color="#2d8cf0" size="mini">
                  {{
                  generalInfo.backupService.length
                  }}
                </el-tag>
              </span>
            </el-col>
            <el-col :xs="24" :md="8">
              <span>
                {{ $t('m.Nacos_Status') }}：
                <el-tag
                  effect="dark"
                  color="#19be6b"
                  size="mini"
                  v-if="generalInfo.nacos.status == 'UP'"
                >{{ generalInfo.nacos.status }}</el-tag>
                <el-tag effect="dark" color="#f90" size="mini" v-else>
                  {{
                  generalInfo.nacos.status
                  }}
                </el-tag>
              </span>
            </el-col>
            <el-col :xs="24" :md="8">
              <span>
                {{ $t('m.HTTPS_Status') }}：
                <el-tag
                  :type="https ? 'success' : 'danger'"
                  size="small"
                  effect="dark"
                >{{ https ? 'Enabled' : 'Disabled' }}</el-tag>
              </span>
            </el-col>
          </el-row>
          <h2 class="home-title">{{ $t('m.Backend_Service') }}</h2>
          <vxe-table stripe auto-resize :data="generalInfo.backupService" align="center">
            <vxe-table-column :title="$t('m.Name')" min-width="130">
              <template v-slot="{ row }">
                <span>{{ row['serviceId'] }}</span>
              </template>
            </vxe-table-column>
            <vxe-table-column field="host" :title="$t('m.Host')" min-width="110"></vxe-table-column>
            <vxe-table-column field="port" :title="$t('m.Port')" min-width="80"></vxe-table-column>
            <vxe-table-column min-width="80" field="backupCores" :title="$t('m.CPU_Core')"></vxe-table-column>

            <vxe-table-column
              min-width="100"
              field="backupPercentCpuLoad"
              :title="$t('m.CPU_Usage')"
            ></vxe-table-column>

            <vxe-table-column
              min-width="100"
              field="backupPercentMemoryLoad"
              :title="$t('m.Mem_Usage')"
            ></vxe-table-column>
            <vxe-table-column field="secure" :title="$t('m.Secure')" min-width="80">
              <template v-slot="{ row }">
                <el-tooltip content="是否触发保护阈值" placement="top">
                  <el-tag effect="dark" color="#ed3f14" v-if="row.secure">True</el-tag>
                  <el-tag effect="dark" color="#2d8cf0" v-else>False</el-tag>
                </el-tooltip>
              </template>
            </vxe-table-column>
            <vxe-table-column :title="$t('m.Healthy_Status')" min-width="100">
              <template v-slot="{ row }">
                <el-tag
                  effect="dark"
                  color="#19be6b"
                  v-if="row.metadata['nacos.healthy'] == 'true'"
                >{{ $t('m.Healthy') }}</el-tag>
                <el-tag effect="dark" color="#f90" v-else>
                  {{
                  $t('m.Unhealthy')
                  }}
                </el-tag>
              </template>
            </vxe-table-column>
          </vxe-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:10px">
      <div
        slot="header"
        style="display: flex; justify-content: space-between; align-items: center;"
      >
        <span class="panel-title home-title">{{ $t('m.Judge_Server') }}</span>
        <el-button
          type="primary"
          @click="refreshJudgeServerList"
          size="small"
          icon="el-icon-refresh"
          :loading="judgeLoading"
        >{{ $t('m.Refresh') }}</el-button>
      </div>
      <div style="margin-bottom: 10px;font-size: 15px;">
        {{ $t('m.Server_Number') }}：
        <el-tag effect="dark" color="rgb(25, 190, 107)" size="mini">{{ judgeInfo.length }}</el-tag>
      </div>
      <vxe-table stripe auto-resize :data="judgeInfo" :loading="judgeLoading" align="center">
        <!-- 主条目行 -->
        <vxe-table-column type="seq" width="50"></vxe-table-column>
        <vxe-table-column :title="$t('m.Name')" min-width="150">
          <template v-slot="{ row }">
            <span>{{ row.service.metadata.judgeName }}</span>
          </template>
        </vxe-table-column>
        <vxe-table-column :title="$t('m.Host')" min-width="80">
          <template v-slot="{ row }">
            <span>{{ row.service.host }}</span>
          </template>
        </vxe-table-column>
        <vxe-table-column :title="$t('m.Port')" min-width="80">
          <template v-slot="{ row }">
            <span>{{ row.service.port }}</span>
          </template>
        </vxe-table-column>

        <vxe-table-column min-width="80" field="cpuCores" :title="$t('m.CPU_Core')"></vxe-table-column>

        <vxe-table-column min-width="100" field="percentCpuLoad" :title="$t('m.CPU_Usage')"></vxe-table-column>

        <vxe-table-column min-width="110" field="percentMemoryLoad" :title="$t('m.Mem_Usage')"></vxe-table-column>

        <vxe-table-column :title="$t('m.Secure')" min-width="80">
          <template v-slot="{ row }">
            <el-tooltip content="是否触发保护阈值" placement="top">
              <el-tag effect="dark" color="#ed3f14" v-if="row.service.secure">True</el-tag>
              <el-tag effect="dark" color="#2d8cf0" v-else>False</el-tag>
            </el-tooltip>
          </template>
        </vxe-table-column>
        <vxe-table-column :title="$t('m.Healthy_Status')" min-width="100">
          <template v-slot="{ row }">
            <el-tag
              effect="dark"
              color="#19be6b"
              v-if="row.service.metadata['nacos.healthy'] == 'true'"
            >{{ $t('m.Healthy') }}</el-tag>
            <el-tag effect="dark" color="#f90" v-else>
              {{
              $t('m.Unhealthy')
              }}
            </el-tag>
          </template>
        </vxe-table-column>

        <!-- Docker 子项渲染 -->
        <vxe-table-column field="docker" type="expand" :title="$t('m.Unfold')" width="100">
          <!-- 子表格 -->
          <template #content="{ row: parentRow }">
            <vxe-table stripe auto-resize :data="parentRow.docker" border="center">
              <vxe-table-column type="seq" width="50" title="#"></vxe-table-column>
              <vxe-table-column field="CONTAINER ID" :title="$t('m.Container_ID')" min-width="60"></vxe-table-column>
              <vxe-table-column field="NAME" :title="$t('m.Container_Name')" min-width="100"></vxe-table-column>
              <vxe-table-column field="IMAGE" :title="$t('m.Container_Image')" min-width="150"></vxe-table-column>
              <vxe-table-column field="CPU %" :title="$t('m.Container_CPU')" min-width="20"></vxe-table-column>
              <vxe-table-column
                field="MEM USAGE / LIMIT"
                :title="$t('m.Container_MEM')"
                min-width="150"
              ></vxe-table-column>
              <vxe-table-column field="MEM %" :title="$t('m.Container_MEM_Usage')" min-width="20"></vxe-table-column>
              <vxe-table-column field="NET I/O" :title="$t('m.Container_NET')" min-width="150"></vxe-table-column>
              <vxe-table-column field="BLOCK I/O" :title="$t('m.Container_BLOCK')" min-width="100"></vxe-table-column>
              <vxe-table-column field="COMMAND" :title="$t('m.Container_Command')" show-overflow></vxe-table-column>
              <vxe-table-column field="PORTS" :title="$t('m.Container_Ports')" show-overflow>
                <template v-slot="{ row }">
                  <span>{{ row.PORTS }}</span>
                </template>
              </vxe-table-column>
              <vxe-table-column field="CREATED" :title="$t('m.Container_Created')" min-width="100"></vxe-table-column>
              <vxe-table-column field="STATUS" :title="$t('m.Container_Status')" min-width="150">
                <template v-slot="{ row }">
                  <span>{{ row.STATUS }}</span>
                  <p></p>
                  <el-tag effect="dark" color="#2d8cf0" v-if="row.STATUS.startsWith('Up')">Up</el-tag>
                  <el-tag
                    effect="dark"
                    color="#f90"
                    v-else-if="row.STATUS.startsWith('Created')"
                  >Created</el-tag>
                  <el-tag
                    effect="dark"
                    color="#ed3f14"
                    v-else-if="row.STATUS.startsWith('Exited')"
                  >Exited</el-tag>
                </template>
              </vxe-table-column>
              <vxe-table-column min-width="150" :title="$t('m.Option')">
                <template v-slot="{ row }">
                  <el-button
                    v-for="action in ['start', 'stop', 'restart', 'pull']"
                    :key="action"
                    type="text"
                    size="mini"
                    @click="editContainer(row['CONTAINER ID'], row['NAME'], action, true, parentRow.service.host)"
                  >{{ action.charAt(0).toUpperCase() + action.slice(1) }}</el-button>
                </template>
              </vxe-table-column>
            </vxe-table>
          </template>
        </vxe-table-column>
      </vxe-table>
    </el-card>

    <el-card style="margin-top:10px">
      <div
        slot="header"
        style="display: flex; justify-content: space-between; align-items: center;"
      >
        <span class="panel-title home-title">{{ $t('m.Docker_Server') }}</span>
        <el-button
          type="primary"
          @click="refreshDockerServerList"
          size="small"
          icon="el-icon-refresh"
          :loading="dockerLoading"
        >{{ $t('m.Refresh') }}</el-button>
      </div>
      <div style="margin-bottom: 10px;font-size: 15px;">
        {{ $t('m.Docker_Number') }}：
        <el-tag effect="dark" color="rgb(25, 190, 107)" size="mini">{{ dockerInfo.length }}</el-tag>
      </div>
      <vxe-table stripe auto-resize :data="dockerInfo" :loading="dockerLoading" align="center">
        <vxe-table-column field="CONTAINER ID" :title="$t('m.Container_ID')" min-width="60"></vxe-table-column>
        <vxe-table-column field="NAME" :title="$t('m.Container_Name')" min-width="100"></vxe-table-column>
        <vxe-table-column field="IMAGE" :title="$t('m.Container_Image')" min-width="150"></vxe-table-column>
        <vxe-table-column field="CPU %" :title="$t('m.Container_CPU')" min-width="20"></vxe-table-column>
        <vxe-table-column field="MEM USAGE / LIMIT" :title="$t('m.Container_MEM')" min-width="150"></vxe-table-column>
        <vxe-table-column field="MEM %" :title="$t('m.Container_MEM_Usage')" min-width="20"></vxe-table-column>
        <vxe-table-column field="NET I/O" :title="$t('m.Container_NET')" min-width="150"></vxe-table-column>
        <vxe-table-column field="BLOCK I/O" :title="$t('m.Container_BLOCK')" min-width="100"></vxe-table-column>

        <vxe-table-column field="COMMAND" :title="$t('m.Container_Command')" show-overflow></vxe-table-column>
        <vxe-table-column field="PORTS" :title="$t('m.Container_Ports')" show-overflow>
          <template v-slot="{ row }">
            <span>{{ row.PORTS }}</span>
          </template>
        </vxe-table-column>
        <vxe-table-column field="CREATED" :title="$t('m.Container_Created')" min-width="100"></vxe-table-column>
        <vxe-table-column field="STATUS" :title="$t('m.Container_Status')" min-width="150">
          <template v-slot="{ row }">
            <span>{{ row.STATUS }}</span>
            <p></p>
            <el-tag effect="dark" color="#2d8cf0 " v-if="row.STATUS.startsWith('Up')">Up</el-tag>
            <el-tag effect="dark" color="#f90" v-else-if="row.STATUS.startsWith('Created')">Created</el-tag>
            <el-tag effect="dark" color="#ed3f14" v-else-if="row.STATUS.startsWith('Exited')">Exited</el-tag>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="150" :title="$t('m.Option')">
          <template v-slot="{ row }">
            <el-button
              v-for="action in ['start', 'stop', 'restart', 'pull']"
              :key="action"
              type="text"
              size="mini"
              @click="editContainer(row['CONTAINER ID'], row['NAME'], action, false)"
            >{{ action.charAt(0).toUpperCase() + action.slice(1) }}</el-button>
          </template>
        </vxe-table-column>
      </vxe-table>
    </el-card>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import browserDetector from "browser-detect";
const InfoCard = () => import("@/components/admin/infoCard.vue");
import api from "@/common/api";
import Avatar from "vue-avatar";
import myMessage from "@/common/message";

export default {
  name: "dashboard",
  components: {
    InfoCard,
    Avatar,
  },
  data() {
    return {
      infoData: {
        userNum: 0,
        recentContestNum: 0,
        todayJudgeNum: 0,
      },
      generalInfo: {
        backupCores: 0,
        backupPercentCpuLoad: "0%",
        backupPercentMemoryLoad: "0%",
        backupService: [],
        nacos: {},
      },
      judgeInfo: [],
      session: {},
      dockerInfo: [],
      dockerLoading: false,
      judgeLoading: false,
    };
  },
  mounted() {
    this.refreshJudgeServerList();
    this.refreshGeneralSystemInfo();
    this.refreshDockerServerList();
    // 每10秒刷新后台服务的情况
    this.intervalId = setInterval(() => {
      this.refreshGeneralSystemInfo();
    }, 10000);
    api.admin_getDashboardInfo().then(
      (resp) => {
        this.infoData = resp.data.data;
      },
      () => {}
    );

    api.getSessions(this.userInfo.uid).then(
      (resp) => {
        this.session = resp.data.data;
      },
      () => {}
    );
  },
  methods: {
    refreshJudgeServerList() {
      this.judgeLoading = true;
      api.getJudgeServer().then(
        (res) => {
          this.judgeInfo = res.data.data;
          this.judgeLoading = false;
        },
        (err) => {
          clearInterval(this.intervalId);
          this.judgeLoading = false;
        }
      );
    },
    refreshGeneralSystemInfo() {
      api.admin_getGeneralSystemInfo().then(
        (res) => {
          this.generalInfo = res.data.data;
          this.generalInfo.backupService[0]["backupCores"] =
            this.generalInfo.backupCores;
          this.generalInfo.backupService[0]["backupPercentCpuLoad"] =
            this.generalInfo.backupPercentCpuLoad;
          this.generalInfo.backupService[0]["backupPercentMemoryLoad"] =
            this.generalInfo.backupPercentMemoryLoad;
        },
        () => {
          clearInterval(this.intervalId);
        }
      );
    },
    refreshDockerServerList() {
      this.dockerLoading = true;
      api.admin_getDockerServer().then(
        (res) => {
          this.dockerInfo = res.data.data;
          this.dockerLoading = false;
        },
        (err) => {
          clearInterval(this.intervalId);
          this.dockerLoading = false;
        }
      );
    },
    editContainer(containerId, containerName, action, isJudge, serverIp) {
      this.$confirm(
        "确定对容器 [ " +
          containerName +
          " ] 要执行 [ " +
          action +
          " ] 操作吗？",
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      ).then(() => {
        api.admin_setDockerConfig(containerId, action, serverIp, isJudge).then(
          (res) => {
            myMessage.success("success");
            if (isJudge) {
              this.refreshJudgeServerList();
            } else {
              this.refreshDockerServerList();
            }
          },
          () => {
            myMessage.error("error");
          }
        );
      });
    },
  },
  computed: {
    ...mapGetters(["userInfo", "isSuperAdmin", "isProblemAdmin"]),
    https() {
      return document.URL.slice(0, 5) === "https";
    },
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
  beforeRouteLeave(to, from, next) {
    clearInterval(this.intervalId);
    next();
  },
};
</script>

<style scoped>
.admin-info {
  margin-bottom: 20px;
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
