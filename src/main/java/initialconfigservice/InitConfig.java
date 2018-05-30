package initialconfigservice;

import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;


public class InitConfig {
    private final String name;
    private final Set<String> ifaces;


    public InitConfig(String name, Set<String> ifaces) {
        this.name = checkNotNull(name);
        this.ifaces = checkNotNull(ImmutableSet.copyOf(ifaces));

    }

    public String name() {
        return name;
    }


    public Set<String> ifaces() {
        return ImmutableSet.copyOf(ifaces);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof InitConfig) {
            InitConfig that = (InitConfig) obj;
            return Objects.equals(name, that.name) &&
                    Objects.equals(ifaces, that.ifaces);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ifaces);
    }
}
