/*
 * eXist Open Source Native XML Database
 * Copyright (C) 2001-2017 The eXist Project
 * http://exist-db.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.existdb.memsort;

import com.ibm.icu.text.Collator;
import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.FunctionDef;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.Sequence;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class SortModule extends AbstractInternalModule {

    public final static String NAMESPACE_URI = "http://exist-db.org/xquery/memsort";

    public final static String PREFIX = "memsort";
    public final static String INCLUSION_DATE = "2017-12-10";
    public final static String RELEASED_IN_VERSION = "eXist-3.6.0";

    public static final FunctionDef[] functions = new FunctionDef[] {
        new FunctionDef(SortFunctions.FNS_CREATE, SortFunctions.class),
        new FunctionDef(SortFunctions.FNS_GET, SortFunctions.class),
        new FunctionDef(SortFunctions.FNS_CLEAR, SortFunctions.class),
        new FunctionDef(SortFunctions.FNS_SORT, SortFunctions.class)
    };

    protected final static ConcurrentSkipListMap<String, SortedLookupTable> map = new ConcurrentSkipListMap<>();

    protected final static SortedLookupTable create(final String id, final Collator collator, final Sequence input, final FunctionReference producer)
            throws XPathException {
        map.remove(id);
        final SortedLookupTable table = new SortedLookupTable(id, collator);
        table.add(input, producer);
        map.put(id, table);
        return table;
    }

    protected final static SortedLookupTable get(final String id) {
        return map.get(id);
    }

    protected final static void remove(final String id) {
        map.remove(id);
    }

    public SortModule(Map<String, List<?>> parameters) {
        super(functions, parameters);
    }

    @Override
    public String getNamespaceURI() {
        return NAMESPACE_URI;
    }

    @Override
    public String getDefaultPrefix() {
        return PREFIX;
    }

    @Override
    public String getDescription() {
        return "Create in memory lookup tables to be used for fast sorting";
    }

    @Override
    public String getReleaseVersion() {
        return RELEASED_IN_VERSION;
    }
}
