<template>
  <div>
    <component :is="currentView"></component>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { RULE_TYPE } from "@/common/constants";
const ACMContestRank = () => import("./ACMContestRank.vue");
const OIContestRank = () => import("./OIContestRank.vue");
const ExamContestRank = () => import("./ExamContestRank.vue");

const NullComponent = {
  name: "null-component",
  template: "<div></div>",
};

export default {
  name: "contest-rank",
  components: {
    ACMContestRank,
    OIContestRank,
    ExamContestRank,
  },
  beforeCreate() {
    if (this.$store.state.contest.contestProblems.length === 0) {
      this.$store.dispatch("getContestProblems");
    }
  },
  computed: {
    ...mapGetters(["contestRuleType"]),
    currentView() {
      if (this.contestRuleType === null) {
        return "NullComponent";
      }
      return this.contestRuleType === RULE_TYPE.ACM
        ? "ACMContestRank"
        : this.contestRuleType === RULE_TYPE.OI
        ? "OIContestRank"
        : "ExamContestRank";
    },
  },
  beforeRouteLeave(to, from, next) {
    this.$store.commit("changeContestItemVisible", { menu: true });
    next();
  },
};
</script>
<style></style>
