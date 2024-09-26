package ca.ubc.cs.beta.aeatk.misc.jcommander.converter;

import ca.ubc.cs.beta.aeatk.misc.inputparsers.DurationToSeconds;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class DurationConverter implements IStringConverter<Integer>{

		@Override
		public Integer convert(String arg0) {
			try {
				return DurationToSeconds.numberOfSecondsFromString(arg0);
			} 	catch(RuntimeException e){
				throw new ParameterException("Could not parse as double " + arg0 + ":" + e.getMessage());
			}
		}

}
