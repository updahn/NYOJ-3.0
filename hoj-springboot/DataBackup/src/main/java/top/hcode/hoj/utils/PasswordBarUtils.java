package top.hcode.hoj.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.sql.Connection;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.pojo.vo.PasswordBarVO;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "hoj")
public class PasswordBarUtils {

    // 数据库 URL，用户名和密码
    private static final String DB_URL = "jdbc:mysql://localhost:3306/hoj";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "hoj123456";
    private static final int THREAD_POOL_SIZE = 10; // 线程数
    private static final int BATCH_SIZE = 1000; // 批量大小

    /**
     * 从 CSV (GBK/ANSI 编码) 文件中读取账号信息，并为每条记录生成随机密码（MD5+Salt）。
     *
     * @param filePath CSV 文件路径（GBK/ANSI 编码）
     * @return 包含账号信息和明文/密文密码的列表
     */
    public static List<PasswordBarVO> readCsvFileGbk(String filePath) {
        List<PasswordBarVO> resultList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "GBK"))) {

            String line;
            boolean skipHeader = true; // 如果第一行是表头则跳过
            while ((line = br.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }
                // 根据你的 CSV 列来拆分，这里仅作示例
                String[] data = line.split(",");
                String studentName = data[0];
                String className = data[2];
                String account = data[3];

                // 生成随机密码（明文）
                String randomPassword = PasswordUtils.generateRamdomPassword(8);
                // 生成 MD5（加盐）密码
                String saltedMd5Password = Md5Utils.generateSaltMD5Password(randomPassword);

                // 封装到 VO 中
                PasswordBarVO passwordBarVO = new PasswordBarVO(className, studentName, account, randomPassword,
                        saltedMd5Password);
                resultList.add(passwordBarVO);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * 为数据库中所有用户更新密码（多线程处理），如不需要可自行移除此方法。
     *
     * @param newPasswordList 需要更新的用户账号及新密码信息
     * @throws SQLException SQL 异常
     */
    public static void updateUserPasswords(List<PasswordBarVO> newPasswordList) throws SQLException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        try {
            connection.setAutoCommit(false);

            // 从数据库读取已有用户，用于取出其旧密码或做其他处理
            List<PasswordBarVO> userListFromDB = loadExistingUsers(connection);

            // 如果外部传入的新密码列表不为空，则将二者合并
            if (newPasswordList != null && !newPasswordList.isEmpty()) {
                userListFromDB.addAll(newPasswordList);
            }

            // 按线程池大小进行分块处理
            int totalUsers = userListFromDB.size();
            int chunkSize = (totalUsers == 0) ? 0 : totalUsers / THREAD_POOL_SIZE;
            List<Future<Void>> futures = new ArrayList<>();

            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                int start = i * chunkSize;
                int end = (i == THREAD_POOL_SIZE - 1) ? totalUsers : start + chunkSize;
                if (start >= end) {
                    break;
                }
                List<PasswordBarVO> sublist = userListFromDB.subList(start, end);
                futures.add(executor.submit(new PasswordUpdateCallable(sublist)));
            }

            // 等待所有线程执行完毕
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            connection.commit();
            System.out.println("All passwords updated successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            // 回滚事务
            if (connection != null) {
                connection.rollback();
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
            executor.shutdown();
        }
    }

    /**
     * 从数据库中查询已有用户信息（示例：只查询 username 和 password）。
     */
    private static List<PasswordBarVO> loadExistingUsers(Connection connection) throws SQLException {
        List<PasswordBarVO> userList = new ArrayList<>();
        // String selectSQL = "SELECT username, password FROM user_info";
        // try (PreparedStatement stmt = connection.prepareStatement(selectSQL);
        //         ResultSet rs = stmt.executeQuery()) {

        //     while (rs.next()) {
        //         String username = rs.getString("username");
        //         String passwordMd5 = rs.getString("password");
        //         userList.add(new PasswordBarVO(username, passwordMd5));
        //     }
        // }
        return userList;
    }

    /**
     * 用于多线程批量更新密码的任务类。
     */
    private static class PasswordUpdateCallable implements Callable<Void> {
        private final List<PasswordBarVO> passwordBarVoList;

        PasswordUpdateCallable(List<PasswordBarVO> passwordBarVoList) {
            this.passwordBarVoList = passwordBarVoList;
        }

        @Override
        public Void call() throws Exception {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                conn.setAutoCommit(false);

                String updateSQL = "UPDATE user_info SET password = ? WHERE username = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                    int count = 0;
                    for (PasswordBarVO passwordBarVo : passwordBarVoList) {
                        ps.setString(1, passwordBarVo.getPasswordMd5());
                        ps.setString(2, passwordBarVo.getAccount());
                        ps.addBatch();

                        if (++count % BATCH_SIZE == 0) {
                            ps.executeBatch();
                        }
                    }
                    ps.executeBatch();
                    conn.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                    conn.rollback();
                }
            }
            return null;
        }
    }

    /**
     * 将账号信息列表转为 HTML 内容（每人一个表格）。
     *
     * @param passwordBarList 账号信息列表
     * @return 生成的 HTML 字符串
     */
    public static String createHtmlContent(List<PasswordBarVO> passwordBarList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<title>账号信息</title>\n")
                .append("<style>\n")
                .append("  table {\n")
                .append("    width: 100%;\n")
                .append("    table-layout: fixed;\n")
                .append("    border-collapse: collapse;\n")
                .append("    border: 1px solid #000;\n")
                .append("    margin-bottom: 20px;\n")
                .append("  }\n")
                .append("  th, td {\n")
                .append("    border: 1px solid #000;\n")
                .append("    vertical-align: middle;\n")
                .append("    text-align: center;\n")
                .append("    padding: 5px;\n")
                .append("    box-sizing: border-box;\n")
                .append("  }\n")
                .append("  pre {\n")
                .append("    font-size: 15px;\n")
                .append("    margin: 0;\n")
                .append("    font-family: inherit;\n")
                .append("  }\n")
                .append("</style>\n")
                .append("</head>\n<body>\n");

        for (PasswordBarVO vo : passwordBarList) {
            sb.append("<table>\n")
                    .append("  <tr>\n")
                    .append("    <th>班级</th>\n")
                    .append("    <th>姓名</th>\n")
                    .append("    <th>账号</th>\n")
                    .append("    <th>密码</th>\n")
                    .append("  </tr>\n")
                    .append("  <tr>\n")
                    .append("    <td><pre>").append(vo.getCourse() == null ? "" : vo.getCourse()).append("</pre></td>\n")
                    .append("    <td><pre>").append(vo.getName() == null ? "" : vo.getName()).append("</pre></td>\n")
                    .append("    <td><pre>").append(vo.getAccount() == null ? "" : vo.getAccount()).append("</pre></td>\n")
                    .append("    <td><pre>").append(vo.getPassword() == null ? "" : vo.getPassword()).append("</pre></td>\n")
                    .append("  </tr>\n")
                    .append("</table><br/>\n\n");
        }

        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    /**
     * 将 HTML 内容写入到指定文件。
     *
     * @param htmlContent HTML 文本
     * @param filePath    目标文件路径
     */
    private static void saveHtmlToFile(String htmlContent, String filePath) {
        try (PrintWriter writer = new PrintWriter(filePath, "UTF-8")) {
            writer.write(htmlContent);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // public static void main(String[] args) {
    //     try {

    //         // 1. 从 CSV 文件读取数据（GBK/ANSI）
    //         String csvFilePath = "C:/Users/Lenovo/Desktop/新生账号.csv";
    //         List<PasswordBarVO> passwordBarVOList = readCsvFileGbk(csvFilePath);

    //         // 2. 将读取的账号信息生成密码条
    //         String outputFilePath = "C:/Users/Lenovo/Desktop/密码条.html";
    //         String htmlContent = createHtmlContent(passwordBarVOList);
    //         saveHtmlToFile(htmlContent, outputFilePath);

    //         // 3. （可选）批量更新数据库中用户密码为 MD5（加盐）后的新密码
    //         // updateUserPasswords(passwordBarVOList);

    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
}
