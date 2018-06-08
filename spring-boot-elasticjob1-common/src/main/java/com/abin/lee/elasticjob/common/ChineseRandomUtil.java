package com.abin.lee.elasticjob.common;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

/**
 * Created by abin on 2018/2/28 17:01.
 * sharding-dbtable
 * com.abin.lee.sharding.dbtable.common.util
 */
public class ChineseRandomUtil {
    public static void main(String[] args) {
        System.out.println(RandomStringUtils.randomGraph(5));
        System.out.println(RandomStringUtils.randomAlphabetic(5));
        System.out.println(RandomStringUtils.randomAlphanumeric(5));
        System.out.println(RandomStringUtils.randomPrint(5));
        for (int i = 1; i < 24; i++) {
            System.out.print(getRandomChar() + "  ");
        }
    }

    public static String getSpecifiedChinese(int size){
        StringBuffer buffer = new StringBuffer();
        for (int i = 1; i < 24; i++) {
            buffer.append(getRandomChar());
        }
        return buffer.toString();
    }
    private static char getRandomChar() {
        String str = "";
        int hightPos; //
        int lowPos;

        Random random = new Random();

        hightPos = (176 + Math.abs(random.nextInt(39)));
        lowPos = (161 + Math.abs(random.nextInt(93)));

        byte[] b = new byte[2];
        b[0] = (Integer.valueOf(hightPos)).byteValue();
        b[1] = (Integer.valueOf(lowPos)).byteValue();

        try {
//            str = new String(b, Consts.UTF_8);
            str = new String(b, "GBK");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("错误");
        }

        return str.charAt(0);
    }
}
