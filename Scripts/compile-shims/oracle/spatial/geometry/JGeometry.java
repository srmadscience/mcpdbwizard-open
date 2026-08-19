// COMPILE-ONLY SHIM -- not shipped, not for runtime.
// Lets the MCPDBWizard-generated test code (see Scripts/testrun_current.sh)
// compile on a bare JDK without the proprietary jars the legacy build used
// (j2ee.jar for javax.jws, xdb.jar for oracle.xdb, sdoapi.jar for
// oracle.spatial.geometry). Supply the real jars via OB_EXTRA_CP to override.
package oracle.spatial.geometry;
import java.sql.Connection;
import java.sql.SQLException;
import oracle.sql.STRUCT;
// Compile-only shim for the Oracle Spatial class (real impl ships in sdoapi.jar).
public class JGeometry {
    public JGeometry(int gtype, int srid, double x, double y, double z,
                     int[] elemInfo, double[] ordinates) { }
    public int getType() { return 0; }
    public int getSRID() { return 0; }
    public double[] getLabelPointXYZ() { return new double[3]; }
    public int[] getElemInfo() { return null; }
    public double[] getOrdinatesArray() { return null; }
    public static JGeometry load(STRUCT s) throws SQLException { return null; }
    public static STRUCT store(JGeometry geom, Connection conn) throws SQLException { return null; }
    public static STRUCT store(Connection conn, JGeometry geom) throws SQLException { return null; }
}
