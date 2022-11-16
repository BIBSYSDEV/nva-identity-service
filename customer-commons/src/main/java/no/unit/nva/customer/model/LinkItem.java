package no.unit.nva.customer.model;

import static nva.commons.core.attempt.Try.attempt;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

public class LinkItem {

    private String href;

    public LinkItem() {
    }

    public LinkItem(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public static LinkItem fromUrl(URL url) {
        return new LinkItem(url.toString());
    }

    public static LinkItem fromString(String url) throws MalformedURLException {
        return fromUrl(URI.create(url).toURL());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
