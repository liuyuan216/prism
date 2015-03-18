package prism;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.BitSet;

import acceptance.AcceptanceOmega;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceType;
import jhoafparser.HOAFParser;
import jhoafparser.ParseException;

public class HOA2DSTAR
{
	@SuppressWarnings("unchecked")
	public static void main(String args[]) throws FileNotFoundException, ParseException, PrismException {
		String hoa_file = args.length > 0 ? args[0] : "-";
		
		HOAF2DA consumerDA = new HOAF2DA();

		InputStream input = hoa_file.equals("-") ? System.in : new FileInputStream(hoa_file);
		HOAFParser.parseHOA(input, consumerDA);

		DA<BitSet, ? extends AcceptanceOmega> da = consumerDA.getDA();
		if (da.getAcceptance().getType()==AcceptanceType.RABIN) {
			DA.printLtl2dstar((DA<BitSet, AcceptanceRabin>) da, System.out);
		}
	}
}
