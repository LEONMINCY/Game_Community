// 文件说明：router/index.js，前端路由配置，维护游客访问、前台页面和后台页面跳转关系。
import { createRouter, createWebHistory } from "vue-router";

import Login from "../views/login/index.vue";
import Home from "../views/home/index.vue";

const routes = [
  {
    path: "/",
    name: "home",
    component: Home,
    redirect: "/forumlist",
    children: [
      {
        path: "forumlist",
        name: "forumlist",
        component: () => import("../views/forumlist/index.vue"),
      },
      {
        path: "forum",
        name: "forum",
        component: () => import("../views/forum/index.vue"),
      },
      {
        path: "userinfo",
        name: "userinfo",
        component: () => import("../views/userinfo/index.vue"),
      },
      {
        path: "games",
        name: "games",
        component: () => import("../views/games/index.vue"),
      },
      {
        path: "rankings",
        name: "rankings",
        component: () => import("../views/rankings/index.vue"),
      },
      {
        path: "news",
        name: "news",
        component: () => import("../views/news/index.vue"),
      },
      {
        path: "search",
        name: "search",
        component: () => import("../views/search/index.vue"),
      },
      {
        path: "ai-assistant",
        name: "aiAssistant",
        component: () => import("../views/aiAssistant/index.vue"),
        meta: { requireAuth: true },
      },
      {
        path: "chat",
        name: "chat",
        component: () => import("../views/chat/index.vue"),
        meta: { requireAuth: true },
      },
      {
        path: "my-posts",
        name: "myPosts",
        component: () => import("../views/myPosts/index.vue"),
        meta: { requireAuth: true },
      },
      {
        path: "wishlist",
        name: "wishlist",
        component: () => import("../views/wishlist/index.vue"),
        meta: { requireAuth: true },
      },
      {
        path: "cart",
        name: "cart",
        component: () => import("../views/cart/index.vue"),
        meta: { requireAuth: true },
      },
      {
        path: "my-orders",
        name: "myOrders",
        component: () => import("../views/myOrders/index.vue"),
        meta: { requireAuth: true },
      },
      {
        path: "privacy-settings",
        name: "privacySettings",
        component: () => import("../views/privacySettings/index.vue"),
        meta: { requireAuth: true },
      },
      {
        path: "/news/detail",
        name: "newsDetail",
        component: () => import("../views/detail/index.vue"),
      },
      {
        path: "/detail",
        redirect: to => ({ path: "/news/detail", query: to.query }),
      },
      {
        path: "/addForum",
        name: "addForum",
        component: () => import("../views/addForum/index.vue"),
        meta: { requireAuth: true },
      },
      {
        path: "/gameDetail",
        name: "gameDetail",
        component: () => import("../views/games/game.vue"),
      },
      {
        path: "/paymentResults",
        name: "paymentResults",
        component: () => import("../views/paymentResults/index.vue"),
        meta: { requireAuth: true },
      }
    ],
  },
  {
    path: "/admin",
    name: "admin",
    redirect: "/admin/index",
    component: () => import("../views/admin/index.vue"),
    meta: { requireAuth: true },
    children: [
      {
        path: "index",
        name: "index",
        component: () => import("../views/admin/home.vue"),
      },
      {
        path: "game",
        name: "game",
        component: () => import("../views/admin/game.vue"),
      },
      {
        path: "user",
        name: "user",
        component: () => import("../views/admin/user.vue"),
        meta: { title: '用户管理' }
      },
      {
        path: "post",
        name: "post",
        component: () => import("../views/admin/post.vue"),
      },
      {
        path: "report",
        name: "report",
        component: () => import("../views/admin/report.vue"),
      },
      {
        path: "message",
        name: "staffMessage",
        component: () => import("../views/admin/message.vue"),
        meta: { title: '站内私信' }
      },
      {
        path: "adminNews",
        name: "adminNews",
        component: () => import("../views/admin/news.vue"),
      },
      {
        path: '/admin/order',
        name: 'OrderManagement',
        component: () => import('../views/admin/order.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: "log",
        name: "operationLog",
        component: () => import("../views/admin/log.vue"),
        meta: { title: '日志管理' }
      },
    ],
  },
  {
    path: "/login",
    name: "login",
    component: Login,
    meta: {
      requireAuth: false,
    },
  },
  {
    path: "/forgot-password",
    name: "forgotPassword",
    component: () => import("../views/forgotPassword/index.vue"),
    meta: {
      requireAuth: false,
    },
  },
];
const router = createRouter({
  history: createWebHistory(),
  routes,
});
export default router;
