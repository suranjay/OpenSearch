package org.opensearch.instrumentation;

import java.util.Objects;

public class SpanName {

    String name;
    String uniqueId;
    public SpanName(String name, String uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpanName)) return false;
        SpanName spanName = (SpanName) o;
        return uniqueId.equals(spanName.uniqueId) && name.equals(spanName.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, name);
    }

    public String getKey(){
        return String.format("%s_%s", this.name, this.uniqueId);
    }
}
