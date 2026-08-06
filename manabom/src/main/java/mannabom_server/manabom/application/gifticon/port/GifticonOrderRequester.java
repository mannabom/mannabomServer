package mannabom_server.manabom.application.gifticon.port;

import mannabom_server.manabom.application.gifticon.port.command.GifticonOrderCommand;

public interface GifticonOrderRequester {

    void requestGift(GifticonOrderCommand command);
}
