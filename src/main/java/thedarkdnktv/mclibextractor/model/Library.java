package thedarkdnktv.mclibextractor.model;

import java.nio.file.Path;

public class Library {

    private final String name;
    private Path path;
    private String url;
    private boolean isNative = false;

    public Library(String name) {
        this(name, null, null);
    }

    public Library(String name, Path path, String url) {
        this.name = name;
        this.path = path;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isNative() {
        return isNative;
    }

    public Library setNative(boolean aNative) {
        isNative = aNative;
        return this;
    }
}
