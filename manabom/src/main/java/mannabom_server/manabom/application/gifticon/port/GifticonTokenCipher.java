package mannabom_server.manabom.application.gifticon.port;

public interface GifticonTokenCipher {

    String encrypt(String plainToken);

    String decrypt(String encryptedToken);
}
