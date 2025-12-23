package top.itangbao.platform.common.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * RSA 密钥对生成工具
 * 用于生成 RS256 算法所需的公钥和私钥（Base64 格式）
 */
public class RsaKeyGenerator {

    public static void main(String[] args) {
        try {
            // 1. 初始化密钥对生成器，指定 RSA 算法
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            // 2. 设置密钥长度为 2048 位（生产环境推荐长度）
            keyPairGenerator.initialize(2048);

            // 3. 生成密钥对
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // 4. 获取私钥并进行 Base64 编码 (PKCS#8 格式)
            String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            // 5. 获取公钥并进行 Base64 编码 (X.509 格式)
            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            // 6. 输出结果
            System.out.println("==================== RSA KEY PAIR GENERATED ====================");
            System.out.println("核心提示：私钥(Private Key) 必须严格保密，公钥(Public Key) 用于验签。");
            System.out.println();
            System.out.println("--- PRIVATE KEY (Base64) ---");
            System.out.println(privateKeyBase64);
            System.out.println();
            System.out.println("--- PUBLIC KEY (Base64) ---");
            System.out.println(publicKeyBase64);
            System.out.println("================================================================");

        } catch (NoSuchAlgorithmException e) {
            System.err.println("错误：系统不支持 RSA 算法生成 - " + e.getMessage());
        }
    }
}