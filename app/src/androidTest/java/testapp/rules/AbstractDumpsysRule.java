package testapp.rules;

import android.os.Trace;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static testapp.utils.TestStorageUtils.buildFileNameFrom;
import static testapp.utils.TestStorageUtils.storeResponse;

/**
 * Abstract class to execute dumpsys commands, It will first reset the dumpsys data for the given service.
 */
abstract class AbstractDumpsysRule extends ExternalResource {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private String packageName;
  private String fileName;

  @Override
  public Statement apply(Statement base, Description description) {
    String testName = description.getMethodName();
    packageName = InstrumentationRegistry.getTargetContext().getPackageName();
    fileName = buildFileNameFrom(testName);
    return super.apply(base, description);
  }

  @Override
  public void before() {
    try {
      UiDevice
        .getInstance(getInstrumentation())
        .executeShellCommand(String.format("dumpsys %s %s --reset", dumpsysService(), packageName));
    } catch (Exception exception) {
      logger.log(Level.SEVERE, "Unable to reset dumpsys", exception);
    }
  }

  @Override
  public void after() {
    try {
      Trace.beginSection("Taking Dumpsys");
      String finalResponse = buildFinalResponse();
      storeResponse(finalResponse, dumpsysService(), fileName);
      logger.log(Level.INFO, "Response is: " + finalResponse);
    } catch (Exception exception) {
      logger.log(Level.SEVERE, "Unable to take a dumpsys", exception);
    } finally {
      Trace.endSection();
    }
    logger.log(Level.INFO, "Dumpsys taken");
  }

  @NonNull
  protected String extraOptions() {
    return "";
  }

  protected abstract String dumpsysService();

  private String buildFinalResponse() throws IOException {
    final String dumpsysCommand = String.format("dumpsys %s %s %s", dumpsysService(), packageName, extraOptions());
    String dumpsysResponse = UiDevice
      .getInstance(getInstrumentation())
      .executeShellCommand(dumpsysCommand);
    final String cpuCoreCommand = "find /sys/devices/system/cpu/ -maxdepth 1";
    String cpuCoreResponse = UiDevice
      .getInstance(getInstrumentation())
      .executeShellCommand(cpuCoreCommand);
    return String.format("%s\n\n%s", cpuCoreResponse, dumpsysResponse);
  }
}


