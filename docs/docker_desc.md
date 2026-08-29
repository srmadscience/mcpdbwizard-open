# Give the model an approved list of tools, not your Oracle password

MCPDBWizard reads your Oracle schema and runs an MCP server for the
objects and SQL statements you selected.

The MCP server does not expose a SQL prompt, and can't tell you
how to log into Oracle. *Only* the selected objects ands SQL
are usable.
# What you can expose
## SQL Statements

By this we mean 'carefully curated and tested SQL statements'.

For example - this statement takes a customer name as a parameter:

```sql
select * from complaints 
where customer_name = ? /* String */
order by complaint_date;
```
Under the covers everything is done with prepared statements, which is
efficient and rules out SQL injection.

## Tables

* You can specify 'Insert', 'Update' or 'Delete' for each one.
* For 'Select' you *must* use a primary key, unique key, foreign key, or indexed column. If you want to offer
  anything else, like "SELECT * FROM foo WHERE bar = ? /* String */ AND shoesize < 9 ;" you do it by creating a SQL statement above.

## PL/SQL procedures and functions
MCP DB Wizard can run **any** PL/SQL procedure or function, regardless of how
convoluted its parameters are.

## Sequences

If, for some reason, you need a sequence, we can do that as well...


# Why is different from an MCP server with a SQL prompt?

## Deterministic access
MCPDBWizard doesn't 'do' blind SQL access. It compiles Java for your selected objects.
It can *only* expose those objects. It has no code in it for anything you left out.

## Auditing

Auditing is integral to MCPDBWizard. You have a full audit trail, so you can sleep  safe at night.
## Arms length access:

* You connect to the MCP DB Wizard server using credentials it created and owns. Not Oracle ones.
* MCP DB Wizard doesn't have to log into the Oracle schema owner to work. It's quite happy
  with its Oracle user being granted READ or EXECUTE access instead.
* The exposed MCP server can't leak credentials, because it doesn't know who it's connected to.

#  Deploying and running MCPDBWizard

We have a [QuickStart](https://mcpdbwizard.com/docs/quickstart/) for this.

We also have the obligatory [demo application](https://github.com/srmadscience/mcpdbwizard-demo)

For further information see our [website](https://mcpdbwizard.com/)
