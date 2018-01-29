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
import org.exist.dom.persistent.NodeProxy;
import org.exist.numbering.NodeId;
import org.exist.xquery.Constants;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.*;

import java.util.*;

public class SortedLookupTable {

    private final String id;
    private final TreeMap<DocNodeId, Integer> table;
    private final Collator collator;

    public SortedLookupTable(final String id, final Collator collator) {
        this.id = id;
        table = new TreeMap<>();
        this.collator = collator;
    }

    public void add(final Sequence input, final FunctionReference call) throws XPathException {
        final List<SortItem> toBeSorted = new ArrayList<>();
        final Sequence[] params = new Sequence[1];
        for (final SequenceIterator nodesIter = input.iterate(); nodesIter.hasNext(); ) {
            final NodeValue nv = (NodeValue) nodesIter.nextItem();
            if (nv.getImplementationType() == NodeValue.IN_MEMORY_NODE)
                throw new XPathException("Cannot create order-index on an in-memory node");
            final NodeProxy node = (NodeProxy) nv;
            if (call != null) {
                // call the callback function to get value
                params[0] = node;
                final Sequence r = call.evalFunction(null, null, params);
                if (!r.isEmpty()) {
                    AtomicValue v = r.itemAt(0).atomize();
                    if (v.getType() == Type.UNTYPED_ATOMIC) {
                        v = v.convertTo(Type.STRING);
                    }
                    toBeSorted.add(new SortItem(new DocNodeId(node.getDoc().getDocId(), node.getNodeId()), v));
                }
            }
        }
        final SortItem[] sortArray = new SortItem[toBeSorted.size()];
        toBeSorted.toArray(sortArray);
        Arrays.sort(sortArray);

        for (int i = 0; i < sortArray.length; i++) {
            final SortItem item = sortArray[i];
            table.put(item.docNodeId, i);
        }
    }

    public int get(NodeProxy node) {
        return table.getOrDefault(new DocNodeId(node.getDoc().getDocId(), node.getNodeId()), -1);
    }

    private static class DocNodeId implements Comparable<DocNodeId> {

        private final NodeId nodeId;
        private final int docId;

        DocNodeId(final int docId, final NodeId nodeId) {
            this.docId = docId;
            this.nodeId = nodeId;
        }

        @Override
        public int compareTo(DocNodeId other) {
            if (other.docId == docId) {
                return nodeId.compareTo(other.nodeId);
            }
            if (docId > other.docId) {
                return Constants.SUPERIOR;
            }
            return Constants.INFERIOR;
        }
    }

    private class SortItem implements Comparable<SortItem> {

        private final DocNodeId docNodeId;
        private final AtomicValue key;

        public SortItem(DocNodeId docNodeId, AtomicValue key) {
            this.docNodeId = docNodeId;
            this.key = key;
        }

        @Override
        public int compareTo(SortItem other) {
            try {
                return key.compareTo(collator, other.key);
            } catch (XPathException e) {
                return Constants.INFERIOR;
            }
        }
    }
}
