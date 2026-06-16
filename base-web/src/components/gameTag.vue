<!-- 文件说明：components/gameTag.vue，游戏标签组件，展示社区帖子关联的游戏标签并支持跳转筛选。 -->
<template>
  <span class="tagWrapper">
    <el-image style="width: 20px; height: 20px;vertical-align: middle;" :src="thumbnailURL(src, 80)" lazy />
    <span style="vertical-align: middle; margin-left: 10px">{{ name }}</span>
  </span>
</template>
<script setup>
import { thumbnailURL } from '@/utils/tools'
const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: ''
  }
})


</script>
<style lang="less" scoped>
.tagWrapper {
  background-color: #f3f4f5;
  color: rgb(20, 25, 30);
  font-size: 12px;
  padding: 8px 8px;
  vertical-align: middle;
  border-radius: 5px;
}
</style>
