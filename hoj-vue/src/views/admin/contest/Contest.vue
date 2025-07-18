<template>
  <div class="view">
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ title }}</span>
      </div>
      <el-form label-position="top">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item :label="$t('m.Contest_Auth')" required>
              <el-radio-group v-model="contest.auth">
                <el-radio :label="0">{{ $t('m.Public') }}</el-radio>
                <el-radio :label="1">{{ $t('m.Private') }}</el-radio>
                <el-radio :label="2">{{ $t('m.Protected') }}</el-radio>
                <el-radio :label="3">{{ $t('m.Official') }}</el-radio>
                <el-radio :label="4">{{ $t('m.Synchronous') }}</el-radio>
                <el-radio :label="5">{{ $t('m.Examination') }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('m.Contest_Title')" required>
              <el-input v-model="contest.title" :placeholder="$t('m.Contest_Title')"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('m.Contest_Description')" required>
              <Editor :value.sync="contest.description"></Editor>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Contest_Start_Time')" required>
              <el-date-picker
                style="width: 50%;"
                v-model="contest.startTime"
                @change="changeDuration"
                type="datetime"
                :placeholder="$t('m.Contest_Start_Time')"
              ></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Contest_End_Time')" required>
              <el-date-picker
                style="width: 50%;"
                v-model="contest.endTime"
                @change="changeDuration"
                type="datetime"
                :placeholder="$t('m.Contest_End_Time')"
              ></el-date-picker>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Contest_Duration')" required>
              <el-input style="width: 50%;" v-model="durationText" disabled></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20" v-if="contest.auth != CONTEST_TYPE.EXAMINATION">
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Contest_Rule_Type')" required>
              <el-radio
                class="radio"
                v-model="contest.type"
                :label="0"
                @change="setSealRankTimeDefaultValue"
                :disabled="disableRuleType"
              >ACM</el-radio>
              <el-radio
                class="radio"
                v-model="contest.type"
                :label="1"
                :disabled="disableRuleType"
                @change="setSealRankTimeDefaultValue"
              >OI</el-radio>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.OI_Rank_Score_Type')" v-show="contest.type == 1">
              <el-radio
                class="radio"
                v-model="contest.oiRankScoreType"
                label="Recent"
              >{{ $t('m.OI_Rank_Score_Type_Recent') }}</el-radio>
              <el-radio
                class="radio"
                v-model="contest.oiRankScoreType"
                label="Highest"
              >{{ $t('m.OI_Rank_Score_Type_Highest') }}</el-radio>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20" v-if="contest.auth != CONTEST_TYPE.EXAMINATION">
          <el-col :md="8" :xs="24" v-if="contest.sealRank">
            <el-form-item :label="$t('m.Timeliness_Of_Rank')" required>
              <el-switch
                v-model="contest.sealRank"
                active-color="#13ce66"
                :active-text="$t('m.Seal_Time_Rank')"
                :inactive-text="$t('m.Real_Time_Rank')"
              ></el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="24" :xs="24" v-else>
            <el-form-item :label="$t('m.Timeliness_Of_Rank')" required>
              <el-switch
                v-model="contest.sealRank"
                active-color="#13ce66"
                :active-text="$t('m.Seal_Time_Rank')"
                :inactive-text="$t('m.Real_Time_Rank')"
                @change="setSealRankTimeDefaultValue"
              ></el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item
              :label="$t('m.Seal_Rank_Time')"
              :required="contest.sealRank"
              v-show="contest.sealRank"
            >
              <el-select style="width: 50%;" v-model="seal_rank_time">
                <el-option
                  :label="$t('m.Contest_Seal_Half_Hour')"
                  :value="0"
                  :disabled="contest.duration < 1800"
                ></el-option>
                <el-option
                  :label="$t('m.Contest_Seal_An_Hour')"
                  :value="1"
                  :disabled="contest.duration < 3600"
                ></el-option>
                <el-option :label="$t('m.Contest_Seal_All_Hour')" :value="2"></el-option>
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Auto_Real_Rank')" required v-show="contest.sealRank">
              <el-switch
                v-model="contest.autoRealRank"
                :active-text="$t('m.Real_Rank_After_Contest')"
                :inactive-text="$t('m.Seal_Rank_After_Contest')"
              ></el-switch>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :md="8" :xs="24" v-if="contest.auth != CONTEST_TYPE.EXAMINATION">
            <el-form-item :label="$t('m.Contest_Outside_ScoreBoard')" required>
              <el-switch
                v-model="contest.openRank"
                :active-text="$t('m.Open')"
                :inactive-text="$t('m.Close')"
              ></el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Allow_Submission_After_The_Contest_Ends')" required>
              <el-switch
                v-model="contest.allowEndSubmit"
                :active-text="$t('m.Open')"
                :inactive-text="$t('m.Close')"
              ></el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24" v-if="contest.auth != CONTEST_TYPE.EXAMINATION">
            <el-form-item :label="$t('m.Print_Func')" required>
              <el-switch
                v-model="contest.openPrint"
                :active-text="$t('m.Support_Offline_Print')"
                :inactive-text="$t('m.Not_Support_Print')"
              ></el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24" v-if="contest.auth != CONTEST_TYPE.EXAMINATION">
            <el-form-item :label="$t('m.BoxFile_Func')" required>
              <el-switch
                v-model="contest.openFile"
                active-color="#13ce66"
                :active-text="$t('m.Support_BoxFile')"
                :inactive-text="$t('m.Not_Support_BoxFile')"
              ></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-show="contest.openFile">
            <el-tag
              :key="index"
              v-for="(file, index) in contest.fileConfigList"
              closable
              :color="'#409eff'"
              effect="dark"
              :disable-transitions="false"
              @close="removeBoxFile(file)"
              size="medium"
              class="filter-item"
            >{{ file.hint }}</el-tag>
            <el-tag
              v-if="contest.fileConfigList"
              :key="contest.fileConfigList.length"
              :color="'#409eff'"
              effect="dark"
              :disable-transitions="false"
              size="medium"
              class="filter-item"
              @close="switchToAdmin"
            >
              <el-tooltip :content="$t('m.Edit_BoxFile')" placement="top">
                <i class="el-icon-plus" @click="switchToAdmin"></i>
              </el-tooltip>
            </el-tag>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item :label="$t('m.Rank_Show_Name')" required>
              <el-radio-group v-model="contest.rankShowName">
                <el-radio label="username">
                  {{
                  $t('m.Show_Username')
                  }}
                </el-radio>
                <el-radio label="nickname">
                  {{
                  $t('m.Show_Nickname')
                  }}
                </el-radio>
                <el-radio label="realname">
                  {{
                  $t('m.Show_Realname')
                  }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col>
            <el-form-item :label="$t('m.Star_User_UserName')" required>
              <el-tag
                v-for="username in contest.starAccount"
                closable
                :close-transition="false"
                :key="username"
                type="warning"
                size="medium"
                @close="removeStarUser(username)"
                style="margin-right: 7px; margin-top: 4px"
              >{{ username }}</el-tag>
              <el-input
                v-if="inputVisible"
                size="medium"
                class="input-new-star-user"
                v-model="starUserInput"
                :trigger-on-focus="true"
                @keyup.enter.native="addStarUser"
                @blur="addStarUser"
              ></el-input>
              <el-tooltip effect="dark" :content="$t('m.Add')" placement="top" v-else>
                <el-button
                  class="button-new-tag"
                  size="small"
                  @click="inputVisible = true"
                  icon="el-icon-plus"
                ></el-button>
              </el-tooltip>
            </el-form-item>
          </el-col>

          <el-col
            :md="8"
            :xs="24"
            v-if="contest.auth != CONTEST_TYPE.PUBLIC && contest.auth != CONTEST_TYPE.OFFICIAL"
          >
            <el-form-item
              :label="$t('m.Contest_Password')"
              v-show="contest.auth != CONTEST_TYPE.PUBLIC && contest.auth != CONTEST_TYPE.OFFICIAL"
              :required="contest.auth != CONTEST_TYPE.PUBLIC && contest.auth != CONTEST_TYPE.OFFICIAL"
            >
              <el-input
                style="width: 50%;"
                v-model="contest.pwd"
                :placeholder="$t('m.Contest_Password')"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col
            :md="8"
            :xs="24"
            v-if="contest.auth != CONTEST_TYPE.PUBLIC && contest.auth != CONTEST_TYPE.SYNCHRONOUS"
          >
            <el-form-item
              :label="$t('m.Account_Limit')"
              v-show="contest.auth != CONTEST_TYPE.PUBLIC && contest.auth != CONTEST_TYPE.SYNCHRONOUS"
              :required="contest.auth != CONTEST_TYPE.PUBLIC && contest.auth != CONTEST_TYPE.SYNCHRONOUS"
            >
              <el-switch v-model="contest.openAccountLimit"></el-switch>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <template v-if="contest.openAccountLimit">
            <el-form :model="formRule">
              <el-col :md="8" :xs="24">
                <el-form-item :label="$t('m.Prefix')" prop="prefix">
                  <el-input style="width: 50%;" v-model="formRule.prefix" placeholder="Prefix"></el-input>
                </el-form-item>
              </el-col>
              <el-col :md="8" :xs="24">
                <el-form-item :label="$t('m.Suffix')" prop="suffix">
                  <el-input style="width: 50%;" v-model="formRule.suffix" placeholder="Suffix"></el-input>
                </el-form-item>
              </el-col>
              <el-col :md="8" :xs="24">
                <el-form-item :label="$t('m.Start_Number')" prop="number_from">
                  <el-input-number style="width: 50%" v-model="formRule.number_from"></el-input-number>
                </el-form-item>
              </el-col>
              <el-col :md="8" :xs="24">
                <el-form-item :label="$t('m.End_Number')" prop="number_to">
                  <el-input-number style="width: 50%" v-model="formRule.number_to"></el-input-number>
                </el-form-item>
              </el-col>

              <div class="userPreview" v-if="formRule.number_from <= formRule.number_to">
                {{ $t('m.The_allowed_account_will_be') }}
                {{ formRule.prefix + formRule.number_from + formRule.suffix }},
                <span
                  v-if="formRule.number_from + 1 < formRule.number_to"
                >
                  {{
                  formRule.prefix +
                  (formRule.number_from + 1) +
                  formRule.suffix +
                  '...'
                  }}
                </span>
                <span
                  v-if="formRule.number_from + 1 <= formRule.number_to"
                >{{ formRule.prefix + formRule.number_to + formRule.suffix }}</span>
              </div>

              <el-col :md="24" :xs="24">
                <el-form-item :label="$t('m.Extra_Account')" prop="prefix">
                  <el-input
                    type="textarea"
                    :placeholder="$t('m.Extra_Account_Tips')"
                    :rows="8"
                    v-model="formRule.extra_account"
                  ></el-input>
                </el-form-item>
              </el-col>
            </el-form>
          </template>
        </el-row>

        <el-row :gutter="20">
          <!-- 正式赛配置 -->
          <template v-if="contest.auth == CONTEST_TYPE.OFFICIAL">
            <el-col :md="8" :xs="24">
              <el-form-item :label="$t('m.Sign_Start_Time')" required>
                <el-date-picker
                  style="width: 50%;"
                  v-model="contest.signStartTime"
                  @change="changeSignDuration"
                  type="datetime"
                  :placeholder="$t('m.Sign_Start_Time')"
                ></el-date-picker>
              </el-form-item>
            </el-col>
            <!-- 报名的结束时间不晚于比赛的结束时间 -->
            <el-col :md="8" :xs="24">
              <el-form-item :label="$t('m.Sign_End_Time')" required>
                <el-date-picker
                  style="width: 50%;"
                  v-model="contest.signEndTime"
                  @change="changeSignDuration"
                  type="datetime"
                  :placeholder="$t('m.Sign_End_Time')"
                ></el-date-picker>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item :label="$t('m.Sign_Duration')" required>
                <el-input style="width: 50%;" v-model="signdurationText" disabled></el-input>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item :label="$t('m.Modify_endTime')" required>
                <el-date-picker
                  style="width: 50%;"
                  v-model="contest.modifyEndTime"
                  type="datetime"
                  :placeholder="$t('m.Modify_endTime')"
                ></el-date-picker>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item :label="$t('m.Max_Participants')" required>
                <el-input-number
                  style="width: 50%;"
                  v-model="contest.maxParticipants"
                  :min="1"
                  :max="3"
                ></el-input-number>
              </el-form-item>
            </el-col>
          </template>

          <!-- 同步赛配置 -->
          <el-col :span="24" v-if="contest.auth == CONTEST_TYPE.SYNCHRONOUS">
            <div style="margin-bottom: 10px">
              <el-button
                type="primary"
                icon="el-icon-plus"
                circle
                @click="insertEvent2(-1)"
                size="small"
              ></el-button>
              <el-button
                type="danger"
                icon="el-icon-delete"
                circle
                @click="removeEvent2()"
                size="small"
              ></el-button>
            </div>
            <vxe-table
              border
              ref="xAwardTable2"
              :data="contest.synchronousConfigList"
              :edit-config="{ trigger: 'click', mode: 'cell' }"
              align="center"
              @edit-closed="editClosedEvent2"
              style="margin-bottom: 15px"
            >
              <vxe-table-column type="checkbox" width="60"></vxe-table-column>
              <vxe-table-column
                field="school"
                min-width="150"
                :title="$t('m.Synchronous_School')"
                :edit-render="{ name: 'input', attrs: { type: 'text' } }"
              ></vxe-table-column>
              <vxe-table-column
                field="link"
                min-width="150"
                :title="$t('m.Synchronous_Link')"
                :edit-render="{ name: 'input', attrs: { type: 'text' } }"
              ></vxe-table-column>
              <vxe-table-column
                field="username"
                min-width="150"
                :title="$t('m.Synchronous_Username')"
                :edit-render="{ name: 'input', attrs: { type: 'text' } }"
              ></vxe-table-column>
              <vxe-table-column
                field="password"
                min-width="150"
                :title="$t('m.Synchronous_Password')"
                :edit-render="{ name: 'input', attrs: { type: 'text' } }"
              ></vxe-table-column>
            </vxe-table>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Contest_Award')" required>
              <el-select
                style="width: 50%;"
                v-model="contest.awardType"
                @change="contestAwardTypeChange"
              >
                <el-option :label="$t('m.Contest_Award_Null')" :value="0"></el-option>
                <el-option
                  v-if="contest.auth != CONTEST_TYPE.EXAMINATION"
                  :label="$t('m.Contest_Award_Set_Proportion')"
                  :value="1"
                ></el-option>
                <el-option
                  v-if="contest.auth != CONTEST_TYPE.EXAMINATION"
                  :label="$t('m.Contest_Award_Set_Number')"
                  :value="2"
                ></el-option>
                <el-option
                  v-if="contest.auth == CONTEST_TYPE.EXAMINATION"
                  :label="$t('m.Contest_Award_Set_Score')"
                  :value="3"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="contest.awardType != 0">
            <div style="margin-bottom:10px">
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
              :data="contest.awardConfigList"
              :edit-config="{trigger: 'click', mode: 'cell'}"
              :sort-config="{trigger: 'cell', defaultSort: {field: 'priority', order: 'asc'}, orders: ['desc', 'asc', null]}"
              align="center"
              @edit-closed="editClosedEvent"
              style="margin-bottom:15px"
            >
              <vxe-table-column type="checkbox" width="60"></vxe-table-column>
              <vxe-table-column
                field="priority"
                width="100"
                :title="$t('m.Contest_Award_Priority')"
                :edit-render="{name: 'input', attrs: {type: 'number'}}"
                sortable
              ></vxe-table-column>
              <vxe-table-column
                field="name"
                min-width="150"
                :title="$t('m.Contest_Award_Name')"
                :edit-render="{name: 'input', attrs: {type: 'text'}}"
              ></vxe-table-column>
              <vxe-table-column
                field="background"
                min-width="150"
                :title="$t('m.Contest_Award_Background')"
              >
                <template v-slot="{ row }">
                  <el-color-picker v-model="row.background" size="small"></el-color-picker>
                </template>
              </vxe-table-column>
              <vxe-table-column field="color" min-width="150" :title="$t('m.Contest_Award_Color')">
                <template v-slot="{ row }">
                  <el-color-picker v-model="row.color" size="small"></el-color-picker>
                </template>
              </vxe-table-column>

              <vxe-table-column field="show" min-width="150" :title="$t('m.Contest_Award_Show')">
                <template v-slot="{ row }">
                  <RankBox
                    :name="row.name"
                    :background="row.background"
                    :color="row.color"
                    :num="1"
                  ></RankBox>
                </template>
              </vxe-table-column>

              <vxe-table-column
                field="num"
                min-width="150"
                :title="
                  contest.awardType == 1
                    ? $t('m.Contest_Award_Proportion')
                    : contest.awardType == 2
                    ? $t('m.Contest_Award_Number')
                    : $t('m.Contest_Award_Score')
                "
              >
                <template v-slot="{ row }">
                  <el-input
                    :placeholder="$t('m.Contest_Award_Proportion')"
                    v-model="row.num"
                    size="small"
                    v-if="contest.awardType == 1"
                    type="number"
                  >
                    <template slot="append">%</template>
                  </el-input>
                  <el-input
                    :placeholder="$t('m.Contest_Award_Number')"
                    v-model="row.num"
                    size="small"
                    v-else-if="contest.awardType == 2"
                  ></el-input>
                  <el-input
                    :placeholder="$t('m.Contest_Award_Score')"
                    v-model="row.num"
                    size="small"
                    v-else-if="contest.awardType == 3"
                  ></el-input>
                </template>
              </vxe-table-column>
            </vxe-table>
          </el-col>
        </el-row>
      </el-form>
      <el-button type="primary" @click.native="saveContest">
        {{
        $t('m.Save')
        }}
      </el-button>
    </el-card>
  </div>
</template>

<script>
import api from "@/common/api";
import time from "@/common/time";
import moment from "moment";
import { mapGetters } from "vuex";
import myMessage from "@/common/message";
const Editor = () => import("@/components/admin/Editor.vue");
const RankBox = () => import("@/components/oj/common/RankBox");
import { CONTEST_TYPE } from "@/common/constants";

export default {
  name: "CreateContest",
  components: {
    Editor,
    RankBox,
  },
  data() {
    return {
      title: "Create Contest",
      disableRuleType: false,
      durationText: "", // 比赛时长文本表示
      signdurationText: "", // 报名时长文本显示
      seal_rank_time: 1, // 当开启封榜模式，即实时榜单关闭时，可选择前半小时，前一小时，全程封榜,默认全程封榜
      boxFileList: [], // 文件柜的默认文件
      contest: {
        title: "",
        description: "",
        startTime: "",
        endTime: "",
        duration: 0,
        type: 0,
        pwd: "",
        sealRank: true,
        sealRankTime: "An hour", //封榜时间
        autoRealRank: true,
        auth: 1,
        openPrint: false,
        rankShowName: "username",
        openAccountLimit: false,
        openRank: true,
        allowEndSubmit: true,
        accountLimitRule: "",
        starAccount: [],
        oiRankScoreType: "Recent",
        awardType: 1,
        awardConfigList: [
          {
            priority: 1,
            name: "金牌",
            background: "#e6bf25",
            color: "#fff",
            num: 3,
          },
          {
            priority: 2,
            name: "银牌",
            background: "#b4c0c7",
            color: "#fff",
            num: 5,
          },
          {
            priority: 3,
            name: "铜牌",
            background: "#CD7F32",
            color: "#fff",
            num: 7,
          },
        ],
        openFile: false,
        fileConfigList: [],
        synchronousConfigList: [
          {
            school: "",
            link: "",
            username: "",
            password: "",
          },
        ],
        signStartTime: "",
        signEndTime: "",
        maxParticipants: 0,
        signDuration: 0,
      },
      formRule: {
        prefix: "",
        suffix: "",
        number_from: 0,
        number_to: 10,
        extra_account: "",
      },
      starUserInput: "",
      inputVisible: false,
      CONTEST_TYPE: {},
    };
  },
  mounted() {
    this.CONTEST_TYPE = Object.assign({}, CONTEST_TYPE);
    this.getBoxFileList();
    if (this.$route.name === "admin-edit-contest") {
      this.title = this.$i18n.t("m.Edit_Contest");
      this.disableRuleType = true;
      this.getContestByCid();
    } else if (this.$route.name === "admin-create-contest") {
      this.title = this.$i18n.t("m.Create_Contest");
      this.disableRuleType = false;
    }
  },
  watch: {
    $route() {
      if (this.$route.name === "admin-edit-contest") {
        this.title = this.$i18n.t("m.Edit_Contest");
        this.disableRuleType = true;
        this.getContestByCid();
      } else if (this.$route.name === "admin-create-contest") {
        this.title = this.$i18n.t("m.Create_Contest");
        this.disableRuleType = false;
      }
    },
    // 如果未开启文件柜
    "contest.openFile": function (newVal, oldVal) {
      if (newVal && this.contest.fileConfigList.length === 0) {
        this.getBoxFileList();
      }
    },
    "contest.auth": function (newVal, oldVal) {
      if (newVal == CONTEST_TYPE.EXAMINATION) {
        this.contest.awardType = 0;
        this.contest.type = 5;

        // 全程封榜
        this.seal_rank_time = 2;
      } else {
        this.contest.awardType = 1;
        this.seal_rank_time = 1;
      }
    },
  },
  computed: {
    ...mapGetters(["userInfo"]),
  },
  methods: {
    getContestByCid() {
      let cid = this.$route.params.contestID;
      if (cid) {
        api
          .admin_getContest(cid)
          .then((res) => {
            let data = res.data.data;
            this.contest = data;
            this.changeDuration();
            this.changeSignDuration();
            // 封榜时间转换
            let halfHour = moment(this.contest.endTime)
              .subtract(1800, "seconds")
              .toString();
            let oneHour = moment(this.contest.endTime)
              .subtract(3600, "seconds")
              .toString();
            let allHour = moment(this.contest.startTime).toString();
            let sealRankTime = moment(this.contest.sealRankTime).toString();
            switch (sealRankTime) {
              case halfHour:
                this.seal_rank_time = 0;
                break;
              case oneHour:
                this.seal_rank_time = 1;
                break;
              case allHour:
                this.seal_rank_time = 2;
                break;
            }
            if (this.contest.accountLimitRule) {
              this.formRule = this.changeStrToAccountRule(
                this.contest.accountLimitRule
              );
            }
          })
          .catch(() => {});
      }
    },

    saveContest() {
      if (!this.contest.title) {
        myMessage.error(
          this.$i18n.t("m.Contest_Title") + " " + this.$i18n.t("m.is_required")
        );
        return;
      }
      if (!this.contest.description) {
        myMessage.error(
          this.$i18n.t("m.Contest_Description") +
            " " +
            this.$i18n.t("m.is_required")
        );
        return;
      }
      if (!this.contest.startTime) {
        myMessage.error(
          this.$i18n.t("m.Contest_Start_Time") +
            " " +
            this.$i18n.t("m.is_required")
        );
        return;
      }
      if (!this.contest.endTime) {
        myMessage.error(
          this.$i18n.t("m.Contest_End_Time") +
            " " +
            this.$i18n.t("m.is_required")
        );
        return;
      }
      if (!this.contest.duration || this.contest.duration <= 0) {
        myMessage.error(this.$i18n.t("m.Contest_Duration_Check"));
        return;
      }

      if (this.contest.auth == this.CONTEST_TYPE.OFFICIAL) {
        if (!this.contest.signDuration || this.contest.signDuration <= 0) {
          myMessage.error(this.$i18n.t("m.Sign_Duration_Check"));
          return;
        }
        if (this.contest.signEndTime > this.contest.endTime) {
          myMessage.error(this.$i18n.t("m.Sign_EndTime_Check"));
          return;
        }
        if (this.contest.signEndTime > this.contest.endTime) {
          myMessage.error(this.$i18n.t("m.Sign_EndTime_Check"));
          return;
        }
        if (!this.contest.modifyEndTime) {
          myMessage.error(
            this.$i18n.t("m.Modify_endTime") +
              " " +
              this.$i18n.t("m.is_required")
          );
          return;
        }
      }

      if (
        this.contest.auth != this.CONTEST_TYPE.PUBLIC &&
        this.contest.auth != this.CONTEST_TYPE.OFFICIAL &&
        this.contest.auth != this.CONTEST_TYPE.SYNCHRONOUS &&
        this.contest.auth != this.CONTEST_TYPE.EXAMINATION &&
        !this.contest.pwd
      ) {
        myMessage.error(
          this.$i18n.t("m.Contest_Password") +
            " " +
            this.$i18n.t("m.is_required")
        );
        return;
      }
      if (this.contest.openAccountLimit) {
        this.contest.accountLimitRule = this.changeAccountRuleToStr(
          this.formRule
        );
      }

      let funcName =
        this.$route.name === "admin-edit-contest"
          ? "admin_editContest"
          : "admin_createContest";

      switch (this.seal_rank_time) {
        case 0: // 结束前半小时
          this.contest.sealRankTime = moment(this.contest.endTime).subtract(
            1800,
            "seconds"
          );
          break;
        case 1: // 结束前一小时
          this.contest.sealRankTime = moment(this.contest.endTime).subtract(
            3600,
            "seconds"
          );
          break;
        case 2: // 全程
          this.contest.sealRankTime = moment(this.contest.startTime);
      }
      let data = Object.assign({}, this.contest);
      if (funcName === "admin_createContest") {
        data["uid"] = this.userInfo.uid;
        data["author"] = this.userInfo.username;
      }

      api[funcName](data)
        .then((res) => {
          myMessage.success("success");
          this.$router.push({
            name: "admin-contest-list",
            query: { refresh: "true" },
          });
        })
        .catch(() => {});
    },
    changeDuration() {
      let start = this.contest.startTime;
      let end = this.contest.endTime;
      let durationMS = time.durationMs(start, end);
      if (durationMS < 0) {
        this.durationText = this.$i18n.t("m.Contets_Time_Check");
        this.contest.duration = 0;
        return;
      }
      if (start != "" && end != "") {
        this.durationText = time.formatSpecificDuration(start, end);
        this.contest.duration = durationMS;
      }
    },
    changeAccountRuleToStr(formRule) {
      let result =
        "<prefix>" +
        formRule.prefix +
        "</prefix><suffix>" +
        formRule.suffix +
        "</suffix><start>" +
        formRule.number_from +
        "</start><end>" +
        formRule.number_to +
        "</end><extra>" +
        formRule.extra_account +
        "</extra>";
      return result;
    },
    changeStrToAccountRule(value) {
      let reg =
        "<prefix>([\\s\\S]*?)</prefix><suffix>([\\s\\S]*?)</suffix><start>([\\s\\S]*?)</start><end>([\\s\\S]*?)</end><extra>([\\s\\S]*?)</extra>";
      let re = RegExp(reg, "g");
      let tmp = re.exec(value);
      return {
        prefix: tmp[1],
        suffix: tmp[2],
        number_from: tmp[3],
        number_to: tmp[4],
        extra_account: tmp[5],
      };
    },

    addStarUser() {
      this.starUserInput = this.starUserInput.replace(/(^\s*)|(\s*$)/g, "");
      if (this.starUserInput) {
        for (var i = 0; i < this.contest.starAccount.length; i++) {
          if (this.contest.starAccount[i] == this.starUserInput) {
            myMessage.warning(this.$i18n.t("m.Add_Star_User_Error"));
            this.starUserInput = "";
            return;
          }
        }
        this.contest.starAccount.push(this.starUserInput);
        this.inputVisible = false;
        this.starUserInput = "";
      }
    },

    // 根据UserName 从打星用户列表中移除
    removeStarUser(username) {
      this.contest.starAccount.splice(
        this.contest.starAccount.map((item) => item.name).indexOf(username),
        1
      );
    },

    setSealRankTimeDefaultValue() {
      if (this.contest.sealRank == true) {
        if (this.contest.type == 0) {
          // ACM比赛开启封榜 默认为一小时,如果比赛时长小于一小时，则默认为全程
          if (this.contest.duration < 3600) {
            this.seal_rank_time = 2;
          } else {
            this.seal_rank_time = 1;
          }
        } else {
          // OI比赛开启封榜 默认全程
          this.seal_rank_time = 2;
        }
      }
    },

    contestAwardTypeChange() {
      if (this.contest.awardType != 0 && !this.contest.awardConfigList.length) {
        this.contest.awardConfigList = [
          {
            priority: 1,
            name: "金牌",
            background: "#e6bf25",
            color: "#fff",
            num: 10,
          },
          {
            priority: 2,
            name: "银牌",
            background: "#b4c0c7",
            color: "#fff",
            num: 20,
          },
          {
            priority: 3,
            name: "铜牌",
            background: "#CD7F32",
            color: "#fff",
            num: 30,
          },
        ];
      }
    },

    async insertEvent(row) {
      let record = {
        priority: this.contest.awardConfigList.length + 1,
        name: "name",
        background: "#ededed",
        color: "#666",
        num: 0,
      };
      let { row: newRow } = await this.$refs.xAwardTable.insertAt(record, row);
      const { insertRecords } = this.$refs.xAwardTable.getRecordset();
      this.contest.awardConfigList =
        this.contest.awardConfigList.concat(insertRecords);
      await this.$refs.xAwardTable.setActiveCell(newRow, "name");
    },

    async removeEvent() {
      this.$refs.xAwardTable.removeCheckboxRow();
      let removeRecords = this.$refs.xAwardTable.getRemoveRecords();
      function getDifferenceSetB(arr1, arr2, typeName) {
        return Object.values(
          arr1.concat(arr2).reduce((acc, cur) => {
            if (
              acc[cur[typeName]] &&
              acc[cur[typeName]][typeName] === cur[typeName]
            ) {
              delete acc[cur[typeName]];
            } else {
              acc[cur[typeName]] = cur;
            }
            return acc;
          }, {})
        );
      }
      this.contest.awardConfigList = getDifferenceSetB(
        this.contest.awardConfigList,
        removeRecords,
        "_XID"
      );
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

    async insertEvent2(row) {
      let record = {
        school: "school",
        link: "",
        authorization: "",
      };
      let { row: newRow } = await this.$refs.xAwardTable2.insertAt(record, row);
      const { insertRecords } = this.$refs.xAwardTable2.getRecordset();
      this.contest.synchronousConfigList =
        this.contest.synchronousConfigList.concat(insertRecords);
      await this.$refs.xAwardTable2.setActiveCell(newRow, "school");
    },
    async removeEvent2() {
      this.$refs.xAwardTable2.removeCheckboxRow();
      let removeRecords = this.$refs.xAwardTable2.getRemoveRecords();
      function getDifferenceSetB(arr1, arr2, typeName) {
        return Object.values(
          arr1.concat(arr2).reduce((acc, cur) => {
            if (
              acc[cur[typeName]] &&
              acc[cur[typeName]][typeName] === cur[typeName]
            ) {
              delete acc[cur[typeName]];
            } else {
              acc[cur[typeName]] = cur;
            }
            return acc;
          }, {})
        );
      }
      this.contest.synchronousConfigList = getDifferenceSetB(
        this.contest.synchronousConfigList,
        removeRecords,
        "_XID"
      );
    },
    editClosedEvent2({ row, column }) {
      let xTable = this.$refs.xAwardTable2;
      let field = column.property;
      // 判断单元格值是否被修改
      if (xTable.isUpdateByRow(row, field)) {
        setTimeout(() => {
          // 局部更新单元格为已保存状态
          this.$refs.xAwardTable2.reloadRow(row, null, field);
        }, 300);
      }
    },

    getBoxFileList() {
      api.getBoxFileList().then((res) => {
        let fileList = res.data.data;
        this.boxFileList = fileList;
        this.contest.fileConfigList = fileList;
      });
    },
    removeBoxFile(file) {
      this.contest.fileConfigList.splice(
        this.contest.fileConfigList.indexOf(file),
        1
      );
    },
    switchToAdmin() {
      // 管理文件柜详情页
      this.$router.push({ name: "admin-file" });
    },
    changeSignDuration() {
      let start = this.contest.signStartTime;
      let end = this.contest.signEndTime;
      let durationMS = time.durationMs(start, end);
      if (durationMS < 0) {
        this.signdurationText = this.$i18n.t("m.Contets_Time_Check");
        this.contest.signDuration = 0;
        return;
      }
      if (start != "" && end != "") {
        this.signdurationText = time.formatSpecificDuration(start, end);
        this.contest.signDuration = durationMS;
      }
    },
  },
};
</script>
<style scoped>
.userPreview {
  padding-left: 10px;
  padding-top: 20px;
  padding-bottom: 20px;
  color: red;
  font-size: 16px;
  margin-bottom: 10px;
}
.input-new-star-user {
  width: 200px;
}
.filter-item {
  margin-right: 1em;
  margin-top: 0.5em;
  font-size: 13px;
}
.filter-item:hover {
  cursor: pointer;
}
</style>
