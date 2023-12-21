package thedarkdnktv.mclibextractor.api.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.ComparableVersion;

import thedarkdnktv.mclibextractor.api.IMinecraftDependencyService;
import thedarkdnktv.mclibextractor.gson.LibraryDeserializer;
import thedarkdnktv.mclibextractor.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.System.out;

public class MinecraftDependencyServiceImpl implements IMinecraftDependencyService {

    private final static Pattern ARTIFACT_PATTERN;
    static {
        ARTIFACT_PATTERN = Pattern.compile("^([[^:].]+):([[^:].]+):([[^:].]+)(?::([[^:].]+))?$");
    }

    private Path mcDir;
    private final Gson gson;

    public MinecraftDependencyServiceImpl() {
        this.gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .registerTypeAdapter(Library.class, new LibraryDeserializer())
                .create();
        var path = Optional.ofNullable(System.getenv("APPDATA"))
                .orElseGet(() -> System.getProperty("user.home"));
        this.setDirectory(Paths.get(path, ".minecraft"));
    }

    @Override
    public void setDirectory(Path dir) {
        this.mcDir = Objects.requireNonNull(dir);
    }

    @Override
    public Path getDirectory() {
        return this.mcDir;
    }

    @Override
    public LauncherProfileSettings loadSettings() throws IOException {
        var profiles = mcDir.resolve("launcher_profiles.json");
        if (Files.notExists(profiles)) {
            throw new IOException("Profiles file not found, run launcher first!");
        }

        try (BufferedReader reader = Files.newBufferedReader(profiles)) {
            return gson.fromJson(reader, LauncherProfileSettings.class);
        }
    }

    @Override
    public VersionProfile loadVersionProfile(String id) throws IOException {
        var file = mcDir.resolve("versions")
                .resolve(id)
                .resolve(id + ".json");

        try (var buffer = Files.newBufferedReader(file)) {
            return gson.fromJson(buffer, VersionProfile.class);
        }
    }

    @Override
    public Set<Dependency> loadLibraries(LauncherProfile profile) throws IOException {
        var dependencies = new HashMap<Dependency, Dependency>();
        var id = profile.getLastVersionId();

        do {
            var versionProfile = this.loadVersionProfile(id);
            for (var lib : versionProfile.getLibraries()) {
                if (lib != null) {
                    var artifact = this.parseArtifact(lib.getName());
                    if (artifact != null) {
                        var current = new Dependency(artifact.getKey(), artifact.getValue(), lib.getPath())
                                .setDownloadUrl(lib.getUrl())
                                .setNative(lib.isNative());

                        dependencies.merge(current, current, (key, previous) -> {
                            out.printf("Found artifact duplicate for [%s], versions are %s and %s\n",
                                    key.getArtifact(),
                                    current.getVersion(),
                                    previous.getVersion());
                            if (current.getVersion().compareTo(previous.getVersion()) > 0) {
                                previous = current;
                            }

                            out.println("\tselecting version " + previous.getVersion());
                            return previous;
                        });


                        dependencies.put(current, current);
                    }
                }
            }

            id = versionProfile.getInheritsFrom();
        } while (id != null && !id.isBlank());

        return new HashSet<>(dependencies.values());
    }

    private Pair<MavenArtifact, ComparableVersion> parseArtifact(String name) {
        var matcher = ARTIFACT_PATTERN.matcher(name);
        if (matcher.lookingAt()) {
            var mvn = new MavenArtifact(matcher.group(1), matcher.group(2), matcher.group(4));
            var version = new ComparableVersion(matcher.group(3));
            return Pair.of(mvn, version);
        }

        return null;
    }
}
