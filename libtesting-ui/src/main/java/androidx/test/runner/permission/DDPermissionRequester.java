package androidx.test.runner.permission;

import static junit.framework.Assert.fail;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

@TargetApi(value = 23)
public class DDPermissionRequester extends PermissionRequester {

    @Override
    public void requestPermissions() {
        for (RequestPermissionCallable requestPermissionCallable : requestedPermissions) {
            int attempts = 0;
            while (attempts < 10) {
                try {
                    if (RequestPermissionCallable.Result.FAILURE == requestPermissionCallable.call()) {
                        Log.i("[ddlog]", "request permission failed, attempt #" + attempts);
                        Thread.sleep(1000);
                    } else {
                        return;
                    }
                } catch (Exception exception) {
                    Log.e("[ddlog]", "An Exception was thrown while granting permission", exception);
                    fail("Failed to grant permissions, see logcat for details");
                    return;
                }
                attempts++;
            }
        }
    }
}
