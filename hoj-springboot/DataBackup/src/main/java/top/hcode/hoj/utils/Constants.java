package top.hcode.hoj.utils;

import top.hcode.hoj.utils.SpringContextUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2021/1/1 13:00
 * @Description: 常量枚举类
 */
public class Constants {

    /**
     * @Description 提交评测结果的状态码
     * @Since 2021/1/1
     */
    public enum Judge {
        STATUS_NOT_SUBMITTED(-10, "Not Submitted", null),
        STATUS_SUBMITTED_UNKNOWN_RESULT(-5, "Submitted Unknown Result", null),
        STATUS_CANCELLED(-4, "Cancelled", "ca"),
        STATUS_PRESENTATION_ERROR(-3, "Presentation Error", "pe"),
        STATUS_COMPILE_ERROR(-2, "Compile Error", "ce"),
        STATUS_WRONG_ANSWER(-1, "Wrong Answer", "wa"),
        STATUS_ACCEPTED(0, "Accepted", "ac"),
        STATUS_TIME_LIMIT_EXCEEDED(1, "Time Limit Exceeded", "tle"),
        STATUS_MEMORY_LIMIT_EXCEEDED(2, "Memory Limit Exceeded", "mle"),
        STATUS_RUNTIME_ERROR(3, "Runtime Error", "re"),
        STATUS_SYSTEM_ERROR(4, "System Error", "se"),
        STATUS_PENDING(5, "Pending", null),
        STATUS_COMPILING(6, "Compiling", null),
        STATUS_JUDGING(7, "Judging", null),
        STATUS_PARTIAL_ACCEPTED(8, "Partial Accepted", "pa"),
        STATUS_SUBMITTING(9, "Submitting", null),
        STATUS_SUBMITTED_FAILED(10, "Submitted Failed", null),
        STATUS_NULL(15, "No Status", null),
        JUDGE_SERVER_SUBMIT_PREFIX(-1002, "Judge SubmitId-ServerId:", null);

        private Judge(Integer status, String name, String columnName) {
            this.status = status;
            this.name = name;
            this.columnName = columnName;
        }

        private final Integer status;
        private final String name;
        private final String columnName;

        public Integer getStatus() {
            return status;
        }

        public String getName() {
            return name;
        }

        public String getColumnName() {
            return columnName;
        }
    }

    /**
     * 等待判题的redis队列
     *
     * @Since 2021/12/22
     */

    public enum Queue {
        CONTEST_JUDGE_WAITING("Contest_Waiting_Handle_Queue"),
        GENERAL_JUDGE_WAITING("General_Waiting_Handle_Queue"),
        TEST_JUDGE_WAITING("Test_Judge_Waiting_Handle_Queue"),
        CONTEST_REMOTE_JUDGE_WAITING_HANDLE("Contest_Remote_Waiting_Handle_Queue"),
        GENERAL_REMOTE_JUDGE_WAITING_HANDLE("General_Remote_Waiting_Handle_Queue");

        private Queue(String name) {
            this.name = name;
        }

        private final String name;

        public String getName() {
            return name;
        }
    }

    public enum RemoteOJ {
        HDU("HDU"),
        CODEFORCES("CF"),
        GYM("GYM"),
        POJ("POJ"),
        SPOJ("SPOJ"),
        ATCODER("AC"),
        LIBRE("LIBRE"),
        SCPC("SCPC"),
        QOJ("QOJ"),
        NSWOJ("NSWOJ"),
        NEWOJ("NEWOJ"),
        VJ("VJ"),
        DOTCPP("DOTCPP"),
        NOWCODER("NOWCODER"),
        ACWING("ACWING"),
        MOSS("MOSS");

        private final String name;

        private RemoteOJ(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Boolean isRemoteOJ(String name) {
            for (RemoteOJ remoteOJ : RemoteOJ.values()) {
                if (remoteOJ.getName().equals(name)) {
                    return true;
                }
            }
            return false;

        }

        public static RemoteOJ getRemoteOJ(String name) {
            for (RemoteOJ remoteOJ : RemoteOJ.values()) {
                if (remoteOJ.getName().equals(name)) {
                    return remoteOJ;
                }
            }
            return null;
        }

        public static List<RemoteOJ> getRemoteOJList() {
            return Arrays.asList(HDU, CODEFORCES, POJ, GYM, ATCODER, SPOJ, LIBRE, SCPC, QOJ, NSWOJ, NEWOJ, VJ, DOTCPP);
        }
    }

    /**
     * @Description 比赛相关的常量
     * @Since 2021/1/7
     */
    public enum Contest {
        TYPE_ACM(0, "ACM"),
        TYPE_OI(1, "OI"),
        TYPE_NEWACM(2, "NEWACM"),
        TYPE_NEWOI(3, "NEWOI"),
        TYPE_OJ(4, "OJ"),
        TYPE_NEWOJ(5, "NEWOJ"),
        TYPE_CODE(6, "CODE"),
        TYPE_EXAM(5, "EXAM"),

        STATUS_SCHEDULED(-1, "Scheduled"),
        STATUS_RUNNING(0, "Running"),
        STATUS_ENDED(1, "Ended"),

        AUTH_PUBLIC(0, "Public"),
        AUTH_PRIVATE(1, "Private"),
        AUTH_PROTECT(2, "Protect"),
        AUTH_OFFICIAL(3, "Official"),
        AUTH_SYNCHRONOUS(4, "Synchronous"),
        AUTH_EXAMINATION(5, "Examination"),

        RECORD_NOT_AC_PENALTY(-1, "未AC通过算罚时"),
        RECORD_NOT_AC_NOT_PENALTY(0, "未AC通过不罚时"),
        RECORD_AC(1, "AC通过"),

        OI_CONTEST_RANK_CACHE(null, "oi_contest_rank_cache"),

        CONTEST_RANK_CAL_RESULT_CACHE(null, "contest_rank_cal_result_cache"),

        OI_RANK_RECENT_SCORE(null, "Recent"),
        OI_RANK_HIGHEST_SCORE(null, "Highest");

        private final Integer code;
        private final String name;

        Contest(Integer code, String name) {
            this.code = code;
            this.name = name;
        }

        public Integer getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

    public enum Group {
        PUBLIC(1),
        PROTECTED(2),
        PRIVATE(3),
        PROPOSITION(4);

        private final Integer auth;

        Group(Integer auth) {
            this.auth = auth;
        }

        public Integer getAuth() {
            return auth;
        }

        public static Group getGroup(Integer auth) {
            for (Group group : Group.values()) {
                if (group.getAuth().equals(auth)) {
                    return group;
                }
            }
            return null;
        }
    }

    public enum GroupNemberAuth {
        APPLYING(1),
        REFUSED(2),
        COMMON(3),
        ADMIN(4),
        SUPERADMIN(5);

        private final Integer auth;

        GroupNemberAuth(Integer auth) {
            this.auth = auth;
        }

        public Integer getAuth() {
            return auth;
        }
    }

    /**
     * @Description 账户相关常量
     * @Since 2021/1/8
     */

    public enum Account {
        CODE_CHANGE_PASSWORD_FAIL("change-password-fail:"),
        CODE_CHANGE_PASSWORD_LOCK("change-password-lock:"),
        CODE_ACCOUNT_LOCK("account-lock:"),
        CODE_CHANGE_EMAIL_FAIL("change-email-fail:"),
        CODE_CHANGE_EMAIL_LOCK("change-email-lock:"),

        TRY_LOGIN_NUM("try-login-num:"),

        ACM_RANK_CACHE("acm_rank_cache"),
        OI_RANK_CACHE("oi_rank_cache"),

        NEW_ACM_RANK_CACHE("new_acm_rank_cache"),
        NEW_OI_RANK_CACHE("new_oi_rank_cache"),

        OJ_RANK_CACHE("oj_rank_cache"),
        NEW_OJ_RANK_CACHE("new_oj_rank_cache"),

        STATISTIC_RANK_DIR("statistic_rank_dir"),
        STATISTIC_RANK_TITLE("statistic_rank_title"),
        STATISTIC_RANK_CACHE("statistic_rank_cache"),
        CODE_RANK_CACHE("code_rank_cache"),

        GROUP_RANK_CACHE("group_rank_cache"),

        SUPER_ADMIN_UID_LIST_CACHE("super_admin_uid_list_case"),

        SUBMIT_NON_CONTEST_LOCK("submit_non_contest_lock:"),
        TEST_JUDGE_LOCK("test_judge_lock:"),
        SUBMIT_CONTEST_LOCK("submit_contest_lock:"),
        DISCUSSION_ADD_NUM_LOCK("discussion_add_num_lock:"),
        GROUP_ADD_NUM_LOCK("group_add_num_lock"),
        CONTEST_ADD_PRINT_LOCK("contest_add_print_lock:"),
        CONTEST_INVENT_LOCK("contest_invent_lock:"),
        CONTEST_SIGN_LOCK("contest_sign_lock:"),

        REMOTE_JUDGE_CF_ACCOUNT_NUM("remote_judge_cf_account:");

        private final String code;

        Account(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * @Description 文件操作的一些常量
     * @Since 2021/1/10
     */
    public enum File {

        USER_AVATAR_FOLDER("/hoj/file/avatar"),

        GROUP_AVATAR_FOLDER("/hoj/file/avatar/group"),

        HOME_CAROUSEL_FOLDER("/hoj/file/carousel"),

        MARKDOWN_FILE_FOLDER("/hoj/file/md"),

        PROBLEM_FILE_FOLDER("/hoj/file/problem"),

        CONTEST_TEXT_PRINT_FOLDER("/hoj/file/contest_print"),

        IMG_API("/api/public/img/"),

        FILE_API("/api/public/file/"),

        TESTCASE_TMP_FOLDER("/hoj/file/zip"),

        BOXFILE_BASE_FOLDER("/hoj/file/file"),

        SCHOOL_BASE_FOLDER("/hoj/file/school"),

        MOSS_BASE_FOLDER("/hoj/file/moss"),

        TESTCASE_BASE_FOLDER("/hoj/testcase"),

        FILE_DOWNLOAD_TMP_FOLDER("/hoj/file/zip/download"),

        CONTEST_AC_SUBMISSION_TMP_FOLDER("/hoj/file/zip/contest_ac"),

        CODE_FOLDER("/hoj/file/code"),

        // docker 容器中的映像路径
        DOCKER_PROBLEM_FILE_FOLDER("/tmp/htmltopdf"),

        DOCKER_CODE_FOLDE("/tmp/code"),

        DOCKER_CERT_PATH("/tmp/docker/certs.d");

        private final String path;

        File(String path) {
            this.path = path;
        }

        public String getPath() {
            try {
                // 获取当前的环境
                String env = SpringContextUtil.getActiveProfile();

                if (env.equals("dev") && path.startsWith("/hoj")) {
                    // 判断系统
                    Boolean isLinux = isLinux();

                    if (!isLinux) {
                        String winPath = "\\\\wsl.localhost\\Ubuntu-22.04\\home\\dym\\workspace\\hoj";
                        return winPath + path.replace("/", "\\");
                    } else {
                        String linuxPath = "/home/dym/hoj";
                        return linuxPath + path;
                    }
                } else {
                    return path;
                }
            } catch (Exception e) {
                return path;
            }
        }

        public static Boolean isLinux() {
            String osName = System.getProperty("os.name").toLowerCase();

            // 判断操作系统类型
            if (osName.contains("win")) {
                return false;
            }
            return true;
        }

    }

    /**
     * @Description 邮件任务的一些常量
     * @Since 2021/1/14
     */

    public enum Email {

        OJ_URL("OJ_UR"),
        OJ_NAME("OJ_NAME"),
        OJ_SHORT_NAME("OJ_SHORT_NAME"),
        EMAIL_FROM("EMAIL_FROM"),
        EMAIL_BACKGROUND_IMG("EMAIL_BACKGROUND_IMG"),
        REGISTER_KEY_PREFIX("register-user:"),
        CHANGE_EMAIL_KEY_PREFIX("change-email:"),
        RESET_PASSWORD_KEY_PREFIX("reset-password:"),
        RESET_EMAIL_LOCK("reset-email-lock:"),
        REGISTER_EMAIL_LOCK("register-email-lock:"),
        CHANGE_EMAIL_LOCK("change-email-lock:");

        private final String value;

        Email(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Schedule {
        RECENT_OTHER_CONTEST("recent-other-contest"),

        CHECK_REMOTE_JUDGE("check-remote-judge");

        private final String code;

        Schedule(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;

        }
    }

    /**
     * @Description 训练题单的一些常量
     * @Since 2021/11/20
     */
    public enum Training {

        AUTH_PRIVATE("Private"),
        AUTH_PUBLIC("Public");

        private final String value;

        Training(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ProblemType {
        ACM(0),
        OI(1),
        Selection(2),
        Filling(3),
        Decide(4);

        private final Integer type;

        ProblemType(Integer type) {
            this.type = type;
        }

        public Integer getType() {
            return type;
        }

        public static ProblemType getProblemType(Integer type) {
            for (ProblemType problemType : ProblemType.values()) {
                if (problemType.getType().equals(type)) {
                    return problemType;
                }
            }
            return null;
        }
    }

    public enum ProblemAuth {
        PUBLIC(1),
        PRIVATE(2),
        CONTEST(3);

        private final Integer auth;

        ProblemAuth(Integer auth) {
            this.auth = auth;
        }

        public Integer getAuth() {
            return auth;
        }

        public static ProblemAuth getProblemAuth(Integer auth) {
            for (ProblemAuth problemAuth : ProblemAuth.values()) {
                if (problemAuth.getAuth().equals(auth)) {
                    return problemAuth;
                }
            }
            return null;
        }
    }

    public enum JudgeMode {
        DEFAULT("default"),
        SPJ("spj"),
        INTERACTIVE("interactive");

        private final String mode;

        JudgeMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }

        public static JudgeMode getJudgeMode(String mode) {
            for (JudgeMode judgeMode : JudgeMode.values()) {
                if (judgeMode.getMode().equals(mode)) {
                    return judgeMode;
                }
            }
            return null;
        }
    }

    public enum JudgeCaseMode {
        DEFAULT("default"),
        SUBTASK_LOWEST("subtask_lowest"),
        SUBTASK_AVERAGE("subtask_average"),
        ERGODIC_WITHOUT_ERROR("ergodic_without_error");

        private final String mode;

        JudgeCaseMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }

        public static JudgeCaseMode getJudgeCaseMode(String mode) {
            for (JudgeCaseMode judgeCaseMode : JudgeCaseMode.values()) {
                if (judgeCaseMode.getMode().equals(mode)) {
                    return judgeCaseMode;
                }
            }
            return DEFAULT;
        }
    }

    public enum TaskType {
        /**
         * 自身评测
         */
        JUDGE("/judge"),
        /**
         * 远程评测
         */
        REMOTE_JUDGE("/remote-judge"),

        /**
         * 在线调试
         */
        TEST_JUDGE("/test-judge"),
        /**
         * 编译特判程序
         */
        COMPILE_SPJ("/compile-spj"),

        /**
         * 编译交互程序
         */
        COMPILE_INTERACTIVE("/compile-interactive"),

        /**
         * 判题机docker管理
         */
        DOCKER("/docker");

        private final String path;

        TaskType(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

    }

    public enum SCPC {
        HOST("http://scpc.fun");

        private final String mode;

        SCPC(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }
    }
}