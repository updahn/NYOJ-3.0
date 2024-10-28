<template>
  <div class="contest-full-screen-body" style="height: 100%;">
    <transition>
      <router-view></router-view>
    </transition>
  </div>
</template>
<script>
export default {
  name: "ContestDetails",
};
</script>