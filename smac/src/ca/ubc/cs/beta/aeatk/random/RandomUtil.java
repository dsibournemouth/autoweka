package ca.ubc.cs.beta.aeatk.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomUtil {
	
	public static int[] getPermutation(int n, int offset, Random rand)
	{
		if (n < 0)
		{
			throw new IllegalArgumentException();
		}
		
		int[] perm = new int[n];
		
		for(int i=0; i < n; i++)
		{
			perm[i] = i + offset;
		}
		
		for(int i=n-1; i > 0 ; i--)
		{
			int rndIdx = rand.nextInt(i+1);
			int tmp = perm[i];
			perm[i] = perm[rndIdx];
			perm[rndIdx] = tmp;
		}
		
		return perm;
	}
	
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void permuteList(List list, int[] permutations)
	{
		if(permutations.length != list.size())
		{
			throw new IllegalArgumentException("List length and permutation length are not equal");
		}
		List<Object> tmpList = new ArrayList<Object>(permutations.length);
		for(int i=0; i < permutations.length; i++ )
		{
			tmpList.add(list.get(permutations[i]));
		}
		list.clear();
		list.addAll( tmpList);
	}
		
	public static double[] getPermutationOfArray(double[] input, Random rand){
		int[] permutation = getPermutation(input.length, 0, rand);
		double result[] = new double[input.length];
		for (int i = 0; i < permutation.length; i++) {
			result[i] = input[permutation[i]]; 
		}
		return result;
	}
		
}
