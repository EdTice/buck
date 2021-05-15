/*
 * Portions Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copyright 2016 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.facebook.buck.skylark.function.packages;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.Structure;
import net.starlark.java.syntax.Location;

/**
 * An abstract base class for Starlark values that have fields, have to_json and to_proto methods,
 * have an associated provider (type symbol), and may be returned as the result of analysis from one
 * target to another.
 *
 * <p>StructImpl does not specify how the fields are represented; subclasses must define {@code
 * getValue} and {@code getFieldNames}. For example, {@code NativeInfo} supplies fields from the
 * subclass's {@code StarlarkMethod(structField=true)} annotations, and {@code StarlarkInfo}
 * supplies fields from the map provided at its construction.
 *
 * <p>Two StructImpls are equivalent if they have the same provider and, for each field name
 * reported by {@code getFieldNames} their corresponding field values are equivalent, or accessing
 * them both returns an error.
 */
public abstract class StructImpl extends Structure implements Info, StructApi {

  private final Provider provider;
  private final Location location;

  /**
   * Constructs an {@link StructImpl}.
   *
   * @param provider the provider describing the type of this instance
   * @param location the Starlark location where this instance is created. If null, defaults to
   *     {@link Location#BUILTIN}.
   */
  protected StructImpl(Provider provider, @Nullable Location location) {
    this.provider = provider;
    this.location = location != null ? location : Location.BUILTIN;
  }

  @Override
  public Provider getProvider() {
    return provider;
  }

  @Override
  public Location getCreationLoc() {
    return location;
  }

  /**
   * Returns the result of {@link #getField(String)}, cast as the given type, throwing {@link
   * EvalException} if the cast fails.
   */
  public final <T> T getValue(String key, Class<T> type) throws EvalException {
    Object obj = getField(key);
    if (obj == null) {
      return null;
    }
    try {
      return type.cast(obj);
    } catch (
        @SuppressWarnings("UnusedException")
        ClassCastException unused) {
      throw Starlark.errorf(
          "for %s field, got %s, want %s", key, Starlark.type(obj), Starlark.classType(type));
    }
  }

  /**
   * Returns the error message format to use for unknown fields.
   *
   * <p>By default, it is the one specified by the provider.
   */
  protected String getErrorMessageFormatForUnknownField() {
    return getProvider().getErrorMessageFormatForUnknownField();
  }

  @Override
  public String getErrorMessageForUnknownField(String name) {
    String suffix =
        "Available attributes: "
            + Joiner.on(", ").join(Ordering.natural().sortedCopy(getFieldNames()));
    return String.format(getErrorMessageFormatForUnknownField(), name) + "\n" + suffix;
  }

  @Override
  public boolean equals(Object otherObject) {
    if (!(otherObject instanceof StructImpl)) {
      return false;
    }
    StructImpl other = (StructImpl) otherObject;
    if (this == other) {
      return true;
    }
    if (!this.getProvider().equals(other.getProvider())) {
      return false;
    }
    // Compare objects' fields and their values
    if (!this.getFieldNames().equals(other.getFieldNames())) {
      return false;
    }
    for (String field : getFieldNames()) {
      if (!Objects.equal(this.getValueOrNull(field), other.getValueOrNull(field))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    List<String> fields = new ArrayList<>(getFieldNames());
    Collections.sort(fields);
    List<Object> objectsToHash = new ArrayList<>();
    objectsToHash.add(getProvider());
    for (String field : fields) {
      objectsToHash.add(field);
      objectsToHash.add(getValueOrNull(field));
    }
    return Objects.hashCode(objectsToHash.toArray());
  }

  /**
   * Convert the object to string using Starlark syntax. The output tries to be reversible (but
   * there is no guarantee, it depends on the actual values).
   */
  @Override
  public void repr(Printer printer) {
    boolean first = true;
    printer.append("struct(");
    // Sort by key to ensure deterministic output.
    for (String fieldName : Ordering.natural().sortedCopy(getFieldNames())) {
      if (!first) {
        printer.append(", ");
      }
      first = false;
      printer.append(fieldName);
      printer.append(" = ");
      printer.repr(getValueOrNull(fieldName));
    }
    printer.append(")");
  }

  private Object getValueOrNull(String name) {
    try {
      return getField(name);
    } catch (EvalException e) {
      return null;
    }
  }

  @Override
  public String toJson() throws EvalException {
    StringBuilder sb = new StringBuilder();
    StructToJson.printJson(this, sb, "struct field", null);
    return sb.toString();
  }

  @Override
  public String toString() {
    return Starlark.repr(this);
  }
}
