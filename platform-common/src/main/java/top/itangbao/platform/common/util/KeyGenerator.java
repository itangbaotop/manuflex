package top.itangbao.platform.common.util;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.util.Base64;

public class KeyGenerator {

    public static void main(String[] args) {
        byte[] keyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        System.out.println("Generated JWT Secret (Base64 for HS512, >= 512 bits): " + base64Key);
        System.out.println("Key length in bits: " + (keyBytes.length * 8));
    }
}
