package com.mcpdbwizard.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JSON-friendly, in-memory model of an MCPDBWizard generation request &mdash;
 * the same information a {@code .pb2} file holds, but as typed Java objects instead of a flat
 * {@link Properties} key/value bag.
 *
 * <p>The high-level, single-valued (scalar) settings are held as {@link String} fields (a
 * {@code .pb2} stores every value as text, so String preserves each one exactly and serialises
 * cleanly to JSON). The repeating selections &mdash; sequences, tables, procedures and user SQL
 * statements &mdash; are held as typed lists ({@link Sequence}, {@link Table}, {@link Procedure},
 * {@link SqlStatement}). Any key not recognised by this model is preserved verbatim in
 * {@link #getExtraProperties() extraProperties} so a round-trip never loses information.
 *
 * <p><b>Round-trip contract:</b> for any {@code .pb2}, {@code new Schema(props).toPb2()} yields a
 * {@link Properties} equal to the original &mdash; the same set of key/value pairs (a {@code .pb2}
 * is order-independent). A null scalar field means the key was absent and stays absent; an empty
 * string means the key was present with an empty value and is re-emitted as such.
 *
 * <p>This class is generated-friendly boilerplate; see {@code gen_schema.py} in the job scratch
 * area for the source of the scalar-field list if it ever needs regenerating.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class Schema {

    private String productVersion;
    private String productBuild;
    private String uiname;
    private String author;
    private String version;
    private String comment;
    private String hostname;
    private String port;
    private String oracleSid;
    private String user;
    private String pass;
    private String oracleVersion;
    private String otherUserName;
    private String xSize;
    private String ySize;
    private String debugMessages;
    private String otherMessages;
    private String codeComments;
    private String userObjects;
    private String otherObjects;
    private String methodsByte;
    private String methodsByteObj;
    private String methodsShort;
    private String methodsShortObj;
    private String methodsInt;
    private String methodsIntObj;
    private String methodsLong;
    private String methodsLongObj;
    private String methodsFloat;
    private String methodsFloatObj;
    private String methodsDouble;
    private String methodsDoubleObj;
    private String validate;
    private String extraSql;
    private String packageName;
    private String codeBaseDirectory;
    private String codeBaseDirectoryFrozen;
    private String sqlFileDirectory;
    private String codeStatistics;
    private String javaAccessType;
    private String javaNamingConvention;
    private String targetJvm;
    private String methodSqlPrefix;
    private String methodPlsqlPrefix;
    private String extraClassCode;
    private String webServices;
    private String mcpServer;
    private String mcpHttpToken;
    private String mcpHttps;
    private String mcpOAuth;
    private String prometheusServer;

    /**
     * WEB APPLICATION ONLY, and the only key in this class that the GENERATOR never reads.
     *
     * <p>{@code RUN_ON_START=YES} asks the web container to bring this config's MCP server back up
     * when it starts, so a restart does not leave every server stopped until somebody presses start.
     * It is a DEPLOYMENT hint stored in the config, not a generation-time flag -- emitted output is
     * byte-identical with it on or off, which is asserted by a test rather than left as an intention.
     */
    private String runOnStart;
    private String metricsPort;
    private String webServicesAbstractBfile;
    private String wsPreCallStub;
    private String wsPostCallStub;
    private String wsAlwaysRelease;
    private String wsRecordType;
    private String wsNumberType;
    private String wsInterfaceName;
    private String wsImplName;
    private String daoFinalize;
    private String daoLogType;
    private String daoLogName;
    private String daoFactoryName;
    private String daoEjb;
    private String daoConnectionType;
    private String daoConnectionName;
    private String commitConnections;
    private String closeConnections;
    private String daoPool;
    private String daoPoolMaxSize;
    private String daoPoolMinIdle;
    private String daoPoolMaxWaitMs;
    private String daoPoolIdleTimeoutMs;
    private String daoPoolOnReturn;
    private String ec30ProjectPath;
    private String ec30RelPath;
    private String defaultTempdir;
    private String defaultTempPrefix;
    private String defaultTempSuffix;
    private String postScriptName;
    private String postScriptContent;
    private String xwsType;
    private String xwsIface;
    private String xmsImpl;

    private List<Sequence> sequences = new ArrayList<>();
    private List<Table> tables = new ArrayList<>();
    private List<Procedure> procedures = new ArrayList<>();
    private List<SqlStatement> sqlStatements = new ArrayList<>();

    /** Any PB2 key this model does not recognise, preserved verbatim for a lossless round-trip. */
    private Map<String, String> extraProperties = new LinkedHashMap<>();

    /** Null constructor &mdash; an empty schema. */
    public Schema() {
    }

    /** Full constructor &mdash; every field value. */
    public Schema(
            String productVersion,
            String productBuild,
            String uiname,
            String author,
            String version,
            String comment,
            String hostname,
            String port,
            String oracleSid,
            String user,
            String pass,
            String oracleVersion,
            String otherUserName,
            String xSize,
            String ySize,
            String debugMessages,
            String otherMessages,
            String codeComments,
            String userObjects,
            String otherObjects,
            String methodsByte,
            String methodsByteObj,
            String methodsShort,
            String methodsShortObj,
            String methodsInt,
            String methodsIntObj,
            String methodsLong,
            String methodsLongObj,
            String methodsFloat,
            String methodsFloatObj,
            String methodsDouble,
            String methodsDoubleObj,
            String validate,
            String extraSql,
            String packageName,
            String codeBaseDirectory,
            String codeBaseDirectoryFrozen,
            String sqlFileDirectory,
            String codeStatistics,
            String javaAccessType,
            String javaNamingConvention,
            String targetJvm,
            String methodSqlPrefix,
            String methodPlsqlPrefix,
            String extraClassCode,
            String webServices,
            String mcpServer,
            String mcpHttpToken,
            String mcpHttps,
            String mcpOAuth,
            String prometheusServer,
            String runOnStart,
            String metricsPort,
            String webServicesAbstractBfile,
            String wsPreCallStub,
            String wsPostCallStub,
            String wsAlwaysRelease,
            String wsRecordType,
            String wsNumberType,
            String wsInterfaceName,
            String wsImplName,
            String daoFinalize,
            String daoLogType,
            String daoLogName,
            String daoFactoryName,
            String daoEjb,
            String daoConnectionType,
            String daoConnectionName,
            String commitConnections,
            String closeConnections,
            String daoPool,
            String daoPoolMaxSize,
            String daoPoolMinIdle,
            String daoPoolMaxWaitMs,
            String daoPoolIdleTimeoutMs,
            String daoPoolOnReturn,
            String ec30ProjectPath,
            String ec30RelPath,
            String defaultTempdir,
            String defaultTempPrefix,
            String defaultTempSuffix,
            String postScriptName,
            String postScriptContent,
            String xwsType,
            String xwsIface,
            String xmsImpl,
            List<Sequence> sequences,
            List<Table> tables,
            List<Procedure> procedures,
            List<SqlStatement> sqlStatements,
            Map<String, String> extraProperties) {
        this.productVersion = productVersion;
        this.productBuild = productBuild;
        this.uiname = uiname;
        this.author = author;
        this.version = version;
        this.comment = comment;
        this.hostname = hostname;
        this.port = port;
        this.oracleSid = oracleSid;
        this.user = user;
        this.pass = pass;
        this.oracleVersion = oracleVersion;
        this.otherUserName = otherUserName;
        this.xSize = xSize;
        this.ySize = ySize;
        this.debugMessages = debugMessages;
        this.otherMessages = otherMessages;
        this.codeComments = codeComments;
        this.userObjects = userObjects;
        this.otherObjects = otherObjects;
        this.methodsByte = methodsByte;
        this.methodsByteObj = methodsByteObj;
        this.methodsShort = methodsShort;
        this.methodsShortObj = methodsShortObj;
        this.methodsInt = methodsInt;
        this.methodsIntObj = methodsIntObj;
        this.methodsLong = methodsLong;
        this.methodsLongObj = methodsLongObj;
        this.methodsFloat = methodsFloat;
        this.methodsFloatObj = methodsFloatObj;
        this.methodsDouble = methodsDouble;
        this.methodsDoubleObj = methodsDoubleObj;
        this.validate = validate;
        this.extraSql = extraSql;
        this.packageName = packageName;
        this.codeBaseDirectory = codeBaseDirectory;
        this.codeBaseDirectoryFrozen = codeBaseDirectoryFrozen;
        this.sqlFileDirectory = sqlFileDirectory;
        this.codeStatistics = codeStatistics;
        this.javaAccessType = javaAccessType;
        this.javaNamingConvention = javaNamingConvention;
        this.targetJvm = targetJvm;
        this.methodSqlPrefix = methodSqlPrefix;
        this.methodPlsqlPrefix = methodPlsqlPrefix;
        this.extraClassCode = extraClassCode;
        this.webServices = webServices;
        this.mcpServer = mcpServer;
        this.mcpHttpToken = mcpHttpToken;
        this.mcpHttps = mcpHttps;
        this.mcpOAuth = mcpOAuth;
        this.prometheusServer = prometheusServer;
        this.runOnStart = runOnStart;
        this.metricsPort = metricsPort;
        this.webServicesAbstractBfile = webServicesAbstractBfile;
        this.wsPreCallStub = wsPreCallStub;
        this.wsPostCallStub = wsPostCallStub;
        this.wsAlwaysRelease = wsAlwaysRelease;
        this.wsRecordType = wsRecordType;
        this.wsNumberType = wsNumberType;
        this.wsInterfaceName = wsInterfaceName;
        this.wsImplName = wsImplName;
        this.daoFinalize = daoFinalize;
        this.daoLogType = daoLogType;
        this.daoLogName = daoLogName;
        this.daoFactoryName = daoFactoryName;
        this.daoEjb = daoEjb;
        this.daoConnectionType = daoConnectionType;
        this.daoConnectionName = daoConnectionName;
        this.commitConnections = commitConnections;
        this.closeConnections = closeConnections;
        this.daoPool = daoPool;
        this.daoPoolMaxSize = daoPoolMaxSize;
        this.daoPoolMinIdle = daoPoolMinIdle;
        this.daoPoolMaxWaitMs = daoPoolMaxWaitMs;
        this.daoPoolIdleTimeoutMs = daoPoolIdleTimeoutMs;
        this.daoPoolOnReturn = daoPoolOnReturn;
        this.ec30ProjectPath = ec30ProjectPath;
        this.ec30RelPath = ec30RelPath;
        this.defaultTempdir = defaultTempdir;
        this.defaultTempPrefix = defaultTempPrefix;
        this.defaultTempSuffix = defaultTempSuffix;
        this.postScriptName = postScriptName;
        this.postScriptContent = postScriptContent;
        this.xwsType = xwsType;
        this.xwsIface = xwsIface;
        this.xmsImpl = xmsImpl;
        this.sequences = (sequences != null) ? sequences : new ArrayList<>();
        this.tables = (tables != null) ? tables : new ArrayList<>();
        this.procedures = (procedures != null) ? procedures : new ArrayList<>();
        this.sqlStatements = (sqlStatements != null) ? sqlStatements : new ArrayList<>();
        this.extraProperties = (extraProperties != null) ? extraProperties : new LinkedHashMap<>();
    }

    /** PB2 file constructor &mdash; load a {@code .pb2} file and parse it. */
    public Schema(File pb2File) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(pb2File)) {
            props.load(in);
        }
        fromPb2(props);
    }

    /** PB2 (Properties) constructor &mdash; parse an already-loaded {@code .pb2} property set. */
    public Schema(Properties props) {
        fromPb2(props);
    }

    /** JSON constructor &mdash; parse a JSON document produced by {@link #toJson()}. */
    public Schema(String json) {
        fromJson(json);
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public String getProductBuild() {
        return productBuild;
    }

    public void setProductBuild(String productBuild) {
        this.productBuild = productBuild;
    }

    public String getUiname() {
        return uiname;
    }

    public void setUiname(String uiname) {
        this.uiname = uiname;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getOracleSid() {
        return oracleSid;
    }

    public void setOracleSid(String oracleSid) {
        this.oracleSid = oracleSid;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getOracleVersion() {
        return oracleVersion;
    }

    public void setOracleVersion(String oracleVersion) {
        this.oracleVersion = oracleVersion;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getXSize() {
        return xSize;
    }

    public void setXSize(String xSize) {
        this.xSize = xSize;
    }

    public String getYSize() {
        return ySize;
    }

    public void setYSize(String ySize) {
        this.ySize = ySize;
    }

    public String getDebugMessages() {
        return debugMessages;
    }

    public void setDebugMessages(String debugMessages) {
        this.debugMessages = debugMessages;
    }

    public String getOtherMessages() {
        return otherMessages;
    }

    public void setOtherMessages(String otherMessages) {
        this.otherMessages = otherMessages;
    }

    public String getCodeComments() {
        return codeComments;
    }

    public void setCodeComments(String codeComments) {
        this.codeComments = codeComments;
    }

    public String getUserObjects() {
        return userObjects;
    }

    public void setUserObjects(String userObjects) {
        this.userObjects = userObjects;
    }

    public String getOtherObjects() {
        return otherObjects;
    }

    public void setOtherObjects(String otherObjects) {
        this.otherObjects = otherObjects;
    }

    public String getMethodsByte() {
        return methodsByte;
    }

    public void setMethodsByte(String methodsByte) {
        this.methodsByte = methodsByte;
    }

    public String getMethodsByteObj() {
        return methodsByteObj;
    }

    public void setMethodsByteObj(String methodsByteObj) {
        this.methodsByteObj = methodsByteObj;
    }

    public String getMethodsShort() {
        return methodsShort;
    }

    public void setMethodsShort(String methodsShort) {
        this.methodsShort = methodsShort;
    }

    public String getMethodsShortObj() {
        return methodsShortObj;
    }

    public void setMethodsShortObj(String methodsShortObj) {
        this.methodsShortObj = methodsShortObj;
    }

    public String getMethodsInt() {
        return methodsInt;
    }

    public void setMethodsInt(String methodsInt) {
        this.methodsInt = methodsInt;
    }

    public String getMethodsIntObj() {
        return methodsIntObj;
    }

    public void setMethodsIntObj(String methodsIntObj) {
        this.methodsIntObj = methodsIntObj;
    }

    public String getMethodsLong() {
        return methodsLong;
    }

    public void setMethodsLong(String methodsLong) {
        this.methodsLong = methodsLong;
    }

    public String getMethodsLongObj() {
        return methodsLongObj;
    }

    public void setMethodsLongObj(String methodsLongObj) {
        this.methodsLongObj = methodsLongObj;
    }

    public String getMethodsFloat() {
        return methodsFloat;
    }

    public void setMethodsFloat(String methodsFloat) {
        this.methodsFloat = methodsFloat;
    }

    public String getMethodsFloatObj() {
        return methodsFloatObj;
    }

    public void setMethodsFloatObj(String methodsFloatObj) {
        this.methodsFloatObj = methodsFloatObj;
    }

    public String getMethodsDouble() {
        return methodsDouble;
    }

    public void setMethodsDouble(String methodsDouble) {
        this.methodsDouble = methodsDouble;
    }

    public String getMethodsDoubleObj() {
        return methodsDoubleObj;
    }

    public void setMethodsDoubleObj(String methodsDoubleObj) {
        this.methodsDoubleObj = methodsDoubleObj;
    }

    public String getValidate() {
        return validate;
    }

    public void setValidate(String validate) {
        this.validate = validate;
    }

    public String getExtraSql() {
        return extraSql;
    }

    public void setExtraSql(String extraSql) {
        this.extraSql = extraSql;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getCodeBaseDirectory() {
        return codeBaseDirectory;
    }

    public void setCodeBaseDirectory(String codeBaseDirectory) {
        this.codeBaseDirectory = codeBaseDirectory;
    }

    public String getCodeBaseDirectoryFrozen() {
        return codeBaseDirectoryFrozen;
    }

    public void setCodeBaseDirectoryFrozen(String codeBaseDirectoryFrozen) {
        this.codeBaseDirectoryFrozen = codeBaseDirectoryFrozen;
    }

    public String getSqlFileDirectory() {
        return sqlFileDirectory;
    }

    public void setSqlFileDirectory(String sqlFileDirectory) {
        this.sqlFileDirectory = sqlFileDirectory;
    }

    public String getCodeStatistics() {
        return codeStatistics;
    }

    public void setCodeStatistics(String codeStatistics) {
        this.codeStatistics = codeStatistics;
    }

    public String getJavaAccessType() {
        return javaAccessType;
    }

    public void setJavaAccessType(String javaAccessType) {
        this.javaAccessType = javaAccessType;
    }

    public String getJavaNamingConvention() {
        return javaNamingConvention;
    }

    public void setJavaNamingConvention(String javaNamingConvention) {
        this.javaNamingConvention = javaNamingConvention;
    }

    public String getTargetJvm() {
        return targetJvm;
    }

    public void setTargetJvm(String targetJvm) {
        this.targetJvm = targetJvm;
    }

    public String getMethodSqlPrefix() {
        return methodSqlPrefix;
    }

    public void setMethodSqlPrefix(String methodSqlPrefix) {
        this.methodSqlPrefix = methodSqlPrefix;
    }

    public String getMethodPlsqlPrefix() {
        return methodPlsqlPrefix;
    }

    public void setMethodPlsqlPrefix(String methodPlsqlPrefix) {
        this.methodPlsqlPrefix = methodPlsqlPrefix;
    }

    public String getExtraClassCode() {
        return extraClassCode;
    }

    public void setExtraClassCode(String extraClassCode) {
        this.extraClassCode = extraClassCode;
    }

    public String getWebServices() {
        return webServices;
    }

    public void setWebServices(String webServices) {
        this.webServices = webServices;
    }

    public String getMcpServer() {
        return mcpServer;
    }

    public void setMcpServer(String mcpServer) {
        this.mcpServer = mcpServer;
    }

    public String getMcpHttpToken() {
        return mcpHttpToken;
    }

    public void setMcpHttpToken(String mcpHttpToken) {
        this.mcpHttpToken = mcpHttpToken;
    }

    public String getMcpHttps() {
        return mcpHttps;
    }

    public String getMcpOAuth() {
        return mcpOAuth;
    }

    public void setMcpHttps(String mcpHttps) {
        this.mcpHttps = mcpHttps;
    }

    public void setMcpOAuth(String mcpOAuth) {
        this.mcpOAuth = mcpOAuth;
    }

    public String getPrometheusServer() {
        return prometheusServer;
    }

    public void setPrometheusServer(String prometheusServer) {
        this.prometheusServer = prometheusServer;
    }

    public String getRunOnStart() {
        return runOnStart;
    }

    public void setRunOnStart(String runOnStart) {
        this.runOnStart = runOnStart;
    }

    /**
     * The Prometheus scrape port this config's server should bind, or null/empty to let the
     * deployment choose one.
     *
     * <p><b>This is the one config scalar that changes no generated code.</b> Every other setting
     * here is read by the generator; this one is read by whatever LAUNCHES the generated server —
     * {@code mcpdbwizard-web}'s {@code RuntimeManager} — which passes it to the child as the
     * {@code MCP_METRICS_PORT} environment variable. The emitted source is byte-identical whether
     * this is set or not, so it needs no estate re-verification and cannot break a tree.
     *
     * <p><b>The key is deliberately NOT called {@code MCP_METRICS_PORT}</b>, though that is the
     * variable it ends up in. {@code MCP_HTTP_TOKEN} being both a config flag and an environment
     * variable is recorded in {@code CLAUDE.md} as the case that catches people out, and the same
     * name for both ends would repeat it. A distinct name makes the direction obvious: the config
     * records an intention, the environment carries it.
     *
     * <p>Empty means "not specified" and is the default, which preserves the existing behaviour
     * exactly — the runtime allocates from {@code mcpdbwizard.runtime.metrics-port-range}, and when
     * that is unset (also the default) no metrics listener is started at all. Setting it here
     * bypasses that allocator for this config, which is why it is worth being explicit that two
     * configs given the same number will collide: the first to start binds it and the second logs
     * and serves tools without metrics rather than failing.
     *
     * <p>Only meaningful with {@code PROMETHEUS_SERVER=YES}; a port without it does nothing,
     * because a server generated without the metrics code has nothing to serve.
     */
    public String getMetricsPort() {
        return metricsPort;
    }

    public void setMetricsPort(String metricsPort) {
        this.metricsPort = metricsPort;
    }

    public String getWebServicesAbstractBfile() {
        return webServicesAbstractBfile;
    }

    public void setWebServicesAbstractBfile(String webServicesAbstractBfile) {
        this.webServicesAbstractBfile = webServicesAbstractBfile;
    }

    public String getWsPreCallStub() {
        return wsPreCallStub;
    }

    public void setWsPreCallStub(String wsPreCallStub) {
        this.wsPreCallStub = wsPreCallStub;
    }

    public String getWsPostCallStub() {
        return wsPostCallStub;
    }

    public void setWsPostCallStub(String wsPostCallStub) {
        this.wsPostCallStub = wsPostCallStub;
    }

    public String getWsAlwaysRelease() {
        return wsAlwaysRelease;
    }

    public void setWsAlwaysRelease(String wsAlwaysRelease) {
        this.wsAlwaysRelease = wsAlwaysRelease;
    }

    public String getWsRecordType() {
        return wsRecordType;
    }

    public void setWsRecordType(String wsRecordType) {
        this.wsRecordType = wsRecordType;
    }

    public String getWsNumberType() {
        return wsNumberType;
    }

    public void setWsNumberType(String wsNumberType) {
        this.wsNumberType = wsNumberType;
    }

    public String getWsInterfaceName() {
        return wsInterfaceName;
    }

    public void setWsInterfaceName(String wsInterfaceName) {
        this.wsInterfaceName = wsInterfaceName;
    }

    public String getWsImplName() {
        return wsImplName;
    }

    public void setWsImplName(String wsImplName) {
        this.wsImplName = wsImplName;
    }

    public String getDaoFinalize() {
        return daoFinalize;
    }

    public void setDaoFinalize(String daoFinalize) {
        this.daoFinalize = daoFinalize;
    }

    public String getDaoLogType() {
        return daoLogType;
    }

    public void setDaoLogType(String daoLogType) {
        this.daoLogType = daoLogType;
    }

    public String getDaoLogName() {
        return daoLogName;
    }

    public void setDaoLogName(String daoLogName) {
        this.daoLogName = daoLogName;
    }

    public String getDaoFactoryName() {
        return daoFactoryName;
    }

    public void setDaoFactoryName(String daoFactoryName) {
        this.daoFactoryName = daoFactoryName;
    }

    public String getDaoEjb() {
        return daoEjb;
    }

    public void setDaoEjb(String daoEjb) {
        this.daoEjb = daoEjb;
    }

    public String getDaoConnectionType() {
        return daoConnectionType;
    }

    public void setDaoConnectionType(String daoConnectionType) {
        this.daoConnectionType = daoConnectionType;
    }

    public String getDaoConnectionName() {
        return daoConnectionName;
    }

    public void setDaoConnectionName(String daoConnectionName) {
        this.daoConnectionName = daoConnectionName;
    }

    public String getCommitConnections() {
        return commitConnections;
    }

    public void setCommitConnections(String commitConnections) {
        this.commitConnections = commitConnections;
    }

    public String getCloseConnections() {
        return closeConnections;
    }

    public void setCloseConnections(String closeConnections) {
        this.closeConnections = closeConnections;
    }

    public String getDaoPool() {
        return daoPool;
    }

    public void setDaoPool(String daoPool) {
        this.daoPool = daoPool;
    }

    public String getDaoPoolMaxSize() {
        return daoPoolMaxSize;
    }

    public void setDaoPoolMaxSize(String daoPoolMaxSize) {
        this.daoPoolMaxSize = daoPoolMaxSize;
    }

    public String getDaoPoolMinIdle() {
        return daoPoolMinIdle;
    }

    public void setDaoPoolMinIdle(String daoPoolMinIdle) {
        this.daoPoolMinIdle = daoPoolMinIdle;
    }

    public String getDaoPoolMaxWaitMs() {
        return daoPoolMaxWaitMs;
    }

    public void setDaoPoolMaxWaitMs(String daoPoolMaxWaitMs) {
        this.daoPoolMaxWaitMs = daoPoolMaxWaitMs;
    }

    public String getDaoPoolIdleTimeoutMs() {
        return daoPoolIdleTimeoutMs;
    }

    public void setDaoPoolIdleTimeoutMs(String daoPoolIdleTimeoutMs) {
        this.daoPoolIdleTimeoutMs = daoPoolIdleTimeoutMs;
    }

    public String getDaoPoolOnReturn() {
        return daoPoolOnReturn;
    }

    public void setDaoPoolOnReturn(String daoPoolOnReturn) {
        this.daoPoolOnReturn = daoPoolOnReturn;
    }

    public String getEc30ProjectPath() {
        return ec30ProjectPath;
    }

    public void setEc30ProjectPath(String ec30ProjectPath) {
        this.ec30ProjectPath = ec30ProjectPath;
    }

    public String getEc30RelPath() {
        return ec30RelPath;
    }

    public void setEc30RelPath(String ec30RelPath) {
        this.ec30RelPath = ec30RelPath;
    }

    public String getDefaultTempdir() {
        return defaultTempdir;
    }

    public void setDefaultTempdir(String defaultTempdir) {
        this.defaultTempdir = defaultTempdir;
    }

    public String getDefaultTempPrefix() {
        return defaultTempPrefix;
    }

    public void setDefaultTempPrefix(String defaultTempPrefix) {
        this.defaultTempPrefix = defaultTempPrefix;
    }

    public String getDefaultTempSuffix() {
        return defaultTempSuffix;
    }

    public void setDefaultTempSuffix(String defaultTempSuffix) {
        this.defaultTempSuffix = defaultTempSuffix;
    }

    public String getPostScriptName() {
        return postScriptName;
    }

    public void setPostScriptName(String postScriptName) {
        this.postScriptName = postScriptName;
    }

    public String getPostScriptContent() {
        return postScriptContent;
    }

    public void setPostScriptContent(String postScriptContent) {
        this.postScriptContent = postScriptContent;
    }

    public String getXwsType() {
        return xwsType;
    }

    public void setXwsType(String xwsType) {
        this.xwsType = xwsType;
    }

    public String getXwsIface() {
        return xwsIface;
    }

    public void setXwsIface(String xwsIface) {
        this.xwsIface = xwsIface;
    }

    public String getXmsImpl() {
        return xmsImpl;
    }

    public void setXmsImpl(String xmsImpl) {
        this.xmsImpl = xmsImpl;
    }

    public List<Sequence> getSequences() {
        return sequences;
    }

    public void setSequences(List<Sequence> sequences) {
        this.sequences = (sequences != null) ? sequences : new ArrayList<>();
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = (tables != null) ? tables : new ArrayList<>();
    }

    public List<Procedure> getProcedures() {
        return procedures;
    }

    public void setProcedures(List<Procedure> procedures) {
        this.procedures = (procedures != null) ? procedures : new ArrayList<>();
    }

    public List<SqlStatement> getSqlStatements() {
        return sqlStatements;
    }

    public void setSqlStatements(List<SqlStatement> sqlStatements) {
        this.sqlStatements = (sqlStatements != null) ? sqlStatements : new ArrayList<>();
    }

    /**
     * TRUE when this config selects no database object of any kind.
     *
     * <p>Such a config is legal and generates successfully -- it emits a DAO factory and, under
     * {@code WEB_SERVICES=YES}, the SOAP scaffolding -- but there is nothing behind any of it, and
     * an {@code MCP_SERVER=YES} config emits no server at all, because a server with no tools is
     * not worth starting. That combination reads as a broken build rather than an empty config,
     * which is why both the save path and the runtime path say so explicitly rather than leaving
     * it to be inferred from a file count.
     *
     * <p>The four lists are the whole of what a config selects; every other field is a setting
     * about HOW to generate, not WHAT. The lists are never null (their setters substitute an empty
     * list), so this needs no null guards.
     *
     * @return true when no table, procedure, sequence or SQL statement is selected
     */
    public boolean selectsNothing() {
        return tables.isEmpty() && procedures.isEmpty()
                && sequences.isEmpty() && sqlStatements.isEmpty();
    }

    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, String> extraProperties) {
        this.extraProperties = (extraProperties != null) ? extraProperties : new LinkedHashMap<>();
    }

    /** Every scalar PB2 key this model owns (so it is never mistaken for an extra property). */
    private static final java.util.Set<String> SCALAR_KEYS = new java.util.HashSet<>(java.util.Arrays.asList(
            "PRODUCT_VERSION",
            "PRODUCT_BUILD",
            "UINAME",
            "AUTHOR",
            "VERSION",
            "COMMENT",
            "HOSTNAME",
            "PORT",
            "ORACLE_SID",
            "USER",
            "PASS",
            "ORACLE_VERSION",
            "OTHER_USER_NAME",
            "X_SIZE",
            "Y_SIZE",
            "DEBUG_MESSAGES",
            "OTHER_MESSAGES",
            "CODE_COMMENTS",
            "USER_OBJECTS",
            "OTHER_OBJECTS",
            "METHODS_BYTE",
            "METHODS_BYTE_OBJ",
            "METHODS_SHORT",
            "METHODS_SHORT_OBJ",
            "METHODS_INT",
            "METHODS_INT_OBJ",
            "METHODS_LONG",
            "METHODS_LONG_OBJ",
            "METHODS_FLOAT",
            "METHODS_FLOAT_OBJ",
            "METHODS_DOUBLE",
            "METHODS_DOUBLE_OBJ",
            "VALIDATE",
            "EXTRA_SQL",
            "PACKAGE_NAME",
            "CODE_BASE_DIRECTORY",
            "CODE_BASE_DIRECTORY_FROZEN",
            "SQL_FILE_DIRECTORY",
            "CODE_STATISTICS",
            "JAVA_ACCESS_TYPE",
            "JAVA_NAMING_CONVENTION",
            "TARGET_JVM",
            "METHOD_SQL_PREFIX",
            "METHOD_PLSQL_PREFIX",
            "EXTRA_CLASS_CODE",
            "WEB_SERVICES",
            "MCP_SERVER",
            "MCP_HTTP_TOKEN",
            "MCP_HTTPS",
            "MCP_OAUTH",
            "PROMETHEUS_SERVER",
            "RUN_ON_START",
            "METRICS_PORT",
            "WEB_SERVICES_ABSTRACT_BFILE",
            "WS_PRE_CALL_STUB",
            "WS_POST_CALL_STUB",
            "WS_ALWAYS_RELEASE",
            "WS_RECORD_TYPE",
            "WS_NUMBER_TYPE",
            "WS_INTERFACE_NAME",
            "WS_IMPL_NAME",
            "DAO_FINALIZE",
            "DAO_LOG_TYPE",
            "DAO_LOG_NAME",
            "DAO_FACTORY_NAME",
            "DAO_EJB",
            "DAO_CONNECTION_TYPE",
            "DAO_CONNECTION_NAME",
            "COMMIT_CONNECTIONS",
            "CLOSE_CONNECTIONS",
            "DAO_POOL",
            "DAO_POOL_MAX_SIZE",
            "DAO_POOL_MIN_IDLE",
            "DAO_POOL_MAX_WAIT_MS",
            "DAO_POOL_IDLE_TIMEOUT_MS",
            "DAO_POOL_ON_RETURN",
            "EC30_PROJECT_PATH",
            "EC30_REL_PATH",
            "DEFAULT_TEMPDIR",
            "DEFAULT_TEMP_PREFIX",
            "DEFAULT_TEMP_SUFFIX",
            "POST_SCRIPT_NAME",
            "POST_SCRIPT_CONTENT",
            "XWS_TYPE",
            "XWS_IFACE",
            "XMS_IMPL"));

    /** Populate this schema from a loaded {@code .pb2} property set. */
    public final void fromPb2(Properties props) {
        this.productVersion = props.getProperty("PRODUCT_VERSION");
        this.productBuild = props.getProperty("PRODUCT_BUILD");
        this.uiname = props.getProperty("UINAME");
        this.author = props.getProperty("AUTHOR");
        this.version = props.getProperty("VERSION");
        this.comment = props.getProperty("COMMENT");
        this.hostname = props.getProperty("HOSTNAME");
        this.port = props.getProperty("PORT");
        this.oracleSid = props.getProperty("ORACLE_SID");
        this.user = props.getProperty("USER");
        this.pass = props.getProperty("PASS");
        this.oracleVersion = props.getProperty("ORACLE_VERSION");
        this.otherUserName = props.getProperty("OTHER_USER_NAME");
        this.xSize = props.getProperty("X_SIZE");
        this.ySize = props.getProperty("Y_SIZE");
        this.debugMessages = props.getProperty("DEBUG_MESSAGES");
        this.otherMessages = props.getProperty("OTHER_MESSAGES");
        this.codeComments = props.getProperty("CODE_COMMENTS");
        this.userObjects = props.getProperty("USER_OBJECTS");
        this.otherObjects = props.getProperty("OTHER_OBJECTS");
        this.methodsByte = props.getProperty("METHODS_BYTE");
        this.methodsByteObj = props.getProperty("METHODS_BYTE_OBJ");
        this.methodsShort = props.getProperty("METHODS_SHORT");
        this.methodsShortObj = props.getProperty("METHODS_SHORT_OBJ");
        this.methodsInt = props.getProperty("METHODS_INT");
        this.methodsIntObj = props.getProperty("METHODS_INT_OBJ");
        this.methodsLong = props.getProperty("METHODS_LONG");
        this.methodsLongObj = props.getProperty("METHODS_LONG_OBJ");
        this.methodsFloat = props.getProperty("METHODS_FLOAT");
        this.methodsFloatObj = props.getProperty("METHODS_FLOAT_OBJ");
        this.methodsDouble = props.getProperty("METHODS_DOUBLE");
        this.methodsDoubleObj = props.getProperty("METHODS_DOUBLE_OBJ");
        this.validate = props.getProperty("VALIDATE");
        this.extraSql = props.getProperty("EXTRA_SQL");
        this.packageName = props.getProperty("PACKAGE_NAME");
        this.codeBaseDirectory = props.getProperty("CODE_BASE_DIRECTORY");
        this.codeBaseDirectoryFrozen = props.getProperty("CODE_BASE_DIRECTORY_FROZEN");
        this.sqlFileDirectory = props.getProperty("SQL_FILE_DIRECTORY");
        this.codeStatistics = props.getProperty("CODE_STATISTICS");
        this.javaAccessType = props.getProperty("JAVA_ACCESS_TYPE");
        this.javaNamingConvention = props.getProperty("JAVA_NAMING_CONVENTION");
        this.targetJvm = props.getProperty("TARGET_JVM");
        this.methodSqlPrefix = props.getProperty("METHOD_SQL_PREFIX");
        this.methodPlsqlPrefix = props.getProperty("METHOD_PLSQL_PREFIX");
        this.extraClassCode = props.getProperty("EXTRA_CLASS_CODE");
        this.webServices = props.getProperty("WEB_SERVICES");
        this.mcpServer = props.getProperty("MCP_SERVER");
        this.mcpHttpToken = props.getProperty("MCP_HTTP_TOKEN");
        this.mcpHttps = props.getProperty("MCP_HTTPS");
        this.mcpOAuth = props.getProperty("MCP_OAUTH");
        this.prometheusServer = props.getProperty("PROMETHEUS_SERVER");
        this.runOnStart = props.getProperty("RUN_ON_START");
        this.metricsPort = props.getProperty("METRICS_PORT");
        this.webServicesAbstractBfile = props.getProperty("WEB_SERVICES_ABSTRACT_BFILE");
        this.wsPreCallStub = props.getProperty("WS_PRE_CALL_STUB");
        this.wsPostCallStub = props.getProperty("WS_POST_CALL_STUB");
        this.wsAlwaysRelease = props.getProperty("WS_ALWAYS_RELEASE");
        this.wsRecordType = props.getProperty("WS_RECORD_TYPE");
        this.wsNumberType = props.getProperty("WS_NUMBER_TYPE");
        this.wsInterfaceName = props.getProperty("WS_INTERFACE_NAME");
        this.wsImplName = props.getProperty("WS_IMPL_NAME");
        this.daoFinalize = props.getProperty("DAO_FINALIZE");
        this.daoLogType = props.getProperty("DAO_LOG_TYPE");
        this.daoLogName = props.getProperty("DAO_LOG_NAME");
        this.daoFactoryName = props.getProperty("DAO_FACTORY_NAME");
        this.daoEjb = props.getProperty("DAO_EJB");
        this.daoConnectionType = props.getProperty("DAO_CONNECTION_TYPE");
        this.daoConnectionName = props.getProperty("DAO_CONNECTION_NAME");
        this.commitConnections = props.getProperty("COMMIT_CONNECTIONS");
        this.closeConnections = props.getProperty("CLOSE_CONNECTIONS");
        this.daoPool = props.getProperty("DAO_POOL");
        this.daoPoolMaxSize = props.getProperty("DAO_POOL_MAX_SIZE");
        this.daoPoolMinIdle = props.getProperty("DAO_POOL_MIN_IDLE");
        this.daoPoolMaxWaitMs = props.getProperty("DAO_POOL_MAX_WAIT_MS");
        this.daoPoolIdleTimeoutMs = props.getProperty("DAO_POOL_IDLE_TIMEOUT_MS");
        this.daoPoolOnReturn = props.getProperty("DAO_POOL_ON_RETURN");
        this.ec30ProjectPath = props.getProperty("EC30_PROJECT_PATH");
        this.ec30RelPath = props.getProperty("EC30_REL_PATH");
        this.defaultTempdir = props.getProperty("DEFAULT_TEMPDIR");
        this.defaultTempPrefix = props.getProperty("DEFAULT_TEMP_PREFIX");
        this.defaultTempSuffix = props.getProperty("DEFAULT_TEMP_SUFFIX");
        this.postScriptName = props.getProperty("POST_SCRIPT_NAME");
        this.postScriptContent = props.getProperty("POST_SCRIPT_CONTENT");
        this.xwsType = props.getProperty("XWS_TYPE");
        this.xwsIface = props.getProperty("XWS_IFACE");
        this.xmsImpl = props.getProperty("XMS_IMPL");

        this.sequences = new ArrayList<>();
        this.tables = new ArrayList<>();
        this.procedures = new ArrayList<>();
        this.sqlStatements = new ArrayList<>();
        this.extraProperties = new LinkedHashMap<>();

        TreeMap<Integer, Sequence> seqMap = new TreeMap<>();
        TreeMap<Integer, Table> tabMap = new TreeMap<>();
        TreeMap<Integer, Procedure> procMap = new TreeMap<>();
        TreeMap<Integer, SqlStatement> stmtMap = new TreeMap<>();
        TreeMap<Integer, TreeMap<Integer, SqlParam>> paramMap = new TreeMap<>();

        Pattern single = Pattern.compile("^([A-Z_]+)_([0-9]+)$");
        Pattern doubl = Pattern.compile("^(SQL_PARAM_NAME|SQL_PARAM_DATATYPE|SQL_PARAM_LINENUMBER)_([0-9]+)_([0-9]+)$");
        // A table's MCP descriptions are per OPERATION, so the key carries a trailing operation
        // rather than ending at the index: TABLE_MCP_DESC_<i>_PK, ..._UK_<CONSTRAINT>. That does
        // not match `single`, whose index is terminal, so it needs its own pattern and must be
        // tried FIRST -- otherwise every one of these falls through to extraProperties, where it
        // would round-trip perfectly while nothing in the application could read it.
        Pattern tableDesc = Pattern.compile("^TABLE_MCP_DESC_([0-9]+)_(.+)$");

        for (String key : props.stringPropertyNames()) {
            if (SCALAR_KEYS.contains(key)) {
                continue; // already claimed by a scalar field
            }
            String value = props.getProperty(key);

            Matcher tdm = tableDesc.matcher(key);
            if (tdm.matches()) {
                tab(tabMap, Integer.parseInt(tdm.group(1)))
                        .setMcpDescription(tdm.group(2), value);
                continue;
            }

            Matcher dm = doubl.matcher(key);
            if (dm.matches()) {
                String fam = dm.group(1);
                int stmt = Integer.parseInt(dm.group(2));
                int pi = Integer.parseInt(dm.group(3));
                TreeMap<Integer, SqlParam> pm = paramMap.computeIfAbsent(stmt, k -> new TreeMap<>());
                SqlParam sp = pm.computeIfAbsent(pi, k -> { SqlParam x = new SqlParam(); x.setIndex(k); return x; });
                if (fam.equals("SQL_PARAM_NAME")) { sp.setName(value); }
                else if (fam.equals("SQL_PARAM_DATATYPE")) { sp.setDatatype(value); }
                else { sp.setLineNumber(value); }
                continue;
            }

            Matcher sm = single.matcher(key);
            if (sm.matches()) {
                String fam = sm.group(1);
                int idx = Integer.parseInt(sm.group(2));
                switch (fam) {
                    case "SEQUENCE_NAME": seq(seqMap, idx).setName(value); continue;
                    case "SEQUENCE_USER": seq(seqMap, idx).setUser(value); continue;
                    case "SEQUENCE_MCP_DESC": seq(seqMap, idx).setMcpDescription(value); continue;
                    case "PROC_MCP_DESC": proc(procMap, idx).setMcpDescription(value); continue;
                    case "SQL_MCP_DESC": stmt(stmtMap, idx).setMcpDescription(value); continue;
                    case "TABLE_NAME": tab(tabMap, idx).setName(value); continue;
                    case "TABLE_USER": tab(tabMap, idx).setUser(value); continue;
                    case "TABLE_MCP_CRUD": tab(tabMap, idx).setMcpCrud(value); continue;
                    case "PROC_NAME": proc(procMap, idx).setName(value); continue;
                    case "PROC_USER": proc(procMap, idx).setUser(value); continue;
                    case "PROC_PACKAGE": proc(procMap, idx).setPkg(value); continue;
                    case "PROC_OVERLOAD": proc(procMap, idx).setOverload(value); continue;
                    case "SQL_FILENAME": stmt(stmtMap, idx).setFilename(value); continue;
                    // The statement text itself. THIS SWITCH is what claims a key: a family that
                    // is not listed here falls through to extraProperties, where it round-trips
                    // perfectly and the model never sees it -- so a round-trip test passes on a
                    // key the code has not actually understood. Add the case, not a regex.
                    case "SQL_TEXT": stmt(stmtMap, idx).setSql(value); continue;
                    case "SQL_CREATE_CLASS": stmt(stmtMap, idx).setCreateClass(value); continue;
                    case "SQL_TURN_CURSORS_INTO_RECORDS": stmt(stmtMap, idx).setTurnCursorsIntoRecords(value); continue;
                    default: break;
                }
            }

            // Unrecognised key: keep it so the round-trip is lossless.
            this.extraProperties.put(key, value);
        }

        // Fold parsed parameters into their statements (creating statements that had only params).
        for (Map.Entry<Integer, TreeMap<Integer, SqlParam>> e : paramMap.entrySet()) {
            SqlStatement s = stmt(stmtMap, e.getKey());
            s.getParams().addAll(e.getValue().values());
        }

        this.sequences.addAll(seqMap.values());
        this.tables.addAll(tabMap.values());
        this.procedures.addAll(procMap.values());
        this.sqlStatements.addAll(stmtMap.values());
    }

    private static Sequence seq(TreeMap<Integer, Sequence> m, int i) {
        return m.computeIfAbsent(i, k -> { Sequence x = new Sequence(); x.setIndex(k); return x; });
    }

    private static Table tab(TreeMap<Integer, Table> m, int i) {
        return m.computeIfAbsent(i, k -> { Table x = new Table(); x.setIndex(k); return x; });
    }

    private static Procedure proc(TreeMap<Integer, Procedure> m, int i) {
        return m.computeIfAbsent(i, k -> { Procedure x = new Procedure(); x.setIndex(k); return x; });
    }

    private static SqlStatement stmt(TreeMap<Integer, SqlStatement> m, int i) {
        return m.computeIfAbsent(i, k -> { SqlStatement x = new SqlStatement(); x.setIndex(k); return x; });
    }

    /** Render this schema back to a {@code .pb2} property set. */
    public Properties toPb2() {
        Properties p = new Properties();
        p.putAll(extraProperties);
        if (productVersion != null) { p.setProperty("PRODUCT_VERSION", productVersion); }
        if (productBuild != null) { p.setProperty("PRODUCT_BUILD", productBuild); }
        if (uiname != null) { p.setProperty("UINAME", uiname); }
        if (author != null) { p.setProperty("AUTHOR", author); }
        if (version != null) { p.setProperty("VERSION", version); }
        if (comment != null) { p.setProperty("COMMENT", comment); }
        if (hostname != null) { p.setProperty("HOSTNAME", hostname); }
        if (port != null) { p.setProperty("PORT", port); }
        if (oracleSid != null) { p.setProperty("ORACLE_SID", oracleSid); }
        if (user != null) { p.setProperty("USER", user); }
        if (pass != null) { p.setProperty("PASS", pass); }
        if (oracleVersion != null) { p.setProperty("ORACLE_VERSION", oracleVersion); }
        if (otherUserName != null) { p.setProperty("OTHER_USER_NAME", otherUserName); }
        if (xSize != null) { p.setProperty("X_SIZE", xSize); }
        if (ySize != null) { p.setProperty("Y_SIZE", ySize); }
        if (debugMessages != null) { p.setProperty("DEBUG_MESSAGES", debugMessages); }
        if (otherMessages != null) { p.setProperty("OTHER_MESSAGES", otherMessages); }
        if (codeComments != null) { p.setProperty("CODE_COMMENTS", codeComments); }
        if (userObjects != null) { p.setProperty("USER_OBJECTS", userObjects); }
        if (otherObjects != null) { p.setProperty("OTHER_OBJECTS", otherObjects); }
        if (methodsByte != null) { p.setProperty("METHODS_BYTE", methodsByte); }
        if (methodsByteObj != null) { p.setProperty("METHODS_BYTE_OBJ", methodsByteObj); }
        if (methodsShort != null) { p.setProperty("METHODS_SHORT", methodsShort); }
        if (methodsShortObj != null) { p.setProperty("METHODS_SHORT_OBJ", methodsShortObj); }
        if (methodsInt != null) { p.setProperty("METHODS_INT", methodsInt); }
        if (methodsIntObj != null) { p.setProperty("METHODS_INT_OBJ", methodsIntObj); }
        if (methodsLong != null) { p.setProperty("METHODS_LONG", methodsLong); }
        if (methodsLongObj != null) { p.setProperty("METHODS_LONG_OBJ", methodsLongObj); }
        if (methodsFloat != null) { p.setProperty("METHODS_FLOAT", methodsFloat); }
        if (methodsFloatObj != null) { p.setProperty("METHODS_FLOAT_OBJ", methodsFloatObj); }
        if (methodsDouble != null) { p.setProperty("METHODS_DOUBLE", methodsDouble); }
        if (methodsDoubleObj != null) { p.setProperty("METHODS_DOUBLE_OBJ", methodsDoubleObj); }
        if (validate != null) { p.setProperty("VALIDATE", validate); }
        if (extraSql != null) { p.setProperty("EXTRA_SQL", extraSql); }
        if (packageName != null) { p.setProperty("PACKAGE_NAME", packageName); }
        if (codeBaseDirectory != null) { p.setProperty("CODE_BASE_DIRECTORY", codeBaseDirectory); }
        if (codeBaseDirectoryFrozen != null) { p.setProperty("CODE_BASE_DIRECTORY_FROZEN", codeBaseDirectoryFrozen); }
        if (sqlFileDirectory != null) { p.setProperty("SQL_FILE_DIRECTORY", sqlFileDirectory); }
        if (codeStatistics != null) { p.setProperty("CODE_STATISTICS", codeStatistics); }
        if (javaAccessType != null) { p.setProperty("JAVA_ACCESS_TYPE", javaAccessType); }
        if (javaNamingConvention != null) { p.setProperty("JAVA_NAMING_CONVENTION", javaNamingConvention); }
        if (targetJvm != null) { p.setProperty("TARGET_JVM", targetJvm); }
        if (methodSqlPrefix != null) { p.setProperty("METHOD_SQL_PREFIX", methodSqlPrefix); }
        if (methodPlsqlPrefix != null) { p.setProperty("METHOD_PLSQL_PREFIX", methodPlsqlPrefix); }
        if (extraClassCode != null) { p.setProperty("EXTRA_CLASS_CODE", extraClassCode); }
        if (webServices != null) { p.setProperty("WEB_SERVICES", webServices); }
        if (mcpServer != null) { p.setProperty("MCP_SERVER", mcpServer); }
        if (mcpHttpToken != null) { p.setProperty("MCP_HTTP_TOKEN", mcpHttpToken); }
        if (mcpHttps != null) { p.setProperty("MCP_HTTPS", mcpHttps); }
        if (mcpOAuth != null) { p.setProperty("MCP_OAUTH", mcpOAuth); }
        if (prometheusServer != null) { p.setProperty("PROMETHEUS_SERVER", prometheusServer); }
        if (runOnStart != null) { p.setProperty("RUN_ON_START", runOnStart); }
        if (metricsPort != null) { p.setProperty("METRICS_PORT", metricsPort); }
        if (webServicesAbstractBfile != null) { p.setProperty("WEB_SERVICES_ABSTRACT_BFILE", webServicesAbstractBfile); }
        if (wsPreCallStub != null) { p.setProperty("WS_PRE_CALL_STUB", wsPreCallStub); }
        if (wsPostCallStub != null) { p.setProperty("WS_POST_CALL_STUB", wsPostCallStub); }
        if (wsAlwaysRelease != null) { p.setProperty("WS_ALWAYS_RELEASE", wsAlwaysRelease); }
        if (wsRecordType != null) { p.setProperty("WS_RECORD_TYPE", wsRecordType); }
        if (wsNumberType != null) { p.setProperty("WS_NUMBER_TYPE", wsNumberType); }
        if (wsInterfaceName != null) { p.setProperty("WS_INTERFACE_NAME", wsInterfaceName); }
        if (wsImplName != null) { p.setProperty("WS_IMPL_NAME", wsImplName); }
        if (daoFinalize != null) { p.setProperty("DAO_FINALIZE", daoFinalize); }
        if (daoLogType != null) { p.setProperty("DAO_LOG_TYPE", daoLogType); }
        if (daoLogName != null) { p.setProperty("DAO_LOG_NAME", daoLogName); }
        if (daoFactoryName != null) { p.setProperty("DAO_FACTORY_NAME", daoFactoryName); }
        if (daoEjb != null) { p.setProperty("DAO_EJB", daoEjb); }
        if (daoConnectionType != null) { p.setProperty("DAO_CONNECTION_TYPE", daoConnectionType); }
        if (daoConnectionName != null) { p.setProperty("DAO_CONNECTION_NAME", daoConnectionName); }
        if (commitConnections != null) { p.setProperty("COMMIT_CONNECTIONS", commitConnections); }
        if (closeConnections != null) { p.setProperty("CLOSE_CONNECTIONS", closeConnections); }
        if (daoPool != null) { p.setProperty("DAO_POOL", daoPool); }
        if (daoPoolMaxSize != null) { p.setProperty("DAO_POOL_MAX_SIZE", daoPoolMaxSize); }
        if (daoPoolMinIdle != null) { p.setProperty("DAO_POOL_MIN_IDLE", daoPoolMinIdle); }
        if (daoPoolMaxWaitMs != null) { p.setProperty("DAO_POOL_MAX_WAIT_MS", daoPoolMaxWaitMs); }
        if (daoPoolIdleTimeoutMs != null) { p.setProperty("DAO_POOL_IDLE_TIMEOUT_MS", daoPoolIdleTimeoutMs); }
        if (daoPoolOnReturn != null) { p.setProperty("DAO_POOL_ON_RETURN", daoPoolOnReturn); }
        if (ec30ProjectPath != null) { p.setProperty("EC30_PROJECT_PATH", ec30ProjectPath); }
        if (ec30RelPath != null) { p.setProperty("EC30_REL_PATH", ec30RelPath); }
        if (defaultTempdir != null) { p.setProperty("DEFAULT_TEMPDIR", defaultTempdir); }
        if (defaultTempPrefix != null) { p.setProperty("DEFAULT_TEMP_PREFIX", defaultTempPrefix); }
        if (defaultTempSuffix != null) { p.setProperty("DEFAULT_TEMP_SUFFIX", defaultTempSuffix); }
        if (postScriptName != null) { p.setProperty("POST_SCRIPT_NAME", postScriptName); }
        if (postScriptContent != null) { p.setProperty("POST_SCRIPT_CONTENT", postScriptContent); }
        if (xwsType != null) { p.setProperty("XWS_TYPE", xwsType); }
        if (xwsIface != null) { p.setProperty("XWS_IFACE", xwsIface); }
        if (xmsImpl != null) { p.setProperty("XMS_IMPL", xmsImpl); }
        for (Sequence s : sequences) { s.toPb2(p); }
        for (Table t : tables) { t.toPb2(p); }
        for (Procedure pr : procedures) { pr.toPb2(p); }
        for (SqlStatement st : sqlStatements) { st.toPb2(p); }
        return p;
    }

    /** Write this schema to a {@code .pb2} file. */
    public void writePb2(File file) throws IOException {
        Properties p = toPb2();
        try (OutputStream out = new FileOutputStream(file)) {
            p.store(out, com.mcpdbwizard.pub.Namer.param_prod_name + " Properties");
        }
    }

    /** Serialise this schema to JSON. */
    public String toJson() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("productVersion", productVersion);
        m.put("productBuild", productBuild);
        m.put("uiname", uiname);
        m.put("author", author);
        m.put("version", version);
        m.put("comment", comment);
        m.put("hostname", hostname);
        m.put("port", port);
        m.put("oracleSid", oracleSid);
        m.put("user", user);
        m.put("pass", pass);
        m.put("oracleVersion", oracleVersion);
        m.put("otherUserName", otherUserName);
        m.put("xSize", xSize);
        m.put("ySize", ySize);
        m.put("debugMessages", debugMessages);
        m.put("otherMessages", otherMessages);
        m.put("codeComments", codeComments);
        m.put("userObjects", userObjects);
        m.put("otherObjects", otherObjects);
        m.put("methodsByte", methodsByte);
        m.put("methodsByteObj", methodsByteObj);
        m.put("methodsShort", methodsShort);
        m.put("methodsShortObj", methodsShortObj);
        m.put("methodsInt", methodsInt);
        m.put("methodsIntObj", methodsIntObj);
        m.put("methodsLong", methodsLong);
        m.put("methodsLongObj", methodsLongObj);
        m.put("methodsFloat", methodsFloat);
        m.put("methodsFloatObj", methodsFloatObj);
        m.put("methodsDouble", methodsDouble);
        m.put("methodsDoubleObj", methodsDoubleObj);
        m.put("validate", validate);
        m.put("extraSql", extraSql);
        m.put("packageName", packageName);
        m.put("codeBaseDirectory", codeBaseDirectory);
        m.put("codeBaseDirectoryFrozen", codeBaseDirectoryFrozen);
        m.put("sqlFileDirectory", sqlFileDirectory);
        m.put("codeStatistics", codeStatistics);
        m.put("javaAccessType", javaAccessType);
        m.put("javaNamingConvention", javaNamingConvention);
        m.put("targetJvm", targetJvm);
        m.put("methodSqlPrefix", methodSqlPrefix);
        m.put("methodPlsqlPrefix", methodPlsqlPrefix);
        m.put("extraClassCode", extraClassCode);
        m.put("webServices", webServices);
        m.put("mcpServer", mcpServer);
        m.put("mcpHttpToken", mcpHttpToken);
        m.put("mcpHttps", mcpHttps);
        m.put("mcpOAuth", mcpOAuth);
        m.put("prometheusServer", prometheusServer);
        m.put("runOnStart", runOnStart);
        m.put("metricsPort", metricsPort);
        m.put("webServicesAbstractBfile", webServicesAbstractBfile);
        m.put("wsPreCallStub", wsPreCallStub);
        m.put("wsPostCallStub", wsPostCallStub);
        m.put("wsAlwaysRelease", wsAlwaysRelease);
        m.put("wsRecordType", wsRecordType);
        m.put("wsNumberType", wsNumberType);
        m.put("wsInterfaceName", wsInterfaceName);
        m.put("wsImplName", wsImplName);
        m.put("daoFinalize", daoFinalize);
        m.put("daoLogType", daoLogType);
        m.put("daoLogName", daoLogName);
        m.put("daoFactoryName", daoFactoryName);
        m.put("daoEjb", daoEjb);
        m.put("daoConnectionType", daoConnectionType);
        m.put("daoConnectionName", daoConnectionName);
        m.put("commitConnections", commitConnections);
        m.put("closeConnections", closeConnections);
        m.put("daoPool", daoPool);
        m.put("daoPoolMaxSize", daoPoolMaxSize);
        m.put("daoPoolMinIdle", daoPoolMinIdle);
        m.put("daoPoolMaxWaitMs", daoPoolMaxWaitMs);
        m.put("daoPoolIdleTimeoutMs", daoPoolIdleTimeoutMs);
        m.put("daoPoolOnReturn", daoPoolOnReturn);
        m.put("ec30ProjectPath", ec30ProjectPath);
        m.put("ec30RelPath", ec30RelPath);
        m.put("defaultTempdir", defaultTempdir);
        m.put("defaultTempPrefix", defaultTempPrefix);
        m.put("defaultTempSuffix", defaultTempSuffix);
        m.put("postScriptName", postScriptName);
        m.put("postScriptContent", postScriptContent);
        m.put("xwsType", xwsType);
        m.put("xwsIface", xwsIface);
        m.put("xmsImpl", xmsImpl);
        List<Object> seqList = new ArrayList<>();
        for (Sequence s : sequences) { seqList.add(s.toJsonMap()); }
        m.put("sequences", seqList);
        List<Object> tabList = new ArrayList<>();
        for (Table t : tables) { tabList.add(t.toJsonMap()); }
        m.put("tables", tabList);
        List<Object> procList = new ArrayList<>();
        for (Procedure pr : procedures) { procList.add(pr.toJsonMap()); }
        m.put("procedures", procList);
        List<Object> stmtList = new ArrayList<>();
        for (SqlStatement st : sqlStatements) { stmtList.add(st.toJsonMap()); }
        m.put("sqlStatements", stmtList);
        LinkedHashMap<String, Object> extra = new LinkedHashMap<>();
        extra.putAll(extraProperties);
        m.put("extraProperties", extra);
        return Json.write(m);
    }

    /** Populate this schema from a JSON document produced by {@link #toJson()}. */
    @SuppressWarnings("unchecked")
    public final void fromJson(String json) {
        Map<String, Object> m = (Map<String, Object>) Json.parse(json);
        this.productVersion = (String) m.get("productVersion");
        this.productBuild = (String) m.get("productBuild");
        this.uiname = (String) m.get("uiname");
        this.author = (String) m.get("author");
        this.version = (String) m.get("version");
        this.comment = (String) m.get("comment");
        this.hostname = (String) m.get("hostname");
        this.port = (String) m.get("port");
        this.oracleSid = (String) m.get("oracleSid");
        this.user = (String) m.get("user");
        this.pass = (String) m.get("pass");
        this.oracleVersion = (String) m.get("oracleVersion");
        this.otherUserName = (String) m.get("otherUserName");
        this.xSize = (String) m.get("xSize");
        this.ySize = (String) m.get("ySize");
        this.debugMessages = (String) m.get("debugMessages");
        this.otherMessages = (String) m.get("otherMessages");
        this.codeComments = (String) m.get("codeComments");
        this.userObjects = (String) m.get("userObjects");
        this.otherObjects = (String) m.get("otherObjects");
        this.methodsByte = (String) m.get("methodsByte");
        this.methodsByteObj = (String) m.get("methodsByteObj");
        this.methodsShort = (String) m.get("methodsShort");
        this.methodsShortObj = (String) m.get("methodsShortObj");
        this.methodsInt = (String) m.get("methodsInt");
        this.methodsIntObj = (String) m.get("methodsIntObj");
        this.methodsLong = (String) m.get("methodsLong");
        this.methodsLongObj = (String) m.get("methodsLongObj");
        this.methodsFloat = (String) m.get("methodsFloat");
        this.methodsFloatObj = (String) m.get("methodsFloatObj");
        this.methodsDouble = (String) m.get("methodsDouble");
        this.methodsDoubleObj = (String) m.get("methodsDoubleObj");
        this.validate = (String) m.get("validate");
        this.extraSql = (String) m.get("extraSql");
        this.packageName = (String) m.get("packageName");
        this.codeBaseDirectory = (String) m.get("codeBaseDirectory");
        this.codeBaseDirectoryFrozen = (String) m.get("codeBaseDirectoryFrozen");
        this.sqlFileDirectory = (String) m.get("sqlFileDirectory");
        this.codeStatistics = (String) m.get("codeStatistics");
        this.javaAccessType = (String) m.get("javaAccessType");
        this.javaNamingConvention = (String) m.get("javaNamingConvention");
        this.targetJvm = (String) m.get("targetJvm");
        this.methodSqlPrefix = (String) m.get("methodSqlPrefix");
        this.methodPlsqlPrefix = (String) m.get("methodPlsqlPrefix");
        this.extraClassCode = (String) m.get("extraClassCode");
        this.webServices = (String) m.get("webServices");
        this.mcpServer = (String) m.get("mcpServer");
        this.mcpHttpToken = (String) m.get("mcpHttpToken");
        this.mcpHttps = (String) m.get("mcpHttps");
        this.mcpOAuth = (String) m.get("mcpOAuth");
        this.prometheusServer = (String) m.get("prometheusServer");
        this.runOnStart = (String) m.get("runOnStart");
        this.metricsPort = (String) m.get("metricsPort");
        this.webServicesAbstractBfile = (String) m.get("webServicesAbstractBfile");
        this.wsPreCallStub = (String) m.get("wsPreCallStub");
        this.wsPostCallStub = (String) m.get("wsPostCallStub");
        this.wsAlwaysRelease = (String) m.get("wsAlwaysRelease");
        this.wsRecordType = (String) m.get("wsRecordType");
        this.wsNumberType = (String) m.get("wsNumberType");
        this.wsInterfaceName = (String) m.get("wsInterfaceName");
        this.wsImplName = (String) m.get("wsImplName");
        this.daoFinalize = (String) m.get("daoFinalize");
        this.daoLogType = (String) m.get("daoLogType");
        this.daoLogName = (String) m.get("daoLogName");
        this.daoFactoryName = (String) m.get("daoFactoryName");
        this.daoEjb = (String) m.get("daoEjb");
        this.daoConnectionType = (String) m.get("daoConnectionType");
        this.daoConnectionName = (String) m.get("daoConnectionName");
        this.commitConnections = (String) m.get("commitConnections");
        this.closeConnections = (String) m.get("closeConnections");
        this.daoPool = (String) m.get("daoPool");
        this.daoPoolMaxSize = (String) m.get("daoPoolMaxSize");
        this.daoPoolMinIdle = (String) m.get("daoPoolMinIdle");
        this.daoPoolMaxWaitMs = (String) m.get("daoPoolMaxWaitMs");
        this.daoPoolIdleTimeoutMs = (String) m.get("daoPoolIdleTimeoutMs");
        this.daoPoolOnReturn = (String) m.get("daoPoolOnReturn");
        this.ec30ProjectPath = (String) m.get("ec30ProjectPath");
        this.ec30RelPath = (String) m.get("ec30RelPath");
        this.defaultTempdir = (String) m.get("defaultTempdir");
        this.defaultTempPrefix = (String) m.get("defaultTempPrefix");
        this.defaultTempSuffix = (String) m.get("defaultTempSuffix");
        this.postScriptName = (String) m.get("postScriptName");
        this.postScriptContent = (String) m.get("postScriptContent");
        this.xwsType = (String) m.get("xwsType");
        this.xwsIface = (String) m.get("xwsIface");
        this.xmsImpl = (String) m.get("xmsImpl");
        this.sequences = new ArrayList<>();
        for (Object o : asList(m.get("sequences"))) { this.sequences.add(Sequence.fromJsonMap((Map<String, Object>) o)); }
        this.tables = new ArrayList<>();
        for (Object o : asList(m.get("tables"))) { this.tables.add(Table.fromJsonMap((Map<String, Object>) o)); }
        this.procedures = new ArrayList<>();
        for (Object o : asList(m.get("procedures"))) { this.procedures.add(Procedure.fromJsonMap((Map<String, Object>) o)); }
        this.sqlStatements = new ArrayList<>();
        for (Object o : asList(m.get("sqlStatements"))) { this.sqlStatements.add(SqlStatement.fromJsonMap((Map<String, Object>) o)); }
        this.extraProperties = new LinkedHashMap<>();
        Object extra = m.get("extraProperties");
        if (extra instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) extra).entrySet()) {
                this.extraProperties.put(e.getKey(), (String) e.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return (o instanceof List) ? (List<Object>) o : new ArrayList<>();
    }
}
