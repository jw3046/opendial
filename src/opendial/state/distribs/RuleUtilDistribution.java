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

package opendial.state.distribs;

import java.util.HashMap;
import java.util.Map;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.utility.UtilityDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.BasicCondition.Relation;
import opendial.domains.rules.conditions.ComplexCondition;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.effects.BasicEffect;
import opendial.domains.rules.effects.BasicEffect.EffectType;
import opendial.domains.rules.effects.Effect;
import opendial.state.anchoring.AnchoredRule;
import opendial.state.anchoring.Output;


/**
 * Utility distribution based on a rule specification.
 *  *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class RuleUtilDistribution implements UtilityDistribution {

	// logger
	public static Logger log = new Logger("RuleUtilDistribution", Logger.Level.DEBUG);

	// A rule
	AnchoredRule rule;

	// a cache with the utility assignments
	Map<Assignment,UtilityTable> cache;

	// ===================================
	//  DISTRIBUTION CONSTRUCTION
	// ===================================


	/**
	 * Creates a new rule-based utility distribution, based on an anchored rule
	 * 
	 * @param rule the anchored rule
	 * @throws DialException if the rule is not a decision rule
	 */
	public RuleUtilDistribution(AnchoredRule rule) throws DialException {

		if ((rule.getRule().getRuleType() == RuleType.UTIL)) {
			this.rule = rule;
		}
		else {
			throw new DialException("only utility rules can define a " +
					"rule-based utility distribution");
		}

		if (rule.getParameters().isEmpty()) {
			cache = new HashMap<Assignment,UtilityTable>();
		}
		
	}




	/**
	 * Does nothing.
	 */
	@Override
	public void modifyVarId(String oldId, String newId) {
		if (cache != null) {
		cache.clear();
		}
	}



	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the utility for Q(input), where input is the assignment
	 * of values for both the chance nodes and the action nodes
	 * 
	 * @param fullInput the value assignment
	 * @return the corresponding utility
	 */
	@Override
	public double getUtil(Assignment fullInput) {

		Assignment input = fullInput.getTrimmedInverse(rule.getOutputVariables());
		Assignment actions = fullInput.getTrimmed(rule.getOutputVariables());

		if (cache != null && cache.containsKey(input) && cache.get(input).getRows().contains(actions)) {
			return cache.get(input).getUtil(actions);
		}
		
		double util = getUtil(input, actions);
		
		if (cache != null) {
			if (!cache.containsKey(input)) {
				cache.put(input, new UtilityTable());
			}
			cache.get(input).setUtil(actions, util);
		}
	
		return util;
	}

	// ===================================
	//  UTILITY METHODS
	// ===================================


	/**
	 * Returns true
	 * @return true
	 */
	@Override
	public boolean isWellFormed() {
		return true;
	}

	/**
	 * Returns a copy of the distribution
	 * 
	 * @return the copy
	 */
	@Override
	public RuleUtilDistribution copy() {
		try { 
			RuleUtilDistribution distrib = new RuleUtilDistribution (rule);
			return distrib;
		} 
		catch (DialException e) { e.printStackTrace(); return null; }
	}


	/**
	 * Returns the pretty print for the rule
	 * 
	 * @return the pretty print
	 */
	@Override
	public String toString() {
		return rule.toString();
	}


	// ===================================
	//  PRIVATE METHODS
	// ===================================


	/**
	 * Returns the utility of the action assignment given the particular input.
	 * 
	 * @param input the input assignment
	 * @param actions the action assignment
	 * @return the resulting utility
	 */
	private double getUtil(Assignment input, Assignment actions) {

		try {
			Assignment formattedAction = actions.removePrimes();
			Output fullCase = rule.getMatchingOutput(input);
			
			double totalUtil = 0;
			for (Effect effectOutput : fullCase.getEffects()) {
				Condition condition = convertToCondition(effectOutput);
				
				if (condition.isSatisfiedBy(formattedAction)) {
					totalUtil += fullCase.getParameter(effectOutput).getParameterValue(input);
				}
			}
			return totalUtil;
		}
		catch (DialException e) {
			log.warning("error extracting utility: " + e);
		}
		return 0.0;	
	}
	
	
	/**
	 * Returns a condition corresponding to the "translation" of the effect into a 
	 * condition.
	 * 
	 * @param e the effect
	 * @return the corresponding condition
	 */
	private Condition convertToCondition(Effect e) {
		ComplexCondition condition = new ComplexCondition();
		for (BasicEffect subeffect : e.getSubEffects()) {
			Template variable = subeffect.getVariable();
			Template value = subeffect.getTemplateValue();
			Relation r = (subeffect.getType() == EffectType.DISCARD)? Relation.UNEQUAL : Relation.EQUAL;
			condition.addCondition(new BasicCondition(variable, value, r));
		}
		return condition;
	}

}
