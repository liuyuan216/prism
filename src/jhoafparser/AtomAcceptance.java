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

/** 
 * Atom of an acceptance condition, either Fin(accSet), Fin(!accSet), Inf(accSet) or Inf(!accSet)
  */
public class AtomAcceptance implements Atom {
	public enum Type {TEMPORAL_FIN, TEMPORAL_INF};
	
	private Type type;
	private Integer accSet = null;
	private boolean negated = false;

	public AtomAcceptance(Type type, Integer accSet, boolean negated) {
		this.type = type;
		this.accSet = accSet;
		this.negated = negated;
	}
	
	public static AtomAcceptance Fin(int accSet) {
		return new AtomAcceptance(Type.TEMPORAL_FIN, accSet, false);
	}
	
	public static AtomAcceptance FinNot(int accSet) {
		return new AtomAcceptance(Type.TEMPORAL_FIN, accSet, true);
	}

	public static AtomAcceptance Inf(int accSet) {
		return new AtomAcceptance(Type.TEMPORAL_INF, accSet, false);
	}
	
	public static AtomAcceptance InfNot(int accSet) {
		return new AtomAcceptance(Type.TEMPORAL_INF, accSet, true);
	}
	
	public Atom clone()
	{
		return new AtomAcceptance(type, accSet, negated);
	}

	
	public Type getType()
	{
		return type;
	}

	public int getAcceptanceSet() {
		return accSet;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	public String toString() {
		return  (type == Type.TEMPORAL_FIN ? "Fin" : "Inf")+"("+(negated ? "!" : "") +accSet+")";
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accSet == null) ? 0 : accSet.hashCode());
		result = prime * result + (negated ? 1231 : 1237);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AtomAcceptance))
			return false;
		AtomAcceptance other = (AtomAcceptance) obj;
		if (type != other.type)
			return false;
		if (accSet == null) {
			if (other.accSet != null)
				return false;
		} else if (!accSet.equals(other.accSet))
			return false;
		if (negated != other.negated)
			return false;
		return true;
	}
}
