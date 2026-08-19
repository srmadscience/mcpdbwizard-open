package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.Namer;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class SqlStatementDictionary {
    /**
     * Array of supported Oracle Versions
     */
    public static final String[] oracleVersions =
            {"12.1.0", "11.2.0", "11.1.0", "10.2.0", "10.1.0", "9.2.0", "9.0.1", "8.1.7", "8.1.6", "8.1.5", "DB2 v9.7", "DB2 v10"};

    /**
     * Array of supported DB2 Versions
     */
    public static final String[] db2Versions =
            {"DB2 v9.7", "DB2 v10"};

    /**
     * Latest supported Oracle Versions
     */
    public static final String latestOracleVersion = "11.2.0";

    /**
     * Generic Arg query search mode
     */
    public static final int GENERIC_ARG_QUERY = 0;

    /**
     * PL/SQL REcord Arg query search mode
     */
    public static final int PLSQL_ARG_QUERY = 1;

    /**
     * Attr Arg query search mode
     */
    public static final int ATTR_ARG_QUERY = 2;

    /**
     * DB type  Arg query search mode
     */
    public static final int DB_ARG_QUERY = 3;
    public static final String plsqlQry =
            "select /* " + Namer.param_product_name + " */ text "
                    + "from all_source "
                    + "where owner = ? "
                    + "and   type  = ? "
                    + "and   name = ? "
                    + "order by line";
    public static final String db2PlsqlQry =
            "select /* " + Namer.param_product_name + " */ text "
                    + "from all_source "
                    + "where owner = ? "
                    + "and   type  = ? "
                    + "and   name = ? ";
    public static final String plsqlErrorQry =
            "select /* " + Namer.param_product_name + " */ owner||'.'||name||': Line '||line||', Column '||position||':'||text text "
                    + "from all_errors "
                    + "where owner = ? "
                    + "and   type  = ? "
                    + "and   name = ? "
                    + "order by sequence ";
    public static final String db2PlsqlErrorQry =
            "select /* " + Namer.param_product_name + " */ owner||'.'||name||': Line '||line||', Column '||'Unknown'||':'||text text "
                    + "from all_errors "
                    + "where owner = ? "
                    + "and   type  = ? "
                    + "and   name = ? "
                    + " /* order by line*/ ";
    public static final String plsqlErrorQryAtt =
            "select /* " + Namer.param_product_name + " */ owner||'.'||name||': Line '||line||', Column '||position||':'||text text "
                    + "from all_errors "
                    + "where owner = ? "
                    + "and   type  = ? "
                    + "and   name = ? "
                    + "and   attribute = 'ERROR' "
                    + "order by sequence ";
    /**
     * Sql to retieve sequence info
     */
    private static final String seqQry =
            "select /* " + Namer.param_product_name + " */ o.owner \"Sequence Owner\", o.object_name \"Sequence Name\", 'X' \"Selected\"  "
                    + " , decode(o.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\"  "
                    + " , s.sequence_owner \"Real Owner\", s.sequence_name \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\""
                    + "from all_objects o  "
                    + ", all_sequences s  "
                    + "where o.object_type = 'SEQUENCE' "
                    + "and   o.object_name = s.sequence_name  "
                    + "and   o.owner = s.sequence_owner  "
                    + "and   (o.owner = ? or  o.owner LIKE ?)  "
                    + "union all   "
                    + "select asyn.owner \"Sequence Owner\", asyn.synonym_name \"Sequence Name\", 'X' \"Selected\"  "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\"  "
                    + " , aseqs.sequence_owner \"Real Owner\", aseqs.sequence_name \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\""
                    + "from all_synonyms asyn  "
                    + "  ,  all_sequences aseqs  "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = aseqs.sequence_owner "
                    + "and asyn.table_name = aseqs.sequence_name "
                    + "order by 1,2,4 ";
    /**
     * Sql to retieve sequence info
     */
    private static final String fakeSeqQry =
            "select /* " + Namer.param_product_name + " */ user \"Sequence Owner\", sequence_name \"Sequence Name\", 'X' \"Selected\"   "
                    + " , decode(user,user,'User Object' ,'Other User''s Object') \"Accessed Via\"   "
                    + " , user \"Real Owner\", s.sequence_name \"Real Name\"   "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\" "
                    + "from user_sequences s   "
                    + "union all    "
                    + "select user \"Sequence Owner\", asyn.synonym_name \"Sequence Name\", 'X' \"Selected\"   "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\"   "
                    + " , aseqs.sequence_owner \"Real Owner\", aseqs.sequence_name \"Real Name\"   "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\" "
                    + "from user_synonyms asyn   "
                    + "  ,  all_sequences aseqs   "
                    + "where (user = ? or  user = ? or  user = ? or  user = ? )  "
                    + "and asyn.table_owner = aseqs.sequence_owner  "
                    + "and asyn.table_name = aseqs.sequence_name  "
                    + "order by 1,2,4  ";
    private static final String funcQry920_0_np_local =
            // Unpackaged, local, 0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\",'' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_objects o2 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    //  + "and not exists (select null from all_arguments o3 where o2.owner = o3.owner "
                    //  + "                and o2.object_name = o3.object_name and o3.package_name is null) "
                    + "and o2.object_type = 'PROCEDURE' "
                    + "group by o2.owner, o2.object_name ";
    private static final String DB2ExcludeList = " ('<','<=','<>','=','>','>=','CHAR','VARCHAR', 'DB2SECURITYLABEL','DB2SQLSTATE')";
    private static final String db2_funcQry920_0_np_local =
            // Unpackaged, local, 0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\",'' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_objects o2 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    //  + "and not exists (select null from all_arguments o3 where o2.owner = o3.owner "
                    //  + "                and o2.object_name = o3.object_name and o3.package_name is null) "
                    + "and o2.object_type = 'PROCEDURE' "
                    + "and o2.object_name NOT IN " + DB2ExcludeList
                    + "group by o2.owner, o2.object_name ";
    private static final String db2_fake_funcQry_2 =
            // Unpackaged, local, 0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\",'' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_objects o2 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    //  + "and not exists (select null from all_arguments o3 where o2.owner = o3.owner "
                    //  + "                and o2.object_name = o3.object_name and o3.package_name is null) "
                    + "and o2.object_type = 'PROCEDURE' and 1 = 2 "
                    + "group by o2.owner, o2.object_name ";
    private static final String fake_funcQry920_0_np_local =
            // Unpackaged, local, 0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\",'' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_objects o2 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    //  + "and not exists (select null from all_arguments o3 where o2.owner = o3.owner "
                    //  + "                and o2.object_name = o3.object_name and o3.package_name is null) "
                    + "and o2.object_type = 'PROCEDURE' "
                    + "group by o2.owner, o2.object_name ";
    private static final String funcQry901_0_np_local =
            // Unpackaged, local, 0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\",'' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_objects o2 /* 901 */ "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    //   + "and not exists (select null from all_arguments o3 where o2.owner = o3.owner "
                    //   + "                and o2.object_name = o3.object_name and o3.package_name||'' = o2.object_name) "
                    + "and o2.object_type = 'PROCEDURE' "
                    + "group by o2.owner, o2.object_name ";
    private static final String funcQry920_n_x_local =
            // local, >0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\",o2.package_name \"Package\",o2.package_name \"Real Package\" "
                    + "   , o2.overload \"Overload\" "
                    + "from all_arguments o2 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    + "group by o2.owner, o2.object_name, o2.package_name,o2.overload ";
    private static final String db2_funcQry920_n_x_local =
            // local, >0 args
            "select /* " + Namer.param_product_name + " */ o3.owner \"Owner\", o3.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o3.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o3.owner \"Real Owner\", o3.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\",o3.package_name \"Package\",o3.package_name \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_arguments o3 "
                    + "where (o3.owner = ? or  o3.owner LIKE ?) "
                    + "and o3.argument_name is not null "
                    + "and o3.object_name NOT IN " + DB2ExcludeList
                    + "group by o3.owner, o3.object_name, o3.package_name ";
    private static final String fake_funcQry920_n_x_local =
            // local, >0 args
            "select /* " + Namer.param_product_name + " */ user \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(user,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , user \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\",o2.package_name \"Package\",o2.package_name \"Real Package\" "
                    + "   , o2.overload \"Overload\" "
                    + "from user_arguments o2 "
                    + "where (user = ? or  user = ?) "
                    + "group by user, o2.object_name, o2.package_name,o2.overload ";
    private static final String funcQry901_n_x_local =
            // local, >0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\" "
                    + " ,decode(o999.object_type,'PACKAGE',o2.package_name,null) \"Package\" "
                    + " ,decode(o999.object_type,'PACKAGE',o2.package_name,null) \"Real Package\" "
                    + "   , o2.overload \"Overload\" "
                    + "from all_arguments o2 "
                    + "   , all_objects o999 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    + "and  (o2.owner = ? or  o2.owner LIKE ?) "
                    + "and o2.owner = o999.owner (+) "
                    + "and o2.package_name = o999.object_name (+) "
                    + "and 'PACKAGE' = o999.object_type (+) "
                    + "group by o2.owner, o2.object_name, decode(o999.object_type,'PACKAGE',o2.package_name,null),o2.overload ";
    private static final String fake_funcQry901_n_x_local =
            // local, >0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\" "
                    + " ,decode(o999.object_type,'PACKAGE',o2.package_name,null) \"Package\" "
                    + " ,decode(o999.object_type,'PACKAGE',o2.package_name,null) \"Real Package\" "
                    + "   , o2.overload \"Overload\" "
                    + "from all_arguments o2 "
                    + "   , all_objects o999 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    + "and  (o2.owner = ? or  o2.owner LIKE ?) "
                    + "and o2.owner = o999.owner (+) "
                    + "and o2.package_name = o999.object_name (+) "
                    + "and 'PACKAGE' = o999.object_type (+) "
                    + "group by o2.owner, o2.object_name, decode(o999.object_type,'PACKAGE',o2.package_name,null),o2.overload ";
    private static final String funcQry901_n_x_local_old =
            // local, >0 args
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\",o2.package_name \"Package\",o2.package_name \"Real Package\" "
                    + "   , o2.overload \"Overload\" "
                    + "from all_arguments o2 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    + "and  (o2.owner = ? or  o2.owner LIKE ?) "
                    + "group by o2.owner, o2.object_name, o2.package_name,o2.overload ";
    private static final String funcQry901_0_np_syn =
            // Unpackaged Procedures and functions that are synonym accesible and have 0 args
            "select as2.owner \"Owner\",  as2.synonym_name \"Name\", 'X' \"Selected\" "
                    + "     , decode(as2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , o6.owner \"Real Owner\", o6.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_synonyms as2 "
                    + "   , all_objects o6 "
                    + "where (as2.owner = ? or  as2.table_owner LIKE ?) "
                    + "and as2.owner||'' IN ('PUBLIC',user)   "
                    + "and as2.table_owner = o6.owner "
                    + "and as2.table_name = o6.object_name "
                    + "and o6.object_type IN ('PROCEDURE','FUNCTION') "
                    //    + "and not exists (select null from all_arguments o33 where o6.owner = o33.owner "
                    //    + "                and o6.object_name = o33.object_name and o33.package_name = o6.object_name ) "
                    + "group by as2.owner, as2.synonym_name, o6.owner, o6.object_name ";
    private static final String fake_funcQry901_0_np_syn =
            // Unpackaged Procedures and functions that are synonym accesible and have 0 args
            "select as2.owner \"Owner\",  as2.synonym_name \"Name\", 'X' \"Selected\" "
                    + "     , decode(as2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , o6.owner \"Real Owner\", o6.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_synonyms as2 "
                    + "   , all_objects o6 "
                    + "where (as2.owner = ? or  as2.table_owner LIKE ?) "
                    + "and as2.owner||'' IN ('PUBLIC',user)   "
                    + "and as2.table_owner = o6.owner "
                    + "and as2.table_name = o6.object_name "
                    + "and o6.object_type IN ('PROCEDURE','FUNCTION') "
                    //    + "and not exists (select null from all_arguments o33 where o6.owner = o33.owner "
                    //    + "                and o6.object_name = o33.object_name and o33.package_name = o6.object_name ) "
                    + "group by as2.owner, as2.synonym_name, o6.owner, o6.object_name ";
    private static final String funcQry920_0_np_syn =
            // Unpackaged Procedures and functions that are synonym accesible and have 0 args
            "select as2.owner \"Owner\",  as2.synonym_name  \"Name\", 'X' \"Selected\" "
                    + "     , decode(as2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , o6.owner \"Real Owner\", o6.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_synonyms as2 "
                    + "   , all_objects o6 "
                    + "where (as2.owner = ? or  as2.table_owner LIKE ?) "
                    + "and as2.table_owner = o6.owner "
                    + "and as2.owner||'' IN ('PUBLIC',user) "
                    + "and as2.table_name = o6.object_name "
                    + "and o6.object_type IN ('PROCEDURE','FUNCTION') "
                    //  + "and not exists (select null from all_arguments o33 where o6.owner = o33.owner "
                    // + "                and o6.object_name = o33.object_name and o33.package_name is null) "
                    + "group by as2.owner, as2.synonym_name, o6.owner, o6.object_name ";
    private static final String fake_funcQry920_0_np_syn =
            // Unpackaged Procedures and functions that are synonym accesible and have 0 args
            "select user \"Owner\",  as2.synonym_name  \"Name\", 'X' \"Selected\" "
                    + ", decode(user,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\"  "
                    + ", as2.table_owner \"Real Owner\", as2.table_name \"Real Name\" "
                    + ", 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\" "
                    + ", '' \"Overload\"  "
                    + "from user_synonyms as2 "
                    + ", all_objects o6 "
                    + "where (user = ? or  user = ?) "
                    + "and as2.table_owner = o6.owner  "
                    + "and as2.table_name = o6.object_name "
                    + "and o6.object_type IN ('PROCEDURE','FUNCTION') "
                    + "group by as2.synonym_name, as2.table_owner,  as2.table_name ";
    private static final String funcQry920_n_np_syn =
            // UnPacked procedures and functions that are synonym accesible and have 0,1 or more args
            "select asyn.owner \"Function Owner\",  asyn.synonym_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\" "
                    + "   , '' \"Overload\" "
                    + "from all_synonyms asyn "
                    + "    , all_objects allobj "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = allobj.owner "
                    + "and asyn.owner||'' IN ('PUBLIC',user) "
                    + "and asyn.table_name = allobj.object_name "
                    + "and allobj.object_type  IN ('PROCEDURE','FUNCTION') "
                    //  + "and exists (select null from all_arguments aseqs "
                    //  + "where allobj.owner = aseqs.owner   "
                    //  + "and allobj.object_name = aseqs.object_name  "
                    //   + "and aseqs.package_name is null) "
                    + "group by asyn.owner , asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , asyn.table_owner , asyn.table_name ";
    private static final String fake_funcQry920_n_np_syn =
            // UnPacked procedures and functions that are synonym accesible and have 0,1 or more args
            "select user \"Function Owner\",  asyn.synonym_name \"Function Name\", 'X' \"Selected\"  "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\"  "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\"  "
                    + "   , '' \"Overload\"  "
                    + "from user_synonyms asyn  "
                    + "         , all_objects allobj  "
                    + "where (user = ? or  user = ?)  "
                    + "and 1=2 "
                    + "and asyn.table_owner = allobj.owner  "
                    + "and asyn.table_name = allobj.object_name  "
                    + "and allobj.object_type  IN ('PROCEDURE','FUNCTION')  "
                    + "group by user , asyn.synonym_name , 'X'  "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym')  "
                    + " , asyn.table_owner , asyn.table_name  ";
    private static final String funcQry901_n_np_syn =
            // UnPacked procedures and functions that are synonym accesible and have 0,1 or more args
            "select /*+ ORDERED */ asyn.owner \"Function Owner\",  asyn.synonym_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    //  + " , aseqs.owner \"Real Owner\", aseqs.object_name \"Real Name\" "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\" "
                    + "   , aseqs.overload \"Overload\" "
                    + "from all_synonyms asyn /* 901 */ "
                    + "    , all_objects allobj "
                    + "   , all_arguments aseqs "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = aseqs.owner (+)   "
                    + "and asyn.owner||'' IN ('PUBLIC',user) "
                    + "and asyn.table_name = aseqs.object_name (+)   "
                    + "and asyn.table_owner = allobj.owner "
                    + "and asyn.table_name = allobj.object_name "
                    + "and allobj.object_type  IN ('PROCEDURE','FUNCTION') "
                    + "and decode(aseqs.package_name,null,'YES','NO') = 'YES' "
                    + "and  2 < aseqs.position (+) "
                    + "group by asyn.owner ,  asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , asyn.table_owner , asyn.table_name "
                    + " ,  aseqs.overload ";
    private static final String fake_funcQry901_n_np_syn =
            // UnPacked procedures and functions that are synonym accesible and have 0,1 or more args
            "select /*+ ORDERED */ asyn.owner \"Function Owner\",  asyn.synonym_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    //  + " , aseqs.owner \"Real Owner\", aseqs.object_name \"Real Name\" "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\" "
                    + "   , aseqs.overload \"Overload\" "
                    + "from all_synonyms asyn /* 901 */ "
                    + "    , all_objects allobj "
                    + "   , all_arguments aseqs "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = aseqs.owner (+)   "
                    + "and asyn.owner||'' IN ('PUBLIC',user) "
                    + "and asyn.table_name = aseqs.object_name (+)   "
                    + "and asyn.table_owner = allobj.owner "
                    + "and asyn.table_name = allobj.object_name "
                    + "and allobj.object_type  IN ('PROCEDURE','FUNCTION') "
                    + "and decode(aseqs.package_name,null,'YES','NO') = 'YES' "
                    + "and  2 < aseqs.position (+) "
                    + "group by asyn.owner ,  asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , asyn.table_owner , asyn.table_name "
                    + " ,  aseqs.overload ";
    private static final String funcQry901_x_p_syn =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select /*+ ORDERED */ asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "   , aseqs2.overload \"Overload\" "
                    + "from all_arguments aseqs2 "
                    + "   , all_synonyms asyn2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and  (aseqs2.owner = ? or  aseqs2.owner LIKE ? ) "
                    + "and aseqs2.position between 0 and 1 "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "and aseqs2.object_name != aseqs2.package_name "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name, aseqs2.overload ";
    private static final String fake_funcQry901_x_p_syn =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select /*+ ORDERED */ asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "   , aseqs2.overload \"Overload\" "
                    + "from all_arguments aseqs2 "
                    + "   , all_synonyms asyn2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and  (aseqs2.owner = ? or  aseqs2.owner LIKE ? ) "
                    + "and aseqs2.position between 0 and 1 "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "and aseqs2.object_name != aseqs2.package_name "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name, aseqs2.overload ";
    private static final String funcQry901_x_p_syn_v3 =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select /*+ ORDERED */ asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "   , aseqs2.overload \"Overload\" "
                    + "from all_synonyms asyn2 "
                    + "   , all_objects allobj2 "
                    + "   , all_arguments aseqs2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and aseqs2.position between 0 and 1 "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "and asyn2.table_owner = allobj2.owner "
                    + "and asyn2.table_name = allobj2.object_name "
                    + "and asyn2.owner||'' IN ('PUBLIC',user) "
                    + "and allobj2.object_type = 'PACKAGE' "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name, aseqs2.overload ";
    private static final String funcQry920_x_p_syn_old =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select /*+ ORDERED */ asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "   , aseqs2.overload \"Overload\" "
                    + "from all_arguments aseqs2 "
                    + "   , all_synonyms asyn2 "
                    + "   , all_objects allobj2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and   (aseqs2.owner = ? or  aseqs2.owner LIKE ? ) "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "and asyn2.table_owner = allobj2.owner "
                    + "and asyn2.table_name = allobj2.object_name "
                    + "and asyn2.owner||'' IN ('PUBLIC',user) "
                    + "and allobj2.object_type = 'PACKAGE' "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name, aseqs2.overload ";
    private static final String funcQry920_x_p_syn =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select /*+ ORDERED */ asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "   , aseqs2.overload \"Overload\" "
                    + "from all_arguments aseqs2 "
                    + "   , all_synonyms asyn2 "
                    + "   , all_objects allobj2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and   (aseqs2.owner = ? or  aseqs2.owner LIKE ? ) "
                    + "and aseqs2.position between 0 and 1 "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "and asyn2.table_owner = allobj2.owner "
                    + "and asyn2.table_name = allobj2.object_name "
                    + "and asyn2.owner||'' IN ('PUBLIC',user) "
                    + "and allobj2.object_type = 'PACKAGE' "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name, aseqs2.overload ";
    private static final String fake_funcQry920_x_p_syn =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select /*+ ORDERED */ user \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\"  "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\"  "
                    + " , user \"Real Owner\", aseqs2.object_name \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\"  "
                    + "   , aseqs2.overload \"Overload\"  "
                    + "from user_arguments aseqs2 "
                    + "   , user_synonyms asyn2 "
                    + "   , user_objects allobj2 "
                    + "where (user = ? or  user= ? ) "
                    + "and   (user = ? or  user = ? ) "
                    + "and aseqs2.position between 0 and 1 "
                    + "and asyn2.table_owner = user "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "and asyn2.table_name = allobj2.object_name "
                    + "and allobj2.object_type = 'PACKAGE' "
                    + "group by user ,  asyn2.synonym_name , 'X' "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , user , aseqs2.object_name "
                    + " , aseqs2.package_name, aseqs2.overload ";
    private static final String db2_fake_funcQry920_x_p_syn =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select /*+ ORDERED */ user \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\"  "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\"  "
                    + " , user \"Real Owner\", aseqs2.object_name \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", '' \"Package\", '' \"Real Package\"  "
                    + "   , ''\"Overload\"  "
                    + "from user_arguments aseqs2 "
                    + "where (user = ? or  user= ? ) "
                    + "and   (user = ? or  user = ? ) "
                    + "and 1 = 2 "
                    + "group by user ,  asyn2.synonym_name , 'X' "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , user , aseqs2.object_name "
                    + " , aseqs2.package_name";
    private static final String funcQry815_x_p_syn =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "   , aseqs2.overload \"Overload\" "
                    + "from all_synonyms asyn2 "
                    + "   , all_objects allobj2 "
                    + "   , all_arguments aseqs2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "and asyn2.table_owner = allobj2.owner "
                    + "and asyn2.table_name = allobj2.object_name "
                    + "and asyn2.owner||'' IN ('PUBLIC',user) "
                    + "and allobj2.object_type = 'PACKAGE' "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name, aseqs2.overload ";
    private static final String fake_funcQry815_x_p_syn =
            // Synonyn based procedures and functions inside packages that have 0 or more args
            "select asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "   , aseqs2.overload \"Overload\" "
                    + "from all_synonyms asyn2 "
                    + "   , all_objects allobj2 "
                    + "   , all_arguments aseqs2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "and asyn2.table_owner = allobj2.owner "
                    + "and asyn2.table_name = allobj2.object_name "
                    + "and asyn2.owner||'' IN ('PUBLIC',user) "
                    + "and allobj2.object_type = 'PACKAGE' "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name, aseqs2.overload ";
    private static final String funcQry920orderBy = "order by 1,10,12,4,2 ";
    private static final String tableQryOrderBy = "order by 1,4 desc,2 ";
    private static final String funcQry4 =
            // Procedures and functions that are directly accessible and have 1 or more args.
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\",o3.package_name \"Package\",o3.package_name \"Real Package\" "
                    + "from all_objects o2 "
                    + "   , all_arguments o3 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    + "and o2.owner = o3.owner (+) "
                    + "and o2.object_name = o3.object_name (+) "
                    + "and o2.object_type = 'PROCEDURE' "
                    + "group by o2.owner, o2.object_name, o3.package_name "
                    + "union all "
                    // Synonym based stand alone Procedures and functions that have 1 or more args and are accessed vis synonyms
                    + "select asyn.owner \"Function Owner\",  asyn.synonym_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs.owner \"Real Owner\", aseqs.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", aseqs.package_name \"Package\", aseqs.package_name \"Real Package\" "
                    + "from all_arguments aseqs "
                    + "   , all_synonyms asyn "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = aseqs.owner "
                    + "and asyn.table_name = aseqs.object_name "
                    + "and aseqs.package_name IS NULL "
                    + "and aseqs.position < 2 "
                    + "group by asyn.owner ,  asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs.owner , aseqs.object_name "
                    + " , aseqs.package_name "
                    + "union all "
                    // Synonym based procedures and functions inside packages that have 0 or more args
                    + "select /*+ ORDERED */ asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "from all_arguments aseqs2 "
                    + "   , all_synonyms asyn2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name "
                    + "order by 1,10,4,2 ";
    private static final String funcQry3 =
            // Procedures and functions that are directly accessible and have 1 or more args.
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\",o2.package_name \"Package\",o2.package_name \"Real Package\" "
                    + "from all_arguments o2 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    + "group by o2.owner, o2.object_name, o2.package_name "
                    + "union all "
                    // Synonym based stand alone Procedures and functions that have 1 or more args and are accessed vis synonyms
                    + "select asyn.owner \"Function Owner\",  asyn.synonym_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs.owner \"Real Owner\", aseqs.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", aseqs.package_name \"Package\", aseqs.package_name \"Real Package\" "
                    + "from all_arguments aseqs "
                    + "   , all_synonyms asyn "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = aseqs.owner "
                    + "and asyn.table_name = aseqs.object_name "
                    + "and aseqs.package_name IS NULL "
                    + "and aseqs.position < 2 "
                    + "group by asyn.owner ,  asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs.owner , aseqs.object_name "
                    + " , aseqs.package_name "
                    + "union all "
                    // Synonym based procedures and functions inside packages that have 0 or more args
                    + "select /*+ ORDERED */ asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "from all_arguments aseqs2 "
                    + "   , all_synonyms asyn2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name "
                    + "order by 1,10,4,2 ";
    private static final String funcQry2 =
            // Procedures and functions that are directly accessible and have 1 or more args.
            "select /* " + Namer.param_product_name + " */ o2.owner \"Owner\", o2.object_name \"Name\", 'X' \"Selected\" "
                    + " , decode(o2.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , o2.owner \"Real Owner\", o2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\",o2.package_name \"Package\",o2.package_name \"Real Package\" "
                    + "from all_arguments o2 "
                    + "where (o2.owner = ? or  o2.owner LIKE ?) "
                    + "group by o2.owner, o2.object_name, o2.package_name "
                    + "union all "
                    // Synonym based stand alone Procedures and functions that have 1 or more args and are accessed vis synonyms
                    + "select asyn.owner \"Function Owner\",  asyn.synonym_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs.owner \"Real Owner\", aseqs.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", aseqs.package_name \"Package\", aseqs.package_name \"Real Package\" "
                    + "from all_arguments aseqs "
                    + "   , all_synonyms asyn "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = aseqs.owner "
                    + "and asyn.table_name = aseqs.object_name "
                    + "and aseqs.package_name IS NULL "
                    + "and aseqs.position < 2 "
                    + "group by asyn.owner ,  asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs.owner , aseqs.object_name "
                    + " , aseqs.package_name "
                    + "union all "
                    // Synonym based procedures and functions inside packages that have 0 or more args
                    + "select /*+ ORDERED */ asyn2.owner \"Function Owner\",  aseqs2.object_name \"Function Name\", 'X' \"Selected\" "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , aseqs2.owner \"Real Owner\", aseqs2.object_name \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", asyn2.synonym_name \"Package\", aseqs2.package_name \"Real Package\" "
                    + "from all_arguments aseqs2 "
                    + "   , all_synonyms asyn2 "
                    + "where (asyn2.owner = ? or  asyn2.table_owner LIKE ? ) "
                    + "and asyn2.table_owner = aseqs2.owner "
                    + "and asyn2.table_name = aseqs2.package_name "
                    + "group by asyn2.owner ,  asyn2.synonym_name , 'X' "
                    + "     , decode(asyn2.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , aseqs2.owner , aseqs2.object_name "
                    + " , aseqs2.package_name "
                    + "order by 1,10,4,2 ";
    private static final String typeArgQrySelect =
            "select /* " + Namer.param_product_name + " */ OWNER, TYPE_NAME OBJECT_NAME, TO_CHAR(null) PACKAGE_NAME, to_char(null) OVERLOAD, ATTR_NAME ARGUMENT_NAME, "
                    + " ATTR_NO POSITION, ATTR_NO SEQUENCE, 0 DATA_LEVEL, attr_type_name DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, length DATA_LENGTH, precision DATA_PRECISION, scale DATA_SCALE, 0 RADIX, CHARACTER_SET_NAME, "
                    + "owner TYPE_OWNER, TYPE_NAME, to_char(null) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_type_attrs "
                    + "WHERE owner = ? "
                    + "AND type_name = ? "
                    + "ORDER BY attr_no";

    private static final String arrayArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ?  OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, ? ARGUMENT_NAME, "
                    + " 1 POSITION, 1 SEQUENCE, 1 DATA_LEVEL, 'ORACLE COLLECTION' DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, nvl(UPPER_BOUND,0) DATA_LENGTH, 0 DATA_PRECISION, 0 DATA_SCALE, 0 RADIX, CHARACTER_SET_NAME, "
                    + "elem_type_owner TYPE_OWNER, elem_type_name TYPE_NAME, to_char(null) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_coll_types "
                    + "WHERE owner = ? "
                    + "AND type_name = ? ";

    private static final String scalerArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ?  OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, ? ARGUMENT_NAME, "
                    + " 1 POSITION, 1 SEQUENCE, 1 DATA_LEVEL, ? DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, 0 DATA_LENGTH, 0 DATA_PRECISION, 0 DATA_SCALE, 0 RADIX, to_char(null) CHARACTER_SET_NAME, "
                    + "'SYS' TYPE_OWNER, ? TYPE_NAME, to_char(null) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM dual ";

    // --- Oracle 23ai nested-type synthesis -------------------------------------------------
    // Oracle 23ai's ALL_ARGUMENTS no longer returns the nested DATA_LEVEL>0 child rows that
    // describe a PACKAGE-level collection's element type (12c returns them; 23ai returns 0).
    // The introspection engine discovers a collection's element by reading the next ALL_ARGUMENTS
    // row at data_level+1; with none, package collections are flagged "not usable" and skipped.
    // These queries synthesise the same ALL_ARGUMENTS-shaped row(s) from the PL/SQL type views
    // (ALL_PLSQL_COLL_TYPES / ALL_PLSQL_TYPE_ATTRS) so the engine consumes them unchanged. They
    // are used ONLY as a fallback when the ALL_ARGUMENTS walk returns nothing, so the 12c path is
    // untouched. Bind order mirrors the proc-coordinate passthrough of arrayArgQrySelect.
    //
    // Scalar element (e.g. TBL_VARRAY_NUMBER_OA = package varray of NUMBER): elem_type_owner IS
    // NULL, so the element is an Oracle scalar — DATA_TYPE = elem_type_name, no TYPE_* (which makes
    // getChildRecord route it to PLSQL_PACK_SCALER_ARRAY, exactly as the 12c child row did).
    // Binds: 1 OWNER, 2 OBJECT_NAME, 3 PACKAGE_NAME, 4 OVERLOAD, 5 SEQUENCE, 6 DATA_LEVEL,
    //        7 type owner, 8 type package_name, 9 collection type_name.
    private static final String plsqlCollScalarArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, to_char(null) ARGUMENT_NAME, "
                    + " 1 POSITION, ? SEQUENCE, ? DATA_LEVEL, elem_type_name DATA_TYPE, 0 DEFAULT_LENGTH, "
                    // DATA_LENGTH is the ELEMENT'S WIDTH, not the collection's bound. This read
                    // upper_bound -- the VARRAY's element COUNT -- which is null for an index-by
                    // table, so nvl made it 0 and a "TABLE OF VARCHAR2(20)" was emitted as
                    // "TABLE OF VARCHAR2(1)": every consumer of DATA_LENGTH treats it as a width
                    // (ExtraTypeSizeWrangler buckets it, the engine writes it into a declaration),
                    // and a package VARRAY's bound comes from getVarrayLengthForPackageVarray
                    // instead, so nothing ever wanted the bound here. Measured against 12c, whose
                    // element row carries length 20 / CHAR_CS for VARCHAR2 and 22,10,2 for NUMBER --
                    // which is what these columns now reproduce, matching the sibling
                    // plsqlIndexbyElemArgQrySelect that had it right all along.
                    + " 'IN/OUT' IN_OUT, nvl(length,0) DATA_LENGTH, precision DATA_PRECISION, scale DATA_SCALE, 0 RADIX, character_set_name CHARACTER_SET_NAME, "
                    + "to_char(null) TYPE_OWNER, to_char(null) TYPE_NAME, to_char(null) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_plsql_coll_types "
                    + "WHERE owner = ? "
                    + "AND package_name = ? "
                    + "AND type_name = ? "
                    + "AND elem_type_owner IS NULL ";

    // ALL_PLSQL_TYPE_ATTRS reports some scalar types with spellings that ALL_ARGUMENTS (and
    // the codegen engine) never use: a 'PL/SQL ' prefix ('PL/SQL ROWID'->'ROWID', 'PL/SQL LONG'
    // ->'LONG', 'PL/SQL BINARY INTEGER'->'BINARY_INTEGER' — also what NATURAL/POSITIVE/SIGNTYPE
    // subtypes report as — and 'PL/SQL PLS INTEGER'->'PLS_INTEGER') plus abbreviated timestamp
    // names ('TIMESTAMP WITH TZ' / 'TIMESTAMP WITH LOCAL TZ'). An unrecognised DATA_TYPE leaves
    // the engine's oracleParamDatatype null and it later NPEs (swallowed -> the record class is
    // silently skipped, surfacing as a missing *Attrs SOAP companion, e.g. AXISTEST's ART record
    // in generic_test8), so map them back. ('PL/SQL BOOLEAN' is left as-is: ALL_ARGUMENTS uses
    // that spelling too.) Used by every arm that selects a.attr_type_name as DATA_TYPE.
    private static final String plsqlAttrDataTypeDecode =
            "decode(a.attr_type_name,'PL/SQL ROWID','ROWID','PL/SQL LONG','LONG','PL/SQL LONG RAW','LONG RAW'"
                    + ",'PL/SQL BINARY INTEGER','BINARY_INTEGER','PL/SQL PLS INTEGER','PLS_INTEGER'"
                    + ",'TIMESTAMP WITH TZ','TIMESTAMP WITH TIME ZONE','TIMESTAMP WITH LOCAL TZ','TIMESTAMP WITH LOCAL TIME ZONE'"
                    + ", a.attr_type_name)";

    // Record element (e.g. TBL_ARRAY_COMMANDS_TYPE_OA = package table of a package RECORD): the
    // element is a PL/SQL RECORD defined in a package (elem_type_owner/elem_type_package/elem_type_name
    // all non-null, typecode 'PL/SQL RECORD'). 12c's ALL_ARGUMENTS returned, for such an argument, an
    // element header row (DATA_LEVEL+1, DATA_TYPE='PL/SQL RECORD', TYPE_OWNER/NAME/SUBNAME = the
    // record's owner/package/name) followed by one field row per record attribute (DATA_LEVEL+2). We
    // reproduce that exact shape: header row from ALL_PLSQL_COLL_TYPES UNION ALL field rows from
    // ALL_PLSQL_TYPE_ATTRS, ORDER BY SEQUENCE so getChildRecord reads the header first (.first()) and
    // the field rows follow. The EXISTS guard restricts this to records (excludes %ROWTYPE and
    // package-record references whose elem_type_package is null, which need separate handling).
    // The header row's ARGUMENT_NAME must be NON-NULL: the record element is an anonymous (result-
    // style) argument, and 12c's ALL_ARGUMENTS walk reads it through a query that applies
    // nvl(argument_name,'_function_result_'||sequence). The downstream dedup keys on procArgName.
    // Historically a null there made PlsqlRecordObject.plsqlEqualsByArgs throw an NPE that the engine's
    // outer catch swallowed, ABORTING the whole duplicate-elimination pass and doubling the generated file
    // count. So we reproduce 12c's synthetic name exactly. (plsqlEqualsByArgs is now also null-safe via
    // java.util.Objects.equals, so a stray null can no longer silently re-break dedup — but keeping this
    // non-null name is still correct and 12c-faithful.)
    // Binds 1-10 = header arm: 1 OWNER, 2 OBJECT_NAME, 3 PACKAGE_NAME, 4 OVERLOAD, 5 SEQUENCE base
    //        (for ARGUMENT_NAME _function_result_<base+1>), 6 SEQUENCE base (header SEQUENCE = base+1),
    //        7 DATA_LEVEL base (header = base+1), 8 coll owner, 9 coll package_name, 10 coll type_name.
    // Binds 11-19 = field arm: 11 OWNER, 12 OBJECT_NAME, 13 PACKAGE_NAME, 14 OVERLOAD, 15 SEQUENCE base
    //        (field SEQUENCE = base+1+attr_no), 16 DATA_LEVEL base (field = base+2), 17 coll owner,
    //        18 coll package_name, 19 coll type_name.
    // The element record's real PACKAGE, which elem_type_package cannot be trusted to give.
    //
    // For a collection whose element is a record declared in the SAME package --
    //     TYPE type_array_commands IS RECORD (...);
    //     TYPE tbl_array_commands_type_ob IS TABLE OF type_array_commands;
    // -- Oracle stores the element's TYPE NAME in ALL_PLSQL_COLL_TYPES.ELEM_TYPE_PACKAGE instead of
    // the package name (elem_type_package = 'TYPE_ARRAY_COMMANDS'), while the record actually lives
    // at owner.<the collection's own package>.TYPE_ARRAY_COMMANDS. A collection whose element is a
    // record from ANOTHER package (…_OA = TABLE OF type_array_commands_oa) reports it correctly.
    // Verified 2026-08-01 on 12c/19c/21c; **corrected by Oracle in 23ai**, which reports the real
    // package on the same schema.
    //
    // The consequence used to be silent: the EXISTS guard below could never match, no element header
    // row was synthesised, no class was generated for the element record, and the SOAP layer emitted
    // a signature referring to a class that does not exist -- generic_teste failed to compile on
    // 19c/21c with "cannot find symbol: OracleArraysv8TblVarrayCommandsTypeObAttrs" (neighbouring
    // params degraded to Object[]). It only bites where BOTH the quirk and a truncated ALL_ARGUMENTS
    // meet: 12c has the quirk but returns the full nested walk so synthesis never runs, and 23ai
    // needs synthesis but has a correct dictionary.
    //
    // So: keep elem_type_package when it really names a package holding the type (every
    // pre-existing case, so their SQL is unchanged), else fall back to the collection's own package.
    private static final String collElemRecordPackage =
            "(case when exists (select 1 from all_plsql_types t2 where t2.owner = c.elem_type_owner "
                    + "  and t2.package_name = c.elem_type_package and t2.type_name = c.elem_type_name "
                    + "  and t2.typecode = 'PL/SQL RECORD') then c.elem_type_package else c.package_name end)";

    private static final String plsqlCollRecordArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, '_function_result_' || to_char(? + 1) ARGUMENT_NAME, "
                    + " 1 POSITION, ? + 1 SEQUENCE, ? + 1 DATA_LEVEL, 'PL/SQL RECORD' DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, 0 DATA_LENGTH, to_number(null) DATA_PRECISION, to_number(null) DATA_SCALE, to_number(null) RADIX, to_char(null) CHARACTER_SET_NAME, "
                    + "c.elem_type_owner TYPE_OWNER, " + collElemRecordPackage + " TYPE_NAME, c.elem_type_name TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_plsql_coll_types c "
                    + "WHERE c.owner = ? AND c.package_name = ? AND c.type_name = ? "
                    + "AND c.elem_type_owner IS NOT NULL AND c.elem_type_name IS NOT NULL "
                    + "AND EXISTS (SELECT 1 FROM all_plsql_types t WHERE t.owner = c.elem_type_owner "
                    + "  AND t.package_name IN (c.elem_type_package, c.package_name) AND t.type_name = c.elem_type_name "
                    + "  AND t.typecode = 'PL/SQL RECORD') "
                    + "UNION ALL "
                    + "select ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, a.attr_name ARGUMENT_NAME, "
                    + " a.attr_no POSITION, ? + 1 + a.attr_no SEQUENCE, ? + 2 DATA_LEVEL, " + plsqlAttrDataTypeDecode + " DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, nvl(a.length,0) DATA_LENGTH, a.precision DATA_PRECISION, a.scale DATA_SCALE, to_number(null) RADIX, a.character_set_name CHARACTER_SET_NAME, "
                    + "a.attr_type_owner TYPE_OWNER, a.attr_type_package TYPE_NAME, decode(a.attr_type_owner,null,null,a.attr_type_name) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_plsql_type_attrs a, all_plsql_coll_types c "
                    + "WHERE c.owner = ? AND c.package_name = ? AND c.type_name = ? "
                    + "AND a.owner = c.elem_type_owner AND a.package_name = " + collElemRecordPackage + " AND a.type_name = c.elem_type_name "
                    + "ORDER BY 7 ";

    // Object element (e.g. ORACLE_ARRAYS.TBL_ARRAY_COMMANDS_TYPE_OB / ORACLE_ARRAYS_EXTRA.
    // TBL_VARRAY_COMMANDS_TYPE_OAX = package collection of a SCHEMA OBJECT type): the element type
    // lives at schema level (elem_type_owner non-null, elem_type_package NULL, an ALL_TYPES OBJECT),
    // not in a package. 12c's ALL_ARGUMENTS returned a SINGLE element-header row for it
    // (DATA_TYPE='OBJECT', TYPE_OWNER=elem_type_owner, TYPE_NAME=elem_type_name, TYPE_SUBNAME=null);
    // the object's attributes are NOT child rows here -- getChildRecord's object arm fetches them
    // separately from ALL_TYPE_ATTRS via getAttrArguments. So we synthesise just that one OBJECT
    // header row. Binds 1-10, same layout as the record header arm (see plsqlCollRecordArgQrySelect):
    // 1 OWNER, 2 OBJECT_NAME, 3 PACKAGE_NAME, 4 OVERLOAD, 5 SEQUENCE base (ARGUMENT_NAME), 6 SEQUENCE
    // base, 7 DATA_LEVEL base, 8 coll owner, 9 coll package_name, 10 coll type_name.
    private static final String plsqlCollObjectArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, '_function_result_' || to_char(? + 1) ARGUMENT_NAME, "
                    + " 1 POSITION, ? + 1 SEQUENCE, ? + 1 DATA_LEVEL, 'OBJECT' DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, 0 DATA_LENGTH, to_number(null) DATA_PRECISION, to_number(null) DATA_SCALE, to_number(null) RADIX, to_char(null) CHARACTER_SET_NAME, "
                    + "c.elem_type_owner TYPE_OWNER, c.elem_type_name TYPE_NAME, to_char(null) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_plsql_coll_types c "
                    + "WHERE c.owner = ? AND c.package_name = ? AND c.type_name = ? "
                    + "AND c.elem_type_owner IS NOT NULL AND c.elem_type_package IS NULL "
                    + "AND EXISTS (SELECT 1 FROM all_types t WHERE t.owner = c.elem_type_owner "
                    + "  AND t.type_name = c.elem_type_name AND t.typecode = 'OBJECT') ";

    // %ROWTYPE element (e.g. ORACLE_ARRAYS.TBL_ARRAY_COMMANDS_ROWTYPE_OA = package collection of
    // ARRAY_COMMANDS%ROWTYPE): the element is a table's row type. 12c's ALL_ARGUMENTS returned a
    // single element-header row with DATA_TYPE='PL/SQL RECORD' and ALL of TYPE_OWNER/NAME/SUBNAME
    // NULL -- the signal that makes getChildRecord parse the package source (getRowTypeofType) to
    // resolve the %ROWTYPE to its base table. So we synthesise exactly that one all-null header row.
    // Binds 1-10, same layout as the record/object header arms.
    private static final String plsqlCollRowtypeArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, '_function_result_' || to_char(? + 1) ARGUMENT_NAME, "
                    + " 1 POSITION, ? + 1 SEQUENCE, ? + 1 DATA_LEVEL, 'PL/SQL RECORD' DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, 0 DATA_LENGTH, to_number(null) DATA_PRECISION, to_number(null) DATA_SCALE, to_number(null) RADIX, to_char(null) CHARACTER_SET_NAME, "
                    + "to_char(null) TYPE_OWNER, to_char(null) TYPE_NAME, to_char(null) TYPE_SUBNAME, to_char(null) TYPE_LINK, "
                    // The base table, which the view spells as the literal text "TAB%ROWTYPE" in
                    // ELEM_TYPE_NAME (the WHERE clause below already keys on that shape). Carried as
                    // two EXTRA columns rather than in TYPE_OWNER/TYPE_NAME, because those three
                    // staying null is what selects getChildRecord's %ROWTYPE branch -- filling them
                    // would take the other branch and rename the generated class. Downstream reads by
                    // column name, so appending is inert for every existing reader.
                    + "c.elem_type_owner ROWTYPE_TABLE_OWNER, "
                    + "replace(c.elem_type_name, '%ROWTYPE') ROWTYPE_TABLE_NAME "
                    + "FROM all_plsql_coll_types c "
                    + "WHERE c.owner = ? AND c.package_name = ? AND c.type_name = ? "
                    + "AND c.elem_type_name LIKE '%\\%ROWTYPE' ESCAPE '\\' ";

    // Direct package RECORD type (e.g. a PL/SQL RECORD used as a procedure parameter, like
    // RECORD_TEST2_8I.RECORDTYPE): 23ai no longer returns the DATA_LEVEL>0 field child rows that the
    // "Supporting class identified" loop walks to discover the record's fields, so the record class
    // comes out empty (no setters). Synthesise one field row per attribute from ALL_PLSQL_TYPE_ATTRS,
    // shaped like the ATTR_ARG_QUERY field rows the record-class engine consumes. Binds: 1 OWNER,
    // 2 OBJECT_NAME, 3 PACKAGE_NAME, 4 OVERLOAD, 5 DATA_LEVEL, 6 record owner, 7 record package_name,
    // 8 record type_name.
    // A field that is itself a PL/SQL RECORD (Item 7, nested records) must report DATA_TYPE='PL/SQL
    // RECORD' so the engine builds a typed nested field + resolves it to that record's generated class
    // (12c's ALL_ARGUMENTS spelled it that way; the flat plsqlAttrDataTypeDecode otherwise yields the
    // record type's NAME, which the engine can't classify). TYPE_OWNER/NAME/SUBNAME already identify
    // the nested record. Only records are promoted here; object/collection-typed fields keep their
    // decoded name and are still gated out upstream (a deeper sub-case).
    private static final String plsqlRecordFieldArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, a.attr_name ARGUMENT_NAME, "
                    + " a.attr_no POSITION, a.attr_no SEQUENCE, ? DATA_LEVEL, "
                    + "case when a.attr_type_owner is not null and exists (select 1 from all_plsql_types t "
                    + "  where t.owner = a.attr_type_owner and t.package_name = a.attr_type_package "
                    + "  and t.type_name = a.attr_type_name and t.typecode = 'PL/SQL RECORD') "
                    + "then 'PL/SQL RECORD' else " + plsqlAttrDataTypeDecode + " end DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, nvl(a.length,0) DATA_LENGTH, a.precision DATA_PRECISION, a.scale DATA_SCALE, to_number(null) RADIX, a.character_set_name CHARACTER_SET_NAME, "
                    + "a.attr_type_owner TYPE_OWNER, a.attr_type_package TYPE_NAME, decode(a.attr_type_owner,null,null,a.attr_type_name) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_plsql_type_attrs a "
                    + "WHERE a.owner = ? AND a.package_name = ? AND a.type_name = ? "
                    + "ORDER BY a.attr_no ";

    // A package RECORD whose fields are COLLECTIONS (e.g. GENERIC_TESTD.IBA_TEST.L_REC, every field an
    // index-by table). The flat one-row-per-attribute synthesis above cannot express these, because
    // 12c reports each such field as TWO rows and the engine needs both: the field itself at
    // DATA_LEVEL+1 typed 'PL/SQL TABLE' and carrying the collection's identity in
    // TYPE_OWNER/TYPE_NAME/TYPE_SUBNAME, then its ELEMENT at DATA_LEVEL+2 with all TYPE_* null.
    // Measured on ORCL12 for IBA_TEST.SAMPLE_REC, which is exactly the shape reproduced here:
    //
    //    SEQ LVL POS ARG           DATA_TYPE     TYPE_NAME TYPE_OWNER    TYPE_SUBNAME
    //      2   1   1 A_VC2_ARRAY   PL/SQL TABLE  IBA_TEST  GENERIC_TESTD VARCHAR2_IBA
    //      3   2   1 (null)        VARCHAR2      (null)    (null)        (null)
    //
    // Kept SEPARATE from plsqlRecordFieldArgQrySelect rather than folded into it: that query serves
    // every record that already synthesises correctly, and this one is only reached after the flat
    // form has been rejected, so a record that works today cannot change path. SEQUENCE is a
    // row_number over the union rather than arithmetic on attr_no, because a record may mix scalar
    // fields (one row) with collection fields (two) and 12c numbers them contiguously.
    // Binds: 1 OWNER, 2 OBJECT_NAME, 3 PACKAGE_NAME, 4 OVERLOAD, 5 DATA_LEVEL, 6/7/8 record
    // owner/package/type for the field rows, 9/10/11 the same for the element rows.
    private static final String plsqlRecordCollectionFieldArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, "
                    + "x.ARGUMENT_NAME, x.POSITION, "
                    + "row_number() over (order by x.ATTR_NO, x.LVL) SEQUENCE, ? + x.LVL DATA_LEVEL, "
                    + "x.DATA_TYPE, 0 DEFAULT_LENGTH, 'IN/OUT' IN_OUT, x.DATA_LENGTH, x.DATA_PRECISION, "
                    + "x.DATA_SCALE, to_number(null) RADIX, x.CHARACTER_SET_NAME, "
                    + "x.TYPE_OWNER, x.TYPE_NAME, x.TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM ( "
                    // the field row -- 'PL/SQL TABLE' where the attribute's type is a collection,
                    // otherwise the ordinary scalar/record decode so a mixed record still works
                    + "  select a.attr_no ATTR_NO, 0 LVL, a.attr_name ARGUMENT_NAME, a.attr_no POSITION, "
                    + "    case when cf.type_name is not null then 'PL/SQL TABLE' "
                    + "      when a.attr_type_owner is not null and exists (select 1 from all_plsql_types t "
                    + "        where t.owner = a.attr_type_owner and t.package_name = a.attr_type_package "
                    + "        and t.type_name = a.attr_type_name and t.typecode = 'PL/SQL RECORD') "
                    + "      then 'PL/SQL RECORD' else " + plsqlAttrDataTypeDecode + " end DATA_TYPE, "
                    + "    nvl(a.length,0) DATA_LENGTH, a.precision DATA_PRECISION, a.scale DATA_SCALE, "
                    + "    a.character_set_name CHARACTER_SET_NAME, "
                    + "    a.attr_type_owner TYPE_OWNER, a.attr_type_package TYPE_NAME, "
                    + "    decode(a.attr_type_owner,null,null,a.attr_type_name) TYPE_SUBNAME "
                    + "  from all_plsql_type_attrs a "
                    + "    left join all_plsql_coll_types cf on (cf.owner = a.attr_type_owner "
                    + "      and cf.package_name = a.attr_type_package and cf.type_name = a.attr_type_name) "
                    + "  where a.owner = ? and a.package_name = ? and a.type_name = ? "
                    + "  union all "
                    // the element row, one per collection field, all TYPE_* null exactly as 12c leaves them
                    + "  select a.attr_no, 1, to_char(null), 1, ce.elem_type_name, "
                    + "    nvl(ce.length,0), ce.precision, ce.scale, ce.character_set_name, "
                    + "    to_char(null), to_char(null), to_char(null) "
                    + "  from all_plsql_type_attrs a "
                    + "    join all_plsql_coll_types ce on (ce.owner = a.attr_type_owner "
                    + "      and ce.package_name = a.attr_type_package and ce.type_name = a.attr_type_name) "
                    + "  where a.owner = ? and a.package_name = ? and a.type_name = ? "
                    + "    and ce.elem_type_owner is null "
                    + ") x ORDER BY x.ATTR_NO, x.LVL ";

    // Table/view %ROWTYPE record (e.g. a parameter of LEGACY_DATATYPES%ROWTYPE): the record's fields
    // are the table's columns. 23ai dropped the DATA_LEVEL>0 child rows the loop walked for them, so
    // synthesise one field row per column from ALL_TAB_COLUMNS. The query returns nothing for a name
    // that is not a real table/view, so it is a safe fallback after the package-record arm (genuine
    // schema OBJECT types still resolve via ALL_TYPE_ATTRS, which 23ai does not break). Binds:
    // 1 OWNER, 2 OBJECT_NAME, 3 PACKAGE_NAME, 4 OVERLOAD, 5 DATA_LEVEL, 6 table owner, 7 table name.
    private static final String plsqlRowtypeFieldArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, c.column_name ARGUMENT_NAME, "
                    + " c.column_id POSITION, c.column_id SEQUENCE, ? DATA_LEVEL, c.data_type DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, nvl(c.data_length,0) DATA_LENGTH, c.data_precision DATA_PRECISION, c.data_scale DATA_SCALE, to_number(null) RADIX, c.character_set_name CHARACTER_SET_NAME, "
                    + "to_char(null) TYPE_OWNER, to_char(null) TYPE_NAME, to_char(null) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_tab_columns c "
                    + "WHERE c.owner = ? AND c.table_name = ? "
                    + "ORDER BY c.column_id ";

    // Index-by (associative array) of a SCALAR element (e.g. PLSQL_INDEXBY_TABLES.INDEXBYTABNUMBER =
    // TABLE OF NUMBER INDEX BY BINARY_INTEGER): 23ai no longer returns the DATA_LEVEL>0 element child
    // row, so CallableStatementParameterEngine.getAttrArguments (the ATTR_ARG_QUERY walk) finds no
    // element and the WS conversion (createIndexByTableFrom<Type>Array helpers) is mis-emitted.
    // Synthesise the element row from ALL_PLSQL_COLL_TYPES, carrying the scalar element's DATA_TYPE
    // and its precision/scale/length, shaped like the 12c child row (TYPE_* all null for a scalar
    // element). Binds: 1 OWNER, 2 OBJECT_NAME, 3 PACKAGE_NAME, 4 OVERLOAD, 5 SEQUENCE base (element =
    // base+1), 6 DATA_LEVEL base (element = base+1), 7 coll owner, 8 coll package_name, 9 coll type_name.
    private static final String plsqlIndexbyElemArgQrySelect =
            "select /* " + Namer.param_product_name + " */ ? OWNER, ? OBJECT_NAME, ? PACKAGE_NAME, ? OVERLOAD, to_char(null) ARGUMENT_NAME, "
                    + " 1 POSITION, ? + 1 SEQUENCE, ? + 1 DATA_LEVEL, c.elem_type_name DATA_TYPE, 0 DEFAULT_LENGTH, "
                    + " 'IN/OUT' IN_OUT, nvl(c.length,0) DATA_LENGTH, c.precision DATA_PRECISION, c.scale DATA_SCALE, to_number(null) RADIX, c.character_set_name CHARACTER_SET_NAME, "
                    + "to_char(null) TYPE_OWNER, to_char(null) TYPE_NAME, to_char(null) TYPE_SUBNAME, to_char(null) TYPE_LINK "
                    + "FROM all_plsql_coll_types c "
                    + "WHERE c.owner = ? AND c.package_name = ? AND c.type_name = ? "
                    + "AND c.elem_type_owner IS NULL ";

    // 05-02-15
    //private static final String argQrySelect=
    //   "select /* " + Namer.param_product_name + " */ OWNER, OBJECT_NAME, PACKAGE_NAME, /*OBJECT_ID,*/ OVERLOAD, nvl(argument_name,'_function_result') ARGUMENT_NAME, "
    //  +"nvl(position,-1) POSITION, SEQUENCE, DATA_LEVEL, DATA_TYPE, DEFAULT_LENGTH, "
    //  +"IN_OUT, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, RADIX, CHARACTER_SET_NAME, "
    //  +"TYPE_OWNER, TYPE_NAME, TYPE_SUBNAME, TYPE_LINK ";

    private static final String argQrySelect1011 =
            "select /* " + Namer.param_product_name + " */ OWNER, OBJECT_NAME, PACKAGE_NAME, /*OBJECT_ID,*/ OVERLOAD, nvl(argument_name,'_function_result'||decode(sequence,0,null,'_'||sequence)) ARGUMENT_NAME, "
                    + "nvl(position,-1) POSITION, SEQUENCE, DATA_LEVEL, DECODE(TYPE_NAME,'XMLTYPE','XMLTYPE', DATA_TYPE) DATA_TYPE, DEFAULT_LENGTH, "
                    + "IN_OUT, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, RADIX, CHARACTER_SET_NAME, "
                    + "TYPE_OWNER, TYPE_NAME, TYPE_SUBNAME, TYPE_LINK ";

    private static final String argQrySelect =
            "select /* " + Namer.param_product_name + " */ OWNER, OBJECT_NAME, PACKAGE_NAME, /*OBJECT_ID,*/ OVERLOAD, nvl(argument_name,'_function_result'||decode(sequence,0,null,'_'||sequence)) ARGUMENT_NAME, "
                    + "nvl(position,-1) POSITION, SEQUENCE, DATA_LEVEL, DECODE(TYPE_NAME,'XMLTYPE','XMLTYPE', 'SDO_GEOMETRY','SDO_GEOMETRY', DATA_TYPE) DATA_TYPE, DEFAULT_LENGTH, "
                    + "IN_OUT, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, RADIX, CHARACTER_SET_NAME, "
                    + "TYPE_OWNER, TYPE_NAME, TYPE_SUBNAME, TYPE_LINK ";

    private static final String db2ArgQrySelect =
            "select /* " + Namer.param_product_name + " */ OWNER, OBJECT_NAME, PACKAGE_NAME, /*OBJECT_ID,*/ '' OVERLOAD, nvl(argument_name,'_function_result'||decode(sequence,0,null,'_'||sequence)) ARGUMENT_NAME, "
                    + "sequence /*nvl(position,-1)*/ POSITION, SEQUENCE, 0 DATA_LEVEL,  /*DECODE(TYPE_NAME,'XMLTYPE','XMLTYPE', 'SDO_GEOMETRY','SDO_GEOMETRY', DATA_TYPE */ decode(DATA_TYPE,'CURSOR','REF CURSOR','TIMESTAMP','DATE',DATA_TYPE) DATA_TYPE, "
                    + "IN_OUT, DATA_LENGTH, 38 DATA_PRECISION, DATA_SCALE, to_number(null) RADIX, decode(data_type,'CHAR','CHAR_CS','NCHAR','NCHAR_CS',null) CHARACTER_SET_NAME, "
                    + "'' TYPE_OWNER, '' TYPE_NAME, '' TYPE_SUBNAME, '' TYPE_LINK ";

    private static final String argQrySelectGtr815 = ", PLS_TYPE ";

    private static final String argQrySelectGtr901 = ", OBJECT_ID ";

    private static final String argQrySelectGtr1020 = ", SUBPROGRAM_ID ";

    private static final String argQryOrderBy =
            "and   data_level = 0 "
                    + "order by sequence";

    private static final String db2ArgQryOrderBy =
            "order by sequence";

    //private static final String argRecQryOrderBy =
    //"and  data_type IN ('PL/SQL RECORD','OBJECT','TABLE','VARRAY') "
    // +"order by sequence";

    private static final String argRecQryOrderBy =
            "and  data_type IN ('PL/SQL RECORD','PL/SQL TABLE','OBJECT','TABLE','VARRAY','REF CURSOR') "
                    + "order by sequence";

    private static final String db2ArgRecQryOrderBy =
            "and  data_type IN ('PL/SQL RECORD','PL/SQL TABLE','OBJECT','TABLE','VARRAY','REF CURSOR') "
                    + "order by sequence";

    private static final String collectionQueryLte102 =
            "select  owner   "
                    + ", type_name  "
                    + ", coll_type  "
                    + ", upper_bound  "
                    + ", elem_type_mod   "
                    + ", decode(elem_type_name,'XMLTYPE',null, elem_type_owner) elem_type_owner "
                    + ", elem_type_name   "
                    + ", length    "
                    + ", precision   "
                    + ", scale   "
                    + ", character_set_name  "
                    + ", elem_storage   "
                    + ", nulls_stored     "
                    + "from all_coll_types "
                    + "where owner = ? "
                    + "and   type_name = ?";

    private static final String collectionQuery =
            "select * "
                    + "from all_coll_types "
                    + "where owner = ? "
                    + "and   type_name = ?";

    // The attr walk used to end with a correlated NVL(MIN(a2.sequence)) subquery
    // (self-join of ALL_ARGUMENTS) that found the next sibling's sequence. On
    // Oracle 19c that self-join loses its bind variables -- every execution dies
    // with ORA-01008 (wrapped in ORA-12801 when a parallel slave hits it), serial
    // or parallel, hints or not. So the boundary is now computed FIRST with the
    // stand-alone query below (single view access: works on 19c) and bound into
    // the walk query as a plain value.
    private static final String argRecQryAttrOrderBy =
            "and  a.sequence > ? "
                    + "and  a.sequence < ? "
                    + "order by sequence";

    private static final String db2ArgRecQryAttrOrderBy =
            "and  a.sequence > ? "
                    + "and  a.sequence < ? "
                    + "order by sequence";

    /**
     * Query for the attr-walk upper boundary: the first sequence after {@code ?}
     * (at data_level &lt;= {@code ?}) that is NOT part of the record/collection
     * being walked, i.e. the next sibling argument. 999999 when the walked
     * argument is the last one. Replaces the correlated subquery that
     * {@link #argRecQryAttrOrderBy} carried before the Oracle 19c ORA-01008 bug
     * (see that field's comment). Binds: owner, object_name, [package_name,]
     * [overload,] data_level, sequence.
     */
    public static String getAttrBoundaryQry(boolean hasPackage, boolean hasOverload) {
        return "select /* " + Namer.param_product_name + " */ nvl(min(sequence), 999999) BOUNDARY_SEQ "
                + "from all_arguments "
                + "where owner = ? "
                + "and   object_name = ? "
                + (hasPackage ? "and   package_name = ? " : "and   package_name is null ")
                + (hasOverload ? "and   overload = ? " : "and   nvl(overload,1) < 2 ")
                + "and   data_level <= ? "
                + "and   sequence > ?";
    }

    private static final String argQry920 =
            "from all_arguments a "
                    + "where owner = ? "
                    + "and   object_name  = ? "
                    + "and   package_name is null "
                    + "and   data_type is not null ";

    private static final String argQry901 =
            "from all_arguments a "
                    + "where owner = ? "
                    + "and   object_name  = ? "
                    + "and   package_name = a.object_name "
                    + "and   data_type is not null ";

    private static final String argQryPack =
            "from all_arguments a "
                    + "where owner = ? "
                    + "and   object_name  = ? "
                    + "and   package_name = ? "
                    + "and   data_type is not null "
                    + "and   nvl(overload,1) < 2 ";

    private static final String db2ArgQryPack =
            "from all_arguments a "
                    + "where owner = ? "
                    + "and   object_name  = ? "
                    + "and   package_name = ? "
                    + "and   data_type is not null ";

    private static final String argQryPackOverload =
            "from all_arguments a "
                    + "where owner = ? "
                    + "and   object_name  = ? "
                    + "and   package_name = ? "
                    + "and   data_type is not null "
                    + "and   overload = ? ";

    private static final String generateTablesColList
            = "DATA_TYPE,COLUMN_NAME,NULLABLE,TABLE_NAME,DATA_LENGTH,DATA_PRECISION,DATA_SCALE";

    // When VECTOR_INFO is available (Oracle 23ai+), rewrite a VECTOR column's DATA_TYPE to a
    // format-specific synthetic token so the rest of the generator (which keys off DATA_TYPE) can
    // distinguish the storage format. VECTOR_INFO looks like VECTOR(<dim>,<format>,<storage>), e.g.
    // VECTOR(16,BINARY,DENSE) / VECTOR(4,FLOAT32,SPARSE). Binary -> VECTOR_BINARY (byte[]); sparse ->
    // VECTOR_SPARSE (skipped, unreadable by ojdbc11 23.7); dense/flexible keep VECTOR (double[]).
    private static final String vectorFormatDataType =
            "case when data_type='VECTOR' and instr(upper(vector_info),'BINARY')>0 then 'VECTOR_BINARY' "
                    + "when data_type='VECTOR' and instr(upper(vector_info),'SPARSE')>0 then 'VECTOR_SPARSE' "
                    + "else data_type end DATA_TYPE";

    private static final String numberBracket = "decode(data_precision, null, '', '('||data_precision||decode(data_scale,null,'',0,'',','||data_scale)||')') FMT ";

    private static final String db2NumberBracket = "decode(data_length, null, '', '('||data_length||decode(data_scale,null,'',0,'',','||data_scale)||')') FMT ";

    /**
     * Query to get Oracle Version info
     */
    private static final String versionQuery =
            "SELECT  /* " + Namer.param_product_name + " */ * FROM V$VERSION";


    private static final String versionQueryDb2 =
            "SELECT service_level FROM SYSIBMADM.env_inst_info";

    /**
     * Query to get julian date
     */
    private static final String dateQuery =
            "SELECT to_number(to_char(sysdate,'J')) /* " + Namer.param_product_name + " */ FROM DUAL";

    /**
     * Query to get tables
     */
    private static final String tableQry =
            "select /* " + Namer.param_product_name + " */ asyn.owner \"Table Owner\",  asyn.synonym_name \"Table Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \" "
                    + "from all_synonyms asyn "
                    + "    , all_objects allobj "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = allobj.owner "
                    + "and asyn.owner||'' IN ('PUBLIC',user) "
                    + "and asyn.table_name = allobj.object_name "
                    + "and allobj.object_type  IN ('TABLE','VIEW') "
                    + "group by asyn.owner ,  asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , asyn.table_owner , asyn.table_name, object_type "
                    + "union all "
                    + "select allobj.owner \"Table Owner\",  allobj.object_name \"Table Name\", 'X' \"Selected\" "
                    + "     , decode(allobj.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , allobj.owner \"Real Owner\", allobj.object_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \" "
                    + "from all_objects allobj "
                    + "where (allobj.owner = ? or  allobj.owner LIKE ?) "
                    + "and allobj.object_type  IN ('TABLE','VIEW') "
                    + "group by allobj.owner ,  allobj.object_name, 'X' "
                    + "     , decode(allobj.owner,user,'User Object' ,'Other User''s Object') "
                    + " , allobj.owner , allobj.object_name  "
                    + " , object_type ";


    private static final String db2TableQry =
            "select /* " + Namer.param_product_name + " */ asyn.owner \"Table Owner\",  asyn.synonym_name \"Table Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \" "
                    + "from all_synonyms asyn "
                    + "    , all_objects allobj "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = allobj.owner "
                    + "and asyn.owner||'' IN ('PUBLIC',user) "
                    + "and asyn.table_name = allobj.object_name "
                    + "and allobj.object_type  IN ('TABLE','VIEW') "
                    + "group by asyn.owner ,  asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , asyn.table_owner , asyn.table_name, object_type "
                    + "union all "
                    + "select allobj.owner \"Table Owner\",  allobj.object_name \"Table Name\", 'X' \"Selected\" "
                    + "     , decode(allobj.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , allobj.owner \"Real Owner\", allobj.object_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \" "
                    + "from all_objects allobj "
                    + "where (allobj.owner = ? or  allobj.owner LIKE ?) "
                    + "and allobj.object_type  IN ('TABLE','VIEW') "
                    + "group by allobj.owner ,  allobj.object_name, 'X' "
                    + "     , decode(allobj.owner,user,'User Object' ,'Other User''s Object') "
                    + " , allobj.owner , allobj.object_name  "
                    + " , object_type ";

    private static final String OLDtableQry =
            "select /* " + Namer.param_product_name + " */ asyn.owner \"Table Owner\",  asyn.synonym_name \"Table Name\", 'X' \"Selected\" "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\" "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \" "
                    + "from all_synonyms asyn "
                    + "    , all_objects allobj "
                    + "where (asyn.owner = ? or  asyn.table_owner LIKE ?) "
                    + "and asyn.table_owner = allobj.owner "
                    + "and asyn.owner||'' IN ('PUBLIC',user) "
                    + "and asyn.table_name = allobj.object_name "
                    + "and allobj.object_type  IN ('TABLE','VIEW') "
                    + "group by asyn.owner ,  asyn.synonym_name , 'X' "
                    + "     , decode(asyn.owner,'PUBLIC','Public Synonym','Private Synonym') "
                    + " , asyn.table_owner , asyn.table_name, object_type "
                    + "union all "
                    + "select allobj.owner \"Table Owner\",  allobj.object_name \"Table Name\", 'X' \"Selected\" "
                    + "     , decode(allobj.owner,user,'User Object' ,'Other User''s Object') \"Accessed Via\" "
                    + " , allobj.owner \"Real Owner\", allobj.object_name  \"Real Name\" "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \" "
                    + "from all_objects allobj "
                    + "where (allobj.owner = ? or  allobj.owner LIKE ?) "
                    + "and allobj.object_type  IN ('TABLE','VIEW') "
                    + "group by allobj.owner ,  allobj.object_name, 'X' "
                    + "     , decode(allobj.owner,user,'User Object' ,'Other User''s Object') "
                    + " , allobj.owner , allobj.object_name  "
                    + " , object_type ";

    /**
     * Query to get tables
     */
    private static final String fakeTableQry =
            "select /*+ ORDERED +*/ user \"Table Owner\",  asyn.synonym_name \"Table Name\", 'X' \"Selected\"  "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\"  "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \"  "
                    + "from user_synonyms asyn  "
                    + "   , all_objects allobj  "
                    + "where (user = ? or user = ?  )  "
                    + "and asyn.table_owner =  allobj.owner "
                    + "and asyn.table_name = allobj.object_name  "
                    + "and allobj.object_type  IN ('TABLE','VIEW')  "
                    + "group by user ,  asyn.synonym_name , 'X'  "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym')  "
                    + " , asyn.table_owner , asyn.table_name, object_type  "
                    + "union all  "
                    + "select user \"Table Owner\",  allobj.object_name \"Table Name\", 'X' \"Selected\"  "
                    + "     , decode(user,user,'User Object' ,'Other User''s Object') \"Accessed Via\"  "
                    + " , user \"Real Owner\", allobj.object_name  \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \"  "
                    + "from user_objects allobj  "
                    + "where (user = ? or  user = ?)  "
                    + "and allobj.object_type  IN ('TABLE','VIEW')  "
                    + "group by user ,  allobj.object_name, 'X'  "
                    + "     , decode(user,user,'User Object' ,'Other User''s Object')  "
                    + " , user , allobj.object_name   "
                    + " , object_type  ";


    private static final String db2FakeTableQry =
            "select /*+ ORDERED +*/ user \"Table Owner\",  asyn.synonym_name \"Table Name\", 'X' \"Selected\"  "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym') \"Accessed Via\"  "
                    + " , asyn.table_owner \"Real Owner\", asyn.table_name  \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \"  "
                    + "from user_synonyms asyn  "
                    + "   , all_objects allobj  "
                    + "where (owner = ? or owner = ?  )  "
                    + "and asyn.table_owner =  allobj.owner "
                    + "and asyn.table_name = allobj.object_name  "
                    + "and allobj.object_type  IN ('TABLE','VIEW')  "
                    + "group by user ,  asyn.synonym_name , 'X'  "
                    + "     , decode(user,'PUBLIC','Public Synonym','Private Synonym')  "
                    + " , asyn.table_owner , asyn.table_name, object_type  "
                    + "union all  "
                    + "select user \"Table Owner\",  allobj2.object_name \"Table Name\", 'X' \"Selected\"  "
                    + "     , decode(user,user,'User Object' ,'Other User''s Object') \"Accessed Via\"  "
                    + " , user \"Real Owner\", allobj2.object_name  \"Real Name\"  "
                    + " , 'X' \"Oracle Name\", 'X' \"Java Name\", 'X' \"Fixed Java Name\", object_type \"Type \"  "
                    + "from all_objects allobj2  "
                    + "where (owner = ? or  owner = ?)  "
                    + "and allobj2.object_type  IN ('TABLE','VIEW')  "
                    + "group by user ,  allobj2.object_name, 'X'  "
                    + "     , decode(user,user,'User Object' ,'Other User''s Object')  "
                    + " , user , allobj2.object_name   "
                    + " , object_type  ";


    private static final String dirQuery =
            "SELECT * /* " + Namer.param_prod_name + " */ "
                    + "FROM all_directories a "
                    + "ORDER BY DECODE (a.directory_name, 'MEDIA_DIR',2,'LOG_FILE_DIR',2,'DATA_FILE_DIR',2,'DM_PMML_DIR',2,1), a.directory_name ";

    /**
     * Chosen Oracle Version
     */
    public String theOracleVersion = null;

    public SqlStatementDictionary(String theOracleVersion) {
        // Default oracle version to latest
        this.theOracleVersion = oracleVersions[oracleVersions.length - 1];
        for (int i = 0; i < oracleVersions.length; i++) {
            if (theOracleVersion.equalsIgnoreCase(oracleVersions[i])) {
                theOracleVersion = new String(oracleVersions[i]);
                break;
            }
        }

    }

    public static String getVersionQuery(int dbProd) {
        if (dbProd == ConnectionWrangler.DB2) {
            return (versionQueryDb2);
        }
        return (versionQuery);
    }

    public static String getVersionQuery() {
        return (versionQuery);
    }


    public static String getDateQuery() {
        return (dateQuery);
    }

    public static String getSeqQuery(boolean hasSecondName) {
        if (hasSecondName) {
            return (seqQry);
        }

        return (fakeSeqQry);
    }

    public static String getArgQryPack(String oracleVersion, int mode) {
        String orderBy = "";
        String post901Attrs = "";
        String post1020Attrs = "";

        if (oracleVersion.startsWith("8")
                || oracleVersion.startsWith("9.0")
                || oracleVersion.startsWith("DB2")) {
        } else {
            post901Attrs = argQrySelectGtr901;
        }

        if (oracleVersion.startsWith("8")
                || oracleVersion.startsWith("9")
                || oracleVersion.startsWith("10.")
                || oracleVersion.startsWith("DB2")) {
        } else {
            post1020Attrs = argQrySelectGtr1020;
        }


        if (oracleVersion.startsWith("DB2")) {
            if (mode == GENERIC_ARG_QUERY) {
                orderBy = new String(db2ArgQryOrderBy);
            } else if (mode == ATTR_ARG_QUERY) {
                orderBy = new String(db2ArgRecQryAttrOrderBy);
            } else {
                orderBy = new String(db2ArgRecQryOrderBy);
            }

            return (db2ArgQrySelect + post1020Attrs + post901Attrs + "," + db2NumberBracket + db2ArgQryPack + orderBy);
        }

        if (mode == GENERIC_ARG_QUERY) {
            orderBy = new String(argQryOrderBy);
        } else if (mode == ATTR_ARG_QUERY) {
            orderBy = new String(argRecQryAttrOrderBy);
        } else {
            orderBy = new String(argRecQryOrderBy);
        }

        return (argQrySelect + post1020Attrs + post901Attrs + "," + numberBracket + argQryPack + orderBy);
    }

    public static String getArgQryPackOverload(String oracleVersion, int mode) {
        String orderBy = "";
        String post901Attrs = "";
        String post1020Attrs = "";

        if (oracleVersion.startsWith("8")
                || oracleVersion.startsWith("9.0")) {
        } else {
            post901Attrs = argQrySelectGtr901;
        }

        if (oracleVersion.startsWith("8")
                || oracleVersion.startsWith("9.0")) {
        } else {
            post901Attrs = argQrySelectGtr901;
        }

        if (oracleVersion.startsWith("8")
                || oracleVersion.startsWith("9")
                || oracleVersion.startsWith("10.")) {
        } else {
            post1020Attrs = argQrySelectGtr1020;
        }

        if (mode == GENERIC_ARG_QUERY) {
            orderBy = new String(argQryOrderBy);
        } else if (mode == ATTR_ARG_QUERY) {
            orderBy = new String(argRecQryAttrOrderBy);
        } else {
            orderBy = new String(argRecQryOrderBy);
        }

        return (argQrySelect + post1020Attrs + post901Attrs + argQryPackOverload + orderBy);
    }

    /**
     * public static String getArgRecQryPack(String oracleVersion)
     * {
     * return(argQrySelect + argQryPack + argRecQryOrderBy);
     * }
     * <p>
     * public static String getArgRecQryPackOverload(String oracleVersion)
     * {
     * return(argQrySelect + argQryPackOverload + argRecQryOrderBy);
     * }
     **/
    public static String getArgQry(String oracleVersion, int mode) {
        String orderBy = "";

        if (oracleVersion == null) {
            oracleVersion = "9.2.0";
        }


        if (oracleVersion.startsWith("DB2")) {
            if (mode == GENERIC_ARG_QUERY) {
                orderBy = new String(db2ArgQryOrderBy);
            } else if (mode == ATTR_ARG_QUERY) {
                orderBy = new String(db2ArgRecQryAttrOrderBy);
            } else // assume PLSQL_ARG_QUERY
            {
                orderBy = new String(db2ArgRecQryOrderBy);
            }

            return (db2ArgQrySelect + argQry920 + orderBy);

        }

        if (mode == GENERIC_ARG_QUERY) {
            orderBy = new String(argQryOrderBy);
        } else if (mode == ATTR_ARG_QUERY) {
            orderBy = new String(argRecQryAttrOrderBy);
        } else // assume PLSQL_ARG_QUERY
        {
            orderBy = new String(argRecQryOrderBy);
        }

        if (oracleVersion.equals("9.0.1")) {
            return (argQrySelect + argQry901 + orderBy);
        }

        return (argQrySelect + argQry920 + orderBy);
    }

    public static String getArgQryOverload(String oracleVersion, int mode) {
        // There is no such thing as an overloaded proc outside a package. So Far.
        return (getArgQry(oracleVersion, mode));
    }

    /**
     * public static String getArgRecQry(String oracleVersion)
     * {
     * <p>
     * if (oracleVersion == null)
     * {
     * oracleVersion = "9.2.0";
     * }
     * <p>
     * if (oracleVersion.equals("9.0.1"))
     * {
     * return(argQrySelect + argQry901 + argRecQryOrderBy);
     * }
     * <p>
     * return(argQrySelect + argQry920 + argRecQryOrderBy);
     * }
     * <p>
     * public static String getArgRecQryOverload(String oracleVersion)
     * {
     * // There is no such thing as an overloaded proc outside a package. So Far.
     * return(getArgRecQry(oracleVersion));
     * }
     **/
    public static String getTableQry(String oracleVersion, boolean hasSecondName) {
        if (hasSecondName) {

            if (oracleVersion.startsWith("DB2")) {
                return (db2TableQry + tableQryOrderBy);
            }
            if (oracleVersion.startsWith("8")
                    || oracleVersion.startsWith("9")) {
                return (tableQry + tableQryOrderBy);
            }

            return (tableQry + tableQryOrderBy);
        }

        if (oracleVersion.startsWith("DB2")) {
            return (db2FakeTableQry + tableQryOrderBy);
        }

        if (oracleVersion.startsWith("8")
                || oracleVersion.startsWith("9")) {
            return (fakeTableQry + tableQryOrderBy);
        }

        return (fakeTableQry + tableQryOrderBy);
    }

    public static String getTypeQry(String oracleVersion) {
        return (typeArgQrySelect);
    }

    public static String getArrayQry(String oracleVersion) {
        return (arrayArgQrySelect);
    }

    public static String getScalerQry(String oracleVersion) {
        return (scalerArgQrySelect);
    }

    /**
     * 23ai fallback: synthesise the element row of a PACKAGE-level collection of a scalar type
     * from ALL_PLSQL_COLL_TYPES, shaped like the ALL_ARGUMENTS child row 23ai no longer returns.
     * See {@code plsqlCollScalarArgQrySelect} for the bind order.
     */
    public static String getPlsqlCollScalarQry(String oracleVersion) {
        return (plsqlCollScalarArgQrySelect);
    }

    /**
     * 23ai fallback: synthesise the element-header + field rows of a PACKAGE-level collection of a
     * PL/SQL RECORD from ALL_PLSQL_COLL_TYPES / ALL_PLSQL_TYPE_ATTRS, shaped like the multi-row
     * ALL_ARGUMENTS child block 23ai no longer returns. See {@code plsqlCollRecordArgQrySelect} for
     * the 19-bind order.
     */
    public static String getPlsqlCollRecordQry(String oracleVersion) {
        return (plsqlCollRecordArgQrySelect);
    }

    /**
     * 23ai fallback: synthesise the single OBJECT element-header row of a PACKAGE-level collection of
     * a SCHEMA OBJECT type (elem_type_package NULL) from ALL_PLSQL_COLL_TYPES. See
     * {@code plsqlCollObjectArgQrySelect} for the 10-bind order. The object's attributes are fetched
     * downstream by {@code getChildRecord}'s object arm via {@code getAttrArguments}.
     */
    public static String getPlsqlCollObjectQry(String oracleVersion) {
        return (plsqlCollObjectArgQrySelect);
    }

    /**
     * 23ai fallback: synthesise the single all-null PL/SQL RECORD element-header row of a PACKAGE-level
     * collection of a table %ROWTYPE from ALL_PLSQL_COLL_TYPES. See {@code plsqlCollRowtypeArgQrySelect}
     * for the 10-bind order. The all-null TYPE_* makes {@code getChildRecord} parse the package source
     * to resolve the %ROWTYPE to its base table; the row ALSO carries that table outright, in
     * ROWTYPE_TABLE_OWNER/NAME, because the source parse cannot see a wrapped package and the field
     * synthesis needs the table by name rather than by whatever objectName ended up holding.
     */
    public static String getPlsqlCollRowtypeQry(String oracleVersion) {
        return (plsqlCollRowtypeArgQrySelect);
    }

    /**
     * 23ai fallback: synthesise the scalar element row of a package INDEX-BY (associative array) table
     * from ALL_PLSQL_COLL_TYPES, shaped like the ALL_ARGUMENTS child row 23ai no longer returns, so
     * CallableStatementParameterEngine recognises the element datatype. See
     * {@code plsqlIndexbyElemArgQrySelect} for the 9-bind order.
     */
    public static String getPlsqlIndexbyElemQry(String oracleVersion) {
        return (plsqlIndexbyElemArgQrySelect);
    }

    /**
     * 23ai fallback: synthesise the field rows of a direct package RECORD type from
     * ALL_PLSQL_TYPE_ATTRS (the record-as-type path used by the "Supporting class identified" loop),
     * shaped like the ATTR_ARG_QUERY field rows. See {@code plsqlRecordFieldArgQrySelect} for the
     * 8-bind order.
     */
    public static String getPlsqlRecordFieldQry(String oracleVersion) {
        return (plsqlRecordFieldArgQrySelect);
    }

    /**
     * 23ai fallback for a package RECORD whose fields are COLLECTIONS, which
     * {@code getPlsqlRecordFieldQry}'s one-row-per-attribute shape cannot express: each such field
     * needs a 'PL/SQL TABLE' row AND an element row a level deeper, the way 12c reports it. Only
     * used after the flat form has been rejected, so no record that synthesises today changes path.
     * See {@code plsqlRecordCollectionFieldArgQrySelect} for the 11-bind order.
     */
    public static String getPlsqlRecordCollectionFieldQry(String oracleVersion) {
        return (plsqlRecordCollectionFieldArgQrySelect);
    }

    /**
     * 23ai fallback: synthesise the field rows of a table/view %ROWTYPE record from ALL_TAB_COLUMNS
     * (one row per column), shaped like the ATTR_ARG_QUERY field rows. See
     * {@code plsqlRowtypeFieldArgQrySelect} for the 7-bind order.
     */
    public static String getPlsqlRowtypeFieldQry(String oracleVersion) {
        return (plsqlRowtypeFieldArgQrySelect);
    }

    /**
     * The table/view a synonym points at. A strong REF CURSOR's RETURN often resolves through a
     * PUBLIC synonym, and 19c/21c/23ai report that synonym's owner ('PUBLIC') as the cursor row's
     * TYPE_OWNER -- an owner ALL_TAB_COLUMNS has no rows under, so the %ROWTYPE field synthesis needs
     * the real target first. Binds: 1 synonym owner, 2 synonym name. Returns no rows when the name is
     * already a real table/view, which the caller treats as "use it as given".
     */
    public static String getSynonymTargetQry(String oracleVersion) {
        return (synonymTargetQrySelect);
    }

    private static final String synonymTargetQrySelect =
            "select /* " + Namer.param_product_name + " */ table_owner TABLE_OWNER, table_name TABLE_NAME "
                    + "FROM all_synonyms WHERE owner = ? AND synonym_name = ? AND table_owner IS NOT NULL";

    public static int getFuncQry1Count(String oracleVersion) {
        if (oracleVersion == null) {
            oracleVersion = latestOracleVersion;
        }

        if (oracleVersion.equals("8.1.5")) {
            return (8);
        } else if (oracleVersion.equals("8.1.6")) {
            return (8);
        } else if (oracleVersion.equals("8.1.7")) {
            return (8);
        } else if (oracleVersion.equals("9.0.1")) {
            return (8);
        } else if (oracleVersion.equals("9.2.0")) {
            return (8);
        } else if (oracleVersion.equals("10.1.0")) {
            return (8);
        } else if (oracleVersion.equals("10.2.0")) {
            return (8);
        } else if (oracleVersion.equals("11.1.0")) {
            return (8);
        } else if (oracleVersion.equals("11.2.0")) {
            return (8);
        } else if (oracleVersion.equals("12.1.0")) {
            return (8);
        } else if (oracleVersion.startsWith("DB2")) {
            return (4);
        }

        return (8);
    }

    public static int getFuncQry2Count(String oracleVersion) {
        if (oracleVersion == null) {
            oracleVersion = latestOracleVersion;
        }

        if (oracleVersion.equals("8.1.5")) {
            return (2);
        } else if (oracleVersion.equals("8.1.6")) {
            return (4);
        } else if (oracleVersion.equals("8.1.7")) {
            return (4);
        } else if (oracleVersion.equals("9.0.1")) {
            return (4);
        } else if (oracleVersion.equals("9.2.0")) {
            return (4);
        } else if (oracleVersion.equals("10.1.0")) {
            return (4);
        } else if (oracleVersion.equals("10.2.0")) {
            return (4);
        } else if (oracleVersion.equals("11.1.0")) {
            return (4);
        } else if (oracleVersion.equals("11.2.0")) {
            return (4);
        } else if (oracleVersion.equals("12.1.0")) {
            return (4);
        } else if (oracleVersion.startsWith("DB2")) {
            return (2);
        }

        return (2);
    }

    public static String getFuncQry(String oracleVersion, boolean hasSecondName) {
        String union = " union ";

        if (oracleVersion == null) {
            oracleVersion = latestOracleVersion;
        }

        if (oracleVersion.startsWith("DB2")) {
            return (db2_funcQry920_0_np_local + union //proc only!
                    + db2_funcQry920_n_x_local
                    + funcQry920orderBy);
        }

        if (hasSecondName) {
            if (oracleVersion.equals("8.1.5")) {
                return (funcQry920_0_np_local + union
                        + funcQry920_n_x_local + union
                        + funcQry920_n_np_syn + union
                        + funcQry920_0_np_syn + /* union
        + funcQry815_x_p_syn + */  funcQry920orderBy);
            }

            if (oracleVersion.equals("8.1.6")
                    || oracleVersion.equals("8.1.7")) {
                return (funcQry920_0_np_local + union
                        + funcQry920_n_x_local + union
                        + funcQry920_n_np_syn + union
                        + funcQry920_0_np_syn /* + union
          + funcQry920_x_p_syn  */ + funcQry920orderBy);
            }

            if (oracleVersion.equals("9.0.1")) {
                return (funcQry901_0_np_local + union
                        + funcQry901_n_x_local + union
                        // + funcQry901_n_np_syn + union
                        // + funcQry920_n_np_syn + union
                        + funcQry901_0_np_syn /* + union
          + funcQry901_x_p_syn */ + funcQry920orderBy);
            }

            if (oracleVersion.equals("9.2.0")) {
                return (funcQry920_0_np_local + union
                        + funcQry920_0_np_syn + union
                        + funcQry920_n_np_syn + union
                        + funcQry920_n_x_local /* + union
            + funcQry920_x_p_syn */ + funcQry920orderBy);
            }

            return (funcQry920_0_np_local + union
                    + funcQry920_0_np_syn + union  // ok
                    + funcQry920_n_np_syn + union     // ok
                    + funcQry920_n_x_local /* + union   // ok
          + funcQry920_x_p_syn */ + funcQry920orderBy);
        }
        // .. else only 1 name

        if (oracleVersion.equals("8.1.5")) {
            return (fake_funcQry920_0_np_local + union
                    + fake_funcQry920_n_x_local + union
                    + fake_funcQry920_n_np_syn + union
                    + fake_funcQry920_0_np_syn + funcQry920orderBy);
        }

        if (oracleVersion.equals("8.1.6")
                || oracleVersion.equals("8.1.7")) {
            return (fake_funcQry920_0_np_local + union
                    + fake_funcQry920_n_x_local + union
                    + fake_funcQry920_n_np_syn + union
                    + fake_funcQry920_0_np_syn + funcQry920orderBy);
        }

        if (oracleVersion.equals("9.0.1")) {
            return (funcQry901_0_np_local + union
                    + fake_funcQry901_n_x_local + union
                    + fake_funcQry901_0_np_syn + funcQry920orderBy);
        }

        if (oracleVersion.equals("9.2.0")) {
            return (fake_funcQry920_0_np_local + union
                    + fake_funcQry920_0_np_syn + union
                    + fake_funcQry920_n_np_syn + union
                    + fake_funcQry920_n_x_local + funcQry920orderBy);
        }

        return (fake_funcQry920_0_np_local + union
                + fake_funcQry920_0_np_syn + union
                + fake_funcQry920_n_np_syn + union
                + fake_funcQry920_n_x_local + funcQry920orderBy);
    }

    public static String getFuncQry2(String oracleVersion, boolean hasSecondName) {
        //String union = " union all ";

        if (oracleVersion == null) {
            oracleVersion = latestOracleVersion;
        }


        if (oracleVersion.startsWith("DB2")) {
            return (db2_fake_funcQry_2 + funcQry920orderBy);
        }


        if (hasSecondName) {
            if (oracleVersion.equals("8.1.5")) {
                return (/*funcQry920_0_np_local + union
          + funcQry920_n_x_local +  union
          + funcQry920_n_np_syn +  union
          + funcQry920_0_np_syn + union
          + */funcQry815_x_p_syn + funcQry920orderBy);
            }

            if (oracleVersion.equals("8.1.6")
                    || oracleVersion.equals("8.1.7")) {
                return (/*funcQry920_0_np_local + union
          + funcQry920_n_x_local +  union
          + funcQry920_n_np_syn +  union
          + funcQry920_0_np_syn + union
          +*/ funcQry920_x_p_syn + funcQry920orderBy);
            }

            if (oracleVersion.equals("9.0.1")) {
                return (/*funcQry901_0_np_local + union
          + funcQry901_n_x_local +  union
         // + funcQry901_n_np_syn + union
         // + funcQry920_n_np_syn + union
          + funcQry901_0_np_syn + union
          + */funcQry901_x_p_syn + funcQry920orderBy);
            }

            if (oracleVersion.equals("9.2.0")) {
                return ( /* funcQry920_0_np_local + union
            + funcQry920_0_np_syn + union
            + funcQry920_n_np_syn +  union
            + funcQry920_n_x_local + union
            + */ funcQry920_x_p_syn + funcQry920orderBy);
            }

            return ( /* funcQry920_0_np_local + union
          + funcQry920_0_np_syn + union
          + funcQry920_n_np_syn +  union
          + funcQry920_n_x_local + union
          + */ funcQry920_x_p_syn + funcQry920orderBy);
        }
        // else has 1 name
        if (oracleVersion.equals("8.1.5")) {
            return (/*funcQry920_0_np_local + union
        + funcQry920_n_x_local +  union
        + funcQry920_n_np_syn +  union
        + funcQry920_0_np_syn + union
        + */ fake_funcQry815_x_p_syn + funcQry920orderBy);
        }

        if (oracleVersion.equals("8.1.6")
                || oracleVersion.equals("8.1.7")) {
            return (/*funcQry920_0_np_local + union
        + funcQry920_n_x_local +  union
        + funcQry920_n_np_syn +  union
        + funcQry920_0_np_syn + union
        +*/ fake_funcQry920_x_p_syn + funcQry920orderBy);
        }

        if (oracleVersion.equals("9.0.1")) {
            return (/*funcQry901_0_np_local + union
        + funcQry901_n_x_local +  union
       // + funcQry901_n_np_syn + union
       // + funcQry920_n_np_syn + union
        + funcQry901_0_np_syn + union
        + */ fake_funcQry901_x_p_syn + funcQry920orderBy);
        }

        if (oracleVersion.equals("9.2.0")) {
            return ( /* funcQry920_0_np_local + union
          + funcQry920_0_np_syn + union
          + funcQry920_n_np_syn +  union
          + funcQry920_n_x_local + union
          + */ fake_funcQry920_x_p_syn + funcQry920orderBy);
        }

        return ( /* funcQry920_0_np_local + union
        + funcQry920_0_np_syn + union
        + funcQry920_n_np_syn +  union
        + funcQry920_n_x_local + union
        + */ fake_funcQry920_x_p_syn + funcQry920orderBy);
    }

    private static String getAllTabColsName(String oracleVersion) {
        if (oracleVersion.startsWith("8")) {
            return ("all_tab_columns");
        }
        return ("all_tab_cols");
    }

    public static String getAllTablesQry(String oracleVersion) {
        return ("SELECT * "
                + "FROM all_tables "
                + "WHERE owner = ? "
                + "AND   table_name = ? ");
    }

    public static String getCollTypeQuery(String oracleVersion) {
        if (oracleVersion.startsWith("8.")
                || oracleVersion.startsWith("9.")
                || oracleVersion.startsWith("10.")) {
            return (collectionQueryLte102);
        }
        return (collectionQuery);
    }

    public static String getAllTabColsQry(String oracleVersion) {
        return getAllTabColsQry(oracleVersion, false);
    }

    /**
     * As {@link #getAllTabColsQry(String)}, additionally selecting the Oracle 23ai
     * {@code VECTOR_INFO} descriptor column when {@code withVectorInfo} is true, so a
     * VECTOR column's storage format (dense / binary / sparse) can be recovered — see
     * {@link com.mcpdbwizard.pub.SqlUtils#getVectorDatatypeFromInfo(String)}.
     * <p>
     * {@code VECTOR_INFO} exists only on Oracle 23ai+; the caller must feature-detect it
     * and pass {@code false} on older releases, otherwise the reference to a non-existent
     * column raises {@code ORA-00904}. When {@code false} the SQL is byte-identical to the
     * pre-existing query (no extra column), so nothing downstream changes.
     *
     * @param oracleVersion   the version string used to shape the query
     * @param withVectorInfo  {@code true} to select {@code VECTOR_INFO} (23ai+ only)
     */
    public static String getAllTabColsQry(String oracleVersion, boolean withVectorInfo) {
        // On 23ai+ (withVectorInfo) rewrite DATA_TYPE for VECTOR columns to the format token; otherwise
        // the column list is byte-identical to the pre-existing query.
        String colList = withVectorInfo
                ? vectorFormatDataType + generateTablesColList.substring("DATA_TYPE".length())
                : generateTablesColList;

        if (oracleVersion.startsWith("8")) {
            return ("SELECT  " + colList + " "
                    + "FROM " + getAllTabColsName(oracleVersion) + " "
                    + "WHERE owner = ? "
                    + "AND   table_name = ? "
                    + "ORDER BY column_id");
        }

        return ("SELECT " + colList + " "
                + "FROM " + getAllTabColsName(oracleVersion) + " "
                + "WHERE owner = ? "
                + "AND   table_name = ? "
                + "AND   hidden_column = 'NO' "
                + "ORDER BY column_id");

    }

    public static String getPlsqlErrorQry(String oracleVersion) {
        if (oracleVersion.startsWith("DB2")) {
            return (db2PlsqlErrorQry);
        }

        if (oracleVersion.startsWith("8") || oracleVersion.startsWith("9")) {
            return (plsqlErrorQry);
        }

        return (plsqlErrorQryAtt);

    }

    public static String getAllTabColsCSEQry(String oracleVersion) {
        return getAllTabColsCSEQry(oracleVersion, false);
    }

    /**
     * As {@link #getAllTabColsCSEQry(String)}, but on Oracle 23ai+ ({@code withVectorInfo})
     * rewrites a VECTOR column's DATA_TYPE to the format token (VECTOR_BINARY / VECTOR_SPARSE)
     * via {@code VECTOR_INFO}, so the callable-statement engine that generates a table's INSERT/
     * UPDATE binds a binary vector as VECTOR_BINARY. Pass {@code false} on pre-23ai (no VECTOR_INFO).
     */
    public static String getAllTabColsCSEQry(String oracleVersion, boolean withVectorInfo) {
        String dataType = withVectorInfo ? vectorFormatDataType : "DATA_TYPE";

        if (oracleVersion.startsWith("8")) {
            return ("SELECT OWNER, 'row_'||lower(column_name) ARGUMENT_NAME, 'IN/OUT' IN_OUT, 0 OVERLOAD, COLUMN_ID POSITION, " + dataType + ", '' PACKAGE_NAME, '' OBJECT_NAME, DATA_LENGTH, " + numberBracket
                    + "FROM " + getAllTabColsName(oracleVersion) + " "
                    + "WHERE owner = ? "
                    + "AND   table_name = ? "
                    + "ORDER BY column_id");
        }

        return ("SELECT OWNER, 'row_'||lower(column_name) ARGUMENT_NAME, 'IN/OUT' IN_OUT, 0 OVERLOAD, COLUMN_ID POSITION, " + dataType + ", '' PACKAGE_NAME, '' OBJECT_NAME, DATA_LENGTH, " + numberBracket
                + "FROM " + getAllTabColsName(oracleVersion) + " "
                + "WHERE owner = ? "
                + "AND   table_name = ? "
                + "AND   hidden_column = 'NO' "
                + "ORDER BY column_id");

    }

    /**
     * Whether a view is a 23ai <b>JSON-relational duality view</b>, which DML
     * operations it permits, its document column's name, and its root table
     * (ROOT_TABLE_OWNER / ROOT_TABLE_NAME — whose primary-key cardinality decides
     * whether the view's {@code _id} is a scalar or a composite-key object). Returns
     * at most one row (JSON_COLUMN_NAME plus ALLOW_INSERT / ALLOW_UPDATE /
     * ALLOW_DELETE, each 'TRUE'/'FALSE'); zero rows means the object is not a
     * duality view. {@code ALL_JSON_DUALITY_VIEWS} only exists on 23ai+, so the
     * caller must feature-detect it first (see
     * {@code SAAdminWrangler.isDualityViewInfoAvailable()}).
     */
    public static String getJsonDualityViewQry() {
        return ("SELECT json_column_name, allow_insert, allow_update, allow_delete "
                + ", root_table_owner, root_table_name "
                + "FROM all_json_duality_views "
                + "WHERE view_owner = ? "
                + "AND   view_name = ? ");
    }

    /**
     * The document fields a duality view exposes, one row per underlying column,
     * from {@code ALL_JSON_DUALITY_VIEW_TAB_COLS}: the field's JSON key name, the
     * column's Oracle datatype, whether the view treats it as read-only, whether it
     * belongs to the root table, its 1-based position in the root table's primary
     * key (null for non-key columns), and the underlying column name. Hidden
     * columns are excluded. Used by the MCP server emission to bake each view's
     * field list into the generated tool descriptions/schemas (a duality view
     * rejects documents with unknown fields, so the tool caller needs to know
     * them). The dictionary view's flag columns are native 23ai BOOLEANs, so they
     * are folded to 'YES'/'NO' text here; only reached behind the duality-view
     * feature gate (23ai+), never on older servers.
     */
    public static String getJsonDualityViewTabColsQry() {
        return ("SELECT json_key_name, data_type, column_name, primary_key_pos "
                + ", CASE WHEN read_only THEN 'YES' ELSE 'NO' END READ_ONLY "
                + ", CASE WHEN root_table THEN 'YES' ELSE 'NO' END ROOT_TABLE "
                + "FROM all_json_duality_view_tab_cols "
                + "WHERE view_owner = ? "
                + "AND   view_name = ? "
                + "AND   NOT is_hidden "
                + "ORDER BY table_number, primary_key_pos NULLS LAST, column_name ");
    }

    /**
     * How many columns a table's primary key has (CNT; 0 when the table has no PK).
     * Used to decide whether a duality view's {@code _id} is a scalar (single-column
     * root PK) or a composite-key JSON object (multi-column root PK). Only reached on
     * 23ai+ behind the duality-view feature gate, so the two-view join is safe (the
     * 19c dictionary bind-loss issue never sees it).
     */
    public static String getPkColCountQry() {
        return ("SELECT COUNT(*) CNT "
                + "FROM all_constraints k, all_cons_columns c "
                + "WHERE k.owner = c.owner "
                + "AND   k.constraint_name = c.constraint_name "
                + "AND   k.constraint_type = 'P' "
                + "AND   k.owner = ? "
                + "AND   k.table_name = ? ");
    }

    /**
     * A table's primary-key columns: COLUMN_NAME plus its 1-based POSITION in the
     * key, ordered by position. Used by the MCP server emission to key the
     * generated ordinary-table row-CRUD tools (get_by_pk / update / delete). Same
     * two-view {@code ALL_CONSTRAINTS}+{@code ALL_CONS_COLUMNS} join as
     * {@link #getPkColCountQry()} (kept a plain join, no correlated subquery, for
     * the 19c dictionary bind-loss issue).
     */
    public static String getPkColsQry() {
        return ("SELECT c.COLUMN_NAME, c.POSITION "
                + "FROM all_constraints k, all_cons_columns c "
                + "WHERE k.owner = c.owner "
                + "AND   k.constraint_name = c.constraint_name "
                + "AND   k.constraint_type = 'P' "
                + "AND   k.owner = ? "
                + "AND   k.table_name = ? "
                + "ORDER BY c.POSITION ");
    }

    /**
     * The columns of a named constraint (COLUMN_NAME ordered by POSITION), from
     * {@code ALL_CONS_COLUMNS}. Used by the MCP server emission to key a generated
     * unique-key row-lookup tool.
     */
    public static String getConstraintColsQry() {
        return ("SELECT column_name, position "
                + "FROM all_cons_columns "
                + "WHERE owner = ? "
                + "AND   constraint_name = ? "
                + "ORDER BY position ");
    }

    /**
     * The columns of a named index (COLUMN_NAME ordered by COLUMN_POSITION), from
     * {@code ALL_IND_COLUMNS}. Used by the MCP server emission to key a generated
     * index row-lookup tool.
     */
    public static String getIndexColsQry() {
        return ("SELECT column_name, column_position "
                + "FROM all_ind_columns "
                + "WHERE index_owner = ? "
                + "AND   index_name = ? "
                + "ORDER BY column_position ");
    }

    /**
     * A table's 12c+ {@code GENERATED ... AS IDENTITY} columns: COLUMN_NAME plus
     * GENERATION_TYPE ('ALWAYS' or 'BY DEFAULT'). {@code ALL_TAB_IDENTITY_COLS} only
     * exists on 12c+, so the caller must feature-detect it first (see
     * {@code SAAdminWrangler.isIdentityInfoAvailable()}) — on older servers the query
     * raises ORA-00942. Kept a single-view query (no join into the tab-cols queries)
     * because 19c is known to lose bind variables in correlated dictionary subqueries.
     */
    public static String getTabIdentityColsQry() {
        return ("SELECT column_name, generation_type "
                + "FROM all_tab_identity_cols "
                + "WHERE owner = ? "
                + "AND   table_name = ? ");
    }


    /**
     * "SELECT 'IN' IN_OUT, 'Param" +paramPropName[i] + "' ARGUMENT_NAME, "
     * + "0 OVERLOAD, '" + paramPropJavaDataTypes[i] + "' DATA_TYPE, "
     * + (i+1) + " POSITION, USER OWNER, '' PACKAGE_NAME, '' OBJECT_NAME FROM DUAL ";
     **/

/*
 public static String getAllIndexesQuery(String oracleVersion)
  {
  return("SELECT * "
        +"FROM   all_indexes "
        +"WHERE  table_owner = ? "
        +"AND    table_name = ? "
        +"ORDER BY index_name");
  }
  */
 /*
 public static String getAllIndexColsQuery(String oracleVersion)
  {
  return("SELECT * "
        +"FROM   all_ind_columns "
        +"WHERE  table_owner = ? "
        +"AND    table_name = ? "
        +"AND    index_name = ? "
        +"ORDER BY index_name, column_position");
  }
  */
    public static String getAllConstraintsQuery(String oracleVersion) {
        return ("SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.OWNER, c.R_OWNER, c.R_CONSTRAINT_NAME, decode(owner, user, 'this.','PARAM_OBJ_OWNER') pcv_owner "
                + "FROM all_constraints c "
                + "WHERE owner = ? "
                + "AND   table_name = ? "
                + "AND   status = 'ENABLED' "
                + "ORDER BY table_name, decode(constraint_type,'P',1,'U',2,'R',3,'C',5,4), constraint_name ");
    }

    public static String getAllConstraintsQuery3(String oracleVersion, boolean doIndices) {
        String orderBy = "ORDER BY 1,3";

        String constraints =
                "SELECT decode(constraint_type,'P',1,'U',2,'R',3,4) ct, TABLE_NAME, CONSTRAINT_NAME,CONSTRAINT_TYPE,OWNER,R_OWNER "
                        + ",R_CONSTRAINT_NAME "
                        + ", decode(owner, user, 'this.','PARAM_OBJ_OWNER') pcv_owner  "
                        + ", decode(constraint_type,'P','UNIQUE','U', 'UNIQUE','R','NONUNIQUE','CHECK') uniqueness "
                        + "FROM all_constraints c "
                        + "WHERE c.owner = ? "
                        + "AND   c.table_name = ? "
                        + "AND   c.status = 'ENABLED' ";

        String indices =
                "SELECT 5, c2.TABLE_NAME, c2.index_name CONSTRAINT_NAME, 'INDEX' CONSTRAINT_TYPE,OWNER,to_char(null) R_OWNER "
                        + ",to_char(null) R_CONSTRAINT_NAME "
                        + ", decode(owner, user, 'this.','PARAM_OBJ_OWNER') pcv_owner, c2.uniqueness "
                        + "FROM all_indexes c2 "
                        + "WHERE c2.table_owner = ? "
                        + "AND   c2.table_name = ? "
                        + "AND   c2.status = 'VALID' "
                        + "AND (NOT EXISTS (SELECT null "
                        + "                 FROM all_cons_columns c3 "
                        + "                 WHERE c3.owner = ? "
                        + "                 AND   c3.table_name = ? "
                        + "                 GROUP BY c3.constraint_name "
                        + "                 HAVING   max(decode(c3.position,1,substr(c3.column_name,1,30), null)) "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,2,substr(c3.column_name,1,30), null)) "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,3,substr(c3.column_name,1,30), null)) "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,4,substr(c3.column_name,1,30), null)) "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,5,substr(c3.column_name,1,30), null)) "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,6,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,7,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,8,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,9,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,10,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,11,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,12,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,13,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,14,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,15,substr(c3.column_name,1,30), null))  "
                        + "                        ||'|' "
                        + "                        ||max(decode(c3.position,16,substr(c3.column_name,1,30), null))  "
                        + "                 = (SELECT   max(decode(aic.column_position,1,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,2,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,3,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,4,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,5,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,6,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,7,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,8,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,9,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,10,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,11,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,12,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,13,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,14,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,15,substr(aic.column_name,1,30), null)) "
                        + "                           ||'|' "
                        + "                           ||max(decode(aic.column_position,16,substr(aic.column_name,1,30), null)) "
                        + "                    FROM   all_ind_columns aic "
                        + "                    WHERE  aic.table_name = c2.table_name "
                        + "                    AND    aic.index_owner = c2.owner "
                        + "                    AND    aic.index_name  = c2.index_name) "
                        + "                )) ";

        if (doIndices) {
            return (constraints + " union all " + indices + orderBy);
        }

        return (constraints + orderBy);
    }

    public static String getAllConstraintsQuery2(String oracleVersion, boolean doIndices) {
        String orderBy = "ORDER BY 1,3";

        String constraints =
                "SELECT decode(constraint_type,'P',1,'U',2,'R',3,4) ct, TABLE_NAME, CONSTRAINT_NAME,CONSTRAINT_TYPE,OWNER,R_OWNER "
                        + ",R_CONSTRAINT_NAME "
                        + ", decode(owner, user, 'this.','PARAM_OBJ_OWNER') pcv_owner  "
                        + ", decode(constraint_type,'P','UNIQUE','U', 'UNIQUE','R','NONUNIQUE','CHECK') uniqueness "
                        + "FROM all_constraints c "
                        + "WHERE c.owner = ? "
                        + "AND   c.table_name = ? "
                        + "AND   c.status = 'ENABLED' ";

        String indices8i =
                "SELECT 5, c2.TABLE_NAME, c2.index_name CONSTRAINT_NAME, 'INDEX' CONSTRAINT_TYPE,OWNER,to_char(null) R_OWNER "
                        + ",to_char(null) R_CONSTRAINT_NAME "
                        + ", decode(owner, user, 'this.','PARAM_OBJ_OWNER') pcv_owner, c2.uniqueness "
                        + "FROM all_indexes c2 "
                        + "WHERE c2.table_owner = ? "
                        + "AND   c2.table_name = ? "
                        + "AND   c2.status = 'VALID' "
                        + "AND NOT EXISTS (SELECT null "
                        + "                FROM all_constraints c3 "
                        + "                WHERE c3.owner = c2.table_owner "
                        + "                AND   c3.table_name = c2.table_name "
                        + "                AND   c3.constraint_name  = c2.index_name) ";

        String indices9i =
                "SELECT 5, c2.TABLE_NAME, c2.index_name CONSTRAINT_NAME, 'INDEX' CONSTRAINT_TYPE,OWNER,to_char(null) R_OWNER "
                        + ",to_char(null) R_CONSTRAINT_NAME "
                        + ", decode(owner, user, 'this.','PARAM_OBJ_OWNER') pcv_owner, c2.uniqueness "
                        + "FROM all_indexes c2 "
                        + "WHERE c2.table_owner = ? "
                        + "AND   c2.table_name = ? "
                        + "AND   c2.status = 'VALID' "
                        + "AND NOT EXISTS (SELECT null "
                        + "                FROM all_constraints c3 "
                        + "                WHERE c3.owner = c2.table_owner "
                        + "                AND   c3.table_name = c2.table_name "
                        + "                AND   NVL(c3.index_owner,c2.owner) = c2.owner  "
                        + "                AND   c3.index_name  = c2.index_name) ";

        String indices = new String(indices9i);

        if (oracleVersion.startsWith("8")) {
            indices = new String(indices8i);
        }

        if (doIndices) {
            return (constraints + " union all " + indices + orderBy);
        }

        return (constraints + orderBy);
    }

    public static String getAllChildConstraintsQuery(String oracleVersion) {
        return ("SELECT c.*, decode(owner, user, 'this.','PARAM_OBJ_OWNER') pcv_owner "
                + "FROM all_constraints c "
                + "WHERE r_owner = ? "
                + "AND   r_constraint_name = ? "
                + "AND   constraint_type = 'R' "
                + "AND   status = 'ENABLED' "
                + "AND   NOT EXISTS (SELECT null FROM all_constraints c2 WHERE c2.owner = c.r_owner AND c2.constraint_name = c.r_constraint_name AND c2.table_name = c.table_name) "
                + "ORDER BY table_name, constraint_name ");
    }

    /**
     * public static String getAllConsColumnsQuery(String oracleVersion)
     * {
     * return("SELECT  c.OWNER, c.CONSTRAINT_NAME, c.TABLE_NAME, c.COLUMN_NAME, c.POSITION, t.DATA_TYPE "
     * +"FROM all_cons_columns c "
     * +"   , all_tab_columns t "
     * +"WHERE c.owner = ? "
     * +"AND   c.constraint_name = ? "
     * +"AND   c.owner = t.owner "
     * +"AND   c.table_name = t.table_name "
     * +"AND   c.column_name = t.column_name "
     * +"ORDER BY c.position");
     * }
     **/

    public static String getAllConsColumnsQuery2(String oracleVersion) {
        return ("SELECT  c.OWNER, c.CONSTRAINT_NAME, c.TABLE_NAME, c.COLUMN_NAME, c.POSITION, t.DATA_TYPE, '' IDX_EXPR, con.owner INDEX_OWNER "
                + "FROM all_cons_columns c "
                + "   , all_tab_columns t "
                + "   , all_constraints con  "
                + "WHERE con.owner = ? "
                + "AND   con.constraint_name = ? "
                + "AND   c.owner = con.owner "
                + "AND   c.constraint_name = con.constraint_name "
                + "AND   c.owner = t.owner  "
                + "AND   c.table_name = t.table_name  "
                + "AND   c.column_name = t.column_name  "
                + "AND   t.table_name = con.table_name "
                + "AND   t.owner = con.owner "
                + "UNION "
                + "SELECT  c2.table_owner OWNER, c2.index_name CONSTRAINT_NAME, c2.TABLE_NAME, c2.COLUMN_NAME, c2.COLUMN_POSITION POSITION, t2.DATA_TYPE, '' IDX_EXPR,c2.index_owner INDEX_OWNER "
                + "FROM all_ind_columns c2 "
                + "   , all_tab_columns t2 "
                + "WHERE ? = c2.index_name "
                + "AND   ? = c2.table_name "
                + "AND   ? = c2.table_owner "
                + "AND   ? = t2.owner (+)  "
                + "AND   c2.table_name = t2.table_name (+)  "
                + "AND   c2.column_name = t2.column_name (+)  "
                + "ORDER BY 5");
    }


    public static String getFuncIndexExpressionDataType(String oracleVersion) {
        return ("select column_expression "
                + ",' ex from '||table_owner||'.'||table_name||' WHERE rownum = 0' rs "
                + "from all_ind_expressions "
                + "where index_owner = ? "
                + "and   index_name  = ? "
                + "and   column_position = ? ");

    }

    public static String getAllConsColumnsQuery2NoFindex(String oracleVersion) {
        return ("SELECT  c.OWNER, c.CONSTRAINT_NAME, c.TABLE_NAME, c.COLUMN_NAME, c.POSITION, t.DATA_TYPE, '' IDX_EXPR "
                + "FROM all_cons_columns c "
                + "   , all_tab_columns t "
                + "   , all_constraints con  "
                + "WHERE con.owner = ? "
                + "AND   con.constraint_name = ? "
                + "AND   c.owner = con.owner "
                + "AND   c.constraint_name = con.constraint_name "
                + "AND   c.owner = t.owner  "
                + "AND   c.table_name = t.table_name  "
                + "AND   c.column_name = t.column_name  "
                + "AND   t.table_name = con.table_name "
                + "AND   t.owner = con.owner "
                + "UNION /*ALL*/ "
                + "SELECT  c2.table_owner OWNER, c2.index_name CONSTRAINT_NAME, c2.TABLE_NAME, c2.COLUMN_NAME, c2.COLUMN_POSITION POSITION, t2.DATA_TYPE , '' IDX_EXPR "
                + "FROM all_ind_columns c2 "
                + "   , all_tab_columns t2 "
                + "WHERE c2.index_name = ? "
                + "AND   c2.table_name = ? "
                + "AND   c2.table_owner = ? "
                + "AND   c2.table_owner = t2.owner "
                + "AND   c2.table_name = t2.table_name "
                + "AND   c2.column_name = t2.column_name "
      /*   +"AND NOT EXISTS (SELECT null "
         +"                FROM all_constraints c3 "
         +"                WHERE c3.owner = c2.table_owner "
         +"                AND   c3.table_name = c2.table_name "
         +"                AND   NVL(c3.index_owner,c2.index_owner) = c2.index_owner  "
         +"                AND   c3.index_name  = c2.index_name) "   */
                + "ORDER BY 5");
    }

    /*
  public static String getAllIndexColumnsQuery(String oracleVersion)
    {
    return("SELECT  c.OWNER, c.CONSTRAINT_NAME, c.TABLE_NAME, c.COLUMN_NAME, c.COLUMN_POSITION POSITION, t.DATA_TYPE "
          +"FROM all_ind_columns c "
          +"   , all_tab_columns t "
          +"WHERE c.index_owner = ? "
          +"AND   c.index_name = ? "
          +"WHERE c.table_owner = ? "
          +"AND   c.table_name = ? "
          +"AND   c.table_owner = t.owner "
          +"AND   c.table_name = t.table_name "
          +"AND   c.column_name = t.column_name "
          +"ORDER BY c.COLUMN_POSITION");
    }
  */
    public static String getDirQuery(String oracleVersion) {
        return (dirQuery);
    }


}


