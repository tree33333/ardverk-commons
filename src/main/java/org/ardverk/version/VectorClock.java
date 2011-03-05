/*
 * Copyright 2010-2011 Roger Kapsi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ardverk.version;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VectorClock<K> implements Version<VectorClock<K>>, Serializable {
    
    private static final long serialVersionUID = 8061383748163285648L;

    public static <K> VectorClock<K> create() {
        return new VectorClock<K>(System.currentTimeMillis(), 
                Collections.<K, Value>emptyMap());
    }
    
    public static <K> VectorClock<K> create(K key) {
        VectorClock<K> clock = create();
        return clock.append(key);
    }
    
    private final long creationTime;
    
    private final Map<? extends K, ? extends Value> map;
    
    private VectorClock(long creationTime, 
            Map<? extends K, ? extends Value> map) {
        this.creationTime = creationTime;
        this.map = map;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public VectorClock<K> append(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key=null");
        }
        
        Map<K, Value> dst = new HashMap<K, Value>(map);
        
        Value value = dst.get(key);
        if (value == null) {
            value = Value.INIT;
        }
        
        dst.put(key, value.increment());
        return new VectorClock<K>(creationTime, dst);
    }
    
    public boolean contains(K key) {
        return map.containsKey(key);
    }
    
    public int size() {
        return map.size();
    }
    
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    public Value get(K key) {
        return map.get(key);
    }
    
    public Set<? extends Map.Entry<? extends K, ? extends Value>> entrySet() {
        return map.entrySet();
    }
    
    public Set<? extends K> keySet() {
        return map.keySet();
    }
    
    public Collection<? extends Value> values() {
        return map.values();
    }
    
    @Override
    public Occured compareTo(VectorClock<K> other) {
        boolean bigger1 = false;
        boolean bigger2 = false;
        
        int size1 = size();
        int size2 = other.size();
        
        if (size1 < size2) {
            bigger2 = true;
            
        } else if (size2 < size1) {
            bigger1 = true;
            
        } else {
            for (Map.Entry<? extends K, ? extends Value> entry : entrySet()) {
                Value value = other.get(entry.getKey());
                if (value == null) {
                    bigger1 = true;
                    
                    for (K key : other.keySet()) {
                        if (!contains(key)) {
                            bigger2 = true;
                            break;
                        }
                    }
                    
                    break;
                }
                
                int diff = entry.getValue().compareTo(value);
                if (diff < 0) {
                    bigger2 = true;
                    break;
                } else if (0 < diff) {
                    bigger1 = true;
                    break;
                }
            }
        }
        
        if (!bigger1 && !bigger2) {
            // Both VectorClocks are the same.
            return Occured.IDENTICAL;
        } else if (bigger1 && !bigger2) {
            // This VectorClock represents an event that
            // happened after the other other one.
            return Occured.AFTER;
        } else if (!bigger1 && bigger2) {
            // This VectorClock represents an event that
            // happened before the other other one.
            return Occured.BEFORE;
        }
        
        // Conflict!!! ;-(
        return Occured.CONCURRENTLY;
    }

    public VectorClock<K> merge(VectorClock<? extends K> other) {
        Map<K, Value> dst = new HashMap<K, Value>(map);
        
        for (Map.Entry<? extends K, ? extends Value> entry : other.entrySet()) {
            K key = entry.getKey();
            Value value = entry.getValue();
            
            Value existing = dst.get(key);
            if (existing != null) {
                value = existing.merge(value);
            }
            
            dst.put(key, value);
        }
        
        long creationTime = Math.min(getCreationTime(), other.getCreationTime());
        return new VectorClock<K>(creationTime, dst);
    }
    
    @Override
    public String toString() {
        return creationTime + ", " + map.toString();
    }
    
    public static class Value implements Comparable<Value>, Serializable {
        
        private static final long serialVersionUID = -1915316363583960219L;

        static final Value INIT = new Value(0);
        
        private final long creationTime = System.currentTimeMillis();
        
        private final int value;
        
        private Value(int value) {
            this.value = value;
        }
        
        public long getCreationTime() {
            return creationTime;
        }
        
        public int get() {
            return value;
        }
        
        Value increment() {
            return new Value(value + 1);
        }

        Value merge(Value other) {
            return new Value(Math.max(value, other.value));
        }
        
        @Override
        public int compareTo(Value o) {
            return value - o.value;
        }
        
        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }
}
