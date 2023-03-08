package thedarkdnktv.mclibextractor.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public record MavenArtifact(
    String group,
    String id
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenArtifact that = (MavenArtifact) o;
        return new EqualsBuilder()
            .append(group, that.group)
            .append(id, that.id)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(group)
            .append(id)
            .toHashCode();
    }

    @Override
    public String toString() {
        return this.group + ":" + this.id;
    }
}
