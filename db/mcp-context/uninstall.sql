REM Copyright 2003-2026 ATB Consultancy Services Ltd
REM (formerly Orinda Software Ltd, Dublin, Ireland)
REM
REM uninstall.sql -- remove what install.sql created: the MCP context and the MCPDBWIZARD user
REM (with its package and every grant on it). Run as a DBA.
REM
REM A generated server whose config declares URL context parameters will then refuse to start,
REM saying the MCP context is not installed. Remove the parameters from the config first.
REM
REM The context is dropped only if it is still bound to our package, for the same reason
REM install.sql will not replace one that is not.

whenever sqlerror exit failure
set serveroutput on
set feedback off

declare
    v_count number;
begin
    select count(*) into v_count from dba_context
     where namespace = 'MCP' and schema = 'MCPDBWIZARD' and package = 'MCPDBWIZARD_CTX';
    if v_count > 0 then
        execute immediate 'drop context mcp';
        dbms_output.put_line('Dropped context MCP.');
    end if;
    select count(*) into v_count from dba_users where username = 'MCPDBWIZARD';
    if v_count > 0 then
        execute immediate 'drop user mcpdbwizard cascade';
        dbms_output.put_line('Dropped user MCPDBWIZARD.');
    end if;
end;
/

exit
