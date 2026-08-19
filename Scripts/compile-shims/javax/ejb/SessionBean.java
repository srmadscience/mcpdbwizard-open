package javax.ejb;

/** COMPILE-ONLY shim for the real javax.ejb API - see EJBException shim. */
public interface SessionBean {
    void ejbActivate() throws EJBException, java.rmi.RemoteException;

    void ejbPassivate() throws EJBException, java.rmi.RemoteException;

    void ejbRemove() throws EJBException, java.rmi.RemoteException;

    void setSessionContext(SessionContext ctx) throws EJBException, java.rmi.RemoteException;
}
