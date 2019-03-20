package mb.pie.api.fs.stamp;

import mb.fs.api.node.FSNode;
import mb.fs.api.node.match.FSNodeMatcher;
import mb.fs.api.node.walk.FSNodeWalker;
import mb.pie.api.fs.FileSystemResource;
import mb.pie.api.stamp.ResourceStamp;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.security.MessageDigest;

public class RecHashResourceStamper implements HashResourceStamperTrait {
    private final @Nullable FSNodeWalker walker;
    private final @Nullable FSNodeMatcher matcher;


    public RecHashResourceStamper(FSNodeWalker walker, FSNodeMatcher matcher) {
        this.walker = walker;
        this.matcher = matcher;
    }

    public RecHashResourceStamper() {
        this.walker = null;
        this.matcher = null;
    }


    @Override public ResourceStamp<FileSystemResource> stamp(FileSystemResource resource) throws IOException {
        final FSNode node = resource.node;
        if(!node.exists()) {
            return new NullResourceStamp(this);
        }
        final MessageDigest digest = createDigester();
        updateRec(digest, node, walker, matcher);
        final byte[] bytes = digest.digest();
        return new ByteArrayResourceStamp(bytes, this);
    }

    @Override public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        final RecHashResourceStamper that = (RecHashResourceStamper) o;
        if(walker != null ? !walker.equals(that.walker) : that.walker != null) return false;
        return matcher != null ? matcher.equals(that.matcher) : that.matcher == null;
    }

    @Override public int hashCode() {
        int result = walker != null ? walker.hashCode() : 0;
        result = 31 * result + (matcher != null ? matcher.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "RecHashStamper(" + walker + ")";
    }
}