package thedarkdnktv.mclibextractor.model;

import java.util.HashMap;
import java.util.Map;

public class LauncherProfileSettings {

    private int version;
    private Map<String, LauncherProfile> profiles = new HashMap<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Map<String, LauncherProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<String, LauncherProfile> profiles) {
        this.profiles = profiles;
    }
}
