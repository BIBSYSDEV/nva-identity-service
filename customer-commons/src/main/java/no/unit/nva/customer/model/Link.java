package no.unit.nva.customer.model;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class Link {

    String href;

    public Link() {
    }

    public Link(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    static Link newLink(URL url) {
        return new Link(url.toString());
    }

    public static Link newLink(String url) throws MalformedURLException {
        return newLink(URI.create(url).toURL());
    }
}
