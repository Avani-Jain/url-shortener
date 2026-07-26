package com.project.url_shortener.util;

public class Base62Encoder {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int BASE = 62;

    public static String encode(long number){
        if (number==0){
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while(number>0){
            int remainder = (int)(number%BASE);
            sb.append(ALPHABET.charAt(remainder));
            number/=BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String shortCode){
        long result =0;
        for(char ch: shortCode.toCharArray()){
            result = result*BASE + ALPHABET.indexOf(ch); //similar to decimal conversion = "123" = ((0*10 +1)*10 +2)*10+3
        }
        return result;
    }
}
