export const JUDGE_STATUS = {
  '-10': {
    name: 'Not Submitted',
    short: 'NS',
    color: 'gray',
    type: 'info',
    rgb: '#909399',
  },
  '-5': {
    name: 'Submitted Unknown Result',
    short: 'SNR',
    color: 'gray',
    type: 'info',
    rgb: '#909399',
  },
  '-4': {
    name: 'Cancelled',
    short: 'CA',
    color: 'purple',
    type: 'info',
    rgb: '#676fc1',
  },
  '-3': {
    name: 'Presentation Error',
    short: 'PE',
    color: 'yellow',
    type: 'warning',
    rgb: '#f90',
  },
  '-2': {
    name: 'Compile Error',
    short: 'CE',
    color: 'yellow',
    type: 'warning',
    rgb: '#f90',
  },
  '-1': {
    name: 'Wrong Answer',
    short: 'WA',
    color: 'red',
    type: 'error',
    rgb: '#ed3f14',
  },
  '0': {
    name: 'Accepted',
    short: 'AC',
    color: 'green',
    type: 'success',
    rgb: '#19be6b',
  },
  '1': {
    name: 'Time Limit Exceeded',
    short: 'TLE',
    color: 'red',
    type: 'error',
    rgb: '#ed3f14',
  },
  '2': {
    name: 'Memory Limit Exceeded',
    short: 'MLE',
    color: 'red',
    type: 'error',
    rgb: '#ed3f14',
  },
  '3': {
    name: 'Runtime Error',
    short: 'RE',
    color: 'red',
    type: 'error',
    rgb: '#ed3f14',
  },
  '4': {
    name: 'System Error',
    short: 'SE',
    color: 'gray',
    type: 'info',
    rgb: '#909399',
  },
  '5': {
    name: 'Pending',
    color: 'yellow',
    type: 'warning',
    rgb: '#f90',
  },
  '6': {
    name: 'Compiling',
    short: 'CP',
    color: 'green',
    type: 'info',
    rgb: '#25bb9b',
  },
  '7': {
    name: 'Judging',
    color: 'blue',
    type: '',
    rgb: '#2d8cf0',
  },
  '8': {
    name: 'Partial Accepted',
    short: 'PAC',
    color: 'blue',
    type: '',
    rgb: '#2d8cf0',
  },
  '9': {
    name: 'Submitting',
    color: 'yellow',
    type: 'warning',
    rgb: '#f90',
  },
  '10': {
    name: 'Submitted Failed',
    color: 'gray',
    short: 'SF',
    type: 'info',
    rgb: '#909399',
  },
  '15': {
    name: 'No Status',
    color: 'gray',
    short: 'NS',
    type: 'info',
    rgb: '#909399',
  },
};

export const JUDGE_STATUS_RESERVE = {
  ns: -10,
  snr: -5,
  ca: -4,
  pe: -3,
  ce: -2,
  wa: -1,
  ac: 0,
  tle: 1,
  mle: 2,
  re: 3,
  se: 4,
  Pending: 5,
  Compiling: 6,
  Judging: 7,
  pa: 8,
  Submitting: 9,
  sf: 10,
  dc: 12,
  ns: 15,
};

export const PROBLEM_LEVEL = {
  '0': {
    name: {
      'zh-CN': '简单',
      'en-US': 'Easy',
    },
    color: '#19be6b',
  },
  '1': {
    name: {
      'zh-CN': '中等',
      'en-US': 'Mid',
    },
    color: '#2d8cf0',
  },
  '2': {
    name: {
      'zh-CN': '困难',
      'en-US': 'Hard',
    },
    color: '#ed3f14',
  },
};

export const PROBLEM_TYPE_LEVEL = {
  '0': {
    name: {
      'zh-CN': 'ACM',
      'en-US': 'ACM',
    },
    color: '#1559A1',
  },
  '1': {
    name: {
      'zh-CN': 'OI',
      'en-US': 'OI',
    },
    color: '#19be6b',
  },
  '2': {
    name: {
      'zh-CN': '选择',
      'en-US': 'Selection',
    },
    color: '#66B1FF',
  },
  '3': {
    name: {
      'zh-CN': '填空',
      'en-US': 'Filling',
    },
    color: '#EEAC3C',
  },
  '4': {
    name: {
      'zh-CN': '判断',
      'en-US': 'Decide',
    },
    color: '#881E1F',
  },
};

export const PROBLEM_TYPE = {
  ACM: 0,
  OI: 1,
  SELECT: 2,
  FILL: 3,
  DECIDED: 4,
};

export const REMOTE_OJ = [
  {
    name: 'HDU',
    key: 'HDU',
  },
  {
    name: 'Codeforces',
    key: 'CF',
  },
  {
    name: 'POJ',
    key: 'POJ',
  },
  {
    name: 'GYM',
    key: 'GYM',
  },
  {
    name: 'AtCoder',
    key: 'AC',
  },
  {
    name: 'SPOJ',
    key: 'SPOJ',
  },
  {
    name: 'LIBREOJ',
    key: 'LIBRE',
  },
  {
    name: 'SCPC',
    key: 'SCPC',
  },
  {
    name: 'QOJ',
    key: 'QOJ',
  },
  {
    name: 'NSWOJ',
    key: 'NSWOJ',
  },
  {
    name: 'NEWOJ',
    key: 'NEWOJ',
  },
  {
    name: 'VJ',
    key: 'VJ',
  },
  {
    name: 'DOTCPP',
    key: 'DOTCPP',
  },
];

export const CONTEST_STATUS = {
  SCHEDULED: -1,
  RUNNING: 0,
  ENDED: 1,
};

export const CONTEST_STATUS_REVERSE = {
  '-1': {
    name: 'Scheduled',
    color: '#f90',
  },
  '0': {
    name: 'Running',
    color: '#19be6b',
  },
  '1': {
    name: 'Ended',
    color: '#ed3f14',
  },
};

export const TRAINING_TYPE = {
  Public: {
    color: 'success',
    name: 'Public',
  },
  Private: {
    color: 'danger',
    name: 'Private',
  },
};

export const HONOR_TYPE = {
  Gold: {
    color: '#FFD700',
    name: 'Honor_Gold',
  },
  Silver: {
    color: '#C0C0C0',
    name: 'Honor_Silver',
  },
  Bronze: {
    color: '#CD7F32',
    name: 'Honor_Bronze',
  },
};

export const GROUP_TYPE = {
  PUBLIC: 1,
  PROTECTED: 2,
  PRIVATE: 3,
  PROPOSITION: 4,
};

export const GROUP_TYPE_REVERSE = {
  '1': {
    name: 'Public',
    color: 'success',
    tips: 'Group_Public_Tips',
  },
  '2': {
    name: 'Protected',
    color: 'warning',
    tips: 'Group_Protected_Tips',
  },
  '3': {
    name: 'Private',
    color: 'danger',
    tips: 'Group_Private_Tips',
  },
  '4': {
    name: 'Proposition',
    color: 'info',
    tips: 'Group_Proposition_Tips',
  },
};

export const SIGN_TYPE_REVERSE = {
  // 未报名
  '-1': {
    name: 'Sign_Required',
    action: 'Send_Sign',
    color: 'warning',
  },
  // 审核中
  '0': {
    name: 'Sign_Waiting',
    action: 'Edit_Sign',
    color: 'warning',
  },
  // 通过
  '1': {
    name: 'Sign_Successfully',
    action: 'Edit_Sign',
    color: 'success',
  },
  // 未通过
  '2': {
    name: 'Sign_Refused',
    action: 'Edit_Sign',
    color: 'danger',
  },
};

export const QUOTA_TYPE_REVERSE = {
  '0': { name: 'Normal_Quota', color: 'success' },
  '1': { name: 'Female_Quota', color: '#FF99CB' },
  '2': { name: 'Star_Quota', color: 'warning' },
  '3': { name: 'Wildcard_Quota', color: 'danger' },
};

export const RULE_TYPE = {
  ACM: 0,
  OI: 1,
  NewACM: 2,
  NewOI: 3,
  OJ: 4,
  NewOJ: 5,
  Code: 6,
  EXAM: 5,
};

export const CONTEST_TYPE_REVERSE = {
  '0': {
    name: 'Public',
    color: 'success',
    tips: 'Public_Tips',
    submit: true, // 公开赛可看可提交
    look: true,
  },
  '1': {
    name: 'Private',
    color: 'danger',
    tips: 'Private_Tips',
    submit: false, // 私有赛 必须要密码才能看和提交
    look: false,
  },
  '2': {
    name: 'Protected',
    color: 'warning',
    tips: 'Protected_Tips',
    submit: false, //保护赛，可以看但是不能提交，提交需要附带比赛密码
    look: true,
  },
  3: {
    name: 'Official',
    color: '',
    tips: 'Official_Tips',
    submit: false, // 正式赛 必须要报名后才能看和提交
    look: false,
  },
  4: {
    name: 'Synchronous',
    color: 'warning',
    tips: 'Synchronous_Tips',
    submit: false, // 同步赛 根据比赛要求可能需要密码才能看和提交
    look: false,
  },
  5: {
    name: 'Examination',
    color: 'info',
    tips: 'Examination_Tips',
    submit: false, // 考试 根据比赛要求可能需要密码才能看和提交
    look: false,
  },
};

export const CONTEST_TYPE = {
  PUBLIC: 0,
  PRIVATE: 1,
  PROTECTED: 2,
  OFFICIAL: 3,
  SYNCHRONOUS: 4,
  EXAMINATION: 5,
};

export const USER_TYPE = {
  REGULAR_USER: 'user',
  ADMIN: 'admin',
  PROBLEM_ADMIN: 'problem_admin',
  SUPER_ADMIN: 'root',
  COACH_ADMIN: 'coah_admin',
};

export const JUDGE_CASE_MODE = {
  DEFAULT: 'default',
  SUBTASK_LOWEST: 'subtask_lowest',
  SUBTASK_AVERAGE: 'subtask_average',
  ERGODIC_WITHOUT_ERROR: 'ergodic_without_error',
};

export const FOCUS_MODE_ROUTE_NAME = {
  TrainingFullProblemDetails: 'TrainingProblemDetails',
  ContestFullProblemDetails: 'ContestProblemDetails',
  ExamFullProblemDetails: 'ExamProblemDetails',
  GroupTrainingFullProblemDetails: 'GroupTrainingProblemDetails',
  GroupContestFullProblemDetails: 'GroupContestProblemDetails',
};

export const STORAGE_KEY = {
  AUTHED: 'authed',
  PROBLEM_CODE_AND_SETTING: 'hojProblemCodeAndSetting',
  languages: 'languages',
  CONTEST_ANNOUNCE: 'hojContestAnnounce',
  individualLanguageAndSetting: 'hojIndividualLanguageAndSetting',
  CONTEST_RANK_CONCERNED: 'hojContestRankConcerned',
};

export function buildIndividualLanguageAndSettingKey() {
  return `${STORAGE_KEY.individualLanguageAndSetting}`;
}

export function buildProblemCodeAndSettingKey(problemID, contestID = null) {
  if (contestID) {
    return `${STORAGE_KEY.PROBLEM_CODE_AND_SETTING}_${contestID}_${problemID}`;
  }
  return `${STORAGE_KEY.PROBLEM_CODE_AND_SETTING}_NoContest_${problemID}`;
}

export function buildContestAnnounceKey(uid, contestID) {
  return `${STORAGE_KEY.CONTEST_ANNOUNCE}_${uid}_${contestID}`;
}

export function buildContestRankConcernedKey(contestID) {
  return `${STORAGE_KEY.CONTEST_RANK_CONCERNED}_${contestID}`;
}
