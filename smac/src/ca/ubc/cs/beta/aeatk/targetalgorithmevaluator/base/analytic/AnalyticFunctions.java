package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.analytic;

import java.util.Arrays;
import java.util.List;

public enum AnalyticFunctions {
	ZERO("xN in R for all N, y = 0"),
	ADD("xN = 0, for all N, y = 0"),
	CAMELBACK( "(x0,x1) = (-0.0898, 0.7126) & (0,0898, -0.7126), xN , N > 2 are all ignored,y=3.9684 (because it is shifted by 5)"),
	BRANINS("(x0,x1) = (-pi, 12.275) & (pi , 2.275) & (9,42478, 2.475), xN, N > 2 are all ignored, y = 0.397887"),
	SINEPLUSONE("x in { 3*pi/2 + 2*pi*n}, n in Z");
	
	double BRANIN_A=1;
	double BRANIN_B=5.1/(4*Math.PI*Math.PI);
	double BRANIN_C = 5/Math.PI;
	double BRANIN_D =6 ;
	double BRANIN_E = 10;
	double BRANIN_F = 1/(Math.PI * 8);
	
	
	private String minima;
	private AnalyticFunctions(String minima)
	{
		this.minima = minima;
	}
	
	public double evaluate(List<Double> xVals)
	{
		double[] myXVals = new double[xVals.size()];
		for(int i=0; i < xVals.size(); i++)
		{
			myXVals[i] = xVals.get(i);
		}
		return evaluate(myXVals);
		
	}
	public double evaluate(double[] xVals)
	{
		switch(this){
		case CAMELBACK:
			{
				if(xVals.length < 2)
				{
					throw new IllegalArgumentException(this +" function requires at least two parameters, x0, x1");
				}
				double x = xVals[0];
				double y = xVals[1];
				return (4-2.1*Math.pow(x,2)+(Math.pow(x,4)/3))*Math.pow(x,2)+x*y+(-4+4*Math.pow(y,2))*Math.pow(y,2)+5;
			}
		case BRANINS:
			{
				if(xVals.length < 2)
				{
					throw new IllegalArgumentException(this +" function requires at least two parameters, x0, x1. Got: " + Arrays.toString(xVals));
				}
				double x = xVals[0];
				double y = xVals[1];
				
				
				
				return BRANIN_A*Math.pow((y-BRANIN_B*x*x*+BRANIN_C*x-BRANIN_D),2)+BRANIN_E*(1-BRANIN_F)*Math.cos(x)+BRANIN_E;
				
			}
		case ADD:
			double sum = 0;
			for(double d : xVals)
			{
				sum += d;
			}
			
			return sum;
		case ZERO:
			return 0;
			
		case SINEPLUSONE:
			return Math.sin(xVals[0])+1;
		default:
			throw new IllegalStateException(this+" not implemented currently");
		}
	}
	
	public String getMinima() {
		return minima;
	}
	
	
	
}
