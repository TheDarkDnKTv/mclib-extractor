package thedarkdnktv.mclibextractor.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public record MavenArtifact(
    String group,
    String id,
    String type
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenArtifact that = (MavenArtifact) o;
        return new EqualsBuilder()
            .append(group, that.group)
            .append(id, that.id)
            .append(type, that.type)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(group)
            .append(id)
            .append(type)
            .toHashCode();
    }

    @Override
    public String toString() {
        var result = this.group + ":" + this.id;

        if (this.type != null) {
            result += ":" + this.type;
        }

        return result;
    }
}
