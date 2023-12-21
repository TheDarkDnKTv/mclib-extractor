package thedarkdnktv.mclibextractor;

import org.apache.commons.lang3.ArrayUtils;
import thedarkdnktv.mclibextractor.api.IMinecraftDependencyService;
import thedarkdnktv.mclibextractor.exception.LaunchException;
import thedarkdnktv.mclibextractor.api.impl.MinecraftDependencyServiceImpl;
import thedarkdnktv.mclibextractor.model.Dependency;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.System.out;
import static java.nio.file.StandardOpenOption.*;

public class Launcher implements Runnable {

    private final IMinecraftDependencyService dependencyService;

    private final Scanner scanner;
    private final Path mcDir;
    private final Path mcLib;
    private final boolean downloadLibraries;

    public Launcher(boolean doDownload) {
        this.dependencyService = new MinecraftDependencyServiceImpl();
        this.scanner = new Scanner(System.in);
        this.mcDir = this.dependencyService.getDirectory();
        this.mcLib = mcDir.resolve("libraries");
        this.downloadLibraries = doDownload;
    }

    public static void main(String[] arguments) {
        var s = ServiceLoader.load(IMinecraftDependencyService.class).findFirst();

        new Launcher(!ArrayUtils.contains(arguments, "nodownload")).run();
    }

    @Override
    public void run() {
        try {
            if (Files.notExists(mcDir)) {
                throw new LaunchException("Minecraft folder does not exist, run launcher first", 1);
            }

            out.println("Loading profiles");
            var profileList = new ArrayList<>(this.dependencyService.loadSettings().getProfiles().values());
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

            var libs = this.dependencyService.loadLibraries(profileList.get(selected));
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
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
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
}
