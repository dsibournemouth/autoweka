package ca.ubc.cs.beta.aeatk.parameterconfigurationspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.objecthunter.exp4j.operator.Operator;

public class ForbiddenOperators {

	
	private static final int HIGHEST_PRECEDENCE = Operator.PRECEDENCE_ADDITION;
	
	/****
	 * NOTE: HIGHER PRECEDENCE OPERATORS SHOULD BE PLACED VERTICALLY ABOVE LOWER PRECEDENCE ONCES.
	 * 
	 * See also this table: http://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html
	 * 
	 * 
	 * 
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	
	
	

	private static final class LessThanOrEqualsOperator extends Operator {
		private LessThanOrEqualsOperator() {
			super("<=", 2, true, HIGHEST_PRECEDENCE - 70);
		}

		@Override
		public double apply(double... values) {
			return (values[0] <= values[1]) ? 1 : 0;
		}
	}


	private static final class GreaterThanOrEqualToOperator extends Operator {
		private GreaterThanOrEqualToOperator() {
			super(">=", 2, true, HIGHEST_PRECEDENCE - 70);
		}

		@Override
		public double apply(double... values) {
			return (values[0] >= values[1]) ? 1 : 0;
		}
	}


	private static final class LessThanOperator extends Operator {
		private LessThanOperator() {
			super("<", 2, true, HIGHEST_PRECEDENCE - 70);
		}

		@Override
		public double apply(double... values) {
			return (values[0] < values[1]) ? 1 : 0;
		}
	}


	private static final class GreaterThanOperator extends Operator {
		private GreaterThanOperator() {
			super(">", 2, true, HIGHEST_PRECEDENCE - 70);
		}

		@Override
		public double apply(double... values) {
			return (values[0] > values[1]) ? 1 : 0;
		}
	}
	
	
	
	
	private static final class NotEqualsOperator extends Operator {
		private NotEqualsOperator() {
			super("!=", 2, true, HIGHEST_PRECEDENCE - 80);
		}

		@Override
		public double apply(double... values) {
			return (values[0] != values[1]) ? 1 : 0;
		}
	}

	
	private static final class EqualsOperator extends Operator {
		public EqualsOperator() {
			super("==",2, true,HIGHEST_PRECEDENCE - 80);
		}

		@Override
		public double apply(double... values) {
			return (values[0] == values[1]) ? 1 : 0;
		}
		
	}
	
	
	
	
	

	private static final class AndOperator extends Operator {
		private AndOperator() {
			super("&&", 2, true, HIGHEST_PRECEDENCE - 90);
		}

		@Override
		public double apply(double... values) {
			return (values[0] > 0 && values[1] > 0 ) ? 1 : 0;
		}
	}
	
	
	
	private static final class OrOperator extends Operator {
		private OrOperator() {
			super("||", 2, true, HIGHEST_PRECEDENCE - 100 );
		}

		@Override
		public double apply(double... values) {
			return (values[0] > 0 || values[1] > 0) ? 1 : 0;
		}
	}






	

	public static final List<Operator> operators;
	
	
	static
	{
		List<Operator> cos = new ArrayList<>();
		//Precedence of 0 (Not Equals)
		cos.add(new NotEqualsOperator());
		
		//==
		cos.add(new EqualsOperator());
				
		
		
		
		//Precendence of 1
		cos.add(new GreaterThanOperator());
		
		cos.add(new LessThanOperator());
		
		cos.add(new GreaterThanOrEqualToOperator());
		
	
		cos.add(new LessThanOrEqualsOperator());

		//Precedence of two
		cos.add(new AndOperator());
		
	
		cos.add(new OrOperator());

		
		
		
		operators = Collections.unmodifiableList(cos);
	}
	
	
}
