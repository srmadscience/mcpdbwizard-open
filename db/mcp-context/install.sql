REM Copyright 2003-2026 ATB Consultancy Services Ltd
REM (formerly Orinda Software Ltd, Dublin, Ireland)
REM
REM install.sql -- the Oracle application context MCP DB Wizard sets for URL context parameters.
REM
REM A config can declare names (CUSTOMER_ID, ...) that a client supplies on its connection URL:
REM
REM     https://<host>/mcp/<owner>/<config>?CUSTOMER_ID=42
REM
REM For every tool call the generated server clears the MCP namespace and sets those values, so
REM your PL/SQL and curated SQL can read them:
REM
REM     SYS_CONTEXT('MCP', 'CUSTOMER_ID')
REM
REM WHAT THIS PROTECTS AGAINST: the agent choosing another customer. The model never sees the URL
REM and no tool can change it. It does NOT protect against a person who can edit the client's
REM configuration, and a value here must never be a secret.
REM
REM WHAT THIS SCRIPT CREATES -- run it ONCE per database (per PDB), as a DBA (SYSTEM will do):
REM
REM   1. User MCPDBWIZARD, which owns one package and can never log in: a schema-only account
REM      (NO AUTHENTICATION) on 18c and later; on 12.1, which has no schema-only accounts, a locked
REM      account with a random password nobody is told.
REM   2. Package MCPDBWIZARD.MCPDBWIZARD_CTX -- clear_all and set_value.
REM   3. CREATE CONTEXT MCP USING MCPDBWIZARD.MCPDBWIZARD_CTX. Only that package can write the
REM      namespace; any other code that calls DBMS_SESSION.SET_CONTEXT('MCP', ...) gets ORA-01031,
REM      including code running as your application account. That is what makes it a control.
REM
REM Then grant each account a generated server connects as:   @grant.sql <ACCOUNT>
REM
REM Needs: CREATE USER, CREATE ANY PROCEDURE, CREATE ANY CONTEXT, SELECT on DBA_CONTEXT and
REM DBA_OBJECTS (the DBA role has them all). Safe to re-run: it replaces the package and leaves
REM the user and the grants alone. It REFUSES to run if a context called MCP already exists bound
REM to some other package -- that is somebody else's, and replacing it would silently break them.
REM
REM Supported: Oracle 12.1 to 26ai.

whenever sqlerror exit failure
set serveroutput on
set feedback off

declare
    v_schema  dba_context.schema%type;
    v_package dba_context.package%type;
begin
    select schema, package into v_schema, v_package
      from dba_context where namespace = 'MCP';
    if v_schema <> 'MCPDBWIZARD' or v_package <> 'MCPDBWIZARD_CTX' then
        raise_application_error(-20900, 'A context named MCP already exists, bound to '
            || v_schema || '.' || v_package || '. It is not ours, so it has not been replaced.'
            || ' Drop it or rename it if you are sure it is unused, then run this again.');
    end if;
exception
    when no_data_found then null;
end;
/

declare
    v_count number;
begin
    select count(*) into v_count from dba_users where username = 'MCPDBWIZARD';
    if v_count = 0 then
        if dbms_db_version.version >= 18 then
            execute immediate 'create user mcpdbwizard no authentication';
        else
            -- 12.1: no schema-only accounts. Locked, with a password generated here and never
            -- shown. A password-verify function in the default profile may reject it; if so,
            -- create the user by hand (ACCOUNT LOCK) and run this script again.
            execute immediate 'create user mcpdbwizard identified by "A'
                || dbms_random.string('X', 29) || '" account lock';
        end if;
        dbms_output.put_line('Created user MCPDBWIZARD.');
    end if;
end;
/

create or replace package mcpdbwizard.mcpdbwizard_ctx authid definer as
    -- Remove every value in the MCP namespace for this session. The generated server calls it
    -- before setting anything, on every tool call, so a pooled connection cannot carry one
    -- caller's values into the next call.
    procedure clear_all;

    -- Set one value. The name is stored upper case. A name that is not a simple identifier of at
    -- most 30 characters, or a NULL value, raises an error rather than setting nothing: Oracle
    -- stores '' as NULL, and a context left NULL is exactly what a required parameter prevents.
    procedure set_value(p_name in varchar2, p_value in varchar2);
end mcpdbwizard_ctx;
/

create or replace package body mcpdbwizard.mcpdbwizard_ctx as
    c_namespace constant varchar2(30) := 'MCP';

    procedure clear_all is
    begin
        dbms_session.clear_all_context(c_namespace);
    end clear_all;

    procedure set_value(p_name in varchar2, p_value in varchar2) is
    begin
        if p_name is null or length(p_name) > 30
                or not regexp_like(p_name, '^[A-Za-z][A-Za-z0-9_]*$') then
            raise_application_error(-20901,
                'MCP context: ''' || substr(p_name, 1, 40) || ''' is not a valid parameter name.');
        end if;
        if p_value is null then
            raise_application_error(-20902,
                'MCP context: parameter ' || upper(p_name) || ' has no value.');
        end if;
        dbms_session.set_context(c_namespace, upper(p_name), p_value);
    end set_value;
end mcpdbwizard_ctx;
/

create or replace context mcp using mcpdbwizard.mcpdbwizard_ctx;

REM A package that does not compile is still CREATED, merely INVALID -- the usual cause would be
REM EXECUTE on DBMS_SESSION revoked from PUBLIC. Say so here, not at the first tool call.
declare
    v_invalid number;
begin
    select count(*) into v_invalid from dba_objects
     where owner = 'MCPDBWIZARD' and object_name = 'MCPDBWIZARD_CTX' and status <> 'VALID';
    if v_invalid > 0 then
        raise_application_error(-20903, 'MCPDBWIZARD.MCPDBWIZARD_CTX did not compile. Check that'
            || ' MCPDBWIZARD can execute DBMS_SESSION (granted to PUBLIC on a stock install).');
    end if;
    dbms_output.put_line('MCP context installed: MCP USING MCPDBWIZARD.MCPDBWIZARD_CTX.');
    dbms_output.put_line('Next: @grant.sql <ACCOUNT> for each account a generated server uses.');
end;
/

exit
