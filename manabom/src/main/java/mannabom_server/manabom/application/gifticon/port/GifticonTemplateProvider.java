package mannabom_server.manabom.application.gifticon.port;

import mannabom_server.manabom.domain.gifticon.vo.GifticonTemplateSnapshot;

import java.util.List;

public interface GifticonTemplateProvider {

    List<GifticonTemplateSnapshot> findAliveTemplates();
}
