import Home from '@/views/oj/Home.vue';
import SetNewPassword from '@/views/oj/user/SetNewPassword.vue';
import UserHome from '@/views/oj/user/UserHome.vue';
import Setting from '@/views/oj/user/Setting.vue';
import ProblemLIst from '@/views/oj/problem/ProblemList.vue';
import Logout from '@/views/oj/user/Logout.vue';
import SubmissionList from '@/views/oj/status/SubmissionList.vue';
import SubmissionDetails from '@/views/oj/status/SubmissionDetails.vue';
import ContestList from '@/views/oj/contest/ContestList.vue';
import Problem from '@/views/oj/problem/Problem.vue';
import ACMRank from '@/views/oj/rank/ACMRank.vue';
import OIRank from '@/views/oj/rank/OIRank.vue';
import OJRank from '@/views/oj/rank/OJRank.vue';
import CODERank from '@/views/oj/rank/CODERank.vue';
import StaticRank from '@/views/oj/rank/StatisticRank.vue';
import StatisticRankList from '@/views/oj/rank/StatisticRankList.vue';
import ContestDetails from '@/views/oj/contest/ContestDetails.vue';
import ContestFullScreenDetails from '@/views/oj/contest/ContestFullScreenDetails.vue';
import ACMScoreBoard from '@/views/oj/contest/outside/ACMScoreBoard.vue';
import OIScoreBoard from '@/views/oj/contest/outside/OIScoreBoard.vue';
import ContestProblemList from '@/views/oj/contest/children/ContestProblemList.vue';
import ContestRank from '@/views/oj/contest/children/ContestRank.vue';
import ACMInfoAdmin from '@/views/oj/contest/children/ACMInfoAdmin.vue';
import Announcements from '@/components/oj/common/Announcements.vue';
import ContestComment from '@/views/oj/contest/children/ContestComment.vue';
import ContestPrint from '@/views/oj/contest/children/ContestPrint.vue';
import ContestAdminPrint from '@/views/oj/contest/children/ContestAdminPrint.vue';
import ContestAdminMoss from '@/views/oj/contest/moss/ContestAdminMoss.vue';
import ContestAdminMossDetails from '@/views/oj/contest/moss/ContestAdminMossDetails.vue';
import ScrollBoard from '@/views/oj/contest/children/ScrollBoard.vue';
import ContestRejudgeAdmin from '@/views/oj/contest/children/ContestRejudgeAdmin.vue';
import ContestSession from '@/views/oj/contest/children/ContestSession.vue';
import DiscussionList from '@/views/oj/discussion/discussionList.vue';
import Honor from '@/views/oj/honor/Honor.vue';
import Discussion from '@/views/oj/discussion/discussion.vue';
import ExaminationRoom from '@/components/oj/common/ExaminationRoom.vue';
import SearchExamination from '@/components/oj/common/SearchExamination.vue';
import AcCsv from '@/components/oj/common/AcCsv.vue';
import Introduction from '@/views/oj/about/Introduction.vue';
import Developer from '@/views/oj/about/Developer.vue';
import Message from '@/views/oj/message/message.vue';
import UserMsg from '@/views/oj/message/UserMsg.vue';
import InventMsg from '@/views/oj/message/InventMsg.vue';
import SysMsg from '@/views/oj/message/SysMsg.vue';
import TrainingList from '@/views/oj/training/TrainingList.vue';
import TrainingDetails from '@/views/oj/training/TrainingDetails.vue';
import TrainingFullScreenDetails from '@/views/oj/training/TrainingFullScreenDetails.vue';
import TrainingProblemList from '@/views/oj/training/TrainingProblemList.vue';
import TrainingRank from '@/views/oj/training/TrainingRank.vue';
import GroupList from '@/views/oj/group/GroupList.vue';
import GroupDetails from '@/views/oj/group/GroupDetails.vue';
import GroupAnnouncementList from '@/views/oj/group/children/GroupAnnouncementList.vue';
import GroupProblemList from '@/views/oj/group/children/GroupProblemList.vue';
import GroupTrainingList from '@/views/oj/group/children/GroupTrainingList.vue';
import GroupContestList from '@/views/oj/group/children/GroupContestList.vue';
import GroupDiscussionList from '@/views/oj/group/children/GroupDiscussionList.vue';
import GroupMemberList from '@/views/oj/group/children/GroupMemberList.vue';
import GroupSetting from '@/views/oj/group/children/GroupSetting.vue';
import GroupRank from '@/views/oj/group/children/GroupRank.vue';
import NotFound from '@/views/404.vue';
import GroupContestFullScreen from '@/views/oj/group/GroupContestFullScreen.vue';
import GroupTrainingFullScreen from '@/views/oj/group/GroupTrainingFullScreen.vue';

const ojRoutes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { title: 'Home' },
  },
  {
    path: '/problem',
    name: 'ProblemList',
    component: ProblemLIst,
    meta: { title: 'Problem' },
  },
  {
    path: '/announcement/:announcementID?',
    name: 'Announcements',
    component: Announcements,
    meta: { title: 'Announcements' },
  },
  {
    path: '/problem/:problemID/description/:descriptionID?',
    name: 'ProblemDetails',
    component: Problem,
    meta: { title: 'Problem Details' },
  },
  {
    name: 'TrainingFullScreenDetails',
    path: '/training/:trainingID/full-screen/',
    component: TrainingFullScreenDetails,
    meta: { title: 'Contest Details', fullScreenSource: 'training' },
    children: [
      {
        name: 'TrainingFullProblemDetails',
        path: 'problem/:problemID?',
        component: Problem,
        meta: { title: 'Training Problem Details', fullScreenSource: 'training' },
      },
      {
        name: 'TrainingFullRank',
        path: 'rank',
        component: TrainingRank,
        meta: { title: 'Training Rank', fullScreenSource: 'training' },
      },
      {
        name: 'TrainingFullSubmissionList',
        path: 'submissions',
        component: SubmissionList,
        meta: { title: 'Training Submission', fullScreenSource: 'training' },
      },
      {
        name: 'TrainingFullSubmissionDetails',
        path: 'submission-detail/:submitID',
        component: SubmissionDetails,
        meta: { title: 'Training Submission Details', fullScreenSource: 'training' },
      },
    ],
  },
  {
    name: 'ContestFullScreenDetails',
    path: '/contest/:contestID/full-screen/',
    component: ContestFullScreenDetails,
    meta: { title: 'Contest Details', fullScreenSource: 'contest' },
    children: [
      {
        name: 'ContestFullProblemDetails',
        path: 'problem/:problemID?',
        component: Problem,
        meta: { title: 'Contest Problem Details', fullScreenSource: 'contest' },
      },
      {
        name: 'ContestFullSubmissionList',
        path: 'submissions',
        component: SubmissionList,
        meta: { title: 'Contest Submission', fullScreenSource: 'contest' },
      },
      {
        name: 'ContestFullSubmissionDetails',
        path: 'submission-detail/:submitID',
        component: SubmissionDetails,
        meta: { title: 'Contest Submission Details', fullScreenSource: 'contest' },
      },
      {
        name: 'ContestFullRank',
        path: 'rank',
        component: ContestRank,
        meta: { title: 'Contest Rank', fullScreenSource: 'contest' },
      },
      {
        name: 'ContestFullAnnouncement',
        path: 'announcement/:announcementID?',
        component: Announcements,
        meta: { title: 'Contest Announcement', fullScreenSource: 'contest' },
      },
      {
        name: 'ContestFullComment',
        path: 'comment',
        component: ContestComment,
        meta: { title: 'Contest Comment', access: 'contestComment', fullScreenSource: 'contest' },
      },
    ],
  },
  {
    path: '/training',
    name: 'TrainingList',
    component: TrainingList,
    meta: { title: 'Training' },
  },
  {
    name: 'TrainingDetails',
    path: '/training/:trainingID/',
    component: TrainingDetails,
    meta: { title: 'Training Details' },
    children: [
      {
        name: 'TrainingProblemList',
        path: 'problems',
        component: TrainingProblemList,
        meta: { title: 'Training Problem' },
      },
      {
        name: 'TrainingProblemDetails',
        path: 'problem/:problemID',
        component: Problem,
        meta: { title: 'Training Problem Details' },
      },
      {
        name: 'TrainingRank',
        path: 'rank',
        component: TrainingRank,
        meta: { title: 'Training Rank' },
      },
    ],
  },
  {
    path: '/contest',
    name: 'ContestList',
    component: ContestList,
    meta: { title: 'Contest' },
  },
  {
    path: '/contest/acm-scoreboard/:contestID',
    name: 'ACMScoreBoard',
    component: ACMScoreBoard,
    meta: { title: 'ACM Contest ScoreBoard' },
  },
  {
    path: '/contest/oi-scoreboard/:contestID',
    name: 'OIScoreBoard',
    component: OIScoreBoard,
    meta: { title: 'OI Contest ScoreBoard' },
  },
  {
    name: 'ContestDetails',
    path: '/contest/:contestID/',
    component: ContestDetails,
    meta: { title: 'Contest Details' },
    children: [
      {
        name: 'ContestAcCsv',
        path: 'acCsv',
        meta: { title: 'Contest AcCsv' },
        component: AcCsv,
      },
      {
        name: 'ContestSubmissionList',
        path: 'submissions',
        component: SubmissionList,
        meta: { title: 'Contest Submission', requireAuth: true },
      },
      {
        name: 'ContestSubmissionDetails',
        path: 'problem/:problemID/submission-detail/:submitID',
        component: SubmissionDetails,
        meta: { title: 'Contest Submission Details', requireAuth: true },
      },
      {
        name: 'ContestAdminMossDetails',
        path: 'moss/:mossID/',
        component: ContestAdminMossDetails,
        meta: {
          title: 'Contest Moss Details',
          requireAuth: true,
        },
      },
      {
        name: 'ContestProblemList',
        path: 'problems',
        component: ContestProblemList,
        meta: { title: 'Contest Problem', requireAuth: true },
      },
      {
        name: 'ContestProblemDetails',
        path: 'problem/:problemID/',
        component: Problem,
        meta: { title: 'Contest Problem Details', requireAuth: true },
      },
      {
        name: 'ContestAnnouncementList',
        path: 'announcement/:announcementID?',
        component: Announcements,
        meta: { title: 'Contest Announcement', requireAuth: true },
      },
      {
        name: 'ContestRank',
        path: 'rank',
        component: ContestRank,
        meta: { title: 'Contest Rank', requireAuth: true },
      },
      {
        name: 'ContestACInfo',
        path: 'ac-info',
        component: ACMInfoAdmin,
        meta: { title: 'Contest AC Info', requireAuth: true },
      },
      {
        name: 'ContestRejudgeAdmin',
        path: 'rejudge',
        component: ContestRejudgeAdmin,
        meta: { title: 'Contest Rejudge', requireAuth: true },
      },
      {
        name: 'ContestComment',
        path: 'comment',
        component: ContestComment,
        meta: { title: 'Contest Comment', access: 'contestComment', requireAuth: true },
      },
      {
        name: 'ContestPrint',
        path: 'print',
        component: ContestPrint,
        meta: { title: 'Contest Print', requireAuth: true },
      },
      {
        name: 'ContestAdminPrint',
        path: 'admin-print',
        component: ContestAdminPrint,
        meta: { title: 'Contest Admin Print', requireAuth: true },
      },
      {
        name: 'ContestAdminMoss',
        path: 'admin-moss',
        component: ContestAdminMoss,
        meta: {
          title: 'Contest Admin Moss',
          requireAuth: true,
        },
      },
      {
        name: 'ScrollBoard',
        path: 'scroll-board',
        component: ScrollBoard,
        meta: { title: 'Contest Scroll Board', requireAuth: true },
      },
      {
        name: 'ContestSession',
        path: 'contest-session',
        component: ContestSession,
        meta: { title: 'Contest Session', requireAuth: true },
      },
    ],
  },
  {
    path: '/exam',
    name: 'ExamList',
    component: ContestList,
    meta: { title: 'Exam' },
  },
  {
    name: 'ExamDetails',
    path: '/exam/:contestID/',
    component: ContestDetails,
    meta: { title: 'Exam Details' },
    children: [
      {
        name: 'ExamSubmissionList',
        path: 'submissions',
        component: SubmissionList,
        meta: { title: 'Exam Submission', requireAuth: true },
      },
      {
        name: 'ExamSubmissionDetails',
        path: 'problem/:problemID/submission-detail/:submitID',
        component: SubmissionDetails,
        meta: { title: 'Exam Submission Details', requireAuth: true },
      },
      {
        name: 'ExamProblemList',
        path: 'problems',
        component: ContestProblemList,
        meta: { title: 'Exam Problem', requireAuth: true },
      },
      {
        name: 'ExamProblemDetails',
        path: 'problem/:problemID/',
        component: Problem,
        meta: { title: 'Exam Problem Details', requireAuth: true },
      },
      {
        name: 'ExamAnnouncementList',
        path: 'announcement/:announcementID?',
        component: Announcements,
        meta: { title: 'Exam Announcement', requireAuth: true },
      },
      {
        name: 'ExamRank',
        path: 'rank',
        component: ContestRank,
        meta: { title: 'Exam Rank', requireAuth: true },
      },
      {
        name: 'ExamACInfo',
        path: 'ac-info',
        component: ACMInfoAdmin,
        meta: { title: 'Exam AC Info', requireAuth: true },
      },
      {
        name: 'ExamRejudgeAdmin',
        path: 'rejudge',
        component: ContestRejudgeAdmin,
        meta: { title: 'Exam Rejudge', requireAuth: true },
      },
      {
        name: 'ExamComment',
        path: 'comment',
        component: ContestComment,
        meta: { title: 'Exam Comment', access: 'contestComment', requireAuth: true },
      },
      {
        name: 'ExamAdminMoss',
        path: 'admin-moss',
        component: ContestAdminMoss,
        meta: {
          title: 'Exam Admin Moss',
          requireAuth: true,
        },
      },
      {
        name: 'ExamAdminMossDetails',
        path: 'moss/:mossID/',
        component: ContestAdminMossDetails,
        meta: {
          title: 'Exam Moss Details',
          requireAuth: true,
        },
      },
    ],
  },
  {
    name: 'ExamFullScreenDetails',
    path: '/exam/:contestID/full-screen/',
    component: ContestFullScreenDetails,
    meta: { title: 'Exam Details', fullScreenSource: 'exam' },
    children: [
      {
        name: 'ExamFullProblemDetails',
        path: 'problem/:problemID?',
        component: Problem,
        meta: { title: 'Exam Problem Details', fullScreenSource: 'exam' },
      },
      {
        name: 'ExamFullSubmissionList',
        path: 'submissions',
        component: SubmissionList,
        meta: { title: 'Exam Submission', fullScreenSource: 'exam' },
      },
      {
        name: 'ExamFullSubmissionDetails',
        path: 'submission-detail/:submitID',
        component: SubmissionDetails,
        meta: { title: 'Exam Submission Details', fullScreenSource: 'exam' },
      },
      {
        name: 'ExamFullRank',
        path: 'rank',
        component: ContestRank,
        meta: { title: 'Exam Rank', fullScreenSource: 'exam' },
      },
      {
        name: 'ExamFullAnnouncement',
        path: 'announcement/:announcementID?',
        component: Announcements,
        meta: { title: 'Exam Announcement', fullScreenSource: 'exam' },
      },
      {
        name: 'ExamFullComment',
        path: 'comment',
        component: ContestComment,
        meta: { title: 'Exam Comment', access: 'contestComment', fullScreenSource: 'exam' },
      },
    ],
  },
  {
    path: '/submissions',
    name: 'SubmissionList',
    component: SubmissionList,
    meta: { title: 'Status' },
  },
  {
    path: '/submission-detail/:submitID',
    name: 'SubmissionDetails',
    component: SubmissionDetails,
    meta: { title: 'Submission Details' },
  },
  {
    path: '/acm-rank',
    name: 'ACM Rank',
    component: ACMRank,
    meta: { title: 'ACM Rank' },
  },
  {
    path: '/oi-rank',
    name: 'OI Rank',
    component: OIRank,
    meta: { title: 'OI Rank' },
  },
  {
    path: '/oj-rank',
    name: 'OJ Rank',
    component: OJRank,
    meta: { title: 'OJ Rank' },
  },
  // {
  //   path: '/code-rank',
  //   name: 'CODE Rank',
  //   component: CODERank,
  //   meta: { title: 'CODE Rank' },
  // },
  {
    path: '/acm-rank-static',
    name: 'Static Rank List',
    component: StatisticRankList,
    meta: {
      title: 'Static Rank List',
    },
  },
  {
    path: '/acm-rank-static/:cids',
    name: 'Static Rank',
    component: StaticRank,
    meta: {
      title: 'ACM Static Rank',
    },
  },
  {
    path: '/reset-password',
    name: 'SetNewPassword',
    component: SetNewPassword,
    meta: { title: 'Reset Password' },
  },
  {
    name: 'UserHome',
    path: '/user-home',
    component: UserHome,
    meta: { title: 'User Home' },
  },
  {
    name: 'Setting',
    path: '/setting',
    component: Setting,
    meta: { requireAuth: true, title: 'Setting' },
  },
  {
    name: 'Logout',
    path: '/logout',
    component: Logout,
    meta: { requireAuth: true, title: 'Logout' },
  },
  {
    path: '/discussion',
    name: 'AllDiscussion',
    meta: { title: 'Discussion', access: 'discussion' },
    component: DiscussionList,
  },
  {
    path: '/discussion/problem/:problemID',
    name: 'ProblemDiscussion',
    meta: { title: 'Discussion', access: 'discussion' },
    component: DiscussionList,
  },
  {
    path: '/discussion/contest/:contestID',
    name: 'ContestDiscussion',
    meta: { title: 'ContsetDiscussion', access: 'discussion' },
    component: DiscussionList,
  },
  {
    path: '/discussion/training/:trainingID',
    name: 'TrainingDiscussion',
    meta: { title: 'TrainingDiscussion', access: 'discussion' },
    component: DiscussionList,
  },
  {
    path: '/discussion-detail/:discussionID',
    name: 'DiscussionDetails',
    meta: { title: 'Discussion Details', access: 'discussion' },
    component: Discussion,
  },
  {
    path: '/group',
    name: 'GroupList',
    component: GroupList,
    meta: { title: 'Group' },
  },
  {
    path: '/group/:groupID/',
    name: 'GroupDetails',
    component: GroupDetails,
    meta: { title: 'Group Details', requireAuth: true, fullScreenSource: 'group' },
    children: [
      {
        name: 'GroupTrainingFullScreen',
        path: 'training/:trainingID/full-screen/',
        component: GroupTrainingFullScreen,
        meta: { title: 'Contest Details', fullScreenSource: 'training' },
        children: [
          {
            name: 'GroupTrainingFullProblemDetails',
            path: 'problem/:problemID?',
            component: Problem,
            meta: { title: 'Group Training Problem Details', fullScreenSource: 'training' },
          },
          {
            name: 'GroupTrainingFullRank',
            path: 'rank',
            component: TrainingRank,
            meta: { title: 'Group Training Rank', fullScreenSource: 'training' },
          },
          {
            name: 'GroupTrainingFullSubmissionList',
            path: 'submissions',
            component: SubmissionList,
            meta: { title: 'Group Training Submission', fullScreenSource: 'training' },
          },
          {
            name: 'GroupTrainingFullSubmissionDetails',
            path: 'submission-detail/:submitID',
            component: SubmissionDetails,
            meta: { title: 'Group Training Submission Details', fullScreenSource: 'training' },
          },
        ],
      },
      {
        name: 'GroupContestFullScreen',
        path: 'contest/:contestID/full-screen/',
        component: GroupContestFullScreen,
        meta: { title: 'Group Contest Details', fullScreenSource: 'contest' },
        children: [
          {
            name: 'GroupContestFullProblemDetails',
            path: 'problem/:problemID?',
            component: Problem,
            meta: { title: 'Group Contest Problem Details', fullScreenSource: 'contest' },
          },
          {
            name: 'GroupContestFullSubmissionList',
            path: 'submissions',
            component: SubmissionList,
            meta: { title: 'Group Contest Submission', fullScreenSource: 'contest' },
          },
          {
            name: 'GroupContestFullSubmissionDetails',
            path: 'submission-detail/:submitID',
            component: SubmissionDetails,
            meta: { title: 'Group Contest Submission Details', fullScreenSource: 'contest' },
          },
          {
            name: 'GroupContestFullRank',
            path: 'rank',
            component: ContestRank,
            meta: { title: 'Group Contest Rank', fullScreenSource: 'contest' },
          },
          {
            name: 'GroupContestFullAnnouncement',
            path: 'announcement/:announcementID?',
            component: Announcements,
            meta: { title: 'Group Contest Announcement', fullScreenSource: 'contest' },
          },
          {
            name: 'GroupContestFullComment',
            path: 'comment',
            component: ContestComment,
            meta: { title: 'Group Contest Comment', access: 'contestComment', fullScreenSource: 'contest' },
          },
        ],
      },
      {
        path: 'announcement',
        name: 'GroupAnnouncementList',
        component: GroupAnnouncementList,
        meta: { title: 'Group Announcement', fullScreenSource: 'group' },
      },
      {
        path: 'problem',
        name: 'GroupProblemList',
        component: GroupProblemList,
        meta: { title: 'Group Problem', fullScreenSource: 'group' },
      },
      {
        name: 'GroupProblemDetails',
        path: 'problem/:problemID/description/:descriptionID?',
        component: Problem,
        meta: { title: 'Group Problem Details', fullScreenSource: 'group' },
      },
      {
        path: 'training',
        name: 'GroupTrainingList',
        component: GroupTrainingList,
        meta: { title: 'Group Training', fullScreenSource: 'group' },
      },
      {
        name: 'GroupTrainingDetails',
        path: 'training/:trainingID/',
        component: TrainingDetails,
        meta: { title: 'Group Training Details', fullScreenSource: 'group' },
        children: [
          {
            name: 'GroupTrainingProblemList',
            path: 'problems',
            component: TrainingProblemList,
            meta: { title: 'Group Training Problem', fullScreenSource: 'group' },
          },
          {
            name: 'GroupTrainingProblemDetails',
            path: 'problem/:problemID/',
            component: Problem,
            meta: { title: 'Group Training Problem Details', fullScreenSource: 'group' },
          },
          {
            name: 'GroupTrainingRank',
            path: 'rank',
            component: TrainingRank,
            meta: { title: 'Group Training Rank', fullScreenSource: 'group' },
          },
        ],
      },
      {
        path: 'submissions',
        name: 'GroupSubmissionList',
        component: SubmissionList,
        meta: { title: 'Group Status', fullScreenSource: 'group' },
      },
      {
        path: 'submission-detail/:submitID',
        name: 'GroupSubmissionDetails',
        component: SubmissionDetails,
        meta: { title: 'Group Submission Details', fullScreenSource: 'group' },
      },
      {
        path: 'discussion',
        name: 'GroupDiscussionList',
        component: GroupDiscussionList,
        meta: { title: 'Group Discussion', access: 'groupDiscussion', fullScreenSource: 'group' },
      },
      {
        path: 'discussion/problem/:problemID',
        name: 'GroupProblemDiscussion',
        meta: { title: 'Group Discussion', access: 'groupDiscussion', fullScreenSource: 'group' },
        component: GroupDiscussionList,
      },
      {
        path: 'discussion/contest/:contestID',
        name: 'GroupContestProblemDiscussion',
        meta: { title: 'Group Contest Discussion', access: 'groupDiscussion', fullScreenSource: 'group' },
        component: GroupDiscussionList,
      },
      {
        path: 'discussion-detail/:discussionID',
        name: 'GroupDiscussionDetails',
        meta: { title: 'Group Discussion Details', access: 'groupDiscussion', fullScreenSource: 'group' },
        component: Discussion,
      },
      {
        path: 'member',
        name: 'GroupMemberList',
        component: GroupMemberList,
        meta: { title: 'Group Member', fullScreenSource: 'group' },
      },
      {
        path: 'setting',
        name: 'GroupSetting',
        component: GroupSetting,
        meta: { title: 'Group Setting', fullScreenSource: 'group' },
      },
      {
        path: 'rank',
        name: 'GroupRank',
        component: GroupRank,
        meta: { title: 'Group Rank', fullScreenSource: 'group' },
      },
      {
        path: 'contest',
        name: 'GroupContestList',
        component: GroupContestList,
        meta: { title: 'Group Contest', fullScreenSource: 'group' },
      },
      {
        name: 'GroupContestDetails',
        path: 'contest/:contestID/',
        component: ContestDetails,
        meta: { title: 'Group Contest Details', fullScreenSource: 'group' },
        children: [
          {
            name: 'GroupContestAcCsv',
            path: 'acCsv',
            meta: { title: 'Group Contest AcCsv', fullScreenSource: 'group' },
            component: AcCsv,
          },
          {
            name: 'GroupContestSubmissionList',
            path: 'submissions',
            component: SubmissionList,
            meta: { title: 'Group Contest Submission', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestSubmissionDetails',
            path: 'problem/:problemID/submission-detail/:submitID',
            component: SubmissionDetails,
            meta: { title: 'Group Contest Submission Details', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestAdminMossDetails',
            path: 'moss/:mossID/',
            component: ContestAdminMossDetails,
            meta: { title: 'Group Contest Moss Details', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestProblemList',
            path: 'problems',
            component: ContestProblemList,
            meta: { title: 'Group Contest Problem', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestProblemDetails',
            path: 'problem/:problemID',
            component: Problem,
            meta: { title: 'Group Contest Problem Details', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestAnnouncementList',
            path: 'announcement/:announcementID?',
            component: Announcements,
            meta: { title: 'Group Contest Announcement', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestRank',
            path: 'rank',
            component: ContestRank,
            meta: { title: 'Group Contest Rank', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestACInfo',
            path: 'ac-info',
            component: ACMInfoAdmin,
            meta: { title: 'Group Contest AC Info', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestRejudgeAdmin',
            path: 'rejudge',
            component: ContestRejudgeAdmin,
            meta: { title: 'Group Contest Rejudge', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestComment',
            path: 'comment',
            component: ContestComment,
            meta: { title: 'Group Contest Comment', access: 'contestComment', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestPrint',
            path: 'print',
            component: ContestPrint,
            meta: { title: 'Group Contest Print', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestAdminPrint',
            path: 'admin-print',
            component: ContestAdminPrint,
            meta: { title: 'Group Contest Admin Print', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestAdminMoss',
            path: 'admin-moss',
            component: ContestAdminMoss,
            meta: { title: 'Group Contest Admin Moss', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupScrollBoard',
            path: 'scroll-board',
            component: ScrollBoard,
            meta: { title: 'Group Contest Scroll Board', requireAuth: true, fullScreenSource: 'group' },
          },
          {
            name: 'GroupContestSession',
            path: 'contest-session',
            component: ContestSession,
            meta: { title: 'Group Contest Session', requireAuth: true, fullScreenSource: 'group' },
          },
        ],
      },
      {
        name: 'GroupUserHome',
        path: 'user-home',
        component: UserHome,
        meta: { title: 'Group User Home', fullScreenSource: 'group' },
      },
    ],
  },
  {
    path: '/searchExamination/:contestID',
    name: 'search-examination-room',
    meta: { title: 'SearchExamination' },
    component: SearchExamination,
  },
  {
    path: '/examinationRoom/:examinationRoomId',
    name: 'get-examination-room',
    meta: { title: 'ExaminationRoom' },
    component: ExaminationRoom,
  },
  {
    path: '/introduction',
    meta: { title: 'Introduction' },
    component: Introduction,
  },
  {
    path: '/developer',
    meta: { title: 'Developer' },
    component: Developer,
  },
  {
    path: '/honor',
    name: 'AllHonor',
    meta: { title: 'Honor' },
    component: Honor,
  },
  {
    name: 'Message',
    path: '/message/',
    component: Message,
    meta: { requireAuth: true, title: 'Message' },
    children: [
      {
        name: 'DiscussMsg',
        path: 'discuss',
        component: UserMsg,
        meta: { requireAuth: true, title: 'Discuss Message' },
      },
      {
        name: 'ReplyMsg',
        path: 'reply',
        component: UserMsg,
        meta: { requireAuth: true, title: 'Reply Message' },
      },
      {
        name: 'LikeMsg',
        path: 'like',
        component: UserMsg,
        meta: { requireAuth: true, title: 'Like Message' },
      },
      {
        name: 'InventMsg',
        path: 'invent',
        component: InventMsg,
        meta: {
          requireAuth: true,
          title: 'Invent Message',
        },
      },
      {
        name: 'SysMsg',
        path: 'sys',
        component: SysMsg,
        meta: { requireAuth: true, title: 'System Message' },
      },
      {
        name: 'MineMsg',
        path: 'mine',
        component: SysMsg,
        meta: { requireAuth: true, title: 'Mine Message' },
      },
    ],
  },
  {
    path: '*',
    meta: { title: '404' },
    component: NotFound,
    meta: { title: '404' },
  },
];
export default ojRoutes;
