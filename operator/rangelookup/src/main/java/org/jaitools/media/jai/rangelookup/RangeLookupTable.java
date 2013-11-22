/* 
 *  Copyright (c) 2009-2013, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   
package org.jaitools.media.jai.rangelookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jaitools.numeric.NumberOperations;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.RangeUtils;


/**
 * A lookup table for the RangeLookup operation. 
 * It holds a collection of source value ranges, each mapped to a destination 
 * value. Instances of this class are immutable. 
 * <p>
 * Use the associated Builder class to construct a new table:
 * <pre><code>
 * // type parameters indicate lookup (source) and result 
 * // (destination) types
 * RangeLookupTable.Builder&lt;Double, Integer&gt; builder = RangeLookupTable.builder();
 *
 * // map all values &lt;= 0 to -1 and all values &gt; 0 to 1
 * builder.add(Range.create(Double.NEGATIVE_INFINITY, false, 0.0, true), -1)
 *        .add(Range.create(0.0, false, Double.POSITIVE_INFINITY, false), 1);
 * 
 * RangeLookupTable&lt;Double, Integer&gt; table = builder.build();
 * </code></pre>
 * 
 * @param <T> type of the lookup (source) value range
 * @param <U> type of the result (destination) value
 * 
 * @author Michael Bedward
 * @author Simone Giannecchini, GeoSolutions
 * @since 1.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RangeLookupTable<T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> {
    
    private final List<LookupItem<T, U>> items;
    
    /**
     * Private constructor called from the Builder's build method.
     */
    private RangeLookupTable(Builder builder) {
        this.items = new ArrayList<LookupItem<T, U>>(builder.items);
        
        // Sort the lookup items on the basis of their source ranges
        Collections.sort(this.items, new LookupItemComparator<T, U>());
    }

    /**
     * Finds the LookupItem containing the given source value.
     *
     * @param srcValue source image value
     * 
     * @return the LookupItem containing the source value or null if no matching
     *     item exists
     */
    public LookupItem<T, U> getLookupItem(T srcValue) {
        if (items.isEmpty()) {
            return null;
            
        } else {
            /*
             * Binary search for source value in items sorted by source range
             */
            int lo = 0;
            int hi = items.size() - 1;
            while (hi >= lo) {
                // update mid position, avoiding int overflow
                int mid = lo + (hi - lo) / 2;
                
                LookupItem<T, U> item = items.get(mid);
                Range<T> r = item.getRange();

                if (r.contains(srcValue)) {
                    return item;
                    
                } else if (!r.isMinNegInf() &&
                        NumberOperations.compare(srcValue, r.getMin()) <= 0) {
                    hi = mid - 1;
                
                } else {
                    lo = mid + 1;
                }
            }
            
            return null;  // no match
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (LookupItem item : items) {
            sb.append(item).append("; ");
        }
        return sb.toString();
    }
    
    
    /**
     * Package private method called by {@link RangeLookupRIF}.
     * 
     * @return an unmodifiable view of the lookup table items
     */
    List<LookupItem<T, U>> getItems() {
        return Collections.unmodifiableList(items);
    }

    private static class SingletonRangeLUT<T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> extends RangeLookupTable<T,U>{
        private final LookupItem<T, U> item;

        private SingletonRangeLUT(LookupItem<T, U> item) {
            super(new Builder());
            this.item = item;
        }

        @Override
        public LookupItem<T, U> getLookupItem(T srcValue) {
            if (item.getRange().contains(srcValue)) {
                return item;
            }else{
                return null;
            }
        }

        @Override
        public String toString() {
            return item.toString();
        }
    }

    /**
     * Builder to create an immutable lookup table.
     * 
     * @param <T> lookup (source) value type
     * @param <U> result (destination) valuetype
     */
    public static class Builder<T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> {
        private final List<LookupItem<T, U>> items;
        
        /**
         * Creates a new builder.
         */
        public Builder() {
            this.items = new ArrayList<LookupItem<T, U>>();
        }
        
        /**
         * Creates a new table that will hold the lookup items added to
         * this builder.
         * 
         * @return a new table instance
         */
        public RangeLookupTable<T, U> build() {
            if(items.size()==1) {
                return new SingletonRangeLUT<T, U>(items.get(0));
            }else{
                return new RangeLookupTable<T, U>(this);
            }
        }
        
        /**
         * Adds a new lookup defined by a range of source values mapping to a 
         * result value.
         *
         * A new lookup range that overlaps one or 
         * more previously set ranges will be truncated or split into 
         * non-overlapping intervals. For example, if the lookup [5, 10] => 1 
         * has previously been set, and a new lookup [0, 20] => 2 is added, 
         * then the following lookups will result:
         * <pre>
         *     [0, 5) => 2
         *     [5, 10] => 1
         *     (10, 20] => 2
         * </pre> 
         * Where a new range is completely overlapped by existing ranges it 
         * will be ignored.
         * <p> 
         * 
         * Note that it is possible to end up with unintended gaps in lookup 
         * coverage. If the first range in the above example had been the 
         * half-open interval (5, 10] rather than the closed interval [5, 10] 
         * then the following would have resulted:
         * <pre>
         *     [0, 5) => 2
         *     (5, 10] => 1
         *     (10, 20] => 2
         * </pre> In this case the value 5 would not be matched.
         *
         * @param srcRange the source value range
         * @param resultValue the destination value
         */
        public Builder add(Range<T> srcRange, U resultValue) {
            if (srcRange == null || resultValue == null) {
                throw new IllegalArgumentException("arguments must not be null");
            }

            // Check for overlap with existing ranges
            for (LookupItem item : items) {
                if (srcRange.intersects(item.getRange())) {
                    List<Range<T>> diffs = RangeUtils.subtract(item.getRange(), srcRange);
                    for (Range<T> diff : diffs) {
                        add(diff, resultValue);
                    }
                    return this;
                }
            }

            items.add(new LookupItem<T, U>(srcRange, resultValue));
            return this;
        }

    }
    
}
