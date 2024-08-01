package top.hcode.hoj.utils;

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
     * 为数据库中所有用户更新密码（多线程处理），如不需要可自行移除此方法。
     *
     * @throws SQLException SQL 异常
     */
    public static void updateUserPasswords() throws SQLException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        try {
            connection.setAutoCommit(false);

            // 从数据库读取已有用户，用于取出其旧密码或做其他处理
            List<PasswordBarVO> userListFromDB = loadExistingUsers(connection);

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
        String selectSQL = "SELECT username, password FROM user_info";
        try (PreparedStatement stmt = connection.prepareStatement(selectSQL);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                String passwordMd5 = rs.getString("password");
                userList.add(new PasswordBarVO(username, passwordMd5));
            }
        }
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

    // public static void main(String[] args) {
    //     try {

    //         // 1. 批量更新数据库中用户密码为 MD5（加盐）后的新密码
    //         updateUserPasswords();

    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
}
