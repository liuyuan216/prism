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

import java.util.ArrayList;
import java.util.List;

public class AcceptanceRepositoryStandard implements AcceptanceRepository
{
	private static final String ACC_ALL = "all";
	private static final String ACC_NONE = "none";
	
	private static final String ACC_BUCHI = "Buchi";
	private static final String ACC_COBUCHI = "coBuchi";
	
	private static final String ACC_RABIN = "Rabin";
	private static final String ACC_STREETT = "Streett";
	
	private static final String ACC_GENERALIZED_RABIN = "generalized-Rabin";
	
	public BooleanExpression<AtomAcceptance> getCanonicalAcceptanceExpression(String accName, List<Object> extraInfo) throws IllegalArgumentException {
		switch (accName) {
		case ACC_ALL:
			return forAll(extraInfo);
		case ACC_NONE:
			return forNone(extraInfo);

		case ACC_BUCHI:
			return forBuchi(extraInfo);
		case ACC_COBUCHI:
			return forCoBuchi(extraInfo);

		case ACC_RABIN:
			return forRabin(extraInfo);
		case ACC_STREETT:
			return forStreett(extraInfo);
		case ACC_GENERALIZED_RABIN:
			return forGeneralizedRabin(extraInfo);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public BooleanExpression<AtomAcceptance> forAll(List<Object> extraInfo) {
		checkNumberOfArguments(ACC_ALL, extraInfo, 0);

		return BooleanExpression.TRUE();
	}

	
	@SuppressWarnings("unchecked")
	public BooleanExpression<AtomAcceptance> forNone(List<Object> extraInfo) {
		checkNumberOfArguments(ACC_ALL, extraInfo, 0);

		return BooleanExpression.FALSE();
	}

	public BooleanExpression<AtomAcceptance> forBuchi(List<Object> extraInfo) {
		checkNumberOfArguments(ACC_BUCHI, extraInfo, 0);

		return new BooleanExpression<AtomAcceptance>(AtomAcceptance.Inf(0));
	}
	
	public BooleanExpression<AtomAcceptance> forCoBuchi(List<Object> extraInfo) {
		checkNumberOfArguments(ACC_COBUCHI, extraInfo, 0);

		return new BooleanExpression<AtomAcceptance>(AtomAcceptance.Fin(0));
	}

	@SuppressWarnings("unchecked")
	public BooleanExpression<AtomAcceptance> forRabin(List<Object> extraInfo) {
		checkNumberOfArguments(ACC_RABIN, extraInfo, 1);
		int numberOfPairs = extraInfoToIntegerList(ACC_RABIN, extraInfo).get(0);

		if (numberOfPairs == 0) {
			return BooleanExpression.FALSE();
		}

		BooleanExpression<AtomAcceptance> result = null;
		for (int i = 0; i < numberOfPairs; i++) {
			BooleanExpression<AtomAcceptance> fin = new BooleanExpression<AtomAcceptance>(AtomAcceptance.Fin(2*i));
			BooleanExpression<AtomAcceptance> inf = new BooleanExpression<AtomAcceptance>(AtomAcceptance.Inf(2*i+1));
			
			BooleanExpression<AtomAcceptance> pair = fin.and(inf);
			
			if (i == 0) {
				result = pair;
			} else {
				result = result.or(pair);
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public BooleanExpression<AtomAcceptance> forStreett(List<Object> extraInfo) {
		checkNumberOfArguments(ACC_STREETT, extraInfo, 1);
		int numberOfPairs = extraInfoToIntegerList(ACC_STREETT, extraInfo).get(0);

		if (numberOfPairs == 0) {
			return BooleanExpression.TRUE();
		}

		BooleanExpression<AtomAcceptance> result = null;
		for (int i = 0; i < numberOfPairs; i++) {
			BooleanExpression<AtomAcceptance> fin = new BooleanExpression<AtomAcceptance>(AtomAcceptance.Fin(2*i));
			BooleanExpression<AtomAcceptance> inf = new BooleanExpression<AtomAcceptance>(AtomAcceptance.Inf(2*i+1));
			
			BooleanExpression<AtomAcceptance> pair = fin.or(inf);
			
			if (i == 0) {
				result = pair;
			} else {
				result = result.and(pair);
			}
		}

		return result;
	}

	public BooleanExpression<AtomAcceptance> forGeneralizedRabin(List<Object> extraInfo) {
		List<Integer> parameters = extraInfoToIntegerList(ACC_GENERALIZED_RABIN, extraInfo);

		if (parameters.size() == 0) {
			throw new IllegalArgumentException("Acceptance "+ACC_GENERALIZED_RABIN+" needs at least one argument");
		}

		int numberOfPairs = parameters.get(0);
		if (parameters.size() != numberOfPairs + 1) {
			throw new IllegalArgumentException("Acceptance "+ACC_GENERALIZED_RABIN+" with " + numberOfPairs +" generalized pairs: There is not exactly one argument per pair");
		}
		
		BooleanExpression<AtomAcceptance> result = null;
		int currentIndex = 0;
		for (int i = 0; i < numberOfPairs; i++) {
			int numberOfInf = parameters.get(i+1);

			BooleanExpression<AtomAcceptance> fin = new BooleanExpression<AtomAcceptance>(AtomAcceptance.Fin(currentIndex++));
			BooleanExpression<AtomAcceptance> pair = fin;
			for (int j = 0; j< numberOfInf; j++) {
				BooleanExpression<AtomAcceptance> inf = new BooleanExpression<AtomAcceptance>(AtomAcceptance.Inf(currentIndex++));
				pair = pair.and(inf);
			}

			if (i == 0) {
				result = pair;
			} else {
				result = result.or(pair);
			}
		}

		return result;
	}

	private List<Integer> extraInfoToIntegerList(String accName, List<Object> extraInfo)
	{
		List<Integer> result = new ArrayList<Integer>(extraInfo.size());
		for (Object i : extraInfo) {
			if (!(i instanceof Integer)) {
				throw new IllegalArgumentException("For acceptance " + accName + ", all arguments have to be integers");
			}
			
			result.add((Integer)i);
		}

		return result;
	}
	
	private void checkNumberOfArguments(String accName, List<Object> extraInfo, int expectedNumberOfArguments) throws IllegalArgumentException
	{
		if (expectedNumberOfArguments != extraInfo.size()) {
			throw new IllegalArgumentException("For acceptance " + accName + ", expected " + expectedNumberOfArguments + " arguments, got " + extraInfo.size());
		}
	}
	
}
