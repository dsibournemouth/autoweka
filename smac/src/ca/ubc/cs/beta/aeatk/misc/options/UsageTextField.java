package ca.ubc.cs.beta.aeatk.misc.options;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
/**
 * This annotation is primarily used to manipulate the output of --help 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public @interface UsageTextField {
	
	/**
	 * Banner that is used to seperate a section, when set on a class.
	 * @return
	 */
	String titlebanner() default "========== %-20s ==========%n%n";
	
	/**
	 * Description of the parameter
	 * @return
	 */
	String description() default "" ;
	
	/**
	 * Overrides the default value with this string
	 * @return
	 */
	String defaultValues()  default "<NOT SET>";

	String title() default "";

	/**
	 * Overrides the auto detected domain with this string for a field.
	 * @return
	 */
	String domain() default "<NOT SET>";

	/**
	 * If true we won't print a banner
	 * @return
	 */
	boolean hiddenSection() default false;
	
	/**
	 * When set on a class, we will pretend that the argument is required even though JCommander thinks it isn't.
	 * @return
	 */
	String[] claimRequired() default {};
	
	/**
	 * When set on a class, if no arguments are provided, this class will be notified (generally to print things, and exit)
	 * @return
	 */
	Class<? extends NoArgumentHandler> noarg() default NoopNoArgumentHandler.class;
	
	
	/**
	 * I honestly don't know what this is doing, something to do with converters.
	 * @return
	 */
	Class<? extends Object> converterFileOptions() default Object.class;
	
	/**
	 * When set on a field this controls the level at which the fields are displayed, can also be set on the class level in which case it 
	 * changes the default for fileds which don't have this annotation defined.
	 * @return
	 */
	OptionLevel level() default OptionLevel.BASIC;
}
