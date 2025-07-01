package cn.com.undefined.abdap_backend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 密码工具类
 * 提供密码加密、验证、生成等功能
 */
@Component
public class PasswordUtil {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // 密码强度验证正则表达式
    private static final String PASSWORD_PATTERN = 
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
    
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    
    /**
     * 加密密码
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * 验证密码
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * 验证密码强度
     * 要求：至少8位，包含大小写字母、数字和特殊字符
     */
    public boolean isValidPassword(String password) {
        return pattern.matcher(password).matches();
    }
    
    /**
     * 生成随机密码
     */
    public String generateRandomPassword(int length) {
        if (length < 8) {
            length = 8; // 最小长度为8
        }
        
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialCharacters = "@#$%^&+=";
        String combinedChars = upperCaseLetters + lowerCaseLetters + numbers + specialCharacters;
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        // 确保包含每种类型的字符
        password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
        password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialCharacters.charAt(random.nextInt(specialCharacters.length())));
        
        // 填充剩余长度
        for (int i = 4; i < length; i++) {
            password.append(combinedChars.charAt(random.nextInt(combinedChars.length())));
        }
        
        // 打乱字符顺序
        return shuffleString(password.toString());
    }
    
    /**
     * 打乱字符串顺序
     */
    private String shuffleString(String string) {
        char[] chars = string.toCharArray();
        SecureRandom random = new SecureRandom();
        
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
    
    /**
     * 获取密码强度描述
     */
    public String getPasswordStrengthDescription(String password) {
        if (password == null || password.length() < 6) {
            return "弱";
        }
        
        int score = 0;
        
        // 长度检查
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        
        // 字符类型检查
        if (password.matches(".*[a-z].*")) score++; // 小写字母
        if (password.matches(".*[A-Z].*")) score++; // 大写字母
        if (password.matches(".*\\d.*")) score++;   // 数字
        if (password.matches(".*[@#$%^&+=].*")) score++; // 特殊字符
        
        if (score <= 2) return "弱";
        if (score <= 4) return "中";
        return "强";
    }
}
