package thedarkdnktv.mclibextractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.ComparableVersion;
import thedarkdnktv.mclibextractor.exception.LaunchException;
import thedarkdnktv.mclibextractor.gson.LibraryDeserializer;
import thedarkdnktv.mclibextractor.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.System.out;
import static java.nio.file.StandardOpenOption.*;

public class Launcher implements Runnable {

    private final static Pattern ARTIFACT_PATTERN;

    private final Gson gson;
    private final Scanner scanner;
    private final Path mcDir;
    private final Path mcLib;

    public Launcher() {
        this.scanner = new Scanner(System.in);
        this.gson = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .registerTypeAdapter(Library.class, new LibraryDeserializer())
            .create();
        var path = Optional.ofNullable(System.getenv("APPDATA"))
            .orElseGet(() -> System.getProperty("user.home"));
        this.mcDir = Paths.get(path, ".minecraft");
        this.mcLib = mcDir.resolve("libraries");
    }

    public static void main(String[] arguments) {
        new Launcher().run();
    }

    @Override
    public void run() {
        try {
            if (Files.notExists(mcDir)) {
                throw new LaunchException("Minecraft folder does not exist, run launcher first", 1);
            }

            out.println("Loading profiles");
            var profiles = this.loadSettings();
            var profileList = new ArrayList<>(profiles.getProfiles().values());
            out.println("Loaded " + profileList.size() + " launcher profiles:");
            for (int i = 0; i < profileList.size(); i ++) {
                out.printf("\t[%d]: %s (%s)\n", i, profileList.get(i).getName(), profileList.get(i).getType());
            }

            int selected;
            while (true) {
                try {
                    out.print("Please select profile for library extraction: ");
                    selected = scanner.nextInt();
                    if (selected < 0 || selected > profileList.size()) {
                        throw new NoSuchElementException("Out of range");
                    }

                    break;
                } catch (NoSuchElementException e) {
                    if (e instanceof InputMismatchException) {
                        scanner.next();
                    }

                    out.print("Please enter number");
                    if (e.getMessage() != null) {
                        out.print(": " + e.getMessage());
                    }

                    out.println();
                }
            }

            var libs = this.getLibraries(profileList.get(selected));
            try {
                this.copyLibraries(libs);
            } catch (IOException e) {
                throw new LaunchException("Unable to copy libraries", e);
            }

            out.printf("Successfully copied %d libraries", libs.size());
        } catch (LaunchException e) {
            if (e.getCause() == null) {
                System.err.println(e.getMessage());
            } else {
                e.printStackTrace(System.err);
            }

            System.exit(e.getExitCode());
        }
    }

    private LauncherProfileSettings loadSettings() throws LaunchException {
        var profiles = mcDir.resolve("launcher_profiles.json");
        if (Files.notExists(profiles)) {
            throw new LaunchException("Profiles file not found, run launcher first!", 2);
        }

        try (BufferedReader reader = Files.newBufferedReader(profiles)) {
            return gson.fromJson(reader, LauncherProfileSettings.class);
        } catch (IOException e) {
            throw new LaunchException(e);
        }
    }

    private Set<Path> getLibraries(LauncherProfile profile) {
        var artifacts = new HashMap<MavenArtifact, Pair<ComparableVersion, Path>>();
        var id = profile.getLastVersionId();

        do {
            var versionProfile = this.loadProfile(id);
            for (var lib : versionProfile.getLibraries()) {
                if (lib != null) {
                    var artifact = this.parseArtifact(lib.getName());
                    if (artifact != null) {
                        var current = Pair.of(artifact.getValue(), lib.getPath());
                        var previous = artifacts.get(artifact.getKey());

                        if (previous != null) {
                            out.printf("Found artifact duplicate for [%s], versions are %s and %s\n", artifact.getKey(), current.getKey(), previous.getKey());
                            if (previous.getKey().compareTo(current.getKey()) > 0) {
                                current = previous;
                            }

                            out.println("\tselecting version " + current.getKey());
                        }

                        artifacts.put(artifact.getKey(), current);
                    }
                }
            }

            id = versionProfile.getInheritsFrom();
        } while (id != null && !id.isBlank());

        return artifacts.values().stream()
            .map(Pair::getValue)
            .filter(Objects::nonNull)
            .map(mcLib::resolve)
            .collect(Collectors.toSet());
    }

    private VersionProfile loadProfile(String id) {
        var file = mcDir.resolve("versions")
            .resolve(id)
            .resolve(id + ".json");

        try (var buffer = Files.newBufferedReader(file)) {
            return gson.fromJson(buffer, VersionProfile.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Pair<MavenArtifact, ComparableVersion> parseArtifact(String name) {
        var matcher = ARTIFACT_PATTERN.matcher(name);
        if (matcher.lookingAt()) {
            var mvn = new MavenArtifact(matcher.group(1), matcher.group(2));
            var version = new ComparableVersion(matcher.group(3));
            return Pair.of(mvn, version);
        }

        return null;
    }

    private void copyLibraries(Set<Path> libs) throws IOException {
        final var localDir = Paths.get(".")
            .toAbsolutePath()
            .normalize()
            .resolve("libraries");
        if (Files.notExists(localDir)) {
            Files.createDirectories(localDir);
        }

        for (Path lib : libs) {
            if (Files.notExists(lib)) {
                continue;
            }

            var localLib = localDir.resolve(mcLib.relativize(lib.getParent()));
            if (Files.notExists(localLib)) {
                Files.createDirectories(localLib);
            }

            try (var fci = FileChannel.open(lib, READ)) {
                var localLibPath = localLib.resolve(lib.getFileName());
                try (var fco = FileChannel.open(localLibPath, WRITE, CREATE, TRUNCATE_EXISTING)) {
                    fco.transferFrom(fci, 0, Long.MAX_VALUE);
                }
            }
        }
    }

    static {
        ARTIFACT_PATTERN = Pattern.compile("(.+):(.+):(.+)");
    }
}
