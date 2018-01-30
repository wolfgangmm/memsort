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
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class SortedLookupTable {

    private final String id;
    private final TreeMap<DocNodeId, Integer> table;
    private final Collator collator;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    SortedLookupTable(final String id, final Collator collator) {
        this.id = id;
        table = new TreeMap<>();
        this.collator = collator;
    }

    int add(final Sequence input, final FunctionReference call) throws XPathException {
        lock.writeLock().lock();
        try {
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
            return sortArray.length;
        } finally {
            lock.writeLock().unlock();
        }
    }

    Sequence get(NodeProxy node) {
        lock.readLock().lock();
        try {
            final Integer ord = table.get(new DocNodeId(node.getDoc().getDocId(), node.getNodeId()));
            return ord == null ? Sequence.EMPTY_SEQUENCE : new IntegerValue(ord);
        } finally {
            lock.readLock().unlock();
        }
    }

    MapType sort(final Sequence input, final XQueryContext context) throws XPathException {
        final Sequence sorted = new ValueSequence(input.getItemCount());
        final Sequence unsorted = new ValueSequence(input.getItemCount());

        lock.readLock().lock();
        try {
            final List<ResultItem> toBeSorted = new ArrayList<>(input.getItemCount());
            for (final SequenceIterator i = input.unorderedIterator(); i.hasNext(); ) {
                final Item item = i.nextItem();
                if (Type.subTypeOf(item.getType(), Type.NODE) && ((NodeValue) item).getImplementationType() == NodeValue.PERSISTENT_NODE) {
                    final NodeProxy node = (NodeProxy) item;
                    final Integer ord = table.get(new DocNodeId(node.getDoc().getDocId(), node.getNodeId()));
                    if (ord != null) {
                        toBeSorted.add(new ResultItem(ord, node));
                    } else {
                        unsorted.add(node);
                    }
                } else {
                    unsorted.add(item);
                }
            }

            toBeSorted.sort(null);
            for (final ResultItem item : toBeSorted) {
                sorted.add(item.item);
            }
        } finally {
            lock.readLock().unlock();
        }
        final MapType map = new MapType(context);
        map.add(new StringValue("sorted"), sorted);
        map.add(new StringValue("unsorted"), unsorted);
        return map;
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

    private static class ResultItem implements Comparable<ResultItem> {

        private final int ord;
        private final Item item;

        ResultItem(int ord, Item item) {
            this.ord = ord;
            this.item = item;
        }

        @Override
        public int compareTo(ResultItem o) {
            return ord > o.ord ? Constants.SUPERIOR : (ord == o.ord ? Constants.EQUAL : Constants.INFERIOR);
        }
    }
}
