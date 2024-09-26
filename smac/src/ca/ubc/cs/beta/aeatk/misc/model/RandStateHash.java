package ca.ubc.cs.beta.aeatk.misc.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import ec.util.MersenneTwister;

/**
 * Utility class that generates hash codes for random objects
 * 
 * Used for matlab synchronization
 * 
 * 
 *
 */
public class RandStateHash {
	
	private static int nextHash(int prevHash, int newValue)
	{
		
		return Math.abs(31*prevHash + newValue) % 32452867;
	}
	
	private static int  nextHash(int prevHash, long newValue)
	{
		int newHash = nextHash(prevHash,(int) newValue );
		return nextHash(newHash, (int) (newValue >>> 32));
	}
	
	
	public static int randHash(Random rOrig)
	{
		Random r = null;
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream o = new ObjectOutputStream(bout);
			o.writeObject(rOrig);
			o.close();
			ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
			ObjectInputStream in = new ObjectInputStream(bin);
			r = (Random) in.readObject();
			
			
			
		} catch (IOException e) {
			//This should never be able to happen.
			throw new IllegalStateException(e);
		} catch (ClassNotFoundException e) {
			//This should never be able to happen.
			throw new IllegalStateException(e);
			
			
		}
		
		int hash = r.nextBoolean() ? 0 : 1;
		hash = nextHash(hash, r.nextInt());
		hash = nextHash(hash, Double.doubleToLongBits(r.nextDouble()));
		hash = nextHash(hash, Double.doubleToLongBits(r.nextGaussian()));
		hash = nextHash(hash, r.nextLong());
		hash = nextHash(hash, r.nextInt(Integer.MAX_VALUE));
		hash = nextHash(hash, Float.floatToIntBits(r.nextFloat()));
		return hash;
	}
	
	
	public static void main(String args[])
	{
		//This tests that the random Hash shouldn't alter state.
		
		Random r = new MersenneTwister(1);
		for(int i=0; i < 100; i++)
		{
			int length = r.nextInt(10000) + 1000;
			Random r2 = new MersenneTwister(r.nextInt());
			for(int j = 0; j < length; j++)
			{
				switch(r.nextInt(5))
				{
				case 0:
					r2.nextBoolean();
					break;
				case 1:
					r2.nextDouble();
					break;
				case 2:
					r2.nextLong();
					break;
				case 3:
					r2.nextGaussian();
					break;
				case 4:					
					r2.nextInt(32);
				}
			}
			
			
			System.out.println( "Random Hash (" + i + ")" +  randHash(r2) + " and again " + randHash(r2) + " and again " + randHash(r2) );
		}
	
	
	r = new MersenneTwister(1);
	for(int i=0; i < 100; i++)
	{
		int length = r.nextInt(10000) + 1000;
		Random r2 = new MersenneTwister(r.nextInt());
		for(int j = 0; j < length; j++)
		{
			switch(r.nextInt(5))
			{
			case 0:
				r2.nextBoolean();
				break;
			case 1:
				r2.nextDouble();
				break;
			case 2:
				r2.nextLong();
				break;
			case 3:
				r2.nextGaussian();
				break;
			case 4:					
				r2.nextInt(32);
			}
		}
		
		
		System.out.println( "Random Hash (" + i + ")" +  randHash(r) + " and again " + randHash(r2) + " and again " + randHash(r2) );
	}

	}

}
