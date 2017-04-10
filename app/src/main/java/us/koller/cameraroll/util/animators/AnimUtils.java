package us.koller.cameraroll.util.animators;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Property;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

// stolen from: https://github.com/nickbutcher/plaid/blob/master/app/src/main/java/io/plaidapp/util/AnimUtils.java

class AnimUtils {

    private AnimUtils() {
    }

    private static Interpolator fastOutSlowIn;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static Interpolator getFastOutSlowInInterpolator(Context context) {
        if (fastOutSlowIn == null) {
            fastOutSlowIn = AnimationUtils.loadInterpolator(context,
                    android.R.interpolator.fast_out_slow_in);
        }
        return fastOutSlowIn;
    }

    /**
     * An implementation of {@link android.util.Property} to be used specifically with fields of
     * type <code>float</code>. This type-specific subclass enables performance benefit by allowing
     * calls to a {@link #set(Object, Float) set()} function that takes the primitive
     * <code>float</code> type and avoids autoboxing and other overhead associated with the
     * <code>Float</code> class.
     *
     * @param <T> The class on which the Property is declared.
     **/
    static abstract class FloatProperty<T> extends Property<T, Float> {
        FloatProperty(String name) {
            super(Float.class, name);
        }

        /**
         * A type-specific override of the {@link #set(Object, Float)} that is faster when dealing
         * with fields of type <code>float</code>.
         */
        public abstract void setValue(T object, float value);

        @Override
        final public void set(T object, Float value) {
            setValue(object, value);
        }
    }

    /**
     * An extension to {@link ColorMatrix} which caches the saturation value for animation
     * purposes.
     */
    static class ObservableColorMatrix extends ColorMatrix {

        private float saturation = 1f;

        ObservableColorMatrix() {
            super();
        }

        private float getSaturation() {
            return saturation;
        }

        @Override
        public void setSaturation(float saturation) {
            this.saturation = saturation;
            super.setSaturation(saturation);
        }

        static final Property<ObservableColorMatrix, Float> SATURATION
                = new FloatProperty<ObservableColorMatrix>("saturation") {

            @Override
            public void setValue(ObservableColorMatrix cm, float value) {
                cm.setSaturation(value);
            }

            @Override
            public Float get(ObservableColorMatrix cm) {
                return cm.getSaturation();
            }
        };
    }
}
