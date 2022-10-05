package androidx.test.rule;

import androidx.test.internal.platform.content.PermissionGranter;
import androidx.test.runner.permission.DDPermissionRequester;

public class DDGrantPermissionRule extends GrantPermissionRule {

    private final PermissionGranter permissionGranter;

    public DDGrantPermissionRule() {
        this(new DDPermissionRequester());
    }

    public DDGrantPermissionRule(PermissionGranter permissionGranter) {
        super(permissionGranter);
        this.permissionGranter = permissionGranter;
    }

    public PermissionGranter getPermissionGranter() {
        return permissionGranter;
    }
}
