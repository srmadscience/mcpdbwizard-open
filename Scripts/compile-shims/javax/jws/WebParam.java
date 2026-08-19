// COMPILE-ONLY SHIM -- not shipped, not for runtime.
// Lets the MCPDBWizard-generated test code (see Scripts/testrun_current.sh)
// compile on a bare JDK without the proprietary jars the legacy build used
// (j2ee.jar for javax.jws, xdb.jar for oracle.xdb, sdoapi.jar for
// oracle.spatial.geometry). Supply the real jars via OB_EXTRA_CP to override.
package javax.jws;
public @interface WebParam {
    String name() default "";
    String partName() default "";
    String targetNamespace() default "";
}
