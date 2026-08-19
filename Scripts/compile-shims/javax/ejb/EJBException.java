package javax.ejb;

/**
 * COMPILE-ONLY shim (see Scripts/testrun_current.sh): stands in for the real
 * javax.ejb API so generated DAO_EJB=SESSION output (generic_test5/6) compiles
 * without an EJB container jar. Never used at runtime.
 */
public class EJBException extends RuntimeException {
    public EJBException() {
    }

    public EJBException(String message) {
        super(message);
    }

    public EJBException(Exception cause) {
        super(cause);
    }
}
