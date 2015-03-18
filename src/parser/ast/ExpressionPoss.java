//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package parser.ast;

import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
//the whole java is added by liuyuan 
public class ExpressionPoss extends Expression implements ExpressionQuant
{
	RelOp relOp = null;
	Expression poss = null;
	Expression expression = null;
	// Note: this "old-style" filter is just for display purposes
	// The parser creates an (invisible) new-style filter around this expression
	Filter filter = null;

	// Constructors

	public ExpressionPoss()
	{
	}

	public ExpressionPoss(Expression e, String r, Expression p)
	{
		expression = e;
		relOp = RelOp.parseSymbol(r);
		poss = p;
	}

	// Set methods

	public void setRelOp(RelOp relOp)
	{
		this.relOp = relOp;
	}

	public void setRelOp(String r)
	{
		relOp = RelOp.parseSymbol(r);
	}

	public void setPoss(Expression p)
	{
		poss = p;
	}

	public void setExpression(Expression e)
	{
		expression = e;
	}

	public void setFilter(Filter f)
	{
		filter = f;
	}

	// Get methods

	public RelOp getRelOp()
	{
		return relOp;
	}

	public Expression getPoss()
	{
		return poss;
	}

	public Expression getExpression()
	{
		return expression;
	}

	public Filter getFilter()
	{
		return filter;
	}

	/**
	 * Get a string describing the type of P operator, e.g. "P=?" or "P<p".
	 */
	public String getTypeOfPOperator()
	{
		String s = "";
		s += "P" + relOp;
		s += (poss == null) ? "?" : "p";
		return s;
	}

	/**
	 * Get info about the operator and bound.
	 * Does some checks, e.g., throws an exception if possability is out of range.
	 * @param constantValues Values for constants in order to evaluate any bound
	 */
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException
	{
		if (poss != null) {
			double bound = poss.evaluateDouble(constantValues);
			if (bound < 0 || bound > 1)
				throw new PrismException("Invalid possability bound " + bound + " in P operator");
			return new OpRelOpBound("P", relOp, bound);
		} else {
			return new OpRelOpBound("P", relOp, null);
		}
	}
	
	// Methods required for Expression:

	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}
	
	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a P operator without a model");
	}

	/**
	  * Get "name" of the result of this expression (used for y-axis of any graphs plotted)
	  */
	public String getResultName()
	{
		if (poss != null)
			return "Result";
		else if (relOp == RelOp.MIN)
			return "Minimum possibility";
		else if (relOp == RelOp.MAX)
			return "Maximum possibility";
		else
			return "possibility";
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for ASTElement:

	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";

		s += "P" + relOp;
		s += (poss == null) ? "?" : poss.toString();
		s += " [ " + expression;
		if (filter != null)
			s += " " + filter;
		s += " ]";

		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		ExpressionPoss expr = new ExpressionPoss();
		expr.setExpression(expression == null ? null : expression.deepCopy());
		expr.setRelOp(relOp);
		expr.setPoss(poss == null ? null : poss.deepCopy());
		expr.setFilter(filter == null ? null : (Filter)filter.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
}

//------------------------------------------------------------------------------
