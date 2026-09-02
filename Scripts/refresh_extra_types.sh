#!/bin/sh
# Recreate the generated shadow SQL types from the regenerated extraObjects.sql files.
#
# TWO THINGS THIS GETS RIGHT, BOTH LEARNED THE HARD WAY:
#  1. The password comes from the propfile's PASS=, never from USER=. Oracle passwords are
#     CASE SENSITIVE, so a schema's own name reused as its password is a wrong password, and
#     repeating it across several propfiles locked the account (ORA-28000). The logs then show
#     only the lock, never the ORA-01017 that caused it.
#  2. One probe connection per account BEFORE any loop, and skip the account if it fails. A
#     retry loop against a wrong password is what turns a typo into a locked schema.
ALIAS=$1
if [ -z "$ALIAS" ]; then
    echo "usage: Scripts/refresh_extra_types.sh <TNS_ALIAS>" >&2
    echo "  Recreates the generated shadow SQL types in every schema a propfile names," >&2
    echo "  from the extraObjects.sql of the CURRENT target/regen tree. Run it AFTER a" >&2
    echo "  regen and BEFORE the tests whenever a record-collection type changes shape." >&2
    exit 2
fi
APP=$(cd "$(dirname "$0")/.." && pwd)
SQLPLUS=${SQLPLUS_HOME:-$HOME/opt/instantclient_23_26}/sqlplus
export TNS_ADMIN=$APP/Scripts/tns

: > /tmp/typemap.txt
for pb in $APP/Propfiles/*.pb2; do
    name=$(basename "$pb" .pb2)
    user=$(grep -m1 '^USER=' "$pb" | cut -d= -f2 | tr -d '\r')
    pass=$(grep -m1 '^PASS=' "$pb" | cut -d= -f2 | tr -d '\r')
    tree=$(find $APP/target/regen/Src/$name -name extraObjects.sql 2>/dev/null | head -1)
    [ -z "$tree" ] && continue
    echo "$user|$pass|$tree" >> /tmp/typemap.txt
done

for acct in $(cut -d'|' -f1,2 /tmp/typemap.txt | sort -u); do
    user=$(echo "$acct" | cut -d'|' -f1); pass=$(echo "$acct" | cut -d'|' -f2)

    # ---- ONE probe. If it fails, skip: never retry a credential. ----
    probe=$($SQLPLUS -s /nolog <<SQL 2>&1
connect $user/$pass@$ALIAS
set feedback off
set pagesize 0
select 'OK' from dual;
exit
SQL
)
    case "$probe" in
        *OK*) : ;;
        *) echo "$user: SKIPPED -- $(echo "$probe" | grep -oE 'ORA-[0-9]+' | head -1)"; continue ;;
    esac

    # Drop exactly the types these files NAME, arrays first. Do NOT match on a prefix: the
    # prefix is the config's DEFAULT_TEMP_PREFIX and only DEFAULTS to OSOFT -- some propfiles
    # use OB. Matching 'OSOFT%' left those undropped, their CREATE OR REPLACE then failed with
    # ORA-02303, and the stale type met freshly generated Java as
    # "ORA-17049: Inconsistent Java and SQL object types", which names neither cause.
    grep "^$user|" /tmp/typemap.txt | cut -d'|' -f3 | xargs cat 2>/dev/null \
      | awk '/^CREATE OR REPLACE TYPE/ { print $5 }' | sort -u > /tmp/typenames_$user.txt
    : > /tmp/dropcmds_$user.sql
    grep '_A$' /tmp/typenames_$user.txt | sed 's/^/DROP TYPE /; s/$/ FORCE;/' >> /tmp/dropcmds_$user.sql
    grep '_T$' /tmp/typenames_$user.txt | sed 's/^/DROP TYPE /; s/$/ FORCE;/' >> /tmp/dropcmds_$user.sql

    $SQLPLUS -s /nolog > /tmp/drop_$user.log 2>&1 <<SQL
connect $user/$pass@$ALIAS
set feedback off
@/tmp/dropcmds_$user.sql
exit
SQL
    dropped=$(grep -c "^DROP TYPE" /tmp/dropcmds_$user.sql 2>/dev/null)

    : > /tmp/create_$user.log
    # DEDUPE across the schema's trees before creating anything. Several propfiles share one
    # schema and each emits the same shape, so replaying the files in turn makes the second one
    # CREATE OR REPLACE a _T whose _A the first one has already created -- ORA-02303 again, this
    # time on the way IN. Emitting each type once avoids it without any drop/create ordering.
    #
    # And add the '/' terminators: extraObjects.sql has none, so SQL*Plus buffers every
    # CREATE TYPE and executes nothing at all, silently.
    grep "^$user|" /tmp/typemap.txt | cut -d'|' -f3 | xargs cat 2>/dev/null \
      | awk '
          /^CREATE OR REPLACE TYPE/ { name=$5; skip=(seen[name]++ > 0) }
          { if (!skip) print }
          /;[ ]*$/ { if (!skip) print "/"; skip=0 }
        ' > /tmp/eo_$user.sql
    $SQLPLUS -s /nolog >> /tmp/create_$user.log 2>&1 <<SQL
connect $user/$pass@$ALIAS
@/tmp/eo_$user.sql
exit
SQL
    made=$($SQLPLUS -s /nolog <<SQL 2>&1 | tr -d ' \n'
connect $user/$pass@$ALIAS
set feedback off
set pagesize 0
select count(*)||' types, '||sum(case when status<>'VALID' then 1 else 0 end)||' invalid' from user_objects where object_type='TYPE';
exit
SQL
)
    echo "$user: dropped $dropped, now $made"
done
