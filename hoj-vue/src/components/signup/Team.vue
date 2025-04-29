<template>
  <div>
    <el-form :model="teamSign" ref="teamSign">
      <span class="info-title" style="font-weight: bold;">{{ $t("m.Team_Info") }}</span>
      <div class="info">
        <el-row :gutter="20">
          <template v-if="teamSign.participants > 1">
            <el-col :md="8" :xs="24">
              <el-form-item prop="cname" :label="$t('m.Cname')">
                <el-input
                  v-model="teamSign.cname"
                  prefix-icon="el-icon-s-flag"
                  :placeholder="$t('m.Cname')"
                  width="80%"
                  :disabled="true"
                ></el-input>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item prop="ename" :label="$t('m.Ename')">
                <el-input
                  v-model="teamSign.ename"
                  :placeholder="$t('m.Ename')"
                  width="80%"
                  :disabled="true"
                ></el-input>
              </el-form-item>
            </el-col>
            <el-col :offset="8" :xs="24"></el-col>
          </template>

          <el-col :md="8" :xs="24">
            <el-form-item prop="school" :label="$t('m.Team_School')">
              <el-input v-model="teamSign.school" :placeholder="$t('m.Team_School')" width="80%"></el-input>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Signup_Type')">
              <el-select
                v-model="teamSign.type"
                :placeholder="$t('m.Please_SelectQuota_In_QuotaType')"
                style="width:100%"
              >
                <el-option
                  v-for="(item, index) in QUOTA_TYPE_REVERSE"
                  :key="index"
                  :value="parseInt(index)"
                  :label="$t('m.' + item.name)"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <p></p>
      <span class="info-title" style="font-weight: bold;">{{ $t("m.Number_Info") }}</span>
      <div class="info">
        <el-row :gutter="20">
          <el-form-item v-for="(item, index) in teamSign.teamConfig" :key="'sample' + index">
            <Accordion
              :title="(index === 0 ? $t('m.Captain') : $t('m.Participant_') + '：' + index)"
              :index="index"
              :key="'accordion-' + index"
            >
              <!-- 个人信息 -->
              <User :profile.sync="teamSign.teamConfig[index]"></User>
            </Accordion>
          </el-form-item>
        </el-row>
      </div>
    </el-form>
  </div>
</template>

<script>
const User = () => import("@/components/signup/User");
const Accordion = () => import("@/components/admin/Accordion.vue");
import { QUOTA_TYPE_REVERSE } from "@/common/constants";

export default {
  components: {
    User,
    Accordion,
  },
  props: {
    teamSign: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {};
  },
  created() {
    this.QUOTA_TYPE_REVERSE = Object.assign({}, QUOTA_TYPE_REVERSE);
  },
  mounted() {},
  methods: {},
  watch: {
    teamSign(newVal) {
      this.$emit("update:teamSign", newVal);
    },
  },
};
</script>

<style>
.info {
  margin-left: 20px;
  margin-right: 20px;
}
</style>