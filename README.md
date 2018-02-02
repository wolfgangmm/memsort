# Pre-order and cache sequences for sorting
This module allows you to create a pre-sorted sequence in memory to speed up sorting via
"order by" in XQuery. Create an ordered sequence using `memsort:create` by mapping each element
to a sort key (an arbitrary atomic value). Calling `memsort:get` for the same element at any later
point will return an integer corresponding to the ordinal position of that element in the sorted
sequence (or empty if no mapping is found).

Example for creating a sorted sequence:

```xquery
declare namespace tei="http://www.tei-c.org/ns/1.0";

import module namespace memsort="http://exist-db.org/xquery/memsort" at "java:org.existdb.memsort.SortModule";

memsort:create(
    "record-date", 
    collection("/db/apps/my-data")//record,
    function($div) {
        $div/@date/xs:dateTime(.)
    }
)
```

Example for using the index in a query, assuming `$hits` are records returned by a lucene full text search:

```xquery
for $hit in $hits
order by memsort:get("record-date", $hit) ascending empty greatest, ft:score($hit) descending
return
    $hit
```

This will first sort by date, then by full text match score. Records without date will appear at the end (due to "empty greatest").

## Building

If you want to create an EXPath Package for the app, you can run:

```bash
$ mvn package
```

There will be a `.xar` file in the `target/` sub-folder.