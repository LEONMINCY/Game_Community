// 文件说明：store/index.js，Pinia用户状态仓库，保存登录用户、令牌和基础资料。
import { defineStore } from "pinia";

export const useUserStore = defineStore("main", {
  state: () => {
    return {
      userInfo: {},
    };
  },
  actions: {
    setUserInfo (data) {
      this.userInfo = data;
    },
  },
  getters: {

  },
});