//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de>
//	* David Mueller <david.mueller@tcs.inf.tu-dresden.de>
//	
//------------------------------------------------------------------------------
//	
//	This file is part of the jhoafparser library, http://www.ltl2dstar.de/jhoafparser/
//
//	The jhoafparser library is free software; you can redistribute it and/or
//	modify it under the terms of the GNU Lesser General Public
//	License as published by the Free Software Foundation; either
//	version 2.1 of the License, or (at your option) any later version.
//	
//	The jhoafparser library is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//	Lesser General Public License for more details.
//	
//	You should have received a copy of the GNU Lesser General Public
//	License along with this library; if not, write to the Free Software
//	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
//	
//==============================================================================


package jhoafparser;

import java.util.BitSet;

public class BooleanExpression<Atoms extends Atom> {
	public enum Type {
		EXP_AND,
		EXP_OR,
		EXP_NOT,
		EXP_TRUE,
		EXP_FALSE,
		EXP_ATOM;
	};

	private Type type;
	private BooleanExpression<Atoms> left = null, right = null;
	private Atoms atom = null;

	public BooleanExpression<Atoms> getLeft() {return left;}
	public BooleanExpression<Atoms> getRight() {return right;}
	public Atoms getAtom() {return atom;}
	
	public BooleanExpression(Type type, BooleanExpression<Atoms> left, BooleanExpression<Atoms> right) {
		this.type=type;
		this.left=left;
		this.right=right;
	}
	
	public BooleanExpression(Atoms atom) {
		type = Type.EXP_ATOM;
		this.atom = atom;
	}
	
	@SuppressWarnings("unchecked")
	public BooleanExpression<Atoms> clone() {
		if (isAtom()) {
			return new BooleanExpression<Atoms>((Atoms) atom.clone());
		}

		BooleanExpression<Atoms> newLeft  = (left  != null ?  left.clone() : null);
		BooleanExpression<Atoms> newRight = (right != null ? right.clone() : null);

		return new BooleanExpression<Atoms>(type, newLeft, newRight);
	}

	public Type getType()
	{
		return type;
	}

	public boolean isAND() {return type==Type.EXP_AND;}
	public boolean isOR() {return type==Type.EXP_OR;}
	public boolean isNOT() {return type==Type.EXP_NOT;}
	public boolean isTRUE() {return type==Type.EXP_TRUE;}
	public boolean isFALSE() {return type==Type.EXP_FALSE;}
	public boolean isAtom() {return type==Type.EXP_ATOM;}

	public BooleanExpression<Atoms> and(BooleanExpression<Atoms> other) {
		return new BooleanExpression<Atoms>(Type.EXP_AND, this, other);
	}

	public BooleanExpression<Atoms> or(BooleanExpression<Atoms> other) {
		return new BooleanExpression<Atoms>(Type.EXP_OR, this, other);
	}

	public BooleanExpression<Atoms> not() {
		return new BooleanExpression<Atoms>(Type.EXP_NOT, this, null);
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static BooleanExpression TRUE() {
		return new BooleanExpression(Type.EXP_TRUE, null, null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static BooleanExpression FALSE() {
		return new BooleanExpression(Type.EXP_FALSE, null, null);
	}

	
	public static BitSet fromImplicit(long implicitIndex) {
		BitSet bs = new BitSet();
		
		int bit = 0;
		while (implicitIndex != 0) {
			bs.set(bit, (implicitIndex & 0x01)==1);
			implicitIndex = implicitIndex >> 2;
			bit++;
		}
		
		return bs;
	}
	
	public static BooleanExpression<AtomLabel> fromImplicit(long implicitIndex, int apSize) {
		BooleanExpression<AtomLabel> result = null;
		BitSet bs = fromImplicit(implicitIndex);
		
		for (int i = 0; i < apSize; i++) {
			BooleanExpression<AtomLabel> label = new BooleanExpression<AtomLabel>(AtomLabel.createAPIndex(new Integer(i)));
			if (bs.get(i) == false) {
				label = label.not();
			}
			
			if (result != null)
				result = result.and(label);
			else
				result = label;
		}
		
		return result;
	}
	
	public static boolean areSyntacticallyEqual(BooleanExpression<?> expr1, BooleanExpression<?> expr2)
	{
		if (expr1 == null || expr2 == null) return false;
		if (expr1.getType() != expr2.getType()) return false;
		
		switch (expr1.getType()) {
		case EXP_TRUE:
		case EXP_FALSE:
			return true;
		case EXP_AND:
		case EXP_OR:
			if (!areSyntacticallyEqual(expr1.getLeft(), expr2.getLeft())) return false;
			if (!areSyntacticallyEqual(expr1.getRight(), expr2.getRight())) return false;
		case EXP_NOT:
			if (!areSyntacticallyEqual(expr1.getLeft(), expr2.getLeft())) return false;
			return true;
		case EXP_ATOM:
			return expr1.getAtom().equals(expr2.getAtom());
		}
		throw new UnsupportedOperationException("Unknown operator in expression: "+expr1);
	}
	
	public boolean needsParentheses(Type enclosingType) {
		switch (type) {
		case EXP_ATOM:
		case EXP_TRUE:
		case EXP_FALSE:
			return false;
		case EXP_AND:
			if (enclosingType==Type.EXP_NOT) return true;
			if (enclosingType==Type.EXP_AND) return false;
			if (enclosingType==Type.EXP_OR) return false;
			break;
		case EXP_OR:
			if (enclosingType==Type.EXP_NOT) return true;
			if (enclosingType==Type.EXP_AND) return true;
			if (enclosingType==Type.EXP_OR) return false;
		case EXP_NOT:
			return false;
		}
		throw new RuntimeException("Unhandled type");
	}
	
	public String toStringInfix() {
		StringBuilder result = new StringBuilder();
		switch (type) {
		case EXP_AND: {
			boolean paren = left.needsParentheses(Type.EXP_AND);
			if (paren) result.append("(");
			result.append(left.toStringInfix());
			if (paren) result.append(")");
			result.append(" & ");
			
			paren = right.needsParentheses(Type.EXP_AND);
			if (paren) result.append("(");
			result.append(right.toStringInfix());
			if (paren) result.append(")");
			return result.toString();
		}
		case EXP_OR: {
			boolean paren = left.needsParentheses(Type.EXP_OR);
			if (paren) result.append("(");
			result.append(left.toStringInfix());
			if (paren) result.append(")");
			result.append(" | ");
			
			paren = right.needsParentheses(Type.EXP_OR);
			if (paren) result.append("(");
			result.append(right.toStringInfix());
			if (paren) result.append(")");
			return result.toString();
		}
		case EXP_NOT: {
			boolean paren = left.needsParentheses(Type.EXP_NOT);
			result.append("!");
			if (paren) result.append("(");
			result.append(left.toStringInfix());
			if (paren) result.append(")");
			return result.toString();
		}
		case EXP_TRUE: return "t";
		case EXP_FALSE: return "f";
		case EXP_ATOM: return getAtom().toString();
		}
		throw new RuntimeException("Unhandled BooleanExpression type: "+type);
	}

	public String toString() {
		return toStringInfix();
	}
}
