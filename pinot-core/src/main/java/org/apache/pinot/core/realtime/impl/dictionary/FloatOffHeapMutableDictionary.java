/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.core.realtime.impl.dictionary;

import java.io.IOException;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.apache.pinot.core.io.readerwriter.PinotDataBufferMemoryManager;
import org.apache.pinot.core.io.readerwriter.impl.FixedByteSingleColumnSingleValueReaderWriter;


public class FloatOffHeapMutableDictionary extends BaseOffHeapMutableDictionary {
  private float _min = Float.MAX_VALUE;
  private float _max = Float.MIN_VALUE;

  private final FixedByteSingleColumnSingleValueReaderWriter _dictIdToValue;

  public FloatOffHeapMutableDictionary(int estimatedCardinality, int maxOverflowSize,
      PinotDataBufferMemoryManager memoryManager, String allocationContext) {
    super(estimatedCardinality, maxOverflowSize, memoryManager, allocationContext);
    final int initialEntryCount = nearestPowerOf2(estimatedCardinality);
    _dictIdToValue = new FixedByteSingleColumnSingleValueReaderWriter(initialEntryCount, Float.BYTES, memoryManager,
        allocationContext);
  }

  @Override
  public Float get(int dictId) {
    return getFloatValue(dictId);
  }

  @Override
  public int getIntValue(int dictId) {
    return (int) getDoubleValue(dictId);
  }

  @Override
  public long getLongValue(int dictId) {
    return (long) getDoubleValue(dictId);
  }

  @Override
  public float getFloatValue(int dictId) {
    return _dictIdToValue.getFloat(dictId);
  }

  @Override
  public double getDoubleValue(int dictId) {
    return getFloatValue(dictId);
  }

  @Override
  public int indexOf(Object rawValue) {
    if (rawValue instanceof String) {
      return getDictId(Float.valueOf((String) rawValue), null);
    } else {
      return getDictId(rawValue, null);
    }
  }

  @Override
  public void index(@Nonnull Object rawValue) {
    if (rawValue instanceof Float) {
      // Single value
      indexValue(rawValue, null);
      updateMinMax((Float) rawValue);
    } else {
      // Multi value
      Object[] values = (Object[]) rawValue;
      for (Object value : values) {
        indexValue(value, null);
        updateMinMax((Float) value);
      }
    }
  }

  @SuppressWarnings("Duplicates")
  @Override
  public boolean inRange(@Nonnull String lower, @Nonnull String upper, int dictIdToCompare, boolean includeLower,
      boolean includeUpper) {
    float lowerFloat = Float.parseFloat(lower);
    float upperFloat = Float.parseFloat(upper);
    float valueToCompare = (Float) get(dictIdToCompare);

    if (includeLower) {
      if (valueToCompare < lowerFloat) {
        return false;
      }
    } else {
      if (valueToCompare <= lowerFloat) {
        return false;
      }
    }

    if (includeUpper) {
      if (valueToCompare > upperFloat) {
        return false;
      }
    } else {
      if (valueToCompare >= upperFloat) {
        return false;
      }
    }

    return true;
  }

  @Nonnull
  @Override
  public Float getMinVal() {
    return _min;
  }

  @Nonnull
  @Override
  public Float getMaxVal() {
    return _max;
  }

  @Nonnull
  @Override
  @SuppressWarnings("Duplicates")
  public float[] getSortedValues() {
    int numValues = length();
    float[] sortedValues = new float[numValues];

    for (int i = 0; i < numValues; i++) {
      sortedValues[i] = (Float) get(i);
    }

    Arrays.sort(sortedValues);
    return sortedValues;
  }

  @Override
  public int getAvgValueSize() {
    return Float.BYTES;
  }

  @Override
  public int compare(int dictId1, int dictId2) {
    return Float.compare(getFloatValue(dictId1), getFloatValue(dictId2));
  }

  @Override
  protected void setRawValueAt(int dictId, Object value, byte[] serializedValue) {
    _dictIdToValue.setFloat(dictId, (Float) value);
  }

  @Override
  public void doClose()
      throws IOException {
    _dictIdToValue.close();
  }

  private void updateMinMax(float value) {
    if (value < _min) {
      _min = value;
    }
    if (value > _max) {
      _max = value;
    }
  }

  @Override
  public long getTotalOffHeapMemUsed() {
    return super.getTotalOffHeapMemUsed() + Float.BYTES * length();
  }
}
