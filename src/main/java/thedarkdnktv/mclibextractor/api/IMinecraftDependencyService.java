package thedarkdnktv.mclibextractor.api;

import thedarkdnktv.mclibextractor.model.Dependency;
import thedarkdnktv.mclibextractor.model.LauncherProfile;
import thedarkdnktv.mclibextractor.model.LauncherProfileSettings;
import thedarkdnktv.mclibextractor.model.VersionProfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface IMinecraftDependencyService {

    void setDirectory(Path dir);

    Path getDirectory();

    LauncherProfileSettings loadSettings() throws IOException;

    VersionProfile loadVersionProfile(String id) throws IOException;

    Set<Dependency> loadLibraries(LauncherProfile profile) throws IOException;
}
