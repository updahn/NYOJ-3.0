import Vue from 'vue';
import VueRouter from 'vue-router';
import { sync } from 'vuex-router-sync';
import adminRoutes from '@/router/adminRoutes';
import signupRoutes from '@/router/signupRoutes';
import ojRoutes from '@/router/ojRoutes';
import mMessage from '@/common/message';
import store from '@/store';
import NProgress from 'nprogress'; // nprogress插件
import 'nprogress/nprogress.css'; // nprogress样式
import i18n from '@/i18n';

// 配置NProgress进度条选项  —— 动画效果
NProgress.configure({ ease: 'ease', speed: 1000, showSpinner: false });

Vue.use(VueRouter);

//获取原型对象上的push函数
const originalPush = VueRouter.prototype.push;
// 修改原型对象中的 push 方法
VueRouter.prototype.push = function push(location, onComplete, onAbort) {
  // 检测 Ctrl 键是否被按下
  if (window.event && window.event.ctrlKey) {
    // 在新标签页中打开链接
    window.open(this.resolve(location).href, '_blank');
    // 阻止默认的 push 行为
    return Promise.resolve();
  } else {
    // 调用原始的 push 方法
    return originalPush.call(this, location, onComplete, onAbort).catch((err) => err);
  }
};

let routes = new Set([...ojRoutes, ...adminRoutes, ...signupRoutes]);
const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return { x: 0, y: 0 };
    }
  },
});

// 路由判断登录 根据路由配置文件的参数(全局身份验证token)
router.beforeEach((to, from, next) => {
  NProgress.start();
  if (to.matched.some((record) => record.meta.requireAuth)) {
    // 判断该路由是否需要登录权限
    const token = localStorage.getItem('token') || '';
    const isSuperAdmin = store.getters.isSuperAdmin;
    const isMainRoleAdmin = store.getters.isMainAdminRole;
    const isAmdin = store.getters.isAdminRole;
    if (token) {
      // 判断当前的token是否存在 ； 登录存入的token

      if (to.matched.some((record) => record.meta.requireSuperAdmin)) {
        // 判断是否需要超级管理权限

        if (isSuperAdmin) {
          // 拥有权限就进入
          next();
        } else {
          // 没有超级管理员权限 全部返回登录页，并且清除缓存
          if (to.path.split('/')[1] === 'admin') {
            //管理端
            next({
              path: '/admin/login',
            });
            mMessage.error(i18n.t('m.Please_login_first_by_admin_account'));
          } else if (to.path.split('/')[1] === 'signup') {
            next({
              path: '/signup/login',
            });
            mMessage.error(i18n.t('m.Please_login_first'));
          } else {
            // oj端
            next({
              path: '/',
            });
            store.commit('changeModalStatus', { mode: 'Login', visible: true });
            mMessage.error(i18n.t('m.Please_login_first'));
            store.commit('clearUserInfoAndToken');
          }
        }
      } else if (to.matched.some((record) => record.meta.requireMainRoleAdmin)) {
        //判断是否需要管理员权限
        if (isMainRoleAdmin) {
          next();
        } else {
          // 没有管理员权限 全部返回登录页，并且清除缓存
          if (to.path.split('/')[1] === 'admin') {
            // 管理端
            next({
              path: '/admin/login',
            });
            mMessage.error(i18n.t('m.Please_login_first_by_admin_account'));
          } else if (to.path.split('/')[1] === 'signup') {
            next({
              path: '/signup/login',
            });
            mMessage.error(i18n.t('m.Please_login_first'));
          } else {
            next({
              path: '/',
            });
            store.commit('changeModalStatus', { mode: 'Login', visible: true });
            mMessage.error(i18n.t('m.Please_login_first'));
            store.commit('clearUserInfoAndToken');
          }
        }
      } else if (to.matched.some((record) => record.meta.requireAdmin)) {
        //判断是否需要管理员权限
        if (isAmdin) {
          next();
        } else {
          // 没有管理员权限 全部返回登录页，并且清除缓存
          if (to.path.split('/')[1] === 'admin') {
            // 管理端
            next({
              path: '/admin/login',
            });
            mMessage.error(i18n.t('m.Please_login_first_by_admin_account'));
          } else if (to.path.split('/')[1] === 'signup') {
            next({
              path: '/signup/login',
            });
            mMessage.error(i18n.t('m.Please_login_first'));
          } else {
            next({
              path: '/',
            });
            store.commit('changeModalStatus', { mode: 'Login', visible: true });
            mMessage.error(i18n.t('m.Please_login_first'));
            store.commit('clearUserInfoAndToken');
          }
        }
      } else {
        next();
      }
    } else {
      // 如果没有token

      if (to.path.split('/')[1] === 'admin') {
        next({
          path: '/admin/login', // 管理端无token认证返回登录页
        });
      } else if (to.path.split('/')[1] === 'signup') {
        next({
          path: '/signup/login',
        });
      } else {
        store.commit('changeModalStatus', { mode: 'Login', visible: true });
      }
      store.commit('clearUserInfoAndToken');
      mMessage.error(i18n.t('m.Please_login_first'));
    }
  } else {
    // 不需要登录认证的页面
    if (to.meta.access) {
      // 单级路由有access控制
      const webConfig = store.getters.websiteConfig;
      switch (to.meta.access) {
        case 'discussion':
          if (!webConfig.openPublicDiscussion) {
            next({
              path: '/',
            });
            mMessage.error(i18n.t('m.No_Access_There_is_no_open_discussion_area_on_the_website'));
          }
          break;
        case 'groupDiscussion':
          if (!webConfig.openGroupDiscussion) {
            next({
              path: '/',
            });
            mMessage.error(i18n.t('m.No_Access_There_is_no_open_group_discussion_area_on_the_website'));
          }
          break;
        case 'contestComment':
          if (!webConfig.openContestComment) {
            next({
              path: '/',
            });
            mMessage.error(i18n.t('m.No_Access_There_is_no_open_contest_comment_area_on_the_website'));
          }
          break;
      }
    }
    next();
  }
});

router.afterEach((to, from, next) => {
  NProgress.done();
});

sync(store, router);

export default router;
