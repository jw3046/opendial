// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.bn.values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.arch.Logger;
import opendial.utils.StringUtils;

/**
 * Factory for creating variable values
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public class ValueFactory {

	// logger
	public static Logger log = new Logger("ValueFactory", Logger.Level.DEBUG);

	// none value (no need to recreate one everytime)
	static NoneVal noneValue = new NoneVal();

	// pattern to find a double value
	static Pattern doublePattern = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");

	// pattern to find an array of doubles
	static Pattern arrayPattern = Pattern.compile("\\[([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?,\\s*)*" +
			"([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)\\]");

	
	/**
	 * Creates a new value based on the provided string representation.
	 * If the string contains a numeric value, "true", "false", "None", 
	 * or opening and closing brackets, convert it to the appropriate
	 * values.  Else, returns a string value.
	 * 
	 * @param str the string representation for the value
	 * @return the resulting value
	 */
	public static Value create(String str) {
		
		if (str == null) {
			return noneValue;
		}
		str = str.trim();

		Matcher m = doublePattern.matcher(str);
		if (m.matches()) {
			return new DoubleVal(Double.parseDouble(str));
		}
		else if (str.equalsIgnoreCase("true")) {
			return new BooleanVal(true);
		}
		else if (str.equalsIgnoreCase("false")) {
			return new BooleanVal(false);
		}
		else if (str.equalsIgnoreCase("None")) {
			return none();
		}
		// adds the converted value
		else {
			Matcher m2 = arrayPattern.matcher(str);
			if (m2.matches()){
				List<Double> subVals = new ArrayList<Double>();
				for (String subVal : str.replace("[", "").replace("]", "").split(",")) {
					subVals.add(Double.parseDouble(subVal));
				}
				return new ArrayVal(subVals);
			}
			else if (str.startsWith("[") && str.endsWith("]")) {

				LinkedList<Value> subVals = new LinkedList<Value>();
				
				boolean openParenthesis = false;
				for (String subVal : str.replace("[", "").replace("]", "").split(",")) {
					subVal = subVal.trim();
					if (subVal.length() == 0) {
						continue;
					}
					else if (!openParenthesis) {
						subVals.add(create(subVal));
					}
					else {
						subVal = subVals.getLast().toString()+","+subVal;
						subVals.set(subVals.size()-1, create(subVal));
					}
					openParenthesis =  (StringUtils.countNbOccurrences(subVal, '(') >
					StringUtils.countNbOccurrences(subVal, ')'));
				}
				return new ListVal(subVals);
			}
		}

		return new StringVal(str);
	}

	/**
	 * Returns a double value given the double
	 * 
	 * @param d the double
	 * @return the value
	 */
	public static DoubleVal create(double d) {
		return new DoubleVal(d);
	}


	public static ArrayVal create(double[] d) {
		return new ArrayVal(d);
	}

	/**
	 * Returns the boolean value given the boolean
	 * 
	 * @param b the boolean 
	 * @return the double
	 */
	public static BooleanVal create(boolean b) {
		return new BooleanVal(b);
	}

	/**
	 * Returns the set value given the values
	 * 
	 * @param vals the values
	 * @return the set value
	 */
	public static ListVal create(Value...vals) {
		return new ListVal(vals);
	}

	/**
	 * Returns the set value given the values
	 * 
	 * @param vals the values
	 * @return the set value
	 */
	public static ListVal create(Collection<Value> vals) {
		return new ListVal(vals);
	}


	/**
	 * Returns the none value
	 * 
	 * @return the none value
	 */
	public static NoneVal none() {
		return noneValue;
	}

	public static Value concatenate(Value value, Value value2) {
		if (value instanceof StringVal && value2 instanceof StringVal) {
			return new StringVal(((StringVal)value).getString() + " " + ((StringVal)value2).getString());
		}
		else if (value instanceof NoneVal) {
			return value2;
		}
		else if (value2 instanceof NoneVal) {
			return value;
		}
		else {
			log.warning("concatenation not implemented for " + value + "+" + value2);
			return noneValue;
		}
	}
}
