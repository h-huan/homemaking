package com.hm.framework.encrypt;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** API encryption examples use ephemeral keys and never log key material.
 * @author 芋道源码
 */
public class ApiEncryptTest {
    @Test
    public void testGenerateAsymmetric() {
        RSA request = SecureUtil.rsa();
        RSA response = SecureUtil.rsa();
        assertNotEquals(request.getPrivateKeyBase64(), response.getPrivateKeyBase64());
    }
    @Test
    public void testEncrypt_aes() {
        var aes = SecureUtil.aes();
        String message = "HM encryption round trip";
        assertEquals(message, aes.decryptStr(aes.encryptBase64(message)));
    }
    @Test
    public void testEncrypt_rsa() {
        RSA rsa = SecureUtil.rsa();
        String message = "HM encryption round trip";
        assertEquals(message, rsa.decryptStr(rsa.encryptBase64(message, KeyType.PublicKey), KeyType.PrivateKey));
    }
}
