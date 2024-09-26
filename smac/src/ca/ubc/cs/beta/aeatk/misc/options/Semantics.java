package ca.ubc.cs.beta.aeatk.misc.options;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Semantics{
	String name() default "";

	String domain() default "";
}

