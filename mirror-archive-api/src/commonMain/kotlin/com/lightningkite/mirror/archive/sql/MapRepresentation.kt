package com.lightningkite.mirror.archive.sql



/*\

Map a HasId type to a set of tables
Most types will be a column, but some might be another table.
Partial tables?



If subtables are allowed, then:

Inserts may reach other tables, and modifications even ONLY those tables
A set of operations will need wrapping in a transaction

Looks like:

SET TRANSACTION
INSERT x INTO table
INSERT y, z, a INTO table_relation_other
COMMIT

Queries may require joins with other tables, and the row parser needs to accommodate for this

A, item1
A, item2
A, item3
B, NULL,
C, item4

Basically, if ID is already seen, skip columns until the relevant ones.  Null means empty list

This feature could be used for:

Multip


\*/