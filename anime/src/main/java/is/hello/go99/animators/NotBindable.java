package is.hello.go99.animators;

import android.animation.Animator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a subtype of {@link android.animation.Animator} as not being
 * eligible for binding with {@link AnimatorContext#bind(Animator, String)}.
 * <p>
 * Within <code>anime-android-go-99</code>, this indicates that
 * the class implements animator context binding by itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface NotBindable {
}
