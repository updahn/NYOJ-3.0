<template>
  <div>
    <el-form>
      <el-form-item :label="$t('m.Remote_OJ')">
        <el-select v-model="otherOJName" style="width:100%;">
          <el-option
            :label="remoteOj.name"
            :value="remoteOj.key"
            v-for="(remoteOj, index) in REMOTE_OJ"
            :key="index"
          ></el-option>
        </el-select>
      </el-form-item>

      <p style="text-align: left; white-space: pre-wrap; color: #66b1ff;">
        <span style="font-size: 18px;">{{ $t('m.Remote_Tips_' + otherOJName) + ' '}}</span>
        <el-popover placement="right" trigger="hover">
          <p style="font-size: 14px;">{{ $t('m.Remote_Tips1') }}</p>
          <p style="font-size: 14px;">{{ $t('m.Remote_Tips2') }}</p>
          <p style="font-size: 14px;">{{ $t('m.Remote_Tips3') }}</p>
          <i slot="reference" class="el-icon-question"></i>
        </el-popover>
      </p>
      <el-form-item :label="$t('m.Problem_ID')" required>
        <el-input v-model="otherOJProblemId" size="small"></el-input>
        <div class="userPreview">
          <span
            v-if="problemIdList.length"
          >{{ $t('m.The_actual_problemId_will_be') }} {{ problemIdList.join(',') }}</span>
          <span v-else>{{ errorMessage }}</span>
        </div>
      </el-form-item>

      <el-form-item
        v-if="contestId"
        :label="$t('m.Enter_The_Problem_Display_ID_in_the_Contest')"
        required
      >
        <el-input v-model="displayId" size="small"></el-input>
      </el-form-item>

      <el-form-item style="text-align:center">
        <el-button
          type="primary"
          icon="el-icon-plus"
          @click="addRemoteOJProblem"
          :loading="addRemoteOJproblemLoading"
        >{{ $t('m.Add') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
<script>
import api from "@/common/api";
import { REMOTE_OJ } from "@/common/constants";
import myMessage from "@/common/message";

export default {
  name: "AddProblemFromRemote",
  props: {
    contestId: {
      type: Number,
      default: null,
    },
  },
  data() {
    return {
      loading: false,
      otherOJProblemId: "",
      otherOJName: "HDU",
      displayId: "",
      REMOTE_OJ: {},
      addRemoteOJproblemLoading: false,
      problemIdList: [],
      errorMessage: "",
    };
  },
  mounted() {
    this.REMOTE_OJ = Object.assign({}, REMOTE_OJ);
  },
  methods: {
    addRemoteOJProblem() {
      if (!this.otherOJProblemId) {
        myMessage.error(this.$i18n.t("m.Problem_ID_is_required"));
        return;
      }

      this.addRemoteOJproblemLoading = true;
      let funcName = "";
      if (this.contestId) {
        funcName = "admin_addContestRemoteOJProblem";
      } else {
        funcName = "admin_addRemoteOJProblem";
      }
      api[funcName](
        this.otherOJName,
        this.otherOJProblemId,
        this.$route.params.groupID,
        this.contestId,
        this.displayId
      ).then(
        (res) => {
          this.addRemoteOJproblemLoading = false;
          this.AddRemoteOJProblemDialogVisible = false;
          myMessage.success(this.$i18n.t("m.Add_Successfully"));
          if (this.contestId) {
            this.$emit("currentChangeProblem");
            this.$emit("handleRemotePage");
          } else {
            this.$emit("handleRemotePage");
            this.$emit("currentChange");
          }
        },
        (err) => {
          this.addRemoteOJproblemLoading = false;
        }
      );
    },
    updateProblemIdList() {
      const inputValue = this.otherOJProblemId.trim();

      // 检查是否是范围格式
      if (inputValue.includes("-")) {
        const [start, end] = inputValue.split("-").map((num) => num.trim());

        if (!this.isValidNumber(start) || !this.isValidNumber(end)) {
          this.errorMessage = this.$i18n.t("m.Range_ID_must_be_valid_number");
          this.problemIdList = [];
          return;
        }

        const startNum = parseInt(start);
        const endNum = parseInt(end);

        if (startNum > endNum) {
          this.errorMessage = this.$i18n.t("m.Range_ID_error");
          this.problemIdList = [];
          return;
        } else {
          this.problemIdList = Array.from(
            { length: endNum - startNum + 1 },
            (_, i) => (startNum + i).toString()
          );
        }
      } else if (inputValue.includes(",")) {
        this.problemIdList = inputValue.split(",").map((num) => num.trim());
      } else {
        this.problemIdList = [inputValue];
      }
    },
    isValidNumber(value) {
      return /^\d+$/.test(value);
    },
  },
};
</script>

<style scoped>
.userPreview {
  padding-left: 10px;
  padding-top: 5px;
  color: red;
  font-size: 16px;
}
</style>
