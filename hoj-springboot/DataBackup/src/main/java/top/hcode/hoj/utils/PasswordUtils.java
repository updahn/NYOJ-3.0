package top.hcode.hoj.utils;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class PasswordUtils {

    private static final String LOWERCASE_CHARACTERS = "abefghqrt";

    private static final String UPPERCASE_CHARACTERS = "ABEFGHQRT";

    private static final String DIGITS = "23456789";

    private static final String SPECIAL_CHARACTERS = "@#$%&";

    private static final String ALL_CHARACTERS = LOWERCASE_CHARACTERS + UPPERCASE_CHARACTERS + DIGITS;

    /**
     * 生成包含大小写字母、数字和特殊字符的强密码
     *
     * @param length 密码长度
     * @return 生成的密码
     */
    public static String generateRamdomPassword(int length) {

        // 确保至少包含一个小写字母、一个大写字母和一个数字字符以及一个特殊字符 把数量都设置为1
        int lowerCaseCount = 1; // 小写字母数量
        int upperCaseCount = 1; // 大写字母数量
        int numberCount = 1; // 数字数量
        int specialCount = 1; // 特殊字符数量
        StringBuilder randomString = new StringBuilder(length);
        SecureRandom random = new SecureRandom();
        // 随机生成大写字母部分
        for (int i = 0; i < lowerCaseCount; i++) {
            int lowerCaseIndex = random.nextInt(LOWERCASE_CHARACTERS.length());
            randomString.append(LOWERCASE_CHARACTERS.charAt(lowerCaseIndex));
        }
        // 随机生成小写字母部分
        for (int i = 0; i < upperCaseCount; i++) {
            int upperCaseIndex = random.nextInt(UPPERCASE_CHARACTERS.length());
            randomString.append(UPPERCASE_CHARACTERS.charAt(upperCaseIndex));
        }
        // 随机生成数字部分
        for (int i = 0; i < numberCount; i++) {
            int digitsIndex = random.nextInt(DIGITS.length());
            randomString.append(DIGITS.charAt(digitsIndex));
        }
        // 随机生成特殊字符部分
        for (int i = 0; i < specialCount; i++) {
            int specialIndex = random.nextInt(SPECIAL_CHARACTERS.length());
            randomString.append(SPECIAL_CHARACTERS.charAt(specialIndex));
        }
        // 生成剩余的字符
        for (int i = lowerCaseCount + upperCaseCount + numberCount + specialCount; i < length; i++) {
            // 在所有字符里面取剩下的字符
            randomString.append(ALL_CHARACTERS.charAt(random.nextInt(ALL_CHARACTERS.length())));
        }
        // 洗牌字符，使其顺序随机
        return shuffleString(randomString.toString());
    }

    /**
     *
     * @Description: 函数用于洗牌字符串中的字符
     * @param input
     * @return String
     */
    private static String shuffleString(String input) {
        SecureRandom random = new SecureRandom();
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int randomIndex = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[randomIndex];
            characters[randomIndex] = temp;
        }
        return new String(characters);
    }
}
