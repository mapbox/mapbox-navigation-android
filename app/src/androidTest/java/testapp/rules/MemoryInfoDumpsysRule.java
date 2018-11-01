package testapp.rules;

public class MemoryInfoDumpsysRule extends AbstractDumpsysRule {

  @Override
  protected String dumpsysService() {
    return "procstats";
  }
}
