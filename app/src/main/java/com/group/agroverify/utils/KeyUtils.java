package com.group.agroverify.utils;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class KeyUtils {

    public static PublicKey loadPublicKey(String publicKeyPem) throws Exception {
        String cleanPem = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.decode(cleanPem, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(keySpec);
    }

    public static String extractKid(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format - must have 3 parts");
        }
        byte[] decodedHeader = Base64.decode(parts[0], Base64.URL_SAFE | Base64.NO_WRAP);
        String headerJson = new String(decodedHeader, StandardCharsets.UTF_8);
        JSONObject header = new JSONObject(headerJson);
        return header.getString("kid");
    }

    public static String extractSignature(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format - must have 3 parts");
        }
        return parts[2];
    }

    public static String extractData(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format - must have 3 parts");
        }
        return parts[0] + "." + parts[1];
    }

    public static JSONObject extractPayload(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format - must have 3 parts");
        }
        byte[] decodedPayload = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP);
        String payloadJson = new String(decodedPayload, StandardCharsets.UTF_8);
        return new JSONObject(payloadJson);
    }

    public static boolean verifySignature(String data, String signatureBase64, PublicKey publicKey) {
        try {
            Log.d("VERIFY", "verifySignature: Data: " + data);
            Log.d("VERIFY", "verifySignature: Signature: " + signatureBase64);
            Log.d("VERIFY", "verifySignature: PublicKey: " + publicKey.toString());

            // For ES256 (ECDSA with SHA-256)
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            // Decode the base64 signature using URL-safe Base64 (JWT format)
            byte[] signatureBytes = Base64.decode(signatureBase64, Base64.URL_SAFE | Base64.NO_WRAP);

            // JWT ECDSA signatures are in r||s format (64 bytes for P-256)
            // Java expects DER encoding, so we need to convert
            byte[] derSignature = convertJwtEcdsaToDer(signatureBytes);

            return signature.verify(derSignature);
        } catch (Exception e) {
            Log.e("VERIFY", "verifySignature: Error", e);
            return false;
        }
    }

    private static byte[] convertJwtEcdsaToDer(byte[] jwtSignature) throws Exception {
        if (jwtSignature.length != 64) {
            throw new IllegalArgumentException("JWT ECDSA signature must be 64 bytes for P-256");
        }

        // Split into r and s components (32 bytes each for P-256)
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(jwtSignature, 0, r, 0, 32);
        System.arraycopy(jwtSignature, 32, s, 0, 32);

        // Convert to DER format
        // DER format: 0x30 [total-length] 0x02 [R-length] [R] 0x02 [S-length] [S]

        // Remove leading zeros and ensure positive values
        r = removeLeadingZeros(r);
        s = removeLeadingZeros(s);

        // Add 0x00 prefix if the first bit is set (to ensure positive values)
        if ((r[0] & 0x80) != 0) {
            byte[] temp = new byte[r.length + 1];
            temp[0] = 0x00;
            System.arraycopy(r, 0, temp, 1, r.length);
            r = temp;
        }

        if ((s[0] & 0x80) != 0) {
            byte[] temp = new byte[s.length + 1];
            temp[0] = 0x00;
            System.arraycopy(s, 0, temp, 1, s.length);
            s = temp;
        }

        // Build DER sequence
        int totalLength = 2 + r.length + 2 + s.length;
        byte[] der = new byte[2 + totalLength];

        int offset = 0;
        der[offset++] = 0x30; // SEQUENCE tag
        der[offset++] = (byte) totalLength;

        der[offset++] = 0x02; // INTEGER tag for r
        der[offset++] = (byte) r.length;
        System.arraycopy(r, 0, der, offset, r.length);
        offset += r.length;

        der[offset++] = 0x02; // INTEGER tag for s
        der[offset++] = (byte) s.length;
        System.arraycopy(s, 0, der, offset, s.length);

        return der;
    }

    private static byte[] removeLeadingZeros(byte[] bytes) {
        int start = 0;
        while (start < bytes.length - 1 && bytes[start] == 0) {
            start++;
        }

        if (start == 0) {
            return bytes;
        }

        byte[] result = new byte[bytes.length - start];
        System.arraycopy(bytes, start, result, 0, result.length);
        return result;
    }

    public static JSONObject parseToken(String token) throws Exception {
        byte[] decodedBytes = Base64.decode(token, Base64.DEFAULT);
        String json = new String(decodedBytes, StandardCharsets.UTF_8);
        return new JSONObject(json);
    }
}
