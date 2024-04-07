package sample;

import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.util.*;

import static sample.LoginController.currentUser;

public class Crypt {

    static Map<String, Key> symKeys = new HashMap<>();

    public static void encipher(String message, String recipient) throws Exception{
        Key key = symKeys.get(recipient);
        System.out.println(key.getAlgorithm());
        Cipher cipher = Cipher.getInstance(key.getAlgorithm()+"/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,key);
        byte[] cipherText = cipher.doFinal(message.getBytes());
        Random random = new Random();
        String suffix = "-"+ random.nextInt(100 )+ random.nextInt(100);
        PrintWriter pw = new PrintWriter("inbox"+ File.separator+getHash(recipient)+File.separator+currentUser+suffix+".txt");
        pw.print(Base64.getEncoder().encodeToString(cipherText)+"@"+Crypt.getSignature(message," "));
        pw.close();
    }
    public static String decipher(String message, String sender)  {
        byte[] cipherText = Base64.getDecoder().decode(message);
        String clearText = "";
        Key key = symKeys.get(sender);
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm()+"/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            clearText = new String(cipher.doFinal(cipherText));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return clearText;
    }

    public static String decryptRSA(String message) throws Exception{
        PEMParser pemParser = new PEMParser(new FileReader("inbox"+ File.separator+getHash(currentUser)
                +File.separator+"keys"+File.separator+"rsaKey.pem"));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair = converter.getKeyPair((PEMKeyPair) pemParser.readObject());
        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding","BC");
        cipher.init(Cipher.DECRYPT_MODE,keyPair.getPrivate());
        return new String(cipher.doFinal(Base64.getDecoder().decode(message)), StandardCharsets.UTF_8);

    }
    public static String encryptRSA(String message, String recipient) throws Exception{

        X509Certificate recipientCert = loadCert("certs" + File.separator + recipient + ".pem");
        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, recipientCert.getPublicKey());
        return new String(Base64.getEncoder().encode(cipher.doFinal(message.getBytes(StandardCharsets.UTF_8))));
    }
    public static X509Certificate loadCert(String filename) throws Exception{
        PEMParser pemParser = new PEMParser(new FileReader(filename));
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        return converter.setProvider("BC").getCertificate((X509CertificateHolder) pemParser.readObject());
    }
    public static X509CRL loadCRL(String filename) throws Exception{
        PEMParser pemParser = new PEMParser(new FileReader(filename));
        JcaX509CRLConverter converter = new JcaX509CRLConverter();
        return converter.setProvider("BC").getCRL((X509CRLHolder) pemParser.readObject());
    }
    public static String genSymParam() throws Exception{
        String[] algorithms = {"AES","Twofish","RC6"};
        Random rand = new Random();
        String algorithm = algorithms[rand.nextInt(3)];
        KeyGenerator keygen = KeyGenerator.getInstance(algorithm);
        keygen.init(128);
        Key key = keygen.generateKey();
        System.out.println(algorithm+"@"+Base64.getEncoder().encodeToString(key.getEncoded()));
        return algorithm+"@"+Base64.getEncoder().encodeToString(key.getEncoded());

    }
    public static String getSignature(String message, String digest) throws Exception {
        if(" ".equals(digest)){
            String[] digests = {"SHA256withRSA","SHA512withRSA","SHA3-256withRSA"};
            digest = digests[new Random().nextInt(3)];
        }
        PEMParser pemParser = new PEMParser(new FileReader("inbox"+ File.separator+getHash(currentUser)
                +File.separator+"keys"+File.separator+"rsaKey.pem"));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair = converter.getKeyPair((PEMKeyPair) pemParser.readObject());
        Signature rsaSignature = Signature.getInstance(digest);
        rsaSignature.initSign(keyPair.getPrivate());
        rsaSignature.update(message.getBytes());
        byte[] signature = rsaSignature.sign();
        return Base64.getEncoder().encodeToString(signature)+"@"+digest;
    }
    public static boolean isCertificateValid(String user) throws Exception {
        X509Certificate caCert = loadCert("certs"+ File.separator+"ca.pem");
        X509Certificate senderCert = loadCert("certs"+ File.separator+user+".pem");
        X509CRL crl = loadCRL("certs"+ File.separator+"crl.pem");
        if(!("CN="+user).equals(senderCert.getSubjectX500Principal().getName().split(",")[1]))
            return false;
        try {
            senderCert.checkValidity();
            senderCert.verify(caCert.getPublicKey());
            crl.verify(caCert.getPublicKey());
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
            e.printStackTrace();
            return false;
        }
        return !crl.isRevoked(senderCert);
    }
    public static boolean isSignatureValid(String signature, String message, String digest, String sender) throws Exception {
        Signature rsaSignature = Signature.getInstance(digest);
        rsaSignature.initVerify(loadCert("certs"+File.separator+sender+".pem"));
        rsaSignature.update(message.getBytes());
        return rsaSignature.verify(Base64.getDecoder().decode(signature)) && isCertificateValid(sender);
    }
    public static boolean isPassCorrect(String hash,String password){
        SHA3.DigestSHA3 digest = new SHA3.DigestSHA3(256);
        return hash.equals(Hex.toHexString(digest.digest(password.getBytes())));
    }
    public static String getHash(String text){
        SHA3.DigestSHA3 digest = new SHA3.DigestSHA3(256);
        return Hex.toHexString(digest.digest(text.getBytes()));
    }
}
