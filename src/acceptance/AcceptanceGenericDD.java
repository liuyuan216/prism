package acceptance;


import common.IterableBitSet;
import acceptance.AcceptanceGeneric.ElementType;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;


/**
 * A generic acceptance condition (based on JDD state sets).
 * This is an AST of a boolean formula (conjunction and disjunction) over
 * atoms of the form Inf(states), Inf(!states), Fin(states) and Fin(!states).
 * <br>
 * Semantics:
 *  Inf(states)  <=> G F states
 *  Inf(!states) <=> G F !states
 *  Fin(states)  <=> F G !states
 *  Fin(!states) <=> F G states
 */
public class AcceptanceGenericDD implements AcceptanceOmegaDD {

	private ElementType kind;
	private AcceptanceGenericDD left = null;
	private AcceptanceGenericDD right = null;
	private JDDNode states = null;

	public AcceptanceGenericDD(AcceptanceGeneric acceptance, JDDVars ddRowVars)
	{
		switch(acceptance.getKind()) {
			case AND:
			case OR:
				kind = acceptance.getKind();
				left = (AcceptanceGenericDD) acceptance.getLeft().toAcceptanceDD(ddRowVars);
				right = (AcceptanceGenericDD) acceptance.getRight().toAcceptanceDD(ddRowVars);
				return;
			case TRUE:
				kind = ElementType.TRUE;
				return;
			case FALSE:
				kind = ElementType.FALSE;
				return;
			case INF:
			case INF_NOT:
			case FIN:
			case FIN_NOT:
				kind = acceptance.getKind();
				states = JDD.Constant(0);
				for (int i : IterableBitSet.getSetBits(acceptance.getStates())) {
					states = JDD.SetVectorElement(states, ddRowVars, i, 1.0);
				}
				return;
		}
		throw new UnsupportedOperationException("Unsupported operatator in generic acceptance condition");
	}

	/** Get the ElementType of this AST element */
	public ElementType getKind()
	{
		return kind;
	}

	/** Get the left child of this AST element */
	public AcceptanceGenericDD getLeft()
	{
		return left;
	}

	/** Get the right child of this AST element */
	public AcceptanceGenericDD getRight()
	{
		return right;
	}

	/** Get a referenced copy of the state sets (if kind is one of INF, FIN, INF_NOT, FIN_NOT).
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getStates()
	{
		if (states != null) {
			JDD.Ref(states);
		}
		return states;
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc)
	{
		switch(kind) {
		case TRUE: 
			return true;
		case FALSE: 
			return false;
		case AND: 
			return left.isBSCCAccepting(bscc) && right.isBSCCAccepting(bscc);
		case OR: 
			return left.isBSCCAccepting(bscc) || right.isBSCCAccepting(bscc);
		case INF:
			// bscc |= G F states?
			// there exists a state in bscc and states
			return JDD.AreInterecting(states, bscc);
		case INF_NOT:
			// bscc_state |= G F !states?
			// the BSCC intersects Not(states)
			JDD.Ref(states);
			return JDD.AreInterecting(JDD.Not(states), bscc);
		case FIN:
			// bscc |= F G !states?
			// the BSCC consists only of !states
			return !JDD.AreInterecting(states, bscc);
		case FIN_NOT:
			// bscc |= F G states?
			// the BSCC consists entirely of states
			JDD.Ref(states);
			return !JDD.AreInterecting(JDD.Not(states), bscc);
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance expression");
	}

	@Override
	public String getSizeStatistics() {
		return "generic acceptance with " + countAcceptanceSets() + " acceptance sets";
	}

	@Override
	public AcceptanceType getType() {
		return AcceptanceType.GENERIC;
	}

	@Override
	public String getTypeAbbreviated() {
		return "";
	}

	@Override
	public String getTypeName() {
		return "generic";
	}

	@Override
	public void clear() {
		switch (kind) {
			case TRUE:
			case FALSE:
				return;
			case AND:
			case OR:
				left.clear();
				right.clear();
				return;
			case INF_NOT:
			case FIN:
			case FIN_NOT:
			case INF:
				if (states != null) JDD.Deref(states);
				states = null;
				return;
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance expression");
	}

	/** Count the number of state sets in this acceptance condition */
	public int countAcceptanceSets() {
		switch(kind) {
			case FALSE:
			case TRUE:
				return 0;
			case INF:
			case FIN: 
			case INF_NOT:
			case FIN_NOT:
				return 1;
			case OR:
			case AND: 
				return left.countAcceptanceSets() + right.countAcceptanceSets();
		}
		throw new UnsupportedOperationException("Unsupported operator in generic acceptance expression");
	}

}
