package thedarkdnktv.mclibextractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.ComparableVersion;
import thedarkdnktv.mclibextractor.exception.LaunchException;
import thedarkdnktv.mclibextractor.gson.LibraryDeserializer;
import thedarkdnktv.mclibextractor.model.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.System.out;
import static java.nio.file.StandardOpenOption.*;

public class Launcher implements Runnable {

    private final static Pattern ARTIFACT_PATTERN;

    private final Gson gson;
    private final Scanner scanner;
    private final Path mcDir;
    private final Path mcLib;
    private final boolean downloadLibraries;

    public Launcher(boolean doDownload) {
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
        this.downloadLibraries = doDownload;
    }

    public static void main(String[] arguments) {
        new Launcher(!ArrayUtils.contains(arguments, "nodownload")).run();
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
                this.processDependencies(libs);
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

    private Set<Dependency> getLibraries(LauncherProfile profile) {
        var dependencies = new HashMap<Dependency, Dependency>();
        var id = profile.getLastVersionId();

        do {
            var versionProfile = this.loadProfile(id);
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
            var mvn = new MavenArtifact(matcher.group(1), matcher.group(2), matcher.group(4));
            var version = new ComparableVersion(matcher.group(3));
            return Pair.of(mvn, version);
        }

        return null;
    }

    private void processDependencies(Set<Dependency> libs) throws IOException {
        Path root = Paths.get(".")
                .toAbsolutePath()
                .normalize();
        final var localDir = root.resolve("libraries");
        final var nativesDir = root.resolve("natives");
        if (Files.notExists(localDir)) {
            Files.createDirectories(localDir);
        }

        if (Files.notExists(nativesDir)) {
            Files.createDirectories(nativesDir);
        }

        for (var lib : libs) {
            var libPath = lib.getPath();

            var localLib = (lib.isNative() ? nativesDir : localDir).resolve(libPath);
            var localLibFolder = localLib.getParent();
            var mcLibrary = this.mcLib.resolve(libPath);
            var download = false;

            if (Files.notExists(mcLibrary)) {
                if (this.downloadLibraries) {
                    download = true;
                } else {
                    out.println("ERR Library not found at MC folder: " + lib.getArtifact() + ", path: " + libPath);
                    continue;
                }
            }

            if (Files.notExists(localLibFolder)) {
                Files.createDirectories(localLibFolder);
            }

            if (download && lib.getDownloadUrl() == null) {
                out.printf("ERR can not download library [%s] as URL is empty\n", lib.getArtifact());
                continue;
            }

            try (var fci = download ? this.downloadDependency(lib) : FileChannel.open(mcLibrary, READ)) {
                try (var fco = FileChannel.open(localLib, WRITE, CREATE, TRUNCATE_EXISTING)) {
                    fco.transferFrom(fci, 0, Long.MAX_VALUE);
                }
            }
        }
    }

    private ReadableByteChannel downloadDependency(Dependency dependency) throws IOException {
        out.println("INFO Downloading library " + dependency.getArtifact());
        var connection = dependency.getDownloadUrl().openConnection();
        connection.connect();
        return Channels.newChannel(connection.getInputStream());
    }

    static {
        ARTIFACT_PATTERN = Pattern.compile("^([[^:].]+):([[^:].]+):([[^:].]+)(?::([[^:].]+))?$");
    }
}
