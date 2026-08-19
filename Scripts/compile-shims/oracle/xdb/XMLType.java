// COMPILE-ONLY SHIM -- not shipped, not for runtime.
// Lets the MCPDBWizard-generated test code (see Scripts/testrun_current.sh)
// compile on a bare JDK without the proprietary jars the legacy build used
// (j2ee.jar for javax.jws, xdb.jar for oracle.xdb, sdoapi.jar for
// oracle.spatial.geometry). Supply the real jars via OB_EXTRA_CP to override.
package oracle.xdb;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import oracle.sql.OPAQUE;
import oracle.sql.CLOB;
// Compile-only shim for the Oracle XML DB class (real impl ships in xdb.jar).
public class XMLType extends OPAQUE {
    public XMLType(Connection conn, String xml) throws SQLException { super(null, conn, null); }
    public XMLType(Connection conn, InputStream xml) throws SQLException { super(null, conn, null); }
    public static XMLType createXML(Object o) throws SQLException { return null; }
    public CLOB getClobVal() throws SQLException { return null; }
    public String getStringVal() throws SQLException { return null; }
    public void close() throws SQLException { }
}
