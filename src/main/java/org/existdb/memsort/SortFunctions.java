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

import org.exist.dom.QName;
import org.exist.dom.persistent.NodeProxy;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

public class SortFunctions extends BasicFunction {

    private static final QName QN_CREATE = new QName("create", SortModule.NAMESPACE_URI, SortModule.PREFIX);
    private static final QName QN_GET = new QName("get", SortModule.NAMESPACE_URI, SortModule.PREFIX);
    private static final QName QN_CLEAR = new QName("clear", SortModule.NAMESPACE_URI, SortModule.PREFIX);
    private static final QName QN_SORT = new QName("sort", SortModule.NAMESPACE_URI, SortModule.PREFIX);

    public final static FunctionSignature FNS_CREATE = new FunctionSignature(
            QN_CREATE,
            "Create a memsort index in memory",
            new SequenceType[] {
                new FunctionParameterSequenceType("id", Type.STRING, Cardinality.EXACTLY_ONE, "The id for the index to be created"),
                new FunctionParameterSequenceType("input", Type.NODE, Cardinality.ZERO_OR_MORE, "The sequence of nodes to be sorted"),
                new FunctionParameterSequenceType("producer", Type.FUNCTION_REFERENCE, Cardinality.EXACTLY_ONE, "Function which produces a key for a given node")
            },
            new SequenceType(Type.EMPTY, Cardinality.EMPTY)
    );

    public final static FunctionSignature FNS_GET = new FunctionSignature(
            QN_GET,
            "Returns an integer number corresponding to the ordinal position of the indexed node in the sorted set or the empty sequence.",
            new SequenceType[] {
                    new FunctionParameterSequenceType("id", Type.STRING, Cardinality.EXACTLY_ONE, "The id for the index to be queried"),
                    new FunctionParameterSequenceType("node", Type.NODE, Cardinality.ZERO_OR_ONE, "Existing maps to merge to create a new map")
            },
            new SequenceType(Type.INTEGER, Cardinality.ZERO_OR_ONE)
    );

    public final static FunctionSignature FNS_SORT = new FunctionSignature(
            QN_SORT,
            "Sort the given sequence of nodes based on the sort index identified by $id. Returns a map with two entries: 'sorted' contains an " +
                    "ordered sequence of all nodes for which an entry in the sort index was found; 'unsorted' contains nodes for which no entry was found.",
            new SequenceType[] {
                    new FunctionParameterSequenceType("id", Type.STRING, Cardinality.EXACTLY_ONE, "The id for the index to be queried"),
                    new FunctionParameterSequenceType("node", Type.NODE, Cardinality.ZERO_OR_MORE, "Existing maps to merge to create a new map")
            },
            new SequenceType(Type.MAP, Cardinality.EXACTLY_ONE)
    );

    public final static FunctionSignature FNS_CLEAR = new FunctionSignature(
            QN_CLEAR,
            "Clear the memsort index identified by id",
            new SequenceType[] {
                    new FunctionParameterSequenceType("id", Type.STRING, Cardinality.EXACTLY_ONE, "The id for the index to be cleared")
            },
            new SequenceType(Type.EMPTY, Cardinality.EMPTY)
    );

    public SortFunctions(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        final String id = args[0].getStringValue();

        if (isCalledAs(QN_CREATE.getLocalPart())) {
            final FunctionReference producer = (FunctionReference) args[2].itemAt(0);
            SortModule.create(id, context.getDefaultCollator(), args[1], producer);
        }
        if (isCalledAs(QN_GET.getLocalPart()) && !args[1].isEmpty()) {
            return SortModule.get(id).get((NodeProxy) args[1].itemAt(0));
        }
        if (isCalledAs(QN_CLEAR.getLocalPart())) {
            SortModule.remove(id);
        }
        if (isCalledAs(QN_SORT.getLocalPart())) {
            return SortModule.get(id).sort(args[1], context);
        }
        return Sequence.EMPTY_SEQUENCE;
    }
}
