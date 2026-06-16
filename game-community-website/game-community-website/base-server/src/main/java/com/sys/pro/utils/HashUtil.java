package com.sys.pro.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * HashUtil 提供Hash相关工具方法，供多个业务模块复用。
 */
public class HashUtil {

    /**
     * 完成HashUtil中的 createMd5Hash 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param word word 字段，来源于当前接口入参或内部调用上下文。
     * @return HashUtil处理后的文本结果。
     */
    public static String createMd5Hash(String word) {
        try {
            MessageDigest firstDigest = MessageDigest.getInstance("MD5");
            firstDigest.update(word.getBytes(StandardCharsets.UTF_8));
            firstDigest.update(word.getBytes(StandardCharsets.UTF_8));
            byte[] current = firstDigest.digest();
            for (int i = 1; i < 1200; i++) {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(current);
                current = digest.digest();
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : current) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            /**
             * 完成HashUtil中的 IllegalStateException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @param e e 字段，来源于当前接口入参或内部调用上下文。
             * @return HashUtil在该步骤产出的业务结果。
             */
            throw new IllegalStateException("历史MD5密码校验失败", e);
        }
    }

    /**
     * 完成HashUtil中的 calculateLevel 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param exp exp 字段，来源于当前接口入参或内部调用上下文。
     * @return HashUtil统计值或主键结果。
     */
    public static int calculateLevel(int exp) {
        if (exp <= 0) {
            return 0;
        }
        return Math.min(exp / 100, 10);
    }

    /**
     * 完成HashUtil中的 calculateLevelProgress 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param exp exp 字段，来源于当前接口入参或内部调用上下文。
     * @return HashUtil统计值或主键结果。
     */
    public static int calculateLevelProgress(int exp) {
        if (exp <= 0) {
            return 0;
        }
        if (calculateLevel(exp) >= 10) {
            return 100;
        }
        return exp % 100;
    }
}
