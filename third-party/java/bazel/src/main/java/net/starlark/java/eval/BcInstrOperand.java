package net.starlark.java.eval;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import net.starlark.java.syntax.Resolver;
import net.starlark.java.syntax.TokenKind;

/**
 * Describe instruction operands of the Starlark bytecode.
 *
 * <p>This code is used only when assertions are enabled, because proper instruction validation
 * might be expensive.
 */
class BcInstrOperand {
  /** Bytecode operand is an integer, stored in the bytecode. */
  static final Operands NUMBER = new NumberOperand();
  /**
   * Bytecode operand is logically a string, stored in the strings storage; the index is stored in
   * the bytecode.
   */
  static final Operands STRING = new StringOperand();
  /**
   * Bytecode operand is logically an object, stored in the strings storage; the index is stored in
   * the bytecode.
   */
  static final Operands OBJECT = new ObjectArg();

  /**
   * Bytecode operand is an input register. Note current implementation does not validate that it is
   * actually read, not write register, it is used mostly as a hint when bytecode is printed.
   *
   * <p>Operand of this type can be a non-negative integer for regular slot, or negative integer for
   * constants.
   */
  static final Operands IN_SLOT = new Register("r");
  /**
   * Bytecode operand is an output register.
   *
   * <p>The value of this operand must be a non-negative integer.
   */
  static final Operands OUT_SLOT = new Register("w");

  /** Bytecode operand is a fixed integer, storing {@link TokenKind}. */
  static final Operands TOKEN_KIND = new KindArg();

  private BcInstrOperand() {}

  /** Fixed of operands, e. g. a pair of operands used to describe a dict key and value. */
  static Operands fixed(Operands... operands) {
    return new FixedOperandsOpcode(operands);
  }

  /** Length-delimited operands, e. g. list constructor arguments. */
  static Operands lengthDelimited(Operands element) {
    return new LengthDelimited(element);
  }

  /** Operand is a fixed number storing the instruction pointer. */
  static Operands addr(String label) {
    return new AddrArg(label);
  }

  /**
   * Sequence of operands.
   *
   * <p>Note in Starlark bytecode, the opcode operands are variable length: The number of operands
   * depend not just on the opcode, but it is encoded in the previous operands. E. g. a list
   * constructor is encoded as a length delimited sequence of register operands.
   */
  abstract static class Operands {
    private Operands() {}

    /** This is low level operation, do not use directly. */
    abstract void visit(OpcodeVisitor visitor);

    /**
     * Get the number of integers occupied by this operands object at the given bytecode offset.
     *
     * <p>For example, length-delimited operand may return the different number of ints depending on
     * the actual bytecode.
     */
    int codeSize(Resolver.Function rfn, int[] text, List<String> strings, List<Object> constantRegs, int offset) {
      OpcodeVisitor visitor = new OpcodeVisitor(text, strings, constantRegs, offset,
          new OpcodeVisitorFunctionContext(rfn));
      visit(visitor);
      return visitor.ip - offset;
    }

    /** Get both instruction count for this operand and the string representation. */
    String toStringAndCount(
        int[] offsetInOut, int[] text, List<String> strings,
        List<Object> constantRegs, OpcodeVisitorFunctionContext fnCtx) {
      OpcodeVisitor visitor = new OpcodeVisitor(text, strings, constantRegs, offsetInOut[0],
          fnCtx);
      visit(visitor);
      offsetInOut[0] = visitor.ip;
      return visitor.sb.toString();
    }
  }

  static class OpcodeVisitorFunctionContext {
    private final ImmutableList<String> locals;
    private final ImmutableList<String> globals;
    private final ImmutableList<String> freeVars;

    public OpcodeVisitorFunctionContext(
        ImmutableList<String> locals, ImmutableList<String> globals,
        ImmutableList<String> freeVars) {
      this.locals = locals;
      this.globals = globals;
      this.freeVars = freeVars;
    }

    public OpcodeVisitorFunctionContext(Resolver.Function rfn) {
      this(rfn.getLocals().stream().map(Resolver.Binding::getName).collect(ImmutableList.toImmutableList()),
          rfn.getGlobals(), rfn.getFreeVars().stream().map(Resolver.Binding::getName).collect(ImmutableList.toImmutableList()));
    }
  }

  /** This class is package-private only because it is referenced from {@link Operands}. */
  private static class OpcodeVisitor {

    private final int[] text;
    private final List<String> strings;
    private final List<Object> constantRegs;
    private final OpcodeVisitorFunctionContext fnCtx;
    private int ip;
    private StringBuilder sb = new StringBuilder();

    private OpcodeVisitor(int[] text,
        List<String> strings, List<Object> constantRegs, int ip, OpcodeVisitorFunctionContext fnCtx) {
      this.fnCtx = fnCtx;
      this.text = text;
      this.strings = strings;
      this.constantRegs = constantRegs;
      this.ip = ip;
    }

    private void append(String s) {
      sb.append(s);
    }

    private int nextOperand() {
      return text[ip++];
    }
  }

  private static class NumberOperand extends Operands {
    @Override
    public void visit(OpcodeVisitor visitor) {
      visitor.append(Integer.toString(visitor.nextOperand()));
    }
  }

  private static class StringOperand extends Operands {
    @Override
    public void visit(OpcodeVisitor visitor) {
      visitor.append(visitor.strings.get(visitor.nextOperand()));
    }
  }

  private static class Register extends Operands {
    /** r or w, for read or write */
    private final String label;

    private Register(String label) {
      this.label = label;
    }

    @Override
    public void visit(OpcodeVisitor visitor) {
      int reg = visitor.nextOperand();
      Object valueToPrint;
      int flag = reg & BcSlot.MASK;
      int index = reg & ~BcSlot.MASK;
      switch (flag) {
        case BcSlot.LOCAL_FLAG:
          if (index < visitor.fnCtx.locals.size()) {
            // local
            valueToPrint = "l$" + index + ":" + visitor.fnCtx.locals.get(index);
          } else {
            // temporary
            valueToPrint = "s$" + index;
          }
          break;
        case BcSlot.GLOBAL_FLAG:
          valueToPrint = "g$" + index + ":" + visitor.fnCtx.globals.get(index);
          break;
        case BcSlot.CELL_FLAG:
          valueToPrint = "c$" + index + ":" + visitor.fnCtx.locals.get(index);
          break;
        case BcSlot.FREE_FLAG:
          valueToPrint = "f$" + index + ":" + visitor.fnCtx.freeVars.get(index);
          break;
        case BcSlot.CONST_FLAG:
          Object constant = visitor.constantRegs.get(index);
          valueToPrint = "=" + Starlark.repr(constant);
          break;
        case BcSlot.NULL_FLAG:
          valueToPrint = "=null";
          break;
        case BcSlot.ANY_FLAG:
          throw new IllegalStateException("any must not appear in bytecode");
        default:
          throw new IllegalStateException("wrong slot");
      }
      visitor.sb.append(label).append(valueToPrint);
    }
  }

  private static class KindArg extends Operands {
    @Override
    public void visit(OpcodeVisitor visitor) {
      visitor.sb.append(TokenKind.values()[visitor.nextOperand()]);
    }
  }

  private static class AddrArg extends Operands {
    private final String label;

    private AddrArg(String label) {
      this.label = label;
    }

    @Override
    public void visit(OpcodeVisitor visitor) {
      visitor.append(label + "=&" + visitor.nextOperand());
    }
  }

  private static class ObjectArg extends Operands {
    @Override
    public void visit(OpcodeVisitor visitor) {
      visitor.append("o" + visitor.nextOperand());
    }
  }

  private static class FixedOperandsOpcode extends Operands {
    private final Operands[] operands;

    private FixedOperandsOpcode(Operands[] operands) {
      this.operands = operands;
    }

    @Override
    public void visit(OpcodeVisitor visitor) {
      visitor.append("(");
      for (int i = 0; i < operands.length; i++) {
        if (i != 0) {
          visitor.sb.append(" ");
        }
        Operands operand = operands[i];
        operand.visit(visitor);
      }
      visitor.append(")");
    }
  }

  private static class LengthDelimited extends Operands {
    private final Operands element;

    private LengthDelimited(Operands element) {
      this.element = element;
    }

    @Override
    public void visit(OpcodeVisitor visitor) {
      visitor.append("[");
      int size = visitor.nextOperand();
      for (int i = 0; i != size; ++i) {
        if (i != 0) {
          visitor.append(" ");
        }
        element.visit(visitor);
      }
      visitor.append("]");
    }
  }
}