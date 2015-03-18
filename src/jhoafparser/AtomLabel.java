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
 * Atom of a label expression (either an atomic proposition index or an alias)
 *
 */
public class AtomLabel implements Atom {
	private enum Type {AP_INDEX, ALIAS};
	
	private Type type;
	private Integer apIndex = null;
	private String aliasName = null;
	
	private AtomLabel() {type = null;}
	
	public static AtomLabel createAPIndex(Integer apIndex) {
		AtomLabel result = new AtomLabel();
		result.type = Type.AP_INDEX;
		result.apIndex = apIndex;
		return result;
	}
	
	public static AtomLabel createAlias(String aliasName) {
		AtomLabel result = new AtomLabel();
		result.type = Type.ALIAS;
		result.aliasName = aliasName;
		return result;
	}

	public AtomLabel clone()
	{
		if (isAlias()) {
			return createAlias(aliasName);
		} else {
			return createAPIndex(apIndex);
		}
	}
	
	public boolean isAlias() {return type == Type.ALIAS;}

	public String getAliasName() {
		if (!isAlias()) throw new UnsupportedOperationException(this.toString()+" is not an alias");
		return aliasName;
	}

	public int getAPIndex() {
		if (isAlias()) throw new UnsupportedOperationException(this.toString()+" is not an AP index");
		return apIndex;
	}

	@Override
	public String toString() {
		switch (type) {
		case AP_INDEX:
			return apIndex.toString();
		case ALIAS:
			return "@"+aliasName;
		}
		
		throw new RuntimeException("Unhandled type");
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aliasName == null) ? 0 : aliasName.hashCode());
		result = prime * result + ((apIndex == null) ? 0 : apIndex.hashCode());
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
		if (!(obj instanceof AtomLabel))
			return false;
		AtomLabel other = (AtomLabel) obj;
		if (type != other.type)
			return false;
		if (aliasName == null) {
			if (other.aliasName != null)
				return false;
		} else if (!aliasName.equals(other.aliasName))
			return false;
		if (apIndex == null) {
			if (other.apIndex != null)
				return false;
		} else if (!apIndex.equals(other.apIndex))
			return false;
		return true;
	}


}
