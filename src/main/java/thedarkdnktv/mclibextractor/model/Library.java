package thedarkdnktv.mclibextractor.model;

import java.nio.file.Path;

public class Library {

    private String name;
    private Path path;

    public Library(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    public Library() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
