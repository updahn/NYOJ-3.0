<template>
  <div class="group-training-full-screen-body" style="height: 100%;">
    <transition>
      <router-view></router-view>
    </transition>
  </div>
</template>
<script>
export default {
  name: "GroupTrainingFullScreen",
};
</script>