package thedarkdnktv.mclibextractor.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

public class Dependency implements Comparable<Dependency> {

    private final MavenArtifact artifact;
    private ComparableVersion version;
    private URL downloadUrl;
    private Path path;
    private boolean isNative;

    public Dependency(Map.Entry<MavenArtifact, Pair<ComparableVersion, Path>> entry) {
        this(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue());
    }

    public Dependency(MavenArtifact artifact) {
        this(artifact, null, null);
    }

    public Dependency(MavenArtifact artifact, ComparableVersion version, Path path) {
        this.artifact = artifact;
        this.version = version;
        this.path = path;
    }

    public MavenArtifact getArtifact() {
        return artifact;
    }

    public ComparableVersion getVersion() {
        return version;
    }

    public Path getPath() {
        return path;
    }

    public URL getDownloadUrl() {
        return downloadUrl;
    }

    public Dependency setDownloadUrl(String downloadUrl) {
        if (StringUtils.isNotBlank(downloadUrl)) {
            try {
                this.downloadUrl = new URL(downloadUrl);
            } catch (MalformedURLException e) {
                System.out.println("WARN Dependency artifact URL is malformed: " + downloadUrl);
            }
        }

        return this;
    }

    public void setVersion(ComparableVersion version) {
        this.version = version;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean isNative() {
        return isNative;
    }

    public Dependency setNative(boolean aNative) {
        isNative = aNative;
        return this;
    }

    @Override
    public int compareTo(Dependency o) {
        return this.version.compareTo(o.version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return new EqualsBuilder()
                .append(artifact, that.artifact)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(artifact)
                .toHashCode();
    }
}
