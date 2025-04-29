// 引入 view 组件
const Login = () => import('@/views/admin/Login');
const SignupHome = () => import('@/views/signup/Home');
const SignupDashboard = () => import('@/views/signup/Dashboard');
const SignupUserSetting = () => import('@/views/signup/user/Setting');
const SignupUserAccount = () => import('@/views/signup/user/Account');
const SignupContestList = () => import('@/views/signup/contest/ContestList');
const SignupTeamSignList = () => import('@/views/signup/contest/SignList');
const SignupUserPool = () => import('@/views/signup/pool/UserList');
const SignupTeamPool = () => import('@/views/signup/pool/TeamList');

const signupRoutes = [
  {
    path: '/signup/login',
    name: 'signup-login',
    component: Login,
    meta: { title: 'Signup Login' },
  },
  {
    path: '/signup/',
    component: SignupHome,
    meta: { requireAuth: true },
    children: [
      {
        path: '',
        redirect: 'dashboard',
        component: SignupDashboard,
        meta: { title: 'Dashboard' },
      },
      {
        path: 'dashboard',
        name: 'signup-dashboard',
        component: SignupDashboard,
        meta: { title: 'Dashboard' },
      },
      {
        path: 'user/setting',
        name: 'signup-user-setting',
        component: SignupUserSetting,
        meta: { title: 'Signup UserSetting' },
      },
      {
        path: 'user/account',
        name: 'signup-user-account',
        component: SignupUserAccount,
        meta: { title: 'Signup UserAccount' },
      },
      {
        path: 'pool/user',
        name: 'signup-user-pool',
        component: SignupUserPool,
        meta: { title: 'Signup UserPool' },
      },
      {
        path: 'pool/team',
        name: 'signup-team-pool',
        component: SignupTeamPool,
        meta: { title: 'Signup TeamPool' },
      },
      {
        path: 'contest',
        name: 'signup-contest-list',
        component: SignupContestList,
        meta: { title: 'Signup ContestList' },
      },
      {
        path: 'contest/:contestID',
        name: 'signup-contest-sign-list',
        component: SignupTeamSignList,
        meta: { title: 'Signup TeamSignList' },
      },
    ],
  },
  {
    path: '/signup/*',
    redirect: '/signup/login',
  },
];

export default signupRoutes;
