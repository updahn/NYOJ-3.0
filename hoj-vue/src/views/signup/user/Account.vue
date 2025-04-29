<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ $t("m.Basic_Info") }}</span>
      </div>
      <User :profile.sync="userInfo"></User>
    </el-card>

    <el-card style="margin-top: 15px" v-if="isCoachAdmin">
      <div slot="header">
        <span class="panel-title home-title">{{ $t("m.Invoice_Info") }}</span>
      </div>
    </el-card>

    <el-card style="margin-top: 15px" v-if="isCoachAdmin">
      <div slot="header">
        <span class="panel-title home-title">{{ $t("m.Mailing_Info") }}</span>
      </div>
    </el-card>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
const User = () => import("@/components/signup/User");

export default {
  name: "UserAccount",
  components: {
    User,
  },
  data() {
    return {
      profile: {},
    };
  },
  methods: {},
  computed: {
    ...mapGetters(["isCoachAdmin", "userInfo"]),
  },
};
</script>

<style scoped>
</style>
