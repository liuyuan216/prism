package jhoafparser;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

public class HOAIntermediateCheckValidity extends HOAIntermediate
{
	protected HashSet<String> supportedMiscHeaders = null;
	
	// the header names that have occurred so far in the automaton definition
	protected HashSet<String> usedHeaders = new HashSet<String>();
	
	// the number of states that have been specified in the header (optional)
	protected Integer numberOfStates = null;
	
	// the number of acceptance sets (mandatory)
	protected Integer numberOfAcceptanceSets = null;

	// the set of states for which addState has been called
	protected BitSet statesWithDefinition = new BitSet();

	// the set of states that occur as target states of some transition
	protected BitSet targetStatesOfTransitions = new BitSet();
	
	// the set of states that are start states
	protected BitSet startStates = new BitSet();

	protected HashSet<String> aliases = new HashSet<String>();
	protected BitSet apsInAliases = new BitSet();

	// the number of atomic propositions
	protected Integer numberOfAPs = null;

	// the acc-name and extra info
	protected String accName = null;
	protected List<Object> accExtraInfo = null;
	
	// the acceptance condition
	protected BooleanExpression<AtomAcceptance> acceptance = null;
	
	protected int currentState = 0;
	protected boolean currentStateHasStateLabel = false;
	protected boolean currentStateHasTransitionLabel = false;
	protected boolean currentStateHasImplicitEdge = false;
	protected boolean currentStateHasExplicitEdge = false;

	protected ImplicitEdgeHelper implicitEdgeHelper = null;

	public HOAIntermediateCheckValidity(HOAConsumer next)
	{
		super(next);
	}
	
	public void setSupportedMiscHeaders(HashSet<String> supportedMiscHeaders)
	{
		this.supportedMiscHeaders = supportedMiscHeaders;
	}
	
	public void addSupportedMiscHeader(String supportedMiscHeader)
	{
		if (supportedMiscHeaders == null)
			supportedMiscHeaders = new HashSet<String>();
		
		supportedMiscHeaders.add(supportedMiscHeader); 
	}
	
	@Override
	public boolean parserResolvesAliases()
	{
		return next.parserResolvesAliases();
	}

	@Override
	public void notifyHeaderStart(String version) throws HOAConsumerException
	{
		if (!version.equals("v1")) {
			throw new HOAConsumerException("Can only parse HOA format v1");
		}
		next.notifyHeaderStart(version);
	}

	@Override
	public void setNumberOfStates(int numberOfStates) throws HOAConsumerException
	{
		headerAtMostOnce("States");
		this.numberOfStates = numberOfStates;
		next.setNumberOfStates(numberOfStates);
	}

	@Override
	public void addStartStates(List<Integer> stateConjunction) throws HOAConsumerException
	{
		for (Integer state : stateConjunction) {
			checkStateIndex(state);
			startStates.set(state);
		}
		next.addStartStates(stateConjunction);
	}

	@Override
	public void addAlias(String name, BooleanExpression<AtomLabel> labelExpr) throws HOAConsumerException
	{
		usedHeaders.add("Alias");

		checkAliasesAreDefined(labelExpr);
		aliases.add(name);

		gatherLabels(labelExpr, apsInAliases);

		next.addAlias(name, labelExpr);
	}

	@Override
	public void setAPs(List<String> aps) throws HOAConsumerException
	{
		headerAtMostOnce("AP");

		numberOfAPs = aps.size();

		next.setAPs(aps);
	}

	@Override
	public void setAcceptanceCondition(int numberOfSets, BooleanExpression<AtomAcceptance> accExpr) throws HOAConsumerException
	{
		headerAtMostOnce("Acceptance");
		numberOfAcceptanceSets = numberOfSets;

		checkAcceptanceCondition(accExpr);
		acceptance = accExpr.clone();

		next.setAcceptanceCondition(numberOfSets, accExpr);
	}

	@Override
	public void provideAcceptanceName(String name, List<Object> extraInfo) throws HOAConsumerException
	{
		headerAtMostOnce("acc-name");
		accName = name;
		
		// TODO: clone
		accExtraInfo = extraInfo;
		next.provideAcceptanceName(name,  extraInfo);
	}

	@Override
	public void setName(String name) throws HOAConsumerException
	{
		headerAtMostOnce("name");
		next.setName(name);
	}

	@Override
	public void setTool(String name, String version) throws HOAConsumerException
	{
		headerAtMostOnce("tool");
		next.setTool(name, version);
	}

	@Override
	public void addProperties(List<String> properties) throws HOAConsumerException
	{
		usedHeaders.add("properties");
		next.addProperties(properties);
	}

	@Override
	public void addMiscHeader(String name, List<Object> content) throws HOAConsumerException
	{
		usedHeaders.add(name);
		if (name.substring(0,1).toUpperCase().equals(name.substring(0,1))) {
			// first character is upper case -> if we don't know what to do with this header,
			// raise exception as this may change the semantics of the automaton
			throw new HOAConsumerException("Header "+name+" potentially has semantic relevance, but is not supported");
		}
		next.addMiscHeader(name, content);
	}

	@Override
	public void notifyBodyStart() throws HOAConsumerException
	{
		// check for existence of mandatory headers
		headerIsMandatory("Acceptance");

		// check that all AP indizes in aliases are valid
		if (numberOfAPs == null) {
			// there was no AP-header, equivalent to AP: 0
			numberOfAPs = 0;
		}
		int highestAPIndex = apsInAliases.length() - 1;
		if (highestAPIndex > 0 && highestAPIndex >= numberOfAPs) {
			throw new HOAConsumerException("AP index "+highestAPIndex+" in some alias definition is out of range (0 - "+(numberOfAPs-1)+")");
		}

		if (accName != null) {
			checkAccName();
		}
		
		implicitEdgeHelper = new ImplicitEdgeHelper(numberOfAPs);
		
		next.notifyBodyStart();
	}

	@Override
	public void addState(int id, String info, BooleanExpression<AtomLabel> labelExpr, List<Integer> accSignature) throws HOAConsumerException
	{
		checkStateIndex(id);
		if (statesWithDefinition.get(id)) {
			throw new HOAConsumerException("State "+id+" is defined multiple times");
		}
		statesWithDefinition.set(id);
		currentState = id;
		
		if (accSignature != null) {
			checkAcceptanceSignature(accSignature, false);
		}
		
		if (labelExpr != null) {
			checkLabelExpression(labelExpr);
		}
		
		// reset flags
		currentStateHasStateLabel = false;
		currentStateHasTransitionLabel = false;
		currentStateHasImplicitEdge = false;
		currentStateHasExplicitEdge = false;

		implicitEdgeHelper.startOfState(id);

		if (labelExpr != null) {
			currentStateHasStateLabel = true;
		}

		next.addState(id, info, labelExpr, accSignature);
	}

	@Override
	public void addEdgeImplicit(int stateId, List<Integer> conjSuccessors, List<Integer> accSignature) throws HOAConsumerException
	{
		assert(stateId == currentState);

		for (Integer succ : conjSuccessors) {
			checkStateIndexTarget(succ);
		}
		
		if (accSignature != null) {
			checkAcceptanceSignature(accSignature, true);
		}

		if (currentStateHasExplicitEdge) {
			throw new HOAConsumerException("Can not mix explicit and implicit edge definitions (state "+stateId+")");
		}
		currentStateHasImplicitEdge = true;
		
		currentStateHasTransitionLabel = true;
		if (currentStateHasStateLabel) {
			throw new HOAConsumerException("Can not mix state labels and implicit edge definitions (state "+stateId+")");
		}

		implicitEdgeHelper.nextImplicitEdge();
		next.addEdgeImplicit(stateId, conjSuccessors, accSignature);
	}

	@Override
	public void addEdgeWithLabel(int stateId, BooleanExpression<AtomLabel> labelExpr, List<Integer> conjSuccessors, List<Integer> accSignature)
			throws HOAConsumerException
	{
		assert(stateId == currentState);

		for (Integer succ : conjSuccessors) {
			checkStateIndexTarget(succ);
		}

		if (accSignature != null) {
			checkAcceptanceSignature(accSignature, true);
		}

		if (labelExpr != null) {
			checkLabelExpression(labelExpr);
		}
		
		if (labelExpr != null) {
			currentStateHasTransitionLabel = true;
			if (currentStateHasStateLabel) {
				throw new HOAConsumerException("Can not mix state and transition labeling (state "+stateId+")");
			}
		}

		if (currentStateHasImplicitEdge) {
			throw new HOAConsumerException("Can not mix explicit and implicit edge definitions (state "+stateId+")");
		}
		currentStateHasExplicitEdge = true;

		next.addEdgeWithLabel(stateId, labelExpr, conjSuccessors, accSignature);
	}
	
	@Override
	public void notifyEndOfState(int stateId) throws HOAConsumerException
	{
		implicitEdgeHelper.endOfState();
		next.notifyEndOfState(stateId);
	}

	@Override
	public void notifyEnd() throws HOAConsumerException
	{
		// check sanity of state definitions and target states
		checkStates();
		next.notifyEnd();
	}

	@Override
	public void notifyAbort()
	{
		next.notifyAbort();
	}

	// ----------------------------------------------------------------------------
	
	private void doWarning(String warning) throws HOAConsumerException
	{
		next.notifyWarning(warning);
	}

	
	// ----------------------------------------------------------------------------
	
	private void headerIsMandatory(String name) throws HOAConsumerException
	{
		if (!usedHeaders.contains(name)) {
			throw new HOAConsumerException("Mandatory header "+name+" is missing");
		}
	}

	private void headerAtMostOnce(String headerName) throws HOAConsumerException
	{
		if (usedHeaders.contains(headerName)) {
			throw new HOAConsumerException("Header "+headerName+" occurs multiple times, but is allowed only once.");
		}
		usedHeaders.add(headerName);
	}

	private void checkStateIndex(int index) throws HOAConsumerException
	{
		if (index < 0) {
			throw new HOAConsumerException("State index "+index+" is negative");
		}
		if (numberOfStates != null) {
			if (index >= numberOfStates) {
				throw new HOAConsumerException("State index "+index+" is out of range (0 - "+(numberOfStates-1)+")");
			}
		}
	}

	/**
	 * Checks that the target state index of a transition is valid.
	 * @param index
	 * @throws HOAConsumerException
	 */
	private void checkStateIndexTarget(int index) throws HOAConsumerException
	{
		if (index < 0) {
			throw new HOAConsumerException("State index "+index+" is negative");
		}
		if (numberOfStates != null) {
			if (index >= numberOfStates) {
				throw new HOAConsumerException("State index "+index+" is out of range (0 - "+(numberOfStates-1)+")");
			}
		}
		targetStatesOfTransitions.set(index);
	}

	private void checkStates() throws HOAConsumerException
	{
		boolean haveComplainedAboutMissingStates = false;
		
		// if numberOfStates is set, check that all states are in range
		if (numberOfStates != null) {
			// the states with a definition
			int highestStateIndex = statesWithDefinition.length() - 1;
			if (highestStateIndex >= numberOfStates) {
				throw new HOAConsumerException("State index "+highestStateIndex+" is out of range (0 - "+(numberOfStates-1)+")");
			}

			// the states occuring as targets
			highestStateIndex = targetStatesOfTransitions.length() - 1;
			if (highestStateIndex >= numberOfStates) {
				throw new HOAConsumerException("State index "+highestStateIndex+" (target in a transition) is out of range (0 - "+(numberOfStates-1)+")");
			}
			
			// the start states
			highestStateIndex = startStates.length() - 1;
			if (highestStateIndex >= numberOfStates) {
				throw new HOAConsumerException("State index "+highestStateIndex+" (start state) is out of range (0 - "+(numberOfStates-1)+")");
			}

			if (statesWithDefinition.cardinality() != numberOfStates) {
				int missing = numberOfStates - statesWithDefinition.cardinality();
				doWarning("There are "+missing+" states without definition");
				haveComplainedAboutMissingStates = true;
			}
		}

		BitSet targetsButNoDefinition = (BitSet) targetStatesOfTransitions.clone();
		targetsButNoDefinition.andNot(statesWithDefinition);
		if (!targetsButNoDefinition.isEmpty() && !haveComplainedAboutMissingStates) {
			doWarning("There are "+targetsButNoDefinition.cardinality()+" states that are targets of transitions but that have no definition");
			haveComplainedAboutMissingStates = true;
		}

		BitSet startStatesButNoDefinition = (BitSet) startStates.clone();
		startStatesButNoDefinition.andNot(statesWithDefinition);
		if (!startStatesButNoDefinition.isEmpty() && !haveComplainedAboutMissingStates) {
			doWarning("There are "+startStatesButNoDefinition.cardinality()+" states that are start states but that have no definition");
			haveComplainedAboutMissingStates = true;
		}
	}

	private void checkAcceptanceCondition(BooleanExpression<AtomAcceptance> accExpr) throws HOAConsumerException
	{
		assert (numberOfAcceptanceSets != null);

		switch (accExpr.getType()) {
		case EXP_TRUE:
		case EXP_FALSE:
			return;
		case EXP_AND:
		case EXP_OR:
			checkAcceptanceCondition(accExpr.getLeft());
			checkAcceptanceCondition(accExpr.getRight());
			return;
		case EXP_NOT:
			throw new HOAConsumerException("Acceptance condition contains boolean negation, not allowed");
		case EXP_ATOM:
			int acceptanceSet = accExpr.getAtom().getAcceptanceSet();
			if (acceptanceSet < 0 || acceptanceSet >= numberOfAcceptanceSets) {
				throw new HOAConsumerException("Acceptance condition contains acceptance set with index "+acceptanceSet+", valid range is 0 - "+(numberOfAcceptanceSets-1));
			}
			return;
		}
		throw new UnsupportedOperationException("Unknown operator in acceptance condition: "+accExpr);
	}
	
	private void checkAcceptanceSignature(List<Integer> accSignature, boolean inTransition) throws HOAConsumerException
	{
		for (Integer acceptanceSet : accSignature) {
			if (acceptanceSet < 0 || acceptanceSet >= numberOfAcceptanceSets) {
				throw new HOAConsumerException("Acceptance signature "
						+ (inTransition ? "(in transition) " : "")
						+ "for state index "+currentState+" contains acceptance set with index "+acceptanceSet+", valid range is 0 - "+(numberOfAcceptanceSets-1));
			}

		}
	}

	private void checkAliasesAreDefined(BooleanExpression<AtomLabel> expr) throws HOAConsumerException
	{
		switch (expr.getType()) {
		case EXP_TRUE:
		case EXP_FALSE:
			return;
		case EXP_AND:
		case EXP_OR:
			checkAliasesAreDefined(expr.getLeft());
			checkAliasesAreDefined(expr.getRight());
			return;
		case EXP_NOT:
			checkAliasesAreDefined(expr.getLeft());
			return;
		case EXP_ATOM:
			if (expr.getAtom().isAlias()) {
				if (!aliases.contains(expr.getAtom().getAliasName())) {
					throw new HOAConsumerException("Alias @"+expr.getAtom().getAliasName()+" is not defined");
				}
			}
			return;
		}
		throw new UnsupportedOperationException("Unknown operator in label expression: "+expr);
	}

	/**
	 * Traverse the expression and, for every label encountered, set the corresponding bit in the
	 * {@code result} BitSet.
	 * @param expr the expression
	 * @param result a BitSet where the additional bits corresponding to APs are set
	 */
	private void gatherLabels(BooleanExpression<AtomLabel> expr, BitSet result) {
		switch (expr.getType()) {
		case EXP_TRUE:
		case EXP_FALSE:
			return;
		case EXP_AND:
		case EXP_OR:
			gatherLabels(expr.getLeft(), result);
			gatherLabels(expr.getRight(), result);
			return;
		case EXP_NOT:
			gatherLabels(expr.getLeft(), result);
			return;
		case EXP_ATOM:
			if (!expr.getAtom().isAlias()) {
				result.set(expr.getAtom().getAPIndex());
			}
			return;
		}
		throw new UnsupportedOperationException("Unknown operator in label expression: "+expr);
	}
	
	private void checkLabelExpression(BooleanExpression<AtomLabel> expr) throws HOAConsumerException
	{
		switch (expr.getType()) {
		case EXP_TRUE:
		case EXP_FALSE:
			return;
		case EXP_AND:
		case EXP_OR:
			checkLabelExpression(expr.getLeft());
			checkLabelExpression(expr.getRight());
			return;
		case EXP_NOT:
			checkLabelExpression(expr.getLeft());
			return;
		case EXP_ATOM:
			if (expr.getAtom().isAlias()) {
				if (!aliases.contains(expr.getAtom().getAliasName())) {
					throw new HOAConsumerException("Alias @"+expr.getAtom().getAliasName()+" is not defined");
				}
			} else {
				assert(numberOfAPs != null);
				int apIndex = expr.getAtom().getAPIndex();
				if (apIndex >= numberOfAPs) {
					throw new HOAConsumerException("AP index "+apIndex+" in expression is out of range (0 - "+(numberOfAPs-1)+"): "+expr);
				}
			}
			return;
		}
		throw new UnsupportedOperationException("Unknown operator in label expression: "+expr);
	}

	// ----------------------------------------------------------------------------
	
	private void checkAccName() throws HOAConsumerException {
		AcceptanceRepository repository = new AcceptanceRepositoryStandard();
		
		try {
			BooleanExpression<AtomAcceptance> canonical = repository.getCanonicalAcceptanceExpression(accName, accExtraInfo);
			if (canonical == null) {
				// acceptance name is not known
				return;
			}

			//System.out.println("Canonical: "+canonical);
			//System.out.println("Acceptance: "+acceptance);
			
			assert (acceptance != null);
			if (!BooleanExpression.areSyntacticallyEqual(acceptance, canonical)) {
				throw new HOAConsumerException("The acceptance given by the Acceptance and by the acc-name headers do not match syntactically:"
						+ "\nFrom Acceptance-header: "+acceptance
						+ "\nCanonical expression for acc-name-header: "+canonical);
			}
		} catch (IllegalArgumentException e) {
			throw new HOAConsumerException(e.getMessage());
		}
	}
}
