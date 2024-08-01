package top.hcode.hoj.utils;

import java.security.MessageDigest;
import java.util.Random;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import cn.hutool.crypto.SecureUtil;

@Component
public class Md5Utils {

    /**
     * 生成盐和加盐后的MD5码，并将盐混入到MD5码中,对MD5密码进行加强
     *
     * @param password 原始密码
     * @return 加盐的MD5字符串
     *
     */
    public static String generateSaltPassword(String password) {

        Random random = new Random();

        // 生成一个16位的随机数，也就是所谓的盐
        StringBuilder stringBuilder = new StringBuilder(16);
        stringBuilder.append(random.nextInt(99999999)).append(random.nextInt(99999999));
        int len = stringBuilder.length();
        if (len < 16) {
            for (int i = 0; i < 16 - len; i++) {
                stringBuilder.append("0");
            }
        }
        String salt = stringBuilder.toString();

        // 将盐加到明文中，并生成新的MD5码
        password = md5Hex(password + salt);

        // 将盐混到新生成的MD5码中，之所以这样做是为了后期更方便的校验明文和秘文
        char[] cs = new char[48];
        for (int i = 0; i < 48; i += 3) {
            cs[i] = password.charAt(i / 3 * 2);
            char c = salt.charAt(i / 3);
            cs[i + 1] = c;
            cs[i + 2] = password.charAt(i / 3 * 2 + 1);
        }
        return new String(cs);
    }

    /**
     * 生成盐和加盐后的MD5码，并将盐混入到MD5码中,对MD5密码进行加强
     *
     * @param password 原始密码
     * @return 加盐的MD5字符串
     *
     */
    public static String generateSaltMD5Password(String password) {
        // md5 加密
        password = SecureUtil.md5(password);

        Random random = new Random();

        // 生成一个16位的随机数，也就是所谓的盐
        StringBuilder stringBuilder = new StringBuilder(16);
        stringBuilder.append(random.nextInt(99999999)).append(random.nextInt(99999999));
        int len = stringBuilder.length();
        if (len < 16) {
            for (int i = 0; i < 16 - len; i++) {
                stringBuilder.append("0");
            }
        }
        String salt = stringBuilder.toString();

        // 将盐加到明文中，并生成新的MD5码
        password = md5Hex(password + salt);

        // 将盐混到新生成的MD5码中，之所以这样做是为了后期更方便的校验明文和秘文
        char[] cs = new char[48];
        for (int i = 0; i < 48; i += 3) {
            cs[i] = password.charAt(i / 3 * 2);
            char c = salt.charAt(i / 3);
            cs[i + 1] = c;
            cs[i + 2] = password.charAt(i / 3 * 2 + 1);
        }
        return new String(cs);
    }

    /**
     * 验证明文和加盐后的MD5码是否匹配
     *
     * @param password 原始密码
     * @param md5      加盐md5密码
     * @return 加盐的MD5字符串
     *
     */
    public static boolean verifySaltPassword(String password, String md5) {
        // md5 加密
        password = SecureUtil.md5(password);

        // 先从MD5码中取出之前加的盐和加盐后生成的MD5码
        char[] cs1 = new char[32];
        char[] cs2 = new char[16];
        for (int i = 0; i < 48; i += 3) {
            cs1[i / 3 * 2] = md5.charAt(i);
            cs1[i / 3 * 2 + 1] = md5.charAt(i + 2);
            cs2[i / 3] = md5.charAt(i + 1);
        }
        String salt = new String(cs2);
        // 比较二者是否相同
        return md5Hex(password + salt).equals(new String(cs1));
    }

    /**
     * 生成MD5密码
     *
     * @param password 原始密码
     * @return MD5字符串
     *
     */
    private static String md5Hex(String src) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bs = md5.digest(src.getBytes());
            return new String(new Hex().encode(bs));
        } catch (Exception e) {
            return null;
        }
    }

}
