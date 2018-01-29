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

    public final static FunctionSignature FNS_CREATE = new FunctionSignature(
            QN_CREATE,
            "Create a memsort index in memory",
            new SequenceType[] {
                new FunctionParameterSequenceType("id", Type.STRING, Cardinality.EXACTLY_ONE, "The id for the index to be created"),
                new FunctionParameterSequenceType("input", Type.NODE, Cardinality.ZERO_OR_MORE, "Existing maps to merge to create a new map"),
                new FunctionParameterSequenceType("producer", Type.FUNCTION_REFERENCE, Cardinality.EXACTLY_ONE, "Functions which produces a memsort key for a given node")
            },
            new SequenceType(Type.EMPTY, Cardinality.EMPTY)
    );

    public final static FunctionSignature FNS_GET = new FunctionSignature(
            QN_GET,
            "Create a memsort index in memory",
            new SequenceType[] {
                    new FunctionParameterSequenceType("id", Type.STRING, Cardinality.EXACTLY_ONE, "The id for the index to be created"),
                    new FunctionParameterSequenceType("node", Type.NODE, Cardinality.ZERO_OR_ONE, "Existing maps to merge to create a new map")
            },
            new SequenceType(Type.INT, Cardinality.EXACTLY_ONE)
    );

    public SortFunctions(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        final String id = args[0].getStringValue();
        final SortedLookupTable table = SortModule.getOrCreate(id, context.getDefaultCollator());

        if (isCalledAs(QN_CREATE.getLocalPart())) {
            final FunctionReference producer = (FunctionReference) args[2].itemAt(0);

            table.add(args[1], producer);

            return Sequence.EMPTY_SEQUENCE;
        }
        if (isCalledAs(QN_GET.getLocalPart()) && !args[1].isEmpty()) {
            return new IntegerValue(table.get((NodeProxy) args[1].itemAt(0)));
        }
        return Sequence.EMPTY_SEQUENCE;
    }
}
