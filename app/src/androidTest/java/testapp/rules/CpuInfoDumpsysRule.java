package testapp.rules;

public class CpuInfoDumpsysRule extends AbstractDumpsysRule {

  @Override
  protected String dumpsysService() {
    return "cpuinfo";
  }
}
