REM Copyright 2003-2026 ATB Consultancy Services Ltd
REM (formerly Orinda Software Ltd, Dublin, Ireland)
REM
REM grant.sql <ACCOUNT> -- let one database account set the MCP context.
REM
REM Run as a DBA after install.sql, once for each account a generated MCP server connects as:
REM
REM     sqlplus system@<db> @grant.sql HOTEL_APP
REM
REM Grant it ONLY to those accounts, not to PUBLIC. Any session holding this grant can set its
REM own MCP values, so an account that can also log in interactively can claim any customer --
REM which is no wider than what that account could already do, but there is no reason to extend
REM it to accounts that never run a generated server.

whenever sqlerror exit failure
set feedback off
set verify off

grant execute on mcpdbwizard.mcpdbwizard_ctx to &1;

prompt Granted EXECUTE on MCPDBWIZARD.MCPDBWIZARD_CTX to &1..

exit
