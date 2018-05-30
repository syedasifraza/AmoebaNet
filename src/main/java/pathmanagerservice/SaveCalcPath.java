package pathmanagerservice;

import java.util.Collection;
import java.util.Map;

/**
 * Created by root on 5/12/17.
 */
public class SaveCalcPath {

    private static Map<String, Map<Collection<String>, Double>> calcPathInfo;

    public static Map<String, Map<Collection<String>, Double>>  getPathInfo() {
        return calcPathInfo;
    }

    public void setPathInfo(Map<String, Map<Collection<String>, Double>>  calcPathInfo) {
        this.calcPathInfo = calcPathInfo;
    }
}
