package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class AnnotationUtils {

    public static Method getFirstAnnotatedMethod(Class<?> type, Class<? extends Annotation> annotation) {

        for (Method method : type.getDeclaredMethods()) {
            final Annotation annotation1 = method.getAnnotation(annotation);
            if (annotation1 != null) { return method; }
        }
        return null;
    }
}
