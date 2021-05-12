package net.starlark.java.eval;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.starlark.java.spelling.SpellChecker;

class BuiltinFunctionLinkedError {
  private final BuiltinFunction fn;
  private final MethodDescriptor desc;

  public BuiltinFunctionLinkedError(BuiltinFunction fn) {
    this.fn = fn;
    this.desc = fn.desc;
  }

  static String plural(int n) {
    return n == 1 ? "" : "s";
  }

  EvalException error(
      StarlarkCallableLinkSig linkSig,
      Object[] args,
      @Nullable Sequence<?> starArgs,
      @Nullable Dict<Object, Object> starStarArgs)
      throws EvalException {

    StarlarkCallableUtils.FoldedArgs foldedArgs =
        StarlarkCallableUtils.foldArgs(linkSig, args, starArgs, starStarArgs);

    ParamDescriptor[] parameters = desc.getParameters();

    int numPositionals = foldedArgs.positional.size();

    // Allocate argument vector.
    Object[] vector = new Object[parameters.length];

    // positional arguments
    int paramIndex = 0;
    int argIndex = 0;
    for (; argIndex < numPositionals && paramIndex < parameters.length; paramIndex++) {
      ParamDescriptor param = parameters[paramIndex];
      if (!param.isPositional()) {
        break;
      }

      Object value = foldedArgs.positional.get(argIndex++);
      vector[paramIndex] = value;
    }

    // *args
    if (!desc.acceptsExtraArgs()) {
      if (argIndex < numPositionals) {
        if (argIndex == 0) {
          return Starlark.errorf("%s() got unexpected positional argument", fn.getName());
        } else {
          return Starlark.errorf(
              "%s() accepts no more than %d positional argument%s but got %d",
              fn.getName(), argIndex, plural(argIndex), numPositionals);
        }
      }
    }

    // named arguments

    for (int i = 0; i != foldedArgs.named.size(); i += 2) {
      String key = (String) foldedArgs.named.get(i);
      Object value = foldedArgs.named.get(i + 1);
      handleNamedArg(vector, key, value);
    }

    String collisionKey = StarlarkCallableUtils.hasKeywordCollision(linkSig, starStarArgs);
    if (collisionKey != null) {
      return Starlark.errorf(
          "%s() got multiple values for keyword argument '%s'", fn.getName(), collisionKey);
    }

    // Set default values for missing parameters,
    // and report any that are still missing.
    MissingParams missing = null;
    for (int i = 0; i < parameters.length; i++) {
      if (vector[i] == null) {
        ParamDescriptor param = parameters[i];
        vector[i] = param.getDefaultValue();
        if (vector[i] == null) {
          if (missing == null) {
            missing = new MissingParams(fn.getName());
          }
          if (param.isPositional()) {
            missing.addPositional(param.getName());
          } else {
            missing.addNamed(param.getName());
          }
        }
      }
    }

    if (missing != null) {
      return missing.error();
    }

    throw new AssertionError(
        String.format(
            "this code is meant to be called when there's linked errors, but no error found;"
                + " fn: %s, linkSig: %s, starArgs: %s, named starStarArgs: %s",
            fn.getName(),
            linkSig,
            starArgs != null ? starArgs.size() : 0,
            starStarArgs != null ? starStarArgs.keySet() : Collections.emptySet()));
  }

  private void handleNamedArg(Object[] vector, String key, Object value) throws EvalException {
    ParamDescriptor[] parameters = desc.getParameters();

    // look up parameter
    int index = desc.getParameterIndex(key);
    // unknown parameter?
    if (index < 0) {
      // spill to **kwargs
      if (!desc.acceptsExtraKwargs()) {
        List<String> allNames =
            Arrays.stream(parameters)
                .map(ParamDescriptor::getName)
                .collect(ImmutableList.toImmutableList());
        throw Starlark.errorf(
            "%s() got unexpected keyword argument '%s'%s",
            fn.getName(), key, SpellChecker.didYouMean(key, allNames));
      }
      return;
    }

    // positional-only param?
    if (!parameters[index].isNamed()) {
      // spill to **kwargs
      if (!desc.acceptsExtraKwargs()) {
        throw Starlark.errorf(
            "%s() got named argument for positional-only parameter '%s'", fn.getName(), key);
      }
      return;
    }

    // duplicate?
    if (vector[index] != null) {
      throw Starlark.errorf("%s() got multiple values for argument '%s'", fn.getName(), key);
    }

    vector[index] = value;
  }

  static EvalException error(
      BuiltinFunction fn,
      StarlarkCallableLinkSig linkSig,
      Object[] args,
      @Nullable Sequence<?> starArgs,
      @Nullable Dict<Object, Object> starStarArgs)
      throws EvalException {
    return new BuiltinFunctionLinkedError(fn).error(linkSig, args, starArgs, starStarArgs);
  }
}